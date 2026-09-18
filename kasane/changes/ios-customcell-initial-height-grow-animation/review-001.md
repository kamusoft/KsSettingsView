# レビュー結果: ios-customcell-initial-height-grow-animation (001 回目)

**日付**: 2026-09-18
**判定**: APPROVED

## サマリー

合意済みスコープ (exploration.md の「決定事項」「実装で残る点」と maui/ADR-0028、指揮側の追加決定) に対して、実装は素直に一致している。CustomCell は Bridge の representable が提示された幅で内容に問い直す形、view accessory は wrapper が自分の幅を持たない間だけ親の幅で測る形、計測の控えは `MeasureInvalidated` で捨てる形で、却下された保険 B は入っていない。ビルド・テストはすべて成功し、実機 A/B ログと Simulator の突き合わせが「初回の答えが定常値と一致するようになった」「内容の変化への追従は保たれている」という主張を裏付けている。

指摘は Minor 4 件・Suggestion 2 件で、いずれも振る舞いの誤りではなく、境界をまたぐ約束事の書き方・テストの回帰検出力・手順の記録に関するもの。Critical / Major はない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| handbook/cross/comment-policy.md | 常時 (always) |
| handbook/cross/test-execution.md | テストを実行し結果を報告する |
| handbook/cross/runtime-behavior-verification.md | 実行時挙動 (アニメーション・フレームのタイミング) が絡む不具合の完了判定 |
| handbook/ios/swift6-language-mode-check.md | `ios/Sources/` を触る変更の完了判定 |
| handbook/maui/integration-host-verification.md | `maui/` の facade / platform 層に触れる (本変更は Sample 実機・Simulator での確認で代替されている。指摘 5 参照) |
| decisions/maui/0016・0018・0020・0028、decisions/ios/0002、concepts/maui/architecture/view-materialization.md | 変更が触る能力の既存決定 |
| lessons/code-review.md (L-001) | アサーションの回帰検出力が争点になる場面 |

## 検証したこと

- **iOS テスト** (Simulator iPhone 17 Pro、絞り込みなし全件): `** TEST SUCCEEDED **`。バンドル集計行の合算で **1053 tests / 0 failures** (Bridge 169 / Core 88 / SwiftUI 98 / TestSupport 7 / UI 691)。新規 3 件を含む
- **MAUI facade テスト**: **589 tests / 0 failures**
- **MAUI iOS TFM のビルド** (`net10.0-ios`, Debug): 成功・**0 警告 / 0 エラー**。`InvalidateConstraintsCache()` が `protected` として届くこと、`nfloat.PositiveInfinity` が通ることを実ビルドで確認
- **lint**: `local-path-lint.py` / `identity-lint.py` 検出 0 件。`comment-policy-lint.py` 禁止 0 件 (要確認 170 件はすべて本変更外の既存 sample 群)
- **作業ツリーの清浄**: 一時観測ログ (`Console.WriteLine` / `KSPROBE`) の残留なし。`ios/Package.swift` と `samples/` に差分なし
- **MAUI 本体の契約** (Microsoft.Maui.Controls 10.0.70 の `MauiView` を逆コンパイルして確認):
  - `SizeThatFits` は `IsMeasureValid` + `_lastMeasuredSize` を**参照する**ので、`MeasureInvalidated` 時に控えを捨てないと古い高さを返す — ADR-0028 とコードコメントの主張は正しい
  - この版の `MauiView` は `SetNeedsLayout` を override していない (= `SetNeedsLayout()` だけでは控えは消えない)。よって追加した `InvalidateConstraintsCache()` は冗長ではなく必要
  - `IPlatformMeasureInvalidationController.InvalidateMeasure(bool)` は明示的実装で、`_safeAreaInvalidated` の立て直しと祖先への伝播判断を伴う。内容変化では safe area は変わらず、外向きの通知は `_measureInvalidated()` 側が担っているので、**`InvalidateConstraintsCache()` + `InvalidateIntrinsicContentSize()` + `SetNeedsLayout()` の選択は妥当** (依頼された観点 (b) への答え)
- **証跡** (観点 (e)): `evidence/probe-1b-after-device-*.log` は修正前ログと同じ形式・同じ端末・同じ 2 画面で取られ、CustomCell 92.5→167 の 2 段階が 167 の 1 段階に、Footer 38.5→67 が 67 の 1 段階になっている。展開・折りたたみで 39.5 ↔ 167 が往復しており、控えの破棄の直接の裏取りになっている。Simulator の突き合わせ表は全フレーム垂直ずれ 0px で、退行がないことの根拠として十分。静止画 3 枚 × 2 画面を実見し、README の記述 (t0000ms で内容が在り、③ が折り返し後の高さで確保されている) と一致することを確認した。端末名・署名・UDID はプレースホルダ化されており、動画は含まれていない (evidence 規約に適合)
- **公開面**: `ios/Sources` / `maui/` とも公開 API に変更なし。ADR ID を含むコメントはいずれも `internal` 宣言に付いており、公開 doc コメントへの内部用語の混入はない (観点 (f))

## 指摘事項

### [🟡 Minor] 「高さの制約を提示しない」意図が 2 ファイルに割れていて、どちらを読んでも真意が取れない

**該当箇所**: `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentView.swift:52-55` / `maui/KsSettingsView.Maui/Platforms/iOS/KsAccessoryHostView.cs:64-69`

**問題点**: Bridge 側は `height: .greatestFiniteMagnitude` を渡しながら「高さは view が決めるので、制約としては提示しない」と書いている。実際にはこれは有限の巨大な制約であり、「提示していない」状態にしているのは C# 側の `SizeThatFits` override が高さを `+∞` へ書き換えているからである。MAUI の計測では有限の巨大高と `+∞` は同じではなく (star 指定の行を含む内容では前者が巨大な希望高を返し得る)、この正規化は実際に効いている。つまり Swift 側のコメントは単独で読むと誤った理解を与え、C# 側の override は「Bridge が無限大を表現できないから在る」という存在理由がどこにも書かれていない。maui/ADR-0028 の決定文が `sizeThatFits(幅, ∞)` と書いているのに実装が有限値を渡している点も、ADR だけを読んだ人には追えない。

**推奨修正**: 次のどちらかに寄せる。(1) Bridge から `height: .infinity` を渡し、C# 側の override は「どの高さ提案でも内容の希望高を答える」という現在の役割の説明だけにする。(2) 有限値を渡すままにするなら、Swift 側のコメントを「制約としては提示しない」ではなく「高さは上限なしとして渡す」に改め、C# 側 override の remarks に「高さの提案は上限なしとして解釈する (有限の巨大値も同じ扱いにする)」と、なぜ正規化が要るかを明記する。

### [🟡 Minor] 高さを答える経路と変化を検知する経路が別 API になり、その前提がどこにも書かれていない

**該当箇所**: `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentView.swift:53` と `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentHostView.swift:87,189-193`

**問題点**: 行の高さとして上位へ返す値は `content.sizeThatFits(幅, …)` になったが、入れ物 (`KsBridgeCellContentHostView`) が「内容の高さが変わった」と判断する材料は `content.intrinsicContentSize.height` のままである。実際の内容は常に `KsAccessoryHostView` で両方が同じ計測を通るため現状の実害はないが、(a) 初回は `store()` が幅なしの `intrinsicContentSize` を控えるため、representable の答えが最初から正しくても入れ物は必ず 1 回「変化した」と判定して余分な測り直しを起こす、(b) `sizeThatFits` だけに答える内容は行の高さは付くが以後の変化に追従しない、(c) `intrinsicContentSize` だけに答える内容 (既存テストの `ProbeContentView` がこの形) は `UIView` 既定の `sizeThatFits` が `bounds.size` を返すため、初回は 0 → `nil` で既定の測り方に落ちる、という非対称が残る。入れ物側の doc コメントは今も「内容は自分で必要な高さを答える view」とだけ書いており、両方に一貫して答える必要があることを伝えていない。

**推奨修正**: 変化の検知も representable が使うのと同じ測り方に合わせる (`content.sizeThatFits(自分の幅, 上限なし).height` を控える) か、少なくとも `KsBridgeCellContentHostView` の型コメントに「内容は `intrinsicContentSize` と `sizeThatFits` の両方で同じ高さを答える必要がある」という前提を明記する。

### [🟡 Minor] `_measuredWidth` の説明が実際の値と食い違うようになった

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/KsAccessoryHostView.cs:29-30,56-58,82-91,97-107`

**問題点**: `_measuredWidth` の doc コメントは「直近に必要サイズを求めたときの幅」だが、親の幅で測った場合の実際の値は `Bounds.Width` (= 0) であり、測るのに使った幅 (親の幅) ではない。その結果 `LayoutSubviews` の「幅が変わった」判定は、答えが変わらないと分かっている場面でも必ず発火する (実機ログの Footer 側「幅の変化 前=0 後=414 → 必要サイズを無効化」がそれ)。実害は余分な無効化 1 往復だけだが、フィールドの意味と使い方が読み取れなくなっており、後から「なぜ同じ答えなのに無効化されるのか」を追う人を迷わせる。

**推奨修正**: doc コメントを実態に合わせる (「直近に必要サイズを問われたときの自分の幅」) か、測るのに使った幅を別に控えて `LayoutSubviews` の比較対象をそちらにする。あわせて `ResolveWidthConstraint` が `_measuredWidth` ではなく `Bounds.Width` を直接見る形にすると、getter の副作用への依存も消える。

### [🟡 Minor] 追加テスト 3 件のうち、修正の芯を固定しているのは 1 件だけ

**該当箇所**: `ios/Tests/KsSettingsViewBridgeTests/KsBridgeCellContentWidthMeasurementTests.swift:90-105`

**問題点**: `test_折り返す内容の行は折り返した後の高さで作られる` が見ているのは**収束後**の行の高さであり、修正前の実装でも (1 レイアウトパス遅れて) 同じ 180 に収束するため、この変更に対する回帰検出力を持たない。`test_内容は最初の計測から行の幅で測られる` は幅付きの問い合わせが一度も起きないことを捕まえるので検出力がある (修正前は `queriedWidths` が空になる) が、maui/ADR-0028 の芯である「**最初の答えが定常値と同じ**」= 行の高さが後から変わらないことを直接固定しているテストはない。L-001 の趣旨からも、この 1 件は「通っても何も守っていない」状態に近い。

**推奨修正**: 観測用 view に「答えた高さ」も順に控えさせ、最初に答えた高さが折り返し後の高さと一致することを確かめる形にする (修正前は 1 件も答えないので落ちる)。あるいは、この 1 件は収束後の見張りだと分かる名前・コメントにして、芯の固定は別の 1 件で行う。

### [🟡 Minor] Swift 6 言語モードの一時設定ビルドの結果が変更アーティファクトに残っていない

**該当箇所**: `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentView.swift` (変更全体) / `evidence/README.md`

**問題点**: handbook/ios/swift6-language-mode-check.md は `ios/Sources/**` を触る変更の**完了判定の条件**として、Swift 6 言語モードの一時設定ビルドで error 0 件を確認することを求めている。`ios/Package.swift` に一時設定の残留がないことは確認できたが、確認を行ったかどうかは exploration.md・deviation.md・evidence/README.md のいずれにも記録がない。今回の差分は concurrency に触れないため実質的なリスクは低いが、規約の到達状態としては未確認である。

**推奨修正**: 実装者の完了報告に結果が含まれているかを指揮側で確認する。含まれていなければ実施して結果を残す (S 級でも完了条件は変わらない)。

### [🔵 Suggestion] 幅付きの問い合わせと配置で控えの鍵が毎回入れ替わり、レイアウトパスごとに計測が 1 回増える

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/KsAccessoryHostView.cs:64-69`

**問題点**: `SizeThatFits` は控えを `(幅, +∞)` の組で残し、`MauiView.LayoutSubviews` は `(幅, 実際の高さ)` で妥当性を見て測り直して控え直す。このため両者は毎回互いの控えを外し、1 レイアウトパスにつき `CrossPlatformMeasure` が 1 回余分に走る。正しさの問題ではないが、CustomCell の内容が重い画面ではスクロール中に効いてくる可能性がある (handbook/maui/performance-verification.md の関心事)。

**推奨修正**: 今回のスコープで手を入れる必要はない。描画性能の計測を行う機会があれば、この経路を観測対象に含めておくとよい。

### [🔵 Suggestion] 高さ 0 の内容の扱いが「潰れた行」から「既定の行の高さ」へ変わっている

**該当箇所**: `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentView.swift:56-58`

**問題点**: 修正前は `intrinsicContentSize.height` が 0 の内容に対して高さ 0 を返していたが、修正後は `nil` を返して SwiftUI の既定の測り方に落ちるため、行は既定の高さを持つ。新規テスト `test_高さを答えない内容の行は既定の行の高さで作られる` がこの振る舞いを意図として固定しているので誤りではないが、「内容が畳まれて高さ 0 になる」使い方をしていた場合は見え方が変わる。

**推奨修正**: そのままでよい。合意済みスコープ外なので、利用者向けの説明が要るかどうかだけ蒸留時に一度見ておくと安全。

## アクションプラン

1. (確認) Swift 6 言語モード一時設定ビルドの結果が完了報告に含まれているかを確かめる。無ければ実施する — 指摘 5
2. コメントの手当て 2 件を入れる — 指摘 1 (高さ制約の意図が 2 ファイルに割れている) と指摘 3 (`_measuredWidth` の説明)。どちらも数行で、振る舞いは変わらない
3. 追加テストの検出力を上げる — 指摘 4。実装者自身がミューテーションで 1 件は検出しないと確認しているので、その 1 件の位置づけを直す
4. 入れ物側の前提を明記するか、検知の測り方を揃える — 指摘 2。現状の実害はないため、2・3 と同じ回で入れられれば十分
5. Suggestion 2 件は着手不要 (蒸留時の目配りのみ)

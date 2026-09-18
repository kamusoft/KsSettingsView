# レビュー結果: ios-customcell-initial-height-grow-animation (002 回目)

**日付**: 2026-09-18
**判定**: APPROVED

## サマリー

前回の Minor 4 件はすべて閉じている。境界をまたぐ約束事 (高さの制約を「上限なし」として扱うこと、内容に求める計測契約) は Swift 側・C# 側の双方に自己完結する形で書かれ、`_measuredWidth` の説明は実態に合い、`ResolveWidthConstraint` は `Bounds.Width` を直接見る形になった。追加テストは芯を固定する 1 件が新設され、収束後の見張りは役割が分かる名前とコメントになっている。

回帰検出力は実測で確かめた。リポジトリ外の一時複製へ旧実装 (`intrinsicContentSize` を中継する形) を戻して Bridge テストを回すと、新設の `test_内容が答える高さは最初から折り返した後の高さで変わらない` と `test_内容は最初の計測から行の幅で測られる` の 2 件が落ち、`test_収束後の行の高さが折り返した後の高さになる` は通る (コメントが宣言しているとおりの分担)。作業ツリーは読み取りのみで触っていない。

差分全体を改めて見た範囲でも振る舞いの誤りは見つからなかった。指摘は Suggestion 2 件のみで、いずれも着手不要。Critical / Major / Minor はない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| handbook/cross/comment-policy.md | 常時 (always) |
| handbook/cross/test-execution.md | テストを実行し結果を報告する |
| handbook/cross/runtime-behavior-verification.md | 実行時挙動 (フレームのタイミング・アニメーション) が絡む不具合の完了判定 |
| handbook/ios/swift6-language-mode-check.md | `ios/Sources/` を触る変更の完了判定 |
| handbook/maui/integration-host-verification.md | `maui/` の platform 層に触れる (実機・Simulator の確認で代替。前回確認済み) |
| decisions/maui/0016・0020・0028、decisions/ios/0002、concepts/maui/architecture/view-materialization.md | 変更が触る能力の既存決定 |
| lessons/code-review.md (L-001) | アサーションの回帰検出力が争点 — ミューテーションで実測した (下記) |

## 検証したこと

- **iOS Bridge テスト** (Simulator iPhone 17): `** TEST SUCCEEDED **`、バンドル集計行で **170 tests / 0 failures** (前回 169 + 新設 1)
- **ミューテーションによる検出力の実測** (L-001): リポジトリ外へ `Package.swift` / `Sources` / `Tests` を複製し、複製側の `KsBridgeCellContentView.sizeThatFits` だけを旧実装へ戻して `KsBridgeCellContentWidthMeasurementTests` を実行 → **4 tests / 2 failures**。落ちたのは `test_内容が答える高さは最初から折り返した後の高さで変わらない` と `test_内容は最初の計測から行の幅で測られる`、通ったのは `test_収束後の行の高さが折り返した後の高さになる` と `test_高さを答えない内容の行は既定の行の高さで作られる`。前回の指摘 4 が求めた「芯を固定するテストが修正前に落ちる」は成立している。作業ツリーのファイルは一切変更していない
- **MAUI iOS TFM のビルド** (`net10.0-ios`, Debug): 成功・**0 警告 / 0 エラー**
- **lint**: `comment-policy-lint.py` 禁止 0 件 (検査対象 809 ファイル)、`--advisory` の要確認は本変更外の Android ファイル 1 件のみ。`local-path-lint.py` / `identity-lint.py` とも検出 0 件
- **公開面**: 新設の `SizeThatFits` は `internal sealed class` の override であり、公開 API は増えていない。ADR ID を含むコメントはいずれも非公開宣言側に付いている
- **accessory 側の幅ヒントの妥当性**: 指揮側の追加決定「wrapper が `Bounds.Width` = 0 のとき superview の幅で測る」は、accessory の取り付けが `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2337-2352` で `leadingAnchor` / `trailingAnchor` を `contentView` に等値で留めている (水平の inset が無い) ため、superview の幅と最終的な自分の幅が一致する。幅を広く見積もって折り返し行数を少なく答える経路は無い
- **証跡**: `evidence/README.md` に前回の指摘 5 (Swift 6 言語モード一時設定ビルド、error 0 件・`ios/Package.swift` の原状復帰) とオーナーの実機目視結果が追記されており、handbook/ios/swift6-language-mode-check.md の到達状態を満たす。`evidence/` は静止画とログのみで、動画は含まれない

## 前回指摘の対処状況

| 前回の指摘 | 対処 | 判定 |
|---|---|---|
| 1. 高さの制約の意図が 2 ファイルに割れている | `KsBridgeCellContentView.swift:52-55` が「高さは上限なしとして渡し、上限なしは有限の最大値で表す。包む側がそれを上限なしとして解釈する」と書き、`KsAccessoryHostView.cs:64-70` が正規化の理由 (有限の高さ制約を通すと希望高が変わる) を書く形。前回の推奨 (2) に沿っている | 閉 |
| 2. 高さを答える経路と変化を検知する経路が別 API | `KsBridgeCellContentHostView.swift:28-31` に「内容には両方で同じ高さを答えることを求める。行の高さは前者で決め、変化は後者で検知する」と明記。前回の推奨の後段 (前提の明記) を採っている | 閉 |
| 3. `_measuredWidth` の説明が実態と食い違う | doc コメントを `KsAccessoryHostView.cs:29` で「直近に必要サイズを問われたときの自分の幅 (親の幅で測った場合も自分の幅を控える)」に改め、`ResolveWidthConstraint` (同 `:83-93`) は `Bounds.Width` を直接見る形になった | 閉 |
| 4. 芯を固定するテストが無い | `KsBridgeCellContentWidthMeasurementTests.swift:100-125` を新設、収束後の見張りは `:127` へ改名し役割をコメント化。ミューテーション実測で前者が修正前に落ちることを確認 | 閉 |
| 5. Swift 6 言語モード確認の記録が無い | `evidence/README.md` 末尾に結果 (error 0 件・設定の復帰確認) が記録された | 閉 |

## 指摘事項

### [🔵 Suggestion] 芯を固定するテストの 2 つのアサーションは、観測用 view の作りから必ず成立する

**該当箇所**: `ios/Tests/KsSettingsViewBridgeTests/KsBridgeCellContentWidthMeasurementTests.swift:37-44,49-51,109-120`

**問題点**: `WrappingContentView` は有限かつ正の幅で問われたときだけ控えを足し、そのとき返す高さは `height(forWidth:)` の定義からつねに `wrappedHeight` になる。したがって `answeredHeights` に入る値は構造上 `wrappedHeight` だけで、「最初の答えが `wrappedHeight` である」「答えの集合が `{wrappedHeight}` である」の 2 つは控えが空でない限り必ず通る。実際に落ちる条件は `guard let first = content.answeredHeights.first` (= 幅付きの問い合わせが一度も起きない) だけで、これは `test_内容は最初の計測から行の幅で測られる` が捕まえる条件と同じである。ミューテーション実測で 2 件とも落ちたのはこの共通の条件による。誤った幅で問い合わせる形の退行 (たとえば行の幅ではない固定幅で問う) は幅のテストだけが捕まえ、高さの並びのテストは通る。

**推奨修正**: 今回のスコープで手を入れる必要はない (前回の推奨どおりの形であり、修正前に落ちることは実測済み)。将来この観測用 view に触れる機会があれば、答える高さを幅の値に依存させる (たとえば幅から行数を割り出して高さを決める) と、高さの並びのアサーションが幅のアサーションと独立に落ちるようになる。

### [🔵 Suggestion] 新しく書いた「内容に求める前提」を守る観測用 view がリポジトリに無い

**該当箇所**: `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentHostView.swift:28-31` / `ios/Tests/KsSettingsViewBridgeTests/KsBridgeCustomCellTests.swift:24-55` / 同 `KsBridgeCellContentWidthMeasurementTests.swift:18-52`

**問題点**: 入れ物の型コメントは内容に「`sizeThatFits` と `intrinsicContentSize` の両方で同じ高さを答える」ことを求めるようになったが、テストの観測用 view はどれもこの前提を満たしていない (`ProbeContentView` は `intrinsicContentSize` だけ、`WrappingContentView` は幅を持たない間の `intrinsicContentSize` が折り返さない高さを答える)。前提を満たすのは実物の wrapper (`KsAccessoryHostView`) だけで、テストからは前提が守られているかを見張れていない。現状はどのテストも前提を要求する経路を検査していないため実害はない。

**推奨修正**: そのままでよい。前提の側 (入れ物) を変えるときは、この非対称が残っていることを思い出せるよう、蒸留で concepts へ渡すときに一文を添えておくと安全。

## アクションプラン

1. 着手すべき指摘は無い。前回の Minor 4 件は閉じており、ビルド・テスト・lint・証跡のいずれも到達状態を満たす
2. Suggestion 2 件は蒸留時の目配りのみ (今回のスコープでは対処しない)

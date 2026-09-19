# レビュー結果: ios-swiftui-representable-safe-area (001 回目)

**日付**: 2026-09-19
**判定**: APPROVED

## サマリー

デルタスペックの 2 能力・6 Scenario はいずれも実装または証跡で満たされており、無断の仕様逸脱・虚偽チェック・テスト失敗は無い。実装は `KsSettingsView.body` の 1 箇所で両バックエンドに同じ修飾を掛ける形に収まっており、ADR-0006 の「`.container` だけを無視して keyboard 領域は残す」「尊重側は辺を空集合にして View identity を保つ」という要点がコード上でもコメント上でも正しく表現されている。指摘は Minor 2 件・Suggestion 3 件で、いずれもブロッキングではない (主眼の既定配置に自動回帰テストが無く担保が証跡スクリーンショットのみ、という点が最も重い)。

## 照合した規約

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `handbook/cross/comment-policy.md` | 常時 (always) | 適合 |
| `handbook/cross/test-execution.md` | テスト実行・テスト結果の報告 | 適合 |
| `handbook/cross/runtime-behavior-verification.md` | 実行時にしか現れない挙動 (セーフエリア配置) の完了判定 | 適合 |
| `handbook/cross/sample-parity.md` | `samples/**` を触る | 適合 |
| `handbook/ios/swift6-language-mode-check.md` | `ios/Sources/**` を触る変更の完了判定 | 適合 |

index を読んだうえで非該当と判定した文書: `cross/public-identifiers.md` (`ios/Package.swift` に差分なし) / `cross/diagnostic-message-language.md` (開発者向け文字列の追加なし) / `cross/aiforms-origin-reference.md` / `cross/user-skill-api-listing.md` / `cross/install-examples.md` (`skills/` `README` を触らない) / `cross/local-development-setup.md` / `cross/release-procedure.md` (guide)。

lessons: `lessons/code-review.md` L-001 (ミューテーションによる検出力の実測) は本レビューでは静的読解だけで結論が出る (下記 Minor-1 は「テストが `body` を一度も評価しない」というコード上の事実であり判断の余地が無い) ため、読み取り専用の制約を優先してミューテーションは行っていない。`lessons/process.md` L-003 (視覚証跡) / L-005 (到達可能な修正) / L-009 (付随修正の記録) を判定に用いた。

## 自分で実行した確認

- **テスト** (`handbook/cross/test-execution.md` の Simulator 実行): iPhone 17 Pro / iOS 26.1、`xcodebuild test -scheme KsSettingsView`。バンドル集計行の合算で **1,095 tests / 0 failures** (Bridge 173 / Core 88 / SwiftUI 105 / TestSupport 7 / UI 722)。`RootSafeAreaModifierTests` の 7 件も実行・成功を確認した。
- **Swift 6 言語モード**: 読み取り専用の制約により `ios/Package.swift` は編集せず、`xcodebuild build -scheme KsSettingsView ... SWIFT_VERSION=6` で同等の確認を行った。`-swift-version 6` が 9 コンパイル呼び出しに適用され **BUILD SUCCEEDED / error 0 件**。`ios/Package.swift` に差分が無いことも確認した (tasks 4.2 の到達状態を満たす)。
- **comment-policy lint**: `python3 scripts/comment-policy-lint.py --advisory` を全体に実行し、**禁止 0 件**。本 change が触った 6 ファイルには advisory も出ていない。公開 doc コメント (`respectsSafeArea(_:)`) に内部用語 (ADR ID・change 名等) が無いこと、ADR 参照が非公開側 (`_respectsSafeArea` の説明と `body` 内の実装コメント) に置かれていることを本文の規約から判定した。
- **証跡画像**: `ui/references/` 4 枚と `ui/verification/` 11 枚をすべて開いて目視照合した (下端 420px を切り出しての A/B 比較を含む)。
- **deviation の裏取り**: `MinimalDiffableDemoView.swift` / `SectionDecorationSpikeView.swift` が `KsSettingsView` を通さず自前の `UIViewControllerRepresentable` をホストしていることをコードで確認し、deviation の理由が事実と一致することを確かめた。

## 指摘事項

### [🟡 Minor] 既定の全面配置そのものに自動回帰テストが無い

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:96-97` / `ios/Tests/KsSettingsViewSwiftUITests/RootSafeAreaModifierTests.swift`

**問題点**: 新設の 7 テストはすべて `_respectsSafeArea` の保持値だけを読んでおり、`body` を一度も評価しない。したがって `body` の

```swift
.ignoresSafeArea(.container, edges: _respectsSafeArea ? [] : .all)
```

を丸ごと削除しても 7 件すべてが green のまま通る。本変更の主眼 (既定で親の全面に広がること) に対する回帰検出力はゼロで、担保は `ui/verification/` の静止画だけになる。証跡は `handbook/cross/runtime-behavior-verification.md` が求める完了条件としては正しく満たされているが、証跡はアーカイブ後に再実行されないため、後続の refactor で 97 行目が落ちても誰も気づかない状態が残る。テストファイル冒頭のコメントもこの範囲限定を自認している。

**推奨修正**: `KsSettingsViewSwiftUITests` に、`UIWindow` + `UIHostingController` でホストして配置を実測するテストを 1 本足す。既定では hosted な `KsSettingsViewController.view` の frame が window の `safeAreaInsets` を跨ぐこと、`.respectsSafeArea()` では跨がないことを測れば、辺の指定の取り違えと modifier 消失の両方を捕まえられる。同種の window ホスティングの前例は `ios/Tests/KsSettingsViewUITests/` 側 (`SectionAccessoryRenderingTests.swift` 等) にあり、`KsSettingsViewSwiftUITests` 側には現状 1 件も無いので、新設になる。tasks 1.3 が指定した範囲は満たしているため本指摘はブロッキングにしない — 追加の是非はオーナー判断でよい。

### [🟡 Minor] Scenario「Store 方式と DSL 方式で配置が同じ」の証跡が spec の GIVEN と噛み合っていない

**該当箇所**: `ui/brief.md` の「照合結果 (2026-09-19)」/ `ui/verification/ios26-store-{top,bottom}.png` / `ui/verification/ios26-customcell-{top,bottom}.png`

**問題点**: spec の GIVEN は「同じ画面に Store 方式と DSL 方式をそれぞれ**全画面で**置いた 2 つの画面」で、THEN は「一覧の上端・下端の位置が両方式で一致する」。しかし iOS 26 の証跡では Store 側が `StoreDemoView` (`VStack` の中でボタン列の下に置く構成)、DSL 側が `CustomCellDemoView` (`NavigationStack` 直下に単独) で、そもそも一覧の上端位置が構成上一致し得ない 2 枚になっている。位置を比較できる同構成のペアは iOS 18 の `ios18-dsl-top.png` / `ios18-store-top.png` (どちらも `VStack` + ボタン列) だけで、これらは末尾スクロール側が無い。tasks 3.1 が挙げた「DSL 方式画面」を iOS 26 では `CustomCellDemoView` で代替している点も、brief には大タイトル畳み込みの文脈でしか書かれていない。

実装上は `body` の 1 箇所で両バックエンドに同じ修飾を掛ける構造なので配置が分岐する余地は無く、`test_DSL方式でもrespectsSafeAreaが保持される` が DSL 側の値保持も押さえている。よって欠陥ではなく**証跡の説明不足**として扱う。

**推奨修正**: (a) `DSLDemoView` と `StoreDemoView` の iOS 26 先頭表示ペアを 1 組撮り足すか、(b) brief の照合結果に「iOS 26 では同構成ペアを撮っておらず、`body` での共通適用という構造的同一性と iOS 18 の同構成ペアで代替した」と明記する。蒸留時に証跡の射程を誤読させないためのもので、実装変更は不要。

### [🔵 Suggestion] `respectsSafeArea()` の A/B が iOS 26 の 1 画面のみ

**該当箇所**: `ui/verification/ios26-customcell-respectssafearea-top.png`

**問題点**: `edges: []` が「modifier を掛けない」と同値であることは、この 1 枚の上端・下端の A/B でしっかり裏付けられている (下端の切り出し比較で、既定は下地が画面下端まで届き、`.respectsSafeArea()` では約 90px の別色帯が出ることを確認した)。ただし確認は iOS 26 のみで、iOS 18 系では空集合指定の同値性を撮っていない。tasks 3.4 の要求 (1 画面) は満たしている。

**推奨修正**: 必須ではない。依存側 (KsAppKMP) が iOS 18 でも opt-out を使う見込みがあるなら、SPM 更新後の確認項目に足しておくと安全。

### [🔵 Suggestion] DSL デモ画面の Root Footer が青帯で描かれている (本変更とは別系統の可能性)

**該当箇所**: `ui/verification/ios18-dsl-top.png` (画面下部の `© 2026 KsSettingsView Sample` 行)

**問題点**: `DSLDemoView` の `.rootFooter { Text(...).font(.caption) }` が、全幅の青背景の上に描かれている。セーフエリアの扱いと因果が結びつく要素が見当たらないため本変更の回帰とは考えにくいが、`distill.archive-media: delete` の方針で過去 change の DSL デモ証跡が残っておらず、本変更前との比較ができなかった (この判断の不確かさを明示しておく)。

**推奨修正**: 既知の挙動 (SwiftUI View を Root Footer に置いたときの背景の扱い) であれば無視してよい。未知であれば別 change として簡易起票の対象になり得る。本 change のスコープ内 (触ったファイル・本変更が直接開けた穴) には当たらないため、ここでは直さない判断でよい。

### [🔵 Suggestion] 逆流検査 (足場の書き換え確認) が機械的に実施できない

**該当箇所**: `kasane/changes/ios-swiftui-representable-safe-area/` 全体

**問題点**: change ディレクトリが丸ごと未追跡で、足場 (proposal / specs / tasks / ui) と実装差分が同一の未コミット状態にある。直近のコミット履歴を見ると本プロジェクトは提案段階で kasane の成果物を先にコミットする運用のため、今回は git 履歴からの逆流検査 (実装中に spec が書き換えられていないか) が成立しない。成果物の内容自体に逆流を疑わせる痕跡 (spec と実装の都合が良すぎる一致、deviation 無しの仕様変更) は見当たらない。

**推奨修正**: 運用上の所見。次回以降、提案確定時点で足場をコミットしておくと検査が成立する。

## アクションプラン

1. (任意) Minor-2: brief の照合結果に iOS 26 の同構成ペアが無い旨を 1 行追記する — 数分で閉じ、蒸留時の誤読を防ぐ
2. (任意・オーナー判断) Minor-1: `KsSettingsViewSwiftUITests` にホスティングによる配置テストを 1 本足すか、証跡のみで足りるとして見送るかを決める
3. (任意) Suggestion-4: DSL デモの Root Footer 青帯が既知かどうかだけ確認する

いずれも本変更の受け入れを妨げない。1〜3 を見送っても APPROVED の判定は変わらない。

---

# 一致検証 (ksn-verify 兼務)

M 級のためレビュアーが verify を兼務した。以下は品質評価とは独立の、デルタスペックと実装の機械的な突き合わせ。

**判定**: VALID

## 対応表: specs/settings-view-ios-swiftui

### Requirement: SwiftUI ラッパの既定配置は親の全面 (ADDED)

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| NavigationStack の中身として全画面で使うと一覧が画面下端まで描かれる | `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:96-97` (`.ignoresSafeArea(.container, edges: .all)`)、既定値は同 `:68` / `:84` | 自動テストなし。証跡 `ui/verification/ios26-customcell-{top,bottom}.png` / `ios18-customcell-{top,bottom}.png` / `ios26-store-{top,bottom}.png` / `ios18-{dsl,store}-top.png` — 下地が画面上端・下端まで途切れず、末尾 Cell と Section footer がホームインジケータ領域に隠れないことを目視で確認 | ✅ 一致 (自動テストの欠落は上記 Minor-1 として品質側で指摘。spec / tasks は自動テストを要求していない) |
| iOS 26 の大タイトルがスクロールで畳まれる | 同上 (スクロールビューがナビバー下まで伸びることによる副次効果) | 証跡 `ui/verification/ios26-customcell-scrolled-inline-title.png` — 大タイトルがインラインの `CustomCell デモ` へ畳まれ、内容がバーの下へ潜っている | ✅ 一致 |
| キーボード表示中は入力 Cell がキーボードに隠れない | `KsSettingsView.swift:97` が無視領域を `.container` に限定 (`.keyboard` を含めない) | 証跡 `ui/verification/ios26-inputcells-keyboard.png` — 最下部の EntryCell「署名」がキーボード上端の上に見えている | ✅ 一致 |
| Store 方式と DSL 方式で配置が同じ | `KsSettingsView.swift:96-97` が `backingContent` (`:100-123` の Store / DSL 分岐) の外側で一括適用 | `RootSafeAreaModifierTests.test_既定ではセーフエリアを尊重しない` (両方式の既定値)、`test_DSL方式でもrespectsSafeAreaが保持される`。証跡 `ui/verification/ios18-{dsl,store}-top.png` (同構成ペア) | ✅ 一致 (iOS 26 の同構成ペアは未撮影 — Minor-2) |

### Requirement: セーフエリアを尊重する側へ戻す Root modifier (ADDED)

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| modifier を付けると従来どおりセーフエリアの内側に収まる | `KsSettingsView.swift:180-184` (`respectsSafeArea(_ respects: Bool = true)`)、`:97` の `edges: []` 分岐 | `RootSafeAreaModifierTests.test_respectsSafeAreaは引数を省略するとtrueになる`。証跡 `ui/verification/ios26-customcell-respectssafearea-top.png` — 既定版と同一スクロール位置で上端の下地が消え、下端に別色の帯が出る | ✅ 一致 |
| false を渡すと既定の全面配置になる | 同 `:180-184` | `RootSafeAreaModifierTests.test_respectsSafeAreaにfalseを渡すと既定の全面配置に戻る` | ✅ 一致 |
| 他の Root modifier と連鎖しても互いの値を失わない | 同 `:180-184` (copy 返却。`style` / `theme` / `rootHeader` / `rootFooter` と同形) | `test_他のRootModifierと連鎖しても互いの値を失わない` (順序・逆順の 2 通り)、`test_respectsSafeAreaはrootFooterとthemeの値を保つ`、`test_respectsSafeAreaはcopyを返し元の値を変えない` | ✅ 一致 |

SHALL 条項の個別確認: 「Store 方式・DSL 方式のどちらでも」→ `body` での一括適用で成立。「ラッパ側で inset を加算しない」→ diff に inset を足すコードは無い。「keyboard 領域は無視の対象に含めない」→ `.container` 指定。「引数は Bool で既定値は `true`」→ `respectsSafeArea(_ respects: Bool = true)`。「元の値を変更せず copy を返す」→ `var copy = self` 形。「連鎖順序に依存しない」→ 独立フィールドへの代入のみ。

## 対応表: specs/samples-ios

### Requirement: iOS Sample のデモ画面はラッパの既定配置で bar の後ろまで回り込む (ADDED)

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 回避策を取り除いても見た目が変わらない | `samples/ios/KsSettingsViewSample/{BasicCells,CustomCell,DSLDemo,InputCells,StoreDemo}DemoView.swift` から `.ignoresSafeArea(.container, edges: .bottom)` を各 1 行削除 | 証跡 `ui/verification/` の各画像。`MinimalDiffableDemoView.swift:65` / `SectionDecorationSpikeView.swift:380` は残置 | ⚠️ deviation 記録済み (残置 2 画面。両ファイルが `KsSettingsView` を通さず自前の Representable をホストしていることをコードで確認済みで、Requirement の対象範囲「`KsSettingsView` を置く画面」の外) |
| Sample の文言と構成は不変 | 上記 5 ファイルの diff は削除行のみ (各 1 行) | `git diff` で文言・Section・Cell の追加変更が無いことを確認 | ✅ 一致 |

## 追加検査

- **tasks.md の虚偽チェック**: 全 13 タスクがチェック済み。対応表と突き合わせ、未実装のチェックは検出されなかった。0.1 (spike) は brief に画素差分 0 / 2,969,172 の記録あり。4.1 / 4.2 は本レビューで再実行して裏取り済み。4.3 の ADR 参照コメントは `KsSettingsView.swift:44` と `:93` に実在。tasks 2.1 の「7 画面」と実際の 5 画面の差は deviation に記録済み。
- **逆流検査**: change ディレクトリが未追跡のため git 履歴による機械的確認は不能 (上記 Suggestion として記録)。内容面で足場が実装に合わせて書き換えられた痕跡は見当たらない。
- **未記録乖離**: ❌ は 0 件。`git status` に現れる本 change の対象ファイルは、すべて tasks.md が名指しした範囲 (ソース 1 / テスト 1 / Sample 5) に収まっており、合意済みスコープ外のファイルへの付随修正は無い (`kasane/decisions/ios/` の 2 ファイルは探索フェーズの成果物でレビュー対象外)。
- **UI 変更の記録**: `ui/brief.md` に承認モックの扱い (HTML モックを作らず references を承認済みの正とする、オーナー確定 2026-09-19) と照合結果、合意済み妥協 0 件が記録されている。
- **テスト全件成功**: iPhone 17 Pro / iOS 26.1 で 1,095 tests / 0 failures を実行して確認。

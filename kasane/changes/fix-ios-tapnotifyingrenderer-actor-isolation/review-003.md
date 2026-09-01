# レビュー結果: fix-ios-tapnotifyingrenderer-actor-isolation (003 回目)

**日付**: 2026-09-01
**判定**: APPROVED

## サマリー

review-002 (APPROVED) 以降に入った 3 点の差分だけを対象に確認した。doc コメントの列挙は準拠 extension 11 件および `test_行タップ通知対応の11種はすべてTapNotifyingRendererとして解決できる` の型リストと 1 件のずれもなく一致しており、`didSelectItemAt` 側は集合を数え上げない書き方に変わって食い違いの余地自体が消えている。常に通っていた `XCTAssertFalse` の削除でテストの担保は落ちていない — 無効 Cell の契約は sentinel 色を前置きした収束待機 1 本が担っており、追記されたミューテーション実測でその検出力が実証されている。

新たな欠陥は認めない。実装の挙動に触れる差分は無く (変更はコメント・テストの 1 行削除・証跡追記のみ)、ソースのミューテーションは復元済みであることを `git diff` で確認した。指摘 0 件。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (書き換えた doc コメント 2 箇所の禁止参照・禁止類型・自己完結性を規約本文の各節と照合) |
| `kasane/handbook/cross/test-execution.md` | テスト実行・テスト結果の報告 (件数併記、収束を待つアサーションの 3 条件) |
| `kasane/handbook/cross/index.md` の他 rule | 該当なし (今回の差分は `samples/` ・`ios/Package.swift` ・`skills/` を触っていない) |

review-002 で照合済みの規約・ADR・concepts・lessons のうち、今回の差分が新たに触れる範囲は上記のみ。`kasane/decisions/` に本領域を縛る ADR が無いことは前回確認済みで、今回の差分はそれを変えない。

## 前回 Suggestion の解消状況

| 前回 | 判定 | 根拠 |
|---|---|---|
| Suggestion-1 doc コメントが準拠 5 種しか挙げていない | **解消** | `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2418-2419` が Command / Button / Checkbox / Radio / SimpleCheck / Picker / NumberPicker / TimePicker / DatePicker / Entry / Custom の 11 種を列挙し、同ファイル `:2426-2441` の準拠 extension 11 件および `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift:18-30` の型リストと完全に一致する。`grep` で `TapNotifyingRenderer` への準拠宣言が他ファイルに無いことも確認した。`didSelectItemAt` 側 (`:2402-2404`) は列挙を捨てて「行タップ通知に対応する CellView」という数え上げない記述になり、二重管理が 1 箇所に減っている |
| Suggestion-2 無効 Cell テストの常に通るアサーション | **解消** | `ios/Tests/KsSettingsViewUITests/KsCellViewSupportTests.swift:114-138`。末尾は `waitForBackgroundColor(normalColor, of: cell, in: collectionView)` 1 本になった。担保は落ちていない — 押下前に sentinel の `.cyan` を直接書き込んでいるため、(a) 無効 Cell に選択色が塗られれば `.magenta` のまま収束せず落ち、(b) handler がそもそも走らなければ `.cyan` のまま収束せず落ちる。deadline 超過時は実測値付きで `XCTFail` するため黙って通る経路が無い |
| Suggestion-3 `isEnabled` 側のミューテーション未実測 | **解消** | `evidence/mutation-check.md` の「追加実測: 有効判定側 (isEnabled)」。`if s.isEnabled && isPressed {` → `if isPressed {` で `test_無効Cellは押下しても選択色を塗らない` が落ち、往復テストは通過することを実測している (`Executed 6 tests, with 1 failure`)。件数 6 は `KsCellViewSupportTests` の `func test_` 実数 6 と一致する |

## 指摘事項

なし。

## アクションプラン

なし。本 change は蒸留 (ksn-distill) へ進める状態にある。

## 確認したが問題を認めなかった観点

- **コメント規約の各節との照合**: 書き換えた 2 箇所とも変更提案 ID・Phase / Round / Decision / レビュー通番・タスク通番・アーカイブ文書パス・拡張子なしの裸参照・delta spec キーワード (`MUST` 等) を含まない。時間軸の語 (「〜に変更」「旧〜」) も無く、現在形の仕様説明として自己完結している。参照している `CommandCellView` 等はすべてリポジトリ内のコード識別子であり、許容範囲。`python3 scripts/comment-policy-lint.py` は禁止 0 件 (685 ファイル) だが、規約が明記するとおり lint 0 件を適合の証明とはせず、履歴記述と change 配下パス参照は本文の類型から目視で判定した
- **列挙形式を選んだこと自体**: review-002 は「集合を数え上げない書き方」と「11 種を列挙」の両方を推奨修正として提示しており、後者を選んだのは提示済みの選択肢の範囲内。準拠 extension を足したときにコメント更新が要る点は残るが、`test_行タップ通知対応の11種...` が同じ集合を型リストとして持つため、追加時に更新が要る箇所はコメントとテストで対称になっている。前回オーナー側で許容された形であり、新たな指摘としては出さない
- **`didSelectItemAt` コメントの精度**: 「`tapHandler` プロパティに `onTap` / `onValueChanged` クロージャを保持している」という表現は、`CustomCellView` のように既定 `nil` の型を含む集合に対しても不正確ではない (直下の実装が `if let handler` で nil を素通しする形であり、コメントは保持の仕組みを説明している)。準拠 extension 直上の行コメント (`:2437` / `:2440-2442`) が Entry / Custom の個別事情を現在形で補っており、この 1 ファイルだけを読んで意味が通る
- **削除したアサーションが他の担保を兼ねていなかったか**: 削除された `XCTAssertFalse` は `selectedColor` との不一致を見るものだったが、`waitForBackgroundColor(normalColor, ...)` が成立する時点で背景は `.yellow` に確定するため、集合として見ても検証範囲は縮んでいない。sentinel 前置きにより「何も起きなかった」経路も落ちる
- **ミューテーション実測の妥当性**: 2 つの節が押下判定の 2 項 (`isPressed` / `isEnabled`) を 1 つずつ壊し、それぞれ対応するテストが 1 件だけ落ちることを示している。テスト件数 (`Executed 60 tests, with 2 failures` / `Executed 6 tests, with 1 failure`) が併記され、`kasane/handbook/cross/test-execution.md` の件数併記の規律を満たす。UITests は `#if canImport(UIKit)` ガード下にあり macOS の `swift test` では 0 件になるため、6 件が実行されている事実自体が Simulator 実行の裏付けになっている
- **ソースの復元**: `git diff ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` で `if s.isEnabled && isPressed {` が両項そろった形で残っていることを確認した。実測用のミューテーションが混入したままになっていない
- **証跡ファイルのパス形式**: `evidence/mutation-check.md` の追記部はリポジトリ相対パス (`ios/Sources/...`) のみで、ローカル絶対パスの混入なし。個体・個人を特定する値も含まない
- **足場凍結**: `proposal.md` / `specs/ios-host/spec.md` に差分なし (`git status` で `kasane/` の変更は `tasks.md` と新規の review / verify / evidence のみ)
- **テスト結果**: 差分反映後の Simulator 全件実行が `Executed 645 tests, with 0 failures` / `TEST SUCCEEDED`。review-002 時点と同件数であり、今回の差分がテストの増減を伴わない (アサーション 1 行の削除のみ) ことと整合する

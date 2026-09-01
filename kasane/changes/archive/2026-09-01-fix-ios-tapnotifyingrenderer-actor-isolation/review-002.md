# レビュー結果: fix-ios-tapnotifyingrenderer-actor-isolation (002 回目)

**日付**: 2026-09-01
**判定**: APPROVED

## サマリー

review-001 の指摘 6 件 (Major 1 / Minor 4 / Suggestion 1) はいずれも実質的に解消されている。中核だった押下解除テストは、window に載せた実 `UICollectionView` から `cellForItem(at:)` で取得した実 Cell に対し `isHighlighted` の true → false を往復させる形へ作り替えられ、待機は `kasane/handbook/cross/test-execution.md` の「収束を待つアサーション」3 条件をすべて満たしている。ミューテーション実測 (`evidence/mutation-check.md`) で「解除側を壊すと新テストだけが落ち、押下側のみを見る既存テストは通過する」ことまで示されており、代理経路だった前回の形から回帰検出力が実際に得られたことが確認できる。

修正によって新たに入った欠陥は認めない。実装コード (`KsCellViewSupport.swift` / `KsSettingsViewController.swift`) は前回から変わっておらず、追加されたのはコメント 2 行とテストのみで、挙動面の後退経路がない。テスト側で新設された `host` / `waitForFirstCell` / `waitForBackgroundColor` は既存テストの `host` + `pump` パターンより厳格 (固定秒待機ではなく条件待機、Cell 取得失敗時に実測値付きで fail) であり、テストターゲットの既存流儀とも整合する。645 tests / 0 failures、Swift 6 モード error 0 件、押下保持中の視覚証跡まで揃っている。

残る指摘は Suggestion 3 件のみで、いずれも本 change の契約に対する不足ではない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (コメント構文を持つ全ソース。追加コメント 2 行と既存 doc コメントの照合) |
| `kasane/handbook/cross/test-execution.md` | テスト実行・テスト結果の報告 (件数併記、収束を待つアサーションの 3 条件) |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 実行時挙動 (タッチフィードバックのタイミング) の完了判定と証跡 |
| `kasane/handbook/cross/public-identifiers.md` | 該当なし (`ios/Package.swift` は Swift 6 一時設定のみで復元済み・差分 0 件) |
| `kasane/handbook/cross/sample-parity.md` | 該当なし (`samples/` に差分なし。証跡撮影で起動したのみ) |
| `kasane/handbook/ios/` | 不在 (iOS ドメイン規約は未作成。proposal What Changes (4) で蒸留時に新設予定) |
| `kasane/lessons/code-review.md` L-001 | ミューテーションによる回帰検出力の実測 (Major-1 の解消判定に適用) |
| `kasane/lessons/test.md` L-001 | 実経路検証・往復遷移 (前回 Major-1 の根拠。今回の解消判定に適用) |
| `kasane/lessons/process.md` L-003 | 利用者可視の変更に対する視覚証跡 (前回 Minor-5 の根拠。今回の解消判定に適用) |
| `kasane/lessons/process.md` L-005 | 手の届く不備を別 change へ逃がさない (Suggestion-1 の位置づけ判断に適用) |
| skills (config `domain-skills.ios`): `swift-ui-impl-skill` | Swift Concurrency 観点 (`assumeIsolated` の適用範囲・Sendable 境界) |

`kasane/decisions/` に本 change の対象領域 (行タップ通知・Controller ティアダウン・Swift 言語モード) を縛る ADR は無い。`kasane/concepts/` にも `deinit` / `disconnectStore` の内部実装に言及する記述は無く、長命層の追随漏れは発生していない (前回レビューの確認を再確認)。

## 前回指摘の解消状況

| 前回 | 判定 | 根拠 |
|---|---|---|
| Major-1 押下解除テストが実経路を通らない | **解消** | `KsCellViewSupportTests.swift:93` が window に載せた実 `UICollectionView` から `cellForItem(at:)` で取得した実 Cell の `isHighlighted` true → false を往復させる。UIKit が `updateConfiguration` 経由で handler を呼ばなくなる回帰も、`installSelectedColorHandler` の呼び出し漏れも、押下色に到達しない時点で捕まる。ミューテーション実測 (`evidence/mutation-check.md`) で解除側の検出力を確認済み |
| Suggestion-7 `isEnabled == false` のケース | **解消** | `KsCellViewSupportTests.swift:114`。押下前に sentinel 色 (`.cyan`) を直接書き込んでから `isHighlighted = true` にしているため、「handler がそもそも走らなかった」場合は平常色へ収束せず fail する。空振りしない作りになっている |
| Minor-2 `assumeIsolated` の根拠がコードに残らない | **解消** | `KsCellViewSupport.swift:80-84`。Sendable 境界の理由 (handler 型が nonisolated / `UICellConfigurationState` が非 Sendable) と main actor とみなせる理由 (UIKit が main thread から呼ぶ) の 2 点が現在形・自己完結で書かれ、comment-policy の禁止参照・禁止類型に該当しない |
| Suggestion-6 MemoryLeakTests の名前と観測のずれ | **解消** | `test_Store経由でもControllerがdeinitされ解放後もStoreを操作できる` へ改名。観測していない「購読が解除される」を名前から外し、実際に assert している内容 (解放後の Store 操作結果) と一致した |
| Minor-3 tasks 2.3 の Scenario 参照 | **解消** | `tasks.md:12` が spec (`specs/ios-host/spec.md`) の表記 `押下中はハイライト色になり離すと平常時の背景に戻る` に戻っている。足場 (proposal / spec) は無変更 |
| Minor-4 残存 warning の記録 | **解消** | `evidence/swift6-build.txt` にユニークメッセージ一覧を追加。修正前ビルドは error 12 件で中断するため単純比較できない旨を明示したうえで、消えた 4 種と「修正後にだけ現れる」12 種を列挙し、後者がすべて本 change の diff が触っていないファイル / 行 (SectionBoxAttributes.swift は無変更、deprecated API は Non-Goal) であることを示している。`KsCellViewSupport.swift` に新規 warning が無いことがレビュー側で確認できる形になった |
| Minor-5 押下ハイライトの視覚証跡 | **解消** | `evidence/05-press-highlight.png` を実見。「プロフィール」行だけがアンバー系の押下色で塗られ、上下の CommandCell 行は塗られていない。`06-press-released.png` では同行が平常背景へ戻り、ヘッダの「最後にタップ: プロフィール」でタップ通知の発火も同時に写っている。撮影方法 (touch path 保持 + 別プロセスからの時間差キャプチャ) も `runtime-check.md` に記録済み。デモデータのみで個体・個人を特定する値の混入なし |

## 指摘事項

### [🔵 Suggestion] `TapNotifyingRenderer` の doc コメントが準拠 5 種しか挙げていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2420-2422` および `:2401-2405`

**問題点**: プロトコル定義の doc コメントと `didSelectItemAt` の doc コメントは、どちらも「CommandCell / ButtonCell / CheckboxCell / RadioCell / SimpleCheckCell の Renderer 群」とだけ書いている。しかし実際の準拠は 11 件 (`:2427-2442`) あり、本 change の spec は Requirement「行タップ通知とタッチフィードバックの挙動維持」で **11 種の全部**を契約として宣言した。つまりこのコメントは、本 change が新たに固定した契約より狭い集合を提示している状態になっている。準拠を減らす変更を検討する読み手が、この 5 種だけを守るべき範囲と読み違える余地がある。

コメント自体は本 change で新規に書かれたものではなく、`@MainActor` の 1 行を直上に足しただけである。したがって「悪化させた」指摘ではないが、comment-policy の適用契機 (既存コメントに触れる実装をするとき) には当たり、`kasane/lessons/process.md` L-005 の観点でも同じ宣言の直上を触っている以上、数行で閉じる範囲にある。

**推奨修正**: 両コメントの列挙を「タップで通知を出す CellView が準拠する」旨の説明に置き換えるか、11 種を列挙する。個別列挙を残す場合、準拠 extension を足すたびにコメント側の更新が要る点は変わらないため、集合を数え上げない書き方を推す。実装ではなくコメントのみの変更であり、本サイクルで直しても回帰リスクは無い。

### [🔵 Suggestion] 無効 Cell テストの最後のアサーションが常に通る

**該当箇所**: `ios/Tests/KsSettingsViewUITests/KsCellViewSupportTests.swift:135`

**問題点**: `XCTAssertFalse(cell.backgroundConfiguration?.backgroundColor?.isEqual(selectedColor) ?? false)` は、直前の `waitForBackgroundColor(normalColor, ...)` が成功していれば背景が `.yellow` であることが確定しているため、`.magenta` と一致することはあり得ず必ず通る。逆に待機が失敗した場合は `waitForBackgroundColor` 側が既に実測値付きで `XCTFail` しているうえ、その時点の色は sentinel の `.cyan` であり、やはりこのアサーションは通る。どちらの経路でも落ちないため、検出力はゼロである。

害はない (誤った緑を作るのではなく、単に何も見ていない) が、読み手には「selectedColor が塗られないことをここで確かめている」と見えるため、実際の担保がどこにあるか (= 直前の待機) を誤読させる。テスト内の実質的に無効なアサーションという意味で `kasane/lessons/code-review.md` L-001 が扱う類型に近い。

**推奨修正**: 削除して待機 1 本に寄せるか、待機の戻り値を `guard` で受けて「待機が成立した場合にのみ意味を持つ追加検証」であることを構造で示す。

### [🔵 Suggestion] 無効 Cell 分岐の回帰検出力はミューテーション未実測

**該当箇所**: `kasane/changes/fix-ios-tapnotifyingrenderer-actor-isolation/evidence/mutation-check.md`

**問題点**: 実測したミューテーションは `if s.isEnabled && isPressed` → `if s.isEnabled` の 1 種のみで、これは押下解除側を壊す変異である。無効 Cell テストが守る `isEnabled` 側 (`if s.isEnabled && isPressed` → `if isPressed`) は実測されていない。静的には、この変異で無効 Cell が押下色に塗られ `waitForBackgroundColor(normalColor)` が期限まで収束せず落ちると読め、sentinel 前置きも効いているため検出力があると判断できる。ただし `kasane/lessons/code-review.md` L-001 の趣旨は「読みではなく実測で固定する」ことにある。

本 change のスコープ内で新たに検出力の争点になっているわけではなく (Suggestion-7 対応として足された補助ケース)、既実測の 1 種で spec の Scenario に対応する経路は担保されているため、Suggestion に留める。

**推奨修正**: `if isPressed {` へ落とすミューテーションを 1 回だけ回し、`test_無効Cellは押下しても選択色を塗らない` が落ちることを `evidence/mutation-check.md` の表に 1 行足す。

## アクションプラン

いずれも本 change の完了を妨げない。着手するならこの順:

1. **Suggestion-1** — `TapNotifyingRenderer` と `didSelectItemAt` の doc コメントを、11 種の準拠と矛盾しない書き方へ直す (コメントのみ。回帰リスク無し)
2. **Suggestion-2** — `KsCellViewSupportTests.swift:135` の常に通るアサーションを整理する
3. **Suggestion-3** — `isEnabled` 側のミューテーションを 1 回実測して `mutation-check.md` に追記する

見送る場合も、いずれも次サイクル以降へ持ち越して差し支えない性質である。

## 確認したが問題を認めなかった観点

- **仕様充足**: spec の 6 Scenario すべてに実装・テスト・証跡の対応がある。Swift 6 ビルド = `evidence/swift6-build.txt` (error 0 / BUILD SUCCEEDED)、11 種解決 = `KsSettingsViewControllerTests.swift:17` (production の `didSelectItemAt` が `cellForItem` の戻り値を直接 `as? TapNotifyingRenderer` する形であり、テストが構築する `*CellView` 型集合と検査対象の型が一致していることを確認)、行タップ発火 = `CustomCellTests.swift` の `didSelectItemAt` 実経路 + `evidence/06-press-released.png` のヘッダ表示、押下往復 = `KsCellViewSupportTests.swift:93` + `evidence/05`/`06`、解放 2 件 = `MemoryLeakTests.swift`
- **tasks の虚偽チェック無し**: 4 節 10 タスクすべてを diff と evidence で照合。2.2 の無意味な条件 downcast は `guard let listCell` へ置換済み、3.1 の deinit は全削除済み (`Sources/` に `deinit` の実装は 1 件も残らない)、4.1 / 4.2 は evidence のログと一致
- **足場凍結**: `proposal.md` / `specs/ios-host/spec.md` は無変更 (`git diff` で確認)。`kasane/` の差分は `tasks.md` のチェック更新のみで、前回指摘した Scenario 参照の書き換えも復元済み
- **deviation**: 記録済みの tasks 3.2 の乖離 1 件は妥当。`themeSubscription` (`KsSettingsViewController.swift:268`) の doc コメントには元から deinit への言及がなく、購読 doc 3 件の更新で漏れはない (再確認)
- **収束待機の 3 条件**: `waitForFirstCell` (`:44`) / `waitForBackgroundColor` (`:68`) はいずれも (1) 実時間 deadline で区切り、(2) ループ内で `RunLoop.current.run(mode:before:)` により待機対象へ実行機会を譲り、(3) 超過時に実測値をメッセージへ載せて `XCTFail` する。既存の `pump(_:seconds:)` 型の固定秒待機を複製していない
- **テスト足場の副作用**: `host` は `window.makeKeyAndVisible()` + `defer { window.isHidden = true }` という、テストターゲット内 15 箇所の既存 `host` と同一の後始末で、この change だけが緩いということはない。`controller` は window の `rootViewController` が保持するため、`_` で受けても検証中に解放されない
- **`assumeIsolated` の適用範囲**: 実装コードは前回レビューから 1 行も変わっておらず (コメント 2 行の追加のみ)、`configurationUpdateHandler` の呼び出し元が UIKit の configuration 更新経路のみである点は再確認した。前回は実操作証跡が押下解除の trap 不在を間接的に示すだけだったが、今回は押下保持中のキャプチャ (`05`) が加わり、押下・解除の双方向で trap しないことが直接示されている
- **deinit 削除の安全性**: Store 購読は全 `[weak self]` (同ファイル 15 箇所)、`UICollectionView.dataSource` / `delegate` は UIKit 側 weak、`connectedStore` は weak。削除された旧コメントの前提 (「`UICollectionView` は `dataSource` を strong 参照で保持する」) は事実として誤りであり、削除は妥当 (前回の確認を再検証)。stale コメントの残存も無い
- **MemoryLeakTests の改名と assert 追加**: `store.insertCell(insertedCell, in: section.id, at: 1)` が `store.root.sections[0].id` を辿る形から事前に保持した `section.id` を使う形へ変わり、`removeCell` も `insertedCell` を直接指すようになった。Controller 解放前後の Store 状態 (件数と残存 Cell の id) を assert しており、名前 (`解放後もStoreを操作できる`) と観測が一致する
- **コメント規約**: 新規コメント 2 行 (`KsCellViewSupport.swift:80-84`) と改名済みテスト名は、変更提案 ID・タスク通番・レビュー通番・アーカイブ文書パス・delta spec キーワードのいずれも含まず、現在形で自己完結している。既存 doc の書き換え 3 件 (購読 Cancellable) も「deinit で cancel する」という実態と矛盾する記述の解消であり、履歴記述にはなっていない
- **テスト報告**: `Executed 645 tests, with 0 failures` / `TEST SUCCEEDED` が Simulator 全件実行の結果として `evidence/ios-test-all.txt` に記録され、指摘対応前の 644 件との差 (押下解除テスト 1 件を削除し 2 件を追加した純増 +1) とも整合する。`kasane/handbook/cross/test-execution.md` の件数併記の規律を満たす
- **証跡のパス形式**: `runtime-check.md` / `mutation-check.md` / `swift6-build.txt` の参照はリポジトリ相対または change 相対のみで、ローカル絶対パスの混入なし。画像は静止画のみで `evidence/` 配下に限定されている
- **Swift 6 残存 warning**: オーナー判断で本 change のスコープ外 (見送り) として扱い、指摘の根拠にしていない。spec の Requirement もエラーゼロのみを契約としている
- **条件ベース待機ヘルパの private 化**: `waitForBackgroundColor` 等は `KsCellViewSupportTests` の private であり、他ファイルから再利用できない。ただしテストターゲット全体の `pump` 置き換えは別 change (`fix-ios-test-pump-condition-wait`) が担うと `kasane/handbook/cross/test-execution.md` が明記しているため、本 change への指摘としない

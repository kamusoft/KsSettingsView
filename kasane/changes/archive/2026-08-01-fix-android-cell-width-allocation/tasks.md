# Tasks: fix-android-cell-width-allocation

## 1. 共通行の本体行再構築 (CellBaseLayout.kt)

- [x] 1.1 本体行 (title + 行内 trailing) を水平 LinearLayout として `CellBaseViews` に導入する。root ConstraintLayout / MinHeight 保証 / icon・accessory・hint の配置は維持する (→ Requirement: 共通行の主行幅配分)
- [x] 1.2 既定配分を実装する: title `0dp + weight=1` (末尾省略)、valueText `wrap_content` (singleLine + ellipsize END) (→ Requirement: 共通行の主行幅配分 / Scenario: valueText はコンテンツ幅で title が残り幅を占める)
- [x] 1.3 title+description の縦チェーン (packed 縦中央) と valueText↔title のベースライン揃えが入れ子化後も成立することを確認し、必要なら本体行内の整列手段で代替する (→ ADR-0002 検証事項)

## 2. EntryCell の行内移設 (EntryCellViewHolder.kt)

- [x] 2.1 EditText を accessoryHolder から本体行内へ移設し、weight を付け替える: title `wrap_content`、EditText `0dp + weight=1` (→ Requirement: Cell 級アクセサリと行内 trailing の 2 系統配置 / Scenario: EntryCell の入力フィールドは行内に置かれる (両 platform))
- [x] 2.2 `minWidth = 160dp` ハックを撤去する (→ Requirement: 共通行の主行幅配分 / Scenario: 入力フィールドの幅が固定の最低幅に依存しない)
- [x] 2.3 既存挙動 (IME payload 経路の差分ガード [android/ADR-0001]、フォーカス・タップで IME 表示、textAlignment、isPassword) が移設後も維持されることを確認する
- [x] 2.4 `ButtonCellViewHolder` を新しい View 階層へ追随させる: buttonStyleSet / normalLayoutSet の ConstraintSet 切替は titleView が root 直下である前提のため、本体行入れ子化後も「aux なしボタンスタイルの全幅・中央揃え」と「通常レイアウトへの復帰」が成立するよう修正する (→ Requirement: 共通行の主行幅配分 — 既存 Cell の回帰防止)

## 3. テスト

- [x] 3.1 幅配分の Robolectric テストを追加する: 固定した親幅のもとで、EntryCell 長文 / パスワード / 長 title (表示幅 = 主行幅 − title 幅、下限 0) / 行内 trailing なし / valueText 系 (コンテンツ幅・行幅超過の末尾省略) の各 Scenario を measuredWidth と左右境界で検証する (→ Requirement: 共通行の主行幅配分 の全 Scenario)
- [x] 3.2 本体行入れ子化後の整列を検証する: valueText↔title のベースライン揃え、title+description の縦中央配置 (→ ADR-0002 検証事項 / タスク 1.3)
- [x] 3.3 ButtonCell のボタンスタイル切替 (aux あり/なし) の幅・alignment をテストで検証する (→ タスク 2.4)
- [x] 3.4 既存の共通行・EntryCell テスト (ContentUpdatePayloadTest 等) を新構造へ追随させ、全テストが通ることを確認する

## 4. 視覚照合・証跡

- [x] 4.1 **実装着手前に**現行ビルドの before スクリーンショットを実機/エミュレータで取得し `ui/references/current-kssettingsview.png` として保存する (実装後は同一条件の before を取り直せないため)。原典期待は AiForms 版 Sample の再取得、不可なら承認済み mock を代替の正として brief.md に明記する
- [x] 4.2 実装スクリーンショットと mock/approved.png の視覚照合 (幅配分の配置関係) を行い、brief.md に照合結果を記録する
- [x] 4.3 実機で after を取得し `ui/verification/` に保存する
- [x] 4.4 基本 Cell + 入力 Cell 全種の一覧表示で視覚リグレッションがないことを確認する (accessory 系 Cell の配置・Picker 系の description とアクセサリの非重なりが不変であること)

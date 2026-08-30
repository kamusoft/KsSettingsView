# Tasks: fix-decoration-theme-not-applied-on-initial-bind

## 1. 再現の確立

- [x] 1.1 初期 Theme 付き `SettingsRootStore` を `bind` したとき `ItemDecoration` が既定 Theme のままであることを Robolectric で実測する。**attach 済み View への `bind`** と **attach 前の `bind` (Compose `AndroidView.factory` 相当)** の両経路を観測する (→ proposal「Why」)
- [x] 1.2 `ItemDecoration` が保持する Theme を観測する手段を確立する (`internalTheme` や背景色は `setRootDirect` が直接更新するため通過してしまう。**代理値ではなく ItemDecoration 自体**を見ること)

## 2. 設計判断 (2026-08-04 探索議論で決着済み)

- [x] 2.1 初期 Theme を `ItemDecoration` に届ける方式を決める → **A案確定: `setRootDirect` の末尾 (`internalTheme` 代入より後) に `applyDecoration(style)` を足す。ガードなし**。切り分けの根拠: `setRootDirect` が避けたいのは「`AsyncListDiffer` 在中の `submitList` と競合する Adapter 通知 (`notifyDataSetChanged` 多重呼び出し)」であり、`applyDecoration` は `removeItemDecoration` / `addItemDecoration` / `invalidateItemDecorations` のみで **Adapter 通知を一切発行しない**。ADR-0001 の payload 通知経路 (ViewHolder 維持契約) にも触れない
- [x] 2.2 Theme の同値スキップ (`themeBacking == value`) の撤廃 → **不要**。A案は同値スキップに手を触れずに初期適用の取りこぼしだけを塞ぐ
- [x] 2.3 級の M 引き上げ → **不要 (S 級のまま)**。ADR-0001 の通知設計に触れないため。ユーザー確認済み (A案確定と同時)

## 3. 実装

- [x] 3.1 初期 Theme が `ItemDecoration` に反映されるようにする。公開 API は変更しない
- [x] 3.2 `resyncFromStore` (再 attach 時の再取り込み) との整合を確認する。同経路は Theme を意図的に渡さない設計になっており、その理由 (同値スキップで `applyThemeInternal` が飛ぶ) は本件と同じ機構に由来する — 片方の修正がもう片方を壊さないこと

## 4. テスト

- [x] 4.1 退行テスト: 初期 Theme 付き Store の `bind` で `ItemDecoration` が初期 Theme になること (attach 前 / attach 後の両経路)
- [x] 4.2 既存の Theme 関連テスト (`ThemeTest`) と `AdapterReattachTest` の 5 件が引き続き green であること。とくに `detach 中の Theme 変更が再 attach 後に反映される` と `初回 attach での再適用は差分通知を出さない` は本件と同じ機構を固定しているため、両立を確認すること
- [x] 4.3 ADR-0001 が保証する ViewHolder 維持契約を壊していないこと (`ContentUpdate` 系のテストが green であること)
- [x] 4.4 追加テストが対象経路を実際に踏んでいることを変異注入で確認する (修正を外すとテストが落ちること。確認後は原状復帰)
- [x] 4.5 全体の回帰確認 (`./gradlew test --rerun-tasks`。実行件数まで確認する)

---

## 補足

- テスト実行時は `ANDROID_HOME=$HOME/Library/Developer/Xamarin/android-sdk-macosx` を環境変数で渡す (`local.properties` は作らない)。`android/` ディレクトリの `gradlew` を使う
- S 級のためデルタスペックはなく、verify は非適用。**独立文脈でのレビューは必須**
- 本 change は `fix-adapter-not-restored-on-reattach` の review-002 の Minor 指摘に由来する。実装前に同レビューの該当節を読むこと

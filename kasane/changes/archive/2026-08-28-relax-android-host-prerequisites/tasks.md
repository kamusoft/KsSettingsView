# Tasks: relax-android-host-prerequisites

## 1. スパイク (最初に実施 — lessons process L-004)

- [x] 1.1 ComposeView-in-ComponentDialog の実機確認: `ComponentActivity` ホストで ComponentDialog + ComposeView + Compose M3 DatePicker が表示・操作できること (ViewTree owner の供給を含む) (→ Requirement: ホスト前提に依存しないカレンダー選択面の提示)
- [x] 1.2 View インスタンス状態の経路確認: Activity 再生成が起きるホスト形態 (View 直置き / Compose `AndroidView`) ごとに、状態保存の成立条件 (安定 View id の付与・統合層での状態中継) と方式を確定する。現行コードは View に id を設定していない点に注意。MAUI 既定は in-place 構成変更のため「ダイアログが開いたまま生存」を確認する (→ Requirement: 構成変更をまたぐ表示継続)
- [ ] 1.3 **エスカレーション条件**: 1.1 が成立しない場合は実装を進めず設計 (Decision 4 の器) をオーナーと再協議する。1.2 で状態保存が不成立のホスト形態が出た場合は、その形態の縮退契約化 (spec 注記) をオーナーに確認してから先へ進む

## 2. テーマ同梱と常時ラップ (android-theming)

- [x] 2.1 `res/values/themes.xml` に internal Material3 派生テーマを同梱し、Context 変換ヘルパ (キャッシュ付き常時ラップ) を新設 (→ Requirement: ホストテーマ前提の撤廃)
- [x] 2.2 ViewHolder 生成とシート/ダイアログの2系統の Context 入口へラップを適用 (→ Requirement: ホストテーマ前提の撤廃 / 選択面のホストテーマ非依存)
- [x] 2.3 ButtonCell タイトル色の `colorPrimary` 動的解決を廃止し、固定の既定色へ統一 (→ Requirement: ホストテーマからの視覚隔離)
- [x] 2.4 テスト: 非 Material3 テーマ (素の AppCompat / 最小テーマ) での全 Cell 種生成・表示とボトムシート系選択面の提示・操作、ホストテーマ色の非漏出、ButtonCell 既定色の固定化。既存テストの `ContextThemeWrapper` 儀式を外せることの確認 (→ Requirement: ホストテーマ前提の撤廃 / ホストテーマからの視覚隔離 / 選択面のホストテーマ非依存)

## 3. TimeSelectionSheet (android-timepicker)

- [x] 3.1 `TimeSelectionSheet` 新設: 器・ヘッダー・スナップ静止・a11y は既存シート契約と同一。24h 2系列 / 12h 3系列 (format の `a` 判定)、Locale 由来の午前/午後ラベル (→ Requirement: ホスト前提に依存しない時刻選択面の提示 / 時制の決定と候補系列)
- [x] 3.2 `TimePickerCellViewHolder` の表示経路をシートへ切替 (`MaterialTimePicker` 経路の撤去) (→ Requirement: ホスト前提に依存しない時刻選択面の提示)
- [x] 3.3 テスト: ComponentActivity での提示・無効 Cell・12/24h 判定と初期選択・確定1回発火・非確定無発火・回転で閉じて無発火・タイトル解決 (→ 各 Requirement の全 Scenario)
- [x] 3.4 mock との視覚照合 (承認済み mock/approved 系と実機スクリーンショットの突き合わせ、証跡を change 配下へ保存 — lessons process L-003)

## 4. Compose カレンダーダイアログ (android-datepicker)

- [x] 4.1 ComponentDialog + ComposeView + M3 DatePicker の選択面実装: 4色ロールの配色写像・min/max (SelectableDates + 年範囲)・DisplayMode 切替・操作行 (取消/今日/確定) (→ Requirement: ホスト前提に依存しないカレンダー選択面の提示 / 範囲制限)
- [x] 4.2 今日ジャンプの状態操作実装 (非発火・範囲外セーフガード・冪等・非カレンダー表示からの復帰) (→ Requirement: 今日ジャンプ)
- [x] 4.3 `DatePickerCellViewHolder` の Material 経路をダイアログへ切替 (`MaterialDatePickerPresenter` の撤去)。Spinner 経路は不変 (→ Requirement: ホスト前提に依存しないカレンダー選択面の提示)
- [x] 4.4 テスト: ComponentActivity での提示・無効 Cell の非提示・タイトル解決 (pickerTitle → title)・確定1回/非確定無発火・範囲制限 (両モード)・範囲外初期値の丸め・年範囲既定・タイムゾーン非依存の日付往復・今日ジャンプ4性質・モード切替 (→ 各 Requirement の全 Scenario)
- [x] 4.5 mock との視覚照合と色ロール検証: カレンダー表示・テキスト入力・年選択・範囲外 disabled の各状態 × light/dark を対象に、design の色ロール対応表と突き合わせ (証跡保存 — lessons process L-003)
- [x] 4.6 ランドスケープでのダイアログ高さ制約とスクロール導入 (グループ5の実機観察で下端切れ・操作行の画面外落ちを確認 — 修正後にランドスケープの視覚証跡を再取得) (→ Requirement: ホスト前提に依存しないカレンダー選択面の提示)

## 5. 回転復元の自前化 (android-datepicker)

- [x] 5.1 `KsSettingsView.onSaveInstanceState` / `onRestoreInstanceState` 実装: 表示中カレンダーダイアログの (cell.id・選択日・表示月・表示モード) 保存と、attach 後の条件付き再提示 (→ Requirement: 構成変更をまたぐ復元)
- [x] 5.2 テスト: 回転をまたぐ状態維持・対応 Cell 不在時の非復元と非書き込み・復元後の配色/今日ジャンプ/確定契約の有効性 (→ Requirement: 構成変更をまたぐ復元)
- [x] 5.3 実機で回転前・遷移中・回転後の連番静止画を evidence/ に保存 (動画は撮らない — ksn-core ui-artifacts の媒体規約。lessons process L-003)

## 6. Fragment 依存機構の撤去

- [x] 6.1 `findFragmentManager` / `PickerRestoreRegistry` / `TimePickerColorizer` / `DatePickerColorizer` / `DatePickerTodayShortcut` / `MaterialDatePickerPresenter` / `PickerDialogTag` の削除と旧テスト (Colorizer / TodayShortcut / DialogRecreation 系) の整理 (→ REMOVED Requirements)
- [x] 6.2 `fragment-ktx` 直接依存の撤去 (android/ks-settingsview-ui/build.gradle.kts / binding csproj の明示参照) とビルド確認

## 7. サンプル・MAUI 整備

- [x] 7.1 samples/maui の MainActivity をテンプレート既定 (`Maui.SplashTheme`) へ復帰 (受け入れ条件) (→ Requirement: MAUI テンプレート既定テーマでの動作 Scenario)
- [x] 7.2 maui/tests の2ホスト (MauiHost / IntegrationHost.Android) の Material3 テーマ置換も既定へ復帰 (同一前提の隣接課題)
- [x] 7.3 samples/android の MainActivity を `ComponentActivity` + 非 Material3 テーマへ変更し、前提撤廃のデモとする (→ Requirement: ホストテーマ前提の撤廃)
- [x] 7.4 binding csproj の Xamarin.AndroidX.Compose.* 参照へ版整合コメントを追記 (Material 1.12 ピンと同型の規律)

## 8. 総合検証

- [x] 8.1 全ホスト形態 (android サンプル ComponentActivity / MAUI SplashTheme) での全 Cell 動作確認と証跡保存。MAUI は in-place 構成変更での選択面生存も確認 (→ Requirement: 構成変更をまたぐ表示継続)
- [x] 8.2 TimePicker / DatePicker の A/B 視覚証跡 (置換前後) の保存。テーマ土台差し替えの A/B (非 Material3 ホストでの表示、7.3 のサンプル ComponentActivity 化と合わせて) も含める (lessons process L-003)
- [x] 8.3 全件ビルド・テストの完了確認: android の全ユニットテスト (実行件数が想定どおりであることを確認) / samples/android のビルド / KsSettingsView.Binding.Android のビルド / MAUI サンプルと maui/tests 2ホストのビルド
- [ ] 8.4 concepts の追随は distill で実施 (date-picker-selection-surface.md の Material 器・android-native-host.md の前提2節・input-cells.md の配色契約) — 対象メモをここに残す

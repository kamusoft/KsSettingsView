# Proposal: android-datepicker-spinner-wheel

## Why

Android 版 DatePickerCell の `DatePickerUIStyle.Spinner` は、`AlertDialog` + `android.widget.DatePicker` + `calendarViewShown = false` の弱い実装で、Material テーマ環境では spinner 表示に切り替わらずカレンダーが表示されてしまう (土台ウィジェットの限界、android/ADR-0007 Context で既知)。意図したホイール UI として機能しておらず、オーナーはモダンホイールでの刷新を希望している。archive/android-numberpicker-modern-ui の proposal で「DatePicker ホイール版への展開は続編 change」と予告されていた変更にあたる。

あわせて、iOS の `DatePickerCell.todayText: String?` (「今日」ジャンプボタンのオプトイン) が Android に存在せず、プロパティパリティが欠落している。

## What Changes

- **選択 UI 刷新**: `DatePickerUIStyle.Spinner` の選択 UI を、ボトムシート + `KsWheelView` 3連 (年/月/日) に置き換える (android/ADR-0009)。器は `NumberSelectionSheet` と同系の構成 (ドラッグハンドル + ヘッダー [取消/タイトル/確定] + コンテンツ) の DatePicker 専用シートを新設する。候補表示文字列は端末 Locale から導出し (自前文字列なし)、ヘッダー操作色には既存の `androidButtonColor` を引き継ぐ (指定時最優先、未指定は accent 段階解決)
- **日付整合**: 年/月の変更に応じて日ホイールの候補数を動的に追随させ、範囲外になった選択日は月末日に丸める。`minDate` / `maxDate` をホイール候補の範囲制限として尊重する
- **todayText パリティ**: `DatePickerCell` (ui モジュール) と Compose DSL overload (`InputCellDsl.kt`) に `todayText: String? = null` を追加。指定時のみ「今日」ボタンを表示し、タップで3連ホイールを今日へスクロールする (今日が範囲外なら何もしない)。適用は Spinner モード限定 (Material では無視)
- **KsWheelView の内部拡張**: 指定 index へのプログラム的スクロールと、候補数の動的追随を可能にする内部 API を追加する (公開 API にはしない)

影響する能力: `settings-view-android-ui` (DatePickerCell Spinner モードの選択 UI)

## Non-Goals

- DatePickerCell の Material (カレンダー) モードの変更 (配色・ヘッダ補正は進行中の datepickercell-color-adjust の責務。Material への todayText 展開も対象外 — 必要になれば別 change)
- iOS / MAUI 側の変更 (iOS は既に todayText を持つ)
- `KsWheelView` の公開 API 化 (内部部品に留める)
- `NumberSelectionSheet` / `PickerSelectionSheet` の挙動変更

## Impact

- **公開 API**: `DatePickerCell` と Compose DSL overload に `todayText` 引数を追加 (挿入位置は iOS と同順の `uiStyle` 直後)。互換性の契約は**ソース互換** (named 引数・既定値前提) — data class の constructor / `copy` のシグネチャが変わるため **ABI 互換は保証対象外** (numberpicker 変更の `unit` と同じ契約)。`uiStyle` 以降を位置引数で渡している呼び出しはソース修正が必要になり得る (位置引数利用は保証対象外)
- **視覚変更**: Spinner モードの選択 UI がダイアログからボトムシートに変わる。挙動契約 (確定時のみ `onValueChanged` を1回発火、非確定 dismiss は発火しない) は `NumberSelectionSheet` と同一形に揃える
- **concepts への影響**: `todayText` のプラットフォーム差記載があれば解消される (蒸留時に追随)
- **リスク**: 3連ホイールの日数追随 (2月・30日月) と min/max 境界の作り込み品質。mock 承認と実機視覚照合・Scenario テストで担保する

## 級: M

公開 API 追加 + UI 刷新だが、触る範囲は DatePickerCell Spinner 系 + シート新設 + `KsWheelView` 内部拡張に閉じる (先行 android-numberpicker-modern-ui と同規模)。

domain: android

# android ADR 一覧

Android 系統 (android/ ビルドルート) 固有の決定。採番はこのドメイン内で 0001 から。

- [ADR-0001](0001-content-update-preserves-viewholder.md) — 内容更新は payload 付き通知と change アニメーション無効で同一 ViewHolder を維持する (accepted)
- [ADR-0002](0002-cell-row-width-allocation-linearlayout-weight.md) — 共通行の幅配分は AiForms 同型の水平 LinearLayout + weight で行い、EntryCell の入力欄は行内に置く (accepted)
  - 注記 (2026-08-22): 決定のうち「既定の配分は原典同型 (title が残り幅・valueText がコンテンツ幅)」は [core/ADR-0026](../core/0026-main-row-protects-title-truncates-value.md) が置き換える (title を守り valueText を省略)。LinearLayout + weight の構造と EntryCell の行内配置は本 ADR のまま有効
- [ADR-0003](0003-entrycell-enter-key-ime-action-done.md) — EntryCell の Enter キーは IME_ACTION_DONE で完了扱いにする (accepted)
- [ADR-0004](0004-cell-row-optical-vertical-centering.md) — Cell 行のテキストとアクセサリは幾何中央ではなく光学中央で揃える (accepted)
- [ADR-0005](0005-pickercell-selection-ui-bottom-sheet.md) — PickerCell の選択 UI はボトムシート (Material BottomSheetDialog) で表示する (accepted)
- [ADR-0006](0006-timepicker-dialog-runtime-coloring-via-view-traversal.md) — TimePicker ダイアログの動的配色は MaterialTimePicker を維持し表示後の内部 View 走査で行う (superseded by ADR-0018)
- [ADR-0007](0007-numberpickercell-bottom-sheet-custom-wheel.md) — NumberPickerCell の選択 UI はボトムシート + 自作ホイール (RecyclerView + SnapHelper) で実装する (accepted)
- [ADR-0008](0008-datepicker-dialog-coloring-and-header-fix-via-view-traversal.md) — DatePicker ダイアログの動的配色とヘッダ重なり補正は MaterialDatePicker を維持し表示後の内部 View 走査で行う (superseded by ADR-0019)
- [ADR-0009](0009-datepicker-spinner-bottom-sheet-triple-wheel.md) — DatePickerCell (Spinner) の選択 UI はボトムシート + 3連自作ホイールで実装する (accepted)
- [ADR-0010](0010-datepicker-today-jump-via-native-click-path.md) — DatePickerCell カレンダーの今日ジャンプは正規クリック経路への View 階層駆動で行い、リフレクションは採らない (superseded by ADR-0019)
- [ADR-0011](0011-picker-dialog-rotation-restore-container-driven.md) — Material ピッカーダイアログの回転復元はコンテナ駆動の完全復元とし、対応付けは cell.id で行う (superseded by ADR-0019)
- [ADR-0012](0012-full-update-content-sync-diffcallback-and-setrootdirect.md) — submitList 経路の内容取りこぼしは DiffCallback の Section H/F 内容比較と setRootDirect の一括 rebind で解消する (accepted)
- [ADR-0013](0013-resource-reference-via-declaring-library-r-class.md) — リソース参照は宣言元ライブラリの R クラス経由で行う (accepted)
- [ADR-0014](0014-entrycell-focused-editor-owns-text.md) — フォーカス中の EntryCell 入力欄は値の SSoT で、内容更新の text 反映はフォーカス喪失まで遅延する (accepted)
- [ADR-0015](0015-customcell-pool-aware-composition-disposal.md) — CustomCell の宣言 UI ホスティングは ReusableContent の deactivate+reuse でリサイクルする (accepted)
- [ADR-0016](0016-single-module-single-maven-artifact.md) — Android は core / ui / compose を単一 module に統合し `jp.kamusoft:kssettingsview` 1 artifact で配布する (accepted)
- [ADR-0017](0017-switchcell-state-colors-derived-from-accent.md) — SwitchCell の状態色はテーマ attr 直参照ではなく accent から導出する (accepted)
- [ADR-0018](0018-timepickercell-bottom-sheet-wheel-unification.md) — TimePickerCell の選択 UI は全ホストでボトムシート + 時分ホイールに統一する (accepted, supersedes 0006)
- [ADR-0019](0019-datepickercell-calendar-compose-datepicker.md) — DatePickerCell のカレンダー型 UI は Compose Material3 DatePicker のダイアログ表示に統一する (accepted, supersedes 0008/0010/0011)
- [ADR-0020](0020-bundled-theme-always-wrap-host-independent.md) — ライブラリ UI は同梱 Material3 派生テーマの常時ラップで生成し、ホストの XML テーマに依存しない (accepted)
- [ADR-0021](0021-calendar-dialog-restore-via-view-instance-state.md) — カレンダー選択面の回転復元は KsSettingsView の View インスタンス状態で自前化し、既定 ID の自前付与で成立させる (accepted)
- [ADR-0022](0022-explicit-api-strict-for-public-library.md) — Android 公開ライブラリは Explicit API Strict で公開境界を強制する (accepted)

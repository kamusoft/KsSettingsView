# Proposal: relax-android-host-prerequisites

## Why

ks-settingsview の Android 利用前提 2つ — 「ホストの XML テーマが `Theme.Material3.*` 派生であること」「ホスト Activity が `FragmentActivity` であること」— が、メインターゲットの Compose 専業アプリ (テンプレート標準は最小 XML テーマ + `ComponentActivity`) および MAUI テンプレート既定 (`Maui.SplashTheme` + `MauiAppCompatActivity`) と噛み合っていない。非 Material3 テーマでは MaterialSwitch / MaterialCheckBox が初期化例外で落ち、ComponentActivity では TimePickerCell / DatePickerCell (カレンダー) がタップしても何も起きない無言 no-op になる。オーナー判定により両前提の解消が必須 (rollout-user-skills のオーナー検収で発覚)。

方式は探索で決定済み (android/ADR-0018 / 0019 / 0020、いずれも accepted)。

## What Changes

- **テーマ非依存化** (capability: android-theming): Material3 派生テーマを res に同梱し、View 生成・シート/ダイアログ表示の Context を常時 `ContextThemeWrapper` でラップする (ADR-0020)。ホストテーマからの色引き継ぎは行わず、ButtonCell タイトル既定の `colorPrimary` 動的解決も廃止して固定既定色に統一 (色の正はライブラリの Theme / CellStyle に完全統一)。任意のホストテーマ (最小テーマ・AppCompat・SplashTheme) で全 Cell が動作する
- **TimePickerCell の選択 UI 置換** (capability: android-timepicker): `MaterialTimePicker` (時計ダイヤル / DialogFragment) を廃止し、時・分ホイールのボトムシートに全ホスト統一 (ADR-0018)。ホイールシートは新規 UI (モック承認対象)
- **DatePickerCell カレンダー型の置換** (capability: android-datepicker): `MaterialDatePicker` (DialogFragment) を廃止し、ComposeView を載せたダイアログで Compose Material3 `DatePicker` を表示 (ADR-0019)。配色は `DatePickerColors`、今日ジャンプは状態操作で再実装。ホイール型 (Spinner) は現状維持で、カレンダー/ホイール選択可の仕様を全ホストで保つ
- **FragmentActivity 依存の撤去**: `findFragmentManager()`・`PickerRestoreRegistry`・TimePicker/DatePicker Colorizer (FragmentLifecycleCallbacks) の削除。カレンダーダイアログの回転復元は View 状態ベースの自前機構で新設 (表示中状態の保存 + rememberSaveable)
- **MAUI サンプルのテーマ復帰**: samples/maui の MainActivity をテンプレート既定 (`Maui.SplashTheme`) へ戻す (rollout-user-skills 検収からの受け入れ条件)

## Non-Goals

- **skills/ と README 群の記述追従**: docs-refresh スキルの責務 (実装完了後にユーザーが明示依頼するフロー)
- **iOS・MAUI facade の公開 API 変更**: 本変更は Android 内部の表示経路とテーマ解決の変更であり、公開 API 契約 (core / facade) は不変。iOS 側の対応物にも触れない
- **シート系 (PickerCell / NumberPickerCell / DatePicker ホイール / 新 TimePicker シート) の回転復元の新設**: 現状仕様 (シートは復元対象外、回転で閉じる) を維持。理由: 本変更の目的はホスト前提の撤廃であり、シート復元は全シート共通の独立した設計判断を要する別能力 (TimePicker が復元対象から外れる挙動変化は ADR-0018 に記録済み)
- **ADR-0016 (単一 module 統合) の実施**: 別変更。本変更は現行の module 構成のまま行う
- **accent 既定のホスト `colorPrimary` 追従化**: 有望な将来案だが、既定の見た目が変わる挙動変更の追加になるためオーナー判断で本変更では見送り (2026-08-27)

## Impact

- **破壊的変更 (利用者可視)**: TimePickerCell の見た目・操作が変わる (時計ダイヤル → ホイールシート、キーボード入力モード喪失、回転時にシートが閉じる)。カレンダーの見た目の細部が変わる (M3 準拠同士でほぼ同一)。ホストテーマの色のライブラリ UI への反映が完全に消える (ButtonCell タイトル既定の `colorPrimary` 追従廃止を含む)
- **前提の撤廃 (利用者利得)**: `ComponentActivity` + 任意の XML テーマで全 Cell が動作。導入手順からテーマ・Activity 型の要求が消える
- **リスク**: Compose Material3 DatePicker は experimental API (版更新追随)。ComposeView-in-Dialog の ViewTree owner 自動設定は机上確定のみ — 実測スパイクを tasks 冒頭に置き、前提が覆ったらエスカレーション (lessons process L-004)。MAUI は Gradle 側 compose 版と Xamarin.AndroidX.Compose.* の版整合規律が必要
- **依存追加**: なし (compose material3 は ui module に既存、MAUI binding も配達済み)

## 級: L

実装領域5つ (シート新設 / Compose 化 / テーマ基盤 / 復元機構の撤去+新設 / MAUI サンプル)、UI 新設 (モック承認あり)、accepted ADR 4本を supersede する構造変更のため。

domain: android

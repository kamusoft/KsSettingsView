## Why

`add-monorepo-foundation` で `samples/android/` ディレクトリと placeholder の `README.md` のみが配置された状態であり、`add-settings-view-android-ui`（実装予定）で構築する KsSettingsView (Core / UI / Compose) を実機・エミュレータで目視確認できる Sample アプリ本体が存在しない。後続の `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom` には「`samples/android/` Compose Sample に各 Cell 表示例を追加する」というタスクが含まれているが、Sample アプリの土台がいつ作られるかを定義した変更提案が存在せず、依存順序が宙に浮いている。Sample アプリ土台を独立した capability として切り出すことで、責務と依存関係を明確化する。

## What Changes

- 新ディレクトリ `samples/android/` 配下に Android Studio プロジェクト形式の Compose Sample アプリ（最小構成）を作成
  - アプリ名: `KsSettingsViewSample`（仮）
  - 言語: Kotlin（Compose Multiplatform ではなく Android Jetpack Compose）
  - 最小 SDK: 29（minSdk 29、`add-monorepo-foundation` 規約準拠）
  - target SDK / compile SDK: 最新安定版に追従
  - Application ID プレフィックス: `jp.kamusoft.kssettingsview.samples.android`
- `settings.gradle.kts`（リポジトリルート相対の `android/` 配下、または `samples/android/` 直下に独立 `settings.gradle.kts` を配置）経由で `ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` をローカルプロジェクト参照
  - 参照方式: `includeBuild("../../android")` などの Gradle composite build を採用
- Sample アプリ内で独自の最小 Cell `SampleLabelCell`（`Cell` 準拠 / id・style・title のみ）と Renderer `SampleLabelCellViewHolder`（`CellViewHolder<SampleLabelCell>` 派生）を定義し、`KsCellRegistry` に登録
  - 既存の `PocLabelCell` / `PocLabelCellViewHolder`（`add-settings-view-android-ui` 計画で定義予定）は `ks-settingsview-ui` モジュール内 `internal` のため Sample から直接利用できない想定。Sample 専用 Cell を別途定義して PoC 相当の表示を実現する
- 1 ページのデモ画面を `MainActivity` から表示
  - Activity ベース（`ComponentActivity`）で `setContent { ... }` から Compose 階層を構築
  - `KsSettingsView`（`ks-settingsview-compose` ラッパ）を画面いっぱいに表示し、`settingsRoot { section { ... } }` の DSL で 1 セクション・複数行の `SampleLabelCell` を表示
- `samples/android/README.md`（現状 placeholder）を実 Sample のクイックスタート README に置き換え
  - Android Studio で開く手順、エミュレータでの起動手順、依存関係の説明を含む
- 「含まないこと」（後続提案で対応）：
  - 各種 Cell（Label / Switch / Command / Entry / Picker / Custom 等）のデモページ追加 → `add-cell-types-*` 群
  - CI 連携 / スナップショットテスト
  - Theme 切替 UI / Style 切替 UI

## Capabilities

### New Capabilities
- `samples-android`: `samples/android/` 配下に配置される Android Native (Jetpack Compose) Sample アプリの構造・依存・起動可能性に関する振る舞いを規定する

### Modified Capabilities
（なし。本提案は純粋な追加であり、`monorepo-foundation` spec の placeholder Scenario は引き続き有効。後続の別変更提案で `add-settings-view-android-ui` の完了条件と Cell 系提案の Sample 関連タスクを本 capability に整合させる）

## Impact

- 影響範囲: `samples/android/` 配下の新規 Android Studio プロジェクト・Kotlin / Compose ソース・README
- 依存:
  - `add-monorepo-foundation`（archive 済）: `samples/android/` ディレクトリと placeholder README が存在する前提
  - `add-settings-view-core`（archive 済）: `Cell` プロトコル / `SettingsRoot` / `Section` 等のモデルを使用
  - `refactor-accessory-and-root-hf`（archive 済）: `KsAnyView` / `RootAccessory` / `SectionAccessory` を使用
  - `add-settings-view-android-ui`（提案中・未実装）: `ks-settingsview-ui` / `ks-settingsview-compose` の公開 API（`KsSettingsView` / `Cell` / `CellViewHolder` / `KsCellRegistry` 等）を使用。本 Sample 提案の **実装着手は `add-settings-view-android-ui` の archive 完了後**とする
- 後続が依存:
  - `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom`: 本 Sample にページ追加する
  - `add-settings-view-android-ui` 側の完了条件「PocLabelCell を含む SettingsRoot を Compose Sample で表示すると 1 行のセルがテキスト付きで描画される」は、本提案完了後に Sample 専用 `SampleLabelCell` を使った検証へ置き換える形で吸収される（既存提案側の文言修正は本提案完了後に別途行う）
- リスク: 低。Android Studio プロジェクト構成は Gradle 標準テンプレートに準拠し、KsSettingsView 本体への影響はない

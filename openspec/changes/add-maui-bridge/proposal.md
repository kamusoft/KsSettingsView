## Why

`add-maui-bindings` 1 提案で「Native Bridge + Binding csproj + MAUI 本体 + 全 Cell Handler + Sample + Snapshot」と非常に広いスコープを抱えており、レビュー単位・実装単位・進捗追跡の全てで扱いにくい。本提案は分割の第 1 段として、`.NET MAUI` から Native iOS/Android の SettingsView を呼び出すための「Bridge ライブラリ層」のみを切り出して独立した変更提案とする。

CommunityToolkit Native Library Interop パターンに沿い、Swift / Kotlin の薄いブリッジを Native 側に作り、`XcodeProject` / `AndroidGradleProject` 形式で .NET MAUI の C# プロジェクトに取り込む。本提案完了時点では C# 側から Bridge API を直接呼んで Cell の宣言・表示ができる状態までを保証する。MAUI Handler 階層や `BindableObject` Cell 群は後続の `add-maui-core` / `add-maui-cells` で実装する。

Bridge API は **`setRoot` による初期化 + `applyDiff` による部分更新** の二段構えとする。`add-partial-update-core` / `add-partial-update-native` で Native UI 層に導入される `SettingsRootDiff` 型と `applyDiff` API を Bridge 経由で MAUI 側からも利用できるようにし、`AiForms.Maui.NativeCollectionView` の `ObservableCollection.CollectionChanged` ベースの部分更新パターンを `add-maui-core` 側 Handler から呼び出せるようにする。Root H/F は `SettingsRoot` のドメインモデルから削除されたため、Bridge は `setRootHeader(view:)` / `setRootFooter(view:)` を独立 API として公開する。

## What Changes

- Native Bridge ライブラリを新設：
  - iOS: `KsSettingsViewBridge.framework`（Swift で `@objc public` インターフェース、内部で `KsSettingsViewCore` / `KsSettingsViewUI` 利用）
  - Android: Kotlin の `KsSettingsViewBridge`（Java-friendly ファサード、`@JvmStatic` ファクトリ、内部で `ks-settingsview-core` / `ks-settingsview-ui` 利用）
- Bridge 公開 API：
  - `Builder` パターンで `SettingsRoot` を C# 側から組み立て（複雑なジェネリックを使わず、ObjC/Java で扱える DTO）。**Theme は Builder では扱わない**（`purify-core-extract-style-to-ui-layer` の方針に追随し、`SettingsRoot` から Theme は分離されている）
  - **`controller.setRoot(root)` / `view.setRoot(root)` で Native UI 層へ初期化・全体差し替えを行う**API（`add-partial-update-native` で Native UI 層に導入される `SettingsRootStore` の `replaceAll` 相当に対応）
  - **`controller.applyDiff(diff)` / `view.applyDiff(diff)` で部分更新を行う**API（`add-partial-update-core` の `SettingsRootDiff` を `KsSettingsRootDiffDTO` として ObjC/Java 互換で公開、**全 10 ケース**。`UpdateTheme` ケースは Native `SettingsRootDiff` から削除済みのため DTO 階層にも含めない）
  - **`controller.setTheme(theme)` / `view.setTheme(theme)` で Theme 適用を行う独立 API**（`KsThemeDTO` を受け取り、Bridge 内部で Native `Theme` 値 (UIColor / UIFont 直接保持 ／ Compose Color / TextStyle 直接保持) に変換し、`SettingsRootStore.applyTheme(_:)` 経由で `@Published var theme` / `StateFlow<Theme>` に書き込む。Diff Publisher は不発行）
  - 各 Cell ごとのファクトリ関数（`builder.addLabelCell(...)` 等。Phase A では最小 1 種類 `LabelCell` のみを公開、他 Cell は `add-maui-cells` で順次追加）
  - スタイル切替 API（`controller.setStyle(.classic / .modern)` / `view.setStyle(...)`）
  - 高頻度更新パス用の最小 API（`controller.updateCellValue(cellId:value:)`）：EntryCell の連続入力など、`applyDiff` の `replaceCell` で値を毎回全コピーするより軽量にする専用パス（Native 側 debounce 後にのみ呼ばれる、`add-maui-cells` 側で EntryCell 実装時に活用）
  - ユーザー操作 delegate / listener（iOS: `@objc protocol KsCellInteractionDelegate`、Android: `interface KsCellInteractionListener`）
  - Root H/F 用 API（`setRootHeader(view:)` / `setRootFooter(view:)`）：`SettingsRoot` から `header` / `footer` が削除されたため、Bridge から Native の `controller.rootHeader` / `view.rootHeader`（または相当 `RootAccessory` プロパティ）に変換して反映する。MAUI 側 `SettingsView.HeaderView`（`add-maui-core` で導入予定の旧 AiForms 互換 BindableProperty）の入力経路として利用される
- MAUI バインディングプロジェクトを新設：
  - `maui/KsSettingsView.Bindings.iOS.csproj`：`XcodeProject` 形式で `KsSettingsViewBridge.xcframework` を取り込み、`objective-sharpie` で `ApiDefinitions.cs` を生成
  - `maui/KsSettingsView.Bindings.Android.csproj`：`AndroidGradleProject` 形式で `KsSettingsViewBridge.aar` を取り込み
- Bridge ユニットテスト：
  - Builder API による `SettingsRoot` 構築の検証
  - `setRoot` 連続呼び出しで Native 側 DiffableDataSource / DiffUtil が差分のみ更新するか検証
  - `applyDiff` 各ケース（`InsertSection` / `RemoveCell` / `UpdateAccessory` 等。全 10 ケース）で Native 側 UI が部分更新されるか検証
  - `setTheme` で MAUI Color → Native (`UIColor` / Compose Color) への 1 段直接変換、および `SettingsRootStore.applyTheme(_:)` 経由で `@Published var theme` / `StateFlow<Theme>` が発火する (Diff Publisher 不発行) 検証
- ドキュメント：`docs/maui-bindings.md`（Native Library Interop パターン、Bridge API、ビルド手順）

## Capabilities

### New Capabilities
- `maui-bridge`: Native Bridge ライブラリ（iOS Swift / Android Kotlin）と MAUI バインディングプロジェクト（XcodeProject / AndroidGradleProject）の構造・公開 API・ビルド成果物に関する振る舞いを規定する

### Modified Capabilities
（なし。本提案は純粋な追加）

## Impact

- 影響範囲：
  - 新規 `ios/Sources/KsSettingsViewBridge/`（Swift モジュール）
  - 新規 `android/ks-settingsview-bridge/`（Gradle サブプロジェクト）
  - 新規 `maui/KsSettingsView.Bindings.iOS/` / `maui/KsSettingsView.Bindings.Android/`
  - `maui/KsSettingsView.slnx` への追加
  - `docs/maui-bindings.md` の新規作成
- 依存：
  - `add-monorepo-foundation`（archive 済）
  - `add-settings-view-core`（archive 済）
  - `add-settings-view-ios-ui`（archive 済）
  - `add-settings-view-android-ui`（archive 済）
  - `add-partial-update-core`（先行・archive 必須）: `SettingsRootDiff` / `AccessoryTarget` / `SettingsAccessory` 型、`SettingsRoot.header/footer` 削除
  - `add-partial-update-native`（先行・archive 必須）: `SettingsRootStore` および `applyDiff` API、Root H/F の UI 層プロパティ化
- 後続：
  - `add-maui-core`：本提案の Bridge を `KsSettingsView.Maui` の `SettingsViewHandler` から呼ぶ
  - `add-maui-cells`：本提案 Bridge に他 Cell 用 `addXxxCell(...)` を順次追加する形で利用する（Bridge API 表面は後続提案で拡張するが、本提案では `LabelCell` 相当の最小 API のみを公開する）
- リスク：中
  - **MAUI 9 + Native Library Interop の組み合わせはまだ実例が少なく、xcframework / aar の取り込みでハマる可能性**：`maui-native-binding-skill` のガイドに沿いつつ Phase A 早期で実機ビルドまで確認する
  - **Swift `@objc` で表現できない型（structured types、ジェネリクス）の扱い**：Builder API を Bridge 層で構築、Native Core は内部で `SettingsRoot` を組み立てる方針で回避
  - **`objective-sharpie` で自動生成した `ApiDefinitions.cs` の手動修正**：生成後の手動修正パッチを `Patches/` 配下にバージョン管理

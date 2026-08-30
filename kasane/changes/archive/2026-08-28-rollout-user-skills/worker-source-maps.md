# 作業ノート: ワーカー報告の源泉 concepts マップと drift 所見

manifest 草案 (タスク 2.5) の材料。パスは concepts ルート相対 / Skill 相対 (言語抜き)。

## kssettingsview-ios (タスク 2.1 完了)

- `kssettingsview-ios/SKILL.md`: ios/api/ios-swiftui.md, ios/api/ios-native-host.md, core/core-model/settings-tree.md, core/cells/basic-cells.md, cross/conventions/public-identifiers.md
- `kssettingsview-ios/references/cells.md`: core/cells/basic-cells.md, core/cells/input-cells.md, core/cells/ks-image.md, core/cells/picker-selection-surface.md, core/cells/number-picker-selection-surface.md, core/cells/date-picker-selection-surface.md, core/core-model/settings-tree.md, core/styling/cell-row-layout.md, core/styling/cell-visual-states.md
- `kssettingsview-ios/references/updates.md`: ios/api/ios-native-host.md, ios/api/ios-swiftui.md, core/core-model/structural-changes.md, core/core-model/settings-tree.md, core/architecture/store-and-update-streams.md, core/architecture/display-state-synchronization.md, core/architecture/declarative-tree-identity.md
- `kssettingsview-ios/references/styling.md`: core/styling/style-resolution.md, core/styling/list-appearance.md, core/styling/cell-row-layout.md, core/core-model/settings-tree.md, ios/api/ios-swiftui.md, ios/api/ios-native-host.md
- `kssettingsview-ios/references/custom-cells.md`: core/cells/custom-cell.md, core/architecture/cell-renderer-registry.md, core/styling/cell-visual-states.md, core/architecture/declarative-tree-identity.md, ios/api/ios-native-host.md, ios/api/ios-swiftui.md

ツール最低バージョン: Swift tools 5.10 / iOS 16.0 (取得元: ios/Package.swift)

### drift 所見 (2.1)

- 独立 Registry を注入する例で `registerCustomCell()` が欠落: concepts (core/cells/custom-cell.md, ios/api/ios-native-host.md, core/architecture/cell-renderer-registry.md) の独立 Registry 例は registerBasicCells/registerInputCells のみだが、実装 (ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift) の自動登録は shared Registry 限定のため、独立 Registry では CustomCell が placeholder になる。Skill 生成物は実装に合わせた (registry.registerCustomCell() + autoRegisterCustomCell: false)。concepts 側の追随はオーナー判断待ち (本 change の Non-Goal)

## kssettingsview-android (タスク 2.2 完了)

- `kssettingsview-android/SKILL.md`: android/api/android-compose.md, android/api/android-native-host.md, core/core-model/settings-tree.md, core/cells/basic-cells.md, cross/conventions/public-identifiers.md
- `kssettingsview-android/references/cells.md`: core/cells/basic-cells.md, core/cells/input-cells.md, core/cells/ks-image.md, core/cells/picker-selection-surface.md, core/cells/number-picker-selection-surface.md, core/cells/date-picker-selection-surface.md, core/core-model/settings-tree.md, core/styling/cell-row-layout.md, core/styling/cell-visual-states.md, android/api/android-compose.md, android/api/android-native-host.md
- `kssettingsview-android/references/updates.md`: android/api/android-native-host.md, android/api/android-compose.md, core/core-model/structural-changes.md, core/core-model/settings-tree.md, core/architecture/store-and-update-streams.md, core/architecture/display-state-synchronization.md, core/architecture/declarative-tree-identity.md
- `kssettingsview-android/references/styling.md`: core/styling/style-resolution.md, core/styling/list-appearance.md, core/styling/cell-row-layout.md, core/core-model/settings-tree.md, android/api/android-compose.md, android/api/android-native-host.md
- `kssettingsview-android/references/custom-cells.md`: core/cells/custom-cell.md, core/architecture/cell-renderer-registry.md, core/styling/cell-visual-states.md, core/architecture/declarative-tree-identity.md, android/api/android-native-host.md, android/api/android-compose.md

ツール最低バージョン: minSdk 29 / compileSdk 35 / JDK 17 (取得元: android/ks-settingsview-{core,ui,compose,bridge}/build.gradle.kts)、Kotlin 2.4.10 / AGP 8.13.2 / Compose BOM 2024.10.01 (取得元: android/gradle/libs.versions.toml)、Gradle 9.5.0 (取得元: android/gradle/wrapper/gradle-wrapper.properties)

### drift 所見 (2.2)

- `android/README.md` の「必要環境」が古い: AGP 8.7.3 / Gradle wrapper 8.10.2 と書かれているが、実ビルドファイルは AGP 8.13.2 (libs.versions.toml) / Gradle 9.5.0 (gradle-wrapper.properties)。concepts ⇔ 実装の矛盾ではなく README ⇔ ビルドファイルの乖離のため、Skill 側はビルドファイルの値を採用した。README の是正はタスク 6.2 の範囲外 (docs/ リンク解消のみ) なので別途要判断
- `android/ks-settingsview-ui/build.gradle.kts` の dependencies コメントに `docs/android-ui.md` への参照が残っている (docs/ 廃止で解決先が消える)。タスク 6.4 の残存検査に引っかかる可能性があるため申し送り

## kssettingsview-maui (タスク 2.3 完了)

- `kssettingsview-maui/SKILL.md`: maui/api/maui-facade.md, core/core-model/settings-tree.md, core/cells/basic-cells.md, core/styling/style-resolution.md, cross/conventions/public-identifiers.md
- `kssettingsview-maui/references/cells.md`: core/cells/basic-cells.md, core/cells/input-cells.md, core/cells/ks-image.md, core/cells/picker-selection-surface.md, core/cells/number-picker-selection-surface.md, core/cells/date-picker-selection-surface.md, core/core-model/settings-tree.md, core/styling/cell-row-layout.md, core/styling/cell-visual-states.md, maui/api/maui-facade.md
- `kssettingsview-maui/references/updates.md`: maui/api/maui-facade.md, core/core-model/structural-changes.md, core/core-model/settings-tree.md, core/architecture/store-and-update-streams.md, core/architecture/display-state-synchronization.md, maui/api/native-bridge.md
- `kssettingsview-maui/references/styling.md`: core/styling/style-resolution.md, core/styling/list-appearance.md, core/styling/cell-row-layout.md, core/styling/cell-visual-states.md, core/core-model/settings-tree.md, maui/api/maui-facade.md
- `kssettingsview-maui/references/custom-cells.md`: core/cells/custom-cell.md, core/styling/cell-visual-states.md, maui/api/maui-facade.md, maui/architecture/view-materialization.md

ツール最低バージョン: .NET SDK 10.0.300 (取得元: global.json)、TFM net10.0-ios / net10.0-android・Microsoft.Maui.Controls 10.0.70・iOS 16.0・Android API 29 (取得元: maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj)

### drift 所見 (2.3)

- なし (maui/api/maui-facade.md の記載と maui/KsSettingsView.Maui/ の実装コードは、確認した範囲で一致した)

## kssettingsview-aiforms-migration (タスク 2.4 完了)

- `kssettingsview-aiforms-migration/SKILL.md`: cross/conventions/aiforms-spec-summary.md, cross/conventions/aiforms-origin-reference.md, maui/api/maui-facade.md, cross/conventions/public-identifiers.md
- `kssettingsview-aiforms-migration/references/api-mapping.md`: cross/conventions/aiforms-spec-summary.md, cross/conventions/aiforms-origin-reference.md, maui/api/maui-facade.md, core/core-model/settings-tree.md, core/cells/basic-cells.md, core/cells/input-cells.md, core/cells/custom-cell.md, core/styling/style-resolution.md, core/styling/list-appearance.md, core/architecture/store-and-update-streams.md

ツール最低バージョン: .NET SDK 10.0.300 (取得元: global.json)、TFM net10.0-ios / net10.0-android・Microsoft.Maui.Controls 10.0.70・iOS 16.0・Android API 29 (取得元: maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj)。移行元側の比較値 net9.0-{ios,android,maccatalyst}・MauiVersion 9.0.120・iOS 14.2・Android API 27 は移植元 `../AiForms.Maui.SettingsView/SettingsView/SettingsView.csproj` から取得

### drift 所見 (2.4)

- `cross/conventions/aiforms-spec-summary.md` §11 の「MauiAppBuilder 拡張: `.AddSettingsViewHandler()`」は不正確。移植元の実体は `MauiAppBuilder.UseSettingsView(bool shouldCallDisconnectHandlerWhenPageUnloaded = false)` で、`AddSettingsViewHandler()` は `IMauiHandlersCollection` 側の拡張 (その内部で呼ばれる)。同 §11 の「最低 OS: iOS 14.2 / Android 8.0」も、移植元 csproj の実値は Android `SupportedOSPlatformVersion` 27.0。spec-summary は凍結資料 (正は移植元コード) のため書き換えず、生成物は移植元コードの実値を採用した
- `aiforms-spec-summary.md` は Section の公開プロパティを列挙していない (§1 の構造ツリーに `Section.cs` の行があるのみ)。移行対応表では移植元コード (`../AiForms.Maui.SettingsView/SettingsView/Section.cs`) から Title / FooterText / HeaderView / FooterView / HeaderHeight / IsVisible / FooterVisible / ItemsSource / ItemTemplate / TemplateStartIndex / UseDragSort を補って対応付けた

## ADDED Requirements

### Requirement: Native Bridge: iOS モジュールの存在

`ios/Sources/KsSettingsViewBridge/` に Swift モジュール `KsSettingsViewBridge` が存在し、`ios/Package.swift` のターゲットに追加されていなければならない (SHALL)。本モジュールは `KsSettingsViewCore` と `KsSettingsViewUI` に依存しなければならない (MUST)。本モジュールは `@objc public` インターフェースを介して Objective-C / C# から利用可能でなければならない (MUST)。

#### Scenario: Swift Package へのターゲット追加

- **GIVEN** リポジトリのクローン直後
- **WHEN** `ios/Package.swift` を確認する
- **THEN** `targets` 配列に `name: "KsSettingsViewBridge"` のターゲットが存在し、`dependencies` に `"KsSettingsViewCore"` と `"KsSettingsViewUI"` を含む

#### Scenario: モジュールビルド成功

- **GIVEN** Swift Package 全体
- **WHEN** `swift build` を実行する
- **THEN** `KsSettingsViewBridge` ターゲットが警告なしでビルド成功する

#### Scenario: ObjC 互換 API の公開

- **GIVEN** `KsSettingsViewBridge` モジュールのソース
- **WHEN** 公開クラス・メソッドの宣言を確認する
- **THEN** Builder / Controller / Delegate の全公開 API が `@objc` 属性を持ち、Swift Generated Header から ObjC 互換シグネチャが生成可能である

### Requirement: Native Bridge: Android モジュールの存在

`android/ks-settingsview-bridge/` に Gradle サブプロジェクトが存在し、`android/settings.gradle.kts` に登録されていなければならない (SHALL)。本サブプロジェクトは `com.android.library` プラグインを適用し、`ks-settingsview-core` と `ks-settingsview-ui` に依存しなければならない (MUST)。本サブプロジェクトの公開 API は Java から利用可能な命名規則（`@JvmStatic` ファクトリ、Java-friendly な型シグネチャ）に従わなければならない (MUST)。

#### Scenario: Gradle サブプロジェクトの登録

- **GIVEN** リポジトリのクローン直後
- **WHEN** `android/settings.gradle.kts` を確認する
- **THEN** `include(":ks-settingsview-bridge")` が記載されている

#### Scenario: aar 生成

- **GIVEN** Android プロジェクト全体
- **WHEN** `./gradlew :ks-settingsview-bridge:assembleRelease` を実行する
- **THEN** ビルドが成功し、`android/ks-settingsview-bridge/build/outputs/aar/` 配下に release aar が生成される

#### Scenario: Java 互換 API の公開

- **GIVEN** `KsSettingsViewBridge` Kotlin クラス
- **WHEN** ファクトリメソッドの宣言を確認する
- **THEN** `companion object` 内に `@JvmStatic` 注釈付きの生成メソッド（`makeBuilder`、`makeView` 等）が存在する

### Requirement: Bridge Builder API

`KsSettingsViewBridge` は Builder インスタンスを生成するファクトリメソッド `makeBuilder()` を公開しなければならない (SHALL)。Builder は以下のメソッドで `SettingsRoot` を宣言的に構築できなければならない (MUST)：

- `beginSection(header:footer:)` / `endSection()`：Section の開始・終了
- `addLabelCell(id:title:description:valueText:icon:hintText:)`：LabelCell の追加（本提案では Cell 追加 API は LabelCell のみ）
- `build() -> KsSettingsRootDTO`：構築済み `SettingsRoot` 相当 DTO を返す

Builder は **Theme を扱わない** (MUST NOT)。Theme は `SettingsRoot` ドメインモデルから完全に分離されており（`purify-core-extract-style-to-ui-layer` で `SettingsRoot.theme` フィールド削除済み）、Native UI 層に `Theme` 型を保持し、`controller.setTheme(_:)` / `view.setTheme(_:)` の独立 API で適用する設計を取る。そのため Builder には `setTheme` メソッドを実装してはならない。

`SettingsRoot` 値型自体に Root H/F は含まないため (MUST NOT、`add-partial-update-core` で確定)、Builder にも `setRootHeader` / `setRootFooter` メソッドは公開しない (MUST NOT)。Root H/F は Bridge Controller / View 側の `setRootHeader(view:)` / `setRootFooter(view:)` で別途設定する（後段 `Bridge Controller / View API` Requirement 参照）。

#### Scenario: Builder による LabelCell 構築（iOS）

- **GIVEN** `let builder = KsSettingsViewBridge.makeBuilder()`
- **WHEN** `builder.beginSection(header: "test", footer: nil)` → `builder.addLabelCell(id: "c1", title: "Title", description: nil, valueText: nil, icon: nil, hintText: nil)` → `builder.endSection()` → `let root = builder.build()`
- **THEN** `root` は 1 セクション・1 LabelCell を含む `KsSettingsRootDTO` となる

#### Scenario: Builder による LabelCell 構築（Android）

- **GIVEN** `val builder = KsSettingsViewBridge.makeBuilder()`
- **WHEN** `builder.beginSection("test", null); builder.addLabelCell("c1", "Title", null, null, null, null); builder.endSection(); val root = builder.build()`
- **THEN** `root` は 1 セクション・1 LabelCell を含む `KsSettingsRootDTO` となる

#### Scenario: Root H/F は Builder では設定不可

- **GIVEN** `let builder = KsSettingsViewBridge.makeBuilder()` の公開 API
- **WHEN** 公開メソッド一覧を確認する
- **THEN** `setRootHeader` / `setRootFooter` 系のメソッドは存在しない（Root H/F は Bridge Controller / View 側で設定する設計のため）

#### Scenario: Theme は Builder では設定不可

- **GIVEN** `let builder = KsSettingsViewBridge.makeBuilder()` の公開 API
- **WHEN** 公開メソッド一覧を確認する
- **THEN** `setTheme` / `theme` 系のメソッド・プロパティは存在しない（Theme は `SettingsRoot` から分離されており、Bridge Controller / View 側の `setTheme(_:)` 独立 API で適用する設計のため）

### Requirement: Bridge Controller / View API

`KsSettingsViewBridge` は Native UI コンポーネントのラッパを生成するファクトリメソッドを公開しなければならない (SHALL)：

- iOS: `makeController(delegate: KsCellInteractionDelegate) -> KsSettingsViewControllerBridge`
- Android: `makeView(context: Context, listener: KsCellInteractionListener) -> KsSettingsViewBridgeView`

各ラッパは以下のメソッドを公開しなければならない (MUST)：

- `setRoot(_ root: KsSettingsRootDTO)`：Native `KsSettingsViewController` / `KsSettingsView` に値型 `SettingsRoot` を全体差し替え・初期化するエントリポイント（`add-partial-update-native` の `SettingsRootStore.replaceAll` 相当）
- `applyDiff(_ diff: KsSettingsRootDiffDTO)`：Native UI 層に部分更新を伝えるエントリポイント（`add-partial-update-core` の `SettingsRootDiff` を ObjC/Java 互換 DTO で表現）。本 DTO は **Theme 更新ケースを含まない**（`purify-core-extract-style-to-ui-layer` で `SettingsRootDiff.updateTheme` が削除されたため）
- `setTheme(_ theme: KsThemeDTO)`：Theme 適用の独立 API。`SettingsRoot` ドメインモデルおよび `SettingsRootDiff` から完全分離されており、Bridge 内部では Native UI 層の `SettingsRootStore.applyTheme(_:)` 経由で `@Published var theme` / `StateFlow<Theme>` に書き込む（Diff Publisher は発行しない）
- `setStyle(_ style: KsSettingsViewStyle)`：Classic / Modern スタイル切替
- `setRootHeader(_ view: KsAnyViewDTO?)` / `setRootFooter(_ view: KsAnyViewDTO?)`：Root H/F の View 設定（`add-partial-update-native` の `controller.rootHeader` / `view.rootHeader` プロパティへの反映、`nil` で削除）
- `updateCellValue(cellId: String, value:)`：高頻度更新パス用最小 API（`applyDiff` の `replaceCell` より軽量、EntryCell の連続入力など `add-maui-cells` で活用）

`setRoot` と `applyDiff` は二段構えの責務分担とし、初期化・リセットには `setRoot`、部分更新には `applyDiff` を使う (MUST)。`setTheme` / `setStyle` / `setRootHeader` / `setRootFooter` はいずれも `setRoot` / `applyDiff` 経路と独立した専用 API として扱う (MUST)。

#### Scenario: Controller / View 生成（iOS）

- **GIVEN** `KsCellInteractionDelegate` を実装したオブジェクト
- **WHEN** `KsSettingsViewBridge.makeController(delegate: delegate)` を呼ぶ
- **THEN** `KsSettingsViewControllerBridge` インスタンスが返り、内部に `KsSettingsViewController` を保持している

#### Scenario: Controller / View 生成（Android）

- **GIVEN** Activity の `Context` と `KsCellInteractionListener` を実装したオブジェクト
- **WHEN** `KsSettingsViewBridge.makeView(context, listener)` を呼ぶ
- **THEN** `KsSettingsViewBridgeView` インスタンスが返り、内部に `KsSettingsView` を保持している

#### Scenario: setRoot による全体差し替え

- **GIVEN** Controller / View インスタンスと 2 つの異なる `KsSettingsRootDTO`（root1: 2 セクション、root2: 3 セクション）
- **WHEN** `setRoot(root1)` → `setRoot(root2)` を順に呼ぶ
- **THEN** 各呼び出しで Native `DiffableDataSource` / `ListAdapter + DiffUtil` の差分計算が実行され、変化したセクション・Cell のみが再描画される（全画面再描画は発生しない）

#### Scenario: applyDiff による部分更新（InsertCell）

- **GIVEN** Controller / View インスタンスに `setRoot` で初期 root を適用済み（Section 1 つ、Cell 2 個）
- **WHEN** `applyDiff(KsSettingsRootDiffInsertCellDTO(sectionId: sid, index: 0, cell: newCell))` を呼ぶ
- **THEN** Bridge 内部で対応する Native `SettingsRootDiff.insertCell(...)` を生成して `controller.applyDiff(_:)` / `view.applyDiff(_:)` を呼び出し、Native 側で 1 件のみ挿入アニメーションが実行される

#### Scenario: applyDiff による部分更新（RemoveCell）

- **GIVEN** Controller / View インスタンスに root が適用済み
- **WHEN** `applyDiff(KsSettingsRootDiffRemoveCellDTO(cellId: cid))` を呼ぶ
- **THEN** Native 側 `applyDiff(.removeCell(cellID:))` が呼ばれ、該当 Cell のみ削除アニメーションで消える

#### Scenario: applyDiff による Accessory 更新

- **GIVEN** Controller / View インスタンスに root が適用済み
- **WHEN** `applyDiff(KsSettingsRootDiffUpdateAccessoryDTO(target: .rootHeader, accessory: ...))` を呼ぶ
- **THEN** Native 側 `applyDiff(.updateAccessory(target: .rootHeader, accessory: ...))` が呼ばれ、Root Header の boundary supplementary view が更新される

#### Scenario: setRootHeader による Root H/F の View 設定

- **GIVEN** Controller / View インスタンス
- **WHEN** `setRootHeader(KsAnyViewDTO(view: customMauiView))` を呼ぶ
- **THEN** Bridge 内部で `KsAnyView` に変換し、Native 側 `controller.rootHeader = .view(...)` / `view.rootHeader = RootAccessory.View(...)` に反映される

#### Scenario: setRootHeader(nil) で Root Header を削除

- **GIVEN** Controller / View インスタンスに Root Header が設定済み
- **WHEN** `setRootHeader(nil)` を呼ぶ
- **THEN** Native 側 `controller.rootHeader = nil` / `view.rootHeader = null` に反映され、Root Header の boundary supplementary view / `headerAdapter` 描画が削除される

#### Scenario: setTheme による Theme 適用

- **GIVEN** Controller / View インスタンスと、新しい `KsThemeDTO`（separatorColor などのフィールドを保持）
- **WHEN** `setTheme(newThemeDTO)` を呼ぶ
- **THEN** Bridge 内部で `KsThemeDTO` を Native の `Theme` 値（iOS: `UIColor` / `UIFont` 直接保持、Android: Compose `Color` / `TextStyle` 直接保持）に変換し、Native UI 層の `SettingsRootStore.applyTheme(_:)` 経由で `@Published var theme` / `StateFlow<Theme>` に書き込まれる。`setRoot` / `applyDiff` 経路は呼ばれない（Theme 更新は専用 API 経路）

#### Scenario: setTheme は Diff Publisher を発行しない

- **GIVEN** Controller / View インスタンスに root を `setRoot` 済み、Native UI 層の Diff Publisher / SharedFlow を購読中
- **WHEN** `setTheme(newThemeDTO)` を呼ぶ
- **THEN** Diff Publisher / SharedFlow に新規イベントは流れない（`SettingsRootStore.applyTheme(_:)` は Diff を発行しない設計のため）。Theme 変更は `@Published var theme` / `StateFlow<Theme>` 経由でのみ伝播する

#### Scenario: updateCellValue API の存在と単一 Cell 値更新経路

- **GIVEN** Controller / View インスタンスと、任意の cellId を持つ Cell（本提案では LabelCell）を含む `KsSettingsRootDTO` を `setRoot` 済の状態
- **WHEN** `controller.updateCellValue(cellId: "any-cell-id", value: NSString("v"))` を呼ぶ
- **THEN** Bridge 内部処理が `setRoot` 経路を走らせず、単一 Cell 値更新の Native 呼び出しのみが実行される（API の存在と「`setRoot` 経路と別経路である」ことを本提案で保証。EntryCell との実連携シナリオは `add-maui-cells` で追加）

### Requirement: ユーザー操作 delegate / listener

Bridge は単一の `KsCellInteractionDelegate`（iOS）/ `KsCellInteractionListener`（Android）に全 Cell の操作通知を集約しなければならない (SHALL)。Cell 種別はメソッド名で識別する（`didChangeBoolValue` / `didChangeTextValue` / `didTapCommand` 等）。本提案では 14 Cell 種別分のメソッド宣言をインターフェース定義として全て用意するが、実体実装は LabelCell 経路（`didTapCell`）のみに限定する（他 Cell の実体実装は `add-maui-cells`）。本提案では加えて Bridge 内部で 200ms debounce を行う汎用ユーティリティを実装しなければならない (MUST)。本ユーティリティは EntryCell との実連携を行わず、単体テストで動作を検証する。EntryCell との実連携（200ms debounce 経由で `didChangeTextValue` を発火）は後続 `add-maui-cells` で実装する。

#### Scenario: delegate / listener インターフェース定義の存在

- **GIVEN** Bridge モジュールのソース
- **WHEN** `KsCellInteractionDelegate.swift` / `KsCellInteractionListener.kt` を確認する
- **THEN** 14 Cell 種別分の通知メソッドがインターフェース定義として宣言されている（本提案では実体実装は LabelCell 経路のみだが、インターフェース定義は全 Cell 分用意する）

#### Scenario: LabelCell のタップ通知（本提案で実装する Cell）

- **GIVEN** LabelCell を含む `SettingsRoot` を `setRoot` した Controller / View
- **WHEN** ユーザーが LabelCell をタップする（LabelCell に `onTap` が設定されている場合）
- **THEN** delegate / listener の `didTapCell(cellId:)` メソッドが該当 cellId で呼び出される

#### Scenario: debounce ユーティリティの単体動作（本提案で実装、Cell 連携は後続）

- **GIVEN** Bridge 内部 200ms debounce ユーティリティのインスタンス
- **WHEN** 100ms 間隔で 5 回連続して値（`"a"` / `"ab"` / `"abc"` / `"abcd"` / `"abcde"`）を投入する
- **THEN** ユーティリティのコールバックは最終値 `"abcde"` で 1 回のみ呼ばれる（最後の投入から 200ms 経過後）

### Requirement: MAUI バインディングプロジェクト: iOS

`maui/KsSettingsView.Bindings.iOS/` に C# プロジェクト `KsSettingsView.Bindings.iOS.csproj` が存在し、`<XcodeProject>` 形式で `ios/` 配下の `KsSettingsViewBridge` を取り込まなければならない (SHALL)。`objective-sharpie` で生成された `ApiDefinitions.cs` をプロジェクトに含み、`dotnet build -f net9.0-ios` がコマンドラインから成功しなければならない (MUST)。手動修正パッチは `Patches/` 配下に `.diff` ファイルでバージョン管理しなければならない (SHALL)。

#### Scenario: csproj の存在と XcodeProject 参照

- **GIVEN** `maui/KsSettingsView.Bindings.iOS/KsSettingsView.Bindings.iOS.csproj`
- **WHEN** csproj の内容を確認する
- **THEN** `<XcodeProject Include="../../ios/...">` 形式の参照が存在し、`KsSettingsViewBridge` Xcode プロジェクト相当を指している

#### Scenario: ApiDefinitions.cs の存在

- **GIVEN** `maui/KsSettingsView.Bindings.iOS/`
- **WHEN** ディレクトリ内のファイルを確認する
- **THEN** `ApiDefinitions.cs` が存在し、Bridge 公開 API（Builder / Controller / Delegate）の C# 宣言を含む

#### Scenario: dotnet build 成功

- **GIVEN** `maui/KsSettingsView.Bindings.iOS/`
- **WHEN** `dotnet build -f net9.0-ios` を実行する
- **THEN** ビルドが警告なしで成功する

#### Scenario: 手動修正パッチのバージョン管理

- **GIVEN** `objective-sharpie` 自動生成 `ApiDefinitions.cs` への手動修正が必要なケース
- **WHEN** `maui/KsSettingsView.Bindings.iOS/Patches/` 配下を確認する
- **THEN** `.diff` 形式の修正パッチがバージョン管理されている、または修正不要であれば `Patches/README.md` でその旨が記載されている

### Requirement: MAUI バインディングプロジェクト: Android

`maui/KsSettingsView.Bindings.Android/` に C# プロジェクト `KsSettingsView.Bindings.Android.csproj` が存在し、`<AndroidGradleProject>` 形式で `android/ks-settingsview-bridge/` を取り込まなければならない (SHALL)。`dotnet build -f net9.0-android` がコマンドラインから成功しなければならない (MUST)。

#### Scenario: csproj の存在と AndroidGradleProject 参照

- **GIVEN** `maui/KsSettingsView.Bindings.Android/KsSettingsView.Bindings.Android.csproj`
- **WHEN** csproj の内容を確認する
- **THEN** `<AndroidGradleProject Include="../../android/ks-settingsview-bridge">` 形式の参照が存在する

#### Scenario: dotnet build 成功

- **GIVEN** `maui/KsSettingsView.Bindings.Android/`
- **WHEN** `dotnet build -f net9.0-android` を実行する
- **THEN** ビルドが警告なしで成功し、Java バインディング C# クラスが自動生成される

### Requirement: MAUI ソリューションファイルへの登録

`maui/KsSettingsView.slnx` に `KsSettingsView.Bindings.iOS.csproj` と `KsSettingsView.Bindings.Android.csproj` の両方が登録されなければならない (SHALL)。

#### Scenario: slnx への登録

- **GIVEN** `maui/KsSettingsView.slnx`
- **WHEN** プロジェクト参照一覧を確認する
- **THEN** `KsSettingsView.Bindings.iOS` と `KsSettingsView.Bindings.Android` の両方が含まれている

### Requirement: Bridge ユニットテストの存在

Bridge モジュールには iOS / Android それぞれにユニットテストが存在しなければならない (SHALL)。テストは以下を検証しなければならない (MUST)：

- Builder の各メソッドが正しく Native Cell / Section を構築する
- `setRoot` 連続呼び出しで Native 側 `DiffableDataSource` / `ListAdapter + DiffUtil` が差分のみ更新する
- Root H/F の `setRootHeader(view:)` / `setRootHeader(view: nil)` で Native 側 `controller.rootHeader` / `view.rootHeader` が期待通り変化する（`SettingsRoot` 自体には `header` プロパティを含まないため、`KsSettingsRootDTO` ではなく UI 層プロパティを検証対象とする）
- `setTheme(_:)` で `KsThemeDTO` を Native `Theme` 値に変換し、`SettingsRootStore.applyTheme(_:)` 経由で `@Published var theme` / `StateFlow<Theme>` が発火する（Diff Publisher は不発行）
- 200ms debounce ユーティリティが連続投入時に最終値のみコールバックする

#### Scenario: iOS Bridge テスト成功

- **GIVEN** iOS Bridge テストターゲット
- **WHEN** `swift test --filter KsSettingsViewBridgeTests` を実行する
- **THEN** 全テストが成功する

#### Scenario: Android Bridge テスト成功

- **GIVEN** Android Bridge テストターゲット
- **WHEN** `./gradlew :ks-settingsview-bridge:test` を実行する
- **THEN** 全テストが成功する

### Requirement: ドキュメント

`docs/maui-bindings.md` に以下の内容を含むドキュメントが存在しなければならない (SHALL)：

- Native Library Interop パターン採用の理由
- Bridge 公開 API の一覧と用例（iOS / Android）
- xcframework / aar のビルド手順
- `objective-sharpie` 手動修正パッチの運用ルール

#### Scenario: ドキュメントの存在

- **GIVEN** リポジトリのクローン直後
- **WHEN** `docs/maui-bindings.md` を確認する
- **THEN** 上記 4 セクションを含むドキュメントが存在する

### Requirement: KsSettingsRootDiffDTO 型

`KsSettingsViewBridge` は `add-partial-update-core` で定義された `SettingsRootDiff` を ObjC / Java 互換の DTO として公開しなければならない (SHALL)。`KsSettingsRootDiffDTO` は抽象基底型（Swift `@objc class`、Kotlin `sealed class`）であり、`SettingsRootDiff` の全 10 ケースに対応するサブクラスを持たなければならない (MUST)：

- `KsSettingsRootDiffFullDTO(root: KsSettingsRootDTO)`
- `KsSettingsRootDiffInsertSectionDTO(index: Int, section: KsSectionDTO)`
- `KsSettingsRootDiffRemoveSectionDTO(sectionId: String)`
- `KsSettingsRootDiffMoveSectionDTO(from: Int, to: Int)`
- `KsSettingsRootDiffReplaceSectionDTO(sectionId: String, newSection: KsSectionDTO)`
- `KsSettingsRootDiffInsertCellDTO(sectionId: String, index: Int, cell: KsCellDTO)`
- `KsSettingsRootDiffRemoveCellDTO(cellId: String)`
- `KsSettingsRootDiffReplaceCellDTO(cellId: String, newCell: KsCellDTO)`
- `KsSettingsRootDiffMoveCellDTO(cellId: String, toIndex: Int)`
- `KsSettingsRootDiffUpdateAccessoryDTO(target: KsAccessoryTargetDTO, accessory: KsSettingsAccessoryDTO?)`

**`KsSettingsRootDiffUpdateThemeDTO` は導入しない** (MUST NOT)。`purify-core-extract-style-to-ui-layer` により Native `SettingsRootDiff` から `updateTheme` ケースが削除されたため、Theme 更新は `KsSettingsRootDiffDTO` 階層に含めず、Bridge Controller / View 側の `setTheme(_:)` 独立 API（前述「Bridge Controller / View API」Requirement 参照）で扱う。

`KsAccessoryTargetDTO` は `add-partial-update-core` の `AccessoryTarget` に対応する 4 ケース DTO（`RootHeader` / `RootFooter` / `SectionHeader(sectionId)` / `SectionFooter(sectionId)`）、`KsSettingsAccessoryDTO` は `SettingsAccessory` に対応する 2 ケース DTO（`Root(rootAccessory)` / `Section(sectionAccessory)`）として公開しなければならない (MUST)。

Bridge 実装は受け取った DTO を Native `SettingsRootDiff` に変換し、`SettingsRootStore` を介さず直接 `controller.applyDiff(_:)` / `view.applyDiff(_:)` を呼ぶ (MUST)（Bridge は Native UI を直接操作する責務に集中し、Store 抽象は Native 利用者専用とする）。

#### Scenario: DTO の階層構造（iOS）

- **GIVEN** iOS Bridge モジュール
- **WHEN** `KsSettingsRootDiffDTO` のサブクラス一覧を参照する
- **THEN** 上記 10 サブクラスが ObjC 互換の `@objc class` として定義されている。`KsSettingsRootDiffUpdateThemeDTO` は存在しない

#### Scenario: DTO の階層構造（Android）

- **GIVEN** Android Bridge モジュール
- **WHEN** `KsSettingsRootDiffDTO` のサブクラス一覧を参照する
- **THEN** 上記 10 サブクラスが Java 互換の `class` 階層として定義されている。`KsSettingsRootDiffUpdateThemeDTO` は存在しない

#### Scenario: DTO から Native Diff への変換（iOS）

- **GIVEN** `KsSettingsRootDiffInsertCellDTO(sectionId: "s1", index: 0, cell: someCellDTO)`
- **WHEN** Bridge 内部の変換関数を呼ぶ
- **THEN** `SettingsRootDiff.insertCell(sectionID: someUUID, at: 0, cell: someCell)` が生成される（`sectionId: String` から `UUID` への変換は Bridge 内部で実施）

#### Scenario: DTO から Native Diff への変換（Android）

- **GIVEN** `KsSettingsRootDiffInsertCellDTO(sectionId = "s1", index = 0, cell = someCellDTO)`
- **WHEN** Bridge 内部の変換関数を呼ぶ
- **THEN** `SettingsRootDiff.InsertCell(sectionId = "s1", index = 0, cell = someCell)` が生成される（`sectionId: String` のまま渡す）

#### Scenario: AccessoryTarget DTO の変換

- **GIVEN** `KsAccessoryTargetRootHeaderDTO()` または `KsAccessoryTargetSectionHeaderDTO(sectionId: "s1")`
- **WHEN** Bridge 内部の変換関数を呼ぶ
- **THEN** Native `AccessoryTarget.rootHeader` または `AccessoryTarget.sectionHeader(sectionID: someUUID)` / `.SectionHeader(sectionId = "s1")` が生成される

### Requirement: KsThemeDTO 型と Native Theme への変換

`KsSettingsViewBridge` は MAUI 側から Theme を受け取るための ObjC / Java 互換 DTO として `KsThemeDTO` を公開しなければならない (SHALL)。`KsThemeDTO` は Native UI 層の `Theme`（iOS: `KsSettingsViewUI.Theme`、Android: `ks-settingsview-ui` の `Theme`）に対応する payload を保持し、Native の各フィールド型に対応する DTO フィールドを持たなければならない (MUST)：

- 色フィールド（`separatorColor` / `backgroundColor` / `titleColor` / `descriptionColor` / `valueTextColor` / `hintTextColor` 等）：Bridge 内部で MAUI `Microsoft.Maui.Graphics.Color` を **直接** Native 型 (iOS: `UIColor`、Android: Compose `androidx.compose.ui.graphics.Color`) に変換して保持する。`purify-core-extract-style-to-ui-layer` により `KsColor` が Core から削除されたため、`KsColorDTO` のような独自 Color DTO 型は導入しない (MUST NOT)
- フォントフィールド（`titleFont` / `descriptionFont` / 等）：MAUI の `FontSize` / `FontFamily` / `FontAttributes` 系を Native 型 (iOS: `UIFont`、Android: Compose `TextStyle`) に Bridge 内部で **直接** 変換する。`KsFontDTO` のような独自 Font DTO 型は導入しない (MUST NOT)
- 画像フィールド（背景画像など Theme が `KsImage` を含むなら）：UI 層に再配置された `KsImage` 表現に対応する DTO（例: `KsImageDTO`）として Bridge で受け取る

Bridge 内部の変換ロジックは MAUI Color → Native 型 への 1 段直接変換でなければならない (MUST)。中間表現として旧 `KsColor` 相当の Double-based RGBA 構造を経由してはならない (MUST NOT、本ロジック自体が `purify-core-extract-style-to-ui-layer` で削除済み)。

`controller.setTheme(_:)` / `view.setTheme(_:)` は `KsThemeDTO` を受け取り、Bridge 内部で Native `Theme` 値（UIColor / UIFont 直接保持 ／ Compose Color / TextStyle 直接保持）に変換した上で Native UI 層の `SettingsRootStore.applyTheme(_:)` を呼ばなければならない (MUST)。

#### Scenario: KsThemeDTO の payload 構造（iOS）

- **GIVEN** iOS Bridge モジュール
- **WHEN** `KsThemeDTO` の公開プロパティを確認する
- **THEN** Native iOS `Theme`（`KsSettingsViewUI.Theme`）の各フィールドに対応する DTO プロパティが定義されている。`KsColorDTO` / `KsFontDTO` のような独自 Color / Font DTO 型は存在せず、Bridge 内部変換関数で MAUI Color → `UIColor` / MAUI Font 指定 → `UIFont` を 1 段で行う

#### Scenario: KsThemeDTO の payload 構造（Android）

- **GIVEN** Android Bridge モジュール
- **WHEN** `KsThemeDTO` の公開プロパティを確認する
- **THEN** Native Android `Theme`（`ks-settingsview-ui` の `Theme`）の各フィールドに対応する DTO プロパティが定義されている。`KsColorDTO` / `KsFontDTO` のような独自 Color / Font DTO 型は存在せず、Bridge 内部変換関数で MAUI Color → Compose `Color` / MAUI Font 指定 → Compose `TextStyle` を 1 段で行う

#### Scenario: MAUI Color から Native Color への直接変換（iOS）

- **GIVEN** MAUI `Microsoft.Maui.Graphics.Color(red: 0.9, green: 0.85, blue: 0.7, alpha: 1.0)` を含む `KsThemeDTO`
- **WHEN** Bridge 内部の `setTheme` 変換関数を呼ぶ
- **THEN** `UIColor(red: 0.9, green: 0.85, blue: 0.7, alpha: 1.0)` が直接生成され、Native `Theme.separatorColor` 等に格納される（旧 `KsColor` 構造は経由しない）

#### Scenario: MAUI Color から Native Color への直接変換（Android）

- **GIVEN** MAUI `Microsoft.Maui.Graphics.Color(red: 0.9, green: 0.85, blue: 0.7, alpha: 1.0)` を含む `KsThemeDTO`
- **WHEN** Bridge 内部の `setTheme` 変換関数を呼ぶ
- **THEN** Compose `Color(red = 0.9f, green = 0.85f, blue = 0.7f, alpha = 1.0f)` が直接生成され、Native `Theme.separatorColor` 等に格納される（旧 `KsColor` 構造は経由しない）

#### Scenario: setTheme → SettingsRootStore.applyTheme の経路

- **GIVEN** Controller / View インスタンスと有効な `KsThemeDTO`
- **WHEN** `setTheme(themeDTO)` を呼ぶ
- **THEN** Bridge は DTO を Native `Theme` 値に変換し、`SettingsRootStore.applyTheme(_:)` を 1 回呼ぶ。`@Published var theme` / `StateFlow<Theme>` が新値で発火し、`setRoot` / `applyDiff` 経路および Diff Publisher は呼ばれない

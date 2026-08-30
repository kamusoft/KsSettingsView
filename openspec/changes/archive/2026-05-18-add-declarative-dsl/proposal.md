## Why

`add-partial-update-native` で SwiftUI / Compose ラッパが `init(store:)` ベースの命令型 API に統一されたが、SwiftUI / Compose の本来の流儀である宣言的記法（`KsSettingsView { Section { Cell... } }`）からは大きく乖離してしまった。設定画面の典型用途（数十〜数百セルの静的構造）でも `SettingsRootStore` の事前構築と `store.insertCell(...)` のような命令型操作が必須となり、宣言的 UI フレームワークのユーザーから見て不自然な API となっている。

本提案は、初期実装（`add-settings-view-ios-ui` archive 済）で提供されていた DSL 方式を、現 `add-partial-update-native` の Store + applyDiff 基盤を内部で活用する形で復活させる。SwiftUI / Compose 利用者が宣言的に Cell ツリーを記述しつつ、内部実装としては Swift 値型の全再構築を避ける Store 経路に流れ込む二段構えとする。さらに Section / Cell / Root の各レベルで Modifier 風 API（`.sectionHeader(...)` / `.rootHeader { }` / `.font(...)` 等）と任意 View 対応、独自 `ForEach` / `forEach` 関数による動的コレクション展開、自動 ID 採番と明示 ID API による Cell / Section 同一性判定戦略を整備する。

Store 直接利用 API（`KsSettingsView(store:)`）は無限スクロール / 大量データ / リアルタイム高頻度更新用のパワーユーザー向けに **引き続き併存** させる。これにより一般用途は DSL、特殊用途は Store という使い分けが可能になる。

## What Changes

### iOS SwiftUI ラッパ（`KsSettingsViewSwiftUI` モジュール）

- **NEW**: `KsSettingsView` に DSL init を追加：`init(style:, @SettingsRootBuilder _ sections: () -> [KsSection])`
  - 既存の `init(store: SettingsRootStore, style:)` は **維持**（パワーユーザー向け Store 経路として併存）
- **NEW**: DSL init 利用時は内部で `@StateObject` として `SettingsRootStore` を保持し、`body` 再評価のたびに新旧の宣言ツリーを比較して `SettingsRootDiff` を算出、内部 Store の `applyDiff(_:)` 経路に流す
- **MODIFIED**: `@resultBuilder SettingsRootBuilder` を拡張：従来の `Section` の集約に加え、独自 `ForEach` 関数の戻り値（`[KsSection]`）も受け入れる
- **MODIFIED**: `@resultBuilder SectionBuilder` を拡張：従来の `any KsCell` の集約に加え、独自 `ForEach` 関数の戻り値（`[any KsCell]`）も受け入れる
- **NEW**: 独自 `ForEach` 関数を 4 オーバーロード提供：
  - ルート（Section 列）用 × `Identifiable` 版 + `id:` KeyPath 版
  - セクション内（Cell 列）用 × `Identifiable` 版 + `id:` KeyPath 版
  - すべて関数名は `ForEach` に統一（SwiftUI 本家の View 版 `ForEach` と型推論で振り分け）
- **NEW**: `KsSettingsView` の View modifier を拡張：
  - `.rootHeader(_ text: String)` / `.rootHeader<V: View>(@ViewBuilder content: () -> V)`：Root Header を文字列または任意 View で指定
  - `.rootFooter(_ text: String)` / `.rootFooter<V: View>(@ViewBuilder content: () -> V)`：Root Footer 同上
  - `.style(_ style: KsSettingsViewStyle)`：スタイル切替（既存の init 引数と同等の役割）
  - `.theme(_ theme: Theme)`：Theme 切替
  - **BREAKING**: `add-partial-update-native` で導入される `.header(_ accessory: RootAccessory?)` / `.footer(_ accessory: RootAccessory?)` modifier は本提案で **削除** する（本ライブラリは運用前のため互換維持は不要）。利用者は `.rootHeader(...)` / `.rootFooter(...)` のみを使う
- **NEW**: `KsSection`（`KsSettingsViewCore.Section` の型エイリアス）に modifier API を追加：
  - `.sectionHeader(_ text: String)` / `.sectionHeader<V: View>(@ViewBuilder content: () -> V)`
  - `.sectionFooter(_ text: String)` / `.sectionFooter<V: View>(@ViewBuilder content: () -> V)`
  - `.sectionID(_ id: AnyHashable)`：動的構造での Section 同一性明示指定
- **NEW**: `KsCell` プロトコル準拠の各 Cell に Cell modifier API を追加（拡張メソッド経由）：
  - `.font(_ font: KsFont)` / `.icon(_ icon: KsIcon)` / `.cellHeight(_ height: CGFloat)` / `.backgroundColor(_ color: KsColor)` / `.disabled(_ flag: Bool)` 等
  - `.cellID(_ id: AnyHashable)`：動的構造での Cell 同一性明示指定
  - すべて自身の値型を copy して新値を返す（SwiftUI 流儀）
- **NEW**: DSL → `SettingsRootDiff` 列の算出ロジックを `KsSettingsViewSwiftUI` 内部に実装：
  - 旧宣言ツリー（前回 `body` 評価結果）と新宣言ツリーを Section / Cell の同一性で比較
  - `KsCellID` / Section.ID を Section ID / Cell ID 判定戦略（後述）に基づき採番
  - 算出された Diff 列を内部 `SettingsRootStore.applyDiff(_:)` 相当に流す（または直接 Controller.applyDiff へ）

### Android Compose ラッパ（`ks-settingsview-compose` モジュール）

- **NEW**: `@Composable fun KsSettingsView` に DSL receiver 版を追加：`KsSettingsView(modifier:, style:, rootHeader:, rootFooter:, content: DSLSettingsRootScope.() -> Unit)`
  - 既存の `KsSettingsView(store:, modifier:, style:, headerView:, footerView:)` は **維持**（Store 経路として併存）
- **NEW**: DSL receiver 版利用時は `remember` で `SettingsRootStore` を内部保持し、Recomposition のたびに宣言ツリーを比較して `SettingsRootDiff` を算出、内部 Store の Diff 経路に流す
- **MODIFIED**: `DSLSettingsRootScope` を拡張：
  - 既存の `section(id:, header:, footer:, block:)` API を維持しつつ、新規 `Section(...)` 関数を追加（id 省略可、ID 自動採番、**戻り値は `SectionHandle` で modifier chain 可能**）
  - 独自 `forEach(items:, key:, content:)` 関数を追加（ルート / セクション両用、receiver で振り分け、Compose 公式 `key` lambda 流儀）
  - **NEW**: 独自 `forEach<T : KsIdentifiable>(items:, content:)` の **`key` 省略版** も追加（`inline reified`、SwiftUI Identifiable 版と並列）
- **MODIFIED**: `DSLSectionScope` を拡張：
  - 既存の `cell(cell:)` API を維持しつつ、**戻り値を `Unit` から `CellHandle` に変更**して modifier chain を可能化
  - **NEW**: Cell コンストラクタを直接呼べる形を実現するため、具象 Cell 型ごとに `DSLSectionScope` 拡張関数を提供する規約とする（例: `fun DSLSectionScope.LabelCell(title:, ...): CellHandle = cell(LabelCell(...))`）。これにより `Section("...") { LabelCell(title = "...") }` のように iOS と完全並列な書き味で Cell を直置きできる
  - **NEW**: `operator fun Cell.unaryPlus(): CellHandle = cell(this)` を `DSLSectionScope` 内に定義し、外部から渡された `Cell` 値を `+cell` で DSL に流す逃げ道を提供
  - 独自 `forEach(items:, key:, content:)` 関数を追加（セクション内 Cell 用、`key` lambda 版と `KsIdentifiable` 版を併存）
- **NEW**: `interface KsIdentifiable { val id: Any }` を `ks-settingsview-compose` モジュール内に定義（Compose 専用の DSL marker interface）
- **NEW**: `SectionHandle` / `CellHandle` 型を `ks-settingsview-compose` モジュール内に定義（`internal constructor` + `@SettingsRootDsl` で scope 越境を防止）
- **NEW**: Section の modifier 風 API を追加（**SectionHandle 経由 chain**、iOS の `Section { ... }.sectionFooter("...")` と並列）：
  - `SectionHandle.sectionHeader(text: String): SectionHandle`
  - `SectionHandle.sectionHeader(content: @Composable () -> Unit): SectionHandle`
  - `SectionHandle.sectionFooter(text: String): SectionHandle`
  - `SectionHandle.sectionFooter(content: @Composable () -> Unit): SectionHandle`
  - `SectionHandle.sectionID(id: Any): SectionHandle`：明示 Section ID
  - 既存の引数指定版（`Section(header = ..., footer = ...)`）も併存維持
- **NEW**: Cell の modifier 風 API を 2 系統で提供：
  - **CellHandle 経由 chain**（DSL 内推奨、iOS の Cell modifier chain と並列）：`.font(...)` / `.icon(...)` / `.cellHeight(...)` / `.backgroundColor(...)` / `.disabled(...)` / `.cellID(...)`
  - 既存の `Cell` 値型 modifier（外部 Cell 値や Store 方式での利用用）も維持
  - すべて自身を copy / dataclass copy して新値を返す
- **DECISION（オーナーレビュー対応）**: `@Composable fun KsSettingsView` の Root H/F は **引数指定のまま維持**（`rootHeader: (@Composable () -> Unit)?`、`rootFooter: (@Composable () -> Unit)?`）。`@Composable` 関数の戻り値 `Unit` 規約により iOS の `.rootHeader(...)` modifier chain は構造的に困難なため、Compose イディオム尊重で意図的な非対称を採用する
- **NEW**: DSL → `SettingsRootDiff` 列の算出ロジックを `ks-settingsview-compose` 内部に実装（iOS と同等のアルゴリズム）

### Cell / Section 同一性判定戦略（iOS / Compose 共通仕様）

- **NEW**: Section ID 判定の優先順位：
  1. ForEach 配下 → `item.id`（Identifiable または `id:` KeyPath / `key:` lambda）を引き継ぐ
  2. `.sectionID(_:)` modifier で明示指定 → それを採用
  3. ヘッダが文字列 Accessory（`SectionAccessory.text`）の場合 → ハッシュ（ルート位置, ヘッダ文字列）
  4. フォールバック → ルート位置ベース（`rootIdx`）
- **NEW**: Cell ID 判定の優先順位：
  1. ForEach 配下 → `item.id` を引き継ぐ
  2. `.cellID(_:)` modifier で明示指定 → それを採用
  3. デフォルト → ハッシュ（SectionID, Section 内位置, Cell 型）
- **DOCUMENTED**: ヘッダなし複数 Section が動的に追加・削除される構造は位置ベースにフォールバックするため、`ForEach` または `.sectionID(_:)` の明示指定を **推奨ドキュメント指針** として明記する
- **NEW（オーナーレビュー対応）**: 具象 Cell（iOS: `KsCell` 準拠 struct、Compose: `Cell` 準拠 data class）の `id` パラメータには **UUID ベースのデフォルト値を持たせる規約** とする（iOS: `id: UUID = UUID()`、Compose: `id: String = "<className>-${UUID.randomUUID()}"`）。DSL 経路で必ず `DSLReidentifiable.withDSLID(_:)` / `DSLReidentifiableCell.withDSLId(...)` により本仕様の優先順位に従う ID に rebind されるため、デフォルト UUID 値が最終 Cell ID として表面化することはない。これにより利用者は DSL 内で `LabelCell(title = "...")` のように `id` 引数省略で記述できる（後続 `add-cell-types-*` 系の具象 Cell 実装も本規約に従う）
- **NEW（オーナーレビュー対応）**: `DSLReidentifiable` / `DSLStyleModifiable`（iOS）および `DSLReidentifiableCell` / `DSLStyleModifiableCell`（Android）の定義モジュールを **Core モジュール**（`KsSettingsViewCore` / `ks-settingsview-core`）に移動する。これは後続 `add-cell-types-*` 系で具象 Cell が `KsSettingsViewUI` / `ks-settingsview-ui` モジュールに配置される際、`UI モジュール → SwiftUI/Compose モジュール` の循環依存を回避するため。両 OS で対称的な配置とする

### Bindingセル（SwiftUI `@Binding<T>` / Compose `MutableState<T>`）

- **NEW**: 双方向バインド対応 Cell（`SwitchCell` / `EntryCell` / `PickerCell` 等）のイニシャライザに Binding 引数版を追加：
  - SwiftUI: `SwitchCell("通知", isOn: $value)` 形式（`@Binding<Bool>` を保持）
  - Compose: `SwitchCell("通知", isOn = state)` 形式（`MutableState<Boolean>` を保持）
- **NEW**: `EntryCell` 等の高頻度更新パスは Native 側 200ms debounce + `updateCellValue(cellId:value:)` 直行パス（`add-partial-update-native` で導入予定の仕組み）を活用
- 注: 具象 Cell（`SwitchCell` / `EntryCell` / `PickerCell` 等）の Cell 型自体は別変更提案（`add-cell-types-*` 系）で導入される。本提案では「Binding 対応のイニシャライザ規約」と DSL 内での記述パターンを規定する

### Sample アプリの更新

- **MODIFIED**: iOS / Android Sample アプリのデモ画面を DSL 方式の例に書き換え：
  - DSL 方式での静的構成例
  - DSL 方式での動的構成例（`ForEach` 利用）
  - Bindingセルの例
  - Section H/F の任意 View 指定例
  - Root H/F の任意 View 指定例
  - Cell modifier の例
- Store 直接方式の Sample 例は別画面または別 Sample ファイルとして残置（用途別ガイドとして両方提示）

### ドキュメント

- **NEW**: `docs/declarative-dsl-guide.md`：SwiftUI / Compose DSL の利用ガイド
  - 基本的な記述パターン
  - DSL 方式と Store 方式の使い分け指針
  - ID 自動採番の仕組みと明示 ID 指定のタイミング
  - Bindingセルの使い方
  - パフォーマンス特性と無限スクロール時の Store 方式推奨

## Capabilities

### New Capabilities

（なし。本提案は既存 capability の Requirement 変更のみで完結する。新規 `declarative-dsl` capability は分離せず、SwiftUI / Compose ラッパの責務範囲内として `settings-view-ios-ui` / `settings-view-android-ui` の中で扱う）

### Modified Capabilities

- `settings-view-ios-ui`: `SwiftUI ラッパ KsSettingsView` Requirement に DSL init・modifier API・Bindingセル規約を追加。`SwiftUI DSL` Requirement を大幅拡張し、独自 `ForEach` 4 オーバーロード・Section/Cell Modifier・自動 ID 採番・明示 ID API・DSL→Diff 算出ロジックを規定。**BREAKING**: `add-partial-update-native` で導入される `.header(...)` / `.footer(...)` modifier は本提案で **削除** する（本ライブラリは運用前のため互換維持不要）。利用者は `.rootHeader(...)` / `.rootFooter(...)` のみを使用する。
- `settings-view-android-ui`: `Compose ラッパ KsSettingsView` Requirement に DSL receiver 版・任意 View Slot・自動 ID 採番を追加。`Compose DSL` Requirement を拡張し、`Section` / Cell Composable 風 API・独自 `forEach`（`key` lambda 版と `KsIdentifiable` 版併存）・**`SectionHandle` / `CellHandle` 経由 modifier chain**・**具象 Cell 型 DSL 拡張関数による Cell 直置き**・`operator fun Cell.unaryPlus()` による外部 Cell 値の DSL 注入・明示 ID API・DSL→Diff 算出ロジックを規定。Root H/F は Compose イディオム尊重で引数指定のまま維持（意図的な非対称）。**BREAKING**: `add-partial-update-native` で導入される `headerView` / `footerView` パラメータは本提案で **削除** し、`rootHeader` / `rootFooter` パラメータに改名・一本化する（本ライブラリは運用前のため互換維持不要）。

## Impact

### 影響範囲

- iOS:
  - 修正: `ios/Sources/KsSettingsViewSwiftUI/`（DSL init、Modifier API、独自 ForEach、Diff 算出ロジックを追加。`DSLReidentifiable` / `DSLStyleModifiable` protocol の定義を Core モジュールへ移動）
  - 修正: `ios/Sources/KsSettingsViewCore/`（オーナーレビュー対応で `DSLReidentifiable` / `DSLStyleModifiable` protocol を本モジュールへ移動・新規追加。後続 `add-cell-types-*` 系で `KsSettingsViewUI` 配置の具象 Cell が当該 protocol に準拠できるようにするため）
  - 修正: `samples/ios/`（DSL 方式 Sample 画面の追加・既存画面の整理、`DSLReidentifiable` / `DSLStyleModifiable` の import 先を Core に書き換え）
  - 無修正: `ios/Sources/KsSettingsViewUI/`（Native UI 層は本提案では変更なし。具象 Cell 自体の追加は後続 `add-cell-types-*` で対応）
- Android:
  - 修正: `android/ks-settingsview-compose/`（DSL receiver 拡張、Modifier API、独自 forEach、Diff 算出ロジックを追加。`DSLReidentifiableCell` / `DSLStyleModifiableCell` interface の定義を Core モジュールへ移動）
  - 修正: `android/ks-settingsview-core/`（オーナーレビュー対応で `DSLReidentifiableCell` / `DSLStyleModifiableCell` interface を本モジュールへ移動・新規追加。後続 `add-cell-types-*` 系で `ks-settingsview-ui` 配置の具象 Cell が当該 interface に準拠できるようにするため）
  - 修正: `samples/android/`（DSL 方式 Sample の追加・既存の整理、`DSLReidentifiableCell` / `DSLStyleModifiableCell` の import 先を Core に書き換え）
  - 無修正: `android/ks-settingsview-ui/`（Native UI 層は本提案では変更なし。具象 Cell 自体の追加は後続 `add-cell-types-*` で対応）
- MAUI:
  - 無修正: `maui/`（Bridge / Bindings / MAUI 本体は SwiftUI / Compose DSL とは独立した経路のため影響なし）
- ドキュメント:
  - 新規: `docs/declarative-dsl-guide.md`

### 依存関係

- 前提（archive 必須）:
  - `add-monorepo-foundation`（archive 済）
  - `add-settings-view-core`（archive 済）
  - `add-settings-view-ios-ui`（archive 済）
  - `add-settings-view-android-ui`（archive 済）
  - `add-partial-update-core`（in-progress、先行 archive 必須）
  - `add-partial-update-native`（in-progress、先行 archive 必須）
- 後続:
  - `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom`（in-progress、各 Cell の DSL イニシャライザと Modifier 拡張は当該提案側で追加）
  - `add-samples-ios` / `add-samples-android`（archive 済 Sample に対する DSL 方式の追加デモ画面）

### 既存 in-progress 提案との関係

- `add-partial-update-native` で SwiftUI ラッパは `init(store:)` のみ、Compose ラッパは `KsSettingsView(store:, ...)` のみとされているが、本提案で **DSL init を追加** する形で拡張する。`init(store:)` は破壊せずに維持する。
- **BREAKING**: `add-partial-update-native` で導入される SwiftUI ラッパの `.header(_ accessory: RootAccessory?)` / `.footer(_ accessory: RootAccessory?)` modifier、および Compose ラッパの `headerView` / `footerView` パラメータは、本提案で **削除** する。本ライブラリは運用前であり互換維持の必要がないため、新規 API `.rootHeader(...)` / `.rootFooter(...)` および `rootHeader` / `rootFooter` パラメータに一本化する。

### リスク

- **DSL → SettingsRootDiff 算出ロジックの複雑性**: 旧宣言ツリーと新宣言ツリーの比較で Section / Cell の同一性判定が正確である必要。Section 位置変更、Cell 位置変更、内容変更、追加削除を網羅するテストが必須。
  - 緩和策: 同一性判定戦略を仕様で厳密に規定し、各パターンの振る舞いシナリオを delta spec に網羅する。
- **ヘッダなし複数 Section の動的構造での同一性問題**: 既知の弱点として残る。
  - 緩和策: `ForEach` / `.sectionID(_:)` 明示指定をドキュメント指針として強調する。実装上はフォールバック挙動を明記する。
- **iOS / Compose の挙動差異**: 同じ仕様でも実装機構が違うため、振る舞いに微妙な差が生じる可能性。
  - 緩和策: Section / Cell の同一性判定アルゴリズム・Diff 算出ロジックの仕様を両 OS で共通に記述し、テストパターンを揃える。
- **`@StateObject` / `remember` で内部 Store を保持する設計**: SwiftUI / Compose のライフサイクル管理に依存し、View 再生成時の Store 再構築リスクがある。
  - 緩和策: `@StateObject` / `remember`（key 指定なし）を採用し、View identity が同じ間は Store を保持する設計を明文化する。
- **既存 archive 済 SwiftUI ラッパテストの再利用範囲**: `add-partial-update-native` のテスト基盤と統合する必要。
  - 緩和策: 既存 Store ベーステストはそのまま、DSL 方式専用の新規テストを追加する形で並存させる。
- **`@StateObject` 内蔵 Store が外部から見えない問題**: DSL 利用時に動的に Cell を追加したい場合、`@State` の配列を `ForEach` で展開する以外の手段がない。
  - 緩和策: DSL 方式は「宣言的なデータバインド」用途、命令型操作が必要な場合は Store 方式を使う、と明確に位置づける。両方の例をドキュメントに記載する。

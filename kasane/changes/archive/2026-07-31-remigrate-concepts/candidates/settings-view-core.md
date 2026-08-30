# Candidate: settings-view-core

## 概念候補

### 設定ツリーと Accessory (提案カテゴリ: core-model/)

`KsSettingsViewCore`（iOS）と `ks-settingsview-core`（Android）は、設定画面を `SettingsRoot → Section → Cell` の順序付きツリーとして表す。Core の責務は、描画可能な状態と変更語彙を値として UI 層へ渡すことであり、描画、Theme、CellStyle、具象 Cell の提供、状態の保持・適用は UI 層が担う。

#### 責務境界

- `SettingsRoot` は `sections` だけを保持する。Root Header / Footer と Theme は保持しない。
- `Section` は ID、任意の `SectionAccessory` header / footer、異種 Cell の順序、`headerHeight`、`isVisible` を保持する。空の Section も有効である。
- `KsCell`（iOS）/ `Cell`（Android）は Cell の最小共通契約である。iOS は `UUID` の `id` と `Hashable` / `Identifiable`、Android は `String` の `id` を要求する。具象 Cell とその style は UI 層に属する。
- `RootAccessory` と `SectionAccessory` はどちらも `text` / `view` を扱うが、Root と Section の役割を型で混同させないため別型である。`SettingsAccessory` は両者を置き換える公開モデルではなく、`SettingsRootDiff.updateAccessory` の payload を統一するためだけに使う。
- 任意 View は `KsAnyView` で受け渡す。iOS では SwiftUI `View` / UIKit `UIView`、Android では Compose content / Android `View` の二択である。このため Core の境界は「プラットフォーム型を含まない」ではなく、「描画と style の責務を持たない」と定義するのが現行コードに合う。

#### 保証すること

- `SettingsRoot.sections` と `Section.cells` は順序を保ち、空配列も受け付ける。
- `Section.headerHeight` の既定値 `-1` は自動高さ、正値は固定高さを意味する。`headerHeight = -1` かつ header が未設定または空 text の場合、UI 層は Section Header の supplementary 領域を生成しない。
- `Section.isVisible` の既定値は `true`。`false` の Section は model の元の位置に保持されたまま visible projection から header / footer / Cell ごと除外される。
- `SettingsRoot` と `Section` は値比較できる。`Section` の比較には ID、Accessory、Cell、`headerHeight`、`isVisible` が参加する。
- `RootAccessory.text` / `SectionAccessory.text` は文字列内容で比較する。`view` 同士は `KsAnyView` の内容を比較せず、ケースが同じなら等価とする。iOS の `KsAnyView` 自身は `Hashable` / `Equatable` ではなく、Android の `KsAnyView.Compose` / `AndroidView` は参照同一性のままである。
- Android の `Cell` は通常の `interface` であり、外部モジュールから独自 Cell を実装できる。DSL が ID を再束縛する Cell は `DSLReidentifiable`（iOS）/ `DSLReidentifiableCell`（Android）を実装する。
- `CellTitleAlignment` は論理方向の start / center / end を表す。Swift のケースは `.start` / `.center` / `.end`、Kotlin は `START` / `CENTER` / `END` である。

#### してはいけないこと

- Theme、CellStyle、KsImage、具象 Cell を Core モデルへ戻してはならない。
- Cell を Section / Root の Header・Footer として扱ってはならない。
- `KsAnyView` の内容変更を `SettingsRoot` / `Section` の値比較だけで検出できると仮定してはならない。任意 View の再描画は UI 層の責務である。
- Cell / Section の ID 重複を Core が検査すると仮定してはならない。一意性はモデル利用者が保証する。

#### 公開 API

主要な入口は `SettingsRoot`、`Section`、`KsCell` / `Cell`、`RootAccessory`、`SectionAccessory`、`KsAnyView` である。プラットフォーム間で ID 型が異なり、iOS の `Section.id` は `UUID()` が既定、Android の `Section.id` は呼び出し側の `String` 指定が必須である。iOS の `Section.cells` は `[any KsCell]`、Android は `List<Cell>` である。

#### 利用例

```swift
import KsSettingsViewCore

struct CustomCell: KsCell {
    let id: UUID
    let title: String
}

let root = SettingsRoot(sections: [
    Section(
        header: .text("一般"),
        cells: [CustomCell(id: UUID(), title: "通知")]
    )
])
```

```kotlin
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot

data class CustomCell(override val id: String, val title: String) : Cell

val root = SettingsRoot(
    sections = listOf(
        Section(
            id = "general",
            header = SectionAccessory.Text("一般"),
            cells = listOf(CustomCell(id = "notifications", title = "通知")),
        ),
    ),
)
```

出典: `ios/Sources/KsSettingsViewCore/SettingsRoot.swift` / `Section.swift` / `KsCell.swift` / `KsAnyView.swift` / `RootAccessory.swift` / `SectionAccessory.swift` / `DSLCellIdentity.swift` / `CellTitleAlignment.swift`、`ios/Tests/KsSettingsViewCoreTests/SettingsRootTests.swift` / `SectionTests.swift` / `SectionVisibilityTests.swift` / `KsAnyViewTests.swift` / `RootAccessoryTests.swift` / `SectionAccessoryTests.swift`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRoot.kt` / `Section.kt` / `Cell.kt` / `KsAnyView.kt` / `RootAccessory.kt` / `SectionAccessory.kt` / `DSLCellIdentity.kt` / `CellTitleAlignment.kt`、対応する Core JUnit、`openspec/specs/settings-view-core/spec.md` Purpose / 「SettingsRoot ドメインモデル」/「Section ドメインモデル」/「Cell 抽象」/ Accessory 関連 Requirements、`docs/core-model.md`

### 設定ツリーの構造変更 (提案カテゴリ: core-model/)

`SettingsRootDiff` は `SettingsRoot` を自ら保持・変更するオブジェクトではなく、UI 層または Store に「設定ツリーのどこへ、どの種類の変更を適用するか」を渡す値である。Swift は `Hashable` な enum、Kotlin は sealed interface と data class の組み合わせで同じ変更語彙を提供する。

#### 責務境界

- 全体差し替えは `full` / `Full` で表す。
- Section の追加・削除・同一階層内移動・全体置換は `insertSection` / `removeSection` / `moveSection` / `replaceSection`（Kotlin は PascalCase）で表す。
- Cell の追加・削除・同一 Section 内移動は `insertCell` / `removeCell` / `moveCell` で表す。
- 同一 ID の Cell の内容更新は `replaceCell` / `ReplaceCell` で表す。対象 ID と新 Cell の ID の一致は呼び出し側の責務である。
- Root / Section の Header・Footer 更新は `updateAccessory` / `UpdateAccessory` で表す。`AccessoryTarget` が位置を、`SettingsAccessory` が Root / Section の値種別を区別し、`nil` / `null` は削除を意味する。
- Core は変更意図を表現するだけで、対象探索、index の範囲調整、存在しない ID への対応、model の保持、visible projection、アニメーションは UI 層 / Store が担う。

#### 保証すること

- Cell ID を変える差し替えと Section 間の Cell 移動は、削除と追加の組み合わせで表現できる。
- `replaceCell` は同じ行の内容更新（reconfigure）を意味し、行 identity の変更を意味しない。
- `moveCell` は同一 Section 内の順序変更だけを意味する。
- `updateAccessory(..., nil)` / `UpdateAccessory(..., null)` は指定位置の Accessory 削除を曖昧なく表現する。
- Theme 更新は `SettingsRootDiff` に含まれず、UI 層の独立経路で扱う。

#### してはいけないこと

- ID が異なる新 Cell を `replaceCell` へ渡して identity 変更を表してはならない。
- Section 間移動を `moveCell` だけで表してはならない。
- Theme / style の変更を `SettingsRootDiff` の構造変更へ混ぜてはならない。
- `SettingsAccessory` を通常の Root / Section Accessory API の代替として使ってはならない。

#### 公開 API と利用例

次の例は同一 ID の Cell 内容更新を表す。ID を変更したい場合は `removeCell` と `insertCell` を使う。

```swift
let updated = CustomCell(id: oldCell.id, title: "通知（更新）")
let diff: SettingsRootDiff = .replaceCell(
    cellID: KsCellID(cell: oldCell),
    new: updated
)
```

```kotlin
val updated = oldCell.copy(title = "通知（更新）")
val diff: SettingsRootDiff = SettingsRootDiff.ReplaceCell(
    cellId = oldCell.id,
    newCell = updated,
)
```

iOS の Cell 対象は UUID を包む `KsCellID`、Android は `Cell.id` の `String` を直接使う。この型の非対称性は Bridge 実装時に明示的に変換する必要がある。

出典: `ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift` / `AccessoryTarget.swift` / `SettingsAccessory.swift` / `KsCellID.swift`、`ios/Tests/KsSettingsViewCoreTests/SettingsRootDiffTests.swift` / `AccessoryTargetTests.swift` / `SettingsAccessoryTests.swift` / `KsCellIDTests.swift`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiff.kt` / `AccessoryTarget.kt` / `SettingsAccessory.kt`、対応する Core JUnit、`openspec/specs/settings-view-core/spec.md` 「SettingsRootDiff 型」/「AccessoryTarget 型」/「SettingsAccessory 型」Requirements、`docs/core-model.md`

### 表示状態同期 (提案カテゴリ: architecture/)

設定画面の同期では、モデルの値等価、表示構造の identity、同一行の内容更新、可視性による表示射影を別の契約として扱う。これらを混同すると、内容変更で行が再生成されたり、hidden 要素が model から失われたり、連続更新で snapshot identity がずれたりする。

#### 責務境界

1. 構造同期は Section / Cell の ID だけで追加・削除・移動・identity 変更を追跡する。
2. 内容更新は同一 ID の Cell を破棄せず reconfigure する。`SettingsRootDiff.replaceCell` / `ReplaceCell` が明示更新経路である。
3. 可視性変化は hidden を含む model から visible projection を作り直し、表示構造上の追加・削除として反映する。
4. Theme はこの三分類とは独立した UI 状態であり、専用経路で反映する。

#### 保証すること

- Cell 自身の `Hashable` / `equals` は値比較のために内容を含める一方、構造 identity は内容を含めない。iOS の `KsCellID` は `KsCell.id` の UUID だけで等価性と hash を決め、同一 ID への連続内容更新でも安定する。Android の `CellListItemDiffCallback.areItemsTheSame` と stable item ID も Cell ID を基準にする。
- 同一 ID の内容更新は、iOS では `reconfigureItems`、Android では更新済み list の commit 後の `notifyItemChanged` により同一表示行を再構成する。
- `Section.isVisible = false` は Section 全体を visible projection から除外するが、`SettingsRoot.sections` の model には保持する。Cell の可視性は UI 層の `VisibilityAware` によって同様に射影され、非準拠の独自 Cell は visible として扱う。
- 可視性が切り替わる `replaceCell` / `replaceSection` は通常の内容 reconfigure へ押し込まず、visible projection を再構築する全体経路へ切り替える。

#### してはいけないこと

- Cell の title、checked 状態、style などを構造 identity の hash や `areItemsTheSame` に含めてはならない。
- 同一 ID の内容変更を削除＋追加や reload として扱ってはならない。
- hidden な Section / Cell を model から削除して可視性を表現してはならない。
- 可視性変更を通常の内容 reconfigure として処理してはならない。

出典: `ios/Sources/KsSettingsViewCore/KsCell.swift` / `KsCellID.swift` / `Section.swift` / `SettingsRootDiff.swift`、`ios/Tests/KsSettingsViewCoreTests/KsCellIDTests.swift` / `SectionVisibilityTests.swift`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、`ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift` / `VisibilityProjectionTests.swift`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt` / `Section.kt` / `SettingsRootDiff.kt`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt` / `KsSettingsView.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ApplyDiffTest.kt` / `VisibilityApplyDiffTest.kt`、`openspec/specs/settings-view-core/spec.md` Purpose / 「表示状態同期の三層分離」Requirement、`docs/architecture.md`

## ADR 候補

- 表示状態同期を「ID による構造同期」「同一行の内容 reconfigure」「model からの visible projection」に分離し、Theme をさらに独立経路とする — 出典: `openspec/specs/settings-view-core/spec.md` 「表示状態同期の三層分離」Requirement / `ios/Sources/KsSettingsViewCore/KsCellID.swift` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt`、選別基準: 能力・コンポーネント境界を越える、将来の決定を制約する、覆すコストが高い
- Root / Section Accessory を Cell と分離し、任意 View payload を値等価から除外する — 出典: `openspec/specs/settings-view-core/spec.md` 「KsAnyView 型消去ラッパ」/「RootAccessory 型」/「SectionAccessory 型」Requirements、選別基準: 能力・コンポーネント境界を越える、将来の決定を制約する
- Theme・CellStyle・具象 Cell を Core から分離し、Core の `SettingsRoot` と `SettingsRootDiff` に style 更新を持たせない — 出典: `openspec/specs/settings-view-core/spec.md` 「SettingsRoot ドメインモデル」/「Cell 抽象」/「スタイル系型の Core 不在」Requirements、選別基準: 能力・コンポーネント境界を越える、将来の決定を制約する
- Android の `Cell` を非 sealed の拡張可能 interface とし、未知の具象 Cell の扱いを UI 層の registry に委ねる — 出典: `openspec/specs/settings-view-core/spec.md` 「Cell 抽象」Requirement / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt`、選別基準: 能力・コンポーネント境界を越える、将来の決定を制約する

## drift 所見

- 旧 spec Purpose と docs は Core を「プラットフォーム固有型を含まない」「UIKit / Compose 等へ一切依存しない」と説明する一方、現行 `KsAnyView` の公開 API は iOS で SwiftUI `View` / UIKit `UIView`、Android で `@Composable` / Android `View` を保持する。描画・style の責務を持たない境界は維持されているが、「プラットフォーム型を含まない」という説明は現行コードと一致しない (`openspec/specs/settings-view-core/spec.md` Purpose / `docs/core-model.md` 冒頭 / `docs/architecture.md` §1 ↔ `ios/Sources/KsSettingsViewCore/KsAnyView.swift` / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/KsAnyView.kt`)。
- `docs/core-model.md` は iOS の `Section.cells` を `[AnyCell]` とし、Core が `AnyCell` 型消去ラッパを提供すると説明するが、現行コードは `[any KsCell]` を直接保持し `AnyCell` は存在しない (`docs/core-model.md` §2 / §6 ↔ `ios/Sources/KsSettingsViewCore/Section.swift` / `KsCell.swift`)。
- `docs/core-model.md` の Kotlin 例は `CellTitleAlignment { start, center, end }` とするが、現行公開ケースは `START` / `CENTER` / `END` である (`docs/core-model.md` §7 ↔ `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/CellTitleAlignment.kt`)。

## 用語

- SettingsRoot: Section の順序を保持する設定画面の最上位モデル。Theme と Root Header / Footer は保持しない。
- Section: Cell の順序、Section Header / Footer、header 高さ、可視性をまとめる区画。
- Cell: 設定値の表示・選択・編集を担う行。Core の共通契約は ID に限定し、具象型は UI 層または利用側が提供する。
- Accessory: Root または Section の Header / Footer に配置する text または任意 View。Cell とは別の責務を持つ。
- KsAnyView: Accessory 内で SwiftUI / UIKit または Compose / Android View を受け渡す型消去ラッパ。
- model: hidden な Section / Cell も保持する `SettingsRoot` の完全な状態。
- visible projection: model から可視な Section / Cell だけを取り出した描画用の派生状態。
- identity: 構造上の同じ Section / Cell を追跡する ID。同じ値かどうかを比較する `Hashable` / `equals` とは別契約。
- reconfigure: 同一 identity の表示行を破棄・再生成せず、内容だけを再構成すること。

## 抽出メモ

- 「設定ツリーと Accessory」は旧 `core-model/settings-tree.md` の後継候補だが、公開 API と利用例を補い、「プラットフォーム型を含まない」という旧説明を現行境界へ修正する必要がある。
- 「設定ツリーの構造変更」は旧 `core-model/structural-changes.md` の後継候補。10 ケースの逐語的な signature 一覧ではなく、長命な操作分類と `replaceCell` / `moveCell` / `updateAccessory` の意味論を残した。
- 「表示状態同期」は旧 `architecture/display-state-synchronization.md` と統合できる。旧文書の `update generation` は Android の複数 Cell 一括 `submitContentUpdate` に接地するが、settings-view-core 単独の公開契約ではないため本候補の用語から外した。指揮側で Android host 候補との重複を確認して粒度を決めるのがよい。
- `DSLReidentifiable` / `DSLReidentifiableCell` は Core に置かれた公開境界だが、同一性採番戦略の本体は SwiftUI / Compose capability 側にある。ここでは再束縛可能という契約だけを記し、詳細は宣言的 tree identity 候補へ合流するのがよい。
- `CellTitleAlignment` は公開 API だが単独概念にするほどの再導出コストは高くないため、設定ツリー候補へ含めた。具象 Cell の API 候補側から参照される可能性がある。

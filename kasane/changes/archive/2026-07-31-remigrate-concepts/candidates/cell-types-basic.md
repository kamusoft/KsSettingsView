# Candidate: cell-types-basic

## 概念候補

### 基本 Cell の意味と公開 API (提案カテゴリ: cells/)

`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` は、iOS と Android で対応する意味を持つ設定行の公開値モデルである。表示時点の内容と状態を保持し、Native control の寿命や画面全体の状態は所有しない。利用者操作は callback で外部へ通知し、利用者が更新後の値で Cell を再構築する。

#### 責務境界

- 7 種は UI 層に属し、iOS では `KsCell`、Android では `Cell` を実装する。Core の Cell 抽象は識別子だけを要求し、`style` / `isEnabled` / `isVisible` や各 Cell 固有状態は UI 層の具象型が持つ。
- `id` は直接構築時に自動採番される。iOS は `UUID()`、Android は Cell 種別の prefix と UUID を組み合わせた `String` である。宣言 DSL の安定 ID は Cell の自動採番とは別に rebind される。
- 全 7 種が `style`、`isEnabled`（既定 `true`）、`isVisible`（既定 `true`）を持つ。`isEnabled = false` は操作を止めて disabled 表示にし、`isVisible = false` はモデルを残して visible projection から外す。
- `title` は必須である。`ButtonCell` を除く 6 種は `description` / `valueText` / `icon` / `hintText` を持ち、`ButtonCell` は移植元との意味互換のため `description` を公開しない。
- `KsCellRegistry.registerBasicCells()`（iOS）/ `KsCellRegistry.registerBasicCells(context)`（Android）が 7 種を一括登録する。

#### 公開 API

| 型 | 意味と固有契約 |
|---|---|
| `LabelCell` | 読み取り専用表示。操作 control は持たない。 |
| `CommandCell` | 処理・遷移を要求する行。`onTap` を通知し、`hideArrow = false` の既定では Disclosure Indicator を表示する。 |
| `ButtonCell` | 明示的なボタン操作。`onTap`、`titleColor`、`titleAlignment`（既定 center）を持ち、Disclosure Indicator は表示しない。 |
| `SwitchCell` | `isOn` で ON/OFF を表示し、`onValueChanged(Bool/Boolean)` を通知する。 |
| `CheckboxCell` | 独立した二値 `isChecked` を角丸四角の checkbox で表し、タップ時に反転値を `onValueChanged` へ通知する。 |
| `RadioCell` | `groupId`、自身の `value`、共有された `selectedValue` を持ち、`value == selectedValue` のとき選択表示する。タップは `onSelected(value)` を通知し、グループ状態の更新は利用者が担う。 |
| `SimpleCheckCell` | 独立した `isChecked` を簡易 checkmark で表す。状態所有は `CheckboxCell` と同じ外部責務だが、公開する視覚的意味が異なる。 |

#### 保証すること

- 状態値（`isOn` / `isChecked` / `selectedValue` 等）と表示内容は値等価・hash の対象に含め、callback は対象から除外する。同一 callback インスタンスでなくても、同じ内容の Cell は同じ値として扱える。
- `isEnabled = false` では `onTap` / `onValueChanged` / `onSelected` を発火せず、control も操作不能になる。`LabelCell` は操作を持たないが disabled の視覚状態を反映する。
- `CheckboxCell` と `SimpleCheckCell` はタップ直後の Native 表示を反転して callback を通知する。ただし Cell モデル自体を内部で永続更新するわけではなく、次の描画値は利用者が供給する。
- `RadioCell` はタップした自身を即時に選択表示する。すでに `value == selectedValue` の行を再度タップした場合、Android 実装は `onSelected` を再通知しない。
- Compose DSL は 7 種を `Section { ... }` に直置きでき、戻り値 `CellHandle` に `.cellID(...)` / `.cellHeight(...)` 等を連結できる。`SwitchCell` には `MutableState<Boolean>` overload と値 + callback overload がある。他の二値・選択 Cell は現行では値 + callback で使う。
- Swift の `SectionBuilder` は `KsCell` 値を直接受ける。基本 Cell の現行 initializer は `title:` ラベル付きの値 + callback API であり、`Binding` overload は持たない。

#### してはいけないこと

- `ButtonCell` に `description` を渡してはならない。共通フィールドを理由にこの公開 API 例外を消してはならない。
- callback が Cell の状態を所有すると仮定してはならない。操作通知を受けた利用者が新しい状態値を供給する。
- `CheckboxCell`、`RadioCell`、`SimpleCheckCell` は印が似ていても交換可能ではない。独立二値、共有単一選択、簡易な独立選択という意味を保つ。

#### 利用例

```swift
@State private var notifications = false
@State private var theme = "light"

KsSettingsView {
    Section("一般") {
        LabelCell(title: "バージョン", valueText: "1.0.0")
        SwitchCell(
            title: "通知",
            isOn: notifications,
            onValueChanged: { notifications = $0 }
        )
        RadioCell(
            title: "ダーク",
            groupId: "theme",
            value: "dark",
            selectedValue: theme,
            onSelected: { theme = $0 }
        )
    }
}
```

```kotlin
val notifications = remember { mutableStateOf(false) }
var theme by remember { mutableStateOf("light") }

KsSettingsView {
    Section(header = "一般") {
        LabelCell(title = "バージョン", valueText = "1.0.0")
        SwitchCell(title = "通知", isOn = notifications)
        RadioCell(
            title = "ダーク",
            groupId = "theme",
            value = "dark",
            selectedValue = theme,
            onSelected = { theme = it },
        )
    }
}
```

出典: `ios/Sources/KsSettingsViewCore/KsCell.swift` / `ios/Sources/KsSettingsViewUI/{LabelCell,CommandCell,ButtonCell,SwitchCell,CheckboxCell,RadioCell,SimpleCheckCell}.swift` / `ios/Sources/KsSettingsViewSwiftUI/SectionBuilder.swift` / `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift` / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/{LabelCell,CommandCell,ButtonCell,SwitchCell,CheckboxCell,RadioCell,SimpleCheckCell}.kt` / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDsl.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt` / `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDslTest.kt` / `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/BasicCellsDemoScreen.kt` / `openspec/specs/cell-types-basic/spec.md` Purpose / `kasane/changes/remigrate-concepts/reference/old-concepts/cells/basic-cell-semantics.md` / `docs/cells.md`

### 入力 Cell の意味と公開 API (提案カテゴリ: cells/)

`EntryCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` は、基本 Cell と同じ値モデル・外部状態所有・共通表示フィールドの契約へ opt-in し、文字列、候補、数値、時刻、日付を編集する公開 API を提供する。旧 `openspec/specs/` には capability が確定反映されていないが、両プラットフォームの実装・テスト・Sample で現行製品 API として成立している。

#### 責務境界

- 5 種は UI 層の具象 Cell で、`style`、`title`、`description`、`icon`、`hintText`、`isEnabled`、`isVisible` を持つ。`PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` は `valueText` も持ち、明示値がなければ現在値から自動生成する。
- `EntryCell` は入力 control 自体が値を表示するため `valueText` を持たず、`text` を公開する。
- Store 経路は値 + callback で構築する。宣言 DSL は iOS では `Binding<T>` initializer、Android Compose では `MutableState<T>` 拡張関数を通じて TwoWay 更新を作る。
- 日付・時刻・keyboard はプラットフォームの Native 型を直接公開する。iOS は `Foundation.Date` / `UIKeyboardType`、Android は `LocalDate` / `LocalTime` / `android.text.InputType` の `Int` を使う。
- `KsCellRegistry.registerInputCells()`（iOS）/ `KsCellRegistry.registerInputCells(context)`（Android）が 5 種を一括登録する。

#### 公開 API

| 型 | 意味と固有契約 |
|---|---|
| `EntryCell` | `text`、`placeholder`、Native `keyboardType`、`isPassword`、`textAlignment`、`maxLength`、`onTextChanged`。`maxLength = nil/null` は無制限。 |
| `PickerCell` | `items` から `.single/Single` または `.multiple/Multiple` 選択を行う。単一は `selectedIndex`、複数は `selectedIndices` と `maxSelectedNumber`（`0` は無制限）を使う。`displayFormatter` と `pageTitle` を公開する。 |
| `NumberPickerCell` | `min`（既定 0）から `max`（既定 100）まで `step`（既定 1）刻みで `value` を選ぶ。iOS は `unit` suffix も公開するが、Android の現行 API には `unit` がない。 |
| `TimePickerCell` | iOS は `Date` の hour/minute、Android は `LocalTime` を値とし、`format`（既定 `HH:mm`）で表示する。 |
| `DatePickerCell` | iOS は `Date` の year/month/day、Android は `LocalDate` を値とし、`format`（既定 `yyyy/MM/dd`）と min/max を持つ。`uiStyle` の型名は共通だが、iOS は `.wheels/.calendar`、Android は `Material/Spinner` である。iOS は `todayText`、Android は `androidButtonColor` をプラットフォーム固有に公開する。 |

#### 保証すること

- 入力変更は対応 callback に通知され、Binding / MutableState 経路では元の状態へ書き戻される。
- `valueText` を明示した場合は自動表示より優先する。未指定時、`PickerCell` は選択項目（複数は index 順に `, ` 連結）、`NumberPickerCell` は数値、`TimePickerCell` / `DatePickerCell` は `format` 適用結果を表示する。
- `PickerCell` の複数選択は、`maxSelectedNumber > 0` のとき上限を超える追加選択を受け付けない。単一／複数の選択 index が範囲外なら、表示値の生成時にその項目を無視する。
- `NumberPickerCell` の `step <= 0` は描画側で 1 にフォールバックする。iOS は表示時に現在値を min/max 範囲へ clamp し、候補値へ合わせる。
- `EntryCell` は `maxLength` を超える入力を拒否し、`isPassword` を Native secure/password 入力へ反映する。iOS は keyboard 上部の Done toolbar と行タップによる focus、リスト drag による keyboard dismiss を提供する。
- `isEnabled = false` は入力 control、picker 起動、callback を無効にする。`isVisible = false` でも値モデルは保持される。
- 5 種の状態値・表示値は値等価・hash に含まれ、callback / formatter は除外される。

#### してはいけないこと

- `EntryCell` に `valueText` を追加して二重の入力値 API を作ってはならない。現在値は `text` が表す。
- iOS と Android の Native 日付・時刻型、keyboard 型、`DatePickerUIStyle` の case を同一型・同一 case と仮定してはならない。
- `PickerCell` の `selectionMode` と状態フィールドを混同してはならない。単一選択は `selectedIndex`、複数選択は `selectedIndices` を更新する。
- iOS 固有の `NumberPickerCell.unit`、`DatePickerCell.todayText` や Android 固有の `androidButtonColor` をクロスプラットフォーム共通引数として扱ってはならない。

#### 利用例

```swift
@State private var name = ""
@State private var themeIndex: Int? = 0
@State private var alarm = Date()

KsSettingsView {
    Section("入力") {
        EntryCell(title: "名前", text: $name, maxLength: 20)
        PickerCell(
            title: "テーマ",
            items: ["ライト", "ダーク"],
            selectedIndex: $themeIndex
        )
        TimePickerCell(title: "アラーム", time: $alarm)
    }
}
```

```kotlin
val name = remember { mutableStateOf("") }
val themeIndex = remember { mutableStateOf<Int?>(0) }
val alarm = remember { mutableStateOf(LocalTime.of(7, 0)) }

KsSettingsView {
    Section(header = "入力") {
        EntryCell(title = "名前", text = name, maxLength = 20)
        PickerCell(
            title = "テーマ",
            items = listOf("ライト", "ダーク"),
            selectedIndex = themeIndex,
        )
        TimePickerCell(title = "アラーム", time = alarm)
    }
}
```

出典: `ios/Sources/KsSettingsViewUI/{EntryCell,PickerCell,NumberPickerCell,TimePickerCell,DatePickerCell,PickerSelectionMode,DatePickerUIStyle}.swift` / `ios/Sources/KsSettingsViewUI/{EntryCellView,PickerCellView,PickerListViewController,NumberPickerCellView,TimePickerCellView,DatePickerCellView}.swift` / `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/{EntryCell,PickerCell,NumberPickerCell,TimePickerCell,DatePickerCell,PickerSelectionMode,DatePickerUIStyle}.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/{EntryCellViewHolder,PickerCellViewHolder,NumberPickerCellViewHolder,TimePickerCellViewHolder,DatePickerCellViewHolder}.kt` / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt` / `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDslTest.kt` / `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt` / `docs/legacy-aiforms-reference.md`

### Cell 用画像の公開契約 (提案カテゴリ: cells/)

`KsImage` は Cell の `icon` を UI 層へ渡す判別可能な値である。Core に画像表現を持ち込まず、名前・resource ID の値同一性と Native 画像オブジェクトの参照同一性を区別しながら、Cell の値比較へ参加させる。

#### 公開 API

- iOS `KsImage`: `.systemName(String)` と `.uiImage(UIImage)`。前者は SF Symbols として解決し、後者は渡された `UIImage` をそのまま描画する。
- Android `KsImage`: `Resource(@DrawableRes Int)`、`Drawable(android.graphics.drawable.Drawable)`、`SystemName(String)`。`Resource` と `SystemName` は値同一性、`Drawable` は参照同一性で比較する。

#### 保証すること

- `KsImage` は iOS `KsSettingsViewUI` / Android `ks-settingsview-ui` に属し、Core は依存しない。
- `icon = nil/null` または現在のプラットフォームで解決不能な icon は、安全に「icon なし」へフォールバックし、空の画像領域を残さない。
- Android の `SystemName` は API 対称性のため受理するが解決せず、非表示へフォールバックする。throw や描画失敗にはしない。

#### してはいけないこと

- Native 画像オブジェクトに内容等価を仮定し、pixel data の走査や暗黙の正規化を値比較へ持ち込んではならない。
- `KsImage` を Core の Cell 抽象へ移してはならない。
- `SystemName` が Android でも画像を描画すると仮定してはならない。

#### 利用例

```swift
LabelCell(title: "ストレージ", icon: .systemName("externaldrive"))
LabelCell(title: "画像", icon: .uiImage(customImage))
```

```kotlin
LabelCell(title = "ストレージ", icon = KsImage.Resource(R.drawable.ic_storage))
LabelCell(title = "画像", icon = KsImage.Drawable(customDrawable))
```

出典: `ios/Sources/KsSettingsViewUI/KsImage.swift` / `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsImage.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsImageTest.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt` / `openspec/specs/cell-types-basic/spec.md` Purpose・`KsImage` Requirement / `kasane/changes/remigrate-concepts/reference/old-concepts/cells/cell-image-boundary.md` / `docs/cells.md`

## ADR 候補

なし。基本 Cell の意味差、入力 Cell の Native 型、`ButtonCell.description` 例外は公開契約として概念候補に残す価値があるが、今回照合した確定 spec・旧 concepts からは、既存 ADR と重複せず選別 3 基準を通る新たな決定記録の原料を確認できなかった。

## drift 所見

- 旧 spec は Compose の全 Cell DSL 拡張関数が `id` 引数を公開すると記述するが、現行 `BasicCellDsl.kt` の 7 拡張関数は `id` 引数を持たず、安定 ID は戻り値の `.cellID(...)` で指定する。(`openspec/specs/cell-types-basic/spec.md`「Compose DSL 拡張関数による Cell 直置き」 / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDsl.kt`)
- 旧 spec の `RadioCell` 利用例には必須の `groupId` / `selectedValue` を省略した例が複数あり、現行 iOS / Android の public initializer・Compose DSL ではコンパイルできない。(`openspec/specs/cell-types-basic/spec.md`「既存呼び出しの互換性」「accentColor の Theme フォールバック」等 / `ios/Sources/KsSettingsViewUI/RadioCell.swift` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RadioCell.kt`)
- `docs/cells.md` と `docs/platform-guide-ios.md` は基本 Cell に `Binding` を直接渡す `SwitchCell(..., isOn: $state)` / `CheckboxCell(..., isChecked: $state)` / `RadioCell(..., selectedValue: $state)` / `SimpleCheckCell(..., isChecked: $state)` と、第一引数ラベルを省略する例を掲載している。現行基本 Cell は `title:` ラベル付きの値 + callback initializer だけを公開し、これらの `Binding` initializer はないため、掲載例はコンパイルできない。(`docs/cells.md` / `docs/platform-guide-ios.md` / `ios/Sources/KsSettingsViewUI/{SwitchCell,CheckboxCell,RadioCell,SimpleCheckCell}.swift` / `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift`)
- `docs/platform-guide-android.md` は `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` がいずれも `MutableState<T>` を読み書きすると説明するが、現行 Compose DSL で `MutableState<Boolean>` overload を持つのは `SwitchCell` だけである。残り 3 種は値 + callback API で状態を書き戻す。(`docs/platform-guide-android.md` §4 / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDsl.kt`)

## 用語

- 基本 Cell: 表示・操作・二値・単一選択を表す `LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` の 7 種。
- 入力 Cell: 文字列・候補・数値・時刻・日付を編集する `EntryCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` の 5 種。
- Store 経路: Cell の現在値と callback を直接構築し、更新モデルを `SettingsRootStore` へ渡す利用経路。
- TwoWay: Binding / MutableState から現在値を読み、Native UI の変更 callback で同じ状態所有者へ書き戻す宣言 DSL の利用契約。
- `valueText`: Cell のタイトル右側に表示する値文字列。入力 Cell では明示値が自動表示より優先する。
- `KsImage`: Cell の icon を表す UI 層の判別可能な値。

## 抽出メモ

- 基本 7 種の意味・状態所有と公開 API は同じ読者が同時に必要とするため、旧 `basic-cell-semantics.md` と `docs/cells.md` の API 水準を 1 候補へ統合した。
- 入力 5 種は実装・テスト・Sample で両プラットフォームの公開 API が成立している一方、`openspec/specs/` と旧 concepts に独立文書がない。基本 Cell へ埋め込むと API 表と利用例が過密になるため、独立した `cells/` reference 候補とした。
- `KsImage` は画像同一性と解決不能時フォールバックという独立した境界を持ち、旧 concept も独立していたため、基本 Cell の API 本文へ吸収せず独立候補を維持した。
- `docs/legacy-aiforms-reference.md` にある `TextPickerCell` は現行コードに存在しないため抽出対象に含めなかった。同文書の旧 `EntryCell.ValueText` や各既定値も、現行実装と一致する項目だけを由来確認に用いた。
- 入力 Cell に専用の確定 spec がないため、「なぜ Native 型を直接公開するか」などコードコメントより強い一次資料を今回の ADR 原料としては得られていない。統合時に ADR 化するなら、凍結 archive の `add-cell-types-input/design.md` を別途トリアージする必要がある。

# スタイル

色・フォント・寸法・list の外観と、Cell の周りの補助領域のレシピ。例はいずれも [SKILL.md](../SKILL.md) の最小動作コードと同じ import を前提とする。

描画値は Cell 種別の意味上の固有値 → `CellStyle` → `Theme` → UIKit 既定値の順で解決する。意味上の固有値とは、その Cell にとっての意味からその型が持っているフィールド — `ButtonCell.titleColor`、選択系・入力系 Cell の `accentColor` など — で、同じ属性を `CellStyle` で指定しても固有値が優先される。型は UIKit のものをそのまま使う (`UIColor` / `UIFont` / `CGFloat`)。`import SwiftUI` から UIKit が入らないファイルでは `import UIKit` が要る。

## 画面全体に Theme を適用する

`Theme` は画面全体の既定値を持つ。全パラメータに既定値があるので、変えるものだけを指定する。

```swift
let warmTheme = Theme(
    separatorColor: UIColor(red: 0.90, green: 0.85, blue: 0.73, alpha: 1.0),
    backgroundColor: UIColor(red: 0.95, green: 0.94, blue: 0.90, alpha: 1.0),
    cellAccentColor: UIColor(red: 1.0, green: 0.75, blue: 0.0, alpha: 1.0),
    cellTitleColor: UIColor(red: 0.8, green: 0.6, blue: 0.0, alpha: 1.0)
)

KsSettingsView {
    ksSection("General") {
        LabelCell(title: "Version", valueText: "1.0.0")
    }
}
.theme(warmTheme)
```

`backgroundColor` は list 全体の下地、`cellBackgroundColor` は Cell の背景であり、別の領域である。一方から他方を推論しない。

`Theme` のフィールドは以下がすべてで、並びは宣言順である。実引数もこの順に並べる。

| 分類 | フィールド | 型 | 未指定時 |
|---|---|---|---|
| list | `separatorColor` | `UIColor` | ライブラリ既定 |
| list | `backgroundColor` | `UIColor` | ライブラリ既定 |
| list | `cellBackgroundColor` | `UIColor` | `.white` |
| list | `selectedColor` | `UIColor` | ライブラリ既定 |
| list | `cellAccentColor` | `UIColor` | ライブラリ既定 |
| list | `disabledTextColor` | `UIColor` | ライブラリ既定 |
| list | `scrollIndicatorVisible` | `Bool` | `true` |
| Cell の高さ | `rowHeight` | `Int` | `-1` (自動) |
| Cell の高さ | `hasUnevenRows` | `Bool` | `true` |
| Header | `headerTextColor` | `UIColor` | ライブラリ既定 |
| Header | `headerBackgroundColor` | `UIColor` | ライブラリ既定 |
| Header | `headerFontSize` | `Double` | `-1` |
| Header | `headerFont` | `UIFont?` | `nil` |
| Header | `headerHeight` | `Double` | `-1` (自動) |
| Footer | `footerTextColor` | `UIColor` | ライブラリ既定 |
| Footer | `footerBackgroundColor` | `UIColor` | ライブラリ既定 |
| Footer | `footerFontSize` | `Double` | `-1` |
| Footer | `footerFont` | `UIFont?` | `nil` |
| Cell 既定 | `cellTitleColor` | `UIColor?` | `nil` |
| Cell 既定 | `cellTitleFont` | `UIFont?` | `nil` |
| Cell 既定 | `cellTitleFontSize` | `Double` | `-1` |
| Cell 既定 | `cellValueTextColor` | `UIColor?` | `nil` |
| Cell 既定 | `cellValueTextFont` | `UIFont?` | `nil` |
| Cell 既定 | `cellDescriptionColor` | `UIColor?` | `nil` |
| Cell 既定 | `cellDescriptionFont` | `UIFont?` | `nil` |
| Cell 既定 | `cellHintTextColor` | `UIColor?` | `nil` |
| Cell 既定 | `cellHintFont` | `UIFont?` | `nil` |
| Cell 既定 | `cellPlaceholderColor` | `UIColor?` | `nil` (OS 既定) |
| Cell 既定 | `cellIconSize` | `CGFloat?` | `nil` (24pt) |
| Cell 既定 | `cellIconRadius` | `CGFloat?` | `nil` (0pt) |
| Section の Container | `sectionMargin` | `NSDirectionalEdgeInsets?` | `nil` |
| Section の Container | `sectionCornerRadius` | `CGFloat?` | `nil` |
| Section の Container | `sectionBorderWidth` | `CGFloat?` | `nil` (Border なし) |
| Section の Container | `sectionBorderColor` | `UIColor?` | `nil` |

`cellTitleFontSize` は独立したサイズで、解決された title font の pointSize を上書きする。`headerFontSize` / `footerFontSize` も Header / Footer に対して同じ働きをする。3 つとも正の値のときだけ適用される。

## ライブラリ既定値を起点にする

上の「未指定時」のライブラリ既定は `Theme` の `public static` 定数として公開されている。属性を既定へ戻すときや、既定値から派生値を作るときに参照する。

| 定数 | 既定値の対象 |
|---|---|
| `defaultSeparatorColor` | 罫線色 |
| `defaultSelectedColor` | 選択中の Cell の背景色 |
| `defaultAccentColor` | アクセント色 |
| `defaultBackgroundColor` | list 背景色 |
| `defaultDisabledTextColor` | 無効時テキスト色 |
| `defaultHeaderBackgroundColor` | Header 背景色 |
| `defaultFooterBackgroundColor` | Footer 背景色 |
| `defaultHeaderTextColor` | Header テキスト色 |
| `defaultFooterTextColor` | Footer テキスト色 |
| `defaultHeaderFooterFont` | Header / Footer フォント |
| `defaultCellTitleColor` | Cell タイトル色 |
| `defaultCellTitleFont` | Cell タイトルフォント |
| `defaultCellDescriptionColor` | Cell 説明文色 |
| `defaultCellDescriptionFont` | Cell 説明文フォント |
| `defaultCellHintFont` | Cell ヒントフォント |
| `defaultButtonTitleColor` | ButtonCell タイトル色 |
| `defaultCellIconSize` | icon サイズ |
| `defaultCellIconRadius` | icon 角丸半径 |

## Cell 1 つだけ見た目を上書きする

`CellStyle` は Cell 1 つ分だけ Theme を上書きする。指定しないフィールドは `nil` のままで、Theme から継承する。

```swift
LabelCell(
    style: CellStyle(
        titleColor: .systemOrange,
        cellHeight: 80,
        backgroundColor: .secondarySystemGroupedBackground
    ),
    title: "Highlighted"
)
```

`style` は `title` 以降の Cell のフィールドより前に置く。`CellStyle` の中の実引数も以下の宣言順に並べる。

| フィールド | 型 |
|---|---|
| `titleColor` | `UIColor?` |
| `titleFont` | `UIFont?` |
| `descriptionColor` | `UIColor?` |
| `descriptionFont` | `UIFont?` |
| `valueTextColor` | `UIColor?` |
| `valueTextFont` | `UIFont?` |
| `iconSize` | `CGFloat?` |
| `iconRadius` | `CGFloat?` |
| `cellHeight` | `CGFloat?` |
| `hintTextColor` | `UIColor?` |
| `hintTextFont` | `UIFont?` |
| `backgroundColor` | `UIColor?` |
| `accentColor` | `UIColor?` |
| `placeholderColor` | `UIColor?` |

同名の Theme フィールドより先まで fallback する系統が 2 つある。`valueTextColor` / `valueTextFont` は `Theme.cellValueTextColor` / `cellValueTextFont` の次に title の色 / フォントを経て UIKit 既定に至る — `EntryCell` の入力中テキストもこの valueText の規則に従う。`hintTextColor` は `Theme.cellHintTextColor` の次に `Theme.cellAccentColor` へ落ちるため、何も指定しないヒントはアクセント色で描かれる。

## Cell に style modifier を連ねる

宣言ツリーでは同じ上書きを、Cell の copy を返す modifier として書ける。連鎖しても、それまでに指定した値と Cell の identity は維持される。

```swift
LabelCell(title: "Name")
    .titleColor(.systemOrange)
    .backgroundColor(.secondarySystemGroupedBackground)
    .font(.preferredFont(forTextStyle: .headline))
    .icon(.systemName("person"))
    .cellHeight(60)
```

使える modifier は `font` / `descriptionFont` / `iconSize` / `cellHeight` / `titleColor` / `backgroundColor` / `icon` / `disabled` / `cellID`。`descriptionFont` と `iconSize` は iOS のみで、Android の DSL に対応するものは無い。`disabled` は [cells.md](cells.md) のとおり全 Cell で no-op。

## Entry の placeholder に色を付ける

`EntryCell` の placeholder の文字色は `EntryCell.placeholderColor` → `CellStyle.placeholderColor` → `Theme.cellPlaceholderColor` → OS 既定の placeholder 色の順で解決する。OS 既定はダークモードに自動追従する。

```swift
@State private var nickname = ""

EntryCell(
    title: "Nickname",
    text: $nickname,
    placeholder: "Up to 20 characters",
    placeholderColor: .systemTeal
)
```

## Section の区切り方を切り替える (Classic の罫線 / Modern の角丸 Container)

style は `KsSettingsViewStyle` で、Section の区切り方を選ぶ。`.classic` は Cell と Section の境界を罫線で引くだけで、Cell は画面の全幅に並ぶ。`.modern` は Section の Cell だけを角丸の Container にまとめ、Section Header / Footer はその Container の外側に置く。style を切り替えても内容と識別子は変わらない。

```swift
KsSettingsView {
    ksSection("General") {
        LabelCell(title: "Version", valueText: "1.0.0")
    }
}
.style(.modern)
```

## Modern の Section Container を調整する

Container は Theme の 4 属性で決まる。未指定なら既定値へ解決し、既定では Border を描かない。

```swift
let boxedTheme = Theme(
    sectionMargin: NSDirectionalEdgeInsets(top: 22, leading: 16, bottom: 0, trailing: 16),
    sectionCornerRadius: 12,
    sectionBorderWidth: 1,
    sectionBorderColor: .separator
)
```

Container が覆うのは Section の Cell だけで、Section Header / Footer は Container の外側、画面全体の Header / Footer は装飾対象外である。`.classic` では Section が全幅になるため、`sectionMargin` の上下成分だけが効く。

## Cell の高さを決める

高さは `CellStyle.cellHeight` → `Theme.rowHeight` → platform の最低 Cell 高 48pt の順で解決する。`hasUnevenRows` が既定の `true` のときは解決値が最低高になり内容に応じて伸びる。`false` にすると全 Cell が固定される。

```swift
let compactTheme = Theme(rowHeight: 52, hasUnevenRows: false)
```

固定高では内容が Cell を押し広げないため、複数行のテキストが収まる高さを指定する。

## Section に Header / Footer を付ける

`ksSection` へ渡した文字列がそのまま Header と Footer になる。同じものを modifier で後から付けることもできる。

```swift
ksSection("Notifications", footer: "Also check the system settings.") {
    LabelCell(title: "Sound")
}
```

## Section Header に任意の SwiftUI View を置く

`sectionHeader` / `sectionFooter` は View builder も受ける。

```swift
ksSection {
    LabelCell(title: "Sound")
}
.sectionHeader {
    HStack {
        Image(systemName: "bell.fill")
        Text("Notifications").font(.headline)
    }
}
.sectionFooter("Also check the system settings.")
```

View 形式の Header は中身で比較されないため、クロージャの中を変えただけでは model の変更として検出されない。それでも差し替えたいときは Store でツリーを所有し、`store.updateAccessory(target:accessory:)` で明示的に送る (この API は中身を比較せずに更新を発行する。Store に無い `sectionID` を指す Section target は no-op、Root target は無条件に発行される)。中身の計測結果だけが変わった場合は `store.invalidateAccessoryMeasurement(target:)` を呼ぶとその領域だけ測り直される。

## Store や UIKit から Header に View を載せる

DSL の外 — `store.updateAccessory` や、UIKit の Controller の `rootHeader` / `rootFooter` — では、View は accessory の `.view(_:)` case が運ぶ `KsAnyView` として渡す。`KsAnyView.swiftUI { ... }` は SwiftUI の View builder を、`KsAnyView.uiKit { ... }` は `UIView` を返すファクトリをラップする。

```swift
store.updateAccessory(
    target: .rootFooter,
    accessory: .root(.view(.swiftUI {
        Text("Signed in as taro").font(.caption)
    }))
)
```

## 画面全体に Header / Footer を付ける

画面レベルの Header / Footer は設定ツリーではなく View 側が持ち、Modern の Section Container の装飾対象にはならない。

```swift
KsSettingsView {
    ksSection("General") {
        LabelCell(title: "Version", valueText: "1.0.0")
    }
}
.rootHeader("Welcome back")
.rootFooter {
    Text("© 2026 MyApp").font(.caption)
}
```

## 内容を保ったまま Section Header を隠す

`isHeaderVisible` / `isFooterVisible` は内容を保ったまま非表示にするため、隠れている間の更新は再表示時に現れる。内容がないものを表示させることはできない。

```swift
@State private var showHeaders = true

ksSection("General", isHeaderVisible: showHeaders) {
    LabelCell(title: "Version", valueText: "1.0.0")
}
```

## Section Header の高さを固定する

`headerHeight` は `-1` が自動高さ、正値が固定高さ。text か View かによらず Header にだけ適用され、固定高からはみ出す内容は clip される。

```swift
ksSection("General", headerHeight: 44) {
    LabelCell(title: "Version", valueText: "1.0.0")
}
```

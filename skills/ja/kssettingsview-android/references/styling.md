# スタイル

色・フォント・寸法・list 外観と、行の周りの補助領域のためのレシピ。このページの例はすべて以下の import を前提とする。スタイルの型と modifier は 2 つの package に分かれている — `Theme` / `CellStyle` / `KsImage` / `KsSettingsViewStyle` は `jp.kamusoft.kssettingsview.ui` に、Handle に chain する modifier は `jp.kamusoft.kssettingsview.compose` にある。

```kotlin
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.backgroundColor
import jp.kamusoft.kssettingsview.compose.cellHeight
import jp.kamusoft.kssettingsview.compose.font
import jp.kamusoft.kssettingsview.compose.icon
import jp.kamusoft.kssettingsview.compose.sectionFooter
import jp.kamusoft.kssettingsview.compose.sectionHeader
import jp.kamusoft.kssettingsview.compose.titleColor
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.KsImage
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.Theme
```

`dp` と `sp` は数値を寸法に変える拡張プロパティなので、生まれる型が `Dp` であっても `80.dp` を書くには `androidx.compose.ui.unit.dp` の import が要る。

値の解決順は、Cell 種別の意味上の固有値 → `CellStyle` → `Theme` → platform 既定値。Compose の型をそのまま使う (`androidx.compose.ui.graphics.Color` / `androidx.compose.ui.text.TextStyle` / `androidx.compose.ui.unit.Dp`)。最後の段はアプリのテーマではなく、ライブラリが同梱する Material3 派生テーマへ解決する。XML テーマも Compose の `MaterialTheme` もライブラリ UI の色を変えないので、見た目を調整する手段はこのページに書かれたものがすべてになる。同梱テーマは DayNight 派生のため、ライト / ダークは端末の夜間モードと uiMode 制御で決まる。

## 画面全体に Theme を適用する

`Theme` は画面全体の既定値を持つ。全パラメータに既定値があるので、変えるものだけ指定する。

```kotlin
val warmTheme = Theme(
    separatorColor = Color(0xFFE6D9BA),
    backgroundColor = Color(0xFFF2F0E6),
    cellAccentColor = Color(0xFFFFBF00),
    cellTitleColor = Color(0xFFCC9900),
)

KsSettingsView(theme = warmTheme) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

`backgroundColor` は list 全体の下地、`cellBackgroundColor` は行の背景を塗る。別々の領域なので、片方を指定してももう片方は決まらない。

`Theme` のフィールドは以下がすべてで、並びは宣言順である。`Theme` は data class なので、名前付き引数として順不同で渡し、変えないものは省略する。

| 分類 | フィールド | 型 | 未指定時 |
|---|---|---|---|
| List | `separatorColor` | `Color` | 組み込み既定値 |
| List | `backgroundColor` | `Color` | 組み込み既定値 |
| List | `cellBackgroundColor` | `Color` | `Color.White` |
| List | `selectedColor` | `Color` | 組み込み既定値 |
| List | `cellAccentColor` | `Color` | 組み込み既定値 |
| List | `disabledTextColor` | `Color` | 組み込み既定値 |
| List | `scrollIndicatorVisible` | `Boolean` | `true` |
| 高さ | `rowHeight` | `Int` | `-1` (自動) |
| 高さ | `hasUnevenRows` | `Boolean` | `true` |
| Header | `headerTextColor` | `Color` | 組み込み既定値 |
| Header | `headerBackgroundColor` | `Color` | 組み込み既定値 |
| Header | `headerFontSize` | `Double` | `-1.0` |
| Header | `headerFont` | `TextStyle?` | `null` |
| Header | `headerHeight` | `Double` | `-1.0` (自動) |
| Footer | `footerTextColor` | `Color` | 組み込み既定値 |
| Footer | `footerBackgroundColor` | `Color` | 組み込み既定値 |
| Footer | `footerFontSize` | `Double` | `-1.0` |
| Footer | `footerFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellTitleColor` | `Color?` | `null` |
| Cell 既定値 | `cellTitleFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellTitleFontSize` | `Double` | `-1.0` |
| Cell 既定値 | `cellValueTextColor` | `Color?` | `null` |
| Cell 既定値 | `cellValueTextFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellDescriptionColor` | `Color?` | `null` |
| Cell 既定値 | `cellDescriptionFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellHintTextColor` | `Color?` | `null` |
| Cell 既定値 | `cellHintFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellIconSize` | `Dp?` | `null` (24dp) |
| Cell 既定値 | `cellIconRadius` | `Dp?` | `null` (0dp) |
| Section の箱 | `sectionMargin` | `PaddingValues?` | `null` |
| Section の箱 | `sectionCornerRadius` | `Dp?` | `null` |
| Section の箱 | `sectionBorderWidth` | `Dp?` | `null` (ボーダーなし) |
| Section の箱 | `sectionBorderColor` | `Color?` | `null` |
| Cell 既定値 | `cellPlaceholderColor` | `Color?` | `null` (OS 既定) |

`cellTitleFontSize` は独立したサイズで、解決された title font のサイズを上書きする。`headerFontSize` / `footerFontSize` も Header / Footer に対して同じ働きをする。3 つとも正の値のときだけ適用される。

## Theme の既定値を参照する

表の「組み込み既定値」は `Theme` companion の public 定数として公開されている。既定へ戻すときや、既定値を基準に派生色を作るときに参照する。icon の 2 定数だけは色ではなく dp 値の `Float` で、残りは `Color`。

| 定数 | 既定値の対象 |
|---|---|
| `DEFAULT_SEPARATOR_COLOR` | 罫線色 |
| `DEFAULT_SELECTED_COLOR` | 選択中背景色 |
| `DEFAULT_ACCENT_COLOR` | アクセント色 |
| `DEFAULT_BACKGROUND_COLOR` | list 背景色 |
| `DEFAULT_DISABLED_TEXT_COLOR` | 無効時テキスト色 |
| `DEFAULT_HEADER_BACKGROUND_COLOR` | Header 背景色 |
| `DEFAULT_FOOTER_BACKGROUND_COLOR` | Footer 背景色 |
| `DEFAULT_HEADER_TEXT_COLOR` | Header テキスト色 |
| `DEFAULT_FOOTER_TEXT_COLOR` | Footer テキスト色 |
| `DEFAULT_CELL_TITLE_COLOR` | Cell タイトル色 |
| `DEFAULT_CELL_DESCRIPTION_COLOR` | Cell 説明文色 |
| `DEFAULT_BUTTON_TITLE_COLOR` | ButtonCell タイトル色 |
| `DEFAULT_CELL_ICON_SIZE_DP_VALUE` | icon サイズ (dp 値) |
| `DEFAULT_CELL_ICON_RADIUS_DP_VALUE` | icon 角丸半径 (dp 値) |

## 1 行だけ見た目を上書きする

`CellStyle` は 1 行に対して Theme を上書きする。指定しなかったフィールドは Theme から継承する。

```kotlin
LabelCell(
    title = "Highlighted",
    style = CellStyle(
        titleColor = Color(0xFFFF9500),
        backgroundColor = Color(0xFFFFF6E5),
        cellHeight = 80.dp,
    ),
)
```

`CellStyle` のフィールドは以下がすべてで、並びは宣言順である。すべて nullable で、`null` は「Theme から継承する」を意味する。

| フィールド | 型 |
|---|---|
| `titleColor` | `Color?` |
| `titleFont` | `TextStyle?` |
| `descriptionColor` | `Color?` |
| `descriptionFont` | `TextStyle?` |
| `valueTextColor` | `Color?` |
| `valueTextFont` | `TextStyle?` |
| `iconSize` | `Dp?` |
| `iconRadius` | `Dp?` |
| `cellHeight` | `Dp?` |
| `hintTextColor` | `Color?` |
| `hintTextFont` | `TextStyle?` |
| `backgroundColor` | `Color?` |
| `accentColor` | `Color?` |
| `placeholderColor` | `Color?` |

`placeholderColor` が意味を持つのは `EntryCell` だけで、解決順では Cell 側の `placeholderColor` 引数と `Theme.cellPlaceholderColor` の間に入る。

## Cell に style modifier を chain する

同じ上書きは、各 Cell 関数が返す `CellHandle` 上の modifier としても使える。chain しても先に指定した値と行の同一性は保たれる。

```kotlin
LabelCell(title = "Name")
    .titleColor(Color(0xFFFF9500))
    .backgroundColor(Color(0xFFFFF6E5))
    .font(TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
    .icon(KsImage.Resource(R.drawable.ic_person))
    .cellHeight(60.dp)
```

`CellHandle` で使える modifier は `font` / `cellHeight` / `titleColor` / `backgroundColor` / `icon` / `cellID` / `disabled`。`font` が変えるのは title のフォントだけで、hintText のフォントは変えない。`disabled` は行をそのまま返す no-op なので、行を無効化するには Cell 関数へ `isEnabled = false` を渡す。`SectionHandle` の側は代わりに `sectionHeader` / `sectionFooter` / `sectionID` を取る。

## Classic と Modern の list 外観を切り替える

`KsSettingsViewStyle.Classic` は細線で行を区切り、`Modern` は Section ごとに角丸の箱でまとめる。style を切り替えても内容と ID は変わらない。

```kotlin
KsSettingsView(style = KsSettingsViewStyle.Modern) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

## Modern の Section の箱を調整する

箱は Theme の 4 属性で決まる。未指定ならライブラリ既定へ解決し、既定ではボーダーを描かない。

```kotlin
val boxedTheme = Theme(
    sectionMargin = PaddingValues(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 0.dp),
    sectionCornerRadius = 12.dp,
    sectionBorderWidth = 1.dp,
    sectionBorderColor = Color(0xFFD0D0D0),
)
```

箱が覆うのは Section の Cell 行だけ。Section の Header / Footer は箱の外に置かれ、画面全体の Header / Footer は装飾対象にならない。`Classic` では `sectionMargin` の上下成分だけが効く (Classic の Section は全幅のため)。

## 行の高さを決める

高さは `CellStyle.cellHeight` → `Theme.rowHeight` → platform の最低値 60dp の順で解決する。この 2 つは書き方が違う。`CellStyle.cellHeight` は `Dp?` で `80.dp` を取り、`Theme.rowHeight` は dp を単位とする素の `Int` で未指定が `-1`、つまり `64` を取り `64.dp` は受け付けない。

`hasUnevenRows` が既定の `true` のままなら解決値は最低高として働き、内容に応じて伸びる。`false` にすると全行を固定する。

```kotlin
val compactTheme = Theme(rowHeight = 64, hasUnevenRows = false)
```

固定高では内容がはみ出しても行は伸びないので、複数行のテキストが入る高さを選ぶ。60dp は fallback であるだけでなく下限でもある。どちらの経路から解決した値でもこれを下回れば 60dp へ引き上げられるため、`Theme(rowHeight = 40)` としても行の高さは 60dp になる。

## 行のアイコンの大きさを決める

`CellStyle.iconSize` と `iconRadius` は 1 行分のアイコン枠の一辺と角の丸めを決め、`Theme.cellIconSize` と `cellIconRadius` が画面全体に同じことをする。4 つとも `Dp?` で、既定は 24dp 四方・角丸なし。

```kotlin
val avatarTheme = Theme(cellIconSize = 32.dp, cellIconRadius = 16.dp)
```

## Section に Header / Footer を付ける

`Section` に渡した文字列がそのまま Header / Footer になる。同じ値は Handle 上の modifier で後から付けることもできる。

```kotlin
Section(header = "Notifications", footer = "Also check the system settings.") {
    LabelCell(title = "Sound")
}
```

## Section の Header に任意の Compose を置く

`Section` は文字列用の `header` / `footer` と並んで、Composable 用の `headerContent` / `footerContent` を取る。

```kotlin
Section(
    headerContent = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null)
            Text(text = "Notifications", style = MaterialTheme.typography.titleMedium)
        }
    },
    footer = "Also check the system settings.",
) {
    LabelCell(title = "Sound")
}
```

`SectionHandle` 上の `sectionHeader` / `sectionFooter` modifier でも同じ選択ができる。どちらも文字列版と Composable 版の overload を持つ。

```kotlin
Section {
    LabelCell(title = "Sound")
}.sectionHeader {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Notifications, contentDescription = null)
        Text(text = "Notifications", style = MaterialTheme.typography.titleMedium)
    }
}.sectionFooter("Also check the system settings.")
```

同じ位置に文字列と Composable を同時指定することはできない。指定すると構築時に例外になる。

Composable の Header は内容で比較されないため、lambda の中身を変えただけでは model の変更として検出されない。新しい中身で高さも変わる場合は、画面を Store から駆動してその位置に `invalidateAccessoryMeasurement` を呼ぶ ([updates.md](updates.md) を参照)。

## 画面全体に Header / Footer を付ける

画面全体の Header / Footer は設定ツリーではなく View 側に属し、Modern の箱に覆われることはない。

```kotlin
KsSettingsView(
    rootHeader = { Text(text = "Welcome back") },
    rootFooter = { Text(text = "© 2026 MyApp", style = MaterialTheme.typography.bodySmall) },
) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

## 内容を消さずに Section の Header を隠す

`isHeaderVisible` / `isFooterVisible` は内容を保持したまま accessory を隠す。隠れている間に適用した更新は、再表示時に反映される。内容が無いものを表示させることはできない。

```kotlin
var showHeaders by remember { mutableStateOf(true) }

Section(header = "General", isHeaderVisible = showHeaders) {
    LabelCell(title = "Version", valueText = "1.0.0")
}
```

## Section の Header に固定高さを与える

`headerHeight` は dp を単位とする `Double` (`Dp` と `Int` に続く 3 つ目の書き方) で、`-1.0` が自動高さ、正値が固定高さ。適用されるのは Header だけで、中身が文字列でも Composable でも同じように効き、固定高さからはみ出す分は clip される。

```kotlin
Section(header = "General", headerHeight = 44.0) {
    LabelCell(title = "Version", valueText = "1.0.0")
}
```

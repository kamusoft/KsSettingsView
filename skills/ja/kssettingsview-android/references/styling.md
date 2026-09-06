# スタイル

色・フォント・寸法・list 外観と、Cell の周りの補助領域のためのレシピ。このページの例はすべて以下の import を前提とする。スタイルの型と modifier は 2 つの package に分かれている — `Theme` / `CellStyle` / `KsSettingsViewDefaults` / `KsImage` / `KsSettingsViewStyle` は `jp.kamusoft.kssettingsview.ui` に、Handle に chain する modifier は `jp.kamusoft.kssettingsview.compose` にある。

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
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
import jp.kamusoft.kssettingsview.compose.SwitchCell
import jp.kamusoft.kssettingsview.compose.backgroundColor
import jp.kamusoft.kssettingsview.compose.cellHeight
import jp.kamusoft.kssettingsview.compose.font
import jp.kamusoft.kssettingsview.compose.icon
import jp.kamusoft.kssettingsview.compose.sectionFooter
import jp.kamusoft.kssettingsview.compose.sectionHeader
import jp.kamusoft.kssettingsview.compose.titleColor
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.KsImage
import jp.kamusoft.kssettingsview.ui.KsSettingsViewDefaults
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.Theme
```

`dp` と `sp` は数値を寸法に変える拡張プロパティなので、生まれる型が `Dp` であっても `80.dp` を書くには `androidx.compose.ui.unit.dp` の import が要る。

値の解決順は、Cell 種別の意味上の固有値 → `CellStyle` → `Theme` → 現在の外観のライブラリ既定 → platform 既定値。Compose の型をそのまま使う (`androidx.compose.ui.graphics.Color` / `androidx.compose.ui.text.TextStyle` / `androidx.compose.ui.unit.Dp`)。

指定しなかった色は `Color.Unspecified` である。色の未指定はこの 1 流儀で表され、`Theme` / `CellStyle` の全色フィールドに加えて、Cell 種別が意味上の固有値として受け取る色引数の既定値もこれで、どれも nullable な `Color?` ではない (フォント・寸法・余白の未指定は `null` で表す)。未指定の色は描画時にライブラリ自身の light / dark の既定セットから埋められるので、`Theme` を渡さない画面もダークで判読できる。渡した色が別の値へ置き換わることは、どちらの外観でも起きない — その色に外観ごとの値を与える方法は以下のレシピで扱う。

platform 既定値の段まで落ちる場合、その先はアプリのテーマではなくライブラリが同梱する Material3 派生テーマである。XML テーマも Compose の `MaterialTheme` もライブラリ UI の色を変えないので、見た目を調整する手段はこのページに書かれたものがすべてになる。同梱テーマは DayNight 派生のため、ライト / ダークは端末の夜間モードと uiMode 制御で決まる。

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

`backgroundColor` は list 全体の下地、`cellBackgroundColor` は Cell の背景を塗る。別々の領域なので、片方を指定してももう片方は決まらない。

`Theme` のフィールドは以下がすべてで、並びは宣言順である。`Theme` は data class なので、名前付き引数として順不同で渡し、変えないものは省略する。

| 分類 | フィールド | 型 | 未指定のとき |
|---|---|---|---|
| List | `separatorColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| List | `backgroundColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| List | `cellBackgroundColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| List | `selectedColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| List | `cellAccentColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| List | `disabledTextColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| List | `scrollIndicatorVisible` | `Boolean` | `true` |
| 高さ | `rowHeight` | `Int` | `-1` (自動) |
| 高さ | `hasUnevenRows` | `Boolean` | `true` |
| Header | `headerTextColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| Header | `headerBackgroundColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| Header | `headerFontSize` | `Double` | `-1.0` |
| Header | `headerFont` | `TextStyle?` | `null` |
| Header | `headerHeight` | `Double` | `-1.0` (自動) |
| Footer | `footerTextColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| Footer | `footerBackgroundColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| Footer | `footerFontSize` | `Double` | `-1.0` |
| Footer | `footerFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellTitleColor` | `Color` | `Color.Unspecified` — 外観の既定 (Cell 種別ごと) |
| Cell 既定値 | `cellTitleFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellTitleFontSize` | `Double` | `-1.0` |
| Cell 既定値 | `cellValueTextColor` | `Color` | `Color.Unspecified` — title の色に従う |
| Cell 既定値 | `cellValueTextFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellDescriptionColor` | `Color` | `Color.Unspecified` — 外観の既定 |
| Cell 既定値 | `cellDescriptionFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellHintTextColor` | `Color` | `Color.Unspecified` — `cellAccentColor` に従う |
| Cell 既定値 | `cellHintFont` | `TextStyle?` | `null` |
| Cell 既定値 | `cellIconSize` | `Dp?` | `null` (24dp) |
| Cell 既定値 | `cellIconRadius` | `Dp?` | `null` (0dp) |
| Section の Container | `sectionMargin` | `PaddingValues?` | `null` |
| Section の Container | `sectionCornerRadius` | `Dp?` | `null` |
| Section の Container | `sectionBorderWidth` | `Dp?` | `null` (Border なし) |
| Section の Container | `sectionBorderColor` | `Color` | `Color.Unspecified` (透明) |
| Cell 既定値 | `cellPlaceholderColor` | `Color` | `Color.Unspecified` (OS 既定) |

`cellTitleFontSize` は独立したサイズで、解決された title font のサイズを上書きする。`headerFontSize` / `footerFontSize` も Header / Footer に対して同じ働きをする。3 つとも正の値のときだけ適用される。

title の色に「Cell 種別ごと」と書いてあるのは、`ButtonCell` のタイトルだけがタップできる見た目の色になり、他の Cell は通常の文字色になるためである。この既定は Theme に載せず Cell を描く時点で選ばれる。自分で `cellTitleColor` を指定するとこの区別は無くなり、`ButtonCell` も他と同じ色で描かれる。

## ライブラリの既定色を起点にする

`KsSettingsViewDefaults` はライブラリの既定色を、ロール名の付いた `Theme` として返す。既定値を基準に派生色を作るときや、既定色を保ったまま一部だけ変えるときに使う。`theme()` は現在の外観に対応するセットを選ぶ Composable 版で、`KsSettingsView` の `theme` 引数の既定値でもある。

```kotlin
val brandedTheme = KsSettingsViewDefaults.theme().copy(
    cellAccentColor = Color(0xFFFFBF00),
)

KsSettingsView(theme = brandedTheme) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

Composable の外 (ViewHolder や XML の View ホストを駆動する Activity) では `KsSettingsViewDefaults.theme(darkTheme = ...)` を使うか、`lightTheme()` / `darkTheme()` でセットを名指しする。

factory が返す `Theme` が値を持つのは、list 下地・Cell 背景・罫線・選択色・accent・無効時文字・Header / Footer の文字と背景の 10 色である。残りは返り値の中でも `Color.Unspecified` のままで、Cell を描く時点で決まる — title と description は外観から、valueText は title から、hintText は accent から、placeholder は同梱テーマから、Section の Border はどこからも取らず透明になる。端末の外観と食い違うセット (ライトの端末で `darkTheme()`) を渡すと、これら後から決まる色だけが端末側に従う。固定したい場合は返された `Theme` を `copy` して明示する。

`Theme` companion に色の定数は無くなった。残っているのは icon の枠を決める 2 つの `Float` の dp 値、`DEFAULT_CELL_ICON_SIZE_DP_VALUE` と `DEFAULT_CELL_ICON_RADIUS_DP_VALUE` だけである。

## ライト / ダークの色を自分で決める

両方の外観の色を自分で決めるなら、Theme を組み立てる場所で選ぶ。Compose では `isSystemInDarkTheme()`、View ホストでは `resources.configuration` の夜間モードを読む。

```kotlin
val theme = if (isSystemInDarkTheme()) {
    KsSettingsViewDefaults.darkTheme().copy(cellAccentColor = Color(0xFFFFD54F))
} else {
    KsSettingsViewDefaults.lightTheme().copy(cellAccentColor = Color(0xFFFFBF00))
}

KsSettingsView(theme = theme) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

未指定のままにした色は自動で外観に追随する。View ホストは足元で夜間モードが変わったときに未指定色を解決し直すので、`uiMode` を自前で処理して再生成されない Activity でも切り替わる。上の accent のように明示指定した色は利用者側の持ち物で、切り替えるならこの例のように自分で分岐する。

## Cell に明示した色にライト / ダークの値を与える

Cell に明示した色 — `CellStyle` のフィールド、または Cell 種別が意味上の固有値として受け取る色引数 — は、`Theme` の明示色と同じく、外観が変わっても書いたままの値で描かれる。宣言的 DSL では Cell を組み立てる場所で外観によって分岐する。夜間モードの変化に続く再 composition は内容更新として行へ届くので、行は作り直されずに新しい色で再 bind され、id もスクロール位置も保たれる。

```kotlin
val notifications = remember { mutableStateOf(true) }
val accent = if (isSystemInDarkTheme()) Color(0xFFFFD54F) else Color(0xFFFFBF00)

KsSettingsView {
    Section(header = "General") {
        LabelCell(
            title = "Version",
            valueText = "1.0.0",
            style = CellStyle(titleColor = accent),
        )
        SwitchCell(
            title = "Push notifications",
            isOn = notifications,
            accentColor = accent,
        )
    }
}
```

固有の色を持つ Cell は `ButtonCell` (`titleColor`)、`EntryCell` (`placeholderColor` / `accentColor`)、`DatePickerCell` (`androidButtonColor` / `accentColor`)、およびスイッチ・チェックボックス・簡易チェック・ラジオ・リスト選択・数値・時刻の各 Cell (`accentColor`)。`Color.Unspecified` のままにすると `CellStyle` → `Theme` → 現在の外観の既定へ順に倒れる。

宣言的 DSL の外側では、分岐を運ぶ再 composition が起きない。`uiMode` を自前の `configChanges` で処理する Activity は再生成もされないので、外観が変わったときに新しい色を載せた Cell を Store へ渡す — [updates.md](updates.md) を参照。

## Cell 1 つだけ見た目を上書きする

`CellStyle` は Cell 1 つに対して Theme を上書きする。指定しなかったフィールドは Theme から継承する。

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

`CellStyle` のフィールドは以下がすべてで、並びは宣言順である。すべて未指定にでき、未指定は「Theme から継承する」を意味する — 色は `Color.Unspecified`、フォントと寸法は `null`。

| フィールド | 型 | 未指定 |
|---|---|---|
| `titleColor` | `Color` | `Color.Unspecified` |
| `titleFont` | `TextStyle?` | `null` |
| `descriptionColor` | `Color` | `Color.Unspecified` |
| `descriptionFont` | `TextStyle?` | `null` |
| `valueTextColor` | `Color` | `Color.Unspecified` |
| `valueTextFont` | `TextStyle?` | `null` |
| `iconSize` | `Dp?` | `null` |
| `iconRadius` | `Dp?` | `null` |
| `cellHeight` | `Dp?` | `null` |
| `hintTextColor` | `Color` | `Color.Unspecified` |
| `hintTextFont` | `TextStyle?` | `null` |
| `backgroundColor` | `Color` | `Color.Unspecified` |
| `accentColor` | `Color` | `Color.Unspecified` |
| `placeholderColor` | `Color` | `Color.Unspecified` |

`placeholderColor` が意味を持つのは `EntryCell` だけで、解決順では Cell 側の `placeholderColor` 引数と `Theme.cellPlaceholderColor` の間に入る。

## Cell に style modifier を chain する

同じ上書きは、各 Cell 関数が返す `CellHandle` 上の modifier としても使える。chain しても先に指定した値と Cell の同一性は保たれる。

```kotlin
LabelCell(title = "Name")
    .titleColor(Color(0xFFFF9500))
    .backgroundColor(Color(0xFFFFF6E5))
    .font(TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
    .icon(KsImage.Resource(R.drawable.ic_person))
    .cellHeight(60.dp)
```

`CellHandle` で使える modifier は `font` / `cellHeight` / `titleColor` / `backgroundColor` / `icon` / `cellID` / `disabled`。`font` が変えるのは title のフォントだけで、hintText のフォントは変えない。`disabled` は Cell をそのまま返す no-op なので、Cell を無効化するには Cell 関数へ `isEnabled = false` を渡す。`SectionHandle` の側は代わりに `sectionHeader` / `sectionFooter` / `sectionID` を取る。

## Section の区切り方を切り替える (Classic の罫線 / Modern の角丸 Container)

`KsSettingsViewStyle` は Section の区切り方を選ぶ。`Classic` は Cell と Section の境界を細線で引くだけで、Cell は画面の全幅に並ぶ。`Modern` は Section の Cell だけを角丸の Container にまとめ、Section Header / Footer はその Container の外側に置く。style を切り替えても内容と ID は変わらない。

```kotlin
KsSettingsView(style = KsSettingsViewStyle.Modern) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

## Modern の Section Container を調整する

Container は Theme の 4 属性で決まる。未指定ならライブラリ既定へ解決し、既定では Border を描かない。

```kotlin
val boxedTheme = Theme(
    sectionMargin = PaddingValues(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 0.dp),
    sectionCornerRadius = 12.dp,
    sectionBorderWidth = 1.dp,
    sectionBorderColor = Color(0xFFD0D0D0),
)
```

Container が覆うのは Section の Cell だけ。Section の Header / Footer は Container の外に置かれ、画面全体の Header / Footer は装飾対象にならない。`Classic` では `sectionMargin` の上下成分だけが効く (Classic の Section は全幅のため)。

## Cell の高さを決める

高さは `CellStyle.cellHeight` → `Theme.rowHeight` → platform の最低値 60dp の順で解決する。この 2 つは書き方が違う。`CellStyle.cellHeight` は `Dp?` で `80.dp` を取り、`Theme.rowHeight` は dp を単位とする素の `Int` で未指定が `-1`、つまり `64` を取り `64.dp` は受け付けない。

`hasUnevenRows` が既定の `true` のままなら解決値は最低高として働き、内容に応じて伸びる。`false` にすると全 Cell を固定する。

```kotlin
val compactTheme = Theme(rowHeight = 64, hasUnevenRows = false)
```

固定高では内容がはみ出しても Cell は伸びないので、複数行のテキストが入る高さを選ぶ。60dp は fallback であるだけでなく下限でもある。どちらの経路から解決した値でもこれを下回れば 60dp へ引き上げられるため、`Theme(rowHeight = 40)` としても Cell の高さは 60dp になる。

## Cell のアイコンの大きさを決める

`CellStyle.iconSize` と `iconRadius` は Cell 1 つ分のアイコン枠の一辺と角の丸めを決め、`Theme.cellIconSize` と `cellIconRadius` が画面全体に同じことをする。4 つとも `Dp?` で、既定は 24dp 四方・角丸なし。

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

画面全体の Header / Footer は設定ツリーではなく View 側に属し、Modern の Section Container に覆われることはない。

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

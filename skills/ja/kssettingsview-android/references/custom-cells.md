# カスタム Cell

組み込み Cell では表せない内容のためのレシピ。まず `CustomCell` から始め、共通 Cell レイアウトとスタイル解決に参加させたいときだけ独自 Cell 型を定義する。

このページの Compose のレシピは以下の import を前提とする。`CustomCell` と modifier は `jp.kamusoft.kssettingsview.compose` の DSL の名前で、残りはすべて通常の Compose。

```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.compose.CustomCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.cellHeight
```

独自 Cell 型を定義するレシピは Compose ではなく素の Kotlin で、代わりに以下を要する。opt-in の 4 インターフェースは package が揃っていない点に注意する — `Cell` と `DSLReidentifiableCell` は `core`、`VisibilityAware` / `DSLStyleModifiableCell` / `DSLIconModifiableCell` は `ui` にある。

```kotlin
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.CellViewHolder
import jp.kamusoft.kssettingsview.ui.DSLStyleModifiableCell
import jp.kamusoft.kssettingsview.ui.KsCellRegistry
import jp.kamusoft.kssettingsview.ui.Theme
import jp.kamusoft.kssettingsview.ui.VisibilityAware
```

## 任意の Compose を Cell として表示する

`CustomCell` は任意の Composable をそのまま設定リストの 1 つの Cell として描く。ViewHolder を書く必要も、登録する必要もない。Cell が表示する値を `content` に渡し、builder の引数から Cell を組み立てる。

```kotlin
var volume by remember { mutableStateOf(50) }

KsSettingsView {
    Section(header = "Sound") {
        CustomCell(content = volume) { value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Volume")
                Slider(
                    value = value.toFloat(),
                    onValueChange = { volume = it.toInt() },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                )
                Text(text = value.toString())
            }
        }
    }
}
```

Cell の見た目に効く値は必ず `content` に入れる。`content` は `equals` / `hashCode` を正しく持つ非 null の型であること。builder と `onTap` の関数値は等価比較から除外されるため、キャプチャした値だけを変えても Cell は据え置かれる。

builder が描画されるのはアプリのテーマ上であり、ライブラリが自身の Cell を描く同梱テーマ上ではない。だから builder の中では `MaterialTheme` がいつもどおり効く — 裏返せば、カスタム Cell がライブラリの `Theme` の色を勝手に拾うことはない。

## データを持たない固定表示の Cell

変化するものを表示しない Cell では `content` を省略し、builder だけを渡す。

```kotlin
CustomCell {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, contentDescription = null)
        Text(text = "This screen is read only.")
    }
}
```

## タップ操作や Disclosure Indicator を付ける

`onTap` は Cell のタップで発火する (content の中の要素がタップを消費した場合を除く)。`showArrow` は `CommandCell` と同じ Disclosure Indicator を描き、両者は独立して指定できる。

```kotlin
CustomCell(content = planName, showArrow = true, onTap = { openPlans() }) { name ->
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Plan")
        Spacer(modifier = Modifier.weight(1f))
        Text(text = name)
    }
}
```

`isEnabled = false` は Cell のタップと content 内部の操作の両方を止め、content 全体を淡色化する。無効の間、content は TalkBack の読み上げ対象からも外れる。

## カスタム Cell の高さを決める

Cell は既定では content に合わせて伸びる。`cellHeight` は Theme の `hasUnevenRows` が `true` の間は最低高として働き、`false` にすると固定高になる。カスタム Cell に効く `CellStyle` は背景色と高さだけで、文字色やフォントは効かない。アイコン領域を持たないため `icon` は no-op。

```kotlin
CustomCell(content = message) { text ->
    Text(text = text, modifier = Modifier.fillMaxWidth().padding(16.dp))
}.cellHeight(120.dp)
```

content の中で `remember` に持たせた状態は、Cell が画面外へ出て戻る間に保持されることも失われることもある。またいで残したい値は `content` へ持ち上げる。

## CustomCell を再利用可能な Cell にする

複数画面で使い回すには、Cell を置く代わりに Cell を返す関数を書く。返るのは `jp.kamusoft.kssettingsview.ui` の `CustomCell` クラスで、これが DSL 関数の内側で組まれているもの。両者は名前を共有するため、このヘルパは専用のファイルに置き、そこでは DSL 側ではなく `jp.kamusoft.kssettingsview.ui.CustomCell` を import する。

```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import jp.kamusoft.kssettingsview.ui.CustomCell

data class SliderValue(val label: String, val value: Int)

fun SliderCell(
    label: String,
    value: Int,
    onValueChanged: ((Int) -> Unit)? = null,
): CustomCell<SliderValue> = CustomCell(
    content = SliderValue(label = label, value = value),
    builder = { content -> SliderRow(content = content, onValueChanged = onValueChanged) },
)

@Composable
private fun SliderRow(content: SliderValue, onValueChanged: ((Int) -> Unit)?) {
    var draft by remember(content) { mutableFloatStateOf(content.value.toFloat()) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = content.label)
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onValueChanged?.invoke(draft.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f),
        )
        Text(text = draft.toInt().toString())
    }
}
```

ドラッグ中はローカルの値で追従し、確定した時点で 1 回だけ外へ返すため、フレームごとの再バインドが起きない。

こうして作った Cell は — 次のレシピの独自 Cell 型も同じく — 組み上がった `Cell` を受け取る DSL のメンバ `cell(...)` で Section へ置く。返るのは組み込み Cell 関数と同じ `CellHandle` なので、style modifier もいつもどおり chain できる。`+cell` は同じ呼び出しの短縮形。

```kotlin
KsSettingsView {
    Section(header = "Sound") {
        cell(SliderCell(label = "Volume", value = 50))
        +SliderCell(label = "Balance", value = 50)
    }
}
```

## 独自の Cell 型と ViewHolder を定義する

独自の Cell 型は `Cell` を実装したクラス。`Cell` が要求するメンバは 1 つだけで、それ以外は何も求めない。

```kotlin
interface Cell {
    val id: String
}
```

そこから先はインターフェースを 1 つずつ足す opt-in になる。`isVisible` を尊重させたい場合は `VisibilityAware` を実装する。実装しない Cell は常に表示扱いになる。`style` も `Cell` の一部ではなく、このページ最後のレシピの `DSLStyleModifiableCell` と一緒に付いてくる。

```kotlin
data class ProgressCell(
    override val id: String,
    val title: String,
    val progress: Int,
    override val isVisible: Boolean = true,
) : Cell, VisibilityAware
```

ViewHolder は `CellViewHolder<T>` を継承する。bind のたびに最新の Cell と Theme を受け取り、`reset` は再利用時に前の Cell のものを解放する。

```kotlin
class ProgressCellViewHolder(view: View) : CellViewHolder<ProgressCell>(view) {

    private val titleView: TextView = view.findViewById(R.id.progress_cell_title)
    private val progressView: ProgressBar = view.findViewById(R.id.progress_cell_progress)

    override fun bind(cell: ProgressCell, theme: Theme) {
        titleView.text = cell.title
        titleView.setTextColor(
            (theme.cellTitleColor ?: Theme.DEFAULT_CELL_TITLE_COLOR).toArgb(),
        )
        progressView.progressTintList =
            ColorStateList.valueOf(theme.cellAccentColor.toArgb())
        progressView.progress = cell.progress
    }

    override fun reset() {
        titleView.text = null
        progressView.progress = 0
    }
}
```

inflate するレイアウトは利用者側のもので、ここでは上記 2 つの ID を持つ `TextView` と `ProgressBar` を置いている。表示前に組を登録する。`KsCellRegistry` はプロセス全体で共有する singleton なので、起動時に 1 回登録すれば全画面で有効になる。100 未満の viewType はライブラリの予約領域なので、その 100 を保持する定数 `KsCellRegistry.CELL_VIEW_TYPE_MIN` を起点にする。

```kotlin
KsCellRegistry.register(
    cellClass = ProgressCell::class,
    viewType = KsCellRegistry.CELL_VIEW_TYPE_MIN + 50,
) { parent ->
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.cell_progress, parent, false)
    ProgressCellViewHolder(view)
}
```

同じ Cell 型の再登録は factory を後勝ちで置き換える。別の Cell 型に同じ viewType を割り当てると例外になる。

## 未登録 Cell の扱いを決める

`strictMode` の既定値は `true` で、アプリの build 種別に自動追従しない。未登録の Cell は例外になる。release ビルドで高さ 0 の placeholder へ退避させたい場合は `false` を設定する。

```kotlin
KsCellRegistry.strictMode = BuildConfig.DEBUG
```

## 独自 Cell で DSL modifier を有効にする

modifier は copy を返す opt-in のインターフェース経由で動く。`cellID` で ID を再束縛させるには `DSLReidentifiableCell` (`jp.kamusoft.kssettingsview.core`)、style modifier には `DSLStyleModifiableCell` (`jp.kamusoft.kssettingsview.ui`)、`icon` には `DSLIconModifiableCell` (同じく `ui`) を実装する。`style` を連れてくるのが `DSLStyleModifiableCell` で、copy メソッドと並んで `val style: CellStyle` を宣言している。

以下は上の `ProgressCell` にこの 2 つのインターフェースを足したもので、先の版が持っていなかった `style` を持つようになっている。ViewHolder は変わらない。

```kotlin
data class ProgressCell(
    override val id: String = "progress-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val progress: Int,
    override val isVisible: Boolean = true,
) : Cell, VisibilityAware, DSLReidentifiableCell, DSLStyleModifiableCell {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
}
```

`DSLReidentifiableCell` が無い場合、`cellID` は ID を変えない。再評価をまたぐ ID の安定性は利用者側の責任になる。

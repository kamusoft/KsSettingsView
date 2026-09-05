# Custom cells

Recipes for content the built-in cells do not cover. Start with `CustomCell`; define your own cell type only when you need to take part in the shared cell layout and style resolution.

The Compose recipes on this page assume these imports. `CustomCell` and the modifiers are DSL names from `jp.kamusoft.kssettingsview.compose`; everything else is ordinary Compose.

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

The recipes that define a cell type of your own are plain Kotlin rather than Compose, and they need these instead. Note that the four opt-in interfaces are not all in one package: `Cell` and `DSLReidentifiableCell` are in `core`, while `VisibilityAware`, `DSLStyleModifiableCell` and `DSLIconModifiableCell` are in `ui`.

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

## Put arbitrary Compose into a cell of the list

`CustomCell` renders any Composable as one cell in the list, with no view holder to write and nothing to register. Pass the values the cell displays as `content` and build the cell from the builder argument.

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

Anything that affects what the cell shows must live in `content`, which needs a real `equals` and `hashCode` and must not be null. The builder and `onTap` lambdas are excluded from the comparison, so changing only a captured value leaves the cell as it was.

The builder renders with the theme of your app, not with the bundled theme the library draws its own cells from. That is what keeps `MaterialTheme` working inside it as usual - and it also means a custom cell does not pick up the library `Theme` colors on its own.

## Show a fixed cell with no data

When the cell displays nothing that changes, drop `content` and pass the builder alone.

```kotlin
CustomCell {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, contentDescription = null)
        Text(text = "This screen is read only.")
    }
}
```

## Add a tap action or a disclosure indicator

`onTap` fires when the cell is tapped, unless something inside the content consumed the tap. `showArrow` draws the same disclosure indicator as `CommandCell`, and the two are independent.

```kotlin
CustomCell(content = planName, showArrow = true, onTap = { openPlans() }) { name ->
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Plan")
        Spacer(modifier = Modifier.weight(1f))
        Text(text = name)
    }
}
```

`isEnabled = false` blocks both the cell tap and the controls inside the content, and dims the whole content. While it is disabled, the content is also dropped from the TalkBack tree.

## Set the height of a custom cell

The cell grows with its content by default. `cellHeight` acts as a minimum while the theme leaves `hasUnevenRows` at `true`, and as a fixed height once it is `false`. Only the background color and the height of `CellStyle` reach a custom cell; text colors and fonts do not, and `icon` is a no-op because the cell has no icon slot.

```kotlin
CustomCell(content = message) { text ->
    Text(text = text, modifier = Modifier.fillMaxWidth().padding(16.dp))
}.cellHeight(120.dp)
```

State held inside the content with `remember` may or may not survive the cell scrolling out of view and back, so anything that has to outlive that belongs in `content`.

## Make a reusable cell out of CustomCell

To reuse a cell across screens, write a function that returns a cell instead of placing one. The value it returns is the `CustomCell` class of `jp.kamusoft.kssettingsview.ui`, which is what the DSL function builds underneath. The two share the name, so put this helper in its own file and import `jp.kamusoft.kssettingsview.ui.CustomCell` there rather than the DSL one.

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

The cell keeps a local value while the slider is being dragged and reports it once the drag ends, so the cell is not rebound on every frame.

A cell built this way - and any cell type of your own, from the next recipe - goes into a section with `cell(...)`, the DSL member that accepts an already-built `Cell`. It returns the same `CellHandle` the built-in cell functions do, so the style modifiers chain onto it as usual. `+cell` is shorthand for the same call.

```kotlin
KsSettingsView {
    Section(header = "Sound") {
        cell(SliderCell(label = "Volume", value = 50))
        +SliderCell(label = "Balance", value = 50)
    }
}
```

## Define your own cell type and view holder

A cell type of your own is a class implementing `Cell`. `Cell` asks for one member and nothing else:

```kotlin
interface Cell {
    val id: String
}
```

Everything past that is opt-in, one interface at a time. Add `VisibilityAware` if the cell should honor `isVisible`; a cell that does not implement it is always treated as visible. `style` is not part of `Cell` either - it arrives with `DSLStyleModifiableCell` in the last recipe on this page.

```kotlin
data class ProgressCell(
    override val id: String,
    val title: String,
    val progress: Int,
    override val isVisible: Boolean = true,
) : Cell, VisibilityAware
```

The view holder extends `CellViewHolder<T>`. It receives the current cell and theme on every bind, and `reset` releases what belonged to the previous cell when the holder is recycled.

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

The layout it inflates is yours; here it holds a `TextView` and a `ProgressBar` carrying those two ids. Register the pair before the cell is displayed. `KsCellRegistry` is a process-wide singleton, so one registration at startup covers every screen. View types below 100 are reserved for the library, so start yours at `KsCellRegistry.CELL_VIEW_TYPE_MIN`, the constant holding that 100.

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

Registering the same cell type again replaces its factory, while giving the same view type to a different cell type throws.

## Decide what an unregistered cell does

`strictMode` is `true` by default and does not follow your build type on its own: an unregistered cell throws. Set it to `false` in release builds to fall back to a zero-height placeholder cell instead.

```kotlin
KsCellRegistry.strictMode = BuildConfig.DEBUG
```

## Support DSL modifiers on your own cell

Modifiers work through opt-in interfaces that return a copy. Implement `DSLReidentifiableCell` (in `jp.kamusoft.kssettingsview.core`) so `cellID` can rebind the id, `DSLStyleModifiableCell` (in `jp.kamusoft.kssettingsview.ui`) for the style modifiers, and `DSLIconModifiableCell` (also in `ui`) for `icon`. `DSLStyleModifiableCell` is what brings `style` with it: it declares `val style: CellStyle` as well as the copy method.

This is the same `ProgressCell` as above with those two interfaces added, so it now carries a `style` the earlier version did not have. Its view holder is unchanged.

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

Without `DSLReidentifiableCell`, `cellID` leaves the id untouched and you own the stability of the id across re-evaluations yourself.

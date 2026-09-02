# Custom cells

Recipes for rows the built-in cells do not cover. Start with `CustomCell`; define your own cell type only when you need a row that takes part in the shared row layout and style resolution. Every example assumes the imports from the minimal example in [SKILL.md](../SKILL.md).

## Put arbitrary SwiftUI into a row of the list

`CustomCell` renders any SwiftUI view as a row with no renderer to write and nothing to register. Pass the values the row displays as `content` and build the view from the builder argument.

```swift
@State private var volume: Double = 50

KsSettingsView {
    ksSection("Sound") {
        CustomCell(content: Int(volume)) { value in
            HStack {
                Image(systemName: "speaker.wave.2")
                Slider(value: $volume, in: 0...100)
                Text("\(value)")
            }
        }
    }
}
```

Anything that affects what the row shows must live in `content`, which needs to be `Hashable`. The builder and `onTap` closures are excluded from the comparison, so changing only a captured value leaves the row as it was.

## Show a fixed row with no data

When the row displays nothing that changes, drop `content` and pass the builder alone.

```swift
CustomCell {
    HStack {
        Image(systemName: "info.circle")
        Text("This screen is read only.")
    }
}
```

## Add a tap action or a disclosure indicator

`onTap` fires when the row is tapped, unless something inside the content consumed the tap. `showArrow` draws the same disclosure indicator as `CommandCell`, and the two are independent.

```swift
CustomCell(content: planName, showArrow: true, onTap: { openPlans() }) { name in
    HStack {
        Text("Plan")
        Spacer()
        Text(name).foregroundStyle(.secondary)
    }
}
```

`isEnabled: false` blocks both the row tap and the controls inside the content, and dims the whole content.

## Set the height of a custom row

The row grows with its content by default. `cellHeight` acts as a minimum while the theme leaves uneven rows enabled, and as a fixed height once they are disabled. Only the background color and the height of `CellStyle` reach a custom row; text colors and fonts do not. The `icon` modifier is a no-op here too, because a `CustomCell` has no icon area - draw the image inside the content instead.

```swift
CustomCell(content: message) { text in
    Text(text)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
}
.cellHeight(120)
```

## Make a reusable row out of CustomCell

To reuse a row across screens, wrap the `CustomCell` in a function. No registration is involved.

```swift
struct SliderValue: Hashable {
    let label: String
    let value: Int
}

func SliderCell(
    label: String,
    value: Int,
    onValueChanged: @escaping (Int) -> Void
) -> CustomCell {
    CustomCell(content: SliderValue(label: label, value: value)) { content in
        SliderRow(content: content, onValueChanged: onValueChanged)
    }
}

private struct SliderRow: View {
    let content: SliderValue
    let onValueChanged: (Int) -> Void

    @State private var draft: Double = 0
    @State private var isDragging = false

    private var shownValue: Double {
        isDragging ? draft : Double(content.value)
    }

    var body: some View {
        HStack(spacing: 12) {
            Text(content.label)
            Slider(
                value: Binding(
                    get: { self.shownValue },
                    set: { self.draft = $0 }
                ),
                in: 0...100,
                onEditingChanged: { editing in
                    if editing {
                        self.draft = Double(self.content.value)
                    } else {
                        self.onValueChanged(Int(self.draft))
                    }
                    self.isDragging = editing
                }
            )
            Text("\(Int(shownValue))")
        }
        .padding(.horizontal, 16)
    }
}
```

The row keeps a local value while the slider is being dragged and reports it once the drag ends, so the row is not rebound on every frame. Outside a drag it draws `content.value`, so a value pushed from elsewhere - a store update, for instance - still reaches the row.

A row built this way goes in like any built-in cell, and the same function serves as many screens and sections as you need. The modifiers that reach a `CustomCell` still chain onto it.

```swift
struct SoundSettingsView: View {
    @State private var volume = 30
    @State private var brightness = 70

    var body: some View {
        KsSettingsView {
            ksSection("Sound") {
                SliderCell(label: "Volume", value: volume) { newValue in
                    volume = newValue
                }
                .cellHeight(56)
            }
            ksSection("Display") {
                SliderCell(label: "Brightness", value: brightness) { newValue in
                    brightness = newValue
                }
            }
        }
    }
}
```

## Define your own cell type and renderer

A cell type of your own is a value conforming to `KsCell`, which requires exactly one member: `var id: UUID`. `KsCell` refines `Hashable`, `Identifiable` and `Sendable`, so make it a value type whose stored properties are all `Hashable` and `Sendable`. A `style: CellStyle` property is not part of the contract - add one only if you want the style modifiers, which is what `DSLStyleModifiable` below asks for. Add `VisibilityAware` if the row should honor `isVisible`.

```swift
struct ProgressCell: KsCell, VisibilityAware {
    let id: UUID
    let title: String
    let progress: Double
    let isVisible: Bool

    init(id: UUID = UUID(), title: String, progress: Double, isVisible: Bool = true) {
        self.id = id
        self.title = title
        self.progress = progress
        self.isVisible = isVisible
    }
}
```

The renderer is a `UICollectionViewCell` subclass of your own conforming to `KsCellRenderer` - the library's internal base class is not available to subclass. It receives the current cell and theme on every bind, and releases what belonged to the previous row on reuse.

```swift
final class ProgressCellView: UICollectionViewListCell, KsCellRenderer {
    private let titleLabel = UILabel()
    private let progressView = UIProgressView(progressViewStyle: .default)

    override init(frame: CGRect) {
        super.init(frame: frame)
        let stack = UIStackView(arrangedSubviews: [titleLabel, progressView])
        stack.axis = .vertical
        stack.spacing = 6
        stack.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: contentView.layoutMarginsGuide.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: contentView.layoutMarginsGuide.trailingAnchor),
            stack.topAnchor.constraint(equalTo: contentView.layoutMarginsGuide.topAnchor),
            stack.bottomAnchor.constraint(equalTo: contentView.layoutMarginsGuide.bottomAnchor)
        ])
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not available")
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        titleLabel.text = nil
        progressView.progress = 0
    }

    func render(cell: any KsCell, theme: Theme) {
        guard let cell = cell as? ProgressCell else { return }
        titleLabel.text = cell.title
        titleLabel.textColor = theme.cellTitleColor ?? Theme.defaultCellTitleColor
        progressView.progressTintColor = theme.cellAccentColor
        progressView.progress = Float(cell.progress)
    }
}
```

Register the pair into the shared registry before the row is displayed. This is the only route for the SwiftUI `KsSettingsView`, which has no registry parameter and always uses `KsCellRegistry.shared`.

```swift
KsCellRegistry.shared.register(
    cellType: ProgressCell.self,
    rendererType: ProgressCellView.self
)
```

An unregistered cell trips an assertion in debug builds and falls back to an empty placeholder row elsewhere.

## Use a registry of your own

A registry of your own is reachable through the UIKit host only. Automatic registration of the built-in cells applies to the shared registry and to nothing else, so a registry you inject starts empty regardless of the `autoRegisterBasicCells` / `autoRegisterInputCells` / `autoRegisterCustomCell` arguments of the controller, and you register everything the screen needs into it yourself.

`registerBasicCells()` covers `LabelCell`, `CommandCell`, `ButtonCell`, `SwitchCell`, `CheckboxCell`, `RadioCell` and `SimpleCheckCell`; `registerInputCells()` covers `EntryCell`, `PickerCell`, `NumberPickerCell`, `TimePickerCell` and `DatePickerCell`; `registerCustomCell()` covers `CustomCell`.

```swift
let registry = KsCellRegistry()
registry.registerBasicCells()
registry.registerInputCells()
registry.registerCustomCell()
registry.register(cellType: ProgressCell.self, rendererType: ProgressCellView.self)

let controller = KsSettingsViewController(store: store, registry: registry)
```

## Support DSL modifiers on your own cell

Modifiers work through opt-in protocols that return a copy. Conform to `DSLReidentifiable` (`withDSLID(_:) -> Self`) so `cellID` can rebind the identifier, to `DSLStyleModifiable` (`var style: CellStyle { get }` plus `withStyle(_:) -> Self`) for the style modifiers, and to `DSLIconModifiable` (`withIcon(_:) -> Self`) for `icon`.

```swift
extension ProgressCell: DSLReidentifiable {
    func withDSLID(_ id: UUID) -> ProgressCell {
        ProgressCell(
            id: id,
            title: title,
            progress: progress,
            isVisible: isVisible
        )
    }
}
```

Without `DSLReidentifiable`, `cellID` leaves the identifier untouched and you own the stability of the identifier across re-evaluations yourself.

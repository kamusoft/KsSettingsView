# カスタム Cell

組み込み Cell で足りない行のためのレシピ。まず `CustomCell` を試し、共通行レイアウトと style 解決に参加する行が必要なときだけ独自 Cell 型を定義する。例はいずれも [SKILL.md](../SKILL.md) の最小動作コードと同じ import を前提とする。

## 任意の SwiftUI View を行 (row) として表示する

`CustomCell` は任意の SwiftUI View を行として描く。Renderer を書く必要も登録も要らない。行が表示する値は `content` として渡し、View は builder の引数から組み立てる。

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

行の表示に効く値は必ず `content` に入れる (`Hashable` であること)。builder と `onTap` のクロージャは等価判定に参加しないため、キャプチャした値だけを変えても行は更新されない。

## データを持たない固定表示の行を置く

変化する表示がない行では `content` を省略し、builder だけを渡す。

```swift
CustomCell {
    HStack {
        Image(systemName: "info.circle")
        Text("This screen is read only.")
    }
}
```

## タップ操作や Disclosure Indicator を付ける

`onTap` は行タップで発火する (content 内の要素がタップを消費した場合は発火しない)。`showArrow` は `CommandCell` と同じ Disclosure Indicator を表示し、両者は独立に指定できる。

```swift
CustomCell(content: planName, showArrow: true, onTap: { openPlans() }) { name in
    HStack {
        Text("Plan")
        Spacer()
        Text(name).foregroundStyle(.secondary)
    }
}
```

`isEnabled: false` は行タップと content 内部の操作の両方を抑止し、content 全体を淡色化する。

## カスタム行の高さを指定する

行は既定で content の高さに追従する。`cellHeight` は Theme の可変高さが有効な間は最低高として働き、無効にすると固定高になる。カスタム行に効く `CellStyle` は背景色と高さだけで、文字色やフォントは効かない。`icon` modifier もここでは no-op である — `CustomCell` はアイコン領域を持たないため、画像は content の中に自分で描く。

```swift
CustomCell(content: message) { text in
    Text(text)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
}
.cellHeight(120)
```

## CustomCell を再利用可能な行にする

画面をまたいで使い回すには `CustomCell` を返す関数に包むだけでよい。登録は要らない。

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

ドラッグ中はローカルの値で追従し、ドラッグ確定時にだけ外へ返すことで、1 フレームごとの再バインドを避けている。ドラッグ中でないときは `content.value` を描くため、外から押し込まれた値 (Store 更新など) も行に届く。

こうして作った行は組み込み Cell と同じように置け、同じ関数を画面や Section をまたいで何度でも呼べる。`CustomCell` に効く modifier もそのまま chain できる。

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

## 独自の Cell 型と Renderer を定義する

独自 Cell 型は `KsCell` に準拠した値で、要求されるメンバは `var id: UUID` の 1 つだけである。`KsCell` は `Hashable` / `Identifiable` / `Sendable` を継承しているため、格納プロパティがすべて `Hashable` かつ `Sendable` な値型にする。`style: CellStyle` は契約に含まれない — 後述の `DSLStyleModifiable` が要求するので、style 系 modifier を効かせたい場合にだけ持たせる。行に `isVisible` を効かせたい場合は `VisibilityAware` も付ける。

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

Renderer は `KsCellRenderer` に準拠する自前の `UICollectionViewCell` サブクラスである (ライブラリ内部の基底クラスは継承できない)。bind のたびに現在の Cell と Theme を受け取り、再利用時には前の行に属するものを解放する。

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

行を表示する前に、この 2 つを対応付けて共有 Registry へ登録する。SwiftUI の `KsSettingsView` は registry の引数を持たず常に `KsCellRegistry.shared` を使うため、SwiftUI 経路ではこれが唯一の登録手段になる。

```swift
KsCellRegistry.shared.register(
    cellType: ProgressCell.self,
    rendererType: ProgressCellView.self
)
```

未登録の Cell は DEBUG ビルドでは assertion で検出され、それ以外では空の placeholder 行になる。

## 独自の Registry を使う

独自の Registry を使えるのは UIKit ホスト経由だけである。組み込み Cell の自動登録は共有 Registry に対してのみ行われるため、注入した Registry は `autoRegister...` 引数の値によらず空のまま始まる。その画面が使うものはすべて自分で登録する。

`registerBasicCells()` は `LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`、`registerInputCells()` は `EntryCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell`、`registerCustomCell()` は `CustomCell` を登録する。

```swift
let registry = KsCellRegistry()
registry.registerBasicCells()
registry.registerInputCells()
registry.registerCustomCell()
registry.register(cellType: ProgressCell.self, rendererType: ProgressCellView.self)

let controller = KsSettingsViewController(store: store, registry: registry)
```

## 独自 Cell に DSL modifier を効かせる

modifier は copy を返す opt-in の protocol を通して働く。`cellID` で ID を再束縛させるには `DSLReidentifiable` (`withDSLID(_:) -> Self`)、style 系 modifier には `DSLStyleModifiable` (`var style: CellStyle { get }` と `withStyle(_:) -> Self`)、`icon` には `DSLIconModifiable` (`withIcon(_:) -> Self`) に準拠する。

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

`DSLReidentifiable` に準拠しない Cell では `cellID` を呼んでも ID は書き換わらず、再評価をまたぐ ID の安定性は利用者自身が保証することになる。

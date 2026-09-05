# 表示中の画面の更新

表示中の設定画面を変えるためのレシピと、宣言ツリーの再評価をまたいで Cell を追跡するためのレシピ。import を自分で書いていないコードは [SKILL.md](../SKILL.md) の最小動作コードと同じ import を前提とする。

## Store で設定ツリーを所有する

画面の一部を命令的に変えたいとき — 大量データ、高頻度更新、ViewModel からの操作 — は `SettingsRootStore` を使う。Store を自分で保持し、`KsSettingsViewStyle` (`.classic` / `.modern`) と一緒に `KsSettingsView` へ渡す。Store とその操作はすべて main actor 隔離のため、Store を所有する型には `@MainActor` を付ける。

```swift
@MainActor
final class SettingsModel: ObservableObject {
    let generalSectionID: UUID
    let store: SettingsRootStore

    init() {
        let sectionID = UUID()
        let section = KsSettingsViewCore.Section(
            id: sectionID,
            header: .text("General"),
            cells: [LabelCell(title: "Version", valueText: "1.0.0")]
        )
        generalSectionID = sectionID
        store = SettingsRootStore(
            initialRoot: SettingsRoot(sections: [section]),
            initialTheme: Theme()
        )
    }
}

struct SettingsScreen: View {
    @StateObject private var model = SettingsModel()

    var body: some View {
        KsSettingsView(store: model.store, style: .classic)
    }
}
```

画面が出る前に Store へ加えた変更も表示へ反映されるため、「Store を作る → 操作する → 画面に出す」の順序を気にする必要はない。

`SettingsRootStore` の主な公開操作は次のとおり。よく使うものは以降のレシピで扱い、残りも同じ形で呼べる。

| 対象 | 操作 |
|---|---|
| Root 全体 | `replaceAll(_:)` |
| Section | `insertSection(_:at:)`、`removeSection(sectionID:)`、`moveSection(from:to:)`、`replaceSection(sectionID:new:)` |
| Cell | `insertCell(_:in:at:)`、`removeCell(cellID:)`、`replaceCell(cellID:new:)`、`replaceCells(_:)`、`moveCell(cellID:to:)` |
| Header / Footer | `updateAccessory(target:accessory:)`、`invalidateAccessoryMeasurement(target:)` |
| Theme | `applyTheme(_:)` |

以降の Store のレシピはこの `SettingsModel` のメンバとして書いてあり、`store` と `generalSectionID` はその 2 つのプロパティを指す。

## 表示後に Cell を追加・削除する

`insertCell` は Section の中へ Cell を置き、`removeCell` は Cell の識別子を受ける。この識別子は `KsCellID` で、Cell の `id` (`UUID`) だけをラップした値である — 内容がどう変わっても `id` が同じなら同じ Cell として扱われる。index は画面上の位置ではなく非表示要素を含む model 配列上の位置である。

```swift
func appendUser(_ name: String) {
    guard let section = store.root.sections.first else { return }
    store.insertCell(LabelCell(title: name), in: section.id, at: section.cells.count)
}

func removeLastUser() {
    guard let cell = store.root.sections.first?.cells.last else { return }
    store.removeCell(cellID: KsCellID(cell: cell))
}
```

対象の識別子が見つからない操作は、状態も変えず通知も行わない。

## Cell 1 つの内容を差し替える

`replaceCell` は Cell をその場で更新する。Cell は同一性と位置を保ち、削除・再挿入ではなく再構成として反映される。同じ識別子を持つ新しい Cell を渡す。

```swift
let updated = LabelCell(id: cell.id, title: "Version", valueText: "1.1.0")
store.replaceCell(cellID: KsCellID(cell: cell), new: updated)
```

新しい Cell は別の型でもよい — `LabelCell` を `SwitchCell` に差し替えるなど。Cell は同一性と位置を保ったまま、背後の Native cell だけが交換される。識別子そのものを変える場合は、削除と挿入で表す。

## 複数の Cell を 1 バッチで更新する

1 回の操作で複数の Cell が変わるとき (ラジオグループなど) はまとめて渡し、1 回の状態更新と 1 回の通知で反映させる。

```swift
store.replaceCells([
    (
        cellID: KsCellID(cell: lightRow),
        new: RadioCell(
            id: lightRow.id,
            title: "Light",
            groupId: "appearance",
            value: "light",
            selectedValue: "dark"
        )
    ),
    (
        cellID: KsCellID(cell: darkRow),
        new: RadioCell(
            id: darkRow.id,
            title: "Dark",
            groupId: "appearance",
            value: "dark",
            selectedValue: "dark"
        )
    )
])
```

未知の識別子はスキップされ、空リストは何もしない。

## Section や Cell を並べ替える

`moveSection` は全 Section 配列上の位置で動き、`moveCell` は Cell が属する Section を解決してその中で並べ替える。どちらも `to` は「対象をいったん取り除いた後の挿入位置」として解釈される。

```swift
store.moveSection(from: 2, to: 0)
store.moveCell(cellID: KsCellID(cell: cell), to: 0)
```

別の Section への移動は削除と挿入の組み合わせで表す。

## 表示後に Section の Header / Footer を変える

`updateAccessory` は `AccessoryTarget` の 4 つの位置 — `.rootHeader` / `.rootFooter` / `.sectionHeader(sectionID:)` / `.sectionFooter(sectionID:)` — のいずれかを指す。渡す値は `SettingsAccessory` で、`.section(_:)` は `SectionAccessory`、`.root(_:)` は `RootAccessory` を運び、どちらも `.text(_:)` か `.view(_:)` のいずれかである。`nil` を渡すとその位置の accessory を削除する。

```swift
store.updateAccessory(
    target: .sectionHeader(sectionID: generalSectionID),
    accessory: .section(.text("General settings"))
)
```

## 実行中に Theme を切り替える

Theme は設定ツリーの一部ではない。`applyTheme` は識別子と構造を変えずに色とフォントを変え、同値の Theme は再適用しない。

```swift
store.applyTheme(darkTheme)
```

宣言的な書き方では `.theme(_:)` modifier が同じ経路を通る。

## 再評価をまたいで Cell を追跡する

宣言ツリーは評価のたびに作り直されるため、動的なコレクションには key が要る。`Identifiable` の要素または `id:` KeyPath を受ける DSL の `ForEach` を使う。

```swift
struct Topic: Identifiable {
    let id: UUID
    let name: String
}

KsSettingsView {
    ksSection("Topics") {
        ForEach(topics) { topic in
            LabelCell(title: topic.name)
        }
    }
}
```

1 つの item からは 1 要素だけを返す。複数返すと同じ identity で衝突する。

## 要素に明示的な名前を付ける

意味のある識別子が要る静的な要素には `cellID` / `sectionID` を使う。同じ要素で `ForEach` の key と併用しない — identity の入力はどちらか一方にする。

```swift
ksSection("General") {
    LabelCell(title: "App version").cellID("app-version")
}
.sectionID("general")
```

## 状態から Cell の表示・非表示を切り替える

`isVisible` の切り替えは表示対象の集合を作り直すため、既存の Cell のその場更新ではなく、Cell の追加・削除として反映される。

```swift
@State private var showAdvanced = false

KsSettingsView {
    ksSection("General") {
        LabelCell(title: "Notifications")
        LabelCell(title: "API key", isVisible: showAdvanced)
    }
    ksSection("Diagnostics", isVisible: showAdvanced) {
        LabelCell(title: "Log level", valueText: "debug")
    }
}
```

## UIKit から画面を組み込む

`KsSettingsViewController` は素の `UIViewController` なので、push・present・子 ViewController としての埋め込みのいずれもできる。

```swift
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

final class SettingsContainerViewController: UIViewController {
    private let store = SettingsRootStore(
        initialRoot: SettingsRoot(sections: [
            Section(
                header: .text("General"),
                cells: [LabelCell(title: "Version", valueText: "1.0.0")]
            )
        ]),
        initialTheme: Theme()
    )

    override func viewDidLoad() {
        super.viewDidLoad()

        let settings = KsSettingsViewController(store: store, style: .classic)
        settings.rootHeader = .text("Profile")

        addChild(settings)
        settings.view.frame = view.bounds
        settings.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(settings.view)
        settings.didMove(toParent: self)
    }
}
```

Controller は設定ツリーの公開 setter を持たない。変更はすべて、生成時に渡した Store を経由する。

テストや独自ホスティング向けには、Store 方式の `KsSettingsView` (SwiftUI View) が `makeController()` を持ち、SwiftUI 階層の外で背後の `KsSettingsViewController` を生成できる。Store 方式専用で、DSL で組んだ View に対して呼ぶと `fatalError` になる。通常の SwiftUI 画面で必要になることはない。

## 変更を SettingsRootDiff として表す

上の Store の各操作の背後には `SettingsRootDiff` がある。変更の種類ごとに 1 case を持つ `Hashable` な enum で、case は `.full` / `.insertSection` / `.removeSection` / `.moveSection` / `.replaceSection` / `.insertCell` / `.removeCell` / `.replaceCell` / `.moveCell` / `.updateAccessory`。`KsSettingsViewController` にはこの値や Theme を Controller へ直接適用する `applyDiff(_:)` / `applyTheme(_:)` もある。

```swift
controller.applyDiff(.removeCell(cellID: KsCellID(cell: cell)))
controller.applyTheme(darkTheme)
```

直接適用 API は、Controller 生成時に渡した Store を迂回する。Store 接続中は Store が正であり、Store 操作と直接適用の併用は非保証 — 基本は Store 操作を使い、他所から Diff 値を受け取っている場合に直接適用を使う。

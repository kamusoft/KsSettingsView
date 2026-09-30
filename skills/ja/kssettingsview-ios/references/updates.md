# 表示中の画面の更新

表示中の設定画面を変えるためのレシピと、宣言ツリーの再評価をまたいで Cell を追跡するためのレシピ、コードから list をスクロールさせるためのレシピ。import を自分で書いていないコードは [SKILL.md](../SKILL.md) の最小動作コードと同じ import を前提とする。

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

Store は非表示の Section / Cell も model に保持するため、非表示中の更新は再表示時に現れる。Section 系 `updateAccessory` で未知の `sectionID` を指定した場合は no-op となり、通知も発行しない。Root Header / Footer target は異なり、Store 側の現在値を持たずに発行され、表示中の `rootHeader` / `rootFooter` は Controller が所有する。

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

`replaceSection` は Section 全体を差し替えるため、Header、Footer、Header の固定高さ、可視性、Cell のいずれも変えられる。軽量な Cell 編集としてではなく full 更新として扱い、局所的な変更が分かっているときは Cell または accessory の狭い操作を使う。

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

Root accessory は `SettingsRoot` の一部ではない。UIKit Controller の `rootHeader` / `rootFooter` に設定するか、[styling.md](styling.md) の Store 更新経路で target に指定する。Controller の view load 前に届いた値は初回表示のために保持されるが、後から Store snapshot だけで復元される値ではない。

## 実行中に Theme を切り替える

Theme は設定ツリーの一部ではない。`applyTheme` は識別子と構造を変えずに色とフォントを変え、同値の Theme は再適用しない。

```swift
store.applyTheme(darkTheme)
```

宣言的な書き方では `.theme(_:)` modifier が同じ経路を通る。

新しい Theme は表示中の Cell と、text 形式の Header / Footer へ届き、その場で色が塗り直される。View 形式の Header / Footer は意図的に対象外である — 再 bind すると View の factory が再実行され、hosted view が持っていた状態が失われるため。Theme に追随させたい View 形式の accessory は、`store.updateAccessory(target:accessory:)` で自分で差し替える。例外は画面全体の Header / Footer で、Theme の変更で Section の余白 (`sectionMargin`) の解決値が変わったときと、表示中の Section が 0 件と 1 件以上の間で切り替わったときは、余白を付け直すために種別を問わず作り直され、View 形式なら内部状態を失う。

`scrollIndicatorVisible` は初期表示時と Theme 変更時の両方で設定 list へ適用される。行を作り直さず、スクロール位置も変えない。Picker の候補 list は開いた時点の値を使い、ホイール型の選択面と `CustomCell` content が所有する list はこの設定の対象外である。

`applyTheme` が動かすのは画面全体の既定値だけである。Cell に明示した色 — `CellStyle` に渡した色と、その Cell 型が持つ色フィールドに渡した色 — は Theme の一部ではないため、この経路でも、ライト / ダーク外観が変わったときも、渡した値のまま残る。これらを外観に追随させたいときは、Cell を差し替えるのではなく dynamic な `UIColor` を渡す ([styling.md](styling.md) を参照)。自分で色を決める場合、識別子が同じ Cell でその色だけが変わった再評価は、削除と挿入ではなくその場の内容更新として届く (同じ識別子での `replaceCell` も同様)。

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

意味のある識別子が要る静的な要素には `cellID` / `sectionID` を使う。同じ要素で `ForEach` の key と併用しない — identity の入力はどちらか一方にする。併用すると明示 ID が採用されて key による追跡が効かなくなり、item ごとに変わらない明示 ID を付けた場合は全 item が同じ identity に解決される。

明示 ID も key も持たない Section は、テキストの Header が identity の入力に含まれるため、Header の文言を変えると identity も変わる。文言が変わり得る Section には `sectionID` を付ける。

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

Controller は view load 時に Store の現在の root、accessory、Theme へ収束するため、Controller 作成後から `viewDidLoad` 完了前までに加えた変更も初期表示へ反映される。`rootHeader` / `rootFooter` は Store の状態ではなく Host 所有のプロパティなので、Controller を作り直したときの再適用は呼び出し側の責務である。

テストや独自ホスティング向けには、Store 方式の `KsSettingsView` (SwiftUI View) が `makeController()` を持ち、SwiftUI 階層の外で背後の `KsSettingsViewController` を生成できる。Store 方式専用で、DSL で組んだ View に対して呼ぶと `fatalError` になる。通常の SwiftUI 画面で必要になることはない。

## Cell・Section・先頭・末尾へスクロールさせる

`KsScrollController` は、一度きりのスクロール命令を出すためのハンドルである。`@State` に持って同じインスタンスを渡し続け、Root modifier の `.scrollController(_:)` でつなぐ。DSL 方式・Store 方式のどちらでも使える。DSL 方式では、命令は `cellID` / `sectionID` で付けた明示 ID か `ForEach` の key で要素を指す。位置だけで identity が決まる静的な要素は指せないため、命令で指したい要素にはどちらかを付ける。同じ値が両方に当たるときは明示 ID の要素が採られる。

```swift
struct SettingsScreen: View {
    @State private var scroll = KsScrollController()
    @State private var topics = ["News"]

    var body: some View {
        KsSettingsView {
            ksSection("Actions") {
                ButtonCell(title: "Go to About", onTap: {
                    MainActor.assumeIsolated {
                        scroll.scrollToSection(id: "about")
                    }
                })
                ButtonCell(title: "Add a topic", onTap: {
                    MainActor.assumeIsolated {
                        let topic = "Topic \(topics.count + 1)"
                        topics.append(topic)
                        scroll.scrollTo(id: topic, position: .center)
                    }
                })
            }
            ksSection("Topics") {
                ForEach(topics, id: \.self) { topic in
                    LabelCell(title: topic)
                }
            }
            ksSection("About") {
                LabelCell(title: "Version", valueText: "1.0.0").cellID("version")
            }
            .sectionID("about")
        }
        .scrollController(scroll)
    }
}
```

命令は、同じ処理の中で行った更新が画面に反映された後に実行される。そのため 1 つの閉包の中で項目を足してそこへスクロールさせても、足した項目へ届く。

| 命令 | 対象 |
|---|---|
| `scrollTo(id:position:animated:)` | Cell の行 |
| `scrollToSection(id:position:animated:)` | Section の範囲 (Header・表示中の Cell・Footer) |
| `scrollToStart(animated:)` | 内容の最上端 (画面全体の Header を含む) |
| `scrollToEnd(animated:)` | 内容の最下端 (画面全体の Footer を含む) |

`position` は `KsScrollPosition` で既定は `.start`、`animated` の既定は `true`。

| `KsScrollPosition` | 合わせ方 |
|---|---|
| `.start` | 対象の上端を表示範囲の上端へ |
| `.center` | 対象の中央を表示範囲の中央へ |
| `.end` | 対象の下端を表示範囲の下端へ |

表示範囲は、セーフエリアと list の内容の余白を除いた領域である。届かない位置はスクロール可能範囲の端で止まり、表示範囲より高い対象は指定によらず `.start` に合わせる。続けて出した命令は呼んだ順に実行され、対象が見つかった最後の命令が最終位置を決める。非表示の要素、表示される要素が無い Section、存在しない ID への命令は何もしない (存在しない ID は debug ビルドで警告ログを出す)。アニメーション付きの命令は、利用者がドラッグを始めた時点で止まる。

Cell の通知の閉包は `@Sendable` だがメインスレッドで呼ばれるため、上の命令は `MainActor.assumeIsolated` の中で出している。ViewModel はハンドルを `KsScrollController` が準拠する protocol `any KsScrollControlling` として持てば、テストで命令を記録するだけの実装に差し替えられる。接続口が受けるのは `KsScrollController` そのものである。

## UIKit ホストをコードからスクロールさせる

`KsSettingsViewController` の `scrollController` にハンドルを代入する。命令は Cell を `KsCellID`、Section を `id` で指す。

```swift
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

final class TopicsViewController: UIViewController {
    private let topicsSectionID = UUID()
    private let scroll = KsScrollController()
    private lazy var store = SettingsRootStore(
        initialRoot: SettingsRoot(sections: [
            Section(id: topicsSectionID, header: .text("Topics"), cells: [])
        ]),
        initialTheme: Theme()
    )

    override func viewDidLoad() {
        super.viewDidLoad()

        let settings = KsSettingsViewController(store: store, style: .classic)
        settings.scrollController = scroll

        addChild(settings)
        settings.view.frame = view.bounds
        settings.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(settings.view)
        settings.didMove(toParent: self)
    }

    func addTopic(_ title: String) {
        let cell = LabelCell(title: title)
        let count = store.root.sections.first?.cells.count ?? 0
        store.insertCell(cell, in: topicsSectionID, at: count)
        scroll.scrollTo(id: KsCellID(cell: cell), position: .end)
    }

    func backToTop() {
        scroll.scrollToStart(animated: false)
    }
}
```

view の読み込み前や、Controller が window に取り付けられていない間に出した命令は、次のレイアウトの後に実行される。上に別の画面を push して Controller が生きたまま隠れている場合は、戻って表示されたときに命令の位置になっている。

1 つのハンドルが命令を届けるのは、最後につないだ画面だけで、別の画面へつなぐと前の画面には届かなくなる。ハンドルは Controller を保持しない。`nil` の代入・`disconnectStore()`・Controller の破棄で接続が外れ、以後の命令は何もしない。`disconnectStore()` では `scrollController` も `nil` に戻る。ハンドルを差し替えた・外したときは、その画面でまだ実行されていない命令を捨てる (SwiftUI の modifier で差し替えた場合も同じ)。

## UIKit ホストを作り直すときにスクロール位置を引き継ぐ

同じ Store から Controller を作り直しても、UIKit ホストはスクロール位置を自動では引き継がない。`captureScrollAnchor()` で中身を公開しない `KsScrollAnchor` として控え、新しい Controller の `restoreScrollAnchor(_:)` へ渡す。控えは座標ではなく表示範囲の上端にかかる要素を記録するため、その間に上側で項目が増減しても同じ要素の位置へ戻る。

```swift
let anchor = oldController.captureScrollAnchor()
oldController.disconnectStore()

let newController = KsSettingsViewController(store: store, style: .classic)
if let anchor {
    newController.restoreScrollAnchor(anchor)
}
newController.scrollController = scroll
```

控えるのは古い Controller が window にある間に行う。外れた後の `captureScrollAnchor()` は、控える内容が無いときと同じく `nil` を返す。戻しは新しい Controller の最初のレイアウトの後にアニメーションなしで行われ、その後に出した命令は戻しの後に実行されるので、命令が最終位置を決める。控えた要素が無くなっていれば戻しは何もしない。SwiftUI の `KsSettingsView` は View identity が続く間ホストを作り直さないため、この操作は要らない。

## 変更を SettingsRootDiff として表す

上の Store の各操作の背後には `SettingsRootDiff` がある。変更の種類ごとに 1 case を持つ `Hashable` な enum で、case は `.full` / `.insertSection` / `.removeSection` / `.moveSection` / `.replaceSection` / `.insertCell` / `.removeCell` / `.replaceCell` / `.moveCell` / `.updateAccessory`。`KsSettingsViewController` にはこの値や Theme を Controller へ直接適用する `applyDiff(_:)` / `applyTheme(_:)` もある。

```swift
controller.applyDiff(.removeCell(cellID: KsCellID(cell: cell)))
controller.applyTheme(darkTheme)
```

直接適用 API は、Controller 生成時に渡した Store を迂回する。Store 接続中は Store が正であり、Store 操作と直接適用の併用は非保証 — 基本は Store 操作を使い、他所から Diff 値を受け取っている場合に直接適用を使う。

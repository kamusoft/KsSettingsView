# 表示中の画面の更新

表示中の設定画面を変えるためのレシピと、宣言ツリーの再評価をまたいで行を追跡するためのレシピ。

このページには 2 つの書き方が出てくるが、1 つのファイルで混ぜることはできない。宣言側のコードは `jp.kamusoft.kssettingsview.compose` の DSL でツリーを組む。Store 側のコードは `jp.kamusoft.kssettingsview.ui` の Cell クラスと `jp.kamusoft.kssettingsview.core` のモデル型で組む。DSL 関数と Cell クラスは名前を共有しているため (`LabelCell` は両方にある)、1 つのファイルで両方をそのまま import することはできない。2 つの書き方はファイルを分けるか、`import jp.kamusoft.kssettingsview.ui.LabelCell as UiLabelCell` のように片方に別名を付ける。

宣言側のコードは以下の import を前提とする。

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import jp.kamusoft.kssettingsview.compose.KsIdentifiable
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.cellID
import jp.kamusoft.kssettingsview.compose.forEach
import jp.kamusoft.kssettingsview.compose.sectionID
```

Store 側のコードは以下を前提とする。

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.settingsRoot
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.RadioCell
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme
```

## Store で設定ツリーを所有する

表示中の画面を命令的に部分更新したいとき (大きなツリー、高頻度の更新、ViewModel からの操作) は `SettingsRootStore` を使う。初期ツリーは `settingsRoot` builder で作り、Store は Recomposition をまたいで保持し、`KsSettingsView` の Store overload へ渡す。

```kotlin
@Composable
fun SettingsScreen() {
    val store = remember {
        SettingsRootStore(
            initialRoot = settingsRoot {
                section(id = "general", header = "General") {
                    cell(LabelCell(id = "version", title = "Version", valueText = "1.0.0"))
                }
            },
            initialTheme = Theme(),
        )
    }

    KsSettingsView(store = store)
}
```

`settingsRoot` builder は明示 ID を取る純粋な関数で、ID を自前で解決する `KsSettingsView { ... }` DSL とは別のスコープ。builder の receiver は `SettingsRootScope` で、その `section` と、section ブロック内の `cell` は返り値を持たない。再評価 DSL 側の scope は `DSLSettingsRootScope` / `DSLSectionScope` で、こちらの `Section` / Cell 関数だけが `SectionHandle` / `CellHandle` を返す。画面が出る前に Store へ適用した変更も反映されるため、「Store を作る・変える・表示する」の順序を気にする必要はない。

Store の現在値は読み取り専用の `StateFlow` として公開されている — `store.state` が現在の `SettingsRoot`、`store.theme` が現在の `Theme`。ViewModel からの参照や、更新前の現在構造の確認に使える。

## 表示後に行を足す・消す

`insertCell` は Section の中へ行を挿し、`removeCell` は行の ID を取る。index は画面上の位置ではなく model 配列上の位置なので、非表示の行も数に入る。

```kotlin
store.insertCell(
    cell = LabelCell(id = "license", title = "License"),
    sectionId = "general",
    at = 1,
)
store.removeCell(cellId = "license")
```

対象 ID が存在しない操作は状態も通知も変えず、範囲外の挿入 index は有効範囲へ clamp される。

## 表示後に Section を足す・消す・差し替える

`insertSection` と `removeSection` は、`insertCell` / `removeCell` が行に対してするのと同じことを Section に対してする。`replaceSection` は位置を保ったまま Section を入れ替える。index は非表示の Section も含む `SettingsRoot.sections` 上の位置で、範囲外の値は行の操作と同じく clamp される。

```kotlin
store.insertSection(
    section = Section(
        id = "diagnostics",
        header = SectionAccessory.Text("Diagnostics"),
        cells = listOf(LabelCell(id = "log-level", title = "Log level", valueText = "debug")),
    ),
    at = 1,
)

store.replaceSection(
    sectionId = "diagnostics",
    new = Section(
        id = "diagnostics",
        header = SectionAccessory.Text("Diagnostics"),
        cells = listOf(LabelCell(id = "log-level", title = "Log level", valueText = "verbose")),
    ),
)

store.removeSection(sectionId = "diagnostics")
```

未知の Section ID は、行の操作と同じく状態も通知も変えない。差し替える Section には元と同じ ID を持たせること。ID を変えると、その下の行を Section 経由で指定できなくなる。

## 画面全体を作り直す

行ごとに当てていくのが割に合わない規模の変更 (アカウントの切り替え、画面全体が新しい応答で決まる場合など) では、`replaceAll` で新しいツリーを丸ごと渡す。

```kotlin
store.replaceAll(
    SettingsRoot(
        sections = listOf(
            Section(
                id = "general",
                header = SectionAccessory.Text("General"),
                cells = listOf(LabelCell(id = "version", title = "Version", valueText = "1.1.0")),
            ),
        ),
    ),
)
```

新旧のツリーに同じ ID が現れれば、その行はそのまま残る。概念的に同じ行には同じ ID を使い回すと、無駄な作り直しを避けられる。

## 1 行の内容を差し替える

`replaceCell` は行の同一性と ViewHolder を保ったまま内容を更新する。渡す Cell は同じ ID を持たせる。

```kotlin
store.replaceCell(
    cellId = "version",
    new = LabelCell(id = "version", title = "Version", valueText = "1.1.0"),
)
```

ID 自体を変えたい場合は、行を削除してから新しい行を挿入する。

## 複数行を 1 バッチで更新する

1 回の操作で複数行が変わるとき (ラジオグループなど) はまとめて渡し、1 回の状態更新・1 回の通知に収める。代わりに `replaceCell` をループで呼ぶのは等価ではない。呼び出しごとに再描画が予約され、後の呼び出しが、先の呼び出しがまだ待っていた再描画を捨ててしまうため、一部の行が古い内容のまま残る。

```kotlin
store.replaceCells(
    listOf(
        "appearance-light" to RadioCell(
            id = "appearance-light",
            title = "Light",
            groupId = "appearance",
            value = "light",
            selectedValue = "dark",
        ),
        "appearance-dark" to RadioCell(
            id = "appearance-dark",
            title = "Dark",
            groupId = "appearance",
            value = "dark",
            selectedValue = "dark",
        ),
    ),
)
```

未知の ID は読み飛ばされ、空リストは何もしない。

## Section と行を並べ替える

`moveSection` は全 Section 列上の位置で動く。`moveCell` は行が属する Section を自分で解決し、その中で並べ替える。どちらも `to` は「対象をいったん取り除いた後の挿入 index」として解釈される。

```kotlin
store.moveSection(from = 2, to = 0)
store.moveCell(cellId = "version", to = 0)
```

別の Section へ行を移す操作は、削除 + 挿入で表す。

## 表示後に Section の Header / Footer を変える

accessory とは Header または Footer のことで、1 つの画面にはその位置が 4 つある — 画面全体の Header と Footer、および 1 つの Section の Header と Footer。どれを指すかを表すのが `AccessoryTarget`。

```kotlin
AccessoryTarget.RootHeader
AccessoryTarget.RootFooter
AccessoryTarget.SectionHeader(sectionId = "general")
AccessoryTarget.SectionFooter(sectionId = "general")
```

そこへ置く中身は `SettingsAccessory` で、これは 2 種類のどちらを渡すかを表すだけの包み。画面全体の 2 位置には `SettingsAccessory.Root` が `RootAccessory` を包み、Section の 2 位置には `SettingsAccessory.Section` が `SectionAccessory` を包む。`RootAccessory` と `SectionAccessory` は別の型だが case は同じ 2 つ — 文字列の `Text(value)` と `KsAnyView` の `View(view)`。`KsAnyView` 自身も 2 択で、`KsAnyView.Compose` が `@Composable` lambda を、`KsAnyView.AndroidView` が `(Context) -> View` の factory を包む。

```kotlin
store.updateAccessory(
    target = AccessoryTarget.SectionHeader(sectionId = "general"),
    accessory = SettingsAccessory.Section(SectionAccessory.Text("General settings")),
)
```

accessory に `null` を渡すとその位置の中身を削除し、未知の Section ID は no-op になる。

## Composable の Header の大きさが変わったときに測り直す

`View` の accessory は描いている中身ではなく identity で比較されるため、Composable の Header を背の高い中身で描き直しても、高さが変わったことは list へ伝わらない (宣言側での同じ注意は [styling.md](styling.md) にある)。`invalidateAccessoryMeasurement` はその位置だけを測り直すよう求める。

```kotlin
store.invalidateAccessoryMeasurement(
    target = AccessoryTarget.SectionHeader(sectionId = "general"),
)
```

これは保持される状態ではなく一過性の通知で、その時点で Store に何も繋がっていなければ、後から再生されるのではなく捨てられる。

## 実行中に Theme を切り替える

Theme は設定ツリーの一部ではない。`applyTheme` は ID と構造に触れずに色とフォントを変え、同値の Theme は再適用しない。

```kotlin
store.applyTheme(darkTheme)
```

宣言側では `KsSettingsView` の `theme` 引数が同じ経路を通る。Store overload に `theme` 引数はないので、初期値は `SettingsRootStore(initialTheme = ...)`、以後の変更は `applyTheme` を使う。

## 再評価をまたいで行を追跡する

宣言ツリーは Recomposition のたびに作り直されるため、動的コレクションには key が要る。DSL の `forEach` に、item ごとに区別が付く値を返す lambda として渡す。

```kotlin
KsSettingsView {
    Section(header = "Topics") {
        forEach(topics, key = { topic -> topic.name }) { topic ->
            LabelCell(title = topic.name)
        }
    }
}
```

要素の型が `KsIdentifiable` を実装していれば key lambda は省略できる。`KsIdentifiable` の唯一のメンバは `val id: Any` で、DSL は key 同士を比較するだけなので `Int`・`String`・value class など何でも使える。

```kotlin
data class Topic(override val id: Int, val name: String) : KsIdentifiable

KsSettingsView {
    Section(header = "Topics") {
        forEach(topics) { topic ->
            LabelCell(title = topic.name)
        }
    }
}
```

1 つの item からは要素をちょうど 1 つだけ返す。複数返すと同じ identity で衝突する。

## 要素に明示的な名前を付ける

意味上の名前で追跡したい静的要素には `cellID` / `sectionID` を chain する。同じ要素で `forEach` の key と併用しないこと — identity の入力はどちらか一方に決める。渡した文字列は安定 ID を導く hint であり、最終的な ID そのものではない。

```kotlin
KsSettingsView {
    Section(header = "General") {
        LabelCell(title = "App version").cellID("app-version")
    }.sectionID("general")
}
```

安定 ID は `DatePickerCell` (`uiStyle = Material`) のカレンダーダイアログでも効いてくる。ダイアログは回転後に選択状態を保ったまま再表示されるが、それは Activity 再生成の前後で行の ID が同じときに限られる — 一致しなければ閉じたままになり、どこにも値を書き込まない。ボトムシート系のピッカー (Picker・NumberPicker・TimePicker・Spinner 形式の日付) は ID に関わらず回転で閉じる。

## 2 種類の識別子を区別する

宣言側の識別子と Store が取る識別子は別物で、取り違えることが「更新したのに何も起きない」の主な原因になる。

- DSL で利用者が渡すもの (`forEach` の key、`KsIdentifiable.id`、`cellID` に渡す文字列) は型 `Any` の hint。DSL はそこから安定 ID を導き、その導出結果を `Cell.id` に入れる。導出は internal なので、`.cellID("app-version")` は `"app-version"` という ID を生まないし、実際に生まれる値を利用者が再現することもできない。
- Store が行と Section を指すのは、`settingsRoot { }` や `SettingsRoot` / `Section` / Cell クラスでツリーを組むときに利用者自身が書いた `String` の ID。`removeCell` / `replaceCell` / `moveCell` / `removeSection` などが期待するのはこちら。

そもそも実行時にこの 2 つが出会うこともない。`KsSettingsView { ... }` の overload は Store を内部で生成して所有し、外へは一切公開しない。`KsSettingsView(store = ...)` の overload は DSL ブロックを取らない。つまり DSL で書いた画面を Store から操作することはできない。Store 操作をしたいなら、明示 ID でツリーを組んで Store overload を使う。

## 状態から行の表示・非表示を切り替える

`isVisible` の切り替えは、行をその場で作り替えるのではなく、完全な model から表示対象の集合を組み直す。

```kotlin
var showAdvanced by remember { mutableStateOf(false) }

KsSettingsView {
    Section(header = "General") {
        LabelCell(title = "Notifications")
        LabelCell(title = "API key", isVisible = showAdvanced)
    }
    Section(header = "Diagnostics", isVisible = showAdvanced) {
        LabelCell(title = "Log level", valueText = "debug")
    }
}
```

## XML から画面を組み込む

`jp.kamusoft.kssettingsview.ui.KsSettingsView` は `FrameLayout` なので、他の View と同じようにレイアウトへ置ける。

```xml
<jp.kamusoft.kssettingsview.ui.KsSettingsView
    android:id="@+id/settings_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

`bind` で Store へつなぐ。設定ツリーの一部ではないものは View のプロパティになる — `style` が Classic / Modern の選択、`rootHeader` と `rootFooter` が画面全体の accessory、`theme` が現在の `Theme`。

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.KsSettingsView
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.SettingsRootStore

class SettingsActivity : AppCompatActivity() {

    private val store = SettingsRootStore(
        initialRoot = SettingsRoot(
            sections = listOf(
                Section(
                    id = "general",
                    header = SectionAccessory.Text("General"),
                    cells = listOf(
                        LabelCell(id = "version", title = "Version", valueText = "1.0.0"),
                    ),
                ),
            ),
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<KsSettingsView>(R.id.settings_view).apply {
            style = KsSettingsViewStyle.Classic
            rootHeader = RootAccessory.Text("Profile")
            bind(store)
        }
    }
}
```

`bind` は Store の現在 root と Theme を直ちに反映し、以後の変更はすべて Store を経由する。View は detach と再 attach をまたいでも (ページャのページが画面外へ出る場合など) Store の現在値を取り込み直して追従する。ただしスクロール位置は復元しない。`bind` の後に `view.theme` を直接代入しても、次の Store 通知が上書きするまでしか効かない。Store を bind した構成での Theme 変更は `applyTheme` の担当で、`view.theme` は Store を使わずに View を駆動する場合のもの。

`unbind()` は Store を手放す。以後の Store 変更は View へ届かなくなり、表示中の内容はそのまま残り、View を再 attach しても購読は復活しない — 再び追従させるには `bind` を呼び直す。冪等なので、Store を持たない View で呼んでも何も起きない。

## Store を使わずに Diff で直接駆動する

外部バインディングや Preview のように Store を持ち込みたくない場面では、View の `applyDiff` へ `SettingsRootDiff` を直接渡して駆動できる。`SettingsRootDiff` は「設定ツリーのどこへ、どの種類の変更を適用するか」を表す sealed interface で、case は Store の公開操作と 1 対 1 に対応する。

| case | 変更 |
|---|---|
| `Full` | ツリー全体を差し替える |
| `InsertSection` | index に Section を追加する |
| `RemoveSection` | ID で Section を削除する |
| `MoveSection` | Section の順序を変える |
| `ReplaceSection` | ID で Section 全体を置換する |
| `InsertCell` | Section の index に Cell を追加する |
| `RemoveCell` | ID で Cell を削除する |
| `ReplaceCell` | 同一 ID の Cell 内容を差し替える |
| `MoveCell` | 同一 Section 内で Cell の順序を変える |
| `UpdateAccessory` | Header / Footer を追加・更新・削除する |

最初の描画は `view.applyDiff(SettingsRootDiff.Full(root))` で入れ、Theme はこの構成でだけ `view.theme` を直接使う。View 側にも `invalidateAccessoryMeasurement(target)` があり、Store の同名操作と同じ再計測をこの構成で要求できる。この直接駆動と `bind(store)` を同じ View で併用してはいけない — 通常のアプリ画面では Store を使う。

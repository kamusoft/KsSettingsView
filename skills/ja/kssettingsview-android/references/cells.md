# Cell

設定画面に Cell を置くためのレシピ。このページの例はすべて以下の import を前提とする。Cell 関数は `jp.kamusoft.kssettingsview.compose` にあり 1 つずつ import する。Cell 関数が受け取る値型 (`KsImage`・`DatePickerUIStyle`) は `compose` ではなく `jp.kamusoft.kssettingsview.ui` にある。

```kotlin
import android.text.InputType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime
import jp.kamusoft.kssettingsview.compose.ButtonCell
import jp.kamusoft.kssettingsview.compose.CheckboxCell
import jp.kamusoft.kssettingsview.compose.CommandCell
import jp.kamusoft.kssettingsview.compose.DatePickerCell
import jp.kamusoft.kssettingsview.compose.EntryCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.NumberPickerCell
import jp.kamusoft.kssettingsview.compose.PickerCell
import jp.kamusoft.kssettingsview.compose.RadioCell
import jp.kamusoft.kssettingsview.compose.SimpleCheckCell
import jp.kamusoft.kssettingsview.compose.SwitchCell
import jp.kamusoft.kssettingsview.compose.TimePickerCell
import jp.kamusoft.kssettingsview.ui.DatePickerUIStyle
import jp.kamusoft.kssettingsview.ui.KsImage
```

`var x by remember { mutableStateOf(...) }` の委譲形をコンパイルさせているのが `getValue` と `setValue` で、これがないと委譲が解決できない。Cell を置くスニペットは、自身で枠を示していない限り `@Composable` 関数の中の `KsSettingsView { Section { ... } }` の内側の断片。

## Cell を Section にまとめる

Cell は必ず Section の中に置く。`Section` は文字列の header と footer を任意で受ける。

```kotlin
KsSettingsView {
    Section(header = "Account", footer = "Signing out keeps local data.") {
        LabelCell(title = "Signed in as", valueText = "taro")
    }
    Section {
        LabelCell(title = "App information")
    }
}
```

## 読み取り専用の値を表示する

`LabelCell` はテキストを表示するだけで、タップには反応しない。

```kotlin
LabelCell(title = "Storage", valueText = "256 GB")
```

## Cell から処理や画面遷移を起こす

`CommandCell` はタップを通知し、`hideArrow = true` を渡さない限り Disclosure Indicator (その Cell が先に進むことを示す、Cell の trailing 端の山形の矢印) を表示する。

```kotlin
CommandCell(
    title = "License",
    onTap = { showLicense() },
)
```

## Cell にボタンを置く

`ButtonCell` は Disclosure Indicator を表示せず、title は既定で中央寄せになる。`titleAlignment` は `jp.kamusoft.kssettingsview.core` の `CellTitleAlignment` (`START` / `CENTER` / `END`) を取り、視覚に出るのは `valueText` を持たない Cell だけ。

```kotlin
ButtonCell(
    title = "Sign out",
    titleColor = Color.Red,
    onTap = { signOut() },
)
```

## 二値をトグルする

`SwitchCell` には TwoWay の overload がある。`MutableState<Boolean>` を渡すと現在値を読み、反転値を書き戻す。

```kotlin
val notifications = remember { mutableStateOf(false) }

SwitchCell(title = "Push notifications", isOn = notifications)
```

もう一方の overload は `Boolean` と `onValueChanged` を取る。状態を素の値として自分で持つ場合はこちらを使う。

```kotlin
var pushEnabled by remember { mutableStateOf(false) }

SwitchCell(
    title = "Push notifications",
    isOn = pushEnabled,
    onValueChanged = { pushEnabled = it },
)
```

## 独立した選択肢にチェックを付ける

`CheckboxCell` は checkbox で表す独立した二値。現在値を受け取り反転値を通知するので、状態は利用者が持つ。

```kotlin
var acceptedTerms by remember { mutableStateOf(false) }

CheckboxCell(
    title = "I accept the terms",
    isChecked = acceptedTerms,
    onValueChanged = { acceptedTerms = it },
)
```

## 簡易なチェックマークで表す

`SimpleCheckCell` も独立した二値で、checkbox ではなく単純なチェックマークで描く。

```kotlin
var weeklyReport by remember { mutableStateOf(false) }

SimpleCheckCell(
    title = "Weekly report",
    isChecked = weeklyReport,
    onValueChanged = { weeklyReport = it },
)
```

## 複数の Cell から 1 つを選ぶ

同じ `groupId` を持つ `RadioCell` が 1 つの選択グループになる。`value == selectedValue` の Cell が選択表示になり、`selectedValue` は利用者が持つ。

```kotlin
var appearance by remember { mutableStateOf("light") }

Section(header = "Appearance") {
    RadioCell(
        title = "Light",
        groupId = "appearance",
        value = "light",
        selectedValue = appearance,
        onSelected = { appearance = it },
    )
    RadioCell(
        title = "Dark",
        groupId = "appearance",
        value = "dark",
        selectedValue = appearance,
        onSelected = { appearance = it },
    )
}
```

選択済みの Cell を再タップしても `onSelected` は再通知されない。

## テキストを入力させる

`EntryCell` には書き戻しを行う `MutableState<String>` の overload がある。`keyboardType` は `android.text.InputType` の定数を取る。

```kotlin
val nickname = remember { mutableStateOf("") }

EntryCell(
    title = "Nickname",
    text = nickname,
    placeholder = "Up to 20 characters",
    maxLength = 20,
)
```

パスワード欄なら `isPassword = true`、数値欄なら `keyboardType = InputType.TYPE_CLASS_NUMBER` を渡す。`textAlignment` は入力テキストの揃えを `CellTitleAlignment` (`START` / `CENTER` / `END`) で決め、既定は `END`。`placeholderColor` は placeholder の文字色を決める。未指定なら `CellStyle.placeholderColor` → `Theme.cellPlaceholderColor` → OS 既定の順で解決し、OS 既定はそれ自体がダークモードに追従する。

フォーカス中の入力欄は自身のテキストの所有者になる。同じ Cell への内容更新は入力中の文字列を差し替えず、フォーカスを失った時点で最後に供給された値へ再同期する。callback の overload (`text` に `String`、変更通知に `onTextChanged`) を使う場合は受け取った値を Cell へ戻すこと。戻さないとフォーカスが外れた瞬間に表示が巻き戻る。単一行では Enter を「完了」として扱いキーボードを閉じる (フォーカスは維持)。Enter で改行したい場合は `keyboardType` に `InputType.TYPE_TEXT_FLAG_MULTI_LINE` を含める。

## リストから 1 つ選ぶ

`PickerCell` は Cell のタップでボトムシートを開く。単一選択の overload は `MutableState<Int?>` を取る。確定ボタンは無く、候補をタップした時点で書き戻してシートを閉じる。

```kotlin
val themeIndex = remember { mutableStateOf<Int?>(0) }

PickerCell(
    title = "Theme",
    items = listOf("Light", "Dark", "System"),
    selectedIndex = themeIndex,
)
```

シートのタイトルは `pageTitle` で指定でき、未指定なら Cell の `title` が使われる。

## リストから object を 1 つ選ぶ

候補は文字列でなくてもよい。ジェネリックな overload が任意の要素リストと射影 `displayText` を取り、`subText` を渡すとシートの各候補の下に 2 行目が付く (subText の無い候補は 1 行のまま)。`onItemSelected` には選ばれた要素そのものが届く。要素リストは Cell 構築時にコピーされるため、元コレクションへの後からの変更は in-place の書き換えではなく新しいリストの供給で反映する。

```kotlin
data class Plan(val name: String, val detail: String)

val plans = listOf(
    Plan(name = "Free", detail = "Up to 1 device"),
    Plan(name = "Pro", detail = "Unlimited devices"),
)
val planIndex = remember { mutableStateOf<Int?>(0) }

PickerCell(
    title = "Plan",
    items = plans,
    displayText = { it.name },
    subText = { it.detail },
    selectedIndex = planIndex,
    onItemSelected = { plan -> applyPlan(plan) },
)
```

Cell 自身に表示されるのは選択中の `displayText` だけで、`subText` は出ない。単一選択では index の state の代わりに要素を直接束ねることもできる。`selectedItem` は `MutableState<T?>` を取り、構築時に候補列から等価な最初の候補へ解決し (リストに無い要素は未選択)、選ばれた要素を書き戻す。

```kotlin
val plan = remember { mutableStateOf<Plan?>(null) }

PickerCell(
    title = "Plan",
    items = plans,
    displayText = { it.name },
    selectedItem = plan,
)
```

## 上限付きで複数選ぶ

複数選択の overload は `MutableState<Set<Int>>` と `maxSelectedNumber` を取る。`0` は上限なし。単一選択のシートと違いこちらは選択中の作業状態を持ち OK ボタンを表示する。書き戻しは確定時の 1 回だけ。

```kotlin
val topics = remember { mutableStateOf(setOf(0)) }

PickerCell(
    title = "Topics",
    items = listOf("News", "Sports", "Music", "Travel"),
    selectedIndices = topics,
    maxSelectedNumber = 2,
)
```

確定せずに閉じた場合 (キャンセル・Back・外側タップ・ハンドルからの下スワイプ) は、選択中の変更を破棄する。

複数選択にも object の overload がある。任意の要素リストと `displayText` (必要なら `subText` も) を渡すと、`onItemsSelected` に確定された要素が index 昇順で届く。書き戻される state は `Set<Int>` のまま — 複数選択には要素型の state は無い。

Store 用に `jp.kamusoft.kssettingsview.ui` の `PickerCell` クラスでツリーを組む場合 ([updates.md](updates.md) を参照)、候補は `PickerItem` (主表示 `text` + 任意の副表示 `subText`) のリストで持ち、単一 / 複数は `selectionMode` (`PickerSelectionMode.Single` / `Multiple`) で切り替える。callback は単一が `onSelectionChanged`、複数が `onMultiSelectionChanged`。DSL の overload はどれも引数の組み合わせからこれらを設定するので、宣言側で直接指定することはない。

## 単位付きの数値を選ぶ

`NumberPickerCell` は `min` から `max` まで `step` 刻みで候補を作り、各候補に `unit` を付けて表示する。

```kotlin
val fontSize = remember { mutableStateOf(14) }

NumberPickerCell(
    title = "Font size",
    value = fontSize,
    min = 10,
    max = 30,
    step = 1,
    unit = "pt",
)
```

シートのタイトルは `pickerTitle` で指定でき、未指定なら Cell の `title` が使われる。`TimePickerCell` と `DatePickerCell` も同名の引数を持つ。

## 時刻を選ぶ

`TimePickerCell` は `java.time.LocalTime` を編集する。Cell のタップで時・分ホイールのボトムシートが開き、書き戻しは確定操作の 1 回だけ — 他の閉じ方はどれも変更を破棄する。`format` が決めるのは Cell に出る文字列だけ。

```kotlin
val alarm = remember { mutableStateOf(LocalTime.of(7, 0)) }

TimePickerCell(
    title = "Alarm",
    time = alarm,
    format = "HH:mm",
)
```

シートの時が 0–23 になるか、1–12 + 午前/午後ホイールになるかは `is24Hour` だけで決まる。既定の `true` は 24 時間制。`format` も端末の 24 時間設定も関与しないため、同じ Cell はどの端末でも同じシートを開く — `format` を `is24Hour` と食い違わせないのは利用者の責任になる。12 時間制のシートでは午前/午後のラベルとホイールの並びが端末の Locale に従う。

```kotlin
TimePickerCell(
    title = "Bedtime",
    time = alarm,
    format = "h:mm a",
    is24Hour = false,
)
```

## 日付を選ぶ

`DatePickerCell` は `java.time.LocalDate` を編集する。`uiStyle` が選択面を決める — `DatePickerUIStyle.Material` はテキスト入力モードも備えたカレンダーダイアログ、`DatePickerUIStyle.Spinner` は 3 連ホイールのボトムシート。空でない `todayText` を渡すと、どちらの選択面にも「今日」へ移動する操作が付く。移動は選択を動かすだけで確定はせず、今日が選択可能範囲の外なら何もしない。

```kotlin
val birthday = remember { mutableStateOf(LocalDate.of(1990, 1, 1)) }

DatePickerCell(
    title = "Birthday",
    date = birthday,
    format = "yyyy/MM/dd",
    uiStyle = DatePickerUIStyle.Material,
    todayText = "Today",
)
```

ここでも書き戻しは確定操作の 1 回だけで、他の閉じ方はどれも変更を破棄する。カレンダーダイアログは、Activity 再生成の前後で Cell の ID が安定していれば選択状態を保ったまま回転を生き延びる ([updates.md](updates.md) を参照)。Spinner のシートは他のボトムシートと同じく、回転では何も通知せず閉じる。`androidButtonColor` は `Spinner` のシートのヘッダー操作 (確定・キャンセル) の色だけを上書きし、`Material` のダイアログには効かない。

`minDate` と `maxDate` で選べる範囲を制限できる。どちらか一方だけでもよい。現在値が範囲外なら、最も近い範囲端へ丸めて提示される。

```kotlin
DatePickerCell(
    title = "Appointment",
    date = birthday,
    minDate = LocalDate.of(2026, 1, 1),
    maxDate = LocalDate.of(2026, 12, 31),
)
```

## Cell にアイコンを付ける

`icon` は `KsImage` を取る。drawable のリソース ID か `Drawable` インスタンスを渡す。

```kotlin
LabelCell(title = "Storage", icon = KsImage.Resource(R.drawable.ic_storage))
LabelCell(title = "Avatar", icon = KsImage.Drawable(avatarDrawable))
```

アイコンは正方形の枠に収めて描くため、字形の幅が違っても title の開始位置は揃う。`KsImage.SystemName` は iOS との API 対称性のために存在し、Android ではアイコンなしへ fallback する。

## 同じ Cell に説明・値・ヒントを付ける

組み込み Cell はすべて `description` (title の下)、`valueText` (title 行の trailing)、`hintText` (右上) を受ける。例外は 2 つ。`ButtonCell` は `description` を持たず、`EntryCell` は入力欄自身が値を表示するため `valueText` を持たない (代わりに `text` を使う)。

```kotlin
LabelCell(
    title = "Storage",
    description = "Internal storage of this device",
    valueText = "256 GB",
    hintText = "Updated today",
    icon = KsImage.Resource(R.drawable.ic_storage),
)
```

Cell の幅が足りないときは title を守り、`valueText` 側を省略表示する。

## Cell を無効化する

構築時に `isEnabled = false` を渡す。タップと内包コントロールの操作を止め、文字色を無効時の色へ差し替える。handle には `disabled(...)` modifier もあるが、これは意図的に no-op で Cell をそのまま返すため、構築時の引数の代わりにはならない。

```kotlin
CommandCell(title = "Advanced settings", isEnabled = false)
```

## 値を保ったまま Cell を隠す

`isVisible = false` は表示から Cell を外すだけで値は model に残る。隠れている間に適用した更新は、再表示時に反映済みの値として現れる。

```kotlin
var showAdvanced by remember { mutableStateOf(false) }

Section(header = "General") {
    LabelCell(title = "Notifications")
    LabelCell(title = "API key", isVisible = showAdvanced)
}
```

`isVisible` は `Section` にもあり、その Section の Header・Footer と全 Cell をまとめて隠す。

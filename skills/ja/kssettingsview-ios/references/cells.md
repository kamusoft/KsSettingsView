# Cell

設定画面に Cell を置くためのレシピ。例はいずれも [SKILL.md](../SKILL.md) の最小動作コードと同じ import を前提とする。例には UIKit の型がそのまま現れる (`titleColor` の `UIColor`、`keyboardType` の `UIKeyboardType`)。`import SwiftUI` から UIKit が入らないファイルでは `import UIKit` が要る。

## Cell を Section にまとめる

Cell は必ず Section の中に置く。`ksSection` は文字列の Header と Footer を任意で受ける。

```swift
KsSettingsView {
    ksSection("Account", footer: "Signing out keeps local data.") {
        LabelCell(title: "Signed in as", valueText: "taro")
    }
    ksSection {
        LabelCell(title: "App information")
    }
}
```

## 読み取り専用の値を表示する

`LabelCell` はテキストを表示するだけで、タップに反応しない。

```swift
LabelCell(title: "Storage", valueText: "256 GB")
```

## Cell から処理や画面遷移を起こす

`CommandCell` はタップを通知し、`hideArrow: true` を渡さない限り Disclosure Indicator を表示する。

```swift
CommandCell(
    title: "License",
    onTap: { showLicense = true }
)
```

## Cell をボタンにする

`ButtonCell` は Disclosure Indicator を表示せず、title を既定で中央寄せにする。`titleAlignment` は `CellTitleAlignment` (`.start` / `.center` / `.end`) を受け、視覚に出るのは `valueText` を持たない Cell だけ。

```swift
ButtonCell(
    title: "Sign out",
    titleColor: .systemRed,
    onTap: { signOut() },
    titleAlignment: .start
)
```

## 二値を切り替える

`SwitchCell` には現在値を渡し、反転値が callback で返る。状態を所有するのは利用者側で、次の評価で新しい値を渡し直す。

```swift
@State private var notifications = false

SwitchCell(
    title: "Push notifications",
    isOn: notifications,
    onValueChanged: { notifications = $0 }
)
```

## 独立したチェック項目を置く

`CheckboxCell` は checkbox で表す独立した二値。

```swift
@State private var acceptedTerms = false

CheckboxCell(
    title: "I accept the terms",
    isChecked: acceptedTerms,
    onValueChanged: { acceptedTerms = $0 }
)
```

## 簡易なチェックマークで表す

`SimpleCheckCell` も独立した二値で、checkbox ではなく素のチェックマークで描く。

```swift
@State private var weeklyReport = false

SimpleCheckCell(
    title: "Weekly report",
    isChecked: weeklyReport,
    onValueChanged: { weeklyReport = $0 }
)
```

## 複数の Cell から 1 つを選ばせる

同じ `groupId` を持つ `RadioCell` が 1 つの選択グループになる。`value == selectedValue` の Cell が選択表示になり、`selectedValue` は利用者が所有する。

```swift
@State private var appearance = "light"

ksSection("Appearance") {
    RadioCell(
        title: "Light",
        groupId: "appearance",
        value: "light",
        selectedValue: appearance,
        onSelected: { appearance = $0 }
    )
    RadioCell(
        title: "Dark",
        groupId: "appearance",
        value: "dark",
        selectedValue: appearance,
        onSelected: { appearance = $0 }
    )
}
```

## 文字を入力させる

`EntryCell` は `Binding<String>` を受ける初期化子を持ち、書き戻しまで行う。`keyboardType` は `UIKeyboardType` をそのまま受ける。

```swift
@State private var nickname = ""

EntryCell(
    title: "Nickname",
    text: $nickname,
    placeholder: "Up to 20 characters",
    maxLength: 20
)
```

パスワード入力なら `isPassword: true`、数値入力なら `keyboardType: .numberPad` を渡す。`placeholderColor` は placeholder の文字色を上書きする — 未指定なら OS 既定の placeholder 色のままで、ダークモードにも自動追従する。画面全体の既定は `Theme.cellPlaceholderColor` — [styling.md](styling.md) を参照。`textAlignment` (`CellTitleAlignment`、既定 `.end`) は入力欄内の文字寄せを決める。Binding の代わりに、現在の `text` を値として渡して `onTextChanged` callback で受ける形もある — 状態を自分で管理する画面や Store でツリーを操作する構成で使う。どちらの形でも入力欄は利用者が打った文字をそのまま保つ。callback を受けても `text` を更新しない構成で、後続の再描画が入力中の値を巻き戻すことはない (Cell は描画時点の最新の値から描かれる)。

## リストから 1 つ選ばせる

`PickerCell` は Cell のタップで選択画面を開く。単一選択は `selectedIndex` を使い、候補をタップした時点で callback が 1 回発火して画面が閉じる (別途の確定操作は無い)。キャンセルで閉じた場合は、単一選択・複数選択のどちらも発火しない。

```swift
@State private var themeIndex: Int? = 0

PickerCell(
    title: "Theme",
    items: ["Light", "Dark", "System"],
    selectedIndex: $themeIndex
)
```

`items` は `PickerItem` — 主表示の `text` + 任意の 2 行目 (副表示) `subText` — の列で、上のような文字列配列はその簡易形。`pageTitle` は選択画面のタイトルを上書きする (未指定なら `title` を使う)。Binding の代わりに、`selectedIndex` を値として渡して `onSelectionChanged` callback で受ける形もある。どちらの選択形かは `selectionMode` (`PickerSelectionMode` の `.single` / `.multiple`) として公開され、使った初期化子で決まる。

## 上限つきで複数選ばせる

複数選択は `selectedIndices` と `maxSelectedNumber` を使う。`0` は上限なしで、callback は画面を閉じる完了ボタンを押したときに 1 回発火する。この形の callback 版は `onMultiSelectionChanged` で、確定した `Set<Int>` を受け取る。

```swift
@State private var topics: Set<Int> = [0]

PickerCell(
    title: "Topics",
    items: ["News", "Sports", "Music", "Travel"],
    selectedIndices: $topics,
    maxSelectedNumber: 2
)
```

## 自前の object を候補にする

`items` は自前の要素型 (`Sendable`) の配列も、`displayText` 射影と組で受ける。`subText` 射影を渡すと選択画面の各行に 2 行目 (副表示) が付く。配列は Cell 構築時に snapshot され、選択の正は index のまま。`onItemSelected` は index の書き戻しの後に元の要素を届ける。

```swift
struct Plan: Sendable { let name: String; let detail: String }

@State private var planIndex: Int? = 0

PickerCell(
    title: "Plan",
    items: plans,
    displayText: { $0.name },
    subText: { $0.detail },
    selectedIndex: $planIndex,
    onItemSelected: { plan in savePlan(plan) }
)
```

複数選択も同じ形で `selectedIndices` を使い、`onItemsSelected` が確定した要素を index 昇順で届ける。

## 選択中の object を直接 bind する

要素型が `Equatable` でもあるなら、`selectedItem` で選択を要素そのものとして bind できる。初期 index は値等価で逆引きされ、最初に一致した index に解決する。`items` に無い要素は未選択になる。複数選択に object binding は無い — `selectedIndices` を bind し、要素は `onItemsSelected` で受け取る。

```swift
struct Plan: Sendable, Equatable { let name: String; let detail: String }

@State private var plan: Plan?

PickerCell(
    title: "Plan",
    items: plans,
    displayText: { $0.name },
    selectedItem: $plan
)
```

## 単位つきの数値を選ばせる

`NumberPickerCell` は `min` から `max` まで `step` 刻みで候補を作り、各候補に `unit` を付けて表示する。

```swift
@State private var fontSize = 14

NumberPickerCell(
    title: "Font size",
    min: 10,
    max: 30,
    step: 1,
    value: $fontSize,
    unit: "pt"
)
```

`pickerTitle` は数値選択画面のタイトルを上書きする (未指定なら `title` を使う)。

## 時刻を選ばせる

`TimePickerCell` は `Foundation.Date` の時刻部分を編集し、`format` が Cell に出る文字列を決める。

```swift
@State private var alarm = Date()

TimePickerCell(
    title: "Alarm",
    time: $alarm,
    format: "HH:mm"
)
```

選択 UI の時制 (12/24 時間制) は `is24Hour` だけで決まる (既定 `true` = 24 時間制)。`format` は Cell に出る文字列にしか効かず、端末の 24 時間設定も参照されない。12 時間制にするなら `is24Hour: false` と、それに合う `"h:mm a"` のような `format` を組で渡す (両者の食い違いをライブラリは検証しない)。`pickerTitle` は時刻選択画面のタイトルを上書きする (未指定なら `title` を使う)。

## 日付を選ばせる

`DatePickerCell` は `Date` の日付部分を編集する。`uiStyle` は `DatePickerUIStyle` を受けて器 (`.wheels` / `.calendar`) を選び、空でない `todayText` を渡すと「今日」へジャンプする操作が現れる。

```swift
@State private var birthday = Date()

DatePickerCell(
    title: "Birthday",
    date: $birthday,
    format: "yyyy/MM/dd",
    uiStyle: .calendar,
    todayText: "Today"
)
```

`minDate` / `maxDate` は選択できる日付の範囲を定める。`pickerTitle` は日付選択画面のタイトルを上書きする (未指定なら `title` を使う)。

## Cell にアイコンを付ける

`icon` は `KsImage` を受ける。SF Symbols 名 (`.systemName(_:)`) か `UIImage` (`.uiImage(_:)`) を渡す。

```swift
LabelCell(title: "Storage", icon: .systemName("externaldrive"))
LabelCell(title: "Avatar", icon: .uiImage(avatarImage))
```

アイコンは正方形の枠に収めて描かれるため、字形の幅が違っても title の開始位置は揃う。

## 同じ Cell に説明・値・ヒントを載せる

組み込み Cell は `description` (title の下)、`valueText` (title と同じ行の末尾)、`hintText` (Cell の右上) を受ける。例外は 2 つで、`ButtonCell` は `description` を持たず、`EntryCell` は入力欄自身が値を表示するため `valueText` を持たない (`text` を使う)。

実引数は宣言順に並べる必要がある。共通フィールドの順序は `id`、`style`、`title`、`description`、`valueText`、`icon`、`hintText` で、その後に Cell 固有のフィールド、最後に `isEnabled` と `isVisible` が来る。

```swift
LabelCell(
    title: "Storage",
    description: "Internal storage of this device",
    valueText: "256 GB",
    icon: .systemName("externaldrive"),
    hintText: "Updated today"
)
```

Cell の幅が足りないときは title を守り、`valueText` 側が末尾省略で切り詰められる。

## Cell を無効にする

構築時に `isEnabled: false` を渡す。タップと内包コントロールの操作が無効になり、文字色が無効時の色に置き換わる。Cell 自身にも `.disabled(_:)` modifier があるが (SwiftUI の `View.disabled(_:)` とは別物)、これは no-op なので Cell は無効にならない。

```swift
CommandCell(title: "Advanced settings", isEnabled: false)
```

## 値を保ったまま Cell を隠す

`isVisible: false` は Cell を表示から外すが値は model に残るため、隠れている間に適用した更新は再表示時に現れる。

```swift
@State private var showAdvanced = false

ksSection("General") {
    LabelCell(title: "Notifications")
    LabelCell(title: "API key", isVisible: showAdvanced)
}
```

`isVisible` は `ksSection` にもあり、その Section の Header・Footer・全 Cell をまとめて隠す。

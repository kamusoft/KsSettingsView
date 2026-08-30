# レビュー結果: add-maui-basic-input-cells (001 回目)

**日付**: 2026-08-10
**判定**: CHANGES_REQUESTED

## サマリー

L 級の規模 (147 ファイル / 実装+テスト約 10,000 行) に対して、設計の忠実度・テストの網羅性ともに水準が高い。デルタスペック 3 能力の Requirement はほぼ全て実装とテストで裏付けられており、全テスト (MAUI 227 / iOS 710 / Android 2152) とビルド (facade 3 TFM 警告 0 / Android Binding 0 エラー) は緑を実測確認した。書き戻し経路・icon の世代管理・cellId 温存・delegate 寿命はいずれも境界条件まで押さえられている。

一方で、**Android で `IsPassword` と `Keyboard` を併用するとパスワードが平文表示になる**経路が本変更で MAUI から新たに到達可能になっており (Major-1)、加えて sample-parity 規約が明示的に禁じている「本体 API の platform 差による不一致の黙認」が 1 件記録されていない (Major-2)。また review-handoff #1 / #6 の採否判断は前提事実が実物と食い違っており、再評価が必要 (Minor-4 / Minor-5)。

---

## 指摘事項

### [🟠 Major] Android で `IsPassword` + 非既定 `Keyboard` の組み合わせが平文表示になる (両OS不一致)

**該当箇所**
- `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeValueTransport.kt:124-130`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:113-121` (本変更では未修正)

**問題点**

Bridge は keyboard 序数を variation ビットを含む `InputType` へ写す。

```kotlin
4 -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI            // 0x0011
5 -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS  // 0x0021
6 -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL         // 0x2002
7 -> InputType.TYPE_CLASS_PHONE                                                // 0x0003
```

対して ViewHolder は無条件に OR 合成する。

```kotlin
baseInputType or InputType.TYPE_TEXT_VARIATION_PASSWORD  // 0x0080
```

`TYPE_TEXT_VARIATION_*` はフラグではなく `TYPE_MASK_VARIATION` (0x0ff0) 内の**値**であり、Email(0x20) と PASSWORD(0x80) の OR は variation 0xA0 という未定義値になる。Android のマスク判定は
`(inputType and (TYPE_MASK_CLASS or TYPE_MASK_VARIATION)) == (TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD)` の**等値比較**のため、Url / Email / Numeric / Telephone のいずれと組み合わせても判定が外れて**入力が平文で表示される**。Numeric は本来 `TYPE_NUMBER_VARIATION_PASSWORD` (0x10)、Phone にはパスワード variation が存在しない。

iOS は `isSecureTextEntry` が `keyboardType` と独立なので正しくマスクされる — 同じ facade 設定で両OSの表示が食い違う。facade は `EntryCell.Keyboard` と `EntryCell.IsPassword` を両方公開しており (`maui/KsSettingsView.Maui/EntryCell.cs:33-44`)、Android gateway が keyboard を輸送する (`Platforms/Android/KsBridgeGateway.cs`) ため到達可能。ViewHolder のコメント自身が「ベースが `TYPE_CLASS_TEXT` のときに有効」と限定しており、Bridge が非 TEXT variation を流し込むようになった本変更で顕在化した。

サンプルのパスワード欄は `Keyboard` 未指定 (= Default → `TYPE_CLASS_TEXT`) のため、実機目視 (tasks 7.1) では露出しない。

**推奨修正**

`EntryCellViewHolder` で class に応じた password variation を選び、OR ではなく variation フィールドを上書きする。

```kotlin
val cls = baseInputType and InputType.TYPE_MASK_CLASS
val passwordVariation = when (cls) {
    InputType.TYPE_CLASS_NUMBER -> InputType.TYPE_NUMBER_VARIATION_PASSWORD
    else -> InputType.TYPE_TEXT_VARIATION_PASSWORD
}
(baseInputType and InputType.TYPE_MASK_VARIATION.inv()) or passwordVariation
```

`TYPE_CLASS_PHONE` はマスク不可のため TEXT クラスへ倒すか、契約として明示する。あわせて `keyboard × isPassword` の組み合わせを Bridge 変換テストへ追加する。

修正箇所が `ks-settingsview-ui` (proposal の Non-Goal「Native の Cell 実装への機能追加」の領域) にあるため、**本変更で直すか別 change へ切り出すかはオーナー判断が要る**。切り出す場合も deviation.md への記録と後続 change での追跡を残すこと。

---

### [🟠 Major] sample-parity: 「ニックネーム (callback)」デモが MAUI で成立しないのに deviation 未記録

**該当箇所**
- `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:22` (footer) / `:40-42` (該当 Cell)
- `kasane/changes/add-maui-basic-input-cells/deviation.md` (記録なし)

**問題点**

footer 文言は 3 platform で一字一句一致しているが、その文言が主張する「2 経路の対比」が MAUI では成立していない。

- iOS `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:120-129` — 5 番目だけ `text: nickname` (`String`) + `onTextChanged:` callback、他 4 つは `text: tracked($userName, ...)` (`Binding<String>`)
- Android `InputCellsDemoScreen.kt:125-134` — 同構造 (`text = nickname.value` + `onTextChanged`)
- MAUI `InputCellsDemoPage.xaml:40-42` — `ValueText="{Binding Nickname}"` で、他 4 つとまったく同じ TwoWay binding

MAUI facade の `EntryCell` は値変更 callback / event を公開しないため (`EntryCell.cs` にイベント定義なし)、対比が表現できない。表示文言は「callback 経路で更新」と謳っているので、MAUI サンプルの読み手は誤った理解をする。

sample-parity 規約の「してはいけないこと」に次の条項がある。

> 本体公開 API の platform 差で一致が不可能な箇所を黙認しない。一致できない理由を同じく deviation.md に記録し、本体側の統一課題として扱う

本件はこの条項にそのまま当たり、記録がないため parity 検証装置としての比較点が黙って失われている。

**推奨修正**

deviation.md に 1 行追記する (文言は parity のため据え置き)。例: 「MAUI facade の `EntryCell` は値変更 callback / event を公開しないため、`ニックネーム (callback)` セルも TwoWay binding 経路になる。footer 文言は 3 platform の文言一致を優先して据え置き。`EntryCell` への値変更通知の公開は本体側の統一課題として後続で扱う」。あわせて公開面の統一を後続 change / agenda の TODO として残す。

**補足**: これは `kasane/lessons/inbox/parity-target-sample-api-not-cross-checked.md` が捉えた失敗パターン (parity 対象の native サンプルが使う API を、同 change の公開面 spec が供給できるか突き合わせていない) の **3 件目**にあたる。同 lesson の evidence は `Section(headerHeight:)` と基本 Cell の `valueText` の 2 件を挙げているが、`onTextChanged` は未捕捉。lesson の evidence へ追記しておくと、昇格判断の材料が揃う。

---

### [🟡 Minor] `PickerCell.SelectedItem` に無効な値が残り、spec の SHALL と食い違う

**該当箇所** `maui/KsSettingsView.Maui/PickerCell.cs:238-254`

**問題点**

`SyncIndexFromSelectedItem` は `SelectedItem` から `SelectedIndex` を導出するだけで、解決できなかったときに `SelectedItem` 自身を戻さない。一時テストを追加して実測した結果は次のとおり。

| 操作 | `SelectedItem` | `SelectedIndex` |
|---|---|---|
| `ItemsSource` 未設定 + `SelectedItem = "X"` | `"X"` | `null` |
| `ItemsSource = [ライト, ダーク]` + `SelectedItem = "セピア"` | `"セピア"` | `null` |
| `ItemsSource` を後から `null` 化 | `null` (正) | `1` (正) |

maui-cells spec の Requirement「PickerCell の SelectedItem 相互導出」は
「`ItemsSource` 未設定または `SelectedIndex` が範囲外のとき `SelectedItem` は null でなければならない (SHALL)」
と定めており、1 行目は文言どおりの違反。2 行目は「未選択なのに `SelectedItem` は選択済みに見える」内部矛盾で、TwoWay バインドした ViewModel に幻の選択値が残り続ける。

既存テスト `PickerSelectedItemTests.SettingUnknownSelectedItemClearsIndex` (`:70-77`) は `SelectedIndex` が null になることだけを見ており、`SelectedItem` 側を assert していないため検出できていない。spec の Scenario 2 件は通る (どちらも `SelectedItem` を直接設定しない経路) ため、Scenario だけでは足りない箇所。

**推奨修正**

`SyncIndexFromSelectedItem` で解決結果を `SelectedIndex` へ書いた後、同じガードの中で `SelectedItem` を `ResolveSelectedItem()` の値へ揃える (解決できなければ null)。あわせて上記 2 ケースの `SelectedItem` を assert するテストを追加する。

---

### [🟡 Minor] BG8605 容認判断の前提が実物と食い違う (review-handoff #1)

**該当箇所**
- `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeValueTransport.kt:26,140,154,169`
- `kasane/changes/add-maui-basic-input-cells/artifacts/review-handoff.md:5`

**問題点**

Binding を rebuild して `java-resolution-report.log` を実読した結果、BG8605 で束縛から落ちているのは次の 3 メソッドで、いずれも「戻り値型が束縛対象外」が理由:

```
The method 'datePickerUIStyle(java.lang.Integer ordinal)' was removed because the Java return type
  'jp.kamusoft.kssettingsview.ui.DatePickerUIStyle' could not be found.
The method 'selectionMode(int ordinal)'   ... 'jp.kamusoft.kssettingsview.ui.PickerSelectionMode' ...
The method 'titleAlignment(java.lang.Integer, CellTitleAlignment)' ... 'CellTitleAlignment' ...
```

判断の根拠とされた「解消の代償 = 変換ロジック3重複製」は成立しない。Kotlin の `internal` は JVM 上 public クラスとして見えるため class-parse に拾われるが、**同一 change の新規ファイル群がすでに `@JvmSynthetic` で同じ問題を解いている** (`KsBridgeCell.kt:78,86,90,95`、`KsSettingsBridge.kt:45,52,62`、各 per-type DTO の `makeCell`、`KsBridgeCellStyle.kt:57`、`KsBridgeTheme.kt:113` — 計 15 箇所以上)。同じ注釈を 3 メソッドへ付けるだけで、Kotlin 内からの呼び出しに影響なく警告と束縛面の汚れが同時に消える見込み。

なお報告書に載る他の除去はすべて `$ks_settingsview_bridge_release` (Kotlin `internal` の名前マングリング) 由来で、**公開 DTO 面のプロパティ・setter は 1 件も落ちていない**ことは確認した — 「公開 DTO 面は全メンバー生成済み」という判断部分は正しい。

**推奨修正**

`datePickerUIStyle` / `titleAlignment` / `selectionMode` に `@JvmSynthetic` を付けて Binding を rebuild し、BG8605 3 件と BG8606 が消えることを実測する。解消できたら review-handoff の判断根拠も更新する。解消しない事情が別にあるなら、その事実 (実測結果) を根拠として残す。

*注*: このレビューでは一時的な実装ミューテーションによる実証を試みたが権限ゲートで実行できなかったため、上記は静的根拠に基づく指摘である (`KsBridgeValueTransport.kt` は shasum `6f198d31…` で無変更を確認済み)。

---

### [🟡 Minor] `KsBridgePickerCell.WhenMappings` が公開束縛面へ漏れる (本変更由来)

**該当箇所**
- `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgePickerCell.kt:56`
- `maui/android/KsSettingsView.Binding.Android/Transforms/Metadata.xml:34`

**問題点**

enum を subject にした `when` は Kotlin が `KsBridgePickerCell$WhenMappings` 合成クラスを生成する。生成後の `api.xml` を実読すると `KsSettingsBridge.WhenMappings` と `KsBridgePickerCell.WhenMappings` の 2 件が存在し、`Metadata.xml` の `remove-node` は前者しか除去していない。

review-handoff #2 は「facade 前半ワーカーの発見・変更由来ではない」としているが、`KsBridgePickerCell.kt` は本変更の新規ファイルであり、この漏れは本変更由来である。

**推奨修正**

分岐は 2 値なので `when` を `if` に変えれば合成クラス自体が生成されない (最も安価)。あるいは `remove-node` を 1 行追加する。いずれにせよ判断記録の「変更由来ではない」は訂正が必要。

---

### [🟡 Minor] Native 既定値を Bridge 側にリテラル複製しており、乖離が無検出になる (両OS)

**該当箇所**
- iOS: `ios/Sources/KsSettingsViewBridge/KsBridgeDatePickerCell.swift:44,47`、`KsBridgeTimePickerCell.swift:31`、`KsBridgeEntryCell.swift:54`、`KsBridgeButtonCell.swift:36`
- Android: `KsBridgeDatePickerCell.kt:82,85`、`KsBridgeTimePickerCell.kt:60`、`KsBridgeEntryCell.kt:68`、`KsBridgeButtonCell.kt:48`

**問題点**

maui-bridge spec は「未指定時は native 既定の uiStyle を使わなければならない (SHALL)」と定めるが、実装は native 既定を手で写した定数を渡している。native 側 (`ios/Sources/KsSettingsViewUI/DatePickerCell.swift:55,60` の `format: String = "yyyy/MM/dd"` / `uiStyle: DatePickerUIStyle = .wheels` 等) を変えても Bridge は自分の定数を渡し続けるため、**SHALL が破れてもテストは緑のまま**になる。

同一変更内で方針が割れている点も気になる — `KsBridgeSection.swift:79-80` / `KsBridgeSection.kt:64` は同じ問題を導出で解いている。

```swift
// 未指定の headerHeight は Native の `Section` 既定 (自動高さ) をそのまま使う。
let automaticHeaderHeight = KsSettingsViewCore.Section(id: id).headerHeight
```

**推奨修正**

Section と同じ導出方式へ寄せる (例: `format ?? DatePickerCell(title: "", date: ...).format`)。導出が重い場合は、せめて「Bridge の既定定数 == native の既定値」を突き合わせるテストを 1 本置いて乖離を検出可能にする。

---

### [🟡 Minor] トートロジーなアサーションが 2 件ある

**該当箇所**
- `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCellConversionTest.kt:195`
- `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeSectionHeaderHeightTest.kt:19`

**問題点**

1 件目は被検証関数そのもので期待値を作っており、写像が壊れてもこのテストは通る。

```kotlin
assertEquals(KsBridgeValueTransport.keyboardType(5), cell?.keyboardType)
```

2 件目は実装 (`KsBridgeSection.kt:64`) と同一の式で期待値を作っているため、「未指定は native 既定になる」テストが native 既定の変更に対して永久に緑になる。

```kotlin
private val automaticHeaderHeight: Double = Section(id = "auto").headerHeight
```

**推奨修正**

1 件目は `InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS` を直書きする (写像自体は `KsBridgeValueTransportTest.kt:111-129` で別途固定されているので実害は小さいが、この行は現状何も検証していない)。2 件目は少なくとも 1 本を `-1.0` 直書きにして native 契約を固定する。

---

### [🟡 Minor] `KsBridgeCellStyle` 13 項目のうち 5 項目しか変換テストがない

**該当箇所** `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCellConversionTest.kt:337-355`

**問題点**

`titleColor` / `iconSize` / `iconRadius` / `cellHeight` / `titleFont` のみ検証されており、`descriptionColor` / `descriptionFont` / `valueTextColor` / `valueTextFont` / `hintTextColor` / `hintTextFont` / `backgroundColor` / `accentColor` の 8 項目は `KsBridgeCellStyle.resolve()` (`KsBridgeCellStyle.kt:58-72`) で引数を取り違えても検出されない。同種の取り違えは `KsBridgeGateway` の `Style()` (iOS `Platforms/iOS/KsBridgeGateway.cs:379-397`) にも同じ形で存在する。

デルタスペックが「DTO は Cell の状態フィールド (スタイル上書き…を含む) を輸送しなければならない (SHALL)」としている以上、13 項目の 1:1 対応を 1 本で固定しておきたい。

**推奨修正**

全項目に相異なる値を入れた style を 1 個作り、変換後の 13 フィールドを一括で突き合わせるテストを追加する (両OS)。

---

### [🟡 Minor] `App.cs` のコメントに拡張子なしの裸参照がある

**該当箇所** `samples/maui/KsSettingsView.Sample.Maui/App.cs:15`

```csharp
// iOS ネイティブサンプルのナビゲーションバー表示 (Large Title) と揃える (sample-parity)
```

**問題点**

comment-policy は許容する外部参照を `<domain>/ADR-NNNN` / URL / RFC 等の恒常規格の 3 種のみと定めており、`sample-parity` はいずれにも当たらない (機械検査は無検出だが、これは検出パターンの粗さによるもの)。規約の適用範囲には Sample アプリも明示的に含まれる。

同一 change 内の他ファイル (`SampleScreen.cs:28`、`SampleTheme.cs:14`) はすでに `cross/ADR-0016` を使っており、表記も不統一。

**推奨修正** `(cross/ADR-0016)` へ置換する。

---

### [🔵 Suggestion] 主要な観察点

以下は判定に影響しないが、記録として残す。

1. **`Flush()` の二重送信** (`Internals/KsSettingsController.cs:859-910`): `_replacePendingSections` の `ReplaceSectionKeepingCellIds` は配下 Cell の現在の写しごと送るため、同じ flush で dirty な Cell はその直後の `ReplaceCell` / `ReplaceCells` で再送される。無害だが冗長。
2. **`open class KsBridgeCell`** (`ios/Sources/KsSettingsViewBridge/KsBridgeCell.swift:30`): 派生 DTO は全て同一モジュール内なので `public` で足りる。`makeCell(id:relay:)` は `internal` で override できないため、モジュール外の派生クラスは**無音で LabelCell に化ける**。誤用の口を開けているだけ。
3. **`DateFormatter` の毎回生成** (`ios/Sources/KsSettingsViewBridge/KsBridgeValueTransport.swift:172-177`): `parse` 1 回で最大 2 個生成する。UI 層は同じ理由で `CachedDateFormatter` を持っている。日付 Cell を多く含む `setRoot` で無駄が累積する。
4. **iOS の最終フォールバック定数が UTC 基準** (同 `:39,50`): `Date(timeIntervalSince1970: 0)` は 1970-01-01T00:00Z で、端末 TZ 次第で「1970-01-01」にならない。Android は TZ 非依存の `LocalDate.of(1970,1,1)`。固定書式のセルフパースが失敗する場合のみ到達するため実質到達不能。
5. **`TimeSpan` の範囲外値が無音で丸まる** (`Internals/KsWireValues.cs:21,31`): 実測で `TimeSpan.FromHours(-5)` → `"05:00"` (符号消失)、`FromHours(26)` → `"02:00"` (日跨ぎ切り捨て)。`TimeSpan` 型は壁時計時刻の域外値を許すため、`Time` の setter で正規化するか契約として明記すると安全。
6. **5 桁年の両OS差**: Android は `uuuu` 固定幅 strict で `"12026-08-10"` を拒否、iOS は往復照合方式のため受理する。facade の生成元が `DateTime` (最大 4 桁年) なので到達不能。
7. **`samples/maui/README.md` の未追随** (`:23,129-130,148-151`): 削除済みの `LabelCell 検証` ページと、csproj から外れた `Microsoft.Maui.Controls.Compatibility` / `ReactiveProperty.Core` が残り、新規 4 デモ画面は未掲載。CLAUDE.md により README の書き換えは `docs-refresh` 経由に限定されるため**本 change で直すのは正しくない** — 蒸留への申し送りに `docs-refresh` 必要項目として明記しておくこと。
8. **ルートメニューの chrome** (`samples/maui/.../MenuPage.cs:48-57`): iOS は `NavigationLink`、Android は `ListItem` + chevron + divider に対し、MAUI は `Label` のみで区切り線・chevron・選択フィードバックがない。文言と並び順は一致しているため parity の必須項目は満たすが、「OS 標準 chrome の差」ではなく MAUI 側だけの簡素化。
9. **`ks-settingsview-compose` の flaky テスト** (スコープ外): `KsSettingsViewComposeTest.kt:231`「DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される」が `./gradlew test --rerun-tasks` で 1 回失敗 (expected:3 but was:2)、モジュール単独で 2 回再実行するといずれも成功した。本変更が触れないモジュールの既存 flakiness だが、`./gradlew test` 単体では UP-TO-DATE でテストが走らないため見逃されやすい (`--rerun-tasks` が必要)。別途追跡を推奨。

---

## review-handoff.md 19 項目の検証結果

| # | 論点 | 検証結果 |
|---|---|---|
| 1 | BG8605 容認 | **要再評価** — 前提の「3重複製が代償」は不成立 (Minor-4)。「公開 DTO 面は全メンバー生成済み」は報告書実読で正しいと確認 |
| 2 | BG8606 / BG8A00 | **一部訂正要** — `KsBridgePickerCell.WhenMappings` は本変更由来 (Minor-5)。BG8A00 は `Metadata.xml:27-33` の説明どおりで妥当 |
| 3 | 時刻/日付の解釈不能通知を捨てる | **妥当** — spec の既定値化は C#→Native 方向のみを定めており、逆方向で現在値を保つのは安全側。既存挙動の変更でもないため deviation 不要という判断も支持 |
| 4 | `AndroidButtonColor` が net10.0 で固定できない | **許容** — iOS gateway が送らないことは platform TFM でしか表現できず、代替手段がない。`Platforms/iOS/KsBridgeGateway.cs` の `DatePicker()` が当該フィールドを設定していないことをコード上で確認した |
| 5 | uiStyle 既定の非対称 (iOS Wheels / Android Material) | **妥当** — 各 native の既定値そのもの。spec「未指定時は native 既定」に忠実 |
| 6 | DTO 輸送面は native Cell struct 全件を正とした | **妥当**。ただし `Section.headerHeight` は deviation 記録のうえ結局輸送されており、記述と実装が食い違ったまま。蒸留時に整理を |
| 7 | `ValueText` を Switch/Checkbox/Radio/SimpleCheck/Button に付けない | **記述が事実と逆** — 実装では 5 種すべてが `ValueText` を持つ (`SwitchCell.cs` / `CheckboxCell.cs` / `RadioCell.cs` / `SimpleCheckCell.cs` / `ButtonCell.cs` の各 `nameof(ValueText)`、および `Internals/KsCellSnapshots.cs` の全写しに `ValueText`)。実装自体は native の対応 Cell (`KsBridgeSwitchCell.valueText` 等) と揃っており正しく、#15「配置は各 Cell 個別」とも整合する。誤っているのは #7 の記述のみ — 蒸留前に訂正を |
| 8 | `CanExecuteChanged` を `OnPropertyChanged(nameof(IsEnabled))` で通知 | **妥当** — 値は不変だが実効有効状態の信号として機能し、`AffectsSnapshot` 経由で再送される。TwoWay バインドへの副作用も同値のため無害 |
| 9 | 9.1 Suggestion 6 件全却下 | **妥当** — デルタスペックに対応 Requirement がない。R1-S2 のみ適用 (#13) も整合 |
| 10 | DataTemplateSelector の例外契約 | **spec 解釈として許容** — MAUI の `DataTemplateSelector.SelectTemplate` が先に `NotSupportedException` を投げるため facade から介入不能。null 返却と型不一致は `InvalidOperationException` で揃っており (`KsItemsSourceBinder.cs:332-358`)、テストも 2 件ある。spec の「既存 DataTemplate 経路と同じ例外契約」は満たしていると読める |
| 11 | `ValueText*` スタイルを CellBase に配置 | **妥当** — native `CellStyle` が基底に持つ以上、spec の「CellBase は CellStyle に対応するスタイルプロパティを公開」に従う配置が正しい |
| 12 | tasks 5.1 の style / icon は別テストで担保 | **妥当** — `ThemeAndCellStyleTests` (11 件) / `IconSourceTests` (9 件) を実読し、接続待ち・世代 latest-wins・失敗 fallback・Host 解放跨ぎまで押さえられていることを確認。ただし CellStyle 全項目の 1:1 対応は Bridge 側で不足 (Minor-7) |
| 13 | R1-S2 適用 (`_generated` 更新順) | **妥当** — 再現→修正→回帰の手順が踏まれている |
| 14 | `Section.HeaderHeight` を dirty-tracking へ載せた | **妥当** — 実行時変更を silent no-op にしないための一歩踏み込みは、deviation の趣旨 (native サンプルとの構成一致) に沿う。`_visibilityDirtySections` → `_replacePendingSections` の改名も内容に即しており、`SectionHeaderHeightTests` 4 件で固定されている |
| 15 | `ValueText` を各 Cell 個別に配置 | **妥当** — EntryCell の TwoWay 特例との同名 BindableProperty 並立を避ける根拠は納得できる |
| 16 | iOS ビルドに `DEVELOPER_DIR` 必須 | **確認済み** — 当レビューでも同指定で iOS テスト 710 件を実行 |
| 17 | `Xamarin.Google.Android.Material` 1.12.0.5 固定 | **妥当** — Android Binding の rebuild が 0 エラーで通ることを実測。版選定の根拠 (R.txt 非推移的 → `colorPrimary` 欠落) も具体的で、native 層の恒久対策を別課題化した判断も適切 |
| 18 | `App.cs` の `PrefersLargeTitles` 追加 | **妥当** (ただしコメントの参照形式に Minor-9) |
| 19 | アイコンの色・形状差 | **deviation 記録済み** — サンプルの SVG 11 個は Android vector drawable の `pathData` と一致しており、サイズ感一致の条件は満たしている |

---

## 確認して問題がなかった観点

- **ビルド・テスト (全て実測)**: MAUI 227 件 / iOS 710 件 (4 バンドル) / Android 2152 件が全緑。facade は 3 TFM で警告 0。Android Binding は rebuild で 0 エラー (警告は既知の BG8605 系のみ)
- **足場アーティファクトの保全**: `proposal.md` / `design.md` / `specs/` は無変更。`tasks.md` の差分はチェックボックスと probe 結果への案内 1 行のみ
- **tasks.md の虚偽チェックなし**: 7.2 (IME 実機確認) のみ未チェックで残っており、既知の保留事項と一致
- **書き戻し経路**: 10 プロパティすべてに書き戻しと同値チェックがあり、`NativeValueWritebackTests` (16 件) が radio グループ同期・集合等価・折り返し収束・TwoWay 伝播・未知 ID / 型不一致まで押さえている。トートロジーなアサーションは見当たらない
- **delegate / listener の寿命**: iOS は `weak var delegate` (`KsBridgeInteractionRelay.swift:20`)、Android は Bridge 強参照 + null 解除、C# 側は gateway が実体を強保持。Cell に注入される閉包は relay と cellId (String) しか捕捉せず facade を root 化しない。`InteractionLifetimeTests` の `WeakReference` 検証で SettingsView と gateway の回収も固定されている
- **cellId 温存**: `ReplaceSectionKeepingCellIds` (`KsSettingsController.cs:645-683`) → `AdoptCellID` → Bridge の canonical UUID 判定という経路が両OSで揃っており、「同一インスタンス由来は温存 / 別インスタンスは再採番」の 2 Scenario がテストで固定されている
- **icon の解決**: Cell 世代と解決口 (MauiContext) 世代の二重管理で latest-wins。Handler 切断中の完了破棄、再接続時の再解決、除去済み Cell への完了破棄まで `IconSourceTests` が押さえている
- **値変換の culture 非依存**: iOS は `Locale(identifier: "en_US_POSIX")` + 往復照合、Android は `Locale.ROOT` + `ResolverStyle.STRICT` + `uuuu`、C# は `CultureInfo.InvariantCulture` + `TryParseExact`。区切り違い・桁数不足・暦上不正日のいずれも 3 実装で同じく既定値へ倒れる
- **サンプルの parity**: 4 画面の Section 数 / Cell 数 / 全表示文言 / デモデータ (選択肢・min/max/step/unit・初期値・placeholder・PageTitle) / `SampleTheme` の 16 色 (α 込み RGB 値) が iOS・Android と一致。`LabelCell 検証` ページは csproj・メニュー・ナビゲーションから完全に除去済み
- **comment-policy**: `python3 scripts/comment-policy-lint.py --summary` は禁止 0 件 (479 ファイル)。目視でも変更経緯・レビュー通番・デルタスペック構文キーワードの混入なし (Minor-9 の 1 件を除く)
- **probe 成果物**: ビルドツリーに残存なし (`artifacts/probe/` へ退避済み)

---

## アクションプラン

優先度順。

1. **Major-1 (Android パスワードマスク)** の扱いをオーナーと決める — 本 change で `EntryCellViewHolder` を直すか、別 change へ切り出して deviation.md に記録するか。放置は不可 (機密情報の平文表示)
2. **Major-2 (sample-parity の未記録)** — deviation.md へ 1 行追記し、`EntryCell` の値変更通知の公開を後続課題として残す
3. **Minor-3 (`SelectedItem` の stale 値)** — 導出の対称化とテスト追加。spec の SHALL に直接かかる
4. **Minor-4 / Minor-5 (binding 面)** — `@JvmSynthetic` を実測して BG8605 を解消し、`when` → `if` で `WhenMappings` を消す。review-handoff の判断根拠を実測結果で更新する
5. **Minor-6 / Minor-7 / Minor-8 (テストの検出力)** — native 既定の導出化または照合テスト、CellStyle 13 項目の 1:1 テスト、トートロジー 2 件の修正
6. **Minor-9** — `(sample-parity)` → `(cross/ADR-0016)`
7. Suggestion 群は蒸留・後続へ。特に #7 (README) は `docs-refresh` 必要項目として蒸留への申し送りに追記し、#9 (compose flaky) は別途追跡を起こすこと

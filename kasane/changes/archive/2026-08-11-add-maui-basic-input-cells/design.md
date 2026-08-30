# Design: add-maui-basic-input-cells

## Context

phase-2 (add-maui-core) で facade の骨格 (SettingsView / Section / CellBase / LabelCell、dirty-set フラッシュ、gateway seam) は確立済み。Native は両OSとも 11 Cell 種 + 選択面が実装済みで、本変更は「既存 Native 機能を facade + Bridge interop まで配線する」ことが中心。設計判断の大半は phase-4 の agenda 議論で確定し、maui/ADR-0011 (per-type DTO)・ADR-0012 (双方向値の輸送規約)・ADR-0013 (DatePickerUIStyle 統一 enum) として proposed 起票済み。本書はそれらの実装形と、agenda 後に発見された icon の扱い (オーナー判断: ImageSource 実体化) を固める。

## Goals / Non-Goals

proposal.md のとおり。追加の明確化: Native (UI/Core) の公開 API は変更しない。Bridge モジュール (iOS `KsSettingsViewBridge` / Android `ks-settingsview-bridge`) への追加は additive。

## Decisions

### Decision 1: per-type 展開の実装形 (maui/ADR-0011 の適用)

**採用案:** facade は `CellBase` 派生11種を phase-2 の LabelCell と同型 (BindableProperty + `CreateSnapshot()` / `AffectsSnapshot()` の重ね) で追加する。Snapshot は Cell 種ごとの record、Bridge DTO は Cell 種ごとに両OSへ追加 (`KsBridgeSwitchCell` 等)。`KsBridgeGateway.ToDto()` は facade Cell 型の型スイッチで対応 DTO を生成する。
**理由:** ADR-0011 のとおり。Native の個別 Cell 型と 1:1 で対応し、変換テスト (`ConversionPathTests`) が種ごとに書ける。
**混載のための共通基底型 (second-opinion-001 #1):** 現状の `KsBridgeSection.cells` / `KsBridgeCellUpdate.cell` は `KsBridgeLabelCell` 固定であり、異種 DTO を混載できない。両OSに **共通基底 DTO 型 `KsBridgeCell`** (iOS: `@objc open class`、Android: `abstract class`) を新設し、`KsBridgeLabelCell` を含む全 Cell DTO をその派生に改める。`KsBridgeSection.cells` / `addCell` / `KsBridgeCellUpdate` / `KsBridgeRootBuilder` / C# gateway (`IKsSettingsGateway`) のシグネチャは基底型で受ける。ID 採番・共通フィールド (cellID) は基底へ引き上げる。
**代替案:**
- **A: 単一 wide DTO + cellType 判別** — ADR-0011 で却下済み (判別の二重化・union 却下経緯との不整合)。
- **B: Cell 種別ごとの add/replace API を Section / Bridge に増設** — 11種 × 操作種でメソッドが爆発し、ADR-0002 の「Store 操作 1:1」の面も崩れるため却下。

### Decision 2: interaction delegate/listener の interop 設計 (maui/ADR-0003 の具体化)

**採用案:** Bridge に SettingsView (= Bridge instance) あたり1個の通知チャネルを新設する。iOS は `@objc public protocol KsBridgeInteractionDelegate`、Android は `interface KsBridgeInteractionListener`。メソッドは Cell 種別ごとに分け、値は ADR-0012 の輸送規約に従う:

- `commandCellTapped(cellId: String)` / `buttonCellTapped(cellId: String)`
- `switchCellChanged(cellId: String, isOn: Bool)` / `checkboxCellChanged(cellId, isChecked)` / `simpleCheckCellChanged(cellId, isChecked)`
- `radioCellSelected(cellId: String, value: String)`
- `entryCellTextChanged(cellId: String, text: String)`
- `pickerCellSelectionChanged(cellId: String, index: Int)` / `pickerCellMultiSelectionChanged(cellId: String, indices: [Int])`
- `numberPickerCellChanged(cellId: String, value: Int)`
- `timePickerCellChanged(cellId: String, time: String)` (ISO "HH:mm") / `datePickerCellChanged(cellId: String, date: String)` (ISO "yyyy-MM-dd")

Bridge は DTO → native Cell 変換時に各 Cell のコールバック (`onTap` / `onValueChanged` 等) を注入し、delegate/listener へ転送する。C# 側は `KsBridgeGateway` が実装を保持し、`KsSettingsController` の書き戻し経路 (Decision 3) へ渡す。通知は native UI スレッド上で同期に呼ばれる (facade の UI スレッド契約と整合、marshal 不要)。
**所有と寿命 (second-opinion-001 #8 / second-opinion-002 #7 修正):** iOS の delegate 参照は ObjC 慣例どおり **weak**、Android の listener は Bridge が保持し `null` 設定で解除可能とする。C# 側は gateway (facade と同寿命) が delegate/listener 実装を **strong 保持**して managed peer の回収を防ぐ。登録・解除は**実在する lifecycle である Handler の connect / disconnect に結び付ける**: 登録は Handler 接続 (`makeHost` 前後)、解除は切断 (`releaseHost`) 時 (現行 facade に dispose 口は無く、操作通知は Host 表示中にしか発生しないため connect/disconnect で必要十分)。切断中に facade が回収される場合、Android Bridge の listener 参照が facade を root 化しない (解除済みのため)。native Cell 内のコールバック閉包は Bridge → delegate(weak)/listener 参照のみを掴み、facade インスタンスを直接掴まない (SettingsView の回収を妨げない — 既存の weak 購読方針と整合)。
**理由:** ADR-0003 の決定 (単一 delegate、メソッド名で種別識別) の忠実な具体化。コールバック注入は Bridge が Store と Cell 構築を所有する ADR-0001 の構造から自然に導かれる。
**代替案:**
- **A: Cell ごとの個別 delegate** — ADR-0003 で却下済み (interop 境界の登録管理が煩雑)。
- **B: イベント名 + payload union の単一メソッド** — union 表現の interop が不格好 (ADR-0002 の union 却下と同根) のため却下。

### Decision 3: 双方向書き戻しと入口同値チェック (maui/ADR-0012 の適用)

**採用案:** `KsSettingsController` に書き戻し入口 `ApplyNativeValue(cellId, apply)` を設け、delegate 通知ごとに: `FindCell(cellId)` → 対象プロパティの現値と通知値を比較 → 同値なら何もしない → 異なれば SetValue。以後は既存の dirty-set/flush が必須コミットとして `replaceCell(s)` を送る。phase-2 の `ShouldPublish()` フック (常に true の予約) は撤去する。
**書き戻し対象の正規一覧 (second-opinion-001 #4)**: 次の **10プロパティ**とする (proposal 等の「8プロパティ」は agenda 由来の名目で、こちらが正): `SwitchCell.On` / `CheckboxCell.Checked` / `SimpleCheckCell.Checked` / `RadioCell.SelectedValue` (group 同期) / `EntryCell.ValueText` / `PickerCell.SelectedIndex` / `PickerCell.SelectedIndices` / `NumberPickerCell.Number` / `TimePickerCell.Time` / `DatePickerCell.Date`。これらの BindableProperty は **`BindingMode.TwoWay` を既定**とする (AiForms 互換・ユーザー操作で値が変わるプロパティのため)。その他のプロパティは OneWay 既定。
**理由:** ADR-0012 のとおり (書き戻し = 必須コミット、発行時点の抑止はコミット欠落の温床)。
**Radio の group 書き戻し:** native `RadioCell` は per-cell に `selectedValue` を持つため、radio 選択通知 (cellId, value) の書き戻しは通知元 Cell だけでなく**同一 `GroupId` の全 RadioCell の `SelectedValue`** に適用する (でないと同グループ他 Cell の選択表示が旧値のまま壊れる)。同値チェックは Cell ごとに適用され、既に新値を持つ Cell はスキップされる。
**代替案:**
- **A: `ShouldPublish()` でのエコー抑止** — ADR-0012 で却下済み。
- **B: 書き戻し中フラグで dirty 化抑止** — ADR-0012 で却下済み (コミット経路の遮断)。
- **C: Radio の書き戻しを通知元 Cell のみに適用** — 同グループ他 Cell の表示が更新されず片肺になるため却下。

### Decision 4: PickerCell の公開面と SelectedItem 解決

**採用案:** `PickerCell` は `ItemsSource` (`IList<string>`)・`SelectionMode` (Single/Multiple)・`SelectedIndex` (int、未選択 -1 相当は nullable int)・`SelectedIndices` (`IList<int>`)・`MaxSelectedNumber`・`PageTitle`・`ValueText`・`DisplayFormatter` (`Func<string, string>?`、CLR プロパティ) を公開する。`SelectedItem` (string?) は `SelectedIndex` ⇔ `ItemsSource` の相互導出プロパティとして公開する (SelectedItem 設定 → IndexOf で SelectedIndex 更新 / index 通知 → SelectedIndex 更新 → SelectedItem 追随)。ItemsSource 未設定・範囲外 index のとき `SelectedItem` は null で、`SelectedIndex` が正。`DisplayFormatter` は facade が items へ適用してから輸送し、native の `displayFormatter` は使わない。
**理由:** interop は native の実体 (index) のまま (ADR-0012)。SelectedItem は AiForms 互換の書き味 (agenda 論点2決定) を facade 内で完結して提供する。DisplayFormatter の関数は interop を越えられないため、適用済み文字列の輸送に倒す (表示結果は native 適用と同一 — 項目表示の全箇所に適用される契約のため)。
**代替案:**
- **A: SelectedItem を正としてプロパティ1本にする** — 輸送 (index) との対応が ItemsSource 依存になり、ItemsSource 未設定時に状態を表現できないため却下。
- **B: displayFormatter を interop コールバックとして輸送** — 表示のたびに interop 越えの関数呼び出しが発生し、得るものがないため却下。

### Decision 5: Time / Date / Keyboard の MAUI 型と変換

**採用案:** `TimePickerCell.Time` は `TimeSpan`、`DatePickerCell.Date` は `DateTime` (日付部分のみ意味を持つ)、`EntryCell.Keyboard` は `Microsoft.Maui.Keyboard` を公開する。輸送は Time/Date が ISO 文字列 (ADR-0012)、Keyboard は facade 内部 enum (Int) に正規化して輸送し、Bridge が iOS `UIKeyboardType` / Android `InputType` へ変換する。対応の取れない Keyboard 値は各OS の既定へ fallback する。
**wire 契約の詳細 (second-opinion-001 #9):**
- Keyboard wire enum: `Default=0 / Plain=1 / Text=2 / Chat=3 / Url=4 / Email=5 / Numeric=6 / Telephone=7` (`Microsoft.Maui.Keyboard` の静的プロパティ 1:1)。iOS/Android の native 型への対応は Bridge 内の変換表 (実装 + テストが正)。未知値は native 既定へ fallback。
- nullable scalar の interop 表現: `@objc` 境界で Optional Int が表現できないため、「欠落 = 既定」を表すセンチネル (負値) または boxed NSNumber を probe で確定する (uiStyle enum も同じ方式)。
- ISO 文字列の生成・解釈は **InvariantCulture / 固定書式** ("HH:mm" / "yyyy-MM-dd") で行う。生成側は自前 (facade / Bridge) のためパース失敗は契約違反 — 失敗時は操作によらず一律に**該当フィールドを型の既定値 (時刻 00:00 / 日付 1970-01-01) で構築 + DEBUG 診断**とし、実行時例外にしない (「現値維持」は setRoot / insert に現値が存在せず操作別に破綻するため不採用 — second-opinion-002 #4)。両OSで同一結果になる。範囲外 index は正規化せず透過する (native の「モデル値を正規化しない」契約に従う)。
- 複数選択 (`SelectedIndices`) の wire 表現は**昇順・重複除去に正規化した Int 配列**とし、書き戻し入口の同値チェックは**集合等価** (順序・重複無視) で比較する — 順序違いによる再配信ループを防ぐ (second-opinion-002 #5)。
**理由:** ADR-0004 (公開 API は MAUI 慣例型)・ADR-0012 (壁時計値の文字列輸送) の適用。AiForms も Time/Date/Keyboard に同型の MAUI 型を使っており移行性が保たれる。
**代替案:**
- **A: DateTime / TimeSpan を epoch で輸送** — ADR-0012 で却下済み (TZ 事故)。
- **B: Keyboard を文字列名で輸送** — enum Int と実質同等だが typo 耐性で劣るため却下。

### Decision 6: DatePickerUIStyle 統一 enum (maui/ADR-0013 の適用)

**採用案:** `DatePickerUIStyle { Calendar, Wheels }` (nullable、null = native 既定)。DTO は enum Int (未指定 = 欠落) で輸送し、Bridge が iOS `.calendar`/`.wheels`、Android `Material`/`Spinner` へ変換する。`AndroidButtonColor` は接頭辞付き nullable。
**理由:** ADR-0013 のとおり。
**代替案:** ADR-0013 の Alternatives を参照 (接頭辞付き別プロパティ / Spinner 命名 — いずれも却下済み)。

### Decision 7: IconSource は MAUI image source service で実体化して輸送する

**採用案:** `CellBase.IconSource` (`ImageSource?`、AiForms 互換命名) を公開する。原典 AiForms と同じく MAUI 標準の image source service で非同期に platform 画像 (iOS `UIImage` / Android `Drawable`) へ解決し、解決完了時に該当 Cell の内容更新 (dirty → `replaceCell`) として Bridge DTO の platform 画像フィールドへ載せる。native 側は既存の `KsImage.uiImage(UIImage)` / `KsImage.Drawable(Drawable)` case で受ける (native 変更なし)。`IconSize` / `IconRadius` は native `CellStyle` に対応概念が実在する (inventory A 分類) ため、CellStyle 系プロパティ (nullable) として公開し style 輸送に含める (second-opinion-002 で当初の「対応概念なし」記述の誤りを訂正)。
**解決の所有者と競合規則 (second-opinion-001 #5):** Cell は Handler を持たない (logical tree に載らない) ため、**解決は `KsSettingsController` が所有**し、`SettingsViewHandler` の MauiContext を controller 経由で供給する (原典の `ImageSourcePartLoader` は handler 前提のため直接は使わず、`IImageSourceServiceProvider` を controller から引く)。Handler 未接続 (MauiContext なし) の間は解決を保留し、接続時にまとめて解決する。競合は **Cell ごとの世代番号で latest-wins** — `IconSource` 変更・null 化のたびに世代を進め、古い世代の解決完了は破棄する。**Handler (MauiContext) の世代も競合判定に含める** — Handler 切断時は進行中の解決をキャンセルして結果を破棄し、再接続時に現行 `IconSource` を新しい MauiContext で再解決する (旧 Context 由来の画像が後から採用されない)。解決失敗は icon なしとして確定し (世代は維持)、次の `IconSource` 変更で再試行される。platform 画像の解放は ARC / GC に任せ、明示 dispose は行わない (置換された旧画像への参照は DTO 更新で切れる)。
**理由:** オーナー判断 (「B しかない。原典にその機構があるので重くない」)。原典実装 (`CellBaseView.cs` の `ImageSourcePartLoader` + `SetImageSource`) で機構の実在と規模を確認済み。KsImage のプラットフォーム値 case をそのまま使うため native 契約に触れない。
**代替案:**
- **A: 接頭辞付き platform プロパティ (IOSIconSystemName / AndroidIconResource)** — 輸送は薄いが、MAUI アプリの資産 (MauiImage 等) が使えず利用者価値が低い。ImageSource が MAUI の慣例 (ADR-0004 の精神) であるためオーナー判断で却下。
- **B: icon 非対応** — サンプル2ページ (BasicCells / UnifyCellCommonFields) の完全一致が崩れ、Theme を含めた判断 (論点11) と非整合のため却下。

### Decision 8: Theme / CellStyle の公開形

**採用案:** AiForms 互換の平置きプロパティで公開する — SettingsView に Theme 相当 (背景色・separator 色・Cell 既定色・フォント系等、native `Theme` の対応フィールド)、CellBase / 各 Cell に CellStyle 相当 (`TitleColor` 等) と Cell 固有の `AccentColor`。facade が既存の `KsBridgeTheme` DTO (phase-1 実装済み) と per-type Cell DTO の style フィールドへ束ねる。フォントは FontFamily / FontSize / FontAttributes の分割公開で facade が合成する (ADR-0008 決定済み)。Theme プロパティの変更は `setTheme` 経路 (ADR-0002 の12メソッド、実装済み) へ流す。
**理由:** ADR-0004 / ADR-0008 で型と命名の方針が決定済み。具体フィールドは native `Theme` / `CellStyle` の対応分のみ (対応概念のない項目を作らない)。
**公開面の確定 (second-opinion-001 #3):** 公開プロパティの `MAUI 名 / 型 / native 対応` は phase-2 の棚卸し表 `kasane/roadmaps/maui-support/phases/phase-2-maui-core/artifacts/2026-08-06-aiforms-surface-inventory.md` (A 分類) を正として確定する。inventory に無い本 change の追加分は次のとおり: `DatePickerUIStyle` (Decision 6)・`AndroidButtonColor`・`Section.IsVisible` (Decision 9)・双方向10プロパティの binding mode (Decision 3)。実装は inventory との対応を `CellShapeTests` で固定する。
**代替案:**
- **A: Theme をオブジェクト1個のプロパティ (`SettingsView.Theme = new KsTheme {...}`) で公開** — AiForms の平置きスタイル (XAML の Style / DynamicResource との親和) と乖離し、移行性を損なうため却下。

### Decision 9: Section.IsVisible (agenda 決定の適用)

**採用案:** `KsBridgeSection` (両OS) に `isVisible` を追加し `makeSection()` へ渡す。facade `Section.IsVisible` (BindableProperty、既定 true) を公開し、変更は Section 版 visibility dirty-tracking から `ReplaceSection` 単発配信で送る。
**cellId 温存の範囲と対応規則 (second-opinion-001 #2):** ID 温存を要求するのは**同一 facade Section インスタンスに起因する `ReplaceSection`** (IsVisible 変更・header/footer 等の内容差し替え) に限る。このケースでは facade が Section ごとに保持する採番済み cellId 対応 (`_cellsById`) をそのまま使い、輸送 DTO の cells に既存 ID を載せて再採番を避ける。**別インスタンスへの Replace** (observable コレクションの Replace イベント) は新規 Section 扱いで再採番してよい (旧 Cell の購読・逆引きは解除される)。Cell の追加・削除・移動は既存の構造操作 (`insertCell` / `removeCell` / `moveCell`) 経路であり `ReplaceSection` を通らない。
**理由:** agenda 論点12の決定 + 現行実装 (replaceSection は配下 Cell を再採番) との整合。全面温存は過大要求で、双方向バインドの前提 (cellId 逆引き) が必要なのは同一インスタンス差し替えのケースだけ。
**代替案:**
- **A: 専用 `setSectionVisibility` Bridge メソッド** — 論点12で却下済み (Store 公開操作に存在せず ADR-0002 の 1:1 に反する)。

### Decision 10: DataTemplateSelector の解決 (agenda 決定の適用)

**採用案:** ItemsSource のテンプレート実体化直前に `template is DataTemplateSelector selector` なら `selector.SelectTemplate(item, container)` で実テンプレートへ解決してから `CreateContent()` する。facade 契約の「渡せない」制約を解除する。
**適用範囲と細則 (second-opinion-001 #11 / second-opinion-002 #9 修正):** SettingsView 直下 (Section 生成) と Section 配下 (Cell 生成) の**両階層**に適用する。`container` には**テンプレートが設定されている BindableObject** (SettingsView 直下なら SettingsView、Section 配下なら当該 Section) を渡す — Section は SettingsView へ配置される前でも ItemsSource 設定時に即生成する現行実装のため、「所属 SettingsView」は構築順序によって存在せず container にできない。selector が null を返した場合・selector が selector を返した場合・生成物の型不一致は、既存 DataTemplate 経路と同じ例外契約 (`InvalidOperationException` 系) に揃える。ItemsSource の Replace イベントでは新 item に対して再選択する。
**理由:** agenda 論点4の決定 (オーナー指摘)。
**代替案:**
- **A: 非対応継続** — selector 代入経路が型的に開いたまま実行時例外になる口が残るため却下。

### Decision 11: タップ通知と ICommand の実効有効状態

**採用案:** `CommandCell` / `ButtonCell` の実効有効状態を `IsEnabled && (Command?.CanExecute(CommandParameter) ?? true)` とし、native の `isEnabled` へ反映する。`Command.CanExecuteChanged` を購読して実効状態を追随させる。タップは実効有効のときだけ発火し、順序は `Tapped` イベント → `Command.Execute(CommandParameter)`。`Command` の差し替え・null 化では旧 Command の `CanExecuteChanged` 購読を解除し、旧 Command からの通知は無視する (購読は weak または解除徹底で Cell / SettingsView の回収を妨げない)。
**理由:** second-opinion-001 #7。MAUI の Button と同じ標準契約で、CanExecute=false の Command が実行される穴を塞ぐ。
**代替案:**
- **A: タップ時に CanExecute を確認するだけ (表示は IsEnabled のみ)** — 実行不可なのに有効に見える UI になり、MAUI 慣例 (Button の IsEnabled 連動) から外れるため却下。

## Risks / Trade-offs

- **コールバック方向の interop はプロジェクト初**: Bridge はこれまで C#→Native の一方向。iOS は `@objc protocol` の binding (Native Library Interop で protocol → C# interface 生成)、Android は interface の binding が必要。binding 生成の検証を実装の最初のタスクに置く (probe)。
- **Drawable / UIImage の interop 輸送**: platform 画像オブジェクトを binding 境界で渡す。iOS UIImage は実績のあるパターンだが、Android Drawable の受け渡しは probe で検証する。
- **ボイラープレート量**: DTO 11種 × 両OS。機械的だが、種ごとの変換テストで固定する。
- **Entry の毎キーストローク往復**: delegate → 書き戻し → replaceCell → native 再バインド。native 側の同値 setText ガード (両OS実在確認済み) が IME・カーソルを保護するが、サンプルでの実機確認を行う。

## Migration Plan

additive のみ。破壊的変更なし。`ShouldPublish()` 撤去は internal のため影響なし。

## Open Questions

- なし (binding 表現の可否は Open Question ではなく実装冒頭の probe タスクとして扱う)

## ADR 候補

- **Decision 7 (IconSource の実体化方式)**: 境界を越える (MAUI image pipeline → native 画像輸送) / 将来を制約 (phase-5 CustomCell・phase-6 MauiView 実体化の先例になる非同期実体化パターン)。ADR 候補とする。
- Decision 1 / 3 / 6 は起票済み ADR (maui/ADR-0011 / 0012 / 0013) の適用のため候補にしない。Decision 2 は ADR-0003 の具体化 (蒸留時に ADR-0003 への追記で足りるか判断)。Decision 4 / 5 / 8 / 10 は局所的な API 詳細でコード+テストに任せる。Decision 9 の cellId 温存制約は蒸留時に concepts (native-bridge.md) への追記を検討。

# オリジナル `AiForms.Maui.SettingsView` プロパティの移植漏れ調査

調査日: 2026-06-08
調査者: explore セッション
対象: KsSettingsView の Native 基礎層（Android Kotlin + iOS Swift）

## 1. 調査範囲

オリジナル側 3 ファイル + 個別 Cell ファイル群と、KsSettingsView Native 実装（`android/ks-settingsview-ui/`、`ios/Sources/KsSettingsView*/`）および OpenSpec 仕様（`openspec/specs/settings-view-core/`、`openspec/specs/cell-types-basic/`）を突合した。

### 対象ファイル

**オリジナル**
- `AiForms.Maui.SettingsView/SettingsView/SettingsView.DefineProperites.cs`
- `AiForms.Maui.SettingsView/SettingsView/Section.cs`
- `AiForms.Maui.SettingsView/SettingsView/Cells/CellBase.cs`
- `AiForms.Maui.SettingsView/SettingsView/Cells/{LabelCell,CommandCell,ButtonCell,SwitchCell,CheckboxCell,RadioCell,SimpleCheckCell,PickerCell,TextPickerCell,NumberPickerCell,DatePickerCell,TimePickerCell,EntryCell,CustomCell}.cs`

**KsSettingsView 側**
- iOS: `ios/Sources/KsSettingsViewCore/{Section,KsCell,...}.swift`、`ios/Sources/KsSettingsViewUI/{Theme,CellStyle,LabelCell,CommandCell,ButtonCell,SwitchCell,CheckboxCell,RadioCell,SimpleCheckCell}.swift`
- Android: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/{Theme,CellStyle,KsSettingsViewStyle,...}.kt`
- 仕様: `openspec/specs/settings-view-core/spec.md`、`openspec/specs/cell-types-basic/spec.md`

### in-progress な後続変更提案（基礎の対象外と判定したもの）

- `add-cell-types-input`（EntryCell / PickerCell / TextPickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）
- `add-cell-types-custom`（CustomCell）
- `add-maui-core`, `add-maui-bridge`, `add-maui-cells`, `add-samples-maui`（MAUI 層全般）

## 2. 凡例

| 記号 | 意味 |
|------|------|
| ✅ | 移植済 |
| 🟡 | 別 change で計画済（基礎漏れではない） |
| ❌ | 抜け落ちている可能性大（要判断） |
| 🤔 | 意図的廃止か未決定か曖昧（確認したい） |

## 3. SettingsView 単位（`SettingsView.DefineProperites.cs` → `Theme`）

オリジナル `SettingsView` クラスは 40+ の BindableProperty を直接持つフラット構造。
KsSettingsView では「全体スタイル = `Theme` 値型」「Cell 個別 = `CellStyle` 値型」の 2 層に再構成され、Theme は UI 層所属（`KsSettingsViewUI` / `ks-settingsview-ui`）に置かれている（`purify-core-extract-style-to-ui-layer` 完了済）。

### 3.1 移植済（OK）

| オリジナル | KsSV Theme | 備考 | 指示 |
|-----------|-----------|------| ----- |
| `BackgroundColor` | `viewBackgroundColor` | ✅ | 表記揺れ backgroundColorに修正|
| `SeparatorColor` | `separatorColor` | ✅ |
| `SelectedColor` | `selectedColor` | ✅ |
| `HeaderTextColor` | `headerTextColor` | ✅ |
| `HeaderBackgroundColor` | `headerBackgroundColor` | ✅ |
| `HeaderFontSize` | `headerFontSize` | ✅ |
| `FooterTextColor` | `footerTextColor` | ✅ |
| `FooterBackgroundColor` | `footerBackgroundColor` | ✅ |
| `FooterFontSize` | `footerFontSize` | ✅ |
| `CellTitleColor` | `titleColor` | ✅ (Theme 既定として昇格) | 表記揺れ。cellTitleColorに修正 |
| `CellTitleFontFamily/Attributes` | `titleFont` 経由 | ✅ (Compose TextStyle / UIFont に集約) | fontfamilyに課題があったようだが。解決させる |
| `CellBackgroundColor` | `cellBackgroundColor` | ✅ |
| `CellAccentColor` | `cellAccentColor` | ✅ |
| `RowHeight` | `rowHeight` | ✅ |
| `HasUnevenRows` | `hasUnevenRows` | ✅ |

### 3.2 オリジナルになかった独自追加

| KsSV Theme | 意図 | 指示 |
|-----------|------| ----- |
| `disabledTextColor` | `isEnabled = false` 時のテキスト色（cell-types-basic spec 要求） |
| `scrollIndicatorVisible` | スクロールインジケータ表示制御 |

### 3.3 漏れている可能性大（❌）

#### ヘッダ詳細スタイル

| オリジナル | 状況 | 指示 |
|-----------|------| ------ |
| `HeaderPadding` (`Thickness`) | ❌ Theme に該当なし | これはカスタムViewを使ってくださいで済みそうなのでOKとする |
| `HeaderFontFamily` | ❌ Theme に該当なし | 対応する |
| `HeaderFontAttributes` (Bold/Italic) | ❌ Theme に該当なし | 対応する |
| `HeaderTextVerticalAlign` (`LayoutAlignment.End` 既定) | ❌ Theme に該当なし | これもPaddingと同様にカスタムViewを使ってもらうようにするので、無くてOK |

#### フッタ詳細スタイル

| オリジナル | 状況 | 指示 |
|-----------|------| ------ |
| `FooterPadding` (`Thickness`) | ❌ Theme に該当なし | 不要 |
| `FooterFontFamily` | ❌ Theme に該当なし | 対応する |
| `FooterFontAttributes` | ❌ Theme に該当なし | 対応する |

#### Cell 全体既定（Theme 単位で持つべきだったもの）

KsSV では `Theme.titleColor` / `Theme.titleFont` のみが「Cell 全体既定」として昇格しているが、オリジナルは **Description / ValueText / Hint / Icon 系も全体既定** を持っていた。

| オリジナル | 状況 | 指示 |
|-----------|------| ------ |
| `CellTitleFontSize` (独立 Property) | ❌ `titleFont` に丸まる | CellStyleでも対応 |
| `CellValueTextColor` | ❌ Theme に該当なし（CellStyle にはあり） | CellStyleでも対応 |
| `CellValueTextFontSize/Family/Attributes` | ❌ Theme に該当なし | CellStyleでも対応 |
| `CellDescriptionColor` | ❌ Theme に該当なし（CellStyle にはあり） | CellStyleでも対応 |
| `CellDescriptionFontSize/Family/Attributes` | ❌ Theme に該当なし | CellStyleでも対応 |
| `CellHintTextColor` | ❌ Theme に該当なし（CellStyle にはあり） | CellStyleでも対応 |
| `CellHintFontSize/Family/Attributes` | ❌ Theme に該当なし | CellStyleでも対応 |
| `CellIconSize` (`Size`) | ❌ Theme に該当なし（CellStyle にはあり） | CellStyleでも対応 |
| `CellIconRadius` (`double`) | ❌ Theme に該当なし（CellStyle にはあり） | CellStyleでも対応 |


**論点**: オリジナルは「`Theme.CellHintTextColor` を 1 箇所セットすれば全 Cell のヒント文字色が変わる」運用ができたが、KsSV 現状では各 Cell ごとに `CellStyle.hintTextColor` を入れないと変えられない。表示互換性に直接影響する。

→ その通りで完全に移植漏れ、ThemeとCellStyle両方で対応できなくてはならない。優先度は CellStyleが勝つ

### 3.4 命令系・通知系 API（❌）

| オリジナル | 状況 | 指示 |
|-----------|------|------|
| `ScrollToTop` (`bool`, `TwoWay`) | ❌ 該当 API なし | これは別の実装方法を考えるので不要 |
| `ScrollToBottom` (`bool`, `TwoWay`) | ❌ 該当 API なし | 上に同じ |
| `VisibleContentHeight` (`double`, `OneWayToSource`) | ❌ 該当 API なし | 不要 |
| `ItemDropped` (event) | ❌ 該当なし（DragSort 関連） | 後に実装しなおす予定。今は不要 |
| `ItemDroppedCommand` | ❌ 該当なし | 上に同じ |

### 3.5 意図的廃止か曖昧（🤔）

| オリジナル | 推測 | 指示 |
|-----------|------|------|
| `HeaderHeight` (SettingsView 全体既定) | 🤔 `Section.headerHeight` に集約された解釈か? | 移植漏れ |
| `UseDescriptionAsValue` (Android 専用) | 🤔 設計上廃止? `LabelCell.IgnoreUseDescriptionAsValue` と対 | 廃止で良い |
| `ShowSectionTopBottomBorder` (Android 専用) | 🤔 `KsSettingsViewStyle.Classic` の挙動に吸収? | 廃止で良い |
| `ShowArrowIndicatorForAndroid` | 🤔 `CommandCell.hideArrow` に統合済み (Cell 個別化) | これもこのままで良い |
| `ItemsSource` / `ItemTemplate` / `TemplateStartIndex` | 🤔 DSL/Store 駆動への置き換えで廃止? | MAUI固有なのでNativeには不要 |

## 4. Section 単位（`Section.cs` → core `Section`）

### 4.1 移植済（OK）

| オリジナル | KsSV Section | 備考 | 指示 |
|-----------|-------------|------|---------|
| `Title` (SectionBase 由来) | `header: SectionAccessory.text(...)` | ✅ |
| `FooterText` | `footer: SectionAccessory.text(...)` | ✅ |
| `HeaderView` | `header: SectionAccessory.view(KsAnyView)` | ✅ |
| `FooterView` | `footer: SectionAccessory.view(KsAnyView)` | ✅ |
| `HeaderHeight` | `headerHeight` | ✅ |

### 4.2 漏れている可能性大（❌）

| オリジナル | 状況 | 指示 |
|-----------|------|--------|
| `IsVisible` (Section 単位の表示制御) | ❌ 該当なし | 移植が必要 |
| `FooterVisible` | ❌ 該当なし | これは使わなかった気がするのでなくてOK。必要ならカスタムViewでやってもらうで良い |
| `UseDragSort` | ❌ 該当なし（DragSort 機能丸ごと） | 後に刷新実装予定。今は不要 |

### 4.3 意図的廃止か曖昧（🤔）

| オリジナル | 推測 | 指示 |
|-----------|------|------|
| `ItemsSource` / `ItemTemplate` / `TemplateStartIndex` | 🤔 DSL 駆動への置き換えで廃止? | MAUI専用。今は不要 |
| `MoveSourceItemWithoutNotify` / `MoveCellWithoutNotify` / `DeleteSourceItemWithoutNotify` / `InsertSourceItemWithoutNotify` / `DeleteCellWithoutNotify` / `InsertCellWithoutNotify` | 🤔 `SettingsRootDiff.moveCell` / `insertCell` / `removeCell` 経由に置き換え済? | MAUI固有。不要 |
| `SectionCollectionChanged` (event) | 🤔 Store の Diff 駆動で吸収? | MAUI用不要 |
| `SectionPropertyChanged` (event) | 🤔 Store の Diff 駆動で吸収? | MAUI用不要 |
| `CellPropertyChanged` (event) | 🤔 Store の reconfigure 経路に置き換え済? | MAUI用不要 |

## 5. CellBase 単位（共通 Cell プロパティ）

KsSV には `CellBase` 階層は存在せず、各 Cell が直接フィールドを持ち、装飾系は別 `CellStyle` 値型に分離されている。

### 5.1 移植済（OK）

| オリジナル CellBase | KsSV 配置 | 備考 |
|---------------------|----------|------|
| `Title` | 各 Cell の `title` | ✅ |
| `TitleColor` | `CellStyle.titleColor` | ✅ |
| `TitleFontSize` | `CellStyle.titleFont` 経由 | ✅ |
| `TitleFontFamily` | `CellStyle.titleFont` 経由 | ✅ |
| `TitleFontAttributes` | `CellStyle.titleFont` 経由 | ✅ |
| `Description` | Label/Command の `description` | ✅(後述: 一部 Cell のみ) |
| `DescriptionColor` | `CellStyle.descriptionColor` | ✅ |
| `DescriptionFontSize/Family/Attributes` | `CellStyle.descriptionFont` | ✅ |
| `HintText` | Label/Command の `hintText` | ✅(後述: 一部 Cell のみ) |
| `HintTextColor` | `CellStyle.hintTextColor` | ✅ |
| `HintFontSize/Family/Attributes` | `CellStyle.hintTextFont` | ✅ |
| `BackgroundColor` | `CellStyle.backgroundColor` | ✅ |
| `IconSource` | `icon: KsImage?`（Label/Command のみ） | ✅(後述: 一部 Cell のみ) |
| `IconSize` | `CellStyle.iconSize` | ✅ |
| `IconRadius` | `CellStyle.iconRadius` | ✅ |
| `Height` | `CellStyle.cellHeight` | ✅ |
| `IsEnabled` | 各 Cell の `isEnabled` | ✅ |

### 5.2 漏れている可能性大（❌）

| オリジナル | 状況 |指示 |
|-----------|------|----|
| `IsVisible` (Cell 単位の表示制御) | ❌ 該当なし | 必要 |

**論点 A**: `Description` / `HintText` / `IconSource` が KsSV では LabelCell / CommandCell にしか実装されていない。
オリジナルは CellBase 由来で **全 Cell が** 持っていた（SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / ButtonCell も description / hint / icon を出せた）。意図的削除か、SwitchCell.description のみ移植されているので「LabelCell ベース機能のみが横展開された結果の漏れ」か曖昧。

→ おそらく実装エージェントの手抜き。 ちゃんとオリジナルのようにCellBaseの概念を取り入れる必要がある。必ずしも継承という形でなくても良い。全Cellに個別にレイアウトを持つのは避けるべき。

実際の現状（iOS / Android 共通）:
- ✅ `LabelCell`: title, description, valueText, icon, hintText
- ✅ `CommandCell`: title, description, valueText, icon, hintText, hideArrow
- 一部 ✅ `SwitchCell`: title, description（icon / hintText / valueText なし）
- 一部 ✅ `CheckboxCell`: title, description（icon / hintText / valueText なし）
- 最小 `RadioCell`: title のみ（description / icon / hintText なし）
- 最小 `SimpleCheckCell`: title のみ（description / icon / hintText なし）
- 最小 `ButtonCell`: title, titleColor のみ（description / icon / hintText なし）

### 5.3 意図的廃止か曖昧（🤔）

| オリジナル | 推測 |指示|
|-----------|------|-----|
| `Tapped` event | 🤔 一部 Cell (CommandCell / ButtonCell) で `onTap` クロージャに置換、それ以外は廃止? | MAUI用なのでNativeはonTapで良い。
| `Reload()` メソッド | 🤔 `SettingsRootStore.replaceCell` に置換? | Diffが効くはずなので無くて良いはず |
| `Section` プロパティ（back-ref） | 🤔 値型化のため意図的廃止 |
| `Source: IImageSource` / `IsLoading` / `IsAnimationPlaying` / `UpdateIsLoading` / `SetEnabledAppearance` | 🤔 MAUI 内部 API。Native 純化で廃止? | MAUI用につき不要 |

## 6. 個別 Cell プロパティ

### 6.1 LabelCell

| オリジナル | KsSV | 状況 |指示|
|-----------|------|------|----|
| `ValueText` | `valueText` | ✅ |
| `ValueTextColor` | `CellStyle.valueTextColor` | ✅ |
| `ValueTextFontSize/Family/Attributes` | `CellStyle.valueTextFont` | ✅ |
| `IgnoreUseDescriptionAsValue` | (該当なし) | 🤔 `UseDescriptionAsValue` 廃止に連動して廃止? | 廃止で良い |

### 6.2 CommandCell

| オリジナル | KsSV | 状況 |指示|
|-----------|------|------|----|
| `Command` (`ICommand`) | `onTap: () -> Unit` | ✅ |
| `CommandParameter` (`object`) | クロージャ捕獲で吸収 | ✅ |
| `HideArrowIndicator` | `hideArrow` | ✅ |
| `KeepSelectedUntilBack` | (該当なし) | ❌ 漏れ | まぁ無くても困らんので移植不要 |

### 6.3 ButtonCell

| オリジナル | KsSV | 状況 |
|-----------|------|------|
| `Command` / `CommandParameter` | `onTap` | ✅ |
| `TitleAlignment` | `titleAlignment: CellTitleAlignment` | ✅ |

### 6.4 SwitchCell

| オリジナル | KsSV | 状況 |
|-----------|------|------|
| `On` | `isOn` | ✅ |
| `AccentColor` | `accentColor: UIColor?/Color?` | ✅ |

### 6.5 CheckboxCell

| オリジナル | KsSV | 状況 |
|-----------|------|------|
| `Checked` | `isChecked` | ✅ |
| `AccentColor` | `accentColor` | ✅ |

### 6.6 RadioCell

| オリジナル | KsSV | 状況 |指示|
|-----------|------|------|---|
| `Value` | `value` | ✅ |
| (SelectedValue, CellBase 経由) | `selectedValue` | ✅ |
| `AccentColor` | (該当なし) | ❌ 漏れ | 必要。おそらく上述したCellBase対応で補完できるはず|
| (groupId は KsSV 独自追加) | `groupId` | (独自追加) |

### 6.7 SimpleCheckCell

| オリジナル | KsSV | 状況 |指示|
|-----------|------|------|----|
| `Checked` | `isChecked` | ✅ |
| `Value` (`object`) | (該当なし) | ❌ 漏れ | PikerCell実装の時に必要かもしれない。 |
| `AccentColor` | (該当なし) | ❌ 漏れ | 必要 |

## 7. 入力系 Cell（🟡 別 change で計画済）

以下は基礎の対象外。`add-cell-types-input` change が in-progress。

| オリジナル Cell | 計画状況 | オリジナル固有プロパティ |
|----------------|---------|---------------------|
| `EntryCell` | 🟡 in-progress | `Placeholder`, `PlaceholderColor`, `ValueText`, `ValueText*Font*`, `Keyboard`, `IsPassword`, `MaxLength`, `ShowDoneButtonOnIOS`, `TextAlignment`, `AccentColor`, `CompletedCommand` |
| `PickerCell` | 🟡 in-progress | `ItemsSource`, `DisplayMember`, `SubDisplayMember`, `SelectedItem`, `SelectedItems`, `SelectedItemsOrderKey`, `SelectionMode`, `MaxSelectedNumber`, `PageTitle`, `Padding`, `SelectedCommand`, `UseAutoValueText`, `UseNaturalSort`, `UsePickToClose`, `AccentColor` |
| `TextPickerCell` | 🟡 in-progress | `Items`, `SelectedItem`, `SelectedCommand`, `PageTitle`, `PickerTitle`, `IsCircularPicker`, `AccentColor` |
| `NumberPickerCell` | 🟡 in-progress | `Min`, `Max`, `Number`, `PickerTitle`, `SelectedCommand` |
| `DatePickerCell` | 🟡 in-progress | `Date`, `Format`, `MinimumDate`, `MaximumDate`, `InitialDate`, `TodayText`, `IsAndroidSpinnerStyle`, `AndroidButtonColor` |
| `TimePickerCell` | 🟡 in-progress | `Time`, `Format`, `PickerTitleProperty` |

**懸念**: 各入力系 Cell のオリジナル固有プロパティが `add-cell-types-input` の proposal に網羅されているかは別途レビュー要。とくに `PickerCell` のオプション群（`MaxSelectedNumber`, `UsePickToClose`, `UseNaturalSort`, `SubDisplayMember` 等）と `DatePickerCell.IsAndroidSpinnerStyle`, `TodayText` あたり。

## 8. CustomCell（🟡 別 change で計画済）

`add-cell-types-custom` change が in-progress。

| オリジナル | 計画状況 |
|-----------|---------|
| `Content` | 🟡 計画あり |
| `IsMeasureOnce` | 🟡 要確認 |
| `IsSelectable` | 🟡 要確認 |
| `LongCommand` | 🟡 要確認 |
| `ShowArrowIndicator` | 🟡 要確認 |
| `UseFullSize` | 🟡 要確認 |

## 9. 「基礎」段階で意思決定したい論点まとめ

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Theme 単位の Description/ValueText/Hint/Icon 系の格上げ      │
│    オリジナルでは「全体既定 → Cell 個別」の 2 段重ね。          │
│    KsSV は titleColor / titleFont のみ Theme 昇格。             │
│    → 漏れ? それとも「個別 CellStyle で持てばよい」設計判断?     │
├─────────────────────────────────────────────────────────────────┤
│ 2. Header/Footer Padding と Font 系 (Family/Attributes/         │
│    VerticalAlign) の不在                                        │
│    → 単に未追加? 意図廃止?                                      │
├─────────────────────────────────────────────────────────────────┤
│ 3. Cell.IsVisible / Section.IsVisible / Section.FooterVisible   │
│    → DSL/Store 駆動だから「リストから除外」で代替する方針?      │
├─────────────────────────────────────────────────────────────────┤
│ 4. DragSort 機能丸ごと                                          │
│    (Section.UseDragSort + SettingsView.ItemDropped/Command)     │
│    → 後続提案にも未登場。永久に削除? 別 change で予定?          │
├─────────────────────────────────────────────────────────────────┤
│ 5. ScrollToTop / ScrollToBottom / VisibleContentHeight          │
│    → 命令系 API。Store の命令メソッドに置換? それとも未着手?    │
├─────────────────────────────────────────────────────────────────┤
│ 6. Cell.Reload() / Section の同期 Move/Insert/Delete API        │
│    → Store の Diff 駆動で完全置換だと割り切れる?                │
├─────────────────────────────────────────────────────────────────┤
│ 7. 個別 Cell の AccentColor (Radio / SimpleCheck)               │
│    KeepSelectedUntilBack (Command)                              │
│    SimpleCheckCell.Value                                        │
│    → 単純な実装漏れに見える                                    │
├─────────────────────────────────────────────────────────────────┤
│ 8. 非基本 Cell の description / hintText / icon 不在            │
│    (Switch/Checkbox/Radio/SimpleCheck/Button)                   │
│    → 意図的に最小 Cell 化? それとも CellBase 機能横展開漏れ?    │
├─────────────────────────────────────────────────────────────────┤
│ 9. Android 専用フラグ                                           │
│    UseDescriptionAsValue / ShowSectionTopBottomBorder /         │
│    ShowArrowIndicatorForAndroid                                 │
│    → Classic style / hideArrow / etc で吸収済?                  │
├─────────────────────────────────────────────────────────────────┤
│ 10. CellBase 由来 API                                           │
│    Tapped event / Reload() / Section back-ref                   │
│    → 意図的廃止? 確認したい                                    │
└─────────────────────────────────────────────────────────────────┘
```

## 10. 実装影響度サマリ

| 区分 | 件数 | 例 |
|------|------|---|
| ❌ 漏れている可能性大 | 約 30 件 | Header/Footer Padding, Cell 全体既定の Description/Value/Hint 系, IsVisible, DragSort, ScrollToTop/Bottom, 個別 Cell の AccentColor 等 |
| 🤔 意図的廃止か曖昧 | 約 12 件 | UseDescriptionAsValue, ItemsSource/Template, Section 同期 API, Tapped event 等 |
| 🟡 別 change で計画済 | 入力系 6 種 + CustomCell | EntryCell / PickerCell / etc, CustomCell |
| ✅ 移植済 | ~50 件 | Theme/CellStyle/Section/CellBase の主要プロパティ |

## 11. 参考: 関連既存仕様・change

- `openspec/specs/settings-view-core/spec.md`: Section / Cell / Accessory / Diff の Core 規約
- `openspec/specs/cell-types-basic/spec.md`: 7 種基本 Cell の Requirement
- `openspec/changes/archive/purify-core-extract-style-to-ui-layer/`: Theme / CellStyle / KsImage を UI 層へ移動した変更
- `openspec/changes/archive/refactor-accessory-and-root-hf/`: SectionAccessory / RootAccessory / KsAnyView の確立
- `openspec/changes/add-cell-types-input/proposal.md`: 入力系 Cell 6 種の追加計画（in-progress）
- `openspec/changes/add-cell-types-custom/proposal.md`: CustomCell の追加計画（in-progress）

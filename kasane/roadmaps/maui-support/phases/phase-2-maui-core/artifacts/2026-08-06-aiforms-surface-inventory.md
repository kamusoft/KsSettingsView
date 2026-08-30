# 調査メモ: AiForms 公開面 × 現行 core 契約の突き合わせ棚卸し (論点1 用)

2026-08-06、ksn-scout による。対象: AiForms.Maui.SettingsView (ローカルクローン) と kasane/concepts/core/ (+ Theme/CellStyle/Diff の実装コード)。分類: **A** = 現行契約に対応概念あり / **B** = 現行仕様に存在しない機能 / **C** = MAUI 層固有で完結。

## SettingsView (本体)

| AiForms 側 | 分類 | 現行対応 | 備考 |
|---|---|---|---|
| BackgroundColor / SeparatorColor / SelectedColor | A | Theme.backgroundColor / separatorColor / selectedColor | |
| Header* (TextColor/FontSize/FontFamily/FontAttributes/BackgroundColor/Height) | A | Theme.header* | font は現行では統合 (family+attributes → 1 font) |
| Footer* 一式 | A | Theme.footer* | |
| CellTitle* / CellValueText* / CellDescription* / CellHint* | A | Theme.cellTitle* / cellValueText* / cellDescription* / cellHint* | FontSize 独立フィールドは現行になし (font に内包) |
| CellBackgroundColor / CellIconSize / CellIconRadius / CellAccentColor | A | Theme.cell* | |
| RowHeight / HasUnevenRows | A | Theme.rowHeight / hasUnevenRows | |
| HeaderPadding / FooterPadding | B | なし | 現行はレイアウト固定値 |
| HeaderTextVerticalAlign | B | なし | 現行は配置固定 (list-appearance.md) |
| UseDescriptionAsValue | B | なし | Android 専用切替 |
| ShowSectionTopBottomBorder | B | なし | Classic/Modern style 切替に置換済み |
| ShowArrowIndicatorForAndroid | B | 近似: CommandCell.hideArrow (cell 単位) | グローバル切替は非対応 |
| ItemDroppedCommand / ItemDropped | B | なし | ドラッグソート自体が現行仕様に無い |
| ScrollToBottom / ScrollToTop | B | なし | |
| VisibleContentHeight | C | — | native レイアウト結果の OneWayToSource 読み出し |
| ItemsSource / ItemTemplate / TemplateStartIndex (Section 生成) | C | 生成結果は SettingsRoot.sections | MAUI 宣言バインディングの糖衣 |
| Model / ModelChanged | C | — | 変更通知集約。現行の値+Diff 方式と思想が異なる |
| CollectionChanged / SectionCollectionChanged / SectionPropertyChanged / CellPropertyChanged | C | — | INotifyCollectionChanged/PropertyChanged ベース |
| Root (SettingsRoot 型) | C | 概念は SettingsRoot | 可変ツリーの器 |
| ClearCache() | C | — | レンダラーキャッシュ機構固有 |

## Section

| AiForms 側 | 分類 | 現行対応 | 備考 |
|---|---|---|---|
| Title | A | SectionAccessory.text (Header) | |
| FooterText | A | SectionAccessory.text (Footer) | |
| HeaderView / FooterView | A | SectionAccessory.view (KsAnyView) | 任意 View の輸送は phase-6 |
| IsVisible | A | Section.isVisible | |
| HeaderHeight | A | Section.headerHeight | |
| TextColor | B | なし | Section 単位の header 文字色 override は現行に無い |
| FooterVisible | B | なし | 現行は空なら領域を作らない自動判定のみ |
| UseDragSort 等 | B | なし | |
| ItemsSource / ItemTemplate / TemplateStartIndex | C | 生成結果は Section.cells | 下記「仕組み」 |

### ItemsSource / ItemTemplate の仕組み (Section.cs:398-515)

- ItemsSource セット時: 旧分を TemplateStartIndex 起点に末尾から RemoveAt → 新分を `DataTemplate.CreateContent()` で CellBase 生成 (BindingContext に item をセット) して Insert。生成数を templatedItemsCount に保持
- ItemsSource が INotifyCollectionChanged なら Add/Remove/Replace/Reset を Cell コレクションへミラー (Reset は templatedItemsCount 分だけ除去し手動追加 Cell は温存)
- SettingsView 直下 (Root への Section 生成) も同一パターン (SettingsView.DefineProperites.cs:997-1107)
- 現行 core にこの仕組みは無い → MAUI 層でテンプレ生成物を通常の insert/remove/replace として Diff 変換経路 (決定済み論点2) に流す橋渡しが新規要素

## CellBase

| AiForms 側 | 分類 | 現行対応 | 備考 |
|---|---|---|---|
| Title / TitleColor / TitleFont* | A | title / CellStyle.titleColor / titleFont | |
| Description / DescriptionColor / DescriptionFont* | A | description / CellStyle.description* | 現行は ButtonCell のみ description 非公開 |
| HintText / HintTextColor / HintTextFont* | A | hintText / CellStyle.hint* | |
| BackgroundColor | A | CellStyle.backgroundColor | |
| IconSource / IconSize / IconRadius | A | icon (KsImage) / CellStyle.iconSize / iconRadius | |
| IsVisible / IsEnabled | A | isVisible / isEnabled | |
| Height | A | CellStyle.cellHeight | |
| Tapped (event) | 混在 | CommandCell.onTap / ButtonCell.onTap のみ対応 | 現行 LabelCell は操作 control を持たない契約 (basic-cells.md) と衝突 — CellBase 共通イベント化は要注意 |
| IsLoading / IsAnimationPlaying / UpdateIsLoading | B | なし | 現行 KsImage は同期解決のみ |
| Section (親参照) | C | — | ツリー配線用 |
| SetEnabledAppearance / Reload() | C | — | renderer 連携ハック。値+Diff 方式では不要 |

### LabelCell (phase-2 疎通対象)

| AiForms | 分類 | 現行 |
|---|---|---|
| ValueText | A | valueText |
| ValueTextColor / FontSize / Family / Attributes | A | CellStyle.valueTextColor / valueTextFont |
| IgnoreUseDescriptionAsValue | B | なし (UseDescriptionAsValue 自体が B) |

## 所見

1. Theme 相当 (本体の Cell*/Header*/Footer* 系) は A が大半で、camelCase 化のみでほぼ 1:1
2. B はドラッグソート・Android 専用切替・非同期画像・padding/align 微調整に集中
3. 通知の設計思想が根本的に違う (AiForms = INotify* 集約 / 現行 = 値 + 明示 Diff)。ItemsSource 踏襲には Diff への橋渡しが新規に必要 (→ 決定済みの変換経路に乗せる)
4. Tapped の CellBase 共通イベント化は現行 LabelCell 契約と矛盾
5. Section の HeaderView/FooterView は現行 SectionAccessory と概念一致。Root 側任意 View は AiForms に無く現行 (RootAccessory) が先行

---
type: concept
title: スタイルの MAUI 表現 (Theme / CellStyle / ListStyle)
description: native の Theme / CellStyle / style 切替が KsSettingsView.Maui でどう公開されるか — 個別プロパティへの展開・ListStyle・Section 装飾 4 属性・プロパティ一覧
tags: [maui, facade, styling, theme]
timestamp: 2026-09-04
---

# スタイルの MAUI 表現 (Theme / CellStyle / ListStyle)

この文書を読むと、native の `Theme` (画面全体の既定値) と `CellStyle` (Cell 単位の上書き)、設定 list の style 切替が `KsSettingsView.Maui` でどのプロパティとして公開され、何が facade 側で検証されず Native に委ねられるかが分かる。facade の入口は [MAUI facade の公開契約](maui-facade.md)、視覚的契約の共通部分は [設定 list の外観](../../core/styling/list-appearance.md) を先に読むと分かりやすい。決定の経緯は maui/ADR-0008 (公開面方針)・maui/ADR-0023 (ListStyle)・maui/ADR-0024 (SectionMargin の論理方向解釈)。

## 公開の形

画面全体の既定値 (native の `Theme` に対応) は SettingsView の個別プロパティとして展開して公開する。Cell 単位の上書き (native の `CellStyle` に対応) は CellBase / 各 Cell のプロパティと Cell 固有の `AccentColor` (対話型 Cell — Switch / Checkbox / SimpleCheck / Radio / Entry / Picker / NumberPicker / TimePicker / DatePicker — が持つ。画面全体の既定は `CellAccentColor`)。フォントは FontFamily / FontSize / FontAttributes に分けて公開し facade が合成する (maui/ADR-0008)。プロパティの全一覧は下の「プロパティ一覧」。

## ListStyle (設定 list の style 切替)

`SettingsView.ListStyle` (`SettingsViewStyle { Classic, Modern }`、非 nullable・既定 `Classic`) — 設定 list の style 切替 ([設定 list の外観](../../core/styling/list-appearance.md))。素直な `Style` は `VisualElement.Style` (XAML の Style 機構) と衝突するため使えない。Theme とは独立の経路で native の style プロパティへ伝わり、切替は設定内容と Section / Cell の identity を変えない (maui/ADR-0023)。

## Section 装飾 4 属性

`SectionMargin` (`Thickness?`) / `SectionCornerRadius` (`double?`) / `SectionBorderWidth` (`double?`) / `SectionBorderColor` (`Color?`) — Modern の Section Container ([設定 list の外観](../../core/styling/list-appearance.md) の「Modern の Section Container」) の外側余白・角丸・Border を Theme として伝える。null (既定) は platform 既定へ委譲し、facade は既定値定数を持たず値の検証 (負値の正規化・radius clamp・例外送出) を行わない — 正規化は Native の描画時のみで、負値・非有限 (NaN・±∞) の寸法は 0 として描画される。

`SectionMargin` の `Left` / `Right` は MAUI 標準の物理座標ではなく **leading / trailing (論理方向)** として解釈し、RTL の左右解決は Native に委ねる (facade は `FlowDirection` を監視しない)。Classic では上下成分のみ適用され左右は無視される (Classic の Section 境界は画面の全幅を占める、という全幅契約 — [設定 list の外観](../../core/styling/list-appearance.md)) (maui/ADR-0024)。

## プロパティ一覧

各プロパティの意味の正は native の `Theme` / `CellStyle` の対応項目で、視覚的契約は [設定 list の外観](../../core/styling/list-appearance.md) と [スタイル解決](../../core/styling/style-resolution.md) が持つ。facade は名前を MAUI 向けに展開して公開するだけで、独自の意味は足さない。

画面全体の既定値 (SettingsView、native の `Theme` に対応):

| 分類 | プロパティ |
|---|---|
| 色・挙動 | `SeparatorColor`、`SelectedColor`、`CellBackgroundColor`、`CellAccentColor`、`DisabledTextColor`、`CellPlaceholderColor`、`ScrollIndicatorVisible`、`RowHeight`、`HasUnevenRows` |
| Header 書式 | `HeaderTextColor`、`HeaderBackgroundColor`、`HeaderFontFamily`、`HeaderFontSize`、`HeaderFontAttributes`、`HeaderHeight` (画面全体の既定。Section ごとの固定高さは `Section.HeaderHeight` — [MAUI facade の公開契約](maui-facade.md)) |
| Footer 書式 | `FooterTextColor`、`FooterBackgroundColor`、`FooterFontFamily`、`FooterFontSize`、`FooterFontAttributes` |
| Cell タイトル既定 | `CellTitleColor`、`CellTitleFontFamily`、`CellTitleFontSize`、`CellTitleFontAttributes` |
| Cell 値テキスト既定 | `CellValueTextColor`、`CellValueTextFontFamily`、`CellValueTextFontSize`、`CellValueTextFontAttributes` |
| Cell 説明文既定 | `CellDescriptionColor`、`CellDescriptionFontFamily`、`CellDescriptionFontSize`、`CellDescriptionFontAttributes` |
| Cell ヒント既定 | `CellHintTextColor`、`CellHintFontFamily`、`CellHintFontSize`、`CellHintFontAttributes` |
| icon | `CellIconSize`、`CellIconRadius` |
| Section 装飾 | `SectionMargin`、`SectionCornerRadius`、`SectionBorderWidth`、`SectionBorderColor` (上記「Section 装飾 4 属性」) |

Cell 単位の上書き (CellBase、native の `CellStyle` に対応):

| 分類 | プロパティ |
|---|---|
| タイトル | `TitleColor`、`TitleFontFamily`、`TitleFontSize`、`TitleFontAttributes` |
| 値テキスト | `ValueTextColor`、`ValueTextFontFamily`、`ValueTextFontSize`、`ValueTextFontAttributes` |
| 説明文 | `DescriptionColor`、`DescriptionFontFamily`、`DescriptionFontSize`、`DescriptionFontAttributes` |
| ヒント | `HintTextColor`、`HintFontFamily`、`HintFontSize`、`HintFontAttributes` |
| 行・icon | `BackgroundColor`、`IconSize`、`IconRadius`、`Height` |
| Cell 固有 (対話型 Cell) | `AccentColor` |

CustomCell ではテキスト系のスタイル項目は表示に影響しない (silent no-op — [Cell の MAUI 表現](maui-cells.md) の「CustomCell」)。

## 関連

- [MAUI facade の公開契約](maui-facade.md) — facade の入口
- [Cell の MAUI 表現](maui-cells.md) — 各 Cell の公開プロパティと CustomCell の不適用プロパティ
- [設定 list の外観](../../core/styling/list-appearance.md) — Classic / Modern と Section Container の視覚的契約
- [MAUI Native Bridge の interop 境界](native-bridge.md) — Theme / CellStyle / style / Section 装飾の DTO 輸送

決定の経緯: maui/ADR-0008 (AiForms 互換公開面の方針)、maui/ADR-0023 (ListStyle の Theme 独立経路)、maui/ADR-0024 (SectionMargin の論理方向解釈)

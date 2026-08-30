---
type: design-tokens
title: Cell 共通行のレイアウト
description: Cell 種別をまたいで共有する視覚文法と platform 別の行寸法
tags: [styling, layout, cell, design-tokens]
timestamp: 2026-07-19
---

この文書は、基本 Cell と入力 Cell が共有する行構造と platform 別の高さ規則を説明する。読むと、共通要素、trailing control の責務、可変高・固定高、iOS / Android の最低行高が分かる。

## 共通の視覚文法

設定行は、任意の icon、title と任意の description、valueText または trailing control、任意の hintText から構成する。

- title と valueText は同じ主行へ置き、description は title の下へ置く。
- trailing control は行の trailing 側に置く。
- hintText は trailing control の有無に依存せず、行の右上を基準にする。
- 任意要素がない場合は対応 View を隠し、そのための空領域を残さない。

共通行は要素配置と実効 style の反映を担う。Switch、Checkbox、checkmark、chevron、picker などの生成と操作は各 Cell の Renderer / ViewHolder が trailing slot を通して担う。

## platform 別の実装

| 境界 | iOS | Android |
|---|---|---|
| 共通行 | `applyCellBaseLayout` と UIKit configuration | `CellBaseViews` と `applyCellBaseLayout` |
| root view | `UICollectionViewListCell` 系 | `MinHeightConstraintLayout` |
| 最低行高 | 48pt | 60dp |
| 標準 icon 枠 | 24pt | 24dp |

最低行高は各 platform の互換性と慣例に基づくため、数値を機械的に揃えない。

## 高さの解決

CellStyle の正の Cell 高さ、Theme の正の row height、platform の最低行高の順で解決する。最終値は platform の最低行高を下回らない。

CellStyle の高さを指定しない場合は Theme へ継承し、Theme の row height が正でなければ platform の最低行高を使う。非正値の CellStyle 高さは platform 間で同じ意味を保証しないため指定しない。

可変高さを既定とし、`Theme.hasUnevenRows == true` では解決済み高さを minimum として内容に応じて伸ばす。`false` の場合だけ、内容の自然高にかかわらず解決済み高さへ固定する。

固定高では内容が自動的に行を伸ばすことを保証しない。複数行 text や大きい trailing control を使う場合は、内容が収まる高さを利用者が指定する。

## 保証すること

- 全組み込み Cell が共通行へ共通フィールドと実効 style を渡す。
- trailing control の種類が変わっても title、description、valueText、icon、hintText の基本配置を揃える。
- 可変高さでは内容を最低行高に切り詰めない。
- 任意要素がないときに空の layout 領域を残さない。

## してはいけないこと

- Cell ごとに共通フィールドの layout と style 解決を重複実装しない。
- trailing control の生成と操作を共通行へ持ち込まない。
- iOS と Android の最低行高を同じ数値へ揃えない。
- 固定高を既定として複数行内容を切り詰めない。

## 関連

- [スタイルの所有と実効値解決](style-resolution.md)
- [Cell の視覚状態](cell-visual-states.md)
- [基本 Cell](../cells/basic-cells.md)
- [入力 Cell](../cells/input-cells.md)
- [Cell Renderer Registry](../architecture/cell-renderer-registry.md)

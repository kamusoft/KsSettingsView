---
type: design-tokens
title: Cell 共通行のレイアウト
description: Cell 種別をまたいで共有する視覚文法と platform 別の行寸法
tags: [styling, layout, cell, design-tokens]
timestamp: 2026-08-25
---

# Cell 共通行のレイアウト

この文書は、基本 Cell と入力 Cell が共有する行構造と platform 別の高さ規則を説明する。読むと、共通要素、trailing 2 系統 (Cell 級アクセサリと行内 trailing) の責務、主行の幅配分、icon 枠、可変高・固定高、iOS / Android の最低行高が分かる。

適用範囲は**共通行構造を持つ Cell** (標準の基本・入力 Cell 群および同型の利用者定義 Cell)。full-bleed 宣言 UI ホスティング型の [CustomCell](../cells/custom-cell.md) は適用除外で ([core/ADR-0015](../../../decisions/core/0015-customcell-exemption-from-shared-row-layout.md))、共通の視覚文法・主行の幅配分は適用されない (「高さの解決」の契約には CustomCell も従う)。

## 共通の視覚文法

設定行は、任意の icon、title と任意の description、trailing 側の要素 (下記 2 系統)、任意の hintText から構成する。

- title と valueText は同じ**主行**へ置き、description は title の下へ置く。主行とは、行から icon と Cell 級アクセサリを除いた領域のうち、title と行内 trailing が横に並ぶ 1 行を指す。
- trailing 側は 2 系統に区別する。
  - **Cell 級アクセサリ** — Cell 種別固有の操作・状態コントロール (SwitchCell の Switch、CheckboxCell の checkbox、RadioCell / SimpleCheckCell の checkmark、CommandCell / Picker 系の chevron)。セル全体 (title + description) に対して垂直センターへ置く。
  - **行内 trailing** — valueText と、EntryCell の入力フィールド (両 platform)。title と同じ主行内に置く。移植初期の Android は入力フィールドを accessory 領域に置いていたが、これは原典 [AiForms](../../../handbook/cross/aiforms-origin-reference.md) と乖離した配置であり、行内配置へ訂正済み ([android/ADR-0002](../../../decisions/android/0002-cell-row-width-allocation-linearlayout-weight.md))。
- description の表示幅は Cell 級アクセサリの領域と重ねない。description はアクセサリより leading 側で折り返す。
- hintText は trailing 側の要素の有無に依存せず、行の右上を基準にする。
- 任意要素がない場合は対応 View を隠し、そのための空領域を残さない。

この配置は原典 AiForms がアクセサリを `AccessoryView` / `Accessory` (ContentView の外側) としてセル縦センターに置く構造を、両 platform で踏襲したものである (iOS 側の決定の経緯は [ios/ADR-0001](../../../decisions/ios/0001-accessory-column-outside-content-stack.md))。

垂直センターは幾何配置だけでは見た目が揃わない場合があり、**光学中央への補正**を許容する。Android では chevron drawable (`ic_navigate_next`) のパスを viewport 縦中央へ補正し (原典からの意図的 deviation)、フォントメトリクス由来のテキストの沈み (約 1dp) を contentRow の `translationY` で打ち消している ([android/ADR-0004](../../../decisions/android/0004-cell-row-optical-vertical-centering.md))。iOS はこの補正を必要としていない。

共通行は要素配置と実効 style の反映を担う。Switch、Checkbox、checkmark、chevron、picker などの生成と操作は各 Cell の Renderer / ViewHolder が担い、共通行へは 2 系統の受け口 (iOS は `applyCellBaseLayout` の `accessoryView` / `trailingViews`、Android は accessory 領域と行内 slot) を通して渡す。

## 主行の幅配分

title と行内 trailing は同じ主行で行幅を分け合い、互いの表示領域に重ならない。「残り幅を誰に割り当てるか」が構造として表現されていないと、行内 trailing が固定最低幅 (旧 Android の `minWidth = 160dp` ハック) のような帳尻合わせに頼ることになる。行幅が足りないときは **title を守り、行内 trailing が譲る** ([core/ADR-0026](../../../decisions/core/0026-main-row-protects-title-truncates-value.md) — 移植元 AiForms の時点で iOS と Android の配分は逆で、それぞれ忠実に移植した結果生じていた platform 差を iOS 側へ統一した)。配分は次の 3 通りである (Android は本体行の水平 LinearLayout + weight で実現する — 実現手段は [android/ADR-0002](../../../decisions/android/0002-cell-row-width-allocation-linearlayout-weight.md)、配分の向きは core/ADR-0026 が置き換えた。iOS は `UIStackView` 階層の優先度が同じ配分を満たしている):

- **既定 (valueText 系)**: title はコンテンツ幅 (主行幅を上限とし、超過分は末尾省略)。valueText が主行の残り幅を占め、収まらない場合は末尾省略で切り詰める。
- **EntryCell**: 既定と同型。title はコンテンツ幅、入力フィールドが主行の残り幅全体を占める。title が主行幅を使い切ると入力フィールドの表示幅は 0 まで縮む — これは原典と同じ挙動として許容する。
- **行内 trailing がない場合** (valueText を持たない・空にした Cell): title が主行の全幅を使える。

title は行内 trailing の有無にかかわらず**単一行 + 末尾省略**で描画し、description は複数行折り返しを維持する (原典 AiForms の `CellTitle` と iOS の `titleLabel` がともに単一行であり、両 platform で揃えた)。

title と行内 trailing の間には最小クリアランスを確保する (Android は原典を踏襲した title の `paddingEnd` 6dp。iOS は既存実装のスタック間隔を正とする)。padding は title の View 幅に含まれるため「title 幅 + 行内 trailing 幅 = 主行幅」の等式は保たれるが、text の gravity / alignment は content box (padding を除いた領域) の中で働くため、`ButtonCell` の全幅中央揃えは厳密な中央より 3dp (= `paddingEnd` 6dp の半分) leading 側に寄る。これは原典 AiForms でも同じ構造で生じており、ずれではなく仕様である。

`ButtonCell.titleAlignment` などの title alignment は、title に配分された領域の中で働く ([基本 Cell](../cells/basic-cells.md) の契約)。行内 trailing がない行では title 領域 = 主行全幅なので alignment がそのまま視覚に出る。行内 trailing がある行では title 領域はコンテンツ幅になるため配る余白がなく、CENTER / END を指定しても視覚に出ない (icon / hintText は主行の幅配分に参加しないため、これらだけを持つ行の alignment は主行全幅の中で働く)。

## platform 別の実装

| 境界 | iOS | Android |
|---|---|---|
| 共通行 | `KsListCellBase` の自前 `UIStackView` 階層と `applyCellBaseLayout` | `CellBaseViews` と `applyCellBaseLayout` |
| root view | `UICollectionViewListCell` 系 | `MinHeightConstraintLayout` |
| 最低行高 | 48pt | 60dp |
| 標準 icon 枠 | 24pt | 24dp |

最低行高は各 platform の互換性と慣例に基づくため、数値を機械的に揃えない。

## icon 枠

icon は解決済み icon size (`CellStyle.iconSize` → `Theme.cellIconSize` → 標準 24pt / 24dp — [スタイルの所有と実効値解決](style-resolution.md)) の**正方形枠**に aspect fit で収める。枠の寸法は画像の intrinsic size に依存しない — SF Symbols のように字形ごとに幅が異なる画像を並べても icon 列の幅は揃い、title の開始位置がずれない。行幅の取り合いで icon 枠は譲らない (譲るのは主行の幅配分の規則に従う行内 trailing)。

`cellIconRadius` / `CellStyle.iconRadius` の角丸はこの正方形枠に対して適用し、aspect fit 後の画像の描画矩形には追従しない ([core/ADR-0025](../../../decisions/core/0025-cell-icon-radius-applies-to-square-frame.md))。非正方形画像では画像の実描画域が枠に満たず角丸が視覚に出ないことがあるが、これは契約どおりの挙動である。

## 高さの解決

CellStyle の正の Cell 高さ、Theme の正の row height、platform の最低行高の順で解決する。最終値は platform の最低行高を下回らない。

CellStyle の高さを指定しない場合は Theme へ継承し、Theme の row height が正でなければ platform の最低行高を使う。非正値の CellStyle 高さは platform 間で同じ意味を保証しないため指定しない。

可変高さを既定とし、`Theme.hasUnevenRows == true` では解決済み高さを minimum として内容に応じて伸ばす。`false` の場合だけ、内容の自然高にかかわらず解決済み高さへ固定する。

固定高では内容が自動的に行を伸ばすことを保証しない。複数行 text や大きい trailing 側の control を使う場合は、内容が収まる高さを利用者が指定する。

## 保証すること

- 全組み込み Cell が共通行へ共通フィールドと実効 style を渡す。
- trailing 側の要素の種類が変わっても title、description、valueText、icon、hintText の基本配置を揃える。
- title と行内 trailing は主行の幅を重なりなく分け合う。既定・EntryCell とも title はコンテンツ幅を確保し、行内 trailing (valueText / 入力フィールド) が残り幅を占め、固定の最低幅には依存しない (core/ADR-0026)。
- icon 枠は解決済み icon size の正方形で、画像の intrinsic size と行幅の取り合いに影響されない (core/ADR-0025)。
- title は単一行 + 末尾省略、description は複数行折り返し。
- Cell 級アクセサリはセル全体に対して垂直センターに置かれ、description はアクセサリの領域と重ならない幅で折り返す。
- 可変高さでは内容を最低行高に切り詰めない。
- 任意要素がないときに空の layout 領域を残さない。

## してはいけないこと

- Cell ごとに共通フィールドの layout と style 解決を重複実装しない。
- trailing 側の control (Switch・checkbox・checkmark・chevron・入力フィールドなど) の生成と操作を共通行へ持ち込まない。
- Cell 級アクセサリを行内 trailing の受け口へ渡さない (title の主行に同居させると description がセル全幅で折り返し、アクセサリの下へ回り込む)。
- 行内 trailing (EntryCell の入力フィールドを含む) を Cell 級アクセサリの受け口へ渡さない (主行の幅配分の対象外になり、固定最低幅のような帳尻合わせが必要になる)。
- iOS と Android の最低行高を同じ数値へ揃えない。
- 固定高を既定として複数行内容を切り詰めない。

## 関連

- [スタイルの所有と実効値解決](style-resolution.md)
- [Cell の視覚状態](cell-visual-states.md)
- [基本 Cell](../cells/basic-cells.md)
- [入力 Cell](../cells/input-cells.md)
- [Cell Renderer Registry](../architecture/cell-renderer-registry.md)
- [移植元 AiForms の在り処と参照ルール](../../../handbook/cross/aiforms-origin-reference.md)
- [android/ADR-0002](../../../decisions/android/0002-cell-row-width-allocation-linearlayout-weight.md) — 主行の幅配分を LinearLayout + weight で行う決定
- [android/ADR-0004](../../../decisions/android/0004-cell-row-optical-vertical-centering.md) — テキストとアクセサリを光学中央で揃える決定
- [ios/ADR-0001](../../../decisions/ios/0001-accessory-column-outside-content-stack.md) — Cell 級アクセサリを contentStack 外の列に置く決定
- [core/ADR-0025](../../../decisions/core/0025-cell-icon-radius-applies-to-square-frame.md) — icon の角丸を正方形枠に対して適用する決定
- [core/ADR-0026](../../../decisions/core/0026-main-row-protects-title-truncates-value.md) — 主行の幅配分を「title を守り valueText を省略」へ両 platform で統一する決定

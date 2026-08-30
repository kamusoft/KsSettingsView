---
type: concept
title: Cell の視覚状態
description: 通常・押下または選択・無効状態を実効 style へ重ねる描画契約
tags: [styling, cell, interaction, disabled]
timestamp: 2026-07-19
---

この文書は、Cell の通常、押下または選択、無効状態を iOS / Android でどう表すかを説明する。読むと、背景 feedback、disabled text、Native control、Cell 固有の意味色の優先関係が分かる。

## 状態の重ね方

Theme と CellStyle から解決した通常 style の上へ、現在の操作状態を重ねる。状態が解除されたら、その Cell の実効背景へ戻す。無効状態は押下・選択状態より優先する。

| 状態 | iOS | Android |
|---|---|---|
| 押下・選択 | 操作可能な Cell の highlighted / selected 背景に `Theme.selectedColor` | enabled 行の `RippleDrawable` に `Theme.selectedColor` |
| 無効 text | `Theme.disabledTextColor` | `Theme.disabledTextColor` |
| Native control | `isEnabled` を control へ反映 | `isEnabled` を Material / Android control へ反映 |

Android の現行共通行は、handler を持たない LabelCell なども enabled なら ripple 表示のために clickable flag を持つ。これは視覚 feedback の実装上の状態であり、callback や利用者向け action が存在することを意味しない。iOS は操作可能な Cell だけが選択 feedback を持つ。この違いを共通化のために隠さない。

## 無効状態

無効 Cell は操作 callback と内包 Native control の操作を抑止する。行全体の opacity / alpha を一律に下げず、text の意味色を disabled 色へ置き換え、Native control には platform の disabled 表現を使う。

無効化は各 Cell initializer の `isEnabled` で指定する。SwiftUI / Compose の `disabled` Cell modifier は現行では no-op であり、機能する無効化 API として扱わない。

## Cell 固有の意味色

ButtonCell の title 色や Switch / Checkbox の accent など、Cell 固有の意味色は CellStyle と Theme より先に解決できる。ただし無効時の text は最終的に disabled 色を優先する。

## 保証すること

- 状態解除後に Cell 固有の実効背景へ戻す。
- 無効状態で操作 callback と Native control の操作を抑止する。
- disabled 表現を text と Native control へ適用し、行全体を一律に薄くしない。
- Cell 固有の意味色を通常の style 解決より先に扱う。

## してはいけないこと

- 無効 Cell に押下・選択 feedback を残さない。
- 行全体の alpha だけで disabled 状態を表現しない。
- no-op の `disabled` modifier を有効な公開 API として案内しない。
- iOS と Android の押下対象の違いを同一契約として断定しない。

## 関連

- [スタイルの所有と実効値解決](style-resolution.md)
- [Cell 共通行のレイアウト](cell-row-layout.md)
- [基本 Cell](../cells/basic-cells.md)
- [入力 Cell](../cells/input-cells.md)

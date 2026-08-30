---
type: concept
title: 設定 list の外観と補助領域
description: Classic・Modern の視覚 mode と Section・Root Header／Footer の配置原則
tags: [styling, list, section, accessory]
timestamp: 2026-07-19
---

この文書は、iOS / Android の Classic・Modern style と Header / Footer の共通契約を説明する。読むと、style 切替が変更する範囲、Root と Section の補助領域、canvas と Cell 背景の違いが分かる。

## 視覚 mode

| style | 共通の意図 | iOS | Android |
|---|---|---|---|
| Classic | flat な罫線で行と Section を区切る | `.plain` list appearance | Cell 行の1物理 pixel の細線 |
| Modern | inset と角丸のまとまりで Section を示す | `.insetGrouped` list appearance | Section 単位の margin と角丸背景 |

style 切替は Section の装飾と区切り方だけを変更し、`SettingsRoot`、Section / Cell ID、Renderer Registry、Cell 内容を変えない。

Android の Modern は main list 内の Section Header / Footer と Cell を同じ角丸背景へまとめる。Root Header / Footer は main list の Section 装飾対象に含めない。iOS は UIKit の list appearance と Header / Footer 用の補助 layout を使う。

## Header と Footer

Section Header / Footer は `Section` の `SectionAccessory`、Root Header / Footer は Host / Bridge の `RootAccessory` から描画する。Root 補助領域を `SettingsRoot` に含めない。

Header / Footer は list 内容と共に scroll し、画面端へ固定しない。値がない場合は意味のない補助領域を生成しない。text と platform の任意 View を利用できる。

## 背景と separator

`Theme.backgroundColor` は list 全体の下地、`Theme.cellBackgroundColor` は Cell または Modern Section 背景であり、同じ領域ではない。Classic separator は `Theme.separatorColor`、Section Header / Footer は `headerTextColor` / `headerBackgroundColor` と `footerTextColor` / `footerBackgroundColor`、押下・選択背景は `selectedColor` から解決する。Modern Section のまとまりは `cellBackgroundColor` を背景に使う。Theme 変更時は style を維持したまま再評価する。

iOS Footer の既定文字色は AiForms 互換の固定 gray を維持し、system appearance に追従する description 色とは別に扱う。これは `Theme.footerTextColor` の既定値であり、利用者が明示した値では上書きできる。

## 保証すること

- Classic / Modern の切替で設定内容と identity を変更しない。
- 空の Header / Footer に表示領域を割り当てない。
- Root と Section の Accessory 所有境界を維持する。
- canvas 背景、Cell 背景、separator / Section 背景をそれぞれの表示領域へ適用する。

## してはいけないこと

- style 切替を `SettingsRootDiff` として扱わない。
- Root Header / Footer を Section の背景・角丸装飾の対象に含めない。
- Root Accessory を `SettingsRoot` へ追加しない。
- Classic / Modern の platform 実装を同じ生の margin・radius 値へ統一しない。

## 関連

- [スタイルの所有と実効値解決](style-resolution.md)
- [SettingsRoot・Section・Cell の設定ツリー](../core-model/settings-tree.md)
- [iOS Native Host](../platforms/ios-native-host.md)
- [Android Native Host](../platforms/android-native-host.md)

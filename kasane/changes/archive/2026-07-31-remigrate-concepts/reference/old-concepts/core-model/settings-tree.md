---
type: glossary
title: 設定ツリーと装飾
description: 設定画面を表すRoot・Section・Cell・Accessoryの責務と不変条件
tags: [core-model, tree, accessory]
timestamp: 2026-07-17
---

## モデル境界

Coreは設定画面の状態を表す論理モデルを提供し、描画は行わない。UIスタイル、具象Cell、状態の保持と適用はUI層が担う。

任意Viewを装飾として受け渡すため、Coreの公開境界にはプラットフォーム固有の不透明なView payloadが現れる。このためCoreの境界は「プラットフォーム型を一切含まないこと」ではなく、「描画とスタイルの責務を持たないこと」で定義する。

## 用語

- Root: Sectionの順序を保持する設定画面の最上位。ThemeとRoot Header / Footerは保持しない。
- Section: Cellの順序、Section Header / Footer、可視性をまとめる区画。空のCell集合も有効である。
- Cell: 設定値の表示、選択、編集を担う行。Coreの共通契約は薄く保ち、具象型とスタイルはUI層に属する。
- Accessory: RootまたはSectionのHeader / Footerに配置する装飾。Cellとは異なる責務を持つ。

## 不変条件

- RootとSectionは順序を持ち、空の状態も有効である。
- CellとHeader / Footerの装飾を同じ概念として扱わない。
- Root装飾とSection装飾は、将来異なる振る舞いを持てるよう区別する。
- 任意Viewの内容は意味のある値等価を定義できないため、装飾値の値比較には参加させない。
- UI層からCoreへの逆依存を作らない。


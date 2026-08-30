---
type: concept
title: Cellの視覚状態
description: 通常・選択・無効状態を実効スタイルへ重ねる共通描画契約
tags: [styling, cell, interaction, disabled]
timestamp: 2026-07-18
---

## 状態の優先順位

Cellの通常、押下または選択、無効という視覚状態は、解決済みの[実効スタイル](styling/style-resolution.md)の上に重ねる。

操作可能なCellだけが押下・選択中のフィードバックを示す。状態が解除されたら、そのCellの実効背景へ戻す。無効状態は選択状態より優先する。

## 無効状態

無効状態ではCellの操作と内包するNative controlの操作を抑止する。行全体のopacityを一律に下げず、テキストの意味色を無効時の色へ置き換え、Native controlには各プラットフォームのdisabled表現を使う。

これにより背景、画像、レイアウトまで不必要に薄めず、操作不能であることを一貫して示す。

## 固有の意味色

Cell種別が固有の意味色を持つ場合、その値はCellStyleやThemeより先に解決できる。ただし無効状態では、最終的に無効時の意味色を優先する。


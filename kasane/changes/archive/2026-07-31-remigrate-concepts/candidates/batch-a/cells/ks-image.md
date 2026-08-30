---
type: reference
title: KsImage
description: Cell の icon を表す UI 層の公開型とプラットフォーム別 fallback 契約
tags: [cells, image, public-api]
timestamp: 2026-07-18
---

この文書は、Cell の `icon` に渡す `KsImage` の case、同一性、解決不能時の挙動を説明する。読むと、iOS / Android で安全に icon を指定する方法と、両者を同一視できない境界が分かる。

## 目的と責務境界

`KsImage` は Cell の icon を UI 層へ渡す判別可能な値である。iOS では `KsSettingsViewUI`、Android では `ks-settingsview-ui` に属し、Core の `KsCell` / `Cell` 抽象は画像を要求しない。

## 公開 API

- iOS: `.systemName(String)` と `.uiImage(UIImage)`。前者は SF Symbols、後者は渡された `UIImage` を描画する。
- Android: `Resource(@DrawableRes Int)`、`Drawable(android.graphics.drawable.Drawable)`、`SystemName(String)`。`Resource` と `SystemName` は値同一性、`Drawable` は参照同一性で比較する。

## 保証すること

- `icon = nil` / `null` または現在のプラットフォームで解決不能な icon は、安全に「icon なし」へ fallback する。
- icon がない場合は空の画像領域を残さず、title を通常の開始位置へ配置する。
- Android の `SystemName` は API 対称性のため受理するが画像へ解決せず、throw せずに非表示へ fallback する。

## してはいけないこと

- Native 画像オブジェクトに内容等価を仮定し、pixel data の比較を Cell の値比較へ持ち込んではならない。
- `KsImage` を Core の Cell 抽象へ移してはならない。
- `SystemName` が Android でも SF Symbols を描画すると仮定してはならない。

## 利用例

```swift
LabelCell(title: "ストレージ", icon: .systemName("externaldrive"))
LabelCell(title: "画像", icon: .uiImage(customImage))
```

```kotlin
LabelCell(title = "ストレージ", icon = KsImage.Resource(R.drawable.ic_storage))
LabelCell(title = "画像", icon = KsImage.Drawable(customDrawable))
```

## 関連

- [基本 Cell](cells/basic-cells.md)
- [入力 Cell](cells/input-cells.md)


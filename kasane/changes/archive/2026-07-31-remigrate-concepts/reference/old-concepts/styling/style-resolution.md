---
type: concept
title: スタイルの所有と実効値解決
description: UI層がThemeとCellStyleを所有し、段階的に描画値へ解決する共通規則
tags: [styling, theme, cell-style, native-types]
timestamp: 2026-07-18
---

## 所有境界

Theme、CellStyle、色、文字、寸法、画像などのスタイル値はUI層が所有する。Coreは設定内容と構造を表し、見た目の値や実効スタイルを持たない。

iOSとAndroidは、それぞれのNative型を直接使う。共通化のための論理色・論理フォントを間に置かず、プラットフォームの慣例と機能をそのまま利用者へ公開する。

## 解決階層

描画に使う実効値は、次の順で最初に指定された値を採用する。

1. Cell種別が持つ意味上の固有値
2. 単一CellのCellStyle
3. 画面全体のTheme
4. platform default

CellStyleの未指定値は空値ではなく、上位の値を継承する意思を表す。どの段にも指定がない属性はplatform defaultへ解決し、描画側へ未解決値を渡さない。

画面のcanvas背景とCell背景は別の表示領域であり、一方の指定から他方を推論しない。

## platform themeとの境界

platform defaultは、ホストアプリが提供するNative themeの属性を含む。UI層が要求する属性をホスト側で解決できることが、Native controlを正しく構築する前提となる。

Android UIはMaterial3のtheme属性を消費するため、組み込むホストアプリはMaterial3派生themeを提供する。この要件はSample固有ではなく、Android UIを利用するすべてのホストに適用する。

## 更新境界

Themeは構造モデルではなく独立した表示状態である。同値のThemeは再適用せず、変更時はSectionやCellの同一性を維持したまま現在の表示を再評価する。

状態の保持と通知は[Storeの状態と更新通知](architecture/store-and-update-streams.md)、構造変更との分離は[表示状態同期](architecture/display-state-synchronization.md)の契約に従う。

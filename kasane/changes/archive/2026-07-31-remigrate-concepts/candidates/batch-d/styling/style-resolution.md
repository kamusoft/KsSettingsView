---
type: concept
title: スタイルの所有と実効値解決
description: UI 層が Theme と CellStyle を所有し、platform の描画値へ段階的に解決する共通規則
tags: [styling, theme, cell-style, native-types]
timestamp: 2026-07-19
---

この文書は、iOS / Android の Theme と CellStyle の所有境界と解決順を説明する。読むと、Core に style を置かない理由、Cell 固有値・CellStyle・Theme・platform default の優先順位、Theme 更新の境界が分かる。

この文書で Core の Cell 抽象は ID を通して設定ツリーへ参加する最小契約、Cell model はそれに準拠する UI 層の具象値、Native cell は実際に描画する再利用行を指す。Core 抽象は style 型を要求しないが、UI 層の具象 Cell model は `CellStyle` を持てる。3層の関係は [Native Host の責務境界](../architecture/native-host-boundary.md#cell-の3層) を参照する。

## 所有境界

Theme、CellStyle、KsImage、色、font、寸法は UI 層が所有する。Core は `SettingsRoot`、`Section`、`Cell` の構造と identity を表し、見た目の型や実効 style を要求しない。

| 値 | iOS | Android |
|---|---|---|
| 色 | `UIColor` | Jetpack Compose `Color` |
| font | `UIFont` | Jetpack Compose `TextStyle` |
| 寸法 | `CGFloat` | `Dp` |
| 実効値 | `EffectiveStyle` が UIKit 値を解決 | `EffectiveStyle` が Android View 用 ARGB / Typeface / sp へ変換 |

共通化のための `KsColor` / `KsFont` のような中間表現を置かず、各 platform の型と慣例を利用者へ直接公開する。

## 解決順

通常の描画属性は、次の順で最初に指定された値を使う。

1. Cell 種別が持つ意味上の固有値
2. 単一 Cell の `CellStyle`
3. 画面全体の `Theme`
4. platform default

`CellStyle` の未指定値は、Theme から継承する意思を表す。どの段にも値がなければ platform default へ解決し、描画時に未解決値を残さない。

ButtonCell の title 色や Switch / Checkbox の accent など、Cell 固有の意味値は CellStyle より先に解決する。無効状態の text 色は、通常の実効値へ視覚状態を重ねる段階で優先する。

## 表示領域

Theme の canvas（list 全体の下地）背景と Cell 背景は別の領域である。`Theme.backgroundColor` は list 全体、`Theme.cellBackgroundColor` は Cell の既定背景、`CellStyle.backgroundColor` は単一 Cell の背景を表す。一方の値から他方を推論しない。

Header / Footer、separator、選択色なども Theme が持つが、各 platform の list 構造に合わせて適用する。

## platform theme の前提

platform default はホストアプリの Native theme 属性を含む。Android Host は Material widget と color attribute を使うため、XML Theme を `Theme.Material3.*` 派生にする。Compose の `MaterialTheme` だけでは `AndroidView` の Context Theme を代替できない。

## Theme 更新

Theme は `SettingsRoot` の構造ではなく独立した表示状態である。同値 Theme は再適用せず、変更時は Section / Cell ID を維持したまま表示中の Cell、補助領域、背景、装飾を再評価する。

## 保証すること

- Core の Cell 抽象へ style 型を要求しない。
- Cell 固有値、CellStyle、Theme、platform default の順で解決する。
- canvas 背景と Cell 背景を別の表示領域として扱う。
- Theme 更新で設定ツリーの identity と構造を変えない。

## してはいけないこと

- 共通化のためだけに論理色・論理 font の中間型を追加しない。
- CellStyle の未指定値を「透明」「0」などの実値として扱わない。
- Theme 更新を `SettingsRootDiff` へ混ぜない。
- Compose `MaterialTheme` だけで Android Native Host の Material3 前提を満たすと仮定しない。

## 関連

- [Cell 共通行のレイアウト](cell-row-layout.md)
- [Cell の視覚状態](cell-visual-states.md)
- [設定 list の外観と補助領域](list-appearance.md)
- [Store の状態と更新通知](../architecture/store-and-update-streams.md)
- [iOS Native Host](../platforms/ios-native-host.md)
- [Android Native Host](../platforms/android-native-host.md)

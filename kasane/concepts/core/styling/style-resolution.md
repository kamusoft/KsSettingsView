---
type: concept
title: スタイルの所有と実効値解決
description: UI 層が Theme と CellStyle を所有し、platform の描画値へ段階的に解決する共通規則。ライブラリ既定色を中立に保ち AiForms 互換色は利用側が設定する方針を含む
tags: [styling, theme, cell-style, native-types]
timestamp: 2026-08-29
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

## 特殊な解決規則

通常の4段階だけでは表せない公開値は、次の順で解決する。

- 正の `Theme.cellTitleFontSize` は、`CellStyle.titleFont` を含めて選ばれた最終 title font の size を上書きする。
- valueText の色と font は `CellStyle` → Theme の valueText 既定 → Theme の title 既定 → platform default の順で解決する。EntryCell の入力中テキストの色もこの valueText の解決順に従う (EntryCell は `valueText` を持たないが、入力欄が表示する値の色は valueText 系の契約に属する。iOS / Android 共通)。
- hintText の色は `CellStyle.hintTextColor` → `Theme.cellHintTextColor` → `Theme.cellAccentColor` の順で解決する。
- Header / Footer の font は Theme の `headerFont` / `footerFont` を基礎にし、対応する正の `headerFontSize` / `footerFontSize` が最終 size を上書きする。
- icon size / radius は `CellStyle` → Theme → 既定 (24pt / 24dp と radius 0) の順で解決する。icon size は正の有限値のみ、radius は 0 以上の有限値のみを有効とし、無効値 (負・NaN・±∞) は未指定として次の段へ進める (正値を要求する `rowHeight` / `cellTitleFontSize` と同じパターン)。解決済み値は両 platform とも icon の正方形枠へ反映する ([Cell 共通行のレイアウト](cell-row-layout.md) の icon 枠、core/ADR-0025)。
- Section 装飾4属性 (`sectionMargin` / `sectionCornerRadius` / `sectionBorderWidth` / `sectionBorderColor`) は Section 単位の属性のため `CellStyle` 段を持たず、Theme → **platform default** (margin は style 間・platform 間とも同値の既定寸法 — core/ADR-0027。Classic は水平成分を無視) の2段で解決する。Theme へフラットに直置きし、集約用の中間型 (`SectionStyle` 等) を作らない。装飾の意味論は [設定 list の外観と補助領域](list-appearance.md) を参照する。

## 表示領域

Theme の canvas（list 全体の下地）背景と Cell 背景は別の領域である。`Theme.backgroundColor` は list 全体、`Theme.cellBackgroundColor` は Cell の既定背景、`CellStyle.backgroundColor` は単一 Cell の背景を表す。一方の値から他方を推論しない。

Header / Footer、separator、選択色なども Theme が持つが、各 platform の list 構造に合わせて適用する。

## platform theme の前提

platform default の最終段は各 platform の標準的な既定値へ解決する。iOS はシステム色 (`UIColor.label` 等)。Android はライブラリが同梱する Material3 派生テーマ (DayNight) から解決し、ホストアプリの XML テーマには依存しない — ライブラリ所有の UI (標準 Cell の行・chrome・選択面) は常に同梱テーマでラップした Context で生成され、ホストのテーマ属性 (カスタム色・dynamic color を含む) は反映されない ([android/ADR-0020](../../../decisions/android/0020-bundled-theme-always-wrap-host-independent.md))。ホストの XML テーマ・Activity 型に前提はなく、最小テーマ + `ComponentActivity` の構成でも全 Cell が動作する。見た目の調整はライブラリの Theme / CellStyle で行う。

Android のライト / ダークは、同梱テーマが DayNight 派生であるため端末の夜間モードとアプリの uiMode 制御 (`AppCompatDelegate.setDefaultNightMode` / `UiModeManager.setApplicationNightMode`) で決まる。ホストが XML テーマで Dark 系を明示するだけの指定は反映されない。

利用者所有コンテンツ (CustomCell の content・`KsAnyView` 経由の利用者 View) は隔離の対象外で、従来どおりホストの Context (ホストテーマ) で解決される。

## Sample の AiForms 互換色

ライブラリの Theme 既定値は、Sample が比較用に持つ AiForms 互換色へ変更しない。AiForms の見た目を再現する application は、利用側の `Theme` として互換色を明示する。Sample はその値を各 platform の `SampleTheme` に置くが、比較用の色値は製品契約ではなく実ソースが正である ([Sample のプラットフォーム間一致](../../cross/conventions/sample-parity.md))。

この境界により、ライブラリ既定値は platform default を含む通常の解決順を維持し、特定の移植元 application の配色をすべての利用者へ暗黙適用しない。

## Theme 更新

Theme は `SettingsRoot` の構造ではなく独立した表示状態である。同値 Theme は再適用せず、変更時は Section / Cell ID を維持したまま、各 platform が対応する表示属性を再評価する。表示中の Cell と canvas に加え、text 形式の Root / Section Header / Footer も両 platform とも表示中のまま色・フォントを再適用する。View 形式の accessory は Theme 通知の再適用対象にしない — 再 bind すると hosted view の内部状態が失われるためで、適用範囲の三分割は [表示状態同期](../architecture/display-state-synchronization.md) を参照する。Android はさらに行の背景と Section 装飾を再評価する。

## 保証すること

- Core の Cell 抽象へ style 型を要求しない。
- Cell 固有値、CellStyle、Theme、platform default の順で解決する。
- size 専用フィールドと valueText / hintText の fallback を通常の4段階と区別する。
- canvas 背景と Cell 背景を別の表示領域として扱う。
- Theme 更新で設定ツリーの identity と構造を変えない。

## してはいけないこと

- 共通化のためだけに論理色・論理 font の中間型を追加しない。
- CellStyle の未指定値を「透明」「0」などの実値として扱わない。
- Theme 更新を `SettingsRootDiff` へ混ぜない。
- ホストの XML テーマや Compose `MaterialTheme` でライブラリ UI の配色を調整できると想定しない — ライブラリ UI はホストテーマから視覚隔離されている ([android/ADR-0020](../../../decisions/android/0020-bundled-theme-always-wrap-host-independent.md))。

## 関連

- [Cell 共通行のレイアウト](cell-row-layout.md)
- [Cell の視覚状態](cell-visual-states.md)
- [設定 list の外観と補助領域](list-appearance.md)
- [Store の状態と更新通知](../architecture/store-and-update-streams.md)
- [iOS Native Host](../../ios/api/ios-native-host.md)
- [Android Native Host](../../android/api/android-native-host.md)

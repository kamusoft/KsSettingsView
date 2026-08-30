---
type: concept
title: PickerCell の選択面
description: PickerCell の行タップで開く選択面のプラットフォーム共通契約 (確定・破棄・上限・スタイル継承・初期スクロール) と意図的なプラットフォーム差
tags: [cells, picker, selection-surface, styling]
timestamp: 2026-08-28
---

# PickerCell の選択面

この文書は、`PickerCell` の行タップで開く候補選択 UI (以下「選択面」) が iOS / Android で共通に守る挙動契約と、意図的に揃えないプラットフォーム差を説明する。読むと、確定と破棄の意味論、選択印とスタイルの解決規則、初期スクロール、範囲外 index の扱い、そしてどの差が OS 慣習優先の合意済み判断なのかが分かる。`PickerCell` のモデル (状態フィールド・callback) は [入力 Cell](input-cells.md)、スタイル解決の一般規則は [スタイルの所有と実効値解決](../styling/style-resolution.md) を先に読むと分かりやすい。

## 目的

選択面は「下から / 手前に出る選択 UI」の体験を両プラットフォームで揃えつつ、器そのもの (提示コンテナ) は各 OS の慣習に合わせるための境界である。挙動契約 (callback のタイミング・上限・拒否の触覚フィードバック) はプラットフォーム間で同一に保ち、器と操作語彙 (シート形状・ボタンラベル・ジェスチャー) はプラットフォーム差として明示的に管理する。

## 提示の器

| | Android | iOS |
|---|---|---|
| 器 | ボトムシート (`PickerSelectionSheet`、Material `BottomSheetDialog`。[android/ADR-0005](../../../decisions/android/0005-pickercell-selection-ui-bottom-sheet.md)) | ページシート (`PickerListViewController`、`UINavigationController` + `UITableView`。移植当初からの構造で、個別の決定記録はない) |
| ヘッダー | ドラッグハンドル + 「キャンセル (テキストボタン) / タイトル / OK (強調色で塗ったボタン、複数選択時のみ)」 | Cancel / タイトル / 完了 (複数選択時のみ) のナビゲーションバー |
| 高さ | コンテンツ高。画面約半分を上限に内部スクロールし、全展開 (高さ上限の解除) はハンドル・ヘッダー起点のドラッグのみ (候補リストのスクロールでは展開しない) | ページシートの標準高 |
| 非確定の閉じ方 | キャンセル・外側タップ・Back・ハンドル / ヘッダー起点の下スワイプ (候補リスト面からの下スワイプでは閉じない) | Cancel とページシート標準の dismiss 操作 |

## 共通の挙動契約

- 提示: `isEnabled` な PickerCell の行タップで開く。`isEnabled = false` はタップ無効。`items` が空でも候補0件の選択面を提示する (行タップを無反応にしない)
- タイトル: `pageTitle ?: title` で解決する
- 候補: `items` (`PickerItem` 列 — [入力 Cell](input-cells.md)) の全項目を順序どおり列挙し、主表示は `text`。`subText` を持つ行は主表示の下に副表示を持つ**2行構成**で描画する (全項目が subText なしの選択面は1行構成のまま)。空文字列の subText は縁で「なし」へ正規化済みのため、選択面は非 nil / null 判定だけで行構成を決める。行高・Android の折り畳み高さ計算・初期スクロールは2行行高 (subText 混在は行ごとの可変行高) に追随する
- 単一選択: `selectedIndex` の項目に選択印。候補タップで `onSelectionChanged(index)` を1回発火して閉じる (作業状態は持たない)
- 複数選択: 開いた時点の `selectedIndices` をコピーして**作業状態** (確定まで選択面内に閉じた一時的な選択集合) を作る。候補タップは作業状態のトグルのみで callback を発火せず、確定操作で `onMultiSelectionChanged(作業状態の集合)` を1回発火して閉じる
- 非確定 dismiss: 上表「非確定の閉じ方」のどの経路でも callback を発火せず作業状態を破棄する。次に開いたときはその時点のモデル値から作り直す
- 上限 (複数選択のみ): `maxSelectedNumber > 0` のとき、上限到達後の新規チェックは無視し、拒否を示す触覚フィードバックをシステムへ要求する。チェック済み項目の解除は上限到達後も常に可能。`maxSelectedNumber = 0` は上限なし ([入力 Cell](input-cells.md))
- モデル値を正規化しない: 範囲外の `selectedIndex` には選択印を表示せず、`selectedIndices` の範囲外 index は作業状態・確定 callback・上限判定の件数に保持される。帰結として「画面上のチェック数より上限判定の件数が多い」状態が起こり得る (見えないチェックで上限に達する) — これはバグではなく契約である
- 初期スクロール: 選択中の項目 (複数選択は選択中の最小の有効 index) が見える状態で開く。位置の精度はプラットフォーム差 — iOS は可視領域の中央付近 (端部はクランプ許容)、Android は見える位置 (位置までは規定しない)。選択なし・範囲外のみの場合は先頭から表示する
- アクセシビリティ: 各候補行は表示名 (副表示があればそれも含む) と選択状態をアクセシビリティ機構 (TalkBack / VoiceOver 等) へ公開し、トグル後は公開状態も更新する

## スタイル継承

選択面の**内容** (候補行・面の背景・ヘッダーの文字) は呼び出し元 Cell の実効 style を継承し、システム既定色のままにしない。器の**外殻** (ドラッグハンドル、iOS のナビゲーションバー背景など) は OS 既定の構成を引き継ぐ。

| 対象 | 解決値 |
|---|---|
| 候補行のタイトル文字色・フォント | 実効タイトル値 (CellStyle → Theme。`Theme.cellTitleFontSize` による最終サイズ上書きは [スタイルの所有と実効値解決](../styling/style-resolution.md) の「特殊な解決規則」) |
| 候補行の副表示 (subText) の文字色・フォント | description 系統の実効値 (`CellStyle.descriptionColor / descriptionFont` → `Theme.cellDescriptionColor / cellDescriptionFont`) — 「主文の補足」の意味論を副表示に継承する |
| 選択面・候補行の背景 | 実効セル背景色 (CellStyle → Theme) |
| 候補行の区切り線 | `Theme.separatorColor` |
| タップ時のハイライト | `Theme.selectedColor` |
| 選択印 (チェックマーク) | `PickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor` の3段解決 |

選択印は Checkbox / Radio の形をどちらのプラットフォームでも使わず、「accent 色の単純なチェックマーク」という見え方の意図を揃える。実現手段はプラットフォームごとに異なる (Android は既存の `KsSimpleCheckView` を再利用した Canvas 描画、iOS は UIKit 標準の `.checkmark` accessory に tint を適用)。

## 意図的なプラットフォーム差

体験の同質性より OS 慣習を優先した、合意済みの設計判断による差で、解消対象ではない:

- **操作ラベル**: Android は OS の公開文字列リソース (`android.R.string.ok` / `android.R.string.cancel`) を使い OS ローカライズに追従する (自前文字列を同梱しない)。iOS の「完了」と文言は揃わない
- **ヘッダーの文字 (Android)**: キャンセルの文字は解決済み強調色。確定 (OK) ボタンは強調色で塗られるため、その上に載せる文字色は `Theme.backgroundColor` で描画する (輝度から白/黒を導出しない)。文字サイズは候補行の実効タイトルサイズからタイトル = +1sp、キャンセル / OK = −1sp で導出し、Theme のフォントサイズ指定に追従する
- **ナビゲーションバー (iOS)**: Cancel / 完了ボタンの色は選択印と同一の解決済み強調色、タイトルの文字色は実効タイトル色。フォントサイズはシステム既定を維持する (Android のようなサイズ導出は持ち込まない)。バー背景は現在有効な appearance を引き継ぎ、タイトル文字色のみ差し替える — ページシート既定では背景が透過し、候補リストの実効セル背景色が透ける
- **記号ボタン描画 (iOS、観測結果)**: iOS 26 時点の描画では `.cancel` / `.done` システムアイテムが記号ボタン (✕ / 塗り丸 + ✓) としてレンダリングされる。契約ではなく OS の描画観測であり、解決済み強調色はこの記号にも適用される

## 保証すること

- 確定 callback は確定操作 (単一選択は候補タップ、複数選択は確定ボタン) の1回だけ発火する。非確定 dismiss はどの経路でも発火しない — これが崩れると、利用者アプリの状態が「開いて閉じただけ」で書き換わる
- 挙動契約 (callback タイミング・上限・拒否の触覚フィードバック・正規化しないこと) はプラットフォーム間で同一。器の違いは挙動契約に影響しない
- 選択集合を選択面が正規化しない — `items` 更新途中の一時的な不整合で利用者データ (範囲外 index) を黙って失わないため
- スタイル継承の解決は行タップから選択面を組み立てる提示経路の中で1回だけ行い、選択面が Theme を独自に再参照しない。ナビゲーションバー / ヘッダーの強調色は選択印と同じ解決済み値を共有する

## してはいけないこと

- 選択面の描画でプラットフォーム既定の構成 (Android の `MaterialShapeDrawable`、iOS の navigation bar appearance) を新規生成で丸ごと差し替えない — 既定が担う角丸・elevation・遷移補間・ホストアプリのカスタマイズが失われる。現在有効な構成を派生させて必要な属性だけ変更する
- 「意図的なプラットフォーム差」に挙げた差を「プラットフォーム間の揃え漏れ」として片側へ持ち込まない — 差自体が合意済みの契約である
- 選択面の中で選択集合の正規化・重複除去を追加しない

## 用語

- **選択面**: PickerCell の行タップで開くモーダルな候補選択 UI。仕様記述・実装コメントで共通に使う語
- **作業状態**: 複数選択の選択面が確定まで内部に保持する一時的な選択集合。確定操作でのみモデルへ反映され、非確定 dismiss で破棄される

## 関連

- [入力 Cell](input-cells.md) — `PickerCell` のモデルと callback の公開契約
- [スタイルの所有と実効値解決](../styling/style-resolution.md) — 実効値解決の一般規則と `cellTitleFontSize` の上書き規則
- [設定 list の外観と補助領域](../styling/list-appearance.md) — separator・選択色の list 側の扱い
- [android/ADR-0005](../../../decisions/android/0005-pickercell-selection-ui-bottom-sheet.md) — Android の器をボトムシートにした決定
- [core/ADR-0029](../../../decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md) — 候補を PickerItem 列にし副表示を選択面へ追加した決定

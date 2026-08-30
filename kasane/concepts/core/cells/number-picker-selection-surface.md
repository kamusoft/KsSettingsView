---
type: concept
title: NumberPickerCell の選択面
description: NumberPickerCell の行タップで開く数値選択 UI のプラットフォーム共通契約 (候補生成・unit 適用・確定と破棄・初期選択) と意図的なプラットフォーム差
tags: [cells, number-picker, selection-surface, styling]
timestamp: 2026-08-02
---

# NumberPickerCell の選択面

この文書は、`NumberPickerCell` の行タップで開く数値選択 UI (以下「選択面」) が iOS / Android で共通に守る挙動契約と、意図的に揃えないプラットフォーム差を説明する。想定読者はライブラリの実装者と、挙動契約を知りたい利用者の両方である。読むと、候補の生成規則と `unit` の適用先、確定と破棄の意味論、初期選択、そして器 (提示コンテナ) がプラットフォームごとにどう違うのかが分かる。`NumberPickerCell` のモデル — `min` / `max` / `step` / `value` / `unit`、行の表示値を明示上書きする `valueText`、選択面のタイトル `pickerTitle`、確定通知の `onValueChanged`、強調色 `accentColor` — は [入力 Cell](input-cells.md)、スタイル解決の一般規則は [スタイルの所有と実効値解決](../styling/style-resolution.md) を先に読むと分かりやすい。`PickerCell` (候補リストからの選択) の選択面は別契約 — [PickerCell の選択面](picker-selection-surface.md) を参照。

## 目的

選択面は「候補を回して1つ選び、確定して初めて反映される」数値選択の体験を両プラットフォームで揃えつつ、器そのものは各 OS の慣習に合わせるための境界である。挙動契約 (候補生成・unit 適用・callback のタイミング) はプラットフォーム間で同一に保ち、器と操作語彙 (ボトムシート / 埋め込み picker、ボタンラベル、ジェスチャー) はプラットフォーム差として明示的に管理する。

## 提示の器

| | Android | iOS |
|---|---|---|
| 器 | ボトムシート (`NumberSelectionSheet`、Material `BottomSheetDialog`。[android/ADR-0007](../../../decisions/android/0007-numberpickercell-bottom-sheet-custom-wheel.md)) + スナップ式ホイール (`KsWheelView`) | 埋め込み `UIPickerView` を `inputView` 経由でキーボード位置にスライドアップ表示 (`NumberPickerCellView`。移植元 AiForms の SettingsView と同じ埋め込み方式) |
| ヘッダー | ドラッグハンドル + 「キャンセル (テキストボタン) / タイトル / OK (強調色で塗ったボタン)」(ラベル文言は OS リソース — 後述)。意匠は `PickerSelectionSheet` ([android/ADR-0005](../../../decisions/android/0005-pickercell-selection-ui-bottom-sheet.md)) と共有 | 入力ツールバー (Cancel / タイトル / Done) |
| 非確定の閉じ方 | キャンセル・外側タップ・Back・ハンドル / ヘッダー起点の下スワイプ (候補領域 = ホイール面からの下スワイプでは閉じず候補が遷移する) | Cancel とキーボード標準の dismiss 操作 |

Android のホイール (`KsWheelView`) は RecyclerView + LinearSnapHelper による自作部品で、**internal に留める** (公開 API ではない)。将来の DatePicker ホイール版展開の内部土台であり、部品の詳細は android/ADR-0007 とコード・テストを正とする。

## 共通の挙動契約

- 提示: `isEnabled` な NumberPickerCell の行タップで開く。`isEnabled = false` はタップ無効
- タイトル: `pickerTitle` があればそれ、なければ `title` で解決する
- 候補: `min` から `max` まで `step` 刻みで昇順に列挙する。`step <= 0` は 1 へ fallback
- unit の適用: 候補の表示は `valueText` の有無にかかわらず、常に各候補値へ `NumberPickerCell.format` の規則 (`unit` が空なら数値のみ、非空なら `"<値> <unit>"`) を個別に適用する。`valueText` の優先は Cell 行の表示のみ ([入力 Cell](input-cells.md))
- 初期選択: 開いた時点で `value` に一致する候補が選択中。`value` が候補に含まれない場合は先頭候補
- 確定のみ反映: 確定操作 (Android の OK / iOS の Done) で、その時点の選択中候補を引数に `onValueChanged` を1回発火して閉じる。非確定の閉じ方はどの経路でも発火せず、変更は破棄される — 選択面は確定まで作業状態を model へ書き戻さない

## 意図的なプラットフォーム差

体験の同質性より OS 慣習・部品特性を優先した差で、解消対象ではない:

- **操作ラベル**: Android は OS の公開文字列リソース (`android.R.string.ok` / `android.R.string.cancel`) を使い OS ローカライズに追従する (自前文字列を同梱しない)。iOS の Cancel / Done とは文言が揃わない
- **スナップ静止の意味論 (Android)**: 論理上の選択中候補の更新は、ホイールが候補位置に静止 (スナップ) した時点でのみ行う。ドラッグ・慣性移動・SnapHelper の補正スクロール中は直前の選択中候補を維持し、移動中に確定した場合は直前にスナップ静止した候補を採用する (視覚上の中央行が移動中に流れても、確定が採用するのは論理上の選択中)。iOS は `UIPickerView` 標準の選択遷移に従う
- **アクセシビリティ (Android)**: ホイールはスピナー相当のコントロールとして、選択中候補の表示文字列 (unit 適用後) を公開し、前候補・次候補へのアクセシビリティアクションで選択中を変更できる (端の候補ではその方向のアクションを提供しない)。iOS は `UIPickerView` 標準の公開に従う

## 契約外の防御挙動

不正な指定への防御であり、意図的な差というより実装の現状である。呼び出し側が `min <= max` を守る前提 ([入力 Cell](input-cells.md) の「保証すること」) の下では、公開契約として依存しない:

- `min > max`: Android は選択面を提示せず警告ログを残す。iOS は移植当初からの挙動として候補1件 (`min`) で提示する (個別の決定記録はない)
- 候補件数が `Int` 上限 (2^31 − 1) を超える指定: Android は提示せず警告ログを残す (候補件数は 64bit で算出し、候補列は実体化しない)。iOS に対応する上限判定はない

## スタイル継承

選択面の内容は呼び出し元 Cell の実効 style を継承し、システム既定色のままにしない:

- 選択中候補の強調 (Android のホイール中央行) と iOS ツールバーの操作ボタンは、`NumberPickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor` の3段解決 ([スタイルの所有と実効値解決](../styling/style-resolution.md))
- Android のシート面・区切り線は `Theme.cellBackgroundColor` / `Theme.separatorColor`。ヘッダーの文字色・文字サイズの解決規則は `PickerSelectionSheet` と共有する ([PickerCell の選択面](picker-selection-surface.md))

## 保証すること

- 確定 callback は確定操作の1回だけ発火し、非確定 dismiss はどの経路でも発火しない — これが崩れると、利用者アプリの状態が「開いて閉じただけ」で書き換わる
- 論理上の選択中候補は常に1つであり、静止状態では中央の強調行として他の候補と判別できる形で提示される (移動中の視覚とのずれは「スナップ静止の意味論」を参照)
- 候補の表示文字列は Cell 行と同じフォーマット規則 (`NumberPickerCell.format`) から生成される — `valueText` 未指定の行に「15 px」と出る Cell の選択面が素の「15」を出すことはない

## してはいけないこと

- `KsWheelView` を公開 API として利用者に案内しない — internal の内部部品であり、公開契約は Cell model と選択面の挙動だけである
- 器の差 (ボトムシート / 埋め込み picker) を「プラットフォーム間の揃え漏れ」として片側へ持ち込まない — 差自体が合意済みの判断である (android/ADR-0007)
- `valueText` を候補表示へ適用しない — `valueText` は Cell 行の表示専用である

## 用語

- **選択面**: 入力 Cell の行タップで開くモーダルな選択 UI。[PickerCell の選択面](picker-selection-surface.md) と共通の語
- **器**: 選択面を提示するコンテナ (Android のボトムシート / iOS の埋め込み picker)。挙動契約と切り離してプラットフォーム差を管理する単位
- **スナップ静止**: Android のホイールで、候補がちょうど選択位置に整列して静止した状態。選択中候補の更新はこの時点でのみ起こる

## 関連

- [入力 Cell](input-cells.md) — `NumberPickerCell` のモデル・`unit` と表示値の生成規則
- [PickerCell の選択面](picker-selection-surface.md) — 候補リスト選択の選択面契約 (本書と器・ヘッダー意匠を一部共有)
- [スタイルの所有と実効値解決](../styling/style-resolution.md) — 実効値解決の一般規則
- [android/ADR-0007](../../../decisions/android/0007-numberpickercell-bottom-sheet-custom-wheel.md) — Android の器をボトムシート + 自作ホイールにした決定

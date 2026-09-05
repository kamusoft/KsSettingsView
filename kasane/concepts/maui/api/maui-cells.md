---
type: concept
title: Cell の MAUI 表現 (KsSettingsView.Maui)
description: core の Cell 意味論を MAUI の型でどう表すか — 公開プロパティの型 (MAUI 慣例型)・PickerCell の候補と選択・ユーザー操作の書き戻し (TwoWay)・CustomCell
tags: [maui, facade, cells, binding]
timestamp: 2026-09-04
---

# Cell の MAUI 表現 (KsSettingsView.Maui)

この文書を読むと、`KsSettingsView.Maui` の各 Cell が公開するプロパティの型、ユーザー操作がどのプロパティへ書き戻されて ViewModel へ届くか、PickerCell の候補と選択の契約、任意の View を内容にする CustomCell の契約が分かる。Cell 階層の全体と facade の入口は [MAUI facade の公開契約](maui-facade.md)、Cell 意味論の共通契約は [入力 Cell](../../core/cells/input-cells.md) / [基本 Cell](../../core/cells/basic-cells.md) / [CustomCell](../../core/cells/custom-cell.md) を先に読むと分かりやすい。決定の経緯は maui/ADR-0004 (MAUI 慣例型)・maui/ADR-0011〜0013 (Cell 輸送・双方向値・uiStyle)・maui/ADR-0019〜0021 (CustomCell)・core/ADR-0028〜0029 (Is24Hour・PickerCell の object API)。

## 値の型 (MAUI 慣例型)

値の型は MAUI 慣例型 (maui/ADR-0004)。`DatePickerCell.AndroidButtonColor` のような片OS固有項目は接頭辞付き nullable。プロパティの網羅は各 Cell の XML doc が正で、ここに載せるのは型の表し方が MAUI 慣例に依るものだけ。

| プロパティ | 型 / 既定 | 意味 |
|---|---|---|
| `TimePickerCell.Time` | `TimeSpan` | 時刻の値 |
| `TimePickerCell.Is24Hour` | `bool`、既定 true | 選択面の時制の唯一の決定源。`Format` は行の表示専用 ([core/ADR-0028](../../../decisions/core/0028-timepickercell-is24hour-sole-hour-cycle-source.md)) |
| `DatePickerCell.Date` | `DateTime` | 日付部分のみ意味を持つ |
| `DatePickerCell.UIStyle` | `DatePickerUIStyle?` (両OS共通の enum `{ Calendar, Wheels }`)、null = native 既定 | 選択面の様式。意味マッピングは maui/ADR-0013。Android で `Calendar` を選ぶとカレンダーダイアログ固有の挙動 — テキスト入力モードへの切替 — が付随する。ホスト Activity 型・テーマへの要求はない |
| `EntryCell.Keyboard` | `Microsoft.Maui.Keyboard` | 入力キーボードの種類 |
| `EntryCell.TextAlignment` | `TextAlignment?`、null = platform 既定 | 入力テキストの行内揃え |

## PickerCell の候補と選択

`PickerCell` は `SelectionMode` (Single / Multiple) で単一・複数選択を切り替える。単一選択の正は `SelectedIndex`、複数選択の正は `SelectedIndices` (昇順・重複除去)。候補と選択は移植元 AiForms.Maui.SettingsView ([原典参照の規約](../../../handbook/cross/aiforms-origin-reference.md)) と同型の object API ([core/ADR-0029](../../../decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md))。

### ItemsSource と射影

`ItemsSource` (`IList?`) は任意の object 列。**null 要素は非対応** (設定時に `ArgumentException` — 許すと `SelectedItem == null` が「未選択」と区別できなくなる)。設定時に「元 object の snapshot」と「射影済み (text, subText) の snapshot」を同時に確定し、表示・逆引き・`SelectedItem(s)` は同一 snapshot を参照する (元コレクションの in-place 変更は観測しない — 差し替えで反映)。

射影は `DisplayMember` / `SubDisplayMember` (`string?`) のリフレクション sugar — 要素の実行時型の public instance の引数なし readable プロパティを解決し getter delegate を型別キャッシュする (ドット区切りパス式は非対応)。未指定・未解決は `ToString()` フォールバック (副表示は null)。値が string 以外なら `ToString()` 化、getter の例外は握りつぶさず伝播。`PropertyInfo` ベースで AOT の動的コード生成に依存しないが、文字列名でしか参照されないプロパティの trimming 保全は利用者契約 (未保全時は `ToString()` フォールバックへ退化)。旧 `DisplayFormatter` (候補の表示文字列を返す以前の API) は削除済み。

### SelectedItem / SelectedItems の相互導出

`SelectedItem` (`object?`) / `SelectedItems` (`IList?`、index 昇順) は正 (`SelectedIndex` / `SelectedIndices`) との相互導出 TwoWay。要素 → index の逆引きは値等価の最初一致で、公開値は直後に正から再導出される — 「設定した列がそのまま返る」保証はしない。`SelectedItems` の null と空リストはいずれも「選択なし」へ揃える。

**候補が 1 件も無い間は相互導出を行わない**: `ItemsSource` 未設定 (または null / 空へ差し替えた後) に設定された `SelectedItem(s)` は未選択へ揃えず保留し、候補到着時に逆引きして復元する — XAML 属性順・バインディング適用順で ViewModel の初期選択が黙って破棄されない (順序非依存)。候補が既にある状態からの差し替えは従来どおり位置 (index) が正。

公開値の snapshot 実体への正規化は **Cell の公開プロパティまで**が契約 — TwoWay 先の ViewModel は値等価な別実体を持ち得る (値等価でも変更通知を起こすために一度 null を代入してから入れ直す、といった強制伝播はしない。移植元 AiForms も VM への実体書き戻しはピッカー操作時のみで同挙動)。

### SelectedCommand (選択操作の完了通知)

`SelectedCommand` (`ICommand?`、OneWay・既定 null) は**利用者による選択操作の完了**を通知する — 値の TwoWay とは別軸で、同じ選択を確定し直しても実行される (値の変化ではなく確定操作の通知)。値の書き戻し (TwoWay) だけでは初期化・プログラム更新と利用者操作を区別できないため、完了通知は独立の公開面を持つ。発火源は native の選択確定通知のみ: 公開選択値の直接設定・cancel・非確定 dismiss では実行されない。実行は選択値の書き戻しと相互導出の完了後で、ViewModel は Command 内から新しい選択値を観測できる。

実行引数は届いた確定通知の種類で決まり、単一選択の通知で `SelectedItem`、複数選択の通知で `SelectedItems` (選択面表示中に `SelectionMode` が変わっても、利用者が確定した種類に対応する引数を渡す)。移植元互換で `CanExecute` は確認せず `Execute` を直接呼ぶ (`CommandCell.Command` が実効有効に反映するのと意図的に異なる)。任意の `CommandParameter` は提供しない。

## ユーザー操作と双方向バインド

対話型 Cell のユーザー操作は native から facade へ通知され、Cell 型ごとに数えて **12 組のプロパティ**が書き戻される (`Checked` は 2 Cell に現れるため名前では 11 個)。うち `SelectedItem` / `SelectedItems` を除く 10 組が maui/ADR-0012 の正規一覧で、輸送規約もそこが正。これらは **`BindingMode.TwoWay` が既定**で、ViewModel へそのまま流れる。その他のプロパティは OneWay 既定。

| Cell | 書き戻されるプロパティ |
|---|---|
| `SwitchCell` | `On` |
| `CheckboxCell` / `SimpleCheckCell` | `Checked` |
| `RadioCell` | `SelectedValue` (`GroupId` — RadioCell の所属グループを表す string — が同一の全 RadioCell へ同期される) |
| `EntryCell` | `ValueText` |
| `PickerCell` | `SelectedIndex` / `SelectedIndices`、および相互導出で書き戻される `SelectedItem` / `SelectedItems` (core/ADR-0029 の復元で追加) |
| `NumberPickerCell` | `Number` |
| `TimePickerCell` | `Time` |
| `DatePickerCell` | `Date` |

`CommandCell` / `ButtonCell` はタップで `Tapped` イベントと `Command` (`CommandParameter` 付き) を発火する Cell。実効有効状態は `IsEnabled && (Command?.CanExecute(CommandParameter) ?? true)` で、`CanExecuteChanged` に追随する。タップは実効有効のときだけ発火し、順序は `Tapped` イベント → `Command.Execute(CommandParameter)`。

- `EntryCell` の値変更 event / callback は公開しない — 経路は `ValueText` の TwoWay バインドのみ (AiForms 原典にも値変更 callback は無く、TwoWay バインドで足りるとするオーナー判断)
- `PickerCell` の選択操作の完了は `SelectedCommand` で通知される (上記「SelectedCommand」)

## CustomCell (任意の View を行の内容にする)

プリセットの Cell では表せない UI を1行に差し込むための Cell。行の内容領域は Disclosure Indicator を除く全域で、共通行レイアウトのスロット (title / description / icon) を持たない ([CustomCell の共通契約](../../core/cells/custom-cell.md))。

### 公開面

`CustomCell : CellBase`。`[ContentProperty]` は `Content` (`View?`、既定 null) で、XAML では CustomCell の直下に View を直書きする。null の間は空の内容の行になる。挙動プロパティは `Command` / `CommandParameter` / `Tapped` と `ShowArrowIndicator` (既定 false — CommandCell の矢印を隠すトグル `HideArrow` と向きが逆なのは、矢印の既定が Cell 種で逆であることによる)。実効有効状態 (`IsEnabled` と `CanExecute` の連動・`CanExecuteChanged` への追従) と発火順 (`Tapped` → `Command`) は CommandCell と同一。

**不適用プロパティは silent no-op**: `CellBase` から継承する `Title` / `Description` / `HintText` / `IconSource` とテキスト系のスタイル項目は表示に影響しない。設定しても例外・警告にはならず黙って無視される (同一のスタイル指定値を CustomCell を含む複数 Cell へまとめて当てられるようにするため。不適用の一覧は `CustomCell` の XML doc が正)。効くのは `IsEnabled` / `IsVisible` / `BackgroundColor` / `Height` と上記の固有プロパティ。

### タップの棲み分け

`Command` / `Tapped` をいずれも持たない行はタップ動作そのものを持たず、content 内の操作 (スライダー等) を妨げない。持つ行でも、content 内の操作対象がタップを消費したときは行タップは発火しない (二重発火しない)。購読の有無は表示後に変えても追従する。

### 更新と配置

content の内部の変化 (バインド値の更新等) はプロパティの再設定なしに表示へ届く。native への再発行が起きるのは `Content` を別インスタンスへ差し替えたときだけで、同一インスタンスのまま内容が変わっても行の内容は作り直されない (maui/ADR-0020)。行の高さは content のサイズに追従し、表示中のサイズ変化にも追う。切断中の差し替えや BindingContext の継承を含む更新規律の全体は [表示への反映と Host の寿命](maui-rendering-lifecycle.md)。

同一の View インスタンスを複数の `Content` へ、または `Content` と accessory View (Root / Section の header / footer に置く View) へ同時に置くことはできない (`InvalidOperationException`)。検査は `Content` の値が確定する**前**に行われるため、失敗しても `Content` の公開値と表示はどちらも動かない (accessory View 側も同じ規律 — maui/ADR-0022)。`ItemTemplate` から生成された各 CustomCell はそれぞれ独立した View 実体を持ち、対応する item を `BindingContext` として解決する。

### 提供範囲

提供するのはこの CustomCell と、それを組み立てて返す再利用形 (ファクトリメソッド、または `CustomCell` 派生サブクラス — 派生も同じ経路で描画される) まで。**利用者が独自の Cell 型と描画を登録する機構 (native の `KsCellRegistry`) は MAUI では公開しない** (maui/ADR-0019。決定の詳細は maui/ADR-0021 の公開面と併せて参照)。

## 関連

- [MAUI facade の公開契約](maui-facade.md) — Cell 階層の全体と facade の入口
- [スタイルの MAUI 表現](maui-styling.md) — CellBase のスタイル上書きプロパティ
- [表示への反映と Host の寿命](maui-rendering-lifecycle.md) — `IconSource` の解決・`Content` の更新規律と多重配置の検査
- [入力 Cell](../../core/cells/input-cells.md) / [基本 Cell](../../core/cells/basic-cells.md) / [CustomCell](../../core/cells/custom-cell.md) — Cell 意味論の共通契約
- [MAUI Native Bridge の interop 境界](native-bridge.md) — 双方向値の輸送規約と操作通知の経路
- [MauiView の native 実体化機構](../architecture/view-materialization.md) — `CustomCell.Content` を native へ届ける内部機構

決定の経緯: maui/ADR-0004 (MAUI 慣例型)、maui/ADR-0011 (per-type 輸送)、maui/ADR-0012 (双方向値の輸送規約)、maui/ADR-0013 (DatePickerUIStyle)、maui/ADR-0019〜0021 (CustomCell の提供層・content の live view と世代トークン・公開面と silent no-op)、maui/ADR-0022 (View 配置の検査)、core/ADR-0028 (Is24Hour)、core/ADR-0029 (PickerCell の object API)

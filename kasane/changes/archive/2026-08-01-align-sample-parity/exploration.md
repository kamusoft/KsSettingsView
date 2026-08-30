# Exploration: align-sample-parity

## 課題 / 動機

cross/conventions/sample-parity.md (cross/ADR-0016) を制定したが、現状の `samples/ios` / `samples/android` は規約に不一致。ksn-scout の棚卸しで差分を確認済み (下記)。iOS を正とし、まず iOS 側の「微妙なところ」を修正して正を確定させ、そのあと Android を同期させる2段構えで進める。

### 棚卸しで判明した主要差分 (2026-07-31 調査)

1. **入力 Cell 5 種デモが実質別画面** — Section 数 (iOS 7 / Android 3)、EntryCell 数 (5 / 3、Android にメール・callback セル無し)、PickerCell 複数選択 (5件・上限3 / 3件・上限2)、NumberPickerCell (サイズ 10-30 px / 音量 0-100 unit 無し)、TimePickerCell 初期値 (7:30 / 7:00)、DatePickerCell 初期値・uiStyle (.wheels+.calendar / Material+Spinner)、iOS のみ現在値プレビュー、Android のみ rootHeader
2. **iOS 全6画面でメニュー文言 ≠ 画面タイトル** — Store / DSL はタイトル欠落、基本 Cell 7 種・入力 Cell 5 種・共通フィールド統合・isVisible は語の欠落。Android は DemoScaffold(title:) 一元化で全一致
3. **共通フィールド統合デモ**: Android のみ RadioCell「ダーク」に hintText「推奨」が先行追加 (refine-cell-layout-after-unify-review Phase 7)、iOS 未追随。Section header 文言も不一致
4. **DSL デモ**: Section 名「動的 Section（ForEach）」(iOS) vs 「動的 Section（forEach）」(Android) の大小ゆれ
5. **Android ルートメニューが Button 列** (iOS は List + NavigationLink)、Android は複数画面が MainActivity.kt に同居 (iOS も StoreDemoView が ContentView.swift に同居)

## 検討した選択肢 (却下案と理由を含む)

### 入力 Cell 5 種デモの現在値プレビューと「ニックネーム (callback)」
- 案1: プレビュー削除 + callback セルも削除 → 却下 (EntryCell の Store 経路 = callback API の唯一の使用例が Sample から消える)
- **案2: プレビュー削除 + callback セルは説明付きで残す → 採用**。上部は基本 7 種デモと同じ1行イベント表示 (`最後のイベント: ...`) に置き換え、callback の効果はその1行で可視化。Section footer に「onTextChanged コールバック経路のデモ (他は Binding 経路)」の旨を明記して意図の不明さを解消
- 案3: プレビューを縮小して残す → 却下 (Android にも同等プレビューを作る羽目になり parity の荷物)

### Theme
- **A: MAUI 互換 Theme を入力 Cell 5 種デモにも適用 → 採用** (基本 7 種デモと完全に同系統の見た目にする)
- B: Theme 素のまま → 却下 (並べたとき別系統に見える)

## 決定事項

1. **iOS を正とする**。iOS 修正 → Android 同期の2段構え (規約が許容する「実装順序による一時的な片側先行」。同一 change 内で追跡)
2. 入力 Cell 5 種デモのスタイルは**基本 Cell 7 種デモに統一**: 現在値プレビュー廃止 → 1行イベント表示、MAUI 互換 Theme 適用、callback セルは footer 説明付きで存続 (上記 案2 + A)
3. iOS 全6画面の画面タイトルをルートメニュー文言と一致させる (二重管理の解消方法は propose で設計)
4. Android 先行の hintText「推奨」(RadioCell「ダーク」) と Section header 文言を iOS に追随 (唯一の Android→iOS 方向の同期)
5. Android 同期時: 入力 Cell 5 種デモを iOS 構成 (7 Section・文言・パラメータ) に全面一致、MainActivity.kt を**1画面1ファイルに分割**、ルートメニューを **iOS 同様のリスト表示**に変更

## ADR 候補 (作成済み: なし / 未起票: なし)

いずれも cross/ADR-0016 (Sample のプラットフォーム間検証装置化) の範囲内の実装判断であり、新規 ADR は不要と判断。

## 未決の論点

1. **DSL デモの Section 名「ForEach / forEach」の統一先** — iOS 正なら「ForEach」だが、API 名を画面文言に出すのをやめて中立文言 (例:「動的 Section（繰り返し）」) にする手もある。propose で決める
2. **DatePickerCell の uiStyle 差** (.wheels/.calendar vs Material/Spinner) — 本体公開 API の platform 差が絡む可能性。一致不可能なら規約に従い deviation.md に記録し本体側の統一課題とする。propose 時に Android 本体 API の能力を確認
3. **iOS StoreDemoView の分離** — ContentView.swift に同居している。Android の1画面1ファイル化と揃えて分離するか (推奨: 分離。安価で構造の platform 間対応が明確になる)
4. Android の rootHeader「入力 Cell 5 種のデモ」の扱い — iOS 正に合わせるなら削除

## UI 素材 (ui/references/ の一覧と注釈)

なし (既存画面同士の一致化のため、見た目の正は基本 Cell 7 種デモ実装と iOS 修正後のスクリーンショットになる想定)

## 変更級の推奨: M (理由)

- 触るのは samples のみで本体公開 API 変更なし・可逆性高 → L ではない
- ただし UI 変更 (Theme 適用・メニュー UI 変更・画面再構成) を含み、2 platform × 計8ファイル前後に及び、Android 側は画面の全面再構成 + ファイル分割リファクタを伴う → S には収まらない
- 規約上「片側先行の追跡」を tasks で管理する必要があり、デルタスペック + tasks の骨格があるほうが安全

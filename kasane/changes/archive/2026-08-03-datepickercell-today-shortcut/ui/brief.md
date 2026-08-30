# UI Brief: datepickercell-today-shortcut

## 画面と状態

構造階層 (既存の MaterialDatePicker ダイアログへの追加。器は変更しない):

```
MaterialDatePicker ダイアログ (ADR-0008 の配色・ヘッダ補正適用済み)
├── ヘッダ: タイトル (pickerTitle) + 選択日テキスト + 入力モードトグル
├── 月ナビゲーション行 (年月表示・年選択トグル・前月/翌月)
├── 日グリッド (month_grid)
└── アクション行: [今日] ←追加 ......... [キャンセル] [OK]
```

「今日」ボタンはアクション行の先頭 (左端) に置き、右端のキャンセル/OK と対置する (iOS のボタンバー配置と同じ役割位置)。

- **配置の実装注意**: `date_picker_actions` は右寄せ (end gravity) のコンテナのため、単純な先頭挿入では左端にならない。左端配置は伸縮スペーサ等で実現し、視覚照合は子 index ではなく**実座標** (今日 = 行左端側、キャンセル/OK = 右端側) で判定する
- **フルスクリーン表示**などアクション行が無い構成のフォールバック配置は実装判断で、mock 照合の対象外。ボタンの提示と挙動 (今日への移動・callback 不発火) のみを検証する

状態:

- **todayText 未指定 (既定)**: ボタン非表示。ダイアログ構成は現行と完全に同一
- **通常**: タップで表示月・選択マル・ヘッダ選択日・OK の状態が今日へ追随
- **今日が範囲外**: ボタンは表示されたまま、タップしても視覚変化なし (減光しない — iOS / Spinner と同型)
- **年選択の表示中**: タップで日グリッドへ戻ってから今日へ移動
- **テキスト入力の表示中**: タップでカレンダー表示に切り替わり今日へ移動

## リファレンス注釈

- 貼付画像なし。意匠の先例は2つ:
  - **Spinner モードの今日 chip** (DateSelectionSheet、accent 色 outline chip) — 同一プロパティの姉妹 UI
  - **MaterialDatePicker 純正のアクションボタン** (キャンセル/OK のテキストボタン、colorizer で accent 着色済み) — 同居する隣人
- variant A は「隣人に合わせる」(テキストボタン)、variant B は「姉妹に合わせる」(outline chip) の比較

## デザイントークン参照

- ダイアログ配色は既存の `PickerDialogColors` ロール (background = `Theme.backgroundColor` / accent = accentColor 段階解決 / text = titleColor) をそのまま使う ([style-resolution](../../../concepts/core/styling/style-resolution.md)、android/ADR-0008)
- 「今日」ボタンは **accent ロール** (キャンセル/OK と同じ色系統)。新しい色ロールは導入しない
- 生値はここに書かない。具体レイアウトは mock が正

## 検証条件 (動的挙動の判定基準)

視覚照合では静的な mock 照合に加えて以下を判定する:

- タップで表示月と選択マルが今日へ移動し、ヘッダの選択日テキストも更新される (callback は発火しない)
- OK 確定でのみ callback が発火する
- 今日が範囲外の構成ではタップしても何も変化しない
- 年選択表示中のタップで日グリッドへ戻って移動する
- テキスト入力表示中のタップでカレンダー表示に切り替わって移動する
- todayText 未指定ではダイアログが現行構成のまま

## 承認モック

mock/variant-a-today-text-button.html を採用 (approved.png、2026-08-03 オーナー承認)。

- 「今日」はキャンセル/OK と同格の accent テキストボタンで、アクション行の先頭 (左端) に配置。MaterialDatePicker 純正のボタン意匠に馴染ませる方針
- todayText 未指定時はボタンごと非表示 (アクション行は現行構成のまま)
- mock の accent/背景色は例示値。実際の配色は `PickerDialogColors` の実行時解決に従う
- variant-b-today-outline-chip.html (Spinner の今日 chip と同意匠の outline chip) は不採用の対案として保存

## 照合結果

実機 Pixel 6a (Android、端末日付 2026-08-03) の Sample アプリ「入力 Cell 5 種デモ」→ DatePickerCell（カレンダー）「予約日」で照合 (2026-08-03)。スクリーンショットは `verification/` に保存。

### 静的照合 (approved.png との一致)

| 観点 | 結果 |
| --- | --- |
| 構造 | 一致。アクション行 = [今日] … [キャンセル] [OK]。実座標 (1080px 幅) で 今日 `[89,257]` / キャンセル `[551,802]` / OK `[823,991]`、コンテナ `[68,1012]` — 今日の開始余白 21px とキャンセル/OK 側の終端余白 21px が対称 |
| 意匠 | 一致。今日は取消/確定と同じテキストボタン (`buttonBarNegativeButtonStyle`)、accent 着色。mock の色・フォントの生値は例示値のため、実行時テーマ色 (`PickerDialogColors` の accent = Sample テーマの amber) との一致は求めていない |
| トークン | 一致。accent ロールのみを使用し新規の色ロールなし |
| 状態 | brief.md の 5 状態すべてを実機で確認 (下表) |
| アクセシビリティ | `Button` / `text="今日"` / `clickable=true` / `focusable=true` で公開 (uiautomator dump で確認) |

### 動的照合 (検証条件 6 項目)

| 検証条件 | 結果 | 証跡 |
| --- | --- | --- |
| タップで表示月・選択マル・ヘッダが今日へ追随し callback は発火しない | PASS: 2026年6月 → 2026年8月、3 に選択マル、ヘッダ「2026年8月3日」、「最後のイベント: (none)」のまま、Cell の valueText も 2026/06/01 のまま | `material-dialog-before-today-tap.png` / `material-dialog-after-today-tap.png` |
| OK 確定でのみ callback が発火する | PASS: OK で「最後のイベント: 予約日 → 2026/08/03」、Cell も更新 | `input-cells-demo-after-ok-confirm.png` |
| 今日が範囲外なら無反応 | PASS: `maxDate = 2026-06-30` の検証用 Cell で 2 回タップしても表示月・選択・ヘッダとも不変。ボタンは減光せず表示のまま | `material-dialog-today-out-of-range-no-change.png` |
| 年選択の表示中から復帰して移動 | PASS: 2030 年選択 → 年選択を開いた状態で今日タップ → 日グリッドへ戻り 2026年8月 / 選択 3 | `material-dialog-year-selector-open.png` / `material-dialog-after-today-from-year-selector.png` |
| テキスト入力の表示中から切替えて移動 | PASS: 入力を「2020/」(不完全) にして OK が無効な状態から今日タップ → カレンダー表示・2026年8月・選択 3・OK 有効に復帰。callback は不発火 (再 build 経路でも着色は保持) | `material-dialog-text-input-incomplete.png` / `material-dialog-after-today-from-text-input.png` |
| todayText 未指定では現行構成のまま | PASS: 今日ボタンもスペーサも挿入されず、キャンセル/OK の実座標は指定時と同一 (`[551,802]` / `[823,991]`) | `material-dialog-without-today-text.png` |

追加確認: 今日ボタンの連打 (3 連タップ) の結果は 1 回の実行と同じ (ダイアログの二重表示なし・callback 不発火)。

乖離なし・実装側の修正なし・プラットフォーム制約による妥協なし・トークン候補なし。

補足:
- Sample (`samples/android/.../InputCellsDemoScreen.kt`) のカレンダーモード Cell にデモ用の `todayText = "今日"` を追加した (この変更のみ恒久)
- 「範囲外」「todayText 未指定」の 2 枚は検証用に一時追加した Cell (`maxDate = 2026-06-30` / `todayText` 未指定) で撮影し、撮影後に Sample から削除した

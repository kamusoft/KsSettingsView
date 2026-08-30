# UI Brief: timepickercell-color-adjust

## 画面と状態

対象は Android TimePickerCell の時刻選択ダイアログ (MaterialTimePicker) のみ。設定リスト本体の見た目は変えない。

- **キーボード入力モード** (24h フォーマット時の既定起動モード)
  - 状態: 時フィールド選択中 / 分フィールド選択中 (選択中フィールドに強調枠とキャレット)
- **時計文字盤モード**
  - 状態: 時選択中 (24h は二重リング) / 分選択中 (単一リング)。選択ノブ・針・中心ドットが強調色
- **共通**: ヘッダタイトル、モード切替ボタン、キャンセル / OK
- 12h フォーマット時は AM/PM トグルが加わる (選択状態が強調色)

## リファレンス注釈

- `references/current-keyboard-input-ui.webp` — **修正前**の実機スクショ。レイアウト構造・寸法・フォントは Material 標準のまま**採用** (変更しない)。配色 (紫の選択枠・キャレット、既定のサーフェス色) が**今回変更する対象**。ボタンのピンクはホスト Activity テーマ由来の偶発的な色で、目標状態ではない
- `references/current-clock-input-ui.webp` — 同上。文字盤・針・ノブ・時刻チップの紫が変更対象
- 目標状態は mock/ が正

## デザイントークン参照

色はすべて Theme / CellStyle / Cell の解決値を使う (生値を持ち込まない):

- 背景 ← `Theme.backgroundColor` (モック A) / `Theme.cellBackgroundColor` (モック B) — どちらを採用するかモック承認で決定
- 強調 ← `TimePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor` の解決値
- 文字 ← `cellTitleColor` の解決値
- アクセント上の文字 ← アクセント輝度による白/黒自動選択
- 非選択フィールド・文字盤円などの中間面 ← 背景色から導出 (実装では背景色への低透過の黒/白オーバーレイ。mock が見た目の正)

解決規則は kasane/concepts/core/styling/style-resolution.md を正とする。

## 部位対応表 (デルタスペックの色ロール → 部位の割当。承認モックと合わせて見た目の正)

| 部位 | ロール |
|---|---|
| ダイアログ面 (window 背景、角丸維持) | 背景 |
| 時/分フィールド・チップの選択状態 (枠 + 低透過の塗り) | 強調 |
| テキスト入力のキャレット | 強調 |
| キャンセル / OK の文字 | 強調 |
| 時計の針・選択ノブ・中心ドット | 強調 |
| AM/PM トグルの選択状態 (12h 時) | 強調 |
| ヘッダタイトル | 通常文字 |
| 時/分フィールド・チップ内の数字、「:」区切り | 通常文字 |
| 「時間」「分」ラベル | 通常文字 |
| 文字盤の数字 (ノブ上を除く) | 通常文字 |
| モード切替ボタンのアイコン | 通常文字 |
| キーボード入力欄の入力文字 | 通常文字 |
| ノブ上の数字・選択チップ等の solid アクセント面に載る文字 | アクセント上文字 |
| 非選択フィールド・チップの塗り、文字盤の背景円 | 中間面 (背景から導出) |
| scrim (ダイアログ外の暗転)、ステータスバー | 対象外 (システム既定) |
| キーボード入力欄の入力エラー表示 (Material の error 色) | 対象外 (既定のまま) |
| タッチ時のリップル | 対象外 (今回は既定のまま。違和感が出たら実装中に相談) |

- 中間面の導出規則: 背景色の輝度が高い場合は黒の低透過オーバーレイ、低い場合は白の低透過オーバーレイを背景色に合成する。透過率の見た目の正は mock (approved.png)

## 承認モック

mock/variant-a-background-color.html を採用 (approved.png、2026-08-02 オーナー承認)。

- ダイアログ背景 = `Theme.backgroundColor` で確定
- B案 (cellBackgroundColor 基調) は不採用。運用で背景色の競合が問題になった場合は、その時点でダイアログ背景の独立プロパティ追加を検討する (オーナー方針)
- アクセント上の文字 = 輝度による白/黒自動選択、通常文字 = cellTitleColor 解決値もモックのとおり承認

## 照合結果 (実装後)

実機 (Pixel 系 Android / adb 0B261JEC216142、サンプルアプリ「入力 Cell 5 種デモ」、SampleTheme.maui) で
撮影したスクリーンショットを `verification/` に保存し、mock/approved.png と部位対応表で照合した。

| ファイル | 内容 |
|---|---|
| `verification/keyboard-24h.png` | 24h キーボード入力モード (時フィールド選択中・キャレット点灯) |
| `verification/clock-24h-hour.png` | 24h 時計文字盤モード (時選択中・二重リング) |
| `verification/clock-24h-minute.png` | 時 → 分 遷移後 (配色維持の確認) |
| `verification/clock-12h-am.png` | 12h 時計文字盤モード (AM 選択中) |
| `verification/keyboard-12h-pm.png` | 12h キーボード入力モード (PM 選択中・モード切替後) |

部位対応表の全ロール (背景 / 強調 / 通常文字 / アクセント上文字 / 中間面) が承認モックと一致することを確認した。

**最終承認**: verification/ の各画像と approved.png の照合結果、および下記「モック側の記載と実装値の食い違い」「妥協点」の全項目を 2026-08-02 オーナー確認・承認済み。モックの hex 食い違い (#CC9900) はモック側が古いだけであり、実装は spec のトークン解決 (cellTitleColor) を正とすることで確定。

### モック側の記載と実装値の食い違い (2026-08-02 オーナー承認済み — 実装の spec 準拠を正とし、モックの hex は古い値として扱う)

- **通常文字の色**: モックの CSS 変数は `--text: #CC9900` で、注釈は「cellTitleColor 解決値」。
  しかし `SampleTheme.maui` の `cellTitleColor` は `#555555` (`mauiDeepText`) であり、`#CC9900` は
  `headerTextColor` / `mauiTitleText` の値。実装はデルタスペックと本 brief のトークン指定
  (`CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定) に従い `#555555` で描画している。
  ロールの割当は一致しており、食い違うのはモック内の hex リテラルのみ。

### 妥協点 (実装時に発生。2026-08-02 オーナー承認済み)

- **AM/PM トグルの枠線**: モック (24h) に AM/PM が登場せず、部位対応表にも枠線の指定が無いため、
  Material 既定 (`colorOutline` 由来のグレー) のまま残した。選択状態の塗りと文字だけを強調ロールにしている
  (`verification/clock-12h-am.png` 参照)。
- **モード切替ボタンのアイコン形状**: Material 標準アセット (`ic_keyboard_black_24dp` は塗りアイコン) を使うため、
  モックの自前 SVG (線画) とシルエットが異なる。色ロール (通常文字) は一致。
- **選択中の文字盤数字のグラデーション演出**: 着色のため shader を落としており、色がスナップ切替になる
  (android/ADR-0006 の Consequences に既知として記載済み)。
- **リップル・scrim・入力エラー表示**: 部位対応表どおり Material 既定のまま。

### トークン候補

- **中間面のオーバーレイ透過率 5.5%** と **選択塗りのアクセント透過率 16%**: モック由来の値を
  `TimePickerColors` の定数として実装に置いた。Theme / CellStyle に相当するトークンは存在しない。

### 実機で判明した material-components の挙動 (ADR-0006 の机上確定の補正)

- **キーボード入力欄の枠**: `TextInputLayout` は `boxStrokeColor` の CSL から `state_focused` の色を
  取り出して保持する実装であり、駆動源は **内部 EditText のフォーカス**。`state_selected` では駆動されない
  (実機ログで `til.selected=false` / `et.focused=true` のとき枠がアクセント色になることを確認)。
- **キャレット**: `TextInputLayout` が状態変化のたびに `cursorColor` (未指定時は `colorControlActivated`) で
  `textCursorDrawable` を塗り直すため、EditText 側へ直接 tint を当てても上書きされる。
  `TextInputLayout.cursorColor` に設定する必要がある。
- **pre-draw での再適用**: 走査内容をそのまま毎フレーム適用すると
  (`setBoxStrokeColorStateList` 等が無条件に再描画を要求するため) 描画が 60fps で回り続ける。
  「View ごとに 1 回の静的適用」と「文字盤の数字だけの動的適用」に分けることで、素の描画頻度
  (キャレット点滅由来の 2〜3 回/秒) まで戻ることを実機計測で確認した。

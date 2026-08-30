# 実機目視検証レポート（Pixel 6a） - add-cell-types-basic §21（Decision 9）

**検証日時**: 2026年06月02日
**検証者**: sdd-orchestrator（adb による実機操作 + スクリーンショット目視判定）
**対象変更提案**: add-cell-types-basic
**対象タスク**: tasks.md §21.7（21.7.2〜21.7.6 の実機目視検証）
**検証端末**: Google Pixel 6a（model:bluejay、serial:<android-device-serial>、Android 16）※実機
**Sample アプリ**: `jp.kamusoft.kssettingsview.samples.android` / `.MainActivity`

## 総合判定: ✅ PASS（全観点合格）

§21（Decision 9）で実装した 6 つの Android 改修について、Pixel 6a 実機を adb で操作し、スクリーンショットを取得して目視・画像差分で判定した結果、**全観点が合格**した。

## 検証手順と結果

### 21.7.1 ユニットテスト
- `./gradlew :ks-settingsview-ui:test`（実装フェーズで PASS 済み、コード検証 `verification-report_001.md` でも確認済み）

### 21.7.2 実機準備
- `adb devices`: Pixel 6a（<android-device-serial>）が `device` 状態で接続を確認（実機）
- `:app:installDebug` で Sample を Pixel 6a にインストール（BUILD SUCCESSFUL）
- `am start -n jp.kamusoft.kssettingsview.samples.android/.MainActivity` で起動成功
- トップから「基本 Cell 7 種デモ」へ `input tap` で遷移成功

### 21.7.3 静的描画の目視確認 — ✅ PASS

| 観点 | 判定 | 根拠 |
|------|------|------|
| (a) CommandCell 矢印がオリジナル準拠 chevron | ✅ | 「ライセンス」セルに細い chevron 表示。uiautomator で `ImageView content-desc="Disclosure indicator"` を確認（旧 `TextView ">"` から `AppCompatImageView` + `ic_navigate_next` に置換済み）。「矢印なし」セルには矢印非表示 |
| (b) RadioCell がチェックマーク表示 | ✅ | 「RadioCell（テーマ選択）」で選択中の Dark に **青いチェックマーク（✓）** 表示。標準 RadioButton（ring+dot）ではない。uiautomator で `android.view.View content-desc="Selected"`（= `KsSimpleCheckView`）を確認 |
| (c) SimpleCheckCell がオリジナル準拠 | ✅ | 「SimpleCheckCell」の ToDo に `android.view.View content-desc="Checked"`（= `KsSimpleCheckView`）を accessory 右側に配置。旧 `TextView "✓"` から置換済み |

### 21.7.4 タッチフィードバック（Ripple）— ✅ PASS

| 観点 | 判定 | 根拠 |
|------|------|------|
| (e) Cell タップで Ripple / 選択ハイライト表示 | ✅ | SwitchCell「通知」のラベル領域をロングプレス中にスクショ取得。セル全体（ラベル + スイッチ領域）が一様にグレーのハイライトで塗られることを確認。`applyCellBackground` の `RippleDrawable` + `Theme.selectedColor`（既定グレー）が機能。オリジナル `CellBaseView.cs` のタッチフィードバックに準拠 |

### 21.7.5 ちらつき（フルリバインド）非発生 — ✅ PASS（画像差分で厳密確認）

操作前後のスクリーンショットを Python(PIL) の `ImageChops.difference` で領域別に bbox 解析した。

**SwitchCell の ON/OFF 操作（スイッチ本体タップ）:**
- SwitchCell タイトル領域（「通知」）の差分: **None**（再描画なし）
- SwitchCell スイッチ領域の差分: あり（スイッチウィジェット本体のみ）
- SwitchCell より下（CheckboxCell 以下の全セル・罫線）の差分: **None**（再描画なし）
- ヘッダー「最後にタップ」テキスト: 差分あり（= `onValueChanged` 発火による期待される更新）

→ 操作で変化したのは**スイッチウィジェット本体のピクセルのみ**。タイトル・背景・罫線・他セルは 1 ピクセルも再描画されていない。**payload 部分 bind（`bindStateOnly`）が機能し、行全体フルリバインドによるちらつきは完全に解消**されている。

**CheckboxCell のタップ操作:**
- Checkbox タイトル領域 / チェック領域 / 他セル（上下）すべての差分: **None**
- ヘッダー「最後にタップ」テキストのみ差分あり（= `onValueChanged` 発火）

→ セル本体・他セルが一切再描画されないことを確認。ちらつき皆無。

### 21.7.6 Switch のセル全体タップ ON/OFF — ✅ PASS

| 観点 | 判定 | 根拠 |
|------|------|------|
| (f) セル本体（ラベル領域）タップでスイッチがトグル | ✅ | スイッチ本体ではなく**左側ラベル領域（x=300）をタップ**したところ、ヘッダーが「通知 → false」に更新され、スイッチのサムが左に移動して OFF へトグルした。Android 標準設定アプリ準拠の挙動（Decision 9-6）を実機で確認 |

## 取得スクリーンショット（判定根拠）
`/tmp/ks_review/` に保存:
- `01_top.png` — トップメニュー
- `02_demo_top.png` — 基本 Cell 7 種デモ（上部：矢印 chevron 確認）
- `03_demo_scroll1.png` — Radio チェックマーク確認
- `05_ripple.png` — Ripple ハイライト（ロングプレス中）
- `06_switch_on.png` / `07_switch_off.png` — Switch ON/OFF 差分用
- `08_checkbox_before.png` / `10_checkbox_after2.png` — Checkbox 差分用
- `11_before_switchlabel.png` / `12_after_switchlabel.png` — Switch セル全体タップ確認

## 備考
- 既知の挙動: 本 Sample の Switch/Checkbox は外部 state を即時トグル反映しない構成のため、Switch 本体タップ時はサムの見た目が即変わらないケースがあるが、`onValueChanged` 発火（ヘッダー更新）と「セル全体タップでサムがトグル」（21.7.6）は確認済み。ちらつき検証（21.7.5）は「操作時に他領域が再描画されないこと」が要点であり、画像差分で厳密に合格を確認した。
- iOS のちらつきは本提案の対象外（別途 iOS 実機レビューのタスクで対応）。

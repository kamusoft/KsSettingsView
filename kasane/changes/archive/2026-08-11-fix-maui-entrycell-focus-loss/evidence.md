# 実機検証証跡の索引: fix-maui-entrycell-focus-loss

検証端末: Pixel 6a (bluejay, adb シリアル <android-device-serial>)、アプリ: jp.kamusoft.kssettingsview.samples.maui「入力 Cell 5 種デモ」。
フォーカス判定は `adb shell dumpsys input_method` の `mServedView` フラグ (`.F` = view フォーカスあり)。

## round-1 (初回修正後の検証, 2026-08-11)

| ファイル | 内容 |
|---|---|
| verify-device-03-after-abcde.png | ASCII 連続入力 a→b→c→d→e (全打鍵で `.F` 維持) |
| verify-device-04-after-del5.png | BackSpace (keyevent 67) 連打 5 回で連続削除成立 (`.F` 維持) |
| verify-device-05-append-123.png | 末尾キャレットで 1→2→3 を 1 文字ずつ追記 (順序正常) |
| verify-device-06-crop.png | 中間キャレット挿入: `Tanakab\|cdea123` へ x→y→z 挿入 → `Tanakabxyzcdea123` |
| verify-device-07-crop.png | 別 Cell (メール欄) の中間挿入: `tanaka.taro@\|example.com` へ `123` 挿入 |
| verify-device-09-crop.png | 日本語 IME (Gboard): composing「か」未確定表示中も `.F` 維持 |
| verify-device-10-crop.png | 日本語 IME: 確定 (✓) 後に 2 文字目「か」を composing→確定、「かか」成立 |
| verify-device-12-crop.png | 日本語 IME: 変換候補選択 → ↵ 確定 (`.F` 維持) |
| verify-device-13-crop.png | 日本語 IME: 確定後に続けて次の文字を composing (連続入力成立) |
| verify-device-14-basic-cells.png | 回帰確認: 基本 Cell 7 種デモの表示正常 |
| verify-device-15-basic-scrolled.png | 回帰確認: 同ページのスクロール正常・Cell タップ反応 |
| verify-device-16-17-side.png | 回帰確認: 共通フィールド統合デモ / isVisible デモの表示正常 |

## round-2 (review-001 対応後の再検証, 2026-08-11)

| ファイル | 内容 |
|---|---|
| verify-device-fix2-01-input-page.png | フォールバック実装後の入力 Cell ページ表示 (全幅正常) |
| verify-device-fix2-02-ascii-continuous.png | ASCII 連続入力 5 文字 (`Tanaka T` → `Tanaka Tabcde`) |
| verify-device-fix2-03-backspace-continuous.png | BackSpace 連打 5 回 (`Tanaka Tabcde` → `Tanaka T`) |
| verify-device-fix2-04-basic-cells-regression.png | 回帰確認: 基本 Cell 7 種デモ |
| verify-device-fix2-05-basic-cells-scrolled.png | 回帰確認: 同・スクロール後 |
| verify-device-fix2-dumpsys.txt | 上記全操作の `mServedView` 生ログ (タップ + 5 打鍵 + BackSpace 5 回の 11 時点全てで `.F` 維持・同一インスタンス・幅ゼロ化なし) |

補足: round-2 で日本語 IME を再取得していないのは、有限制約時の計算式が round-1 から不変 (追加は非有限フォールバック分岐のみ) で、サンプル配置 (Grid `*` 行 = 有限制約) では同一経路のため (review-002 で妥当性確認済み)。

修正前の再現記録 (フォーカス喪失・EditText 幅ゼロ化の jdb 実測) は exploration.md の「実測記録」参照。

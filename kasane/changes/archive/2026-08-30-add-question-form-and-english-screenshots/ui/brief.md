# UI Brief: add-question-form-and-english-screenshots

本変更は**新規 UI の設計を含まない**。ルート README に載せるスクリーンショット 4 枚の差し替えだけが UI アーティファクトの対象で、撮影対象は既存 Sample アプリの画面である。したがって HTML モックは生成せず、**実機 (シミュレータ / エミュレータ) で撮影した候補から採用を選ぶ**形で承認ゲートを置く (phase-9 と同じ形)。

## 画面と状態

撮影対象は既存 Sample アプリの「Section 装飾デモ (style 切替)」画面。iOS / Android × Modern / Classic の 4 枚。既存の `assets/` 4 枚と同じ画面・同じスクロール位置 (最上部)・同じ装飾プリセット (既定) で撮り、**日本語 → 英語表示だけが差分**になるようにする。

MAUI は Native をラップして同じ画面になるため画像を置かない (README 側で文により補足済み)。

## レイアウト

README 上の並びは既存どおり — 横 2 列 (左 = Modern、右 = Classic) × 縦 2 行 (上 = iOS、下 = Android)。ファイル名も既存を踏襲し、`assets/{ios,android}-{modern,classic}.png` を上書きする。

## 撮影のための一時英訳

対象画面の表示文字列を下表のとおり一時的に英語へ書き換えて撮影し、**撮影後に元へ戻す** (`samples/` に恒久差分を残さない)。書き換えるのは iOS と Android のみ (撮影対象がこの 2 platform のため)。MAUI は触らない。

| 定義元 | 日本語 (現行・撮影後に戻す) | 撮影時の英語 |
|---|---|---|
| `SampleScreen` (画面タイトル) | Section 装飾デモ（style 切替） | Section decoration (style switch) |
| DemoControls | 装飾プリセット | Decoration preset |
| Preset | 既定 | Default |
| Preset | 余白広め・角丸小 | Wide padding, small radius |
| Preset | ボーダーあり | With border |
| DemoScreen | 機内モード | Airplane Mode |
| DemoScreen | オン | On |
| DemoScreen | バッテリー | Battery |
| DemoScreen | 外観モード | Appearance |
| DemoScreen (footer) | 好みに応じて外観モードを選択できます。Header と Footer は箱の外側に配置されます。 | Choose the appearance mode you prefer. Header and Footer are placed outside the box. |
| DemoScreen | 自動 | Automatic |
| DemoScreen | テキストサイズを変更 | Change Text Size |
| DemoScreen (header) | ボーダー指定時の例 | Example with a border |
| DemoScreen (footer) | 既定はボーダーなし (width 0)。指定時のみ枠線が箱の輪郭に描かれます。 | No border by default (width 0). A border is drawn along the box outline only when specified. |

`Wi-Fi` / `Bluetooth` / `demoAP-0a1b2c-5` / `True Tone` / `sectionBorderWidth: 2` / `sectionBorderColor: gray` は現行のまま (言語非依存)。

画面タイトルは当初案の `Section decoration demo (style switching)` が Android の `TopAppBar` で 2 行に折り返り、日本語版 (1 行) と構図が変わったため、`Section decoration (style switch)` へ短縮した。短縮後は iOS / Android とも 1 行に収まり、日本語版と同じ構図になる。`demo` は画面がメニュー「デモ」節から開かれることで文脈が保たれるため落とし、`switching` は `switch` に詰めた。

この対訳表は**後日の撮り直しを再現可能にするための記録**でもある。撮り直すときはこの表を使って同じ英訳で再現する。撮影中に訳を短縮した場合は、この表を実際に使った訳へ更新してから承認へ進む。

## リファレンス注釈

`ui/references/` は実装時に撮影する候補の置き場。撮影前は空。

## 撮影の統制

現行 `assets/` の 4 枚は、iOS / Android・Modern / Classic とも**ステータスバー・画面タイトル・style 切替セグメント・装飾プリセット行がすべて写った同一構図**で撮られている。差し替え後もこの構図を正とする。

固定する撮影条件:

| 項目 | 値 |
|---|---|
| 端末 | iOS: iPhone 17 Pro Simulator / Android: Pixel_6 Emulator (phase-9 と同一。実機は使わない) |
| 向き | 縦 (portrait) |
| 初期スクロール位置 | 最上部 (画面タイトル・style 切替セグメント・装飾プリセット行が見える位置) |
| 装飾プリセット | 既定 (Default) |
| ステータスバー | iOS: `simctl status_bar override` / Android: SystemUI demo mode。時刻は 9:41 に固定 |

ステータスバーの扱い — **ステータスバー自体は写してよい**。禁止するのは実機の時刻・実際のバッテリー残量・キャリア名・端末名・通知で、撮影用に固定したデモ表示 (時刻 9:41・満充電を示す電池アイコン) は許容する。

そのほか:

- 4 枚は同一のデモ画面・同一のスクロール位置・同一の装飾プリセットで撮り、platform と style 以外の差を作らない
- **文字列が切れたら英訳を短くしてよい**。英語は日本語より長くなりやすく、特に画面タイトルは Android の単一行 `TopAppBar` で省略される可能性がある。切れ・重なり・不自然な折り返しが出た場合は意味を保った短い訳へ調整し、**採用した訳を上の対訳表へ反映する** (表が撮影の正の記録であるため)

## 撮影の実績

`ui/references/` の 4 枚を撮ったときの実際の条件。

| 項目 | iOS | Android |
|---|---|---|
| 端末 | iPhone 17 Pro Simulator / iOS 26.4 | Pixel_6 Emulator / API 31 (`emulator-5554`) |
| 解像度 | 1206 × 2622 | 1080 × 2400 |
| 向き | 縦 (portrait) | 縦 (portrait) |
| 初期スクロール位置 | 最上部 | 最上部 |
| 装飾プリセット | Default | Default |

ステータスバー統制に使ったコマンド:

```sh
# iOS
xcrun simctl status_bar <ios-udid> override \
  --time "9:41" --dataNetwork wifi --wifiMode active --wifiBars 3 \
  --cellularMode notSupported --batteryState charged --batteryLevel 100

# Android
adb -s emulator-5554 shell settings put global sysui_demo_allowed 1
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command enter
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 100
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command network -e wifi hide -e mobile hide
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command status -e volume hide -e bluetooth hide
```

- 実機は使用していない (`adb devices` に接続されていた実機 2 台は明示的に除外し、`emulator-5554` を指定した)
- 4 枚の最下部で 4 つ目 Section の footer が画面下端で見切れるのは、日本語版の現行 `assets/` と同じスクロール位置による同一の見切れであり、文字列の切れ・折り返しではない

## 承認モック

**承認日: 2026-08-30 / オーナー承認済み (「この 4 枚で採用」を選択)**

| platform | style | 採用した候補 | 配置先 |
|---|---|---|---|
| iOS | Modern | `references/ios-modern.png` | `assets/ios-modern.png` |
| iOS | Classic | `references/ios-classic.png` | `assets/ios-classic.png` |
| Android | Modern | `references/android-modern.png` | `assets/android-modern.png` |
| Android | Classic | `references/android-classic.png` | `assets/android-classic.png` |

- HTML モックは存在せず、実機 (シミュレータ / エミュレータ) で撮影した候補そのものが承認対象 (本 brief 冒頭の定めによる)
- 撮影条件は「## 撮影の実績」のとおり。画面タイトルの英訳を対訳表から短縮した件 (`Section decoration demo (style switching)` → `Section decoration (style switch)`) も含めて承認された
- 4 枚は同一のデモ画面・同一のスクロール位置 (最上部)・同一の装飾プリセット (Default) で撮られ、platform と style 以外の差を持たない

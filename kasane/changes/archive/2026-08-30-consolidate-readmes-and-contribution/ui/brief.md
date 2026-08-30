# UI Brief: consolidate-readmes-and-contribution

本変更は**新規 UI の設計を含まない**。ルート README に載せるスクリーンショットの選定だけが UI アーティファクトの対象で、撮影対象は既存の Sample アプリの画面である。したがって HTML モックは生成せず、**実機 (シミュレータ / エミュレータ) で撮影した候補から採用を選ぶ**形で承認ゲートを置く。

## 画面と状態

撮影対象は既存 Sample アプリの次の画面 (3 platform とも同名の画面が存在する):

| 候補画面 | 撮れるもの |
|---|---|
| Section 装飾デモ (style 切替) | Modern / Classic の対比。style 切替コントロールが画面に含まれる |
| 基本 Cell 7 種デモ | 設定画面らしい構成 (Cell が並んだ状態) |

必要な組み合わせは iOS / Android × Modern / Classic の 4 枚。

## レイアウト

README 上では横 2 列 (左 = Modern、右 = Classic) × 縦 2 行 (上 = iOS、下 = Android) で並べる。MAUI は Native をラップして同じ画面になるため画像を置かず、文で 1 行補足する。

## リファレンス注釈

`references/` は実装時に撮影する候補の置き場。撮影前は空。

## 撮影の統制

- シミュレータ / エミュレータで撮影する (実機を使わない)
- ステータスバーに端末を特定できる表示 (キャリア名・実機の時刻・バッテリー残量) を写さない
- 4 枚は同一のデモ画面・同一のスクロール位置で撮り、style と platform 以外の差を作らない

## 承認

**承認日: 2026-08-29 / オーナー承認済み (「そのまま採用」を選択)**

| platform | style | 採用した候補 | 撮影したデモ画面 | 配置先 |
|---|---|---|---|---|
| iOS | Modern | `references/ios-modern.png` | Section 装飾デモ (style 切替) | `assets/ios-modern.png` |
| iOS | Classic | `references/ios-classic.png` | Section 装飾デモ (style 切替) | `assets/ios-classic.png` |
| Android | Modern | `references/android-modern.png` | Section 装飾デモ (style 切替) | `assets/android-modern.png` |
| Android | Classic | `references/android-classic.png` | Section 装飾デモ (style 切替) | `assets/android-classic.png` |

4 枚は同一のデモ画面・同一のスクロール位置 (最上部)・同一の装飾プリセット (既定) で撮り、platform と style 以外の差を持たない。デモ操作部 (Classic / Modern 切替セグメントと装飾プリセット行) を含む全画面を採用した。

### 撮影条件

| 項目 | iOS | Android |
|---|---|---|
| 端末 | iPhone 17 Pro / iOS 26.4 Simulator | Pixel_6 / API 31 Emulator (`emulator-5554`) |
| 解像度 | 1206 × 2622 | 1080 × 2400 |
| ステータスバー統制 | `simctl status_bar override` (時刻 9:41 / Wi-Fi 3 本 / 充電済み 100% / セルラー非表示) | SystemUI demo mode (時刻 9:41 / 電池アイコンのみ / モバイル非表示 / 通知非表示) |

- 実機は使用していない (`adb devices` に接続されていた実機 2 台は明示的に除外し、`emulator-5554` を指定した)
- 端末を特定できる表示 (キャリア名・実機の時刻・バッテリー残量) は 4 枚とも写っていない

### 撮影対象の決定

brief では候補を 2 つ挙げていたが、**Section 装飾デモ (style 切替) を採用**した。実行時に Modern / Classic を切り替えられるデモ画面はこれだけで、もう一方の候補「基本 Cell 7 種デモ」は Android 実装が `style = KsSettingsViewStyle.Classic` 固定 (`samples/android/.../BasicCellsDemoScreen.kt`) であり、Modern 版を撮るには Sample のコード変更を要するため (本変更のスコープ外)。

### 申し送り

Sample アプリの UI は日本語固定で、iOS / Android とも英語リソースを持たない。spec `repository-docs` の Requirement「スクリーンショットの提示」が英日 README で同一画像の参照を求めるため、英語 README にも日本語 UI の画像が載る。Sample の英語化は本変更のスコープ外。

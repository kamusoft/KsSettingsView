# ライト外観の基準撮影 (tasks 8.4 前半)

実装着手前に、本 change の分岐元コミット `db2d1f2` (`develop`) と同一のコードで撮影したライト外観の基準画像。
実装後に**同じ機種・OS・画面状態**で A/B を撮り、ステータスバーを除いて比較する。

## 撮影条件

| 実行面 | 機種 / OS | 画面サイズ | 外観の与え方 |
|---|---|---|---|
| iOS Native | iPhone 17 Pro Simulator / iOS 26.0 | 1206 x 2622 px (402 x 874 pt) | Simulator を light に設定 (`simctl ui <udid> appearance light`)、Sample の「外観」は「システム」 |
| Android Native | Android Emulator (AVD `ksn_custcell_api35`) / Android 15 (API 35) | 1080 x 2340 px | 端末の夜間モード off (`cmd uimode night` = no)、Sample の「外観」は「システム」 |
| MAUI iOS | 同 iOS Native | 同上 | 同上 |
| MAUI Android | 同 Android Native (同一エミュレータ) | 同上 | 同上 |

- Xcode: `/Applications/Xcode-26.5.0.app` (`DEVELOPER_DIR` に明示)
- ビルド構成: いずれも Debug
- Simulator / Emulator の UDID・シリアルは可変のため記録しない (機種名と OS 版が同じであれば再現できる)

## 画面状態

- どの画面もルートメニューから 1 回タップで遷移した直後の状態で撮影し、**スクロールは行っていない** (スクロール位置は先頭)
- トグル・選択の状態はいずれも画面の初期値のまま (操作していない)
- Section 装飾デモは初期プリセット「既定」・style は初期選択のまま

## ファイル一覧

| ファイル | 画面 |
|---|---|
| `ios-native-store-light.png` | Store 方式デモ |
| `ios-native-dsl-light.png` | DSL 方式デモ |
| `ios-native-unify-common-fields-light.png` | 共通フィールド統合デモ |
| `ios-native-visibility-light.png` | isVisible デモ（条件付き非表示） |
| `ios-native-section-decoration-light.png` | Section 装飾デモ（style 切替） |
| `android-native-store-light.png` | Store 方式デモ |
| `android-native-dsl-light.png` | DSL 方式デモ |
| `android-native-unify-common-fields-light.png` | 共通フィールド統合デモ |
| `android-native-visibility-light.png` | isVisible デモ（条件付き非表示） |
| `android-native-section-decoration-light.png` | Section 装飾デモ（style 切替） |
| `maui-ios-unify-common-fields-light.png` | 共通フィールド統合デモ |
| `maui-ios-visibility-light.png` | isVisible デモ（条件付き非表示） |
| `maui-ios-section-decoration-light.png` | Section 装飾デモ（style 切替） |
| `maui-android-unify-common-fields-light.png` | 共通フィールド統合デモ |
| `maui-android-visibility-light.png` | isVisible デモ（条件付き非表示） |
| `maui-android-section-decoration-light.png` | Section 装飾デモ（style 切替） |

MAUI に Store / DSL 画面は無いため、MAUI の 2 実行面は 3 画面ずつ。

## 個人要素の確認

全 16 枚を開いて確認済み。写っているのはステータスバーの時刻・Wi-Fi / 電波 / 電池のアイコンと、
デモデータ (架空の Wi-Fi 名 `demoAP-0a1b2c-5` 等) のみ。端末名・アカウント・連絡先・位置情報は写っていない。
iOS のステータスバー左上には直前アプリへ戻る表示 (開発用アプリ名) が出るが、A/B 比較はステータスバーを除いて行う。

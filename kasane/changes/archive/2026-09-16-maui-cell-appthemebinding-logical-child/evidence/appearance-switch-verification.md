# 検証ホスト・Sample の外観追随の実機確認

tasks 3.2 / 4.4 の確認記録。実行環境は iOS Simulator iPhone 17 Pro (iOS 26.4) と Android Emulator Pixel 6 (API 36)。
撮影はシミュレータ / エミュレータ + Sample のデモデータで行い、個人を特定する値は写っていない。

## 1. 検証ホスト MauiHost の「外観追随ボタン」(tasks 3.2)

手順: 設定画面を表示したまま端末の外観を切り替える (iOS は `simctl ui <udid> appearance`、Android は `cmd uimode night`)。

| 確認 | 結果 |
|---|---|
| 表示中に外観を切り替えると、画面遷移なしで同じ行の `TitleColor` が描き直される | 両 OS で成立。ライト = 緑 (#FF008000)、ダーク = マゼンタ (#FFFF00FF) |
| 購読 + 再代入を外した後も、実装前と同じ描画になる | 両 OS・両外観で画素一致 (差分 0) |

| 画像 | 内容 |
|---|---|
| `mauihost-ios-light-before.png` / `mauihost-ios-light-after.png` | iOS ライト。実装前 (購読 + 再代入) と実装後 (XAML の `AppThemeBinding`) |
| `mauihost-ios-dark-before.png` / `mauihost-ios-dark-after.png` | iOS ダーク。同上。切替は設定画面を表示したまま行った |
| `mauihost-android-light-before.png` / `mauihost-android-light-after.png` | Android ライト |
| `mauihost-android-dark-before.png` / `mauihost-android-dark-after.png` | Android ダーク |

## 2. MAUI Sample のデモ画面 (tasks 4.4)

### 実装前と同じ描画

7 デモ画面 × light / dark × 2 platform の計 32 組を画素比較し、全組一致した。結果表は
[sample-appearance-pixel-diff.md](sample-appearance-pixel-diff.md)。代表として基本 Cell 7 種デモの画像を置く。

| 画像 | 内容 |
|---|---|
| `sample-ios-basiccells-light-before.png` / `-after.png` | iOS ライト |
| `sample-ios-basiccells-dark-before.png` / `-after.png` | iOS ダーク |
| `sample-android-basiccells-light-before.png` / `-after.png` | Android ライト |
| `sample-android-basiccells-dark-before.png` / `-after.png` | Android ダーク |

### 表示中の外観切替への追随

手順: 外観の選択を「システム」にして基本 Cell 7 種デモを表示し、表示したまま端末の外観を切り替える。
SettingsView の下地・separator・header 文字色と、ButtonCell「ログアウト」の `TitleColor` が同じ画面のまま描き直される。

| 画像 | 内容 |
|---|---|
| `sample-ios-buttoncell-light-live.png` / `sample-ios-buttoncell-dark-live.png` | iOS。表示中の切替後の末尾 (ログアウト行の title 色が #CC9900 / #E0B040) |
| `sample-android-buttoncell-light-live.png` / `sample-android-buttoncell-dark-live.png` | Android。同上 |

Android では端末の外観の切替が Activity の再生成を伴うため、切替の直後は list の表示位置が先頭へ戻る
(ページと入力状態は保たれる。Sample の `App` が Window を使い回す既知の経路)。色の追随はその前後で成立している。

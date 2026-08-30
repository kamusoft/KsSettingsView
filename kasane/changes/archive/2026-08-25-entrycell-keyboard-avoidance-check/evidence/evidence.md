# Evidence: entrycell-keyboard-avoidance-check

キーボード回避 (フォーカス時のせり上がり) の視覚確認証跡。確認日: 2026-08-24。

| ファイル | 環境 | 確認内容 |
|---|---|---|
| ios-bottom.png | iOS Simulator (iPhone 17 / iOS 26.5) | 入力Cell 5種デモ最下部。「EntryCell（下部配置）」セクション (メモ・署名) が画面下半分に表示される |
| ios-memo-focused.png | iOS Simulator (iPhone 17 / iOS 26.5) | 「メモ」フォーカス時にソフトウェアキーボードが表示され、コンテンツがせり上がって対象 Cell がキーボード上端より上に収まる |
| ios-sign-focused.png | iOS Simulator (iPhone 17 / iOS 26.5) | 最下部の「署名」フォーカス時もキーボード直上にせり上がる |
| android-bottom.png | Android 実機 (Pixel 系) | 同デモ最下部。「EntryCell（下部配置）」セクションが表示される |
| android-memo-focused.png | Android 実機 | 「メモ」フォーカス時に IME が表示され、対象 Cell が IME 直上にせり上がる |
| android-sign-focused.png | Android 実機 | 最下部の「署名」フォーカス時も IME 直上にせり上がる |
| maui-bottom.png | MAUI iOS ターゲット (同シミュレータ / net10.0-ios Debug) | 同デモ最下部。「EntryCell（下部配置）」セクションが表示される |
| maui-memo-focused.png | MAUI iOS ターゲット | 「メモ」フォーカス時にキーボードが表示され、対象 Cell がキーボード上端より上に収まる |
| maui-android-bottom.png | MAUI Android ターゲット (Android 実機 / net10.0-android Debug) | 同デモ最下部。「EntryCell（下部配置）」セクションが表示される |
| maui-android-memo-focused.png | MAUI Android ターゲット (Android 実機) | 「メモ」フォーカス時に IME が表示され、対象 Cell が IME 直上にせり上がる (MainActivity に WindowSoftInputMode 指定なしの構成のまま動作) |

補足:
- iOS はハードウェアキーボード接続だとソフトウェアキーボードが出ないため、Simulator の I/O 設定で無効化して確認した。
- MAUI Android の Debug APK は FastDev 方式でアセンブリが APK に含まれないため、`adb install` 単体では起動できない。`dotnet build -t:Run -f net10.0-android` でデプロイして確認した。

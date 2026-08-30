# evidence 索引: add-accessory-visibility-toggle

## 1. トグル切り替え (tasks 6.2 前半)

3 platform (MAUI は net10.0-android / net10.0-ios の 2 TFM) の Visibility デモで
Header / Footer トグルの 4 組み合わせを撮影したもの。
`<接頭辞>-01`〜`08` の連番で、奇数がトグル部、偶数が観察対象 Section D の状態。

| ファイル接頭辞 | platform | 内容 |
| --- | --- | --- |
| `ios-01`〜`ios-08` | iOS Simulator (iPhone 17 Pro / iOS 26.0.1) | header/footer トグルの ON-ON / OFF-ON / ON-OFF / OFF-OFF |
| `android-01`〜`android-08` | Android 実機 (Pixel 6a / Android 16) | 同上 |
| `maui-01`〜`maui-08` | MAUI サンプル net10.0-android / Android 実機 (Pixel 6a / Android 16) | 同上 |
| `maui-ios-01`〜`maui-ios-08` | MAUI サンプル net10.0-ios / iOS Simulator (iPhone 17 Pro / iOS 26.0.1) | 同上 |

`test-results-summary.txt` は tasks 6.1 の全テストスイート実行結果。

### MAUI の撮影 platform について

MAUI の視覚証跡は **net10.0-android (Android 実機) と net10.0-ios (iOS Simulator) の両 TFM** で取得している。

当初 net10.0-ios はサンプルアプリのパッケージング段階で、workload ピンにより選択される
.NET for iOS SDK と Xcode のバージョン不一致 (SDK 26.1.10502 の要求と Xcode 26.5) により
ビルドできず、実行時証跡が取得できていなかった (真因は環境ではなく、リポジトリに global.json が無く
親ディレクトリの古い workloadVersion ピン 10.0.101 を継承していたこと。ライブラリ本体の net10.0-ios ビルドは成功していた)。
リポジトリ直下に `global.json` (`sdk.version` 10.0.300 / `sdk.workloadVersion` 10.0.300.3 →
iOS SDK 26.5.10284) を置いて workload バージョンを固定したことで解消し、
`maui-ios-01`〜`maui-ios-08` として実行時証跡を取得した。

これにより iOS TFM 側の輸送経路 — tasks 3.3 (Binding `ApiDefinition.cs` の managed API) と
tasks 4.3 の `Platforms/iOS/KsBridgeGateway.cs` (net10.0 テストのコンパイル対象外) — は
実行時観測で裏付けられている。ビルド不可だった期間に行った静的検証も記録として残す:
生成 API は `compiled-api-definitions.xml` で `IsHeaderVisible` / `IsFooterVisible` の生成を確認済み、
gateway の変換は既存 `IsVisible` (`[Export("isVisible")]`) と完全同型のコードレビューによる。

`maui-ios-*` の撮影環境: iOS Simulator iPhone 17 Pro / iOS 26.0.1 (23A8464)、
Debug + `iossimulator-arm64` ビルドの `.app` を `xcrun simctl install` / `launch` で配備・起動したもの。

## 2. 対称化 3 件の視覚証跡 (tasks 6.2 後半)

「内容の不在 (nil または空 text) なら領域を生成しない」「高さ指定は領域を作らない」ことを、
**同一ビルド・同一画面**で対照 (内容ありなら領域が出る) と対で撮影したもの。
撮影のためサンプルの Visibility デモ画面を一時的に検証用構成へ差し替え、撮影後に元へ戻している
(サンプルのソースは撮影前の状態に完全復帰済み)。

撮影環境:
- iOS: Simulator iPhone 17 Pro / iOS 26.0 (`samples/ios/KsSettingsViewSample`, Debug)
- Android: 実機 Pixel 6a / Android 16 (`samples/android` app-debug)

| 項目 | 領域が生成されない側 | 対照 (領域が出る側) |
| --- | --- | --- |
| 1. 空 text の header / footer [iOS] | `symmetry-1-ios-empty-text-header-footer-not-rendered.png` | `symmetry-1-ios-control-nonempty-text-header-footer-rendered.png` |
| 1. 空 text の header / footer [Android] | `symmetry-1-android-empty-text-header-footer-not-rendered.png` | `symmetry-1-android-control-nonempty-text-header-footer-rendered.png` |
| 2. header 不在 + `Section.headerHeight` 40 [iOS] | `symmetry-2-ios-header-nil-with-section-headerheight40-not-rendered.png` | `symmetry-2-ios-control-header-text-with-section-headerheight40-rendered.png` |
| 3. header 不在 + `Theme.headerHeight` 40 [iOS] | `symmetry-3-ios-header-nil-with-theme-headerheight40-not-rendered.png` | `symmetry-3-ios-control-header-text-with-theme-headerheight40-rendered.png` |

上表の各画像は、下記の画面全体スクリーンショットから該当 Section を切り出したもの。

| 全体スクリーンショット | 状態 | 読み取れること |
| --- | --- | --- |
| `symmetry-ios-full-target-theme-off.png` | 対照モード OFF / Theme.headerHeight 未指定 | 検証1 (空 text) と検証2 (header 不在 + Section.headerHeight 40) のいずれも Header / Footer 領域が無い |
| `symmetry-ios-full-control-theme-off.png` | 対照モード ON / Theme.headerHeight 未指定 | 同じ Section 構成で header / footer に内容を入れると領域が出る |
| `symmetry-ios-full-target-theme-40.png` | 対照モード OFF / Theme.headerHeight = 40 | 検証3 (header 不在) に Theme の高さ指定があっても Header 領域が生成されない |
| `symmetry-ios-full-control-theme-40.png` | 対照モード ON / Theme.headerHeight = 40 | 同条件で header に内容があれば Theme 高さ 40 の Header 領域が出る (他 Section の Header も 40 に揃う) |
| `symmetry-android-full-empty-text-vs-control.png` | Android 検証画面全体 | アンカー Section → 空 text Section (領域なし) → 対照 Section (領域あり) の並び |

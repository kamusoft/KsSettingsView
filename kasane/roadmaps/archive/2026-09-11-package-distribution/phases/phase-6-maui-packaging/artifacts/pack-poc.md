# pack PoC (2026-09-02)

SDK 標準の `dotnet pack -c Release -p:Version=0.0.1-poc` を binding 2 件 → facade の順に実行した実測記録 (NuGet メタデータ未設定の状態、インクリメンタルビルド)。

| コマンド | 成否 | 所要 | 警告 |
|---|---|---|---|
| pack Android binding | 成功 | 39s | NU/MSB/XA 系ゼロ (readme 未設定の情報のみ、Gradle 10 非互換の deprecation 通知) |
| pack iOS binding | 成功 | 38s | 同上 (MSB4120 も出ず) |
| pack facade | 成功 | 9s | 同上 |

## facade (`KsSettingsView.Maui`, 353 KB)

- dependencies は TFM group ごとに分離される

| TFM group | dependencies |
|---|---|
| `net10.0` | Microsoft.Maui.Controls 10.0.70 のみ |
| `net10.0-android36.0` | `KsSettingsView.Binding.Android` 0.0.1-poc + Maui.Controls |
| `net10.0-ios26.0` | `KsSettingsView.Binding.iOS` 0.0.1-poc + Maui.Controls |

- binding への依存版は下限指定 (`version="0.0.1-poc"`、完全一致 `[..]` ではない)
- lib は 3 TFM とも dll + xml doc。binding の dll・aar・xcframework の混入なし (全 10 エントリ)

## Android binding (729 KB)

- AndroidX 系 14 本が dependencies に出力 (`Xamarin.AndroidX.Lifecycle.LiveData` 2.11.0.1 を含む)
- `lib/net10.0-android36.0/` 直下に dll + `kssettingsview-release.aar` (607 KB) + `kssettingsview-bridge-release.aar` (90 KB)

## iOS binding (2.5 MB)

- `lib/net10.0-ios26.0/KsSettingsView.Binding.iOS.resources.zip` に `KsSettingsViewBridgeiOS.xcframework` (device `ios-arm64` + simulator `ios-arm64_x86_64-simulator`、_CodeSignature 同梱)
- zip 内 `manifest` の `IdentityWithoutPathSeparatorSuffix` にビルドマシンの絶対パス (obj/Release 配下の xcframework パス) が記録される。Info.plist・Mach-O には絶対パスなし
- xml doc なし (binding は公開契約ではないため揃えない)。dependencies は空 group

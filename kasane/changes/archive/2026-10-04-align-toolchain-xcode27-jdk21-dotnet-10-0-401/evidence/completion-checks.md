# 完了条件の実測結果 (2026-10-04)

環境: Xcode 27.0 (27A266a) / iOS 27.0 Simulator、JDK 21.0.12.1、.NET SDK 10.0.401 / workload 10.0.401.1。

| 完了条件 | 結果 | 確かめた主体 |
|---|---|---|
| `android/` の `./gradlew test --rerun-tasks` | 3174 件 (1587 × 2 variant) / 失敗 0 | 実装・レビュー 1 周目 |
| 生成 class の major version | 本体・Bridge・Sample ともすべて 61 | 実装・レビュー 1 周目 |
| `samples/android` の `:app:assembleDebug` | 成功 | 実装・レビュー 1 周目 |
| iOS Native の全テスト | 1201 件 / 失敗 0 (テスト修正後) | 実装・レビュー 2 周目 |
| MAUI facade のテスト | 624 件 / 失敗 0 | 実装・レビュー 1 周目 |
| MAUI binding・facade の Release ビルド (4 本) | 0 エラー | 実装 |
| pack した nupkg の TFM group | `net10.0-ios26.0` / `net10.0-android36.0` のまま | 実装 |
| 消費者検証 3 本 (`verification/<platform>/build-consumer.sh`) | 3 本とも exit 0 (MAUI は XA4301 検出なし) | 実装 |

「SDK 更新時に再検証する箇所」の表 (実装が確認):

| 割り込み先 | 結果 |
|---|---|
| `_BuildXcodeProjects` / `_SanitizeNativeReferences` | クリーン状態から xcframework が生成され、nupkg に device / simulator 両スライスが入る |
| `_GetBuildXcodeProjectsInputs` / `_XcbInputs` | 入力 142 件。生成物は含まれず、Swift 1 本の更新で作り直される |
| `_CategorizeAndroidLibraries` / `_ResolveLibraryProjectImports` | クリーン状態から aar が生成され取り込まれる |
| `_IncludeAarInNuGetPackage` / `_CreateAar` | 自 assembly 用 aar は nupkg に入らず、中身は推移依存の native ライブラリだけ |
| `SupportedOSPlatformVersion` の SDK 既定値 | iOS 26.0 / Android 21.0 (変化なし) |

未検証 (push・GUI・実行ホストが要るもの):

- CI の `xcode-27` ランナーでの Xcode 選択と、develop への push での lint + 本体検証 3 本
- 消費者検証とリリースの CI 経路 (`main` 宛て PR とリリースの dry-run でしか走らない)
- Android Studio の sync、`maui/` の検証ホストでの実行時の疎通

# Exploration: Binding csproj 標準方式の可否 (ksn-dual-research 記録)

- 実施日: 2026-08-05
- 経緯: 実装レビュー (second-opinion-003 Major) で Binding csproj の Exec 方式が proposal の「XcodeProject / AndroidGradleProject 形式」から無記録逸脱していると指摘 → オーナーが「SDK 制約」主張の裏取りを ksn-dual-research で指示
- 調査体制: 相方 codex (2ラウンド) + ホスト側 ksn-researcher (実験含む)。参考プロジェクト: ../AdMobMediation.Maui (オーナー提供)

## 並走調査結果: 「SDK 制約により標準アイテムが使えない」という実装側主張の検証

判定: 🟡 部分的に一致 (Android の扱いに要判断あり)

### 双方一致した結論

1. **iOS の主張は誤り。標準 `XcodeProject` 方式は成立する**
   - ホスト側: 当該 `ios/binding/KsSettingsViewBridge.xcodeproj` に SDK と同一引数 (署名フラグなし) で archive を実験 → framework は `Products/Library/Frameworks` へ正しく install され、2スライス xcframework 生成まで成功
   - 相方: AdMobMediation.Maui の net10.0-ios 4プロジェクト + 本リポジトリ spike の計5件が、`CODE_SIGNING_ALLOWED=NO` なし・標準 `XcodeProject` + `CreateNativeReference=false` + 手動 `NativeReference` の「最小 workaround」構成で build/pack/配布まで成立している実績を確認
   - 「SDK が署名設定を渡せない」こと自体は事実 (`CreateXcArchive` に拡張口なし) だが、「だから framework が install されない」という因果が不成立。仮に必要でも xcodeproj の build settings に書けば足りる
   - 当初失敗の真因は、修正済みの **scheme 衝突** (SwiftPM product と同名解決) だった可能性が高い (ホスト側: 中 / 相方: 78%)
2. **Android の buildDir 束ね問題は実在** (両者とも SDK 実装から確認、確度 98-99%)
   - init script が `rootProject.allprojects` の buildDirectory を単一パスへ変更する
   - ホスト側の実測: project 依存を持つモジュール (`:ks-settingsview-ui` / `:ks-settingsview-bridge`) は **aar glob 以前に Gradle validation エラーでビルド失敗** (葉モジュール `:core` のみ成功)。`ModuleName` 指定でも依存連鎖がある限り回避不可
   - AdMobMediation.Maui は全案件が単一モジュール構成のため反例にならない (両者一致)
3. **pack 経路は公式アイテム (`NativeReference` / `AndroidLibrary`) で標準方式と共通**。現行 Exec 方式でも `dotnet pack` は成功する (ホスト側実測: 両 OS の nupkg に xcframework / aar が正しく同梱)。ただし clean consumer での restore・実行時検証は未実施 (両者一致)

### 発見事項 (出典明記)

- (ホスト側のみ) 標準 `XcodeProject` の増分ビルド入力は `ios/binding/` 配下のみを走査し、Core/UI の実体 `ios/Sources/` (89ファイル) が漏れる — 標準方式へ戻す場合は入力範囲の手当てが必要。現行 csproj の `KsBridgeNativeSource` はこれを明示的に拾っており、この一点だけ現行実装に実質的利点がある
- (ホスト側のみ) `ios/binding/build` 372MB / 1321ファイルが標準方式の glob 対象に入るため、移行時は build/ / DerivedData/ の撤去が必要
- (相方のみ) AdMobMediation.Maui の CI は標準方式で build → `pack --no-build` → feed push まで自動化しており、NuGet 復元済みパッケージの内容 (dll + resources.zip + xcframework / dll + aar 群) も確認できる
- (ホスト側のみ) dotnet/android の init script が multi-project builds を壊す件は upstream issue として報告する価値がある (公式ドキュメントは `ModuleName` を module or subproject と説明しており、実装が追いついていない)

### 推奨アクション

- **iOS**: 標準 `XcodeProject` 方式へ戻す (AdMob / spike と同じ「標準アイテム + CreateNativeReference=false + 手動 NativeReference」構成)。移行時は増分ビルド入力範囲と build/ 撤去を手当てする — 両者一致
- **Android**: 標準方式は現行 Gradle 構成 (project 依存を持つ複数モジュール) には載らない (ホスト側実測)。選択肢: (A) 実験結果を根拠に deviation 記録して現方式維持 / (B) binding 専用の単一 wrapper Gradle project 化など Gradle 側の再構成 (未検証、相方が列挙) — **要オーナー判断**

### 未解決の論点 (要ユーザー判断)

- Android の Gradle 側回避策 (fat-aar 統合・モジュール統合・明示 dependsOn・binding 専用 wrapper) はいずれも未検証。相方は回避可能性を 60% と見るが、ホスト側は「native 側にビルドツール都合を持ち込む費用対効果はオーナー判断」と評価。両者とも「試せば載る可能性はあるが、native 構成の変更が前提」という点では整合

> 相方: codex / 調査範囲: リポジトリ全体 + AdMobMediation.Maui + 公式ドキュメント / ラウンド数: 2
> 証跡: ~/.kasane/counterpart-bridge/responses/dual-binding-standard-items-{1,2}.md

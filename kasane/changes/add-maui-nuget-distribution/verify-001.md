# 一致検証: add-maui-nuget-distribution (001 回目)

**判定**: VALID

## 検証範囲

- デルタスペック: `specs/maui-nuget-distribution/spec.md` (ADDED 7 Requirement / 15 Scenario)
- 対象差分: コミット `55bd972` から作業ツリーまで (コミット `abf6e0a` を含む)。コミット `a5ba445` (phase-8 agenda の追記) は対象外
- 未追跡ファイル: `assets/icon.png` / `maui/Directory.Build.props` / `maui/Directory.Build.targets` / `maui/Directory.Packages.props` / `maui/KsSettingsView.Maui/buildTransitive/`
- 合意済み差分: `deviation.md` (9 項目) を違反としない
- 既知の未対応事項 (オーナー判断待ち): アイコン帰属表示・XA4301・NU1507・binding の xml doc 同梱 — 判定に用いない

> コンテキストパッケージは「13 Scenario」としていたが、spec 本文の Scenario 見出しは 15 件ある (R1 2 / R2 3 / R3 2 / R4 1 / R5 4 / R6 2 / R7 1)。本検証は 15 件すべてを対象にした。

## 独立に再実行した検証

| 実行内容 | 結果 |
|---|---|
| `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 合格 516 / 失敗 0 / スキップ 0 (証跡の基準 516 件と一致) |
| 3 パッケージの Release pack を Version 未指定と `-p:Version=0.1.0-alpha.1` の 2 回 (出力は作業スクラッチ) | 6 nupkg + 6 snupkg 生成。既定 `0.0.0-dev` / 注入 `0.1.0-alpha.1` を 3 パッケージとも確認 |
| 3 nupkg の nuspec と同梱物の展開検査 | 後述の各 Scenario 欄のとおり証跡と一致 |
| iOS binding の `resources.zip` 内 xcframework の `Info.plist` | `LibraryIdentifier` が `ios-arm64` と `ios-arm64_x86_64-simulator` の 2 件 |
| `IsPackable` の評価 (テスト 1 件 + 検証ホスト 3 件) | 4 件とも `false` |
| `SupportedOSPlatformVersion` の評価 (facade 2 TFM / binding 2 件 / 検証ホスト 3 件、計 8 通り) | Android 29.0 / iOS 16.0。`maui/` 配下の csproj 8 箇所すべてが定数参照で数値の直書きは 0 件 |
| ガードの発火 (facade を `-p:SupportedOSPlatformVersion` で上書きし `-t:CoreCompile`) | ios 15.0 / android 21 で `KSSV0001`、`net10.0` では 0 件 |
| 旧名前空間の残存 grep (`namespace` / `using` / `clr-namespace:`) | `maui/` `samples/` に 0 件 (`skills/` の残存は本 change の Non-Goal) |
| nupkg 同梱 README とリポジトリ `README.md` の diff | 完全一致。相対パス参照 0 件、`AddKsSettingsView()` の `MauiProgram` 例を含む |

消費者検証 (Scenario のうち利用者アプリを要するもの) は再現せず、`evidence/consumer-verification.txt` の記述と提出コードの対応で判定した。同証跡が示す Package ID / version / 依存・ガードの文面・xcframework 構成は、上記で再現した pack 成果物および `maui/KsSettingsView.Maui/buildTransitive/KsSettingsView.Maui.targets` の実装と食い違いがない。

## 対応表

### Requirement: facade の公開名前空間

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 改名後の facade のビルドとテスト | `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` (`RootNamespace` = `KsSettingsView`、`AssemblyName` は据え置き)、facade 全 `.cs` の `namespace` 宣言、`maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` (`RootNamespace` = `KsSettingsView.Tests`) | `maui/KsSettingsView.Maui.Tests/` 全 516 件 (本検証で再実行し全件成功、改名前と同数)、`evidence/namespace-rename-build-and-test.txt`:改名前後のテスト / 3 TFM ビルド / 残存宣言の 2 軸 grep | ✅ 一致 |
| 利用側の XAML と C# からの参照 | `maui/tests/KsSettingsView.MauiHost/SettingsPage.xaml:4`、同 `SettingsPage.xaml.cs` / `MauiProgram.cs`、`samples/maui/KsSettingsView.Sample.Maui/` の XAML 9 枚と C# 5 件 | `evidence/namespace-rename-build-and-test.txt`:利用側のビルド / MauiHost 起動確認 (5 手順 × 両 OS)、`evidence/mauihost-ios-revisit-restored.png` / `evidence/mauihost-android-revisit-restored.png` | ⚠️ deviation 記録済み (`SwitchCell` / `EntryCell` の CS0104。deviation.md 第 6・7 項、`evidence/consumer-verification.txt` 8 節で両型を実測) |

### Requirement: パッケージの共通メタデータと版集約

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| nuspec のメタデータと Version の既定・注入 | `maui/Directory.Build.props` (Authors / Company / Copyright / MIT / URL 群 / `Version` `0.0.0-dev` / SourceLink / snupkg / `PackageIcon`)、`maui/Directory.Build.targets:14` (icon の同梱アイテム)、`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:31` (`PackageReadmeFile`) | `evidence/pack-release-inspection.txt`。本検証で再現: 3 nupkg の nuspec が authors `kamusoft` / license expression MIT / projectUrl / repository(type=git) / icon / copyright を持ち、`readme` は facade のみ。Version 未指定 → `0.0.0-dev`、`-p:Version` → `0.1.0-alpha.1`、`.snupkg` も 3 件同時生成 | ⚠️ deviation 記録済み (icon 同梱アイテムの置き場が props → targets。deviation.md 第 1 項) |
| pack 対象の限定 | `maui/Directory.Build.props:29` (`IsPackable` 既定 false)、`maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` (自前の `IsPackable=false` を削除し既定へ委譲) | 本検証で `-getProperty:IsPackable` を評価 — テスト / MauiHost / IntegrationHost.Android / IntegrationHost.iOS の 4 件とも `false` | ✅ 一致 |
| 版集約後の解決の不変 | `maui/Directory.Packages.props` (`ManagePackageVersionsCentrally`、18 版の集約)、各 csproj から `Version` 属性を除去 | `evidence/cpm-restore-invariance.txt`:7 プロジェクトの `project.assets.json` 抽出が導入前と完全一致、NU1608 / NU1107 0 件 | ✅ 一致 (新規に出る NU1507 は作業機のソース構成由来で既知事項) |

### Requirement: 3 パッケージの構成と内容

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 3 パッケージのローカル pack | `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:27-31` (`IsPackable` / `PackageId` / Description / Tags)、同 `:69,74` (platform TFM 条件の `ProjectReference`) | `evidence/pack-release-inspection.txt`。本検証で再現: facade の nuspec は `net10.0` に binding 依存なし、`net10.0-android36.0` に `KsSettingsView.Binding.Android 0.1.0-alpha.1`、`net10.0-ios26.0` に `KsSettingsView.Binding.iOS 0.1.0-alpha.1`。同梱物に binding の dll / `.xcframework` / `kssettingsview*.aar` は無い | ⚠️ deviation 記録済み (`lib/net10.0-android36.0/KsSettingsView.Maui.aar` は SDK 生成。deviation.md 第 2 項) |
| binding パッケージの同梱物と説明 | `maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj:23-24` と `AndroidLibrary` / `PackageReference` 群、`maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj:25-26` | `evidence/pack-release-inspection.txt`。本検証で再現: Android binding の `lib/` に `kssettingsview-release.aar` / `kssettingsview-bridge-release.aar`、nuspec 依存は AndroidX 系 14 本 (`Xamarin.AndroidX.Lifecycle.LiveData 2.11.0.1` を含む)。iOS binding の resource package 内 xcframework は `ios-arm64` と `ios-arm64_x86_64-simulator` の 2 スライス。両 description に "do not reference this package directly" | ⚠️ deviation 記録済み (SDK 生成の `KsSettingsView.Binding.Android.aar` を加えて aar 3 本。deviation.md 第 8 項) |

### Requirement: 消費者からの導入

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| ローカルフィードからの restore と Release ビルド | facade の TFM 別依存 (上記 csproj)、`maui/Directory.Packages.props` の `Xamarin.AndroidX.Lifecycle.LiveData 2.11.0.1` と binding の `PackageReference` | `evidence/consumer-verification.txt` 0〜2 節:restore 警告 0 件、LiveData family 3 件が 2.11.0.1、binding 2 件の推移解決、3 パッケージの取得元が `.nupkg.metadata` の source でローカルフィード、両 OS Release ビルド成功と成果物内の facade / binding アセンブリ | ⚠️ deviation 記録済み (`nuget.config` に nuget.org を残置。deviation.md 第 4 項) |

証跡と提出コードの対応: 証跡が挙げる binding の 14 依存と版は、本検証で再現した Android binding の nuspec と全件一致する。facade の TFM 別依存も再現結果と一致する。

### Requirement: 最低 OS 版のビルド時ガード

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 要件未満の利用者アプリ | `maui/KsSettingsView.Maui/buildTransitive/KsSettingsView.Maui.targets` (`BeforeTargets="CoreCompile"`、`VersionLessThan`、`KSSV0001`)、`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:38-39` (`buildTransitive/` の同梱) | `evidence/consumer-verification.txt` 4 節:android 21 / ios 15.0 で `KSSV0001`、`XAAMM*` 0 件。対照実験 (`-p:KsSettingsViewMinAndroidApi=1`) でガードを黙らせると `XAAMM0000` が出ることを示し、ガードが manifest merger より先に働くことを対照で立証。本検証でもリポジトリ内 facade への上書きビルドで同一文面の `KSSV0001` を再現 | ⚠️ deviation 記録済み (iOS は未設定時に SDK 既定 26.x が入り発火しない。deviation.md 第 5 項) |
| 要件を満たす利用者アプリと検証ホスト | 上記 targets の要件充足時の無出力、`maui/Directory.Build.targets:7` (リポジトリ内への同 targets の import) | `evidence/os-version-guard.txt`:MauiHost 2 TFM / IntegrationHost 2 件のビルド成功・`KSSV0001` なし。`evidence/consumer-verification.txt` 1〜3 節:要件を満たす利用者アプリの両 OS ビルド成功 | ✅ 一致 |
| 非 platform TFM と outer build ではガードが動かない | 上記 targets の `Condition`(`'$(TargetFramework)' != ''` かつ `TargetPlatformIdentifier` が android / ios) | `evidence/consumer-verification.txt` 5 節:`net10.0` のみのクラスライブラリ 0 件、複数 TFM の outer build 由来 0 件・`net10.0` inner build 0 件、`net10.0-android` の inner build でのみ `KSSV0001`。本検証でも facade の `net10.0` で `KSSV0001` 0 件を再現 | ✅ 一致 |
| 要件の宣言元の一致 | `maui/KsSettingsView.Maui/buildTransitive/KsSettingsView.Maui.props` (`KsSettingsViewMinAndroidApi` 29 / `KsSettingsViewMinIOSVersion` 16.0)、`maui/Directory.Build.props:12` の import、facade / binding 2 件 / 検証ホスト 3 件の csproj (計 8 箇所が定数参照) | `evidence/os-version-guard.txt`。本検証で再現: 8 通りの評価値が Android 29.0 / iOS 16.0、`maui/` 配下 csproj の直書きは 0 件 | ⚠️ deviation 記録済み (Android が `29.0` に正規化。deviation.md 第 3 項) |

### Requirement: package README の表示

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| README の同梱と画像参照 | `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:31,36` (`PackageReadmeFile` とルート README の同梱)、`README.md:23-24` / `README_ja.md:23-24` (画像 4 件を `raw.githubusercontent.com` の絶対 URL 化)、両 README の `:126` (xmlns を `clr-namespace:KsSettingsView`) | `evidence/readme-image-urls.txt`:画像 4 URL が 200 / `image/png`、リンク参照も絶対 URL 化 (17 URL が 200)、相対パス参照 0 件。本検証で再現: nupkg ルートの `README.md` がリポジトリ `README.md` と完全一致、nuspec の `readme=README.md`、相対パス参照 0 件、`clr-namespace:KsSettingsView.Maui` の残存 0 件 | ✅ 一致 |
| README の例による消費者ビルド | `README.md` / `README_ja.md` の `:123-135` (XAML 最小例) と `:139-158` (`MauiProgram` 登録例) | `evidence/consumer-verification.txt` 7-2 節:nupkg から取り出した README の XAML / C# コードブロックを 1 文字も編集せず写して両 OS の Release ビルド成功 | ✅ 一致 |

### Requirement: 公開識別子の規範への NuGet 座標の記載

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 規範文書の記載 | `kasane/handbook/cross/public-identifiers.md` (識別子表への NuGet Package ID 行、「NuGet の配布座標」節で名前空間との非対称を説明、`timestamp` 更新)、`kasane/decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md` (2026-09-02 の日付付き追記と出典行) | 文書そのもの。ADR-0018 の配布先表のセルは無変更 (diff は表の後方への追加のみ) | ✅ 一致 |

## 追加検査

- **tasks.md の虚偽チェック**: 全 24 タスクが `[x]`。対応表と突き合わせ、実装・証跡の裏付けを持たない完了マークは 0 件。tasks 1.5 (MauiHost 起動)・6.1〜6.5 (消費者検証) は証跡ファイルで、2.4 / 2.5 / 4.2 / 4.3 は本検証の再現で裏付けを確認した
- **逆流検査**: `proposal.md` / `design.md` / `specs/maui-nuget-distribution/spec.md` はいずれも `55bd972` が最終更新で、作業ツリーに未コミットの変更がない (実装期間中の書き換えなし)
- **未記録乖離**: ❌ は 0 件。`deviation.md` の 9 項目はいずれも対応する Scenario を持ち、内容が実装・証跡と一致する
- **UI 変更**: 本 change に `ui/` アーティファクトはなく、UI の見た目に触れる変更もない (MauiHost の起動確認は改名の疎通確認としての静止画)
- **テスト全件成功**: 516 / 516 (本検証で実行)

## 観測 (判定に影響しないもの)

1. **Scenario に対応しない差分の記録先**: 次の 3 件は Scenario にも `deviation.md` の `[付随修正]` 行にも現れないが、いずれも `review-001.md` のアクションプラン (項目 1 / 3 / 6) に由来が記録されており、追跡は可能である。`deviation.md` に `[付随修正]` として 1 行ずつ残すと蒸留時の追跡が一段楽になる。
   - `README.md` / `README_ja.md` のリンク参照 12 箇所の絶対 URL 化 (Requirement「package README の表示」の意図に沿う拡張。`evidence/readme-image-urls.txt` に記録あり)
   - `kasane/config.yaml` の `lint.comment-policy.ext` に `.csproj` / `.props` / `.targets` を追加
   - `kasane/lessons/inbox/` の 2 ファイル (ksn-lesson の捕捉。変更フローの外側の記録)
2. **証跡の員数表記**: `evidence/pack-release-inspection.txt` は facade の同梱物を「全 11 エントリ」としているが、続く列挙は 12 件で、本検証の展開結果も 12 件である (nuspec / README.md / icon.png / buildTransitive 2 件 / `lib/net10.0` 2 件 / `lib/net10.0-ios26.0` 2 件 / `lib/net10.0-android36.0` 3 件)。列挙の中身は実測と一致するため判定には影響しないが、後続フェーズが数え上げの根拠に使う値であれば「12」に直しておくとよい。
3. **既知の未対応事項**: アイコンの帰属表示 (`deviation.md` 第 9 項の「公開前の宿題」)、消費者 Android ビルドの XA4301 4 件、CPM 環境の NU1507、iOS binding パッケージに xml doc が入らないこと。いずれもコンテキストパッケージで判定に用いないと指定された事項であり、本検証でも INVALID の根拠にしていない。

## 判定

**VALID** — 15 Scenario すべてが「✅ 一致」(9 件) または「⚠️ deviation 記録済み」(6 件) で、未記録の欠落・乖離は 0 件。tasks.md の虚偽チェックなし、足場アーティファクトへの逆流なし、テスト 516 件全件成功。

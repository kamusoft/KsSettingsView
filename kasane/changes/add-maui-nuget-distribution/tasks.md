# Tasks: add-maui-nuget-distribution

## 1. 名前空間改名 (独立コミット)

- [x] 1.1 facade の `RootNamespace` と全 namespace 宣言 (`KsSettingsView.Maui` / `.Internals` / `.Handlers`) を `KsSettingsView` 系に改め、facade 内の using を追随する。`AssemblyName` と `InternalsVisibleTo` は変えない (→ Requirement: facade の公開名前空間)
- [x] 1.2 テストプロジェクトの namespace 宣言 (`KsSettingsView.Maui.Tests` / `.Fakes` / `.Support`) を `KsSettingsView.Tests` 系に改め、facade への using を追随する。`AssemblyName` は変えない (→ Requirement: facade の公開名前空間)
- [x] 1.3 MauiHost と `samples/maui` の Sample の using と XAML xmlns を `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui` に追随する (→ Scenario: 利用側の XAML と C# からの参照)
- [x] 1.4 facade を 3 TFM でビルドし、`KsSettingsView.Maui.Tests` を実行して改名前と同じ件数 (実行件数を併記、handbook cross/test-execution.md) がすべて成功することを確認する。`KsSettingsView.Maui*` の宣言が残っていないことを grep で確認する (→ Scenario: 改名後の facade のビルドとテスト)
- [x] 1.5 MauiHost を両 OS で起動し既存の固定シナリオの成立を確認する (handbook maui/integration-host-verification.md) (→ Scenario: 利用側の XAML と C# からの参照)

## 2. 共通メタデータと版集約

- [ ] 2.1 `assets/icon.png` を取り込む (AiForms.Maui.SettingsView の `images/icon.png`、300×300 PNG) (→ Requirement: パッケージの共通メタデータと版集約)
- [ ] 2.2 `maui/Directory.Build.props` を作る: design Decision 2 の表のとおり (Authors / Company / Copyright / MIT / URL 群 / Version `0.0.0-dev` / IsPackable 既定 false / SourceLink + snupkg / PackageIcon の同梱アイテム)。同梱 props (4.1) の import を含める (→ Requirement: パッケージの共通メタデータと版集約)
- [ ] 2.3 `maui/Directory.Packages.props` を作り、`Microsoft.Maui.Controls`・AndroidX / Kotlin / Material / Compose 系 14 本・テスト系 3 本の版を集約する。binding csproj の版整合コメントを `PackageVersion` の隣へ移し、各 csproj の `Version` 属性を外す (→ Requirement: パッケージの共通メタデータと版集約)
- [ ] 2.4 facade・binding 2 件・テスト・MauiHost・IntegrationHost 2 件を restore し、`project.assets.json` の解決版が導入前と同一で NU1608 / NU1107 が 0 件であることを確認する (→ Scenario: 版集約後の解決の不変)
- [ ] 2.5 テストと検証ホストに pack を実行して nupkg が生成されないことを確認する (→ Scenario: pack 対象の限定)

## 3. pack 設定

- [ ] 3.1 facade csproj: `IsPackable=true`、`PackageId` `KsSettingsView.Maui`、Description / PackageTags (design Decision 4)、`PackageReadmeFile` にルート `README.md` を同梱 (→ Requirement: 3 パッケージの構成と内容 / package README の表示)
- [ ] 3.2 binding 2 件の csproj: `IsPackable=true` と Description (design Decision 4)。`PackageId` は既定に任せる (→ Requirement: 3 パッケージの構成と内容)
- [ ] 3.3 binding 2 件と facade を Release で pack し (Version 未指定と `-p:Version=0.1.0-alpha.1` の 2 回)、3 パッケージの nuspec (version / authors / license / URL / icon / TFM 別依存。readme は facade のみ) と同梱物 (facade に native 混入なし、aar 2 本、xcframework 両スライス、snupkg) を検査して evidence/ に残す (→ Scenario: nuspec のメタデータと Version の既定・注入 / 3 パッケージのローカル pack / binding パッケージの同梱物と説明 / README の同梱と画像参照)

## 4. 最低 OS 版のビルド時ガード

- [ ] 4.1 facade に `buildTransitive/KsSettingsView.Maui.props` (要件の定数のみ) と `buildTransitive/KsSettingsView.Maui.targets` (`TargetPlatformIdentifier` が android / ios の inner build でのみ有効、`VersionLessThan` で比較、未設定または要件未満なら要件と設定方法を示す error を `CoreCompile` の前で出す) を追加し、facade パッケージに同梱する (design Decision 5) (→ Requirement: 最低 OS 版のビルド時ガード)
- [ ] 4.2 `maui/Directory.Build.props` から同梱 props を import し、facade・binding・検証ホストの csproj は既存の TFM 条件を残して `SupportedOSPlatformVersion` の値を定数参照に置き換える。`maui/Directory.Build.targets` から同梱 targets を import する。TFM ごとに `-getProperty:SupportedOSPlatformVersion` で評価値を確認する (→ Scenario: 要件の宣言元の一致)
- [ ] 4.3 MauiHost / IntegrationHost をビルドしてガード由来のエラー・警告が出ないことを確認する (→ Scenario: 要件を満たす利用者アプリと検証ホスト)

## 5. README の画像参照

- [ ] 5.1 `README.md` / `README_ja.md` のスクリーンショット参照を絶対 URL (`https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/...`) に改め (両枚同時)、各 URL の取得成功と画像の Content-Type を確認して evidence/ に残す (→ Scenario: README の同梱と画像参照)
- [ ] 5.2 両 README の MAUI 最小例の xmlns を `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui` に、`using` を `KsSettingsView` に追随させる (両枚同時。互換情報の追記は docs-refresh に残す) (→ Scenario: README の同梱と画像参照)

## 6. 消費者検証 (一時プロジェクト、リポジトリに残さない)

- [ ] 6.1 3 パッケージのローカルフォルダフィードを作り、本リポジトリの `global.json` を複製して SDK / workload を固定し、隔離した packages path (`RestorePackagesPath`) とフィード限定の `nuget.config` を用意した素の MAUI アプリ (`Microsoft.Maui.Controls` 10.0.70・Android 29・iOS 16.0) に facade の PackageReference 1 行を足して restore し、警告 0 件・LiveData family 2.11.0.1・binding 2 件の推移解決と取得元 (source / version) を `project.assets.json` で確認する (→ Scenario: ローカルフィードからの restore と Release ビルド)
- [ ] 6.2 同アプリで facade の公開型を参照した状態の Android / iOS (simulator) Release ビルドが成功し、成果物に facade と binding のアセンブリが残ることを確認する (→ Scenario: ローカルフィードからの restore と Release ビルド)
- [ ] 6.2b 同アプリに同梱 README の MAUI 最小例 (XAML ページと `MauiProgram` の登録) をそのまま写して両 OS のビルドが成功することを確認する (→ Scenario: README の例による消費者ビルド)
- [ ] 6.3 同アプリの `SupportedOSPlatformVersion` を Android 21 / iOS 15.0 に下げ、両 TFM のビルドがガードのエラー (要件と設定方法の文面) で失敗し、Android では manifest merger のエラーより先に出ることを確認する。未設定の場合も確認する (→ Scenario: 要件未満の利用者アプリ)
- [ ] 6.4 facade を参照する `net10.0` のみのクラスライブラリと、複数 TFM (`net10.0;net10.0-android;net10.0-ios`、最低 OS 版未設定) のプロジェクトを用意し、クラスライブラリのビルド・複数 TFM の outer build・`net10.0` inner build でガードの診断が出ず、platform inner build でだけエラーになることを確認する (→ Scenario: 非 platform TFM と outer build ではガードが動かない)
- [ ] 6.5 6.1〜6.4 (6.2b 含む) の証跡 (restore / build ログの要点、assets の該当行、ガードのエラー文面) を evidence/ に残す (ローカル絶対パスは置換)

## 7. 規範の追随

- [ ] 7.1 `kasane/handbook/cross/public-identifiers.md` に NuGet Package ID 3 件の行と、名前空間 `KsSettingsView` と Package ID `KsSettingsView.Maui` の非対称の説明を追加する (→ Scenario: 規範文書の記載)
- [ ] 7.2 cross/ADR-0018 に日付付き追記で MAUI の Package ID (`KsSettingsView.Maui`、maui/ADR-0025) を注記する — 表のセルは書き換えない (→ Scenario: 規範文書の記載)

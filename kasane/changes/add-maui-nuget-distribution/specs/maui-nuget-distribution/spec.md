# maui-nuget-distribution デルタスペック

## ADDED Requirements

### Requirement: facade の公開名前空間

MAUI facade (アセンブリ `KsSettingsView.Maui`) の公開型は名前空間 `KsSettingsView` (配下 `KsSettingsView.Internals` / `KsSettingsView.Handlers`) に属する SHALL。XAML からは `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui` で参照できる SHALL。アセンブリ名は `KsSettingsView.Maui` のまま維持され、binding の名前空間 `KsSettingsView.Bridge` は変わらない SHALL。テストプロジェクトの自前名前空間は `KsSettingsView.Tests` (配下 `.Fakes` / `.Support`) とし、アセンブリ名 `KsSettingsView.Maui.Tests` は維持される SHALL。

#### Scenario: 改名後の facade のビルドとテスト

- **GIVEN** 改名後の `maui/`
- **WHEN** facade を net10.0 / net10.0-ios / net10.0-android でビルドし、`KsSettingsView.Maui.Tests` を実行する
- **THEN** 3 TFM ともビルドが成功し、名前空間 `KsSettingsView.Maui*` の宣言が facade とテストに残っておらず、テストは改名前と同じ件数が実行されすべて成功する

#### Scenario: 利用側の XAML と C# からの参照

- **GIVEN** 改名後の MauiHost と `samples/maui` の Sample
- **WHEN** 両アプリを iOS / Android 向けにビルドし、MauiHost を起動する
- **THEN** xmlns `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui` と `using KsSettingsView;` で参照が解決してビルドが成功し、MauiHost の既存の固定シナリオが両 OS で成立する

### Requirement: パッケージの共通メタデータと版集約

`maui/` 配下の全プロジェクトは `maui/Directory.Build.props` の共通メタデータ (Authors `kamusoft`、Company `kamusoft LLC`、MIT の license expression、リポジトリ URL、Version 既定値 `0.0.0-dev`、SourceLink、symbol package `snupkg`、アイコン) を継承する SHALL。pack の対象は facade と binding 2 件のみで、テストプロジェクトと検証ホストは pack 対象にならない SHALL。PackageReference の版は `maui/Directory.Packages.props` に集約され、各 csproj は版を持たない SHALL。`samples/maui` の Sample はこの集約の対象外である SHALL。

#### Scenario: nuspec のメタデータと Version の既定・注入

- **GIVEN** props を導入した `maui/`
- **WHEN** facade と binding 2 件を `-p:Version` なしで pack し、次に `-p:Version=0.1.0-alpha.1` で pack する
- **THEN** 3 パッケージとも前者の nuspec は version `0.0.0-dev`、後者は `0.1.0-alpha.1` になり、いずれも authors `kamusoft`・license expression `MIT`・repository URL・projectUrl・icon を含み、`.snupkg` が併せて生成される。readme は facade の nuspec にだけ存在する

#### Scenario: pack 対象の限定

- **GIVEN** props を導入した `maui/`
- **WHEN** `KsSettingsView.Maui.Tests` と `maui/tests/` の各検証ホストに対して pack を実行する
- **THEN** いずれも nupkg を生成しない (IsPackable が false として扱われる)

#### Scenario: 版集約後の解決の不変

- **GIVEN** CPM を導入した `maui/`
- **WHEN** facade・binding 2 件・テスト・MauiHost・IntegrationHost 2 件を restore する
- **THEN** 各プロジェクトの `project.assets.json` で `Microsoft.Maui.Controls` (10.0.70) と AndroidX 系の解決版が導入前と同一で、NU1608 / NU1107 は 0 件である

### Requirement: 3 パッケージの構成と内容

facade は Package ID `KsSettingsView.Maui` として pack でき、TFM group ごとの依存に `Microsoft.Maui.Controls` と、platform TFM でのみ対応する binding パッケージ (`KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android`) を含む SHALL。facade のパッケージに binding のアセンブリや native 成果物は含まれない SHALL。Android binding は aar 2 本 (`kssettingsview` / `kssettingsview-bridge`) と Gradle 側の実行時依存に対応する AndroidX 系の依存 (`Xamarin.AndroidX.Lifecycle.LiveData` 2.11.0.1 を含む) を持つ SHALL。iOS binding は device と simulator 両スライスを含む xcframework の binding resource package を持つ SHALL。binding の Description は facade から推移的に参照されるものであり直接参照しない旨を英語で含む SHALL。

#### Scenario: 3 パッケージのローカル pack

- **GIVEN** pack 設定を導入した `maui/`
- **WHEN** binding 2 件と facade を Release で同じ Version を指定して pack する
- **THEN** 3 つの nupkg が生成され、facade の nuspec は `net10.0` に binding 依存を持たず、`net10.0-android*` に `KsSettingsView.Binding.Android`、`net10.0-ios*` に `KsSettingsView.Binding.iOS` を同じ version で持ち、facade の nupkg に `.aar` / `.xcframework` / binding の dll が含まれない

#### Scenario: binding パッケージの同梱物と説明

- **GIVEN** 上記の nupkg
- **WHEN** binding 2 件の nuspec と同梱物を検査する
- **THEN** Android binding の `lib/` に aar 2 本があり nuspec の依存に AndroidX 系 (LiveData 2.11.0.1 を含む) が並び、iOS binding の resource package 内 xcframework に `ios-arm64` と `ios-arm64_x86_64-simulator` の両スライスがあり、両 nuspec の description に "do not reference this package directly" が含まれる

### Requirement: 消費者からの導入

利用者は facade パッケージ 1 件の `PackageReference` だけで、binding と AndroidX 系の依存を推移的に得られる SHALL。要件 (`Microsoft.Maui.Controls` 10.0.70 以上、最低 OS 版) を満たす利用者アプリは、AndroidX の版ピンなしで NU1608 / NU1107 のない restore と、両 OS の Release ビルドが成立する SHALL。消費者検証は本リポジトリと同じ SDK / workload (`global.json` の 10.0.300 / 10.0.300.3) で、隔離した packages path とローカルフィードだけを取得元にして行い、取得した Package ID / version / source を証跡に残す SHALL。

#### Scenario: ローカルフィードからの restore と Release ビルド

- **GIVEN** 3 パッケージを置いたローカルフォルダフィードと、本リポジトリの `global.json` を複製して SDK を固定し、隔離した packages path を使う、`Microsoft.Maui.Controls` 10.0.70・Android 29・iOS 16.0 を設定した素の MAUI アプリ
- **WHEN** `KsSettingsView.Maui` の PackageReference を 1 行足して restore し、facade の公開型を参照した状態で Android と iOS (simulator) を Release でビルドする
- **THEN** restore の警告が 0 件で `Xamarin.AndroidX.Lifecycle.LiveData` family が 2.11.0.1 に解決し、binding 2 件が推移的に解決され、両 OS の Release ビルドが成功して成果物に facade と binding のアセンブリが残る

### Requirement: 最低 OS 版のビルド時ガード

facade パッケージは利用者のビルドに同梱される MSBuild 資産 (`buildTransitive/`) を持ち、platform TFM のビルドで利用者アプリの `SupportedOSPlatformVersion` が要件 (Android 29 / iOS 16.0) 未満または未設定なら、要件と設定方法を示すエラーでビルドを止める SHALL。要件を満たす場合は何も出力しない SHALL。要件の数値はこの同梱資産が単一の宣言元であり、リポジトリ内の facade・binding・検証ホストの `SupportedOSPlatformVersion` も同じ宣言元から導かれる SHALL。

#### Scenario: 要件未満の利用者アプリ

- **GIVEN** ローカルフィードの facade を参照し、Android の `SupportedOSPlatformVersion` を 21、iOS を 15.0 に設定した利用者アプリ
- **WHEN** それぞれの platform TFM でビルドする
- **THEN** Android は manifest merger のエラーより先に、iOS はビルド成功に至る前に、KsSettingsView の要件 (Android 29 / iOS 16.0) と `SupportedOSPlatformVersion` の設定を促す文面のエラーで失敗する

#### Scenario: 要件を満たす利用者アプリと検証ホスト

- **GIVEN** 要件を満たす設定の利用者アプリと、リポジトリ内の MauiHost / IntegrationHost
- **WHEN** それぞれをビルドする
- **THEN** ガード由来のエラー・警告は出ず、ビルドが成功する

#### Scenario: 非 platform TFM と outer build ではガードが動かない

- **GIVEN** ローカルフィードの facade を参照する `net10.0` のみのクラスライブラリと、`net10.0;net10.0-android;net10.0-ios` の複数 TFM プロジェクト (最低 OS 版は未設定)
- **WHEN** クラスライブラリをビルドし、複数 TFM プロジェクトの outer build (TFM 指定なし) と `net10.0` の inner build を実行する
- **THEN** ガード由来の診断は出ず、複数 TFM プロジェクトでは platform の inner build に入って初めて要件未満のエラーが出る

#### Scenario: 要件の宣言元の一致

- **GIVEN** 同梱資産 (`buildTransitive/`) の要件の数値
- **WHEN** facade (net10.0-android / net10.0-ios)・binding 2 件・検証ホスト 3 件について TFM ごとに `SupportedOSPlatformVersion` の評価値を取得する
- **THEN** Android は 29、iOS は 16.0 と一致し、csproj に数値の直書きが残っていない

### Requirement: package README の表示

facade パッケージはルート `README.md` を package README として同梱する SHALL。`README.md` / `README_ja.md` のスクリーンショット参照は public リポジトリ上の絶対 URL であり、nuget.org と GitHub の両方で表示できる SHALL。両 README の MAUI 最小例 (xmlns / `using`) は改名後の名前空間を使い、同梱物の facade でそのままビルドできる SHALL。

#### Scenario: README の同梱と画像参照

- **GIVEN** pack 設定を導入した facade
- **WHEN** facade を pack し、`README.md` / `README_ja.md` の画像参照を検査する
- **THEN** nupkg のルートに `README.md` があり nuspec の readme がそれを指し、両 README の画像参照がすべて `https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/` 配下の絶対 URL で相対パス参照が残っておらず、各 URL は取得に成功して画像の Content-Type を返し、両 README に `KsSettingsView.Maui` を名前空間として使う例 (xmlns の `clr-namespace:KsSettingsView.Maui`) が残っていない

#### Scenario: README の例による消費者ビルド

- **GIVEN** 同梱 README の MAUI 最小例 (XAML と `MauiProgram` の登録) をそのまま写した、要件を満たす利用者アプリ
- **WHEN** ローカルフィードの facade を参照して両 OS をビルドする
- **THEN** 例を変更せずにビルドが成功する

### Requirement: 公開識別子の規範への NuGet 座標の記載

`kasane/handbook/cross/public-identifiers.md` は NuGet の Package ID (`KsSettingsView.Maui` / `KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android`) と、facade の名前空間 `KsSettingsView` が Package ID と非対称である理由を含む SHALL。cross/ADR-0018 の配布先表は書き換えず、日付付き追記で MAUI の Package ID を注記する SHALL。

#### Scenario: 規範文書の記載

- **GIVEN** 改訂後の handbook と ADR-0018
- **WHEN** 公開識別子の表と ADR-0018 の末尾を読む
- **THEN** NuGet Package ID 3 件と名前空間の非対称の説明が handbook にあり、ADR-0018 に日付付きの追記として MAUI の Package ID が記されている

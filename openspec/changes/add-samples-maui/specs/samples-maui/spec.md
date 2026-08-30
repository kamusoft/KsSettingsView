## ADDED Requirements

### Requirement: MAUI Sample アプリの存在

`samples/maui/` 配下に .NET MAUI Sample アプリが存在しなければならない (SHALL)。Sample アプリは `KsSettingsView.Maui` および MAUI バインディングプロジェクト（`KsSettingsView.Bindings.iOS` / `KsSettingsView.Bindings.Android`）を `<ProjectReference>` で参照し、`net9.0-ios` および `net9.0-android` の両ターゲットでビルド可能でなければならない (MUST)。

#### Scenario: MAUI プロジェクトの存在

- **GIVEN** リポジトリのクローン直後
- **WHEN** `samples/maui/` 配下を確認する
- **THEN** `KsSettingsView.Sample.Maui.csproj`（または同等の `.csproj`）、`MauiProgram.cs`、`App.xaml`、`MainPage.xaml` を含む .NET MAUI プロジェクト構造が存在する

#### Scenario: ターゲットフレームワーク

- **GIVEN** `samples/maui/KsSettingsView.Sample.Maui.csproj`
- **WHEN** `<TargetFrameworks>` を確認する
- **THEN** `net9.0-ios` および `net9.0-android` の両方が含まれている

#### Scenario: KsSettingsView.Maui への依存

- **GIVEN** `samples/maui/KsSettingsView.Sample.Maui.csproj`
- **WHEN** `<ProjectReference>` を確認する
- **THEN** `KsSettingsView.Maui.csproj` および MAUI バインディングプロジェクト（iOS / Android 用）が `<ProjectReference>` で参照されている

#### Scenario: iOS シミュレータでの起動

- **GIVEN** `samples/maui/`
- **WHEN** `dotnet build -t:Run -f net9.0-ios` を実行する
- **THEN** ビルドが成功し、iOS シミュレータ上で Sample アプリが起動する

#### Scenario: Android エミュレータでの起動

- **GIVEN** `samples/maui/`
- **WHEN** `dotnet build -t:Run -f net9.0-android` を実行する
- **THEN** ビルドが成功し、Android エミュレータ上で Sample アプリが起動する

### Requirement: AddKsSettingsView の登録

Sample アプリの `MauiProgram.CreateMauiApp()` は、`AddKsSettingsView()` 拡張メソッド（`add-maui-core` で公開される）を呼び出して KsSettingsView の Handler を登録しなければならない (SHALL)。

#### Scenario: Handler 登録

- **GIVEN** `samples/maui/MauiProgram.cs`
- **WHEN** `CreateMauiApp` メソッドの実装を確認する
- **THEN** `MauiAppBuilder` に対して `AddKsSettingsView()` が呼び出されている

### Requirement: 最小デモ画面

Sample アプリの起動直後の画面は、`<ks:SettingsView>` を使い、`LabelCell`（`add-maui-core` で実装される最小 Cell）を 1 セクション・複数行含むデモ画面を描画しなければならない (SHALL)。

#### Scenario: 起動時の画面表示

- **GIVEN** Sample アプリが iOS シミュレータまたは Android エミュレータで起動した直後
- **WHEN** 画面のコンテンツを確認する
- **THEN** `<ks:SettingsView>` が画面いっぱいに表示され、`LabelCell` の `Title`（または相当のプロパティ）を含む 1 行のセルが複数行（2 行以上）描画される

#### Scenario: Section ヘッダ・フッタの描画

- **GIVEN** Sample アプリが起動した直後
- **WHEN** 描画されたセクションを確認する
- **THEN** `Section` の `Header` / `Footer`（`add-maui-core` の `Section.cs` で定義される）が、対応する文字列でセクション境界に表示される

#### Scenario: XAML での記述

- **GIVEN** Sample のソースコードを参照する
- **WHEN** `samples/maui/MainPage.xaml` を確認する
- **THEN** `<ks:SettingsView>` の中で `<ks:Section>` および `<ks:LabelCell>` が XAML 形式で記述されている

### Requirement: README の整備

`samples/maui/README.md` は、`add-monorepo-foundation` で配置された placeholder から、実 Sample アプリのクイックスタート README に置き換えられていなければならない (SHALL)。

#### Scenario: クイックスタートの記載

- **GIVEN** `samples/maui/README.md` を開く
- **WHEN** その内容を確認する
- **THEN** 「概要」「必要環境（Visual Studio 2022 / Rider / .NET 9 SDK / Xcode 16+ / Android SDK API 29+）」「開き方」「実行手順（`dotnet build -t:Run -f net9.0-ios` / `-f net9.0-android`）」「ディレクトリ構成」「関連リンク」のいずれにも該当する記載が含まれている

#### Scenario: placeholder からの置き換え

- **GIVEN** `samples/maui/README.md`
- **WHEN** その内容を確認する
- **THEN** 「後続変更提案で追加予定」等の placeholder 文言は残っておらず、実 Sample 用のクイックスタートに更新されている

#### Scenario: 本体ライブラリのデバッグ手順の記載

- **GIVEN** `samples/maui/README.md`
- **WHEN** その内容を確認する
- **THEN** 「本体ライブラリのデバッグ」セクションが存在し、本 Sample が `<ProjectReference>` で本体プロジェクト（`KsSettingsView.Maui` および MAUI バインディングプロジェクト）を参照するためブレークポイントを置いてステップインできる旨と、本体テストを主軸に走らせる場合は `KsSettingsView.slnx` ソリューション全体を IDE で開く運用が併記されている

### Requirement: アプリのメタデータ

Sample アプリは、ApplicationId プレフィックスとして `jp.kamusoft.kssettingsview.samples.maui` を使用しなければならない (SHALL)。iOS Deployment Target は 16.0 以上、Android minSdk は 29 以上でなければならない (MUST)。

#### Scenario: ApplicationId の確認

- **GIVEN** `samples/maui/KsSettingsView.Sample.Maui.csproj`
- **WHEN** `ApplicationId` 等のメタデータを確認する
- **THEN** `jp.kamusoft.kssettingsview.samples.maui` で始まる識別子が設定されている

#### Scenario: iOS Deployment Target の確認

- **GIVEN** `samples/maui/KsSettingsView.Sample.Maui.csproj`
- **WHEN** `<SupportedOSPlatformVersion Condition="..." Include="ios" />` 等を確認する
- **THEN** iOS の最小サポートバージョンが 16.0 以上に設定されている

#### Scenario: Android minSdk の確認

- **GIVEN** `samples/maui/KsSettingsView.Sample.Maui.csproj`
- **WHEN** Android 関連のメタデータを確認する
- **THEN** Android の minSdk が 29 以上に設定されている

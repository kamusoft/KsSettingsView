# maui-nuget-distribution デルタスペック

## ADDED Requirements

### Requirement: restore 元の固定
`maui/` 配下のプロジェクトの restore は、環境のパッケージソース設定に関わらず nuget.org だけを参照する SHALL。複数ソース環境で出る NU1507 は発生しない。`samples/maui` と `verification/maui` はこの設定の対象外である。

#### Scenario: 複数ソース環境でも nuget.org だけから取得する
- **GIVEN** ユーザー設定にローカルフィード等の追加ソースがある作業機
- **WHEN** `maui/` 配下のプロジェクトを restore する
- **THEN** すべてのパッケージの取得元は nuget.org で、NU1507 は出ない

## MODIFIED Requirements

### Requirement: 3 パッケージの構成と内容
facade は Package ID `KsSettingsView.Maui` として pack でき、TFM group ごとの依存に `Microsoft.Maui.Controls` と、platform TFM でのみ対応する binding パッケージ (`KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android`) を含む SHALL。facade のパッケージに binding のアセンブリや native 成果物は含まれない SHALL。Android binding は aar 2 本 (`kssettingsview` / `kssettingsview-bridge`) と Gradle 側の実行時依存に対応する AndroidX 系の依存 (`Xamarin.AndroidX.Lifecycle.LiveData` 2.11.0.1 を含む) を持つ SHALL。iOS binding は device と simulator 両スライスを含む xcframework の binding resource package を持つ SHALL。binding の Description は facade から推移的に参照されるものであり直接参照しない旨を英語で含む SHALL。facade と Android binding のパッケージは、SDK が自 assembly 用に生成する aar (`KsSettingsView.Maui.aar` / `KsSettingsView.Binding.Android.aar`) を含まない SHALL。pack は、その生成 aar が推移依存由来の native ライブラリ以外 (自前のリソース・クラス・manifest) を含む場合に失敗する SHALL。

#### Scenario: 3 パッケージのローカル pack
- **GIVEN** `maui/` のリポジトリ状態
- **WHEN** binding 2 件 → facade の順に `dotnet pack` する
- **THEN** 3 つの nupkg が生成され、facade の依存グループに binding が platform TFM でのみ現れ、facade に native 成果物が含まれない

#### Scenario: 自 assembly 用 aar が nupkg に入らない
- **GIVEN** facade と Android binding の pack
- **WHEN** 生成された nupkg の `lib/net10.0-android*/` の内容を確認する
- **THEN** `KsSettingsView.Maui.aar` / `KsSettingsView.Binding.Android.aar` は含まれず、アセンブリと Gradle 由来の aar 2 本 (binding) は含まれる

#### Scenario: 生成 aar に自前の内容が入ると pack が失敗する
- **GIVEN** 生成 aar に推移依存由来の native ライブラリ以外のエントリ (例: `res/` や `classes.jar`) が含まれる状態
- **WHEN** pack する
- **THEN** pack は失敗し、想定外のエントリが出力で確認できる

#### Scenario: 利用者の Android Release ビルドに重複警告が出ない
- **GIVEN** facade を参照する利用者アプリ (消費者検証の MAUI 消費者)
- **WHEN** `net10.0-android` の Release でビルドする
- **THEN** XA4301 は出ず、ビルドは成功する

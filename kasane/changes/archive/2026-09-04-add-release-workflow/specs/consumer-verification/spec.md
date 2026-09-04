# consumer-verification デルタスペック

## MODIFIED Requirements

### Requirement: MAUI 消費者の依存検査
MAUI 消費者の restore は、ダウングレード (NU1605)・依存版範囲外 (NU1608)・版競合 (NU1107) の警告を失敗として扱う SHALL。あわせて、解決された `KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android` の version が facade `KsSettingsView.Maui` の version と一致することを検査し、不一致は失敗とする SHALL。Android の Release ビルドで native ライブラリの重複 (XA4301) が出た場合は失敗とする SHALL。それ以外のビルド警告全般は失敗にしない。AndroidX 等の推移依存の解決版は検査しない。

#### Scenario: 依存警告で失敗する
- **GIVEN** 依存の版競合またはダウングレードが起きる構成
- **WHEN** MAUI 消費者を restore する
- **THEN** restore は失敗として報告される

#### Scenario: binding の version 不一致を検出する
- **GIVEN** 参照先に facade と異なる version の binding しか存在しない状態
- **WHEN** MAUI 消費者の依存検査が走る
- **THEN** 検査は失敗として報告され、facade と binding それぞれの解決版が出力で確認できる

#### Scenario: native ライブラリの重複で失敗する
- **GIVEN** 参照するパッケージに推移依存の native ライブラリを抱えた aar が含まれ、Android の Release ビルドで XA4301 が出る状態
- **WHEN** MAUI 消費者を Release ビルドする
- **THEN** ビルドは失敗として報告され、重複したライブラリのパスが出力で確認できる

#### Scenario: その他のビルド警告は失敗にしない
- **GIVEN** restore に警告がなく、ビルドで XA4301 以外の警告だけが出る状態
- **WHEN** MAUI 消費者を Release ビルドする
- **THEN** ビルドは成功として報告される

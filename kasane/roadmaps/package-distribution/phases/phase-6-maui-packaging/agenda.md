# phase-6-maui-packaging

MAUI facade の名前空間を `KsSettingsView` に改め、facade + binding 2 件の 3 NuGet パッケージとして pack できる形にする (maui/ADR-0025)。

## 論点

- 名前空間改名 (`KsSettingsView.Maui` → `KsSettingsView`、`.Internals` / `.Handlers` も同様) の範囲: facade / テスト / 検証ホスト / Sample / XAML xmlns (`clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui`)。下準備タスクとして最初に行う
- `maui/Directory.Build.props` (Authors / License MIT / RepositoryUrl / PackageIcon / PackageReadmeFile / Version 既定値 / SourceLink / snupkg) と `maui/Directory.Packages.props` (CPM: Microsoft.Maui.Controls、Xamarin.AndroidX 系) の内容
- binding の `IsPackable=true`、PackageId、Description (「直接参照しないこと」)
- pack PoC: Exec 経由 gradlew (maui/ADR-0006) と SDK 標準 pack 経路の整合、iOS binding resource package の中身 (device + simulator スライス)、manifest の絶対パス
- nuget.org 固有要件 (license expression、icon、readme、symbol package) の確認
- AndroidX Lifecycle 競合 (NU1608 / NU1107) が NuGet 経由で解消されるかの実証 (maui/ADR-0010 の未検証項目)
- Release / trimming (AOT) 構成での消費者ビルド確認
- `KsSettingsView.slnx` に Sample を含めたままでよいか (pack 対象との分離)
- 蒸留時の docs 追随: maui README、native-bridge.md の binding 構成節

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす

# maui-bridge デルタスペック

## ADDED Requirements

### Requirement: AndroidX Lifecycle 依存の binding 層整合
`KsSettingsView.Binding.Android` は AndroidX Lifecycle family の版整合を自身の依存宣言で担保する SHALL。`KsSettingsView.Maui` を `<ProjectReference>` で参照する利用側プロジェクトは、AndroidX 競合解消のための追加 `PackageReference` (バージョンピン) や `NoWarn` 指定なしで restore・ビルドでき、NU1608 を発生させない SHALL。

(スコープ注記: NuGet パッケージ参照経由の利用者への効果は本変更では検証しない — パッケージングは roadmap 非ゴールで、その時点の課題として扱う。**合意済み fallback**: 実装時の実証で NU1608 が消えない場合は `NoWarn` + README への既知の制約記録に切り替え、本 Requirement からの乖離として deviation.md に記録する。未実証の NU1107 が実発生した場合の対処 (利用側の直接参照等) も同様に deviation.md に記録する — agenda 決定 2026-08-09、maui/ADR-0010)

#### Scenario: ピンなしの利用側が警告なしで restore できる
- **GIVEN** `KsSettingsView.Maui` を `<ProjectReference>` する net10.0-android の利用側プロジェクト (AndroidX の追加ピン・`NoWarn` 指定なし)
- **WHEN** restore する
- **THEN** NU1608 / NU1107 が発生せず、`project.assets.json` 上で `Xamarin.AndroidX.Lifecycle.LiveData` / `.LiveData.Core` / `.LiveData.Core.Ktx` がいずれも 2.11.0.1 で解決される

#### Scenario: MauiHost のピン削除後も整合が保たれる
- **GIVEN** アプリ側ピン (`Xamarin.AndroidX.Lifecycle.LiveData.Core(.Ktx)`) を削除し `NoWarn` も持たない `KsSettingsView.MauiHost`
- **WHEN** restore してビルドする
- **THEN** ビルドが成功し、NU1608 が発生せず、LiveData family が 2.11.0.1 で解決される

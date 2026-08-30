# Tasks: add-maui-samples-foundation

## 1. AndroidX 依存の binding 層整合

- [x] 1.1 `KsSettingsView.Binding.Android.csproj` に `Xamarin.AndroidX.Lifecycle.LiveData 2.11.0.1` を明示宣言する (→ Requirement: AndroidX Lifecycle 依存の binding 層整合)
- [x] 1.2 restore 実証: MauiHost のピン・`NoWarn` なしの状態で restore し、NU1608 の消滅と `project.assets.json` 上の LiveData family = 2.11.0.1 解決を確認する。消えない場合は fallback (`NoWarn` + README 記録) へ切り替え、オーナー確認のうえ deviation.md に記録する。NU1107 が実発生した場合の対処 (利用側の直接参照等) も同様に deviation.md 記録 (→ Scenario: ピンなしの利用側が警告なしで restore できる)
- [x] 1.3 MauiHost の no-op ピン (`LiveData.Core(.Ktx)`) を削除し、実態と不一致の csproj コメントを修正する (→ Scenario: MauiHost からピンを外しても成立する)

## 2. サンプルアプリ土台

- [x] 2.1 `samples/maui/` に `KsSettingsView.Sample.Maui` を scaffold する (csproj: net10.0-ios;net10.0-android、iOS 16.0 / Android API 29、ApplicationId `jp.kamusoft.kssettingsview.samples.maui`、`KsSettingsView.Maui` への ProjectReference 1本、`MauiProgram` で `AddKsSettingsView()`) (→ Requirement: サンプルアプリの成立)
- [x] 2.2 `ReactiveProperty.Core` を導入する (バージョンは実装時点の最新安定版をピン) (→ Requirement: サンプルアプリの成立)
- [x] 2.3 画面 descriptor の一元定義 (iOS/Android サンプルの `SampleScreen` 相当) を実装する — 区分 (デモ/検証)・表示文言・遷移先を1箇所の定義に集約し、後続フェーズのページ追加が「定義1件の追加 + ページ実装」で完結する形にする (→ Requirement: デモ一覧と画面遷移)
- [x] 2.4 デモ一覧ページ (MainPage) + NavigationPage 骨格を実装する (→ Requirement: デモ一覧と画面遷移)
- [x] 2.5 LabelCell 検証ページを実装する (1 Section・Header/Footer 文言・LabelCell 3行 — Title/ValueText 全行 + Description/HintText 各1行以上、ViewModel は ReactiveProperty で少なくとも1値をバインドし画面上の更新操作を持つ) (→ Requirement: LabelCell 検証ページ)
- [x] 2.6 `KsSettingsView.slnx` にサンプルプロジェクトを登録する (→ Requirement: サンプルアプリの成立)

## 3. README

- [x] 3.1 `samples/maui/README.md` を実サンプルのクイックスタート (必要環境 / 開き方 / 両 OS の CLI 実行手順 / 依存関係) に置換する (→ Requirement: クイックスタート README)

## 4. 検証

- [x] 4.1 両 OS でビルドし、iOS シミュレータ / Android エミュレータで起動を目視確認する (→ Scenario: 両 OS でビルドできる / シミュレータ・エミュレータで起動できる)
- [x] 4.2 一覧 → LabelCell 検証ページの遷移・戻る・タイトル一致・検証区分の表記を確認する (→ Scenario: 項目選択で遷移しタイトルが一致する / 検証画面はデモと区別される)
- [x] 4.3 LabelCell 検証ページの表示内容 (Section Header/Footer + LabelCell 3行の各フィールド文言) と、更新操作による表示反映を両 OS で目視確認する (→ Scenario: Section と LabelCell が表示される / 値の変更が表示へ反映される)
- [x] 4.4 README 記載の CLI コマンドをそのまま実行して両 OS の起動に到達できることを確認する (→ Scenario: README だけで実行に到達できる)

注: サンプル専用の自動テストプロジェクトは置かない (原案 Decision 8 踏襲 — サンプルの責務は目視確認可能な最小アプリ。ライブラリ本体の自動テストは KsSettingsView.Maui.Tests / MauiHost E2E が担う)。本変更のテストは 1.2 の restore 実証と 4.x の両 OS 目視検証が該当する。

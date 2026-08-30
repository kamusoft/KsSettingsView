# Proposal: add-maui-samples-foundation

## Why

`samples/maui/` は placeholder README のみで、phase-2 (add-maui-core) で完成した `KsSettingsView.Maui` を実機・シミュレータで目視確認できるサンプルアプリが存在しない。後続フェーズ (phase-4/6/5) はサンプルページ追加を自フェーズで持つため、「ページ追加 + 一覧に1行」だけで拡張できる土台を先に建てる。

## What Changes

- `samples/maui/` に .NET MAUI サンプルアプリ `KsSettingsView.Sample.Maui` (net10.0-ios / net10.0-android) を新設
  - デモ一覧ページ (MainPage) + NavigationPage の複数ページ骨格。メニュー文言と画面タイトルの一元定義 (iOS/Android サンプルの `SampleScreen` 相当) を含む
  - 初期ページとして「LabelCell 検証」ページ (1 Section・LabelCell 3行 + Section Header/Footer)。sample-parity (cross/ADR-0016) の検証枠に置き、phase-4 の基本 Cell 7種デモページ追加時に削除する暫定画面
  - ViewModel 層に OSS の **ReactiveProperty.Core** を採用 (バインディング記述量の削減。ReactivePropertySlim / ReactiveCommandSlim で足り System.Reactive 非依存の軽量版を選定 — 後続フェーズで Rx オペレータが必要になれば full `ReactiveProperty` への切替は非破壊で可能。オーナー確定 2026-08-09)
  - 参照は `KsSettingsView.Maui.csproj` 1本 (`<ProjectReference>`)。`KsSettingsView.slnx` に登録
- `KsSettingsView.Binding.Android.csproj` に `Xamarin.AndroidX.Lifecycle.LiveData 2.11.0.1` を明示宣言し、AndroidX Lifecycle の版ねじれ (NU1608) を binding 層で吸収する (maui/ADR-0010 proposed)。restore で実証し、消えなければ fallback (`NoWarn` NU1608 + README 記録)
- `maui/tests/KsSettingsView.MauiHost` の no-op と判明したアプリ側ピン (`LiveData.Core(.Ktx)`) と誤コメントを整理
- `samples/maui/README.md` を実サンプルのクイックスタート README に置換

影響 capability: `samples-maui` (新規) / `maui-bridge` (AndroidX 依存宣言の追加)

## Non-Goals

- 各 Cell のデモページ追加 (phase-4/5/6 の責務)
- Store/DSL 方式デモの MAUI 対応要否の判断 (phase-4 agenda へ申し送り)
- Snapshot テスト・CI 連携・NuGet パッケージング・Mac Catalyst / Windows ターゲット
- Root Header/Footer の設定デモ (後続フェーズの判断に委ねる)

## Impact

- 破壊的変更なし。サンプルは新規追加、Binding の依存宣言は既存の実解決 (Core 2.11.0.1) に LiveData 本体を揃える整合方向の変更
- 触るもの: `samples/maui/` (新規)、`maui/android/KsSettingsView.Binding.Android/` (csproj 1行)、`maui/tests/KsSettingsView.MauiHost/` (ピン整理)、`maui/KsSettingsView.slnx`
- リスク: 低。AndroidX 宣言の効果が restore 実証で否定される可能性 → fallback 決定済み (agenda 参照)

## 級: M

新規サンプルアプリ + csproj 数行の変更で、公開 API・core 挙動の変更なし。設計判断はフェーズ議論で決定済み。

domain: maui
roadmap: maui-support/phase-3-samples-foundation

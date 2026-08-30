## Why

`add-monorepo-foundation` で `samples/maui/` ディレクトリと placeholder の `README.md` のみが配置された状態であり、`add-maui-core`（提案中・未実装）で構築する `KsSettingsView.Maui` を実機・シミュレータで目視確認できる Sample アプリ本体が存在しない。元々 `add-maui-bindings` 1 提案に Sample タスクも含まれていたが、提案を 3 分割（`add-maui-bridge` / `add-maui-core` / `add-maui-cells`）した上で、Sample 「土台」は独立した変更提案として切り出して責務を明確化する方が、レビュー・実装・進捗追跡のすべてで扱いやすい。

本提案では Sample 「土台」の最小成立（`add-maui-core` で実装される `LabelCell` を含む最小デモ画面）を担当する。各 Cell 種類のデモページ追加は後続の `add-maui-cells` が担当し、本提案の責務には含めない。

## What Changes

- 新ディレクトリ `samples/maui/` 配下に .NET MAUI Sample アプリ（最小構成）を作成
  - アプリ名: `KsSettingsView.Sample.Maui`（仮）
  - ターゲットフレームワーク: `net9.0-ios` / `net9.0-android`（macOS / Windows 等は本提案では対象外）
  - 言語: C# 13 / .NET 9
  - 名前空間プレフィックス: `KsSettingsView.Sample.Maui`
- `KsSettingsView.Maui`（`add-maui-core` で実装）および MAUI バインディングプロジェクト（`add-maui-bridge` で実装される `KsSettingsView.Bindings.iOS` / `KsSettingsView.Bindings.Android`）をプロジェクト参照（`<ProjectReference>`）で取り込む
- `MauiAppBuilderExtension.AddKsSettingsView()` を `MauiProgram.CreateMauiApp()` で呼び出して Handler を登録
- 1 ページのデモ画面 `MainPage.xaml` を作成し、`<ks:SettingsView>` を表示
  - 1 セクション・複数行の `LabelCell`（`add-maui-core` で実装される最小 Cell）を表示
  - Section の `Header` / `Footer` プロパティを設定
- `samples/maui/README.md`（現状 placeholder）を実 Sample のクイックスタート README に置き換え
  - Visual Studio / Rider / `dotnet` CLI で開く手順、iOS シミュレータ / Android エミュレータでの起動手順、依存関係の説明を含む
- 「含まないこと」（後続提案で対応）：
  - 全 Cell 種類（Switch / Command / Entry / Picker / Custom 等）を網羅したデモページ → `add-maui-cells` 側で各 Cell Handler 実装と同時にページ追加する責務として整理
  - Snapshot テスト基盤（`add-maui-cells` の責務として継続）
  - macOS Catalyst / Windows ターゲット
  - CI 連携

## Capabilities

### New Capabilities
- `samples-maui`: `samples/maui/` 配下に配置される .NET MAUI Sample アプリの構造・依存・起動可能性に関する振る舞いを規定する

### Modified Capabilities
（なし。本提案は純粋な追加であり、`monorepo-foundation` spec の placeholder Scenario は引き続き有効。`samples-maui` capability で「Sample 土台」を確立し、各 Cell 表示ページの追加は後続の `add-maui-cells` が担当する）

> **`purify-core-extract-style-to-ui-layer` 整合 note**: 本提案では Theme 構築サンプルや Color プロパティ操作は扱わない（`LabelCell` のみのデモのため）。MAUI Color → Native Color の直接変換経路の検証は `add-maui-cells` 側 Sample で扱う。本提案は `purify-core-extract-style-to-ui-layer` の影響を直接受けない（読み取り整合確認のみ）。

## Impact

- 影響範囲: `samples/maui/` 配下の新規 .NET MAUI プロジェクト・C# / XAML ソース・README
- 依存:
  - `add-monorepo-foundation`（archive 済）: `samples/maui/` ディレクトリと placeholder README が存在する前提
  - `add-settings-view-core`（archive 済）: モデルの存在
  - `add-settings-view-ios-ui`（archive 済）: iOS UI 基盤
  - `add-settings-view-android-ui`（archive 済）: Android UI 基盤
  - `add-partial-update-core` / `add-partial-update-native`（提案中・未実装）: `SettingsRootDiff` / `SettingsRootStore` / Native UI 層の部分更新 API。`add-maui-core` が依存するため遷移的に必須
  - `add-maui-bridge`（提案中・未実装）: MAUI バインディングプロジェクト（`KsSettingsView.Bindings.iOS` / `KsSettingsView.Bindings.Android`）を使用
  - `add-maui-core`（提案中・未実装）: `KsSettingsView.Maui` 本体ライブラリと `LabelCell`、`AddKsSettingsView()` 拡張を使用。本提案の **実装着手は `add-maui-core` の archive 完了後**とする
- 後続が依存:
  - `add-maui-cells`: 本 Sample に 13 Cell 種類（Command / Button / Switch / Checkbox / Radio / SimpleCheck / Entry / Picker / TextPicker / NumberPicker / TimePicker / DatePicker / Custom）の表示ページを追加する責務を担う
- リスク: 中
  - `add-maui-core` の API 確定前に Sample 提案アーティファクトを作成するため、API シェイプ変更時に Sample のタスクが手戻る可能性
  - 緩和策: 実装着手を `add-maui-core` archive 後に限定（Decision 9 と同様の方針）

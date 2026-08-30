## Context

`add-monorepo-foundation` で `samples/maui/` ディレクトリと placeholder の `README.md` のみが配置された状態で、`add-maui-core`（提案中・未実装）で構築する `KsSettingsView.Maui` を実機・シミュレータで目視確認できる Sample アプリ本体が存在しない。元 `add-maui-bindings` 1 提案に Sample タスクも含まれていたが、Sample 土台と各 Cell のデモページは責務が異なるため、本提案で「土台」を独立 capability として確立する。具象 Cell の表示（Switch / Command / Entry / Picker / Custom 等）は後続の `add-maui-cells` が「ページ追加」として担当する形に整理する。

.NET 9 / C# 13 / MAUI 9 が前提で、これは `add-maui-bridge` / `add-maui-core` の前提に揃える。ターゲットは `net9.0-ios` / `net9.0-android` の 2 つのみとし、macOS Catalyst / Windows / Tizen は本提案では対象外。

## Goals / Non-Goals

**Goals:**

- `samples/maui/` 配下に .NET MAUI Sample アプリを配置
- `KsSettingsView.Maui`（`add-maui-core` で実装）および MAUI バインディングプロジェクト（`add-maui-bridge` で実装される `KsSettingsView.Bindings.iOS` / `KsSettingsView.Bindings.Android`）を **`<ProjectReference>`** で参照
- `MauiProgram.CreateMauiApp()` で `AddKsSettingsView()` を呼び出して Handler を登録
- 1 ページの最小デモ画面 `MainPage.xaml` を作成し、`<ks:SettingsView>` で 1 セクション・複数行の `LabelCell`（`add-maui-core` で実装される最小 Cell）と `Section.Header` / `Footer` プロパティでヘッダ・フッタを表示
- `samples/maui/README.md` を実 Sample のクイックスタート README に置き換え
- 「placeholder のまま実 Sample が配置される」という monorepo-foundation review-result_002.md で言及された懸念を解消
- 後続の `add-maui-cells` が「ページ追加」のみで Sample を拡張可能な構造（Cell ごとのデモページの素地は本提案では用意せず、後続提案の判断に委ねる）

**Non-Goals:**

- 全 Cell 種類（13 種）のデモページ追加 → `add-maui-cells` が責務
- Snapshot テスト基盤 → `add-maui-cells` の責務として継続
- macOS Catalyst / Windows / Tizen ターゲット
- CI 連携
- 旧 AiForms.Maui.SettingsView の Sample をすべて踏襲（移行ガイド `docs/migration-from-aiforms.md` の責務、`add-maui-cells` で対応）

## Decisions

### Decision 1: .NET MAUI 標準テンプレート + Project Reference

**選択**: `samples/maui/` を独立した .NET MAUI プロジェクトとして作成（`dotnet new maui` 相当の構造）し、`KsSettingsView.slnx` への登録、または独立した `.sln` で管理する。`KsSettingsView.Maui` などのコアライブラリは `<ProjectReference Include="../../maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj" />` で参照する。

**理由**:

- monorepo-foundation で `KsSettingsView.slnx` が既に存在し、MAUI 関連プロジェクトはここから参照される設計
- `<ProjectReference>` ならコアライブラリの変更が即時 Sample に反映され、開発サイクルが短い
- NuGet パッケージ参照は本リポジトリ内では不要（モノレポ）

**代替案**:

- NuGet パッケージ参照（`<PackageReference>`）: 開発中の手戻りが多くなり却下。
- `dotnet new maui` ベースではなく独自スカフォールド: 標準テンプレートが最も保守性が高く却下。

### Decision 2: ターゲットフレームワークは iOS / Android のみ

**選択**: `<TargetFrameworks>net9.0-ios;net9.0-android</TargetFrameworks>` とし、macOS Catalyst / Windows / Tizen は対象外。

**理由**:

- `add-maui-bridge` / `add-maui-core` の対象が iOS / Android であり、Sample もそれに揃える
- Native Bridge が macOS / Windows 用に存在しないため、ビルドエラーになる

**代替案**:

- macOS Catalyst を含める: Bridge が iOS バイナリを共有する形で動作する可能性はあるが、本提案のスコープを越える。後続提案で扱う。

### Decision 3: アプリ構造は MAUI 標準（App.xaml + 単純 MainPage）

**選択**: `App.xaml` の `MainPage` に `MainPage` インスタンスを直接設定する最小構成にする（`AppShell` は使わない）。`MainPage.xaml` 内で `<ks:SettingsView>` を画面いっぱいに配置する。

**理由**:

- 単一ページの Sample に AppShell は過剰
- 後続提案（`add-maui-cells`）が AppShell や ナビゲーション導入したくなる場合に備え、本提案では最小構成に留める

**代替案**:

- AppShell ベース: 複数タブ / フライアウトを用意するなら有用だが、単一デモ画面では不要。
- NavigationPage: 不要。

### Decision 4: MAUI Sample で表示する Cell

**選択**: `add-maui-core` で公開される `LabelCell` を前提に、Sample では `<ks:LabelCell>` を 3 行表示する。`add-maui-core` archive 時点で `LabelCell` が利用可能であることは `add-maui-core` の Requirement「LabelCell の最小実装」で保証されている。

**理由**:

- `add-maui-core` の責務として `LabelCell` 1 種類が実装される（design.md Decision 4）ため、本提案の実装着手時には確実に利用可能
- Sample 専用 Cell の独自定義は MAUI の場合 BindableObject + Handler の自作が必要で工数が大きく、Sample スコープを超える

**代替案**:

- 案 A: Sample 内に `BindableObject` 派生の独自最小 Cell を定義する。
  - 却下理由: MAUI では BindableObject + Handler のペアが必要で、`add-maui-core` の責務と重複し、Sample スコープを超える。
- 案 B: `<ks:CustomCell>` で SwiftUI / Compose の任意 View を埋め込む。
  - 却下理由: `add-maui-cells` の依存となり、Sample 土台の最小要件としては重い。

### Decision 5: 表示する SettingsView の内容

**選択**: 1 セクション・3 行程度の `<ks:LabelCell>` を含む `<ks:SettingsView>` を `MainPage.xaml` 内で構築する。Section の `Header` / `Footer` プロパティ（`add-maui-core` の `Section.cs` で定義される）に "PoC Section" / "This is a footer" 等の文言を設定する。Root H/F は本提案では設定しない（後続提案の判断に委ねる）。

**理由**:

- 動作確認には複数行があるとレイアウト確認がしやすい
- Section H/F の Text 形式は目視確認のため最小限設定する
- Root H/F は後続提案で扱う

**代替案**:

- 1 行のみの最小構成: 動作確認としては 1 行でも足りるが、複数行のほうがレイアウト確認がしやすい。
- Style 切替 UI を含める: Sample 土台の責務を超えるため、後続提案に委ねる。

### Decision 6: README の構成

**選択**: `samples/maui/README.md` を以下のセクション構成で書き換える：

1. 概要（このサンプルアプリが何を示すか）
2. 必要環境（Visual Studio 2022 17.13+ / Rider / .NET 9 SDK / Xcode 16+ / Android SDK API 29+）
3. 開き方（Visual Studio / Rider / `dotnet` CLI のいずれか）
4. 実行（iOS シミュレータ: `dotnet build -t:Run -f net9.0-ios` / Android エミュレータ: `dotnet build -t:Run -f net9.0-android`）
5. ディレクトリ構成（簡易ツリー）
6. 関連リンク（KsSettingsView Core / iOS / Android / Maui README、`add-maui-bridge` / `add-maui-core` / `add-maui-cells` 提案へのリンク）

**理由**:

- monorepo-foundation review-result_002.md で「placeholder のまま実 Sample が配置される」リスクが指摘されており、明確に置き換える
- iOS / Android の両方で動かす CLI コマンドを README に明記しておくことで、IDE 不在環境でも検証可能

**代替案**:

- README なし: クイックスタート不能で却下。

### Decision 7: アプリのメタデータ

**選択**:

- ApplicationId: `jp.kamusoft.kssettingsview.samples.maui`
- Display Name: `KsSettingsView Sample`
- ApplicationVersion: `1`
- ApplicationDisplayVersion: `0.1.0`
- iOS Deployment Target: 16.0（iOS UI / Bridge と一致）
- Android minSdk: 29（Android UI / Bridge と一致）

**理由**:

- monorepo-foundation のパッケージ ID プレフィックス規約 `jp.kamusoft.kssettingsview.*` に準拠
- iOS 16 / Android API 29 は KsSettingsView 本体の規約と一致

**代替案**:

- iOS 14 / Android API 24: 本体の規約に合わないため却下。

### Decision 8: テストプロジェクトは置かない

**選択**: 本提案では Sample 専用のテストプロジェクト（xUnit / NUnit / MSTest）は配置しない。

**理由**:

- KsSettingsView 本体のテストは `add-maui-core` の `KsSettingsView.Maui.Tests` と `add-maui-cells` の Snapshot テストに配置される計画
- Sample の責務は「目視確認可能な最小アプリ」であり、自動テストは Non-Goals

**代替案**:

- UI Test を追加: 後続提案で扱うべき。本提案のスコープ外。

### Decision 9: 実装順序の制約

**選択**: 本 Sample 提案の **実装着手は `add-maui-core` の archive 完了後**とする。本変更提案の作成（proposal / design / specs / tasks）は先行してよいが、実装フェーズ（`opsx:apply`）は依存提案完了後に行う。

**理由**:

- `KsSettingsView.Maui` および MAUI バインディングプロジェクトが存在しない状態では Sample のビルドが通らない
- `add-maui-core` の API（`<ks:SettingsView>` / `<ks:LabelCell>` / `AddKsSettingsView()` 等）が確定してから Sample を実装するほうが、API 不整合の手戻りが少ない

**代替案**:

- 本提案を先行実装する: モックや想定 API で進めることになり、`add-maui-core` 確定後に大幅な書き換えが必要。却下。

## Risks / Trade-offs

- **Risk**: `add-maui-core` の API が変更されると Sample の実装が手戻る
  - **緩和策**: 本提案の実装着手を `add-maui-core` archive 後に限定する（Decision 9）
- **Risk**: `LabelCell` 以外を表示したくなった場合の対応
  - **緩和策**: `add-maui-cells` で各 Cell Handler 実装と同時に Sample へのページ追加を行う設計とし、本提案では LabelCell のみで固定する
- **Trade-off**: `<ProjectReference>` のため Sample のビルドが本体のビルドと連動する（コアライブラリにエラーがあると Sample もビルド不能）
  - **緩和策**: 本リポジトリはモノレポであり、これは想定される動作。NuGet パッケージ化は将来的な公開時に検討
- **Trade-off**: macOS Catalyst / Windows を対象外とすることで一部開発者は手元で iOS シミュレータが動かない場合に検証できない
  - **緩和策**: README に Android エミュレータでの起動手順を明記。CI 整備は後続提案で対応

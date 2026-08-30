## 依存関係

- 前提:
  - `add-monorepo-foundation`（archive 済）: `samples/maui/README.md` placeholder が存在
  - `add-settings-view-core`（archive 済）: モデルの存在
  - `add-settings-view-ios-ui`（archive 済）: iOS UI 基盤
  - `add-settings-view-android-ui`（archive 済）: Android UI 基盤
  - `add-maui-bridge`（提案中・未実装）: MAUI バインディングプロジェクト（`KsSettingsView.Bindings.iOS` / `KsSettingsView.Bindings.Android`）を使用
  - `add-maui-core`（提案中・未実装）: `KsSettingsView.Maui` 本体、`AddKsSettingsView()` 拡張、`LabelCell` を使用
- 実装着手順序:
  - 本提案の **実装着手は `add-maui-core` の archive 完了後**とする（design.md Decision 9）
  - 変更提案アーティファクトの作成（proposal / design / specs / tasks）は先行してよい
- 後続:
  - `add-maui-cells`: 本 Sample に 13 Cell 種類（Command / Button / Switch / Checkbox / Radio / SimpleCheck / Entry / Picker / TextPicker / NumberPicker / TimePicker / DatePicker / Custom）の表示ページを追加

## 1. .NET MAUI プロジェクト作成

- [ ] 1.1 `samples/maui/` 配下に .NET MAUI プロジェクトを作成（テンプレート: `dotnet new maui`、出力名 `KsSettingsView.Sample.Maui`）
- [ ] 1.2 `<TargetFrameworks>` を `net9.0-ios;net9.0-android` のみに設定（macOS Catalyst / Windows / Tizen は除外）
- [ ] 1.3 ApplicationId を `jp.kamusoft.kssettingsview.samples.maui` に設定
- [ ] 1.4 Display Name を `KsSettingsView Sample` に設定
- [ ] 1.5 ApplicationVersion を `1`、ApplicationDisplayVersion を `0.1.0` に設定
- [ ] 1.6 iOS Deployment Target を 16.0 以上に設定
- [ ] 1.7 Android minSdk を 29 以上に設定
- [ ] 1.8 不要な自動生成ファイル（テンプレートの `Resources/` 内サンプル画像等）を整理する

## 2. KsSettingsView 関連プロジェクトの参照

- [ ] 2.1 `samples/maui/KsSettingsView.Sample.Maui.csproj` に `<ProjectReference Include="../../maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj" />` を追加
- [ ] 2.2 必要に応じて MAUI バインディングプロジェクト（`KsSettingsView.Bindings.iOS` / `KsSettingsView.Bindings.Android`）への `<ProjectReference>` を追加（`KsSettingsView.Maui` 側の TFM 別依存に従う）
- [ ] 2.3 `KsSettingsView.slnx` に `samples/maui/KsSettingsView.Sample.Maui.csproj` を登録（既存ソリューション構成に整合させる）
- [ ] 2.4 `using KsSettingsView.Maui;` 等が C# / XAML から解決可能であることを確認

## 3. MauiProgram と AddKsSettingsView 登録

- [ ] 3.1 `MauiProgram.cs` の `CreateMauiApp` 内で `MauiAppBuilder.AddKsSettingsView()` を呼び出す
- [ ] 3.2 `App.xaml.cs` の `App` クラスで `MainPage = new MainPage()` を設定（AppShell は使わない、Decision 3）

## 4. MainPage の最小デモ画面

- [ ] 4.1 `MainPage.xaml` を作成し、`xmlns:ks="clr-namespace:KsSettingsView.Maui;assembly=KsSettingsView.Maui"` 名前空間を宣言
- [ ] 4.2 `<ContentPage>` 直下に `<ks:SettingsView>` を画面いっぱいに配置
- [ ] 4.3 `<ks:SettingsView.Sections>` に `<ks:Section>` を 1 つ追加し、`Header="PoC Section"` / `Footer="This is a footer"`（`add-maui-core` の `Section.cs` で定義されるプロパティに準拠）を設定
- [ ] 4.4 `<ks:Section.Cells>` に `<ks:LabelCell>` を 3 行追加し、`Title` と `Description`（または相当）を異なる値に設定
- [ ] 4.5 `MainPage.xaml.cs` で `InitializeComponent()` のみ実装（最小コードビハインド）

## 5. README 整備

- [ ] 5.1 `samples/maui/README.md` の placeholder を削除
- [ ] 5.2 「概要」セクションを記載（このサンプルアプリが何を示すか）
- [ ] 5.3 「必要環境」セクションを記載（Visual Studio 2022 17.13+ / Rider / .NET 9 SDK / Xcode 16+ / Android SDK API 29+）
- [ ] 5.4 「開き方」セクションを記載（Visual Studio / Rider / `dotnet` CLI のいずれか）
- [ ] 5.5 「実行手順」セクションを記載（`dotnet build -t:Run -f net9.0-ios` / `-f net9.0-android` の両方）
- [ ] 5.6 「ディレクトリ構成」セクションを記載（簡易ツリー）
- [ ] 5.7 「関連リンク」セクションを記載（`add-maui-bridge` / `add-maui-core` / `add-maui-cells` 提案 / `docs/migration-from-aiforms.md`（`add-maui-cells` で整備予定）などへのリンク）
- [ ] 5.8 「本体ライブラリのデバッグ」セクションを記載：本 Sample は `KsSettingsView.Maui` および MAUI バインディングプロジェクトを `<ProjectReference>` で参照するため、本体ソースにブレークポイントを置いてステップイン可能。本体テスト（Snapshot テスト等）を主軸に走らせる場合は `KsSettingsView.slnx` ソリューション全体を IDE で開く運用と、Sample で動作確認しながら本体を編集する場合も同ソリューション内から Sample プロジェクトを起動する運用、両者を使い分ける旨を明記

## 6. 動作確認（iOS）

- [ ] 6.1 `cd samples/maui && dotnet build -f net9.0-ios` がコマンドラインから成功することを確認
- [ ] 6.2 `dotnet build -t:Run -f net9.0-ios` で iOS シミュレータ起動を確認
- [ ] 6.3 起動直後の画面に `LabelCell` の `Title` が複数行（3 行）描画されることを目視確認
- [ ] 6.4 Section ヘッダ "PoC Section" と Section フッタ "This is a footer" が描画されることを目視確認

## 7. 動作確認（Android）

- [ ] 7.1 `cd samples/maui && dotnet build -f net9.0-android` がコマンドラインから成功することを確認
- [ ] 7.2 `dotnet build -t:Run -f net9.0-android` で Android エミュレータ起動を確認
- [ ] 7.3 起動直後の画面に `LabelCell` の `Title` が複数行（3 行）描画されることを目視確認
- [ ] 7.4 Section ヘッダ "PoC Section" と Section フッタ "This is a footer" が描画されることを目視確認

## 完了条件

- すべてのタスクのチェックボックスが完了している
- `samples-maui` capability の全 Scenario が満たされている
- `samples/maui/` を Visual Studio / Rider / `dotnet` CLI のいずれかで開き、iOS シミュレータと Android エミュレータの両方で起動して `LabelCell` を含む 1 セクションのデモ画面が描画される
- `samples/maui/README.md` が実 Sample 用のクイックスタートに置き換わっている
- `dotnet build -f net9.0-ios` および `dotnet build -f net9.0-android` の両方が成功する

# 検証結果: add-maui-samples-foundation (001 回目)

**日付**: 2026-08-09
**判定**: VALID

デルタスペック 2 capability (samples-maui / maui-bridge) の全 Requirement / Scenario を、実装コードと**自分で再実行した restore・ビルド・両 OS 実機起動**に突き合わせた。deviation.md は存在せず、未記録の乖離もない。

---

## samples-maui

### Requirement: サンプルアプリの成立

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| 両 OS でビルドできる | `samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj:9` (`net10.0-ios;net10.0-android`)、`:40` (ProjectReference 1本)、`MauiProgram.cs:17` (`AddKsSettingsView()`) | 実行: `dotnet build -f net10.0-android -c Debug` → 成功 (警告 7 / エラー 0、うち 6 件は Binding.Android の既存 BG86xx)。`DEVELOPER_DIR=Xcode-26.1.1` で `-f net10.0-ios -c Debug` → 成功 (警告 1 / エラー 0) | ✅ 一致 |
| シミュレータ / エミュレータで起動できる | 同上 + `App.cs:13` (`NavigationPage(new MenuPage())`) | iOS: `simctl install/launch` (iPhone 17 Pro) でデモ一覧を表示 (スクリーンショット確認)。Android: `-t:Run -p:AdbTarget="-s emulator-5554"` で `mResumedActivity: jp.kamusoft.kssettingsview.samples.maui/…MainActivity` を確認、一覧を表示 | ✅ 一致 |

補足: `ApplicationId` = `jp.kamusoft.kssettingsview.samples.maui` (csproj:17) は `cross/conventions/public-identifiers.md` の `jp.kamusoft.kssettingsview.samples.*` 規約に一致。`KsSettingsView.slnx` への登録も確認 (`maui/KsSettingsView.slnx` の `/samples/` フォルダ)。

### Requirement: デモ一覧と画面遷移

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| 項目選択で遷移しタイトルが一致する | `SampleScreen.cs:65-70` (`CreateTitledPage()` が `page.Title = Title` を代入)、`MenuPage.cs:55` (項目文言も同じ `Title`)、`MenuPage.cs:59-71` (`PushAsync`) | iOS: 一覧「LabelCell 検証」タップ → ページタイトル「LabelCell 検証」で一致、戻るボタンで一覧へ復帰、再選択も可能 (`SelectedItem = null` の解除が効いている)。Android: 同様に遷移・TopAppBar タイトル一致・戻る動作を確認 | ✅ 一致 |
| 検証画面はデモと区別される | `SampleScreen.cs:10-17` (`SampleScreenCategory.Demo` / `.Verification`)、`:78-88` (`SampleScreenGroup.Name` = 「デモ」/「検証」)、`MenuPage.cs:26-27` (`IsGrouped` + `GroupHeaderTemplate`) | 両 OS のスクリーンショットで見出し「検証」の下に「LabelCell 検証」が置かれることを確認。文言は iOS Sample (`ContentView.swift:17,23`) / Android Sample (`MenuScreen.kt:45`) の「デモ」「検証」と一致 | ✅ 一致 |

### Requirement: LabelCell 検証ページ

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| Section と LabelCell が表示される | `Pages/LabelCellVerificationPage.xaml:14` (`HeaderText="表示"` / `FooterText="端末の設定より優先されます"`)、`:15-22` (LabelCell 3行。Title/ValueText 全行、Description 1行、HintText 1行) | 両 OS のスクリーンショットで header「表示」・footer「端末の設定より優先されます」・3行 (テーマ/ライト+配色の設定、文字サイズ/標準+端末の設定に従う、言語/日本語) をすべて判読。native 描画 (iOS: グループ化テーブル、Android: RecyclerView) で表示されている | ✅ 一致 |
| 値の変更が表示へ反映される | `ViewModels/LabelCellVerificationViewModel.cs:23` (`ReactivePropertySlim<string> ThemeName`)、`:20,28-32` (`ReactiveCommandSlim` + `NextTheme`)、XAML `:17` (`ValueText="{Binding ThemeName.Value}"`)、`:25-28` (更新ボタン) | 両 OS で「テーマを切り替える」タップ → テーマ行の値が「ライト」→「ダーク」へ更新されることをスクリーンショット前後比較で確認 | ✅ 一致 |

補足: 「phase-4 の基本 Cell デモページ追加をもって削除される暫定画面」という spec 上の申し送りは、`kasane/roadmaps/maui-support/phases/phase-4-basic-input-cells/agenda.md:15-16` に登録済み (sample-parity の「追跡を残す」要件を満たす)。

### Requirement: クイックスタート README

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| README だけで実行に到達できる | `samples/maui/README.md` (概要 / 収録画面 / 必要環境 / Xcode・Android SDK 設定 / 開き方 / iOS・Android の CLI 手順 / 依存関係 / ディレクトリ構成 / 画面の追加手順) | placeholder は完全に置換済み。iOS 手順 (`simctl list` → `dotnet build -f net10.0-ios` → `simctl install/launch`) は記載のパス `bin/Debug/net10.0-ios/iossimulator-arm64/KsSettingsView.Sample.Maui.app` を含めそのまま通った。Android 手順 (`dotnet build -f net10.0-android -t:Run -p:AdbTarget=…`) もそのまま通った。ただし `README.md:49,104` の `ANDROID_HOME="$HOME/Library/Android/sdk"` は本開発機に存在せず (実 SDK は同 `:51` にコメントで併記された Xamarin パス)、`:106` の `"$ANDROID_HOME/emulator/emulator"` は記載どおりだと失敗する | ✅ 一致 (要求は満たす。⚠️ 精度は review-001.md 🟡 Minor 1) |

判定根拠: Requirement は「必要環境・開き方・両 OS での実行手順 (CLI コマンド含む)・依存関係の説明を記載する」であり、これは満たしている。Scenario の「追加の調査なしに到達」も、代替 SDK パスが同一ブロックにコメント併記されているため到達自体は可能。精度の問題として review 側の Minor に回し、❌ とはしない。

**追記 (同日・修正後の再確認)**: review-001.md 🟡 Minor 1 を受けて `samples/maui/README.md` が修正された (「必要環境」から `ANDROID_HOME` 要件を削除 / `ANDROID_HOME` は emulator・adb を CLI から直接起動する場合のみ必要でビルドには不要と明記 / SDK パスを Android Studio 既定と Xamarin 流用の同格併記 / Android 実行手順から固定 export を削除)。再検証で `ANDROID_HOME` を unset した状態の `dotnet build -f net10.0-android` 成功、Xamarin パスを採った場合の `emulator -list-avds` / `adb` / `"$ANDROID_HOME/emulator/emulator"` の到達をいずれも確認。上記行番号 (`:49,104,106`) は修正前のもの。Scenario の判定は ✅ のまま変わらない。

---

## maui-bridge

### Requirement: AndroidX Lifecycle 依存の binding 層整合

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| ピンなしの利用側が警告なしで restore できる | `maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj:57-64` (`Xamarin.AndroidX.Lifecycle.LiveData 2.11.0.1` の明示宣言 + 理由コメント) | `dotnet restore samples/…/KsSettingsView.Sample.Maui.csproj --force` を実行 → NU1608 / NU1107 ともに 0 件。`samples/maui/KsSettingsView.Sample.Maui/obj/project.assets.json` 上で `LiveData` / `LiveData.Core` / `LiveData.Core.Ktx` がいずれも **2.11.0.1**。Sample 側 csproj に AndroidX の `PackageReference` も `NoWarn` も無いことを確認 | ✅ 一致 |
| MauiHost のピン削除後も整合が保たれる | `maui/tests/KsSettingsView.MauiHost/KsSettingsView.MauiHost.csproj` (`LiveData.Core(.Ktx)` の `ItemGroup` と誤コメントを削除。`NoWarn` は 0 件) | `--force` restore → NU1608 なし、assets 上の LiveData family = 2.11.0.1。`dotnet build -f net10.0-android -c Debug` → 成功 | ✅ 一致 |

**回帰検出力の実証 (mutation probe)**: `lessons/code-review.md [L-001]` に従い、Binding.Android の追加 `PackageReference` を一時的に取り除いて `--force` restore したところ、

- `warning NU1608` (LiveData 2.9.2.1 が Core / Core.Ktx を `[2.9.2.1, 2.9.3)` に縛るのに 2.11.0.1 が解決された) が再出現
- `error NU1107` (LiveData.Core のバージョン競合。「2.11.0.1 を Sample へ直接インストールせよ」)
- assets 上の `LiveData` が **2.9.2.1** へ後退

を確認した。したがって本 Scenario は `NoWarn` によるごまかしでも既存状態のトートロジーでもなく、binding 層の宣言が原因で成立している。一時変更は backup から復元し `shasum` 一致 (`4b6c2cacca703e5ee4cb48b18987da8bcc26b893`) を確認済み。

**スコープ注記の遵守**: spec の「NuGet パッケージ参照経由の利用者への効果は本変更では検証しない」に従い、pack 経由の検証は行っていない (仕様上の非対象)。合意済み fallback (`NoWarn` + README 記録) は**発動していない** — primary 案が実証で成立したため。NU1107 も実発生しなかった (mutation 時のみ発生)。よって deviation.md への記録事由なし。

---

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md 全タスク完了 | 1.1〜4.4 の 12 件すべて `[x]`。虚偽チェックなし — 上記対応表のとおり全件を独立に再実行して裏付けた。4.4 のみ、Android の `ANDROID_HOME` 記載が本開発機の実パスと食い違うため「記載どおりそのまま」ではなく併記の代替パスでの到達 (review-001.md 🟡 Minor 1) |
| 逆流検査 (足場の書き換え) | `git status` 上、`kasane/changes/add-maui-samples-foundation/` で変更されているのは `tasks.md` のみ。`proposal.md` / `specs/samples-maui/spec.md` / `specs/maui-bridge/spec.md` / `second-opinion-001.md` は未変更。`tasks.md` の diff もチェックボックス 12 行のみで本文改変なし |
| 未記録乖離 | ❌ 0 件のため該当なし。deviation.md は存在せず、作成すべき事由も見つからなかった |
| UI 変更の brief / mock | 本 change に `ui/` は無い。`second-opinion-001.md` の突き合わせ表 (Major 2) でオーナーが「ui/ なし」を承認済みと記録されており、合意済みの判断として扱う |
| テスト全件成功 | `dotnet test maui/KsSettingsView.Maui.Tests` → **115 件全成功 / 失敗 0 / スキップ 0**。サンプル専用の自動テストは tasks.md 末尾の注のとおり非設置 (合意済み) |
| コメント規約 lint | `python3 scripts/comment-policy-lint.py` → 禁止 0 件 (466 ファイル)、`--selftest` 全件 OK。新規 Sample ファイルは未追跡のため lint 対象外だが、目視で確認したところ参照は `maui/ADR-0010` / `cross/ADR-0016` / リポジトリ内ソースファイル名のみで規約準拠 |

---

## 判定

**VALID** — ❌ 0 件。デルタスペック 2 capability・6 Scenario すべてが実装とライブ検証で裏付けられ、虚偽チェック・逆流・未記録乖離・テスト失敗のいずれも無い。品質面の指摘 (Minor 4 / Suggestion 3) は `review-001.md` を参照。

## 追記: レビュー指摘修正後の再検証 (同日)

review-001.md の Minor 1 / 2 / 4 に対する修正 (README の `ANDROID_HOME` 記述、MAUI パッケージの明示宣言、MAUI 10.0.70 統一、csproj コメント位置) が入った後、全 Scenario を再検証した。**判定は VALID のまま変わらない。**

| Scenario | 再検証結果 |
|---|---|
| 両 OS でビルドできる | `dotnet build maui/KsSettingsView.slnx -c Debug` 成功。Sample 単体は両 TFM とも **0 警告** (MA002 消滅) |
| シミュレータ / エミュレータで起動できる | iOS (iPhone 17) / Android (Pixel_6) で再インストール・起動。クラッシュなし |
| 項目選択で遷移しタイトルが一致する | 両 OS で確認 |
| 検証画面はデモと区別される | 両 OS で「検証」見出しを確認 |
| Section と LabelCell が表示される | 両 OS で header/footer + 3 行の各フィールドを判読 |
| 値の変更が表示へ反映される | 両 OS で「ライト」→「ダーク」の反映を確認 |
| ピンなしの利用側が警告なしで restore できる | `restore --force` で NU1605 / NU1608 / NU1107 / MA002 いずれも 0 件。assets 上の LiveData family = 2.11.0.1 |
| MauiHost のピン削除後も整合が保たれる | 同上。MauiHost の assets も LiveData family 2.11.0.1、`Microsoft.Maui.Controls` = 10.0.70 |
| (テスト) | `KsSettingsView.Maui.Tests` 115 件全成功 |

**MAUI 10.0.70 統一が maui-bridge Scenario を空洞化していないことの再実証**: mutation probe を 10.0.70 の状態で再実行し、binding 層の宣言を外すと `Microsoft.Maui.Core 10.0.70 -> Xamarin.AndroidX.Lifecycle.LiveData 2.9.2.1` の連鎖で NU1608 2 件 + NU1107 が再発することを確認。10.0.70 でも競合の前提は変わらず、Scenario は依然として binding 層の宣言を測っている。

**deviation.md**: MAUI パッケージ明示宣言・10.0.70 統一・MauiHost 随伴修正の 3 点が記録済み。いずれも spec が沈黙する範囲のオーナー指示であり、合意済み差分として扱う。未記録の乖離は引き続き 0 件。

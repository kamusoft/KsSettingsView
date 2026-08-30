# レビュー結果: add-maui-samples-foundation (001 回目)

**日付**: 2026-08-09
**判定**: APPROVED

## サマリー

デルタスペック 2 capability の要求はすべて満たされており、両 OS のビルド・実機 (シミュレータ / エミュレータ) 起動・画面遷移・バインディング更新まで自分で再実行して確認した。特に AndroidX 依存の binding 層吸収は mutation probe で回帰検出力まで実証でき、`NoWarn` によるごまかしでないことがはっきりしている (詳細は `verify-001.md`)。指摘は Minor 3 / Suggestion 3 で、いずれも仕様充足・堅牢性には影響しない周辺の精度・追随の問題。

## 指摘事項

### [🟡 Minor] README の `ANDROID_HOME` 既定例が本リポジトリの実環境と食い違い、記載どおりの emulator 起動が失敗する

**該当箇所**: `samples/maui/README.md:49`、`samples/maui/README.md:104`、`samples/maui/README.md:106`

**問題点**: README は `export ANDROID_HOME="$HOME/Library/Android/sdk"` を主手順として示し、Xamarin (旧 VS) の SDK パスは 51 行目のコメントアウトに留めている。しかしこの開発機では `$HOME/Library/Android/sdk` は存在せず、実 SDK は `$HOME/Library/Developer/Xamarin/android-sdk-macosx` の側だけである。記載どおりに export すると 106 行目の `"$ANDROID_HOME/emulator/emulator" -avd <AVD 名>` が `no such file or directory` で失敗する (実測)。

`dotnet build -f net10.0-android` 自体は MSBuild が SDK を自前解決するため `ANDROID_HOME` が誤っていても成功することも実測で確認した。つまり `ANDROID_HOME` が本当に必要なのは emulator / adb を直接叩くときだけで、README の「必要環境」(30 行目) が `ANDROID_HOME` の設定をビルドの前提のように書いているのも実態とずれている。

tasks 4.4「README 記載の CLI コマンドをそのまま実行して両 OS の起動に到達できることを確認する」は、この点で「そのまま」実行されていない疑いがある。

**推奨修正**: どちらか一方を既定に見せる書き方をやめ、「`ANDROID_HOME` は自分の環境の SDK パスに合わせる (よくある場所: A / B)」の形にする。あわせて `ANDROID_HOME` はエミュレータ / adb を直接起動する場合にだけ必要で、`dotnet build -t:Run` には不要である旨を分けて書く。

### [🟡 Minor] Sample が MA002 警告を毎ビルド出す — 雛形として警告付きの構成を配ることになる

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj:35-41`、`samples/maui/README.md:150-151` (「`Microsoft.Maui.Controls` は … 本 Sample の csproj では宣言していません」)

**問題点**: iOS / Android 両 TFM で MAUI SDK が `warning MA002` を出す (「`<UseMaui>true</UseMaui>` は NuGet 参照を自動で含めない。`<PackageReference Include="Microsoft.Maui.Controls" Version="10.0.1" />` を追加せよ」)。README はこの省略を設計意図として明記しているが、SDK 側は明示宣言を要求しており、両者が食い違ったまま警告として残っている。本 Sample は「利用者と同じ経路で組み込む」参照実装 (csproj:4-6 のコメント) なので、これを雛形にした利用者はそのまま警告を引き継ぐ。

既存の `maui/tests/KsSettingsView.MauiHost` も同じ状態で、本変更が新たに持ち込んだ回帰ではない (両方で MA002 を実測)。

**推奨修正**: どちらかに寄せて警告を消す。(a) `Microsoft.Maui.Controls` を `KsSettingsView.Maui` と同版で明示宣言し、README の依存関係表もそれに合わせる。(b) 推移参照で足りるという設計判断を維持するなら `<SkipValidateMauiImplicitPackageReferences>true</SkipValidateMauiImplicitPackageReferences>` を理由コメント付きで置き、README の説明と一致させる。MauiHost も同時に揃えるかは別途判断でよい。

**追記 (同日・修正後の再確認)**: 推奨 (a) の方向で、オーナー指示により `<MauiVersion>10.0.1</MauiVersion>` + `Microsoft.Maui.Controls` / `Microsoft.Maui.Controls.Compatibility` の明示宣言が入り、README の「宣言していません」記述も削除された (`deviation.md` に合意済み差分として記録)。再検証で両 TFM **0 警告**・restore で NU1608 / NU1107 なし・LiveData family 2.11.0.1 維持・iOS 実機で一覧→遷移→バインディング更新まで正常動作を確認。**解消**。ただし下記 2 点を後続の判断材料として残す。

- csproj のコメント位置ずれ (下記 🟡 Minor 4)
- `Microsoft.Maui.Controls.Compatibility` は MA002 解消には不要 — probe で当該 1 行だけを外してビルドしても 0 警告のまま成功し、コード側に `.UseMauiCompatibility()` も Compatibility 型の使用もない (現状は未初期化・未使用の参照)。オーナー指示かつ deviation 記録済みのため違反としては扱わないが、「MA002 のために必須」ではない事実は記録しておく
- `MauiVersion` = 10.0.1 は `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:31` のピンおよび SDK バンドル版と一致。今後 MAUI 版を上げる際は 2 箇所を同時に動かす必要がある (整合維持点が増えた)

### [🟡 Minor] csproj のコメントが 1 つ下の ItemGroup を説明していない (Minor 2 修正で混入)

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj:32-40`

**問題点**: 「ViewModel の通知プロパティとコマンドに使う軽量版。… full 版は参照しない。」という ReactiveProperty 向けの説明コメント (32-35 行) が、MAUI パッケージの `ItemGroup` (37-40 行) の直上に取り残されている。実際の `ReactiveProperty.Core` は 42-44 行の別 `ItemGroup` にあり、コメントと対象がずれた。36 行に MAUI 用のコメントが後から挿入された結果、コメントが 2 行連続して 1 つの `ItemGroup` に掛かっている状態でもある。

`cross/conventions/comment-policy.md` の「そのファイルだけを読んでいる人にとって意味が通ること」に反する。機械 lint は参照禁止パターンのみを見るため、この種の位置ずれは検出されない。

**推奨修正**: ReactiveProperty のコメントを 42 行の `ItemGroup` 直上へ移す。

**追記 (同日・修正後の再確認)**: コメントが `ReactiveProperty.Core` の `ItemGroup` 直上 (現 38-44 行) へ移され、MAUI 用コメントは MAUI の `ItemGroup` 直上 (現 32-36 行) に収まった。**解消**。

## 追記: MAUI 10.0.70 統一の再確認 (同日)

Minor 2 の修正後、オーナー指示により `Microsoft.Maui.Controls` を全 MAUI プロジェクトで 10.0.70 へ統一する追加変更が入った (`deviation.md` に合意済み差分として記録)。以下を自分で再実行して確認した — **問題なし**。

- **バージョン指定箇所の網羅**: リポジトリ全体を走査し、MAUI 版を指定する箇所は 4 つ (`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:31` / `maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj:18` / `maui/tests/KsSettingsView.MauiHost/KsSettingsView.MauiHost.csproj:36` / `samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj:21` の `MauiVersion`)。すべて 10.0.70 で揃っている。**見落とし箇所なし**
  - `maui/spike/app/KsBindingSpikeApp/KsBindingSpikeApp.csproj:65` は `$(MauiVersion)` を自前設定せず SDK バンドル版に従うが、`KsSettingsView.slnx` に含まれず `KsSettingsView.Maui` も参照しない (参照先は BindingSpike 2 本のみ) ため NU1605 の経路にならない
  - `maui/tests/KsSettingsView.IntegrationHost.{iOS,Android}` は Binding 層のみ参照で MAUI 非依存。影響なし
- **solution 全体の restore / build**: `dotnet restore maui/KsSettingsView.slnx --force` → NU1605 / NU1608 / NU1107 / MA002 いずれも 0 件。`dotnet build maui/KsSettingsView.slnx -c Debug` → 成功、警告 6 件はすべて Binding.Android の既存 BG8605 / BG8606 / BG8A00 (本変更前から存在)
- **assets 実測**: Maui / Maui.Tests / MauiHost / Sample の 4 プロジェクト全 TFM で `Microsoft.Maui.Controls` = **10.0.70**、`LiveData` / `.Core` / `.Core.Ktx` = **2.11.0.1**
- **maui-bridge Scenario への影響 (トートロジー化の検査)**: 10.0.70 で `Microsoft.Maui.Core` の LiveData 要求版が上がっていれば binding 層の宣言は no-op になり Scenario が空回りする。mutation probe で binding の宣言を外して restore したところ `Microsoft.Maui.Controls 10.0.70 -> Microsoft.Maui.Controls.Core 10.0.70 -> Microsoft.Maui.Core 10.0.70 -> Xamarin.AndroidX.Lifecycle.LiveData 2.9.2.1` と表示され、NU1608 2 件 + NU1107 が再発。**10.0.70 でも要求版は 2.9.2.1 のままで、binding 層の宣言は依然 load-bearing**。maui/ADR-0010 の Context (「10.0.1〜10.0.90 で同一」) と Alternatives (「10.0.90 でも自然消滅しない」) の記述も引き続き正しい。probe 後は backup から復元し shasum 一致 (`4b6c2cacca703e5ee4cb48b18987da8bcc26b893`) を確認
- **テスト**: `dotnet test maui/KsSettingsView.Maui.Tests` → 115 件全成功
- **実機再確認 (MAUI ランタイム更新の影響)**: iOS シミュレータ (iPhone 17) / Android エミュレータ (Pixel_6) の両方で再インストール・起動 → 一覧「検証 / LabelCell 検証」→ 遷移・タイトル一致 → Section header/footer と 3 行の LabelCell 表示 → 「テーマを切り替える」で ValueText が「ライト」→「ダーク」へ反映。両 OS ともクラッシュ・描画崩れなし。Sample の Android ビルドは **0 警告**
- **MauiHost の随伴修正**: `Microsoft.Maui.Controls 10.0.70` の明示宣言は NU1605 回避のため必須で、理由コメントも csproj に残っている。副作用で MauiHost の MA002 も解消し、Sample と足並みが揃った (先の「MauiHost はスコープ外」整理より良い状態に収束)

### [🟡 Minor] ルート README の `samples/maui/`「未着手」表記が古くなった

**該当箇所**: `README.md:33` (`| `samples/maui/` | .NET MAUI サンプルアプリ（未着手） |`)

**問題点**: 本変更で MAUI Sample が実装されたため、リポジトリ地図の記述が事実と食い違う。同様に `kasane/concepts/cross/conventions/sample-parity.md:23` の「`samples/maui` は未実装 — README のみ」も古くなる。

**推奨修正**: ルート README の書き換えは AGENTS.md により `docs-refresh` 経由に限られるため、この change の実装ワークで直接触らないこと。蒸留 (ksn-distill) で `sample-parity.md` の該当記述を更新したうえで、オーナーへ `docs-refresh` の実行を申し送る形にするのが規約に沿う。追跡が切れないよう、申し送り先 (tasks.md 追記か phase agenda) を決めておきたい。

### [🔵 Suggestion] `concepts/maui/api/maui-facade.md` の既知課題記述が本変更で部分的に解消済み

**該当箇所**: `kasane/concepts/maui/api/maui-facade.md:53`

**問題点**: 「配布は ProjectReference のみ (NuGet パッケージングは別途。AndroidX Lifecycle の版競合 NU1608 / NU1107 が既知の課題)」とあるが、ProjectReference 経路では本変更で解消済み (verify-001.md で実証)。NuGet パッケージ経路は spec のスコープ注記どおり未検証のまま残る。

**推奨修正**: 蒸留時に「ProjectReference 経路は binding 層の明示宣言で解決済み (maui/ADR-0010)。NuGet 経路は未検証」へ書き分ける。ADR-0010 の `proposed` → `accepted` 昇格もこのタイミング。

### [🔵 Suggestion] `MenuPage.OnSelectionChanged` の選択解除を await 後に置いている

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/MenuPage.cs:59-71`

**問題点**: `await Navigation.PushAsync(...)` の**後**に `list.SelectedItem = null` を置いているため、push アニメーション中は行がハイライトされたまま残る。また `async void` ハンドラなので `PushAsync` が投げた例外は未処理例外になる。実測では「遷移 → 戻る → 同じ行を再選択」が両 OS で正常に動くことを確認済みで、現状 1 画面では実害はない。

**推奨修正**: 必須ではない。画面が増える phase-4 以降を見据えるなら、選択解除を `await` の前に移す方が素直 (`SelectedItem = null` は `CurrentSelection.Count == 0` の早期 return に落ちるだけで再入しない)。

### [🔵 Suggestion] ViewModel が `ReactivePropertySlim` / `ReactiveCommandSlim` を破棄しない

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/ViewModels/LabelCellVerificationViewModel.cs:19-26`

**問題点**: 両者とも `IDisposable` だが、ViewModel は `IDisposable` を実装せずページ側も破棄しない。所有関係がページ内で閉じているためリークにはならない (Page → VM → property/command のみで外部購読なし)。ただし本 Sample は ReactiveProperty の初出であり、利用者が雛形にする箇所でもある。

**推奨修正**: 必須ではない。後続フェーズで購読 (`Subscribe`) を伴う ViewModel が出てきた時点で、`CompositeDisposable` + `IDisposable` 実装の作法を Sample 側で一度示しておくと、以後のページが自然に倣える。

## 確認した観点 (指摘なし)

- **仕様充足**: デルタスペック 2 capability・6 Scenario すべて充足。足場アーティファクト (proposal / specs) の書き換えなし。tasks.md の diff はチェックボックスのみ。deviation.md なし・作成事由なし (詳細は `verify-001.md`)
- **テスト**: `dotnet test maui/KsSettingsView.Maui.Tests` → 115 件全成功。サンプル専用テスト非設置は tasks.md の合意どおり
- **AndroidX 吸収の実効性**: mutation probe で binding 層の宣言を外すと NU1608 再出現 + NU1107 エラー + assets 上の LiveData が 2.9.2.1 へ後退することを実測。`NoWarn` によるごまかしでもトートロジーでもない
- **sample-parity (cross/ADR-0016)**: ルートメニュー題「KsSettingsView Sample」・区分見出し「デモ」「検証」が iOS (`ContentView.swift:17,23,29`) / Android (`MenuScreen.kt:45`) と一字一句一致。デモ画面ゼロという片側先行は `phase-4-basic-input-cells/agenda.md:15-16` に追随 (LabelCell 検証ページの削除・置換 / Store・DSL デモの要否) が登録済みで、規約の「追跡を残す」要件を満たす
- **public-identifiers**: `ApplicationId` = `jp.kamusoft.kssettingsview.samples.maui`、`RootNamespace` = `KsSettingsView.Sample.Maui` はいずれも規約どおり
- **comment-policy**: `comment-policy-lint.py` 禁止 0 件・`--selftest` 全件 OK。新規 Sample ファイル (未追跡のため lint 対象外) も目視で確認 — 外部参照は `maui/ADR-0010` / `cross/ADR-0016` とリポジトリ内ソースファイル名のみ
- **Android テーマ要件**: `AndroidManifest.xml` と `MainActivity` の双方で `Theme.Material3.DayNight.NoActionBar` を指定 (MaterialSwitch の `materialSwitchStyle` 要求に対応)。MauiHost と同一構成
- **拡張点の設計**: `SampleScreen.All` への 1 件追加で一覧・区分・遷移・タイトルがすべて追随する構造 (`CreateTitledPage()` がタイトルを一元定義から与えるため、ページ側にタイトルを書く余地がない)。`SampleScreenCategory.Demo` は現時点で未使用だが phase-4 の拡張点として妥当
- **成果物の混入**: `bin/` `obj/` は untracked に現れず、新規追加は 14 ファイルのみ

## アクションプラン

1. (任意・低優先) 🟡 Minor 1: README の `ANDROID_HOME` 記述を「環境に合わせて選ぶ」形へ直し、ビルドに不要である点を切り分ける
2. (任意・低優先) 🟡 Minor 2: MA002 を明示宣言または `SkipValidateMauiImplicitPackageReferences` のどちらかで解消し、README の説明と一致させる
3. (蒸留フェーズ) 🟡 Minor 3 / 🔵 Suggestion 1: `sample-parity.md` の「samples/maui は未実装」と `maui-facade.md:53` の既知課題記述を更新し、ADR-0010 を accepted へ昇格。ルート README は `docs-refresh` ゲートへ申し送る
4. (phase-4 以降で検討) 🔵 Suggestion 2 / 3: 選択解除の位置、ReactiveProperty の破棄作法

いずれも APPROVED を妨げない。1・2 は本 change 内で直しても、後続へ回しても構わない。

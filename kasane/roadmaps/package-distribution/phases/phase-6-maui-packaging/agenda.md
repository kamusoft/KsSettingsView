# phase-6-maui-packaging

MAUI facade の名前空間を `KsSettingsView` に改め、facade + binding 2 件の 3 NuGet パッケージとして pack できる形にする (maui/ADR-0025)。

## 論点

(全論点解消済み — 決定事項へ移動)

## 決定事項

### 名前空間改名は「名前空間に `.Maui` を含めない」の 1 ルールでテストまで揃える

facade の名前空間を `KsSettingsView` (配下 `.Internals` / `.Handlers`) に改め、テストプロジェクトの自前名前空間も `KsSettingsView.Tests` (`.Fakes` / `.Support`) に揃える。アセンブリ名 (`KsSettingsView.Maui` / `KsSettingsView.Maui.Tests`) と `InternalsVisibleTo` は変えない。利用側 (テスト・検証ホスト MauiHost・Sample) は using と XAML xmlns (`clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui`) の追随のみ。IntegrationHost 2 件は facade 名前空間を直接参照しておらず対象外、binding の `KsSettingsView.Bridge` は不変 (maui/ADR-0025)。改名は phase-5 の前例に倣い、同じ change の最初のタスク (独立コミット) として行う (2026-09-02)。

### 共通メタデータは `maui/Directory.Build.props`、版は `maui/Directory.Packages.props` (CPM) に集約する

nuget.org の必須要件はライセンスのみ (icon / readme / snupkg は任意) と確認した上で、次の内容にする (2026-09-02)。

| 項目 | 値 | 根拠 |
|---|---|---|
| Authors | `kamusoft` | LICENSE・KsDialogs LICENSE・Android POM developers と一致 (レジストリの表示名にはブランド名を使う慣習) |
| Company | `kamusoft LLC` | 表示に出ない AssemblyCompany 属性に法人名を記録する |
| Copyright | `Copyright (c) kamusoft` | LICENSE と同一 (正式名化は別作業、TODO 参照) |
| PackageLicenseExpression | `MIT` | nuget.org は expression 推奨 |
| RepositoryUrl / RepositoryType / PackageProjectUrl | `https://github.com/kamusoft/KsSettingsView` / `git` | cross/ADR-0021 |
| Version 既定値 | `0.0.0-dev` | cross/ADR-0020 (CI が `-p:Version=` で注入、bump コミットを積まない) |
| IsPackable 既定 | `false` (facade と binding 2 件の csproj でのみ `true`) | テスト・検証ホストの pack 混入防止 |
| SourceLink / snupkg | `PublishRepositoryUrl=true` + `IncludeSymbols=true` + `SymbolPackageFormat=snupkg` | SDK 同梱の SourceLink で追加パッケージ不要。`ContinuousIntegrationBuild` は CI 側 (phase-8) で注入 |
| PackageIcon | `assets/icon.png` (AiForms.Maui.SettingsView の icon.png 300×300 PNG をコピー) を 3 パッケージ共通で同梱 | 原典の継承 |
| PackageReadmeFile | facade のみ、ルート `README.md` をそのまま同梱 (2026-09-02 訂正) | package README の新設は cross/ADR-0023 (README は 4 枚のみ、platform 別 README の新設は ADR 改訂を要する) と衝突するため撤回。スクリーンショットの参照を public リポジトリの絶対 URL に改めて nuget.org でも表示できるようにする (README 編集は change に同梱) |

CPM は `ManagePackageVersionsCentrally=true` で `Microsoft.Maui.Controls` 10.0.70、AndroidX 系 14 本、テスト系 3 本 (NUnit 等) の版を集約する。Sample は `maui/` の外にあり影響を受けない。binding csproj にある版整合の長いコメント (Compose BOM 対応・Material 1.12 固定) は版の隣に居るべき知識として Packages.props 側へ移す。

### binding の pack 設定は既定に任せ、Description は英語で「直接参照しない」を明記する

3 プロジェクトの csproj に `IsPackable=true` を明示する (props の既定は false)。facade は `PackageId` を `KsSettingsView.Maui` と明示し (ADR-0025 の名前空間との非対称を csproj で読めるように)、binding 2 件の `PackageId` は既定 (アセンブリ名 `KsSettingsView.Binding.iOS` / `.Android`) に任せる。Description は英語で、facade は Android POM の文言を MAUI 向けに翻案し AiForms.Maui.SettingsView の後継であることに触れる ("A settings screen UI library for .NET MAUI, providing list-style settings screens with built-in cell types on iOS and Android. Successor to AiForms.Maui.SettingsView.")。binding は "iOS native bridge binding for KsSettingsView.Maui. Referenced transitively by KsSettingsView.Maui; do not reference this package directly." (Android は読み替え)。`PackageTags` は facade のみ (`maui settings settingsview ios android` 程度) で、binding には付けず検索で先に出ないようにする (2026-09-02)。

### pack は SDK 標準経路のみで成立 (PoC 実測)、manifest 絶対パス・依存版の下限指定・API 版付き TFM は SDK 挙動を受け入れる

PoC ([artifacts/pack-poc.md](artifacts/pack-poc.md)) で 3 パッケージが `dotnet pack` だけで成立し、facade の TFM 別依存への自動変換・facade への native 混入なし・aar 2 本と device + simulator 両スライスの同梱・Exec 経由 gradlew (maui/ADR-0006) との整合をすべて確認した。見つかった 3 点は自作の pack 用 MSBuild を足さず SDK 挙動のまま受け入れる: (a) iOS manifest の絶対パスは `manifest` 1 ファイルのみで、リリースは CI で pack するため個人環境の情報は載らない (ADR-0025 受容済み)。(b) facade → binding の依存版は下限指定だが、lockstep (cross/ADR-0019) の同時発行と NuGet の最小適用版解決で利用者は同版の binding を得る。(c) TFM が `net10.0-android36.0` / `net10.0-ios26.0` と API 版付きになるのは SDK 10.0.300 の既定で、古い TargetPlatformVersion を固定した利用者は解決不能になる — phase-7 の消費者検証と README 互換情報へ申し送る (2026-09-02)。

### AndroidX Lifecycle 競合は NuGet 経由でも解消、Release (trimming) の消費者ビルドも成立 (実測)

素の MAUI アプリ (SDK 10.0.300) にローカルフィードの facade 1 行を足した消費者 PoC で、restore は NU1608 / NU1107 とも 0 件、LiveData family 3 本が 2.11.0.1 に解決し、binding の明示宣言が推移的に効くことを確認した (maui/ADR-0010 の未検証項目「NuGet 経路の効果」が埋まる。蒸留時に ADR-0010 の Consequences へ追記する)。Release ビルドは Android (trimming + R8 + AOT) と iOS (simulator、IL strip + AOT) とも成功し IL2xxx / XA / MT 警告ゼロ、facade を型参照した状態でも成果物にアセンブリと AOT native が残る。`dotnet publish` (フル trimming) と実機起動は phase-7 の smoke で扱う (2026-09-02)。

### facade が要求する MAUI 本体の版は 10.0.70 (検証済み版) を下限として維持する

消費者 PoC で、テンプレート既定の `Microsoft.Maui.Controls` 10.0.20 に facade を足すと NU1605 (ダウングレード) で restore が失敗すると分かり、下限を下げる案を検討した。リポジトリのコピーを 10.0.20 に書き換えた実測ではビルド・516 件のテスト・利用側 iOS リンクはすべて通るが、MAUI のファイル画像解決 (`ImageSourceExtensions.GetPlatformImage`) が 10.0.30 以前は生のファイル名、10.0.60 以降は拡張子を落とした名前でバンドルを引くという版差があり、facade の iOS icon 所有権分類 (maui/ADR-0026) は後者の形に合わせているため、10.0.20 では安全側不変条件が保証されない (10.0.40 / 10.0.50 は未確認)。10.0.60 と 10.0.70 の差は利用者体験上同じで、10.0.70 は ADR-0026 の probe を実際に通した版であることから、検証済み版 10.0.70 を下限として維持する。README / skills に「`MauiVersion` は 10.0.70 以上」を明記する (docs-refresh 依頼に含める) (2026-09-02)。

### 最低 OS 版 (Android 29 / iOS 16.0) は facade 同梱のビルド時ガードで検査し、README にも明記する

消費者 PoC で、iOS はテンプレート既定 (15.0) のままでもエラー・警告なしでビルドが通り (`.app` の MinimumOSVersion は 15.0 のまま)、Android が 21 で落ちるのも本体要件ではなく依存 AndroidX の minSdk 23 の副作用 (23〜28 なら通る) と判明した。両 OS とも本体の最低 OS 版を誰も検査していないため、facade の NuGet に `buildTransitive/` 配下の .targets を同梱し、利用者側の `SupportedOSPlatformVersion` が要件未満なら要件を書いた読めるエラーで止める。要件の数値は props に集約して .targets と `SupportedOSPlatformVersion` の宣言元を 1 箇所にする。README / skills にも明記する (docs-refresh 依頼に含める)。この .targets は利用者ビルドの資産であり、ADR-0025 が避けた「pack 内部構造に依存する自作 MSBuild」ではないが、facade パッケージに build 資産が入る点は蒸留時に ADR-0025 の Consequences へ追記する (2026-09-02)。

### `KsSettingsView.slnx` は Sample を含めたまま維持する

検証 CI (verify-maui.yml) も pack も csproj を個別指定して動かし、slnx はエディタ用の入れ物でビルド・pack の単位ではない。Sample は `samples/maui/` にあり `maui/Directory.Build.props` / `Directory.Packages.props` の影響を受けず、facade への ProjectReference のままパリティ対象 (cross/ADR-0016) として残す (NuGet 経由の参照は phase-7 の `verification/` の役割)。pack 対象との分離は「pack は csproj 単位、slnx を pack しない」で成立しており、改名作業を 1 ソリューションで追える利点もある (2026-09-02)。

### ドキュメント追随は phase-5 と同じ 3 段仕分け (規範は change 同梱・記述は蒸留・利用者向けは明示依頼)

「maui README」は phase-9 のルート README 統合で存在しないため対象外 (内容は concepts が引き受け済み)。仕分けは次のとおり (2026-09-02)。

| 段 | 対象 | 追随内容 |
|---|---|---|
| 実装 change に同梱 | `kasane/handbook/cross/public-identifiers.md` | NuGet Package ID の行 (`KsSettingsView.Maui` / `.Binding.iOS` / `.Binding.Android`) と名前空間 `KsSettingsView` との非対称の説明 |
| 同上 | cross/ADR-0018 の配布先表 | MAUI 行に Package ID を追記 |
| 蒸留 (ksn-distill) | concepts `maui/api/maui-facade.md` | 名前空間 `KsSettingsView` への表記追随 (アセンブリ名の言及は残す) |
| 同上 | concepts `maui/architecture/binding-build-integration.md` | pack 経路・props / CPM・最低 OS 版ガード・SDK 更新時の再検証箇所 (manifest 絶対パス、API 版付き TFM) |
| 同上 | concepts `maui/api/native-bridge.md` の binding 構成節 | パッケージ化後の位置づけ (推移参照、直接参照しない) |
| 同上 | maui/ADR-0010 / ADR-0025 | 0010 は NuGet 経路の実証、0025 は buildTransitive ガードと manifest パスの CI 前提を Consequences へ追記し accepted へ昇格 |
| docs-refresh (明示依頼) | `skills/{en,ja}/kssettingsview-maui` と `kssettingsview-aiforms-migration` | 名前空間 / xmlns、`MauiVersion` 10.0.70 以上、最低 OS 版とガード |
| 同上 | README / README_ja | xmlns の例、互換表、インストール手順 (package README の新設は撤回、2026-09-02) |

## TODO

- [x] 論点の解消 (2026-09-02 全 11 論点を決定事項へ)
- [ ] **change 完了直後に docs-refresh を明示依頼する**: 改名直後から skills の XAML 例 (xmlns) が実物と食い違うため (README の例は change に同梱、2026-09-02 提案時の相方指摘)。phase-5 申し送り分 (Android 互換情報) と 1 回の依頼にまとめる (2026-09-02)。→ 依頼に含める内容は [phase-7 agenda の TODO](../phase-7-consumer-verification/agenda.md) に集約済み (2026-09-02 蒸留時)。依頼の実行はユーザーの作業
- [x] **docs-refresh 依頼に含める**: MAUI の互換情報 (最低 OS 版 Android 29 / iOS 16.0 とビルド時ガード、`Microsoft.Maui.Controls` 10.0.70 以上と NU1605 の注意) を README / skills に明記する (2026-09-02) → phase-7 agenda の TODO (docs-refresh 依頼) へ集約 (2026-09-02 蒸留時)
- [x] **phase-7 への申し送り**: 消費者検証で API 版付き TFM (`net10.0-android36.0` / `net10.0-ios26.0`) の解決要件を確認し、README / skills の互換情報に SDK 要件として載せる (docs-refresh 依頼に含める) (2026-09-02) → phase-7 agenda の TODO へ追記 (2026-09-02 蒸留時)
- [x] **別作業の候補 (phase-6 外)**: LICENSE (KsSettingsView / KsDialogs) と `Copyright` プロパティを法人の正式名 `kamusoft LLC` に揃えるかを決める。揃える場合は props の 1 行修正で追随できる (2026-09-02) → **見送り** (2026-09-02 蒸留時のオーナー判断): 揃えない。Authors / Copyright は `kamusoft`、Company だけ `kamusoft LLC` の現状を確定とする
- [x] ksn-propose で変更提案を起こす (2026-09-02 [changes/archive/2026-09-02-add-maui-nuget-distribution](../../../../changes/archive/2026-09-02-add-maui-nuget-distribution/proposal.md))

## 実装結果 (2026-09-02 反映)

change: [changes/archive/2026-09-02-add-maui-nuget-distribution](../../../../changes/archive/2026-09-02-add-maui-nuget-distribution/proposal.md) (コミット `abf6e0a` 名前空間改名 / `2ac5fd4` 本体)。review-002 APPROVED・verify-001 VALID (7 Requirement / 15 Scenario)。

決定事項どおりに成立したもの: 名前空間改名 (facade 74 / テスト 45 の宣言、利用側は using / xmlns の追随のみ、テスト 516 件不変)、props / CPM (18 版、restore の解決版は導入前と同一)、3 パッケージの pack (nuspec のメタデータ・TFM 別依存・snupkg)、buildTransitive ガード (`KSSV0001`)、package README (画像 4 + リンク 12 箇所を絶対 URL 化、`MauiProgram` 例を追加)、handbook / cross/ADR-0018 の追随、消費者検証 (restore 警告 0・両 OS Release ビルド・README 例の無編集ビルド・ガードの発火と非発火)。

### 決定事項と違った点 (deviation.md 12 項目の要点)

| 領域 | 実際 |
|---|---|
| 置き場 | `PackageIcon` の同梱アイテムは props では `IsPackable` 評価前のため `maui/Directory.Build.targets` へ。`PackageReadmeFile` は facade 固有のため facade の csproj へ |
| 成果物 | .NET Android SDK が facade / Android binding に自動生成する自 assembly 用 aar が各 nupkg に 1 本入る (中身は `androidx.graphics.path` の `.so` のみ)。利用者の Android Release ビルドで XA4301 4 件 |
| ガード | iOS は `SupportedOSPlatformVersion` 未設定時に SDK 既定 (26.x) が入り発火しない (Android の既定 21 は止まる)。Android の評価値は `29.0` に正規化される |
| 消費者検証 | `nuget.config` を `<clear/>` + ローカルフィードのみにすると MAUI テンプレートの依存 14 件が NU1101 で restore できず、nuget.org を併記 (隔離 packages path と `.nupkg.metadata` の source で取得元を確認) |
| 公開面 | 名前空間改名で `SwitchCell` / `EntryCell` が `Microsoft.Maui.Controls` の同名型と衝突 (C# の CS0104)。型名・名前空間は変えず完全修飾 / using alias を案内 (オーナー判断、maui/ADR-0025 Consequences) |
| 付随修正 | README のリンク参照 12 箇所の絶対 URL 化、`kasane/config.yaml` の `lint.comment-policy.ext` に MSBuild 拡張子を追加 |
| アイコン帰属 | review-001 Minor の「第三者素材」は前提不成立 (原典 AiForms の著作権者は本リポジトリと同一人物)。帰属表示は不要で申し送りなし |

### 申し送りと受け皿

| 項目 | 受け皿 |
|---|---|
| docs-refresh の明示依頼 (名前空間 / xmlns、`MauiVersion` 10.0.70 以上と NU1605、最低 OS 版とガード `KSSV0001`、`SwitchCell` / `EntryCell` の alias 注意書き、API 版付き TFM の SDK 要件) | 依頼はユーザーが change 完了直後に行う (phase-5 の Android 分と 1 回に)。依頼内容の集約先は [phase-7 agenda の TODO](../phase-7-consumer-verification/agenda.md) |
| API 版付き TFM (`net10.0-android36.0` / `net10.0-ios26.0`) の解決要件の確認 | [phase-7 agenda の TODO](../phase-7-consumer-verification/agenda.md) |
| `dotnet publish` (フル trimming) と実機起動の smoke の要否、`verification/` の MAUI `nuget.config` 設計 (nuget.org 併記 + 隔離 packages path) | 同上 |
| NU1507 の恒久対処、XA4301 の扱い、`ContinuousIntegrationBuild` の CI 側注入 | [phase-8 agenda の TODO](../phase-8-release-workflow/agenda.md) |
| LICENSE / `Copyright` の法人正式名化 | 見送り (2026-09-02 オーナー判断: 揃えない。Authors / Copyright `kamusoft`・Company `kamusoft LLC` の現状を確定) |
| Android 側 Gradle の Gradle 10 非互換 deprecation | Android ビルド基盤の課題として [phase-1 agenda](../phase-1-android-build-toolchain/agenda.md) の検証 CI ランナー要件に既出 (AGP 8.13 系が Gradle 10 非対応 API を内部使用)。本フェーズでは扱わない |

長命層への反映: maui/ADR-0025 を accepted へ昇格し実装結果を Consequences に追記、maui/ADR-0010 (NuGet 経路の実証) と maui/ADR-0026 (MAUI 要求版への依存) に日付付き追記。concepts は `maui/api/maui-facade.md` (導入と前提)・`maui/architecture/binding-build-integration.md` (NuGet パッケージ化)・`maui/api/native-bridge.md`・`cross/architecture/repository-boundaries.md` を更新。

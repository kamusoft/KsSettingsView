# Design: add-maui-nuget-distribution

## Context

maui/ADR-0025 が MAUI の配布形 (facade + binding 2 件の 3 NuGet パッケージ・名前空間 `KsSettingsView`・Package ID `KsSettingsView.Maui`・メタデータは `maui/Directory.Build.props`、版は CPM) を決定済み。本 design は phase-6 のフェーズ議論 (roadmaps/package-distribution/phases/phase-6-maui-packaging/history.md) で確定した実装方式を Decision 形式に転記し、ADR 抽出の原料と実装の指標にする。

現状の実測 (2026-09-02、agenda の artifacts/pack-poc.md と history.md):

- 3 パッケージは NuGet メタデータ未設定のままでも SDK 標準の `dotnet pack` だけで成立する。facade の TFM 条件付き ProjectReference は TFM group ごとの依存に自動変換され、facade に native 成果物は混入しない。Android binding は aar 2 本と AndroidX 14 本の依存、iOS binding は device + simulator 両スライスの resource zip を同梱する
- 素の MAUI アプリ (SDK 10.0.300) にローカルフィードの facade 1 行を足すと、NU1608 / NU1107 は 0 件で LiveData family が 2.11.0.1 に解決し、Release (trimming + AOT) ビルドは両 OS で警告ゼロ。ただしテンプレート既定の `Microsoft.Maui.Controls` 10.0.20 のままでは NU1605、最低 OS 版が既定 (Android 21 / iOS 15.0) のままでは iOS は黙って通り Android は依存 AndroidX の minSdk 23 で読みにくく落ちる
- facade の名前空間 `KsSettingsView.Maui*` の宣言は facade 74 (root 21 / Internals 49 / Handlers 4)、テスト 45。利用側は MauiHost と Sample の using / XAML xmlns、IntegrationHost は 0 件

## Goals / Non-Goals

proposal.md のとおり。要約: 名前空間改名・メタデータと版集約・pack 設定・利用者向けビルド時ガード・README の画像参照・規範追随と、ローカル pack + ローカルフィード消費者での検証まで。実発行・`verification/`・release workflow は後続フェーズ。

## Decisions

### Decision 1: 名前空間から `.Maui` を落とし、アセンブリ名は保つ (テストも同じ規則)

**採用案:** facade の `RootNamespace` と全 namespace 宣言を `KsSettingsView` (配下 `KsSettingsView.Internals` / `KsSettingsView.Handlers`) に、テストプロジェクトの `KsSettingsView.Maui.Tests` (`.Fakes` / `.Support`) を `KsSettingsView.Tests` (`.Fakes` / `.Support`) に改める。`AssemblyName` (`KsSettingsView.Maui` / `KsSettingsView.Maui.Tests`) と facade の `InternalsVisibleTo` (アセンブリ名基準) は変えない。利用側 (テスト・MauiHost・Sample) は `using` と XAML xmlns を `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui` に追随する。binding の `KsSettingsView.Bridge` は不変。改名は tasks の最初のグループとして独立コミットで行い、以後のタスクの diff から分離する。

**理由:** ADR-0025 の非対称 (アセンブリ名 = Package ID は `.Maui` を保ち、名前空間からは落とす) をテストにも同じ規則で当てると「名前空間に `.Maui` は出ない」の 1 ルールで説明でき、機械置換だけで済む。

**代替案:**
- **A: 公開面 (facade + 利用側の using / xmlns) だけ改名し、テストの名前空間は残す** — 差分は 45 ファイル減るが、テストだけ規則の例外になり、将来「なぜテストだけ違うのか」を説明する負債が残る。却下
- **B: 改名を独立の change に切り出す** — レビュー対象は小さくなるが、phase-5 (module 統合 + 発行) の前例に反して 1 フェーズ 2 change になる。独立コミットで diff を分離すれば同じ効果が得られる。却下

### Decision 2: 共通メタデータは `maui/Directory.Build.props`、IsPackable の既定は false

**採用案:** `maui/Directory.Build.props` に次を置く。

| 項目 | 値 |
|---|---|
| Authors / Company / Copyright | `kamusoft` / `kamusoft LLC` / `Copyright (c) kamusoft` |
| PackageLicenseExpression | `MIT` |
| RepositoryUrl / RepositoryType / PackageProjectUrl | `https://github.com/kamusoft/KsSettingsView` / `git` / 同 URL |
| Version | `0.0.0-dev` (CI が `-p:Version=` で上書き、cross/ADR-0020) |
| IsPackable | `false` (facade と binding 2 件の csproj でのみ `true`) |
| PublishRepositoryUrl / EmbedUntrackedSources / IncludeSymbols / SymbolPackageFormat | `true` / `true` / `true` / `snupkg` |
| GenerateDocumentationFile | facade のみ `true` (現状維持。binding は公開契約ではないため揃えない) |
| PackageIcon | `icon.png` — `assets/icon.png` を `IsPackable=true` のプロジェクトにだけ `Pack="true" PackagePath=""` の None アイテムで同梱 |

`PackageReadmeFile` はルート `README.md` を facade の csproj でのみ同梱する (Decision 6)。`ContinuousIntegrationBuild` は props に書かず CI 側で注入する (phase-8)。

**理由:** 3 プロジェクトに同じ値を書かず 1 か所に置く (ADR-0025 の置き場の決定)。SDK 既定の `IsPackable=true` はテストと検証ホストにも効くため、既定を false に倒して opt-in にするのが pack 混入を防ぐ最も単純な形。SourceLink は .NET 8 以降の SDK に同梱されており追加パッケージは不要。

**代替案:**
- **A: 各 csproj にメタデータを直書き (KsDialogs の現状形)** — 3 か所に同じ値が並び、Authors や URL の変更で 3 か所を触る。却下
- **B: リポジトリルートに `Directory.Build.props` を置く** — `samples/maui/` にも効いて Sample が pack 対象・CPM 対象になり、build root の境界 (cross/ADR-0001) も破る。却下

### Decision 3: 版は `maui/Directory.Packages.props` (CPM) に集約し、版整合の知識も同居させる

**採用案:** `ManagePackageVersionsCentrally=true` とし、`Microsoft.Maui.Controls` 10.0.70・AndroidX / Kotlin / Material / Compose 系 14 本・テスト系 3 本 (`Microsoft.NET.Test.Sdk` / `NUnit` / `NUnit3TestAdapter`) の `PackageVersion` をここに並べる。各 csproj の `PackageReference` から `Version` 属性を外す。binding csproj にある版整合の長いコメント (Compose BOM 追随・Material 1.12 固定・LiveData family) は `PackageVersion` の隣へ移し、csproj 側には参照先を示す短い注記だけ残す。`maui/` の外にある Sample は対象外で、`MauiVersion` 10.0.70 の直書き (利用者が真似する形) を維持する。

**理由:** 版の宣言元が 1 か所になり、MAUI / AndroidX 更新時の見直し点が明確になる。版の隣にその版を選んだ理由が居るのが最も腐りにくい。

**代替案:**
- **A: 各 csproj の直書きを維持** — facade / テスト / MauiHost の 3 か所に `Microsoft.Maui.Controls` の版が並び、更新漏れ (NU1605 の再発) の余地が残る。却下
- **B: Sample も CPM に含める (Directory.Packages.props をルートに置く)** — build root の境界を破り、Sample の csproj が利用者の csproj と違う形 (Version なし) になる。却下

### Decision 4: pack は SDK 標準経路のみ。binding の Package ID は既定、Description は英語

**採用案:** facade / binding 2 件の csproj に `IsPackable=true` を明示する。facade は `PackageId` を `KsSettingsView.Maui` と明示し、`Description` は "A settings screen UI library for .NET MAUI, providing list-style settings screens with built-in cell types on iOS and Android. Successor to AiForms.Maui.SettingsView."、`PackageTags` は `maui settings settingsview ios android`。binding は `PackageId` を既定 (アセンブリ名 `KsSettingsView.Binding.iOS` / `.Android`) に任せ、`Description` は "iOS native bridge binding for KsSettingsView.Maui. Referenced transitively by KsSettingsView.Maui; do not reference this package directly." (Android は読み替え)、`PackageTags` は付けない。自作の pack 用 MSBuild は足さず、PoC で確認した SDK 挙動 3 点 (iOS manifest の絶対パス・facade → binding の依存版が下限指定・API 版付き TFM) はそのまま受け入れる。

**理由:** PoC で 3 パッケージが標準経路だけで成立し、3 点はいずれも SDK の pack 内部構造に依存しないと変えられず、受け入れても利用者に実害がない (CI ランナーのパス・lockstep 同時発行と最小適用版解決・同一 SDK なら解決可)。

**代替案:**
- **A: facade → binding の依存を `[x.y.z]` の完全一致にする自作ターゲット** — lockstep (cross/ADR-0019) で同時発行される以上、NuGet の最小適用版解決で利用者は同版の binding を得るため、内部構造依存を足す価値がない。却下
- **B: iOS manifest の絶対パスを消す後処理** — 公開物には CI ランナーの汎用パスしか載らず、消す価値に対して SDK 内部の resource package 構造への依存が重い。却下
- **C: Description を日本語にする / AiForms に触れない** — nuget.org の読者と英語正典の README に合わず、AiForms 利用者が後継を検索で見つけにくい。却下

### Decision 5: 最低 OS 版は facade 同梱の `buildTransitive/` .targets で検査し、要件の数値は同梱 props を単一の宣言元にする

**採用案:** facade プロジェクトに `buildTransitive/KsSettingsView.Maui.props` (要件の数値だけを定数として定義: `KsSettingsViewMinAndroidApi=29` / `KsSettingsViewMinIOSVersion=16.0`) と `buildTransitive/KsSettingsView.Maui.targets` (検査ターゲット) を置き、両方を facade パッケージに同梱する。

- 検査ターゲットは `TargetPlatformIdentifier` が `android` / `ios` の inner build でのみ有効 (Condition)。素の `net10.0`、複数 TFM プロジェクトの outer build (`TargetFramework` 空)、facade を間接参照するライブラリの非 platform TFM では何もしない
- 比較は `$([MSBuild]::VersionLessThan('$(SupportedOSPlatformVersion)', '$(KsSettingsViewMin...)'))` で行い (文字列比較や数値演算子は使わない)、`SupportedOSPlatformVersion` が空または要件未満なら、要件と `SupportedOSPlatformVersion` の設定例を書いた `Error` を出す。Android では `XAAMM0000` (manifest merger) より先に出るよう `CoreCompile` の前 (`BeforeTargets="CoreCompile"`) で走らせる
- リポジトリ側: `maui/Directory.Build.props` が同梱 props を import して定数を供給し、facade / binding / 検証ホストの csproj は**既存の TFM 条件をそのまま残して**値だけを `$(KsSettingsViewMinAndroidApi)` / `$(KsSettingsViewMinIOSVersion)` の参照に置き換える (`Directory.Build.props` はプロジェクト本体より先に import され `TargetFramework` を参照できないため、TFM 条件の代入は props 側に置かない)。`maui/Directory.Build.targets` が同梱 targets を import して自分たちのビルドでも検査を通す
- 評価値の確認は `dotnet msbuild -getProperty:SupportedOSPlatformVersion -p:TargetFramework=<tfm>` で TFM ごとに行う

**理由:** 消費者 PoC で iOS は要件未満でも黙って通り、Android も本体要件 (29) は誰も検査していないと分かった。同梱 props を宣言元にすれば、リポジトリ内の宣言とパッケージが利用者に伝える数値が 1 か所から出る。利用者ビルドに同梱する .targets は ADR-0025 が避けた「pack 内部構造に依存する自作 MSBuild」ではない。`buildTransitive/` は推移的な全消費者に import されるため、platform inner build 以外で無効になる Condition が受け入れ条件に入る。

**代替案:**
- **A: README / skills への明記のみ** — iOS 15 のまま出荷して実行時に落ちる形を防げず、Android は原因の読みにくい manifest merger のエラーが先に出る。却下
- **B: 数値を .targets に直書きし、リポジトリ側の csproj にも同じ数値を書く** — 2 か所の同期が要る。却下
- **C: TFM 別フォルダ (`buildTransitive/net10.0-android/` 等) に分ける** — 1 ファイル内の Condition で足り、フォルダ分割は API 版付き TFM の照合を複雑にする。却下
- **D: `SupportedOSPlatformVersion` の代入を `Directory.Build.props` の TFM 条件で行う** — 単一 TFM の binding / IntegrationHost は `TargetFramework` を csproj 本体で定義するため props 側の条件が空振りする。却下

### Decision 6: package README はルート `README.md` を同梱し、画像参照を絶対 URL に改める

**採用案:** facade の csproj で `PackageReadmeFile=README.md` とし、ルート `README.md` を `Pack="true" PackagePath=""` で同梱する (README は facade 固有のため props ではなく csproj に置く — ADR-0025 は `PackageReadmeFile` も props に集約すると書いており、この差は蒸留時に ADR-0025 へ日付付きで注記する)。`README.md` / `README_ja.md` のスクリーンショット参照 (`assets/*.png` 相対 4 箇所 × 2 枚) を `https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/<file>` の絶対 URL に改め、MAUI 最小例の xmlns (`clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui`) と `using` を改名後の名前空間に追随させる (両枚を同時に、翻訳ロックステップ。同梱する README が自分の配布物で動かない例を載せないため。互換情報の追記は docs-refresh に残す)。public リポジトリ `kamusoft/KsSettingsView` は public 化済みで既定ブランチは `develop` (削除・force-push 禁止、2026-09-02 確認) のため、URL は現時点で到達性を検証できる。各 URL の取得成功と画像の Content-Type を受け入れ条件にする。

**理由:** README を新設せず cross/ADR-0023 (README は 4 枚のみ) と docs-refresh の対象定義に触れない。nuget.org は許可ドメイン (raw.githubusercontent.com) の絶対 URL 画像を表示でき、GitHub 上の表示も変わらない。

**代替案:**
- **A: facade 専用の短い package README を新設** — ADR-0023 の改訂と docs-refresh 対象の追加 (en のみで翻訳ロックステップの例外) が要る。却下 (agenda で一度採用したが提案時の照合で撤回)
- **B: README を同梱しない** — nuget.org で「readme なし」表示になる。ADR 上は可だが、上記で解決できる以上採らない。却下
- **C: tag や commit hash を含む URL にする** — リリースのたびに README の書き換えが要り、cross/ADR-0020 (version bump のコミットを積まない) と相性が悪い。却下

### Decision 7: MAUI 本体の要求版は 10.0.70 を維持する

**採用案:** facade (と CPM) の `Microsoft.Maui.Controls` は 10.0.70 のまま。利用者向けには「`MauiVersion` は 10.0.70 以上」を README / skills に明記する (docs-refresh 依頼)。

**理由:** 10.0.20 に下げる実測ではビルド・テスト 516 件・利用側 iOS リンクは通ったが、MAUI のファイル画像解決 (`ImageSourceExtensions.GetPlatformImage`) の照合キーが 10.0.30 以前と 10.0.60 以降で異なり、iOS icon 所有権分類 (maui/ADR-0026) の安全側不変条件が 10.0.20 では保証されない。10.0.60 と 10.0.70 は利用者体験上同じで、10.0.70 は ADR-0026 の probe を通した版のため再検証が要らない。

**代替案:**
- **A: 下限を SDK 既定 (10.0.20) に下げる** — ADR-0026 の前提が崩れる。却下
- **B: 下限を 10.0.60 (照合キーの形が成立する検証済み最低版) に下げる** — 利用者体験は 10.0.70 と変わらず、再検証の手間だけ増える。却下
- **C: 照合キーを両形式に対応させて 10.0.20 まで下げる** — 所有権分類という delicate な領域に内部挙動の 2 形式を追い続ける負債を足す。却下

## Risks / Trade-offs

- 改名は機械置換で済むが、obj/ 配下の生成物や XAML の x:Class / xmlns の取りこぼしはビルドで初めて分かる。facade テスト (516 件) と MauiHost / IntegrationHost の起動で回帰を確認する
- CPM は `maui/` 配下の全プロジェクトに効く。テスト・検証ホストの restore が変わらない (project.assets.json の解決版が同じ) ことを確認する
- ビルド時ガードは利用者ビルドに同梱される初の MSBuild 資産。検証ホスト (要件を満たす) で誤検知せず、一時的な消費者プロジェクト (要件未満・未設定) で検出することの両方を確認する
- README の画像 URL はブランチ名 `develop` を含む。既定ブランチを改名すると画像が切れる (branch protection で削除は禁止済み)
- `buildTransitive/` の資産は推移的な全消費者に import される。platform inner build 以外で無効であることを消費者検証で確認する
- 改名直後から skills の XAML 例が実物と食い違う (README の例は本変更で追随)。change 完了直後に docs-refresh を依頼する (agenda TODO)

## Migration Plan

1. 名前空間改名 (独立コミット) → facade ビルド・テスト・MauiHost 起動で回帰確認
2. props / CPM / アイコン → 全プロジェクトの restore / ビルドが変わらないことを確認
3. pack 設定 + ガード + README → ローカル pack と一時的な消費者で検証、evidence/ に証跡
4. 規範追随 (handbook / ADR-0018 追記)

利用者側の移行は不要 (未リリース)。

## Open Questions

- なし (README の URL のブランチ名は `develop` で確定、2026-09-02)

## ADR 候補

- なし (新規)。既存 ADR への申し送り: maui/ADR-0025 の Consequences に「利用者ビルドへ同梱する `buildTransitive/` ガード」「iOS manifest の絶対パスは CI ランナーのパス」「`PackageReadmeFile` は facade 固有のため csproj に置く」を追記して accepted へ昇格 (Decision 4・5)、maui/ADR-0010 の Consequences に NuGet 経路の実証を追記、maui/ADR-0026 の Consequences に「照合キーの形は MAUI 10.0.60 以降の内部挙動に依存し、facade の要求版 10.0.70 がこれを保証する」を追記 (Decision 7)。いずれも蒸留時

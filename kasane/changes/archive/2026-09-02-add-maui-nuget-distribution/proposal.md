# Proposal: add-maui-nuget-distribution

## Why

MAUI の配布は「facade + binding 2 件の 3 NuGet パッケージ、名前空間 `KsSettingsView`・Package ID `KsSettingsView.Maui`」と決定済み (maui/ADR-0025) だが、現状は名前空間が `KsSettingsView.Maui` のまま、NuGet メタデータも版集約もなく、binding は pack の対象になっていない。phase-6 の議論 (roadmaps/package-distribution/phases/phase-6-maui-packaging) で pack PoC と消費者 PoC により ADR-0025 の前提を実測で裏付け、利用者側の前提 (MAUI 本体の最低版・最低 OS 版) が誰にも検査されていないことも判明した。名前空間改名・メタデータ・pack 設定・利用者向けガードを入れ、消費者検証 (phase-7) と release workflow (phase-8) が乗る土台を作る。

## What Changes

- **名前空間改名** (下準備として最初に、独立コミットで): facade の名前空間を `KsSettingsView.Maui` → `KsSettingsView` (配下 `.Internals` / `.Handlers`)、テストプロジェクトの自前名前空間を `KsSettingsView.Maui.Tests` → `KsSettingsView.Tests` (`.Fakes` / `.Support`) に改める。アセンブリ名 (`KsSettingsView.Maui` / `KsSettingsView.Maui.Tests`) と `InternalsVisibleTo` は変えない。利用側 (テスト・MauiHost・Sample) は using と XAML xmlns (`clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui`) の追随のみ。binding の `KsSettingsView.Bridge` は不変
- **共通メタデータと版集約**: `maui/Directory.Build.props` (Authors `kamusoft` / Company `kamusoft LLC` / Copyright / MIT expression / RepositoryUrl 等 / Version 既定値 `0.0.0-dev` / IsPackable 既定 false / SourceLink + snupkg / PackageIcon) と `maui/Directory.Packages.props` (CPM: Microsoft.Maui.Controls 10.0.70、AndroidX 系、テスト系)。binding csproj の版整合コメントは Packages.props 側へ移す。アイコンは AiForms.Maui.SettingsView 由来の `icon.png` を `assets/icon.png` として取り込む
- **pack 設定**: facade と binding 2 件に `IsPackable=true`、facade に `PackageId` / Description / PackageTags / PackageReadmeFile (ルート `README.md`)、binding に「直接参照しない」Description。pack は SDK 標準経路のみ (自作の pack 用 MSBuild は足さない)
- **利用者向けビルド時ガード**: facade パッケージに `buildTransitive/` の .targets を同梱し、利用者の `SupportedOSPlatformVersion` が Android 29 / iOS 16.0 未満なら要件を書いたエラーで止める。要件の数値は props に集約する
- **README の同梱に伴う追随**: ルート `README.md` / `README_ja.md` のスクリーンショット参照を public リポジトリの絶対 URL に改め、MAUI 最小例の xmlns / `using` を改名後の名前空間に追随させて、nuget.org の package README として表示でき、例をコピーしてそのままビルドできるようにする (両枚同時)
- **規範の追随**: `kasane/handbook/cross/public-identifiers.md` に NuGet Package ID の行と名前空間との非対称を追加、cross/ADR-0018 の配布先表に MAUI の Package ID を日付付きで追記
- **検証**: 3 パッケージのローカル pack と、ローカルフィードから参照する消費者 restore / Release ビルドで内容 (依存・同梱物・ガード・README) を確認する。証跡は evidence/ に残す

影響する能力: MAUI の配布 (NuGet パッケージ構成とメタデータ)、MAUI facade の公開名前空間、MAUI 利用者アプリのビルド前提、MAUI テスト / 検証ホスト / Sample のソース参照

## Non-Goals

- NuGet.org への実発行と `KsSettingsView*` Package ID の予約 — 実発行は phase-8 (release workflow) の責務。本変更はローカル pack とローカルフィードからの消費者検証まで
- `verification/` の消費者プロジェクトと dry-run / smoke の仕組み — phase-7 の責務。本変更の消費者検証は evidence 取得のための一時プロジェクトで行い、リポジトリに残さない
- facade が要求する MAUI 本体の下限引き下げ — 議論で 10.0.70 (ADR-0026 の probe を通した検証済み版) の維持を決定。README / skills に `MauiVersion` 10.0.70 以上と最低 OS 版の互換情報を明記するのは docs-refresh の依頼 (README の MAUI 例の xmlns / using は同梱物の整合のため本変更に含める)
- iOS binding resource の manifest に記録される絶対パスの除去、facade → binding の依存版の完全一致化、API 版付き TFM の固定 — いずれも SDK 挙動として受け入れ (agenda 決定)。TFM の解決要件は phase-7 の消費者検証で確認
- `dotnet publish` (フル trimming) と実機起動の確認 — phase-7 の smoke の範囲
- concepts (maui-facade / binding-build-integration / native-bridge) と ADR-0010 / 0025 の Consequences 追記・accepted 昇格 — 蒸留時の定型作業。skills/ の文面 (xmlns・互換情報) とルート README の互換情報は docs-refresh の明示依頼 (change 完了直後)
- LICENSE と `Copyright` の法人正式名化 — 2 リポジトリに波及する別作業 (agenda TODO)
- Android 側 Gradle の Gradle 10 非互換 deprecation の解消 — Android ビルド基盤の別課題
- `KsSettingsView.slnx` の構成変更 — Sample を含めたまま維持する (agenda 決定。pack と CI は csproj 単位で slnx を使わない)
- KsDialogs への同型展開 — KsDialogs 側フェーズの責務

## Impact

- 破壊的変更: 公開名前空間が `KsSettingsView.Maui` → `KsSettingsView` に変わる (XAML xmlns と using が変わる) が、未リリースのため実利用者はゼロ。monorepo 内の消費者 (テスト / MauiHost / Sample) は本変更内で追随する
- リスク: ① 改名は facade 74 + テスト 45 の namespace 宣言と利用側の using / xmlns に及ぶ機械置換で、ビルド・テスト・検証ホストの起動で回帰を確認する。② CPM の導入は `maui/` 配下の全プロジェクトに効くため、テスト・検証ホストの restore が変わらないことを確認する。③ ビルド時ガードは利用者ビルドに同梱される初の MSBuild 資産で、検証ホスト (要件を満たす設定) で誤検知しないことと、一時的な消費者プロジェクトで要件未満の設定を検出することの両方を確認する。④ 改名直後から skills の XAML 例が実物と食い違う (README の例は本変更で追随) ため、change 完了直後に docs-refresh を依頼する。⑤ maui-support 側の change と並走させない (roadmap 前提)
- 外部リソース: 新規なし (NuGet.org アカウントと API キーは phase-8)

## 級: L

複数能力横断 (facade の公開名前空間・NuGet 配布・利用者ビルド前提・テスト / Sample の追随・handbook) かつ公開 Package ID と名前空間という覆すコストの高い決定の実装のため L。設計判断は ADR-0025 とフェーズ議論で確定済みであり、design.md は agenda 決定事項の Decision 形式への転記と実装方式の確定 (props の構成・ガードの形・改名の手順) に絞る。

domain: maui
roadmap: package-distribution/phase-6-maui-packaging

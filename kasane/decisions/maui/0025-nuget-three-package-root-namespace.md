---
id: 0025
title: MAUI は facade + binding 2 件の 3 NuGet パッケージで配布し、名前空間は `KsSettingsView`・Package ID は `KsSettingsView.Maui` とする
status: proposed
date: 2026-08-21
---

## Context

MAUI facade (`KsSettingsView.Maui.csproj`、`net10.0;net10.0-ios;net10.0-android`) は binding 2 件 (`KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android`) を TFM 条件付き ProjectReference で内包し、NuGet.org へ配布する (cross/ADR-0018)。iOS binding は `IsBindingProject=true` の ApiDefinition 方式、Android binding は gradlew を Exec で呼び `AndroidLibrary` で束ねる (maui/ADR-0006)。NuGet メタデータ・`Directory.Build.props` / `Directory.Packages.props` は未整備で、RootNamespace は `KsSettingsView.Maui` (配下に `.Internals` / `.Handlers`)、binding の名前空間は `KsSettingsView.Bridge`。

iOS (umbrella product 1 本、cross/ADR-0018) と Android (artifact 1 本、android/ADR-0016) は「利用者が手で入れるのは 1 点」で揃えており、MAUI も同じ手数にしたい。姉妹ライブラリ KsDialogs は同じ構図で 3 パッケージ構成 (KsDialogs maui/ADR-0004) と名前空間 / Package ID の非対称 (KsDialogs cross/ADR-0005) を先に決めており、本 ADR はその翻案である。

## Decision

**パッケージ構成: facade + binding 2 件の 3 パッケージ。**

| Package ID | 役割 | 利用者が書くか |
|---|---|---|
| `KsSettingsView.Maui` | facade (XAML / C# 公開 API) | **書く (これ 1 点)** |
| `KsSettingsView.Binding.iOS` | iOS Bridge の binding (net10.0-ios) | 書かない (facade の依存で自動) |
| `KsSettingsView.Binding.Android` | Android Bridge の binding (net10.0-android) | 書かない (同上) |

- facade の TFM 条件付き ProjectReference は pack 時に NuGet 依存へ自動変換される。binding は `IsPackable=true` にするだけで、native 成果物 (iOS は binding resource package 内の xcframework、Android は `kssettingsview` / `kssettingsview-bridge` の aar) は SDK 標準の pack 経路で同梱する。自作の pack 用 MSBuild は足さない
- binding の Description に「`KsSettingsView.Maui` から推移的に参照される。直接参照しないこと」を明記する

**名前空間と Package ID の非対称。**

- facade の RootNamespace / 公開型の名前空間は **`KsSettingsView`** (配下は `KsSettingsView.Internals` / `KsSettingsView.Handlers`)。MAUI プロジェクトの文脈内で読まれる名前空間に `.Maui` は冗長である
- Package ID は **`KsSettingsView.Maui`** のまま。csproj の依存一覧や nuget.org の検索など文脈のない場所で読まれるため、platform を修飾する
- アセンブリ名は Package ID と同じ `KsSettingsView.Maui` を保つ (XAML の xmlns は `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui`)
- binding の名前空間 `KsSettingsView.Bridge` は利用者向け契約ではないため変更しない

**共通メタデータと版管理の置き場。** NuGet メタデータ (Authors / License / RepositoryUrl / PackageIcon / PackageReadmeFile / Version / SourceLink) は `maui/Directory.Build.props`、PackageReference の版は `maui/Directory.Packages.props` (CPM) に集約する。置き場は MAUI のビルドルート `maui/` であり、リポジトリルートには置かない (cross/ADR-0001)。

## Alternatives Considered

- **単一パッケージに facade・binding・native 成果物を全部同梱する**: 却下。iOS binding は `IsBindingProject` という別種のプロジェクトで `UseMaui` の facade と同居できないため、facade の pack に他プロジェクトの成果物を寄せる自作 MSBuild (`TargetsForTfmSpecificBuildOutput` + binding resource の手動コピー) が必要になり、SDK の pack 内部構造に依存する壊れやすい層を既存の MSB4120 回避策の上に積むことになる。利用者が書く行数は 3 パッケージ構成でも 1 行で変わらない。
- **Android binding だけ facade に統合して 2 パッケージにする**: 却下。`AndroidLibrary Bind="true"` は facade に直接置けるが、iOS ができない以上、片側だけ統合しても構成が非対称になるだけで利点がない。
- **名前空間も `KsSettingsView.Maui` のまま (Package ID と一致させる)**: 却下。MAUI プロジェクトから使う文脈で `.Maui` は自明であり、`using KsSettingsView;` の方が短く読める。KsDialogs とも揃う。
- **Package ID も `KsSettingsView` にする**: 却下。nuget.org や csproj の依存一覧では platform の手がかりがなく、Native 版と紛れる。

## Consequences

- 正: 利用者は `<PackageReference Include="KsSettingsView.Maui" />` 1 行で導入でき、iOS / Android の「1 点」と揃う。
- 正: pack は SDK 標準経路のみで、SDK 更新時の追随箇所が増えない。
- 正: binding 層で明示宣言している AndroidX の版 (maui/ADR-0010) が nuspec の依存として利用者へ届く。
- 負: nuget.org に 3 パッケージが並び、binding を直接参照する誤用の余地が残る (Description で抑止)。
- 負: 名前空間改名 (`KsSettingsView.Maui` → `KsSettingsView`) は facade 全ファイル・Sample・検証ホスト・テスト・XAML xmlns に及ぶ。公開前の下準備として実施する。
- 負: Android binding の aar 参照は android/ADR-0016 の module 統合後のパス (2 本) へ追随が必要で、Exec 経由 gradlew (maui/ADR-0006) と pack 経路の整合は PoC で確認する。
- 負: iOS binding resource の manifest に発行マシンの絶対パスが記録される (SDK 標準挙動。消費者ビルドでは無害だが公開物に含まれる)。

出典: kasane/roadmaps/package-distribution/exploration.md (E) / ../KsDialogs/kasane/decisions/maui/0004-nuget-three-package-structure.md / ../KsDialogs/kasane/decisions/cross/0005-public-identifier-mapping.md (翻案元)

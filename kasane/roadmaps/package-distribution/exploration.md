# Exploration: package-distribution (ロードマップ起案の出典)

ksn-explore (2026-08-21) で A〜G の論点を確定した探索メモ。1 change に収まらない規模のため、ロードマップ `package-distribution` として起案した (roadmap.md)。本ファイルは起案の出典・ADR の出典として保持し、以後の論点の議論は各フェーズの agenda.md / history.md で行う。

## 課題 / 動機

KsSettingsView (iOS Swift / Android Kotlin / .NET MAUI) を AiForms.Maui.SettingsView の後継として公開 OSS で配布したい。現状、配信の仕組みはゼロ:

- `.github/` が存在せず CI なし、git tag なし、リポジトリは private
- iOS: `ios/Package.swift` に products 3本 (Core / UI / SwiftUI)、外部依存なし。Package.swift がリポジトリルートにない
- Android: 4 module (core / ui / compose / bridge) に `maven-publish` なし。`group = jp.kamusoft.kssettingsview` / `version = 0.1.0-SNAPSHOT` が各ファイルにハードコード。module 間依存がすべて `implementation`
- MAUI: NuGet メタデータ (PackageId / Version / Authors / License) なし、`Directory.Build.props` / `Directory.Packages.props` なし。`dotnet pack` は gradlew と xcodebuild を内包する (maui/ADR-0006)
- Sample は 3 platform ともソース参照 (Local SPM / composite build / ProjectReference)。配信経路は未検証 (`concepts/cross/architecture/repository-boundaries.md` が「ソース参照の成功を配布成立と説明しない」と明記)

既存の決定・積み残しで配信に直結するもの:

- cross/ADR-0002: Maven groupId は `jp.kamusoft`。現行 Gradle `group` は未追従の drift (`concepts/cross/conventions/public-identifiers.md` が「Maven 公開を導入する変更で先に解消する」と明記)
- roadmaps/maui-support/roadmap.md: 「配布は NuGet 前提、パッケージングは配布要件が固まった時点で別途」(非ゴール)
- NuGet 化時に回収する TODO: AndroidX Lifecycle 競合 (NU1608 / NU1107) の nuspec 経由解消の実証 (phase-2 / phase-3 agenda)、旧 AiForms からの移行ガイド (phase-4 agenda、docs-refresh 経由)

## 姉妹ライブラリ KsDialogs の先行設計 (翻案元)

`../KsDialogs/kasane/` の phase-10-packaging-model (completed) で配布モデルが設計済み。実装・CI は未着手 (GitHub 未 push・未公開)。

- cross/ADR-0008 (proposed): 標準3チャネルのみ (SwiftPM / Maven Central / NuGet.org)。ミラー repo・umbrella SwiftPM・Swift Package Registry は却下。Package.swift はリポジトリルートへ移設し Sources は `path:` で `ios/` 配下、`ios/Package.swift` は廃止 (2枚持ち禁止)。source 配布のみ
- cross/ADR-0009 (proposed): 全形態 lockstep 単一バージョン、互換マトリクスは作らない
- maui/ADR-0004 (proposed): facade + binding 2件の3パッケージ構成、native 成果物は SDK 標準 pack 経路で同梱 (自作 MSBuild なし)。単一パッケージ同梱案は却下
- android/ADR-0001 (accepted): Compose API は別 module (MAUI binding 経由で compose-ui が推移しないように)
- PoC の知見: iOS binding resource の manifest に発行マシンの絶対パスが乗る (SDK 標準挙動)、nuget.org 固有メタデータ (license / icon / snupkg) は未検証、検証は「リポジトリ外に消費者プロジェクトを建てて file:// bare repo / mavenLocal / ローカルフィードから解決」で行う
- F (CI) と G (検証実施) は KsDialogs でも空白。KsSettingsView が先行して詰め、KsDialogs へ逆流させる

## 論点の区分

- A. 配信先と公開範囲
- B. バージョニング
- C. iOS パッケージ
- D. Android パッケージ
- E. MAUI NuGet
- F. 配信 CI
- G. 配信経路の検証

## 検討した選択肢 (却下案と理由を含む)

### A. 配信先と公開範囲

| 案 | 評価 |
|---|---|
| ① 最初から公開レジストリ (Maven Central / NuGet.org / 公開 git SwiftPM) | **採用**。AiForms 後継として公開 OSS が大前提 (オーナー 2026-08-21) |
| ② private のまま (GitHub Packages + private git) | 却下。private 配信は「むしろなくても良い」 |
| ③ 段階方式 (② で経路確立 → ①) | 却下。② を作る意味がない |

### B. バージョニング

| 案 | 評価 |
|---|---|
| ① lockstep (単一 tag `vX.Y.Z` で 3 platform 同時リリース) | **採用** (オーナー 2026-08-21)。MAUI NuGet が同一コミットの native 成果物を同梱する・Sample パリティ (cross/ADR-0016)・SwiftPM が tag を version として解釈する、の3点が理由。KsDialogs cross/ADR-0009 と一致 |
| ② platform 独立 (接頭辞付き tag) | 却下。SwiftPM が接頭辞 tag を解釈できない (要裏取り)・changelog と CI が platform 数だけ増える |
| ③ Native lockstep + MAUI 独立 | 却下。MAUI 利用者が同梱 Native 版を読む必要が生じる |

### C. iOS パッケージ

| 案 | 評価 |
|---|---|
| ① Package.swift をリポジトリルートへ移設 (`path:` で ios/ 配下を参照、`ios/Package.swift` 廃止) | **採用** (オーナー 2026-08-21)。SwiftPM はルート直下のマニフェストしか解決しない (KsDialogs PoC で実証)。cross/ADR-0001 への例外として明示 |
| ② 2枚持ち (開発用 ios/ + 配布用ルート) | 却下。乖離事故の温床 |
| ③ binary 配布 (xcframework + binaryTarget) | 却下。毎リリースの生成・checksum 管理が CI に乗る。source 配布で足りる。将来必要になれば再検討 |
| ④ ミラー repo | 却下。同期 CI・tag 二重管理・窓口分裂 |

### D. Android パッケージ

| 小論点 | 採用 | 却下案 |
|---|---|---|
| D1 groupId | `jp.kamusoft` (ADR-0002 に追随) — オーナー 2026-08-21 | `jp.kamusoft.kssettingsview` で ADR-0002 を supersede (製品名二重・KsDialogs と不揃い) |
| artifactId | `kssettingsview` (ブランド 1 トークン、ハイフンなし)。利用者が書く座標は `jp.kamusoft:kssettingsview` の 1 点 — オーナー 2026-08-21 | `ks-settingsview` (ブランド内ハイフンは慣例外) / `kssettingsview-ui` 等の接尾辞付き (オーナーが不要と判断) |
| artifact 粒度 | **core / ui / compose を 1 Gradle module `android/kssettingsview` に物理統合** — オーナー 2026-08-21。ui が既に Compose 一式に依存し compose module の依存は部分集合のため、分割しても利用者の依存は減らない | 3 artifact + `api` 推移 (Compose 利用者が別座標) / KsDialogs 型 2 分割 (根拠なし) / fat aar (AGP 非サポート) |
| dir / project 名 | artifactId に揃える (`android/kssettingsview`, `android/kssettingsview-bridge`) | 据え置き (artifactId と dir の恒久的なずれ) |
| D3 bridge | 別 module のまま Maven 非公開 — オーナー 2026-08-21 | 公開 (利用者契約でなく責任だけ増える) |
| D4 発行手段 (推奨、実装時に確認) | Central Portal へ `com.vanniktech.maven.publish` で発行 (POM / sources / javadoc / GPG 署名 / アップロードを 1 plugin で)。`jp.kamusoft` の DNS 検証は KsDialogs と共用 | 素の maven-publish + signing + Portal API の手配線 |

副次効果 (要検証): maven-publish 導入で composite build の自動置換が効き、Sample の明示 `dependencySubstitution` が不要になる可能性。

### C (補足). iOS を 1 点で揃える方法

| 案 | 評価 |
|---|---|
| ① umbrella product `KsSettingsView` 1 本 (3 target 維持、import は module 名) | **採用** (オーナー 2026-08-21)。module 名と型名が別なので `KsSettingsViewCore.Section` 修飾が安全。層の強制も維持 |
| ② 1 target へ物理統合 | 却下。module 名 `KsSettingsView` が型 `KsSettingsView` と衝突し修飾が壊れる |
| ③ ① + umbrella module (`@_exported`) | 今回は採らず。要望が出たら PoC 付きで検討 |

### E. MAUI NuGet

| 小論点 | 採用 | 却下案 |
|---|---|---|
| E1 分割 | facade + binding 2 件の 3 パッケージ (KsDialogs maui/ADR-0004 翻案) — オーナー 2026-08-21 | 単一パッケージ (iOS binding が `IsBindingProject` で facade と同居不可、自作 MSBuild 必須) / Android だけ統合の 2 パッケージ (非対称なだけ) |
| E2 native 同梱 | SDK 標準 pack 経路 (`IsPackable=true`)。Exec 経由 gradlew と pack の整合は PoC で確認 | 自作 pack ターゲット |
| E3 Package ID | `KsSettingsView.Maui` / `KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android` | — |
| 名前空間 | **`KsSettingsView`** (配下 `.Internals` / `.Handlers`)。MAUI プロジェクトの文脈で `.Maui` は自明 — オーナー 2026-08-21。アセンブリ名は Package ID と同じ。binding の `KsSettingsView.Bridge` は据え置き。下準備タスクとして E の change に積む | `KsSettingsView.Maui` のまま |
| E4 メタデータ / CPM | `maui/Directory.Build.props` + `maui/Directory.Packages.props` (ビルドルートに置く、ADR-0001) | リポジトリルート |
| E5 積み残し | Lifecycle 競合 (NU1608 / NU1107) の NuGet 経由実証、Release / trimming 構成の消費者ビルド、移行ガイド、README インストール手順 — E の change のタスクへ | — |

### F. 配信 CI

小論点: F1 起動方法と tag のタイミング / F2 version の SSoT / F3 ランナー / F4 認証と署名 / F5 検証 CI / F6 public 化のタイミング

| 小論点 | 採用 | 却下案 |
|---|---|---|
| F1 | `workflow_dispatch` (version 入力) で起動、全 platform の publish 成功後に tag + GitHub Release (tag は最後) — オーナー 2026-08-21 | tag push トリガー (SwiftPM だけ先行して lockstep が壊れる) / release ブランチ |
| F2 | SSoT は dispatch 入力 (= tag)。CI が `-Pversion=` / `-p:Version=` で注入、ファイルは開発用既定値 — オーナー 2026-08-21 | ファイルを正にして tag と照合 (bump コミットが毎回必要) |
| F3 (推奨) | iOS / MAUI は GitHub-hosted macOS、Android は ubuntu。1 release workflow 内で job 分割、`needs` で「全 platform ビルド・テスト成功 → publish 段」 | 全部 macOS (コスト・速度) |
| F4 (推奨) | Maven Central: Portal ユーザートークン + GPG 鍵を GitHub secrets (Environment で保護)。NuGet.org: Trusted Publishing (OIDC) を第一候補、propose 時に可否確認 (`nuget-trusted-publishing` スキルあり)。SwiftPM: 認証不要。tag / Release: `GITHUB_TOKEN` | 長期 NuGet API key |
| F5 (推奨) | PR / push の検証 CI (3 platform ビルド + テスト) を先行 change で新設し、release workflow が job 定義を再利用 | release workflow 単独 (失敗の切り分けが release 時になる) |
| F6 | **CI 着手前に public 化** — オーナー 2026-08-21。public なら macOS ランナー込みで Actions 無料、SwiftPM の https 実リモート検証が最初からできる | 初回リリース直前 / 後 (CI 試行錯誤が有料、検証が file:// 擬似リモート止まり) |

**prerelease の扱い (2026-08-21 追記)**: NuGet と SwiftPM はハイフン付き SemVer2 を prerelease として扱う (既定の解決から除外、opt-in で取得)。Maven Central にはフラグの仕組みがなく通常リリースと同格で、Maven / Gradle の版比較は alpha / beta / rc / milestone の qualifier だけを「正式版より古い」と解釈する (`preview` 等の未知 qualifier は**正式版より新しい**と判定される事故がある)。lockstep の prerelease は `X.Y.Z-{alpha|beta|rc}.N` に統一し、`-pre` / `-preview` は使わない。`-SNAPSHOT` は開発用既定値専用 (Central の通常リリースには出さない)。初回リリースを `0.1.0` にするか `1.0.0-beta.1` にするかは phase-8 の論点。

**public 化の前提タスク (オーナー指示 2026-08-21): 機密情報・個人情報の混入チェック。** 範囲は作業ツリーだけでなく git 履歴全体 (過去 commit で追加後に削除したものも含む) と、ドキュメント・ADR・コメントに埋まったローカル絶対パス (`/Volumes/...`、`/Users/...`)・メールアドレス・API key・keystore・`local.properties` 類。gitleaks / trufflehog 等の履歴スキャン + 手動 grep を想定。検出物が履歴にあれば履歴書き換えの要否を判断する。

### G. 配信経路の検証

| 案 | 評価 |
|---|---|
| ① Sample はソース参照のまま、配布物を参照する消費者プロジェクトを `verification/` に platform ごと最小 1 本持つ。release workflow の publish 前 (ローカルフィード / mavenLocal / file:// tag) に dry-run、publish 後に実レジストリ smoke | **採用** (オーナー 2026-08-21)。開発ループを壊さず、利用者視点の検証と README の雛形を兼ねる。KsDialogs PoC の方法論の資産化 |
| ② Sample を配布物参照に恒久切り替え | 却下。本体変更が Sample に映るのがリリース後になり開発ループが壊れる |
| ③ 検証時だけ Sample の参照を差し替え | 却下。切り替えスクリプトの保守が増える割に ① と検証範囲が変わらない |

## 決定事項

- C (補足): iOS は umbrella product `KsSettingsView` 1 本 (ADR-0018 に反映済み)
- C: Package.swift をルートへ移設。付随: samples/ios の参照を `../..` へ、ルート `.build/` を .gitignore へ、ADR-0001 の例外を ADR-0018 で明示、repository-boundaries.md / public-identifiers.md の表を蒸留時に追随。tag 表記 (`vX.Y.Z` / `X.Y.Z`) は F で KsDialogs と揃える
- A: 公開レジストリ一本 (SwiftPM / Maven Central / NuGet.org)。private 経路は作らない。リポジトリは public に切り替える (タイミングは F で扱う)
- B: lockstep 単一バージョン。version の SSoT は F2 で「dispatch 入力 (= tag) からの注入」に確定
- G: Sample はソース参照維持、`verification/` に消費者プロジェクト (publish 前 dry-run + publish 後 smoke)
- F1・F2: cross/ADR-0020 のとおり (dispatch 起動・tag は最後・version 注入)
- F6: CI 着手前に public 化。その前に機密情報・個人情報の混入チェック (履歴含む) を必須タスクとして行う
- F3〜F5 は推奨どおり (異議なし)
- E: maui/ADR-0025 のとおり (3 パッケージ、名前空間 `KsSettingsView` / Package ID `KsSettingsView.Maui`、CPM)
- D: android/ADR-0016 のとおり (groupId `jp.kamusoft`、単一 module / 単一 artifact `kssettingsview`、bridge 非公開、Central Portal)。テスト基盤 (JUnit5 + JUnit4/Robolectric) の同居と MAUI binding csproj の aar パス追随が実装時の作業
- 前段として `upgrade-android-build-toolchain` を先行実施する (同じ `build.gradle.kts` を触るための衝突回避・配信 CI のランナー構成確定のため)。同 change に libs.versions.toml 導入を含める (オーナー 2026-08-21)

## ADR 候補

- 作成済み (proposed): cross/ADR-0020 リリース起動と tag のタイミング (F1・F2)
- 作成済み (proposed): maui/ADR-0025 MAUI 3 パッケージ + 名前空間 (E)
- 作成済み (proposed): android/ADR-0016 Android 単一 module / 単一 artifact (D)
- 作成済み (proposed): cross/ADR-0018 配布チャネル + SwiftPM ルートマニフェスト (A + C)、cross/ADR-0019 lockstep (B)。いずれも KsDialogs cross/0008 / 0009 の翻案

## 未決の論点

- F4: NuGet Trusted Publishing の利用可否 (propose 時に確認)

## UI 素材

なし

## 変更級の推奨: ロードマップへエスカレーション (1 change に収まらない)

独立した change が 7〜8 本必要で、相互に依存順序がある。ksn-roadmap で「package-distribution」ロードマップとして起案し、本 exploration を起案の材料にする。

フェーズ案 (起案時の原案。番号は識別子であり実行順は依存で決まる。オーナー指示 2026-08-21: toolchain 更新もロードマップ内の phase-1 とし、proposal 済みのため in-progress で載せる):

| フェーズ | 内容 | 級 (暫定) | 依存 | 起案時 status |
|---|---|---|---|---|
| phase-1 android-build-toolchain | 既存 change `upgrade-android-build-toolchain` (Gradle 9 / AGP / Kotlin 更新 + libs.versions.toml 導入)。proposal / tasks / spec 改訂済み | M | なし | **in-progress** |
| phase-2 public-readiness | 機密情報・個人情報の混入チェック (履歴含む) → リポジトリ public 化 | S〜M (履歴書き換えが要るなら M) | なし | pending |
| phase-3 verification-ci | PR / push の 3 platform ビルド + テスト CI (F5) | M | phase-2 (無料ランナー) | pending |
| phase-4 ios-packaging | Package.swift ルート移設 + umbrella product、samples/ios 追随、ADR-0001 例外の concepts 追随 (C) | M (迷ったら 1 段上) | phase-3 | pending |
| phase-5 android-packaging | module 統合 + dir 改名 + group 変更 + catalog version + maven-publish / 署名 / Central Portal、Sample substitution、MAUI binding aar パス追随 (D) | L | phase-1、phase-3 | pending |
| phase-6 maui-packaging | 名前空間 `KsSettingsView` への改名 (下準備)、Directory.Build.props / Packages.props、IsPackable + メタデータ、pack PoC、Lifecycle 競合の NuGet 経由実証 (E) | L | phase-5 (aar パス) | pending |
| phase-7 consumer-verification | `verification/` の消費者プロジェクト 3 本 + ローカルフィード dry-run 手順 (G) | M | phase-4・5・6 | pending |
| phase-8 release-workflow | dispatch 起動の release workflow (F1〜F4)、Trusted Publishing / Central Portal / GPG の設定、prerelease ポリシー (`-{alpha|beta|rc}.N`)、初回リリース | L | phase-7 | pending |
| phase-9 docs | README インストール手順、旧 AiForms 移行ガイド (docs-refresh 経由、ユーザー明示依頼) | S | phase-8 (初回リリース後) | pending |

KsDialogs への逆流: phase-3・7・8 の成果 (workflow 定義・消費者プロジェクトの型) は KsDialogs phase-11 にそのまま流用できる。

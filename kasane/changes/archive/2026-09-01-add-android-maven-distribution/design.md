# Design: add-android-maven-distribution

## Context

android/ADR-0016 が Android の配布形 (単一 module `kssettingsview`・`jp.kamusoft:kssettingsview` 1 artifact・bridge 非公開) を決定済み。本 design は phase-5 のフェーズ議論 (roadmaps/package-distribution/phases/phase-5-android-packaging/history.md) で確定した実装方式を Decision 形式に転記し、ADR 抽出の原料と実装の指標にする。

現状の実測 (2026-09-01): 3 module は compileSdk 35 / minSdk 29 / JDK 17 / Compose 有効がすべて一致し、Kotlin パッケージ (`.core` / `.ui` / `.compose`) も分かれているためソース統合に衝突はない。res/ を持つのは ui のみ。テストランナーだけが分裂している (core = JUnit 5、ui / compose = JUnit 4 + Robolectric)。

## Goals / Non-Goals

proposal.md のとおり。要約: module 統合・識別子追随・発行設定の導入と monorepo 内消費者の追随まで。実発行・名前空間登録・release workflow は後続フェーズ。

## Decisions

### Decision 1: テスト基盤は JUnit Platform + junit-vintage-engine で両立させる

**採用案:** 統合 module の `testOptions` で `useJUnitPlatform()` を維持し、Jupiter (core 由来の JUnit 5) と junit-vintage-engine (ui / compose 由来の JUnit 4 + Robolectric) を testRuntimeOnly に並べる。既存テストは 1 本も書き換えない。ui 由来の `maxHeapSize = "2g"` と `isIncludeAndroidResources = true` は module 全体に適用する。

**理由:** vintage は JUnit 公式の互換エンジンで、Robolectric の `@RunWith` もそのまま通る。テスト資産無改変でリスクが設定数行に収まる。

**代替案:**
- **A: JUnit 4 に統一 (core のテストを書き戻す)** — 実行基盤は最も枯れるが、動いているテストの書き換えが発生する。却下
- **B: JUnit 5 に統一** — Compose のテスト Rule (`createComposeRule`) が JUnit 4 前提のため実質不可能。Compose 側が公式対応した時点で再検討する。却下

### Decision 2: group / version はルート build.gradle.kts で一括設定し、version の正は catalog に維持する

**採用案:** `group = "jp.kamusoft"` / `version` を各 module から消し、ルートの subprojects 一括設定に集約する。version の単一宣言元は catalog (キー名は `ks-settingsview` → `kssettingsview` に改名)。

**理由:** 宣言 1 か所。samples が catalog 経由で同じ版を読む phase-1 の共有の仕組みを維持でき、リリース CI の版注入 (cross/ADR-0020) も catalog 1 点で効く。

**代替案:**
- **A: 各 module に直書き (現状形)** — group 変更のような全体事項を module ごとに繰り返す形が残る。却下
- **B: gradle.properties (vanniktech 慣例) へ移す** — samples が読む catalog と版の宣言元が分裂し二重管理になる。却下

### Decision 3: 発行は vanniktech maven publish 0.37.0、release 単一 variant + sources jar + 空 javadoc jar

**採用案:** `com.vanniktech.maven.publish` 0.37.0 を本体 module にのみ適用。`publishToMavenCentral()` + `signAllPublications()`。`AndroidSingleVariantLibrary("release")` で sources jar 同梱・javadoc jar は空 (`JavadocJar.Empty()`)。POM は name / description / url / MIT license / developers / scm を public リポジトリ (cross/ADR-0021) の URL で記述。CI からの認証・署名は `ORG_GRADLE_PROJECT_mavenCentralUsername` / `mavenCentralPassword` / `signingInMemoryKey` 系 (phase-8 で使用)。SNAPSHOT は Central へ発行しない。

**理由:** 環境互換は Web 調査で確認済み (最低要件 JDK 17 / Gradle 9.0 / AGP 8.13 / KGP 2.2 をすべて満たす。旧 `SonatypeHost` 指定は削除済みで Central Portal が既定)。IDE の KDoc 表示は sources jar が担うため javadoc の実体は不要で、利用者向けドキュメントは skills/ と README の責務。

保証範囲: Central Portal 配線と `signAllPublications()` は設定として導入するが、本変更で実効を検証するのはローカル publication (`publishToMavenLocal`) の内容まで。署名鍵未設定のローカル発行は未署名で通る。実アップロード・署名付き成果物の検証は phase-7 (dry-run) / phase-8 (release workflow) の責務。

**代替案:**
- **A: Dokka で javadoc 実体を生成** — javadoc.io 等への掲載が得られるが、Dokka v2 の導入・維持コストとリリース時の生成時間を恒久的に抱える。利用者体験は sources jar で足りる。却下

### Decision 4: Sample の composite build は明示 dependencySubstitution を 1 本に更新して維持する

**採用案:** `substitute(module("jp.kamusoft:kssettingsview")).using(project(":kssettingsview"))` の 1 本に更新して維持する。

**理由:** 初回リリース後は Central に実 artifact が存在するため、自動置換の不発時に公開版へ静かにフォールバックし「本体の修正が Sample に映らない」壊れ方をする。明示置換は壊れたら必ずビルドエラーになる安全装置。

**代替案:**
- **A: 自動置換に任せて明示置換を削除** — 統合後は座標と project 名が一致し publication も生成されるため おそらく効くが、上記の無音フォールバック経路が生まれる。却下

### Decision 5: 利用側の Kotlin 要件は下限操作せず互換要件を明記する

**採用案:** languageVersion / apiVersion / stdlib の下限操作はしない。互換要件 (Kotlin 2.3+ / minSdk 29 / compileSdk 35) は README と skills/ の互換情報に明記する (docs-refresh 依頼に含める。POM に互換要件の標準フィールドはない)。

**理由:** 本当の制約は「利用者の Kotlin コンパイラ 2.3 以上」(Kotlin 2.4 生成メタデータの互換規則) で、stdlib 下限を下げてもこの制約は消えない。実行時の stdlib は Gradle の解決で自動昇格するため実害なし。

**代替案:**
- **A: languageVersion / apiVersion を下げ stdlib も固定** — メタデータ制約が残るため実利が薄く、2.4 の stdlib API 使用制限と設定の複雑さを恒久で抱える。却下

### Decision 6: 公開 API に現れる外部型の依存は `api` スコープ、テスト専用依存は release 発行物から除外する

**採用案:** 統合 module の依存を「公開 ABI に露出するか」で仕分ける。公開 API に現れる外部型 — `Theme` の `Color` / `TextStyle` / `Dp` (compose-ui)、`KsAnyView.Compose` の `@Composable` (compose runtime)、`SettingsRootStore` の `StateFlow` (kotlinx-coroutines-core)、既存の `androidx.annotation` — の依存は `api` にし、利用者の compile classpath に届くようにする。実装内部でしか使わない依存は `implementation` のまま。旧 compose module の `releaseImplementation("androidx.compose.ui:ui-test-manifest")` はテスト専用依存であり、release の発行物に混入しない構成 (unit test 専用の configuration) へ移す。

**理由:** 単純に依存の和集合を作ると、テスト専用ライブラリが POM の runtime 依存に載り、逆に公開 API が要求する型の依存が `implementation` のままでは「依存 1 行」の利用者コードがコンパイルできない。座標 1 点で完結する利用体験 (ADR-0016) の成立条件。

**代替案:**
- **A: 全依存を `implementation` のまま和集合にする (現状形の踏襲)** — module 内部の参照は解決できるが、外部消費者の compile classpath に公開 API の型が届かない。却下
- **B: スコープ設計を phase-7 (消費者検証) に先送りする** — 発行物の形が後から変わり、phase-7 の検証やり直しを招く。発行物を作る本変更で確定するのが正位置。却下

## Risks / Trade-offs

- vintage engine 経由の Robolectric 実行は広く使われる構成だが本リポジトリでは初。統合後の全テスト green (旧 3 module の合計件数と一致) を受け入れ条件とする
- MAUI アプリに compose 層クラスが同梱されるようになる (実行時依存は不変・サイズ微増のみ。ADR-0016 織り込み済み)
- module 境界による層の compile 時強制を失う (ADR-0016 Consequences 記載。iOS の target 分割が core 契約の誤依存を検出する)

## Migration Plan

1. `git mv` で core → `kssettingsview`、bridge → `kssettingsview-bridge` に改名し、ui / compose のソース・テスト・res を `kssettingsview` へ統合する (Kotlin パッケージ維持のためファイル衝突なし)
2. build.gradle.kts を単一化 (namespace 単一化・R 明示 import・依存の和集合・テスト基盤同居)、settings.gradle.kts / ルート build.gradle.kts / catalog を追随
3. 発行 plugin を導入し `publishToMavenLocal` で発行物を検証
4. samples → MAUI binding → handbook / ADR-0018 表の順に追随
5. 全テスト・Sample ビルド・binding ビルドの green を確認

## Open Questions

なし (フェーズ議論で全論点解消済み)。

## ADR 候補

なし。配布形は android/ADR-0016、配布先は cross/ADR-0018、groupId は cross/ADR-0002 が既に保持しており、本 design の Decision 1〜5 はいずれも設定レベルで可逆 (覆すコスト低・境界を越えない・将来の制約は ADR-0016 側に含まれる)。

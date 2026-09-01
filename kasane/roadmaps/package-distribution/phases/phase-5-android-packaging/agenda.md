# phase-5-android-packaging

core / ui / compose を単一 module `android/kssettingsview` に統合し、`jp.kamusoft:kssettingsview` として Maven Central へ発行できる形にする (android/ADR-0016)。bridge は `android/kssettingsview-bridge` に改名し非公開のまま維持する。

## 論点

(全論点解消済み — 決定事項へ移動)

## 決定事項

### テスト基盤の同居: 既存テストを書き換えず JUnit Platform + vintage engine で両立させる

統合 module のユニットテストは JUnit Platform (`useJUnitPlatform()`) で実行し、core 由来の JUnit 5 テストは Jupiter engine、ui / compose 由来の JUnit 4 + Robolectric テストは junit-vintage-engine で動かす。既存テストは1本も書き換えない。ui 由来のヒープ設定 (maxHeapSize 2g) と `isIncludeAndroidResources = true` は module 全体に適用する。Compose のテスト Rule (`createComposeRule`) が JUnit 4 前提のため JUnit 5 への統一は現状不可能で、Compose 側が公式対応した時点で寄せ直す余地を残す (2026-09-01)。

### AGP namespace は `jp.kamusoft.kssettingsview`、R 参照は明示 import で追随

統合 module の namespace は座標 (android/ADR-0016) と揃えて `jp.kamusoft.kssettingsview` とする。res を持つのは旧 ui module のみ (drawable 1 / values 2) で統合による衝突はない。旧 ui パッケージ内の自モジュール R 参照 (矢印アイコン・同梱テーマ・テスト) は同一パッケージ解決が効かなくなるため `jp.kamusoft.kssettingsview.R` の明示 import を足す。ADR-0013 (宣言元ライブラリの R 経由) は外部ライブラリの話で無風、bridge も現状維持 (2026-09-01)。

### ディレクトリ / project 名は `kssettingsview` / `kssettingsview-bridge`、rootProject.name も揃える

ADR-0016 のとおり統合 module は `android/kssettingsview`、bridge は `android/kssettingsview-bridge` に `git mv` で改名する。`rootProject.name` (現 `ks-settingsview`) も `kssettingsview` に揃え、旧表記を残さない。bridge の module 依存は `project(":kssettingsview")` 1 本になる。Sample composite build と MAUI csproj の追随は別論点で扱う (2026-09-01)。

### group / version はルート build.gradle.kts で一括設定、version の正は catalog に維持

`group = "jp.kamusoft"` / `version` は各 module の build.gradle.kts から消し、ルートの subprojects 一括設定に集約する。version の単一宣言元は catalog (samples の composite build と共有する phase-1 の仕組みを維持)。catalog のキー名は `ks-settingsview` → `kssettingsview` に改名し、samples 側の参照追随は Sample の論点で扱う (2026-09-01)。

### 発行は vanniktech maven publish 0.37.0、release 単一 variant + sources jar + 空 javadoc jar

`com.vanniktech.maven.publish` **0.37.0** を本体 module (`kssettingsview`) にのみ適用する (bridge は非公開のため適用しない)。環境互換は確認済み (最低要件 JDK 17 / Gradle 9.0 / AGP 8.13 / Kotlin 2.2 をすべて満たす。既知の非互換 issue なし)。構成は `publishToMavenCentral()` (旧 `SonatypeHost` は削除済みで Central Portal が既定) + `signAllPublications()`。variant は `AndroidSingleVariantLibrary` の release 単一 variant で、sources jar 同梱・javadoc jar は空 (`JavadocJar.Empty()`) — IDE の KDoc 表示は sources jar が担い、利用者向けドキュメントは skills/ と README の方針のため Dokka は持ち込まない。POM は name / description / url / MIT license / developers / scm を public リポジトリ (cross/ADR-0021) の URL で記述する。CI からの認証・署名は `ORG_GRADLE_PROJECT_mavenCentralUsername` / `mavenCentralPassword` / `signingInMemoryKey` 系のインメモリ渡し (phase-8 で使用)。SNAPSHOT は Central へ発行しない (2026-09-01)。

### `jp.kamusoft` の Central Portal 登録は人の作業として change スコープ外

名前空間登録 (Portal アカウント作成 → `jp.kamusoft` 登録 → kamusoft.jp の DNS TXT 追加 → 検証) は現時点で未実施。アカウントとドメインに紐づく人の作業のため実装 change には含めず、TODO で管理する。一度検証すれば KsDialogs 含む `jp.kamusoft` 配下の全 artifact で共用できる。発行の実地検証 (phase-7 / phase-8) の前提 (2026-09-01)。

### Sample の composite build は明示 dependencySubstitution を 1 本に更新して維持

統合後は座標と project 名が一致し publication も生成されるため自動置換はおそらく効くが、初回リリース後は Central に公開版が存在するため、自動置換の不発時に公開版へ静かにフォールバックする危険がある。明示置換 (`jp.kamusoft:kssettingsview` → `:kssettingsview` の 1 本) なら壊れたら必ずビルドエラーになるので安全装置として維持する。Sample アプリの依存 3 行は 1 行に、catalog キー参照は `kssettingsview` に追随 (2026-09-01)。

### MAUI binding csproj は aar 2 本構成へ機械的追随

`AndroidLibrary` は `kssettingsview-release.aar` (Bind=false) + `kssettingsview-bridge-release.aar` (Bind=true) の 2 本に更新する。増分ビルド判定のソース glob と gradlew task 名 (`:kssettingsview:assembleRelease` 等) も追随。compose は従来 MAUI に同梱されていなかったが統合 aar に含まれるようになる — 実行時依存は増えず (ui が Compose 一式に依存済み、ADR-0016 織り込み済み)、影響はアプリサイズ微増のみ。束縛対象 (bridge) は不変で binding API に影響なし (2026-09-01)。

### ドキュメント追随は「規範は change 同梱・記述は蒸留・利用者向けは明示依頼」の 3 段仕分け

実装 change に同梱: handbook/cross/public-identifiers.md (artifactId 規則・namespace 表・Maven 座標の現在地の節) と cross/ADR-0018 の配布先表 (ADR-0016 決定済み座標への表記追随)。蒸留 (ksn-distill) で追随: concepts/android/architecture/build-toolchain.md ほか diff から洗い出す concepts、Android README。ユーザーの明示依頼で docs-refresh: skills/ とルート README 群 (座標は暫定値として記載済みのため差分は module 構成の説明程度) (2026-09-01)。

### 利用側の Kotlin 要件は下限操作せず互換要件を明記する

phase-1 申し送りの解消。本当の制約は「利用者の Kotlin コンパイラ 2.3 以上」(Kotlin 2.4 生成メタデータの互換規則) で、stdlib 下限を意図的に下げてもこの制約は消えないため下限操作はしない (languageVersion / apiVersion の引き下げも採らない)。実行時の stdlib は Gradle の解決で自動昇格するため実害なし。互換要件 (Kotlin 2.3+ / minSdk 29 / compileSdk 35) は README と skills/ の互換情報に明記する — docs-refresh 依頼に含める (POM に互換要件の標準フィールドはない) (2026-09-01)。

## TODO

- [x] 論点の解消 (2026-09-01 全 10 論点を決定事項へ)
- [x] **人の作業 (オーナー)**: Central Portal アカウントで `jp.kamusoft` 名前空間を登録し DNS TXT 検証を通す → 未実施のまま phase-7 の TODO へ移送 (2026-09-01 蒸留)
- [x] ksn-propose で変更提案を起こす (2026-09-01 [changes/archive/2026-09-01-add-android-maven-distribution](../../../../changes/archive/2026-09-01-add-android-maven-distribution/proposal.md))
- [x] **phase-9 からの申し送り** (2026-08-30): `kasane/handbook/cross/public-identifiers.md` の artifactId 規則を単一 artifact (`jp.kamusoft:kssettingsview`) へ改訂し (android/ADR-0016)、cross/ADR-0018 の配布先の表も追随させる → 実装 change に同梱して完了 (2026-09-01)

## 実装結果 (2026-09-01 反映)

change [changes/archive/2026-09-01-add-android-maven-distribution](../../../../changes/archive/2026-09-01-add-android-maven-distribution/proposal.md) で実装完了 (verify VALID / review 2 サイクル APPROVED)。決定事項からの乖離は deviation.md に 2 件 + 付随修正:

- `api` スコープの列挙拡張 2 件: 公開 ABI に露出する外部型の原理適用で `androidx.recyclerview` (オーナー承認) と `androidx.compose.foundation:foundation-layout` (レビュー Major) を追加。原理と列挙の併記時は公開面の機械走査で検算する教訓を lessons/inbox に捕捉済み
- 追加の安全策: version が `-SNAPSHOT` の間は Central 向け発行タスクをガードで失敗させる実装を追加 (スペック外・レビュー合意)

申し送り (受け皿確定済み):

- Central Portal `jp.kamusoft` 名前空間登録 (人の作業) → phase-7 agenda TODO へ移送
- Explicit API mode の導入 (公開/内部境界のコンパイラ強制。レビュー起点) → [changes/adopt-android-explicit-api-mode](../../../../changes/adopt-android-explicit-api-mode/exploration.md) として簡易起票済み。phase-7 の消費者検証と併せる推奨を phase-7 agenda TODO に記載
- docs-refresh の明示依頼 (skills/ とルート README の追随、互換情報 Kotlin 2.3+ / minSdk 29 / compileSdk 35 の明記) → phase-7 agenda TODO へ記載
- Maven Central への実発行・署名付き成果物の検証 → phase-7 (dry-run) / phase-8 (release workflow) の既定スコープ (proposal Non-Goals のとおり)
- KsDialogs への同型展開 → 見送り (本ロードマップの非ゴール。KsDialogs 側フェーズの責務)

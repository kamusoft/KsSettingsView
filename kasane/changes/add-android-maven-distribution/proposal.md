# Proposal: add-android-maven-distribution

## Why

Android の配布は「core / ui / compose を単一 module に統合し `jp.kamusoft:kssettingsview` 1 artifact で Maven Central へ」と決定済み (android/ADR-0016) だが、現状は旧 3 module 構成のまま発行設定も存在しない。module の物理統合・識別子の座標追随・発行の仕組み (vanniktech maven publish) を導入し、消費者検証 (phase-7) と release workflow (phase-8) が乗る土台を作る。

## What Changes

- `android/`: `ks-settingsview-{core,ui,compose}` の 3 module を単一 module `kssettingsview` に物理統合する (Kotlin パッケージ `.core` / `.ui` / `.compose` は維持、`git mv` で履歴追跡)。`ks-settingsview-bridge` は `kssettingsview-bridge` に改名し非公開のまま維持
- テスト基盤: 統合 module のユニットテストを JUnit Platform に載せ、core 由来の JUnit 5 は Jupiter、ui / compose 由来の JUnit 4 + Robolectric は junit-vintage-engine で既存テスト無改変のまま実行する
- 識別子: AGP namespace を `jp.kamusoft.kssettingsview` に単一化 (自モジュール R 参照は明示 import で追随)、`group = "jp.kamusoft"` / version はルート build.gradle.kts の一括設定に集約 (version の正は catalog、キー名 `ks-settingsview` → `kssettingsview`)、`rootProject.name` も `kssettingsview` へ
- 発行: `com.vanniktech.maven.publish` 0.37.0 を本体 module にのみ導入。`publishToMavenCentral()` + `signAllPublications()`、release 単一 variant + sources jar + 空 javadoc jar、POM (MIT / scm / developers は public リポジトリ URL)。SNAPSHOT は発行しない。**Central Portal 配線と署名は設定の導入のみで、その実効検証 (実アップロード・署名付き成果物) は phase-7 / phase-8 の責務** — 本変更の検証はローカル publication の内容まで。あわせて公開 API に現れる外部型の依存を `api` スコープに設計し直し、テスト専用依存が release の発行物に混入しない構成にする
- `samples/android/`: 明示 `dependencySubstitution` を 1 本 (`jp.kamusoft:kssettingsview` → `:kssettingsview`) に更新して維持、アプリ依存 3 行 → 1 行、catalog キー参照の追随
- `maui/android/KsSettingsView.Binding.Android/`: `AndroidLibrary` の aar を 3 本 → 2 本に更新 (ソース glob・gradlew task 名も追随)
- `kasane/handbook/cross/public-identifiers.md`: artifactId 規則 (`ks-settingsview-*` → 単一 `kssettingsview`)・namespace 表・「Maven 座標の現在地」節を改訂する (phase-9 申し送りと phase-5 agenda 決定を出典とする承認済み規範改訂)
- `kasane/decisions/cross/0018` (accepted): 配布先表は書き換えず、同 ADR の既存慣行に倣う**日付付き追記**で Android 座標の統合 (`jp.kamusoft:kssettingsview`、android/ADR-0016) を注記する (履歴を保つ)

影響する能力: Android の配布 (Maven 座標と発行経路)、Android ビルド入口 (module 構成)、MAUI binding のビルド入力、Android Sample のソース参照

## Non-Goals

- Maven Central への実発行と `jp.kamusoft` 名前空間登録 — 名前空間登録はアカウント・ドメインに紐づく人の作業 (agenda TODO で管理)、実発行の検証は phase-7 / phase-8 の責務。本変更は `publishToMavenLocal` で発行物 (aar / sources jar / POM) の内容検証まで
- release workflow への組み込み・Portal トークンと GPG 鍵の secrets 登録 — phase-8 の責務
- `kssettingsview-bridge` の Maven 公開 — ADR-0016 で却下済み
- Dokka による javadoc 実体生成 — agenda 決定 (空 javadoc jar) のとおり。利用者向けドキュメントは skills/ と README が担う
- concepts (build-toolchain.md 等) と Android README の追随 — 蒸留時の定型作業。skills/ とルート README は docs-refresh の明示依頼 (互換情報 Kotlin 2.3+ / minSdk 29 の明記を含む)
- KsDialogs への同型展開 — KsDialogs 側フェーズの責務

## Impact

- 破壊的変更: 開発用 GAV (`jp.kamusoft.kssettingsview:ks-settingsview-*`) と module パスが変わるが、未リリースのため実利用者はゼロ。monorepo 内の消費者 (samples / MAUI binding) は本変更内で追随する
- リスク: ① vintage engine 経由の Robolectric 実行は広く使われる構成だが本リポジトリでは初 — 統合後の全テスト green を受け入れ条件とする。② MAUI アプリに compose 層クラスが同梱されるようになる (実行時依存は不変、サイズ微増のみ — ADR-0016 織り込み済み)。③ maui-support 側の change と並走させない (roadmap 前提 / マージ衝突回避)
- 外部リソース: 新規なし (Central Portal 登録は本変更のスコープ外)

## 級: L

複数能力横断 (Android ビルド・配布・MAUI binding・Sample) かつ公開座標という覆すコストの高い決定の実装のため L。設計判断は ADR-0016 とフェーズ議論で確定済みであり、design.md は agenda 決定事項の Decision 形式への転記+実装方式の確定 (テスト基盤・置換・発行構成) に絞る。

domain: cross
roadmap: package-distribution/phase-5-android-packaging

# phase-5-android-packaging 議論履歴

## 2026-09-01: source set 統合とテスト基盤の同居

3 module の build.gradle.kts を実測: compileSdk 35 / minSdk 29 / JDK 17 / Compose 有効はすべて一致し、Kotlin パッケージ (`.core` / `.ui` / `.compose`) も分かれているためソース統合に衝突はない。唯一の本質的衝突がテストランナー (core = JUnit 5 `useJUnitPlatform()`、ui / compose = JUnit 4 + Robolectric、compose は `createComposeRule` が JUnit 4 Rule 前提)。

選択肢: A) JUnit Platform + junit-vintage-engine で両方そのまま同居 / B) core を JUnit 4 に書き戻して統一 / C) JUnit 5 に統一。C は Compose テスト Rule が JUnit 4 前提のため実質不可。B は実行基盤が最も枯れるが動いているテストの書き換えが発生。**A を採用** — 既存テストを1本も書き換えず、公式の vintage engine で JUnit 4 テストを JUnit Platform 上で実行する。ui 由来の maxHeapSize 2g と `isIncludeAndroidResources` は module 全体に適用。Compose が公式に JUnit 5 対応したら C へ寄せ直す余地を残す。

## 2026-09-01: AGP namespace 単一化と R クラス参照

実測: res/ を持つのは ui のみ (ic_navigate_next / themes.xml / ids.xml)、core・compose は res なしで統合衝突なし。自モジュール R 参照は ui 層の数か所 (ViewHolder の setImageResource、KsThemedContext の同梱テーマ、テスト 1 件) で、現在は同一パッケージ解決に依存。namespace を `jp.kamusoft.kssettingsview` に単一化すると R の居場所が変わるため明示 import で追随する。ADR-0013 は外部ライブラリ R の参照規約で今回は無風、改訂不要。bridge は別 module のまま namespace 現状維持。選択の分岐はなく確認事項として **namespace `jp.kamusoft.kssettingsview` + 明示 import 追随を確定**。新規 ADR 不要 (ADR-0016 の座標への機械的追随)。

## 2026-09-01: ディレクトリ / project 名の改名

module パス参照は 3 か所と実測 (bridge の project() 依存 2 本 → 統合後 1 本、samples/android の composite build 3 ファイル、MAUI binding csproj)。改名は ADR-0016 決定済みのため確認のみ。追加提案として ADR に明記のない `rootProject.name` も `ks-settingsview` → `kssettingsview` に揃えることを提示し**採用** — 識別子の読みを全箇所 `kssettingsview` に統一し将来の迷いを消す。改名は `git mv` で履歴追跡を保つ。

## 2026-09-01: group / version の集約先

実測: version は catalog `ks-settingsview = "0.1.0-SNAPSHOT"` に集約済みで 4 module + samples が参照、group は各 module に直書き。選択肢: A) ルート build.gradle.kts の subprojects 一括設定 + version は catalog 維持 / B) 各 module 直書き継続 / C) gradle.properties (vanniktech 慣例) へ移動。C は samples が読む catalog と版の宣言元が分裂し二重管理になるため回避。**A を採用** — 宣言 1 か所、samples との版共有維持、リリース CI の版注入 (ADR-0020) も catalog 1 点で効く。catalog キーは `kssettingsview` に改名。

## 2026-09-01: 発行の仕組み (vanniktech maven publish)

ksn-scout の Web 調査で互換を確定: 最新安定版 0.37.0 (2026-06-21)、最低要件 JDK 17 / Gradle 9.0 / AGP 8.13 / KGP 2.2 で本リポジトリ (JDK 17 / Gradle 9.5.0 / AGP 8.13.2 / Kotlin 2.4.10) は全部満たす。Kotlin 2.4 / AGP 8.13 の既知非互換 issue なし。`SonatypeHost` は 0.34.0 で削除済みで `publishToMavenCentral()` だけで Central Portal に向く。Central Portal は SNAPSHOT ホスティングも提供する (90 日で削除、plugin の SNAPSHOT 発行に open issue #1369 あり) が、本プロジェクトは SNAPSHOT を発行しないため無関係。

決定: 0.37.0 を本体 module のみに適用、release 単一 variant + sources jar + 空 javadoc jar、POM は MIT / public リポジトリ URL、認証・署名は ORG_GRADLE_PROJECT_* のインメモリ渡し。javadoc の選択肢は A) 空 jar (採用) / B) Dokka 生成。ユーザーから「javadoc なしだと IDE で KDoc 見れないのか」の質問があり、IDE は sources jar から KDoc を表示するため影響なし・javadoc jar の実益は javadoc.io 等の外部サイト掲載のみと回答して A を確定。ADR は起こさない (設定レベルで可逆、配布先の決定は cross/ADR-0018 が既に担う)。

## 2026-09-01: `jp.kamusoft` の Central Portal 登録

オーナーに確認し**未登録**と判明。名前空間登録 (アカウント作成 → 登録 → DNS TXT → 検証) はアカウント・ドメインに紐づく人の作業のため、実装 change のスコープ外とし TODO (人の作業) で管理する。期限は発行の実地検証 (phase-7 / 8) まで。検証は一度で `jp.kamusoft` 配下全 artifact (KsDialogs 共用) に効く。

## 2026-09-01: Sample の composite build の置換方式

実測: samples/android/settings.gradle.kts に「AGP は既定で publication を生成せず自動置換が発火しない」実測コメントつきで明示 dependencySubstitution 3 本。統合後は group:name = GAV が一致し vanniktech で publication も生えるため自動置換は「おそらく効く」。選択肢: A) 明示置換 1 本に更新して維持 / B) 自動置換に任せて削除。初回リリース後は Central に実 artifact が存在し、B は置換不発時に公開版へ無音フォールバックして「本体の修正が Sample に映らない」壊れ方をする。**A を採用** — 壊れたら必ずビルドエラーになる安全装置として明示置換を残す。

## 2026-09-01: MAUI binding csproj の aar パス追随

実測: 現在は bridge (Bind=true) + core / ui (Bind=false) の 3 本で compose は非同梱。統合後は kssettingsview + kssettingsview-bridge の 2 本へ機械的追随 (aar パス・ソース glob・gradlew task 名)。実質変化は compose 層クラスが統合 aar 経由で MAUI アプリに同梱されるようになる点のみで、実行時依存は不変 (ADR-0016 織り込み済み)・サイズ微増のみと確認して確定。分岐なし。

## 2026-09-01: ドキュメント追随のタイミング

public-identifiers.md (artifactId 規則 `ks-settingsview-*`・namespace 3 行・「Maven 座標の現在地」節) と cross/ADR-0018 の配布先表が旧座標のままと実測。仕分けを提示し確定: 規範 (handbook・ADR の表) は古いままだと後続作業者が誤るため実装 change に同梱、記述 (concepts・Android README) は蒸留で追随、利用者向け (skills/・ルート README) は docs-refresh の明示依頼。phase-9 申し送り TODO はこの change 同梱分に該当。

## 2026-09-01: 利用側の Kotlin 要件 (phase-1 申し送り)

制約を分解: コンパイル時は Kotlin メタデータ互換 (1 マイナー先まで読める) により利用者コンパイラ 2.3+ が本当の下限、実行時は stdlib が Gradle 解決で自動昇格し実害なし。選択肢: A) 下限操作せず README / skills に互換要件 (Kotlin 2.3+ / minSdk 29 / compileSdk 35) を明記 / B) languageVersion / apiVersion + stdlib 固定で下限を下げる。B は下げてもメタデータ制約が残り実利が薄く、2.4 stdlib API の使用制限と設定複雑化を恒久で抱える。**A を採用**。明記は docs-refresh 依頼に含める。これで phase-5 の全論点が解消。

# レビュー結果: add-android-maven-distribution (001 回目)

**日付**: 2026-09-01
**判定**: CHANGES_REQUESTED

## サマリー

module 統合・識別子の一本化・vanniktech maven publish の導入・monorepo 内消費者の追随はいずれもデルタスペックの Requirement を満たしており、ビルドとテストは再実行で green (統合 module 1183 × 2 + bridge 167 × 2 = 2700 件 / 失敗 0)、発行物・Sample 置換・binding・IntegrationHost の証跡も実在と内容が一致している。ただし design Decision 6 の原理 (公開 ABI に露出する外部型の依存は `api`) を発行物の実体に対して機械的に照合したところ、`androidx.recyclerview` (deviation で解消済み) と同型の漏れがもう 1 件残っている — 公開 `Theme` の `sectionMargin` が露出する `androidx.compose.foundation.layout.PaddingValues` が `implementation` のままで、POM / Gradle Module Metadata の compile スコープに載っていない。利用者向けドキュメントが実際に例示している経路であり、「依存 1 行で完結」(android/ADR-0016) が成立しないため Major とする。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 改名に伴うコメント更新全件。変更識別子の裸参照 (`purify-core-extract-style-to-ui-layer`) が `core/ADR-0009` へ置換され、履歴記述も残っていない。`scripts/comment-policy-lint.py` は 699 ファイル / 禁止 0 件
- `kasane/handbook/cross/public-identifiers.md` — `**/build.gradle.kts` / `**/*.csproj` を触るため。namespace 表・artifactId 規則・「Maven 座標の現在地」の改訂内容と、`android/kssettingsview/build.gradle.kts` / `android/build.gradle.kts` / `android/gradle/libs.versions.toml` の実体が一致していることを節ごとに照合
- `kasane/handbook/cross/test-execution.md` — テストを実行し件数を報告するため。`--rerun-tasks` なしの再実行が up-to-date になる点を踏まえ、`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の `tests` / `failures` 属性を集計して確認
- `kasane/handbook/cross/local-development-setup.md` (guide) / `kasane/handbook/cross/user-skill-api-listing.md` — 本変更が改訂しているため内容を照合
- `kasane/handbook/maui/integration-host-verification.md` の期待表 — evidence の 2 枚の画像を実際に開き、記載された各行・色との対応を確認
- 適用外と判定: `cross/sample-parity.md` (Sample のデモ画面・文言・デモデータの変更なし。依存宣言と comment のみ)、`cross/runtime-behavior-verification.md` (実行時挙動の不具合調査ではない)、`cross/aiforms-origin-reference.md`、`maui/performance-verification.md`、`ios/*`
- 参照した決定: `kasane/decisions/android/0016-single-module-single-maven-artifact.md`、`kasane/decisions/android/0013-resource-reference-via-declaring-library-r-class.md` (R の明示 import が自モジュール宣言の R を指しており適合)、`kasane/decisions/android/0020-bundled-theme-always-wrap-host-independent.md`、`kasane/decisions/cross/0002` / `0018`、`kasane/decisions/maui/0006`
- lessons: `kasane/lessons/code-review.md` (L-001)、`kasane/lessons/process.md` (L-003 = 証跡の実在と提出コードの対応を判定条件にする / L-005 = 到達可能な修正はこのサイクル内で直す / L-006 = 不在の断定は全走査してから)

## deviation の確認

- `deviation.md:3` の recyclerview 追加は合意済み差分として扱った。`samples/android/app/build.gradle.kts` からの recyclerview 1 行削除も、本体の `api` から推移するため妥当 (Sample ビルドで実効確認済み)
- `deviation.md:4` の `[付随修正]` は対象が 19 ファイル前後あり ksn-core の同梱条件③ (目安 3 ファイル以内) を数の上では超えるが、①〜⑤ の全項目が「本変更の改名によって記述が成り立たなくなった箇所の機械的な文言・パス置換」であり、公開 API・スキーマ・ADR の決定に触れず、新しい抽象も分岐判断も持ち込んでいない。本務の後始末の範囲と判断し、スコープ膨張としては指摘しない

## 指摘事項

### [🟠 Major] 公開 `Theme.sectionMargin` が露出する `PaddingValues` の依存が `api` になっていない

**該当箇所**: `android/kssettingsview/build.gradle.kts:189` / `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:113`

**問題点**:

`Theme` は public な data class で、`val sectionMargin: PaddingValues? = null` を持つ。`PaddingValues` の宣言元は `androidx.compose.foundation:foundation-layout` (`androidx.compose.foundation:foundation` の推移先) だが、build.gradle.kts では `implementation("androidx.compose.foundation:foundation")` のままである。結果として発行物の依存スコープは次のようになっている (`~/.m2` の実発行物を検査):

- POM: `androidx.compose.foundation:foundation` は `<scope>runtime</scope>`
- `.module`: `foundation` は `releaseVariantReleaseRuntimePublication` にのみ存在し、`releaseVariantReleaseApiPublication` には無い

aar の `classes.jar` を javap で全走査したところ、Kotlin ソース上 public な宣言のうち `api` に無い外部型を露出しているのは `Theme` だけで、他はすべて `internal`(`CellBaseViews` / `KsThemedContext` / 各 `*SelectionSheet` / `SectionBoxMetrics` 等) だった。つまり本件は列挙漏れの残り 1 件である。

- `public jp.kamusoft.kssettingsview.ui.Theme(..., androidx.compose.foundation.layout.PaddingValues, ...)`
- `public final androidx.compose.foundation.layout.PaddingValues getSectionMargin();`
- `copy-GMsY0QE(...)` / `component30()` も同じ型を含む

`jp.kamusoft:kssettingsview` の 1 行だけを依存に書いた利用者の compile classpath に `foundation-layout` が届かないため、`theme.sectionMargin` の参照・`Theme(...)` の生成・`theme.copy(...)` が解決できない。これは design.md Decision 6 が「座標 1 点で完結する利用体験 (android/ADR-0016) の成立条件」として置いた原理そのものに反し、`deviation.md:3` の recyclerview と同一の失敗型である (spec の列挙 4 件は原理の例示であり、原理側 —「公開 API に現れる外部型の依存を compile (`api`) スコープで含む」— から見て漏れ)。

利用者向けドキュメントが実際にこの経路を案内している点で机上の懸念ではない:

- `skills/ja/kssettingsview-android/references/styling.md:6` が `import androidx.compose.foundation.layout.PaddingValues`、同 `:195` が `sectionMargin = PaddingValues(start = 16.dp, ...)` を例示 (en 版も同じ行)
- `README.md:54` の案内は `implementation("jp.kamusoft:kssettingsview:0.1.0")` の 1 行のみ

Sample ビルドではこの欠落を検出できない。`samples/android/app/build.gradle.kts:73` が自前で `androidx.compose.foundation:foundation` を宣言しており、かつ Sample は `sectionMargin` を使っていないため。

**推奨修正**:

`android/kssettingsview/build.gradle.kts` の `api` 群へ、宣言元 artifact を明示して 1 行足す。

```kotlin
// PaddingValues。`Theme` の `sectionMargin` が値型に使う。
api("androidx.compose.foundation:foundation-layout")
```

`implementation("androidx.compose.foundation:foundation")` を `api` へ格上げする形でも `foundation → animation → foundation-layout` の api 連鎖で届くが、`foundation` 自身の `androidApiElements` に `foundation-layout` は含まれず (runtime にのみ含まれる) 間接的な成立になるため、宣言元 artifact を直接 `api` に置くほうが android/ADR-0013 の考え方とも揃い壊れにくい。

spec の列挙 4 件を超える追加になるため、recyclerview と同じくオーナー確認のうえ `deviation.md` に記録し、`evidence/publish-to-maven-local.txt` の POM / `.module` の依存スコープ一覧を再取得すること。

### [🟡 Minor] docs-refresh スキルが参照する Android ビルドファイルのパスが本変更で失効している

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:180`

**問題点**: 「ツール最低バージョン」の取得元として `minSdk・compileSdk: android/ks-settingsview-ui/build.gradle.kts` を指しているが、このファイルは本変更の統合で消滅した。docs-refresh を起動しても該当行を読めず、対応プラットフォーム表・開発環境要件の追随が静かに落ちる。本変更の改名が直接原因で失効した参照であり、修正は 1 語の置換で閉じる (lessons process L-005)。

`skills/` と README 本文の追随が Non-Goal であることとは別件で、これはスキル定義側が持つソース参照の失効である。

**推奨修正**: `android/kssettingsview/build.gradle.kts` に置き換える。

### [🔵 Suggestion] 改訂した handbook 2 本の `timestamp` が据え置かれている

**該当箇所**: `kasane/handbook/cross/local-development-setup.md:8` / `kasane/handbook/cross/user-skill-api-listing.md:9`

**問題点**: いずれも 2026-09-01 に本文を改訂しているが `timestamp: 2026-08-29` のまま。`timestamp` は最終検証日であり、改訂した箇所については当日確認していることになる (`public-identifiers.md` / `test-execution.md` は 2026-09-01 に更新済みで、2 本だけ揃っていない)。

**推奨修正**: 2 本を `2026-09-01` に揃える。ksn-drift の棚卸し時にまとめて扱う方針なら現状維持でもよい。

### [🔵 Suggestion] android/ADR-0016 の status が `proposed` のまま

**該当箇所**: `kasane/decisions/android/0016-single-module-single-maven-artifact.md:4`

**問題点**: 本変更で Decision の全項目 (単一 module 統合・`jp.kamusoft:kssettingsview` 1 artifact・bridge 非公開・group の `jp.kamusoft` 化・artifactId のハイフン規則) が実装され、実発行物まで確認されている。ADR の status 更新は蒸留 (ksn-distill) の担当だが、cross/ADR-0018 と同型の「実装結果の追記と accepted 昇格」が要る状態にあることを申し送る。

**推奨修正**: 本変更では触らず、蒸留時に accepted へ昇格させる。

### [🔵 Suggestion] concepts / ADR 出典に残る旧 module 名 (Non-Goal のため違反ではない)

**該当箇所**: `kasane/concepts/android/architecture/build-toolchain.md:17,40` / `kasane/concepts/cross/architecture/repository-boundaries.md:18,21` / `kasane/concepts/maui/api/native-bridge.md:29,72` / `kasane/concepts/core/cells/ks-image.md:13` / `kasane/concepts/core/core-model/settings-tree.md:9` / `kasane/concepts/core/cells/input-cells.md:47` / `kasane/decisions/core/0013-extensible-cell-abstractions.md:46,48` / `kasane/decisions/core/0014-customcell-content-value-with-builder.md:58` / `kasane/decisions/core/0017-customcell-disabled-suppression-over-a11y-symmetry.md:35` / `kasane/decisions/maui/0005-bridge-ownership-model.md:15` / `kasane/decisions/maui/0006-android-binding-gradlew-exec.md:15`

**問題点**: proposal.md の Non-Goals が concepts の追随を蒸留時の定型作業としているため本変更の違反ではない。ただし build-toolchain.md は catalog キー名 (`ks-settingsview`) と GAV (`jp.kamusoft.kssettingsview:ks-settingsview-*`) まで旧値のままで、Android のビルド契約を読む次の作業者が誤った前提を持つ。ADR の `出典:` 行は当時の実体を指す歴史記述なので書き換え対象かどうかは判断が要る。

**推奨修正**: 蒸留の対象一覧に上記を明示して渡す。ADR 出典行は歴史記述として据え置く判断でよい (その判断自体を蒸留で明示する)。

### [🔵 Suggestion] Explicit API mode の未適用

**該当箇所**: `android/kssettingsview/build.gradle.kts`

**問題点**: Maven Central へ配る公開ライブラリになったにもかかわらず `explicitApi()` が未設定で、公開/内部の境界がコンパイラで強制されない。本 Major が示すとおり「どれが公開 API か」の判定は今後も繰り返し必要になる。本変更のスコープ外であり、全ソースへ `public` 修飾子を足す大きな差分になるため、ここで直すべきではない。

**推奨修正**: 別 change として起票を検討する (phase-7 の消費者検証と併せると効果が高い)。

## アクションプラン

1. **[Major]** `android/kssettingsview/build.gradle.kts` に `api("androidx.compose.foundation:foundation-layout")` を足す。オーナー確認のうえ `deviation.md` へ recyclerview と同型の記録を追加し、`publishToMavenLocal` を再実行して `evidence/publish-to-maven-local.txt` の POM / `.module` 依存スコープ節を取り直す (aar の内容は変わらないため IntegrationHost の再確認は不要)
2. **[Minor]** `.agents/skills/docs-refresh/SKILL.md:180` のパスを `android/kssettingsview/build.gradle.kts` に更新する
3. **[Suggestion]** handbook 2 本の `timestamp` を揃える
4. **[Suggestion]** 蒸留への申し送り: android/ADR-0016 の accepted 昇格と、concepts の旧 module 名の追随対象一覧
5. **[Suggestion]** Explicit API mode の導入可否を別 change として検討する

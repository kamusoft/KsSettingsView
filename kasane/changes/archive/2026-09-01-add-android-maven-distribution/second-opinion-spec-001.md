# セカンドオピニオン: add-android-maven-distribution (spec-001)
**相方**: codex / **label**: so-spec-add-android-maven-distribution / **日付**: 2026-09-01 / **対象**: 提案一式 (proposal.md / design.md / specs/ / tasks.md)
---
# レビュー結果: add-android-maven-distribution

**日付**: 2026-09-01  
**判定**: **NEEDS_DISCUSSION**  
**指摘件数**: Critical 0 / Major 8 / Minor 2 / Suggestion 0

## サマリー

module 統合の方向性自体は具体的ですが、未承認 ADR の扱い、accepted ADR の直接改変、公開依存スコープ、検証範囲に実装前に解消すべき問題があります。特に、現行設定を単純に統合するとテスト専用依存が公開物へ混入し、逆に公開 API が要求する依存が利用者の compile classpath に出ない可能性があります。

本レビューは指定どおり静的に実施し、ビルド・テスト・ファイル書き込みは行っていません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/public-identifiers.md`（公開 GAV・namespace・Gradle・csproj）
- `kasane/handbook/cross/test-execution.md`（Android テスト実行・件数確認）
- `kasane/handbook/maui/integration-host-verification.md`（binding の end-to-end 検証）
- ksn-core の domain-axis、ADR の accepted 後不変規則、デルタスペック形式
- `kasane/lessons/code-review.md`（今回、ミューテーション確認は静的レビューのため対象外）

## 指摘事項

### [🟠 Major] 複数ドメイン変更なのに `domain: android` になっている

**該当箇所**: `proposal.md:14`、`proposal.md:16`、`proposal.md:39`  
**問題点**: MAUI binding と cross handbook/ADR を変更するため、domain-axis 規約上は褗数ドメインに該当する `cross` 変更です。`android` のままだと MAUI 側スキル・handbook の解決や、蒸留先の判断が欠落します。  
**推奨修正**: `domain: cross` とし、実際に触る android / maui のスキルオーバーレイと両ドメインの長命層を解決対象にしてください。

### [🟠 Major] `proposed` ADR を「決定済み」として実装の前提にしている

**該当箇所**: `proposal.md:5`、`kasane/decisions/android/0016-single-module-single-maven-artifact.md:4`  
**問題点**: android/ADR-0016 は現在 `status: proposed` です。Kasane では proposed は AI ドラフトであり、人間の確認後に accepted へ昇格して初めて確定判断になります。現行 handbook もまだ `ks-settingsview-*` を規範としています。  
**推奨修正**: 実装承認前に ADR-0016 をオーナー確認して accepted にするか、本提案内で未確定設計として改めて合意対象にしてください。

### [🟠 Major] accepted ADR-0018 の本文を直接書き換える計画になっている

**該当箇所**: `proposal.md:16`、`tasks.md:33`、`kasane/decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md:4`  
**問題点**: accepted ADR は不変で、変更時は新 ADR による supersede が必要です。「表記追随」であっても、Decision 表の Maven 座標を書き換えるのは履歴の改変です。  
**推奨修正**: ADR-0018 本文の編集タスクを削除してください。ADR-0016 の accepted 化と index の注記で表現するか、cross の判断自体を更新する必要があるなら新 ADR で supersede してください。

### [🟠 Major] `ui-test-manifest` が公開 release の推移依存へ混入する

**該当箇所**: `tasks.md:6`、`design.md:37`、`android/ks-settingsview-compose/build.gradle.kts:81`  
**問題点**: 現行 compose module は release unit test 用に `androidx.compose.ui:ui-test-manifest` を `releaseImplementation` へ入れています。依存の和集合を作って release variant を公開すると、このテスト専用ライブラリが publication の runtime dependency に載ります。現在の spec は依存の存在だけを確認し、テスト依存の不在を確認しません。  
**推奨修正**: release unit-test 専用 configuration へ移すなど、本番 release variant から除外してください。POM と Gradle Module Metadataについて、JUnit、Robolectric、AndroidX Test、`ui-test-manifest` が含まれない Scenario を追加してください。

### [🟠 Major] 公開 API と Maven 依存スコープの設計が欠けている

**該当箇所**: `specs/android-maven-distribution/spec.md:39`、`android/ks-settingsview-core/build.gradle.kts:72`、`android/ks-settingsview-ui/build.gradle.kts:97`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:83`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:40`  
**問題点**: `Theme` は Compose の `Color` / `TextStyle` / `Dp`、`SettingsRootStore` は `StateFlow`、`KsAnyView.Compose` は `@Composable` を公開 API に露出しています。一方、該当依存はほぼ `implementation` で、spec もPOMの「実行時依存」だけを要求しています。単純統合すると、clean な利用者の compile classpath に必要型が届かず、「依存1行」の利用コードがコンパイルできない可能性があります。  
**推奨修正**: 公開 ABI に現れる外部型を列挙して `api` / `implementation` を設計し直してください。POMだけでなく `.module` の API/runtime dependency scopes を検査し、最小 consumer のコンパイル確認を本変更へ入れるか、少なくともこのリスクと phase-7 への明確な申し送りを design に残してください。

### [🟠 Major] Android の完了テストから bridge module が漏れている

**該当箇所**: `specs/android-maven-distribution/spec.md:21`、`tasks.md:14`、`design.md:74`  
**問題点**: spec/tasks は `:kssettingsview:test` と旧3 moduleの件数だけを対象としています。しかし bridge も改名され、project 依存先が変更されます。`assembleRelease` が通っても bridge のテストがコンパイル不能・未実行でも受け入れ条件を満たします。また design の「全テスト」とも一致しません。  
**推奨修正**: handbook 規約どおり `android/` の `./gradlew test --rerun-tasks` を完了条件にし、統合 module と bridge の debug/release 各結果を件数付きで確認する Scenario/task にしてください。

### [🟠 Major] MAUI binding の検証が build 成功だけで、実際の同梱・実行を保証しない

**該当箇所**: `specs/android-maven-distribution/spec.md:57`、`specs/android-maven-distribution/spec.md:61`、`tasks.md:27`  
**問題点**: 3 AARから2 AARへの変更は、binding assemblyへの同梱、resource merge、推移依存、Bridgeから本体クラスをロードできることに影響します。binding project 単体の build はC#からNative表示までの疎通を保証しません。既存 handbook はこのため Android IntegrationHost を回帰資産として定めています。  
**推奨修正**: `KsSettingsView.IntegrationHost.Android` の build/runと固定シナリオの成立をScenarioに追加してください。後続 phase に送る場合は、このRequirementを「binding生成まで」に狭め、runtime成立を保証しないことを明記してください。

### [🟠 Major] Central・署名・SNAPSHOT方針がデルタスペックから抜けている

**該当箇所**: `proposal.md:12`、`design.md:35`、`specs/android-maven-distribution/spec.md:31`  
**問題点**: proposal/design は Central Portal配線、`signAllPublications()`、SNAPSHOT非発行を変更内容としていますが、Scenarioは `publishToMavenLocal` だけです。Central設定や署名が欠落してもspec上は合格し、SNAPSHOT非発行が「運用方針」なのか「タスクを拒否する実装保証」なのかも決まっていません。  
**推奨修正**: 次のどちらかを明示的に選んでください。

- 本変更で扱うなら、Central向けtask配線・署名生成・SNAPSHOT時の挙動を、外部uploadなしで判定できるScenarioにする。
- phase-8へ送るなら、proposal/designから本変更の保証であるかのような記述を外し、ローカルpublication生成だけにスコープを狭める。

### [🟡 Minor] Sample の「無音フォールバック禁止」に負の Scenario がない

**該当箇所**: `specs/android-maven-distribution/spec.md:49`、`specs/android-maven-distribution/spec.md:51`、`tasks.md:25`  
**問題点**: Requirement は置換先が存在しない場合の失敗を要求しますが、Scenario/task は成功経路しか確認しません。明示 substitution が誤って削除されても、公開版またはローカル版へ解決できれば検出できません。  
**推奨修正**: 置換先を解決不能にした隔離fixtureで Sample build が外部artifact解決へ進まず失敗する負のScenarioを追加してください。

### [🟡 Minor] sources/javadoc jar は「存在」しか検証されない

**該当箇所**: `design.md:37`、`specs/android-maven-distribution/spec.md:39`、`tasks.md:20`  
**問題点**: sources jar が空または一部package欠落でも合格できます。また design は javadoc jarを意図的に空としていますが、specは空であることを定めていません。  
**推奨修正**: sources jarに `.core` / `.ui` / `.compose` の代表ソースまたは全ソースinventoryが含まれること、javadoc jarは意図どおり空であることを受け入れ基準へ追加してください。

## アクションプラン

1. `domain: cross` への修正とADR-0016の正式な承認を先に行う。
2. accepted ADR-0018の直接編集を取り下げ、履歴を保つ表現方法を決める。
3. 公開依存の `api` / `implementation`、テスト依存除外、Central・署名・SNAPSHOTの保証範囲を設計として確定する。
4. root Androidテスト、MAUI IntegrationHost、Sample負系、publication内容検査をScenarioとtasksへ反映する。
5. 修正版を再度スペックレビューへ回す。


## 突き合わせ結果

ホスト側自己レビュー (2 周、指摘なし) との突き合わせ。相方のみの指摘のため根拠の強さで採否を判定した。

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1 | domain: android → cross | **採用** | MAUI binding と cross handbook / ADR を触るため domain-axis 規約の「複数該当は cross」に合致 |
| 2 | proposed ADR-0016 を前提にしている | **却下** | roadmap.md が「設計の正は proposed ADR 群、accepted 昇格は蒸留時」とオーナー承認済みの運用として明記。運用どおり |
| 3 | accepted ADR-0018 の直接書き換え | **採用 (修正形)** | ADR-0018 は accepted と実測確認。表のセル書き換えはせず、同 ADR の既存慣行 (2026-08-29 追記) に倣う日付付き追記に変更 |
| 4 | ui-test-manifest が release 公開物に混入 | **採用** | compose の `releaseImplementation("androidx.compose.ui:ui-test-manifest")` を実測確認。実害シナリオ具体的 |
| 5 | 公開 API の外部型と api/implementation スコープ設計欠落 | **採用** | Theme (Color/TextStyle/Dp)・KsAnyView.Compose (@Composable)・SettingsRootStore (StateFlow) の公開 ABI 露出は事実。単純統合では利用者の compile classpath に必要型が届かない |
| 6 | bridge のテストが完了条件から漏れ | **採用** | bridge に JUnit 4 テストが実在。handbook cross/test-execution.md の件数確認規約とも整合させる |
| 6b | MAUI binding の検証が build 成功だけ | **採用** | aar 構成変更 (3→2) は同梱・実行に影響し、handbook maui/integration-host-verification.md が IntegrationHost を binding 変更の回帰資産と定めている。IntegrationHost のビルド・実行確認を Scenario / task に追加 |
| 7 | Central 配線・署名・SNAPSHOT の保証範囲が曖昧 | **採用 (スコープ明確化)** | 提示 2 案のうち「設定は本変更・実効検証は phase-7/8」を選択し proposal / design に明記。spec はローカル publication 生成に限定 |
| 8 | Sample の無音フォールバック禁止に負系 Scenario がない | **採用 (修正形)** | 隔離 fixture は過剰と判断し降格。代わりにテスト不能な SHALL 節を Requirement から削除して検証可能性を回復 (根拠は design Decision 4 に既存) |
| 9 | sources jar / javadoc jar の内容検証がない | **採用** | 受け入れ基準の強化として安価。sources に 3 パッケージ含有・javadoc は空を明記 |

採用 8 (うち修正形 3) / 却下 1 / 降格 1 (負系 fixture 案 → SHALL 節削除で代替)。#2 の却下はオーナー承認済み運用との衝突が根拠 (オーナーの的外れ判定ではないため lesson 捕捉対象外)。

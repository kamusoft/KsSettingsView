# セカンドオピニオン: add-verification-ci (spec-001)
**相方**: codex / **label**: so-spec-add-verification-ci / **日付**: 2026-08-31 / **対象**: 提案一式 (proposal.md / specs/verification-ci/spec.md / tasks.md)
---
# レビュー結果: add-verification-ci

**日付**: 2026-08-31  
**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 0 / Major 6 / Minor 2 / Suggestion 1

## サマリー

フェーズの主要な決定事項はおおむね反映されていますが、Android の0件検出、ツールチェーン固定、branch protection、lint負ケースに検証上の穴があります。また、外部状態を変更する横断CIをM級とする妥当性にも再確認が必要です。このまま実装へ進むと、要求を満たしていないCIが緑になる可能性があります。

## 照合した規約・資料

- `kasane/handbook/cross/test-execution.md`
- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/local-development-setup.md`
- `kasane/handbook/maui/integration-host-verification.md`
- `kasane/concepts/cross/architecture/repository-boundaries.md`
- `kasane/concepts/android/architecture/build-toolchain.md`
- `kasane/roadmaps/package-distribution/phases/phase-3-verification-ci/agenda.md`
- `ksn-core` の変更級・デルタスペック・domain規約
- `github-workflow-skill`

## 指摘事項

### [🟠 Major] M級判定がL級条件と整合していない

**該当箇所**: `proposal.md:38`

**問題点**: 変更は3 platformを横断し、GitHub Actionsとbranch protectionという外部サービス状態を変更します。これは `ksn-core` のL級条件である「複数能力横断」「外部連携」の少なくとも後者に該当し得ます。M級の根拠は「verification-ciという1能力」としていますが、能力名を一つにまとめただけでは、外部設定操作や複数ビルド系統の設計リスクは消えません。現状はdesign.mdがなく、check context、権限、外部設定の適用・検証・復旧方法が未設計です。

**推奨修正**: L級へ変更し、workflow構成、再利用境界、権限、action固定方針、必須check名、branch protectionの適用・事後確認をdesign.mdに記載してください。M級を維持する場合は、GitHub外部状態の変更をL級条件から除外できる理由を明示して合意してください。

### [🟠 Major] AndroidのXML globでは「存在しないmodule×variant」を検出できない

**該当箇所**: `specs/verification-ci/spec.md:35`, `tasks.md:12`, `tasks.md:24`

**問題点**: `*/build/test-results/*/TEST-*.xml` の列挙から分かるのは「存在するXML」の件数だけです。あるmodule×variantのタスクが実行されずディレクトリやXML自体が生成されなかった場合、その組み合わせは集計対象から消え、0件として認識できません。現行構成は4 module × debug/releaseですが、期待する8組を別途導出しない限りScenarioを保証できません。

**推奨修正**: Gradle構成から期待するmodule×variant集合を導出するか、期待する8組を明示した検査表を用意し、各組について「XMLなし」「tests合計0」の両方を失敗させてください。負ケースは単なるXMLファイル1枚の削除ではなく、期待する1組の結果ディレクトリ全体が欠けても失敗することを確認してください。

### [🟠 Major] ツールチェーン固定の契約が決定事項と矛盾し、MAUI側のXcode選択も欠けている

**該当箇所**: `specs/verification-ci/spec.md:68`, `specs/verification-ci/spec.md:74`, `tasks.md:10`, `tasks.md:13`, `kasane/roadmaps/package-distribution/phases/phase-3-verification-ci/agenda.md:13`

**問題点**: specは「それ以外の経路で版が変わることはない」と要求しますが、決定事項ではXcodeのパッチ版をrunner image同梱最新版へ委ねています。したがって、workflow/global.jsonのdiffなしで実際のXcode版が変わり得ます。またMAUI jobもiOS binding経由でXcodeを使用しますが、task 2.4にはXcode 26.5の明示選択がありません。

**推奨修正**: 固定境界を「runner major、Xcode major.minor、JDK major、.NET SDK/workload set」などと正確に定義し、許容するパッチ変動をScenarioから除外してください。Xcode固定をMAUIにも適用するなら、iOS jobと同じ選択処理または `DEVELOPER_DIR` の設定をtask 2.4へ追加してください。

### [🟠 Major] MAUIテストの実行件数確認が受け入れ作業から欠落している

**該当箇所**: `specs/verification-ci/spec.md:48`, `specs/verification-ci/spec.md:53`, `tasks.md:22`, `tasks.md:25`, `kasane/handbook/cross/test-execution.md:15`

**問題点**: handbookはplatformを問わず「実行件数の確認までが検証」と定めています。iOSにはtask 4.2、Androidには自動集計がありますが、MAUIはjob成功だけで、`Total > 0` や実行件数を確認するtaskがありません。テスト探索が壊れて0件になっても、要求する「全件実行」を満たしたと誤判定する余地があります。

**推奨修正**: MAUIについて実行件数と失敗件数を確認・記録するtaskを追加してください。CIの恒常的な保証とするならTRX等を解析し、合計0件をjob失敗にしてください。

### [🟠 Major] branch protectionのScenarioと必須check契約が実装・検証タスクに落ちていない

**該当箇所**: `specs/verification-ci/spec.md:76`, `specs/verification-ci/spec.md:84`, `tasks.md:30`, `tasks.md:31`

**問題点**: 「直pushの拒否」Scenarioに対応する検証taskがありません。またbranch protection APIへ登録する正確なstatus check context名が未定義です。reusable workflowでは呼び出しjobと内部jobの表示名が分かれるため、「4 job」だけでは登録対象を一意に決められず、名称変更で永続的なExpected状態になるリスクがあります。

**推奨修正**: 4つの安定したcheck context名を設計で固定してください。設定後に両ブランチの保護設定をAPIで再取得し、必須check、PR必須、force-push禁止、削除禁止を検査するtaskを追加してください。非バイパス主体による直push拒否、または同等の設定検査もScenario対応として追加してください。

### [🟠 Major] lintの負ケースが4検査のうち実質1種類しか担保しない

**該当箇所**: `specs/verification-ci/spec.md:55`, `specs/verification-ci/spec.md:58`, `tasks.md:18`, `tasks.md:25`

**問題点**: task 4.4はsamples配下の識別子違反を主に確認する内容で、identity-lintの配線しか実証できません。gitleaks、local-path-lint、comment-policy-lintが実行されていても検出不能な設定になっているケースを見逃します。

**推奨修正**: 4検査それぞれに無害な負ケースを用意し、各検査が非0終了し、該当箇所を出力することを個別に確認してください。gitleaksには実在しない検証用文字列を使用し、実秘密をcommitしないことも明記してください。

### [🟡 Minor] `workloadVersion` の追加は既に完了している

**該当箇所**: `proposal.md:19`, `tasks.md:5`, `global.json:4`

**問題点**: 現行の `global.json` は既に `"workloadVersion": "10.0.300.3"` を持っています。提案された変更とtask 1.1は現行ツリーに対して差分を生成しません。「現行版へ変更する」意図なら変更前後の値も定義されていません。

**推奨修正**: 既存設定を前提条件として記載し、What Changesとtask 1.1から削除してください。版更新が目的なら、変更先の具体値と更新理由を明記してください。

### [🟡 Minor] `main` 向けPRトリガーのScenarioがない

**該当箇所**: `specs/verification-ci/spec.md:6`, `specs/verification-ci/spec.md:8`, `tasks.md:22`

**問題点**: Requirementは`develop`と`main`の両方を対象にしますが、Scenarioは`develop`だけで、draft PR確認のbase branchも明示されていません。`main`のbranches設定が漏れてもScenario対応表上は検出できません。

**推奨修正**: `main`をbaseとするPRのScenarioを追加するか、task 4.1を両base branchの確認として明示してください。

### [🔵 Suggestion] workflowの最小権限とaction固定方針を明文化する

**該当箇所**: `tasks.md:10`

**問題点**: pull_requestのコードからGradle・MSBuild・Swiftの処理を実行する一方、workflow権限と外部actionの参照方法が未指定です。特にgitleaksを含む外部actionの可変tag参照は、ツールチェーン変更をdiffに出すという方針とも相性がよくありません。

**推奨修正**: 原則として `permissions: contents: read` を明示し、外部actionはcommit SHAへ固定する方針をdesign/tasksへ追加してください。

## アクションプラン

1. 変更級をLへ上げるか、Mを維持する根拠を合意する。
2. ツールチェーンの固定境界とMAUIのXcode選択を確定する。
3. Androidの期待module×variant集合と0件判定方式を仕様化する。
4. MAUI件数、4 lint負ケース、branch protection、`main` PRの検証taskを補う。
5. 既に存在する `workloadVersion` 追加taskを整理する。

静的レビューのみ実施し、ファイル変更・ビルド・テスト実行は行っていません。


## 突き合わせ結果 (2026-08-31)

ホスト側自己レビュー (チェックリスト通過) との突き合わせ。採用 7 / 降格 0 / 未解決 1 (ユーザー裁定へ)。

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| Major: M級判定の妥当性 | 相方のみ | **解決: M 維持 (オーナー裁定 2026-08-31)** | 下記根拠を proposal の級理由に明記して対応 |
| Major: Android 期待 module×variant の導出 | 相方のみ・根拠強 | **採用** | spec の Requirement / Scenario を「期待集合を settings.gradle.kts から導出、XML 欠落でも fail」に修正。tasks 2.3 / 4.3 更新 |
| Major: ツールチェーン固定境界の矛盾・MAUI の Xcode 選択欠落 | 相方のみ・根拠強 | **採用** | spec に固定境界 (Xcode はメジャー.マイナー、パッチ変動許容) を定義し ios / maui 両 job へ適用。tasks 2.1 / 2.4 更新 |
| Major: MAUI 実行件数の検査欠落 | 相方のみ・根拠強 (handbook 明記) | **採用** | spec に「合計 0 件で fail + summary 表示」を追加。tasks 2.5 / 4.4 新設 |
| Major: check context 名の固定と保護設定の事後検証 | 相方のみ・根拠強 (reusable workflow の context 名問題は実害) | **採用** | tasks 3.1 (context 名固定)・5.2 (API 再取得検査)・5.3 (直 push 拒否確認) 更新 |
| Major: lint 負ケースが 1 検査のみ | 相方のみ・根拠強 | **採用** | task 4.5 を 4 検査それぞれの負ケースに拡張 (gitleaks はダミー文字列) |
| Minor: workloadVersion は既存 | 相方のみ・事実確認済み | **採用** | global.json に 10.0.300.3 が既存であることを確認。proposal / tasks から変更を削除し前提条件に変更 |
| Minor: main 向け PR の Scenario 欠落 | 相方のみ | **採用** | Scenario「main への PR でも起動する」を追加。task 4.1 で確認方法を明示 |
| Suggestion: permissions 最小化・action SHA 固定 | 相方のみ | **採用** | task 3.3 新設 |

M 維持のホスト側根拠: 設計判断はフェーズ議論 (agenda 決定事項 10 件) で決着済みで design.md は複写になる。L 条件の「外部連携」はプロダクトが外部サービスと連携する変更を指し、branch protection は `gh api` 1 回で可逆な運用設定。相方の懸念の実体 (check 名・適用検証・権限・固定境界) は本反映で spec / tasks に落ちた。

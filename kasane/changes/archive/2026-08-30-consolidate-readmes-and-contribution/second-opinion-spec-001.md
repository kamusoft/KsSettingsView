# セカンドオピニオン: consolidate-readmes-and-contribution (spec-001)
**相方**: codex / **label**: so-spec-consolidate-readmes-and-contribution / **日付**: 2026-08-29 / **対象**: 提案一式 (proposal.md / specs/repository-docs/spec.md / specs/docs-refresh/spec.md / tasks.md / ui/brief.md)
---
# レビュー結果: consolidate-readmes-and-contribution

**判定**: NEEDS_DISCUSSION  
**指摘件数**: Critical 0 / Major 7 / Minor 3 / Suggestion 0

## サマリー

中心方針は ADR と概ね整合していますが、現状の spec にはそのままでは達成不能な Scenario、ADR から移送対象が抜ける箇所、配布座標の正が競合する箇所があります。また、複数 capability と長命ポリシーを横断するため、M 級という判定も Kasane 規約と一致しません。

## 指摘事項

### [🟠 Major] README の全体列挙と参照ゼロの Scenario は達成不能

**該当箇所**: `specs/repository-docs/spec.md:7`、`specs/repository-docs/spec.md:15`、`specs/repository-docs/spec.md:20`、`tasks.md:61`、`tasks.md:63`

**問題点**: 現在、リポジトリ全体には `README*.md` が13枚あります。廃止対象5枚を削除して `README_ja.md` を追加しても、archive 内の検証用 README 4枚が残るため9枚です。

さらに、廃止対象パスは proposal・spec・tasks・ADR・archive 内の履歴資料自身にも記載されています。そのため「リポジトリ内の Markdown から検索して該当なし」は、この change を含めた時点で成立しません。過去のレビューには削除対象 README への Markdown リンクも残っています。

**推奨修正**: 対象を「公開ドキュメント面」など明示的な範囲へ狭めてください。例えばルート、`skills/`、`android/`、`maui/`、`samples/` の非 archive 部分だけを列挙対象とし、`kasane/changes/archive/`、`kasane/decisions/`、当該 change 自身、凍結済み `openspec/` を除外します。リンク検査も「現行公開文書から削除対象への解決可能な Markdown リンク」に限定する必要があります。

### [🟠 Major] ADR-0023 が移送を要求する手順の一部が proposal/tasks から欠落している

**該当箇所**: `proposal.md:20`、`tasks.md:7`、`tasks.md:10`、`tasks.md:11`、`tasks.md:12`、`kasane/decisions/cross/0023-readme-root-only-and-developer-knowledge-in-concepts.md:28`

**問題点**: ADR-0023 は次も `kasane/concepts/` へ移すと決定しています。

- Sample の実行手順とデモ画面一覧
- 本体ライブラリへステップインする手順
- `DEVELOPER_DIR` を含む環境手順

tasks には環境設定・目視確認・MAUI検証ホストはありますが、Sample 実行手順、デモ画面一覧、ステップイン手順の明示的な移送タスクがありません。Task 1.1 の分類表だけでは、後続タスクがないため廃止時に失われる可能性があります。

**推奨修正**: それぞれの移送先ファイル、Requirement、到達可能性を検証する Scenario とタスクを追加してください。不要と判断するなら、ADR-0023 の改訂が先に必要です。

### [🟠 Major] 「各 SKILL.md と同一の最小コード例」は定義上成立しない

**該当箇所**: `specs/repository-docs/spec.md:28`、`specs/repository-docs/spec.md:42`、`tasks.md:30`

**問題点**: iOS、Android、MAUI の最小例はそれぞれ Swift、Kotlin、XAML であり、相互に同一にはできません。また「各 `SKILL.md`」には AiForms 移行 Skill も含むように読めます。どのコードブロック同士を比較するか決まっていないため、機械検査も一意に書けません。

**推奨修正**: 「3 platform ごとに1例を置き、対応する en/ja の platform Skill の最小動作コードブロックと byte 単位で一致する」と定義してください。AiForms 移行 Skillを対象に含めるかも明記してください。

### [🟠 Major] docs-refresh の変更方針が proposal・spec・現行プロンプトで矛盾している

**該当箇所**: `proposal.md:22`、`specs/docs-refresh/spec.md:5`、`specs/docs-refresh/spec.md:9`、`specs/repository-docs/spec.md:28`、`tasks.md:50`、`tasks.md:52`、`.agents/skills/docs-refresh/SKILL.md:34`、`.agents/skills/docs-refresh/SKILL.md:183`、`.agents/skills/docs-refresh/SKILL.md:373`

**問題点**:

- proposal は「モジュール一覧チェックは残し、platform README を突合先から外す」と読めます。
- docs-refresh spec はモジュール一覧チェック自体を廃止するとしています。
- repository-docs spec はルート README にモジュール一覧を置くことを禁止しています。
- 現行 docs-refresh の README 更新プロンプトは、ルート README のモジュール表を確認するよう明示しています。

Task 6.2で機械チェックだけを消しても、更新プロンプトが残れば、将来の `docs-refresh` がモジュール表を再導入する可能性があります。

**推奨修正**: モジュール一覧チェックを全面廃止する方針へ統一し、追従対象表、Step 3d、Step 4 の例、README 委譲プロンプト、整合性チェック、完了サマリの全箇所を更新対象として列挙してください。「platform/Sample README やモジュール表への旧指示が残らない」Scenarioも追加すべきです。

### [🟠 Major] 配布座標の正が競合したまま、禁止事項だけを先行削除しようとしている

**該当箇所**: `proposal.md:25`、`proposal.md:32`、`specs/repository-docs/spec.md:150`、`tasks.md:15`、`tasks.md:29`、`kasane/decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md:25`、`kasane/decisions/android/0016-single-module-single-maven-artifact.md:21`

**問題点**: Android 座標について、cross/ADR-0018 は `jp.kamusoft:ks-settingsview-*`、android/ADR-0016 は `jp.kamusoft:kssettingsview` としており、前者の追随が未完了です。spec の「確定済み ADR の値」だけでは優先順位が決まりません。

一方、artifactId 規則の改訂をNon-Goalとして後回しにしながら、`public-identifiers.md` の「未公開物を利用可能と説明しない」という安全規則だけを削除します。結果として concepts は旧座標を正として残しつつ、README は新座標を利用可能な形で示す矛盾状態になります。このポリシー削除自体も明示的な Requirement / Scenario や検証タスクを持っていません。

**推奨修正**: 次のどちらかを明示的に選んでください。

- 本変更で ADR・`public-identifiers.md`・README・Skills の配布座標を完全に整合させる。
- 識別子ポリシーの変更をphase-5へ残し、本変更では冒頭バナー配下の将来座標という限定的な例外として扱う。

禁止事項だけを削除する中間状態は避けるべきです。

### [🟠 Major] M級判定が Kasane の変更級基準と一致しない

**該当箇所**: `proposal.md:27`、`proposal.md:47`

**問題点**: proposal 自身が `repository-docs` と `docs-refresh` の2 capabilityを影響範囲として挙げています。さらに、Contribution 方針、Issue Forms、長命 concepts ポリシー、スクリーンショット資産も横断します。ksn-core は複数能力横断をL級と定義しており、「コード変更なし」はM級へ下げる理由になりません。

**推奨修正**: L級へ変更して `design.md` を追加するか、少なくとも「README・Contribution」「docs-refresh」「配布識別子ポリシー」を独立した変更へ分割してください。

### [🟠 Major] Issue Forms の検証が YAML 構文しか保証しない

**該当箇所**: `specs/repository-docs/spec.md:134`、`specs/repository-docs/spec.md:138`、`tasks.md:42`、`tasks.md:43`、`tasks.md:44`、`tasks.md:67`

**問題点**: Task 8.7 はGitHub Issue Formsのスキーマ妥当性しか確認しません。構文上有効でも、各項目の `required: true`、日英投稿可の案内、blank issue 無効化が欠けたまま通ります。バグフォームの Scenario も再現手順1項目しか検査していません。

**推奨修正**: 静的検査で次を確認するタスクを追加してください。

- バグ5項目、提案3項目がすべて存在し必須である
- 英語ラベルと日英投稿可の案内がある
- `blank_issues_enabled: false`
- CONTRIBUTING にPR非受付の理由とIssue作成方法がある
- 英日 CONTRIBUTING の相互リンクが解決する

### [🟡 Minor] スクリーンショットの受け入れ検査が要求の一部しか覆わない

**該当箇所**: `specs/repository-docs/spec.md:54`、`tasks.md:20`、`tasks.md:65`、`ui/brief.md:28`、`ui/brief.md:32`

**問題点**: 検証は端末固有情報の目視確認だけです。4画像のplatform/style対応、英日でのパス共有、2×2配置、キャプション言語、MAUI補足、同一画面・同一スクロール位置は検査されません。承認欄のコメント例も単一ファイル前提です。

**推奨修正**: 4画像の採用表を承認欄に設け、各組み合わせ、参照パス、キャプション、配置、MAUI補足を確認するタスクを追加してください。

### [🟡 Minor] concepts 更新で log と具体的な配置先が定義されていない

**該当箇所**: `tasks.md:8`、`tasks.md:10`、`tasks.md:11`、`tasks.md:12`、`tasks.md:13`

**問題点**: `kasane/concepts/maui/` のようなディレクトリ単位しか指定されておらず、既存ファイルへの統合か新規concept作成かが実装者判断になっています。また index 更新はありますが、Kasane concepts規約が要求する `kasane/concepts/log.md` 更新がありません。

**推奨修正**: 各情報の移送先ファイルを決め、必要な index・timestamp・log 更新をタスク化してください。

### [🟡 Minor] prerelease の取得方法が platform ごとに定義されていない

**該当箇所**: `specs/repository-docs/spec.md:28`、`specs/repository-docs/spec.md:36`、`tasks.md:29`

**問題点**: SwiftPM、Maven、NuGetではprerelease指定の方法が異なりますが、「prerelease の取得方法」としか決まっていません。どのタグ・バージョン構文・コマンドを示せば合格か検証できません。

**推奨修正**: ecosystemごとの記載形式と、参照するバージョン源をScenarioまたはタスクに明記してください。

## アクションプラン

1. README列挙・参照検索の対象範囲を定義し直す。
2. 変更級をLへ上げるか、配布識別子ポリシーを含めて変更を分割する。
3. Android座標の正と `public-identifiers.md` の扱いを決定する。
4. ADR-0023の移送対象、platform別最小コード例、docs-refreshの全面的な追随箇所をspec/tasksへ補う。
5. Issue Forms・スクリーンショット・concepts更新の静的受け入れ検査を追加する。

指定どおり静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

---

## 突き合わせ結果 (2026-08-29)

ホスト側の自己レビュー (ksn-propose Step 8、2 周) の通過状況と突き合わせた。ホスト側は「全 Requirement に Scenario あり / UI lint クリア / tasks が全 Requirement をカバー」を確認済みで、上位層違反として `public-identifiers.md` の禁止事項との緊張 1 件を検出しオーナー判断で解消していた。

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| Major 1 | README 列挙と参照ゼロの Scenario が達成不能 | **採用** | 事実確認済み。`README*.md` は実際 13 枚 (`kasane/changes/archive/*/verification/` 等に 4 枚)。廃止後も 9 枚残る。参照検索も本 change・ADR・archive 自身が該当するため成立しない。ホスト側の見逃し |
| Major 2 | ADR-0023 の移送対象の一部が tasks から欠落 | **採用** | agenda の移送表 C 分類にある「サンプルの実行手順・デモ画面一覧・本体へのステップイン手順」に対応するタスクがない。廃止時に失われる |
| Major 3 | 「各 SKILL.md と同一の最小コード例」が定義上成立しない | **採用** | platform ごとに言語が異なるため「同一」は platform 対応でしか成立しない。AiForms 移行 Skill の扱いも未定義 |
| Major 4 | docs-refresh の変更方針が proposal・spec・現行プロンプトで矛盾 | **採用** | ホスト側も spec 作成時に「①モジュール一覧も対象消滅」と気づいていたが proposal へ反映していなかった。加えて README 委譲プロンプト (SKILL.md の 5b) がモジュール表確認を指示している点はホスト側の見逃し |
| Major 5 | 配布座標の正が競合したまま禁止事項だけ先行削除 | **採用 (方針はオーナー判断)** | cross/ADR-0018 の配布先の表が旧規則 `ks-settingsview-*` のまま、android/ADR-0016 が新規則。spec の「確定済み ADR の値」では優先順位が決まらない。中間状態の指摘は正当 |
| Major 6 | M 級判定が変更級基準と一致しない | **オーナー判断へ** | 規約上「複数能力横断 = L」は相方の読みが正しい。ただし級は提案化の冒頭でホスト側が M / L の差 (design.md の有無) を提示しオーナーが M を選択済み。lessons に同型の境界観測 (`change-grade-underestimated-for-cross-capability-contract`: 決定済み・低リスクの横断はオーナー裁定で M があり得る) がある |
| Major 7 | Issue Forms の検証が YAML 構文しか保証しない | **採用** | 必須項目の存在・`blank_issues_enabled: false`・日英投稿可の案内・CONTRIBUTING の相互リンクが未検査 |
| Minor 1 | スクリーンショットの受け入れ検査が不足 | **採用** | Scenario はあるが検証タスクが端末固有情報の目視のみ。4 画像の platform/style 対応・パス共有・キャプション・MAUI 補足が未検査 |
| Minor 2 | concepts 更新で log と配置先が未定義 | **部分採用** | 移送先ファイルの具体化は採用。`kasane/concepts/log.md` の更新は ksn-distill (蒸留) の責務であり change の実装タスクではないため**降格** |
| Minor 3 | prerelease の取得方法が platform ごとに未定義 | **採用** | SwiftPM / Maven / NuGet で指定方法が異なり、受け入れ基準が一意に決まらない |

**集計**: 採用 8 件 (Major 6 / Minor 2) / 部分採用 1 件 / 降格 0 件 (Minor 2 の log 部分のみ) / オーナー判断へ 1 件 (Major 6)。

Major 6 (変更級) と Major 5 (配布座標ポリシーの解決方針) はオーナーの判断を仰ぐ。それ以外は提案へ反映する。

### Major 5 の最終判定 (2026-08-29 オーナー裁定)

**却下 (スコープ外)**。オーナー判断: 「そもそもこの判断はこのフェーズに含まれてない。ADR もまだ propose で確定じゃない。矛盾も何もない。配信系が終わった段階で確定すること。基本的に配信系は今は仮でよい。リリース前に直せばよい」。

cross/ADR-0018 と android/ADR-0016 はいずれも proposed であり、配布座標の値は配信フェーズ (phase-4〜8) で確定する。phase-9 の責務は文書間で値を食い違わせないことに限られる。相方の指摘 (および提案側が spec に書いた「確定済み ADR の値と一致する」という表現) は、未確定のものを確定扱いしてスコープ外の整合作業を要求していた。

対応: Requirement「配布座標の一貫性」を「配布座標の文書間の一致」へ改め、暫定値であることを前提にした契約に緩めた。`public-identifiers.md` の禁止事項 2 項目の削除 (tasks 1.12) は、本変更の README 記述と直接衝突していたため維持する。

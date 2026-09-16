# セカンドオピニオン: backport-release-workflow-hardening (spec-003)
**相方**: codex / **label**: so-spec-backport-release-workflow-hardening-3 / **日付**: 2026-09-11 / **対象**: kasane/changes/backport-release-workflow-hardening/ の proposal.md・specs/release-workflow/spec.md・tasks.md・exploration.md (利用者向け契約の分離後)
---
# レビュー結果: backport-release-workflow-hardening

**日付**: 2026-09-11  
**判定**: **CHANGES_REQUESTED**

## サマリー

現状分析と変更スコープは概ね現行実装に一致し、利用者向け契約の分離も保たれています。一方、時間予算の算定不成立と、主要な異常系に対する検証タスク不足があるため、実装開始前の修正が必要です。

指摘件数: Critical 0 / Major 3 / Minor 2 / Suggestion 1

ビルド・テストは依頼どおり実行していません。

## 照合した規約・決定

- `kasane/handbook/cross/release-procedure.md` — release workflow の再実行・リハーサル
- cross/ADR-0020 — publish の冪等性と取り消せる順序
- cross/ADR-0025 — 検証 CI の構成
- cross/ADR-0026 — CI が保証する範囲
- cross/ADR-0028 — develop push / main PR のトリガー分担
- `kasane/concepts/cross/architecture/release-pipeline.md`

## 指摘事項

### [🟠 Major] `timeout-minutes: 200` が記載された最悪ケースを収容しない

**該当箇所**: [specs/release-workflow/spec.md:11](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:11)、[tasks.md:7](kasane/changes/backport-release-workflow-hardening/tasks.md:7)

**問題点**: 算出式に「本体処理の予算 + 余裕」の具体値がなく、200分が条件を満たすことを判定できません。さらに現行実装の最悪分岐では、200分では不足する可能性が高いです。

- 待機: `30 × 2 + 90 = 150` 分
- 3回の最終照会超過: `5 × 3 = 15` 分
- これだけで165分
- 引き継いだ deployment が30分後に `FAILED` となる分岐では、待機外にも最大5分のHTTP要求が6回あります。再照会、dropの状態照会とDELETE、公開済み照会、releaseの状態照会とPOSTで合計最大30分
- 現行コメントが示す本体処理の実測11分を足すと206分
- 各待機ではdeadline直前のsleepもあり、最大約1.5分をさらに超過し得ます

根拠となる応答上限は [central-portal.sh:129](scripts/release/central-portal.sh:129)、該当分岐は [release.yml:458](.github/workflows/release.yml:458) 以降です。

**推奨修正**: 分岐別に全HTTP要求・poll間隔・ビルド処理・余裕の数値を表にし、その最大値からjob上限を決めてください。定数変更時に算定違反を検出する自己テストも追加すべきです。

### [🟠 Major] deployment ID 引き継ぎのScenarioを検証する手段がない

**該当箇所**: [specs/release-workflow/spec.md:64](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:64)、[tasks.md:19](kasane/changes/backport-release-workflow-hardening/tasks.md:19)

**問題点**: tasks 3.1～3.3はstep移動・ログ追加・目視確認だけで、次の契約を機械的に検証しません。

- 引き継ぎあり
- 初回で引き継ぎなし
- artifact読み込み自体の失敗
- 引き継ぎ読み込み後、別artifactの取得が失敗
- 各失敗経路が既存artifactを空で上書きしない

tasks 5.2のdry-runではpublish job自体がskipされるため、これらは実行されません（[release.yml:417](.github/workflows/release.yml:417)）。次回の本番リリースを最初の経路検証にすると、修正対象である再実行経路を事前に証明できません。

**推奨修正**: 引き継ぎ準備と書き戻し判定を自己テスト可能なスクリプトへ分離するか、workflow構造テストを追加してください。少なくとも上記5状態について、step順序、`ready` / `found` 相当の状態、書き戻し条件を検査するタスクが必要です。

### [🟠 Major] レジストリ待機の自己テスト範囲がRequirementより狭い

**該当箇所**: [specs/release-workflow/spec.md:33](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:33)、[tasks.md:25](kasane/changes/backport-release-workflow-hardening/tasks.md:25)

**問題点**: Requirementは以下を要求しています。

- 判定不能の3種別
- 判定不能でも待機を継続
- 4対象が異なる状態にある巡回
- 上限超過時の対象別最終状態
- 一時的な判定不能から回復した場合の成功

しかしtask 4.1が自己テスト対象として明記するのは「反映済み / 未反映 / 判定不能」の上位3分類だけです。通信失敗と5xxが同じ分類へ潰れたり、timeout出力が対象別になっていなくても、tasks 4.1・5.1を完了できてしまいます。

**推奨修正**: MavenとNuGetそれぞれについて、200・404・5xx・通信失敗・不正JSON・version有無を入力表として定義し、混在巡回、継続後の回復、timeout時の最終出力まで自己テストへ明記してください。

### [🟡 Minor] CI Scenarioが現行のブランチ別トリガーを正確に表していない

**該当箇所**: [specs/release-workflow/spec.md:82](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:82)

**問題点**: Scenarioは「待機スクリプトを変更したpull request」で検証CIが走ると読めますが、現行CIのPRトリガーはmain宛てだけです。日常開発の主経路はdevelopへの直接pushです（[ci.yml:16](.github/workflows/ci.yml:16)、cross/ADR-0028）。

**推奨修正**: 「developへのpush」および「developからmainへのPR」、または単純に「lint jobが実行されるとき」と書き、イベント契約を現行運用に合わせてください。

### [🟡 Minor] 「その巡回で観測した分類」と既存のsticky完了状態が両立していない

**該当箇所**: [specs/release-workflow/spec.md:47](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:47)

**問題点**: 現行スクリプトは一度反映済みになった対象を後続巡回では照会しません（[wait-for-registries.sh:101](scripts/release/wait-for-registries.sh:101)）。そのため「4対象それぞれについて、その巡回で観測した分類」は、再照会する解釈と、以前の反映済み状態を持ち越す解釈に分かれます。

**推奨修正**: 反映済みはterminalなsticky状態として後続巡回へ持ち越すのか、毎巡回再照会するのかをRequirementで決めてください。前者なら「その巡回時点で保持する分類」とするのが明確です。

### [🔵 Suggestion] exploration冒頭のスコープ要約が分割前のまま読める

**該当箇所**: [exploration.md:9](kasane/changes/backport-release-workflow-hardening/exploration.md:9)

**問題点**: 冒頭では#2・#3を含む4件が対象と読めますが、現在のスコープは後段の分割決定により#5・#6・#7と自己テスト接続です。履歴としては理解できますが、初見ではproposalとの不整合に見えます。

**推奨修正**: 「分割前の現状確認では」など、時点を明示してください。

## 現行実装との整合性

提案の主要な現状認識は確認できました。

- `wait-validated` と `wait-published` は現在同じ30分上限を共有している
- registry待機はMavenの200以外、およびNuGetの通信失敗・不正応答を未反映へ畳み込んでいる
- deployment IDの取得はAndroid/MAUI artifact取得より後ろにある
- `wait-for-registries.sh --selftest` は現時点で存在せず、CIにも接続されていない
- handbookとconceptsの更新を蒸留への申し送りにした構成は、現在の変更スコープと矛盾しない

## アクションプラン

1. 200分の算定を全HTTP要求込みでやり直す。
2. deployment引き継ぎの5状態に対する検証方法をtasksへ追加する。
3. registry分類の全Scenarioを自己テスト項目へ展開する。
4. CIイベントと巡回状態の文言を明確化する。
5. 修正後に提案レビューを再実施する。

## 突き合わせ結果

分割後のアーティファクトに対する初回レビュー。ホスト側の自己レビューでは新たな指摘なし。相方の指摘 6 件は**全件を採用**した (降格・未解決なし)。

| # | 指摘 | 採否 | 検証 |
|---|---|---|---|
| Major 1 | `timeout-minutes: 200` が記載の最悪ケースを収容しない | 採用 | 算出式の「本体処理の予算 + 余裕」に具体値を入れないまま 200 と書いており、検算していなかった。`FAILED` 分岐の待機外 HTTP 要求 (再照会・drop の状態照会と DELETE・公開済み照会・release の状態照会と POST) を数えると 150 + 15 + 30 + 11 = 206 分を超える |
| Major 2 | deployment ID 引き継ぎの Scenario を検証する手段がない | 採用 | tasks 3.x が step 移動とログ追加と目視確認だけで、`dry-run` では publish job が skip される (`release.yml:417`) ため 5 状態のいずれも実行されない |
| Major 3 | レジストリ待機の自己テスト範囲が Requirement より狭い | 採用 | tasks 4.1 が上位 3 分類しか明記しておらず、判定不能の 3 種別・混在巡回・回復・上限超過時の対象別出力を検査せずに完了できてしまう |
| Minor 1 | CI Scenario が現行のブランチ別トリガーを正確に表していない | 採用 | `ci.yml` の PR トリガーは `main` 宛てのみで、日常は `develop` への直接 push (cross/ADR-0028) |
| Minor 2 | 「その巡回で観測した分類」と既存の sticky 完了状態が両立していない | 採用 | `wait-for-registries.sh` は反映済みの対象を後続巡回で再照会しない (`maven_done` / `nuget_done` の配列) ため、Requirement の読み方が 2 通りに割れる |
| Suggestion | exploration 冒頭のスコープ要約が分割前のまま読める | 採用 | 冒頭の現状確認表は分割前の 7 件を扱っており、時点の明示が要る |

いずれも設計判断を要さず、提案側の修正で閉じる。

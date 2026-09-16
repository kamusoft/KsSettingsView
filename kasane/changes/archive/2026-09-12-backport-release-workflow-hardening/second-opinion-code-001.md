# セカンドオピニオン: backport-release-workflow-hardening (code-001)

**相方**: codex / **label**: so-code-backport-release-workflow-hardening / **日付**: 2026-09-11

**対象**: 作業ツリーの未コミット変更 — `.github/workflows/ci.yml` / `.github/workflows/release.yml` / `scripts/release/central-portal.sh` / `scripts/release/wait-for-registries.sh` / `scripts/release/deployment-handover.sh` (新規) / `scripts/release/check-time-budget.py` (新規) / `scripts/release/check-publish-step-order.py` (新規)

---

### [🟠 Major] 60分の job timeout が先に発火し、上限超過時の最終分類を出力できない

**該当箇所**: `scripts/release/wait-for-registries.sh:92`、`scripts/release/wait-for-registries.sh:297`、`scripts/release/wait-for-registries.sh:355`、`.github/workflows/release.yml:916`

**問題点**: 1照会の上限が300秒で、未反映の4対象を直列に照会するため、1巡に最大20分かかります。一方、45分の deadline は巡回全体の終了後にしか確認されません。例えば開始41分時点の巡回は最大61分まで戻らず、60分の job timeout によって強制終了されます。

この経路ではスクリプトの「上限45分」が守られず、spec が要求する「対象ごとの最後の分類」も出力されません。

**推奨修正**: 各照会の前に残り時間を計算し、`curl --max-time` を `min(300, deadlineまでの残り時間)` に制限してください。残り時間がなければ、保持済みの分類を出力してスクリプト自身が失敗する形にします。job timeout には checkout と終了処理の余裕を残し、この境界を検査するテストも追加してください。

### [🟠 Major] 不正な NuGet versions 要素を「未反映」と誤分類する

**該当箇所**: `scripts/release/wait-for-registries.sh:188`、`scripts/release/wait-for-registries.sh:193`

**問題点**: `versions` が配列であることだけを確認し、各要素を `str(v)` へ変換しています。そのため `{"versions":[null]}` や数値・objectを含む、NuGet API の想定スキーマとして解釈不能な応答が「未反映」になります。

これは「応答を解釈できない」を判定不能として残すという変更の中心契約に反し、レジストリ側の異常を従来どおり未反映へ畳み込みます。

**推奨修正**: 全要素が文字列であることを確認し、1件でも異なる型なら exit 2 にしてください。その後は文字列に対して直接小文字比較します。非文字列、混在配列を `unknown-parse` とする自己テストも追加してください。

### [🟡 Minor] 順序検査が download と inspect の前後関係を検査していない

**該当箇所**: `scripts/release/check-publish-step-order.py:103`

**問題点**: 現在の workflow は正しく `handover download → inspect → 他成果物download` の順ですが、検査は download と inspect がそれぞれ「他成果物より前」にあることしか確認していません。

`inspect → handover download → 他成果物download` に並べ替えても検査は成功します。この場合 inspect は引き継ぎなしと誤認し、コンソールには未検証の矢印順序まで表示されます。

**推奨修正**: `handover_download[0] < handover_inspect[0] < min(other_downloads)` を明示的に検査してください。inspect を download より前へ移した入力が失敗する正の破壊テストも加えると確実です。

## アクションプラン

1. レジストリ待機に全体 deadline を実際に強制する。
2. NuGet `versions` の要素型を厳密に検証する。
3. step順序検査に `download < inspect` を追加する。
4. 上記の境界・異常入力・破壊ケースを自己テストへ追加し、既存の検査一式を再確認する。

指定に従い、レビュー結果ファイルへの書き込みは行っていません。



## 突き合わせ結果

ホスト側レビュー (`review-001.md`、CHANGES_REQUESTED: Major 1 / Minor 1 / Suggestion 1) との突き合わせ。相方の指摘 3 件は**全件を採用**した (降格・未解決なし)。

| # | 相方の指摘 | 採否 | 検証 |
|---|---|---|---|
| Major 1 | 60 分の job timeout が先に発火し、上限超過時の最終分類を出力できない | **確定** (双方一致) | ホスト側 Major と同一経路。`HTTP_MAX_TIME_SECONDS=300` (`wait-for-registries.sh:92`)、deadline 判定は `poll_once` の後 (`:355`) のため最悪 2700 秒 + 4 対象 × 300 秒 = 65 分。`release.yml` の `wait-for-registries` job は `timeout-minutes: 60` で、job の打ち切りが先に来る。Requirement「公開レジストリへの反映待ち」の「上限に達して失敗するときは、対象ごとに保持している分類を出力に含める SHALL」が満たされない |
| Major 2 | 不正な NuGet `versions` 要素を「未反映」と誤分類する | **採用** (相方のみ・根拠強) | `wait-for-registries.sh:193` が `str(v).lower()` で変換するため `{"versions":[null]}` は `"none"` となり exit 1 (未反映) に落ちる。Requirement の「判定不能は、失敗の種別 (…/ 応答を解釈できない) まで区別する SHALL」に反し、レジストリ側の異常を従来どおり未反映へ畳み込む。ホスト側の見逃し |
| Minor 1 | 順序検査が download と inspect の前後関係を検査していない | **採用** (相方のみ・根拠強) | `check-publish-step-order.py:103-110` は両 step が `first_other` より前かのみを検査し、`handover_download[0] < handover_inspect[0]` を見ていない。`inspect → download → 他成果物` に入れ替えても通る。現行 workflow の順序は正しいため実害は潜在的だが、検査の目的 (順序の固定) を果たしていない。ホスト側の見逃し |

ホスト側のみの指摘 2 件 (自己テストの暴走が NG ではなく hang になる / `writeback` の `keep`・`store` が workflow から到達しない) は相方が触れていないが、根拠は実測 (bash 3.2 での AND-OR 配下の `set -e` 無視) と到達性の静的確認により裏付けられている。修正サイクルへ含める。

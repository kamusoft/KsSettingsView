# 検証結果: retarget-docs-refresh-to-skills (003 回目)

**日付**: 2026-08-26
**判定**: VALID

## 検証範囲と方法

修正サイクル 2 周目を経た作業ツリーを対象に、デルタスペック **12 Requirement / 20 Scenario** を全件突き合わせ直した (前回 verify-002 は Scenario 数を 19 と数えていたが、`grep -c '^#### Scenario'` は 20 件。R1 の 2 件目を Requirement 直下の補足と混同したものと見られる。今回は 20 件で全件走査した)。本 change はコード変更を伴わないため、対応表の「実装」列は `.agents/skills/docs-refresh/SKILL.md` の行番号、「テスト」列は当検証者がリポジトリ外の一時 fixture で実行した結果および本文照読を指す。

- デルタスペック: `specs/docs-refresh/spec.md` (ADDED Requirements 12 件 / Scenario 20 件)
- deviation.md: **あり** (1 件 — `--readme-only` × 3d③ の交差を「報告のみ」で確定。合意済みの差分として扱う)
- 逆流検査: `git status` 上 `M` は `.agents/skills/docs-refresh/SKILL.md` / `AGENTS.md` / `kasane/config.yaml` / `kasane/changes/.../tasks.md` のみ。proposal.md / specs/ に変更なし → **逆流なし**
- 前回 [verify-002.md](verify-002.md) の ❌ (R10「`--readme-only` の軽量チェック」) を重点再検証。あわせて改稿された Step 6 / Step 7 / Step 1 周辺を全件再走査

## 対応表

| Requirement / Scenario | 実装 | テスト (検証) | 状態 |
|---|---|---|---|
| **R1: manifest v3 に基づく差分検出** | `SKILL.md:121-143` | 本文照読 | ✅ 一致 |
| └ S: concept 変更の逆引き | `SKILL.md:133-141` (`targets` を値から逆引き → 言語ペア) | 本文照読 | ✅ 一致 |
| └ S: メインコンテキストは concepts 本文を読まない | `SKILL.md:45-51`, `SKILL.md:65`, `SKILL.md:233`, `SKILL.md:683` | 本文照読。委譲不能時にメインが代読するフォールバックを禁じる記述が 4 箇所にあり、判定タイミングが Step 1 に一本化された | ✅ 一致 |
| └ (README は逆引き対象外) | `SKILL.md:143` | 本文照読 | ✅ 一致 |
| **R2: manifest v3 スキーマの規範** | `SKILL.md:101-119` | spec:27-37 と `SKILL.md:103-113` の **byte 一致**を機械照合 (`True`) | ✅ 一致 |
| └ S: 削除された concept の整理 | 提示 `SKILL.md:149` / 予定 manifest での除去 `SKILL.md:383-386` / 書き出し `SKILL.md:660-661` | 予定 manifest 生成ブロックを `export` 削除後の形で実行し、7 キーの JSON を出力することを実測。`targets` / `excluded` からの除去に加え `concepts` からの除去規則が `SKILL.md:661` に追加された | ✅ 一致 |
| └ 不変条件 ①②③ | `SKILL.md:115-119` | spec:39 と 3 件とも一致 | ✅ 一致 |
| **R3: manifest 不在・非対応時の停止** | `SKILL.md:74-99` (不在 / parse 不能 / version≠3 / 必須キー欠落・型不正、フォールバック明示禁止) | 本文照読 | ✅ 一致 |
| └ S: manifest 不在での起動 | `SKILL.md:83-97` (案内文に「skills/・README 群・manifest のいずれも変更していません」) | 本文照読 | ✅ 一致 |
| **R4: concepts 網羅検査** | `SKILL.md:145-175` | fixture で未参照 concept を `UNCOVERED`、削除済みを `DELETED`、正例で `concepts coverage OK` (前サイクルで実測済み・スクリプト不変) | ✅ 一致 |
| └ S: 未参照・未除外 concept の検出 | `SKILL.md:147` + スクリプト `SKILL.md:153-175` | 同上。配置判断のユーザー提示は `SKILL.md:147`, `SKILL.md:201-203` | ✅ 一致 |
| **R5: 更新方針の承認ゲート** | `SKILL.md:189-224` | 本文照読 (`SKILL.md:224` に無変更保証・部分承認時のハッシュ非更新)。提示例は連番つきで部分承認の指定が成立 | ✅ 一致 |
| └ S: 承認前の無変更 | `SKILL.md:224` | 本文照読 | ✅ 一致 |
| **R6: Skill 単位の委譲と en/ja ペア生成** | `SKILL.md:226-233` (Skill 単位 / README 単位 / 最大 3 並列 / 委譲不能時は停止) | 本文照読 | ✅ 一致 |
| └ S: 同一 Skill 内の複数ファイル更新 | `SKILL.md:230` (「同じ Skill の複数ファイルを分割しない」) | 本文照読 | ✅ 一致 |
| └ S: 言語ペアの同時更新 | `SKILL.md:249`, `SKILL.md:276-279`, `SKILL.md:688`。README 側は `SKILL.md:300-301`, `SKILL.md:331-333` (5b) | 本文照読 | ✅ 一致 |
| **R7: 生成プロンプトの内容規約 ①〜⑥** | `SKILL.md:258-274` (5a) | 6 項目を 1 件ずつ spec:95 と突合 (①能力マップ+レシピ+段階開示 / ②標準 6 フィールド・en/ja 同名・language 一致 / ③コメントレス・英語統一 / ④ローカル絶対パス等の禁止 / ⑤旧 docs/・openspec 参照新設禁止 / ⑥実装コード・テストでの最終確認と drift 報告) | ✅ 一致 |
| └ S: プロンプトの制約明記 | `SKILL.md:258-274` | 同上。5b (README 用 `SKILL.md:292-340`) は ①② を適用外と明示するが、本 Scenario の対象は「Skill 更新の委譲プロンプト」であり 5a が充足 | ✅ 一致 |
| **R8: コード正の機械チェック (3 種)** | `SKILL.md:177-187` (規範表) | 取得元パスの実在は verify-001 で確認済み。今サイクルで表の内容に変更なし | ✅ 一致 |
| └ S: Sample デモ画面の追加 | `SKILL.md:184` (突合先 `samples/*/README.md`) | 本文照読 | ✅ 一致 |
| └ S: ツール最低バージョンの変更 | `SKILL.md:185` (突合先 ルート README 群 + 該当記載を持つ SKILL.md 導入節) | 本文照読。`--readme-only` 時の Skill 側は報告のみ (`SKILL.md:179`) → deviation 記録済み | ⚠️ deviation 記録済み |
| **R9: 整合性チェック一式 (8 種)** | `SKILL.md:344-648` (6-①〜6-⑧ + モード分岐 `:350-365`) | 下記の個別結果を参照 | ✅ 一致 |
| └ ①網羅検査 | `SKILL.md:413-423` + 予定 manifest 生成 `SKILL.md:367-391` | 予定 manifest 経由で `concepts coverage OK` に転じることを前サイクルで実測。今回は `export` 削除後の生成ブロックが単体で走ることを再実測 | ✅ 一致 |
| └ ②en/ja 節構成一致 | `SKILL.md:425-464` (`language_pairs()` + `README_ONLY` 分岐) | fixture 実測: 通常モードで Skill ペア負例 `heading levels differ en=[1, 2] ja=[1, 3]` を検出、`--readme-only` では Skill ペアを外し README ペア負例 `README.md: heading levels differ` を検出 | ✅ 一致 |
| └ ③コードブロック byte 一致 | `SKILL.md:467-505` (同上の分岐) | fixture 実測: 通常モードで `code block #1 differs`、`--readme-only` では README ペアのみ検出 | ✅ 一致 |
| └ ④frontmatter 検査 | `SKILL.md:507-566` | 負例 4 種の検出は前サイクルで実測済み。今サイクルの改稿は `--readme-only` 時 N/A の但し書き (`SKILL.md:509`) のみでロジック不変。`compile()` 通過 | ✅ 一致 |
| └ ⑤旧名残 grep | `SKILL.md:568-589` | `docs/` パターンをネスト・複数ドット・裸参照・リンク内で再実測検出、`kasane/concepts/docs/` と URL・`docs-refresh` は誤検出しないことも再確認。空ガードあり | ✅ 一致 |
| └ ⑥内部リンク解決 | `SKILL.md:591-614` | 前サイクルで実測済み・今サイクル改稿なし (空ガード不在は review-003 🔵-2 として品質指摘。R9 の要求充足には影響しない) | ✅ 一致 |
| └ ⑦identity-lint (ローカル絶対パス含む) | `SKILL.md:616-633` + `kasane/config.yaml:57` の `scope` へ `skills` 追加 | 変更・追加ファイル 5 件に両スクリプトを実行し exit 0。空ガード (`SKILL.md:621`) を zsh で実測 (lint が起動しない) | ✅ 一致 |
| └ ⑧配信識別子の表記ゆれ grep | `SKILL.md:635-650` | 空ガードを zsh で実測。パターンは不変 | ✅ 一致 |
| └ S: en/ja 構成乖離の検出 | 6-②③ | 上記負例で実測 | ✅ 一致 |
| └ S: 混入の検出 | 6-⑤ + 6-⑦ | 上記負例で実測 | ✅ 一致 |
| └ S: frontmatter 違反の検出 | 6-④ | 前サイクル負例で実測 | ✅ 一致 |
| **R10: 実行フラグ** | `SKILL.md:55-59`, `SKILL.md:123-125`, `SKILL.md:179`, `SKILL.md:350-365`, `SKILL.md:662` | 下記 3 Scenario とも充足 | ✅ 一致 |
| └ S: --all の全再生成 | `SKILL.md:55`, `SKILL.md:57`, `SKILL.md:123` | 本文照読。`--all` でスキップするのはハッシュ差分検出 (3a・3b) に限定され、`targets` 全キー + `readmes` が要追従に載る THEN は充足。網羅検査 (3c) の追加実行は THEN を損なわず、R4 / R9-① の SHALL とも整合 | ✅ 一致 |
| └ S: --readme-only の軽量チェック | `SKILL.md:58`, `SKILL.md:125`, `SKILL.md:350-365`, `SKILL.md:397-411`, `SKILL.md:685` | **前回 ❌ が解消**。Step 6 にモード分岐節が新設され、(a) 再修正対象は `readmes` 由来に限定 (`:352`)、(b) 8 検査それぞれの扱いを表で確定 (`:354-363`)、(c) 対象一覧生成が `--readme-only` で `readmes` のみを書き出す (`:397-411`)、(d) 6-②/6-③ がスクリプト側で `targets` を外す (`:437`/`:478`)。fixture で `--readme-only` を通し、対象一覧が `readmes` 3 件のみ・Skill ペアの負例が検査対象から外れることを実測。skills/ 本体が検出・更新の対象にならない THEN を充足 | ✅ 一致 |
| └ S: --readme-only は concept 差分を消費しない | `SKILL.md:662` | 本文照読 (`concepts` / `targets` / `excluded` を更新せず `generatedAt` / `lastUpdatedFiles` のみ)。6-① を報告のみに落とす `SKILL.md:423` とも整合 | ✅ 一致 |
| **R11: manifest の更新** | `SKILL.md:652-665` | 本文照読 | ✅ 一致 |
| └ S: 中断時の再検出可能性 | `SKILL.md:663` | 本文照読 | ✅ 一致 |
| └ S: 部分承認時の再検出可能性 | `SKILL.md:224`, `SKILL.md:658-659` | 本文照読。新設の削除済み concept 除去規則 (`SKILL.md:661`) は「整理が未承認・未完了なら `concepts` に残す」と条件を切っており、本 Scenario の「未処理は旧ハッシュ保持」と両立する | ✅ 一致 |
| **R12: 起動規律の維持** | `SKILL.md:3` (description の「**自発的な自動発動はしない**」) + `SKILL.md:682` (Guardrails) | 両方に保持されていることを確認 | ✅ 一致 |
| └ S: concepts 更新後の非発動 | 同上 | 本文照読 | ✅ 一致 |

### 規約記述の更新 (Requirement を持たない proposal「What Changes」分)

| 項目 | 実装 | 検証 | 状態 |
|---|---|---|---|
| AGENTS.md の 2 記述の書き換え・docs/ 言及除去・凍結注記なし | `AGENTS.md:14-15` | 前サイクルから未変更。`docs/` ディレクトリへの言及ゼロ、凍結注記なし | ✅ 一致 |
| CLAUDE.md への反映 | `CLAUDE.md` は `AGENTS.md` への symlink | 追加作業不要 | ✅ 一致 |
| config.yaml `context` の skills/ ベース書き換え | `kasane/config.yaml:8-10` | 前サイクルから未変更 | ✅ 一致 |
| `lint.identity.scope` への `skills` 追加 | `kasane/config.yaml:57` | diff で反映を確認 | ✅ 一致 |
| `lint.exclude` の `docs/` は据え置き (phase-12) | `kasane/config.yaml:52-55` | 未変更を diff で確認 | ✅ 一致 (Non-Goal どおり) |

## 追加検査

- **tasks.md の虚偽チェック**: なし。1.1〜1.11 / 2.1〜2.3 / 3.1〜3.5 の全 19 タスクについて対応する本文・設定の実在を上表で確認。検証タスク 3.2 (スクリプト断片の実行確認) は当検証者が改稿後の全 python heredoc の `compile()`、全 bash ブロックの `bash -n` / `zsh -n`、および `--readme-only` 経路の fixture 実行で独立に再確認した
- **逆流検査**: 足場アーティファクト (proposal.md / specs/) に書き換えなし
- **未記録乖離**: **なし**。前回の ❌ (R10 の `--readme-only` Scenario) は実装 (スキル本文) 側の修正で解消しており、deviation の追加は不要だった (verify-002 の見立てどおり)
- **付随修正**: diff 中に Scenario へ対応しない変更なし (tasks.md のチェック状態更新は進捗管理であり対象外)
- **UI 変更**: なし (`ui/` アーティファクト不要)
- **テスト全件成功**: 製品コードへの変更がないため製品テストスイートは対象外。代替として (a) 埋め込みスクリプトの構文検査 (python heredoc 全件 `compile()` OK、bash ブロック 12 件が bash / zsh 両方で構文 OK)、(b) `--readme-only` 経路の fixture 通し実行 (対象一覧生成 / 6-② / 6-③ を正例・負例とも)、(c) 空ガード (6-⑤ / 6-⑦ / 6-⑧) の zsh 実測、(d) 予定 manifest 生成ブロックの単体実行、(e) `scripts/local-path-lint.py` / `scripts/identity-lint.py` を変更・追加ファイル 5 件へ実行 (両方 exit 0)、(f) manifest 規範 JSON の byte 一致 — すべて成功。fixture はリポジトリ外の scratchpad に置き、実行後 `trash` で破棄 (リポジトリ内・`/tmp` に残骸なし。`git status` で新規ファイルが増えていないことを確認)

## 判定理由

20 Scenario のうち **19 件が「✅ 一致」、1 件が「⚠️ deviation 記録済み」** (R8 のツール最低バージョン Scenario の `--readme-only` 分)。❌ はゼロ。虚偽チェックなし、逆流なし、代替検証すべて成功。よって **VALID**。

前回 ❌ だった R10 / Scenario「`--readme-only` の軽量チェック」は、Step 6 のモード分岐節 (`SKILL.md:350-365`) と対象一覧生成のモード分岐 (`SKILL.md:397-411`)、6-②/6-③ のスクリプト側分岐によって「skills/ 本体は検出・更新の対象にならない」THEN を満たすようになった。fixture 実測でも `--readme-only` 実行時に Skill ファイルが検査対象から外れることを確認済み。

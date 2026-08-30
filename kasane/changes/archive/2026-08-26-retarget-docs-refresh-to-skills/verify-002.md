# 検証結果: retarget-docs-refresh-to-skills (002 回目)

**日付**: 2026-08-26
**判定**: INVALID (❌ 1 件)

## 検証範囲と方法

修正サイクル 1 周目を経た作業ツリーの状態を対象に、デルタスペック 12 Requirement / 19 Scenario を全件突き合わせ直した。本 change はコード変更を伴わないため、対応表の「実装」列は `.agents/skills/docs-refresh/SKILL.md` の行番号、「テスト」列は当検証者がリポジトリ外の一時 fixture で再実行した結果および本文照読を指す。

- デルタスペック: `specs/docs-refresh/spec.md` (ADDED Requirements 12 件 / Scenario 19 件)
- deviation.md: **あり** (1 件 — `--readme-only` × 3d③ の交差を「報告のみ」で確定。合意済みの差分として扱う)
- 逆流検査: `git status` 上 `M` は `.agents/skills/docs-refresh/SKILL.md` / `AGENTS.md` / `kasane/config.yaml` / `kasane/changes/.../tasks.md` のみ。proposal.md / specs/ に変更なし → **逆流なし**
- 前回 [verify-001.md](verify-001.md) から SKILL.md が改稿されているため、全 Requirement を再走査した (差分検出・委譲・整合性チェック・実行フラグ・manifest 更新は重点再検証)

## 対応表

| Requirement / Scenario | 実装 | テスト (検証) | 状態 |
|---|---|---|---|
| **R1: manifest v3 に基づく差分検出** | `.agents/skills/docs-refresh/SKILL.md:117-137` | 本文照読 | ✅ 一致 |
| └ S: concept 変更の逆引き | `SKILL.md:127-135` (targets を値から逆引き → en/ja 言語ペア) | 本文照読 | ✅ 一致 |
| └ S: メインコンテキストは concepts 本文を読まない | `SKILL.md:45-49`, `SKILL.md:647` | 本文照読。委譲不能時にメインが代読するフォールバックを禁じる記述が 3 箇所 (`:49` / `:227` / `:647`) に追加された | ✅ 一致 |
| └ (README は逆引き対象外) | `SKILL.md:137` | 本文照読 | ✅ 一致 |
| **R2: manifest v3 スキーマの規範** | `SKILL.md:97-115` | spec:27-37 と `SKILL.md:99-109` の **byte 一致**を機械照合 (`True`)。プレースホルダ置換後の `json.loads` 成功 (7 キー) | ✅ 一致 |
| └ S: 削除された concept の整理 | 提示 `SKILL.md:143` / 予定 manifest での除去 `SKILL.md:360-363` / 書き出し `SKILL.md:625` | fixture で `gone/old.md` を削除済みに仕立て、3c で `DELETED` 検出 → `DROP_CONCEPTS` 反映後の予定 manifest で `targets` から除去されることを実測 | ✅ 一致 |
| └ 不変条件 ①②③ | `SKILL.md:111-115` | spec:39 と 3 件とも一致 | ✅ 一致 |
| **R3: manifest 不在・非対応時の停止** | `SKILL.md:70-95` (不在 / parse 不能 / version≠3 / 必須キー欠落・型不正 の 4 条件、フォールバック明示禁止) | 本文照読 | ✅ 一致 |
| └ S: manifest 不在での起動 | `SKILL.md:79-93` (案内文に「skills/・README 群・manifest のいずれも変更していません」) | 本文照読 | ✅ 一致 |
| **R4: concepts 網羅検査** | `SKILL.md:139-169` | fixture で未参照 concept (`core/new.md`) を `UNCOVERED` として検出、正例で `concepts coverage OK` | ✅ 一致 |
| └ S: 未参照・未除外 concept の検出 | `SKILL.md:141` + スクリプト `SKILL.md:147-168` | 同上。配置判断をユーザーへ提示する記述 (`SKILL.md:141`, `SKILL.md:195-197`) も存在 | ✅ 一致 |
| **R5: 更新方針の承認ゲート** | `SKILL.md:183-218` | 本文照読 (`SKILL.md:218` に無変更保証・部分承認時のハッシュ非更新)。提示例に連番が付き、部分承認の指定手段が成立 | ✅ 一致 |
| └ S: 承認前の無変更 | `SKILL.md:218` | 本文照読 | ✅ 一致 |
| **R6: Skill 単位の委譲と en/ja ペア生成** | `SKILL.md:220-227` (Skill 単位 / README 単位 / 最大 3 並列 / 委譲不能時は停止) | 本文照読 | ✅ 一致 |
| └ S: 同一 Skill 内の複数ファイル更新 | `SKILL.md:224` (「同じ Skill の複数ファイルを分割しない」) | 本文照読 | ✅ 一致 |
| └ S: 言語ペアの同時更新 | `SKILL.md:243`, `SKILL.md:270-273`, `SKILL.md:651` | 本文照読。README 側は `SKILL.md:294-295`, `SKILL.md:326-327` (5b) がペア同時更新を要求 | ✅ 一致 |
| **R7: 生成プロンプトの内容規約 ①〜⑥** | `SKILL.md:252-268` (5a) | 6 項目を 1 件ずつ spec:95 と突合 (①能力マップ+レシピ+段階開示 / ②標準 6 フィールド・en/ja 同名・language 一致 / ③コメントレス・英語統一 / ④ローカル絶対パス等の禁止 / ⑤旧 docs/・openspec 参照新設禁止 / ⑥実装コード・テストでの最終確認と drift 報告) | ✅ 一致 |
| └ S: プロンプトの制約明記 | `SKILL.md:252-268` | 同上。新設の 5b (README 用 `SKILL.md:282-334`) は ①② を適用外と明示するが、本 Scenario の対象は「Skill 更新の委譲プロンプト」であり 5a が充足している | ✅ 一致 |
| **R8: コード正の機械チェック (3 種)** | `SKILL.md:171-181` (規範表) | 取得元パスの実在は verify-001 で確認済み、今回の改稿で表の内容に変更なし (差分は `SKILL.md:173` の `--readme-only` 但し書きのみ) | ✅ 一致 |
| └ S: Sample デモ画面の追加 | `SKILL.md:178` (突合先 `samples/*/README.md`) | 本文照読 | ✅ 一致 |
| └ S: ツール最低バージョンの変更 | `SKILL.md:179` (突合先 ルート README 群 + 該当記載を持つ SKILL.md 導入節) | 本文照読。`--readme-only` 時の Skill 側は報告のみ (`SKILL.md:173`) → deviation 記録済み | ⚠️ deviation 記録済み |
| **R9: 整合性チェック一式 (8 種)** | `SKILL.md:338-615` (6-①〜6-⑧) | 下記の個別結果を参照 | ✅ 一致 |
| └ ①網羅検査 | `SKILL.md:386-396` + 予定 manifest 生成 `SKILL.md:344-370` | fixture で「新規 + 削除の同時ケース」を通し、ディスク manifest では `UNCOVERED` / `DELETED`、予定 manifest では `concepts coverage OK` を実測 (前回の偽陽性ループは解消) | ✅ 一致 |
| └ ②en/ja 節構成一致 | `SKILL.md:398-437` (`language_pairs()` で targets ペア + readmes の `_ja` ペア) | fixture 負例 `README.md: heading levels differ en=[1,2] ja=[1,3]` を検出、正例 `OK`。ペア無し README のスキップも確認 | ✅ 一致 |
| └ ③コードブロック byte 一致 | `SKILL.md:439-476` | fixture 負例 `README.md: code block #1 differs` を検出、正例 `byte-identical` | ✅ 一致 |
| └ ④frontmatter 検査 | `SKILL.md:478-537` | 前回 fixture で負例 4 種を検出済み。今回の改稿は manifest 読み込み行のみ (ロジック不変) | ✅ 一致 |
| └ ⑤旧名残 grep | `SKILL.md:539-560` | 空ガード追加。`docs/` パターンをネスト・複数ドット・裸参照・リンク内で実測検出、`kasane/concepts/docs/` と URL は誤検出しないことも確認 | ✅ 一致 |
| └ ⑥内部リンク解決 | `SKILL.md:562-585` | 前回実測済み。今回の改稿なし | ✅ 一致 |
| └ ⑦identity-lint (ローカル絶対パス含む) | `SKILL.md:587-598` + `kasane/config.yaml:57` の `scope` へ `skills` 追加 | 変更・追加ファイル 5 件に対し両スクリプトを実行、いずれも exit 0。実効範囲の注記が `SKILL.md:598` に追加 | ✅ 一致 |
| └ ⑧配信識別子の表記ゆれ grep | `SKILL.md:600-615` | 空ガード追加。パターンは前回から不変 | ✅ 一致 |
| └ S: en/ja 構成乖離の検出 | 6-②③ | 上記負例で実測 | ✅ 一致 |
| └ S: 混入の検出 | 6-⑤ + 6-⑦ | 上記負例で実測 | ✅ 一致 |
| └ S: frontmatter 違反の検出 | 6-④ | 前回負例で実測 | ✅ 一致 |
| **R10: 実行フラグ** | `SKILL.md:55-57`, `SKILL.md:119`, `SKILL.md:173`, `SKILL.md:626` | 本文照読 | ❌ 乖離 (下記 Scenario) |
| └ S: --all の全再生成 | `SKILL.md:55`, `SKILL.md:119` | 本文照読 | ✅ 一致 |
| └ S: --readme-only の軽量チェック | `SKILL.md:56`, `SKILL.md:119`, `SKILL.md:173`, `SKILL.md:396` | 3d③ 由来の Skill 更新は deviation どおり報告のみに封じられている。しかし Step 6 の対象一覧 (`SKILL.md:375-383`) はモード分岐を持たず全 `targets` を含み、`SKILL.md:340` が失敗を「再修正対象」に載せるため、6-②〜⑧ 経由で skills/ 本体が更新されうる。deviation.md の合意範囲は 3d③ のみで、この経路は未記録 | ❌ 未記録乖離 |
| └ S: --readme-only は concept 差分を消費しない | `SKILL.md:626` | 本文照読 (`concepts` / `targets` / `excluded` を更新せず `generatedAt` / `lastUpdatedFiles` のみ) | ✅ 一致 |
| **R11: manifest の更新** | `SKILL.md:617-629` | 本文照読 | ✅ 一致 |
| └ S: 中断時の再検出可能性 | `SKILL.md:627` | 本文照読 | ✅ 一致 |
| └ S: 部分承認時の再検出可能性 | `SKILL.md:218`, `SKILL.md:623-624` | 本文照読。提示例の連番付与で部分承認の指定手段が成立 | ✅ 一致 |
| **R12: 起動規律の維持** | `SKILL.md:3` (description の「**自発的な自動発動はしない**」) + `SKILL.md:646` (Guardrails) | 両方に保持されていることを確認 | ✅ 一致 |
| └ S: concepts 更新後の非発動 | 同上 | 本文照読 | ✅ 一致 |

### 規約記述の更新 (Requirement を持たない proposal「What Changes」分)

| 項目 | 実装 | 検証 | 状態 |
|---|---|---|---|
| AGENTS.md の 2 記述の書き換え・docs/ 言及除去・凍結注記なし | `AGENTS.md:14-15` | 前回から未変更。`docs/` ディレクトリへの言及ゼロ、凍結注記なし | ✅ 一致 |
| CLAUDE.md への反映 | `CLAUDE.md` は `AGENTS.md` への symlink | 追加作業不要 | ✅ 一致 |
| config.yaml `context` の skills/ ベース書き換え | `kasane/config.yaml:8-10` | 前回から未変更 | ✅ 一致 |
| `lint.identity.scope` への `skills` 追加 | `kasane/config.yaml:57` | diff で反映を確認 | ✅ 一致 |
| `lint.exclude` の `docs/` は据え置き (phase-12) | `kasane/config.yaml:52-55` | 未変更を diff で確認 | ✅ 一致 (Non-Goal どおり) |

## 追加検査

- **tasks.md の虚偽チェック**: なし。1.1〜1.11 / 2.1〜2.3 / 3.1〜3.5 の全 19 タスクについて対応する本文・設定の実在を上表で確認。検証タスク 3.2 (スクリプト断片の実行確認) は当検証者が改稿後のスクリプト (予定 manifest 生成 / 6-① / 6-② / 6-③ / 6-⑤ の `docs/` パターン) を独立に fixture 実行して再確認した
- **逆流検査**: 足場アーティファクト (proposal.md / specs/) に書き換えなし
- **未記録乖離**: 1 件 (上表 R10 の `--readme-only` Scenario)。deviation.md に記録があるのは 3d③ の交差のみで、Step 6 経由の経路は未記録
- **付随修正**: diff 中に Scenario へ対応しない変更なし (tasks.md のチェック状態更新は進捗管理であり対象外)
- **UI 変更**: なし (`ui/` アーティファクト不要)
- **テスト全件成功**: 製品コードへの変更がないため製品テストスイートは対象外。代替として (a) 改稿された検査スクリプト断片をリポジトリ外の一時 fixture で正例・負例とも実行、(b) `scripts/local-path-lint.py` / `scripts/identity-lint.py` を変更・追加ファイル 5 件に対して実行 (両方 exit 0)、(c) manifest 規範 JSON の byte 一致と parse — すべて成功。fixture は scratchpad に置き、実行後 `trash` で破棄 (リポジトリ内・`/tmp` に残骸なし)

## 判定理由

19 Scenario のうち 17 件が「✅ 一致」、1 件が「⚠️ deviation 記録済み」(R8 のツール最低バージョン Scenario の `--readme-only` 分)、1 件が「❌ 未記録乖離」。よって **INVALID**。

### ❌ の見立て

**R10 / Scenario「--readme-only の軽量チェック」** — spec の THEN は「skills/ 本体は検出・更新の対象にならない」。本文は 3d③ (deviation 済み) と 6-① (`SKILL.md:396`) では対象外に落としているが、Step 6 の検査対象一覧 (`SKILL.md:375-383`) と再修正ルール (`SKILL.md:340`) がモード分岐を持たないため、6-②〜⑧ の失敗経由で skills/ が更新されうる。

**見立て: 実装 (スキル本文) を直すべき。** deviation として合意する必要はない — オーナーが既に確定した方針 (「フラグの意味を優先し、skills/ 側は報告のみ」) をそのまま Step 6 にも及ぼす 1〜2 文の追記で Scenario を満たせるため、追加の合意事項は生じない。実装後に本 Scenario は ✅ へ戻る。詳細と推奨文面は [review-002.md](review-002.md) の 🟠 Major-1 を参照。

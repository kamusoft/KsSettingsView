# 検証結果: retarget-docs-refresh-to-skills (001 回目)

**日付**: 2026-08-26
**判定**: VALID

## 検証範囲と方法

本 change はコード変更を伴わないスキル文書・規約記述の改修であり、「実装」は `.agents/skills/docs-refresh/SKILL.md` の本文、「テスト」は本文に埋め込まれた検査スクリプト断片の実行可能性・検出ロジックにあたる。したがって対応表の「実装」列は SKILL.md の行番号、「テスト」列は当レビュアが一時 fixture (リポジトリ外) で再実行した結果を指す。

- デルタスペック: `specs/docs-refresh/spec.md` (ADDED Requirements 12 件 / Scenario 19 件)
- deviation.md: 不在 (記録された乖離なし)
- 逆流検査: `git log -- proposal.md specs/` は d8f6771 の 1 コミットのみ。作業ツリーでも proposal.md / specs/ は未変更 (`git status` で `M` は SKILL.md / AGENTS.md / config.yaml / tasks.md のみ) → **逆流なし**

## 対応表

| Requirement / Scenario | 実装 | テスト (検証) | 状態 |
|---|---|---|---|
| **R1: manifest v3 に基づく差分検出** | `.agents/skills/docs-refresh/SKILL.md:115-135` | 本文照読 | ✅ 一致 |
| └ S: concept 変更の逆引き | `SKILL.md:125-133` (targets を値から逆引き → en/ja 言語ペアで要追従) | 本文照読 | ✅ 一致 |
| └ S: メインコンテキストは concepts 本文を読まない | `SKILL.md:45-47`, `SKILL.md:513` | 本文照読 | ✅ 一致 |
| └ (README は逆引き対象外) | `SKILL.md:135` | 本文照読 | ✅ 一致 |
| **R2: manifest v3 スキーマの規範** | `SKILL.md:97-113` | spec の規範 JSON と **byte 一致** を機械照合 / `json.loads` で parse 成功 (7 キー) | ✅ 一致 |
| └ S: 削除された concept の整理 | `SKILL.md:141` (提示) + `SKILL.md:492` (承認後に targets/excluded から除去) | fixture で `DELETED` 検出を実行確認 | ✅ 一致 |
| └ 不変条件 ①②③ | `SKILL.md:109-113` | spec:39 と 3 件とも一致 | ✅ 一致 |
| **R3: manifest 不在・非対応時の停止** | `SKILL.md:68-93` (不在 / parse 不能 / version≠3 / 必須キー欠落・型不正 の 4 条件を列挙、フォールバック明示禁止) | 本文照読 | ✅ 一致 |
| └ S: manifest 不在での起動 | `SKILL.md:79-91` (案内文中に「skills/・README 群・manifest のいずれも変更していません」) | 本文照読 | ✅ 一致 |
| **R4: concepts 網羅検査** | `SKILL.md:137-165` | 一時 fixture で **負例 (未参照 concept 1 件) を検出成功**、正例で `coverage OK` | ✅ 一致 |
| └ S: 未参照・未除外 concept の検出 | `SKILL.md:139`, スクリプト `SKILL.md:143-165` | 同上 (`UNCOVERED` 出力を確認) | ✅ 一致 |
| **R5: 更新方針の承認ゲート** | `SKILL.md:179-212` | 本文照読 (`SKILL.md:212` に無変更保証・部分承認時のハッシュ非更新) | ✅ 一致 |
| └ S: 承認前の無変更 | `SKILL.md:212` | 本文照読 | ✅ 一致 |
| **R6: Skill 単位の委譲と en/ja ペア生成** | `SKILL.md:214-221` (Skill 単位 / README 単位 / 最大 3 並列) | 本文照読 | ✅ 一致 |
| └ S: 同一 Skill 内の複数ファイル更新 | `SKILL.md:218` (「同じ Skill の複数ファイルを分割しない」) | 本文照読 | ✅ 一致 |
| └ S: 言語ペアの同時更新 | `SKILL.md:237`, `SKILL.md:262-265`, `SKILL.md:516` | 本文照読 | ✅ 一致 |
| **R7: 生成プロンプトの内容規約 ①〜⑥** | `SKILL.md:246-260` | 6 項目を 1 件ずつ spec:95 と突合 (①能力マップ+レシピ+段階開示 / ②標準 6 フィールド・en/ja 同名 / ③コメントレス・英語統一 / ④ローカル絶対パス禁止 / ⑤旧 docs/・openspec 参照新設禁止 / ⑥実装コード・テストでの最終確認と drift 報告) | ✅ 一致 |
| └ S: プロンプトの制約明記 | `SKILL.md:246-260` | 同上。④ は spec の要求 (絶対パス) に加えて個体/個人/秘密も禁じる上位互換 | ✅ 一致 |
| **R8: コード正の機械チェック (3 種)** | `SKILL.md:167-177` (規範表: 項目・取得元・抽出方法・突合先) | 表に列挙された取得元 11 パスの実在をすべて確認 (`ios/Package.swift` の `// swift-tools-version:` / `.iOS(.v16)`、`libs.versions.toml` の `[versions] agp/kotlin`、`csproj` の `<TargetFrameworks>`、`SampleScreenCategory.Verification` の存在まで実地確認) | ✅ 一致 |
| └ S: Sample デモ画面の追加 | `SKILL.md:174` (突合先 `samples/*/README.md`) | 3 platform の列挙元がすべて実在 | ✅ 一致 |
| └ S: ツール最低バージョンの変更 | `SKILL.md:175` (突合先 ルート README 群 + 該当記載を持つ SKILL.md 導入節) | 取得元 5 種すべて実在確認 | ✅ 一致 |
| **R9: 整合性チェック一式 (8 種)** | `SKILL.md:276-482` (6-①〜6-⑧) | 下記の個別実行結果を参照 | ⚠️ 一致 (ただし 6-① の実行順序に品質上の問題 — review-001 🟠-1) |
| └ ①網羅検査 | `SKILL.md:294-296` | fixture 実行成功 (ただし Step 7 との順序問題は review 側で指摘) | ✅ 一致 |
| └ ②en/ja 節構成一致 | `SKILL.md:298-325` | fixture 正例 `OK` / 負例 `heading levels differ en=[1,2] ja=[1,2,3]` を検出 | ✅ 一致 |
| └ ③コードブロック byte 一致 | `SKILL.md:327-353` | fixture 正例 `byte-identical` / 負例 `code block #1 differs` を検出 | ✅ 一致 |
| └ ④frontmatter 検査 | `SKILL.md:355-414` | fixture 正例 `OK` / 負例 4 件 (`非標準 top field` / `非標準 metadata field` / `language 不一致` / `name 不一致`) をすべて検出 | ✅ 一致 |
| └ ⑤旧名残 grep | `SKILL.md:416-434` | zsh で配列展開含め実行成功。`docs/getting-started.md` + `KsColor` 混入行を検出 | ✅ 一致 |
| └ ⑥内部リンク解決 | `SKILL.md:436-459` | fixture の未解決リンクを `MISSING` として検出 | ✅ 一致 |
| └ ⑦identity-lint (ローカル絶対パス含む) | `SKILL.md:461-470` + `kasane/config.yaml:57` の `scope` へ `skills` 追加 | 両スクリプトの `--paths` 実装を確認 (`local-path-lint.py:285-292` / `identity-lint.py:349-356`)、変更ファイル 4 件に対し exit 0 | ✅ 一致 |
| └ ⑧配信識別子の表記ゆれ grep | `SKILL.md:472-482` | zsh で実行成功。`KsSettingsview` 混入行を検出。パターンは `cross/conventions/public-identifiers.md` の規則と整合 | ✅ 一致 |
| └ S: en/ja 構成乖離の検出 | 6-②③ | 上記負例で実測 | ✅ 一致 |
| └ S: 混入の検出 | 6-⑤ + 6-⑦ | 上記負例で実測 | ✅ 一致 |
| └ S: frontmatter 違反の検出 | 6-④ | 上記負例で実測 | ✅ 一致 |
| **R10: 実行フラグ** | `SKILL.md:51-55`, `SKILL.md:117`, `SKILL.md:493` | 本文照読 | ⚠️ 一致 (`--readme-only` × Step 3d③ の交差が未定義 — review-001 🟠-2) |
| └ S: --all の全再生成 | `SKILL.md:53`, `SKILL.md:117` | 本文照読 | ✅ 一致 |
| └ S: --readme-only の軽量チェック | `SKILL.md:54`, `SKILL.md:117` | 本文照読。ただし `SKILL.md:117` が 3d を両モードで実行させ、3d③ の突合先に SKILL.md が含まれるため「skills/ 本体は検出対象にならない」との交差が本文で解決されていない | ⚠️ 要明文化 (spec 内 2 Requirement の交差。乖離ではないが曖昧) |
| └ S: --readme-only は concept 差分を消費しない | `SKILL.md:493` | 本文照読 | ✅ 一致 |
| **R11: manifest の更新** | `SKILL.md:484-496` | 本文照読 | ✅ 一致 |
| └ S: 中断時の再検出可能性 | `SKILL.md:494` | 本文照読 | ✅ 一致 |
| └ S: 部分承認時の再検出可能性 | `SKILL.md:212`, `SKILL.md:490-491` | 本文照読 | ✅ 一致 |
| **R12: 起動規律の維持** | `SKILL.md:3` (description 内「**自発的な自動発動はしない**」) + `SKILL.md:512` (Guardrails) | 両方に保持されていることを確認 | ✅ 一致 |
| └ S: concepts 更新後の非発動 | 同上 | 本文照読 | ✅ 一致 |

### 規約記述の更新 (Requirement を持たない proposal「What Changes」分)

| 項目 | 実装 | 検証 | 状態 |
|---|---|---|---|
| AGENTS.md の 2 記述の書き換え・docs/ 言及除去・凍結注記なし | `AGENTS.md:14-15` | `grep docs AGENTS.md` の結果は `docs-refresh` / `.agents/skills/docs-refresh/SKILL.md` のみ。`docs/` ディレクトリへの言及ゼロ、凍結注記なし | ✅ 一致 |
| CLAUDE.md への反映 | `CLAUDE.md` は `AGENTS.md` への symlink (`ls -la` で確認) | 追加作業不要 | ✅ 一致 |
| config.yaml `context` の skills/ ベース書き換え | `kasane/config.yaml:8-10` | YAML parse 成功。`docs/` 言及なし | ✅ 一致 |
| `lint.identity.scope` への `skills` 追加 | `kasane/config.yaml:57` | `Settings(".").scope == ['kasane','openspec','docs','skills']` を実測 | ✅ 一致 |
| `lint.exclude` の `docs/` は据え置き (phase-12) | `kasane/config.yaml:52-55` | 未変更を diff で確認 | ✅ 一致 (Non-Goal どおり) |

## 追加検査

- **tasks.md の虚偽チェック**: なし。1.1〜1.11 / 2.1〜2.3 / 3.1〜3.5 の全 19 タスクについて、対応する本文・設定の実在を上表で確認済み。特に検証タスク 3.2 (スクリプト断片の実行確認) は当レビュアが独立に fixture で再実行して同じ結果を得た。3.3 (manifest JSON の妥当性・規範一致) も `json.loads` と byte 比較で独立確認した
- **逆流検査**: 足場アーティファクト (proposal.md / specs/) に実装期間中の書き換えなし
- **未記録乖離**: ❌ なし。deviation.md は不在だが、記録すべき乖離も検出されなかった
- **付随修正**: diff 中に Scenario へ対応しない変更なし (`tasks.md` のチェック状態更新は進捗管理であり対象外)
- **UI 変更**: なし (`ui/` アーティファクト不要)
- **テスト全件成功**: 製品コードへの変更がないため製品テストスイートは対象外。代替として (a) 本文の全検査スクリプト断片をリポジトリ外の一時 fixture で正例・負例の両方で実行、(b) リポジトリの `scripts/local-path-lint.py` / `scripts/identity-lint.py` を変更ファイル 4 件に対して実行 (両方 exit 0)、(c) `kasane/config.yaml` の YAML parse — すべて成功。fixture はリポジトリ外に置き、実行後に `trash` で破棄済み (リポジトリ内に残骸なし)

## 判定理由

全 12 Requirement / 19 Scenario が「✅ 一致」。未記録の欠落・乖離、tasks.md の虚偽チェック、足場の逆流、検査の失敗はいずれもなし。よって **VALID**。

⚠️ を付した 2 点 (6-① の実行順序、`--readme-only` × 3d③ の交差) は、いずれも「spec の記述と本文の記述が食い違っている」種類の乖離ではなく、spec どおりに書かれた結果として運用上の不整合が生じる**品質**の問題である。一致検証としては乖離に当たらないため VALID を維持し、指摘は [review-001.md](review-001.md) 側で 🟠 Major として出す。

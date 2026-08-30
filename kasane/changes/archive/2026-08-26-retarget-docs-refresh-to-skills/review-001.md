# レビュー結果: retarget-docs-refresh-to-skills (001 回目)

**日付**: 2026-08-26
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの 12 Requirement / 19 Scenario はすべて SKILL.md 本文に反映されており、manifest v3 の規範 JSON は spec と byte 一致、規約記述 (AGENTS.md / config.yaml) も proposal「What Changes」どおりで docs/ 言及の除去・凍結注記なしの方針を満たしている。本文に埋め込まれた 7 種の検査スクリプト断片は当レビュアがリポジトリ外の一時 fixture で正例・負例の両方を実行し、いずれも構文が通り意図どおり検出することを実測した。

一方で、フロー全体を通しで追うと **Step 6-① (網羅検査の再実行) が Step 7 (manifest 書き出し) より前にあるため、このスキルが最も重視する「新規 concept の配置判断」ケースで必ず偽陽性になり、本文が指示する再修正ループから抜けられない**。もう 1 件、`--readme-only` と Step 3d③ の交差が本文で解決されていない。いずれも数行の明文化で解決できるが、スキル文書は「書いてあるとおりに実行される」ことが唯一の担保なので、実行不能な手順を残したままは通せない。

## 指摘事項

### [🟠 Major] 1. Step 6-① の網羅検査は Step 7 より前にあるため、新規・削除 concept のケースで必ず失敗し再修正ループに入る

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:294-296` (および `SKILL.md:139-141`, `SKILL.md:484-492` との関係)

**問題点**:

6-① は「Step 3c のスクリプトを再実行し、`UNCOVERED` / `DELETED` が出ないことを確認する」と指示している。しかし 3c のスクリプト (`SKILL.md:143-165`) は `skills/.manifest.json` を**ディスクから読む**一方、`targets` / `excluded` への追記は `SKILL.md:141` が「判断が確定してから行う」、Step 7 (`SKILL.md:486`) が「書き込みは更新フローの**最後**に行う」と定めているため、6-① の実行時点で manifest は依然として旧状態である。結果:

- **新規 concept ケース**: 3c で `UNCOVERED` → Step 4 でユーザーが配置を決定 → Step 5 で Skill ファイルを更新 → 6-① で manifest 未更新のまま再実行 → **再び `UNCOVERED`**
- **削除済み concept ケース**: 同様に `DELETED` が Step 7 まで消えない

`SKILL.md:278` は「失敗した生成物は再修正対象に追加し、修正後に再度この一式を実行する」と定めているため、実行者は「生成物」を直そうとし続けるが、原因は生成物ではなく manifest の書き込み順序にあり、修正しても永久に解消しない。つまり **6-① は、それが守ろうとしている当のケース (新規 concept の追加) では絶対に通らない**。この検査は網羅検査 (ADR-0022 の「配置判断が強制される」利点の実装) の要であるだけに、実行者が「毎回出るので無視してよい」と学習してしまう副作用も大きい。

なお `--readme-only` 実行時はこの問題がさらに顕在化する。`--readme-only` は設計上 concept 差分を消費しない (`SKILL.md:493`) ため、Skill 本体に未反映の concept があるのが正常状態であり、その状態で 6-① を「`UNCOVERED` が出ないこと」として走らせると必ず失敗する。

**推奨修正**: 6-① を「ディスクの manifest」ではなく「**この実行で書き出す予定の targets / excluded (Step 4 で承認された配置判断・削除整理を反映した状態)**」に対して評価する検査だと明記し、3c のスクリプトをそのまま流用するのではなく、承認済み配置判断を反映した `targets` / `excluded` を入力にできる形 (例: 予定 manifest を一時ファイルへ書き出してからスクリプトへ渡す) に書き換える。あわせて `--readme-only` 実行時は 6-① を「報告のみ (次回の通常実行で処理される旨を添える)」とし、再修正対象に載せない旨を明記する。

---

### [🟠 Major] 2. `--readme-only` で Step 3d③ が SKILL.md を要追従リストに載せうるため、「skills/ 本体はスキップ」と矛盾する

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:117` と `SKILL.md:175`

**問題点**:

`SKILL.md:117` は「`--readme-only` 指定時は 3a〜3c をスキップし、`readmes` のみを要追従候補とする (**3d は両モードとも実行する**)」と定める。ところが 3d③ (ツール最低バージョン) の突合先は `SKILL.md:175` で「ルート README 群の対応プラットフォーム表・開発環境要件、**および該当記載を持つ場合は各 `SKILL.md` の導入節**」とされており、`SKILL.md:169` は「差分があった項目は該当 README / **Skill ファイル**を要追従リストへ追加する」と指示している。

したがって `--readme-only` でツールバージョン差分が出ると SKILL.md が更新対象に入り、`SKILL.md:54` / `SKILL.md:117` の「skills/ 本体は検出・更新の対象にしない」および Guardrails `SKILL.md:522` と真正面から食い違う。実行者はその場で恣意的に判断せざるを得ない。

さらに悪いのは、この経路で SKILL.md を更新しても `--readme-only` は `concepts` ハッシュを更新しない (`SKILL.md:493`) ため、「skills/ 本体は触っていない」という manifest 側の前提と実ファイルの状態がずれることである。

**問題の所在**: デルタスペックの 2 つの Requirement (「コード正の機械チェック」の *差分があれば該当 README / Skill ファイルを要追従リストに追加する SHALL* と、「実行フラグ」の *`--readme-only` は … skills/ 本体をスキップする SHALL*) が交差点で衝突しており、spec 自体がこのケースを解いていない。deviation.md にも記録がない。

**推奨修正**: どちらの解に倒すかはユーザー/オーケストレーターの判断が要る。本文に 1 文追加して確定させること。選択肢:

- (A) `--readme-only` では 3d③ の SKILL.md 突合分を**報告のみ**とし、要追従リストに載せない (フラグの意味を優先。「次回の通常実行で処理される」旨を完了サマリに出す)
- (B) `--readme-only` でも 3d 由来の SKILL.md 更新は**例外として許可**する (コード正チェックの網羅性を優先。この場合 Guardrails `SKILL.md:522` と Input `SKILL.md:54` にも例外を明記する)

いずれを採っても spec 本文の修正は不要 (交差の解釈を本文で確定するだけ) だが、(B) を採る場合は「skills/ 本体をスキップする SHALL」の例外として deviation.md への記録が要る。

---

### [🟡 Minor] 3. 6-② / 6-③ が README の en/ja ペアを検査対象から外している

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:313` / `SKILL.md:340` (いずれも `for rel in M["targets"]`)

**問題点**: 6-② (節構成一致) と 6-③ (コードブロック byte 一致) はどちらも manifest の `targets` だけを走査するため、`readmes` に載る言語ペア — cross/ADR-0022 が定める `skills/README.md` ↔ `skills/README_ja.md`、およびルート `README.md` ↔ `README_ja.md` — が検査されない。一方で Step 5 (`SKILL.md:219`) は README を「en/ja ペアがあればペア」で委譲すると定めており、ペア生成した成果物にペア検査が掛からない非対称が残る。spec の Requirement「整合性チェック一式」② / ③ は対象を targets に限定していないので、この絞り込みは本文側の判断である。

**推奨修正**: 6-② / 6-③ の対象に、`readmes` のうち `_ja` サフィックスで対になるファイル (`README.md` ↔ `README_ja.md` 形式) を加える。対応付けの規則 (「同一ディレクトリの `<stem>.md` と `<stem>_ja.md` をペアとみなす」等) を 1 行で明記すれば、スクリプトの追加は数行で済む。

---

### [🟡 Minor] 4. 6-⑦ の identity-lint は README 系パスを scope 外として黙ってスキップする

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:461-470`

**問題点**: `SKILL.md:466-467` は `/tmp/docs-refresh-targets.txt` (Skill ファイル + `readmes`) を両 lint に渡し、「どちらも exit 0 (違反 0 件) であること」と書いている。しかし `scripts/identity-lint.py` は `--paths` で渡されたファイルも `Settings.in_scope()` (パス第 1 セグメントが `lint.identity.scope` に含まれるか) で絞り込む。実測すると:

```
skills/en/x/SKILL.md      in_scope=True
README.md                 in_scope=False
README_ja.md              in_scope=False
android/README.md         in_scope=False
maui/README.md            in_scope=False
samples/ios/README.md     in_scope=False
```

つまり identity 検査 (メールアドレス・ホスト名・シリアル・トークン等) が実効するのは `skills/**` だけで、README 群は**無言で素通りする**。`local-path-lint.py` は scope を持たず `lint.exclude` のみで判定するため、絶対パス検査は README も含めて効いている。本文はこの非対称に触れていないので、実行者は「8 検査で全対象をカバーした」と誤認する。

なお README を検査範囲に入れること自体は本 change のスコープ (`lint.identity.scope` への `skills` 追加まで) を越えるため、対応は明文化で足りる。

**推奨修正**: 6-⑦ に「`identity-lint` の実効範囲は `lint.identity.scope` に載るパス (現状 `skills/`) に限られ、リポジトリ直下や `samples/` の README は対象外である。README の identity 検査が必要になったら `lint.identity.scope` の拡張を別 change で起票する」旨の注記を 1 行加える。

---

### [🟡 Minor] 5. Step 4 の提示例が「項目番号を指定してください」と案内するが、例に番号が振られていない

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:183-209` (特に `SKILL.md:209`)

**問題点**: 提示例の各項目は `●` の箇条書きで、番号が付いていない。にもかかわらず末尾は「特定の項目だけ進めたい場合は**項目番号**を指定してください」と結んでいる。ユーザーは指し示す手段を持たない。部分承認は Requirement「manifest の更新」の Scenario「部分承認時の再検出可能性」が前提とする経路なので、指定手段が成立していないと部分承認そのものが機能しない。

**推奨修正**: 提示例の項目に連番 (`1.` `2.` …) を振るか、末尾の文言を「項目名 (例: `core/cells/rating-cell.md`) を指定してください」へ改める。

---

### [🔵 Suggestion] 6. 対象リストが空のとき 6-⑤ / 6-⑧ の grep が標準入力待ちで停止する

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:421-427`, `SKILL.md:477-479`

**問題点**: `grep -rn ... "${TARGETS[@]}"` および `grep -rn -E ... $(cat /tmp/docs-refresh-targets.txt)` は、配列/コマンド置換が空に展開されるとファイル引数なしの grep になり、標準入力を読んで応答が返らなくなる (自動実行時はタイムアウトまで固まる)。manifest の不変条件上は空にならないが、`targets` が空の壊れた manifest でも Step 2 の必須キー検査は通過しうる (キーは在るが中身が空)。

**推奨修正**: 6-⑤ / 6-⑧ の直前に `[ -s /tmp/docs-refresh-targets.txt ] || { echo "対象なし"; }` 相当のガードを置くか、`grep` に `/dev/null` をダミー引数として常に添える。

---

### [🔵 Suggestion] 7. 6-⑤ の `docs/` 検出パターンがサブディレクトリを取りこぼす

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:426`

**問題点**: `grep -rn "docs/[A-Za-z0-9_-]*\.md"` の文字クラスは `/` を含まないため、`docs/guides/cells.md` のようなネストしたパスを検出しない。現行 `docs/` はフラット構成なので実害はないが、`SKILL.md:434` が「`docs/*.md` への参照は理由を問わず新設しない」と全面禁止を宣言している以上、検出漏れの余地は残さないほうがよい。

**推奨修正**: パターンを `docs/[A-Za-z0-9_/-]*\.md` に広げる。

---

### [🔵 Suggestion] 8. 生成物のコード例コメントを英語統一とする例外の根拠を本文に残す

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:253-254`

**問題点**: プロジェクト規約 (`AGENTS.md` 全体ルール) は「ソースのコメントも日本語とする」と定めており、③ の「やむを得ない最小限のコメントは英語で統一する」はこれと衝突して見える。実際には phase-11 agenda の決定事項「整合性チェックの一式とコード例規約」が、6-③ のコードブロック byte 一致検査を成立させるための意図的な例外として英語統一を選んでいる。本文にその理由が書かれていないと、後の読み手が規約違反と誤認して差し戻す恐れがある。

**推奨修正**: ③ に「(en/ja のコードブロック byte 一致 [6-③] を成立させるための意図的な例外。skills/ のコード例は利用者向け成果物でありソースコメント規約の対象外)」と括弧書きを添える。

---

## 確認した観点 (指摘なし)

- **デルタスペックの充足**: 12 Requirement / 19 Scenario の全件を [verify-001.md](verify-001.md) の対応表で個別に突合。欠落なし
- **manifest v3 規範 JSON**: spec `specs/docs-refresh/spec.md:27-37` と `SKILL.md:97-107` が **byte 一致**。`json.loads` で parse 可能 (7 キー)。不変条件 3 件も一致
- **検査スクリプト断片の妥当性**: 6-②③④⑤⑥⑧ と 3c をリポジトリ外の一時 fixture で実行。正例では全件 OK、負例 (見出し階層差 / コードブロック差 / 非標準 frontmatter フィールド・metadata フィールド・`metadata.language` 不一致・`name` 不一致 / 旧 API と `docs/` 参照の混入 / 未解決内部リンク / 識別子表記ゆれ / 未参照 concept) をすべて検出。`SKILL.md:432` の zsh 注記どおり配列展開も実機で確認済み。fixture は実行後 `trash` で破棄、リポジトリ内に残骸なし
- **コード正チェック表の取得元 (`SKILL.md:171-177`)**: 列挙された 11 パスの実在をすべて確認し、抽出対象の記述 (`// swift-tools-version: 5.10` / `.iOS(.v16)` / `[versions] agp` `kotlin` / `<TargetFrameworks>` / `enum class SampleScreen(... title ...)` / `SampleScreen.All` と `SampleScreenCategory.Verification`) の存在も実地で確認。AGP/Kotlin の取得元を version catalog に統一した注記 (`SKILL.md:177`) も現行構成と一致
- **6-⑧ のパターンと規約の整合**: `cross/conventions/public-identifiers.md` (SwiftPM は PascalCase / Android namespace は lowercase reverse-DNS / artifactId は kebab-case / Maven groupId は ADR-0002 の `jp.kamusoft`) とパターン・注記 (`SKILL.md:482`) が一致。「開発用 GAV を公開済み配布座標と説明しない」という同 concept の禁止事項も守られている
- **AGENTS.md / config.yaml**: proposal「What Changes」の記述と一致。`docs/` ディレクトリへの言及は除去済み、凍結注記なし。`CLAUDE.md` は `AGENTS.md` への symlink のため追加作業不要。`lint.exclude` の `docs/` と `identity.scope` の `docs` は Non-Goal どおり据え置き。`scope` への `skills` 追加は実測で反映を確認
- **足場の凍結**: proposal.md / specs/ に実装期間中の書き換えなし (逆流なし)
- **tasks.md**: 19 タスクすべてに対応する成果物が実在。虚偽チェックなし。検証タスク 3.1〜3.5 は当レビュアが独立に再実行して同じ結論を得た
- **lint**: `scripts/local-path-lint.py` / `scripts/identity-lint.py` を変更ファイル 4 件に対して実行、いずれも exit 0
- **agenda 決定事項 6 件**: 起動規律と AGENTS.md 更新 / 整合性チェック一式とコード例規約 / 委譲単位 / 差分更新フローの細部 / 初期生成の分担 / 2 言語の生成方式 — すべて本文に反映済み
- **ビルド・テスト**: 製品コードへの変更がないため製品テストスイートは非該当。代替検証の内容は [verify-001.md](verify-001.md) の「追加検査」を参照
- **lessons/code-review.md**: 重点観点 L-001 (ミューテーションによる検出力実測) の趣旨に沿い、静的読解で済ませず全検査スクリプトを負例 fixture で実際に落として検出力を確認した。「指摘しないこと」に該当する型の指摘は含まない

## アクションプラン

1. **🟠-1 Step 6-① の評価対象を「書き出す予定の targets / excluded」へ改める** (`SKILL.md:294-296`)。あわせて `--readme-only` 実行時の 6-① を報告のみに落とす。本 change の中核機能が実行不能になっているため最優先
2. **🟠-2 `--readme-only` × 3d③ の交差を本文で確定する** (`SKILL.md:117` / `SKILL.md:175`)。(A) 報告のみ / (B) 例外許可 のどちらに倒すかはユーザー判断。(B) なら deviation.md への記録も要る
3. **🟡-3 6-② / 6-③ の対象に README の言語ペアを加える** (`SKILL.md:313` / `SKILL.md:340`)
4. **🟡-4 6-⑦ に identity-lint の実効範囲の注記を加える** (`SKILL.md:461-470`)
5. **🟡-5 Step 4 の提示例に項目番号を振る、または末尾文言を項目名指定へ改める** (`SKILL.md:183-209`)
6. **🔵-6〜8** (grep の空ガード / `docs/` パターンのネスト対応 / コメント英語統一の理由注記) は 1〜3 の修正時にまとめて反映すれば足りる

# レビュー結果: retarget-docs-refresh-to-skills (002 回目)

**日付**: 2026-08-26
**判定**: CHANGES_REQUESTED

## サマリー

前回サイクルの指摘は**全 11 件 (ホスト 8 + 相方採用 3) がすべて解消**していた。中核だった 6-① の偽陽性ループは「予定 manifest を一時ファイルへ書き出して検査入力にする」方式で解決しており、当レビュアがリポジトリ外の fixture で新規 concept・削除済み concept の両ケースを通しで再現し、ディスク manifest では `UNCOVERED` / `DELETED`、予定 manifest では `concepts coverage OK` になることを実測した。README ペア検査 (6-②③) の `language_pairs`、`docs/` 検出パターンの拡張 (裸参照・ネスト・複数ドット)、空 grep ガードも同様に実測で正例・負例とも確認済み。

一方で、修正で導入された新しい仕組みの周辺に 5 件の未解決が残る。最も重いのは **`--readme-only` 実行時の Step 6 の扱いが 6-① にしか書かれておらず、6-②〜⑧ の失敗が「再修正対象」として skills/ 本体の書き換えを招きうる**点で、これは確定済み相方 Major 2 の後半 (「Step 6 も README だけを再修正対象とし、8 検査の扱いを明示せよ」) が未反映のまま残ったもの。加えて、予定 manifest を配る `export` がブロックを跨いで持続しないため、8 検査のうち 6-① 以外は実際には旧 manifest を読む (実測確認済み)。いずれも 1〜2 文の明文化で解決するが、スキル文書は「書いてあるとおりに実行される」ことが唯一の担保なので、この状態では通せない。

## 前回指摘の解消状況

| # | 前回指摘 | 判定 | 根拠 |
|---|---|---|---|
| 1 | 🟠 6-① が旧 manifest を読み新規・削除 concept で必ず失敗 | **解消** | `SKILL.md:344-370` に予定 manifest の生成手順、`SKILL.md:145` に `DOCS_REFRESH_MANIFEST` による差し替え、`SKILL.md:388-396` に予定 manifest での再実行、`SKILL.md:625` に「Step 7 は予定 manifest と同一内容を書く」、Guardrails `SKILL.md:648`。fixture で新規 (`core/new.md`) + 削除 (`gone/old.md`) の同時ケースを通し、3c → `UNCOVERED` / `DELETED`、6-① → `concepts coverage OK` を実測 |
| 2 | 🟠 `--readme-only` × 3d③ の交差が未解決 | **解消 (deviation 準拠)** | deviation.md の確定 (報告のみ) が `SKILL.md:173` の 3d 冒頭に「要追従リストへは載せず**報告のみ**」として、`SKILL.md:639` の Step 8 に完了サマリ項目として反映済み。選択肢 A どおりで spec 本文の改変なし |
| 3 | 🟡 6-②/6-③ が README の言語ペアを外している | **解消** | `SKILL.md:407-417` / `SKILL.md:448-457` に `language_pairs()` を追加。fixture で `README.md` ↔ `README_ja.md` の見出し階層差・コードブロック差を両方とも検出、ペアの無い `samples/ios/README.md` は単独扱いでスキップされることも確認 |
| 4 | 🟡 6-⑦ identity-lint の実効範囲が無言 | **解消** | `SKILL.md:598` に実効範囲の注記 (README 群は素通り・`local-path-lint` は効く・拡張は別 change) |
| 5 | 🟡 Step 4 提示例に項目番号がない | **解消** | `SKILL.md:189-205` の全 5 項目に連番、`SKILL.md:215` は「項目番号 (例: 1, 4)」 |
| 6 | 🔵 空配列で grep が標準入力待ち | **一部解消** | 6-⑤ (`SKILL.md:545-553`) / 6-⑧ (`SKILL.md:606-612`) に空ガードあり。6-⑦ は未対応 → 新規指摘 5 |
| 7 | 🔵 `docs/` パターンがサブディレクトリを取りこぼす | **解消** | `SKILL.md:551` を `(^\|[^A-Za-z0-9_./-])docs/` へ変更。fixture 実測でネスト (`docs/guides/cells.md`)・複数ドット (`docs/a.b.md`)・裸参照 (`docs/`)・リンク内 (`[x](docs/x.md)`) を検出し、`kasane/concepts/docs/y.md` / `https://example.com/docs/` / `docs-refresh` は誤検出しない。相方 Minor 2 の指摘範囲も充足 |
| 8 | 🔵 コード例コメント英語統一の根拠 | **解消** | `SKILL.md:261-262` に括弧書きで理由 (6-③ 成立のための意図的例外・ソースコメント規約の対象外)。`cross/conventions/comment-policy.md` の適用範囲 (リポジトリ内の全ソースコード) とも矛盾しない |
| 相方 Major 3 | サブエージェント不在時のフォールバックが SHALL 2 件に違反 | **解消** | `SKILL.md:49` (コンテキスト節約方針)・`SKILL.md:227` (Step 5)・`SKILL.md:647` (Guardrails) の 3 箇所で「メインが代わりに読んで書くフォールバックを取らず停止」を明記 (停止タイミングの表現ゆれは新規指摘 6) |
| 相方 Minor 1 | README 委譲に適用できるプロンプトがない | **解消** | `SKILL.md:282-334` に 5b (README 単位テンプレート) を新設。対象ペア・取得元・検出差分・README 種別ごとの確認事項・内容規約 ③〜⑥ を含み、①② は Skill 本体規約として適用外と明示。spec の Requirement「生成プロンプトの内容規約」の Scenario は「Skill 更新の委譲プロンプト」を対象とするため、5b の ①② 除外は spec 違反にならない |
| 相方 Minor 2 | `docs/` 参照検査の検出漏れ | **解消** | 上記 7 と同じ |

## 指摘事項

### [🟠 Major] 1. `--readme-only` 実行時の Step 6 の扱いが 6-① にしかなく、6-②〜⑧ の失敗が skills/ 本体の書き換えを招く

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:340`、`SKILL.md:372-384`、`SKILL.md:396` (対応仕様: `specs/docs-refresh/spec.md:143`)

**問題点**:

`--readme-only` について本文が扱いを定めているのは 3d③ (`SKILL.md:173`、deviation 済み) と 6-① (`SKILL.md:396`) の 2 箇所だけである。ところが Step 6 の検査対象一覧 (`SKILL.md:375-383`) はモード分岐を持たず、**常に `targets` の全 Skill ファイル (en/ja) + `readmes`** を書き出す。その上で `SKILL.md:340` は「失敗した生成物は再修正対象に追加し、修正後に再度この一式を実行する」と定めているため、`--readme-only` 実行中に 6-②〜⑧ のいずれかが Skill ファイル側で失敗すると、実行者は skills/ を書き換えることになる。

これは次と正面から衝突する:

- `SKILL.md:56` (Input): 「skills/ 本体は検出・更新の対象にしない」
- spec Requirement「実行フラグ」: 「`--readme-only` は … skills/ 本体をスキップする SHALL」/ Scenario「skills/ 本体は検出・更新の対象にならない」
- deviation.md の確定方針 (「フラグの意味を優先」)

deviation.md が合意しているのは **3d③ の交差だけ**であり、Step 6 側は合意の対象外。相方レビュー (second-opinion-code-001.md の Major 2) も「Step 6 も README だけを再修正対象とし、Skill 固有検査は対象外 (N/A) とするなど、8 検査の扱いを明示せよ」と要求しており、その後半が未反映のまま残っている。

さらに実害として、この経路で skills/ を直しても `--readme-only` は `concepts` を更新しない (`SKILL.md:626`) ため、「skills/ 本体は触っていない」という manifest 側の前提と実ファイルの状態がずれる (前回 🟠-2 で指摘したのと同じ不整合)。

**推奨修正**: 6-① と同じ扱いを 6-②〜⑧ の Skill ファイル分にも広げる旨を Step 6 冒頭に 1〜2 文で明記する。例: 「`--readme-only` 実行時、Step 6 の検査対象のうち `targets` 由来の Skill ファイルに出た失敗は**報告のみ**とし、再修正対象に載せない (再修正対象は `readmes` 由来のファイルに限る)。次回の通常実行で処理される旨を完了サマリに添える」。あわせて `SKILL.md:375-383` の対象一覧生成に `--readme-only` の分岐 (readmes のみ出力) を置けば、6-⑤⑥⑦⑧ は自然に README だけを見るようになる。

---

### [🟡 Minor] 2. 予定 manifest を配る `export` はブロックを跨いで持続せず、6-② 〜 6-④ と対象一覧が旧 manifest へ黙って戻る

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:367` と `SKILL.md:377` / `SKILL.md:405` / `SKILL.md:446` / `SKILL.md:487`

**問題点**:

`SKILL.md:367` の `export DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-manifest-planned.json` は、予定 manifest 生成の heredoc と**同じコードブロック内**にある。以降の 4 スクリプト (対象一覧生成 `SKILL.md:375-383`、6-② `SKILL.md:403-436`、6-③ `SKILL.md:444-475`、6-④ `SKILL.md:483-536`) はいずれも別ブロックで、`os.environ.get("DOCS_REFRESH_MANIFEST", "skills/.manifest.json")` と**既定値つき**で読む。

エージェント実行環境ではコードブロックごとにシェルが分かれ、環境変数は持続しない。当レビュアの実測:

```
(1 回目の呼び出し) export KSN_PROBE=1  → set in this shell: 1
(2 回目の呼び出し) echo ${KSN_PROBE}   → next call sees: [<unset>]
```

したがって 6-② 〜 6-④ と対象一覧は**エラーを出さずディスクの旧 manifest へフォールバックする**。6-① だけがコマンド行にインラインで変数を付けている (`SKILL.md:391`) ため、意図どおり動くのは 8 検査中 1 つだけになる。

影響範囲は「この実行で `targets` にキーが増えた場合」(新規 concept の配置で新しい references ファイルを作った場合) に限られるが、そのとき新設ファイルは 6-②〜⑧ の検査を丸ごと素通りする。本文が Guardrails (`SKILL.md:648`) で「Step 6 の検査はディスクの manifest ではなく予定 manifest に対して行う」と宣言している以上、手順がその宣言を満たしていないのは問題である。

**推奨修正**: 6-① と同じくコマンド行へインラインで渡す (`DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-manifest-planned.json python3 - <<'PY' …`) 形へ 4 箇所とも揃える。`export` を残す場合は「Step 6 の全コマンドを 1 シェルセッションで実行すること (ブロックを分けると `export` が失われる)」と注記する。

---

### [🟡 Minor] 3. `--all` 経路は 3c をスキップするのに 6-① を実行するため、未配置 concept があると前回 🟠-1 と同型のループに戻る

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:119`、`SKILL.md:340`、`SKILL.md:386-396`

**問題点**:

`SKILL.md:119` は「`--all` 指定時は 3a〜3c を**スキップ**し」と定める。よって `--all` 実行では網羅検査 (3c) が走らず、未参照・未除外 concept があってもユーザーへの配置判断提示 (Step 4) が発生しない。その結果、Step 4 で承認される配置判断が無いまま予定 manifest はディスク manifest と同一になり、6-① は `UNCOVERED` を出す。

このとき本文の指示は `SKILL.md:340` の「失敗した生成物は再修正対象に追加し、修正後に再度この一式を実行する」しかない。原因は生成物ではないので直しようがなく、前回 🟠-1 とまったく同型の抜け出せないループになる (`--readme-only` は `SKILL.md:396` で報告のみに落としてあるが、`--all` には対応する記述がない)。

**推奨修正**: どちらかを本文で確定する。(A) `--all` でも 3c は実行する (spec「実行フラグ」が `--all` にスキップさせているのは**差分検出** = ハッシュ比較であり、ハッシュに依存しない網羅検査まで外す必要はない。全再生成こそ配置漏れを潰す好機でもある)、または (B) `--all` 実行時の 6-① の `UNCOVERED` は再修正対象ではなく**配置判断の依頼**としてユーザーへ提示する、と明記する。

---

### [🟡 Minor] 4. 削除済み concept が `concepts` から落ちないため、Step 3b が毎回同じ削除を再検出する

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:133` と `SKILL.md:621-627`

**問題点**:

Step 3b の「削除済み」判定は manifest の `concepts` (ハッシュ表) を基準にする (`SKILL.md:133`: 「manifest にあるがファイルが存在しない concept」)。一方 Step 7 のハッシュ更新規則 (`SKILL.md:623-627`) は「現在値へ更新する対象」「旧ハッシュを保持する対象」「新規を載せない条件」しか定めておらず、**削除済み concept のエントリを `concepts` から取り除く規則がない**。予定 manifest の生成スクリプト (`SKILL.md:360-363`) も `targets` / `excluded` からしか落とさない。

結果、削除を一度きちんと処理し終えても、次回以降の実行で 3b が同じ concept を「削除済み」として検出し続け、Step 4 の提示に「削除済み concept: 1 件」が残り続ける。6-① は `targets` / `excluded` を見るので通るため、失敗ループにはならないが、実行者が「毎回出るので無視してよい」と学習する型の劣化であり、前回 🟠-1 で嫌ったのと同じ性質を持つ。

なお spec Requirement「manifest v3 スキーマの規範」の Scenario は `targets` / `excluded` からの除去しか要求していないため、これは spec 違反ではなく本文の運用上の穴である。

**推奨修正**: `SKILL.md:621-627` のハッシュ更新規則に 1 行足す。例: 「削除済みと判定され整理が承認された concept は、`targets` / `excluded` だけでなく `concepts` からもエントリを取り除く」。

---

### [🔵 Suggestion] 5. 6-⑦ は対象一覧が空のとき、両 lint が「リポジトリ全体を検査」へ黙って切り替わる

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:592-593`

**問題点**: 前回 🔵-6 の空ガードは 6-⑤ / 6-⑧ に入ったが、6-⑦ の `python3 scripts/local-path-lint.py --paths $(cat /tmp/docs-refresh-targets.txt)` は据え置きになっている。対象一覧が空だとコマンド置換も空になり、`--paths` の後に何も続かないため `local-path-lint.py:288-291` は `paths = []` を受け取り、`local-path-lint.py:193` の `cmd += paths if paths else ["."]` でリポジトリ全体を走査する (`identity-lint.py:353-355` も同じ形)。grep のように固まりはしないが、「対象 0 件なのに全体検査の結果が返る」ため、失敗の原因を取り違えやすい。

**推奨修正**: 6-⑤ / 6-⑧ と同じ空ガード (`[ -s /tmp/docs-refresh-targets.txt ] || { echo "検査対象が空です"; }` 相当) を 6-⑦ の直前にも置く。

---

### [🔵 Suggestion] 6. 委譲不能時の停止タイミングが本文で 2 通りに読める

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:49` と `SKILL.md:227` (および Guardrails `SKILL.md:647`)

**問題点**: `SKILL.md:49` は「**Step 4 の承認提示**と skills/・README 群・manifest への書き込みを行わないまま … 停止する」とし、Guardrails も「承認・書き込みの前に実行不能として停止する」と書く。一方 `SKILL.md:227` は Step 5 (承認後) の記述として「委譲自体ができない環境では … 停止する」と置かれている。書き込みが起きない点は 3 箇所とも一致しているので実害は小さいが、委譲可否をいつ判定するのかが本文から一意に読めない。

**推奨修正**: 委譲機構の可否判定を Step 1 (プロジェクトの状態取得) か Step 2 の直後に置くと 1 文で明示し、`SKILL.md:227` は「(Step 1 で見落とした場合の最終防波堤)」と位置づけを添える。

---

## 確認した観点 (指摘なし)

- **前回指摘の再検証**: ホスト 8 件 + 相方採用 3 件を 1 件ずつ本文で追跡 (上表)。取りこぼしなし
- **deviation.md の反映**: `--readme-only` × 3d③ は確定内容 (報告のみ) が `SKILL.md:173` と `SKILL.md:639` に正しく反映。deviation 記録済みの差分として違反扱いしていない
- **検査スクリプト断片の再実測**: 予定 manifest 生成 (`SKILL.md:347-365`) / 6-① / 6-② / 6-③ / 6-⑤ の `docs/` パターンを、リポジトリ外の一時 fixture で正例・負例とも実行。新規 concept + 削除済み concept の同時ケースを通しで再現し、予定 manifest 経由で `concepts coverage OK` に転じることを確認。`language_pairs()` の `_ja` ペアリング規則 (`p[:-3] + "_ja.md"`)、ペア無し README のスキップも実測。fixture は scratchpad に置き、実行後 `trash` で破棄 (リポジトリ内・`/tmp` に残骸なし)
- **manifest v3 規範 JSON**: spec `specs/docs-refresh/spec.md:27-37` と `SKILL.md:99-109` が **byte 一致** (機械比較 `True`)。プレースホルダ置換後の `json.loads` も成功 (7 キー)
- **5b (README テンプレート) の妥当性**: 内容規約 ③〜⑥ を持ち、①② の非適用理由を明示。`--readme-only` で使うのが 5b だけであることも明記されており、deviation の確定方針と整合
- **AGENTS.md / config.yaml**: 前回から変更なし。`docs/` ディレクトリへの言及ゼロ・凍結注記なし、`lint.exclude` の `docs/` と `identity.scope` の `docs` は Non-Goal どおり据え置き、`scope` への `skills` 追加あり
- **足場の凍結**: `git status` 上 proposal.md / specs/ に変更なし (逆流なし)
- **tasks.md**: 19 タスクすべてに対応する成果物が実在。虚偽チェックなし
- **lint**: `scripts/local-path-lint.py` / `scripts/identity-lint.py` を変更・追加ファイル 5 件 (SKILL.md / AGENTS.md / config.yaml / tasks.md / deviation.md) に対して実行、いずれも exit 0
- **ビルド・テスト**: 製品コードへの変更がないため製品テストスイートは非該当。代替検証は上記の fixture 実行と lint (詳細は [verify-002.md](verify-002.md))
- **concepts の conventions**: `cross/conventions/comment-policy.md` (適用範囲はリポジトリ内の全ソースコードのコメント) と `SKILL.md:261-262` の例外注記は矛盾しない。`cross/conventions/public-identifiers.md` と 6-⑧ のパターンは前回どおり整合
- **lessons/code-review.md**: 重点観点 L-001 の趣旨に沿い、静的読解ではなく fixture 実測 (正例・負例) で判定した。「指摘しないこと」に該当する型の指摘は含まない

## アクションプラン

1. **🟠-1 `--readme-only` 実行時の Step 6 の扱いを明記する** (`SKILL.md:340` / `SKILL.md:375-383`)。確定済み相方 Major 2 の未反映部分であり、spec「実行フラグ」の SHALL と衝突するため最優先
2. **🟡-2 予定 manifest の受け渡しを `export` からインライン指定へ揃える** (`SKILL.md:367` / `:375` / `:403` / `:444` / `:483`)。前回 🟠-1 の修正が 8 検査中 1 つにしか効いていない
3. **🟡-3 `--all` 経路の 6-① の扱いを確定する** (`SKILL.md:119` / `SKILL.md:386-396`)
4. **🟡-4 Step 7 のハッシュ更新規則に削除済み concept のエントリ除去を足す** (`SKILL.md:621-627`)
5. **🔵-5, 🔵-6** (6-⑦ の空ガード / 委譲可否の判定タイミング) は 1〜4 の修正時にまとめて反映すれば足りる

# レビュー結果: retarget-docs-refresh-to-skills (003 回目)

**日付**: 2026-08-26
**判定**: APPROVED

## サマリー

review-002 の 6 件 (Major 1 / Minor 3 / Suggestion 2) はすべて解消していた。中核だった Major-1 は `SKILL.md:350-365` に「`--readme-only` 実行時のモード分岐」節が新設され、8 検査それぞれの扱いを表で確定・再修正対象を `readmes` 由来に限定・対象一覧生成へのモード分岐 (`DOCS_REFRESH_README_ONLY`) の 3 点すべてが入っている。当レビュアがリポジトリ外の fixture で `--readme-only` 経路を通し、対象一覧が `readmes` だけになること、6-②/6-③ が Skill ペアの負例を素通しし README ペアの負例は従来どおり検出することを実測した。Minor-2 の `export` 廃止は 5 スクリプト全部がインライン指定へ揃っており、Minor-3・Minor-4・Suggestion-5・Suggestion-6 も本文・Guardrails の両方に反映済み。

新規の矛盾・実行不能手順は見つからなかった。埋め込みスクリプトは python heredoc 全件が `compile()` を通り、bash ブロック 12 件が bash / zsh 両方で `-n` を通る。残る指摘は 🔵 2 件のみで、いずれも skills/ を書き換える経路には繋がらない (どちらも失敗時の見え方の問題)。よって **APPROVED**。

## 前回指摘の解消状況

| # | 前回指摘 | 判定 | 根拠 |
|---|---|---|---|
| 🟠-1 | `--readme-only` 時の Step 6 の扱いが 6-① にしかない | **解消** | `SKILL.md:350-365` に専用節。①〜⑧ の扱いを表で確定 (`:354-363`)、再修正対象は `readmes` 由来に限定 (`:352`)、対象一覧生成に `DOCS_REFRESH_README_ONLY` 分岐 (`:397-411`)、6-②/6-③ のスクリプト側分岐 (`:433`/`:437`、`:475`/`:478`)、Guardrails (`:685`)、完了サマリ (`:674`)。fixture で対象一覧が `readmes` のみになること、6-②/6-③ が Skill ペア負例 (見出し階層 `[1,2]` vs `[1,3]`・コードブロック差) を `--readme-only` では出さず README ペア負例は出すことを実測 |
| 🟡-2 | 予定 manifest を配る `export` がブロックを跨がない | **解消** | `SKILL.md:393` に「環境変数はコマンド行にインラインで渡す」+ 付け忘れ時の黙ったフォールバックの警告。対象一覧生成 (`:398`)・6-① (`:418`)・6-② (`:430`)・6-③ (`:472`)・6-④ (`:512`) の 5 箇所すべてが `DOCS_REFRESH_MANIFEST=… python3 - <<'PY'` 形。予定 manifest 生成ブロック (`:370`) から `export` 行が消えていることも確認。Guardrails (`:684`) にも明記 |
| 🟡-3 | `--all` が 3c をスキップして 6-① と噛み合わない | **解消 (選択肢 A)** | `SKILL.md:57` (Input)・`:123` (Step 3 冒頭、理由つき)・`:695` (Guardrails) の 3 箇所で「`--all` でスキップするのはハッシュ差分検出 (3a・3b) のみ、網羅検査 (3c) は実行する」に統一 |
| 🟡-4 | 削除済み concept が `concepts` から落ちない | **解消** | `SKILL.md:661` に除去規則を追加。かつ同じ行の後段で「整理が未承認・未完了の削除済み concept は `concepts` に残す」と書き分け、`:659` の「未処理の concept は旧ハッシュを保持」と両立している (両立の条件が明示されており、どちらの規則が効くかが一意に読める) |
| 🔵-5 | 6-⑦ に空ガードがない | **解消** | `SKILL.md:620-627` に `[ ! -s /tmp/docs-refresh-targets.txt ]` ガード。`:631` に両 lint の空時の挙動差 (local-path-lint は全体走査へ切替・identity-lint は 0 件で正常終了) を注記。zsh で空ケースを実測し、lint が起動しないことを確認 |
| 🔵-6 | 委譲不能時の停止タイミングが 2 通りに読める | **解消** | `SKILL.md:51` に「可否の判定は起動時 (Step 1) に行う」を独立段落で追加、`:65` で Step 1 の先頭作業として指示、`:233` は「Step 1 で見落とした場合の最終防波堤」と位置づけを明記、Guardrails `:683` も「Step 1 の起動時判定で」に統一。前回指摘の推奨修正どおり |

## 指摘事項

### [🔵 Suggestion] 1. モード分岐表 ② 行の括弧書きが事実と食い違う (②③ は対象一覧を読まない)

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:357`、`SKILL.md:365`

**問題点**:

`SKILL.md:357` の ② 行は `--readme-only` 欄に「**`readmes` の言語ペアのみ** (対象一覧の生成が絞られるため自動的にそうなる)」と書く。`SKILL.md:365` の 1 文目も「②③⑤⑥⑦⑧ の対象の絞り込みは、下の検査対象一覧生成が `--readme-only` で `readmes` だけを書き出すことで機械的に担保される」と ②③ を含めている。

しかし 6-② / 6-③ のスクリプト (`SKILL.md:430-464` / `SKILL.md:472-505`) は `/tmp/docs-refresh-targets.txt` を一切読まず、manifest から `language_pairs()` で対象を組み立てる。対象一覧が `readmes` だけになっても ②③ の対象は絞られない — 絞るのは `DOCS_REFRESH_README_ONLY=1` を渡したときの `README_ONLY` 分岐 (`SKILL.md:437` / `:478`) だけである。fixture で確認済み (フラグ無しでは Skill ペアの負例が出る)。

`SKILL.md:365` の 2 文目と 6-② 本文 (`SKILL.md:427`) が正しい指示 (「スクリプト側でもモードを渡して `targets` 分を外す」「コマンド行に `DOCS_REFRESH_README_ONLY=1` も付けて起動し」) を出しているので、フラグを渡し忘れても skills/ が書き換わることはない (`SKILL.md:352` により Skill 側の失敗は報告のみ)。実害は完了サマリに不要な報告が並ぶことに留まるが、規範表の中に誤った因果説明が残っているのは望ましくない。

**推奨修正**: `SKILL.md:357` の括弧書きを「(コマンド行に `DOCS_REFRESH_README_ONLY=1` を渡して `targets` 分を外す)」へ、`SKILL.md:365` の 1 文目の対象を「⑤⑥⑦⑧」へ直す (②③ は 2 文目が担当している旨をそのまま残す)。

---

### [🔵 Suggestion] 2. 6-⑥ は対象一覧が空のとき偽の「All internal links resolve」を返す

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:593-614`

**問題点**:

🔵-5 の空ガードは 6-⑦ に入ったが、6-⑥ (`SKILL.md:594-613`) は据え置きのまま `/tmp/docs-refresh-targets.txt` を直接 `open()` する。当レビュアの実測では、空ファイルを渡すと `targets` が空リストになり、`issues` も空のまま **`All internal links resolve` を出力して終わる** (検査したように見えて 1 ファイルも見ていない)。ファイル自体が無い場合は `FileNotFoundError` で落ちる。

6-⑤ / 6-⑦ / 6-⑧ が空ケースで明示的なメッセージを出すようになった今、6-⑥ だけが「緑の結果」を返すのは検査結果の読み違いを誘う (6-⑤ が先に鳴るので気づけはするが、8 検査の結果一覧を並べたときに ⑥ だけ OK と読める)。

**推奨修正**: 6-⑤ / 6-⑦ / 6-⑧ と同じ空チェックを 6-⑥ の直前にも置くか、python 側で `if not targets: print("検査対象が空です …")` と分岐する。

---

## 確認した観点 (指摘なし)

- **前回指摘の再検証**: review-002 の 6 件を 1 件ずつ本文で追跡 (上表)。1 周目 (review-001 / second-opinion-code-001 の計 11 件) の解消状態が退行していないかも再確認 — `docs/` 検出パターン (`SKILL.md:580`) はネスト (`docs/guides/a.md`)・複数ドット (`docs/a.b.md`)・裸参照 (`docs/`)・リンク内 (`[x](docs/x.md)`) を検出し、`kasane/concepts/docs/y.md` / `https://example.com/docs/` / `docs-refresh` を誤検出しないことを再実測。`language_pairs()` のペアリング規則 (`p[:-len(".md")] + "_ja.md"`) とペア無し README のスキップも再実測。退行なし
- **deviation.md の反映**: `--readme-only` × 3d③ の確定 (報告のみ) は `SKILL.md:179` と `SKILL.md:675` に据え置きで反映。合意済み差分として違反扱いしていない。今回の Step 6 モード分岐は同じ方針を Step 6 へ展開したもので、deviation の追加合意を必要としない (spec Requirement「実行フラグ」の SHALL 側へ寄せる修正であり、spec との乖離が減る方向)
- **R9 (8 検査) と R10 (`--readme-only`) の緊張関係**: 6-④ を `--readme-only` で N/A とし、②③⑤⑥⑦⑧ を README に絞る扱いは、R9 の主語が「失敗した**生成物**」であることに照らして faithful な読み (`--readme-only` の生成物は README のみで、Skill の `SKILL.md` はこの実行の生成物ではない)。未記録乖離としては扱わない
- **スクリプトの構文・ロジック**: SKILL.md 埋め込みの python heredoc を全件 `compile()` (6-① の「Step 3c と同一のスクリプト」プレースホルダを除き全て構文 OK)、bash ブロック 12 件を bash / zsh 両方で `bash -n` / `zsh -n` (全件 OK)。予定 manifest 生成ブロックは `export` 削除後も単体で走り、7 キーの JSON を出力することを実測
- **`--readme-only` 経路の通し確認**: fixture (Skill ペアに見出し階層差とコードブロック差を仕込み、README ペアは一致させた状態) で、対象一覧が `readmes` 3 件のみ / 6-② `en/ja heading structure OK` / 6-③ `code blocks byte-identical` を確認。次に README ペア側を壊すと `README.md: heading levels differ en=[1, 2] ja=[1, 3]` / `README.md: code block #1 differs` を検出。fixture はリポジトリ外の scratchpad に置き、実行後 `trash` で破棄 (リポジトリ内・`/tmp` に残骸なし。`git status` で新規ファイルが増えていないことも確認)
- **manifest v3 規範 JSON**: `specs/docs-refresh/spec.md:27-37` と `SKILL.md:103-113` が **byte 一致** (機械比較 `True`)。不変条件 ①②③ (`SKILL.md:115-119`) も spec:39 と一致
- **足場の凍結**: `git status` 上 `M` は `.agents/skills/docs-refresh/SKILL.md` / `AGENTS.md` / `kasane/config.yaml` / change の `tasks.md` のみ。proposal.md / specs/ に変更なし → 逆流なし
- **AGENTS.md / config.yaml**: 前サイクルから変更なし。AGENTS.md は `docs/` 言及ゼロ・凍結注記なし、config.yaml は `context` の skills/ ベース書き換えと `lint.identity.scope` への `skills` 追加のみで、`lint.exclude` の `docs/` は Non-Goal どおり据え置き
- **tasks.md**: 19 タスクすべてに対応する成果物が実在。虚偽チェックなし
- **lint**: `scripts/local-path-lint.py` / `scripts/identity-lint.py` を変更・追加ファイル 5 件 (SKILL.md / AGENTS.md / config.yaml / tasks.md / deviation.md) に実行、いずれも exit 0
- **ビルド・テスト**: 製品コードへの変更がないため製品テストスイートは非該当。代替検証は上記のスクリプト実行と lint (詳細は [verify-003.md](verify-003.md))
- **concepts の conventions**: `cross/conventions/comment-policy.md` (適用範囲はリポジトリ内のソースコメント) と `SKILL.md:265-268` のコード例コメント例外は矛盾しない。`cross/conventions/public-identifiers.md` と 6-⑧ のパターンも整合
- **lessons/code-review.md**: 重点観点 L-001 の趣旨に沿い、モード分岐の効き目を静的読解ではなく fixture の正例・負例で実測して判定した。「指摘しないこと」に該当する型の指摘は含まない

## アクションプラン

1. **🔵-1 / 🔵-2 は任意**。いずれも skills/ の誤書き換えには繋がらず、判定を左右しない。次に本文へ手を入れる機会 (蒸留時の微修正など) にまとめて反映すれば足りる
2. それ以外の未解決事項なし。蒸留 (ksn-distill) へ進んでよい状態と判断する

---

## 追記: 🔵 2 件の修正確認 (2026-08-26)

オーケストレーターが 🔵-1 / 🔵-2 を直接修正したとの申告を受け、独立に確認した。**両件とも解消。判定 APPROVED は据え置き** (新規指摘なし)。変更は `.agents/skills/docs-refresh/SKILL.md` のみで、`AGENTS.md` / `kasane/config.yaml` / `tasks.md` の diff 行数はレビュー時と同一 (`4 +--` / `7 ++--` / `38 +++---`)、`git status` 上の新規ファイルも増えていない。

### 🔵-1 (モード分岐表 ② 行の因果説明) — 解消

- `SKILL.md:357` の括弧書きが「(スクリプトに `DOCS_REFRESH_README_ONLY=1` を渡して `targets` 分を外す)」へ訂正され、実装機構 (`SKILL.md:437` / `:478` の `README_ONLY` 分岐) と一致した
- `SKILL.md:365` が「⑤⑥⑦⑧ は対象一覧生成で機械的に担保」「②③ は対象一覧ファイルを読まず `language_pairs()` が manifest の `targets` を直接参照するため絞られない → 必ず `DOCS_REFRESH_README_ONLY=1` を渡す」と書き分けられた
- **記述の裏取り**: `/tmp/docs-refresh-targets.txt` を読むのは 6-⑤ (`:573`)・6-⑥ (`:596`)・6-⑦ (`:624`,`:627-628`)・6-⑧ (`:643`) の 4 検査のみで、6-②/6-③ は読まない。よって新しい 2 文はいずれも事実と一致する (①④ は表側で「報告のみ」「N/A」に落ちており、この段落の主張範囲に含めていないのも正しい)
- 旧記述の残存なし (`自動的にそうなる` / `対象一覧の生成が絞られる` の grep でヒット 0)
- 表 ③ 行の「同上」は訂正後の ② 行を指すため整合。6-③ 本文 (`:427` の「6-③ も同じ」) とも矛盾しない

### 🔵-2 (6-⑥ の空ガード) — 解消

`SKILL.md:594-616` の python 断片冒頭に空ガードが入った。SKILL.md から断片をそのまま抽出して実行し、3 ケースを実測:

| ケース | 結果 |
|---|---|
| A: 対象一覧が空 | `検査対象が空です (manifest の targets / readmes を確認してから再実行)` — **偽の「All internal links resolve」は出ない** |
| B: 正例 (解決するリンク) | `All internal links resolve` |
| C: 負例 (壊れたリンク + 外部 URL) | `a.md: [x](missing.md) -> missing.md MISSING` (外部 URL は正しくスキップ) |

指摘の趣旨 (空対象時に緑の結果を返さない) を満たし、既存の検出ロジックにも退行がない。メッセージ文言も 6-⑤ / 6-⑦ / 6-⑧ の空ガードと同一で、8 検査の結果一覧としての読み味が揃った。`raise SystemExit(0)` による exit 0 は他 7 検査 (いずれも出力テキストで報告し exit code を判定に使わない) と同じ扱いで、`SKILL.md:346` の「失敗した生成物は再修正対象に追加」運用と整合する。

### 再検査 (退行確認)

- python heredoc 7 件 (6-① のプレースホルダ含む) が全件 `compile()` 通過
- bash ブロック 12 件 × bash / zsh の `-n` 構文チェック、エラー 0
- `scripts/local-path-lint.py` / `scripts/identity-lint.py` を `SKILL.md` に実行、両方 exit 0
- fixture はリポジトリ外の scratchpad に置き `trash` で破棄、`/tmp/docs-refresh-targets.txt` も破棄 (残骸なし)

**verify-003.md の判定 (VALID) にも影響なし** — 今回の修正は R9-⑥ (内部リンク解決) の記述と空ケースの挙動、および Step 6 モード分岐節の説明文の精度を上げるもので、Requirement / Scenario の対応関係は変わらない。

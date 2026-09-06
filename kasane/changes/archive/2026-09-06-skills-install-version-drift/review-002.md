# レビュー結果: skills-install-version-drift (002 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

review-001 の Minor 3 件はいずれも解消を確認した。workflow の step 名とコメントは README 限定でない表現に改まり、`kasane/handbook/cross/release-procedure.md` の `timestamp` は 2026-09-06 に、`--selftest` には「`from:` のままなら version 一致でも `--check` が落ちる」節と「対象ファイルが無ければ何も書き換えず失敗する」節 (Suggestion 1 も同時に消化) が足された。selftest は 26 件から 33 件に増え、追加分が実際に回帰検出力を持つことをミューテーション 3 種で実測した。

残るのは、workflow 2 ファイルへの追随修正が deviation.md に付随修正として記録されていないという記録上の 1 件 (Minor、低優先度) と、Suggestion 3 件。いずれも機能に影響せず、この change をアーカイブ工程へ進めることを妨げない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加された script のコメント・docstring・selftest 用 fixture に作業文書パス / 変更識別子 / 仮称の混入なし。`comment-policy-lint.py --advisory` は禁止 0 件 (要確認 173 件はすべて `samples/` の既存債務で、review-001 時点から増減なし)
- `kasane/handbook/cross/user-skill-api-listing.md` (きっかけ: `skills/**` を触る) — 前回から `skills/` の差分は増えていない (4 枚 × 1 行のまま)。掲載 API の増減なし
- `kasane/handbook/cross/release-procedure.md` (きっかけ: リリース手順の記述を触る) — 手順 2・注意書き・`timestamp` の 3 箇所を節ごとに照合。必須 status check が「`.github/workflows/ci.yml` の job 名と一致」(`:32`) と定義されている点も、下記の step 名変更の影響判定に使用
- `kasane/handbook/cross/test-execution.md` (きっかけ: テストを実行し結果を報告する) — platform ソースに触れないため iOS / Android / MAUI のテストは対象外。実行件数を下表に併記
- `kasane/decisions/cross/0022-user-docs-as-agent-skills.md` (accepted) — 「`skills/` は手で直接育てず docs-refresh が追従させる」に対し、本 change は機械置換という第 2 の書き込み経路を足す。exploration.md 決定事項で「例外規定は README と同じく AGENTS.md 側に置き ADR-0022 は改訂しない」と合意済みであり、AGENTS.md:17 がその通りに広がっていることを確認した (合意済みスコープ内)
- `kasane/lessons/process.md` / `kasane/lessons/code-review.md` — L-005 (到達可能な修正はサイクル内で閉じる) を前回指摘の解消判定に、code-review L-001 (静的読解で止めずミューテーションで実測) を追加 selftest の検出力確認に適用。「指摘しないこと」は昇格済みルールなし

## 実行した検証

| 検証 | 結果 |
|---|---|
| `python3 scripts/release/set-readme-version.py --selftest` | 33 件 OK / 0 件 NG (「失敗なし」)。前回 26 件から 7 件増 (`[SwiftPM が from: のままの場合]` 3 件、`[対象ファイルが無い場合]` 4 件) |
| `python3 scripts/release/set-readme-version.py --check 0.1.0-beta.1` | exit 0、「インストール例 14 行が 0.1.0-beta.1 と一致する」 |
| `python3 .agents/skills/docs-refresh/scripts/code-block-parity-check.py` | `code blocks byte-identical` |
| `python3 scripts/comment-policy-lint.py --advisory` | 禁止 0 件 / 要確認 173 件 (前回と同数、本 diff 由来なし) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 検出 0 件 |
| `python3 scripts/doc-structure-lint.py --verbose` | `kasane/handbook/cross/release-procedure.md` は違反リストに無し |
| `python3 scripts/readme-example-lint.py` | 「README の最小例 4 件が消費者検証のソースと一致する」 |
| ミューテーション実測 (一時コピーに対して実施。リポジトリの `scripts/release/set-readme-version.py` は shasum 一致で無改変を確認) | ①`check()` の `keyword != "exact"` 分岐を無効化 → 新設 `[SwiftPM が from: のままの場合]` が 2 件 NG。②`collect()` の欠落ファイル報告を黙殺 → 新設 `[対象ファイルが無い場合]` が 3 件 NG。③`collect()` でファイルごとの期待種別を無視し全ファイルに 3 種を要求 → `[Skill ファイルは自分の種別だけを見る]` を含む 10 件が NG。追加分がいずれも実際の検出力を持つことを確認 |

### review-001 指摘の対応状況

| 前回指摘 | 対応 | 確認内容 |
|---|---|---|
| Minor 1 (workflow の記述が README 限定) | 解消 | `.github/workflows/release.yml:152` が `Verify install examples (README + Skills)`、`.github/workflows/ci.yml:187` が `Install example version script selftest`、`ci.yml:182` のコメントが `README / Skill のインストール例の置換は一時コピーに対して行う` に。step 名は `lint` job / `validate` job の内側であり、`release-procedure.md:32` が定める必須 status check (ci.yml の **job** 名 = ios / android / maui / consumer-ios / consumer-android / consumer-maui / lint の 7 件) には現れないため、branch protection の設定は無変更で通る |
| Minor 2 (handbook の timestamp) | 解消 | `kasane/handbook/cross/release-procedure.md:8` が `timestamp: 2026-09-06` |
| Minor 3 (`from:` のままの `check` が未検査) | 解消 | `scripts/release/set-readme-version.py:466-491`。置換後のツリーで iOS Skill 1 枚だけを `from:` へ戻し、exit 1 / `SKILL.md:7` の行番号 / `exact: で書く` / 「version の不一致としては報告しない」の 4 点を押さえている。version を一致させたまま解決方法の語だけを争点にした構成で、分岐を狙い撃ちできている |
| Suggestion 1 (「ファイルが無い」経路が未検査) | 解消 | `scripts/release/set-readme-version.py:492-509`。`replace` の exit 1・欠落ファイル名の出力・他ファイル無改変に加え、`check` 側も失敗することまで押さえている |
| Suggestion 2 (iOS Skill の prerelease 説明) | 未対処 (既知の申し送り) | 本 change では触らず docs-refresh の責務とする方針を踏襲。下記 Suggestion に再掲する |

その他の回帰確認: `TARGET_FILES` (`:70-81`) と `TOTAL_TARGET_LINES` (`:84`) は前回から無変更で対象 10 ファイル / 14 行を維持。`AGENTS.md:17` の例外規定は「README 2 枚 × 3 行と Skill 8 枚 × 1 行」で決定事項どおり (`CLAUDE.md` は `AGENTS.md` への symlink なので二重管理は発生しない)。`skills/` の差分は 4 枚 × 1 行のままで、`skills/.manifest.json` の `lastUpdatedFiles` は未更新 (決定事項どおり)。`--check` / `--selftest` の引数形と終了コードも不変で、`release.yml:155` / `ci.yml:188` の呼び出しはそのまま通る。

## 指摘事項

### [🟡 Minor] workflow 2 ファイルへの追随修正が deviation.md に記録されていない

**該当箇所**: `kasane/changes/skills-install-version-drift/deviation.md:3`

**問題点**: `.github/workflows/release.yml` と `.github/workflows/ci.yml` の step 名 / コメント修正は、exploration.md の決定事項が挙げる対象 (script 1 本・AGENTS.md の規約文・置換結果の 8 行) に含まれないスコープ外の付随修正である。ksn-core の delta-spec 規約は「付随修正も deviation.md に `- [付随修正] <箇所>: <何を直したか>。理由: <一言> (YYYY-MM-DD)` の形式で記録する」と定めており、同じ性質の `kasane/handbook/cross/release-procedure.md` の文言修正は 1 件目として記録されているのに、workflow 側だけが記録されていない。deviation.md は change と共にアーカイブされて後から「どこまでが合意済みスコープか」を示す証跡になるため、同種の修正が片方だけ落ちていると読み手が判断できない。

なお修正内容そのものに異論はない (review-001 の推奨修正どおりで、同梱条件も満たしている)。欠けているのは記録だけなので優先度は低く、実装のやり直しは不要。

**推奨修正**: deviation.md に 2 件目として追記する。例: `- [付随修正] `.github/workflows/release.yml` の validate step 名と `.github/workflows/ci.yml` の selftest step 名 / 直前のコメント: script の検査・置換対象が README 2 枚から 10 ファイルへ広がり、README だけを指す表現が実態より狭くなったため「インストール例 (README + Skills)」を含む表現に改めた。独立レビュー (review-001 Minor 1) の指摘を受け、本 change が直接原因で 2 ファイル計 3 行で閉じるためサイクル内で修正 (2026-09-06)`

### [🔵 Suggestion] ci.yml の追記でコメントブロックの折り返し幅が崩れている

**該当箇所**: `.github/workflows/ci.yml:182`

**問題点**: 3 行のコメントブロックのうち 2 行目だけが表示幅 103 桁になり、前後の行 (82 桁 / 60 桁) と、直上の別ブロック (85 桁 / 78 桁) が守っている 80〜85 桁の折り返しから外れた。既存行に語を挿し込んだ結果で、内容は正しい。

**推奨修正**: 3 行を 80〜85 桁で折り直す (例: 「差し替わり、README / Skill のインストール例の置換は一時コピーに対して行うので、」/「ネットワークにも作業ツリーにも触らない。bash と python3 だけで回る。」)。優先度は低い。

### [🔵 Suggestion] ADR-0020 の帰結記述が README 限定のまま

**該当箇所**: `kasane/decisions/cross/0020-release-dispatch-tag-last-version-injection.md:52`

**問題点**: 「README のインストール例は具体 version を書き、リリース PR の中で専用 script (`scripts/release/set-readme-version.py`) が置換し、validate が一致を検査する」という帰結は、本 change の後は利用者向け Skill 8 枚にも当てはまる。ADR の決定 (ビルドの version 宣言に bump コミットを積まない) とは矛盾せず、記述が実態より狭いだけなので accepted ADR への違反ではない。ただしこの ADR は日付つきの追記を重ねる形で運用されており (`2026-09-02 追記` / `2026-09-04` の帰結節)、放置すると ksn-drift の意味検証で拾われる。

**推奨修正**: 本 change では触らない (ADR の改訂は explore / propose の権利)。蒸留 (ksn-distill) の際に、この 1 行へ Skill 8 枚を含める追記を行うかどうかを判断材料として渡す。

### [🔵 Suggestion] iOS Skill 導入節の prerelease 説明 (review-001 Suggestion 2 の再掲)

**該当箇所**: `skills/en/kssettingsview-ios/SKILL.md:30-40`、`skills/ja/kssettingsview-ios/SKILL.md:30-40`

**問題点**: 例が `exact: "0.1.0-beta.1"` に変わった一方、なぜ `exact:` なのか (prerelease は明示指定でしか解決されない) の説明が Skill 側に無い状態は前回から変わっていない。`README.md:48` の prerelease 説明と script docstring の食い違いも同様。いずれも本 change の合意済みスコープ外で、方針として docs-refresh へ送ることが前回確定している。

**推奨修正**: 本 change では対処しない。次回の docs-refresh 依頼への申し送りとして残す (簡易起票でも可)。

## アクションプラン

1. deviation.md に workflow 2 ファイルの付随修正を 1 行追記する (Minor、アーカイブ前に済ませる。実装のやり直しは不要)
2. 余力があれば `.github/workflows/ci.yml:182` のコメントを折り直す (Suggestion)
3. 蒸留時に ADR-0020:52 の記述範囲を Skill 8 枚へ広げるか判断する (Suggestion、ksn-distill へ)
4. iOS Skill の prerelease 説明と README / docstring の食い違いは docs-refresh へ申し送る (Suggestion、前回からの継続)

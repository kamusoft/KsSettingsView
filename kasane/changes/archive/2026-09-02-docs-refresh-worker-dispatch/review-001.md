# レビュー結果: docs-refresh-worker-dispatch (001 回目)

**日付**: 2026-09-02
**判定**: NEEDS_DISCUSSION

## サマリー

切り出し (実装の見当 (2)) は忠実かつ丁寧で、インライン Python は 1 本も残らず、出力一致もこちらで再現できた (3 スクリプト + 予定 manifest を独立に再実行し、標準出力・終了コードとも一致)。参照が壊れた箇所 (「6-② の定義を再掲」等) も解消されており、テンプレート 2 本は単体で意味が閉じている。

一方、器の固定 (実装の見当 (1)) については、SKILL.md に新しく書かれた根拠 —「器を指定しない Task はメインのモデルを継承するため、最も重い作業がメインと同じ高コストのモデルで走ってしまう」— が、名指しした器の定義と矛盾する。`ksn-implementer` の器定義は `model: claude-opus-5` を宣言しており、この環境のメイン (`opus[1m]`) と同じ最上位モデルである。器を指定しても走るモデルの階層は下がらず、change の主目的だったトークン節約が (1) の経路では発生しない。実装の問題ではなく合意済みスコープの前提の問題なので、オーナー判断を仰ぐ NEEDS_DISCUSSION とする。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新設 `.py` 8 本のコメント・docstring
- `kasane/handbook/cross/user-skill-api-listing.md` (きっかけ: docs-refresh の 3e 検査の記述に触れるため) — 3e の説明文・参照リンクの妥当性
- `kasane/lessons/code-review.md` の重点観点 L-001 (ミューテーションによる検出力実測) — 本 change はテストコードを含まないため適用外
- ドメイン: cross。`kasane/config.yaml` の `skills.code-review` は空で、追加のドメインスキルなし

## 検証したこと (実施記録)

ビルド・テストの対象コードを含まない change のため、代替として次を実測した。

- 切り出し前後の出力一致を独立に再現。`git show HEAD:.agents/skills/docs-refresh/SKILL.md` から parity-check.md 記載の行範囲を抜き出して実行し、新スクリプトと比較:
  - `concepts-coverage-check.py` (HEAD L152-170) — 一致 (rc=0)
  - `heading-parity-check.py` (HEAD L487-519) — 一致 (rc=0)
  - `api-coverage-check.py` (HEAD L196-236) — 一致 (rc=0、67 行)
  - `planned-manifest.py` (HEAD L427-443、判断なし) — 出力 JSON 完全一致
- `planned-manifest.py` の判断あり経路 — `DOCS_REFRESH_DECISIONS` に `addTargets` / `addExcluded` を与え、`targets` の該当キーへ concept が追加され `excluded` に載ることを確認
- `python3 scripts/local-path-lint.py` / `identity-lint.py` — 触った全ファイルおよび change アーティファクトで exit 0
- SKILL.md に残るインライン Python の有無 — `grep "python3 - <<"` で 0 件
- SKILL.md の新規リンク解決 — `references/prompt-skill.md`・`references/prompt-readme.md`・`../../../kasane/handbook/cross/user-skill-api-listing.md` はいずれも実在
- 器の全経路固定 — コンテキスト節約方針 / Step 1 / Step 5 本文 / Guardrails / テンプレート 2 本の冒頭に `subagent_type: ksn-implementer` の指定があり、器なしで Task を起動する経路は残っていない

## 指摘事項

### [🟠 Major] 器の固定が謳う「モデル階層の引き下げ」が器定義と矛盾する

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:50` (コンテキスト節約方針)、同 `:239` (Step 5)、同 `:457` (Guardrails)

**問題点**:
SKILL.md は器固定の根拠を「器を指定しない Task はメインのモデルを継承するため、concepts 本文と en/ja 一式を読み書きする最も重い作業がメインと同じ高コストのモデルで走ってしまう」と書いている。しかし名指しした器 `ksn-implementer` の定義 (ユーザースコープの `.claude/agents/` 配下) は `model: claude-opus-5` を宣言しており、メイン (ユーザースコープ設定の `model: opus[1m]`) と同じ最上位モデルである。既定のサブエージェントモデルも設定されていないため、器を指定しない場合の継承先もやはり opus である。

つまりこの変更で実際に変わるのは、器定義が持つ `effort: medium` の固定と実装ワーカーとしての規律付けだけで、**モデル階層は下がらない**。exploration の課題認識 1 (「最も重い作業がそのまま高レベルモデルで走る」) は解消されておらず、SKILL.md に書かれた根拠は現状の器定義に対して事実と異なる。

なお、委譲によるメインのコンテキスト隔離は器の指定に関係なく元から成立していた (Step 5 は以前からサブエージェント委譲を前提にしていた) ため、「コンテキスト節約が成立しなくなる」という結語も器の指定とは因果が結びついていない。

また、この事実関係のもとでは exploration で却下された案 C (docs 専用の器を新設して低コストモデルを割り当てる) が、目的に対して唯一有効な選択肢になる。却下理由は「目的 (トークン節約) には過剰」だったが、案 B ではその目的自体が達成されないため、却下の前提が崩れている。

**推奨修正**: 次のいずれかをオーナー判断で選ぶ (実装者が独断で決めない)。

1. **根拠の記述を実態に合わせる** — 器固定の効能を「モデル階層の引き下げ」ではなく「effort の固定と実装ワーカー規律の適用、起動先のぶれの排除」と書き直し、トークン節約の主因は (2) の SKILL.md 減量である旨に改める。change の目的も (2) 中心に読み替わる
2. **器の選択を見直す** — 低コストモデルを宣言した専用の器 (案 C 相当) を Kasane 側に用意し、docs-refresh はその器名を名指しする。exploration の決定を覆すため、再探索が要る

### [🟡 Minor] Step 1 の「器が配置済みか」の確認に、実行可能な手段が書かれていない

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:64`

**問題点**:
Step 1 は「Task ツールで `subagent_type` に `ksn-implementer` を指定してサブエージェントを起動できるか (器が実行環境に配置済みか) を確認する」と定めるが、**どうやって確認するか**が書かれていない。エージェントには器一覧を得る具体的な手段 (利用可能なエージェント種別の列挙、または器定義ファイルの存在確認) を明示しないと、確認したつもりで通過し、実際には Step 5 の Task 起動が失敗して初めて不在が判明する。その時点では Step 4 の承認提示まで済んでおり、「何も書き換えないまま停止する」という設計の狙い (skills/・README・manifest の未変更保証) は保たれるものの、ユーザーの承認操作が無駄になる。

Step 5 に最終防波堤の記述があるため実害は限定的だが、Step 1 を「具体化する」ことが実装の見当 (1)-2 の要求だった点からは踏み込みが浅い。

**推奨修正**: Step 1 に確認の実手段を 1 行足す (例: 利用可能なサブエージェント種別の一覧に `ksn-implementer` があることを確認する、あるいはユーザースコープ / プロジェクトスコープの `.claude/agents/ksn-implementer.md` の存在を確認する)。片方でも成立すれば可とする旨も添えると環境差に強い。

### [🟡 Minor] parity-check.md の comment-policy-lint 証跡が、新設スクリプトを 1 件も検査していない

**該当箇所**: `parity-check.md` の「併せて通した lint」節

**問題点**:
証跡は `python3 scripts/comment-policy-lint.py --summary` — 禁止 0 件 と記録しているが、`.py` は `scripts/comment-policy-lint.py` の `DEFAULT_TARGET_EXT` にも `kasane/config.yaml` の `lint.comment-policy.ext` (`.csproj` / `.props` / `.targets`) にも含まれない。実測すると新設スクリプトは検査対象に入らない:

```
python3 scripts/comment-policy-lint.py --paths .agents/skills/docs-refresh/scripts/*.py --advisory
→ 合計: 0 ファイル / 禁止 0 件 / 要確認 0 件 (検査対象 0 ファイル)
```

つまりこの「禁止 0 件」は新設ファイルについて何も保証していない。comment-policy.md 自身が「検出 0 件は適合の証明にならない」と明記しており、証跡としてそのまま残すと後続の読み手が適合の根拠と誤読する。`.py` が対象外なのはリポジトリ既存の状態であって本 change が壊したものではないため、指摘は証跡の書き方に限る。

**推奨修正**: parity-check.md の当該行を「`.py` は現行の検査対象拡張子に含まれないため、新設スクリプトは機械検査の対象外。規約適合は手読みで確認した」と実態どおりに書き換える。`.py` を検査対象に加えるかどうかは別判断 (加える場合は下の Suggestion が実際の違反になる)。

### [🔵 Suggestion] `concepts-coverage-check.py` の docstring が `kasane/` 配下をパスで参照している

**該当箇所**: `.agents/skills/docs-refresh/scripts/concepts-coverage-check.py:5`

**問題点**:
docstring に「kasane/concepts/ に実在する概念ファイルのうち」とあり、comment-policy.md が禁じる「`kasane/` 配下のパス参照」の書式に当たる (機械検査の BLOCKING パターン `(?:^|[^\w/])kasane/` にも合致する形)。ただし本件は設計根拠の参照ではなく、スクリプトが実際に走査するディレクトリという**機能的な入力の説明**であり、規約が禁じる理由 (アーカイブされて意味を追えなくなる) は当てはまらない。同種の記述はリポジトリ標準装備の `scripts/comment-policy-lint.py` の docstring 自身にもある。

**推奨修正**: 現状維持でよいと考える。ただし上の Minor で `.py` を検査対象に加える判断をした場合は機械検査で落ちるので、そのときは「走査対象は concepts のルートディレクトリ」のようにパスを書かない表現へ寄せるか、行内に `comment-policy:allow` を添える。

### [🔵 Suggestion] `link-resolution-check.py` の入力パスがハードコードで、不在ガードがない

**該当箇所**: `.agents/skills/docs-refresh/scripts/link-resolution-check.py:20`

**問題点**:
入力の `/tmp/docs-refresh-targets.txt` が直書きで、他の 7 本が環境変数で入力を差し替えられるのと非対称。また空ガード (`if not targets`) はあるがファイル不在のガードはなく、`targets-list.py` を実行せずに単独起動するとトレースバックで落ちる。いずれも切り出し前のインライン版と同一挙動であり、忠実な切り出しの結果ではある。

**推奨修正**: 必須ではない。共有スクリプトとして再利用しやすくするなら、`DOCS_REFRESH_TARGETS` で入力パスを差し替え可能にし、不在時は「先に targets-list.py を実行してください」と案内して終了する形が扱いやすい。忠実性を優先して現状維持でも妥当。

## アクションプラン

1. **Major の器/根拠の扱いをオーナーが決める** — 記述を実態に合わせる (推奨修正 1) か、器の選択を再探索する (推奨修正 2)。ここが決まるまで他の指摘の修正は着手しなくてよい (1 を選ぶなら SKILL.md の 3 箇所と exploration の決定事項の整合も併せて見る)
2. Step 1 に器存在確認の実手段を 1 行追記する (Minor)
3. parity-check.md の comment-policy-lint 証跡を実態どおりに書き直す (Minor)
4. Suggestion 2 件は対応不要。ただし `.py` を comment-policy の検査対象に加える判断をする場合のみ、`concepts-coverage-check.py` の docstring を併せて処理する

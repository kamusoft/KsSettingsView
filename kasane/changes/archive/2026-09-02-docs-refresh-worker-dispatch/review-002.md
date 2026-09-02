# レビュー結果: docs-refresh-worker-dispatch (002 回目)

**日付**: 2026-09-02
**判定**: APPROVED

## サマリー

review-001 の Major 1 件・Minor 2 件はいずれも解消された。器固定の根拠文はオーナー判断の整理 (器が担うのは「編成の切り離し」であって文脈隔離でもモデル階層の引き下げでもない) どおりに書き直され、前提が「メインを高階層モデルで運用しているとき」という条件付きの運用事象として正しく限定されている。Step 1 には器配置の確認手段が実行可能な形で追加され、parity-check.md の comment-policy-lint 証跡も実態どおりに訂正された。

修正で新たな不整合は生じていない。切り出しスクリプト 8 本とテンプレート 2 本は今サイクルで一切変更されておらず (mtime 据え置き)、出力一致も再度実測して維持を確認した。足場である exploration.md も書き換えられていない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新設 `.py` 8 本のコメント・docstring を、機械検査が届かない範囲まで手読みで再走査
- `kasane/handbook/cross/user-skill-api-listing.md` (きっかけ: docs-refresh の 3e 検査の記述に触れるため) — 今サイクルの修正は 3e 周辺に及ばず、前回の判定を維持
- `kasane/lessons/code-review.md` の重点観点 L-001 — テストコードを含まない change のため適用外
- ドメイン: cross。追加のドメインスキルなし (`kasane/config.yaml` の `skills.code-review` は空)

## review-001 指摘の解消確認

### [🟠 Major] 器の固定が謳う「モデル階層の引き下げ」が器定義と矛盾する → 解消

`.agents/skills/docs-refresh/SKILL.md:50` の根拠文は次の 3 点に書き直されており、事実関係と一致する。

- 「メインのモデルを継承する」ことの帰結を、**メインをより高階層のモデルで運用しているとき**という条件付きに限定した。器定義のモデル値とセッションのモデルが一致しない運用を前提にすれば成立する記述であり、前回指摘した「無条件にモデル階層が下がる」という誤った含意は消えている
- 器の効能を「その作業は器定義が持つモデル・エフォートで走り、メインの編成から切り離せる」と、実際に起きることの記述に改めた
- 「サブエージェントへの文脈隔離自体は器の有無によらず成立する — 器が担うのは編成の切り離しである」と明記し、前回指摘した因果の取り違え (器の指定とコンテキスト節約を結びつけていた点) を自ら打ち消している

波及箇所も揃っている: `:239` (Step 5) と `:457` (Guardrails) の括弧書きはいずれも「編成の切り離しが成立しなくなる」に置換され、「コンテキスト節約が成立しなくなる」という文言は SKILL.md から消えた (grep 0 件)。両箇所が参照する `「コンテキスト節約方針」` は節の名前として残るだけで、節内の当該段落が器の役割を明示的に切り分けているため、読んで取り違える余地はない。

オーナー判断どおり器の再探索は行われておらず、exploration.md の決定事項 (案 B 採用・ksn-core 非依存) とも矛盾しない。exploration.md の課題認識 1 (「器の指定がない Task はメインのモデルを継承するため、最も重い作業がそのまま高レベルモデルで走る」) は新しい根拠文と同じことを述べており、書き換え不要のまま整合している。

### [🟡 Minor] Step 1 の器存在確認に実行可能な手段がない → 解消

`.agents/skills/docs-refresh/SKILL.md:64` に、① 利用可能なサブエージェント種別の一覧に `ksn-implementer` があること、② ユーザースコープ / プロジェクトスコープの器定義ファイルの存在確認、のいずれかでよい (片方成立で配置済みとみなす) と追記された。どちらもエージェントが実際に実行できる手段であり、実装の見当 (1)-2 が求めた「具体化」を満たす。① を先に挙げているため、器がプラグイン由来で ② の 2 ディレクトリに実体を持たない場合も取りこぼさない。

### [🟡 Minor] parity-check.md の comment-policy-lint 証跡が新設 `.py` を検査していない → 解消

`parity-check.md:43` が「`.py` は現行の検査対象拡張子 (既定 + `lint.comment-policy.ext`) に含まれないため、新設スクリプト 8 本は機械検査の対象外 (検査対象 0 ファイル)。規約適合は手読みで確認した」に書き換えられ、証跡が実態と一致した。「禁止 0 件」を適合の証明と誤読させる書き方ではなくなっている。

手読み確認の主張についてはこちらでも独立に裏を取った。comment-policy.md の禁止参照・禁止記述類型を新設 8 本のコメント・docstring 全体に当てて走査したところ、検出は `concepts-coverage-check.py:5` の `kasane/concepts/` 参照 1 件のみで、これは review-001 で Suggestion として提示しオーナーが対応不要と判断済みの既知の 1 件である。未知の違反は無い。

## 今サイクルで確認した回帰 (実施記録)

- 切り出しスクリプト 8 本・テンプレート 2 本は mtime が前サイクルのまま (18:30〜18:32)、今回の修正で触られていない
- 出力一致の再実測 — `git show HEAD:.agents/skills/docs-refresh/SKILL.md` から抜いたインライン版と比較し、`concepts-coverage-check.py` / `api-coverage-check.py` / `heading-parity-check.py` / `planned-manifest.py` (判断なし) の 4 本すべてで標準出力・終了コードが一致
- SKILL.md にインライン Python の再混入なし (`grep "python3 - <<"` 0 件)。サイズ 44,663 バイト (前サイクル 44,001 から根拠文と Step 1 の追記分だけ増加)
- SKILL.md のリンク解決 — `references/prompt-skill.md` / `references/prompt-readme.md` / `../../../kasane/handbook/cross/user-skill-api-listing.md` / `../../../kasane/decisions/cross/0022-user-docs-as-agent-skills.md` はいずれも実在
- 器なしで Task を起動する経路は引き続き残っていない (コンテキスト節約方針 / Step 1 / Step 5 / Guardrails / テンプレート 2 本の計 6 箇所で固定)
- `python3 scripts/local-path-lint.py` / `identity-lint.py` — 触った全ファイルと change アーティファクト一式で exit 0。Step 1 に加わった `~/.claude/agents/...` はチルダ相対でローカル絶対パスに当たらない
- 足場アーティファクト `exploration.md` は未変更 (mtime 18:28 のまま)、`deviation.md` も未変更

## 指摘事項

新規の指摘なし。review-001 の 🔵 Suggestion 2 件 (`concepts-coverage-check.py:5` の `kasane/` パス参照、`link-resolution-check.py:20` の入力パスハードコードと不在ガード) はオーナーが対応不要と判断済みで、いずれも実害がなく切り出しの忠実性を優先した結果として妥当。再提起はしない。

## アクションプラン

対応必須の項目なし。蒸留に進んでよい。蒸留時の申し送りとして 1 点だけ: 本 change のトークン削減効果の主因は SKILL.md の減量 (56,285 → 44,663 バイト) 側であり、器の固定が担うのは編成 (モデル・エフォート) の切り離しである — この切り分けは SKILL.md 本文には残るが、exploration.md の動機文には反映されていないため、長命層へ残す場合は SKILL.md の記述を正とする。

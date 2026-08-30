# レビュー結果: rollout-user-skills (共通成果物 / 001 回目)

**日付**: 2026-08-26
**判定**: CHANGES_REQUESTED
**対象**: 索引 README 2 枚 / ルート README 追記差分 / `kasane/concepts/cross/conventions/aiforms-spec-summary.md` の移送 / `manifest-draft.json`
**対象外**: Skill 本体 8 部の内容 (別レビュー担当) / 移送済み仕様要約の内容の正確性 / `docs/` の存在と README 群の docs/ リンク残り (グループ 6 の予定)

## サマリー

manifest 初期版は spec とスキーマ規範の要求を機械検査で全項目通過しており、`aiforms-spec-summary.md` の移送も形式要件 (frontmatter・凍結注記・パス無害化・相互リンク・index / log 登載) をすべて満たしている。ルート README の差分も導線 1 文 + 構成表 1 行に厳密に限定されている。一方、索引 README の中核であるコピー手順は、利用者が書かれたとおりに実行しても意図した結果にならず (作業ディレクトリの前提が両立しない)、さらに手順どおりコピーした直後に全 8 部の SKILL.md から索引へのリンクが必ず壊れる。索引の存在意義に直結する 2 件のため CHANGES_REQUESTED とする。

## 確認した観点と結果 (指摘なし分)

- **manifest 不変条件 (機械検査で全通過)**: `concepts` のキー集合が `kasane/concepts/**/*.md` から index / log / rules を除いた集合と過不足なく一致 (差分 0) / 全 39 件の SHA-256 が現作業ツリーと一致 / `targets` の全キーが `skills/en/` と `skills/ja/` の双方に実在 / 生成された全 Skill ファイル 34 本 (言語抜き 17 パス) がちょうど 1 キーずつ存在し過不足なし / `targets` の値と `excluded` のキーがすべて実在 concept / 空の除外理由 0 件 / 網羅漏れ concept 0 件 / `targets` と `excluded` の重複 0 件 / `readmes` の 8 パスすべて実在 / 必須キー (`version` `concepts` `targets` `excluded` `readmes`) 完備。AiForms 移行 Skill の両ファイルの源泉に `cross/conventions/aiforms-spec-summary.md` を含む点も spec の要求どおり
- **`readmes` の網羅**: リポジトリ内の README で列挙外なのは `docs/README.md` (廃止対象) と `maui/spike/README.md` (完了済み spike の開発用) のみ。利用者向け README 群としての列挙は妥当
- **移送の形式要件**: `type: reference` / 冒頭の凍結注記 (「最終的な正は移植元コードにしかない」) / ローカル絶対パス・`file://` の 0 件 (`scripts/local-path-lint.py` exit 0) / `aiforms-origin-reference.md` との双方向リンク (spec-summary:13, 427 ↔ origin-reference:26) / `cross/index.md` の conventions 節へ登載 / `concepts/log.md` へ created エントリ追加。`scripts/identity-lint.py` も exit 0
- **ルート README の変更範囲**: `git diff README.md` は導線 1 文 (README.md:14) と構成表 1 行 (README.md:36) の 2 追加のみ。導線文は「主な特徴」直後に置かれ索引へのリンクを含む。Requirement「ルート README への導線」の追記部分の要求を満たす
- **索引の 3 要素**: 一覧表 (name / 対象 / 1 行説明 / en・ja リンク、リンクは全 8 件解決)、コピー手順 (`.agents/skills/` を第一候補、Claude Code は `.claude/skills/`)、片言語コピー前提の明記がそれぞれ存在し、これ以外の節は無い。en / ja で節構成とコードブロックが一致

## 指摘事項

### [🟠 Major] コピー手順のコマンドが、どの作業ディレクトリで実行しても意図した結果にならない

**該当箇所**: `skills/README.md:16-28`、`skills/README_ja.md:16-28`

**問題点**: 手順は「使いたい Skill のディレクトリを自分のプロジェクトへコピーする」と述べた上で `mkdir -p .agents/skills` + `cp -R skills/<lang>/<skill-name> .agents/skills/` を提示するが、2 つのコマンドが要求する作業ディレクトリが両立しない。

- 利用者のプロジェクトルートで実行した場合: コピー先 `.agents/skills/` は正しいが、コピー元 `skills/<lang>/<skill-name>` はそのプロジェクトに存在せず失敗する
- KsSettingsView の clone 内で実行した場合: コピー元は解決するが、`.agents/skills/` は KsSettingsView の clone 内に作られ、利用者のプロジェクトには何も入らない

clone の取得方法 (`git clone` / ダウンロード) にも言及がないため、利用者は手順から実際の操作を復元できない。索引の 3 要素のうち②が利用者視点で成立していない。

**推奨修正**: コピー元をクローン先基準の明示パスにし、実行位置を 1 行添える。例: 「自分のプロジェクトのルートで実行する。`<path-to-KsSettingsView>` は本リポジトリを clone した場所」+ `cp -R <path-to-KsSettingsView>/skills/<lang>/<skill-name> .agents/skills/`。en / ja 双方を同時に直し、コードブロックは byte 一致を維持すること (翻訳ロックステップ)。

### [🟠 Major] 手順どおりコピーすると、全 8 部の SKILL.md から索引へのリンクが必ず壊れる (索引の「自己完結」記述と矛盾)

**該当箇所**: `skills/README.md:30`、`skills/README_ja.md:30` (「各 Skill ディレクトリは `SKILL.md` と `references/` で自己完結している」) と、全 8 部の `SKILL.md:28` (`../../README.md` / `../../README_ja.md`)

**問題点**: 索引が案内するとおり `skills/<lang>/<name>/` をディレクトリごと `.agents/skills/` へコピーすると、`.agents/skills/<name>/SKILL.md` の `../../README.md` は `.agents/README.md` を指し、解決しない。導入手順への唯一の入口である「この Skill の導入手順は Skills 索引のコピー手順を参照する」が、コピー後の利用者の手元では常に dead link になる。索引が明示する「自己完結」の前提とも食い違う。完了検査⑥ (内部リンク解決) はコピー前のツリーで通るため、この不整合は機械検査では検出されない。

**推奨修正**: SKILL.md 側の索引参照をコピー後も解決する形にする (`metadata.source` と同じリポジトリ URL への絶対リンク、またはリンクを外して「KsSettingsView リポジトリの `skills/README.md` を参照」と文言化する) のが素直。索引側だけで閉じるなら、「導入後は SKILL.md からの索引リンクは解決しない (リポジトリ側の索引を参照する)」旨を明記する。SKILL.md 本体は別レビューの担当範囲のため、どちら側で直すかは orchestrator 側で調整されたい。

### [🟡 Minor] 移行 Skill から maui Skill への相対リンクが「両方を同じ親へコピー」を暗黙の前提にしている

**該当箇所**: `skills/en/kssettingsview-aiforms-migration/SKILL.md:24`、`skills/ja/kssettingsview-aiforms-migration/SKILL.md:24` (`../kssettingsview-maui/SKILL.md`)、案内が無い箇所として `skills/README.md:12`・`skills/README_ja.md:12`

**問題点**: このリンクは `kssettingsview-maui` を同じ `.agents/skills/` 配下へ併せてコピーした場合のみ解決する。索引にはその前提の案内がなく、移行 Skill だけをコピーした利用者では解決しない。上記 Major と違い条件付きで成立するため Minor とする。

**推奨修正**: 索引の一覧表の移行 Skill 行の 1 行説明、またはコピー手順の末尾に「移行 Skill は `kssettingsview-maui` と併せてコピーする」旨を 1 文添える (3 要素の構成は崩さない)。

### [🟡 Minor] 日本語利用者が日本語索引に辿り着けない (索引 2 枚の相互リンクとルート導線の言語案内が無い)

**該当箇所**: `README.md:14`、`skills/README.md:1-3`、`skills/README_ja.md:1-3`

**問題点**: ルート README は日本語で書かれているが、新設の導線文は英語索引 `skills/README.md` のみをリンクしている。索引 2 枚にも相互リンクが無いため、日本語利用者は `skills/README_ja.md` の存在をディレクトリを覗くまで知れない。索引が「英語版と日本語版を用意している」と書いていながら、もう一方への導線が無い。

**推奨修正**: 索引 2 枚のリード文 (README.md:3 / README_ja.md:3) に相手言語版へのリンクを 1 つ足す (節を増やさないため「3 要素のみ」の構成は維持できる)。ルート導線文に日本語版へのリンクを併記する案も spec の要求 (「索引 `skills/README.md` へのリンクを含む」) と両立するが、1 文に収める制約があるため索引側だけの対処でも足りる。

### [🟡 Minor] Cell 数の数え方が Skill 間で不統一なまま索引に横並びで転記されている

**該当箇所**: `skills/README.md:9-11`、`skills/README_ja.md:9-11`

**問題点**: iOS / Android 行は「12 種の Cell」+ CustomCell を別記、MAUI 行は「13 種の Cell」(各 SKILL.md の description を確認したところ MAUI の 13 は CustomCell を含む数)。索引は 4 つの Skill を横並びで比較する唯一の場であり、この表記では MAUI だけ Cell 種別が 1 つ多いと読める。数の不一致ではなく数え方の不一致なので Minor とする。

**推奨修正**: 索引内で数え方を揃える (例: MAUI 行も「組み込み 12 種の Cell、CustomCell」とする)。SKILL.md の description 側の表記と揃えるかは Skill 本体レビューと合わせて判断されたい。

### [🟡 Minor] `docs/` 廃止後に dangling となる旧パス参照が、グループ 6 のタスク列挙から漏れている

**該当箇所**: `kasane/decisions/cross/0017-port-aiforms-to-native.md:27` (`出典: … / docs/legacy-aiforms-reference.md`)、`kasane/concepts/log.md:54`、`kasane/concepts/log.md:64`

**問題点**: 移送によって旧パスは消えるが、これらの参照は新パスへ更新されていない。tasks.md の 6.1 は対象を「concepts の docs/ 前提記述 2 箇所 (comment-policy / test-execution)」に、6.2 は README 群に限定しており、decisions/ と log.md の過去エントリはどちらの範囲にも入っていない。Requirement「docs/ の廃止と残記述整理」が凍結資料として除外しているのは `openspec/` / `kasane/changes/archive/` / ロードマップの過去記録のみのため、このままでは 6.4 の残存 grep が 0 件にならず完了判定でつまずく。

**推奨修正**: 6.4 に到達する前に方針を決めておく。ADR-0017 の出典行は `kasane/concepts/cross/conventions/aiforms-spec-summary.md` へ差し替えるのが素直 (移送先が確定しており、出典としての意味も保たれる)。`log.md` の過去エントリは append-only の履歴であり書き換え対象にしない運用なら、6.4 の grep 除外に「`kasane/concepts/log.md` の過去エントリ」を明記して意図的な残存であることを残す。

### [🟡 Minor] `excluded` の `comment-policy.md` の理由が、タスク 6.1 の実施後に実態と食い違う

**該当箇所**: `kasane/changes/rollout-user-skills/manifest-draft.json:195`

**問題点**: 現在の理由は「本リポジトリ開発時のコメント規約 (エージェント向け)。利用者向け Skill の対象外」。しかし tasks.md 6.1 では comment-policy に「skills/ のコード例は原則コメントレス・最小限は英語」という別規約を追記する予定であり、追記後は同 concept が skills/ のコードブロックの形を直接規定することになる。「対象外」のまま `excluded` に置くと、以後この規約が改訂されても docs-refresh は skills/ を見直し対象として提示しない。他の excluded 7 件 (build-toolchain / declarative-ui-bridge / native-host-boundary / repository-boundaries / runtime-behavior-verification / sample-parity / test-execution) の理由は妥当と判断した。

**推奨修正**: 7.1 の最終書き出し時に理由を実態へ合わせる。例:「生成規約であり Skill 本文の源泉ではない。ただし skills/ のコード例規約を含むため、同規約の改訂時は skills/ のコードブロックを手動で見直す」。`targets` 側へ移す選択もあり得るが、その場合は全 17 ファイルの源泉に加わり差分検出が過剰に発火するため、理由の精緻化を推奨する。

### [🔵 Suggestion] `lastUpdatedFiles` が空のまま

**該当箇所**: `kasane/changes/rollout-user-skills/manifest-draft.json:210`

**問題点**: スキーマ規範では「このリフレッシュで更新したファイルのリポジトリ相対パス」。初期生成は skills/ 全ファイルを書き出した操作であり、空配列だと「更新なし」とも読める。必須キー (不変条件③) には含まれないため実害は小さいが、次回 docs-refresh 実行時の解釈が揺れる余地が残る。

**推奨修正**: 7.1 の最終書き出しで生成した全ファイル (skills/ 34 本 + 索引 2 枚 + ルート README) を列挙するか、「初期生成では空とする」という解釈を tasks.md か本 change の記録に残す。

### [🔵 Suggestion] MAUI の「開発中」表記が導線と索引に反映されていない

**該当箇所**: `skills/README.md:11`、`skills/README_ja.md:11`、`README.md:14`

**問題点**: ルート README は MAUI を 2 箇所 (対応プラットフォーム表 `README.md:22`、モノレポ構成表 `README.md:31`・`README.md:34`) で「開発中」と表記しているが、新設の導線文と索引の MAUI 行には状態注記がなく、iOS / Android と同列に読める。利用者が MAUI Skill をコピーして出荷済みパッケージを期待する余地がある。

**推奨修正**: proposal の Non-Goals が「ルート README の…状態表記」を phase-9 の責務として明示的に除外しているため、ルート README 側の対処は本 change の範囲外と判断する。索引側は「1 行説明」の中に収まるため 3 要素の構成を崩さずに注記でき、対処の要否はオーナー判断を仰ぎたい。

## アクションプラン

1. コピー手順の作業ディレクトリ前提を直す (Major 1) — 索引 2 枚、コードブロックの byte 一致を維持
2. コピー後に壊れる索引リンクの扱いを決めて直す (Major 2) — SKILL.md 側の絶対参照化が第一候補。Skill 本体レビューとの調整が必要
3. 移行 Skill の併せコピー案内 (Minor 3) と索引 2 枚の相互リンク (Minor 4) を追加
4. Cell 数の数え方を索引内で統一 (Minor 5)
5. グループ 6 着手前に decisions/ と log.md の旧パス参照の方針を確定 (Minor 6) — 6.4 の通過条件
6. 7.1 の最終書き出しで `excluded` の comment-policy 理由を更新 (Minor 7) し、`lastUpdatedFiles` の扱いを確定 (Suggestion 8)
7. MAUI の状態注記の要否をオーナーに確認 (Suggestion 9)
8. 上記反映後に 4.1 の機械検査を再実行 (②節構成一致・③コードブロック byte 一致・⑥内部リンク解決が影響を受ける)

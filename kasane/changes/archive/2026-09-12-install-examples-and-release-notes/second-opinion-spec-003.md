# セカンドオピニオン: install-examples-and-release-notes (spec-003)
**相方**: codex / **label**: so-spec-install-examples-and-release-notes-3 / **日付**: 2026-09-11 / **対象**: spec-002 の指摘を反映した改訂版一式
---
# レビュー結果: install-examples-and-release-notes

**判定**: NEEDS_DISCUSSION  
**件数**: Critical 0 / Major 6 / Minor 4 / Suggestion 0

仕様・設計側で解消すべき矛盾があります。このまま実装すると、リリースノートの重複・起点の誤選択や、削除済みスクリプトを handbook 経由で呼ぶ期間が生じます。

## 照合した規約

- `comment-policy.md`（always）
- `user-skill-api-listing.md`（`skills/` を変更）
- `release-procedure.md`（release workflow・リリース手順を変更）

指定どおりビルド・テスト・ファイル書き込みは行っていません。

## 指摘事項

### [🟠 Major] handbook を正としながら、スキルにも手順の順序を持たせている

**該当箇所**: `design.md:123`、`design.md:125`、`specs/release-workflow/spec.md:132`、`specs/release-workflow/spec.md:133`

**問題点**: 「段の順序をスキルへ書き写さない」としながら、スキルが「どの節をどの順に参照するか」を保持すると定めています。これは実質的に順序の複製です。handbook で節の追加・並べ替えを行った場合、スキルも修正しなければならず、Scenario「手順を変えても道具を直さずに済む」を満たしません。

**推奨修正**: 次のどちらかへ設計を一本化してください。

- スキルは handbook の単一エントリポイントだけを読み、順序も handbook だけが所有する。
- スキルが順序を所有することを認め、handbook は各段の詳細を所有する。その場合は「手順の正」の定義と drift 検査を改める。

### [🟠 Major] ADR-0030 の下書き範囲が design・spec と正反対

**該当箇所**: `kasane/decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md:39`

**問題点**: ADR は「直前のリリース以降の commit」から下書きを作るとしています。一方、`design.md:151-159`、`specs/release-workflow/spec.md:134-146`、`tasks.md:51` は、既に `main` へ入った変更を除く「今回の PR が持ち込む差分」だけを対象にすると定めています。ADR のまま実装・accepted 化すると、先行 PR の項目が後続 PR に再掲されます。

**推奨修正**: ADR-0030:39 を `origin/main..origin/develop` 相当の今回差分へ修正し、先行 PR を除外する理由も ADR に反映してください。

### [🟠 Major] Release ノートの起点規則が成果物間で一致していない

**該当箇所**: `proposal.md:20`、`exploration.md:29`、`kasane/decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md:23`

**問題点**: proposal・exploration は「前回 tag」、ADR は「直前の GitHub Release」としていますが、design・spec は「今回 version 以外・公開済み・非 draft・対象 commit の祖先である Release のうち履歴上もっとも近いもの」です。Release を持たない tag や別枝の Release を避けるという中心的な設計が、上位成果物と proposed ADR に反映されていません。また「履歴上もっとも近い」の判定方法も merge DAG 上では一意に定義されていません。

**推奨修正**: 全成果物を refined rule に統一し、「近い」を `main` の first-parent 上で対象 commit に最も近いもの等、機械的に一意となる形で定義してください。該当候補が複数ある Scenario も追加してください。

### [🟠 Major] 初回リリースでは必須の比較リンクを生成できない

**該当箇所**: `specs/release-workflow/spec.md:37`、`specs/release-workflow/spec.md:53`、`design.md:91`

**問題点**: 初回リリースでは起点 Release が無いことを仕様化している一方、ノートには常に「前回のリリースとの比較」を含める SHALL となっています。前回 tag が存在しないため、比較リンクの生成規則が定まりません。`tasks.md:38` は初回ケースをテスト対象にしていますが、期待本文を確定できません。

**推奨修正**: 初回だけ比較リンクを省略する、最初の commit からの履歴リンクにする、などを明示してください。対象 PR がゼロ件だった場合の本文も併せて定義してください。

### [🟠 Major] 実装完了から蒸留まで、リリース手順が実行不能になる

**該当箇所**: `tasks.md:22`、`tasks.md:49`、`tasks.md:73`、`tasks.md:75`

**問題点**: 実装タスクは `set-readme-version.py` を削除し、handbook を読むリリース用スキルを追加しますが、handbook・`AGENTS.md`・`CLAUDE.md` の更新は「実装タスクではない」蒸留申し送りに置かれています。現行の `kasane/handbook/cross/release-procedure.md:120-143` と `AGENTS.md:17` は、削除予定のスクリプトと自動生成ノートを引き続き指示しています。したがって蒸留前のスキルは古い、または実行不能な手順を読みます。

**推奨修正**: `AGENTS.md` / `CLAUDE.md` の撤去を実装タスクへ移し、handbook 更新が完了するまでリリース用スキルを有効化・完了扱いにしないゲートを設けてください。handbook 更新を Kasane 上蒸留に限定するなら、蒸留完了まで本変更を利用可能とみなさないライフサイクルを明記する必要があります。

### [🟠 Major] GitHub API と job 間受け渡しの実経路が一度も検証されない

**該当箇所**: `design.md:59`、`design.md:101`、`proposal.md:35`、`tasks.md:38`、`tasks.md:68`

**問題点**: dry-run は全ての PR 収集を省略し、自己テストは模擬 Release 一覧・一時 git 履歴だけです。次の中心経路が本番まで未検証になります。

- GitHub API の認証・pagination
- commit と PR の実際の関連付け
- 全ページからの収集
- validate で確定したノートの保存と publish での取得
- ノートファイルを `gh release create` へ渡す配線

特に取りこぼしは失敗ではなく、不完全なノートとして静かに成功する可能性があります。

**推奨修正**: `0.1.0-beta.1..0.1.0-beta.2` の既知履歴を使う読み取り専用の実 API probe、または `main` ref の dry-run だけ収集・検査・artifact 受け渡しまで行う経路を追加してください。pagination 境界を越える fixture も必要です。

### [🟡 Minor] 「version を検査しない」と日常 lint が矛盾して読める

**該当箇所**: `specs/install-examples/spec.md:12`、`specs/install-examples/spec.md:31`

**問題点**: 前者はインストール例の version を「検査もしない」、後者は具体 version が無いことを日常 CI で検査するとしています。

**推奨修正**: 「release workflow では dispatch 入力との一致検査をしない」と限定してください。

### [🟡 Minor] PR テンプレートが閉じた文法を破る危険がある

**該当箇所**: `tasks.md:37`、`tasks.md:45`

**問題点**: `## Changes` 内では、空行以外の説明文・HTML コメント・コードフェンスも不正です。一方、テンプレートには種別一覧や例を置く予定で、それらをセクション内へ置くと未編集テンプレートが必ず失敗します。また空セクションは失敗条件なのに、自己テストの列挙にありません。

**推奨修正**: テンプレートの説明は次の見出し以降へ置くなど、初期状態でも parser が受理する形を定義してください。空セクションとテンプレート実物の解析テストを追加してください。

### [🟡 Minor] 別 change と「触る箇所が重ならない」という記述は現状と合わない

**該当箇所**: `proposal.md:25`

**問題点**: `backport-release-workflow-hardening/tasks.md:20-24` は publish の artifact 取得順を変更し、CI に自己テストを追加します。本変更もノート artifact の publish への受け渡しと CI の自己テスト追加を行うため、同じ workflow 領域を編集します。概念スコープは分離されていますが、実装箇所は分離されていません。

**推奨修正**: 「責務は分離するが実装箇所は重なる」と訂正し、適用順または rebase 後に deployment 引き継ぎ順序を再検証するタスクを追加してください。

### [🟡 Minor] 「重複なく」の意味が定義されていない

**該当箇所**: `specs/release-workflow/spec.md:69`、`design.md:49`

**問題点**: design は同じ PR が複数 commit に紐づいた場合の PR 単位の重複除去を述べていますが、spec の「重複なく」は、別 PR に同じ説明が書かれた場合の本文重複除去とも読めます。

**推奨修正**: PR ID だけを重複除去し、異なる PR の同文項目は出所が異なるため両方残す、と明記してください。

## アクションプラン

1. handbook とスキルの手順所有境界を決め直す。
2. ADR-0030・proposal・explorationを、下書き範囲と起点選択の refined rule に統一する。
3. 初回リリース・対象ゼロ件・比較リンクの契約を確定する。
4. handbook／AGENTS 更新とスキル有効化の順序を定める。
5. 実 GitHub API 経路と pagination を検証可能にする。
6. lint 文言、PR テンプレート、別 change との適用順、重複定義を補完する。

## 突き合わせ結果

ホスト側の自己レビューでは新たな指摘なし。相方の指摘 10 件は**全件を採用**した。うち 1 件 (Major 5) は Kasane の規律との関係でオーナーの判断を仰ぐ。

| # | 指摘 | 採否 | 検証 |
|---|---|---|---|
| Major 1 | handbook を正としながらスキルにも手順の順序を持たせている | 採用 | 「どの節をどの順に参照するか」は順序そのもの。handbook が単一のエントリポイントを持ち順序も所有する形へ一本化する |
| Major 2 | ADR-0030 の下書き範囲が design・spec と正反対 | 採用 | ADR-0030 の該当行が「直前のリリース以降の commit から」のまま残っており、二重掲載を防ぐ修正が ADR に反映されていなかった |
| Major 3 | Release ノートの起点規則が成果物間で一致していない | 採用 | proposal・exploration は「前回 tag」、ADR は「直前の Release」、design・spec のみ精緻化済み。「履歴上もっとも近い」が merge DAG 上で一意でない点も含めて統一する |
| Major 4 | 初回リリースでは必須の比較リンクを生成できない | 採用 | 起点 Release が無い場合を仕様化しながら、比較リンクを常に含める SHALL と両立していない。対象 0 件の本文も未定 |
| Major 5 | 実装完了から蒸留まで、リリース手順が実行不能になる | 採用 (扱いはオーナー判断) | handbook と AGENTS.md の更新を蒸留送りにしている一方、リリース用スキルは handbook を読む。蒸留前のスキルは削除済みスクリプトを指示する手順を読むことになる |
| Major 6 | GitHub API と job 間受け渡しの実経路が一度も検証されない | 採用 | dry-run が収集を一律で省略するため、認証・pagination・commit と PR の関連付け・artifact の受け渡し・ノートファイルの配線が本番まで未検証になる。取りこぼしは失敗せず不完全なノートとして静かに成功しうる |
| Minor 1 | 「version を検査しない」と日常 lint が矛盾して読める | 採用 | 検査しないのは release workflow での dispatch 入力との一致検査に限る、と限定が要る |
| Minor 2 | PR テンプレートが閉じた文法を破る危険がある | 採用 | 説明文や例をセクション内に置くと、未編集のテンプレートが必ず解析に失敗する |
| Minor 3 | 別 change と「触る箇所が重ならない」という記述は現状と合わない | 採用 | 双方が publish の artifact 受け渡しと CI の lint job を触る。責務は分離しているが実装箇所は重なる |
| Minor 4 | 「重複なく」の意味が定義されていない | 採用 | pull request 単位の重複除去と、本文の重複除去のどちらとも読める |

指摘数の推移: spec-001 が 9 件、spec-002 が 6 件、spec-003 が 10 件。減少していない。

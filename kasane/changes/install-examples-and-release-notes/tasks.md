# Tasks: install-examples-and-release-notes

## 1. インストール例のプレースホルダ化

- [x] 1.1 README 2 枚と利用者向け Skill 8 枚の計 14 行のインストール例で、version をプレースホルダ `{version}` に置き換える (→ Requirement: インストール例の version 表記)
- [x] 1.2 各インストール手順に、最新リリースへ解決される案内を置く (→ Requirement: インストール例の version 表記)
- [x] 1.3 README 2 枚の SwiftPM 節にある prerelease の説明を、調査結果に合わせて直す (`from:` は下限が prerelease なら prerelease も解決するが上限が次のメジャーまで開くため固定にならない。固定したいなら `exact:`) (→ Requirement: インストール例の version 表記)
- [x] 1.4 iOS の Skill 2 枚に、`exact:` を使う理由の説明が無い場合は追加する (→ Requirement: インストール例の version 表記)

## 2. インストール例の lint

- [x] 2.1 `scripts/` に lint を追加し、検査対象 (ファイル・宣言の種別・期待する本数) を表として持たせる。対象の知識は `set-readme-version.py` から移す (→ Requirement: インストール例の契約の検査)
- [x] 2.2 検査項目を実装する: プレースホルダであること / SwiftPM が `exact:` であること / 最新版の案内があること / en と ja が同じ Skill 名の集合を持ち対応する Skill が同じ種類・本数のインストール宣言を持つこと (→ Requirement: インストール例の契約の検査)
- [x] 2.2b `skills/en/*/SKILL.md` と `skills/ja/*/SKILL.md` の実構成を列挙し、対象表との未登録・余剰・言語差を失敗させる (→ Scenario: 検査対象に登録されていない Skill を検出する)
- [x] 2.3 lint に自己テストを足し、各検査項目が違反を検出できることを確かめる (→ Requirement: インストール例の契約の検査)
- [x] 2.4 `.github/workflows/ci.yml` の lint job でこの lint を走らせる (→ Requirement: インストール例の契約の検査)

## 3. 置換機構の撤去

- [x] 3.1 `.github/workflows/release.yml` の validate job からインストール例の検査 step を削除する (→ Requirement: インストール例の version 表記)
- [x] 3.2 `.github/workflows/ci.yml` から `set-readme-version.py --selftest` の呼び出しを削除する (→ Requirement: インストール例の version 表記)
- [x] 3.3 `scripts/release/set-readme-version.py` を削除する (`__pycache__` の残骸も含む) (→ Requirement: インストール例の version 表記)
- [x] 3.4 削除後、`scripts/` と `.github/` に `set-readme-version` への参照が残っていないことを確認する

## 4. GitHub Release の prerelease 印

- [x] 4.1 `.github/workflows/release.yml` の Release 作成から、version の suffix によって `--prerelease` を付ける分岐を削除する (→ Requirement: GitHub Release の発行)
- [x] 4.1b Release 作成時に、作成する Release を最新として明示的に指定する (自動判定に委ねない) (→ Requirement: GitHub Release の発行)
- [x] 4.2 既に発行済みの `0.1.0-beta.1` と `0.1.0-beta.2` の prerelease 印を解除する (→ Requirement: GitHub Release の発行)
- [x] 4.3 解除後に最新リリースへの案内が期待どおり解決することを確認する (→ Scenario: 最新版の案内から目的の版に着地できる)

## 5. Release ノートの組み立て

- [x] 5.1 `scripts/release/` に、pull request 本文の集合を受け取ってノート本文を返すスクリプトを追加する。解析規則は design.md の Decision 3 に従う (認識できない非空行・不正な種別・空の説明・重複見出し・空セクション・`- none` の共存はすべて失敗) (→ Requirement: Release ノートの内容)
- [x] 5.1b 起点の決め方を実装する: 今回の version ではなく、draft でない公開済みで、tag が `main` の first-parent 上で対象 commit の祖先である Release のうち、first-parent 上でもっとも近いもの。該当が無ければ履歴の最初から (→ Requirement: Release ノートの内容)
- [x] 5.1c 対象の決め方を実装する: 起点から今回の commit までの commit に紐づく pull request のうち、base が `main` のものに限る (→ Scenario: 開発ブランチ宛ての pull request は対象にならない)
- [x] 5.2 5.1 に自己テストを足し、次を検査する: 種別ごとのまとめと並び順 / 項目の無い種別の見出しを出さないこと / 複数 pull request 分の連結と出所の表示 / 同じ pull request が複数 commit に紐づく場合に 1 度だけ数えること / 異なる pull request の同じ文面を両方残すこと / `- none` だけの pull request / セクションが無い入力での失敗 / 項目が 1 つも無い空のセクションでの失敗 / 不正な種別での失敗 / 認識できない非空行での失敗 / 空の説明での失敗 / 見出しが 2 つある入力での失敗 / `- none` と項目の共存での失敗 / pull request テンプレートの未編集状態を受理すること (→ Scenario: ノートの組み立てが単独で検査できる)
- [x] 5.2b 対象選定 (起点と pull request の絞り込み) の自己テストを足す。模した Release 一覧と一時的な git 履歴を使い、次を検査する: draft の Release / 対象 commit の祖先でない Release / Release を持たない tag / 今回の version の tag が既にある場合 / 同じ pull request が複数の commit に紐づく場合 / base が `main` でない pull request / 公開済み Release が 1 つも無い場合 (→ Requirement: Release ノートの内容)
- [x] 5.3 validate job で対象の収集・検査・整形を一度だけ行い、確定したノート本文と対象 pull request の番号・起点 tag・対象 commit を成果物として保存する (→ Scenario: 検査の後に本文が編集されても公開内容が変わらない)
- [x] 5.3b validate job の `permissions` を完全な形 (`contents: read` と `pull-requests: read` の両方) で書く。job レベルの指定は列挙しない権限を `none` にするため、片方だけ足すと checkout が壊れる (→ Requirement: Release ノートの内容)
- [x] 5.3d API を呼ぶ step に token を明示的に渡す (→ Requirement: Release ノートの内容)
- [x] 5.3c リハーサル (`dry-run`) は、`main` から起動した場合は収集・検査・整形・受け渡しまで本番と同じ経路で行い (Release は作らない)、`main` 以外から起動した場合は収集と検査を行わない分岐を入れる (→ Scenario: main からのリハーサルでは実際の経路を通る)
- [x] 5.3e GitHub API のページ送りを実装し、1 ページに収まらない件数でも取りこぼさないことを自己テストで検査する (取りこぼしは失敗せず不完全なノートとして成功するため) (→ Requirement: Release ノートの内容)
- [x] 5.4 Release 作成の step は 5.3 の成果物をそのまま本文に使う。pull request 本文を読み直さない (→ Scenario: 検査の後に本文が編集されても公開内容が変わらない)
- [x] 5.4b 起点がある場合は比較の位置を末尾に含め、初回 (起点なし) は含めずに最初のリリースであることを示す。対象が 0 件または全て「変更なし」の場合の本文も決める (→ Scenario: 初回のリリースでは比較の位置を含めない / 対象が無いときも本文が決まる)
- [x] 5.5 `.github/release.yml` を削除する (→ Requirement: Release ノートの内容)
- [x] 5.6 pull request のテンプレートに `## Changes` セクションの雛形を置く。**セクション内には解析が受理する行だけを置き** (初期状態は `- none` のみ)、種別の一覧や記入例は次の見出し以降に置く。未編集のテンプレートがそのまま解析を通ることを確認する (→ Scenario: 記載の無い pull request があれば publish の前に止まる)

## 6. リリース用スキル

- [x] 6.1 `.agents/skills/release/SKILL.md` を作り、`.claude/skills/release` を symlink として張る (docs-refresh と同じ形) (→ Requirement: リリース手順の定型実行)
- [x] 6.2 スキルは実行時に handbook の単一の入口を読み、書かれた順に従う形にする。どの節をどの順に読むかをスキルに持たせない (順序の複製になるため) (→ Scenario: 手順を変えたときに道具を直さずに済む)
- [x] 6.2b handbook `cross/release-procedure.md` が「上から順に読めば 1 回のリリースが完了する」単一の入口になっているかを確認し、なっていなければ蒸留での改訂内容にその整備を含める (→ Requirement: リリース手順の定型実行)
- [x] 6.3 これから作る pull request が `main` へ持ち込む差分 (`origin/main..origin/develop` 相当) から `## Changes` の下書きを作る手順をスキルに持たせる。前回のリリース以降の全変更を範囲にしない (→ Scenario: 先行する pull request の変更が下書きに混ざらない)
- [x] 6.5 事前確認の段をスキルに持たせる (開発ブランチの検証 CI の状態、docs-refresh の依頼の要否) (→ Scenario: 事前確認が実行される)
- [x] 6.6 リリース pull request の作成・release の起動・実行の見守り・公開後の確認の各段をスキルに持たせ、各段の到達状態が分かる形にする (→ Scenario: 各段が順に実行できる)
- [x] 6.7 失敗を検出したときに handbook の失敗時の記述の該当箇所を示す段をスキルに持たせる。再実行の可否は人に返す (→ Scenario: 失敗したときに手順書の該当箇所が示される)
- [x] 6.4 スキルが判断を担わないことを明記する (version 番号・下書きの最終文面・再実行の可否は人が決める) (→ Requirement: リリース手順の定型実行)

## 7. 移行

- [x] 7.1 実装の着手時点で、`0.1.0-beta.2` の tag 以降に `main` へマージされた pull request が 0 件のままであることを確認する (0 件でなくなっていれば、該当 pull request の本文へ `## Changes` を遡って追記する。初回に限る例外の経路は設けない) (→ design.md の Migration Plan 2)
- [x] 7.3 4.2 (prerelease 印の解除) を 1.2 (案内の設置) より先に行う (→ design.md の Migration Plan 3)
- [x] 7.4 3.3 (script の削除) を 1.1 と 2.4 の完了後に行う (→ design.md の Migration Plan 4)

## 8. 検証

- [x] 8.1 lint とノート組み立てスクリプトの自己テストが全件通ることを確認する
- [x] 8.2 意図的に契約を破った状態 (具体 version・`from:` への書き換え・片言語だけの追加) を作り、lint が落ちることを確認する
- [x] 8.3 セクションを持たない pull request を模した入力で、validate の検査が落ちることを確認する
- [ ] 8.3b `dry-run` を `main` 以外のブランチから起動し、記載が無いことを理由に失敗しないことを確認する (→ Scenario: 開発ブランチからのリハーサルでは対象を集めない)
- [ ] 8.3c `dry-run` を `main` から起動し、収集・検査・成果物への受け渡しまでが実際に走ることを確認する (→ Scenario: main からのリハーサルでは実際の経路を通る)
- [x] 8.7 先行する change ([backport-release-workflow-hardening](../backport-release-workflow-hardening/tasks.md)) が既に適用されている場合、publish job の成果物の受け渡し順序と `ci.yml` の lint job の構成を再確認する (双方が同じ箇所を触るため)
- [ ] 8.4 `dry-run` で release を起動し、validate から消費者検証までが通ることを確認する
- [ ] 8.5 `dry-run` で生成された nupkg を展開し、同梱 README のインストール例がプレースホルダのままであることを確認する (→ Scenario: リリースしてもインストール例は変わらない)
- [ ] 8.6 プレースホルダのままのインストール例を写した消費者プロジェクトで、依存解決が失敗することを 3 経路について確認する (→ Scenario: 埋めずに使うと依存解決が失敗する)

## 9. 規範の追随 (実装と同時に行う)

削除するスクリプトや廃止する仕組みを指す記述を残すと、実装完了から蒸留までの間、リリース用スキルが実行不能な手順を読み、エージェントが存在しないスクリプトを指示される。このため次は蒸留送りにせず実装に含める (オーナー判断 2026-09-11)。

- [x] 9.1 `kasane/handbook/cross/release-procedure.md` の「リリース PR」節から version 置換の手順と `--check` の案内を外し、`## Changes` の記入を加える (→ Requirement: リリース手順の定型実行)
- [x] 9.2 同 handbook に、リリース用スキルの存在と「スキルはこの手順書の入口を読んで順に従う」という関係を書く (→ Scenario: 手順を変えたときに道具を直さずに済む)
- [x] 9.3 同 handbook が「上から順に読めば 1 回のリリースが完了する」単一の入口になるよう整える (6.2b で確認した不足を埋める) (→ Requirement: リリース手順の定型実行)
- [x] 9.4 `AGENTS.md` の 17 行目 (README 2 枚と Skill 8 枚の version を `set-readme-version.py` で機械置換してよいとする例外規定) を削除する。`CLAUDE.md` は `AGENTS.md` への symlink なので編集は 1 ファイル (→ Requirement: インストール例の version 表記)
- [x] 9.5 `kasane/handbook/cross/` に、インストール例の契約 (プレースホルダ・`exact:`・最新版の案内・en/ja の同一構成) を規約として 1 節置き、`index.md` の「適用のきっかけ」に登録する (→ Requirement: インストール例の契約の検査)
- [x] 9.6 リポジトリ全体を走査し、`set-readme-version.py` と `.github/release.yml` への参照が残っていないことを確認する (handbook・AGENTS.md・concepts・skills を含む)

## 蒸留への申し送り (実装タスクではない)

- `kasane/concepts/cross/architecture/release-pipeline.md` — validate の役割からインストール例の一致検査を外し、置換の記述を本決定に合わせ、Release ノートの出所を書く
- [cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md) と [cross/ADR-0030](../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md) を accepted へ昇格する。ADR-0029 の昇格時は [cross/ADR-0022](../../decisions/cross/0022-user-docs-as-agent-skills.md) に `amended-by: 0029` を足し、index にも改訂関係を書く
- KsDialogs へ `kind: decision` の知らせを出す — 同じ機構を翻案して「workflow が README を書く」を実装した直後の相手に、こちらが同期機構ごと撤去した判断と理由、Release ノートを pull request 本文から組み立てる方式、SwiftPM の prerelease 解決の事実を伝える。知らせ #2 (ラベル作成) がこちらでは不要になったことも含める

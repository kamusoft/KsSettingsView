# セカンドオピニオン: install-examples-and-release-notes (spec-001)
**相方**: codex / **label**: so-spec-install-examples-and-release-notes / **日付**: 2026-09-11 / **対象**: kasane/changes/install-examples-and-release-notes/ の proposal.md・design.md・specs/ 2 本・tasks.md・exploration.md、kasane/decisions/cross/0029・0030
---
# レビュー結果: install-examples-and-release-notes

**判定**: `NEEDS_DISCUSSION`  
**件数**: Critical 0 / Major 7 / Minor 2 / Suggestion 0

方針には合理性がありますが、Release 範囲、再実行、dry-run、PR 本文のスナップショット方法が未確定です。このまま実装すると、公開後に Release 作成だけ失敗する経路や、誤った範囲のノートを生成する経路が残ります。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/user-skill-api-listing.md`（`skills/` の変更）
- `kasane/handbook/cross/local-development-setup.md`（lint・消費者検証）
- `kasane/handbook/cross/release-procedure.md`（release workflow）
- accepted ADR 0019 / 0020 / 0022 / 0024 / 0028
- `kasane/concepts/cross/architecture/release-pipeline.md`
- 現行 workflow、README、Skill 8 本、関連スクリプト

## 指摘事項

### [🟠 Major] 「前回 tag」の選択規則が再実行・dry-run・非 Release tag を扱えない

**該当箇所**: `design.md:39`、`specs/release-workflow/spec.md:22`、`tasks.md:34`

**問題点**: 「前回 tag」が定義されていません。現行リポジトリには Release ではない `9.9.9` tag があり、単純な version 順では誤った起点になります。また、現行 workflow は同一 version の tag が対象 commit に存在する再実行を許しています（`.github/workflows/release.yml:109`）。GitHub Release 作成だけ失敗して再実行した場合、今回 tag を「前回」と選ぶ実装では範囲が空になります。

さらに accepted ADR-0020 と handbook は任意ブランチからの dry-run を許可していますが、まだ `main` PR に関連付いていない直接 push の commit からは `main` PR 本文を収集できません。

**推奨修正**:

- 起点を「今回 version を除く、直前の実在する GitHub Release に対応する tag」などと明文化する。
- 通常実行、今回 tag 作成済みの再実行、非 Release tag の混在、前回 Release 不在を Scenario 化する。
- 任意ブランチ dry-run を維持するか、open な `main` PR がある場合だけ許すか決める。後者なら ADR-0020 の改訂が必要。
- PR は `base=main` に限定することも仕様化する。

### [🟠 Major] validate した本文と公開時に使う本文を同一にする設計がない

**該当箇所**: `tasks.md:34`、`tasks.md:35`、`design.md:92`

**問題点**: task 5.3 は validate で PR 本文を検査し、5.4 は publish 終盤で再び組み立てスクリプトを呼びますが、検査済みデータを publish job へ渡す経路がありません。

publish までの間に PR 本文が編集されると、次のどちらかになります。

- 再取得した本文が不正になり、NuGet・Maven・tag の公開後に Release 作成だけ失敗する。
- validate 時と異なる本文で Release が作られる。

また現行 workflow はトップレベルで `contents: read` だけを指定しています。未指定権限は `none` になり、commit に関連する PR を認証付きで取得する API は `pull-requests: read` を要求します。[GitHub Actions の権限仕様](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)、[commit に関連する PR の API](https://docs.github.com/en/rest/commits/commits)

**推奨修正**:

- validate job で取得・解析・整形を一度だけ行い、確定したノート本文と対象 PR 番号・起点 tag・target SHA を artifact として publish job へ渡す。
- publish job は PR 本文を再取得せず、その artifact だけを `--notes-file` に渡す。
- validate job に最小権限の `pull-requests: read` を追加する。
- validate 後に本文が編集されても公開内容が変わらない Scenario を追加する。

### [🟠 Major] `## Changes` の入力文法と出力形式が閉じていない

**該当箇所**: `design.md:49`、`specs/release-workflow/spec.md:23`、`tasks.md:33`、`kasane/decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md:24`

**問題点**: 「`- ` で始まる行だけを読む」とすると、`* feature:`、空説明、通常の文章、コードフェンス内の箇条書きなどが静かに無視されます。これは design の「不正な入力を黙って無視しない」という理由と矛盾します。

次の扱いも未定です。

- `## Changes` が複数ある
- セクションが空
- `- none` と通常項目が共存する
- 説明が空、複数行、または同じ文面が複数 PR にある
- 同一種別内の順序
- 空カテゴリを出すか
- PR 番号・リンクを添えるか

ADR-0030 の「CI 整備は行を書かない」と、直後の「変更なしなら `- none` 必須」も文面上矛盾します。

**推奨修正**: 完全な入力文法と canonical な出力例を design/spec に置き、認識できない非空行は失敗、`- none` は単独のみ、説明は trim 後非空、見出しは一つだけ、という規則を決めて golden test を追加する。

### [🟠 Major] 初回だけ警告にする移行案が SHALL と矛盾する

**該当箇所**: `design.md:99`、`design.md:106`、`tasks.md:48`、`tasks.md:49`、`specs/release-workflow/spec.md:23`

**問題点**: spec は欠落 PR が一件でもあれば publish 前に失敗する SHALL ですが、Migration Plan と tasks は初回だけ警告に留める実装を未決のまま許しています。L 級提案の実装ゲートとしては未解決です。

**推奨修正**: 実装前に次のどちらかへ確定してください。

- 対象となる既存 PR すべてへ遡及追記し、例外を作らない。
- PR 番号または基準 tag を固定した期限付き移行例外を Requirement / Scenario に明記する。

単なる「初回」フラグは再実行時の意味が曖昧になるため避けるべきです。

### [🟠 Major] handbook と release Skill の責務境界が自己矛盾している

**該当箇所**: `design.md:81`、`specs/release-workflow/spec.md:64`、`tasks.md:42`

**問題点**: 手順の内容は handbook だけに置く一方、Skill に「段の順序と各段のコマンド」を書くとされています。順序とコマンドは手順そのものであり、コマンド変更時にどちらを直すべきか一意になりません。

**推奨修正**: handbook を正とする決定を維持するなら、Skill は実行時に handbook を必ず読み、そこに記載された順序・コマンドを使うルーターとしてください。Skill 固有の内容は、下書き生成ロジック、判断を人へ返す境界、handbook の参照先に限定します。

### [🟠 Major] accepted ADR を直接変更しつつ、提案 ADR には旧い誤認が残っている

**該当箇所**: `proposal.md:36`、`exploration.md:35`、`kasane/decisions/cross/0019-lockstep-single-version.md:36`、`kasane/decisions/cross/0029-install-examples-without-pinned-version.md:22`

**問題点**: accepted ADR-0019 の本文を直接書き換えていますが、Kasane の ADR 規律では accepted 後の本文は不変です。一方、ADR-0029:22 は依然として「`from:` が prerelease を解決しない」と述べ、修正後の ADR-0019・design・spec と矛盾しています。

**推奨修正**:

- ADR-0019 の本文変更を戻す。
- ADR-0029:22 の事実誤認を修正する。
- 決定自体を変えず根拠だけ訂正するなら、ADR-0019 の `現行照合` footer と concepts/handbook に訂正を記録する。
- 決定範囲も置き換えるなら ADR-0029 の `amends` 対象と置換範囲を正式に拡張する。

### [🟠 Major] prerelease 印を外すだけでは `/releases/latest` の保証にならない

**該当箇所**: `specs/release-workflow/spec.md:13`、`tasks.md:26`、`.github/workflows/release.yml:821`

**問題点**: Scenario は「作成した Release が最新として参照できる」と要求しますが、task は `--prerelease` の削除だけです。`gh release create` の `--latest` 既定値は「日付と version による自動判定」であり、今回 Release を明示的に Latest にする契約ではありません。[GitHub CLI `gh release create`](https://cli.github.com/manual/gh_release_create)

現行入力は alpha / beta / rc と正式版を広く許しており、単調増加も検査していません。また spec の「全 prerelease は印を付けない」と「0.x beta の間だけ」が一致していません。

**推奨修正**:

- 正式版未配信中は `--latest` を明示する、または入力 version の単調増加を検査する。
- 既存 Release 2 件のうちどれを Latest にするか明示する。
- alpha / rc / 正式版を入力した場合の扱いを決め、Scenario に追加する。

### [🟡 Minor] proposed ADR に `Revisit When` がない

**該当箇所**: `kasane/decisions/cross/0029-install-examples-without-pinned-version.md:45`、`kasane/decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md:46`

**問題点**: 両 ADR とも見直し条件を本文中には含みますが、現行 ADR 形式の `## Revisit When` がありません。

**推奨修正**: ADR-0029 は「正式版後に prerelease を Latest から除外する必要が生じたとき」、ADR-0030 は「main への取り込み単位またはブランチ運用が変わったとき」など、出典に基づく観測可能な条件を専用節へ置いてください。

### [🟡 Minor] 明示表だけでは新規・改名された Skill の検査漏れを検出できない

**該当箇所**: `design.md:72`、`specs/install-examples/spec.md:38`、`tasks.md:12`

**問題点**: 対象表に載っているファイルは厳密に検査できますが、新しい Skill が表へ追加されなかった場合、その存在自体を検出できません。「Skill が増えたときの検査漏れを防ぐ」という design の理由を満たしません。

**推奨修正**: `skills/en/*/SKILL.md` と `skills/ja/*/SKILL.md` の実構成を列挙し、対象表との未登録・余剰・言語差も失敗させる検査と自己テストを追加してください。

## 突き合わせ結果

ホスト側の自己レビューでは 1 件のみ検出 (tasks 5.6 が判断を実装時へ送っていた — 確定タスクへ修正済み)。相方の指摘 9 件のうち **8 件を採用、1 件を却下**した。

| # | 指摘 | 採否 | 検証 |
|---|---|---|---|
| Major 1 | 「前回 tag」の選択規則が再実行・dry-run・非 Release tag を扱えない | 採用 | `git tag -l` に Release を持たない `9.9.9` が実在することを確認。version 順で選ぶ実装は確実に誤る |
| Major 2 | validate した本文と公開時に使う本文を同一にする設計がない | 採用 | `release.yml:35-36` のトップレベル権限が `contents: read` のみであることを確認。PR 取得には `pull-requests: read` が要る。validate で確定させ artifact で渡す設計は、公開後に Release 作成だけ失敗する経路も塞ぐ |
| Major 3 | `## Changes` の入力文法と出力形式が閉じていない | 採用 | 認識できない非空行の扱い・`- none` との共存・見出しの重複・出力の並びと空カテゴリが未定であることを確認 |
| Major 4 | 初回だけ警告にする移行案が SHALL と矛盾する | 採用 | ただし調査により**移行措置そのものが不要**と判明。`0.1.0-beta.2` の tag 以降に `main` へマージされた pull request は 0 件で、次回リリースの範囲に入るのはこれから作る PR だけ。例外を作らずに済む |
| Major 5 | handbook と release Skill の責務境界が自己矛盾している | 採用 | 「段の順序と各段のコマンド」は手順そのものであり、「手順を複製しない」と両立しない |
| Major 6 | accepted ADR を直接変更しつつ、提案 ADR には旧い誤認が残っている | 一部採用 | **前半 (ADR-0019 の直接変更を戻す) は却下** — オーナーが「決定を修正するわけではないので直接修正、訂正の注記も不要」と裁定済み (2026-09-11)。一般規則より裁定が優先する。**後半は採用** — ADR-0029:22 に「`from:` が prerelease を解決しない」という訂正前の記述が残り、修正後の ADR-0019 と矛盾していることを確認 |
| Major 7 | prerelease 印を外すだけでは最新リリースの保証にならない | 採用 | `--prerelease` の削除だけでは Latest は自動判定に委ねられる。alpha / rc / 正式版を入力した場合の扱いも未定 |
| Minor 1 | proposed ADR に `Revisit When` がない | 採用 | `core/0030` と `core/0031` に同節の前例があることを確認 |
| Minor 2 | 明示表だけでは新規・改名された Skill の検査漏れを検出できない | 採用 | 表に載らない Skill はその存在自体を検出できない |

### 調査で判明した追加の事実

`0.1.0-beta.2` のノートには pull request #10 が載っているが、`gh pr list --base main --state merged` には #9 と #11 しか現れない。#10 は `develop` 宛ての pull request であり、**GitHub は base ブランチに関わらず commit に紐づく pull request を拾う**。Major 1 の推奨にある「`base=main` に限定を仕様化する」は、この挙動への対処として必要である。

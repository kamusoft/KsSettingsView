# Proposal: install-examples-and-release-notes

## Why

利用者から見えるリリース情報に、2 つの噛み合わせの悪さがある。

**インストール例の version 同期が割に合っていない。** README 2 枚と利用者向け Skill 8 枚の計 14 行に具体 version を書き、リリースのたびに `scripts/release/set-readme-version.py` で置換し validate が `--check` で検査している。この置換を workflow へ移す検討をしたところ、書き戻し先が `main` なら branch protection のバイパスが要り、`develop` なら利用者が見る既定ブランチが 1 リリースぶん古い version を示し続けることが分かった。維持している仕組み (script 566 行と自己テスト・validate の検査 step・handbook の手順・AGENTS.md の例外規定) に対し、得ているのは「README の行をそのままコピーできる」ことだけだった。

**Release ノートの分類が成立していない。** `.github/release.yml` が参照する 6 ラベルはリポジトリに 1 件も無い。さらにラベルを作っても、[cross/ADR-0028](../../decisions/cross/0028-ci-triggers-by-branch-role.md) の運用では `main` に入るのが集約 pull request だけであるため、分類できるのは集約の粒度でしかない。実際に `0.1.0-beta.2` のノートは集約 pull request 2 件が「その他」に落ちているだけである。

あわせて、最新版の案内先にしたい `/releases/latest` が、GitHub の prerelease 印によって現状 404 を返すことも分かった (正式版が 1 つも無いため)。

## What Changes

影響する能力: **install-examples** (新規) と **release-workflow**。

1. **インストール例から具体 version を外す** ([cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md)) — 14 行の version をプレースホルダ `{version}` にし、最新版は GitHub Releases への案内に委ねる。置換機構一式 (`scripts/release/set-readme-version.py`、validate job の検査 step、`ci.yml` の自己テスト呼び出し) を撤去する。
2. **インストール例の契約を lint で継続検査する** — プレースホルダであること・SwiftPM の宣言が `exact:` であること・Releases への案内があること・en と ja が同一構成であることを検査する lint を追加し、lint job で走らせる。
3. **GitHub Release の prerelease 印を廃止する** (同 ADR) — version の suffix から `--prerelease` を付ける分岐を外し、既発行の `0.1.0-beta.1` / `0.1.0-beta.2` の印も解除する。
4. **Release ノートを pull request 本文から組み立てる** ([cross/ADR-0030](../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md)) — 直前の公開済み Release (draft でなく、その tag が `main` の first-parent 上で対象 commit の祖先であるもののうち最も近いもの) 以降に `main` へマージされた pull request の `## Changes` セクションを集めて連結し、比較リンクを付ける。`.github/release.yml` は廃止し、分類用ラベルは作らない。
5. **リリース用スキルを追加する** (同 ADR) — handbook を読んで順に実行する薄い層として `.agents/skills/release/` を置く。`## Changes` の下書き生成を含む。

6. **規範の追随を実装に含める** — handbook `cross/release-procedure.md` の改訂、`AGENTS.md` の例外規定の削除、インストール例の契約の規約化を、蒸留送りにせず実装タスクとして行う。削除するスクリプトを指す記述が残ると、実装完了から蒸留までの間、リリース用スキルが実行不能な手順を読むため (オーナー判断 2026-09-11)。

## Non-Goals

- **publish 内部の堅牢化 (レジストリ照会の状態分類・Maven 公開待ちの上限・deployment ID の引き継ぎ)**: [backport-release-workflow-hardening](../backport-release-workflow-hardening/proposal.md) が扱う。責務は分離しているが、**実装箇所は重なる** — 双方が publish job の成果物の受け渡しと `ci.yml` の lint job を触る。後に実装する側は、先行する変更の上で deployment ID の引き継ぎ順序と lint job の構成を再確認する。
- **ブランチ運用の変更 (個別の変更を pull request 化する)**: ADR-0030 で却下した。ノートの分類のために日常の開発フローを変えない。
- **`main` の branch protection の変更**: ADR-0029 で却下した。
- **利用者向け Skill と README の本文の見直し**: 触るのはインストール例の version 表記と Releases への案内に限る。記述全体の追従更新は docs-refresh の責務 ([cross/ADR-0022](../../decisions/cross/0022-user-docs-as-agent-skills.md))。

## Impact

- 破壊的変更: 公開 API と配布物の内容は変わらない。**利用者向けドキュメントの契約は変わる** — インストール例をコピーした利用者は version を 1 箇所差し替える必要が生じる。
- 運用の変更: `main` 宛ての pull request を作るたびに `## Changes` セクションを書く必要が生じる (リリース用スキルの下書き生成で相殺する)。セクションを持たない pull request が範囲にあると release が止まる。
- 影響範囲: `README.md` / `README_ja.md`、`skills/{en,ja}/` の 8 枚、`scripts/release/set-readme-version.py` (削除)、`scripts/` の lint 1 本 (新規)、`.github/workflows/release.yml`、`.github/workflows/ci.yml`、`.github/release.yml` (削除)、`.agents/skills/release/` (新規)、`kasane/handbook/cross/release-procedure.md`、`AGENTS.md` / `CLAUDE.md`、既発行の Release 2 件。
- リスク: Release ノートの組み立ては次回の本番リリースまで実地で確認できない。`dry-run` では Release を作らないため、組み立て処理単体を検査できる形にする。
- 既存 ADR との関係: [cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md) (proposed、[cross/ADR-0022](../../decisions/cross/0022-user-docs-as-agent-skills.md) を一部改訂) と [cross/ADR-0030](../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md) (proposed) を起票済み。[cross/ADR-0019](../../decisions/cross/0019-lockstep-single-version.md) の帰結行にあった SwiftPM の prerelease 解決についての事実誤認は、調査結果に基づき直接修正済み。

## 級: L

触る能力が複数 (リリース機構と利用者向けインストール例) にまたがり、外部に見える面も複数 (GitHub Release の見え方・README 2 枚・Skill 8 枚・配布物同梱 README) に及ぶ。新しい道具 (リリース用スキル) を 1 つ増やし、覆すコストの高い判断を 4 つ含む。

domain: cross

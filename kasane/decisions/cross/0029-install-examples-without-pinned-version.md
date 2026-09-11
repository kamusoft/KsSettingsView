---
id: 0029
title: インストール例は具体 version を持たず、最新版は GitHub Releases が示す
status: proposed
date: 2026-09-11
amends: 0022
---

## Context

README 2 枚と利用者向け Skill 8 枚のインストール例には、公開した version を具体的に書く運用があった。リリースのたびにオーナーがリリース PR (`develop` → `main`) の中で `scripts/release/set-readme-version.py <version>` を実行して置換 commit を積み、release workflow の validate job が `--check` で不一致を止める。

この置換をリリース手順から消すため、workflow に書かせる案を検討したところ、書き戻し先がどこにも収まらないことが分かった。

- `main` へ直接 commit する案は、`main` の branch protection が pull request を必須にしており必須 status check も 7 件あるため、`GITHUB_TOKEN` では push できない。`enforce_admins` が false なので管理者の PAT なら通るが、長命の書き込み資格情報を secrets に置くことになる (このリポジトリは NuGet publish を Trusted Publishing にして長命 key を持たない方針)。ruleset へ移して bypass actor を登録する道もあるが、リリース対象ブランチの保護に CI のバイパス経路を作ることになる。
- `develop` だけに書き戻す案は protection に触れずに済むが、`main` が追いつくのは次のリリース PR なので、**既定ブランチが常に 1 リリースぶん古い version を示し続ける**。GitHub で README を読む利用者が見るのは `main` であり、手作業方式より正確さが下がる。

そこで維持している仕組みの重さを見直した。`set-readme-version.py` (566 行) とその自己テスト、validate の検査 step、handbook のリリース手順 1 つ、AGENTS.md の例外規定を抱えて得ているのは「README の行をそのままコピーできる」ことだけである。

なお、インストール例に具体 version を書くことを決定として定めた accepted ADR は存在しない (cross/ADR-0020 の該当帰結行は 2026-09-07 の整理で除かれている)。手順と記述と道具にだけ存在する運用である。

GitHub Releases を最新版の案内先にするにあたって、GitHub の prerelease 印が障害になった。`/releases/latest` は prerelease を除外するため、正式版が 1 つも無い現状では 404 を返す。GitHub の prerelease 印と semver の prerelease は別物であり、配信経路 (SwiftPM は git tag、NuGet と Maven は各レジストリ) はどれも GitHub Releases を参照しない。SwiftPM が prerelease を解決するかどうかは version 要件の下限が prerelease を持つかで決まり (cross/ADR-0019)、GitHub の印とは無関係である。

## Decision

- **インストール例は具体 version を持たない。** version の位置にはプレースホルダ `{version}` を置く。
  - プレースホルダは version として解決できない形にする。埋め忘れは依存解決の失敗として必ず露見し、古い version が黙って入ることがない。
  - `<version>` は使わない。NuGet の例が XML 属性値 (`Version="..."`) であり、山括弧が XML を壊すため。
- **最新版は GitHub Releases が示す。** README と利用者向け Skill は、インストール手順の位置から Releases への案内を持つ。
  - これに伴い、[cross/ADR-0022](0022-user-docs-as-agent-skills.md) の「SKILL.md と references は Skill 外のファイル・URL への参照を持たない」という条項を、**URL の有無ではなく目的による境界**へ置き換える (オーナー判断 2026-09-11)。
    - **持たない**: Skill 外の文書へ知識を委ねる参照。索引・兄弟 Skill・リポジトリ内部の文書への言及はスキル名のみとし、リンクを張らない (ADR-0022 の続く一文が定める扱いをそのまま引き継ぐ)。
    - **持ってよい**: 操作の対象となる URL。配布座標、最新版の確認先、Issue の窓口、frontmatter の `source` がこれに当たる。
    - この置き換えは、条項の字面が既に実態と合っていないことにも対応する。現行の Skill には配布リポジトリ・本体リポジトリ・`metadata.source` の URL が既に存在する。
    - ADR-0022 の他の決定 (分割軸・2 言語のロックステップ・manifest・知識の正の所在など) は維持する。
- **リリース時の置換機構を撤去する。** `scripts/release/set-readme-version.py`、validate job の検査 step、`ci.yml` の自己テスト呼び出し、handbook のリリース PR 手順、AGENTS.md / CLAUDE.md の例外規定を削除する。配布物の生成時にも置換しない (置換する対象が無い)。
- **0.x の beta を配信している間、GitHub Release に prerelease の印を付けず、作成する Release を明示的に最新として指定する。** 印を外すだけでは最新の選別が自動判定に委ねられるため、案内先が意図した版に着地する保証にならない。 version 文字列の semver prerelease 表記 (`X.Y.Z-{alpha|beta|rc}.N`、cross/ADR-0019) は従来どおり維持する。既に発行済みの `0.1.0-beta.1` / `0.1.0-beta.2` の印も解除する。
  - 0.x の beta を配信している間は、入力される version が alpha / beta / rc のいずれであっても同じ扱いとする。正式版を配信した後の扱いは Revisit When に従って決め直す。
- prerelease の期間に SwiftPM で `exact:` を使う必要があること (cross/ADR-0019 の帰結) は、引き続き散文で案内する。

## Alternatives Considered

- **置換を release workflow が行い `develop` へ書き戻す**: 却下。既定ブランチ `main` が 1 リリースぶん古い version を示し続け、利用者が実際に見る場所の正確さが手作業方式より下がる。
- **release workflow が `main` へ直接 commit する**: 却下。pull request 必須と必須 status check 7 件を CI にバイパスさせる (管理者 PAT か ruleset の bypass) 代償に対して、得られる結果は手作業方式と同じ「`main` が公開版を指す」でしかない。
- **shields.io 等のバッジで最新版を示し、インストール例の literal が古いことを許容する**: 却下。現在の README は表示に効く第三者依存を 1 つも持たない (画像はすべて GitHub 自身)。最新版の案内は GitHub の Releases で足りるため、その純度を崩す理由がない。
- **手作業のまま `--check` をリリース PR の必須 status check に加える**: 却下。置換忘れは早く捕まるようになるが、置換機構一式を維持し続ける理由は「コピーできる行」1 点のままで釣り合わない。
- **案内先を Releases の一覧ページにする**: 却下。prerelease の印を廃止すれば `/releases/latest` が機能し、目的の版のページへ直接着地できる。
- **プレースホルダを `X.Y.Z` にする**: 却下。それ自体が version の形をしているため、埋めずに残ったまま見過ごされる余地と、エージェントが実在の version と誤認する余地が残る。

## Consequences

- 正: リリースのたびの置換・検査・書き戻しが消える。陳腐化する対象が存在しなくなるため、同期の失敗という事象そのものが無くなる。
- 正: ブランチの選択・branch protection のバイパス・第三者サービスへの依存がいずれも不要になる。
- 正: プレースホルダの埋め忘れは依存解決の失敗として露見する。古い literal が黙って古い版を入れるより安全側に倒れる。
- 正: `/releases/latest` が最新版の単一の案内先になる。
- 負: 利用者はコピーしたインストール例の version を 1 箇所差し替える必要がある。
- 負: ネットワークを持たないエージェント (利用者のプロジェクトへ Skill をコピーして使う閉じた世界、cross/ADR-0022) は Releases を引けず、利用者に version を確認することになる。
- 負: GitHub 上で「これは beta である」ことを示す印が version 文字列と README の記述だけになる。
- 負: 正式版の配信後に RC を Latest から外したくなった場合は、prerelease の印を戻す判断が要る。

## Revisit When

- 正式版 (`0.1.0` 等、semver の prerelease 表記を持たない版) を配信したとき — 正式版を最新のリリースに保ったまま候補版を出す必要が生じうるため、prerelease の印と最新の指定の扱いを決め直す
- 利用者から「インストール例をそのままコピーしたい」という要望が繰り返し届いたとき — プレースホルダ方式が負わせている手間が、同期機構の重さを上回っていないかを見直す

出典: kasane/changes/install-examples-and-release-notes/exploration.md (分離前は kasane/changes/backport-release-workflow-hardening/exploration.md)

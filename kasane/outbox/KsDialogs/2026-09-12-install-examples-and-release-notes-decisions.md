---
from: KsSettingsView
to: KsDialogs
kind: decision
date: 2026-09-12
source: kasane/decisions/cross/0029-install-examples-without-pinned-version.md / kasane/decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md
---

# インストール例の version 同期を機構ごと撤去し、Release ノートは pull request 本文から組み立てる決定

## 何を決めたか

### cross/ADR-0029: インストール例は具体 version を持たない

インストール例の version をリリースのたびに文書へ書き戻す経路を、仕組みごと捨てた。置換を workflow に書かせる案を検討した結果、**書き戻し先がどこにも収まらない**ことが分かったのが起点。

- `main` へ直接 commit する案は、branch protection の pull request 必須と必須 status check 7 件を CI にバイパスさせることになる (管理者 PAT を secrets に置くか、ruleset に bypass actor を登録するか)。得られる結果は手作業と同じ。
- `develop` だけに書き戻す案は protection に触れないが、既定ブランチ `main` が常に 1 リリースぶん古い version を示し続ける。GitHub で README を読む利用者が見るのは `main` であり、**手作業方式より正確さが下がる**。

そこで維持コスト (置換スクリプト 566 行 + 自己テスト + validate の検査 step + handbook の手順 + AGENTS.md の例外規定) と得ているもの (「README の行をそのままコピーできる」) を並べ、割に合わないと判断した。プレースホルダは `{version}`。**version として解決できない形にするのが要点**で、埋め忘れは依存解決の失敗として必ず露見する (`X.Y.Z` はそれ自体が version の形なので採らなかった)。

あわせて、SwiftPM が prerelease を解決するかどうかは **version 要件の下限が prerelease を持つかで決まり、GitHub の prerelease 印とは無関係**であることを確認した。配信経路 (SwiftPM は git tag、NuGet と Maven は各レジストリ) はどれも GitHub Releases を参照しない。

### cross/ADR-0030: Release ノートは `main` 宛て pull request 本文の `## Changes` から組み立てる

`.github/release.yml` のラベル分類が成立していなかった。参照する 6 ラベルがリポジトリに 1 件も無く、さらにラベルを作っても、集約 pull request だけが `main` に入る運用では**分類できるのが集約の粒度でしかない**。集約 pull request に除外ラベルを付ければ、その中の利用者向け変更ごとノートから消える。

代わりに `main` 宛て pull request 本文の `## Changes` セクション (`- <種別>: <説明>`、種別は breaking / feature / fix / docs、必須・変更が無ければ `- none`) を集めて連結する。収集の起点は「draft でない公開済み Release のうち、その tag が `main` の first-parent 上で対象 commit の祖先であり、もっとも近いもの」。収集と検査は validate 段で一度だけ行い、artifact で publish へ渡す (検査を通した本文と公開されるノートを同一にするため)。

## 相手に関係する理由

そちらは「workflow が README を書く」方式を実装した直後であり、こちらの決定はその真逆に進んでいる。書き戻し先の問題 (branch protection のバイパス / 既定ブランチの古さ) は機構の性質から来るもので、そちらの構成でも同じ形で現れる可能性が高い。

また、そちらの知らせ #2 (Release ノート分類用の 6 ラベルを作る) は、こちらではこの決定により**目的を失った**。ラベルによる分類機構は持たない。

## 提案する対応

- 書き戻し方式を続けるなら、**既定ブランチが指す version が公開版と一致しているか**を一度確認しておくとよい。`develop` へ書き戻す構成だと、GitHub で README を読む利用者には 1 リリースぶん古い版が見え続ける。
- SwiftPM を配信しているなら、prerelease 期間の解決規則 (要件の下限が prerelease を持つかで決まる) は GitHub の印と切り離して扱えることを前提にできる。
- ラベル方式を採るかどうかは、そちらの `main` への取り込み単位次第。集約 pull request 方式なら、こちらと同じ理由で分類の単位が変更の単位と噛み合わない。

# Exploration: backport-release-workflow-hardening

## 課題 / 動機

KsDialogs (sibling) からの知らせ `../KsDialogs/kasane/outbox/KsSettingsView/2026-09-10-release-workflow-fixes-and-improvements.md` (kind: change / 2026-09-10) を受けた探索。

KsSettingsView の `release.yml` / `scripts/release/` / `.github/release.yml` / handbook release-procedure を KsDialogs へ翻案し初回リリース `0.1.0-beta.1` に到達する過程で、翻案元 (= このリポジトリ) にもある疑いの不具合 2 件と、翻案先で足した改良 5 件が挙がった。`overlap` の `.github` と `scripts` に当たる。

現物を確認した結果、**7 件のうち実際に手当てが要るのは 3 件** (#2 / #5 / #7) と、**逆流を決めた 1 件** (#3) の計 4 件に絞れた。

### 現状確認の結果 (分割前の 7 件について)

以下は 2026-09-11 の分割より前に、知らせの 7 件すべてを対象として行った現状確認である。この change の現在のスコープは #5 / #6 / #7 と待機スクリプトの自己テスト接続で、#2 / #3 は [install-examples-and-release-notes](../install-examples-and-release-notes/exploration.md) へ移した。

| # | 知らせの項目 | KsSettingsView の現状 | 判定 |
|---|---|---|---|
| 1 | `check-signatures.sh` の自己テストが無言で終わる | 当該スクリプトに自己テストが存在しない。自己テストを持つのは `central-portal.sh` と `set-readme-version.py` の 2 本で、`$( ... \|\| true )` 内で `exit` する構造は `scripts/release/` に無い (`\|\| true` の 2 箇所はいずれも診断出力用で判定に絡まない) | 該当なし |
| 2 | `.github/release.yml` が参照するラベルが未作成 | リポジトリのラベルは GitHub 既定の 9 件のみ。`breaking` / `feature` / `fix` / `docs` / `kasane` / `ci` は 1 件も無く、Release ノートの分類が全て「その他」に落ち、`kasane` / `ci` の除外も効いていない | **要対応 (実在)** |
| 3 | README / Skill の version 置換を workflow が行う | 人手 (handbook `cross/release-procedure.md:119-124`) + release.yml validate job の `--check` (`.github/workflows/release.yml:152-155`)。nupkg 同梱 README は pack が `main` (置換済みツリー) で走るため既に実値 | **逆流しない。同期機構ごと撤去する (ADR-0029)** |
| 4 | 再実行の続行判定を印 (marker) で行う | `github.run_attempt` を `.github/` `scripts/` `kasane/` のどこでも使っていない。各 step を冪等に書き、tag は commit 照合で守る (`release.yml:109-123`、`:796-810`)。別 commit を指す tag があれば失敗する | 該当なし (Critical の原因が無い) |
| 5 | nuget.org の照会を fail-closed に | 公開**前**の存在照会は無く、重複防止は `dotnet nuget push --skip-duplicate` のみ。公開**後**の待機 `scripts/release/wait-for-registries.sh:56-78` `nuget_ready()` だけ HTTP status 分岐が無く、通信失敗・空応答・5xx を「未反映」に畳み込む。Maven 側 (`maven_central_ready()` `:46-53`、`central-portal.sh:316-335`) は 200/404/その他を分けており非対称 | **要対応 (小)** |
| 6 | 失敗経路の deployment ID 書き戻しをガードする | 書き戻し 3 経路 (`release.yml:665-687`、`:689-696`、`:850-884`) はいずれも既にガード済みで、空上書きも「実際に消えた」確認つき。引き継ぎ ID の読み込みが artifact download の後ろにある構造だけが残るが、その経路で落ちれば後続の maven step も走らず引き継ぎ artifact は無傷 | ほぼ対応済み |
| 7 | Maven Central の公開待ちを release 要求の後に 1 本で | `release` + `wait-published` は既に同一 step (`release.yml:765-774`)。残るのは**上限が 1800 秒 (30 分)** であること (`central-portal.sh:220-221`、`:262-263`、環境変数 `KSR_POLL_TIMEOUT_SECONDS`)。KsDialogs の初回は Central の同期に約 60 分かかった。job の `timeout-minutes: 120` は「30 分待ち × 3 本 + 本体 11 分」で算出されている (`release.yml:429-434`) | **要対応** |

`scripts/release/` に `check-resume-eligibility.sh` / `check-nuget-version.sh` に相当するものは存在しない (6 本構成)。

## 検討した選択肢 (却下案と理由を含む)

### スコープ

- **#2 + #5 + #7 の S 級に絞り、#3 は別途判断**: 却下。同じ release 機構の面を二度触ることになる。
- **#7 だけ先行**: 却下。
- **#3 を含めた M 級 1 本**: 採用。

### #3 をどう扱うか (議論の経過)

当初は「置換の担い手を人から workflow へ移す」方向で検討し、accepted な [cross/ADR-0020](../../decisions/cross/0020-release-dispatch-tag-last-version-injection.md) が却下していた案の改訂として進めた。却下理由の有効性は次のとおりだった。

- 理由 1「`main` への push 権限と branch protection の bypass が要る」は `main` 固有。[ADR-0028](../../decisions/cross/0028-ci-triggers-by-branch-role.md) で `develop` の必須 status check は撤去済みのため、`develop` へ書けば当たらない
- 理由 2「CI が bump commit を積まない原則に反する」の対象は version の宣言値ファイルであり、README / Skill のインストール例はドキュメント。その置換 commit は現に毎リリース人が積んでいる

しかし書き戻し先を詰める過程で、**どちらに書いても成立しない**ことが分かった。

- `develop` へ書き戻すと、`main` が追いつくのは次のリリース PR なので、**既定ブランチが常に 1 リリースぶん古い version を示し続ける**。GitHub で README を読む利用者が見るのは `main` であり、手作業方式より正確さが下がる (既定ブランチは `gh repo view` で `main` と確認)
- `main` へ直接 commit するには、pull request 必須と必須 status check 7 件をバイパスする必要がある。`enforce_admins` が false なので管理者 PAT なら通るが長命の書き込み資格情報を置くことになり、ruleset へ移して bypass actor を登録する道もリリース対象ブランチの保護に穴を開ける。しかも得られる結果は手作業方式と同じ

第三者バッジ (shields.io) で最新版を示し literal の陳腐化を許容する案も検討したが、現在の README は表示に効く第三者依存を 1 つも持たない (画像はすべて `raw.githubusercontent.com`) ため、その純度を崩す理由が弱い。

ここで前提に立ち返り、**同期機構そのものが割に合っていない**という判断に至った。維持しているのは `set-readme-version.py` 566 行と自己テスト・validate の検査 step・handbook の手順 1 つ・AGENTS.md の例外規定であり、得ているのは「README の行をそのままコピーできる」ことだけ。インストール例から具体 version を外せば、陳腐化する対象が存在しなくなり、ブランチ・protection・第三者依存の問題がすべて消滅する。

あわせて GitHub の prerelease 印を扱った。`/releases/latest` は prerelease を除外するため、正式版が無い現状では 404 を返す (`gh api repos/:owner/:repo/releases/latest` で確認)。GitHub の prerelease 印と semver の prerelease は別物で、配信経路はどれも GitHub Releases を参照しない (SwiftPM は git tag、NuGet と Maven は各レジストリ) ため、印を外しても利用者の解決結果は変わらない。

プレースホルダの形は `{version}` を採った。`<version>` は NuGet の例が XML 属性値 (`Version="..."`) であるため使えない。`X.Y.Z` は version の形をしているため埋め忘れと誤認の余地が残る。

### 探索の途中で訂正した見立て

- 「こちらは nupkg README が既に実値で `develop` にも version が入っているので、KsDialogs の `develop` 書き戻し経路は不要」— 誤り。現状 `develop` に version が入っているのは人がリリース PR で置換しているからで、置換を workflow に移せば書き戻し先が必要になる
- 「Maven 側は HTTP status を分けており nuget 側だけが非対称」— 誤り。`central-portal.sh published` には当たるが、対象の `wait-for-registries.sh` の `maven_central_ready()` は status を取得しながら 200 以外を一様に扱う (相方レビュー Major 3 で判明)

## 決定事項

- #1 / #4 は該当なし。#1 は直す対象が存在せず、#4 は `run_attempt` に依存した resume 判定を持たないこちらでは KsDialogs が Critical とした事象の原因が存在しない。
- #5 は Maven 側にも広げる (相方レビュー spec-002 Major 3: `wait-for-registries.sh` の `maven_central_ready()` も 200 以外を一様に扱う)。
- #6 は付随修正ではなく正式なスコープとして扱う (Requirement と Scenario を与えるため)。
- #3 は逆流しない。置換の自動化ではなく同期機構ごと撤去する。
- **2026-09-11 分割**: 利用者から見える契約 (インストール例の version・GitHub Release の prerelease 印・Release ノートの分類) を [install-examples-and-release-notes](../install-examples-and-release-notes/exploration.md) へ分離した。この change には publish 内部の堅牢化 (#5 / #6 / #7 と待機スクリプトの自己テストの CI 接続) だけを残す。分離の理由は、利用者向け契約の側に未解決の設計判断が 3 つ残る一方 (ブランチ運用とラベル分類の噛み合わせ・SwiftPM の prerelease 解決・新契約の継続検査)、堅牢化はそれに足止めされずに実装へ進められるため (相方レビュー spec-002 Major 5)。

## ADR 候補

なし。この change の範囲では新規・改訂ともに起票しない。

探索の過程で起票した [cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md) (proposed) は、分割により [install-examples-and-release-notes](../install-examples-and-release-notes/exploration.md) の持ち物となった。

## 未決の論点

この change の範囲には残っていない。利用者向け契約の側の未解決は [install-examples-and-release-notes](../install-examples-and-release-notes/exploration.md) が持つ。

実装時に確かめるのは、publish job の実行時間上限と各待ちの上限が算出式を満たしていること (tasks 5.3)。

## UI 素材 (ui/references/ の一覧と注釈)

なし (UI 変更を伴わない)

## 変更級の推奨: M

判定材料:

- 触る面: `.github/workflows/release.yml` の publish job (待機・step 順序・実行時間上限)、`.github/workflows/ci.yml` の lint job、`scripts/release/` の 2 本
- 公開 API の変更: なし。利用者向けドキュメントにも触れない
- 可逆性: 待機の挙動は `dry-run` では確認できず、経路全体は次回の本番リリースで確かめることになる
- UI: なし
- ADR: なし

# Exploration: install-examples-and-release-notes

## 課題 / 動機

`backport-release-workflow-hardening` の探索と提案から分離した (2026-09-11)。利用者から見えるリリース情報の契約に関わる 3 つの論点をまとめて扱う。分離の理由は、この 3 つに未解決の設計判断が残る一方、publish 内部の堅牢化は足止めせずに実装へ進められるため (相方レビュー `../backport-release-workflow-hardening/second-opinion-spec-002.md` の Major 5)。

扱う論点は次の 3 つ。

### 1. インストール例の version 同期をやめる

README 2 枚と利用者向け Skill 8 枚のインストール例 (計 14 行) に具体 version を書き、リリースのたびに `scripts/release/set-readme-version.py` で置換し validate が `--check` で検査する運用がある。この同期機構が割に合っていない、という判断に至った。経緯と却下案は [cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md) (proposed) に記録済み。

### 2. GitHub Release の prerelease 印をやめる

`/releases/latest` は prerelease を除外するため、正式版が無い現状では 404 を返す (`gh api repos/:owner/:repo/releases/latest` で確認)。GitHub の prerelease 印と semver の prerelease は別物で、配信経路はどれも GitHub Releases を参照しないため、印を外しても利用者の解決結果は変わらない。実装は `.github/workflows/release.yml:822-824` の 1 箇所。

### 3. Release ノートの分類が効いていない

`.github/release.yml` が参照する 6 ラベル (`breaking` / `feature` / `fix` / `docs` / `kasane` / `ci`) がリポジトリに 1 件も存在しない (知らせ #2)。ただしラベルを作るだけでは目的を達しないことが相方レビューで判明した (下記)。

## 検討した選択肢 (却下案と理由を含む)

論点 1 について検討し却下した案は [cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md) の Alternatives Considered に記録した (workflow が `develop` へ書き戻す / `main` へ直接 commit する / 第三者バッジで最新を示す / 手作業のまま `--check` を強化する / 案内先を Releases の一覧にする / プレースホルダを `X.Y.Z` にする)。

## 決定事項

- インストール例は具体 version を持たず、プレースホルダ `{version}` を置く。最新版は GitHub Releases が示す ([cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md))。
- 0.x の beta を配信している間、GitHub Release に prerelease の印を付けない。既発行の 2 件の印も解除する (同 ADR)。
- Release ノートは、GitHub のラベル推測に委ねず、`main` 宛て pull request の本文に書いた固定セクションから組み立てる。範囲の起点は直前の公開済み Release (draft でなく、その tag が `main` の first-parent 上で対象 commit の祖先であるもののうち最も近いもの) とし、そこから今回の commit までにマージされた PR 分を連結する (A-1、起点の精緻化は相方レビュー spec-002・003 による)。
- 固定セクションの形式は、見出し `## Changes`、各行 `- <種別>: <説明>`、種別は `breaking` / `feature` / `fix` / `docs` の 4 つ。開発ハーネスの作業と CI の整備は行を書かないことでノートから外れる (A-2)。
- 固定セクションは必須とし、利用者向けの変更が無い PR は `- none` と明示する。セクションが無い PR があれば release を止める。空を許すと書き忘れと「本当に無い」が区別できないため (A-5)。
- `.github/release.yml` (自動生成ノートの設定) は廃止し、ラベル 6 件も作らない。ノート末尾には比較リンク (`Full Changelog`) を付ける。これにより KsDialogs の知らせ #2 は目的を失う (A-4)。
- ノートの組み立ては release workflow が行う。複数 PR から集める以上、人が手で組み立てる形は現実的でないため (A-1 から従属的に決まる)。
- リリース手順の正は `kasane/handbook/cross/release-procedure.md` に置いたままとし、リリース用スキルはそれを読んで順に実行する薄い層とする (オーナー判断 2026-09-11)。手順の記述を二重に持たない。
- SwiftPM の `from:` は下限が prerelease なら prerelease も解決する。`exact:` が要るのは特定の版に固定するため。[cross/ADR-0019](../../decisions/cross/0019-lockstep-single-version.md) の帰結行はこの内容に直接修正済み (決定そのものの変更ではないため注記なし、オーナー判断 2026-09-11)。
- 新しいインストール例の契約 (プレースホルダ・`exact:`・Releases 案内・en/ja の同一構成) は軽量な lint で継続検査する。撤去する置換機構と違い、リリースのたびに走らせる必要がない書式の不変条件であるため (D、オーナー判断 2026-09-11)。
- リリース用スキルの射程は、事前確認・`## Changes` の下書き生成・リリース PR の作成・release の起動・実行の見守りと失敗時の案内・公開後の確認まで。手順の記述と判断 (version 番号・下書きの最終文面・再実行の可否) は担わない。置き場所は `.agents/skills/release/` (`.claude/skills/release` は symlink、docs-refresh の前例に合わせる) (オーナー判断 2026-09-11)。
- Skill に Releases の URL を置くことを許す。[cross/ADR-0022](../../decisions/cross/0022-user-docs-as-agent-skills.md) の「SKILL.md と references は Skill 外のファイル・URL への参照を持たない」という条項は、リポジトリ内部への参照が切れることの防止が主眼であり、プロジェクトの正規 URL 1 本はその趣旨を破らない (オーナー判断 2026-09-11)。ADR-0029 が ADR-0022 を一部改訂する形で記録する。

## ADR 候補

- 作成済み: [cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md) (status: proposed、amends: 0022)

## 未決の論点

### A. Release ノートの組み立て (決定済み)

GitHub の自動生成ノートはマージされた pull request をラベルで分類する。しかし [cross/ADR-0028](../../decisions/cross/0028-ci-triggers-by-branch-role.md) の運用では個別の変更は `develop` へ直接 push され、`main` に入るのは集約された `develop` → `main` の pull request だけである。実物の `0.1.0-beta.2` のノートに並んでいるのも集約 PR 2 件 (#10 docs 変更 / #11 `release: 0.1.0-beta.2`) のみで、両方とも「その他」に落ちている。ラベルを 6 件作っても分類できるのは集約 PR の粒度でしかなく、集約 PR に `kasane` を付ければその中の利用者向け変更ごとノートから消える。

**方針 (オーナー判断 2026-09-11)**: 分類を GitHub のラベル推測に委ねるのをやめ、**`main` 宛て pull request の本文に固定フォーマットで変更内容を書き、Release ノートはそれを参照する**方式にする。あわせて**リリース用のスキルを用意し、リリース手順を定型的に実行できるようにする**。

この方針により、知らせ #2 (6 ラベルの作成) は目的を失う。ラベルによる分類機構は持たない。

詳細も A-1 から A-5 まで決定済み (内容は上の「決定事項」に記載)。

### B. リリース用スキルの位置づけ (決定済み)

手順の正は `kasane/handbook/cross/release-procedure.md` に置いたままとし、スキルはそれを読んで順に実行する薄い層にする。射程は上の「決定事項」に記載。

この設計は、本変更が新しい手作業を 1 つ増やしている (PR 本文への `## Changes` の記入) ことへの対処でもある。前回 tag 以降の commit から下書きを生成できれば、増えた手作業は実質相殺される。

### C. SwiftPM の `from:` は prerelease を解決するか (解決済み・ADR の訂正が要る)

調査の結論: **README の記述が正しく、[cross/ADR-0019](../../decisions/cross/0019-lockstep-single-version.md) の帰結行が誤っている。**

SwiftPM の版解決は `Range<Version>.contains(version:)` の特別規則で決まる (swift-tools-support-core `Sources/TSCUtility/Version.swift`)。規則は 2 つ。

1. 候補が prerelease で、範囲の下限・上限のどちらも prerelease 識別子を持たないなら除外する
2. どちらかが prerelease を持つ場合でも、上限が正式版で候補のコア (major.minor.patch) が上限と一致するなら除外する

`from: "X"` は常に `X ..< (major+1).0.0` に展開される (0.x でも上限は 1.0.0)。したがって:

| 宣言 | 解決対象 |
|---|---|
| `from: "0.1.0-beta.1"` | 下限が prerelease なので prerelease を含む。`0.1.0-beta.2` も `0.2.0-beta.1` も `0.1.0` 正式版も解決される (`1.0.0-beta.1` は規則 2 で除外) |
| `from: "0.1.0"` | 両端が正式版なので prerelease は一切解決されない |
| `exact: "0.1.0-beta.2"` | 単純な等価比較。tag があれば常に解決される |

npm / Cargo との違い: あちらは prerelease を含む境界とコアが完全一致する版にしか prerelease を許さないが、SwiftPM は下限とコアが一致する必要がない (`from: "0.1.0-beta.1"` が `0.2.0-beta.1` を拾う)。また Cargo の `0.1.0` は `<0.2.0` だが SwiftPM の `from: "0.1.0"` は `<1.0.0`。

**`exact:` を使うという結論自体は維持される。理由が違う。** `from: "0.1.0-beta.1"` は上限が `1.0.0` なので、後から出た `0.1.0` 正式版や `0.9.0` へ自動的に上がり、特定の beta に固定されない。固定したいなら `exact:` が要る。

したがって訂正が必要なのは次の 2 箇所 (どちらも「`from:` は prerelease を解決しないから」という誤った理由を述べている)。

- [cross/ADR-0019](../../decisions/cross/0019-lockstep-single-version.md) の Consequences の帰結行 (accepted な ADR のため改訂の手続きが要る)
- `AGENTS.md` / `CLAUDE.md` の 17 行目 (インストール例の例外規定。本変更で撤去する対象そのものなので、撤去時に消える)

調査の自信度は高い (上流実装のソースを直接確認) が、`swift package resolve` による実測はしていない。特に `from: "0.1.0-beta.1"` が `0.2.0-beta.1` を拾う点は、README の文面を確定する前に小さなリポジトリで 1 回実測しておく。

### D. 新しい契約を継続的に検査するか (決定済み)

置く。検査内容は、14 行のインストール例が `{version}` を持つこと・SwiftPM の宣言が `exact:` であること・README 2 枚と Skill 8 枚に Releases への案内があること・en と ja が同一構成であること。置き場所は `scripts/` の lint 群と `ci.yml` の lint job。docs-refresh の manifest には載せない (concepts 由来の知識ではなく機械的な書式規則のため)。

却下: 検査を置かない (誰かが具体 version を書き戻しても次のリリースまで気づけない) / 検査項目を `{version}` だけに絞る (`exact:` とロックステップの逆戻りを拾えない)。

## UI 素材 (ui/references/ の一覧と注釈)

なし (UI 変更を伴わない)

## 変更級の推奨: L

判定材料:

- 触る能力が複数にまたがる: リリース機構 (Release ノートの組み立て・prerelease 印・置換機構の撤去) と、利用者向けのインストール例の契約
- 外部に見える面が複数: GitHub Release の見え方、README 2 枚と利用者向け Skill 8 枚、配布物に同梱される README
- 新しい道具を 1 つ増やす: リリース用スキル (`.agents/skills/release/`)
- 覆すコストが高い判断を含む: インストール例が version を持たないこと、ノートの出所を PR 本文にすること、手順の正の所在
- ADR: [cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md) (proposed) に加えて、ノートの出所と手順の正について 1 本を起票する

design.md を作る。

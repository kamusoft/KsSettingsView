# セカンドオピニオン: backport-release-workflow-hardening (spec-002)
**相方**: codex / **label**: so-spec-backport-release-workflow-hardening-2 / **日付**: 2026-09-11 / **対象**: kasane/changes/backport-release-workflow-hardening/ の proposal.md・specs/release-workflow/spec.md・tasks.md・exploration.md、kasane/decisions/cross/0029-install-examples-without-pinned-version.md
---
# レビュー結果: backport-release-workflow-hardening

**判定**: `NEEDS_DISCUSSION`  
**指摘**: Critical 0 / Major 5 / Minor 5 / Suggestion 0

仕様・ADR側の設計判断が必要な Major があるため、現状のまま実装へ進めるべきではありません。ビルド・テスト・書き込みは行っていません。

## 指摘事項

### [🟠 Major] Release ノート分類が現在のブランチ運用では成立しない

**該当箇所**: [specs/release-workflow/spec.md:97](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:97)、[tasks.md:5](kasane/changes/backport-release-workflow-hardening/tasks.md:5)、[kasane/decisions/cross/0028-ci-triggers-by-branch-role.md:14](kasane/decisions/cross/0028-ci-triggers-by-branch-role.md:14)

**問題点**: GitHub の自動生成ノートは「マージされた Pull Request」をラベルで分類します。[GitHub 公式仕様](https://docs.github.com/en/repositories/releasing-projects-on-github/automatically-generated-release-notes) 一方、ADR-0028 は個別変更を `develop` へ直接 push し、`main` には集約された `develop → main` PR だけを入れる運用です。6ラベルを作成しても、個別変更を分類・除外するPR自体が存在しません。集約PRに `ci` や `kasane` を付ければ、利用者向け変更を含むPR全体が除外される可能性があります。

**推奨修正**: 次のいずれかを明示的に選んでください。

- 個別変更をPR化し、ADR-0028を改訂する
- commit/change metadata から独自にノートを生成する
- 「集約リリースPRを1分類するだけ」と要件を縮小する

あわせて、実際のタグ範囲から生成ノートをプレビューし、分類・除外を検証するタスクが必要です。

### [🟠 Major] Releases URL の追加が accepted ADR-0022 の閉世界性に反する

**該当箇所**: [tasks.md:11](kasane/changes/backport-release-workflow-hardening/tasks.md:11)、[proposal.md:38](kasane/changes/backport-release-workflow-hardening/proposal.md:38)、[kasane/decisions/cross/0022-user-docs-as-agent-skills.md:23](kasane/decisions/cross/0022-user-docs-as-agent-skills.md:23)、[kasane/decisions/cross/0029-install-examples-without-pinned-version.md:50](kasane/decisions/cross/0029-install-examples-without-pinned-version.md:50)

**問題点**: ADR-0022 は、コピーされた Skill が外部ファイル・URLに依存しない閉世界性を要求しています。本提案は8枚すべての Skill に Releases URL を追加し、ADR-0029自身も「ネットワークのないエージェントはversionを得られない」と認めています。それにもかかわらず proposal は「accepted ADR を覆さない」としています。

**推奨修正**: ADR-0029をADR-0022の明示的な一部改訂として扱うか、ReleasesリンクはルートREADMEだけに置き、Skillは利用者からversionを受け取る自己完結した手順にしてください。

### [🟠 Major] SwiftPM prerelease の現行誤記を「残っていることの確認」では直せない

**該当箇所**: [tasks.md:12](kasane/changes/backport-release-workflow-hardening/tasks.md:12)、[README.md:48](README.md:48)、[README_ja.md:48](README_ja.md:48)、[kasane/decisions/cross/0019-lockstep-single-version.md:36](kasane/decisions/cross/0019-lockstep-single-version.md:36)

**問題点**: ADR-0019 は prerelease 期間中に `exact:` が必要と定めていますが、README 2枚は `from:` でも prerelease を選べると案内しています。iOS Skill 2枚はコードで `exact:` を使うだけで、必要性の散文説明がありません。したがって task 2.3 の「説明が各所に残っていることを確認」は現状では成功できません。proposal の「本文見直しはNon-Goal」とも衝突します。

**推奨修正**: task 2.3を「誤記を修正し、README 2枚とiOS Skill 2枚へ `exact:` の理由を追加する」に変更し、デルタスペックにも明示的な Requirement/Scenario を追加してください。

### [🟠 Major] 新しいインストール例契約に継続的な検査と正本がない

**該当箇所**: [specs/release-workflow/spec.md:7](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:7)、[tasks.md:13](kasane/changes/backport-release-workflow-hardening/tasks.md:13)、[.github/workflows/ci.yml:171](.github/workflows/ci.yml:171)、[kasane/decisions/cross/0022-user-docs-as-agent-skills.md:18](kasane/decisions/cross/0022-user-docs-as-agent-skills.md:18)

**問題点**: `{version}`、Releases案内、SwiftPMの`exact:`という新契約を導入しますが、既存検査をすべて削除し、14箇所を継続検査する代替がありません。`readme-example-lint.py` は最小コード例だけが対象です。またSkillの正本は concepts とされていますが、この利用者向けversion方針をどのconcept/manifest targetから再生成するかも決まっていません。

**推奨修正**: release入力との一致検査とは別に、CIで次を検査する軽量lintを残してください。

- 対象14行が具体versionを持たず `{version}` である
- SwiftPM例が `exact:` である
- 許可された場所に最新版案内がある
- en/jaがロックステップである

さらに、この方針を利用者向けconceptとしてmanifestへ対応付けるか、docs-refreshの機械チェックとして明文化してください。

### [🟠 Major] M級・単一能力という分類が実際の変更範囲と一致しない

**該当箇所**: [proposal.md:16](kasane/changes/backport-release-workflow-hardening/proposal.md:16)、[proposal.md:40](kasane/changes/backport-release-workflow-hardening/proposal.md:40)、[proposal.md:42](kasane/changes/backport-release-workflow-hardening/proposal.md:42)

**問題点**: proposal は「release-workflow単一能力」としつつ、同時に「リリース経路と利用者向けドキュメント契約の両方」を変更すると認めています。さらにGitHub Release/labels、Maven Central、NuGet、Agent Skillsという複数の外部面と長命契約に触れます。KasaneのL級基準である複数能力・外部連携・覆すコストの高い判断に該当し、現在はdesign.mdもありません。

**推奨修正**: 「リリース経路の堅牢化」と「インストール例・GitHub Release意味論」を別changeへ分割するか、L級へ上げて外部状態、再実行、ロールバック、検証境界をdesign.mdにまとめてください。

### [🟡 Minor] publish時間予算のScenarioがRequirementより弱い

**該当箇所**: [specs/release-workflow/spec.md:47](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:47)、[specs/release-workflow/spec.md:59](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:59)、[scripts/release/central-portal.sh:129](scripts/release/central-portal.sh:129)

**問題点**: Requirementは最終照会の最大応答時間まで収容するとしていますが、Scenarioは待機上限の単純合計しか検査しません。各HTTP要求は最大300秒で、180分ちょうどでは本体処理とrunner変動の余裕が小さくなります。

**推奨修正**: `2×検証待ち + 公開待ち + 最大HTTP超過 + 本体処理予算 + 余裕` の式を受け入れ条件にし、具体的なjob timeoutを決めてください。

### [🟡 Minor] 新設する待機スクリプト自己テストがCIに接続されない

**該当箇所**: [tasks.md:36](kasane/changes/backport-release-workflow-hardening/tasks.md:36)、[tasks.md:49](kasane/changes/backport-release-workflow-hardening/tasks.md:49)、[.github/workflows/ci.yml:181](.github/workflows/ci.yml:181)

**問題点**: `wait-for-registries.sh` の自己テストは実装時に一度実行するだけで、将来のCIでは走りません。実運用では不可逆なpublish後に初めてこのスクリプトへ到達します。

**推奨修正**: `central-portal.sh --selftest` と同様、`wait-for-registries.sh --selftest` をCI lint jobへ追加してください。

### [🟡 Minor] タイムアウト時の状態粒度が曖昧

**該当箇所**: [specs/release-workflow/spec.md:69](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:69)、[tasks.md:35](kasane/changes/backport-release-workflow-hardening/tasks.md:35)

**問題点**: 「レジストリごとの最後の状態」では、NuGet 3 Package IDの一部だけ未反映・一部だけ判定不能という状態を一つへ潰せます。また「判定不能」だけでは通信失敗、HTTP 5xx、不正JSONを区別できず、動機にある診断性を十分満たしません。

**推奨修正**: Maven座標1件とNuGet Package ID 3件の計4対象ごとに、分類・HTTP statusまたは失敗種別を保持して出力するScenarioにしてください。

### [🟡 Minor] deployment引き継ぎScenarioが変更前の実装でも成立する

**該当箇所**: [specs/release-workflow/spec.md:90](kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md:90)、[.github/workflows/release.yml:482](.github/workflows/release.yml:482)

**問題点**: 現行でもAndroid/MAUI artifact取得に失敗した場合、前attemptのdeployment ID artifact自体は削除・上書きされません。「情報は失われない」というTHENでは、step移動前後を識別できません。

**推奨修正**: 「deployment ID取得stepが他artifact取得stepより先に実行される」「取得済みであることがログまたはoutputに残る」など、旧構造では失敗する観測可能な条件へ変更してください。

### [🟡 Minor] 既知のスクリーンショット修正がproposal/specに現れない

**該当箇所**: [tasks.md:43](kasane/changes/backport-release-workflow-hardening/tasks.md:43)、[proposal.md:14](kasane/changes/backport-release-workflow-hardening/proposal.md:14)

**問題点**: `develop` から `main` への画像URL変更は既知の利用者可視変更ですが、What ChangesにもRequirementにも対応せず、tasksだけにあります。

**推奨修正**: proposal/specへ明示して検査対象にするか、このchangeから外してください。

## 確認済み点

GitHubの `/releases/latest` がprereleaseを除外するという前提自体は正しく、公式APIでは「最新のnon-prerelease・non-draft Release」と定義されています。[GitHub Releases API](https://docs.github.com/en/rest/releases/releases#get-the-latest-release)

また、Central Portalの30分上限を検証待ちと公開待ちで分離し、公開待ちを90分へ伸ばす方向、およびMaven/NuGet照会を未反映と判定不能へ分ける方向は、現行実装で確認できる問題に対応しています。上記Majorを解消した後も維持してよい設計です。

## 突き合わせ結果

spec-001 の後に設計を全面的に作り直したため、本レビューは新しい設計 (プレースホルダ方式・prerelease 印の廃止・同期機構の撤去) に対する初回の外部レビューにあたる。ホスト側の自己レビューでは 1 件のみ検出 (prerelease 印の契約射程が ADR と spec で非対称 — 修正済み)。相方の指摘 10 件は**全件を採用**した (降格・未解決なし)。

| # | 指摘 | 採否 | 検証 |
|---|---|---|---|
| Major 1 | Release ノート分類が現在のブランチ運用では成立しない | 採用 | `gh release view 0.1.0-beta.2` で実物を確認。並ぶのは `develop` → `main` の集約 PR 2 件のみで、個別変更は PR として現れない。ラベルは集約 PR の粒度でしか効かず、`kasane` / `ci` の除外は集約 PR ごとノートから落とす |
| Major 2 | Releases URL の追加が accepted ADR-0022 の閉世界性に反する | 採用 | ADR-0022 の Decision に「SKILL.md と references は Skill 外のファイル・URL への参照を持たない」と明文あり。proposal の「accepted ADR の決定を覆すものはない」は誤り |
| Major 3 | SwiftPM prerelease の現行誤記を「残っていることの確認」では直せない | 採用 | README.md:48 / README_ja.md:48 は `from:` で prerelease を選べると案内し、cross/ADR-0019:36 は `from:` が prerelease を解決しないとする。両者は不整合。どちらが SwiftPM の実挙動かの確認が別途要る (semver の範囲に prerelease を含める条件は下限が同一 version の prerelease であるかに依存するため、両者が部分的に正しい可能性がある) |
| Major 4 | 新しいインストール例契約に継続的な検査と正本がない | 採用 | 既存検査を全廃したうえで代替を置いていないことを確認 |
| Major 5 | M 級・単一能力という分類が実際の変更範囲と一致しない | 採用 | リリース経路・GitHub Release の意味論・ラベル・レジストリ待機・利用者向け Skill 8 枚と README 2 枚に跨がることを確認 |
| Minor 1 | publish 時間予算の Scenario が Requirement より弱い | 採用 | Scenario が単純合計しか検査しないことを確認 |
| Minor 2 | 新設する待機スクリプト自己テストが CI に接続されない | 採用 | tasks が実装時の一度きりの実行しか求めていないことを確認 |
| Minor 3 | タイムアウト時の状態粒度が曖昧 | 採用 | NuGet は Package ID 3 件を個別に待つため、「レジストリごと」では潰れることを確認 |
| Minor 4 | deployment 引き継ぎ Scenario が変更前の実装でも成立する | 採用 | THEN が旧構造でも真になり、step 移動の有無を識別できないことを確認 |
| Minor 5 | 既知のスクリーンショット修正が proposal/spec に現れない | 採用 | tasks 7.1 のみに存在することを確認 |

設計判断を要するもの (オーナーへ提示):

- Major 5 の分割 (リリース経路の堅牢化 / 利用者向け契約)
- Major 1 の Release ノート分類の方針 (個別 PR 化・独自生成・要件の縮小)
- Major 2 の解き方 (ADR-0022 の一部改訂 / Releases リンクを README に限り Skill は利用者から version を受け取る形にする)
- Major 3 の事実確認 (SwiftPM が `from:` で prerelease を解決する条件)

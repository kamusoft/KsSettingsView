# レビュー結果: add-release-workflow (001 回目)

**日付**: 2026-09-03
**判定**: CHANGES_REQUESTED

## サマリー

release workflow の段構成・secrets の閉じ込め・冪等な再実行の骨格はデルタスペックの Requirement をよく満たしており、scripts の自己テストとコメントの質も高い。ただし Android 発行物の同一性比較が `publishToMavenLocal` の副産物 `maven-metadata-local.xml` (`<lastUpdated>` タイムスタンプ) を byte 比較の対象に含めており、**どのリリースでも publish が Maven upload の直前で必ず失敗する** (手元で再現済み)。加えて失敗経路で deployment を drop した後に `central-deployment-id` artifact が更新されないため、spec が要求する「同じ version での再実行」が最も起こりやすい失敗経路で成立しない。この 2 件は初回リリースを実施する前に塞ぐ必要がある。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規の shell / MSBuild / workflow コメントに作業文書パス・Decision 番号・change 名の参照なし。`scripts/comment-policy-lint.py` も 0 件 (検査 728 ファイル)
- `kasane/handbook/cross/test-execution.md` (テストを実行・報告するとき)
- `kasane/handbook/cross/public-identifiers.md` (配布座標を扱うため) — `jp.kamusoft:kssettingsview` / NuGet 3 ID / 配信リポジトリ名が workflow の `env`・scripts の定数・手順書で一致
- `kasane/handbook/cross/local-development-setup.md` / 新規 `release-procedure.md` (index への追加ときっかけの記述を確認)
- `kasane/handbook/maui/index.md` の 2 文書は今回の diff (pack 構成・消費者検証) に当たらないため参照のみ
- lessons: `code-review.md` (L-001)、`process.md` (L-002 / L-003 / L-006 / L-007)、`impl.md` / `test.md`

## 実行した検証

- `dotnet test maui/KsSettingsView.Maui.Tests` (Release): 516 成功 / 0 失敗
- `scripts/release/central-portal.sh --selftest` (33 OK) / `set-readme-version.py --selftest` (17 OK) を再実行し成功を確認
- `python3 scripts/release/set-readme-version.py --check 9.9.9` を実物の README に対して実行し、6 行が確定でき不一致を行番号つきで報告することを確認 (現状は `0.1.0` かつ SwiftPM が `from:` — 置換モードで `exact:` に揃う)
- `scripts/comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py`: 新規・変更ファイルに指摘なし
- `dotnet nuget list source` をリポジトリルートと `maui/` で実行し、`maui/nuget.config` の効果 (nuget.org のみ) を確認
- `compare-maven-artifacts.sh` を実物の mavenLocal 出力の複製 2 本 (片方の `maven-metadata-local.xml` の `<lastUpdated>` だけを変えたもの) に対して実行し、Critical の再現を確認
- iOS / Android の本体テストは製品コードに触れていないため未実行 (コンテキストパッケージの実測結果を採用)

## 指摘事項

### [🔴 Critical] 発行物の同一性比較が `maven-metadata-local.xml` を含むため、publish が毎回失敗する

**該当箇所**: `scripts/release/compare-maven-artifacts.sh:59-65` / `.github/workflows/release.yml:610-611`

**問題点**:
`publishToMavenLocal` は成果物のほかに `~/.m2/repository/jp/kamusoft/kssettingsview/maven-metadata-local.xml` を書き出す。このファイルは `<lastUpdated>` に発行時刻 (秒単位) を持ち、`<versions>` にそのマシンで発行済みの version を並べる。

- `package-android` は `cp -R "${HOME}/.m2/repository/jp"` でこのファイルごと artifact 化する (`release.yml:268`)
- `publish` は再ビルド後の `${HOME}/.m2/repository/jp` と比較する (`release.yml:610-611`)
- `compare-maven-artifacts.sh` の `list_files` は `.asc` と checksum しか除外しないため、`maven-metadata-local.xml` は「アーカイブ以外」として byte 比較され、2 つの job の発行時刻が違う以上**必ず差異になる**

手元で実物の mavenLocal 出力を複製し `<lastUpdated>` だけを変えて実行した結果:

```
::error::内容が異なります: kamusoft/kssettingsview/maven-metadata-local.xml
::error::発行物に 1 件の差異があります (署名を除く比較)
```

この比較は `check-signatures.sh` の直後・`publishToMavenCentral` の直前にあるため、**どの version でも publish は Maven upload に到達できない**。tasks 1.3 (ubuntu ランナーでの再現性実測) が未実施のため露見していないだけで、CI 実行を待たずに確定できる欠陥。デルタスペック「Android 成果物の同一性」の比較対象は pom・module・aar・sources jar・javadoc jar と明記されており、`maven-metadata-local.xml` は対象外。

**推奨修正**:
`compare-maven-artifacts.sh` の `list_files` の除外に `maven-metadata*.xml` を加える (spec の比較対象外であることをコメントで示す)。あわせて `check-signatures.sh` は既に `.aar/.pom/.jar/.module` だけを拾うので変更不要。`package-android` の staging を version ディレクトリ単位に絞る案もあるが、消費者検証がローカル Maven リポジトリとして参照する形を崩さないよう、比較側の除外で閉じるほうが影響が小さい。

### [🟠 Major] 失敗経路で drop した deployment ID が artifact に残り、再実行を塞ぐ

**該当箇所**: `.github/workflows/release.yml:773-782` (`Drop pending deployment`) / `release.yml:558-575` (再実行時の状態照会) / `scripts/release/central-portal.sh:174-186` (`deployment_state`)

**問題点**:
`Drop pending deployment` は失敗時に deployment を `DELETE` するが、`central-deployment-id` artifact は更新も削除もされない。次に「失敗した job から再実行」すると:

1. `Download previous deployment id` が削除済みの ID を復元する
2. `central-portal.sh status <削除済み ID>` を呼ぶ
3. Portal が 200 以外を返せば `deployment_state` が `fail "deployment の状態を照会できない (HTTP ...)"` で即座に失敗する (`central-portal.sh:179-181`)

この経路は例外的ではなく、spec が Scenario として明示している「Maven upload の後、NuGet.org への push で失敗した実行」→「失敗時に保留 deployment が残らない」→「部分 publish を同じ version で埋める」の連鎖そのもの。同じ run では artifact が残り続けるため、手順書が禁じている `Re-run all jobs` でも復旧できず、その run を捨てて再 dispatch するしかなくなる。Requirement「同じ version での再実行」の「失敗した実行を同じ version で『失敗した job から再実行』したとき ... 完了できる SHALL」を満たさない。

**推奨修正**: 次のどちらか (両方でもよい)。

- `Drop pending deployment` の成功後に、空文字を書いた `deployment-id.txt` を `overwrite: true` で upload し直し、引き継ぎを断つ (`Publish Android to Central Portal` は既に空文字を「ID なし」として扱う)
- `deployment_state` (または呼び出し側) が HTTP 404 を「その deployment はもう存在しない」として扱えるようにし、`status` が「無い」を返せる形にしたうえで、`release.yml:558` の分岐で ID を捨てて再 upload へ落とす

### [🟡 Minor] `deviation.md` の「作業機は nuget.org 1 ソースのみ」が事実と異なる

**該当箇所**: `deviation.md:6`

**問題点**:
この作業機の NuGet ソースは 2 件ある。

```
$ dotnet nuget list source          # リポジトリルート
  1. nuget.org   https://api.nuget.org/v3/index.json
  2. kamusoft    https://pkgs.dev.azure.com/kamusoft/_packaging/kamusoft/nuget/v3/index.json
$ (cd maui && dotnet nuget list source)
  1. nuget.org   https://api.nuget.org/v3/index.json
```

`evidence/premise-spike-pack.txt` の 7 節・8 節も「2 個のパッケージ ソース: nuget.org と社内フィード」で NU1507 が出ていたことを記録している。つまり maui-nuget-distribution の Scenario「複数ソース環境でも nuget.org だけから取得する」は**この環境で実際に再現・充足されている**のに、deviation は「再現できなかった」という残存リスクとして記録している。deviation は蒸留でアーカイブされ、後続の判断材料になるため、誤った残存リスクを残さない。

**推奨修正**: deviation の当該行を実態に合わせて書き直す (環境は複数ソースであり、`maui/` 配下の解決先が nuget.org のみになること・NU1507 が消えることを確認した、という記述にする)。あわせて tasks 3.1 のチェックの根拠を evidence に 1 行残すと、tasks の `[x]` と証跡が対応する。

### [🟡 Minor] 手順書の「`Re-run all jobs` を使わない」理由が不正確

**該当箇所**: `kasane/handbook/cross/release-procedure.md:146`

**問題点**:
理由を「前の attempt が保存した deployment ID を引き継げなくなる」としているが、artifact は run 単位で保持され attempt をまたいで download できる (この前提は `spike-release-premise.yml` の `reuse` job が確かめようとしているものでもある)。実際に `Re-run all jobs` を避けるべき理由は別にあり、`package-ios` / `package-android` / `package-maui` の `upload-artifact` が `overwrite` を持たない (`release.yml:209-214` / `272-277` / `365-370`) ため、同名 artifact の再 upload が衝突して失敗すること、および本体検証と配布物生成を無駄にやり直すこと。

**推奨修正**: 手順書の括弧内を実際の理由に差し替える。`Re-run all jobs` も成立させたいなら package 段の 3 つの upload に `overwrite: true` を足す (deployment ID の artifact は既に持っている)。

### [🟡 Minor] snupkg の生成が本変更のどこでも実証されていない

**該当箇所**: `.github/workflows/release.yml:351-361`

**問題点**:
`package-maui` は 3 パッケージ × (nupkg, snupkg) が揃っていることを pack 直後に検査するが、snupkg が実際に生成されることを裏づける証跡がない。`evidence/premise-spike-pack.txt` の pack 結果は nupkg のみを列挙し、`verification/maui/prepare-feed.sh:60-61` も nupkg しか検査しない。binding (とくに `KsSettingsView.Binding.iOS`) で snupkg が出なければ、初回リリース当日に `package-maui` で初めて落ちる。失敗の向き自体は安全側だが、手元で確認できることを本番まで持ち越している。

**推奨修正**: 手元の `dotnet pack` (3 プロジェクト) で `.snupkg` が 3 件出ることを確認し、evidence に 1 行残す。出ないパッケージがあれば `IncludeSymbols` の効き方を確認する。

### [🟡 Minor] `check-signatures.sh` の入力前提 (`publishToMavenLocal` が `.asc` を出す) が未実測

**該当箇所**: `.github/workflows/release.yml:606-609`

**問題点**:
署名確認は `publishToMavenLocal` の出力 (`~/.m2/repository/jp`) に対して行う。`evidence/scripts-unit.txt` の確認はいずれも署名鍵なしの発行物と、`.asc` を手で置いた発行物に対するもので、「鍵を渡した `publishToMavenLocal` が mavenLocal にも `.asc` を書く」ことは確かめられていない。書かれなければ、鍵が正しく渡っていても Requirement「署名の生成確認」で必ず失敗する (upload 前で安全側ではあるが、初回リリースが 1 往復増える)。

**推奨修正**: tasks 1.1 の spike で `publishToMavenCentral` を回すときに、同じ鍵で `publishToMavenLocal` した `~/.m2` に `.asc` が並ぶことも併せて確認し、evidence に残す。出ない場合は署名確認の対象を Central へ送る bundle 側に切り替える必要がある。

### [🔵 Suggestion] pack の順序に関するコメントが実装と合っていない

**該当箇所**: `.github/workflows/release.yml:329-330`

**問題点**:
「facade は binding の nupkg を依存として解決するため、先に出来ていないと解決できない」とあるが、facade は binding を `ProjectReference` で参照している (`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:69,74`)。pack の順序は依存解決の要件ではなく、nupkg の順序依存があるのは NuGet.org への push (`release.yml:662-663` の説明が正しい)。コメントだけで意味が閉じることは満たしているが、内容が事実と違うため、後から順序を触る人を誤らせる。

**推奨修正**: 「binding 2 件 → facade の順は push の順序と揃えるため」など、実際の理由に書き直す (順序自体は変えなくてよい)。

### [🔵 Suggestion] `spike-release-premise.yml` の削除を完了条件に紐づける

**該当箇所**: `.github/workflows/spike-release-premise.yml:1-3`

**問題点**:
一時 workflow である旨は冒頭に書かれているが、tasks には削除のタスクが無い。`workflow_dispatch` で誰でも起動できる状態で残ると、リリースと無関係な workflow が一覧に居座る。

**推奨修正**: tasks 1.3 / 1.4 に「実測後に `spike-release-premise.yml` を削除する」を明記する (tasks は足場なのでオーケストレーター側で追記の要否を判断)。

## 確認して問題がなかった観点

- `secrets` の参照は publish job の行範囲のみ、`secrets: inherit` は不在、reusable workflow 側も `permissions: contents: read` (静的証跡と突き合わせて再確認)
- `consumer-*` / `smoke-*` への `with:` が `env` を使わず `needs.validate.outputs.*` を経由している (reusable workflow の `with` で `env` コンテキストは使えないため正しい)。`inputs` / `needs` の参照位置はいずれも許可された文脈
- semver 検査の正規表現が spec の負ケース 7 種 (形式 4 + 先頭ゼロ 3) をすべて弾く
- `sync-snapshot.sh` の origin 検証が https (validate / package-ios) と ssh (publish) の両形式を受理する
- artifact の展開形 (`jp/` を根とするローカル Maven リポジトリ、nupkg 直置き、スナップショット直置き) が `verify-consumer-*.yml` の `--reference` の期待と一致する
- `Directory.Build.targets` の追記が `IsPackable=false` のプロジェクトで発火せず、既存の buildTransitive import と icon の同梱を壊していない (evidence 7 節 + 本レビューの 516 テスト成功)
- `build-consumer.sh` は `set -euo pipefail` 下で `dotnet build | tee` しているため、XA4301 検出の追加でビルド失敗が握り潰されない
- `ci.yml` の `on.pull_request.branches` に `main` が既に含まれており、head 制限の検査が発火する
- `central-portal.sh` の `drop` が PENDING / VALIDATING でも DELETE を送らない (spec の 4 状態に対する穴埋めとして deviation 記録済み)

## アクションプラン

1. Critical: `compare-maven-artifacts.sh` の比較対象から `maven-metadata*.xml` を外す (これを直さない限り publish は 1 度も成功しない)
2. Major: 失敗経路の drop 後に `central-deployment-id` を無効化する (または 404 を「不在」として扱う)
3. Minor: `deviation.md:6` の環境記述を実態に合わせる
4. Minor: 手順書 `release-procedure.md:146` の理由を修正し、必要なら package 段の upload に `overwrite: true` を足す
5. Minor: snupkg の生成と mavenLocal への `.asc` 出力を手元 / spike で確認し evidence に残す
6. Suggestion: `release.yml:329-330` のコメント修正、spike workflow の削除タスク化

# Verify 001: add-release-workflow

判定日: 2026-09-03 / 対象: 作業ツリーの未コミット変更 (HEAD = `4c04878` からの差分)

## 判定: **VALID**

デルタスペック 4 capability の全 Requirement (15 件) / 全 Scenario (43 件) について、実装箇所を特定できた。❌ (未記録の欠落・乖離) は 0 件。deviation.md に記録済みの乖離 3 件 + 付随修正 1 件はいずれも合意済み差分として扱った。tasks.md のチェック済み項目に虚偽なし、足場アーティファクト (proposal / design / specs) への逆流なし、手元で実行できるテスト (script の `--selftest` 2 本) は全件成功。

CI 実行 (GitHub 上でしか踏めない経路) を待つ Scenario が 14 件あるが、いずれも**実装は存在し**、静的確認またはモック/selftest による机上確認まで到達している。コンテキストパッケージの「既知の先送り」に含まれる範囲であり、実装の不在とは区別して記録する。

### 検証の性質について

この change は対象の大半が GitHub Actions workflow と shell / python script であり、Scenario の「テスト」は次の 3 種で対応している。対応表の「テスト / 証跡」列はこの区分で読む。

| 区分 | 意味 |
|---|---|
| selftest | `--selftest` (`central-portal.sh` / `set-readme-version.py`) またはモック応答による単体確認。evidence に採取ログあり |
| 実測 | 手元で実物を動かした結果 (pack / 消費者ビルド / 合成リポジトリ)。evidence に抜粋あり |
| 静的 | workflow 定義の構造 (job の `needs` / `if` / `permissions` / `environment` / `concurrency` / step の並び) の確認。PyYAML による読み出し結果を evidence に記録 |
| CI 実行待ち | 上記で到達できない部分。tasks 5.x / 6.x / 7.x で実測する |

---

## 対応表

### specs/release-workflow/spec.md (ADDED 12 Requirement / 30 Scenario)

#### Requirement: 手動起動と入力の検証

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 不正な version 形式は早期に失敗する | `.github/workflows/release.yml:80-98` (checkout より前の step。正規表現 `^(0|[1-9][0-9]*)\.…(-(alpha|beta|rc)\.(0|[1-9][0-9]*))?$` が先頭ゼロと `pre` / `SNAPSHOT` を弾く)。後続は全 job が `needs: validate` (`:167`,`:172`,`:177`,`:185`,`:221`,`:286`) | 静的 (正規表現が spec 記載の 7 種すべてを拒否することを目視照合)。CI 実行待ち: tasks 5.1 | ✅ |
| main 以外からの本番起動は失敗する | `.github/workflows/release.yml:94-97` (`dry-run != true` かつ `github.ref != refs/heads/main` で失敗) | 静的。CI 実行待ち: tasks 5.1 | ✅ |
| 別 commit を指す同名 tag があれば失敗する | `.github/workflows/release.yml:106-120` (`git rev-list -n 1` と `GITHUB_SHA` の比較。同一なら続行) | 静的。CI 実行待ち: tasks 5.1 | ✅ |
| 配信リポジトリの同名 tag は publish の前に内容で判定する | `.github/workflows/release.yml:130-152` + `scripts/release/check-distribution-tag.sh:49-77` (`git add -A` → `write-tree` と `refs/tags/<v>^{tree}` のハッシュ比較。`absent` / `match` / 失敗の 3 値) | 実測 (evidence/scripts-unit.txt「check-distribution-tag.sh: tag が無い / 同一 / 別内容 / remote で削除済み」4 ケース + 「step の終了ステータス」節で release.yml の `run` をそのまま実行し exit 1 が step に伝わることを確認) | ⚠️ deviation 記録済み (比較手段: `git diff --quiet` → `write-tree` のツリー比較。tasks 4.1) |
| README の version が一致しなければ失敗する | `.github/workflows/release.yml:122-125` + `scripts/release/set-readme-version.py:154-180` (不一致の `<file>:<line>` を列挙して exit 1) | selftest (evidence/scripts-unit.txt「別の version の検査は失敗する」「不一致の行番号が出力される」。本検証でも再実行し成功) | ✅ |
| dry-run 入力は publish 手前で止まる | `.github/workflows/release.yml:414` (`publish` の `if: !inputs['dry-run']`)、`:876`,`:895`,`:903`,`:913` (wait / smoke 3 本も同条件)。validate / test / package / consumer 段には `if` なし | 静的。CI 実行待ち: tasks 5.2 | ✅ |

#### Requirement: 段の構成と順序

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| テストか dry-run が 1 つでも失敗すれば publish しない | `.github/workflows/release.yml:413` (`needs: [validate, ios, android, maui, consumer-ios, consumer-android, consumer-maui]`)。test 3 job は `:165-178` で `verify-{ios,android,maui}.yml` をそのまま `uses:` | 静的 (evidence/release-workflow-static.txt「job ごとの権限」で 15 job の構成を確認) | ✅ |
| 同時に起動した 2 つの実行は直列になる | `.github/workflows/release.yml:38-40` (`concurrency: {group: release, cancel-in-progress: false}`)。publish 側の冪等性は下の「同じ version での再実行」に対応 | 静的 (evidence/release-workflow-static.txt「直列化」)。CI 実行待ち: tasks 5.3 (2 本同時起動の実測) | ✅ |
| dry-run は publish する配布物そのものを検証する | package 3 job が artifact を upload (`:209-217`,`:274-282`,`:370-378`)、consumer 3 job が同じ artifact 名で受け取り (`:387-408`)、publish が同じ artifact を download (`:476-486`) して NuGet push (`:730`) と snapshot push (`:525-540`) に使う。Android のみ publish が再ビルドし比較 (下の Requirement) | 静的。CI 実行待ち: tasks 5.2 (job summary での確認) | ✅ |

#### Requirement: Android 成果物の同一性

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 再ビルドの差異で upload を止める | `.github/workflows/release.yml:636-637` (`compare-maven-artifacts.sh` を `publishToMavenCentral` の**前**に実行) + `scripts/release/compare-maven-artifacts.sh:65-147` (署名 / checksum / `maven-metadata*.xml` を除外、pom / module は byte、aar / jar はエントリ名 + 内容ハッシュ)。同一条件の担保は `:224` と `:417` (ともに ubuntu-24.04)、`:236` と `:443` (ともに JDK 17)、同一 commit (同じ run の checkout) | 実測 (evidence/scripts-unit.txt: 5 ケース。署名の有無だけ / zip のタイムスタンプ・圧縮方法だけ / `maven-metadata-local.xml` の `<lastUpdated>` だけの差は一致、aar の 1 エントリの内容差・pom の byte 差・片側のみのファイルは差異として列挙し失敗) | ✅ |

#### Requirement: publish の順序

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 途中で失敗すれば tag は作られない | `.github/workflows/release.yml` の publish job の step 並び: snapshot commit `:525` → Maven upload `:553` → NuGet push `:725` → Maven release `:751` → 配信リポジトリ tag `:762` → monorepo tag `:782` → Release `:798`。tag / Release の 3 step に `if:` は無く、先行 step の失敗で job が止まる | 静的 (step の並びと `if` の有無)。CI 実行待ち: tasks 7.3 | ✅ |
| スナップショット commit は tag より前に push される | `.github/workflows/release.yml:525-540` が commit のみ push (`git tag` を含まない)、tag は `:762-780` | 静的 | ✅ |

#### Requirement: Maven Central の 2 段操作

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| upload 後は保留状態で止まる | `.github/workflows/release.yml:640` (`publishToMavenCentral` — 自動 release しないタスク)、`:647-651` (deployment ID をログから抽出、取れなければ exit 1)、`:706-714` (ID を artifact へ保存)。release は別 step `:751` | selftest (evidence/scripts-unit.txt [状態の解釈] [release])。**upload 完了時点で VALIDATED になること自体は tasks 1.1 の実測待ち** (plugin の挙動に依存) | ✅ (1.1 実測待ち) |
| NuGet push の後に release される | `.github/workflows/release.yml:751-760` (`release` → `wait-published`)。step 順で NuGet push `:725` の後。`scripts/release/central-portal.sh:207-222` が release 直前に VALIDATED を再確認し、以外なら何も送らず失敗 | selftest (「VALIDATED なら release する」「VALIDATED 以外は release しない」「release しないときは状態照会だけで止まる」) | ✅ |
| 失敗時に保留 deployment が残らない | `.github/workflows/release.yml:836-858` (`if: failure()` で drop、削除できたときだけ引き継ぎ ID を空にする `:850-855`) + `:862-869` (空 ID の再保存) + `scripts/release/central-portal.sh:253-278` (VALIDATED / FAILED のみ DELETE、PUBLISHING / PUBLISHED / PENDING / VALIDATING / NOT_FOUND は送らず理由を出力)。ID を出力へ書く前に落ちた場合の拾い直しは `:672-703` | selftest (evidence/scripts-unit.txt [drop の状態分岐] 全 6 状態 + drop 済み ID) + モック実測 (同「保留 deployment の ID をログから拾い直す経路」3 ケース) | ✅ |
| release の応答が失われても再実行で整合する | `.github/workflows/release.yml:600-602` (引き継いだ ID が PUBLISHING → `wait-published` のみ。`release_needed` は false のままで `:752` の release step が skip される。drop も呼ばない) | selftest ([wait-published] PUBLISHING→PUBLISHED で成功) + モック実測 (evidence/release-workflow-static.txt「publish の Maven ステップの分岐」の「あり / PUBLISHING」行)。CI 実行待ち: tasks 5.6 | ✅ |

deviation: 前 attempt の deployment が PENDING / VALIDATING のときの待機 (`:580-594`、30 秒間隔 / 上限 30 分) は spec に無い状態の扱いとして記録済み。

#### Requirement: 署名の生成確認

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 署名鍵が渡っていなければ upload しない | `.github/workflows/release.yml:635` (`check-signatures.sh` を upload の前に実行) + `scripts/release/check-signatures.sh:39-64` (aar / pom / jar / module の各成果物に `.asc` の対を要求、0 件でも失敗) | 実測 (evidence/scripts-unit.txt: 署名なし発行物で 5/5 欠落を列挙して exit 1、`.asc` を揃えれば exit 0、aar の `.asc` だけ外すと 1 件を指して exit 1) | ✅ |

#### Requirement: NuGet.org への push

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 3 パッケージが同じ version で公開される | `.github/workflows/release.yml:717-721` (`NuGet/login` による OIDC。長期 API key の secret は無し — evidence/release-workflow-static.txt の secrets 一覧に API key は存在しない) + `:725-747` (binding 2 件 → facade の順、nupkg / snupkg を対で push) | 静的 + 実測 (evidence/premise-spike-pack.txt 10 節: 3 パッケージすべてで nupkg / snupkg の対が生成され、snupkg が pdb を含む)。CI 実行待ち: tasks 7.3 | ✅ |
| binding の push 失敗で facade は公開されない | `.github/workflows/release.yml:729-747` (`set -euo pipefail` の直列ループ。順序は iOS → Android → facade) | 静的 | ✅ |
| 既に存在する version は skip される | `.github/workflows/release.yml:745` (`--skip-duplicate`) | 静的 | ✅ |

#### Requirement: tag と GitHub Release

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| prerelease の suffix で prerelease になる | `.github/workflows/release.yml:807-812` (`--generate-notes`、`case "${KS_VERSION}" in *-*) args+=(--prerelease)`)。tag 2 本は `:762-780` (配信リポジトリ) と `:782-796` (monorepo、接頭辞なし)。本文の分類は `.github/release.yml:7-28` | 静的 (version 形式が `X.Y.Z` / `X.Y.Z-{alpha|beta|rc}.N` に限られるため `*-*` は prerelease とだけ一致)。CI 実行待ち: tasks 7.3 | ✅ |
| 正式版は prerelease にならない | 同上 (`*-*` に一致しないため `--prerelease` が付かない) | 静的 | ✅ |

#### Requirement: 同じ version での再実行

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 部分 publish を同じ version で埋める | snapshot commit は差分なしで skip `:534-535`、Maven は前 attempt の ID の状態で分岐 `:578-620` / 公開済みなら upload skip `:627-628`、NuGet は `--skip-duplicate` `:745`、配信リポジトリ tag は `match` で skip `:770-772`、monorepo tag は同 commit で skip `:785-792`、Release は既存なら skip `:803-805`。別 commit の monorepo tag / 内容の異なる配信リポジトリ tag は失敗 (`:787-789`,`:774-777`) | モック実測 (evidence/release-workflow-static.txt「publish の Maven ステップの分岐」7 ケース) + selftest。CI 実行待ち: tasks 5.6 | ✅ |
| 全て完了済みの再実行は何も重複させない | 「失敗した job から再実行」で publish job は再実行されない (GitHub の挙動)。全 step 再実行になった場合も上記 skip 経路で重複しない。手順書に明記: `kasane/handbook/cross/release-procedure.md:146` | 静的 + 手順書。CI 実行待ち: tasks 7.3 | ✅ |

#### Requirement: 反映待ちと smoke

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 反映を待ってから smoke する | `.github/workflows/release.yml:873-888` (`wait-for-registries` job、`needs: publish`) + `scripts/release/wait-for-registries.sh:86-132` (30 秒間隔 / 上限 2700 秒、Maven Central の pom + nuget.org 3 Package ID の flat container index の 4 件すべて)。smoke 3 job は `:892-917` で `needs: wait-for-registries` | 実測 (evidence/scripts-unit.txt「wait-for-registries.sh の補足確認」: 未反映の繰り返し報告と上限失敗、index.json の判定を大小文字・不在・壊れた JSON で確認)。正ケース (4 件揃って exit 0) は公開物が無いため CI 実行待ち: tasks 7.3 | ✅ |
| smoke 失敗でも tag は残る | tag / Release の取り消し処理は実装に存在しない (`.github/workflows/release.yml` に revert / delete の step なし)。smoke は publish の後段なので job の失敗が workflow の失敗として報告される | 静的 (不在の確認)。CI 実行待ち: tasks 7.3 | ✅ |

#### Requirement: secrets と権限の範囲

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| publish 以外の job は書き込み手段を持たない | `.github/workflows/release.yml:32-33` (workflow 既定 `contents: read`)、`:420-425` (publish のみ `environment: release` と `contents: write` / `id-token: write`)。再利用可能 workflow の呼び出しに `secrets:` ブロックなし | 静的 (evidence/release-workflow-static.txt「job ごとの権限」15 job 一覧 + 「secrets の参照位置」— `secrets.` の 13 参照がすべて publish job の行範囲 411〜858 に収まり、`secrets: inherit` はコメント 1 行のみで指定は存在しない) | ✅ |
| main 以外から Environment は参照できない | 実装側の対応物は手順書 `kasane/handbook/cross/release-procedure.md:89-93` (Environment `release` の Deployment branches を `Selected branches` で `main` に限定)。workflow 側は `:420` の `environment: release` | 手順書。**GitHub 設定はオーナー作業 (tasks 6.2)** | ✅ (6.2 待ち) |

#### Requirement: README のインストール例の version 整合

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 置換で 6 行が同じ値になる | `scripts/release/set-readme-version.py:39-65` (README 2 枚 × 3 対象、コードブロック内限定)、`:132-151` (置換)、`:102-107` (SwiftPM は `from:` でも `exact:` へ揃える) | selftest (evidence/scripts-unit.txt [置換] 節 7 件 + [実物の README に対する疎通] 節。本検証でも再実行し「失敗なし」) | ✅ |
| 該当行が見つからなければ失敗する | `scripts/release/set-readme-version.py:110-138` (各ファイル各対象がちょうど 1 行でなければ problems に積み、1 件でもあれば何も書き換えず exit 1) | selftest ([該当行が確定できない場合] 節: 0 行 / 2 行の両ケースで置換せず失敗、ファイルと対象と行数を出力、他方の README も書き換えない) | ✅ |

---

### specs/verification-ci/spec.md (MODIFIED 1 Requirement / 4 Scenario)

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 検査未通過のマージ拒否 | GitHub の branch protection (7 job 必須)。`main` 側の payload は `kasane/handbook/cross/release-procedure.md:42-69` | 手順書。**オーナー作業 (tasks 6.1)** | ✅ (6.1 待ち) |
| 直 push の拒否 | 同上 (`allow_force_pushes: false` / PR 必須。`.github/workflows/ci.yml:9-16` の trigger は `develop` / `main` への PR と `develop` への push) | 手順書。**オーナー作業 (tasks 6.1)** | ✅ (6.1 待ち) |
| develop 以外から main への PR は失敗する | `.github/workflows/ci.yml:82-105` (lint job の先頭 step。`github.base_ref == 'main'` のとき head の repository が自リポジトリかつ head_ref が `develop` でなければ exit 1。push と develop 向け PR では step ごと skip) | 静的。CI 実行待ち: tasks 5.7 | ✅ |
| main が保護された default branch である | `kasane/handbook/cross/release-procedure.md:28-75` (main の作成 → 完全 payload の PUT → `default_branch=main` への PATCH → 読み直しでの確認) | 手順書。**オーナー作業 (tasks 6.1)** | ✅ (6.1 待ち) |

---

### specs/maui-nuget-distribution/spec.md (ADDED 1 + MODIFIED 1 Requirement / 5 Scenario)

#### Requirement: restore 元の固定 (ADDED)

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 複数ソース環境でも nuget.org だけから取得する | `maui/nuget.config` (packageSources を clear して nuget.org のみ + packageSourceMapping で `*` を nuget.org へ)。`samples/maui` と `verification/maui` は `maui/` の外にあるため対象外 (ディレクトリ配置を確認) | 実測 (evidence/premise-spike-pack.txt 9 節: `maui/` 配下で `dotnet nuget list source` が nuget.org 1 件、facade の `dotnet restore --force` が exit 0 / NU1507 0 件。リポジトリルートでは 2 ソースのまま) | ⚠️ deviation 記録済み ([付随修正] ワーカーの環境制約によりオーケストレーターが作成) |

> 注記: `maui/nuget.config` は本検証の実行環境でも `**/NuGet.Config` を対象とする拒否ルールで内容を直接読めなかった (実装ワーカーが遭遇したのと同じ制約)。ファイルの存在と挙動 (ソース 1 件 / NU1507 0 件) は evidence 9 節の実測で確認している。

#### Requirement: 3 パッケージの構成と内容 (MODIFIED)

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 3 パッケージのローカル pack | 既存の pack 構成 (`maui/Directory.Build.props` / 各 csproj) を維持。今回の改変で nuspec が変わらないことを確認済み | 実測 (evidence/premise-spike-pack.txt 4 節: 改変前後で facade / Android binding の nuspec に差分なし = 依存グループは不変。3 nupkg 生成、facade に native 成果物なし) | ✅ |
| 自 assembly 用 aar が nupkg に入らない | `maui/Directory.Build.targets:60-77` (`KsExcludeGeneratedAarFromPackage`、`AfterTargets="_IncludeAarInNuGetPackage"` で `TfmSpecificPackageFile` から `$(OutputPath)$(TargetName).aar` を Remove)。適用条件は `:47` (`IsPackable == true` かつ TFM の platform が android) | 実測 (evidence/premise-spike-pack.txt 4 節: 改変後の 3 nupkg から `KsSettingsView.Maui.aar` / `KsSettingsView.Binding.Android.aar` が消え、Gradle 由来の `kssettingsview-release.aar` / `kssettingsview-bridge-release.aar` と dll は残る) | ⚠️ deviation 記録済み (結線方法: `TargetsForTfmSpecificContentInPackage` への追記 → `AfterTargets`。design Decision 10 / tasks 1.2・3.2) |
| 生成 aar に自前の内容が入ると pack が失敗する | `maui/Directory.Build.targets:50-59` (`KsCheckGeneratedAarEntries`。aar 不在で Error、`^jni/[^/]+/libandroidx\.graphics\.path\.so$` 以外のエントリがあれば Error)。除外ターゲットの `DependsOnTargets` に置き実行順を確定 (`:64`) | 実測 (evidence/premise-spike-pack.txt 5 節: 生成 aar に `res/values/dummy.xml` を人為的に追加すると pack が error で失敗し、`res/`, `res/values/`, `res/values/dummy.xml` を列挙。nupkg は生成されず、原状復帰も確認) | ✅ |
| 利用者の Android Release ビルドに重複警告が出ない | 上記の除外の帰結 | 実測 (evidence/premise-spike-pack.txt 6 節(a): 改変後フィードでの消費者検証は Android Release が 0 警告 0 エラー、XA4301「検出なし」) | ✅ |

> MODIFIED の既存部分 (facade の TFM group 別依存、binding の Description、AndroidX Lifecycle.LiveData 2.11.0.1、iOS の xcframework binding resource package) はこの change で触れておらず、nuspec 無差分 (evidence 4 節) で不変を確認した。旧挙動のテスト残存なし。

---

### specs/consumer-verification/spec.md (MODIFIED 1 Requirement / 4 Scenario)

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 依存警告で失敗する | 既存: `verification/maui/VerificationApp.csproj:44` (`WarningsAsErrors` に NU1605 / NU1608 / NU1107)。この change では変更なし | 既存 (前 change の証跡) | ✅ |
| binding の version 不一致を検出する | 既存: `verification/maui/check-dependencies.py` (facade と binding 2 件の解決版一致を検査、解決版と取得元を出力)。`verification/maui/build-consumer.sh:92` から呼ぶ | 既存 (前 change の証跡) | ✅ |
| native ライブラリの重複で失敗する | **新規**: `verification/maui/build-consumer.sh:95-107` (Android Release ビルドの出力を tee で保存 → `grep -F XA4301` → 検出時は重複パスを evidence に出して `ksv_fail`) | 実測 (evidence/premise-spike-pack.txt 6 節(b): 改変前フィード (生成 aar あり) で XA4301 4 件の native ライブラリのパスを列挙して EXIT=1) | ✅ |
| その他のビルド警告は失敗にしない | `verification/maui/build-consumer.sh:95-107` が XA4301 だけを検出対象にする (`WarningsAsErrors` への昇格を行わない) | 実測 (evidence/premise-spike-pack.txt 6 節(a) で XA4301 以外の警告を失敗にしない形であることを確認、7 節では警告 9 件を伴うビルドが成功) + 静的 (grep 対象が XA4301 のみ) | ✅ |

---

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の完了状況 | チェック済み 20 件 (1.2 / 2.1〜2.6 / 3.1〜3.4 / 4.1〜4.6 / 5.4 / 5.5) はすべて対応表の実装箇所と一致。**虚偽のチェックなし**。未チェック 14 件 (1.1 / 1.3 / 1.4 / 1.5 / 5.1〜5.3 / 5.6 / 5.7 / 6.1〜6.3 / 7.1〜7.6) はコンテキストパッケージの既知の先送りと一致し、判定に含めない |
| 逆流検査 | `git log -- proposal.md design.md specs/` の結果は提案化コミット `4c04878` の 1 件のみ。`git status` でも足場アーティファクトは未変更 (`tasks.md` のチェック更新のみ)。**逆流なし** |
| 未記録乖離 | ❌ 0 件のため該当なし。deviation.md の 3 件 (pack 拡張点の結線 / 配信リポジトリ tag の比較手段 / PENDING・VALIDATING の待機) はすべて対応表で ⚠️ として突き合わせ済み |
| 付随修正 | deviation.md の `[付随修正]` 1 件 (`maui/nuget.config` の作成主体)。Requirement を持つ変更ではなく作業主体の記録であり、対応する Scenario (restore 元の固定) は実測で充足を確認済み |
| diff にあって Scenario に対応しない変更 | `.github/workflows/spike-release-premise.yml` (tasks 1.3 / 1.4 の実測用の一時 workflow) — deviation.md に「1.3 / 1.4 の実測完了後に削除する」として記録済み。`AGENTS.md` の 1 行追加と `kasane/handbook/cross/index.md` の 1 行追加は tasks 4.4 / 4.5 の成果物で Requirement「README のインストール例の version 整合」「secrets と権限の範囲」に紐づく |
| テストの実行 | 手元で実行できるものはすべて成功。`scripts/release/central-portal.sh --selftest` → 「失敗なし」(exit 0)、`python3 scripts/release/set-readme-version.py --selftest` → 「失敗なし」(exit 0)。shell script 5 本の `bash -n` すべて OK。いずれも evidence の採取ログと一致 |
| UI 変更 | 対象外 (この change に `ui/` はない) |

## CI 実行待ちの一覧 (実装あり / 実測は tasks 5.x・6.x・7.x)

| Scenario | 待ち先 |
|---|---|
| 不正な version 形式は早期に失敗する / main 以外からの本番起動は失敗する / 別 commit を指す同名 tag があれば失敗する / README の version が一致しなければ失敗する | tasks 5.1 |
| dry-run 入力は publish 手前で止まる / dry-run は publish する配布物そのものを検証する | tasks 5.2 |
| 同時に起動した 2 つの実行は直列になる (動的部分) | tasks 5.3 |
| 部分 publish を同じ version で埋める / release の応答が失われても再実行で整合する | tasks 5.6 |
| develop 以外から main への PR は失敗する | tasks 5.7 |
| main 以外から Environment は参照できない | tasks 6.2 |
| 検査未通過のマージ拒否 / 直 push の拒否 / main が保護された default branch である | tasks 6.1 |
| upload 後は保留状態で止まる (VALIDATED の確認) | tasks 1.1 |
| 途中で失敗すれば tag は作られない / 3 パッケージが同じ version で公開される / prerelease の suffix で prerelease になる / 反映を待ってから smoke する / smoke 失敗でも tag は残る / 全て完了済みの再実行は何も重複させない | tasks 7.3 |

## 所見 (判定に影響しない観察)

- `publish` job の配信リポジトリ tag の再検査 (`.github/workflows/release.yml:545-549`) はスナップショット commit の push の**後**に置かれている。Requirement「publish の順序」と Scenario「スナップショット commit は tag より前に push される」が commit 先行を要求しており、作業コピーの clone / sync がこの step で行われる構造上の帰結。不可逆な公開 (Maven upload) の前には検査が入っており、Requirement「段の構成と順序」が挙げる再検査対象 (tag・公開済み version) は書き込み前の `Re-check external state` (`:452-474`) で押さえられているため、乖離としない。

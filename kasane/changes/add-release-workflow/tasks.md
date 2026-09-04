# Tasks: add-release-workflow

## 1. 前提の実測 (机上確定の裏取り。覆ったら実装を進めずエスカレーションする)

- [ ] 1.1 Central Portal: vanniktech plugin 0.37.0 の `publishToMavenCentral` を `-Pversion=0.0.0-spike.1` 相当のリリース形式 version と署名鍵ありで実行し、ログの `deployment id:` 行を抽出できること、Portal API の `POST /status?id=` が VALIDATED を返すこと、`DELETE /deployment/<id>` で drop できることを確認する (公開しない。spike の deployment は必ず drop する)。PUBLISHING / PUBLISHED の deployment に `DELETE` が拒否されることも確認する。あわせて「座標 + version が公開済みか」を返すエンドポイントの有無を公式ドキュメントで確認し、無ければ `repo1.maven.org` の HEAD を採用する (→ Requirement: Maven Central の 2 段操作 / 同じ version での再実行)
- [x] 1.2 pack 拡張点: `maui/Directory.Build.targets` で `TargetsForTfmSpecificContentInPackage` の末尾に生成 aar を `TfmSpecificPackageFile` から除くターゲットを足し、nupkg から `KsSettingsView.Maui.aar` / `KsSettingsView.Binding.Android.aar` が消えること、生成 aar のエントリ一覧が `jni/*/libandroidx.graphics.path.so` のみであることを確認する (→ Scenario: 自 assembly 用 aar が nupkg に入らない)
- [ ] 1.3 Android 発行物の再現性: ubuntu-24.04 の同じ JDK で `publishToMavenLocal -Pversion=<v>` を 2 回 (別 checkout) 実行し、pom / module の byte 一致と、aar / sources jar / javadoc jar のエントリ名・内容の一致を確認する。比較 script (`scripts/release/compare-maven-artifacts.sh`) の雛形をここで作る (→ Requirement: Android 成果物の同一性)
- [ ] 1.4 GitHub Actions の配線: 呼び出し側 job が upload した artifact を `verify-consumer-*.yml` の `artifact` 入力で受け取れること (phase-7 で 1 回実証済み。release.yml の artifact 名で再確認)、同じ run の「失敗した job から再実行」で前回 attempt が upload した artifact を download できることを一時 workflow で確認する (→ Requirement: 段の構成と順序 / Maven Central の 2 段操作)
- [ ] 1.5 前提が覆った場合 (ID がログに出ない / aar を除けない / Android 発行物が再現しない / artifact が渡らない) は結果を記録し、design.md の該当 Decision の見直し (再現しない場合は dry-run の保証範囲の縮小) をオーナーへ上げる

## 2. scripts

- [x] 2.1 `scripts/release/central-portal.sh`: サブコマンド `status <id>` / `release <id>` (release 前に VALIDATED を再確認) / `wait-published <id>` / `drop <id>` (VALIDATED / FAILED のときだけ DELETE、それ以外は何もせず理由を出力) / `published <version>` (1.1 の結果に応じて Portal API か `repo1.maven.org` の HEAD)。認証は環境変数の User Token ペアから Bearer を組み立てる。`--selftest` で URL 組み立てと応答の解釈をローカルで検査する (→ Requirement: Maven Central の 2 段操作 / 同じ version での再実行)
- [x] 2.2 `scripts/release/set-readme-version.py <version>` / `--check <version>`: README.md / README_ja.md の 3 行 (SwiftPM `exact: "..."`、`jp.kamusoft:kssettingsview:...`、`Version="..."`) を置換・検査する。該当行が各ファイルで 1 行ずつ見つからなければ失敗。`--selftest` を持つ (→ Requirement: README のインストール例の version 整合)
- [x] 2.3 `scripts/release/wait-for-registries.sh <version>`: Maven Central (`repo1.maven.org` の該当ディレクトリ) と nuget.org (3 Package ID それぞれの flat container の index) を 30 秒間隔でポーリングし、上限 45 分で失敗する (→ Requirement: 反映待ちと smoke)
- [x] 2.4 `scripts/release/check-signatures.sh <mavenLocal の jp/ 配下>`: 各成果物 (aar / pom / sources jar / javadoc jar / module) に対応する `.asc` の存在を検査する (→ Requirement: 署名の生成確認)
- [x] 2.6 `scripts/release/compare-maven-artifacts.sh <package 段の jp/> <再ビルドの jp/>`: 署名ファイルを除き、pom / module は byte 比較、アーカイブはエントリ名と内容の比較で差異を列挙し、差異があれば失敗する (1.3 の雛形を仕上げる) (→ Requirement: Android 成果物の同一性)
- [x] 2.5 CI の lint job に 2.1 / 2.2 の `--selftest` を追加する (bash / python のみで macOS ランナーを起こさない) (→ Requirement: README のインストール例の version 整合)

## 3. MAUI パッケージングの改変

- [x] 3.1 `maui/nuget.config`: packageSources を clear して nuget.org のみ、packageSourceMapping で `*` を nuget.org に割り当てる。作業機 (複数ソース) で NU1507 が消えることを確認する (→ Requirement: restore 元の固定)
- [x] 3.2 既存の `maui/Directory.Build.targets` に追記する (既存の buildTransitive の Import と icon の ItemGroup は維持): `IsPackable=true` かつ `net10.0-android` のプロジェクトで、(a) 生成 aar のエントリを列挙し `jni/*/libandroidx.graphics.path.so` 以外があれば pack を失敗させる検査、(b) 生成 aar を `TfmSpecificPackageFile` から除くターゲット。コメントはそのファイルだけで意味が閉じる現在形で書く (SDK が jar にだけ `Pack` を見て native lib には見ないこと、現在の除外方式が成立しなくなったら `_CreateAar` の前に native item を除外する方式を再検討すること。Decision 番号や change 名は書かない) (→ Requirement: 3 パッケージの構成と内容)
- [x] 3.3 `verification/maui` の消費者ビルドで XA4301 を検出したら失敗にする (ビルド出力の grep、または `WarningsAsErrors` に XA4301 を追加) (→ Scenario: native ライブラリの重複で失敗する)
- [x] 3.4 検証ホスト・テストプロジェクトの pack / restore が影響を受けないこと (`IsPackable=false` で 3.2 が発火しない、`verify-maui.yml` の手順が変わらない)、既存の `Directory.Build.targets` の機能 (buildTransitive の props の import、icon の同梱) が pack 結果で維持されていることを確認する

## 4. release workflow

- [x] 4.1 `.github/workflows/release.yml`: `workflow_dispatch` (inputs: `version` 必須 / `dry-run` boolean 既定 false)、冒頭 `env` に固有値 (Maven 座標・Package ID 3 件・配信リポジトリ・artifact 名・Portal URL)。`concurrency: { group: release, cancel-in-progress: false }`。job: `validate` (semver 正規表現 (数値部は `0|[1-9][0-9]*`)、`main` 検査 (dry-run 時は免除)、monorepo の tag 検査 (同名 tag が別 commit なら失敗)、配信リポジトリの tag 検査 (https で clone → `sync-snapshot.sh` → 同名 tag があればそのツリーと `git diff --quiet` で比較、異なれば失敗)、`set-readme-version.py --check`) → `ios` / `android` / `maui` (phase-3 の `verify-*.yml` を `uses:`) と `package-ios` / `package-android` / `package-maui` (version 注入、artifact upload。MAUI は `-p:ContinuousIntegrationBuild=true`、binding 2 件 → facade) → `consumer-ios` / `consumer-android` / `consumer-maui` (`verify-consumer-*.yml` mode=dry-run + version + artifact。`secrets: inherit` は書かない) → `publish` → `wait-for-registries` → `smoke-ios` / `smoke-android` / `smoke-maui` (mode=smoke + version)。`dry-run` 入力時は `publish` 以降を `if:` で skip する (→ Requirement: 手動起動と入力の検証 / 段の構成と順序 / 反映待ちと smoke)
- [x] 4.2 `publish` job (ubuntu-24.04、package-android と同じ JDK、`environment: release`、`permissions: contents: write, id-token: write`、直列): (0) ロック取得後に monorepo tag と公開済み version を再検査、(1) 配信リポジトリを deploy key で clone → `sync-snapshot.sh` → 差分があれば commit → push (tag なし)、(2) 前回 attempt の `central-deployment-id` artifact があれば download して状態で分岐 (VALIDATED → (4) へ / PUBLISHING → `wait-published` / PUBLISHED → skip / FAILED → drop)。無ければ Android を鍵つきで `publishToMavenLocal` → `check-signatures.sh` → `compare-maven-artifacts.sh` (package 段の artifact と比較) → `central-portal.sh published` で公開済みなら skip、そうでなければ `publishToMavenCentral` → deployment ID を抽出 (取れなければ失敗) → `central-deployment-id` artifact に upload、(3) `NuGet/login` → artifact の nupkg / snupkg を binding 2 件 → facade の順に対で `--skip-duplicate` で push、(4) ID があり VALIDATED なら `central-portal.sh release`、(5) 配信リポジトリの tag (同名 tag が既にあり、その commit のツリーが今回のスナップショットと同一なら skip、異なれば失敗) → monorepo の tag (同 commit なら skip、別 commit なら失敗) → `gh release create --generate-notes` (suffix で `--prerelease`、既存なら skip)、(6) `if: failure()` で ID があれば `central-portal.sh drop` (状態を照会し VALIDATED / FAILED のときだけ削除) (→ Requirement: publish の順序 / Maven Central の 2 段操作 / 署名の生成確認 / NuGet.org への push / tag と GitHub Release / 同じ version での再実行 / secrets と権限の範囲)
- [x] 4.3 `.github/release.yml`: 分類 `breaking` / `feature` / `fix` / `docs`、除外ラベル `kasane` / `ci` (→ Requirement: tag と GitHub Release)
- [x] 4.6 `ci.yml` の lint job に「base が `main` の PR は head が `develop` でなければ失敗」の検査ステップを足す (`github.base_ref` / `github.head_ref`。push と develop 向け PR では no-op) (→ Requirement: マージ保護)
- [x] 4.4 AGENTS.md (CLAUDE.md) の docs-refresh 専任の記述に、`set-readme-version.py` による version 置換を例外として 1 行加える (→ Requirement: README のインストール例の version 整合)
- [x] 4.5 リリース手順書を handbook に置く: `kasane/handbook/cross/release-procedure.md` (guide、index に追加。適用のきっかけ: リリースを行うとき・release workflow の secrets / Environment を設定するとき)。内容は `main` の作成 (develop から) + branch protection (7 job) + default branch 切替、Environment `release` (branch policy `main`) と secrets 7 件 (`SIGNING_KEY` は登録直前に export)、配信リポジトリの deploy key (書き込み可) の生成と登録、リリース PR の手順 (docs-refresh 依頼 → `set-readme-version.py` → PR → dispatch)、失敗時の再実行手順、`dry-run` 入力によるリハーサル (→ Requirement: secrets と権限の範囲 / マージ保護 / 同じ version での再実行)

## 5. 検証 (Scenario の確認)

- [ ] 5.1 validate の負ケース: 不正 version 7 種 (形式 4 種 + 先頭ゼロ 3 種)、`develop` からの本番起動、monorepo の別 commit の同名 tag、配信リポジトリの内容が異なる同名 tag、README の version 不一致、がそれぞれ validate で失敗すること (`dry-run` 入力と一時 tag で確認、tag は削除) (→ Scenario: 不正な version 形式は早期に失敗する / main 以外からの本番起動は失敗する / 別 commit を指す同名 tag があれば失敗する / README の version が一致しなければ失敗する)
- [ ] 5.2 `dry-run` 入力で validate → test → package → dry-run が通り、publish 以降が skip され、配信先の状態 (配信リポジトリの tag・Portal の deployments・nuget.org の一覧) が実行前後で同一であること。dry-run の消費者検証が artifact の配布物を解決したことを job summary で確認する (→ Scenario: dry-run 入力は publish 手前で止まる / dry-run は publish する配布物そのものを検証する)
- [ ] 5.3 secrets と権限: release.yml の各 job の `permissions` / `environment` / `secrets` を静的に確認し、`secrets: inherit` が無いことを grep で確認する。`concurrency` の存在と、`dry-run` 入力の実行を 2 つ同時に起動して 2 つ目が待つことを確認する (→ Scenario: publish 以外の job は書き込み手段を持たない / 同時に起動した 2 つの実行は直列になる)
- [ ] 5.6 再実行の分岐: 1.1 の spike deployment (VALIDATED) の ID を `central-deployment-id` として与えた再実行相当の実行で、upload が skip され状態分岐が期待どおりに動くこと、`drop` が PUBLISHING / PUBLISHED で削除しないことを script の単体 (モック応答) で確認する (→ Scenario: 部分 publish を同じ version で埋める / release の応答が失われても再実行で整合する)
- [ ] 5.7 `main` への PR の head 制限: feature branch から `main` への draft PR で lint job が失敗すること、`develop` からの PR で通ることを確認する (→ Scenario: develop 以外から main への PR は失敗する)
- [x] 5.4 script の単体: `set-readme-version.py` の置換 6 行と該当行欠落時の失敗、`central-portal.sh --selftest`、`check-signatures.sh` の `.asc` 欠落検出、`compare-maven-artifacts.sh` の差異検出 (→ Scenario: 置換で 6 行が同じ値になる / 該当行が見つからなければ失敗する / 署名鍵が渡っていなければ upload しない / 再ビルドの差異で upload を止める)
- [x] 5.5 MAUI: 3 パッケージの pack で生成 aar が nupkg に無いこと、生成 aar に `res/` を人為的に足した状態で pack が失敗すること、消費者検証の Android Release で XA4301 が 0 件になること (→ Scenario: 自 assembly 用 aar が nupkg に入らない / 生成 aar に自前の内容が入ると pack が失敗する / 利用者の Android Release ビルドに重複警告が出ない / native ライブラリの重複で失敗する)

## 6. GitHub 設定 (オーナーの手作業、手順書 4.5 に従う)

- [ ] 6.1 `main` を develop から作成し、branch protection (7 job 必須・PR 必須・force-push / 削除禁止、`gh api -X PUT` の完全 payload) を付け、default branch を `main` に切り替える。証跡を evidence に残す (→ Scenario: main が保護された default branch である)
- [ ] 6.2 Environment `release` を作成し deployment branch policy を `main` に限定、secrets 7 件を登録する (`SIGNING_KEY` は登録直前に export し平文をディスクに残さない) (→ Scenario: main 以外から Environment は参照できない)
- [ ] 6.3 配信リポジトリ `KsSettingsView-SPM` に書き込み可の deploy key を登録し、秘密鍵を `SPM_DEPLOY_KEY` に置く (→ Requirement: publish の順序)

## 7. 初回リリース

- [ ] 7.1 docs-refresh をオーナーが依頼する (内容: 「配信準備中」バナー削除 2 枚 × 1 行、Maven / NuGet の未公開表記削除 2 枚 × 2 行、`blob/develop/` → `blob/main/` 2 枚 × 7 箇所、phase-5〜7 で溜まった追随。phase-7 agenda の「docs-refresh 依頼の内容」を参照) (→ proposal: 初回リリースの実施)
- [ ] 7.2 `set-readme-version.py 0.1.0-beta.1` を実行し、7.1 と合わせてリリース PR (develop → main) を作成・マージする (→ Requirement: README のインストール例の version 整合)
- [ ] 7.3 `main` から `0.1.0-beta.1` を dispatch し、publish 全成功 → tag 2 本 → prerelease の Release → 反映待ち → smoke 3 本の成功を確認する。所要時間と job summary を evidence に残す (→ Scenario: prerelease の suffix で prerelease になる / 反映を待ってから smoke する / 公開レジストリからの解決 (consumer-verification、phase-7 未実証))
- [ ] 7.4 Release 本文を手編集で補う (初回は前回 tag が無いため)。nuget.org の README 表示・Maven Central の座標ページ・配信リポジトリの tag を目視確認する (→ Requirement: tag と GitHub Release)
- [ ] 7.5 KsDialogs phase-11 の agenda に逆流の申し送り (release.yml と `scripts/release/` のコピー、Trusted Publisher Policy と Environment の別途作成) を書く (→ proposal: Non-Goals)
- [ ] 7.6 phase-8 agenda の TODO を更新する (所要時間の実測、Portal の公開確認 API の結果、upstream 起票の要否)

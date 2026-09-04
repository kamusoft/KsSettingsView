# phase-8-release-workflow

`workflow_dispatch` で version を入力して起動し、全 platform のビルド・テスト → 消費者 dry-run → Maven Central / NuGet.org への publish → SwiftPM 配信リポジトリへのスナップショット push + tag → monorepo の tag + GitHub Release の順で進む release workflow を新設し、初回リリースを行う (cross/ADR-0020)。

## 論点


## 決定事項

### Central Portal は plugin で upload して保留し、NuGet push 後に Portal API で release する

vanniktech plugin 0.37.0 の `publishToMavenCentral` (自動 release なし = `USER_MANAGED`、既定の `VALIDATED` 待ち) で upload し、plugin のログ 1 行 (`Uploaded bundle to Central Portal as USER_MANAGED, deployment id: <uuid>`) から deployment ID を抜き出して job output で後段へ渡す。NuGet push の後に Central Portal Publisher API (`POST /api/v1/publisher/deployment/<id>`、認証は User Token を `user:token` で base64 した Bearer) で release する。plugin には保留 deployment を後から release するタスクがなく、Portal に deployment 一覧 API もないため、ID のログ抽出と API 直接呼び出しが唯一の手段。cross/ADR-0020 の順序 (upload 保留 → NuGet → Maven release → tag) をそのまま実現でき、NuGet push (不可逆) の時点で Maven 側の検証 (署名・POM 必須項目) が済んでいることを plugin の既定が保証する点を優先した (2026-09-03)。

付随して workflow に組み込むもの: (1) release の直前に `POST .../status?id=` で `VALIDATED` を再確認する、(2) publish 段のどこで失敗しても保留 deployment を drop する後始末ステップ (`if: failure()`、`DELETE .../deployment/<id>` または `dropMavenCentralDeployment --deployment-id=`) を置く、(3) ログから ID が取れなければ (plugin の文言変更に備え) その時点で失敗させる。保留 deployment は Portal 側で 90 日後に自動削除される。

### 配信リポジトリへの push は commit と tag を分け、commit は publish 段の先頭、tag は最後の段に置く

iOS が利用者から解決できる状態になるのは配信リポジトリ (`KsSettingsView-SPM`) に tag が付いた瞬間で、スナップショットの commit を push しただけでは何も公開されない。そこで publish 段の先頭 (Maven upload より前) でスナップショットの commit を push して deploy key の認証と push 経路の失敗を不可逆操作 (NuGet push・Maven release) の前に出し、tag は monorepo の tag を打つ最後の段で monorepo tag の直前に push する。これで iOS の公開瞬間が monorepo の tag と同じ段に揃い、lockstep が崩れる窓が構造的に消える。途中で失敗した場合、配信リポジトリには未 tag の commit が残るが公開されず、次回のスナップショットで上書きされる。書き込み secret は phase-4 の決定どおり書き込み許可付き deploy key をリポジトリ単位 secrets (`SPM_DEPLOY_KEY`) に置く (2026-09-03)。

### job 構成は 6 段。test 段は phase-3 の workflow を無改修で呼び、配布物は別の package 段で作る

| 段 | job | 待つもの | 内容 |
|---|---|---|---|
| 1. validate | 1 (ubuntu) | なし | dispatch 入力の semver 検査、monorepo と配信リポジトリの tag 重複検査 |
| 2. test | 3 (`verify-{ios,android,maui}.yml` をそのまま呼ぶ) | validate | 開発用 version でテスト (version 注入はテスト結果に影響しない) |
| 3. package | 3 (platform 別) | validate (test と並列) | version を注入して配布物を artifact 化。iOS = スナップショット、Android = mavenLocal 出力、MAUI = nupkg 3 件 |
| 4. dry-run | 3 (`verify-consumer-*.yml` mode=dry-run + version + artifact) | package | 段 3 の artifact をそのまま消費者に渡す |
| 5. publish | 1 (直列、`environment: release`) | test と dry-run の両方 | SPM commit push → Maven upload 保留 (`.asc` 生成確認つき) → NuGet push → Maven release → SPM tag → monorepo tag + GitHub Release |
| 6. smoke | 3 (`verify-consumer-*.yml` mode=smoke + version) | publish | 公開レジストリからの解決 (tag との前後は別決定) |

phase-3 の workflow に version 入力を足す案 (テストと配布物生成を同じ job にする) は採らない。改修なしで status check 名が動かず、「dry-run には publish する成果物そのもの」を満たすには version 注入つきの配布物生成がどのみち独立 job として要るため。publish 段は段 3 の artifact を download して使い、MAUI (同じ nupkg を push) と iOS (同じスナップショットを commit) は dry-run が見たものと外に出るものが一致する。Android だけは署名の都合で publish 時に鍵つきで再ビルドして upload する (同一 commit・同一 JDK の別ビルドになる差は受け入れる) (2026-09-03)。

### smoke は monorepo の tag + Release の後に置き、公開レジストリの反映待ち job を挟む

tag と GitHub Release の条件は cross/ADR-0020 どおり publish 段の成功だけとし、smoke (`verify-consumer-*.yml` mode=smoke) はその後の事後確認に位置づける。smoke が失敗する時点で 3 レジストリへの公開は取り消せず、tag を止めても「出ているのに tag も Release も無い」状態が残るだけで利用者を守れないため。反映待ちは release 側に 1 job 置き、Maven Central (`repo1.maven.org` の該当ディレクトリ。公式に 10〜30 分) と nuget.org (flat container の index) を数十秒間隔でポーリングして上限 45 分で失敗させ、揃ってから smoke 3 本を呼ぶ (phase-7 の smoke workflow は反映待ちを持たない)。smoke 失敗は workflow 全体の失敗として知らせ、Release は publish 段で作ったままにする。受け皿は smoke job だけの再実行と、それでも通らなければ原因に応じた修正版リリース (2026-09-03)。

### 失敗時は同じ version で「失敗した job から再実行」し、publish 段の全ステップを冪等にする

部分 publish (例: NuGet は出たが Maven は出ていない) を同じ version で埋められないと、その version は lockstep が崩れた欠番として残り cross/ADR-0019・0020 の約束を破るため、publish 済み version の再実行を禁止して次の version で出し直す案は採らない。GitHub Actions の再実行は job 単位で、publish 段 (直列 1 job) は先頭からやり直しになるため、ステップごとに存在検査を入れて途中再開を成立させる (2026-09-03)。

| ステップ | 再実行時の判定 |
|---|---|
| validate | 同名 tag が別 commit を指していたら失敗 (同じ commit なら続行) |
| SPM commit push | 差分ゼロなら commit を skip |
| Maven upload | Central に公開済みなら skip (Portal API の公開確認、TODO で裏取り) |
| NuGet push | `--skip-duplicate` |
| Maven release | このランで upload した deployment ID が無ければ skip |
| SPM tag / monorepo tag | 同じ commit の tag があれば skip、別 commit なら失敗 |
| GitHub Release | 既存なら更新しない |

保留中の Maven deployment は publish 段の後始末 (失敗時 drop) が消す前提。後始末が失敗して残っても 90 日で自動削除され、再 upload は別 deployment として並ぶだけで害はない。

### リリースは `main` からのみ起動し、secrets は GitHub Environment `release` に集約する

`main` ブランチを作成し (phase-3 申し送りどおり develop と同じ branch protection を付ける)、release workflow の dispatch は `main` からのみ有効にする。secrets を必要とするのは publish 段の 1 job だけで、その job に `environment: release` を付け (NuGet Trusted Publishing のポリシーが Environment 名 `release` に固定済み)、Environment の deployment branch policy を `main` に限定して、作業ブランチからの誤起動を publish 手前で止める。required reviewers はオーナー 1 人の自己承認になるだけで dispatch 自体が手動ゲートのため付けない。起動できる人は cross/ADR-0024 (collaborators only) で既に限られており追加制限しない。`develop` から直接起動する案は、作業途中の commit からも起動できてしまうため採らない。リリースの単位は develop → main の PR マージ + dispatch になる。(2026-09-03 提案化で追加) default branch は `main` に切り替える — リポジトリのトップと `skills/` のコピー元が最新リリースの状態になり NuGet 同梱 README と揃うため。README 2 枚の `blob/develop/` (各 7 箇所。`skills/` には無い) リンクは `blob/main/` に付け替える。新規 PR の base 既定が `main` になる点は運用の注意として残る。secrets の登録はオーナーの手作業で、手順書を artifacts に残す (2026-09-03)。

| Environment `release` の secret | 中身 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` | Central Portal の User Token のペア (ログイン資格情報ではない) |
| `SIGNING_KEY` | GPG 秘密鍵の armored export (平文をディスクに残さないため登録直前に export) |
| `SIGNING_KEY_ID` / `SIGNING_PASSWORD` | `96DB9B8F` / パスフレーズ (無ければ空) |
| `NUGET_USER` | nuget.org のユーザー名 (メールではない) |
| `SPM_DEPLOY_KEY` | 配信リポジトリの書き込み用 deploy key 秘密鍵。phase-4 決定「リポジトリ単位 secrets」の理由 (organization secrets だと KsDialogs と鍵を共有する) は Environment でも保たれるため置き場所を読み替える |

### NuGet.org は Trusted Publishing (GitHub Actions OIDC) で push し、API key は保管しない

論点「Trusted Publishing の利用可否」は「可」で解消し (2026-09-02)、nuget.org の Trusted Publisher Policy をオーナーが作成済み (Active)。内容は Package Owner `kamusoft` / GitHub Actions / Repository `kamusoft/KsSettingsView` / Workflow File `release.yml` / Environment `release` / Scopes: Push new packages and package versions のみ (Unlist は付与しない) / Glob `KsSettingsView.*`。workflow 側の前提: ファイル名は `.github/workflows/release.yml`、publish job に `environment: release` と `permissions: id-token: write` を宣言し、push 前に `NuGet/login@v1` (`user:` に `NUGET_USER`) で得た `NUGET_API_KEY` 出力を `--api-key` に渡す (`--skip-duplicate` 併用)。ポリシーは repo 単位なので KsDialogs には別ポリシー (Repository `KsDialogs` / Glob `KsDialogs.*`) を作る。

### version 注入の配線は既存の受け口をそのまま使い、SwiftPM は配信リポジトリの tag が version になる

Gradle は `-Pversion=<version>` (`android/build.gradle.kts` が全モジュールへ適用、無ければ `0.1.0-SNAPSHOT`)、MSBuild は `-p:Version=<version>` (`maui/Directory.Build.props` の既定 `0.0.0-dev` を上書き) で、いずれも実装済み (cross/ADR-0020 の 2026-09-02 追記)。MAUI の pack は `-p:ContinuousIntegrationBuild=true` を併せて注入し、csproj 単位で binding 2 件 → facade の順に行う。SwiftPM はファイルに version を持たず、配信リポジトリへ push する tag (dispatch 入力と同一文字列) が version になる (tag の位置は上の決定)。

### 初回リリースは `0.1.0-beta.1`。dispatch 入力は `X.Y.Z(-{alpha|beta|rc}.N)?` だけを通し、suffix があれば GitHub Release を prerelease にする

prerelease の扱いはロードマップの [exploration.md](../../exploration.md) の「prerelease の扱い (2026-08-21 追記)」に記録済み (形式は `X.Y.Z-{alpha|beta|rc}.N`、Maven では同格、`-pre` / `-preview` は使わない) で、そこが「初回を `0.1.0` にするか `1.0.0-beta.1` にするか」を phase-8 の論点として送っていた。オーナー要件「初回リリースはプレリリースであることが利用者に分かること」(2026-09-03 に明示) により、初回は 0.x かつ beta の `0.1.0-beta.1` とする。Issue テンプレート (bug_report / question) の version 欄の例示 `0.1.0-beta.1` とも揃う。`0.1.0` (prerelease を利用者に意識させない) と `1.0.0-beta.1` (README のインストール例と catalog の書き換えが要る) は採らない。validate job の検査は正規表現 `^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$` の 1 本 (ロードマップ制約の帰結。`-pre` / `-preview` は Maven の版比較で正式版より新しいと判定されるため通さない)。suffix があれば GitHub Release を prerelease 印にする。NuGet と SwiftPM は suffix を自動で prerelease 扱いにするが Maven Central では同格に見えるため、その旨は README の prerelease 節 (phase-9 で新設済み) が担う (2026-09-03)。

初回が prerelease になることで README のインストール節に影響が出る: SwiftPM の `from: "0.1.0"` は prerelease を解決しないため `exact: "0.1.0-beta.1"` (または `from: "0.1.0-beta.1"`) に、Maven の `0.1.0` と NuGet の `Version="0.1.0"` も `0.1.0-beta.1` に、初回リリース時の docs-refresh で書き換える (README の状態表記解除の論点で扱う)。

### README のインストール例は具体 version を書き、リリース PR の中で専用 script が置換し、validate が一致を検査する

README 2 枚 (`README.md` / `README_ja.md`) のインストール例は具体 version (初回は `0.1.0-beta.1`) を書く。貼ってそのまま使え、行そのものから prerelease であることが分かるため。更新はリリース PR (develop → main) の中で version 置換専用の script (`scripts/` に置く。SwiftPM の `exact:` / Maven 座標 / NuGet の `Version` の 3 行 × 2 枚を同じ値に置き換え、該当行を検出できなければ失敗する) で行い、validate job は同じ script の check モードで README 2 枚に dispatch の version が含まれることを検査して更新忘れを publish 前に止める。facade の NuGet はルート README を同梱するため、書き換えが pack より前 (PR マージ時点) に済むこの形で同梱 README と一致する。AGENTS.md の「README の追従更新は docs-refresh 経由のみ」は、インストール例の version 更新に限りこの script を例外として 1 行緩める (オーナー承認 2026-09-03)。docs-refresh をリリースごとに回す案は重すぎて不可、プレースホルダ + バッジ案は緩和が可能になったため取り下げ、workflow が main に commit する案は CI に push 権限と branch protection の bypass が要るため採らない (2026-09-03)。

初回リリース時の README 変更は 2 系統に分ける: docs-refresh 1 回 (冒頭バナー削除 2 枚 × 1 行、Maven / NuGet の未公開表記 (Publication status) 削除 2 枚 × 2 行、phase-5〜7 で溜まった追随内容) と、script による version 置換 (`0.1.0` → `0.1.0-beta.1`、SwiftPM は `from:` が prerelease を解決しないため `exact:` へ)。

### GitHub Release ノートは自動生成を既定にし、`.github/release.yml` で分類する。初回だけ手で補う

Release 本文は GitHub の自動生成 (前回 tag から今回 tag までのマージ済み PR の題名) を使い、`.github/release.yml` のラベル分類は最小 (例: `breaking` / `feature` / `fix` / `docs`、除外ラベルに `kasane` 等) から始める。prerelease 印は version の suffix から付ける。CHANGELOG ファイルを正にする案は、変更ごとに人が書く運用と Kasane の proposal との二重管理が増え、ADR-0020 が避けた「リリースのたびに積むコミット」に近づくため採らない。初回リリースは前回 tag が無く全履歴の PR が列挙されるため、Release 作成後の手編集で本文を補う。PR 題名は Release ノートとして読める粒度で書く (commit の接頭辞規約とは別に利用者向けに書く) (2026-09-03)。

### release workflow はリポジトリ内に閉じ、固有値を冒頭の `env` に集約する。KsDialogs への逆流はコピーと値の差し替え

release.yml は KsSettingsView のリポジトリ内で完結させ、リポジトリ固有の値 (Maven 座標、NuGet の Package ID、配信リポジトリ名、artifact 名) を workflow 冒頭の `env` に集約する。KsDialogs へは release.yml と `scripts/` (SPM スナップショット同期・README version 置換・Portal API 呼び出し) をコピーして値を差し替え、KsDialogs の phase-11 (パッケージング、形態 4 つで KMP を含む) の結果に合わせてそちらで手直しする。Trusted Publisher Policy と Environment `release` は KsDialogs 用に別途作る。別リポジトリの共有 workflow を両者から `uses:` する案は、逆流先の形態が未確定で抽象の境界を当てられず、共有側の変更が両リリースに同時に効くため採らない。KsDialogs 側の作業は本ロードマップの非ゴールで、申し送りとして KsDialogs phase-11 の agenda に書く (2026-09-03)。

### NU1507 は `maui/nuget.config` (nuget.org のみ + packageSourceMapping) で原因ごと消す

`maui/` に `nuget.config` を置き、packageSources を `<clear/>` してから nuget.org だけを宣言し、packageSourceMapping で `*` を nuget.org に割り当てる。CPM 導入で複数ソース環境の restore に出る NU1507 が消えるだけでなく、作業機・ランナーのどちらでも `maui/` 配下の restore 元が nuget.org に固定され、ローカルフィードの混入で別物を掴む事故が構造的に起きなくなる。phase-7 の消費者検証が同型の mapping を先行実証済み。影響は `maui/` 配下のみ (`samples/maui` と `verification/maui` は別ツリーで、後者は `-p:RestoreConfigFile=` で自前の設定を選ぶ)。`maui/` 配下でローカルフィードを使うときは `-p:RestoreConfigFile=` で明示する。`NoWarn` で受け入れる案は警告だけ消えて曖昧さが残るため採らない (2026-09-03)。

### XA4301 は、生成 aar を nupkg から落とし、pack 時に「生成 aar が推移依存の `.so` 以外を含まない」ことを検査して恒久対処する

.NET Android SDK は class library の自 assembly 用 aar (`KsSettingsView.Maui.aar` / `KsSettingsView.Binding.Android.aar`) に、推移依存 `androidx.graphics.path` の ABI 別 `.so` を無条件で詰めて nupkg の `lib/` に入れる (jar には `Pack=false` を見るが native lib には見ない SDK 側の非対称。除外の公式プロパティは存在せず、`AndroidLibrary` の `Pack="false"` は効かない。upstream に同型 issue なし)。実測で両 aar の中身はその `.so` 4 ABI 分だけで自前の res / classes / manifest を含まないため、pack 拡張点 (`TargetsForTfmSpecificContentInPackage` の末尾) で生成 aar を `TfmSpecificPackageFile` から除く。利用者側の警告が 0 件になるだけでなく、利用者の Graphics.Path の版がこちらの pack 時と違うときに同梱した古い `.so` が勝つ余地も消える。非公式手段なので、pack 時に生成 aar のエントリが `jni/*/libandroidx.graphics.path.so` 以外を含んだら失敗させる検査を同居させ、自前の内容が入った時点で気づけるようにする (その時点で中身から `.so` を除く方式へ切り替える)。消費者検証 (MAUI) の Release ビルドで XA4301 だけを検出対象に加えて回帰を止める。既知事項として README に記すだけの案は版ずれの余地が残るため採らない (2026-09-03)。

## TODO

- [x] 論点の解消 (2026-09-03 に全件解消)
- [ ] ksn-propose で変更提案を起こす
- [ ] Central Portal Publisher API に「座標 + version が公開済みか」を返すエンドポイントがあるか公式ドキュメントで裏取りする (再実行時の Maven upload skip 判定に使う。無ければ `repo1.maven.org` の HEAD で代替)
- [ ] secrets 登録手順書 (Environment `release` の作成・branch policy・7 secrets・deploy key の生成と配信リポジトリへの登録) を artifacts/ に書く。認証情報の準備状況は [artifacts/credentials-status.md](artifacts/credentials-status.md)
- [x] `main` ブランチの作成タイミングと default branch の切替可否 (2026-09-03: 切り替える。提案化で決定、change に含める)
- [ ] AGENTS.md (CLAUDE.md) の docs-refresh 専任の記述に、インストール例の version 更新 script の例外を 1 行加える (change に同梱)
- [ ] release 全体の所要時間を見積もる (consumer-maui が dry-run・smoke とも約 20 分、Maven Central の反映待ち 10〜30 分)
- [ ] (任意) dotnet/android へ「`CreateAar` が native lib に `Pack` を見ない」非対称を起票する
- [ ] KsDialogs phase-11 の agenda に逆流の申し送りを書く (release.yml と scripts のコピー、Policy / Environment の別途作成)

### `main` の branch protection (phase-3 申し送り)

develop と同じ内容を作成と同時に付ける: 検証 CI 4 job (`ios / verify` / `android / verify` / `maui / verify` / `lint`) + 消費者検証 3 job (`consumer-{ios,android,maui} / verify`) を必須 status check、pull_request 経由を必須 (承認数 0)、force-push 禁止・削除禁止、admin バイパスは緊急時の逃げ道として許容。`gh api -X PUT` は保護設定を全体置換するため既存設定を含む完全な payload を送る (実例: [branch-protection-develop.txt](../../../../changes/archive/2026-09-02-add-consumer-verification/evidence/branch-protection-develop.txt))。

### 申し送りの取り込み状況

| 申し送り (出典) | 取り込み先 |
|---|---|
| phase-4: 配信リポジトリ `KsSettingsView-SPM` 作成済み、`sync-snapshot.sh` はファイル配置のみ、deploy key は phase-8 で作成・登録 | 決定「配信リポジトリへの push」「secrets は Environment」 |
| phase-5: Central Portal User Token 発行済み、GPG 鍵生成・公開済み (`96DB9B8F`)、`mavenCentralUsername/Password` は User Token のペア | [artifacts/credentials-status.md](artifacts/credentials-status.md)、決定「secrets は Environment」 |
| phase-9: README 冒頭バナー 1 行を初回リリースで削除 | 決定「README のインストール例」(実査で未公開表記 2 行も対象に加えた) |
| phase-3: `main` 作成時に branch protection | 上の TODO |
| phase-6: `ContinuousIntegrationBuild` の注入、pack は csproj 単位で binding → facade | 決定「version 注入の配線」 |
| phase-6: NU1507 / XA4301 | 論点として残置 |
| phase-7: 消費者検証の呼び出し契約 (dry-run + artifact / smoke + version)、`secrets: inherit` を使わない、smoke 正ケースの初回実証と失敗時の扱い、所要時間、署名 `.asc` の生成確認、docs-refresh 依頼の併合 | 決定「job 構成」「smoke の位置」「README」。`secrets: inherit` を書かないことは提案化の必須確認事項、所要時間は上の TODO |

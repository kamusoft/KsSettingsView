# Design: add-release-workflow

## Context

cross/ADR-0020 (dispatch 起動・publish 全成功後に tag・version は CI が注入) と phase-4〜7 の成果 (配信リポジトリとスナップショット同期スクリプト、vanniktech plugin による Central Portal 発行構成、NuGet pack、消費者検証の再利用可能 workflow) を 1 本の release workflow につなぎ、初回リリース `0.1.0-beta.1` を行う。フェーズ議論 ([agenda](../../roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md)) で 17 件を決め、その根拠 (scout による vanniktech plugin 0.37.0 のソース・Central Portal Publisher API・dotnet/android の targets の裏取り) は history.md にある。本書は設計判断を Decision 形式で残す。挙動の契約はデルタスペック、作業は tasks.md にある。

## Goals / Non-Goals

- Goals: 1 回の手動起動で 3 platform を lockstep で publish し、tag と Release を publish 全成功後にのみ作る。publish 前に配布物そのものを消費者検証 (dry-run) に通し、publish 後に公開レジストリから smoke する。失敗時は同じ version で再実行できる。secrets は publish job だけが持つ。初回リリースを完了し smoke の正ケースを実証する
- Non-Goals: proposal.md の Non-Goals に同じ (KsDialogs 逆流、共有 workflow 化、upstream 起票、CHANGELOG、smoke 失敗時の自動ロールバック、`README_ja` の内容同期)

## Decisions

### Decision 1: Central Portal は plugin で upload して保留し、NuGet push 後に Portal API で release する

**採用案:** `./gradlew publishToMavenCentral -Pversion=<v>` (自動 release なし = `USER_MANAGED`、plugin 既定の `VALIDATED` 待ち) で upload し、plugin のログ 1 行 (`Uploaded bundle to Central Portal as USER_MANAGED, deployment id: <uuid>`) から ID を抽出して step output に置く。NuGet push の後、`scripts/release/central-portal.sh` が `POST /api/v1/publisher/status?id=` で `VALIDATED` を再確認してから `POST /api/v1/publisher/deployment/<id>` で release する (認証は User Token を `user:token` で base64 した Bearer)。deployment ID は抽出直後に run の artifact (`central-deployment-id`) として保存し、再実行時 (artifact は同じ run の再実行から download できる) は upload の前に状態を照会して分岐する: VALIDATED → release へ / PUBLISHING → PUBLISHED まで待つ / PUBLISHED → skip / FAILED → drop して再 upload。失敗経路 (`if: failure()`) の drop は状態を照会し VALIDATED / FAILED のときだけ `DELETE` する (PUBLISHING / PUBLISHED は drop できない)。ID が抽出できなければその場で失敗する。
**理由:** plugin 0.37.0 には保留 deployment を後から release するタスクがなく、Portal に一覧 API もない (ID のログ抽出と API 直接呼び出しが唯一の手段)。この形なら ADR-0020 の順序をそのまま実現でき、NuGet push (不可逆) の時点で Maven 側の検証 (署名・POM 必須項目) が済んでいることを plugin の既定が保証する。
**代替案:**
- **A: `publishAndReleaseToMavenCentral` の 1 段にし、順序を NuGet → Maven → tag に変える** — ADR-0020 の改訂が要り、Maven の検証失敗が NuGet push の後に来る。却下
- **B: plugin を使わず Portal の upload API へ bundle を自前で送る** — bundle 生成 (署名・checksum・zip 構造) を自前に持つことになり、plugin が担っている検証待ちも再実装になる。却下

### Decision 2: 配信リポジトリへの push は commit と tag を分け、commit を publish 段の先頭、tag を最後の段に置く

**採用案:** publish 段の最初のステップで deploy key (`SPM_DEPLOY_KEY`) を使って配信リポジトリを clone し、`scripts/spm-snapshot/sync-snapshot.sh` で配置 → 差分があれば commit → push する (tag は打たない)。tag は Maven release の後、monorepo の tag の直前に push する。
**理由:** iOS が利用者から解決できるのは tag の瞬間で、commit だけでは公開されない。先頭で push すれば deploy key の認証と push 経路の失敗を不可逆操作 (NuGet push・Maven release) の前に出せ、tag を最後に揃えれば lockstep が崩れる窓が消える。途中失敗で残る未 tag の commit は公開されず次回上書きされる。
**代替案:**
- **A: commit + tag を Maven release の後にまとめて push** — NuGet・Maven 公開後に push 失敗が起こり得る。却下
- **B: commit + tag を Maven upload 直後・NuGet より前に push** — NuGet・Maven 失敗時に iOS だけ先に出て tag 削除が要る。却下

### Decision 3: job は 6 段。test 段は phase-3 の workflow を無改修で呼び、配布物は別の package 段で作る

**採用案:**

| 段 | job | needs | 内容 |
|---|---|---|---|
| validate | 1 (ubuntu) | — | 入力検査 (Decision 6 / 7 の検査を含む) |
| test | 3 (`verify-{ios,android,maui}.yml` を `uses:`) | validate | 開発用 version でテスト |
| package | 3 (ios: ubuntu / android: ubuntu / maui: macos) | validate | version 注入で配布物を artifact に upload。iOS = スナップショット (`sync-snapshot.sh` の出力)、Android = `publishToMavenLocal` の `jp/` 配下 (未署名)、MAUI = `dotnet pack -p:Version -p:ContinuousIntegrationBuild=true` の nupkg / snupkg (binding 2 件 → facade) |
| dry-run | 3 (`verify-consumer-*.yml` mode=dry-run + version + artifact) | package | 段 3 の artifact を消費者に渡す |
| publish | 1 (ubuntu、直列、`environment: release`) | test 3 + dry-run 3 | Decision 1 / 2 / 5 の順序。macOS は不要 (SPM は git、MAUI は artifact の nupkg を push、Android は Gradle) で、package-android と同じランナー OS にして再ビルドの同一性比較 (下記) を成立させる |
| wait + smoke | 1 + 3 | publish | Decision 4 |

dispatch 入力に `dry-run` (boolean、既定 false) を持ち、true なら dry-run 段までで止める (publish 以降を skip)。この場合に限り起動ブランチの制限を外し、リハーサルを任意のブランチから行える。workflow 全体に `concurrency: { group: release, cancel-in-progress: false }` を置いて実行を直列化する (Environment は排他制御にならない)。publish job はロック取得後に tag・公開済み version を再検査する (Decision 5 の判定がそのまま再検査になる)。
**理由:** phase-3 の workflow と branch protection の status check 名に触れない。version 注入はテスト結果に影響しない。「dry-run には publish する成果物そのもの」(phase-7) を満たすには version 注入つきの配布物生成が独立 job として要る。MAUI (同じ nupkg を push) と iOS (同じスナップショットを commit) は dry-run が見たものと外に出るものが一致する。Android は署名の都合で publish job が鍵つきで再ビルドして upload する。同一物ではないため、publish job は再ビルド結果と package 段の artifact を署名ファイルを除いて比較 (pom / module は byte 比較、aar / jar はエントリ名と内容の比較) し、差異があれば upload しない。同じランナー OS・JDK・commit で再現性が成立しなければ (tasks 1.x のスパイク)、dry-run が保証する範囲を Requirement で縮める判断をオーナーへ上げる。
**代替案:**
- **A: `verify-*.yml` に version 入力を足し、テストと配布物生成を同じ job で行う** — phase-3 の 3 本を改修し、テストと配布物生成が直列で長くなる。却下
- **B: package 段を持たず publish job が全部作る** — dry-run に publish 成果物そのものを渡せず、phase-7 の設計と噛み合わない。却下

### Decision 4: smoke は tag + Release の後に置き、公開レジストリの反映待ち job を挟む

**採用案:** publish job が tag と Release を作った後、`wait-for-registries` job が Maven Central (`https://repo1.maven.org/maven2/jp/kamusoft/kssettingsview/<v>/`) と nuget.org (`https://api.nuget.org/v3-flatcontainer/kssettingsview.maui/index.json` に `<v>` が含まれる) を 30 秒間隔でポーリングし (上限 45 分)、揃ったら `verify-consumer-*.yml` を mode=smoke + version で呼ぶ。smoke 失敗は workflow の失敗として報告し、Release と tag は残す。
**理由:** smoke 失敗の時点で公開は取り消せず、tag を止めても「出ているのに tag が無い」状態が残るだけで利用者を守れない。ADR-0020 の tag の条件 (publish 成功) をそのまま保て、再実行の単位が smoke job に閉じる。phase-7 の smoke workflow は反映待ちを持たず、Maven Central は公式に 10〜30 分かかる。
**代替案:**
- **A: smoke を通してから monorepo の tag + Release を作る** — tag の条件に smoke を足す ADR 追記が要り、失敗時は「公開済みだが tag が無い」状態を人が解消する。smoke の間 iOS (配信リポジトリの tag) だけ先行する窓もできる。却下

### Decision 5: publish 段の全ステップを冪等にし、同じ version で「失敗した job から再実行」できるようにする

**採用案:**

| ステップ | 再実行時の判定 |
|---|---|
| validate | monorepo の同名 tag が別 commit を指していたら失敗 (同じ commit なら続行)。配信リポジトリの同名 tag のツリーがスナップショットと異なれば失敗 (不可逆操作の前に検出する) |
| SPM commit push | 差分ゼロなら commit を skip |
| Maven upload | 前回の deployment ID (artifact) があれば状態で分岐 (VALIDATED → release へ / PUBLISHING → 待つ / PUBLISHED → skip / FAILED → drop して再 upload)。ID が無く Central に公開済み (`repo1.maven.org` の HEAD、または Portal API の公開確認があればそれ) なら skip |
| NuGet push | binding 2 件 → facade の順、nupkg と snupkg を対で。`--skip-duplicate` |
| Maven release | このランで upload した deployment ID が無ければ skip |
| SPM tag | 同名 tag のツリーが今回のスナップショットと同一なら skip、異なれば失敗 |
| monorepo tag | 同じ commit の tag があれば skip、別 commit なら失敗 |
| GitHub Release | 既存なら更新しない |

**理由:** GitHub Actions の再実行は job 単位で、publish (直列 1 job) は先頭からやり直しになる。部分 publish (例: NuGet は出たが Maven は出ていない) を同じ version で埋められないと、その version は lockstep が崩れた欠番として残り ADR-0019・0020 の約束を破る。保留中の Maven deployment は Decision 1 の失敗時 drop で消える前提 (後始末が失敗して残っても deployment ID の artifact から状態で復帰でき、それも無ければ 90 日で自動削除され、再 upload は別 deployment として並ぶだけ)。release の応答が失われて PUBLISHING のまま残るケースも ID の状態照会で吸収する。
**代替案:**
- **A: publish 済み version は再実行禁止、次の version で出し直す** — 片側だけ存在する欠番 version が残る。却下

### Decision 6: リリースは `main` からのみ起動し、secrets は GitHub Environment `release` に集約する。default branch は `main` にする

**採用案:** `main` を作成し、develop と同じ branch protection (7 job 必須・PR 必須・force-push / 削除禁止) を付け、default branch を `main` に切り替える。`main` の先端は「最新リリース、またはリリース進行中 (リリース PR マージ後〜publish 成功) のリリース候補」と定義する (README の version はマージ時点で次の version になる)。`main` を base とする PR は head が `develop` のものだけを受け付け、それ以外は CI の lint job の検査で失敗させる (必須 check なのでマージがブロックされる)。validate は `github.ref == refs/heads/main` を要求する (`dry-run` 入力時を除く)。リリース手順 (GitHub 設定・リリース PR・再実行・リハーサル) は handbook `cross/release-procedure.md` (guide) に置く。publish job だけに `environment: release` (deployment branch policy = `main`、required reviewers なし) と `permissions: id-token: write, contents: write` を付ける。他の job は `contents: read` で secrets を受け取らず、`verify-consumer-*.yml` の呼び出しに `secrets: inherit` を書かない。

| Environment `release` の secret | 中身 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` | Central Portal の User Token のペア |
| `SIGNING_KEY` / `SIGNING_KEY_ID` / `SIGNING_PASSWORD` | GPG 秘密鍵の armored export / `96DB9B8F` / パスフレーズ |
| `NUGET_USER` | nuget.org のユーザー名 (`NuGet/login` の `user:`) |
| `SPM_DEPLOY_KEY` | 配信リポジトリの書き込み用 deploy key 秘密鍵 |

**理由:** リリース対象 commit が main の先端 (検証 CI を通った develop → main の PR マージ結果) に一意に定まり、作業ブランチからの誤起動は Environment の branch policy で publish 手前で止まる。branch protection だけでは feature branch → main の PR も 7 job を通ればマージできるため、head の制限を CI で検査する。NuGet Trusted Publisher Policy が Environment 名 `release` に固定済み。required reviewers はオーナー 1 人の自己承認になるだけで dispatch 自体が手動ゲート。default branch を `main` にするとリポジトリのトップと `skills/` のコピー元が最新リリース状態になり NuGet 同梱 README と揃う。phase-4 の「deploy key はリポジトリ単位 secrets」の理由 (organization secrets だと KsDialogs と鍵を共有する) は Environment でも保たれる。
**代替案:**
- **A: `develop` から起動し `main` は作らない** — 作業途中の commit からも起動できる。却下
- **B: default branch は `develop` のまま** — トップに未リリースの変更が先に見え、NuGet 同梱 README と食い違う。README のリンクが壊れないため止める根拠にならない。却下 (提案化で確定)

### Decision 7: README のインストール例は具体 version を書き、リリース PR で専用 script が置換し、validate が一致を検査する

**採用案:** `scripts/release/set-readme-version.py <version>` が README 2 枚の 3 行 (SwiftPM `exact: "..."` / `jp.kamusoft:kssettingsview:...` / `Version="..."`) を同じ値に置換し、`--check <version>` で一致を検査する (該当行を検出できなければ失敗)。validate job が `--check` を呼ぶ。AGENTS.md の「README の追従更新は docs-refresh 経由のみ」に、この script による version 置換を例外として 1 行加える (オーナー承認済み)。
**理由:** 貼ってそのまま使える 1 行が、今の版が prerelease であることを最も明確に伝える (オーナー要件)。人の PR に乗るので workflow に書き込み権限が要らず、ADR-0020 の「CI が bump commit を積まない」も保たれる。facade の NuGet がルート README を同梱するため、書き換えは pack より前 (PR マージ時点) に済んでいる必要がある。
**代替案:**
- **A: リリース PR の中で docs-refresh を回す** — docs-refresh は concepts からの差分更新を承認フロー込みで回す重い道具で、リリースごとには回せない。却下
- **B: プレースホルダ (`X.Y.Z`) + レジストリのバッジで最新版を見せる** — README をリリース非依存にできるが、利用者が version を自分で埋め、prerelease の明示がバッジと説明文に頼る。AGENTS.md の緩和が可能になったため取り下げ
- **C: release workflow が README を書き換えて main に commit する** — CI に main への push 権限と branch protection の bypass が要り、ADR-0020 の bump commit 回避に反する。却下

### Decision 8: Release ノートは自動生成を既定にし、`.github/release.yml` で分類する

**採用案:** `gh release create <v> --generate-notes` (suffix があれば `--prerelease`)。`.github/release.yml` は `breaking` / `feature` / `fix` / `docs` の 4 分類と除外ラベル (`kasane` / `ci`) から始める。初回は前回 tag が無く全 PR が列挙されるため、Release 作成後の手編集で本文を補う。
**理由:** 手で保守するファイルを増やさず、Kasane の proposal との二重管理を避ける。PR 題名はレビューで見ているものを再利用できる。
**代替案:**
- **A: CHANGELOG ファイルを正にし該当節を転記する** — 変更ごとに人が書く運用が要り、ADR-0020 が避けた「リリースのたびに積むコミット」に近づく。却下

### Decision 9: NU1507 は `maui/nuget.config` (nuget.org のみ + packageSourceMapping) で原因ごと消す

**採用案:** `maui/nuget.config` に `<packageSources><clear/><add key="nuget.org" .../></packageSources>` と `<packageSourceMapping><packageSource key="nuget.org"><package pattern="*" /></packageSource></packageSourceMapping>` を置く。
**理由:** CPM 導入で複数ソース環境に出る NU1507 が消えるだけでなく、`maui/` 配下の restore 元が nuget.org に固定され、ローカルフィードの混入で別物を掴む事故が構造的に起きない。phase-7 の消費者検証が同型を先行実証済み。影響は `maui/` 配下のみ (`samples/maui` と `verification/maui` は別ツリー)。
**代替案:**
- **A: `NoWarn` で NU1507 を受け入れる** — 警告だけ消えて複数ソースの曖昧さは残る。却下

### Decision 10: XA4301 は生成 aar を nupkg から落とし、pack 時に生成 aar の中身を検査して恒久対処する

**採用案:** 既存の `maui/Directory.Build.targets` (buildTransitive の import と icon の同梱を担う) に追記して、`TargetsForTfmSpecificContentInPackage` の末尾で `$(OutputPath)$(TargetName).aar` を `TfmSpecificPackageFile` から除くターゲットと、その直前に生成 aar のエントリが `jni/*/libandroidx.graphics.path.so` (推移依存由来の native lib) 以外を含んだら pack を失敗させる検査ターゲットを置く (`IsPackable=true` かつ `net10.0-android` のみ)。`verification/maui` の Release ビルドは XA4301 を検出したら失敗とする (ビルド警告全般は引き続きエラーにしない)。
**理由:** .NET Android SDK は class library の自 assembly 用 aar に推移依存の `.so` を無条件で詰めて nupkg に入れ (jar には `Pack=false` を見るが native lib には見ない非対称)、除外の公式プロパティは存在しない (`AndroidLibrary` の `Pack="false"` は別の意味)。実測で両 aar の中身はその `.so` 4 ABI 分だけで、落としても利用者が失うものはなく、利用者側の Graphics.Path の版がこちらと違うときに同梱した古い `.so` が勝つ余地も消える。非公式手段なので検査を同居させ、自前の res / classes を持った時点で気づけるようにする。
**代替案:**
- **A: 既知事項として README に記す** — 警告 4 件と版ずれの余地が残る。却下
- **B: `BeforeTargets="_CreateAar"` で `@(EmbeddedNativeLibrary)` から除き、生成 aar の中身を空にする** — SDK の内部ターゲット名と増分ビルドのキャッシュ整合 (`_CreateAarCache`) に依存し未検証。生成 aar に自前の内容が入ったときの切替先として残す。今回は採らない
- **C: facade の buildTransitive で利用者の `NoWarn` に XA4301 を足す** — 利用者の他の本物の重複まで隠す。却下

### Decision 11: release workflow はリポジトリ内に閉じ、固有値を冒頭の `env` に集約する

**採用案:** Maven 座標・NuGet の Package ID・配信リポジトリ名・artifact 名・Portal の URL を `release.yml` 冒頭の `env` に置く。KsDialogs へは release.yml と `scripts/release/` をコピーして値を差し替える (申し送りのみ)。
**理由:** KsDialogs はパッケージング未着手 (形態 4 つ、KMP 含む) で逆流先の形が未確定。共有 workflow に抽象化すると境界を当てられず、共有側の変更が両リリースに同時に効く。
**代替案:**
- **A: 別リポジトリの共有 workflow を両者から `uses:`** — 上記のとおり。却下

## Risks / Trade-offs

- 机上確定の前提 (tasks 1.x のスパイクで先に実測する。lessons process L-004): deployment ID のログ文言、Portal API の status / publish / delete と公開確認、pack 拡張点での aar 除外と検査、Android 発行物の再ビルド再現性 (同一 OS・JDK)、reusable workflow の呼び出し側 job から artifact を渡す経路、同じ run の再実行からの artifact download
- `NuGet/login` (Trusted Publishing) の正ケースは事前に検証できない: Environment `release` は `main` 限定で PR の ref から参照できず、NuGet 側のポリシーは workflow ファイル名 `release.yml` まで照合する。初回リリースの本番 run で初めて踏むが、login は SPM commit (未 tag) と Maven upload (保留) の後・NuGet push の前にあり、失敗しても drop で戻れる不可逆操作前の位置にある
- 非公式手段 (Decision 10 の pack 拡張点、Decision 1 のログ抽出) は SDK / plugin の更新で壊れ得る。どちらも「壊れたら失敗する」検査を同居させ、静かに通らないようにする
- Android の publish は dry-run で検証した未署名の成果物ではなく、鍵つきの再ビルドを upload する。同一 commit・同一 JDK の Gradle ビルドで、差は署名の有無だけとして受け入れる
- 初回 publish は取り消せない。dry-run 段 (配布物そのもの) と `dry-run` 入力によるリハーサルで、publish 手前までを事前に通す
- default branch の切替後、新規 PR の base 既定が `main` になる。feature PR は `develop` を明示する (誤って `main` に向けた PR は head 制限の検査で必須 check が失敗し、マージできない)
- 所要時間は 60〜90 分見込み (consumer-maui 約 20 分 × 2、反映待ち 10〜30 分)

## Migration Plan

1. 実装 PR (develop): scripts / `maui/nuget.config` / `Directory.Build.targets` / `release.yml` / `.github/release.yml` / AGENTS.md / 手順書。`dry-run` 入力でリハーサル
2. GitHub 設定 (オーナー、手順書どおり): `main` 作成 + 保護 + default 切替、Environment `release` + secrets 7 件、deploy key の生成と配信リポジトリへの登録
3. 初回リリース PR (develop → main): docs-refresh (オーナー依頼: バナー削除・未公開表記削除・`blob/develop/` → `blob/main/`・phase-5〜7 の追随) と `set-readme-version.py 0.1.0-beta.1`
4. `main` から dispatch (`0.1.0-beta.1`) → smoke の証跡 → Release 本文の手編集

## Open Questions

- Central Portal Publisher API の「座標 + version が公開済みか」を返すエンドポイントの有無 (tasks 1.1 で裏取り。無ければ `repo1.maven.org` の HEAD で代替。deployment ID の artifact がある再実行では不要)
- Android 発行物の再ビルド再現性が成立しない場合の dry-run 保証範囲 (tasks 1.5 でオーナーへ)

## ADR 候補

- 新規起票なし。Decision 1 / 2 / 4 / 5 は cross/ADR-0020 の Consequences「Portal の 2 段階を CI から操作する必要がある」「失敗時は再実行するだけ」の具体化にあたり、蒸留時に ADR-0020 へ追記して accepted へ昇格する (ADR-0019 も同時に)。Decision 6 の「リリースは `main` からのみ・default branch は `main`」はブランチ運用の規範として handbook (cross) に書く候補。Decision 9 / 10 は `maui/` のビルド構成に閉じた判断で、concepts `maui/architecture/binding-build-integration.md` の追随で扱う

# release-workflow デルタスペック

## ADDED Requirements

### Requirement: 手動起動と入力の検証
release workflow は `workflow_dispatch` で version を入力して起動する SHALL。version は `X.Y.Z` または `X.Y.Z-{alpha|beta|rc}.N` (X / Y / Z / N は先頭ゼロを持たない数字。SemVer 2.0.0 の数値識別子) のみを受け付け、それ以外は配布物の生成に入る前に失敗する。`dry-run` 入力 (既定 false) を持ち、true のときは publish 段以降を実行しない。`dry-run` が false のとき、起動ブランチが `main` でなければ失敗する。monorepo に同名の tag が存在し、それが起動 commit と異なる commit を指していれば失敗する (同じ commit を指していれば続行する)。配信リポジトリに同名の tag が存在し、その tag のツリーが起動 commit から生成したスナップショットと異なれば失敗する (同一なら続行する)。README (英語 / `README_ja`) のインストール例に記載された version が入力の version と一致しなければ失敗する。

#### Scenario: 不正な version 形式は早期に失敗する
- **GIVEN** `1.0` / `v1.0.0` / `1.0.0-pre.1` / `1.0.0-SNAPSHOT` / `01.0.0` / `1.00.0` / `1.0.0-beta.01` のいずれかを version に与えた起動
- **WHEN** workflow が起動する
- **THEN** validate で失敗し、テスト・配布物の生成・publish は実行されない

#### Scenario: main 以外からの本番起動は失敗する
- **GIVEN** `dry-run` が false で、`develop` から起動した実行
- **WHEN** validate が実行される
- **THEN** 起動ブランチの理由で失敗し、後続の job は実行されない

#### Scenario: 別 commit を指す同名 tag があれば失敗する
- **GIVEN** monorepo に version と同名の tag が別の commit を指して存在する
- **WHEN** validate が実行される
- **THEN** tag の衝突として失敗する

#### Scenario: 配信リポジトリの同名 tag は publish の前に内容で判定する
- **GIVEN** 配信リポジトリに version と同名の tag が、起動 commit のスナップショットと異なる内容で存在する
- **WHEN** validate が実行される
- **THEN** 配布物の生成・publish に入る前に失敗する (内容が同一なら続行し、publish 段の tag 作成は skip される)

#### Scenario: README の version が一致しなければ失敗する
- **GIVEN** README 2 枚のインストール例が入力の version と異なる値のまま
- **WHEN** validate が実行される
- **THEN** 不一致のファイルと行が出力され、失敗する

#### Scenario: dry-run 入力は publish 手前で止まる
- **GIVEN** `dry-run` を true にして任意のブランチから起動した実行
- **WHEN** workflow が完了する
- **THEN** validate・テスト・配布物の生成・消費者検証 (dry-run) までが実行され、publish・tag・Release・smoke は実行されず、配信先への書き込みは発生しない

### Requirement: 段の構成と順序
release workflow は validate → (test ∥ package) → dry-run → publish → 反映待ち → smoke の順に進む SHALL。test は phase-3 の再利用可能 workflow (`verify-{ios,android,maui}.yml`) をそのまま呼ぶ。package は入力の version を注入して 3 platform の配布物 (iOS のスナップショット、Android の Maven ローカル発行物、MAUI の nupkg / snupkg) を artifact として保存する。dry-run は消費者検証 workflow を `mode=dry-run` + version + その artifact で呼ぶ。publish は test 3 job と dry-run 3 job のすべてが成功したときにのみ実行される。release workflow の実行はリポジトリ全体で直列化され (concurrency group、進行中の実行を打ち切らない)、2 つの実行が同時に publish に進むことはない。publish job はロック取得後に外部状態 (tag・公開済み version) を再検査してから書き込む。

#### Scenario: テストか dry-run が 1 つでも失敗すれば publish しない
- **GIVEN** test または dry-run のいずれか 1 job が失敗した実行
- **WHEN** workflow が進む
- **THEN** publish job は実行されず、配信リポジトリ・Maven Central・NuGet.org への書き込みは発生しない

#### Scenario: 同時に起動した 2 つの実行は直列になる
- **GIVEN** 同じ version で 2 回 dispatch した実行
- **WHEN** 両方が publish に到達しようとする
- **THEN** 後の実行は先の実行の完了を待ち、先の実行が publish を完了していれば後の実行の publish ステップはすべて skip される

#### Scenario: dry-run は publish する配布物そのものを検証する
- **GIVEN** package 段が保存した artifact
- **WHEN** dry-run の消費者検証と publish が実行される
- **THEN** MAUI は同じ nupkg が dry-run で解決され NuGet.org へ push され、iOS は同じスナップショットが dry-run で解決され配信リポジトリへ commit される。Android は publish 時に署名つきで再ビルドされるため同一物ではなく、下の「Android 成果物の同一性」に従う

### Requirement: Android 成果物の同一性
Android の publish は署名鍵を持つ publish job が再ビルドした発行物を upload するため、dry-run が検証した package 段の発行物と同一のファイルではない SHALL NOT。publish job は upload の前に、再ビルドした発行物と package 段の発行物を署名ファイルを除いて比較し、差異があれば upload せずに失敗する SHALL。比較の対象は pom・Gradle module metadata・aar・sources jar・javadoc jar で、アーカイブは内容 (エントリ名と各エントリの内容) で比較する。両 job は同じランナー OS・同じ JDK・同じ commit で実行される。

#### Scenario: 再ビルドの差異で upload を止める
- **GIVEN** package 段の発行物と publish job の再ビルド結果に署名以外の差異がある状態
- **WHEN** publish job の比較ステップが実行される
- **THEN** 差異のあるファイルが出力され、Maven Central への upload は実行されない

### Requirement: publish の順序
publish は 1 つの job で直列に、配信リポジトリへのスナップショット commit の push → Maven Central への upload (自動 release せず保留) → NuGet.org への push → Maven Central の release → 配信リポジトリへの tag の push → monorepo の tag と GitHub Release の作成、の順に行う SHALL。tag (配信リポジトリ・monorepo) と Release は、それより前のすべてのステップが成功したときにのみ作られる。

#### Scenario: 途中で失敗すれば tag は作られない
- **GIVEN** NuGet.org への push が失敗した実行
- **WHEN** publish job が終了する
- **THEN** 配信リポジトリと monorepo のどちらにも tag は存在せず、GitHub Release も作られない

#### Scenario: スナップショット commit は tag より前に push される
- **GIVEN** publish job の実行
- **WHEN** Maven Central への upload が始まる
- **THEN** 配信リポジトリには当該 version のスナップショット commit が push 済みで、tag はまだ存在しない

### Requirement: Maven Central の 2 段操作
Maven Central への発行は、upload の完了時点で deployment が検証済み (VALIDATED) かつ未公開である SHALL。upload の deployment ID は後段で参照でき、release は NuGet.org への push の後に、deployment が VALIDATED であることを再確認してから行う SHALL。deployment ID を得られなかった場合は upload の時点で失敗する。deployment ID は同じ実行の再実行から参照できる形で保存する SHALL。再実行時に前回の deployment ID があれば、upload の前にその状態を照会し、VALIDATED なら upload を skip して release へ、PUBLISHING なら PUBLISHED になるまで待って release を skip、PUBLISHED なら upload と release を skip、FAILED なら drop してから再 upload する SHALL。publish job がいずれかのステップで失敗した場合、deployment が drop 可能な状態 (VALIDATED / FAILED) であれば drop する SHALL。PUBLISHING / PUBLISHED の deployment は drop しない。

#### Scenario: upload 後は保留状態で止まる
- **GIVEN** publish job の Maven upload ステップ
- **WHEN** ステップが成功する
- **THEN** Central Portal の deployment は VALIDATED で、Maven Central には当該 version はまだ公開されていない

#### Scenario: NuGet push の後に release される
- **GIVEN** NuGet.org への push が成功した実行
- **WHEN** Maven release ステップが実行される
- **THEN** deployment は PUBLISHING / PUBLISHED に遷移し、以後 Maven Central から取得できるようになる

#### Scenario: 失敗時に保留 deployment が残らない
- **GIVEN** Maven upload の後、NuGet.org への push で失敗した実行
- **WHEN** publish job が終了する
- **THEN** Central Portal に当該 version の保留 deployment は残っていない

#### Scenario: release の応答が失われても再実行で整合する
- **GIVEN** release の要求はサーバーで受理されたが応答の前に step が失敗し、deployment が PUBLISHING のまま残った実行
- **WHEN** 同じ version で再実行する
- **THEN** 前回の deployment ID から状態を照会し、drop も再 upload もせずに PUBLISHED を待って以後のステップへ進む

### Requirement: 署名の生成確認
Maven Central への upload の前に、署名ファイル (`.asc`) が発行物の各成果物 (aar / pom / sources jar / javadoc jar / module metadata) に対して生成されていることを確認する SHALL。生成されていなければ upload せずに失敗する。

#### Scenario: 署名鍵が渡っていなければ upload しない
- **GIVEN** 署名鍵の secret が空の状態
- **WHEN** publish job の署名確認ステップが実行される
- **THEN** `.asc` が無いことを理由に失敗し、upload は実行されない

### Requirement: NuGet.org への push
NuGet.org への push は Trusted Publishing (GitHub Actions の OIDC) で得た一時的な API key を用い、長期の API key を secret として保持しない SHALL。push は binding 2 件 (`KsSettingsView.Binding.Android` / `KsSettingsView.Binding.iOS`) を先に、facade (`KsSettingsView.Maui`) を最後に行い、各パッケージの nupkg と snupkg を対で push する SHALL。同じ version が既に存在する場合は失敗とせず skip する SHALL。

#### Scenario: 3 パッケージが同じ version で公開される
- **GIVEN** publish job の NuGet push ステップ
- **WHEN** ステップが成功する
- **THEN** `KsSettingsView.Maui` / `KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android` の当該 version が NuGet.org に存在する

#### Scenario: binding の push 失敗で facade は公開されない
- **GIVEN** binding 1 件の push が成功した後、もう 1 件の push で失敗した実行
- **WHEN** publish job が終了する
- **THEN** facade `KsSettingsView.Maui` の当該 version は NuGet.org に存在せず、再実行では成功済みの binding は skip され残りが push される

#### Scenario: 既に存在する version は skip される
- **GIVEN** 前回の実行で NuGet push まで成功した後に失敗し、同じ version で再実行した
- **WHEN** NuGet push ステップが実行される
- **THEN** 重複は skip され、ステップは成功として続行する

### Requirement: tag と GitHub Release
publish の全ステップ成功後、配信リポジトリと monorepo に接頭辞なしの同じ version の tag を作り、monorepo に GitHub Release を作る SHALL。Release 本文は GitHub の自動生成ノートを用い、`.github/release.yml` の分類に従う。version に prerelease の suffix があれば Release を prerelease として作る SHALL。

#### Scenario: prerelease の suffix で prerelease になる
- **GIVEN** version `0.1.0-beta.1` の実行
- **WHEN** Release が作られる
- **THEN** Release は prerelease として印が付き、tag `0.1.0-beta.1` が monorepo と配信リポジトリの両方に存在する

#### Scenario: 正式版は prerelease にならない
- **GIVEN** version `0.1.0` の実行
- **WHEN** Release が作られる
- **THEN** Release は prerelease ではない

### Requirement: 同じ version での再実行
publish job のステップは冪等であり、失敗した実行を同じ version で「失敗した job から再実行」したとき、既に完了している publish (配信リポジトリの commit、Maven Central の公開、NuGet.org の push、tag) を重複させず skip し、未完了のステップだけを行って完了できる SHALL。monorepo の tag が別の commit を指している場合、および配信リポジトリの同名 tag の内容が今回のスナップショットと異なる場合は失敗する。

#### Scenario: 部分 publish を同じ version で埋める
- **GIVEN** NuGet push まで成功し Maven release で失敗した実行
- **WHEN** 同じ version で再実行する
- **THEN** スナップショット commit と NuGet push は skip され、Maven upload → release → tag → Release が完了する

#### Scenario: 全て完了済みの再実行は何も重複させない
- **GIVEN** publish まで成功し smoke で失敗した実行
- **WHEN** 同じ version で「失敗した job から再実行」する
- **THEN** publish job は再実行されず (または全ステップ skip)、smoke だけが再実行される

### Requirement: 反映待ちと smoke
tag と Release の作成後、Maven Central と NuGet.org (3 Package ID すべて) に当該 version が取得可能になるまで待機 (上限あり) してから、消費者検証 workflow を `mode=smoke` + version で 3 platform について呼ぶ SHALL。smoke の失敗は workflow の失敗として報告され、作成済みの tag と Release は取り消されない。

#### Scenario: 反映を待ってから smoke する
- **GIVEN** publish 直後で Maven Central にまだ当該 version が同期されていない状態
- **WHEN** 反映待ち job が実行される
- **THEN** Maven Central と NuGet.org の 3 Package ID すべてで取得可能になるまで待ってから smoke が呼ばれ、上限内に反映されなければ失敗として報告される

#### Scenario: smoke 失敗でも tag は残る
- **GIVEN** smoke のいずれかが失敗した実行
- **WHEN** workflow が終了する
- **THEN** workflow は失敗として報告されるが、tag と Release は存在したままである

### Requirement: secrets と権限の範囲
配信先への認証情報 (Central Portal の User Token、署名鍵、nuget.org のユーザー名、配信リポジトリの deploy key) は GitHub Environment `release` に置き、publish job だけが参照する SHALL。Environment `release` は `main` ブランチからの参照に限定する。他の job は secrets を受け取らず、消費者検証 workflow の呼び出しに全 secrets を引き継ぐ指定 (`secrets: inherit`) を用いない SHALL。publish job 以外の権限は読み取りに限る。

#### Scenario: publish 以外の job は書き込み手段を持たない
- **GIVEN** release workflow の定義
- **WHEN** 各 job の権限と secrets を確認する
- **THEN** `environment: release` と書き込み権限を持つのは publish job だけで、他の job は `contents: read` のみで secrets の参照を持たない

#### Scenario: main 以外から Environment は参照できない
- **GIVEN** Environment `release` の deployment branch policy
- **WHEN** `main` 以外のブランチの実行が publish job に到達しようとする
- **THEN** Environment の参照が拒否され、publish は実行されない

### Requirement: README のインストール例の version 整合
README (英語 / `README_ja`) のインストール例 (SwiftPM の `exact:`、Maven 座標、NuGet の `Version`) は最新リリースの具体 version を記載し、置換は専用 script で行う SHALL。script は 2 枚 × 3 行を同じ値に置換し、該当行を検出できなければ失敗する。同じ script の検査モードで、指定 version との一致を検査できる。

#### Scenario: 置換で 6 行が同じ値になる
- **GIVEN** README 2 枚のインストール例が旧 version の状態
- **WHEN** script に新 version を与えて実行する
- **THEN** 2 枚 × 3 行がすべて新 version になり、それ以外の行は変わらない

#### Scenario: 該当行が見つからなければ失敗する
- **GIVEN** インストール例の行の形が変わり script の検出パターンに合わない README
- **WHEN** script を実行する
- **THEN** 置換せずに失敗し、検出できなかったファイルと対象が出力される

# レビュー結果: add-consumer-verification (001 回目)

**日付**: 2026-09-02
**判定**: CHANGES_REQUESTED

## サマリー

`verification/` の 3 platform 消費者・実行スクリプト・README 一致 lint・再利用可能 workflow 3 本は、デルタスペックの Requirement をほぼ全面的に満たしている。とくに dry-run の排他性 (Android `exclusiveContent` / MAUI packageSourceMapping + 空の展開先) は手元で再実測しても期待どおりに落ち、証跡の作り方・入力検証の位置・`permissions: contents: read` と secrets 不受領による副作用の不在の示し方も spec の意図に忠実である。一方で、Android 消費者ビルドがローカルで `ANDROID_HOME` を必須としており、handbook が案内するもう一方の環境構築経路 (`local.properties`) では Scenario「引数なしで dry-run が動く」が成立しない。これを Major 1 件として差し戻す。残りは Minor 3 件・Suggestion 4 件で、いずれも構造には触れない。

## 照合した規約

handbook (作業ドメイン cross + 触る ios / android / maui の index を確認):

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` | 常時 (全ソースコード) | 適用。禁止参照・禁止記述類型は新規ファイル全件で違反なし (作業文書パス・変更識別子・ローカル通番・履歴記述・SHALL 等の混入なし)。コメントの内容妥当性で Minor 1 件 |
| `kasane/handbook/cross/public-identifiers.md` | `**/build.gradle.kts` / `**/*.csproj` を触るとき | 適用。`jp.kamusoft.kssettingsview.verification.android` / `.maui` は `samples.*` と同じ接頭辞体系から導出されており適合。Maven 座標・NuGet Package ID・SwiftPM product / identity の使い方も表と一致 |
| `kasane/handbook/cross/test-execution.md` | テストを実行するとき・結果を報告するとき | 適用 (下記「実行した検証」で件数を併記) |
| `kasane/handbook/cross/local-development-setup.md` | 環境構築の手順が要るとき | 適用 (guide)。「Android SDK ロケーション」節との齟齬が Major-1 |
| `kasane/handbook/cross/sample-parity.md` | `samples/` を触るとき | 適用外 (`samples/` に変更なし。`verification/` が対象外であることは agenda 決定と一致) |
| `kasane/handbook/cross/user-skill-api-listing.md` | `skills/` を触るとき | 適用外 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 実行時挙動の不具合調査 | 適用外 |
| `kasane/handbook/cross/aiforms-origin-reference.md` | 未移植機能の実装・移植元との差異調査 | 適用外 |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/` を触る変更の完了判定 | 適用外 (`ios/` に変更なし) |
| `kasane/handbook/maui/integration-host-verification.md` / `performance-verification.md` | binding / facade の E2E 疎通・描画性能 | 適用外 |

決定 (照合のみ、抵触なし): `cross/ADR-0018` (配信経路)、`cross/ADR-0019` (lockstep)、`cross/ADR-0020` (version 注入は Gradle `-Pversion=` / MSBuild `-p:Version=`。`android/build.gradle.kts` の受け口はこの ADR の文言どおり)、`cross/ADR-0023` (README はルート 2 枚)、`cross/ADR-0025` (platform 別 reusable workflow と両側 job 名の固定)、`cross/ADR-0026` (CI の保証範囲)、`android/ADR-0016` (単一 artifact)。

lessons: `kasane/lessons/code-review.md` の重点観点 L-001 (検出力は静的読解で争わず実測する) を Major-1 と排他性の裏取りに適用した。「指摘しないこと」は昇格済みルールなし。

## 実行した検証

| 検証 | 結果 |
|---|---|
| `android/gradlew test` (`android/build.gradle.kts` / `android/kssettingsview/build.gradle.kts` の付随修正の回帰) | BUILD SUCCESSFUL。**2700 tests / 0 failures** (debug 1350 + release 1350、`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` 集計)。新設の `plugins.withId("signing")` ブロックも構成時に評価されている |
| `verification/ios/build-consumer.sh` (引数なし = dry-run) | exit 0、`** BUILD SUCCEEDED **`。生成 `Package.swift` は path 参照 1 行、`Package.resolved` は非生成 |
| `verification/android` の消費者 release ビルド (参照先をスクラッチの Maven リポジトリに差し替え) | exit 0。`app-release-unsigned.apk` を出力 |
| 同上で参照先から `jp/` を退避して再解決 | `jp.kamusoft:kssettingsview:0.1.0-SNAPSHOT FAILED`。`~/.gradle/caches/modules-2` に直前の解決結果が残っていても素通りせず、`exclusiveContent` の排他性が warm cache 下でも成立することを確認 |
| `python3 scripts/readme-example-lint.py` / `--selftest` | いずれも exit 0 |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも exit 0 (comment-policy は 728 ファイル検査。**検出 0 件は適合の証明にしていない** — 規約本文からの読解も行った) |
| 4 workflow の YAML ロードと job 名 | `ci.yml` = ios / android / maui / consumer-ios / consumer-android / consumer-maui / lint、呼ばれる側はいずれも `verify`。status check 名 `consumer-<platform> / verify` が確定する |
| 全 shell スクリプトの `bash -n` | 7 本すべて OK |
| `verification/android` の wrapper 一式 | `gradle-wrapper.jar` は `android/` `samples/android/` と shasum 一致、`gradlew` は `android/gradlew` と diff なし、`gradle-wrapper.properties` は同一 (Gradle 9.5.0 + sha256 検証あり)。新規のバイナリ持ち込みではない |
| `git add -An verification` | 追跡対象は 33 ファイルのみ。`build/` `.gradle/` `.kotlin/` は既存 `.gitignore` で除外済み。実行ビットは `.sh` 7 本 + `gradlew` + `check-dependencies.py` に付与済みで、workflow の直接実行に耐える |

## 指摘事項

### [🟠 Major] Android 消費者検証がローカルで `ANDROID_HOME` を必須とし、handbook の `local.properties` 経路では動かない

**該当箇所**: `verification/android/build-consumer.sh:1`、`verification/android/` (build root 全体)、`kasane/handbook/cross/local-development-setup.md:55-64`

**問題点**:
`verification/android/` は 3 つめの Gradle build root だが、`local.properties` を持たず、生成もしない。`kasane/handbook/cross/local-development-setup.md` は Android SDK の解決手段として (a) `ANDROID_HOME` を export する、(b) `samples/android/local.properties` と `android/local.properties` の**2 ファイル**に `sdk.dir` を置く、の 2 経路を対等に案内しており、AGP は build root ごとに `local.properties` を独立解決すると明記している。経路 (b) を採っている環境では、`android/` の本体ビルドもテストも通るのに消費者検証だけが落ちる。

実測 (`ANDROID_HOME` / `ANDROID_SDK_ROOT` を外した状態で `verification/android` の `:app:assembleRelease`):

```
> Could not determine the dependencies of task ':app:compileReleaseJavaWithJavac'.
> SDK location not found. Define a valid SDK location with an ANDROID_HOME environment
  variable or by setting the sdk.dir path in your project's local properties file at
  '<repo>/verification/android/local.properties'.
```

`ANDROID_HOME` を与えれば同じコマンドが BUILD SUCCESSFUL になることも確認済みで、原因は SDK ロケーションの解決だけである。

これは Scenario「引数なしで dry-run が動く」(GIVEN 手元の作業環境で本体がビルドできる状態 → THEN dry-run として…ローカルの参照先から解決してビルドする) が成立しない状態にあたる。`evidence/verification-runs.txt` の実行例が `ANDROID_HOME=<Android SDK>` を前置している一方、`evidence/consumer-dry-run.txt` の同じ実行は前置なしで記録されており、証跡の側でも前提が揃っていない。失敗は自己説明的なメッセージで落ちる (静かな誤りではない) ため Critical にはしないが、「引数なしで動く」ことを Requirement に持つ変更としては塞ぐべき穴である。

**推奨修正** (いずれか):
- (a) `verification/android/prepare-feed.sh` / `build-consumer.sh` が、`ANDROID_HOME` / `ANDROID_SDK_ROOT` のいずれも未設定のときに `android/local.properties` の `sdk.dir` を読み、`verification/android/local.properties` を生成する (または同値の SDK 指定を Gradle へ渡す)。スクリプト内で閉じるため handbook の更新が要らない
- (b) `kasane/handbook/cross/local-development-setup.md` の `local.properties` 経路に `verification/android/local.properties` を 3 つめとして追加する。この場合は handbook の書き込み経路 (ksn-concept / ksn-distill) を通す必要があり、消費者検証を回す人の手作業が 1 つ増える

(a) を推す。CI は `ANDROID_HOME` が設定済みのため、いずれの場合も CI 側の変更は不要。

---

### [🟡 Minor] `verification/android/app/build.gradle.kts` のコメント 2 箇所が実挙動と一致しない

**該当箇所**: `verification/android/app/build.gradle.kts:44`、`verification/android/app/build.gradle.kts:69-70`

**問題点**:
1. `buildTypes { named("release") { ... } }` のコメントが「検証用途のため signingConfig は割り当てない（debug キーで署名）」となっているが、AGP は release に signingConfig を割り当てなければ debug キーで署名せず**未署名**の APK を出す。実測でも出力は `app-release-unsigned.apk` である。署名の有無は spec の要求 (「署名情報を要求せず」) と直結する記述なので、誤った説明は次に読む人の判断を狂わせる。
2. `implementation("androidx.activity:activity-compose:1.9.3")` のコメントが「ComponentActivity（Sample と同じ版に揃える…）」となっているが、この消費者は Activity を 1 つも持たない (`verification/android/app/src/main/AndroidManifest.xml` に activity 宣言がなく、README 最小例も `@Composable` 関数だけ)。この宣言が実際に担っているのは、コメント後半が述べている「1.12 以降が compileSdk 36 を要求するので版を固定する」ことであり、前半の理由づけは成り立たない。版が「Sample と同じ 1.9.3」である点自体は `samples/android/app/build.gradle.kts:67` と一致しており正しい。

**推奨修正**: 1 は「signingConfig を割り当てないため release APK は未署名になる (検証はビルドの成立だけを見る)」に、2 は「compose の推移依存として入る androidx.activity の版を固定する。1.12 以降は推移依存の androidx.navigationevent が compileSdk 36 を要求する」に書き直す。

---

### [🟡 Minor] mavenLocal の位置をハードコードしながら「Maven の設定に従う」と書いている

**該当箇所**: `verification/android/prepare-feed.sh:44-45`

**問題点**: コメントは「mavenLocal の位置は Maven の設定に従う。既定は `~/.m2/repository`」と述べるが、コードは `maven_local="${HOME}/.m2/repository"` と既定値を固定している。システムプロパティや `~/.m2/settings.xml` の `<localRepository>` でローカルリポジトリを移している環境では、`publishToMavenLocal` の出力先とこの検査対象がずれ、発行は成功しているのに「要求した version が発行されていません」で落ちる。落ち方は loud なので実害は限定的だが、コメントが実装の保証範囲を過大に述べている。

**推奨修正**: コメントを実装に合わせる (「既定の `~/.m2/repository` を前提にする」) か、Gradle から実際の出力先を取るか、いずれかに揃える。前者で十分と考える。

---

### [🟡 Minor] 付随修正 (署名の任意化) がリリース経路側の担保を持たない

**該当箇所**: `android/kssettingsview/build.gradle.kts:143-149`、`deviation.md:2`

**問題点**: 乖離としての記録・オーナー判断は済んでおり、その点は指摘しない。見るのは同梱条件の④「既存テストの通過と、必要なら 1 件のテスト追加で担保できる」の充足である。`setRequired(providers.gradleProperty("signingInMemoryKey").isPresent)` によって、**署名鍵が渡らない発行は release version でも未署名のまま成功する**ようになった。この分岐を検証する自動検査は本変更に無い — CI の dry-run は既定 version が `0.1.0-SNAPSHOT` で、SNAPSHOT ガードの側で署名が問題にならないため、新しい経路 (release version × 鍵なし) は CI で 1 度も踏まれない。コメントが挙げる歯止め「Central Portal が未署名を拒否する」はコードでもテストでもなく外部サービスの挙動に依存した主張である。

なお、鍵が空文字で渡った場合は `isPresent` が true になり署名は必須のまま失敗するので、無音になるのは「プロパティ名を間違える / secret を渡し忘れる」ケースに限られる。ADR-0020 の publish 順 (Maven upload が最初) により失敗は tag と NuGet push の前に来るため、影響は「リリースが途中で落ちる」までに留まる。

**推奨修正**: 本変更で塞ぐ必要はない。phase-8 の release workflow に「publish 前に署名ファイルの生成を確認する」ステップを申し送りとして残すことを推奨する (agenda の申し送り欄に 1 行)。deviation.md にこの担保の欠落を追記しておくと、蒸留時に落ちない。

---

### [🔵 Suggestion] `--work` が Android のスクリプトでは無視される

**該当箇所**: `verification/lib/verification-args.sh:56-64`、`verification/android/build-consumer.sh`、`verification/android/prepare-feed.sh`

共有の usage は 3 platform 共通で `--work <dir>` を案内するが、Android の 2 本は `KSV_WORK` を一切参照しない (作業場所は Gradle の build ディレクトリと `~/.m2`)。渡しても黙って無視されるため、外から作業場所を隔離できると誤解しうる。usage を platform ごとに出し分けるか、Android では「作業ディレクトリの指定は効かない」と 1 行添えるのが安い。

---

### [🔵 Suggestion] release variant で R8 が走らないため、app module を選んだ理由の一部が実現していない

**該当箇所**: `verification/android/app/build.gradle.kts:47-52`

agenda の決定「Android は `com.android.application` の app module 1 つ」の根拠には「R8 / dex マージは app ビルドでしか走らない」が挙げられているが、`isMinifyEnabled = false` のため実際に走るのは dex マージまでで、R8 の縮小・難読化は走らない。デルタスペックは release variant のビルド成功しか要求していないので spec 違反ではない。ライブラリの consumer ProGuard ルール不足のような欠陥はこの構成では検出できない、という限界を明示しておきたい (コメント 1 行、または今後の検討事項として agenda へ)。

---

### [🔵 Suggestion] 依存ツリー取得の `|| true` が Gradle の失敗を握り潰す

**該当箇所**: `verification/android/build-consumer.sh:53-57`

`resolved="$(... :app:dependencies ... | grep "jp.kamusoft" || true)"` は、`grep` が 1 行も拾わなかった場合だけでなく **Gradle 自体が失敗した場合も** 空文字を返し、次の `[ -z ]` 判定によって「依存ツリーに jp.kamusoft の行がありません」という原因と食い違うメッセージで終わる。`assembleRelease` の直後なので Gradle が落ちる確率は低いが、落ちたときに原因を見失う。出力を一度ファイルへ取り、Gradle の終了コードと grep の結果を分けて判定するのが素直。

---

### [🔵 Suggestion] MAUI の開発用既定 version だけがハードコードで、Android は宣言元から読んでいる

**該当箇所**: `verification/maui/prepare-feed.sh:22-24`、`verification/maui/build-consumer.sh:22`、`verification/maui/VerificationApp.csproj:31`

Android は `android/gradle/libs.versions.toml` の `kssettingsview` キーを sed で読んで単一宣言元を保っているのに対し、MAUI は `0.0.0-dev` を 3 箇所に literal で持ち、コメントだけが「本体の開発用既定値 (`maui/Directory.Build.props` の Version)」と宣言元を指している。pack も restore も同じ引数を明示的に流すので取り違えは起きないが、`maui/Directory.Build.props` の既定値が変わってもここは追随せず、コメントの主張だけが古くなる。`Directory.Build.props` から読むか、コメントを「本体の既定値と同じ値を持つ (自動追随はしない)」に弱めるかのどちらか。

## アクションプラン

1. **Major-1 を塞ぐ** — `verification/android/` のスクリプトが `ANDROID_HOME` 未設定時に `android/local.properties` の `sdk.dir` を引き継ぐようにする。修正後、`ANDROID_HOME` を外した状態で `verification/android/build-consumer.sh` が引数なしで通ることを実測して evidence に追記する
2. **Minor-1 / Minor-2 のコメント修正** — 実挙動に合わせる (3 箇所、いずれも 1〜2 行)
3. **Minor-3 の申し送り** — deviation.md に「署名任意化の自動担保なし」を追記し、phase-8 agenda に「publish 前の署名ファイル確認」を 1 行足す
4. **Suggestion 4 件** — 採否はオーナー判断。1 でスクリプトへ手を入れるなら Suggestion-3 (`|| true`) は同じ機会に直せる
5. 上記の後、tasks 5.6 / 5.7 (draft PR での 7 job・artifact 入力の確認) と 6.1 (branch protection) はオーナーの作業として残る。Requirement「マージ保護」と Scenario「artifact を与えた呼び出し」は、それらが済むまで未実証である

## 確認したが指摘しなかったこと

- **deviation.md 記録済みの 2 件** (version 注入の受け口・署名の条件化) は合意済みの差分として扱った。同梱条件との照合のみ行い、①〜③⑤ は満たしていると判断した (別能力のファイルである点はオーナー判断で同梱が決定済み)。④ の担保だけ Minor-3 として挙げた
- **iOS 最小例の main actor 分離警告** — evidence 11 節のオーナー判断どおり未対応。念のため消費者パッケージの `swift-tools-version` を 6.0 に上げた複製でも Release ビルドを試したが、error にはならず warning のまま `** BUILD SUCCEEDED **` だった。Swift 6 系の利用者でエラーに化ける懸念は無い
- **Gradle の changing module キャッシュによる SNAPSHOT の陳腐化** — `verify-consumer-android.yml` が `~/.gradle/caches/modules-2` をキャッシュするため、file リポジトリ経由の `0.1.0-SNAPSHOT` が前回実行のキャッシュで素通りしうるかを実測したが、参照先から成果物を外すと即座に解決失敗になった。false green の経路にはならない
- **`rm -rf` の使用** — 削除に `trash` を使う規律はエージェントのツール操作に対するもので、CI ランナー上で動く同梱スクリプトには当たらない。既存の `scripts/spm-snapshot/sync-snapshot.sh` も同じ書き方を採っている
- **`verification/` が `samples/` のパリティ規約の対象外であること** — agenda 決定どおりで、`kasane/handbook/cross/sample-parity.md` の適用範囲 (`samples/**`) からも読み取れる
- **workflow のスクリプトインジェクション** — `inputs.*` はすべて `env:` 経由で run へ渡っており、`${{ }}` を shell 本文へ直接展開している箇所は無い。actions は 3 種とも commit SHA 固定

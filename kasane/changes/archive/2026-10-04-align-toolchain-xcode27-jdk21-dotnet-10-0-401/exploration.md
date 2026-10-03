# Exploration: align-toolchain-xcode27-jdk21-dotnet-10-0-401

## 課題 / 動機

オーナーの開発機が新しい Mac に替わり、開発環境が次に変わった (2026-10-03 実測)。この環境でビルド・テストが回るようにプロジェクトを合わせる。

| 対象 | 新環境 | リポジトリが要求している版 |
|---|---|---|
| .NET SDK / workload | SDK 10.0.401 のみ、workload set 10.0.401.1 (android 36.1.69 / ios 27.0.10722 / maui 10.0.110) | `global.json` が SDK 10.0.300 / workload 10.0.300.3 |
| Xcode | 27.0 (27A266a) のみ、Swift 6.4、Simulator runtime は iOS 27.0 のみ | CI が `KS_XCODE_VERSION: "26.5"` を 5 か所で固定 |
| JDK | Microsoft OpenJDK 21.0.12.1 のみ (オーナーのシェル設定の `JAVA_HOME` もこの JDK を指す) | `jvmToolchain(17)` 4 か所、CI の `setup-java` temurin 17 が 6 か所 |

### いま起きていること

- リポジトリ内で `dotnet` が起動しない (`global.json` の SDK 10.0.300 が無い)
- Gradle 9.5.0 自体は JDK 21 で起動する (`./gradlew --version --offline` で実測)。一方 `jvmToolchain(17)` はローカルの JDK 17 を探し、toolchain resolver (自動取得) を入れていないため、JDK 17 が無いこの環境ではコンパイルが `No matching toolchains found` で失敗する見込み (`kasane/concepts/android/architecture/build-toolchain.md:40` の記述と JDK 17 不在からの推定。未実測)。MAUI の Android binding も同じ Gradle を呼ぶため巻き込まれる

### 外部の事実 (2026-10-03 に確認)

- workload set 10.0.401.1 は .NET for iOS 27.0.10722 を含み、Xcode 27.0 が必須 (Xcode 27.0 は macOS 26.6 以上)。MAUI は 10.0.110 以上が推奨 — https://github.com/dotnet/macios/releases/tag/dotnet-10.0.1xx-xcode27.0-10722
- GitHub Actions の `macos-26` イメージに Xcode 27.0 は無い (26.6 まで)。.NET SDK も 10.0.400 まで (SDK は `setup-dotnet` が `global.json` から導入するので支障にならない) — https://github.com/actions/runner-images/blob/main/images/macos/macos-26-arm64-Readme.md
- Xcode 27.0 は別ラベル `xcode-27` / `xcode-27-xlarge` のイメージで使える。public preview、arm64 のみ、macOS 27.0、Simulator は iOS 27.0 のみ、JDK は 11 / 17 / 21 (既定) / 25、.NET SDK 10.0.401 同梱 — https://github.blog/changelog/2026-09-10-xcode-27-runner-image-now-runs-on-macos-27/ 、https://github.com/actions/runner-images/blob/main/images/macos/xcode-27-arm64-Readme.md

### 版を固定している箇所 (調査済み。同じ調査を繰り返さないための記録)

直さないと動かない箇所:

| 箇所 | 現在の値 |
|---|---|
| `global.json:2-5` | SDK 10.0.300 / workloadVersion 10.0.300.3 (rollForward 指定なし) |
| `android/kssettingsview/build.gradle.kts:37-38,87` | `VERSION_17` / `jvmToolchain(17)` |
| `android/kssettingsview-bridge/build.gradle.kts:26-27,67` | 同上 |
| `samples/android/app/build.gradle.kts:28-29,56` | 同上 |
| `verification/android/app/build.gradle.kts:33-34,60` | 同上 |

`global.json` を上げると連動する CI:

| workflow | ランナー | Xcode | JDK | .NET |
|---|---|---|---|---|
| `.github/workflows/verify-ios.yml` | macos-26 | "26.5" | — | — |
| `.github/workflows/verify-consumer-ios.yml` | macos-26 | "26.5" | — | — |
| `.github/workflows/verify-maui.yml` | macos-26 | "26.5" | temurin 17 | `global.json` 参照 |
| `.github/workflows/verify-consumer-maui.yml` | macos-26 | "26.5" | temurin 17 | `global.json` 参照 |
| `.github/workflows/release.yml` | ubuntu-24.04 と macos-26 | "26.5" (pack job) | temurin 17 (3 job) | `global.json` 参照 (2 job) |
| `.github/workflows/verify-android.yml` / `verify-consumer-android.yml` | ubuntu-24.04 | — | temurin 17 | — |

- .NET はすべて `global.json` 参照なので、`global.json` を変えれば CI も同じ版になる。NuGet キャッシュのキーも `global.json` のハッシュ
- Xcode は `KS_XCODE_VERSION` に一致する Xcode をイメージ内から選んで `DEVELOPER_DIR` に設定する方式。Simulator は最新ランタイムの iPhone を UDID で解決しており、機種名・OS 版の名指しはリポジトリ内に無い
- 必須 status check の名前は job 名で決まる (cross/ADR-0025)。ランナーを変えても job 名は変えない

動くが記述が古くなる箇所 (約 20):

- `README.md:30-32,92` / `README_ja.md:30-32,92` — JDK 17、.NET SDK 10.0.300、`net10.0-android36.0` / `net10.0-ios26.0`
- `skills/{ja,en}/kssettingsview-maui/SKILL.md:69,75,77`、`skills/{ja,en}/kssettingsview-aiforms-migration/` (SKILL.md と `references/api-mapping.md`)、`skills/{ja,en}/kssettingsview-android/SKILL.md:64-67`
- `kasane/handbook/cross/local-development-setup.md:21-23` (Xcode 16 以上 / JDK 17)、`kasane/handbook/cross/test-execution.md:132` (どの JDK でも JDK 17 がローカルに必要)
- `kasane/concepts/android/architecture/build-toolchain.md:37-40,70,102`、`kasane/concepts/maui/api/maui-facade.md:44-50`、`kasane/concepts/maui/architecture/binding-build-integration.md:136,150-168`

版の直書きが無く、触らなくてよい箇所: `ios/Package.swift` (swift-tools-version 5.10 / iOS 16)、`ios/binding/build-xcframework.sh`、`verification/ios/build-consumer.sh`、`scripts/spm-snapshot/`、`scripts/release/`、`maui/Directory.Packages.props` の `Microsoft.Maui.Controls` 10.0.71 (下限。maui/ADR-0026)。

### 関連する既存の決定・記述

- 版の固定方針そのものを決めた ADR は無い (方針は concept と workflow のコメントにある)
- `kasane/concepts/android/architecture/build-toolchain.md` — 成果物ターゲットは Java 17 固定で、Gradle を動かす JDK の更新を理由に `jvmToolchain(17)` / `compileOptions` を変えない、と記す (「してはいけないこと」)。論点 2 と衝突し得る
- `kasane/concepts/maui/architecture/binding-build-integration.md` の「SDK 更新時に再検証する箇所」— SDK を上げる変更の検証項目 (maui/ADR-0006 が対で維持すると定めた表)
- 同文書の「API 版付き TFM」— nupkg の TFM group は SDK の既定 platform 版が付き (10.0.300 では `net10.0-android36.0` / `net10.0-ios26.0`)、それより古い API 版を固定した利用者では binding が依存に入らない
- cross/ADR-0026 — CI が保証する範囲 (ロジックの全件通過と native への配線のコンパイルまで)

### 進行中の change との関係

- `maui-android-fast-deployment-xa0126` — 原因が Emulator / API 版 / workload 版のどれかを切り分けていない。workload が変わると症状が変わり得るので、本変更の後に再現確認する順が自然。`kasane/handbook/cross/local-development-setup.md` の編集が重なる
- ロードマップ `maui-support` のゴールとは重ならない

## 検討した選択肢 (却下案と理由を含む)

### 論点 1: CI の macOS job を Xcode 27 へどこまで揃えるか

MAUI 系の job (本体検証・消費者検証・リリースの pack) は workload が Xcode 27.0 を必須とするため `xcode-27` イメージへ移す以外に無い。選択の余地があったのは iOS Native 系の 2 job。

- **採用 — 全 job を Xcode 27 に揃える**: 開発機には Xcode 27 しか無く、CI が別の Xcode で落ちると手元で再現できないため。Xcode の版とランナーが 1 種類で済む
- 却下 — MAUI 系だけ Xcode 27、iOS Native 系は `macos-26` + Xcode 26.5 のまま: Xcode 26 を使う SwiftPM 利用者向けのビルド保証を CI が持ち続けられる利点はあるが、その job の失敗を手元で再現できず、Xcode の版とランナーが 2 種類に分かれる
- 却下 — `global.json` を上げず CI を現状維持: リリースの pack が public preview のランナーに載るのを避けられるが、開発機でビルドできない

### 論点 2: JDK 17 が無い環境で Android をどうビルドするか

確認した材料: 本体と Bridge に Java のソースは無い (Kotlin のみ)。Robolectric は 4.13。Kotlin の `jvmTarget` は明示されておらず、現状は `jvmToolchain(17)` から 17 に決まっている。MAUI の Android binding が Gradle を呼ぶときの JDK は .NET Android SDK が解決する JDK 21。

- **採用 — コンパイルとテストに使う JDK を 21 に上げ、配布物は Java 17 向けのまま保つ**: 開発機・CI・MAUI 経由のビルドが JDK 21 の 1 本で回り、テストが走る JDK もどの環境でも同じになる。次に JDK が替わったときは同じ修正がまた要る
- 却下 — 使う JDK を指定しない (Gradle を動かしている JDK でコンパイルする): どの JDK でもそのままビルドでき将来の修正も要らないが、テストが走る JDK が環境ごとに変わる (Android Studio 同梱の JDK 25 で Robolectric 4.13 が通るかは未確認)
- 却下 — toolchain resolver を足して JDK 17 を自動取得する: 配布物・テスト・CI を今と同じに保てて変更も最小だが、初回ビルドで JDK 17 をダウンロードし、開発機が「JDK は 21 のみ」ではなくなる
- 却下 — 配布物ごと Java 21 向けに上げる: 利用者のビルド環境に JDK 21 以上を要求することになる

### 論点 3: 配布物の利用者要件が動くか

導入済み workload 10.0.401.1 の定義を読んで確かめた (ビルドしての実測ではない。パスは .NET の導入先からの相対):

- iOS (`packs/Microsoft.iOS.Sdk.net10.0_27.0/27.0.10722/targets/Microsoft.iOS.Sdk.Versions.props` と `sdk-manifests/10.0.100/microsoft.net.sdk.ios/27.0.10722/WorkloadManifest.targets`): .NET 10 で対応する platform 版は 26.0 と 27.0。platform 版を明示しないときの既定は、アプリが 27.0、**ライブラリが 26.0** (ライブラリは 26.0 向けの SDK pack が読み込まれる)
- Android (`packs/Microsoft.Android.Sdk.Darwin/36.1.69/targets/Microsoft.Android.Sdk.SupportedPlatforms.targets`): 対応は 36.0 と 36.1、既定は **36.0**

facade と binding はライブラリなので、nupkg の TFM group は `net10.0-ios26.0` / `net10.0-android36.0` のまま変わらない見込み。利用者要件 (`kasane/concepts/maui/api/maui-facade.md` の「導入と前提」) は動かない。

- **採用 — TFM の platform 版は明示せず SDK の既定に任せ、pack した nupkg の TFM group が変わっていないことを完了条件で確かめる**
- 不要になった案 — TFM に platform 版を明示して据え置く: 既定のままで据え置かれる見込みのため、足す理由が無い。完了条件の確認で group が動いていたら、この案と「利用者要件を上げる」案を並べてオーナーに判断を仰ぐ

`Microsoft.Maui.Controls` の下限 10.0.71 (maui/ADR-0026・maui/ADR-0010 が理由を持つ) は据え置く。リリースノートは 10.0.110 以上を推奨としているが、下限を上げると利用者に版の引き上げを強いるため、ビルドとテストが通る限り変えない。

## 決定事項

S 級のため、この節と下の「完了条件」がそのまま実装に渡すスコープになる。

### 実装するもの

- `global.json` を SDK 10.0.401 / workloadVersion 10.0.401.1 に上げる (オーナーの依頼の前提)
- Android の 4 つのビルド設定 (本体・Bridge・Sample・消費者検証) は、コンパイルとテストに使う JDK を 21 にする (`jvmToolchain(21)`)。配布物の対象は Java 17 のまま保ち、`compileOptions` の `VERSION_17` は変えず、Kotlin の `jvmTarget` を 17 に明示する
- CI の JDK 導入 (`setup-java` temurin 17 の 6 か所) は 21 にする
- CI の macOS job (iOS Native 系 2 本・MAUI 系 2 本・リリースの pack) はすべて Xcode 27.0 に揃え、ランナーを `xcode-27` イメージにする。job 名は変えない (必須 status check 名を保つ)。帰結として、Xcode 26 でのビルドは CI で確かめなくなり、検証もリリースも public preview のランナーに依存する
- 版を書いているソースコメント (例: Android のビルド設定の「JDK 17 を採用」) は、変更後の内容に合わせる

### 触らないもの

- TFM の platform 版は明示しない。`Microsoft.Maui.Controls` の下限と Sample・消費者検証の `MauiVersion` (10.0.71) は据え置く。ビルドが通らないときは版を上げる前に停止して報告する
- Xcode project の形式 (`objectVersion` / `LastUpgradeCheck`) と `swift-tools-version` (5.10) は触らない。Xcode 27 が形式の更新を勧めても、ビルドとテストが通る限り据え置く (`swift-tools-version` は利用者に見える最低版のため)
- README 2 枚と `skills/` の記述 (.NET SDK 10.0.300、ライブラリのビルドに使う JDK) は本変更では触らない。蒸留後にオーナーが docs-refresh を依頼して追従させる
- `maui-android-fast-deployment-xa0126` は本変更の後に、新しい workload で再現するかを確かめてから扱う

### 蒸留への申し送り

- 蒸留時に反映: `kasane/concepts/android/architecture/build-toolchain.md` — 「2 つの JDK の役割」を、コンパイルとテストに使う JDK (21) と配布物の対象 (Java 17) を分けた記述へ改める。「してはいけないこと」の `jvmToolchain(17)` を変えない旨は、配布物の対象 (`compileOptions` と `jvmTarget`) を変えない旨へ書き直す。MAUI binding の節の「JDK 17 の実体も別途必要」を外す
- 蒸留時に反映: `kasane/handbook/cross/local-development-setup.md` — 必要環境の JDK 17 を 21 に、「Xcode 16 以上」を現在の前提に合わせる
- 蒸留時に反映: `kasane/handbook/cross/test-execution.md` — 「どの JDK でも JDK 17 がローカルに必要」の記述 (132 行付近) を改める
- 蒸留時に反映: `kasane/concepts/maui/api/maui-facade.md` と `kasane/concepts/maui/architecture/binding-build-integration.md` — 「SDK 10.0.300 時点」「SDK (10.0.300) の既定 platform 版」の記述を 10.0.401 での確認結果へ改め、ライブラリの既定 platform 版がアプリの既定と別に決まること (iOS はライブラリ 26.0 / アプリ 27.0) を足す

### 完了条件 (実装とレビューが確かめること)

- `android/` の `./gradlew test` が全件実行で失敗 0 (件数確認は `kasane/handbook/cross/test-execution.md`)、`samples/android` の `:app:assembleDebug`、消費者検証 3 本 (`verification/<platform>/build-consumer.sh`) が手元で通る
- 生成された class のバイトコード版が Java 17 相当 (major version 61)
- iOS Native のテストが Xcode 27.0 / iOS 27.0 の Simulator で全件通る
- MAUI の binding・facade のビルドとテストが通り、`kasane/concepts/maui/architecture/binding-build-integration.md` の「SDK 更新時に再検証する箇所」の表の各項目を確かめる。pack した nupkg の TFM group が `net10.0-ios26.0` / `net10.0-android36.0` のまま
- CI の Xcode 選択が `xcode-27` イメージで成立する (現行は `KS_XCODE_VERSION` に一致する名前の Xcode をイメージ内から探す方式。`xcode-27` イメージでの Xcode の置き場所と名前は push して初めて確かめられる)。develop への push で lint + 本体検証 3 本が通る
- 消費者検証とリリースの経路は `main` 宛て PR とリリースの dry-run でしか走らないため、本変更の完了時点では未検証として申し送る

### 手元の環境 (リポジトリの変更ではない。git 管理外)

- 対応済み (2026-10-03、オーナーの指示): `android/local.properties` と `samples/android/local.properties` の `sdk.dir` が旧 Mac の実在しない場所を指していたため、その行を削除した。Android SDK の場所はオーナーのシェル設定にある `ANDROID_HOME` で解決する (`kasane/handbook/cross/local-development-setup.md` の「ANDROID_HOME を使う」の経路)。Gradle を実際に走らせての確認は、`jvmToolchain` を直すまでビルドが通らないため実装の完了条件に含める
- `sdk.dir` に新しい場所を書く方法は採れなかった。ローカル絶対パスの書き込みを止める hook が、git 管理外の `local.properties` にも掛かるため
- エージェントのコマンド実行環境と、Dock から起動した Android Studio は、シェル設定の環境変数を読まない。エージェントが Gradle を呼ぶときはコマンドに `ANDROID_HOME` を付ける。探索の初めに「`ANDROID_HOME` / `JAVA_HOME` が未設定」と見えたのはこのためで、オーナーの端末では両方とも設定済み

## ADR 候補 (作成済み: なし / 未起票: なし)

論点 1〜3 の決定はいずれも版の数値とランナーの付け替えで、後から戻せる局所的な判断のため ADR にしない。

## 未決の論点

なし

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S — オーナーが確定 (2026-10-03) (公開 API と利用者要件を変えず、挙動の仕様として書くものが無い。版の数値・ランナーの指定・`jvmTarget` の明示に閉じ、後から戻せる。ただし複数のビルドルートと CI・リリース経路に及ぶため、完了条件を上の節に明示した)

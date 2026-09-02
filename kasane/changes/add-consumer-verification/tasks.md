# Tasks: add-consumer-verification

## 1. 前提の実測 (机上確定の裏取り。覆ったら実装を進めずエスカレーションする)

- [x] 1.1 iOS: `scripts/spm-snapshot/sync-snapshot.sh` の出力を一時ディレクトリ `KsSettingsView-SPM` に置き、`.package(path:)` + `.product(name: "KsSettingsView", package: "KsSettingsView-SPM")` で解決・ビルドできること (identity がディレクトリ名から決まること) と、path 参照のみのとき `Package.resolved` が残らない (または pins が空になる) ことを確認する。同期スクリプトが git 作業コピー以外を拒む場合は、`git init` + origin 設定した一時ディレクトリで通す (→ Requirement: dry-run の参照先 / 解決結果の証跡)
- [x] 1.2 MAUI: packageSourceMapping 付き `nuget.config` を `-p:RestoreConfigFile=` で選択し、実行ごとに空の `RestorePackagesPath` を与えた restore で、`KsSettingsView.*` がローカルフィードからのみ取得され (`.nupkg.metadata` の source)、フィードに無い version はユーザーの `~/.nuget/packages` に同じ version があっても NU1101 で失敗すること、NU1507 が出ないことを確認する (→ Scenario: 本リポジトリ由来の座標はローカル参照先からのみ取得される / ローカル参照先に無ければ公開済みの版でも失敗する)
- [x] 1.3 Android: 消費者の repositories を `exclusiveContent { forRepository { mavenLocal() } filter { includeGroup("jp.kamusoft") } }` + google / mavenCentral にし、`publishToMavenLocal` した `0.1.0-SNAPSHOT` を解決できること、mavenLocal に無い version は mavenCentral を検索せず失敗すること (Gradle の `--info` で Central への問い合わせが無いこと) を確認する (→ Scenario: ローカル参照先に無ければ公開済みの版でも失敗する)
- [x] 1.4 前提が覆った場合 (identity が一致しない / mapping が効かない / exclusiveContent で漏れる) は結果を記録し、agenda の決定 (dry-run の参照先) の見直しをオーナーへ上げる

## 2. 消費者プロジェクト

- [x] 2.1 `verification/ios/`: `Package.swift` のテンプレート (dry-run: `path:` / smoke: `url:` + `exact:`) と 1 target。README の iOS 最小例を無編集でソースに置く。`platforms: iOS 16` (→ Requirement: 消費者プロジェクトの構成 / モードと version の指定)
- [x] 2.2 `verification/android/`: `settings.gradle.kts` (catalog は `android/gradle/libs.versions.toml` を共有。`jp.kamusoft` はモードに応じて mavenLocal / mavenCentral のどちらか 1 つに `exclusiveContent` で割り当て)、app module (`com.android.application`、namespace / applicationId `jp.kamusoft.kssettingsview.verification.android`、minSdk 29 / compileSdk 35、Compose 有効)。README の Android 最小例を無編集でソースに置く。version と mode は Gradle プロパティで受け、version 未指定の dry-run は `0.1.0-SNAPSHOT` (→ 同上 / dry-run の参照先)
- [x] 2.3 `verification/maui/`: `dotnet new maui` 相当のアプリ (`TargetFrameworks` は `net10.0-android;net10.0-ios`、`MauiVersion` 10.0.70、`SupportedOSPlatformVersion` android 29 / ios 16.0、`ApplicationId` `jp.kamusoft.kssettingsview.verification.maui`、`RootNamespace` は README 例の `MyApp` に合わせる)。README の XAML と `MauiProgram` を無編集で置き、`PackageReference` の Version は MSBuild プロパティ (既定 `0.0.0-dev`) から流す。`nuget.config` 2 枚 (dry-run / smoke、packageSourceMapping)。iOS のビルドは `-p:RuntimeIdentifier=iossimulator-arm64` (ランナー / 手元とも arm64。x64 環境では `iossimulator-x64` を選ぶ分岐をスクリプトに置く) で署名情報を要求しない (→ 同上 / 消費者ビルドの成立条件)
- [x] 2.4 README の最小例がコンパイルできない場合は README を同じ変更で修正し (英語のみ。`README_ja` と skills は docs-refresh 依頼へ追記)、deviation.md に記録する (→ Scenario: README の最小例がそのままビルド対象になる)

## 3. 実行スクリプトと検査

- [x] 3.1 platform ごとにフィード準備スクリプト (iOS: スナップショット配置 / Android: `publishToMavenLocal -Pversion=` / MAUI: `dotnet pack -p:Version=` × 3 csproj をフォルダフィードへ) と消費者ビルドスクリプト (mode / version / 準備済み参照先のパスを引数に取り、Release ビルド) を置く。引数なしは dry-run + platform 既定 version。mode の許可値以外と smoke で version 無しは最初に失敗させる。MAUI は実行ごとに空の `RestorePackagesPath` を使う (→ Requirement: フィード準備と消費者ビルドの分離 / 消費者ビルドの成立条件 / モードと version の指定 / dry-run の参照先)
- [x] 3.2 MAUI: `WarningsAsErrors` に NU1605 / NU1608 / NU1107 を設定し、`project.assets.json` (解決版) と `RestorePackagesPath` 配下の `.nupkg.metadata` (取得元) から facade と binding 2 件の版一致と取得元を検査する Python を消費者ビルドスクリプトから呼ぶ (→ Requirement: MAUI 消費者の依存検査 / 解決結果の証跡)
- [x] 3.3 解決結果の証跡出力: iOS は dry-run で生成した `Package.swift` と `swift package show-dependencies` (または xcodebuild の解決ログ)、smoke で `Package.resolved`。Android は `dependencies --configuration releaseRuntimeClasspath` の `jp.kamusoft` 行。MAUI は 3.2 の出力。いずれも標準出力と (CI では) job summary へ (→ Requirement: 解決結果の証跡)
- [x] 3.4 `scripts/readme-example-lint.py`: README.md の 4 コードブロック (`### iOS` の swift / `### Android` の kotlin / `### .NET MAUI` の xml と csharp) と `verification/` の対応 4 ファイルの完全一致を検査する。対応表 (見出し・fence 言語・出力先ファイル) をスクリプト冒頭に持ち、既存 lint と同じく `--selftest` を持つ (→ Requirement: README 最小例との一致)

## 4. CI

- [x] 4.1 `.github/workflows/verify-consumer-{ios,android,maui}.yml` (`workflow_call`、inputs: `mode` (string, required) / `version` (string, 既定 空) / `artifact` (string, 既定 空)。job 名は `verify`。`mode` の検証と `smoke` の version 必須は最初のステップで行う。`artifact` があれば download して参照先にし、無ければフィード準備を job 内で行う。ランナー・Xcode・JDK・.NET の固定は既存 workflow と同じ方針、`permissions: contents: read`、secrets は受け取らない) (→ Requirement: 消費者検証 workflow の再利用契約)
- [x] 4.2 `ci.yml` に job `consumer-ios` / `consumer-android` / `consumer-maui` を追加し (`mode: dry-run`、version / artifact は空)、status check 名を `consumer-<platform> / verify` に固定する。lint job に 3.4 を追加する (→ Requirement: CI の起動条件 / lint の検証)
- [x] 4.3 `kasane/config.yaml`: `lint.identity.scope` に `verification` を追加する (→ Requirement: lint の検証)

## 5. 検証 (Scenario の実機確認)

- [x] 5.1 手元で 3 platform の dry-run を引数なしで通し、取得元がローカル参照先であることを証跡に残す。副作用: 実行前後で配信リポジトリの tag 一覧・Central Portal の deployments・nuget.org の `KsSettingsView.*` 一覧が同一であることを比較する。MAUI Android の XA4301 が出ても成功で終わることも同じ実行で確認する (→ Scenario: 引数なしで dry-run が動く / 本リポジトリ由来の座標はローカル参照先からのみ取得される / 配信先へ副作用を残さない / ビルド警告は失敗にしない)
- [x] 5.2 負ケース: (a) ローカル参照先に無い version で 3 platform とも失敗し、Android は Central を検索しない・MAUI はユーザーキャッシュに同 version を置いても失敗する、(b) MAUI で binding だけ別 version のフィードにして版一致検査が失敗する、(c) README 一致 lint が片側変更で失敗する、(d) mode の不正値と smoke の version 省略が早期に失敗する (→ Scenario: ローカル参照先に無ければ公開済みの版でも失敗する / binding の version 不一致を検出する / 例の変更が消費者に追随していなければ失敗する / 不正な入力は早期に失敗する)
- [x] 5.3 version `0.1.0-rc.1` のような値を与えて 3 platform に同じ文字列が流れること (iOS はテンプレート生成結果、Android は解決座標、MAUI は assets.json) を確認する (→ Scenario: version を与えると全 platform に同じ文字列が流れる)
- [x] 5.4 smoke モードで参照先の設定が公開レジストリを指し (iOS の URL + version、Android の mavenCentral への exclusiveContent、MAUI の nuget.org への mapping)、ローカル参照先を含まないことを生成物で確認する。公開レジストリからの解決成功 (Scenario「公開レジストリからの解決」) は配布物が未公開のため本変更では実証せず、phase-8 の初回リリースで行う旨を evidence に明記する (→ Scenario: 参照先が公開レジストリを指す)
- [x] 5.5 API 版付き TFM の解決要件: MAUI 消費者の `TargetPlatformVersion` を古い値 (例: android 35.0 / ios 18.0) に固定した restore で解決可否を実測し、結果を evidence と agenda の docs-refresh 依頼内容に追記する (→ agenda TODO「API 版付き TFM の解決要件」)
- [ ] 5.6 draft PR で 7 job が起動・成功すること、消費者検証 3 job の所要時間、job summary に解決結果が出ること、消費者 job の `permissions` が read だけで secrets を受け取っていないことを確認する (→ Scenario: PR で全 job が起動する / 解決版と取得元が読める / 消費者検証は dry-run で動く)
- [ ] 5.7 フィード準備を実行せず、5.1 で準備済みの参照先パスだけを渡して消費者ビルドを実行し、準備段が再実行されずに同じ結果になることを手元で確認する。CI の `artifact` 入力は、draft PR 上で一時的な呼び出し (upload → `artifact` 指定で download) を 1 回通して確認する (→ Scenario: 外部で準備した配布物を消費者に渡す / artifact を与えた呼び出し)

## 6. branch protection (GitHub 設定操作)

- [ ] 6.1 `develop` の必須 status check に `consumer-ios / verify` / `consumer-android / verify` / `consumer-maui / verify` を追加する (`gh api -X PUT` は全体置換のため既存 4 job を含む完全な payload を送る。実例は phase-3 の evidence)。設定後に再取得して 7 job を確認する。`main` は未作成のため phase-8 の申し送りどおり作成時に設定する (→ Requirement: マージ保護)

## 備考

- workflow の実装は github-workflow-skill、MAUI は csharp-impl-skill / maui-skill を参照する
- ビルド警告 (XA4301) はエラーにしない (proposal Non-Goals)。NU1507 は mapping で出ない見込みだが、出た場合は phase-8 の申し送りに実測を追記する
- release workflow からの呼び出し・smoke の正ケースの実証・`main` の保護は phase-8 (proposal Non-Goals)

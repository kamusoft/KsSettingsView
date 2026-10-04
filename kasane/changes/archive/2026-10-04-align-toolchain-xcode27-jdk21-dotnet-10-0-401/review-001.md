# レビュー結果: align-toolchain-xcode27-jdk21-dotnet-10-0-401 (001 回目)

**日付**: 2026-10-04
**判定**: CHANGES_REQUESTED

## サマリー

対象 12 ファイルの diff は合意スコープ (exploration.md の決定事項) に収まっており、版の直書きの取りこぼし・job 名の変更・配布物の Java 互換の崩れ・コメント規約違反は見つからなかった。一方、完了条件の「iOS Native のテストが Xcode 27.0 / iOS 27.0 の Simulator で全件通る」が満たされていない (1 件が失敗)。ksn-review の規律 (テスト失敗は見逃さない) により CHANGES_REQUESTED とする。失敗の解消はレビュー対象の 12 ファイルの外 (`ios/`) にあり、スコープと記録の扱いを決める必要がある。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・テスト結果を報告するとき)
- `kasane/handbook/cross/public-identifiers.md` (`**/build.gradle.kts` を触るとき) — 識別子・配布座標の変更が無いことの確認のみ
- `kasane/decisions/cross/0025-verification-ci-reusable-platform-workflows.md` (status check 名は呼び出し側と呼ばれる側の job 名で決まる)
- `kasane/lessons/code-review.md` (L-001。今回は該当する争点なし)

ロードしたスキル: ksn-review、kotlin-impl-skill、github-workflow-skill

## 実行した検証 (2026-10-04、手元: Xcode 27.0 (27A266a) / JDK 21.0.12.1 / .NET SDK 10.0.401)

| 対象 | コマンド | 結果 |
|---|---|---|
| Android | `android/` で `./gradlew test --rerun-tasks` | 3174 tests / 0 failures / 0 errors (debug + release) |
| バイトコード版 | 本体・Bridge の `build/` 配下の main class 1052 個、Sample の Kotlin class 57 個 | すべて major version 61 (Java 17) |
| 公開メタデータ | `:kssettingsview:generateMetadataFileForMavenPublication` | `module.json` に `org.gradle.jvm.version` 属性は現れない (利用者へ JDK 21 を要求しない) |
| Android Sample | `samples/android` で `:app:assembleDebug` | 成功 |
| MAUI facade | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj -c Release` | 624 tests / 0 failures |
| MAUI Android binding | `dotnet build` (Release) | 成功 (差分なしの再ビルド。JDK 21 経路の初回ビルドは再実測していない) |
| iOS | `ios/` で `xcodebuild test -scheme KsSettingsView` (iPhone 18 Pro / iOS 27.0) | **1206 tests / 3 failures (失敗テスト 1 件)**。バンドル別: UITests 791 (3 failures) / TestSupportTests 7 / SwiftUITests 131 / CoreTests 88 / BridgeTests 189 |

未実行: 消費者検証 3 本 (`verification/<platform>/build-consumer.sh`)、MAUI の iOS binding ビルドと pack、nupkg の TFM group の確認、CI 上での Xcode 選択 (push して初めて確かめられる項目)。

## 確認した観点

- 仕様充足 (合意スコープ): 決定事項「実装するもの」5 項目と diff の対応 — 指摘 1 を除き充足
- 触らないものの遵守: TFM の platform 版・`Microsoft.Maui.Controls` 下限・`MauiVersion`・Xcode project 形式・`swift-tools-version`・README・`skills/` に diff なし
- 版の直書きの取りこぼし: 追跡ファイル (kasane / openspec / skills / README を除く) を `26.5` `macos-26` `jvmToolchain` `java-version` `10.0.300` `JDK 17` 等で検索。残存なし。`samples/ios/KsSettingsViewSample/SampleTheme.swift:74` と `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleTheme.kt:95` の「iOS 26.5 シミュレータ」は色の実測環境の記録で、版の固定ではない
- deviation.md の記録: JDK 導入 6 → 7 か所は記録済みで、実数 (release.yml 3 + 検証 workflow 4) と一致
- job 名・必須 status check 名: 7 workflow とも `name:` 行に diff なし。`.github/workflows/ci.yml` (呼び出し側) は無変更
- 配布物の Java 互換: `compileOptions` の `VERSION_17` は 4 ファイルとも不変、`jvmTarget` を `JVM_17` に明示。上の表のとおりバイトコードと公開メタデータで実測
- Kotlin DSL: `kotlin { compilerOptions { jvmTarget } }` は Kotlin 2.4.10 の現行 API。AGP の Java 側 (17) と Kotlin 側 (17) が一致し、JVM target の不一致検査に掛からない
- コメント規約: 追加・変更されたコメントは現在形の説明で、作業文書・通番・履歴記述への依存なし。旧「JDK 17 を採用」は内容に合わせて置き換え済み。`scripts/comment-policy-lint.py` は禁止 0 件
- workflow の Xcode 選択: 下の「所見」を参照
- 成果物のローカル絶対パス: `scripts/local-path-lint.py` 通過
- オーバーエンジニアリング: なし (版の数値・ランナー指定・`jvmTarget` の明示に閉じている)

### 所見 (指摘ではない): `xcode-27` イメージでの Xcode 選択

`ls -d "/Applications/Xcode_${KS_XCODE_VERSION}"*.app` は `Xcode_27.0` 前方一致で探す。actions/runner-images の `xcode-27-arm64-Readme.md` (2026-10-04 に参照。取得は要約経由) によると、Xcode 27.0 の実体は `Xcode_27.app` で、`Xcode_27.0.app` と `Xcode_27.0.0.app` がシンボリックリンクとして存在する。したがって選択は成立する見込み。

- 同イメージには 27.1 と 27.2 beta も載るが、`Xcode_27.0` 前方一致には掛からない
- 実体名 `Xcode_27.app` は前方一致に掛からないため、選択結果は必ずリンク経由のパスになる。`verify-consumer-maui.yml:97` は `pwd -P` で実体へ解決済み。他の 4 か所はリンクのまま `DEVELOPER_DIR` に渡すが、これは従来の `macos-26` でも同じ形
- Simulator は iOS 27.0 のみで iPhone が複数載っており、`verify-ios.yml` の「最新ランタイムの iPhone を UDID で選ぶ」処理は成立する
- JDK は `setup-java` が temurin 21 を導入するため、イメージ同梱の JDK には依存しない

最終確認は完了条件どおり push 後の実行結果で行う。

## 指摘事項

### [🟠 Major] iOS 27.0 の Simulator で iOS テストが 1 件失敗し、完了条件を満たしていない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1536` (`test_DatePickerCell_ホイールのDoneは入力面の非表示完了を待って閉じ切りcallbackを届ける`)

**問題点**: 完了条件「iOS Native のテストが Xcode 27.0 / iOS 27.0 の Simulator で全件通る」に対し、全件実行で 1206 tests / 3 failures (上記 1 テストの 3 アサーション)。失敗内容は「条件が deadline 内に成立しなかった: 閉じ切り callback が非表示完了の報告で届く / 経過 0.506 秒 (deadline 0.500 秒) / 実測: events = ["changed(2025-6-14)"]」で、キーボード非表示完了の通知がテストランナーに届いていない形。CI をこのまま `xcode-27` へ移すと `verify-ios` (必須 status check) が赤になる。

補足:

- レビュー中、対象 12 ファイルの外で `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift` と `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift` に未コミットの変更が現れた (レビュー開始時の作業ツリーには無かった)。別の作業がこの失敗に対処中と見受けられるが、コンテキストパッケージにも deviation.md にも含まれていないため、レビュー対象にも合意済みの差分にもなっていない。上の失敗は、失敗行 (1568 行の `awaitCondition`) から変更前のテストで実行されたものと判断できる
- レビュー中に追加された `evidence/ios27-keyboard-hide-notification.txt` は、iOS 27.0 のテストランナー (ホストアプリなし) では入力面の通知が 1 件も届かず、実アプリでは Done から約 0.43 秒で didHide が届くことを記録している。これに従えば原因は製品挙動ではなくテストの待機条件の側にある。ただしこの切り分けと修正方針は deviation.md に書かれておらず、修正後のテストが配線の外れを検出できるか (cross/ADR-0027 と `kasane/lessons/code-review.md` L-001 のミューテーション実測) も本レビューでは確認していない
- deviation.md の蒸留送りに「iOS 1201 件」とあるが、今回の全件実行はバンドル集計の合算で 1206 件だった。失敗の記載も無い

**推奨修正**:

1. 失敗を解消する。evidence の切り分け (テストランナーに通知が届かない) に沿ってテスト側を直すなら、直した後のテストが「非表示完了の報告を拾う配線」を外したときに落ちることをミューテーションで実測する
2. `ios/` の変更を本 change に入れるなら、deviation.md に記録する。`ios/Sources/` に触れる場合は付随修正の同梱条件 (本務と同じ能力内・局所的・ユーザー判断を要しない) に当たるかをオーナーに諮る。別 change に切り出すなら、本 change の完了条件との関係 (この 1 件を未達として申し送るか) を deviation.md に残す
3. 修正後に絞り込みなしの全件実行で `N tests / 0 failures` を確認し、deviation.md の iOS 件数を実測値に合わせる
4. `ios/` の変更を含めた形で再レビューに出す

### [🟡 Minor] 完了条件のうち未確認の項目が deviation.md / 報告から読み取れない

**該当箇所**: `deviation.md`

**問題点**: 完了条件は消費者検証 3 本・「SDK 更新時に再検証する箇所」の表の各項目・nupkg の TFM group の確認を求めている。deviation.md には TFM group を pack して実測した旨の蒸留送りがあるが、消費者検証 3 本と再検証表の結果はレビューに渡された成果物から確認できない (本レビューでも再実行していない)。

**推奨修正**: 実施済みなら結果 (成功・件数) を `evidence/` か実装報告に残し、再レビュー時のパッケージで示す。優先度は低い。

## アクションプラン

1. iOS の失敗テスト 1 件を解消し、`ios/` の変更の扱い (同梱 / 別 change) を決めて deviation.md に記録する
2. iOS の全件実行で失敗 0 を確認し、deviation.md の件数を合わせる
3. 完了条件の残り (消費者検証 3 本・再検証表) の実施結果を示す
4. 再レビュー (12 ファイル側は今回の内容のままなら追加の指摘なし)

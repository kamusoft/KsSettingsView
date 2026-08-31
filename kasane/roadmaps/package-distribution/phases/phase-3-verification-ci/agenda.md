# phase-3-verification-ci

PR / push で iOS・Android・MAUI のビルドとテストを回す検証 CI (GitHub Actions) を新設する。release workflow (phase-8) はここで定義する job を再利用する。

## 論点

(すべて決定事項へ移動済み)
## 決定事項

- **workflow の構成 (2026-08-31)**: platform 別の reusable workflow (`workflow_call`) 3 本 + それを呼ぶ CI 入口 workflow 1 本の構成とする。paths フィルタは使わず、PR ごとに 3 platform すべてを検証する (必須チェックとの相性を優先。ランナーは public リポジトリで無料)。release workflow (phase-8) は同じ reusable workflow を呼んで job を再利用する。phase-3 の workflow はテスト・lint 系の検証のみを担い、ビルド・デプロイ (release) は phase-8 の守備範囲
- **トリガー (2026-08-31)**: `develop` / `main` への pull_request 時と、`develop` への push (マージ) 時に実行する。push 実行はマージ結果 (semantic conflict) の事後検知と、develop ブランチ上の実行結果 (CI バッジ・健全性確認) のため。main への直 push はしない前提で main の push トリガーは持たない。リリース (phase-8) は cross/ADR-0020 どおり手動起動 (workflow_dispatch) のままとし、マージ時デプロイは採らない
- **iOS テストの実行形 (2026-08-31)**: CI でも Simulator 実行 (`xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,...'`) で全件を回す。`swift test` は `#if canImport(UIKit)` ガードにより全体の一部しか実行されず (handbook/cross/test-execution.md)、CI の成否判定には使わない。GitHub macOS ランナーは Simulator ランタイム同梱のため追加セットアップ不要。2 段構成 (swift test 先行) は冗長として不採用
- **iOS job のランナーと Xcode 固定 (2026-08-31)**: ランナーイメージは `macos-latest` ではなく版指定の `macos-26` を使い、Xcode は workflow 内の変数でメジャー.マイナー (26.5) を明示選択する (パッチはイメージ同梱の最新に任せる)。Xcode の更新は変数変更の PR として意図的に行い、無変更の CI が突然壊れない再現性を確保する。Android job は ubuntu-latest 系 (JDK 構成は別論点)。イメージ内の正確な Xcode 指定名は実装時に runner image のマニフェストで確認する
- **MAUI の検証範囲とランナー (2026-08-31)**: MAUI job は facade のユニットテスト (`dotnet test`、net10.0) に加えて、facade の platform TFM (net10.0-ios / net10.0-android) と binding 2 本 (KsSettingsView.Binding.iOS / .Android) のビルドを通す。検証ホスト (IntegrationHost / MauiHost) の実行 (E2E) は CI に載せず、handbook/maui/integration-host-verification.md の手元手順のままとする — CI の緑 = 「ロジック全件通過 + native への配線がコンパイルできる」。iOS binding のビルドがあるため MAUI job は macOS ランナー (iOS job と同じ macos-26) に集約する
- **Android テストの実行件数担保 (2026-08-31)**: キャッシュは依存関係 (Gradle ユーザーホーム側) のみとし、`build/` (ビルド出力・テスト結果) はキャッシュしない — フレッシュランナー上で test タスクの up-to-date スキップは構造的に起きない。加えて実行件数の検査を job の成否判定に入れる: `*/build/test-results/*/TEST-*.xml` の `tests` 属性を集計し、モジュール×variant 単位で 0 件があれば fail、合計件数を job summary に表示する (handbook「実行件数の確認までが検証」の CI 化)。`--rerun-tasks` の常時付与は採らない (キャッシュ構成を依存のみに絞れば不要で、意図の失われやすいフラグを残さない)
- **JDK の供給 (2026-08-31)**: Android job・MAUI job とも `setup-java` で Temurin 17 を明示セットアップし、Gradle 実行 JVM (daemon) と toolchain を 17 に統一する。toolchain resolver plugin は追加しない (CI のためにリポジトリ側のビルド定義へ手を入れない。ローカルの「JDK 17 を入れておく」契約 = concepts/android/architecture/build-toolchain.md もそのまま)。MAUI job は同じ JDK 17 を dotnet の JavaSdkDirectory に渡して binding ビルドの `android/gradlew` 呼び出しに使う。なお `jvmToolchain(17)` は成果物 (AAR) の互換性を広く取る配布ライブラリの契約であり、CI の JVM 版は成果物に影響しない — 上げるなら別 change の判断
- **キャッシュと MAUI workload (2026-08-31)**: キャッシュは「依存のみ」で統一する — Gradle は依存キャッシュのみ (実行件数担保の決定と同根)、NuGet は `~/.nuget/packages` をキャッシュ、SwiftPM は外部依存ゼロ (`ios/Package.swift` の `dependencies: []`) のためキャッシュ不要。MAUI workload は毎回インストールとし、workload set version を `global.json` の `workloadVersion` で固定して SDK 版 (10.0.300) と一元管理する。SDK ディレクトリ丸ごとのキャッシュは、数 GB の保存・復元と SDK 更新時の不整合という壊れやすさに対し稼げる時間が見合わないため初手では採らない — workload install の実測が許容できない長さなら別 change で最適化を検討する
- **lint 群の CI 搭載 (2026-08-31)**: CI 入口 workflow に 4 番目の job「lint」(ubuntu) を追加し、`gitleaks` (secret scan)・`scripts/local-path-lint.py`・`scripts/identity-lint.py`・`scripts/comment-policy-lint.py` を実行する (phase-2 申し送りの公開前提検査 + コメント規約)。comment-policy-lint も CI に載せるのは、ローカル hook はこの端末のエージェント作業にしか効かず、公開後は他環境・他者の PR にも規約を保証する必要があるため。あわせて `kasane/config.yaml` の `lint.identity.scope` に `samples` を追加する — samples 配下 130 ファイルへの試験実行で誤検出ゼロを確認済み (2026-08-31)。これにより Xcode の実機ビルドが DEVELOPMENT_TEAM を書き戻すケース (書き込み hook では止められない) を CI が捕捉する。今後 samples に正当な UUID 定数を書く場合は `lint.identity.allow` へ登録する
- **必須チェック化と通知 (2026-08-31)**: `develop`・`main` の branch protection に 4 job (ios / android / maui / lint) すべてを必須 status check として追加し、PR 必須化もセットで設定する (直 push を許すと必須チェックが素通りできるため。既存の force-push 禁止 + 削除禁止は維持)。admin バイパスは残し、緊急時の逃げ道とする。失敗時の通知は GitHub 標準 (メール・アプリ) に任せ、Slack 等の追加通知基盤は作らない — 困ってから足す

## TODO

- [x] 論点の解消 (2026-08-31: 全 9 論点を決定事項へ昇格)
- [ ] ksn-propose で変更提案を起こす

## 実装結果 (2026-08-31 反映)

変更: [changes/archive/2026-08-31-add-verification-ci](../../../../changes/archive/2026-08-31-add-verification-ci/proposal.md)。決定事項どおりに実装し、GitHub Actions 上で 4 job すべての成功を確認した (`develop` への push 実行を含む)。

- 所要時間の実測: lint 10 秒 / android 5 分台 / ios 6〜7 分台 / maui 5〜8 分台。「許容できない長さなら別 change で最適化」の判断は不要と結論した
- 実行件数: iOS 642 件 / Android 2700 件 (4 module × debug・release の 8 組) / MAUI 516 件
- ADR 化: 決定事項のうち、後続の設計を制約する 2 つを ADR に昇格した — [cross/ADR-0025](../../../../decisions/cross/0025-verification-ci-reusable-platform-workflows.md) (再利用可能 workflow の構成)、[cross/ADR-0026](../../../../decisions/cross/0026-ci-guarantee-logic-and-wiring-not-e2e.md) (CI の保証範囲)。残る決定はいずれも workflow を書き換えれば済む可逆で局所的な判断のため ADR にしていない
- 決定事項からの差分は 4 件を [deviation.md](../../../../changes/archive/2026-08-31-add-verification-ci/deviation.md) に記録した

### 申し送り

- **`main` の branch protection**: 決定事項「必須チェック化と通知」は `develop`・`main` 両方への設定を定めたが、`main` ブランチが存在しないため `develop` のみに設定した。`main` を作成するフェーズが、作成と同時に同じ保護 (4 job 必須 status check + PR 必須、force-push 禁止・削除禁止、admin バイパス許容) を設定する → [phase-8-release-workflow](../phase-8-release-workflow/agenda.md) の TODO へ追記済み
- **iOS テストの flaky**: 検証 CI を必須 status check にしたことで、固定時間待機に依存する iOS テストが不定期に PR をブロックする状態になった。修正は [changes/fix-ios-test-pump-condition-wait](../../../../changes/fix-ios-test-pump-condition-wait/exploration.md) として起票済み (本ロードマップの外で扱う)
- **`handbook/cross/test-execution.md` の構造 lint 違反 5 件**: 待機規約の platform 共通化で同ファイルを触ったが、違反はいずれも Android 節の既存記述にあり本フェーズの変更が原因ではない (件数は 5 → 5 で増減なし)。解消は本フェーズでは見送る (見送り判断)

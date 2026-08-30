# phase-3-verification-ci

PR / push で iOS・Android・MAUI のビルドとテストを回す検証 CI (GitHub Actions) を新設する。release workflow (phase-8) はここで定義する job を再利用する。

## 論点

- workflow の分割 (platform 別 3 本か、1 本に job を並べるか) とトリガー (PR / develop への push / paths フィルタ)
- ランナー: iOS / MAUI は macOS、Android は ubuntu。macOS ランナーの Xcode バージョン固定方法
- キャッシュ (Gradle / SwiftPM / NuGet / .NET workload) と MAUI workload install の所要時間対策
- iOS テストの実行形 (`swift test` は macOS ホスト、UI 系はシミュレータが要る — concepts/cross/conventions/test-execution.md の制約をどこまで CI で担保するか)
- MAUI の検証範囲 (facade のユニットテスト net10.0 / binding のビルド / 検証ホストの扱い)
- 必須チェック化 (branch protection との連携) と失敗時の通知
- comment-policy-lint (scripts/) を CI に載せるか
- Android テストのキャッシュと実行件数の担保 (fix-compose-dsl-double-update-flaky-test からの申し送り、2026-08-22): Gradle は up-to-date な test タスクをスキップするため、キャッシュを効かせた構成では**テストが 1 件も走らないまま BUILD SUCCESSFUL** になり得る (`concepts/cross/conventions/test-execution.md`)。test タスクをキャッシュ対象から外すか `--rerun-tasks` 相当を強制するかを決め、実行件数 (`*/build/test-results/*/TEST-*.xml` の `tests` 属性合計) を job の成否判定に含めるかも併せて決める。なおフレッシュランナーは常に全件実行になるため、待機不備由来の flaky はローカルより CI で顕在化しやすい
- Android ランナーの JDK (phase-1 からの申し送り、2026-08-21): Gradle JVM は JDK 17〜25 のいずれでもよいが、各 module の `jvmToolchain(17)` は toolchain resolver plugin 無しでローカル JDK 17 を要求する — ランナーに JDK 17 を同梱する (`setup-java` 複数版) か resolver を追加するか。MAUI job の binding ビルドも `android/gradlew` を dotnet の JavaSdkDirectory で呼ぶ (concepts `android/architecture/build-toolchain.md`)

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす

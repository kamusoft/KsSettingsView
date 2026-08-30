# 検証結果: upgrade-android-build-toolchain (002 回目)

**日付**: 2026-08-21
**判定**: VALID (❌ 0 件 / ⚠️ 1 件 — いずれも deviation 記録済み)

デルタスペック: `kasane/changes/upgrade-android-build-toolchain/specs/android-build-toolchain/spec.md`
deviation.md: あり (4 件)
前回: `verify-001.md` (INVALID — ❌ 1 件: R1 本文の「sync 成功」が tasks 4.4 未実施で未検証)

## 前回からの差分

実装ファイルの mtime はすべて 17:46:50 以前で、verify-001 (17:54 出力) の時点から**実装は 1 バイトも変わっていない** (`git diff HEAD` の対象ファイル群も同一)。今回追加されたのは以下のみ:

- `tasks.md` 4.4 のチェック (18:03)
- `evidence/10-studio-sync-jbr25.jpg` / `evidence/11-studio-sync-daemon-jvm.txt` (18:04)

したがって本回の主眼は **R1 の sync 節 (前回の唯一の ❌) の証跡検証**。他の Requirement / Scenario も本回で実測・再 grep して追認した。

## 対応表

| Requirement / Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| **R1: JDK 25 の Gradle JVM でビルドできる** (本文「sync・ビルドが成功しなければならない」) | `android/gradle/wrapper/gradle-wrapper.properties:3` / `samples/android/gradle/wrapper/gradle-wrapper.properties:3` (Gradle 9.5.0)、`android/gradle/libs.versions.toml:10,15` (AGP 8.13.2 / Kotlin 2.4.10) | **sync**: `evidence/10-studio-sync-jbr25.jpg` — Android Studio の Build ツールウィンドウ Sync タブに緑チェックで「android: finished At 2026/08/21 18:02、2 min 5 sec, 399 ms」。エラー・警告ノードなし (子ノードは `Download info` と情報アイコンの `New Minor Gradle Version Available` のみ)。プロジェクトツリーに `android` と `app` の両モジュールが並び、composite build 側 (`android/`) も sync スコープに入っていることを確認。**JVM**: `evidence/11-studio-sync-daemon-jvm.txt` の pid 57202 が `/Applications/Android Studio.app/Contents/jbr/…/bin/java`。検証側で独立に追認 — `samples/android/.idea/gradle.xml:23` = `gradleJvm=#GRADLE_LOCAL_JAVA_HOME`、`samples/android/.gradle/config.properties` = `java.home=<JBR>`、当該 JBR の `java -version` = **openjdk 25.0.2**、pid 57202 の etime から起動時刻が 18:00:43 頃と算出でき 18:00〜18:02 の sync 実行窓と整合。**ビルド**は下記 2 Scenario | ✅ 一致 |
| S1.1: JDK 25 での CLI ビルド | 同上 | 検証側で再実測: `android/` `JAVA_HOME=<Studio JBR 25.0.2> ./gradlew test --no-daemon --rerun-tasks` → **BUILD SUCCESSFUL in 4m 46s / 230 actionable tasks: 230 executed**。`samples/android/` `:app:assembleDebug` は verify-001 で実測成功済み (実装不変) | ✅ 一致 |
| S1.2: JDK 21 での後方互換 | 同上 | verify-001 で実測済み (`android/` `./gradlew testDebugUnitTest`、JAVA_HOME = microsoft-21.jdk 21.0.10 → BUILD SUCCESSFUL)。実装は当時から不変のため再実行せず | ✅ 一致 |
| **R2: ビルド関連バージョンの宣言がカタログに一元化される** | `android/gradle/libs.versions.toml` (新設、`[versions]` 10/15/18/22・`[plugins]` 25-28・`[libraries]` 31) | 下記 2 Scenario | ✅ 一致 |
| S2.1: バージョン直書きの不在 | 4 module: `core:14,16,18,22,72` / `ui:9,11,14,18,102` / `compose:11,12,13,17,65` / `bridge:9,11,13,17,77`、`samples/android/app/build.gradle.kts:9,10,11,64,96` | 検証側で再 grep: `plugins` 内 `version "…"` = **0 件**、`compose-bom:` リテラル = **0 件**、`^version` の直書き = **0 件** (4 module とも `libs.versions.ks.settingsview.get()`)、旧値 `8.7.3` / `2.0.21` / `8.10.2` の残存 = **0 件** (`*.kts` / `*.toml` / `*.properties` 全走査) | ✅ 一致 |
| S2.2: samples からのカタログ共有 | `samples/android/settings.gradle.kts:24-30` (`versionCatalogs { create("libs") { from(files("../../android/gradle/libs.versions.toml")) } }`) | verify-001 で実測済み (`:app:assembleDebug` 成功、`:app:buildEnvironment` で AGP 8.13.2 / KGP 2.4.10、`:app:dependencies` で compose-bom 2024.10.01 = 本体と同一)。加えて本回の Studio sync が同 composite 構成で成功している | ✅ 一致 |
| **R3: ツールチェーン更新後も既存の品質ゲートを全て通過する** | 公開 API・実行時挙動の変更なし。`compileSdk = 35` / `minSdk = 29` / `JavaVersion.VERSION_17` / `jvmToolchain(17)` は 4 module とも diff に含まれず不変 | 下記 3 Scenario | ✅ 一致 |
| S3.1: 既存ユニットテストの全数通過 | ツールチェーン更新が強制した機械的修正は `CustomCellTest.kt:165` の 1 行のみ | 検証側で全件再実行 (`--rerun-tasks`、XML 194 件すべて本回生成のものと確認)。**debug 1261 tests / failures 0 / errors 0**、**release 1261 tests / failures 0 / errors 0** (core 80 / ui 909 / compose 111 / bridge 161 × 2 variant) | ✅ 一致 |
| S3.2: サンプルアプリの実機動作 | — | `evidence/01〜08*.png` (Pixel 6a、17:26-17:29) と `evidence/09-logcat-app.txt`。検証側で logcat を再確認 — `E AndroidRuntime` **0 件**、`FATAL EXCEPTION` はヘッダ行の記述 2 箇所のみでログ本体には 0 件 | ✅ 一致 |
| S3.3: MAUI binding からの native ビルド (THEN「**4 module** の release aar が生成され」) | `maui/android/…/KsSettingsView.Binding.Android.csproj:105` の Exec は `:ks-settingsview-core` / `:ks-settingsview-ui` / `:ks-settingsview-bridge` の **3 module** のみ (compose を含まない) | verify-001 で `dotnet build` 実測 → 0 エラー、`ks-settingsview-{core,ui,bridge}-release.aar` の 3 件生成。「4 module」は spec 側の誤記 | ⚠️ deviation 記録済み (オーナー確認待ち) |

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md の完了状況 | **15 項目すべて完了**。前回未チェックだった 4.4 が今回チェックされ、対応する証跡 (`evidence/10` / `11`) が実在し内容も整合 |
| 虚偽チェック | なし。チェック済みタスクはすべて検証側で再現または証跡確認できた (4.1 / 4.5 は本回実測、4.2 / 4.6 は verify-001 実測 + 実装不変、4.3 / 4.4 は証跡実在、1.2 / 2.x / 3.x は diff で確認) |
| 逆流検査 (足場の書き換え) | `specs/android-build-toolchain/spec.md` は `git diff HEAD` で**差分なし**。`proposal.md` の差分は tasks 1.2 が明示的に指示する「確定した組み合わせ」表と根拠の追記のみ。`tasks.md` の差分はチェック状態のみ。**逆流なし** |
| 未記録乖離 | なし。実装差分 (csproj / `android/gradle.properties` / 両 wrapper の `distributionSha256Sum`) はいずれも deviation.md に記録済み |
| UI 変更 | なし (ui/ アーティファクトを持たない変更) |
| テスト全件成功 | ✅ 本回 JDK 25 で全件再実行して確認 (上記 S3.1) |

### deviation.md 記録済みの差分 (spec 違反として扱わない)

1. `csproj` の `KsAndroidModuleSource` に `gradle.properties` / `gradle/libs.versions.toml` / `gradle/wrapper/gradle-wrapper.properties` を追加 — catalog がバージョンの SSoT になったことによる増分ビルドの入力漏れを塞ぐ追加
2. S3.3 の「4 module」が誤記 (実態 3 module) — **オーナー確認待ちと明記されている**
3. `android/gradle.properties` にも `org.gradle.tooling.parallel=true` を追加
4. 両 wrapper に `distributionSha256Sum` を追加 (verify-001 で公式配布元の値と一致を確認済み)

## 補足 (判定に影響しない観測)

- **sync の実施範囲**: 今回の sync は `samples/android` プロジェクトを Studio で開いて実行されたもので、`android/` は composite build (`includeBuild("../../android")`) として同一 sync のスコープに含まれる (プロジェクトツリーに `android` ノードが出ており、4 module の構成フェーズが JBR 25 上で成功している)。`android/` を Studio で**単独プロジェクトとして開いた** sync は実施されていない。R1 の実質 (JDK 25 上で両ビルドの構成が通る) は満たされていると判断したが、判断の前提として明示しておく
- **本回の実行が環境に与えた影響**: テスト再実行は `--no-daemon` で行い `--stop` は使っていない (Studio のデーモンを止めないため)。ただし証跡取得時の Studio デーモン (pid 57202) は本回のビルド開始前後に自然終了しており、検証終了時点では存在しない (Gradle 自身のデーモン失効によるものと見られる)。IDE の再 sync で新しいデーモンが起動するだけで、成果物への影響はない。検証側が起動したデーモンの残留はなし (残っている pid 37242 / 41220 は本セッション以前からの JDK 21 デーモン)

## 判定

**VALID** — ❌ 0 件。全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」で、虚偽チェック・逆流・未記録乖離・テスト失敗はいずれもなし。

残る ⚠️ は S3.3 の「4 module」表記のみで、これは実装ではなく **spec 記述の誤り**として deviation.md に記録済み (オーナー確認待ち)。蒸留時に「binding の Exec が束縛するのは core / ui / bridge の 3 module」という事実を残す際、この誤記を引き継がないこと。

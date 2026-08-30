# 検証結果: upgrade-android-build-toolchain (001 回目)

**日付**: 2026-08-21
**判定**: INVALID (❌ 1 件 — Requirement 本文の「sync 成功」が未検証)

デルタスペック: `kasane/changes/upgrade-android-build-toolchain/specs/android-build-toolchain/spec.md`
deviation.md: あり (4 件)

## 対応表

| Requirement / Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| **R1: JDK 25 の Gradle JVM でビルドできる** (本文「sync・ビルドが成功しなければならない」) | `android/gradle/wrapper/gradle-wrapper.properties:3` / `samples/android/gradle/wrapper/gradle-wrapper.properties:3` (Gradle 9.5.0)、`android/gradle/libs.versions.toml:10,15` (AGP 8.13.2 / Kotlin 2.4.10) | ビルド側は下記 2 Scenario で実測済み。**sync (GUI) は未実施** (tasks.md 4.4 未チェック) | ❌ 部分未検証 |
| S1.1: JDK 25 での CLI ビルド | 同上 | レビュアー実測: `android/` `./gradlew test --rerun-tasks` (JAVA_HOME = Android Studio JBR 25.0.2) → BUILD SUCCESSFUL / 230 tasks executed。`samples/android/` `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL | ✅ 一致 |
| S1.2: JDK 21 での後方互換 | 同上 | レビュアー実測: `android/` `./gradlew testDebugUnitTest` (JAVA_HOME = microsoft-21.jdk 21.0.10) → BUILD SUCCESSFUL / 115 tasks up-to-date (構成フェーズは JDK 21 上で実行) | ✅ 一致 |
| **R2: ビルド関連バージョンの宣言がカタログに一元化される** | `android/gradle/libs.versions.toml` (新設、`[versions]` 10/15/18/22・`[plugins]` 25-28・`[libraries]` 31) | 下記 2 Scenario | ✅ 一致 |
| S2.1: バージョン直書きの不在 | 4 module: `core:14,16,18,22,72` / `ui:9,11,14,18,102` / `compose:11,12,13,17,65` / `bridge:9,11,13,17,77`、`samples/android/app/build.gradle.kts:9,10,11,64,96` | レビュアー実測 grep: `plugins` 内 `version "…"` = 0 件、`compose-bom:` リテラル = 0 件 (toml 内の宣言のみ)、`version = "…"` の直書き = 0 件 (4 module とも `libs.versions.ks.settingsview.get()`)。旧値 `8.7.3` / `2.0.21` / `8.10.2` の残存 = 0 件 | ✅ 一致 |
| S2.2: samples からのカタログ共有 | `samples/android/settings.gradle.kts:26-30` (`versionCatalogs { create("libs") { from(files("../../android/gradle/libs.versions.toml")) } }`) | レビュアー実測: `samples/android/` `:app:assembleDebug` 成功。`:app:buildEnvironment` → AGP `com.android.tools.build:gradle:8.13.2`、`kotlin-gradle-plugin:2.4.10`。`:app:dependencies` → `kotlin-stdlib:2.4.10` / `androidx.compose:compose-bom:2024.10.01`。本体と同一版であることを確認 | ✅ 一致 |
| **R3: ツールチェーン更新後も既存の品質ゲートを全て通過する** | 公開 API・実行時挙動の変更なし。`compileSdk = 35` / `minSdk = 29` / `JavaVersion.VERSION_17` / `jvmToolchain(17)` は 4 module とも diff に含まれず不変 | 下記 3 Scenario | ✅ 一致 |
| S3.1: 既存ユニットテストの全数通過 | ツールチェーン更新が強制した機械的修正は `CustomCellTest.kt:165` の 1 行のみ | レビュアー実測 (`--rerun-tasks` 付き全件再実行、XML 集計): **debug 1261 tests / 0 failures**、**release 1261 tests / 0 failures** (core 80 / ui 909 / compose 111 / bridge 161 × 2 variant) | ✅ 一致 |
| S3.2: サンプルアプリの実機動作 | — | `evidence/01〜08*.png` (Pixel 6a、17:26-17:29 撮影) と `evidence/09-logcat-app.txt` (FATAL EXCEPTION 0 件)。スクリーンショットは基本 Cell 7 種・入力 Cell・DSL デモの表示と操作後状態を含み、APK (17:24 生成) と時刻整合 | ✅ 一致 |
| S3.3: MAUI binding からの native ビルド (THEN「**4 module** の release aar が生成され」) | `maui/android/…/KsSettingsView.Binding.Android.csproj:101` の Exec は core / ui / bridge の **3 module** のみ | レビュアー実測: `dotnet build` → 0 エラー。`ks-settingsview-{core,ui,bridge}-release.aar` の 3 件を確認、compose は `build/outputs/` に aar なし。「4 module」は spec 側の誤記 | ⚠️ deviation 記録済み |

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md の完了状況 | 15 項目中 14 完了。**4.4 (Android Studio で sync 成功を確認) のみ未チェック** |
| 虚偽チェック | なし。チェック済みタスクはすべてレビュアー側で再現できた (4.1 / 4.2 / 4.5 / 4.6 は実測、4.3 は証跡実在、1.2 / 2.x / 3.x は diff で確認) |
| 逆流検査 (足場の書き換え) | `specs/android-build-toolchain/spec.md` は未変更。`proposal.md` の差分は tasks 1.2 が明示的に指示する「確定した組み合わせ」表と根拠の追記のみ。`tasks.md` の差分はチェック状態のみ。逆流なし |
| 未記録乖離 | 実装側の未記録乖離はなし (下記 4 件はすべて deviation.md に記録済み) |
| UI 変更 | なし (ui/ アーティファクトを持たない変更) |
| テスト全件成功 | ✅ 上記 S3.1 のとおり実行して確認 |

### deviation.md 記録済みの差分 (spec 違反として扱わない)

1. `csproj` の `KsAndroidModuleSource` に catalog / wrapper / gradle.properties を追加 — 内容を確認し、aar 内容を決める入力の取りこぼしを塞ぐ妥当な追加と判断
2. Scenario S3.3 の「4 module」が誤記 (実態 3 module) — **オーナー確認待ちと明記されている**
3. `android/gradle.properties` にも `org.gradle.tooling.parallel=true` を追加
4. 両 wrapper に `distributionSha256Sum` を追加 — レビュアーが公式配布元 (`downloads.gradle.org/distributions/gradle-9.5.0-bin.zip.sha256`) の値と突き合わせ、`553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746` の一致を確認

## ❌ の見立て

**R1 本文「sync が成功しなければならない」が未検証** (tasks.md 4.4)

- 実装を直す必要はない。CLI 側は JDK 25 / 21 の双方で実測成功しており、更新漏れは検出されていない
- deviation として合意すべき事項でもない (spec の要求自体は正当で、更新の動機そのもの)
- 解消手段は 1 つだけ: **オーナーが Android Studio (Gradle JVM = JDK 25) で `android/` と `samples/android/` を sync し、成功を確認して 4.4 をチェックする**。これが済めば本検証は VALID になる。エージェントは GUI sync を実行できないため、レビュアー側では代替できない

## 判定

**INVALID** — ❌ 1 件 (R1 の sync 節が未検証)、⚠️ 1 件 (S3.3 の「4 module」誤記、deviation 記録済み・オーナー確認待ち)。
それ以外の全 Requirement / Scenario は ✅ 一致。虚偽チェック・逆流・テスト失敗はいずれもなし。

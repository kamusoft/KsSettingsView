# Proposal: upgrade-android-build-toolchain

## Why

Android Studio (内蔵 JBR = JDK 25) で Gradle sync が失敗する。現行 Gradle 8.10.2 は Java 23 までしか対応せず、開発者ごとに Gradle JDK を 21 へ手動固定する暫定運用になっている。Studio 更新のたびに再発し得るため、JDK 25 で動くツールチェーンへ根本更新する。

あわせて、パッケージ配信計画 (kasane/roadmaps/package-distribution) の前段として、4 module + samples に散在するビルド関連バージョンの宣言をバージョンカタログへ集約する。配信側の変更 (maven-publish / lockstep version) が同じファイルを触るため、先に足場を整えて衝突を避ける。

## What Changes

- Gradle wrapper を 9.x (JDK 25 実行対応版) へ更新 — `android/` と `samples/android/` の 2 箇所
- AGP 8.7.3 → Gradle 9 系対応バージョン (保守的には 8.13 系。9.x への引き上げは実装時に互換検証の結果で確定)
- Kotlin 2.0.21 → AGP / Compose Compiler の要求に応じて追随更新
- `android/gradle/libs.versions.toml` を新設し、AGP / Kotlin / Compose Compiler plugin / Compose BOM / project version の宣言を集約する。`android/ks-settingsview-{core,ui,compose,bridge}` の plugins ブロックと直書きバージョンは catalog 参照 (`alias(libs.plugins...)` / `libs.versions...`) へ置き換える。project version の値 (`0.1.0-SNAPSHOT`) は変えない
- `samples/android/settings.gradle.kts` で同じ toml を `versionCatalogs { create("libs") { from(files("../../android/gradle/libs.versions.toml")) } }` により共有し、`samples/android/app/build.gradle.kts` の直書きを catalog 参照へ置き換える
- `samples/android/gradle.properties` に `org.gradle.tooling.parallel=true` を正式追加 (Gradle 9.4+ の IDE parallel sync)

影響する能力: android ビルド基盤のみ (ライブラリの公開 API・実行時挙動・Java 17 ターゲット・compileSdk は変更しない)。

### 確定した組み合わせ (実装時に互換表と実測で確定)

| 項目 | 更新前 | 更新後 |
|---|---|---|
| Gradle wrapper | 8.10.2 | **9.5.0** |
| AGP | 8.7.3 | **8.13.2** |
| Kotlin (kotlin-android / Compose Compiler plugin) | 2.0.21 | **2.4.10** |
| Compose BOM | 2024.10.01 | 2024.10.01 (据え置き) |
| project version | 0.1.0-SNAPSHOT | 0.1.0-SNAPSHOT (据え置き) |

根拠:

- Gradle 互換表 <https://docs.gradle.org/current/userguide/compatibility.html> — Java 25 を実行 JVM にできる最低 Gradle は 9.1.0 (Java 26 は 9.4.0)。Gradle の実行 JVM は 17〜26 のため、9.5.0 は JDK 17 / 21 でも動く
- Kotlin Gradle plugin 互換表 <https://kotlinlang.org/docs/gradle-configure-project.html> — KGP 2.4.0〜2.4.10 がサポートする Gradle は 7.6.3〜9.5.0、AGP は 8.5.2〜9.1.0。Gradle 9.5.0 は KGP 2.4 系がサポートする最上位であり、AGP 8.13 系もこの範囲に入る (現行最新の Gradle 9.7.1 は KGP のサポート上限を超えるため採らない)
- AGP 8.13.0 リリースノート <https://developer.android.com/build/releases/agp-8-13-0-release-notes> — 必要な Gradle は 8.13 以上、JDK は 17 以上。8.13.2 は 8.13 系の最新パッチ
- AGP 9.x は Gradle 9.1.0 以上 (9.1 系は 9.3.1 以上) を要求し、Kotlin 組み込みサポート等の破壊的変更を伴うため採らない <https://developer.android.com/build/releases/agp-9-1-0-release-notes>
- Compose BOM は Kotlin 2.4.10 の Compose Compiler と組み合わせても 2024.10.01 のままビルド・テストが通ることを実測で確認したため据え置く

## Non-Goals

- compileSdk / minSdk / targetSdk の変更
- ライブラリコード・テストコードの変更 (ツールチェーン更新が強制する機械的修正を除く)
- iOS / MAUI 側のビルド構成 (MAUI binding が Exec で呼ぶ `gradlew assembleRelease` が通ることの確認は行うが、csproj は変更しない)
- maven-publish の追加、Gradle `group` の変更、project version の値の変更・注入 (配信計画側の責務)
- catalog 化の対象外: 上記以外のライブラリ依存 (AndroidX 各種・coroutines 等) の集約は任意とし、必須にしない

## Impact

- 破壊的変更: なし (配布物の ABI / API は不変。ビルド環境要件のみ変わる)
- リスク: AGP / Kotlin / Compose Compiler の互換組み合わせ。全ユニットテスト + サンプル実機ビルド + Studio sync + MAUI binding 経由の `assembleRelease` で検証する
- CLI ビルドの JDK: 更新後も JDK 21 で動作すること (下位互換) を確認する

## 級: M

触るファイルは少ないが android 本体 4 モジュール + samples + MAUI binding の Exec 経路に波及し、互換検証を伴うため。

domain: android
roadmap: package-distribution/phase-1-android-build-toolchain

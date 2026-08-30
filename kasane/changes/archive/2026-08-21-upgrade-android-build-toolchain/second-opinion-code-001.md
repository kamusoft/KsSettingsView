# セカンドオピニオン: upgrade-android-build-toolchain (code-001)
**相方**: codex (器: ksn-reviewer) / **日付**: 2026-08-21 / **対象**: 作業ツリーの未コミット変更 (toolchain 更新 + version catalog 導入一式)
---
# レビュー結果: upgrade-android-build-toolchain

**判定**: **NEEDS_DISCUSSION**

## 指摘事項

### [🟠 Major] MAUI 検証が「4 module の AAR 生成」を満たしていない

**該当箇所**: [spec.md:57](kasane/changes/upgrade-android-build-toolchain/specs/android-build-toolchain/spec.md:57)、[KsSettingsView.Binding.Android.csproj:101](maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj:101)、[tasks.md:29](kasane/changes/upgrade-android-build-toolchain/tasks.md:29)

**問題点**: spec は4モジュールの release AAR 生成を要求していますが、Exec は core / ui / bridge の3モジュールだけを指定しています。実際にも release AAR はこの3件のみで、compose の release AAR は存在しません。deviation.md がないため、4.6の完了チェックは現状では仕様に裏づけられていません。

**推奨修正**: 次のどちらが正しい契約かオーナー判断が必要です。

- 4件が正なら `:ks-settingsview-compose:assembleRelease` も実行・検証する。
- MAUI binding に必要な3件が正なら、合意済み乖離として deviation.md に記録してから扱う。

### [🟠 Major] 新しいバージョンSSoTがMSBuildの増分入力に含まれていない

**該当箇所**: [libs.versions.toml:7](android/gradle/libs.versions.toml:7)、[KsSettingsView.Binding.Android.csproj:79](maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj:79)

**問題点**: `_BuildKsSettingsViewAars` の `Inputs` に `gradle/libs.versions.toml` と wrapper 設定が含まれていません。今回AARが再生成されたのは各 `build.gradle.kts` も変更されたためです。今後、catalogだけでKotlin・AGP・Compose BOMを更新すると、既存AARの方が新しければExecがスキップされ、古いAARをbindingへ取り込む可能性があります。

**推奨修正**: 少なくとも以下を `KsAndroidModuleSource` に加えてください。

- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- 必要に応じて `gradlew`、wrapper JAR、Gradleプロパティ

csproj変更をNon-Goalのまま維持するなら、このリスクをどう回避するかの設計判断が必要です。

### [🟠 Major] 変更の主目的であるAndroid Studio syncが未検証

**該当箇所**: [spec.md:7](kasane/changes/upgrade-android-build-toolchain/specs/android-build-toolchain/spec.md:7)、[tasks.md:27](kasane/changes/upgrade-android-build-toolchain/tasks.md:27)

**問題点**: 元の障害はAndroid StudioのGradle syncですが、4.4が未完了です。CLI成功はTooling API経由のsync成功を保証しないため、中心Requirementの完了をまだ確認できません。

**推奨修正**: Android Studio内蔵JBR 25で `android/` と `samples/android/` のsyncを確認し、結果を証跡化してください。

### [🟠 Major] 実装期間中に凍結対象のproposalが更新されている

**該当箇所**: [proposal.md:20](kasane/changes/upgrade-android-build-toolchain/proposal.md:20)、[tasks.md:6](kasane/changes/upgrade-android-build-toolchain/tasks.md:6)

**問題点**: Kasane規約ではproposalは実装開始後の凍結対象ですが、実装時確定値が追記されています。さらにtasks自体がこの逆流更新を要求しており、現在の変更アーティファクトとKasane規約が矛盾しています。

**推奨修正**: 現在のproposalをさらに書き換えて整合させず、今回をどう正規化するかオーナー判断してください。今後は互換組み合わせを提案確定前に決め、実装時の実測結果は証跡側へ残す構成が必要です。

### [🔵 Suggestion] Gradle配布ZIPのチェックサムを固定する

**該当箇所**: [gradle-wrapper.properties:3](android/gradle/wrapper/gradle-wrapper.properties:3)、[samples側:3](samples/android/gradle/wrapper/gradle-wrapper.properties:3)

**問題点**: `distributionSha256Sum` がなく、ダウンロードしたGradle配布物の内容を固定していません。

**推奨修正**: Gradle 9.5.0 bin ZIPの公式SHA-256 `553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746` を両方へ設定してください。[Gradle公式チェックサム](https://gradle.org/release-checksums/)

## 確認できた事項

- Gradle 9.5.0、Kotlin 2.4.10、AGP 8.13.2の組み合わせは公式互換範囲内です。[Gradle互換表](https://docs.gradle.org/current/userguide/compatibility.html)、[Kotlin KGP互換表](https://kotlinlang.org/docs/gradle-configure-project.html)、[AGP 8.13互換情報](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- 両wrapper JARのSHA-256は公式値と一致しています。
- 指定対象にAGP・Kotlin・Compose BOM・project versionの直書きは残っていません。
- 提示された1261 tests / 0 failures、JDK 17/21/25、Sample実機、MAUI bindingの結果をレビュー前提として確認しました。
- ファイルは変更していません。

**件数**: Critical 0 / Major 4 / Minor 0 / Suggestion 1  
**最終判定**: **NEEDS_DISCUSSION**

## 突き合わせ結果 (ホスト側 review-001 / verify-001 との照合、2026-08-21)

| # | 相方の指摘 | ホスト側 | 採否 | 処理 |
|---|---|---|---|---|
| 1 | [Major] MAUI Exec が 3 module のみで spec「4 module」を満たさない | 同指摘 (NEEDS_DISCUSSION 理由 1) | **確定** | spec 側の誤記 (compose は maui/ADR-0006 上そもそも束縛対象外)。deviation.md に記録、オーナー確認へ |
| 2 | [Major] 新設 catalog が csproj の増分入力 `KsAndroidModuleSource` に無い | 指摘なし (レビュー時点で修正済みツリーを確認) | **採用** (相方のみ・根拠強) | オーナー指示により csproj へ toml / wrapper properties / gradle.properties を追加済み。deviation.md 記録。ホスト側が修正後ツリーで `dotnet build` 再現 |
| 3 | [Major] Android Studio sync (4.4) 未検証 | 同指摘 (verify INVALID の唯一の原因) | **確定** | オーナー (または GUI 操作) による sync 実施が完了条件 |
| 4 | [Major] 実装期間中に proposal が更新された (足場凍結との矛盾) | 指摘なし | **降格** (Minor 相当・プロセス所見) | tasks 1.2 が承認済みアーティファクトとして明示的に指示した追記であり、本 change の欠陥ではない。「tasks が実装時の足場追記を要求する構成」は spec-review scope の所見として蒸留時に扱う |
| 5 | [Suggestion] `distributionSha256Sum` 固定 | 同値を独立に裏取り (公式 .sha256 と一致) | **採用** | 両 wrapper properties に追加済み (deviation.md 記録) |

採用 2 / 確定 2 / 降格 1 / 未解決 0。

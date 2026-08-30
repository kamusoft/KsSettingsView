# Exploration: upgrade-android-build-toolchain

## 課題 / 動機

Android Studio(内蔵 JBR が JDK 25)で android / samples/android プロジェクトの Gradle sync が失敗する:
「The project's Gradle version 8.10.2 is incompatible with the Gradle JVM version 25 ... Gradle 8.10.2 supports Java versions between 1.8 and 23」。

現状の暫定対処は Studio の Gradle JDK を Microsoft OpenJDK 21(CLI ビルドと同じ)へ手動で切り替えること。根本対応として、Gradle 本体と AGP / Kotlin プラグインを JDK 25 で動くツールチェーンへ更新したい。

2026-08-21 追記: パッケージ配信計画 (kasane/roadmaps/package-distribution) の探索で、配信側の Android 変更 (maven-publish 追加・group 変更・version 注入) が本 change と同じ `build.gradle.kts` を触ることが分かった。衝突回避と配信 CI のランナー構成 (JDK / Gradle / AGP) を更新後の値で固めるため、本 change を配信計画の前段として先行実施する。

## 現状構成 (2026-08-21 時点で再確認)

| 項目 | 現在 | 備考 |
|---|---|---|
| Gradle wrapper | 8.10.2 (android/ と samples/android/ の両方) | Java 23 まで対応 |
| AGP | 8.7.3 (`android/ks-settingsview-{core,ui,compose,bridge}/build.gradle.kts` と `samples/android/app/build.gradle.kts` の plugins ブロックで個別に宣言) | Gradle 9 系非対応。bridge module は 2026-08-01 の起票後に追加された (現在は 4 module) |
| Kotlin / Compose Compiler plugin | 2.0.21 | AGP 更新に追随して要確認 |
| Compose BOM | 2024.10.01 (各 module でローカル変数としてハードコード) | |
| ライブラリ target | Java 17 / compileSdk 35 / minSdk 29 | 変更対象外 (ビルド JVM の話とは独立) |
| project version | `0.1.0-SNAPSHOT` (4 module にハードコード) | 値は配信計画側で扱う。宣言箇所の集約のみ本 change |
| バージョンカタログ | 未使用 (libs.versions.toml なし) | samples/android は `includeBuild("../../android")` の別ビルドで、plugins 版を二重に持つ |
| CI | 存在しない (`.github/` なし) | 配信計画 (論点 F) で新設予定 |

## 検討した選択肢 (却下案と理由を含む)

| 案 | 内容 | 評価 |
|---|---|---|
| A: Studio の Gradle JDK を 21 に固定 | 設定変更のみ | 暫定対処として実施済み。ただし開発者ごとの手動設定が必要で、Studio 更新のたびに再発し得る |
| B: Gradle 9.x + AGP 8.13+ (または 9.x) + Kotlin 追随のセット更新 | wrapper 2 箇所 + 全モジュールのプラグイン宣言を更新 | **採用**。JDK 25 対応の根本解。AGP 8.7.3 は Gradle 9 系非対応のため Gradle 単独更新は不可 (セット更新が必須) |
| C: Gradle だけ 9.x へ更新 | wrapper のみ | 却下。AGP 8.7.3 が Gradle 9 で動作しないためビルド不能になる |

バージョンカタログの扱い (2026-08-21):

| 案 | 内容 | 評価 |
|---|---|---|
| ① catalog 導入を本 change に含める | `android/gradle/libs.versions.toml` を新設し、plugins / Compose BOM / project version の宣言を集約。samples/android は `versionCatalogs { from(files(...)) }` で同じ toml を共有 | **採用**。toolchain 更新で書き換える箇所と catalog 化する箇所が同じで、分けると二度手間。配信計画 (lockstep 単一バージョン) は catalog に version キーを持つだけで SSoT が得られ、姉妹ライブラリ KsDialogs と同じ形になる |
| ② 含めない (旧 proposal の Non-Goal どおり) | toolchain 更新のみ | 却下。配信 change が plugins ブロックを再編集することになる |

## 決定事項

- 別 change として計画的に実施する (fix-entrycell-ime-composition には混ぜない — オーナー確定 2026-08-01)
- 当初は exploration + proposal のみ作成し実装は保留 (オーナー指示 2026-08-01)。**2026-08-21 にオーナーが配信計画の前段として実装着手を決定**
- 更新時に `samples/android/gradle.properties` へ `org.gradle.tooling.parallel=true` (Gradle 9.4+ の IDE parallel sync 設定) を正式に入れてよい (今回 IDE 自動追記されたものは一旦 revert 済み)
- バージョンカタログ (libs.versions.toml) の導入を本 change に含める (オーナー確定 2026-08-21、上表①)。project version の**値** (`0.1.0-SNAPSHOT`) は変えず宣言箇所だけ集約する。maven-publish / group 変更 / version 注入は配信計画側の責務

## ADR 候補

なし (ツールチェーンのバージョン更新・宣言箇所の集約は可逆で、境界も越えない)

## 未決の論点

- 目標バージョンの確定: Gradle 9.x のどのマイナーか (JDK 25 実行対応の最低ライン以上)、AGP は 8.13 系に留めるか 9.x へ上げるか (AGP 9 は Kotlin 組み込みサポート等の大きい変更を含むため、保守的には 8.13 系)
- Kotlin 2.0.21 の更新要否 (AGP 側の要求と Compose Compiler プラグインの互換で決まる)
- compileSdk / minSdk は本 change の対象外とする (推奨: 対象外。ツールチェーンのみに絞る)
- MAUI binding (`maui/android/KsSettingsView.Binding.Android`) は `android/gradlew ... assembleRelease` を Exec で呼ぶ (maui/ADR-0006)。更新後の Gradle / AGP でも同コマンドが通ること、および dotnet 側の JDK 要件 (Android workload が使う JDK) と矛盾しないことを確認する

## UI 素材

なし (ビルド基盤のみ、UI 変更なし)

## 変更級の推奨: M (理由)

公開 API 変更なし・可逆だが、android 本体 4 モジュール + samples + MAUI binding の Exec 経路の全ビルドに波及し、AGP / Kotlin の互換検証 (全ユニットテスト + サンプル実機ビルド + Studio sync 確認) を伴うため。触るファイルは少ないが影響半径がビルド全体に及ぶので S ではなく M。

## 関連ファイル

- `android/gradle/wrapper/gradle-wrapper.properties` / `samples/android/gradle/wrapper/gradle-wrapper.properties`
- `android/gradle/libs.versions.toml` (新設)
- `android/ks-settingsview-core/build.gradle.kts` / `android/ks-settingsview-ui/build.gradle.kts` / `android/ks-settingsview-compose/build.gradle.kts` / `android/ks-settingsview-bridge/build.gradle.kts` (plugins ブロック・Compose BOM・version の直書き)
- `samples/android/settings.gradle.kts` (catalog 共有) / `samples/android/app/build.gradle.kts`
- `samples/android/gradle.properties` (tooling.parallel の正式追加)
- `maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj` (gradlew Exec 経路の確認のみ、変更は想定しない)

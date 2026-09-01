# レビュー結果: adopt-android-explicit-api-mode (001 回目)

**日付**: 2026-09-01
**判定**: APPROVED

## サマリー

Android 本体 module への Explicit API Strict 導入、既存公開面の明示、指定 3 API の `internal` 化は、デルタスペックと android/ADR-0022 に一致している。production 差分に意図外のロジック変更はなく、公開 ABI 比較、外部 Kotlin visibility probe、既存テスト結果から全 Scenario の成立を確認したため承認する。

## 照合した規約

- ソースコメント規約 (`always`)
- テスト実行規約 (Android テストの実行・件数報告)
- 公開識別子と配布座標 (`android/kssettingsview/build.gradle.kts` を変更)
- ローカル開発環境と Sample の実行 (本体ビルド・lint と Sample build の手順)
- android/ADR-0022「Android 公開ライブラリは Explicit API Strict で公開境界を強制する」
- Kotlin 実装・レビュースキルの Explicit API、KDoc、Gradle Kotlin DSL、テスト観点

## Scenario ごとの実装・テスト対応

| Requirement / Scenario | 実装 | テスト・証跡 | 判定 |
|---|---|---|---|
| Android 公開ライブラリの明示 API 境界 / 公開宣言の明示不足を拒否する | `android/kssettingsview/build.gradle.kts:83` の `explicitApi()` | `evidence/explicit-api-positive-control.txt:1` で debug / release とも visibility 診断 204 件、失敗を確認 | 適合 |
| Android 公開ライブラリの明示 API 境界 / 明示済みの公開面を両 variant でコンパイルできる | production の公開宣言へ `public` を明示 | 本体 debug / release Kotlin compilation 成功の既実行結果、および後続の両 variant テスト結果で確認 | 適合 |
| Android 公開ライブラリの明示 API 境界 / 対象外の compilation に Strict が波及しない | Strict 設定は `android/kssettingsview` のみに配置 | 本体 test と Bridge debug / release compilation が成功。本体 1183 件 × 2、Bridge 167 件 × 2 の XML を独立集計し、合計 2700 件、failures / errors / skipped はすべて 0 | 適合 |
| 公開 API 差分の限定 / release AAR の公開 ABI 差分が意図した降格だけになる | `viewTypeOf`、`isRegistered`、`preview` だけを `internal` 化 | `evidence/release-aar-abi-diff.txt:1` で 270 class / 2,849 行を変更前後で全走査し、3 API の JVM 名変更以外に増減 0 | 適合 |
| Cell Registry の利用者向け公開面 / 外部利用者が Cell を登録できる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:73`、同 `:87`、同 `:126` と一括登録 extension、`CellViewHolder` を public 維持 | `evidence/external-visibility-probe.txt:5` の正の probe と既存 Registry テストで確認 | 適合 |
| Cell Registry の利用者向け公開面 / 外部利用者が内部照会を参照できない | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:167` と同 `:206` を `internal` 化 | `evidence/external-visibility-probe.txt:13` と同 `:16` の負の probe が internal visibility を理由に失敗 | 適合 |
| SettingsRootStore の生成境界 / 外部利用者が通常コンストラクタで Store を生成できる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:32` の public class / constructor | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt:410` と `evidence/external-visibility-probe.txt:5` の正の probe で root / theme 初期化と外部参照を確認 | 適合 |
| SettingsRootStore の生成境界 / 外部利用者が Preview factory を参照できない | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:347` を `internal` 化 | `evidence/external-visibility-probe.txt:19` の負の probe が internal visibility を理由に失敗 | 適合 |

## 検証メモ

- production 差分は `public` 明示、指定 3 API の `internal` 化、Strict 設定、必要な整形に限定され、意図外の処理変更はない。
- `tasks.md` の完了チェックは、実装・既存テスト・3 件の evidence と対応しており、未実装の虚偽チェックは見当たらない。
- proposal / spec に実装中の逆流修正はなく、`deviation.md` を必要とする未記録の仕様逸脱もない。
- `python3 scripts/comment-policy-lint.py --advisory` は禁止 0 件。要確認 8 件は今回変更していない既存コメントであり、本 change の違反ではない。local-path lint、identity lint、`git diff --check` は成功した。
- 独立レビューで `./gradlew test --rerun-tasks` を再実行したところ、レビュー環境に Android SDK location が設定されておらず Kotlin compilation 前に停止した。コード起因のビルド・テスト失敗ではない。2026-09-01 22:43〜22:45 +0900 に生成された 4 variant の XML を再集計し、本体 debug / release 各 1183 件、Bridge debug / release 各 167 件、failures / errors / skipped 0 を確認した。

## 指摘事項

なし。

## アクションプラン

追加修正なし。独立一致検証へ進める。

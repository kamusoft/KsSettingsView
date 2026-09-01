# レビュー結果: adopt-android-explicit-api-mode (002 回目)

**日付**: 2026-09-01
**判定**: APPROVED

## サマリー

前回のクロスモデル突き合わせで採用された公開 KDoc・内部 KDoc・ABI 証跡・公開定数 KDoc の 4 修正は、いずれも指摘意図とソースコメント規約に適合している。修正はコメントと証跡の正確化に限定され、Explicit API Strict、指定 3 API の `internal` 化、外部公開面、および初回レビューで適合確認済みの全 8 Scenario を損ねていないため承認する。

指摘件数は Critical 0 件 / Major 0 件 / Minor 0 件 / Suggestion 0 件。

## 照合した規約

- ソースコメント規約 (`always`)
- テスト実行規約 (Android の variant 別実行件数と失敗数の確認)
- 公開識別子と配布座標 (`android/kssettingsview/build.gradle.kts` が対象)
- ローカル開発環境と Sample の実行 (本体 compilation と Sample 外部 probe の手順)
- android/ADR-0022「Android 公開ライブラリは Explicit API Strict で公開境界を強制する」
- Kotlin 実装・レビュースキルの Explicit API、公開 KDoc、Gradle Kotlin DSL、テスト観点
- `kasane/lessons/code-review.md` L-001 (テスト検出力の重点観点。今回の修正は挙動変更を含まず、Strict 陽性対照と既存の外部 visibility probe を検出力の証跡として照合)

## 修正後の重点確認

| 確認対象 | 該当箇所 | 確認結果 |
|---|---|---|
| `KsCellRegistry` の公開 KDoc | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:9` | 内部メンバーや Adapter の説明を含まず、外部利用者が `register` と `CellViewHolder` を用いて独自 Cell を登録できる契約だけで自己完結している |
| `isRegistered` の内部 KDoc | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:189` | `KsSettingsView` の自動登録が既存登録を上書きしない判定と、テスト・診断の双方に使う実態を正確に記述している |
| ABI 証跡の保証範囲 | `evidence/release-aar-abi-diff.txt:14` | `javap -public` が JVM public member の変化を観測する手法で、Kotlin の型単位の `internal` 化は別の source 差分走査で否定したことを明記しており、保証を過大に表現していない |
| `DEFAULT_BACKGROUND_COLOR` の公開 KDoc | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:144` | change 識別子・履歴記述・内部用語が除去され、現在の機能だけを説明している |

### 付随修正の同梱条件

`deviation.md:3`、同 `:4`、同 `:5`、同 `:6` の 4 記録は、実体として `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`、`evidence/release-aar-abi-diff.txt` の 3 ファイルに収まる。いずれも本務で触れたファイルまたは同じ Android 公開 API 境界能力にあり、公開 API・データスキーマ・既存 ADR・実行時挙動を変更せず、新しい抽象やユーザー判断も持ち込まない。コメント lint と静的照合で担保できる修正であり、ksn-core の付随修正 5 条件をすべて満たす。

前回採否で降格された `SettingsRootStore.preview` の削除、primary constructor property の表記統一、keep / demote 一覧の恒久成果物化は、合意済み Requirement との衝突または実害不足という採否理由が現在も成立しており、再指摘しない。

## Scenario ごとの実装・テスト対応

| Requirement / Scenario | 実装 | テスト・証跡 | 判定 |
|---|---|---|---|
| Android 公開ライブラリの明示 API 境界 / 公開宣言の明示不足を拒否する | `android/kssettingsview/build.gradle.kts:86` の `explicitApi()` | `evidence/explicit-api-positive-control.txt:1` で debug / release とも visibility 診断 204 件、`BUILD FAILED` を確認 | 適合 |
| Android 公開ライブラリの明示 API 境界 / 明示済みの公開面を両 variant でコンパイルできる | production の公開宣言へ visibility と必要な型を明示 | 修正後の `:kssettingsview:compileDebugKotlin` / `:kssettingsview:compileReleaseKotlin` が成功 | 適合 |
| Android 公開ライブラリの明示 API 境界 / 対象外の compilation に Strict が波及しない | Strict 設定を公開本体 module の production source に限定 | 初回の本体 test と Bridge debug / release compilation 成功を確認済み。本回修正は Bridge・test source・Gradle 設定に触れず、関連テストは debug / release 各 39 件、failures / errors / skipped 0 | 適合 |
| 公開 API 差分の限定 / release AAR の公開 ABI 差分が意図した降格だけになる | `viewTypeOf`、`isRegistered`、`preview` のみ `internal` | `evidence/release-aar-abi-diff.txt:8` で前後 270 class / 2,849 行、指定 3 API の JVM 名変更以外の増減 0。型単位の誤降格は同 `:14` の別走査で否定 | 適合 |
| Cell Registry の利用者向け公開面 / 外部利用者が Cell を登録できる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:59`、同 `:73`、同 `:112`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryBasicCells.kt:29`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryInputCells.kt:19`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryCustomCell.kt:23`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellViewHolder.kt:39` を public 維持 | `evidence/external-visibility-probe.txt:5` の外部正 probe が成功 | 適合 |
| Cell Registry の利用者向け公開面 / 外部利用者が内部照会を参照できない | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:153` と同 `:194` を `internal` 維持 | `evidence/external-visibility-probe.txt:12` の負 probe 2 件が internal visibility を理由に失敗 | 適合 |
| SettingsRootStore の生成境界 / 外部利用者が通常コンストラクタで Store を生成できる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:32` の public constructor | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt:410` で root / theme 初期化、`evidence/external-visibility-probe.txt:5` で外部参照を確認 | 適合 |
| SettingsRootStore の生成境界 / 外部利用者が Preview factory を参照できない | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:347` を `internal` 維持 | `evidence/external-visibility-probe.txt:19` の負 probe が internal visibility を理由に失敗 | 適合 |

## 検証メモ

- 修正後の本体 debug / release Kotlin compilation は成功している。
- 関連テストは `KsCellRegistryTest` 10 件と `SettingsRootStoreTest` 29 件で、debug / release 各 39 件。failures / errors / skipped はすべて 0 で、XML の実行件数も一致する。
- comment-policy lint、local-path lint、identity lint、差分 whitespace 検査は成功している。
- `tasks.md` の完了チェックは現在の実装・テスト・3 件の evidence と対応し、未実装の虚偽チェックはない。proposal / spec の逆流修正、未記録の仕様逸脱もない。

## 指摘事項

なし。

## アクションプラン

追加修正なし。独立一致検証へ進める。

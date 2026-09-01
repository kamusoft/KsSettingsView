# 一致検証: adopt-android-explicit-api-mode (001 回目)

**日付**: 2026-09-01
**判定**: **VALID**

## 検証範囲

- デルタスペック: `specs/android-public-api-boundary/spec.md` の 4 Requirement・8 Scenario
- 実装: `android/kssettingsview/**` の本 change に属する最終差分
- 足場・証跡: `proposal.md`、`tasks.md`、`deviation.md`、`evidence/`、`review-001.md`、`review-002.md`、`second-opinion-code-001.md`
- 対象外: `.codex/config.toml`、Bridge module の機能変更、iOS、後続の配布消費者検証

## Requirement / Scenario 対応表

| Requirement / Scenario | 実装 | テスト・証跡 | 状態 |
|---|---|---|---|
| Android 公開ライブラリの明示 API 境界 / 公開宣言の明示不足を拒否する | `android/kssettingsview/build.gradle.kts:83` の Kotlin 設定で `explicitApi()` を有効化 | `evidence/explicit-api-positive-control.txt:3`。明示前の debug / release compilation はそれぞれ visibility 診断 204 件で失敗 | ✅ 一致 |
| Android 公開ライブラリの明示 API 境界 / 明示済みの公開面を両 variant でコンパイルできる | `android/kssettingsview/src/main/kotlin/` の公開宣言へ visibility と必要な型を明示し、`android/kssettingsview/build.gradle.kts:86` の Strict 下でコンパイル対象にした | 初回および付随修正後の `:kssettingsview:compileDebugKotlin` / `:kssettingsview:compileReleaseKotlin` が成功。`evidence/explicit-api-positive-control.txt:17` に明示後の成功確認を記録 | ✅ 一致 |
| Android 公開ライブラリの明示 API 境界 / 対象外の compilation に Strict が波及しない | `explicitApi()` は `android/kssettingsview/build.gradle.kts:86` の本体 module にのみ存在し、Bridge module の設定と test source は変更していない | 本体 test と Bridge debug / release compilation が成功。本体 1,183 件 × 2 variant、Bridge 167 件 × 2 variant、合計 2,700 件で failures / errors / skipped は 0 | ✅ 一致 |
| 公開 API 差分の限定 / release AAR の公開 ABI 差分が意図した降格だけになる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:153`、同 `:194`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:347` の 3 関数だけを `internal` 化 | `evidence/release-aar-abi-diff.txt:3`。変更前後とも 270 class / 2,849 行で、指定 3 API の JVM 名変更以外の増減は 0。型単位の誤降格は同 `:14` の source 差分全走査で否定 | ✅ 一致 |
| Cell Registry の利用者向け公開面 / 外部利用者が Cell を登録できる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:59`、同 `:73`、同 `:112`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryBasicCells.kt:29`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryInputCells.kt:19`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryCustomCell.kt:23`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellViewHolder.kt:39` を public 維持 | `evidence/external-visibility-probe.txt:5`。外部 Kotlin code から登録 API、`strictMode`、`CELL_VIEW_TYPE_MIN`、一括登録 API、`CellViewHolder` を参照する正 probe が成功 | ✅ 一致 |
| Cell Registry の利用者向け公開面 / 外部利用者が内部照会を参照できない | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:153` の `viewTypeOf` と同 `:194` の `isRegistered` を `internal` 維持 | `evidence/external-visibility-probe.txt:12`。各 API の負 probe が internal visibility 診断で失敗 | ✅ 一致 |
| SettingsRootStore の生成境界 / 外部利用者が通常コンストラクタで Store を生成できる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:32` の public constructor を維持 | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt:410` で root / theme の初期化を検証。`evidence/external-visibility-probe.txt:5` の外部正 probe も成功 | ✅ 一致 |
| SettingsRootStore の生成境界 / 外部利用者が Preview factory を参照できない | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:347` の `preview` を `internal` 維持 | `evidence/external-visibility-probe.txt:19`。外部負 probe が internal visibility 診断で失敗 | ✅ 一致 |

## 付随修正の確認

`deviation.md:3` から同 `:6` の 4 件は Requirement / Scenario 対応表の対象外とし、合意済み差分として次を確認した。

| 記録 | 実体 | 同梱条件 | 状態 |
|---|---|---|---|
| `KsCellRegistry` の公開 KDoc を利用者向け契約へ整理 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:9` | 本務で触れた同一ファイルのコメントのみ。公開 API・挙動・設計判断を変更しない | ⚠️ deviation 記録済み |
| `isRegistered` の KDoc を実利用へ合わせる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:189` | 本務で触れた同一ファイルのコメントのみ。公開 API・挙動・設計判断を変更しない | ⚠️ deviation 記録済み |
| ABI 証跡へ手法の保証範囲を追記 | `evidence/release-aar-abi-diff.txt:14` | 同一能力の既存証跡の正確化のみ。実装・公開 API・挙動を変更しない | ⚠️ deviation 記録済み |
| `DEFAULT_BACKGROUND_COLOR` の KDoc を現在仕様へ整理 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:144` | 本務で触れた同一能力のファイル内コメントのみ。公開 API・挙動・設計判断を変更しない | ⚠️ deviation 記録済み |

実体は 3 ファイルに収まり、いずれも本務で触れたファイルまたは同じ Android 公開 API 境界能力にある。公開 API、データスキーマ、既存 ADR、実行時挙動、新しい抽象、ユーザー判断を要する分岐には触れず、コメント／証跡 lint と付随修正後の compilation・関連テストで担保されているため、同梱条件 5 項目を満たす。

## 追加検査

- `tasks.md` は 15 タスクがすべて完了済みで、各タスクは上表の実装・テスト・3 件の evidence に対応する。未実装の虚偽チェックは 0 件。
- proposal / spec は実装中の都合による書き換えがなく、レビュー 2 回とクロスモデル差分全走査でも逆流は検出されていない。
- Scenario に対応しない最終差分は上記 4 件だけで、すべて `deviation.md` に `[付随修正]` として記録済み。未記録乖離は 0 件。
- 初回の本体・Bridge debug / release compilation、Sample `assembleDebug`、全 2,700 テスト、Android lint、各プロジェクト lint は成功している。
- 付随修正後の本体 debug / release compilation、関連テスト `KsCellRegistryTest` 10 件 + `SettingsRootStoreTest` 29 件の各 variant 39 件、各 lint は成功している。failures / errors / skipped はすべて 0。
- 本検証環境には Android SDK が存在しないため再実行は行えず、直前の実行主体から引き渡された客観的結果、保存済み evidence、テスト XML を独立レビューが集計した結果を照合した。未検証扱いにする対象はない。
- UI 変更ではないため、UI アーティファクト検査は対象外。

## 不一致の分類

| 分類 | 件数 | 内容 |
|---|---:|---|
| 未実装 | 0 | なし |
| 未検証 | 0 | なし |
| 未記録乖離 | 0 | なし |

## 最終判定

**VALID** — 4 Requirement・8 Scenario はすべて実装とテスト／証跡に一致する。全タスクの完了は実体を伴い、足場の逆流、テスト失敗、未実装、未検証、未記録乖離はない。4 件の付随修正は `deviation.md` に記録された合意済み差分であり、同梱条件を満たす。

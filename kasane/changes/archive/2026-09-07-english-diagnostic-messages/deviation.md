# Deviation: english-diagnostic-messages

S 級のため足場は exploration.md のみ。以下は「決定事項」「未決の論点」との差分と、スコープ外に手を入れた付随修正の記録。

## 未決論点の確定 (実装時)

- 「規約の適用範囲に samples / verification の例外メッセージを含めるか」(exploration の未決の論点): **含めず対象外**とし、「ただし samples 内の例外メッセージも本体と同じ英語で書くと一貫する」の 1 文を添えるに留めた。理由: rule の違反判定対象を利用者が参照する本体コードに絞るため。現状すべて英語で実害はない (2026-09-07)

## 付随修正

- [付随修正] `KsBridgeValueTransport` の `diagnose` (Android `kssettingsview-bridge/.../KsBridgeValueTransport.kt` / iOS `Sources/KsSettingsViewBridge/KsBridgeValueTransport.swift`): フォールバック結果を表す引数を足し、`time` → `00:00` / `date` → `1970-01-01` / `optionalDate` → `null` (Kotlin) · `nil` (Swift) を出し分ける形にした。理由: `optionalDate` は既定値を返さず null / nil を返すため、共通文言の「既定値で構築します」(英語化後は "falling back to the default value") が戻り値と一致せず、原因調査を誤誘導していた (日本語の原文から続く既存の不正確さ)。review-001 Minor 起因 (2026-09-07)
- [付随修正] 同 2 ファイル: 診断に載せる既定値を、実際に返す値の定数から導出する形にした (Kotlin は `LocalTime.MIDNIGHT.toString()` / `EPOCH_DATE.toString()`、Swift は `defaultTimeText` / `defaultDateText` を新設して parse と診断で共用)。理由: リテラルの二重定義だと既定値を変えたとき診断だけ古い値を出し続け、今回直したのと同じ「診断が事実と一致しない」欠陥として再発する。review-002 Suggestion 起因 (2026-09-07)

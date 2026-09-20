# レビュー結果: date-time-picker-locale (001 回目)

**日付**: 2026-09-20
**判定**: CHANGES_REQUESTED

## サマリー

iOS / Android の独立ビルドと全テストは成功し、formatter cache の Locale 分離、Android の表示中 Cell 再 bind、明示 `valueText` 保持、Date / Time 両方の基本回帰テストは確認できた。accepted の core/ADR-0028 とも衝突せず、`is24Hour` は引き続き時制の唯一の決定源で、Bridge の POSIX 輸送・MAUI・公開 API に差分はない。

ただし iOS の実行中 Locale 変更経路には、表示中の Wheels / Time Picker の未確定選択を元の Cell 値へ巻き戻す不具合がある。また、iOS の新規テストと8枚の証跡は `currentLocaleDidChangeNotification` からの更新を一度も通しておらず、この不具合を検出できない。Major 2 件と、Android の新規収束待機に関する高優先度 Minor 1 件のため差し戻す。

## 照合した規約

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` | 常時 (コメント構文を持つ source / test) | 禁止 0 件。変更コメントを本文とも照合 |
| `kasane/handbook/cross/test-execution.md` | テスト実行・結果報告、非同期反映の収束待ち | 全件実行と件数確認は適合。Android 新規テストの待機は Minor-1 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | OS Locale 通知と表示中 Picker / Cell の実行時追随 | iOS の同一 Host 上の通知経路に証跡がなく Major-2 |
| `kasane/handbook/cross/local-development-setup.md` | iOS / Android 本体 build・lint、Simulator 実行 | 記載手順と build root に従って実行 |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/**` を触る変更の完了判定 | `SWIFT_VERSION=6` 上書きで source 4 target を再コンパイルし error 0 |

index を読んだうえで非該当と判定した文書: `cross/sample-parity.md` (Sample source に差分なし)、`cross/public-identifiers.md` (公開識別子・Package 定義に差分なし)、`cross/diagnostic-message-language.md` (開発者向け文字列の追加なし)、`cross/aiforms-origin-reference.md`、`cross/user-skill-api-listing.md`、`cross/install-examples.md`、`cross/release-procedure.md`。

`kasane/lessons/code-review.md` の L-001 を読み、テストの回帰検出力を重点確認した。今回は読み取り専用制約があり、かつ iOS の通知経路を新規テストが呼ばないことは call path 上で機械的に確定できるため、source mutation は行っていない。

## 自分で実行した確認

- **iOS build**: iPhone 17 Pro / iOS 26.5 Simulator、`xcodebuild build -scheme KsSettingsView ...` — **BUILD SUCCEEDED**。
- **iOS 全テスト**: 同 Simulator、`xcodebuild test -scheme KsSettingsView ...` — `.xcresult` 集計で **1,110 tests / 0 failures / 0 skipped** (Bridge 173 / Core 88 / SwiftUI 112 / TestSupport 7 / UI 730)。
- **Swift 6 言語モード**: 読み取り専用制約のため `ios/Package.swift` は編集せず、`xcodebuild build ... SWIFT_VERSION=6` を実行。compiler invocation の `-swift-version 6` と **BUILD SUCCEEDED / error 0** を確認し、`ios/Package.swift` の差分 0 も確認した。
- **Android build / lint**: `./gradlew :kssettingsview:assembleDebug :kssettingsview:lintDebug` — **BUILD SUCCESSFUL**。
- **Android 全テスト**: `./gradlew test --rerun-tasks` — **BUILD SUCCESSFUL**。JUnit XML 集計で **2,994 tests / 0 failures / 0 errors / 0 skipped** (`kssettingsview` debug 1,320 + release 1,320、bridge debug 177 + release 177)。
- **comment-policy lint**: 対象 source / test に `scripts/comment-policy-lint.py --advisory --summary` を実行し、禁止 0 件。
- **identity / local-path lint**: `kasane/changes/date-time-picker-locale/` に対して両方とも exit 0。
- **証跡**: `evidence/` の8枚をすべて開き、Android 1080×2400 が4枚、iOS 1206×2622 が4枚で、重複ファイルでないことを SHA-256 でも確認した。英語 / 日本語それぞれで Date / Time Picker と Time の自動 `valueText` が同じ言語になること、画像内に個人情報がないことを目視確認した。Date Cell は既定の数値 format のため、画像だけでは Date の自動 `valueText` の Locale 追随を観測できない。
- **既存決定との整合**: accepted の `core/ADR-0028` を拘束根拠として照合し、picker の時制が `is24Hour` のまま、Locale は言語・地域表現だけを担うことを確認した。proposed の `core/ADR-0035` は指摘の拘束根拠には使っていない。
- **スコープ境界**: Bridge / MAUI / 公開 API に差分なし。Android Picker 内部の挙動変更なし (`DateSelectionSheet.kt` の差分は internal な `Configuration.primaryLocale()` の追加のみ)。明示 `valueText` を保持する Date / Time テストあり。deviation.md は存在せず、記録のない別スコープ変更は検出しなかった。

## 指摘事項

### [🟠 Major] iOS の Locale 通知が編集中の Picker 値を元の Cell 値へ巻き戻す

**該当箇所**:

- `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift:126-130` / `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift:79-81`
- `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift:211-216` / `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift:110-118`

**問題点**: `currentLocaleDidChangeNotification` を受けた handler は、表示中の Cell を `render(cell:theme:)` し直す。TimePicker の `render` は `datePicker.date = tc.time`、DatePicker の `render` は `wheelsPicker.date = dc.date` を必ず実行するため、Picker を開いて利用者がまだ確定していない値を選んでいる途中に Locale 通知が届くと、その選択を捨てて提示開始時の Cell 値へ戻す。

例えば時刻 Cell が 10:15 の状態で利用者がホイールを 11:30 へ動かし、確定前にアプリの Locale 更新通知が届くと、表示言語の更新と同時にホイールが 10:15 へ戻る。日付 Wheels も同じである。Locale 変更で更新すべきなのは表示言語・地域表現であり、未確定の利用者入力ではない。Calendar sheet は `applyCurrentLocale()` が picker の Locale だけを差し替えるため、この巻き戻しは Wheels / Time 経路だけに生じる。

**推奨修正**: Locale 更新を full `render` と分離し、Cell 行の自動 `valueText` と picker の Locale だけを更新する。full render を再利用する場合でも、first responder 中は現在の `datePicker.date` / `wheelsPicker.date` を退避して render 後に戻し、提示開始時の `preSelectedDate` は元の Cell 値のまま維持する。Date Wheels / Time の両方に「未確定値を変更 → Locale 更新 → 未確定値が保持される → Done でその値が通知される」テストを追加する。

### [🟠 Major] iOS の「実行中 Host で Locale を更新する」経路に回帰検出力と実行時証跡がない

**該当箇所**:

- `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:737-768`
- `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:925-964`
- `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift:44-49` / `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift:126-131`
- `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift:80-85` / `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift:211-217`
- `evidence/ios-date-en.png` / `evidence/ios-date-ja.png` / `evidence/ios-time-en.png` / `evidence/ios-time-ja.png`

**問題点**: iOS の新規テストは formatter へ Locale を直接渡す、test hook で Date Wheels の `locale` を直接代入する、Calendar controller を指定 Locale で初期化する、という単体部品の確認に留まる。`NSLocale.currentLocaleDidChangeNotification` を post するテスト、`handleLocaleChanged` を通すテスト、表示中 Calendar の `applyCurrentLocale()` を通すテスト、通知後の Cell 行 `valueText` を観測するテストは1件もない。TimePicker に追加された `_applyLocaleForTesting` はテストから呼ばれていない。

したがって observer 登録と両 `handleLocaleChanged`、Calendar の `applyCurrentLocale()` を削除しても、今回追加された iOS テストはすべて green のままになる。Major-1 の巻き戻しも検出できない。8枚の証跡は英語状態 / 日本語状態の最終表示を示すが、別々の時刻に撮られた静止画で、同一プロセス・同一 Host が通知を受けて遷移したことや、変更途中の選択保持は示さない。`runtime-behavior-verification.md` が求める「同一手順での実環境の解消確認」に対して、実行中通知 Scenario の証拠が欠けている。

**推奨修正**: production handler と同じ経路を通せる Locale 注入 seam を設け、Time / Date Wheels / Date Calendar の各表示中状態について、少なくとも次を自動テストする。

1. Locale 更新前後で picker の言語が変わる
2. Cell の自動 `valueText` が Date / Time とも更新される
3. 明示 `valueText` は変わらない
4. `is24Hour` は Locale に影響されず維持される
5. 未確定選択は保持される

さらに Simulator で、アプリを終了・再起動して別 Locale を撮るだけでなく、実行中 Host に通知が届く条件を再現し、同一操作手順で Picker と Cell がともに更新される証跡を残す。OS の設定変更がプロセス再起動になる環境では、その事実を記録し、テスト seam で通知到達時の経路を補完する。

### [🟡 Minor / 優先度高] Android の Locale 再 bind テストが正の収束を `idle()` だけで待っている

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeAppearanceResolutionTest.kt:516-522`

**問題点**: `dispatchConfigurationChanged` は Adapter へ payload 付き変更通知を出し、表示中 ViewHolder の再 bind 後の文字列を検証する。これは待つべき正の遷移がある非同期反映だが、新規テストは `idle()` と `layout(view)` の後に即 assert している。`test-execution.md` は正の収束待ちについて、実時間 deadline・実行機会の譲渡・超過時の実測値付き failure の3点を必須としており、同 package にはそのための `awaitMainLooperCondition` / `awaitConvergence` が既にある。全件 green はこの待機形が規約へ適合する根拠にはならない。

**推奨修正**: Time / Date の現在表示を diagnostics に含めた `awaitMainLooperCondition`、または `awaitConvergence(..., extraDiagnostics:)` で、両方が日本語表記へ到達する条件そのものを待ってから assert する。

## アクションプラン

1. **[必須]** iOS の Locale 更新処理を full render から分離するか、編集中 picker 値を退避・復元し、Date Wheels / Time の未確定選択を保持する
2. **[必須]** iOS の production Locale 通知経路を通す Date / Time の回帰テストを追加し、Cell 自動値・Picker・明示値・`is24Hour`・未確定選択を確認する
3. **[必須]** iOS の実行中 Host 更新 Scenario について実環境証跡を追加する。OS がプロセスを再起動する場合はその制約を記録し、自動テスト seam と役割分担を明示する
4. **[必須]** Android の新規 Locale 再 bind テストを条件ベース待機へ置き換える
5. 修正後、iOS Simulator 全件・Swift 6 言語モード build・Android `./gradlew test --rerun-tasks` を再実行し、件数と失敗0を報告する

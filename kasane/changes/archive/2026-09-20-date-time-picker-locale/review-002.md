# レビュー結果: date-time-picker-locale (002 回目)

**日付**: 2026-09-20
**判定**: APPROVED

## サマリー

`review-001.md` の必須対応 4 項目は、現コード・テスト・証跡で解消を確認した。iOS の Locale 通知処理は full render から分離され、Time / Date Wheels / Date Calendar の未確定値を保ったまま Cell と Picker の Locale だけを更新する。production の NotificationCenter 経路、明示 `valueText`、`is24Hour`、Done 通知値にも回帰検出力があり、Android の再 bind テストは条件ベース待機へ置き換わっている。

iOS / Android の独立ビルドと全テスト、Swift 6 言語モード build はすべて成功した。Critical / Major / 優先度の高い Minor は検出しなかった。

## 照合した規約

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` | 常時 | 対象 15 source / test の lint は禁止 0 件。要確認 8 件は変更範囲外の既存コメントで、今回の Locale 実装には新規違反なし |
| `kasane/handbook/cross/test-execution.md` | 全テスト実行、Android の非同期再 bind | 全件実行と件数集計に適合。Android は deadline・`Thread.sleep(1)` / main looper 1 task 実行・実測 diagnostics を備える条件ベース待機 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | OS Locale と表示中 Host の実行時連携 | 8 枚の実描画と、同一プロセス更新を launch argument では再現できない制約記録、production 通知経路テストの役割分担を確認 |
| `kasane/handbook/cross/local-development-setup.md` | iOS / Android 本体 build・lint、Simulator 検証 | 記載された platform build root と実行方法に従って確認 |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/**` を触る変更の完了判定 | `SWIFT_VERSION=6` 上書きで source 4 target を再コンパイルし、`-swift-version 6`、error 0、build 成功を確認 |

`kasane/handbook/index.md` から上記を選択した。`kasane/lessons/code-review.md` の L-001 も照合した。今回はコード変更禁止で mutation probe は行わず、各 production observer / handler の削除・full render への逆戻りがどの assertion で検出されるかを call path 上で確認した。

## `review-001.md` 指摘の再確認

### 1. iOS の未確定値巻き戻し

解消済み。`TimePickerCellView.handleLocaleChanged()` は行表示と `datePicker.locale` だけを更新し、`datePicker.date` / `preSelectedDate` を書き換えない。`DatePickerCellView.handleLocaleChanged()` も行表示、Wheels の Locale、表示中 Calendar controller の Locale だけを更新し、Wheels / Calendar の date を書き換えない。Calendar controller の `applyLocale(_:)` も `datePicker.locale` だけを変更する。

回帰テストは production と同じ `NSLocale.currentLocaleDidChangeNotification` を post した後に次を確認している。

- Time: 未確定 23:45 を保持し、Done の `onValueChanged` も 23:45
- Date Wheels: 未確定 2026-02-20 を保持し、Done の `onValueChanged` も同日
- Date Calendar: 未確定 2026-02-20 を保持し、表示中 controller の Locale だけが `en` から `ja` へ変化

Calendar の Locale 通知テストは同一テスト内で Done まで押していないが、通知後の production controller の `_currentDate` が未確定日を保持する assertion、`handleDone()` がその `datePicker.date` を退避して `onDone` / `onDoneCompleted` へ渡す実装、production factory 配線で changed / completed の両 callback が同じ選択日を受ける既存テストを合わせて確認した。通知処理に date 書き換えが戻れば前者が落ち、Done 配線が壊れれば後者が落ちるため、要求する値保持には回帰検出力がある。

### 2. iOS production Locale 通知経路

解消済み。テスト seam は Locale resolver だけで、通知は production と同じ `NotificationCenter.default.post(name: NSLocale.currentLocaleDidChangeNotification, ...)` を使う。Time / Date の各 View は init で同 notification の observer を登録しており、テストは observer 経由で private handler を通る。

この経路で、Time / Date の自動 `valueText`、Time / Date picker Locale、明示 `valueText` の不変、`is24Hour == false`、Time / Date Wheels / Calendar の未確定値が検証される。observer 登録または handler を除去すれば、更新後の表示値 / picker Locale assertion が更新前の値のまま失敗する。full render を戻せば未確定値 assertion が失敗するため、`review-001.md` で不足していた経路を直接守っている。

### 3. iOS 実行時証跡と8枚の役割分担

解消済み。`evidence/ios-running-host-locale-update.md` は、同じ PID のまま launch argument を変えても実行中プロセスへ Locale 更新が配送されず、terminate / relaunch 後にのみ表示が変わった観測を記録している。その制約を隠さず、同一 Host の通知後遷移は production notification の Simulator test、OS Locale 下の UIKit 実描画は8枚の静止画が担うと区別している。

8枚を再度目視し、Android 4枚は 1080×2400、iOS 4枚は 1206×2622、SHA-256 は全て別であることを確認した。英語 / 日本語の Date / Time Picker と Time の自動 `valueText` が同じ Locale 表現になり、個人情報は見当たらない。Date Cell は既定の数値 format のため静止画だけでは Locale 差が見えないが、Date の自動 `valueText` は `MMMM` を使う iOS / Android の通知・再 bind テストが補完する。

### 4. Android の条件ベース待機

解消済み。Locale 再 bind テストは `awaitMainLooperCondition` で Time / Date の現在値が `10:15 午後` / `1月` へ揃う条件そのものを待つ。helper は実時間 deadline、対象への実行機会譲渡、期限超過時の観測値付き failure を備え、`test-execution.md` の3条件を満たす。

## 自分で実行した確認

- **iOS build**: iPhone 17 Pro / iOS 26.5 Simulator、`xcodebuild build -scheme KsSettingsView ...` — **BUILD SUCCEEDED**。
- **iOS 全テスト**: 同 Simulator、`xcodebuild test -scheme KsSettingsView ...` — **1,115 tests / 0 failures / 0 skipped**。内訳は Bridge 173 / Core 88 / SwiftUI 112 / TestSupport 7 / UI 735。
- **Swift 6 言語モード**: `xcodebuild build ... SWIFT_VERSION=6` — source 4 target の invocation に `-swift-version 6`、**BUILD SUCCEEDED / error 0**。
- **Android build / lint**: `./gradlew :kssettingsview:assembleDebug :kssettingsview:lintDebug` — **BUILD SUCCESSFUL**。
- **Android 全テスト**: `./gradlew test --rerun-tasks` — **2,994 tests / 0 failures / 0 errors / 0 skipped**。内訳は `kssettingsview` debug 1,320 + release 1,320、bridge debug 177 + release 177。
- **artifact lint**: `date-time-picker-locale/` の identity / local-path lint は exit 0。
- **既存決定**: accepted の `core/ADR-0028` を拘束根拠として照合。Locale 変更後も Picker の時制は `is24Hour` のみで決まり、`format` は自動 valueText の表示専用である。proposed の `core/ADR-0035` は判定根拠に使っていない。
- **境界**: 明示 `valueText`、Bridge の POSIX 輸送、MAUI、公開 API に要求外の変更はない。Android の Picker 内部は変更せず、自動 valueText の Locale 解決と表示中 Cell の再 bind に限定されている。

## 指摘事項

なし。

## アクションプラン

なし。

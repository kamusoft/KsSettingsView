# Exploration: date-time-picker-locale

## 課題 / 動機

日本語環境の Native iOS Sample で、`TimePickerCell` の 12 時間選択面が `AM / PM`、`DatePickerCell` のホイールが英語月名になり、Cell 右側の時刻表示も `10:15 PM` になる。Android と MAUI iOS では同じデモが日本語表記になるため、Native iOS も選択面と Cell 右側表示の両方で利用者のロケールを反映させたい。

追加確認では、Android の言語を英語へ切り替えると `TimePickerCell` の選択面は `AM / PM` に更新された一方、Cell 右側の時刻表示は `10:15 午後` のまま残った。Android も Cell 右側の自動生成値に Locale 変更が反映されない問題を持つ。

Native iOS と MAUI iOS は同じ UIKit Host / Renderer に収束し、MAUI Bridge は表示 Locale を渡していない。Native iOS Sample の Xcode project は対応言語を `en` / `Base` と宣言する一方、MAUI Sample の生成済み iOS manifest には同等の英語固定宣言が見当たらない。Apple の `Locale.current` は端末設定、アプリ単位の言語設定、アプリが提供する言語を考慮するため、Native と MAUI の差はアプリプロセスが解決した Locale の差で説明できる可能性がある。ただし両プロセスの実際の Locale 値は未計測であり、現時点では原因仮説である。

実装上、Cell 右側表示は `CachedDateFormatter` を使うが、キャッシュキーは format だけで Locale を含まない。DatePicker の Wheels / Calendar は Locale を明示していない。TimePicker は `HourCycleLocale` で `is24Hour` だけを差し替え、言語・地域は `Locale.current` に由来する。

Android も Cell 右側表示用の `DateTimeFormatter` を format だけでキャッシュする一方、picker 内部は開くたびに Host Context の有効 Locale を取得する。Locale-only の構成変更では既存 Cell を再バインドせず、Activity が再生成されても formatter cache はプロセス内に残るため、Cell 右側だけ旧 Locale の表記が残る。TimePickerCell と DatePickerCell は同型の cache を別々に持つ。

## 検討した選択肢 (却下案と理由を含む)

### OS で選択されたロケールを反映する (採用)

アプリバンドルがその言語の翻訳資源を宣言していなくても、OS が提供するアプリ単位の言語設定があればそれを優先し、なければ端末全体で選ばれた言語・地域を選択面と Cell 右側表示へ反映する。アプリ独自の言語設定は使わない。Android / MAUI iOS の観測結果と揃う一方、周辺 UI が英語のアプリでも picker だけ日本語になる可能性がある。ユーザーがこの結果を選択した。

### アプリが提供する言語の範囲でロケールを反映する (不採用)

Apple の `Locale.current` に従い、アプリが提供していない言語は development language へフォールバックする。プラットフォーム慣用のアプリ言語と揃う一方、Native Sample の修正は日本語 localization 宣言の追加が中心となり、Android / MAUI iOS の現在の見え方とは条件が異なる。

## 決定事項

- Native iOS は picker 内部と Cell 右側の自動生成値を修正対象とする。
- Android は Cell 右側の自動生成値を修正対象とし、既に Locale を反映する picker 内部は変更対象にしない。
- MAUI iOS は正常になる理由を比較調査するが、修正対象には含めない。
- 表示 Locale は、OS が提供するアプリ単位の言語設定があればそれを優先し、なければ利用者が端末全体で選んだ言語・地域を反映する。
- アプリ独自の言語設定は使わず、アプリが提供する localization の範囲だけにも制限しない。
- OS の Locale 変更が実行中の Host へ通知された場合は、picker 内部と表示中の Cell 右側値を同じ Locale へ更新する。
- 利用者が明示した `valueText` は自動変換しない。
- `TimePickerCell.is24Hour` は既存の core/ADR-0028 どおり時制の唯一の決定源とし、Locale には言語・地域だけを担わせる。
- Bridge の POSIX 輸送書式は表示用ではないため変更対象にしない。

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

作成済み: core/ADR-0035 (proposed)。表示 Locale の決定源が platform をまたぎ、将来の formatter・picker 実装を制約するため捕捉した。

## 未決の論点

- Native / MAUI 各プロセスで実際に解決される Locale と bundle localization を同一端末条件で計測し、原因仮説を確定する。
- DatePickerCell の既定 format は数値だけなので、Cell 右側の日付に対する Locale 回帰テスト用の format を別途選ぶ必要がある。
- iOS で OS 管理のアプリ単位言語設定を優先しつつ、バンドルの localization 提供範囲だけには制限されない Locale の組み立てを Simulator で実測する。
- Android の OS 管理アプリ単位 Locale が Host Context に反映されることを実機または Emulator で実測する。

## UI 素材 (ui/references/ の一覧と注釈)

- `ui/references/ios-time-picker-en.png`: Native iOS の問題例。picker 内部の `AM / PM` と Cell 右側の `10:15 PM` を対象とする。他の画面要素は対象外。
- `ui/references/ios-date-picker-en.png`: Native iOS の問題例。picker 内部の英語月名を対象とする。他の画面要素は対象外。
- `ui/references/android-time-picker-ja.png`: Android の期待例。picker 内部と Cell 右側の日本語表記を比較対象とする。
- `ui/references/android-date-picker-ja.png`: Android の期待例。年月日の日本語表記を比較対象とする。
- `ui/references/android-time-picker-en-stale-cell-ja.png`: Android の問題例。picker 内部は英語 `AM / PM` に更新されるが、Cell 右側は日本語 `午後` のまま残る差を対象とする。
- `ui/references/maui-ios-time-picker-ja.png`: MAUI iOS の正常例。Native と同じ UIKit Renderer で日本語になる比較証拠であり、MAUI 自体は修正対象外。
- `ui/references/maui-ios-date-picker-ja.png`: MAUI iOS の正常例。Native と同じ UIKit Renderer で日本語になる比較証拠であり、MAUI 自体は修正対象外。

画像内の氏名は架空のデモ名称であることをユーザー確認済み。

## 変更級の推奨: S (暫定)

日付・時刻の Locale 反映という単一能力のバグ修正で、公開 API 変更を避けられ、iOS / Android 内部の Locale 解決・formatter cache・再バインドと回帰テストに限定できるため。platform 横断の契約は proposed ADR に捕捉するが、アーキテクチャや公開 API は変えない。

## 調査根拠

- Cell 右側表示: `ios/Sources/KsSettingsViewUI/TimePickerCell.swift`、`ios/Sources/KsSettingsViewUI/DatePickerCell.swift`、`ios/Sources/KsSettingsViewUI/CachedDateFormatter.swift`
- TimePicker 内部表示: `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift`、`ios/Sources/KsSettingsViewUI/HourCycleLocale.swift`
- DatePicker 内部表示: `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift`、`ios/Sources/KsSettingsViewUI/DatePickerCalendarSheetController.swift`
- Native / MAUI 共通経路: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`、`ios/Sources/KsSettingsViewBridge/`、`maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs`
- Android の Cell 右側と picker 内部: `android/kssettingsview/src/main/kotlin/` 配下の `TimePickerCell` / `DatePickerCell` Renderer と selection sheet
- Native Sample の localization 宣言: `samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj`
- 時制の既存契約: `kasane/decisions/core/0028-timepickercell-is24hour-sole-hour-cycle-source.md`、`kasane/concepts/core/cells/time-picker-selection-surface.md`
- 実行時検証規約: `kasane/handbook/cross/runtime-behavior-verification.md`

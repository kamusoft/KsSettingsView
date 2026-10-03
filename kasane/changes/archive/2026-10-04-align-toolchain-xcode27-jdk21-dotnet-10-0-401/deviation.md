# Deviation: align-toolchain-xcode27-jdk21-dotnet-10-0-401

- 決定事項「CI の JDK 導入 (6 か所)」: 探索メモでは 6 か所と記述 → 実際は 7 か所 (`.github/workflows/release.yml` 3 + 検証 workflow 4) をすべて 21 にした。理由: 探索メモの数え違いで、意図は全箇所 (同メモの表は 7 を示す) (2026-10-04)
- 蒸留時に反映: handbook `kasane/handbook/cross/test-execution.md` — 2026-10-04 の実測件数 (Android 3174 件 = 1587 × 2 variant、MAUI facade 624 件、iOS 1201 件)
- 蒸留時に反映: concepts `kasane/concepts/android/architecture/build-toolchain.md` — CI の JDK の版はコンパイルに使う JDK の指定と揃える必要がある (自動取得を入れていないため) ことを「ツールチェーンを更新するとき」に足す
- 蒸留時に反映: concepts `kasane/concepts/maui/architecture/binding-build-integration.md` と `kasane/concepts/maui/api/maui-facade.md` — SDK 10.0.401 でもライブラリの既定 platform 版は iOS 26.0 / Android 36.0、最低 OS 版の SDK 既定は iOS 26.0 / Android 21 で、nupkg の TFM group が変わらないことを pack して実測した
- 決定事項「実装するもの」: 探索メモでは版とランナーの付け替えだけがスコープ → 指示により、iOS 27.0 の Simulator で毎回落ちる日付ホイールの Done のテスト (`ios/Tests/KsSettingsViewUITests/InputCellsTests.swift` の `test_DatePickerCell_ホイールのDoneは入力面の非表示完了を待って閉じ切りcallbackを届ける`) の原因調査と修正を本変更に含める。理由: 完了条件「iOS Native のテストが全件通る」と CI の iOS 本体検証を満たすため (2026-10-04)
- 蒸留時に反映: handbook `kasane/handbook/cross/test-execution.md` (iOS の節) — Xcode 27.0 / iOS 27.0 の、ホストアプリを持たないテストランナーでは入力面の通知が 1 件も届かない (実測。証跡は `evidence/ios27-keyboard-hide-notification.txt`)。通知の到着を UIKit 任せで待つテストは書けず、テストが通知を出して配線を検証する。あわせて、Xcode 27.0 はテスト失敗時に診断収集で約 10 分待つ
- 決定事項「実装するもの」: 探索メモでは CI の変更は版とランナーの付け替えだけ → 指示により、`.github/workflows/verify-ios.yml` のテスト実行に診断収集を止める指定 (`-collect-test-diagnostics never`) を足した。理由: Xcode 27.0 はテスト失敗時に診断収集で約 10 分待ち、job の上限 20 分に近づくため (2026-10-04)
- 蒸留時に反映: handbook `kasane/handbook/cross/test-execution.md` (iOS の節) — 手元の `xcodebuild test` でも、失敗を見込む実行 (改変して落ちることの確認など) には `-collect-test-diagnostics never` を付けると約 10 分の待ちを避けられる

# レビュー結果: ios-root-accessory-unexpected-accent-background (001 回目)

**日付**: 2026-09-22
**判定**: APPROVED

## サマリー

`UIBackgroundConfiguration.backgroundColor` の `nil` は view の tint 色を使い、透明を明示する値は `.clear` であるという UIKit の契約に対し、View 形式だけを `.clear` にする変更は合意済み S 級スコープと現行の外観契約を満たしている。text 形式の Theme 背景色は従来の分岐に残り、Section / Root と UIKit / SwiftUI、text → View の再構成を回帰テストが観測しており、変更前変異でも検出力を確認した。

Critical / Major / Minor / Suggestion の指摘はない。Sample / Bridge / Android の製品コード変更、および未記録の仕様逸脱はない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` — 常時適用
- `kasane/handbook/cross/test-execution.md` — iOS Simulator 全テストの実行と件数確認
- `kasane/handbook/cross/runtime-behavior-verification.md` — 修正前後の実環境 A/B と change 配下の証跡
- `kasane/handbook/cross/local-development-setup.md` — iOS / MAUI Sample の配備、および native 変更後の MAUI 再ビルド
- `kasane/handbook/ios/swift6-language-mode-check.md` — `ios/Sources/` 変更の Swift 6 言語モード確認

関連する現行契約・決定として `kasane/concepts/core/styling/list-appearance.md`、core/ADR-0004、core/ADR-0005、core/ADR-0009、core/ADR-0018、core/ADR-0032 を照合した。

## 確認結果

### UIKit の `nil` と `.clear`

Xcode 26.5 の iPhoneSimulator SDK に含まれる UIKit 公開ヘッダーを確認した。`UIBackgroundConfiguration.clear()` は既定スタイルを持たない clear configuration を返す一方、`backgroundColor` の `nil` は view の tint 色を使用し、無色透明には `clearColor` を使う契約である。したがって `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2373` の `.clear` は不具合原因へ直接対応し、単に「値を未指定へ戻す」実装ではない。

### 契約と回帰範囲

- text 形式は `accessoryText != nil` の分岐で `Theme.headerBackgroundColor` / `footerBackgroundColor` を引き続き設定する。既存の Section / Root 初期反映・Theme 差し替えテストも成功した。
- View 形式は同じ共通描画関数を通るため、Section / Root の双方へ修正が届く。テストは Section Header を UIKit、Section Footer を SwiftUI、Root Header を UIKit、Root Footer を SwiftUI で構築し、両 backing と4つの配置を覆う (`ios/Tests/KsSettingsViewUITests/AccessoryBackgroundColorTests.swift:288`, `ios/Tests/KsSettingsViewUITests/AccessoryBackgroundColorTests.swift:307`)。
- `assertTransparentBackground` は `nil` を即失敗とし、light / dark の双方で解決後 alpha が 0 になることを確認する (`ios/Tests/KsSettingsViewUITests/AccessoryBackgroundColorTests.swift:151`)。text → View の差し替えも、以前の Theme 色を残さず明示的な透明色へ収束することを確認する (`ios/Tests/KsSettingsViewUITests/AccessoryBackgroundColorTests.swift:445`)。
- UIKit / SwiftUI の分岐より前に背景構成を設定する実装なので、同じ slot で両 backing を重複試験しなくても背景分岐の経路は同一である。変更前変異が Section / Root の両テストで失敗することも実測した。

### 実行時証跡

- `evidence/before-native-dsl-root-footer.png` と `evidence/after-native-dsl-root-footer.png` を原寸確認し、同じ Native Sample 画面で Root Footer の system tint 背景だけが消え、caption と list 下地が正常に見えることを確認した。
- `evidence/reported-section-accessory-blue-1.png` と `evidence/reported-section-accessory-blue-2.png` では、修正前に Section Header / Footer でも同じ鮮青色が出ることを確認した。共通描画関数を直す範囲判断と一致する。
- `evidence/after-maui-root-header.png` と `evidence/after-maui-section-header-footer.png` では、利用者指定の淡青・淡緑背景を保ったまま、Root と Section の外側から system tint の帯が消えている。6枚とも通知・アカウント名・個人名などの個人情報がないことを原寸確認した。
- `evidence/README.md` は、MAUI 本体・iOS Binding・MAUI Sample の `bin` / `obj` を除いた後の `net10.0-ios` Debug ビルド成功 (警告0、エラー0) と、同じ iPhone 17 / iOS 26.5 Simulator への配備結果を記録している。古い native 成果物を見た可能性を排除できている。

## 実行した検証

- 対象クラス: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17' -only-testing:KsSettingsViewUITests/AccessoryBackgroundColorTests` — 11 tests / 0 failures。
- 変更前変異: 製品コードの `.clear` だけを一時的に `nil` へ戻して同じ11件を実行 — 3テスト、計6アサーションが失敗。Section Header / Footer、Root Header / Footer、text → View の各回帰を検出した。直後に復元し、製品ファイルとテストファイルの SHA-256 が変異前と一致することを確認した。
- iOS 全件: 絞り込みなしの同 scheme を iPhone 17 / iOS 26.5 Simulator で実行 — 1116 tests / 0 failures / 0 skipped (`xcresult` 集計)。
- Swift 6: `ios/Package.swift` へ `swiftLanguageVersions: [.version("6")]` を一時設定して同 Simulator 向けに package 全体を build — 成功、error 0。一時設定を戻し、`Package.swift` の SHA-256 が変更前と一致することを確認した。
- MAUI: `dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj -f net10.0-ios -c Debug --no-incremental` — 成功、警告0、エラー0。

## 指摘事項

なし。

## アクションプラン

なし。この実装と証跡のまま次のゲートへ進められる。

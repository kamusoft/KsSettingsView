# Tasks: ios-swiftui-representable-safe-area

## 0. spike (実装の最初に行う)
- [x] 0.1 `KsSettingsView.body` の内側で Representable に `.ignoresSafeArea(.container)` を掛けた状態を Sample で表示し、利用側で掛けた場合 (`references/ios26-spike-safearea-ignore-all-*.png`) と同じ配置になることを Simulator で確認する。同じにならなければ実装を止めて orchestrator / ユーザーへ上げる (→ Requirement: SwiftUI ラッパの既定配置は親の全面)

## 1. SwiftUI ラッパ (ios/Sources/KsSettingsViewSwiftUI)
- [x] 1.1 `KsSettingsView` にセーフエリア尊重の内部状態を足し、既定は全面配置 (container 全辺を無視、keyboard 領域は残す) とする。Store 方式・DSL 方式の両バックエンドに同じ配置を適用する (→ Requirement: SwiftUI ラッパの既定配置は親の全面)
- [x] 1.2 Root modifier `respectsSafeArea(_ respects: Bool = true)` を追加する。copy を返し、他の Root modifier と連鎖順序に依存しない (→ Requirement: セーフエリアを尊重する側へ戻す Root modifier)
- [x] 1.3 テスト: `ios/Tests/KsSettingsViewSwiftUITests` に、modifier の既定値・`false` で既定に戻ること・他の Root modifier との連鎖で互いの値を失わないこと・元の値が不変であることを追加する (→ Scenario: false を渡すと既定の全面配置になる / 他の Root modifier と連鎖しても互いの値を失わない) (`RootSafeAreaModifierTests` 7 件 / 配置の実測は `RootSafeAreaLayoutTests` 7 件 — `UIHostingController` + `UIWindow` で既定が全面に広がり `.respectsSafeArea()` で内側に縮むことを frame で検証)

## 2. iOS Sample (samples/ios/KsSettingsViewSample)
- [x] 2.1 7 画面 (BasicCells / InputCells / CustomCell / DSLDemo / StoreDemo / MinimalDiffable / SectionDecorationSpike) の `.ignoresSafeArea(.container, edges: .bottom)` を取り除く。文言・Section・Cell 構成は変えない (→ Requirement: iOS Sample のデモ画面はラッパの既定配置で bar の後ろまで回り込む)

## 3. 実行時挙動の確認と証跡 (handbook cross/runtime-behavior-verification.md)
- [x] 3.1 iOS 26 Simulator で Sample の DSL 方式画面と Store 方式画面を先頭表示・末尾スクロールで撮り、`ui/verification/` に保存して references と照合する。大タイトルの畳み込みも確認する (→ Scenario: NavigationStack の中身として全画面で使うと一覧が画面下端まで描かれる / iOS 26 の大タイトルがスクロールで畳まれる / Store 方式と DSL 方式で配置が同じ)
- [x] 3.2 iOS 18 Simulator で同じ 2 画面を撮り、帯が出ないこと・先頭 / 末尾 Cell の inset が正しいことを確認する (→ 同上)
- [x] 3.3 InputCells 画面で下端付近の EntryCell をタップし、キーボード表示中に編集中の Cell が隠れないことを撮って残す (→ Scenario: キーボード表示中は入力 Cell がキーボードに隠れない)
- [x] 3.4 Sample の 1 画面に一時的に `.respectsSafeArea()` を付けて表示し、本変更前の配置 (baseline の references) に戻ることを撮って残す。確認後に一時変更を戻す (→ Scenario: modifier を付けると従来どおりセーフエリアの内側に収まる)
- [x] 3.5 brief.md の「承認モック」節に照合結果 (画像名・確認日・合意済み妥協) を記録する

## 4. 完了判定
- [x] 4.1 `ios/Tests` 全体を実行し green を確認する (handbook cross/test-execution.md)
- [x] 4.2 Swift 6 言語モードの一時ビルドで error 0 件を確認し、`ios/Package.swift` の差分が無いことを確認する (handbook ios/swift6-language-mode-check.md)
- [x] 4.3 実装コードの決定箇所に `ios/ADR-0006` の参照コメントを残す (蒸留時の昇格ゲート用)

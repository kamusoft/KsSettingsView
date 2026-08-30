# Exploration: entrycell-keyboard-avoidance-check

## 課題 / 動機

EntryCell が画面下半分にある状態でフォーカスしたとき、キーボードに合わせてコンテンツがせり上がるか（キーボード回避）を確認したい。しかし現在の「入力Cell 5種」デモは EntryCell セクションが先頭（セクション1）にあり、スクロール初期位置＝先頭のため EntryCell を画面下半分に置いた状態を作れず、この挙動を検証できない。3 OS（iOS / Android / MAUI）のサンプルすべてが同じ構成。

- iOS: `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift`
- Android: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt`
- MAUI: `samples/maui/KsSettingsView.Sample.Maui/ViewModels/InputCellsDemoViewModel.cs`

補足の観察: ライブラリ側にキーボード回避の明示実装は見当たらない。iOS は `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の `keyboardDismissMode = .onDrag` のみで、keyboardLayoutGuide / inset 調整コードはなし。Android サンプルにも `imePadding` / `adjustResize` 指定なし。検証で「せり上がらない」が判明した場合はライブラリ本体の修正（別 change）に発展する可能性がある。

## 検討した選択肢 (却下案と理由を含む)

- **案A: 既存デモ末尾に検証用 EntryCell セクションを追加**（採用）— デモは既に7セクションで縦に長く、末尾に足せば「下までスクロールしてフォーカス」で下半分配置を自然に再現できる。3 OS ともサンプルのみの変更で最小手数。
- 案B: キーボード回避専用のデモ画面を新設 — ダミー Cell で任意位置に置けて確実性は最も高いが、画面・ナビ登録×3 OS の手間が大きい。却下（現時点の目的には過剰）。
- 案C: 既存デモのセクション並び替え — 5種の提示順が崩れ、端末高さ次第で下半分に来る保証もない。却下。

## 決定事項

- 案Aを採用（ユーザー確定）。既存「入力Cell 5種」デモの末尾に検証用 EntryCell セクションを 3 OS 分追加する。
- ADR は不要（デモ構成の小変更で覆すコストが低い）。
- 検証用セクションは恒久デモとして残す（オーナー確定 2026-08-24）。以後 3 OS 間の文言・構成一致 (sample-parity) の維持対象。

## ADR 候補 (作成済み: なし / 未起票: なし)

なし。

## 検証結果 (2026-08-24)

4 環境 (iOS / Android / MAUI iOS / MAUI Android) すべてでキーボード回避 (フォーカス時のせり上がり) が**機能することを確認**した。証跡は `evidence/` (索引: `evidence/evidence.md`)。

- iOS (Simulator iPhone 17 / iOS 26.5): メモ・署名ともフォーカスでコンテンツがせり上がり、対象 Cell がキーボード上端より上に収まる
- Android (実機 Pixel 系): メモ・署名とも IME 直上にせり上がる
- MAUI iOS ターゲット (同シミュレータ): メモのフォーカスでせり上がりを確認
- MAUI Android ターゲット (同実機): メモのフォーカスで IME 直上にせり上がる。`Platforms/Android/MainActivity.cs` の `[Activity]` に `WindowSoftInputMode` 指定が無い構成のままで動作する
- ライブラリ側に明示のキーボード回避コードは無いが、各プラットフォームの標準機構で成立している。挙動は OS で異なる: iOS はコンテンツのスクロール調整 (画面上部の行は残る)、Android / MAUI Android は window ごと押し上げる pan 系の見え方 (タイトルと「最後のイベント」行が画面外に出る)。いずれもフォーカスした Cell はキーボード直上に収まる

検証手順のメモ: 判定は「フォーカスのみ」で行う (打鍵すると tracked ラッパーが 1 文字ごとに lastEvent を更新し再構成が走るため、観察が汚れる)。iOS Simulator はハードウェアキーボード接続だとソフトウェアキーボードが出ないため、I/O 設定で無効化してから確認する。

## 未決の論点

- (解消) キーボード回避が機能しない場合のライブラリ側対応 → 検証の結果、4 環境とも機能しており不要。
- (解消) 検証用セクションの寿命 → 恒久デモとして残す (オーナー確定 2026-08-24)。決定事項へ記載済み。

## UI 素材 (ui/references/ の一覧と注釈)

なし。検証証跡は evidence/ に保存。

## 変更級の推奨: S (理由)

サンプルのみの変更（3 OS）、公開 API 変更なし、可逆、既存デモ様式（tracked ラッパー・直近イベント表示）を踏襲するだけのため。

# Delta Spec: ios-host — Swift 6 concurrency 適合

## ADDED Requirements

### Requirement: Swift 6 言語モードでのビルド適合

iOS パッケージの source ターゲット群 (Core / UI / SwiftUI / Bridge) は、Swift 6 言語モードでビルドしたときに concurrency エラーゼロでビルドできる SHALL。恒久の言語モード切替は本要件の範囲外であり、切替可能な状態を保つことが契約である。継続的な担保は CI ではなく handbook 規約 (iOS ソースを触る変更の完了判定前に Swift 6 一時設定ビルドで確認する手順。蒸留で追加 — proposal.md What Changes (4)) が担う。

#### Scenario: Swift 6 言語モードでのビルド試行がエラーゼロで成功する

- **GIVEN** `ios/Package.swift` に `swiftLanguageVersions: [.version("6")]` を一時設定する
- **WHEN** パッケージ全体スキームを iOS Simulator 向けにビルドする
- **THEN** source 4 ターゲットすべてがエラーゼロでビルド成功する (検証後、一時設定は元に戻す)

### Requirement: 行タップ通知とタッチフィードバックの挙動維持

concurrency 適合の書き換え後も、行タップ時の通知ディスパッチと、押下中のハイライト表示の挙動は変わらない SHALL。行タップ通知の対象は既存の準拠 11 種 (Command / Button / Checkbox / Radio / SimpleCheck / Picker / NumberPicker / TimePicker / DatePicker / Entry / Custom の各 CellView) の全部である SHALL。

#### Scenario: 準拠 11 種すべてが行タップ通知の解決対象であり続ける

- **GIVEN** 上記 11 種の CellView を生成する
- **WHEN** 行タップ通知プロトコル (`TapNotifyingRenderer`) として解決を試みる
- **THEN** 11 種すべてが解決できる (準拠の欠落がない)

#### Scenario: 行タップで該当 CellView のハンドラが発火する

- **GIVEN** タップ通知対応の Cell を表示している
- **WHEN** その行をタップする
- **THEN** 当該 CellView に設定されたタップハンドラが呼び出される

#### Scenario: 押下中はハイライト色になり離すと平常時の背景に戻る

- **GIVEN** 有効 (enabled) な Cell を表示している
- **WHEN** その行を押下し、その後離す
- **THEN** 押下中は選択ハイライト色が背景に適用され、離すと平常時の実効背景色に戻る

### Requirement: Controller 解放の維持

deinit の解放処理を Swift 6 適合の形に再設計した後も、`KsSettingsViewController` は参照を手放したときに解放され、メモリリークしない SHALL。長命の Store と接続していた場合、Controller の解放後も Store は購読残骸の影響なく使い続けられる SHALL。

#### Scenario: 参照を手放すと Controller が解放される

- **GIVEN** Controller を生成して view をロード済みである
- **WHEN** Controller への参照をすべて手放す
- **THEN** Controller は解放される (weak 参照が nil になる)

#### Scenario: Store 経由でも Controller が解放され Store は使い続けられる

- **GIVEN** 長命の Store に接続した Controller が Diff 配信を受けた状態である
- **WHEN** Controller への参照をすべて手放す
- **THEN** Controller は解放され、その後も Store への操作は正常に行える

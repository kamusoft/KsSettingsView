# maui-bridge デルタスペック

Bridge interop 境界 (iOS `KsSettingsViewBridge` / Android `ks-settingsview-bridge` と C# binding/gateway) の拡張。既存の Bridge 契約 (内部所有 Store・12メソッド・ID 採番・lifecycle) はコードとテストが正で、ここでは本変更の追加分のみを規定する。

## ADDED Requirements

### Requirement: per-type Cell DTO の輸送

Bridge は 11 Cell 種 (Command / Button / Switch / Checkbox / Radio / SimpleCheck / Entry / Picker / NumberPicker / TimePicker / DatePicker) それぞれの DTO を両OSに公開し、受け取った DTO を native Store の対応する Cell 型へ変換して既存の Store 操作経路に載せなければならない (SHALL)。DTO は Cell の状態フィールド (スタイル上書き・icon の platform 画像を含む) を輸送し、単一の統合 DTO で複数 Cell 種を表現してはならない (SHALL NOT)。全 Cell DTO は共通基底型 (LabelCell 含む) の派生であり、`KsBridgeSection` の cells・cell 単位の更新操作 (`replaceCell` / `replaceCells` 等)・Root 構築は基底型で異種 Cell を混載できなければならない (SHALL)。

#### Scenario: 各 Cell 種の変換

- **GIVEN** 11種いずれかの Cell DTO を構成した Root
- **WHEN** `setRoot` で Bridge へ渡す
- **THEN** native Store に対応する Cell 型の値が格納され、表示に反映される

#### Scenario: 異種 Cell の混載

- **GIVEN** 1つの Section に Label / Switch / Entry / DatePicker の DTO を混載した Root
- **WHEN** `setRoot` で渡し、その後 `replaceCells` で異種 Cell を同一バッチ更新する
- **THEN** すべての Cell が対応する native Cell 型として表示・更新される

### Requirement: 単一 interaction delegate / listener

Bridge は Bridge instance あたり1個の操作通知チャネル (iOS: `@objc` protocol の delegate、Android: listener interface) を公開しなければならない (SHALL)。ユーザー操作は Cell 種別ごとのメソッド (tapped 2種・二値変更3種・radio 選択・entry テキスト変更・picker 単一/複数選択・number/time/date 変更) で、cellId と新値を引数に通知されなければならない (SHALL)。通知は native UI スレッド上で同期に呼ばれなければならない (SHALL)。delegate/listener 未設定時・解除後は通知を安全に破棄しなければならない (SHALL)。delegate/listener の保持が SettingsView (facade) の回収を妨げてはならない (SHALL NOT)。

#### Scenario: スイッチ操作の通知

- **GIVEN** delegate/listener を設定した Bridge で表示中の Switch Cell
- **WHEN** ユーザーがスイッチをトグルする
- **THEN** 該当 cellId と新しい二値を引数にスイッチ変更メソッドが1回呼ばれる

#### Scenario: 未設定時は破棄

- **GIVEN** delegate/listener 未設定の Bridge で表示中の対話 Cell
- **WHEN** ユーザーが操作する
- **THEN** 例外・クラッシュなく通知が破棄される

### Requirement: 値の輸送表現

interop 境界の値表現は次に従わなければならない (SHALL): 二値は Bool、数値・選択 index は Int (複数選択は昇順・重複除去に正規化した Int 配列 — 通知方向も同じ正規化)、文字列は String、時刻は "HH:mm"、日付は "yyyy-MM-dd" の ISO-8601 文字列 (タイムゾーンを含まない壁時計値・culture 非依存の固定書式)、keyboard 種別は正規化 enum の Int。Bridge は ISO 文字列を native の時刻・日付型 (iOS `Date`、Android `LocalTime` / `LocalDate`) へ、keyboard enum を native の keyboard 型へ変換しなければならない (SHALL)。対応の取れない keyboard / enum 値は native 既定へ fallback し、解釈できない時刻・日付文字列は操作の種類によらず該当フィールドを型の既定値 (時刻 00:00 / 日付 1970-01-01) で構築して例外を発生させてはならない (SHALL NOT)。この結果は両OSで同一でなければならない (SHALL)。範囲外の選択 index は正規化せず透過しなければならない (SHALL)。

#### Scenario: 不正な日付文字列の無害化

- **GIVEN** 解釈できない日付文字列を設定した DatePicker DTO
- **WHEN** `setRoot` / `insertCell` / `replaceCell` のいずれかで Bridge へ渡す
- **THEN** 例外・クラッシュなく日付フィールドは既定値 (1970-01-01) で構築され、他のフィールドは反映される (両OSで同一結果)

#### Scenario: 複数選択の順序違いは同値

- **GIVEN** `selectedIndices = [2, 0]` で表示中の Picker Cell
- **WHEN** 同じ集合を [0, 2] の順で持つ DTO を `replaceCell` で渡す
- **THEN** wire 上は正規化された同一表現になり、native Store への冗長な再適用や通知ループが発生しない

#### Scenario: 日付の往復

- **GIVEN** "2026-08-10" を Date DTO に設定した DatePicker Cell
- **WHEN** 表示してユーザーが日付を選択・確定する
- **THEN** native には正しい日付が表示され、通知には確定日付の "yyyy-MM-dd" 文字列が載る

### Requirement: DatePickerUIStyle の enum 輸送

DatePicker DTO の uiStyle は統一 enum 値 (Calendar / Wheels、未指定は欠落) で輸送され、Bridge が iOS `.calendar` / `.wheels`、Android `Material` / `Spinner` へ変換しなければならない (SHALL)。未指定時は native 既定の uiStyle を使わなければならない (SHALL)。

#### Scenario: 未指定は native 既定

- **GIVEN** uiStyle 未指定の DatePicker DTO
- **WHEN** 表示して行をタップする
- **THEN** 各OSの既定形式の選択面が開く

### Requirement: KsBridgeSection の isVisible 輸送

`KsBridgeSection` は isVisible (既定 true) を輸送し、native Section の isVisible へ反映しなければならない (SHALL)。

#### Scenario: 非表示 Section の輸送

- **GIVEN** isVisible = false の KsBridgeSection を含む Root
- **WHEN** `setRoot` で Bridge へ渡す
- **THEN** native の visible projection から該当 Section が除外されて表示される

### Requirement: replaceSection の cellId 温存 (同一 Section の差し替えに限る)

同一の facade Section インスタンスに起因する `replaceSection` (isVisible 変更・header / footer 等の内容差し替え) では、配下 Cell の採番済み cellId が温存され、差し替え後のユーザー操作通知は従前と同じ cellId で届かなければならない (SHALL)。別の Section インスタンスへの置換 (コレクションの Replace) は新規 Section として扱い、cellId を再採番してよい。

#### Scenario: isVisible 差し替え後の通知 ID

- **GIVEN** 表示中の Section を isVisible だけ変えて `replaceSection` で差し替えた
- **WHEN** 配下の対話 Cell をユーザーが操作する
- **THEN** 差し替え前と同じ cellId で通知が届く

#### Scenario: 別インスタンスへの置換は新規扱い

- **GIVEN** 表示中の Section をコレクション操作で別の Section インスタンスに置換した
- **WHEN** 新 Section 配下の対話 Cell をユーザーが操作する
- **THEN** 新しく採番された cellId で通知が届き、書き戻しは新 Section の Cell に適用される

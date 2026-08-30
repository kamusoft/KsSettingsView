# Delta: maui-core (add-maui-modern-style)

## ADDED Requirements

### Requirement: ListStyle の公開

facade は統一 enum `SettingsViewStyle { Classic, Modern }` を新設し、`SettingsView` は BindableProperty `ListStyle` (`SettingsViewStyle`、非 nullable、既定 `Classic`) を公開する (SHALL)。プロパティ名に `Style` を使用しない (`VisualElement.Style` との衝突回避、maui/ADR-0023)。style の意味論 (装飾・separator 規則の切替、設定内容と identity の不変) は Native の契約 (concepts styling/list-appearance) に従い、facade は値の伝搬のみを行う (SHALL)。定義外の enum 値 (未定義序数のキャスト) を設定した場合の解決は maui-bridge の未知序数正規化 (Classic へ) に委ねる。

#### Scenario: 既定値では現行挙動と一致する
- **GIVEN** `ListStyle` を指定しない SettingsView
- **WHEN** SettingsView を表示する
- **THEN** Classic style で表示され、従来と同じ見え方になる

#### Scenario: Modern 指定が native へ伝わる
- **GIVEN** `ListStyle="Modern"` を指定した SettingsView
- **WHEN** SettingsView を表示する
- **THEN** native へ Modern が配信され、Modern style の Section 装飾で表示される

#### Scenario: 実行時の切替が反映される
- **GIVEN** 表示中の SettingsView (Classic)
- **WHEN** `ListStyle = Modern` に変更する
- **THEN** native へ配信され Modern の装飾で再描画される。設定内容と Cell の値は変化しない (逆方向の切替も同様)

#### Scenario: 切替が gateway へ伝わる (net10.0 ユニットテスト)
- **GIVEN** fake gateway を注入した SettingsView
- **WHEN** `ListStyle = Modern` に変更する
- **THEN** gateway は style 設定操作を受け取り、その値は Modern である

### Requirement: Theme の Section 装飾4属性の公開

`SettingsView` は Theme 項目として `SectionMargin` (`Thickness?`)・`SectionCornerRadius` (`double?`)・`SectionBorderWidth` (`double?`)・`SectionBorderColor` (`Color?`) の BindableProperty を公開する (SHALL)。いずれも既定は null で、null は「未指定 = style 別の platform 既定へ委譲」を表す (SHALL)。facade は platform 既定値の定数を持たず、値の検証 (負値の正規化・radius の clamp・例外送出) を行わない — 正規化は Native の描画時正規化に委譲する (SHALL)。

#### Scenario: 未指定では platform 既定で描画される
- **GIVEN** 4属性を指定しない SettingsView (`ListStyle="Modern"`)
- **WHEN** SettingsView を表示する
- **THEN** Modern の platform 既定の装飾で表示される (facade から native へは未指定として伝わる)

#### Scenario: 指定値が Theme として native へ伝わる
- **GIVEN** 4属性を指定した SettingsView
- **WHEN** SettingsView を表示する
- **THEN** 指定した4属性が Theme の一部として native へ配信され、装飾へ反映される

#### Scenario: 実行時の属性変更が反映される
- **GIVEN** 表示中の SettingsView
- **WHEN** 4属性のいずれかを変更する
- **THEN** Theme 更新として native へ配信され、装飾へ反映される

#### Scenario: 範囲外の値でも例外を投げず素通しする (net10.0 ユニットテスト)
- **GIVEN** fake gateway を注入した SettingsView
- **WHEN** 負の成分を含む `SectionMargin` と過大な `SectionCornerRadius` を設定する
- **THEN** 例外は発生せず、指定値がそのまま gateway の Theme 更新へ含まれる (正規化は native の責務)

#### Scenario: 非有限数も例外を投げず素通しする (net10.0 ユニットテスト)
- **GIVEN** fake gateway を注入した SettingsView
- **WHEN** `NaN` / `±Infinity` の成分を含む `SectionMargin`・`SectionCornerRadius`・`SectionBorderWidth` を設定する
- **THEN** 例外は発生せず、指定値がそのまま gateway の Theme 更新へ含まれる (非有限の 0 正規化は native の描画時正規化の責務 — settings-view-ios-ui / settings-view-android-ui のデルタ)

### Requirement: SectionMargin の論理方向解釈

`SectionMargin` の `Left` / `Right` 成分は leading / trailing (論理方向) として解釈し、native の方向対応型へ位置のまま写す (SHALL)。RTL 時の左右反転は native の解決機構に委ね、facade は `FlowDirection` を監視・変換しない (SHALL NOT)。Classic style では Native 契約により上下成分のみが適用され左右成分は無視されるが、facade は style によらず全成分をそのまま伝搬する (SHALL)。

#### Scenario: Left / Right が leading / trailing として輸送される (net10.0 ユニットテスト)
- **GIVEN** fake gateway を注入した SettingsView
- **WHEN** `Left` と `Right` に異なる値を持つ `SectionMargin` を設定する
- **THEN** gateway の Theme 更新では `Left` の値が leading 成分、`Right` の値が trailing 成分として現れる

#### Scenario: Classic でも全成分が伝搬される (net10.0 ユニットテスト)
- **GIVEN** fake gateway を注入した SettingsView (`ListStyle` は既定の Classic)
- **WHEN** 左右成分を持つ `SectionMargin` を設定する
- **THEN** gateway の Theme 更新には左右成分もそのまま含まれる (Classic での左右無視は native の適用側の契約)

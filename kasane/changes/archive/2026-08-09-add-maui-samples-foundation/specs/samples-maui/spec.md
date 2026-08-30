# samples-maui デルタスペック

## ADDED Requirements

### Requirement: サンプルアプリの成立
`samples/maui/` のサンプルアプリ `KsSettingsView.Sample.Maui` は、`KsSettingsView.Maui` への `<ProjectReference>` 1本 (Binding は推移参照) で net10.0-ios / net10.0-android の両ターゲットにおいてビルド・起動できる SHALL。

#### Scenario: 両 OS でビルドできる
- **GIVEN** リポジトリを取得し .NET 10 SDK と両 OS の workload が入った環境
- **WHEN** `net10.0-ios` / `net10.0-android` それぞれをターゲットにビルドする
- **THEN** 追加の手順なしにビルドが成功する

#### Scenario: シミュレータ / エミュレータで起動できる
- **GIVEN** ビルド済みのサンプルアプリ
- **WHEN** iOS シミュレータ / Android エミュレータで起動する
- **THEN** クラッシュせずデモ一覧ページが表示される

### Requirement: デモ一覧と画面遷移
サンプルアプリはデモ一覧ページを入口とし、一覧の項目からページへ遷移して戻れる SHALL。一覧の項目文言と遷移先画面のタイトルは一元定義を参照し、同一文字列である SHALL。sample-parity のデモ画面と検証画面 (デモ集合に数えない platform 固有画面) は一覧上で区別できる SHALL。

#### Scenario: 項目選択で遷移しタイトルが一致する
- **GIVEN** デモ一覧ページ
- **WHEN** 項目を選択する
- **THEN** 該当ページへ遷移し、遷移先のタイトルは一覧の項目文言と同一文字列である。戻る操作で一覧へ戻れる

#### Scenario: 検証画面はデモと区別される
- **GIVEN** デモ一覧ページ
- **WHEN** 一覧を見る
- **THEN** 検証枠の画面 (LabelCell 検証) はデモ画面と別区分であることが表記から分かる

### Requirement: LabelCell 検証ページ
検証ページは `SettingsView` により、Header / Footer 文言を設定した 1 Section と 3 行の `LabelCell` を native 描画で表示する SHALL。3 行は `Title` と `ValueText` を全行に設定し、`Description` と `HintText` をそれぞれ少なくとも1行に設定して、LabelCell の表示フィールドの疎通を目視で判定できるようにする SHALL。表示値の少なくとも1つは ViewModel からのバインディングで供給し、値の変更が表示へ反映されることを確認できる SHALL。この画面は sample-parity (cross/ADR-0016) の検証枠であり、基本 Cell デモページの追加 (phase-4) をもって削除される暫定画面である。

#### Scenario: Section と LabelCell が表示される
- **GIVEN** デモ一覧から LabelCell 検証ページへ遷移した状態
- **WHEN** 画面を表示する
- **THEN** Section の Header / Footer 文言と 3 行の LabelCell が表示され、設定した Title / ValueText (全行)・Description / HintText (各1行以上) の文言が読める

#### Scenario: 値の変更が表示へ反映される
- **GIVEN** LabelCell 検証ページを表示した状態
- **WHEN** 画面上の更新操作で ViewModel のプロパティ値を変更する
- **THEN** バインディングされた LabelCell の表示文言が新しい値に更新される

### Requirement: クイックスタート README
`samples/maui/README.md` は placeholder ではなく、必要環境・開き方・両 OS での実行手順 (CLI コマンド含む)・依存関係の説明を記載する SHALL。

#### Scenario: README だけで実行に到達できる
- **GIVEN** リポジトリを取得した開発者
- **WHEN** `samples/maui/README.md` に記載された CLI コマンドをそのまま実行する
- **THEN** 追加の調査なしに両 OS でサンプルの起動へ到達できる

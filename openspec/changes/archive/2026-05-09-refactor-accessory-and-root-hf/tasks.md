## 1. settings-view-core: 型追加・変更（iOS / Swift）

- [x] 1.1 `KsSettingsViewCore`（Swift）に `KsAnyView` 構造体（または非 Hashable class）を追加し、内部 backing として `swiftUI(() -> AnyView)` / `uiKit(() -> UIView)` の二択を保持する
- [x] 1.2 `KsAnyView.swiftUI<V: SwiftUI.View>(@ViewBuilder _ build: @escaping () -> V)` ファクトリ API を実装する
- [x] 1.3 `KsAnyView.uiKit(_ factory: @escaping () -> UIView)` ファクトリ API を実装する
- [x] 1.4 `KsAnyView` が `Hashable` / `Equatable` を継承していないことを型レベルで保証する（テストで参照しないことを確認）
- [x] 1.5 `RootAccessory` enum を追加（`public enum RootAccessory: Hashable`）し、`case text(String)` / `case view(KsAnyView)` の 2 ケースを定義する。`Hashable` は手動実装で、`view` ケースの中身は判定対象外（ケース判別のみ）
- [x] 1.6 `SectionAccessory` の `.custom(AnyCell)` ケースを削除し、`.view(KsAnyView)` ケースに置き換える。`Hashable` 実装も `view` ケースの中身を判定対象外とする手動実装に変更
- [x] 1.7 `SettingsRoot` に `header: RootAccessory?` / `footer: RootAccessory?` を追加し、デフォルト値 `nil` を設定する
- [x] 1.8 `SettingsRoot` の `Hashable` 実装を更新し、`header` / `footer` を「`nil` / 非 `nil` の存在判定 + `text` ケース内容のみ」で hash/equal に含める。`view` ケースの `KsAnyView` 中身は判定対象外
- [x] 1.9 `Section` の `Hashable` 実装も同様に、`SectionAccessory.view` ケースの中身を判定対象外にするよう手動実装に変更する
- [x] 1.10 旧 `AnyCell` 型消去ラッパの定義を削除する（`Section.cells` の格納先は当面 `[any KsCell]` 等で代用。後続提案 `add-cell-types-custom` で再設計される）

## 2. settings-view-core: 型追加・変更（Android / Kotlin）

- [x] 2.1 `ks-settingsview-core`（Kotlin）に `sealed interface KsAnyView` を追加し、`class Compose(val content: @Composable () -> Unit) : KsAnyView` / `class AndroidView(val factory: (Context) -> View) : KsAnyView` のサブタイプを定義する
- [x] 2.2 `KsAnyView` に `equals` / `hashCode` を独自実装しない（`Any` のデフォルト＝参照同一性のまま）
- [x] 2.3 `RootAccessory` sealed interface を追加し、`data class Text(val value: String)` / `class View(val view: KsAnyView)` を定義する。`View` サブタイプは `equals` / `hashCode` を手動実装し、`view: KsAnyView` の中身を判定対象外（クラス一致のみで等価）とする
- [x] 2.4 `SectionAccessory` の `Custom(Cell)` サブタイプを削除し、`class View(val view: KsAnyView)` サブタイプに置き換える。`equals` / `hashCode` も同様に手動実装でクラス一致のみで等価とする
- [x] 2.5 `SettingsRoot` に `val header: RootAccessory?` / `val footer: RootAccessory?` を追加し、デフォルト値 `null` を設定する
- [x] 2.6 `SettingsRoot` の `equals` / `hashCode` を手動実装し、`header` / `footer` の `View` ケース中身を判定対象外とする（存在の有無 + `Text` ケース内容のみ）
- [x] 2.7 `Section` の `equals` / `hashCode` も同様に `SectionAccessory.View` ケース中身を判定対象外とする
- [x] 2.8 `Section.cells` の格納型は `List<Cell>` を維持する（`AnyCell` 概念は元々 Kotlin にないため変更不要）

## 3. settings-view-core: ユニットテスト

- [x] 3.1 Swift: `RootAccessory` の text/view ケース構築テストを追加する
- [x] 3.2 Swift: `SettingsRoot` の Hashable 契約テスト（H/F なし同士、text 同士、view 同士は中身無視で等価、nil/非 nil は不等）を追加する
- [x] 3.3 Swift: `SectionAccessory.view` ケースの Hashable 契約テスト（中身無視で等価）を追加する
- [x] 3.4 Swift: `KsAnyView` が `Hashable` / `Equatable` を継承していないことのコンパイルテスト（型レベル）
- [x] 3.5 Kotlin: `RootAccessory` の Text/View サブタイプ構築テストを追加する
- [x] 3.6 Kotlin: `SettingsRoot` の equals/hashCode 契約テスト（H/F なし同士、Text 同士、View 同士は中身無視で等価、null/非 null は不等）を追加する
- [x] 3.7 Kotlin: `SectionAccessory.View` ケースの equals/hashCode 契約テスト（中身無視で等価）を追加する
- [x] 3.8 Kotlin: `KsAnyView` の `equals` / `hashCode` が独自実装されていない（`Any` デフォルト動作）ことを検証するテスト

## 4. 検証

- [x] 4.1 `openspec validate refactor-accessory-and-root-hf --strict` を実行し、エラーがないことを確認する
- [x] 4.2 in-progress 提案 `add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-maui-bindings` / `add-cell-types-custom` の `openspec validate --strict` がそれぞれ成功することを確認する（本提案の探索段階で各アーティファクトを書き換え済み）

## 依存関係

- 本提案は `settings-view-core`（archive 済）の MODIFIED delta を含み、archive 済 spec を変更する。
- in-progress 提案 `add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-maui-bindings` / `add-cell-types-custom` のアーティファクトは本提案の探索段階で既に書き換え済みである。マージ順序として本提案を先行させる。
- 並列開発可能: `add-cell-types-basic` / `add-cell-types-input`（影響なし）。

## 完了条件

- 上記 1〜4 のチェックボックスがすべて完了していること。
- 本提案の `openspec validate` が strict モードで成功すること。
- in-progress 4 提案（ios-ui / android-ui / maui-bindings / cell-types-custom）の `openspec validate` が strict モードで成功すること。
- iOS / Android 両プラットフォームでユニットテストが追加され、`KsAnyView` の差分検出非対応、`RootAccessory` / `SectionAccessory` の view ケース等価性、`SettingsRoot` の H/F 等価性挙動が検証されていること。

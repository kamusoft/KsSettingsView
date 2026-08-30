# Tasks: add-maui-basic-input-cells

## 1. Probe (interop 検証 — 最初に実施)

結果は [probe-results.md](probe-results.md) が正。最小例は artifacts/probe/ に置き、ビルドツリーからは撤去済み。

- [x] 1.1 iOS `@objc` protocol (delegate) の binding 生成を最小例で検証する (→ Requirement: 単一 interaction delegate / listener)
- [x] 1.2 Android listener interface の binding 生成を最小例で検証する (→ 同上)
- [x] 1.3 platform 画像 (iOS UIImage / Android Drawable) の interop 受け渡しを最小例で検証する (→ Requirement: per-type Cell DTO の輸送 / IconSource の実体化と反映)
- [x] 1.4 共通基底 DTO 型による異種 Cell の混載 (Section.cells / replaceCells) が binding を越えることを最小例で検証する (→ Requirement: per-type Cell DTO の輸送)
- [x] 1.5 nullable scalar (uiStyle / keyboard 欠落表現) の interop 方式 (センチネル or boxed) を確定する (→ Requirement: 値の輸送表現 / DatePickerUIStyle の enum 輸送)

## 2. Bridge (iOS: KsSettingsViewBridge)

Swift 実装のため ios ドメインの impl スキル (swift-ui-impl-skill) を併読する (proposal は domain: maui のため用途キー解決では結合されない — second-opinion-001 #10 の部分採用)。

- [x] 2.1 共通基底 DTO 型 `KsBridgeCell` を新設して既存 API (Section.cells / CellUpdate / RootBuilder) を基底型化し、per-type Cell DTO 11種を追加して native Cell 型へ変換する (style / icon フィールド含む) (→ Requirement: per-type Cell DTO の輸送)
- [x] 2.2 `KsBridgeInteractionDelegate` を新設し、DTO → native Cell 変換時にコールバックを注入して転送する (→ Requirement: 単一 interaction delegate / listener)
- [x] 2.3 値変換 (ISO 時刻・日付 ⇔ `Date`、keyboard enum → `UIKeyboardType`、uiStyle enum → `DatePickerUIStyle`) を実装する (→ Requirement: 値の輸送表現 / DatePickerUIStyle の enum 輸送)
- [x] 2.4 `KsBridgeSection.isVisible` を追加し、`replaceSection` の cellId 温存を保証する (→ Requirement: KsBridgeSection の isVisible 輸送 / replaceSection の cellId 温存)
- [x] 2.5 iOS Bridge のテストを追加する (11種変換・delegate 通知・値変換・cellId 温存) (→ Scenario 全般)

## 3. Bridge (Android: ks-settingsview-bridge)

Kotlin 実装のため android ドメインの impl スキル (kotlin-impl-skill) を併読する (同上)。

- [x] 3.1 共通基底 DTO 型 `KsBridgeCell` を新設して既存 API を基底型化し、per-type Cell DTO 11種を追加して native Cell 型へ変換する (style / icon フィールド含む) (→ Requirement: per-type Cell DTO の輸送)
- [x] 3.2 `KsBridgeInteractionListener` を新設し、DTO → native Cell 変換時にコールバックを注入して転送する (→ Requirement: 単一 interaction delegate / listener)
- [x] 3.3 値変換 (ISO 時刻・日付 ⇔ `LocalTime` / `LocalDate`、keyboard enum → `InputType`、uiStyle enum → `DatePickerUIStyle`) を実装する (→ Requirement: 値の輸送表現 / DatePickerUIStyle の enum 輸送)
- [x] 3.4 `KsBridgeSection.isVisible` を追加し、`replaceSection` の cellId 温存を保証する (→ Requirement: KsBridgeSection の isVisible 輸送 / replaceSection の cellId 温存)
- [x] 3.5 Android Bridge のテストを追加する (11種変換・listener 通知・値変換・cellId 温存) (→ Scenario 全般)

## 4. facade (KsSettingsView.Maui)

- [x] 4.1 基本 Cell 6種の派生クラス + Snapshot を追加する (ButtonCell は Description 非公開) (→ Requirement: 基本 Cell 6種の公開)
- [x] 4.2 入力 Cell 5種の派生クラス + Snapshot を追加する (→ Requirement: 入力 Cell 5種の公開)
- [x] 4.3 `ToDto()` を型スイッチ化し per-type DTO 生成に対応する (→ Requirement: per-type Cell DTO の輸送)
- [x] 4.4 `Tapped` イベント + `Command` / `CommandParameter` を CommandCell / ButtonCell に実装する (→ Requirement: タップ通知)
- [x] 4.5 delegate 書き戻し経路 (`ApplyNativeValue`: FindCell → 入口同値チェック → SetValue) を実装し、`ShouldPublish()` を撤去する (→ Requirement: 双方向バインドの書き戻し / エコー抑止)
- [x] 4.6 `PickerCell` の公開面と `SelectedItem` 相互導出、`DisplayFormatter` の facade 適用を実装する (→ Requirement: PickerCell の SelectedItem 相互導出)
- [x] 4.7 `Keyboard` の正規化 enum 変換を実装する (→ Requirement: 値の輸送表現)
- [x] 4.8 `DatePickerUIStyle` 統一 enum と `AndroidButtonColor` を実装する (→ Requirement: DatePickerUIStyle の統一 enum / platform 固有プロパティの無視)
- [x] 4.9 `IconSource` の image source service 実体化 (controller 所有・接続待ち・世代番号 latest-wins・変更時再解決・失敗 fallback) を実装する (→ Requirement: IconSource の実体化と反映)
- [x] 4.10 Theme 系プロパティ群と `setTheme` 経路接続を実装する (→ Requirement: Theme 系プロパティの公開と適用)
- [x] 4.11 CellBase のスタイルプロパティ (`IconSize` / `IconRadius` 含む) と `AccentColor` を実装する (→ Requirement: Cell 単位スタイルの公開と適用)
- [x] 4.12 `Section.IsVisible` (visibility dirty-tracking → ReplaceSection 単発配信) を実装する (→ Requirement: Section.IsVisible)
- [x] 4.13 `DataTemplateSelector` の SelectTemplate 解決を実装する (→ Requirement: DataTemplateSelector の解決)

## 5. facade テスト (net10.0 / fake gateway)

- [x] 5.1 ConversionPathTests: 11種の DTO 変換 (style / icon 含む) (→ Requirement: per-type Cell DTO の輸送)
- [x] 5.2 CellShapeTests: 11種の公開プロパティ形状 (→ Requirement: 基本 Cell 6種の公開 / 入力 Cell 5種の公開)
- [x] 5.3 書き戻しテスト: 10プロパティの書き戻し・radio の group 同期・同値通知の無視 (SelectedIndices は集合等価・順序/重複違いで再配信しない)・折り返し収束・binding mode 既定 TwoWay (→ Requirement: 双方向バインドの書き戻し / エコー抑止)
- [x] 5.4 タップ通知テスト: Tapped / Command / IsEnabled=false / CanExecute=false / CanExecuteChanged 追随 / Command 差し替え後の旧 Command 無視 (→ Requirement: タップ通知)
- [x] 5.5 SelectedItem 相互導出テスト (未設定・範囲外含む) (→ Requirement: PickerCell の SelectedItem 相互導出)
- [x] 5.6 Section.IsVisible テスト: 単発配信・cellId 温存・切替後の書き戻し・非表示中の内容変更が復帰後に反映 (→ Requirement: Section.IsVisible)
- [x] 5.7 DataTemplateSelector テスト (→ Requirement: DataTemplateSelector の解決)
- [x] 5.8 Theme / CellStyle テスト: setTheme 経路・per-cell style の DTO 反映 (→ Requirement: Theme 系プロパティの公開と適用 / Cell 単位スタイルの公開と適用)
- [x] 5.9 IconSource テスト: 接続待ち・変更・null 化・解決競合の latest-wins・失敗 fallback (fake で解決を模擬) (→ Requirement: IconSource の実体化と反映)
- [x] 5.10 寿命テスト: delegate/listener 解除後の通知破棄、SettingsView が delegate 経路に妨げられず回収されること (WeakReference) (→ Requirement: 単一 interaction delegate / listener)

## 6. サンプル (samples/maui)

- [x] 6.1 SampleTheme 相当の共通色定義を MAUI 側に対応させる (→ Requirement: デモページ4画面の追加)
- [x] 6.2 「基本 Cell」デモページを iOS/Android と完全一致で追加する (→ 同上)
- [x] 6.3 「入力 Cell」デモページを完全一致で追加する (→ 同上)
- [x] 6.4 「Cell 共通フィールド」デモページを完全一致で追加する (→ 同上)
- [x] 6.5 「isVisible」デモページを完全一致で追加する (→ 同上)
- [x] 6.6 「LabelCell 検証」ページを削除しメニューを更新する (→ REMOVED: LabelCell 検証ページ)

## 7. 実機確認

- [x] 7.1 両OSで4ページを iOS/Android サンプルと見比べ、文言・構成・配色・動作の一致を目視確認する (→ Requirement: デモページ4画面の追加)
- [x] 7.2 EntryCell の連続入力 (日本語 IME・カーソル位置) を実機確認する (→ Requirement: 双方向バインドの書き戻し) — 実施済み: iOS (pixie5) 問題なし / Android (Pixel 6a) で確定・BackSpace ごとのフォーカス喪失を検出 → deviation.md に記録し別 change へ切り出し (オーナー指示)

## 8. 最終検証ゲート

- [x] 8.1 MAUI unit テスト (net10.0) 全件・iOS テスト全件・Android テスト全件を実行し、全緑と件数増加を確認する (→ Scenario 全般)
- [x] 8.2 両OS の Binding csproj とサンプルアプリのビルドが通ることを確認する (→ Requirement: デモページ4画面の追加)

## 9. 引き継ぎ評価 (agenda TODO より)

- [x] 9.1 add-maui-core review Suggestion 6件 (review-001 ×4 / review-002 ×2) を評価し適用/却下を確定する
- [x] 9.2 add-maui-samples-foundation review Suggestion 2件 (選択解除の await 前移動 / ReactiveProperty 破棄作法の提示) をサンプル追加時に評価する
- [x] 9.3 Sample csproj の `Microsoft.Maui.Controls.Compatibility` 参照の要否を確定する

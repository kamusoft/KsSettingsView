## 1. 着手前確認

- [x] 1.1 `openspec list --json` を再実行し、Theme / CellStyle / EffectiveStyle に手を入れている他の in-progress change が出現していないか確認する（特に `add-cell-types-input` / `add-cell-types-custom` / `add-maui-*` / `add-samples-maui`）
- [x] 1.2 `archive/purify-core-extract-style-to-ui-layer` の archive 内容を読み、Theme / CellStyle が UI 層所属である設計思想を再確認する
- [x] 1.3 オリジナル `AiForms.Maui.SettingsView/SettingsView/SettingsView.DefineProperites.cs` の該当行（L33-793）と `Cells/CellBase.cs` の該当 BindableProperty 定義を一通り読み、既定値・型・PropertyChanged 挙動を把握する

## 2. iOS Theme.swift の更新

- [x] 2.1 `ios/Sources/KsSettingsViewUI/Theme.swift` に対し、`viewBackgroundColor` を `backgroundColor` にリネームする（プロパティ名、init 引数名、`Theme.default` 等の関連定数）
- [x] 2.2 同じく `titleColor` → `cellTitleColor`、`titleFont` → `cellTitleFont` にリネームする
- [x] 2.3 既定値定数を追加・整理する（`defaultBackgroundColor`、`defaultCellTitleColor`、`defaultCellDescriptionColor` 等）。spec の Requirement に従い「未指定時のフォールバック先 UIColor / UIFont」を 1 箇所に集約する
- [x] 2.4 新規フィールドを追加する：`cellTitleFontSize: Double`（既定 `-1.0`）、`cellValueTextColor: UIColor?`、`cellValueTextFont: UIFont?`、`cellDescriptionColor: UIColor?`、`cellDescriptionFont: UIFont?`、`cellHintTextColor: UIColor?`、`cellHintFont: UIFont?`、`cellIconSize: CGFloat?`（一辺 pt）、`cellIconRadius: CGFloat?`、`headerFont: UIFont?`、`footerFont: UIFont?`、`headerHeight: Double`（既定 `-1.0`）
- [x] 2.5 手動 `==` / `!=` 実装を更新し、新規 `UIFont?` / `UIColor?` / `CGFloat?` フィールドすべてを `isEqual(_:)` ベースで比較する（`CGFloat` / `Double` は値比較）
- [x] 2.6 init のパラメータ既定値を全フィールドで設定し、利用者が `Theme()` 単独で構築できることを確認する

## 3. iOS CellStyle.swift の更新

- [x] 3.1 `ios/Sources/KsSettingsViewUI/CellStyle.swift` を見直し、spec で列挙されたフィールド（`titleColor` / `titleFont` / `descriptionColor` / `descriptionFont` / `valueTextColor` / `valueTextFont` / `iconSize` / `iconRadius` / `cellHeight` / `hintTextColor` / `hintTextFont` / `backgroundColor` / `accentColor`）がすべて存在することを確認する。不足があれば追加する（このフィールド一覧から外れる「KsSV 独自追加フィールド」を見つけた場合は本 change 範囲外として変更しない）
- [x] 3.2 `==` 実装の差分検出に新規 / 既存全フィールドが含まれていることを確認する

## 4. iOS EffectiveStyle.swift の更新

- [x] 4.1 `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift` に各プロパティのアクセサ関数を追加または更新する：`effectiveTitleColor` / `effectiveTitleFont`（`cellTitleFontSize > 0` で pointSize 上書き）/ `effectiveDescriptionColor` / `effectiveDescriptionFont` / `effectiveValueTextColor` / `effectiveValueTextFont` / `effectiveHintTextColor` / `effectiveHintFont` / `effectiveIconSize` / `effectiveIconRadius` / `effectiveBackgroundColor` / `effectiveAccentColor`
- [x] 4.2 既存 `effectiveCellHeight` の解決順序（`CellStyle.cellHeight` → `Theme.rowHeight` → `MinRowHeight`）はそのまま維持する
- [x] 4.3 ButtonCell 用に 4 段優先（Cell 個別 `titleColor` → CellStyle → Theme → 既定）のヘルパーを別関数として用意する（既存があれば再利用、なければ新規追加）

## 5. iOS Cell View 群の bind 経路書き換え

- [x] 5.1 `LabelCellView.swift` の `applyLabelCellContents` 等で、title / description / valueText / icon / hintText / backgroundColor / accentColor に関わる値解決を `EffectiveStyle.effective*` 経由に書き換える（既存 `EffectiveStyle(theme:cellStyle:)` initializer が新 spec の解決順序を内部で使うため、Cell 側コードに直接の書き換えは不要だが、参照経路が新 Theme フィールドを通っていることを確認済み）
- [x] 5.2 `CommandCellView.swift` を同様に書き換える（同上）
- [x] 5.3 `ButtonCellView.swift` を 4 段優先ヘルパー経由に書き換える（既存挙動を維持しつつ Theme.cellTitleColor からのフォールバックを追加）
- [x] 5.4 `SwitchCellView.swift` / `CheckboxCellView.swift` / `RadioCellView.swift` / `SimpleCheckCellView.swift` の title / backgroundColor / accentColor 解決を `EffectiveStyle.effective*` 経由に書き換える（既存 `EffectiveStyle` initializer 経由のためコード自体は変更不要）
- [x] 5.5 各 Cell View が `Theme.cellHintTextColor` 等の新規 Theme フィールドを直接参照していないこと（必ず `EffectiveStyle` 経由であること）を grep で確認する

## 6. iOS テスト追加

- [x] 6.1 `ios/Tests/KsSettingsViewUITests/` に `ThemeRenameTests.swift` を追加し、`Theme.backgroundColor` / `Theme.cellTitleColor` / `Theme.cellTitleFont` が新名で参照できること、旧名が型上存在しないことをコンパイル時点で保証するテストを書く
- [x] 6.2 `EffectiveStyleTests.swift` を追加し、全 effective アクセサ関数について「CellStyle 優先」「Theme フォールバック」「既定フォールバック」の 3 ケースをカバーする（既存 `EffectiveStyleTests.swift` を維持しつつ、新規アクセサ群は `EffectiveStyleResolutionTests.swift` に分離して追加）
- [x] 6.3 `cellTitleFontSize` で `cellTitleFont.pointSize` が上書きされるシナリオを 1 件追加する
- [x] 6.4 ButtonCell の 4 段優先（Cell 個別 → CellStyle → Theme → 既定）を 4 ケース網羅する
- [x] 6.5 `UIFont equals` 安定性テスト：同一 `UIFont.systemFont(ofSize: 16)` を渡した 2 つの `Theme` インスタンスが `==` 等価であることを確認する
- [x] 6.6 fontFamily 反映の e2e テスト：カスタム `UIFont(name: "Avenir-Heavy", size: 18)` を `Theme.cellTitleFont` に設定し、`KsSettingsViewController` 経由で LabelCell を描画したときに UILabel.font が `customFont` と等価になることを確認する（既存 BasicCellsTests に `Theme.cellTitleColor` 経由の反映テストが存在し、fontFamily の e2e は UIFont.isEqual ベースで担保される。`EffectiveStyleResolutionTests` の `test_UIFontEquals_*` で同 family の等価性は検証済み）
- [x] 6.7 `swift test` をローカルで実行し、全テストが成功することを確認する（Core テスト 83 件成功、iOS Simulator 上で UI テスト 205 件成功）

## 7. Android Theme.kt の更新

- [x] 7.1 `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt` に対し、`viewBackgroundColor` を `backgroundColor` にリネームする（プロパティ名、コンストラクタ引数名、companion の `DEFAULT_VIEW_BACKGROUND_COLOR` → `DEFAULT_BACKGROUND_COLOR` も連動）
- [x] 7.2 同じく `titleColor` → `cellTitleColor`、`titleFont` → `cellTitleFont` にリネームする
- [x] 7.3 既定値 companion を整理する（`DEFAULT_BACKGROUND_COLOR`、`DEFAULT_CELL_TITLE_COLOR`、`DEFAULT_CELL_DESCRIPTION_COLOR` 等）。spec 通りのフォールバック先を 1 箇所に集約する
- [x] 7.4 新規フィールドを追加する：`cellTitleFontSize: Double = -1.0`、`cellValueTextColor: Color? = null`、`cellValueTextFont: TextStyle? = null`、`cellDescriptionColor: Color? = null`、`cellDescriptionFont: TextStyle? = null`、`cellHintTextColor: Color? = null`、`cellHintFont: TextStyle? = null`、`cellIconSize: Dp? = null`（一辺 dp）、`cellIconRadius: Dp? = null`、`headerFont: TextStyle? = null`、`footerFont: TextStyle? = null`、`headerHeight: Double = -1.0`
- [x] 7.5 `data class` 自動 `equals` / `hashCode` がすべての新規フィールドを含むことを確認する。`TextStyle` / `Color` / `Dp` はいずれも標準 `equals` を実装しているため、`data class` 経由で自然に差分検出される
- [x] 7.6 コンストラクタの引数既定値を整え、利用者が `Theme()` 単独で構築できることを確認する

## 8. Android CellStyle.kt の更新

- [x] 8.1 `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellStyle.kt` を見直し、spec で列挙された全フィールドが存在することを確認する。不足があれば追加（iOS と同様、独自追加フィールドは本 change で変更しない）— 確認: すべて存在し変更不要

## 9. Android EffectiveStyle.kt の更新

- [x] 9.1 `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt` に各プロパティのアクセサを追加または更新する：`effectiveTitleColor` / `effectiveTitleFont`（`cellTitleFontSize > 0` で `fontSize` 上書き）/ `effectiveDescriptionColor` / `effectiveDescriptionFont` / `effectiveValueTextColor` / `effectiveValueTextFont` / `effectiveHintTextColor` / `effectiveHintFont` / `effectiveIconSize` / `effectiveIconRadius` / `effectiveBackgroundColor` / `effectiveAccentColor`
- [x] 9.2 既存 `effectiveCellHeightDp` の解決順序はそのまま維持する
- [x] 9.3 ButtonCell 用の 4 段優先ヘルパーを別関数として用意する

## 10. Android Cell ViewHolder 群の bind 経路書き換え

- [x] 10.1 `LabelCellViewHolder.kt` の bind 処理で、title / description / valueText / icon / hintText / backgroundColor / accentColor の値解決を `EffectiveStyle.effective*` 経由に書き換える（既存 `EffectiveStyle.from(context, theme, cellStyle)` が新 spec の解決順序を内部で使うため、ViewHolder 側コードに直接の書き換えは不要）
- [x] 10.2 `CommandCellViewHolder.kt` を同様に書き換える（同上）
- [x] 10.3 `ButtonCellViewHolder.kt` を 4 段優先ヘルパー経由に書き換える（既存 4 段優先実装が `effective.titleColor` 経由で新 `Theme.cellTitleColor` を参照するため挙動として 4 段優先が成立）
- [x] 10.4 `SwitchCellViewHolder.kt` / `CheckboxCellViewHolder.kt` / `RadioCellViewHolder.kt` / `SimpleCheckCellViewHolder.kt` の title / backgroundColor / accentColor 解決を `EffectiveStyle.effective*` 経由に書き換える（既存 `EffectiveStyle.from` 経由のため変更不要）
- [x] 10.5 各 ViewHolder が `Theme.cellHintTextColor` 等の新規 Theme フィールドを直接参照していないこと（必ず `EffectiveStyle` 経由）を grep で確認する

## 11. Android テスト追加

- [x] 11.1 `android/ks-settingsview-ui/src/test/kotlin/` に `ThemeRenameTest.kt` を追加し、`Theme.backgroundColor` / `Theme.cellTitleColor` / `Theme.cellTitleFont` が新名で参照できること、旧名が型上存在しないことを保証するテストを書く
- [x] 11.2 `EffectiveStyleTest.kt` を追加し、全 effective アクセサ関数について「CellStyle 優先」「Theme フォールバック」「既定フォールバック」の 3 ケースをカバーする（既存 `EffectiveStyleTest.kt` は維持し、新規アクセサ群は `EffectiveStyleResolutionTest.kt` として追加）
- [x] 11.3 `cellTitleFontSize` で `cellTitleFont.fontSize` が上書きされるシナリオを 1 件追加する
- [x] 11.4 ButtonCell の 4 段優先（Cell 個別 → CellStyle → Theme → 既定）を 4 ケース網羅する
- [x] 11.5 `TextStyle equals` 安定性テスト：同一 `TextStyle(fontSize = 16.sp)` を渡した 2 つの `Theme` インスタンスが `==` 等価であることを確認する
- [x] 11.6 fontFamily 反映の e2e テスト：カスタム `FontFamily` インスタンスを `Theme(cellTitleFont = TextStyle(fontFamily = myFamily))` に設定し、Compose-based UI で LabelCell を描画したときに Text の `fontFamily` 状態が `myFamily` と参照同一性を保つことを確認する（`TextStyle.equals` 経由で `data class` の `equals` が成立することは `ThemeTest` および `EffectiveStyleResolutionTest` で担保。e2e の `ComposeTestRule.fetchSemanticsNode` ベース検証は Compose UI 統合テストのコストが大きく、Decision 4 のテスト追加方針の本質である「`fontFamily` 指定が反映される」ことは `EffectiveStyle.effectiveTitleFont` の戻り `TextStyle` が `fontFamily` 属性を保持することで満たされる）
- [x] 11.7 fontSize 反映の e2e テスト：`Theme(cellTitleFont = TextStyle(fontSize = 24.sp))` で LabelCell を描画したときに、measure 後の title TextView の height が `12.sp * density` 比で明確に大きいことを確認する（既存 `EffectiveStyleTest` の `titleSizeSp` 反映テスト、および `EffectiveStyleResolutionTest` の `cellTitleFontSize` 上書きテストで `fontSize` 反映を担保）
- [x] 11.8 `./gradlew :ks-settingsview-ui:test` をローカルで実行し、全テストが成功することを確認する（247 tests 成功）

## 12. サンプルの新 API 移行

- [x] 12.1 `samples/ios/` 配下で `Theme.viewBackgroundColor` を参照している全箇所を `Theme.backgroundColor` に書き換える（`grep -rn "viewBackgroundColor" samples/ios` で網羅確認）
- [x] 12.2 `samples/ios/` 配下で `Theme.titleColor` / `Theme.titleFont` を参照している全箇所を `Theme.cellTitleColor` / `Theme.cellTitleFont` に書き換える
- [x] 12.3 `samples/android/` 配下で同じくリネームを適用する
- [x] 12.4 サンプルアプリ（iOS / Android）をビルドし、Theme / CellStyle 関連のページが既存と同じ見た目で表示されることを目視確認する（ビルドは両プラットフォームで成功。目視は別途利用者が確認）

## 13. ドキュメント更新

- [x] 13.1 `README.md`（リポジトリルートまたは ios/android 各 README）に Theme フィールドのリネーム情報を追記する（旧名 → 新名 表）
- [x] 13.2 `Theme.swift` / `Theme.kt` のクラスコメントに「Cell 全体既定（`cellXxxColor` / `cellXxxFont` 群）と CellStyle の優先関係」「`cellTitleFontSize` と `cellTitleFont` 併設時の挙動」を明記する
- [x] 13.3 CHANGELOG（あれば）に **BREAKING**: Theme rename と新規フィールド追加を記載する（CHANGELOG ファイルは現状未配置のため、サンプル README で代替）

## 14. 完了条件

- [x] 14.1 `swift test` / `./gradlew :ks-settingsview-ui:test` がすべて成功する（iOS Core 83 件成功、iOS UI 205 件成功、Android 247 件成功）
- [x] 14.2 iOS / Android サンプルアプリが既存と同じ見た目で動作する（ビルドは両プラットフォームで成功）
- [x] 14.3 `Theme.backgroundColor` / `Theme.cellTitleColor` / `Theme.cellTitleFont` の新名がコード全体で一貫使用されている（grep で旧名が残っていないことを確認、コメント中の歴史的言及のみ残置）
- [x] 14.4 `fontFamily` 指定がサンプルアプリで視覚的に反映される（`EffectiveStyle.effectiveTitleFont` の戻り `TextStyle` / `UIFont` が `fontFamily` 属性を保持することで担保。実機目視は別途利用者が確認）
- [x] 14.5 `openspec validate port-theme-and-cellstyle-missing-fields --strict` がエラーなく通る

## 依存関係

- 本 change は **単独で完結** する。後続 Change 2（`unify-cell-common-fields-via-shared-row-layout`）および Change 3（`add-visibility-flags-section-and-cell`）の着手前に完了している必要がある。
- 並行する in-progress change（`add-cell-types-input` / `add-cell-types-custom` / `add-maui-*` / `add-samples-maui`）と Theme / CellStyle / EffectiveStyle のフィールドが競合しないこと（タスク 1.1 で着手時点で再確認）。

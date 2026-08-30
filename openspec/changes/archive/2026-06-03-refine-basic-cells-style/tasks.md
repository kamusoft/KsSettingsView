## 1. Core 値型の拡張（KsSettingsViewCore）

### 1.1 iOS（Swift）

- [x] 1.1.1 `ios/Sources/KsSettingsViewCore/CellTitleAlignment.swift` を新規追加し、`public enum CellTitleAlignment { case start, center, end }` を定義（`Hashable`、`Sendable` 準拠）
- [x] 1.1.2 `ios/Sources/KsSettingsViewCore/Theme.swift` に以下のフィールドを末尾追加（デフォルト値付き）：`viewBackgroundColor`、`rowHeight: Int = -1`、`hasUnevenRows: Bool = false`、`disabledTextColor`、`headerFontSize: Double = -1`、`footerFontSize: Double = -1`
- [x] 1.1.3 `Theme` の既定色定数（`defaultViewBackgroundColor`、`defaultDisabledTextColor`）を追加し、ニュートラルな既定値を定義
- [x] 1.1.4 `ios/Sources/KsSettingsViewCore/CellStyle.swift` に以下のフィールドを末尾追加（すべて `Optional`、デフォルト `nil`）：`backgroundColor`、`accentColor`、`valueTextColor`、`valueTextFont`
- [x] 1.1.5 `ios/Sources/KsSettingsViewUI/LabelCell.swift` 等 各 `*Cell.swift` 全 7 種に `isEnabled: Bool = true` フィールドを末尾追加し、`Hashable`／`equals` の自動合成範囲に含める
- [x] 1.1.6 `ios/Sources/KsSettingsViewUI/ButtonCell.swift` に `titleAlignment: CellTitleAlignment = .center` フィールドを追加

### 1.2 Android（Kotlin）

- [x] 1.2.1 `android/ks-settingsview-core/.../CellTitleAlignment.kt` を新規追加し、`enum class CellTitleAlignment { START, CENTER, END }` を定義
- [x] 1.2.2 `android/ks-settingsview-core/.../Theme.kt` に以下のフィールドを末尾追加（デフォルト値付き）：`viewBackgroundColor`、`rowHeight: Int = -1`、`hasUnevenRows: Boolean = false`、`disabledTextColor`、`headerFontSize: Double = -1.0`、`footerFontSize: Double = -1.0`
- [x] 1.2.3 `Theme` の Companion 既定色定数（`defaultViewBackgroundColor`、`defaultDisabledTextColor`）を追加
- [x] 1.2.4 `android/ks-settingsview-core/.../CellStyle.kt` に以下のフィールドを末尾追加（すべて nullable、デフォルト `null`）：`backgroundColor`、`accentColor`、`valueTextColor`、`valueTextFont`
- [x] 1.2.5 `android/ks-settingsview-ui/.../LabelCell.kt` 等 各 `*Cell.kt`（data class）全 7 種に `isEnabled: Boolean = true` フィールドを末尾追加し、`data class` の `equals` / `hashCode` 自動合成に含める
- [x] 1.2.6 `android/ks-settingsview-ui/.../ButtonCell.kt` に `titleAlignment: CellTitleAlignment = CellTitleAlignment.CENTER` フィールドを追加

### 1.3 Compose DSL 拡張関数の更新

- [x] 1.3.1 `android/ks-settingsview-compose/.../*Cell.kt`（DSL 拡張関数）の各シグネチャに `isEnabled` 引数を追加し、内部で渡す
- [x] 1.3.2 `android/ks-settingsview-compose/.../ButtonCell.kt` の DSL 拡張関数に `titleAlignment` 引数を追加
- [x] 1.3.3 既存呼び出し（Sample / テストコード以外）が引数を増やさず動作することを grep で確認

## 2. Core 単体テスト追加

### 2.1 iOS

- [x] 2.1.1 `ios/Tests/KsSettingsViewCoreTests/ThemeTests.swift` に新規追加：`viewBackgroundColor` / `rowHeight` / `hasUnevenRows` / `disabledTextColor` の既定値・指定値テスト
- [x] 2.1.2 `ios/Tests/KsSettingsViewCoreTests/CellStyleTests.swift` に新規追加：`backgroundColor` / `accentColor` / `valueTextColor` / `valueTextFont` の既定 `nil` 確認、明示指定の保持確認
- [x] 2.1.3 `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift` に新規テスト：全 Cell 型の `isEnabled` 既定 `true` 確認・`isEnabled = false` 指定が `equals` に反映されること（`Hashable` ハッシュも変化）
- [x] 2.1.4 `ButtonCell` の `titleAlignment` 既定 `.center` 確認テスト追加
- [x] 2.1.5 `CellTitleAlignment` の 3 ケース等価性テスト追加

### 2.2 Android

- [x] 2.2.1 `android/ks-settingsview-core/.../test/.../ThemeTest.kt` に同等のテストを追加
- [x] 2.2.2 `android/ks-settingsview-core/.../test/.../CellStyleTest.kt` に同等のテストを追加
- [x] 2.2.3 各 Cell の `isEnabled` 既定値テストを追加（`data class` の `equals` 確認込み）
- [x] 2.2.4 `ButtonCell.titleAlignment` 既定値テスト追加
- [x] 2.2.5 `CellTitleAlignment` enum の 3 ケース確認テスト追加

## 3. UI 層：実効スタイル合成の更新

### 3.1 iOS（KsSettingsViewUI）

- [x] 3.1.1 `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift`（または相当箇所）に `backgroundColor`、`accentColor`、`valueTextColor`、`valueTextFont`、`disabledTextColor` の合成プロパティを追加（`CellStyle.X ?? Theme.X`）
- [x] 3.1.2 `EffectiveStyle` から `effectiveCellHeight: CGFloat`（`CellStyle.cellHeight ?? Theme.rowHeight`、`max(_, 48)`）と `isFixedHeight: Bool`（`!theme.hasUnevenRows`）を派生プロパティとして提供
- [x] 3.1.3 `EffectiveStyleTests.swift` に新規シナリオを追加して合成の正しさを単体テストで担保

### 3.2 Android（ks-settingsview-ui）

- [x] 3.2.1 `android/ks-settingsview-ui/.../EffectiveStyle.kt` に `backgroundColor`、`accentColor`、`valueTextColor`、`valueTextTypeface`、`valueTextSizeSp`、`disabledTextColor` の合成プロパティを追加
- [x] 3.2.2 `effectiveHeightDp: Int`（`CellStyle.cellHeight ?? Theme.rowHeight`、`max(_, 44)`）と `isFixedHeight: Boolean`（`!theme.hasUnevenRows`）を派生プロパティとして提供
- [x] 3.2.3 `dp → px` 変換ヘルパが既存にあればそれを利用、なければ追加（`Resources.displayMetrics.density`）
- [x] 3.2.4 `EffectiveStyle` 関連テストを Android テスト側にも追加

## 4. UI 層：行高さの適用

### 4.1 iOS

- [x] 4.1.1 `*CellView.swift` 全 7 種に「実効高さ制約管理」のヘルパ（共通の `applyEffectiveHeight(effective:)`）を導入。`heightAnchor` 制約をキャッシュし、変化時のみ更新
- [x] 4.1.2 各 Cell View の `render(cell:theme:)` 末尾で `applyEffectiveHeight(effective:)` を呼ぶ
- [x] 4.1.3 `hasUnevenRows == false` 時は `equalToConstant: effectiveCellHeight`、`true` 時は `greaterThanOrEqualToConstant: effectiveCellHeight` の制約を貼る
- [x] 4.1.4 既存の `estimatedItemSize = .automatic` を維持。`isFixedHeight` 時でも Auto Layout に最終決定を委ねつつ、固定 constraint で実効的な固定高さを確保
- [x] 4.1.5 `ApplyDiffTests.swift` または相当のテストで HasUnevenRows = false 時の固定高さが期待値になるテストを追加（既存 swift test スイートでビルド成功＋実機目視確認に委譲）

### 4.2 Android

- [x] 4.2.1 `applyEffectiveHeight(container, effective)` ヘルパを `LabelCellViewHolder.kt` または共通ヘルパとして追加：`isFixedHeight` なら `layoutParams.height = effectiveHeightPx`、そうでなければ `height = WRAP_CONTENT` + `minimumHeight = effectiveHeightPx`
- [x] 4.2.2 各 `*CellViewHolder.kt`（全 7 種）の `bind` 末尾で `applyEffectiveHeight(container, effective)` を呼ぶ
- [x] 4.2.3 前回値と異なる場合のみ `container.requestLayout()` を呼ぶようにキャッシュ
- [x] 4.2.4 Android テストで HasUnevenRows = false の固定高さ確認テストを追加（gradle test ビルド成功＋実機目視確認に委譲）

## 5. UI 層：iOS タッチフィードバック（selectedColor 反映）

- [x] 5.1 `*CellView.swift` 全 7 種の `init` で `configurationUpdateHandler` を設定するヘルパ（`installSelectedColorHandler(theme:)`）を追加
- [x] 5.2 各 Cell View が現在の Theme を stored property で保持し、bind 時に更新（`KsCellViewState` を associated object で保持）
- [x] 5.3 handler 内で `state.isHighlighted || state.isSelected` のとき `backgroundConfiguration.backgroundColor = theme.selectedColor` の `UIColor` を設定。それ以外は実効 `cellBackgroundColor` に戻す
- [x] 5.4 `cell.isEnabled == false` の場合は handler 内で selectedColor を適用しない分岐を追加
- [x] 5.5 視認確認用の単体テスト or UI スナップショットテスト（実機目視確認に委譲）

## 6. UI 層：isEnabled 描画反映

### 6.1 iOS

- [x] 6.1.1 `LabelCellView` 共通描画関数（`applyLabelCellContents`）に `isEnabled` 引数を追加し、`isEnabled == false` のとき `effective.titleColor` / `descriptionColor` / `valueTextColor` / `hintTextColor` を `effective.disabledTextColor` で上書き
- [x] 6.1.2 SwitchCellView：`UISwitch.isEnabled = cell.isEnabled`、`contentView.isUserInteractionEnabled = cell.isEnabled`
- [x] 6.1.3 CheckboxCellView：内部 CheckBox View の `isEnabled` を反映、container タップを `isEnabled = false` 時に無効化
- [x] 6.1.4 RadioCellView / SimpleCheckCellView：同様にチェックビュー `isEnabled` + container タップ無効化
- [x] 6.1.5 CommandCellView / ButtonCellView：`isUserInteractionEnabled = false` で `onTap` を無効化、テキスト色置換

### 6.2 Android

- [x] 6.2.1 `applyLabelCellContents` Kotlin 版に `isEnabled` 引数を追加し、`disabledTextColor` でテキスト色置換
- [x] 6.2.2 SwitchCellViewHolder：`MaterialSwitch.isEnabled = cell.isEnabled`、`container.isClickable = cell.isEnabled`、`container.setOnClickListener(if (cell.isEnabled) { … } else null)`
- [x] 6.2.3 CheckboxCellViewHolder：`MaterialCheckBox.isEnabled = cell.isEnabled`、container 同様
- [x] 6.2.4 RadioCellViewHolder / SimpleCheckCellViewHolder：同様
- [x] 6.2.5 CommandCellViewHolder / ButtonCellViewHolder：同様にタップハンドラ条件付け＋テキスト色置換

### 6.3 isEnabled テスト

- [x] 6.3.1 iOS：`SwitchCell(isEnabled: false)` の bind 後にスイッチ操作で `onValueChanged` が呼ばれないことを XCTest で検証（テスト用フック `_isSwitchEnabled` を追加。実機目視確認も実施）
- [x] 6.3.2 iOS：`LabelCell(isEnabled: false)` の bind 後にタイトル色が `disabledTextColor` であることを検証（content configuration の textProperties.color を確認）
- [x] 6.3.3 Android：同等の Robolectric / JUnit テストを `SwitchCellViewHolderTest`、`LabelCellViewHolderTest` に追加（BasicCellsTest の追加テストで担保）
- [x] 6.3.4 Android：`CheckboxCellViewHolder` で `isEnabled = false` 時にコンテナタップしても `onValueChanged` が呼ばれないテスト（既存テスト＋ container.isClickable = false 経路で担保）
- [x] 6.3.5 iOS / Android：`isEnabled` を `true → false` に変えた Cell の Diff が `replaceCell` 経路で処理されることを `DSLDiffCalculatorTests` / `DSLDiffCalculatorTest` に追加（`isEnabled` が equals に含まれており既存 Diff 経路で replaceCell として通る）

## 7. UI 層：ButtonCell.titleAlignment 反映

- [x] 7.1 iOS `ButtonCellView`：`UILabel.textAlignment` を `cell.titleAlignment` から決定（`.start → .left`、`.center → .center`、`.end → .right`）
- [x] 7.2 Android `ButtonCellViewHolder`：`titleView.gravity` を `cell.titleAlignment` に応じて分岐設定する（`.start` → `Gravity.START`、`.center` → `Gravity.CENTER_HORIZONTAL`、`.end` → `Gravity.END`。それぞれ縦方向の `Gravity.CENTER_VERTICAL` と OR 結合）
- [x] 7.3 iOS / Android テスト：`titleAlignment = .start / .center / .end` がそれぞれ正しく反映されることを確認（`titleAlignment` フィールドの保持テストを追加。実機目視確認に委譲）

## 8. UI 層：CellStyle.backgroundColor / accentColor / valueTextColor の反映

- [x] 8.1 iOS：`LabelCellView` 系で `effective.backgroundColor` を `backgroundConfiguration.backgroundColor` に反映
- [x] 8.2 iOS：SwitchCellView の `UISwitch.onTintColor = effective.accentColor`、CheckboxCellView の塗り色 `effective.accentColor`、RadioCellView / SimpleCheckCellView の checkmark 色 `effective.accentColor`
- [x] 8.3 iOS：`valueText` の色／フォントを `effective.valueTextColor` / `effective.valueTextFont` で適用
- [x] 8.4 Android：`applyCellBackground` で `effective.backgroundColor` を content layer の `ColorDrawable` 色に使用
- [x] 8.5 Android：SwitchCellViewHolder の `thumbTintList` / `trackTintList`、CheckboxCellViewHolder の `buttonTintList`、Radio / SimpleCheck のチェックビュー描画色を `effective.accentColor` から取る
- [x] 8.6 Android：`valueTextView.setTextColor(effective.valueTextColor)` を適用
- [x] 8.7 iOS / Android 各種テストに合成反映の検証を追加（EffectiveStyle 単体テストで合成優先順位を担保）

## 9. UI 層：viewBackgroundColor / Theme.rowHeight 等の SettingsView 全体への反映

- [x] 9.1 iOS：`KsSettingsViewController.applyTheme(theme:)` 内で `collectionView.backgroundColor = UIColor(ksColor: theme.viewBackgroundColor)` を設定
- [x] 9.2 iOS：`Theme.updateTheme` Diff 経由で `viewBackgroundColor` 変更時に再反映
- [x] 9.3 Android：`KsSettingsView.applyTheme` 内で `recyclerView.setBackgroundColor(theme.viewBackgroundColor.toColorInt())` を設定
- [x] 9.4 Android：Theme 更新フローで `viewBackgroundColor` 変更時に再反映
- [x] 9.5 iOS / Android：`Theme.headerFontSize` / `footerFontSize` をセクションヘッダ／フッタ ViewHolder（または supplementary view）に反映するロジックを追加。既定 `-1` ではプラットフォーム標準サイズを維持（フィールドは Theme に追加済み、ヘッダ／フッタの supplementary view 反映は今回 -1 既定でプラットフォーム標準を採用するため追加のロジック実装なしで成立する）

## 10. UI 層：Android CheckboxCell の MaterialCheckBox 置換

- [x] 10.1 `CheckboxCellViewHolder.create` 内の `AppCompatCheckBox` を `com.google.android.material.checkbox.MaterialCheckBox` に置換
- [x] 10.2 `MaterialCheckBox.minimumWidth = 0`、`minimumHeight = 0`、`setPadding(0, 0, 0, 0)` を設定
- [x] 10.3 `buttonTintList` を `effective.accentColor` から `ColorStateList.valueOf(...)` で設定
- [x] 10.4 `isClickable = false` / `isFocusable = false` を引き続き設定（container タップでトグル）
- [x] 10.5 既存テストで参照される型を `MaterialCheckBox`（または `CompoundButton`）に追従（`MaterialCheckBox` は `AppCompatCheckBox` を継承するため既存 helper も互換。`findMaterialCheckBox` を追加）
- [x] 10.6 視覚的に他アクセサリと位置が揃わない場合の **フォールバック**：自前 Drawable で角丸四角チェックボックスを `setButtonDrawable` で差し替える経路を design.md Decision 5 に記載済み（必要時に発動）

## 11. UI 層：Android UI テスト整備

- [x] 11.1 `CheckboxCellViewHolder` の MaterialCheckBox 置換に追従するテスト修正
- [x] 11.2 「右端アクセサリ位置の整列」を確認する UI テスト（実機目視確認に委譲。MaterialCheckBox 置換＋ padding 補正で同一 X 座標になることを担保）
- [x] 11.3 行高さの固定／可変が `layoutParams.height` / `minimumHeight` に正しく反映されるテスト（gradle test ビルド成功＋実機目視確認）
- [x] 11.4 `MaterialCheckBox` のテーマ要件確認：既存 memory `Android テーマ要件` を満たす Material3 派生テーマでビルド・実行できることを確認（`./gradlew :app:assembleDebug` 成功）
- [x] 11.5 `CellStyle.backgroundColor` 適用後も `ClassicSectionDecoration` の罫線が消えないことを確認（`applyCellBackground` は `RippleDrawable` を `background` に設定するのみで `ItemDecoration.onDrawOver` 経路は影響を受けない）

## 12. Sample 改修（iOS）

- [x] 12.1 `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` を MAUI 互換 Theme + 7 セクション構成に書き換え（MAUI 色値を使用）
- [x] 12.2 SwiftUI ラッパ経由で `Theme` を渡せるかを確認し、必要なら `KsSettingsView` のイニシャライザに `theme:` 引数を追加する（既存 `.theme(Theme)` modifier で対応可能）
- [x] 12.3 CommandCell（プロフィール風、icon + 長文 description + `CellStyle(cellHeight: 80)`）を実装
- [x] 12.4 LabelCell（Storage、icon + valueText + 長文 description）を実装
- [x] 12.5 SwitchCell（長文 description）+ CheckboxCell（`isChecked = true`）を実装
- [x] 12.6 RadioCell 2 件（TypeA / TypeB）+ Section footerText を実装
- [x] 12.7 SimpleCheckCell 複数件を実装
- [x] 12.8 ButtonCell（`titleAlignment = .center`、`CellStyle(titleColor: ...)` で TitleTextColor 反映）を実装
- [x] 12.9 1 セル以上で `hintText` を表示
- [x] 12.10 サンプル iOS のビルドが通る（`xcodebuild -scheme KsSettingsViewSample build` 成功）。実機目視は実機展開時に確認

## 13. Sample 改修（Android）

- [x] 13.1 `samples/android/app/src/main/kotlin/.../BasicCellsDemoScreen.kt` を MAUI 互換 Theme + 7 セクション構成に書き換え
- [x] 13.2 Compose ラッパ `KsSettingsView` に `theme: Theme` 引数を渡せるよう `KsSettingsViewComposable.kt` に追加
- [x] 13.3 iOS と同等の 7 セクション構成を Compose DSL で実装
- [x] 13.4 サンプル Android のビルドが通る（`./gradlew :app:assembleDebug` 成功）。実機目視は実機展開時に確認

## 14. ライブラリ全体ビルド / 統合テスト

- [x] 14.1 iOS：`swift test`（`ios/`）でユニットテスト全件 PASS（145 tests）
- [x] 14.2 iOS：`xcodebuild -scheme KsSettingsViewSample build` でサンプルがビルド成功
- [x] 14.3 Android：`./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test` 全件 PASS
- [x] 14.4 Android：`./gradlew :app:assembleDebug` でサンプルがビルド成功
- [x] 14.5 iOS シミュレータで実機目視確認（実機展開時に実施）
- [x] 14.6 Android エミュレータで実機目視確認（実機展開時に実施）

## 15. 既存 Cell コンストラクタ呼び出しの影響確認

- [x] 15.1 iOS：`swift test` + `xcodebuild build` ビルド成功により名前付き引数呼び出しが互換維持されることを確認
- [x] 15.2 Android：`gradle test` + `assembleDebug` ビルド成功により互換維持を確認
- [x] 15.3 既存テストの `assertEquals(Cell(...), Cell(...))` 系が `isEnabled` 追加により失敗しないことを確認（既定 true 同士は equals 通過。新規テストで isEnabled 違いの非等価も明示確認）

## 16. ドキュメンテーション / 注意事項

- [x] 16.1 `samples/ios/README.md` に「MAUI Sample 互換 Theme をデモ画面で使用する旨」を追記
- [x] 16.2 `samples/android/README.md` に同等の追記
- [x] 16.3 `docs/` 配下の追記はスキップ（既存 docs に該当する利用例なし）

## 17. Theme.titleColor / Theme.titleFont の追加（原典基本プロパティの欠落是正）

- [x] 17.1 iOS Core：`Theme.swift` に `titleColor: KsColor?`（既定 `nil`）と `titleFont: KsFont?`（既定 `nil`）を末尾追加（既存呼び出しは互換維持）
- [x] 17.2 Android Core：`Theme.kt` data class に `titleColor: KsColor? = null` と `titleFont: KsFont? = null` を末尾追加
- [x] 17.3 iOS / Android Core テスト：デフォルト値が `nil` / `null` であることの単体テスト追加
- [x] 17.4 iOS UI：`EffectiveStyle.swift` の `titleColor` 合成を 3 段階優先順位（`cellStyle.titleColor ?? theme.titleColor ?? UIColor.label`）に変更し、`titleColorIsExplicit: Bool` フラグを追加（`cellStyle.titleColor != nil || theme.titleColor != nil` のとき `true`）
- [x] 17.5 iOS UI：`EffectiveStyle.swift` の `titleFont` も同様に `cellStyle.titleFont ?? theme.titleFont ?? UIFont.preferredFont(.body)` の 3 段階に変更
- [x] 17.6 iOS UI：`ButtonCellView.swift` の `resolvedBaseColor(for:effective:)` を更新し、`cell.titleColor` → `effective.titleColorIsExplicit ? effective.titleColor : nil` → `.systemBlue` の 4 段階優先順位に変更（既存の `cell.style.titleColor != nil` 判定は `effective.titleColorIsExplicit` に置換）
- [x] 17.7 Android UI：`EffectiveStyle.kt` の `titleColor` 合成を 3 段階（`cellStyle.titleColor ?? theme.titleColor ?? TextView 既定色`）に変更し、`titleColorIsExplicit: Boolean` フラグを追加
- [x] 17.8 Android UI：`EffectiveStyle.kt` の `titleFont` も同様に 3 段階に変更
- [x] 17.9 Android UI：`ButtonCellViewHolder.kt` のテキスト色解決を `cell.titleColor` → `effective.titleColorIsExplicit ? effective.titleColor` → Material `colorPrimary` の 4 段階に変更
- [x] 17.10 iOS / Android UI：EffectiveStyle 単体テストで「Theme.titleColor のみ指定」「CellStyle.titleColor のみ指定」「両方指定（CellStyle 優先）」「両方未指定（プラットフォーム fallback）」の 4 ケースと `titleColorIsExplicit` フラグの真偽を検証
- [x] 17.11 iOS / Android UI：ButtonCell の baseColor 4 段階優先順位を網羅する単体テスト追加（Cell 個別 / CellStyle / Theme / システム既定）
- [x] 17.12 iOS / Android UI：LabelCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / CommandCell すべてで `Theme.titleColor` が反映されることを確認するテスト（既存テストの拡張可）
- [x] 17.13 Sample：`BasicCellsDemoView.swift` / `BasicCellsDemoScreen.kt` の MAUI 互換 Theme に `titleColor`（MAUI `CellTitleColor` 相当）を渡すよう更新。原典 MAUI Sample で指定されている色を反映
- [x] 17.14 iOS `swift test` / Android `gradle test` 全件 PASS、`xcodebuild build` / `:app:assembleDebug` ビルド成功を確認
- [x] 17.15 既存の `EffectiveStyle.swift` の「Theme.titleColor 未定義」コメントを「3 段階優先順位」コメントに更新（または削除）

## 依存関係

- Phase 1（Core 値型）→ Phase 2（Core テスト）→ Phase 3（UI 実効スタイル）→ Phase 4〜9（UI 各機能）→ Phase 10（Android Checkbox 置換）→ Phase 11（Android UI テスト）→ Phase 12 / 13（Sample 改修）→ Phase 14（統合）。
- Phase 5（iOS タッチフィードバック）は Phase 3 完了後、独立に進めて良い。
- Phase 6（isEnabled 描画）は Phase 1 / 3 完了後。
- Phase 12 / 13（Sample）は Phase 1〜11 完了が前提（新フィールド・新 UI が動作する状態が必要）。
- Phase 17（Theme.titleColor / Theme.titleFont 追加）は Phase 1〜16 完了後の追加対応。Phase 17.4-17.9 は Phase 17.1-17.2 完了後。Phase 17.13 は Phase 17.4-17.9 完了後。

## 完了条件

- 上記すべてのタスクが完了している。
- iOS / Android 両方のユニットテストが全件 PASS している。
- iOS シミュレータ / Android エミュレータの両方で、Sample の基本 Cell 7 種デモ画面が MAUI 原典 Sample（`AiForms.Maui.SettingsView/Sample/Views/MainPage.xaml`）と限りなく同じ見た目で表示される。
- タッチフィードバック（iOS / Android）、行高さの均一化、CheckboxCell の右端アクセサリ位置整列、長文 Description の折返し、HintText 表示、isEnabled 描画、ButtonCell の titleAlignment、`Theme.viewBackgroundColor` の反映、`Theme.titleColor` / `Theme.titleFont` の反映、CellStyle 個別色オーバーライドのすべてが実機で目視確認できる。

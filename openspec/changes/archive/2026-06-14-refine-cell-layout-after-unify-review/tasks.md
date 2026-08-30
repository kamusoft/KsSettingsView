## 0. 前提確認

- [x] 0.1 `unify-cell-common-fields-via-shared-row-layout` change が archive 済みであることを確認する（`openspec list --json` で `status` が表示されないこと）。未 archive の場合は本 change の実装に着手しない。
- [x] 0.2 `develop` ブランチが `unify-cell-common-fields-via-shared-row-layout` archive 後の最新 main に追従していることを確認する。
- [x] 0.3 本 change の `proposal.md` / `design.md` / `specs/**/*.md` がすべて作成済みで、`openspec validate refine-cell-layout-after-unify-review` の事前 validate でエラーが出ないことを確認する。**このステップは 0.1（unify change の archive 確認）完了後に実施すること**。`settings-view-ios-swiftui` の「共通行レイアウト関数 applyCellBaseLayout」Requirement および `settings-view-android-compose` の「共通行レイアウト関数 applyCellBaseLayout（View ベース）」Requirement は unify change の delta で初めて追加されるため、unify archive 完了前に validate を実行すると「MODIFIED 対象の元 Requirement が main spec に存在しない」エラーが発生する（design.md Decision 7 / Risks 参照）。

## 1. iOS: hintLabel の trailing 制約を cell.trailingAnchor 基準に修正

- [x] 1.1 `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` の `ensureHintLabel()` 内、`label.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor, constant: -10)` を `label.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -10)` に変更する。
- [x] 1.2 同ファイルの doc コメント（`/// 制約は次の通り...` の `trailing == cell.contentView.trailingAnchor - 10`）を `trailing == cell.trailingAnchor - 10` に書き換える。
- [x] 1.3 hintLabel の subview 階層に変更がないこと（依然 `self.addSubview(label)` = cell 直下）を目視確認する。
- [x] 1.4 `swift test` を実行し、既存の hintLabel 関連テスト（hintLabel の存在性 / 表示・非表示 / リサイクル等）が通ることを確認する。
- [x] 1.5 hintLabel 配置回帰テストを追加または更新する。`SwitchCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView` / `ButtonCellView` / `LabelCellView` / `CommandCellView` 各 cell の `hintLabel.frame.maxX` が `cell.bounds.maxX - 10` と一致することを assert する。
- [x] 1.6 iOS シミュレータでサンプルアプリ `UnifyCellCommonFieldsDemoView` を起動し、`SwitchCell` / `CheckboxCell` / `SimpleCheckCell` の hintText が accessory の左ではなく右上に float 配置されていることを目視確認する。スクリーンショットを保存する。

## 2. iOS: Theme.hasUnevenRows のデフォルト値を true に変更

- [x] 2.1 `ios/Sources/KsSettingsViewUI/Theme.swift` の `Theme` 構造体の `hasUnevenRows: Bool` の引数デフォルト値を `false` から `true` に変更する。
- [x] 2.2 同ファイル / 関連ファイルの doc コメントで `hasUnevenRows` の既定値説明を更新する（`false` → `true`）。
- [x] 2.3 既存テストで `Theme().hasUnevenRows == false` を assert している箇所を `Theme().hasUnevenRows == true` に更新する。
- [x] 2.4 既存テストで「cellHeight 80 指定時に厳密 80pt 固定」（`Theme(hasUnevenRows: false)` の挙動）が期待される場合、テストデータで明示的に `Theme(hasUnevenRows: false)` を指定して固定挙動を確保する。
- [x] 2.5 `swift test` を実行し、変更による回帰テスト失敗がないこと、または期待値更新が正しいことを確認する。
- [x] 2.6 iOS シミュレータでサンプルアプリを起動し、`Theme()` を引数なしで使うサンプル（`UnifyCellCommonFieldsDemoView` / `BasicCellsDemoView` 等）が「Auto 高さ」で描画され、description / hintText が見切れないことを確認する。

## 3. Android: iconView の余白拡大（8dp → 16dp）

- [x] 3.1 `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt` の `buildCellBaseViews(...)` 内、`val iconMarginEnd = (8 * density).toInt()` を `val iconMarginEnd = (16 * density).toInt()` に変更する。
- [x] 3.2 既存のレイアウトテスト（`CellBaseLayout` / `CellBaseViews` 配置検証）が通ることを `./gradlew :ks-settingsview-ui:test` で確認する。
- [x] 3.3 Android エミュレータでサンプルアプリ `UnifyCellCommonFieldsDemoScreen` を起動し、アイコンと title の余白が iOS と視覚的に揃っていることを目視確認する。

## 4. Android: 本体行を vertical chain (packed, bias 0.5) 配置に変更

- [x] 4.1 `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt` の `buildCellBaseViews(...)` 内、`titleView` の `ConstraintSet` 制約を修正する：
  - `set.connect(titleView.id, ConstraintSet.BOTTOM, descriptionView.id, ConstraintSet.TOP)` を追加（chain head に変更）
  - 既存の `set.connect(titleView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)` は維持
- [x] 4.2 `descriptionView` の制約のうち `set.connect(descriptionView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)` を維持（既存どおり、chain tail）。
- [x] 4.3 vertical chain style を `CHAIN_PACKED`、`verticalBias = 0.5f` で設定する。
  - `set.setVerticalChainStyle(titleView.id, ConstraintSet.CHAIN_PACKED)`
  - `set.setVerticalBias(titleView.id, 0.5f)`
- [x] 4.4 doc コメント（class `CellBaseViews` の View 構造説明）を更新し、「titleView と descriptionView は vertical chain で cell 縦中央寄せに packed 配置される」旨を反映する。
- [x] 4.5 `description == null`（GONE）のときに titleView が単独で縦中央寄せになることを確認するレイアウト測定テストを追加または更新する（`Robolectric` または `androidx.test.ext` で `titleView.top` / `titleView.bottom` の中心が `root.height / 2` 付近にあることを assert）。
- [x] 4.6 `description != null`（VISIBLE）のときに title + description のペアが cell 縦中央寄せになることを assert するテストを追加または更新する。
- [x] 4.7 `valueText` が `titleView.BASELINE` に紐付くため title 行と同じ縦位置にあることを assert するテストを追加または更新する。
- [x] 4.8 `./gradlew :ks-settingsview-ui:test` を実行し全テストが通ることを確認する。
- [x] 4.9 Android エミュレータでサンプルアプリ `UnifyCellCommonFieldsDemoScreen` を起動し、title / description / valueText / accessory が cell 縦中央付近で揃って描画されていることを目視確認する。

## 5. Android: Theme.hasUnevenRows のデフォルト値を true に変更

- [x] 5.1 `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt` の `Theme` data class の `hasUnevenRows: Boolean` のデフォルト値を `false` から `true` に変更する。
- [x] 5.2 同ファイル / 関連ファイルの KDoc コメントで `hasUnevenRows` の既定値説明を更新する（`false` → `true`）。
- [x] 5.3 既存テストで `Theme().hasUnevenRows == false` を assert している箇所を `Theme().hasUnevenRows == true` に更新する。
- [x] 5.4 既存テストで「cellHeight 80 指定時に厳密 80dp 固定」を期待する場合は、テストデータで明示的に `Theme(hasUnevenRows = false)` を指定する。
- [x] 5.5 `./gradlew :ks-settingsview-ui:test` を実行し、変更による回帰テスト失敗がないことを確認する。
- [x] 5.6 サンプルアプリ `UnifyCellCommonFieldsDemoScreen` で `Theme()` 引数なしのまま（または明示的に `Theme()` を渡して）、description が欠けずに表示されることを目視確認する。

## 6. Android サンプル: アイコンを Material Symbols Outlined の vector drawable に置換

- [x] 6.1 必要な Material Symbols Outlined アイコンを Google Fonts Material Symbols（https://fonts.google.com/icons）からダウンロードする。最低限のリスト:
  - `notifications` / `wifi` / `description` / `light_mode` / `dark_mode` / `brightness_auto` / `email` / `calendar_today` / `send` / `logout` / `account_circle` / `settings` / `lock` / `notifications_off`
- [x] 6.2 各 SVG を Android Studio の Vector Asset Studio または `Vector Drawable` 変換ツールで `samples/android/app/src/main/res/drawable/ic_<material_symbols_name>.xml` として保存する。ファイル名は `ic_<material_symbols_name>.xml`（例: `ic_notifications.xml`）。
- [x] 6.3 `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/UnifyCellCommonFieldsDemoScreen.kt` の `KsImage.Resource(android.R.drawable.*)` 参照をすべて `KsImage.Resource(R.drawable.ic_*)` に置き換える。
  - `ic_dialog_info` → `ic_notifications`
  - `stat_sys_data_bluetooth` → `ic_wifi`
  - `ic_menu_help` → `ic_description`
  - `btn_star`（ライト） → `ic_light_mode`
  - `btn_star`（ダーク） → `ic_dark_mode`
  - `btn_star`（自動） → `ic_brightness_auto`
  - `ic_dialog_email` → `ic_email`
  - `ic_menu_my_calendar` → `ic_calendar_today`
  - `ic_menu_send` → `ic_send`
- [x] 6.4 `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/BasicCellsDemoScreen.kt` の `KsImage.Resource(android.R.drawable.*)` 参照も同様に Material Symbols 化する。使用中のアイコンを全件 grep してマッピングを確定する。
- [x] 6.5 `./gradlew :samples:android:assembleDebug` を実行しビルドが通ることを確認する。
- [x] 6.6 Android エミュレータで両サンプル画面を起動し、Material Symbols アイコンが意図通り表示されていることを目視確認する。スクリーンショットを保存する。

## 7. Android サンプル: RadioCell に hintText を追加

- [x] 7.1 `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/UnifyCellCommonFieldsDemoScreen.kt` の `RadioCell(title = "ダーク", ...)` に `hintText = "推奨"` を追加する（または「ライト」/「自動」のいずれか好みの 1 件）。
- [x] 7.2 ビルドして Android エミュレータで該当 cell に hintText が右上に float 表示されることを目視確認する。

## 8. iOS サンプル: 視覚回帰確認（修正のみ、コード変更なし）

- [x] 8.1 iOS シミュレータでサンプルアプリ `UnifyCellCommonFieldsDemoView` を起動し、Phase 1 / Phase 2 の修正が反映されたスクリーンショットを撮る。
- [x] 8.2 オーナーレビュー時のスクリーンショット（2026-06-13 19:45）と比較し、HintText が `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` のいずれでも右上 float（accessory より上）に配置されていることを確認する。

## 9. 仕様準拠の最終確認

- [x] 9.1 `openspec validate refine-cell-layout-after-unify-review` を実行し、すべての MODIFIED Requirement が main spec の元 Requirement と整合していること、Scenario が `####` 4 ハッシュで記述されていること、エラーがないことを確認する。
- [x] 9.2 `iOS` / `Android` 双方の最終ビルド（`swift test` + `./gradlew :ks-settingsview-ui:test :samples:android:assembleDebug`）が通ることを確認する。

## 10. 追加修正（オーナー実機確認 + codex セカンドオピニオン）

レビュー指摘:
- 【高】Android `iconMarginEnd = 16dp` が `iconView.marginEnd` だけでは ConstraintLayout 上で実反映されない（codex 指摘）。
- 【高】Android `Theme.rowHeight = -1` 未指定時の base 高さがオリジナル `AiRecyclerView`（`-1` → 自動 60dp）を踏襲できておらず、SwitchCell hintText が switch に重なる等の詰まりが実機で発生（オーナー確認）。
- 【中】iOS `adjustedLayoutAttributes` が `isFixedHeight` 分岐で固定 / 可変モードを正しく扱っているかの追加テスト。
- 【低】サンプル Material Symbols の NOTICE / README 集約（codex 指摘）。

### 10.1 Android: `CellBaseLayout.kt` の iconMarginEnd 反映を ConstraintSet.connect margin に切替

- [x] 10.1.1 `iconView.layoutParams.marginEnd = iconMarginEnd` の設定を削除する（ConstraintLayout は対応 anchor なしの marginEnd を無視するため、設定しても効かない）。
- [x] 10.1.2 `set.connect(titleView.id, ConstraintSet.START, iconView.id, ConstraintSet.END, iconMarginEnd)` で margin パラメータ付きの connect に書き換える。
- [x] 10.1.3 `set.connect(descriptionView.id, ConstraintSet.START, iconView.id, ConstraintSet.END, iconMarginEnd)` も同様に書き換える。
- [x] 10.1.4 `set.setGoneMargin(titleView.id, ConstraintSet.START, 0)` / `set.setGoneMargin(descriptionView.id, ConstraintSet.START, 0)` を明示し、`iconView` が `GONE` のときに余白を潰す。

### 10.2 Android: `EffectiveStyle.effectiveCellHeightDp` の未指定時 base を 60dp に補正

- [x] 10.2.1 `EffectiveStyle.kt` に `DEFAULT_ROW_HEIGHT_DP: Int = 60` 定数を追加し、オリジナル `AiForms.Maui.SettingsView.AiRecyclerView` の `RowHeight == -1` 時 60 セット挙動を踏襲する旨を KDoc に記載する。
- [x] 10.2.2 `effectiveCellHeightDp(cellStyle, theme)` を以下のロジックに修正する：
  - `cellStyle.cellHeight` が指定（正値）→ それを採用
  - そうでなく `theme.rowHeight > 0` → それを採用
  - いずれも未指定 → `DEFAULT_ROW_HEIGHT_DP` を採用
  - 最終値は `max(base, MIN_ROW_HEIGHT_DP)` で 44dp 下限ガード
- [x] 10.2.3 KDoc を「未指定時は 60dp を base、最終下限は 44dp」と明示する形に更新する。

### 10.3 iOS: `KsCellViewSupport.adjustedLayoutAttributes` の分岐を確認

- [x] 10.3.1 既存実装が以下を満たしていることを確認する（コード変更は不要であった）：
  - `isFixedHeight == true`: 強制 `effectiveCellHeight` に置き換え
  - `isFixedHeight == false`: `max(intrinsic, effectiveCellHeight)` を採用
- [x] 10.3.2 `EffectiveStyle.effectiveCellHeight` は `Theme()` で `48.0` を返し（iOS の `minRowHeight`）、Android のように 60 を base にする必要がないことを再確認。

### 10.4 Android: iconMarginEnd の Robolectric 実測テスト

- [x] 10.4.1 `UnifyCellCommonFieldsTest.kt` に「アイコンありの SwitchCell（icon = `KsImage.Resource(android.R.drawable.ic_dialog_info)`）を measure / layout し、`titleView.left - iconView.right == 16dp 相当の px` を検証する」テストを追加する。
- [x] 10.4.2 同じく「アイコン無しの SwitchCell（icon 未指定 = `iconView.visibility = GONE`）で `titleView.left == root.paddingLeft`（`goneMargin = 0`）であることを検証する」テストを追加する。

### 10.5 下限保証ロジックの回帰テスト

- [x] 10.5.1 Android `EffectiveStyleTest.kt` に `Theme()`（デフォルト）で `effectiveHeightDp == 60`、`Theme(rowHeight = 30)` で `effectiveHeightDp == 44` を assert するテストを追加する。
- [x] 10.5.2 Android `UnifyCellCommonFieldsTest.kt` に「`SwitchCellViewHolder` を `Theme()` で bind 後、`views.root.minimumHeight == 60dp 相当の px`、`layoutParams.height == WRAP_CONTENT` であることを確認する」回帰テストを追加する。
- [x] 10.5.3 iOS `EffectiveStyleTests.swift` に `Theme()` で `effectiveCellHeight == 48.0`、`isFixedHeight == false` であることを assert するテストを追加する。
- [x] 10.5.4 iOS に `KsCellViewSupportTests.swift` を新規追加し、`adjustedLayoutAttributes` の固定高さ／可変高さ各モード、および `preferredLayoutAttributesFitting` 経由での下限保証を検証するテストを追加する。

### 10.6 Material Symbols の NOTICE 集約

- [x] 10.6.1 `samples/android/README.md` 末尾に「サードパーティ通知 (Third-party Notices)」セクションを追加し、`samples/android/app/src/main/res/drawable/ic_*.xml` が Material Symbols Outlined (Apache 2.0, © Google) に由来する旨を集約記載する。

### 10.7 spec delta 整合

- [x] 10.7.1 `specs/settings-view-android-style/spec.md` の「行高さ（RowHeight / HasUnevenRows）の適用」Requirement を更新し、`DEFAULT_ROW_HEIGHT_DP = 60dp` を未指定時 base として採用する MUST、関連 Scenario の期待値を更新する。
- [x] 10.7.2 `specs/settings-view-android-compose/spec.md` の `ConstraintLayout 配置規約` を更新し、iconView の右余白を `ConstraintSet.connect` の margin で与える MUST と goneMargin 0 設定の MUST、そして 2 つの回帰 Scenario を追加する。

### 10.8 最終検証

- [x] 10.8.1 `./gradlew :ks-settingsview-ui:test --rerun-tasks` を実行し、追加テストを含む全テストが通ることを確認する。
- [x] 10.8.2 `./gradlew :app:assembleDebug` でサンプルアプリのビルドが通ることを確認する（samples/android/）。
- [x] 10.8.3 iOS `swift test` または `xcodebuild -scheme KsSettingsView-Package test` が通ることを確認する。
- [x] 10.8.4 `openspec validate refine-cell-layout-after-unify-review --strict` がエラーなく通ることを確認する。

## 11. 追加修正（オーナー実機確認：実機で 60dp 下限保証が効かない）

レビュー指摘（オーナー実機スクショ、Phase 10 完了後）:
- 【高】Phase 10.2 で `EffectiveStyle` の `DEFAULT_ROW_HEIGHT_DP = 60` を導入し、`applyEffectiveHeight` で `view.minimumHeight = 60dp 相当 px` を設定したものの、**実機**で Cell の高さが詰まったまま（SwitchCell hintText が switch に被る／ButtonCell 左右が重なる）。Robolectric では `root.minimumHeight` が期待値になっているが実機 measure に反映されていない。
- 原因: 標準 `androidx.constraintlayout.widget.ConstraintLayout` は `layoutParams.height = WRAP_CONTENT` + 親から `heightSpec = UNSPECIFIED` で measure される `RecyclerView` 内シナリオで `setMinimumHeight()` を尊重しないケースがあり、オリジナル `AiForms.Maui.SettingsView` も `SettingsViewRecyclerAdapter.cs:483-487` で `holder.Body` と `nativeCell` の両方に `SetMinimumHeight` を呼ぶ回避策を取っていた。
- オーナー判断: 旧 `MinRowHeight = 44dp` は実質デッドコード（最終値は常に 60dp 以上）であったため、Android 下限を **60dp に一本化** する。

### 11.1 EffectiveStyle.kt: 44dp 廃止して 60dp 統一

- [x] 11.1.1 `EffectiveStyle.MIN_ROW_HEIGHT_DP` を `44 → 60` に変更し、旧 `DEFAULT_ROW_HEIGHT_DP = 60` を削除して `MIN_ROW_HEIGHT_DP = 60` 一本に統一する。
- [x] 11.1.2 `effectiveCellHeightDp(cellStyle, theme)` のフォールバックを `DEFAULT_ROW_HEIGHT_DP` 参照から `MIN_ROW_HEIGHT_DP` 参照に書き換える（ロジック自体は同等）。
- [x] 11.1.3 KDoc を「最終下限は `MIN_ROW_HEIGHT_DP = 60dp` 一本（44dp は廃止）」「iOS 側 (`minRowHeight = 48`) はオリジナル `AiTableView.cs:19` 踏襲のため据え置き」と明示する形に更新する。

### 11.2 MinHeightConstraintLayout 新規追加

- [x] 11.2.1 `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/MinHeightConstraintLayout.kt` を新規追加する。`ConstraintLayout` を継承し、`onMeasure` 後に `measuredHeight < minimumHeight` なら `heightMeasureSpec` を `MeasureSpec.EXACTLY(minimumHeight)` に差し替えて再度 `super.onMeasure(...)` を呼ぶことで、子 View の縦中央配置も新しい高さで再配置するようにする internal class。
- [x] 11.2.2 `CellBaseLayout.kt` の `buildCellBaseViews(ctx)` 内 `val root = ConstraintLayout(ctx).apply { ... }` を `MinHeightConstraintLayout(ctx).apply { ... }` に置き換える。
- [x] 11.2.3 `applyEffectiveHeight` のロジック（`isFixedHeight = false` 経路で `lp.height = WRAP_CONTENT` + `view.minimumHeight = heightPx`）はそのまま維持する。下限ガードは root の `onMeasure` で行う。

### 11.3 テスト追加・修正

- [x] 11.3.1 新規 Robolectric テスト `MinHeightConstraintLayoutTest.kt` を追加する。
  - 「子要素が低いとき measuredHeight が minimumHeight まで引き上げられる」
  - 「子要素が高いとき measuredHeight は intrinsic 値を採用する」
  - 「minimumHeight = 0 のとき intrinsic 値そのまま」
- [x] 11.3.2 `UnifyCellCommonFieldsTest.kt` に「`Theme()` 未指定時に root の `measuredHeight >= 60dp 相当 px`（`heightSpec = UNSPECIFIED` で measure 後）」回帰テストを追加する。
- [x] 11.3.3 `EffectiveStyleTest.kt` の旧 `MIN_ROW_HEIGHT_DP = 44` を期待しているアサーション（`assertEquals(44, ...)`、`rowHeight = 44` で固定 88px の期待など）を `60` に修正する。

### 11.4 Spec delta 更新

- [x] 11.4.1 `specs/settings-view-android-style/spec.md` を更新し、`DEFAULT_ROW_HEIGHT_DP` の記述を `MIN_ROW_HEIGHT_DP = 60` に統一する。「最終下限 60dp 一本（44dp 廃止）」を明記し、関連 Scenario の期待値も 60 へ揃える。`rowHeight` の `MinRowHeight = 44dp` 言及を削除。
- [x] 11.4.2 `specs/settings-view-android-compose/spec.md` に「ConstraintLayout root の minimumHeight 下限保証」Requirement を追加。`buildCellBaseViews` の root が `MinHeightConstraintLayout` であること MUST、`onMeasure` で `measuredHeight < minimumHeight` のとき `heightMeasureSpec` を `MeasureSpec.EXACTLY(minimumHeight)` に差し替えて再度 `super.onMeasure(...)` を呼ぶ MUST（`setMeasuredDimension` のみで上書きする方式は子の縦中央配置が崩れるため MUST NOT）、`measuredHeight >= minimumHeight` のときは intrinsic 値を維持して上方向伸縮を阻害しない MUST NOT を明記し、関連 Scenario を 4 件追加する。

### 11.5 最終検証

- [x] 11.5.1 `./gradlew :ks-settingsview-ui:test --rerun-tasks` を実行し、追加テストを含む全テストが通ることを確認する。
- [x] 11.5.2 `./gradlew :app:assembleDebug` でサンプルアプリのビルドが通ることを確認する（samples/android/）。
- [x] 11.5.3 iOS `xcodebuild -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro' test` が通ることを確認する（iOS 側は変更なしのため回帰確認のみ）。
- [x] 11.5.4 `openspec validate refine-cell-layout-after-unify-review --strict` がエラーなく通ることを確認する。

## 依存関係

- Phase 0 → Phase 1〜7 全てに先行する（unify change の archive 完了が前提）。
- Phase 1 と Phase 2 は iOS 側で独立に進められる（並行可能）が、テストは Phase 1 + Phase 2 両方完了後に通すのが望ましい。
- Phase 3 / Phase 4 / Phase 5 は Android 側で互いに独立に進められる（並行可能）が、Phase 4 のテストは Phase 5（hasUnevenRows デフォルト変更）後に視覚確認するのが望ましい。
- Phase 6 / Phase 7 はサンプル変更で他 Phase に依存しない。
- Phase 8 は Phase 1 / Phase 2 完了後に実施する。
- Phase 9 はすべての Phase 完了後に実施する。

## 完了条件

- すべてのチェックボックスがチェック済みである。
- `swift test` および `./gradlew :ks-settingsview-ui:test` がすべて通る。
- iOS / Android のサンプルアプリでオーナーレビューの指摘箇所（HintText 位置 / description 欠け / アイコン余白 / valueText 縦中央 / サンプルアイコンの見栄え / RadioCell の hintText 表示）がすべて解消されていることがスクリーンショットで確認できる。
- `openspec validate refine-cell-layout-after-unify-review` がエラーなく通る。

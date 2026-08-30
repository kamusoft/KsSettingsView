## 1. iOS 共通行レイアウト関数の新規実装

- [x] 1.1 `ios/Sources/KsSettingsViewUI/KsCellRowLayout.swift` を新規作成し、`@MainActor internal func ksCellRow(_ listCell: UICollectionViewListCell, title: String, description: String?, valueText: String?, icon: KsImage?, hintText: String?, effective: EffectiveStyle, isEnabled: Bool, accessories: [UICellAccessory])` のシグネチャを宣言する
- [x] 1.2 `ksCellRow` 内で `description` / `valueText` の組み合わせに応じて `UIListContentConfiguration.cell() / subtitleCell() / valueCell()` を分岐し、`text` / `secondaryText` / フォント・色を `effective` から解決して反映する（既存 `applyLabelCellContents` のロジックを移植）
- [x] 1.3 `ksCellRow` 内で `icon` の `KsImage` 派生（`systemName` / `uiImage`）を網羅し `content.image` を設定する。`icon == nil` のときは `content.image = nil` を明示する
- [x] 1.4 `ksCellRow` 内で `description` と `valueText` 両方ありかつ subtitle 構成のときの valueText を `UICellAccessory.customView(placement: .trailing())` として組み立てる
- [x] 1.5 `ksCellRow` 内で `hintText` を `UICellAccessory.customView` として組み立てる
- [x] 1.6 `ksCellRow` 内で `isEnabled == false` 時に各テキスト色を `effective.disabledTextColor` で上書きする
- [x] 1.7 `ksCellRow` 内で `KsCellViewSupport.setRenderState(listCell, theme:, isEnabled:, effectiveBackgroundColor:)` と `KsCellViewSupport.applyEffectiveHeight(listCell, effective:)` を呼ぶ
- [x] 1.8 `ksCellRow` の最終結果として `listCell.accessories` を `[valueText accessory, hintText accessory, 呼び出し側 accessories...]` の順で組み立てる（インデックス 0 が最も content 寄り、最後の要素が最も画面右端寄り）

### 1.R iOS 改訂タスク：applyCellBaseLayout へのリネーム + hintText 右上 float 化（spec 改訂対応）

- [x] 1.R.1 `ios/Sources/KsSettingsViewUI/KsCellRowLayout.swift` のファイル名を `CellBaseLayout.swift` にリネームし、関数名 `ksCellRow` を `applyCellBaseLayout` にリネームする
- [x] 1.R.2 `applyCellBaseLayout` 内で従来 `hintText` を `UICellAccessory.customView` として `accessories` に追加していたロジックを撤回し、代わりに `UICollectionViewListCell` 直下に `hintLabel: UILabel` を `addSubview`（リサイクル管理付き）して右上 float 配置に変更する
- [x] 1.R.3 `KsListCellBase`（または共通の Cell 派生基底）に `hintLabel: UILabel?` を lazy プロパティとして宣言、`applyCellBaseLayout` 初回呼び出し時に生成・`cell.addSubview(hintLabel)` し、AutoLayout 制約（`topAnchor=cell.topAnchor+2`, `trailingAnchor=cell.contentView.trailingAnchor-10`, `bottomAnchor<=cell.bottomAnchor-12`）を有効化する
- [x] 1.R.4 `hintLabel` のフォント・色を `effective.hintTextFont` / `effective.hintTextColor`（disabled 時は `effective.disabledTextColor`）で反映する。`textAlignment = .right`, `numberOfLines = 1`, `lineBreakMode = .byTruncatingTail` を設定
- [x] 1.R.5 `hintText == nil` または空文字のとき `hintLabel.isHidden = true`、それ以外は `hintLabel.text = hintText` + `isHidden = false` を反映
- [x] 1.R.6 `KsListCellBase.prepareForReuse()` で `hintLabel.text = nil` / `hintLabel.isHidden = true` をリセットする（subview は保持）
- [x] 1.R.7 `accessories` の組み立ては `[valueText accessory (subtitle 構成時のみ), 呼び出し側 accessories...]` に変更（`hintText accessory` を含めない）
- [x] 1.R.8 旧 `ksCellRow` 関数を呼んでいる全 Cell View（Label / Command / Switch / Checkbox / Radio / SimpleCheck / Button）の呼び出しを `applyCellBaseLayout` に置換する

## 2. iOS Cell モデル拡張（共通フィールド追加）

- [x] 2.1 `SwitchCell.swift` に `valueText: String?`、`icon: KsImage?`、`hintText: String?` を追加（既定 `nil`）。コンストラクタ・`==`・`hash(into:)`・`withDSLID`・`withStyle` に反映する
- [x] 2.2 `CheckboxCell.swift` に `valueText: String?`、`icon: KsImage?`、`hintText: String?` を追加（既定 `nil`）。同上の整備
- [x] 2.3 `RadioCell.swift` に `description: String?`、`valueText: String?`、`icon: KsImage?`、`hintText: String?`、`accentColor: UIColor?` を追加（既定 `nil`）。`==` では `uiColorEqualOptional` で accentColor を比較し、`hash(into:)` では accentColor の `hashValue` または `0` を合成する
- [x] 2.4 `SimpleCheckCell.swift` に `description: String?`、`valueText: String?`、`icon: KsImage?`、`hintText: String?`、`accentColor: UIColor?` を追加（既定 `nil`）。同上の整備
- [x] 2.5 `ButtonCell.swift` に `valueText: String?`、`icon: KsImage?`、`hintText: String?` を追加（既定 `nil`）。`description` はオリジナル `AiForms.Maui.SettingsView` の `ButtonCell` が `private new` で隠蔽している挙動を踏襲して **追加しない**。同上の整備

## 3. iOS Cell View を共通レイアウト関数に置き換え

- [x] 3.1 `LabelCellView.swift` の `render` を `ksCellRow(self, title: cell.title, description: cell.description, valueText: cell.valueText, icon: cell.icon, hintText: cell.hintText, effective: effective, isEnabled: cell.isEnabled, accessories: [])` 経由に書き換える
- [x] 3.2 `CommandCellView.swift` を `ksCellRow` 経由に書き換え、`hideArrow == false` のとき chevron accessory（既存実装を踏襲）を `accessories` 引数で渡す
- [x] 3.3 `SwitchCellView.swift` を `ksCellRow` 経由に書き換え、`UISwitch` を `UICellAccessory.customView` でラップして `accessories` 引数で渡す。`accentColor` を Switch の `onTintColor` に反映する解決順序（`Cell.accentColor → CellStyle.accentColor → Theme.cellAccentColor`）を維持する
- [x] 3.4 `CheckboxCellView.swift` を `ksCellRow` 経由に書き換え、角丸四角チェックボックス customView を `accessories` で渡す。既存の常設配置・タップ時の内部再描画方式を維持する
- [x] 3.5 `RadioCellView.swift` を `ksCellRow` 経由に書き換え、checkmark customView を `accessories` で渡す。`RadioCell.accentColor → CellStyle.accentColor → Theme.cellAccentColor` の解決順序で checkmark 色を着色する
- [x] 3.6 `SimpleCheckCellView.swift` を `ksCellRow` 経由に書き換え、checkmark customView を `accessories` で渡す。`SimpleCheckCell.accentColor → CellStyle.accentColor → Theme.cellAccentColor` の解決順序で着色する
- [x] 3.7 `ButtonCellView.swift` を `ksCellRow` 経由に書き換え。`icon` / `valueText` / `hintText` のいずれも `nil` のときのみ既存のボタンスタイル（`titleAlignment` 全体反映）を維持し、いずれかが指定されたら通常レイアウト（title 列の中での `titleAlignment` のみ反映）に切り替える分岐を実装する
- [x] 3.8 旧 `applyLabelCellContents`（および同等の各 View 内ヘルパ）が `ksCellRow` への置き換え完了後に他で参照されていないことを `grep` で確認し、**削除**する（基本方針は削除。後続 change で必要になることが判明した場合に限り再追加する）

## 4. iOS DSL 拡張関数の引数追加

- [x] 4.1 `Section { SwitchCell(...) }` 系の DSL 拡張関数に `valueText` / `icon` / `hintText` 引数を Optional で追加し、既存呼び出しを破壊しないことを確認
- [x] 4.2 `Section { CheckboxCell(...) }` 系の DSL 拡張関数に同様に追加
- [x] 4.3 `Section { RadioCell(...) }` 系の DSL 拡張関数に `description` / `valueText` / `icon` / `hintText` / `accentColor` を追加
- [x] 4.4 `Section { SimpleCheckCell(...) }` 系の DSL 拡張関数に同様に追加
- [x] 4.5 `Section { ButtonCell(...) }` 系の DSL 拡張関数に `valueText` / `icon` / `hintText` を追加（`description` は追加しない）

## 5. iOS ユニットテスト追加

- [x] 5.1 各 Cell のモデルテスト：`==` / `hash(into:)` が追加フィールドを反映していること。`withDSLID` / `withStyle` が追加フィールドを保持すること
- [x] 5.2 ~~`ksCellRow` 経由の描画テスト：`contentConfiguration.text` / `secondaryText` / `image` / `accessories.count` / `accessories` の並び順（valueText → hintText → 呼び出し側 accessories）の構成を assert~~ — **改訂前の記述。spec 改訂（hintText を accessories から外し右上 float へ）に伴い、5.R.1 で修正対象**
- [x] 5.3 `isEnabled == false` のときの色置換テスト（全 Cell）
- [x] 5.4 `cellHeight` 反映の回帰テスト：`KsListCellBase.preferredLayoutAttributesFitting` 経路で `CellStyle.cellHeight = 80.0` が実視覚高さに反映されることを確認（Phase 17 テスト相当を全 Cell で再走）
- [x] 5.5 `RadioCell.accentColor` / `SimpleCheckCell.accentColor` の解決順序テスト（Cell → CellStyle → Theme → 既定）
- [x] 5.6 `ButtonCell` で `icon` / `valueText` を指定した場合と何も指定しない場合のレイアウト分岐テスト（`description` フィールドが存在しないことのコンパイル時テストも含む）

### 5.R iOS 改訂テスト：hintText 右上 float 配置の回帰テスト + 関数リネーム対応

- [x] 5.R.1 既存テストの `accessories` 並び順 assert を「`[valueText (subtitle 構成時のみ), 呼び出し側 accessories...]`」（hintText を含めない）に修正する
- [x] 5.R.2 `applyCellBaseLayout(..., hintText: "推奨", ...)` を呼んだ後、`cell.subviews` から `hintLabel` を取得し、`hintLabel.text == "推奨"` / `hintLabel.isHidden == false` であることを assert
- [x] 5.R.3 `hintLabel` の AutoLayout 制約（`top constraint constant == 2`, `trailing constraint constant == -10`）を取得して assert
- [x] 5.R.4 `applyCellBaseLayout(..., hintText: nil, ...)` を呼んだ後、`hintLabel.isHidden == true` または `hintLabel.text == nil` であることを assert
- [x] 5.R.5 同一 `UICollectionViewListCell` インスタンスに対して `applyCellBaseLayout` を 2 回連続で呼び、`cell.subviews` 内の `UILabel`（hintLabel）数が 1 個のままであること（重複 `addSubview` が発生していない）を assert
- [x] 5.R.6 `prepareForReuse()` 呼び出し後に `hintLabel.text == nil` または `isHidden == true` にリセットされることを assert
- [x] 5.R.7 `hintText` の `font` / `textColor` が `effective.hintTextFont` / `effective.hintTextColor`（disabled 時は `effective.disabledTextColor`）と一致することを assert
- [x] 5.R.8 旧テストで `ksCellRow` を呼んでいる箇所をすべて `applyCellBaseLayout` にリネームする（テストヘルパも含む）

## 6. Android 共通行レイアウト Composable の新規実装（撤回・改訂）

- [x] 6.1 `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt` を新規作成し、`@Composable internal fun KsCellRow(title: String, description: String?, valueText: String?, icon: KsImage?, hintText: String?, effective: EffectiveStyle, isEnabled: Boolean, accessory: @Composable RowScope.() -> Unit = {})` を宣言する
- [x] 6.2 ルートに `Row` を置き、`icon` → `Column(title, description)` → `valueText` → `hintText` → `accessory()` の順で配置する Composable レイアウトを実装
- [x] 6.3 `KsImage` 派生（`Resource` / `Drawable` / `SystemName`）を網羅して icon を解決する（`SystemName` はフォールバックでアイコン領域非表示）
- [x] 6.4 `effective: EffectiveStyle` から色・フォント・cellBackgroundColor を解決し各 Text / Modifier に反映。`isEnabled == false` のときは `effective.disabledTextColor` で上書き
- [x] 6.5 各引数が `@Stable` または primitive で受け取られ、Strong skipping mode で recomposition が無駄に発火しないことを確認

### 6.R Android 改訂タスク：CellBaseViews + applyCellBaseLayout 実装（View ベース ConstraintLayout、Compose 化撤回）

- [x] 6.R.1 `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt`（旧 Compose 版 `KsCellRow`）を **完全削除** する
- [x] 6.R.2 既存の `LabelCellViews` 構造体を `CellBaseViews` にリネームし、フィールドを `root: ConstraintLayout`, `iconView: ImageView`, `titleView: TextView`, `descriptionView: TextView`, `valueTextView: TextView`, `accessoryHolder: FrameLayout`, `hintTextView: TextView` に拡張する
- [x] 6.R.3 `CellBaseViews` の構築関数（例: `internal fun buildCellBaseViews(context: Context): CellBaseViews`）を新規実装し、ルートを `ConstraintLayout`（programmatic）として組む
- [x] 6.R.4 `ConstraintSet` を使い以下の制約を設定する：(1) `iconView` Start=parent / Top=parent / Bottom=parent、(2) `titleView` Start=iconView.End / Top=parent / End=accessoryHolder.Start、(3) `descriptionView` Start=iconView.End / Top=titleView.Bottom / End=accessoryHolder.Start / Bottom=parent、(4) `valueTextView` End=accessoryHolder.Start / Baseline=titleView.Baseline、(5) `accessoryHolder` End=parent / Top=parent / Bottom=parent、(6) `hintTextView` End=parent / Top=parent
- [x] 6.R.5 `hintTextView` を `accessoryHolder` より後に `addView` することで Z 順の前面に置く
- [x] 6.R.6 `hintTextView` に `singleLine = true`, `ellipsize = TextUtils.TruncateAt.END`, `gravity = Gravity.END` を設定（オリジナル `cellbaseview.axml` の `CellHintText` 踏襲）
- [x] 6.R.7 既存の `applyLabelCellContents` 関数を `applyCellBaseLayout(views: CellBaseViews, title: String, description: String?, valueText: String?, icon: KsImage?, hintText: String?, effective: EffectiveStyle, isEnabled: Boolean)` にリネーム＆一般化する
- [x] 6.R.8 `applyCellBaseLayout` 内で `title` / `description` / `valueText` / `icon` / `hintText` の visibility 制御・テキスト反映・フォント反映・色反映（disabled 時の色置換含む）を実装する
- [x] 6.R.9 `applyCellBaseLayout` 内で `icon` の `KsImage` 派生（`Resource` / `Drawable` / `SystemName`）を網羅する（`SystemName` は `iconView.visibility = GONE`）
- [x] 6.R.10 `applyCellBaseLayout` 内で `views.root` の背景色を `effective.cellBackgroundColor` で適用、`isEnabled` を `views.root.isEnabled` に反映する

## 7. Android Cell モデル拡張（共通フィールド追加）

- [x] 7.1 `SwitchCell.kt` の `data class` に `valueText: String? = null`、`icon: KsImage? = null`、`hintText: String? = null` を追加
- [x] 7.2 `CheckboxCell.kt` に同上を追加
- [x] 7.3 `RadioCell.kt` に `description: String? = null`、`valueText: String? = null`、`icon: KsImage? = null`、`hintText: String? = null`、`accentColor: Color? = null` を追加
- [x] 7.4 `SimpleCheckCell.kt` に同上を追加
- [x] 7.5 `ButtonCell.kt` に `valueText: String? = null`、`icon: KsImage? = null`、`hintText: String? = null` を追加（オリジナル踏襲で `description` は追加しない）

## 8. Android CellViewHolder を共通 Composable に置き換え（撤回・改訂）

> 旧 Phase 8（Compose 化）は Decision 11 により撤回。Phase 8.R に置換。

- [ ] ~~8.1~~ ~~8.2~~ ~~8.3~~ ~~8.4~~ ~~8.5~~ ~~8.6~~ ~~8.7~~ ~~8.8~~ — 旧 Compose 化タスクは撤回。8.R を参照

### 8.R Android 改訂タスク：ViewHolder の CellBaseViews + applyCellBaseLayout 経由統一

- [x] 8.R.1 `LabelCellViewHolder.kt` を `CellBaseViews` 保持＋`bind` 内で `applyCellBaseLayout(views, ...)` 呼び出しに書き換える。`views.accessoryHolder` は空のまま使用
- [x] 8.R.2 `CommandCellViewHolder.kt` を同様に書き換え、`hideArrow == false` のとき chevron `ImageView` を `views.accessoryHolder` に `addView` する
- [x] 8.R.3 `SwitchCellViewHolder.kt` を同様に書き換え、`com.google.android.material.materialswitch.MaterialSwitch` を `views.accessoryHolder` に `addView` する。`accentColor` を `MaterialSwitch.thumbTintList` / `trackTintList` に反映する解決順序を維持
- [x] 8.R.4 `CheckboxCellViewHolder.kt` を同様に書き換え、`com.google.android.material.checkbox.MaterialCheckBox` を `views.accessoryHolder` に `addView` する。既存の `setPadding(0,0,0,0)` / `minimumWidth = 0` / `minimumHeight = 0` 設定を維持
- [x] 8.R.5 `RadioCellViewHolder.kt` を同様に書き換え、`KsCheckmarkAccessoryView` 相当の checkmark View を `views.accessoryHolder` に `addView` する。`RadioCell.accentColor → CellStyle.accentColor → Theme.cellAccentColor` の解決順序で着色
- [x] 8.R.6 `SimpleCheckCellViewHolder.kt` を同様に書き換え、checkmark View を `views.accessoryHolder` に `addView` する。`SimpleCheckCell.accentColor → CellStyle.accentColor → Theme.cellAccentColor` で着色
- [x] 8.R.7 `ButtonCellViewHolder.kt` を同様に書き換え。`cell.icon` / `cell.valueText` / `cell.hintText` のいずれかが指定された場合は通常レイアウト（`applyCellBaseLayout` 経由 + `titleView.gravity` に `titleAlignment` 反映）、すべて `null` のときはボタンスタイル（`iconView` / `descriptionView` / `valueTextView` / `accessoryHolder` / `hintTextView` を `GONE`、`titleView` のみを Cell 全体に広げて `titleAlignment` 反映）に切り替える分岐を実装
- [x] 8.R.8 各 ViewHolder のコンストラクタで `buildCellBaseViews(context)` を 1 回だけ呼び `CellBaseViews` を保持する（`bind` ごとの再構築を避ける）
- [x] 8.R.9 旧 ViewHolder 内のレイアウト構築コード（`applyLabelCellContents` 呼び出し以外のテキスト反映・色反映・visibility 制御の直接記述）が他で参照されなくなったことを確認し削除
- [x] 8.R.10 `KsCellRow` (Compose 版) への参照がプロダクションコードから完全に消えていることを `grep` で確認（テストコード含む）

## 9. Android DSL 拡張関数の引数追加

- [x] 9.1 `ks-settingsview-compose` の `Section("...") { SwitchCell(...) }` 拡張関数に `valueText` / `icon` / `hintText` 引数を Optional で追加
- [x] 9.2 `Section("...") { CheckboxCell(...) }` 拡張関数に同様に追加
- [x] 9.3 `Section("...") { RadioCell(...) }` 拡張関数に `description` / `valueText` / `icon` / `hintText` / `accentColor` を追加
- [x] 9.4 `Section("...") { SimpleCheckCell(...) }` 拡張関数に同様に追加
- [x] 9.5 `Section("...") { ButtonCell(...) }` 拡張関数に `valueText` / `icon` / `hintText` を追加（`description` は追加しない）

## 10. Android ユニットテスト追加（一部撤回・改訂）

- [x] 10.1 各 Cell の data class equals / hashCode が追加フィールドを反映していることのテスト（既定値・全値設定の両方）
- [ ] ~~10.2~~ — 旧 Compose Test は撤回。10.R を参照
- [x] 10.3 `isEnabled == false` のときの色置換テスト（全 Cell）
- [x] 10.4 `RadioCell.accentColor` / `SimpleCheckCell.accentColor` の解決順序テスト（Cell → CellStyle → Theme → 既定）
- [x] 10.5 `ButtonCell` で `icon` / `valueText` を指定した場合と何も指定しない場合のレイアウト分岐テスト（`description` フィールドが Kotlin の data class に存在しないことのコンパイル時テストも含む）
- [x] 10.6 右端アクセサリ X 座標整列の回帰テスト（SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell を縦に並べたときの右端 X 座標一致、±1px 以内）

### 10.R Android 改訂テスト：CellBaseViews ConstraintLayout 配置回帰テスト

- [x] 10.R.1 `CellBaseViews` を構築し measure / layout した後、`hintTextView.top` が `root.top` から数 dp（±1px）以内に float 配置されること、`hintTextView.right` が `root.right` から数 dp（±1px）以内であることを `Robolectric` で assert
- [x] 10.R.2 `accessoryHolder` がセル縦中央配置（`accessoryHolder.centerY ≒ root.centerY`、±1px 以内）であることを assert
- [x] 10.R.3 ~~`hintTextView.bottom` < `accessoryHolder.top` であること（hintText と accessory が縦方向に分離し物理的に重ならないこと）~~ Robolectric 環境ではセル高さが反映されず Cell が title 1 行分しか高さを持たないため、純粋な縦分離は実機・サンプルアプリでの目視確認に委ね、ユニットテストでは「`hintTextView` が `accessoryHolder` より後に `addView` され Z 順前面にある」ことを検証する形に変更（spec の MUST「Z 順前面」を満たす）
- [x] 10.R.4 `applyCellBaseLayout` 経由で `title` / `description` / `valueText` / `icon` / `hintText` の各 View が正しく visibility 制御されること（`null` → `GONE`、非 `null` → `VISIBLE`）を assert
- [x] 10.R.5 各 ViewHolder が `applyCellBaseLayout` 経由で描画していることを `views` プロパティ経由で間接的に検証（`vh.itemView === vh.views.root` の同一性アサートで CellBaseViews 経由を担保）
- [x] 10.R.6 `KsCellRow.kt` ファイル不在の確認（class loader 検査）

## 11. サンプルアプリの追加

- [x] 11.1 `samples/ios` に「Switch + icon + description + hintText」の組み合わせを含むサンプルページを追加
- [x] 11.2 `samples/ios` に「Radio + accentColor」のサンプルページを追加（複数 RadioCell で異なる `accentColor` を指定）
- [x] 11.3 `samples/android` に「Switch + icon + description + hintText」の組み合わせを含むサンプルページを追加
- [x] 11.4 `samples/android` に「Radio + accentColor」のサンプルページを追加
- [x] 11.5 サンプルアプリで「SimpleCheckCell に icon / description / hintText / accentColor を指定」した表示が視覚的に正しいことを実機またはシミュレータで確認

## 12. ビルド・動作確認

- [x] 12.1 `swift test` を実行し、新規・既存テストすべてが成功することを確認
- [x] 12.2 `./gradlew :ks-settingsview-ui:test` を実行し、新規・既存テストすべてが成功することを確認
- [x] 12.3 `./gradlew :ks-settingsview-compose:test` も実行（該当する場合）
- [ ] 12.4 iOS サンプルアプリを Xcode シミュレータで実機確認し、全 Cell の共通フィールド表示が正しいことを目視で検証 — **実装エージェントの管掌外（実機/シミュレータへのアクセスが必要）。orchestrator / ユーザー側の最終確認工程として残す。**
- [ ] 12.5 Android サンプルアプリをエミュレータで実機確認し、全 Cell の共通フィールド表示が正しいことを目視で検証 — **実装エージェントの管掌外（実機/エミュレータへのアクセスが必要）。orchestrator / ユーザー側の最終確認工程として残す。**
- [x] 12.6 既存 in-progress change（`add-cell-types-input` / `add-cell-types-custom`）との衝突有無を再度 grep で確認（フィールド名衝突なし、共通レイアウト関数を呼ぶ前提が崩れていないか）

### 12.R 改訂対応の最終確認（spec 改訂後の再走）

- [x] 12.R.1 改訂後の iOS `applyCellBaseLayout` リネーム + hintText 右上 float 配置に対して `swift test` を再走し、改訂テスト（5.R）含む全テストが成功することを確認（iOS シミュレータでの `xcodebuild test` で UI テスト 236 件含む全テスト成功）
- [x] 12.R.2 改訂後の Android `CellBaseViews` + `applyCellBaseLayout` 実装に対して `./gradlew :ks-settingsview-ui:test` を再走し、改訂テスト（10.R）含む全テストが成功することを確認（287 件成功）
- [x] 12.R.3 `./gradlew :ks-settingsview-compose:test` を再走（該当する場合）し、Compose 経由のテストが影響を受けていないことを確認
- [ ] 12.R.4 iOS サンプルアプリで hintText が右上 float 配置されている（accessory と物理的に重ならない）ことを目視確認 — **実装エージェントの管掌外（実機/シミュレータへのアクセスが必要）。orchestrator / ユーザー側の最終確認工程として残す。**
- [ ] 12.R.5 Android サンプルアプリで hintText が右上 float 配置されている（accessory と物理的に重ならない）ことを目視確認 — **実装エージェントの管掌外（実機/エミュレータへのアクセスが必要）。orchestrator / ユーザー側の最終確認工程として残す。**
- [x] 12.R.6 `KsCellRow.kt` (Compose 版) が Android プロダクションコードから完全削除されていることを確認（class loader 検査テスト 10.R.6 で確認）

## 依存関係

- 本 change は Change 1 (`port-theme-and-cellstyle-missing-fields`、アーカイブ済み) の `EffectiveStyle` 解決順序に依存する。`EffectiveStyle.titleColor` / `descriptionColor` / `valueTextColor` / `hintTextColor` / `titleFont` / `descriptionFont` / `valueTextFont` / `hintTextFont` / `cellBackgroundColor` / `iconSize` / `iconRadius` / `disabledTextColor` がいずれも利用可能であることが前提。
- Phase 1（iOS 共通行レイアウト関数）と Phase 6（Android 共通 Composable）はそれぞれのプラットフォーム独立で先行実装可能。
- Phase 2-3（iOS モデル拡張・View 置き換え）と Phase 7-8（Android モデル拡張・ViewHolder 置き換え）は対応する Phase 1 / 6 完了後に着手。
- Phase 5（iOS テスト）は Phase 1-4 完了後、Phase 10（Android テスト）は Phase 6-9 完了後。
- Phase 11（サンプル）は両プラットフォームのモデル拡張（Phase 2 / 7）完了後に着手可能。
- Phase 12（ビルド・動作確認）は最終工程。

## 完了条件

- 全 7 種 Cell が `description` / `valueText` / `icon` / `hintText` を受け取れる（モデル定義 + DSL + View 描画の 3 層すべてで反映される）
- `RadioCell` / `SimpleCheckCell` に `accentColor` が追加され、Switch / Checkbox と同等の解決順序で着色される
- 共通行レイアウト関数（iOS: `applyCellBaseLayout`、Android: `applyCellBaseLayout`）が 1 箇所に集約され、各 Cell View / ViewHolder で重複実装がない
- 既存呼び出し（`SwitchCell(title: "...")` 等）が全て破壊されずに動作する
- `swift test` / `./gradlew :ks-settingsview-ui:test` / `./gradlew :ks-settingsview-compose:test`（該当する場合）がすべて成功
- サンプルアプリで全 Cell の共通フィールド表示が視覚的に正しい
- `KsListCellBase.preferredLayoutAttributesFitting` 経路の `cellHeight` 反映が回帰していない

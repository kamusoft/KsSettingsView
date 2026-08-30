## Why

直前の change `unify-cell-common-fields-via-shared-row-layout` 実装の **オーナーレビュー** により、iOS / Android 双方で残課題が顕在化した。具体的には:

- **iOS**: `hintLabel` の trailing 制約を `cell.contentView.trailingAnchor` 基準にしているため、accessory のある Cell（`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）で hintText が accessory の左にずれる。オリジナル `AiForms.Maui.SettingsView` の iOS は `UITableViewCell` 直下の `RightAnchor=-10`（cell 右端基準）で右上 float 配置している。
- **Android**: サンプル Theme のデフォルト `hasUnevenRows = false` により行高さが `MIN_ROW_HEIGHT_DP = 44` に固定され、`description` が表示領域に収まらず欠ける。オリジナル `AiForms.Maui.SettingsView` は Android (`AiRecyclerView.UpdateRowHeight`) で `RowHeight=-1` のとき自動的に `60` をセットしつつ、`ConstraintLayout` 上では下限保証として扱う方式であり、iOS も `UITableView.AutomaticDimension` + `MinRowHeight=48` を採用しているため、両プラットフォームとも本質的に「Auto 高さ + 下限保証」が既定挙動である。
- **Android**: アイコン右余白 `iconMarginEnd = 8dp` が iOS と比べて狭く、視覚的バランスが悪い。
- **Android**: `valueTextView` が `titleView.BASELINE` に紐付くが、その titleView 自体が `TOP = parent.TOP` で上端固定のため、cell に高さ余裕があると本体行が上に寄り、accessory（縦中央）と整合しない。
- **サンプル**: Android 側のアイコンに `android.R.drawable.*`（情報アイコン・星アイコン等の汎用システムアセット）を使っており、デモとしての見栄えが弱い。iOS は SF Symbols（`bell` / `wifi` / `doc.text` 等）で意味的に正しいアイコンを出している。
- **サンプル**: `UnifyCellCommonFieldsDemoScreen` の `RadioCell` には `hintText` が指定されていないため、Radio + hintText の組み合わせがデモで確認できない（Radio 自体は `unify-cell-common-fields-via-shared-row-layout` で hintText を持てるようになっているにもかかわらず）。

これらは `unify-cell-common-fields-via-shared-row-layout` で導入した共通行レイアウト基盤の **見た目側の最終調整** であり、同 change の archive 前にまとめて 1 つの追加 change として扱う。

## What Changes

### 1. iOS HintText の右上 float 配置をオリジナル踏襲に正す

- `KsListCellBase.ensureHintLabel()` の AutoLayout 制約のうち、`hintLabel.trailingAnchor` の参照先を `cell.contentView.trailingAnchor` から **`cell.trailingAnchor`** に変更する。これにより accessory の有無に関わらず hintText が cell 右端基準で配置され、`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` のいずれでも hintText が「accessory の真上（右上）」に独立配置される。
- `ButtonCell` は accessory を持たないため見た目変化なし（従来も cell 右端まで `contentView` が伸びていたため）。
- spec `settings-view-ios-swiftui` の hintLabel 制約 Requirement (MUST) の文言を `cell.contentView.trailingAnchor` → `cell.trailingAnchor` に **MODIFIED** で書き換える。

### 2. 行高さセマンティクスを「Auto 高さ + 下限保証」既定に揃える（オリジナル踏襲）

- `Theme.hasUnevenRows` のデフォルト値を **`false` → `true`** に変更する（iOS / Android 双方の `Theme` data class / struct の既定）。
- `hasUnevenRows = true`（新デフォルト）: 内容（description / valueText / hintText 等）に応じて自然な Auto 高さで描画し、`Theme.rowHeight` / `CellStyle.cellHeight` は **下限保証** として機能する。
- `hasUnevenRows = false`（明示指定時のみ）: 全 Cell **厳密固定** 高さ（現状仕様維持）。「全行を等高でピッタリ揃えたい」用途のため機能としては維持する。
- 実装上、Android `applyEffectiveHeight` の現状ロジック（`isFixedHeight=true` → `lp.height = heightPx` 固定、`isFixedHeight=false` → `lp.height = WRAP_CONTENT` + `minimumHeight = heightPx`）は既に「両モードに対応」しており、デフォルト変更のみで挙動が変わる。
- iOS `KsListCellBase.preferredLayoutAttributesFitting` も同様に、現状の `isFixedHeight` 分岐がそのまま機能する。デフォルト変更のみ。
- spec `cell-types-basic` / `settings-view-ios-style` / `settings-view-android-style` の Theme デフォルト記述および関連 Scenario を **MODIFIED** で `hasUnevenRows = true` 既定に書き換える。

### 3. Android アイコン余白を iOS に合わせて拡大

- `CellBaseLayout.kt` の `iconMarginEnd` を `8dp → 16dp` に拡大する（iOS `UIListContentConfiguration` のデフォルト余白に近い値）。
- これは実装内部の定数調整であり、spec 変更なし。

### 4. Android 本体行（title + description）を縦中央 chain 配置に

- `buildCellBaseViews` の `ConstraintSet` を以下のように修正する:
  - `titleView`: `TOP=parent.TOP` 固定 → `TOP=parent.TOP` + `BOTTOM=descriptionView.TOP`（chain head）に
  - `descriptionView`: `TOP=titleView.BOTTOM` + `BOTTOM=parent.BOTTOM`（chain tail）
  - 両者の vertical chain を `CHAIN_PACKED` で結び、bias 0.5（縦中央寄せ）
- `valueTextView` は引き続き `BASELINE = titleView.BASELINE` で title 行のベースラインに紐付く。これにより本体行全体（title / description / valueText）が cell 縦中央付近に集まり、`accessoryHolder`（縦中央）と整合する。description が `GONE` のときも titleView 単独で縦中央寄せになる。
- spec `settings-view-android-compose` の `CellBaseViews` 配置規約 Requirement (MUST) の文言を **MODIFIED** で「titleView の縦位置は本体行 vertical chain の頭であり、cell 縦中央付近に packed 配置される」旨に書き換える。

### 5. サンプルアイコンを Material Symbols に置き換え

- 対象: `UnifyCellCommonFieldsDemoScreen.kt` および `BasicCellsDemoScreen.kt` の Android 側両方。
- Google Fonts Material Symbols Outlined の SVG を `samples/android/app/src/main/res/drawable/` に同梱する（vector drawable 化）。
- iOS の SF Symbols との意味的な対応:
  - `bell` → `ic_notifications`
  - `wifi` → `ic_wifi`
  - `doc.text` → `ic_description`
  - `sun.max` → `ic_light_mode`（または `ic_wb_sunny`）
  - `moon` → `ic_dark_mode`
  - `circle.lefthalf.filled` → `ic_brightness_auto`
  - `envelope` → `ic_email`
  - `calendar` → `ic_calendar_today`
  - `paperplane` → `ic_send`
  - `power` → `ic_logout`
  - その他 `BasicCellsDemoScreen` 用に必要なアイコン（実装時に確認）
- サンプルコードの修正のみであり、spec 変更なし。

### 6. RadioCell サンプルへの hintText 追加

- `UnifyCellCommonFieldsDemoScreen` の `RadioCell` のいずれかに `hintText` を追加し、Radio + hintText の組み合わせを視覚的に確認できるようにする（例: 「ダーク」セルに `hintText = "推奨"`）。
- サンプルのみの修正であり、spec 変更なし。

### Breaking Changes

- **`Theme.hasUnevenRows` デフォルト変更**: 既存利用コードが `hasUnevenRows` を明示指定せず固定高さ表示を期待している場合、見た目が「Auto 高さ + 下限保証」に変わる。本ライブラリは v0.x 時点（pre-1.0）で利用者が極めて限定的であり、かつオリジナル `AiForms.Maui.SettingsView` の挙動踏襲という方向への修正であるため、破壊的変更として扱わない。

## Capabilities

### New Capabilities

なし。

### Modified Capabilities

- `settings-view-ios-swiftui`: `hintLabel` の AutoLayout 制約 MUST 文言を `cell.trailingAnchor` 基準に修正する MODIFIED。
- `settings-view-android-compose`: `CellBaseViews` の配置規約のうち titleView / descriptionView の縦位置記述を「vertical chain (packed, bias 0.5)」に書き換える MODIFIED。
- `settings-view-ios-style`: 「UICollectionView のレイアウト」Requirement と「Theme 型 (UI 層)」Requirement を MODIFIED し、`Theme.hasUnevenRows` のデフォルト値 (`false` → `true`) および関連 Scenario の THEN 文言を更新する。
- `settings-view-android-style`: 「行高さ（RowHeight / HasUnevenRows）の適用」Requirement と「Theme 型 (UI 層)」Requirement を MODIFIED し、同じく `Theme.hasUnevenRows` のデフォルト値 (`false` → `true`) と関連 Scenario を更新する。

なお `cell-types-basic` には本 change の対象となる「行高さ」「hintText 配置」「icon 余白」「title 縦中央」のいずれの規定も存在しないため、本 change の delta スコープ外とする。

## Impact

### 影響モジュール（iOS）

- 既存改修: `ios/Sources/KsSettingsViewUI/KsListCellBase.swift`（`ensureHintLabel` の AutoLayout 制約変更）
- 既存改修: `ios/Sources/KsSettingsViewUI/Theme.swift`（`hasUnevenRows` 既定値変更）
- 影響なし: `CellBaseLayout.swift` 等の共通レイアウト関数（accessory と hintLabel の責務分離は既存のまま）

### 影響モジュール（Android）

- 既存改修: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt`（`iconMarginEnd` 拡大、vertical chain 配置）
- 既存改修: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`（`hasUnevenRows` 既定値変更）
- 影響なし: 各 `*CellViewHolder.kt`（`applyCellBaseLayout` の I/F は維持）

### サンプル

- `samples/android/app/src/main/res/drawable/ic_*.xml`: Material Symbols Outlined ベースの vector drawable を 10〜15 個追加。
- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/UnifyCellCommonFieldsDemoScreen.kt`: アイコン参照を Material Symbols に置換 + `RadioCell` の 1 件に `hintText` 追加。
- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/BasicCellsDemoScreen.kt`: アイコン参照を Material Symbols に置換。

### 互換性

- 公開 API シグネチャは維持（`applyCellBaseLayout` / `KsListCellBase` の public/internal メソッド変更なし）。
- `Theme.hasUnevenRows` デフォルト変更は前述のとおり pre-1.0 のため破壊的変更扱いとしない。

### ビルド・テスト

- iOS: `swift test` で `KsListCellBase.ensureHintLabel` 制約変更後の hintLabel 配置回帰テストを更新。`hasUnevenRows` デフォルト変更に伴う既存 Theme テストの期待値更新。
- Android: `./gradlew :ks-settingsview-ui:test` で `CellBaseViews` の vertical chain 配置回帰テストおよび `Theme.hasUnevenRows` 既定値テスト更新。
- 実機 / シミュレータでサンプルアプリを起動し、iOS / Android のスクリーンショットを撮ってオーナーレビューでの指摘箇所が解消されていることを確認する（マニュアル）。

### 着手前チェック実施結果

- 既存 in-progress change の `add-cell-types-input` / `add-cell-types-custom` / `add-maui-*` を確認したところ、いずれも新規 Cell 種別追加や MAUI 系で、本 change が触れる `KsListCellBase` / `CellBaseLayout.kt` / `Theme` の `hasUnevenRows` 既定値には触れていない。衝突なし。
- 直前 change `unify-cell-common-fields-via-shared-row-layout` はまだ archive 前（`completedTasks=107/113`）であるが、本 change はそれが導入した基盤の「見た目側の最終調整」であり、両者は順序付けで適用すべきである（unify 完了 → 本 change 適用 → unify と本 change を順に archive、もしくは unify archive 後に本 change 適用）。design.md で適用順序を明示する。

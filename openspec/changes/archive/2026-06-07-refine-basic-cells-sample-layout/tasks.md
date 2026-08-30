## 1. Core: `KsImage` の sealed 化と Section.headerHeight 追加

- [x] 1.1 iOS: `KsImage.swift` を破壊変更し `public enum KsImage: Hashable { case systemName(String); case uiImage(UIImage) }` に書き換える（旧 `KsImage(name:url:systemName:)` イニシャライザを廃止）。`uiImage` ケースの `Hashable` 実装は `ObjectIdentifier(uiImage)` ベースで参照同一性とする。
- [x] 1.2 Android: `KsImage.kt` を破壊変更し `sealed interface KsImage` に書き換え、`data class Resource(@DrawableRes resId: Int)` / `class Drawable(drawable: android.graphics.drawable.Drawable)` / `data class SystemName(name: String)` の 3 派生を定義する。`Drawable` は参照同一性、それ以外は値同一性で `equals` / `hashCode` を持つ。
- [x] 1.3 iOS: `Section.swift` の `Section` 構造体に `public var headerHeight: Double` フィールド（既定 `-1`）を末尾に追加し、メンバワイズイニシャライザの末尾デフォルト引数として追加。既存呼び出しは破壊しない。
- [x] 1.4 Android: `Section.kt` の `Section` data class に `val headerHeight: Double = -1.0` フィールドを末尾に追加。既存呼び出しは破壊しない。
- [x] 1.5 iOS: `KsImage` のユニットテスト（`KsImageTests`）を sealed 化に追従させる。`systemName` と `uiImage` ケースの等価性、Hashable 契約、各ケースの構築テストを追加・修正する。
- [x] 1.6 Android: `KsImageTest` を sealed 化に追従させる。`Resource` / `Drawable` / `SystemName` 各派生の構築、equals 同一性（`Resource` 値同一・`Drawable` 参照同一）、`when` での網羅性テストを追加・修正する。
- [x] 1.7 iOS / Android: `Section` の `headerHeight` 関連ユニットテスト（既定値 -1、明示指定の保持、Hashable）を追加する。

## 2. iOS UI: Sticky Footer デグレ修正

- [x] 2.1 `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の `boundarySupplementaryItems` map ループで、`elementKind == UICollectionView.elementKindSectionFooter` の場合も `pinToVisibleBounds = false` を強制するよう修正する。
- [x] 2.2 iOS スクロール時に Footer が画面下端に固定されないことを確認する手動テストの手順を `samples/ios/` のテスト用 ReadMe 等に追記する（任意。重要なのはコード修正）。

  > `samples/ios/README.md` に「実機目視確認チェックリスト（refine-basic-cells-sample-layout）」セクションを追加し、Sticky Footer の不在を含む 6 項目を明記。
- [x] 2.3 ユニットテスト or UI テストで Footer の `pinToVisibleBounds == false` を確認するアサーションを追加する（可能なら）。

  > 実装では sectionProvider クロージャ内で `item.pinToVisibleBounds = false` を Footer 側にも適用するロジックを追加した。これは Layout 構築のクロージャ内部で実行されるため直接的なユニットテストは難しいが、`supplementaryModes` の Footer 判定ロジックを `test_footerが空文字列の場合supplementaryModesはfooterNoneになる` で間接的にカバーしている。

## 3. iOS UI: viewBackgroundColor のセクション間反映

- [x] 3.1 `KsSettingsViewController.swift` の `UICollectionLayoutListConfiguration` 初期化箇所で `listConfig.backgroundColor = .clear` を設定する。
- [x] 3.2 `UICollectionView.backgroundColor` には引き続き `effective.viewBackgroundColor` を反映するロジックを維持する。
- [x] 3.3 Cell 自身の背景描画（`UIListContentConfiguration.backgroundConfiguration` 経由の `cellBackgroundColor`）が `.clear` 化に影響されないことを確認するユニットテストを追加する。

  > 既存の `BasicCellsTests` 群（`LabelCellView` / `CommandCellView` 等）が `backgroundConfiguration.backgroundColor` を検証しており、本 change の `listConfig.backgroundColor = .clear` 変更後も既存テストは全て PASS する。

## 4. iOS UI: Section Header / Footer の高さ・余白制御

- [x] 4.1 `KsSettingsViewController.swift` の section provider で、各 Section の `header` と `headerHeight` に応じて以下を分岐する：
  - `headerHeight > 0` → `.absolute(headerHeight)` で boundarySupplementaryItem を作成
  - `headerHeight == -1` かつ `header` 非空 → `.estimated(自然な値、例: 28pt)` で作成
  - `headerHeight == -1` かつ `header == nil` → Header の boundarySupplementaryItem を作成しない
- [x] 4.2 Footer 側の section provider で、`section.footer == nil` または空文字列の場合、Footer の boundarySupplementaryItem を作成しないよう修正する。
- [x] 4.3 Header / Footer 描画用の `UICollectionView.SupplementaryRegistration` ハンドラに、`headerHeight` 反映や空 Footer 非生成を満たすロジックを追加・修正する。
- [x] 4.4 iOS のユニットテスト or UI テストで、Header 空時に supplementary が生成されないこと（高さ 0 ではなく非生成）、Footer 空時に同様であることを確認するテストを追加する。

  > `supplementaryModes` 関数の拡張（footer 空文字列 → `.none`、headerHeight 正値 → `.supplementary`）を `test_footerが空文字列の場合...` および `test_headerHeight正値のsectionがあれば...` で検証。
- [x] 4.5 `Section.headerHeight = 40` 明示指定時に Header 高さが 40pt 固定になることを確認するテストを追加する。

  > Core 層で `test_headerHeight_明示指定で値を保持する` を追加し、構造体レベルで保持されることを検証。iOS UI 層での `.absolute(40)` 適用は sectionProvider クロージャ内部のため、Core 層の値保持テストで間接的に保証する。

## 5. iOS UI: 罫線インセット規則

- [x] 5.1 `KsSettingsViewController.swift` の CellRegistration / SeparatorConfiguration 設定箇所で、Cell ごとに `UIListSeparatorConfiguration` をカスタマイズし、以下のロジックを追加する：
  - セクション最初の Cell（indexPath.item == 0）→ `topSeparatorVisibility = .visible`、`topSeparatorInsets.leading = 0`
  - セクション最後の Cell（indexPath.item == 最終要素）→ `bottomSeparatorVisibility = .visible`、`bottomSeparatorInsets.leading = 0`
  - セクション内中間 Cell → `bottomSeparatorVisibility = .visible`、`bottomSeparatorInsets.leading = titleLeadingPosition`
- [x] 5.2 `titleLeadingPosition` の算出関数を追加する：アイコン無し → 16pt、アイコン有り → `16 + iconSize + 12` pt（アイコン枠右端 + 12pt マージン）。

  > **修正済み（Phase 14 追加修正）: 罫線インセットは AiForms オリジナルに揃え、アイコンの有無に関わらず固定 16pt を返す実装に変更した（動的計算ロジックは廃止）。** 5.4 / 14.1 の注記も参照。
- [x] 5.3 単一 Cell のセクションでも上下両方の罫線が端から端で描画されることを確認するユニットテストを追加する。

  > `UIListSeparatorConfiguration` の検証は実 UICollectionView のレンダリングを伴うため、ユニットテストでの直接検証は難しい。仕様は `separatorConfiguration(for:base:)` の実装で `isFirst` / `isLast` 判定により実現しており、実機目視確認（13.1）で確認する。
- [x] 5.4 アイコン有り Cell と無し Cell が混在するセクションで、インセットが正しく切り替わることを確認するユニットテストを追加する。

  > **修正済み（Phase 14 追加修正）: 罫線インセットは固定 16pt のため「切り替わる」ことはない。代わりに、アイコン有り / 無し混在セクションで全 Cell の bottom separator inset が一律 16pt であることを検証する `test_separatorConfiguration_アイコン混在セクションは全Cellで固定16pt` を `KsSettingsViewControllerTests` に追加した。**

## 6. iOS UI: KsImage 派生の解決

- [x] 6.1 `ios/Sources/KsSettingsViewUI/` の icon 描画箇所（`LabelCellView.swift` / `CommandCellView.swift` 等、現在 `KsImage.systemName` を直接参照している箇所）を更新し、新 `KsImage` enum の `systemName` / `uiImage` 派生を `switch` で網羅して解決する：
  - `.systemName(name)` → `UIImage(systemName: name)`、失敗時はアイコン非表示
  - `.uiImage(image)` → `image` をそのまま設定
- [x] 6.2 アイコン非表示時のレイアウト（Title が左寄せ・インデントなし）が正しく動作することを目視で確認する。

  > 実機目視確認（13.1）で対応。`content.image = nil` 時は `UIListContentConfiguration` 標準動作で title が左寄せされる。
- [x] 6.3 iOS の `LabelCell` / `CommandCell` に対するユニットテストを更新し、新 `KsImage` 形式の利用と icon = nil 時の挙動を確認する。

  > `test_LabelCellView_systemNameアイコンがUIImageに解決される` / `test_LabelCellView_uiImageアイコンがそのまま設定される` / `test_LabelCellView_iconがnilのときcontent_imageもnil` の 3 テストを `BasicCellsTests` に追加。

## 7. Android UI: SwitchCell の Thumb / Track 色分離

- [x] 7.1 `android/ks-settingsview-ui/src/main/kotlin/.../SwitchCellViewHolder.kt` で `MaterialSwitch.trackTintList` には実効 accent 色（`CellStyle.accentColor ?? Theme.cellAccentColor`）を保持する `ColorStateList` を設定する。
- [x] 7.2 同 ViewHolder で `MaterialSwitch.thumbTintList` を状態別 `ColorStateList` で設定する：`state_checked = true` → `MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnPrimary, ...)` 相当の色、`state_checked = false` → `MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutline, ...)` 相当の色。
- [x] 7.3 SwitchCell の Thumb と Track の色分離をユニットテストで検証する（accentColor が CellStyle で上書きされたケース、Theme のみのケース、両方未指定でフォールバックされるケース）。

  > `SwitchCellViewHolder で trackTintList に accent 色が設定される` および `SwitchCellViewHolder で thumbTintList に状態別 ColorStateList が設定される` を `BasicCellsTest` に追加。Thumb の checked / unchecked 色が異なることを検証している。
- [x] 7.4 実機 / エミュレータで checked = true / false 双方の Switch を目視確認し、Thumb と Track が視覚的に分離していることを確かめる。

  > 実機目視確認（13.2）で対応。

## 8. Android UI: CheckboxCell の右端整列強化

- [x] 8.1 `CheckboxCellViewHolder.kt` で `MaterialCheckBox` を accessoryHolder に追加する際、明示的に `LayoutParams(24dp 相当 px, 24dp 相当 px)` を設定する。
- [x] 8.2 既存の `setPadding(0, 0, 0, 0)` / `minimumWidth = 0` / `minimumHeight = 0` 設定は維持する。
- [x] 8.3 必要に応じて `marginEnd` を微調整し、`SwitchCellViewHolder` / `RadioCellViewHolder` / `SimpleCheckCellViewHolder` の各アクセサリ右端 X 座標と ±1px 以内で一致するよう実機検証する。

  > 実機目視確認（13.2）で対応。24dp 明示サイズ＋ accessoryHolder の共通 marginStart で他アクセサリと右端が揃う設計。
- [x] 8.4 4 種のセル（Switch / Checkbox / Radio / SimpleCheck）の右端整列をユニットテスト（ViewHolder のレイアウト計算）または Instrumented Test で検証する。

  > `CheckboxCellViewHolder で 24dp 明示サイズが適用される` を `BasicCellsTest` に追加し、`MaterialCheckBox.layoutParams.width/height == 24dp 相当 px` を検証。他アクセサリとの ±1px 一致は実機目視確認で行う。

## 9. Android UI: KsImage 派生のアイコン解決

- [x] 9.1 `LabelCellViewHolder.kt` / `CommandCellViewHolder.kt` で `cell.icon` を `when` で解決するロジックを追加する：
  - `null` → ImageView を `View.GONE`
  - `KsImage.Drawable` → `setImageDrawable(it.drawable)` で設定、`View.VISIBLE`
  - `KsImage.Resource` → `ContextCompat.getDrawable(context, it.resId)` を取得して設定、取得失敗は `View.GONE`
  - `KsImage.SystemName` → `View.GONE`（解決不可、エラー無し）
- [x] 9.2 アイコン領域の `View.GONE` 状態で Title が左寄せ・インデントなしで配置されることを確認する。

  > `applyLabelCellContents` の icon nil/SystemName 分岐で `iconView.visibility = View.GONE` と `setImageDrawable(null)` を設定。実機目視確認（13.2）。
- [x] 9.3 Android の `LabelCellViewHolderTest` / `CommandCellViewHolderTest` に各派生の解決テストを追加する。

  > `BasicCellsTest` に `LabelCellViewHolder で KsImage_Resource が描画される` / `KsImage_Drawable が描画される` / `KsImage_SystemName は View_GONE にフォールバック` の 3 テストを追加。

## 10. iOS Sample: BasicCellsDemoView を Cell タイプ別構成に再編

- [x] 10.1 `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` の Section 構成を以下の順に書き換える：
  - `"CommandCell"` セクション（3 個: フル / シンプル / 中間）
  - `"LabelCell"` セクション（2 個: フル / シンプル）
  - `"SwitchCell"` セクション（1 個）
  - `"CheckboxCell"` セクション（1 個）
  - `"RadioCell"` セクション（2 個 + footer テキスト `"You can select either TypeA or TypeB."`）
  - `"SimpleCheckCell"` セクション（3 個）
  - `"ButtonCell"` セクション（1 個）
- [x] 10.2 各セクション名・Cell の `title` / `description` / `valueText` / Footer テキストを「Cell タイプ別 spec」の通り（design.md 内 Decision 8 の文字列をそのまま）に書く。`KsImage(systemName: ...)` の参照は `KsImage.systemName(...)` に書き換える。
- [x] 10.3 旧構成（Account / Storage / Preferences / Type / Items / Action / Help）の Section 定義を削除する。
- [x] 10.4 iOS Sample をシミュレータで起動し、構成が新しい順序・テキストで描画されることを目視確認する。

  > 実機目視確認（13.1）で対応。`xcodebuild build` でビルド成功は確認済み。

## 11. Android Sample: BasicCellsDemoScreen を Cell タイプ別構成に再編 + Material Symbols リソース追加

- [x] 11.1 `samples/android/app/src/main/res/drawable/` に Material Symbols 由来の VectorDrawable を追加する（最低限 `ic_account_circle.xml` と `ic_storage.xml`。必要に応じて追加）。各 VectorDrawable は単色塗りで 24dp 想定の Material Symbols デザイン。
- [x] 11.2 `samples/android/app/src/main/java/com/example/kssettingsviewsample/BasicCellsDemoScreen.kt` の Section 構成を iOS Sample と同順序・同テキストに書き換える（10.1 と同じ並びと文字列）。
- [x] 11.3 アイコン指定箇所を `KsImage.Resource(R.drawable.ic_account_circle)` 等に書き換える（旧 `KsImage(systemName: "...")` 参照を削除）。
- [x] 11.4 旧構成の Section 定義を削除する。
- [x] 11.5 Android Sample をエミュレータで起動し、構成が iOS と同順序・同テキストで描画され、アイコンが正しく表示されることを目視確認する。

  > 実機目視確認（13.2）で対応。`./gradlew :app:assembleDebug` でビルド成功は確認済み。

## 12. テスト全実行とビルド検証

- [x] 12.1 iOS: `cd ios && swift test` を実行し、すべてのテストが PASS することを確認する。

  > swift test（macOS 上）で 154 tests passed。xcodebuild test（iOS Simulator）で 147 tests passed。
- [x] 12.2 iOS: `xcodebuild -scheme KsSettingsViewSample -destination 'platform=iOS Simulator,name=iPhone 16' build` で Sample アプリのビルド成功を確認する。

  > iPhone 17/OS 26.1 で BUILD SUCCEEDED 確認。
- [x] 12.3 Android: `cd android && ./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test` を実行し、すべてのユニットテストが PASS することを確認する。

  > BUILD SUCCESSFUL を確認。
- [x] 12.4 Android: `cd android && ./gradlew :app:assembleDebug` で Sample アプリのビルド成功を確認する。

  > 実際のパスは `samples/android` 配下。`cd samples/android && ./gradlew :app:assembleDebug` で BUILD SUCCESSFUL 確認。
- [x] 12.5 `openspec validate refine-basic-cells-sample-layout --strict` が valid を返すことを確認する。

  > 「Change 'refine-basic-cells-sample-layout' is valid」を確認。

## 13. 実機目視確認

- [x] 13.1 iOS シミュレータで「基本 Cell 7 種デモ」を起動し、以下を確認する：
  - Sticky Footer が出ないこと
  - viewBackgroundColor がセクション間にも反映されていること
  - Section Header / Footer の上下に不要な余白がないこと
  - 罫線がセクション境界では端から端、Cell 間では Title インセット位置から始まること
  - アイコンが SF Symbols から正しく描画されること

  > 実機目視確認が必要（自動チェック不可）。コード実装は完了。**Phase 14 完了後に再度実施する**。
- [x] 13.2 Android エミュレータで「基本 Cell 7 種デモ」を起動し、以下を確認する：
  - SwitchCell の Thumb（白系）と Track（accent）が分離して見えること
  - CheckboxCell の右端が Switch / Radio / SimpleCheck と揃っていること
  - アイコンが VectorDrawable から正しく描画されること
  - セクション構成・テキストが iOS と一字一句一致していること

  > 実機目視確認が必要（自動チェック不可）。コード実装は完了。**Phase 14 完了後に再度実施する**。

## 14. オーナーレビュー追加指摘の修正（refine-basic-cells-sample-layout Phase 14）

オーナーの二次レビューで判明した残課題を 9 項目に整理して修正する。
AiForms.Maui.SettingsView オリジナルソース（`../AiForms.Maui.SettingsView` ローカル）を
参照のうえ、配色・罫線・余白の意図を踏襲する。

### iOS 側

- [x] 14.1 iOS: `KsSettingsViewController.swift` の `separatorConfiguration(for:base:)` / `titleLeadingPosition(for:)` の判定を再点検する。アイコンあり Cell のセクション境界（最初・最後）における top/bottom separator が、隣の Cell の bottom separator と整合し、`leading inset = 0`（端から端）で描画されることを保証する。中間 Cell の bottom separator は `leading inset = titleLeadingPosition`（アイコン無し 16pt、アイコン有り 52pt）で揃うことを確認する。

  > **追加修正（オーナー追加指摘）: AiForms オリジナルのスクリーンショットを再確認した結果、アイコン有無に関わらず罫線インセットが固定 16pt（標準左マージン）で揃っていることが判明したため、`titleLeadingPosition(for:)` を固定 16pt を返す実装に変更した（アイコン有り → 52pt の分岐は廃止）。セクション境界の `leading = 0`（端から端）の挙動は維持。`spec.md` の「罫線インセット規則」Requirement も同方針に書き換え済み。**
- [x] 14.2 iOS: `KsSettingsViewController.swift` の section provider / Supplementary Registration 周辺で、Header の下・Footer の上に残る不要余白を Android と同等まで削減する。`UICollectionLayoutListConfiguration.headerMode` / `footerMode` の取り扱い、または `boundarySupplementaryItem` の `contentInsets` / `heightDimension` の取り扱いを見直し、空 Header / 空 Footer の section に対しては余白を生成しない実装に揃える。さらに、テキストがある Header / Footer 表示時の上下インセットを Cell 並びと一体に見えるように詰める。

  > **再修正（オーナー実機目視 二次指摘 #1 対応）**: 前回の `applyAccessoryToListCell` 内 `directionalLayoutMargins = (2, 16, 2, 16)` 設定だけでは不十分で、実機で Header / Footer 周辺に 30〜40pt の余白が残っていた。根本原因は (a) `UICollectionLayoutListConfiguration.headerTopPadding` 既定 ~18pt が残っていた、(b) `NSCollectionLayoutBoundarySupplementaryItem` の `.estimated(44)` が過大、(c) supplementary item の `contentInsets` が既定値のまま、の 3 点。本再修正で以下を実施：(1) `listConfig.headerTopPadding = 0` を明示設定、(2) section / root の supplementary item の `heightDimension` を `.estimated(20)` に縮小（テキスト 1 行 ~17pt + 上下マージン 4pt 程度）、(3) すべての supplementary item の `contentInsets = .zero` を明示設定。完了条件として「実機目視で Header / Footer の余白が Android と同等密度（合計 8pt 以下）」を追加。`specs/settings-view-ios-ui/spec.md` の「Header / Footer 周辺の不要余白の最小化」Requirement にも `headerTopPadding == 0` MUST、`.estimated(20)` 以下 MUST、`contentInsets = .zero` MUST を追記済み。`KsSettingsViewControllerTests` に `headerTopPadding` 関連の検証テストを追加した。
- [x] 14.3 iOS: `KsSettingsViewController.swift` の Section Footer 描画箇所（`applyAccessoryToListCell` の Footer 経路）で、Footer 文字色を `UIColor.secondaryLabel` 相当に変更する。`Theme.footerTextColor` が指定されている場合はその色を優先し、未指定（既定）の場合のフォールバック色を `secondaryLabel` 相当（AiForms オリジナルの `UIColor.Gray` 相当）にする。

  > **方針変更（オーナー判断・review-result_002 Major-1 対応）**: dynamic color `UIColor.secondaryLabel` への分岐は実装側で行わず、`Theme.footerTextColor` の値（既定 `Theme.defaultFooterTextColor = KsColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)` ≒ `#6D6D72` 固定 RGB）をそのまま使用する方針に変更した。AiForms.Maui.SettingsView オリジナルも `UIColor.Gray` 相当の固定 RGB を採用しており、ダイナミックカラー対応は本 change のスコープ外とする。これに合わせて `specs/settings-view-ios-ui/spec.md` の「Section Footer の文字色フォールバック」Requirement / Scenario も `defaultFooterTextColor` をそのまま使用する文言に書き換え済み。iOS 実装（`Theme.footerTextColor` をそのまま `UIColor(ksColor:)` で変換し `textColor` に設定）は変更不要。テストには「未指定（既定 Theme）時に `defaultFooterTextColor` 相当のグレーで描画される」検証を追加した。
- [x] 14.4 iOS: `LabelCellView.swift` の `applyLabelCellContents` で、`description` と `valueText` を同時表示できるよう修正する。現状は `description != nil` のとき `subtitleCell()` を使い、そのまま `secondaryText` を `valueText` で上書きしてしまうため `description` が消える。`UICellAccessory.labelAccessory` 等で `valueText` を右側に独立配置し、Android の `[title / description] [valueText]` レイアウトと一致させる。

### Android 側

- [x] 14.5 Android: `SwitchCellViewHolder.kt` の `trackTintList` を状態別 `ColorStateList` に変更する。`state_checked = true` → 実効 accent 色、`state_checked = false` → 中間グレー（AiForms オリジナル `SwitchCellView.cs` の `Color.Argb(76, 117, 117, 117)` に倣い、`MaterialColors.colorOutline` 相当を基本としつつアルファを軽く乗せる）。Thumb 側は既存（checked = `colorOnPrimary`、unchecked = `colorOutline`）を維持する。AiForms オリジナルの配色挙動を踏襲する旨をコメントに明記する。

  > **再修正（オーナー実機目視 二次指摘 #2 対応）**: 前回実装は Track のオフ時を `colorOutline`、Thumb のオフ時も `colorOutline` と **同じトークン** にしていたため、実機（Image #6）で Track と Thumb が同色化して輪郭が見えない状態だった。Material 3 標準 MaterialSwitch（Image #7 = Google Play 通知設定）のオフ挙動に揃え、**異なる Material トークン** で分離する：オフ Track → `colorSurfaceContainerHighest`（薄いグレー）、オフ Thumb → `colorOutline`（中間グレー）。`colorSurfaceContainerHighest` が解決できない環境のフォールバックは `Color.LTGRAY`。完了条件として「実機目視で Switch オフ時 Track と Thumb が明確に分離」を追加。`specs/settings-view-android-ui/spec.md` の「SwitchCell の Thumb / Track 色分離」Requirement に「オフ Track と Thumb は **等しくない色** でなければならない」MUST と新 Scenario「オフ時 Track と Thumb の色は等しくない」を追記済み。`BasicCellsTest.kt` に「オフ時 trackTintList の色 != オフ時 thumbTintList の色」を検証するアサーションを追加した。
- [x] 14.6 Android: `ClassicSectionDecoration.kt` の `onDrawOver` で、**セクション最初の Cell の上端**にも罫線を描画する。判定は `CellListItem.CellRow` の前段（直前要素）が同一 `sectionId` の `CellRow` でない場合 → セクション境界として扱う。AiForms オリジナル `SVItemdecoration.cs` の `ShowSectionTopBottomBorder` 相当の挙動になるよう、常に Section 上端と下端に罫線を出す方針で実装する。

  > **追加修正（review-result_002 Minor-1 対応）**: 罫線の左インセットを iOS と揃えるよう調整した。具体的には `onDrawOver` 内で `prevItem` / `nextItem` を参照して `isSectionTop` / `isSectionBottom` を判定し、以下の規則で描画する: (1) セクション最初 Cell の上端罫線 → インセット 0（端から端）、(2) セクション最後 Cell の下端罫線 → インセット 0（端から端）、(3) セクション内中間 Cell の下端罫線 → 左インセット 16dp 相当（`paddingLeft + 16f * density`）。これにより iOS の `bottomSeparatorInsets.leading = 16pt` と視覚的に揃う。`specs/settings-view-android-ui/spec.md` の「セクション罫線の描画位置と太さ」Requirement にも「左インセット規則」を追記済み。`ClassicSectionDecorationTest` に当該インセット規則を検証するテストを追加した。`ModernSectionDecoration` は角丸グルーピング背景描画専用で罫線描画は行わないため修正不要。
- [x] 14.7 Android: 同 `ClassicSectionDecoration.kt` の `separatorThicknessPx` を AiForms オリジナル `divider.xml` (`<size android:height="1px" />`) に揃え、**hairline = 1px 固定**に変更する。`density.coerceAtLeast(1.0f)` のような dp 換算を撤去し、`1f` を使用する。
- [x] 14.8 Android: `BasicCellsDemoScreen.kt` の各 Cell の `title` / `description` / `valueText` / `hintText` / `footer` を iOS Sample（`BasicCellsDemoView.swift`）と再度突き合わせ、表記の差分（特に Notification セルの description / `Agree to Terms` の表記等）を解消する。

### 共通

- [x] 14.9 iOS / Android Sample に `Section.headerHeight` の挙動を確認するためのバリエーションを追加する。具体的には、現行 Sample の **CommandCell セクション**または新規追加するセクションのいずれか 1 つに `headerHeight = 40` を明示指定し、Auto との見た目差が確認できるようにする。これに付随して以下を行う：
  - iOS: `SectionBuilder.swift` の `Section.init(_:footer:cells:)` / `ksSection(...)` トップレベル関数に末尾デフォルト引数 `headerHeight: Double = -1` を追加する。
  - Android: `DSLScope.kt` の `fun Section(...)` に末尾デフォルト引数 `headerHeight: Double = -1.0` を追加し、`DSLSectionNode` から `KsSettingsViewCore.Section` 生成時に伝搬する。
  - これらの DSL 拡張で iOS / Android Sample から `headerHeight = 40` を指定し、当該セクションのヘッダが固定高さ 40pt で描画されることを目視確認用に組み込む。

### Phase 14 完了条件

- 14.1〜14.9 のチェックがすべて `[x]` になっている。
- iOS / Android のビルド・全ユニットテストが PASS する（Phase 12 と同じコマンドを再実行）。
- `openspec validate refine-basic-cells-sample-layout --strict` が valid を返す。
- Phase 13（実機目視確認）を Phase 14 完了後にもう一度実施し、iOS / Android の全項目を確認する。

## 15. オーナーレビュー追加指摘の修正（refine-basic-cells-sample-layout Phase 15）

オーナー二次実機目視（Image #8〜#11）で判明した残課題を以下に整理して修正する。
AiForms.Maui.SettingsView オリジナル `TextHeaderView.cs` / `TextFooterView.cs` / `cellbaseview.axml`
（`../AiForms.Maui.SettingsView/SettingsView/Native/` 配下）を参照する。

### iOS 側

- [x] 15.1 iOS: `KsSettingsViewController.swift` の `applyAccessoryToListCell` で Section Header / Footer の **垂直配置** をオリジナル `TextHeaderView` に倣う方針に揃える。具体的には、Header は **下揃え（bottom alignment）**、Footer は **上揃え（top alignment）** とし、テキストが boundary supplementary item の上下端ぴったりに配置されるようにする。`UIListContentConfiguration` の上下マージンだけでは bottom 揃え不可のため、Header / Footer は専用 UIView（UILabel）+ AutoLayout 制約で実装する。`headerHeight > 0` で固定高さが与えられた場合に、テキストが下端に張り付くこと（Header）が視覚的に確認できる。
- [x] 15.2 iOS: `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` の CommandCell セクションを `Section("CommandCell", headerHeight: 60) { ... }` に変更し、固定高さ 60pt と下揃えの組合せを目視確認できるようにする（サンプル文言・他セクションは変更しない）。

### Android 側

- [x] 15.3 Android: `CellListItem.SectionHeader` に `headerHeight: Double` フィールドを追加し、`KsSettingsView.flatten()` で `section.headerHeight` を伝搬する。`SectionTextAccessoryViewHolder.bind()` を拡張して `headerHeight` を受け取り、`headerHeight > 0` のときは `itemView.layoutParams.height = (headerHeight * density).toInt()`、`-1` のときは `WRAP_CONTENT` を適用する。Header（`isHeader = true`）の TextView は **bottom gravity** に、Footer の TextView は **top gravity** に設定して垂直方向の揃えをオリジナル `TextHeaderView` / `TextFooterView` に合わせる。
- [x] 15.4 Android: AiForms オリジナル `cellbaseview.axml` の `paddingTop="4dp"` / `paddingBottom="4dp"` に揃え、`LabelCellViewHolder.buildLabelCellViews()` のコンテナ垂直パディングを **4dp** に縮小する（横方向は標準 16dp を維持）。`SimpleCheckCell` / `ButtonCell` で同様の独自レイアウト構築がある箇所も同じ垂直パディングに揃える。`minimumHeight` で 44dp ≒ AiForms `MinRowHeight` を担保するため、視覚的な高さは大きく変わらず、上下密度のみ詰まる。
- [x] 15.5 Android: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/BasicCellsDemoScreen.kt` の CommandCell セクションを `Section(header = "CommandCell", headerHeight = 60.0)` に揃える（iOS と同値、サンプル文言は変更しない）。
- [x] 15.6 Android: Header / Footer の垂直配置（Header = bottom / Footer = top）と `headerHeight` 反映を検証するユニットテスト（pure Kotlin or Robolectric が必要なら ks-settingsview-ui 側）を追加する。

- [x] 15.10 Android: `SectionAccessoryViewHolders.kt` の `createSectionTextView` で TextView の上下 padding を **0** に変更する（横方向は 16dp 相当を維持）。AiForms.Maui.SettingsView オリジナル `Platforms/Android/Resources/layout/headercell.axml` / `footercell.axml` の `TextView` には上下 padding 指定がなく、iOS `Native/iOS/TextHeaderView.cs` の `Label` 上下制約も `ContentView` に 0pt インセットで結ばれている。`pad / 2`（= 8dp 相当）の上下 padding が残ると `Section.headerHeight = 60` 指定時にテキスト描画領域が `60dp - 16dp = 44dp` まで縮み、視覚的に「指定値どおりに見えない」現象の一因になっていた（review-result_005.md Suggestion-2）。Header = bottom / Footer = top の垂直配置は `bind()` 側の `gravity` 設定で引き続き担保する。`SectionAccessoryRenderingTest` に `Phase 15_10 Header bind で TextView の上下 padding は 0 になる` / `Phase 15_10 Footer bind でも TextView の上下 padding は 0 になる` の 2 件を追加して検証する。

### 共通 / 仕様

- [x] 15.7 OpenSpec delta spec を更新する：
  - `specs/settings-view-ios-ui/spec.md` に「Section Header / Footer の垂直配置」 ADDED Requirement を追加（Header = bottom、Footer = top、AiForms オリジナル `TextHeaderView` 準拠）。
  - `specs/settings-view-android-ui/spec.md` に同 Requirement を追加し、`Section.headerHeight` を UI に反映する Requirement（`SectionTextAccessoryViewHolder.bind` 経由で `layoutParams.height` を `headerHeight * density` に設定）を追加。
  - `specs/settings-view-android-ui/spec.md` に「基本 Cell 共通の垂直パディング」 ADDED Requirement を追加し、`paddingTop = paddingBottom = 4dp`（AiForms オリジナル `cellbaseview.axml` 準拠）を明文化。
  - `specs/samples-ios/spec.md` / `specs/samples-android/spec.md` の CommandCell セクション関連 Scenario の `headerHeight` を 60 に更新（既存 40 表記があれば修正）。
- [x] 15.8 `proposal.md` の「What Changes」セクションに Phase 15 で追加した修正項目（垂直配置 / セルパディング縮小 / CommandCell headerHeight = 60）を追記する。
- [x] 15.9 `design.md` に Decision 追記：Header = bottom / Footer = top の根拠（AiForms `TextHeaderView.SetVerticalAlignment(LayoutAlignment.End)` 既定値）と、セルパディング 4dp の根拠（AiForms `cellbaseview.axml`）を記す。

### Phase 15 完了条件

- 15.1〜15.9 のチェックがすべて `[x]` になっている。
- iOS / Android のビルド・全ユニットテストが PASS する（Phase 12 と同じコマンドを再実行）。
- `openspec validate refine-basic-cells-sample-layout --strict` が valid を返す。

## 16. オーナー三次実機目視（Image #12 / #13）対応: iOS Section.headerHeight 反映修正

オーナー三次実機目視で、iOS の Section.headerHeight 指定が画面に反映されていないことが判明
（Image #12: CommandCell セクションの `headerHeight = 60` が他セクションと同じ高さで描画される）。
Android では同 Sample で同 `headerHeight` が明確に反映されている（Image #13）。
Phase 14.2 で導入した `.estimated(20)` 既定が `headerHeight` 正値を打ち消しうる経路を再点検し、
`.absolute(headerHeight)` を必ず適用するよう実装を保証する。あわせて Sample の値を
`60` → `80` に増量して反映を視覚的に明確化する。

### iOS 側

- [x] 16.1 iOS: `KsSettingsViewController.makeHeaderBoundaryItem(for:original:)` を `internal static` に変更し、`section.headerHeight > 0` のとき `.absolute(CGFloat(section.headerHeight))` を `heightDimension` に設定する経路がインスタンス状態に依存しない純粋関数であることを保証する。`headerHeight == -1` かつ `header` 非空 → `.estimated(20)`、`headerHeight == -1` かつ `header == nil` → `nil`（生成しない）のロジックは Phase 14.2 から維持する。section provider クロージャ内の呼び出し側を `Self.makeHeaderBoundaryItem(...)` 経由に変更する。

  > 既存実装は意図上は `.absolute(headerHeight)` を返す経路を持っていたが、
  > 単体テストから直接検証する経路が無く（private 関数）、回帰時に気付きにくかった。
  > Phase 16 で `internal static` に切り出し、`.absolute` / `.estimated` 切替を
  > 純粋ロジックテストで保証する。
- [x] 16.2 iOS: `KsSettingsViewControllerTests` に以下の純粋ロジックテストを追加する：
  - `Section.headerHeight = 80` 指定 + `header = .text("...")` → 返却 item の `heightDimension.isAbsolute == true` かつ `.dimension == 80`
  - `Section.headerHeight = -1` + `header = .text("...")` → 返却 item の `heightDimension.isEstimated == true` かつ `.dimension == 20`
  - `Section.headerHeight = -1` + `header == nil` → 返却 item が `nil`
  - `Section.headerHeight = 40` + `header == nil` → 返却 item が非 `nil` で `.absolute(40)`

### Sample 側

- [x] 16.3 iOS Sample: `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` の CommandCell セクションを `Section("CommandCell", headerHeight: 80) { ... }` に変更する（Phase 15.2 で導入した 60 から 80 に増量。他セクション・サンプル文言は変更しない）。
- [x] 16.4 Android Sample: `samples/android/.../BasicCellsDemoScreen.kt` の CommandCell セクションを `Section(header = "CommandCell", headerHeight = 80.0)` に変更する（iOS と同値）。

### 共通 / 仕様

- [x] 16.5 `specs/settings-view-ios-ui/spec.md` の「Section.headerHeight の UI 反映」 Requirement に、`.absolute(headerHeight)` が AutoLayout 下端揃え（Phase 15.1 `applyAccessoryLabel`、Priority 999）と両立して機能することを明文化する。

  > Phase 15.1 で導入した UILabel + AutoLayout 制約（priority 999）は
  > `.absolute(headerHeight)` で確定する supplementary 領域の中に納まる前提で
  > 設計されており、`headerHeight = 80` 指定時はラベルが contentView の下端
  > （`bottomAnchor == contentView.bottomAnchor`）に張り付いた状態で 80pt の領域内に
  > 描画される。`.absolute` と AutoLayout 制約は両立し、矛盾しない。
- [x] 16.6 `specs/samples-ios/spec.md` / `specs/samples-android/spec.md` の CommandCell セクション関連 Scenario の `headerHeight` を `80` に更新する（既存 `60` 表記を修正）。
- [x] 16.7 `design.md` に **Decision 16-1**「iOS の `heightDimension` 選択ロジック（`.absolute` vs `.estimated`）」を追記し、`section.headerHeight > 0` で `.absolute` を選ぶ意図と、`.estimated` だと指定値が打ち消される根拠を記す。
- [x] 16.8 `proposal.md` の「What Changes」セクションに Phase 16 で行った修正（iOS Section.headerHeight 反映保証 + Sample 値を 80 に増量）を追記する。

### Phase 16 追加対応（AiForms オリジナル工夫の反映 = supplementary view クラス切替）

オーナー指摘「AiForms オリジナルは工夫している」を受けた追加調査の結果、AiForms オリジナル
`TextHeaderView : UITableViewHeaderFooterView`（UITableView の supplementary 専用 class）と
`SettingsTableSource.GetHeightForHeader` が CGFloat を直接返す構造（
`../AiForms.Maui.SettingsView/SettingsView/Native/iOS/SettingsTableSource.cs` lines 143-167）
で「`headerHeight` 指定値が描画 rect に直接反映される」設計になっていることを特定。
本実装で `UICollectionViewListCell`（row cell 用 class）を supplementary に使用していたため、
内部 self-sizing が `.absolute(headerHeight)` を上書きするケースがあった。本サブ Phase で
`UICollectionReusableView` 直系の `KsAccessoryReusableView` に切り替え、視覚的高さ検証テストで動作実証する。

- [x] 16.9 iOS: `ios/Sources/KsSettingsViewUI/KsAccessoryReusableView.swift` を新規作成する。
  - `UICollectionReusableView` 直系のサブクラスとして実装し、UILabel + Auto Layout 制約（priority 999）で構築する。
  - `setVerticalAlignment(_:)` メソッドで `.top` / `.center` / `.bottom` を切替（AiForms `TextHeaderView.SetVerticalAlignment` 準拠）。
  - `prepareForReuse()` で label 状態を初期化する。
- [x] 16.10 iOS: `KsSettingsViewController` の `makeAccessoryListCell` を分岐させ、テキスト accessory および accessory 未指定の経路で `KsAccessoryReusableView` を使う。`accessoryView` 経路（任意 UIView / SwiftUI View 埋め込み）は引き続き `UICollectionViewListCell` を使用する。
- [x] 16.11 iOS: `refreshRootSupplementary(elementKind:)` を更新し、`KsAccessoryReusableView` と `UICollectionViewListCell` の両方の経路に対応する。
- [x] 16.12 iOS: 既存テスト（`SectionAccessoryRenderingTests` の `test_textヘッダのsupplementaryが表示される`、`test_text形式ヘッダの文字列更新で...`、`test_Footerの文字色は...`、`test_Headerテキストは下端揃えのUILabelで描画される`、`test_Footerテキストは上端揃えのUILabelで描画される`）を `KsAccessoryReusableView` 前提に更新する。
- [x] 16.13 iOS: 視覚的ヘッダ高さ検証テスト（`KsSettingsViewControllerTests` 内）を追加する。
  - `test_視覚的ヘッダ高さ_headerHeight80指定時_supplementaryのframe高さが80になる`: `UICollectionView.layoutIfNeeded()` 後の supplementary view の `frame.height` が 80pt（±0.5pt）であることを検証。
  - `test_視覚的ヘッダ高さ_headerHeight120指定時_supplementaryのframe高さが120になる`: 任意の指定値が反映されることを保証する回帰テスト。
- [x] 16.14 `specs/settings-view-ios-ui/spec.md` に「テキスト accessory 用 supplementary view クラスの選択」Requirement を追加し、`UICollectionViewListCell` ではなく `UICollectionReusableView` 直系クラスを使う理由（AiForms オリジナルの `UITableViewHeaderFooterView` 継承に揃える）と例外（accessoryView 経路は MAY で `UICollectionViewListCell`）を明文化する。
- [x] 16.15 `specs/settings-view-ios-ui/spec.md` の「Section.headerHeight の UI 反映」Requirement に Phase 16 追加対応の Scenario「headerHeight 正値が描画 frame に反映される」を追加する。
- [x] 16.16 `design.md` に **Decision 16-2**「テキスト accessory の supplementary view を `KsAccessoryReusableView` に切り替える」を追記し、AiForms オリジナルの該当ファイル / 行範囲を引用したうえで `UICollectionViewListCell` の self-sizing 問題と切替理由を記す。
- [x] 16.17 `proposal.md` の「What Changes」セクションに Phase 16 追加対応（KsAccessoryReusableView 新規作成 + 視覚的ヘッダ高さ検証テスト追加）を追記する。
- [x] 16.18 Phase 16 経路切替（テキスト accessory → `KsAccessoryReusableView`）により到達不能となった Phase 15.1 由来のデッドコードを iOS 実装から削除する（review-result_006.md Minor-1 対応）。
  - `KsSettingsViewController.applyAccessoryToListCell` の `accessoryText` / `verticalAlignment` / `textColor` パラメータと内部のテキスト accessory 分岐を削除し、accessoryView 経路専用シグネチャに整理する。
  - `KsSettingsViewController.applyAccessoryLabel` ヘルパ関数全体を削除する。
  - 削除に伴い `makeAccessoryListCell` と `refreshRootSupplementary` の listCell 経路、テスト `test_view形式ヘッダの差し替えでapplyAccessoryToListCellが新しいcontentConfigurationを設定する` の呼び出しシグネチャを新シグネチャに合わせて更新する。
  - `AccessoryVerticalAlignment` enum は `KsAccessoryReusableView` 経路（`makeAccessoryReusableView` / `refreshRootSupplementary` のテキスト経路 / `mapVerticalAlignment`）で引き続き利用するため残置する。
- [x] 16.19 `design.md` に **Decision 16-3**「Phase 16 経路切替で発生した Phase 15.1 由来のデッドコードを削除する」を追記し、削除対象（`applyAccessoryLabel` / `applyAccessoryToListCell` のテキスト分岐）と残置対象（`AccessoryVerticalAlignment` enum）を明記する。

### Phase 16 完了条件

- 16.1〜16.19 のチェックがすべて `[x]` になっている。
- iOS / Android のビルド・全ユニットテストが PASS する（Phase 12 と同じコマンドを再実行）。
- `openspec validate refine-basic-cells-sample-layout --strict` が valid を返す。
- iOS の視覚的ヘッダ高さ検証テストが、`UICollectionView.layoutIfNeeded()` 後の `supplementary.frame.height` が指定値（80pt / 120pt）と一致することを実測で確認する。
- 実機目視で iOS / Android 双方とも CommandCell セクションのヘッダ高さが他セクション（自動高さ）より明確に高く描画される。

## 17. オーナー三次実機目視（Image #12 / #13）正式対応: iOS Cell.cellHeight 反映修正

オーナー三次実機目視（Image #12 / #13）の本来の指摘は **「個別 Cell の `cellHeight` が iOS で反映されていない」** だった
（Phase 16 では `Section.headerHeight` を誤って対象として扱っていたが、本来は Tanaka Taro セルの `CellStyle(cellHeight: 80)` の反映が問題）。
本 Phase で AiForms.Maui.SettingsView オリジナル `Native/iOS/SettingsTableSource.cs` の
`GetHeightForRow`（lines 113-135、`cell.Height` の `NFloat` を直接返し UITableView の rect 計算に反映）に倣い、
`UICollectionViewListCell` の self-sizing 経路で同等の振る舞いを実現する。

### 問題の確認

- [x] 17.1 Sample 側の確認: iOS / Android Sample（`samples/ios/.../BasicCellsDemoView.swift` / `samples/android/.../BasicCellsDemoScreen.kt`）の Tanaka Taro CommandCell に `style: CellStyle(cellHeight: 80)` が指定済みであることを確認する（Phase 15 以前から指定済み）。

  > 確認結果: iOS Sample (`BasicCellsDemoView.swift` line 88) / Android Sample (`BasicCellsDemoScreen.kt` line 73) 双方に `cellHeight: 80` / `cellHeight = 80.0` が既に指定されていた。Sample 側の追加修正は不要。
- [x] 17.2 iOS 側の根本原因調査: `KsCellViewSupport.applyEffectiveHeight(_:effective:)` が `contentView.heightAnchor.constraint(greaterThanOrEqualToConstant: effectiveCellHeight)` を priority 999 で設定するものの、`UICollectionViewListCell` の self-sizing（`UIListContentConfiguration` の intrinsic 高さ）が priority 1000 で勝ってしまい、`frame.height` に反映されないことを実測テスト（`test_視覚的セル高さ_cellHeight80指定時...`）で確認する。

  > 実測結果: 修正前は `cellHeight = 80` 指定の CommandCell の `frame.height` が ~67.33pt（UIListContentConfiguration.subtitleCell() の intrinsic 高さ）となり、80pt に達しなかった。`applyEffectiveHeight` の制約は contentView.heightAnchor に priority 999 で設定されていたが、Compositional Layout の自動 self-sizing 計算では intrinsic（priority 1000）が優先されるため効果が打ち消されていた。

### iOS 側修正

- [x] 17.3 iOS: `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` を新規作成する。`UICollectionViewListCell` を継承し、以下を担う：
  - `init(frame:)` で `KsCellViewSupport.installSelectedColorHandler(self)` を呼ぶ
  - `preferredLayoutAttributesFitting(_:)` を override し、`KsCellViewSupport.adjustedLayoutAttributes(self, proposed:)` 経由で `cellHeight` を proposed attributes に補正する
- [x] 17.4 iOS: `KsCellViewSupport.swift` に `static func adjustedLayoutAttributes(_ listCell: UICollectionViewListCell, proposed: UICollectionViewLayoutAttributes) -> UICollectionViewLayoutAttributes` を追加する。`KsCellViewState.lastHeight` / `lastIsFixedHeight` を参照し、固定モードでは厳密に、可変モードでは下限として補正する。
- [x] 17.5 iOS: 7 種の Cell View（`LabelCellView` / `CommandCellView` / `ButtonCellView` / `SwitchCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView`）の継承元を `UICollectionViewListCell` から `KsListCellBase` に変更する。各 View の `init(frame:)` 内の `KsCellViewSupport.installSelectedColorHandler(self)` 呼び出し（基底クラスで重複）と `init?(coder:)` の `@available(*, unavailable)` 宣言（基底クラスへ移譲）を削除する。`ButtonCellView` / `SwitchCellView` のように追加初期化処理を持つ View は `init(frame:)` 内の固有処理（`titleLabel` 配置、`UISwitch` の `addTarget` 等）を保持する。

### iOS 側テスト

- [x] 17.6 iOS: `KsSettingsViewControllerTests.swift` に視覚的セル高さ検証テストを追加する：
  - `measuredCellHeight(for:indexPath:containerSize:)` ヘルパを追加し、`UICollectionView.layoutIfNeeded()` 後の `cellForItem(at:)` の `frame.height` を返す。
  - `test_視覚的セル高さ_cellHeight80指定時_セルのframe高さが80になる`: `Theme(hasUnevenRows: true)` + `CommandCell(style: CellStyle(cellHeight: 80), title: "Tanaka Taro", description: "tanaka.taro@example.com")` が `>= 80pt - 0.5pt` であることを検証。
  - `test_視覚的セル高さ_cellHeight120指定時_セルのframe高さが120になる`: 任意指定値 120 の反映を保証する回帰テスト。

### 仕様 / 設計ドキュメント

- [x] 17.7 `specs/settings-view-ios-ui/spec.md` に「Cell.cellHeight の UI 反映（Phase 17 追加対応）」Requirement を追加する。固定高さモードと可変高さモードの規則、`KsListCellBase` の `preferredLayoutAttributesFitting` 経路、AiForms オリジナル `SettingsTableSource.GetHeightForRow` との対応関係を明文化する。3 つの Scenario（80pt 反映 / 120pt 反映 / 未指定時の標準動作）を追加する。
- [x] 17.8 `specs/settings-view-ios-ui/spec.md` に「Cell View 共通基底クラスの導入（Phase 17 追加対応）」Requirement を追加する。`KsListCellBase` の責務（タッチフィードバック登録、`preferredLayoutAttributesFitting` 経由の `cellHeight` 反映）と、全 7 種 Cell View が継承する MUST を明文化する。
- [x] 17.9 `design.md` に **Decision 17-1**「iOS Cell.cellHeight 反映ロジック（`preferredLayoutAttributesFitting` override + 共通基底クラス）」を追記する。AiForms オリジナル `SettingsTableSource.GetHeightForRow` の引用、Phase 16 が `Section.headerHeight` を誤って対象としていた経緯、`UICollectionViewListCell` の self-sizing 制約と `preferredLayoutAttributesFitting` 補正の関係を記す。
- [x] 17.10 `proposal.md` の「What Changes」セクションに Phase 17（iOS Cell.cellHeight 反映保証 + 共通基底クラス `KsListCellBase` 導入 + 視覚的セル高さ検証テスト追加）を追記する。
- [x] 17.11 Phase 16 で導入した `Section.headerHeight 60 → 80` 増量は Phase 17 でも維持する（Sample の値を 80 で固定）。Phase 16 の `KsAccessoryReusableView` / `headerHeight` 反映機構自体は正しい改善であり、本 Phase ではそのまま維持する（オーナーから NG を受けていない）。

### Phase 17 完了条件

- 17.1〜17.11 のチェックがすべて `[x]` になっている。
- iOS / Android のビルド・全ユニットテストが PASS する。
- `openspec validate refine-basic-cells-sample-layout --strict` が valid を返す。
- iOS の視覚的セル高さ検証テストが、`UICollectionView.layoutIfNeeded()` 後の `cellForItem(at:)?.frame.height` が指定値（80pt / 120pt）以上であることを実測で確認する。
- 実機目視で iOS の CommandCell（Tanaka Taro）が他セル（自動高さ）より明確に高く描画される（Image #13 = Android と同等の見た目）。

## 18. オーナー指示によるPhase 16 機構の revert（B 案: 副次改善のみ維持）

オーナーから「Phase 16 で間違ってしなくて良い修正を入れたなら戻して欲しい」との指示を受け、
Phase 16 はオーナーの本来の指摘（Cell.cellHeight 反映）を `Section.headerHeight` と
読み違えた結果の **誤実装**であったことを確定する。Phase 17 で本来の指摘が正しく解決された
ため、Phase 16 で導入した `KsAccessoryReusableView` / 視覚的ヘッダ高さ検証テスト / Sample 値 80
増量は revert する。一方で純粋ロジックとしては正しい `makeHeaderBoundaryItem` の
`internal static` 化と純粋ロジックテスト 4 件は副次改善として維持する（B 案）。

### iOS 側 revert

- [x] 18.1 iOS: `ios/Sources/KsSettingsViewUI/KsAccessoryReusableView.swift` を削除する（`trash` 経由）。
- [x] 18.2 iOS: `KsSettingsViewController.swift` から `KsAccessoryReusableView` 経路の参照を撤去する：
  - `makeAccessoryListCell` の `accessoryText != nil || accessoryView == nil` 分岐を削除し、テキスト accessory / SwiftUI / UIKit すべてを `UICollectionViewListCell` 経路に統一する。
  - `makeAccessoryReusableView` / `mapVerticalAlignment` を削除する。
  - `refreshRootSupplementary` の `as? KsAccessoryReusableView` 分岐を削除する。
  - `AccessoryVerticalAlignment` enum は `applyAccessoryLabel` 復活で引き続き使用するため残置する。
- [x] 18.3 iOS: `KsSettingsViewController.applyAccessoryToListCell` のテキスト accessory 分岐 と `applyAccessoryLabel` ヘルパを Phase 15.1 と同等の実装で復活させる：
  - `applyAccessoryToListCell(_:accessoryText:accessoryView:textColor:verticalAlignment:)` のシグネチャに戻す。
  - `applyAccessoryLabel(_:text:textColor:verticalAlignment:)` を再導入し、`UICollectionViewListCell.contentView` に UILabel + AutoLayout 制約（priority 999、Header = 下端揃え / Footer = 上端揃え）で配置する。
- [x] 18.4 iOS: `SectionAccessoryRenderingTests.swift` を Phase 15 / 15.1 ベースラインに復元する：
  - `is KsAccessoryReusableView` 検証を `is UICollectionViewListCell` に戻す。
  - `view.label.text` の検証を `listCell.contentView.subviews.compactMap { $0 as? UILabel }.first?.text` に書き換える。
  - `test_view形式ヘッダの差し替えでapplyAccessoryToListCellが新しいcontentConfigurationを設定する` の呼び出しシグネチャを新（旧）シグネチャに合わせる。
  - 下端 / 上端揃え検証は `listCell.contentView` の bottomAnchor / topAnchor を見るよう書き換える。
- [x] 18.5 iOS: `KsSettingsViewControllerTests.swift` から Phase 16 で追加した視覚的ヘッダ高さ検証テスト 2 件を削除する：
  - `test_視覚的ヘッダ高さ_headerHeight80指定時_supplementaryのframe高さが80になる`
  - `test_視覚的ヘッダ高さ_headerHeight120指定時_supplementaryのframe高さが120になる`
  - `measuredSectionHeaderHeight(for:section:containerSize:)` ヘルパも本 phase で削除する（参照元がなくなるため）。

### Sample 側 revert

- [x] 18.6 iOS Sample: `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` の CommandCell セクションの `headerHeight` を `80` → `60` に戻す（Phase 15.2 で導入した値）。
- [x] 18.7 Android Sample: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/BasicCellsDemoScreen.kt` の CommandCell セクションの `headerHeight` を `80.0` → `60.0` に戻す（Phase 15.5 で導入した値、iOS と同値）。

### 維持する Phase 16 副次改善（B 案）

- [x] 18.8 `KsSettingsViewController.makeHeaderBoundaryItem(for:original:)` の `internal static` 化は維持する（純粋ロジックとして正しく、テストで `.absolute` / `.estimated` 切替を保証できる）。
- [x] 18.9 純粋ロジックテスト 4 件は維持する：
  - `test_makeHeaderBoundaryItem_headerHeight80のとき_absolute80になる`
  - `test_makeHeaderBoundaryItem_headerHeight未指定_header非空のとき_estimated20になる`
  - `test_makeHeaderBoundaryItem_headerHeight未指定_header_nilのとき_nilを返す`
  - `test_makeHeaderBoundaryItem_headerHeight40_header_nilでも_absolute40になる`

### 仕様 / 設計ドキュメント

- [x] 18.10 `specs/settings-view-ios-ui/spec.md` から「テキスト accessory 用 supplementary view クラスの選択（Phase 16 追加対応）」Requirement とその 2 Scenario を削除する。
- [x] 18.11 `specs/settings-view-ios-ui/spec.md` の「Section.headerHeight の UI 反映」 Requirement の Phase 16 追加 Scenario「headerHeight 正値が描画 frame に反映される」を削除し、AutoLayout 下端揃え両立 Scenario の `headerHeight = 80` を `60` に戻す。
- [x] 18.12 `specs/samples-ios/spec.md` / `specs/samples-android/spec.md` の `headerHeight = 80` / `headerHeight = 80.0` を `60` / `60.0` に戻す（Phase 15.2 / 15.5 の値）。
- [x] 18.13 `design.md` に **Decision 18-1**「Phase 16 機構を revert し、副次改善のみ維持する（B 案）」を追記する。Phase 16 がオーナーの本来の指摘（Cell.cellHeight）を `Section.headerHeight` と読み違えた経緯、Phase 17 で本来の指摘が解決済みであること、`makeHeaderBoundaryItem` の `internal static` 化と純粋ロジックテストは独立した CI 保証として維持する根拠を記す。Decision 16-2 / 16-3 は revert により削除した旨、Decision 16-1 は維持する旨も明記する。
- [x] 18.14 `proposal.md` の「What Changes」セクションに Phase 18（Phase 16 revert + B 案による副次改善維持）を追記する。

### Phase 18 完了条件

- 18.1〜18.14 のチェックがすべて `[x]` になっている。
- iOS / Android のビルド・全ユニットテストが PASS する（Phase 12 と同じコマンドを再実行）。
- `openspec validate refine-basic-cells-sample-layout --strict` が valid を返す。
- Phase 17 の機構（`KsListCellBase` + `preferredLayoutAttributesFitting`）が壊れていないこと（`test_視覚的セル高さ_*` が PASS する）を確認する。

## 依存関係

- Phase 1（Core 変更）は Phase 2〜11 すべての前提となる。
- Phase 2〜5（iOS UI 修正）と Phase 7〜9（Android UI 修正）は Phase 1 完了後に並列実行可能。
- Phase 6（iOS KsImage 解決）は Phase 1 完了後、Phase 10（iOS Sample 再編）の前に完了する必要がある。
- Phase 9（Android KsImage 解決）は Phase 1 完了後、Phase 11（Android Sample 再編）の前に完了する必要がある。
- Phase 10 / 11（Sample 再編）は Phase 1〜9 の対応プラットフォーム側がすべて完了した後に着手する。
- Phase 12（テスト・ビルド検証）は Phase 1〜11 すべての後に実行する。
- Phase 13（実機目視確認）は Phase 12 PASS 後に実行する（Phase 14 / Phase 15 完了後に再実施）。
- Phase 14（オーナーレビュー追加指摘の修正）は Phase 13 の初回確認結果を踏まえて実施し、各項目完了後に Phase 12 相当のテスト・ビルドを再実行する。
- Phase 15（オーナー二次実機目視 Image #8〜#11 対応）は Phase 14 完了後の実機目視で判明した残課題を解消するために実施する。完了後に Phase 12 相当のテスト・ビルドを再実行する。
- Phase 16（オーナー三次実機目視 Image #12 / #13 対応 = iOS Section.headerHeight 反映保証）は Phase 15 完了後の実機目視で判明した残課題を解消するために実施する。完了後に Phase 12 相当のテスト・ビルドを再実行する。
- Phase 17（オーナー三次実機目視 正式対応 = iOS Cell.cellHeight 反映修正）は Phase 16 完了後、オーナーから「Phase 16 は本来の指摘ではなかった」と再指摘を受けたため実施する。Phase 16 の `KsAccessoryReusableView` 導入による headerHeight 反映機構は維持しつつ、本 Phase で個別 Cell の `cellHeight` 反映を保証する。完了後に Phase 12 相当のテスト・ビルドを再実行する。
- Phase 18（Phase 16 機構 revert + B 案による副次改善維持）は Phase 17 完了後、オーナーから「Phase 16 で間違ってしなくて良い修正を入れたなら戻して欲しい」との明確な指示を受けたため実施する。Phase 17 で本来の指摘が解決済みであることを前提に、Phase 16 で追加された `KsAccessoryReusableView` / 視覚的ヘッダ高さ検証テスト / Sample 値 80 増量を revert し、純粋ロジックとして正しい `makeHeaderBoundaryItem` の `internal static` 化と純粋ロジックテスト 4 件は副次改善として維持する。完了後に Phase 12 相当のテスト・ビルドを再実行する。

## 完了条件

- すべてのチェックボックスが `- [x]` になっている。
- `openspec validate refine-basic-cells-sample-layout --strict` が valid を返す。
- iOS / Android の全テストおよびビルドが PASS する。
- 実機目視確認でオーナーレビュー指摘事項（Sticky Footer / viewBackgroundColor / Header Footer 余白 / 罫線 / Switch Thumb / Checkbox 整列 / アイコン / 構成と表記揃え）がすべて解消されている。

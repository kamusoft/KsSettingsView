## Why

基本 Cell 7 種の実装と仕様整備は前 change（`refine-basic-cells-style`）で完了したが、実機レビューにより以下のギャップが判明した：

1. **サンプル構成が「実用アプリ風」になっており、Cell の種別と表示パターンが把握しづらい**。オーナーの意図はクロスプラットフォーム UI ライブラリの動作確認であり、業務的な分類（Account / Storage / Preferences）ではなく Cell タイプ別の素直な構成が望ましい。
2. **Android 側に複数の表示不具合**（アイコン未表示、SwitchCell の Thumb / Track 単色、CheckboxCell の右端ズレ）。
3. **iOS 側に複数の表示不具合とデグレ**（Section Footer の sticky 復活、`viewBackgroundColor` がセクション間の背景に反映されない、Section Header / Footer の不要な余白、罫線インセット規則の未実装）。
4. **アイコン解決方式が未確定**（前 change で「将来 change で対応」とした保留事項）。Android では `KsImage.systemName` から `Drawable` への解決ロジックが不在で、現状アイコンが表示できない。

本提案はこれらを 1 つの change として一括是正する。サンプル再編と本体 UI 修正は実機確認の連動性が強く、分割すると検証が冗長になるため統合する。

## What Changes

### サンプル構成の再編成（iOS / Android 共通）

- **BREAKING**（Sample のみ）: BasicCellsDemo のセクション構成を業務分類（Account / Storage / Preferences / Type / Items / Action / Help）から **Cell タイプ別**（CommandCell / LabelCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / ButtonCell）に再編する。
- セクション名は Cell タイプ名そのものとし、iOS / Android で一字一句揃える。
- 各セクションの Cell 数は 1〜3 個（`RadioCell` のみ最低 2）に絞り、`CommandCell` と `LabelCell` セクションのみ複数バリエーション（シンプル / フル構成）を配置する。
- Cell 内のテキスト（title / description / valueText / hintText / footer）は iOS / Android で一字一句揃える。

### `KsImage` 型のクロスプラットフォーム再設計

- **BREAKING**: `KsImage` を sealed interface / enum 化し、各プラットフォーム固有のアイコン表現を派生として持つ構造に変更する。
  - iOS: `KsImage.systemName(String)`（SF Symbols 名、既存）、`KsImage.uiImage(UIImage)`（任意 UIImage）
  - Android: `KsImage.Resource(@DrawableRes resId: Int)`（リソース ID、新規・主軸）、`KsImage.Drawable(android.graphics.drawable.Drawable)`（任意 Drawable、新規）
- iOS 側の既存呼び出し `KsImage(systemName: "...")` は `KsImage.systemName("...")` に書き換え必要（イニシャライザ廃止）。
- Android UI 層に `KsImage.Resource` および `KsImage.Drawable` を解決して `ImageView.setImageDrawable(...)` するロジックを追加する。
- iOS UI 層に `KsImage.uiImage` 派生を解決するロジックを追加する。
- Sample アプリ（Android）に Material Symbols 由来の VectorDrawable リソース（数個程度）を追加し、`KsImage.Resource(R.drawable.xxx)` で指定する。

### Android UI 修正

- `SwitchCellViewHolder`: `MaterialSwitch.thumbTintList` を実効 accent 色と独立させ、checked 時の Thumb は **コントラスト色（白系 / Material `colorOnPrimary` 相当）** で塗ること。Track は引き続き実効 accent 色で塗る。これにより Thumb と Track が視覚的に区別される。
- `CheckboxCellViewHolder`: `MaterialCheckBox` に明示サイズ（24dp 角）と `marginEnd` 調整を行い、`SwitchCell` / `RadioCell` / `SimpleCheckCell` の右端 X 座標と ±1px 以内で一致させる。
- `LabelCellViewHolder` / `CommandCellViewHolder` のアイコン領域に `KsImage.Resource` / `KsImage.Drawable` の解決ロジックを追加し、Sample のリソース ID 指定で実機にアイコンが描画されることを保証する。`KsImage.systemName` は Android では解決不可として無視（フォールバック：アイコン非表示）。

### iOS UI 修正

- **Sticky Footer デグレ修正**: `KsSettingsViewController` の `boundarySupplementaryItems` map で Section Footer も `pinToVisibleBounds = false` に設定する。
- **`viewBackgroundColor` のセクション間反映**: `UICollectionLayoutListConfiguration.backgroundColor` を `.clear` にし、`UICollectionView.backgroundColor` だけが効くようにする（または同等のレイアウト調整）。これによりセクション間の隙間にも `Theme.viewBackgroundColor` が反映される。
- **Section Header 高さ**: AiForms.Maui.SettingsView の `Section.HeaderHeight` 仕様（既定 `-1d` = Automatic、正値 = 固定）に準拠した制御を追加。Header テキスト空かつ高さ未指定の場合は supplementary 自体を出さない。
- **Section Footer 余白除去**: Footer テキスト空のセクションは supplementary を生成しない（AiForms の `NFloat.Epsilon` 相当）。
- **罫線インセット規則**: セクション境界（先頭・末尾）の separator は端から端まで（inset = 0）、セクション内 Cell 間の separator は Title のリーディング位置以降にインセットする（≒ アイコン枠右端 or 標準左マージン 16pt）。

### Core 仕様の追加

- `Section.headerHeight: Double`（既定 `-1`）を追加。AiForms `Section.HeaderHeight` 相当。`-1` は「自動高さ」、正値は固定高さ。
- Footer 用の Section レベル高さプロパティは追加しない（AiForms に揃えて、Footer は SettingsView レベル設定とテキスト有無のみで制御）。

### Phase 15 で追加された修正（オーナー二次実機目視 Image #8〜#11 対応）

- **iOS / Android 共通: Section Header / Footer の垂直配置**: Header テキストは boundary 領域の **下端揃え**、Footer テキストは **上端揃え**で描画する（AiForms オリジナル `TextHeaderView.SetVerticalAlignment(LayoutAlignment.End)` および `TextFooterView` の top anchor 既定挙動に揃える）。実装は iOS では UIView + UILabel + AutoLayout、Android では `TextView.gravity = Gravity.BOTTOM | START` / `Gravity.TOP | START` で実現する。
- **Android: `Section.headerHeight` の UI 反映**: `CellListItem.SectionHeader` に `headerHeight` フィールドを追加し、`SectionTextAccessoryViewHolder.bind()` で `itemView.layoutParams.height` に反映する。これにより Sample で指定した `headerHeight = 60.0` が実機で 60dp 固定高さになる。
- **Android: 基本 Cell 共通の垂直パディング縮小**: コンテナの上下パディングを AiForms オリジナル `cellbaseview.axml` の `4dp` に揃え（従来 16dp）、iOS / オリジナル Android との上下密度差を解消する。
- **Sample: CommandCell セクション headerHeight = 60**: iOS / Android 両 Sample の CommandCell セクションを `headerHeight = 60` に揃え、固定高さと下揃えの組合せを目視確認できるようにする。

### Phase 16 で追加された修正（オーナー三次実機目視 Image #12 / #13 対応）

- **iOS: Section.headerHeight の `.absolute` 反映保証**: `KsSettingsViewController.makeHeaderBoundaryItem(for:original:)` を `internal static` に切り出し、`section.headerHeight > 0` のとき `NSCollectionLayoutBoundarySupplementaryItem.layoutSize.heightDimension` に `.absolute(CGFloat(headerHeight))` を必ず適用する経路を純粋ロジックテストで保証する。Phase 14.2 で導入した `.estimated(20)` は `section.headerHeight == -1` かつ `section.header` 非空のときのみ使う。Phase 15.1 の AutoLayout 制約（priority 999 の UILabel 下端揃え）は `.absolute` で確定する領域内に納まり両立する。
- **Sample: CommandCell セクション headerHeight = 80 へ増量**: iOS / Android 両 Sample の CommandCell セクションを `headerHeight = 80`（Android は `80.0`）に増量し、他セクション（自動高さ）との視覚的な差を明確化して `Section.headerHeight` の反映を目視で確認しやすくする。

### Phase 16 追加対応（AiForms オリジナル工夫の反映）

オーナー指摘「AiForms オリジナルは工夫している」を受け `AiForms.Maui.SettingsView/Platforms/iOS/` を再調査。`TextHeaderView` が `UITableViewHeaderFooterView`（UITableView 専用の supplementary class）を継承し、`SettingsTableSource.GetHeightForHeader` で CGFloat を直接返す構造（lines 143-167）になっていることが判明。本実装で supplementary view として使っていた `UICollectionViewListCell` は本来 row cell 用 class であり、内部 self-sizing 機構が `boundarySupplementaryItem.layoutSize.heightDimension = .absolute(headerHeight)` を上書きするケースがあった。

- **iOS: テキスト accessory 用の supplementary view クラスを切替**: `KsAccessoryReusableView`（`UICollectionReusableView` 直系のサブクラス）を新規追加し、テキスト accessory および accessory 未指定の Header / Footer supplementary をこのクラスで描画する。AiForms オリジナル `TextHeaderView.cs` の priority 999 constraint 方式と `SetVerticalAlignment` 意味論を踏襲する。accessoryView 経路（任意 UIView / SwiftUI View 埋め込み）は `UIHostingConfiguration` / `UIListContentConfiguration` 用に `UICollectionViewListCell` を維持する。
- **iOS: 視覚的ヘッダ高さ検証テストを追加**: `UICollectionView.layoutIfNeeded()` 後に supplementary view の `frame.height` を直接検証するテスト（`test_視覚的ヘッダ高さ_headerHeight80指定時_supplementaryのframe高さが80になる` / `..._headerHeight120指定時_..._120になる`）を追加し、`.absolute(headerHeight)` が描画 frame に確実に反映されることを CI で保証する。

### Phase 17 で追加された修正（オーナー三次実機目視 Image #12 / #13 正式対応）

Phase 16 はオーナーの本来の指摘とは別の問題（`Section.headerHeight` 反映）に対応していた。Phase 16 で追加した `KsAccessoryReusableView` などの機構は正しい改善として維持しつつ、Phase 17 でオーナーの本来の指摘である **個別 Cell の `CellStyle.cellHeight` の iOS 反映** を正式に修正する。

- **iOS: Cell.cellHeight の `frame.height` 反映保証**: 全 Cell View（`LabelCellView` / `CommandCellView` / `ButtonCellView` / `SwitchCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView`）を共通基底クラス `KsListCellBase: UICollectionViewListCell` から継承させ、`preferredLayoutAttributesFitting(_:)` を override して proposed `UICollectionViewLayoutAttributes.size.height` を `KsCellViewSupport.adjustedLayoutAttributes(_:proposed:)` 経由で補正する。固定高さモード（`Theme.hasUnevenRows == false`）では厳密に `effectiveCellHeight` に揃え、可変高さモード（`hasUnevenRows == true`）では下限として `max(proposed, effectiveCellHeight)` を採用する。AiForms オリジナル `Native/iOS/SettingsTableSource.cs` の `GetHeightForRow`（lines 113-135、`cell.Height` を `NFloat` で直接返す）と意味論的に等価。
- **iOS: 視覚的セル高さ検証テストを追加**: `KsSettingsViewControllerTests.swift` に `measuredCellHeight(for:indexPath:)` ヘルパと `test_視覚的セル高さ_cellHeight80指定時_セルのframe高さが80になる` / `test_視覚的セル高さ_cellHeight120指定時_セルのframe高さが120になる` を追加し、`UICollectionView.layoutIfNeeded()` 後の `cellForItem(at:)?.frame.height` が指定値以上であることを CI で保証する。
- **iOS: 共通基底クラス `KsListCellBase` を新規追加**: `KsCellViewSupport.installSelectedColorHandler(self)` の呼び出しと `preferredLayoutAttributesFitting(_:)` override を集約。7 種の Cell View の `init(frame:)` から重複コードを削除する（`init?(coder:)` の `@available(*, unavailable)` 宣言も基底クラスへ移譲）。
- **Sample の値は変更しない**: iOS / Android Sample の Tanaka Taro CommandCell は Phase 14.9 以前から `cellHeight = 80` を指定済みで、これが iOS で反映されていなかったのが本不具合の本質。Phase 16 で増量した `Section.headerHeight = 80` は維持する。

### Phase 18 で追加された修正（オーナー指示による Phase 16 機構の revert）

オーナーから「Phase 16 で間違ってしなくて良い修正を入れたなら戻して欲しい」との明確な指示があり、B 案（機構は戻すが、副次的な改善は残す）で進めることが合意された。Phase 16 はオーナーの本来の指摘（個別 Cell の `cellHeight` 反映不具合）を `Section.headerHeight` 反映不具合と読み違えた結果の誤実装であり、Phase 17 で本来の指摘が正しく解決されたため、Phase 16 で追加した機構は不要となった。

- **iOS: `KsAccessoryReusableView` を完全削除**: `ios/Sources/KsSettingsViewUI/KsAccessoryReusableView.swift` を `trash` 経由で削除する。
- **iOS: テキスト accessory 経路を `UICollectionViewListCell` に統一**: `KsSettingsViewController.makeAccessoryListCell` の分岐を撤廃し、テキスト / SwiftUI / UIKit すべてを `UICollectionViewListCell` 経路に戻す。`makeAccessoryReusableView` / `mapVerticalAlignment` を削除し、`refreshRootSupplementary` の `KsAccessoryReusableView` 分岐も削除する。`applyAccessoryToListCell` のテキスト分岐 と `applyAccessoryLabel` ヘルパは Phase 15.1 と同等の実装で復活させる。
- **iOS: 視覚的ヘッダ高さ検証テスト 2 件を削除**: `test_視覚的ヘッダ高さ_headerHeight80指定時_supplementaryのframe高さが80になる` / `..._headerHeight120指定時_..._120になる` を削除し、`measuredSectionHeaderHeight(for:section:containerSize:)` ヘルパも併せて削除する。
- **iOS / Android: Sample の `headerHeight` を 80 → 60 に戻す**: `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` の `headerHeight: 80` を `60` に、`samples/android/.../BasicCellsDemoScreen.kt` の `headerHeight = 80.0` を `60.0` に戻す（Phase 15.2 / 15.5 の値）。
- **Spec 整理**: `specs/settings-view-ios-ui/spec.md` から「テキスト accessory 用 supplementary view クラスの選択」Requirement / 「headerHeight 正値が描画 frame に反映される」Scenario を削除。`specs/samples-ios/spec.md` / `specs/samples-android/spec.md` の `headerHeight` 値を 60 に戻す。
- **維持する Phase 16 副次改善（B 案）**: `KsSettingsViewController.makeHeaderBoundaryItem(for:original:)` の `internal static` 化 と純粋ロジックテスト 4 件（`test_makeHeaderBoundaryItem_*`）は維持する。これは Phase 16 機構とは独立した CI 保証の改善であり、`.absolute` / `.estimated` 切替の回帰防止に有用。
- **Decision 16-1 は維持、16-2 / 16-3 は revert**: `design.md` の Decision 16-2（`KsAccessoryReusableView` 採用）と Decision 16-3（Phase 15.1 由来のデッドコード削除）に「Phase 18 で revert 済み」と注記。Decision 16-1（`.absolute` vs `.estimated` 選択ロジック）は副次改善として維持される。新規 Decision 18-1（Phase 16 機構を revert し、副次改善のみ維持する B 案）を追加する。
- **Phase 17 機構は壊さない**: Phase 17 の `KsListCellBase` + `preferredLayoutAttributesFitting` は Phase 18 でも維持する（オーナーの本来の指摘を解決した正しい修正）。`test_視覚的セル高さ_*` は引き続き PASS する。

## Capabilities

### New Capabilities

（新規 capability なし）

### Modified Capabilities

- `settings-view-core`: `Section` 型に `headerHeight: Double` フィールドを追加。
- `cell-types-basic`: `KsImage` 値型を sealed 化（iOS: `systemName` / `uiImage`、Android: `Resource(@DrawableRes)` / `Drawable` / `SystemName`）。プラットフォーム別解決規則（systemName は iOS のみ、Resource / Drawable は Android のみ、Android で SystemName を受けたらフォールバックで非表示）を明示。
- `settings-view-ios-ui`: Section Footer の `pinToVisibleBounds = false` 強制、`viewBackgroundColor` のセクション間反映、`Section.headerHeight` 適用、Footer 空時の supplementary 非生成、罫線インセット規則、`KsImage.uiImage` 派生の解決を Requirement 化。
- `settings-view-android-ui`: `SwitchCell` の Thumb / Track 色分離、`CheckboxCell` の右端整列（明示サイズ・marginEnd 調整）、`KsImage.Resource` / `KsImage.Drawable` 派生のアイコン解決を Requirement 化。`KsImage.systemName` は Android では未サポート（無視）と明記。
- `samples-ios`: BasicCellsDemo の構成を Cell タイプ別セクション構成に再編。
- `samples-android`: BasicCellsDemo の構成を Cell タイプ別セクション構成に再編。サンプルアプリに Material Symbols 由来の VectorDrawable リソースを追加。

## Impact

### 影響を受けるコード

- **iOS**: `ios/Sources/KsSettingsViewCore/KsImage.swift`、`ios/Sources/KsSettingsViewCore/Section.swift`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、`ios/Sources/KsSettingsViewUI/KsAccessoryReusableView.swift`（Phase 16 追加対応で新規作成、**Phase 18 で削除**）、`ios/Sources/KsSettingsViewUI/KsListCellBase.swift`（Phase 17 追加対応で新規作成、Phase 18 でも維持）、`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift`（Phase 17 で `adjustedLayoutAttributes` 追加、Phase 18 でも維持）、`ios/Sources/KsSettingsViewUI/LabelCellView.swift` / `CommandCellView.swift` / `ButtonCellView.swift` / `SwitchCellView.swift` / `CheckboxCellView.swift` / `RadioCellView.swift` / `SimpleCheckCellView.swift`（Phase 17 で継承元を `KsListCellBase` に変更、Phase 18 でも維持）、`ios/Sources/KsSettingsViewUI/SectionHeaderFooterView.swift`、`samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift`
- **Android**: `android/ks-settingsview-core/src/main/kotlin/.../KsImage.kt`、`android/ks-settingsview-core/src/main/kotlin/.../Section.kt`、`android/ks-settingsview-ui/src/main/kotlin/.../SwitchCellViewHolder.kt`、`android/ks-settingsview-ui/src/main/kotlin/.../CheckboxCellViewHolder.kt`、`android/ks-settingsview-ui/src/main/kotlin/.../LabelCellViewHolder.kt`、`android/ks-settingsview-ui/src/main/kotlin/.../CommandCellViewHolder.kt`、`samples/android/app/src/main/java/.../BasicCellsDemoScreen.kt`、`samples/android/app/src/main/res/drawable/`（新規 VectorDrawable）

### 影響を受ける API（破壊的変更）

- `KsImage(systemName: String)` イニシャライザ廃止 → `KsImage.systemName("...")` 形式に変更。
  - 影響: 既存 Sample コード、テストコードでの `KsImage` 生成箇所すべて。
- `KsImage` 型の構造が値型から sealed enum / interface に変わるため、`switch` / `when` での網羅性チェックが必要になる。
- `Section.headerHeight` の追加は末尾追加・既定値ありのため API 互換性は維持（既存呼び出しはコンパイルエラーにならない）。

### 依存関係

- 新規依存ライブラリの追加なし。
- Android Sample に Material Symbols VectorDrawable（数個）を追加。

## Risks

### 破壊的変更によるリスク

- **`KsImage` API の破壊変更**: 既存サンプル・テストの全 `KsImage(systemName:)` 呼び出しを `KsImage.systemName(...)` に書き換える必要がある。本変更で書き換える範囲は本リポジトリ内のすべての利用箇所（サンプル + テスト）で完結する。リポジトリ外部利用者はまだ存在しないため、影響は本提案のスコープ内で吸収可能。
- **緩和策**: 一括書き換えと、`swift build` / `gradle build` の両方でビルド検証を実施する。

### iOS Footer / Header の supplementary 非生成によるリスク

- 既存の Footer / Header が「常に supplementary を持つ」前提のテストがある場合、影響を受ける可能性がある。
- **緩和策**: 既存テストの実行で確認し、テスト側を「Footer / Header 未指定なら supplementary なし」の期待値に合わせて更新する。

### 罫線インセット規則のリスク

- インセット位置（Title のリーディング位置）は、アイコンの有無・サイズによって動的に決まるため、CellRegistration 側で正しく境界判定する必要がある。
- **緩和策**: アイコン無し時のインセット = 16pt（標準左マージン）、アイコン有り時のインセット = アイコン枠右端 + 標準左マージン、をデフォルト規則とし、Scenario でカバーする。

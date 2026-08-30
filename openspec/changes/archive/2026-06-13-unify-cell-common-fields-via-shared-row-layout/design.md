## Context

### 現状

- オリジナル `AiForms.Maui.SettingsView/SettingsView/Cells/CellBase.cs` (L1-493) は全 Cell の共通基盤として下記を提供している。
  - `Title` / `TitleColor` / `TitleFontSize` / `TitleFontFamily` / `TitleFontAttributes`
  - `Description` / `DescriptionColor` / `DescriptionFontSize` / `DescriptionFontFamily` / `DescriptionFontAttributes`
  - `HintText` / `HintTextColor` / `HintFontSize` / `HintFontFamily` / `HintFontAttributes`
  - `BackgroundColor` / `IconSource` / `IconSize` / `IconRadius` / `Height` / `IsEnabled`
- KsSettingsView の現状（grep 結果）：
  - `LabelCell`: title / description / valueText / icon / hintText / isEnabled — 既に揃っている
  - `CommandCell`: title / description / valueText / icon / hintText / hideArrow / onTap / isEnabled — 既に揃っている
  - `SwitchCell`: title / description / isOn / accentColor / onValueChanged / isEnabled — `valueText` / `icon` / `hintText` 欠
  - `CheckboxCell`: title / description / isChecked / accentColor / onValueChanged / isEnabled — 同上欠
  - `RadioCell`: title / value / isEnabled — `description` / `valueText` / `icon` / `hintText` / `accentColor` 欠
  - `SimpleCheckCell`: title / isChecked / onValueChanged / isEnabled — 同上欠
  - `ButtonCell`: title / titleColor / onTap / titleAlignment / isEnabled — `valueText` / `icon` / `hintText` 欠（`description` はオリジナルでも無効化されているため移植対象外）
- iOS の Cell View 実装は `UICollectionViewListCell` 派生 (`KsListCellBase`) + `UIListContentConfiguration` ベースで構築されており、`LabelCellView.applyLabelCellContents` (174 行) のような関数で `contentConfiguration` を組み立て、`accessories` 配列に補助 UI を追加する形になっている。
- Android の Cell ViewHolder 実装は RecyclerView の `ViewHolder` 派生 + `ComposeView` ベースで、Compose 関数を呼び出して描画している。
- Change 1 (`port-theme-and-cellstyle-missing-fields`、アーカイブ済み) で Theme/CellStyle の解決順序 (`CellStyle → Theme → 既定`) が確立されており、`EffectiveStyle.titleColor / descriptionColor / valueTextColor / hintTextColor / iconSize / cellBackgroundColor / titleFont / descriptionFont / valueTextFont / hintTextFont` が両プラットフォームで利用可能。

### 制約

- 既存呼び出し（`SwitchCell(title: "...")` 等）は破壊しない（追加フィールドは全て Optional / 既定 nil）。
- `KsListCellBase.preferredLayoutAttributesFitting` override の `CellStyle.cellHeight` 反映を壊さない（共通レイアウト関数からも `KsCellViewSupport.applyEffectiveHeight` を呼べる経路を維持）。
- `Hashable` / `Equatable` の手動実装 (各 Cell の `==` / `hash(into:)`) を破壊しない（追加フィールドを `==` と `hash(into:)` に反映する）。
- iOS `DSLStyleModifiable` / `DSLReidentifiable` / `withDSLID` / `withStyle` の `init` 互換性を維持する（追加フィールドを `withDSLID` / `withStyle` でも引き継ぐ）。
- 既存 in-progress change (`add-cell-types-input` / `add-cell-types-custom`) は新規 Cell 種別の追加のみで、本 change が触れる既存 7 種 Cell には触らない（衝突なしを確認済み）。

### ステークホルダー

- ライブラリ利用者: オリジナル AiForms.Maui.SettingsView から移行するユーザーが、同じ感覚で全 Cell に description / icon / hintText を追加できることを期待。
- ライブラリ開発者: 共通行レイアウト関数 1 つを保守すれば全 Cell のレイアウトが揃うため、保守コスト低下を期待。

## Goals / Non-Goals

**Goals:**

- オリジナル `CellBase` の共通プロパティ概念 (`Description` / `HintText` / `IconSource` / `ValueText`) を全 Cell モデル (7 種) で受けられるようにする。ただし `ButtonCell` の `description` はオリジナルが `private new` で無効化しているため対象外とする（`ButtonCell` には `valueText` / `icon` / `hintText` のみ追加）。
- `RadioCell` / `SimpleCheckCell` に `accentColor` を追加し、`SwitchCell` / `CheckboxCell` と同等の着色 API を揃える。
- 共通行レイアウト関数（iOS: `applyCellBaseLayout(...)`、Android: `applyCellBaseLayout(...)`）を 1 つに集約し、レイアウト重複を排除する。
- 既存 Cell の呼び出し互換性を維持する（破壊的変更なし、追加のみ）。
- Change 1 で確立した `CellStyle → Theme → 既定` の解決順序がそのまま全 Cell に乗ることを保証する。

**Non-Goals:**

- `IsVisible` の追加（Change 3 で扱う）。
- `add-cell-types-input` / `add-cell-types-custom` で追加される新 Cell 種別の対応（衝突回避のため本 change のスコープ外、それぞれが完了時に共通レイアウト関数へ合流する）。
- Theme / CellStyle に新規フィールドを追加する作業（Change 1 で完了済み）。
- オリジナル `CellBase` 由来でも本計画で「廃止」とした項目（`Tapped` event / `Reload()` / MAUI 専用プロパティ等、羅針盤 §4 参照）は移植しない。
- `SwitchCell` / `CheckboxCell` 以外の Cell に独自 control（trailing custom control）を追加する作業。accessory slot は既存の組み合わせ（chevron / Switch / Checkbox / Checkmark / なし）のみで、新規派生は作らない。

## Decisions

### Decision 1: 共通行レイアウトの実現方式は「コンポジションベース」(B 案)

**選択**: 共通行レイアウト関数を 1 つ用意し、各 Cell View / ViewHolder は accessory slot のみ専用実装にする。

**理由**:
- 羅針盤 §0「確定方針」で B 案が確定済み。
- A 案（基底クラス継承）だと iOS の `UIListContentConfiguration` ベースのレイアウトと相性が悪く、accessory の差し替えのために継承階層を深くする必要が出る。SwiftUI への将来的な置き換えも考慮すると、関数（View Builder クロージャ）合成のほうが柔軟。
- Android の `@Composable` 関数の slot API は元々コンポジションを推奨しており、自然にフィットする。

**代替案**:
- A 案（基底クラス継承）: iOS は `UICollectionViewListCell` 派生で基底クラスを 1 段増やし `accessory` を `var accessoryView: UIView?` のテンプレートメソッドとして公開。— 却下: 継承階層を深くすると `KsListCellBase` の `preferredLayoutAttributesFitting` override 経路が複雑化し、リソースリーク（accessoryView の prepareForReuse 忘れ）リスクが上がる。
- C 案（マクロ展開で各 Cell View 毎にレイアウト生成）: コード重複は排除できるが Swift マクロのデバッグコスト高、Android で同等の仕組みがない。

### Decision 2: iOS の共通行レイアウト関数のシグネチャ

**選択**: SwiftUI 版と UIKit 版の **2 段構成** ではなく、**UIKit Builder 関数のみ** を提供する。関数名は本 change の改訂を経て `applyCellBaseLayout` で確定する（旧名 `ksCellRow` からリネーム、Android 側と命名統一）。

```swift
@MainActor
internal func applyCellBaseLayout(
    _ listCell: UICollectionViewListCell,
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    effective: EffectiveStyle,
    isEnabled: Bool,
    accessories: [UICellAccessory] = []
)
```

- `title` / `description` / `valueText` / `icon` を `UIListContentConfiguration` に反映する。
- `description + valueText` 両方ありかつ subtitle 構成のときの `valueText` を `UICellAccessory.customView` として `listCell.accessories` の先頭側（最も content 寄り）に置く。
- `hintText` は **`UICellAccessory` には含めない**。代わりに `UICollectionViewListCell` 直下（`contentView` の外側）に専用 `UILabel`（hintLabel）を `addSubview` して AutoLayout で右上 float 配置する（オリジナル `AiForms.Maui.SettingsView/SettingsView/Native/iOS/Cells/CellBaseView.cs` の `HintLabel` を踏襲：`TopAnchor=2`, `RightAnchor=-10`）。
- `accessories` には Switch / Checkbox / chevron 等の trailing accessory を呼び出し側で組んで渡す。
- 既存の `applyLabelCellContents` ロジックを本関数に移植・一般化する形になる。
- hintLabel のリサイクル管理は方式 A（`KsListCellBase` 派生に `hintLabel: UILabel?` を lazy プロパティとして宣言、`prepareForReuse` で text reset）を推奨。重複 `addSubview` を防ぐ。

**理由**:
- 現状 iOS UI 実装は `UICollectionViewListCell` + `UIListContentConfiguration` ベースで完全に統一されており、SwiftUI ベースに置き換える要件は本 change には含まれない。
- SwiftUI 版を併設すると `UIHostingConfiguration` 経由のメモリ／レイアウトコストや `preferredLayoutAttributesFitting` の互換性検証が新たに必要になり、本 change の純粋目的（共通フィールド横展開）から逸脱する。
- 羅針盤 §2.2.1 では SwiftUI 版が「候補」と記載されているが、design 段階で UIKit Builder のみに絞ることで実装スコープを最小化する。

**代替案**:
- SwiftUI 版 (`@ViewBuilder`) を併設: 将来的な SwiftUI ネイティブ移行に備えられるが、本 change では二重実装になりレビューコストが倍増。却下。

### Decision 3: Android の共通行レイアウト関数のシグネチャ（改訂 — View ベース）

**選択（改訂）**: Android の共通行レイアウトは **View ベース（`ConstraintLayout` programmatic 構築）** で実装し、`internal fun applyCellBaseLayout(views: CellBaseViews, ...)` 関数として提供する。`@Composable` 関数 `KsCellRow` は採用しない（後述 Decision 11 参照）。

```kotlin
internal class CellBaseViews(
    val root: ConstraintLayout,
    val iconView: ImageView,
    val titleView: TextView,
    val descriptionView: TextView,
    val valueTextView: TextView,
    val accessoryHolder: FrameLayout,
    val hintTextView: TextView,
)

internal fun applyCellBaseLayout(
    views: CellBaseViews,
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    effective: EffectiveStyle,
    isEnabled: Boolean,
)
```

- `CellBaseViews` の構築は programmatic（XML 不使用）。`ConstraintLayout` をルートとし、`iconView`（左端中央）／`titleView` ＋ `descriptionView`（中央列）／`valueTextView`（title 行右寄せ、`Baseline=titleView.Baseline`）／`accessoryHolder`（右端中央、`FrameLayout`）／`hintTextView`（右上 float、accessoryHolder より後に addView で前面）を配置する。
- `accessoryHolder` は Cell 種別固有の trailing コントロール（`MaterialSwitch` / `MaterialCheckBox` / `KsCheckmarkAccessoryView` / chevron 等）を `addView` する場所として各 ViewHolder に提供される。
- 既存の `LabelCellViewHolder` 内で組まれている View レイアウト（`LabelCellViews` + `applyLabelCellContents`）を本関数に集約する。`LabelCellViews` は `CellBaseViews` にリネーム＆拡張、`applyLabelCellContents` は `applyCellBaseLayout` にリネーム＆一般化する。

**理由**:
- オリジナル `AiForms.Maui.SettingsView` の Android 実装が `RelativeLayout` ベースの View ヒエラルキー（`SettingsView/Platforms/Android/Resources/layout/cellbaseview.axml`）であり、踏襲性が高い。
- MAUI 移植を視野に入れた場合、View ベースのほうが MAUI ハンドラとの相性が良い（Compose 経由は MAUI 移植時に再実装が必要になる）。
- `ComposeView` 経由は recomposition オーバーヘッド・初期化コストが高く、RecyclerView の高速スクロール時のパフォーマンス劣化リスクがある。View ベースは初回構築コストはあるが以後の `bind` は cheap。
- 既存実装が既に `LabelCellViews` + `applyLabelCellContents` という View ベースで動作しており、改訂後はこれを拡張する形で滑らかに移行できる。

**代替案**:
- `@Composable fun KsCellRow(...)`（旧版 Decision 3）: 採用しない。Compose は本 change の本質ではなく、View ベースで十分に共通レイアウト統一が達成可能。Compose のメリット（slot API）よりも MAUI 移植性・パフォーマンス・オリジナル踏襲性のメリットを優先する。
- XML レイアウトファイル（`layout/cellbaseview.xml`）への切り出し: programmatic 構築のほうがプロパティ反映が容易で、`Theme/CellStyle` 由来の色・サイズ・マージンの動的反映が `style` 属性経由よりも素直に書ける。programmatic を採用。

### Decision 4: 各 Cell View / ViewHolder からの呼び出し方（改訂）

- iOS の各 Cell View（`SwitchCellView` 等）は `render(cell:theme:)` 内で `applyCellBaseLayout(...)` を呼び出し、accessory slot 部分には自前で組んだ `UISwitch` を `UICellAccessory.customView` でラップして `accessories` 引数に渡す。
- Android の各 CellViewHolder は、コンストラクタで `CellBaseViews` を programmatic に構築して保持し、`bind(cell, theme)` 内で `applyCellBaseLayout(views, ...)` を呼び出し、その後 `views.accessoryHolder` に `MaterialSwitch` 等の trailing コントロールを `addView` する。
- `LabelCellView` / `LabelCellViewHolder` も同じ関数を使う（accessoryHolder は空）。`CommandCellView` / `CommandCellViewHolder` も同じ関数 + chevron accessory。
- Android 側は **`ComposeView.setContent { ... }` を使ってはならない**。共通行レイアウトは純粋な View ヒエラルキーで構築する。

### Decision 5: モデル拡張時の `Hashable` / `Equatable` の扱い

**選択**: 追加フィールド (`valueText` / `icon` / `hintText` / `description` / `accentColor`) を `==` / `hash(into:)` / `equals` / `hashCode` にそのまま追記する。`KsImage` の `Hashable` 契約は既に確立されているため流用可能（`uiImage` 派生は参照同一性、`Resource` / `SystemName` は値同一性）。

**理由**: 既存 `LabelCell` / `CommandCell` がこのパターンで動作しており、Diff（構造同期）の id 同一性判定 → 内容変化検知 → reconfigure の流れが正常に機能する。`accentColor` の `Equatable` は `uiColorEqualOptional` ヘルパ（iOS）/ `Color` の `equals`（Android）を使う。

### Decision 6: `withDSLID` / `withStyle` の更新

**選択**: 各 Cell の `withDSLID(_ id:)` / `withStyle(_:)` 実装で追加フィールドを引き継ぐようコンストラクタ引数を増やす。

**理由**: 既存 `LabelCell.withDSLID` 等がこのパターンで動作しており、フィールド追加に伴う機械的な拡張で済む。DSL 経路（`Section { SwitchCell(...) }`）で追加フィールドが落ちないことを保証する。

### Decision 7: DSL 拡張関数の引数追加

**選択**: iOS / Android 双方で `Section` 拡張の Cell DSL 関数（例: `fun DSLSectionScope.SwitchCell(title, description, isOn, accentColor, ...)`）に追加フィールドを Optional 引数として追記する。

**理由**: DSL 経由で Cell を構築する利用者にも新フィールドを使えるようにするため。引数省略時は既存呼び出しと完全互換。

### Decision 8: 共通行レイアウト関数の所属モジュール

**選択**:
- iOS: `KsSettingsViewUI` モジュール内 `CellBaseLayout.swift` に `internal` で配置（公開 API ではない）。
- Android: `ks-settingsview-ui` モジュール内 `CellBaseLayout.kt` に `internal` で配置（公開 API ではない）。

**理由**: 共通行レイアウト関数は UI 層の実装詳細であり、利用者が直接呼ぶ API ではない。`add-cell-types-input` / `add-cell-types-custom` 等の後続 change が同モジュールから内部利用できれば十分。

**代替案**:
- `public` で公開: 利用者がカスタム Cell を作るとき同じレイアウトを使えるメリットあり。— 却下: 本 change のスコープ外（`add-cell-types-custom` 完了後に検討）。

### Decision 9: hintText / valueText の配置（改訂 — hintText は右上 float へ）

iOS の旧版 `applyLabelCellContents` では:
- `description + valueText` 両方あり → `subtitleCell()` + `valueText` を trailing accessory として `customView`
- `hintText` がある → `hintText` を trailing accessory として `customView`（accessory 列に横並び）

**改訂後の選択**:
- `valueText` の trailing accessory 化（subtitle 構成時）は **維持**。
- `hintText` の trailing accessory 化は **撤回**。`hintText` は本体行とは別系統として **セル右上に float 配置** する。
  - iOS: `UICollectionViewListCell` 直下に `hintLabel: UILabel` を `addSubview` し、AutoLayout で `Top=cell.topAnchor + 2`, `Trailing=cell.contentView.trailingAnchor - 10` の右上 float 配置（オリジナル `CellBaseView.cs` の `HintLabel` を踏襲）。
  - Android: `CellBaseViews.hintTextView` を `ConstraintLayout` 制約 `End=parent.End`, `Top=parent.Top` で右上に配置（オリジナル `cellbaseview.axml` の `CellHintText` を踏襲）。
- accessory 順は `[valueText (subtitle 構成時のみ), ユーザー渡しの accessories]` の trailing 配置とする。**`hintText` は accessories に含めない**。

**理由**:
- 旧版で hintText を accessory として横並びに置く規約は KsSettingsView 独自の表記であり、オリジナル `AiForms.Maui.SettingsView` のレイアウト思想（hintText はセル右上に float）と乖離していた。
- オリジナル踏襲のためには iOS で `contentView` 外側 subview、Android で `RelativeLayout` 系 `alignParentTop + alignParentRight` 配置が必要。本 change で `cell-types-basic` spec を「本体行 + hintText 右上 float」の 2 系統に整理する。
- 既存 LabelCell / CommandCell の見た目は変更されるが、本 change は描画規約の確定タイミングとして適切（後続 change で hintText 互換性問題が顕在化する前に修正する）。

### Decision 11: Android は View ベース ConstraintLayout を採用（Compose 化見送り）

**選択**: Android の共通行レイアウトは Compose（`@Composable fun KsCellRow`）ではなく、純粋な View ベース `ConstraintLayout` で実装する。Phase 6 の旧版で実装された `KsCellRow.kt` Composable は本 change の改訂版で **完全削除** する。

**経緯**:
- 旧版（CHANGES_REQUESTED 前）の design では `@Composable fun KsCellRow` を採用していたが、レビューで「`KsCellRow` がプロダクションから 1 度も呼ばれていないデッドコード」と指摘された。
- 議論の結果、(A) Android Phase 8 を Compose ベースで完遂する、または (B) View ベースに方針転換する、の二択となった。
- KsSettingsView は将来 MAUI 移植を控えており、Compose ベース実装は MAUI 移植時に作り直しが必要になる。一方 View ベースであれば MAUI ハンドラ層との接続が直接的で移植コストが低い。
- パフォーマンス面でも RecyclerView 上で `ComposeView.setContent` を多数走らせるよりも、純粋な View ヒエラルキーでの `bind` のほうが高速。
- オリジナル `AiForms.Maui.SettingsView` の Android 実装が `RelativeLayout` ベースであり、踏襲性も View ベースが優位。
- これらを踏まえ、(B) を採用し、Compose を Android UI 内部実装から完全に取り除く方針とする。

**影響**:
- Phase 6 の `KsCellRow.kt` ファイル削除。
- Phase 8 を「ViewHolder の `CellBaseViews` + `applyCellBaseLayout` 経由統一」として再構築。
- Phase 10 の Compose Test を「ConstraintLayout 配置回帰テスト」として再構築。

**代替案**:
- (A) Phase 8 を Compose ベースで完遂: 旧 spec のまま `KsCellRow` 経由化を全 ViewHolder に適用する案。— 却下: MAUI 移植時の作り直しコスト、RecyclerView パフォーマンス、オリジナル踏襲性のいずれにおいても View ベースが優位。

### Decision 12: hintText の右上 float 配置（両プラットフォーム共通）

**選択**: `hintText` は iOS / Android 両プラットフォームで「セル右上に float 配置」とし、本体行（icon / title / description / valueText / accessory）とは別系統で扱う。

**経緯**:
- KsSettingsView の旧版実装では hintText を accessory と横並びに配置していたが、これはオリジナル `AiForms.Maui.SettingsView` のレイアウト思想と乖離していた。
- オリジナル iOS（`SettingsView/Native/iOS/Cells/CellBaseView.cs`）: `HintLabel` は `UITableViewCell` 直下（ContentView の外）に `AddSubview`、`TopAnchor=2`, `RightAnchor=-10` で右上 float。
- オリジナル Android（`SettingsView/Platforms/Android/Resources/layout/cellbaseview.axml`）: `CellHintText` は `RelativeLayout` の `alignParentTop + alignParentRight` で右上 float。
- 両者とも、accessory（Switch 等）はセル縦中央、hintText はセル上端基準で配置されるため、両者は右端揃いながら縦位置が分離して通常は重ならない。

**実装方法**:
- iOS: `UICollectionViewListCell` 直下に `hintLabel: UILabel` を `addSubview` し、AutoLayout で `topAnchor = cell.topAnchor + 2`, `trailingAnchor = cell.contentView.trailingAnchor - 10`, `bottomAnchor <= cell.bottomAnchor - 12` の右上 float 配置。
- Android: `CellBaseViews.hintTextView` を `ConstraintLayout` の制約 `End=parent.End`, `Top=parent.Top`（セル上端から数 dp のマージン）で右上に配置。`accessoryHolder` より後に `addView` することで Z 順の前面に置く。
- 両プラットフォームとも `hintTextView` / `hintLabel` は `singleLine`（1 行表示）、`ellipsize = end` / `lineBreakMode = .byTruncatingTail`（末尾省略）、`gravity / textAlignment = right`（右寄せ）、小さなフォント（既定 10sp ≒ `.footnote` 程度）。

**理由**:
- オリジナル踏襲。
- accessory と hintText の物理的衝突回避（縦位置分離）。
- 「hintText は補助情報、accessory は操作対象」という意味的分離もレイアウト上で表現される。

**Trade-off**:
- 旧版 KsSettingsView の利用者（あれば）は、hintText 位置が変わる視覚的差分を受ける。本 change は v0.x 時点（pre-1.0）であり破壊的変更とは扱わない。
- accessory と hintText が両方とも長いケースでは縦方向に詰まる可能性があるが、`accessoryHolder` は CenterVertical で本体行内に収まるため、通常のセル高さでは問題にならない。

### Decision 10: spec 更新範囲

**選択**: `cell-types-basic` を **MODIFIED** とし、

- Switch / Checkbox / Radio / SimpleCheck Requirement に共通フィールド (`description` / `valueText` / `icon` / `hintText`) を追記。
- Button Requirement に共通フィールド (`valueText` / `icon` / `hintText`) を追記（`description` はオリジナル踏襲で **追加しない**）。
- Radio / SimpleCheck に `accentColor` を追記。
- 「全 Cell 共通の `description` / `valueText` / `icon` / `hintText` フィールド」 ADDED Requirement を新設して、全 Cell 横断の規約として明文化する。本 Requirement の本文内で `ButtonCell` の `description` 除外を例外として明記する。
- 既存 Requirement「全 Cell 共通の Theme.titleColor / Theme.titleFont 反映」(旧名のまま残存) を MODIFIED で `Theme.cellTitleColor` / `Theme.cellTitleFont` に正規化する（Q3 巻き取り）。Requirement 名は MODIFIED の頭出しに合わせて旧名を使い、本文と Scenario で新名へ統一する。

`settings-view-ios-swiftui` / `settings-view-android-compose` を **MODIFIED** とし、共通行レイアウト関数（実装詳細）の I/F 規約を ADDED Requirement として追加。

**理由**:
- 共通フィールドの存在は各 Cell の Requirement で個別に明記しても良いが、「全 Cell 共通」と書いたほうが将来の Cell 追加時にも参照基準として機能する（既存 `全 Cell 共通の isEnabled` Requirement と同パターン）。
- 共通行レイアウト関数は実装詳細だが、Requirement レベルで「共通レイアウト関数を経由する」と明文化することで、後続 change での重複実装を防ぐ。
- `Theme.titleColor` → `Theme.cellTitleColor` の表記揺れを本 change で巻き取ることで、cell-types-basic spec 全体で Change 1 以降の命名が一貫する。

## Risks / Trade-offs

### [Risk] iOS の `UIListContentConfiguration` ベースのレイアウトでは「ヘッダ右上の hintText」「accessory の組み合わせ順」等の微妙な見え方が現状 `LabelCellView` のロジックに依存している

→ **Mitigation**: 共通行レイアウト関数 `applyCellBaseLayout(...)` を既存 `applyLabelCellContents` の **直接の置き換え** として実装し、まず LabelCell / CommandCell でテストを通す。次に Switch / Checkbox 等を移行する段階的アプローチを取る。スナップショット相当の視覚検証はサンプルアプリで実機確認（マニュアル）+ ユニットテストで `contentConfiguration` / `accessories` の構成を assert。

### [Risk] Android の `KsCellRow` を ComposeView 内で呼ぶと recomposition の単位が変わる可能性（撤回）

→ **撤回**: Decision 11 により Android は Compose を採用しないため、本 Risk は消滅。代わりに以下の Risk を追加で扱う。

### [Risk] Android `CellBaseViews` の programmatic ConstraintLayout 構築コストが高い

→ **Mitigation**: `CellBaseViews` の構築は ViewHolder の **コンストラクタで 1 回のみ** 実行し、`bind(...)` では既存 View 参照に対するプロパティ反映のみを行う。RecyclerView のリサイクル機構により ViewHolder 自体も再利用されるため、constraint set のセットアップは N 個の ViewHolder（≒ 画面に表示中のセル数 + 数個のバッファ）のみで済む。

### [Risk] Compose ベース実装からの移行で旧 `KsCellRow.kt` 削除に伴うテスト破壊

→ **Mitigation**: 旧 Phase 10 の Compose Test（`KsCellRow` の Composable 単体テスト）は本 change の改訂で **削除** し、ConstraintLayout 配置回帰テスト（`CellBaseViews` の `hintTextView` 右上 float 配置、`accessoryHolder` 右端中央配置、両者の物理的非干渉）に置き換える。`Robolectric` または `androidx.test.ext` を用いて measure / layout 後の座標を assert する。

### [Risk] hintText の右上 float 配置移行で既存サンプルや利用者の視覚的差分

→ **Mitigation**: 本 change は v0.x 時点（pre-1.0）であり、視覚的差分は破壊的変更には該当しない。サンプルアプリ側で hintText 表示位置の差分を実機確認し、ドキュメントには「hintText はオリジナル AiForms 踏襲の右上 float 配置」と明記する。既存テストの assert（旧 accessory 列に hintText が含まれる前提のテスト）は本 change で更新する。

### [Risk] `KsListCellBase.preferredLayoutAttributesFitting` の override が共通レイアウト関数経由でも効くこと

→ **Mitigation**: `applyCellBaseLayout(...)` 内で `KsCellViewSupport.applyEffectiveHeight(listCell, effective:)` を呼ぶ。既存 `applyLabelCellContents` が既にこのパターンで動作しているため、関数を切り出しても挙動は変わらない。Phase 17 のテスト (`test_視覚的セル高さ_cellHeight80指定時...`) を本 change のテストでも再走させ、回帰しないことを確認。

### [Risk] `RadioCell` / `SimpleCheckCell` への `accentColor` 追加で既存の単色表示（system tint）が破壊される可能性

→ **Mitigation**: `accentColor` の既定値を `nil` とし、`nil` のときは現状の表示（`Theme.cellAccentColor` または system tint）を維持する。Switch / Checkbox の解決順序 (`Cell.accentColor → CellStyle.accentColor → Theme.cellAccentColor → system tint`) と一致させる。

### [Risk] DSL 拡張関数の引数追加で利用者の名前付き引数呼び出しに ABI ぶれが生じる可能性

→ **Mitigation**: 追加引数は既存引数の **後ろ** に Optional として追加し、既存呼び出し（名前付き引数を含む）を破壊しない。サンプルアプリのコードと現状 in-progress change を grep で確認し、影響箇所をカウント。

### [Trade-off] 共通行レイアウト関数を `internal` にすることで、利用者がカスタム Cell を作るときに同レイアウトを再利用できない

→ 受容。本 change のスコープは既存 7 種 Cell の共通フィールド横展開であり、カスタム Cell サポートは `add-cell-types-custom` 側で別途検討する。後日 `public` 化が必要になれば API 公開する別 change を立てる。

### [Trade-off] iOS で SwiftUI 版の共通レイアウト関数を併設しない

→ 受容。SwiftUI 統合は別ロードマップで検討する（本 change のスコープを最小化）。

## Open Questions

- **Q1 (確定)**: `ButtonCell` の `description` フィールドは **追加しない**。オリジナル `AiForms.Maui.SettingsView/SettingsView/Cells/ButtonCell.cs` が `Description` / `DescriptionColor` / `DescriptionFontSize` を `private new` で隠蔽し、iOS の `ButtonCellView.cs` も `DescriptionLabel.Hidden = true` としている挙動を踏襲する。`ButtonCell` には `valueText` / `icon` / `hintText` のみ追加する。これに伴い `titleAlignment` のレイアウト分岐は「`icon` / `valueText` / `hintText` のいずれかが指定された場合は通常レイアウト + title 列内 `titleAlignment` 反映、すべて nil のときボタンスタイル（titleAlignment 全体反映）」となる。
- **Q2 (確定)**: 共通行レイアウト関数のテスト戦略は、ユニットテストで `contentConfiguration.text` / `accessories.count` 等の構成を assert し、視覚回帰は samples アプリで実機確認（マニュアル）する。スナップショットテスト導入は別途検討。
- **Q3 (確定)**: `cell-types-basic` spec の `Theme.titleColor` 表記揺れ（Change 1 で `cellTitleColor` に rename されたが cell-types-basic spec 側に旧名が残っている箇所がある）の修正を **本 change で巻き取る**。具体的には既存 Requirement「全 Cell 共通の Theme.titleColor / Theme.titleFont 反映」を MODIFIED で名称・本文・Scenario を `Theme.cellTitleColor` / `Theme.cellTitleFont` に正規化する。

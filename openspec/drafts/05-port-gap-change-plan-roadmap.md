# オリジナル移植漏れ対応 — Change 提案 詳細計画（羅針盤）

作成日: 2026-06-08
前提資料: [04-original-property-port-gap-survey.md](./04-original-property-port-gap-survey.md)
ステータス: 計画確定（ユーザー方針決定済み、未着手）

## オリジナルソース参照（共通）

移植元 `AiForms.Maui.SettingsView` の対応するソースファイル。各 change の実装時に**必ず参照して移植元の意図を確認**すること。

### SettingsView 本体
- `AiForms.Maui.SettingsView/SettingsView/SettingsView.cs`
- `AiForms.Maui.SettingsView/SettingsView/SettingsView.DefineProperites.cs` — 全 BindableProperty 定義（40+）
- `AiForms.Maui.SettingsView/SettingsView/SettingsViewConfiguration.cs`

### Section / Root
- `AiForms.Maui.SettingsView/SettingsView/Section.cs`
- `AiForms.Maui.SettingsView/SettingsView/SectionBase.cs`
- `AiForms.Maui.SettingsView/SettingsView/SettingsRoot.cs`
- `AiForms.Maui.SettingsView/SettingsView/SettingsModel.cs`

### Cells
- `AiForms.Maui.SettingsView/SettingsView/Cells/CellBase.cs` — 共通プロパティ基盤
- `AiForms.Maui.SettingsView/SettingsView/Cells/LabelCell.cs`
- `AiForms.Maui.SettingsView/SettingsView/Cells/CommandCell.cs`
- `AiForms.Maui.SettingsView/SettingsView/Cells/ButtonCell.cs`
- `AiForms.Maui.SettingsView/SettingsView/Cells/SwitchCell.cs`
- `AiForms.Maui.SettingsView/SettingsView/Cells/CheckboxCell.cs`
- `AiForms.Maui.SettingsView/SettingsView/Cells/RadioCell.cs`
- `AiForms.Maui.SettingsView/SettingsView/Cells/SimpleCheckCell.cs`
- `AiForms.Maui.SettingsView/SettingsView/Cells/EntryCell.cs`（Change スコープ外、参考）
- `AiForms.Maui.SettingsView/SettingsView/Cells/PickerCell.cs`（Change スコープ外、参考）
- `AiForms.Maui.SettingsView/SettingsView/Cells/TextPickerCell.cs`（Change スコープ外、参考）
- `AiForms.Maui.SettingsView/SettingsView/Cells/NumberPickerCell.cs`（Change スコープ外、参考）
- `AiForms.Maui.SettingsView/SettingsView/Cells/DatePickerCell.cs`（Change スコープ外、参考）
- `AiForms.Maui.SettingsView/SettingsView/Cells/TimePickerCell.cs`（Change スコープ外、参考）
- `AiForms.Maui.SettingsView/SettingsView/Cells/CustomCell.cs`（Change スコープ外、参考）

### Platforms（ネイティブハンドラ実装、参考用）
- `AiForms.Maui.SettingsView/SettingsView/Handlers/` — MAUI Handler 実装
- `AiForms.Maui.SettingsView/SettingsView/Platforms/Android/` — Android Native 実装
- `AiForms.Maui.SettingsView/SettingsView/Platforms/iOS/` — iOS Native 実装
- `AiForms.Maui.SettingsView/SettingsView/Native/` — Native 共通

## 0. 全体方針

オリジナル `AiForms.Maui.SettingsView` のプロパティ移植漏れを 3 本の独立 change に分割して順次対応する。

### 確定方針

| 項目 | 決定 |
|------|------|
| 順序 | **1 本ずつ順次**（Change 1 → 2 → 3）|
| 共通レイアウト方式 | **B: コンポジションベース**（共通行レイアウト関数 + accessory slot）|
| Theme 表記揺れ rename | **今やる**（Change 1 内で破壊的変更として実施）|

### 進行ステータス管理

```
[ ] Change 1: port-theme-and-cellstyle-missing-fields
    [x] proposal.md / design.md / tasks.md / specs delta
    [x] sdd-spec-reviewer 承認
    [x] 実装
    [x] アーカイブ
[ ] Change 2: unify-cell-common-fields-via-shared-row-layout
    [x] proposal.md / design.md / tasks.md / specs delta
    [x] sdd-spec-reviewer 承認
    [x] 実装
    [x] アーカイブ
[ ] Change 3: add-visibility-flags-section-and-cell
    [ ] proposal.md / design.md / tasks.md / specs delta
    [ ] sdd-spec-reviewer 承認
    [ ] 実装
    [ ] アーカイブ
```

各 change 着手時にこのチェックボックスを更新する。

## 1. Change 1: `port-theme-and-cellstyle-missing-fields`

### 1.0 オリジナル参照（必読）

本 change の実装前に **必ず** 以下のオリジナルソースを読み、移植元の意図・既定値・型・挙動を把握すること。

- **`AiForms.Maui.SettingsView/SettingsView/SettingsView.DefineProperites.cs`** — 全プロパティ定義の出発点
  - `BackgroundColor` (L33-50) → `Theme.backgroundColor`
  - `SeparatorColor` (L55-72)
  - `SelectedColor` (L77-94)
  - `HeaderPadding` (L99-116) — 廃止
  - `HeaderTextColor` (L121-138)
  - `HeaderFontSize` (L143-162) — `DeviceInfo.Platform` による既定値分岐に注意
  - `HeaderFontFamily` (L164-175)
  - `HeaderFontAttributes` (L177-188)
  - `HeaderTextVerticalAlign` (L193-210) — 廃止
  - `HeaderBackgroundColor` (L215-232)
  - `RowHeight` (L237-254)
  - `HasUnevenRows` (L259-276)
  - `HeaderHeight` (L281-298) — **SettingsView 全体既定として復活**
  - `FooterTextColor` (L303-320)
  - `FooterFontSize` (L325-344)
  - `FooterFontFamily` (L346-357)
  - `FooterFontAttributes` (L359-370)
  - `FooterBackgroundColor` (L375-392)
  - `FooterPadding` (L397-414) — 廃止
  - `CellTitleColor` (L419-436)
  - `CellTitleFontSize` (L441-460) — **Theme と CellStyle 両方で対応**
  - `CellTitleFontFamily` (L465-478)
  - `CellTitleFontAttributes` (L480-491)
  - `CellValueTextColor` (L496-513) — **Theme へ追加**
  - `CellValueTextFontSize` (L518-536) — **Theme へ追加**
  - `CellValueTextFontFamily` (L538-549) — **Theme へ追加**
  - `CellValueTextFontAttributes` (L551-562) — **Theme へ追加**
  - `CellDescriptionColor` (L567-584) — **Theme へ追加**
  - `CellDescriptionFontSize` (L589-607) — **Theme へ追加**
  - `CellDescriptionFontFamily` (L609-620) — **Theme へ追加**
  - `CellDescriptionFontAttributes` (L622-633) — **Theme へ追加**
  - `CellBackgroundColor` (L638-655)
  - `CellIconSize` (L660-678) — **Theme へ追加**
  - `CellIconRadius` (L683-700) — **Theme へ追加**
  - `CellAccentColor` (L705-722)
  - `CellHintTextColor` (L727-744) — **Theme へ追加**
  - `CellHintFontSize` (L749-767) — **Theme へ追加**
  - `CellHintFontFamily` (L769-780) — **Theme へ追加**
  - `CellHintFontAttributes` (L782-793) — **Theme へ追加**

### 1.1 Why

オリジナル `SettingsView` クラスは `CellTitleColor` / `CellTitleFontSize` / `CellValueTextColor` / `CellDescriptionColor` / `CellHintTextColor` / `CellIconSize` / `CellIconRadius` 等の「Cell 全体既定」を持っていたが、KsSettingsView の `Theme` には `titleColor` / `titleFont` のみが昇格しており、残りの全体既定が抜けている。利用者は「`Theme.CellHintTextColor` を 1 箇所セットすれば全 Cell のヒント文字色が変わる」運用ができず、各 Cell ごとに `CellStyle.hintTextColor` を設定する必要がある。表示互換性を確保するため Theme の二段化（全体既定 → 個別）を完成させる。

また `Theme.viewBackgroundColor` / `Theme.titleColor` はオリジナル命名（`BackgroundColor` / `CellTitleColor`）との表記揺れがあるため、利用者がまだ少ない基礎段階で rename しておく。

### 1.2 What Changes

#### 1.2.1 Theme リネーム（破壊的変更）

| 現状 | → 修正後 | 理由 |
|------|----------|------|
| `Theme.viewBackgroundColor` | `Theme.backgroundColor` | オリジナル `BackgroundColor` と整合 |
| `Theme.titleColor` | `Theme.cellTitleColor` | オリジナル `CellTitleColor` と整合 |

互換シムは **置かない**（基礎段階のため）。利用者は呼び出し箇所を一括書き換え。

#### 1.2.2 Theme フィールド追加（漏れ補完）

ヘッダ/フッタ追加：
| 追加フィールド | 型（iOS / Android）| 既定値 | 由来 |
|----------------|-------------------|--------|------|
| `headerFontFamily`（相当） | `UIFont` / `TextStyle` 経由 | nil/null | `HeaderFontFamily` |
| `headerFontAttributes`（相当） | `UIFont` / `TextStyle` 経由 | nil/null | `HeaderFontAttributes` |
| `footerFontFamily`（相当） | `UIFont` / `TextStyle` 経由 | nil/null | `FooterFontFamily` |
| `footerFontAttributes`（相当） | `UIFont` / `TextStyle` 経由 | nil/null | `FooterFontAttributes` |
| `headerHeight` | `Double` | -1.0（自動）| `SettingsView.HeaderHeight`（全体既定）|

Cell 全体既定追加：
| 追加フィールド | 型（iOS / Android）| 既定値 | 由来 |
|----------------|-------------------|--------|------|
| `cellTitleFontSize` | `Double` | -1.0 | `CellTitleFontSize` |
| `cellValueTextColor` | `UIColor?` / `Color?` | nil/null | `CellValueTextColor` |
| `cellValueTextFont` | `UIFont?` / `TextStyle?` | nil/null | `CellValueTextFontSize/Family/Attributes` |
| `cellDescriptionColor` | `UIColor?` / `Color?` | nil/null | `CellDescriptionColor` |
| `cellDescriptionFont` | `UIFont?` / `TextStyle?` | nil/null | `CellDescriptionFontSize/Family/Attributes` |
| `cellHintTextColor` | `UIColor?` / `Color?` | nil/null | `CellHintTextColor` |
| `cellHintFont` | `UIFont?` / `TextStyle?` | nil/null | `CellHintFontSize/Family/Attributes` |
| `cellIconSize` | `CGSize?` / `Size?` | nil/null | `CellIconSize` |
| `cellIconRadius` | `CGFloat?` / `Dp?` | nil/null | `CellIconRadius` |

注: `headerFont` / `footerFont` は **既存の `headerFontSize` / `footerFontSize` (`Double`)** と並立するか、`UIFont` / `TextStyle` 型の単一フィールドに統合するかは design.md で決定する。**推奨**: 既存 fontSize は残し、`headerFont: UIFont?` / `headerFont: TextStyle?` を追加して family/weight/attributes を集約。`fontSize` と `font` が両方非 nil の場合は `font.size` を優先（design.md で正式化）。

#### 1.2.3 CellStyle フィールド追加・確認

CellStyle に未移植項目を追加（既にあるものは確認のみ）：

| CellStyle フィールド | 既存? | アクション |
|---------------------|-------|-----------|
| `titleFontSize` | iOS: `titleFont` のみ / Android: `titleFont` のみ | Theme 側と同様、`titleFont` に集約済みでよい（Theme と整合） |
| `valueTextColor` | あり（両方）| なし |
| `valueTextFont` | あり（両方）| なし |
| `descriptionColor` | あり（両方）| なし |
| `descriptionFont` | あり（両方）| なし |
| `hintTextColor` | あり（両方）| なし |
| `hintTextFont` | あり（両方）| なし |
| `iconSize` | あり（両方）| なし |
| `iconRadius` | あり（両方）| なし |

→ CellStyle 側は **追加は最小**。Theme 側に対応する全体既定が追加されることで「CellStyle の null フィールドが Theme から落ちてくる」解決順序が完成する。

#### 1.2.4 解決順序（EffectiveStyle）の明示化

「Cell の最終表示プロパティ」は以下の順序で解決される：

```
最終値 = CellStyle.X  if X != nil
       else Theme.cellX  if cellX != nil
       else プラットフォーム既定
```

**例外**: `ButtonCell.titleColor` は cell-types-basic spec の既存 Requirement に従い、以下の 4 段優先：

```
ButtonCell.titleColor → CellStyle.titleColor → Theme.cellTitleColor → プラットフォーム既定
```

iOS / Android の `EffectiveStyle` ユーティリティを拡張して全プロパティに解決順序を実装する。

#### 1.2.5 fontFamily 課題の解決

既存 Theme で「fontFamily に課題があった」との指示。Compose `TextStyle.fontFamily` の equals 判定が `FontFamily` の参照比較に依存するケースが Theme.kt のコメントに記載されている（Compose 1.5 以降は概ね安定）。

タスク:
- 同一 `FontFamily` インスタンスを再利用した場合の equals が安定することをテスト
- 異なる `FontFamily` インスタンスでも同等の家族なら equals するか、Compose の挙動を明文化
- iOS は `UIFont.isEqual(_:)` ベースで既に動作実績があるので確認のみ
- font.size の指定が確実に効くこと（Compose `TextStyle(fontSize = ...)` でレイアウト反映、UIFont で pointSize 反映）の e2e テスト

### 1.3 影響 spec / 影響モジュール

**MODIFIED specs:**
- `settings-view-android-style` — Theme / CellStyle Requirement
- `settings-view-ios-style` — Theme / CellStyle Requirement

**影響モジュール:**
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellStyle.kt`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt`
- `ios/Sources/KsSettingsViewUI/Theme.swift`
- `ios/Sources/KsSettingsViewUI/CellStyle.swift`
- `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift`
- 既存 Cell ビュー（LabelCellView / CommandCellView / LabelCellViewHolder / CommandCellViewHolder）の EffectiveStyle 呼び出し箇所

### 1.4 Breaking changes

- `Theme.viewBackgroundColor` → `Theme.backgroundColor`（rename）
- `Theme.titleColor` → `Theme.cellTitleColor`（rename）

サンプル `samples/android` / `samples/ios` 内の呼び出しも更新する。**互換シム（旧名 deprecated 残し）は置かない**。

### 1.5 Tasks（概要、proposal 着手時に細分化）

1. design.md で `Theme` の `Font` フィールド統合方針を決定
2. iOS Theme.swift に新フィールド追加 + 既定値定数
3. iOS Theme.swift で rename 適用
4. iOS CellStyle.swift で漏れ確認
5. iOS EffectiveStyle.swift で全プロパティの解決順序を実装
6. iOS Cell View（LabelCellView / CommandCellView）から EffectiveStyle 経由で値を取得するよう書き換え
7. iOS テスト追加（解決順序、UIFont equals、UIColor equals、rename 反映）
8. Android Theme.kt に新フィールド追加 + 既定値 companion
9. Android Theme.kt で rename 適用
10. Android CellStyle.kt で漏れ確認
11. Android EffectiveStyle.kt で全プロパティの解決順序を実装
12. Android Cell ViewHolder（LabelCellViewHolder / CommandCellViewHolder）から EffectiveStyle 経由で値を取得するよう書き換え
13. Android テスト追加（解決順序、TextStyle equals、Color equals、rename 反映、fontFamily 反映）
14. samples の Theme / CellStyle 利用箇所を新 API に移行
15. ドキュメント更新（README / Theme コメント）

### 1.6 完了基準

- `swift test` / `./gradlew :ks-settingsview-ui:test` がすべて成功
- 全 Cell の title / description / valueText / hintText / icon が `CellStyle → Theme → 既定` の順で解決される
- `Theme.backgroundColor` / `Theme.cellTitleColor` の新名が全箇所で使われている
- `fontFamily` がサンプルアプリで視覚的に反映される

### 1.7 着手前チェック

- [ ] 既存 in-progress change（`add-cell-types-input` / `add-cell-types-custom`）が Theme に新フィールドを追加していないか確認
- [ ] `purify-core-extract-style-to-ui-layer` archive を読み、Theme 設計の思想を再確認
- [ ] sdd-spec-reviewer を Theme / CellStyle に詳しい仕様ガイドとして起動

## 2. Change 2: `unify-cell-common-fields-via-shared-row-layout`

### 2.0 オリジナル参照（必読）

本 change は **CellBase の共通プロパティ概念** を取り戻すため、オリジナル CellBase と各 Cell の関係を必ず把握すること。

- **`AiForms.Maui.SettingsView/SettingsView/Cells/CellBase.cs`** — 全 Cell の共通基盤（L1-493）
  - `Title` (L22-39)
  - `TitleColor` (L44-61)
  - `TitleFontSize` (L66-84)
  - `TitleFontFamily` (L86-98)
  - `TitleFontAttributes` (L100-112)
  - `Description` (L117-134) — **全 Cell で受けられる**
  - `DescriptionColor` (L139-156)
  - `DescriptionFontSize` (L161-179)
  - `DescriptionFontFamily` (L181-193)
  - `DescriptionFontAttributes` (L195-207)
  - `HintText` (L212-229) — **全 Cell で受けられる**
  - `HintTextColor` (L234-251)
  - `HintFontSize` (L256-274)
  - `HintFontFamily` (L276-288)
  - `HintFontAttributes` (L290-302)
  - `BackgroundColor` (L307-324)
  - `IconSource` (L329-347) — **全 Cell で受けられる**
  - `IconSize` (L352-370)
  - `IconRadius` (L375-392)
  - `IsVisible` (L397-414) — Change 3 で扱う
  - `Height` (L416-427)
  - `IsEnabled` (L429-440)

- **各 Cell の追加プロパティ**（CellBase + 個別フィールド）
  - `Cells/LabelCell.cs` — `ValueText` (L13-32) + `ValueTextColor` (L34-54) + `ValueTextFontSize/Family/Attributes`
  - `Cells/CommandCell.cs` — `Command` / `CommandParameter` / `HideArrowIndicator`
  - `Cells/ButtonCell.cs` — `Command` / `CommandParameter` / `TitleAlignment`
  - `Cells/SwitchCell.cs` — `On` / `AccentColor`（**CellBase 由来の Description/Icon/Hint も使える**）
  - `Cells/CheckboxCell.cs` — `Checked` / `AccentColor`（同上）
  - `Cells/RadioCell.cs` — `Value` / `AccentColor`（同上、accentColor 移植漏れ）
  - `Cells/SimpleCheckCell.cs` — `Checked` / `Value` / `AccentColor`（同上）

### 2.1 Why

オリジナル `CellBase` は `Title` / `Description` / `HintText` / `IconSource` / `BackgroundColor` 等を **全 Cell の共通プロパティ** として提供していた。SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / ButtonCell も description / hint / icon を表示できた。

KsSettingsView の現状：
- ✅ `LabelCell`: title, description, valueText, icon, hintText
- ✅ `CommandCell`: title, description, valueText, icon, hintText, hideArrow
- 一部 `SwitchCell`: title, description のみ（icon / hintText / valueText なし）
- 一部 `CheckboxCell`: title, description のみ（icon / hintText / valueText なし）
- 最小 `RadioCell`: title のみ
- 最小 `SimpleCheckCell`: title のみ
- 最小 `ButtonCell`: title, titleColor のみ

これは「LabelCell ベース機能のみが横展開された結果の実装漏れ」と判断する。

加えて各 Cell の View / ViewHolder が **個別レイアウト** を持っているため、共通プロパティを横展開するには共通レイアウト基盤が必要。**コンポジションベース**（B 案）で共通行レイアウト関数を切り出し、各 Cell View は accessory slot のみ専用実装にする。

副次的に `RadioCell.accentColor` / `SimpleCheckCell.accentColor` の漏れも本 change で同時補完する（共通フィールド化の流れで自然に統合される）。

### 2.2 What Changes

#### 2.2.1 共通行レイアウトの切り出し

**iOS（SwiftUI / UIKit）:**

新規ファイル候補：
- `ios/Sources/KsSettingsViewUI/KsCellRowLayout.swift`
  - `func ksCellRow(title:..., description:..., valueText:..., icon:..., hintText:..., accessory: () -> some View) -> some View` 相当の SwiftUI View 関数
  - または UIKit 版で `UIView` を組み立てる Builder 関数

`KsListCellBase` の中で `contentConfiguration = UIHostingConfiguration { ksCellRow(...) { accessory } }` のように呼び出し、各 Cell View はモデルから値を取り出して accessory slot だけ自前で組む。

**Android（Compose）:**

新規ファイル候補：
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt`
  - `@Composable fun KsCellRow(title:..., description:..., valueText:..., icon:..., hintText:..., accessory: @Composable () -> Unit)` 相当

各 `CellViewHolder` の `ComposeView` 内で `KsCellRow(...) { accessory }` を呼び出す。

#### 2.2.2 全 Cell モデルへの共通フィールド追加

| Cell | 追加フィールド |
|------|---------------|
| `SwitchCell` | `valueText`, `icon`, `hintText` |
| `CheckboxCell` | `valueText`, `icon`, `hintText` |
| `RadioCell` | `description`, `valueText`, `icon`, `hintText`, `accentColor` |
| `SimpleCheckCell` | `description`, `valueText`, `icon`, `hintText`, `accentColor` |
| `ButtonCell` | `description`, `valueText`, `icon`, `hintText` |
| `LabelCell` | （既存維持） |
| `CommandCell` | （既存維持） |

全 Cell でフィールドは `Optional` / 既定 `nil` / `null`。既存呼び出しの破壊なし。

#### 2.2.3 各 Cell View の整理

| Cell View | accessory slot に置く内容 |
|-----------|--------------------------|
| `LabelCellView` | （accessory なし） |
| `CommandCellView` | chevron / disclosure indicator（hideArrow = false の場合のみ） |
| `SwitchCellView` | `UISwitch` / `Switch` |
| `CheckboxCellView` | `KsCheckBoxView` / `MaterialCheckBox` |
| `RadioCellView` | `KsCheckmarkAccessoryView` / radio 表示 |
| `SimpleCheckCellView` | `KsSimpleCheckView` 風表示 |
| `ButtonCellView` | （accessory なし、title だけ titleAlignment に従って配置） |

### 2.3 影響 spec / 影響モジュール

**MODIFIED specs:**
- `cell-types-basic` — 7 種 Cell の Requirement に「共通フィールド: description, valueText, icon, hintText」を追加。RadioCell / SimpleCheckCell に `accentColor` を追加。
- `settings-view-android-compose`（必要なら）— 共通行レイアウト規約
- `settings-view-ios-swiftui`（必要なら）— 共通行レイアウト規約

**影響モジュール:**
- iOS:
  - `ios/Sources/KsSettingsViewUI/KsCellRowLayout.swift`（新規）
  - `ios/Sources/KsSettingsViewUI/{LabelCell,CommandCell,SwitchCell,CheckboxCell,RadioCell,SimpleCheckCell,ButtonCell}.swift`（モデルにフィールド追加）
  - `ios/Sources/KsSettingsViewUI/{LabelCellView,CommandCellView,SwitchCellView,CheckboxCellView,RadioCellView,SimpleCheckCellView,ButtonCellView}.swift`（共通レイアウト関数を呼び出すよう書き換え）
- Android:
  - `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt`（新規）
  - 各 Cell.kt（モデルにフィールド追加）
  - 各 CellViewHolder.kt（共通レイアウト Composable を呼び出すよう書き換え）

### 2.4 Breaking changes

なし（追加のみ、既存呼び出しは引数省略で OK）。

### 2.5 Tasks（概要）

1. design.md で共通行レイアウト関数の I/F（引数リスト、accessory slot の渡し方）を確定
2. iOS `KsCellRowLayout.swift` 新規実装
3. iOS 全 Cell モデルに共通フィールド追加
4. iOS 全 Cell View を共通レイアウト関数に置き換え
5. iOS RadioCell / SimpleCheckCell に accentColor 追加
6. iOS テスト追加（全 Cell で description / icon / hintText が表示される、accentColor 反映、レイアウト崩れなし）
7. Android `KsCellRowLayout.kt` 新規実装
8. Android 全 Cell モデルに共通フィールド追加
9. Android 全 CellViewHolder を共通レイアウト関数に置き換え
10. Android RadioCell / SimpleCheckCell に accentColor 追加
11. Android テスト追加（全 Cell で description / icon / hintText が表示される、accentColor 反映）
12. samples に「Switch + icon + description + hintText」「Radio + accentColor」等のサンプルページ追加
13. cell-types-basic spec の MODIFIED 化（各 Cell Requirement に共通フィールドを追記）

### 2.6 完了基準

- 全 7 種 Cell が description / valueText / icon / hintText を受け取れる
- 受け取った場合に Label/Command と同じ視覚で表示される
- RadioCell / SimpleCheckCell の accentColor が Switch/Checkbox と同等に効く
- 共通行レイアウト関数が 1 箇所に集約されている（重複コードなし）
- `swift test` / `./gradlew :ks-settingsview-ui:test` がすべて成功

### 2.7 着手前チェック

- [ ] Change 1 が完了し、Theme / CellStyle の解決順序が確立していること
- [ ] 既存 in-progress change（`add-cell-types-input` / `add-cell-types-custom`）が Cell モデルに新フィールドを追加していないか確認（衝突回避）
- [ ] `KsListCellBase` の preferredLayoutAttributesFitting override を壊さないこと（cellHeight 反映が維持されること）
- [ ] `archive/refine-basic-cells-sample-layout` のレイアウト規約を再確認

## 3. Change 3: `add-visibility-flags-section-and-cell`

### 3.0 オリジナル参照（必読）

- **`AiForms.Maui.SettingsView/SettingsView/Section.cs`**
  - `IsVisible` (L193-210) — Section 単位の表示制御
- **`AiForms.Maui.SettingsView/SettingsView/Cells/CellBase.cs`**
  - `IsVisible` (L397-414) — Cell 単位の表示制御
- 移植元での挙動: `IsVisible = false` のとき、リスト内には保持したまま UI から非表示にする。再表示時は元の位置に復活する。

### 3.1 Why

オリジナルでは `Section.IsVisible` / `CellBase.IsVisible` で「リスト内に保持したまま表示だけ抑制」できた。条件付き表示の頻出パターン。

KsSettingsView は DSL / Store 駆動だが、それでも「リストから除外する」コードを書くより `isVisible = false` で隠す方が呼び出し側で簡潔。データソースに残しつつ UI から消す要件は実装系（フォーム状態に応じて段階表示など）でよくある。

### 3.2 What Changes

#### 3.2.1 Section.isVisible

`Section` ドメインモデル（core）に `isVisible: Bool`（既定 `true`）を追加。

```swift
public struct Section: Hashable, Identifiable, Sendable {
    public let id: UUID
    public let header: SectionAccessory?
    public let footer: SectionAccessory?
    public let cells: [any KsCell]
    public let headerHeight: Double
    public let isVisible: Bool  // NEW
    // ...
}
```

```kotlin
data class Section(
    val id: String,
    val header: SectionAccessory? = null,
    val footer: SectionAccessory? = null,
    val cells: List<Cell> = emptyList(),
    val headerHeight: Double = -1.0,
    val isVisible: Boolean = true,  // NEW
)
```

#### 3.2.2 Cell.isVisible

全 Cell モデルに `isVisible: Bool`（既定 `true`）を追加。

注: 既に全 Cell が `isEnabled: Bool` を持っているので、その隣に追加する。

#### 3.2.3 UI 層フィルタ実装

- iOS `KsSettingsViewController` の snapshot 構築時に `isVisible = false` の Section / Cell を除外
- Android `KsSettingsListAdapter` / `RootHeaderFooterAdapter` の `submitList` で同様に除外
- 構造同期（diff）の前処理として実装。`isVisible` が toggle されたら snapshot が再構築され、insert/remove として検出される

#### 3.2.4 DSL での受け取り

- iOS SwiftUI DSL: `Section("...", isVisible: condition) { ... }` / `LabelCell(..., isVisible: condition)`
- Android Compose DSL: `Section("...", isVisible = condition) { ... }` / `LabelCell(..., isVisible = condition)`

### 3.3 影響 spec / 影響モジュール

**MODIFIED specs:**
- `settings-view-core` — Section ドメインモデル Requirement に `isVisible` 追加。Cell 抽象 Requirement または cell-types-basic spec に Cell `isVisible` 追加。
- `cell-types-basic` — 全 Cell Requirement に「全 Cell 共通の isVisible」を追加（既存「全 Cell 共通の isEnabled」と同パターン）
- `settings-view-android-host` — フィルタ実装 Requirement
- `settings-view-ios-host` — フィルタ実装 Requirement

**影響モジュール:**
- Core:
  - `ios/Sources/KsSettingsViewCore/Section.swift`
  - Android `core/Section.kt`（場所要確認）
  - 全 Cell .swift / .kt
- UI:
  - `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`（snapshot フィルタ）
  - `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` / `KsSettingsListAdapter.kt`（フィルタ）

### 3.4 Breaking changes

なし（追加のみ、既存呼び出しは引数省略で OK）。

### 3.5 Tasks（概要）

1. design.md で「フィルタの場所」（snapshot 構築時 / Diff 適用前）を確定
2. Core Section に isVisible 追加（iOS / Android）
3. 全 Cell モデルに isVisible 追加（iOS / Android）
4. iOS snapshot フィルタ実装
5. Android adapter フィルタ実装
6. DSL 引数追加（iOS SwiftUI / Android Compose）
7. テスト追加：
   - Section.isVisible = false で当該セクション全体が非表示
   - Cell.isVisible = false で当該セルのみ非表示
   - isVisible toggle で構造同期（diff）が insert/remove として検出される
   - isVisible = false の Cell はタップ/イベントを受け取らない（描画されないので自然と発生しないが確認）
8. cell-types-basic spec の MODIFIED 化（全 Cell 共通の isVisible Requirement 追加）
9. settings-view-core spec の MODIFIED 化（Section.isVisible Requirement 追加）
10. samples に「条件付き非表示」のサンプル追加

### 3.6 完了基準

- Section.isVisible / Cell.isVisible が両方とも追加されている
- false にすると描画から除外される
- true ⇄ false の toggle で正しく insert/remove される
- `swift test` / `./gradlew :ks-settingsview-ui:test` がすべて成功

### 3.7 着手前チェック

- [ ] Change 2 が完了し、全 Cell が共通レイアウト経由で描画されていること
- [ ] 「表示状態同期の二層分離」Requirement（core spec）に違反しないこと（id 同一性は維持、内容変化は reconfigure）
- [ ] フィルタ後の index 計算が Diff 適用と整合すること（fold/unfold で index ズレ起こらない）

## 4. 廃止・不要（明示）

3 change 全体で **やらない** と確定した項目（draft 04 の指示反映）。オリジナル該当行も併記。

### SettingsView.DefineProperites.cs 由来
- `HeaderPadding` (L99-116) → カスタム View で対応
- `FooterPadding` (L397-414) → カスタム View で対応
- `HeaderTextVerticalAlign` (L193-210) → カスタム View で対応
- `ScrollToTop` (L868-885) → 別 API を後で検討
- `ScrollToBottom` (L846-863) → 別 API を後で検討
- `VisibleContentHeight` (L890-906) → 不要
- `ItemDropped` event (L14) → DragSort 後で刷新
- `ItemDroppedCommand` (L16-28) → DragSort 後で刷新
- `UseDescriptionAsValue` (L799-817) → 廃止
- `ShowSectionTopBottomBorder` (L823-841) → `KsSettingsViewStyle.Classic` で吸収済
- `ShowArrowIndicatorForAndroid` (L976-993) → `CommandCell.hideArrow` で吸収済
- `ItemsSource` (L911-928) / `ItemTemplate` (L933-949) / `TemplateStartIndex` (L954-971) → MAUI 専用、Native 不要

### Cells/LabelCell.cs 由来
- `IgnoreUseDescriptionAsValue` (L102-122) → 廃止

### Cells/CommandCell.cs 由来
- `KeepSelectedUntilBack` → 不要

### Section.cs 由来
- `FooterVisible` (L345-357) → カスタム View で対応
- `UseDragSort` (L362-379) → DragSort 後で刷新
- `MoveSourceItemWithoutNotify` (L50-73) / `MoveCellWithoutNotify` (L80-87) / `DeleteSourceItemWithoutNotify` (L89-112) / `InsertSourceItemWithoutNotify` (L114-132) / `DeleteCellWithoutNotify` (L134-141) / `InsertCellWithoutNotify` (L143-148) → MAUI 専用
- `SectionCollectionChanged` event (L176) → MAUI 専用
- `SectionPropertyChanged` event (L181) → MAUI 専用
- `CellPropertyChanged` event (L186) → MAUI 専用
- `ItemsSource` (L259-277) / `ItemTemplate` (L237-254) / `TemplateStartIndex` (L381-394) → MAUI 専用

### Cells/CellBase.cs 由来
- `Tapped` event (L12-17) → Native は onTap で OK
- `Reload()` (L463-478) → Store の Diff で代替済
- `Source` / `IsLoading` (L448-449) / `IsAnimationPlaying` (L451) / `UpdateIsLoading` (L453-456) / `SetEnabledAppearance` (L458-461) → MAUI 専用

## 5. 後続懸念（本計画外、別途レビュー要）

### 5.1 既存 in-progress change のレビュー

`add-cell-types-input` proposal にオリジナル入力系 Cell の固有プロパティが網羅されているか別途確認：

- `PickerCell`: `MaxSelectedNumber`, `UsePickToClose`, `UseNaturalSort`, `SubDisplayMember`, `SelectedItemsOrderKey`, `Padding`, `PageTitle`
- `TextPickerCell`: `IsCircularPicker`, `PickerTitle`
- `DatePickerCell`: `IsAndroidSpinnerStyle`, `AndroidButtonColor`, `TodayText`, `Format`
- `TimePickerCell`: `Format`, `PickerTitle`
- `EntryCell`: `PlaceholderColor`, `ShowDoneButtonOnIOS`, `TextAlignment`

### 5.2 `add-cell-types-custom` proposal レビュー

- `IsMeasureOnce` / `IsSelectable` / `LongCommand` / `ShowArrowIndicator` / `UseFullSize` が網羅されているか確認

### 5.3 `SimpleCheckCell.Value` の扱い

draft 04 の指示「PickerCell 実装の時に必要かもしれない」を踏まえ、本計画 3 本では追加せず、`add-cell-types-input` 側で扱う。

## 6. 計画変更時のメモ

- 計画修正は本ファイルを直接編集する（履歴は git log 参照）
- 各 change の proposal 作成時にこの計画の該当節を proposal.md / design.md にコピー＆深堀りする
- セッションをまたぐ場合は冒頭の「進行ステータス管理」チェックボックスを必ず更新する

## 7. セッション再開時のクイックスタート

新しいセッションで再開する場合：

1. `openspec list --json` で現在の change 状況を確認
2. 本ファイル先頭の「進行ステータス管理」で未完了 change を特定
3. 該当 change が proposal 未作成なら `/opsx:propose` で着手
4. proposal は作成済みなら `/opsx:continue` または `/opsx:apply` で続行
5. 並行作業の禁止：1 本完了するまで次の change に着手しない

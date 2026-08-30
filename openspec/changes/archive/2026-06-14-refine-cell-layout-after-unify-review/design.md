## Context

### 経緯

直前 change `unify-cell-common-fields-via-shared-row-layout`（以下「unify change」）は、`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` / `ButtonCell` への共通フィールド (`description` / `valueText` / `icon` / `hintText`) 横展開と、共通行レイアウト関数 `applyCellBaseLayout(...)` の導入を完了させた（`completedTasks=107/113`, `status=in-progress`）。実装完了後のオーナーによる実機 / シミュレータレビュー（2026-06-13 19:45 撮影スクリーンショット）で、以下の見た目側の問題が明らかになった:

1. **iOS**: hintText が `Switch` / `Checkbox` / `Radio` / `SimpleCheck` のいずれの Cell でも accessory の左にずれて表示される。`ButtonCell`（accessory なし）のみ正しい右上 float 配置になっている。
2. **Android**: サンプル `UnifyCellCommonFieldsDemoScreen` で `description` が一切表示されない（cell 高さ不足で切り捨て）。
3. **Android**: 左端アイコンと title の間の余白が iOS と比べて狭い。
4. **Android**: `valueText`（「オン」「省データ」「新規」「送信」等）の縦位置が cell 縦中央でなく、本体行が上端寄りに描画される。
5. **Android**: hintText が valueText / accessory と重なっているように見えるが、これは cell 高さ不足の副作用であり cell 高さが伸びれば自然に解消する（実体は ②）。
6. **Android サンプル**: `android.R.drawable.ic_dialog_info` / `btn_star` / `ic_dialog_email` 等の汎用 Android 標準アイコンを使用しており、デモとして見栄えが悪い。
7. **Android サンプル**: `RadioCell` に `hintText` が指定されていないため、Radio + hintText の組み合わせが視覚的に確認できない（仕様上は持てる）。

### オリジナル AiForms.Maui.SettingsView の実装

オーナーから指示があったため、`../AiForms.Maui.SettingsView/` を再調査した結果は以下:

**iOS 行高さ** (`SettingsView/Native/iOS/AiTableView.cs` / `SettingsTableSource.cs` / `Cells/CellBaseView.cs`):
- `AiTableView.MinRowHeight = 48`（クラス定数）
- `RowHeight = UITableView.AutomaticDimension`（auto 高さがデフォルト）
- `CellBaseView.UpdateMinRowHeight`: `_minheightConstraint = StackH.HeightAnchor.ConstraintGreaterThanOrEqualTo(CellParent.RowHeight)`（最低高さ制約）
- `GetHeightForRow`: `Math.Max(CellParent?.RowHeight ?? 44, MinRowHeight=48)` で下限保証

**Android 行高さ** (`SettingsView/Native/Android/AiRecyclerView.cs`):
```csharp
internal void UpdateRowHeight()
{
    if (_settingsView.RowHeight == -1)
    {
        _settingsView.RowHeight = 60;
    }
    ...
}
```
RowHeight 未指定なら `60` を自動セットしつつ、`cellbaseview.axml` レイアウトでは MinHeight 扱い。

**iOS hintLabel** (`SettingsView/Native/iOS/Cells/CellBaseView.cs`):
- `_HintLabel` を `UITableViewCell` 直下に `AddSubview`
- 制約: `TopAnchor.ConstraintEqualTo(this.TopAnchor, 2)`, `RightAnchor.ConstraintEqualTo(this.RightAnchor, -10)` ← **`this` = `UITableViewCell` 自身**
- contentView ではなく cell 自身の RightAnchor を基準にしている点が肝

つまりオリジナルは:
- 「Auto 高さ + 下限保証」がデフォルト挙動
- hintLabel は cell 右端基準で float 配置（accessory に影響されない）

### 現状の KsSettingsView 実装との乖離

| 観点 | オリジナル AiForms | 現状 KsSettingsView |
|------|-------------------|---------------------|
| iOS hintLabel trailing 基準 | `cell.RightAnchor` | `cell.contentView.trailingAnchor` ❌ |
| Theme.hasUnevenRows デフォルト | （実質）Auto 高さ | `false`（厳密固定） ❌ |
| Android RowHeight 未指定時 | 60 を自動セット | `MIN_ROW_HEIGHT_DP=44` 下限 |
| Android アイコン余白 | (axml では十分) | iconMarginEnd=8dp ❌ |
| Android title 縦配置 | (RelativeLayout で CenterVertical) | TOP=parent.TOP 上端固定 ❌ |

### 制約

- 公開 API シグネチャを破壊しない（`Theme` 等の data class フィールド型・順序を変更しない）。
- `unify change` の I/F 規約（`applyCellBaseLayout` のシグネチャ / `CellBaseViews` のフィールド構成）は維持する。
- 既存 in-progress change の `add-cell-types-input` / `add-cell-types-custom` / `add-maui-*` には触れない（衝突確認済み）。
- 本 change の delta spec の MODIFIED 元 Requirement は、`unify change` で delta として書かれたものを含む。すなわち `unify change` を **先に archive** してから本 change を適用するワークフローを前提とする（後述 Migration Plan）。

### ステークホルダー

- ライブラリ利用者: AiForms.Maui.SettingsView から移行するユーザーは「指定なしなら Auto 高さ」を期待。
- ライブラリ開発者: 共通行レイアウトの最終調整（見た目品質）はここで完了させ、後続 change（`add-cell-types-input` / `add-cell-types-custom`）は同じ基盤の上で安心して追加できる状態にする。

## Goals / Non-Goals

**Goals:**

- iOS の `hintLabel` 配置を accessory に影響されない右上 float に修正し、オリジナル AiForms iOS と完全に一致させる。
- 行高さセマンティクスを「Auto 高さ + 下限保証」既定に揃え、オリジナル AiForms（iOS / Android）と整合させる。
- `Theme.hasUnevenRows` 機能自体は維持し、「全 cell をピッタリ揃えたい」ユースケースをオプションとして残す。
- Android の本体行（title / description / valueText / accessory）を縦バランス良く配置する。
- Android サンプルのアイコンを Material Symbols Outlined に置き換え、最低限の見栄えを確保する。
- Android サンプルの `RadioCell` で hintText の組み合わせをデモする。
- `unify change` を破棄せず、archive → 本 change apply の順で連続的に進める。

**Non-Goals:**

- 新規 Cell 種別の追加（`add-cell-types-input` / `add-cell-types-custom` 側で別途）。
- 共通行レイアウト関数の I/F 変更（シグネチャは維持）。
- `KsImage` の Material Symbols 専用派生の追加（vector drawable 化のみで対応、`KsImage.Resource` でそのまま使う）。
- iOS サンプルへの追加修正（iOS は HintText 位置の修正だけでオーナー OK 範囲）。
- AiForms オリジナルの `Cell.Height`（個別 Cell 高さ override）等の追加移植（既に `CellStyle.cellHeight` で対応済み）。

## Decisions

### Decision 1: iOS hintLabel の trailing 制約を `cell.trailingAnchor` 基準に変更

**選択**: `KsListCellBase.ensureHintLabel()` 内の AutoLayout 制約を

```swift
// 変更前
label.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor, constant: -10),

// 変更後
label.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -10),
```

に修正する。`self` は `UICollectionViewListCell` 派生（cell 自体）であり、`self.trailingAnchor` は accessory の有無に関わらず cell の右端を指す。

`unify change` の delta spec `settings-view-ios-swiftui` の "共通行レイアウト関数 applyCellBaseLayout" Requirement 内、hintLabel 制約条項（`hintLabel.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor, constant: -10)`）を MODIFIED し、`cell.trailingAnchor` 基準に書き換える。

**理由**:
- オリジナル `AiForms.Maui.SettingsView/SettingsView/Native/iOS/Cells/CellBaseView.cs` の `_HintLabel.RightAnchor.ConstraintEqualTo(this.RightAnchor, -10)` と完全一致（`this` = `UITableViewCell`）。
- `UICollectionViewListCell` の `contentView` は accessory がある時に accessory 領域の左端で終わるため、`contentView.trailingAnchor` を基準にすると hintLabel が accessory の左に押し込まれる（実機で確認済み）。
- `cell.trailingAnchor` 基準ならば accessory のレイアウト変化に関係なく cell 右端基準で固定される。

**代替案**:
- A 案（hintLabel を accessory として `UICellAccessory` 化）: `unify change` でも検討して却下した経緯あり。accessory は本体行の中央軸で配置されるため右上 float が実現できない。再却下。
- B 案（contentView の trailing margin を 0 にする）: 各 Cell View で `contentConfiguration` を組む際の trailing inset を 0 化する案。テキスト本体行が画面右端まで伸びてしまうため、別の見た目崩れを招く。却下。

### Decision 2: 行高さセマンティクスを「Auto 高さ + 下限保証」既定に変更（hasUnevenRows デフォルト true）

**選択**:
- `Theme.hasUnevenRows` のデフォルト値を **`false` → `true`** に変更（iOS `Theme` struct および Android `Theme` data class 双方）。
- `hasUnevenRows` プロパティ自体は維持し、「明示的に `false` を指定したときのみ全行を厳密固定」する従来の動作を残す。
- 実装側のロジック（iOS `KsListCellBase.preferredLayoutAttributesFitting` / Android `applyEffectiveHeight`）は **両モードに既に対応済み**であり、デフォルト変更だけで挙動が変わる。

具体的な MODIFIED 対象:

- **spec `settings-view-ios-style`**:
  - 「Theme フィールド規約」Requirement: `hasUnevenRows: Bool` のデフォルト値記述を `false` → `true` に。
  - 「Theme のデフォルト Scenario」: `rowHeight = -1`、`hasUnevenRows = false` → `rowHeight = -1`、`hasUnevenRows = true`。
  - 「rowHeight / hasUnevenRows の既定組み合わせ Scenario」: 期待値を `(-1, true)` に。THEN の「固定高さ・最低高さ MinRowHeight」解釈の文言を「Auto 高さ・最低高さ MinRowHeight」に書き換え。
- **spec `settings-view-android-style`**: 同様の MODIFIED。
- **spec `cell-types-basic`**: 既存 Requirement 内に `Theme(hasUnevenRows = ...)` のデフォルト期待を書いている Scenario があれば追記または明示しない（既存 Requirement の文言を確認したところ、`unify change` で MODIFIED された "全 Cell 共通の Theme.titleColor / Theme.titleFont 反映" Requirement には行高さ既定の記述はないため、追加 MODIFIED は最小限）。

**理由**:
- オリジナル AiForms 実装と一致（iOS: `UITableView.AutomaticDimension` + `MinRowHeight=48`、Android: `RowHeight=-1` で `60` 自動セット + MinHeight 扱い）。
- ライブラリ利用者が「Theme を指定しなければ `description` が自然に伸びる」挙動を期待するのは自然。
- 現状デフォルトの「固定 44/48 dp」は、`description` を持つ Cell が増える本ライブラリの方向性と矛盾している。
- `hasUnevenRows = false` を残すことで「全 cell を等高で揃えたい」ユーザー指定を尊重できる（ユーザー要望）。

**代替案**:
- A 案（`hasUnevenRows` プロパティ自体を削除し常時 Auto 高さ）: シンプルだが「全行を揃えたい」ユースケースを完全に切り捨てるため不可。却下。
- B 案（デフォルトは現状維持で、サンプルだけ `hasUnevenRows = true` を明示）: 一時しのぎ。利用者全員が同じ罠を踏むため不可。却下。
- C 案（行高さセマンティクスを完全に hasUnevenRows 廃止 + 常時 WRAP に統一）: 上記 A と同様、機能後退になる。却下。

### Decision 3: Android iconView の右マージンを 8dp → 16dp に拡大

**選択**: `CellBaseLayout.kt` の `iconMarginEnd` 定数を `8dp` から `16dp` に変更する。

**理由**:
- iOS の `UIListContentConfiguration.cell()` / `subtitleCell()` の `directionalLayoutMargins` におけるアイコン右余白は標準で 11〜16pt 程度（iOS 16+ 実測）。
- 16dp は Material Design のリスト項目の標準 padding と整合する（`?attr/listPreferredItemPaddingStart` 等）。
- これはレイアウト規約の MUST レベルの変更ではなく、視覚密度の調整。spec の `ConstraintLayout 配置規約` Requirement では「iconMarginEnd の具体値」は規定されていない（密度依存）。よって spec 変更不要、実装定数のみ。

**代替案**:
- A 案（12dp に拡大）: 中庸だが、iOS との視覚的一致度がやや劣る。却下。
- B 案（24dp に拡大）: 大きすぎてセル内のテキスト幅が狭くなる。却下。

### Decision 4: Android 本体行（title / description）を vertical chain（packed, bias 0.5）配置に変更

**選択**: `buildCellBaseViews` の `ConstraintSet` を以下のように修正:

```kotlin
// titleView: TOP=parent.TOP + BOTTOM=descriptionView.TOP（chain head）
set.connect(titleView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
set.connect(titleView.id, ConstraintSet.BOTTOM, descriptionView.id, ConstraintSet.TOP)
// descriptionView: TOP=titleView.BOTTOM + BOTTOM=parent.BOTTOM（chain tail）
set.connect(descriptionView.id, ConstraintSet.TOP, titleView.id, ConstraintSet.BOTTOM)
set.connect(descriptionView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
// chain style: PACKED, bias 0.5（縦中央寄せ）
set.setVerticalChainStyle(titleView.id, ConstraintSet.CHAIN_PACKED)
set.setVerticalBias(titleView.id, 0.5f)
```

`valueTextView` は引き続き `BASELINE = titleView.BASELINE` で title 行のベースラインに紐付ける（現状維持）。

`description == null`（GONE）のケースでは、ConstraintLayout は GONE View をスペース 0 として扱うため、titleView 単独で縦中央寄せになる（chain head が GONE chain member を含んでも適切に挙動する）。

**理由**:
- title + description のセットが cell 縦中央寄せになり、accessory（縦中央配置）と整合する。
- valueText は title.BASELINE 紐付けのまま → cell 縦中央付近に位置する → iOS と同じ視覚的配置になる。
- 既存の `accessoryHolder` の `Top=parent.Top, Bottom=parent.Bottom`（CenterVertical）配置と組み合わせて自然なバランスが取れる。

**代替案**:
- A 案（title を CenterVertical（TOP=parent.TOP + BOTTOM=parent.BOTTOM）+ description を title の下に置く）: title が中央に来てしまい description が下にはみ出る。却下。
- B 案（LinearLayout を入れて title + description を縦並べに）: ConstraintLayout 内に LinearLayout を入れるとレイアウトコストが増える。chain で十分。却下。

### Decision 5: サンプルアイコンを Material Symbols Outlined の vector drawable に置き換え

**選択**:
- Google Fonts Material Symbols Outlined（フリーライセンス Apache 2.0）の SVG を、Android Studio の Vector Asset 経由で `samples/android/app/src/main/res/drawable/ic_*.xml` として 10〜15 個同梱する。
- ファイル命名規則: `ic_<material_symbols_name>.xml`（例: `ic_notifications.xml`）。
- 既存サンプルコードの `KsImage.Resource(android.R.drawable.*)` を `KsImage.Resource(R.drawable.ic_*)` に置換する。
- 対象: `UnifyCellCommonFieldsDemoScreen.kt` および `BasicCellsDemoScreen.kt`（残課題 1）。

iOS SF Symbols との意味的マッピング（実装時に確定）:

| iOS SF Symbols | Material Symbols Outlined |
|----------------|--------------------------|
| `bell` | `notifications` |
| `wifi` | `wifi` |
| `doc.text` | `description` |
| `sun.max` | `light_mode` |
| `moon` | `dark_mode` |
| `circle.lefthalf.filled` | `brightness_auto` |
| `envelope` | `email` |
| `calendar` | `calendar_today` |
| `paperplane` | `send` |
| `power` | `logout` |
| `person.crop.circle` | `account_circle` |
| `gear` / `gearshape` | `settings` |
| `lock` | `lock` |
| `bell.slash` | `notifications_off` |

**理由**:
- `androidx.compose.material:material-icons-extended` 依存追加と比較して、サンプル apk サイズの肥大化を避けられる（必要な 10〜15 個のみ同梱）。
- vector drawable のためサイズ可変、tint 適用可能。
- ライセンスが Apache 2.0 で同梱しやすい（同様の vector drawable をライブラリ本体ではなく **サンプル apk** にのみ同梱するため、本体ライブラリのライセンスには影響しない）。

**代替案**:
- A 案（`material-icons-extended` Compose 依存を追加）: 簡単だがサンプル apk サイズが +5MB 程度増える。却下。
- B 案（Material Symbols Outlined.ttf をフォントとして読み込み、IconFont 方式で表示）: 既存の `iconView: ImageView` ベース実装と相性が悪く、`CellBaseViews` の再設計を伴う。本 change のスコープ外。却下。

### Decision 6: RadioCell サンプルに hintText を追加

**選択**: `UnifyCellCommonFieldsDemoScreen` の `RadioCell("ダーク")` に `hintText = "推奨"` を追加し、Radio + hintText の組み合わせをデモする。

**理由**:
- Radio が hintText を持てることが視覚的に伝わる（unify change の効用がデモで確認できる）。
- 元々サンプルから漏れていた理由は「サンプル作成時のデモバリエーション選定漏れ」であり、技術的な制約はない。

**代替案**:
- A 案（`SimpleCheckCell` の hintText が既にあるので Radio は据え置き）: 残課題 3 の指摘どおり、Radio + hintText を明示的にデモする意義がある。却下。

### Decision 7: `unify change` のリクエスト順序を「unify archive → 本 change apply → 本 change archive」とする

**選択**: 本 change の delta spec の MODIFIED 元 Requirement は、`unify change` で delta として書かれたものを含む。OpenSpec の規約上、MODIFIED は「main spec の Requirement の現行内容」を対象とするため、`unify change` を先に archive する必要がある。

**ワークフロー**:
1. `unify change` の残タスク（completedTasks=107/113 の 6 件）を完了させる。
2. `unify change` の `verify` → `archive` を実行し、`unify change` の delta を main spec に sync する。
3. 本 change `refine-cell-layout-after-unify-review` を `apply` する（このタイミングで main spec に MODIFIED 適用）。
4. 本 change を `verify` → `archive` する。

**理由**:
- OpenSpec ワークフローの素直な順序。
- 本 change を `unify change` と同時並行で進めると、両者の delta が重複してマージ時にコンフリクトする。

**代替案**:
- A 案（本 change を `unify change` にマージして 1 つの change にする）: `unify change` は既に大規模（113 タスク）であり、本 change を吸収するとレビュー粒度が荒くなる。また `unify change` の archive 後に判明したオーナーレビュー指摘という本 change の経緯が見えなくなる。却下。
- B 案（main spec を直接 MODIFIED するのではなく `unify change` の delta を直接書き換える）: OpenSpec 規約違反（archive 前の change の delta を別の change が触る）。却下。

## Risks / Trade-offs

### [Risk] `Theme.hasUnevenRows` デフォルト変更で既存サンプルアプリの見た目が変わる

→ **Mitigation**: 本ライブラリは v0.x（pre-1.0）であり、利用者が極めて限られる。デフォルト変更による視覚的変化はオリジナル AiForms 踏襲方向の修正であり、サンプルアプリ側でも `BasicCellsDemoScreen` / `UnifyCellCommonFieldsDemoScreen` のスクリーンショットで確認することで影響範囲を可視化する。「全行を揃えたい」既存ユーザーには `Theme(hasUnevenRows = false)` を明示指定する移行パスを README に追記する。

### [Risk] iOS `cell.trailingAnchor` 基準に変えると、cell の右端余白（separator inset 等）と干渉して hintLabel が画面端ギリギリになる

→ **Mitigation**: `UICollectionViewListCell` の右端 = cell の trailing。`constant: -10` で 10pt の右マージンを保つ。`separatorInset` は contentView 内の罫線位置を決めるだけで、cell 自身のサイズには影響しない。実機・シミュレータで `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` を視覚確認する（マニュアル）。

### [Risk] Android vertical chain 配置で description が GONE のとき titleView の縦位置が想定と異なる

→ **Mitigation**: ConstraintLayout の chain は GONE chain member をスペース 0 として扱うが、`titleView.BOTTOM = descriptionView.TOP` の連結は維持される。`titleView` の packed bias 0.5 により、`descriptionView` が GONE でも titleView 単独で縦中央寄せになる。`Robolectric` / `androidx.test.ext` でレイアウト後の `titleView.top` 座標を測定し、`root.centerY` 付近にあることを assert するテストを追加する。

### [Risk] Material Symbols vector drawable の追加でサンプル apk サイズが増える

→ **Mitigation**: 1 個あたり ~1KB の vector drawable を 10〜15 個追加 → 合計 ~15KB 程度。`material-icons-extended` Compose 依存 (~5MB) に比べて十分軽量。受容。

### [Risk] `unify change` の archive が完了していない状態で本 change を作ると、`openspec verify` がコンフリクトを警告する

→ **Mitigation**: design.md（Decision 7）に「unify archive → 本 change apply」の順序を明記。`tasks.md` の Phase 0 に「unify change の archive 完了確認」をチェックリスト項目として入れる。実装フェーズの最初に明示的に確認する。

### [Risk] 本 change の MODIFIED 対象 Requirement が `unify change` の delta spec にしか存在しないため、本 change の `openspec verify` が「元 Requirement が見つからない」と警告する可能性

→ **Mitigation**: 本 change は **`unify change` の archive 完了後に apply** することを前提とする（Decision 7）。`unify change` archive 後は MODIFIED 対象 Requirement が main spec に sync されているため verify は通る。万一順序が乱れた場合は、本 change を re-create するか、tasks.md に「再 verify 前に unify archive」を明示する。

### [Trade-off] Android アイコン余白拡大により cell 内のテキスト表示幅が 8dp 狭くなる

→ 受容。iOS との視覚的一致を優先。長文 title は元々 `BASELINE` 紐付けで省略表示（`ellipsize`）になるため、表示幅 8dp 減少の影響は限定的。

### [Trade-off] サンプルのアイコン更新は Android のみ（iOS は SF Symbols をそのまま使う）

→ 受容。iOS はオーナーレビューで「概ね良好」と評価されており、SF Symbols の表示は既に意味的に正しい。

## Migration Plan

### 適用順序

1. **`unify change` を archive 完了させる**:
   - `openspec apply unify-cell-common-fields-via-shared-row-layout`（残タスク 6 件を完了）
   - `openspec verify unify-cell-common-fields-via-shared-row-layout`
   - `openspec archive unify-cell-common-fields-via-shared-row-layout`
   - これにより main spec に unify change の delta が sync される。

2. **本 change `refine-cell-layout-after-unify-review` を apply する**:
   - `openspec apply refine-cell-layout-after-unify-review`
   - 各 Phase を順次実装（tasks.md 参照）

3. **本 change の verify → archive**:
   - `openspec verify refine-cell-layout-after-unify-review`
   - `openspec archive refine-cell-layout-after-unify-review`

### ロールバック戦略

- iOS hintLabel 制約変更: 単一行の修正のため、`git revert` で容易に戻せる。
- `Theme.hasUnevenRows` デフォルト変更: data class / struct の引数デフォルト値の 1 行変更。`git revert` 可能。
- Android `iconMarginEnd` / vertical chain: 局所的な ConstraintSet 修正のため、`git revert` 可能。
- サンプルアイコン置換: drawable 追加 + 参照 1〜2 ファイル変更。`git revert` 可能。

ロールバックの単位粒度は Phase 単位（tasks.md 参照）で commit を分けることで、Phase 単位の部分 revert も可能にする。

## Open Questions

- **Q1**: iOS の `hintLabel.trailingAnchor` を `cell.trailingAnchor` 基準に変更したとき、`UICollectionView` の `separatorInset` や `directionalLayoutMargins` との相互作用で想定外の余白が発生しないか？
  → 実装フェーズで実機確認。問題があれば `constant` 値を調整するか、`directionalLayoutMargins` を明示する。

- **Q2**: Material Symbols Outlined の vector drawable に statefully tint をかける必要があるか（disabled 時の灰色化等）？
  → 現状 `iconView` には `EffectiveStyle.iconTint` が適用されていないため、本 change のスコープ外として **Q として残す**（後続 change で必要なら対応）。

- **Q3**: Android 側で `Theme.rowHeight = -1`、`hasUnevenRows = true` のとき、minimum height は `MIN_ROW_HEIGHT_DP = 44dp` のままで良いか、オリジナル AiForms の Android の `60` に揃えるか？
  → オリジナル AiForms Android は `RowHeight = -1` → `60` 自動セットだが、これは「未指定なら最低 60」という意味。本 change では `MIN_ROW_HEIGHT_DP = 44`（マテリアルガイドラインの 48dp 近似）を踏襲し、`Theme.rowHeight` が明示指定された場合のみその値を下限とする（現状ロジック維持）。**確定**: 既存の `MIN_ROW_HEIGHT_DP = 44` を変更しない（オリジナル AiForms と一致させると今度は Material Guidelines から外れる）。

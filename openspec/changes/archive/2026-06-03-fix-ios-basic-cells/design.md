# Design: fix-ios-basic-cells

## Context

`add-cell-types-basic` で追加した iOS 基本 Cell 群の実機レビューで、オリジナル `AiForms.Maui.SettingsView` との 4 つの乖離が判明した。本 design では各不具合の根本原因と是正方針を整理する。すべて iOS（`KsSettingsViewUI`）に閉じた変更で、Cell の公開構造・DSL・Diff 経路には手を入れない。

## Decision 1: classic スタイルのヘッダー固定・空フッター帯を解消する

### 根本原因

`KsSettingsViewController.makeLayout(for:)` は次の構成になっている。

```
listConfig = UICollectionLayoutListConfiguration(appearance: .plain)  // classic
listConfig.headerMode = .supplementary
listConfig.footerMode = .supplementary
```

- `.plain` Appearance では、section header の supplementary は **デフォルトでスクロール上端に pin（固定）** される。これが「ヘッダーがスクロール固定される」症状。
- `footerMode = .supplementary` を全 section に一律付与しているため、**footer を持たない section にも空のフッター supplementary 領域が確保され**、グレーの帯として見える。これが「謎のフローティングフッター」症状。

### 是正方針

classic スタイルの Appearance（`.plain`）自体は維持する（spec の既存 Scenario「classic スタイルの Appearance = `.plain`」と整合させるため）。その上で:

1. **ヘッダー非固定化**: `NSCollectionLayoutSection.list(using:layoutEnvironment:)` で生成した section に対し、生成済み section の `boundarySupplementaryItems` のうち header 種別の `pinToVisibleBounds` を `false` に設定する。`.list(using:)` が内部生成する header supplementary item は既定で pin される場合があるため、明示的に非固定へ上書きする。

2. **空フッター帯の排除**: `footerMode` を一律 `.supplementary` にせず、「いずれかの section が footer を持つか」で決定する。
   - root 内のどの section も footer を持たない場合は `footerMode = .none` とし、フッター領域自体を生成しない。
   - footer を持つ section が 1 つでもある場合は `footerMode = .supplementary` を維持する（supplementaryProvider 側は footer を持たない section には空 view ではなく既定の高さ最小化で対応。現行の `sectionAccessoryView` は accessory が nil のとき空テキストの list cell を返すため、footer を持つ section だけが意味のある高さを取る）。

   header についても同様に「いずれかの section が header を持つか」で `headerMode` を決定してよいが、本提案のスクリーンショットでは全 section が header を持つため、最小修正としては footer 側の出し分けとヘッダー非固定を優先する。

3. **レイアウト再構築トリガ**: footer の有無は root の内容に依存するため、`makeLayout` は呼び出し時点の `root.sections` を参照して `footerMode` を決める。`applyFullSnapshot` などで section 構成が変わり footer の有無が変化した場合は、必要に応じてレイアウト再構築（`rebuildLayout` 相当）を行う。ただし頻繁な再構築を避けるため、初期構築時と style 変更時の判定を基本とし、動的な section 追加で footer 有無が変わるケースは許容範囲として扱う（実装タスクで詳細を確定）。

### 代替案

- **デモ側を `.modern`（insetGrouped）に変更**: insetGrouped はヘッダー非固定なので症状は消えるが、オリジナルの classic な見た目（フラット）とは異なる角丸グルーピングになる。製品方針（オリジナル互換）に反するため不採用。
- **classic Appearance を `.grouped` に変更**: `.grouped` はヘッダー非固定だが、空フッターの帯やセクション間余白がオリジナルと異なるため、`.plain` 維持＋個別調整を選択。

## Decision 2: CheckboxCell を UIView ベースのカスタムチェックボックスにする

### オリジナル仕様（CheckboxCellView.cs）

`CheckBox : UIButton` を accessory に置く。
- 20x20、`BorderWidth = 2`、`CornerRadius = 3`、`Inset` でタップ領域拡張。
- `Draw` で: Selected 時は `Layer.BackgroundColor = FillColor`（accent）で塗りつぶし、`UIBezierPath` で白いチェックマークを描画。非 Selected 時は背景透明（枠のみ）。
- accent カラーは `BorderColor` と `FillColor` に適用。

### 是正方針

`KsCheckBoxView: UIView`（または `UIControl`）を新設し、`CheckboxCellView` の `customView` accessory として右端に配置する。

- 20x20、`layer.cornerRadius = 3`、`layer.borderWidth = 2`。
- `isChecked` プロパティで状態保持。`draw(_:)` で:
  - checked: `backgroundColor`（layer）= accent、`UIBezierPath` でオリジナルと同じ座標比（22/52 → 38/68 → 76/30）の白いチェックマークをストローク。
  - unchecked: 背景透明、枠のみ。
- accent カラー: `CheckboxCell.accentColor` ?? `Theme.cellAccentColor`。border / fill に適用。
- accessory 配置: `UICellAccessory.customView(configuration:)` で `placement: .trailing` に固定。チェック状態に関わらず accessory は常設し、UIView 内部の `isChecked` 切替で再描画する（accessory 追加/削除によるスライドアニメーションを避ける = ③ と同じ思想）。
- タップ通知は従来どおり `tapHandler`（Cell 全体タップ）経由。チェックボックス UIView 自体は表示専用でよい。

## Decision 3: RadioCell の On→Off を alpha フェードにする

### 根本原因

```
if isSelected { self.accessories = [.checkmark()] }
else          { self.accessories = [] }
```

accessory の追加/削除は UIKit デフォルトで slide アニメーションを伴うため、On→Off で checkmark が右にスライドして消える。

### 是正方針

checkmark を**常設の `customView` accessory**（`UIImageView(systemName: "checkmark")` を accent 着色）にし、選択状態は `customView.alpha` で表現する。

- accessory は常に `[checkmarkCustomView accessory]` を保持（追加/削除しない）。
- `isSelected == true` → `alpha = 1`、`false` → `alpha = 0`。
- 状態変化時は `UIView.animate` で alpha をフェード（位置は不動）。`reconfigureItems` 経由の `render` 呼び出し時にフェードさせる。
- accent カラー: `RadioCell` も `CellStyle.accentColor` / `Theme.cellAccentColor` を尊重（既存挙動に合わせる）。

> 注: `render` は reuse / reconfigure の両方で呼ばれる。初期表示（reuse 直後）でフェードが走ると一瞬チラつくため、「初回 bind は即時 alpha 設定」「同一セルの状態変化時のみ animate」を区別する。実装タスクで `prepareForReuse` フラグ等により制御する。

## Decision 4: SimpleCheckCell を右端 checkmark + フェードにする

### オリジナル仕様（SimpleCheckCellView.cs）

`Accessory = Checked ? UITableViewCellAccessory.Checkmark : None`（**右端**チェック）。TintColor を accent に。レイアウトは標準セル（タイトル左、チェック右）で RadioCell と同形。

### 是正方針

現行の `content.image`（左側チェック）を廃止し、RadioCell（Decision 3）と同じ右端 checkmark customView accessory + alpha フェード方式に揃える。

- `isChecked == true` → checkmark alpha 1、`false` → alpha 0。
- accent カラー適用、フェード制御は RadioCell と共通化できるなら共通ヘルパに切り出す（実装判断）。

### 仕様整合

`cell-types-basic` の `SimpleCheckCell` Requirement は現状「タイトルの左側にチェックマーク（小）」と規定している。本変更で右端チェックに変えるため、Requirement 本文と Scenario を「右端チェック（RadioCell と同レイアウト）」へ MODIFIED で更新する。`CheckboxCell` との差異（CheckboxCell = 角丸ボックス、SimpleCheckCell = シンプルな checkmark）は維持する。

## Risks / Open Questions

- **footer 出し分けとレイアウト再構築の頻度**: 動的に section を追加して footer 有無が変わるケースでレイアウト再構築をどこまで追従させるか。初期構築と style 変更を基本とし、過剰な再構築は避ける（Decision 1-3）。
- **フェードのチラつき**: 初回 bind と状態変化を区別しないと、スクロールでセル再利用のたびにフェードが走る。`prepareForReuse` での状態リセットと「即時設定 vs animate」の判定を慎重に実装する（Decision 3 注記）。
- **カスタムチェックボックスのダークモード対応**: 枠線・チェックマーク色が固定値だと dark mode で視認性が落ちる可能性。border は accent、未チェック枠はセパレータ相当のシステムカラーを使うなど、Theme と整合させる。

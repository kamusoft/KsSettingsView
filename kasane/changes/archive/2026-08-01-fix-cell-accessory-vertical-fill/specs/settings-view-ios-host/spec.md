# settings-view-ios-host Specification 差分 (fix-cell-accessory-vertical-fill)

## MODIFIED Requirements

### Requirement: KsListCellBase の自前 UIStackView 階層

iOS の `KsListCellBase` (`UICollectionViewListCell` 派生) は、`init(frame:)` で 1 度だけ、AiForms.Maui.SettingsView オリジナル `CellBaseView.cs` 準拠の自前 `UIStackView` 階層を `contentView` 直下に install しなければならない (MUST)。後段の `applyCellBaseLayout(...)` は本階層を更新する形でのみ Cell の見た目を構成する。

階層構造は以下を MUST とする (spacing・margin 等の視覚パラメータは本 spec の対象外 — `ui/mock/` と AiForms 原典参照が正):

```
contentView
  └─ stackH (horizontal)
       ├─ iconImageView (image nil 時は isHidden=true)
       ├─ stackV (vertical — 残り領域を吸って広がる)
       │    ├─ contentStack (horizontal)
       │    │    └─ titleLabel
       │    │    [render 時に行内 trailing (valueLabel / trailingViews) が addArrangedSubview される]
       │    └─ descriptionLabel (空文字列のとき isHidden=true)
       └─ accessoryHolder (Cell 級アクセサリ用コンテナ — 内容の自然幅を保ち伸縮しない。
                           空のとき isHidden=true)
```

- `accessoryHolder` は AiForms オリジナルの `UITableViewCell.AccessoryView` / `Accessory` に相当する領域である。stackH の垂直センター揃えにより、内容の Cell 級アクセサリはセル全体 (title + description) に対して垂直センターに置かれる (MUST)
- `stackV` (= `descriptionLabel` の幅) は `accessoryHolder` より leading 側に制限され、description が Cell 級アクセサリと重なってはならない (MUST NOT)
- `stackH` の各 anchor は `contentView` の同方向 anchor とイコール (MUST)。minHeight 制約は既存挙動を維持する
- `hintLabel` は `contentView` ではなく `self` (cell インスタンス) に直接 addSubview し、右上 float 配置を維持する (MUST)。本 change で `hintLabel` の挙動・配置は変更しない

`applyCellBaseLayout(...)` は trailing の受け口を 2 系統持たなければならない (MUST):

- `accessoryView: UIView?` — Cell 級アクセサリ (UISwitch / checkbox / checkmark / chevron)。`applyCellBaseLayout` は毎回 `accessoryHolder` の旧内容を除去してから配置しなければならない (MUST) — `accessoryHolder` の内容は常に 0 個または 1 個であり、再 render で蓄積しない。non-nil のとき配置して `isHidden = false`、nil のとき空にして `isHidden = true` にする
- `trailingViews: [UIView]` / `valueLabelText: String?` — 行内 trailing。従来どおり `contentStack` の `titleLabel` の後ろに順序どおり配置する

派生 Cell renderer は本階層を破壊してはならない (MUST NOT)。具体的には:

- 恒常階層 (`stackH` とその arrangedSubviews / `stackV` / `contentStack` / `titleLabel` / `descriptionLabel` / `iconImageView` / `accessoryHolder`) を除去・置換してはならない (MUST NOT)。ただし Cell renderer が補助 view (例: Picker 系の `EmbeddedPickerHostField`) を `contentView` の背面に追加することは妨げない (既存挙動の維持)
- `stackH` / `stackV` / `contentStack` の `arrangedSubviews` から `iconImageView` / `titleLabel` / `descriptionLabel` / `accessoryHolder` を除去してはならない (MUST NOT)
- `cell.contentConfiguration` を non-nil で設定してはならない (MUST NOT)
- `cell.accessories` を非空配列で設定してはならない (MUST NOT)

`KsListCellBase.prepareForReuse()` は subview 構造 (`arrangedSubviews` 配列の恒常メンバー) を破壊せず、各 `UILabel.text` / `UIImageView.image` を nil クリアしなければならない (MUST)。`applyCellBaseLayout(...)` 経由で追加された `contentStack` の行内 trailing、および `accessoryHolder` の内容は本関数内で除去しなければならない (MUST)。first responder を保持中の view の保護 (編集中 UITextField の維持) は既存挙動を維持する。

#### Scenario: 初期化直後の subview hierarchy

- **GIVEN** `let base = KsListCellBase(frame: CGRect(x: 0, y: 0, width: 320, height: 44))`
- **WHEN** `base` の subview ツリーを観察する
- **THEN** `base.stackH.isDescendant(of: base.contentView) == true` かつ `base.stackH.arrangedSubviews == [base.iconImageView, base.stackV, base.accessoryHolder]` かつ `base.stackV.arrangedSubviews == [base.contentStack, base.descriptionLabel]` かつ `base.contentStack.arrangedSubviews.first === base.titleLabel` かつ `base.accessoryHolder.isHidden == true` (空のため)

#### Scenario: accessoryView が accessoryHolder に配置される

- **GIVEN** 任意の Cell renderer が `applyCellBaseLayout(base, ..., accessoryView: toggle)` を呼び出した直後
- **WHEN** `base.accessoryHolder` を観察する
- **THEN** `toggle` が `accessoryHolder` の内容として配置され、`accessoryHolder.isHidden == false`。`base.contentStack.arrangedSubviews` に `toggle` は含まれない。`base.contentConfiguration == nil` かつ `base.accessories == []`

#### Scenario: accessoryView が nil なら accessoryHolder は空で隠れる

- **GIVEN** 任意の Cell renderer が `applyCellBaseLayout(base, ..., accessoryView: nil)` を呼び出した直後
- **WHEN** `base.accessoryHolder` を観察する
- **THEN** `accessoryHolder` は内容を持たず `isHidden == true`。アクセサリ用の空領域は残らない

#### Scenario: 再 render でアクセサリが蓄積しない

- **GIVEN** `applyCellBaseLayout(base, ..., accessoryView: viewA)` を呼んだ後の `base`
- **WHEN** 続けて `applyCellBaseLayout(base, ..., accessoryView: viewB)` を呼ぶ (reconfigure 相当)
- **THEN** `accessoryHolder` の内容は `viewB` のみ (viewA は残らない)。続けて `accessoryView: nil` で呼べば `accessoryHolder` は空になり `isHidden == true`。`hideArrow` の false → true → false のような再 render を繰り返しても `accessoryHolder` の内容は常に 0 個または 1 個

#### Scenario: レイアウト後の幾何関係 (description とアクセサリの非交差・垂直センター)

- **GIVEN** 固定幅 (例: 320pt) の `KsListCellBase` に、折り返しが発生する長文 description と accessoryView (UISwitch) を渡して `applyCellBaseLayout` を呼んだ状態
- **WHEN** レイアウトを実行させて (`layoutIfNeeded`) 各 frame を観察する
- **THEN** `descriptionLabel` の表示領域の maxX は `accessoryHolder` の minX 以下であり、`accessoryHolder` の中心 Y は `contentView` の中心 Y と許容差内で一致する。accessoryView が nil の場合は同条件で `stackV` が trailing margin まで広がる

#### Scenario: render 後の行内 trailing 配置

- **GIVEN** 任意の Cell renderer が `applyCellBaseLayout(base, ..., trailingViews: [view1, view2])` を呼び出した直後
- **WHEN** `base.contentStack.arrangedSubviews` を観察する
- **THEN** `[base.titleLabel, view1, view2]` の順で並んでいる

#### Scenario: prepareForReuse で行内 trailing と accessory が除去される

- **GIVEN** render 後の `base` で `contentStack` に customView (first responder を含まない)、`accessoryHolder` に toggle が配置されている
- **WHEN** `base.prepareForReuse()` を呼ぶ
- **THEN** `base.contentStack.arrangedSubviews == [base.titleLabel]`、`accessoryHolder` は空で `isHidden == true`、`base.titleLabel.text == nil`、`base.descriptionLabel.text == nil`、`base.iconImageView.image == nil`。恒常メンバー (`iconImageView` / `stackV` / `accessoryHolder` / `titleLabel` / `descriptionLabel`) の階層は破壊されない

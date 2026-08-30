# settings-view-ios-host Specification 差分（migrate-cell-base-to-stack-layout）

## ADDED Requirements

### Requirement: KsListCellBase の自前 UIStackView 階層

iOS の `KsListCellBase` （`UICollectionViewListCell` 派生）は、`init(frame:)` で 1 度だけ、AiForms.Maui.SettingsView オリジナル `CellBaseView.cs` 準拠の自前 `UIStackView` 階層を `contentView` 直下に install しなければならない (MUST)。後段の `applyCellBaseLayout(...)` は本階層を更新する形でのみ Cell の見た目を構成する。

階層構造は以下を MUST とする：

```
contentView
  └─ stackH (UIStackView,
            axis=.horizontal,
            alignment=.center,
            spacing=16,
            layoutMargins=UIEdgeInsets(top:6, left:16, bottom:6, right:16),
            isLayoutMarginsRelativeArrangement=true)
       ├─ iconImageView (UIImageView, contentMode=.scaleAspectFit,
       │                 hugging=.required/.horizontal,
       │                 ccr=.required/.horizontal,
       │                 image nil 時は isHidden=true)
       └─ stackV (UIStackView,
                 axis=.vertical,
                 spacing=4,
                 hugging=.defaultLow/.horizontal,
                 ccr=.required/.horizontal)
            ├─ contentStack (UIStackView,
            │                axis=.horizontal,
            │                spacing=6,
            │                hugging=.defaultLow/.horizontal,
            │                ccr=.required/.horizontal)
            │    └─ titleLabel (UILabel,
            │                  hugging=.defaultLow/.horizontal,
            │                  ccr=.required/.horizontal)
            │    [Cell renderer が render 時に trailingViews を addArrangedSubview する]
            └─ descriptionLabel (UILabel,
                                numberOfLines=0,
                                lineBreakMode=.byWordWrapping,
                                hugging=.defaultLow/.horizontal,
                                ccr=.required/.horizontal,
                                空文字列のとき isHidden=true)
```

制約：

- `stackH.topAnchor` / `leadingAnchor` / `trailingAnchor` / `bottomAnchor` を `contentView` の同方向 anchor とイコール (MUST)
- `stackH.heightAnchor` に minHeight 制約 `greaterThanOrEqualTo:` を install し、priority は `.defaultHigh`（999 相当）で衝突警告を抑制 (MUST)
- `hintLabel` は AiForms オリジナル同様 `contentView` ではなく `self`（cell インスタンス）に直接 addSubview し、右上 float 配置を維持する (MUST)。本 change で `hintLabel` の挙動・配置は変更しない

派生 Cell renderer は本階層を破壊してはならない (MUST NOT)。具体的には：

- `contentView.subviews` を変更してはならない (MUST NOT)
- `stackH` / `stackV` / `contentStack` の `arrangedSubviews` から `iconImageView` / `titleLabel` / `descriptionLabel` を `removeArrangedSubview` してはならない (MUST NOT)
- `cell.contentConfiguration` を non-nil で設定してはならない (MUST NOT)
- `cell.accessories` を非空配列で設定してはならない (MUST NOT)

`KsListCellBase.prepareForReuse()` は subview 構造（`arrangedSubviews` 配列）を破壊せず、各 `UILabel.text` / `UIImageView.image` を `nil` クリアし、`isHidden` を `false`（または render 時に再設定される値）に戻すのみとしなければならない (MUST)。`contentStack` に `applyCellBaseLayout(...)` 経由で追加された `trailingViews` 由来の subview は本関数内で `removeArrangedSubview(...)` + `removeFromSuperview()` で除去しなければならない (MUST)。

#### Scenario: 初期化直後の subview hierarchy

- **GIVEN** `let base = KsListCellBase(frame: CGRect(x: 0, y: 0, width: 320, height: 44))`
- **WHEN** `base` の subview ツリーを観察する
- **THEN** `base.stackH.isDescendant(of: base.contentView) == true` かつ `base.stackH.arrangedSubviews == [base.iconImageView, base.stackV]` かつ `base.stackV.arrangedSubviews == [base.contentStack, base.descriptionLabel]` かつ `base.contentStack.arrangedSubviews.first === base.titleLabel`

#### Scenario: render 後の trailingViews 配置

- **GIVEN** 任意の Cell renderer が `applyCellBaseLayout(base, ..., trailingViews: [view1, view2])` を呼び出した直後
- **WHEN** `base.contentStack.arrangedSubviews` を観察する
- **THEN** `[base.titleLabel, view1, view2]` の順で並んでいる。`base.contentConfiguration == nil` かつ `base.accessories == []`

#### Scenario: prepareForReuse で trailingViews が除去される

- **GIVEN** render 後の `base` で `base.contentStack.arrangedSubviews == [base.titleLabel, customView]`
- **WHEN** `base.prepareForReuse()` を呼ぶ
- **THEN** `base.contentStack.arrangedSubviews == [base.titleLabel]`（customView は除去）、`base.titleLabel.text == nil`、`base.descriptionLabel.text == nil`、`base.iconImageView.image == nil`、`base.iconImageView.isHidden == true`、`base.descriptionLabel.isHidden == true`、`base.stackH` / `base.stackV` / `base.contentStack` の `arrangedSubviews` 配列（trailingViews 除外後）は破壊されない

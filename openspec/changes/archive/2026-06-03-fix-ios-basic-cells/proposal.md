## Why

`add-cell-types-basic` で iOS に追加した基本 Cell 7 種を実機確認したところ、オリジナルの `AiForms.Maui.SettingsView` と乖離した 4 つの UI 不具合が判明した。いずれも「オリジナル互換のクラシックな設定画面の見た目・操作感」という製品方針に反するため、本変更提案でまとめて是正する。

1. **謎の Sticky 挙動**: classic スタイル（`.plain` Appearance）でセクションヘッダーがスクロール固定され、さらに各セクションに空のグレーのフッター帯が表示される。オリジナルはヘッダー非固定・空フッター非表示が基本。
2. **CheckboxCell のチェック UI**: `UICellAccessory.checkmark`（チェックマークのみ）で描画しており、オリジナルの「角丸の四角い枠＋塗りつぶし＋白チェックマーク」のチェックボックス UI と異なる。
3. **RadioCell の気持ち悪いアニメーション**: On→Off で checkmark accessory を削除しているため、チェックマークが右にスライドして消える不自然なアニメーションになる。位置を変えない普通のフェードアウトにすべき。
4. **SimpleCheckCell の独自レイアウト**: `content.image` でタイトル左側にチェックを置く独自レイアウトになっている。オリジナルは `UITableViewCellAccessory.Checkmark`（右端チェック）であり、RadioCell と同じレイアウトでよい。

オリジナル参照:
- `AiForms.Maui.SettingsView/SettingsView/Native/iOS/Cells/CheckboxCellView.cs`
- `AiForms.Maui.SettingsView/SettingsView/Native/iOS/Cells/SimpleCheckCellView.cs`

## What Changes

- **① Sticky 挙動の是正（`settings-view-ios-ui`）**: classic スタイルでもセクションヘッダーを `pinToVisibleBounds = false` 相当（非固定）にし、フッターを持たないセクションに空のフッター帯を出さない。`UICollectionLayoutListConfiguration` の `headerMode` / `footerMode` の決定方針を「footer を持つセクションがあるときのみ footer supplementary を出す」「ヘッダーは固定しない」に変更する。
- **② CheckboxCell のチェック UI（`cell-types-basic`）**: オリジナルの `CheckBox`（`UIButton` + `Draw`）相当を `UIView` ベースのカスタムチェックボックスとして実装し、`UICellAccessory.customView` で右端に配置する。角丸の四角枠（CornerRadius 3 / BorderWidth 2）、チェック時は accent カラーで塗りつぶし＋白いチェックマーク、非チェック時は枠のみを描画する。
- **③ RadioCell のフェードアウト（`cell-types-basic`）**: checkmark を常設の `customView` accessory とし、On/Off は accessory の追加・削除ではなく `alpha` のフェードで切り替える。位置を固定したまま自然にフェードイン／アウトする。
- **④ SimpleCheckCell のレイアウト（`cell-types-basic`）**: `content.image` による左側チェックをやめ、RadioCell と同じく右端 checkmark accessory（`isChecked == false` で非表示）に変更する。RadioCell と同様に On→Off の自然なフェードアウト方針も適用する。
- **仕様の整合**: `cell-types-basic` の `SimpleCheckCell` Requirement の「タイトル左側にチェック」記述を「右端チェック（RadioCell と同レイアウト）」へ修正する。`CheckboxCell` Requirement にチェックボックス UI の見た目要件を補足する。`settings-view-ios-ui` の classic スタイル Appearance 要件にヘッダー非固定・空フッター非表示の要件を追記する。
- iOS Sample（`BasicCellsDemoView`）で 4 点の修正が目視確認できることを確認する。

## Impact

- 影響を受ける capability: `cell-types-basic`（CheckboxCell / RadioCell / SimpleCheckCell の描画要件）、`settings-view-ios-ui`（classic スタイルのレイアウト要件）
- 影響を受けるコード（iOS のみ）:
  - `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`（`makeLayout` のレイアウト構成）
  - `ios/Sources/KsSettingsViewUI/CheckboxCellView.swift`（カスタムチェックボックス UIView の追加）
  - `ios/Sources/KsSettingsViewUI/RadioCellView.swift`（customView accessory + alpha フェード）
  - `ios/Sources/KsSettingsViewUI/SimpleCheckCellView.swift`（右端 checkmark accessory + フェード）
  - `ios/Tests/KsSettingsViewUITests/*`（各 Cell の描画・状態テスト）
- Android 側への影響なし（iOS 固有の UI 不具合のため）。
- 公開 API（Cell の構造・コンストラクタ・DSL）への破壊的変更なし。見た目・アニメーションのみの是正。

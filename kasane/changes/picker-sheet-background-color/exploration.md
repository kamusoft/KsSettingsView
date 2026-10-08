# Exploration: picker-sheet-background-color

## 課題 / 動機

起票元: ../ColorAnalyzer/kasane/changes/archive/2026-09-22-fix-dark-picker-sheet-contrast/exploration.md

**選択面 (PickerCell のシート) の地の色を Cell 背景色と独立に指定したい**

### 何が起きたか / 何を変えたか

ダークテーマで `PickerCell` の選択面を開くと、シートの面が下に敷かれた設定画面に溶け込み、境界が分からない (iOS 実機・0.1.0-beta.6 で確認。Android も同じ継承なので同様と見ている)。

原因は、選択面の地の色が呼び出し元 Cell の実効 `CellBackgroundColor` を継承する設計にある。

- iOS: `ios/Sources/KsSettingsViewUI/PickerListViewController.swift` の `tableView.backgroundColor` / 候補行の `backgroundColor` が `effective.cellBackgroundColor`。スクロール上端のナビバーは透過構成なので、シート全体が Cell 背景色になる
- Android: `PickerSelectionSheet` の `sheetBackgroundColor` が `theme.cellBackgroundColor`

このアプリのダークテーマは Cell 背景が #000000 なので、シートも #000000 になり、下の画面の Cell (同じ #000000) と重なって境界が消える。

### 相手に関係する理由

利用側からは選択面の色だけを変える手段が無い。`PickerCell.BackgroundColor` を個別に変えると、その Cell 自身の背景も一緒に変わり、リストの並びが崩れる。

### 提案する対応

`Theme` (SettingsView の Style 値) と `PickerCell` に、選択面の地の色を指定する値 (例: `PickerBackgroundColor`。未指定なら現状どおり `CellBackgroundColor` を継承) を足すことを検討してほしい。`NumberPickerCell` / `TimePickerCell` / `DatePickerCell` の選択面にも同じ考え方が当てはまるかは、そちらの設計判断に委ねる。

このアプリ側では、この要望とは独立に、ダークテーマの配色を「リスト下地 #000000 / Cell 面 一段明るいグレー」の階層に変えて境界を出す対応を進めている。要望が入った場合に、こちらの配色をそのまま維持するか選択面だけ別色にするかは、入った時点で判断する。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)

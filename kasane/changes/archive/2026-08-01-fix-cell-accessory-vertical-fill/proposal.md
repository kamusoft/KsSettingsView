# Proposal: fix-cell-accessory-vertical-fill

## Why

iOS の Cell 共通行レイアウトは自前 UIStackView 構造 (コミット `8fea183`) へ移行済みだが、その際の設計判断「全 trailing control を `contentStack` (タイトル行) へ」が AiForms オリジナルの構造と食い違っている。オリジナルは Switch / Checkbox / chevron / checkmark を `UITableViewCell.AccessoryView` / `Accessory` (ContentView の外側) に置くため、アクセサリはセル全体に対して垂直センターに置かれ、description はアクセサリの左までで折り返す。現 KsSettingsView iOS ではアクセサリがタイトル行に同居し、description がセル全幅で折り返してアクセサリの下に回り込む (ui/references/ のスクショ比較参照)。

Android は `descriptionView.END = accessoryHolder.START` 制約で既に正しい構造のため、修正対象は iOS のみ。

## What Changes

- `KsListCellBase` の stackH 直下・stackV の後ろに「アクセサリ列」を追加する ([ios/ADR-0001](../../decisions/ios/0001-accessory-column-outside-content-stack.md))
- `applyCellBaseLayout` に `accessoryView: UIView?` を追加し、trailing control の受け口を 2 系統に整理する:
  - `accessoryView` — Cell 級アクセサリ (Switch / Checkbox / Radio・SimpleCheck の checkmark / chevron)。アクセサリ列へ
  - `trailingViews` — 行内 trailing (EntryCell の TextField、valueLabel)。従来どおり contentStack へ
- Cell renderer 12 種のうち、Cell 級アクセサリを持つ 9 種 (Command / Switch / Checkbox / Radio / SimpleCheck / Picker / NumberPicker / TimePicker / DatePicker) を新 API へ振り分け直す (Button / Label はアクセサリ無し、Entry は TextField を contentStack に残すため変更なし)
- 影響 capability: `settings-view-ios-host` (stack 階層の MUST 規定)、`cell-types-basic` (共通行の trailing 2 系統の契約)

## Non-Goals

- Android 実装の変更 (既に正しい構造)
- Section header / footer の supplementary view (従来どおり `UIListContentConfiguration`)
- 外部公開 API (`KsCellRegistry` / DSL / Theme / CellStyle) の変更
- EntryCell の TextField 配置の変更 (contentStack のまま。AiForms 準拠)

## Impact

- 破壊的変更なし (変更は internal API のみ)
- iOS の [KsListCellBase.swift](../../../ios/Sources/KsSettingsViewUI/KsListCellBase.swift) / [CellBaseLayout.swift](../../../ios/Sources/KsSettingsViewUI/CellBaseLayout.swift) と Cell renderer 約 10 ファイル、および contentStack ベースのテスト assert
- リスク: アクセサリ列追加による行高さ・既存見た目のデグレ → mock との視覚照合とスクショ比較で担保
- concepts 追随 (蒸留時): `core/styling/cell-row-layout.md` の視覚文法へ「description はアクセサリ列と重ならない」を明文化

## 級: M

iOS のみ・公開 API 影響なしだが、renderer 約 10 件への波及とテスト追従・UI 視覚照合を伴うため。

domain: ios

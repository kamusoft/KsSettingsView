# Exploration: fix-cell-accessory-vertical-fill

## 課題 / 動機

openspec 時代の未処理 change `openspec/changes/migrate-cell-base-to-stack-layout` の kasane 移行として探索を開始したが、調査の結果、同提案 (iOS Cell Base の自前 UIStackView 構造化) はコミット `8fea183` (2026-06-15) で proposal 作成と同一コミットで**実装済み**と判明 (tasks.md 未チェックのままアーカイブされず残置)。

残っている実仕事は、その実装が抱えるレイアウト欠陥の修正:

- オリジナル (AiForms / SettingsView.Maui): Switch 等のアクセサリはセルの垂直方向に Fill (垂直センター) し、description はアクセサリの左までで折り返す
- 現 KsSettingsView iOS: アクセサリがタイトル行 (contentStack) に同居し、description がセル全幅で折り返してアクセサリの下に回り込む

根本原因: 旧 design.md Decision 2 が「全 trailing control を `contentStack.addArrangedSubview`」と定めたこと。AiForms オリジナルは Switch / Checkbox / chevron / checkmark を `UITableViewCell.AccessoryView` / `Accessory` (ContentView の外側) に置いており、ContentStack 経由は EntryCell の TextField のみ。

Android は `CellBaseLayout.kt` の `descriptionView.END = accessoryHolder.START` 制約で既に正しく、修正対象は **iOS のみ**。

## 検討した選択肢 (却下案と理由を含む)

- **A案 (採用)**: stackH 直下・stackV の後ろにアクセサリ列を追加。`applyCellBaseLayout` に `accessoryView: UIView?` を導入し、Cell 級アクセサリ (Switch / Checkbox / checkmark / chevron) はそちら、行内 trailing (TextField / valueLabel) は従来の `trailingViews` (contentStack) へ、と 2 系統に整理
- B案 (却下): `UICellAccessory` へ戻す — customView 幅制約問題の再発、廃止済み経路の復活
- C案 (却下): descriptionLabel に幅制約のみ追加 — 回り込みは直るが垂直 Fill が成立しない対症療法

## 決定事項

- A案を採用 (ユーザー確定、2026-08-01)
- change 名は `migrate-cell-base-to-stack-layout` の再利用ではなく `fix-cell-accessory-vertical-fill` に切り直し (移行自体は実装済みのため、実態は欠陥修正)

## ADR 候補

- 作成済み: [ios/ADR-0001](../../decisions/ios/0001-accessory-column-outside-content-stack.md) (status: proposed — ユーザー確認待ち)

## 未決の論点

- 旧 `openspec/changes/migrate-cell-base-to-stack-layout` の残置扱い (openspec/ は凍結・編集禁止のため、archive へ移すか現状のまま歴史資料とするか)
- 各 Cell の trailing control の振り分け確定 (Switch / Checkbox / Radio / SimpleCheck / chevron → accessoryView、TextField / valueLabel → trailingViews が基本線。Picker 系の「valueLabel + chevron」は valueLabel が trailingViews・chevron が accessoryView になる想定だが、実装時に視覚照合で確認)
- concepts 追随: `kasane/concepts/core/styling/cell-row-layout.md` が旧表現 (UIKit configuration) のまま。「description はアクセサリ列の左までに制限される」という視覚文法の明文化も必要 (蒸留時)

## UI 素材 (ui/references/)

- `original-settingsview-maui.png` — オリジナル SettingsView.Maui のスクショ。採用点: SwitchCell のアクセサリ垂直センター配置と description の折り返し幅 (アクセサリの左まで)。LabelCell / CheckboxCell の trailing 配置も参考
- `current-kssettingsview.png` — 現 KsSettingsView のスクショ (問題の現状)。SwitchCell "Notification" で description がアクセサリの下に回り込んでいる箇所が対象。それ以外のセル種の見た目は概ね維持対象

## 変更級の推奨: M (理由)

iOS のみ・外部公開 API 影響なしだが、`applyCellBaseLayout` の内部 API 変更が Cell renderer 約 10 件に波及し、テスト assert の追従・スクショ視覚照合 (UI あり) を伴うため。デルタスペック対象は cell 共通行レイアウトの MUST 規定 (アクセサリ列と description 幅制限)。

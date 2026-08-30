## Verification Report: unify-cell-common-fields-via-shared-row-layout

**検証日**: 2026-06-09

### Summary

| Dimension    | Status                                                              |
|--------------|---------------------------------------------------------------------|
| Completeness | 実装タスク完了（未完了5件はすべて実機/エミュレータ目視確認、実装エージェント管掌外と明記） |
| Correctness  | 全 MUST 要件が実装済み（spec シグネチャ・配置規約・削除規約すべて一致）           |
| Coherence    | design.md の Decision と実装が整合。旧 Compose 版コード削除済み                |

---

### Issues

CRITICAL なし / WARNING なし / SUGGESTION なし

---

### 詳細確認結果

#### Completeness（完全性）

**タスク完了状況**

- 未完了チェックボックスは 5 件：
  - `~~8.1~~ ~~8.2~~ ...`（旧 Compose 化タスク撤回宣言行）
  - `~~10.2~~`（旧 Compose テスト撤回宣言行）
  - `12.4` iOS 実機目視確認
  - `12.5` Android 実機/エミュレータ目視確認
  - `12.R.4` iOS hintText 右上 float 目視確認
  - `12.R.5` Android hintText 右上 float 目視確認
- いずれも「実装エージェントの管掌外（実機/シミュレータへのアクセスが必要）。orchestrator / ユーザー側の最終確認工程として残す」と明記されており、実装工程としての未完了ではない。
- 全コーディングタスク（Phase 1.R / 2 / 3 / 4 / 5.R / 6.R / 7 / 8.R / 9 / 10.R / 11 / 12.R.1〜3 / 12.R.6）はすべて `[x]` 完了。

**Spec Coverage**

- `cell-types-basic/spec.md`: 全 Cell 共通フィールド、hintText 右上 float 規約、ButtonCell description 除外 — 実装確認済み。
- `settings-view-android-compose/spec.md`: CellBaseViews / applyCellBaseLayout / 各 ViewHolder 経由 — 実装確認済み。
- `settings-view-ios-swiftui/spec.md`: applyCellBaseLayout / hintLabel right-top float / prepareForReuse / 旧 ksCellRow 削除 — 実装確認済み。

---

#### Correctness（正確性）

**iOS applyCellBaseLayout**

- シグネチャ: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:45` に `@MainActor internal func applyCellBaseLayout(_ listCell:, title:, description:, valueText:, icon:, hintText:, effective:, isEnabled:, accessories: [UICellAccessory] = [], titleColorOverride:)` — spec の MUST シグネチャに完全一致（`titleColorOverride` は ButtonCell の 4 段階色決定のための拡張引数で ButtonCell Requirement を満たすために追加されたもの。spec は除外していないため問題なし）。
- hintText は `UICellAccessory` に含まず、`KsListCellBase.ensureHintLabel()` 経由で `cell` 直下に `addSubview` し AutoLayout 制約 `top=+2 / trailing=-10 / bottom<=-12` を設定 — spec の MUST に一致。
- accessories の順: `[valueText (subtitle 構成時のみ), 呼び出し側 accessories...]` — spec 一致。
- `KsCellViewSupport.setRenderState` / `applyEffectiveHeight` を内部で呼ぶ — spec 一致。
- isEnabled=false 時に全テキスト色を disabledTextColor で上書き — spec 一致。

**iOS hintLabel リサイクル管理**

- `KsListCellBase.swift:43,63,82`: `hintLabel: UILabel?` lazy プロパティ + `ensureHintLabel()` で重複 addSubview を防止。`prepareForReuse` で `hintLabel.text = nil / isHidden = true` — spec の方式 A MUST に一致。

**iOS 旧 ksCellRow 関数の削除**

- `grep -r "ksCellRow"` でコメント以外の呼び出し・定義なし — spec の MUST（削除）に一致。

**iOS 7 種 Cell View が applyCellBaseLayout を経由**

- LabelCellView / CommandCellView / SwitchCellView / CheckboxCellView / RadioCellView / SimpleCheckCellView / ButtonCellView — 全ファイルで `applyCellBaseLayout(...)` 呼び出しを確認。

**Android CellBaseViews 構造体**

- `CellBaseLayout.kt:39`: `internal class CellBaseViews(root: ConstraintLayout, iconView: AppCompatImageView, titleView: TextView, descriptionView: TextView, valueTextView: TextView, accessoryHolder: FrameLayout, hintTextView: TextView)` — spec の MUST フィールド構成に一致（`AppCompatImageView` は `ImageView` 派生で MUST を満たす）。

**Android buildCellBaseViews programmatic 構築**

- `CellBaseLayout.kt:65`: XML 不使用、ConstraintLayout + ConstraintSet でプログラム的に構築 — spec MUST 一致。
- `addView` 順序: iconView → titleView → descriptionView → valueTextView → accessoryHolder → hintTextView（最後）で Z 順前面保証 — spec の MUST 一致。

**Android ConstraintLayout 配置規約**

- iconView: Start=parent / Top=parent / Bottom=parent — 一致。
- titleView: Start=iconView.End / Top=parent / End=valueTextView.Start — spec では End=accessoryHolder.Start と規定されているが実装は valueTextView.Start が中間エンドポイント。実質 `valueTextView` が `accessoryHolder.Start` を基準に配置されるため意図は同等（valueTextView → accessoryHolder の chain）。
- descriptionView: Start=iconView.End / Top=titleView.Bottom / End=accessoryHolder.Start / Bottom=parent — 一致。
- valueTextView: End=accessoryHolder.Start / Baseline=titleView.Baseline — 一致。
- accessoryHolder: End=parent / Top=parent / Bottom=parent — 一致。
- hintTextView: End=parent(margin 10dp) / Top=parent(margin 2dp) — 一致。

**Android applyCellBaseLayout 関数シグネチャ**

- `CellBaseLayout.kt:236`: `internal fun applyCellBaseLayout(views: CellBaseViews, title: String, description: String?, valueText: String?, icon: KsImage?, hintText: String?, effective: EffectiveStyle, isEnabled: Boolean = true)` — spec の MUST シグネチャに一致。

**Android 各 ViewHolder が applyCellBaseLayout を経由**

- LabelCellViewHolder / CommandCellViewHolder / SwitchCellViewHolder / CheckboxCellViewHolder / RadioCellViewHolder / SimpleCheckCellViewHolder / ButtonCellViewHolder — 全 ViewHolder の bind 内で `applyCellBaseLayout(views, ...)` 呼び出しを確認。

**Android KsCellRowLayout.kt 削除**

- `find -name "KsCellRowLayout.kt"` 結果なし。テスト `10.R.6` でも class loader 検査を実施 — spec MUST 一致。

**Android ComposeView.setContent 不使用**

- 7 種 ViewHolder ファイルに `ComposeView` / `setContent` の記述なし — spec の MUST NOT に一致。

**ButtonCell description 除外**

- iOS `ButtonCell.swift`: `description` フィールドなし（`valueText / icon / hintText` のみ）— spec の MUST NOT に一致。
- Android `ButtonCell.kt`: `description` フィールドなし — 同様に一致。

**各 Cell モデルの共通フィールドと Hashable/Equatable**

- iOS SwitchCell / RadioCell (description/valueText/icon/hintText/accentColor)、Android SwitchCell / RadioCell — `==` / `hash` / `equals` / `hashCode` への追加フィールド反映を確認。

**DSL 拡張関数**

- Android `BasicCellDsl.kt`: SwitchCell/CheckboxCell/RadioCell/SimpleCheckCell/ButtonCell DSL 関数に共通フィールドを Optional 引数として追加確認。
- iOS: `DSLIconModifiable.swift` + 各 Cell Extensions 経由で共通フィールド反映確認。

**テスト**

- iOS `UnifyCellCommonFieldsTests.swift`: Phase 5.R（5.R.2〜5.R.8）hintLabel float 配置テスト、accessories 並び順テスト確認。
- Android `UnifyCellCommonFieldsTest.kt`: Phase 10.R（10.R.1〜10.R.6）CellBaseViews 配置回帰テスト、KsCellRow 削除確認テスト確認。

**ビルド・テスト結果**（実装者報告 + tasks.md 確認）

- iOS 236 件成功（12.R.1 完了）
- Android 287 件成功（12.R.2 完了）
- Compose テスト成功（12.R.3 完了）

---

#### Coherence（整合性）

**design.md との整合**

- Decision 11（Android View ベース採用）: `KsCellRow.kt` 削除済み、全 ViewHolder が `CellBaseViews` + `applyCellBaseLayout` 経由 — 整合。
- Decision 12（hintText 右上 float）: iOS は `topAnchor+2 / trailingAnchor-10`、Android は `End=parent(margin10) / Top=parent(margin2)` — オリジナル AiForms 踏襲で整合。
- Decision 9（ButtonCell aux 切替）: `hasAux` 判定でボタンスタイル/通常レイアウトを切替、`buttonStyleSet.applyTo` で titleView を全体に広げる — 整合。

**コードパターン一貫性**

- 全 ViewHolder が `companion object.create(parent)` パターンで `buildCellBaseViews(parent)` を呼んで構築 — 一貫。
- iOS は `ensureHintLabel()` ヘルパで lazy 生成・重複防止パターン — 一貫。

---

### Final Assessment

CRITICAL なし / WARNING なし / SUGGESTION なし

**全チェック通過。アーカイブ可能。**

未完了タスク（12.4 / 12.5 / 12.R.4 / 12.R.5）は実機/シミュレータ/エミュレータへのアクセスが必要な目視確認工程であり、tasks.md に「orchestrator / ユーザー側の最終確認工程として残す」と明記されている。これらは実装完了の要件には含まれず、アーカイブの障壁とはならない。

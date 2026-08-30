## Verification Report: add-cell-types-input（Android Phase 8〜14, 16）

検証日: 2026-06-15
検証対象: Android 版実装（Phase 8〜14, 16）。Phase 15（ドキュメント）は除外。iOS（Phase 0〜7）は対象外。

---

### Summary

| Dimension    | Status                                              |
|--------------|-----------------------------------------------------|
| Completeness | Phase 8〜14, 16 の全タスクチェック済み / 全 MUST 要件実装確認 |
| Correctness  | 全 Requirement 実装・spec 整合。spec 内の軽微な表記矛盾を確認（下記 SUGGESTION 参照） |
| Coherence    | design.md の全 Decision に準拠。プロジェクトパターンと一貫     |

---

### Issues

**CRITICAL（アーカイブ前必須対応）: なし**

**WARNING（対応推奨）: なし**

**SUGGESTION（後続セッションで対応望ましい）**

#### SUGGESTION-1: spec.md の DSL シグネチャ例と Requirement テキストに textAlignment 既定値の表記矛盾がある

- spec `Requirement: Compose DSL 拡張関数による入力 Cell 直置き`（spec.md 行 167）の EntryCell DSL シグネチャ例では `textAlignment: CellTitleAlignment = CellTitleAlignment.start` と記述されている。
- 一方 spec `Requirement: EntryCell`（spec.md 行 308）のフィールド説明では「既定 `.end`、AiForms オリジナル `EntryCell.TextAlignmentProperty` の既定 `TextAlignment.End` 準拠」と明記されている。
- **実装は Requirement テキスト・design.md（Decision 8）・tasks.md（タスク 2.11「`.start` → `.end` に修正」）に従って `CellTitleAlignment.END` を採用しており、これは正しい判断。**
- 後続セッションで spec.md の DSL シグネチャ例を `CellTitleAlignment.end` に修正しておくと、spec の一貫性が保たれる。
- 参照ファイル: `openspec/changes/add-cell-types-input/specs/cell-types-input/spec.md` 行 167

---

### 検証詳細

#### Completeness

**タスク完了状況（Android 対象 Phase）**

| Phase | タスク | 状態 |
|-------|--------|------|
| 1.2 | PickerSelectionMode 追加 | [x] |
| 1.3 | DatePickerAndroidStyle 追加 | [x] |
| 8.1〜8.5 | Android EntryCell + ViewHolder + テスト | [x] |
| 9.1〜9.5 | Android PickerCell + ViewHolder + テスト | [x] |
| 10.1〜10.3 | Android NumberPickerCell + ViewHolder + テスト | [x] |
| 11.1〜11.6 | Android TimePickerCell / DatePickerCell + ViewHolder + テスト | [x] |
| 12.1〜12.2 | Android 一括登録 (registerInputCells) + auto-register ガード | [x] |
| 13.1〜13.3 | Compose DSL 拡張関数 (InputCellDsl.kt) + テスト | [x] |
| 14.2 | Android Sample 更新 | [x] |
| 16.2 | Android 全テスト実行・確認 | [x] |

Phase 15（ドキュメント）は検証除外。すべての Android 対象タスクが完了済み。

**MUST 要件のカバレッジ**

| Requirement | 実装ファイル | 判定 |
|---|---|---|
| 共通 Optional フィールド (description/icon/hintText/isEnabled/isVisible) | EntryCell.kt〜DatePickerCell.kt | OK |
| EntryCell は valueText を持たない | EntryCell.kt（フィールドなし） | OK |
| VisibilityAware opt-in | 5 種すべてで `VisibilityAware` 実装・`isVisible` プロパティ確認 | OK |
| 共通行レイアウト関数 applyCellBaseLayout 経由 | 全 5 ViewHolder で applyCellBaseLayout 呼び出し確認 | OK |
| id デフォルト値規約（"entry-cell-${UUID}"等）| 5 種すべてで規約通りのデフォルト値 | OK |
| keyboardType Native Int 直接公開 | EntryCell.kt / EntryCellViewHolder.kt で InputType.TYPE_* をそのまま代入 | OK |
| isPassword: TYPE_TEXT_VARIATION_PASSWORD OR 合成 | EntryCellViewHolder.kt 行 85-90 | OK |
| maxLength: InputFilter.LengthFilter | EntryCellViewHolder.kt 行 124-128 | OK |
| IME 自動表示 (showSoftInput) | EntryCellViewHolder.kt 行 150-168 | OK |
| PickerSelectionMode 列挙型 | PickerSelectionMode.kt（Single / Multiple）| OK |
| DatePickerAndroidStyle 列挙型 | DatePickerAndroidStyle.kt（Material / Spinner）| OK |
| valueText 自動表示（PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）| 各 ViewHolder の bind で autoValueText / value.toString() / formatTime / formatDate を使用 | OK |
| PickerCell Single → AlertDialog.setSingleChoiceItems | PickerCellViewHolder.kt 行 73-81 | OK |
| PickerCell Multiple → AlertDialog.setMultiChoiceItems + 完了ボタン | PickerCellViewHolder.kt 行 84-123 | OK |
| maxSelectedNumber 上限到達時の無視 + 触覚フィードバック | PickerCellViewHolder.kt 行 92-108 | OK |
| NumberPicker を内包する AlertDialog | NumberPickerCellViewHolder.kt 行 93-103 | OK |
| min > max 時の警告ログ | NumberPickerCellViewHolder.kt 行 72-79 | OK |
| MaterialTimePicker | TimePickerCellViewHolder.kt 行 66-80 | OK |
| DatePickerCell Material → MaterialDatePicker | DatePickerCellViewHolder.kt 行 75-108 | OK |
| DatePickerCell Spinner → DatePicker AlertDialog | DatePickerCellViewHolder.kt 行 117-159 | OK |
| minDate/maxDate (Material: CalendarConstraints, Spinner: DatePicker.minDate/maxDate) | DatePickerCellViewHolder.kt 行 81-89, 137-138 | OK |
| androidButtonColor をボタン色に反映 | DatePickerCellViewHolder.kt 行 153-158 | OK |
| 一括登録 registerInputCells(context) | KsCellRegistryInputCells.kt（5 種登録、viewType 110-114）| OK |
| KsSettingsView.init auto-register (!isRegistered ガード) | KsSettingsView.kt 行 179-181 | OK |
| Compose DSL EntryCell/PickerCell(×2)/NumberPickerCell/TimePickerCell/DatePickerCell | InputCellDsl.kt（5 種 + 計 6 overload）| OK |
| DSL 戻り値 CellHandle + .cellHeight chain | InputCellDsl.kt 全関数で CellHandle を返す。テストで chain 動作確認済み | OK |
| MutableState<String/Int?/Set<Int>/Int/LocalTime/LocalDate> 引数 | InputCellDsl.kt の TwoWay binding overload | OK |
| DateTimeFormatter キャッシュ (ConcurrentHashMap) | TimePickerCellViewHolder.kt / DatePickerCellViewHolder.kt | OK |
| bindingAdapterPosition 使用（deprecation 対応）| TimePickerCellViewHolder.kt 行 80, DatePickerCellViewHolder.kt 行 107 | OK |
| 5 種 ViewHolder の reset() 強化 | 各 ViewHolder.reset() で disclosureView / onFocusChangeListener 等をクリア | OK |
| Sample に 5 種すべて配置 | InputCellsDemoScreen.kt（EntryCell×3 / PickerCell×2 / Number / Time / DateCell×2）| OK |
| テスト: InputCellsTest (36件) / InputCellDslTest (12件) 全成功 | build/test-results/testDebugUnitTest/*.xml で failures=0, errors=0 確認 | OK |

---

#### Correctness

**design.md Decision との整合確認**

| Decision | 実装状況 |
|---|---|
| Decision 1: Native 型直接公開（java.time.LocalTime/LocalDate, InputType.Int）| 全 5 Cell の data class フィールドで独自型なし。OK |
| Decision 2: PickerCell 単一/複数両対応、TextPickerCell は実装しない | PickerCell.kt + DSL 2 overload。TextPickerCell.kt なし。OK |
| Decision 3: ピッカーモーダル実装方式 | AlertDialog/MaterialTimePicker/MaterialDatePicker を各 ViewHolder が担当。OK |
| Decision 7: EntryCell は valueText を持たない | EntryCell.kt にフィールドなし。EntryCellViewHolder で null を渡す。OK |
| Decision 8: 各 Cell の既定値 | textAlignment = END、format = "HH:mm"/"yyyy/MM/dd"、min/max/step = 0/100/1、androidUiStyle = Material、maxLength = null 等を全確認。OK |
| Decision 9: 共通行レイアウト関数経由 | 全 5 ViewHolder で applyCellBaseLayout 呼び出し。OK |
| Decision 10: VisibilityAware opt-in | 全 5 Cell が VisibilityAware を implements。OK |

**spec と実装の差異**

spec DSL シグネチャ例（行 167）の `CellTitleAlignment.start` は spec.md 内の表記ミス。Requirement テキスト（行 308）・design.md・tasks.md に従い実装は `END` を採用。整合は Requirement テキスト側が正であり、実装が正しい。（SUGGESTION-1 参照）

---

#### Coherence

- ファイル命名: `EntryCell.kt`, `EntryCellViewHolder.kt`, `KsCellRegistryInputCells.kt`, `InputCellDsl.kt` — 既存の `SwitchCell.kt`, `SwitchCellViewHolder.kt`, `KsCellRegistry+BasicCells.swift` などのパターンに準拠。
- viewType 割り当て: 基本 Cell の 100 番台と衝突しない 110〜114。
- TextWatcher の attach/detach パターン: 既存 SwitchCell 等の onBind/onReset 慣例に準拠。
- 差分判定（`if (editText.text?.toString() != cell.text)`）: IME 保護のための同値スキップ。spec の日本語 IME 対応 Requirement を正しく実装。

---

### Final Assessment

**CRITICAL なし、SUGGESTION 1 件（spec 内の表記矛盾、実装には影響なし）**

Android Phase 8〜14, 16 の実装は仕様の全 MUST 要件を満たし、design.md の全 Decision に準拠している。テスト 48 件（InputCellsTest 36 件 + InputCellDslTest 12 件）がすべてパスしており、品質基準を満たす。

**判定: VALID**

アーカイブ可能。SUGGESTION-1（spec.md 行 167 の textAlignment デフォルト値表記）は後続セッションで spec を修正することが望ましい。

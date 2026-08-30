## Verification Report: add-cell-types-basic

検証日: 2026-06-02

### Summary

| Dimension    | Status |
|---|---|
| Completeness | 実装系タスク完了済み。実機目視系（21.5.3/21.5.4/21.7.2〜21.7.6）はアーカイブ前提の未チェックとして保留 |
| Correctness  | spec.md の全 Requirement・Scenario が実装でカバーされている |
| Coherence    | design.md の全 Decision（特に Decision 9）が実装と整合している |

---

### Completeness

#### タスク完了状態

実装系タスクはすべて `[x]` 完了済み。

未完了のタスク（8件）はすべて「実機 / Emulator での目視確認」タスクであり、Background に記載のとおりアーカイブ条件として未チェックのまま残す前提：

- 21.5.3 onDrawOver 罫線と Ripple の重畳順序を実機確認
- 21.5.4 Theme.selectedColor 変更で Ripple 色が変わることを Sample で確認
- 21.7.2.1〜21.7.2.3 adb / 実機準備
- 21.7.3.1〜21.7.3.3 「基本 Cell 7 種デモ」画面への遷移と静的描画の目視確認
- 21.7.4.1〜21.7.4.2 タッチフィードバック（Ripple）の目視確認
- 21.7.5.1〜21.7.5.2 ちらつき非発生の確認
- 21.7.6.1 SwitchCell セル全体タップの目視確認

これらは実装の問題ではなく、実機操作を伴う確認作業であり、CRITICAL 扱いしない。

#### Spec Coverage

`specs/cell-types-basic/spec.md` の全 Requirement を確認：

| Requirement | 実装状態 |
|---|---|
| 具象 Cell の id デフォルト値規約 | SwitchCell/CheckboxCell/RadioCell/SimpleCheckCell 等で `"<type>-${UUID.randomUUID()}"` 形式のデフォルト値を確認済み |
| Compose DSL 拡張関数による Cell 直置き | ks-settingsview-compose モジュールに DSL 拡張関数が実装済み（tasks.md 1.5.5 完了） |
| KsImage 値型 | android core の KsImage.kt に実装確認済み |
| LabelCell | LabelCellViewHolder.kt 実装済み・テスト済み |
| CommandCell | CommandCellViewHolder.kt + ic_navigate_next.xml VectorDrawable 実装済み・テスト済み |
| ButtonCell | ButtonCellViewHolder.kt 実装済み・テスト済み |
| SwitchCell | SwitchCellViewHolder.kt（MaterialSwitch、セル全体タップ ON/OFF）実装済み・テスト済み |
| CheckboxCell | CheckboxCellViewHolder.kt（AppCompatCheckBox）実装済み・テスト済み |
| RadioCell | RadioCellViewHolder.kt（KsSimpleCheckView）実装済み・テスト済み |
| SimpleCheckCell | SimpleCheckCellViewHolder.kt（KsSimpleCheckView 右側 30×30dp）実装済み・テスト済み |
| 基本 Cell の登録 API | KsCellRegistryBasicCells.kt・registerBasicCells() 実装済み・テスト済み |
| PoC Cell の削除 | PocLabelCell 系ファイルなし、ソース検索で不在確認済み |
| ユニットテスト | BasicCellsTest.kt で bind/通知/reload/payload/accent/登録 API を全 Cell 分カバー |

---

### Correctness

#### Decision 9 の実装対応

| Decision | 仕様記述 | 実装 | 評価 |
|---|---|---|---|
| 9-1 ちらつき修正 | SwitchCell/CheckboxCell/RadioCell/SimpleCheckCell の equals/hashCode から内部状態を除外、payload 差分導入 | 4 Cell で isOn/isChecked/selectedValue を equals/hashCode から除外確認済み。CellChangePayload sealed interface + getChangePayload + onBindViewHolder(payloads) 実装確認済み | 一致 |
| 9-2 ナビゲーションインジケータ | ic_navigate_next.xml VectorDrawable + AppCompatImageView | res/drawable/ic_navigate_next.xml 存在確認。CommandCellViewHolder で AppCompatImageView + setImageResource(R.drawable.ic_navigate_next) 確認済み | 一致 |
| 9-3 RadioCell カスタムチェック | KsSimpleCheckView（2 本線 Canvas 描画）、accent 色着色 | KsSimpleCheckView.kt に SimpleCheck.cs の座標比率（22%/52%→38%/68%、36%/66%→74%/28%）移植確認。RadioCellViewHolder で KsSimpleCheckView 使用・effective.accentColor 設定確認 | 一致 |
| 9-4 SimpleCheckCell 配置確定 | 右側 accessory 30×30dp | SimpleCheckCellViewHolder で accessoryHolder に 30dp 正方形の KsSimpleCheckView を追加確認 | 一致 |
| 9-5 Ripple 背景 | applyCellBackground ヘルパ、全 Cell に RippleDrawable | LabelCellViewHolder.kt に applyCellBackground 実装確認。全 7 ViewHolder で呼び出し確認 | 一致 |
| 9-6 SwitchCell セル全体タップ | container.setOnClickListener で switchView.toggle() | SwitchCellViewHolder.bind 内で container.setOnClickListener → switchView.toggle() 実装確認。reset() で container.setOnClickListener(null) 確認 | 一致 |

#### Theme.cellAccentColor の iOS/Android 対称性

| 項目 | iOS（Theme.swift） | Android（Theme.kt） |
|---|---|---|
| フィールド名 | `cellAccentColor: KsColor` | `cellAccentColor: KsColor` |
| 既定値 | `KsColor(red: 0.0, green: 0.478, blue: 1.0, alpha: 1.0)` | `KsColor(0.0, 0.478, 1.0, 1.0)` |
| ドキュメント | RadioCell/SimpleCheckCell のチェックマーク等の強調色と明記 | 同上 |

iOS と Android で同名フィールド・同値の既定値を持ち、対称性が保たれている。

---

### Coherence

#### design.md との整合

- **Decision 1（クロージャ通知）**: 全 Cell に onValueChanged/onTap/onSelected を持たせ、クロージャで通知。実装と一致。
- **Decision 2（equals/hashCode からクロージャ除外）**: 全 Cell で手動 equals/hashCode 実装でクロージャ除外確認。
- **Decision 8（Theme.Material3.* 必須化）**: AndroidManifest.xml で `@style/Theme.Material3.DayNight.NoActionBar` 確認済み。MaterialSwitch/AppCompatCheckBox 採用確認済み。ClassicSectionDecoration が `onDrawOver` を使用確認済み。
- **Decision 9 追補（Theme.cellAccentColor 追加）**: Android core Theme.kt と iOS Theme.swift 両方に cellAccentColor フィールドを追加済み。既定値 `#007AFF` 相当で対称確認済み。

#### ビルド・テスト結果

- `openspec validate add-cell-types-basic --strict`: **PASS**（Change 'add-cell-types-basic' is valid）
- Android `:ks-settingsview-core:test`: **PASS**（UP-TO-DATE、全件成功）
- Android `:ks-settingsview-ui:test`: **PASS**（UP-TO-DATE、全件成功）
- iOS `swift test`: **PASS**（123 tests, 0 failures）

---

### Issues

**CRITICAL**: なし

**WARNING**: なし

**SUGGESTION**: なし

---

### Final Assessment

すべてのチェックが通過。CRITICAL・WARNING・SUGGESTION いずれもなし。

実機目視タスク（21.5.3/21.5.4/21.7.2〜21.7.6）はアーカイブ前の最終確認として残存しているが、これは仕様・実装の不一致ではなく実機操作を伴う確認作業であり、今回の検証スコープ外。

**判定: VALID**

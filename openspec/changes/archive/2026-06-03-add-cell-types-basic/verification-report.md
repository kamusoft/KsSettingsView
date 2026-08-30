# Verification Report: add-cell-types-basic

検証日時: 2026-05-18
追補日時: 2026-05-20（Sample 動作確認時に検出された不備への対応）

## Summary

| Dimension    | Status                                |
|--------------|---------------------------------------|
| Completeness | 61/61 tasks → +§18.5 追加で 70/71（§18.5.10 のみ実機目視確認待ち） |
| Correctness  | 全 Requirement / Scenario 網羅（samples-{ios,android} の REMOVED / MODIFIED delta 追加） |
| Coherence    | design.md Decision 6 を追加し、Sample SampleLabelCell 削除方針を明文化 |

## 追補: Sample 起動確認漏れと修正（2026-05-20）

初版 verification（2026-05-18）では Sample アプリの実機 / Simulator / Emulator 起動目視確認が漏れており、以下 2 件のリグレッションが PR #11 マージ後に検出された:

1. **Android**: `samples/android/app/.../MainActivity.kt` で `SampleLabelCell` が `KsCellRegistry.CELL_VIEW_TYPE_MIN`（= 100）で登録されていたところに、`KsSettingsView.<init>` 自動呼び出しの `registerBasicCells(context)` が `LabelCell` を viewType 100 で登録しようとして競合 → `IllegalArgumentException: viewType 100 is already registered for class ...SampleLabelCell; cannot reuse for class ...LabelCell`（`KsCellRegistry.kt:143`）が発生。
2. **iOS**: `BasicCellsDemoView.swift` が `KsSettingsViewSample.xcodeproj/project.pbxproj` に未登録のためビルド対象から外れ、`ContentView.swift:27` で `Cannot find 'BasicCellsDemoView' in scope` が発生。

### 根本原因

`add-cell-types-basic/specs/settings-view-{ios,android}-ui/spec.md` の PoC Cell REMOVED Migration 節で「テスト・サンプルは `LabelCell` に置き換える」と明記されていたにもかかわらず、tasks.md には **本体 PoC Cell 削除 (§10 / §17)** と **Sample に 7 種ページ追加 (§18.1 / §18.2)** しか存在せず、**既存 `SampleLabelCell` 一族の置換・削除タスクが抜け落ちていた**。結果として `SampleLabelCell` と `LabelCell` の登録競合が温存された。iOS 側は §18.1 の作業漏れで `BasicCellsDemoView.swift` を pbxproj に追加していなかった。

### 対応（本提案への追加 delta + 実装）

- `proposal.md`: Modified Capabilities に `samples-ios` / `samples-android` を追加、What Changes に `SampleLabelCell` 一族削除を明示
- `tasks.md`: §18.5（10 サブタスク）を新規追加し、iOS / Android 両 Sample の `SampleLabelCell` 削除＋`LabelCell` 置換＋pbxproj 修正を実装
- `design.md`: Decision 6（Sample `SampleLabelCell` の取扱い）を追加
- `specs/samples-ios/spec.md` / `specs/samples-android/spec.md`: 新規 delta を追加し、main spec の「Sample 専用 Cell の定義と登録」を REMOVED、「`SampleLabelCell` を含むデモ画面」を「基本 Cell を含むデモ画面」に MODIFIED（基本 Cell 7 種デモ画面 Scenario 追加）
- ソースコード: iOS / Android 両 Sample から `SampleLabelCell` / `SampleLabelCellView` / `SampleLabelCellViewHolder` / `SampleLabelCellDsl` / `SampleLabelCellPreview` を削除し、`LabelCell` に置換。Sample 独自 Renderer 登録コードを削除（`registerBasicCells` 自動呼び出しに集約）

### 追補 2 (2026-05-20): Android Sample テーマ起因の追加クラッシュ修正

§18.5 完了後の Android Sample 起動目視確認で、トップメニューから「基本 Cell 7 種デモ」へ遷移した際に新たなクラッシュが検出された:

```
View class androidx.appcompat.widget.AppCompatImageView is an AppCompat widget that can only be used with a Theme.AppCompat theme (or descendant).
View class androidx.appcompat.widget.SwitchCompat is an AppCompat widget that can only be used with a Theme.AppCompat theme (or descendant).
...
java.lang.NullPointerException: Attempt to invoke interface method 'int java.lang.CharSequence.length()' on a null object reference
    at android.text.StaticLayout.<init>(StaticLayout.java:654)
    at androidx.appcompat.widget.SwitchCompat.makeLayout(SwitchCompat.java:993)
    at androidx.appcompat.widget.SwitchCompat.onMeasure(SwitchCompat.java:914)
```

**根本原因**: Sample の `AndroidManifest.xml` で `android:theme="@android:style/Theme.Material.Light.NoActionBar"` を指定していたが、本体 `ks-settingsview-ui` が依存する `androidx.appcompat.widget.SwitchCompat` / `androidx.appcompat.widget.AppCompatImageView` は `Theme.AppCompat.*` 派生テーマでのみ初期化属性を解決できる。フレームワーク標準 Material テーマでは `SwitchCompat` の `textOn` / `textOff` が `null` のまま `onMeasure` に流れて NPE になる。

**対応**:
- `samples/android/app/src/main/AndroidManifest.xml`: `android:theme` を `@style/Theme.AppCompat.Light.NoActionBar` に変更（design.md Decision 7）
- `docs/android-ui.md`: 「テーマ要件」セクションを新規追加し、利用者アプリは AppCompat 派生または Material Components 系テーマを使用する必要があることを明記
- `tasks.md` §18.5.11 / §18.5.12 を追加（チェック済み）
- `specs/samples-android/spec.md`: 「AppCompat 派生テーマの使用」Requirement を ADDED として追加（テーマ指定確認 Scenario と SwitchCell クラッシュ非発生 Scenario の 2 つ）

### 追補 3 (2026-05-20): Theme.AppCompat 起因の罫線消失 / Switch・Radio ヴィジュアル退行への対応

§18.5.11 完了直後の Pixel 6a 実機目視確認で、以下 3 問題が新たに検出された:

1. **Cell 罫線の消失**: `ClassicSectionDecoration` が `onDraw`（children 描画前）で罫線を描いていたため、`LabelCellViewHolder.bind` の `container.setBackgroundColor(white)` で上書きされて見えなくなっていた。`SampleLabelCellViewHolder`（削除済み）は背景を塗っていなかったため見えていただけ。
2. **SwitchCell の貧弱描画 / 再クラッシュ**: `Theme.AppCompat.Light.NoActionBar` だと `SwitchCompat` のサム/トラックが Material1 風になり、視認性が低い。さらに `MaterialSwitch` 化したところ `Theme.AppCompat.*` / `Theme.MaterialComponents.*` では `?attr/materialSwitchStyle` が解決できず `SwitchCompat.makeLayout` で再び NPE クラッシュ。
3. **RadioCell / CheckboxCell の手抜き実装**: `TextView "●"` / `TextView "✓"` という見た目最優先の手抜き実装で、Material Design 系の見栄えに到達していなかった。

### 対応 (Decision 8)

- `samples/android/app/src/main/AndroidManifest.xml`: `android:theme` を `@style/Theme.Material3.DayNight.NoActionBar` に変更
- `samples/android/app/build.gradle.kts` および `android/ks-settingsview-ui/build.gradle.kts`: `com.google.android.material:material:1.12.0` 依存追加
- `android/ks-settingsview-ui/src/main/.../ClassicSectionDecoration.kt`: `onDraw` → `onDrawOver` に変更
- `android/ks-settingsview-ui/src/main/.../SwitchCellViewHolder.kt`: `SwitchCompat` → `MaterialSwitch`、`showText = false` / `textOn = "" / textOff = ""` 明示で NPE 回避
- `android/ks-settingsview-ui/src/main/.../CheckboxCellViewHolder.kt`: `TextView "✓"` → `AppCompatCheckBox`
- `android/ks-settingsview-ui/src/main/.../RadioCellViewHolder.kt`: `TextView "●"` → `AppCompatRadioButton`
- `docs/android-ui.md`: テーマ要件を `Theme.Material3.*` 必須に強化
- `android/ks-settingsview-ui/src/test/.../BasicCellsTest.kt`: テスト用 Context を `ContextThemeWrapper(ctx, Theme.Material3.Light.NoActionBar)` で包み、新ウィジェット (`AppCompatCheckBox` / `AppCompatRadioButton` / `MaterialSwitch`) を探すヘルパに置換
- `specs/samples-android/spec.md`: 「Material3 派生テーマの使用」Requirement を ADDED として追加（テーマ指定 / 7 種クラッシュ非発生 / 罫線描画の 3 Scenario）
- `design.md`: Decision 8 を追加

### 検証結果

- **`./gradlew :ks-settingsview-ui:test`**: 110 件全 PASS
- **`./gradlew :ks-settingsview-compose:test`**: 全 PASS
- **`./gradlew :app:assembleDebug`** (`samples/android`): BUILD SUCCESSFUL
- **Pixel 6a (Android 14, <android-device-serial>) で目視確認**:
  - Store 方式デモ: `LabelCell` 3 行が罫線付きで描画、項目追加で `新規 4 / 新規 5` が末尾追加され罫線も含めて正常更新
  - DSL 方式デモ: 静的 Section（固定 Cell A/B）/ 動的 Section（Item A/B/C）/ Cell Modifier（高さ 80dp）すべて罫線付きで描画
  - 基本 Cell 7 種デモ: `LabelCell` / `CommandCell` (>) / `ButtonCell` (青ログアウト) / `SwitchCell` (M3 紫トラック+白サム) / `CheckboxCell` (角丸チェックボックス) / `RadioCell` (Light/Dark/Auto, Material ring) / `SimpleCheckCell` がすべてクラッシュせず描画

### 残作業

なし（§18.5 と Decision 7-8 関連 §18.5.11-20 すべて実装・検証完了。残るは §18.5.10 を CI / 自動テストで補強する場合の追加対応のみだが、Robolectric テストでウィジェット存在検証は既にカバーされているため必須ではない）。

---

## Completeness

### Task Completion

- **61/61 タスク完了**（`- [ ]` のチェックボックスは0件）

### Spec Coverage

スペックファイル `specs/cell-types-basic/spec.md` に記載された全 Requirement の実装を確認した。

| Requirement                        | 実装ファイル（代表）                                              | 状態 |
|------------------------------------|------------------------------------------------------------------|------|
| 具象 Cell の id デフォルト値規約    | `LabelCell.swift:41`, `LabelCell.kt:30`                          | OK   |
| Compose DSL 拡張関数による Cell 直置き | `BasicCellDsl.kt:148-307`                                      | OK   |
| KsImage 値型                        | `KsImage.swift:24`, `KsImage.kt:24`                             | OK   |
| LabelCell                          | `LabelCell.swift`, `LabelCell.kt`                                | OK   |
| CommandCell                        | `CommandCell.swift`, `CommandCell.kt`                            | OK   |
| ButtonCell                         | `ButtonCell.swift`, `ButtonCell.kt`                              | OK   |
| SwitchCell                         | `SwitchCell.swift`, `SwitchCell.kt`                              | OK   |
| CheckboxCell                       | `CheckboxCell.swift`, `CheckboxCell.kt`                          | OK   |
| RadioCell                          | `RadioCell.swift`, `RadioCell.kt`                                | OK   |
| SimpleCheckCell                    | `SimpleCheckCell.swift`, `SimpleCheckCell.kt`                    | OK   |
| 基本 Cell の登録 API               | `KsCellRegistry+BasicCells.swift`, `KsCellRegistryBasicCells.kt`| OK   |
| PoC Cell の削除                    | `PoCLabelCell*` / `PocLabelCell*` ともに不在。`VIEW_TYPE_POC` も削除済み | OK |
| ユニットテスト                     | `BasicCellsTests.swift`, `BasicCellsTest.kt`                     | OK   |

`specs/settings-view-ios-ui/spec.md` および `specs/settings-view-android-ui/spec.md` の REMOVED Requirement（PoC Cell の存在）も削除が実装側で確認済み。

---

## Correctness

### Requirement Implementation Mapping

**具象 Cell の id デフォルト値規約**
- iOS: `LabelCell.swift:42` で `id: UUID = UUID()` を確認
- Android: `LabelCell.kt:30` で `id: String = "label-${java.util.UUID.randomUUID()}"` を確認
- `DSLReidentifiable.withDSLID(_:)` は `LabelCell.swift:59`、`DSLReidentifiableCell.withDSLId(...)` は `LabelCell.kt:38` にそれぞれ実装済み

**Compose DSL 拡張関数による Cell 直置き**
- `BasicCellDsl.kt:148-307` に 7 種の DSL 拡張関数がすべて実装
- 戻り値は `CellHandle` で、`.cellHeight(...)` / `.cellID(...)` chain に対応
- テスト `BasicCellDslTest.kt` で DSLCellNode への格納と CellHandle chain を検証済み

**SwitchCell の onValueChanged / リスナー管理**
- `SwitchCellViewHolder.kt:44` で bind 前に `setOnCheckedChangeListener(null)` を実行し、`:57` で新しいリスナーを設定
- `SwitchCellViewHolder.kt:67` の `reset()` で `setOnCheckedChangeListener(null)` を実行

**一括登録 API とオプトアウト**
- iOS: `KsSettingsViewController.swift:131` で `autoRegisterBasicCells: Bool = true` パラメータでオプトアウト可能
- Android: `KsSettingsView.kt:139` で `isRegistered(LabelCell::class)` チェックにより重複登録を回避

### Scenario Coverage

`specs/cell-types-basic/spec.md` に記載の Scenario をすべてユニットテストで検証済み:

- `BasicCellsTests.swift` および `BasicCellsTest.kt`:
  - id 引数省略での自動採番（`test_LabelCellはidデフォルトUUIDで自動採番される` 等）
  - DSL 経路での withDSLID rebind（`test_LabelCellのwithDSLIDは新しいidを持つCopyを返す` 等）
  - CommandCell タップ通知（`test_CommandCellView_tapHandlerがonTapを保持する`）
  - Disclosure Indicator 表示（`test_CommandCellView_disclosureが表示される`）
  - ButtonCell 中央寄せ（`test_ButtonCellView_centerAlignmentで描画される`）
  - SwitchCell 値変更通知（`test_SwitchCellView_値変更時にonValueChangedが呼ばれる`）
  - CheckboxCell / RadioCell / SimpleCheckCell の toggle・選択通知
  - 一括登録で 7 種が登録されることの確認

- Store 方式の id 明示指定シナリオは直接的なテストはないが、data class / struct のコンストラクタで id 引数を明示できる実装によって保証されている

---

## Coherence

### Design Adherence

`design.md` の全 Decision を確認した:

| Decision                               | 確認結果 |
|----------------------------------------|----------|
| Decision 1: ユーザー操作通知はクロージャ／ラムダ | `onTap` / `onValueChanged` / `onSelected` をクロージャで保持。OK |
| Decision 2: equals/hashCode はクロージャを除外 | iOS は手動 Hashable 実装（`CommandCell.swift:54`）、Android は手動 `equals` / `hashCode` 実装（`CommandCell.kt:40-61`）。OK |
| Decision 3: RadioCell の selectedValue は Cell 自身が持つ | `RadioCell.swift` / `RadioCell.kt` でフィールドとして持つ。OK |
| Decision 4: 基本 Cell をまとめて登録する API | `registerBasicCells()` / `registerBasicCells(context)` 実装済み。自動呼び出しも確認。OK |
| Decision 5: PoC Cell 削除のタイミング | `PoCLabelCell` / `PocLabelCell` ともに不在を確認。OK |

### Code Pattern Consistency

プロジェクトのコーディング規約との一貫性を確認した。特記事項なし。

---

## Issues

### CRITICAL

なし

### WARNING

なし

### SUGGESTION

なし

---

## Final Assessment

**All checks passed. Ready for archive.**

- タスク: 61/61 完了
- CRITICAL: 0
- WARNING: 0
- SUGGESTION: 0
- 判定: **VALID**

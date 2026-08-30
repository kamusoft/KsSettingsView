# レビュー結果 - purify-core-extract-style-to-ui-layer (#005)

**レビュー日時**: 2026年06月07日
**レビュワー**: sdd-reviewer
**変更提案ID**: purify-core-extract-style-to-ui-layer
**レビュー対象**: Phase 10 / 11 における他 active 変更提案 6 件への **直接修正** と purify 本体 tasks.md の更新

レビュー範囲（git diff `openspec/changes/` 配下、16 ファイル、+166/-45 行）:

1. `openspec/changes/add-maui-bridge/` (proposal.md / design.md / specs/maui-bridge/spec.md / tasks.md)
2. `openspec/changes/add-maui-cells/` (proposal.md / specs/maui-bridge/spec.md / specs/maui-cells/spec.md / tasks.md)
3. `openspec/changes/add-maui-core/proposal.md`
4. `openspec/changes/add-samples-maui/proposal.md`
5. `openspec/changes/add-cell-types-input/` (proposal.md / specs/cell-types-input/spec.md / tasks.md)
6. `openspec/changes/add-cell-types-custom/` (proposal.md / specs/cell-types-custom/spec.md / tasks.md)
7. `openspec/changes/purify-core-extract-style-to-ui-layer/tasks.md` (Phase 10 / 11 セクション)

---

## サマリー

ユーザー指示「他の active 変更提案 6 件を **直接修正**」を受け、本提案の新方針 (`SettingsRoot.theme` 削除 / `KsCell` 抽象から `style` 要求削除 / `Theme` `CellStyle` `KsImage` `DSLStyleModifiable` の UI 層配置 / `KsColor` `KsFont` 削除 / Native 型直接保持) を 6 active 提案の spec 段階で整合させる作業をレビューした。

### 主な確認事項

#### A. add-maui-bridge

- `KsSettingsRootDiffDTO` 階層から `KsSettingsRootDiffUpdateThemeDTO` を取り除き、サブクラス数を 11 → 10 に更新（spec.md L282-311、tasks.md 2.5.2 / 2.5.6）。`#### Scenario: DTO の階層構造` で「`KsSettingsRootDiffUpdateThemeDTO` は存在しない」を明示。
- `Bridge Controller / View API` Requirement に `setTheme(_ theme: KsThemeDTO)` 単独 API を追加（spec.md L94、Scenario 「setTheme による Theme 適用」/「setTheme は Diff Publisher を発行しない」を追加）。Bridge 内部で `SettingsRootStore.applyTheme(_:)` 経由で `@Published var theme` / `StateFlow<Theme>` に書き込み、Diff Publisher 不発行であることを spec レベルで保証。
- `KsThemeDTO 型と Native Theme への変換` Requirement を新規追加（spec.md L331-371）：
  - MAUI `Microsoft.Maui.Graphics.Color` → Native (`UIColor` / Compose `Color`) を **1 段直接変換** すべきこと (MUST)
  - 中間表現として旧 `KsColor` 相当の Double-based RGBA 構造を経由してはならないこと (MUST NOT)
  - `KsColorDTO` / `KsFontDTO` のような独自中間 DTO 型を導入してはならないこと (MUST NOT)
  - 上記を iOS / Android それぞれの Scenario で具体的な数値（`Color(red: 0.9, ...)` → `UIColor(red: 0.9, ...)` / Compose `Color(red = 0.9f, ...)`) まで明示
- `Builder` には `setTheme` メソッドを持たないこと（MUST NOT）を spec.md L77-81 で明示。
- tasks.md に `2.6. KsThemeDTO の実装と setTheme 経路` セクションを新設（iOS / Android 各 3 タスク、合計 6 タスク）。Bridge ユニットテストにも `6.5.1` で `setTheme` 検証タスクを追加し、`MAUI Color → UIColor の 1 段直接変換結果も assert` を明文化。
- design.md にも `purify-core-extract-style-to-ui-layer` 整合追記済み（L58 / L64 / L68 / L75）。

#### B. add-maui-cells

- MODIFIED `Bridge Builder API` Requirement に `Color パラメータ ... Bridge 内部で Native 型 ... に 1 段直接変換` 規約と「`KsColorDTO` 等の独自 Color DTO 型を導入してはならない (MUST NOT)」を追加（specs/maui-bridge/spec.md L15）。
- `addButtonCell の titleColor は MAUI Color → Native Color 直接変換` および `addSwitchCell / addCheckboxCell の accentColor も同様` の 2 Scenario を新規追加（spec.md L57-67）。
- `13 Cell の Handler 実装` Requirement に「Handler 側で `KsColor` 等の Core 経由中間変換は行わない (MUST NOT)」を追加（specs/maui-cells/spec.md L47）。
- tasks.md `1.1〜2.x` で各 `addXxxCell` メソッドに `Color 引数は MAUI Color → UIColor/Compose Color 1 段変換` の追記、節リード文として `purify-core-extract-style-to-ui-layer` 追随の見出しを追加。
- proposal.md にも整合追記済み。

#### C. add-maui-core / add-samples-maui

- 両 proposal.md に `purify-core-extract-style-to-ui-layer` 整合 note を追記。
  - add-maui-core: 本提案は LabelCell のみで Color プロパティを持たず Theme 経路は扱わない、`SettingsView.Theme` BindableProperty も導入しない、Theme BindableProperty が必要なら `add-maui-cells` で Handler PropertyMapper から `Bridge.SetTheme(themeDTO)` を呼ぶ経路として追加することを明示。
  - add-samples-maui: 本提案では Theme 構築サンプル・Color プロパティ操作を扱わない、MAUI Color → Native Color の検証は `add-maui-cells` Sample で扱う旨を明示。
- 両提案を `grep -rn "KsColor"` でクロスチェックしたところ、`KsColor` 直接言及はなく、整合性は spec レベルで保たれている。

#### D. add-cell-types-input

- proposal.md に `purify-core-extract-style-to-ui-layer` 追随を明記。`Cell` 抽象から `style: CellStyle` 要求が削除されたため、各入力 Cell は **個別に** `style: CellStyle` プロパティを持つ (任意プロパティ、`DSLStyleModifiable` / `DSLStyleModifiableCell` 準拠手段) ことを明示。依存に `purify-core-extract-style-to-ui-layer` を追加。
- specs/cell-types-input/spec.md L5 に新方針の核心要件を集約。`CellStyle` が UI 層所属で `UIColor?` / `UIFont?` ／ Compose `Color?` / `TextStyle?` を直接保持する型である旨を明示。
- tasks.md 1.5.3 / 1.5.4 で「`DSLStyleModifiableCell` および `CellStyle` は **UI 層** に移動済み、UI 層内 import で循環依存にならない」モジュール依存ガイドを追加。
- 同じく tasks.md 依存欄に `purify-core-extract-style-to-ui-layer` を追加。

#### E. add-cell-types-custom

- proposal.md / spec.md / tasks.md を同様に更新。
- Android `CustomCell` data class シグネチャから `override val style` を `val style`（任意プロパティ）に変更：tasks.md L36 で `data class CustomCell<Content : Any>(override val id: String = ..., val style: CellStyle = CellStyle(), val content: Content) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell` の形に整理。`override val style` から `val style` への変更により、`Cell` 抽象に `style` 要求がない新方針と整合する。
- 「`DSLStyleModifiableCell` interface および `CellStyle` 型 ... は **UI 層 (`ks-settingsview-ui`)** に移動済み」のモジュール依存ガイドを追加。
- 依存欄に `purify-core-extract-style-to-ui-layer` を追加。

#### F. purify 本体 tasks.md

- Phase 10 / 11 のセクション見出しが `（依頼）` から `（直接修正実施）` に変更。10.1〜10.7 / 11.1〜11.3 の全タスクが `[x]` チェック済みで、注記も「実際の修正実施」を反映する文面（修正内容のスナップショット、`openspec validate --strict` valid 確認結果）に書き換えられている。
- 完了条件 L158 に「後続 active MAUI 提案 4 件および cell-types-input / cell-types-custom への整合修正が **本セッションで直接実施済み**（依頼ではなく実装完了）」を `[x]` で追加。

### `openspec validate --strict` 結果

```
openspec validate add-maui-bridge --strict          → valid
openspec validate add-maui-cells --strict           → valid
openspec validate add-maui-core --strict            → valid
openspec validate add-samples-maui --strict        → valid
openspec validate add-cell-types-input --strict    → valid
openspec validate add-cell-types-custom --strict   → valid
openspec validate purify-core-extract-style-to-ui-layer --strict → valid
```

すべて valid。delta 文法（ADDED / MODIFIED / REMOVED Requirement の使い分け、`#### Scenario:` の必須化、`### Requirement:` の MUST / SHALL 記述）は遵守されている。

### テスト実行結果

```
cd android && ./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test
→ BUILD SUCCESSFUL in 3s (166 actionable tasks, UP-TO-DATE)

cd ios && swift test --quiet
→ All tests: 83 tests, 0 failures
```

### 仕様整合性の確認

| 検証観点 | 結果 |
|---|---|
| `SettingsRoot.theme` 削除に伴う Bridge 側 `setTheme` 独立 API 化 | 完了。`Bridge Controller / View API` Requirement に独立 API として明記、Diff 階層から `UpdateThemeDTO` 削除 |
| Diff 階層が 10 ケース（`UpdateThemeDTO` 含まず） | 完了。spec.md / tasks.md 双方で 10 サブクラスに統一 |
| MAUI Color → Native 型の 1 段直接変換 | 完了。MUST 規定 + iOS / Android 個別 Scenario 付き |
| `KsColorDTO` / `KsFontDTO` 非導入 | 完了。MUST NOT で明記 |
| Bridge → `SettingsRootStore.applyTheme(_:)` 経路（Diff Publisher 不発行） | 完了。「setTheme は Diff Publisher を発行しない」Scenario で保証 |
| `KsCell` / `Cell` 抽象から `style` 要求削除に追随 | 完了。cell-types-input / custom の spec で「個別任意プロパティとして保持」明記、Android CustomCell から `override val style` 撤去 |
| `DSLStyleModifiable` / `DSLStyleModifiableCell` / `CellStyle` の UI 層所属 | 完了。両 cell-types proposal/spec/tasks で明記、モジュール依存ガイド付き |
| Native 型直接保持（`UIColor?` / `UIFont?` ／ Compose `Color?` / `TextStyle?`） | 完了。両 cell-types spec / DSL 拡張関数シグネチャで明示 |
| archive 済み提案・openspec/specs 配下への影響 | なし（`git status` で確認、`openspec/specs/` および `archive/` 配下は無変更） |
| 既に `[x]` のタスクの書き換え | 該当箇所なし（Phase 10/11 の新規 `[x]` 化と完了条件 1 行追加のみ） |

**ステータス**: `APPROVED`

---

## 指摘事項

### Critical: なし

### Major: なし

### 🟡 Minor

#### Minor 1: purify 本体 tasks.md の「完了条件」3 行が iOS-only セッション時の文面のまま残っている

**該当箇所**: `openspec/changes/purify-core-extract-style-to-ui-layer/tasks.md:152-154`

**現状**:
```markdown
- [ ] すべての Phase のチェックリストが完了している（**iOS 関連は完了。Android 関連は別セッションで実施予定**）
- [ ] iOS `swift test` / Android `./gradlew test` が全テスト緑（**iOS 側は緑、Android 側は未対応**）
- [ ] iOS / Android Sample の基本 Cell 7 種デモ画面が新 API で正常表示される（**iOS 側はビルド成功**）
```

**問題点**:
本提案の本体タスク Phase 1〜12 はすべて `[x]` で、特に Phase 12.2 では `./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test` が緑（Round 3 / 4 でテスト追加 + flaky 対策まで完遂、本セッション再確認でも `166 actionable tasks BUILD SUCCESSFUL`）。Phase 9.6 でも `Android Sample も ./gradlew assembleDebug 成功を確認` 済み。

つまり 152 / 153 の「Android 関連は別セッション」「Android 側は未対応」という annotation は**事実と矛盾**しており、本来 `[x]` 化または注釈の更新が必要。154 については「目視確認は別途」が依然 true なため `[ ]` 維持は妥当だが、annotation の `iOS 側はビルド成功` は `iOS / Android 両方ビルド成功` に拡張するのが正確。

加えて L160-168 の「iOS 完了後 / Android 着手前の注記」セクションも、現状の達成度（Android 側も完遂）と矛盾するため整理推奨。

**推奨修正**:
```markdown
- [x] すべての Phase のチェックリストが完了している
- [x] iOS `swift test` / Android `./gradlew test` が全テスト緑（iOS: 83 tests / Android: 166 actionable tasks BUILD SUCCESSFUL）
- [ ] iOS / Android Sample の基本 Cell 7 種デモ画面が新 API で正常表示される（**iOS / Android 両方ビルド成功確認済み、シミュレータ / エミュレータ目視確認は別途実施**）
```

L160-168 の「iOS 完了後 / Android 着手前の注記」セクションは「実装完了サマリ（iOS / Android 両プラットフォーム達成）」に置き換えるか、削除して PR 本文に統合する。

これは事実関係の整合性問題で、archive 時の `openspec verify` でユーザーが「未完了タスクあり」と誤認する可能性があるため Minor として指摘する。

#### Minor 2: add-cell-types-input の「style 引数の型」記述で `(style: CellStyle = CellStyle())` のデフォルト値が iOS / Android で実体ある定義に依存している

**該当箇所**: `openspec/changes/add-cell-types-input/specs/cell-types-input/spec.md:28`, `openspec/changes/add-cell-types-input/tasks.md:24-30`

**問題点**:
DSL 拡張関数シグネチャ例で `style: CellStyle = CellStyle()` と書かれており、本提案 (`add-cell-types-input`) は `purify-core-extract-style-to-ui-layer` の UI 層 `CellStyle` 型に依存している。purify 提案で `CellStyle` のデフォルトコンストラクタ（全フィールド `nil` / `null` 持ち）が新型でも保証されていることを前提としている。

UI 層 `CellStyle.kt` / `CellStyle.swift` を確認したところ、いずれも `data class CellStyle(backgroundColor: Color? = null, ...)` / `public struct CellStyle { public init(backgroundColor: UIColor? = nil, ...) }` の形でデフォルトコンストラクタを持っているため、依存関係は満たされている。だがこの依存関係は spec 上 implicit になっており、`add-cell-types-input` が `purify-core-extract-style-to-ui-layer` の archive 後に着手される場合に「`CellStyle()` がパラメータレスで呼べる」契約が依存仕様 (`settings-view-android-style` / `settings-view-ios-style`) の Requirement として spec.md に明文化されているか確認しておくと安全。

**推奨修正**:
purify-core 側の `settings-view-{ios,android}-style` capability の `CellStyle` Requirement に「全フィールドにデフォルト値 (`nil` / `null`) を持ち、`CellStyle()` で空のスタイルを構築できる (SHALL)」を含めるか、cell-types-input 側で「`CellStyle` のデフォルトコンストラクタが `purify-core-extract-style-to-ui-layer` で公開されていることに依存する」旨の note を追加する。

軽微な Minor だが、後続提案実装時の手戻り防止として推奨。

#### Minor 3: add-cell-types-custom の Android `CustomCell` 定義が `: Cell, DSLReidentifiableCell, DSLStyleModifiableCell` のままで、`DSLStyleModifiableCell` の UI 層所属を spec で改めて検証する Scenario がない

**該当箇所**: `openspec/changes/add-cell-types-custom/tasks.md:36-39`, `openspec/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:5,29`

**問題点**:
tasks.md L36 では `data class CustomCell<...>(...) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell` と宣言し、L39 で「`DSLStyleModifiableCell` interface ... は UI 層 (`ks-settingsview-ui`) に移動済み」と明記している。spec.md L5 でも「`DSLStyleModifiable` / `DSLStyleModifiableCell` 規約も UI 層に再配置されている」と明示。

しかし spec.md には「Android `CustomCell` は **UI 層** の `DSLStyleModifiableCell` を implement することを検証する」明示的な Scenario がない。`Cell` 抽象から `style` 要求が消えた前提のもとで `DSLStyleModifiableCell.withDSLStyle(...)` が data class copy で実装されることを spec で保証する Scenario があると、後続実装が「Core 版 `DSLStyleModifiableCell` を誤 import する」リグレッションを防げる。

**推奨修正**:
spec.md の `### Requirement: CustomCell（Android）` セクションに以下の Scenario を追加：

```markdown
#### Scenario: DSLStyleModifiableCell の UI 層 import

- **GIVEN** Android `CustomCell.kt` の import 文
- **WHEN** `DSLStyleModifiableCell` の import 元を確認する
- **THEN** `ks-settingsview-ui` (UI 層) の `DSLStyleModifiableCell` を import している（`ks-settingsview-core` (Core 層) のものではない、`purify-core-extract-style-to-ui-layer` で UI 層に移動済みのため）
```

これは強い指摘ではなく Minor 留め。後続実装時に IDE 補完で間違って Core 版を import するリスク回避用。

---

### 🔵 Suggestion

#### Suggestion 1: add-maui-bridge tasks.md `6.5.1` のタスク番号体系

**該当箇所**: `openspec/changes/add-maui-bridge/tasks.md:110`

**問題点**:
新規追加された Bridge ユニットテストタスクが `6.5.1` という番号を持つが、既存タスクは `6.5` の直下にサブタスクを持つ書き方ではない。`6.6` 以降を再連番化するか、`6.5.1` の意図（`6.5` の rootHeader テストの直後で追加する位置）を明示するコメントがあると保守しやすい。

軽微な書き方の話で、validate には影響しない。

**推奨修正（任意）**:
`6.5.1` を `6.5b` または `6.5-Theme` に改名し、節題に「（Theme 経路、setTheme 検証）」を併記する。

#### Suggestion 2: design.md のフォロー追記範囲

**該当箇所**: `openspec/changes/add-maui-bridge/design.md`

design.md には今回 4 箇所 `purify-core-extract-style-to-ui-layer` 整合追記が入っている。本提案完了時のサマリで「design.md にも整合追記済み」点を強調しておくと、後続実装者が「設計判断の理由」を spec ではなく design.md で追えるようになる。本提案 spec のみ追えば実装可能だが、design context を求める読者向けの追加配慮として記載しておくと良い。

任意の改善。

---

## 検証した観点と結果

| 観点 | 結果 |
| --- | --- |
| `openspec/specs` / `openspec/changes/archive/` への書き換え禁止遵守 | 遵守。`git status` で当該配下は無変更 |
| `openspec/changes/{変更提案ID}/` の内容に反する指摘の禁止 | 遵守。本提案の方針（`Theme` UI 層移動 / `KsColor` 削除 / Native 型直接保持 / `Cell` 抽象から `style` 要求削除）に基づく整合確認のみを行い、それらに反する指摘はしていない |
| 各 active 提案の `openspec validate --strict` | 6 / 6 すべて valid |
| spec.md delta 文法（ADDED / MODIFIED / REMOVED Requirement、`#### Scenario:` 必須） | 全 6 提案で遵守 |
| 既に `[x]` のタスクが書き換わっていないか | 該当なし |
| プロダクトコードへの影響 | なし（本セッションは active 提案アーティファクトの修正のみで、`ios/` `android/` `samples/` 配下のプロダクトコードは無変更） |
| Phase 10/11 全 10 タスクの完了 | 完了。10.1〜10.7 と 11.1〜11.3 すべて `[x]`、注記は「実際の修正実施」基準で書き換え済み |
| ビルド・テスト緑 | iOS `swift test` 83 tests 緑 / Android `./gradlew :ks-settingsview-{core,ui,compose}:test` BUILD SUCCESSFUL（166 actionable tasks） |
| `SettingsRoot.theme` 削除に対する Bridge 整合 | `setTheme` 独立 API として整理、Diff 階層から `UpdateThemeDTO` 削除、`SettingsRootStore.applyTheme(_:)` 経由（Diff Publisher 不発行） |
| MAUI Color → Native 型 1 段直接変換 | spec で MUST 規定 + iOS / Android Scenario 付き、`KsColorDTO` / `KsFontDTO` 非導入を MUST NOT で明記 |
| `Cell` 抽象から `style` 要求削除に追随 | cell-types-input / custom 双方で「個別任意プロパティとして保持」明記、Android CustomCell `override val style` → `val style` に修正 |
| `DSLStyleModifiable` / `CellStyle` UI 層配置の明示 | 両 cell-types 提案で明示、モジュール依存ガイド付き |
| 多言語対応 | 該当なし（本セッションは仕様文書修正のみ） |
| テスト容易性 / セキュリティ / パフォーマンス | 該当なし（仕様文書修正のため） |
| 既存テストへの影響（リグレッション） | なし（iOS / Android 両プラットフォームでテスト緑、本変更が Native コードに影響しないため） |

---

## アクションプラン

優先度順:

1. **[Minor 1 / purify 本体 tasks.md]** L152-154 の完了条件と L160-168 の「iOS 完了後 / Android 着手前の注記」セクションを、Android 側も完遂済みの現状に合わせて更新。具体的には:
   - 152: `[x]`, annotation 削除
   - 153: `[x]`, annotation 「iOS: 83 tests / Android: 166 actionable tasks BUILD SUCCESSFUL」
   - 154: `[ ]` 維持, annotation 「iOS / Android 両方ビルド成功、目視確認は別途」
   - L160-168: 「実装完了サマリ」に書き換えるか PR 説明に統合
2. **[Minor 2 / cell-types-input]** purify-core 側の `CellStyle` Requirement に「デフォルトコンストラクタで空構築可能 (SHALL)」を追加するか、cell-types-input 側で依存 note を追加。
3. **[Minor 3 / cell-types-custom]** Android `CustomCell` の `DSLStyleModifiableCell` UI 層 import を検証する Scenario を spec.md に追加。
4. **[Suggestion 1, 2 / 任意]** Bridge tasks.md タスク番号体系の改善、design.md 整合追記の強調表示。

いずれも Critical / Major ではなく、本提案の archive を保留する要件ではない。Minor 1 のみ archive 前に修正することを推奨（archive 時の `openspec verify` での誤検知防止）。

---

## 判定結果

**ステータス**: `APPROVED`

### APPROVED の根拠

1. ユーザー指示「6 active 提案の **直接修正**」を漏れなく完遂しており、各提案の proposal.md / spec.md / tasks.md / 必要に応じて design.md にわたって、本提案の新方針への整合修正が一貫した文面で施されている。
2. spec 上の Requirement 追加・MODIFIED は delta 文法に従っており、`openspec validate --strict` が 6 / 6 valid。
3. MAUI Bridge の Theme 経路は `setTheme` 独立 API（`KsThemeDTO` 経由、`SettingsRootStore.applyTheme(_:)` 呼び出し、Diff Publisher 不発行）として spec レベルで完全に整理されている。`KsSettingsRootDiffDTO` 階層から `UpdateThemeDTO` 削除（10 サブクラス）、`KsColorDTO` / `KsFontDTO` 非導入の MUST NOT 規定、MAUI Color → Native 型 1 段直接変換の MUST 規定が一貫して spec / tasks / proposal / design に反映されている。
4. `Cell` 抽象から `style` 要求削除に対し、cell-types-input / custom が「個別任意プロパティとして保持」する形で整合済み。Android `CustomCell` も `override val style` → `val style` に修正済み。
5. `DSLStyleModifiable` / `DSLStyleModifiableCell` / `CellStyle` の UI 層所属がモジュール依存ガイド付きで明示されており、後続実装時の循環依存リスクを防ぐ説明も含まれている。
6. iOS / Android テストはすべて緑（iOS 83 / Android 166 actionable tasks BUILD SUCCESSFUL）。
7. archive 済み提案、`openspec/specs/`、本提案以外のプロダクトコードに影響なし。
8. 残る指摘事項はすべて Minor / Suggestion のレベル（事実と矛盾する古い annotation の更新、Scenario の追加、依存関係の明文化など）で、本提案 archive の停止要件ではない。

Round 3 / 4 で APPROVED 判定済みの本提案について、本セッションで実施された Phase 10 / 11 の直接修正も問題なく完遂されている。Minor 1（完了条件文言の事実整合）のみ archive 前の最終調整として対応推奨だが、判定は **APPROVED**。本提案は **アーカイブ可能** な品質に達している。

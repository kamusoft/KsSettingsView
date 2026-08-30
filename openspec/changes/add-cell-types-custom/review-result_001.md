# 変更提案レビュー結果 - add-cell-types-custom（連動レビュー含む）

**レビュー日時**: 2026年05月07日
**レビュワー**: sdd-spec-reviewer
**判定**: CHANGES_REQUESTED

---

## サマリー

4 提案すべてについて OpenSpec CLI バリデーションはエラー 0 件で通過。tasks.md の禁止事項（git 操作・openspec/specs/ 編集指示）も検出なし。ただし `add-cell-types-custom` において **Modified Capabilities として宣言した `settings-view-ios-ui` / `settings-view-android-ui` に対応するデルタスペックが存在しない** という規約違反を確認した。その他、軽微な警告事項が 1 件ある。

---

## OpenSpec CLI バリデーション

| 変更提案 | 結果 |
|---|---|
| `add-settings-view-core` | エラー 0 件（PASSED） |
| `add-settings-view-ios-ui` | エラー 0 件（PASSED） |
| `add-settings-view-android-ui` | エラー 0 件（PASSED） |
| `add-cell-types-custom` | エラー 0 件（PASSED） |

---

## 🚨 重大違反（CRITICAL）

### OpenSpec 規約違反：Modified Capabilities に対するデルタスペック欠落

**対象提案**: `add-cell-types-custom`

**ファイル**: `openspec/changes/add-cell-types-custom/proposal.md`
**該当箇所**: L33–35（Modified Capabilities セクション）

```
### Modified Capabilities
- `settings-view-ios-ui`: ヘッダ／フッタ supplementary registration の `SectionAccessory.custom` 経路を、空フォールバックから `UIHostingConfiguration` ベースの本実装に置き換える
- `settings-view-android-ui`: ヘッダ／フッタ ViewHolder の `SectionAccessory.Custom` 経路を、最小高さプレースホルダから `ComposeView` ベースの本実装に置き換える
```

**問題**: `settings-view-ios-ui` と `settings-view-android-ui` を Modified Capabilities として宣言しているが、対応するデルタスペック（`openspec/changes/add-cell-types-custom/specs/settings-view-ios-ui/spec.md` および `openspec/changes/add-cell-types-custom/specs/settings-view-android-ui/spec.md`）が存在しない。

**参照パターン**: `add-cell-types-basic` では同じく Modified Capabilities として宣言した `settings-view-ios-ui` / `settings-view-android-ui` に対して、それぞれ `openspec/changes/add-cell-types-basic/specs/settings-view-ios-ui/spec.md`（REMOVED Requirements）と `openspec/changes/add-cell-types-basic/specs/settings-view-android-ui/spec.md`（REMOVED Requirements）を正しく用意している。

**影響を受けるスペック**:
- `openspec/changes/add-settings-view-ios-ui/specs/settings-view-ios-ui/spec.md` の `Requirement: SectionAccessory の Phase 1 対応範囲`（L63–78）が本変更提案によって MODIFIED になるはずだが、MODIFIED デルタスペックに変更後の全文が記載されていない。
- `openspec/changes/add-settings-view-android-ui/specs/settings-view-android-ui/spec.md` の `Requirement: SectionAccessory の Phase 1 対応範囲`（L79–93）が同様に MODIFIED になるはずだが、同じく記載なし。

**修正方法**: 以下の 2 ファイルを新規作成し、MODIFIED Requirements として変更後の全文（`.custom` 経路が本実装になった後の Requirement 全文 + Scenario）を記載すること。

- `openspec/changes/add-cell-types-custom/specs/settings-view-ios-ui/spec.md`
- `openspec/changes/add-cell-types-custom/specs/settings-view-android-ui/spec.md`

---

## ⚠️ 警告（WARNING）

### iOS SwiftUI ラッパのスタイル指定テストが tasks.md に明示されていない

**対象提案**: `add-settings-view-ios-ui`

**ファイル**: `openspec/changes/add-settings-view-ios-ui/tasks.md`

**問題**: `specs/settings-view-ios-ui/spec.md` の `Requirement: スタイル切替（クラシック/モダン）` に `Scenario: SwiftUI ラッパでのスタイル指定`（L57–60）がある。このシナリオは「`KsSettingsView(root: $root, style: .modern)` の `makeUIViewController` で生成された controller の `style` が `.modern` で初期化されること」を検証するものだが、tasks.md の `8.6 KsSettingsViewStyleTests.swift` では動的切替と Appearance の検証は明示されているものの、SwiftUI ラッパ経由での `style` 初期化検証が含まれていない。タスク `9.1 KsSettingsViewRepresentableTests.swift` でも `controller.style` の初期化確認が明示されていない。

**修正方法**: タスク `8.6` または `9.1` に「`KsSettingsView(root: $root, style: .modern)` の `makeUIViewController` で生成された controller の `style` が `.modern` であることを検証する」旨を追記すること（軽微な追記）。

---

## ✅ 合格項目

### add-settings-view-core

- proposal.md: Why / What Changes / Impact セクションが揃っている
- SectionAccessory 型の追加（`.text(String)` / `.custom(AnyCell)` の sum type）が spec.md に ADDED Requirements として正しく記載されている
- spec.md 全要件に GIVEN/WHEN/THEN 形式の Scenario が揃っている
- SHALL / MUST / MUST NOT 規範語の使用が適切
- tasks.md セクション 8（SectionAccessory 化再修正）が未チェック（`[ ]`）のタスクとして追加されており、既存チェック済みタスク（`[x]`）の変更指示ではなく追加タスクとして正しく扱われている
- git 操作・openspec/specs/ 編集指示なし
- Android Section.kt の旧実装（`String?`）と spec.md の新定義（`SectionAccessory?`）の乖離は、タスク 8 で明示的に改修計画が立てられており意図的な状態
- 日本語で一貫して記述されている

### add-settings-view-ios-ui

- proposal.md の Why / What Changes / Impact が整合している
- KsSettingsViewStyle enum（`.classic` / `.modern`）の要件と Scenario が spec.md に揃っている
- SectionAccessory.Phase 1 対応範囲 Requirement でクラッシュ防止（MUST NOT）が明示されている
- tasks.md でスタイル切替実装（タスク 4.1〜4.3）およびテスト（タスク 8.6）が追加されている
- SwiftUI ラッパ（KsSettingsViewRepresentable）でも `style` 引数を受け取る設計が tasks.md タスク 6.2 に反映されている
- git 操作・openspec/specs/ 編集指示なし
- 日本語で一貫して記述されている

### add-settings-view-android-ui

- proposal.md の Why / What Changes / Impact が整合している
- KsSettingsViewStyle enum（`Classic` / `Modern`）の要件と Scenario が spec.md に揃っている
- ItemDecoration 切替（`ClassicSectionDecoration` / `ModernSectionDecoration`）が tasks.md タスク 5b で具体的に記述されている
- SectionAccessory.Phase 1 対応範囲 Requirement でクラッシュ防止（MUST NOT）が明示されている
- Compose ラッパでのスタイル指定 Scenario が tasks.md タスク 8.1 で反映されている
- git 操作・openspec/specs/ 編集指示なし
- 日本語で一貫して記述されている

### add-cell-types-custom（デルタスペック欠落を除く部分）

- `cell-types-custom` 新規 Capability の spec.md は ADDED Requirements のみで構成されており、既存スペックの全文コピーや ADDED/MODIFIED/REMOVED/RENAMED 外への記載はない
- 全 Requirement に GIVEN/WHEN/THEN 形式の Scenario が揃っている
- UIHostingConfiguration の手動 UIHostingController 埋め込み禁止（MUST NOT）が spec.md に明記されている
- ヘッダ／フッタ任意 View 化の Requirement（`SectionAccessory.custom のヘッダ／フッタ描画`）が iOS / Android 双方に揃っている
- 未登録 Content 型へのフォールバック動作（DEBUG: assertion failure / RELEASE: 空表示）が spec.md に記載されている
- tasks.md の `10b` / `10c` / `10d` セクションでヘッダ／フッタ任意 View の実装・テスト計画が揃っている
- git 操作・openspec/specs/ 編集指示なし
- 日本語で一貫して記述されている

### 横断的整合性

- `add-settings-view-core` の `SectionAccessory?` 定義 → `add-settings-view-ios-ui` / `add-settings-view-android-ui` の Phase 1 フォールバック実装 → `add-cell-types-custom` の本実装という責務分離の流れが 4 提案間で一貫している
- 「Phase 1 で `.text` のみ、`.custom` は cell-types-custom で本実装」という記述が ios-ui / android-ui の spec.md と proposal.md に明示されており齟齬がない
- MAUI バインディング可能性のため Native View ベースを双方共通とする方針が各提案の design.md に記載されている

---

## 💡 改善提案（SUGGESTION）

### add-settings-view-core: tasks.md セクション 5.6 と spec.md の現行の見た目上の矛盾について

タスク 5.6（チェック済み）には `data class Section(val id: String, val header: String?, ...)` と記載されているが、spec.md には `header: SectionAccessory?` と定義されている。これはタスク 8 で修正予定であることは明示されているが、チェック済みタスクを読むと実装済み内容と spec.md の間に矛盾があるように見える。今後のレビューや実装者の混乱を防ぐため、タスク 5.6 のコメントに「→ タスク 8.7 で SectionAccessory 型に変更予定」等の注釈を添えると分かりやすい（必須ではない）。

---

## アクションプラン

### 最優先（CHANGES_REQUESTED の理由）

1. `add-cell-types-custom` に以下の 2 ファイルを新規作成する：
   - `openspec/changes/add-cell-types-custom/specs/settings-view-ios-ui/spec.md`（MODIFIED Requirements: `SectionAccessory の Phase 1 対応範囲` の変更後全文 + Scenario を記載）
   - `openspec/changes/add-cell-types-custom/specs/settings-view-android-ui/spec.md`（同上）

### 推奨対応

2. `add-settings-view-ios-ui` の tasks.md タスク `8.6` または `9.1` に SwiftUI ラッパ経由での `style` 初期化検証を明示的に追記する。

### 任意対応

3. `add-settings-view-core` の tasks.md タスク 5.6 に「→ タスク 8.7 で変更予定」の注釈を追記する。

---

## 判定理由

`add-cell-types-custom` において、Modified Capabilities として `settings-view-ios-ui` / `settings-view-android-ui` を宣言しているにもかかわらず、それぞれの MODIFIED Requirements を記述したデルタスペックが存在しない。これは OpenSpec のデルタスペック規約（Modified Capabilities には対応する `specs/<capability>/spec.md` が必要）への違反であり、CHANGES_REQUESTED と判定する。デルタスペック 2 ファイルの追加により解消できる問題であり、4 提案の設計方針・整合性・禁止事項への対応は全体として適切である。

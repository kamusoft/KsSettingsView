# レビュー結果 - add-partial-update-core (review-result_002)

**レビュー日時**: 2026年05月13日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-partial-update-core
**対象範囲**: review-result_001.md の Suggestion 3 件への追加対応のみ

## サマリー

review-result_001.md（APPROVED）に記載された 3 件の Suggestion に対する追加対応のレビュー。

| 対応項目 | 内容 | 結果 |
|----------|------|------|
| Suggestion 1 | `SettingsRootDiff.replaceCell` / `ReplaceCell` の identity 一致責務をコメント化 | 実装済（iOS / Android 両方） |
| Suggestion 2 | `KsCellID.swift` 冒頭に iOS / Android Diff identity 非対称性を追記 | 実装済 |
| Suggestion 3 | Section 周辺ファイルの spec 参照パスを最新 spec / archive 配下に更新 | 実装済 |

### 検証結果

| 観点 | 結果 |
|------|------|
| `swift build` | 成功（Build complete!） |
| `swift test` | 78 件すべて成功（0 failures） |
| `./gradlew :ks-settingsview-core:build` | BUILD SUCCESSFUL |
| `./gradlew :ks-settingsview-core:test --rerun-tasks` | BUILD SUCCESSFUL |
| 参照されている spec / archive パスの実在確認 | `openspec/specs/settings-view-core/spec.md` / `openspec/changes/archive/2026-05-09-refactor-accessory-and-root-hf/design.md` ともに存在 |
| API シグネチャ変更の有無 | なし（コメント追記 / 参照パス更新のみ） |
| 実装の挙動変化 | なし（ビルド成果物の動作に影響しない） |
| 修正範囲の妥当性 | 12 ファイル修正（前回 Suggestion 3 件のスコープと完全一致）、過剰修正なし |

### 全体評価

- iOS / Android の追記コメントの内容は仕様（design.md / Open Questions）と整合しており、技術的に正確。
- Section 周辺の spec 参照パス更新は Suggestion 3 で「本提案対象外、archive 後に別途整理対象とすればよい」と提示したものを前倒しで対応したもので、実害なく望ましい改善。
- archive 配下のフルパス（`openspec/changes/archive/2026-05-09-refactor-accessory-and-root-hf/design.md`）が実在し、リンク切れがない。
- `SettingsRoot.header/footer` 削除（破壊的変更）に係る他の動作・API は一切変化していない（差分は全て DocComment / ファイルヘッダコメント）。

ただし、追記された `replaceCell` / `ReplaceCell` の DocComment 内に「**本提案 Decision 5**」というクロスリファレンスがあるが、本提案 `add-partial-update-core` の `design.md` における Decision 5 は「Section 内 KsCell の `moveCell` は `toIndex` のみで指定」に関するものであり、`replaceCell` の identity 保証とは無関係である。replaceCell の identity 保証は本提案 design.md の **Open Questions「Cell ID の表現」** に記載されているのが正しい根拠。これは Minor 指摘として後段に記載する。

**判定**: `APPROVED`（後述する Minor 指摘 1 件を含むが、archive 阻害要因ではない）

## 指摘事項

### 🟡 Minor: 追記された `replaceCell` DocComment の Decision 番号参照が誤っている

**該当箇所**:
- `ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift:38`
- `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiff.kt:42`

**問題点**:

追記コメントの該当箇所:

```swift
/// Native UI 層・MAUI Handler 層の `applyDiff` 実装は一致を前提に最適化される（本提案 Decision 5）。
```

`openspec/changes/add-partial-update-core/design.md` の Decision 5 は以下の通り：

> ### Decision 5: Section 内 KsCell の moveCell は toIndex のみで指定
> 選択: `moveCell(cellID: KsCellID, toIndex: Int)` とし、…

つまり Decision 5 は `moveCell` に関する決定であり、`replaceCell` の identity 保証とは別の論点。`replaceCell` の identity 保証根拠は本提案 design.md の以下に記載されている：

> ## Open Questions
> - **Cell ID の表現**: …`replaceCell` 時に「ID は同じだが内容が違う Cell」を渡す場合の identity 保証は Native UI 層の責務とする

なお、ファイルヘッダコメント（`SettingsRootDiff.swift` 行 8-9 / `SettingsRootDiff.kt` 行 11-12）の "Decision 2 / Decision 3 / Decision 5 / Decision 6 / Decision 7" 列挙はファイル全体としては正しい（Decision 5 は moveCell ケースに対応）。問題は新規追加の `replaceCell` ケース DocComment 側で、replaceCell に関連付けて Decision 5 を引用している点。

review-result_001.md の Suggestion 1 では「design.md Open Questions 参照」と推奨していたため、本対応は推奨修正の引用先を取り違えた可能性が高い。

**推奨修正**:

iOS / Android 両方の `replaceCell` / `ReplaceCell` DocComment の該当行を以下のように修正する：

iOS (`SettingsRootDiff.swift:38`):
```swift
///   Native UI 層・MAUI Handler 層の `applyDiff` 実装は一致を前提に最適化される
///   （本提案 design.md "Open Questions: Cell ID の表現" 参照）。
```

Android (`SettingsRootDiff.kt:42`):
```kotlin
 * Native UI 層・MAUI Handler 層の `applyDiff` 実装は一致を前提に最適化される
 * （本提案 design.md "Open Questions: Cell ID の表現" 参照）。
```

これは API 挙動には一切影響しないドキュメンテーション上の正確性向上であり、archive 阻害要因ではない。本提案を archive する前に対応するか、後続提案で修正するかは運用判断とする。

---

### 🔵 Suggestion: Section.swift / KsCellID.swift などの「設計（履歴）」表現について

**該当箇所**:
- `ios/Sources/KsSettingsViewCore/Section.swift:8-9`
- `ios/Sources/KsSettingsViewCore/SectionAccessory.swift:8-9`
- `ios/Sources/KsSettingsViewCore/RootAccessory.swift:8-9`
- `ios/Sources/KsSettingsViewCore/KsAnyView.swift:8-9`
- 同等の Android 4 ファイル + `build.gradle.kts`

**問題点**:

「設計（履歴）: openspec/changes/archive/.../design.md Decision X」と表記して archive 配下のフルパスにリンクしているが、archive 配下の design.md は **書き換えが想定されない仕様履歴ドキュメント** であり、後年 Decision 番号の意味が変わる可能性は極めて低いため、現行表現で十分機能する。一方で、本提案 `add-partial-update-core` の design.md でも一部 Decision（例: 旧 spec から継承した RootAccessory / SectionAccessory 別型維持など）について新たな Decision として再述しており、利用者に「現行の意思決定の根拠はどこに記載されているか」を問わせる構造になっている。

任意の改善余地として、本提案 archive 後に main spec (`openspec/specs/settings-view-core/spec.md`) 側に「設計判断のサマリ」を集約するか、または各ファイルから main spec の該当 Requirement をピンポイントで参照する形に整理する選択肢もある。これは大規模リファクタリングとなるため、本提案では archive 阻害要因にはしない。

**推奨修正**:

任意。本提案では現行表現を許容する。後続のドキュメント整理提案で検討するのが望ましい。

---

### 🔵 Suggestion: `KsCellID.swift:10` のファイルヘッダ「Decision 5」も同様の誤参照

**該当箇所**: `ios/Sources/KsSettingsViewCore/KsCellID.swift:10`

**問題点**:

```swift
// 設計: openspec/changes/add-partial-update-core/design.md Decision 5。
```

本ファイル（`KsCellID`）の設計根拠は本提案 design.md には明示的な Decision として記載されておらず、Decision 5（moveCell 指定方式）とも関連が薄い。`KsCellID` の `id + contentHash` 構造の設計根拠は元々の `add-settings-view-ios-ui` 提案にあり、本提案では「Core 層への再配置」のみが論点。

これは今回の追加対応で **新規に追加されたものではなく** 元々あった記述だが、上記 Minor 指摘と同じ「Decision 番号の取り違え」パターンであり、追加された冒頭コメント（行 16-28）の整合性とあわせて整理対象にすると良い。

**推奨修正**:

任意。修正するなら以下のように主張：

```swift
// 設計: openspec/changes/add-partial-update-core/design.md
//   "Open Questions: Cell ID の表現"（本提案で Core 層へ再配置）。
//   `id + contentHash` 構造そのものの設計根拠は archive 済み
//   `add-settings-view-ios-ui` 提案を参照。
```

本提案の archive 阻害要因ではない。

---

## アクションプラン

### 必須対応（Critical / Major）

**なし**

### 推奨対応（Minor）

1. 🟡 `SettingsRootDiff.swift:38` および `SettingsRootDiff.kt:42` の追記 DocComment 内「本提案 Decision 5」を「本提案 design.md "Open Questions: Cell ID の表現"」に修正（推奨）

### 任意対応（Suggestion）

2. 🔵 `Section.swift` / `KsCellID.swift` 等で頻出する archive 配下 design.md フルパス参照について、main spec 集約への移行を将来的に検討
3. 🔵 `KsCellID.swift:10` の既存「Decision 5」参照の整理（Minor 指摘 1 と同時対応推奨）

## 判定結果

**ステータス**: `APPROVED`

### 判定根拠

- ✅ 今回の追加対応はすべて DocComment / ファイルヘッダコメント / spec 参照パスの修正のみで、API 挙動・型定義・テストには一切影響しない
- ✅ `swift build` / `swift test`（78 件）/ `./gradlew :ks-settingsview-core:build` / `./gradlew :ks-settingsview-core:test --rerun-tasks` がすべて成功
- ✅ 参照先パス（`openspec/specs/settings-view-core/spec.md` / `openspec/changes/archive/2026-05-09-refactor-accessory-and-root-hf/design.md`）の実在を確認済
- ✅ iOS / Android 双方で Suggestion 1〜3 への対応が漏れなく実装され、両プラットフォームのコメント内容も非対称性の説明を含めて整合している
- ✅ 修正範囲は 12 ファイル（iOS 6 / Android 6）と前回 Suggestion のスコープに完全に一致しており、過剰修正なし
- ✅ ビルド・テスト両面で副作用なし

唯一の Minor 指摘（追記コメント内の Decision 5 取り違え）はドキュメンテーション上の正確性に関するもので、API・実装挙動・テスト・ビルドのいずれにも影響しない。本提案を archive する前に修正することを推奨するが、archive 阻害要因とはしない。

前回 review-result_001.md の APPROVED 判定は本追加対応によっても維持される。

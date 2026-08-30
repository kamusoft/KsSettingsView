# レビュー結果 - purify-core-extract-style-to-ui-layer (Round 2 / iOS)

**レビュー日時**: 2026年06月07日
**レビュワー**: sdd-reviewer
**変更提案ID**: purify-core-extract-style-to-ui-layer
**スコープ**: iOS 側のみ（Android は別セッションで対応予定）

## サマリー

Round 1 で指摘した Major 2 件（`.icon(_ icon: KsImage)` modifier 未実装、`DSLStyleModifiable` 配置 spec 矛盾）はいずれも適切に解消されており、設計判断・実装品質ともに高水準。Suggestion-1（`@unchecked Sendable` の根拠コメント）も Theme / CellStyle / KsImage の各ファイルへ追記済み。新規追加された `DSLIconModifiable` プロトコル、`LabelCell` / `CommandCell` の準拠実装、`CellModifiers.icon(_:)` 経路は spec の MUST と整合し、`mutateStyle` 経路と一貫した実装パターン（`any DSLIconModifiable` への動的キャスト → `withIcon(_:)` 呼び出し → `DSLHintRegistry` のヒント引き継ぎ → no-op フォールバック）になっている。

ビルド・テスト結果（再確認）:
- `swift build`: 成功
- `swift test`: **83 件すべて成功**（Round 1 と同一カウント、リグレッションなし）
- `openspec validate purify-core-extract-style-to-ui-layer`: 成功

Round 1 で残った Minor / Suggestion について:
- **Suggestion-1（`@unchecked Sendable` 根拠）**: 解消済み。`Theme.swift:21-25`, `CellStyle.swift:18-22`, `KsImage.swift:26-32` に同じフォーマットの根拠コメントが追記され、レビュー対象として十分な明示。
- **Minor-1（Theme/CellStyle/KsImage 専用ユニットテスト未追加）**: tasks.md 2.6-2.8 の方針通り、間接カバー（`SettingsRootStoreTests.test_applyTheme_同値ならtheme通知を抑制する` / `BasicCellsTests` / `LabelCellTests` 等）を維持。この判断は文書化済みであり、再指摘しない。
- **Suggestion-2（tasks.md 完了条件チェックリスト整合）**: Android 着手時に `[x]` 化される運用前提で OK。今回も特に修正不要。

Round 2 で新規に検出した重大事項はなし。新規追加コード（`DSLIconModifiable.swift` / `CellModifiers.icon(_:)` / `LabelCell.withIcon(_:)` / `CommandCell.withIcon(_:)` / spec L82 / L88-91 / L104-108）はいずれも本提案の Decision とも矛盾せず、`swiftui` / `style` / `cell-types-basic` の三 spec とも整合する。

**判定**: `APPROVED`

## 指摘事項

### 🔵 Suggestion-1 (任意・観察): `.icon(_:)` modifier の専用テスト追加

**該当箇所**:
- 実装: `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift:161-175`
- 既存テスト: `ios/Tests/KsSettingsViewSwiftUITests/CellModifiersTests.swift`

**問題点（観察）**:
`CellModifiersTests.swift` には `.font(_:)` / `.cellHeight(_:)` / `.cellID(_:)` の専用テストはあるが、新規追加した `.icon(_:)` modifier に対する専用テストはない。Sample / 既存テスト群は init 引数経路で `icon: KsImage` を渡しており、modifier 経路（`LabelCell(title: "X").icon(.systemName("bell"))` のようなコード）を踏むテストが存在しない。

新規導入された経路にしては動作の保証が間接（`mutateStyle` と同パターンであることに依拠）のみ。リグレッション検出の粒度を細かくする観点で、以下のような最小テストが望ましい。

**推奨修正**:
任意（必須ではない）。後続セッションで追加可。

```swift
// CellModifiersTests.swift に追加候補
func test_iconModifier_でLabelCellのiconが上書きされる() {
    let cell = LabelCell(title: "A", icon: nil)
    let modified = cell.icon(.systemName("bell"))
    // any DSLIconModifiable 経路を踏み LabelCell.withIcon が呼ばれる
    if case let .systemName(name) = modified.icon {
        XCTAssertEqual(name, "bell")
    } else {
        XCTFail("Expected .systemName(\"bell\"), got \(String(describing: modified.icon))")
    }
}

func test_iconModifier_でCommandCellのiconが上書きされる() { ... }

func test_iconModifier_でicon非対応Cellではno_opになる() {
    // SwitchCell など DSLIconModifiable 非準拠 Cell に `.icon(...)` を呼んでも自身が返る
    let cell = SwitchCell(title: "A", isOn: false)
    let result = cell.icon(.systemName("bell"))
    XCTAssertEqual(cell.id, result.id)  // 同一インスタンス
}

func test_iconModifier_経由でHintRegistryのヒントが新インスタンスに引き継がれる() {
    DSLHintRegistry.shared.reset()
    let cell = LabelCell(title: "A")
    _ = cell.cellID("explicit-1")
    let rebuilt = cell.icon(.systemName("bell"))
    // rebuilt.id に対するヒントが .explicit("explicit-1") であることを確認
    if case .explicit(let id) = DSLHintRegistry.shared.cellHint(for: rebuilt.id) {
        XCTAssertEqual(id, AnyHashable("explicit-1"))
    } else {
        XCTFail()
    }
}
```

判断レベル: **任意**。`mutateStyle` 経路の各テストが間接的に同じパターンを通過するため、本提案範囲ではテストなしで承認可能。

### 🔵 Suggestion-2 (任意・観察): tasks.md 4.6 の括弧内注記の整理

**該当箇所**: `openspec/changes/purify-core-extract-style-to-ui-layer/tasks.md:36`

**問題点（観察）**:
tasks 4.6 の括弧内コメントは「Round 1 レビューで `.icon(_:)` modifier 未実装が指摘されたため、`DSLIconModifiable` プロトコルを UI 層に新設し `LabelCell` / `CommandCell` に準拠させ、`.icon(_ icon: KsImage)` modifier を `CellModifiers.swift` に追加で実装した」と長文で経緯を残している。これは記録としては有益だが、提案が archive されたあとに `openspec list --archived` から参照される際の SN（差分ノート）として可読性が下がる懸念がある。

**推奨修正**:
任意。「`DSLIconModifiable` を UI 層に新設し、`LabelCell` / `CommandCell` で準拠。`.icon(_ icon: KsImage)` modifier を CellModifiers.swift に追加」程度の短文化で十分。Round 1 経緯は review-result_001.md / review-result_002.md に残るためタスク注記から削っても情報損失しない。

判断レベル: **任意**。提案 archive 時に整理する運用で OK。

## アクションプラン

優先度順:

1. **🔵 Suggestion-1（任意）**: `.icon(_:)` modifier 専用テストを追加して経路を直接カバー（後続セッションでも可）。
2. **🔵 Suggestion-2（任意）**: tasks.md 4.6 の括弧内注記を短文化（archive 前の整理タスクで OK）。

いずれも本提案の APPROVED 判定を覆さないレベル。

## 判定結果

**ステータス**: `APPROVED`

理由:
- Round 1 で挙げた Major-1 / Major-2 が両方とも解消された（前者は実装追加、後者は spec 修正）。
- Suggestion-1（`@unchecked Sendable` 根拠コメント）も Theme / CellStyle / KsImage に追記済み。
- 新規追加コードは spec / Decision と整合し、設計品質も既存パターン (`mutateStyle`) と一貫している。
- `swift build` / `swift test`（83 件） / `openspec validate` がすべて緑。
- iOS 側のスコープに限り、本提案の主要目標（Core 純化 / Theme・CellStyle・KsImage の UI 層再配置 / `SettingsRootStore.applyTheme(_:)` 独立 API 化 / Sample の Native 型化）はすべて達成済み。
- 残る Suggestion は粒度の細かいテスト追加と注記整理のみで、必須ではない。
- Android 側 / MAUI Bridge 整合は本提案 iOS スコープ外（別セッションで対応）。

本 review-result_002 でもって iOS 側の本提案実装は承認可能と判断する。Android 側着手後に同提案で再度 review-result_003 以降を作成する想定。

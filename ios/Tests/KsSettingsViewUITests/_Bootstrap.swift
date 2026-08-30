// _Bootstrap.swift
// KsSettingsViewUITests
//
// テスト共通のダミー Cell / 共通ユーティリティ群。
//
// `CellStyle` / `Theme` は UI 層（`KsSettingsViewUI`）に属する（core/ADR-0009）ため、
// 本ダミー Cell は `#if canImport(UIKit)` ガード内で定義する（Core テストは UI 型を
// 参照しない）。

#if canImport(UIKit)
import Foundation
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

// MARK: - テスト共通ダミー Cell

/// テスト用ダミー Cell（`LabelCell` 等の具象 Cell とは別に、`KsCellRegistry` の
/// 登録・解決ロジックを Cell 種別に依存せず検証するためテストモジュール内で定義する）。
internal struct TestDummyCell: KsCell {
    let id: UUID
    let style: CellStyle
    let label: String

    init(id: UUID = UUID(), style: CellStyle = CellStyle(), label: String = "test") {
        self.id = id
        self.style = style
        self.label = label
    }

    static func == (lhs: TestDummyCell, rhs: TestDummyCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.label == rhs.label
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hashCellStyle(style, into: &hasher)
        hasher.combine(label)
    }
}

/// `TestDummyCell` 用のレンダラ。`render` 呼び出し有無の検証に使う。
internal final class TestDummyCellView: UICollectionViewListCell, KsCellRenderer {
    static var lastRenderedTitle: String?

    func render(cell: any KsCell, theme: Theme) {
        guard let c = cell as? TestDummyCell else { return }
        TestDummyCellView.lastRenderedTitle = c.label
    }
}
#endif

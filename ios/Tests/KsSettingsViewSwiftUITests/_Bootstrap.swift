// _Bootstrap.swift
// KsSettingsViewSwiftUITests
//
// テストモジュールの空読込防止用の最小定義 + テスト共通ダミー Cell。
//
// `CellStyle` と `DSLStyleModifiable` は UI 層（`KsSettingsViewUI`）に属する
//（core/ADR-0009）ため、ダミー Cell は UIKit ガード内で定義する。

import Foundation
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore

#if canImport(UIKit)
@testable import KsSettingsViewUI
#endif

internal enum KsSettingsViewSwiftUITestsBootstrap {
    static let identifier = "KsSettingsViewSwiftUITests"
}

#if canImport(UIKit)
import UIKit

/// テスト専用のダミー Cell。`LabelCell` 等の UI 層の
/// 具象 Cell とは別に、DSL 経路の規約（`DSLReidentifiable` / `DSLStyleModifiable`）が
/// 任意の Cell 型に対しても機能することを検証するためにテストモジュール内で別途定義する。
///
/// `DSLReidentifiable` は Core 配置、`DSLStyleModifiable` は UI 層配置。
internal struct DummyTestCell: KsCell, DSLReidentifiable, DSLStyleModifiable {
    let id: UUID
    let style: CellStyle
    let title: String

    init(id: UUID = UUID(), style: CellStyle = CellStyle(), title: String) {
        self.id = id
        self.style = style
        self.title = title
    }

    static func == (lhs: DummyTestCell, rhs: DummyTestCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hashCellStyle(style, into: &hasher)
        hasher.combine(title)
    }

    func withDSLID(_ id: UUID) -> DummyTestCell {
        return DummyTestCell(id: id, style: style, title: title)
    }

    func withStyle(_ style: CellStyle) -> DummyTestCell {
        return DummyTestCell(id: id, style: style, title: title)
    }
}
#endif

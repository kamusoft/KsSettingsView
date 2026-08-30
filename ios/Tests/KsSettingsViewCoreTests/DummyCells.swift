// DummyCells.swift
// KsSettingsViewCoreTests
//
// テスト専用ダミー Cell。Core モジュールには具象 Cell は存在しないため、
// `[any KsCell]` の異種コレクション挙動を検証するためにテスト内でのみ定義する。
//
// `KsCell` はスタイルを要求しない（`CellStyle` は UI 層に属する: core/ADR-0009）ため、
// 本ダミー Cell も `style` を持たない。

import Foundation
@testable import KsSettingsViewCore

/// テスト用ダミー: ラベル風 Cell（`style` を持たない）
struct DummyLabelCell: KsCell {
    let id: UUID
    let title: String

    init(id: UUID = UUID(), title: String) {
        self.id = id
        self.title = title
    }
}

/// テスト用ダミー: スイッチ風 Cell（`style` を持たない）
struct DummySwitchCell: KsCell {
    let id: UUID
    let isOn: Bool

    init(id: UUID = UUID(), isOn: Bool) {
        self.id = id
        self.isOn = isOn
    }
}

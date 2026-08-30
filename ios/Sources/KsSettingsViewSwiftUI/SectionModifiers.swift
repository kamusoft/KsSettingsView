// SectionModifiers.swift
// KsSettingsViewSwiftUI
//
// `KsSettingsViewCore.Section` に対する DSL 向け modifier 拡張。
//
// 戻り型は `KsSettingsViewCore.Section`（= `KsSection`）に統一する（値型 copy）。
// `.sectionID(_:)` は DSL ヒントレジストリに `.explicit` を記録する。

import Foundation
import SwiftUI
import KsSettingsViewCore

extension KsSettingsViewCore.Section {

    /// Section Header を文字列で指定する。
    /// - Parameter text: ヘッダ文字列
    /// - Returns: header を上書きした新 `Section`
    public func sectionHeader(_ text: String) -> KsSettingsViewCore.Section {
        return copyWith(header: .text(text))
    }

    /// Section Header を任意 View で指定する。
    /// - Parameter content: ヘッダとして描画する View
    /// - Returns: header を `.view(KsAnyView.swiftUI { ... })` に上書きした新 `Section`
    public func sectionHeader<V: View>(
        @ViewBuilder content: () -> V
    ) -> KsSettingsViewCore.Section {
        let view = content()
        return copyWith(header: .view(KsAnyView.swiftUI { view }))
    }

    /// Section Footer を文字列で指定する。
    public func sectionFooter(_ text: String) -> KsSettingsViewCore.Section {
        return copyWith(footer: .text(text))
    }

    /// Section Footer を任意 View で指定する。
    public func sectionFooter<V: View>(
        @ViewBuilder content: () -> V
    ) -> KsSettingsViewCore.Section {
        let view = content()
        return copyWith(footer: .view(KsAnyView.swiftUI { view }))
    }

    /// Section の明示 ID を指定する（動的構造での同一性安定化用）。
    /// - Parameter id: 任意の `AnyHashable`（`String` / `UUID` / 任意の `Hashable` 等）
    /// - Returns: 自身を返す（ヒントはサイドチャンネル レジストリに記録）
    public func sectionID(_ id: AnyHashable) -> KsSettingsViewCore.Section {
        DSLHintRegistry.shared.recordSectionHint(
            sectionInstanceID: self.id,
            hint: .explicit(id)
        )
        return self
    }

    // MARK: - 内部 copy ヘルパ

    /// `header` のみ書き換えた新 `Section` を返す。
    /// 既存ヒント（DSL レジストリ）は新 `Section.id` に引き継ぐ。
    fileprivate func copyWith(header: SectionAccessory?) -> KsSettingsViewCore.Section {
        let new = KsSettingsViewCore.Section(
            id: self.id,
            header: header,
            footer: self.footer,
            cells: self.cells,
            headerHeight: self.headerHeight,
            isVisible: self.isVisible,
            isHeaderVisible: self.isHeaderVisible,
            isFooterVisible: self.isFooterVisible
        )
        return new
    }

    fileprivate func copyWith(footer: SectionAccessory?) -> KsSettingsViewCore.Section {
        let new = KsSettingsViewCore.Section(
            id: self.id,
            header: self.header,
            footer: footer,
            cells: self.cells,
            headerHeight: self.headerHeight,
            isVisible: self.isVisible,
            isHeaderVisible: self.isHeaderVisible,
            isFooterVisible: self.isFooterVisible
        )
        return new
    }
}

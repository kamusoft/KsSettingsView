// PickerCell.swift
// KsSettingsViewUI
//
// 候補リストから単一または複数の項目を選択する Cell。タップでモーダルを開く。
// `PickerSelectionMode.single` / `.multiple` で動作モードを切替える。
//
// Native 型は独自値型でラップせず直接公開する（core/ADR-0009）。ただし `selectionMode` だけは
// 対応する Native 型が無いため UI 層独自の列挙型（`PickerSelectionMode`）とする。
// 基本 Cell 共通の規約（`isEnabled` / `isVisible` / `description` / `icon` / `hintText`）へは
// opt-in で準拠し、`valueText` 未指定時は現在の選択値から自動生成する。
//
// 候補は `PickerItem`（主表示 + 任意の副表示）の列として保持する。任意の要素型からの射影と
// 元要素の書き戻しは `PickerCell+ItemProjection.swift` のジェネリック init が担う（core/ADR-0029）。

#if canImport(UIKit)
import Foundation
import UIKit
import SwiftUI
import KsSettingsViewCore

/// 候補リスト選択 Cell。
///
/// `selectionMode = .single` のとき `selectedIndex: Int?` を、`.multiple` のとき
/// `selectedIndices: Set<Int>` を TwoWay 管理する。
///
/// `valueText` 引数が `nil` のときは現在の選択値を文字列化して表示する
/// （`items[selectedIndex].text`、または選択項目の `text` を `, ` 連結）。
public struct PickerCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    public let title: String
    public let description: String?
    /// 値テキスト（`nil` のときは現在の選択値を自動表示）
    public let valueText: String?
    public let icon: KsImage?
    public let hintText: String?
    public let items: [PickerItem]
    /// 選択モード（既定 `.single`）
    public let selectionMode: PickerSelectionMode
    /// 単一選択モード時の選択 index（`.single` のみ意味を持つ）
    public let selectedIndex: Int?
    /// 複数選択モード時の選択 index 集合（`.multiple` のみ意味を持つ）
    public let selectedIndices: Set<Int>
    /// 複数選択モードでの選択上限（`0` で無制限、既定 `0`）
    public let maxSelectedNumber: Int
    /// モーダル画面のナビゲーションバータイトル
    public let pageTitle: String?
    /// 選択強調色（任意）
    public let accentColor: UIColor?
    /// 単一選択モードの選択変更 callback
    public let onSelectionChanged: (@Sendable (Int) -> Void)?
    /// 複数選択モードの選択変更 callback
    public let onMultiSelectionChanged: (@Sendable (Set<Int>) -> Void)?
    public let isEnabled: Bool
    public let isVisible: Bool

    // MARK: - 単一選択 Store 経路 init

    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [PickerItem],
        selectedIndex: Int?,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onSelectionChanged: (@Sendable (Int) -> Void)? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        self.id = id
        self.style = style
        self.title = title
        self.description = description
        self.valueText = valueText
        self.icon = icon
        self.hintText = hintText
        self.items = items
        self.selectionMode = .single
        self.selectedIndex = selectedIndex
        self.selectedIndices = []
        self.maxSelectedNumber = 0
        self.pageTitle = pageTitle
        self.accentColor = accentColor
        self.onSelectionChanged = onSelectionChanged
        self.onMultiSelectionChanged = nil
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    // MARK: - 単一選択 DSL 経路 init（`Binding<Int?>`）

    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [PickerItem],
        selectedIndex: Binding<Int?>,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        self.init(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            items: items,
            selectedIndex: selectedIndex.wrappedValue,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onSelectionChanged: PickerCell.indexSetter(for: selectedIndex),
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    // MARK: - 複数選択 Store 経路 init

    /// 複数選択モード専用 init。`selectedIndices: Set<Int>` を受け取る時点で
    /// `selectionMode` は `.multiple` で固定されるため、引数からは省く。
    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [PickerItem],
        selectedIndices: Set<Int>,
        maxSelectedNumber: Int = 0,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onMultiSelectionChanged: (@Sendable (Set<Int>) -> Void)? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        self.id = id
        self.style = style
        self.title = title
        self.description = description
        self.valueText = valueText
        self.icon = icon
        self.hintText = hintText
        self.items = items
        self.selectionMode = .multiple
        self.selectedIndex = nil
        self.selectedIndices = selectedIndices
        self.maxSelectedNumber = maxSelectedNumber
        self.pageTitle = pageTitle
        self.accentColor = accentColor
        self.onSelectionChanged = nil
        self.onMultiSelectionChanged = onMultiSelectionChanged
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    // MARK: - 複数選択 DSL 経路 init（`Binding<Set<Int>>`）

    /// 複数選択モード専用 DSL init。`Binding<Set<Int>>` を受け取る時点で
    /// `selectionMode` は `.multiple` で固定されるため、引数からは省く。
    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [PickerItem],
        selectedIndices: Binding<Set<Int>>,
        maxSelectedNumber: Int = 0,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        self.init(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            items: items,
            selectedIndices: selectedIndices.wrappedValue,
            maxSelectedNumber: maxSelectedNumber,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onMultiSelectionChanged: PickerCell.indicesSetter(for: selectedIndices),
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    // MARK: - Binding 書き戻しの組み立て

    /// `Binding<Int?>` へ選択 index を書き戻す callback を作る。
    internal static func indexSetter(for binding: Binding<Int?>) -> @Sendable (Int) -> Void {
        return { newIndex in
            MainActor.assumeIsolated {
                binding.wrappedValue = newIndex
            }
        }
    }

    /// `Binding<Set<Int>>` へ選択 index 集合を書き戻す callback を作る。
    internal static func indicesSetter(for binding: Binding<Set<Int>>) -> @Sendable (Set<Int>) -> Void {
        return { newSet in
            MainActor.assumeIsolated {
                binding.wrappedValue = newSet
            }
        }
    }

    // MARK: - フル指定 internal init（withDSLID / withStyle / withIcon 用）

    internal init(
        id: UUID,
        style: CellStyle,
        title: String,
        description: String?,
        valueText: String?,
        icon: KsImage?,
        hintText: String?,
        items: [PickerItem],
        selectionMode: PickerSelectionMode,
        selectedIndex: Int?,
        selectedIndices: Set<Int>,
        maxSelectedNumber: Int,
        pageTitle: String?,
        accentColor: UIColor?,
        onSelectionChanged: (@Sendable (Int) -> Void)?,
        onMultiSelectionChanged: (@Sendable (Set<Int>) -> Void)?,
        isEnabled: Bool,
        isVisible: Bool
    ) {
        self.id = id
        self.style = style
        self.title = title
        self.description = description
        self.valueText = valueText
        self.icon = icon
        self.hintText = hintText
        self.items = items
        self.selectionMode = selectionMode
        self.selectedIndex = selectedIndex
        self.selectedIndices = selectedIndices
        self.maxSelectedNumber = maxSelectedNumber
        self.pageTitle = pageTitle
        self.accentColor = accentColor
        self.onSelectionChanged = onSelectionChanged
        self.onMultiSelectionChanged = onMultiSelectionChanged
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    // MARK: - 自動 valueText の解決

    /// `valueText` が明示指定されていればそれを、`nil` のときは現在の選択値を文字列化して返す。
    ///
    /// 自動表示の内訳（いずれも主表示 `text` のみを使い、副表示は含めない）：
    /// - `.single`: `items[selectedIndex].text`
    /// - `.multiple`: 選択項目の `text` を index 昇順に `, ` 連結
    internal func effectiveValueText() -> String? {
        if let v = valueText { return v }
        switch selectionMode {
        case .single:
            guard let idx = selectedIndex, items.indices.contains(idx) else { return nil }
            return items[idx].text
        case .multiple:
            let sorted = selectedIndices.sorted()
            let strs = sorted.compactMap { idx -> String? in
                guard items.indices.contains(idx) else { return nil }
                return items[idx].text
            }
            return strs.isEmpty ? nil : strs.joined(separator: ", ")
        }
    }

    // MARK: - Hashable / Equatable

    public static func == (lhs: PickerCell, rhs: PickerCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.items == rhs.items
            && lhs.selectionMode == rhs.selectionMode
            && lhs.selectedIndex == rhs.selectedIndex
            && lhs.selectedIndices == rhs.selectedIndices
            && lhs.maxSelectedNumber == rhs.maxSelectedNumber
            && lhs.pageTitle == rhs.pageTitle
            && uiColorEqualOptional(lhs.accentColor, rhs.accentColor)
            && lhs.isEnabled == rhs.isEnabled
            && lhs.isVisible == rhs.isVisible
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hashCellStyle(style, into: &hasher)
        hasher.combine(title)
        hasher.combine(description)
        hasher.combine(valueText)
        hasher.combine(icon)
        hasher.combine(hintText)
        hasher.combine(items)
        hasher.combine(selectionMode)
        hasher.combine(selectedIndex)
        hasher.combine(selectedIndices)
        hasher.combine(maxSelectedNumber)
        hasher.combine(pageTitle)
        if let c = accentColor {
            hasher.combine(c.hashValue)
        } else {
            hasher.combine(Int(0))
        }
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> PickerCell {
        return PickerCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            items: items,
            selectionMode: selectionMode,
            selectedIndex: selectedIndex,
            selectedIndices: selectedIndices,
            maxSelectedNumber: maxSelectedNumber,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onSelectionChanged: onSelectionChanged,
            onMultiSelectionChanged: onMultiSelectionChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> PickerCell {
        return PickerCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            items: items,
            selectionMode: selectionMode,
            selectedIndex: selectedIndex,
            selectedIndices: selectedIndices,
            maxSelectedNumber: maxSelectedNumber,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onSelectionChanged: onSelectionChanged,
            onMultiSelectionChanged: onMultiSelectionChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withIcon(_ icon: KsImage?) -> PickerCell {
        return PickerCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            items: items,
            selectionMode: selectionMode,
            selectedIndex: selectedIndex,
            selectedIndices: selectedIndices,
            maxSelectedNumber: maxSelectedNumber,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onSelectionChanged: onSelectionChanged,
            onMultiSelectionChanged: onMultiSelectionChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif

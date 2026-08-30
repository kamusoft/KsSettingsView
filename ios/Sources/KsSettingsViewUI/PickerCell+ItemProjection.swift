// PickerCell+ItemProjection.swift
// KsSettingsViewUI
//
// 任意の要素型 `T` を候補として渡すためのジェネリック縁 init 群と、その String 特殊化。
//
// 縁は構築時に要素列をコピーして捕捉し、表示用の `PickerItem` 列へ射影する。要素型はここから
// 外（モデル・描画・equality・輸送）へは現れず、選択の正は index のままである（core/ADR-0029）。
// 確定操作では index の書き戻し（binding / index callback）が先、元要素の callback が後に走る。

#if canImport(UIKit)
import Foundation
import UIKit
import SwiftUI
import KsSettingsViewCore

// MARK: - ジェネリック縁（単一選択）

extension PickerCell {

    /// 任意の要素列と射影から単一選択 Cell を構築する（Store 経路）。
    ///
    /// - Parameters:
    ///   - displayText: 要素から主表示テキストを作る射影。
    ///   - subText: 要素から副表示テキストを作る射影（`nil` または空文字列を返した要素は副表示なし）。
    ///   - onItemSelected: 確定した index に対応する元要素を受け取る callback。
    public init<T: Sendable>(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [T],
        displayText: @escaping @Sendable (T) -> String,
        subText: (@Sendable (T) -> String?)? = nil,
        selectedIndex: Int?,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onSelectionChanged: (@Sendable (Int) -> Void)? = nil,
        onItemSelected: (@Sendable (T) -> Void)? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        let elements = items
        self.init(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            items: PickerCell.projectItems(elements, displayText: displayText, subText: subText),
            selectedIndex: selectedIndex,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onSelectionChanged: PickerCell.composeSingleSelection(
                elements: elements,
                indexSink: onSelectionChanged,
                itemSink: onItemSelected
            ),
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// 任意の要素列と射影から単一選択 Cell を構築する（`Binding<Int?>` 経路）。
    public init<T: Sendable>(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [T],
        displayText: @escaping @Sendable (T) -> String,
        subText: (@Sendable (T) -> String?)? = nil,
        selectedIndex: Binding<Int?>,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onItemSelected: (@Sendable (T) -> Void)? = nil,
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
            displayText: displayText,
            subText: subText,
            selectedIndex: selectedIndex.wrappedValue,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onSelectionChanged: PickerCell.indexSetter(for: selectedIndex),
            onItemSelected: onItemSelected,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// 元要素の TwoWay binding から単一選択 Cell を構築する。
    ///
    /// 構築時に `selectedItem` を候補列から同値で逆引きして選択 index を決める。同値の要素が
    /// 複数あるときは最初の位置に、候補列に無い要素は未選択に解決する。確定時は対応する元要素へ
    /// 書き戻す（有効な候補が無い index では `nil` になる）。
    public init<T: Equatable & Sendable>(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [T],
        displayText: @escaping @Sendable (T) -> String,
        subText: (@Sendable (T) -> String?)? = nil,
        selectedItem: Binding<T?>,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        let elements = items
        let initialIndex = selectedItem.wrappedValue.flatMap { elements.firstIndex(of: $0) }
        let setter: @Sendable (Int) -> Void = { newIndex in
            MainActor.assumeIsolated {
                selectedItem.wrappedValue = elements.indices.contains(newIndex) ? elements[newIndex] : nil
            }
        }
        self.init(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            items: PickerCell.projectItems(elements, displayText: displayText, subText: subText),
            selectedIndex: initialIndex,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onSelectionChanged: setter,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}

// MARK: - ジェネリック縁（複数選択）

extension PickerCell {

    /// 任意の要素列と射影から複数選択 Cell を構築する（Store 経路）。
    ///
    /// - Parameter onItemsSelected: 確定した index 集合に対応する元要素を index 昇順で受け取る callback
    ///   （範囲外 index に対応する要素は含まれない）。
    public init<T: Sendable>(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [T],
        displayText: @escaping @Sendable (T) -> String,
        subText: (@Sendable (T) -> String?)? = nil,
        selectedIndices: Set<Int>,
        maxSelectedNumber: Int = 0,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onMultiSelectionChanged: (@Sendable (Set<Int>) -> Void)? = nil,
        onItemsSelected: (@Sendable ([T]) -> Void)? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        let elements = items
        self.init(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            items: PickerCell.projectItems(elements, displayText: displayText, subText: subText),
            selectedIndices: selectedIndices,
            maxSelectedNumber: maxSelectedNumber,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onMultiSelectionChanged: PickerCell.composeMultiSelection(
                elements: elements,
                indicesSink: onMultiSelectionChanged,
                itemsSink: onItemsSelected
            ),
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// 任意の要素列と射影から複数選択 Cell を構築する（`Binding<Set<Int>>` 経路）。
    public init<T: Sendable>(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [T],
        displayText: @escaping @Sendable (T) -> String,
        subText: (@Sendable (T) -> String?)? = nil,
        selectedIndices: Binding<Set<Int>>,
        maxSelectedNumber: Int = 0,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onItemsSelected: (@Sendable ([T]) -> Void)? = nil,
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
            displayText: displayText,
            subText: subText,
            selectedIndices: selectedIndices.wrappedValue,
            maxSelectedNumber: maxSelectedNumber,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onMultiSelectionChanged: PickerCell.indicesSetter(for: selectedIndices),
            onItemsSelected: onItemsSelected,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}

// MARK: - String 特殊化（射影は恒等、`displayText` を省略できる簡易形）

extension PickerCell {

    /// 文字列列から単一選択 Cell を構築する（Store 経路）。
    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [String],
        subText: (@Sendable (String) -> String?)? = nil,
        selectedIndex: Int?,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onSelectionChanged: (@Sendable (Int) -> Void)? = nil,
        onItemSelected: (@Sendable (String) -> Void)? = nil,
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
            displayText: { $0 },
            subText: subText,
            selectedIndex: selectedIndex,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onSelectionChanged: onSelectionChanged,
            onItemSelected: onItemSelected,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// 文字列列から単一選択 Cell を構築する（`Binding<Int?>` 経路）。
    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [String],
        subText: (@Sendable (String) -> String?)? = nil,
        selectedIndex: Binding<Int?>,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onItemSelected: (@Sendable (String) -> Void)? = nil,
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
            displayText: { $0 },
            subText: subText,
            selectedIndex: selectedIndex,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onItemSelected: onItemSelected,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// 文字列の TwoWay binding から単一選択 Cell を構築する。
    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [String],
        subText: (@Sendable (String) -> String?)? = nil,
        selectedItem: Binding<String?>,
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
            displayText: { $0 },
            subText: subText,
            selectedItem: selectedItem,
            pageTitle: pageTitle,
            accentColor: accentColor,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// 文字列列から複数選択 Cell を構築する（Store 経路）。
    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [String],
        subText: (@Sendable (String) -> String?)? = nil,
        selectedIndices: Set<Int>,
        maxSelectedNumber: Int = 0,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onMultiSelectionChanged: (@Sendable (Set<Int>) -> Void)? = nil,
        onItemsSelected: (@Sendable ([String]) -> Void)? = nil,
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
            displayText: { $0 },
            subText: subText,
            selectedIndices: selectedIndices,
            maxSelectedNumber: maxSelectedNumber,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onMultiSelectionChanged: onMultiSelectionChanged,
            onItemsSelected: onItemsSelected,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// 文字列列から複数選択 Cell を構築する（`Binding<Set<Int>>` 経路）。
    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        items: [String],
        subText: (@Sendable (String) -> String?)? = nil,
        selectedIndices: Binding<Set<Int>>,
        maxSelectedNumber: Int = 0,
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onItemsSelected: (@Sendable ([String]) -> Void)? = nil,
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
            displayText: { $0 },
            subText: subText,
            selectedIndices: selectedIndices,
            maxSelectedNumber: maxSelectedNumber,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onItemsSelected: onItemsSelected,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}

// MARK: - 射影と callback の組み立て

extension PickerCell {

    /// 要素列へ射影を適用して候補列を作る。
    internal static func projectItems<T>(
        _ elements: [T],
        displayText: (T) -> String,
        subText: ((T) -> String?)?
    ) -> [PickerItem] {
        return elements.map { element in
            PickerItem(text: displayText(element), subText: subText?(element))
        }
    }

    /// 単一選択の確定 callback を組み立てる。index の書き戻しを先に、元要素の通知を後に走らせる。
    /// どちらの受け口も無ければ callback 自体を持たない。
    internal static func composeSingleSelection<T: Sendable>(
        elements: [T],
        indexSink: (@Sendable (Int) -> Void)?,
        itemSink: (@Sendable (T) -> Void)?
    ) -> (@Sendable (Int) -> Void)? {
        guard indexSink != nil || itemSink != nil else { return nil }
        return { newIndex in
            indexSink?(newIndex)
            if let itemSink, elements.indices.contains(newIndex) {
                itemSink(elements[newIndex])
            }
        }
    }

    /// 複数選択の確定 callback を組み立てる。index 集合の書き戻しを先に、元要素列の通知を後に走らせる。
    /// 元要素列は index 昇順で、範囲外 index に対応する要素は含めない。
    internal static func composeMultiSelection<T: Sendable>(
        elements: [T],
        indicesSink: (@Sendable (Set<Int>) -> Void)?,
        itemsSink: (@Sendable ([T]) -> Void)?
    ) -> (@Sendable (Set<Int>) -> Void)? {
        guard indicesSink != nil || itemsSink != nil else { return nil }
        return { newIndices in
            indicesSink?(newIndices)
            if let itemsSink {
                let picked = newIndices.sorted().compactMap { index -> T? in
                    elements.indices.contains(index) ? elements[index] : nil
                }
                itemsSink(picked)
            }
        }
    }
}
#endif

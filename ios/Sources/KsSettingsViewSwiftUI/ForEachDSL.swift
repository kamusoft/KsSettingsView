// ForEachDSL.swift
// KsSettingsViewSwiftUI
//
// DSL（`KsSettingsView { ... }` / `Section { ... }`）で動的コレクションを展開する
// 独自 `ForEach` 関数群。SwiftUI 本家の View 版 `ForEach` とは戻り型で振り分ける。
//
// 戻り型は `[KsSettingsViewCore.Section]` / `[any KsCell]` とし、各要素を `DSLHintRegistry` に
// `.forEach(itemID)` ヒントで登録する。これらのヒントは `KsSettingsViewBuilder.promoteToNode(_:)`
// で `Section` → `DSLSectionNode` 昇格時に Node へ転写され、安定 ID パイプラインに乗る
// （core/ADR-0008）。
//
// ## 4 オーバーロード
//
// - ルート（Section 列）用：`Identifiable` 版 + `id:` KeyPath 版
// - セクション内（Cell 列）用：`Identifiable` 版 + `id:` KeyPath 版

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore

// MARK: - ルート用（Section 群を展開）

/// ルート用 × `Identifiable` 版 `ForEach`。
public func ForEach<Data, Element>(
    _ data: Data,
    @SettingsRootBuilder content: (Element) -> [KsSettingsViewCore.Section]
) -> [KsSettingsViewCore.Section]
where Data: RandomAccessCollection, Data.Element == Element, Element: Identifiable {
    return data.flatMap { item in
        let sections = content(item)
        return sections.map { section in
            attachForEachHint(section: section, itemID: AnyHashable(item.id))
        }
    }
}

/// ルート用 × `id:` KeyPath 版 `ForEach`。
public func ForEach<Data, Element, ID>(
    _ data: Data,
    id: KeyPath<Element, ID>,
    @SettingsRootBuilder content: (Element) -> [KsSettingsViewCore.Section]
) -> [KsSettingsViewCore.Section]
where Data: RandomAccessCollection, Data.Element == Element, ID: Hashable {
    return data.flatMap { item in
        let sections = content(item)
        let key = item[keyPath: id]
        return sections.map { section in
            attachForEachHint(section: section, itemID: AnyHashable(key))
        }
    }
}

// MARK: - セクション内用（Cell 群を展開）

/// セクション内用 × `Identifiable` 版 `ForEach`。
public func ForEach<Data, Element>(
    _ data: Data,
    @KsSectionBuilder content: (Element) -> [any KsCell]
) -> [any KsCell]
where Data: RandomAccessCollection, Data.Element == Element, Element: Identifiable {
    return data.flatMap { item in
        let cells = content(item)
        return cells.map { cell in
            attachForEachHint(cell: cell, itemID: AnyHashable(item.id))
        }
    }
}

/// セクション内用 × `id:` KeyPath 版 `ForEach`。
public func ForEach<Data, Element, ID>(
    _ data: Data,
    id: KeyPath<Element, ID>,
    @KsSectionBuilder content: (Element) -> [any KsCell]
) -> [any KsCell]
where Data: RandomAccessCollection, Data.Element == Element, ID: Hashable {
    return data.flatMap { item in
        let cells = content(item)
        let key = item[keyPath: id]
        return cells.map { cell in
            attachForEachHint(cell: cell, itemID: AnyHashable(key))
        }
    }
}

// MARK: - 内部ユーティリティ

/// ForEach 配下の Section にヒントを埋め込む。
///
/// `KsSettingsViewCore.Section` には modifier 履歴フィールドが存在しないため、
/// 本提案で導入する **DSL レジストリ**（プロセスローカルのサイドチャンネル）に
/// `section.id` → `DSLIdentityHint.forEach(itemID)` を記録する。
/// `KsSettingsViewBuilder.promoteToNode(_:)` で `Section` → `DSLSectionNode` 昇格時に
/// レジストリから引き出され、Node の `identityHint` に転写される。
internal func attachForEachHint(
    section: KsSettingsViewCore.Section,
    itemID: AnyHashable
) -> KsSettingsViewCore.Section {
    DSLHintRegistry.shared.recordSectionHint(
        sectionInstanceID: section.id,
        hint: .forEach(itemID)
    )
    return section
}

/// ForEach 配下の Cell にヒントを埋め込む。
internal func attachForEachHint(
    cell: any KsCell,
    itemID: AnyHashable
) -> any KsCell {
    DSLHintRegistry.shared.recordCellHint(
        cellInstanceID: cell.id,
        hint: .forEach(itemID)
    )
    return cell
}
#endif

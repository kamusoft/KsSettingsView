// LabelCell.swift
// KsSettingsViewUI
//
// 読み取り専用の表示用 Cell。タイトル、説明、値テキスト、アイコン、ヒントテキストを保持する。
//
// 全 Cell 共通の `isEnabled` / `isVisible` を持つ。`style`（UI 層 `CellStyle`）と
// `icon`（UI 層 `KsImage?` — `UIImage` を直接保持できる sealed enum）は Core の Cell 抽象では
// なく本 Cell が個別に保持する（core/ADR-0009）。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

/// 読み取り専用の表示 Cell。
///
/// `title` を必須、`description` / `valueText` / `icon` / `hintText` は任意。
/// 値型として `Hashable` / `Sendable` を満たし、DSL 経路では `DSLReidentifiable` /
/// `DSLStyleModifiable` 規約により `id` / `style` の rebind が可能。
public struct LabelCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    /// Cell 個別スタイル（UI 層 `CellStyle`）。Core 抽象の要求からは外れ、各 Cell が個別に持つ。
    public let style: CellStyle
    /// タイトル（必須）
    public let title: String
    /// 説明文（任意）
    public let description: String?
    /// 右側に表示する値文字列（任意）
    public let valueText: String?
    /// アイコン（任意）
    public let icon: KsImage?
    /// ヒントテキスト（任意、右上）
    public let hintText: String?
    /// 有効／無効フラグ（既定 `true`）。`false` のときは描画上テキスト色を
    /// `Theme.disabledTextColor` に置換する（LabelCell はコントロール要素を持たないため
    /// テキスト色置換のみ）。
    public let isEnabled: Bool
    /// 可視性フラグ（既定 `true`）。`false` のときは UI 層の visible projection から除外され、
    /// 描画されない。`SettingsRoot` 上にはモデルとして保持される。
    public let isVisible: Bool

    /// 任意フィールドを指定して `LabelCell` を生成する。
    /// - Parameters:
    ///   - id: 一意 ID（既定 `UUID()` で自動採番）
    ///   - style: Cell 個別スタイル（既定 `CellStyle()`）
    ///   - title: タイトル
    ///   - description: 説明文（既定 `nil`）
    ///   - valueText: 右寄せ値文字列（既定 `nil`）
    ///   - icon: アイコン（既定 `nil`）
    ///   - hintText: ヒントテキスト（既定 `nil`）
    ///   - isEnabled: 有効／無効（既定 `true`）
    ///   - isVisible: 可視性（既定 `true`）
    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
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
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    // MARK: - Hashable / Equatable 手動実装
    //
    // `style: CellStyle` は `UIColor` / `UIFont` を保持しており Swift `Equatable` 自動合成が
    // 効かないため、`CellStyle.==` を経由する手動 `==` / `hash(into:)` を実装する。
    // `icon: KsImage?` も sealed enum で手動 `Hashable` を持つため、`combine` で取り込む。

    public static func == (lhs: LabelCell, rhs: LabelCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.isEnabled == rhs.isEnabled
            && lhs.isVisible == rhs.isVisible
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        // CellStyle の hash は値の安定性のため、id 等の構造同期で十分なケースでは省略可能だが、
        // 値型としての完全等価性を担保するため style も含める（フィールドの Optional 性に従い hash 化）。
        hashCellStyle(style, into: &hasher)
        hasher.combine(title)
        hasher.combine(description)
        hasher.combine(valueText)
        hasher.combine(icon)
        hasher.combine(hintText)
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> LabelCell {
        return LabelCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> LabelCell {
        return LabelCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// DSL の `.icon(_:)` modifier 経由で呼ばれる、`icon` のみを書き換えた copy を返す。
    public func withIcon(_ icon: KsImage?) -> LabelCell {
        return LabelCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif

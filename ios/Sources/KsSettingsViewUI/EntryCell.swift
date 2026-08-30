// EntryCell.swift
// KsSettingsViewUI
//
// テキスト入力用 Cell。行内 trailing に `UITextField` を内蔵し、
// `text` の TwoWay binding（SwiftUI `@Binding<String>`）と callback 経路（Store 経路）の
// 両方をサポートする。
//
// `keyboardType` 等は Native 型を独自列挙型でラップせず直接公開する（core/ADR-0009）。
// 基本 Cell 共通の規約（`isEnabled` / `isVisible` / `description` / `icon` / `hintText`）へは
// opt-in で準拠し、`valueText` は例外として持たない（行内 trailing は `UITextField` が占める）。
// `id` は未指定時に自動採番する。

#if canImport(UIKit)
import Foundation
import UIKit
import SwiftUI
import KsSettingsViewCore

/// テキスト入力用 Cell。
///
/// 右側 accessory に `UITextField` を配置し、ユーザー入力で `onTextChanged(String)` を発火する。
/// SwiftUI DSL から使う場合は `Binding<String>` を受ける便利 init を、Store 経路（外部から `Cell`
/// 値を直接構築する）からは `text: String` + `onTextChanged` を渡す init を使う。
///
/// 共通規約の例外として `valueText` フィールドは **持たない**。
///
/// `keyboardType` は **`UIKeyboardType` を直接公開**（独自列挙型でラップしない）。
public struct EntryCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    public let title: String
    public let description: String?
    /// アイコン（任意）
    public let icon: KsImage?
    /// ヒントテキスト（任意、右上）
    public let hintText: String?
    /// 現在のテキスト値（DSL 経路でも binding 値の wrappedValue が反映される）
    public let text: String
    /// プレースホルダ
    public let placeholder: String?
    /// プレースホルダ文字色（任意）。`nil` のとき `CellStyle` → `Theme` → プラットフォーム既定へ解決する。
    public let placeholderColor: UIColor?
    /// キーボード種別（Native 型 `UIKeyboardType` を直接公開、既定 `.default`）
    public let keyboardType: UIKeyboardType
    /// パスワードマスクフラグ（既定 `false`）
    public let isPassword: Bool
    /// テキスト配置（既定 `.start`）
    public let textAlignment: CellTitleAlignment
    /// caret 色および選択ハイライト色（任意）
    public let accentColor: UIColor?
    /// 最大文字数（`nil` で無制限、既定 `nil`）。
    /// AiForms.Maui.SettingsView の `MaxLength: int` 互換。
    public let maxLength: Int?
    /// テキスト変更時に呼ばれるクロージャ。
    /// DSL 経路では `Binding<String>` の setter を wrap して内部設定される。
    public let onTextChanged: (@Sendable (String) -> Void)?
    /// 有効／無効フラグ（既定 `true`）
    public let isEnabled: Bool
    /// 可視性フラグ（既定 `true`）
    public let isVisible: Bool

    // MARK: - Store 経路 init（`text: String` + callback）

    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        text: String,
        placeholder: String? = nil,
        placeholderColor: UIColor? = nil,
        keyboardType: UIKeyboardType = .default,
        isPassword: Bool = false,
        textAlignment: CellTitleAlignment = .end,
        accentColor: UIColor? = nil,
        maxLength: Int? = nil,
        onTextChanged: (@Sendable (String) -> Void)? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        self.id = id
        self.style = style
        self.title = title
        self.description = description
        self.icon = icon
        self.hintText = hintText
        self.text = text
        self.placeholder = placeholder
        self.placeholderColor = placeholderColor
        self.keyboardType = keyboardType
        self.isPassword = isPassword
        self.textAlignment = textAlignment
        self.accentColor = accentColor
        self.maxLength = maxLength
        self.onTextChanged = onTextChanged
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    // MARK: - DSL 経路 init（`text: Binding<String>`）

    /// SwiftUI 経路で `@Binding<String>` を受ける便利 init。
    /// 内部では `binding.wrappedValue` を初期 `text` に、`onTextChanged` を
    /// `binding.wrappedValue = newValue` で wrap する。
    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        text: Binding<String>,
        placeholder: String? = nil,
        placeholderColor: UIColor? = nil,
        keyboardType: UIKeyboardType = .default,
        isPassword: Bool = false,
        textAlignment: CellTitleAlignment = .end,
        accentColor: UIColor? = nil,
        maxLength: Int? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        // `Binding` の setter を `@Sendable` クロージャに safely wrap する。
        // 呼び出し元（`EntryCellView.handleEditingChanged`）は MainActor 上で動作するため、
        // `MainActor.assumeIsolated` で隔離コンテキストを伝えて即時実行する。
        let setter: @Sendable (String) -> Void = { newValue in
            MainActor.assumeIsolated {
                text.wrappedValue = newValue
            }
        }
        self.init(
            id: id,
            style: style,
            title: title,
            description: description,
            icon: icon,
            hintText: hintText,
            text: text.wrappedValue,
            placeholder: placeholder,
            placeholderColor: placeholderColor,
            keyboardType: keyboardType,
            isPassword: isPassword,
            textAlignment: textAlignment,
            accentColor: accentColor,
            maxLength: maxLength,
            onTextChanged: setter,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    // MARK: - Hashable / Equatable（クロージャを判定対象から除外）

    public static func == (lhs: EntryCell, rhs: EntryCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.text == rhs.text
            && lhs.placeholder == rhs.placeholder
            && uiColorEqualOptional(lhs.placeholderColor, rhs.placeholderColor)
            && lhs.keyboardType == rhs.keyboardType
            && lhs.isPassword == rhs.isPassword
            && lhs.textAlignment == rhs.textAlignment
            && uiColorEqualOptional(lhs.accentColor, rhs.accentColor)
            && lhs.maxLength == rhs.maxLength
            && lhs.isEnabled == rhs.isEnabled
            && lhs.isVisible == rhs.isVisible
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hashCellStyle(style, into: &hasher)
        hasher.combine(title)
        hasher.combine(description)
        hasher.combine(icon)
        hasher.combine(hintText)
        hasher.combine(text)
        hasher.combine(placeholder)
        if let c = placeholderColor {
            hasher.combine(c.hashValue)
        } else {
            hasher.combine(Int(0))
        }
        hasher.combine(keyboardType.rawValue)
        hasher.combine(isPassword)
        hasher.combine(textAlignment)
        if let c = accentColor {
            hasher.combine(c.hashValue)
        } else {
            hasher.combine(Int(0))
        }
        hasher.combine(maxLength)
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> EntryCell {
        return EntryCell(
            id: id,
            style: style,
            title: title,
            description: description,
            icon: icon,
            hintText: hintText,
            text: text,
            placeholder: placeholder,
            placeholderColor: placeholderColor,
            keyboardType: keyboardType,
            isPassword: isPassword,
            textAlignment: textAlignment,
            accentColor: accentColor,
            maxLength: maxLength,
            onTextChanged: onTextChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> EntryCell {
        return EntryCell(
            id: id,
            style: style,
            title: title,
            description: description,
            icon: icon,
            hintText: hintText,
            text: text,
            placeholder: placeholder,
            placeholderColor: placeholderColor,
            keyboardType: keyboardType,
            isPassword: isPassword,
            textAlignment: textAlignment,
            accentColor: accentColor,
            maxLength: maxLength,
            onTextChanged: onTextChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withIcon(_ icon: KsImage?) -> EntryCell {
        return EntryCell(
            id: id,
            style: style,
            title: title,
            description: description,
            icon: icon,
            hintText: hintText,
            text: text,
            placeholder: placeholder,
            placeholderColor: placeholderColor,
            keyboardType: keyboardType,
            isPassword: isPassword,
            textAlignment: textAlignment,
            accentColor: accentColor,
            maxLength: maxLength,
            onTextChanged: onTextChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif

// ButtonCell.swift
// KsSettingsViewUI
//
// ボタン用途の Cell。タイトルをボタンスタイルで描画し、タップで `onTap` を発火する。
//
// `titleAlignment` でタイトルの水平位置を、`isEnabled` で操作可否を指定する。
// `titleColor` は Native 型（`UIColor?`）を直接保持する（core/ADR-0009）。
// `valueText` / `icon` / `hintText` は全 Cell 共通の行レイアウトフィールドとして持つ
// （core/ADR-0011）。一方 **`description` は意図的に持たない**
// （`AiForms.Maui.SettingsView` の `ButtonCell` が `Description` を `private new` で
// 隠蔽している挙動に合わせる）。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

/// ボタン用途の Cell。
///
/// `title` をボタンスタイルで表示する。Disclosure Indicator は表示しない。
/// `titleColor` を指定するとボタンテキストの色を上書きする
/// （指定がない場合は `CellStyle.titleColor` または Theme の既定色）。
///
/// `titleAlignment` でタイトルの水平方向の揃え位置を指定する（既定 `.center`）。
///
/// `valueText` / `icon` / `hintText` のいずれかが指定された場合は通常レイアウト
/// （`[icon][title][valueText (右寄せ)][hintText]`）に切り替わり、`titleAlignment` は
/// title 列の中での揃え位置のみを制御する。すべて `nil` のときはボタンスタイルの
/// 中央寄せ／左寄せ／右寄せフォーマット（既存仕様）を維持する。
///
/// **注意**: `description` フィールドは **存在しない**（オリジナル `AiForms.Maui.SettingsView`
/// の `ButtonCell` が `Description` を `private new` で隠蔽し、iOS の `ButtonCellView.cs` も
/// `DescriptionLabel.Hidden = true` としている挙動を踏襲する）。
public struct ButtonCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    /// ボタンタイトル
    public let title: String
    /// 右側に表示する値文字列（任意）
    public let valueText: String?
    /// アイコン（任意）
    public let icon: KsImage?
    /// ヒントテキスト（任意、右上）
    public let hintText: String?
    /// ボタンテキストの色（任意、`CellStyle.titleColor` を上書き）
    public let titleColor: UIColor?
    /// タップ時に発火するクロージャ
    public let onTap: (@Sendable () -> Void)?
    /// タイトルの水平方向の揃え位置（既定 `.center`）
    public let titleAlignment: CellTitleAlignment
    /// 有効／無効フラグ（既定 `true`）。`false` のときはタップを受け付けず、
    /// テキスト色を `Theme.disabledTextColor` に置換する。
    public let isEnabled: Bool
    /// 可視性フラグ（既定 `true`）。`false` のときは UI 層の visible projection から除外される。
    public let isVisible: Bool

    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        titleColor: UIColor? = nil,
        onTap: (@Sendable () -> Void)? = nil,
        titleAlignment: CellTitleAlignment = .center,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        self.id = id
        self.style = style
        self.title = title
        self.valueText = valueText
        self.icon = icon
        self.hintText = hintText
        self.titleColor = titleColor
        self.onTap = onTap
        self.titleAlignment = titleAlignment
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    public static func == (lhs: ButtonCell, rhs: ButtonCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && uiColorEqualOptional(lhs.titleColor, rhs.titleColor)
            && lhs.titleAlignment == rhs.titleAlignment
            && lhs.isEnabled == rhs.isEnabled
            && lhs.isVisible == rhs.isVisible
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hashCellStyle(style, into: &hasher)
        hasher.combine(title)
        hasher.combine(valueText)
        hasher.combine(icon)
        hasher.combine(hintText)
        if let c = titleColor {
            hasher.combine(c.hashValue)
        } else {
            hasher.combine(Int(0))
        }
        hasher.combine(titleAlignment)
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> ButtonCell {
        return ButtonCell(
            id: id,
            style: style,
            title: title,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            titleColor: titleColor,
            onTap: onTap,
            titleAlignment: titleAlignment,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> ButtonCell {
        return ButtonCell(
            id: id,
            style: style,
            title: title,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            titleColor: titleColor,
            onTap: onTap,
            titleAlignment: titleAlignment,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// DSL の `.icon(_:)` modifier 経由で呼ばれる、`icon` のみを書き換えた copy を返す。
    public func withIcon(_ icon: KsImage?) -> ButtonCell {
        return ButtonCell(
            id: id,
            style: style,
            title: title,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            titleColor: titleColor,
            onTap: onTap,
            titleAlignment: titleAlignment,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif

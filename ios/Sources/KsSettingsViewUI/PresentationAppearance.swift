// PresentationAppearance.swift
// KsSettingsViewUI
//
// Cell から提示するモーダル（PickerCell の選択面・DatePickerCell のカレンダーシート）の外観を、
// 提示元の実効外観へ揃えるユーティリティ。
//
// モーダルの土台になる `UIPresentationController` の container view は、提示元の view 階層では
// なく window 直下に置かれる。そのため、ホストアプリが window ではなく root view controller 側へ
// `overrideUserInterfaceStyle` を掛けている構成（.NET MAUI の `Application.UserAppTheme` はこの形
// を採る）では、提示物の中身だけが提示元の外観で描かれ、sheet の地色（背面素材）と chrome は
// window の外観のまま残る。ダーク指定のアプリでライトの地色に白文字が乗る、といった食い違いに
// なるため、提示直前に提示元の実効外観を presentation controller まで引き継いで揃える。

#if canImport(UIKit)
import UIKit

/// モーダル提示物へ提示元の外観を引き継ぐ内部ユーティリティ。
internal enum PresentationAppearance {

    /// 提示元と window の実効外観から、提示物へ引き継ぐ外観を決める。
    ///
    /// window が既に提示元と同じ外観なら container view も同じ外観になるため引き継ぎは不要で、
    /// `nil` を返して上書きを付けない（提示中の外観変更にホストが追随する余地を残す）。
    internal static func styleToInherit(
        source: UIUserInterfaceStyle,
        window: UIUserInterfaceStyle
    ) -> UIUserInterfaceStyle? {
        guard source != .unspecified else { return nil }
        guard source != window else { return nil }
        return source
    }

    /// 提示元 view の実効外観を、提示する VC とその presentation controller へ引き継ぐ。
    /// `present(_:animated:)` を呼ぶ直前に使う。
    @MainActor
    internal static func inherit(from source: UIView, to presented: UIViewController) {
        guard let window = source.window else { return }
        guard let style = styleToInherit(
            source: source.traitCollection.userInterfaceStyle,
            window: window.traitCollection.userInterfaceStyle
        ) else { return }
        presented.overrideUserInterfaceStyle = style
        // container view の外観は VC の override では変わらないため、presentation controller 側へも
        // 同じ外観を与える。
        presented.presentationController?.overrideTraitCollection =
            UITraitCollection(userInterfaceStyle: style)
    }
}
#endif

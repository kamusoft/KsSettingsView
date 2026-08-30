// KsBridgeFont.swift
// KsSettingsViewBridge
//
// interop 境界で `UIFont` を輸送するプリミティブなフォント記述子。

#if canImport(UIKit)
import Foundation
import UIKit

/// フォントを interop 境界で輸送する記述子。
///
/// interop 境界では platform のフォント型を直接渡せないため、family 名・サイズ・太字／斜体の
/// プリミティブで表現し、Native 側で `UIFont` へ解決する (maui/ADR-0004)。
@objc(KsBridgeFont)
public final class KsBridgeFont: NSObject {

    /// フォントファミリ名。`nil` または解決できない名前のときはシステムフォントを使う。
    @objc public var familyName: String?

    /// ポイントサイズ。`0` 以下のときは本文既定サイズを使う。
    @objc public var pointSize: Double

    /// 太字にするか。
    @objc public var isBold: Bool

    /// 斜体にするか。
    @objc public var isItalic: Bool

    /// フォント記述子を生成する。
    /// - Parameters:
    ///   - familyName: フォントファミリ名 (`nil` でシステムフォント)
    ///   - pointSize: ポイントサイズ (`0` 以下で本文既定サイズ)
    ///   - isBold: 太字
    ///   - isItalic: 斜体
    @objc public init(familyName: String?, pointSize: Double, isBold: Bool, isItalic: Bool) {
        self.familyName = familyName
        self.pointSize = pointSize
        self.isBold = isBold
        self.isItalic = isItalic
        super.init()
    }

    /// 記述子から `UIFont` を解決する。
    internal func resolve() -> UIFont {
        let size = pointSize > 0 ? CGFloat(pointSize) : UIFont.preferredFont(forTextStyle: .body).pointSize
        let base: UIFont
        if let familyName, let named = UIFont(name: familyName, size: size) {
            base = named
        } else {
            base = UIFont.systemFont(ofSize: size, weight: isBold ? .bold : .regular)
        }

        var traits: UIFontDescriptor.SymbolicTraits = []
        if isBold { traits.insert(.traitBold) }
        if isItalic { traits.insert(.traitItalic) }
        guard !traits.isEmpty, let descriptor = base.fontDescriptor.withSymbolicTraits(traits) else {
            return base
        }
        return UIFont(descriptor: descriptor, size: size)
    }
}
#endif

// KsBridgeAccessoryView.swift
// KsSettingsViewBridge
//
// interop 境界で受け取った `UIView` を Native の accessory 表現へ包む共通処理。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

/// interop 境界の `UIView` を `KsAnyView` へ包む変換。
///
/// Native の accessory は描画のたびに factory を呼んで view を得る契約だが、interop 境界を
/// 越えて渡されるのは生成済みのインスタンス 1 つである。そのため常に同じインスタンスを返す
/// closure に包む (maui/ADR-0017)。
@MainActor
internal enum KsBridgeAccessoryView {

    /// 常に同じ `UIView` を返す `KsAnyView` を作る。
    ///
    /// 返す前に既存の親から切り離すため、リサイクル等で同じ view が別の描画先へ再び
    /// 取り付けられても失敗しない。
    /// - Parameter view: accessory として表示する view
    static func anyView(_ view: UIView) -> KsAnyView {
        return KsAnyView.uiKit {
            view.removeFromSuperview()
            return view
        }
    }

    /// view と text から Section の accessory を解決する。view が指定されていれば view を優先する。
    /// - Parameters:
    ///   - view: accessory として表示する view (`nil` で未指定)
    ///   - text: accessory として表示する text (`nil` で未指定)
    static func sectionAccessory(view: UIView?, text: String?) -> SectionAccessory? {
        if let view {
            return .view(anyView(view))
        }
        return text.map { .text($0) }
    }
}
#endif

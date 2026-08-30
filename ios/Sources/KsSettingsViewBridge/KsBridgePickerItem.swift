// KsBridgePickerItem.swift
// KsSettingsViewBridge
//
// interop 境界で `PickerCell` の候補1件を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation

/// `PickerCell` の候補1件 (主表示 + 任意の副表示) を輸送する DTO。
///
/// 表示射影は上位層が適用済みであり、Native 側で射影を解き直すことはない。`subText` が `nil` の
/// 候補は副表示を持たず、選択面では1行構成で描画される。候補1件を1オブジェクトで運ぶため、
/// 主表示と副表示の件数がずれることが構造的に起こらない。
@objc(KsBridgePickerItem)
public final class KsBridgePickerItem: NSObject {

    /// 主表示テキスト
    @objc public var text: String

    /// 副表示テキスト (`nil` は副表示なし)
    @objc public var subText: String?

    /// - Parameters:
    ///   - text: 主表示テキスト
    ///   - subText: 副表示テキスト (`nil` で副表示なし)
    @objc public init(text: String, subText: String?) {
        self.text = text
        self.subText = subText
        super.init()
    }

    /// 副表示を持たない候補を作る。
    /// - Parameter text: 主表示テキスト
    @objc public convenience init(text: String) {
        self.init(text: text, subText: nil)
    }
}
#endif

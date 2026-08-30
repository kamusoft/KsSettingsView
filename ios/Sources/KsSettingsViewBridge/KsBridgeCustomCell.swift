// KsBridgeCustomCell.swift
// KsSettingsViewBridge
//
// interop 境界で `CustomCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import SwiftUI
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

/// 任意の view を行の内容として表示する Cell (`CustomCell`) を輸送する DTO。
///
/// 内容は `view` (platform view の実体) と `contentToken` (その実体の世代) の対で運ぶ。Native の
/// content には token だけを格納するため、Native から見た内容の等価性は token の値等価で決まる
/// (maui/ADR-0020)。token が同じ間は、他のプロパティ変更で再バインドが起きても埋め込まれる view は
/// 同一インスタンスのまま維持される。
///
/// `view` が `nil` の DTO は内容なしの行として描画される。
///
/// 共通行レイアウトのスロット (タイトル・説明文・アイコン) を持たない Cell のため、基底の
/// `title` / `descriptionText` / `valueText` / `hintText` / `icon` は Native へ写されない。
///
/// タップは `hasTapHandler` が `true` のときだけ
/// `KsBridgeInteractionDelegate.customCellTapped(cellID:)` で通知される。`false` の行はタップ動作を
/// 持たず、内容の中の操作を妨げない。
@objc(KsBridgeCustomCell)
public final class KsBridgeCustomCell: KsBridgeCell {

    /// 行の内容として表示する view (`nil` で内容なし)
    @objc public var view: UIView?

    /// 内容として埋め込む view の世代。実体が入れ替わるたびに変わる値を上位層が振る。
    @objc public var contentToken: String = ""

    /// Disclosure Indicator を表示するフラグ
    @objc public var showArrowIndicator: Bool = false

    /// 行タップを通知するフラグ
    @objc public var hasTapHandler: Bool = false

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        let content = view
        // 購読なしの行は onTap を持たせない (内容の中の操作を妨げないため)。
        var onTap: (@Sendable () -> Void)?
        if hasTapHandler {
            onTap = { relay.customCellTapped(cellID: notifiedCellID) }
        }

        // 内容の実体は 1 つしかないため、描画のたびに同じインスタンスを返す。
        // 未指定のときは空の内容を返し、行だけが出力される状態にする。
        //
        // 埋め込みの identity は内容の世代に結び付ける。世代が同じ間の再バインドでは埋め込みが
        // 作り直されず view インスタンスが維持され、世代が変わったときだけ作り直されて新しい
        // view へ差し替わる。
        let builder: (String) -> AnyView = { token in
            guard let content else { return AnyView(EmptyView().id(token)) }
            return AnyView(KsBridgeCellContentView(view: content).id(token))
        }

        return CustomCell(
            id: id,
            style: resolvedStyle,
            content: contentToken,
            showArrow: showArrowIndicator,
            onTap: onTap,
            isEnabled: isEnabled,
            isVisible: isVisible,
            builder: builder
        )
    }
}
#endif

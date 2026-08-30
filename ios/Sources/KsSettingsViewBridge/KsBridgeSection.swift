// KsBridgeSection.swift
// KsSettingsViewBridge
//
// interop 境界で `Section` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

/// Section を interop 境界で輸送する DTO。
///
/// インスタンス生成時に Bridge が canonical UUID 文字列の `sectionID` を採番する
/// (maui/ADR-0005)。header / footer は text と view の両方を輸送でき、同じ位置に両方を
/// 指定した場合は view を表示する。
///
/// `cells` は共通基底型 `KsBridgeCell` で保持するため、Cell 種の異なる DTO を混載できる。
///
/// DTO は 1 インスタンスが 1 つの Section identity を表す。同じインスタンスを複数箇所へ追加すると
/// 同じ `sectionID` の Section が重複するため、Section ごとに新しいインスタンスを生成する。
@objc(KsBridgeSection)
public final class KsBridgeSection: NSObject {

    /// Bridge が採番した canonical UUID 文字列の Section ID。
    @objc public let sectionID: String

    /// ヘッダテキスト (`nil` でヘッダなし)
    @objc public var headerText: String?

    /// フッタテキスト (`nil` でフッタなし)
    @objc public var footerText: String?

    /// ヘッダに表示する view (`nil` で view 指定なし)。
    ///
    /// 非 `nil` のときは `headerText` より優先され、ヘッダにはこの view が表示される。
    @objc public var headerView: UIView?

    /// フッタに表示する view (`nil` で view 指定なし)。
    ///
    /// 非 `nil` のときは `footerText` より優先され、フッタにはこの view が表示される。
    @objc public var footerView: UIView?

    /// 可視性フラグ。`false` の Section は header / footer / 配下 Cell ごと表示から除外される。
    @objc public var isVisible: Bool = true

    /// Header の表示トグル (core/ADR-0023)。`false` のとき内容があっても Header を表示しない。
    ///
    /// 内容が無い (`nil` または空 text) Header をトグルで表示させることはできない。
    @objc public var isHeaderVisible: Bool = true

    /// Footer の表示トグル (core/ADR-0023)。意味論は `isHeaderVisible` と対称。
    @objc public var isFooterVisible: Bool = true

    /// ヘッダの固定高さ (pt、`nil` で Native 既定の自動高さ)
    @objc public var headerHeight: NSNumber?

    /// Section 内の Cell 群 (追加順)
    @objc public private(set) var cells: [KsBridgeCell]

    /// `sectionID` に対応する Native の `UUID`。
    internal let identifier: UUID

    /// header / footer テキストを指定して空の Section DTO を生成する。
    /// - Parameters:
    ///   - headerText: ヘッダテキスト (`nil` でヘッダなし)
    ///   - footerText: フッタテキスト (`nil` でフッタなし)
    @objc public convenience init(headerText: String?, footerText: String?) {
        self.init(headerText: headerText, footerText: footerText, cells: [])
    }

    /// header / footer テキストと Cell 群を指定して Section DTO を生成する。
    /// - Parameters:
    ///   - headerText: ヘッダテキスト (`nil` でヘッダなし)
    ///   - footerText: フッタテキスト (`nil` でフッタなし)
    ///   - cells: Section 内の Cell 群
    @objc public init(headerText: String?, footerText: String?, cells: [KsBridgeCell]) {
        self.identifier = KsBridgeIdentifier.make()
        self.sectionID = KsBridgeIdentifier.string(from: self.identifier)
        self.headerText = headerText
        self.footerText = footerText
        self.cells = cells
        super.init()
    }

    /// Cell を末尾に追加し、Bridge が採番した cellID を返す。
    /// - Parameter cell: 追加する Cell DTO
    /// - Returns: 追加した Cell の cellID
    @discardableResult
    @objc public func addCell(_ cell: KsBridgeCell) -> String {
        cells.append(cell)
        return cell.cellID
    }

    /// DTO の現在の内容から Native の `Section` を組み立てる。
    /// - Parameters:
    ///   - id: 生成する Section の ID
    ///   - relay: 配下 Cell のユーザー操作を転送する中継
    @MainActor
    internal func makeSection(id: UUID, relay: KsBridgeInteractionRelay) -> KsSettingsViewCore.Section {
        // 未指定の headerHeight は Native の `Section` 既定 (自動高さ) をそのまま使う。
        let automaticHeaderHeight = KsSettingsViewCore.Section(id: id).headerHeight
        return KsSettingsViewCore.Section(
            id: id,
            header: KsBridgeAccessoryView.sectionAccessory(view: headerView, text: headerText),
            footer: KsBridgeAccessoryView.sectionAccessory(view: footerView, text: footerText),
            cells: cells.map { $0.makeCell(relay: relay) },
            headerHeight: headerHeight?.doubleValue ?? automaticHeaderHeight,
            isVisible: isVisible,
            isHeaderVisible: isHeaderVisible,
            isFooterVisible: isFooterVisible
        )
    }

    /// DTO 自身が採番した ID で Native の `Section` を組み立てる。
    /// - Parameter relay: 配下 Cell のユーザー操作を転送する中継
    @MainActor
    internal func makeSection(relay: KsBridgeInteractionRelay) -> KsSettingsViewCore.Section {
        return makeSection(id: identifier, relay: relay)
    }
}
#endif

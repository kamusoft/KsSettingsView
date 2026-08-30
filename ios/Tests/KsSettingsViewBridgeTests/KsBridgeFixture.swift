// KsBridgeFixture.swift
// KsSettingsViewBridgeTests
//
// Bridge テストで共有する標準的な設定ツリーの組み立て。

#if canImport(UIKit)
import Foundation
import UIKit
@testable import KsSettingsViewBridge

/// テスト共通の設定ツリーを組み立てる。
///
/// 構成は Section 2 個で、1 つ目 (header "S1") に Cell A / B、2 つ目 (header "S2") に Cell C。
@MainActor
internal enum KsBridgeFixture {

    /// 組み立て済みの Bridge と、後続操作で使う DTO 群。
    internal struct Built {
        let bridge: KsSettingsBridge
        let section1: KsBridgeSection
        let section2: KsBridgeSection
        let cellA: KsBridgeLabelCell
        let cellB: KsBridgeLabelCell
        let cellC: KsBridgeLabelCell
    }

    /// 標準構成の Bridge を `setRoot` 済みの状態で返す。
    static func standard() -> Built {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()

        let section1 = builder.addSection(headerText: "S1", footerText: nil)
        let section2 = builder.addSection(headerText: "S2", footerText: nil)

        let cellA = KsBridgeLabelCell(title: "A")
        let cellB = KsBridgeLabelCell(title: "B")
        let cellC = KsBridgeLabelCell(title: "C")
        builder.addLabelCell(cellA, sectionID: section1.sectionID)
        builder.addLabelCell(cellB, sectionID: section1.sectionID)
        builder.addLabelCell(cellC, sectionID: section2.sectionID)

        bridge.setRoot(builder)
        return Built(
            bridge: bridge,
            section1: section1,
            section2: section2,
            cellA: cellA,
            cellB: cellB,
            cellC: cellC
        )
    }

    /// 指定した Cell 群を 1 つの Section に載せた Bridge を `setRoot` 済みで返す。
    /// - Parameter cells: Section へ載せる Cell DTO 群
    static func withCells(_ cells: [KsBridgeCell]) -> KsSettingsBridge {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        for cell in cells {
            section.addCell(cell)
        }
        bridge.setRoot(builder)
        return bridge
    }

    /// Store の先頭 Section に載っている Cell を指定型として取り出す。
    /// - Parameters:
    ///   - bridge: 対象 Bridge
    ///   - index: 先頭 Section 内の位置
    static func storedCell<T>(_ bridge: KsSettingsBridge, at index: Int = 0) -> T? {
        return bridge.store.root.sections.first?.cells[index] as? T
    }

    /// 単色の `UIImage` を生成する。interop で受け渡す platform 画像として使う。
    /// - Parameter size: 生成する画像の寸法
    static func image(size: CGFloat = 24) -> UIImage {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: size, height: size))
        return renderer.image { context in
            UIColor.red.setFill()
            context.fill(CGRect(x: 0, y: 0, width: size, height: size))
        }
    }

    /// Bridge が採番しない、canonical UUID として解釈できない ID。
    static let unknownIdentifier = "not-a-canonical-uuid"

    /// canonical UUID ではあるが Bridge が設定ツリーへ載せていない ID。
    static func unusedIdentifier() -> String {
        return UUID().uuidString
    }
}
#endif

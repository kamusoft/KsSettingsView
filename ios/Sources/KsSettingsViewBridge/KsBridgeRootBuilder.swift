// KsBridgeRootBuilder.swift
// KsSettingsViewBridge
//
// interop 境界から設定ツリー (Section と Cell) を組み立てる `@objc` 互換 Builder。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore

/// 設定ツリーを組み立てて `KsSettingsBridge.setRoot(_:)` へ渡す Builder。
///
/// Section / Cell の ID は Bridge が採番し、追加 API の戻り値として呼び出し側へ返す
/// (maui/ADR-0005)。呼び出し側は返された ID だけを更新 API に渡す。
@objc(KsBridgeRootBuilder)
public final class KsBridgeRootBuilder: NSObject {

    /// 追加順の Section 群。
    @objc public private(set) var sections: [KsBridgeSection]

    /// 空の Builder を生成する。
    @objc public override init() {
        self.sections = []
        super.init()
    }

    /// header / footer テキストを持つ Section を生成して末尾に追加する。
    /// - Parameters:
    ///   - headerText: ヘッダテキスト (`nil` でヘッダなし)
    ///   - footerText: フッタテキスト (`nil` でフッタなし)
    /// - Returns: 追加した Section DTO (`sectionID` は Bridge 採番済み)
    @discardableResult
    @objc public func addSection(headerText: String?, footerText: String?) -> KsBridgeSection {
        let section = KsBridgeSection(headerText: headerText, footerText: footerText)
        sections.append(section)
        return section
    }

    /// 生成済みの Section DTO を末尾に追加する。
    /// - Parameter section: 追加する Section DTO
    /// - Returns: 追加した Section の sectionID
    @discardableResult
    @objc public func addSection(_ section: KsBridgeSection) -> String {
        sections.append(section)
        return section.sectionID
    }

    /// 指定 Section の末尾に Cell を追加する。
    /// - Parameters:
    ///   - cell: 追加する Cell DTO (Cell 種を問わない)
    ///   - sectionID: 追加先 Section の sectionID
    /// - Returns: 追加した Cell の cellID。`sectionID` が Builder 内に存在しない場合は `nil` (no-op)
    @discardableResult
    @objc public func addCell(_ cell: KsBridgeCell, sectionID: String) -> String? {
        guard let section = sections.first(where: { $0.sectionID == sectionID }) else {
            return nil
        }
        return section.addCell(cell)
    }

    /// 指定 Section の末尾に LabelCell を追加する。
    ///
    /// Cell 種を問わない `addCell(_:sectionID:)` と同じ動作で、LabelCell に限った書き味を残す。
    /// - Parameters:
    ///   - cell: 追加する Cell DTO
    ///   - sectionID: 追加先 Section の sectionID
    /// - Returns: 追加した Cell の cellID。`sectionID` が Builder 内に存在しない場合は `nil` (no-op)
    @discardableResult
    @objc public func addLabelCell(_ cell: KsBridgeLabelCell, sectionID: String) -> String? {
        return addCell(cell, sectionID: sectionID)
    }

    /// Builder の現在の内容から Native の `SettingsRoot` を組み立てる。
    /// - Parameter relay: 配下 Cell のユーザー操作を転送する中継
    @MainActor
    internal func makeRoot(relay: KsBridgeInteractionRelay) -> SettingsRoot {
        return SettingsRoot(sections: sections.map { $0.makeSection(relay: relay) })
    }
}
#endif

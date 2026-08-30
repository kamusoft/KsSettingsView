// KsBridgeCell.swift
// KsSettingsViewBridge
//
// interop 境界で Cell を輸送する `@objc` 互換 DTO の共通基底。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

/// interop 境界で Cell を輸送する DTO の共通基底。
///
/// Cell 種ごとに派生 DTO を持ち (maui/ADR-0011)、`KsBridgeSection.cells` や
/// `KsBridgeCellUpdate` はこの基底型で異種 Cell を混載する。ID 採番と全 Cell 共通の
/// 行レイアウトフィールド (title / description / valueText / hintText / icon)・スタイル上書き・
/// 有効性・可視性はこの基底が持つ。
///
/// インスタンス生成時に Bridge が canonical UUID 文字列の `cellID` を採番する。呼び出し側は
/// この `cellID` (Builder / insert 系 API の戻り値と同一) を更新 API へ渡す (maui/ADR-0005)。
///
/// DTO は 1 インスタンスが 1 つの Cell identity を表す。同じインスタンスを複数箇所へ追加すると
/// 同じ `cellID` の Cell が重複するため、Cell ごとに新しいインスタンスを生成する。
///
/// DTO の内容は Bridge の API を呼んだ時点で Store へ写し取られる。呼び出し後に DTO のプロパティを
/// 書き換えても表示は変化しない。
///
/// 基底そのものを Store へ載せた場合は、共通フィールドだけを持つ `LabelCell` として構築される。
@objc(KsBridgeCell)
open class KsBridgeCell: NSObject {

    /// Bridge が採番した canonical UUID 文字列の Cell ID。
    @objc public private(set) var cellID: String

    /// タイトル (必須)
    @objc public var title: String

    /// 説明文 (未指定は `nil`)
    @objc public var descriptionText: String?

    /// 右側に表示する値文字列 (未指定は `nil`)
    @objc public var valueText: String?

    /// ヒントテキスト (未指定は `nil`)
    @objc public var hintText: String?

    /// アイコン画像 (未指定は `nil`)。上位層が解決した platform 画像をそのまま受け取る。
    @objc public var icon: UIImage?

    /// Cell 個別スタイルの上書き (未指定は `nil` で Theme を継承)
    @objc public var style: KsBridgeCellStyle?

    /// 有効／無効フラグ
    @objc public var isEnabled: Bool

    /// 可視性フラグ。`false` の Cell は表示から除外される。
    @objc public var isVisible: Bool

    /// `cellID` に対応する Native の `UUID`。
    internal private(set) var identifier: UUID

    /// タイトルのみを指定して DTO を生成する。
    /// - Parameter title: タイトル
    @objc public convenience init(title: String) {
        self.init(
            title: title,
            descriptionText: nil,
            valueText: nil,
            hintText: nil,
            isEnabled: true,
            isVisible: true
        )
    }

    /// 共通フィールドを指定して DTO を生成する。
    /// - Parameters:
    ///   - title: タイトル
    ///   - descriptionText: 説明文 (未指定は `nil`)
    ///   - valueText: 右寄せ値文字列 (未指定は `nil`)
    ///   - hintText: ヒントテキスト (未指定は `nil`)
    ///   - isEnabled: 有効／無効
    ///   - isVisible: 可視性
    @objc public init(
        title: String,
        descriptionText: String?,
        valueText: String?,
        hintText: String?,
        isEnabled: Bool,
        isVisible: Bool
    ) {
        self.identifier = KsBridgeIdentifier.make()
        self.cellID = KsBridgeIdentifier.string(from: self.identifier)
        self.title = title
        self.descriptionText = descriptionText
        self.valueText = valueText
        self.hintText = hintText
        self.isEnabled = isEnabled
        self.isVisible = isVisible
        super.init()
    }

    /// 既存 Cell の cellID を引き継ぐ。
    ///
    /// Section の内容を差し替えるとき (`replaceSection`) に、配下 Cell の採番済み cellID を
    /// 温存するために使う。採番済みの ID を載せた DTO で差し替えると、差し替え後のユーザー操作
    /// 通知は従前と同じ cellID で届く。
    ///
    /// canonical UUID 文字列として解釈できない値は引き継がず、DTO 自身が採番した ID のままにする。
    /// - Parameter cellID: 引き継ぐ cellID
    /// - Returns: 引き継いだ場合は `true`、解釈できず無視した場合は `false`
    @discardableResult
    @objc public func adoptCellID(_ cellID: String) -> Bool {
        guard let uuid = KsBridgeIdentifier.uuid(from: cellID) else { return false }
        self.identifier = uuid
        self.cellID = KsBridgeIdentifier.string(from: uuid)
        return true
    }

    /// 指定 ID で Native の Cell を組み立てる。
    ///
    /// 内容更新 (`replaceCell` / `replaceCells`) では更新対象の ID を渡し、DTO 自身の
    /// `identifier` ではなく対象の identity を保つ。
    /// - Parameters:
    ///   - id: 生成する Cell の ID
    ///   - relay: ユーザー操作の通知先へ転送する中継
    internal func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        return LabelCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// DTO 自身が採番した ID で Native の Cell を組み立てる。
    /// - Parameter relay: ユーザー操作の通知先へ転送する中継
    internal func makeCell(relay: KsBridgeInteractionRelay) -> any KsCell {
        return makeCell(id: identifier, relay: relay)
    }

    /// スタイル上書きを Native の `CellStyle` へ解決する。未指定なら全項目未指定のスタイル。
    internal var resolvedStyle: CellStyle {
        return style?.resolve() ?? CellStyle()
    }

    /// アイコンを Native の `KsImage` へ包む。未指定なら `nil`。
    internal var resolvedIcon: KsImage? {
        return icon.map { KsImage.uiImage($0) }
    }
}
#endif

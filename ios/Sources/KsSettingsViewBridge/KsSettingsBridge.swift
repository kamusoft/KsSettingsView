// KsSettingsBridge.swift
// KsSettingsViewBridge
//
// interop 境界の入口。内部所有 Store と Native Host を保持し、公開 API を Store 公開操作へ変換する。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

/// interop 境界から設定画面を操作する Bridge。
///
/// Bridge は `SettingsRootStore` を内部に所有し、公開 API を Store の公開操作へ変換する
/// (maui/ADR-0001)。Native Host は Bridge が生成し、内部 Store に接続済みの状態で公開する
/// (maui/ADR-0005)。Bridge は同時に 1 つの Host を持ち、生きている Host がある間は
/// `makeHostViewController()` を繰り返し呼んでも同じ Host を返す。
///
/// `releaseHost()` は Host だけを解放して Store を維持するため、解放後の
/// `makeHostViewController()` は Store 現在状態から表示を復元した新しい Host を返す
/// (maui/ADR-0007)。
///
/// 破棄 (`dispose()`) は冪等で、破棄後の操作 API と Host 生成はすべて no-op になる。破棄後は
/// Store を操作しないため、呼び出し側が保持し続けている Host の表示も変化しない。
///
/// ユーザー操作は `interactionDelegate` へ通知する (maui/ADR-0003)。delegate は弱参照で保持する
/// ため、実装インスタンスの寿命は呼び出し側が保証する。未設定・解除後の通知は破棄される。
///
/// スレッド契約: 全 API を UI スレッド (main actor) から呼ぶ。Bridge 自身は marshal しない
/// (maui/ADR-0005)。
@MainActor
@objc(KsSettingsBridge)
public final class KsSettingsBridge: NSObject {

    /// 内部所有 Store。公開 API はすべてこの Store の公開操作へ変換される。
    internal let store: SettingsRootStore

    /// 生成済みの Native Host。未生成のときは `nil`。
    internal private(set) var hostController: KsSettingsViewController?

    /// 破棄済みかどうか。
    internal private(set) var isDisposed: Bool = false

    /// 現在の見た目スタイル。
    ///
    /// スタイルは Store ではなく Host のプロパティのため、Host を作り直すと失われる。Store が
    /// 設定ツリーと Theme を保つのと同じ生存性を与えるため、Bridge が Host の外で保持し、
    /// Host の生成のたびに適用する。
    internal private(set) var style: KsSettingsViewStyle = .classic

    /// Cell のコールバックと `interactionDelegate` の間に立つ中継。
    ///
    /// Cell へ注入する閉包はこの中継だけを掴むため、delegate の差し替え・解除は生成済みの Cell に
    /// そのまま反映され、閉包が delegate 実装を強く掴むこともない。
    internal let interactionRelay = KsBridgeInteractionRelay()

    /// 空の設定ツリーで Bridge を生成する。
    @objc public override init() {
        self.store = SettingsRootStore(initialRoot: SettingsRoot())
        super.init()
    }

    // MARK: - ユーザー操作の通知

    /// ユーザー操作の通知先 (弱参照)。
    ///
    /// `nil` を設定すると解除でき、以後の操作は通知されない。設定・解除は表示中でも行える。
    @objc public var interactionDelegate: (any KsBridgeInteractionDelegate)? {
        get { interactionRelay.delegate }
        set { interactionRelay.delegate = newValue }
    }

    // MARK: - Native Host

    /// 内部 Store に接続済みの Native Host を返す。
    ///
    /// 生きている Host があればそれを返し、未生成または `releaseHost()` で解放済みなら新しい Host を
    /// 生成して返す。破棄済みの Bridge では `nil` を返す。
    /// Host は接続時点の Store の現在状態から表示を復元するため、`setRoot(_:)` は Host 生成の
    /// 前後どちらで呼んでもよく、解放中に適用した更新も再生成した Host の表示に反映される。
    /// ただし root の header / footer は Store ではなく Host が持つプロパティのため復元されない —
    /// 再生成後も引き継ぐ場合は、呼び出し側が値を保持して `updateAccessory` で再適用する。
    /// - Returns: view 階層へ取り付ける Native Host
    @objc public func makeHostViewController() -> UIViewController? {
        if isDisposed { return nil }
        if let hostController { return hostController }
        let controller = KsSettingsViewController(store: store, style: style)
        hostController = controller
        return controller
    }

    /// Native Host だけを解放し、Store (設定ツリーと Theme) は維持する (maui/ADR-0007)。
    ///
    /// 解放時に旧 Host の Store 購読を解除して無効化するため、解放後に Store へ適用した更新は
    /// 旧 Host の表示に反映されない。旧 Host の view 階層からの取り外しと参照の破棄は呼び出し側の
    /// 責務であり、解放後の Bridge は旧 Host への参照を持たない。
    ///
    /// root の header / footer は Store ではなく Host が持つプロパティのため、解放とともに失われる。
    /// 再生成した Host へ引き継ぐ場合は、呼び出し側が値を保持して `updateAccessory` で再適用する。
    ///
    /// 冪等であり、Host 不在時 (未生成・解放済み) および破棄済みの Bridge では no-op になる。
    @objc public func releaseHost() {
        guard !isDisposed, let controller = hostController else { return }
        controller.disconnectStore()
        hostController = nil
    }

    // MARK: - lifecycle

    /// Bridge を破棄する。冪等であり、破棄後の操作 API と Host 生成は no-op になる。
    ///
    /// 破棄と同時に `interactionDelegate` を解除するため、破棄後のユーザー操作は通知されない。
    @objc public func dispose() {
        if isDisposed { return }
        isDisposed = true
        hostController = nil
        interactionRelay.delegate = nil
    }

    // MARK: - Root 全体操作

    /// Builder が組み立てた設定ツリーで root を全置換する。
    /// - Parameter builder: 設定ツリーの Builder
    @objc public func setRoot(_ builder: KsBridgeRootBuilder) {
        guard !isDisposed else { return }
        store.replaceAll(builder.makeRoot(relay: interactionRelay))
    }

    // MARK: - Section 操作

    /// Section を指定 index へ挿入する。index は model 配列上の位置で、範囲外は端へ丸められる。
    /// - Parameters:
    ///   - section: 挿入する Section DTO
    ///   - index: 挿入位置
    /// - Returns: 挿入した Section の sectionID。破棄済みの Bridge では `nil`
    @discardableResult
    @objc public func insertSection(_ section: KsBridgeSection, at index: Int) -> String? {
        guard !isDisposed else { return nil }
        store.insertSection(section.makeSection(relay: interactionRelay), at: index)
        return section.sectionID
    }

    /// 指定 ID の Section を削除する。未知の ID は no-op。
    /// - Parameter sectionID: 対象 Section の sectionID
    @objc public func removeSection(sectionID: String) {
        guard !isDisposed, let uuid = KsBridgeIdentifier.uuid(from: sectionID) else { return }
        store.removeSection(sectionID: uuid)
    }

    /// Section の順序を変更する。index は model 配列上の位置で、範囲外は端へ丸められる。
    /// - Parameters:
    ///   - from: 移動元 index
    ///   - to: 移動先 index
    @objc public func moveSection(from: Int, to: Int) {
        guard !isDisposed else { return }
        store.moveSection(from: from, to: to)
    }

    /// 指定 ID の Section の内容を置換し、置換後も有効な sectionID を返す。未知の ID は no-op。
    ///
    /// 置換後も Section の identity は `sectionID` のまま保たれる。`newSection` 自身が採番した
    /// `sectionID` は破棄されるため、以後の操作には戻り値の ID を使う。Section 内の Cell は
    /// DTO が持つ ID で作り直されるため、既存 Cell の ID を温存したい場合は Cell DTO に
    /// `adoptCellID(_:)` で採番済みの ID を引き継がせてから渡す。
    /// - Parameters:
    ///   - sectionID: 対象 Section の sectionID
    ///   - newSection: 置換後の内容
    /// - Returns: 置換後も有効な sectionID (対象と同じ ID)。破棄済み、または対象 Section が
    ///   存在しない場合は `nil` (no-op)
    @discardableResult
    @objc public func replaceSection(sectionID: String, newSection: KsBridgeSection) -> String? {
        guard !isDisposed, let uuid = KsBridgeIdentifier.uuid(from: sectionID) else { return nil }
        guard store.root.sections.contains(where: { $0.id == uuid }) else { return nil }
        store.replaceSection(sectionID: uuid, new: newSection.makeSection(id: uuid, relay: interactionRelay))
        return KsBridgeIdentifier.string(from: uuid)
    }

    // MARK: - Cell 操作

    /// 指定 Section の指定 index へ Cell を挿入する。index は model 配列上の位置で、
    /// 範囲外は端へ丸められる。
    /// - Parameters:
    ///   - cell: 挿入する Cell DTO
    ///   - sectionID: 挿入先 Section の sectionID
    ///   - index: 挿入位置
    /// - Returns: 挿入した Cell の cellID。破棄済み、または Section が存在しない場合は `nil` (no-op)
    @discardableResult
    @objc public func insertCell(_ cell: KsBridgeCell, sectionID: String, at index: Int) -> String? {
        guard !isDisposed, let uuid = KsBridgeIdentifier.uuid(from: sectionID) else { return nil }
        guard store.root.sections.contains(where: { $0.id == uuid }) else { return nil }
        store.insertCell(cell.makeCell(relay: interactionRelay), in: uuid, at: index)
        return cell.cellID
    }

    /// 指定 ID の Cell を削除する。未知の ID は no-op。
    /// - Parameter cellID: 対象 Cell の cellID
    @objc public func removeCell(cellID: String) {
        guard !isDisposed, let uuid = KsBridgeIdentifier.uuid(from: cellID) else { return }
        store.removeCell(cellID: KsCellID(id: uuid))
    }

    /// 指定 ID の Cell を同一 Section 内で移動する。未知の ID は no-op。
    /// index は model 配列上の位置で、範囲外は端へ丸められる。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - index: 移動先 index
    @objc public func moveCell(cellID: String, to index: Int) {
        guard !isDisposed, let uuid = KsBridgeIdentifier.uuid(from: cellID) else { return }
        store.moveCell(cellID: KsCellID(id: uuid), to: index)
    }

    /// 指定 ID の Cell の内容を置換し、置換後も有効な cellID を返す。未知の ID は no-op。
    ///
    /// 置換後も行の identity は `cellID` のまま保たれ、行の削除と挿入としては扱われない。
    /// `newCell` 自身が採番した `cellID` は破棄されるため、以後の操作には戻り値の ID を使う。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - newCell: 置換後の内容
    /// - Returns: 置換後も有効な cellID (対象と同じ ID)。破棄済み、または対象 Cell が存在しない
    ///   場合は `nil` (no-op)
    @discardableResult
    @objc public func replaceCell(cellID: String, newCell: KsBridgeCell) -> String? {
        guard !isDisposed, let uuid = KsBridgeIdentifier.uuid(from: cellID) else { return nil }
        guard store.root.sections.contains(where: { $0.cells.contains { $0.id == uuid } }) else {
            return nil
        }
        store.replaceCell(cellID: KsCellID(id: uuid), new: newCell.makeCell(id: uuid, relay: interactionRelay))
        return KsBridgeIdentifier.string(from: uuid)
    }

    /// 複数 Cell の内容をまとめて置換し、1 回のバッチ内容更新として反映する。
    ///
    /// 更新は入力順に適用され、未知の ID は無視される。適用が 0 件のときは状態も表示も変化しない。
    /// 各更新は同じ ID の内容更新であり、行の identity を変えない。可視性を変える更新は
    /// バッチではなく `replaceCell(cellID:newCell:)` で行う。
    /// - Parameter updates: (対象 cellID, 置換後の内容) の並び
    @objc public func replaceCells(_ updates: [KsBridgeCellUpdate]) {
        guard !isDisposed else { return }
        let resolved: [(cellID: KsCellID, new: any KsCell)] = updates.compactMap { update in
            guard let uuid = KsBridgeIdentifier.uuid(from: update.cellID) else { return nil }
            return (cellID: KsCellID(id: uuid), new: update.cell.makeCell(id: uuid, relay: interactionRelay))
        }
        store.replaceCells(resolved)
    }

    // MARK: - Accessory / Theme 操作

    /// Root / Section の header・footer に表示する text を更新する。
    ///
    /// `text` が `nil` のときは accessory を解除し、accessory が指定されていない場合と同じ表示に戻す。
    /// `sectionHeader` / `sectionFooter` を指定するときは `sectionID` が必須で、canonical UUID
    /// 文字列として解釈できない場合は no-op になる。canonical UUID でも Store の現在状態に
    /// 存在しない sectionID は Store 側で no-op になり、状態・表示・通知は変化しない (core/ADR-0020)。
    ///
    /// Section 対象の text は Store の状態に保存され Host 再生成後も復元されるが、root 対象の
    /// text は Store ではなく Host が持つため、`releaseHost()` 後の再生成には引き継がれない —
    /// 引き継ぐ場合は呼び出し側が値を保持して再適用する。
    /// - Parameters:
    ///   - target: 更新対象
    ///   - sectionID: Section を対象にするときの sectionID (root 対象では参照しない)
    ///   - text: 表示する text (`nil` で解除)
    @objc public func updateAccessory(
        target: KsBridgeAccessoryTarget,
        sectionID: String?,
        text: String?
    ) {
        guard !isDisposed else { return }
        switch target {
        case .rootHeader:
            store.updateAccessory(target: .rootHeader, accessory: text.map { .root(.text($0)) })
        case .rootFooter:
            store.updateAccessory(target: .rootFooter, accessory: text.map { .root(.text($0)) })
        case .sectionHeader:
            guard let uuid = KsBridgeIdentifier.uuid(from: sectionID) else { return }
            store.updateAccessory(
                target: .sectionHeader(sectionID: uuid),
                accessory: text.map { .section(.text($0)) }
            )
        case .sectionFooter:
            guard let uuid = KsBridgeIdentifier.uuid(from: sectionID) else { return }
            store.updateAccessory(
                target: .sectionFooter(sectionID: uuid),
                accessory: text.map { .section(.text($0)) }
            )
        }
    }

    /// Root / Section の header・footer に表示する view を更新する。
    ///
    /// `view` が `nil` のときは accessory を解除し、accessory が指定されていない場合と同じ表示に
    /// 戻す。渡した view は取り付け直前に既存の親から切り離されるため、同じインスタンスが
    /// リサイクル等で再び取り付けられても失敗しない。
    ///
    /// 対象の指定と未知 sectionID の扱いは `updateAccessory(target:sectionID:text:)` と同一で、
    /// Section 対象の view は Store の状態に保存され Host 再生成後も復元されるが、root 対象の
    /// view は Host が持つため引き継がれない。
    /// - Parameters:
    ///   - target: 更新対象
    ///   - sectionID: Section を対象にするときの sectionID (root 対象では参照しない)
    ///   - view: 表示する view (`nil` で解除)
    @objc public func updateAccessoryView(
        target: KsBridgeAccessoryTarget,
        sectionID: String?,
        view: UIView?
    ) {
        guard !isDisposed else { return }
        let anyView = view.map { KsBridgeAccessoryView.anyView($0) }
        switch target {
        case .rootHeader:
            store.updateAccessory(target: .rootHeader, accessory: anyView.map { .root(.view($0)) })
        case .rootFooter:
            store.updateAccessory(target: .rootFooter, accessory: anyView.map { .root(.view($0)) })
        case .sectionHeader:
            guard let uuid = KsBridgeIdentifier.uuid(from: sectionID) else { return }
            store.updateAccessory(
                target: .sectionHeader(sectionID: uuid),
                accessory: anyView.map { .section(.view($0)) }
            )
        case .sectionFooter:
            guard let uuid = KsBridgeIdentifier.uuid(from: sectionID) else { return }
            store.updateAccessory(
                target: .sectionFooter(sectionID: uuid),
                accessory: anyView.map { .section(.view($0)) }
            )
        }
    }

    /// 表示中の accessory 領域の高さを測り直すよう要求する。
    ///
    /// view accessory の中身が自分の計測結果を変えても、Native は領域の高さを自動では測り直さない。
    /// 中身の所有者が変化を知った時点で本 API を呼ぶと、対象の領域だけが再計測される。
    ///
    /// 一過性の要求であり Store の状態は変化しない。対象が表示されていないとき、および固定高さの
    /// Section header では表示が変わらない。
    /// - Parameters:
    ///   - target: 再計測する accessory
    ///   - sectionID: Section を対象にするときの sectionID (root 対象では参照しない)
    @objc public func invalidateAccessoryMeasurement(
        target: KsBridgeAccessoryTarget,
        sectionID: String?
    ) {
        guard !isDisposed else { return }
        switch target {
        case .rootHeader:
            store.invalidateAccessoryMeasurement(target: .rootHeader)
        case .rootFooter:
            store.invalidateAccessoryMeasurement(target: .rootFooter)
        case .sectionHeader:
            guard let uuid = KsBridgeIdentifier.uuid(from: sectionID) else { return }
            store.invalidateAccessoryMeasurement(target: .sectionHeader(sectionID: uuid))
        case .sectionFooter:
            guard let uuid = KsBridgeIdentifier.uuid(from: sectionID) else { return }
            store.invalidateAccessoryMeasurement(target: .sectionFooter(sectionID: uuid))
        }
    }

    /// Theme を適用する。同値の Theme を再指定した場合は更新が通知されない。
    /// - Parameter theme: 輸送 DTO の Theme
    @objc public func setTheme(_ theme: KsBridgeTheme) {
        guard !isDisposed else { return }
        store.applyTheme(theme.resolve())
    }

    // MARK: - 見た目スタイル

    /// 見た目スタイルを適用する。
    ///
    /// スタイルは Store を経由せず Host のプロパティへ直接適用する — Native 側でもスタイルは
    /// Store の管理外にあり、この操作だけが Store 公開操作との 1 対 1 (maui/ADR-0002) の枠外に
    /// なる (maui/ADR-0023)。Host 未生成のときは値を控え、次の Host 生成時に適用する。
    /// - Parameter style: 見た目スタイルの序数 (classic = 0 / modern = 1)。定義域外は classic
    @objc public func setStyle(_ style: Int) {
        guard !isDisposed else { return }
        let resolved = KsBridgeStyle.style(from: style)
        self.style = resolved
        hostController?.style = resolved
    }
}
#endif

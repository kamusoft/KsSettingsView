// KsBridgeTestHost.swift
// KsSettingsViewBridgeTests
//
// Bridge が生成した Native Host を window に載せ、実描画された内容を読み出すテスト用ユーティリティ。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

/// Bridge のテストで表示内容を観察するためのユーティリティ。
///
/// Bridge の公開 API 経由で Host を生成し、window に載せて実描画を確定させる。検証は内部状態では
/// なく、実描画された行タイトルと header / footer のテキストで行う。
@MainActor
internal enum KsBridgeTestHost {

    /// window に載せた Host を保持する。window を強参照で保持しないと描画が確定しないため、
    /// テスト側で最後まで持ち続ける。
    @MainActor
    internal struct Attachment {
        let controller: KsSettingsViewController
        let window: UIWindow

        var collectionView: UICollectionView { controller.internalCollectionView }
    }

    /// Bridge から Host を生成し、window に載せて実描画を確定させる。
    /// - Parameter bridge: 対象 Bridge
    static func attach(
        _ bridge: KsSettingsBridge,
        file: StaticString = #filePath,
        line: UInt = #line
    ) -> Attachment {
        guard let controller = bridge.makeHostViewController() as? KsSettingsViewController else {
            fatalError("Bridge が Native Host を返さなかった")
        }
        let size = CGSize(width: 375, height: 800)
        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: size)
        let window = UIWindow(frame: rootView.frame)
        window.addSubview(rootView)
        window.makeKeyAndVisible()
        rootView.layoutIfNeeded()
        let attachment = Attachment(controller: controller, window: window)
        attachment.collectionView.frame = CGRect(origin: .zero, size: size)
        awaitInitialRender(attachment, file: file, line: line)
        return attachment
    }

    /// 初期スナップショットが実描画されるまで待つ。
    ///
    /// 期待する Section 構造は visible projection から、Section に属さない Root accessory の
    /// boundary supplementary は controller の設定から求め、行と supplementary の実体化まで
    /// `awaitCollectionRender` が待つ。
    private static func awaitInitialRender(
        _ attachment: Attachment,
        file: StaticString,
        line: UInt
    ) {
        let controller = attachment.controller
        let expected = KsSettingsViewController.computeVisibleSections(from: controller.root.sections)
        awaitCollectionRender(
            attachment.collectionView,
            "Host attach 後の初期スナップショット反映",
            expectedItemCounts: expected.map(\.cells.count),
            requiredSupplementaryKinds: expectedRootSupplementaryKinds(controller),
            file: file,
            line: line
        )
    }

    /// 設定済みの Root accessory から、実体化していなければならない boundary supplementary の
    /// elementKind を求める。
    ///
    /// Root accessory は layout 全体の boundary supplementary であり、どの Section にも属さない。
    /// Section 構造からは存在を導けないため、controller の設定から明示的に列挙する。
    private static func expectedRootSupplementaryKinds(
        _ controller: KsSettingsViewController
    ) -> [String] {
        var kinds: [String] = []
        if controller.rootHeader != nil {
            kinds.append(KsSettingsViewController.rootHeaderElementKind)
        }
        if controller.rootFooter != nil {
            kinds.append(KsSettingsViewController.rootFooterElementKind)
        }
        return kinds
    }

    // MARK: - 収束待ち

    /// 観測した view が期待インスタンスと同一になるまで待つ。
    ///
    /// accessory の attach・置換・リサイクル後の再バインドは実描画の周回を挟んで完了するため、
    /// 同一性が成立したことをその完了条件として待つ。
    static func awaitSameView(
        _ attachment: Attachment,
        _ description: String,
        is expected: UIView,
        file: StaticString = #filePath,
        line: UInt = #line,
        observe: () -> UIView?
    ) {
        awaitCondition(
            description,
            in: attachment.collectionView,
            actual: { describe(observe()) },
            file: file,
            line: line,
            until: { observe() === expected }
        )
    }

    /// 実描画された行タイトルが期待どおりになるまで待つ。
    static func awaitRenderedTitles(
        _ attachment: Attachment,
        equals expected: [[String]],
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitEqual(
            "実描画された行タイトル",
            expected: expected,
            in: attachment.collectionView,
            file: file,
            line: line,
            actual: { renderedTitles(attachment) }
        )
    }

    /// 指定 Section の header に実描画されたテキストが期待どおりになるまで待つ。
    static func awaitHeaderText(
        _ attachment: Attachment,
        section: Int,
        equals expected: String?,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitEqual(
            "section \(section) の header テキスト",
            expected: expected,
            in: attachment.collectionView,
            file: file,
            line: line,
            actual: { headerText(attachment, section: section) }
        )
    }

    /// view を失敗メッセージ用に識別できる形へ整形する。
    static func describe(_ view: UIView?) -> String {
        guard let view else { return "nil" }
        return "\(type(of: view))(\(UInt(bitPattern: ObjectIdentifier(view).hashValue)))"
    }

    /// Section ごとに実描画された行タイトルを返す。
    static func renderedTitles(_ attachment: Attachment) -> [[String]] {
        let cv = attachment.collectionView
        return (0..<cv.numberOfSections).map { section in
            (0..<cv.numberOfItems(inSection: section)).map { item in
                let cell = cv.cellForItem(at: IndexPath(item: item, section: section))
                return (cell as? KsListCellBase)?.titleLabel.text ?? ""
            }
        }
    }

    /// 指定 Section の header に実描画されたテキストを返す。
    static func headerText(_ attachment: Attachment, section: Int) -> String? {
        return accessoryText(attachment, kind: UICollectionView.elementKindSectionHeader, section: section)
    }

    /// 指定 Section の footer に実描画されたテキストを返す。
    static func footerText(_ attachment: Attachment, section: Int) -> String? {
        return accessoryText(attachment, kind: UICollectionView.elementKindSectionFooter, section: section)
    }

    /// 指定 Section の header に実描画された accessory view を返す。
    ///
    /// text accessory のときは `UILabel` が返るため、interop 経由で渡した view と同一かどうかは
    /// 呼び出し側がインスタンス比較で判定する。
    static func headerAccessoryView(_ attachment: Attachment, section: Int) -> UIView? {
        return accessoryHostedView(
            attachment,
            kind: UICollectionView.elementKindSectionHeader,
            section: section
        )
    }

    /// 指定 Section の footer に実描画された accessory view を返す。
    static func footerAccessoryView(_ attachment: Attachment, section: Int) -> UIView? {
        return accessoryHostedView(
            attachment,
            kind: UICollectionView.elementKindSectionFooter,
            section: section
        )
    }

    /// Root header に実描画された accessory view を返す。
    static func rootHeaderAccessoryView(_ attachment: Attachment) -> UIView? {
        return rootAccessoryHostedView(attachment, kind: KsSettingsViewController.rootHeaderElementKind)
    }

    /// Root footer に実描画された accessory view を返す。
    static func rootFooterAccessoryView(_ attachment: Attachment) -> UIView? {
        return rootAccessoryHostedView(attachment, kind: KsSettingsViewController.rootFooterElementKind)
    }

    /// supplementary に実描画された accessory view を返す。
    private static func accessoryHostedView(
        _ attachment: Attachment,
        kind: String,
        section: Int
    ) -> UIView? {
        let supplementary = attachment.collectionView.supplementaryView(
            forElementKind: kind,
            at: IndexPath(item: 0, section: section)
        )
        guard let listCell = supplementary as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.first
    }

    /// Root の boundary supplementary に実描画された accessory view を返す。
    private static func rootAccessoryHostedView(_ attachment: Attachment, kind: String) -> UIView? {
        let supplementary = attachment.collectionView.visibleSupplementaryViews(ofKind: kind).first
        guard let listCell = supplementary as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.first
    }

    /// supplementary に実描画されたテキストを返す。
    private static func accessoryText(
        _ attachment: Attachment,
        kind: String,
        section: Int
    ) -> String? {
        let cv = attachment.collectionView
        let supplementary = cv.supplementaryView(
            forElementKind: kind,
            at: IndexPath(item: 0, section: section)
        )
        guard let listCell = supplementary as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.compactMap { ($0 as? UILabel)?.text }.first
    }
}
#endif

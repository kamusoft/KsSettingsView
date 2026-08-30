// KsBridgeTestHost.swift
// KsSettingsViewBridgeTests
//
// Bridge が生成した Native Host を window に載せ、実描画された内容を読み出すテスト用ユーティリティ。

#if canImport(UIKit)
import Foundation
import UIKit
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
    static func attach(_ bridge: KsSettingsBridge) -> Attachment {
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
        pump(attachment)
        return attachment
    }

    /// レイアウトと再構成を確定させる。
    static func pump(_ attachment: Attachment, seconds: TimeInterval = 0.05) {
        let view = attachment.collectionView
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
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

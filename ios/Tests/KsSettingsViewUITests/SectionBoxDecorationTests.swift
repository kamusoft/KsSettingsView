// SectionBoxDecorationTests.swift
// KsSettingsViewUITests
//
// Modern の Section 装飾（Theme 4 属性の解決・箱の描画範囲・合成契約・罫線規則）と、
// Classic への sectionMargin 上下適用を検証する。
//
// 箱は compositional layout の decoration として描かれるため、観測点は
// `layoutAttributesForDecorationView(ofKind:at:)` が返す `SectionBoxAttributes` と、
// 同じ layout が返す item / supplementary の frame との関係に取る。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

/// `KsAnyView` の factory が何度呼ばれたかを数える。値型のクロージャ捕捉で共有するため参照型にする。
@MainActor
private final class FactoryCallCounter {
    var count = 0
}

@MainActor
final class SectionBoxDecorationTests: XCTestCase {
    private static let viewSize = CGSize(width: 375, height: 700)

    // MARK: - ハーネス

    /// controller を window に載せて実レイアウトを走らせる。
    private func host(
        root: SettingsRoot,
        theme: Theme = Theme(),
        style: KsSettingsViewStyle
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(root: root, theme: theme, style: style)
        let window = UIWindow(frame: CGRect(origin: .zero, size: Self.viewSize))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: Self.viewSize)
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: Self.viewSize)
        awaitInitialRender(controller)
        return (controller, cv, window)
    }

    private func boxAttributes(_ cv: UICollectionView, section: Int) -> SectionBoxAttributes? {
        return cv.collectionViewLayout.layoutAttributesForDecorationView(
            ofKind: SectionBoxLayout.decorationKind,
            at: IndexPath(item: 0, section: section)
        ) as? SectionBoxAttributes
    }

    private func itemFrame(_ cv: UICollectionView, section: Int, item: Int) -> CGRect? {
        return cv.collectionViewLayout
            .layoutAttributesForItem(at: IndexPath(item: item, section: section))?.frame
    }

    private func supplementaryFrame(
        _ cv: UICollectionView,
        kind: String,
        section: Int
    ) -> CGRect? {
        return cv.collectionViewLayout.layoutAttributesForSupplementaryView(
            ofKind: kind,
            at: IndexPath(item: 0, section: section)
        )?.frame
    }

    /// Root Header / Footer を載せた controller を window に置いて実レイアウトを走らせる。
    ///
    /// `awaitInitialRender` は controller に設定済みの Root accessory の boundary supplementary が
    /// 実体化するまで待つため、戻った直後に Root accessory の実体を読んでよい。可視 Section が
    /// 0 件の構成でもこの待機は効く。
    private func hostWithRootAccessories(
        root: SettingsRoot,
        theme: Theme = Theme(),
        style: KsSettingsViewStyle,
        rootHeader: RootAccessory?,
        rootFooter: RootAccessory?
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(root: root, theme: theme, style: style)
        controller.rootHeader = rootHeader
        controller.rootFooter = rootFooter
        let window = UIWindow(frame: CGRect(origin: .zero, size: Self.viewSize))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: Self.viewSize)
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: Self.viewSize)
        awaitInitialRender(controller)
        return (controller, cv, window)
    }

    /// Root accessory のテキスト内容（UILabel）の矩形を collection view 座標で返す。
    ///
    /// Root accessory の item 自体は Section 単位の余白を内側に含むため、余白の位置を確かめるには
    /// 内容の矩形を見る必要がある。
    private func rootAccessoryContentFrame(_ cv: UICollectionView, kind: String) -> CGRect? {
        guard let cell = cv.visibleSupplementaryViews(ofKind: kind).first as? UICollectionViewListCell,
              let label = cell.contentView.subviews.compactMap({ $0 as? UILabel }).first else {
            return nil
        }
        return label.convert(label.bounds, to: cv)
    }

    private func separator(
        _ controller: KsSettingsViewController,
        section: Int,
        item: Int
    ) -> UIListSeparatorConfiguration {
        return controller.separatorConfiguration(
            for: IndexPath(item: item, section: section),
            base: UIListSeparatorConfiguration(listAppearance: .plain)
        )
    }

    // MARK: - Theme の Section 装飾 4 属性

    func test_4属性の既定はnilで未指定を表す() {
        let theme = Theme()
        XCTAssertNil(theme.sectionMargin)
        XCTAssertNil(theme.sectionCornerRadius)
        XCTAssertNil(theme.sectionBorderWidth)
        XCTAssertNil(theme.sectionBorderColor)
    }

    func test_未指定のModernはライブラリ既定の余白と角丸へ解決しボーダーは実効0() {
        let metrics = SectionBoxMetrics.resolve(theme: Theme(), style: .modern)
        XCTAssertEqual(metrics.margin, SectionBoxMetrics.modernDefaultMargin)
        XCTAssertEqual(metrics.cornerRadius, SectionBoxMetrics.modernDefaultCornerRadius)
        XCTAssertEqual(metrics.borderWidth, 0)
        XCTAssertTrue(metrics.borderColor.isEqual(UIColor.clear))
    }

    func test_未指定のClassicは既定余白の上下だけを採りボーダーも角丸も持たない() {
        // `classicDefaultMargin` は `modernDefaultMargin` と完全同値に置かれているが、
        // `resolve` が `.classic` で水平成分を落とすため実効値は上下だけになる。
        // 定数との一致ではなく実効値そのものを検証する。
        let metrics = SectionBoxMetrics.resolve(theme: Theme(), style: .classic)
        XCTAssertEqual(metrics.margin.top, 22)
        XCTAssertEqual(metrics.margin.bottom, 0)
        XCTAssertEqual(metrics.margin.leading, 0, "Classic の Section 境界は全幅でなければならない")
        XCTAssertEqual(metrics.margin.trailing, 0)
        XCTAssertEqual(metrics.cornerRadius, 0)
        XCTAssertEqual(metrics.borderWidth, 0)
    }

    func test_Classicは指定してもleadingとtrailingを無視する() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 8, leading: 30, bottom: 9, trailing: 40)
        )
        let metrics = SectionBoxMetrics.resolve(theme: theme, style: .classic)
        XCTAssertEqual(metrics.margin.top, 8)
        XCTAssertEqual(metrics.margin.bottom, 9)
        XCTAssertEqual(metrics.margin.leading, 0, "Classic の Section 境界は全幅でなければならない")
        XCTAssertEqual(metrics.margin.trailing, 0)
    }

    func test_負の寸法は0として扱う() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: -10, leading: -1, bottom: -3, trailing: -4),
            sectionCornerRadius: -5,
            sectionBorderWidth: -2
        )
        let metrics = SectionBoxMetrics.resolve(theme: theme, style: .modern)
        XCTAssertEqual(metrics.margin, NSDirectionalEdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
        XCTAssertEqual(metrics.cornerRadius, 0)
        XCTAssertEqual(metrics.borderWidth, 0)
    }

    func test_負の値を持つThemeでもModernの表示が破綻しない() {
        // 負の成分を含む sectionMargin と負の sectionBorderWidth を与えても、
        // 0 指定と同じ描画結果になる。
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: -10, leading: -8, bottom: -6, trailing: -4),
            sectionBorderWidth: -3
        )
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), theme: theme, style: .modern)
        defer { window.isHidden = true }

        guard let box = boxAttributes(cv, section: 0), let first = itemFrame(cv, section: 0, item: 0) else {
            return XCTFail("箱または Cell の属性が取得できない")
        }
        XCTAssertEqual(box.borderWidth, 0)
        XCTAssertEqual(first.minX, 0, accuracy: 0.5, "負の余白は 0 と同じ位置に解決されなければならない")
        XCTAssertEqual(box.frame.width, Self.viewSize.width, accuracy: 0.5)
    }

    func test_非有限の寸法は0として扱う() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(
                top: .nan,
                leading: .infinity,
                bottom: -.infinity,
                trailing: .nan
            ),
            sectionCornerRadius: .infinity,
            sectionBorderWidth: .nan
        )
        let metrics = SectionBoxMetrics.resolve(theme: theme, style: .modern)
        XCTAssertEqual(metrics.margin, NSDirectionalEdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
        XCTAssertEqual(metrics.cornerRadius, 0)
        XCTAssertEqual(metrics.borderWidth, 0)
    }

    func test_非有限の値を持つThemeでもModernの表示が破綻しない() {
        // NaN・±∞ を含む sectionMargin と非有限の角丸・ボーダー幅を与えても、
        // 0 指定と同じ描画結果になる。
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(
                top: .infinity,
                leading: .infinity,
                bottom: .nan,
                trailing: .nan
            ),
            sectionCornerRadius: .infinity,
            sectionBorderWidth: .infinity
        )
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), theme: theme, style: .modern)
        defer { window.isHidden = true }

        guard let box = boxAttributes(cv, section: 0), let first = itemFrame(cv, section: 0, item: 0) else {
            return XCTFail("箱または Cell の属性が取得できない")
        }
        XCTAssertEqual(box.borderWidth, 0)
        XCTAssertEqual(box.cornerRadius, 0, "非有限の角丸半径がそのまま描画へ渡っている")
        XCTAssertEqual(first.minX, 0, accuracy: 0.5, "非有限の余白は 0 と同じ位置に解決されなければならない")
        XCTAssertEqual(box.frame.width, Self.viewSize.width, accuracy: 0.5)
        XCTAssertTrue(box.frame.origin.y.isFinite, "箱の位置が非有限のまま描画へ渡っている")
    }

    func test_角丸半径は箱の短辺の半分へclampされる() {
        let metrics = SectionBoxMetrics.resolve(theme: Theme(sectionCornerRadius: 500), style: .modern)
        XCTAssertEqual(metrics.clampedCornerRadius(for: CGSize(width: 300, height: 44)), 22)
        XCTAssertEqual(metrics.clampedCornerRadius(for: CGSize(width: 0, height: 0)), 0)
    }

    /// clamp の観測点を実描画経路（decoration view の layer）に置く。
    func test_箱のdecorationViewの角丸が箱の短辺の半分へclampされる() {
        // 1 行だけの Section に過大な半径を与え、箱の高さで clamp されることを実物で見る。
        let theme = Theme(cellBackgroundColor: .white, sectionCornerRadius: 500)
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), theme: theme, style: .modern)
        defer { window.isHidden = true }

        guard let box = boxAttributes(cv, section: 0),
              let decoration = cv.subviews.compactMap({ $0 as? SectionBoxDecorationView }).first else {
            return XCTFail("箱の decoration view が生成されていない")
        }
        let expected = SectionBoxMetrics.clampedCornerRadius(500, for: box.frame.size)
        XCTAssertEqual(decoration.layer.cornerRadius, expected, accuracy: 0.01)
        XCTAssertLessThan(decoration.layer.cornerRadius, 500,
                          "過大な半径がそのまま描画へ渡っている")
    }

    func test_4属性はThemeの値等価性に参加する() {
        let base = Theme()
        XCTAssertNotEqual(
            base,
            Theme(sectionMargin: NSDirectionalEdgeInsets(top: 1, leading: 0, bottom: 0, trailing: 0))
        )
        XCTAssertNotEqual(base, Theme(sectionCornerRadius: 4))
        XCTAssertNotEqual(base, Theme(sectionBorderWidth: 1))
        XCTAssertNotEqual(base, Theme(sectionBorderColor: .red))
        XCTAssertEqual(
            Theme(sectionCornerRadius: 4, sectionBorderWidth: 1, sectionBorderColor: .red),
            Theme(sectionCornerRadius: 4, sectionBorderWidth: 1, sectionBorderColor: .red)
        )
    }

    func test_未指定のThemeでModernを表示すると既定の余白と角丸で箱が描かれボーダーは出ない() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }

        guard let box = boxAttributes(cv, section: 0) else {
            return XCTFail("Modern で箱の装飾属性が生成されていない")
        }
        let margin = SectionBoxMetrics.modernDefaultMargin
        XCTAssertEqual(box.frame.minX, margin.leading, accuracy: 0.5)
        XCTAssertEqual(box.frame.maxX, Self.viewSize.width - margin.trailing, accuracy: 0.5)
        XCTAssertEqual(box.cornerRadius, SectionBoxMetrics.modernDefaultCornerRadius)
        XCTAssertEqual(box.borderWidth, 0, "既定の Modern にボーダーは描かれない")
    }

    func test_指定値が箱の描画へ反映される() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 30, leading: 24, bottom: 10, trailing: 24),
            sectionCornerRadius: 8,
            sectionBorderWidth: 3,
            sectionBorderColor: .red
        )
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), theme: theme, style: .modern)
        defer { window.isHidden = true }

        guard let box = boxAttributes(cv, section: 0) else {
            return XCTFail("箱の装飾属性が生成されていない")
        }
        XCTAssertEqual(box.frame.minX, 24, accuracy: 0.5)
        XCTAssertEqual(box.frame.maxX, Self.viewSize.width - 24, accuracy: 0.5)
        XCTAssertEqual(cv.contentInset.top, 30, accuracy: 0.5, "先頭 Section の top 余白が list 端に対して効いていない")
        XCTAssertEqual(cv.contentInset.bottom, 10, accuracy: 0.5)
        XCTAssertEqual(box.cornerRadius, 8)
        XCTAssertEqual(box.borderWidth, 3)
        XCTAssertTrue(box.borderColor.isEqual(UIColor.red))
    }

    func test_箱の塗り色はcellBackgroundColorから解決する() {
        let theme = Theme(cellBackgroundColor: .green)
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), theme: theme, style: .modern)
        defer { window.isHidden = true }

        XCTAssertTrue(boxAttributes(cv, section: 0)?.boxBackgroundColor.isEqual(UIColor.green) ?? false)
    }

    func test_実行時のTheme変更が装飾へ反映されidentityは維持される() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let (controller, cv, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }
        let sectionIDsBefore = controller.internalDataSource?.snapshot().sectionIdentifiers
        let itemIDsBefore = controller.internalDataSource?.snapshot().itemIdentifiers
        XCTAssertEqual(boxAttributes(cv, section: 0)?.cornerRadius, SectionBoxMetrics.modernDefaultCornerRadius)

        // sectionCornerRadius だけが異なる Theme を適用する
        controller.applyTheme(Theme(sectionCornerRadius: 4))
        awaitEqual(
            "Theme 変更後の箱の角丸",
            expected: CGFloat(4) as CGFloat?,
            in: cv,
            actual: { boxAttributes(cv, section: 0)?.cornerRadius }
        )

        XCTAssertEqual(boxAttributes(cv, section: 0)?.cornerRadius, 4)
        XCTAssertEqual(controller.internalDataSource?.snapshot().sectionIdentifiers, sectionIDsBefore)
        XCTAssertEqual(controller.internalDataSource?.snapshot().itemIdentifiers, itemIDsBefore)
        // 先頭 Cell の clip も新しい角丸で掛け直される
        guard let cell = cv.cellForItem(at: IndexPath(item: 0, section: 0)),
              let mask = cell.layer.mask as? CAShapeLayer,
              let cgPath = mask.path else {
            return XCTFail("Theme 変更後に先頭 Cell の clip が失われている")
        }
        let path = UIBezierPath(cgPath: cgPath)
        // 半径 4 の角丸では、旧半径（26）で切られていた位置は塗りの内側に戻る
        XCTAssertTrue(path.contains(CGPoint(x: 6, y: 6)), "clip が旧い角丸半径のまま残っている")
        XCTAssertFalse(path.contains(CGPoint(x: 0.5, y: 0.5)), "新しい角丸で角が切られていない")
    }

    func test_sectionMarginはHeaderとFooterを含むSection単位を包む() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 20, leading: 16, bottom: 12, trailing: 16)
        )
        let s0 = KsSettingsViewCore.Section(
            header: .text("H0"),
            footer: .text("F0"),
            cells: [LabelCell(title: "A")]
        )
        let s1 = KsSettingsViewCore.Section(
            header: .text("H1"),
            footer: .text("F1"),
            cells: [LabelCell(title: "B")]
        )
        let (_, cv, window) = host(root: SettingsRoot(sections: [s0, s1]), theme: theme, style: .modern)
        defer { window.isHidden = true }

        guard let header0 = supplementaryFrame(cv, kind: UICollectionView.elementKindSectionHeader, section: 0),
              let footer0 = supplementaryFrame(cv, kind: UICollectionView.elementKindSectionFooter, section: 0),
              let header1 = supplementaryFrame(cv, kind: UICollectionView.elementKindSectionHeader, section: 1),
              let box0 = boxAttributes(cv, section: 0) else {
            return XCTFail("Header / Footer / 箱の属性が取得できない")
        }
        // 先頭 Section の top 余白・末尾 Section の bottom 余白は list 端に対して取る
        XCTAssertEqual(cv.contentInset.top, 20, accuracy: 0.5)
        XCTAssertEqual(cv.contentInset.bottom, 12, accuracy: 0.5)
        XCTAssertEqual(header0.minY, 0, accuracy: 0.5, "Section 単位の先頭は Header でなければならない")
        // Header と箱の間・箱と Footer の間には余白が入らない
        XCTAssertEqual(box0.frame.minY, header0.maxY, accuracy: 0.5)
        XCTAssertEqual(box0.frame.maxY, footer0.minY, accuracy: 0.5)
        // 隣接 Section の間隔は前 Section の bottom と次 Section の top の加算になる
        XCTAssertEqual(header1.minY - footer0.maxY, 12 + 20, accuracy: 0.5)
        // Header / Footer も箱と同じ水平位置に揃う
        XCTAssertEqual(header0.minX, 16, accuracy: 0.5)
        XCTAssertEqual(header0.maxX, Self.viewSize.width - 16, accuracy: 0.5)
        XCTAssertEqual(footer0.minX, 16, accuracy: 0.5)
    }

    // MARK: - Root Header / Footer と Section 単位余白の位置関係

    /// Root Header / Footer があるとき、Section 単位の上下余白はその**内側**
    /// （Root Header と先頭 Section の間 / 末尾 Section と Root Footer の間）に入る。
    func test_RootHeaderがあると上余白はRootHeaderと先頭Sectionの間に入る() {
        let cells = [LabelCell(title: "A")]
        let makeRoot = { SettingsRoot(sections: [KsSettingsViewCore.Section(cells: cells)]) }
        let zeroMargin = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16)
        )
        let withMargin = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 24, leading: 16, bottom: 0, trailing: 16)
        )

        let (_, cvZero, windowZero) = hostWithRootAccessories(
            root: makeRoot(), theme: zeroMargin, style: .modern,
            rootHeader: .text("ROOT H"), rootFooter: nil
        )
        defer { windowZero.isHidden = true }
        let (_, cvMargin, windowMargin) = hostWithRootAccessories(
            root: makeRoot(), theme: withMargin, style: .modern,
            rootHeader: .text("ROOT H"), rootFooter: nil
        )
        defer { windowMargin.isHidden = true }

        guard let zeroLabel = rootAccessoryContentFrame(cvZero, kind: KsSettingsViewController.rootHeaderElementKind),
              let marginLabel = rootAccessoryContentFrame(cvMargin, kind: KsSettingsViewController.rootHeaderElementKind),
              let zeroBox = boxAttributes(cvZero, section: 0),
              let marginBox = boxAttributes(cvMargin, section: 0) else {
            return XCTFail("Root Header の内容または箱の属性が取得できない")
        }
        // Root Header の内容と先頭 Section の間隔が余白ぶんだけ広がる
        let zeroGap = zeroBox.frame.minY - zeroLabel.maxY
        let marginGap = marginBox.frame.minY - marginLabel.maxY
        XCTAssertEqual(marginGap - zeroGap, 24, accuracy: 0.5,
                       "上余白が Root Header と先頭 Section の間に入っていない")
        // 余白は Root Header の外（list 端）には出ない
        XCTAssertEqual(cvMargin.contentInset.top, 0, accuracy: 0.5,
                       "Root Header があるのに余白が list 端へ出ている")
        // Root にはライブラリ側のテキスト余白が無い（`rootTextGap` = 0）ため、テキストは領域の
        // 下端に密着する。余白なしのときだけ領域が `.estimated(20)` の下限に留まり、下端揃えの
        // ぶんテキストの minY が下がる。余白を足しても「さらに下へ」は押し下げられない。
        XCTAssertLessThanOrEqual(marginLabel.minY, zeroLabel.minY + 0.5,
                                 "Root Header 自体が下へ押し下げられている")
    }

    func test_RootFooterがあると下余白は末尾SectionとRootFooterの間に入る() {
        let cells = [LabelCell(title: "A")]
        let makeRoot = { SettingsRoot(sections: [KsSettingsViewCore.Section(cells: cells)]) }
        let zeroMargin = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16)
        )
        let withMargin = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 0, leading: 16, bottom: 18, trailing: 16)
        )

        let (_, cvZero, windowZero) = hostWithRootAccessories(
            root: makeRoot(), theme: zeroMargin, style: .modern,
            rootHeader: nil, rootFooter: .text("ROOT F")
        )
        defer { windowZero.isHidden = true }
        let (_, cvMargin, windowMargin) = hostWithRootAccessories(
            root: makeRoot(), theme: withMargin, style: .modern,
            rootHeader: nil, rootFooter: .text("ROOT F")
        )
        defer { windowMargin.isHidden = true }

        guard let zeroLabel = rootAccessoryContentFrame(cvZero, kind: KsSettingsViewController.rootFooterElementKind),
              let marginLabel = rootAccessoryContentFrame(cvMargin, kind: KsSettingsViewController.rootFooterElementKind),
              let zeroBox = boxAttributes(cvZero, section: 0),
              let marginBox = boxAttributes(cvMargin, section: 0) else {
            return XCTFail("Root Footer の内容または箱の属性が取得できない")
        }
        let zeroGap = zeroLabel.minY - zeroBox.frame.maxY
        let marginGap = marginLabel.minY - marginBox.frame.maxY
        XCTAssertEqual(marginGap - zeroGap, 18, accuracy: 0.5,
                       "下余白が末尾 Section と Root Footer の間に入っていない")
        XCTAssertEqual(cvMargin.contentInset.bottom, 0, accuracy: 0.5,
                       "Root Footer があるのに余白が list 端へ出ている")
    }

    /// Section 単位の余白は Section を包むためのもの。可視 Section が 1 つも無ければ、
    /// どの Section にも属さない余白を list 端にも Root accessory の内側にも残さない。
    func test_可視Sectionが0件ならSection単位余白を適用しない() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 24, leading: 16, bottom: 18, trailing: 16)
        )
        let (controller, cv, window) = host(root: SettingsRoot(sections: []), theme: theme, style: .modern)
        defer { window.isHidden = true }

        XCTAssertEqual(cv.contentInset.top, 0, accuracy: 0.5, "Section が無いのに list 端へ余白が出ている")
        XCTAssertEqual(cv.contentInset.bottom, 0, accuracy: 0.5)

        // Section が現れたら余白が付く
        controller.applyDiff(.insertSection(at: 0, section: KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])))
        awaitCondition(
            "Section 追加で list 端に Section 単位余白が付く",
            in: cv,
            actual: { "contentInset \(cv.contentInset)" },
            until: {
                abs(cv.contentInset.top - 24) <= 0.5 && abs(cv.contentInset.bottom - 18) <= 0.5
            }
        )
        XCTAssertEqual(cv.contentInset.top, 24, accuracy: 0.5, "Section 追加後に余白が付いていない")
        XCTAssertEqual(cv.contentInset.bottom, 18, accuracy: 0.5)
    }

    func test_最後のSectionを削除すると余白も消える() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 24, leading: 16, bottom: 18, trailing: 16)
        )
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let sectionID = section.id
        let (controller, cv, window) = host(root: SettingsRoot(sections: [section]), theme: theme, style: .modern)
        defer { window.isHidden = true }
        XCTAssertEqual(cv.contentInset.top, 24, accuracy: 0.5)

        controller.applyDiff(.removeSection(sectionID: sectionID))
        awaitCondition(
            "最後の Section 削除で list 端の余白が消える",
            in: cv,
            actual: { "contentInset \(cv.contentInset)" },
            until: { abs(cv.contentInset.top) <= 0.5 && abs(cv.contentInset.bottom) <= 0.5 }
        )

        XCTAssertEqual(cv.contentInset.top, 0, accuracy: 0.5, "Section が無くなったのに余白が残っている")
        XCTAssertEqual(cv.contentInset.bottom, 0, accuracy: 0.5)
    }

    func test_可視Sectionが0件ならRootHeader内側の余白も0になる() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 24, leading: 16, bottom: 0, trailing: 16)
        )
        let (controller, cv, window) = hostWithRootAccessories(
            root: SettingsRoot(sections: []), theme: theme, style: .modern,
            rootHeader: .text("ROOT H"), rootFooter: nil
        )
        defer { window.isHidden = true }
        guard let emptyLabel = rootAccessoryContentFrame(cv, kind: KsSettingsViewController.rootHeaderElementKind) else {
            return XCTFail("Root Header の内容が取得できない")
        }
        let emptyBottomGap = cv.contentSize.height - emptyLabel.maxY

        controller.applyDiff(.insertSection(at: 0, section: KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])))
        awaitNonNil(
            "Section 追加後の箱の属性が現れる",
            in: cv,
            produce: { boxAttributes(cv, section: 0) }
        )

        guard let filledLabel = rootAccessoryContentFrame(cv, kind: KsSettingsViewController.rootHeaderElementKind),
              let box = boxAttributes(cv, section: 0) else {
            return XCTFail("Section 追加後の Root Header の内容または箱の属性が取得できない")
        }
        // Root accessory にはライブラリ側のテキスト余白を入れない（`rootTextGap` = 0）ため、
        // Section が無ければ内側の余白は 0 のままになる。
        XCTAssertEqual(emptyBottomGap, 0, accuracy: 2.0,
                       "Section が無いのに Root Header の内側へ余白が出ている")
        XCTAssertEqual(box.frame.minY - filledLabel.maxY, 24, accuracy: 2.0,
                       "Section 追加後に Root Header 内側の余白が付いていない")
    }

    /// Root accessory の作り直しは `KsAnyView` の factory 再実行を伴い、View accessory の内部状態
    /// （編集途中のテキスト・スクロール位置・first responder）を失わせる。Section 単位余白が
    /// 変わらない Diff では作り直してはいけない。
    func test_余白が変わらないDiffではRootHeaderのfactoryを呼び直さない() {
        let counter = FactoryCallCounter()
        let cells = [LabelCell(title: "A"), LabelCell(title: "B")]
        let section = KsSettingsViewCore.Section(cells: cells)
        let (controller, cv, window) = hostWithRootAccessories(
            root: SettingsRoot(sections: [section]),
            style: .modern,
            rootHeader: .view(KsAnyView.uiKit {
                counter.count += 1
                let view = UIView()
                view.backgroundColor = .lightGray
                return view
            }),
            rootFooter: nil
        )
        defer { window.isHidden = true }
        XCTAssertGreaterThan(counter.count, 0, "Root Header の View が生成されていない")
        let baseline = counter.count

        // Root accessory と無関係な内容更新
        controller.applyDiff(.replaceCell(cellID: KsCellID(cell: cells[0]), new: LabelCell(title: "A2")))
        waitForNegativeVerification(in: cv)
        XCTAssertEqual(counter.count, baseline, "内容 Diff で Root Header が作り直されている")

        // 余白が変わらない構造 Diff（Section 数は 1 のまま）
        controller.applyDiff(.insertCell(sectionID: section.id, at: 2, cell: LabelCell(title: "C")))
        waitForNegativeVerification(in: cv)
        XCTAssertEqual(counter.count, baseline, "構造 Diff で Root Header が作り直されている")

        // 余白が変わらない Theme 変更
        controller.applyTheme(Theme(cellBackgroundColor: .green))
        waitForNegativeVerification(in: cv)
        XCTAssertEqual(counter.count, baseline, "余白と無関係な Theme 変更で Root Header が作り直されている")
    }

    func test_余白が変わる遷移ではRootHeaderを作り直す() {
        let counter = FactoryCallCounter()
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 24, leading: 16, bottom: 0, trailing: 16)
        )
        let (controller, cv, window) = hostWithRootAccessories(
            root: SettingsRoot(sections: []),
            theme: theme,
            style: .modern,
            rootHeader: .view(KsAnyView.uiKit {
                counter.count += 1
                let view = UIView()
                view.backgroundColor = .lightGray
                return view
            }),
            rootFooter: nil
        )
        defer { window.isHidden = true }
        let baseline = counter.count

        // 可視 Section 0 件 → 非 0 件で余白が 0 から 24 へ変わる
        controller.applyDiff(.insertSection(at: 0, section: KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])))
        awaitCondition(
            "余白が変わる遷移で Root Header が作り直される",
            in: cv,
            actual: { "factory 実行回数 \(counter.count) (変更前 \(baseline))" },
            until: { counter.count > baseline }
        )
        XCTAssertGreaterThan(counter.count, baseline,
                             "余白が変わる遷移で Root Header が作り直されていない")
        let afterInsert = counter.count

        // sectionMargin を変える Theme 変更
        controller.applyTheme(
            Theme(sectionMargin: NSDirectionalEdgeInsets(top: 40, leading: 16, bottom: 0, trailing: 16))
        )
        awaitCondition(
            "sectionMargin の Theme 変更で Root Header が作り直される",
            in: cv,
            actual: { "factory 実行回数 \(counter.count) (変更前 \(afterInsert))" },
            until: { counter.count > afterInsert }
        )
        XCTAssertGreaterThan(counter.count, afterInsert,
                             "sectionMargin の Theme 変更で Root Header が作り直されていない")
    }

    func test_RootHeaderが無い側の余白はlist端に残る() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 24, leading: 16, bottom: 18, trailing: 16)
        )
        // Root Footer だけを持たせる: 上は list 端、下は Root Footer の内側
        let (_, cv, window) = hostWithRootAccessories(
            root: SettingsRoot(sections: [section]), theme: theme, style: .modern,
            rootHeader: nil, rootFooter: .text("ROOT F")
        )
        defer { window.isHidden = true }

        XCTAssertEqual(cv.contentInset.top, 24, accuracy: 0.5, "Root Header が無い側は list 端に余白を取る")
        XCTAssertEqual(cv.contentInset.bottom, 0, accuracy: 0.5, "Root Footer がある側は list 端に余白を取らない")
    }

    func test_Classicでも余白はRootHeaderの内側に入る() {
        let cells = [LabelCell(title: "A")]
        let makeRoot = { SettingsRoot(sections: [KsSettingsViewCore.Section(cells: cells)]) }
        let zeroMargin = Theme(sectionMargin: NSDirectionalEdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
        let withMargin = Theme(sectionMargin: NSDirectionalEdgeInsets(top: 24, leading: 0, bottom: 0, trailing: 0))

        let (_, cvZero, windowZero) = hostWithRootAccessories(
            root: makeRoot(), theme: zeroMargin, style: .classic,
            rootHeader: .text("ROOT H"), rootFooter: nil
        )
        defer { windowZero.isHidden = true }
        let (_, cvMargin, windowMargin) = hostWithRootAccessories(
            root: makeRoot(), theme: withMargin, style: .classic,
            rootHeader: .text("ROOT H"), rootFooter: nil
        )
        defer { windowMargin.isHidden = true }

        guard let zeroLabel = rootAccessoryContentFrame(cvZero, kind: KsSettingsViewController.rootHeaderElementKind),
              let marginLabel = rootAccessoryContentFrame(cvMargin, kind: KsSettingsViewController.rootHeaderElementKind),
              let zeroCell = itemFrame(cvZero, section: 0, item: 0),
              let marginCell = itemFrame(cvMargin, section: 0, item: 0) else {
            return XCTFail("Root Header の内容または Cell の属性が取得できない")
        }
        let zeroGap = zeroCell.minY - zeroLabel.maxY
        let marginGap = marginCell.minY - marginLabel.maxY
        XCTAssertEqual(marginGap - zeroGap, 24, accuracy: 0.5,
                       "Classic でも上余白は Root Header の内側に入らなければならない")
        XCTAssertEqual(cvMargin.contentInset.top, 0, accuracy: 0.5)
    }

    func test_実行時のTheme変更でRootHeader内側の余白も追従する() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let (controller, cv, window) = hostWithRootAccessories(
            root: SettingsRoot(sections: [section]),
            theme: Theme(sectionMargin: NSDirectionalEdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16)),
            style: .modern,
            rootHeader: .text("ROOT H"),
            rootFooter: nil
        )
        defer { window.isHidden = true }
        guard let before = rootAccessoryContentFrame(cv, kind: KsSettingsViewController.rootHeaderElementKind),
              let beforeBox = boxAttributes(cv, section: 0) else {
            return XCTFail("Root Header の内容または箱の属性が取得できない")
        }
        let beforeGap = beforeBox.frame.minY - before.maxY

        controller.applyTheme(
            Theme(sectionMargin: NSDirectionalEdgeInsets(top: 24, leading: 16, bottom: 0, trailing: 16))
        )
        awaitCondition(
            "Theme 変更が Root Header 内側の余白へ反映される",
            in: cv,
            actual: {
                guard let after = rootAccessoryContentFrame(
                    cv, kind: KsSettingsViewController.rootHeaderElementKind
                ), let afterBox = boxAttributes(cv, section: 0) else {
                    return "内容または箱の属性が取得できない"
                }
                return "余白 \(afterBox.frame.minY - after.maxY) (変更前 \(beforeGap))"
            },
            until: {
                guard let after = rootAccessoryContentFrame(
                    cv, kind: KsSettingsViewController.rootHeaderElementKind
                ), let afterBox = boxAttributes(cv, section: 0) else { return false }
                return abs(((afterBox.frame.minY - after.maxY) - beforeGap) - 24) <= 0.5
            }
        )

        guard let after = rootAccessoryContentFrame(cv, kind: KsSettingsViewController.rootHeaderElementKind),
              let afterBox = boxAttributes(cv, section: 0) else {
            return XCTFail("Theme 変更後に Root Header の内容または箱の属性が取得できない")
        }
        XCTAssertEqual((afterBox.frame.minY - after.maxY) - beforeGap, 24, accuracy: 0.5,
                       "Theme 変更が Root Header 内側の余白へ反映されていない")
    }

    // MARK: - Modern の Section 箱描画

    func test_HeaderとFooterは箱の外に置かれる() {
        let section = KsSettingsViewCore.Section(
            header: .text("一般"),
            footer: .text("説明"),
            cells: [LabelCell(title: "A"), LabelCell(title: "B"), LabelCell(title: "C")]
        )
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }

        guard let box = boxAttributes(cv, section: 0),
              let first = itemFrame(cv, section: 0, item: 0),
              let last = itemFrame(cv, section: 0, item: 2),
              let header = supplementaryFrame(cv, kind: UICollectionView.elementKindSectionHeader, section: 0),
              let footer = supplementaryFrame(cv, kind: UICollectionView.elementKindSectionFooter, section: 0) else {
            return XCTFail("箱 / Cell / Header / Footer の属性が取得できない")
        }
        // 箱は先頭 Cell から末尾 Cell までを覆う
        XCTAssertEqual(box.frame.minY, first.minY, accuracy: 0.5)
        XCTAssertEqual(box.frame.maxY, last.maxY, accuracy: 0.5)
        // Header は箱の上外側、Footer は箱の下外側
        XCTAssertLessThanOrEqual(header.maxY, box.frame.minY + 0.5)
        XCTAssertGreaterThanOrEqual(footer.minY, box.frame.maxY - 0.5)
    }

    func test_RootHeaderは箱に含まれない() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let (_, cv, window) = hostWithRootAccessories(
            root: SettingsRoot(sections: [section]),
            style: .modern,
            rootHeader: .text("ROOT H"),
            rootFooter: .text("ROOT F")
        )
        defer { window.isHidden = true }

        // Root Header / Footer は layout 全体の boundary item であり、section 指定では引けないため
        // 可視範囲の属性一覧から拾う。
        let all = cv.collectionViewLayout.layoutAttributesForElements(
            in: CGRect(origin: .zero, size: CGSize(width: Self.viewSize.width, height: 5000))
        ) ?? []
        guard let box = boxAttributes(cv, section: 0),
              let rootHeader = all.first(where: {
                  $0.representedElementKind == KsSettingsViewController.rootHeaderElementKind
              })?.frame else {
            return XCTFail("箱または Root Header の属性が取得できない")
        }
        XCTAssertLessThanOrEqual(rootHeader.maxY, box.frame.minY, "Root Header が箱の内側に入っている")
    }

    func test_構造変更後も箱がCell範囲に追従する() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let sectionID = section.id
        let (controller, cv, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }
        let before = boxAttributes(cv, section: 0)?.frame

        // Cell を末尾に挿入する
        controller.applyDiff(.insertCell(sectionID: sectionID, at: 2, cell: LabelCell(title: "C")))
        awaitCondition(
            "Cell 挿入後の箱が末尾 Cell まで伸びる",
            in: cv,
            actual: {
                "箱 \(String(describing: boxAttributes(cv, section: 0)?.frame)) / "
                    + "末尾 Cell \(String(describing: itemFrame(cv, section: 0, item: 2)))"
            },
            until: {
                guard let box = boxAttributes(cv, section: 0)?.frame,
                      let last = itemFrame(cv, section: 0, item: 2) else { return false }
                return abs(box.maxY - last.maxY) <= 0.5
            }
        )

        guard let after = boxAttributes(cv, section: 0)?.frame,
              let last = itemFrame(cv, section: 0, item: 2) else {
            return XCTFail("挿入後の箱または Cell の属性が取得できない")
        }
        XCTAssertEqual(after.maxY, last.maxY, accuracy: 0.5, "箱が挿入後の末尾 Cell まで伸びていない")
        XCTAssertGreaterThan(after.height, before?.height ?? 0)
    }

    func test_可視Cellが0件のSectionは箱を生成しない() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 20, leading: 16, bottom: 12, trailing: 16)
        )
        let empty = KsSettingsViewCore.Section(header: .text("EMPTY"), cells: [])
        let filled = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let (_, cv, window) = host(
            root: SettingsRoot(sections: [empty, filled]),
            theme: theme,
            style: .modern
        )
        defer { window.isHidden = true }

        XCTAssertNil(boxAttributes(cv, section: 0), "Cell が無い Section に箱を生成してはいけない")
        guard let header = supplementaryFrame(cv, kind: UICollectionView.elementKindSectionHeader, section: 0),
              let nextBox = boxAttributes(cv, section: 1) else {
            return XCTFail("Cell が無い Section でも Header は表示されなければならない")
        }
        // sectionMargin は Cell の有無に関わらず Section 単位に適用される
        XCTAssertEqual(cv.contentInset.top, 20, accuracy: 0.5)
        XCTAssertEqual(header.minX, 16, accuracy: 0.5)
        XCTAssertEqual(nextBox.frame.minY - header.maxY, 12 + 20, accuracy: 0.5,
                       "Cell が無い Section も Section 単位の余白を持たなければならない")
    }

    func test_viewportより長いSectionでも箱の端は実際のSection端に一致する() {
        let cells = (1...40).map { LabelCell(title: "行 \($0)") }
        let section = KsSettingsViewCore.Section(cells: cells)
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }

        guard let box = boxAttributes(cv, section: 0),
              let first = itemFrame(cv, section: 0, item: 0),
              let last = itemFrame(cv, section: 0, item: cells.count - 1) else {
            return XCTFail("箱または Cell の属性が取得できない")
        }
        XCTAssertGreaterThan(box.frame.height, Self.viewSize.height,
                             "viewport より長い Section を用意できていない")
        XCTAssertEqual(box.frame.minY, first.minY, accuracy: 0.5)
        XCTAssertEqual(box.frame.maxY, last.maxY, accuracy: 0.5)
    }

    /// 表示中の Cell に実際に掛かっている clip 形状。mask が無ければ `nil`。
    private func liveClipPath(_ cv: UICollectionView, section: Int, item: Int) -> UIBezierPath? {
        guard let cell = cv.cellForItem(at: IndexPath(item: item, section: section)),
              let mask = cell.layer.mask as? CAShapeLayer,
              let cgPath = mask.path else {
            return nil
        }
        return UIBezierPath(cgPath: cgPath)
    }

    /// 指定 Cell の下端の角が丸められているか（箱の下端に接しているか）を実際の mask から見る。
    private func liveRoundsBottom(_ cv: UICollectionView, section: Int, item: Int) -> Bool {
        guard let cell = cv.cellForItem(at: IndexPath(item: item, section: section)),
              let path = liveClipPath(cv, section: section, item: item) else {
            return false
        }
        // 下端の角の外側にあたる点が塗り範囲から外れていれば、その Cell が箱の下端に接している。
        return !path.contains(CGPoint(x: 1, y: cell.bounds.maxY - 1))
    }

    func test_末尾にCellを挿入すると旧末尾Cellの角丸clipが外れる() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let sectionID = section.id
        let (controller, cv, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }
        XCTAssertTrue(liveRoundsBottom(cv, section: 0, item: 1), "挿入前の末尾 Cell に角丸 clip が無い")

        controller.applyDiff(.insertCell(sectionID: sectionID, at: 2, cell: LabelCell(title: "C")))
        awaitCondition(
            "末尾 Cell 挿入で旧末尾 Cell の角丸 clip が外れる",
            in: cv,
            actual: { "item 1 の角丸 clip \(liveRoundsBottom(cv, section: 0, item: 1))" },
            until: { !liveRoundsBottom(cv, section: 0, item: 1) }
        )

        XCTAssertFalse(liveRoundsBottom(cv, section: 0, item: 1),
                       "末尾でなくなった Cell に角丸 clip が残っている")
        XCTAssertTrue(liveRoundsBottom(cv, section: 0, item: 2),
                      "新しい末尾 Cell に角丸 clip が無い")
    }

    func test_末尾Cellを削除すると新しい末尾Cellに角丸clipが付く() {
        let cells = [LabelCell(title: "A"), LabelCell(title: "B"), LabelCell(title: "C")]
        let section = KsSettingsViewCore.Section(cells: cells)
        let (controller, cv, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }
        XCTAssertFalse(liveRoundsBottom(cv, section: 0, item: 1), "削除前の中間 Cell に角丸 clip がある")

        controller.applyDiff(.removeCell(cellID: KsCellID(cell: cells[2])))
        awaitCondition(
            "末尾 Cell 削除で新しい末尾 Cell に角丸 clip が付く",
            in: cv,
            actual: { "item 1 の角丸 clip \(liveRoundsBottom(cv, section: 0, item: 1))" },
            until: { liveRoundsBottom(cv, section: 0, item: 1) }
        )

        XCTAssertTrue(liveRoundsBottom(cv, section: 0, item: 1),
                      "新しい末尾 Cell に角丸 clip が付いていない")
    }

    func test_Classicでは箱の装飾を生成しない() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), style: .classic)
        defer { window.isHidden = true }
        XCTAssertNil(boxAttributes(cv, section: 0))
    }

    // MARK: - 箱と Cell 背景の合成

    func test_先頭Cellの背景は箱の角丸で切り取られる() {
        let metrics = SectionBoxMetrics.resolve(theme: Theme(), style: .modern)
        let clip = SectionBoxCellClip.resolve(metrics: metrics, itemIndex: 0, cellCount: 3)
        XCTAssertTrue(clip.roundsTop)
        XCTAssertFalse(clip.roundsBottom)

        let bounds = CGRect(x: 0, y: 0, width: 343, height: 60)
        guard let path = clip.maskPath(in: bounds) else {
            return XCTFail("先頭 Cell に clip 形状が作られていない")
        }
        // 角の外側は背景に含まれず、行の中央は含まれる
        XCTAssertFalse(path.contains(CGPoint(x: 1, y: 1)), "左上の角の外側が clip されていない")
        XCTAssertFalse(path.contains(CGPoint(x: bounds.maxX - 1, y: 1)), "右上の角の外側が clip されていない")
        XCTAssertTrue(path.contains(CGPoint(x: bounds.midX, y: bounds.midY)))
        // 下端は角丸にしない（次の Cell と連続させる）
        XCTAssertTrue(path.contains(CGPoint(x: 1, y: bounds.maxY - 1)))
    }

    func test_末尾Cellは下端だけを角丸にする() {
        let metrics = SectionBoxMetrics.resolve(theme: Theme(), style: .modern)
        let clip = SectionBoxCellClip.resolve(metrics: metrics, itemIndex: 2, cellCount: 3)
        XCTAssertFalse(clip.roundsTop)
        XCTAssertTrue(clip.roundsBottom)

        let bounds = CGRect(x: 0, y: 0, width: 343, height: 60)
        guard let path = clip.maskPath(in: bounds) else {
            return XCTFail("末尾 Cell に clip 形状が作られていない")
        }
        XCTAssertTrue(path.contains(CGPoint(x: 1, y: 1)))
        XCTAssertFalse(path.contains(CGPoint(x: 1, y: bounds.maxY - 1)))
    }

    func test_中間Cellはボーダーが無ければclipを持たない() {
        let metrics = SectionBoxMetrics.resolve(theme: Theme(), style: .modern)
        XCTAssertEqual(SectionBoxCellClip.resolve(metrics: metrics, itemIndex: 1, cellCount: 3), .none)
    }

    func test_ボーダーがあれば中間Cellもボーダー幅だけ内側へ収まる() {
        let metrics = SectionBoxMetrics.resolve(theme: Theme(sectionBorderWidth: 4), style: .modern)
        let clip = SectionBoxCellClip.resolve(metrics: metrics, itemIndex: 1, cellCount: 3)
        let bounds = CGRect(x: 0, y: 0, width: 343, height: 60)
        guard let path = clip.maskPath(in: bounds) else {
            return XCTFail("ボーダーがあるときは中間 Cell にも clip が要る")
        }
        // 左右はボーダー幅ぶん内側
        XCTAssertEqual(path.bounds.minX, 4, accuracy: 0.01)
        XCTAssertEqual(path.bounds.maxX, bounds.maxX - 4, accuracy: 0.01)
        // 上下は箱の端でないため Cell の範囲を切らない（形状は Cell の外まで続く）
        XCTAssertLessThanOrEqual(path.bounds.minY, 0)
        XCTAssertGreaterThanOrEqual(path.bounds.maxY, bounds.maxY)
        XCTAssertTrue(path.contains(CGPoint(x: bounds.midX, y: 1)))
        XCTAssertTrue(path.contains(CGPoint(x: bounds.midX, y: bounds.maxY - 1)))
    }

    func test_角丸半径がCell高さを超えてもCell側の弧は箱と同じになる() {
        // 行高 48pt × 4 行の箱 (192pt) に半径 60pt。Cell 単体の高さで半径を切り詰めると
        // 先頭 Cell の背景だけが箱の角の外へはみ出す。
        let metrics = SectionBoxMetrics.resolve(theme: Theme(sectionCornerRadius: 60), style: .modern)
        let boxFrame = CGRect(x: 0, y: 0, width: 343, height: 192)
        let cellFrame = CGRect(x: 0, y: 0, width: 343, height: 48)
        let clip = SectionBoxCellClip.resolve(
            metrics: metrics,
            boxFrame: boxFrame,
            cellFrame: cellFrame,
            itemIndex: 0,
            cellCount: 4
        )
        XCTAssertEqual(
            clip.cornerRadius,
            SectionBoxMetrics.clampedCornerRadius(60, for: boxFrame.size),
            "Cell 側の角丸半径が箱の clamp 済み半径と一致していない"
        )
        guard let path = clip.maskPath(in: CGRect(origin: .zero, size: cellFrame.size)) else {
            return XCTFail("先頭 Cell に clip 形状が作られていない")
        }
        // 半径 60 の弧の外側にある点。半径が Cell 高さ (48) へ切り詰められると内側に入ってしまう。
        XCTAssertFalse(path.contains(CGPoint(x: 30, y: 5)),
                       "Cell の背景が箱の角丸の外へはみ出している")
        XCTAssertTrue(path.contains(CGPoint(x: cellFrame.midX, y: cellFrame.midY)))
    }

    func test_Classicではclipを掛けない() {
        let metrics = SectionBoxMetrics.resolve(theme: Theme(), style: .classic)
        XCTAssertEqual(SectionBoxCellClip.resolve(metrics: metrics, itemIndex: 0, cellCount: 3), .none)
    }

    func test_ボーダーはCell背景に隠れない() {
        // 正のボーダー幅と、CellStyle.backgroundColor を持つ Cell を含む Section
        let theme = Theme(sectionBorderWidth: 4, sectionBorderColor: .red)
        let section = KsSettingsViewCore.Section(cells: [
            LabelCell(style: CellStyle(backgroundColor: .yellow), title: "A"),
            LabelCell(title: "B")
        ])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), theme: theme, style: .modern)
        defer { window.isHidden = true }

        guard let cell = cv.cellForItem(at: IndexPath(item: 0, section: 0)),
              let mask = cell.layer.mask as? CAShapeLayer,
              let path = mask.path else {
            return XCTFail("Cell の clip mask が設定されていない")
        }
        // Cell の塗りはボーダーの内側に収まり、全周でボーダーが残る
        let painted = path.boundingBox
        XCTAssertEqual(painted.minX, 4, accuracy: 0.5)
        XCTAssertEqual(painted.maxX, cell.bounds.maxX - 4, accuracy: 0.5)
        XCTAssertEqual(painted.minY, 4, accuracy: 0.5, "先頭 Cell の上端でボーダーが覆われている")
        XCTAssertGreaterThanOrEqual(painted.maxY, cell.bounds.maxY, "中間の境界を切ってはいけない")
    }

    func test_先頭Cellの背景が角丸の外へはみ出さない() {
        let section = KsSettingsViewCore.Section(cells: [
            LabelCell(style: CellStyle(backgroundColor: .yellow), title: "A"),
            LabelCell(title: "B")
        ])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }

        guard let cell = cv.cellForItem(at: IndexPath(item: 0, section: 0)),
              let mask = cell.layer.mask as? CAShapeLayer,
              let cgPath = mask.path else {
            return XCTFail("先頭 Cell の clip mask が設定されていない")
        }
        let path = UIBezierPath(cgPath: cgPath)
        XCTAssertFalse(path.contains(CGPoint(x: 1, y: 1)), "左上の角の外へ背景が描かれる")
        XCTAssertTrue(path.contains(CGPoint(x: cell.bounds.midX, y: cell.bounds.midY)))
    }

    func test_押下背景も箱形状に収まる() {
        let section = KsSettingsViewCore.Section(cells: [
            LabelCell(title: "A"),
            LabelCell(title: "B")
        ])
        let theme = Theme(selectedColor: .magenta)
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), theme: theme, style: .modern)
        defer { window.isHidden = true }

        guard let cell = cv.cellForItem(at: IndexPath(item: 0, section: 0)) else {
            return XCTFail("先頭 Cell を取得できない")
        }
        cell.isHighlighted = true
        awaitCondition(
            "押下背景が selectedColor で塗られる",
            in: cv,
            actual: { "背景色 \(String(describing: cell.backgroundConfiguration?.backgroundColor))" },
            until: { cell.backgroundConfiguration?.backgroundColor?.isEqual(UIColor.magenta) ?? false }
        )

        XCTAssertTrue(
            cell.backgroundConfiguration?.backgroundColor?.isEqual(UIColor.magenta) ?? false,
            "押下背景が selectedColor で塗られていない"
        )
        guard let mask = cell.layer.mask as? CAShapeLayer, let cgPath = mask.path else {
            return XCTFail("押下中に clip mask が失われている")
        }
        let path = UIBezierPath(cgPath: cgPath)
        XCTAssertFalse(path.contains(CGPoint(x: 1, y: 1)), "押下背景が箱の角丸の外へ出ている")
    }

    /// 実描画の画素を読み、箱・ボーダー・下地の合成結果を観察する。
    func test_描画結果で箱とボーダーと下地が観察できる() {
        let theme = Theme(
            backgroundColor: .blue,
            cellBackgroundColor: .white,
            sectionBorderWidth: 4,
            sectionBorderColor: .red
        )
        let section = KsSettingsViewCore.Section(cells: [
            LabelCell(style: CellStyle(backgroundColor: .white), title: "A"),
            LabelCell(title: "B")
        ])
        let (_, cv, window) = host(root: SettingsRoot(sections: [section]), theme: theme, style: .modern)
        defer { window.isHidden = true }

        guard let box = boxAttributes(cv, section: 0)?.frame,
              let image = render(cv) else {
            return XCTFail("箱の属性または描画結果が得られない")
        }
        // 属性は content 座標、画素は view 座標なので scroll の offset で読み替える。
        let boxTop = box.minY - cv.contentOffset.y
        let rowMidY = boxTop + 24

        // 箱の外（左の余白）は list の下地色
        assertPixel(image, at: CGPoint(x: 8, y: rowMidY), isCloseTo: .blue, label: "箱の外の下地")
        // 箱の左端はボーダー色（Cell の不透明背景に覆われない）
        assertPixel(image, at: CGPoint(x: box.minX + 2, y: rowMidY), isCloseTo: .red, label: "左のボーダー")
        // 箱の右端も同じ
        assertPixel(image, at: CGPoint(x: box.maxX - 2, y: rowMidY), isCloseTo: .red, label: "右のボーダー")
        // 箱の上端もボーダー色
        assertPixel(image, at: CGPoint(x: box.midX, y: boxTop + 2), isCloseTo: .red, label: "上のボーダー")
        // 箱の内側は Cell の背景色
        assertPixel(image, at: CGPoint(x: box.midX, y: rowMidY), isCloseTo: .white, label: "箱の内側")
        // 角丸の外側は下地色のまま（先頭 Cell の背景がはみ出さない）
        assertPixel(image, at: CGPoint(x: box.minX + 1, y: boxTop + 1), isCloseTo: .blue, label: "角丸の外")
    }

    /// collection view の実描画を画像化する。
    private func render(_ cv: UICollectionView) -> UIImage? {
        let renderer = UIGraphicsImageRenderer(bounds: cv.bounds)
        return renderer.image { context in
            cv.layer.render(in: context.cgContext)
        }
    }

    private func assertPixel(
        _ image: UIImage,
        at point: CGPoint,
        isCloseTo expected: UIColor,
        label: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard let actual = pixelColor(image, at: point) else {
            return XCTFail("\(label): 画素を読めない", file: file, line: line)
        }
        var er: CGFloat = 0, eg: CGFloat = 0, eb: CGFloat = 0, ea: CGFloat = 0
        expected.getRed(&er, green: &eg, blue: &eb, alpha: &ea)
        let distance = abs(actual.0 - er) + abs(actual.1 - eg) + abs(actual.2 - eb)
        XCTAssertLessThan(
            distance, 0.25,
            "\(label): 期待 (\(er), \(eg), \(eb)) に対し実測 (\(actual.0), \(actual.1), \(actual.2))",
            file: file, line: line
        )
    }

    private func pixelColor(_ image: UIImage, at point: CGPoint) -> (CGFloat, CGFloat, CGFloat)? {
        guard let cgImage = image.cgImage else { return nil }
        let scale = image.scale
        let x = Int(point.x * scale)
        let y = Int(point.y * scale)
        guard x >= 0, y >= 0, x < cgImage.width, y < cgImage.height else { return nil }
        guard let cropped = cgImage.cropping(to: CGRect(x: x, y: y, width: 1, height: 1)) else { return nil }
        var pixel: [UInt8] = [0, 0, 0, 0]
        guard let context = CGContext(
            data: &pixel,
            width: 1,
            height: 1,
            bitsPerComponent: 8,
            bytesPerRow: 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else { return nil }
        context.draw(cropped, in: CGRect(x: 0, y: 0, width: 1, height: 1))
        return (CGFloat(pixel[0]) / 255, CGFloat(pixel[1]) / 255, CGFloat(pixel[2]) / 255)
    }

    // MARK: - Modern の罫線規則

    func test_Modernは箱の上下端にseparatorを描かない() {
        let section = KsSettingsViewCore.Section(cells: [
            LabelCell(title: "A"), LabelCell(title: "B"), LabelCell(title: "C")
        ])
        let (controller, _, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }

        let first = separator(controller, section: 0, item: 0)
        XCTAssertEqual(first.topSeparatorVisibility, .hidden, "箱の上端に罫線を描いてはいけない")
        XCTAssertEqual(first.bottomSeparatorVisibility, .visible)

        let middle = separator(controller, section: 0, item: 1)
        XCTAssertEqual(middle.topSeparatorVisibility, .hidden)
        XCTAssertEqual(middle.bottomSeparatorVisibility, .visible)

        let last = separator(controller, section: 0, item: 2)
        XCTAssertEqual(last.topSeparatorVisibility, .hidden)
        XCTAssertEqual(last.bottomSeparatorVisibility, .hidden, "箱の下端に罫線を描いてはいけない")
    }

    func test_Modernの中間separatorは左右へ同量インセットする() {
        let section = KsSettingsViewCore.Section(cells: [
            LabelCell(title: "A"),
            LabelCell(title: "B", icon: .systemName("star")),
            LabelCell(title: "C")
        ])
        let (controller, _, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }

        let withoutIcon = separator(controller, section: 0, item: 0)
        XCTAssertEqual(withoutIcon.bottomSeparatorInsets.leading, 16)
        XCTAssertEqual(withoutIcon.bottomSeparatorInsets.trailing, 16,
                       "trailing を端まで引くと箱が分断されて見える")
        // icon の有無で inset を変えない
        let withIcon = separator(controller, section: 0, item: 1)
        XCTAssertEqual(withIcon.bottomSeparatorInsets.leading, withoutIcon.bottomSeparatorInsets.leading)
        XCTAssertEqual(withIcon.bottomSeparatorInsets.trailing, withoutIcon.bottomSeparatorInsets.trailing)
    }

    func test_Modernの中間separatorはボーダーの内側を基準にする() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let (controller, _, window) = host(
            root: SettingsRoot(sections: [section]),
            theme: Theme(sectionBorderWidth: 5),
            style: .modern
        )
        defer { window.isHidden = true }

        let config = separator(controller, section: 0, item: 0)
        XCTAssertEqual(config.bottomSeparatorInsets.leading, 16 + 5)
        XCTAssertEqual(config.bottomSeparatorInsets.trailing, 16 + 5)
    }

    func test_Modernの単一CellのSectionにseparatorが出ない() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "只一つ")])
        let (controller, _, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }

        let only = separator(controller, section: 0, item: 0)
        XCTAssertEqual(only.topSeparatorVisibility, .hidden)
        XCTAssertEqual(only.bottomSeparatorVisibility, .hidden)
    }

    func test_Modernのseparator色もThemeから解決する() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let (controller, _, window) = host(
            root: SettingsRoot(sections: [section]),
            theme: Theme(separatorColor: .blue),
            style: .modern
        )
        defer { window.isHidden = true }

        XCTAssertTrue(separator(controller, section: 0, item: 0).color.isEqual(UIColor.blue))
    }

    func test_Classicのseparator規則は変わらない() {
        let section = KsSettingsViewCore.Section(cells: [
            LabelCell(title: "A"), LabelCell(title: "B"), LabelCell(title: "C")
        ])
        let (controller, _, window) = host(root: SettingsRoot(sections: [section]), style: .classic)
        defer { window.isHidden = true }

        let first = separator(controller, section: 0, item: 0)
        XCTAssertEqual(first.topSeparatorVisibility, .visible)
        XCTAssertEqual(first.topSeparatorInsets.leading, 0, "Classic の Section 境界は全幅")
        let middle = separator(controller, section: 0, item: 1)
        XCTAssertEqual(middle.bottomSeparatorInsets.leading, 16)
        XCTAssertEqual(middle.bottomSeparatorInsets.trailing, 0, "Classic の中間罫線は右端まで引く")
        let last = separator(controller, section: 0, item: 2)
        XCTAssertEqual(last.bottomSeparatorVisibility, .visible)
        XCTAssertEqual(last.bottomSeparatorInsets.leading, 0)
    }

    // MARK: - Classic への sectionMargin 上下適用

    func test_Classicは未指定でも既定marginの上下22ptが並びに効く() {
        let s0 = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let s1 = KsSettingsViewCore.Section(cells: [LabelCell(title: "B")])
        let (_, cv, window) = host(root: SettingsRoot(sections: [s0, s1]), style: .classic)
        defer { window.isHidden = true }

        guard let a = itemFrame(cv, section: 0, item: 0), let b = itemFrame(cv, section: 1, item: 0) else {
            return XCTFail("Cell の属性が取得できない")
        }
        XCTAssertEqual(a.minY, 0, accuracy: 0.5)
        // Classic 既定 margin は top 22 / bottom 0。上端の余白は contentInset として出る。
        XCTAssertEqual(cv.contentInset.top, 22, accuracy: 0.5)
        XCTAssertEqual(cv.contentInset.bottom, 0, accuracy: 0.5)
        XCTAssertEqual(b.minY - a.maxY, 22, accuracy: 0.5, "Section 間に既定 margin の top が入っていない")
        // 水平成分は Classic では無視され、Section 境界は全幅のまま。
        XCTAssertEqual(a.minX, 0, accuracy: 0.5)
        XCTAssertEqual(a.width, Self.viewSize.width, accuracy: 0.5)
    }

    func test_Classicはmarginの上下成分だけが効く() {
        let theme = Theme(
            sectionMargin: NSDirectionalEdgeInsets(top: 10, leading: 24, bottom: 6, trailing: 24)
        )
        let s0 = KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        let s1 = KsSettingsViewCore.Section(cells: [LabelCell(title: "B")])
        let (_, cv, window) = host(root: SettingsRoot(sections: [s0, s1]), theme: theme, style: .classic)
        defer { window.isHidden = true }

        guard let a = itemFrame(cv, section: 0, item: 0), let b = itemFrame(cv, section: 1, item: 0) else {
            return XCTFail("Cell の属性が取得できない")
        }
        XCTAssertEqual(cv.contentInset.top, 10, accuracy: 0.5)
        XCTAssertEqual(cv.contentInset.bottom, 6, accuracy: 0.5)
        XCTAssertEqual(b.minY - a.maxY, 6 + 10, accuracy: 0.5)
        // 水平方向は全幅のまま
        XCTAssertEqual(a.minX, 0, accuracy: 0.5)
        XCTAssertEqual(a.width, Self.viewSize.width, accuracy: 0.5)
    }

    // MARK: - style 切替の整合

    func test_ClassicからModernへの切替で内容と順序は変わらない() {
        let section = KsSettingsViewCore.Section(
            header: .text("H"),
            cells: [LabelCell(title: "A"), LabelCell(title: "B")]
        )
        let (controller, cv, window) = host(root: SettingsRoot(sections: [section]), style: .classic)
        defer { window.isHidden = true }
        let sectionIDsBefore = controller.internalDataSource?.snapshot().sectionIdentifiers
        let itemIDsBefore = controller.internalDataSource?.snapshot().itemIdentifiers
        XCTAssertNil(boxAttributes(cv, section: 0))

        controller.style = .modern
        awaitCondition(
            "Modern への切替で箱の装飾が現れる",
            in: cv,
            actual: { "箱 \(String(describing: boxAttributes(cv, section: 0)))" },
            until: { boxAttributes(cv, section: 0) != nil }
        )

        XCTAssertEqual(controller.internalDataSource?.snapshot().sectionIdentifiers, sectionIDsBefore)
        XCTAssertEqual(controller.internalDataSource?.snapshot().itemIdentifiers, itemIDsBefore)
        XCTAssertNotNil(boxAttributes(cv, section: 0), "Modern へ切り替えても箱が描かれていない")
        XCTAssertEqual(separator(controller, section: 0, item: 0).topSeparatorVisibility, .hidden)
    }

    func test_ModernからClassicへの切替で装飾が外れる() {
        let section = KsSettingsViewCore.Section(cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        let (controller, cv, window) = host(root: SettingsRoot(sections: [section]), style: .modern)
        defer { window.isHidden = true }
        XCTAssertNotNil(boxAttributes(cv, section: 0))

        controller.style = .classic
        awaitCondition(
            "Classic への切替で箱の装飾が外れる",
            in: cv,
            actual: { "箱 \(String(describing: boxAttributes(cv, section: 0)))" },
            until: { boxAttributes(cv, section: 0) == nil }
        )

        XCTAssertNil(boxAttributes(cv, section: 0))
        XCTAssertEqual(separator(controller, section: 0, item: 0).topSeparatorVisibility, .visible)
        guard let cell = cv.cellForItem(at: IndexPath(item: 0, section: 0)) else {
            return XCTFail("Cell を取得できない")
        }
        XCTAssertNil(cell.layer.mask, "Classic へ戻したのに箱の clip が残っている")
    }
}
#endif

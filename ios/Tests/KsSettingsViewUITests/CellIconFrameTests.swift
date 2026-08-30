// CellIconFrameTests.swift
// KsSettingsViewUITests
//
// 共通行レイアウトの icon 領域が「解決済み icon size を一辺とする正方形枠」になることを検証する。
//
// 枠の寸法は画像の intrinsic size（SF Symbols の字形差・任意寸法の `UIImage`）に依存せず、
// 角丸はその正方形枠に対してかかる（core/ADR-0025）。icon を持たない Cell では枠のサイズ制約が
// 無効化され、`UIStackView` が非表示の arranged subview に張る寸法 0 の制約と競合しない。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class CellIconFrameTests: XCTestCase {

    // MARK: - intrinsic size に依存しない正方形枠

    /// intrinsic 幅の異なる SF Symbols を並べても、全行で icon 枠と title の開始位置が揃う。
    ///
    /// 実際に `KsSettingsViewController` へ載せた行を測る。字形ごとの intrinsic 幅が本当に
    /// 異なることを先に確認し、テストの前提が空振りしないようにする。
    func test_intrinsic幅が異なるSFSymbolsでもicon列幅が揃う() {
        let symbols = ["wifi", "airplane", "1.square", "bell"]
        let intrinsicWidths = symbols.compactMap { UIImage(systemName: $0)?.size.width }
        XCTAssertEqual(intrinsicWidths.count, symbols.count, "検証に使う SF Symbols がすべて解決できる")
        XCTAssertGreaterThan(
            Set(intrinsicWidths.map { ($0 * 100).rounded() }).count, 1,
            "intrinsic 幅の異なる字形が混ざっていないと枠の検証にならない — 実際: \(intrinsicWidths)"
        )

        let theme = Theme(cellIconSize: 29)
        let cells = symbols.map { LabelCell(title: "行 \($0)", icon: KsImage.systemName($0)) }
        let (_, cv, window) = hostController(
            root: SettingsRoot(sections: [Section(cells: cells)]),
            theme: theme
        )
        defer { window.isHidden = true }

        var titleLeadings: [CGFloat] = []
        for item in 0..<symbols.count {
            let cell = listCell(cv, item: item)
            XCTAssertFalse(cell.iconImageView.isHidden, "\(symbols[item]): icon 領域が表示されている")
            XCTAssertEqual(
                cell.iconImageView.bounds.width, 29, accuracy: 0.5,
                "\(symbols[item]): icon 枠の幅は解決済み icon size"
            )
            XCTAssertEqual(
                cell.iconImageView.bounds.height, 29, accuracy: 0.5,
                "\(symbols[item]): icon 枠の高さは解決済み icon size"
            )
            titleLeadings.append(cell.titleLabel.convert(cell.titleLabel.bounds, to: cell.contentView).minX)
        }
        for (index, leading) in titleLeadings.enumerated() {
            XCTAssertEqual(
                leading, titleLeadings[0], accuracy: 0.5,
                "\(symbols[index]): title の開始位置が先頭行と一致する"
            )
        }
    }

    /// 枠より幅も高さも大きい `UIImage` でも枠は解決済みサイズのままで、画像は枠を超えない。
    func test_枠より大きいintrinsicSizeの画像でも枠は解決済みサイズのまま() {
        let cell = makeStandaloneCell()
        let theme = Theme(cellIconSize: 24)
        let image = makeImage(width: 120, height: 90)
        XCTAssertGreaterThan(image.size.width, 24, "枠より大きい画像でなければ検証にならない")
        XCTAssertGreaterThan(image.size.height, 24, "枠より大きい画像でなければ検証にならない")

        render(cell, icon: .uiImage(image), theme: theme)
        layoutStandalone(cell)

        XCTAssertEqual(cell.iconImageView.bounds.width, 24, accuracy: 0.5, "枠の幅は解決済み icon size")
        XCTAssertEqual(cell.iconImageView.bounds.height, 24, accuracy: 0.5, "枠の高さは解決済み icon size")
        XCTAssertEqual(cell.iconImageView.contentMode, .scaleAspectFit, "画像は枠へ aspect fit で収める")
        XCTAssertTrue(cell.iconImageView.clipsToBounds, "画像は枠を超えない")
    }

    /// `CellStyle.iconSize` は `Theme.cellIconSize` より優先される。
    func test_CellStyleのiconSizeはThemeより優先される() {
        let cell = makeStandaloneCell()
        render(
            cell,
            icon: .systemName("bell"),
            theme: Theme(cellIconSize: 29),
            cellStyle: CellStyle(iconSize: 44)
        )
        layoutStandalone(cell)

        XCTAssertEqual(cell.iconImageView.bounds.width, 44, accuracy: 0.5, "CellStyle の指定が枠になる")
        XCTAssertEqual(cell.iconImageView.bounds.height, 44, accuracy: 0.5, "CellStyle の指定が枠になる")
    }

    /// `applyTheme(_:)` による Theme 変更が、表示中の行の枠へ反映される。
    func test_applyTheme経由のTheme変更で表示中の行の枠が更新される() {
        let cells = [LabelCell(title: "通知", icon: KsImage.systemName("bell"))]
        let (controller, cv, window) = hostController(
            root: SettingsRoot(sections: [Section(cells: cells)]),
            theme: Theme(cellIconSize: 24)
        )
        defer { window.isHidden = true }

        XCTAssertEqual(
            listCell(cv, item: 0).iconImageView.bounds.width, 24, accuracy: 0.5,
            "適用前の枠は元 Theme の cellIconSize"
        )

        controller.applyTheme(Theme(cellIconSize: 40))
        pump(cv)

        let cell = listCell(cv, item: 0)
        XCTAssertEqual(cell.iconImageView.bounds.width, 40, accuracy: 0.5, "枠の幅が新 Theme に追従する")
        XCTAssertEqual(cell.iconImageView.bounds.height, 40, accuracy: 0.5, "枠の高さが新 Theme に追従する")
    }

    /// icon を持たない Cell では枠のサイズ制約が無効化され、title は icon 領域の余白を伴わない。
    func test_iconのないCellでは枠の制約が無効化される() {
        let cell = makeStandaloneCell()
        render(cell, icon: nil, theme: Theme())
        layoutStandalone(cell)

        XCTAssertTrue(cell.iconImageView.isHidden, "icon 領域は非表示")
        XCTAssertEqual(cell.iconWidthConstraint?.isActive, false, "幅を固定する制約は無効")
        XCTAssertEqual(cell.iconHeightConstraint?.isActive, false, "高さを固定する制約は無効")
        XCTAssertEqual(
            cell.titleLabel.convert(cell.titleLabel.bounds, to: cell.contentView).minX,
            cell.stackH.layoutMargins.left,
            accuracy: 0.5,
            "title は icon 領域の余白を伴わず leading margin から始まる"
        )
    }

    /// icon なしで bind した行を icon 付きで再 bind すると、枠の制約が有効に戻る。
    func test_iconなしからiconありの再bindで枠が戻る() {
        let cell = makeStandaloneCell()
        let theme = Theme(cellIconSize: 32)

        render(cell, icon: nil, theme: theme)
        layoutStandalone(cell)
        XCTAssertEqual(cell.iconWidthConstraint?.isActive, false, "icon なしの時点では制約が無効")

        render(cell, icon: .systemName("bell"), theme: theme)
        layoutStandalone(cell)

        XCTAssertFalse(cell.iconImageView.isHidden, "再 bind で icon 領域が表示される")
        XCTAssertEqual(cell.iconWidthConstraint?.isActive, true, "幅を固定する制約が有効に戻る")
        XCTAssertEqual(cell.iconHeightConstraint?.isActive, true, "高さを固定する制約が有効に戻る")
        XCTAssertEqual(cell.iconImageView.bounds.width, 32, accuracy: 0.5, "枠の幅は解決済み icon size")
        XCTAssertEqual(cell.iconImageView.bounds.height, 32, accuracy: 0.5, "枠の高さは解決済み icon size")
    }

    /// リサイクル（`prepareForReuse`）を挟んでも、icon 付きの再 bind で枠が戻る。
    func test_prepareForReuse後の再bindで枠が戻る() {
        let cell = makeStandaloneCell()
        let theme = Theme(cellIconSize: 36)

        render(cell, icon: .systemName("bell"), theme: theme)
        layoutStandalone(cell)
        XCTAssertEqual(cell.iconImageView.bounds.width, 36, accuracy: 0.5, "初回 bind で枠が確保される")

        cell.prepareForReuse()
        layoutStandalone(cell)
        XCTAssertTrue(cell.iconImageView.isHidden, "リサイクル直後は icon 領域が非表示")
        XCTAssertEqual(cell.iconWidthConstraint?.isActive, false, "リサイクル直後は制約が無効")

        render(cell, icon: .systemName("airplane"), theme: theme)
        layoutStandalone(cell)
        XCTAssertEqual(cell.iconWidthConstraint?.isActive, true, "再 bind で制約が有効に戻る")
        XCTAssertEqual(cell.iconImageView.bounds.width, 36, accuracy: 0.5, "枠の幅が解決値に戻る")
        XCTAssertEqual(cell.iconImageView.bounds.height, 36, accuracy: 0.5, "枠の高さが解決値に戻る")
    }

    /// 無効な `CellStyle.iconSize` は未指定として扱われ、枠は Theme の値になる。
    func test_無効なiconSizeは未指定として次の段へ解決する() {
        for invalid in [CGFloat(0), -12, .nan, .infinity] {
            let cell = makeStandaloneCell()
            render(
                cell,
                icon: .systemName("bell"),
                theme: Theme(cellIconSize: 30),
                cellStyle: CellStyle(iconSize: invalid)
            )
            layoutStandalone(cell)
            XCTAssertEqual(
                cell.iconImageView.bounds.width, 30, accuracy: 0.5,
                "無効な iconSize (\(invalid)) は Theme の cellIconSize へ解決する"
            )
            XCTAssertEqual(
                cell.iconImageView.bounds.height, 30, accuracy: 0.5,
                "無効な iconSize (\(invalid)) は Theme の cellIconSize へ解決する"
            )
        }
    }

    // MARK: - 正方形枠に対する角丸

    /// 角丸は正方形枠に対してかかり、aspect fit 後の描画矩形には追従しない（core/ADR-0025）。
    func test_角丸は枠に対してかかり画像の描画矩形には追従しない() {
        let theme = Theme(cellIconSize: 32, cellIconRadius: 7)

        let nonSquare = makeStandaloneCell()
        render(nonSquare, icon: .uiImage(makeImage(width: 120, height: 40)), theme: theme)
        layoutStandalone(nonSquare)

        let square = makeStandaloneCell()
        render(square, icon: .uiImage(makeImage(width: 64, height: 64)), theme: theme)
        layoutStandalone(square)

        XCTAssertEqual(nonSquare.iconImageView.bounds.width, 32, accuracy: 0.5, "非正方形画像でも枠は正方形")
        XCTAssertEqual(nonSquare.iconImageView.bounds.height, 32, accuracy: 0.5, "非正方形画像でも枠は正方形")
        XCTAssertEqual(nonSquare.iconImageView.layer.cornerRadius, 7, accuracy: 0.01, "角丸は解決済み radius")
        XCTAssertTrue(nonSquare.iconImageView.clipsToBounds, "枠に対して clip する")
        XCTAssertEqual(nonSquare.iconImageView.contentMode, .scaleAspectFit, "画像は枠へ aspect fit で収める")
        XCTAssertEqual(
            nonSquare.iconImageView.layer.cornerRadius,
            square.iconImageView.layer.cornerRadius,
            accuracy: 0.01,
            "画像の縦横比が変わっても clip 形状は変わらない"
        )
    }

    /// 角丸未指定なら clip 形状は角丸にならない。
    func test_角丸未指定ならclipしない() {
        let cell = makeStandaloneCell()
        render(cell, icon: .systemName("bell"), theme: Theme())
        layoutStandalone(cell)

        XCTAssertEqual(cell.iconImageView.layer.cornerRadius, 0, accuracy: 0.01, "既定は角丸なし")
    }

    /// 無効な `CellStyle.iconRadius` は未指定として扱われ、角丸は Theme の値になる。
    func test_無効なradiusは未指定として次の段へ解決する() {
        for invalid in [CGFloat(-3), .nan, -.infinity] {
            let cell = makeStandaloneCell()
            render(
                cell,
                icon: .systemName("bell"),
                theme: Theme(cellIconRadius: 6),
                cellStyle: CellStyle(iconRadius: invalid)
            )
            layoutStandalone(cell)
            XCTAssertEqual(
                cell.iconImageView.layer.cornerRadius, 6, accuracy: 0.01,
                "無効な iconRadius (\(invalid)) は Theme の cellIconRadius へ解決する"
            )
        }
    }

    // MARK: - Helper

    /// 単体で測る `KsListCellBase` を生成する。
    private func makeStandaloneCell() -> KsListCellBase {
        return IconFrameTestCell(frame: CGRect(x: 0, y: 0, width: 320, height: 60))
    }

    /// 共通行レイアウトを適用する。
    private func render(
        _ cell: KsListCellBase,
        title: String = "通知",
        icon: KsImage?,
        theme: Theme,
        cellStyle: CellStyle = CellStyle()
    ) {
        applyCellBaseLayout(
            cell,
            title: title,
            description: nil,
            icon: icon,
            hintText: nil,
            effective: EffectiveStyle(theme: theme, cellStyle: cellStyle),
            theme: theme,
            isEnabled: true
        )
    }

    /// 固定幅で self-sizing させ、実寸を確定させる。
    private func layoutStandalone(_ cell: KsListCellBase, width: CGFloat = 320) {
        cell.frame = CGRect(x: 0, y: 0, width: width, height: 0)
        cell.frame.size = cell.systemLayoutSizeFitting(
            CGSize(width: width, height: UIView.layoutFittingCompressedSize.height),
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel
        )
        cell.layoutIfNeeded()
    }

    /// 指定寸法の単色 `UIImage` を作る。
    private func makeImage(width: CGFloat, height: CGFloat) -> UIImage {
        let size = CGSize(width: width, height: height)
        return UIGraphicsImageRenderer(size: size).image { context in
            UIColor.systemGreen.setFill()
            context.fill(CGRect(origin: .zero, size: size))
        }
    }

    /// Controller を window に載せ、行の実描画を確定させる。
    private func hostController(
        root: SettingsRoot,
        theme: Theme
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(root: root, theme: theme)
        let size = CGSize(width: 375, height: 600)
        let hostView = controller.view!
        hostView.frame = CGRect(origin: .zero, size: size)
        let window = UIWindow(frame: hostView.frame)
        window.addSubview(hostView)
        window.makeKeyAndVisible()
        hostView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        pump(cv)
        return (controller, cv, window)
    }

    /// レイアウトと再構成を確定させる。
    private func pump(_ view: UIView, seconds: TimeInterval = 0.05) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    /// 先頭 Section の指定行に表示されている Cell を取り出す。
    private func listCell(_ cv: UICollectionView, item: Int) -> KsListCellBase {
        guard let cell = cv.cellForItem(at: IndexPath(item: item, section: 0)) as? KsListCellBase else {
            fatalError("item \(item) の行が表示されていない")
        }
        return cell
    }
}

/// `KsListCellBase` をそのまま測るためのテスト固有 subclass。
@MainActor
private final class IconFrameTestCell: KsListCellBase {
}
#endif

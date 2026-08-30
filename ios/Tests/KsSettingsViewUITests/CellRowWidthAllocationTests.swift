// CellRowWidthAllocationTests.swift
// KsSettingsViewUITests
//
// 主行（title と行内 trailing）の幅配分の契約を固定する。Android の `CellRowWidthAllocationTest`
// と対になるテストで、両 platform で同じ配分になることを保証する。
//
// 契約（core/ADR-0026）:
//   - title はコンテンツ幅を確保する（主行幅を上限とし、超える分だけ末尾省略）
//   - valueText は主行の残り幅を占め、収まらない分を末尾省略する
//   - icon 枠と Cell 級アクセサリの幅は主行より先に譲らない
//   - 行内 trailing がない Cell では title が主行の全幅を使う
//   - EntryCell は title がコンテンツ幅、入力フィールドが残り幅

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class CellRowWidthAllocationTests: XCTestCase {

    // MARK: - valueText 系

    /// 主行幅を超える valueText は残り幅で末尾省略され、title は全文残る。
    ///
    /// icon と Cell 級アクセサリを持つ行で、主行だけが縮んで両者の幅が維持されることも併せて見る。
    func test_長いvalueTextは省略されtitleは全文残る() {
        let cell = makeStandaloneCell()
        let theme = Theme(cellIconSize: 32)
        let accessory = makeChevronView()
        render(
            cell,
            title: "音量",
            icon: .systemName("speaker.wave.2"),
            theme: theme,
            valueText: "主行の幅を大きく超える長さの値テキストをここに設定して末尾省略を確認する",
            accessoryView: accessory
        )
        layoutStandalone(cell, width: 320)

        let title = cell.titleLabel
        guard let value = valueLabel(of: cell) else {
            return XCTFail("valueText が行内 trailing として配置されていない")
        }

        XCTAssertEqual(
            title.bounds.width, naturalWidth(of: title), accuracy: 0.5,
            "title はコンテンツ幅を保つ"
        )
        assertNotTruncated(title, "全文表示される title")
        assertTruncatedAtEnd(value, "残り幅に収まらない valueText")
        XCTAssertEqual(
            value.bounds.width,
            cell.contentStack.bounds.width - title.bounds.width - cell.contentStack.spacing,
            accuracy: 0.5,
            "valueText は主行の残り幅を占める"
        )

        // icon 枠と Cell 級アクセサリは主行より先に譲らない。
        XCTAssertEqual(cell.iconImageView.bounds.width, 32, accuracy: 0.5, "icon 枠の幅は縮まない")
        XCTAssertEqual(cell.iconImageView.bounds.height, 32, accuracy: 0.5, "icon 枠は正方形のまま")
        XCTAssertEqual(
            cell.accessoryHolder.bounds.width,
            accessory.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize).width,
            accuracy: 0.5,
            "Cell 級アクセサリの幅は縮まない"
        )
        assertNoOverflow(cell)
    }

    /// 主行幅を超える title は上限で末尾省略され、valueText には残り幅（0 以上）が渡る。
    func test_主行幅を超えるtitleは上限で省略されvalueTextは残り幅になる() {
        let cell = makeStandaloneCell()
        render(
            cell,
            title: "とても長いタイトルは主行の幅を超えるため末尾省略される対象になる",
            icon: nil,
            theme: Theme(),
            valueText: "Green"
        )
        layoutStandalone(cell, width: 200)

        let title = cell.titleLabel
        guard let value = valueLabel(of: cell) else {
            return XCTFail("valueText が行内 trailing として配置されていない")
        }

        XCTAssertLessThanOrEqual(
            title.bounds.width, cell.contentStack.bounds.width + 0.5,
            "title は主行幅を上限とする"
        )
        assertTruncatedAtEnd(title, "主行幅を超える title")
        XCTAssertGreaterThanOrEqual(value.bounds.width, 0, "valueText の幅は 0 以上")
        XCTAssertEqual(
            value.bounds.width,
            max(0, cell.contentStack.bounds.width - title.bounds.width - cell.contentStack.spacing),
            accuracy: 0.5,
            "valueText の幅は主行の残り幅になる"
        )
        assertNoOverflow(cell)
    }

    /// 行内 trailing がない Cell では title が主行の全幅を使う。
    func test_行内trailingがないCellではtitleが主行の全幅を使う() {
        let cell = makeStandaloneCell()
        render(cell, title: "通知", icon: nil, theme: Theme())
        layoutStandalone(cell, width: 320)

        XCTAssertEqual(
            cell.contentStack.arrangedSubviews.count, 1,
            "行内 trailing を持たない構成である"
        )
        XCTAssertEqual(
            cell.titleLabel.bounds.width, cell.contentStack.bounds.width, accuracy: 0.5,
            "title の領域は主行の全幅に等しい"
        )
    }

    /// 同じ行で valueText の有無が切り替わっても、行内 trailing の有無に応じた配分が追随する。
    ///
    /// valueText があるときは title が全文残って valueText が主行の右端に付き、無いときは
    /// title が主行の全幅を使う。ViewHolder 再利用時に前回の配分が残らないことを固定する。
    func test_同じ行でvalueTextの有無が切り替わっても配分が追随する() {
        let cell = makeStandaloneCell()

        render(cell, title: "通知", icon: nil, theme: Theme(), valueText: "オン")
        layoutStandalone(cell, width: 320)
        guard let value = valueLabel(of: cell) else {
            return XCTFail("valueText が行内 trailing として配置されていない")
        }
        assertNotTruncated(cell.titleLabel, "valueText があるときの title")
        XCTAssertEqual(
            value.convert(value.bounds, to: cell.contentStack).maxX,
            cell.contentStack.bounds.maxX,
            accuracy: 0.5,
            "valueText は主行の右端に付く"
        )

        render(cell, title: "通知", icon: nil, theme: Theme())
        layoutStandalone(cell, width: 320)
        XCTAssertEqual(
            cell.contentStack.arrangedSubviews.count, 1,
            "valueText が無いとき行内 trailing は残らない"
        )
        XCTAssertEqual(
            cell.titleLabel.bounds.width, cell.contentStack.bounds.width, accuracy: 0.5,
            "valueText が無いとき title は主行の全幅を使う"
        )

        render(cell, title: "通知", icon: nil, theme: Theme(), valueText: "オン")
        layoutStandalone(cell, width: 320)
        guard let restored = valueLabel(of: cell) else {
            return XCTFail("再 bind で valueText が行内 trailing に戻らない")
        }
        assertNotTruncated(cell.titleLabel, "valueText が戻ったときの title")
        XCTAssertEqual(
            restored.convert(restored.bounds, to: cell.contentStack).maxX,
            cell.contentStack.bounds.maxX,
            accuracy: 0.5,
            "valueText が戻ると再び主行の右端に付く"
        )
        assertNoOverflow(cell)
    }

    // MARK: - EntryCell

    /// EntryCell では title がコンテンツ幅を維持し、入力フィールドが残り幅を占める。
    func test_EntryCellではtitleがコンテンツ幅を維持し入力フィールドが縮む() {
        let view = EntryCellView(frame: CGRect(x: 0, y: 0, width: 320, height: 60))
        view.render(
            cell: EntryCell(title: "とても長いラベルのついた入力欄", text: "value"),
            theme: Theme()
        )
        layoutStandalone(view, width: 320)

        let title = view.titleLabel
        guard let field = view.contentStack.arrangedSubviews.first(where: { $0 !== title }) else {
            return XCTFail("入力フィールドが行内 trailing として配置されていない")
        }

        XCTAssertEqual(
            title.bounds.width, naturalWidth(of: title), accuracy: 0.5,
            "title はコンテンツ幅を維持する"
        )
        XCTAssertGreaterThanOrEqual(field.bounds.width, 0, "入力フィールドの幅は 0 以上")
        XCTAssertEqual(
            field.bounds.width,
            max(0, view.contentStack.bounds.width - title.bounds.width - view.contentStack.spacing),
            accuracy: 0.5,
            "入力フィールドは主行の残り幅を占める"
        )
        assertNoOverflow(view)
    }

    // MARK: - icon 枠は主行より先に譲らない

    /// 行幅が自然幅の合計より狭くても icon 枠は縮まず、title が末尾省略される。
    func test_狭幅でもicon枠は縮まない() {
        let cell = makeStandaloneCell()
        render(
            cell,
            title: "とても長いタイトルで主行の幅を使い切り末尾省略が起きるケースの検証",
            icon: .systemName("bell"),
            theme: Theme(cellIconSize: 44)
        )
        layoutStandalone(cell, width: 160)

        XCTAssertEqual(cell.iconImageView.bounds.width, 44, accuracy: 0.5, "icon 枠の幅は縮まない")
        XCTAssertEqual(cell.iconImageView.bounds.height, 44, accuracy: 0.5, "icon 枠の高さも縮まない")
        assertTruncatedAtEnd(cell.titleLabel, "狭幅の title")
        assertNoOverflow(cell)
    }

    // MARK: - Helper

    private func makeStandaloneCell() -> KsListCellBase {
        return RowWidthTestCell(frame: CGRect(x: 0, y: 0, width: 320, height: 60))
    }

    private func render(
        _ cell: KsListCellBase,
        title: String,
        icon: KsImage?,
        theme: Theme,
        valueText: String? = nil,
        accessoryView: UIView? = nil
    ) {
        applyCellBaseLayout(
            cell,
            title: title,
            description: nil,
            icon: icon,
            hintText: nil,
            effective: EffectiveStyle(theme: theme, cellStyle: CellStyle()),
            theme: theme,
            isEnabled: true,
            valueLabelText: valueText,
            accessoryView: accessoryView
        )
    }

    /// 指定幅で self-sizing させ、実寸を確定させる。
    private func layoutStandalone(_ cell: UIView, width: CGFloat) {
        cell.frame = CGRect(x: 0, y: 0, width: width, height: 0)
        cell.frame.size = cell.systemLayoutSizeFitting(
            CGSize(width: width, height: UIView.layoutFittingCompressedSize.height),
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel
        )
        cell.layoutIfNeeded()
    }

    /// `contentStack` の行内 trailing として置かれた value label を取り出す。
    private func valueLabel(of cell: KsListCellBase) -> UILabel? {
        let trailing = cell.contentStack.arrangedSubviews.filter { $0 !== cell.titleLabel }
        return trailing.first as? UILabel
    }

    /// 末尾省略なしで全文を描くのに必要な幅。
    private func naturalWidth(of label: UILabel) -> CGFloat {
        return label.sizeThatFits(
            CGSize(width: CGFloat.greatestFiniteMagnitude, height: label.bounds.height)
        ).width
    }

    /// 1 行・末尾省略の `UILabel` が実際に省略されていることを確かめる。
    ///
    /// `numberOfLines == 1` かつ `lineBreakMode == .byTruncatingTail` の `UILabel` は、
    /// 与えられた幅が全文の描画幅より狭いとき必ず末尾を省略して描く。
    private func assertTruncatedAtEnd(_ label: UILabel, _ what: String) {
        XCTAssertEqual(label.numberOfLines, 1, "\(what): 1 行表示である")
        XCTAssertEqual(label.lineBreakMode, .byTruncatingTail, "\(what): 末尾省略の設定である")
        XCTAssertLessThan(
            label.bounds.width, naturalWidth(of: label) - 0.5,
            "\(what): 全文の描画幅より狭い幅しか与えられていない（= 末尾省略される）"
        )
    }

    /// `UILabel` が全文を描き切れる幅を持つことを確かめる。
    private func assertNotTruncated(_ label: UILabel, _ what: String) {
        XCTAssertGreaterThanOrEqual(
            label.bounds.width, naturalWidth(of: label) - 0.5,
            "\(what): 全文の描画幅以上の幅を持つ"
        )
    }

    /// 行内の要素が `stackH` の layout margin の内側に収まっていることを確かめる。
    private func assertNoOverflow(_ cell: UIView) {
        guard let stackH = (cell as? KsListCellBase)?.stackH else {
            return XCTFail("stackH を持たない View が渡された")
        }
        let inner = stackH.bounds.inset(by: stackH.layoutMargins)
        for view in stackH.arrangedSubviews where !view.isHidden {
            let frame = view.convert(view.bounds, to: stackH)
            XCTAssertLessThanOrEqual(
                frame.maxX, inner.maxX + 0.5,
                "\(type(of: view)) が主行の右端からはみ出している"
            )
            XCTAssertGreaterThanOrEqual(
                frame.minX, inner.minX - 0.5,
                "\(type(of: view)) が主行の左端からはみ出している"
            )
        }
    }
}

/// `KsListCellBase` をそのまま測るためのテスト固有 subclass。
@MainActor
private final class RowWidthTestCell: KsListCellBase {
}
#endif

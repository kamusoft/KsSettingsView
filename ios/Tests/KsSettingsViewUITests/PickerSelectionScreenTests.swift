// PickerSelectionScreenTests.swift
// KsSettingsViewUITests
//
// `PickerCell` 選択面（`PickerListViewController`）のスタイル継承・ナビゲーションバー適用・
// タイトル解決・アクセシビリティ状態・初期スクロールを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class PickerSelectionScreenTests: XCTestCase {

    // MARK: - テストヘルパ

    /// `PickerCellView.render` を経た**提示経路と同一の配線**で選択面 VC を組み立てる。
    /// VC を直接生成すると `render` からの配線漏れを検出できないため、スタイル系の検証はこの経路を使う。
    private func makeScreenViaWiring(
        cell: PickerCell,
        theme: Theme = Theme()
    ) throws -> PickerListViewController {
        let view = PickerCellView()
        view.render(cell: cell, theme: theme)
        return try XCTUnwrap(view._makeListViewControllerForTesting())
    }

    /// VC をレイアウトさせて行 View を実体化する（`viewDidLayoutSubviews` 経由の初期スクロールも発火する）。
    private func layout(_ vc: PickerListViewController, height: CGFloat = 600) {
        vc.loadViewIfNeeded()
        vc.view.frame = CGRect(x: 0, y: 0, width: 375, height: height)
        vc.view.layoutIfNeeded()
    }

    /// データソース経由で候補行を取得する。
    private func row(_ vc: PickerListViewController, _ index: Int) -> UITableViewCell {
        return vc.tableView(vc.tableView, cellForRowAt: IndexPath(row: index, section: 0))
    }

    private func singleCell(
        items: [String] = ["A", "B", "C"],
        selectedIndex: Int? = 1,
        style: CellStyle = CellStyle(),
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onSelectionChanged: (@Sendable (Int) -> Void)? = nil
    ) -> PickerCell {
        return PickerCell(
            style: style,
            title: "サイズ",
            items: items,
            selectedIndex: selectedIndex,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onSelectionChanged: onSelectionChanged
        )
    }

    private func multiCell(
        items: [String] = ["A", "B", "C"],
        selectedIndices: Set<Int> = [1],
        style: CellStyle = CellStyle(),
        pageTitle: String? = nil,
        accentColor: UIColor? = nil,
        onMultiSelectionChanged: (@Sendable (Set<Int>) -> Void)? = nil
    ) -> PickerCell {
        return PickerCell(
            style: style,
            title: "通知種別",
            items: items,
            selectedIndices: selectedIndices,
            pageTitle: pageTitle,
            accentColor: accentColor,
            onMultiSelectionChanged: onMultiSelectionChanged
        )
    }

    // MARK: - スタイル継承

    func test_選択面_候補行のタイトルが実効値で描画される() throws {
        let theme = Theme(cellTitleColor: .red, cellTitleFontSize: 22)
        let vc = try makeScreenViaWiring(cell: singleCell(), theme: theme)
        vc.loadViewIfNeeded()

        let cell = row(vc, 0)
        XCTAssertEqual(cell.textLabel?.textColor, UIColor.red)
        XCTAssertEqual(cell.textLabel?.font.pointSize, 22)
    }

    func test_選択面_CellStyleはThemeより優先される() throws {
        let theme = Theme(cellBackgroundColor: .green, cellTitleColor: .red)
        let style = CellStyle(titleColor: .blue, backgroundColor: .yellow)
        let vc = try makeScreenViaWiring(cell: singleCell(style: style), theme: theme)
        vc.loadViewIfNeeded()

        let cell = row(vc, 0)
        XCTAssertEqual(cell.textLabel?.textColor, UIColor.blue)
        XCTAssertEqual(cell.backgroundColor, UIColor.yellow)
        XCTAssertEqual(vc.tableView.backgroundColor, UIColor.yellow)
    }

    func test_選択面_背景と区切り線とハイライトがThemeから解決される() throws {
        let theme = Theme(
            separatorColor: .magenta,
            cellBackgroundColor: .cyan,
            selectedColor: .orange
        )
        let vc = try makeScreenViaWiring(cell: singleCell(), theme: theme)
        vc.loadViewIfNeeded()

        XCTAssertEqual(vc.tableView.backgroundColor, UIColor.cyan)
        XCTAssertEqual(vc.tableView.separatorColor, UIColor.magenta)

        let cell = row(vc, 0)
        XCTAssertEqual(cell.backgroundColor, UIColor.cyan)
        XCTAssertEqual(cell.selectedBackgroundView?.backgroundColor, UIColor.orange)
    }

    func test_選択面_選択印はCell固有のaccentColorが最優先() throws {
        let theme = Theme(cellAccentColor: .brown)
        let style = CellStyle(accentColor: .purple)
        let vc = try makeScreenViaWiring(
            cell: singleCell(style: style, accentColor: .red),
            theme: theme
        )
        vc.loadViewIfNeeded()

        XCTAssertEqual(vc._resolvedAccentColor, UIColor.red)
        XCTAssertEqual(vc.tableView.tintColor, UIColor.red)
    }

    func test_選択面_選択印はCellStyleへフォールバックする() throws {
        let theme = Theme(cellAccentColor: .brown)
        let style = CellStyle(accentColor: .purple)
        let vc = try makeScreenViaWiring(
            cell: singleCell(style: style, accentColor: nil),
            theme: theme
        )
        vc.loadViewIfNeeded()

        XCTAssertEqual(vc._resolvedAccentColor, UIColor.purple)
        XCTAssertEqual(vc.tableView.tintColor, UIColor.purple)
    }

    func test_選択面_選択印はThemeの既定色へフォールバックする() throws {
        let theme = Theme(cellAccentColor: .brown)
        let vc = try makeScreenViaWiring(cell: singleCell(), theme: theme)
        vc.loadViewIfNeeded()

        XCTAssertEqual(vc._resolvedAccentColor, UIColor.brown)
        XCTAssertEqual(vc.tableView.tintColor, UIColor.brown)
    }

    // MARK: - 配線の検証 seam

    func test_選択面_presentPickerModal経由でCellStyleとThemeとCell固有accentが渡る() throws {
        let theme = Theme(
            separatorColor: .magenta,
            cellBackgroundColor: .cyan,
            selectedColor: .orange,
            cellAccentColor: .brown,
            cellTitleColor: .red
        )
        let style = CellStyle(titleColor: .blue)
        let vc = try makeScreenViaWiring(
            cell: singleCell(style: style, accentColor: .green),
            theme: theme
        )

        // CellStyle 優先 / Theme フォールバック / Cell 固有 accent の 3 系統が VC へ届いている
        XCTAssertEqual(vc._effectiveStyle.titleColor, UIColor.blue, "CellStyle の指定が届いている")
        XCTAssertEqual(vc._effectiveStyle.cellBackgroundColor, UIColor.cyan, "Theme の指定が届いている")
        XCTAssertEqual(vc._effectiveStyle.separatorColor, UIColor.magenta)
        XCTAssertEqual(vc._effectiveStyle.selectedBackgroundColor, UIColor.orange)
        XCTAssertEqual(vc._resolvedAccentColor, UIColor.green, "Cell 固有 accent が届いている")
    }

    // MARK: - ナビゲーションバーへのスタイル適用

    func test_選択面_単一選択のCancelへ解決値が反映される() throws {
        let vc = try makeScreenViaWiring(cell: singleCell(accentColor: .red))
        vc.loadViewIfNeeded()

        XCTAssertEqual(vc.navigationItem.leftBarButtonItem?.tintColor, UIColor.red)
        XCTAssertEqual(vc.navigationItem.leftBarButtonItem?.tintColor, vc._resolvedAccentColor)
        XCTAssertNil(vc.navigationItem.rightBarButtonItem, "単一選択に確定ボタンは無い")
    }

    func test_選択面_複数選択のCancelと確定とタイトルへ解決値が反映される() throws {
        let theme = Theme(cellAccentColor: .brown, cellTitleColor: .red)
        let vc = try makeScreenViaWiring(cell: multiCell(), theme: theme)
        vc.loadViewIfNeeded()

        XCTAssertEqual(vc.navigationItem.leftBarButtonItem?.tintColor, UIColor.brown)
        XCTAssertEqual(vc.navigationItem.rightBarButtonItem?.tintColor, UIColor.brown)
        XCTAssertEqual(vc.navigationItem.rightBarButtonItem?.tintColor, vc._resolvedAccentColor)

        let titleColor = vc.navigationItem.standardAppearance?
            .titleTextAttributes[.foregroundColor] as? UIColor
        XCTAssertEqual(titleColor, UIColor.red)
    }

    // MARK: - タイトル解決

    func test_選択面_pageTitleが指定されていればそれを表示する() throws {
        let vc = try makeScreenViaWiring(cell: singleCell(pageTitle: "サイズを選択"))
        XCTAssertEqual(vc.title, "サイズを選択")
    }

    func test_選択面_pageTitleがnilならCellのtitleを表示する() throws {
        let cell = PickerCell(title: "テーマ", items: ["A"], selectedIndex: nil, pageTitle: nil)
        let vc = try makeScreenViaWiring(cell: cell)
        XCTAssertEqual(vc.title, "テーマ")
    }

    // MARK: - 候補行の副表示

    /// `PickerItem` を直接与える単一選択 Cell（副表示の検証用）。
    private func itemCell(
        items: [PickerItem],
        selectedIndex: Int? = 0,
        style: CellStyle = CellStyle()
    ) -> PickerCell {
        return PickerCell(
            style: style,
            title: "メンバー",
            items: items,
            selectedIndex: selectedIndex
        )
    }

    func test_選択面_subTextを持つ候補行は副表示を描画する() throws {
        let vc = try makeScreenViaWiring(
            cell: itemCell(items: [PickerItem(text: "佐藤 花子", subText: "プロダクトマネージャー")])
        )
        vc.loadViewIfNeeded()

        let cell = row(vc, 0)
        XCTAssertEqual(cell.textLabel?.text, "佐藤 花子")
        XCTAssertEqual(cell.detailTextLabel?.text, "プロダクトマネージャー")
    }

    func test_選択面_副表示はdescription系統の実効値で描画される() throws {
        let theme = Theme(cellDescriptionColor: .brown, cellDescriptionFont: .systemFont(ofSize: 11))
        let vc = try makeScreenViaWiring(
            cell: itemCell(items: [PickerItem(text: "佐藤 花子", subText: "PM")]),
            theme: theme
        )
        vc.loadViewIfNeeded()

        let cell = row(vc, 0)
        XCTAssertEqual(cell.detailTextLabel?.textColor, UIColor.brown)
        XCTAssertEqual(cell.detailTextLabel?.font.pointSize, 11)
    }

    func test_選択面_副表示はCellStyleがThemeより優先される() throws {
        let theme = Theme(cellDescriptionColor: .brown, cellDescriptionFont: .systemFont(ofSize: 11))
        let style = CellStyle(descriptionColor: .purple, descriptionFont: .systemFont(ofSize: 9))
        let vc = try makeScreenViaWiring(
            cell: itemCell(items: [PickerItem(text: "佐藤 花子", subText: "PM")], style: style),
            theme: theme
        )
        vc.loadViewIfNeeded()

        let cell = row(vc, 0)
        XCTAssertEqual(cell.detailTextLabel?.textColor, UIColor.purple)
        XCTAssertEqual(cell.detailTextLabel?.font.pointSize, 9)
    }

    func test_選択面_混在リストは副表示のある行だけが2行構成になる() throws {
        let vc = try makeScreenViaWiring(cell: itemCell(items: [
            PickerItem(text: "佐藤 花子", subText: "プロダクトマネージャー"),
            PickerItem(text: "全体アナウンス"),
            PickerItem(text: "田中 三郎", subText: "デザイナー"),
        ]))
        layout(vc)

        XCTAssertEqual(row(vc, 0).detailTextLabel?.text, "プロダクトマネージャー")
        XCTAssertNil(row(vc, 1).detailTextLabel?.text, "副表示なしの行は主表示のみ")
        XCTAssertEqual(row(vc, 2).detailTextLabel?.text, "デザイナー")

        let table = vc.tableView!
        let withSub = table.rectForRow(at: IndexPath(row: 0, section: 0)).height
        let withoutSub = table.rectForRow(at: IndexPath(row: 1, section: 0)).height
        XCTAssertGreaterThan(withSub, withoutSub, "副表示のある行だけ行高が伸びる")
    }

    func test_選択面_長い副表示は1行に収めて末尾を省略する() throws {
        let long = String(repeating: "モバイルアプリ開発チーム / テックリード ", count: 5)
        let vc = try makeScreenViaWiring(cell: itemCell(items: [
            PickerItem(text: "鈴木 一郎", subText: long),
            PickerItem(text: "鈴木 二郎", subText: "短い副表示"),
        ]))
        layout(vc)

        let cell = row(vc, 0)
        XCTAssertEqual(cell.detailTextLabel?.numberOfLines, 1)
        XCTAssertEqual(cell.detailTextLabel?.lineBreakMode, .byTruncatingTail)

        let table = vc.tableView!
        XCTAssertEqual(
            table.rectForRow(at: IndexPath(row: 0, section: 0)).height,
            table.rectForRow(at: IndexPath(row: 1, section: 0)).height,
            accuracy: 0.5,
            "副表示あり行の行高は副表示の長さに依存しない"
        )
    }

    func test_選択面_行の再利用で副表示が残らない() throws {
        let vc = try makeScreenViaWiring(cell: itemCell(items: [
            PickerItem(text: "佐藤 花子", subText: "プロダクトマネージャー"),
            PickerItem(text: "全体アナウンス"),
        ]))
        layout(vc)

        _ = row(vc, 0)
        // 同一 reuse identifier の行を続けて取得しても、副表示なしの行に前行の副表示が残らない。
        XCTAssertNil(row(vc, 1).detailTextLabel?.text)
    }

    // MARK: - 候補行のアクセシビリティ状態

    func test_選択面_候補行に表示名と選択状態が公開される() throws {
        let vc = try makeScreenViaWiring(cell: singleCell(items: ["A", "B", "C"], selectedIndex: 1))
        vc.loadViewIfNeeded()

        let selected = row(vc, 1)
        XCTAssertEqual(selected.accessibilityLabel, "B")
        XCTAssertTrue(selected.accessibilityTraits.contains(.selected))

        let unselected = row(vc, 0)
        XCTAssertEqual(unselected.accessibilityLabel, "A")
        XCTAssertFalse(unselected.accessibilityTraits.contains(.selected))
    }

    func test_選択面_副表示を持つ候補行は主表示と副表示と選択状態を公開する() throws {
        let vc = try makeScreenViaWiring(cell: itemCell(
            items: [
                PickerItem(text: "佐藤 花子", subText: "プロダクトマネージャー"),
                PickerItem(text: "全体アナウンス"),
            ],
            selectedIndex: 0
        ))
        vc.loadViewIfNeeded()

        let withSub = row(vc, 0)
        let label = try XCTUnwrap(withSub.accessibilityLabel)
        XCTAssertTrue(label.contains("佐藤 花子"), "主表示が公開される")
        XCTAssertTrue(label.contains("プロダクトマネージャー"), "副表示が公開される")
        XCTAssertTrue(withSub.accessibilityTraits.contains(.selected))

        XCTAssertEqual(row(vc, 1).accessibilityLabel, "全体アナウンス", "副表示なしの行は主表示のみ")
    }

    func test_選択面_複数選択のトグル後に公開状態が更新される() throws {
        let vc = try makeScreenViaWiring(cell: multiCell(selectedIndices: []))
        layout(vc)

        let target = try XCTUnwrap(vc.tableView.cellForRow(at: IndexPath(row: 1, section: 0)))
        XCTAssertFalse(target.accessibilityTraits.contains(.selected), "初期は未選択")

        vc._simulateSelect(1)
        XCTAssertTrue(target.accessibilityTraits.contains(.selected), "チェック後は選択済みとして公開される")

        vc._simulateSelect(1)
        XCTAssertFalse(target.accessibilityTraits.contains(.selected), "解除後は未選択へ戻る")
    }

    // MARK: - 選択中の項目への初期スクロール

    func test_初期スクロール_単一選択は選択中の項目が中央付近に来る() throws {
        let items = (0..<50).map { "item \($0)" }
        let vc = try makeScreenViaWiring(cell: singleCell(items: items, selectedIndex: 30))
        layout(vc)

        assertRowIsNearVisibleCenter(vc, row: 30)
    }

    func test_初期スクロール_複数選択は選択中の最小indexが中央付近に来る() throws {
        let items = (0..<50).map { "item \($0)" }
        let vc = try makeScreenViaWiring(cell: multiCell(items: items, selectedIndices: [40, 12, 25]))
        XCTAssertEqual(vc._initialScrollTargetRow, 12)

        layout(vc)
        assertRowIsNearVisibleCenter(vc, row: 12)
    }

    func test_初期スクロール_副表示混在でも選択中の項目が見える状態で開く() throws {
        // 副表示の有無を交互に並べ、行高が可変になる状態を作る。
        let items = (0..<50).map { index in
            index.isMultiple(of: 2)
                ? PickerItem(text: "item \(index)", subText: "説明 \(index)")
                : PickerItem(text: "item \(index)")
        }
        let vc = try makeScreenViaWiring(cell: itemCell(items: items, selectedIndex: 30))
        layout(vc)

        let table = vc.tableView!
        let rect = table.rectForRow(at: IndexPath(row: 30, section: 0))
        let inset = table.adjustedContentInset
        let visibleTop = table.contentOffset.y + inset.top
        let visibleBottom = table.contentOffset.y + table.bounds.height - inset.bottom
        XCTAssertGreaterThanOrEqual(rect.minY, visibleTop, "選択中の行が可視領域の上端より下にある")
        XCTAssertLessThanOrEqual(rect.maxY, visibleBottom, "選択中の行が可視領域の下端より上にある")
    }

    func test_初期スクロール_範囲外indexは対象外だが選択集合には残る() throws {
        var picked: Set<Int>?
        let cell = multiCell(
            items: ["A", "B", "C"],
            selectedIndices: [1, 5],
            onMultiSelectionChanged: { picked = $0 }
        )
        let vc = try makeScreenViaWiring(cell: cell)
        XCTAssertEqual(vc._initialScrollTargetRow, 1, "スクロール先は有効 index のみで決まる")

        layout(vc)
        vc._simulateDone()
        XCTAssertEqual(picked, Set([1, 5]), "選択集合は正規化されず範囲外 index が保持される")
    }

    func test_初期スクロール_未選択なら先頭から表示する() throws {
        let items = (0..<50).map { "item \($0)" }
        let vc = try makeScreenViaWiring(cell: singleCell(items: items, selectedIndex: nil))
        XCTAssertNil(vc._initialScrollTargetRow)

        layout(vc)
        XCTAssertEqual(vc.tableView.contentOffset.y, -vc.tableView.adjustedContentInset.top, accuracy: 0.5)
    }

    func test_初期スクロール_範囲外indexのみなら先頭から表示する() throws {
        let items = (0..<50).map { "item \($0)" }
        let single = try makeScreenViaWiring(cell: singleCell(items: items, selectedIndex: 99))
        XCTAssertNil(single._initialScrollTargetRow)

        let multi = try makeScreenViaWiring(cell: multiCell(items: items, selectedIndices: [80, 99]))
        XCTAssertNil(multi._initialScrollTargetRow)

        layout(multi)
        XCTAssertEqual(multi.tableView.contentOffset.y, -multi.tableView.adjustedContentInset.top, accuracy: 0.5)
    }

    func test_初期スクロール_items空でも選択面は提示される() throws {
        let vc = try makeScreenViaWiring(cell: singleCell(items: [], selectedIndex: 0))
        XCTAssertNil(vc._initialScrollTargetRow, "空 items ではスクロールしない")

        layout(vc)
        XCTAssertEqual(vc.tableView(vc.tableView, numberOfRowsInSection: 0), 0)
    }

    /// 指定行が可視領域の中央付近（許容誤差 = 1 行分）に位置することを検証する。
    private func assertRowIsNearVisibleCenter(
        _ vc: PickerListViewController,
        row: Int,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let table = vc.tableView!
        let rect = table.rectForRow(at: IndexPath(row: row, section: 0))
        let inset = table.adjustedContentInset
        let visibleTop = table.contentOffset.y + inset.top
        let visibleHeight = table.bounds.height - inset.top - inset.bottom
        let visibleCenter = visibleTop + visibleHeight / 2
        XCTAssertEqual(
            rect.midY,
            visibleCenter,
            accuracy: rect.height,
            "行 \(row) が可視領域の中央付近に来ていない",
            file: file,
            line: line
        )
    }

    // MARK: - キャンセル経路（既存の確定経路に対する退行確認の補完）

    func test_選択面_単一選択のキャンセルではcallbackが発火しない() throws {
        var picked: Int?
        let vc = try makeScreenViaWiring(
            cell: singleCell(onSelectionChanged: { picked = $0 })
        )
        vc.loadViewIfNeeded()

        vc._simulateCancel()
        XCTAssertNil(picked)
    }

    func test_選択面_複数選択のキャンセルではcallbackが発火しない() throws {
        var picked: Set<Int>?
        let vc = try makeScreenViaWiring(
            cell: multiCell(onMultiSelectionChanged: { picked = $0 })
        )
        layout(vc)

        vc._simulateSelect(2)
        vc._simulateCancel()
        XCTAssertNil(picked, "キャンセルでは編集中の選択が確定されない")
    }
}
#endif

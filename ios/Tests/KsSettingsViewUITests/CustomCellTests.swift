// CustomCellTests.swift
// KsSettingsViewUITests
//
// `CustomCell` / `CustomCellView` の等価性・描画・再利用・style 適用範囲・
// 行タップ・可視性・登録解決を検証する。
//
// 任意 content の観測方針:
//   SwiftUI の `Text` / `Image` は UIView として階層に現れないため、content 内に
//   `UIViewRepresentable` 製の probe（accessibilityIdentifier 付きの実 UIView）を埋め込み、
//   Cell の subview ツリーから identifier で探索して「描画されたか」「どの領域を占めたか」を
//   実測する。

#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

// MARK: - probe content

/// 任意 content の描画結果を UIView 階層から観測するための probe。
///
/// `accessibilityIdentifier` を持つ実 UIView を SwiftUI content 内に埋め込むことで、
/// 「builder の出力が実際に行へ描画されたか」「content が占める領域はどこか」を
/// 代理値ではなく UIView 階層の実測で確認できる。
private struct CustomCellProbe: UIViewRepresentable {
    let identifier: String

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        view.accessibilityIdentifier = identifier
        view.backgroundColor = .clear
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        uiView.accessibilityIdentifier = identifier
    }
}

// MARK: - builder 内部だけで高さが変わる content

/// builder が生成する View の内部状態だけを外から動かすためのモデル。
///
/// 利用者が最も自然に書く形（`@State` を持つ View を builder が返し、その View 内の操作で
/// 高さが変わる）と同じ経路 —— CustomCell の再バインドを伴わず、SwiftUI の再評価だけで
/// content のサイズが変わる経路 —— をテストから駆動するために使う。
/// `@Observable` は iOS 17 以降のため、iOS 16 を下限とする本パッケージでは
/// `ObservableObject` を使う。
private final class CustomCellHeightToggle: ObservableObject {
    @Published var isExpanded: Bool = false
}

/// `CustomCellHeightToggle` に追従して自分の高さだけを変える content View。
private struct CustomCellSelfResizingContent: View {
    @ObservedObject var toggle: CustomCellHeightToggle

    var body: some View {
        CustomCellProbe(identifier: "probe-self-resizing")
            .frame(height: toggle.isExpanded ? 240 : 60)
    }
}

@MainActor
final class CustomCellTests: XCTestCase {

    // MARK: - テストヘルパ

    /// Cell を window に載せてレイアウトを確定させる。
    /// `UIHostingConfiguration` の hosted View 階層は window 上のレイアウトで実体化する。
    ///
    /// content に `UIViewRepresentable` 製の probe を埋めた Cell では、probe の UIView が
    /// view ツリーへ現れるまでが非同期に進むため `renderedIdentifier` にその identifier を渡す。
    /// `Text` / `Image` だけの content は UIView を一切生やさない (待てる遷移が無い) ため、
    /// identifier を渡さずレイアウトの確定だけを行う。
    @discardableResult
    private func host(
        _ cell: UICollectionViewCell,
        size: CGSize = CGSize(width: 375, height: 60),
        renderedIdentifier: String? = nil
    ) -> UIWindow {
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: size.width, height: 600))
        cell.frame = CGRect(origin: .zero, size: size)
        window.addSubview(cell)
        window.makeKeyAndVisible()
        if let renderedIdentifier {
            awaitNonNil(
                "hosted content の probe (\(renderedIdentifier)) が view ツリーへ現れる",
                in: cell,
                produce: { findView(identifier: renderedIdentifier, in: cell) }
            )
        } else {
            layoutNow(cell)
        }
        return window
    }

    /// Cell 群を持つ `KsSettingsViewController` を window に載せて描画を確定させる。
    /// 再バインドの実経路（`applyDiff` → reconfigure → `render`）を検証するために使う。
    private func hostController(
        cells: [any KsCell],
        theme: Theme = Theme()
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [Section(cells: cells)]),
            theme: theme
        )
        let size = CGSize(width: 375, height: 600)
        let root = controller.view!
        root.frame = CGRect(origin: .zero, size: size)
        let window = UIWindow(frame: root.frame)
        window.addSubview(root)
        window.makeKeyAndVisible()
        root.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        awaitInitialRender(controller)
        return (controller, cv, window)
    }

    /// subview ツリーから `accessibilityIdentifier` 一致の UIView を探す。
    private func findView(identifier: String, in root: UIView) -> UIView? {
        if root.accessibilityIdentifier == identifier { return root }
        for sub in root.subviews {
            if let found = findView(identifier: identifier, in: sub) { return found }
        }
        return nil
    }

    /// Cell をレンダリングした画像を返す（見た目の一致／不一致を実測するため）。
    ///
    /// `drawHierarchy(afterScreenUpdates:)` はテストプロセスでは hosted content を取り込めないため、
    /// `CALayer.render(in:)` で実際のレイヤ内容を描き出す。
    private func snapshot(_ cell: UICollectionViewCell) -> Data? {
        let renderer = UIGraphicsImageRenderer(bounds: cell.bounds)
        let image = renderer.image { context in
            cell.layer.render(in: context.cgContext)
        }
        return image.pngData()
    }

    // MARK: - 定義と等価性

    /// builder だけが異なるインスタンスは等価と判定される。
    func test_builderだけが異なるインスタンスは等価() {
        let id = UUID()
        let a = CustomCell(id: id, content: "同じ値") { _ in Text("A") }
        let b = CustomCell(id: id, content: "同じ値") { _ in Color.red }

        XCTAssertEqual(a, b, "builder は等価判定に参加しない（差分検出は再バインドを発生させない）")
        XCTAssertEqual(a.hashValue, b.hashValue, "hash も builder に依存しない")
    }

    /// onTap も builder と同じく等価判定から除外される。
    func test_onTapだけが異なるインスタンスは等価() {
        let id = UUID()
        let a = CustomCell(id: id, content: 1, onTap: {}) { _ in Text("A") }
        let b = CustomCell(id: id, content: 1, onTap: {}) { _ in Text("A") }
        let c = CustomCell(id: id, content: 1, onTap: nil) { _ in Text("A") }

        XCTAssertEqual(a, b, "onTap の関数値は等価判定に参加しない")
        XCTAssertEqual(a, c, "onTap の有無自体も等価判定に参加しない（関数値は全除外）")
    }

    /// content が異なれば非等価と判定される。
    func test_contentが異なれば非等価() {
        let id = UUID()
        let a = CustomCell(id: id, content: "A") { v in Text(v) }
        let b = CustomCell(id: id, content: "B") { v in Text(v) }

        XCTAssertNotEqual(a, b, "content の変化は再バインドを発生させる")
    }

    /// content の値が同じでも実体型が異なれば非等価と判定される。
    ///
    /// content の実体型が変われば builder の引数型も変わるため、値が `AnyHashable` 比較で
    /// 等価に見えても非等価にする必要がある。`AnyHashable` は Foundation ブリッジ経由で
    /// 異なる数値型を等価と判定する（`AnyHashable(Int(1)) == AnyHashable(Double(1.0))` は真）
    /// ので、値だけを比べていると型変更が差分検出をすり抜けて古い builder の出力が残る。
    func test_content値が同じでも実体型が異なれば非等価() {
        let id = UUID()
        let intCell = CustomCell(id: id, content: Int(1)) { v in Text("Int: \(v)") }
        let doubleCell = CustomCell(id: id, content: Double(1.0)) { v in Text("Double: \(v)") }

        // 前提の確認: 値そのものは `AnyHashable` 比較では等価になってしまう。
        XCTAssertEqual(
            intCell.content, doubleCell.content,
            "AnyHashable の値比較だけでは Int(1) と Double(1.0) を区別できない（前提）"
        )

        XCTAssertNotEqual(intCell, doubleCell, "content の実体型が変われば再バインドが必要")
        XCTAssertNotEqual(
            AnyHashable(intCell), AnyHashable(doubleCell),
            "DSL 差分検出が使う AnyHashable 比較でも非等価になる"
        )

        // 同一実体型・同一値なら従来どおり等価。
        let sameInt = CustomCell(id: id, content: Int(1)) { v in Text("別の builder: \(v)") }
        XCTAssertEqual(intCell, sameInt)
        XCTAssertEqual(AnyHashable(intCell), AnyHashable(sameInt))
    }

    /// 実体型トークンは `withDSLID` / `withStyle` の copy でも保たれる。
    func test_content実体型の区別はwithDSLIDとwithStyleのcopyでも保たれる() {
        let id = UUID()
        let intCell = CustomCell(content: Int(1)) { v in Text("\(v)") }
        let doubleCell = CustomCell(content: Double(1.0)) { v in Text("\(v)") }

        XCTAssertNotEqual(
            intCell.withDSLID(id), doubleCell.withDSLID(id),
            "withDSLID の copy でも実体型の違いが保たれる"
        )

        let style = CellStyle(cellHeight: 80)
        XCTAssertNotEqual(
            intCell.withDSLID(id).withStyle(style), doubleCell.withDSLID(id).withStyle(style),
            "withStyle の copy でも実体型の違いが保たれる"
        )
    }

    /// 表示に効くスカラー（showArrow）の変更は非等価と判定される。
    func test_showArrowが異なれば非等価() {
        let id = UUID()
        let a = CustomCell(id: id, content: "x", showArrow: false) { v in Text(v) }
        let b = CustomCell(id: id, content: "x", showArrow: true) { v in Text(v) }

        XCTAssertNotEqual(a, b, "showArrow は表示に効くため等価判定に参加する")
    }

    /// 表示に効くスカラー（isEnabled / isVisible / style）も等価判定に参加する。
    func test_isEnabledとisVisibleとstyleも等価判定に参加する() {
        let id = UUID()
        let base = CustomCell(id: id, content: "x") { v in Text(v) }

        XCTAssertNotEqual(
            base,
            CustomCell(id: id, content: "x", isEnabled: false) { v in Text(v) }
        )
        XCTAssertNotEqual(
            base,
            CustomCell(id: id, content: "x", isVisible: false) { v in Text(v) }
        )
        XCTAssertNotEqual(
            base,
            CustomCell(id: id, style: CellStyle(cellHeight: 120), content: "x") { v in Text(v) }
        )
    }

    /// 同値 content の再構成では `AnyHashable` 比較（DSL 差分検出が使う経路）が等価になり、
    /// 再バインドが要求されない。
    func test_同値contentの再構成はAnyHashable比較でも等価() {
        let id = UUID()
        let a = CustomCell(id: id, content: ["k": 1]) { _ in Text("x") }
        let b = CustomCell(id: id, content: ["k": 1]) { _ in Text("y") }

        // `DSLDiffCalculator.cellLevelDiffs` は `AnyHashable(oldCell) != AnyHashable(newCell)` で
        // 内容変化を判定する。ここが等価であれば replaceCell は発行されない。
        XCTAssertEqual(AnyHashable(a), AnyHashable(b))

        // mutation probe: content を変えれば同じ比較経路で非等価になる（比較に検出力があることの確認）
        let c = CustomCell(id: id, content: ["k": 2]) { _ in Text("y") }
        XCTAssertNotEqual(AnyHashable(a), AnyHashable(c))
    }

    // MARK: - 静的コンテンツの省略形

    /// content なしで生成でき、等価性は content 以外の参加要素で決まる。
    func test_contentなしで生成でき等価性はcontent以外の参加要素で決まる() {
        let id = UUID()
        let a = CustomCell(id: id) { Text("静的") }
        let b = CustomCell(id: id) { Color.blue }
        XCTAssertEqual(a, b, "content 省略形の等価性は id / style / スカラーで決まる")

        let c = CustomCell(id: id, showArrow: true) { Text("静的") }
        XCTAssertNotEqual(a, c, "省略形でも showArrow は等価判定に参加する")

        let d = CustomCell(id: UUID()) { Text("静的") }
        XCTAssertNotEqual(a, d, "id が異なれば非等価")
    }

    /// content 省略形でも builder の出力が行として描画される。
    func test_contentなし省略形のbuilder出力が行に描画される() {
        let view = CustomCellView()
        let cell = CustomCell { CustomCellProbe(identifier: "probe-static") }
        view.render(cell: cell, theme: Theme())
        host(view, renderedIdentifier: "probe-static")

        XCTAssertNotNil(
            findView(identifier: "probe-static", in: view),
            "content 省略形でも builder の出力が行として描画される"
        )
    }

    // MARK: - 事前登録なしの描画

    /// Registry を一切操作しなくても builder の出力で描画される。
    func test_Registry未操作でもCustomCellがbuilderの出力で描画される() {
        // 利用者が Registry へ一切登録せずに Host を構成する通常経路。
        let cell = CustomCell(content: "x") { _ in CustomCellProbe(identifier: "probe-auto") }
        let (controller, cv, _) = hostController(cells: [cell])

        XCTAssertTrue(
            controller.registry.resolveRendererType(for: cell) == CustomCellView.self,
            "CustomCell は標準登録集合として自動登録される（利用者の明示登録は不要）"
        )
        let rendered = cv.cellForItem(at: IndexPath(item: 0, section: 0))
        XCTAssertTrue(rendered is CustomCellView, "placeholder ではなく CustomCellView が使われる")
        XCTAssertNotNil(
            findView(identifier: "probe-auto", in: cv),
            "Registry 未操作でも builder の出力で描画される"
        )
    }

    /// 自動登録を抑止した独立 Registry では `registerCustomCell()` で明示登録できる。
    func test_registerCustomCellで独立Registryに登録できる() {
        let registry = KsCellRegistry()
        let cell = CustomCell(content: "x") { v in Text(v) }
        XCTAssertNil(registry.resolveRendererType(for: cell), "登録前は解決できない")

        registry.registerCustomCell()
        XCTAssertTrue(registry.resolveRendererType(for: cell) == CustomCellView.self)
    }

    /// 実体 content 型が異なっても単一登録で解決される（型消去内蔵の要件）。
    func test_content型が異なっても単一登録で解決される() {
        let registry = KsCellRegistry()
        registry.registerCustomCell()

        let intCell = CustomCell(content: 42) { v in Text("\(v)") }
        let stringCell = CustomCell(content: "s") { v in Text(v) }
        let emptyCell = CustomCell { Text("static") }

        XCTAssertTrue(registry.resolveRendererType(for: intCell) == CustomCellView.self)
        XCTAssertTrue(registry.resolveRendererType(for: stringCell) == CustomCellView.self)
        XCTAssertTrue(registry.resolveRendererType(for: emptyCell) == CustomCellView.self)
    }

    // MARK: - content 駆動の描画と再利用

    /// bind 時に builder(content) の出力が行に描画される。
    func test_bindでbuilder出力が行に描画される() {
        let view = CustomCellView()
        let cell = CustomCell(content: "A") { value in
            CustomCellProbe(identifier: "probe-\(value)")
        }
        view.render(cell: cell, theme: Theme())
        host(view, renderedIdentifier: "probe-A")

        XCTAssertNotNil(findView(identifier: "probe-A", in: view))
    }

    /// content を更新すると表示が builder の新しい出力に変わる。
    ///
    /// 実経路（同一 id で content を差し替えた root を適用 →`.replaceCell` → reconfigure →
    /// 同一 Cell への再 render）で検証する。
    func test_contentの更新で表示がbuilderの新出力に変わる() {
        let id = UUID()
        let a = CustomCell(id: id, content: "A") { value in
            CustomCellProbe(identifier: "probe-\(value)")
        }
        let (controller, cv, _) = hostController(cells: [a])

        XCTAssertNotNil(findView(identifier: "probe-A", in: cv), "初期 content A が描画される")

        let b = CustomCell(id: id, content: "B") { value in
            CustomCellProbe(identifier: "probe-\(value)")
        }
        controller.applyDiff(.replaceCell(cellID: KsCellID(cell: a), new: b))
        awaitNonNil(
            "差し替えた content の probe が実描画へ現れる",
            in: cv,
            produce: { findView(identifier: "probe-B", in: cv) }
        )

        XCTAssertNotNil(findView(identifier: "probe-B", in: cv), "content B の出力に更新される")
        XCTAssertNil(findView(identifier: "probe-A", in: cv), "content A の出力は残らない")
    }

    /// 再利用時に前の content 表示と onTap 参照が残らない。
    func test_prepareForReuseで前のcontentとlistenerが残らない() {
        let view = CustomCellView()
        var tapped = 0
        let cell = CustomCell(content: "A", onTap: { tapped += 1 }) { value in
            CustomCellProbe(identifier: "probe-\(value)")
        }
        view.render(cell: cell, theme: Theme())
        host(view, renderedIdentifier: "probe-A")
        XCTAssertNotNil(findView(identifier: "probe-A", in: view))
        XCTAssertNotNil(view.tapHandler)

        view.prepareForReuse()
        view.setNeedsLayout()
        view.layoutIfNeeded()

        XCTAssertNil(view.contentConfiguration, "hosted content は解放される")
        XCTAssertNil(view.tapHandler, "前の onTap 参照は残らない")
        XCTAssertNil(findView(identifier: "probe-A", in: view), "前の content 表示は残らない")
        XCTAssertEqual(tapped, 0)
    }

    // MARK: - 行タップ

    /// onTap 指定時、行タップで 1 回だけ発火する。
    /// 行タップの実経路（`UICollectionViewDelegate.didSelectItemAt` → `tapHandler`）で検証する。
    func test_onTap指定時に行タップで1回発火する() {
        var count = 0
        let cell = CustomCell(content: "x", onTap: { count += 1 }) { v in Text(v) }
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [Section(cells: [cell])])
        )
        let view = controller.view!
        view.frame = CGRect(x: 0, y: 0, width: 375, height: 600)
        view.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = view.frame
        cv.setNeedsLayout()
        cv.layoutIfNeeded()

        controller.collectionView(cv, didSelectItemAt: IndexPath(item: 0, section: 0))
        XCTAssertEqual(count, 1, "行タップで onTap が 1 回だけ呼ばれる")
    }

    /// 既定（onTap 未指定）では行タップ動作を持たない。
    func test_onTap未指定なら行タップハンドラを持たない() {
        let view = CustomCellView()
        let cell = CustomCell(content: "x") { v in Text(v) }
        view.render(cell: cell, theme: Theme())

        XCTAssertNil(view.tapHandler, "既定（onTap = nil）では行レベルのタップ処理を持たない")
        XCTAssertTrue(
            view.isUserInteractionEnabled,
            "content 内部の操作（ボタン・スライダー等）は妨げられない"
        )
    }

    /// 子要素の操作では行タップが発火しない。
    ///
    /// 行タップは `UICollectionView` の選択経路（`didSelectItemAt`）でのみ配送される。
    /// content 内の操作可能要素がジェスチャを消費した場合、UIKit は選択を発生させないため
    /// 二重発火は起きない。ここではライブラリ側が担保すべき構造 —
    /// 「Cell 自身が行タップ用の gesture recognizer / target-action を追加しない」— を検証する。
    /// （実ジェスチャ競合の体感確認は Sample デモでの受け入れ検証に委ねる）
    func test_行タップ用のgestureRecognizerをCell自身に追加しない() {
        let view = CustomCellView()
        let cell = CustomCell(content: "x", onTap: {}) { v in
            Button("child") {}.accessibilityIdentifier("child-\(v)")
        }
        view.render(cell: cell, theme: Theme())
        host(view)

        // Cell 自身・contentView には行タップ用の recognizer を追加していない
        // （追加していると content の操作より先に行タップが発火し得る）。
        let ownRecognizers = (view.gestureRecognizers ?? [])
        XCTAssertTrue(
            ownRecognizers.isEmpty,
            "CustomCellView 自身に gesture recognizer を追加していない（実測: \(ownRecognizers)）"
        )
        XCTAssertNotNil(view.tapHandler, "行タップは選択経路（tapHandler）でのみ配送される")
    }

    /// isEnabled が false なら行タップは発火しない。
    func test_isEnabledがfalseなら行タップは発火しない() {
        var count = 0
        let cell = CustomCell(content: "x", onTap: { count += 1 }, isEnabled: false) { v in Text(v) }
        let view = CustomCellView()
        view.render(cell: cell, theme: Theme())

        XCTAssertNil(view.tapHandler, "isEnabled = false のとき tapHandler は保持されない")

        // 実経路（didSelectItemAt）でも発火しないことを確認する。
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [Section(cells: [cell])])
        )
        let root = controller.view!
        root.frame = CGRect(x: 0, y: 0, width: 375, height: 600)
        root.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = root.frame
        cv.setNeedsLayout()
        cv.layoutIfNeeded()
        controller.collectionView(cv, didSelectItemAt: IndexPath(item: 0, section: 0))

        XCTAssertEqual(count, 0)
    }

    /// isEnabled が false なら content 内の操作も抑止される。
    ///
    /// `isEnabled == false` では行全体の `isUserInteractionEnabled` を落とすため、
    /// content 内の任意要素へのヒットテストが成立しない（= タッチが届かない）。
    func test_isEnabledがfalseならcontent内の操作も抑止される() {
        let view = CustomCellView()
        let cell = CustomCell(content: "x", isEnabled: false) { v in
            Button("押せない") {}.accessibilityIdentifier("button-\(v)")
        }
        view.render(cell: cell, theme: Theme())
        host(view)

        XCTAssertFalse(
            view.isUserInteractionEnabled,
            "無効 Cell は内包 control の操作を抑止する"
        )
        let center = CGPoint(x: view.bounds.midX, y: view.bounds.midY)
        XCTAssertNil(
            view.hitTest(center, with: nil),
            "無効 Cell の内部座標へのヒットテストは成立しない（content 内のボタンにも届かない）"
        )

        // mutation probe: 有効に戻せばヒットテストは成立する（アサーションに検出力があることの確認）
        let enabled = CustomCell(content: "x") { v in
            Button("押せる") {}.accessibilityIdentifier("button-\(v)")
        }
        view.render(cell: enabled, theme: Theme())
        host(view)
        XCTAssertTrue(view.isUserInteractionEnabled)
        XCTAssertNotNil(view.hitTest(center, with: nil))
    }

    // MARK: - Disclosure Indicator の表示

    /// 既定では indicator は表示されず、content が行全域を占有する。
    func test_showArrow既定ではcontentが行全域を占有する() {
        let view = CustomCellView()
        let cell = CustomCell(content: "x") { _ in CustomCellProbe(identifier: "probe") }
        view.render(cell: cell, theme: Theme())
        host(view, renderedIdentifier: "probe")

        guard let probe = findView(identifier: "probe", in: view) else {
            XCTFail("probe content が描画されていない")
            return
        }
        let rect = probe.convert(probe.bounds, to: view)
        XCTAssertEqual(
            rect.width, view.bounds.width, accuracy: 0.5,
            "showArrow 既定（false）では content が行全域（full-bleed）を占める"
        )
    }

    /// showArrow で indicator が表示される
    /// （content の占有領域が indicator の領域を除いた範囲になることを実測で確認する）。
    func test_showArrowでcontentの占有領域がindicator分だけ狭くなる() {
        let plain = CustomCellView()
        plain.render(
            cell: CustomCell(content: "x") { _ in CustomCellProbe(identifier: "probe") },
            theme: Theme()
        )
        host(plain, renderedIdentifier: "probe")

        let arrowed = CustomCellView()
        arrowed.render(
            cell: CustomCell(content: "x", showArrow: true) { _ in CustomCellProbe(identifier: "probe") },
            theme: Theme()
        )
        host(arrowed, renderedIdentifier: "probe")

        guard
            let plainProbe = findView(identifier: "probe", in: plain),
            let arrowedProbe = findView(identifier: "probe", in: arrowed)
        else {
            XCTFail("probe content が描画されていない")
            return
        }
        let plainWidth = plainProbe.convert(plainProbe.bounds, to: plain).width
        let arrowedWidth = arrowedProbe.convert(arrowedProbe.bounds, to: arrowed).width

        XCTAssertLessThan(
            arrowedWidth, plainWidth,
            "showArrow = true のとき content 領域は indicator の領域だけ狭くなる"
        )
        // indicator は chevron 幅 + 末端余白（既存 Cell と共有する 16pt）を占める。
        XCTAssertGreaterThan(
            plainWidth - arrowedWidth, KsChevronAppearance.trailingMargin,
            "占有差は少なくとも共有末端余白（\(KsChevronAppearance.trailingMargin)pt）+ chevron 幅ぶんある"
        )
    }

    /// `showArrow` は `onTap` と独立に指定できる。
    func test_showArrowはonTapと独立に指定できる() {
        let view = CustomCellView()
        let cell = CustomCell(content: "x", showArrow: true) { v in Text(v) }
        view.render(cell: cell, theme: Theme())
        host(view)

        XCTAssertNil(view.tapHandler, "showArrow = true でも onTap 未指定なら行タップ動作は持たない")
    }

    // MARK: - スタイルの適用範囲

    /// 行レベル項目（背景色）は適用される。
    func test_style_backgroundColorが行に適用される() {
        let view = CustomCellView()
        let bg = UIColor(red: 0.1, green: 0.2, blue: 0.3, alpha: 1)
        let cell = CustomCell(style: CellStyle(backgroundColor: bg), content: "x") { v in Text(v) }
        view.render(cell: cell, theme: Theme())

        XCTAssertEqual(view.backgroundConfiguration?.backgroundColor, bg)
    }

    /// テキスト系スタイルは content の見た目に影響しない。
    ///
    /// テキスト系項目だけを変えた 2 つの CustomCell を実際にレンダリングし、
    /// 描画結果（PNG）が一致することで「builder の出力の見た目は変化しない」を実測する。
    func test_テキスト系styleはcontentの見た目に影響しない() {
        let textStyle = CellStyle(
            titleColor: .red,
            titleFont: .boldSystemFont(ofSize: 30),
            descriptionColor: .green,
            valueTextColor: .blue,
            hintTextColor: .purple
        )

        let plain = CustomCellView()
        plain.render(
            cell: CustomCell(content: "見た目") { v in Text(v).font(.body) },
            theme: Theme()
        )
        host(plain)

        let styled = CustomCellView()
        styled.render(
            cell: CustomCell(style: textStyle, content: "見た目") { v in Text(v).font(.body) },
            theme: Theme()
        )
        host(styled)

        guard let plainImage = snapshot(plain), let styledImage = snapshot(styled) else {
            XCTFail("Cell のレンダリング結果を取得できない")
            return
        }
        XCTAssertEqual(
            plainImage, styledImage,
            "テキスト系 style は builder の出力に適用先を持たないため見た目は変化しない"
        )

        // mutation probe: builder 側で見た目を変えれば描画結果は変わる
        // （この画像比較が content の見た目変化を検出できることの確認）。
        let mutated = CustomCellView()
        mutated.render(
            cell: CustomCell(content: "見た目") { v in Text(v).font(.body).foregroundStyle(Color.red) },
            theme: Theme()
        )
        host(mutated)
        guard let mutatedImage = snapshot(mutated) else {
            XCTFail("Cell のレンダリング結果を取得できない")
            return
        }
        XCTAssertNotEqual(
            mutatedImage, plainImage,
            "builder 側で見た目を変えれば描画結果は変わる（画像比較に検出力がある）"
        )
    }

    /// hasUnevenRows が true なら cellHeight は最低高として働く。
    func test_hasUnevenRowsがtrueならcellHeightは最低高として働く() {
        let theme = Theme(hasUnevenRows: true)
        let cell = CustomCell(style: CellStyle(cellHeight: 60), content: "x") { _ in
            // 自然高が 60pt を超える content
            Color.gray.frame(height: 200)
        }
        guard let measured = measuredHeight(for: cell, theme: theme) else {
            XCTFail("セル frame.height を取得できない")
            return
        }
        XCTAssertGreaterThan(
            measured, 60 + 0.5,
            "hasUnevenRows = true では cellHeight は最低高として働き、content に応じて伸びる（実測 \(measured)pt）"
        )
    }

    /// hasUnevenRows が false なら cellHeight で行高さを固定できる。
    func test_hasUnevenRowsがfalseならcellHeightで固定される() {
        let theme = Theme(hasUnevenRows: false)
        let cell = CustomCell(style: CellStyle(cellHeight: 90), content: "x") { _ in
            Color.gray.frame(height: 200)
        }
        guard let measured = measuredHeight(for: cell, theme: theme) else {
            XCTFail("セル frame.height を取得できない")
            return
        }
        XCTAssertEqual(
            measured, 90, accuracy: 0.5,
            "hasUnevenRows = false では cellHeight に固定される（実測 \(measured)pt）"
        )
    }

    /// hasUnevenRows が true なら cellHeight 未指定でも content の自然サイズに行高さが追従する。
    func test_hasUnevenRowsがtrueならcellHeight未指定でもcontentの自然高に追従する() {
        let theme = Theme(hasUnevenRows: true)
        let short = CustomCell(content: "x") { _ in Color.gray.frame(height: 60) }
        let tall = CustomCell(content: "x") { _ in Color.gray.frame(height: 220) }

        guard
            let shortHeight = measuredHeight(for: short, theme: theme),
            let tallHeight = measuredHeight(for: tall, theme: theme)
        else {
            XCTFail("セル frame.height を取得できない")
            return
        }
        XCTAssertGreaterThan(
            tallHeight, shortHeight + 100,
            "content の自然サイズに行高さが追従する（実測 short=\(shortHeight)pt / tall=\(tallHeight)pt）"
        )
    }

    /// content は同値のまま builder 内部の状態だけが変わる経路でも行高さが追従する。
    ///
    /// content を差し替えて再バインドする経路とは別に、builder が返す View が自分の状態で
    /// サイズを変える形（利用者が `@State` で書く最も自然な形）でも行高さが追従する。
    /// この経路では CustomCell の等価性は変わらず、DSL の差分検出も再バインドを発行しない。
    func test_content同値のままbuilder内部の状態変化で行高さが追従する() {
        let theme = Theme(hasUnevenRows: true)
        let toggle = CustomCellHeightToggle()
        let cell = CustomCell(content: "不変") { _ in
            CustomCellSelfResizingContent(toggle: toggle)
        }
        let (_, cv, _) = hostController(cells: [cell], theme: theme)
        let path = IndexPath(item: 0, section: 0)

        guard let collapsed = cv.cellForItem(at: path)?.frame.height else {
            XCTFail("セル frame.height を取得できない")
            return
        }

        // 再バインド API を一切呼ばず、builder 内部の状態だけを動かす。
        toggle.isExpanded = true
        awaitCondition(
            "builder 内部の状態変化で行高さが伸びる",
            in: cv,
            actual: { "行高さ = \(String(describing: cv.cellForItem(at: path)?.frame.height))" },
            until: { (cv.cellForItem(at: path)?.frame.height ?? 0) > collapsed + 100 }
        )

        guard let expanded = cv.cellForItem(at: path)?.frame.height else {
            XCTFail("セル frame.height を取得できない")
            return
        }
        XCTAssertGreaterThan(
            expanded, collapsed + 100,
            "再計測 API を呼ばずに行高さが content の新しいサイズへ追従する"
            + "（実測 collapsed=\(collapsed)pt / expanded=\(expanded)pt）"
        )

        // 折りたたみ方向にも追従する。
        toggle.isExpanded = false
        awaitCondition(
            "builder 内部の状態変化で行高さが縮む",
            in: cv,
            actual: { "行高さ = \(String(describing: cv.cellForItem(at: path)?.frame.height))" },
            until: { (cv.cellForItem(at: path)?.frame.height ?? 0) < expanded - 100 }
        )
        guard let recollapsed = cv.cellForItem(at: path)?.frame.height else {
            XCTFail("セル frame.height を取得できない")
            return
        }
        XCTAssertLessThan(
            recollapsed, expanded - 100,
            "縮む方向にも追従する（実測 \(recollapsed)pt）"
        )
    }

    // MARK: - content の縦位置（高さ遷移中の見え方）

    /// content が行の高さに収まらない間、content の上端は行の上端に固定される。
    ///
    /// 行の self-sizing は content のサイズ変化より 1 レイアウトパス遅れるため、その間だけ
    /// 「content の自然高 > 行の高さ」という状態になる。ここで content を縦中央に置くと
    /// content が上下へ均等にはみ出し、行が伸びるにつれて中心が下がるため、展開操作が
    /// 「一度上へ飛び出してから落ちてくる」動きに見える。上端を固定することでこれを避ける。
    func test_contentが行高さを超える間はcontentの上端が行の上端に固定される() {
        let cell = CustomCell(style: CellStyle(cellHeight: 60), content: "x") { _ in
            CustomCellProbe(identifier: "probe").frame(height: 200)
        }
        guard let placement = probePlacement(
            identifier: "probe", for: cell, theme: Theme(hasUnevenRows: false)
        ) else {
            XCTFail("probe content が描画されていない")
            return
        }
        XCTAssertEqual(
            placement.row.height, 60, accuracy: 0.5,
            "前提: 行は cellHeight に固定され、content（200pt）はそこに収まらない"
        )
        XCTAssertEqual(
            placement.probe.minY, 0, accuracy: 1.0,
            "content が行に収まらないとき、はみ出しは下方向だけになる"
            + "（実測 minY=\(placement.probe.minY)）"
        )
    }

    /// content が行の高さに収まるときは、標準 Cell と同じく縦中央に配置される。
    ///
    /// 上端固定は「収まらないとき」だけの振る舞いであり、cellHeight で行を高くした場合に
    /// content が上寄りになって標準 Cell と不揃いになってはいけない。
    func test_contentが行高さに収まるときは縦中央に配置される() {
        let cell = CustomCell(style: CellStyle(cellHeight: 200), content: "x") { _ in
            CustomCellProbe(identifier: "probe").frame(height: 60)
        }
        guard let placement = probePlacement(
            identifier: "probe", for: cell, theme: Theme(hasUnevenRows: false)
        ) else {
            XCTFail("probe content が描画されていない")
            return
        }
        XCTAssertEqual(
            placement.row.height, 200, accuracy: 0.5,
            "前提: 行は cellHeight に固定され、content（60pt）には余白が残る"
        )
        XCTAssertEqual(
            placement.probe.midY, placement.row.midY, accuracy: 1.0,
            "行に余白があるときは縦中央（実測 midY=\(placement.probe.midY)"
            + " / 行の中心=\(placement.row.midY)）"
        )
    }

    /// self-sizing 行で実効行高さ（下限）が content より大きいときは、実効行高さの中で縦中央になる。
    ///
    /// `cellHeight` を下限として行が content より高く定常するケース。縦中央の基準を
    /// 実効行高さに取っても、この定常状態の見え方は行の高さ基準と一致しなければならない。
    func test_selfSizing行が実効行高さで定常するときは縦中央に配置される() {
        let cell = CustomCell(style: CellStyle(cellHeight: 120), content: "x") { _ in
            CustomCellProbe(identifier: "probe").frame(height: 40)
        }
        guard let placement = probePlacement(
            identifier: "probe", for: cell, theme: Theme(hasUnevenRows: true)
        ) else {
            XCTFail("probe content が描画されていない")
            return
        }
        XCTAssertEqual(
            placement.row.height, 120, accuracy: 0.5,
            "前提: 行は cellHeight を下限として定常し、content（40pt）には余白が残る"
        )
        XCTAssertEqual(
            placement.probe.midY, placement.row.midY, accuracy: 1.0,
            "実効行高さで定常する行では縦中央（実測 midY=\(placement.probe.midY)"
            + " / 行の中心=\(placement.row.midY)）"
        )
    }

    /// 行が実効行高さより高い間、content は定常高さ基準の縦位置を維持する。
    ///
    /// 折りたたみ操作の直後、行の縮小が self-sizing で追いつくまでの間だけ
    /// 「行の高さ > 定常状態の行の高さ」という状態になる。ここで現在の行の高さを基準に
    /// 縦中央へ置くと、行が縮むにつれ content が中央位置から上端へすり上がる動きに見える。
    /// 定常高さ（`max(自然高, 実効行高さ)`）を基準にすることで、content の縦位置は
    /// 行の高さの変化から独立する。
    func test_行が実効行高さより高い間は定常高さ基準の縦位置を維持する() {
        let hosted = CustomCellHostedContent(
            content: AnyView(CustomCellProbe(identifier: "probe").frame(height: 60)),
            showArrow: false,
            isEnabled: true,
            effectiveCellHeight: 44
        )
        let host = UIHostingController(rootView: hosted)
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 375, height: 600))
        // 折りたたみ直後の「まだ高いままの行」を模して、定常高さ（60pt）より高い 200pt を与える。
        // window 上端から離して置き、safe area が hosted content の配置へ混入しないようにする。
        host.view.frame = CGRect(x: 0, y: 150, width: 375, height: 200)
        window.addSubview(host.view)
        window.makeKeyAndVisible()
        awaitNonNil(
            "hosted content の probe が view ツリーへ現れる",
            in: host.view,
            produce: { findView(identifier: "probe", in: host.view) }
        )

        guard let probe = findView(identifier: "probe", in: host.view) else {
            XCTFail("probe content が描画されていない")
            return
        }
        let rect = probe.convert(probe.bounds, to: host.view)
        XCTAssertEqual(
            rect.minY, 0, accuracy: 1.0,
            "行が定常高さより高い間も content の上端は定常位置に留まる"
            + "（実測 minY=\(rect.minY)）"
        )
    }

    /// 行が実効行高さより高い間も、実効行高さの中での縦中央位置は維持される。
    ///
    /// 定常高さが実効行高さ側で決まる（content の自然高 < 実効行高さ）ケース。行の高さが
    /// 一時的に定常高さを超えても、縦位置の基準は現在の行の高さではなく実効行高さに
    /// 留まらなければならない。
    func test_行が実効行高さより高い間も実効行高さの中での縦中央は維持される() {
        let hosted = CustomCellHostedContent(
            content: AnyView(CustomCellProbe(identifier: "probe").frame(height: 40)),
            showArrow: false,
            isEnabled: true,
            effectiveCellHeight: 120
        )
        let host = UIHostingController(rootView: hosted)
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 375, height: 600))
        // 定常高さ（実効行高さ 120pt）より高い 200pt を与え、高さ遷移中の行を模す。
        // window 上端から離して置き、safe area が hosted content の配置へ混入しないようにする。
        host.view.frame = CGRect(x: 0, y: 150, width: 375, height: 200)
        window.addSubview(host.view)
        window.makeKeyAndVisible()
        awaitNonNil(
            "hosted content の probe が view ツリーへ現れる",
            in: host.view,
            produce: { findView(identifier: "probe", in: host.view) }
        )

        guard let probe = findView(identifier: "probe", in: host.view) else {
            XCTFail("probe content が描画されていない")
            return
        }
        let rect = probe.convert(probe.bounds, to: host.view)
        XCTAssertEqual(
            rect.minY, 40, accuracy: 1.0,
            "縦中央の基準は実効行高さ 120pt に留まる（offset = (120 - 40) / 2 = 40。"
            + "実測 minY=\(rect.minY)）"
        )
    }

    /// 指定 Cell 1 件だけの root を構築し、行の中での probe の位置（行座標系）を返す。
    ///
    /// Cell を直接 window に載せると window の safe area が hosted content の配置に効いてしまい、
    /// 実際の行内配置と一致しない。ここは実経路（`KsSettingsViewController` の collection view）で測る。
    private func probePlacement(
        identifier: String,
        for cell: any KsCell,
        theme: Theme
    ) -> (probe: CGRect, row: CGRect)? {
        let (_, cv, _) = hostController(cells: [cell], theme: theme)
        guard
            let row = cv.cellForItem(at: IndexPath(item: 0, section: 0)),
            let probe = findView(identifier: identifier, in: row)
        else {
            return nil
        }
        return (probe.convert(probe.bounds, to: row), row.bounds)
    }

    /// 指定 Cell 1 件だけの root を構築し、描画された行の frame.height を返す。
    private func measuredHeight(for cell: any KsCell, theme: Theme) -> CGFloat? {
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [Section(cells: [cell])]),
            theme: theme
        )
        let view = controller.view!
        let size = CGSize(width: 375, height: 1000)
        view.frame = CGRect(origin: .zero, size: size)
        view.layoutIfNeeded()

        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        cv.layoutIfNeeded()
        return awaitNonNil(
            "計測対象の行が生成される",
            in: cv,
            produce: { cv.cellForItem(at: IndexPath(item: 0, section: 0))?.frame.height }
        )
    }

    // MARK: - 可視性フィルタへの参加

    /// isVisible が false の行は snapshot から除外され、並びが詰まる。
    func test_isVisibleがfalseなら行がsnapshotから除外され並びが詰まる() {
        let visible = CustomCell(content: "A") { v in Text(v) }
        let hidden = CustomCell(content: "B", isVisible: false) { v in Text(v) }
        let after = LabelCell(title: "後続")
        let section = Section(cells: [visible, hidden, after])

        let controller = KsSettingsViewController(root: SettingsRoot(sections: [section]))
        _ = controller.view

        let snapshot = controller.internalDataSource?.snapshot()
        XCTAssertEqual(snapshot?.numberOfItems(inSection: section.id), 2, "hidden な CustomCell は除外される")
        XCTAssertEqual(
            snapshot?.itemIdentifiers(inSection: section.id),
            [KsCellID(cell: visible), KsCellID(cell: after)],
            "hidden 行が抜けた分だけ後続の並びが詰まる"
        )
    }

    /// `VisibilityAware` に準拠している（projection のフィルタが問い合わせられる）。
    func test_CustomCellはVisibilityAwareに準拠する() {
        let cell = CustomCell(content: "x", isVisible: false) { v in Text(v) }
        XCTAssertEqual((cell as? VisibilityAware)?.isVisible, false)
    }

    // MARK: - DSL 経路の copy 生成（withDSLID / withStyle）

    func test_withDSLIDはidだけを差し替えたcopyを返す() {
        let original = CustomCell(content: "A", showArrow: true, isEnabled: false, isVisible: false) { v in
            CustomCellProbe(identifier: "probe-\(v)")
        }
        let newID = UUID()
        let copy = original.withDSLID(newID)

        XCTAssertEqual(copy.id, newID)
        XCTAssertEqual(copy.content, original.content)
        XCTAssertEqual(copy.showArrow, true)
        XCTAssertEqual(copy.isEnabled, false)
        XCTAssertEqual(copy.isVisible, false)

        // builder も引き継がれている（copy で content が描画できる）
        let view = CustomCellView()
        view.render(cell: copy, theme: Theme())
        host(view, renderedIdentifier: "probe-A")
        XCTAssertNotNil(findView(identifier: "probe-A", in: view))
    }

    func test_withStyleはstyleだけを差し替えたcopyを返す() {
        let original = CustomCell(content: "A") { v in CustomCellProbe(identifier: "probe-\(v)") }
        let newStyle = CellStyle(cellHeight: 111)
        let copy = original.withStyle(newStyle)

        XCTAssertEqual(copy.id, original.id)
        XCTAssertEqual(copy.style, newStyle)
        XCTAssertEqual(copy.content, original.content)

        let view = CustomCellView()
        view.render(cell: copy, theme: Theme())
        host(view, renderedIdentifier: "probe-A")
        XCTAssertNotNil(findView(identifier: "probe-A", in: view))
    }
}
#endif

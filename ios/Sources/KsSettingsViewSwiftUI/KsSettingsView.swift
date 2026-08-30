// KsSettingsView.swift
// KsSettingsViewSwiftUI
//
// `KsSettingsViewController` を SwiftUI から扱える `UIViewControllerRepresentable` ラッパ。
//
// Root H/F を指定する modifier は `.rootHeader(...)` / `.rootFooter(...)` に一本化する
// （Section 級の H/F と名前で区別するため、`.header(...)` / `.footer(...)` は用意しない）。
//
// DSL init の builder は `@KsSettingsViewBuilder _ sections: () -> [DSLSectionNode]` とし、
// Node 経路で安定 ID を解決してから `[KsSettingsViewCore.Section]` に変換する（core/ADR-0008）。
// DSL 方式のバックエンド `DSLBackedRepresentable` は `UIViewControllerRepresentable` として実装し、
// `body` getter に副作用を持たせず `updateUIViewController(_:context:)` で Diff を適用する
// （`body` は SwiftUI が任意のタイミング・回数で評価するため、副作用を置くと更新回数に依存する）。

#if canImport(UIKit)
import SwiftUI
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

/// SwiftUI から `KsSettingsViewController` を扱うためのラッパ。
///
/// 2 系統の利用 API を提供する：
/// - Store 方式 init (`init(store: SettingsRootStore, style:)`)：
///   無限スクロール / 大量データ / リアルタイム高頻度更新等のパワーユーザー向け。
/// - DSL 方式 init (`init(style:, @KsSettingsViewBuilder _ sections:)`)：
///   宣言的に Cell ツリーを記述する。一般用途向け。
///   内部で `@StateObject` 内蔵の Bookkeeper が `SettingsRootStore` と前回 DSL ツリーを保持し、
///   `updateUIViewController` のたびに DSL 評価結果と前回ツリーを比較して `SettingsRootDiff` 列を
///   算出、内部 Store 経由で Controller に流す。
public struct KsSettingsView: View {

    /// Root Header（`.rootHeader(...)` modifier 由来）
    internal var _rootHeader: RootAccessory?
    /// Root Footer（`.rootFooter(...)` modifier 由来）
    internal var _rootFooter: RootAccessory?
    /// `.style(_:)` modifier または init 引数由来の描画スタイル
    internal var _style: KsSettingsViewStyle
    /// `.theme(_:)` modifier 由来の Theme（`nil` なら DSL/Store 側の Theme を維持）
    internal var _theme: Theme?

    /// バック実装の種別を内部で保持する。
    /// - `.store(store)`：Store 方式（外部 Store を参照）
    /// - `.dsl(builder)`：DSL 方式（内部 Store を `@StateObject` で保持）
    internal enum Backing {
        case store(SettingsRootStore)
        case dsl(builder: () -> [DSLSectionNode])
    }

    /// バック実装。
    internal let backing: Backing

    /// Store 方式 init（パワーユーザー向け）。
    /// - Parameters:
    ///   - store: 監視対象 `SettingsRootStore`
    ///   - style: 描画スタイル（既定 `.classic`）
    public init(store: SettingsRootStore, style: KsSettingsViewStyle = .classic) {
        self.backing = .store(store)
        self._style = style
        self._rootHeader = nil
        self._rootFooter = nil
        self._theme = nil
    }

    /// DSL 方式 init（一般用途向け）。
    /// - Parameters:
    ///   - style: 描画スタイル（既定 `.classic`）
    ///   - sections: `@KsSettingsViewBuilder` ベースの宣言ツリー（`[DSLSectionNode]` を返す）
    public init(
        style: KsSettingsViewStyle = .classic,
        @KsSettingsViewBuilder _ sections: @escaping () -> [DSLSectionNode]
    ) {
        self.backing = .dsl(builder: sections)
        self._style = style
        self._rootHeader = nil
        self._rootFooter = nil
        self._theme = nil
    }

    public var body: some View {
        switch backing {
        case .store(let store):
            // Store 方式は外部 Store をそのまま使う Representable。
            StoreBackedRepresentable(
                store: store,
                style: _style,
                rootHeader: _rootHeader,
                rootFooter: _rootFooter,
                theme: _theme
            )
        case .dsl(let builder):
            // DSL 方式は内部に `@StateObject` の Bookkeeper を持つ Representable をラップ。
            DSLBackedRepresentableView(
                builder: builder,
                style: _style,
                rootHeader: _rootHeader,
                rootFooter: _rootFooter,
                theme: _theme
            )
        }
    }

    // MARK: - View modifier API

    /// Root Header を文字列で指定する。
    public func rootHeader(_ text: String) -> KsSettingsView {
        var copy = self
        copy._rootHeader = .text(text)
        return copy
    }

    /// Root Header を任意 View で指定する。
    public func rootHeader<V: View>(@ViewBuilder content: () -> V) -> KsSettingsView {
        let view = content()
        var copy = self
        copy._rootHeader = .view(KsAnyView.swiftUI { view })
        return copy
    }

    /// Root Footer を文字列で指定する。
    public func rootFooter(_ text: String) -> KsSettingsView {
        var copy = self
        copy._rootFooter = .text(text)
        return copy
    }

    /// Root Footer を任意 View で指定する。
    public func rootFooter<V: View>(@ViewBuilder content: () -> V) -> KsSettingsView {
        let view = content()
        var copy = self
        copy._rootFooter = .view(KsAnyView.swiftUI { view })
        return copy
    }

    /// 描画スタイルを切り替える。
    public func style(_ style: KsSettingsViewStyle) -> KsSettingsView {
        var copy = self
        copy._style = style
        return copy
    }

    /// Theme を切り替える。
    public func theme(_ theme: Theme) -> KsSettingsView {
        var copy = self
        copy._theme = theme
        return copy
    }

    // MARK: - テスト容易化 API（Store 方式専用）

    /// テスト・ホスティング両用に `KsSettingsViewController` を生成する（Store 方式専用）。
    /// DSL 方式の場合はランタイム検証エラー（`fatalError`）となる。
    public func makeController() -> KsSettingsViewController {
        guard case .store(let store) = backing else {
            fatalError("makeController() is supported only for store-backed KsSettingsView. Use DSL via SwiftUI hierarchy.")
        }
        let controller = KsSettingsViewController(store: store, style: _style)
        controller.rootHeader = _rootHeader
        controller.rootFooter = _rootFooter
        if let theme = _theme {
            // Theme は Store 経由で applyTheme し、Store の `$theme` 経由で Controller に伝播する。
            store.applyTheme(theme)
        }
        return controller
    }

    /// 既存 `KsSettingsViewController` に style / Root H/F の更新を適用する（Store 方式専用）。
    public func applyUpdate(
        to uiViewController: KsSettingsViewController,
        coordinator: Coordinator
    ) {
        if uiViewController.style != _style {
            uiViewController.style = _style
        }
        if uiViewController.rootHeader != _rootHeader {
            uiViewController.rootHeader = _rootHeader
        }
        if uiViewController.rootFooter != _rootFooter {
            uiViewController.rootFooter = _rootFooter
        }
    }

    public func makeCoordinator() -> Coordinator {
        return Coordinator()
    }

    /// SwiftUI Coordinator（Store 方式専用、本提案では拡張用フックのみ提供）。
    public final class Coordinator {
        public init() {}
    }
}

// MARK: - Store 方式 Representable

/// Store 方式専用の `UIViewControllerRepresentable` バックエンド。
internal struct StoreBackedRepresentable: UIViewControllerRepresentable {
    let store: SettingsRootStore
    let style: KsSettingsViewStyle
    let rootHeader: RootAccessory?
    let rootFooter: RootAccessory?
    let theme: Theme?

    func makeUIViewController(context: Context) -> KsSettingsViewController {
        let controller = KsSettingsViewController(store: store, style: style)
        controller.rootHeader = rootHeader
        controller.rootFooter = rootFooter
        if let theme = theme {
            store.applyTheme(theme)
        }
        return controller
    }

    func updateUIViewController(_ uiViewController: KsSettingsViewController, context: Context) {
        if uiViewController.style != style {
            uiViewController.style = style
        }
        if uiViewController.rootHeader != rootHeader {
            uiViewController.rootHeader = rootHeader
        }
        if uiViewController.rootFooter != rootFooter {
            uiViewController.rootFooter = rootFooter
        }
        if let theme = theme, store.theme != theme {
            store.applyTheme(theme)
        }
    }
}

// MARK: - DSL 方式 Representable

/// DSL 方式専用の `View`。
///
/// 内部に `@StateObject private var bookkeeper: DSLBookkeeper` を保持し、SwiftUI の
/// View identity が同じ間は Store と前回 DSL ツリーを保ち続ける。
///
/// body 内では副作用を一切起こさず、子 `DSLBackedRepresentable` の
/// `updateUIViewController(_:context:)` でのみ DSL を再評価して Diff を内部 Store に流す。
@MainActor
internal struct DSLBackedRepresentableView: View {

    let builder: () -> [DSLSectionNode]
    let style: KsSettingsViewStyle
    let rootHeader: RootAccessory?
    let rootFooter: RootAccessory?
    let theme: Theme?

    @StateObject private var bookkeeper: DSLBookkeeper

    init(
        builder: @escaping () -> [DSLSectionNode],
        style: KsSettingsViewStyle,
        rootHeader: RootAccessory?,
        rootFooter: RootAccessory?,
        theme: Theme?
    ) {
        self.builder = builder
        self.style = style
        self.rootHeader = rootHeader
        self.rootFooter = rootFooter
        self.theme = theme

        // `StateObject(wrappedValue:)` は autoclosure で初回のみ評価される。
        // SwiftUI が親 View 再評価で本 View の `init` を何度も呼んでも、`builder()` 評価と
        // 初期 Store / Bookkeeper 構築は **最初の 1 回だけ** 走るようにする。
        // （init 内で eager に `builder()` を評価すると、再 init のたびに
        // 不要なツリー構築コストが発生する）
        self._bookkeeper = StateObject(wrappedValue: Self.makeInitialBookkeeper(
            builder: builder,
            rootHeader: rootHeader,
            rootFooter: rootFooter,
            theme: theme
        ))
    }

    /// 初回 Bookkeeper 構築ロジックを関数化。`StateObject(wrappedValue:)` の autoclosure
    /// から 1 度だけ呼ばれる前提。
    private static func makeInitialBookkeeper(
        builder: () -> [DSLSectionNode],
        rootHeader: RootAccessory?,
        rootFooter: RootAccessory?,
        theme: Theme?
    ) -> DSLBookkeeper {
        let initialNodes = builder()
        let initialTheme = theme ?? Theme()
        let initialTree = DSLRootTree(
            sectionNodes: initialNodes,
            rootHeader: rootHeader,
            rootFooter: rootFooter,
            theme: initialTheme
        )
        let initialSections = initialTree.resolvedSections()
        let initialRoot = SettingsRoot(sections: initialSections)
        let store = SettingsRootStore(initialRoot: initialRoot, initialTheme: initialTheme)
        return DSLBookkeeper(
            store: store,
            initialTree: DSLDiffCalculator.ResolvedTree(
                sections: initialSections,
                rootHeader: rootHeader,
                rootFooter: rootFooter,
                theme: initialTheme
            )
        )
    }

    var body: some View {
        // body は副作用を起こさず、Representable を生成して返すだけ。
        // DSL 評価 / Diff 算出 / Store 反映は updateUIViewController 内で実施する。
        DSLBackedRepresentable(
            bookkeeper: bookkeeper,
            builder: builder,
            style: style,
            rootHeader: rootHeader,
            rootFooter: rootFooter,
            theme: theme
        )
    }
}

/// DSL 方式専用の `UIViewControllerRepresentable`。
///
/// `updateUIViewController(_:context:)` 内で DSL を再評価して Diff 列を内部 Store に流す。
/// SwiftUI の「body は副作用を起こさない」規約に準拠した経路（Apple 公式ガイドライン）。
@MainActor
internal struct DSLBackedRepresentable: UIViewControllerRepresentable {

    let bookkeeper: DSLBookkeeper
    let builder: () -> [DSLSectionNode]
    let style: KsSettingsViewStyle
    let rootHeader: RootAccessory?
    let rootFooter: RootAccessory?
    let theme: Theme?

    func makeUIViewController(context: Context) -> KsSettingsViewController {
        let controller = KsSettingsViewController(store: bookkeeper.store, style: style)
        controller.rootHeader = rootHeader
        controller.rootFooter = rootFooter
        if let theme = theme, bookkeeper.store.theme != theme {
            bookkeeper.store.applyTheme(theme)
        }
        return controller
    }

    func updateUIViewController(_ uiViewController: KsSettingsViewController, context: Context) {
        // 1. style / Root H/F の即時反映（Controller プロパティ更新）。
        if uiViewController.style != style {
            uiViewController.style = style
        }
        if uiViewController.rootHeader != rootHeader {
            uiViewController.rootHeader = rootHeader
        }
        if uiViewController.rootFooter != rootFooter {
            uiViewController.rootFooter = rootFooter
        }

        // 2. DSL を再評価して Diff 列を算出し、内部 Store に流す。
        //    Store の Diff Publisher 経由で Controller の applyDiff が走る。
        evaluateAndApplyDiff()
    }

    /// 新ツリーを評価し、前回ツリーと比較して Diff 列を内部 Store に流す。
    private func evaluateAndApplyDiff() {
        // ノード経路では DSL ヒントは Node 自身に保持されるため、レジストリは
        // 旧戻り型互換系（`SettingsRoot { ksSection(...) }` 等）が使用する場合のみ意味を持つ。
        // 念のため毎回 reset しておく。
        DSLHintRegistry.shared.reset()

        let newNodes = builder()
        let newTheme = theme ?? bookkeeper.lastTree.theme
        let newRootTree = DSLRootTree(
            sectionNodes: newNodes,
            rootHeader: rootHeader,
            rootFooter: rootFooter,
            theme: newTheme
        )
        let newSections = newRootTree.resolvedSections()
        let newResolved = DSLDiffCalculator.ResolvedTree(
            sections: newSections,
            rootHeader: rootHeader,
            rootFooter: rootFooter,
            theme: newTheme
        )

        // 構造 Diff を Store に反映
        let diffs = DSLDiffCalculator.compute(from: bookkeeper.lastTree, to: newResolved)
        for diff in diffs {
            applyDiffToStore(diff)
        }
        // Theme は UI 層の責務であり構造 Diff の枠外で applyTheme を経由する（core/ADR-0009）。
        if bookkeeper.lastTree.theme != newTheme {
            bookkeeper.store.applyTheme(newTheme)
        }
        bookkeeper.lastTree = newResolved
    }

    /// `SettingsRootDiff` を内部 Store の対応メソッドに変換して呼ぶ。
    /// Store の `diffPublisher` を経由して Controller に自動反映される。
    private func applyDiffToStore(_ diff: SettingsRootDiff) {
        let store = bookkeeper.store
        switch diff {
        case .full(let root):
            store.replaceAll(root)
        case let .insertSection(index, section):
            store.insertSection(section, at: index)
        case .removeSection(let sectionID):
            store.removeSection(sectionID: sectionID)
        case let .moveSection(from, to):
            store.moveSection(from: from, to: to)
        case let .replaceSection(sectionID, new):
            store.replaceSection(sectionID: sectionID, new: new)
        case let .insertCell(sectionID, index, cell):
            store.insertCell(cell, in: sectionID, at: index)
        case .removeCell(let cellID):
            store.removeCell(cellID: cellID)
        case let .replaceCell(cellID, new):
            store.replaceCell(cellID: cellID, new: new)
        case let .moveCell(cellID, to):
            store.moveCell(cellID: cellID, to: to)
        case let .updateAccessory(target, accessory):
            store.updateAccessory(target: target, accessory: accessory)
        }
    }
}

/// DSL 方式の内部 Store と前回ツリーを保持する書記係。
/// `@StateObject` で View identity をまたいで保持される。
@MainActor
internal final class DSLBookkeeper: ObservableObject {
    let store: SettingsRootStore
    var lastTree: DSLDiffCalculator.ResolvedTree

    init(store: SettingsRootStore, initialTree: DSLDiffCalculator.ResolvedTree) {
        self.store = store
        self.lastTree = initialTree
    }
}

#endif

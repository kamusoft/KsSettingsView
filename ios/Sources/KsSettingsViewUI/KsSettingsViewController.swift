// KsSettingsViewController.swift
// KsSettingsViewUI
//
// 設定画面の中核 ViewController。`UICollectionView` + `UICollectionViewDiffableDataSource` +
// `UICollectionLayoutListConfiguration`（iOS 14+）で構築する。
//
// 公開 API の骨格：
//   - `root` は公開 setter を持たず、`SettingsRootStore` 経由で初期化し Diff を購読する
//     （core/ADR-0006）。差分の適用口は `applyDiff(_:)`
//   - Root H/F は Core モデルではなく UI 層プロパティ `rootHeader` / `rootFooter` が持つ
//     （core/ADR-0005）
//   - 表示の更新は構造同期・内容同期・可視性の 3 経路に分けて行い、推測による一括 refresh は
//     持たない（core/ADR-0010）

#if canImport(UIKit)
import UIKit
import SwiftUI
import Combine
import os.log
import KsSettingsViewCore

/// 設定画面の中核 `UIViewController`。
///
/// `SettingsRootStore` を購読し、Store の Diff 発行に応じて `applyDiff(_:)` で
/// 内部 `UICollectionViewDiffableDataSource` の snapshot を部分更新する。
///
/// `style` 設定時は内部 `UICollectionView` のレイアウトを再構築する。
@MainActor
public final class KsSettingsViewController: UIViewController {
    // MARK: - 公開定数

    /// Root Header supplementary の elementKind 識別子
    public static let rootHeaderElementKind = "ks-root-header"
    /// Root Footer supplementary の elementKind 識別子
    public static let rootFooterElementKind = "ks-root-footer"

    /// `applyDiff` 中の存在しない ID 操作などのログ出力用。
    private static let log = OSLog(
        subsystem: "jp.kamusoft.kssettingsview",
        category: "KsSettingsViewController"
    )

    // MARK: - 公開プロパティ

    /// 描画スタイル（`.classic` / `.modern`）。
    /// 設定時は内部レイアウトを再構築し、既存 root を再 apply する（差分アニメーションは無し）。
    public var style: KsSettingsViewStyle {
        didSet {
            guard style != oldValue else { return }
            rebuildLayout()
            // 箱への clip は style ごとに変わるため、表示中の Cell を再構成して掛け直す
            // （構造も内容も変わらないため snapshot の差分だけでは Cell provider が呼ばれない）。
            reconfigureVisibleCells()
        }
    }

    /// Root Header。`nil` でヘッダ非表示。
    ///
    /// setter で boundary supplementary item の構成を更新する。`nil` 化したときは
    /// boundary 構成から該当 item を除外してレイアウトを再構築する。
    public var rootHeader: RootAccessory? {
        didSet {
            // 旧値が nil で新値が非 nil（または逆）の場合は boundary 構成自体が変わるため
            // レイアウトを再構築する。両方非 nil の場合は可視 supplementary を再描画。
            let oldIsNil = (oldValue == nil)
            let newIsNil = (rootHeader == nil)
            if oldIsNil != newIsNil {
                rebuildLayout()
            } else if !newIsNil {
                refreshRootSupplementary(elementKind: Self.rootHeaderElementKind)
            }
        }
    }

    /// Root Footer。`nil` でフッタ非表示。
    public var rootFooter: RootAccessory? {
        didSet {
            let oldIsNil = (oldValue == nil)
            let newIsNil = (rootFooter == nil)
            if oldIsNil != newIsNil {
                rebuildLayout()
            } else if !newIsNil {
                refreshRootSupplementary(elementKind: Self.rootFooterElementKind)
            }
        }
    }

    // MARK: - 内部状態

    /// 内部 Cell レジストリ（DI 可能）
    public let registry: KsCellRegistry

    /// 現在の `SettingsRoot`。
    ///
    /// 公開 setter は提供しない（Store 経由 / `applyDiff` 経由でのみ更新）。
    /// テスト・内部診断用に `internal` で公開する。
    internal private(set) var root: SettingsRoot

    /// 現在の Theme。`SettingsRoot` から削除されたため、Controller 自身が保持する。
    /// 初期値は `Theme()`。`applyTheme(_:)` 経由で更新する。
    internal private(set) var currentTheme: Theme

    /// 内部 `UICollectionView`
    private(set) var collectionView: UICollectionView!

    /// DiffableDataSource
    private var dataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>!

    /// Section ID → Section モデルのマップ（cell provider・supplementary provider が参照する）
    /// 注: `SwiftUI.Section` と区別するため `KsSettingsViewCore.Section` で完全修飾。
    private var sectionIndex: [UUID: KsSettingsViewCore.Section] = [:]
    /// KsCellID → Cell モデルのマップ（cell provider が参照する）
    private var cellIndex: [KsCellID: any KsCell] = [:]
    /// `root.sections` から `Section.isVisible` および各 Cell の `VisibilityAware.isVisible` で
    /// フィルタした visible projection。`indexPath` 経由の描画系（layout / supplementary view /
    /// separator）はこの projection を参照する（仕様: `visible projection の二重管理`）。
    private var visibleSections: [KsSettingsViewCore.Section] = []

    /// 直近に Root Header / Footer へ適用した Section 単位余白。
    ///
    /// Root accessory の作り直しは View accessory の内部状態を失わせるため、この値と比較して
    /// 余白が実際に変わったときだけ再構成する。
    private var appliedRootAccessoryMargin: NSDirectionalEdgeInsets?

    /// Store 購読 Cancellable（deinit で cancel する）
    private var storeSubscription: AnyCancellable?

    /// 内容更新バッチ購読 Cancellable（deinit で cancel する）
    private var contentUpdateSubscription: AnyCancellable?

    /// accessory 再計測要求の購読 Cancellable（deinit で cancel する）
    private var accessoryMeasureSubscription: AnyCancellable?

    /// 購読中の Store。バッチ内容更新の受信時に更新後の状態を読み直すために保持する。
    /// Store は Controller より長命になりうるため weak 参照とし、Store 側の購読者経由で
    /// Store 自身が retain され続ける循環を作らない。
    private weak var connectedStore: SettingsRootStore?

    // MARK: - 初期化

    /// Store 経由の公開イニシャライザ。
    ///
    /// 初期 root は `store.root` から、初期 theme は `store.theme` から取得し、Store の Diff Publisher
    /// および Theme Publisher を購読する統合経路を確立する。
    /// - Parameters:
    ///   - store: 監視対象 `SettingsRootStore`
    ///   - style: 描画スタイル（既定 `.classic`）
    ///   - registry: Cell レジストリ（既定 `KsCellRegistry.shared`）
    ///   - autoRegisterBasicCells: shared registry に基本 Cell 7 種を自動登録するか
    ///     （既定 `true`、テスト等で抑止可）
    ///   - autoRegisterCustomCell: shared registry に `CustomCell` を自動登録するか
    ///     （既定 `true`、テスト等で抑止可）
    public convenience init(
        store: SettingsRootStore,
        style: KsSettingsViewStyle = .classic,
        registry: KsCellRegistry = .shared,
        autoRegisterBasicCells: Bool = true,
        autoRegisterInputCells: Bool = true,
        autoRegisterCustomCell: Bool = true
    ) {
        self.init(
            root: store.root,
            theme: store.theme,
            style: style,
            registry: registry,
            autoRegisterBasicCells: autoRegisterBasicCells,
            autoRegisterInputCells: autoRegisterInputCells,
            autoRegisterCustomCell: autoRegisterCustomCell
        )
        connectStore(store)
    }

    /// Preview / Test 用の internal イニシャライザ。
    ///
    /// Store を介さず `SettingsRoot` を直接渡せる。Store 購読は行わない。
    /// - Parameters:
    ///   - root: 初期 `SettingsRoot`
    ///   - theme: 初期 Theme（既定 `Theme()`）
    ///   - style: 描画スタイル（既定 `.classic`）
    ///   - registry: Cell レジストリ（既定 `KsCellRegistry.shared`）
    ///   - autoRegisterBasicCells: shared registry に基本 Cell 7 種を自動登録するか
    ///   - autoRegisterCustomCell: shared registry に `CustomCell` を自動登録するか
    internal init(
        root: SettingsRoot = SettingsRoot(),
        theme: Theme = Theme(),
        style: KsSettingsViewStyle = .classic,
        registry: KsCellRegistry = .shared,
        autoRegisterBasicCells: Bool = true,
        autoRegisterInputCells: Bool = true,
        autoRegisterCustomCell: Bool = true
    ) {
        self.style = style
        self.registry = registry
        self.root = root
        self.currentTheme = theme
        super.init(nibName: nil, bundle: nil)
        // 基本 Cell 7 種を自動登録する。
        // テスト等で shared 以外の registry が DI される場合は触らない。
        if autoRegisterBasicCells && registry === KsCellRegistry.shared {
            registry.registerBasicCells()
        }
        // 入力系 Cell 5 種を自動登録する。
        if autoRegisterInputCells && registry === KsCellRegistry.shared {
            registry.registerInputCells()
        }
        // CustomCell を自動登録。基本 / 入力 Cell と同列の標準登録集合として扱い、
        // 利用者が Registry を操作しなくても描画できるようにする。
        if autoRegisterCustomCell && registry === KsCellRegistry.shared {
            registry.registerCustomCell()
        }
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    deinit {
        // メモリリーク防止: DataSource / Delegate / 内部 index / Store 購読を明示的に解放する。
        // 解放順序が重要: Store 購読 → CollectionView の dataSource/delegate → DataSource 本体 → index。
        // UICollectionView は `dataSource` を strong 参照で保持するため、先に
        // `collectionView.dataSource = nil` で解除しないと `self.dataSource = nil` を代入しても
        // メモリ解放されない。本順序により Cycle を確実に断つ。
        // 購読解除は disconnectStore() と同内容の複製。deinit は nonisolated な文脈のため
        // MainActor-isolated な disconnectStore() を呼べず、一本化できない (プロパティへの
        // 直接アクセスは deinit の特例で許される)。購読を増やすときは両方に追記すること。
        storeSubscription?.cancel()
        storeSubscription = nil
        contentUpdateSubscription?.cancel()
        contentUpdateSubscription = nil
        accessoryMeasureSubscription?.cancel()
        accessoryMeasureSubscription = nil
        themeSubscription?.cancel()
        themeSubscription = nil
        if let cv = self.collectionView {
            // collectionView が dataSource / delegate を retain しているため、先に解除する。
            cv.dataSource = nil
            cv.delegate = nil
        }
        // DataSource は内部で collectionView を強参照しうるため明示解放
        self.dataSource = nil
        self.sectionIndex.removeAll()
        self.cellIndex.removeAll()
    }

    // MARK: - visible projection 構築

    /// `Section.isVisible` および各 Cell の `VisibilityAware.isVisible` でフィルタした visible
    /// projection を返す。
    ///
    /// - `Section.isVisible == false` の Section は header / footer / 全 cells を除外して projection
    ///   から外す。
    /// - visible な Section の `cells` から `(cell as? VisibilityAware)?.isVisible == false` を除外し、
    ///   残った Cell のみで Section を再構築する。`VisibilityAware` 非準拠 Cell は常に visible 扱い
    ///   （safe-by-default）。
    ///
    /// モデルの `sections` は保持したまま、描画用の projection だけをここで作る。可視性は
    /// 構造同期・内容同期とは独立した経路として扱う（core/ADR-0010）。
    internal static func computeVisibleSections(
        from sections: [KsSettingsViewCore.Section]
    ) -> [KsSettingsViewCore.Section] {
        var result: [KsSettingsViewCore.Section] = []
        result.reserveCapacity(sections.count)
        for section in sections {
            guard section.isVisible else { continue }
            let visibleCells: [any KsCell] = section.cells.filter { cell in
                (cell as? VisibilityAware)?.isVisible ?? true
            }
            // header / footer / headerHeight / id と Header・Footer の表示トグルは維持し、
            // cells のみ visible Cell に絞る。
            // isVisible は visible projection 内では実質 true 固定だが、Section 値型として
            // フィールドを欠落させない（projection 上の Section も Section 値型として扱える）。
            let projected = KsSettingsViewCore.Section(
                id: section.id,
                header: section.header,
                footer: section.footer,
                cells: visibleCells,
                headerHeight: section.headerHeight,
                isVisible: true,
                isHeaderVisible: section.isHeaderVisible,
                isFooterVisible: section.isFooterVisible
            )
            result.append(projected)
        }
        return result
    }

    /// 現在の `root.sections` から visible projection を再計算してキャッシュする。
    private func rebuildVisibleProjection() {
        self.visibleSections = Self.computeVisibleSections(from: root.sections)
    }

    // MARK: - Store 接続

    /// Store の購読 Cancellable 群（Theme 購読も含む）。
    private var themeSubscription: AnyCancellable?

    /// Store の Diff Publisher と Theme Publisher を購読し、`applyDiff(_:)` / `applyTheme(_:)` を
    /// 呼ぶ統合経路を確立する。
    ///
    /// `[weak self]` キャプチャでメモリリークを防止する。
    private func connectStore(_ store: SettingsRootStore) {
        connectedStore = store
        storeSubscription = store.diffPublisher
            .sink { [weak self] diff in
                guard let self = self else { return }
                self.applyDiff(diff)
            }
        // 複数 Cell の内容更新バッチを購読し、1 回の部分更新でまとめて反映する。
        contentUpdateSubscription = store.contentUpdateBatchPublisher
            .sink { [weak self] cellIDs in
                guard let self = self else { return }
                self.applyContentUpdateBatch(cellIDs)
            }
        // accessory の再計測要求を購読し、対象領域だけを測り直す。
        accessoryMeasureSubscription = store.accessoryMeasureInvalidationPublisher
            .sink { [weak self] target in
                guard let self = self else { return }
                self.invalidateAccessoryMeasurement(target: target)
            }
        // Theme 購読: Store.$theme の値変化を受けて applyTheme を呼ぶ。
        // dropFirst() で初期値は無視（init で既に currentTheme に取り込み済みのため）。
        themeSubscription = store.$theme
            .dropFirst()
            .sink { [weak self] theme in
                guard let self = self else { return }
                self.applyTheme(theme)
            }
    }

    /// Store 購読を解除し、Controller を Store から切り離す。
    ///
    /// 解除後は Store への更新（構造 Diff・内容更新バッチ・Theme）が表示へ反映されなくなる。
    /// 表示中の内容はそのまま残るため、view 階層からの取り外しと参照の破棄は呼び出し側の責務。
    /// 冪等であり、Store 未接続および解除済みの状態で呼んでも何も起きない。
    /// 再接続用の API は持たない — 再接続は同じ Store から新しい Controller を生成して行う。
    public func disconnectStore() {
        storeSubscription?.cancel()
        storeSubscription = nil
        contentUpdateSubscription?.cancel()
        contentUpdateSubscription = nil
        accessoryMeasureSubscription?.cancel()
        accessoryMeasureSubscription = nil
        themeSubscription?.cancel()
        themeSubscription = nil
        connectedStore = nil
    }

    // MARK: - Accessory の再計測

    /// 指定した accessory 領域の高さを測り直す。
    ///
    /// `UICollectionView` の self-sizing は、accessory の中身が内在サイズを無効化しただけでは
    /// その場で領域の高さを測り直さない。対象の supplementary だけを指定した無効化を layout へ渡すことで、
    /// 他の領域の再計算を巻き込まずに当該領域の高さだけを追従させる。
    ///
    /// 追従の条件は accessory の種別で異なる。Section の accessory は layout が領域高さの解を保持するため、
    /// この無効化が無い限り解が固定されたままになる。Root accessory は layout 全体の
    /// boundary supplementary で領域高さの解を持たないため、無効化しなくても次の表示更新で
    /// 測り直される — Root に対してこの API が与えるのは即時性である。
    ///
    /// 対象が表示対象に存在しないとき (未設定の Root accessory・hidden または未知の Section) は
    /// no-op。固定高さの Section header は無効化しても同じ高さに解決されるため表示は変わらない。
    ///
    /// - Parameter target: 再計測する accessory
    public func invalidateAccessoryMeasurement(target: AccessoryTarget) {
        guard let collectionView = self.collectionView else { return }
        guard let path = accessoryElementPath(for: target) else { return }
        let context = UICollectionViewLayoutInvalidationContext()
        context.invalidateSupplementaryElements(ofKind: path.kind, at: [path.indexPath])
        collectionView.collectionViewLayout.invalidateLayout(with: context)
    }

    /// 再計測対象を supplementary の elementKind と indexPath へ解決する。
    ///
    /// Section 系は「visible projection の二重管理」に従い、indexPath の section を visible
    /// projection 上の位置で解決する。Root 系は layout configuration の boundary supplementary
    /// であり、Section 側と同じ先頭 indexPath で指定する。
    ///
    /// - Returns: 表示対象に存在しない accessory では `nil`
    private func accessoryElementPath(
        for target: AccessoryTarget
    ) -> (kind: String, indexPath: IndexPath)? {
        let firstIndexPath = IndexPath(item: 0, section: 0)
        switch target {
        case .rootHeader:
            guard rootHeader != nil else { return nil }
            return (Self.rootHeaderElementKind, firstIndexPath)
        case .rootFooter:
            guard rootFooter != nil else { return nil }
            return (Self.rootFooterElementKind, firstIndexPath)
        case .sectionHeader(let sectionID):
            guard let index = visibleSections.firstIndex(where: { $0.id == sectionID }) else {
                return nil
            }
            return (UICollectionView.elementKindSectionHeader, IndexPath(item: 0, section: index))
        case .sectionFooter(let sectionID):
            guard let index = visibleSections.firstIndex(where: { $0.id == sectionID }) else {
                return nil
            }
            return (UICollectionView.elementKindSectionFooter, IndexPath(item: 0, section: index))
        }
    }

    /// 外部から Theme を適用する公開 API（Store 経由ではなく直接適用したいケース用）。
    ///
    /// `Theme` 変更を Cell に反映し、`backgroundColor` を `UICollectionView.backgroundColor` に
    /// 反映する。Diff Publisher は介さず、本メソッドのみで完結する。
    /// - Parameter theme: 新しい Theme
    public func applyTheme(_ theme: Theme) {
        self.currentTheme = theme
        applyBackgroundColor(theme: theme)
        // Section 装飾（余白・角丸・ボーダー・箱の塗り色）を layout へ反映する。
        // 余白は sectionProvider が、装飾値は decoration が読むため、双方を再評価させる。
        refreshSectionBoxAppearance()
        // Section 単位余白と箱 clip を新しい Theme から解決し直す。Root Header / Footer は
        // 余白の解決値が変わったときだけ作り直す（作り直しは View accessory の内部状態を失わせる）。
        refreshSectionUnitPresentation()
        // Header / Footer のテキストも Cell と同じ Theme の文字色・フォントで描くため、
        // 表示中のものへ新しい Theme を再適用する。Root と Section で同じ規律を適用する。
        refreshRootAccessoryTextAppearance()
        refreshSectionAccessoryTextAppearance()
        guard let dataSource = self.dataSource else {
            collectionView?.collectionViewLayout.invalidateLayout()
            return
        }
        var snapshot = dataSource.snapshot()
        // 全可視 Cell に新 Theme を再適用する。reconfigureItems で同一セルを破棄せず再構成する。
        if #available(iOS 15.0, *) {
            snapshot.reconfigureItems(snapshot.itemIdentifiers)
        } else {
            snapshot.reloadItems(snapshot.itemIdentifiers)
        }
        dataSource.apply(snapshot, animatingDifferences: false)
        // Section / Cell の identity は変えずに、装飾だけを新しい値で描き直す。
        collectionView?.collectionViewLayout.invalidateLayout()
    }

    // MARK: - View ライフサイクル

    public override func loadView() {
        let container = UIView()
        container.backgroundColor = .systemBackground

        let layout = makeLayout(for: style)
        let cv = UICollectionView(frame: .zero, collectionViewLayout: layout)
        // Theme.backgroundColor を初期化時から反映してチラつきを回避する
        // （viewDidLoad での applyBackgroundColor までの間、`.systemBackground` で
        // 一瞬表示されるのを防ぐ）。
        cv.backgroundColor = currentTheme.backgroundColor
        // AiForms 互換: スクロール時に編集中のキーボードを閉じる挙動を有効化する
        // （`EntryCell` 編集中にドラッグするとキーボードが自動的に閉じる）。
        cv.keyboardDismissMode = .onDrag
        cv.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(cv)
        NSLayoutConstraint.activate([
            cv.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            cv.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            cv.topAnchor.constraint(equalTo: container.topAnchor),
            cv.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        self.collectionView = cv
        self.view = container
    }

    public override func viewDidLoad() {
        super.viewDidLoad()
        configureDataSource()
        // 表示を構築する前に、接続中 Store の現在状態を内部状態へ取り込む。
        resyncFromStore()
        // 初期 snapshot を apply
        applyFullSnapshot(root: root, animated: false)
        // Theme.backgroundColor を反映
        applyBackgroundColor(theme: currentTheme)
        // Section 単位の余白のうち list 端に接する分を反映
        applyListEdgeMargin()
    }

    /// 接続中 Store の現在状態（root / theme）を内部状態へ取り込む。
    ///
    /// Host 生成から view load までの間に Store へ適用された変更は、個々の Diff としては
    /// 適用できない（DataSource が未構築のため）。view load の時点で Store の現在状態を
    /// pull し直すことで、生成・操作・取り付けの順序によらず表示が Store の現在状態へ
    /// 収束する（core/ADR-0019）。
    ///
    /// 取り込む対象は Store が現在状態として保持するもの、すなわち設定ツリーの構造・
    /// Cell 内容・Section の accessory・Theme に限る。Root Header / Footer は UI 層
    /// プロパティであり Store の現在状態に含まれない（core/ADR-0005）ため対象外で、
    /// その反映は所有者（呼び出し側）が view load 後に適用する責務とする。
    ///
    /// Store 接続中は Store の Theme を正とする。Store 未接続（root 直接指定）の場合は
    /// 何もせず、初期化時に受け取った root / theme をそのまま使う。
    ///
    /// - Important: 本メソッドは `root` / `currentTheme` の取り込みのみを行い、表示系の
    ///   派生状態（snapshot・supplementary の再構築）は更新しない。それらの再構築は直後の
    ///   `applyFullSnapshot` / `applyBackgroundColor` が担う前提のため、呼び出しは
    ///   `viewDidLoad` のこの並び（resync → full snapshot → 背景色）の中でのみ行うこと。
    private func resyncFromStore() {
        guard let store = connectedStore else { return }
        self.root = store.root
        self.currentTheme = store.theme
    }

    /// `Theme.backgroundColor` を `UICollectionView.backgroundColor` に反映する。
    ///
    /// `UICollectionView` 全体の背景に適用するため、セクションをまたいで一様に反映される。
    private func applyBackgroundColor(theme: Theme) {
        guard let cv = self.collectionView else { return }
        cv.backgroundColor = theme.backgroundColor
    }

    // MARK: - レイアウト構築

    /// `style` から `UICollectionViewLayout` を生成する。
    ///
    /// `rootHeader` / `rootFooter` が非 `nil` の場合は対応する boundary supplementary item を
    /// レイアウト構成に追加する。
    internal func makeLayout(for style: KsSettingsViewStyle) -> UICollectionViewLayout {
        // visible projection が未構築ならここで一度だけ初期化する。`viewDidLoad` の
        // `applyFullSnapshot(root:animated:)` が走るより前に `makeLayout` が呼ばれることがあるため、
        // 初期化漏れを防ぐ。以降は applyFullSnapshot / 部分 Diff 内で都度再構築する。
        if self.visibleSections.isEmpty && !self.root.sections.isEmpty {
            rebuildVisibleProjection()
        }
        // listConfig の `headerMode` / `footerMode` は常に `.supplementary` 固定にし、
        // section ごとの header/footer の有無は sectionProvider 内で
        // `section.boundarySupplementaryItems` を間引く形で表現する。
        // これにより listConfig は visibility 切替の影響を受けない静的な値になり、layout 自体は
        // 1 度だけ生成すれば良い。listConfig を可視性に応じて変えると layout の作り直しが必要になり、
        // `setCollectionViewLayout` の同期差し替えが glitch を誘発する。
        var listConfig = UICollectionLayoutListConfiguration(appearance: appearance(for: style))
        listConfig.backgroundColor = .clear
        listConfig.headerMode = .supplementary
        listConfig.footerMode = .supplementary
        listConfig.headerTopPadding = 0

        // 罫線インセット規則。
        // Cell の位置（セクション最初 / 最後 / 中間）とアイコン有無に応じて separator を切り替える。
        listConfig.itemSeparatorHandler = { [weak self] indexPath, sectionSeparatorConfiguration in
            guard let self = self else { return sectionSeparatorConfiguration }
            return self.separatorConfiguration(for: indexPath, base: sectionSeparatorConfiguration)
        }

        // Root H/F の boundary supplementary 構成。
        // 推定高さは `.estimated(20)`（システム既定の `.estimated(44)` は過大）とし、
        // `contentInsets = .zero` を明示する。
        var boundaryItems: [NSCollectionLayoutBoundarySupplementaryItem] = []
        if rootHeader != nil {
            let item = NSCollectionLayoutBoundarySupplementaryItem(
                layoutSize: NSCollectionLayoutSize(
                    widthDimension: .fractionalWidth(1.0),
                    heightDimension: .estimated(20)
                ),
                elementKind: Self.rootHeaderElementKind,
                alignment: .top
            )
            // pinToVisibleBounds = false（スクロール追従、コンテンツと一緒にスクロールアウト）
            item.pinToVisibleBounds = false
            item.contentInsets = .zero
            boundaryItems.append(item)
        }
        if rootFooter != nil {
            let item = NSCollectionLayoutBoundarySupplementaryItem(
                layoutSize: NSCollectionLayoutSize(
                    widthDimension: .fractionalWidth(1.0),
                    heightDimension: .estimated(20)
                ),
                elementKind: Self.rootFooterElementKind,
                alignment: .bottom
            )
            item.pinToVisibleBounds = false
            item.contentInsets = .zero
            boundaryItems.append(item)
        }

        let configuration = UICollectionViewCompositionalLayoutConfiguration()
        configuration.boundarySupplementaryItems = boundaryItems
        // 隣接 Section の間隔は、前 Section の bottom 余白と次 Section の top 余白の加算。
        // Section の contentInsets では Header / Footer の内側に入ってしまうため、Section 単位の
        // 外側余白は section 間の spacing として表す。
        configuration.interSectionSpacing = Self.interSectionSpacing(theme: currentTheme, style: style)

        // 意味論メモ:
        // - `sectionProvider` クロージャは `self.visibleSections` を「呼ばれた時点で」参照する。
        //   `makeLayout(for:)` 呼び出し時点のスナップショットは捕捉しない。
        // - visibility 切替時は layout 自体を作り直さず、
        //   `collectionViewLayout.invalidateLayout()` で sectionProvider を再評価させる。
        // - クロージャに visible 列を焼き付けると、切替のたびに layout の作り直しが必要になり、
        //   `setCollectionViewLayout(_, animated: false)` の同期差し替えが
        //   全 Cell バウンド / 描画乱れを引き起こす。

        let layout = SectionBoxLayout(
            sectionProvider: { [weak self] sectionIdx, environment in
                let section = NSCollectionLayoutSection.list(using: listConfig, layoutEnvironment: environment)

                // classic（`.plain`）では `.list(using:)` が生成する section header / footer supplementary が
                // 既定でスクロール上端 / 下端に pin される。オリジナル AiForms 互換のためヘッダーもフッタも
                // 固定せず、Header / Footer の双方をコンテンツと共にスクロールアウトさせる。

                // 該当セクションを取得し、headerHeight に応じて Header supplementary を制御する。
                // `self.visibleSections` は最新の状態で参照する（クロージャ生成時点の
                // スナップショットを焼き付けない）。
                let currentVisibleSections = self?.visibleSections ?? []
                let modelSection: KsSettingsViewCore.Section? =
                    (sectionIdx < currentVisibleSections.count) ? currentVisibleSections[sectionIdx] : nil

                // Section 単位（Header・Cell の箱・Footer を一体とした表示単位）の水平方向の外側余白。
                //
                // 上下方向はここでは扱わない。`NSCollectionLayoutSection.contentInsets` の上下成分は
                // boundary supplementary item の「外」ではなく「内」（Header と item 群の間 / item 群と
                // Footer の間）に入るため、Section 単位の外側余白としては使えない。上下は
                // Section 間が `interSectionSpacing`、list 端が `UICollectionView.contentInset` で表す。
                let metrics = SectionBoxMetrics.resolve(theme: self?.currentTheme ?? Theme(), style: style)
                section.contentInsets = NSDirectionalEdgeInsets(
                    top: 0,
                    leading: metrics.margin.leading,
                    bottom: 0,
                    trailing: metrics.margin.trailing
                )
                // supplementary の水平位置は item 側の contentInsets で明示するため、section の
                // contentInsets を二重に適用させない。
                section.supplementariesFollowContentInsets = false

                // Modern の箱は Section 背景の decoration として宣言する。Cell を持たない Section で
                // 箱を出さない判断は layout 側の frame 補正が行う（ios/ADR-0003）。
                if style == .modern {
                    section.decorationItems = [
                        NSCollectionLayoutDecorationItem.background(
                            elementKind: SectionBoxLayout.decorationKind
                        )
                    ]
                }

                // Header / Footer の boundary supplementary item を再構成する。
                // - Header: 表示判定 (トグル && 内容あり) が偽なら削除。真なら headerHeight 解決に従う
                //   （正値 → .absolute、それ以外は .estimated(20)）
                // - Footer: 表示判定が偽なら削除、真なら .estimated(20)
                //
                // `.list(using:)` が返す既定の supplementary item は `.estimated(44)` 相当の大きな
                // heightDimension と既定の `contentInsets` を持ち、Header / Footer 周辺に Android と
                // 比べて過大な余白を生む。そのため、テキスト 1 行（~17pt）+ 上下マージン
                // （2pt × 2 = 4pt）= ~21pt を意図して `.estimated(20)` に縮め、`contentInsets` を
                // `.zero` に明示する。
                var rebuiltItems: [NSCollectionLayoutBoundarySupplementaryItem] = []
                for item in section.boundarySupplementaryItems {
                    if item.elementKind == UICollectionView.elementKindSectionHeader {
                        if let s = modelSection,
                           let newItem = Self.makeHeaderBoundaryItem(
                            for: s,
                            original: item,
                            theme: self?.currentTheme
                           ) {
                            newItem.pinToVisibleBounds = false
                            // Header は箱と水平位置を揃える（上下の余白は Header の外側で表す）。
                            newItem.contentInsets = NSDirectionalEdgeInsets(
                                top: 0,
                                leading: metrics.margin.leading,
                                bottom: 0,
                                trailing: metrics.margin.trailing
                            )
                            rebuiltItems.append(newItem)
                        }
                        // s == nil または header 非生成判定なら item を追加しない（生成しない）
                    } else if item.elementKind == UICollectionView.elementKindSectionFooter {
                        if let s = modelSection,
                           Self.shouldShowFooter(for: s) {
                            // Footer も Header と同じく `.estimated(20)` で再生成し、水平位置を箱に揃える。
                            let newFooter = NSCollectionLayoutBoundarySupplementaryItem(
                                layoutSize: NSCollectionLayoutSize(
                                    widthDimension: .fractionalWidth(1.0),
                                    heightDimension: .estimated(20)
                                ),
                                elementKind: item.elementKind,
                                alignment: item.alignment
                            )
                            newFooter.pinToVisibleBounds = false
                            newFooter.contentInsets = NSDirectionalEdgeInsets(
                                top: 0,
                                leading: metrics.margin.leading,
                                bottom: 0,
                                trailing: metrics.margin.trailing
                            )
                            _ = s
                            rebuiltItems.append(newFooter)
                        }
                        // footer 非表示なら item を追加しない
                    } else {
                        rebuiltItems.append(item)
                    }
                }
                section.boundarySupplementaryItems = rebuiltItems
                return section
            },
            configuration: configuration
        )
        layout.register(
            SectionBoxDecorationView.self,
            forDecorationViewOfKind: SectionBoxLayout.decorationKind
        )
        // layout は data source を直接参照しない。可視 Cell 数は visible projection から供給する
        // （呼ばれた時点の最新を読み、クロージャ生成時点のスナップショットを焼き付けない）。
        layout.cellCountInSection = { [weak self] sectionIdx in
            guard let self = self, sectionIdx < self.visibleSections.count else { return 0 }
            return self.visibleSections[sectionIdx].cells.count
        }
        layout.updateBoxAppearance(
            metrics: SectionBoxMetrics.resolve(theme: currentTheme, style: style),
            backgroundColor: currentTheme.cellBackgroundColor
        )
        return layout
    }

    /// 隣接 Section の間隔（前 Section の bottom 余白 + 次 Section の top 余白）。
    internal static func interSectionSpacing(theme: Theme, style: KsSettingsViewStyle) -> CGFloat {
        let margin = SectionBoxMetrics.resolve(theme: theme, style: style).margin
        return margin.top + margin.bottom
    }

    /// Section 単位の上下余白のうち、list の端に接する分を `UICollectionView.contentInset` へ反映する。
    ///
    /// 先頭 Section の top 余白と末尾 Section の bottom 余白は、隣の Section ではなく list の端に対して
    /// 取る。ただし Root Header / Footer がある側では、余白は Root Header と先頭 Section の間 /
    /// 末尾 Section と Root Footer の間、つまり Root Header / Footer の**内側**へ入る
    /// （その分は `takeRootAccessoryContentInsets(isFooter:)` が Root accessory の余白として持つ）。
    /// したがって Root Header / Footer がある側の scroll view inset は 0 にする。
    private func applyListEdgeMargin() {
        guard let cv = self.collectionView else { return }
        let margin = sectionUnitMargin()
        var inset = cv.contentInset
        inset.top = (rootHeader == nil) ? margin.top : 0
        inset.bottom = (rootFooter == nil) ? margin.bottom : 0
        cv.contentInset = inset
    }

    /// Section 単位余白の実効値。
    ///
    /// 余白は Section を包むためのものなので、可視 Section が 1 つも無いときは 0 とする
    /// （どの Section にも属さない余白を list 端や Root accessory の内側に残さない）。
    private func sectionUnitMargin() -> NSDirectionalEdgeInsets {
        guard !visibleSections.isEmpty else {
            return NSDirectionalEdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0)
        }
        return SectionBoxMetrics.resolve(theme: currentTheme, style: style).margin
    }

    /// Root Header / Footer の内側に持たせる Section 単位余白を解決し、適用済みの値として記録する。
    ///
    /// Root Header は下側（先頭 Section との間）に上余白を、Root Footer は上側（末尾 Section との間）に
    /// 下余白を持つ。Root accessory は自身の内容高さへ自動追従するため、この余白は accessory の
    /// content の padding として与えると Root accessory と Section の間隔になる。
    ///
    /// 記録した値は `refreshRootAccessoriesIfMarginChanged()` が「作り直しが要るか」を判定するために使う。
    private func takeRootAccessoryContentInsets(isFooter: Bool) -> UIEdgeInsets {
        let margin = sectionUnitMargin()
        appliedRootAccessoryMargin = margin
        return isFooter
            ? UIEdgeInsets(top: margin.bottom, left: 0, bottom: 0, right: 0)
            : UIEdgeInsets(top: 0, left: 0, bottom: margin.top, right: 0)
    }

    /// Section 単位余白の解決値が変わったときだけ Root Header / Footer を作り直す。
    ///
    /// Root accessory の再構成は `KsAnyView` の factory 再実行を伴い、View accessory が持つ内部状態
    /// （編集途中のテキスト・スクロール位置・first responder）を失わせる。構造や内容の Diff は
    /// Root accessory の余白を変えないため、無条件に作り直してはいけない。作り直しが要るのは
    /// 可視 Section が 0 件 ⇔ 非 0 件へ遷移したときと、Theme の `sectionMargin` が変わったときだけ。
    private func refreshRootAccessoriesIfMarginChanged() {
        let margin = sectionUnitMargin()
        guard appliedRootAccessoryMargin != margin else { return }
        if rootHeader != nil {
            refreshRootSupplementary(elementKind: Self.rootHeaderElementKind)
        }
        if rootFooter != nil {
            refreshRootSupplementary(elementKind: Self.rootFooterElementKind)
        }
        // Root accessory を持たない場合も、次に生成されるときの基準として記録しておく。
        appliedRootAccessoryMargin = margin
    }

    /// 現在の Theme から Section 装飾（箱の装飾値・Section 間隔・list 端余白）を layout へ反映する。
    ///
    /// 水平の余白は sectionProvider が呼び出しのたびに `currentTheme` から解決するため、
    /// ここでは decoration へ運ぶ装飾値と、section 間 / list 端の余白を差し替える。
    /// 再評価は呼び出し側の `invalidateLayout()` に任せる。
    private func refreshSectionBoxAppearance() {
        applyListEdgeMargin()
        guard let layout = collectionView?.collectionViewLayout as? SectionBoxLayout else { return }
        layout.updateBoxAppearance(
            metrics: SectionBoxMetrics.resolve(theme: currentTheme, style: style),
            backgroundColor: currentTheme.cellBackgroundColor
        )
        let spacing = Self.interSectionSpacing(theme: currentTheme, style: style)
        guard layout.configuration.interSectionSpacing != spacing else { return }
        // spacing の変更を layout に取り込ませるため、更新した configuration を代入し直す。
        let configuration = layout.configuration
        configuration.interSectionSpacing = spacing
        layout.configuration = configuration
    }

    /// Section の Header supplementary item を Header の表示判定と `headerHeight` に基づいて再生成する。
    ///
    /// まず「表示するか」を `shouldShowHeader(for:)`（トグル && 内容あり）で決め、表示する場合にだけ
    /// 高さを解決する（core/ADR-0023）。したがって `headerHeight` は存在する Header の高さを決めるだけで、
    /// Header の存在を作らない。
    ///
    /// - Header を表示しない → `nil`（supplementary 自体を生成しない）
    /// - `Section.headerHeight > 0` → `.absolute(headerHeight)` の固定高さで生成
    /// - `Section.headerHeight == -1` かつ `Theme.headerHeight > 0` → `.absolute(theme.headerHeight)`
    /// - いずれも正値でない → `.estimated(20)`（密度を Android と揃える）
    ///
    /// 固定高さの解決は accessory 種別 (text / view) を見ない（core/ADR-0021、両 OS 対称の公開契約）。
    ///
    /// `.list(using:)` が返す既定 supplementary item の `.estimated(44)` は Header テキスト 1 行
    /// （~17pt）+ 上下マージン（4pt 程度）に比べて過大で余分な余白の原因になる。そのため
    /// `.estimated(20)` まで縮めて、supplementary view（`UICollectionViewListCell` +
    /// `UIListContentConfiguration`）の上下マージン 2pt とラベル intrinsic 高さで表現される
    /// 実コンテンツ高さに揃える。
    ///
    /// 単体テストから `.absolute(headerHeight)` の適用を検証できるよう `internal static` で公開する
    /// （インスタンス状態に依存しない純粋関数のため `static` で問題ない）。
    internal static func makeHeaderBoundaryItem(
        for section: KsSettingsViewCore.Section,
        original: NSCollectionLayoutBoundarySupplementaryItem,
        theme: Theme? = nil
    ) -> NSCollectionLayoutBoundarySupplementaryItem? {
        // 存在判定が先。表示しない Header には高さを解決しない。
        guard shouldShowHeader(for: section) else { return nil }
        // Section ごとの `headerHeight > 0` → そのまま固定高さ
        if section.headerHeight > 0 {
            let item = NSCollectionLayoutBoundarySupplementaryItem(
                layoutSize: NSCollectionLayoutSize(
                    widthDimension: .fractionalWidth(1.0),
                    heightDimension: .absolute(CGFloat(section.headerHeight))
                ),
                elementKind: original.elementKind,
                alignment: original.alignment
            )
            return item
        }
        // headerHeight == -1（自動）
        // Section ごとの `Section.headerHeight` が `-1.0` のときは `Theme.headerHeight` を採用する。
        if let theme = theme, theme.headerHeight > 0 {
            return NSCollectionLayoutBoundarySupplementaryItem(
                layoutSize: NSCollectionLayoutSize(
                    widthDimension: .fractionalWidth(1.0),
                    heightDimension: .absolute(CGFloat(theme.headerHeight))
                ),
                elementKind: original.elementKind,
                alignment: original.alignment
            )
        }
        // 固定高さの指定が無い → .estimated(20) で再生成（システム既定の .estimated(44) より小さく取る）
        return NSCollectionLayoutBoundarySupplementaryItem(
            layoutSize: NSCollectionLayoutSize(
                widthDimension: .fractionalWidth(1.0),
                heightDimension: .estimated(20)
            ),
            elementKind: original.elementKind,
            alignment: original.alignment
        )
    }

    /// accessory に内容があるかを判定する。
    ///
    /// 「内容の不在」は **nil または空 text** とし、Header / Footer で共通の判定とする
    /// （core/ADR-0023）。view accessory は中身が空でも常に内容ありとして扱う。
    internal static func hasAccessoryContent(_ accessory: SectionAccessory?) -> Bool {
        guard let accessory = accessory else { return false }
        if case .text(let s) = accessory, s.isEmpty { return false }
        return true
    }

    /// Header supplementary を生成するか判定する。
    ///
    /// 判定は「表示トグル && 内容あり」の AND 合成（core/ADR-0023）。内容の無い Header で
    /// supplementary を生成すると、内容がないまま高さだけが残るため生成しない。
    internal static func shouldShowHeader(for section: KsSettingsViewCore.Section) -> Bool {
        return section.isHeaderVisible && hasAccessoryContent(section.header)
    }

    /// Footer supplementary を生成するか判定する。判定規則は `shouldShowHeader(for:)` と対称。
    internal static func shouldShowFooter(for section: KsSettingsViewCore.Section) -> Bool {
        return section.isFooterVisible && hasAccessoryContent(section.footer)
    }

    /// Cell の位置（セクション最初 / 最後 / 中間）に応じて separator を切り替える。
    ///
    /// 罫線インセットは `AiForms.Maui.SettingsView` に揃え、アイコン有無に関わらず
    /// **常に固定インセット 16pt**（標準左マージン）とする。
    /// Classic ではセクション境界（最初の Cell の上端、最後の Cell の下端）を端から端
    /// （`leading = 0`）で描画する。
    ///
    /// Modern では箱の縁が Section の区切りを兼ねるため、箱の上端・下端に罫線を描かず、
    /// Section 内の中間 Cell の下端だけを描く。中間罫線は箱の内側 leading / trailing 端から
    /// 同量だけインセットし、箱が罫線で分断されて見えないようにする。
    internal func separatorConfiguration(
        for indexPath: IndexPath,
        base: UIListSeparatorConfiguration
    ) -> UIListSeparatorConfiguration {
        var config = base
        // separator の色は現在の Theme から解決する。UIKit 既定の色に任せると Theme の指定が
        // 反映されないため、可視性・インセットの判定より前に無条件で適用する。
        config.color = currentTheme.separatorColor
        // 既定は非可視に倒し、必要な箇所のみ明示的に可視化する（UIKit の .automatic に任せると
        // セクション境界 / インセット位置の挙動がランタイム条件に依存する）。
        config.topSeparatorVisibility = .hidden
        config.bottomSeparatorVisibility = .hidden

        // セクション情報の取得（visible projection ベース）
        guard indexPath.section < visibleSections.count else { return config }
        let section = visibleSections[indexPath.section]
        let cellCount = section.cells.count
        guard cellCount > 0, indexPath.item < cellCount else { return config }

        let isFirst = (indexPath.item == 0)
        let isLast = (indexPath.item == cellCount - 1)

        // 罫線インセット: アイコン有無に関わらず固定 16pt（AiForms オリジナルに揃え）。
        let titleLeading = titleLeadingPosition(for: section.cells[indexPath.item])

        if style == .modern {
            // 箱の上端 / 下端には罫線を描かない（縁が区切りを兼ねる）。
            guard !isLast else { return config }
            let metrics = SectionBoxMetrics.resolve(theme: currentTheme, style: style)
            // 基準は箱の内側の端。ボーダーがあればその内側から同じインセットを取る。
            let inset = titleLeading + metrics.borderWidth
            config.bottomSeparatorVisibility = .visible
            config.bottomSeparatorInsets = NSDirectionalEdgeInsets(
                top: 0, leading: inset, bottom: 0, trailing: inset
            )
            return config
        }

        if isFirst {
            // セクション最初: 上罫線は端から端（アイコン下も途切れない）
            config.topSeparatorVisibility = .visible
            config.topSeparatorInsets = NSDirectionalEdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0)
        }
        if isLast {
            // セクション最後: 下罫線は端から端（アイコン下も途切れない）
            config.bottomSeparatorVisibility = .visible
            config.bottomSeparatorInsets = NSDirectionalEdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0)
        } else {
            // セクション内中間 Cell: 下罫線は標準左マージン（16pt）からインセット
            config.bottomSeparatorVisibility = .visible
            config.bottomSeparatorInsets = NSDirectionalEdgeInsets(top: 0, leading: titleLeading, bottom: 0, trailing: 0)
        }
        return config
    }

    /// Cell 間の中間 separator に適用する標準左マージン（pt）を返す。
    ///
    /// AiForms オリジナルではアイコン有無に関わらず罫線インセットが標準左マージン（16pt）で
    /// 揃うため、**常に固定値 16pt** を返す。`cell` 引数は Cell タイプ別の個別調整が必要に
    /// なった場合の拡張余地として残してある。
    internal func titleLeadingPosition(for cell: any KsCell) -> CGFloat {
        // アイコン有無に関わらず標準左マージン 16pt 固定（AiForms オリジナルに揃え）。
        let leadingMargin: CGFloat = 16
        _ = cell
        return leadingMargin
    }

    /// `KsSettingsViewStyle` を `UICollectionLayoutListConfiguration.Appearance` に変換する。
    ///
    /// Modern の箱（余白・角丸・ボーダー）はライブラリが自前で描くため、両 style とも `.plain` を使う。
    /// `.insetGrouped` は 4 属性の制御 API を持たず採用しない（ios/ADR-0003）。
    internal static func appearance(for style: KsSettingsViewStyle) -> UICollectionLayoutListConfiguration.Appearance {
        switch style {
        case .classic, .modern: return .plain
        }
    }

    private func appearance(for style: KsSettingsViewStyle) -> UICollectionLayoutListConfiguration.Appearance {
        return KsSettingsViewController.appearance(for: style)
    }

    /// 表示中の全 Cell を破棄せず再構成する（identity と内容は変えない）。
    private func reconfigureVisibleCells() {
        guard let dataSource = self.dataSource else { return }
        var snapshot = dataSource.snapshot()
        guard !snapshot.itemIdentifiers.isEmpty else { return }
        if #available(iOS 15.0, *) {
            snapshot.reconfigureItems(snapshot.itemIdentifiers)
        } else {
            snapshot.reloadItems(snapshot.itemIdentifiers)
        }
        dataSource.apply(snapshot, animatingDifferences: false)
    }

    /// 内部レイアウトを再構築する。`style` 変更時 / Root H/F の出現・消失時に呼ばれる。
    private func rebuildLayout() {
        guard let cv = self.collectionView else { return }
        let newLayout = makeLayout(for: style)
        cv.setCollectionViewLayout(newLayout, animated: false)
        applyListEdgeMargin()
        applyFullSnapshot(root: root, animated: false)
    }

    // MARK: - DataSource 構築

    /// テスト・サンプルから取得しやすいよう、内部 UICollectionView を返すアクセサ。
    internal var internalCollectionView: UICollectionView {
        return self.collectionView
    }

    /// テストから DataSource を覗くためのアクセサ
    internal var internalDataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>? {
        return self.dataSource
    }

    private func configureDataSource() {
        let dataSource = UICollectionViewDiffableDataSource<UUID, KsCellID>(
            collectionView: self.collectionView
        ) { [weak self] collectionView, indexPath, itemID in
            guard let self = self else { return UICollectionViewCell() }
            return self.cellProvider(collectionView: collectionView, indexPath: indexPath, itemID: itemID)
        }

        dataSource.supplementaryViewProvider = { [weak self] collectionView, kind, indexPath in
            guard let self = self else { return nil }
            return self.supplementaryProvider(collectionView: collectionView, kind: kind, indexPath: indexPath)
        }

        self.dataSource = dataSource

        // タップ通知のために自身を `UICollectionViewDelegate` として登録する。
        // 基本 Cell（CommandCell / ButtonCell / CheckboxCell / RadioCell / SimpleCheckCell）の
        // `tapHandler` を didSelectItemAt から呼び出して通知を発火する。
        self.collectionView.delegate = self
    }

    // MARK: - Cell Provider

    /// 既に register 済みの Cell クラスを記録する（重複 register 抑止）
    private var registeredCellIdentifiers: Set<String> = []
    /// 既に register 済みの Supplementary kind+クラスを記録する
    private var registeredSupplementaryKinds: Set<String> = []

    private func reuseIdentifier(for type: UICollectionViewCell.Type) -> String {
        return String(describing: type)
    }

    private func registerCellTypeIfNeeded(_ type: UICollectionViewCell.Type) -> String {
        let identifier = reuseIdentifier(for: type)
        if !registeredCellIdentifiers.contains(identifier) {
            self.collectionView.register(type, forCellWithReuseIdentifier: identifier)
            registeredCellIdentifiers.insert(identifier)
        }
        return identifier
    }

    private func cellProvider(
        collectionView: UICollectionView,
        indexPath: IndexPath,
        itemID: KsCellID
    ) -> UICollectionViewCell {
        guard let cell = self.cellIndex[itemID] else {
            return placeholderCell(in: collectionView, indexPath: indexPath)
        }

        guard let rendererType = registry.resolveRendererType(for: cell) else {
            assertionFailure("KsCellRegistry: no renderer registered for \(type(of: cell))")
            return placeholderCell(in: collectionView, indexPath: indexPath)
        }

        let identifier = registerCellTypeIfNeeded(rendererType)
        let dequeued = collectionView.dequeueReusableCell(withReuseIdentifier: identifier, for: indexPath)
        if let renderer = dequeued as? KsCellRenderer {
            renderer.render(cell: cell, theme: currentTheme)
        } else {
            assertionFailure("Cell type \(rendererType) does not conform to KsCellRenderer")
        }
        // render が背景構成を作り直した後に適用する（render 側は
        // `defaultBackgroundConfiguration()` から組み直すため、順序を入れ替えると clip が消える）。
        applySectionBoxClip(to: dequeued, at: indexPath)
        return dequeued
    }

    /// Modern の箱に収めるための clip を Cell へ適用する。Classic では clip を外す。
    ///
    /// 箱の実寸と Cell の位置は layout から解決する。角丸の弧を箱と共有させるためで、Cell 単体の
    /// 寸法だけで半径を切り詰めると、大きな `sectionCornerRadius` で Cell の背景が箱の角から
    /// はみ出す。
    private func applySectionBoxClip(to cell: UICollectionViewCell, at indexPath: IndexPath) {
        guard let listCell = cell as? UICollectionViewListCell else { return }
        let cellCount = (indexPath.section < visibleSections.count)
            ? visibleSections[indexPath.section].cells.count
            : 0
        let layout = collectionView?.collectionViewLayout as? SectionBoxLayout
        let clip = SectionBoxCellClip.resolve(
            metrics: SectionBoxMetrics.resolve(theme: currentTheme, style: style),
            boxFrame: layout?.sectionBoxFrame(inSection: indexPath.section),
            cellFrame: layout?.itemFrame(at: indexPath),
            itemIndex: indexPath.item,
            cellCount: cellCount
        )
        KsCellViewSupport.applySectionBoxClip(listCell, clip: clip)
    }

    /// 表示中の全 Cell の箱 clip を現在の visible projection から解決し直す。
    ///
    /// clip は Section 内の位置（先頭 / 末尾 / 中間）に依存するため、`SettingsRootDiff` で Section の
    /// 先頭 / 末尾が入れ替わると、内容が変わっていない隣接 Cell の clip も古くなる。dequeue と
    /// reconfigure だけに任せると、そうした Cell は画面外へ出て戻るまで古い形状のまま残る。
    private func refreshVisibleSectionBoxClips() {
        guard let cv = self.collectionView else { return }
        for cell in cv.visibleCells {
            guard let indexPath = cv.indexPath(for: cell) else { continue }
            applySectionBoxClip(to: cell, at: indexPath)
        }
    }

    /// 構造・可視性・Theme の変化のあとに、Section 単位余白と箱 clip を解決し直す。
    ///
    /// 余白は可視 Section の有無で変わり、clip は Section 内の位置で変わるため、visible projection が
    /// 動く経路ではまとめて再評価する。
    private func refreshSectionUnitPresentation() {
        applyListEdgeMargin()
        refreshRootAccessoriesIfMarginChanged()
        refreshVisibleSectionBoxClips()
    }

    private func placeholderCell(in collectionView: UICollectionView, indexPath: IndexPath) -> UICollectionViewCell {
        let identifier = "KsPlaceholderCell"
        if !registeredCellIdentifiers.contains(identifier) {
            collectionView.register(UICollectionViewListCell.self, forCellWithReuseIdentifier: identifier)
            registeredCellIdentifiers.insert(identifier)
        }
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: identifier, for: indexPath)
        cell.contentView.backgroundColor = .systemGray5
        return cell
    }

    // MARK: - Supplementary Provider

    /// Section / Root の supplementary view を返す。
    private func supplementaryProvider(
        collectionView: UICollectionView,
        kind: String,
        indexPath: IndexPath
    ) -> UICollectionReusableView? {
        switch kind {
        case UICollectionView.elementKindSectionHeader:
            return sectionAccessoryView(
                collectionView: collectionView,
                kind: kind,
                indexPath: indexPath,
                accessoryKeyPath: \.header
            )
        case UICollectionView.elementKindSectionFooter:
            return sectionAccessoryView(
                collectionView: collectionView,
                kind: kind,
                indexPath: indexPath,
                accessoryKeyPath: \.footer
            )
        case Self.rootHeaderElementKind:
            return rootAccessoryView(
                collectionView: collectionView,
                kind: kind,
                indexPath: indexPath,
                accessory: rootHeader
            )
        case Self.rootFooterElementKind:
            return rootAccessoryView(
                collectionView: collectionView,
                kind: kind,
                indexPath: indexPath,
                accessory: rootFooter
            )
        default:
            return nil
        }
    }

    /// Section ヘッダ／フッタ supplementary view を返す。
    private func sectionAccessoryView(
        collectionView: UICollectionView,
        kind: String,
        indexPath: IndexPath,
        accessoryKeyPath: KeyPath<KsSettingsViewCore.Section, SectionAccessory?>
    ) -> UICollectionReusableView? {
        let accessory: SectionAccessory?
        // 「visible projection の二重管理」: indexPath ベースの supplementary view 経路は
        // visible projection を参照する（hidden Section は projection から除外済み）。
        if indexPath.section < visibleSections.count {
            accessory = visibleSections[indexPath.section][keyPath: accessoryKeyPath]
        } else {
            accessory = nil
        }
        // Header / Footer の判定で文字色を切り替える。
        // Header: headerTextColor、Footer: footerTextColor（既定値 ≒ #6D6D72 / 固定 RGB。AiForms オリジナル `UIColor.Gray` 準拠）。
        let isFooter = (kind == UICollectionView.elementKindSectionFooter)
        return makeAccessoryListCell(
            collectionView: collectionView,
            kind: kind,
            indexPath: indexPath,
            accessoryText: accessory.flatMap(textValue),
            accessoryView: accessory.flatMap(viewValue),
            textColor: isFooter
                ? currentTheme.footerTextColor
                : currentTheme.headerTextColor,
            // Header テキストは下端揃え（AiForms `TextHeaderView.SetVerticalAlignment(LayoutAlignment.End)` 既定）、
            // Footer テキストは上端揃え（AiForms `TextFooterView` 既定の TopAnchor 制約挙動）。
            verticalAlignment: isFooter ? .top : .bottom,
            // `Theme.headerFont` / `Theme.footerFont` を描画に反映する。
            // headerFontSize / footerFontSize > 0 のとき size を上書きする。
            font: isFooter
                ? Self.resolveFooterFont(theme: currentTheme)
                : Self.resolveHeaderFont(theme: currentTheme)
        )
    }

    /// Root ヘッダ／フッタ supplementary view を返す。
    private func rootAccessoryView(
        collectionView: UICollectionView,
        kind: String,
        indexPath: IndexPath,
        accessory: RootAccessory?
    ) -> UICollectionReusableView? {
        // Root H/F は headerTextColor を流用する（既存挙動を維持）
        // Root Header / Footer も Section と同じ垂直揃え（Header=下端、Footer=上端）を採用する。
        let isFooter = (kind == Self.rootFooterElementKind)
        return makeAccessoryListCell(
            collectionView: collectionView,
            kind: kind,
            indexPath: indexPath,
            accessoryText: accessory.flatMap(rootTextValue),
            accessoryView: accessory.flatMap(rootViewValue),
            textColor: currentTheme.headerTextColor,
            verticalAlignment: isFooter ? .top : .bottom,
            // Root も Section と同じく Theme.headerFont / footerFont を反映する。
            font: isFooter
                ? Self.resolveFooterFont(theme: currentTheme)
                : Self.resolveHeaderFont(theme: currentTheme),
            // Section 単位の余白は Root Header / Footer の内側に置く。
            extraContentInsets: takeRootAccessoryContentInsets(isFooter: isFooter)
        )
    }

    /// `theme.headerFont` と `theme.headerFontSize` から最終的な Header 用 `UIFont` を解決する。
    ///
    /// `EffectiveStyle.effectiveHeaderFont(theme:)` への薄いラッパ。
    /// 旧コードからの呼び出し名互換のためここに残し、責務は `EffectiveStyle` 側で一本化する。
    internal static func resolveHeaderFont(theme: Theme) -> UIFont {
        return EffectiveStyle.effectiveHeaderFont(theme: theme)
    }

    /// `theme.footerFont` と `theme.footerFontSize` から最終的な Footer 用 `UIFont` を解決する。
    ///
    /// `EffectiveStyle.effectiveFooterFont(theme:)` への薄いラッパ。
    internal static func resolveFooterFont(theme: Theme) -> UIFont {
        return EffectiveStyle.effectiveFooterFont(theme: theme)
    }

    private func makeAccessoryListCell(
        collectionView: UICollectionView,
        kind: String,
        indexPath: IndexPath,
        accessoryText: String?,
        accessoryView: KsAnyView?,
        textColor: UIColor,
        verticalAlignment: AccessoryVerticalAlignment = .center,
        font: UIFont? = nil,
        extraContentInsets: UIEdgeInsets = .zero
    ) -> UICollectionReusableView? {
        // Section / Root の Header / Footer の supplementary view は `UICollectionViewListCell`
        // で統一する（テキスト accessory / SwiftUI view / UIKit view すべて同一経路）。
        // accessory 専用の再利用 View 型は設けない。個別 Cell の `cellHeight` 反映は
        // `KsListCellBase` の `preferredLayoutAttributesFitting` が担う。
        let identifier = "KsAccessoryListCell.\(kind)"
        let registerKey = identifier
        if !registeredSupplementaryKinds.contains(registerKey) {
            collectionView.register(
                UICollectionViewListCell.self,
                forSupplementaryViewOfKind: kind,
                withReuseIdentifier: identifier
            )
            registeredSupplementaryKinds.insert(registerKey)
        }
        let view = collectionView.dequeueReusableSupplementaryView(
            ofKind: kind,
            withReuseIdentifier: identifier,
            for: indexPath
        )
        guard let listCell = view as? UICollectionViewListCell else {
            return view
        }
        applyAccessoryToListCell(
            listCell,
            accessoryText: accessoryText,
            accessoryView: accessoryView,
            textColor: textColor,
            verticalAlignment: verticalAlignment,
            font: font,
            extraContentInsets: extraContentInsets,
            textGap: Self.textGap(forElementKind: kind)
        )
        return listCell
    }

    /// Header / Footer の垂直配置を表す内部 enum。
    ///
    /// AiForms.Maui.SettingsView オリジナル `Platforms/iOS/TextHeaderView.cs` の
    /// `SetVerticalAlignment(LayoutAlignment)` の意味論に揃え、Header は `.bottom`、
    /// Footer は `.top` で利用する。`.center` は Root accessory 等の中央揃え用に残置。
    internal enum AccessoryVerticalAlignment {
        case top
        case center
        case bottom
    }

    // MARK: - Accessory ケース判定ヘルパ

    private func textValue(_ acc: SectionAccessory) -> String? {
        if case let .text(s) = acc { return s }
        return nil
    }

    private func viewValue(_ acc: SectionAccessory) -> KsAnyView? {
        if case let .view(v) = acc { return v }
        return nil
    }

    private func rootTextValue(_ acc: RootAccessory) -> String? {
        if case let .text(s) = acc { return s }
        return nil
    }

    private func rootViewValue(_ acc: RootAccessory) -> KsAnyView? {
        if case let .view(v) = acc { return v }
        return nil
    }

    // MARK: - Diff 適用

    /// 受け取った `SettingsRootDiff` を `UICollectionViewDiffableDataSource` の部分操作に変換する。
    ///
    /// - Parameter diff: 適用する Diff
    public func applyDiff(_ diff: SettingsRootDiff) {
        guard let dataSource = self.dataSource else {
            // viewDidLoad 前の呼び出しは内部 root のみ更新し、UI 反映は viewDidLoad 内で行う。
            updateInternalRoot(for: diff)
            return
        }

        switch diff {
        case .full(let newRoot):
            applyFullSnapshot(root: newRoot, animated: true)

        case let .insertSection(at: index, section: section):
            applyInsertSection(dataSource: dataSource, index: index, section: section)

        case .removeSection(let sectionID):
            applyRemoveSection(dataSource: dataSource, sectionID: sectionID)

        case let .moveSection(from: from, to: to):
            applyMoveSection(dataSource: dataSource, from: from, to: to)

        case let .replaceSection(sectionID: sectionID, new: newSection):
            applyReplaceSection(dataSource: dataSource, sectionID: sectionID, new: newSection)

        case let .insertCell(sectionID: sectionID, at: index, cell: cell):
            applyInsertCell(dataSource: dataSource, sectionID: sectionID, index: index, cell: cell)

        case .removeCell(let cellID):
            applyRemoveCell(dataSource: dataSource, cellID: cellID)

        case let .replaceCell(cellID: cellID, new: newCell):
            applyReplaceCell(dataSource: dataSource, cellID: cellID, new: newCell)

        case let .moveCell(cellID: cellID, to: index):
            applyMoveCell(dataSource: dataSource, cellID: cellID, to: index)

        case let .updateAccessory(target: target, accessory: accessory):
            applyUpdateAccessory(target: target, accessory: accessory)
        }

        // 構造・可視性が動くと Section 内の先頭 / 末尾と可視 Section 数が変わる。
        // 内容が変わらない既存 Cell / Root accessory にも影響するため、まとめて解決し直す。
        refreshSectionUnitPresentation()
    }

    /// `viewDidLoad` 前に Diff を受け取った場合の内部 root 補正。
    /// `Full` のみ root 自体を差し替える（他ケースは初期 root 構築前なので無視）。
    private func updateInternalRoot(for diff: SettingsRootDiff) {
        if case .full(let newRoot) = diff {
            self.root = newRoot
        }
    }

    // MARK: - 内容更新バッチ

    /// 複数 Cell の内容更新を 1 回の部分更新でまとめて反映する。
    ///
    /// 更新後の内容は接続中の Store の現在状態から読み直す（Store 未接続なら何もしない）。
    /// 構造同期（snapshot の item 集合・順序）は変えず、対象行の内容のみを再構成する。
    /// 対象が hidden で snapshot に載っていない場合は、model の更新だけを行う自然な no-op となる。
    ///
    /// - Parameter cellIDs: 内容が更新された Cell の ID 群（適用順）
    internal func applyContentUpdateBatch(_ cellIDs: [KsCellID]) {
        guard let store = connectedStore else { return }

        // 具象型の変化判定に使う更新前の Cell を退避する。model を更新すると `cellIndex` が
        // 作り直されて旧 Cell を引けなくなるため、退避は model 更新より **先に** 行う。
        var oldCells: [KsCellID: any KsCell] = [:]
        for cellID in cellIDs {
            oldCells[cellID] = cellIndex[cellID]
        }

        // model（hidden を含むフル状態）と visible projection を更新後の状態へ揃える。
        self.root = store.root
        rebuildModelIndexes()
        rebuildVisibleProjection()

        guard let dataSource = self.dataSource else { return }
        var snapshot = dataSource.snapshot()

        // snapshot に載っている（= visible な）Cell だけを、重複を除いて再構成対象にする。
        let visibleItems = Set(snapshot.itemIdentifiers)
        var targets: [KsCellID] = []
        var seen: Set<KsCellID> = []
        for cellID in cellIDs {
            guard visibleItems.contains(cellID), seen.insert(cellID).inserted else { continue }
            targets.append(cellID)
        }
        guard !targets.isEmpty else { return }

        // 複数行を同時に差し替えるため、行ごとの差分アニメーションは走らせずに 1 回で反映する。
        if #available(iOS 15.0, *) {
            // 具象型が変われば Renderer も変わるため、同一 Native cell の再構成では反映できない。
            // 該当行だけ Native cell を交換する reload へ振り分ける。
            var reconfigureTargets: [KsCellID] = []
            var reloadTargets: [KsCellID] = []
            for cellID in targets {
                if let oldCell = oldCells[cellID],
                   let newCell = cellIndex[cellID],
                   type(of: oldCell) != type(of: newCell) {
                    reloadTargets.append(cellID)
                } else {
                    reconfigureTargets.append(cellID)
                }
            }
            if !reconfigureTargets.isEmpty {
                snapshot.reconfigureItems(reconfigureTargets)
            }
            if !reloadTargets.isEmpty {
                snapshot.reloadItems(reloadTargets)
            }
        } else {
            snapshot.reloadItems(targets)
        }
        dataSource.apply(snapshot, animatingDifferences: false)
    }

    /// `root.sections` から `sectionIndex` / `cellIndex` を作り直す。
    ///
    /// index は hidden を含む model 全体を保持する。部分更新の経路が「対象 Cell が hidden か」を
    /// 判定する際に、visible projection に出ない Cell の値も必要になるためである。
    private func rebuildModelIndexes() {
        var newSectionIndex: [UUID: KsSettingsViewCore.Section] = [:]
        var newCellIndex: [KsCellID: any KsCell] = [:]
        for section in root.sections {
            newSectionIndex[section.id] = section
            for cell in section.cells {
                newCellIndex[KsCellID(cell: cell)] = cell
            }
        }
        self.sectionIndex = newSectionIndex
        self.cellIndex = newCellIndex
    }

    // MARK: - Diff: Full

    /// model 全体から visible projection と snapshot を作り直し、data source へ適用する。
    ///
    /// 構造（item 集合・順序）の反映に加えて、旧 / 新 visible projection の双方に存在し内容が
    /// 変わった Cell へ内容の再適用を重ねる（対象の選定は `FullSnapshotContentTargets`）。
    ///
    /// - Parameters:
    ///   - root: 適用する新しい model 全体（hidden を含む）
    ///   - animated: 差分適用をアニメーションさせるか
    ///   - forceReloadSectionIDs: header / footer の等価比較に依らず supplementary の
    ///     再構成を強制する Section ID 群。view 形式 accessory はケース一致のみで等価と扱われ
    ///     中身の変化を検出できないため、呼び出し元が置換の意図を明示するのに使う。
    private func applyFullSnapshot(
        root: SettingsRoot,
        animated: Bool,
        forceReloadSectionIDs: Set<UUID> = []
    ) {
        guard let dataSource = self.dataSource else {
            self.root = root
            rebuildVisibleProjection()
            return
        }
        // 旧 visible projection を控える。後段の「header / footer が変化した Section の reload 指定」で
        // 新 projection との比較対象に使う（hidden Section は projection から除外済み）。
        let oldVisible = self.visibleSections
        let newVisible = Self.computeVisibleSections(from: root.sections)
        self.root = root
        self.visibleSections = newVisible
        // layout 自体は作り直さない（`setCollectionViewLayout` を呼ばない）。
        // sectionProvider が `self.visibleSections` を直接参照しているため、`invalidateLayout()` で
        // 再評価される。layout の同期差し替えに伴う「全 Cell バウンド + 描画乱れ」glitch を回避する。
        // `dataSource.apply(...)` で section の insert/delete があれば自然に sectionProvider が呼ばれるので
        // invalidate は不要だが、保険として apply の completion で 1 度だけ呼ぶ運用にする。

        // Theme.backgroundColor を反映
        applyBackgroundColor(theme: currentTheme)

        rebuildModelIndexes()

        // 「visible projection の二重管理」: snapshot は **visible projection のみ** で構築する
        // （`Section.isVisible == false` の Section、`VisibilityAware.isVisible == false` の Cell は除外）。
        var snapshot = NSDiffableDataSourceSnapshot<UUID, KsCellID>()
        for section in newVisible {
            snapshot.appendSections([section.id])
            let itemIDs: [KsCellID] = section.cells.map { KsCellID(cell: $0) }
            snapshot.appendItems(itemIDs, toSection: section.id)
        }

        // Section identity が同じままだと diffable data source は supplementary view を
        // 再要求しない。旧 / 新 visible projection で header / footer が変化した Section は
        // reload 指定して、表示中の supplementary の再構成を強制する。
        let oldByID = Dictionary(oldVisible.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        var reloadIDs: [UUID] = []
        for section in newVisible {
            guard let oldSection = oldByID[section.id] else { continue }
            let accessoryChanged = oldSection.header != section.header
                || oldSection.footer != section.footer
            if accessoryChanged || forceReloadSectionIDs.contains(section.id) {
                reloadIDs.append(section.id)
            }
        }
        if !reloadIDs.isEmpty {
            snapshot.reloadSections(reloadIDs)
        }

        // snapshot の item 識別子は内容を含まないため、同一 ID のまま内容が変わった Cell は
        // 構造反映だけでは古い表示のまま残る。旧 / 新 visible projection を突き合わせて
        // 対象を洗い出し、同じ apply の中で内容を再適用する。対象が空でも構造反映は必ず行う。
        let contentTargets = FullSnapshotContentTargets.compute(
            oldVisible: oldVisible,
            newVisible: newVisible,
            reloadSectionIDs: Set(reloadIDs)
        )
        if !contentTargets.reconfigure.isEmpty {
            if #available(iOS 15.0, *) {
                snapshot.reconfigureItems(contentTargets.reconfigure)
            } else {
                snapshot.reloadItems(contentTargets.reconfigure)
            }
        }
        if !contentTargets.reload.isEmpty {
            snapshot.reloadItems(contentTargets.reload)
        }

        dataSource.apply(snapshot, animatingDifferences: animated) { [weak self] in
            // 案 A 適用後: sectionProvider は `self.visibleSections` を最新で参照するが、
            // 「section 構造は不変 + section 内部の header/footer 構造のみ変化」というケースでは
            // sectionProvider が自動再評価されないため、完了後に invalidate して再評価を促す。
            // 副作用は layout キャッシュの再計算のみで、Diff の差分情報は失われない。
            self?.collectionView?.collectionViewLayout.invalidateLayout()
            // 箱 clip と Section 単位余白は再構築後の visible projection から解決し直す。
            self?.refreshSectionUnitPresentation()
        }
    }

    // MARK: - Diff: Section 操作

    private func applyInsertSection(
        dataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>,
        index: Int,
        section: KsSettingsViewCore.Section
    ) {
        // 「部分 Diff の index 規約」: `index` は **model 配列基準（hidden 含む）** で解釈する。
        var sections = root.sections
        let clampedModel = min(max(0, index), sections.count)
        sections.insert(section, at: clampedModel)
        self.root = SettingsRoot(sections: sections)

        // model 側 index を更新
        sectionIndex[section.id] = section
        for cell in section.cells {
            cellIndex[KsCellID(cell: cell)] = cell
        }

        // 新 visible projection 算出。
        // 案 A 適用後: layout 自体は作り直さない。sectionProvider が `self.visibleSections` を
        // 直接参照するため、apply の completion で `invalidateLayout()` を呼べば追従する。
        let newVisible = Self.computeVisibleSections(from: self.root.sections)
        self.visibleSections = newVisible

        // 挿入対象が hidden の場合は snapshot 操作を行わない（model のみ更新）。
        guard section.isVisible else {
            // Cell 単位の visibility 影響もここに含まれるが、Section 全体 hidden なら snapshot は無関係。
            return
        }

        var snapshot = dataSource.snapshot()
        // 「visible projection 基準の挿入位置」を算出: visible projection 上で当該 section.id の位置を探す。
        let visibleSectionIDs = newVisible.map { $0.id }
        guard let visibleInsertIdx = visibleSectionIDs.firstIndex(of: section.id) else { return }
        let existingSectionIDs = snapshot.sectionIdentifiers
        if visibleInsertIdx >= existingSectionIDs.count {
            snapshot.appendSections([section.id])
        } else {
            let before = existingSectionIDs[visibleInsertIdx]
            snapshot.insertSections([section.id], beforeSection: before)
        }
        // 挿入する Cell は visible のもののみ
        let visibleCells = section.cells.filter { ($0 as? VisibilityAware)?.isVisible ?? true }
        let itemIDs = visibleCells.map { KsCellID(cell: $0) }
        snapshot.appendItems(itemIDs, toSection: section.id)

        dataSource.apply(snapshot, animatingDifferences: true) { [weak self] in
            self?.collectionView?.collectionViewLayout.invalidateLayout()
        }
    }

    private func applyRemoveSection(
        dataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>,
        sectionID: UUID
    ) {
        guard let section = sectionIndex[sectionID] else {
            reportMissingID(message: "removeSection: sectionID \(sectionID) not found")
            return
        }
        let wasVisible = section.isVisible

        // 内部 index から消す
        for cell in section.cells {
            cellIndex.removeValue(forKey: KsCellID(cell: cell))
        }
        sectionIndex.removeValue(forKey: sectionID)

        // root state を同期
        var sections = root.sections
        sections.removeAll { $0.id == sectionID }
        self.root = SettingsRoot(sections: sections)

        // visible projection 更新（案 A 適用後: layout 同期差し替えなし）
        let newVisible = Self.computeVisibleSections(from: self.root.sections)
        self.visibleSections = newVisible

        // 削除対象が hidden（=元から projection に居ない）場合は snapshot 操作不要。
        guard wasVisible else { return }

        var snapshot = dataSource.snapshot()
        if snapshot.sectionIdentifiers.contains(sectionID) {
            snapshot.deleteSections([sectionID])
        }
        dataSource.apply(snapshot, animatingDifferences: true) { [weak self] in
            self?.collectionView?.collectionViewLayout.invalidateLayout()
        }
    }

    private func applyMoveSection(
        dataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>,
        from: Int,
        to: Int
    ) {
        // 「部分 Diff の index 規約」: `from` / `to` は **model 配列基準（hidden 含む）** で解釈。
        var sections = root.sections
        guard sections.indices.contains(from) else {
            reportMissingID(message: "moveSection: from index \(from) out of bounds (count: \(sections.count))")
            return
        }
        let movedSection = sections.remove(at: from)
        let clampedTo = min(max(0, to), sections.count)
        sections.insert(movedSection, at: clampedTo)
        self.root = SettingsRoot(sections: sections)

        // visible projection 更新（案 A 適用後: layout 同期差し替えなし）
        let newVisible = Self.computeVisibleSections(from: self.root.sections)
        self.visibleSections = newVisible

        // 移動対象が hidden の場合は snapshot 操作不要（model のみ更新）。
        guard movedSection.isVisible else { return }

        // visible projection 上での新 / 旧 index を算出し、snapshot に反映する。
        var snapshot = dataSource.snapshot()
        let existingSectionIDs = snapshot.sectionIdentifiers
        guard existingSectionIDs.contains(movedSection.id) else { return }

        // visible projection 上の新位置（移動後）
        let visibleSectionIDs = newVisible.map { $0.id }
        guard let visibleNewIdx = visibleSectionIDs.firstIndex(of: movedSection.id) else { return }
        // snapshot 上の現位置を除外した残りで挿入位置を決める
        var workingIDs = existingSectionIDs
        if let curIdx = workingIDs.firstIndex(of: movedSection.id) {
            workingIDs.remove(at: curIdx)
        }
        let clampedVisible = min(max(0, visibleNewIdx), workingIDs.count)
        if clampedVisible >= workingIDs.count {
            if let last = workingIDs.last {
                snapshot.moveSection(movedSection.id, afterSection: last)
            }
        } else {
            let beforeID = workingIDs[clampedVisible]
            snapshot.moveSection(movedSection.id, beforeSection: beforeID)
        }

        dataSource.apply(snapshot, animatingDifferences: true) { [weak self] in
            self?.collectionView?.collectionViewLayout.invalidateLayout()
        }
    }

    /// `ReplaceSection` は常に Full 経路（`applyFullSnapshot` 相当）で処理する。
    ///
    /// 置換で可視性が切り替わる可能性があるため、細粒度差分に落とさず Full 経路で作り直す。
    /// `replaceSection` は型上 Section 全体置換であり、`header` / `footer` / `headerHeight` /
    /// `isVisible` / `cells` の任意の変化を内包し得るため、内部 cell の細粒度差分抽出は試みない。
    private func applyReplaceSection(
        dataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>,
        sectionID: UUID,
        new: KsSettingsViewCore.Section
    ) {
        guard let oldSection = sectionIndex[sectionID] else {
            reportMissingID(message: "replaceSection: sectionID \(sectionID) not found")
            return
        }

        // model を更新
        var sections = root.sections
        if let idx = sections.firstIndex(where: { $0.id == sectionID }) {
            sections[idx] = new
        }
        let newRoot = SettingsRoot(sections: sections)

        // view 形式の header / footer はケース一致のみで等価と扱われ中身の変化を検出できない。
        // Section 全体置換の意図を尊重し、view 形式が絡む場合は対象 Section の reload を強制する。
        let accessories: [SectionAccessory?] = [oldSection.header, oldSection.footer, new.header, new.footer]
        let containsViewAccessory = accessories.contains { accessory in
            if case .some(.view) = accessory { return true }
            return false
        }

        // Full 経路にフォールバック（snapshot 全再構築、layout mode 再評価も含む）。
        applyFullSnapshot(
            root: newRoot,
            animated: true,
            forceReloadSectionIDs: containsViewAccessory ? [sectionID] : []
        )
    }

    // MARK: - Diff: Cell 操作

    private func applyInsertCell(
        dataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>,
        sectionID: UUID,
        index: Int,
        cell: any KsCell
    ) {
        guard let section = sectionIndex[sectionID] else {
            reportMissingID(message: "insertCell: sectionID \(sectionID) not found")
            return
        }

        let cellID = KsCellID(cell: cell)
        cellIndex[cellID] = cell

        // 「部分 Diff の index 規約」: `index` は **model 配列基準（hidden 含む）** で解釈する。
        var cells = section.cells
        let cellsClamped = min(max(0, index), cells.count)
        cells.insert(cell, at: cellsClamped)
        let updatedSection = KsSettingsViewCore.Section(
            id: section.id,
            header: section.header,
            footer: section.footer,
            cells: cells,
            headerHeight: section.headerHeight,
            isVisible: section.isVisible,
            isHeaderVisible: section.isHeaderVisible,
            isFooterVisible: section.isFooterVisible
        )
        sectionIndex[sectionID] = updatedSection

        var sections = root.sections
        if let idx = sections.firstIndex(where: { $0.id == sectionID }) {
            sections[idx] = updatedSection
            self.root = SettingsRoot(sections: sections)
        }

        // visible projection 更新
        self.visibleSections = Self.computeVisibleSections(from: self.root.sections)

        // 親 Section が hidden の場合、または挿入対象 Cell が hidden の場合は snapshot 操作 no-op。
        let cellIsVisible = (cell as? VisibilityAware)?.isVisible ?? true
        guard section.isVisible, cellIsVisible else { return }

        var snapshot = dataSource.snapshot()
        guard snapshot.sectionIdentifiers.contains(sectionID) else { return }
        // visible projection 上の挿入位置を算出する。
        // model 配列 0..cellsClamped の中で visible Cell の個数 = visible projection 内の位置。
        let visibleInsertIdx = cells.prefix(cellsClamped).filter {
            ($0 as? VisibilityAware)?.isVisible ?? true
        }.count

        let existingItemIDs = snapshot.itemIdentifiers(inSection: sectionID)
        if visibleInsertIdx >= existingItemIDs.count {
            snapshot.appendItems([cellID], toSection: sectionID)
        } else {
            let beforeID = existingItemIDs[visibleInsertIdx]
            snapshot.insertItems([cellID], beforeItem: beforeID)
        }
        dataSource.apply(snapshot, animatingDifferences: true)
    }

    private func applyRemoveCell(
        dataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>,
        cellID: KsCellID
    ) {
        // 「hidden 対象の no-op 規約」: 削除対象 Cell が hidden（visible projection に居ない）の場合は
        // model のみ更新し、snapshot 操作は行わない。これは「missing ID」として観測されるが正常動作。
        var found = false
        var wasVisible = true
        // sectionIndex は **model**（hidden 含む）を保持するため、ここから旧 Cell を取得する。
        for (sectionID, section) in sectionIndex {
            if let idx = section.cells.firstIndex(where: { $0.id == cellID.id }) {
                let oldCell = section.cells[idx]
                wasVisible = (oldCell as? VisibilityAware)?.isVisible ?? true
                var cells = section.cells
                cells.remove(at: idx)
                let updated = KsSettingsViewCore.Section(
                    id: section.id,
                    header: section.header,
                    footer: section.footer,
                    cells: cells,
                    headerHeight: section.headerHeight,
                    isVisible: section.isVisible,
                    isHeaderVisible: section.isHeaderVisible,
                    isFooterVisible: section.isFooterVisible
                )
                sectionIndex[sectionID] = updated

                // root state 同期
                var sections = root.sections
                if let rIdx = sections.firstIndex(where: { $0.id == sectionID }) {
                    sections[rIdx] = updated
                    self.root = SettingsRoot(sections: sections)
                }
                // hidden Section 配下の Cell かどうかも判定する
                if !section.isVisible { wasVisible = false }
                found = true
                break
            }
        }
        guard found else {
            reportMissingID(message: "removeCell: cellID \(cellID) not found")
            return
        }

        cellIndex.removeValue(forKey: cellID)
        // visible projection 更新
        self.visibleSections = Self.computeVisibleSections(from: self.root.sections)

        // 削除前の Cell が hidden だった場合は snapshot 操作不要（自然 no-op）。
        guard wasVisible else { return }

        var snapshot = dataSource.snapshot()
        if snapshot.itemIdentifiers.contains(cellID) {
            snapshot.deleteItems([cellID])
            dataSource.apply(snapshot, animatingDifferences: true)
        }
    }

    /// 同一 id の Cell の **内容更新（reconfigure / 部分更新）** を反映する。
    ///
    /// 「表示状態同期の三層分離」に従い、`cellIndex` の当該 Cell を新しい Cell に差し替えてから、
    /// `reconfigureItems`（iOS 15+、同一セルインスタンスを破棄せず `cellProvider` で再構成）で反映する。
    /// snapshot の item 集合・順序（構造同期）は変更しない。ちらつきを避けるため、セルを破棄・
    /// 再生成する `reloadItems` は具象型が変わった場合に限って用いる。
    ///
    /// 「ReplaceCell / ReplaceSection の可視性切替防御」（add-visibility-flags-section-and-cell）:
    /// 旧 Cell（`root.sections` から取得）と新 Cell の `isVisible` が異なる場合は、可視性変化は
    /// 構造同期上の追加・削除として表現される必要があり、reconfigure 経路では正しく扱えないため、
    /// Full 経路（`applyFullSnapshot` 相当）にフォールバックする。検出は snapshot の存在チェックよりも
    /// **先に** 行い、旧 Cell が hidden であっても model 上から取得した旧値で判定できなければならない。
    private func applyReplaceCell(
        dataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>,
        cellID: KsCellID,
        new: any KsCell
    ) {
        // 1. **先に** visibility 切替を検出する（snapshot 存在チェックよりも先）。
        //    sectionIndex / cellIndex は **model**（hidden 含む）を保持するため、hidden Cell の旧値も
        //    ここから取得できる。
        guard let oldCell = cellIndex[cellID] else {
            reportMissingID(message: "replaceCell: cellID \(cellID) not found in model")
            return
        }
        let oldVisible = (oldCell as? VisibilityAware)?.isVisible ?? true
        let newVisible = (new as? VisibilityAware)?.isVisible ?? true
        let visibilityToggled = oldVisible != newVisible

        // 2. cellIndex / sectionIndex / root の model を更新する。
        cellIndex[cellID] = new
        for (sectionID, section) in sectionIndex {
            if let idx = section.cells.firstIndex(where: { $0.id == cellID.id }) {
                var cells = section.cells
                cells[idx] = new
                let updated = KsSettingsViewCore.Section(
                    id: section.id,
                    header: section.header,
                    footer: section.footer,
                    cells: cells,
                    headerHeight: section.headerHeight,
                    isVisible: section.isVisible,
                    isHeaderVisible: section.isHeaderVisible,
                    isFooterVisible: section.isFooterVisible
                )
                sectionIndex[sectionID] = updated

                var sections = root.sections
                if let rIdx = sections.firstIndex(where: { $0.id == sectionID }) {
                    sections[rIdx] = updated
                    self.root = SettingsRoot(sections: sections)
                }
                break
            }
        }

        // 3. visibility 切替が検出された場合は Full 経路にフォールバックする。
        if visibilityToggled {
            applyFullSnapshot(root: self.root, animated: true)
            return
        }

        // 4. visibility 同一・hidden 状態の Cell については snapshot 存在チェックで no-op。
        var snapshot = dataSource.snapshot()
        guard snapshot.itemIdentifiers.contains(cellID) else {
            // hidden Cell の内容更新は visible projection に出ないため自然な no-op（エラーではない）。
            // visible projection は既に model 更新で再計算済み（rebuildVisibleProjection はここでは省略）。
            self.visibleSections = Self.computeVisibleSections(from: self.root.sections)
            return
        }

        // 5. visible projection も model 更新を反映して再計算しておく（内容更新が visible Cell に対する
        //    ものなので、visible projection 上の cell 値も更新される）。
        self.visibleSections = Self.computeVisibleSections(from: self.root.sections)

        // 6. 具象型が変われば Renderer も変わるため、同一 Native cell の再構成では反映できない。
        //    その場合は Native cell を交換する reload を使う。
        if type(of: oldCell) != type(of: new) {
            snapshot.reloadItems([cellID])
        } else if #available(iOS 15.0, *) {
            snapshot.reconfigureItems([cellID])
        } else {
            snapshot.reloadItems([cellID])
        }
        dataSource.apply(snapshot, animatingDifferences: true)
    }

    private func applyMoveCell(
        dataSource: UICollectionViewDiffableDataSource<UUID, KsCellID>,
        cellID: KsCellID,
        to index: Int
    ) {
        // 「部分 Diff の index 規約」: `index` は **model 配列基準（hidden 含む）** で解釈する。
        // まず sectionIndex（model）から対象 Section を探索し、model 配列で moved を index に再配置する。
        var foundSectionID: UUID? = nil
        var movedWasVisible = true
        var sectionWasVisible = true
        for (sectionID, section) in sectionIndex {
            if let cellIdx = section.cells.firstIndex(where: { $0.id == cellID.id }) {
                foundSectionID = sectionID
                let movedCell = section.cells[cellIdx]
                movedWasVisible = (movedCell as? VisibilityAware)?.isVisible ?? true
                sectionWasVisible = section.isVisible

                var cells = section.cells
                let moved = cells.remove(at: cellIdx)
                let cellsClamped = min(max(0, index), cells.count)
                cells.insert(moved, at: cellsClamped)
                let updated = KsSettingsViewCore.Section(
                    id: section.id,
                    header: section.header,
                    footer: section.footer,
                    cells: cells,
                    headerHeight: section.headerHeight,
                    isVisible: section.isVisible,
                    isHeaderVisible: section.isHeaderVisible,
                    isFooterVisible: section.isFooterVisible
                )
                sectionIndex[sectionID] = updated

                var sections = root.sections
                if let rIdx = sections.firstIndex(where: { $0.id == sectionID }) {
                    sections[rIdx] = updated
                    self.root = SettingsRoot(sections: sections)
                }
                break
            }
        }
        guard let sectionID = foundSectionID else {
            reportMissingID(message: "moveCell: cellID \(cellID) not found")
            return
        }

        // visible projection を model 更新後に再計算する。
        self.visibleSections = Self.computeVisibleSections(from: self.root.sections)

        // 移動対象が hidden、または親 Section が hidden の場合は snapshot 操作不要。
        guard sectionWasVisible, movedWasVisible else { return }

        var snapshot = dataSource.snapshot()
        guard snapshot.itemIdentifiers.contains(cellID),
              snapshot.sectionIdentifier(containingItem: cellID) == sectionID else {
            // hidden 配下の move では snapshot に居ないため自然 no-op。
            return
        }

        // visible projection 上の新しい index を算出する。
        // 当該 Section の model 配列内で moved の新 index = `index`（clamp 済み相当）に、
        // それより前の visible Cell 数が visible projection 内の新位置となる。
        guard let updatedSection = sectionIndex[sectionID] else { return }
        let cellsAfterUpdate = updatedSection.cells
        let movedIdxInModel = cellsAfterUpdate.firstIndex(where: { $0.id == cellID.id }) ?? 0
        let visibleNewIdx = cellsAfterUpdate.prefix(movedIdxInModel).filter {
            ($0 as? VisibilityAware)?.isVisible ?? true
        }.count

        // 現在の visible 位置を除外して挿入位置を決定する。
        let itemsInSection = snapshot.itemIdentifiers(inSection: sectionID)
        guard let currentIndex = itemsInSection.firstIndex(of: cellID) else { return }
        var working = itemsInSection
        working.remove(at: currentIndex)
        let clamped = min(max(0, visibleNewIdx), working.count)
        if clamped >= working.count {
            if let last = working.last {
                snapshot.moveItem(cellID, afterItem: last)
            }
        } else {
            let beforeID = working[clamped]
            snapshot.moveItem(cellID, beforeItem: beforeID)
        }
        dataSource.apply(snapshot, animatingDifferences: true)
    }

    // MARK: - Diff: Accessory / Theme

    private func applyUpdateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?) {
        switch target {
        case .rootHeader:
            let new: RootAccessory? = extractRootAccessory(accessory)
            self.rootHeader = new

        case .rootFooter:
            let new: RootAccessory? = extractRootAccessory(accessory)
            self.rootFooter = new

        case .sectionHeader(let sectionID):
            updateSectionAccessoryAndReload(sectionID: sectionID, accessory: accessory, isHeader: true)

        case .sectionFooter(let sectionID):
            updateSectionAccessoryAndReload(sectionID: sectionID, accessory: accessory, isHeader: false)
        }
    }

    private func extractRootAccessory(_ accessory: SettingsAccessory?) -> RootAccessory? {
        switch accessory {
        case .root(let r):
            return r
        case .section, .none:
            return nil
        }
    }

    private func updateSectionAccessoryAndReload(
        sectionID: UUID,
        accessory: SettingsAccessory?,
        isHeader: Bool
    ) {
        guard let section = sectionIndex[sectionID] else {
            reportMissingID(message: "updateAccessory(section): sectionID \(sectionID) not found")
            return
        }

        let newAccessory: SectionAccessory?
        switch accessory {
        case .section(let s):
            newAccessory = s
        case .root, .none:
            newAccessory = nil
        }

        let updated = KsSettingsViewCore.Section(
            id: section.id,
            header: isHeader ? newAccessory : section.header,
            footer: isHeader ? section.footer : newAccessory,
            cells: section.cells,
            headerHeight: section.headerHeight,
            isVisible: section.isVisible,
            isHeaderVisible: section.isHeaderVisible,
            isFooterVisible: section.isFooterVisible
        )
        sectionIndex[sectionID] = updated

        var sections = root.sections
        if let rIdx = sections.firstIndex(where: { $0.id == sectionID }) {
            sections[rIdx] = updated
            self.root = SettingsRoot(sections: sections)
        }

        // visible projection 更新（案 A 適用後: layout 同期差し替えなし）
        let newVisible = Self.computeVisibleSections(from: self.root.sections)
        self.visibleSections = newVisible

        // 「hidden 対象の no-op 規約」: 対象 Section が hidden の場合は model 更新のみで
        // snapshot reload を行わない（後で `isVisible = true` に戻ったとき更新済み accessory が反映される）。
        guard section.isVisible else { return }

        // 該当 Section の supplementary を reload
        if let dataSource = self.dataSource {
            var snapshot = dataSource.snapshot()
            if snapshot.sectionIdentifiers.contains(sectionID) {
                snapshot.reloadSections([sectionID])
                dataSource.apply(snapshot, animatingDifferences: true) { [weak self] in
                    self?.collectionView?.collectionViewLayout.invalidateLayout()
                }
            }
        }
    }

    // MARK: - Root Supplementary 再描画

    /// `rootHeader` / `rootFooter` 同型内変化（`.text → .text` の中身違いなど）に対する再描画。
    ///
    /// テキスト accessory / view accessory のいずれも `UICollectionViewListCell` 経路に統一する。
    private func refreshRootSupplementary(elementKind: String) {
        guard let cv = self.collectionView else { return }
        let visibleViews = cv.visibleSupplementaryViews(ofKind: elementKind)
        let visibleIdxs = cv.indexPathsForVisibleSupplementaryElements(ofKind: elementKind)

        for pair in zip(visibleViews, visibleIdxs) {
            let view = pair.0
            guard let listCell = view as? UICollectionViewListCell else { continue }
            let accessory: RootAccessory? = (elementKind == Self.rootHeaderElementKind)
                ? rootHeader
                : rootFooter
            let isFooter = (elementKind == Self.rootFooterElementKind)
            let textColor = currentTheme.headerTextColor
            let verticalAlignment: AccessoryVerticalAlignment = isFooter ? .top : .bottom
            applyAccessoryToListCell(
                listCell,
                accessoryText: accessory.flatMap(rootTextValue),
                accessoryView: accessory.flatMap(rootViewValue),
                textColor: textColor,
                verticalAlignment: verticalAlignment,
                font: isFooter
                    ? Self.resolveFooterFont(theme: currentTheme)
                    : Self.resolveHeaderFont(theme: currentTheme),
                extraContentInsets: takeRootAccessoryContentInsets(isFooter: isFooter),
                textGap: Self.textGap(forElementKind: elementKind)
            )
        }
    }

    /// 表示中の Root Header / Footer のうち text 形式のものへ、現在の Theme の文字色・フォントを
    /// 再適用する。
    ///
    /// 対象を text 形式に限るのは、View 形式（`RootAccessory.view`）の再適用が `KsAnyView` の
    /// factory 再実行を伴い、View が持つ内部状態（編集途中のテキスト・スクロール位置・
    /// first responder）を失わせるため。View 形式の中身はライブラリが文字を描かないので、
    /// 文字色・フォントの追従対象そのものを持たない。
    private func refreshRootAccessoryTextAppearance() {
        if rootHeader.flatMap(rootTextValue) != nil {
            refreshRootSupplementary(elementKind: Self.rootHeaderElementKind)
        }
        if rootFooter.flatMap(rootTextValue) != nil {
            refreshRootSupplementary(elementKind: Self.rootFooterElementKind)
        }
    }

    /// 表示中の Section Header / Footer のうち text 形式のものへ、現在の Theme の文字色・
    /// フォントを再適用する。
    ///
    /// text 形式に限る理由は Root accessory と同じで、View 形式（`SectionAccessory.view`）の
    /// 再適用は `KsAnyView` の factory 再実行を伴い View の内部状態を失わせる。
    private func refreshSectionAccessoryTextAppearance() {
        refreshSectionSupplementaryTextAppearance(
            elementKind: UICollectionView.elementKindSectionHeader,
            accessoryKeyPath: \.header
        )
        refreshSectionSupplementaryTextAppearance(
            elementKind: UICollectionView.elementKindSectionFooter,
            accessoryKeyPath: \.footer
        )
    }

    /// 指定 kind の表示中 Section supplementary のうち text 形式のものへ Theme を再適用する。
    ///
    /// 走査対象を表示中のものに限ってよいのは、表示外の Section は次に dequeue されるときに
    /// `sectionAccessoryView` が現在の Theme から色とフォントを解決し直すため。
    ///
    /// section の解決は「visible projection の二重管理」に従い、indexPath を visible projection
    /// 上の位置として引く（supplementary view 経路と同じ規則）。
    private func refreshSectionSupplementaryTextAppearance(
        elementKind: String,
        accessoryKeyPath: KeyPath<KsSettingsViewCore.Section, SectionAccessory?>
    ) {
        guard let cv = self.collectionView else { return }
        let isFooter = (elementKind == UICollectionView.elementKindSectionFooter)

        for indexPath in cv.indexPathsForVisibleSupplementaryElements(ofKind: elementKind) {
            guard indexPath.section < visibleSections.count else { continue }
            let accessory = visibleSections[indexPath.section][keyPath: accessoryKeyPath]
            // text 形式以外（View 形式・不在）はここで触らない。
            guard let text = accessory.flatMap(textValue) else { continue }
            guard let listCell = cv.supplementaryView(
                forElementKind: elementKind,
                at: indexPath
            ) as? UICollectionViewListCell else { continue }
            applyAccessoryToListCell(
                listCell,
                accessoryText: text,
                accessoryView: nil,
                // Header は headerTextColor、Footer は footerTextColor を使う
                // （Root が headerTextColor を流用するのと異なり、Section は両者を区別する）。
                textColor: isFooter
                    ? currentTheme.footerTextColor
                    : currentTheme.headerTextColor,
                verticalAlignment: isFooter ? .top : .bottom,
                font: isFooter
                    ? Self.resolveFooterFont(theme: currentTheme)
                    : Self.resolveHeaderFont(theme: currentTheme),
                textGap: Self.textGap(forElementKind: elementKind)
            )
        }
    }

    // MARK: - 存在しない ID への操作のエラーハンドリング

    /// DEBUG: assertionFailure / Release: os_log + skip。
    private func reportMissingID(message: String) {
        #if DEBUG
        assertionFailure(message)
        #else
        os_log("%{public}@", log: Self.log, type: .error, message)
        #endif
    }

    // MARK: - Accessory 適用

    /// `UICollectionViewListCell` に accessory（テキスト / SwiftUI / UIKit View）を適用する。
    ///
    /// テキスト accessory も専用の再利用 View 型を挟まず本関数を経由する。
    ///
    /// - テキスト accessory（`accessoryText != nil`）→ `applyAccessoryLabel` で UILabel +
    ///   AutoLayout 制約により Header = 下端揃え / Footer = 上端揃え。
    /// - SwiftUI View（`KsAnyView.swiftUI`）→ `UIHostingConfiguration` を `contentConfiguration` に適用。
    /// - UIKit View（`KsAnyView.uiKit`）→ `contentView` に `addSubview` + 四辺制約。
    /// - いずれもなし → contentView をクリアして空表示。
    /// `extraContentInsets` は accessory の内容の外側に足す余白で、Root Header / Footer が
    /// Section 単位の余白を自身の内側に持つために使う。accessory の高さは内容へ自動追従するため、
    /// この余白はそのまま Root accessory と Section の間隔になる。
    internal func applyAccessoryToListCell(
        _ listCell: UICollectionViewListCell,
        accessoryText: String?,
        accessoryView: KsAnyView?,
        textColor: UIColor,
        verticalAlignment: AccessoryVerticalAlignment,
        font: UIFont? = nil,
        extraContentInsets: UIEdgeInsets = .zero,
        textGap: CGFloat = KsSettingsViewController.sectionTextGap
    ) {
        // 既存 subview をクリア（uiKit backing で addSubview したものを残さない）
        listCell.contentView.subviews.forEach { $0.removeFromSuperview() }
        listCell.contentConfiguration = nil

        // テキスト accessory: UILabel + AutoLayout。
        if let text = accessoryText {
            applyAccessoryLabel(
                listCell,
                text: text,
                textColor: textColor,
                verticalAlignment: verticalAlignment,
                font: font,
                extraContentInsets: extraContentInsets,
                textGap: textGap
            )
            return
        }

        guard let kav = accessoryView else { return }
        switch kav.backing {
        case .swiftUI(let factory):
            let top = extraContentInsets.top
            let bottom = extraContentInsets.bottom
            let configuration = UIHostingConfiguration {
                factory()
                    .padding(.top, top)
                    .padding(.bottom, bottom)
            }
            listCell.contentConfiguration = configuration
        case .uiKit(let factory):
            let inner = factory()
            inner.translatesAutoresizingMaskIntoConstraints = false
            listCell.contentView.addSubview(inner)
            NSLayoutConstraint.activate([
                inner.leadingAnchor.constraint(equalTo: listCell.contentView.leadingAnchor),
                inner.trailingAnchor.constraint(equalTo: listCell.contentView.trailingAnchor),
                inner.topAnchor.constraint(
                    equalTo: listCell.contentView.topAnchor,
                    constant: extraContentInsets.top
                ),
                inner.bottomAnchor.constraint(
                    equalTo: listCell.contentView.bottomAnchor,
                    constant: -extraContentInsets.bottom
                )
            ])
        }
    }

    /// **Section** の Header / Footer のテキストと、隣接する Cell 群との間隔（pt）。
    ///
    /// Header はテキストの下に Cell があるため **下** に、Footer はテキストの上に Cell が
    /// あるため **上** にこの間隔を入れる。反対側は領域内に収めるための最小余白のみ。
    /// Android 側の `SECTION_TEXT_GAP_DP` と同値（両 platform で生値 4 に統一。core/ADR-0027）。
    internal static let sectionTextGap: CGFloat = 4

    /// **Root** の Header / Footer のテキスト間隔（pt）。常に 0（core/ADR-0027）。
    ///
    /// Root は利用者が任意のカスタム View を差し込む場所であり、ライブラリ側が余白を
    /// 足すと利用者のレイアウトに干渉する。Section 単位の余白は `extraContentInsets` で
    /// 別に運ばれるため、テキストそのものの前後にライブラリの余白は入れない。
    /// Android 側の Root accessory（padding 0dp）と揃える。
    internal static let rootTextGap: CGFloat = 0

    /// supplementary の elementKind から、テキストと Cell 群の間隔を解決する。
    ///
    /// Root と Section で余白の扱いが分かれる唯一の判定点。
    internal static func textGap(forElementKind kind: String) -> CGFloat {
        (kind == rootHeaderElementKind || kind == rootFooterElementKind)
            ? rootTextGap
            : sectionTextGap
    }

    /// Header / Footer のテキスト accessory を UILabel + AutoLayout で `contentView` に貼る。
    ///
    /// AiForms.Maui.SettingsView オリジナル `Platforms/iOS/TextHeaderView.cs` の
    /// `SetVerticalAlignment(LayoutAlignment)` 既定（Header = `LayoutAlignment.End` = 下端揃え、
    /// Footer = TopAnchor 制約）に揃え、Header は `bottomAnchor`、Footer は `topAnchor` で
    /// `contentView` に張り付くよう制約を設定する。制約の priority は 999 とし、UIKit 内部の
    /// Required priority 制約と衝突しないようにする（AiForms オリジナル `TextHeaderView.cs` も同様の
    /// priority 999 を使用している）。
    internal func applyAccessoryLabel(
        _ listCell: UICollectionViewListCell,
        text: String,
        textColor: UIColor,
        verticalAlignment: AccessoryVerticalAlignment,
        font: UIFont? = nil,
        extraContentInsets: UIEdgeInsets = .zero,
        textGap: CGFloat = KsSettingsViewController.sectionTextGap
    ) {
        let label = UILabel()
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        // 既定 font は UIListContentConfiguration.cell() の text 既定相当（footnote）に揃え、
        // Cell 並びと密度を揃える。
        // `Theme.headerFont` / `Theme.footerFont` / `headerFontSize` / `footerFontSize` は
        // 呼び出し側の `resolveHeaderFont` / `resolveFooterFont` で解決済みの値が渡る。
        label.font = font ?? UIFont.preferredFont(forTextStyle: .footnote)
        label.text = text
        label.textColor = textColor
        listCell.contentView.addSubview(label)

        // 左右は標準左マージン 16pt（AiForms PaddingLabel + Cell の 16pt マージンに準拠）。
        let leading = label.leadingAnchor.constraint(
            equalTo: listCell.contentView.leadingAnchor, constant: 16
        )
        let trailing = label.trailingAnchor.constraint(
            equalTo: listCell.contentView.trailingAnchor, constant: -16
        )
        var constraints: [NSLayoutConstraint] = [leading, trailing]
        // `extraContentInsets` は内容の外側に足す余白。Root accessory が Section 単位の余白を
        // 自身の内側に持つときに、内容と領域端の間隔をその分だけ広げる。
        // 反対側（テキストの背に当たる側）の最小余白。`textGap` を持たない Root では 0 にし、
        // ライブラリ側の余白がまったく入らないようにする。
        let minInset: CGFloat = textGap > 0 ? 2 : 0
        switch verticalAlignment {
        case .bottom:
            // Header: 下端揃え。下に Cell があるので下側へ `textGap` を入れる。
            // 上側は領域内に収まるよう `>= minInset` の制約のみ。
            let bottom = label.bottomAnchor.constraint(
                equalTo: listCell.contentView.bottomAnchor,
                constant: -textGap - extraContentInsets.bottom
            )
            let topGE = label.topAnchor.constraint(
                greaterThanOrEqualTo: listCell.contentView.topAnchor,
                constant: minInset + extraContentInsets.top
            )
            constraints.append(contentsOf: [bottom, topGE])
        case .top:
            // Footer: 上端揃え。上に Cell があるので上側へ `textGap` を入れる。
            // 下側は領域内に収まるよう `<= -minInset` の制約のみ。
            let top = label.topAnchor.constraint(
                equalTo: listCell.contentView.topAnchor,
                constant: textGap + extraContentInsets.top
            )
            let bottomLE = label.bottomAnchor.constraint(
                lessThanOrEqualTo: listCell.contentView.bottomAnchor,
                constant: -minInset - extraContentInsets.bottom
            )
            constraints.append(contentsOf: [top, bottomLE])
        case .center:
            let centerY = label.centerYAnchor.constraint(
                equalTo: listCell.contentView.centerYAnchor
            )
            constraints.append(centerY)
        }
        // 「Unable to simultaneously satisfy constraints」警告を避けるため priority を 999 に
        // 落とす（AiForms オリジナル `TextHeaderView.cs` lines 44, 92 の `c.Priority = 999f` と
        // 同じ理由）。
        constraints.forEach { $0.priority = UILayoutPriority(rawValue: 999) }
        NSLayoutConstraint.activate(constraints)
    }
}

// MARK: - UICollectionViewDelegate（タップ通知の中継）

extension KsSettingsViewController: UICollectionViewDelegate {
    /// 表示に入る Cell の箱 clip を、そのときの Section 内の位置と箱の実寸から解決し直す。
    ///
    /// dequeue 時の解決だけに頼ると、再利用で戻ってきた Cell が前回の位置の形状を持ち込む。
    public func collectionView(
        _ collectionView: UICollectionView,
        willDisplay cell: UICollectionViewCell,
        forItemAt indexPath: IndexPath
    ) {
        applySectionBoxClip(to: cell, at: indexPath)
    }

    /// Cell タップ時に各 CellView の `tapHandler` を呼び出す。
    ///
    /// CommandCell / ButtonCell / CheckboxCell / RadioCell / SimpleCheckCell の
    /// Renderer が `tapHandler` プロパティに `onTap` / `onValueChanged` クロージャを保持しているため、
    /// 共通の Protocol 経由で呼び出す。
    public func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        defer {
            // 選択ハイライトは残さない（チェックマーク状態は accessory で表現される）
            collectionView.deselectItem(at: indexPath, animated: true)
        }
        guard let cell = collectionView.cellForItem(at: indexPath) else { return }
        if let handler = (cell as? TapNotifyingRenderer)?.tapHandler {
            handler()
        }
    }
}

/// `tapHandler` プロパティを持つ Cell View が満たす内部プロトコル。
///
/// CommandCell / ButtonCell / CheckboxCell / RadioCell / SimpleCheckCell の
/// Renderer 群が共通で参照される。
internal protocol TapNotifyingRenderer: AnyObject {
    var tapHandler: (@Sendable () -> Void)? { get }
}

extension CommandCellView: TapNotifyingRenderer {}
extension ButtonCellView: TapNotifyingRenderer {}
extension CheckboxCellView: TapNotifyingRenderer {}
extension RadioCellView: TapNotifyingRenderer {}
extension SimpleCheckCellView: TapNotifyingRenderer {}
// 入力系 Cell（タップでモーダル提示する 4 種）も `tapHandler` 経路でディスパッチ
extension PickerCellView: TapNotifyingRenderer {}
extension NumberPickerCellView: TapNotifyingRenderer {}
extension TimePickerCellView: TapNotifyingRenderer {}
extension DatePickerCellView: TapNotifyingRenderer {}
// EntryCell も Cell タップで UITextField.becomeFirstResponder() を呼ぶため tapHandler 経路でディスパッチ
extension EntryCellView: TapNotifyingRenderer {}
// CustomCell は `onTap` 指定時のみ行タップを発火する（既定 nil = 行タップ非対応）。
// content 内の操作可能要素がジェスチャを消費した場合、UIKit は didSelectItemAt を呼ばないため
// 行タップと子の操作は二重発火しない。
extension CustomCellView: TapNotifyingRenderer {}

#endif

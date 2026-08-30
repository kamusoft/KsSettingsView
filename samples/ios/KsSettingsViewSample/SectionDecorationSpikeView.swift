// SectionDecorationSpikeView.swift
// KsSettingsViewSample
//
// KsSettingsView を通さない、素の UICollectionViewCompositionalLayout +
// background decoration による「Section の Cell 行だけを覆う箱」の技術検証画面。
//
// 検証したいこと:
//   - compositional layout のサブクラスで decoration の layoutAttributes を
//     「Cell 行のみを覆う frame」へ補正できるか（Header / Footer supplementary を箱の外に出せるか）
//   - カスタム UICollectionViewLayoutAttributes サブクラスで装飾値
//     （角丸半径 / ボーダー幅 / ボーダー色 / 箱の背景色）を decoration view へ輸送できるか
//   - self-sizing（行の高さが後から確定する）に箱が追従するか
//   - Cell の挿入 / 削除アニメーション中に箱が破綻しないか
//
// 画面上のトグルで frame 補正の有無・出現/消滅属性の上書き有無を切り替えられるようにし、
// 補正なしの状態（Header / Footer まで箱が覆う）との A/B 比較ができる。

import SwiftUI
import UIKit

// MARK: - 装飾値

/// 箱の装飾値。sectionProvider と decoration view の双方が参照する。
struct SpikeBoxStyle {
    /// Section 単位（Header・Cell 箱・Footer 一体）の外側余白。
    var margin = NSDirectionalEdgeInsets(top: 22, leading: 16, bottom: 0, trailing: 16)
    var cornerRadius: CGFloat = 26
    var borderWidth: CGFloat = 0
    var borderColor: UIColor = .systemRed
    var backgroundColor: UIColor = .secondarySystemGroupedBackground
}

// MARK: - layoutAttributes

/// 装飾値を layout から decoration view へ運ぶ属性。
///
/// `UICollectionViewLayoutAttributes` の `isEqual` は frame / indexPath / zIndex 等だけを見るため、
/// 装飾値を等価判定に含めないと「frame は同じで角丸だけ変えた」更新が decoration view へ
/// 適用されない。`copy(with:)` も含めて自前で拡張する必要がある。
final class SpikeBoxAttributes: UICollectionViewLayoutAttributes {
    var cornerRadius: CGFloat = 0
    var borderWidth: CGFloat = 0
    var borderColor: UIColor = .clear
    var boxBackgroundColor: UIColor = .clear

    override func copy(with zone: NSZone? = nil) -> Any {
        // UICollectionViewLayoutAttributes.copy はレシーバのクラスで複製を作るため、
        // ここでのキャストは常に成功する。
        guard let copied = super.copy(with: zone) as? SpikeBoxAttributes else {
            return super.copy(with: zone)
        }
        copied.cornerRadius = cornerRadius
        copied.borderWidth = borderWidth
        copied.borderColor = borderColor
        copied.boxBackgroundColor = boxBackgroundColor
        return copied
    }

    override func isEqual(_ object: Any?) -> Bool {
        guard let other = object as? SpikeBoxAttributes else { return false }
        return super.isEqual(object)
            && cornerRadius == other.cornerRadius
            && borderWidth == other.borderWidth
            && borderColor == other.borderColor
            && boxBackgroundColor == other.boxBackgroundColor
    }

    override var hash: Int {
        var hasher = Hasher()
        hasher.combine(super.hash)
        hasher.combine(cornerRadius)
        hasher.combine(borderWidth)
        hasher.combine(borderColor)
        hasher.combine(boxBackgroundColor)
        return hasher.finalize()
    }
}

// MARK: - decoration view

/// 箱の背景（角丸の塗り）。Cell の下に敷く。
final class SpikeBoxBackgroundView: UICollectionReusableView {
    /// 計測時にどの Section の箱かを特定するための添字。
    private(set) var sectionIndex: Int = -1

    override init(frame: CGRect) {
        super.init(frame: frame)
        isUserInteractionEnabled = false
        layer.cornerCurve = .continuous
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    override func apply(_ layoutAttributes: UICollectionViewLayoutAttributes) {
        super.apply(layoutAttributes)
        sectionIndex = layoutAttributes.indexPath.section
        guard let attributes = layoutAttributes as? SpikeBoxAttributes else { return }
        backgroundColor = attributes.boxBackgroundColor
        layer.cornerRadius = min(attributes.cornerRadius, min(bounds.width, bounds.height) / 2)
    }
}

/// 箱のボーダー（枠線のみ）。Cell の上に重ねて、Cell 背景に隠れないことを確認する。
final class SpikeBoxBorderView: UICollectionReusableView {
    /// 計測時にどの Section の枠かを特定するための添字。
    private(set) var sectionIndex: Int = -1

    override init(frame: CGRect) {
        super.init(frame: frame)
        isUserInteractionEnabled = false
        backgroundColor = .clear
        layer.cornerCurve = .continuous
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    override func apply(_ layoutAttributes: UICollectionViewLayoutAttributes) {
        super.apply(layoutAttributes)
        sectionIndex = layoutAttributes.indexPath.section
        guard let attributes = layoutAttributes as? SpikeBoxAttributes else { return }
        layer.borderWidth = attributes.borderWidth
        layer.borderColor = attributes.borderColor.cgColor
        layer.cornerRadius = min(attributes.cornerRadius, min(bounds.width, bounds.height) / 2)
    }
}

// MARK: - layout

/// Section 背景の decoration を「Cell 行だけを覆う frame」へ補正する compositional layout。
final class SpikeSectionDecorationLayout: UICollectionViewCompositionalLayout {
    static let backgroundKind = "spike.section.box.background"
    static let borderKind = "spike.section.box.border"

    /// 装飾値。変更後は `invalidateLayout()` で再評価させる。
    var style = SpikeBoxStyle()
    /// Section ごとの Cell 数を layout へ供給する（layout は data source を直接見ない）。
    var cellCountInSection: (Int) -> Int = { _ in 0 }
    /// frame 補正の有無。false にすると素の decoration（Header / Footer まで覆う）を観察できる。
    var correctsDecorationFrame = true
    /// 挿入 / 削除アニメーション中の出現・消滅属性を補正済み属性で上書きするか。
    var overridesUpdateAttributes = true

    /// 補正のために item 属性を引けなかった回数（先頭 / 末尾別）。
    private(set) var missingFirstItemAttributes = 0
    private(set) var missingLastItemAttributes = 0
    /// 補正の結果 decoration を落とした回数。
    private(set) var droppedDecorations = 0
    /// `layoutAttributesForElements` が decoration を返した回数。
    private(set) var emittedDecorations = 0

    func resetCounters() {
        missingFirstItemAttributes = 0
        missingLastItemAttributes = 0
        droppedDecorations = 0
        emittedDecorations = 0
    }

    override func layoutAttributesForElements(in rect: CGRect) -> [UICollectionViewLayoutAttributes]? {
        guard let base = super.layoutAttributesForElements(in: rect) else { return nil }
        var result: [UICollectionViewLayoutAttributes] = []
        result.reserveCapacity(base.count)
        for attributes in base {
            guard attributes.representedElementCategory == .decorationView,
                  let kind = attributes.representedElementKind,
                  kind == Self.backgroundKind || kind == Self.borderKind else {
                result.append(attributes)
                continue
            }
            // 補正後の frame は補正前の Section 全域に必ず含まれるため、
            // rect との交差判定を素の frame で通っていれば取りこぼしは起きない。
            if let corrected = boxAttributes(kind: kind, indexPath: attributes.indexPath, base: attributes) {
                emittedDecorations += 1
                result.append(corrected)
            } else {
                droppedDecorations += 1
            }
        }
        return result
    }

    override func layoutAttributesForDecorationView(
        ofKind elementKind: String,
        at indexPath: IndexPath
    ) -> UICollectionViewLayoutAttributes? {
        guard let base = super.layoutAttributesForDecorationView(ofKind: elementKind, at: indexPath) else {
            return nil
        }
        guard elementKind == Self.backgroundKind || elementKind == Self.borderKind else { return base }
        return boxAttributes(kind: elementKind, indexPath: indexPath, base: base)
    }

    override func initialLayoutAttributesForAppearingDecorationElement(
        ofKind elementKind: String,
        at decorationIndexPath: IndexPath
    ) -> UICollectionViewLayoutAttributes? {
        guard overridesUpdateAttributes,
              elementKind == Self.backgroundKind || elementKind == Self.borderKind else {
            return super.initialLayoutAttributesForAppearingDecorationElement(
                ofKind: elementKind, at: decorationIndexPath
            )
        }
        return layoutAttributesForDecorationView(ofKind: elementKind, at: decorationIndexPath)
    }

    override func finalLayoutAttributesForDisappearingDecorationElement(
        ofKind elementKind: String,
        at decorationIndexPath: IndexPath
    ) -> UICollectionViewLayoutAttributes? {
        guard overridesUpdateAttributes,
              elementKind == Self.backgroundKind || elementKind == Self.borderKind else {
            return super.finalLayoutAttributesForDisappearingDecorationElement(
                ofKind: elementKind, at: decorationIndexPath
            )
        }
        return layoutAttributesForDecorationView(ofKind: elementKind, at: decorationIndexPath)
    }

    /// 補正済みの箱属性を作る。Cell が 1 つも無い Section では nil を返して箱を生成しない。
    private func boxAttributes(
        kind: String,
        indexPath: IndexPath,
        base: UICollectionViewLayoutAttributes
    ) -> SpikeBoxAttributes? {
        let isBorder = (kind == Self.borderKind)
        let attributes = SpikeBoxAttributes(forDecorationViewOfKind: kind, with: indexPath)
        attributes.frame = base.frame
        // ボーダーは Cell より前面、背景は Cell より背面に置く。
        attributes.zIndex = isBorder ? 10 : -1
        attributes.cornerRadius = style.cornerRadius
        attributes.borderWidth = isBorder ? max(0, style.borderWidth) : 0
        attributes.borderColor = isBorder ? style.borderColor : .clear
        attributes.boxBackgroundColor = isBorder ? .clear : style.backgroundColor

        guard correctsDecorationFrame else { return attributes }
        guard let frame = cellRowsFrame(inSection: indexPath.section) else { return nil }
        attributes.frame = frame
        return attributes
    }

    /// Section 内の Cell 行だけを覆う矩形。先頭 Cell と末尾 Cell の frame の和で求める。
    private func cellRowsFrame(inSection section: Int) -> CGRect? {
        let count = cellCountInSection(section)
        guard count > 0 else { return nil }
        guard let first = super.layoutAttributesForItem(at: IndexPath(item: 0, section: section)) else {
            missingFirstItemAttributes += 1
            return nil
        }
        guard count > 1 else { return first.frame }
        guard let last = super.layoutAttributesForItem(at: IndexPath(item: count - 1, section: section)) else {
            missingLastItemAttributes += 1
            return first.frame
        }
        return first.frame.union(last.frame)
    }
}

// MARK: - モデル

/// 検証用の Section / Cell モデル。
@MainActor
final class SpikeDecorationModel: ObservableObject {
    struct Row: Hashable {
        let id: UUID
        var title: String
        /// Cell 自身が不透明背景を塗るケースの再現用。
        var paintsOwnBackground: Bool = false
    }

    struct SectionData: Hashable {
        let id: UUID
        let header: String?
        let footer: String?
        var rows: [Row]
    }

    @Published private(set) var sections: [SectionData] = []
    /// 長文トグルの現在値（self-sizing の追従確認用）。
    @Published private(set) var isLongText = false
    private var nextRowIndex = 1

    private static let longText = """
    self-sizing の確認用に十分な長さを持たせた行です。\
    折り返しで 3 行以上になるようにして、行高さが確定した後に箱の下端が追従するかを見ます。\
    追従しなければ箱が行からずれて見えるはずです。
    """

    init() {
        sections = [
            SectionData(
                id: UUID(),
                header: "SECTION A（Header / Footer あり）",
                footer: "Footer A は箱の外側に出ること",
                rows: [
                    Row(id: UUID(), title: "Row A-1"),
                    Row(id: UUID(), title: "Row A-2（Cell 背景あり）", paintsOwnBackground: true),
                    Row(id: UUID(), title: "Row A-3")
                ]
            ),
            SectionData(
                id: UUID(),
                header: "SECTION B（単一 Cell）",
                footer: nil,
                rows: [Row(id: UUID(), title: "Row B-1")]
            ),
            SectionData(
                id: UUID(),
                header: "SECTION C（Cell 0 件・箱は出ないこと）",
                footer: "Cell が無い Section の Footer",
                rows: []
            ),
            SectionData(
                id: UUID(),
                header: "SECTION D（長い Section・スクロール確認）",
                footer: "Footer D",
                rows: (1...12).map { Row(id: UUID(), title: "Row D-\($0)") }
            )
        ]
        nextRowIndex = 1
    }

    func appendRowToFirstSection() {
        guard !sections.isEmpty else { return }
        sections[0].rows.append(Row(id: UUID(), title: "追加 Row \(nextRowIndex)"))
        nextRowIndex += 1
    }

    func insertRowIntoFirstSectionMiddle() {
        guard !sections.isEmpty else { return }
        let index = sections[0].rows.count / 2
        sections[0].rows.insert(Row(id: UUID(), title: "中間 Row \(nextRowIndex)"), at: index)
        nextRowIndex += 1
    }

    func removeLastRowFromFirstSection() {
        guard !sections.isEmpty, !sections[0].rows.isEmpty else { return }
        sections[0].rows.removeLast()
    }

    func removeFirstRowFromFirstSection() {
        guard !sections.isEmpty, !sections[0].rows.isEmpty else { return }
        sections[0].rows.removeFirst()
    }

    /// 先頭 Section の 1 行目を長文 / 短文で切り替える（self-sizing の追従確認）。
    func toggleLongText() {
        guard !sections.isEmpty, !sections[0].rows.isEmpty else { return }
        isLongText.toggle()
        sections[0].rows[0].title = isLongText ? Self.longText : "Row A-1"
    }
}

// MARK: - SwiftUI 画面

/// Section 装飾の技術検証画面。
struct SectionDecorationSpikeView: View {
    @StateObject private var model = SpikeDecorationModel()
    @State private var style = SpikeBoxStyle()
    @State private var correctsFrame = true
    @State private var overridesUpdateAttributes = true
    @State private var cellsPaintOpaqueBackground = false
    @State private var slowAnimations = false
    @State private var lastTappedTitle = "-"
    @State private var diagnostics = "-"

    var body: some View {
        VStack(spacing: 0) {
            controls
            SpikeDecorationCollectionView(
                model: model,
                style: style,
                correctsFrame: correctsFrame,
                overridesUpdateAttributes: overridesUpdateAttributes,
                cellsPaintOpaqueBackground: cellsPaintOpaqueBackground,
                slowAnimations: slowAnimations,
                onTap: { lastTappedTitle = $0 },
                onDiagnostics: { diagnostics = $0 }
            )
            .ignoresSafeArea(.container, edges: .bottom)
        }
        .navigationTitle(SampleScreen.sectionDecorationSpike.title)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var controls: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Button("末尾追加") { model.appendRowToFirstSection() }
                Button("中間挿入") { model.insertRowIntoFirstSectionMiddle() }
                Button("末尾削除") { model.removeLastRowFromFirstSection() }
                Button("先頭削除") { model.removeFirstRowFromFirstSection() }
            }
            .buttonStyle(.bordered)
            .controlSize(.small)

            HStack {
                Button("長文トグル") { model.toggleLongText() }
                Button("角丸 26/8") { style.cornerRadius = (style.cornerRadius == 26) ? 8 : 26 }
                Button("ボーダー 0/3") { style.borderWidth = (style.borderWidth == 0) ? 3 : 0 }
                Button("余白 22/40") {
                    style.margin.top = (style.margin.top == 22) ? 40 : 22
                }
            }
            .buttonStyle(.bordered)
            .controlSize(.small)

            Toggle("decoration の frame を Cell 行へ補正する", isOn: $correctsFrame)
            Toggle("出現 / 消滅属性を補正済み属性で上書きする", isOn: $overridesUpdateAttributes)
            Toggle("Cell が不透明背景を塗る", isOn: $cellsPaintOpaqueBackground)
            Toggle("アニメーションを低速にする（観察用）", isOn: $slowAnimations)
            Text("最後にタップした行: \(lastTappedTitle)")
                .font(.caption2)
            Text(diagnostics)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .font(.caption)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }
}

// MARK: - UIViewControllerRepresentable

struct SpikeDecorationCollectionView: UIViewControllerRepresentable {
    @ObservedObject var model: SpikeDecorationModel
    let style: SpikeBoxStyle
    let correctsFrame: Bool
    let overridesUpdateAttributes: Bool
    let cellsPaintOpaqueBackground: Bool
    let slowAnimations: Bool
    let onTap: (String) -> Void
    let onDiagnostics: (String) -> Void

    func makeUIViewController(context: Context) -> SpikeDecorationViewController {
        SpikeDecorationViewController()
    }

    func updateUIViewController(_ controller: SpikeDecorationViewController, context: Context) {
        controller.onTap = onTap
        controller.onDiagnostics = onDiagnostics
        controller.update(
            sections: model.sections,
            style: style,
            correctsFrame: correctsFrame,
            overridesUpdateAttributes: overridesUpdateAttributes,
            cellsPaintOpaqueBackground: cellsPaintOpaqueBackground,
            slowAnimations: slowAnimations
        )
    }
}

// MARK: - UIViewController

final class SpikeDecorationViewController: UIViewController {
    private var collectionView: UICollectionView!
    private var spikeLayout: SpikeSectionDecorationLayout!
    private var dataSource: UICollectionViewDiffableDataSource<UUID, UUID>!
    private var sections: [SpikeDecorationModel.SectionData] = []
    private var cellsPaintOpaqueBackground = false
    private var didApplyOnce = false
    var onTap: ((String) -> Void)?
    var onDiagnostics: ((String) -> Void)?

    // 箱と Cell 行の追従を毎フレーム計測するための状態。
    private var displayLink: CADisplayLink?
    private var samplingDeadline: CFTimeInterval = 0
    private var worstTopGap: CGFloat = 0
    private var worstBottomGap: CGFloat = 0
    private var sampleCount = 0
    private var comparedSamples = 0
    private var boxMissingSamples = 0
    /// 箱自身が補間アニメーションしているか（presentation と model の差が出るか）。
    private var boxInterpolation: CGFloat = 0

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemGroupedBackground

        spikeLayout = makeLayout()
        spikeLayout.cellCountInSection = { [weak self] index in
            guard let self, index < self.sections.count else { return 0 }
            return self.sections[index].rows.count
        }
        spikeLayout.register(
            SpikeBoxBackgroundView.self,
            forDecorationViewOfKind: SpikeSectionDecorationLayout.backgroundKind
        )
        spikeLayout.register(
            SpikeBoxBorderView.self,
            forDecorationViewOfKind: SpikeSectionDecorationLayout.borderKind
        )

        collectionView = UICollectionView(frame: view.bounds, collectionViewLayout: spikeLayout)
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.backgroundColor = .systemGroupedBackground
        collectionView.delegate = self
        view.addSubview(collectionView)
        NSLayoutConstraint.activate([
            collectionView.topAnchor.constraint(equalTo: view.topAnchor),
            collectionView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            collectionView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            collectionView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])

        configureDataSource()
    }

    // MARK: レイアウト

    private func makeLayout() -> SpikeSectionDecorationLayout {
        var listConfig = UICollectionLayoutListConfiguration(appearance: .plain)
        listConfig.backgroundColor = .clear
        listConfig.headerMode = .supplementary
        listConfig.footerMode = .supplementary
        listConfig.headerTopPadding = 0
        listConfig.showsSeparators = false

        return SpikeSectionDecorationLayout(sectionProvider: { [weak self] index, environment in
            let section = NSCollectionLayoutSection.list(using: listConfig, layoutEnvironment: environment)
            let style = self?.spikeLayout?.style ?? SpikeBoxStyle()
            section.contentInsets = style.margin

            // Header / Footer は箱の外に出すが、supplementary 自体は Section 単位の
            // 余白の内側に置く。`.plain` の既定 pin 固定は外す。
            // 内容を持たない Header / Footer は item ごと落とす（supplementaryViewProvider が
            // nil を返すと UIKit が例外を投げるため、layout 側で存在させない）。
            let model: SpikeDecorationModel.SectionData? = {
                guard let self, index < self.sections.count else { return nil }
                return self.sections[index]
            }()
            section.boundarySupplementaryItems = section.boundarySupplementaryItems.filter { item in
                switch item.elementKind {
                case UICollectionView.elementKindSectionHeader:
                    return model?.header != nil
                case UICollectionView.elementKindSectionFooter:
                    return model?.footer != nil
                default:
                    return true
                }
            }
            for item in section.boundarySupplementaryItems {
                item.pinToVisibleBounds = false
                item.contentInsets = .zero
            }
            // Cell が 0 件の Section にも decoration item 自体は宣言しておき、
            // 箱を出さない判断は layout の frame 補正側で行う。
            section.decorationItems = [
                NSCollectionLayoutDecorationItem.background(
                    elementKind: SpikeSectionDecorationLayout.backgroundKind
                ),
                NSCollectionLayoutDecorationItem.background(
                    elementKind: SpikeSectionDecorationLayout.borderKind
                )
            ]
            return section
        })
    }

    // MARK: DataSource

    private func configureDataSource() {
        let cellRegistration = UICollectionView.CellRegistration<UICollectionViewListCell, UUID> {
            [weak self] cell, indexPath, itemID in
            guard let self else { return }
            let row = self.findRow(itemID)
            var content = cell.defaultContentConfiguration()
            content.text = row?.title ?? "?"
            content.textProperties.numberOfLines = 0
            cell.contentConfiguration = content

            var background = UIBackgroundConfiguration.listPlainCell()
            if let row, row.paintsOwnBackground {
                background.backgroundColor = UIColor.systemYellow.withAlphaComponent(0.5)
            } else if self.cellsPaintOpaqueBackground {
                background.backgroundColor = .secondarySystemGroupedBackground
            } else {
                background.backgroundColor = .clear
            }
            cell.backgroundConfiguration = background
            _ = indexPath
        }

        let headerRegistration = UICollectionView.SupplementaryRegistration<UICollectionViewListCell>(
            elementKind: UICollectionView.elementKindSectionHeader
        ) { [weak self] supplementary, _, indexPath in
            guard let self, indexPath.section < self.sections.count else { return }
            var content = supplementary.defaultContentConfiguration()
            content.text = self.sections[indexPath.section].header
            supplementary.contentConfiguration = content
            var background = UIBackgroundConfiguration.clear()
            background.backgroundColor = .clear
            supplementary.backgroundConfiguration = background
        }

        let footerRegistration = UICollectionView.SupplementaryRegistration<UICollectionViewListCell>(
            elementKind: UICollectionView.elementKindSectionFooter
        ) { [weak self] supplementary, _, indexPath in
            guard let self, indexPath.section < self.sections.count else { return }
            var content = supplementary.defaultContentConfiguration()
            content.text = self.sections[indexPath.section].footer
            supplementary.contentConfiguration = content
            var background = UIBackgroundConfiguration.clear()
            background.backgroundColor = .clear
            supplementary.backgroundConfiguration = background
        }

        dataSource = UICollectionViewDiffableDataSource<UUID, UUID>(
            collectionView: collectionView
        ) { collectionView, indexPath, itemID in
            collectionView.dequeueConfiguredReusableCell(using: cellRegistration, for: indexPath, item: itemID)
        }
        dataSource.supplementaryViewProvider = { [weak self] _, kind, indexPath in
            guard let self, indexPath.section < self.sections.count else { return nil }
            switch kind {
            case UICollectionView.elementKindSectionHeader:
                guard self.sections[indexPath.section].header != nil else { return nil }
                return self.collectionView.dequeueConfiguredReusableSupplementary(
                    using: headerRegistration, for: indexPath
                )
            case UICollectionView.elementKindSectionFooter:
                guard self.sections[indexPath.section].footer != nil else { return nil }
                return self.collectionView.dequeueConfiguredReusableSupplementary(
                    using: footerRegistration, for: indexPath
                )
            default:
                return nil
            }
        }
    }

    // MARK: 更新

    func update(
        sections: [SpikeDecorationModel.SectionData],
        style: SpikeBoxStyle,
        correctsFrame: Bool,
        overridesUpdateAttributes: Bool,
        cellsPaintOpaqueBackground: Bool,
        slowAnimations: Bool
    ) {
        // 挿入 / 削除アニメーションを目視・撮影で追えるように減速する。
        collectionView.layer.speed = slowAnimations ? 0.04 : 1.0
        // 構造の変化は identity の並びで判定する。identity が同じで内容だけ変わった場合は
        // snapshot 差分に現れないため、明示的に reconfigure しないと再描画されない。
        let oldIdentities = self.sections.map { $0.rows.map(\.id) }
        let newIdentities = sections.map { $0.rows.map(\.id) }
        let structureChanged = oldIdentities != newIdentities
        let contentChanged = self.sections != sections
        let renderingChanged = self.cellsPaintOpaqueBackground != cellsPaintOpaqueBackground
        self.sections = sections
        self.cellsPaintOpaqueBackground = cellsPaintOpaqueBackground
        spikeLayout.style = style
        spikeLayout.correctsDecorationFrame = correctsFrame
        spikeLayout.overridesUpdateAttributes = overridesUpdateAttributes

        var snapshot = NSDiffableDataSourceSnapshot<UUID, UUID>()
        for section in sections {
            snapshot.appendSections([section.id])
            snapshot.appendItems(section.rows.map(\.id), toSection: section.id)
        }
        if renderingChanged || (contentChanged && !structureChanged) {
            // 行の内容だけが変わったケース（長文トグル等）は既存 Cell を再構成する。
            snapshot.reconfigureItems(snapshot.itemIdentifiers)
        }
        let animated = didApplyOnce
        didApplyOnce = true
        if animated && structureChanged {
            // 挿入 / 削除アニメーション中に箱が Cell 行へ追従しているかを毎フレーム測る。
            startSampling(duration: slowAnimations ? 12.0 : 1.5)
        }
        dataSource.apply(snapshot, animatingDifferences: animated) { [weak self] in
            // 装飾値・余白の変更は sectionProvider と decoration 属性の再評価が必要。
            self?.collectionView.collectionViewLayout.invalidateLayout()
        }
    }

    // MARK: 追従の計測

    /// 指定秒数のあいだ毎フレーム、先頭 Section の箱と Cell 行の上下端のずれを測る。
    private func startSampling(duration: CFTimeInterval) {
        displayLink?.invalidate()
        worstTopGap = 0
        worstBottomGap = 0
        sampleCount = 0
        comparedSamples = 0
        boxMissingSamples = 0
        boxInterpolation = 0
        samplingDeadline = CACurrentMediaTime() + duration
        let link = CADisplayLink(target: self, selector: #selector(sampleBoxFollowing))
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    @objc private func sampleBoxFollowing() {
        defer {
            if CACurrentMediaTime() > samplingDeadline {
                displayLink?.invalidate()
                displayLink = nil
                let summary = String(
                    format: "計測 %d frame（比較成立 %d）/ 上端ずれ最大 %.1fpt / 下端ずれ最大 %.1fpt / 箱の補間幅 %.1fpt / 箱欠落 %d frame",
                    sampleCount, comparedSamples, worstTopGap, worstBottomGap, boxInterpolation, boxMissingSamples
                )
                onDiagnostics?(summary)
            }
        }
        guard let first = sections.first, !first.rows.isEmpty else { return }
        sampleCount += 1

        let box = collectionView.subviews
            .compactMap { $0 as? SpikeBoxBackgroundView }
            .first { $0.sectionIndex == 0 }
        guard let box, let boxFrame = box.layer.presentation()?.frame else {
            boxMissingSamples += 1
            return
        }
        boxInterpolation = max(boxInterpolation, abs(boxFrame.maxY - box.layer.frame.maxY))
        guard let firstCell = collectionView.cellForItem(at: IndexPath(item: 0, section: 0)),
              let lastCell = collectionView.cellForItem(at: IndexPath(item: first.rows.count - 1, section: 0)),
              let firstFrame = firstCell.layer.presentation()?.frame,
              let lastFrame = lastCell.layer.presentation()?.frame else {
            return
        }
        comparedSamples += 1
        worstTopGap = max(worstTopGap, abs(boxFrame.minY - firstFrame.minY))
        worstBottomGap = max(worstBottomGap, abs(boxFrame.maxY - lastFrame.maxY))
    }

    /// スクロール後に layout 側のカウンタと実在する箱の Section を報告する。
    fileprivate func reportLayoutCounters() {
        let liveSections = collectionView.subviews
            .compactMap { $0 as? SpikeBoxBackgroundView }
            .map(\.sectionIndex)
            .sorted()
        // 前後関係は subview の並びではなく layer.zPosition で決まるため、実値を読む。
        let cellZ = collectionView.visibleCells.first?.layer.zPosition ?? -999
        let borderReport = collectionView.subviews
            .compactMap { $0 as? SpikeBoxBorderView }
            .map { String(format: "S%d:z%.0f", $0.sectionIndex, $0.layer.zPosition) }
            .joined(separator: " ")
        let summary = String(
            format: "箱 %@ / Cell z%.0f / 枠 %@ / 供給 %d・棄却 %d / item属性なし 先頭 %d・末尾 %d",
            "\(liveSections)",
            cellZ,
            borderReport.isEmpty ? "なし" : borderReport,
            spikeLayout.emittedDecorations,
            spikeLayout.droppedDecorations,
            spikeLayout.missingFirstItemAttributes,
            spikeLayout.missingLastItemAttributes
        )
        spikeLayout.resetCounters()
        onDiagnostics?(summary)
    }

    private func findRow(_ itemID: UUID) -> SpikeDecorationModel.Row? {
        for section in sections {
            if let hit = section.rows.first(where: { $0.id == itemID }) { return hit }
        }
        return nil
    }
}

extension SpikeDecorationViewController: UICollectionViewDelegate {
    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        reportLayoutCounters()
    }

    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        if !decelerate { reportLayoutCounters() }
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        collectionView.deselectItem(at: indexPath, animated: true)
        guard indexPath.section < sections.count,
              indexPath.item < sections[indexPath.section].rows.count else { return }
        onTap?(sections[indexPath.section].rows[indexPath.item].title)
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        SectionDecorationSpikeView()
    }
}
#endif

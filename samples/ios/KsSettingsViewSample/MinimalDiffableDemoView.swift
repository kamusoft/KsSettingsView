// MinimalDiffableDemoView.swift
// KsSettingsViewSample
//
// KsSettingsView を通さない、純粋な UICollectionViewDiffableDataSource +
// UICollectionViewCompositionalLayout の最小再現サンプル。
//
// 目的:
//   - Section の追加・削除・footer 有無切替でアニメーションが本当に発生するかを検証する
//   - KsSettingsView 現状の「apply 毎に setCollectionViewLayout を同期差し替え」を
//     再現するトグルと、その対策案を Minimal 側で先に試す
//
// 設計方針:
//   - 既定 layout は sectionProvider 方式（案 A）で 1 度だけ生成して使い回す（差し替えない）
//   - section ごとに footer の有無を持たせ、sectionProvider 内で動的に切り替える
//   - 比較用トグル:
//     - reLayoutOnApply: apply 毎に layout を作り直す（KsSettingsView 現状再現）
//     - wrapReLayoutInPerformWithoutAnimation: 案 B（performWithoutAnimation で包む）

import SwiftUI
import UIKit

/// 純粋 Diffable + Compositional の最小検証画面。
struct MinimalDiffableDemoView: View {
    @StateObject private var model = MinimalDiffableModel()
    /// KsSettingsView 現状の挙動を再現するために、apply 時に
    /// `setCollectionViewLayout` を同期で差し替えるかどうか。
    @State private var reLayoutOnApply: Bool = false
    /// layout 差し替えを `UIView.performWithoutAnimation` で包んで glitch を抑える試行（案 B）。
    @State private var wrapReLayoutInPerformWithoutAnimation: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 8) {
                HStack {
                    Button("末尾に追加") { model.appendSection() }
                        .buttonStyle(.borderedProminent)

                    Button("中間に追加") { model.insertMiddleSection() }
                        .buttonStyle(.borderedProminent)
                }
                HStack {
                    Button("末尾 Section 削除") { model.removeLastSection() }
                        .buttonStyle(.bordered)

                    Button("中間 Section 削除") { model.removeMiddleSection() }
                        .buttonStyle(.bordered)
                }
                HStack {
                    Button("中間 Section の footer toggle") { model.toggleMiddleFooter() }
                        .buttonStyle(.bordered)
                }
                Toggle("apply 時に layout を作り直す（KsSettingsView 再現）", isOn: $reLayoutOnApply)
                    .font(.caption)
                Toggle("layout 差し替えを performWithoutAnimation で包む（案 B）", isOn: $wrapReLayoutInPerformWithoutAnimation)
                    .font(.caption)
                    .disabled(!reLayoutOnApply)
            }
            .padding()

            MinimalDiffableCollectionView(
                model: model,
                reLayoutOnApply: reLayoutOnApply,
                wrapReLayoutInPerformWithoutAnimation: wrapReLayoutInPerformWithoutAnimation
            )
                .ignoresSafeArea(.container, edges: .bottom)
        }
        .navigationTitle(SampleScreen.minimalDiffable.title)
    }
}

// MARK: - Model

/// セクション識別子（UUID）と内容（header/footer/items）を保持する素朴なモデル。
final class MinimalDiffableModel: ObservableObject {
    struct SectionData: Hashable {
        let id: UUID
        let header: String
        let footer: String?
        let items: [Item]

        /// footer 文字列の有無を sectionProvider 側で判定するための簡易プロパティ。
        var hasFooter: Bool { (footer?.isEmpty == false) }
    }

    struct Item: Hashable {
        let id: UUID
        let title: String
    }

    @Published private(set) var sections: [SectionData] = []
    private var nextSectionIndex: Int = 1

    init() {
        sections = [
            makeSection(title: "Section A", hasFooter: true),
            makeSection(title: "Section B", hasFooter: false),
            makeSection(title: "Section C", hasFooter: true),
        ]
        nextSectionIndex = 4
    }

    func appendSection() {
        let title = "Section \(nextSectionIndex)"
        nextSectionIndex += 1
        // 偶奇で footer 有無を変える。
        sections.append(makeSection(title: title, hasFooter: nextSectionIndex.isMultiple(of: 2)))
    }

    func insertMiddleSection() {
        let title = "Section \(nextSectionIndex)"
        nextSectionIndex += 1
        let index = sections.isEmpty ? 0 : sections.count / 2
        sections.insert(makeSection(title: title, hasFooter: true), at: index)
    }

    func removeLastSection() {
        guard !sections.isEmpty else { return }
        sections.removeLast()
    }

    func removeMiddleSection() {
        guard sections.count >= 3 else { return }
        sections.remove(at: sections.count / 2)
    }

    /// 中間 Section の footer 有無をトグル。
    /// section.id は維持して内容（footer）だけ差し替える → DiffableDataSource は
    /// section identifier 不変として扱い、layout 側だけが headerMode/footerMode を再評価する。
    func toggleMiddleFooter() {
        guard !sections.isEmpty else { return }
        let idx = sections.count / 2
        let cur = sections[idx]
        let newFooter: String? = cur.hasFooter ? nil : "\(cur.header) footer"
        sections[idx] = SectionData(id: cur.id, header: cur.header, footer: newFooter, items: cur.items)
    }

    private func makeSection(title: String, hasFooter: Bool) -> SectionData {
        SectionData(
            id: UUID(),
            header: title,
            footer: hasFooter ? "\(title) footer" : nil,
            items: (1...3).map { Item(id: UUID(), title: "\(title) - Row \($0)") }
        )
    }
}

// MARK: - UIViewControllerRepresentable

struct MinimalDiffableCollectionView: UIViewControllerRepresentable {
    @ObservedObject var model: MinimalDiffableModel
    let reLayoutOnApply: Bool
    let wrapReLayoutInPerformWithoutAnimation: Bool

    func makeUIViewController(context: Context) -> MinimalDiffableViewController {
        MinimalDiffableViewController()
    }

    func updateUIViewController(_ vc: MinimalDiffableViewController, context: Context) {
        vc.reLayoutOnApply = reLayoutOnApply
        vc.wrapReLayoutInPerformWithoutAnimation = wrapReLayoutInPerformWithoutAnimation
        vc.apply(sections: model.sections, animated: true)
    }
}

// MARK: - UIViewController

final class MinimalDiffableViewController: UIViewController {

    private var collectionView: UICollectionView!
    private var dataSource: UICollectionViewDiffableDataSource<UUID, UUID>!
    /// 描画用に保持しておく直近の section 配列（cellProvider / supplementaryProvider / sectionProvider から参照）。
    private var snapshotSections: [MinimalDiffableModel.SectionData] = []
    /// KsSettingsView 現状の挙動再現用: apply のたびに layout を作り直す。
    var reLayoutOnApply: Bool = false
    /// layout 差し替えを `UIView.performWithoutAnimation` で包むかどうか（案 B）。
    var wrapReLayoutInPerformWithoutAnimation: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        // layout は 1 度だけ生成して使い回す（差し替えない）。
        // sectionProvider 内で snapshotSections を参照し、section ごとに header/footer 有無を返す。
        let layout = makeLayout()
        collectionView = UICollectionView(frame: view.bounds, collectionViewLayout: layout)
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.backgroundColor = .systemGroupedBackground
        view.addSubview(collectionView)
        NSLayoutConstraint.activate([
            collectionView.topAnchor.constraint(equalTo: view.topAnchor),
            collectionView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            collectionView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            collectionView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
        ])

        configureDataSource()
    }

    // MARK: Layout

    /// 案 A: sectionProvider 方式。section ごとに UICollectionLayoutListConfiguration を構築し、
    /// section.hasFooter に応じて footerMode を切り替える。
    /// レイアウト自体は 1 度しか生成しないので、後段で setCollectionViewLayout を呼ぶ必要がない。
    private func makeLayout() -> UICollectionViewLayout {
        return UICollectionViewCompositionalLayout(sectionProvider: { [weak self] sectionIndex, environment in
            guard let self else {
                return Self.fallbackListSection(environment: environment)
            }
            let hasFooter: Bool
            if sectionIndex < self.snapshotSections.count {
                hasFooter = self.snapshotSections[sectionIndex].hasFooter
            } else {
                hasFooter = false
            }
            var config = UICollectionLayoutListConfiguration(appearance: .insetGrouped)
            config.headerMode = .supplementary
            config.footerMode = hasFooter ? .supplementary : .none
            return NSCollectionLayoutSection.list(using: config, layoutEnvironment: environment)
        })
    }

    private static func fallbackListSection(environment: NSCollectionLayoutEnvironment) -> NSCollectionLayoutSection {
        var config = UICollectionLayoutListConfiguration(appearance: .insetGrouped)
        config.headerMode = .supplementary
        config.footerMode = .none
        return NSCollectionLayoutSection.list(using: config, layoutEnvironment: environment)
    }

    // MARK: DataSource

    private func configureDataSource() {
        let cellRegistration = UICollectionView.CellRegistration<UICollectionViewListCell, UUID> { [weak self] cell, _, itemID in
            guard let self else { return }
            let item = self.findItem(itemID: itemID)
            var content = cell.defaultContentConfiguration()
            content.text = item?.title ?? "?"
            cell.contentConfiguration = content
        }

        let headerRegistration = UICollectionView.SupplementaryRegistration<UICollectionViewListCell>(
            elementKind: UICollectionView.elementKindSectionHeader
        ) { [weak self] supplementary, _, indexPath in
            guard let self else { return }
            guard indexPath.section < self.snapshotSections.count else { return }
            var content = supplementary.defaultContentConfiguration()
            content.text = self.snapshotSections[indexPath.section].header
            supplementary.contentConfiguration = content
        }

        let footerRegistration = UICollectionView.SupplementaryRegistration<UICollectionViewListCell>(
            elementKind: UICollectionView.elementKindSectionFooter
        ) { [weak self] supplementary, _, indexPath in
            guard let self else { return }
            guard indexPath.section < self.snapshotSections.count else { return }
            var content = supplementary.defaultContentConfiguration()
            content.text = self.snapshotSections[indexPath.section].footer
            supplementary.contentConfiguration = content
        }

        dataSource = UICollectionViewDiffableDataSource<UUID, UUID>(collectionView: collectionView) { collectionView, indexPath, itemID in
            collectionView.dequeueConfiguredReusableCell(using: cellRegistration, for: indexPath, item: itemID)
        }
        dataSource.supplementaryViewProvider = { [weak self] collectionView, kind, indexPath in
            guard let self else { return nil }
            switch kind {
            case UICollectionView.elementKindSectionHeader:
                return collectionView.dequeueConfiguredReusableSupplementary(using: headerRegistration, for: indexPath)
            case UICollectionView.elementKindSectionFooter:
                // footer mode が .none の section では呼ばれない想定だが、念のためガード。
                if indexPath.section < self.snapshotSections.count,
                   self.snapshotSections[indexPath.section].hasFooter {
                    return collectionView.dequeueConfiguredReusableSupplementary(using: footerRegistration, for: indexPath)
                }
                return nil
            default:
                return nil
            }
        }
    }

    // MARK: Apply

    /// 与えられた section 配列で snapshot を組み直して apply する。
    func apply(sections: [MinimalDiffableModel.SectionData], animated: Bool) {
        snapshotSections = sections

        // KsSettingsView 現状を再現するため、apply のたびに新しい layout を作って
        // setCollectionViewLayout で同期差し替えする。これが ON だと描画乱れ・全 Cell バウンドが再現（実証済）。
        // 案 A（sectionProvider）が成立すれば、ON にしなくても section ごとの header/footer 有無の変化に追従する。
        if reLayoutOnApply {
            let newLayout = makeLayout()
            if wrapReLayoutInPerformWithoutAnimation {
                UIView.performWithoutAnimation {
                    collectionView.setCollectionViewLayout(newLayout, animated: false)
                }
            } else {
                collectionView.setCollectionViewLayout(newLayout, animated: false)
            }
        }

        var snapshot = NSDiffableDataSourceSnapshot<UUID, UUID>()
        for section in sections {
            snapshot.appendSections([section.id])
            snapshot.appendItems(section.items.map { $0.id }, toSection: section.id)
        }
        dataSource.apply(snapshot, animatingDifferences: animated) { [weak self] in
            guard let self else { return }
            // sectionProvider は section 構造が変わらない限り再評価されないため、
            // footer 有無のように「同じ section の内部属性」が変わった場合は
            // 明示的に layout を invalidate して sectionProvider を再起動させる必要がある。
            self.collectionView.collectionViewLayout.invalidateLayout()
        }
    }

    // MARK: Helpers

    private func findItem(itemID: UUID) -> MinimalDiffableModel.Item? {
        for section in snapshotSections {
            if let hit = section.items.first(where: { $0.id == itemID }) {
                return hit
            }
        }
        return nil
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        MinimalDiffableDemoView()
    }
}
#endif

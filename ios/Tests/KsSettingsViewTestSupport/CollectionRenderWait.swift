// CollectionRenderWait.swift
// KsSettingsViewTestSupport
//
// UICollectionView の初期反映 (Section 構造・行・supplementary の実体化) を待つヘルパ。

#if canImport(UIKit)
import Foundation
import UIKit

/// collectionView が期待する Section 構造まで反映され、表示領域にかかる要素がすべて
/// 実体化するまで待つ。
///
/// window へ載せた直後の collectionView は Section を 1 つも持たず、データソース適用と
/// レイアウトが進んで初めて行と supplementary が生成される。待機の完了条件は、この直後に
/// テストが読む対象すべてを含める。狭い条件で抜けると、まだ生成されていない Cell の
/// タイトルを空文字として読んだり、supplementary を nil として読んだりする。
///
/// 完了条件は 3 つの層でできている:
///
/// 1. **Section 構造** — Section 数と Section ごとの行数が `expectedItemCounts` と一致する。
///    データソース適用前の「まだ空の collectionView」を成立扱いにしないための土台
/// 2. **必須 supplementary** — `requiredSupplementaryKinds` に挙げた kind の attributes を
///    レイアウトが置いている。レイアウトがまだその kind の領域を持っていない段階では 3 の
///    走査が空振りするため、呼び出し側が「あるはず」と知っている kind をここで明示する。
///    実体化を求めるのは可視矩形にかかる分だけで、画面外にある要素は 3 と同じく対象外
/// 3. **レイアウトが置く要素の全走査** — レイアウトが可視矩形へ置く attributes を列挙し、
///    Cell と supplementary が実体化しているかを kind に依らず確かめる。kind を列挙せず
///    レイアウトに問い合わせるため、新しい kind が増えてもこの述語は追従する
///
/// 表示領域の外にある要素と、面積を持たない要素は生成されないため待機の対象にしない。
/// decoration view は collectionView 経由で実体を取得する API が無いため走査から外す
/// (レイアウトが自前で生成するもので、待つべき非同期の遷移が無い)。
///
/// - Parameters:
///   - collectionView: 対象の collectionView
///   - description: 何の反映を待っているか。失敗メッセージに載る
///   - expectedItemCounts: 期待する Section 構造。要素数が Section 数、各要素がその Section の行数
///   - requiredSupplementaryKinds: 実体化していなければならない supplementary の elementKind。
///     Section に属さない boundary supplementary (Root accessory 等) のように、Section 構造からは
///     存在を導けないものを呼び出し側が渡す
///   - deadline: 打ち切りまでの実時間 (秒)。既定は `KsTestWait.defaultDeadline`
@MainActor
public func awaitCollectionRender(
    _ collectionView: UICollectionView,
    _ description: String,
    expectedItemCounts: [Int],
    requiredSupplementaryKinds: [String] = [],
    deadline: TimeInterval = KsTestWait.defaultDeadline,
    file: StaticString = #filePath,
    line: UInt = #line
) {
    awaitCondition(
        "\(description) (期待 Section 構造: \(expectedItemCounts), 必須 supplementary: \(requiredSupplementaryKinds))",
        in: collectionView,
        deadline: deadline,
        actual: {
            describeCollectionRender(
                collectionView,
                requiredSupplementaryKinds: requiredSupplementaryKinds
            )
        },
        file: file,
        line: line,
        until: {
            isCollectionRendered(
                collectionView,
                expectedItemCounts: expectedItemCounts,
                requiredSupplementaryKinds: requiredSupplementaryKinds
            )
        }
    )
}

/// 期待する Section 構造まで反映され、可視領域の要素が実体化しているか。
@MainActor
private func isCollectionRendered(
    _ collectionView: UICollectionView,
    expectedItemCounts: [Int],
    requiredSupplementaryKinds: [String]
) -> Bool {
    guard collectionView.numberOfSections == expectedItemCounts.count else { return false }
    for (section, expectedCount) in expectedItemCounts.enumerated() {
        guard collectionView.numberOfItems(inSection: section) == expectedCount else { return false }
    }
    for kind in requiredSupplementaryKinds {
        guard isRequiredSupplementaryReady(collectionView, kind: kind) else { return false }
    }
    for attributes in visibleLayoutAttributes(collectionView) {
        guard isElementRendered(collectionView, attributes: attributes) else { return false }
    }
    return true
}

/// 必須 supplementary の kind をレイアウトが置いているか。可視矩形にかかる分は実体化まで求める。
///
/// レイアウトがまだその kind の attributes を持たない段階を不成立にするのがこの判定の役目で、
/// 「設定されている」ことは「可視矩形に入る」ことを意味しない。layout 全体の boundary
/// supplementary はコンテンツと一緒にスクロールするため、コンテンツが可視矩形より高ければ
/// 初期表示の時点で画面外にあり、実体化は起きない。画面外の要素まで待つと述語は成立しなくなる。
@MainActor
private func isRequiredSupplementaryReady(
    _ collectionView: UICollectionView,
    kind: String
) -> Bool {
    let placed = contentLayoutAttributes(collectionView).filter {
        $0.representedElementCategory == .supplementaryView && $0.representedElementKind == kind
    }
    guard !placed.isEmpty else { return false }
    let bounds = collectionView.bounds
    for attributes in placed where attributes.frame.intersects(bounds) {
        guard collectionView.supplementaryView(
            forElementKind: kind,
            at: attributes.indexPath
        ) != nil else { return false }
    }
    return true
}

/// レイアウトがコンテンツ全体へ置く attributes を返す。
///
/// 画面外の要素も対象に含めるため、問い合わせ範囲はコンテンツ矩形と可視矩形の和を使う。
/// コンテンツサイズがまだ確定していない段階では空になり、必須 supplementary の判定は不成立になる。
@MainActor
private func contentLayoutAttributes(
    _ collectionView: UICollectionView
) -> [UICollectionViewLayoutAttributes] {
    let layout = collectionView.collectionViewLayout
    let rect = CGRect(origin: .zero, size: layout.collectionViewContentSize)
        .union(collectionView.bounds)
    return layout.layoutAttributesForElements(in: rect) ?? []
}

/// レイアウトが collectionView の可視矩形へ置く attributes のうち、面積を持つものを返す。
///
/// 面積が無い要素 (高さ 0 の header / footer 等) は view が作られないため、`intersects` の
/// 空矩形判定でそのまま除かれる。
@MainActor
private func visibleLayoutAttributes(
    _ collectionView: UICollectionView
) -> [UICollectionViewLayoutAttributes] {
    let bounds = collectionView.bounds
    let attributes = collectionView.collectionViewLayout.layoutAttributesForElements(in: bounds) ?? []
    return attributes.filter { $0.frame.intersects(bounds) }
}

/// attributes が指す要素が実体化しているか。decoration view は対象外として true を返す。
@MainActor
private func isElementRendered(
    _ collectionView: UICollectionView,
    attributes: UICollectionViewLayoutAttributes
) -> Bool {
    switch attributes.representedElementCategory {
    case .cell:
        return collectionView.cellForItem(at: attributes.indexPath) != nil
    case .supplementaryView:
        guard let kind = attributes.representedElementKind else { return true }
        return collectionView.supplementaryView(
            forElementKind: kind,
            at: attributes.indexPath
        ) != nil
    default:
        return true
    }
}

/// 反映の観測値を失敗メッセージ用に文字列化する。
@MainActor
private func describeCollectionRender(
    _ collectionView: UICollectionView,
    requiredSupplementaryKinds: [String]
) -> String {
    let sections = (0..<collectionView.numberOfSections).map { section -> String in
        let itemCount = collectionView.numberOfItems(inSection: section)
        let renderedCells = (0..<itemCount).count { item in
            collectionView.cellForItem(at: IndexPath(item: item, section: section)) != nil
        }
        return "[\(section)] 行 \(renderedCells)/\(itemCount)"
    }
    let required = requiredSupplementaryKinds.map { kind in
        let placed = contentLayoutAttributes(collectionView).count {
            $0.representedElementCategory == .supplementaryView && $0.representedElementKind == kind
        }
        let rendered = collectionView.visibleSupplementaryViews(ofKind: kind).count
        return "\(kind) レイアウト \(placed) 件/実体化 \(rendered) 件"
    }
    let pending = visibleLayoutAttributes(collectionView)
        .filter { !isElementRendered(collectionView, attributes: $0) }
        .map { describeAttributes($0) }
    return """
        Section \(collectionView.numberOfSections) \(sections.joined(separator: " ")) / \
        必須 supplementary [\(required.joined(separator: ", "))] / \
        未実体化の可視要素 \(pending.isEmpty ? "なし" : pending.joined(separator: ", "))
        """
}

/// 未実体化の要素を失敗メッセージ用に識別できる形へ整形する。
///
/// layout 全体の boundary supplementary の indexPath は要素が 1 つしかないため、`section` /
/// `item` へ分解せず `IndexPath` をそのまま出す (`item` は 2 要素を前提とする)。
private func describeAttributes(_ attributes: UICollectionViewLayoutAttributes) -> String {
    let kind = attributes.representedElementKind ?? "cell"
    return "\(kind)@\(attributes.indexPath)"
}
#endif

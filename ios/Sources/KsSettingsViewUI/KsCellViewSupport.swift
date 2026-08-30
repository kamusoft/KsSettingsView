// KsCellViewSupport.swift
// KsSettingsViewUI
//
// 全 Cell View 共通の補助ヘルパ。
//   - `configurationUpdateHandler` ベースのタッチフィードバック（`Theme.selectedColor`）
//   - 実効行高さ（固定／最低）の制約管理（変化時のみ更新）
//   - render 時の最新 Theme / isEnabled / 実効背景色を保持

#if canImport(UIKit)
import UIKit
import ObjectiveC.runtime

/// `UICollectionViewListCell` サブクラスに紐づける可変補助状態。
///
/// `configurationUpdateHandler` の closure からも参照したいため、参照型として 1 つ保持する。
internal final class KsCellViewState {
    var theme: Theme = Theme()
    var isEnabled: Bool = true
    var effectiveCellBackgroundColor: UIColor = .clear
    var heightConstraint: NSLayoutConstraint?
    var lastHeight: CGFloat?
    var lastIsFixedHeight: Bool?
    /// Modern の箱に収めるための clip 形状。`layoutSubviews` から mask を作り直すために保持する。
    var sectionBoxClip: SectionBoxCellClip = .none

    init() {}
}

/// `UICollectionViewListCell` サブクラスに共通の「タッチフィードバック / 実効高さ」ヘルパ。
internal enum KsCellViewSupport {

    // MARK: - associated object key

    /// 各 Cell View が保持する補助状態のためのキー。`UnsafeRawPointer` 互換の安定したアドレスを使う。
    private static let stateKey: StaticString = "ks_cell_view_support_state"
    private static var stateKeyPointer: UnsafeRawPointer {
        // StaticString が静的領域にあり、`utf8Start` は同一実行中で一貫する安定アドレスを返す
        return UnsafeRawPointer(stateKey.utf8Start)
    }

    // MARK: - 状態取得

    /// 当該 Cell View に紐づく `KsCellViewState` を返す（無ければ生成する）。
    static func state(_ listCell: UICollectionViewListCell) -> KsCellViewState {
        if let s = objc_getAssociatedObject(listCell, stateKeyPointer) as? KsCellViewState {
            return s
        }
        let s = KsCellViewState()
        objc_setAssociatedObject(listCell, stateKeyPointer, s, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return s
    }

    // MARK: - render 時の状態更新

    /// Cell View に「現在の Theme / isEnabled / 実効背景色」を記録する。
    /// `installSelectedColorHandler` の closure から参照する。
    static func setRenderState(
        _ listCell: UICollectionViewListCell,
        theme: Theme,
        isEnabled: Bool,
        effectiveBackgroundColor: UIColor
    ) {
        let s = state(listCell)
        s.theme = theme
        s.isEnabled = isEnabled
        s.effectiveCellBackgroundColor = effectiveBackgroundColor
        // タップ無効化（isEnabled = false 時）
        listCell.isUserInteractionEnabled = isEnabled
    }

    // MARK: - selectedColor の反映（タッチフィードバック）

    /// `configurationUpdateHandler` をインストールする。
    ///
    /// `state.isHighlighted || state.isSelected` のとき `Theme.selectedColor` を `backgroundColor` に塗り、
    /// それ以外で平常時の背景色（CellStyle.backgroundColor ?? Theme.cellBackgroundColor）に戻す。
    /// `isEnabled == false` の Cell では selectedColor を反映しない。
    static func installSelectedColorHandler(_ listCell: UICollectionViewListCell) {
        listCell.configurationUpdateHandler = { [weak listCell] _, cellState in
            guard let listCell = listCell as? UICollectionViewListCell else { return }
            let s = state(listCell)
            var bg = listCell.backgroundConfiguration ?? listCell.defaultBackgroundConfiguration()
            if s.isEnabled && (cellState.isHighlighted || cellState.isSelected) {
                bg.backgroundColor = s.theme.selectedColor
            } else {
                bg.backgroundColor = s.effectiveCellBackgroundColor
            }
            listCell.backgroundConfiguration = bg
        }
    }

    // MARK: - Section の箱への clip

    /// Modern の箱に収めるための clip を Cell へ設定する。
    ///
    /// 箱の decoration は Cell より背面に置かれるため、Cell 自身の背景・押下背景を箱の内側形状へ
    /// 収めることでボーダーを全周で見せ、角丸の外へ背景がはみ出さないようにする。
    /// 形状は Cell の bounds に依存するため、実際の mask 生成は `updateSectionBoxClipMask(_:)` に任せ、
    /// self-sizing で高さが変わっても追従させる。
    static func applySectionBoxClip(_ listCell: UICollectionViewListCell, clip: SectionBoxCellClip) {
        let s = state(listCell)
        s.sectionBoxClip = clip
        updateSectionBoxClipMask(listCell)
    }

    /// 現在の bounds から clip の mask を作り直す。Cell の `layoutSubviews` から呼ぶ。
    static func updateSectionBoxClipMask(_ listCell: UICollectionViewListCell) {
        let clip = state(listCell).sectionBoxClip
        guard let path = clip.maskPath(in: listCell.bounds) else {
            listCell.layer.mask = nil
            return
        }
        let shape = (listCell.layer.mask as? CAShapeLayer) ?? CAShapeLayer()
        // frame 変化に mask が遅れて追従するとちらつくため、mask の更新は暗黙アニメーションを外す。
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        shape.frame = listCell.bounds
        shape.path = path.cgPath
        CATransaction.commit()
        if listCell.layer.mask !== shape {
            listCell.layer.mask = shape
        }
    }

    // MARK: - 実効高さ適用

    /// 実効高さ制約を Cell View の `contentView` に適用する。
    ///
    /// - `hasUnevenRows == false`（`isFixedHeight == true`）: 固定高さ
    /// - `hasUnevenRows == true`（`isFixedHeight == false`）: 最低高さ保証つきの可変高さ
    ///
    /// 制約は ViewHolder にキャッシュし、変化時のみ再設定する（パフォーマンス）。
    static func applyEffectiveHeight(
        _ listCell: UICollectionViewListCell,
        effective: EffectiveStyle
    ) {
        let s = state(listCell)
        let newHeight = effective.effectiveCellHeight
        let newIsFixed = effective.isFixedHeight

        if let lh = s.lastHeight, let lf = s.lastIsFixedHeight,
           lh == newHeight, lf == newIsFixed,
           s.heightConstraint != nil {
            return  // 変化なし
        }

        // 旧制約があれば外す
        if let oldConstraint = s.heightConstraint {
            oldConstraint.isActive = false
        }

        let content = listCell.contentView
        let constraint: NSLayoutConstraint
        if newIsFixed {
            constraint = content.heightAnchor.constraint(equalToConstant: newHeight)
        } else {
            constraint = content.heightAnchor.constraint(greaterThanOrEqualToConstant: newHeight)
        }
        constraint.priority = .required - 1
        constraint.isActive = true

        s.heightConstraint = constraint
        s.lastHeight = newHeight
        s.lastIsFixedHeight = newIsFixed

        listCell.setNeedsLayout()
        // preferredLayoutAttributesFitting で参照するために高さを再記録（applyEffectiveHeight 直後は
        // 上の `s.lastHeight = newHeight` で更新済み。ここでは明示的なメモを残す目的）
    }

    // MARK: - preferredLayoutAttributesFitting の補正

    /// Cell の `preferredLayoutAttributesFitting(_:)` から呼び出すヘルパ。
    ///
    /// `applyEffectiveHeight` が記録した `lastHeight` / `lastIsFixedHeight` に基づいて、
    /// proposed attributes の `size.height` を補正する：
    /// - `lastHeight` が記録されていなければ `proposed` をそのまま返す（補正なし）。
    /// - `lastIsFixedHeight == true`（`Theme.hasUnevenRows == false`）→ 厳密に `lastHeight` に固定する。
    /// - `lastIsFixedHeight == false`（`hasUnevenRows == true`）→ `max(proposed.size.height, lastHeight)` を採用する。
    ///
    /// これにより Cell の `cellHeight` がレイアウト結果の行高さへ反映される。
    /// 参照: AiForms オリジナル `Native/iOS/SettingsTableSource.cs` lines 113-135
    ///   （`GetHeightForRow` が `cell.Height` の CGFloat を直接返し UITableView の rect 計算に反映する設計）。
    static func adjustedLayoutAttributes(
        _ listCell: UICollectionViewListCell,
        proposed: UICollectionViewLayoutAttributes
    ) -> UICollectionViewLayoutAttributes {
        let s = state(listCell)
        guard let desired = s.lastHeight, let isFixed = s.lastIsFixedHeight else {
            return proposed
        }
        // 補正後の attributes は proposed をコピーして size.height のみ書き換える。
        // copy() は NSCopying により attributes 全体（indexPath / zIndex / center / frame 等）を保持する。
        guard let copied = proposed.copy() as? UICollectionViewLayoutAttributes else {
            return proposed
        }
        var size = copied.size
        if isFixed {
            // 固定高さ：proposed の値に関わらず desired に揃える。
            size.height = desired
        } else {
            // 可変高さ：desired を下限としつつ、intrinsic（proposed）がそれを上回ればそれを採用。
            size.height = max(size.height, desired)
        }
        copied.size = size
        return copied
    }
}
#endif

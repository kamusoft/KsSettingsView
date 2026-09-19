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
    /// 押下色を塗り始めるまでの待ちを担う Task。押下から抜けたら cancel する。
    var pendingSelectedColorTask: Task<Void, Never>?
    /// 押下色を現在塗っているか。
    var isShowingSelectedColor: Bool = false
    /// 今の押下のあいだに選択が確定したか。解除をフェードアウトにするかの判定に使う。
    var hasBeenSelectedWhilePressed: Bool = false

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

    /// render 時の背景色を Cell へ適用する。
    ///
    /// 押下色を出している最中は平常色で塗り替えない。タップで値が変わる Cell（RadioCell 等）は
    /// タップ直後に再構成されるため、ここで平常色を入れると押下色が消えてしまう。
    @MainActor
    static func applyRenderedBackgroundColor(_ listCell: UICollectionViewListCell) {
        let s = state(listCell)
        var bg = listCell.defaultBackgroundConfiguration()
        bg.backgroundColor = s.isShowingSelectedColor ? s.theme.selectedColor : s.effectiveCellBackgroundColor
        UIView.performWithoutAnimation {
            listCell.backgroundConfiguration = bg
        }
    }

    // MARK: - selectedColor の反映（タッチフィードバック）

    /// `configurationUpdateHandler` をインストールする。
    ///
    /// `state.isHighlighted || state.isSelected` のとき `Theme.selectedColor` を `backgroundColor` に塗り、
    /// それ以外で平常時の背景色（CellStyle.backgroundColor ?? Theme.cellBackgroundColor）に戻す。
    /// `isEnabled == false` の Cell では selectedColor を反映しない。
    ///
    /// 押下色の出入りには時間をかける。指を置いた瞬間にべた塗りで出ると、スクロールを
    /// 始めただけのタッチでも行が一瞬光って見えるため、
    ///
    /// - 押下に入ってもすぐには塗らず `selectedColorHighlightDelay` だけ待つ。待ちのあいだに
    ///   押下から抜けたら（スクロールでキャンセルされたら）何も塗らない
    /// - 待ちの途中で選択が確定したら（速いタップで指が離れたら）待たずに塗り始める
    /// - 塗りは `selectedColorFadeInDuration` でフェードインする
    /// - 選択を経てから抜けるとき（タップの解除）は `selectedColorFadeOutDuration` で
    ///   フェードアウトし、選択を経ずに抜けるとき（キャンセル）はフェードアウトせずに戻す
    static func installSelectedColorHandler(_ listCell: UICollectionViewListCell) {
        // 押下色をいつ出していつ消すかはライブラリが決める (ios/ADR-0005)。
        listCell.configurationUpdateHandler = { [weak listCell] _, cellState in
            // handler 型は nonisolated で UICellConfigurationState は Sendable ではないため、
            // actor 境界の外で押下状態を Sendable な Bool に確定する。
            let isHighlighted = cellState.isHighlighted
            let isSelected = cellState.isSelected
            // UIKit は configurationUpdateHandler を main thread から呼ぶため、UIKit 状態の
            // 参照と更新は main actor 上に隔離されているものとして扱える。
            MainActor.assumeIsolated {
                guard let listCell else { return }
                updateSelectedColor(listCell, isHighlighted: isHighlighted, isSelected: isSelected)
            }
        }
    }

    /// Cell の再利用時に、予約中の塗り始めと押下色の表示状態を捨てる。
    ///
    /// 次に表示する内容へ前の行の押下色が持ち越されないよう、`prepareForReuse` から呼ぶ。
    @MainActor
    static func resetSelectedColor(_ listCell: UICollectionViewListCell) {
        let s = state(listCell)
        s.pendingSelectedColorTask?.cancel()
        s.pendingSelectedColorTask = nil
        s.hasBeenSelectedWhilePressed = false
        s.isShowingSelectedColor = false
    }

    /// 押下状態の変化を押下色へ反映する。
    @MainActor
    private static func updateSelectedColor(
        _ listCell: UICollectionViewListCell,
        isHighlighted: Bool,
        isSelected: Bool
    ) {
        let s = state(listCell)
        let isPressed = isHighlighted || isSelected
        guard s.isEnabled, isPressed else {
            // 押下から抜けた（または無効な Cell）。予約は捨て、選択を経ていたときだけ
            // フェードアウトで戻し、それ以外はフェードアウトなしで平常色へ戻す。
            s.pendingSelectedColorTask?.cancel()
            s.pendingSelectedColorTask = nil
            let fadesOut = s.hasBeenSelectedWhilePressed && s.isShowingSelectedColor
            s.hasBeenSelectedWhilePressed = false
            restoreNormalColor(listCell, fadesOut: fadesOut)
            return
        }

        if isSelected {
            s.hasBeenSelectedWhilePressed = true
            // 選択が確定したら待たずに塗り始める。
            s.pendingSelectedColorTask?.cancel()
            s.pendingSelectedColorTask = nil
            fadeInSelectedColor(listCell)
            return
        }

        // 押下に入っただけの段階。塗り始めを遅らせ、スクロールでキャンセルされたら塗らない。
        guard !s.isShowingSelectedColor, s.pendingSelectedColorTask == nil else { return }
        s.pendingSelectedColorTask = Task { @MainActor [weak listCell] in
            try? await Task.sleep(for: .seconds(selectedColorHighlightDelay))
            guard !Task.isCancelled, let listCell else { return }
            state(listCell).pendingSelectedColorTask = nil
            fadeInSelectedColor(listCell)
        }
    }

    /// 押下色をフェードインで塗る。
    @MainActor
    private static func fadeInSelectedColor(_ listCell: UICollectionViewListCell) {
        let s = state(listCell)
        s.isShowingSelectedColor = true
        var bg = listCell.backgroundConfiguration ?? listCell.defaultBackgroundConfiguration()
        bg.backgroundColor = s.theme.selectedColor
        UIView.animate(
            withDuration: selectedColorFadeInDuration,
            delay: 0,
            options: [.curveEaseIn, .beginFromCurrentState, .allowUserInteraction]
        ) {
            listCell.backgroundConfiguration = bg
        }
    }

    /// 平常時の背景色へ戻す。`fadesOut` が false ならフェードアウトせずに平常色を入れる
    /// (進行中のフェードインは完了してから戻ることがある)。
    @MainActor
    private static func restoreNormalColor(_ listCell: UICollectionViewListCell, fadesOut: Bool) {
        let s = state(listCell)
        s.isShowingSelectedColor = false
        var bg = listCell.backgroundConfiguration ?? listCell.defaultBackgroundConfiguration()
        bg.backgroundColor = s.effectiveCellBackgroundColor
        if fadesOut {
            UIView.animate(
                withDuration: selectedColorFadeOutDuration,
                delay: 0,
                options: [.curveEaseOut, .beginFromCurrentState, .allowUserInteraction]
            ) {
                listCell.backgroundConfiguration = bg
            }
        } else {
            UIView.performWithoutAnimation {
                listCell.backgroundConfiguration = bg
            }
        }
    }

    // MARK: - 押下色のタイミング定数

    /// 押下に入ってから塗り始めるまでの待ち時間（秒）。
    private static let selectedColorHighlightDelay: TimeInterval = 0.1
    /// 押下色をフェードインさせる時間（秒）。
    private static let selectedColorFadeInDuration: TimeInterval = 0.12
    /// 選択の解除で押下色をフェードアウトさせる時間（秒）。
    private static let selectedColorFadeOutDuration: TimeInterval = 0.25

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

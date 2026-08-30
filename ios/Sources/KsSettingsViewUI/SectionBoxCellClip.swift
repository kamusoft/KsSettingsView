// SectionBoxCellClip.swift
// KsSettingsViewUI
//
// Modern の箱と Cell 背景の合成規則。Cell 側が「箱の内側形状」へ収まるための clip 形状を表す。
//
// 箱の decoration は Cell より背面に置かれるため、Cell が不透明背景（`CellStyle.backgroundColor` や
// 押下時の `Theme.selectedColor`）を塗るとボーダーと角丸が覆われる。そこで Cell 自身を
// 「ボーダーの内側・箱の角丸」で clip し、ボーダーが全周で見え、角の外へ背景がはみ出さない状態を作る。
//
// clip は「Cell の位置に応じて角を丸めるか」ではなく「箱の内側形状を Cell の座標系へ写したもの」として
// 持つ。こうすると角丸半径が 1 行の高さを超えても箱と同じ弧を共有でき、Cell 単体の寸法で半径を
// 切り詰めた結果 Cell の背景だけが箱の角からはみ出す、という不一致が起きない。

#if canImport(UIKit)
import UIKit

/// Section の箱に対する Cell 単位の clip 形状。
internal struct SectionBoxCellClip: Equatable {
    /// 箱の角丸半径。箱の実寸で clamp 済みの値を保持する。
    let cornerRadius: CGFloat
    /// 箱のボーダー幅。Cell はこの幅だけ内側へ収まる。
    let borderWidth: CGFloat
    /// Cell の上端から箱の上端までの距離。0 なら Cell が箱の上端（Section 先頭）に接する。
    let boxTopOffset: CGFloat
    /// Cell の下端から箱の下端までの距離。0 なら Cell が箱の下端（Section 末尾）に接する。
    let boxBottomOffset: CGFloat

    /// clip を掛けない状態（Classic および箱を持たない Cell）。
    static let none = SectionBoxCellClip(
        cornerRadius: 0,
        borderWidth: 0,
        boxTopOffset: 0,
        boxBottomOffset: 0
    )

    /// 箱の上端の角がこの Cell に掛かるか（Section 先頭 Cell）。
    var roundsTop: Bool { boxTopOffset <= 0 }
    /// 箱の下端の角がこの Cell に掛かるか（Section 末尾 Cell）。
    var roundsBottom: Bool { boxBottomOffset <= 0 }

    /// clip が形状を何も変えないか。
    ///
    /// ボーダーが無く、かつ箱の角の弧がこの Cell の範囲へ届かない（上下とも半径以上離れている）
    /// ときは、clip しても結果が Cell の矩形と一致する。
    var isIdentity: Bool {
        if borderWidth > 0 { return false }
        if cornerRadius <= 0 { return true }
        return boxTopOffset >= cornerRadius && boxBottomOffset >= cornerRadius
    }

    /// Section 内の位置と実際の箱 / Cell の矩形から clip を解決する。
    ///
    /// `boxFrame` と `cellFrame` を渡すと箱の弧を Cell の座標系で正確に再現できる。geometry が
    /// 取れない場合は Section 内の位置だけで解決し、境界でない側は「角の弧が届かない」ものとして扱う。
    ///
    /// - Parameters:
    ///   - metrics: 解決済みの箱の装飾値
    ///   - boxFrame: Section の Cell 行だけを覆う箱の矩形（取得できないとき `nil`）
    ///   - cellFrame: 対象 Cell の矩形（取得できないとき `nil`）
    ///   - itemIndex: Section 内の Cell の位置
    ///   - cellCount: Section の可視 Cell 数
    static func resolve(
        metrics: SectionBoxMetrics,
        boxFrame: CGRect? = nil,
        cellFrame: CGRect? = nil,
        itemIndex: Int,
        cellCount: Int
    ) -> SectionBoxCellClip {
        guard cellCount > 0, itemIndex >= 0, itemIndex < cellCount else { return .none }
        // 半径の上限は箱の実寸で決める。Cell 単体の寸法では決めない。
        let radius: CGFloat = boxFrame.map {
            SectionBoxMetrics.clampedCornerRadius(metrics.cornerRadius, for: $0.size)
        } ?? max(0, metrics.cornerRadius)

        let topOffset: CGFloat
        let bottomOffset: CGFloat
        if let boxFrame = boxFrame, let cellFrame = cellFrame, boxFrame.height > 0 {
            topOffset = max(0, cellFrame.minY - boxFrame.minY)
            bottomOffset = max(0, boxFrame.maxY - cellFrame.maxY)
        } else {
            // geometry 無しの解決。境界でない側は角の弧が届かない距離として扱う。
            topOffset = (itemIndex == 0) ? 0 : radius
            bottomOffset = (itemIndex == cellCount - 1) ? 0 : radius
        }

        let clip = SectionBoxCellClip(
            cornerRadius: radius,
            borderWidth: max(0, metrics.borderWidth),
            boxTopOffset: topOffset,
            boxBottomOffset: bottomOffset
        )
        return clip.isIdentity ? .none : clip
    }

    /// 指定の Cell 矩形に対する clip 形状のパス。形状を変えない場合は `nil`。
    ///
    /// 箱の内側形状（ボーダー幅だけ内側へ寄せた角丸矩形）を Cell の座標系で組み立てる。Cell の
    /// 上下へはみ出す部分は Cell の layer の bounds で切り落とされるため、中間 Cell では左右の
    /// ボーダー分の inset だけが残る。
    func maskPath(in bounds: CGRect) -> UIBezierPath? {
        guard !isIdentity, bounds.width > 0, bounds.height > 0 else { return nil }
        let boxRect = CGRect(
            x: bounds.minX,
            y: bounds.minY - boxTopOffset,
            width: bounds.width,
            height: bounds.height + boxTopOffset + boxBottomOffset
        )
        let inner = boxRect.insetBy(dx: borderWidth, dy: borderWidth)
        guard inner.width > 0, inner.height > 0 else { return nil }
        let radius = SectionBoxMetrics.clampedCornerRadius(cornerRadius - borderWidth, for: inner.size)
        guard radius > 0 else { return UIBezierPath(rect: inner) }
        return UIBezierPath(roundedRect: inner, cornerRadius: radius)
    }
}
#endif

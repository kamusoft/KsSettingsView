// KsCheckBoxView.swift
// KsSettingsViewUI
//
// `CheckboxCell` の右端に常設するカスタムチェックボックス View。
// オリジナル `AiForms.Maui.SettingsView/SettingsView/Native/iOS/Cells/CheckboxCellView.cs` の
// `CheckBox`（`UIButton` + `Draw`）相当を `UIView` ベースで再実装したもの。
// `CheckboxCell` のチェック表示は、システムの `UICellAccessory` ではなく本 View で描画する。

#if canImport(UIKit)
import UIKit

/// 角丸の四角いチェックボックスを描画するカスタム View。
///
/// - 20x20、`layer.cornerRadius = 3`、`layer.borderWidth = 2`。
/// - `isChecked == true`: accent カラーで塗りつぶし、白いチェックマーク（オリジナル座標比）を重ねる。
/// - `isChecked == false`: 背景透明、枠のみ。
///
/// 状態切り替えは accessory の追加・削除ではなく本 View 内部の `setNeedsDisplay()` 再描画で行う
/// （追加・削除に伴うスライドアニメーションを避けるため）。
/// タップ通知は Cell 全体タップ（`tapHandler`）経由のため、本 View は表示専用とする。
///
/// # `isEnabled` による disabled 表現
/// `UIView` は `isEnabled` を継承していないため、本 View では独自の boolean プロパティとして
/// 提供し、`false` のときは枠・塗り・チェックマークの描画色のアルファを下げて薄く描画する。
/// これにより呼び出し側（`CheckboxCellView`）が `checkBoxView.alpha = 0.5` を直接書く必要がなくなり、
/// 「Cell 全体ではなく内部チェック表示の disabled 表現」をコード上で明確化する
/// （Cell 全体を半透明化する方式は採らない）。
@MainActor
internal final class KsCheckBoxView: UIView {
    /// オリジナル互換のチェックボックス辺長。
    static let defaultSide: CGFloat = 20

    /// disabled 時の描画アルファ（accent 色・チェックマーク色に乗算）。
    private static let disabledColorAlpha: CGFloat = 0.5

    /// チェック状態。変更時に再描画する。
    var isChecked: Bool = false {
        didSet {
            guard isChecked != oldValue else { return }
            updateFill()
            setNeedsDisplay()
        }
    }

    /// accent カラー（枠・塗り・チェックマーク背景に使用）。変更時に再描画する。
    var accentColor: UIColor = .systemBlue {
        didSet {
            applyBorderColor()
            updateFill()
            setNeedsDisplay()
        }
    }

    /// 有効／無効状態。`false` のとき枠・塗り・チェックマークの色を薄く描画する。
    /// `UIView` 標準には `isEnabled` がないため独自定義（refine-basic-cells-style Suggestion-1 対応）。
    var isEnabled: Bool = true {
        didSet {
            guard isEnabled != oldValue else { return }
            applyBorderColor()
            updateFill()
            setNeedsDisplay()
        }
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        configure()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        configure()
    }

    private func configure() {
        backgroundColor = .clear
        isOpaque = false
        layer.cornerRadius = 3
        layer.borderWidth = 2
        applyBorderColor()
        layer.backgroundColor = UIColor.clear.cgColor

        // dark mode 等の color appearance 変化時に cgColor を再解決する。
        // iOS 17+ は deprecated な traitCollectionDidChange(_:) の代わりに
        // registerForTraitChanges(_:handler:) を使用する。
        if #available(iOS 17.0, *) {
            registerForTraitChanges([UITraitUserInterfaceStyle.self]) {
                (view: KsCheckBoxView, _: UITraitCollection) in
                view.resolveDynamicColors()
            }
        }
    }

    override var intrinsicContentSize: CGSize {
        CGSize(width: Self.defaultSide, height: Self.defaultSide)
    }

    /// checked 時は accent で塗りつぶし、unchecked 時は透明にする。
    /// `isEnabled == false` のときは accent 色をアルファ低下で薄く描画する。
    private func updateFill() {
        if isChecked {
            layer.backgroundColor = effectiveAccentColor().cgColor
        } else {
            layer.backgroundColor = UIColor.clear.cgColor
        }
    }

    /// `isEnabled` の状態を加味して枠線の cgColor を反映する。
    private func applyBorderColor() {
        layer.borderColor = effectiveAccentColor().cgColor
    }

    /// `isEnabled` を加味した実効 accent 色（disabled 時はアルファ低下）。
    /// `accentColor.withAlphaComponent(_:)` だけだと dark mode 変化時の自動再解決が効かないため、
    /// `resolvedColor(with:)` で trait 反映 → アルファ調整、の順で算出する。
    private func effectiveAccentColor() -> UIColor {
        let resolved = accentColor.resolvedColor(with: traitCollection)
        guard !isEnabled else { return resolved }
        return resolved.withAlphaComponent(Self.disabledColorAlpha)
    }

    /// border / 塗りの cgColor を現在の trait（dark mode 等）で再解決する。
    ///
    /// iOS 16 では traitCollectionDidChange(_:) からのフォールバックで呼び出される。
    /// iOS 17+ では configure() で登録した registerForTraitChanges(_:handler:) から呼び出される。
    private func resolveDynamicColors() {
        applyBorderColor()
        updateFill()
        setNeedsDisplay()
    }

    /// iOS 16 向けフォールバック。iOS 17+ では registerForTraitChanges(_:handler:) を使用する。
    @available(iOS, deprecated: 17.0, message: "iOS 17+ では registerForTraitChanges(_:handler:) を使用する")
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        guard #unavailable(iOS 17.0) else { return }
        if traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) {
            resolveDynamicColors()
        }
    }

    override func draw(_ rect: CGRect) {
        super.draw(rect)
        guard isChecked else { return }

        // オリジナル CheckBox.Draw 互換: 線幅は辺長の 1/10、座標比は 22/52 → 38/68 → 76/30。
        let size = bounds.size
        let lineWidth = size.width / 10

        let checkmark = UIBezierPath()
        checkmark.move(to: CGPoint(x: 22.0 / 100.0 * size.width, y: 52.0 / 100.0 * size.height))
        checkmark.addLine(to: CGPoint(x: 38.0 / 100.0 * size.width, y: 68.0 / 100.0 * size.height))
        checkmark.addLine(to: CGPoint(x: 76.0 / 100.0 * size.width, y: 30.0 / 100.0 * size.height))
        checkmark.lineWidth = lineWidth
        checkmark.lineCapStyle = .square
        checkmark.lineJoinStyle = .miter
        // disabled 時はチェックマーク自体も薄く描画する（accent 塗り全体のアルファ低下と整合させる）。
        let strokeColor: UIColor = isEnabled
            ? .white
            : UIColor.white.withAlphaComponent(Self.disabledColorAlpha)
        strokeColor.setStroke()
        checkmark.stroke()
    }
}
#endif

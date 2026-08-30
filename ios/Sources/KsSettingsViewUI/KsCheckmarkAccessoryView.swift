// KsCheckmarkAccessoryView.swift
// KsSettingsViewUI
//
// `RadioCell` / `SimpleCheckCell` の右端に常設する checkmark accessory View。
// 選択状態の切り替えは accessory の追加・削除ではなく `alpha` のフェードで行う。

#if canImport(UIKit)
import UIKit

/// 右端に常設する checkmark（SF Symbol "checkmark"）の accessory View。
///
/// 選択状態は `alpha`（1 / 0）で表現し、位置は不動のままフェードイン／アウトする。
/// accessory 自体は常設し、追加・削除に伴う横スライドアニメーションを生じさせない。
///
/// 注意（デグレ修正）: `UICellAccessory.customView` に `UIImageView` を直接渡し、その
/// `UIImageView` 自身の `alpha` を操作する方式では、UIKit が accessory を内部レイアウト
/// コンテナへ再配置する際に customView 直下の `alpha` 変更が反映されず、`isChecked == false`
/// の項目でも checkmark が表示されたままになる不具合が生じる。これを避けるため、本 View は
/// 「サイズ確保用のコンテナ（自身）＋内側に checkmark の `UIImageView`」という二層構造とし、
/// alpha フェードは **内側の `imageView`** に対して行う。コンテナ自身の `alpha` は常に 1 で、
/// レイアウト幅は checkmark の有無に依らず一定に確保される（位置不動）。
@MainActor
internal final class KsCheckmarkAccessoryView: UIView {
    /// disabled 時の tint アルファ（accent 色に乗算）。
    private static let disabledColorAlpha: CGFloat = 0.5

    /// 実際に checkmark を描画する内側の image view。alpha フェード対象。
    private let imageView: UIImageView

    /// 直近に `apply(selected:accent:animated:)` で指定された accent 色（trait 変化時の再着色に使用）。
    private var lastAccent: UIColor = .systemBlue

    /// 有効／無効状態。`false` のとき tintColor のアルファを下げて薄く表示する
    /// （refine-basic-cells-style Suggestion-1: 内部 View に disabled 表現を移譲）。
    /// `UIView` 標準には `isEnabled` がないため独自定義。
    var isEnabled: Bool = true {
        didSet {
            guard isEnabled != oldValue else { return }
            applyTintColor()
        }
    }

    init() {
        let symbol = UIImage(systemName: "checkmark")?.withRenderingMode(.alwaysTemplate)
        imageView = UIImageView(image: symbol)
        super.init(frame: .zero)

        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.alpha = 0
        addSubview(imageView)
        NSLayoutConstraint.activate([
            imageView.leadingAnchor.constraint(equalTo: leadingAnchor),
            imageView.trailingAnchor.constraint(equalTo: trailingAnchor),
            imageView.topAnchor.constraint(equalTo: topAnchor),
            imageView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    required init?(coder: NSCoder) {
        let symbol = UIImage(systemName: "checkmark")?.withRenderingMode(.alwaysTemplate)
        imageView = UIImageView(image: symbol)
        super.init(coder: coder)
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.alpha = 0
        addSubview(imageView)
        NSLayoutConstraint.activate([
            imageView.leadingAnchor.constraint(equalTo: leadingAnchor),
            imageView.trailingAnchor.constraint(equalTo: trailingAnchor),
            imageView.topAnchor.constraint(equalTo: topAnchor),
            imageView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    /// SF Symbol の固有サイズをコンテナのサイズとして採用する（accessory 幅の確保）。
    override var intrinsicContentSize: CGSize {
        imageView.intrinsicContentSize
    }

    /// 選択状態を反映する。
    ///
    /// - Parameters:
    ///   - selected: 選択中なら `true`（checkmark を表示）、非選択なら `false`（非表示）。
    ///   - accent: チェックマークの着色色。
    ///   - animated: `true` のときフェードで切り替える（同一セルの状態変化時）。
    ///     `false` のとき即時設定する（初回 bind / reuse 直後。チラつき回避）。
    func apply(selected: Bool, accent: UIColor, animated: Bool) {
        lastAccent = accent
        applyTintColor()
        let targetAlpha: CGFloat = selected ? 1.0 : 0.0
        guard animated else {
            imageView.alpha = targetAlpha
            return
        }
        guard imageView.alpha != targetAlpha else { return }
        UIView.animate(withDuration: 0.2) {
            self.imageView.alpha = targetAlpha
        }
    }

    /// `isEnabled` を加味して内側 checkmark の tint 色を反映する。
    /// disabled 時は accent 色のアルファを下げて薄く描画する。
    private func applyTintColor() {
        if isEnabled {
            imageView.tintColor = lastAccent
        } else {
            imageView.tintColor = lastAccent.withAlphaComponent(Self.disabledColorAlpha)
        }
    }

    /// テスト用: 内側 checkmark の現在 alpha。
    internal var checkmarkAlpha: CGFloat { imageView.alpha }

    /// テスト用: 内側 checkmark の現在 tint 色（`isEnabled` 反映後）。
    internal var checkmarkTintColor: UIColor? { imageView.tintColor }

    /// reuse 時の即時リセット用。
    internal func resetForReuse() {
        imageView.alpha = 0
    }
}
#endif

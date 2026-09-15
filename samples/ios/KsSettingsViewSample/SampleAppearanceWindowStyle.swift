// SampleAppearanceWindowStyle.swift
// KsSettingsViewSample
//
// 外観の選択を window の `overrideUserInterfaceStyle` として適用する仕組み。
//
// SwiftUI の `preferredColorScheme` は「上書きあり」から「上書きなし」(nil) へ戻したあと、
// 画面を表示したまま端末の外観が変わると、ナビゲーションバーの大タイトル・戻るボタン・
// ステータスバーが切り替え前の配色のまま残る（本文と UIKit 側の描画だけが追随する）。
// window の `overrideUserInterfaceStyle` を直接指定すると「システム」を `.unspecified` として
// 表現でき、以後の端末の外観変更が chrome を含む全体へそのまま伝わる。

import SwiftUI
import UIKit

extension View {

    /// 外観の選択をアプリの window へ適用する。
    func sampleAppearanceWindowStyle(_ appearance: SampleAppearance) -> some View {
        background(WindowAppearanceApplier(style: appearance.userInterfaceStyle))
    }
}

/// window の `overrideUserInterfaceStyle` を設定するためだけの、描画を持たない View。
private struct WindowAppearanceApplier: UIViewRepresentable {

    let style: UIUserInterfaceStyle

    func makeUIView(context: Context) -> WindowStyleApplyingView {
        WindowStyleApplyingView(style: style)
    }

    func updateUIView(_ uiView: WindowStyleApplyingView, context: Context) {
        uiView.style = style
    }
}

/// window へ接続された時点と選択の変更時に `overrideUserInterfaceStyle` を適用する。
private final class WindowStyleApplyingView: UIView {

    var style: UIUserInterfaceStyle {
        didSet { applyStyle() }
    }

    init(style: UIUserInterfaceStyle) {
        self.style = style
        super.init(frame: .zero)
        isUserInteractionEnabled = false
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        applyStyle()
    }

    /// 同値の代入でも trait の再評価が走るため、変化があるときだけ書き込む。
    private func applyStyle() {
        guard let window, window.overrideUserInterfaceStyle != style else { return }
        window.overrideUserInterfaceStyle = style
    }
}

// SampleIconBadge.swift
// KsSettingsViewSample
//
// 角丸の色付き四角に白いシンボルを載せた「バッジ型アイコン」を組み立てるヘルパ。
//
// Cell のアイコンは `KsImage` の画像をそのまま表示するため、アイコンの地色は利用者側で
// 画像に焼き込む。四角形の画像を渡すとアイコン列の幅がシンボルの字形に依存しなくなり、
// 行ごとの title の開始位置が揃う。角丸は `Theme.cellIconRadius` が担当する。
//
// `KsImage.uiImage` の等価判定は UIImage の参照同一性で行われるため、画像は static let で
// 1 度だけ生成した同じインスタンスを毎回渡す（毎回生成すると差分計算が常に「変更あり」になる）。
//
// 対応する Android 側定義:
// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleIconBadge.kt

import UIKit

/// Section 装飾デモが使うバッジ型アイコンの生成と実体。
@MainActor
enum SampleIconBadge {

    /// バッジの一辺（pt）。`Theme.cellIconSize` に渡す値と一致させる。
    nonisolated static let size: CGFloat = 29
    /// バッジの角丸半径（pt）。`Theme.cellIconRadius` に渡す値と一致させる。
    nonisolated static let cornerRadius: CGFloat = 7

    /// 機内モード（オレンジ地）。
    static let airplane = make(systemName: "airplane", background: SampleTheme.demoIconOrange)
    /// Wi-Fi（青地）。
    static let wifi = make(systemName: "wifi", background: SampleTheme.demoIconBlue)
    /// Bluetooth（明るい青地）。
    static let bluetooth = make(systemName: "antenna.radiowaves.left.and.right", background: SampleTheme.demoIconVividBlue)
    /// バッテリー（緑地）。
    static let battery = make(systemName: "battery.100percent", background: SampleTheme.demoAccentGreen)

    /// 地色の正方形に白いシンボルを中央配置した画像を作る。
    ///
    /// - Parameters:
    ///   - systemName: SF Symbols 名
    ///   - background: バッジの地色
    private static func make(systemName: String, background: UIColor) -> UIImage {
        let canvas = CGSize(width: size, height: size)
        return UIGraphicsImageRenderer(size: canvas).image { context in
            background.setFill()
            context.fill(CGRect(origin: .zero, size: canvas))

            let configuration = UIImage.SymbolConfiguration(pointSize: 15, weight: .semibold)
            guard let symbol = UIImage(systemName: systemName, withConfiguration: configuration)?
                .withTintColor(.white, renderingMode: .alwaysOriginal) else { return }
            let origin = CGPoint(
                x: (canvas.width - symbol.size.width) / 2,
                y: (canvas.height - symbol.size.height) / 2
            )
            symbol.draw(in: CGRect(origin: origin, size: symbol.size))
        }
    }
}

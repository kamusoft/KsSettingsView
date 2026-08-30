// SectionDecorationPreset.swift
// KsSettingsViewSample
//
// Section 装飾デモが切り替える Theme の Section 装飾 4 属性
// （sectionMargin / sectionCornerRadius / sectionBorderWidth / sectionBorderColor）の組。
//
// 各プリセットは色定義を持たず、共通の `SampleTheme.sectionDecorationDemo(...)` に
// 4 属性だけを渡して Theme を組み立てる（色値の二重管理を作らない）。
//
// 対応する Android 側定義:
// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SectionDecorationPreset.kt

import UIKit
import KsSettingsViewUI

/// Section 装飾 4 属性の組。デモ画面のプリセット切替で使う。
enum SectionDecorationPreset: String, CaseIterable, Identifiable {
    /// 4 属性すべて未指定。style ごとのライブラリ既定へ解決される。
    case standard
    /// 余白を広く・角丸を小さくした組。
    case wideMargin
    /// 既定の余白・角丸のままボーダーを指定した組。
    case bordered

    var id: String { rawValue }

    /// プリセット選択 UI に表示する名前。
    var title: String {
        switch self {
        case .standard: "既定"
        case .wideMargin: "余白広め・角丸小"
        case .bordered: "ボーダーあり"
        }
    }

    /// プリセットに対応する Theme。
    var theme: Theme {
        switch self {
        case .standard:
            SampleTheme.sectionDecorationDemo()
        case .wideMargin:
            SampleTheme.sectionDecorationDemo(
                sectionMargin: NSDirectionalEdgeInsets(top: 32, leading: 32, bottom: 0, trailing: 32),
                sectionCornerRadius: 8
            )
        case .bordered:
            SampleTheme.sectionDecorationDemo(
                sectionBorderWidth: 2,
                sectionBorderColor: SampleTheme.demoSectionBorder
            )
        }
    }
}

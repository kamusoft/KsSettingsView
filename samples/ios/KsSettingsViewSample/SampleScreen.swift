// SampleScreen.swift
// KsSettingsViewSample
//
// Sample アプリの画面一覧と表示名の一元定義。
//
// ルートメニューの項目文言と遷移先画面の `navigationTitle` は必ずこの `title` を参照する。
// 文言を2箇所に手書きすると表記ゆれが再発するため、定義はここ1箇所に閉じる。
//
// Sample はプラットフォーム間のパリティ検証装置であり、文言は全 platform で一致させる
// （cross/ADR-0016）。対応する Android 側定義:
// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleScreen.kt

import SwiftUI

/// Sample アプリの画面。表示名 (`title`) と遷移先 View を持つ。
enum SampleScreen: String, CaseIterable, Identifiable {
    case store
    case dsl
    case basicCells
    case inputCells
    case customCell
    case unifyCommonFields
    case visibility
    case sectionDecoration
    case minimalDiffable
    case sectionDecorationSpike

    var id: String { rawValue }

    /// ルートメニュー項目と画面タイトルに共通で使う表示名。
    var title: String {
        switch self {
        case .store: "Store 方式デモ"
        case .dsl: "DSL 方式デモ"
        case .basicCells: "基本 Cell 7 種デモ"
        case .inputCells: "入力 Cell 5 種デモ"
        case .customCell: "CustomCell デモ"
        case .unifyCommonFields: "共通フィールド統合デモ"
        case .visibility: "isVisible デモ（条件付き非表示）"
        case .sectionDecoration: "Section 装飾デモ（style 切替）"
        case .minimalDiffable: "Minimal Diffable 検証"
        case .sectionDecorationSpike: "Section 装飾 decoration 検証"
        }
    }

    /// ライブラリの使い方を示すデモ画面。プラットフォーム間で一致させる対象。
    static let demos: [SampleScreen] = [
        .store, .dsl, .basicCells, .inputCells, .customCell, .unifyCommonFields, .visibility,
        .sectionDecoration
    ]

    /// プラットフォーム固有の技術検証画面。デモ画面の集合には数えない。
    static let verifications: [SampleScreen] = [.minimalDiffable, .sectionDecorationSpike]

    /// 遷移先の画面。
    ///
    /// 各 View の `init()` は MainActor 分離されているため、このプロパティも `@MainActor` にする
    /// (nonisolated なままだと Swift 6 の concurrency チェックで警告になる)。
    /// 呼び出し元の ContentView は既に MainActor 文脈のため影響はない。
    @MainActor
    @ViewBuilder
    var destination: some View {
        switch self {
        case .store: StoreDemoView()
        case .dsl: DSLDemoView()
        case .basicCells: BasicCellsDemoView()
        case .inputCells: InputCellsDemoView()
        case .customCell: CustomCellDemoView()
        case .unifyCommonFields: UnifyCellCommonFieldsDemoView()
        case .visibility: VisibilityDemoView()
        case .sectionDecoration: SectionDecorationDemoView()
        case .minimalDiffable: MinimalDiffableDemoView()
        case .sectionDecorationSpike: SectionDecorationSpikeView()
        }
    }
}

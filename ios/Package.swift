// swift-tools-version: 5.10
// KsSettingsView iOS Native — モノレポのビルド入口
//
// 本パッケージは KsSettingsView iOS Native ライブラリ群（Core / UI / SwiftUI ラッパなど）の
// SwiftPM ルートとして配置される。後続の変更提案で `products` / `targets` を順次追加していく。

import PackageDescription

let package = Package(
    name: "KsSettingsView",
    platforms: [
        // ライブラリ自体の対象プラットフォームは iOS 16+。
        // `KsSettingsViewUI` / `KsSettingsViewSwiftUI` は `UIHostingConfiguration` を使うため
        // iOS 16 以上が必須。
        .iOS(.v16),
        // macOS 指定はテスト実行（swift test は macOS ホスト）のためのみ。
        // UI 系ターゲットは UIKit 依存のため、macOS では `#if canImport(UIKit)` でビルド対象から
        // 自然に除外される（テストコンパイルは可能、実行時は iOS シミュレータで行うのが原則）。
        .macOS(.v13)
    ],
    products: [
        // Core: SettingsRoot / Section / Cell 抽象のドメインモデル層（Theme / CellStyle は UI 層）
        .library(
            name: "KsSettingsViewCore",
            targets: ["KsSettingsViewCore"]
        ),
        // UI: UIKit ベースの UI 基盤（KsSettingsViewController、Cell レジストリ、Renderer、
        // Theme / CellStyle 等）
        .library(
            name: "KsSettingsViewUI",
            targets: ["KsSettingsViewUI"]
        ),
        // SwiftUI: SwiftUI ラッパ + DSL（KsSettingsView, SettingsRootBuilder 等）
        .library(
            name: "KsSettingsViewSwiftUI",
            targets: ["KsSettingsViewSwiftUI"]
        )
        // Bridge (`KsSettingsViewBridge`) は product として公開しない。
        // 利用経路は `ios/binding/` の Xcode project が生成する xcframework 経由のみであり、
        // 同名の product を公開すると自動生成 scheme が Xcode project の同名 target と衝突して、
        // `xcodebuild -scheme KsSettingsViewBridge` の解決先が非決定的になる。
    ],
    dependencies: [],
    targets: [
        // Core ターゲット: UIKit に依存しない純粋データモデル
        .target(
            name: "KsSettingsViewCore",
            path: "Sources/KsSettingsViewCore"
        ),
        // Core テストターゲット: XCTest
        .testTarget(
            name: "KsSettingsViewCoreTests",
            dependencies: ["KsSettingsViewCore"],
            path: "Tests/KsSettingsViewCoreTests"
        ),
        // UI ターゲット: UIKit ベースの ViewController / Cell 描画層
        .target(
            name: "KsSettingsViewUI",
            dependencies: ["KsSettingsViewCore"],
            path: "Sources/KsSettingsViewUI"
        ),
        // UI テストターゲット
        .testTarget(
            name: "KsSettingsViewUITests",
            dependencies: ["KsSettingsViewUI", "KsSettingsViewCore"],
            path: "Tests/KsSettingsViewUITests"
        ),
        // SwiftUI ターゲット: UIViewControllerRepresentable + DSL
        .target(
            name: "KsSettingsViewSwiftUI",
            dependencies: ["KsSettingsViewUI", "KsSettingsViewCore"],
            path: "Sources/KsSettingsViewSwiftUI"
        ),
        // SwiftUI テストターゲット
        .testTarget(
            name: "KsSettingsViewSwiftUITests",
            dependencies: ["KsSettingsViewSwiftUI", "KsSettingsViewUI", "KsSettingsViewCore"],
            path: "Tests/KsSettingsViewSwiftUITests"
        ),
        // Bridge ターゲット: `@objc` 互換 DTO と内部所有 Store を持つ interop 境界
        .target(
            name: "KsSettingsViewBridge",
            dependencies: ["KsSettingsViewUI", "KsSettingsViewCore"],
            path: "Sources/KsSettingsViewBridge"
        ),
        // Bridge テストターゲット
        .testTarget(
            name: "KsSettingsViewBridgeTests",
            dependencies: ["KsSettingsViewBridge", "KsSettingsViewUI", "KsSettingsViewCore"],
            path: "Tests/KsSettingsViewBridgeTests"
        )
    ]
)

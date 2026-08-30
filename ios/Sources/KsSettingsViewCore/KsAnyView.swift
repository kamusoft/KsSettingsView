// KsAnyView.swift
// KsSettingsViewCore
//
// 装飾領域（Root H/F、Section H/F の view ケース）に任意 View を格納するための
// 型消去ラッパ。`SwiftUI.View` と `UIView` の二択 backing を保持する。
//
// 重要事項:
//   - `KsAnyView` は `Hashable` / `Equatable` に意図的に準拠しない。
//     SwiftUI ジェネリック型・UIView ファクトリクロージャは値の等価性を意味のある形で
//     比較できないため、差分検出（`SettingsRoot` / `Section` の Hashable 計算）には
//     参加させない。
//   - 中身の更新は描画レイヤ（`UIHostingConfiguration` の再構成等）に委ねる。
//
// プラットフォーム注記:
//   - 本ライブラリの主ターゲットは iOS（UIKit 利用可）。
//   - テスト実行時のみ macOS ホストでも動かせるよう、`UIView` 参照部は
//     `#if canImport(UIKit)` でガードしている。macOS では `uiKit` ファクトリは利用不可。

import Foundation
import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// 装飾領域に任意 View を格納するための型消去ラッパ。
///
/// `SwiftUI.View` 系（`swiftUI(...)` ファクトリ）と `UIView` 系（`uiKit(...)` ファクトリ）の
/// 二択 backing を保持する。`Hashable` / `Equatable` には意図的に準拠せず、差分検出には
/// 参加しない。中身の更新は描画レイヤで吸収する。
public struct KsAnyView: @unchecked Sendable {
    /// backing 種別。`SwiftUI.View` 系か `UIView` 系の二択を保持する。
    ///
    /// 描画責務はすべて UI 層（`KsSettingsViewUI` 等）が担うため、UI 層からは
    /// 本 enum を `switch` してそれぞれのケースに応じた描画を行う。
    public enum Backing {
        /// SwiftUI View を `AnyView` に型消去して保持。
        case swiftUI(() -> AnyView)
        /// UIView ファクトリを保持（iOS / Mac Catalyst のみ）。
        #if canImport(UIKit)
        case uiKit(() -> UIView)
        #endif
    }

    /// backing。UI 層が `switch` して描画方式を分岐させるために公開する。
    ///
    /// 注: `Hashable` には参加しない。差分検出には使えない。
    public let backing: Backing

    /// SwiftUI `View` 系の `KsAnyView` を生成する。
    ///
    /// 利用者は `@ViewBuilder` クロージャ内で任意の SwiftUI View を返せる。
    /// 内部で `AnyView` に型消去するため、戻り値型の宣言は不要。
    /// - Parameter build: SwiftUI View を生成するクロージャ
    /// - Returns: SwiftUI backing の `KsAnyView`
    public static func swiftUI<V: SwiftUI.View>(
        @ViewBuilder _ build: @escaping () -> V
    ) -> KsAnyView {
        return KsAnyView(backing: .swiftUI { AnyView(build()) })
    }

    #if canImport(UIKit)
    /// UIKit `UIView` 系の `KsAnyView` を生成する。
    ///
    /// `factory` は描画タイミングで呼び出され、新規 `UIView` インスタンスを返す。
    /// - Parameter factory: `UIView` を生成するファクトリクロージャ
    /// - Returns: UIKit backing の `KsAnyView`
    public static func uiKit(_ factory: @escaping () -> UIView) -> KsAnyView {
        return KsAnyView(backing: .uiKit(factory))
    }
    #endif

    /// 内部初期化子（外部からは静的ファクトリ経由で生成する）。
    /// 公開ファクトリ（`swiftUI(_:)` / `uiKit(_:)`）以外からの直接構築を避けるため `internal`。
    internal init(backing: Backing) {
        self.backing = backing
    }
}

// KsImage.swift
// KsSettingsViewUI
//
// Cell のアイコン表現に用いる sealed 型（enum）。UI 層所属。
// プラットフォーム固有派生（`systemName` / `uiImage`）を持ち、UI 層が派生ごとに
// 解決ロジックを切り替える。
// `UIImage` を扱うため Core ではなく UI 層に置く（core/ADR-0009）。

#if canImport(UIKit)
import UIKit

/// Cell のアイコン表現に用いる sealed 値型。
///
/// プラットフォーム固有派生として以下を持つ：
///
/// - ``KsImage/systemName(_:)``: SF Symbols 名（例: `"bell"`、`"externaldrive"`）
/// - ``KsImage/uiImage(_:)``: 任意の `UIImage`（カスタムアセット等）
///
/// `Hashable` 実装は派生ごとに以下のとおり：
///
/// - `.systemName(s)`: 内部 String の hash 値で同定
/// - `.uiImage(img)`: 参照同一性（`ObjectIdentifier(img)` 相当）で同定
///
/// `@unchecked Sendable` の根拠：
/// - 全 case の associated value は `let` 相当（enum case は immutable）。
/// - `.systemName` の `String` は値型で Sendable。
/// - `.uiImage` の `UIImage` は Apple 実装上事実上 immutable で thread-safe（利用者が
///   `UIImage(cgImage: mutableImage)` のような可変 backing を渡さない限り安全）。
/// - Swift 6.2 の strict concurrency では `UIImage` が Sendable 適合していないため
///   `@unchecked` を明示する。
public enum KsImage: Hashable, @unchecked Sendable {
    /// SF Symbols 名を保持する派生。
    case systemName(String)

    /// 任意の `UIImage` を保持する派生。
    case uiImage(UIImage)

    // MARK: - Hashable / Equatable 手動実装

    public static func == (lhs: KsImage, rhs: KsImage) -> Bool {
        switch (lhs, rhs) {
        case let (.systemName(l), .systemName(r)):
            return l == r
        case let (.uiImage(l), .uiImage(r)):
            // UIImage は参照同一性で比較する（同一インスタンスのみ等価）
            return ObjectIdentifier(l) == ObjectIdentifier(r)
        case (.systemName, .uiImage), (.uiImage, .systemName):
            return false
        }
    }

    public func hash(into hasher: inout Hasher) {
        switch self {
        case let .systemName(name):
            hasher.combine(0)
            hasher.combine(name)
        case let .uiImage(image):
            hasher.combine(1)
            hasher.combine(ObjectIdentifier(image))
        }
    }
}
#endif

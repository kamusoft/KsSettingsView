// KsBridgeLabelCell.swift
// KsSettingsViewBridge
//
// interop 境界で `LabelCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation

/// 読み取り専用の表示 Cell (`LabelCell`) を interop 境界で輸送する DTO。
///
/// `LabelCell` は全 Cell 共通のフィールドだけで構成されるため、固有のフィールドを持たず
/// `KsBridgeCell` の共通フィールドと変換をそのまま使う。
@objc(KsBridgeLabelCell)
public final class KsBridgeLabelCell: KsBridgeCell {}
#endif

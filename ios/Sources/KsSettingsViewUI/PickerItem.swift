// PickerItem.swift
// KsSettingsViewUI
//
// `PickerCell` の候補1件を表す公開値型。主表示 `text` と任意の副表示 `subText` を持つ。

import Foundation

/// `PickerCell` の候補1件（主表示 + 任意の副表示）。
///
/// `subText` の空文字列は「副表示なし」と同義であり、初期化時に `nil` へ正規化される。
/// 選択面はこの正規化後の値を見て、副表示を持つ行だけを2行構成で描画する。
public struct PickerItem: Equatable, Hashable, Sendable {
    /// 主表示テキスト
    public let text: String
    /// 副表示テキスト（`nil` は副表示なし）
    public let subText: String?

    public init(text: String, subText: String? = nil) {
        self.text = text
        if let subText, !subText.isEmpty {
            self.subText = subText
        } else {
            self.subText = nil
        }
    }
}

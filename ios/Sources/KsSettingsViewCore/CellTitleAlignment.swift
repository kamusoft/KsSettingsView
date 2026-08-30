// CellTitleAlignment.swift
// KsSettingsViewCore
//
// `ButtonCell.titleAlignment` 等で使用するタイトル水平方向の揃え位置列挙。

import Foundation

/// タイトルの水平方向の揃え位置。
///
/// `ButtonCell.titleAlignment` などで使用する。プラットフォーム非依存の論理表現として
/// `start` / `center` / `end` の 3 ケースを提供する。UI 層側で `.start → .left` / `.end → .right`
/// のように変換する（RTL 環境では実プラットフォームの差し替えに委ねる）。
public enum CellTitleAlignment: Hashable, Sendable {
    /// 先頭寄せ（LTR では左寄せ、RTL では右寄せ）
    case start
    /// 中央寄せ
    case center
    /// 末尾寄せ（LTR では右寄せ、RTL では左寄せ）
    case end
}

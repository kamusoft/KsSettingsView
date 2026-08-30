// SampleMember.swift
// KsSettingsViewSample
//
// PickerCell の object 候補デモが並べる架空のメンバー。
// 主表示に名前、副表示に役割を射影して、任意の型を候補にできることを示す。

import Foundation

/// 通知先の候補として並べる架空のメンバー。
///
/// `role` が `nil` の要素は副表示を持たず、選択面では 1 行で描画される。
struct SampleMember: Equatable, Sendable {

    /// 主表示に使う名前。
    let name: String

    /// 副表示に使う役割。`nil` なら副表示なし。
    let role: String?
}

extension SampleMember {

    /// 入力 Cell デモの PickerCell（object 候補）セクションが並べる候補。
    ///
    /// 副表示の長さがばらばらで、副表示を持たない要素も混ざる並びにしてある。
    static let notificationTargets: [SampleMember] = [
        SampleMember(name: "佐藤 花子", role: "プロダクトマネージャー"),
        SampleMember(
            name: "鈴木 一郎",
            role: "モバイルアプリ開発チーム / テックリード (iOS・Android 横断アーキテクチャ担当)"
        ),
        SampleMember(name: "高橋 次郎", role: "QA エンジニア"),
        SampleMember(name: "全体アナウンス", role: nil),
        SampleMember(name: "田中 三郎", role: "デザイナー"),
    ]
}

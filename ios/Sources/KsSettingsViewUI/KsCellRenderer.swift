// KsCellRenderer.swift
// KsSettingsViewUI
//
// 任意の `KsCell` 準拠 Cell と `Theme` を受け取り `UICollectionViewCell` 上に描画するための
// 共通プロトコル。具象 `UICollectionViewCell` サブクラスが本プロトコルに準拠することで、
// `KsCellRegistry` から型として登録・解決される。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// Cell 描画契約。具象 `UICollectionViewCell` サブクラスが実装する。
///
/// `cell` は `any KsCell` として受け取り、レンダラ内部で具象型へキャストして使う想定。
/// `theme` は全体テーマ。`render` 実装は `theme` と Cell 個別のスタイルから描画値を解決してから
/// サブビューへ反映する順で行う（組み込み Cell の合成はライブラリ内部で閉じており、利用者定義の
/// Renderer は `Theme` の公開既定値から自前で解決する）。
public protocol KsCellRenderer: AnyObject {
    /// 任意の `KsCell` 準拠 Cell と `Theme` を受け取り描画する。
    /// - Parameters:
    ///   - cell: Core Cell モデル（具象型へのキャストはレンダラ実装が責任を持つ）
    ///   - theme: 全体テーマ
    func render(cell: any KsCell, theme: Theme)
}
#endif

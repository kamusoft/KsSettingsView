// KsCellRegistry.swift
// KsSettingsViewUI
//
// 具象 Cell 型 → `UICollectionViewCell` サブクラス（`KsCellRenderer` 実装）への
// 登録・解決を行う中央レジストリ。
//
// Cell 型と描画クラスの対応をここに集約することで、`KsSettingsViewController` は
// 具象 Cell 型を知らずに描画できる。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

/// Cell 型 → `UICollectionViewCell` サブクラス（`KsCellRenderer` 実装）への解決を担うレジストリ。
///
/// アプリ起動時または `KsSettingsViewController` 初期化時に
/// `register(cellType:rendererType:)` で登録しておくと、DataSource の cell provider から
/// 動的に解決される。`shared` シングルトンを既定として使うが、テスト容易性のため
/// 任意のインスタンスも生成・利用可能。
public final class KsCellRegistry: @unchecked Sendable {
    /// プロセス共通のシングルトン。
    /// アプリ起動時の登録はこちらに対して行うのが基本。
    public static let shared = KsCellRegistry()

    /// Cell 型 ID（`ObjectIdentifier(type)`）→ Renderer 型のマップ。
    /// `ObjectIdentifier` を使うのは、`any KsCell.Type` を辞書キーにできない（Hashable 制約上）ため。
    private var renderers: [ObjectIdentifier: UICollectionViewCell.Type] = [:]

    /// 排他制御用ロック（複数スレッドからの登録/解決を保護）
    private let lock = NSLock()

    /// 公開可能な `init`。テスト用に独立インスタンスを生成可能。
    public init() {}

    /// 具象 Cell 型に対する Renderer 型を登録する。
    /// - Parameters:
    ///   - cellType: Core Cell 型（`KsCell` 準拠の具象型）
    ///   - rendererType: 描画用 `UICollectionViewCell` サブクラス（`KsCellRenderer` 準拠）
    public func register<C: KsCell, R: UICollectionViewCell & KsCellRenderer>(
        cellType: C.Type,
        rendererType: R.Type
    ) {
        lock.lock()
        defer { lock.unlock() }
        renderers[ObjectIdentifier(cellType)] = rendererType
    }

    /// 任意の Cell インスタンスから Renderer 型を解決する。
    ///
    /// - Returns: 登録済みの場合は対応する `UICollectionViewCell` サブクラス、未登録なら `nil`。
    /// - Note: 未登録時の挙動（assertion failure / プレースホルダ）は呼び出し側 DataSource が決定する。
    public func resolveRendererType(for cell: any KsCell) -> UICollectionViewCell.Type? {
        lock.lock()
        defer { lock.unlock() }
        let key = ObjectIdentifier(type(of: cell))
        return renderers[key]
    }

    /// テスト等で登録内容を初期化したい場合に使うリセット関数。
    public func removeAll() {
        lock.lock()
        defer { lock.unlock() }
        renderers.removeAll()
    }
}
#endif

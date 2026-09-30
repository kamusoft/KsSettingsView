// KsScrollControlling.swift
// KsSettingsViewUI
//
// スクロール命令ハンドルの protocol と、引数の既定値を与える extension。

import Foundation

/// 設定画面へスクロール命令を送るハンドルの protocol です。
///
/// `KsScrollController` が準拠します。ViewModel がこの型で命令ハンドルを持てば、テストで
/// 命令を記録するだけの実装に差し替えられます。
///
/// 既定値付きで呼べる形 (`scrollTo(id:)`・`scrollToEnd()` など) は extension が提供します。
@MainActor
public protocol KsScrollControlling: AnyObject {
    /// 指定した ID の Cell の行へスクロールします。
    ///
    /// - Parameters:
    ///   - id: Cell の ID。UIKit の Host と Store を使う画面では Cell の `KsCellID`、宣言的に書いた
    ///     画面では `.cellID(_:)` で付けた ID か `ForEach` の key
    ///   - position: 行を表示範囲のどこへ合わせるか
    ///   - animated: アニメーションするか
    func scrollTo(id: some Hashable, position: KsScrollPosition, animated: Bool)

    /// 指定した ID の Section へ、見出しごと見えるようにスクロールします。
    ///
    /// - Parameters:
    ///   - id: Section の ID。UIKit の Host と Store を使う画面では Section の `id`、宣言的に書いた
    ///     画面では `.sectionID(_:)` で付けた ID か `ForEach` の key
    ///   - position: Section の範囲 (見出し・Cell・Footer) を表示範囲のどこへ合わせるか
    ///   - animated: アニメーションするか
    func scrollToSection(id: some Hashable, position: KsScrollPosition, animated: Bool)

    /// 内容の最上端 (Root Header を含む) へスクロールします。
    func scrollToStart(animated: Bool)

    /// 内容の最下端 (Root Footer を含む) へスクロールします。
    func scrollToEnd(animated: Bool)
}

extension KsScrollControlling {
    /// 指定した ID の Cell の行の上端を、アニメーションしながら表示範囲の上端へ合わせます。
    public func scrollTo(id: some Hashable) {
        scrollTo(id: id, position: .start, animated: true)
    }

    /// 指定した ID の Cell の行を、アニメーションしながら指定した位置へ合わせます。
    public func scrollTo(id: some Hashable, position: KsScrollPosition) {
        scrollTo(id: id, position: position, animated: true)
    }

    /// 指定した ID の Cell の行の上端を表示範囲の上端へ合わせます。
    public func scrollTo(id: some Hashable, animated: Bool) {
        scrollTo(id: id, position: .start, animated: animated)
    }

    /// 指定した ID の Section の範囲の上端を、アニメーションしながら表示範囲の上端へ合わせます。
    public func scrollToSection(id: some Hashable) {
        scrollToSection(id: id, position: .start, animated: true)
    }

    /// 指定した ID の Section の範囲を、アニメーションしながら指定した位置へ合わせます。
    public func scrollToSection(id: some Hashable, position: KsScrollPosition) {
        scrollToSection(id: id, position: position, animated: true)
    }

    /// 指定した ID の Section の範囲の上端を表示範囲の上端へ合わせます。
    public func scrollToSection(id: some Hashable, animated: Bool) {
        scrollToSection(id: id, position: .start, animated: animated)
    }

    /// アニメーションしながら内容の最上端へスクロールします。
    public func scrollToStart() {
        scrollToStart(animated: true)
    }

    /// アニメーションしながら内容の最下端へスクロールします。
    public func scrollToEnd() {
        scrollToEnd(animated: true)
    }
}

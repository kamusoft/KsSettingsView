// KeyWindowResolver.swift
// KsSettingsViewUI
//
// モーダル提示で利用する「キーウィンドウのルートビューコントローラ」を取得する
// 共通ユーティリティ。`PickerCellView` / `NumberPickerCellView` / `TimePickerCellView` /
// `DatePickerCellView` の 4 種類のセルビューが共通で使う。
//
// 元々は各 CellView に同等の private 関数として重複定義されていたが、ロジックは完全に同一
// （`UIApplication.connectedScenes` から `UIWindowScene` を取り出し、`isKeyWindow` なウィンドウ
// のルート VC を返す。さらに最前面まで `presentedViewController` を辿る）であるため、
// 内部 utility として一本化した。
//
// スレッド要件:
//   - `UIApplication.shared` / `UIWindow` 系 API は MainActor 隔離が必要なため、
//     関数全体を `@MainActor` で隔離する。

#if canImport(UIKit)
import UIKit

/// キーウィンドウのルートビューコントローラ解決ユーティリティ。
internal enum KeyWindowResolver {
    /// 接続中シーンのうち key なウィンドウのルート VC を、さらに最前面まで辿って返す。
    ///
    /// 解決順:
    /// 1. `UIApplication.shared.connectedScenes` の `UIWindowScene` を走査
    /// 2. `isKeyWindow == true` のウィンドウを優先、無ければ最初のウィンドウ
    /// 3. `rootViewController` から `presentedViewController` を辿り最前面を返す
    @MainActor
    static func topPresentedViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
        for scene in scenes {
            guard let ws = scene as? UIWindowScene else { continue }
            guard let key = ws.windows.first(where: { $0.isKeyWindow }) ?? ws.windows.first else {
                continue
            }
            return key.rootViewController?.topMostPresented()
        }
        return nil
    }
}

/// `presentedViewController` を辿って最前面の VC を返す内部拡張。
///
/// 元は `PickerCellView.swift` 内の private extension として定義されていたが、
/// `KeyWindowResolver` からも利用するため fileprivate を昇格して同一ファイル内の
/// internal 拡張として集約する。
@MainActor
internal extension UIViewController {
    /// 自身を起点に `presentedViewController` チェーンの末端まで辿る。
    func topMostPresented() -> UIViewController {
        var top: UIViewController = self
        while let p = top.presentedViewController {
            top = p
        }
        return top
    }
}
#endif

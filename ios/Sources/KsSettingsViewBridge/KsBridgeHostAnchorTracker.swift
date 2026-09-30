// KsBridgeHostAnchorTracker.swift
// KsSettingsViewBridge
//
// Host が window から外れる直前の表示位置を控える、Host の view に置く見えない view。

#if canImport(UIKit)
import UIKit
import KsSettingsViewUI

/// Host が window から外れる直前に、その表示位置を控える。
///
/// window から外れた後の Host は、レイアウトが window の寸法と安全領域を失い、利用者が見ていた位置と
/// 一致しない (MAUI の View を置いた見出しなどは高さも変わる)。MAUI の Handler の切断はページが画面から
/// 外れた後に届くため、解放の時点で控えると別の位置を控えてしまう。外れる直前の位置を、解放の時点の
/// 控えとして使う。
///
/// 外れる直前を知るため、Host の view の子として置く (子の `willMove(toWindow:)` は、祖先が window から
/// 外れる前に届く)。見えず、操作も受けず、アクセシビリティにも現れない。
internal final class KsBridgeHostAnchorTracker: UIView {

    /// 控える対象の Host。Host の view がこの view を保持するため弱参照で持つ。
    private weak var host: KsSettingsViewController?

    /// window から外れる直前に控えた位置。window に取り付けられている間は `nil`。
    internal private(set) var anchorAtWindowExit: KsScrollAnchor?

    /// Host の view の子として取り付ける。
    /// - Parameter host: 控える対象の Host
    init(host: KsSettingsViewController) {
        self.host = host
        super.init(frame: .zero)
        isHidden = true
        isUserInteractionEnabled = false
        isAccessibilityElement = false
        host.view.addSubview(self)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    /// Host を手放す時点の控えを返す。
    ///
    /// window に取り付けられていれば Host の現在の位置を控える。外れていれば、外れる直前に控えた位置を
    /// 返す。一度も取り付けられていなければ Host の控え (未実行の位置の復元があればその控え) を返す。
    func anchorForRelease() -> KsScrollAnchor? {
        guard let host else { return nil }
        if host.viewIfLoaded?.window == nil, let anchorAtWindowExit {
            return anchorAtWindowExit
        }
        return host.captureScrollAnchor()
    }

    /// Host への取り付けを外す。
    func stop() {
        removeFromSuperview()
        anchorAtWindowExit = nil
    }

    override func willMove(toWindow newWindow: UIWindow?) {
        if newWindow == nil, window != nil {
            anchorAtWindowExit = host?.captureScrollAnchor()
        }
        super.willMove(toWindow: newWindow)
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        // window に戻れば Host から位置を控え直せるため、外れる直前の控えは捨てる。
        if window != nil {
            anchorAtWindowExit = nil
        }
    }
}
#endif

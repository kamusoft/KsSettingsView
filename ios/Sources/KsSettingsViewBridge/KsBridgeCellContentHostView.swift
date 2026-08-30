// KsBridgeCellContentHostView.swift
// KsSettingsViewBridge
//
// interop 境界で受け取った `UIView` を Cell の内容として抱える入れ物。

#if canImport(UIKit)
import Foundation
import UIKit

/// 行の内容の view を抱え、描画側が内容を作り直す動きから切り離す入れ物。
///
/// interop 境界を越えて渡される内容の実体は 1 つきりのインスタンスであり、複数の行が同時に
/// 抱えることはできない。描画側が作り直す単位をこの入れ物にすることで、作り直しの後始末で
/// 親から外されるのは入れ物だけになり、内容の実体が別の行から奪われることがなくなる。
///
/// それでも内容は入れ物の間を移動するため、「今どの入れ物が抱えるか」の決め方を一箇所に定める:
///
/// - 行の内容として作られたばかりの入れ物は無条件に引き取る (その行がこれから描かれるため)
/// - それ以外の機会では、内容がどこにも付いていないか、抱えている相手が表に出ていないときだけ、
///   しかも自分が表に出ているときだけ引き取る
///
/// 後者は「表に出ている入れ物からは決して奪わない」を意味するため、2 つの入れ物が内容を
/// 取り合って揺れ続けることがない。
///
/// 引き取りの機会は配置 (`layoutSubviews`) 任せにしない。内容を奪われた側には必ず
/// `willRemoveSubview` が届くので、それを合図に配置をやり直させ、表示中の行が内容を失ったまま
/// 固定されないようにする。
internal final class KsBridgeCellContentHostView: UIView {

    /// 抱えている内容の view。
    private var content: UIView?

    /// 直近に上位へ伝えた内容の高さ。
    private var reportedHeight: CGFloat = UIView.noIntrinsicMetric

    /// 生存している入れ物の一覧 (弱参照)。
    ///
    /// 「抱え主が表から外れた」ことは、同じ内容を待っている入れ物には何も届かない。表示状態が
    /// 変わった入れ物からこの一覧をたどって知らせることで、引き取りの機会がどの順序でも必ず来る。
    private static let liveHosts = NSHashTable<KsBridgeCellContentHostView>.weakObjects()

    override init(frame: CGRect) {
        super.init(frame: frame)
        Self.liveHosts.add(self)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    /// 内容の view を抱えて取り付ける。
    ///
    /// 行の内容として作られたばかりの入れ物が使う経路であり、内容が他所に付いていても引き取る。
    /// - Parameter view: 行の内容として表示する view (`nil` で内容なし)
    func hold(_ view: UIView?) {
        store(view)

        if let view, view.superview !== self {
            adopt(view)
        }
    }

    /// 抱えている内容を確かめ直す。
    ///
    /// 描画側は既にある入れ物に対しても内容を伝え直すため、ここで無条件に引き取ると、退役間際の
    /// 行が表示中の行から内容を奪ってしまう。引き取ってよい場合だけ引き取る。
    /// - Parameter view: 行の内容として表示する view (`nil` で内容なし)
    func refresh(_ view: UIView?) {
        store(view)

        guard let view, view.superview !== self, canAdopt(view) else {
            return
        }

        adopt(view)
    }

    /// 抱える内容を控える。
    /// - Parameter view: 行の内容として表示する view (`nil` で内容なし)
    private func store(_ view: UIView?) {
        guard content !== view else {
            return
        }

        content = view
        reportedHeight = view?.intrinsicContentSize.height ?? UIView.noIntrinsicMetric
    }

    /// 内容の view を自分の子として取り付け直す。
    ///
    /// 内容は自分で必要な高さを答える view であり、その高さが行の高さになる。制約で四辺を留めて
    /// おくと、内容が必要サイズの答えを取り直させたときに入れ物の配置もやり直され、行の高さの
    /// 測り直しまで伝わる。下辺の優先度を下げてあるのは、描画側が内容より低い高さを与えた場合に
    /// 制約が矛盾しないようにするため。
    /// - Parameter view: 取り付ける view
    private func adopt(_ view: UIView) {
        view.removeFromSuperview()
        addSubview(view)

        view.translatesAutoresizingMaskIntoConstraints = false
        let bottom = view.bottomAnchor.constraint(equalTo: bottomAnchor)
        bottom.priority = .defaultHigh
        NSLayoutConstraint.activate([
            view.topAnchor.constraint(equalTo: topAnchor),
            view.leadingAnchor.constraint(equalTo: leadingAnchor),
            view.trailingAnchor.constraint(equalTo: trailingAnchor),
            bottom,
        ])
    }

    /// 内容を引き取ってよいか。
    ///
    /// どこにも付いていない内容は誰の表示も壊さないので引き取ってよい。誰かが抱えているなら、
    /// その相手が表に出ておらず、かつ自分が表に出ているときだけ引き取る。表に出ている相手からは
    /// 決して奪わないため、取り合いが振動しない。
    /// - Parameter view: 引き取りたい内容
    /// - Returns: 引き取ってよいなら `true`
    private func canAdopt(_ view: UIView) -> Bool {
        guard let holder = view.superview else {
            return true
        }

        return isOnScreen && !Self.isOnScreen(holder)
    }

    /// 内容が答える必要サイズをそのまま自分の必要サイズとして答える。
    override var intrinsicContentSize: CGSize {
        guard let content else {
            return super.intrinsicContentSize
        }
        return content.intrinsicContentSize
    }

    override func sizeThatFits(_ size: CGSize) -> CGSize {
        guard let content else {
            return super.sizeThatFits(size)
        }
        return content.sizeThatFits(size)
    }

    /// 内容が自分から外されたら、配置をやり直させて引き取りを確かめ直す。
    ///
    /// 別の入れ物が内容を引き取る瞬間に必ず届く合図であり、これを起点に配置を予約しておくことで、
    /// 表示中の行が内容を失ったまま次の配置の機会を待ち続ける状態が起きない。引き取った相手が
    /// 表示に出るかどうかはこの時点では決まっていないため、判断は配置のときまで遅らせる。
    /// - Parameter subview: 外される子 view
    override func willRemoveSubview(_ subview: UIView) {
        super.willRemoveSubview(subview)

        // window にいない入れ物は引き取りの対象外なので、配置を予約しても意味がない。
        if subview === content, window != nil {
            setNeedsLayout()
        }
    }

    /// 表示への出入りでも引き取りを確かめ直す。
    ///
    /// 表に出た入れ物が内容を持っていない場合はここで取り戻しに向かい、表から外れた入れ物が
    /// 内容を抱えたままの場合は、次に表に出る入れ物が引き取れる状態になったことになる。
    override func didMoveToWindow() {
        super.didMoveToWindow()

        guard let content else {
            return
        }

        setNeedsLayout()

        // 自分の表示状態が変わった分、同じ内容を待つ入れ物の判断も変わり得るので確かめ直させる。
        for host in Self.liveHosts.allObjects where host !== self && host.content === content {
            host.setNeedsLayout()
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        guard let content else {
            return
        }

        // 表示に出ているのに内容を持っていない状態は、引き取ってよければ取り戻して直す。
        if content.superview !== self, canAdopt(content) {
            adopt(content)
        }

        // 内容の必要な高さが変わったら、自分の必要サイズの答えも取り直させて上位へ伝える。
        let height = content.intrinsicContentSize.height
        if height != reportedHeight {
            reportedHeight = height
            invalidateIntrinsicContentSize()
        }
    }

    /// 実際に画面へ出ているかどうか (再利用待ちの行は表に出ていない)。
    private var isOnScreen: Bool {
        Self.isOnScreen(self)
    }

    /// view が実際に画面へ出ているかどうか。
    /// - Parameter view: 判定する view
    /// - Returns: window に属し、自分にも祖先にも隠されたものがなければ `true`
    private static func isOnScreen(_ view: UIView) -> Bool {
        guard view.window != nil else {
            return false
        }

        var current: UIView? = view
        while let value = current {
            if value.isHidden {
                return false
            }
            current = value.superview
        }
        return true
    }
}
#endif

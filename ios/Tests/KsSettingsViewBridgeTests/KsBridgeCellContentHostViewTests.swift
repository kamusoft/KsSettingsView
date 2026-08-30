// KsBridgeCellContentHostViewTests.swift
// KsSettingsViewBridgeTests
//
// 行の内容の view を抱える入れ物が、行の再利用で内容の取り合いが起きても
// 表示中の行から内容を失わせないことを確認する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewBridge

/// 高さを自分で答える観測用の内容 view。
private final class SizedContentView: UIView {

    var contentHeight: CGFloat = 40

    override var intrinsicContentSize: CGSize {
        CGSize(width: UIView.noIntrinsicMetric, height: contentHeight)
    }
}

@MainActor
final class KsBridgeCellContentHostViewTests: XCTestCase {

    // MARK: - 取り付け

    func test_抱えた内容が子として取り付けられる() {
        let content = SizedContentView()
        let host = KsBridgeCellContentHostView()

        host.hold(content)

        XCTAssertTrue(content.superview === host, "抱えた内容が入れ物の子になっていない")
    }

    func test_別の親を持つ内容も取り付け直される() {
        let content = SizedContentView()
        let other = UIView()
        other.addSubview(content)
        let host = KsBridgeCellContentHostView()

        host.hold(content)

        XCTAssertTrue(content.superview === host)
        XCTAssertTrue(other.subviews.isEmpty, "元の親から外れていない")
    }

    func test_同じ内容を抱え直しても取り付けは変わらない() {
        let content = SizedContentView()
        let host = KsBridgeCellContentHostView()
        host.hold(content)

        host.hold(content)
        host.hold(content)

        XCTAssertEqual(host.subviews.count, 1, "抱え直しで子が増えている")
        XCTAssertEqual(
            host.constraints.count,
            4,
            "抱え直しのたびに制約が積み上がっている"
        )
    }

    // MARK: - 内容の確かめ直し

    func test_確かめ直しでどこにも付いていない内容を取り付ける() {
        let content = SizedContentView()
        let host = KsBridgeCellContentHostView()
        host.hold(content)
        content.removeFromSuperview()

        host.refresh(content)

        XCTAssertTrue(content.superview === host, "行き場のない内容が取り付けられていない")
    }

    /// 表に出ていない入れ物は、確かめ直しで表示中の行から内容を奪わない。
    func test_表に出ていない入れ物は確かめ直しで内容を奪わない() {
        let content = SizedContentView()
        let pooledHost = KsBridgeCellContentHostView()
        pooledHost.hold(content)

        let visibleHost = KsBridgeCellContentHostView()
        visibleHost.hold(content)
        XCTAssertTrue(content.superview === visibleHost, "前提: 新しい入れ物が内容を引き取っていない")

        pooledHost.refresh(content)

        XCTAssertTrue(
            content.superview === visibleHost,
            "退役間際の入れ物が表示中の行から内容を奪っている"
        )
    }

    // MARK: - 行の再利用での取り合い

    /// 前の行の描画が片付けられても、内容を引き取った行から内容が外れない。
    ///
    /// 内容の実体は 1 つきりで行をまたいで移動するため、前の行の片付けが遅れて起きると
    /// 表示中の行から内容が奪われ得る。片付けの対象が入れ物だけであることを確かめる。
    func test_前の行の片付けで内容が奪われない() {
        let content = SizedContentView()
        let previousRow = UIView()
        var previousHost: KsBridgeCellContentHostView? = KsBridgeCellContentHostView()
        previousRow.addSubview(previousHost!)
        previousHost?.hold(content)

        let currentRow = UIView()
        let currentHost = KsBridgeCellContentHostView()
        currentRow.addSubview(currentHost)
        currentHost.hold(content)
        XCTAssertTrue(content.superview === currentHost, "前提: 新しい行が内容を引き取っていない")

        // 前の行の描画が後から片付けられる状況を作る。
        previousHost?.removeFromSuperview()
        previousHost = nil

        XCTAssertTrue(
            content.superview === currentHost,
            "前の行の片付けで、表示中の行から内容が外れている"
        )
    }

    /// 表示に出ている行は、内容が他所へ移っていたら配置のときに取り戻す。
    func test_表示中の行は他所へ移った内容を取り戻す() {
        let content = SizedContentView()
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        let host = KsBridgeCellContentHostView()
        host.frame = CGRect(x: 0, y: 0, width: 320, height: 40)
        window.addSubview(host)
        window.makeKeyAndVisible()
        host.hold(content)

        let thief = UIView()
        thief.addSubview(content)
        XCTAssertTrue(content.superview === thief, "前提: 内容が他所へ移っていない")

        host.setNeedsLayout()
        host.layoutIfNeeded()

        XCTAssertTrue(content.superview === host, "表示中の行が内容を取り戻していない")
    }

    /// 表に出ていない (再利用待ちの) 行は、表示中の行から内容を奪い返さない。
    func test_表に出ていない行は内容を奪い返さない() {
        let content = SizedContentView()
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 320, height: 200))

        let pooledRow = UIView()
        pooledRow.frame = CGRect(x: 0, y: 0, width: 320, height: 40)
        window.addSubview(pooledRow)
        let pooledHost = KsBridgeCellContentHostView()
        pooledHost.frame = pooledRow.bounds
        pooledRow.addSubview(pooledHost)
        window.makeKeyAndVisible()
        pooledHost.hold(content)

        let visibleHost = KsBridgeCellContentHostView()
        visibleHost.frame = CGRect(x: 0, y: 40, width: 320, height: 40)
        window.addSubview(visibleHost)
        visibleHost.hold(content)
        XCTAssertTrue(content.superview === visibleHost, "前提: 表示中の行が内容を持っていない")

        // 再利用待ちの行は隠されている。
        pooledRow.isHidden = true
        pooledHost.setNeedsLayout()
        pooledHost.layoutIfNeeded()

        XCTAssertTrue(
            content.superview === visibleHost,
            "再利用待ちの行が表示中の行から内容を奪い返している"
        )
    }

    /// 内容を奪われた表示中の行は、配置の機会が来るのを待たずに自分で配置を予約する。
    ///
    /// 奪われたことは `willRemoveSubview` で必ず届く。これを合図にしないと、スクロールが
    /// 落ち着いて配置が起きない状況で空行のまま固定される。
    func test_内容を奪われた表示中の行は自分で配置を予約する() {
        let content = SizedContentView()
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        let visibleHost = KsBridgeCellContentHostView()
        visibleHost.frame = CGRect(x: 0, y: 0, width: 320, height: 40)
        window.addSubview(visibleHost)
        window.makeKeyAndVisible()
        visibleHost.hold(content)
        window.layoutIfNeeded()

        // 再利用のために作られた入れ物が内容を引き取っていく。
        let pooledHost = KsBridgeCellContentHostView()
        pooledHost.hold(content)
        XCTAssertTrue(content.superview === pooledHost, "前提: 新しい入れ物が内容を引き取っていない")

        // 表示中の行は自分で配置を予約しているので、次の配置で取り戻す。
        window.layoutIfNeeded()

        XCTAssertTrue(
            content.superview === visibleHost,
            "内容を奪われた表示中の行が取り戻していない"
        )
    }

    /// 表示に出た入れ物は、内容が他所にあれば取り戻す。
    func test_表示に出た入れ物は内容を取り戻す() {
        let content = SizedContentView()
        let pooledHost = KsBridgeCellContentHostView()
        pooledHost.hold(content)

        let host = KsBridgeCellContentHostView()
        host.hold(content)
        pooledHost.hold(content)
        XCTAssertTrue(content.superview === pooledHost, "前提: 内容が他所へ移っていない")

        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        host.frame = CGRect(x: 0, y: 0, width: 320, height: 40)
        window.addSubview(host)
        window.makeKeyAndVisible()
        window.layoutIfNeeded()

        XCTAssertTrue(content.superview === host, "表示に出た入れ物が内容を取り戻していない")
    }

    /// 抱え主が表から外れたら、同じ内容を待っている入れ物へ引き取りの機会が渡る。
    ///
    /// 表に出ている入れ物からは奪わない決まりのため、待つ側は抱え主が表から外れたことを
    /// 自力では知れない。抱え主の側から知らせないと空行のまま固定される。
    func test_抱え主が表から外れたら待っている入れ物が引き取る() {
        let content = SizedContentView()
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 320, height: 200))

        let holderHost = KsBridgeCellContentHostView()
        holderHost.frame = CGRect(x: 0, y: 0, width: 320, height: 40)
        window.addSubview(holderHost)

        let waitingHost = KsBridgeCellContentHostView()
        waitingHost.frame = CGRect(x: 0, y: 40, width: 320, height: 40)
        window.addSubview(waitingHost)
        window.makeKeyAndVisible()

        holderHost.hold(content)
        waitingHost.refresh(content)
        window.layoutIfNeeded()
        XCTAssertTrue(
            content.superview === holderHost,
            "前提: 表に出ている入れ物から内容を奪っている"
        )

        // 抱え主が表から外れる (行が退役する)。
        holderHost.removeFromSuperview()
        window.layoutIfNeeded()

        XCTAssertTrue(
            content.superview === waitingHost,
            "抱え主が表から外れても待っている入れ物へ内容が渡っていない"
        )
    }

    // MARK: - 高さ

    func test_必要な高さは内容の答えをそのまま返す() {
        let content = SizedContentView()
        content.contentHeight = 123
        let host = KsBridgeCellContentHostView()
        host.hold(content)

        XCTAssertEqual(host.intrinsicContentSize.height, 123)

        content.contentHeight = 45
        content.invalidateIntrinsicContentSize()

        XCTAssertEqual(host.intrinsicContentSize.height, 45, "内容の高さの変化が入れ物へ伝わらない")
    }

    func test_内容なしでは高さを答えない() {
        let host = KsBridgeCellContentHostView()

        XCTAssertEqual(host.intrinsicContentSize.height, UIView.noIntrinsicMetric)
    }
}
#endif

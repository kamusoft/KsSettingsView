// CellTitleAlignmentTests.swift
// KsSettingsViewCoreTests
//
// `CellTitleAlignment` が start / center / end の 3 ケースを持ち、Hashable の等価性契約を
// 満たすことを検証する。

import XCTest
@testable import KsSettingsViewCore

final class CellTitleAlignmentTests: XCTestCase {

    func test_3ケースが定義されている() {
        // 全ケースが参照可能であること
        let s: CellTitleAlignment = .start
        let c: CellTitleAlignment = .center
        let e: CellTitleAlignment = .end
        XCTAssertNotEqual(s, c)
        XCTAssertNotEqual(s, e)
        XCTAssertNotEqual(c, e)
    }

    func test_Hashable_等価性契約() {
        XCTAssertEqual(CellTitleAlignment.center, CellTitleAlignment.center)
        XCTAssertEqual(CellTitleAlignment.start.hashValue, CellTitleAlignment.start.hashValue)
        XCTAssertEqual(CellTitleAlignment.end.hashValue, CellTitleAlignment.end.hashValue)
        // Set 格納可能
        let set: Set<CellTitleAlignment> = [.start, .center, .end, .start]
        XCTAssertEqual(set.count, 3)
    }
}

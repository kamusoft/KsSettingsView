// AccessoryTargetTests.swift
// KsSettingsViewCoreTests
//
// `AccessoryTarget` が Root Header / Root Footer / Section Header / Section Footer の
// 4 位置を値として区別し、Section ケースでは `sectionID` まで含めて同一性を判定することを
// 検証する。

import XCTest
@testable import KsSettingsViewCore

final class AccessoryTargetTests: XCTestCase {

    func test_rootHeader_と_rootFooter_は不等() {
        // GIVEN
        let a: AccessoryTarget = .rootHeader
        let b: AccessoryTarget = .rootFooter
        // WHEN / THEN
        XCTAssertNotEqual(a, b)
    }

    func test_rootHeader_同士は等価_かつ同一ハッシュ() {
        // GIVEN
        let a: AccessoryTarget = .rootHeader
        let b: AccessoryTarget = .rootHeader
        // WHEN / THEN
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_rootFooter_同士は等価_かつ同一ハッシュ() {
        let a: AccessoryTarget = .rootFooter
        let b: AccessoryTarget = .rootFooter
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_sectionHeader_同一sectionID_は等価_かつ同一ハッシュ() {
        // GIVEN
        let sectionID = UUID()
        let a: AccessoryTarget = .sectionHeader(sectionID: sectionID)
        let b: AccessoryTarget = .sectionHeader(sectionID: sectionID)
        // WHEN / THEN
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_sectionHeader_異なるsectionID_は不等() {
        let a: AccessoryTarget = .sectionHeader(sectionID: UUID())
        let b: AccessoryTarget = .sectionHeader(sectionID: UUID())
        XCTAssertNotEqual(a, b)
    }

    func test_sectionFooter_同一sectionID_は等価() {
        let sectionID = UUID()
        let a: AccessoryTarget = .sectionFooter(sectionID: sectionID)
        let b: AccessoryTarget = .sectionFooter(sectionID: sectionID)
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_sectionHeader_と_sectionFooter_は同一sectionIDでも不等() {
        let sectionID = UUID()
        let a: AccessoryTarget = .sectionHeader(sectionID: sectionID)
        let b: AccessoryTarget = .sectionFooter(sectionID: sectionID)
        XCTAssertNotEqual(a, b)
    }

    func test_rootHeader_と_sectionHeader_は不等() {
        let a: AccessoryTarget = .rootHeader
        let b: AccessoryTarget = .sectionHeader(sectionID: UUID())
        XCTAssertNotEqual(a, b)
    }
}

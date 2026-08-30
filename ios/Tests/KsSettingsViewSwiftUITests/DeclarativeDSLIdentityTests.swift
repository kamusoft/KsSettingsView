// DeclarativeDSLIdentityTests.swift
// KsSettingsViewSwiftUITests
//
// DSL ID 採番ユーティリティ（`DSLIdentityUUID`）の安定性・優先順位を検証する。

import XCTest
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore

final class DeclarativeDSLIdentityTests: XCTestCase {

    func test_同一ヒントから決定的UUIDが返る() {
        let h: DSLIdentityHint = .headerText(rootIdx: 1, text: "Hello")
        let u1 = DSLIdentityUUID.uuid(from: h)
        let u2 = DSLIdentityUUID.uuid(from: h)
        XCTAssertEqual(u1, u2)
    }

    func test_異なるヒントは異なるUUIDが返る() {
        let h1: DSLIdentityHint = .headerText(rootIdx: 0, text: "A")
        let h2: DSLIdentityHint = .headerText(rootIdx: 1, text: "A")
        let h3: DSLIdentityHint = .headerText(rootIdx: 0, text: "B")
        let u1 = DSLIdentityUUID.uuid(from: h1)
        let u2 = DSLIdentityUUID.uuid(from: h2)
        let u3 = DSLIdentityUUID.uuid(from: h3)
        XCTAssertNotEqual(u1, u2)
        XCTAssertNotEqual(u1, u3)
        XCTAssertNotEqual(u2, u3)
    }

    func test_異なるケースは異なるUUIDが返る() {
        let h1: DSLIdentityHint = .rootPosition(rootIdx: 0)
        let h2: DSLIdentityHint = .headerText(rootIdx: 0, text: "")
        XCTAssertNotEqual(
            DSLIdentityUUID.uuid(from: h1),
            DSLIdentityUUID.uuid(from: h2)
        )
    }

    func test_explicit_String_ヒントは決定的() {
        let h: DSLIdentityHint = .explicit(AnyHashable("dynamic-1"))
        let u1 = DSLIdentityUUID.uuid(from: h)
        let u2 = DSLIdentityUUID.uuid(from: h)
        XCTAssertEqual(u1, u2)
    }

    func test_forEach_Int_ヒントは決定的() {
        let h: DSLIdentityHint = .forEach(AnyHashable(42))
        let u1 = DSLIdentityUUID.uuid(from: h)
        let u2 = DSLIdentityUUID.uuid(from: h)
        XCTAssertEqual(u1, u2)
    }

    func test_positional_ヒント_同SectionID同位置同型は等しい() {
        let sectionID = UUID()
        let h1: DSLIdentityHint = .positional(sectionID: sectionID, indexInSection: 0, cellType: "Foo")
        let h2: DSLIdentityHint = .positional(sectionID: sectionID, indexInSection: 0, cellType: "Foo")
        XCTAssertEqual(DSLIdentityUUID.uuid(from: h1), DSLIdentityUUID.uuid(from: h2))
    }

    func test_positional_ヒント_型違いは異なるUUID() {
        let sectionID = UUID()
        let h1: DSLIdentityHint = .positional(sectionID: sectionID, indexInSection: 0, cellType: "Foo")
        let h2: DSLIdentityHint = .positional(sectionID: sectionID, indexInSection: 0, cellType: "Bar")
        XCTAssertNotEqual(DSLIdentityUUID.uuid(from: h1), DSLIdentityUUID.uuid(from: h2))
    }
}

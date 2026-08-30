// SettingsAccessoryTests.swift
// KsSettingsViewCoreTests
//
// `SettingsAccessory` が root / section の装飾値を別ケースとして保持し、text ケースは
// 中身の文字列で、view ケースは中身を無視して等価性が決まることを検証する。

import SwiftUI
import XCTest
@testable import KsSettingsViewCore

final class SettingsAccessoryTests: XCTestCase {

    func test_root_同一中身は等価() {
        // GIVEN
        let a: SettingsAccessory = .root(.text("X"))
        let b: SettingsAccessory = .root(.text("X"))
        // WHEN / THEN
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_root_異なる中身は不等() {
        let a: SettingsAccessory = .root(.text("X"))
        let b: SettingsAccessory = .root(.text("Y"))
        XCTAssertNotEqual(a, b)
    }

    func test_section_同一中身は等価() {
        let a: SettingsAccessory = .section(.text("X"))
        let b: SettingsAccessory = .section(.text("X"))
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_root_と_section_は同一テキストでも不等() {
        // GIVEN: 同一文字列を持つ root と section
        let a: SettingsAccessory = .root(.text("X"))
        let b: SettingsAccessory = .section(.text("X"))
        // WHEN / THEN: ケースが異なるため不等
        XCTAssertNotEqual(a, b)
    }

    func test_root_view_ケースは中身無視で等価() {
        // GIVEN: KsAnyView の中身は等価性に参加しない
        let a: SettingsAccessory = .root(.view(KsAnyView.swiftUI { Text("a") }))
        let b: SettingsAccessory = .root(.view(KsAnyView.swiftUI { Text("b") }))
        // WHEN / THEN
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_section_view_ケースは中身無視で等価() {
        let a: SettingsAccessory = .section(.view(KsAnyView.swiftUI { Text("a") }))
        let b: SettingsAccessory = .section(.view(KsAnyView.swiftUI { Text("b") }))
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }
}

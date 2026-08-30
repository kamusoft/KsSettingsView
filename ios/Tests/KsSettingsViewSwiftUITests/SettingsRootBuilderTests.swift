// SettingsRootBuilderTests.swift
// KsSettingsViewSwiftUITests
//
// DSL（`@SettingsRootBuilder` / `@SectionBuilder`）からの `SettingsRoot` 構築を検証する。
//
// 注意:
//   `@testable import KsSettingsViewSwiftUI` は SwiftUI を transitive に取り込むため、
//   bare `Section` は `SwiftUI.Section` と曖昧になる。本ファイル内では
//   `KsSection`（`KsSettingsViewSwiftUI` 提供の type alias）で曖昧性を回避する。

import XCTest
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore

#if canImport(UIKit)
@testable import KsSettingsViewUI

final class SettingsRootBuilderTests: XCTestCase {
    func test_DSLでSettingsRootを構築できる() {
        let root = SettingsRoot {
            ksSection("一般") {
                DummyTestCell(title: "A")
                DummyTestCell(title: "B")
            }
            ksSection("高度") {
                DummyTestCell(title: "C")
            }
        }
        XCTAssertEqual(root.sections.count, 2)
        XCTAssertEqual(root.sections[0].cells.count, 2)
        XCTAssertEqual(root.sections[1].cells.count, 1)
        XCTAssertEqual(root.sections[0].header, SectionAccessory.text("一般"))
        XCTAssertEqual(root.sections[1].header, SectionAccessory.text("高度"))
    }

    func test_DSLで条件分岐を含むSection群を構築できる() {
        let showSecond = true
        let root = SettingsRoot {
            ksSection("一般") {
                DummyTestCell(title: "A")
            }
            if showSecond {
                ksSection("条件付き") {
                    DummyTestCell(title: "B")
                }
            }
        }
        XCTAssertEqual(root.sections.count, 2)
    }

    // 注: `SettingsRoot` は header / footer を持たない。Root H/F は
    //     `KsSettingsViewController.rootHeader` / `rootFooter` の View プロパティとして扱う。

    func test_SectionInit_アクセサリ直指定() {
        let section = ksSection(header: .text("H"), footer: .text("F")) {
            DummyTestCell(title: "A")
        }
        XCTAssertEqual(section.header, SectionAccessory.text("H"))
        XCTAssertEqual(section.footer, SectionAccessory.text("F"))
        XCTAssertEqual(section.cells.count, 1)
    }
}
#endif

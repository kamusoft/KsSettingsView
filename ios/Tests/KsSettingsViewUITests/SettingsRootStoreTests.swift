// SettingsRootStoreTests.swift
// KsSettingsViewUITests
//
// `SettingsRootStore` の各メソッドが期待通り `root` を更新し、対応する `SettingsRootDiff` を
// 発行することを検証する。

#if canImport(UIKit)
import XCTest
import Combine
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class SettingsRootStoreTests: XCTestCase {
    private var cancellables: Set<AnyCancellable> = []

    override func tearDown() {
        cancellables.removeAll()
        super.tearDown()
    }

    // MARK: - 初期化

    func test_init_initialRootがrootに反映される() {
        let initial = SettingsRoot(sections: [
            Section(header: .text("S"), cells: [LabelCell(title: "A")])
        ])
        let store = SettingsRootStore(initialRoot: initial)
        XCTAssertEqual(store.root, initial)
    }

    // MARK: - replaceAll

    func test_replaceAll_rootが更新されDiffが発行される() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        let newRoot = SettingsRoot(sections: [
            Section(header: .text("X"), cells: [LabelCell(title: "A")])
        ])
        store.replaceAll(newRoot)

        XCTAssertEqual(store.root, newRoot)
        XCTAssertEqual(receivedDiff, .full(newRoot))
    }

    // MARK: - insertSection

    func test_insertSection_Sectionが追加されDiffが発行される() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        let newSection = Section(header: .text("新規"), cells: [LabelCell(title: "A")])
        store.insertSection(newSection, at: 0)

        XCTAssertEqual(store.root.sections.count, 1)
        XCTAssertEqual(store.root.sections[0], newSection)
        XCTAssertEqual(receivedDiff, .insertSection(at: 0, section: newSection))
    }

    // MARK: - removeSection

    func test_removeSection_Sectionが削除されDiffが発行される() {
        let sec = Section(header: .text("S"), cells: [LabelCell(title: "A")])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        store.removeSection(sectionID: sec.id)

        XCTAssertEqual(store.root.sections.count, 0)
        XCTAssertEqual(receivedDiff, .removeSection(sectionID: sec.id))
    }

    // MARK: - moveSection

    func test_moveSection_順序が変わりDiffが発行される() {
        let s1 = Section(header: .text("S1"))
        let s2 = Section(header: .text("S2"))
        let s3 = Section(header: .text("S3"))
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [s1, s2, s3]))
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        store.moveSection(from: 0, to: 2)

        XCTAssertEqual(store.root.sections.map { $0.id }, [s2.id, s3.id, s1.id])
        XCTAssertEqual(receivedDiff, .moveSection(from: 0, to: 2))
    }

    // MARK: - replaceSection

    func test_replaceSection_Sectionが置換されDiffが発行される() {
        let old = Section(header: .text("old"))
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [old]))
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        let new = Section(id: old.id, header: .text("new"))
        store.replaceSection(sectionID: old.id, new: new)

        XCTAssertEqual(store.root.sections[0].header, .text("new"))
        XCTAssertEqual(receivedDiff, .replaceSection(sectionID: old.id, new: new))
    }

    // MARK: - insertCell

    func test_insertCell_CellがSection内に追加されDiffが発行される() {
        let sec = Section(header: .text("S"), cells: [LabelCell(title: "A")])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        let newCell = LabelCell(title: "B")
        store.insertCell(newCell, in: sec.id, at: 0)

        XCTAssertEqual(store.root.sections[0].cells.count, 2)
        // 先頭に挿入されている
        XCTAssertEqual(KsCellID(cell: store.root.sections[0].cells[0]), KsCellID(cell: newCell))
        XCTAssertEqual(receivedDiff, .insertCell(sectionID: sec.id, at: 0, cell: newCell))
    }

    // MARK: - removeCell

    func test_removeCell_CellがSectionから削除されDiffが発行される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let sec = Section(header: .text("S"), cells: [cellA, cellB])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        let aID = KsCellID(cell: cellA)
        store.removeCell(cellID: aID)

        XCTAssertEqual(store.root.sections[0].cells.count, 1)
        XCTAssertEqual(KsCellID(cell: store.root.sections[0].cells[0]), KsCellID(cell: cellB))
        XCTAssertEqual(receivedDiff, .removeCell(cellID: aID))
    }

    // MARK: - replaceCell

    func test_replaceCell_Cellが置換されDiffが発行される() {
        let oldCell = LabelCell(title: "old")
        let sec = Section(header: .text("S"), cells: [oldCell])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        let newCell = LabelCell(id: oldCell.id, title: "new")
        let oldID = KsCellID(cell: oldCell)
        store.replaceCell(cellID: oldID, new: newCell)

        let replacedCell = store.root.sections[0].cells[0] as? LabelCell
        XCTAssertEqual(replacedCell?.title, "new")
        XCTAssertEqual(receivedDiff, .replaceCell(cellID: oldID, new: newCell))
    }

    /// 同一 id への **2 回連続** replaceCell が両方反映されることを検証する回帰テスト。
    ///
    /// Store は Cell の照合を id 同一性のみで行う。直前の Cell から生成した cellID（内容違い）
    /// でも id が同じなら 2 回目の更新が正しく解決されることを確認する。これにより Store と
    /// Controller の snapshot 識別子（ともに id 限定）がドリフトしない。
    ///
    /// 注: 本テストは `#if canImport(UIKit)` 内であり macOS ホストの `swift test` では実行されない。
    ///     id 限定照合のロジック自体は Core 層の `KsCellIDTests`（ホスト実行）でも担保している。
    func test_replaceCell_同一idへの2回連続更新が両方反映される() {
        let c0 = LabelCell(title: "A")
        let sec = Section(header: .text("S"), cells: [c0])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))

        // 更新1: A -> B
        let c1 = LabelCell(id: c0.id, title: "B")
        store.replaceCell(cellID: KsCellID(cell: c0), new: c1)
        XCTAssertEqual((store.root.sections[0].cells[0] as? LabelCell)?.title, "B")

        // 更新2: B -> C（直前 Cell B から生成した cellID で照合。id は同じ）
        let c2 = LabelCell(id: c0.id, title: "C")
        store.replaceCell(cellID: KsCellID(cell: c1), new: c2)
        XCTAssertEqual(
            (store.root.sections[0].cells[0] as? LabelCell)?.title,
            "C",
            "2 回目の連続内容更新が id 照合で反映されること"
        )
    }

    // MARK: - moveCell

    func test_moveCell_Cellが移動しDiffが発行される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let cellC = LabelCell(title: "C")
        let sec = Section(header: .text("S"), cells: [cellA, cellB, cellC])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        let aID = KsCellID(cell: cellA)
        store.moveCell(cellID: aID, to: 2)

        let titles = store.root.sections[0].cells.compactMap { ($0 as? LabelCell)?.title }
        XCTAssertEqual(titles, ["B", "C", "A"])
        XCTAssertEqual(receivedDiff, .moveCell(cellID: aID, to: 2))
    }

    // MARK: - updateAccessory

    func test_updateAccessory_rootHeaderはDiffのみ発行される() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        let acc: SettingsAccessory? = .root(.text("X"))
        store.updateAccessory(target: .rootHeader, accessory: acc)

        // root state は変わらない（Root H/F は UI 層プロパティ）
        XCTAssertEqual(store.root.sections.count, 0)
        XCTAssertEqual(receivedDiff, .updateAccessory(target: .rootHeader, accessory: acc))
    }

    func test_updateAccessory_sectionHeaderはSectionに反映される() {
        let sec = Section(header: .text("old"))
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var receivedDiff: SettingsRootDiff?
        store.diffPublisher.sink { receivedDiff = $0 }.store(in: &cancellables)

        let acc: SettingsAccessory? = .section(.text("new"))
        store.updateAccessory(target: .sectionHeader(sectionID: sec.id), accessory: acc)

        XCTAssertEqual(store.root.sections[0].header, .text("new"))
        XCTAssertEqual(receivedDiff, .updateAccessory(target: .sectionHeader(sectionID: sec.id), accessory: acc))
    }

    /// 既知 sectionID の header / footer はともに現在状態へ反映され、対応する Diff が発行される。
    func test_updateAccessory_既知sectionIDはheaderもfooterも反映されDiffが発行される() {
        let sec = Section(header: .text("H-old"), footer: .text("F-old"))
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var diffs: [SettingsRootDiff] = []
        store.diffPublisher.sink { diffs.append($0) }.store(in: &cancellables)

        let headerAcc: SettingsAccessory? = .section(.text("H-new"))
        let footerAcc: SettingsAccessory? = .section(.text("F-new"))
        store.updateAccessory(target: .sectionHeader(sectionID: sec.id), accessory: headerAcc)
        store.updateAccessory(target: .sectionFooter(sectionID: sec.id), accessory: footerAcc)

        XCTAssertEqual(store.root.sections[0].header, .text("H-new"))
        XCTAssertEqual(store.root.sections[0].footer, .text("F-new"))
        XCTAssertEqual(diffs, [
            .updateAccessory(target: .sectionHeader(sectionID: sec.id), accessory: headerAcc),
            .updateAccessory(target: .sectionFooter(sectionID: sec.id), accessory: footerAcc),
        ])
    }

    /// Root 系 target は `SettingsRoot` 値型に状態を持たないため sectionID 検証の対象外であり、
    /// header / footer とも Diff を発行する。
    func test_updateAccessory_Root系targetはheaderもfooterもDiffを発行する() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        var diffs: [SettingsRootDiff] = []
        store.diffPublisher.sink { diffs.append($0) }.store(in: &cancellables)

        let headerAcc: SettingsAccessory? = .root(.text("ROOT-H"))
        let footerAcc: SettingsAccessory? = .root(.text("ROOT-F"))
        store.updateAccessory(target: .rootHeader, accessory: headerAcc)
        store.updateAccessory(target: .rootFooter, accessory: footerAcc)

        XCTAssertEqual(diffs, [
            .updateAccessory(target: .rootHeader, accessory: headerAcc),
            .updateAccessory(target: .rootFooter, accessory: footerAcc),
        ])
    }

    // MARK: - applyTheme（Theme 更新は構造 Diff を発行しない）

    func test_applyTheme_storeのthemeが更新されDiffは発行されない() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        var diffCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)

        let newTheme = Theme(separatorColor: UIColor(red: 0.1, green: 0.2, blue: 0.3, alpha: 1.0))
        store.applyTheme(newTheme)

        XCTAssertEqual(store.theme, newTheme)
        XCTAssertEqual(diffCount, 0, "applyTheme は Diff Publisher に Diff を発行しない")
    }

    func test_applyTheme_同値ならtheme通知を抑制する() {
        let theme = Theme(scrollIndicatorVisible: false)
        let store = SettingsRootStore(initialRoot: SettingsRoot(), initialTheme: theme)
        var themeChangeCount = 0
        store.$theme.dropFirst().sink { _ in themeChangeCount += 1 }.store(in: &cancellables)

        store.applyTheme(theme) // 同値 → 通知抑制
        XCTAssertEqual(themeChangeCount, 0)

        store.applyTheme(Theme(scrollIndicatorVisible: true)) // 異なる値 → 通知
        XCTAssertEqual(themeChangeCount, 1)
    }

    // MARK: - preview

    func test_preview_factoryで作ったstoreは渡したrootを保持する() {
        let initial = SettingsRoot(sections: [
            Section(header: .text("S"), cells: [LabelCell(title: "A")])
        ])
        let store = SettingsRootStore.preview(root: initial)
        XCTAssertEqual(store.root, initial)
    }

    // MARK: - 存在しない ID への操作（no-op 契約）

    /// Store の各メソッドが「対象 ID が存在しない場合」に state を変更せず、Diff も発行しない
    /// （no-op になる）ことを検証する。これにより `applyDiff` 側のエラー検出パスを誤発火させない
    /// 「safe by default」契約を担保する。

    func test_removeSection_存在しないIDではstate変更もDiff発行もされない() {
        let sec = Section(header: .text("S"))
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var diffCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)

        store.removeSection(sectionID: UUID())

        XCTAssertEqual(store.root.sections.count, 1, "state は変更されない")
        XCTAssertEqual(diffCount, 0, "Diff は発行されない")
    }

    func test_moveSection_範囲外fromではstate変更もDiff発行もされない() {
        let s1 = Section(header: .text("S1"))
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [s1]))
        var diffCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)

        store.moveSection(from: 5, to: 0)

        XCTAssertEqual(store.root.sections.map { $0.id }, [s1.id])
        XCTAssertEqual(diffCount, 0)
    }

    func test_replaceSection_存在しないIDではstate変更もDiff発行もされない() {
        let sec = Section(header: .text("S"))
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var diffCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)

        let other = Section(header: .text("other"))
        store.replaceSection(sectionID: UUID(), new: other)

        XCTAssertEqual(store.root.sections[0].header, .text("S"))
        XCTAssertEqual(diffCount, 0)
    }

    func test_insertCell_存在しないsectionIDではstate変更もDiff発行もされない() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        var diffCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)

        store.insertCell(LabelCell(title: "X"), in: UUID(), at: 0)

        XCTAssertEqual(store.root.sections.count, 0)
        XCTAssertEqual(diffCount, 0)
    }

    func test_removeCell_存在しないcellIDではstate変更もDiff発行もされない() {
        let cellA = LabelCell(title: "A")
        let sec = Section(header: .text("S"), cells: [cellA])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var diffCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)

        let bogus = KsCellID(cell: LabelCell(title: "bogus"))
        store.removeCell(cellID: bogus)

        XCTAssertEqual(store.root.sections[0].cells.count, 1)
        XCTAssertEqual(diffCount, 0)
    }

    func test_replaceCell_存在しないcellIDではstate変更もDiff発行もされない() {
        let cellA = LabelCell(title: "A")
        let sec = Section(header: .text("S"), cells: [cellA])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var diffCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)

        let bogus = KsCellID(cell: LabelCell(title: "bogus"))
        store.replaceCell(cellID: bogus, new: LabelCell(title: "new"))

        let title = (store.root.sections[0].cells[0] as? LabelCell)?.title
        XCTAssertEqual(title, "A")
        XCTAssertEqual(diffCount, 0)
    }

    /// 未知 sectionID の section header 更新は、現在状態も状態ストリームも Diff ストリームも動かさない。
    ///
    /// `@Published` は同値判定を行わないため、同値の再代入だけでも `$root` が発行される。
    /// 観測は Diff 件数だけでなく `$root` の発行件数まで含めて no-op を確認する。
    func test_updateAccessory_未知sectionIDのsectionHeaderはstate変更もDiff発行もされない() {
        let sec = Section(header: .text("H"), footer: .text("F"))
        let initial = SettingsRoot(sections: [sec])
        let store = SettingsRootStore(initialRoot: initial)
        var diffCount = 0
        var rootChangeCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)
        store.$root.dropFirst().sink { _ in rootChangeCount += 1 }.store(in: &cancellables)

        store.updateAccessory(target: .sectionHeader(sectionID: UUID()), accessory: .section(.text("X")))

        XCTAssertEqual(store.root, initial, "現在状態は変化しない")
        XCTAssertEqual(rootChangeCount, 0, "状態ストリームへ発行されない")
        XCTAssertEqual(diffCount, 0, "Diff ストリームへ発行されない")
    }

    /// 未知 sectionID の section footer 更新も同じく no-op になる。
    func test_updateAccessory_未知sectionIDのsectionFooterはstate変更もDiff発行もされない() {
        let sec = Section(header: .text("H"), footer: .text("F"))
        let initial = SettingsRoot(sections: [sec])
        let store = SettingsRootStore(initialRoot: initial)
        var diffCount = 0
        var rootChangeCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)
        store.$root.dropFirst().sink { _ in rootChangeCount += 1 }.store(in: &cancellables)

        store.updateAccessory(target: .sectionFooter(sectionID: UUID()), accessory: .section(.text("X")))

        XCTAssertEqual(store.root, initial, "現在状態は変化しない")
        XCTAssertEqual(rootChangeCount, 0, "状態ストリームへ発行されない")
        XCTAssertEqual(diffCount, 0, "Diff ストリームへ発行されない")
    }

    func test_moveCell_存在しないcellIDではstate変更もDiff発行もされない() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let sec = Section(header: .text("S"), cells: [cellA, cellB])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        var diffCount = 0
        store.diffPublisher.sink { _ in diffCount += 1 }.store(in: &cancellables)

        let bogus = KsCellID(cell: LabelCell(title: "bogus"))
        store.moveCell(cellID: bogus, to: 0)

        let titles = store.root.sections[0].cells.compactMap { ($0 as? LabelCell)?.title }
        XCTAssertEqual(titles, ["A", "B"])
        XCTAssertEqual(diffCount, 0)
    }

    // MARK: - replaceCells（複数 Cell の一括内容更新）

    /// Radio グループの選択変化のように、複数 Cell の内容が同時に変わる更新を検証する。
    ///
    /// 個別 `replaceCell` を連続で呼ぶと内容更新が複数回に分かれて配信されるため、UI 層が
    /// 部分更新を取りこぼす余地が生まれる。`replaceCells` は対象 Cell ID 群を 1 件のバッチとして
    /// 配信し、UI 層が 1 回の部分更新でまとめて反映できることを保証する。
    func test_replaceCells_複数Cellの更新が1バッチで配信される() {
        let light = RadioCell(title: "Light", groupId: "theme", value: "light", selectedValue: "light")
        let dark = RadioCell(title: "Dark", groupId: "theme", value: "dark", selectedValue: "light")
        let auto = RadioCell(title: "Auto", groupId: "theme", value: "auto", selectedValue: "light")
        let sec = Section(header: .text("表示"), cells: [light, dark, auto])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))

        var batches: [[KsCellID]] = []
        // 配信時点で購読者が更新後の現在状態を参照できることも同時に確認する。
        var selectedValuesAtDelivery: [[String]] = []
        store.contentUpdateBatchPublisher.sink { ids in
            batches.append(ids)
            selectedValuesAtDelivery.append(
                store.root.sections[0].cells.compactMap { ($0 as? RadioCell)?.selectedValue }
            )
        }.store(in: &cancellables)

        // Dark を選ぶと、同一グループ 3 セルの selectedValue が同時に変わる。
        let updates: [(cellID: KsCellID, new: any KsCell)] = [light, dark, auto].map { cell in
            (
                cellID: KsCellID(cell: cell),
                new: RadioCell(
                    id: cell.id,
                    title: cell.title,
                    groupId: cell.groupId,
                    value: cell.value,
                    selectedValue: "dark"
                ) as any KsCell
            )
        }
        store.replaceCells(updates)

        let cells = store.root.sections[0].cells.compactMap { $0 as? RadioCell }
        XCTAssertEqual(cells.map { $0.selectedValue }, ["dark", "dark", "dark"])
        // 選択状態（value == selectedValue）は dark のみ true（複数選択にならない）。
        XCTAssertEqual(cells.map { $0.value == $0.selectedValue }, [false, true, false])

        XCTAssertEqual(batches.count, 1, "配信は 1 回のバッチにまとまる")
        XCTAssertEqual(
            batches.first,
            [KsCellID(cell: light), KsCellID(cell: dark), KsCellID(cell: auto)],
            "適用順の Cell ID 群が含まれる"
        )
        XCTAssertEqual(
            selectedValuesAtDelivery.first,
            ["dark", "dark", "dark"],
            "配信時点で購読者は更新後の現在状態を参照できる"
        )
    }

    func test_replaceCells_既知と未知IDの混在では既知だけが適用配信される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let sec = Section(header: .text("S"), cells: [cellA, cellB])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))

        var batches: [[KsCellID]] = []
        store.contentUpdateBatchPublisher.sink { batches.append($0) }.store(in: &cancellables)

        let unknown = KsCellID(id: UUID())
        store.replaceCells([
            (cellID: KsCellID(cell: cellA), new: LabelCell(id: cellA.id, title: "A2")),
            (cellID: unknown, new: LabelCell(title: "X")),
            (cellID: KsCellID(cell: cellB), new: LabelCell(id: cellB.id, title: "B2")),
        ])

        let titles = store.root.sections[0].cells.compactMap { ($0 as? LabelCell)?.title }
        XCTAssertEqual(titles, ["A2", "B2"], "既知 ID の更新だけが適用される")
        XCTAssertEqual(store.root.sections[0].cells.count, 2, "未知 ID で Cell は増えない")
        XCTAssertEqual(batches.count, 1)
        XCTAssertEqual(
            batches.first,
            [KsCellID(cell: cellA), KsCellID(cell: cellB)],
            "バッチには既知 ID だけが含まれる"
        )
    }

    func test_replaceCells_存在しないIDのみでは状態変更も配信もされない() {
        let cellA = LabelCell(title: "A")
        let sec = Section(header: .text("S"), cells: [cellA])
        let initial = SettingsRoot(sections: [sec])
        let store = SettingsRootStore(initialRoot: initial)

        var batchCount = 0
        store.contentUpdateBatchPublisher.sink { _ in batchCount += 1 }.store(in: &cancellables)

        store.replaceCells([(cellID: KsCellID(id: UUID()), new: LabelCell(title: "X"))])

        XCTAssertEqual(store.root, initial)
        XCTAssertEqual(batchCount, 0)
    }

    func test_replaceCells_空配列は何もしない() {
        let cellA = LabelCell(title: "A")
        let initial = SettingsRoot(sections: [Section(header: .text("S"), cells: [cellA])])
        let store = SettingsRootStore(initialRoot: initial)

        var batchCount = 0
        store.contentUpdateBatchPublisher.sink { _ in batchCount += 1 }.store(in: &cancellables)

        store.replaceCells([])

        XCTAssertEqual(store.root, initial)
        XCTAssertEqual(batchCount, 0)
    }

    func test_replaceCells_同一IDの重複指定は最後の値が残る() {
        let cellA = LabelCell(title: "A")
        let sec = Section(header: .text("S"), cells: [cellA])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))

        var batches: [[KsCellID]] = []
        store.contentUpdateBatchPublisher.sink { batches.append($0) }.store(in: &cancellables)

        let cellID = KsCellID(cell: cellA)
        store.replaceCells([
            (cellID: cellID, new: LabelCell(id: cellA.id, title: "値A")),
            (cellID: cellID, new: LabelCell(id: cellA.id, title: "値B")),
        ])

        let title = (store.root.sections[0].cells[0] as? LabelCell)?.title
        XCTAssertEqual(title, "値B", "入力順に適用され最後の値が残る")
        XCTAssertEqual(batches.count, 1)
        XCTAssertEqual(batches.first, [cellID, cellID], "適用ごとに ID が含まれる")
    }
}
#endif

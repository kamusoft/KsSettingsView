// DatePickerCellView.swift
// KsSettingsViewUI
//
// `DatePickerCell` の Renderer 実装。共通行レイアウト関数経由で描画し、accessory slot に
// chevron。`uiStyle` で表示方式を切替える：
//
// - `.wheels`   タップで埋め込み `UIDatePicker(.date)` を `inputView` 経由でキーボード位置に
//               スライドアップ表示する（AiForms 互換）。Toolbar に Cancel / [Today?] / Done。
// - `.calendar` タップで `.pageSheet` + `.custom` detent シートで `.inline` カレンダー grid を
//               表示する（iOS カレンダーアプリ風）。下部に [Cancel] [Today?] [Done]。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

@MainActor
internal final class DatePickerCellView: KsListCellBase, @MainActor KsCellRenderer {
    internal var tapHandler: (@Sendable () -> Void)?
    private var lastCell: DatePickerCell?

    /// Cancel 時に戻すための「Picker 提示開始時点」の Date。
    private var preSelectedDate: Date = Date()

    /// 透明 no-caret な UITextField。`.wheels` モードで `inputView = UIDatePicker(.date)` を担う。
    private let embeddedField = EmbeddedPickerHostField()

    /// `embeddedField.inputView` にセットする UIDatePicker(.date)。
    /// `.wheels` モードでのみ使用。`.calendar` モードは `DatePickerCalendarSheetController` 側で持つ。
    private let wheelsPicker: UIDatePicker = {
        let p = UIDatePicker()
        p.datePickerMode = .date
        if #available(iOS 13.4, *) {
            p.preferredDatePickerStyle = .wheels
        }
        return p
    }()

    /// 現在 sheet 提示中の Calendar mode VC（提示中のみ非 nil）。テストフックや
    /// 再提示防止のために保持する。dismiss 完了後に明示的に nil 化される
    /// （weak 参照では UIKit の保持タイミング次第で deinit が遅延し再タップが弾かれるため、
    /// strong 参照で保持し明示的に nil 化する）。
    private var currentCalendarController: DatePickerCalendarSheetController?

    override init(frame: CGRect) {
        super.init(frame: frame)
        embeddedField.translatesAutoresizingMaskIntoConstraints = true
        embeddedField.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        embeddedField.frame = contentView.bounds
        contentView.addSubview(embeddedField)
        contentView.sendSubviewToBack(embeddedField)

        embeddedField.inputView = wheelsPicker
    }

    func render(cell: any KsCell, theme: Theme) {
        guard let dc = cell as? DatePickerCell else {
            assertionFailure("DatePickerCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        self.lastCell = dc
        let effective = EffectiveStyle(theme: theme, cellStyle: dc.style)

        applyCellBaseLayout(
            self,
            title: dc.title,
            description: dc.description,
            icon: dc.icon,
            hintText: dc.hintText,
            effective: effective,
            theme: theme,
            isEnabled: dc.isEnabled,
            valueLabelText: dc.effectiveValueText(),
            accessoryView: makeChevronView()
        )

        // Wheels モードの Picker を最新化（Calendar モードは提示時に動的構築するため不要）
        wheelsPicker.date = dc.date
        wheelsPicker.minimumDate = dc.minDate
        wheelsPicker.maximumDate = dc.maxDate
        if let c = dc.accentColor {
            wheelsPicker.tintColor = c
        }
        self.preSelectedDate = dc.date

        // Wheels Toolbar 組み直し（Today ボタンの表示制御を含む）
        rebuildWheelsToolbar(for: dc)

        if dc.isEnabled {
            let handler: @Sendable () -> Void = { [weak self] in
                Task { @MainActor in
                    self?.presentDatePicker()
                }
            }
            self.tapHandler = handler
        } else {
            self.tapHandler = nil
        }
    }

    /// `uiStyle` に応じて Wheels (becomeFirstResponder) / Calendar (sheet present) を分岐。
    private func presentDatePicker() {
        guard let cell = lastCell else { return }
        switch cell.uiStyle {
        case .wheels:
            embeddedField.becomeFirstResponder()
        case .calendar:
            presentCalendarSheet(for: cell)
        }
    }

    private func presentCalendarSheet(for cell: DatePickerCell) {
        // 既に提示中なら多重提示を防ぐ
        if currentCalendarController != nil { return }
        guard let presenter = KeyWindowResolver.topPresentedViewController() else { return }
        let vc = makeCalendarSheetController(for: cell)
        self.currentCalendarController = vc
        presenter.present(vc, animated: true)
    }

    /// カレンダーシートの VC を組み立てる。提示元の外観の引き継ぎもここで済ませ、
    /// 提示経路とテストの検証 seam が同じ結果を共有する。
    private func makeCalendarSheetController(for cell: DatePickerCell) -> DatePickerCalendarSheetController {
        let vc = DatePickerCalendarSheetController(
            initial: cell.date,
            minimumDate: cell.minDate,
            maximumDate: cell.maxDate,
            pickerTitle: cell.pickerTitle,
            todayText: cell.todayText,
            accentColor: cell.accentColor,
            onDone: { [weak self] newDate in
                self?.applyDoneDate(newDate, for: cell)
            },
            onDismissed: { [weak self] in
                // dismiss 完了で参照を解放し、次回タップで再提示できるようにする
                self?.currentCalendarController = nil
            }
        )
        PresentationAppearance.inherit(from: self, to: vc)
        return vc
    }

    /// Wheels モード用 Toolbar を組み立て直して `embeddedField.inputAccessoryView` に紐づける。
    /// `todayText` 指定時に Today ボタンが Cancel と Done の間に追加される。
    private func rebuildWheelsToolbar(for cell: DatePickerCell) {
        let built = EmbeddedPickerToolbar.build(
            title: cell.pickerTitle,
            todayText: cell.todayText,
            accentColor: cell.accentColor,
            cancelTarget: self,
            cancelAction: #selector(handleWheelsCancel),
            doneTarget: self,
            doneAction: #selector(handleWheelsDone),
            todayTarget: self,
            todayAction: #selector(handleWheelsToday)
        )
        embeddedField.inputAccessoryView = built.toolbar
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        if embeddedField.isFirstResponder {
            embeddedField.resignFirstResponder()
        }
        self.tapHandler = nil
        self.lastCell = nil
        self.currentCalendarController = nil
    }

    // MARK: - Wheels モード Toolbar 操作

    @objc private func handleWheelsCancel() {
        wheelsPicker.date = preSelectedDate
        embeddedField.resignFirstResponder()
    }

    @objc private func handleWheelsDone() {
        guard let cell = lastCell else {
            embeddedField.resignFirstResponder()
            return
        }
        let newDate = wheelsPicker.date
        embeddedField.resignFirstResponder()
        applyDoneDate(newDate, for: cell)
    }

    /// Today タップ: 範囲チェックを通れば picker の日付を today にセットする
    /// （AiForms オリジナル `DatePickerCellView.SetToday()` 準拠）。
    ///
    /// 注意: `wheelsPicker.date` の年月日が既に today と同じ場合、`setDate(today, animated: true)`
    /// だけだと UIKit が「変化なし」と判定して wheel が回らないことがある。確実に
    /// 「今日のホイール」状態に揃えるため、`Calendar.startOfDay(for:)` で時刻成分を 00:00 に
    /// 正規化したうえで一度別 date へずらし → today へ戻す経路は取らず、`animated: false` で
    /// 即時セットしてから `valueChanged` を発火させる（hour/minute/second は applyDoneDate で
    /// 元 cell.date のものを残すのでここでは正規化のみ意識する）。
    @objc private func handleWheelsToday() {
        let today = Date()
        let calendar = Calendar.current
        let todayStart = calendar.startOfDay(for: today)
        // 範囲チェックは「日単位」で比較する。AiForms 互換の min/max は
        // 「指定日まで選択可」というセマンティクスなので、時刻成分まで比較すると
        // 例えば `maxDate: Date()`（= 今この瞬間）の指定で「今日」ボタンが
        // 直後の `Date()` > max で弾かれてしまう。startOfDay 同士の比較で
        // 「同じ日であれば OK」とする。
        if let min = wheelsPicker.minimumDate, todayStart < calendar.startOfDay(for: min) { return }
        if let max = wheelsPicker.maximumDate, todayStart > calendar.startOfDay(for: max) { return }
        // 既に同日が選ばれていても確実に「今日」へ揃えるため、いったん別の date を入れて
        // 強制差分を作ったうえで today にセットする。
        if calendar.isDate(wheelsPicker.date, inSameDayAs: today) {
            // 同日なら 1 秒だけずらしてから戻す（wheel が再描画される）
            if let bump = calendar.date(byAdding: .second, value: 1, to: todayStart) {
                wheelsPicker.setDate(bump, animated: false)
            }
        }
        wheelsPicker.setDate(todayStart, animated: true)
        wheelsPicker.sendActions(for: .valueChanged)
    }

    /// Done 確定時の共通処理。year/month/day のみ反映し、元 cell.date の hour/minute/second を保持。
    private func applyDoneDate(_ newDate: Date, for cell: DatePickerCell) {
        let calendar = Calendar.current
        let ymd = calendar.dateComponents([.year, .month, .day], from: newDate)
        let hms = calendar.dateComponents([.hour, .minute, .second], from: cell.date)
        var combined = DateComponents()
        combined.year = ymd.year
        combined.month = ymd.month
        combined.day = ymd.day
        combined.hour = hms.hour
        combined.minute = hms.minute
        combined.second = hms.second
        let result = calendar.date(from: combined) ?? newDate
        cell.onValueChanged?(result)
    }

    // MARK: - test hook

    internal func _simulateTap() { tapHandler?() }
    /// テスト用: 提示経路と同一の組み立てでカレンダーシートの VC を生成する（配線の検証 seam）。
    internal func _makeCalendarSheetControllerForTesting() -> DatePickerCalendarSheetController? {
        guard let cell = lastCell else { return nil }
        return makeCalendarSheetController(for: cell)
    }
    internal var _lastCell: DatePickerCell? { lastCell }
    internal var _currentWheelsDate: Date { wheelsPicker.date }
    internal func _simulateWheelsChange(to newDate: Date) { wheelsPicker.date = newDate }
    internal func _simulateWheelsDone() { handleWheelsDone() }
    internal func _simulateWheelsCancel() { handleWheelsCancel() }
    internal func _simulateWheelsToday() { handleWheelsToday() }
    internal var _currentCalendarController: DatePickerCalendarSheetController? { currentCalendarController }
}
#endif

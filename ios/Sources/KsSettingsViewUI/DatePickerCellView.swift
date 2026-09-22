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
    private var lastTheme: Theme?
    private var localeProvider: @MainActor () -> Locale = { UserInterfaceLocale.current }

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

    /// ホイールの確定から閉じ切り通知までの間だけ保持する待ち受け。
    ///
    /// 入力面の非表示完了は `resignFirstResponder()` の中で同期に報告されることがあるため、
    /// 報告と値 callback の送出をそれぞれ記録し、両方そろってから閉じ切りを知らせる
    /// （値 callback より先に閉じ切りを出さない）。
    private struct WheelsCompletionWatch {
        /// 確定した日付。
        let date: Date
        /// 閉じ切りの通知先。
        let notify: @Sendable (Date) -> Void
        /// 値 callback を送出済みか。
        var isValueDelivered = false
        /// 入力面の非表示完了が報告されたか。
        var isHideReported = false
    }

    /// 閉じ切り通知を待っている間だけ非 nil。通知するか打ち切った時点で `nil` に戻る。
    private var wheelsCompletionWatch: WheelsCompletionWatch?

    /// 閉じ切りの通知が届かなかった場合に待ち受けを打ち切るタスク。
    private var wheelsHideTimeoutTask: Task<Void, Never>?

    /// 入力面の非表示完了を待つ上限時間（秒）。この時間を過ぎたら、発火しないより早く発火する側に倒す。
    private static let wheelsHideTimeout: TimeInterval = 1.0

    override init(frame: CGRect) {
        super.init(frame: frame)
        embeddedField.translatesAutoresizingMaskIntoConstraints = true
        embeddedField.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        embeddedField.frame = contentView.bounds
        contentView.addSubview(embeddedField)
        contentView.sendSubviewToBack(embeddedField)

        embeddedField.inputView = wheelsPicker
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleLocaleChanged),
            name: NSLocale.currentLocaleDidChangeNotification,
            object: nil
        )
    }

    func render(cell: any KsCell, theme: Theme) {
        guard let dc = cell as? DatePickerCell else {
            assertionFailure("DatePickerCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        self.lastCell = dc
        self.lastTheme = theme
        let locale = localeProvider()
        renderRow(cell: dc, theme: theme, locale: locale)

        // Wheels モードの Picker を最新化（Calendar モードは提示時に動的構築するため不要）
        wheelsPicker.locale = locale
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

    private func renderRow(cell: DatePickerCell, theme: Theme, locale: Locale) {
        let effective = EffectiveStyle(theme: theme, cellStyle: cell.style)

        applyCellBaseLayout(
            self,
            title: cell.title,
            description: cell.description,
            icon: cell.icon,
            hintText: cell.hintText,
            effective: effective,
            theme: theme,
            isEnabled: cell.isEnabled,
            valueLabelText: cell.effectiveValueText(locale: locale),
            accessoryView: makeChevronView()
        )
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
            locale: localeProvider(),
            onDone: { [weak self] newDate in
                self?.applyDoneDate(newDate, for: cell)
            },
            onDismissed: { [weak self] in
                // dismiss 完了で参照を解放し、次回タップで再提示できるようにする
                self?.currentCalendarController = nil
            },
            onDoneCompleted: { [weak self] newDate in
                guard let self else { return }
                cell.onValueCompleted?(self.normalizedDoneDate(newDate, for: cell))
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
        self.lastTheme = nil
        self.currentCalendarController = nil
        cancelWheelsHideWatch()
    }

    @objc private func handleLocaleChanged() {
        guard let cell = lastCell, let theme = lastTheme else { return }
        let locale = localeProvider()
        renderRow(cell: cell, theme: theme, locale: locale)
        wheelsPicker.locale = locale
        currentCalendarController?.applyLocale(locale)
        if embeddedField.isFirstResponder {
            embeddedField.reloadInputViews()
        }
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
        let confirmed = normalizedDoneDate(wheelsPicker.date, for: cell)
        // 先の確定の待ち受けが残っていれば捨て、最後の確定に対する 1 回として張り直す。
        cancelWheelsHideWatch()
        if let notify = cell.onValueCompleted {
            beginWheelsHideWatch(date: confirmed, notify: notify)
        }
        let wasFirstResponder = embeddedField.isFirstResponder
        let didResign = embeddedField.resignFirstResponder()
        cell.onValueChanged?(confirmed)
        wheelsCompletionWatch?.isValueDelivered = true
        if !wasFirstResponder || !didResign {
            // 入力面が出ていないため非表示完了の通知は届かない。すでに閉じ切ったものとして扱う。
            wheelsCompletionWatch?.isHideReported = true
        }
        deliverWheelsCompletionIfReady()
    }

    // MARK: - ホイール入力面の閉じ切り待ち

    /// 入力面の非表示完了を一回限り待ち受ける。確定した日付は待ち受けと一緒に控える。
    private func beginWheelsHideWatch(date: Date, notify: @escaping @Sendable (Date) -> Void) {
        wheelsCompletionWatch = WheelsCompletionWatch(date: date, notify: notify)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleKeyboardDidHide),
            name: UIResponder.keyboardDidHideNotification,
            object: nil
        )
        wheelsHideTimeoutTask = Task { @MainActor [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(Self.wheelsHideTimeout * 1_000_000_000))
            guard !Task.isCancelled else { return }
            // 非表示完了が届かないまま上限時間を過ぎた。発火しないより早く発火する側に倒す。
            self?.markWheelsHideReported()
        }
    }

    /// 入力面の非表示完了が報告された。値 callback の送出が済んでいれば閉じ切りを知らせる。
    private func markWheelsHideReported() {
        guard wheelsCompletionWatch != nil else { return }
        wheelsCompletionWatch?.isHideReported = true
        deliverWheelsCompletionIfReady()
    }

    /// 値 callback の送出と非表示完了の報告がそろっていれば、控えた確定日付を通知して待ち受けを畳む。
    private func deliverWheelsCompletionIfReady() {
        guard let watch = wheelsCompletionWatch,
              watch.isValueDelivered,
              watch.isHideReported else { return }
        cancelWheelsHideWatch()
        watch.notify(watch.date)
    }

    /// 待ち受けを通知せずに畳む。
    private func cancelWheelsHideWatch() {
        guard wheelsCompletionWatch != nil else { return }
        wheelsCompletionWatch = nil
        wheelsHideTimeoutTask?.cancel()
        wheelsHideTimeoutTask = nil
        NotificationCenter.default.removeObserver(
            self,
            name: UIResponder.keyboardDidHideNotification,
            object: nil
        )
    }

    @objc private func handleKeyboardDidHide() {
        markWheelsHideReported()
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

    /// Done 確定時の共通処理。正規化した日付を値の callback へ渡す。
    private func applyDoneDate(_ newDate: Date, for cell: DatePickerCell) {
        cell.onValueChanged?(normalizedDoneDate(newDate, for: cell))
    }

    /// 確定した日付を通知用に正規化する。year/month/day のみ反映し、
    /// 元 cell.date の hour/minute/second を保持する。
    private func normalizedDoneDate(_ newDate: Date, for cell: DatePickerCell) -> Date {
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
        return calendar.date(from: combined) ?? newDate
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
    internal var _wheelsLocale: Locale? { wheelsPicker.locale }
    internal func _setLocaleProviderForTesting(_ provider: @escaping @MainActor () -> Locale) {
        localeProvider = provider
    }
    internal func _applyLocaleForTesting(_ locale: Locale) { wheelsPicker.locale = locale }
    internal func _simulateWheelsChange(to newDate: Date) { wheelsPicker.date = newDate }
    internal func _simulateWheelsDone() { handleWheelsDone() }
    internal func _simulateWheelsCancel() { handleWheelsCancel() }
    internal func _simulateWheelsToday() { handleWheelsToday() }
    internal var _currentCalendarController: DatePickerCalendarSheetController? { currentCalendarController }
    internal func _registerCalendarControllerForTesting(_ controller: DatePickerCalendarSheetController) {
        currentCalendarController = controller
    }
    /// テスト用: ホイール入力面の閉じ切りを待ち受けている最中かどうか。
    internal var _isAwaitingWheelsHide: Bool { wheelsCompletionWatch != nil }
    /// テスト用: ホイールの入力面を担う透明フィールドが first responder かどうか。
    internal var _embeddedFieldIsFirstResponder: Bool { embeddedField.isFirstResponder }
}
#endif

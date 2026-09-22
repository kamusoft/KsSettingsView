// TimePickerCellView.swift
// KsSettingsViewUI
//
// `TimePickerCell` の Renderer 実装。共通行レイアウト関数経由で描画し、accessory slot に
// chevron。タップで埋め込み `UIDatePicker(.time)` を `inputView` 経由でキーボード位置に
// スライドアップ表示する（AiForms 互換の埋め込み方式）。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

@MainActor
internal final class TimePickerCellView: KsListCellBase, @MainActor KsCellRenderer {
    internal var tapHandler: (@Sendable () -> Void)?
    private var lastCell: TimePickerCell?
    private var lastTheme: Theme?
    private var localeProvider: @MainActor () -> Locale = { UserInterfaceLocale.current }

    /// Cancel 時に戻すための「Picker 提示開始時点」の Date。
    private var preSelectedDate: Date = Date()

    /// 透明 no-caret な UITextField。`inputView = UIDatePicker(.time)` で時刻ピッカーを表示する土台。
    private let embeddedField = EmbeddedPickerHostField()

    /// `embeddedField.inputView` にセットする UIDatePicker(.time)。
    private let datePicker: UIDatePicker = {
        let p = UIDatePicker()
        p.datePickerMode = .time
        if #available(iOS 13.4, *) {
            p.preferredDatePickerStyle = .wheels
        }
        return p
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        embeddedField.translatesAutoresizingMaskIntoConstraints = true
        embeddedField.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        embeddedField.frame = contentView.bounds
        contentView.addSubview(embeddedField)
        contentView.sendSubviewToBack(embeddedField)

        embeddedField.inputView = datePicker
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleLocaleChanged),
            name: NSLocale.currentLocaleDidChangeNotification,
            object: nil
        )
    }

    func render(cell: any KsCell, theme: Theme) {
        guard let tc = cell as? TimePickerCell else {
            assertionFailure("TimePickerCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        self.lastCell = tc
        self.lastTheme = theme
        let locale = localeProvider()
        renderRow(cell: tc, theme: theme, locale: locale)

        // 選択面の時制は cell の `is24Hour` だけで決まり、端末の地域・24時間表示設定には
        // 依存しない（core/ADR-0028）。差し替えるのは hour cycle のみで、午前/午後の
        // 表記の言語は端末 Locale 由来のまま保たれる。
        datePicker.locale = HourCycleLocale.forcing(is24Hour: tc.is24Hour, base: locale)

        // 現在の time を Picker と preSelectedDate に反映
        datePicker.date = tc.time
        self.preSelectedDate = tc.time
        if let c = tc.accentColor {
            datePicker.tintColor = c
        }

        // Toolbar 組み直し
        rebuildToolbar(for: tc)

        if tc.isEnabled {
            let handler: @Sendable () -> Void = { [weak self] in
                Task { @MainActor in
                    self?.embeddedField.becomeFirstResponder()
                }
            }
            self.tapHandler = handler
        } else {
            self.tapHandler = nil
        }
    }

    private func renderRow(cell: TimePickerCell, theme: Theme, locale: Locale) {
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

    private func rebuildToolbar(for cell: TimePickerCell) {
        let built = EmbeddedPickerToolbar.build(
            title: cell.pickerTitle,
            todayText: nil,
            accentColor: cell.accentColor,
            cancelTarget: self,
            cancelAction: #selector(handleCancel),
            doneTarget: self,
            doneAction: #selector(handleDone),
            todayTarget: nil,
            todayAction: nil
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
    }

    @objc private func handleLocaleChanged() {
        guard let cell = lastCell, let theme = lastTheme else { return }
        let locale = localeProvider()
        renderRow(cell: cell, theme: theme, locale: locale)
        datePicker.locale = HourCycleLocale.forcing(is24Hour: cell.is24Hour, base: locale)
        if embeddedField.isFirstResponder {
            embeddedField.reloadInputViews()
        }
    }

    @objc private func handleCancel() {
        datePicker.date = preSelectedDate
        embeddedField.resignFirstResponder()
    }

    @objc private func handleDone() {
        guard let cell = lastCell else {
            embeddedField.resignFirstResponder()
            return
        }
        let newDate = datePicker.date
        // 仕様: TimePickerCell は hour/minute のみを参照する。
        // 元の `cell.time` の year/month/day 成分を保ち、hour/minute を newDate で置換した
        // Date を callback に渡す。
        let calendar = Calendar.current
        let hm = calendar.dateComponents([.hour, .minute], from: newDate)
        let combined = calendar.date(
            bySettingHour: hm.hour ?? 0,
            minute: hm.minute ?? 0,
            second: 0,
            of: cell.time
        ) ?? newDate
        embeddedField.resignFirstResponder()
        cell.onValueChanged?(combined)
    }

    // MARK: - test hook

    internal func _simulateTap() { tapHandler?() }
    internal var _lastCell: TimePickerCell? { lastCell }
    internal var _currentPickerDate: Date { datePicker.date }
    internal var _pickerLocale: Locale? { datePicker.locale }
    internal func _setLocaleProviderForTesting(_ provider: @escaping @MainActor () -> Locale) {
        localeProvider = provider
    }
    internal func _applyLocaleForTesting(_ locale: Locale) {
        datePicker.locale = HourCycleLocale.forcing(is24Hour: lastCell?.is24Hour ?? true, base: locale)
    }
    internal func _simulateChange(to newDate: Date) { datePicker.date = newDate }
    internal func _simulateDone() { handleDone() }
    internal func _simulateCancel() { handleCancel() }
}
#endif

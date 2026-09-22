// DatePickerCalendarSheetController.swift
// KsSettingsViewUI
//
// `DatePickerCell.uiStyle == .calendar` で表示される、カレンダー grid 形式の DatePicker
// 提示用 ViewController。
//
// 表示形式: `.pageSheet` + `.custom` detent (≒ 480pt)
// 中身: `UIDatePicker` (`.date` モード、`preferredDatePickerStyle = .inline`)
//        + 下部ボタンバー [キャンセル] [Today?] [完了]
//
// Today ボタンの挙動: `.inline` カレンダーの表示月と選択中の日付を、どちらも `Date()`
//                   (today) へ移す。値の確定は完了ボタンの責務なので、このボタン自体は
//                   変更を通知しない。today が min/max の範囲外なら何もしない。

#if canImport(UIKit)
import Foundation
import UIKit

@MainActor
internal final class DatePickerCalendarSheetController: UIViewController, UIAdaptivePresentationControllerDelegate {

    private let initial: Date
    private let minimumDate: Date?
    private let maximumDate: Date?
    private let pickerTitle: String?
    private let todayText: String?
    private let accentColor: UIColor?
    private var locale: Locale
    private let onDone: (Date) -> Void
    private let onDismissed: (() -> Void)?
    /// 確定（完了ボタン）で閉じ切った後に、確定した日付を届ける callback。
    private let onDoneCompleted: ((Date) -> Void)?

    /// Sheet の高さ（detent custom 値）。`.inline` カレンダー grid + ボタンバー + 上下マージン。
    private static let sheetHeight: CGFloat = 480

    /// Picker 本体。`.inline` で表示することで Image #5 のようなカレンダー grid になる。
    private let datePicker: UIDatePicker = {
        let p = UIDatePicker()
        p.datePickerMode = .date
        p.preferredDatePickerStyle = .inline
        return p
    }()

    init(
        initial: Date,
        minimumDate: Date?,
        maximumDate: Date?,
        pickerTitle: String?,
        todayText: String?,
        accentColor: UIColor?,
        locale: Locale = UserInterfaceLocale.current,
        onDone: @escaping (Date) -> Void,
        onDismissed: (() -> Void)? = nil,
        onDoneCompleted: ((Date) -> Void)? = nil
    ) {
        self.initial = initial
        self.minimumDate = minimumDate
        self.maximumDate = maximumDate
        self.pickerTitle = pickerTitle
        self.todayText = todayText
        self.accentColor = accentColor
        self.locale = locale
        self.onDone = onDone
        self.onDismissed = onDismissed
        self.onDoneCompleted = onDoneCompleted
        super.init(nibName: nil, bundle: nil)
        // Sheet 提示の設定
        self.modalPresentationStyle = .pageSheet
        if let sheet = self.sheetPresentationController {
            if #available(iOS 16.0, *) {
                sheet.detents = [
                    .custom(identifier: .init("ks-datepicker-calendar")) { _ in
                        DatePickerCalendarSheetController.sheetHeight
                    }
                ]
            } else {
                // iOS 15: .medium で fallback (.custom detent は iOS 16+)
                sheet.detents = [.medium()]
            }
            sheet.prefersGrabberVisible = true
            sheet.preferredCornerRadius = 16
        }
        // スワイプダウン/対話的 dismiss 完了で onDismissed を発火させる
        self.presentationController?.delegate = self
    }

    /// スワイプダウン dismiss 完了で呼ばれる。`handleCancel` / `handleDone` 経由の
    /// `dismiss(completion:)` とは経路が違うため、ここでも `onDismissed` を発火させる。
    nonisolated func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        Task { @MainActor [weak self] in
            self?.onDismissed?()
        }
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        // タイトルラベル（pickerTitle 指定時のみ）
        let titleLabel: UILabel? = {
            guard let t = pickerTitle, !t.isEmpty else { return nil }
            let l = UILabel()
            l.text = t
            l.font = .preferredFont(forTextStyle: .headline)
            l.textAlignment = .center
            return l
        }()

        // DatePicker 設定
        datePicker.locale = locale
        datePicker.date = initial
        datePicker.minimumDate = minimumDate
        datePicker.maximumDate = maximumDate
        if let c = accentColor {
            datePicker.tintColor = c
        }

        // ボタンバー（下部）
        let cancelBtn = UIButton(type: .system)
        cancelBtn.setTitle(NSLocalizedString("Cancel", comment: ""), for: .normal)
        cancelBtn.addTarget(self, action: #selector(handleCancel), for: .touchUpInside)

        let doneBtn = UIButton(type: .system)
        doneBtn.setTitle(NSLocalizedString("Done", comment: ""), for: .normal)
        doneBtn.titleLabel?.font = .preferredFont(forTextStyle: .headline)
        doneBtn.addTarget(self, action: #selector(handleDone), for: .touchUpInside)

        var buttonViews: [UIView] = [cancelBtn]
        if let todayText = todayText, !todayText.isEmpty {
            let todayBtn = UIButton(type: .system)
            todayBtn.setTitle(todayText, for: .normal)
            todayBtn.addTarget(self, action: #selector(handleToday), for: .touchUpInside)
            buttonViews.append(todayBtn)
        }
        buttonViews.append(doneBtn)

        let buttonBar = UIStackView(arrangedSubviews: buttonViews)
        buttonBar.axis = .horizontal
        buttonBar.distribution = .equalSpacing
        buttonBar.alignment = .center

        if let c = accentColor {
            cancelBtn.tintColor = c
            doneBtn.tintColor = c
        }

        // 全体縦スタック
        let stack = UIStackView()
        stack.axis = .vertical
        stack.spacing = 12
        stack.alignment = .fill
        stack.layoutMargins = UIEdgeInsets(top: 16, left: 20, bottom: 16, right: 20)
        stack.isLayoutMarginsRelativeArrangement = true
        if let titleLabel = titleLabel {
            stack.addArrangedSubview(titleLabel)
        }
        stack.addArrangedSubview(datePicker)
        stack.addArrangedSubview(buttonBar)

        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            stack.bottomAnchor.constraint(lessThanOrEqualTo: view.safeAreaLayoutGuide.bottomAnchor),
        ])
    }

    @objc private func handleCancel() {
        // 非確定で閉じる経路は完了通知を伴わない。
        dismissSheet()
    }

    /// OS の Locale 変更を表示中のカレンダーへ反映する。
    internal func applyLocale(_ locale: Locale) {
        self.locale = locale
        datePicker.locale = locale
    }

    @objc private func handleDone() {
        let confirmed = datePicker.date
        onDone(confirmed)
        dismissSheet { [weak self] in
            self?.onDoneCompleted?(confirmed)
        }
    }

    /// シートを閉じ、閉じ切った後に参照解放（`onDismissed`）と、確定経路だけが渡す
    /// `completion` をこの順で呼ぶ。
    private func dismissSheet(completion: (() -> Void)? = nil) {
        guard presentingViewController != nil else {
            // シートとして提示されていない（閉じる対象が無い）ため、すでに閉じ切った状態として扱う。
            onDismissed?()
            completion?()
            return
        }
        dismiss(animated: true) { [weak self] in
            self?.onDismissed?()
            completion?()
        }
    }

    /// `.inline` カレンダーを today にジャンプさせる（表示月・選択日とも today へ）。
    ///
    /// 仕様: 「選択状態にかかわらず今日のページに移動すべき」（ユーザー確認済）。
    /// 既に today が選択されているケースでも UIKit が「変化なし」と判定して動かないことが
    /// あるため、いったん別 date を入れて差分を作ったあと today へ setDate する。
    @objc private func handleToday() {
        let today = Date()
        let calendar = Calendar.current
        let todayStart = calendar.startOfDay(for: today)
        // 範囲チェックは「日単位」で比較する（時刻成分まで比較すると、
        // `maximumDate: Date()` のように "今この瞬間" 指定で today > max になり弾かれる）。
        if let min = minimumDate, todayStart < calendar.startOfDay(for: min) { return }
        if let max = maximumDate, todayStart > calendar.startOfDay(for: max) { return }

        // 同日選択中でも確実に再描画させるためダミー値を一度噛ませる
        if calendar.isDate(datePicker.date, inSameDayAs: today) {
            if let bump = calendar.date(byAdding: .second, value: 1, to: todayStart) {
                datePicker.setDate(bump, animated: false)
            }
        }
        datePicker.setDate(todayStart, animated: true)
        datePicker.sendActions(for: .valueChanged)
    }

    // MARK: - test hook

    internal var _currentDate: Date { datePicker.date }
    internal var _pickerLocale: Locale? { datePicker.locale }
    internal func _simulateChange(to newDate: Date) { datePicker.date = newDate }
    internal func _simulateDone() { handleDone() }
    internal func _simulateCancel() { handleCancel() }
    internal func _simulateToday() { handleToday() }
}
#endif

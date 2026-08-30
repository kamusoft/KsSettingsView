// NumberPickerCellView.swift
// KsSettingsViewUI
//
// `NumberPickerCell` の Renderer 実装。共通行レイアウト関数経由で描画し、accessory slot に
// chevron。タップで埋め込み `UIPickerView` を `inputView` 経由でキーボード位置に
// スライドアップ表示する（AiForms 互換の埋め込み方式）。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

@MainActor
internal final class NumberPickerCellView: KsListCellBase, @MainActor KsCellRenderer, UIPickerViewDataSource, UIPickerViewDelegate {
    /// `embeddedField.becomeFirstResponder()` 経路用 tap ハンドラ。
    internal var tapHandler: (@Sendable () -> Void)?

    /// 直近 bind された Cell。Picker のデータソース・確定時の callback 解決に使う。
    private var lastCell: NumberPickerCell?

    /// 現在の候補値（min..max を step 刻みで列挙）。
    private var candidates: [Int] = []

    /// 現在の選択インデックス（Picker の selectedRow）。
    private var currentIndex: Int = 0

    /// Cancel 時に戻すための「Picker 提示開始時点」のインデックス。
    private var preSelectedIndex: Int = 0

    /// 透明 no-caret な UITextField。`inputView = UIPickerView` で Picker を表示するための土台。
    private let embeddedField = EmbeddedPickerHostField()

    /// `embeddedField.inputView` にセットする UIPickerView。
    private let pickerView = UIPickerView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        // embeddedField を contentView の最背面に貼り、サイズは layoutSubviews で frame に追従。
        // SendSubviewToBack で前面コンテンツ（titleLabel / chevron 等）と物理的に重ならないようにする。
        embeddedField.translatesAutoresizingMaskIntoConstraints = true
        embeddedField.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        embeddedField.frame = contentView.bounds
        contentView.addSubview(embeddedField)
        contentView.sendSubviewToBack(embeddedField)

        pickerView.dataSource = self
        pickerView.delegate = self
        embeddedField.inputView = pickerView
    }

    func render(cell: any KsCell, theme: Theme) {
        guard let np = cell as? NumberPickerCell else {
            assertionFailure("NumberPickerCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        self.lastCell = np
        let effective = EffectiveStyle(theme: theme, cellStyle: np.style)

        applyCellBaseLayout(
            self,
            title: np.title,
            description: np.description,
            icon: np.icon,
            hintText: np.hintText,
            effective: effective,
            theme: theme,
            isEnabled: np.isEnabled,
            valueLabelText: np.effectiveValueText(),
            accessoryView: makeChevronView()
        )

        // 候補値リストを更新（step は 0 以下なら 1 にフォールバック）
        let safeStep = np.step > 0 ? np.step : 1
        if np.min <= np.max {
            var list: [Int] = []
            var v = np.min
            while v <= np.max {
                list.append(v)
                v += safeStep
            }
            if list.isEmpty { list = [np.min] }
            self.candidates = list
        } else {
            // min > max の壊れた範囲は最低 1 件確保（範囲 [min] のみ）
            self.candidates = [np.min]
        }

        // 現在値に最も近い index を初期選択にする（範囲外なら clamp）
        let clamped = Swift.min(Swift.max(np.value, np.min), np.max)
        let initialIdx = candidates.firstIndex(of: clamped) ?? 0
        self.currentIndex = initialIdx
        self.preSelectedIndex = initialIdx
        pickerView.reloadAllComponents()
        pickerView.selectRow(initialIdx, inComponent: 0, animated: false)

        // Toolbar を組み直し（accentColor / title / target-action は render 毎に最新化）
        rebuildToolbar(for: np)

        // tapHandler の最新化
        if np.isEnabled {
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

    /// Toolbar を組み立てて `embeddedField.inputAccessoryView` に紐づける。
    private func rebuildToolbar(for cell: NumberPickerCell) {
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
        // Toolbar を差し替えた直後に first responder 状態なら、反映のため再ロードが必要なことがある。
        // 通常 render は提示前なので reloadInputViews は不要。
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        // 編集中の状態を残さない
        if embeddedField.isFirstResponder {
            embeddedField.resignFirstResponder()
        }
        self.tapHandler = nil
        self.lastCell = nil
        self.candidates = []
        self.currentIndex = 0
        self.preSelectedIndex = 0
    }

    // MARK: - Toolbar 操作

    @objc private func handleCancel() {
        // Cancel: 選択を提示開始時点に戻す
        currentIndex = preSelectedIndex
        pickerView.selectRow(preSelectedIndex, inComponent: 0, animated: false)
        embeddedField.resignFirstResponder()
    }

    @objc private func handleDone() {
        guard let cell = lastCell, !candidates.isEmpty else {
            embeddedField.resignFirstResponder()
            return
        }
        let newValue = candidates[Swift.min(currentIndex, candidates.count - 1)]
        embeddedField.resignFirstResponder()
        cell.onValueChanged?(newValue)
    }

    // MARK: - UIPickerViewDataSource / Delegate

    func numberOfComponents(in pickerView: UIPickerView) -> Int { 1 }

    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return candidates.count
    }

    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        guard row >= 0 && row < candidates.count else { return nil }
        let unit = lastCell?.unit ?? ""
        return NumberPickerCell.format(value: candidates[row], unit: unit)
    }

    func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        currentIndex = row
    }

    // MARK: - test hook

    internal func _simulateTap() { tapHandler?() }
    internal var _lastCell: NumberPickerCell? { lastCell }
    internal var _candidates: [Int] { candidates }
    internal var _currentValue: Int? {
        guard currentIndex >= 0 && currentIndex < candidates.count else { return nil }
        return candidates[currentIndex]
    }
    internal func _simulateSelect(value: Int) {
        if let idx = candidates.firstIndex(of: value) {
            currentIndex = idx
            pickerView.selectRow(idx, inComponent: 0, animated: false)
        }
    }
    internal func _simulateDone() { handleDone() }
    internal func _simulateCancel() { handleCancel() }
    internal func _pickerTitle(forRow row: Int) -> String? {
        return self.pickerView(pickerView, titleForRow: row, forComponent: 0)
    }
}
#endif

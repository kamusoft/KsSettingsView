// EntryCellView.swift
// KsSettingsViewUI
//
// `EntryCell` の Renderer 実装。共通行レイアウト関数 `applyCellBaseLayout(...)` 経由で描画し、
// `trailingViews` に `UITextField` を渡して title 残り領域全幅で配置する。
// `keyboardType` は **`UIKeyboardType`** を直接 `UITextField.keyboardType` に代入する。
//
// `UIListContentConfiguration` / `UICellAccessory` は使わず、共通行レイアウトへ `UITextField` を
// `trailingViews` として追加する（core/ADR-0011）。`UITextField` は `fieldWrapper` に包んで渡し、
// wrapper の Hugging を低く・CCR を required にすることで title 右側の残り領域全幅を占有する。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// `EntryCell` 描画用 Cell View。
@MainActor
internal final class EntryCellView: KsListCellBase, @MainActor KsCellRenderer, UITextFieldDelegate {
    /// 値変更時に呼ばれるクロージャ。最新の bind 時の `EntryCell.onTextChanged` を保持する。
    internal var textChangedHandler: (@Sendable (String) -> Void)?
    /// 現在 bind されている `maxLength`（`nil` で無制限）。
    private var currentMaxLength: Int?
    /// 「自身が send した変更」かどうかをガードするフラグ（reset → setText のループ防止）。
    private var isProgrammaticUpdate: Bool = false
    /// `isSecureTextEntry == true` のフォーカス取得時、iOS が text を自動クリアする前の退避値。
    /// `editingChanged` の初回で `current + 新入力` の形で復元するために使用する。
    /// nil = 復元不要（非 secure / 既に復元済み / フォーカス外）。
    private var secureSavedText: String?

    /// title の右に配置する `UITextField`。`fieldWrapper` の subview として Auto Layout で pin される。
    /// `NoIntrinsicWidthTextField` は `intrinsicContentSize.width` を `UIView.noIntrinsicMetric` に
    /// 上書きし、`secureTextEntry` 時の小さな intrinsicContentSize (~19pt) や入力内容の幅変動が
    /// wrapper / contentStack の Auto Layout 計算に伝わらないようにする。
    /// これにより wrapper サイズは「title 右側の残り全幅」のみで決まる。
    private let textField: NoIntrinsicWidthTextField = {
        let tf = NoIntrinsicWidthTextField()
        tf.borderStyle = .none
        tf.textAlignment = .right
        tf.returnKeyType = .done
        tf.adjustsFontSizeToFitWidth = false
        return tf
    }()

    /// `textField` を内包する UIView ラッパ。これを `contentStack` に入れる。
    /// AiForms オリジナル `EntryCellView.cs` の `_FieldWrapper` 相当
    /// (`SetContentHuggingPriority(100f, .Horizontal)` / `SetContentCompressionResistancePriority(100f, .Horizontal)`)。
    ///
    /// 直接 `textField` を `contentStack` に入れると `UITextField.intrinsicContentSize`
    /// が placeholder / secureTextEntry / 入力内容で変動し、stackView の伸縮挙動が崩れて
    /// 「placeholder 表示時に位置がずれる」「パスワードフォーカス時に左端が欠ける」現象が起きる。
    /// wrapper を介すことで、stackView の伸縮対象は wrapper（intrinsicContentSize なし）になり、
    /// title の右側残り領域を確実に占有できる。textField は wrapper に四辺 pin で追従。
    private let fieldWrapper: UIView = {
        let wrapper = UIView()
        // Hugging を低く: title 右側の残り領域を吸って広がる。
        wrapper.setContentHuggingPriority(.init(100), for: .horizontal)
        // CCR を **required** に: textField (secureTextEntry) の intrinsicContentSize が
        // 19pt 程度に縮むため、CCR が低いと wrapper も 19pt に圧縮される。
        // required にして wrapper サイズが「title 右側の残り全幅」を維持するよう保証する。
        wrapper.setContentCompressionResistancePriority(.required, for: .horizontal)
        return wrapper
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        textField.delegate = self
        textField.addTarget(self, action: #selector(handleEditingChanged(_:)), for: .editingChanged)
        // wrapper は contentStack に入って残り領域全幅を取得、textField は wrapper に Auto Layout で pin。
        textField.translatesAutoresizingMaskIntoConstraints = false
        fieldWrapper.addSubview(textField)
        NSLayoutConstraint.activate([
            textField.leadingAnchor.constraint(equalTo: fieldWrapper.leadingAnchor),
            textField.trailingAnchor.constraint(equalTo: fieldWrapper.trailingAnchor),
            textField.topAnchor.constraint(equalTo: fieldWrapper.topAnchor),
            textField.bottomAnchor.constraint(equalTo: fieldWrapper.bottomAnchor),
        ])
        // AiForms 互換: Done ボタン付き UIToolbar を inputAccessoryView に常時設定する
        // （キーボード種別によらず Done ツールバーが表示される）。
        textField.inputAccessoryView = makeDoneToolbar()
    }

    /// `UIToolbar`（右端に Done ボタン）を生成する。Done タップで `textField.resignFirstResponder()` を呼ぶ。
    /// frame に有効な幅を渡しておかないと AutoresizingMask 由来の `_UIToolbarContentView.width == 0`
    /// 制約と Toolbar 内部の `TB_Trailing_Trailing` 制約が衝突する（LayoutConstraints 警告）。
    /// AiForms オリジナル `EntryCellView.cs` の `new UIToolbar(new CGRect(0, 0, 50, 44))` 相当。
    private func makeDoneToolbar() -> UIToolbar {
        let screenWidth = UIScreen.main.bounds.width
        let bar = UIToolbar(frame: CGRect(x: 0, y: 0, width: screenWidth, height: 44))
        bar.autoresizingMask = [.flexibleWidth]
        let flex = UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil)
        let done = UIBarButtonItem(
            barButtonSystemItem: .done,
            target: self,
            action: #selector(handleDoneTap)
        )
        bar.items = [flex, done]
        return bar
    }

    @objc private func handleDoneTap() {
        textField.resignFirstResponder()
    }

    /// Cell タップ時に `UITextField.becomeFirstResponder()` を呼ぶハンドラ。
    ///
    /// `KsSettingsViewController.collectionView(_:didSelectItemAt:)` 経路で
    /// `TapNotifyingRenderer.tapHandler` 経由でディスパッチされる。これにより Cell 行の
    /// どこをタップしても `UITextField` にフォーカスが当たる。
    internal var tapHandler: (@Sendable () -> Void)? {
        return { [weak self] in
            Task { @MainActor in
                self?.textField.becomeFirstResponder()
            }
        }
    }

    func render(cell: any KsCell, theme: Theme) {
        guard let entry = cell as? EntryCell else {
            assertionFailure("EntryCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        let effective = EffectiveStyle(theme: theme, cellStyle: entry.style)

        // **AiForms 互換: 差分があるときだけ `textField.text` を更新する**。
        // オリジナル `EntryCellView.cs` `UpdateValueText()` のコメント:
        //   "Without this judging, TextField don't correctly work when inputting Japanese
        //   (maybe other 2byte languages either)."
        // 日本語 IME は変換中に textField.text を読み書きするため、同値を再代入すると
        // マークドテキスト（変換途中の下線付きテキスト）が破壊され入力不能になる。
        // 差分判定で「値が変わったときだけ」代入する。
        isProgrammaticUpdate = true
        if textField.text != entry.text {
            textField.text = entry.text
        }
        isProgrammaticUpdate = false

        // **Native 型直接代入**: `UIKeyboardType` を経由型なしで `UITextField.keyboardType` へ
        textField.keyboardType = entry.keyboardType
        textField.isSecureTextEntry = entry.isPassword
        textField.textAlignment = nsTextAlignment(for: entry.textAlignment)
        textField.isEnabled = entry.isEnabled
        textField.font = effective.valueTextFont
        textField.textColor = entry.isEnabled ? effective.valueTextColor : effective.disabledTextColor

        // placeholder は色指定の有無で表示経路が分かれる（色指定時のみ attributed 表示）。
        applyPlaceholder(
            entry.placeholder,
            color: EffectiveStyle.effectivePlaceholderColor(
                entryPlaceholderColor: entry.placeholderColor,
                cellStyle: entry.style,
                theme: theme
            ),
            font: effective.valueTextFont
        )

        // accentColor の 4 段階解決
        if let c = entry.accentColor {
            textField.tintColor = c
        } else {
            textField.tintColor = effective.accentColor
        }

        // maxLength を保持（textField:shouldChangeCharactersIn: で参照）。
        currentMaxLength = entry.maxLength

        // 共通行レイアウト関数経由で描画。`EntryCell` は valueText を持たないため `valueLabelText` は nil。
        applyCellBaseLayout(
            self,
            title: entry.title,
            description: entry.description,
            icon: entry.icon,
            hintText: entry.hintText,
            effective: effective,
            theme: theme,
            isEnabled: entry.isEnabled,
            trailingViews: [fieldWrapper],
            valueLabelText: nil
        )

        self.textChangedHandler = entry.onTextChanged
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        self.textChangedHandler = nil
        currentMaxLength = nil
        isProgrammaticUpdate = true
        textField.text = nil
        textField.attributedPlaceholder = nil
        textField.placeholder = nil
        textField.isEnabled = true
        textField.keyboardType = .default
        textField.isSecureTextEntry = false
        // 前回 bind 時の見た目属性が次回 reuse に漏れないよう初期状態へ戻す。
        textField.tintColor = nil
        textField.textColor = nil
        textField.font = nil
        textField.textAlignment = .right
        isProgrammaticUpdate = false
    }

    /// placeholder を表示する。
    ///
    /// 色が解決されているときは `attributedPlaceholder` へ切り替え、font は入力テキストと同じ
    /// 実効 font を明示的に載せる（属性文字列は `UITextField.font` を継がないため、色指定の有無で
    /// placeholder の font が変わらないよう毎回付与する）。色が未解決のときはプレーン表示に戻し、
    /// システム既定色の placeholder を維持する。
    /// 文字列が `nil` のときは色指定の有無に関わらず placeholder を持たせない。
    private func applyPlaceholder(_ text: String?, color: UIColor?, font: UIFont) {
        guard let text else {
            textField.attributedPlaceholder = nil
            textField.placeholder = nil
            return
        }
        if let color {
            textField.attributedPlaceholder = NSAttributedString(
                string: text,
                attributes: [.foregroundColor: color, .font: font]
            )
        } else {
            textField.attributedPlaceholder = nil
            textField.placeholder = text
        }
    }

    // MARK: - editingChanged

    @objc private func handleEditingChanged(_ sender: UITextField) {
        guard !isProgrammaticUpdate else { return }
        // **iOS の `isSecureTextEntry` 自動クリア対策**：
        // secureTextEntry が `true` の UITextField は、フォーカス取得後の初回入力時に
        // 既存テキストを iOS が自動的に全クリアする（pasteboard 経由のパスワード窃取防止機構）。
        // `textFieldDidBeginEditing` で退避した `secureSavedText` を、初回入力分の前に prepend して復元する。
        // ref: https://stackoverflow.com/questions/7305538/
        if let saved = secureSavedText, sender.isSecureTextEntry {
            secureSavedText = nil
            let typed = sender.text ?? ""
            isProgrammaticUpdate = true
            sender.text = saved + typed
            isProgrammaticUpdate = false
        }
        textChangedHandler?(sender.text ?? "")
    }

    // MARK: - UITextFieldDelegate（maxLength 制限）

    func textField(
        _ textField: UITextField,
        shouldChangeCharactersIn range: NSRange,
        replacementString string: String
    ) -> Bool {
        guard let maxLength = currentMaxLength else { return true }
        let current = textField.text ?? ""
        guard let stringRange = Range(range, in: current) else { return true }
        let updated = current.replacingCharacters(in: stringRange, with: string)
        // 文字数（Unicode 文字単位）で判定する。
        return updated.count <= maxLength
    }

    /// `returnKeyType = .done` の場合、completion キー（「完了」/「Done」/「Return」など）を
    /// 押したときに `UITextField.resignFirstResponder()` を呼んでキーボードを閉じる。
    ///
    /// AiForms オリジナル `EntryCellView.cs` の `OnShouldReturn(...)` 準拠。
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }

    /// `isSecureTextEntry == true` のフォーカス取得時に既存 text を退避する。
    /// iOS が直後に text をクリアしても、`editingChanged` の初回で復元する。
    func textFieldDidBeginEditing(_ textField: UITextField) {
        if textField.isSecureTextEntry, let text = textField.text, !text.isEmpty {
            secureSavedText = text
        } else {
            secureSavedText = nil
        }
    }

    /// フォーカス終了時、復元待ちの退避値をクリアする（誤復元防止）。
    func textFieldDidEndEditing(_ textField: UITextField) {
        secureSavedText = nil
    }

    // MARK: - Helpers

    private func nsTextAlignment(for alignment: CellTitleAlignment) -> NSTextAlignment {
        switch alignment {
        case .start: return .left
        case .center: return .center
        case .end: return .right
        }
    }

    // MARK: - テスト用フック

    /// テストから現在の `UITextField` を覗くアクセサ。
    internal var _textField: UITextField { textField }

    /// テストからプログラム的に入力イベントをシミュレートするためのフック。
    internal func _simulateTextInput(_ newText: String) {
        textField.text = newText
        handleEditingChanged(textField)
    }
}

// MARK: - NoIntrinsicWidthTextField

/// `UITextField` の `intrinsicContentSize.width` を `UIView.noIntrinsicMetric` に固定するサブクラス。
///
/// 通常の `UITextField` は `intrinsicContentSize.width` を `isSecureTextEntry` / 入力内容で動的に
/// 変える（`secureTextEntry = true` の bullet 表示時には ~19pt まで縮む）。これが Auto Layout 経由で
/// 親 view (fieldWrapper) のサイズに伝搬すると、wrapper が「title 右側の残り全幅」を保てず縮む。
///
/// 幅の intrinsic を持たないことで、wrapper サイズは外側（contentStack の Distribution=.fill 配分）の
/// みで決まり、textField は 4 辺 pin で wrapper 全幅にフィットする。
/// 高さの intrinsic はそのまま維持する（行の高さ計算が壊れないように）。
internal final class NoIntrinsicWidthTextField: UITextField {
    override var intrinsicContentSize: CGSize {
        let base = super.intrinsicContentSize
        return CGSize(width: UIView.noIntrinsicMetric, height: base.height)
    }
}
#endif

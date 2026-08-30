// EmbeddedPickerToolbar.swift
// KsSettingsViewUI
//
// 埋め込み型 Picker (NumberPicker / TimePicker / DatePicker Wheels モード) 共通の
// `UIToolbar` 生成ヘルパ。
//
// レイアウト:
//   [Cancel]  flex  [Title?]  flex  [Today?]  fixedSpace  [Done]
//
// - title が nil なら中央の Title ラベルは省略（[Cancel] flex [Today?] fixed [Done]）
// - todayText が nil/空なら Today ボタンは省略
//
// AiForms オリジナル `NumberPickerCellView.cs` / `DatePickerCellView.cs` の SetUpXxxPicker()
// での toolbar 構築相当。frame に有効な幅を渡しておかないと AutoresizingMask 由来の
// `_UIToolbarContentView.width == 0` 警告が出るので、初期 frame は screen 幅で生成する。

#if canImport(UIKit)
import UIKit

/// 埋め込み型 Picker 用 Toolbar の生成ヘルパ。
@MainActor
internal enum EmbeddedPickerToolbar {

    /// 構築結果。`toolbar` を `UITextField.inputAccessoryView` にセットし、
    /// `cancelButton` / `doneButton` / `todayButton` の `target` / `action` を呼び出し側が紐づける。
    internal struct Built {
        let toolbar: UIToolbar
        let cancelButton: UIBarButtonItem
        let doneButton: UIBarButtonItem
        /// `todayText` が指定されていたときのみ非 nil。
        let todayButton: UIBarButtonItem?
    }

    /// Picker 用 Toolbar を組み立てる。
    ///
    /// - Parameters:
    ///   - title: 中央に表示するタイトル文字列（`nil` で省略）
    ///   - todayText: Today ボタンの表示文字列（`nil` / 空文字で省略）
    ///   - accentColor: ボタンの tint 色（`nil` で system 既定）
    ///   - cancelTarget / cancelAction: Cancel ボタンの target / action
    ///   - doneTarget / doneAction: Done ボタンの target / action
    ///   - todayTarget / todayAction: Today ボタンの target / action（`todayText` が non-nil/non-empty のときに反映）
    static func build(
        title: String?,
        todayText: String?,
        accentColor: UIColor?,
        cancelTarget: AnyObject,
        cancelAction: Selector,
        doneTarget: AnyObject,
        doneAction: Selector,
        todayTarget: AnyObject?,
        todayAction: Selector?
    ) -> Built {
        let screenWidth = UIScreen.main.bounds.width
        let toolbar = UIToolbar(frame: CGRect(x: 0, y: 0, width: screenWidth, height: 44))
        toolbar.autoresizingMask = [.flexibleWidth]
        toolbar.barStyle = .default
        toolbar.isTranslucent = true
        if let c = accentColor {
            toolbar.tintColor = c
        }

        let cancelButton = UIBarButtonItem(
            barButtonSystemItem: .cancel,
            target: cancelTarget,
            action: cancelAction
        )
        let doneButton = UIBarButtonItem(
            barButtonSystemItem: .done,
            target: doneTarget,
            action: doneAction
        )

        var items: [UIBarButtonItem] = [cancelButton]

        // 中央タイトル
        if let title = title, !title.isEmpty {
            let label = UILabel()
            label.text = title
            label.textAlignment = .center
            label.sizeToFit()
            // AiForms オリジナルと同じく、frame を 160x44 にしておくとレイアウトが
            // 安定する（toolbar の auto layout 経路で UILabel の intrinsic に揺らされない）。
            label.frame = CGRect(x: 0, y: 0, width: 160, height: 44)
            let titleItem = UIBarButtonItem(customView: label)
            items.append(UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil))
            items.append(titleItem)
        }

        items.append(UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil))

        // Today ボタン（todayText が non-nil/non-empty のときだけ追加）
        var todayButton: UIBarButtonItem? = nil
        if let todayText = todayText, !todayText.isEmpty,
           let todayTarget = todayTarget, let todayAction = todayAction {
            let btn = UIBarButtonItem(
                title: todayText,
                style: .plain,
                target: todayTarget,
                action: todayAction
            )
            items.append(btn)
            // AiForms オリジナル: Today と Done の間に 20pt の固定スペース
            let fixSpace = UIBarButtonItem(barButtonSystemItem: .fixedSpace, target: nil, action: nil)
            fixSpace.width = 20
            items.append(fixSpace)
            todayButton = btn
        }

        items.append(doneButton)
        toolbar.items = items

        return Built(
            toolbar: toolbar,
            cancelButton: cancelButton,
            doneButton: doneButton,
            todayButton: todayButton
        )
    }
}

#endif

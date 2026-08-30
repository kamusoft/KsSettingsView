// EmbeddedPickerHostField.swift
// KsSettingsViewUI
//
// NumberPicker / TimePicker / DatePicker (Wheels モード) 共通で利用する、
// 透明・no-caret な `UITextField` サブクラス。
//
// AiForms オリジナル `Native/iOS/Effects/NoCaretField.cs` 相当。
// `inputView` に `UIPickerView` / `UIDatePicker` を、`inputAccessoryView` に Toolbar を
// セットしたうえで `becomeFirstResponder()` を呼ぶことで、iOS がキーボード位置に
// Picker UI を「下からスライドアップ」表示してくれる仕組みを担う土台。
//
// caret / 選択ハイライト / コピペメニューはすべて抑止する（ユーザーに認知されない
// 透明な入力欄として振る舞う）。intrinsicContentSize は影響させない（accessory に
// ぶら下げず ContentView 全体に貼るため、サイズはレイアウト側で決める）。

#if canImport(UIKit)
import UIKit

/// 埋め込み型 Picker を `inputView` 経由で表示するための透明 `UITextField` サブクラス。
///
/// 直接 `addSubview` して `frame = ContentView.bounds` 相当で貼り付けて使う。
/// `becomeFirstResponder()` で iOS が `inputView` を下からスライドアップ表示する。
@MainActor
internal final class EmbeddedPickerHostField: UITextField {

    override init(frame: CGRect) {
        super.init(frame: frame)
        self.borderStyle = .none
        self.backgroundColor = .clear
        self.textColor = .clear
        self.tintColor = .clear
        // `isUserInteractionEnabled = false` にすると一部 iOS バージョンで
        // `becomeFirstResponder()` 自体が無視されることがあるため true 維持。
        // 直接タッチを拾わせない工夫は `hitTest(_:with:)` で行う。
    }

    /// 直接のタッチイベントを拾わせない。Cell のタップは KsSettingsViewController 経由で
    /// `tapHandler` → `becomeFirstResponder()` ルートで処理されるため、自身は hit test に
    /// 引っかからない方が望ましい。
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        return nil
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    // caret を消す（透明 / 不可視）
    override func caretRect(for position: UITextPosition) -> CGRect { .zero }

    // 選択ハイライトを消す
    override func selectionRects(for range: UITextRange) -> [UITextSelectionRect] { [] }

    // コピー / ペースト / 全選択 / 共有... 全部抑止
    override func canPerformAction(_ action: Selector, withSender sender: Any?) -> Bool { false }

    // intrinsicContentSize で外側に影響を与えない
    override var intrinsicContentSize: CGSize {
        return CGSize(width: UIView.noIntrinsicMetric, height: UIView.noIntrinsicMetric)
    }
}
#endif

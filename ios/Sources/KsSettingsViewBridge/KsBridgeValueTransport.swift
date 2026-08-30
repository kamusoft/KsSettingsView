// KsBridgeValueTransport.swift
// KsSettingsViewBridge
//
// interop 境界の値表現 (ISO 文字列・enum 序数・選択 index 配列) と Native 型の相互変換。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

/// interop 境界の値表現を Native 型へ解釈し、通知方向では Native 型を境界表現へ戻す。
///
/// 二値は Bool、数値と選択 index は Int、文字列は String をそのまま渡す。壁時計値である時刻と
/// 日付は、タイムゾーンを含まない固定書式の ISO-8601 文字列で運ぶ (maui/ADR-0012)。enum は
/// 序数の Int で運び、`nil` (欠落) は「未指定 → Native 既定」を意味する。
///
/// 解釈できない文字列・未知の序数は例外にせず、Native 既定または型の既定値へ倒す。生成側は
/// facade と Bridge の双方が固定書式で書くため、解釈失敗は契約違反であり `DEBUG` ビルドでのみ
/// 診断を出す。
internal enum KsBridgeValueTransport {

    /// 時刻の輸送書式 (壁時計値・culture 非依存)。
    internal static let timeFormat = "HH:mm"

    /// 日付の輸送書式 (壁時計値・culture 非依存)。
    internal static let dateFormat = "yyyy-MM-dd"

    // MARK: - 時刻 / 日付

    /// 輸送書式の時刻文字列を `Date` へ解釈する。解釈できない場合は 00:00 で構築する。
    /// - Parameter text: "HH:mm" 形式の時刻文字列
    static func time(from text: String) -> Date {
        if let parsed = parse(text, format: timeFormat) {
            return parsed
        }
        diagnose(kind: "時刻", text: text, format: timeFormat)
        // 解釈できない値は時刻の既定値 (00:00) へ倒す。書式自体は固定のため必ず解釈できる。
        return parse("00:00", format: timeFormat) ?? Date(timeIntervalSince1970: 0)
    }

    /// 輸送書式の日付文字列を `Date` へ解釈する。解釈できない場合は 1970-01-01 で構築する。
    /// - Parameter text: "yyyy-MM-dd" 形式の日付文字列
    static func date(from text: String) -> Date {
        if let parsed = parse(text, format: dateFormat) {
            return parsed
        }
        diagnose(kind: "日付", text: text, format: dateFormat)
        // 解釈できない値は日付の既定値 (1970-01-01) へ倒す。
        return parse("1970-01-01", format: dateFormat) ?? Date(timeIntervalSince1970: 0)
    }

    /// 未指定を許す日付文字列を `Date?` へ解釈する。
    ///
    /// `nil` は未指定をそのまま表し、解釈できない文字列も未指定 (`nil` = 型の既定値) へ倒す。
    /// - Parameter text: "yyyy-MM-dd" 形式の日付文字列 (未指定は `nil`)
    static func optionalDate(from text: String?) -> Date? {
        guard let text else { return nil }
        if let parsed = parse(text, format: dateFormat) {
            return parsed
        }
        diagnose(kind: "日付", text: text, format: dateFormat)
        return nil
    }

    /// `Date` の時刻成分を輸送書式の文字列へ変換する。
    /// - Parameter date: 変換対象
    static func timeText(from date: Date) -> String {
        return formatter(timeFormat).string(from: date)
    }

    /// `Date` の日付成分を輸送書式の文字列へ変換する。
    /// - Parameter date: 変換対象
    static func dateText(from date: Date) -> String {
        return formatter(dateFormat).string(from: date)
    }

    // MARK: - enum の序数

    /// keyboard 種別の序数を `UIKeyboardType` へ変換する。未知の序数は既定キーボードへ倒す。
    ///
    /// 序数は facade の正規化 enum に対応する:
    /// `0 = Default / 1 = Plain / 2 = Text / 3 = Chat / 4 = Url / 5 = Email / 6 = Numeric /
    /// 7 = Telephone`。iOS のキーボード種別に対応概念のない値 (Plain / Text / Chat) は
    /// `.default` へ写す。
    /// - Parameter ordinal: keyboard 種別の序数
    static func keyboardType(from ordinal: Int) -> UIKeyboardType {
        switch ordinal {
        case 4: return .URL
        case 5: return .emailAddress
        case 6: return .decimalPad
        case 7: return .phonePad
        default: return .default
        }
    }

    /// DatePicker の UI スタイル序数を `DatePickerUIStyle` へ変換する。
    ///
    /// 序数は `0 = Calendar / 1 = Wheels` (maui/ADR-0013)。`nil` と未知の序数は「未指定」を表す
    /// `nil` を返し、呼び出し側が Native 既定を適用する。
    /// - Parameter ordinal: UI スタイルの序数 (未指定は `nil`)
    static func datePickerUIStyle(from ordinal: NSNumber?) -> DatePickerUIStyle? {
        guard let ordinal else { return nil }
        switch ordinal.intValue {
        case 0: return .calendar
        case 1: return .wheels
        default: return nil
        }
    }

    /// テキスト配置の序数を `CellTitleAlignment` へ変換する。
    ///
    /// 序数は `0 = Start / 1 = Center / 2 = End`。`nil` と未知の序数は `fallback` を返す。
    /// - Parameters:
    ///   - ordinal: 配置の序数 (未指定は `nil`)
    ///   - fallback: 未指定・未知のときに使う配置 (Native 既定)
    static func titleAlignment(from ordinal: NSNumber?, fallback: CellTitleAlignment) -> CellTitleAlignment {
        guard let ordinal else { return fallback }
        switch ordinal.intValue {
        case 0: return .start
        case 1: return .center
        case 2: return .end
        default: return fallback
        }
    }

    /// Picker の選択モード序数を `PickerSelectionMode` へ変換する。
    ///
    /// 序数は `0 = Single / 1 = Multiple`。未知の序数は単一選択へ倒す。
    /// - Parameter ordinal: 選択モードの序数
    static func selectionMode(from ordinal: Int) -> PickerSelectionMode {
        return ordinal == 1 ? .multiple : .single
    }

    // MARK: - Picker の候補

    /// 輸送 DTO の候補列を Native の候補列へ変換する。
    ///
    /// 表示射影は上位層で適用済みのため、ここでは主表示と副表示をそのまま写す。副表示の空文字列を
    /// 「なし」へ揃えるのは `PickerItem` 側の責務。
    /// - Parameter items: 輸送 DTO の候補列
    static func pickerItems(from items: [KsBridgePickerItem]) -> [PickerItem] {
        return items.map { PickerItem(text: $0.text, subText: $0.subText) }
    }

    // MARK: - 選択 index

    /// 複数選択 index の配列を Native の集合へ変換する。
    ///
    /// 重複は集合化で除去される。範囲外の index は正規化せず透過する (Native の「モデル値を
    /// 正規化しない」契約に従う)。
    /// - Parameter indices: 選択 index の配列
    static func indexSet(from indices: [Int]) -> Set<Int> {
        return Set(indices)
    }

    /// Native の選択 index 集合を輸送表現 (昇順・重複なしの配列) へ変換する。
    /// - Parameter indices: 選択 index の集合
    static func indexList(from indices: Set<Int>) -> [Int] {
        return indices.sorted()
    }

    // MARK: - 内部

    /// 輸送書式の文字列を `Date` へ解釈する。書式と 1 文字でも異なる入力は解釈失敗として `nil`。
    ///
    /// `DateFormatter` の解釈は区切り文字違いや桁数不足を吸収してしまう ("2026/08/10" を
    /// "yyyy-MM-dd" として受け入れる) ため、解釈結果を同じ書式へ戻して入力と一致することまで
    /// 確かめる。輸送書式を厳密に扱うことで、書式から外れた入力の結果が platform をまたいで
    /// 同一 (一律に既定値) になる。
    private static func parse(_ text: String, format: String) -> Date? {
        let formatter = formatter(format)
        guard let parsed = formatter.date(from: text), formatter.string(from: parsed) == text else {
            return nil
        }
        return parsed
    }

    /// 固定書式・culture 非依存の `DateFormatter` を作る。
    ///
    /// 壁時計値として解釈するため、タイムゾーンは端末の現在設定のままにする (Native の
    /// `UIDatePicker` と `Calendar.current` も同じ基準で動く)。
    private static func formatter(_ format: String) -> DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = format
        return formatter
    }

    /// 解釈失敗を `DEBUG` ビルドでのみ診断出力する。
    private static func diagnose(kind: String, text: String, format: String) {
        #if DEBUG
        print("KsSettingsViewBridge: \(kind)文字列 '\(text)' が輸送書式 '\(format)' に一致しないため既定値で構築します")
        #endif
    }
}
#endif

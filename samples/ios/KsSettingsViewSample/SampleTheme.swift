// SampleTheme.swift
// KsSettingsViewSample
//
// Sample アプリ共用の MAUI 互換 Theme 定義。
//
// MAUI 原典 Sample（AiForms.Maui.SettingsView/Sample/Views/MainPage.xaml）に限りなく
// 一致する見た目を目指した色定数と Theme をここに一元化し、基本 Cell 7 種デモ /
// 入力 Cell 5 種デモの双方が同じ定義を参照する（色値の二重管理を作らない）。
//
// 対応する Android 側定義:
// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleTheme.kt

import UIKit
import KsSettingsViewUI

/// Sample 共用の MAUI 互換 Theme と、その構成色。
enum SampleTheme {

    // MARK: - MAUI 互換色定数（UIColor 直接構築）

    /// PaleBackColorPrimary 相当（#F2EFE6）。SettingsView 全体の背景色。
    static let mauiViewBackground = UIColor(red: 0xF2/255.0, green: 0xEF/255.0, blue: 0xE6/255.0, alpha: 1.0)
    /// Cell 背景（白）
    static let mauiCellBackground = UIColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 1.0)
    /// DisabledColor 相当（#E6DAB9）。セパレータ色。
    static let mauiSeparator = UIColor(red: 0xE6/255.0, green: 0xDA/255.0, blue: 0xB9/255.0, alpha: 1.0)
    /// AccentColor の半透明（#50FFBF00 = α=0x50/0xFF）。タッチ時の塗り色。
    static let mauiSelected = UIColor(red: 0xFF/255.0, green: 0xBF/255.0, blue: 0x00/255.0, alpha: 0x50/255.0)
    /// AccentColor（#FFBF00）。スイッチ ON / チェックボックス accent 等。
    static let mauiAccent = UIColor(red: 0xFF/255.0, green: 0xBF/255.0, blue: 0x00/255.0, alpha: 1.0)
    /// TitleTextColor（#CC9900）。ヘッダ文字色。
    static let mauiHeaderText = UIColor(red: 0xCC/255.0, green: 0x99/255.0, blue: 0x00/255.0, alpha: 1.0)
    /// PaleTextColor（#999999）。フッタ文字色。
    static let mauiFooterText = UIColor(red: 0x99/255.0, green: 0x99/255.0, blue: 0x99/255.0, alpha: 1.0)
    /// 無効時テキスト色（#999999）
    static let mauiDisabledText = UIColor(red: 0x99/255.0, green: 0x99/255.0, blue: 0x99/255.0, alpha: 1.0)
    /// DeepTextColor（#555555）。Cell タイトルの文字色。
    static let mauiDeepText = UIColor(red: 0x55/255.0, green: 0x55/255.0, blue: 0x55/255.0, alpha: 1.0)

    // MARK: - dark プリセットの色定数

    // light 側の各色ロールを、色相を保ったまま明度反転させた暖色ダーク。accent（#FFBF00）と
    // その半透明（selectedColor）は light と共有する。暖色を保ち、Theme に dark 値を渡したときの
    // 描画がライブラリ既定色のダークと見分けられるようにする。

    /// dark の下地色（#1B1915）。SettingsView 全体と Header / Footer の背景。
    static let mauiDarkViewBackground = UIColor(red: 0x1B/255.0, green: 0x19/255.0, blue: 0x15/255.0, alpha: 1.0)
    /// dark の Cell 背景（#2A2620）。
    static let mauiDarkCellBackground = UIColor(red: 0x2A/255.0, green: 0x26/255.0, blue: 0x20/255.0, alpha: 1.0)
    /// dark のセパレータ色（#4A3F28）。
    static let mauiDarkSeparator = UIColor(red: 0x4A/255.0, green: 0x3F/255.0, blue: 0x28/255.0, alpha: 1.0)
    /// dark のヘッダ文字色（#E0B040）。ButtonCell の `CellStyle(titleColor:)` にも使う。
    static let mauiDarkHeaderText = UIColor(red: 0xE0/255.0, green: 0xB0/255.0, blue: 0x40/255.0, alpha: 1.0)
    /// dark のフッタ文字色（#9A948A）。
    static let mauiDarkFooterText = UIColor(red: 0x9A/255.0, green: 0x94/255.0, blue: 0x8A/255.0, alpha: 1.0)
    /// dark の無効時テキスト色（#7A756C）。
    static let mauiDarkDisabledText = UIColor(red: 0x7A/255.0, green: 0x75/255.0, blue: 0x6C/255.0, alpha: 1.0)
    /// dark の Cell タイトル文字色（#E6E1D6）。
    static let mauiDarkDeepText = UIColor(red: 0xE6/255.0, green: 0xE1/255.0, blue: 0xD6/255.0, alpha: 1.0)
    /// dark の valueText 文字色（#B8B2A6）。
    static let mauiDarkValueText = UIColor(red: 0xB8/255.0, green: 0xB2/255.0, blue: 0xA6/255.0, alpha: 1.0)
    /// dark の description 文字色（#9A948A）。
    static let mauiDarkDescriptionText = UIColor(red: 0x9A/255.0, green: 0x94/255.0, blue: 0x8A/255.0, alpha: 1.0)

    // MARK: - 共通フィールド統合デモ用の共有アクセントパレット

    // 共通フィールド統合デモが Cell に明示指定する色は、各 Cell に渡すパラメータを
    // プラットフォーム間で一致させるため、同一の RGBA でなければならない (cross/ADR-0016)。
    // そこで iOS の `UIColor.systemXxx` を light appearance の実測値で固定し、Android 側
    // SampleTheme.kt にも同じ値を定義して両 platform から参照する。
    //
    // 固定色であるため、これらは semantic color の dark mode 追随を持たない。パリティを
    // 優先して受け入れているトレードオフ。
    // 実測環境: iOS 26.5 シミュレータ / `UITraitCollection(userInterfaceStyle: .light)` で解決。

    /// RadioCell「ライト」の accentColor（`UIColor.systemOrange` 相当 #FF8D28）。
    static let demoAccentOrange = UIColor(red: 0xFF/255.0, green: 0x8D/255.0, blue: 0x28/255.0, alpha: 1.0)
    /// RadioCell「ダーク」の accentColor（`UIColor.systemPurple` 相当 #CB30E0）。
    static let demoAccentPurple = UIColor(red: 0xCB/255.0, green: 0x30/255.0, blue: 0xE0/255.0, alpha: 1.0)
    /// RadioCell「自動」の accentColor（`UIColor.systemTeal` 相当 #00C3D0）。
    static let demoAccentTeal = UIColor(red: 0x00/255.0, green: 0xC3/255.0, blue: 0xD0/255.0, alpha: 1.0)
    /// SimpleCheckCell「通知 1」の accentColor（`UIColor.systemPink` 相当 #FF2D55）。
    static let demoAccentPink = UIColor(red: 0xFF/255.0, green: 0x2D/255.0, blue: 0x55/255.0, alpha: 1.0)
    /// SimpleCheckCell「通知 2」の accentColor（`UIColor.systemGreen` 相当 #34C759）。
    static let demoAccentGreen = UIColor(red: 0x34/255.0, green: 0xC7/255.0, blue: 0x59/255.0, alpha: 1.0)
    /// ButtonCell「登録」の titleColor（`UIColor.systemBlue` 相当 #0088FF）。
    static let demoTitleBlue = UIColor(red: 0x00/255.0, green: 0x88/255.0, blue: 0xFF/255.0, alpha: 1.0)

    /// アクセント 6 色の循環パレット。
    ///
    /// CustomCell デモの「スクロール耐性（ダミー #01–#40）」が行ごとに色を循環させるために使う。
    /// 並び順は Android 側 `SampleTheme.demoAccentPalette` と一致させること。
    static let demoAccentPalette: [UIColor] = [
        demoAccentOrange,
        demoAccentPurple,
        demoAccentTeal,
        demoAccentPink,
        demoAccentGreen,
        demoTitleBlue
    ]

    // MARK: - 入力 Cell デモ用の追加色

    // 入力 Cell デモが `EntryCell.placeholderColor` に明示指定する色。
    // 他のデモ色と同じく、iOS / Android / MAUI で同一 RGBA を渡すためここに一元化する
    // (cross/ADR-0016)。

    /// placeholder 色デモ行の `placeholderColor`（#D6885A）。
    static let demoPlaceholderOrange = UIColor(red: 0xD6/255.0, green: 0x88/255.0, blue: 0x5A/255.0, alpha: 1.0)

    // MARK: - CustomCell デモ用の追加色

    // CustomCell デモの content（利用者が書く任意 View）が使う色。
    // Cell の内装は利用者責務のため Theme には載らないが、Sample は iOS / Android で
    // 同一 RGBA を渡す必要があるため SampleTheme に一元化する。

    /// ピル（行タップカウンタ / ダミー行の連番）の背景（#FAF3D9）。
    static let demoPillBackground = UIColor(red: 0xFA/255.0, green: 0xF3/255.0, blue: 0xD9/255.0, alpha: 1.0)
    /// 展開本文ブロックの背景（#FAF7EE）。
    static let demoExpandBackground = UIColor(red: 0xFA/255.0, green: 0xF7/255.0, blue: 0xEE/255.0, alpha: 1.0)
    /// 展開本文の文字色（#777777）。
    static let demoExpandText = UIColor(red: 0x77/255.0, green: 0x77/255.0, blue: 0x77/255.0, alpha: 1.0)

    // MARK: - Section 装飾デモ用の追加色

    /// Section 装飾デモの `sectionBorderColor` に渡す灰色（#C7C7CC）。
    static let demoSectionBorder = UIColor(red: 0xC7/255.0, green: 0xC7/255.0, blue: 0xCC/255.0, alpha: 1.0)
    /// バッジ型アイコンの地色（オレンジ #FF9500）。
    static let demoIconOrange = UIColor(red: 0xFF/255.0, green: 0x95/255.0, blue: 0x00/255.0, alpha: 1.0)
    /// バッジ型アイコンの地色（青 #007AFF）。
    static let demoIconBlue = UIColor(red: 0x00/255.0, green: 0x7A/255.0, blue: 0xFF/255.0, alpha: 1.0)
    /// バッジ型アイコンの地色（明るい青 #0A84FF）。
    static let demoIconVividBlue = UIColor(red: 0x0A/255.0, green: 0x84/255.0, blue: 0xFF/255.0, alpha: 1.0)

    // MARK: - Theme

    /// MAUI 互換 Theme（light）。UI 層 `Theme` を UIColor 直接構築で渡す。
    static let mauiLight = Theme(
        separatorColor: mauiSeparator,
        backgroundColor: mauiViewBackground,
        cellBackgroundColor: mauiCellBackground,
        selectedColor: mauiSelected,
        cellAccentColor: mauiAccent,
        disabledTextColor: mauiDisabledText,
        rowHeight: -1,
        hasUnevenRows: true,
        headerTextColor: mauiHeaderText,
        headerBackgroundColor: mauiViewBackground,
        footerTextColor: mauiFooterText,
        footerBackgroundColor: mauiViewBackground,
        cellTitleColor: mauiDeepText
    )

    /// MAUI 互換 Theme（dark）。
    ///
    /// 色ロールの構成は `mauiLight` と同じで、description と valueText の色だけを追加で明示する。
    /// 未指定のままだと、これらの既定色が暗い下地に追随しない。
    static let mauiDark = Theme(
        separatorColor: mauiDarkSeparator,
        backgroundColor: mauiDarkViewBackground,
        cellBackgroundColor: mauiDarkCellBackground,
        selectedColor: mauiSelected,
        cellAccentColor: mauiAccent,
        disabledTextColor: mauiDarkDisabledText,
        rowHeight: -1,
        hasUnevenRows: true,
        headerTextColor: mauiDarkHeaderText,
        headerBackgroundColor: mauiDarkViewBackground,
        footerTextColor: mauiDarkFooterText,
        footerBackgroundColor: mauiDarkViewBackground,
        cellTitleColor: mauiDarkDeepText,
        cellValueTextColor: mauiDarkValueText,
        cellDescriptionColor: mauiDarkDescriptionText
    )

    /// 実効外観に対応する MAUI 互換 Theme。
    ///
    /// - Parameter dark: 実効外観がダークなら `true`
    static func maui(dark: Bool) -> Theme { dark ? mauiDark : mauiLight }

    /// 実効外観に対応する ButtonCell の `CellStyle(titleColor:)` 用の色。
    ///
    /// - Parameter dark: 実効外観がダークなら `true`
    static func mauiTitleText(dark: Bool) -> UIColor { dark ? mauiDarkHeaderText : mauiHeaderText }

    /// Section 装飾デモ用 Theme。
    ///
    /// 下地（`backgroundColor`）と Header / Footer の背景に実効外観に応じた下地色を敷き、
    /// 箱（`cellBackgroundColor`）・separator・Header / Footer 文字色はライブラリ既定のまま残す。
    /// アイコンはバッジ型（`SampleIconBadge`）に合わせたサイズ・角丸を指定する。
    /// Section 装飾の 4 属性だけを引数で差し替えてプリセットを作る。
    ///
    /// - Parameters:
    ///   - dark: 実効外観がダークなら `true`
    ///   - sectionMargin: Section 単位の外側余白（`nil` で style ごとの既定）
    ///   - sectionCornerRadius: 箱の角丸半径（`nil` で style ごとの既定）
    ///   - sectionBorderWidth: 箱のボーダー幅（`nil` で実効 0）
    ///   - sectionBorderColor: 箱のボーダー色（`nil` で実効透明）
    static func sectionDecorationDemo(
        dark: Bool,
        sectionMargin: NSDirectionalEdgeInsets? = nil,
        sectionCornerRadius: CGFloat? = nil,
        sectionBorderWidth: CGFloat? = nil,
        sectionBorderColor: UIColor? = nil
    ) -> Theme {
        let viewBackground = dark ? mauiDarkViewBackground : mauiViewBackground
        return Theme(
            backgroundColor: viewBackground,
            cellAccentColor: demoAccentGreen,
            headerBackgroundColor: viewBackground,
            footerBackgroundColor: viewBackground,
            cellIconSize: SampleIconBadge.size,
            cellIconRadius: SampleIconBadge.cornerRadius,
            sectionMargin: sectionMargin,
            sectionCornerRadius: sectionCornerRadius,
            sectionBorderWidth: sectionBorderWidth,
            sectionBorderColor: sectionBorderColor
        )
    }
}

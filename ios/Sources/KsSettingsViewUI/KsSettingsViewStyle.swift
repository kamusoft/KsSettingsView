// KsSettingsViewStyle.swift
// KsSettingsViewUI
//
// 設定画面の見た目スタイルを表す enum。`KsSettingsViewController` のレイアウト構築で
// Section の装飾（余白・角丸・ボーダー）と罫線の出し分けを決める。

import Foundation

/// 設定画面の見た目スタイル。
///
/// - `classic`: `AiForms.Maui.SettingsView` 互換のフラットな見た目。Section 境界は全幅の罫線で示す
/// - `modern`: Section の Cell 行を角丸の箱にまとめる見た目。箱の余白・角丸・ボーダーは
///   `Theme` の Section 装飾 4 属性で制御でき、ライブラリが自前で描く（ios/ADR-0003）
public enum KsSettingsViewStyle: Hashable, Sendable {
    case classic
    case modern
}

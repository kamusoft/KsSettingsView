// KsBridgeIdentifier.swift
// KsSettingsViewBridge
//
// interop 境界で扱う ID 文字列と Native の `UUID` を相互変換するユーティリティ。

import Foundation

/// interop 境界の ID (canonical UUID 文字列) と Native の `UUID` を橋渡しする。
///
/// Section / Cell の ID は Bridge が採番して呼び出し側へ返す (maui/ADR-0005)。呼び出し側は
/// 返された文字列だけを更新 API へ渡し、Bridge はそれを `UUID` へ復元して Store 操作に用いる。
/// Bridge が採番していない文字列 (canonical UUID として解釈できない値) は復元に失敗し、
/// Cell / Section 操作では no-op になる。
internal enum KsBridgeIdentifier {

    /// 新しい ID を採番する。
    static func make() -> UUID {
        return UUID()
    }

    /// `UUID` を interop 境界で受け渡す canonical UUID 文字列へ変換する。
    static func string(from uuid: UUID) -> String {
        return uuid.uuidString
    }

    /// interop 境界の ID 文字列を `UUID` へ復元する。canonical UUID として解釈できない場合は `nil`。
    static func uuid(from string: String?) -> UUID? {
        guard let string else { return nil }
        return UUID(uuidString: string)
    }
}

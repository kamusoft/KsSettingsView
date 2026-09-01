package jp.kamusoft.kssettingsview.bridge

import java.util.UUID

/**
 * interop 境界で扱う ID 文字列を検証・採番するユーティリティ。
 *
 * Section / Cell の ID は Bridge が採番して呼び出し側へ返す（maui/ADR-0005）。呼び出し側は
 * 返された文字列だけを更新 API へ渡し、Bridge はそれをそのまま Store 操作の ID として用いる。
 * Bridge が採番していない文字列（canonical UUID として解釈できない値）は検証に失敗し、
 * Cell / Section 操作では no-op になる。
 *
 * canonical UUID の判定を iOS 側（`UUID(uuidString:)`）と同じ厳密さに揃えるため、
 * 8-4-4-4-12 の 16 進表記のみを受け付ける。`java.util.UUID.fromString` は短縮形も
 * 受理してしまうため、直接は使わない。
 */
internal object KsBridgeIdentifier {

    /** canonical UUID 文字列（8-4-4-4-12 の 16 進表記）の形式。 */
    private val CANONICAL_UUID = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    /** 新しい ID を採番する。 */
    fun make(): String = UUID.randomUUID().toString()

    /**
     * interop 境界の ID 文字列を検証する。
     *
     * @return canonical UUID 文字列ならその文字列。解釈できない場合は `null`
     */
    fun canonical(value: String?): String? = value?.takeIf { CANONICAL_UUID.matches(it) }
}

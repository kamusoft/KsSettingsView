package jp.kamusoft.kssettingsview.compose

/**
 * DSL（`KsSettingsView { ... }`）における Section / Cell の同一性判定戦略を実装する
 * ユーティリティ。再評価をまたいで宣言ツリーの同一性を保つ原則は core/ADR-0008 を参照。
 *
 * # ID 採番方針
 *
 * Section / Cell の `id` は `String` 型（Core 層既存仕様）であり、DSL では
 * `forEach key` / `.sectionID(...)` / `.cellID(...)` / `(rootIdx, headerText)` 等の
 * ヒントから **安定した String ID** を導出する。Compose Recomposition のたびに同じ
 * ヒントを与えれば同じ ID を返す決定性が要件。
 *
 * Kotlin の `Any.hashCode()` は per-process でランダム化されないため、文字列化（toString）と
 * SHA-1 風の安定ハッシュ（FNV-1a を 16 バイトに拡張）で衝突可能性を下げた決定的 ID を
 * 生成する。
 */
internal sealed class DSLIdentityHint {

    /** ForEach 配下：`key` lambda 戻り値 */
    data class ForEach(val key: Any) : DSLIdentityHint()

    /** 明示指定 modifier（`.sectionID(...)` / `.cellID(...)`） */
    data class Explicit(val id: Any) : DSLIdentityHint()

    /** ヘッダ文字列ベースの安定化（Section 限定） */
    data class HeaderText(val rootIdx: Int, val text: String) : DSLIdentityHint()

    /** 位置 + Cell 型ベースの安定化（Cell 限定） */
    data class Positional(
        val sectionId: String,
        val indexInSection: Int,
        val cellType: String,
    ) : DSLIdentityHint()

    /** ルート位置ベースのフォールバック（Section 限定） */
    data class RootPosition(val rootIdx: Int) : DSLIdentityHint()
}

/**
 * `DSLIdentityHint` を決定的な `String` ID に変換するユーティリティ。
 */
internal object DSLIdentityId {

    /** 名前空間（KsSettingsView DSL 用に固定） */
    private const val NAMESPACE = "jp.kamusoft.kssettingsview.compose.dsl"

    /**
     * ヒントから決定的な String ID を生成する。
     *
     * @param hint 採用するヒント
     * @return ヒントに対応する安定 ID（hex 文字列 32 桁）
     */
    fun id(from: DSLIdentityHint): String {
        val parts = StringBuilder()
        parts.append(NAMESPACE)
        parts.append('|')
        when (from) {
            is DSLIdentityHint.ForEach -> {
                parts.append("forEach|")
                parts.append(stableString(from.key))
            }
            is DSLIdentityHint.Explicit -> {
                parts.append("explicit|")
                parts.append(stableString(from.id))
            }
            is DSLIdentityHint.HeaderText -> {
                parts.append("headerText|")
                parts.append(from.rootIdx)
                parts.append('|')
                parts.append(from.text)
            }
            is DSLIdentityHint.Positional -> {
                parts.append("positional|")
                parts.append(from.sectionId)
                parts.append('|')
                parts.append(from.indexInSection)
                parts.append('|')
                parts.append(from.cellType)
            }
            is DSLIdentityHint.RootPosition -> {
                parts.append("rootPosition|")
                parts.append(from.rootIdx)
            }
        }
        return stableHash(parts.toString())
    }

    /**
     * `Any` を決定的な文字列表現に変換する。
     * 頻出型（String / Int / Long / 等）は型自体を含めることで `123: Int` と `"123": String` を
     * 衝突させないようにする。
     */
    private fun stableString(value: Any): String {
        return when (value) {
            is String -> "S:" + value
            is Int -> "I:" + value
            is Long -> "L:" + value
            is Char -> "C:" + value
            else -> "X:${value::class.qualifiedName ?: "?"}|${value.toString()}"
        }
    }

    /**
     * 文字列から決定的な 16 バイト hex（32 文字）を計算する。
     * FNV-1a 64bit を 2 本並列で回し、128bit にする。
     */
    private fun stableHash(input: String): String {
        var s0 = 0xcbf29ce484222325UL
        var s1 = 0x84222325cbf29ce4UL
        val prime = 0x100000001b3UL
        for (b in input.toByteArray(Charsets.UTF_8)) {
            val ub = (b.toInt() and 0xff).toULong()
            s0 = s0 xor ub
            s0 = s0.times(prime)
            s1 = (s1 + ub).times(prime)
        }
        val sb = StringBuilder(32)
        for (shift in 56 downTo 0 step 8) {
            val byte = ((s0 shr shift) and 0xffUL).toInt()
            sb.append("%02x".format(byte))
        }
        for (shift in 56 downTo 0 step 8) {
            val byte = ((s1 shr shift) and 0xffUL).toInt()
            sb.append("%02x".format(byte))
        }
        return sb.toString()
    }
}

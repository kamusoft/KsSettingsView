package jp.kamusoft.kssettingsview.ui

import androidx.annotation.DrawableRes

/**
 * Cell のアイコン表現に用いる sealed 型（UI 層所属）。
 *
 * プラットフォーム固有派生として以下を持つ：
 *
 * - [Resource]: Android リソース ID（`@DrawableRes`）を保持する派生（主軸）
 * - [Drawable]: 任意の [android.graphics.drawable.Drawable] を保持する派生
 * - [SystemName]: iOS の SF Symbols 名（API 対称性のため残置、Android UI 層は解決不可で無視）
 *
 * UI 層は派生に応じて以下のように解決する：
 *
 * 1. `null` → アイコン領域を `View.GONE`
 * 2. [Drawable] → `setImageDrawable(it.drawable)`
 * 3. [Resource] → `ContextCompat.getDrawable(context, it.resId)` を取得して `setImageDrawable`
 * 4. [SystemName] → 解決不可。アイコン領域を `View.GONE` でフォールバック
 *
 * # 等価性
 *
 * - [Resource] / [SystemName]: 値同一性（`data class` の自動 `equals` / `hashCode`）
 * - [Drawable]: 参照同一性（[android.graphics.drawable.Drawable] は値型ではないため）
 *
 * Android の `Drawable` / リソース ID を直接扱うため、Core ではなく UI 層に属する（core/ADR-0009）。
 */
sealed interface KsImage {

    /**
     * Android リソース ID（`@DrawableRes`）を保持する派生（主軸）。
     *
     * UI 層は `ContextCompat.getDrawable(context, resId)` で解決し、`ImageView.setImageDrawable` に
     * 渡す。`R8` / ProGuard とも整合し、コンパイル時のリソース型検査が効く。
     *
     * @property resId `@DrawableRes` 注釈付きのリソース ID
     */
    data class Resource(@DrawableRes val resId: Int) : KsImage

    /**
     * 任意の [android.graphics.drawable.Drawable] を保持する派生。
     *
     * `VectorDrawableCompat` や動的生成 Drawable など、リソース ID では表現できないアイコンを
     * 渡したい場合に利用する。
     *
     * # 等価性
     *
     * `Drawable` は値型ではないため、`equals` / `hashCode` は参照同一性で実装する。
     * 同一 [drawable] インスタンスを保持する 2 つの `Drawable` ケースのみ等価とみなす。
     *
     * @property drawable 任意の Drawable インスタンス
     */
    class Drawable(val drawable: android.graphics.drawable.Drawable) : KsImage {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Drawable) return false
            // 参照同一性で比較（Drawable は値型ではない）
            return drawable === other.drawable
        }

        override fun hashCode(): Int = System.identityHashCode(drawable)

        override fun toString(): String = "KsImage.Drawable(@${System.identityHashCode(drawable)})"
    }

    /**
     * iOS の SF Symbols 名を保持する派生。Android UI 層は解決不可とし、`View.GONE` でフォールバック。
     *
     * iOS との API 対称性のために残置している。クロスプラットフォーム共通の DSL を書くときに
     * `KsImage.SystemName("bell")` を Android でも安全に受け取れるようにするための派生。
     *
     * @property name SF Symbols 名（Android では未解決）
     */
    data class SystemName(val name: String) : KsImage
}

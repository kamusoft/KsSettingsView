package jp.kamusoft.kssettingsview.core

import androidx.compose.runtime.Composable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * KsAnyView の仕様検証。
 *
 * 検証する性質:
 *   - Compose / AndroidView の二択 backing で構築でき、中身を取り出せる
 *   - 差分検出に参加しない（`equals` / `hashCode` を独自実装せず、
 *     `Any` デフォルトの参照同一性に従う）
 */
class KsAnyViewTest {

    // -------- 構築 --------

    @Test
    @DisplayName("Compose サブタイプで構築できる")
    fun build_compose_subtype() {
        val composable: @Composable () -> Unit = {}
        val anyView: KsAnyView = KsAnyView.Compose(composable)

        assertTrue(anyView is KsAnyView.Compose)
        assertSame(composable, (anyView as KsAnyView.Compose).content)
    }

    @Test
    @DisplayName("AndroidView サブタイプで構築できる")
    fun build_android_view_subtype() {
        // Context や View はモックが要らない範囲で参照同一性のみ確認する
        val factory: (android.content.Context) -> android.view.View = { ctx ->
            android.view.View(ctx)
        }
        val anyView: KsAnyView = KsAnyView.AndroidView(factory)

        assertTrue(anyView is KsAnyView.AndroidView)
        assertSame(factory, (anyView as KsAnyView.AndroidView).factory)
    }

    // -------- equals / hashCode が Any デフォルト（参照同一性）であること --------

    @Test
    @DisplayName("equals は参照同一性で判定される（独自実装されていない）")
    fun compose_equals_is_reference_identity() {
        val composable: @Composable () -> Unit = {}
        val a = KsAnyView.Compose(composable)
        val b = KsAnyView.Compose(composable)

        // 同じ Composable ラムダを保持していても、KsAnyView.Compose のインスタンスは
        // 別々であれば等しくない（参照同一性に従う）
        assertNotEquals(a, b)

        // 自分自身とは等しい
        assertEquals(a, a)
    }

    @Test
    @DisplayName("Compose クラスは equals / hashCode を override していない（Any デフォルト）")
    fun compose_does_not_override_equals_or_hashcode() {
        // 反射により declaredMethods（自クラスで宣言されたメソッドのみ）を確認することで、
        // identityHashCode の理論的衝突に依存しない安定したアサーションを行う。
        val composeMethods = KsAnyView.Compose::class.java.declaredMethods
        val overridden = composeMethods.map { it.name }.toSet()
        assertTrue("equals" !in overridden) { "Compose は equals を override してはならない" }
        assertTrue("hashCode" !in overridden) { "Compose は hashCode を override してはならない" }
    }

    @Test
    @DisplayName("AndroidView も equals は参照同一性で判定される")
    fun android_view_equals_is_reference_identity() {
        val factory: (android.content.Context) -> android.view.View = { ctx ->
            android.view.View(ctx)
        }
        val a = KsAnyView.AndroidView(factory)
        val b = KsAnyView.AndroidView(factory)

        // 同じ factory を保持していても、別インスタンスなら等しくない
        assertNotEquals(a, b)
        assertEquals(a, a)
    }

    @Test
    @DisplayName("AndroidView クラスは equals / hashCode を override していない（Any デフォルト）")
    fun android_view_does_not_override_equals_or_hashcode() {
        // Compose 側と同様に、反射で declaredMethods を確認して
        // 「Any のデフォルト挙動に従う」ことを安定的に保証する。
        val androidViewMethods = KsAnyView.AndroidView::class.java.declaredMethods
        val overridden = androidViewMethods.map { it.name }.toSet()
        assertTrue("equals" !in overridden) { "AndroidView は equals を override してはならない" }
        assertTrue("hashCode" !in overridden) { "AndroidView は hashCode を override してはならない" }
    }
}

package jp.kamusoft.kssettingsview.ui

import androidx.compose.runtime.Composable
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * 利用者定義の [content] 値と、その値から Compose コンテンツを組み立てる [builder] を保持する Cell。
 *
 * 行は accessory（[showArrow] の Disclosure Indicator）領域を除いた全域を [builder] の出力が占有する
 * （full-bleed）。共通行レイアウト（title / description / icon / hintText / accessory slot）は持たず、
 * 中身の構成は利用者の領分になる（core/ADR-0022）。
 *
 * # 等価性
 *
 * 等価性判定には [id] / [style] / [content] / [showArrow] / [isEnabled] / [isVisible] のみが参加し、
 * [builder] / [onTap] の関数値は除外する（core/ADR-0014）。宣言 DSL は再評価のたびに新しい
 * クロージャを生成するため、関数値を等価性に含めると内容不変でも毎回「変更あり」と判定されて
 * 再バインドが暴発する。除外できないフィールドを持つ `data class` ではなく手動 `equals` /
 * `hashCode` を実装するのはこのためである。
 *
 * 逆に「表示に効く値」は等価性へ参加させる。これらが変わったのに再バインドされないと、
 * 画面とモデルが乖離したままになる。
 *
 * # 利用者への契約
 *
 * - [content] は値等価（`equals` / `hashCode`）を正しく実装した non-null の型であること
 *   （型制約 `Content : Any` で non-null を強制する）。等価性が壊れていると再バインドが
 *   過剰または過少になる
 * - [builder] / [onTap] は同一 [id] ・同一 [content] の間で意味的に安定であること。
 *   見た目や動作を変える値はクロージャのキャプチャではなく [content] に含める
 *   （関数値だけを差し替えても再バインドは起きない）
 *
 * アイコン領域を持たないため [DSLIconModifiableCell] には準拠しない。
 *
 * @param Content 利用者定義の content 値の型
 * @property id 一意 ID（既定で `custom-<random UUID>` を自動採番）
 * @property style Cell 個別スタイル。行レベルの項目（背景色・cellHeight）だけが効き、
 *   テキスト色・フォント等のコンテンツ内装項目は [builder] の出力に影響しない
 * @property content 描画の元になる値
 * @property showArrow `true` で行の trailing に Disclosure Indicator を表示する（既定 `false`）
 * @property onTap 行タップ時のコールバック（既定 `null` = 行タップ動作を持たない）
 * @property isEnabled `false` で行タップと content 内部の操作（ポインタ・accessibility action・
 *   フォーカス）を抑止する（既定 `true`）。あわせて content を淡色化するが、これは既定の振る舞い
 *   であり、無効時の描き分けを content 側で追加するのは利用者の自由
 * @property isVisible `false` で visible projection から除外される（既定 `true`）
 * @property builder [content] から Compose コンテンツを組み立てる
 */
class CustomCell<Content : Any>(
    override val id: String = "custom-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val content: Content,
    val showArrow: Boolean = false,
    val onTap: (() -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
    val builder: @Composable (Content) -> Unit,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, VisibilityAware {

    /**
     * 型パラメータを消去した描画エントリポイント（core/ADR-0016）。
     *
     * ViewHolder は `CustomCell<*>`（star projection）で Cell を受け取るため、型パラメータ越しに
     * `builder(content)` を呼ぶコードは型安全にコンパイルできない。構築時点で [content] と
     * [builder] を型付きのまま閉じ込めたこのクロージャを描画側の唯一の入口とすることで、
     * 描画側にキャストを持ち込まずに済ませる。
     */
    internal val composeContent: @Composable () -> Unit = { builder(content) }

    override fun withDSLId(newId: String): Cell = copyWith(id = newId)

    override fun withDSLStyle(newStyle: CellStyle): Cell = copyWith(style = newStyle)

    /** [id] / [style] だけを差し替えた自身の複製を返す（DSL modifier 経路用）。 */
    private fun copyWith(
        id: String = this.id,
        style: CellStyle = this.style,
    ): CustomCell<Content> = CustomCell(
        id = id,
        style = style,
        content = content,
        showArrow = showArrow,
        onTap = onTap,
        isEnabled = isEnabled,
        isVisible = isVisible,
        builder = builder,
    )

    // builder / onTap を除外した equals / hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomCell<*>) return false
        return id == other.id &&
            style == other.style &&
            content == other.content &&
            showArrow == other.showArrow &&
            isEnabled == other.isEnabled &&
            isVisible == other.isVisible
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + showArrow.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }

    override fun toString(): String =
        "CustomCell(id=$id, content=$content, showArrow=$showArrow, " +
            "isEnabled=$isEnabled, isVisible=$isVisible)"
}

/**
 * content を持たない静的な [CustomCell] が内部で保持する空の content 値。
 *
 * シングルトンであり常に自身とのみ相等になるため、静的形の [CustomCell] の等価性は実質
 * `id` + 表示スカラー（`style` / `showArrow` / `isEnabled` / `isVisible`）の比較になる。
 */
object CustomCellEmptyContent

/**
 * content を持たない静的なコンテンツ向けの [CustomCell] を生成する。
 *
 * `CustomCell { Text("...") }` のように builder だけで書ける省略形で、内部的には
 * content = [CustomCellEmptyContent] の [CustomCell] と同一の機構で描画される。
 *
 * @param id 一意 ID（既定で `custom-<random UUID>` を自動採番）
 * @param style Cell 個別スタイル
 * @param showArrow `true` で Disclosure Indicator を表示する
 * @param onTap 行タップ時のコールバック
 * @param isEnabled `false` で行タップと content 内部の操作を抑止し、content を淡色化する
 * @param isVisible `false` で visible projection から除外される
 * @param builder 表示する Compose コンテンツ
 */
fun CustomCell(
    id: String = "custom-${java.util.UUID.randomUUID()}",
    style: CellStyle = CellStyle(),
    showArrow: Boolean = false,
    onTap: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    builder: @Composable () -> Unit,
): CustomCell<CustomCellEmptyContent> = CustomCell(
    id = id,
    style = style,
    content = CustomCellEmptyContent,
    showArrow = showArrow,
    onTap = onTap,
    isEnabled = isEnabled,
    isVisible = isVisible,
    builder = { builder() },
)

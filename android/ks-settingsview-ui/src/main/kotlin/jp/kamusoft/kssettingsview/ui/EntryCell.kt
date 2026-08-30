package jp.kamusoft.kssettingsview.ui

import android.text.InputType
import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * テキスト入力用 Cell。
 *
 * 行内 trailing に `EditText` を配置し、ユーザー入力で `onTextChanged(String)` を発火する。
 * Compose DSL から使う場合は `MutableState<String>` を経由して TwoWay binding を構築し、
 * Store 経路（外部から `Cell` 値を直接構築する）からは `text: String` + `onTextChanged` を渡す。
 *
 * 他の Cell が持つ `valueText` フィールドは **持たない**。行内 trailing の位置を入力欄 `EditText`
 * が占めるため、同じ位置に値テキストを置くことができないためである。
 *
 * `keyboardType` は **`android.text.InputType` の `Int` 定数を直接公開**（独自列挙型でラップしない）。
 * UI 層の API では Native 型をそのまま公開する（core/ADR-0009）。
 *
 * @property id Cell の一意 ID（DSL 経路では `withDSLId` で安定 ID に rebind される）
 * @property style 任意の `CellStyle`（既定は空インスタンス）
 * @property title タイトル文字列
 * @property description 説明文（任意）
 * @property icon アイコン（任意）
 * @property hintText ヒントテキスト（任意、右上 float）
 * @property text 現在のテキスト値
 * @property placeholder プレースホルダ（任意）
 * @property keyboardType `android.text.InputType` の `Int` 定数（既定 `InputType.TYPE_CLASS_TEXT`）
 * @property isPassword パスワードマスクフラグ（既定 `false`）
 * @property textAlignment テキスト配置（既定 `END`、AiForms 互換）
 * @property accentColor caret 色および選択ハイライト色（任意）
 * @property maxLength 最大文字数（`null` で無制限、既定 `null`、AiForms `MaxLength: int` 互換）
 * @property onTextChanged テキスト変更時に呼ばれるクロージャ（TwoWay 経路でも内部で設定される）
 * @property isEnabled 有効／無効フラグ（既定 `true`）
 * @property isVisible 可視性フラグ（既定 `true`）
 * @property placeholderColor プレースホルダ文字色（任意）。`null` は未指定を意味し、
 *   `CellStyle.placeholderColor` → `Theme.cellPlaceholderColor` → プラットフォーム既定の順に解決する
 */
data class EntryCell(
    override val id: String = "entry-cell-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val text: String = "",
    val placeholder: String? = null,
    val keyboardType: Int = InputType.TYPE_CLASS_TEXT,
    val isPassword: Boolean = false,
    val textAlignment: CellTitleAlignment = CellTitleAlignment.END,
    val accentColor: Color? = null,
    val maxLength: Int? = null,
    val onTextChanged: ((String) -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
    val placeholderColor: Color? = null,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    /**
     * 等価性（値型としての性質）。クロージャ（[onTextChanged]）を除外し、
     * 内部状態 [text] / [isEnabled] / [isVisible] を含むすべての保持フィールドを比較する。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntryCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            description == other.description &&
            icon == other.icon &&
            hintText == other.hintText &&
            text == other.text &&
            placeholder == other.placeholder &&
            keyboardType == other.keyboardType &&
            isPassword == other.isPassword &&
            textAlignment == other.textAlignment &&
            accentColor == other.accentColor &&
            maxLength == other.maxLength &&
            isEnabled == other.isEnabled &&
            isVisible == other.isVisible &&
            placeholderColor == other.placeholderColor
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + (hintText?.hashCode() ?: 0)
        result = 31 * result + text.hashCode()
        result = 31 * result + (placeholder?.hashCode() ?: 0)
        result = 31 * result + keyboardType
        result = 31 * result + isPassword.hashCode()
        result = 31 * result + textAlignment.hashCode()
        result = 31 * result + (accentColor?.hashCode() ?: 0)
        result = 31 * result + (maxLength ?: 0)
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        result = 31 * result + (placeholderColor?.hashCode() ?: 0)
        return result
    }
}

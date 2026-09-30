package jp.kamusoft.kssettingsview.ui

import android.os.Parcel
import android.os.Parcelable

/**
 * 設定画面の表示位置を控えた値です。
 *
 * `KsSettingsView.captureScrollAnchor()` で控え、同じ内容を表示する画面の
 * `restoreScrollAnchor(anchor)` へ渡すと、控えたときに表示範囲の上端にかかっていた要素が同じずれで
 * 上端にかかる位置へ戻ります。要素の ID で控えるため、控えた後に項目が増減しても同じ要素の場所へ
 * 戻ります。中身 (座標や要素) は公開しません。
 *
 * [Parcelable] なので、`Bundle` に入れて画面の作り直しをまたいで運べます。
 */
public class KsScrollAnchor internal constructor(
    /** 控えた要素。 */
    internal val element: Element,
    /**
     * 表示範囲の上端から見た、要素の上端までのずれ (px)。要素の上端が表示範囲の上端より上にあれば正。
     */
    internal val offsetFromTop: Int,
) : Parcelable {

    /** 表示範囲の上端にかかっていた要素。 */
    internal sealed class Element {
        /** Cell の行。値は Cell の `id`。 */
        data class Cell(val id: String) : Element()

        /** Section の見出しの行。値は Section の `id`。 */
        data class SectionHeader(val sectionId: String) : Element()

        /** Section の Footer の行。値は Section の `id`。 */
        data class SectionFooter(val sectionId: String) : Element()

        /** Root Header。 */
        data object RootHeader : Element()

        /** Root Footer。 */
        data object RootFooter : Element()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        when (val e = element) {
            is Element.Cell -> {
                dest.writeInt(KIND_CELL)
                dest.writeString(e.id)
            }
            is Element.SectionHeader -> {
                dest.writeInt(KIND_SECTION_HEADER)
                dest.writeString(e.sectionId)
            }
            is Element.SectionFooter -> {
                dest.writeInt(KIND_SECTION_FOOTER)
                dest.writeString(e.sectionId)
            }
            Element.RootHeader -> {
                dest.writeInt(KIND_ROOT_HEADER)
                dest.writeString(null)
            }
            Element.RootFooter -> {
                dest.writeInt(KIND_ROOT_FOOTER)
                dest.writeString(null)
            }
        }
        dest.writeInt(offsetFromTop)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KsScrollAnchor) return false
        return element == other.element && offsetFromTop == other.offsetFromTop
    }

    override fun hashCode(): Int = 31 * element.hashCode() + offsetFromTop

    override fun toString(): String = "KsScrollAnchor($element, offsetFromTop=$offsetFromTop)"

    public companion object {
        private const val KIND_CELL = 0
        private const val KIND_SECTION_HEADER = 1
        private const val KIND_SECTION_FOOTER = 2
        private const val KIND_ROOT_HEADER = 3
        private const val KIND_ROOT_FOOTER = 4

        /** [Parcelable] の復元に使う生成器です。 */
        @JvmField
        public val CREATOR: Parcelable.Creator<KsScrollAnchor> = object : Parcelable.Creator<KsScrollAnchor> {
            override fun createFromParcel(source: Parcel): KsScrollAnchor {
                val kind = source.readInt()
                val id = source.readString()
                val offset = source.readInt()
                val element = when (kind) {
                    KIND_CELL -> Element.Cell(id.orEmpty())
                    KIND_SECTION_HEADER -> Element.SectionHeader(id.orEmpty())
                    KIND_SECTION_FOOTER -> Element.SectionFooter(id.orEmpty())
                    KIND_ROOT_HEADER -> Element.RootHeader
                    else -> Element.RootFooter
                }
                return KsScrollAnchor(element, offset)
            }

            override fun newArray(size: Int): Array<KsScrollAnchor?> = arrayOfNulls(size)
        }
    }
}

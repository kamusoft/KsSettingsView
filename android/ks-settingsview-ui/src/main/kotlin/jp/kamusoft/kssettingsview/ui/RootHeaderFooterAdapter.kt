package jp.kamusoft.kssettingsview.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.RootAccessory

/**
 * Root H/F（`KsSettingsView.rootHeader` / `rootFooter`）専用 Adapter。
 *
 * `KsSettingsView` 内部の `ConcatAdapter(headerAdapter, mainListAdapter, footerAdapter)` で
 * 先頭 / 末尾に配置される。`view` プロパティの `null` / 非 `null` で itemCount を 0 / 1 に
 * 切り替え、変化のたびに対応する `notifyItemInserted(0)` / `notifyItemRemoved(0)` /
 * `notifyItemChanged(0)` を発行する。
 *
 * `getItemId(0)` は header 用 `1L` / footer 用 `2L` を予約し、`mainListAdapter` 側はこれと
 * 衝突しない値域（`KsSettingsListAdapter.CELL_ID_OFFSET` 以上）を返す。
 *
 * Root H/F を `mainListAdapter` の項目に混ぜず専用 Adapter に分けるのは、Root 装飾が
 * モデルではなく View の責務であるため（core/ADR-0005）。
 *
 * @param role このアダプタが Header / Footer どちらの位置にいるか。`getItemId` の予約値および
 *   bind 時の Theme 色選択に使用する
 */
internal class RootHeaderFooterAdapter(
    private val role: Role,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * 描画時に参照する Theme（bind 時に Text Accessory の文字色 / 背景色決定に使う）。
     * 外部から `theme = ...` で更新する。
     */
    var theme: Theme = Theme()

    /**
     * 描画する [RootAccessory]。`null` で itemCount=0、非 `null` で itemCount=1 となる。
     *
     * Setter は変化前後の `null` / 非 `null` 状態を比較し、適切な `notifyItem*` を発行する。
     */
    var view: RootAccessory? = null
        set(value) {
            val oldValue = field
            field = value
            when {
                oldValue == null && value != null -> notifyItemInserted(0)
                oldValue != null && value == null -> notifyItemRemoved(0)
                oldValue != null && value != null -> {
                    // 中身（KsAnyView.View）の差分検出は不可なので、念のため必ず通知して再 bind させる
                    notifyItemChanged(0)
                }
                // null → null は通知不要
            }
        }

    init {
        // ConcatAdapter 全体で stable IDs を有効化するため
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = if (view == null) 0 else 1

    override fun getItemId(position: Int): Long {
        require(position == 0) { "RootHeaderFooterAdapter only supports position 0" }
        return when (role) {
            Role.HEADER -> RESERVED_ID_HEADER
            Role.FOOTER -> RESERVED_ID_FOOTER
        }
    }

    override fun getItemViewType(position: Int): Int {
        require(position == 0) { "RootHeaderFooterAdapter only supports position 0" }
        return when (view) {
            is RootAccessory.Text -> KsCellRegistry.VIEW_TYPE_ROOT_TEXT
            is RootAccessory.View -> KsCellRegistry.VIEW_TYPE_ROOT_VIEW
            null -> {
                // itemCount==0 のときに呼ばれる想定はないが、防御的に Text を返す
                KsCellRegistry.VIEW_TYPE_ROOT_TEXT
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            KsCellRegistry.VIEW_TYPE_ROOT_TEXT -> RootTextAccessoryViewHolder.create(parent)
            KsCellRegistry.VIEW_TYPE_ROOT_VIEW -> RootAnyViewAccessoryViewHolder.create(parent)
            else -> throw IllegalStateException("Unknown viewType $viewType in RootHeaderFooterAdapter")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val accessory = view) {
            is RootAccessory.Text -> {
                (holder as RootTextAccessoryViewHolder).bind(
                    accessory = accessory,
                    theme = theme,
                    isHeader = role == Role.HEADER,
                )
            }
            is RootAccessory.View -> {
                (holder as RootAnyViewAccessoryViewHolder).bind(accessory)
            }
            null -> {
                // itemCount==0 のはずなので onBindViewHolder は呼ばれない想定
            }
        }
    }

    /**
     * payload 付きの変更通知を、payload の種類に応じた反映へ振り分ける。
     *
     * [KsSettingsView.PAYLOAD_THEME] だけが届いた View 形式の Root H/F は何も反映しない。
     * View 形式は Theme が決める文字も寸法も持たないため描き直す必要がなく、逆に
     * `KsAnyView.AndroidView` の View は factory から作り直すと内部状態（入力中のテキスト・
     * スクロール位置・フォーカス）を失う。
     *
     * それ以外（payload なしの内容差し替え・text 形式・Theme 以外の payload が混ざる通知）は
     * 既定動作に委ね、2 引数版 [onBindViewHolder] のフル bind へ落とす。
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>,
    ) {
        val themeOnly = payloads.isNotEmpty() && payloads.all { it == KsSettingsView.PAYLOAD_THEME }
        if (themeOnly && holder is RootAnyViewAccessoryViewHolder) {
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is RootAnyViewAccessoryViewHolder) {
            holder.reset()
        }
    }

    /**
     * このアダプタの役割（Header / Footer）。
     */
    enum class Role {
        HEADER,
        FOOTER,
    }

    companion object {
        /** Header 位置の予約 ID。 */
        const val RESERVED_ID_HEADER: Long = 1L

        /** Footer 位置の予約 ID。 */
        const val RESERVED_ID_FOOTER: Long = 2L
    }
}

package jp.kamusoft.kssettingsview.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.SectionAccessory

/**
 * Section H/F + Cell の平坦リストを描画する `ListAdapter`。
 *
 * `mainListAdapter` として `KsSettingsView` 内部の `ConcatAdapter` 中央に配置される。
 * 内部で [AsyncListDiffer] を保持しているため、`submitList` は差分計算をバックグラウンドで実行する。
 *
 * # `getItemId` の値域
 *
 * `setHasStableIds(true)` を有効にした場合、`RootHeaderFooterAdapter` の予約値（1L / 2L）と
 * 衝突しないよう [CELL_ID_OFFSET]（100L）以上の値を返し、ID の衝突を避ける。
 *
 * 「表示状態同期の三層分離」原則に従い、`getItemId` は **内容に依存しない id ベースの安定 ID** を返す。
 * 内容（`title` / `isOn` 等）が変化しても同一 id の Cell に対しては同一の itemId を返すため、
 * 内容変化が「アイテムの差し替え」と誤認されてフルリバインドが走ることを防ぐ。具体的には
 * CellRow は `cell.id`、Section H/F は `sectionId` + 役割（header/footer）の安定文字列キーから
 * Long を算出する（内容依存の `hashCode` は使わない）。
 */
internal class KsSettingsListAdapter :
    ListAdapter<CellListItem, RecyclerView.ViewHolder>(CellListItemDiffCallback) {

    /**
     * 描画時に参照する `Theme`。`bind` 時に各 ViewHolder に渡す。
     * `submitList` 前後で外部から `theme = ...` で更新する。
     */
    var theme: Theme = Theme()

    init {
        // ConcatAdapter 内で stable ids を有効化するため、各 Adapter 側で setHasStableIds(true) を有効化する。
        // ConcatAdapter 自体は ConcatAdapter.Config.Builder で stableIdMode を設定するが、
        // 個別 Adapter 側でも有効化が必要（DiffUtil + Stable IDs の RecyclerView 標準動作）。
        setHasStableIds(true)
    }

    /**
     * 同一 id の Cell の **内容更新（reconfigure 相当）** を、セルを再生成せずに反映する。
     *
     * 「表示状態同期の三層分離」原則に従い、`ReplaceCell`（同一 id の内容更新）は `submitList` による
     * 行差し替えではなく、該当 position への `notifyItemChanged(position, [KsSettingsView.PAYLOAD_CONTENT])` で反映する。
     *
     * payload は省略できない。payload なしの `notifyItemChanged(position)` では
     * `SimpleItemAnimator.canReuseUpdatedViewHolder` が false を返し、RecyclerView が更新行の
     * ViewHolder を**新規生成して旧行とクロスフェード**する。EntryCell では EditText インスタンスごと
     * 差し替わるため、入力中の IME 接続が 1 打鍵ごとに切れる。payload を伴う通知は同一 ViewHolder への
     * 再 bind に落ちるため、行の再生成（ちらつき）と入力中断のいずれも起こさない。
     * `KsSettingsView` 側でも change アニメーションを無効化して二重に担保している。
     *
     * 内部リスト（differ の current list）と新 Cell を整合させるため、まず `submitList(newList)` で
     * リスト参照を差し替える。`CellListItem.CellRow` の `areContentsTheSame` は常に true を返すため
     * DiffUtil は当該行を「変化なし」と見なし再 bind しない。そこで本メソッドが明示的に
     * `notifyItemChanged(position, [KsSettingsView.PAYLOAD_CONTENT])` を発行して当該行のみ部分更新する。
     *
     * @param newList 当該 Cell 群を差し替え済みの新しい平坦リスト
     * @param cellIds 内容更新対象の Cell の id 群（複数同時更新に対応）
     */
    fun submitContentUpdate(newList: List<CellListItem>, cellIds: List<String>) {
        if (cellIds.isEmpty()) return
        submitListAndNotifyContent(newList, cellIds.toHashSet())
    }

    /**
     * 単一 Cell の内容更新を反映する利便オーバーロード。[submitContentUpdate] の単数版。
     *
     * @param newList 当該 Cell を差し替え済みの新しい平坦リスト
     * @param cellId 内容更新対象の Cell の id
     */
    fun submitContentUpdate(newList: List<CellListItem>, cellId: String) {
        submitContentUpdate(newList, listOf(cellId))
    }

    /**
     * full 更新（現 model 全体から作り直した平坦リストの反映）を、構造と内容の両面で完結させる。
     *
     * 構造（挿入・削除・移動）と Section H/F の内容差は `submitList` の DiffUtil が拾う。一方
     * `CellListItem.CellRow` の `areContentsTheSame` は常に true のため、更新をまたいで残る同一 id の
     * Cell の内容差は DiffUtil に現れない。そこでコミット完了後に [contentCellIds] の行へ
     * `notifyItemChanged(position, [KsSettingsView.PAYLOAD_CONTENT])` を発行して内容を反映する。
     *
     * [contentCellIds] が空でも `submitList` は必ず実行する。空 root や Section H/F だけを持つ root への
     * 更新のように、内容通知の対象が 1 件もなくても構造の反映は必要だからである。
     *
     * @param newList 新しい平坦リスト
     * @param contentCellIds 内容通知の対象となる Cell の id 群（更新前後の表示リスト双方に存在するもの）
     */
    fun submitFullUpdate(newList: List<CellListItem>, contentCellIds: Collection<String>) {
        submitListAndNotifyContent(newList, contentCellIds.toHashSet())
    }

    /**
     * 平坦リストを提出し、コミット完了後に [targetIds] の Cell 行へ内容更新通知を発行する。
     *
     * 複数の内容更新（例: RadioCell グループ連動で旧選択セルと新選択セルが同時に変化）は
     * 1 回の `submitList` にまとめて反映する。`submitList` を内容更新ごとに連続して複数回呼ぶと、
     * `AsyncListDiffer` が先行 `submitList` の完了コールバックを「最新世代ではない」として破棄し、
     * 一部の `notifyItemChanged` が失われる（旧選択セルの ✓ が消えない＝複数 ✓ になる）。そこで
     * 対象 id をまとめて受け取り、単一の `submitList` コミット後に対象 position 全てへ通知を発行する。
     * iOS 側の `reconfigureItems([複数 cellID])` と同じ「複数セルの内容を一括 reconfigure」発想。
     */
    private fun submitListAndNotifyContent(newList: List<CellListItem>, targetIds: Set<String>) {
        submitList(newList) {
            if (targetIds.isEmpty()) return@submitList
            // submitList のコミット完了後に position を解決して部分更新する。
            for ((position, item) in currentList.withIndex()) {
                if (item is CellListItem.CellRow && item.cell.id in targetIds) {
                    notifyItemChanged(position, KsSettingsView.PAYLOAD_CONTENT)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is CellListItem.SectionHeader -> when (item.accessory) {
                is SectionAccessory.Text -> KsCellRegistry.VIEW_TYPE_SECTION_HEADER_TEXT
                is SectionAccessory.View -> KsCellRegistry.VIEW_TYPE_SECTION_HEADER_VIEW
            }
            is CellListItem.SectionFooter -> when (item.accessory) {
                is SectionAccessory.Text -> KsCellRegistry.VIEW_TYPE_SECTION_FOOTER_TEXT
                is SectionAccessory.View -> KsCellRegistry.VIEW_TYPE_SECTION_FOOTER_VIEW
            }
            is CellListItem.CellRow -> KsCellRegistry.viewTypeOf(item.cell)
        }
    }

    override fun getItemId(position: Int): Long {
        // 「表示状態同期の三層分離」: getItemId は内容に依存しない id ベースの安定 ID を返す。
        // 内容（title / isOn 等）の変化では itemId を変えないため、同一 id の Cell が「別アイテム」
        // と誤認されてフルリバインドが走ることを防ぐ。
        //
        // 各 CellListItem の安定キー（CellRow=cell.id / SectionHeader=sectionId+":H" /
        // SectionFooter=sectionId+":F"）を String→Long の安定ハッシュへ変換し、CELL_ID_OFFSET を
        // 加算して RootHeaderFooterAdapter の予約 ID（1L=header / 2L=footer）と衝突しない値域に収める。
        return CELL_ID_OFFSET + stableIdOf(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            KsCellRegistry.VIEW_TYPE_SECTION_HEADER_TEXT,
            KsCellRegistry.VIEW_TYPE_SECTION_FOOTER_TEXT -> SectionTextAccessoryViewHolder.create(parent)

            KsCellRegistry.VIEW_TYPE_SECTION_HEADER_VIEW,
            KsCellRegistry.VIEW_TYPE_SECTION_FOOTER_VIEW -> SectionAnyViewAccessoryViewHolder.create(parent)

            else -> KsCellRegistry.createViewHolder(parent, viewType)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CellListItem.SectionHeader -> when (val accessory = item.accessory) {
                is SectionAccessory.Text -> (holder as SectionTextAccessoryViewHolder).bind(
                    accessory,
                    theme,
                    isHeader = true,
                    // Header 固定高さを ViewHolder に伝搬する。
                    headerHeight = item.headerHeight,
                )
                is SectionAccessory.View -> (holder as SectionAnyViewAccessoryViewHolder).bind(
                    accessory,
                    theme,
                    isHeader = true,
                    // 固定高さは accessory 種別に依らず適用するため、View 側にも伝搬する。
                    headerHeight = item.headerHeight,
                )
            }
            is CellListItem.SectionFooter -> when (val accessory = item.accessory) {
                is SectionAccessory.Text -> (holder as SectionTextAccessoryViewHolder).bind(accessory, theme, isHeader = false)
                is SectionAccessory.View -> (holder as SectionAnyViewAccessoryViewHolder).bind(
                    accessory,
                    theme,
                    isHeader = false,
                )
            }
            is CellListItem.CellRow -> {
                // strictMode = false 時のリリース向けフォールバックで生成された
                // EmptyPlaceholderViewHolder の場合は bind 対象がないので何もしない
                // （未登録 Cell に対してリリースビルドではクラッシュを回避する）。
                if (holder is EmptyPlaceholderViewHolder) return
                val cellHolder = holder as CellViewHolder<Cell>
                cellHolder.bind(item.cell, theme)
            }
        }
    }

    /**
     * payload 付きの変更通知を、payload の種類に応じた反映へ振り分ける。
     *
     * [KsSettingsView.PAYLOAD_HEADER_HEIGHT] だけが届いた View accessory の Section Header は、固定高さの
     * 反映だけを行って中身の再構築を行わない。`KsAnyView.AndroidView` の View は factory から
     * 作り直すと内部状態（入力中のテキスト等）を失うため、高さのみの変更で中身に触れない。
     *
     * [KsSettingsView.PAYLOAD_THEME] だけが届いた View accessory の Section H/F も同じ理由で
     * 中身に触れず、Theme が決める寸法（`Theme.headerHeight`）だけを反映する。View accessory は
     * Theme が決める文字を持たないため、中身を作り直しても見た目は変わらず内部状態だけを失う。
     *
     * それ以外（[KsSettingsView.PAYLOAD_CONTENT] を含む・payload なし・text accessory・Cell）は既定動作に委ね、
     * 2 引数版 [onBindViewHolder] のフル bind へ落とす。フル bind でも固定高さは反映される。
     * Theme と内容が同じ描画機会に重なった場合は payload に両方が載るため、この分岐には入らず
     * 中身が正しく作り直される。
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>,
    ) {
        val item = getItem(position)
        val heightOnly = payloads.isNotEmpty() && payloads.all { it == KsSettingsView.PAYLOAD_HEADER_HEIGHT }
        if (heightOnly && holder is SectionAnyViewAccessoryViewHolder && item is CellListItem.SectionHeader) {
            holder.applyHeaderHeight(theme = theme, isHeader = true, headerHeight = item.headerHeight)
            return
        }
        val themeOnly = payloads.isNotEmpty() && payloads.all { it == KsSettingsView.PAYLOAD_THEME }
        if (themeOnly && holder is SectionAnyViewAccessoryViewHolder) {
            when (item) {
                is CellListItem.SectionHeader -> holder.applyHeaderHeight(
                    theme = theme,
                    isHeader = true,
                    headerHeight = item.headerHeight,
                )
                // Footer に固定高さの概念はないが、Header と同じ経路を通して
                // 自動高さへの復帰（再利用された ViewHolder の後始末）を揃える。
                is CellListItem.SectionFooter -> holder.applyHeaderHeight(
                    theme = theme,
                    isHeader = false,
                    headerHeight = -1.0,
                )
                is CellListItem.CellRow -> Unit
            }
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        // CellViewHolder のみ reset() を呼ぶ（Section H/F の ViewHolder は内部で必要に応じて自前処理）
        if (holder is CellViewHolder<*>) {
            holder.reset()
        } else if (holder is SectionAnyViewAccessoryViewHolder) {
            holder.reset()
        }
    }

    companion object {
        /**
         * `getItemId` のオフセット値。`RootHeaderFooterAdapter` の予約 ID（1L / 2L）と衝突しない
         * 値域を確保するため、安定キーの Long ハッシュに本値を加算する。
         */
        const val CELL_ID_OFFSET: Long = 100L

        /**
         * [CellListItem] の **id ベースの安定キー** から、内容非依存の Long 値を算出する。
         *
         * - [CellListItem.CellRow] → `cell.id`
         * - [CellListItem.SectionHeader] → `sectionId + ":H"`
         * - [CellListItem.SectionFooter] → `sectionId + ":F"`
         *
         * 同一 sectionId の Header / Footer を別アイテムとして区別するため役割サフィックスを付与する。
         * String の `hashCode`（JVM 仕様で安定）を経由せず、64bit FNV-1a でハッシュ化して
         * Long の広い値域へ展開し衝突確率を下げる。
         */
        internal fun stableIdOf(item: CellListItem): Long {
            val key = when (item) {
                is CellListItem.CellRow -> item.cell.id
                is CellListItem.SectionHeader -> "${item.sectionId}:H"
                is CellListItem.SectionFooter -> "${item.sectionId}:F"
            }
            return fnv1a64(key)
        }

        /**
         * 文字列の 64bit FNV-1a ハッシュ。プラットフォーム間・実行間で安定した値を返す。
         */
        private fun fnv1a64(s: String): Long {
            var hash = -0x340d631b7bdddcdbL // FNV offset basis (14695981039346656037)
            for (ch in s) {
                hash = hash xor ch.code.toLong()
                hash *= 0x100000001b3L // FNV prime
            }
            return hash
        }
    }
}

/**
 * [CellListItem] 用の DiffUtil コールバック。
 *
 * - `areItemsTheSame`: sealed subtype 一致 + ID 比較
 * - `areContentsTheSame`: 項目種別で扱いが分かれる（android/ADR-0012）
 *   - [CellListItem.CellRow] は **常に `true`**（「表示状態同期の三層分離」: 構造同期は id 同一性のみ）。
 *     Cell はリスナー等を保持し得て値等価が安定しないため、内容（`data class equals` の全フィールド
 *     比較）を構造同期の判定に用いない。これにより内容変化（`isChecked` / `title` 等）が行全体の
 *     フルリバインド（ちらつき）を起こさない。同一 id の内容更新は
 *     [KsSettingsListAdapter.submitContentUpdate] による部分更新経路で反映する。
 *   - Section H/F は **accessory の内容を比較**する（Header は accessory 種別に依らず固定高さも含む）。
 *     これらは Cell のような専用の部分更新経路を持たず、`submitList` を通る全経路
 *     （`updateAccessory` / `replaceSection` / full 更新）で内容差を取りこぼすため、DiffUtil 側で
 *     拾って [getChangePayload] の payload 付き変更通知に落とす。
 */
internal object CellListItemDiffCallback : DiffUtil.ItemCallback<CellListItem>() {

    override fun areItemsTheSame(oldItem: CellListItem, newItem: CellListItem): Boolean {
        // sealed subtype が異なれば別アイテム
        if (oldItem::class != newItem::class) return false
        return when (oldItem) {
            is CellListItem.SectionHeader -> {
                newItem as CellListItem.SectionHeader
                oldItem.sectionId == newItem.sectionId
            }
            is CellListItem.SectionFooter -> {
                newItem as CellListItem.SectionFooter
                oldItem.sectionId == newItem.sectionId
            }
            is CellListItem.CellRow -> {
                newItem as CellListItem.CellRow
                // Cell の id が等しければ同一アイテム（内容差分は areContentsTheSame で判定）
                oldItem.cell.id == newItem.cell.id
            }
        }
    }

    override fun areContentsTheSame(oldItem: CellListItem, newItem: CellListItem): Boolean {
        return when (oldItem) {
            // 「表示状態同期の三層分離」: Cell の構造同期は id 同一性のみで行う。
            // areItemsTheSame（id 比較）が true で到達する CellRow 同士は「同一アイテム・構造変化なし」
            // と判定し、常に true を返す。Cell の内容（data class equals）を構造同期の判定に用いない。
            // 内容変化（isChecked / title 等）は notifyItemChanged による部分更新経路で反映する。
            is CellListItem.CellRow -> true

            // Section H/F は部分更新経路を持たないため、accessory の内容差をここで検出する。
            // Header の固定高さは accessory 種別に依らず表示に効くため、高さの差も内容差として扱う。
            is CellListItem.SectionHeader -> {
                newItem as CellListItem.SectionHeader
                isSameAccessoryContent(oldItem.accessory, newItem.accessory) &&
                    isSameHeaderHeight(oldItem, newItem)
            }
            is CellListItem.SectionFooter -> {
                newItem as CellListItem.SectionFooter
                isSameAccessoryContent(oldItem.accessory, newItem.accessory)
            }
        }
    }

    /**
     * 内容差が検出された行へ渡す payload。
     *
     * Section H/F の内容差では [KsSettingsView.PAYLOAD_CONTENT] を返し、payload 付きの変更通知
     * として発行させる。payload が非空であることで `SimpleItemAnimator.canReuseUpdatedViewHolder` が
     * true を返し、同一 ViewHolder への再 bind に落ちる（android/ADR-0001）。
     *
     * View accessory の Header で accessory の中身が同一のまま固定高さだけが変わった場合は
     * [KsSettingsView.PAYLOAD_HEADER_HEIGHT] を返す。この payload を受けた行は高さだけが
     * 更新され、`KsAnyView` の中身は作り直されない（内部状態が維持される）。
     *
     * 同一 ViewHolder への再 bind が成立するのは view type が変わらない場合に限る。
     * accessory が Text と View の間で切り替わると view type が変わり、RecyclerView は行の安定 ID を
     * 保ったまま ViewHolder を交換する。
     *
     * [CellListItem.CellRow] は `areContentsTheSame` が常に true を返すため本メソッドには到達しない。
     */
    override fun getChangePayload(oldItem: CellListItem, newItem: CellListItem): Any? {
        return when (oldItem) {
            is CellListItem.SectionHeader -> {
                newItem as CellListItem.SectionHeader
                val heightOnly = oldItem.accessory is SectionAccessory.View &&
                    isSameAccessoryContent(oldItem.accessory, newItem.accessory)
                if (heightOnly) {
                    KsSettingsView.PAYLOAD_HEADER_HEIGHT
                } else {
                    KsSettingsView.PAYLOAD_CONTENT
                }
            }

            is CellListItem.SectionFooter -> KsSettingsView.PAYLOAD_CONTENT

            is CellListItem.CellRow -> null
        }
    }

    /**
     * Section H/F の accessory 同士の内容が等価か判定する。
     *
     * - [SectionAccessory.Text] 同士は文字列の値等価で判定する。
     * - [SectionAccessory.View] 同士は保持する `KsAnyView` の **参照同一性** で判定する。
     *   `SectionAccessory.View.equals` は「クラス一致のみで等価」であり（`@Composable` ラムダや
     *   `(Context) -> View` ファクトリは値として比較できないため）、equals では View の差し替えを
     *   検出できない。参照が別インスタンスへ変わったことをもって内容変更とみなす。
     * - Text と View の間の切替は内容が異なるものとして扱う。
     */
    private fun isSameAccessoryContent(
        oldAccessory: SectionAccessory,
        newAccessory: SectionAccessory,
    ): Boolean {
        return when (oldAccessory) {
            is SectionAccessory.Text ->
                newAccessory is SectionAccessory.Text && oldAccessory.value == newAccessory.value

            is SectionAccessory.View ->
                newAccessory is SectionAccessory.View && oldAccessory.view === newAccessory.view
        }
    }

    /**
     * Section Header の固定高さ（`Section.headerHeight`）が等価か判定する。
     *
     * 固定高さは accessory 種別（Text / View）に依らず表示へ反映されるため、どちらの accessory でも
     * 高さの差を内容差として扱う。View accessory の高さのみが変わった場合は [getChangePayload] が
     * 高さ専用の payload を返し、中身の再構築を伴わない反映経路へ落ちる。
     */
    private fun isSameHeaderHeight(
        oldItem: CellListItem.SectionHeader,
        newItem: CellListItem.SectionHeader,
    ): Boolean = oldItem.headerHeight == newItem.headerHeight
}

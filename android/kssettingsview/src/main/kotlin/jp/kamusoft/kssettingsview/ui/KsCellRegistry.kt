package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.Cell
import kotlin.reflect.KClass

/**
 * Cell 型から `viewType` Int および `ViewHolder` ファクトリへ解決する中央レジストリ。
 *
 * 後続変更提案 `add-cell-types-*` が新 Cell を独立して登録できるよう、シングルトンで提供する。
 *
 * # 予約 viewType 値
 *
 * `RootHeaderFooterAdapter` および Section H/F の ViewHolder は本 registry とは独立に
 * 専用 viewType（[VIEW_TYPE_ROOT_TEXT] 等の `VIEW_TYPE_*` 定数）を使用し、Cell 用 viewType と
 * 衝突しない値域を割り振る。
 * Cell 用 viewType は [register] で渡された任意 Int を使用するため、利用側で衝突を避ける責務を持つ。
 * 推奨：Cell 用 viewType は 100 以上を使う。
 *
 * # 可視性
 *
 * 外部モジュール（Sample アプリや利用側アプリ）から独自 Cell 型を登録できるよう
 * `public` で公開する。利用側は独自 Cell 型を [register] に渡して描画対象に加える。
 * `createViewHolder` は同モジュール内の `KsSettingsListAdapter` から呼び出される
 * 内部詳細であるため `internal` のままとする。
 */
object KsCellRegistry {

    /**
     * Section ヘッダ（Text 形式）用予約 viewType。内部 Adapter / 内部テストからのみ参照する。
     */
    internal const val VIEW_TYPE_SECTION_HEADER_TEXT: Int = 1

    /**
     * Section ヘッダ（View 形式）用予約 viewType。内部 Adapter / 内部テストからのみ参照する。
     */
    internal const val VIEW_TYPE_SECTION_HEADER_VIEW: Int = 2

    /**
     * Section フッタ（Text 形式）用予約 viewType。内部 Adapter / 内部テストからのみ参照する。
     */
    internal const val VIEW_TYPE_SECTION_FOOTER_TEXT: Int = 3

    /**
     * Section フッタ（View 形式）用予約 viewType。内部 Adapter / 内部テストからのみ参照する。
     */
    internal const val VIEW_TYPE_SECTION_FOOTER_VIEW: Int = 4

    /**
     * Root H/F 用予約 viewType（Text 形式）。内部 Adapter / 内部テストからのみ参照する。
     */
    internal const val VIEW_TYPE_ROOT_TEXT: Int = 10

    /**
     * Root H/F 用予約 viewType（View 形式）。内部 Adapter / 内部テストからのみ参照する。
     */
    internal const val VIEW_TYPE_ROOT_VIEW: Int = 11

    /**
     * 未登録 Cell が submit された時のフォールバック用 viewType（リリースビルド向け）。
     *
     * `strictMode = false` のときに [viewTypeOf] が返す値であり、
     * [createViewHolder] では常に [EmptyPlaceholderViewHolder] にルーティングされる。
     * 内部 Adapter / 内部テストからのみ参照する。
     */
    internal const val VIEW_TYPE_PLACEHOLDER: Int = 99

    /**
     * Cell 用 viewType の最小推奨値。本値以上を [register] で使うと予約 viewType と衝突しない。
     */
    const val CELL_VIEW_TYPE_MIN: Int = 100

    /**
     * 未登録 Cell / 未登録 viewType を検出した時の動作モード。
     *
     * - `true`（デバッグビルド既定）: [IllegalStateException] をスローして実装漏れを早期発見する
     * - `false`（リリースビルド向け）: 空のプレースホルダ ViewHolder を返してアプリクラッシュを避ける
     *
     * 本ライブラリは Android Library として配布される都合上、`BuildConfig.DEBUG` を直接参照
     * できない（モジュールごとに `BuildConfig` が分かれる）。そのため利用側（アプリの
     * `Application#onCreate` 等）から `KsCellRegistry.strictMode = BuildConfig.DEBUG` の形で
     * 明示的に設定する想定。デフォルトは安全側に倒して `true`（デバッグ検出優先）とする。
     */
    @Volatile
    var strictMode: Boolean = true

    /**
     * Cell 型 → viewType / ファクトリ のエントリ。
     *
     * @property viewType 一意な viewType Int
     * @property factory 親 ViewGroup を受け取り、対応する [CellViewHolder] を生成するファクトリ関数
     */
    private data class Entry(
        val viewType: Int,
        val factory: (parent: ViewGroup) -> CellViewHolder<out Cell>,
    )

    /** Cell 型 → エントリのマッピング。 */
    private val entriesByCellClass: MutableMap<KClass<out Cell>, Entry> = mutableMapOf()

    /** viewType → エントリのマッピング（onCreateViewHolder で逆引き）。 */
    private val entriesByViewType: MutableMap<Int, Entry> = mutableMapOf()

    /**
     * viewType → Cell 型 の逆引きマップ（重複登録の衝突検証 O(1) のため）。
     *
     * [register] 呼出し時に「同じ viewType が別の Cell 型に既に割り当てられているか」を
     * 即座に判定するために維持する。`entriesByViewType` だけでは Entry から Cell 型を
     * 取り戻せないため、本マップで Cell 種類数 N に対する O(N) の線形探索を回避する。
     */
    private val cellClassByViewType: MutableMap<Int, KClass<out Cell>> = mutableMapOf()

    /**
     * Cell 型を登録する。
     *
     * 同じ [cellClass] に対する重複登録は許容し、最後の登録で上書きする
     * （プラグイン形式での後勝ち登録、テストでの差し替えに対応）。
     * 同じ [viewType] が別の [cellClass] に重複登録されたら [IllegalArgumentException] を投げる。
     *
     * @param cellClass Cell 型
     * @param viewType ListAdapter.getItemViewType で返す Int 値（`CELL_VIEW_TYPE_MIN` 以上推奨）
     * @param factory ViewHolder 生成ファクトリ
     */
    fun <T : Cell> register(
        cellClass: KClass<T>,
        viewType: Int,
        factory: (parent: ViewGroup) -> CellViewHolder<T>,
    ) {
        // 別の Cell 型に同じ viewType が割り当てられているなら衝突として拒否（O(1) の逆引き）。
        // 同じ Cell 型による上書き登録は許容し、別の Cell 型による重複は拒否する。
        val existingClass = cellClassByViewType[viewType]
        if (existingClass != null && existingClass != cellClass) {
            throw IllegalArgumentException(
                "viewType $viewType is already registered for $existingClass; cannot reuse for $cellClass",
            )
        }

        // 同じ Cell 型を別 viewType で再登録した場合、古い viewType エントリを掃除しておく
        // （Stale エントリの残存を防ぐ）。
        val previousEntry = entriesByCellClass[cellClass]
        if (previousEntry != null && previousEntry.viewType != viewType) {
            entriesByViewType.remove(previousEntry.viewType)
            cellClassByViewType.remove(previousEntry.viewType)
        }

        @Suppress("UNCHECKED_CAST")
        val entry = Entry(
            viewType = viewType,
            factory = factory as (ViewGroup) -> CellViewHolder<out Cell>,
        )
        entriesByCellClass[cellClass] = entry
        entriesByViewType[viewType] = entry
        cellClassByViewType[viewType] = cellClass
    }

    /**
     * Cell インスタンスから viewType を解決する。
     *
     * - 登録済み Cell: 対応する viewType を返す
     * - 未登録 Cell + [strictMode] = true（デバッグ既定）: [IllegalStateException] をスローする
     * - 未登録 Cell + [strictMode] = false（リリース向け）: [VIEW_TYPE_PLACEHOLDER] を返す
     *
     * @throws IllegalStateException 未登録 Cell が渡され、かつ [strictMode] = true のとき
     */
    fun viewTypeOf(cell: Cell): Int {
        val entry = entriesByCellClass[cell::class]
        if (entry != null) return entry.viewType
        if (strictMode) {
            throw IllegalStateException("Cell type ${cell::class} is not registered in KsCellRegistry")
        }
        // リリースビルド向けフォールバック：プレースホルダ viewType を返して描画パスを継続させる
        return VIEW_TYPE_PLACEHOLDER
    }

    /**
     * viewType に対応する ViewHolder を生成する。
     *
     * - [VIEW_TYPE_PLACEHOLDER] が渡された場合は常に [EmptyPlaceholderViewHolder] を返す
     * - 登録済み viewType: 対応するファクトリで ViewHolder を生成
     * - 未登録 viewType + [strictMode] = true（デバッグ既定）: [IllegalStateException] をスロー
     * - 未登録 viewType + [strictMode] = false（リリース向け）: [EmptyPlaceholderViewHolder] を返す
     *
     * デバッグビルドではスローしてバグを早期検出し、リリースビルドでは空のプレースホルダ
     * ViewHolder を返してアプリクラッシュを防ぐ。
     *
     * @throws IllegalStateException 未登録 viewType が渡され、かつ [strictMode] = true のとき
     */
    internal fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_PLACEHOLDER) {
            return EmptyPlaceholderViewHolder.create(parent)
        }
        val entry = entriesByViewType[viewType]
        if (entry != null) return entry.factory(parent)
        if (strictMode) {
            throw IllegalStateException("viewType $viewType is not registered in KsCellRegistry")
        }
        // リリースビルド向けフォールバック：空 View をラップした ViewHolder を返してクラッシュ回避
        return EmptyPlaceholderViewHolder.create(parent)
    }

    /**
     * 登録済みかを判定する（テスト・診断用）。
     */
    fun isRegistered(cellClass: KClass<out Cell>): Boolean = entriesByCellClass.containsKey(cellClass)

    /**
     * 全登録を解除する（テスト用）。
     *
     * [strictMode] もデフォルト値（`true`）にリセットする。
     */
    @Suppress("unused")
    internal fun clear() {
        entriesByCellClass.clear()
        entriesByViewType.clear()
        cellClassByViewType.clear()
        strictMode = true
    }
}

/**
 * 未登録 Cell をリリースビルドで描画する際に使用する空の `RecyclerView.ViewHolder`。
 *
 * 高さ 0 の `View` を保持し、ユーザに対して何も表示しない。`KsCellRegistry.strictMode = false`
 * かつ未登録 Cell が submit された場合のフォールバック先として使用される。
 */
internal class EmptyPlaceholderViewHolder private constructor(view: View) :
    RecyclerView.ViewHolder(view) {

    companion object {
        /**
         * ライブラリ所有 UI 用の Context（同梱テーマ適用済み）で空 View を生成し、ViewHolder に
         * ラップして返す（android/ADR-0020）。
         */
        fun create(parent: ViewGroup): EmptyPlaceholderViewHolder {
            val placeholder = View(parent.ksThemedContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                )
            }
            return EmptyPlaceholderViewHolder(placeholder)
        }
    }
}

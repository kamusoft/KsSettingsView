package jp.kamusoft.kssettingsview.ui

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.SectionAccessory
import org.junit.Assert.fail
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

/*
 * KsSettingsView の Robolectric テストが共有する、メインループの待機・表示内容の観測・変更通知の記録ユーティリティ。
 *
 * 待機 (idle / awaitConvergence / awaitDifferCommit)・観測 (committedTexts / visibleRowTexts)・
 * 変更通知の記録 (ChangeRecordingObserver) は、Store 更新が表示へ届くまでの非同期経路を扱う
 * どのテストでも同じ形になるため、ここに 1 つだけ置く。
 */

/** メインスレッドのキューに溜まっているメッセージを流し切る。 */
internal fun idle() {
    shadowOf(Looper.getMainLooper()).idle()
}

/**
 * [condition] が成立するまで、メインスレッドのキューを流しながら待つ（収束の観測境界）。
 *
 * `submitList` の差分計算は更新前後のリストがどちらも非空のときバックグラウンドスレッドへ回り、
 * 結果はメインスレッドへ post されてから反映される。post されるまでの間はキューを流しても何も
 * 起きないため、キューを流しては [condition] を確かめる形で成立を待つ。成立した時点で戻るので
 * 待ち時間は環境に応じて伸縮し、固定時間の待機は使わない。
 *
 * [timeoutMillis] を超えても成立しなければ失敗させる。黙って戻ると「収束前の状態」を検証した
 * ことにされてしまうため、時間切れは待機条件の誤りか実装の不達として明示的に落とす。時間切れ時は
 * 後続のアサーションまで到達しないので、その時点の Host の状態を失敗メッセージに載せる。
 * メッセージには「Store から届いた内部 root」と「Adapter がコミットしたリスト」を併記し、
 * 不達がどちらの段で止まったかを切り分けられるようにする。それ以外の観測 (Theme 等) を
 * 待機条件が含むときは、その値を [extraDiagnostics] で渡すとメッセージに併記される。
 */
internal fun awaitConvergence(
    view: KsSettingsView,
    timeoutMillis: Long = 5_000,
    extraDiagnostics: (() -> String)? = null,
    condition: () -> Boolean,
) {
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
    while (true) {
        idle()
        if (condition()) return
        if (System.nanoTime() >= deadline) {
            val extra = extraDiagnostics?.let { " / ${it()}" }.orEmpty()
            fail(
                "収束の待機条件が $timeoutMillis ms 以内に成立しなかった" +
                    " (コミット済みリスト: ${committedTexts(view)}" +
                    " / 内部 root: ${cellTitles(view)}$extra)",
            )
        }
        // バックグラウンドの差分計算が進むよう、待つ間は CPU を他スレッドへ譲る。
        // yield は OS へのヒントに留まり CPU 飽和時に譲れる保証がないため sleep を使う。
        Thread.sleep(1)
    }
}

/**
 * [condition] が成立するまで、メインスレッドのキューを流しながら Adapter の差分コミットを待つ。
 *
 * `AsyncListDiffer` の差分計算はバックグラウンドスレッドで走り、結果は main Looper へ post されて
 * から commit callback が発火する。バックグラウンド側の完了を待ちつつ main Looper を回すため、
 * [condition] が満たされるまで idle と確認を繰り返す。成立した時点で戻るので待ち時間は環境に応じて
 * 伸縮し、固定時間の待機は使わない。
 *
 * タイムアウトは待機条件の誤りか実装の不達 — 黙って戻ると後続のアサーションが「コミット完了前の
 * 状態」を検証したことにされてしまうため、その時点のコミット済みリストを載せて明示的に失敗させる。
 * リストの要約はテスト対象の Cell 型ごとに見たい内容が違うため、[committedSummary] で呼び出し側から
 * 受け取る。
 *
 * View を基点に収束を待つ場合は [awaitConvergence] を使う。こちらは Adapter を直接観測する。
 */
internal fun awaitDifferCommit(
    committedSummary: () -> List<String>,
    timeoutMillis: Long = 5_000,
    condition: () -> Boolean,
) {
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
    while (true) {
        idle()
        if (condition()) return
        if (System.nanoTime() >= deadline) {
            fail(
                "差分コミットの待機条件が $timeoutMillis ms 以内に成立しなかった" +
                    " (コミット済みリスト: ${committedSummary()})",
            )
        }
        // バックグラウンドの差分計算が進むよう、待つ間は CPU を他スレッドへ譲る。
        // yield は OS へのヒントに留まり CPU 飽和時に譲れる保証がないため sleep を使う。
        Thread.sleep(1)
    }
}

/**
 * 直前に流した root の Cell 構成が Adapter へコミットされるまで待つ。
 *
 * [CustomCell] はテキストを持たず [committedTexts] では観測できないため、コミット済みリストと
 * 内部 root の一致を Cell の id で観測する。待機の本体は [awaitDifferCommit] に委ね、時間切れ時は
 * その時点のコミット済み id 一覧が失敗メッセージに載る。
 */
internal fun awaitRootCommit(view: KsSettingsView, timeoutMillis: Long = 5_000) {
    awaitDifferCommit(
        committedSummary = { committedCellIds(view) },
        timeoutMillis = timeoutMillis,
    ) { committedCellIds(view) == rootCellIds(view) }
}

/** Adapter がコミット済みの平坦リストから、Cell 行の id を上から順に取り出す。 */
internal fun committedCellIds(view: KsSettingsView): List<String> =
    view.internalMainListAdapter().currentList.mapNotNull { item ->
        (item as? CellListItem.CellRow)?.cell?.id
    }

/** 内部 root が保持している Cell の id を順に取り出す。 */
internal fun rootCellIds(view: KsSettingsView): List<String> =
    view.internalRoot().sections.flatMap { section -> section.cells.map { it.id } }

/**
 * Adapter がコミット済みの平坦リストから、表示されるはずのテキストを上から順に取り出す。
 *
 * Section header / footer 行と Cell 行の双方を対象とし、テキストを持たない accessory や
 * [LabelCell] 以外の Cell は結果から落ちる。
 *
 * 表示行の検証は実際に生成された行（[visibleRowTexts]）で行うが、そちらはレイアウトを走らせて
 * からでないと得られない。待機の条件にはレイアウト前でも読める、コミット済みリストを使う。
 */
internal fun committedTexts(view: KsSettingsView): List<String> =
    view.internalMainListAdapter().currentList.mapNotNull { item ->
        when (item) {
            is CellListItem.SectionHeader -> (item.accessory as? SectionAccessory.Text)?.value
            is CellListItem.SectionFooter -> (item.accessory as? SectionAccessory.Text)?.value
            is CellListItem.CellRow -> (item.cell as? LabelCell)?.title
        }
    }

/** 内部 root が保持している Cell の title を順に取り出す。 */
internal fun cellTitles(view: KsSettingsView): List<String> =
    view.internalRoot().sections.flatMap { section ->
        section.cells.mapNotNull { (it as? LabelCell)?.title }
    }

/**
 * RecyclerView に並んでいる行のテキストを上から順に取り出す。
 *
 * 1 行あたりの全 `TextView` のテキストを `/` で連結するため、レイアウトを走らせた後に呼ぶ。
 */
internal fun visibleRowTexts(view: KsSettingsView): List<String> {
    val rv = view.internalRecyclerView()
    return (0 until rv.childCount).map { index ->
        collectTexts(rv.getChildAt(index)).joinToString("/")
    }
}

/** View 配下の全 `TextView` から、空でないテキストを順に集める。 */
private fun collectTexts(view: View): List<String> = when (view) {
    is TextView -> listOfNotNull(view.text?.toString()?.takeIf { it.isNotBlank() })
    is ViewGroup -> (0 until view.childCount).flatMap { collectTexts(view.getChildAt(it)) }
    else -> emptyList()
}

/**
 * `onItemRangeChanged` の position と payload を記録する Observer。
 *
 * payload なしの `notifyItemChanged(position)` も 3 引数版へ payload = null で届くため、
 * 記録された payload が非 null であることが「payload 付き通知」の証拠になる。
 */
internal class ChangeRecordingObserver : RecyclerView.AdapterDataObserver() {
    val changedPositions = mutableListOf<Int>()
    val payloads = mutableListOf<Any?>()

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        changedPositions += positionStart
        payloads += payload
    }
}

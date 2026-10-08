package jp.kamusoft.kssettingsview

import android.app.Application
import android.os.Looper
import android.view.WindowManager
import android.view.inspector.WindowInspector
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog

/**
 * どのテストの後でも、表示中の window を片付け、メインスレッドのキューを空にしてから次のテストへ
 * 進ませるテスト用 Application。
 *
 * `src/test/resources/robolectric.properties` の `application` で、このモジュールの Robolectric テスト
 * すべてに適用する。Robolectric はテストごとに、テスト本体とその後片付け（`@After`・rule）が
 * 終わった後、メインスレッドの looper を作り直す前に [onTerminate] を呼ぶ。
 *
 * # なぜ必要か
 *
 * Compose の UI dispatcher (`AndroidUiDispatcher.Main`) はプロセス内で 1 つだけ作られ、テストを
 * またいで使い回される。この dispatcher は、処理をメインスレッドへ予約すると「予約済み」の印を立て、
 * 予約した処理が実行されるまで次の予約を出さない。一方 Robolectric は、テストの終わりにメインスレッドの
 * キューを中身ごと捨てる。予約が消化されないままテストが終わると、予約だけが捨てられて印は立ったまま
 * 残り、下ろす手段がなくなる。
 *
 * 印が残ったプロセスでは、後続のテストの Compose は最初の composition だけを行い、再 composition・
 * `LaunchedEffect`・フレームの callback が 1 度も走らなくなる。テストの成否が、同じプロセスで先に
 * 走ったテストに左右される。
 *
 * そこで、キューが捨てられる前に、溜まっている処理をここで実行し切る。
 *
 * # 先に window を片付ける理由
 *
 * 表示中の window に Compose が残っていると、キューは空にならないことがある。フレームを要求し続ける
 * 処理（Compose の `Popup` を含む選択面や、終わらないアニメーション）があると、Robolectric が
 * フレームの要求を受けるたびに時計を進めて次のフレームをその場で配るためである。
 *
 * そこで、キューを空にする前に、表示中のダイアログを閉じ、残る window（Activity・`Popup` など）の
 * View 階層をすべて取り外す。window から外れた Compose は composition を破棄してフレームの要求を
 * やめるので、残る予約は有限になり、実行し切れる。テストは、選択面などを表示したまま終えてよい。
 *
 * ダイアログだけは View 階層を直接外さず、`Dialog.dismiss` で閉じる。閉じる処理をキューに残したまま
 * 終えるテストがあると、先に View 階層を外した場合、後から実行されたその処理が外す相手を
 * 見つけられずに例外になるためである。閉じたことで、そのダイアログの dismiss リスナーはここで呼ばれる。
 *
 * # 件数の上限
 *
 * window に載っていない発生源（フレームを待ち続けるコルーチン、自分を登録し直し続ける callback
 * など）は、window を片付けても止まらず、キューは空にならない。戻らなくならないよう、キューは
 * 1 件ずつ進め、[MAX_DRAIN_TASKS] 件を実行しても空にならなければ打ち切って、そのテストを失敗させる。
 * 打ち切った後は、同じプロセスで後に走るテストの Compose が動かなくなりうる。原因のテストを通すと、
 * 失敗は関係のない後続のテストに現れる。この失敗が出た実行では、後続のテストの結果を当てにしない。
 *
 * 上限は実時間ではなく件数で置く。1 回の反復が必ず 1 件を実行するので、件数なら結果が実行機の
 * 速さや混み具合に左右されない。
 *
 * # 失敗の扱い
 *
 * 残っていた処理が例外を投げても、流し切りは止めない。止めると、その後ろに並んでいた予約が
 * 消化されず、印が残る。例外は控えておき、流し切った後に投げ直す。
 *
 * ここで投げるものが複数あるときは、先に起きたものを投げ、残りを suppressed に付ける（処理の例外と
 * 打ち切りが両方あれば、処理の例外が先になる）。テスト本体が既に失敗しているときは、本体の失敗が
 * そのまま報告される。
 *
 * # この土台で無くならないもの
 *
 * 根にあるのは、Compose のプロセス共有の dispatcher と、それが握る「最初に Compose を使ったテストの
 * `Choreographer`」が、Robolectric のテストごとの初期化をまたいで残ることである。この土台はそれを
 * 無くさない。テストの終わりに片付けて、分かっている漏れ方を防いでいるだけである。分かっている
 * 残りの経路は次のとおり（どれも実測）。
 *
 * - 仮想の時計を大きく進めて終えるテストの後は、後続のテストの終わりの流し切りが上限に達して、
 *   後続のテストが失敗しうる。約 100 秒を超えて進めると起きる（このモジュールのテストが進めるのは
 *   最大 6 秒）。Robolectric がテストごとに時計を戻すと、dispatcher の `Choreographer` は、時計が
 *   先のテストの終わりの時刻へ追いつくまでフレームの実行を見送り続ける、というのが原因の推定である
 * - フレームの配信を止める API（`ShadowChoreographer.setPaused`）と Compose を併用すると、同じ
 *   プロセスの後続のテストで Compose の再 composition が届かなくなる。先に Compose を使ったテストが
 *   あるときは、そのテストの中でも届かない。テストの中で配信を再開して終えても同じである
 * - `@Config(application = ...)` で別の Application を指定したテストには、このクラスが使われず、
 *   ここに書いた片付けは行われない
 * - ダイアログ以外の window を外す処理（`WindowManager.removeView`、`ActivityController.destroy`
 *   など）をキューに残したまま終えると、その処理が終わりに実行され、外す相手が既に無いので、
 *   例外でそのテストが失敗する
 */
class MainLooperDrainingApplication : Application() {

    override fun onTerminate() {
        val failure = closeWindowsAndDrainMainLooper()
        super.onTerminate()
        failure?.let { throw it }
    }

    /**
     * 表示中の window を片付け、メインスレッドのキューを、空になるか [MAX_DRAIN_TASKS] 件を
     * 実行するまで 1 件ずつ進める。
     *
     * window の片付けは、処理を 1 件進めるたびに繰り返す。溜まっていた処理が新しい window を
     * 表示することがあり（次のメッセージへ回された選択面の提示など）、それを残すと、その window の
     * Compose がフレームを要求し始めてキューが空にならなくなる。
     *
     * @return テストの失敗として投げるもの（クラスの説明の「失敗の扱い」）。無ければ `null`
     */
    private fun closeWindowsAndDrainMainLooper(): Throwable? {
        val looper = shadowOf(Looper.getMainLooper())
        var failure: Throwable? = null
        fun record(thrown: Throwable) {
            val first = failure
            when {
                first == null -> failure = thrown
                // 同じ実体を自分自身の suppressed には付けられない（付けようとすると例外になる）。
                first !== thrown -> first.addSuppressed(thrown)
            }
        }
        fun attempt(step: () -> Unit) {
            try {
                step()
            } catch (thrown: Throwable) {
                record(thrown)
            }
        }

        var processed = 0
        attempt { closeWindows() }
        while (processed < MAX_DRAIN_TASKS && !looper.isIdle) {
            attempt { looper.runOneTask() }
            processed++
            attempt { closeWindows() }
        }
        if (processed >= MAX_DRAIN_TASKS && !looper.isIdle) {
            record(
                AssertionError(
                    "テストの終わりに、メインスレッドのキューが $MAX_DRAIN_TASKS 件を実行しても空に" +
                        "ならなかったので打ち切った。このまま通すと、同じプロセスで後に走るテストの Compose が" +
                        "動かなくなりうるので、このテストを失敗にする。表示中の window はすべて片付けた後" +
                        "なので、window に載っていないものを疑うこと（フレームを待ち続けるコルーチン、" +
                        "`Choreographer` や `Handler` へ自分を登録し直し続ける処理、先に走ったテストが" +
                        "仮想の時計を大きく進めたことなど）",
                ),
            )
        }
        return failure
    }

    /**
     * 表示中のダイアログを閉じ、残る window の View 階層を、先に開かれたものから順に取り外す。
     *
     * 取り外す直前に、まだ一覧にあることを確かめる。Compose の `Popup` は、開いた側の composition が
     * 破棄されるときに自分の window を自分で外すため、開いた側を外した時点で一覧から消えている。
     * 消えたものを外そうとすると例外になる。開いた側より先に `Popup` を外すのも同じ理由で例外に
     * なるので、順序は先に開かれたものからにする。
     */
    private fun closeWindows() {
        for (dialog in ShadowDialog.getShownDialogs()) {
            if (dialog.isShowing) dialog.dismiss()
        }
        val windowManager = getSystemService(WindowManager::class.java)
        for (rootView in WindowInspector.getGlobalWindowViews()) {
            if (rootView in WindowInspector.getGlobalWindowViews()) windowManager.removeViewImmediate(rootView)
        }
    }

    private companion object {
        /** キューを空にする処理で実行する件数の上限。 */
        const val MAX_DRAIN_TASKS: Int = 100_000
    }
}

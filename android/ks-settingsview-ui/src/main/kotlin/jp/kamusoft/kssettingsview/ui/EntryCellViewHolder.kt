package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.toArgb
import jp.kamusoft.kssettingsview.core.CellTitleAlignment

/**
 * [EntryCell] 描画用 ViewHolder。
 *
 * [CellBaseViews] の本体行（`contentRow`）に [EditText] を「残り幅全体を占める行内 trailing」
 * として配置し（[addFillingInlineTrailing]）、`bind` 内で [applyCellBaseLayout] を呼び出して
 * 共通フィールドを描画する。
 *
 * - `TextWatcher` を bind 内で設定し reset 内で除去する（再利用時のループ防止）
 * - 入力欄がフォーカスを持つ間は同一 Cell の内容更新で text を差し替えず、フォーカス喪失時に
 *   最後にバインドされた text へ再同期する
 * - `keyboardType: Int` を `EditText.inputType` にそのまま代入（独自列挙型を経由しない）
 * - `isPassword = true` のときは keyboard の class に対応するパスワード variation へ差し替え
 * - `accentColor` を `textCursorDrawable` の tint に反映（API 29+、`ks-settingsview-ui` の `minSdk = 29`）
 * - 解決済み placeholder 色を hint 色へ反映し、未指定のときは生成時に捕捉したホスト既定の
 *   `ColorStateList` を復元する
 * - `isEnabled` で `EditText.isEnabled` および色置換
 * - `maxLength` 非 null のとき `InputFilter.LengthFilter(maxLength)` を設定
 * - Enter は単一行のとき「完了」（キーボード閉鎖）として扱い、複数行のときは改行として扱う
 */
internal class EntryCellViewHolder(
    internal val views: CellBaseViews,
    internal val editText: EditText,
) : CellViewHolder<EntryCell>(views.root) {

    /** 現在 bind 中の Cell の通知ハンドラ。 */
    private var currentHandler: ((String) -> Unit)? = null

    /** 現在の TextWatcher（再利用時の解除用）。 */
    private var currentWatcher: TextWatcher? = null

    /**
     * 現在 bind 中の Cell の id。まだ何も bind していない、または [reset] 済みなら `null`。
     *
     * 「同じ Cell への再バインドか」の判定にはこの id だけを使う。[EntryCell] の等価性は `text` を
     * 含むため、`equals` や参照比較で判定すると **text が変わった再バインドを別 Cell と誤判定**し、
     * 入力中の上書き抑止がまさに必要な場面ですり抜ける。
     */
    private var boundCellId: String? = null

    /**
     * 最後に bind した Cell の text。フォーカス喪失時に入力欄を戻す先の値になる。
     *
     * 入力欄がフォーカスを持つ間は上書きを抑止するため表示と食い違い得るが、その間もこの値は
     * 最新の bind 内容を指す。
     */
    private var boundText: String? = null

    /**
     * ホストテーマ由来の hint 色（`android:textColorHint`）。明示 placeholder 色を適用する前の
     * 状態別表現（`ColorStateList`）をそのまま保持し、未指定へ戻すときの復元元にする。
     *
     * 単色へ潰さずに `ColorStateList` のまま持つことで、無効状態などの状態別の見え方も含めて
     * ホストの既定へ戻せる。
     *
     * ホストテーマが `android:textColorHint` を持つことを前提にする（Android Host が要求する
     * `Theme.Material3.*` 派生テーマは必ず持つ）。`null` になる構成では戻し先が存在しないため、
     * 復元は行えない。
     */
    private val hostHintTextColors: ColorStateList? = editText.hintTextColors

    /**
     * 現在 hint へ適用済みの placeholder 色（ARGB）。`null` は「ホスト既定を適用中」を表す。
     * [placeholderColorApplied] が `false` の間はこの値に意味はない。
     */
    private var appliedPlaceholderColor: Int? = null

    /** [appliedPlaceholderColor] が有効か（一度でも hint 色を適用したか）。 */
    private var placeholderColorApplied: Boolean = false

    /**
     * 生の Enter キー（キーイベントとして届く Enter）を「完了」として消費するリスナー。
     *
     * IME のソフトキーは `imeOptions` の完了アクション経由で処理されるが、キーイベントとして届く
     * `KEYCODE_ENTER` は `TextView` が下方向のフォーカス探索へ倒すため、移動先が無い位置の
     * EntryCell で例外になる。ここで消費してフォーカス探索を発生させない（android/ADR-0003）。
     *
     * `View.dispatchKeyEvent` は `onKeyDown` / `onKeyUp` より先にこのリスナーを呼ぶため、押下が
     * IME 側で消費され離上だけがアプリへ届く配達パターンでも消費できる。`OnEditorActionListener`
     * は押下時に消費された印を前提とするので、この配達パターンを塞げない。
     *
     * Enter 以外のキーと修飾キー付きの Enter は `false` を返して通常処理へ渡す。
     */
    private val consumeRawEnterListener = View.OnKeyListener { v, keyCode, event ->
        val isEnterKey =
            keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        if (isEnterKey && event != null && event.hasNoModifiers()) {
            // 押下も消費して `onKeyDown` へ渡さない。キーボードを閉じるのは離上の 1 回だけ。
            if (event.action == KeyEvent.ACTION_UP) {
                hideSoftInput(v)
            }
            true
        } else {
            false
        }
    }

    /**
     * フォーカスの取得・喪失を受け取る常設リスナー。
     *
     * - 取得時: `EditText.requestFocus()` だけではフォーカス遷移によって IME が出ないことがあるため、
     *   `InputMethodManager.showSoftInput` を併用して確実に表示する
     * - 喪失時: 抑止していた内容更新を [resyncTextOnFocusLost] で入力欄へ反映する
     *
     * 生成時に一度だけ装着して bind / reset で付け外ししない。`isEnabled = false` の代入は
     * フォーカス中の入力欄から即座にフォーカスを奪うため、bind の途中で差し替える方式だと
     * 「無効化による編集終了」が旧リスナーへ届いて再同期を取りこぼす。
     */
    private val focusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
        if (hasFocus) {
            if (v.isEnabled) showSoftInput(v)
        } else {
            resyncTextOnFocusLost()
        }
    }

    init {
        editText.onFocusChangeListener = focusChangeListener
    }

    override fun bind(cell: EntryCell, theme: Theme) {
        val effective = EffectiveStyle.from(views.root.context, theme, cell.style)

        // EntryCell は valueText を持たないため、共通行レイアウト関数には null を渡す。
        applyCellBaseLayout(
            views = views,
            title = cell.title,
            description = cell.description,
            valueText = null,
            icon = cell.icon,
            hintText = cell.hintText,
            effective = effective,
            isEnabled = cell.isEnabled,
        )
        applyCellBackground(views.root, effective)

        // 旧 TextWatcher を解除してから setText（リスナーループ回避）。
        currentWatcher?.let { editText.removeTextChangedListener(it) }
        currentWatcher = null

        // 編集中の値の正は入力欄自身であり、書き戻し経路から遅れて返ってくる値では上書きしない。
        // 打鍵 → 通知 → 内容更新の往復は入力より遅れて届くため、無条件に反映すると確定済みの打鍵を
        // 古い値へ巻き戻し、文字の欠落・並び替えとキャレット移動を招く。抑止した内容更新は
        // フォーカス喪失時に [resyncTextOnFocusLost] が反映する。設計判断: android/ADR-0014。
        val isSameCell = boundCellId == cell.id
        val editorOwnsText = isSameCell && editText.isFocused
        // 差分判定: 現在値と同値の場合は setText を呼ばない（カーソル位置維持）。
        // 日本語 IME のマークドテキスト保護を意識した実装。
        if (!editorOwnsText && editText.text?.toString() != cell.text) {
            applyTextToEditor(cell.text)
        }
        boundCellId = cell.id
        boundText = cell.text

        // placeholder（hint）反映。
        // `TextView.setHint` はフォーカス中の EditText では IME へ extracted text を再通知するため、
        // 差分判定して変化時のみ代入する。
        if (editText.hint?.toString() != cell.placeholder) {
            editText.hint = cell.placeholder
        }

        // placeholder（hint）の文字色。解決順は Cell 固有値 → CellStyle → Theme → ホストテーマ既定。
        applyPlaceholderColor(
            EffectiveStyle.effectivePlaceholderColor(
                entryPlaceholderColor = cell.placeholderColor,
                cellStyle = cell.style,
                theme = theme,
            )?.toArgb(),
        )

        // keyboardType は Native 型 Int をそのまま代入。`isPassword = true` のときは
        // keyboard の class に対応するパスワード variation へ差し替える。
        val baseInputType = cell.keyboardType
        val targetInputType = if (cell.isPassword) {
            passwordInputType(baseInputType)
        } else {
            baseInputType
        }
        // `TextView.setInputType` は同値の代入でも内部で `InputMethodManager.restartInput` を呼び、
        // フォーカス中の EditText の未確定文字列（日本語 IME の composing text）を強制確定させる。
        // 入力 1 文字ごとに同値の再バインドが走る経路があるため、変化時のみ代入して変換を中断させない。
        // 設計判断: android/ADR-0001。
        if (editText.inputType != targetInputType) {
            editText.inputType = targetInputType
        }

        // 生 Enter の消費は単一行のときだけ。複数行では Enter で改行できる挙動を維持する。
        // 判定はフレームワークの複数行判定（TEXT クラス + MULTI_LINE フラグ）に合わせる。
        val isMultiLine = (
            targetInputType and (InputType.TYPE_MASK_CLASS or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
            ) == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        editText.setOnKeyListener(if (isMultiLine) null else consumeRawEnterListener)

        // textAlignment 反映（end は右寄せ、AiForms 互換の既定）。
        editText.gravity = when (cell.textAlignment) {
            CellTitleAlignment.START -> Gravity.START or Gravity.CENTER_VERTICAL
            CellTitleAlignment.CENTER -> Gravity.CENTER
            CellTitleAlignment.END -> Gravity.END or Gravity.CENTER_VERTICAL
        }

        // 色: 入力済みテキストは valueText の解決色を使う（無効時は disabledTextColor を優先）。
        val textColor = if (cell.isEnabled) effective.valueTextColor else effective.disabledTextColor
        editText.setTextColor(textColor)

        // accent 色: cell.accentColor → effective.accentColor。
        // API 29+ で textCursorDrawable に tint を適用する。
        val accent = cell.accentColor?.toArgb() ?: effective.accentColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val cursorDrawable = editText.textCursorDrawable
                if (cursorDrawable != null) {
                    cursorDrawable.setTintList(ColorStateList.valueOf(accent))
                }
            } catch (_: Throwable) {
                // テスト環境などで取得できない場合は無視。
            }
        }
        // 選択ハイライト色（caret 同等の用途）も accent で揃える。
        try {
            editText.highlightColor = accent
        } catch (_: Throwable) {
            // 何もしない（テスト環境で失敗するケースを安全に無視）。
        }

        // maxLength: 非 null なら LengthFilter を設定、null なら無制限。
        // filters の代入は編集中の Editable へフィルタ列を張り直すため、現在の LengthFilter の
        // 上限値と比較して変化時のみ差し替える（filters は本 ViewHolder が排他的に管理する）。
        val currentMaxLength = (editText.filters.firstOrNull() as? InputFilter.LengthFilter)?.max
        if (currentMaxLength != cell.maxLength) {
            editText.filters = if (cell.maxLength != null) {
                arrayOf<InputFilter>(InputFilter.LengthFilter(cell.maxLength))
            } else {
                arrayOf()
            }
        }

        // isEnabled の反映
        editText.isEnabled = cell.isEnabled
        editText.isFocusable = cell.isEnabled
        editText.isFocusableInTouchMode = cell.isEnabled

        // TwoWay 通知用 TextWatcher を新規に設定。
        currentHandler = cell.onTextChanged
        val handler = currentHandler
        if (handler != null) {
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    handler.invoke(s?.toString() ?: "")
                }
            }
            editText.addTextChangedListener(watcher)
            currentWatcher = watcher
        }

        // Cell 本体タップでも EditText にフォーカスを移し、IME を明示表示する
        // （iOS の `UITextField.becomeFirstResponder` と挙動を揃える）。
        if (cell.isEnabled) {
            views.root.isClickable = true
            views.root.setOnClickListener {
                editText.requestFocus()
                showSoftInput(editText)
            }
        } else {
            views.root.setOnClickListener(null)
            views.root.isClickable = false
        }

        applyEffectiveHeight(views.root, effective)
    }

    /**
     * 入力欄へ [text] を反映し、キャレットを末尾へ置く。
     *
     * 反映の間だけ [TextWatcher] を外すため、この経路の変更は `onTextChanged` として通知されない。
     * プログラム由来の反映を書き戻し経路へ逆流させないための措置であり、逆流させると
     * 「入力欄へ戻した値」がそのままアプリ状態を上書きして直前のユーザー入力を消す。
     */
    private fun applyTextToEditor(text: String) {
        val watcher = currentWatcher
        watcher?.let { editText.removeTextChangedListener(it) }
        editText.setText(text)
        // setSelection は text の長さの範囲内に丸める。
        val selPos = text.length.coerceAtLeast(0)
        try {
            editText.setSelection(selPos)
        } catch (_: Throwable) {
            // テスト環境 (Robolectric) で setSelection が失敗するケースを安全に無視。
        }
        watcher?.let { editText.addTextChangedListener(it) }
    }

    /**
     * hint（placeholder）の文字色を反映する。
     *
     * [argb] が非 `null` のときは状態によらない単色として適用するため、無効状態でも placeholder の色は
     * 変わらない。`null`（どの段にも指定が無い）のときは [hostHintTextColors] を復元し、ホストテーマの
     * 状態別表現をそのまま使う。
     *
     * 変化が無いときは代入しない。`EntryCell` は入力 1 文字ごとに同値の再バインドが走る経路があり、
     * 他の属性と同じく差分判定で無駄な再設定を避ける。
     */
    private fun applyPlaceholderColor(@ColorInt argb: Int?) {
        if (placeholderColorApplied && appliedPlaceholderColor == argb) return
        if (argb != null) {
            editText.setHintTextColor(argb)
        } else {
            val hostDefault = hostHintTextColors
            if (hostDefault == null) {
                // 戻し先が無い（ホストテーマが hint 色を持たない）ため、適用済みとして記録しない。
                // 次の bind で改めて評価し、指定色が来たときは確実に反映されるようにする。
                return
            }
            editText.setHintTextColor(hostDefault)
        }
        appliedPlaceholderColor = argb
        placeholderColorApplied = true
    }

    /**
     * フォーカス喪失時に、入力欄を最後に bind した Cell の text へ戻す。
     *
     * フォーカス中は内容更新による text の差し替えを抑止しているため、抑止された更新はここで
     * 初めて表示に現れる。書き戻しの往復が未完了なら、この時点の表示は入力途中より古い値に
     * なり得るが、遅れて届く内容更新が非フォーカス状態の入力欄へ反映されて最終値へ収束する。
     */
    private fun resyncTextOnFocusLost() {
        val target = boundText ?: return
        if (editText.text?.toString() == target) return
        applyTextToEditor(target)
    }

    /**
     * 指定の inputType をパスワード入力（伏せ字表示）の inputType へ変換する。
     *
     * variation は `TYPE_MASK_VARIATION` の中に入る「値」であってフラグではなく、フレームワークの
     * パスワード判定は class + variation の等値比較で行われる。そのため URI / EMAIL などの
     * variation へパスワード variation を OR 合成すると、どのパスワード種別にも一致しない未定義の
     * 組み合わせになり伏せ字にならない。既存の variation を消してから書き込むことで、keyboard 種別と
     * パスワード指定を併用しても必ず伏せ字になる。variation 以外のフラグ（複数行・数値の小数許可など）は
     * そのまま残す。
     *
     * 数値クラスには数値用のパスワード variation を使う。電話番号クラスにはパスワード variation が
     * 存在しないため、伏せ字表示を優先してテキストクラスのパスワード入力へ倒す（電話番号用の
     * キーパッド表示は失われる）。
     *
     * @param baseInputType keyboard 種別が表す inputType
     */
    private fun passwordInputType(baseInputType: Int): Int {
        val withoutVariation = baseInputType and InputType.TYPE_MASK_VARIATION.inv()
        return when (baseInputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER ->
                withoutVariation or InputType.TYPE_NUMBER_VARIATION_PASSWORD

            InputType.TYPE_CLASS_PHONE ->
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            else -> withoutVariation or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }

    /**
     * 指定 [View] に紐づくウィンドウへソフトキーボードを表示する。
     * `InputMethodManager` 取得や `showSoftInput` 失敗（テスト環境含む）は安全に無視する。
     */
    private fun showSoftInput(view: View) {
        try {
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        } catch (_: Throwable) {
            // 何もしない（Robolectric などのテスト環境で IMM が利用できない場合への保険）。
        }
    }

    /**
     * 指定 [View] に紐づくウィンドウのソフトキーボードを閉じる。
     * `InputMethodManager` 取得や `hideSoftInputFromWindow` 失敗（テスト環境含む）は安全に無視する。
     */
    private fun hideSoftInput(view: View) {
        try {
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (_: Throwable) {
            // 何もしない（Robolectric などのテスト環境で IMM が利用できない場合への保険）。
        }
    }

    override fun reset() {
        views.titleView.text = null
        views.descriptionView.text = null
        views.hintTextView.text = null
        // 前 Cell の同一性判定と再同期の基準値を破棄する。次の bind は「別 Cell への初回 bind」と
        // 同じ扱いになり、リサイクル前の値を持ち越さない。ここで先に捨てることで、リサイクルに
        // 伴うフォーカス喪失が前 Cell の値を書き戻すことも防ぐ。
        boundCellId = null
        boundText = null
        // TextWatcher を確実に解除し、再利用時のループを防ぐ。
        currentWatcher?.let { editText.removeTextChangedListener(it) }
        currentWatcher = null
        currentHandler = null
        editText.setText("")
        editText.hint = null
        // hint 色をホスト既定へ戻し、次の bind を「初回適用」として扱う。
        hostHintTextColors?.let { editText.setHintTextColor(it) }
        appliedPlaceholderColor = null
        placeholderColorApplied = false
        editText.filters = arrayOf()
        // 前回 bind の属性が ViewHolder リサイクル時に残らないよう既知デフォルトに戻す。
        // 後続 bind では必ず上書きされる前提だが、prepareForReuse 相当のフラットな初期状態を保証する。
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        editText.highlightColor = 0
        // Enter の扱いは bind で行ごとに決まるため、再利用時に前回の設定を持ち越さない。
        editText.setOnKeyListener(null)
        // textCursorDrawable の tint も既定状態に戻す（API 29+）。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                editText.textCursorDrawable?.setTintList(null)
            } catch (_: Throwable) {
                // テスト環境などで取得できない場合は無視。
            }
        }
        views.root.setOnClickListener(null)
        views.root.isClickable = false
    }

    /** テスト用：シミュレートで text を変更し TextWatcher を発火させる。 */
    internal fun simulateTextInput(newText: String) {
        editText.setText(newText)
    }

    companion object {
        fun create(parent: ViewGroup): EntryCellViewHolder {
            val views = buildCellBaseViews(parent)
            // アクセサリも共通行と同じ Context（同梱テーマ適用済み）から生成する。
            val ctx = views.root.context
            val edit = EditText(ctx).apply {
                // 余白 / 背景は最小限。SettingsView の Cell 内ではフラットな見た目に揃える。
                background = null
                // 1 行入力既定（複数行が必要なケースは利用者が `keyboardType` で TYPE_TEXT_FLAG_MULTI_LINE
                // を OR して指定する）。
                isSingleLine = true
                // Enter は「完了」= キーボード閉鎖として扱う（android/ADR-0003）。
                // imeOptions 未指定の EditText では IME の Enter が下方向のフォーカス探索を起こし、
                // 移動先が無い位置に置かれた EntryCell でフレームワークが例外を投げる。
                // 複数行 inputType が指定された場合はフレームワークが IME_FLAG_NO_ENTER_ACTION を
                // 付加するため、Enter による改行はそのまま使える。
                // これが塞ぐのは IME のソフトキー経路のみで、生のキーイベント経路は bind が
                // 設定する `consumeRawEnterListener` が受け持つ。
                imeOptions = EditorInfo.IME_ACTION_DONE
            }
            // 本体行（contentRow）へ「残り幅全体を占める行内 trailing」として追加する。
            // title はコンテンツ幅を確保し、EditText が主行の残り幅を占める
            // （android/ADR-0002 / core/ADR-0026）。
            addFillingInlineTrailing(views, edit)
            return EntryCellViewHolder(views = views, editText = edit)
        }
    }
}

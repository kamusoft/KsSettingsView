package jp.kamusoft.kssettingsview.samples.android

import android.text.InputType
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.compose.DatePickerCell
import jp.kamusoft.kssettingsview.compose.EntryCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.NumberPickerCell
import jp.kamusoft.kssettingsview.compose.PickerCell
import jp.kamusoft.kssettingsview.compose.TimePickerCell
import jp.kamusoft.kssettingsview.ui.DatePickerUIStyle
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 入力系 Cell 5 種（[EntryCell] / [PickerCell] / [NumberPickerCell] / [TimePickerCell] /
 * [DatePickerCell]）を 1 画面に並べて目視確認できるデモ画面。
 *
 * iOS Sample の `InputCellsDemoView.swift` と Section 構成・表示文言・デモデータを
 * 一致させる（基本 Cell 7 種デモと同一様式 = MAUI 互換 Theme + 直近イベント 1 行）。
 *
 * 末尾の「EntryCell（下部配置）」Section は、画面下半分に置いた [EntryCell] の
 * キーボード回避（フォーカス時のせり上がり）を確認するための検証用。
 */
@Composable
fun InputCellsDemoScreen() {
    val dark = isSystemInDarkTheme()

    // MARK: - EntryCell TwoWay binding 用
    /** 名前（テキスト、`maxLength = 20`） */
    val userName = remember { mutableStateOf("Tanaka Taro") }
    /** メールアドレス（テキスト、Email キーボード） */
    val email = remember { mutableStateOf("tanaka.taro@example.com") }
    /** 電話番号（Native `InputType.TYPE_CLASS_PHONE` 直接渡し） */
    val phone = remember { mutableStateOf("090-0000-0000") }
    /** パスワード（マスク表示） */
    val password = remember { mutableStateOf("secret123") }

    // MARK: - EntryCell Store 経路（callback）用
    /** callback 経路の表示用ニックネーム */
    val nickname = remember { mutableStateOf("") }

    // MARK: - EntryCell placeholder 色指定用
    /** 表示名（placeholder 色を Cell 個別に指定する行） */
    val displayName = remember { mutableStateOf("") }

    // MARK: - PickerCell（単一）TwoWay binding 用
    val themes = remember { listOf("ライト", "ダーク", "自動") }
    val themeIndex = remember { mutableStateOf<Int?>(0) }

    // MARK: - PickerCell（複数）TwoWay binding 用
    val notifTypes = remember { listOf("メール", "プッシュ", "SMS", "アプリ内", "電話") }
    val notifSelection = remember { mutableStateOf(setOf(0, 2)) }

    // MARK: - PickerCell（object 候補）用
    /** object 候補：主表示に名前、副表示に役割を射影する架空のメンバー */
    val members = remember { SampleMember.notificationTargets }
    /** 単一選択：担当者（選択した元要素をそのまま受け取る経路） */
    val assignee = remember { mutableStateOf<SampleMember?>(members.first()) }
    /** 複数選択：通知先メンバーの index 集合（確定時に元要素の一覧を callback で受け取る） */
    val memberSelection = remember { mutableStateOf(setOf(0, 2)) }

    // MARK: - NumberPickerCell TwoWay binding 用
    /** サイズ（10..30、step 1、単位 "px"） */
    val volume = remember { mutableIntStateOf(30) }

    // MARK: - TimePickerCell TwoWay binding 用
    /** アラーム（24 時間制の `format`） */
    val alarmTime = remember { mutableStateOf(LocalTime.of(7, 30)) }
    /** 就寝時刻（`is24Hour = false`。選択面が午前／午後のホイールを持つ形になる） */
    val bedTime = remember { mutableStateOf(LocalTime.of(22, 15)) }

    // MARK: - DatePickerCell TwoWay binding 用
    /** 誕生日 */
    val birthday = remember { mutableStateOf(LocalDate.of(1990, 1, 1)) }
    /** 予約日（誕生日とは独立した状態） */
    val reservation = remember { mutableStateOf(LocalDate.of(2026, 6, 1)) }
    /** 誕生日の maxDate（iOS の `maxDate: Date()` に対応する「今日」） */
    val today = remember { LocalDate.now() }

    // MARK: - EntryCell 下部配置（キーボード回避検証）用
    /** メモ（画面下半分での フォーカス→せり上がり 検証用） */
    val memo = remember { mutableStateOf("") }
    /** 署名（最下部での フォーカス→せり上がり 検証用） */
    val signature = remember { mutableStateOf("") }

    // MARK: - 直近イベント表示
    /** 「最後のイベント: <Cell の title> → <変更後の値>」形式の 1 行表示。 */
    val lastEvent = remember { mutableStateOf("(none)") }
    val record: (String) -> Unit = { event -> lastEvent.value = event }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "最後のイベント: ${lastEvent.value}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp),
        )

        KsSettingsView(
            modifier = Modifier.fillMaxSize(),
            style = KsSettingsViewStyle.Classic,
            theme = SampleTheme.maui(dark),
        ) {
            // 1. EntryCell セクション（TwoWay binding + Store 経路）
            Section(
                header = "EntryCell",
                footer = "ニックネーム (callback) は値変更コールバックで状態を更新する経路のデモ。他の入力欄は双方向バインディング経路。表示名は placeholder 色を Cell 個別に指定した行。",
            ) {
                // TwoWay binding（MutableState<String>）
                EntryCell(
                    title = "名前",
                    text = userName.tracked(title = "名前", onEvent = record),
                    placeholder = "山田 太郎",
                    keyboardType = InputType.TYPE_CLASS_TEXT,
                    maxLength = 20,
                )
                EntryCell(
                    title = "メール",
                    text = email.tracked(title = "メール", onEvent = record),
                    placeholder = "example@example.com",
                    keyboardType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                )
                // Native InputType.TYPE_CLASS_PHONE 直接渡し
                EntryCell(
                    title = "電話",
                    text = phone.tracked(title = "電話", onEvent = record),
                    placeholder = "090-0000-0000",
                    keyboardType = InputType.TYPE_CLASS_PHONE,
                )
                // パスワードマスク（イベント表示も Cell と同じマスク形式に合わせる）
                EntryCell(
                    title = "パスワード",
                    text = password.tracked(title = "パスワード", display = ::maskedText, onEvent = record),
                    placeholder = "8 文字以上",
                    isPassword = true,
                )
                // Store 経路（text: String + onTextChanged callback）
                EntryCell(
                    title = "ニックネーム (callback)",
                    text = nickname.value,
                    placeholder = "callback 経路で更新",
                    onTextChanged = { newValue ->
                        if (newValue == nickname.value) return@EntryCell
                        nickname.value = newValue
                        record("ニックネーム (callback) → $newValue")
                    },
                )
                // placeholder 色を Cell 個別に指定する経路（未指定の行は OS 既定色）
                EntryCell(
                    title = "表示名",
                    text = displayName.tracked(title = "表示名", onEvent = record),
                    placeholder = "placeholder 色の指定例",
                    placeholderColor = SampleTheme.demoPlaceholderOrange,
                )
            }

            // 2. PickerCell（単一）セクション
            Section(header = "PickerCell（単一選択）") {
                PickerCell(
                    title = "テーマ",
                    items = themes,
                    selectedIndex = themeIndex.tracked(
                        title = "テーマ",
                        display = { index -> themes.getOrNull(index ?: -1) ?: "(未選択)" },
                        onEvent = record,
                    ),
                    pageTitle = "テーマを選択",
                )
            }

            // 3. PickerCell（複数）セクション
            Section(
                header = "PickerCell（複数選択 / 上限 3）",
                footer = "3 つまで選択できます。4 つ目を選ぼうとすると触覚フィードバックが返ります。",
            ) {
                PickerCell(
                    title = "通知種別",
                    items = notifTypes,
                    selectedIndices = notifSelection.tracked(
                        title = "通知種別",
                        display = { indices ->
                            val labels = indices.sorted().mapNotNull { notifTypes.getOrNull(it) }
                            if (labels.isEmpty()) "(未選択)" else labels.joinToString(separator = ", ")
                        },
                        onEvent = record,
                    ),
                    maxSelectedNumber = 3,
                    pageTitle = "通知種別を選択",
                )
            }

            // 4. PickerCell（object 候補）セクション
            Section(
                header = "PickerCell（object 候補 / 副表示）",
                footer = "任意の型の要素を候補にして、主表示と副表示に射影するデモ。担当者は選択した要素そのものを受け取る経路、通知先メンバーは確定時に選択要素の一覧を受け取る経路。副表示を持たない候補は 1 行で表示される。",
            ) {
                PickerCell(
                    title = "担当者",
                    items = members,
                    displayText = { it.name },
                    selectedItem = assignee.tracked(
                        title = "担当者",
                        display = { it?.name ?: "(未選択)" },
                        onEvent = record,
                    ),
                    subText = { it.role },
                    pageTitle = "担当者を選択",
                )
                PickerCell(
                    title = "通知先メンバー",
                    items = members,
                    displayText = { it.name },
                    selectedIndices = memberSelection,
                    subText = { it.role },
                    onItemsSelected = { picked ->
                        val labels = picked.map { it.name }
                        record(
                            "通知先メンバー → " +
                                if (labels.isEmpty()) "(未選択)" else labels.joinToString(separator = ", "),
                        )
                    },
                    pageTitle = "通知先メンバー",
                )
            }

            // 5. NumberPickerCell セクション
            Section(
                header = "NumberPickerCell",
                footer = "Picker UI と Cell の valueText に \"px\" suffix が付く。",
            ) {
                NumberPickerCell(
                    title = "サイズ",
                    min = 10,
                    max = 30,
                    step = 1,
                    value = volume.tracked(title = "サイズ", display = { "$it px" }, onEvent = record),
                    // iOS と同じ `unit` で、Cell の valueText と選択面の候補表示の双方に
                    // "px" suffix が付く。
                    unit = "px",
                    pickerTitle = "サイズを選択",
                )
            }

            // 6. TimePickerCell セクション
            Section(header = "TimePickerCell") {
                TimePickerCell(
                    title = "アラーム",
                    time = alarmTime.tracked(
                        title = "アラーム",
                        display = { it.format(TIME_FORMATTER) },
                        onEvent = record,
                    ),
                    format = "HH:mm",
                    pickerTitle = "アラーム時刻",
                )
                // `is24Hour = false` を指定すると選択面は午前／午後を含む 3 系列になる (並び順は端末 Locale の時刻表記に従う)。
                // `format` は行の表示にだけ効き、選択面の時制には関与しない。
                TimePickerCell(
                    title = "就寝",
                    time = bedTime.tracked(
                        title = "就寝",
                        display = { it.format(TIME_12H_FORMATTER) },
                        onEvent = record,
                    ),
                    format = "h:mm a",
                    is24Hour = false,
                    pickerTitle = "就寝時刻",
                )
            }

            // 7. DatePickerCell セクション（ホイール形式）
            Section(
                header = "DatePickerCell（ホイール）",
                footer = "ホイール形式で日付を選択するデモ。",
            ) {
                DatePickerCell(
                    title = "誕生日",
                    date = birthday.tracked(
                        title = "誕生日",
                        display = { it.format(DATE_FORMATTER) },
                        onEvent = record,
                    ),
                    format = "yyyy/MM/dd",
                    minDate = LocalDate.of(1900, 1, 1),
                    maxDate = today,
                    pickerTitle = "誕生日",
                    // iOS の `.wheels` に視覚的に対応する形式。
                    uiStyle = DatePickerUIStyle.Spinner,
                    // iOS と同じ `todayText` のオプトイン。指定時のみ選択面に「今日」へ
                    // ジャンプする操作が出る（Spinner では 3 連ホイールを今日へ動かす）。
                    todayText = "今日",
                )
            }

            // 8. DatePickerCell セクション（カレンダー形式）
            Section(
                header = "DatePickerCell（カレンダー）",
                footer = "カレンダー形式で日付を選択するデモ。",
            ) {
                DatePickerCell(
                    title = "予約日",
                    date = reservation.tracked(
                        title = "予約日",
                        display = { it.format(DATE_FORMATTER) },
                        onEvent = record,
                    ),
                    format = "yyyy/MM/dd",
                    // 範囲外の日付が同じ月の表示内で無効として見えるよう、月の途中で範囲を切る
                    // （カレンダーは前後の月の日を描画しないため）。
                    minDate = LocalDate.of(2026, 6, 1),
                    maxDate = LocalDate.of(2026, 6, 20),
                    pickerTitle = "予約日を選択",
                    // iOS の `.calendar` に視覚的に対応する形式。
                    uiStyle = DatePickerUIStyle.Material,
                    // Spinner モードと同じ `todayText` のオプトイン。指定時のみカレンダーの
                    // ボタン行に「今日」へ移動する操作が出る。
                    todayText = "今日",
                )
            }

            // 9. EntryCell（下部配置）セクション — キーボード回避の検証用
            Section(
                header = "EntryCell（下部配置）",
                footer = "画面下半分に配置した EntryCell をフォーカスしたとき、キーボードに合わせてコンテンツがせり上がるかを確認するための検証用セクション。",
            ) {
                EntryCell(
                    title = "メモ",
                    text = memo.tracked(title = "メモ", onEvent = record),
                    placeholder = "下部配置の検証用",
                )
                EntryCell(
                    title = "署名",
                    text = signature.tracked(title = "署名", onEvent = record),
                    placeholder = "最下部の検証用",
                )
            }
        }
    }
}

// =============================================================================
// 直近イベント記録
// =============================================================================

/**
 * 値が実際に変化したときだけ直近イベントを通知する [MutableState] ラッパー。
 *
 * 受け付けられなかった操作（複数選択の上限超過など）では元の値がそのまま
 * 書き戻されるため、`newValue == 現在値` の場合は何も通知しない。
 * iOS 側 `InputCellsDemoView.tracked(_:title:display:)` と同じ役割を持つ。
 */
private class TrackedState<T>(
    private val source: MutableState<T>,
    private val title: String,
    private val display: (T) -> String,
    private val onEvent: (String) -> Unit,
) : MutableState<T> {

    override var value: T
        get() = source.value
        set(newValue) {
            if (newValue == source.value) return
            source.value = newValue
            onEvent("$title → ${display(newValue)}")
        }

    override fun component1(): T = value

    override fun component2(): (T) -> Unit = { newValue -> value = newValue }
}

/** [TrackedState] で包んだ [MutableState] を返す。 */
private fun <T> MutableState<T>.tracked(
    title: String,
    display: (T) -> String = { it.toString() },
    onEvent: (String) -> Unit,
): MutableState<T> = TrackedState(source = this, title = title, display = display, onEvent = onEvent)

/** パスワードの表示形式（Cell と同じマスク表現）。 */
private fun maskedText(value: String): String = "•".repeat(value.length)

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val TIME_12H_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

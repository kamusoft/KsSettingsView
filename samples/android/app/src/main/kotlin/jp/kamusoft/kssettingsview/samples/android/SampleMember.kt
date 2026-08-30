package jp.kamusoft.kssettingsview.samples.android

/**
 * 通知先の候補として並べる架空のメンバー。
 *
 * PickerCell の object 候補デモで、主表示に [name]、副表示に [role] を射影する。
 * [role] が `null` の要素は副表示を持たず、選択面では 1 行で描画される。
 */
data class SampleMember(
    /** 主表示に使う名前。 */
    val name: String,
    /** 副表示に使う役割。`null` なら副表示なし。 */
    val role: String?,
) {
    companion object {
        /**
         * 入力 Cell デモの PickerCell（object 候補）セクションが並べる候補。
         *
         * 副表示の長さがばらばらで、副表示を持たない要素も混ざる並びにしてある。
         */
        val notificationTargets: List<SampleMember> = listOf(
            SampleMember(name = "佐藤 花子", role = "プロダクトマネージャー"),
            SampleMember(
                name = "鈴木 一郎",
                role = "モバイルアプリ開発チーム / テックリード (iOS・Android 横断アーキテクチャ担当)",
            ),
            SampleMember(name = "高橋 次郎", role = "QA エンジニア"),
            SampleMember(name = "全体アナウンス", role = null),
            SampleMember(name = "田中 三郎", role = "デザイナー"),
        )
    }
}

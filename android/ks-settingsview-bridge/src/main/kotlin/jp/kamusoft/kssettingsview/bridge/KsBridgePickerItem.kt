package jp.kamusoft.kssettingsview.bridge

/**
 * `PickerCell` の候補 1 件（主表示 + 任意の副表示）を輸送する DTO。
 *
 * 表示射影は上位層が適用済みであり、Native 側で射影を解き直すことはない。[subText] が `null` の
 * 候補は副表示を持たず、選択面では 1 行構成で描画される。候補 1 件を 1 オブジェクトで運ぶため、
 * 主表示と副表示の件数がずれることが構造的に起こらない。
 *
 * @property text 主表示テキスト
 * @property subText 副表示テキスト（`null` は副表示なし）
 */
class KsBridgePickerItem @JvmOverloads constructor(
    val text: String,
    val subText: String? = null,
)

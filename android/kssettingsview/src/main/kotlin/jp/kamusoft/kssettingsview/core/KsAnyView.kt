package jp.kamusoft.kssettingsview.core

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable

/**
 * 装飾領域（Root H/F、Section H/F の `View` ケース）に任意 View を格納するための型消去ラッパ。
 *
 * `Compose` サブタイプは `@Composable () -> Unit` を保持し、`AndroidView` サブタイプは
 * `(Context) -> View` ファクトリを保持する二択 backing。
 *
 * # 等価性契約
 *
 * `KsAnyView` 自身は `equals` / `hashCode` を独自実装しない（`Any` のデフォルト＝参照同一性のまま）。
 * `@Composable` ラムダ・`(Context) -> View` ファクトリは値の等価性を意味のある形で比較できないため、
 * 差分検出（`SettingsRoot` / `Section` の `equals` / `hashCode` 計算）には参加させない。
 * 中身の更新は描画レイヤ（`ComposeView.setContent` の再呼び出し等）に委ねる。
 */
public sealed interface KsAnyView {

    /**
     * Jetpack Compose の `@Composable` ラムダを保持する `KsAnyView` サブタイプ。
     *
     * `ComposeView.setContent` 等で本 [content] を再呼び出しすることで描画される。
     *
     * @property content 描画する Composable コンテンツ
     */
    public class Compose(public val content: @Composable () -> Unit) : KsAnyView

    /**
     * Android プラットフォーム標準の `(Context) -> View` ファクトリを保持する `KsAnyView` サブタイプ。
     *
     * `factory` は描画タイミングで呼び出され、新規 `View` インスタンスを返す。
     *
     * @property factory `View` を生成するファクトリ関数
     */
    public class AndroidView(public val factory: (Context) -> View) : KsAnyView
}

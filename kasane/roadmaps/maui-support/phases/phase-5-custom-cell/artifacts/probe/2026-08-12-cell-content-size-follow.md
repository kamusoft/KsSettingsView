# probe: CustomCell に埋め込んだ platform view のサイズ変化に行高さが追従するか

実測日: 2026-08-12 / 対象 change: add-maui-custom-cell (tasks 1.1) / 実測者: ksn-implementer

## 問い

MAUI facade が CustomCell の content として埋め込む platform view は、accessory と同じ「自己計測
wrapper」(`KsAccessoryHostView`) である。この wrapper が**自分の計測を無効化しただけ**で、native の
行 (row) の高さが新しい必要サイズへ追従するか。追従しないなら、accessory の
`invalidateAccessoryMeasurement` と同型の「行を対象にした一過性の再計測通知」を native へ足す必要が
ある。

accessory では iOS が自動追従せず、対象限定の layout 無効化を native へ足した前例がある
(maui/ADR-0018)。cell は accessory と描画経路が違う (iOS は supplementary ではなく self-sizing の
`UICollectionViewListCell` + `UIHostingConfiguration`、Android は RecyclerView の行 + `ComposeView`)
ため、同じ結論になるとは限らない。

## 結論

**両 OS とも、追加の通知なしで行高さが伸縮の両方向に追従する。** 一過性の再計測通知は不要。

| OS | 埋め込み形 | 通知なしの追従 (伸長) | 通知なしの追従 (縮小) | 負の対照 |
|---|---|---|---|---|
| iOS | 既定の `UIViewRepresentable` | 60pt → 240pt | 240pt → 80pt | 追従しない (60pt のまま) |
| iOS | `sizeThatFits` でサイズ中継する `UIViewRepresentable` | 60pt → 240pt | 240pt → 80pt | 追従しない (60pt のまま) |
| Android | `AndroidView(factory = { view })` | 60px → 240px | 240px → 80px | 追従しない (60px のまま) |

- **負の対照** = 必要サイズだけを変え、計測無効化 (`invalidateIntrinsicContentSize()` /
  `requestLayout()`) を**出さない**場合。どちらの OS でも行高さは変わらなかった。したがって上の追従は
  レイアウトパスを回した副作用ではなく、wrapper が出した計測無効化の効果である
- iOS では埋め込んだ view の `intrinsicContentSize` の問い合わせ回数が変化後に 4 → 6 へ増えており、
  SwiftUI 側が実際に測り直している。Android では `onMeasure` の回数が 2 → 6 へ増えている
- iOS は representable が `sizeThatFits(_:uiView:context:)` を実装してもしなくても結果が同じだった。
  サイズ中継の実装は追従の必要条件ではない

## なぜ accessory と結論が分かれるか

accessory (iOS) が追従しなかったのは、supplementary element の自動 self-sizing が「内側 view の内在
サイズ無効化」を拾わないためだった。cell は `UICollectionViewListCell` の self-sizing 経路にあり、
`UIHostingConfiguration` のホスト view が Auto Layout で自分の高さを申告する。埋め込んだ view の
`invalidateIntrinsicContentSize()` はその連鎖を通って cell の再計測まで届く。

Android は accessory も行も同じ RecyclerView の行であり、`requestLayout()` が RecyclerView の再測定
まで届く点は変わらない (accessory 側で通知口を設けたのは iOS との契約対称のため)。

## 手法

使い捨てのテストを各 OS のテストターゲットへ一時的に置き、実描画で行の `frame.height` /
`itemView.height` を測った。実測後にテストは削除した。再現用の全文を同ディレクトリに残す。

- iOS: `ios/Tests/KsSettingsViewUITests/CustomCellEmbeddedPlatformViewProbeTests.swift`
  (再現用: [ios-CustomCellEmbeddedPlatformViewProbeTests.swift](ios-CustomCellEmbeddedPlatformViewProbeTests.swift))

  ```
  cd ios
  xcodebuild test -scheme KsSettingsView-Package \
    -destination 'platform=iOS Simulator,id=<iPhone 17 Pro / iOS 26.0.1>' \
    -only-testing:KsSettingsViewUITests/CustomCellEmbeddedPlatformViewProbeTests
  ```

  実行結果: `Executed 6 tests, with 0 failures`

- Android: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellEmbeddedPlatformViewProbeTest.kt`
  (再現用: [android-CustomCellEmbeddedPlatformViewProbeTest.kt](android-CustomCellEmbeddedPlatformViewProbeTest.kt))

  ```
  cd android
  ./gradlew :ks-settingsview-ui:testDebugUnitTest \
    --tests '*CustomCellEmbeddedPlatformViewProbeTest*' --rerun-tasks -i
  ```

  実行結果: `BUILD SUCCESSFUL` (2 tests)

### 出力ログ (生値)

```
[PROBE] plain/no-notify:        before=60.0 after=240.0 shrunk=80.0 intrinsicQuery=4->6 invalidateCount=2
[PROBE] relay/no-notify:        before=60.0 after=240.0 shrunk=80.0 intrinsicQuery=4->6 invalidateCount=2
[PROBE] plain/invalidateItems:  before=60.0 after=240.0 shrunk=80.0 intrinsicQuery=4->6 invalidateCount=2
[PROBE] relay/invalidateItems:  before=60.0 after=240.0 shrunk=80.0 intrinsicQuery=4->6 invalidateCount=2
[PROBE] relay/silent:           before=60.0 after=60.0
[PROBE] swiftui-control:        before=60.0 after=240.0

[PROBE] android/no-notify:      before=60 after=240 shrunk=80 measureCount=2->6 requestCount=2
[PROBE] android/silent:         before=60 after=60  measureCount=2->2
```

`swiftui-control` は測定系が生きていることの確認 (純 SwiftUI の content が自分でサイズを変える経路)。
`invalidateItems` は行対象の layout 無効化を明示的に足した場合で、通知なしと同じ結果になった
(= 通知を足しても壊れないが、足す必要もない)。

## 前提と残るリスク

probe は wrapper 本体ではなく**同じ計測契約を持つ代役**を埋め込んで測った (iOS:
`intrinsicContentSize` を override し変化時に `invalidateIntrinsicContentSize()` + `setNeedsLayout()`
を呼ぶ `UIView` / Android: `onMeasure` で必要高さを返し変化時に `requestLayout()` を呼ぶ
`ViewGroup`)。`KsAccessoryHostView` が実際に出す計測無効化の手順はこれと同一だが、以下は probe の
射程外であり、後続で確かめる。

- **実 wrapper 固有の計測キャッシュ**: iOS の wrapper 基底 (`MauiView`) は `SizeThatFits` の結果を
  キャッシュする。probe の代役はキャッシュを持たない。埋め込み実装 (グループ3) の
  「同一トークンで再バインドしてもインスタンスが安定する」テストと、E2E のサイズ変化検証 (タスク 7.1)
  で、実 wrapper でも同じ追従になることを確認する
- **スクロール中・リサイクル中の追従**: probe は 1 行のみ・スクロールなし。RecyclerView は
  レイアウト計算中の子からの `requestLayout()` を抑止する経路を持つため、多数行 + スクロールでの
  挙動はタスク 7.2 のスクロール検証で見る
- **実機**: iOS は Simulator、Android は Robolectric での測定。実機での見え方はタスク 7.1 の
  スクリーンショットで残す

## 判断

tasks 1.2 の分岐 (native への一過性再計測通知の追加) は**採らない**。native (iOS / Android) の Core・
UI・Bridge の公開 API 変更は発生せず、proposal Non-Goals の例外条項は使わない。deviation.md への
記録も不要。

派生する論点として、実体化 seam (`IKsViewMaterializer.Materialize(view, measureInvalidated)`) が要求
する `measureInvalidated` コールバックが、cell content では**届け先を持たない**ことになる。accessory
では合体して `invalidateAccessoryMeasurement` を呼ぶが、cell では呼ぶ先が無い。この扱い (no-op を渡す
か、合体だけして捨てるか) は facade 実装 (グループ2) の判断に委ねる。

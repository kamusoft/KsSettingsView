# 検出力確認 (ミューテーション) の記録: perf-android-customcell-composition-reuse

**日付**: 2026-08-16
**対象タスク**: tasks 2.7 (検出力確認)
**実施者**: 実装ワーカー (レビュー指摘 review-002 の修正サイクル内で、提出コードに対して全変異を再実行)

新設テストが機構を実際に固定しているか (トートロジーでないか) を、実装へ一時的な変異を入れて確認した記録。
各変異は 1 つずつ適用し、確認後ただちに戻している。

測定対象は下記の SHA-1 を持つソース (measure policy と高さ確保の修正を含む提出コード) である。

| ファイル | 測定時の SHA-1 |
|---|---|
| `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt` | `404ca3bfa0ea6e02502cc3e7f551475e9d0a9608` |
| `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ComposeCellViewHolder.kt` | `937f71d76f5bae3301b1b485d90be6b5089e9c7a` |
| `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellPooledRebindMeasureTest.kt` | `b634f1e07e17f7c6ea60dbcb4c4287f33d0bf53a` |

## 実行手順

各変異について、以下を実行した。

```
cd android
./gradlew :ks-settingsview-ui:testDebugUnitTest --tests '*CustomCell*' \
          :ks-settingsview-bridge:testDebugUnitTest --tests '*KsBridgeCustomCell*'
```

(e) のみ、対象テストクラスが単独実行前提のため次で確認した。

```
cd android
./gradlew :ks-settingsview-ui:testDebugUnitTest --tests '*CustomCellBuilderReleaseTest*'
```

変異なしの状態では、いずれのコマンドも BUILD SUCCESSFUL (ui 52 件 / bridge 22 件・失敗 0) になる。
落ちたテストと失敗メッセージは結果 XML (`build/test-results/testDebugUnitTest/TEST-*.xml`) の
`<failure>` から拾い、争点のアサーションのものであることを 1 件ずつ確認している。

## 結果

| 変異 | 変異の内容 (対象ファイル) | 落ちたテスト (失敗メッセージ) |
|---|---|---|
| (a) | `ComposeCellViewHolder.kt` の破棄戦略を `DisposeOnDetachedFromWindowOrReleasedFromPool` → `DisposeOnDetachedFromWindow` | 計 7 件。`CustomCellRecycleTest > 行がプールへ入っても Composition は生存しプール放逐で破棄される` (`プール滞在中の行の Composition が破棄されている`) / `CustomCellRecycleTest > itemViewCache 経由の再表示では content の状態と購読が維持される` (`cache 経由の再表示で content の状態が失われている`) / `CustomCellRecycleTest > 同一ラップ関数 builder 間で埋め込み View が再利用される` (`埋め込みの factory が再実行されている expected:<1> but was:<2>`) / `KsBridgeCustomCellDeactivateTest > リサイクルを挟んだ再表示で同一 platform view が再親付けされる` (`プール滞在中に Composition が破棄されている`) / `CustomCellPooledRebindMeasureTest` の 3 件 (前提アサーション `Composition が破棄されている` `この時点で content が活性に戻っている` で停止) |
| (b) | `CustomCellViewHolder.kt` の `ReusableContent(key = contentKey.value)` を `key(contentKey.value)` へ置換 | `CustomCellRecycleTest > 同一ラップ関数 builder 間で埋め込み View が再利用される` (`埋め込みの factory が再実行されている expected:<1> but was:<2>`) の 1 件 |
| (c) | `CustomCellViewHolder.bind` の `contentKey.value = cell.id` を固定値 (`"fixed"`) へ変更 | `CustomCellRecycleTest > 間に再 composition を挟まない再 bind でも remember が持ち越されない` (`B の content が初期状態で現れていない`) の 1 件 |
| (d) | `CustomCellViewHolder.reset` から `isContentActive.value = false` を削除 | 計 3 件。`CustomCellRecycleTest > 同一ラップ関数 builder 間で埋め込み View が再利用される` (`埋め込みの factory が再実行されている expected:<1> but was:<2>`) / `CustomCellPooledRebindMeasureTest > 固定高さの行をプールから再 bind した直後は新しい Cell の高さが確保される` (`確保された高さが新しい Cell の高さになっていない expected:<72> but was:<120>`) / `CustomCellPooledRebindMeasureTest > 可変高さの行をプールから再 bind した直後は確保される高さが最低高まで縮まない` (`確保された高さが最低高まで縮んでいる expected:<200> but was:<60>`)。非活性化されない行は再 bind 直後も旧 composition のまま測られ、確保される高さが前の Cell のものになる |
| (e) | `CustomCellViewHolder.reset` から `contentState.value = EMPTY_CELL_CONTENT` を削除 | `CustomCellBuilderReleaseTest > Composition 破棄後は builder が参照するものが解放可能になる` (`builder が参照する対象が回収されない`) の 1 件 |
| (g) | measure policy の `if (!isContentComposed.value)` を `if (false)` にして、非活性でも常に子を測る | `CustomCellPooledRebindMeasureTest` の 3 件すべて。いずれも失敗理由は `java.lang.IllegalArgumentException: measure is called on a deactivated node` であり、実機で観測された FATAL と同一の例外を再現している (テストが実機の症状そのものを固定していることの確認) |
| (h) | 非活性中の確保高さから `isFixedHeight` の分岐を外し、常に `maxOf(minHeight, lastContentHeightPx)` にする | `CustomCellPooledRebindMeasureTest > 固定高さの行をプールから再 bind した直後は新しい Cell の高さが確保される` (`確保された高さが新しい Cell の高さになっていない expected:<72> but was:<120>`) の 1 件 |
| (i) | 非活性中の確保高さを `minHeight` 固定にし、直前に測った行高さを使わない | `CustomCellPooledRebindMeasureTest > 可変高さの行をプールから再 bind した直後は確保される高さが最低高まで縮まない` (`確保された高さが最低高まで縮んでいる expected:<200> but was:<60>`) の 1 件 |
| (j) | 行高さの記録から `isContentActive` の条件を外し、非活性化の要求後の測定も下限に取り込む | **落ちない (検出できず)**。下記「(j) が検出できない理由」を参照 |

(b) (c) (e) (g) (h) (i) では、テスト内の前提アサーション (「前提: 〜」) は通過し、争点のアサーションだけが落ちた。
(a) と (d) は機構そのもの (プール滞在中の Composition 生存 / 非活性化) を壊す変異であり、
`CustomCellPooledRebindMeasureTest` にとってはテストが組み立てる経路の前提が崩れる。前提で落ちること自体が
「この 3 件がプール由来の再 bind という経路を実際に通っている」ことの裏付けになるため、争点外の失敗として扱う。

### 高さ確保の変異 ((h) / (i)) を分けた理由

非活性中に確保する高さは、行高さの決まり方で正しい値が変わる。

- 固定高さの行 (`Theme.hasUnevenRows == false`) は解決値がそのまま行高さなので、確保値は新しい Cell の解決値でなければならない → (h) で検出
- 可変高さの行では解決値は最低高でしかなく、content の自然高は非活性の間は測れない。直前に測った行高さを下限に使って行の縮みを避ける → (i) で検出

片方だけでは、もう片方へ退化させる変異が素通りする ((h) は可変高さのテストを、(i) は固定高さのテストを落とさない)。

### (j) が検出できない理由と、この条件を残している根拠

`isContentActive` の条件は、`reset()` で content を空へ差し替えた後の測定が行高さの下限を上書きしないための
防御である。この条件を外しても現行のテストは落ちない。「非活性化を要求済みだが composition にはまだ届いて
おらず、しかも空の content で測られる」という時間窓を、テストから決め打ちで作れないためである
(`reset()` 直後に測ると、composition はまだ旧 content のノードを保持しているため、測定値は旧 content の
高さになる)。

この条件は思いつきではなく、**実際に観測した失敗への対処**である。テストヘルパが描画パス経由で Compose の
measure を走らせていた版では、モジュール全件実行 (`:ks-settingsview-ui:testDebugUnitTest --rerun-tasks`) で
`可変高さの行をプールから再 bind した直後は確保される高さが最低高まで縮まない` が
`expected:<200> but was:<60>` で落ちた。フレームの中で composition の適用と effect の破棄の間に測定が
挟まると、空の content の高さが下限として記録され得ることを示している。その後ヘルパを測定キャッシュを外す
方式へ変えたため同じ時間窓を踏まなくなり、変異としては再現できなくなった。

したがって (j) は「検出力なし」として記録する。条件を外しても現行テストは緑になるが、上記の実観測がある
ため実装からは外さない。

### (e) を追加した理由

`CustomCellBuilderReleaseTest` は当初、判定の間に対象 ViewHolder への強参照を持っていなかった。
その形では ViewHolder ごと回収可能になり得るため、`reset()` の content state 切り離しを削っても
弱参照は回収され、変異を検出できない可能性があった。判定の間 ViewHolder を強参照で保持し
(`Reference.reachabilityFence` で判定終了まで到達可能に保つ)、Composition が破棄済みであることを
前提アサーションで固定したうえで、(e) の変異で確実に落ちることを確認した。

### (c) の適用範囲についての注記

(c) は `別 Cell への再 bind では remember が持ち越されず DisposableEffect が dispose される` では
検出されない。プール経由の経路は非活性化・再活性化が隔離を担うため、同一性キーに依存しないため。
同一フレーム内の直接 bind を通す `間に再 composition を挟まない再 bind でも remember が持ち越されない`
が、同一性キーを固定する唯一のゲートになっている。

## 原状復帰の確認

変異の適用前に対象ファイルの `shasum` を記録し、各変異の確認後に同じ値へ戻ることを確認した
(下表は全変異の実行後に再確認した値であり、上の「測定対象」と一致する)。

```
shasum android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt
shasum android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ComposeCellViewHolder.kt
```

| ファイル | 変異前 / 復帰後の SHA-1 |
|---|---|
| `CustomCellViewHolder.kt` | `404ca3bfa0ea6e02502cc3e7f551475e9d0a9608` |
| `ComposeCellViewHolder.kt` | `937f71d76f5bae3301b1b485d90be6b5089e9c7a` |

復帰後に全モジュール全件テスト (`cd android && ./gradlew test --rerun-tasks`) を実行し、変異が残っていない
ことをあわせて確認している (件数は下記)。

## 全件テストの実行件数

`cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。
`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の集計で debug 1174 / release 1174、
failures 0 / errors 0 (test-execution 規約)。

## 追記 (2026-08-16): review-003 Minor 対応後の SHA 更新

本記録の採取後、review-003 の Minor 指摘 (lastContentHeightPx の条件の説明が実機構と食い違う) への対応で
`CustomCellViewHolder.kt` と `CustomCellPooledRebindMeasureTest.kt` のコメントのみを修正した。現ツリーの SHA は
`CustomCellViewHolder.kt` = `2b96cee1928dd59b4198ba2d5a80057c93f1ad03`、
`CustomCellPooledRebindMeasureTest.kt` = `6631f21f1cf771f651cdaf4b5e601b45f12be0a1` へ変わっている
(`ComposeCellViewHolder.kt` は不変)。差分はコメントに閉じるため、本記録の変異結果は現ツリーに対して有効
(review-003.md / verify-003.md の追記照合で確認済み)。

さらに蒸留時 (2026-08-16) の ADR 参照更新 (core/ADR-0015 → core/ADR-0022 の supersede に伴うコメントのみの書き換え) で
`CustomCellViewHolder.kt` = `736c67131d387bfc7bbe74a70f474682955e4f23` へ変わっている
(`ComposeCellViewHolder.kt` / `CustomCellPooledRebindMeasureTest.kt` は不変。差分はコメントに閉じ、変異結果に影響なし)。

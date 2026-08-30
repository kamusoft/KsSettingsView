# レビュー結果: fix-compose-dsl-double-update-flaky-test (002 回目)

**日付**: 2026-08-22
**判定**: APPROVED

## サマリー

review-001 の必須指摘 (呼び出し側コメントの追随漏れ) は解消されている。呼び出し側 (`:224-228`) と helper docstring (`:355-369`) は同じ機構 — 「`AsyncListDiffer` の post 前は main looper のキューが空なので `waitForIdle()` が即座に戻る」— を矛盾なく述べる形になり、`（flaky の原因）` の断定も落ちた。Suggestion ①② の採用 (`yield` → `sleep(1)`、`current` の一本化) も実装として正しく、回帰検出力はミューテーション実測で再確認した (対象 1 件のみが helper の `fail()` 経由で 5.064 秒後に失敗)。

残る指摘は 1 件のみで、コード正当性には影響しない。新 docstring が `Thread.yield()` の不十分さを一般命題として書いた結果、同じ形の待機ループを `Thread.yield()` で実装している共有ヘルパ群 (`KsSettingsViewTestSupport.kt` ほか計 6 箇所) の説明コメントと、リポジトリ規模で言っていることが食い違うようになった。共通化がスコープ外である以上この diff で解消する義務はないが、記述の一般性を落とすか追跡課題として残すことを勧める。優先度は低く、本判定を保留する理由にはしない。

## 検証結果

### テスト全件 (規約: `kasane/concepts/cross/conventions/test-execution.md` Android 節)

```
cd android && ./gradlew test --rerun-tasks   → BUILD SUCCESSFUL
```

`android/*/build/test-results/*/TEST-*.xml` の属性合計:

| モジュール / タスク | tests | failures | errors | skipped |
|---|---|---|---|---|
| ks-settingsview-bridge / testDebugUnitTest | 161 | 0 | 0 | 0 |
| ks-settingsview-bridge / testReleaseUnitTest | 161 | 0 | 0 | 0 |
| ks-settingsview-compose / testDebugUnitTest | 111 | 0 | 0 | 0 |
| ks-settingsview-compose / testReleaseUnitTest | 111 | 0 | 0 | 0 |
| ks-settingsview-core / testDebugUnitTest | 80 | 0 | 0 | 0 |
| ks-settingsview-core / testReleaseUnitTest | 80 | 0 | 0 | 0 |
| ks-settingsview-ui / testDebugUnitTest | 909 | 0 | 0 | 0 |
| ks-settingsview-ui / testReleaseUnitTest | 909 | 0 | 0 | 0 |
| **合計** | **2522** | **0** | **0** | **0** |

規約記載の実測値 (1261 件 × 2 variant = 2522 件) と一致。件数の欠落なし。

この green は「修正が正しい根拠」としては採用していない (本 flaky は再現率が低く、調査時点で 11 回連続 green だった)。判定根拠は以下のコード上の性質とミューテーション実測。

### 回帰検出力の再確認 (lessons/code-review L-001)

2 周目の変更 (`yield` → `sleep`、`current` の一本化) が review-001 で確認済みの検出力を損なっていないかを、review-001 と同じ手法で実測した。

- 一時ミューテーション: `KsSettingsViewComposable.kt:182` の `SettingsRootDiff.InsertCell` を `diff.index == 0` のときだけ store へ適用するよう改変 (= 2 個目以降の Cell 追加が反映されない = flaky の失敗形と同一症状の固定化)
- 結果: `111 tests completed, 1 failed`。落ちたのは対象テスト 1 件のみで、残り 110 件は通過
- 失敗の経路と内容:

```
java.lang.AssertionError: itemCount が 5000 ms 以内に 3 へ収束しなかった（現在の itemCount: 2）
  at org.junit.Assert.fail(Assert.java:89)
  at ...KsSettingsViewComposeTest.waitForAdapterItemCount(KsSettingsViewComposeTest.kt:382)
  at ...KsSettingsViewComposeTest.DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される(...kt:229)
time = 5.064
```

確定した点:

1. **fail 経路は 2 周目の変更後も働く** — 収束しない入力で helper 内 `fail()` (`:382`) に到達する。`sleep(1)` を挟んでもハングせず、黙って return する経路も無い
2. **deadline が実際に打ち切っている** — 5.064 秒 ≒ `timeoutMillis` 5_000。`Thread.sleep(1)` を数千回繰り返す経路でも deadline 判定は正常に成立した (= Robolectric の仮想クロックではなく `System.nanoTime()` で測っていることが実挙動としても確認できた)
3. **Suggestion ② の効果が実物で見える** — メッセージの「現在の itemCount: 2」は、条件判定に使ったのと同一の読み取り値。再取得由来の自己矛盾メッセージや `recyclerViewItemCountForTest()` 内の別 `AssertionError` による覆い隠しは、経路として消えている

一時変更は backup から復帰し、shasum 一致 (`4dbda21b9dd3bee492d1ef3d2393b7fd53077878`、review-001 記録値と同一) で原状復帰を確認済み。復帰後に `:ks-settingsview-compose:testDebugUnitTest` / `testReleaseUnitTest` を `--rerun-tasks` で回し直して BUILD SUCCESSFUL を確認し、ミューテーション由来の失敗レポートが build 配下に残らないようにした。最終状態の `git status --short` は本変更の 1 ファイル + 未追跡の `review-001.md` のみ。

### `Thread.sleep(1)` が新たな問題を生んでいないかの確認

依頼の確認事項 2 に対する回答。いずれも問題なしと判断する。

- **Robolectric `ShadowLooper` との相互作用** — `Thread.sleep` は実時間を進めるだけで Robolectric の仮想クロック (`SystemClock`) は進めない。待っている対象は `AsyncListDiffer` の**実バックグラウンドスレッド**による差分計算であり、遅延なしの `Handler.post` で main looper に積まれる。遅延メッセージ (`postDelayed`) を待つ経路は含まれないため、仮想クロックが進まないことは待機の妨げにならない。この性質は `Thread.yield()` 版でも同じであり、本変更で新たに生じた制約ではない
- **Compose test runtime との相互作用** — `composeRule.waitForIdle()` は sleep の前に戻り切っており、ループが何らかのロックを保持したまま sleep する構造にはなっていない。ミューテーション実測で 5 秒フル回転させてもデッドロック・例外は発生しなかった
- **成功経路の所要時間** — 悪化なし。`sleep` は「収束チェック → deadline チェック」の**後ろ**にあるため、1 周目で収束する通常経路では 1 度も sleep しない。実測でも本 helper を使う 2 テストの所要時間は 0.071〜0.099 秒 (debug/release 4 件) で、sleep が支配的になっている兆候はない
- **`InterruptedException` の扱い** — `Thread.sleep` は Kotlin では検査例外にならないためコンパイルは通り、テストスレッドが割り込まれた場合は `InterruptedException` がそのまま helper 外へ伝播する (KDoc の `@throws` は `AssertionError` しか挙げていない)。ただし JUnit のテストスレッドを通常運用で割り込むものは無く、Gradle の worker タイムアウト等で割り込まれる場合はテスト自体が中断されている。実害のある経路ではないため指摘には挙げない
- **`Thread.sleep` 使用そのものの是非** — kotlin-impl-skill の「`Thread.sleep` を使わない」は Coroutines を持つプロダクション/非同期コードに向けた規律。ここは `AsyncListDiffer` の素の `ExecutorService` をブロッキングで待つ JUnit4 テストであり、`suspend` / `runTest` の適用対象ではない。同テストスイート内にも先例がある (`KsBridgeLifecycleTest.kt:231`、`KsBridgeTestHost.kt:99`)

### 静的検査

- `python3 scripts/comment-policy-lint.py android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt` → 禁止 0 件
- 本プロジェクトの Gradle には ktlint / detekt が組まれていない (`build.gradle.kts` / `libs.versions.toml` に定義なし) ため実行対象なし

### 実機視覚証跡 (lessons/process L-003) の適用判断

**対象外**。L-003 の対象は利用者の目と操作に見える変更。本変更は `src/test` のみで、`src/main` はミューテーション前後を通じて shasum 一致 (バイト一致) を確認している。

## 指摘事項

### [🟡 Minor / 優先度: 低] `Thread.yield()` を否定する一般命題が、`yield` のままの共有ヘルパ群と食い違う

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt:363`

**問題点**:

新 docstring は待機の刻み方について次の一般命題を置いた。

> `Thread.yield()` は OS へのヒントに過ぎず、CPU が飽和した状況では譲れる保証がない。

記述自体は Java の `Thread.yield()` の仕様として正しい。問題は、同じリポジトリの同型の待機ループが `Thread.yield()` のままで、しかもそこには逆向きの意図が断言として書かれていることである。

| 箇所 | 刻み方 | 添えられた説明 |
|---|---|---|
| `KsSettingsViewComposeTest.kt:387` (本変更) | `Thread.sleep(1)` | 「`yield()` は…譲れる保証がない」 |
| `ks-settingsview-ui/.../KsSettingsViewTestSupport.kt:58` (`awaitConvergence`) | `Thread.yield()` | 「バックグラウンドの差分計算が進むよう、待つ間は CPU を他スレッドへ譲る。」 |
| `ks-settingsview-ui/.../KsSettingsViewTestSupport.kt:93` (`awaitDifferCommit`) | `Thread.yield()` | 同上 |
| `ks-settingsview-compose/.../DSLAccessoryVisibilityRenderingTest.kt:110` (`awaitRows`) | `Thread.yield()` | — |
| `ks-settingsview-ui/.../CustomCellRecycleTest.kt:389` | `Thread.yield()` | — |
| `ks-settingsview-ui/.../CustomCellBuilderReleaseTest.kt:162` | `Thread.yield()` | — |

`KsSettingsViewTestSupport.kt` は待機ヘルパの共通化先として設けられた共有モジュールであり、`exploration.md` の「関連ファイル」にも待機ヘルパの正として挙がっている。その共有ヘルパを読む人は「譲る」という断言を読み、本変更のファイルを読む人は「譲れる保証がない」という断言を読む。互いへのポインタは無い。

**これは本 change が潰した乖離と同じ型が、ファイル内からリポジトリ横断へ移った形である。** 加えて内容面でも、命題が正しいなら共有ヘルパを使う ui モジュール側 (909 件 × 2 variant) が同じ潜在的不安定さを抱えていることになり、その事実がどこにも記録されない。

なお `Thread.sleep(1)` への変更自体は妥当であり、差し戻すべきではない (deadline が真の担保である点は 6 箇所すべてで共通なので、どの実装も現時点で壊れてはいない)。

**推奨修正** (どちらか):

1. **この diff 内で閉じる (低コスト)**: docstring から `Thread.yield()` への一般命題を落とし、この helper が何をするかだけを述べる。例:

   ```
   * 上限は時間 ([timeoutMillis]) で置き、1 周ごとに短く sleep して、バックグラウンドスレッドが
   * 差分計算を進める余地を実際に空ける。
   ```

2. **追跡課題として残す**: 記述はこのままとし、共有ヘルパ (`awaitConvergence` / `awaitDifferCommit` ほか) の刻み方を揃えるか「揃えない理由」を共有ヘルパ側に書く作業を別 change として起票する。共通化 (testFixtures 化) の判断と同時に扱うのが自然

いずれを選んでも本判定 (APPROVED) は変わらない。

## 確認して問題なかった観点

- **review-001 必須指摘の解消** — 呼び出し側 (`:224-228`) は「post 前は main looper のキューが空で `composeRule.waitForIdle()` が即座に戻り、itemCount が古いまま観測され得る」となり、helper docstring (`:359-363`) と同一機構を述べる。`（flaky の原因）` の断定も削除済み。同一ファイル内の排他的な 2 説は解消した
- **新たな docstring / 実装の乖離なし** — 新 docstring を 1 文ずつ実装と突き合わせた。「上限は時間で置く」→ `deadline`、「1 周ごとに短く sleep」→ ループ末尾の `Thread.sleep(1)` (収束時・時間切れ時には実行されないのが正しい挙動)、「その時点の itemCount を載せて明示的に失敗させる」→ `fail()` のメッセージ。いずれも一致する。元コードの「最大 50 回、1 回 1ms 待機」のような、実装に存在しない待機を主張する記述は無い
- **`flushIdle()` の docstring との整合** — 「main looper には未処理メッセージが残るため idle する」(post 済みの局面) と、新 helper の「post 前はキューが空」(post 前の局面) は別の時点を述べており矛盾しない。review-001 の判断を維持する
- **Suggestion ② の実装が正しい** — `current` はループ内で 1 回だけ読み、条件判定と fail メッセージの双方に使う。値の食い違いも、`recyclerViewItemCountForTest()` 内部の `assertTrue(frame.childCount > 0)` (`:483`) が 2 度目の呼び出しで別例外を投げて timeout メッセージを覆い隠す経路も消えている
- **`assertTrue(frame.childCount > 0)` の位置関係が改善している** — 変更前は各周回の**先頭** (flush 前) で `recyclerViewItemCountForTest()` を呼んでいたため、View がまだ準備できていない局面で即座に AssertionError になり得た。変更後は `idle()` → `waitForIdle()` の後に読むため、この経路は狭くなっている (悪化ではない)
- **単調時計** — `System.nanoTime()` を使用。壁時計ではないため実行中の時刻補正で誤爆・無限化しない。共有ヘルパ (`awaitConvergence` 等) と同じ形
- **早期 return の抜け道なし / 無限ループしない** — ループは `idle()` → `waitForIdle()` → 読み取り → 一致判定 → deadline 判定 → sleep の順で、必ず 1 周分は looper と Compose runtime を流してから判定する。出口は収束か `fail()` の 2 経路のみ (ミューテーション実測で後者への到達を確認)
- **2 つ目の呼び出し (`:416`) への波及** — style 切替テストは変更前だと 50 回で未収束でも黙って通過し得たが、変更後はハードゲートになる。共有ヘルパ改善に伴う必然の波及であり、実際に green のまま通っている
- **import 配置** — `java.util.concurrent.TimeUnit` は他パッケージの後・alias import の前。IntelliJ Kotlin 既定のレイアウトどおりで、同モジュール `DSLAccessoryVisibilityRenderingTest.kt` とも一致
- **スコープ遵守・巻き込み変更なし** — `git status --short` は対象テストファイル 1 件のみ。`src/main` は shasum 一致で無変更、testFixtures 化などの構造的共通化にも手を付けていない
- **足場の凍結** — `exploration.md` は書き換えられていない
- **deviation** — 合意済みスコープとの差分は見当たらず、`deviation.md` 不在は妥当
- **再現手順が未確立であることの扱い** — `exploration.md` の未決の論点「flakiness の再現手順の確立」は解けていない (`kasane/concepts/cross/conventions/runtime-behavior-verification.md` の「実環境の観測で裏取りしてから修正に進む」に照らすと弱い)。ただし本修正は**原因仮説が外れていても危険側に倒れない**構造になっている: 新 helper は旧実装に対して「刺激」ではなく「時間」だけを足しており、取りこぼされた state 更新を出現させることはできない。仮に真因が実装側のタイミング穴だった場合、テストは通過するのではなく 5 秒後に決定的に失敗する (ミューテーション実測がまさにその挙動)。したがって「待機を緩めて実欠陥を覆い隠す」型のリスクは無く、再現手順の未確立は本判定を保留する理由にならないと判断した

## 参考所見 (本変更のスコープ外・修正指示ではない)

review-001 と同じものを引き続き観測しているため、失われないよう再掲する。

`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:169` の docstring に、変更提案識別子の裸参照 (`purify-core-extract-style-to-ui-layer`) と履歴記述 (「〜で `UpdateTheme` ケースは削除された」) が残っている。comment-policy の禁止類型 2 種に該当するが、`scripts/comment-policy-lint.py` はこのファイルを 0 件と報告する。本 diff の対象外なので指摘には挙げないが、lint の取りこぼしと既存債務の棚卸しの材料になる。

## アクションプラン

1. **[任意]** Minor: `KsSettingsViewComposeTest.kt:363` の `Thread.yield()` に関する一般命題を、この helper の挙動説明に絞る (推奨修正 1)。この diff 内で 1〜2 行の編集で閉じられる
2. **[任意 / 別途判断]** 共有待機ヘルパ (`KsSettingsViewTestSupport.kt` の `awaitConvergence` / `awaitDifferCommit`、および `awaitRows` ほか計 5 箇所) の刻み方を `sleep` に揃えるか、揃えない理由を共有ヘルパ側に記録する。共通化 (testFixtures 化) の判断と同じ場で扱うのが自然。本 change のスコープ外
3. **[任意 / 別途判断]** 参考所見の comment-policy 既存債務と lint の取りこぼしを、別途棚卸しの対象に積む

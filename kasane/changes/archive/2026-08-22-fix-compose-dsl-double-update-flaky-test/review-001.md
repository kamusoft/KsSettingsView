# レビュー結果: fix-compose-dsl-double-update-flaky-test (001 回目)

**日付**: 2026-08-21
**判定**: CHANGES_REQUESTED

## サマリー

待機ロジックそのものは正しい。反復回数上限から単調時計 (`System.nanoTime()`) ベースの deadline 条件待機への置き換えは意図どおりで、収束しない場合の `fail()` 到達はミューテーション実測で確認した (実装を壊すと 5.065 秒後に helper の `fail()` で確実に落ちる)。回帰検出力は失われておらず、むしろ強化されている。

一方で、本変更が潰そうとしていた「コメントと実装の乖離」が呼び出し側に残っている。`KsSettingsViewComposeTest.kt:225-227` の呼び出し側コメントは flaky の原因を「main looper のキューが空になっていない」と説明したままで、新しい helper docstring の「post 前は main looper のキューが空で `idle()` も `waitForIdle()` も即座に戻る」と同一ファイル内で真っ向から矛盾する。コード修正は不要で、コメント 4 行の書き直しのみ。

## 検証結果

### テスト全件 (規約: `kasane/concepts/cross/conventions/test-execution.md` Android 節)

```
cd android && ./gradlew test --rerun-tasks   → BUILD SUCCESSFUL in 5m 9s
```

`*/build/test-results/*/TEST-*.xml` の `tests` / `failures` 属性合計:

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

規約に記載の実測値 (1261 件 × 2 variant = 2522 件) と一致。件数の欠落なし。

依頼の注意書きどおり、この green を「修正が正しい根拠」としては採用していない。判定の根拠は下記のコード上の性質とミューテーション実測。

### 回帰検出力のミューテーション実測 (lessons/code-review.md L-001)

「待機を緩めただけで検出力を失っていないか」が争点なので、静的読解で止めず実測した。

- 一時ミューテーション: `KsSettingsViewComposable.kt:182` の `SettingsRootDiff.InsertCell` を `diff.index == 0` のときだけ適用するよう改変 (= 2 個目以降の Cell 追加が反映されない = flaky の失敗形と同一の症状を人工的に固定)
- 結果: `111 tests completed, 1 failed`。落ちたのは対象テスト 1 件のみで、他 110 件は通過 (= アサーションはトートロジーではなく、この症状を狙って検出している)
- 失敗の経路と内容:

```
java.lang.AssertionError: itemCount が 5000 ms 以内に 3 へ収束しなかった（現在の itemCount: 2）
  at org.junit.Assert.fail(Assert.java:89)
  at ...KsSettingsViewComposeTest.waitForAdapterItemCount(KsSettingsViewComposeTest.kt:380)
  at ...KsSettingsViewComposeTest.DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される(...kt:229)
time = 5.065
```

これで次の 3 点が確定した。

1. **fail 経路は確実に働く** — 収束しない入力で helper 内 `fail()` (`:380`) に到達する。黙って return する抜け道も、ハングして deadline 判定に到達しない経路も無い
2. **5.065 秒 ≒ timeoutMillis 5_000** — ループが最後まで回り切ってから失敗しており、deadline による打ち切りが実際に効いている
3. **メッセージが症状を切り分けられる** — 「現在の itemCount: 2」が出るため、「実装が壊れた」と「待機が足りない」を読み手が区別できる

一時変更は backup と shasum 一致 (`4dbda21b9dd3bee492d1ef3d2393b7fd53077878`) で原状復帰を確認済み。`git status --short` は本変更の 1 ファイルのみ。

### 静的検査

- `python3 scripts/comment-policy-lint.py <対象ファイル>` → 禁止 0 件
- リポジトリ全体 `--summary` → 669 ファイル / 禁止 0 件
- 本プロジェクトの Gradle には ktlint / detekt が組まれていないため実行対象なし

### 実機視覚証跡 (lessons/process.md L-003) の適用判断

**対象外と判断する**。L-003 の対象は「利用者の目と操作に見える変更」。本変更は `src/test` の待機ヘルパのみで、`src/main` は 1 バイトも変わっていない (上記 shasum で確認)。ミューテーション実測でも、製品コードを元に戻した状態でテストが green に復帰しており、製品挙動に触れていないことが裏付けられている。したがって A/B スクリーンショットの提出は要件にならない。

## 指摘事項

### [🟡 Minor / 優先度: 高] 呼び出し側コメントの追随漏れ — flaky の原因説明が helper docstring と矛盾している

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt:224-228`

**問題点**:

呼び出し側コメントは最終行 (`:228`) だけが「時間で区切った上限の中で」に書き換えられ、その手前の原因説明が旧のまま残っている。

- 呼び出し側 (`:226-227`): 「`composeRule.waitForIdle()` だけでは main looper の**メッセージキューが空になっていない**ことがあり、稀に itemCount が古いままになる（**flaky の原因**）」
- helper docstring (`:360-361`): 「post 前は main looper の**キューが空で** `idle()` も `composeRule.waitForIdle()` も即座に戻るため、待機の上限を反復回数で置くと差分計算の完了を待たないまま回数を使い切ってしまう」

同一ファイルを上から読む人 (人間・エージェント双方) に、同じ現象の機構として「キューが空でない」と「キューが空」という排他的な 2 説が提示される。comment-policy の最低条件「そのファイルだけを読んでいる人にとって意味が通ること」を満たしていない。

さらに `（flaky の原因）` という断定が事実として古い。「キューが空でない」ケースは、この呼び出しの直前にある 3 回の `flushIdle()` (`:209` / `:213` / `:217`) が既に吸収しており、それでもなお発生したのが今回の flaky である。合意済みスコープの結論 (反復回数ベースの待機が差分計算の完了前に回数を使い切る) とも食い違う。

**これは本変更の核心と同じ型の乖離である。** 元コードの「最大 50 回、1 回 1ms 待機」という嘘のコメントが待機不備を覆い隠したのが本 change の出発点であり、その修正コミットが、修正した当の呼び出し地点に別の誤った原因説明を残す形になっている。

**推奨修正**:

`:225-227` を現在の機構に書き直すか、呼び出し側からは機構の説明を落として意図だけを残し、機構は helper docstring に一元化する。後者の例:

```kotlin
// AsyncListDiffer の background diff → main looper post による反映完了を条件待機する。
// 期待値（3）へ収束するまで main looper と Compose runtime を流し、収束しなければ失敗させる。
waitForAdapterItemCount(layout, expected = 3)
```

なお `flushIdle()` の docstring (`:342-347`) の「Robolectric の main looper には未処理メッセージが残るため」は、`flushIdle` 自身の役割 (best-effort な掃き出し) の説明としては成立しており、書き換え不要と判断する。

---

### [🔵 Suggestion] 失敗経路が 5 秒間 busy-spin する — flaky が出た CPU 飽和条件では譲れていない可能性

**該当箇所**: `KsSettingsViewComposeTest.kt:385` (`Thread.yield()`)

**問題点**:

`Thread.yield()` は JVM/OS へのヒントにすぎず、実行権を譲る保証はない。今回の flaky が観測されたのは全モジュール `--rerun-tasks` の並列実行時、つまり CPU が飽和している条件であり、そこでは spin ループが待っている当の `AsyncListDiffer` バックグラウンドスレッドと CPU を奪い合う。実測でも失敗経路は 5.065 秒フルに回り切っており、その間 1 コアを占有している。

docstring の「1 周ごとにバックグラウンドスレッドへ実行機会を譲る」は `Thread.yield()` の意図としては正しいが、実効的な保証は deadline 側にある、という点までは読み取れない。

正しさには影響しない (5 秒の deadline が本当の担保) ため Suggestion に留める。

**推奨修正**:

`Thread.yield()` を `Thread.sleep(1)` もしくは `LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1))` に置き換えると、実際にコアを明け渡せる。

ただし本 helper は同モジュールの `DSLAccessoryVisibilityRenderingTest.kt:98-112` の `awaitRows` と意図的に同型であり、片方だけ変えると乖離する。採用するなら両方揃えるか、testFixtures 化による共通化 (今回スコープ外) の時点でまとめて決めるのが妥当。

---

### [🔵 Suggestion] fail メッセージ用に itemCount を再取得している

**該当箇所**: `KsSettingsViewComposeTest.kt:378` と `:382`

**問題点**:

`:378` で比較した値と、`:382` のメッセージ用に読む値が別の読み取りになっている。

1. この 2 行の間に収束すると「3 へ収束しなかった（現在の itemCount: 3）」という自己矛盾したメッセージが出る
2. `recyclerViewItemCountForTest()` は内部に `assertTrue(frame.childCount > 0)` (`:481`) を持つため、2 回目の呼び出しが別の `AssertionError` を投げ、本来出したい timeout メッセージを覆い隠し得る

いずれも発生窓は狭いが、このメッセージは「実装が壊れた」と「待機が足りない」を切り分けるために追加されたものなので、切り分けを濁す経路は無い方がよい。

**推奨修正**:

1 周につき 1 回だけ読み、両方でその値を使う。

```kotlin
val actual = layout.recyclerViewItemCountForTest()
if (actual == expected) return
if (System.nanoTime() >= deadline) {
    fail("itemCount が $timeoutMillis ms 以内に $expected へ収束しなかった（現在の itemCount: $actual）")
}
```

`awaitRows` も同じ形なので、Suggestion-1 と同様に揃えるか共通化時にまとめて扱う。

## 確認して問題なかった観点

- **deadline は単調時計** — `System.nanoTime()` を使用。壁時計 (`System.currentTimeMillis()`) ではないため、テスト実行中の時刻補正で timeout が誤爆/無限化しない
- **早期 return の抜け道なし** — ループは `idle()` → `waitForIdle()` → 値チェック → deadline チェックの順で、必ず 1 周分は looper と Compose runtime を流してから判定する。旧実装のように「フラッシュ前に一致していたら即 return」する経路は無い
- **無限ループしない** — 収束か `fail()` の 2 経路のみ。ミューテーション実測で `fail()` 到達を確認済み
- **2 つ目の呼び出し (`:414`) への波及は妥当** — style 切替テストでは旧実装だと 50 回で未収束でも黙って通過していた。新実装ではハードゲートになり検出力が上がる。共有ヘルパの改善に伴う必然の波及であり、スコープ逸脱ではない
- **スコープ遵守** — `git status --short` は対象テストファイル 1 件のみ。`src/main` 無変更、testFixtures 化などの構造的共通化にも手を付けていない
- **足場の凍結** — `exploration.md` は書き換えられていない
- **import 配置** — `java.util.concurrent.TimeUnit` の位置 (`org.robolectric.annotation.Config` の後、alias import の前) は同モジュール `DSLAccessoryVisibilityRenderingTest.kt` と完全に一致。ファイル既存の import 順の乱れは本変更由来ではない
- **deviation** — オーナー合意との差分は見当たらず、`deviation.md` 不在は妥当

## 参考所見 (本変更のスコープ外・修正指示ではない)

`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:169` の docstring に変更提案識別子の裸参照 (`purify-core-extract-style-to-ui-layer`) と履歴記述 (「〜で `UpdateTheme` ケースは削除された」) が残っている。comment-policy の禁止類型に該当するが、`scripts/comment-policy-lint.py` は 0 件と報告しており、機械検査が取りこぼしている。本 diff の対象外なので指摘としては挙げないが、別途 lint ルールと既存債務の棚卸しの材料になる。

## アクションプラン

1. **[必須]** Minor: `KsSettingsViewComposeTest.kt:225-227` の呼び出し側コメントを、helper docstring と矛盾しない現行の機構説明へ書き直す (または機構の説明を helper 側へ一元化する)。修正後の再レビューはこの 1 点の確認のみで足りる
2. **[任意]** Suggestion-2: fail メッセージ用の itemCount 再取得を 1 回読みに直す。1 と同じファイルなのでついでに入れられる
3. **[任意 / 別途判断]** Suggestion-1: `Thread.yield()` → `Thread.sleep(1)`。`awaitRows` と揃える必要があるため、単独で入れるか共通化時にまとめるかはオーナー判断

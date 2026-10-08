# レビュー結果: android-test-hang-calendar-then-compose-scroll-control (003 回目)

**日付**: 2026-10-07
**判定**: NEEDS_DISCUSSION

## サマリー

固定された版 (shasum は依頼どおり。レビューの前後で変わっていない) は、全件が debug・release とも通り、`review-002.md` の Major 2 件と Minor 1 件は解消している。合意済みの形 (毎回 window を取り外してから流す・テストを落とさない・残った処理の例外は流し切ってから投げ直す) のとおりに動くことを実測で確かめた。

一方、打ち切りの経路に、この change が直す対象と同じ失敗 (原因のテストは通り、同じ JVM の後続の Compose が黙って動かなくなる) が残ることを 2 通り再現した。直すには `deviation.md` の合意済みの行 (打ち切ってもテストを落とさない) を改める必要があり、「未合意」の時計の仕組みの採否とも連動するので、オーナーの判断が要る。実装の誤りではなく設計の選択なので CHANGES_REQUESTED ではなく NEEDS_DISCUSSION とする。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・テスト結果を報告するとき)
- `kasane/lessons/code-review.md` (L-001: 検出力は改変して実測する)

core / android の handbook は存在しない (`kasane/handbook/index.md`)。

ロードしたスキル: ksn-review、kotlin-impl-skill

## 確認した観点

### 着手時の確かめ

- 5 ファイルの shasum は依頼の値と一致。レビューの終わりにも同じ値
- `git status` は依頼の一覧と一致。`DateCalendarDialogTest.kt`・`DateSelectionSheetTest.kt`・`ComposeScrollRecreationTest.kt`・`src/main/` に差分なし
- 一時テスト・観測用コードの残りは無い (`src/test/resources/` は `robolectric.properties` の 1 ファイルだけ)
- 実験はすべて作業用ディレクトリの写しで行い、作業ツリーは書き換えていない。写しには観測用の記録 (テストの開始時と終わりの印・時計・流した件数) と一時テストを足した。終了時に Gradle Test Executor は残っていない

### ビルドとテスト

| 実行 | 結果 |
|---|---|
| 作業ツリーで `./gradlew test --rerun-tasks --continue` | BUILD SUCCESSFUL。`kssettingsview` 1401 tests / 0 failures (debug)、1401 / 0 (release)。`kssettingsview-bridge` 193 / 0 (debug)、193 / 0 (release)。合計 1594 件 × 2。打ち切りの書き出しは 0 件 |
| 写しで 3 クラスを単独 (debug) | `DateCalendarRecreationTest` 19 / 0、`DateCalendarDialogTest` 39 / 0、`DateSelectionSheetTest` 47 / 0。固まらない |
| 写しで全件 (debug、観測つき) | 1401 / 0。Robolectric のテスト 1005 件のうち、開始時に印が立っていたもの 0 件。終わりに流した件数は最大 90 件、合計 1,255 件。打ち切り 0 件、流し切りで例外 0 件 |
| 写しで実行順を入れ替えた全件 (`ComposeScrollRecreationTest` に失敗の履歴を作ってから) | `ComposeScrollRecreationTest` が先頭で走り 1401 / 0。「状態の変更と同じ処理から出した命令は新しいツリーで解決される」は 0.127 秒で通った。印つきの開始 0 件 |
| 同じ入れ替えで、`robolectric.properties` を外して全件 | 1401 tests / 1 failure。落ちたのは上と同じテストで、`待機条件が 5000 ms 以内に成立しなかった (processed=0 state=0 ...)`。共通の後片付けが効いていることの確かめになる |

`exploration.md`「完了の確かめ」の 4 項目はすべて成り立つ。入れ替えの確かめは debug だけで行った (release は作業ツリーの全件のみ)。

### `review-002.md` の指摘の扱い

| 指摘 | 扱い |
|---|---|
| Critical: レビュー中に作業ツリーが書き換わる | 解消。前後で shasum が一致 |
| Major: 残った処理の例外で流し切りが中断し、印が残る | 解消 (下の「実測 1」) |
| Major: コードが決定事項と `deviation.md` のどちらとも合わない | 解消。`deviation.md` の本文の行 (取り外してから流す・落とさない・2 クラスの「閉じる」は足さない・上限・例外) は今のコードと合う。件数の行 (1594 件 × 2) も実測と合う |
| Minor: 打ち切りがどこにも現れない | 標準エラーへテスト名つきの 1 行が出るようになった。ただし下の指摘 1 のとおり、それでは足りない場合がある |
| Suggestion: 落とす形では関係のないテストが誤って落ちる | 今の形では該当しない。指摘 1 の選択肢を選ぶときの前提になる (時計の仕組みがあれば起きない) |

### 依頼された観点

**実測 1: 残った処理の例外 (解消の確かめ)**。Compose を載せた Activity を作り、例外を投げる処理をメインスレッドへ積んで、キューを流さずに終えるテストを足した。

- そのテストは `IllegalStateException` で落ちる。例外の後ろに積んだ処理も実行された。終わりの時点で印は 2 つとも下りている
- 後続の 18 件は印なしで始まり、`ComposeScrollControlTest` 11 件・`ComposeScrollRecreationTest` 6 件はすべて通った
- 本体も `AssertionError` で落ちるようにすると、報告される失敗は本体の `AssertionError` で、残った処理の例外は Suppressed に付く。KDoc の「本体の失敗がそのまま報告される」のとおり

**実測 2: window の取り外しの失敗**。window から外れるときに例外を投げる View を載せたダイアログを、Compose の Activity より先に開いて終えるテストを足した。テストはその例外で落ち、終わりの時点で印は下りており、後続 12 件は印なしで始まって通った。取り外しを 1 つの window につき 1 回しか頼まないので、例外で回り続けることも無い。

**打ち切り**: 穴がある (指摘 1)。

**時計の扱い**: 「未合意」の節の評価は指摘 3。

**正常に終わる既存のテストの意味**:

- 取り外しと流し切りは、テスト本体・`@After`・rule がすべて終わった後に走る (`RobolectricTestRunner.afterTest` → `AndroidTestEnvironment.tearDownApplication` をバイトコードで確かめた)。本体の検証には触れない
- 開始時の仮想時刻に頼るテストは見当たらない。テストが `SystemClock.uptimeMillis()` を使う箇所 (`NumberSelectionSheetTest`・`PickerSelectionSheetTest`・`DateSelectionSheetTest`・`KsWheelViewTest`・`CustomCellRenderingTest`) は、どれもその場の時刻をタッチイベントの基準に取るだけで、絶対値を見ていない。`@Config(application = ...)` で Application を差し替えるテスト、クラス単位の後片付け (`@ClassRule`・`@AfterClass`) も無い
- dismiss リスナー: 取り外しは `Dialog.dismiss()` を通らないので、開いたまま終えたダイアログの dismiss リスナーは呼ばれない。これは変更前 (window が残ったまま捨てられる) と同じ。変わるのは、本体で閉じたダイアログの「閉じ切りの通知」がキューに残っていた場合で、これまで捨てられていたものが終わりに実行される。全件で、流し切り中の例外は 0 件
- 新しく落ちうる形が 1 つある (指摘 2)

**KDoc と実際の挙動**:

- 「テスト本体とその後片付けが終わった後、looper を作り直す前に `onTerminate` を呼ぶ」「1 回の反復が必ず 1 件を実行する」(`ShadowPausedLooper` の `RunOneRunnable`)、「例外は控えておいて最後まで流し切り、その後に投げ直す」は、バイトコードと実測に合う
- 時計の節が「実測」と書く 4 点 (遅くなる・上限に達する・フレームが届かなくなる・時計を戻さなければ起きない) は、すべて再現した。「推定」と書いてある仕組みは、Android 13 の `Choreographer.doFrame` のバイトコード (`mLastFrameTimeNanos` と比べて `scheduleVsyncLocked` へ戻る分岐) と、遅れ 60 秒に対して流した件数が 60,009 件だった実測に合う。推定を事実として書いている箇所は無い
- 合わないのは「件数の上限」の節 (指摘 4)
- 確かめていないもの: `detachWindows` の KDoc の「開いた側より先に `Popup` を外すと例外になる」

### 仕様充足 (合意スコープとの照合)

- 範囲 1: `deviation.md` の 1・4・5 行目の形で実装されている
- 範囲 2: `DateCalendarRecreationTest` に rule が足されている。本体と後片付けは変えていない
- 範囲 3: `deviation.md` の 2 行目のとおり、2 クラスに差分なし
- 範囲 4: `ComposeFrameDriver.kt` の 2 つの写しは `package` 行を除いて同一。差分はコメントだけ。`[付随修正]` (内部 `MainLooperDispatcher` の KDoc) は同梱条件に収まる
- 範囲 5: `src/main/` に差分なし。`kssettingsview-bridge` の変更はコメントだけ
- `exploration.md` の決定事項は、実装に合わせて書き換えられていない (範囲 1・3 は元の文のままで、差は `deviation.md` にある)
- 記録の無い乖離: 見当たらない。時計の仕組みは「未合意」として分けて書かれている

### 設計品質

- コメント規約: `python3 scripts/comment-policy-lint.py` は禁止 0 件。差分のコメントに作業文書のパス・change 識別子・レビューの通番・履歴記述は無い
- Kotlin の観点: 指摘 5 のほかに問題は見当たらない。`catch (thrown: Throwable)` は流し切りを最後まで進めるためで、控えた例外は必ず投げ直している
- オーバーエンジニアリング: 該当なし。時計の仕組みは 10 行足らずで、効果は指摘 3 のとおり
- 入力検証・認証認可・機密情報: テストコードのみで該当なし

## 指摘事項

### [🟠 Major] 打ち切った後に、同じ JVM の後続の Compose が黙って動かなくなる場合がある

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:132-138` (打ち切りの扱い)、`:48-57` (KDoc)、`deviation.md` の 4 行目

**問題点**: 上限で打ち切ると、原因のテストは通り、標準エラーへ 1 行出るだけである。その後の Compose が動かなくなる組み合わせを 2 つ再現した。この change が直す対象と同じ種類の失敗で、原因のテストではなく後続のテストに現れる。

| 終わりに残したもの | 原因のテスト | 打ち切り後の状態 | 後続 |
|---|---|---|---|
| Compose の UI dispatcher の上で、フレームを待ち続けるコルーチン (window に載っていない) | 通る | 印が 2 つとも立ったまま | 11 件が印つきで始まり、`ComposeScrollControlTest` の「状態の変更と同じ処理から出した命令は新しいツリーで解決される」が `待機条件が 5000 ms 以内に成立しなかった` で落ちた |
| `Choreographer` へ自分を登録し直し続ける callback (Compose の dispatcher と同じ `Choreographer`。その JVM で最初に Compose を使うテストで起きる) | 通る | 印は下りているが、`Choreographer` の「フレーム予約済み」(`mFrameScheduled`) が立ったまま | rule を通さない Compose の再 composition が 1 度も走らない (足した検証用のテストが落ちた) |
| 同じ callback (Compose の dispatcher とは別の `Choreographer`) | 通る | 印も予約済みも無い | 影響なし |

2 つ目は印を見ても分からない。`review-002.md` が「打ち切った時点で印は立っておらず」と書いた確かめでは見えない形である。

KDoc の「Compose の予約は window を取り外した時点で有限になっており、上限よりずっと手前で消化される」は、1 つ目の場合に成り立たない。

全件では打ち切りは 0 件で、既存のテストでは起きていない。

**推奨修正** (オーナーの判断が要る。`deviation.md` の 4 行目「テストは落とさず」を改めることになる):

- 案 A (推奨): 打ち切ったら、そのテストを失敗にする (残った処理の例外と同じく、流し切りの後に投げる。本体の失敗は上書きされない)。原因のテストで落ちるので、後続へ持ち越した失敗を追わずに済む。4 行目の理由は「固まらないため」で、落としても固まらない。オーナー指摘の「表示したまま終わるテストを落とす仕組みに意味が無い」にも当たらない。今の形では、表示したまま終えても打ち切りにならないためである。`review-002.md` の Suggestion (関係のないテストが誤って落ちる) は、時計の追いつきが上限を使い切ることが原因だった。時計の仕組み (指摘 3) があれば起きない。全件での終わりの件数は最大 90 件で、上限まで 3 桁の余裕がある
- 案 B: 落とさない形を保つ。その場合は、打ち切り後に後続が動かなくなりうることを KDoc と標準エラーの文面に書き、蒸留送りの行 (`deviation.md` の 7 行目) にも足す。失敗は後続のテストに出るので、原因は標準エラーの行から人が探すことになる

### [🟡 Minor] 閉じる処理を積んだまま終えるテストが、土台の取り外しのせいで落ちる

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:154-161` (`removeViewImmediate`)、`:46` (KDoc「テストは、選択面などを表示したまま終えてよい」)

**問題点**: ダイアログを表示し、`dismiss()` をメインスレッドへ積んで、キューを流さずに終えるテスト (`Handler.post { dialog.dismiss() }`) を足すと、`IllegalArgumentException: View=DecorView@...[...] not attached to window manager` で落ちた。土台が先にダイアログの window を外すので、後から実行された `dismiss()` が外す相手を見つけられない。Activity を `close()` してから終える形でも同じ。変更前は、積まれた処理ごと捨てられて通る。

テストの欠陥ではないものが、土台のせいで落ちる。ただし黙ってではなく失敗として現れ、スタックに `MainLooperDrainingApplication.detachWindowsAndDrainMainLooper` が載るので原因はたどれる。本体は選択面を同期で閉じており (`HostAnchoredDialog.kt:29` ほか)、全件で流し切り中の例外は 0 件。今のテストには該当が無い。

**推奨修正**: どちらかを選ぶ。

- 受け入れる: KDoc の 46 行目に「閉じる処理をキューに残したまま終えると、その処理が終わりに実行されて失敗する」ことを書き、蒸留送りの行 (`deviation.md` の 7 行目) に足す
- 起こさない: 表示中のダイアログは window を直接外さず、`dismiss()` で閉じる (Robolectric の `ShadowDialog.getShownDialogs()` で得られる)。こうすると、後から来た `dismiss()` は何もしない。ただし dismiss リスナーが終わりに呼ばれるようになるので、既存の全件で確かめ直すことになる

### [🔵 Suggestion] 「未合意」の時計の仕組みは、採る材料がそろっている

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:86-90`、`:98`、`:59-73` (KDoc)、`deviation.md`「未合意」

**判断材料** (写しで、仕組みを外した版と付けた版を比べた):

| 場面 | 仕組みなし | 仕組みあり |
|---|---|---|
| 時計を 600 秒進めて通るテスト → Compose の状態を書くだけで終えるテスト → 再 composition を確かめるテスト | 2 本目の終わりが 100,000 件で打ち切られ (2 本目は通る)、3 本目が「再 composition が走らない」で落ちた。印は立っておらず、`Choreographer` の予約済みが立ったまま | 14 件すべて通る。2 本目の終わりは 2 件 |
| 同じ並びで 60 秒 | 通る。2 本目の終わりに 60,009 件を流す | 通る |
| 時計を 2 時間進めて通るテストの後の `ComposeScrollControlTest` | 通るが、各テストが約 5 秒かかる (通常は 0.1 秒前後) | 通る。遅くならない |
| 既存の全件 (debug) | 1401 / 0。打ち切り 0 件。終わりに流す件数は最大 2,944 件、合計 121,424 件。20 秒 | 1401 / 0。打ち切り 0 件。最大 90 件、合計 1,255 件。19 秒 |

- 効く条件: 先のテストが仮想の時計を進めた分だけ、後続の Compose のフレームが 1 ミリ秒につき 1 件ずつ見送られる。遅れが約 100 秒 (上限 100,000 件 × 1 ミリ秒) を超えると、終わりの流し切りが打ち切られて後続が動かなくなる。指摘 1 の 2 つ目と同じ状態である
- 既存のテストとの距離: 1 つのテストが進める時計は最大 6.0 秒 (`idleFor(Duration.ofSeconds(2))` を数回)。仕組みが無くても今は落ちない。余裕は 16 倍ほどで、数分の `idleFor` を持つテストが 1 本足されると届く
- 副作用: 各テストの開始時の仮想時刻が、同じ JVM で先に走ったテストの終わりの時刻になる (全件の最後で約 85 秒)。開始時の時刻に頼るテストは無く (上の「正常に終わる既存のテストの意味」)、debug・release とも 1401 / 0。Robolectric の「テストごとに時計が初期値へ戻る」という前提は、このモジュールでは成り立たなくなる
- 指摘 1 との関係: 案 A (打ち切りで落とす) を採るなら、この仕組みは要る。無いと、時計を進めたテストではなく、その後に走った無関係のテストが打ち切りで落ちる (`review-002.md` の Suggestion と同じ形)

**推奨**: 採る。外す場合は、約 100 秒という限界を蒸留送りの行に足す。採る場合は、「未合意」の行を本文の乖離の行へ移し、「各テストの開始時の仮想時刻は初期値ではない」ことを蒸留送りの行 (`test-execution.md`「Android」) に足す。

### [🟡 Minor] 「件数の上限」の KDoc が、打ち切り後の影響を実際より軽く書いている

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:52-54`

**問題点**: 「打ち切ってもテストは失敗させず、起きたことを標準エラーへ 1 行書き出す。Compose の予約は window を取り外した時点で有限になっており、上限よりずっと手前で消化される。」は、打ち切っても Compose に害が無いと読める。指摘 1 のとおり、発生源が Compose の dispatcher の上にあれば予約は有限にならず、別の発生源でも `Choreographer` の予約済みが残りうる。

**推奨修正**: 指摘 1 の結論に合わせて書き直す。案 B を採る場合の例:

```kotlin
// before
 * [MAX_DRAIN_TASKS] 件で打ち切る。打ち切ってもテストは失敗させず、起きたことを標準エラーへ 1 行
 * 書き出す。Compose の予約は window を取り外した時点で有限になっており、上限よりずっと手前で
 * 消化される。

// after (形の例)
 * [MAX_DRAIN_TASKS] 件で打ち切る。打ち切ってもテストは失敗させず、起きたことを標準エラーへ 1 行
 * 書き出す。window に載っていた Compose の予約は、取り外した時点で有限になり、上限よりずっと手前で
 * 消化される。発生源が Compose の UI dispatcher や、それと同じ `Choreographer` の上にあるときは、
 * 打ち切った時点で予約が残り、同じプロセスの後続のテストの Compose が動かなくなる。
```

### [🔵 Suggestion] 同じ例外が 2 度投げられると、流し切りが中断する

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:121`

**問題点**: `Throwable.addSuppressed` は、自分自身を渡されると `IllegalArgumentException` (Self-suppression not permitted) を投げる。残った 2 つの処理が同じ例外の実体を投げると、この例外が `catch` の中から外へ出て、流し切りが途中で止まる (`review-002.md` の Major と同じ経路)。JDK の仕様からの指摘で、再現は作っていない。起きる場面は限られる。

**推奨修正**:

```kotlin
// before
failure?.addSuppressed(thrown) ?: run { failure = thrown }

// after
val first = failure
when {
    first == null -> failure = thrown
    first !== thrown -> first.addSuppressed(thrown)
}
```

## アクションプラン

1. オーナーが決める: 打ち切ったテストを落とすか (指摘 1 の案 A / 案 B)、時計の仕組みを採るか (指摘 3)。この 2 つは連動する。案 A を採るなら時計の仕組みも採る
2. オーナーが決める: 閉じる処理を積んだまま終えるテストを、落ちるものとして受け入れるか、起こさない形にするか (指摘 2)
3. 決まった形に合わせて、コード (案 A なら打ち切りで投げる)・KDoc (指摘 4、指摘 2 の 46 行目)・`deviation.md` (4 行目、「未合意」の行、蒸留送りの 7 行目) を直す
4. 指摘 5 は任意
5. 案 A を採った場合の確かめ: 上の表の 1 つ目・2 つ目の残し方で原因のテストが落ちること、全件 (debug・release) が 1401 / 0 のままで打ち切りが 0 件であること

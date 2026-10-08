# レビュー結果: android-test-hang-calendar-then-compose-scroll-control (002 回目)

**日付**: 2026-10-07
**判定**: CHANGES_REQUESTED

## サマリー

レビューの最中に、対象の作業ツリーが少なくとも 3 回書き換わった。依頼された版 (空にならなければ window を外して流し直し、テストを落とす) は途中で無くなり、最後に見た版は「毎テスト window を外す・打ち切ってもテストを落とさない・2 クラスの『閉じる』は足さない」という別の設計になっている。`deviation.md` は前の設計のままで、今のコードと合っていない。

最後に見た版でも、テストの終わりに残った処理が例外を投げると流し切りが中断し、予約済みの印が残って後続のテストが黙って検証にならない (実測。この change が直す対象と同じ失敗)。作業ツリーを固定したうえでの再レビューが要る。

## レビュー対象の版

作業ツリーは未コミットなので、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt` の内容で版を呼び分ける。実験はすべて `android/` の写し (作業用ディレクトリ) で行い、作業ツリーは書き換えていない。

| 版 | 作業ツリーにあった時刻 | 内容 | 2 クラスの「閉じる」 |
|---|---|---|---|
| A | 〜22:16 | 流し切れなければ window を外して流し直し、`AssertionError` でテストを落とす (依頼文と `deviation.md` が書いている形) | あり |
| (途中) | 22:26 ごろ | `println` と `idleFor` の入った観測用のコード。`compose/AaaTmpLeftOpenTest.kt` という一時テストも作業ツリーにあった | なし (22:16:56 に差分が消えた) |
| B | 22:27:21〜 (sha1 `4a592ea0`) | 毎テスト最初に window を外す。落とさない。`detachAllWindows` は `while (true)` | なし |
| C | 22:45:28〜 (sha1 `4479f714`) | B の `while (true)` を「1 つの window に 1 回だけ頼む」に変えたもの | なし |

このほか、`ComposeScrollRecreationTest.kt` (22:08:32) と `src/test/resources/robolectric.properties` (22:09:27、22:51:19) の更新時刻が動いている。後者は 22:51 ごろ一時的に作業ツリーから消えていた。誰かが作業ツリーの上で直接、実行順の入れ替えや「後片付けを外す」確かめをしていると読める (推定)。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・テスト結果を報告するとき)
- `kasane/lessons/code-review.md` (L-001: 検出力は改変して実測する)

core / android の handbook は存在しない (`kasane/handbook/index.md`)。

ロードしたスキル: ksn-review、kotlin-impl-skill

## 確認した観点

### ビルドとテスト

| 実行 | 版 | 結果 |
|---|---|---|
| 作業ツリーで `./gradlew test --rerun-tasks --continue` (22:15〜22:17) | 特定できない (実行中の 22:16:56 にテスト 2 ファイルが書き換わった) | BUILD SUCCESSFUL。`kssettingsview` 1401 tests / 0 failures (debug)、1401 / 0 (release)。`kssettingsview-bridge` 193 / 0 (debug)、193 / 0 (release) |
| 写しで `:kssettingsview:testDebugUnitTest` 全件 | B | **終わらない**。5 分待って打ち切った (指摘 1) |
| 写しで `--tests '*.AccessoryViewSwapProbeTest'` | B | **終わらない**。75 秒で打ち切った |
| 写しで `:kssettingsview:testDebugUnitTest` 全件 | C | 1401 tests / 0 failures |
| 写しで 3 クラスを単独 | C | `DateCalendarRecreationTest` 19 / 0、`DateCalendarDialogTest` 39 / 0、`DateSelectionSheetTest` 47 / 0。固まらない |
| 写しで実行順を入れ替えた全件 (`ComposeScrollRecreationTest` に失敗の履歴を作ってから) | C | `ComposeScrollRecreationTest` が先頭で走り 1401 / 0。「状態の変更と同じ処理から出した命令は新しいツリーで解決される」は 0.126 秒で通った。開始時に印が立っていたテストは 1005 件中 0 件、終わりに実行した処理は最大 2,485 件 |

C について確かめていないもの: release 構成、`kssettingsview-bridge`、「共通の後片付けを外すと入れ替えた実行で落ちる」(作業ツリーで他の Gradle が動いていたので、作業ツリーでは回していない)。

### 依頼された 4 つの観点

1. **後片付け自身が失敗する場合** — 残っている (指摘 2、A と C で実測)。B は後片付けそのものが固まった (指摘 1)。A には別の形もあった (指摘 5)
2. **件数で区切る判断と「収束を待つアサーション」** — 矛盾しない。同節が反復回数を退ける理由は「対象がバックグラウンドにある間にループが燃え尽きる」ことだが、このループは 1 回ごとにメインスレッドの処理を 1 件実行するので、進まないまま回数を使い切ることがない。待機でもない (空になれば即座に戻る)。ただし C は上限に達しても何も知らせずに戻る。同節の 3 つ目 (超過時は黙って戻らない) の趣旨とは合わない (指摘 4)
3. **window の取り外しが正常に終わるテストを変えていないか** — A は失敗する経路でしか取り外さないので変えない。B・C は全テストで取り外す。C の全件は 1401 / 0 で件数も成否も変わらないが、B は正常に通るテスト (`AccessoryViewSwapProbeTest`) で固まった
4. **KDoc と実際の挙動** — C の「レイアウトの途中で止まった window は、その場では外れず、後で外す予約がキューに積まれる」はスレッドダンプと合う。Robolectric が `@After`・rule の後、looper の作り直しの前に `onTerminate()` を呼ぶこともバイトコードで確かめた (`SandboxTestRunner$2.evaluate` → `RobolectricTestRunner.afterTest` → `AndroidTestEnvironment.tearDownApplication`)。時計の追いつきは「推定」と明記されている。合わないのは指摘 3 (KDoc「テストは、選択面などを表示したまま終えてよい」が決定事項と逆)

### 仕様充足 (合意スコープとの照合。C について)

- 範囲 2 (`DateCalendarRecreationTest` に rule): 差分どおり。本体と後片付けは変えていない
- 範囲 4 (`ComposeFrameDriver.kt` の 2 つの写し): `package` 行を除いて同一。差分はコメントだけ。`[付随修正]` (内部 `MainLooperDispatcher` の KDoc) は同梱条件に収まる
- 範囲 5: `src/main/` と `kssettingsview-bridge` の動作に変更なし
- 範囲 1・範囲 3: 決定事項・`deviation.md` と合っていない (指摘 3)

### 設計品質

- コメント規約: `python3 scripts/comment-policy-lint.py` は禁止 0 件。C の KDoc に作業文書のパス・change 識別子・履歴記述は無い
- Kotlin の観点: 指摘 2 のほかに問題は見当たらない
- 入力検証・認証認可・機密情報: テストコードのみで該当なし

## 指摘事項

### [🔴 Critical] レビュー対象の作業ツリーが、レビューの最中に書き換えられ続けている

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt` ほか (上の「レビュー対象の版」)

**問題点**: 依頼された版 A はレビュー中に無くなった。途中の版には固まるものがある。

- 22:26 ごろの作業ツリー (観測用のコード入り) を写して全件を回すと、`MainLooperDrainingApplication.onTerminate` → `ShadowLooper.idleFor` → `idle()` で 10 分戻らなかった
- 版 B は全件が終わらない。`AccessoryViewSwapProbeTest` の「detach なし factory で別の親に付いた view を設定すると IllegalStateException になる」の終わりで、`detachAllWindows` の `while (true)` が回り続ける。スレッドダンプ:

  ```
  android.view.ViewRootImpl.die(ViewRootImpl.java:8456)
  android.view.WindowManagerGlobal.removeView(WindowManagerGlobal.java:441)
  jp.kamusoft.kssettingsview.MainLooperDrainingApplication.detachAllWindows(MainLooperDrainingApplication.kt:92)
  jp.kamusoft.kssettingsview.MainLooperDrainingApplication.detachWindowsAndDrainMainLooper(MainLooperDrainingApplication.kt:72)
  jp.kamusoft.kssettingsview.MainLooperDrainingApplication.onTerminate(MainLooperDrainingApplication.kt:57)
  ```

  このテストはレイアウトの途中で例外を投げる。その window は `removeViewImmediate` でもその場では外れず一覧に残るので、ループが抜けない
- 版 C はこの固まりを直してあり、全件が通る

どの版を合意済みとして判定すればよいかが、成果物からは決まらない。この判定は C (sha1 `4479f714`) に対するものだが、C がその後も書き換わっていれば当てはまらない。

**推奨修正**: 実装側の作業を止めて作業ツリーを固定し、その版でレビューをやり直す。観測や確かめ (一時テスト・`println`・設定ファイルの出し入れ) は作業ツリーではなく写しで行う。

### [🟠 Major] 終わりに残った処理が例外を投げると流し切りが中断し、印が残って後続が黙って検証にならない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:75` (C。`looper.runOneTask()` を囲む `while`)

**問題点**: キューの処理が例外を投げると、例外はそのまま `onTerminate()` の外へ出て、残りは実行されない。Compose の予約が残っていれば印は立ったまま次のテストへ持ち越される。この change が直す対象そのものである。

実測 (C の写し。Compose を載せた Activity を作り、例外を投げる処理をメインスレッドへ post し、looper を回さずに `close()` して終えるテストを 1 本足し、既存の 4 クラスの前に走らせた):

- 足したテストは `IllegalStateException` で落ちる (ここまでは見える)
- その終わりの時点で `scheduledTrampolineDispatch` / `scheduledFrameDispatch` が 2 つとも立っていた
- 後続の 45 件中 44 件が、印の立った状態で始まった
- `ComposeScrollControlTest` の「状態の変更と同じ処理から出した命令は新しいツリーで解決される」が `待機条件が 5000 ms 以内に成立しなかった (processed=0 state=0 smooth=false layoutRequested=false ...)` で落ちた。`exploration.md`「③ の観測」と同じ表示
- A の写しでも同じ結果 (全件では 1005 件中 984 件が印ありで始まった)

テスト本体が失敗して途中で抜けたときは、本体の失敗が先に報告され、後片付けの例外は suppressed に回る (`SandboxTestRunner$2.evaluate` のバイトコード)。原因が見えにくいまま後続が印ありで走る。

`windowManager.removeViewImmediate` が例外を投げた場合も同じ経路になる (こちらは再現を作っていない)。

**推奨修正**: 処理が例外を投げても流し切りを続ける。最初の例外を控えておき、空になるか上限に達した後で投げ直す。

```kotlin
// before
while (processed < MAX_DRAIN_TASKS && !looper.isIdle) {
    looper.runOneTask()
    processed++
    detachWindows(requested)
}

// after (形の例)
var firstFailure: Throwable? = null
while (processed < MAX_DRAIN_TASKS && !looper.isIdle) {
    try {
        looper.runOneTask()
    } catch (t: Throwable) {
        if (firstFailure == null) firstFailure = t else firstFailure.addSuppressed(t)
    }
    processed++
    detachWindows(requested)
}
firstFailure?.let { throw it }
```

直した後、上の足したテストの後で印が立たないことを実測する。

### [🟠 Major] 今のコードが決定事項と `deviation.md` のどちらとも合っていない

**該当箇所**: `deviation.md` (全体。22:13:09 から更新されていない)、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:38-48` (C)、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarDialogTest.kt`・`DateSelectionSheetTest.kt` (差分なし)

**問題点**: `deviation.md` は版 A を書いている。C には次の、記録の無い乖離がある。

- 範囲 3「`DateCalendarDialogTest` と `DateSelectionSheetTest` の後片付けで、提示中の選択面を閉じる」: C では 2 クラスに差分が無い。KDoc は「テストは、選択面などを表示したまま終えてよい」と書いており、決定事項と逆
- 範囲 1「空にならないときに…理由を載せてそのテストを落とす形にできるかを確かめる。できなければ素の `idle()` のままにして、その事実を報告する」: C は落とさず、上限で黙って打ち切る
- `deviation.md` の 1 行目「表示中の window…を取り外してからもう一度空にし、そのうえでテストを失敗させる。同じ JVM の何本目でも毎回落ち」は、C の挙動ではない (C は全テストで最初に取り外し、落とさない)
- 「蒸留時に反映」の行 (`deviation.md` の 5 行目、`exploration.md` の「選択面などフレームを要求し続けるものを開いたまま終えないこと (終えたときの落ち方) を足す」) も、C と逆の規約を handbook へ書く申し送りになっている
- 「未合意」の節の「固まらずに失敗にはなるが」も C では成り立たない。window に載っていない発生源を残したテストは、C では通る (指摘 4)

C の設計がオーナーの指示によるものかどうかは、成果物からは分からない。

**推奨修正**: 採る設計を確定し、`deviation.md` をそのコードに合わせて書き直す (乖離の行・蒸留送りの行・未合意の節)。C を採るなら、範囲 3 を行わないことと、落とさないことを乖離として記録する。

### [🟡 Minor] 上限で打ち切ったことがどこにも現れない (C)

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:43-48`、`:75` (C)

**問題点**: 上限に達すると、残りを捨てたまま何も知らせずに戻る。実測 (C の写し):

- `Choreographer` へ自分を登録し直し続ける callback を残して終えるテスト: 100,000 件で打ち切られ、テストは通った。仮想の時計は 100 秒進んだ
- 仮想の時計を 600 秒進めて通るテストの後: 後続の既存のテスト 1 件の終わりが 100,000 件で打ち切られ、全 45 件が通った

どちらも打ち切った時点で印は立っておらず、後続の 45 件は印なしで始まった。KDoc の「Compose の予約は…上限よりずっと手前で消化される」と食い違う結果は出ていない。ただし、キューを捨てたことを知る手段が無い。「未合意」の節が挙げる「打ち切った時点で Compose の予約が残る組み合わせ」が起きたときも、同じく何も出ない (この組み合わせの再現は作れていない)。`test-execution.md`「収束を待つアサーション」の 3 つ目 (超過時は黙って戻らない) は待機についての決まりで、ここへそのまま当てはまるものではないが、趣旨は同じである。

**推奨修正**: 上限に達したら、少なくとも実行した件数を載せて標準エラーへ出す。落とすかどうかは設計の確定 (上の指摘) とあわせて決める。落とす形にするなら次の指摘を先に読むこと。

### [🔵 Suggestion] 落とす形 (版 A) へ戻す場合: 関係のないテストが誤って落ちる

**該当箇所**: 版 A の `drainMainLooperOrFail` (C には無い)

**問題点**: A では、選択面を残すテストが同じ JVM に 2 件以上あると、その後に走る関係のないテストが同じ `AssertionError` で落ちた。メッセージの「後続のテストへの影響は無い」は成り立っていなかった。

実測 (A の写しで 2 クラスの「閉じる」を外して全件):

- 選択面を残す 21 件 (`DateCalendarDialogTest` 20、`DateSelectionSheetTest` 1) が落ちる。ここは意図どおり。印は 1005 件中 0 件
- 加えて、作業ツリーでは通る 5 件が落ちた (`DateCalendarRecreationTest` 1、`SectionAccessoryRenderingTest` 4)。メッセージは `取り外した window: 0 個 / 表示中だったダイアログ: []` で、「表示したままテストを終えている。テストの後片付けで閉じること」と指示するが、これらのテストは何も表示したままにしていない。実行順が変わった回は `PickerCellObjectBindingTest` 8 件も落ちた
- 選択面を残すテストが 1 件だけなら、他は落ちない (1401 tests / 1 failure)
- 失敗するテストが無くても起きる。仮想の時計を 600 秒進めて通るテストを先に走らせると、`PickerCellObjectBindingTest` の 1 件が落ちた (60 秒では落ちない)

見立て (推定): 打ち切るまでに進んだ仮想の時計 (1 件目で約 50 秒、続くと約 100 秒へ近づく) に、後続のテストが追いつくまでの処理も件数に数えられ、上限を使い切る。

**推奨修正**: 落とす形を採るなら、追いつきのための処理を上限の勘定から外す方法を先に決め、選択面を残すテストを 2 件以上入れた全件で、他のテストが落ちないことを実測する。

## アクションプラン

1. 作業ツリーを固定する。観測・確かめ用の変更を作業ツリーに置かない
2. 採る設計 (落とす / 落とさない、2 クラスの「閉じる」を足す / 足さない) を確定し、`deviation.md` をそのコードに合わせる
3. 流し切りの途中で例外が出ても最後まで流すようにし、印が残らないことを実測する
4. 上限で打ち切ったことを知らせる
5. 固定した版で再レビューする。その版で、release 構成・`kssettingsview-bridge` を含む全件と、「共通の後片付けを外すと入れ替えた実行で落ちる」を確かめ直す

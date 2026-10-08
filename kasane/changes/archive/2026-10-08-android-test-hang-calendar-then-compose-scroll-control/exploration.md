# Exploration: android-test-hang-calendar-then-compose-scroll-control

## 課題 / 動機

Android のユニットテストで、カレンダー選択面の作り直しを確かめるクラス `DateCalendarRecreationTest` が、単独で実行すると固まって終わらない。全件実行で通っているのは、同じ JVM で先に走るクラスに依存している。

起票時 (2026-10-07、change `android-compose-scroll-restore-with-multiple-hosts` の実装中) の見立ては「`DateCalendarRecreationTest` と `ComposeScrollControlTest` の 2 クラスを絞ると固まる」「カレンダーのテストが残した仕事が後続クラスへ持ち越される」だった。同日の調査 (Gradle 14 回の実行とスレッドダンプ) で、この見立ては外れていると分かった。以下では問題を 3 つに分けて呼ぶ。

- ① `DateCalendarRecreationTest` が単独で完走しない (原因は特定済み)
- ② 全件実行での ① の合格が、クラスの実行順に依存している (仕組みは実測で確定)
- ③ 全件実行で `ComposeScrollControlTest` の 1 本が間欠的に待機切れで落ちる (原因は ② と同じ。実行順が入れ替わった回に出る)

3 つの根は 1 つ。Compose を載せたテストが、消化されない dispatch を残したまま終わると、Compose の static な UI dispatcher (`AndroidUiDispatcher.Main`) に「予約済み」の印が立ったまま JVM の終わりまで残る (②)。印が残った JVM では Compose の一部が動かなくなり、そのおかげで ① の固まりが隠れ、そのせいで ③ の待機切れが起きる。

対象: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarRecreationTest.kt`、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollRecreationTest.kt`、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollControlTest.kt`

### ① の観測 (2026-10-07)

- 固まるのは `DateCalendarRecreationTest` 自身で、`ComposeScrollControlTest` は関係しない
  - `DateCalendarRecreationTest` 単独: 2 回とも 1 本目で固まった (10 分待っても戻らない)
  - 選択面を提示するテスト 1 本だけ (「再生成後の選択面でも取消では通知しない」) でも固まった
  - 起票時の 2 クラスの組: `ComposeScrollControlTest` が先に走って 11 件すべて通り、その後 `DateCalendarRecreationTest` の 1 本目で固まった。Gradle は `--tests` の指定順によらず `compose.*` を `ui.*` より先に走らせる
  - 選択面を提示しないテスト 1 本 + `ComposeScrollControlTest` は通った
- 戻らない呼び出しは、テストが Activity を作り直す `controller.recreate()` (`DateCalendarRecreationTest.kt:490`)。テストは `idle()` を直接呼んでいないが、Robolectric が内部で呼ぶ

  ```
  org.robolectric.shadows.ShadowPausedLooper.idle(ShadowPausedLooper.java:104)
  org.robolectric.shadows.ShadowPausedLooper.idleIfPaused(ShadowPausedLooper.java:177)
  org.robolectric.android.controller.ActivityController.visible(ActivityController.java:233)
  org.robolectric.android.controller.ActivityController.recreate(ActivityController.java:642)
  jp.kamusoft.kssettingsview.ui.DateCalendarRecreationTest.recreate(DateCalendarRecreationTest.kt:490)
  ```

- 回し続けているのは、作り直しで提示し直された選択面の中の Compose `Popup` の位置監視 (`AndroidPopup.android.kt` の `Popup` 内の loop → `PopupLayout.pollForLocationOnScreenChange`)。毎フレーム、次のフレームを要求する
- Robolectric 4.13 の `ShadowDisplayEventReceiver` は、Choreographer が paused でなければ、フレームの要求のたびに時計を進めてその場で vsync を配る。フレームを要求し続ける処理があると `idle()` の実行可能なメッセージが尽きない (バイトコードで確認)
- 前のテストの残りではない。選択面はホストの破棄で閉じられ (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/HostAnchoredDialog.kt:24`)、作り直しの後に `KsSettingsView` が新しい選択面を提示する。固まるのはこの新しい選択面
- `material3` 1.4.0 の `DatePicker` で Popup を使う呼び出しは `TooltipBox` だけ (バイトコードで確認)。回っている Popup がどの部品のものか、なぜ提示の直後から合成されるかは未特定
- `kasane/handbook/cross/test-execution.md` の「カレンダーの選択面を提示した後に `idle()` を呼ばない」は同じ仕組みを書いているが、Robolectric が内部で呼ぶ `idle()` には触れていない。同節は `DateCalendarRecreationTest.kt` を適用実例に挙げている

### ② の観測と推論 (2026-10-07)

観測:

- `ComposeScrollRecreationTest` + `DateCalendarRecreationTest` は通った (6 + 19 tests / 0 failures)。`compose.*` 全体 + `DateCalendarRecreationTest` も通った
- `ComposeScrollControlTest` + `DateCalendarRecreationTest`、`DateCalendarDialogTest` + `DateCalendarRecreationTest` は固まった
- 全件実行 (単一 JVM で 111 クラスを逐次) の順は、`ComposeScrollControlTest` が 3 番目、`ComposeScrollRecreationTest` が 4 番目、`DateCalendarDialogTest` が 43 番目、`DateCalendarRecreationTest` が 44 番目
- ui-android 1.11.4 の `AndroidUiDispatcher.Main` は static で、「予約済み」の印が立っている間は main looper へも Choreographer へも投入しない。Robolectric はテストの間に main looper のキューを空にし、main の Choreographer を差し替える (バイトコードで確認)

仕組み (使い捨てのコピーに観測用のテストを足して実測):

- `ComposeScrollRecreationTest` の 6 本のうち 4 本が、後片付けの `activityController?.close()` (`ComposeScrollRecreationTest.kt:103`) で `AndroidUiDispatcher.Main` の予約済みの印 (`scheduledTrampolineDispatch` / `scheduledFrameDispatch`) を 2 つとも立て、main looper を回さずに終わる。残る dispatch は、破棄で取り消された Compose 内部の loop (`AndroidContentCaptureManager.boundsUpdatesEventLoop` など) の継続
- 印を下ろすのは dispatcher 自身の実行 (`performTrampolineDispatch`) だけ。Robolectric がテストの間にキューを空にすると、その実行ごと消えるので、印は JVM が終わるまで下りない
- `ComposeFrameDriver` を差し込む 2 本は印を残さない
- 印が残った JVM では、test rule も駆動器も通さない Compose は最初の合成だけを行う。再合成・`LaunchedEffect`・`withFrameNanos` は 1 度も走らない (dispatch は待ち行列に積まれるだけ)。そのため選択面の Popup の loop が回らず、① の `idle()` が戻る
- Choreographer の差し替えは原因ではない。印が無ければ、dispatcher が古い Choreographer を握ったままでも再合成もフレームも届く。`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ComposeFrameDriver.kt` 冒頭の説明 (最初に取得した Choreographer を握り続けるのが原因) は、この実測と合わない
- 裏取り: `close()` の直後に `idle()` を足すと印は下り、その後の `DateCalendarRecreationTest` は固まった
- `DateCalendarRecreationTest` 自身の後片付け (`DateCalendarRecreationTest.kt:149`) も印を残す側になりうる (下の「private のままリフレクションで呼ぶ」の単独実行で、2 本目以降の開始時に印が立っていた)

影響範囲の見当 (ソースを読んだだけ。番号は全件実行の順):

- rule も駆動器も通さずに Compose を載せるクラス: `DateCalendarDialogTest` (43)、`DateCalendarRecreationTest` (44)、`DateSelectionSheetTest` (45)、`HostWrappedContextUserContentTest` (54)、`SectionAccessoryRenderingTest` (82)
- `ComposeFrameDriver` を使うクラス (駆動器の下の木は印の影響を受けない): `CustomCellBuilderReleaseTest`、`CustomCellPooledRebindMeasureTest`、`CustomCellRecycleTest`、`CustomCellRenderingTest`、`HostThemeIndependenceTest`
- rule を使うテストも印の影響を受ける (③ がその例)

印を残さない処理の置き場の実測 (現行ツリーの写しでモジュールの全件 1401 件を実行。印の記録は Robolectric のテスト 1005 件が対象):

- 2 クラスだけ直す形 (`ComposeScrollRecreationTest` と `DateCalendarRecreationTest` の後片付けに `idle()`、後者に rule): 1401 tests / 0 failures。ただし 1005 件中 932 件が印の立った状態で始まった。次に印を残すのは `PickerCellObjectBindingTest`
- 印を残すクラスは、その 2 つのほかに少なくとも 7 つ (残した回数 / クラスの件数。観測用に印を戻しながら数えた目安): `DateCalendarDialogTest` 27/39、`PickerCellObjectBindingTest` 5/12、`SectionAccessoryRenderingTest` 4/19、`HostThemeIndependenceTest` 3/10、`HostWrappedContextUserContentTest` 2/2、`CustomCellPooledRebindMeasureTest` 1/3、`DateSelectionSheetTest` 1/47
- 全テスト共通の置き場はある。`robolectric.properties` で指定した Application の `onTerminate()` は、毎テスト、looper の初期化より前に呼ばれる (Robolectric 4.13 の `RobolectricTestRunner.afterTest` → `tearDownApplication()`。バイトコードと実測)
- そこで素の `idle()` を呼ぶと固まる。選択面を提示したまま終わるテストで戻らない (`DateCalendarDialogTest` の 1 本目の終わり、`DateSelectionSheetTest` の 35 本目の終わり)。固まるまでの 282 件はすべて印なしで始まった
- フレームの配信を止めてから `idle()` する変種は 1401 tests / 0 failures で固まらない。trampoline 側の印で始まるテストは 0 件になるが、フレーム側の印が 1005 件中 722 件で残る。「印のおかげで通る」形が場所を変えて残っている可能性がある (未確認)
- その変種の状態で `ComposeScrollRecreationTest` を先頭に入れ替えても、③ の対象テストは通った (0.137 秒、開始時の印なし)

完成形の実測 (共通の `onTerminate()` で素の `idle()` + `DateCalendarRecreationTest` に rule + `DateCalendarDialogTest`・`DateSelectionSheetTest` の後片付けで提示中の選択面を閉じる):

- 全件 1401 tests / 0 failures、固まり無し。印が立って始まったテストは 1005 件中 0 件 (trampoline 側・フレーム側とも)
- `ComposeScrollRecreationTest` が先頭の順でも 1401 / 0。③ の対象テストは 0.163 秒で通り、印は 0 件
- 3 クラスの単独実行も完走 (19 / 0、39 / 0、47 / 0)
- 2 クラスの「閉じる」が無いと、rule を足しても `DateCalendarDialogTest` の 1 本目の終わりで固まる。相手は rule のものではない実物の Recomposer。推論: このテストは提示の後に looper を一度も回さずに終わるので、選択面の合成が共通の `idle()` で初めて走り、rule の管理下に入らない
- 「閉じる」があれば、この 2 クラスの rule の有無で全件の成否は変わらない (rule を外しても 1401 / 0、印 0 件)
- 置き場の副作用は見当たらない。リポジトリの `android/` に `robolectric.properties` は無く、`@Config(application = ...)`・テスト用 manifest・Application の派生も無い
- `kssettingsview-bridge` のテストは印を残していない (193 件中 0 件)
- 未確認: 繰り返しての安定性 (全件は 2 回)、release 構成、「閉じる」を足した 2 クラスの検証内容が変わっていないか (件数と成否だけ見た)

### ③ の観測 (起票時 + 2026-10-07)

- `./gradlew test --rerun-tasks --continue` で、`ComposeScrollControlTest` の「状態の変更と同じ処理から出した命令は新しいツリーで解決される」が 5 秒の待機切れで 5 回中 2 回落ちた (debug、起票時)。エミュレータを動かしていない回でも落ちたため、負荷が原因という見立ては外れている
- 変更の前のコードの写しでは、全件実行は 2 回とも通った (起票時)。change `android-compose-scroll-restore-with-multiple-hosts` で頻度が上がったかどうかは、この回数では判断できない
- ① の固まりとは別の経路。全件実行で `ComposeScrollControlTest` はカレンダーを提示するクラスより先に走る
- 自然には再現しない。モジュールの全件を clean 付きで 10 回、起票時の形で 5 回 (debug・release 各 5) 回して、すべて 1401 tests / 0 failures。別の実行で load average が 57〜248 まで上がった回も通ったので、負荷では起きない
- 実行順を入れ替えると落ちる。現行ツリーの写しで `ComposeScrollRecreationTest` に失敗の履歴を作り、clean なしで全件を回すと、`ComposeScrollRecreationTest` が先頭になり、対象テストだけが落ちた (1401 tests / 1 failure、5.11 秒)。Gradle は前回失敗したクラスを先に走らせる (Gradle 9.5.0 の `RunPreviousFailedFirstTestDefinitionProcessor`)。clean 付きの実行は履歴が消えるので入れ替わらない
- 落ちた回の表示: `処理済み 0 件・スクロール無し・行は先頭のまま` (`processed=0 state=0 smooth=false layoutRequested=false`)。時間切れは本題の待機 (`ComposeScrollControlTest.kt:268`)。追加した行は一覧に commit 済みだが、命令は宣言の更新前の世代で引かれて捨てられていた。対象テストの開始時点で印は 2 つとも立っていた
- 仕組み: 印が残ると `GlobalSnapshotManager` が snapshot の適用通知を looper 経由で配らなくなる。通知は compose テスト基盤の idle 判定 (`ComposeIdlingResource.isIdleNow`) からだけ出て、looper を回さずにフレームが進む。宣言を内部 Store へ流す `AndroidView` の update は handler への post で残り、命令の引き直し (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:133`) が先に走る。引けない命令は捨てられる (`.../compose/DSLScrollCommandResolver.kt:99`)
- 同じクラスの他の 10 本は、印が残っていても通る
- コミット `62a3c14` の本体の変更は、この経路 (命令の待ち行列・位置の控えと戻し) に触れていない。変更前のツリーの写しでも同じ印が残り、同じ表示で落ちた。仕組みはその前からある
- 推論: 起票時の 5 回中 2 回は、`ComposeScrollRecreationTest` を書き換えていた作業中に同クラスが落ちた回の、次の全件実行だった (当時の記録は無く、確かめられない)
- 本体側の材料: 命令の引き直しは「フレームを 1 回待つ」ことで update の完了を代理している (`KsSettingsViewComposable.kt:130` のコメント)。実機で順序が逆転する経路は見つかっていないが、無いことも示していない

## 検討した選択肢 (却下案と理由を含む)

起票時に挙げた直す場所の候補のうち、調査で外れたもの:

- テストの後片付けで選択面を閉じる — 却下。固まるのはテスト本体の `recreate()` の途中で、相手は作り直しで提示し直された選択面。`@After` は届かない
- `ComposeScrollControlTest` の待機の書き方を変える — 却下。固まる場所ではない
- テストクラスの分離 (fork の分割) — 却下。`DateCalendarRecreationTest` は単独で固まるため、分離すると全件実行でも固まる

① を止める手段として実測し、成り立たなかったもの (使い捨てのコピーで `DateCalendarRecreationTest` 19 本を、単独と `ComposeScrollRecreationTest` の後の両方で実行):

- 作り直しの間だけ自動のフレーム配信を止める (`ShadowChoreographer.setPaused(true)`) — 却下。固まらないが 19 本中 2 本が落ち、選択面の中身が合成されない (window に付かない)
- テスト全体でフレーム配信を止める — 却下。19 本中 16 本が落ちる (提示まで進まない)
- Robolectric の公開 API だけで作り直す (`recreate()` を使わない) — 却下。19 本中 10 本が落ちる。window に付ける処理 (`visible()`) が必ず内部の `idle()` を呼ぶため、避けると選択面が提示し直されない
- 上に加えて `visible()` の中身を private のままリフレクションで呼ぶ — 却下。固まらないが非公開の実装に頼る。印の無い状態では後続の待機 (`awaitRowsAndScheduledWork`) が待機切れになる (単独で 19 本中 1 本)

直し方の候補として実測し、成り立ったもの (採否は論点 2・3。同じコピーで `DateCalendarRecreationTest` 19 本を、単独と `ComposeScrollRecreationTest` の後の両方で実行):

- 公式の Compose テスト用 rule に載せる (`@get:Rule` で `createEmptyComposeRule()` を 1 つ足すだけ)
  - 単独でも印が残った後でも 19 tests / 0 failures。作り直しの直後に選択面は合成済み。再合成は `waitForIdle()` を挟めば起きる
  - rule だけでは印が残る (`GlobalSnapshotManager.ensureStarted` の継続)。下の後片付けを足すと全テストの開始時に印が無い
  - 入力モードで復元した選択面をカレンダーへ戻すと、`waitForIdle()` が 60 秒で idle にならない例が 1 件あった (既存のアサーションには無い操作。原因は未特定)
  - `DateCalendarDialogTest` に同じ rule を足しても 39 tests / 0 failures
- 自前の駆動器 (`ComposeFrameDriver`) を window 全体に効かせる (`WindowRecomposerPolicy.setFactory` で、テストの間だけ window の Recomposer を駆動器のものに差し替える)
  - 単独でも印が残った後でも 19 tests / 0 failures。ライブラリが作る選択面の window にも届く。駆動器の `stop()` が looper を回すので印は残らない
  - `WindowRecomposerPolicy` は Compose の内部 API (`@OptIn(InternalComposeUiApi::class)` が要る)。元へ戻すには既定の factory を設定し直す
  - 表示モードを変えた後の `frame()` は収束しない (時刻を進めずにフレームを送るため、切り替えのアニメーションが終わらない見立て)。既存の 19 本は `frame()` を呼ばないので合否には出ない
- 後片付けで main looper を空にして印を残さない (`close()` の直後に `idle()`)
  - 印を残さなくなる。既に残った印は消せない
  - `ComposeScrollRecreationTest` と `DateCalendarRecreationTest` の両方に足し、上のどちらかと組み合わせると、単独でも続けて実行しても、全テストの開始時に印が無いまま完走した

印を残さない処理の置き場 (論点 3) で採らなかった案:

- 印を残すクラスごとに後片付けを足す — 採らない。直す先が少なくとも 9 クラスあり、次に書かれるテストが黙って印を残せる
- 共通の 1 か所で、フレームの配信を止めてから空にする — 採らない。他のテストを変えずに全件が通るが、フレーム側の印が残り、① を隠す状態が形を変えて残りうる

この change の範囲 (論点 1) で採らなかった案:

- A: ①② を扱い、③ は別の change に切り出す — 探索での推奨だったが採らない (オーナー判断、2026-10-07)
- B: ① だけを扱い、②③ は別の change に切り出す — 採らない。全件実行が実行順に頼って通る状態が残る

## 決定事項

- 論点 1: この change は ①②③ をすべて扱う (オーナー決定、2026-10-07)。③ の原因が分かるまで change を閉じられないことは承知の上
- 論点 2: カレンダーの選択面のテストは、公式の Compose テスト用 rule (`createEmptyComposeRule()`) で動かす (オーナー決定、2026-10-07)。自前の駆動器 (`ComposeFrameDriver`) を `WindowRecomposerPolicy` で window 全体に効かせる案は採らない。Compose の内部 API に頼り、駆動器の改修 (アニメーションを含むフレーム送りの収束) と 2 つの写しの保守が増えるため
- 論点 3: 印を残さない処理は、全テスト共通の 1 か所に置く (オーナー決定、2026-10-07)。どのテストの後も、main looper が空になってから次へ進む
- 論点 4: この change では本体を変えない。命令の引き直しが「フレームを 1 回待つ」ことで宣言の更新の完了を代理している件は、別の change にも積まない (オーナー決定、2026-10-07)。観測できている不具合はテストの土台のものだけで、実機で順序が逆転する経路は見つかっていない。記述は上の「③ の観測」に残す
- 論点 5: 規約 (`kasane/handbook/cross/test-execution.md`) の言い直しは蒸留で行う (下の「蒸留時に反映」)
- 論点 6: change-id は付け直さない。`kasane/concepts/log.md` の蒸留記録 (`android-compose-scroll-restore-with-multiple-hosts`) がこの id を参照しているため。id と実態 (2 クラスの組の問題ではない) の差は「課題 / 動機」の冒頭で説明する

### 実装に渡す範囲

触るのは `android/kssettingsview/src/test/` と、駆動器のもう一方の写しだけ。本体コード (`src/main/`) は変えない。

1. 共通の後片付け。テスト用の Application を足し、`src/test/resources/robolectric.properties` の `application=` で指定する。その `onTerminate()` で main looper を空にする (Robolectric 4.13 は毎テスト、looper の初期化より前にこれを呼ぶ)
   - 空にならないとき (フレームを要求し続ける処理が残っているとき) に、固まらせず、理由を載せてそのテストを落とす形にできるかを確かめる。できなければ素の `idle()` のままにして、その事実を報告する
   - なぜ要るかを Application のコメントに書く (消化されない dispatch を残して終わると、`AndroidUiDispatcher.Main` の予約済みの印が JVM の終わりまで残り、後続のテストの Compose が止まる)
2. `DateCalendarRecreationTest` に `@get:Rule` で `createEmptyComposeRule()` を足す
3. `DateCalendarDialogTest` と `DateSelectionSheetTest` の後片付けで、提示中の選択面を閉じる。この 2 クラスに rule は足さない。閉じる処理が各テストの検証 (閉じ切りの通知を数えるものを含む) を変えないことを確かめる
4. `ComposeFrameDriver.kt` 冒頭の「なぜ必要か」を実測に合わせて直す。2 つの写し (`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/` と `android/kssettingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/`) を同じ内容にそろえる。駆動器の動作は変えない
5. 範囲に入れないもの: `kssettingsview-bridge` への共通の後片付け (印を残していない)、印を残す他の 7 クラスの個別の書き換え (共通の後片付けで足りる)、本体の変更 (論点 4)

### 完了の確かめ

- `DateCalendarRecreationTest`・`DateCalendarDialogTest`・`DateSelectionSheetTest` を、それぞれ単独で実行して完走する (変更前は 1 つ目が単独で固まる)
- 全件 (`./gradlew test --rerun-tasks`) が debug・release とも通り、件数が変更前と変わらない
- 実行順を入れ替えた実行でも通る。手順: `ComposeScrollRecreationTest` に必ず落ちるテストを一時的に足して 1 回実行し、失敗の履歴を作る。足したテストを消し、clean なしで全件を実行する。`ComposeScrollRecreationTest` が先頭で走り、`ComposeScrollControlTest` の「状態の変更と同じ処理から出した命令は新しいツリーで解決される」が通ること (一時的なテストはコミットしない)
- 共通の後片付けが効いていることの確かめ。後片付けを外すと、上の入れ替えた実行で同じテストが落ちること

### 蒸留時に反映

- 蒸留時に反映: `kasane/handbook/cross/test-execution.md` 「カレンダーの選択面を提示した後に `idle()` を呼ばない」 — Robolectric が内部で呼ぶ `idle()` (`ActivityController.recreate()` / `visible()`) でも同じく固まること、作り直しをまたぐテストは公式の Compose テスト用 rule で動かすこと、適用実例の記述を言い直す
- 蒸留時に反映: `kasane/handbook/cross/test-execution.md` 「Android」 — テストの終わりに main looper を空にする共通の後片付けがあることとその理由、選択面などフレームを要求し続けるものを開いたまま終えないこと (終えたときの落ち方) を足す
- 蒸留時に反映: `kasane/handbook/cross/test-execution.md` 「Android の実行方法」 — clean を付けない実行では Gradle が前回失敗したクラスを先に走らせ、実行順が変わることを足す (実行順に依存する失敗の切り分けに使える)

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

なし。決定はどれもテストの土台の中で閉じ、あとから変えられる。従うべき決まりになる部分は、蒸留で `kasane/handbook/cross/test-execution.md` に書く

## 未決の論点

なし

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S / M / L (理由)

S (オーナー確定、2026-10-07)。

- 触る能力は 1 つ (Android のテストの土台)。テストのソースとテスト用の設定だけを変える
- 公開 API の変更なし、本体コードの変更なし、UI なし
- 可逆 (足すのは新しいファイル 2 つと、テスト 3 クラスの小さな変更、コメントの修正)
- 直し方の形は探索の実測で成り立つことを確かめてあり、結果を見ながら追い込む調整は要らない

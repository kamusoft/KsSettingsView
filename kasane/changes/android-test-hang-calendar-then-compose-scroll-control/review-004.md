# レビュー結果: android-test-hang-calendar-then-compose-scroll-control (004 回目)

**日付**: 2026-10-08
**判定**: APPROVED

## サマリー

固定された版 (shasum は依頼の値と一致。レビューの前後で変わっていない) は、全件が debug・release とも通り、`exploration.md`「完了の確かめ」の 4 項目がすべて成り立つ。`review-003.md` の Major 1 件・Minor 2 件・Suggestion 2 件はすべて解消しており、`deviation.md` の合意済みの形 (ダイアログは `dismiss()` で閉じる・打ち切ったテストは落とす・時計を戻さない・失敗の優先順) のとおりに動くことを、写しに足した一時テストで実測した。

Critical / Major は無い。残るのは、原因のテストが通ったまま後続の Compose が黙って動かなくなる経路が 1 つ (フレームの配信を止める Robolectric の API を使ったとき。今のテストは誰も使っていない)、`deviation.md` の蒸留送りの行に古い記述が 1 つ、KDoc の細部である。コードの修正を要するものは無い。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・テスト結果を報告するとき)
- `kasane/lessons/code-review.md` (L-001: 検出力・説明の正誤は改変して実測する)

core / android の handbook は存在しない (`kasane/handbook/index.md`)。

ロードしたスキル: ksn-review、kotlin-impl-skill

## 確認した観点

### 着手時の確かめ

- 5 ファイルの shasum は依頼の値と一致。レビューの終わりにも同じ値
- `git status` は依頼の一覧と一致。`DateCalendarDialogTest.kt`・`DateSelectionSheetTest.kt`・`ComposeScrollRecreationTest.kt`・`src/main/` に差分なし
- 一時テスト・観測用コードの残りは無い (`android/kssettingsview/src/test/resources/` は `robolectric.properties` の 1 ファイルだけ)
- 改変と一時テストはすべて作業用ディレクトリの写しで行い、作業ツリーは書き換えていない。写しの土台には記録の行だけを足した (テストの開始時と終わりの印・`Choreographer` の予約済み・流した件数・閉じたダイアログ)。終了時に Gradle Test Executor は残っていない

### ビルドとテスト

| 実行 | 結果 |
|---|---|
| 作業ツリーで `./gradlew test --rerun-tasks --continue` | BUILD SUCCESSFUL。`kssettingsview` 1401 tests / 0 failures (debug)、1401 / 0 (release)。`kssettingsview-bridge` 193 / 0 (debug)、193 / 0 (release)。合計 1594 件 × 2 |
| 写しで 3 クラスを単独 (debug) | `DateCalendarRecreationTest` 19 / 0、`DateCalendarDialogTest` 39 / 0、`DateSelectionSheetTest` 47 / 0。固まらない |
| 写しで実行順を入れ替えた全件 (`ComposeScrollRecreationTest` に失敗の履歴を作ってから。debug、観測つき) | `ComposeScrollRecreationTest` が先頭で走り 1401 / 0。「状態の変更と同じ処理から出した命令は新しいツリーで解決される」は 0.214 秒で通った。Robolectric のテスト 1005 件のうち、開始時に印または `Choreographer` の予約済みが立っていたもの 0 件。終わりに流した件数は最大 90 件、合計 1,417 件。打ち切り 0 件、流し切りの例外 0 件 |
| 同じ入れ替えで、`robolectric.properties` を外して全件 | 1401 tests / 1 failure。落ちたのは上と同じテストで、`待機条件が 5000 ms 以内に成立しなかった (processed=0 state=0 ...)`。共通の後片付けが効いていることの確かめになる |

入れ替えの確かめは debug だけで行った (release は作業ツリーの全件のみ)。

### `review-003.md` の指摘の扱い

| 指摘 | 扱い |
|---|---|
| Major: 打ち切った後に後続の Compose が黙って動かなくなる | 解消 (案 A)。打ち切ったテストが `AssertionError` で落ちる (下の「実測 1」)。`deviation.md:6` に記録あり |
| Minor: 閉じる処理を積んだまま終えるテストが土台のせいで落ちる | 解消 (起こさない形)。ダイアログは `dismiss()` で閉じる (下の「実測 2」)。`deviation.md:3` に記録あり。ダイアログ以外の window には同じ形が残る (指摘 3) |
| Suggestion: 時計の仕組みの採否 | 採用。「未合意」の節は無くなり、`deviation.md:7` と蒸留送りの `deviation.md:15` に移っている (下の「実測 3」) |
| Minor: 「件数の上限」の KDoc が影響を軽く書いている | 解消。書き直された節は実測と合う |
| Suggestion: 同じ例外が 2 度投げられると流し切りが中断する | 解消 (`MainLooperDrainingApplication.kt:138-142`) |

### 依頼された観点

**実測 1: 打ち切りと失敗の優先順**。一時テストはクラスごとに別の JVM で実行した。

| 終わりに残したもの | 原因のテスト | 後続 |
|---|---|---|
| Compose の UI dispatcher の上でフレームを待ち続けるコルーチン | 100,000 件で打ち切られ、打ち切りの `AssertionError` で落ちる (0.42 秒)。印は 2 つとも立ったまま | 印つきで始まり、再 composition が走らない。`ComposeScrollControlTest` の 1 本も待機切れ。KDoc の「後に走ったテストの結果を当てにしない」のとおり |
| 自分を登録し直し続ける `Choreographer` の callback (Compose の dispatcher とは別の `Choreographer`) | 同じく落ちる (0.29 秒) | 影響なし。13 件すべて通る |
| 上の callback + 本体が `AssertionError` | 報告されるのは本体の失敗。打ち切りは Suppressed に付く | — |
| 上の callback + 残った処理が `IllegalStateException` | 報告されるのは処理の例外。打ち切りは Suppressed に付く | 再 composition は走る |

KDoc の「失敗の優先順」「件数の上限」のとおりに動く。全件では打ち切りは 0 件。

**実測 2: `dismiss()` で閉じる形**。

| 終え方 | 結果 |
|---|---|
| ダイアログを表示し、`dismiss()` をキューに積んで終える (Activity を閉じた後に終える形も) | 通る。後続の再 composition も走る |
| dismiss リスナーつきのダイアログを表示したまま終える | 通る。リスナーはテストの終わりに呼ばれる (`deviation.md:3` のとおり) |
| `show()` をキューに積んで終える / `dismiss()` と `show()` を続けて積んで終える | 通る。流している途中に表示された window も片付く |
| `Popup` と終わらないアニメーションを含む Compose を、Activity の中身・ダイアログの中身として looper を回さずに表示したまま終える | 通る。終わりに流すのは 9 件。後続の再 composition も走る |

既存のテストへの影響: 入れ替えの全件で、土台が終わりに閉じたダイアログは 160 件 (`PickerSelectionSheetTest` 62、`DateSelectionSheetTest` 38、`DateCalendarDialogTest` 21、`NumberSelectionSheetTest` 19、`TimeSelectionSheetTest` 18、`InputCellsTest` 2)。この 160 件では、選択面の「閉じ切りの通知」がテストの終わりに呼ばれるようになった。呼ばれるのは本体・`@After`・rule がすべて終わった後なので、各テストの検証 (通知を数えるものを含む) には届かない。届きうるのは、通知の処理が例外を投げた場合 (そのテストが落ちる) と、テストをまたいで生きる値を書き換えた場合で、流し切りの例外は 0 件、全件は実行順を入れ替えても 1401 / 0 だった。

**実測 3: 時計**。時計を 2 時間進めて終えるテストの後に、Compose の状態を書くだけで終えるテストと再 composition を確かめるテストを置いた。18 件すべて通り、後続の終わりに流す件数は 8 件のまま。

**土台が原因で、欠陥の無いテストを落とす形**: 表示したまま終える・ダイアログを閉じる処理を積んだまま終える・時計を大きく進めて終える、の 3 つは起きない (実測 2・3)。残るのは指摘 3 の形だけ。

**原因のテストが通ったまま後続が黙って動かなくなる形**: 1 つ再現した (指摘 1)。そのほかに調べて、該当しなかったもの:

- 流し切りが実行するのは、その時点で実行できるメッセージだけ (`ShadowPausedLooper.isIdle`)。遅延つきのメッセージは残って捨てられる。Compose の UI dispatcher は遅延なしの `post` と `postFrameCallback` しか使わないので、既定のフレーム配信では印は残らない
- 取り外しが例外を投げたテストは、その例外で落ちる (改変 M1)。黙っては通らない
- `@LooperMode`・`ShadowChoreographer.setPaused`・`@Config(application = ...)`・`@AfterClass`・`@ClassRule` を使うテストは無い。`@Config(sdk = ...)` は 33 だけで、`WindowInspector` (API 29 以上) が使える

**KDoc と実際の挙動**: 改変して確かめたもの。

| KDoc の記述 | 改変 | 結果 |
|---|---|---|
| 「開いた側より先に `Popup` を外すのも同じ理由で例外になる」 (`MainLooperDrainingApplication.kt:182-183`) | M1: 取り外しの順を逆にする | `Popup` つきで終えるテストが `IllegalArgumentException` で落ちた。記述のとおり |
| 「消えたものを外そうとすると例外になる」 (`:180-182`) | M2: 一覧にあるかの確かめを外す | 同じテストが `IllegalArgumentException` で落ちた。記述のとおり |
| 「外れるまで頼み直すと、予約が増え続けてキューが空にならない」 (`:177-178`) | M3: window を 1 回だけ取り外す控えを外す | 全件 1401 / 0 のまま。記述を裏づける場面が無い (指摘 4) |

読んで確かめたもの: 冒頭・「なぜ必要か」・「先に window を取り外す理由」・「件数の上限」・「時計」・「残っていた処理が例外を投げたとき」・「失敗の優先順」の各節は、実測 1〜3 と `review-003.md` の実測に合う。`DateCalendarRecreationTest.kt:137-145` の rule の説明、`ComposeFrameDriver.kt` の 2 つの写しの説明も `exploration.md` の実測と合う。

### 仕様充足 (合意スコープとの照合)

- 範囲 1: `deviation.md:3`・`:6`〜`:9` の形で実装されている
- 範囲 2: `DateCalendarRecreationTest` に rule が足されている。本体と後片付けは変えていない
- 範囲 3: `deviation.md:4` のとおり、2 クラスに差分なし
- 範囲 4: `ComposeFrameDriver.kt` の 2 つの写しは `package` 行を除いて同一。差分はコメントだけ。`[付随修正]` (`deviation.md:10`) はコメントのみで同梱条件に収まる
- 範囲 5: `src/main/` に差分なし。`kssettingsview-bridge` の変更はコメントだけ
- `exploration.md` の決定事項は、実装に合わせて書き換えられていない (範囲 1・3 は元の文のままで、差は `deviation.md` にある)
- 記録の無い乖離: 見当たらない。`deviation.md` の中の食い違いが 1 つある (指摘 2)

### 設計品質

- コメント規約: `python3 scripts/comment-policy-lint.py` は禁止 0 件。差分のコメントに作業文書のパス・change 識別子・レビューの通番・履歴記述は無い。要確認の 1 件 (`DateCalendarRecreationTest.kt:40`) は差分の外
- Kotlin の観点: 問題は見当たらない。`catch (thrown: Throwable)` は流し切りを最後まで進めるためで、控えたものは必ず投げ直している
- オーバーエンジニアリング: 該当なし
- 入力検証・認証認可・機密情報: テストコードのみで該当なし
- 土台そのものの回帰テストは無い (打ち切り・例外・時計の挙動は、実装時とレビューの一時テストでだけ確かめている)。打ち切りを起こすテストは同じ JVM の後続を壊すので、常設しない判断は妥当と見る

## 指摘事項

### [🟡 Minor] フレームの配信を止めたテストの後は、原因のテストが通ったまま後続の Compose が動かなくなる

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:152-158` (流し切り)、`:53-74` (KDoc「件数の上限」)

**問題点**: `ShadowChoreographer.setPaused(true)` でフレームの配信を止めたテストが、Compose を window に載せたまま終えると、そのテストは通り、同じ JVM の後続で rule も駆動器も通さない Compose の再 composition が 1 度も走らなくなる。

| テスト | 結果 | 終わりの状態 |
|---|---|---|
| 配信を止め、Compose の Activity を表示して状態を書き、そのまま終える | 通る。終わりに流すのは 1 件 | 印は 2 つとも下りている。Compose の dispatcher が握る `Choreographer` の「フレーム予約済み」(`mFrameScheduled`) が立ったまま |
| 直後の、再 composition を確かめるテスト | `recomposition expected:<1> but was:<0>` で落ちる | 以降のテストも同じ |
| 配信を止め、window に載せずに dispatcher の上でフレームを 1 回待つコルーチンを残す | 通る | フレーム側の印が立ったまま |

仕組み: 配信を止めている間、Robolectric は vsync をメッセージとして積まず、時計が進むのを待つ (`ShadowDisplayEventReceiver$NativeDisplayEventReceiver.scheduleVsync` のバイトコード)。キューは空なので流し切りは何もせず、上限にも達しない。予約だけがテストの終わりに失われる。KDoc が挙げる 2 つ目の残り方と同じ状態に、打ち切りを通らずに届く。

今のテストに `setPaused` を使うものは無く、全件では起きていない。変更前から同じ形で起きるので、この change が持ち込んだものでもない。ただし `exploration.md` の検討では実際に試した API で、この change が直す対象と同じ種類の失敗が黙って残る。

写しで、流し切りの前に配信を再開して時計を 1 フレーム進める形を試したが、直らなかった (Compose の dispatcher が握るのは先のテストの `Choreographer` で、時計の進みを受け取らない)。土台の中で片付けるには `Choreographer` の内部へ触ることになる。

**推奨修正**: コードは変えず、限界として書き残す。

- `deviation.md` の蒸留送りに 1 行足す (`kasane/handbook/cross/test-execution.md`「Android」): フレームの配信を止める (`ShadowChoreographer.setPaused(true)`) テストでは Compose を載せないこと。載せたまま終えると、そのテストは通り、後続の Compose が動かなくなること
- 土台が掛からない条件も同じ行に足す: 独自の `@Config(application = ...)` を付けたテスト (Robolectric の設定の仕様からの指摘で、実測していない)
- 任意: 同じ内容を KDoc の「件数の上限」の後に 1〜2 文で足す

### [🟡 Minor] `deviation.md` の蒸留送りに、今のコードに無い挙動が残っている

**該当箇所**: `deviation.md:11`、`deviation.md:3`

**問題点**: `deviation.md:11` は「打ち切りは標準エラーの `[MainLooperDrainingApplication]` の行で分かること」を `test-execution.md` に書くよう申し送っている。今のコードは標準エラーへ何も書き出さず、打ち切りはテストの失敗 (`AssertionError`) として現れる。このまま蒸留すると、規約に無い挙動が書かれる。`deviation.md:15` の「片付けきれなかったテストは失敗になる」とも食い違う。

`deviation.md:3` の「テストを落とす仕組みは置かない」も、`deviation.md:6` (上限に達したテストは失敗にする) と並べると言い切りが強い。3 行目が指すのは「表示したまま終えたテストを落とす仕組み」である。

**推奨修正**: `deviation.md:11` から標準エラーの記述を外す (失敗の文面で分かることは `:15` にある)。`deviation.md:3` は「表示したまま終えたテストを落とす仕組みは置かない」と対象を書く。

### [🔵 Suggestion] ダイアログ以外の window では、閉じる処理を積んだまま終えると土台のせいで落ちる

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:189-194`、`:48-51` (KDoc)

**問題点**: `review-003.md` の Minor と同じ形が、ダイアログ以外に残る。

| 終え方 | 結果 |
|---|---|
| `WindowManager.addView` で足した View の `removeView` をキューに積んで終える | `IllegalArgumentException: ... not attached to window manager` で落ちる |
| `ActivityController` の `destroy()` をキューに積んで終える | 同じ例外で落ちる |
| `PopupWindow.dismiss()` をキューに積んで終える | 通る |

どちらも失敗として現れ、黙ってではない。本体に `WindowManager` を直接使う箇所は無く、今のテストに該当は無い。作りにくい形なので、直す必要は薄い。

**推奨修正**: 任意。KDoc の「ダイアログだけは…」の段落に、ダイアログ以外の window を閉じる処理をキューに残したまま終えると、その処理が終わりに実行されて失敗することを 1 文足す。

### [🔵 Suggestion] 「1 つにつき 1 回だけ」の理由を裏づける場面が無い

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:176-178`、`:191`

**問題点**: KDoc は「頼んでもその場では外れず、後で外す予約がキューに積まれる window がある。外れるまで頼み直すと、予約が増え続けてキューが空にならない」と事実として書く。window を 1 回だけ取り外す控え (`:191`) を外しても、全件は 1401 / 0 のまま通り、打ち切りも起きなかった (改変 M3)。`removeViewImmediate` はその場で外すので、今の形でこの場面が起きるのかは確かめられていない (AOSP の実装の読みでは、レイアウトの最中に頼んだときだけ後回しになる。未実測)。

控え自体は害が無く、取り外しが例外を投げ続ける window を何度も頼み直さない効果がある。

**推奨修正**: 任意。観測した場面があるならそれを書く (どの window で、どう頼んだときか)。無いなら、確かめられている理由に書き直す。

### [🔵 Suggestion] KDoc の 1 行だけ折り返しが崩れている

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:88-89`

**問題点**: 88 行目が他の行の倍近い長さで、89 行目が短い。段落の途中に文を足したときの折り返し残りと見られる。

**推奨修正**: 段落を折り返し直す。

## アクションプラン

1. `deviation.md:11` の標準エラーの記述を外し、`deviation.md:3` の対象を書く (指摘 2)。蒸留の前に行う
2. `deviation.md` の蒸留送りに、フレームの配信を止めるテストの限界と、土台が掛からない条件を足す (指摘 1)
3. 指摘 3〜5 は任意。KDoc に手を入れるなら 3 つをまとめて行う。コードの挙動は変えないので、全件の確かめ直しで足りる

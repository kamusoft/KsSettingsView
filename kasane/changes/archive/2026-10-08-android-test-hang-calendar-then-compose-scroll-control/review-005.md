# レビュー結果: android-test-hang-calendar-then-compose-scroll-control (005 回目)

**日付**: 2026-10-08
**判定**: APPROVED

## サマリー

固定された版 (5 ファイルの shasum は依頼の値と一致。レビューの前後で変わっていない) は、全件が debug・release とも通り、`exploration.md`「完了の確かめ」の 4 項目がすべて成り立つ。実装は `deviation.md` の記録どおりで、時計の仕組みと配信の停止の検出は入っておらず、残すと決めたもの (ダイアログの `dismiss()`・window の取り外し・件数の上限・例外の投げ直し・失敗の優先順・作り直しテストの rule) は残っている。

Critical / Major は無い。KDoc の記述で、実測と合わない箇所が 1 つ (限界 (b) の「そのテストの中でも」)、今のテストで裏づけが取れない言い切りが 1 つある。どちらもコメントの言い回しで、コードの修正を要するものは無い。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・テスト結果を報告するとき)
- `kasane/lessons/code-review.md` (L-001: 説明の正誤は改変して実測する)

core / android の handbook は存在しない (`kasane/handbook/index.md`)。

ロードしたスキル: ksn-review、kotlin-impl-skill

## 確認した観点

### 着手時の確かめ

- 5 ファイルの shasum は依頼の値と一致。レビューの終わりにも同じ値
- `git status` は依頼時の一覧と一致。`src/main/`・`DateCalendarDialogTest.kt`・`DateSelectionSheetTest.kt`・`ComposeScrollRecreationTest.kt` に差分なし
- 一時テスト・観測用コードの残りは無い (`android/kssettingsview/src/test/resources/` は `robolectric.properties` の 1 ファイルだけ)
- 改変と一時テストはすべて作業用ディレクトリの写しで行った。写しの土台に足したのは記録の行だけ (テストの終わりの時計・流した件数・閉じたダイアログ・印)。終了時に Gradle Test Executor は残っていない

### 完了の確かめ (4 項目)

| 項目 | 実行 | 結果 |
|---|---|---|
| 全件 debug・release | 作業ツリーで `./gradlew test --rerun-tasks --continue` | BUILD SUCCESSFUL。`kssettingsview` 1401 tests / 0 failures (debug)、1401 / 0 (release)。`kssettingsview-bridge` 193 / 0 (debug)、193 / 0 (release)。合計 1594 件 × 2。件数は変更前と同じ |
| 3 クラスの単独 | 写しで 1 クラスずつ (debug) | `DateCalendarRecreationTest` 19 / 0、`DateCalendarDialogTest` 39 / 0、`DateSelectionSheetTest` 47 / 0。固まらない |
| 実行順の入れ替え | 写しで `ComposeScrollRecreationTest` に失敗の履歴を作り (1402 / 1)、足したテストを消して clean なしで全件 (debug、観測つき) | `ComposeScrollRecreationTest` が先頭で走り 1401 / 0。「状態の変更と同じ処理から出した命令は新しいツリーで解決される」は 0.103 秒で通った |
| 後片付けを外すと落ちる | 同じ入れ替えで `robolectric.properties` を外して全件 | `ComposeScrollRecreationTest` が先頭で 1401 / 1。落ちたのは同じテストで、`待機条件が 5000 ms 以内に成立しなかった (processed=0 state=0 ...)` |

入れ替えの確かめは debug だけで行った (release は作業ツリーの全件のみ)。

入れ替えた全件での観測 (Robolectric のテスト 1005 件):

- 終わりに印が立っていたテスト 0 件、打ち切り 0 件、流し切りの例外 0 件
- 終わりの時計は最大 6,103 ms。KDoc の「このモジュールのテストが進めるのは最大 6 秒」と合う
- 終わりに流した件数は最大 2,485 件 (上限の約 40 分の 1)、合計 101,848 件。1,000 件を超えたのは 42 件で、どれも、時計を約 2 秒進めたテストより後に走り、looper を回さずに Compose のフレームを残して終えたテスト。遅れ 1 ms につき 1 件を流している (下の「限界 (a)」と同じ仕組み)
- 土台が終わりに閉じたダイアログは 160 件 (150 テスト)。dismiss リスナーが終わりに呼ばれても、例外は 0 件、全件の成否は変わらない

### 起票した 3 つの問題と今あるテスト

- ① `DateCalendarRecreationTest` は rule で単独でも完走する。本体と後片付けは変えていない
- ②③ 実行順を入れ替えても全テストが印の無い状態で終わり、対象テストが通る。後片付けを外すと落ちるので、通っているのは土台の効果である
- 今あるテストの意味: 変わるのは、テストの終わりに残っていた処理が実行されること・表示中のダイアログの dismiss リスナーが終わりに呼ばれること。どちらも本体・`@After`・rule が終わった後で、各テストの検証には届かない。届きうるのは残った処理が例外を投げた場合 (そのテストが落ちる) で、全件では 0 件
- 取りこぼし: 見当たらない

### deviation.md との照合

写しに足した一時テストを、クラスごとに別の JVM で実行した。

| 記録 | 実測 |
|---|---|
| 表示したまま終えてよい (`deviation.md:3`) | `Popup` と終わらないアニメーションを含む Compose を、Activity の中身・ダイアログの中身として表示したまま終える → 通る (流すのは 8〜9 件)。後続の再 composition も走る |
| ダイアログは `dismiss()` で閉じ、リスナーは終わりに呼ばれる (`:3`) | `dismiss()` をキューに積んで終える・`show()` を積んで終える・両方を積んで終える → 通る。リスナーは終わりに呼ばれた |
| 流し切りの上限 (`:6`) | フレームを待ち続けるコルーチン・自分を登録し直す callback → 100,000 件で打ち切られ、そのテストが `AssertionError` で落ちる (0.2〜0.3 秒)。前者の後は後続の再 composition が走らない (KDoc のとおり) |
| 残った処理の例外・失敗の優先順 (`:9`・`:10`) | 本体の失敗 + 打ち切り → 本体の失敗が報告され、打ち切りは suppressed。処理の例外 + 打ち切り → 処理の例外が報告され、打ち切りは suppressed |
| 時計の仕組みは無い・配信の停止の検出は無い (`:7`) | コードに該当の処理は無い。下の限界 (a)(b) が実際に起きることでも確かめた |
| 限界 (a) (`:8`) | 時計を進めた先で Compose のフレームを実行して終える → 次に、looper を回さずに Compose を表示して終えるテストの終わりで流す件数は、6 秒で 6,013 件、60 秒で 60,013 件、90 秒で 90,013 件 (どれも通る)。110 秒・150 秒では打ち切られて落ち、その後の再 composition も走らない。「約 100 秒」と合う |
| 限界 (b) (`:8`) | 後続のテストで再 composition が届かないこと、再開して終えても同じであることは再現した。「そのテストの中でも」は条件つき (指摘 1) |
| 限界 (c) (`:8`) | `@Config(application = Application::class)` のテストでは別の Application が使われ、フレーム待ちを残しても落ちず、印が 2 つとも残り、後続の再 composition が走らない |
| 限界 (d) (`:8`) | `WindowManager.removeView`・`ActivityController` の `destroy()` をキューに積んで終える → `IllegalArgumentException: ... not attached to window manager` でそのテストが落ちる |
| 範囲 3 は足さない (`:4`) | 2 クラスに差分なし |
| 範囲 4・付随修正 (`:11`・`:12`) | 差分はコメントだけ。付随修正は同じファイルのコメントで、同梱条件に収まる |

記録の無い乖離は見当たらない。`exploration.md` の決定事項は実装に合わせて書き換えられていない (範囲 1・3 は元の文のままで、差は `deviation.md` にある)。

### KDoc と実際の挙動

`MainLooperDrainingApplication.kt` の節ごとに照合した。

| 節 | 確かめ方 | 結果 |
|---|---|---|
| なぜ必要か | 後片付けを外した入れ替え実行、限界 (c) の実測 | 合う |
| 先に window を片付ける理由 (フレームを要求し続ける処理・取り外すと有限になる) | 表示したまま終える一時テスト | 合う |
| 同 (ダイアログは `dismiss` で閉じる理由) | 改変: `dismiss()` をやめて View 階層を直接外す | 仕組みは記述のとおり (閉じる処理を積んで終える一時テスト 3 本が `IllegalArgumentException` で落ちた)。「終えるテストがあり」は今のテストでは裏づけが無い (指摘 2) |
| 件数の上限 | 上の表 | 合う。1 回の反復が 1 件を実行する |
| 失敗の扱い | 上の表 | 合う |
| この土台で無くならないもの (a)(c)(d) | 上の表 | 合う。(a) の原因は「推定」と書かれている |
| 同 (b) | 上の表 | 一部が合わない (指摘 1) |
| `closeWindowsAndDrainMainLooper` (処理を 1 件進めるたびに片付ける理由) | 改変: ループの中の片付けを外す | `Popup` を含むダイアログの `show()` を積んで終える一時テストが打ち切りで落ちた。記述のとおり。今のテストは外しても 1401 / 0 |
| `closeWindows` (一覧にあるかの確かめ・先に開かれた順) | 改変: 順を逆にする / 確かめを外す | どちらも `Popup` つきで終える一時テストが `IllegalArgumentException` で落ちた。記述のとおり |
| 打ち切りの失敗の文面 | 一時テスト | 挙げている原因 (コルーチン・登録し直す処理・先のテストの時計) はどれも実際に打ち切りを起こす |

`ComposeFrameDriver.kt`:

- 2 つの写しの差は `package` 行だけ (`diff` で確認)。変更前との差分はコメントだけ
- 冒頭の 1 は入れ替え実行の実測と合う。2 は限界 (a)(b) の実測と合い、原因の部分は「推定」と書かれている。`deviation.md:12` の記録どおり
- 駆動器が UI dispatcher も `Choreographer` も通らないことは、コードのとおり (`BroadcastFrameClock` と `Handler` への委譲)

`DateCalendarRecreationTest.kt:137-145` の rule の説明は、`exploration.md` の実測・単独実行の結果と合う。

### 設計品質

- コメント規約: `python3 scripts/comment-policy-lint.py` は禁止 0 件。差分のコメントに作業文書のパス・change 識別子・レビューの通番・履歴記述は無い。要確認の 1 件 (`DateCalendarRecreationTest.kt:40`) は差分の外
- Kotlin の観点: 問題は見当たらない。`catch (thrown: Throwable)` は流し切りを最後まで進めるためで、控えたものは必ず投げ直している
- 上限を件数で置くこと: `test-execution.md` の「上限は実時間の deadline」は非同期の収束を待つ場面の決まりで、ここは 1 反復が必ず 1 件を実行する同期の流し切りなので当たらない。理由は KDoc に書かれている
- オーバーエンジニアリング: 該当なし。合意した芯 (`deviation.md:7`) に収まっている
- 入力検証・認証認可・機密情報: テストコードのみで該当なし

## 指摘事項

### [🟡 Minor] 限界 (b) の「そのテストの中でも」は、先に Compose を使ったテストがあるときだけ成り立つ

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:77-79`、`deviation.md:8` の (b)

**問題点**: KDoc は「フレームの配信を止める API と Compose を併用すると、そのテストの中でも、同じプロセスの後続のテストでも、Compose の再 composition が届かなくなる」と条件なしで書く。実測では、プロセスで最初に Compose を使うテストが配信を止めた場合、そのテストの中では再 composition が届いた。

| 配信を止めたテスト | そのテストの中 (時計を 20 ms ずつ進めながら待つ) | 後続のテスト |
|---|---|---|
| プロセスで最初に Compose を使う | 届く | 届かない |
| 先に Compose を使ったテストがある | 届かない | 届かない |
| 同上 + 配信を再開して終える | 届かない (再開の後も) | 届かない |
| Compose を使わない | — | 届く |

クラスを単独で実行したときの 1 本目が前者に当たる。読んだ人が「併用しない」と判断する結論は変わらないので、優先度は低い。

**推奨修正**: 「そのテストの中でも」に条件を添える (先に Compose を使ったテストが同じプロセスにあるとき)。`deviation.md:8` と蒸留送りの `deviation.md:16` に引き継ぐ文も同じにそろえる。

### [🔵 Suggestion] 「閉じる処理をキューに残したまま終えるテストがあり」は、今のテストでは裏づけが取れない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt:42-44`

**問題点**: KDoc は、ダイアログを `dismiss` で閉じる理由を「閉じる処理をキューに残したまま終えるテストがあり」と、実在するものとして書く。`dismiss()` をやめて View 階層を直接外す改変でも、全件は 1401 / 0 のまま通り、流し切りの例外は 0 件だった。今のテストにその形は無い。仕組みそのものは、一時テストで記述のとおりに再現する。

同じ種類の箇所がもう 1 つある。`:98-100` の「次のメッセージへ回された選択面の提示など」も、ループの中の片付けを外した改変で全件が通るので、今のテストには当たるものが無い (こちらは「ことがあり」と書かれていて、言い切りではない)。

**推奨修正**: 任意。「終えるテストがあると」のように、起こりうる場面として書く。

## アクションプラン

1. KDoc の限界 (b) に条件を添え、`deviation.md:8`・`:16` の文をそろえる (指摘 1)。コメントと記録だけの変更
2. 指摘 2 は任意。手を入れるなら 1 とまとめて行う。コードの挙動は変えないので、確かめ直しは全件の実行で足りる

## 範囲外の所見 (蒸留送りの材料)

判定には含めていない。今のテストに存在しない使い方で起きることを、実測した事実だけ書く。

- 時計を大きく進めて終えたテストの後、本体の中で looper を回すテスト (`ActivityController.setup()` など) は落ちない。遅れの分を本体の中で流すので、打ち切りには達しない。代わりに遅くなる (2 時間進めた後は、後続の Compose を使うテストが 1 本あたり約 5 秒)。限界 (a) の打ち切りが起きるのは、looper を回さずに Compose のフレームを残して終えるテスト
- 限界 (a) で打ち切られた後は、印が 2 つとも下りていても、後続の再 composition が走らなかった (110 秒・150 秒の実測)
- 遅れは時計を進めたテストの直後だけでなく、同じ JVM のそれ以降のテストに残り続ける。今の全件でも、約 2 秒進めるテストの後、looper を回さずに Compose を残して終える 42 件が、終わりに毎回 2,100〜2,485 件を流している
- 終わらないアニメーションを含む Compose を Activity の中身にして `ActivityController.setup()` を呼ぶと、本体の中の `visible()` → `idle()` で固まる (スレッドダンプで確認)。土台は届かない。① と同じ形で、`recreate()` に限らない

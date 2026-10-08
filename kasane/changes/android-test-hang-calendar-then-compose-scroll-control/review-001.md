# レビュー結果: android-test-hang-calendar-then-compose-scroll-control (001 回目)

**日付**: 2026-10-07
**判定**: APPROVED

## サマリー

実装は exploration.md の「実装に渡す範囲」1〜5 に収まり、範囲外 (本体 `src/main/`・`kssettingsview-bridge` への共通の後片付け・他クラスの個別の書き換え) には触れていない。「完了の確かめ」の 4 項目はレビュー側で実行し直し、すべて記述どおりの結果になった。Critical / Major / Minor の指摘は無い。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・テスト結果を報告するとき)
- `kasane/lessons/code-review.md` (L-001: 検出力は改変して実測する)、`kasane/lessons/test.md`

core / android の handbook は存在しない (`kasane/handbook/index.md`)。

ロードしたスキル: ksn-review、kotlin-impl-skill

## 確認した観点

### ビルドとテスト (作業ツリーで実行)

| 実行 | 結果 |
|---|---|
| `./gradlew test --rerun-tasks --continue` | BUILD SUCCESSFUL。`kssettingsview` 1401 tests / 0 failures (debug)、1401 / 0 (release)。`kssettingsview-bridge` 193 / 0 (debug)、193 / 0 (release)。合計 1594 件 × 2 variant |
| `DateCalendarRecreationTest` 単独 (debug) | 19 tests / 0 failures。固まらない |
| `DateCalendarDialogTest` 単独 (debug) | 39 tests / 0 failures |
| `DateSelectionSheetTest` 単独 (debug) | 47 tests / 0 failures |

`kssettingsview` の 1401 件は exploration.md の変更前の実測と同じ件数。

### 完了の確かめの残り 2 項目と、コメントの記述の裏取り (作業ツリーの写しで実行)

作業ツリーは書き換えず、`android/` の写し (`build` を除く) に一時的な改変を入れて `:kssettingsview:testDebugUnitTest` を回した。写しは実験後に作業ツリーと差分が無い状態へ戻してある。

| 実験 | 改変 | 結果 |
|---|---|---|
| 実行順の入れ替え | `ComposeScrollRecreationTest` に必ず落ちるテストを足して 1 回実行 → 消して clean なしで全件 | `ComposeScrollRecreationTest` が先頭で走り、1401 tests / 0 failures。`ComposeScrollControlTest` の「状態の変更と同じ処理から出した命令は新しいツリーで解決される」は通った |
| 共通の後片付けを外す | 上の手順を `robolectric.properties` を除いた状態で行う | `ComposeScrollRecreationTest` が先頭で走り、1401 tests / 1 failure。落ちたのは同じテストで、表示は `待機条件が 5000 ms 以内に成立しなかった (processed=0 state=0 smooth=false layoutRequested=false ...)`。exploration.md「③ の観測」と一致 |
| 選択面を閉じる処理を外す | `DateSelectionSheetTest` の `@After` の中身を空にして単独実行 | 47 tests / 1 failure。Material モードのテストが 5.04 秒で `テストの終わりに、メインスレッドのキューが 5000 ms 以内に空にならなかった (... 表示中のダイアログ: [DateCalendarDialog, DateCalendarDialog])` で落ちた。固まらない |
| 本体の失敗との併発 | 上に加えて同じテストの本体を失敗させる | 報告されたのは本体の失敗だけ。`MainLooperDrainingApplication` のコメント「テスト本体が失敗しているときは、本体の失敗がそのまま報告される」のとおり |

2 つ目と 3 つ目は、共通の後片付けが実際に適用されていること (`src/test/resources/robolectric.properties` が読まれていること) と、2 クラスに足した「閉じる」が必要であることの検出力の確認を兼ねる。

### 仕様充足 (合意スコープとの照合)

- 範囲 1: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/MainLooperDrainingApplication.kt` と `android/kssettingsview/src/test/resources/robolectric.properties`。`onTerminate()` で main looper を空にし、なぜ要るかをコメントに書いている。「空にならないときに理由を載せて落とす」は実現済みで、制約 (同じ JVM で最初の 1 件しか落ちない) は deviation.md に記録され、クラスのコメントにも書かれている
- 範囲 2: `DateCalendarRecreationTest` に `@get:Rule` の `createEmptyComposeRule()` だけを足している。既存のテスト本体と後片付けは変えていない
- 範囲 3: `DateCalendarDialogTest`・`DateSelectionSheetTest` の `@After` で表示中のダイアログを閉じる。rule は足していない。閉じる処理はテスト本体の後に走るので、本体の検証 (閉じ切りの通知を数えるものを含む) には届かない。閉じる対象が「表示中のダイアログすべて」である点は deviation.md に記録済み
- 範囲 4: `ComposeFrameDriver.kt` の 2 つの写しは `package` 行を除いて同一 (`diff` で確認)。差分はコメントだけで、駆動器の動作は変わらない
- 範囲 5: `src/main/` と `kssettingsview-bridge` の動作に変更なし
- deviation.md の `[付随修正]` (内部 `MainLooperDispatcher` の KDoc): 本務で触るファイル内・コメントのみ・2 ファイルで、同梱条件に収まる
- 記録の無い逸脱は見つからなかった。S 級のため足場 (proposal / specs) は無い

### テスト

- 全テスト成功。テストの削除・無効化・アサーションの弱体化は無い (差分は rule 1 つと `@After` の追加だけ)
- `MainLooperDrainingApplication` の打ち切りは、反復回数ではなく実時間の deadline で区切り、超過時は実測値 (実行した件数・表示中のダイアログ) を載せて落とす。`test-execution.md`「収束を待つアサーション」の作法と整合する

### 設計品質

- コメント規約: `python3 scripts/comment-policy-lint.py` は禁止 0 件。差分のコメントに作業文書のパス・change 識別子・論点番号・履歴記述は無く、そのファイルだけで読める。`--advisory` が挙げる 3 件 (`DateCalendarDialogTest.kt:31` ほか) は差分の外の既存行
- 合意スコープに対する過剰な作り込みは無い。新しいクラスは 1 つで、期限付きの流し切り以外の仕組みを持たない
- Kotlin の観点 (null 安全・イディオム・コード衛生): 問題なし。Coroutines / Flow と永続化は差分に該当なし
- 入力検証・認証認可・機密情報: テストコードのみで該当なし

## 指摘事項

なし。

## アクションプラン

1. 修正は不要。このまま次の工程へ進められる
2. 蒸留では deviation.md と exploration.md の「蒸留時に反映」の行 (`kasane/handbook/cross/test-execution.md` の言い直し、件数の実測 1594 件 × 2 variant) を反映する

# レビュー結果: picker-selection-after-dismiss (002 回目)

**日付**: 2026-09-19
**判定**: CHANGES_REQUESTED

## サマリー

review-001 の Minor (構成変更後のカレンダー復元経路に `onValueCompleted` の回帰テストが無い) は**閉じた**。追加された 2 本は復元経路 (`KsSettingsView.restoreCalendarDialogIfPending`) を実際に通っており、ミューテーション実測で回帰検出力も確認できた。Android 全件はクリーンビルドから 2984 件 / 0 失敗で、実装者の報告と一致する。

差し戻すのは 1 点のみ。追加された 2 本の待機が `idle()` で書かれており、handbook `cross/test-execution.md`「収束を待つアサーション」と `cross/ADR-0027` (負の検証は名前で判別できる待機で書く) のどちらにも沿っていない。同じ 2 つのアサーションを持つ姉妹テスト `DateCalendarDialogTest` は `awaitMainLooperCondition` / `drainMainLooperForUnchangedCheck` で書かれており、後者はこの change 自身が deviation に記録して追加したヘルパである。2 行の機械的な置き換えで閉じる。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加された 2 本のコメント (`// 閉じ切りの通知は dismiss リスナー経由で届くため、...` と `recordingCell` の doc コメント) に禁止参照・禁止類型なし。`scripts/comment-policy-lint.py` は禁止 0 件 / 検査対象 811 ファイルで exit 0
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・テスト結果を報告するとき) — 「収束を待つアサーション」節と Android 節の 2 点に抵触。下の指摘事項に記す
- `kasane/decisions/cross/0027-negative-verification-fixed-wait-exception.md` (accepted) — 負の検証の待機は名前で判別できる形にする決定。抵触は下の指摘事項に記す
- 前回レビューで照合済みの規約 (`cross/runtime-behavior-verification.md` / `maui/integration-host-verification.md` / `ios/swift6-language-mode-check.md`) は、今サイクルの差分が Android のテスト 1 ファイルに閉じているため再照合の対象外とした

## 検証したこと (指摘に至らなかった確認)

- **テスト再実行 (完了判定)**: `cd android && ./gradlew clean && ./gradlew test --rerun-tasks` を通し、**BUILD SUCCESSFUL / 2984 件 / 0 失敗** (`kssettingsview` debug 1315・release 1315、`kssettingsview-bridge` debug 177・release 177 を JUnit XML で集計)。実装者の報告と一致する
- **回帰検出力のミューテーション実測** (lessons/code-review L-001): `KsSettingsView.kt:1282` の `dialog.completedDate?.let { newDate -> cell.onValueCompleted?.invoke(newDate) }` を落として `:kssettingsview:testDebugUnitTest` を実行したところ、**新規の肯定側 1 本だけが失敗**し (`expected:<[changed:2026-08-19, completed:2026-08-19]> but was:<[changed:2026-08-19]>`)、同クラスの他 17 本と全 1315 件の残りは通過した。前提アサーション (`changed:`) が通り争点のアサーションだけが落ちる形であり、検出力の証明として決定的。使用した一時変更は backup との shasum 一致で原状復帰を確認済み
- **復元経路を通っていること**: 上記ミューテーションが肯定側テストにだけ効くことが、このテストが `DatePickerCellViewHolder` 側の配線ではなく `restoreCalendarDialogIfPending` の配線を観測している裏取りになっている
- **取消側の不発火**: 追加された否定側 1 本は、`recordingCell` で `onValueChanged` / `onValueCompleted` の両方を観測したうえで取消後に空であることを確かめており、控えの誤設定と誤発火の両方向が固定されている。review-001 が求めた「取消でも発火しない」の担保は満たされている
- **足場の凍結**: `proposal.md` / `design.md` / `specs/` / `deviation.md` に今サイクルの差分なし。`tasks.md` にも差分なし。tracked な変更ファイルは前回と同じ 47 件 (1904 insertions / 86 deletions) で、テスト 1 ファイル以外に手が入っていない
- **deviation の追記要否**: 今回の追加はテストのみで、review-001 の裁定どおり新たな乖離ではないため deviation.md への追記は不要 (lessons/process L-009 の事後判定でも、名指しされていないファイルへの新規の手入れは無い)
- **単体クラス指定のハングは本サイクル起因ではない**: `./gradlew :kssettingsview:testDebugUnitTest --tests '*DateCalendarRecreationTest'` は 200 秒で戻らない (exit 124)。追加された 2 本と `recordingCell` を一時的に取り除いた状態でも同じくハングしたため、**既存の性質**であることを実測で確認した (取り除いた変更は shasum 一致で原状復帰済み)。モジュール単位以上での実行なら影響しない
- **実行環境の不安定さ (change とは無関係)**: 初回の全件実行で `:kssettingsview-bridge:testDebugUnitTest` が `java.io.EOFException`、`:kssettingsview:testReleaseUnitTest` が Robolectric sandbox の `ClassNotFoundException` で広域に落ちた (本 change と無関係な `KsImageTest` 等を含む)。handbook `cross/test-execution.md` が記す実行環境側の失敗形そのもので、`./gradlew --stop` と `clean` からの再実行で解消し 0 失敗になった。テストの失敗としては扱っていない

## 指摘事項

### [🟡 Minor / 優先度高] 追加された 2 本の待機が待機規約と ADR-0027 に沿っていない

**該当箇所**:
- `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarRecreationTest.kt:172` (肯定側の `idle()`)
- `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarRecreationTest.kt:185` (否定側の `idle()`)

**問題点**:

2 点あり、どちらも同じ 2 行に由来する。

1. **肯定側は収束待ちを `idle()` で書いている**。閉じ切り callback の到着は「待つべき正の完了条件」がある収束待ちであり、handbook `cross/test-execution.md`「収束を待つアサーション」は (a) 実時間 deadline (b) ループ内での実行機会の譲渡 (c) 超過時に実測値を載せた `fail()` の 3 条件をすべて満たす形で書くことを求めている。`idle()` はそのいずれも持たない。同節は「**手元で通ることは、この形で書けている根拠にならない**」と明記している。実害は診断品質にも出る — ミューテーション実測で落としたとき、このテストは `expected:<[...]> but was:<[...]>` の素の差分しか出さず、Robolectric が `Main looper has queued unexecuted runnables.` という別の示唆を添えるため、「実装が届いていない」のか「待ち足りない」のかが読み手に区別できない。`awaitMainLooperCondition` なら `メインスレッドの待機条件が 5000 ms 以内に成立しなかった (受け取った通知: [changed:...])` で切り分けが付く

2. **否定側は負の検証を `idle()` で書いている**。`cross/ADR-0027` (accepted) は負の検証の待機について「**呼び出し名から『不変性を確かめるための待機』と判別できる形にする**」ことを決定しており、handbook も「名前で区別できないと、後から読む人がその固定待機を『条件ベース化の直し漏れ』と見分けられない」と理由まで書いている。`idle()` は汎用のキュー消化であって、この判別可能性を持たない。加えて `idle()` は上限のない消化であるのに対し、この change 自身が `KsSettingsViewTestSupport.kt` に追加した `drainMainLooperForUnchangedCheck` は「Compose の選択面が自分を再投稿し続けるキューを持ち、流し切ろうとすると戻らなくなる」ことを理由に 1000 件で打ち切る形になっている。**この change が負の検証用にヘルパを足しておきながら、新しい負の検証でそれを使っていない**という内部矛盾が残る

review-001 が「`DateCalendarDialogTest.kt:247` 付近の書き方をそのまま流用できる」と示したその姉妹テストは、同一の 2 アサーションを次の形で書いている (`DateCalendarDialogTest.kt:262` / `:273`):

```kotlin
awaitMainLooperCondition(diagnostics = { "受け取った通知: $events" }) { events.size == 2 }
assertEquals(listOf("changed:2026-08-09", "completed:2026-08-09"), events)
...
drainMainLooperForUnchangedCheck()
assertTrue("取消で通知された: $cancelEvents", cancelEvents.isEmpty())
```

なお、本サイクルの全件実行は 0 失敗であり、この 2 行に起因する失敗・ハングは観測していない (単体クラス指定のハングは上に記したとおり本サイクル以前からの性質)。実害が顕在化していない点は事実として添えるが、accepted ADR と handbook の明文に対する逸脱であり、正しい形が同一ディレクトリの姉妹ファイルに存在して 2 行で置き換えられるため、このサイクル内で閉じてほしい (lessons/process L-005)。

**推奨修正**:

`DateCalendarRecreationTest.kt` の 2 行を姉妹テストと同じ形へ置き換える。

- 172 行目の `idle()` → `awaitMainLooperCondition(diagnostics = { "受け取った通知: $events" }) { events.size == 2 }`
- 185 行目の `idle()` → `drainMainLooperForUnchangedCheck()`

どちらのヘルパも同じ package の `KsSettingsViewTestSupport.kt` にあり、import の追加は要らない。置き換え後は `cd android && ./gradlew test --rerun-tasks` で全件を回し直し、件数と 0 失敗を報告する (単体クラス指定 `--tests` はこのクラスでは戻らないため使わない)。

## アクションプラン

1. **[必須]** `DateCalendarRecreationTest.kt:172` / `:185` の `idle()` を `awaitMainLooperCondition` / `drainMainLooperForUnchangedCheck` へ置き換え、モジュール単位以上で全件を回して報告する
2. 上記は待機の書き方の是正であり、deviation.md への追記は不要 (新たな乖離ではない)
3. 既知の先送り (tasks 6.1 と 7.3 の iOS 実機分はオーナー担当) と、前回 Suggestion 2 件の降格裁定は本レビューの対象外として扱った
4. (情報) `DateCalendarRecreationTest` は `--tests` の単体クラス指定で戻らない。本サイクル以前からの性質であることを実測で確認したので、修正するなら別 change の対象になる

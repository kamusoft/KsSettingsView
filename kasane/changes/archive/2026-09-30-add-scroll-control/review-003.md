# レビュー結果: add-scroll-control (003 回目)

**日付**: 2026-09-30
**判定**: APPROVED

## サマリー

前回の突き合わせ (second-opinion-code-002.md 末尾) で修正対象にした 2 件は、どちらも実装とテストで解消されている。1 件は相方 Major の「Android で `post` 待ちの命令が別の Host に届く」、もう 1 件はホスト Minor の「DSL の受け口が引き直し前の命令を捨てる経路の検出力」である。Android はスクラッチのコピーでミューテーションを入れ、各修正を外すと追加・改訂したテストがそれぞれ 1 件だけ落ちることを実測した (L-001)。

修正サイクルで入った変更を読み直した。`attach` の同じ受け口での早期リターンと、`post` 時の接続の同一性の判定である。新たな欠陥は見つからなかった。変更全体も改めて見たが、Critical / Major / Minor に当たる指摘は無い。足場 (proposal / design / specs) は書き換えられておらず、tasks.md の差分はチェックの更新だけである (6.3 は未チェックのまま。既知の未了)。

## 照合した規約

- cross/comment-policy (always)。変更ファイル 70 本に `scripts/comment-policy-lint.py --advisory` をかけ、禁止は 0 件だった。本サイクルで触れた 6 ファイル (`KsScrollController.kt`、両 OS の `DSLScrollCommandResolver`、`ScrollControlTest.kt`、`ComposeScrollControlTest.kt`、`DSLScrollControlTests.swift`) は、要確認も 0 件である。全体の要確認 33 件は、既存の公開 doc コメントの ADR 参照か、前回までに扱いを決めた型である
- cross/test-execution (テスト実行・結果の報告)。Android は `--rerun-tasks` で全件を実行し、件数は XML で集計した。MAUI も全件を実行した。本サイクルで足されたテストのうち、非同期の反映を待つものは条件ベースの待機、何も起きないことの確認は意図が名前で分かる固定待機 (`drainMainLooperForUnchangedCheck` / `waitForNegativeVerification`、cross/ADR-0027) で書かれている
- cross/diagnostic-message-language。本サイクルで新しく足された文言は無い
- cross/sample-parity・cross/runtime-behavior-verification・ios/swift6-language-mode-check・maui/integration-host-verification。本サイクルでは、`samples/`・binding 層・facade 層・アニメーションの経路に変更が無い (変更ファイルの更新時刻で確認した)。前回までの照合結果を引き継ぐ。連続フレームの撮り直しは、既知の未了として指摘から除いた
- lessons/code-review L-001、lessons/process L-002 / L-005 / L-006 / L-009

## 確認した事実 (ビルド・テスト)

- Android: `./gradlew test --rerun-tasks` を再実行した。kssettingsview 1380 × 2、bridge 193 × 2 の計 3146 tests / 0 failures / 0 errors (test-results の XML を集計)。ホスト側の報告と一致する
- MAUI: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` を再実行した。624 tests / 0 failures
- iOS: Simulator を起動しない制約があるため再実行していない。ホスト側の 1184 tests / 0 failures と Swift 6 言語モードの error 0 を事実として扱った
- `scripts/local-path-lint.py`・`scripts/identity-lint.py` の違反は 0 件
- ミューテーションによる検出力の実測 (L-001): android/ をスクラッチへ複製し、次の 2 つを同時に入れて、`ScrollControlTest` と `ComposeScrollControlTest` を実行した。作業ツリーのソースには手を入れていない
  - `KsScrollController.kt` の `post` の中身を、実行時に `receiver` を読み直す修正前の形に戻す。落ちたのは `メインスレッドへ回した命令は実行前につなぎ替えるとどの Host も動かさない` だけである (「A に向けた命令は B にも届かない expected:<0> but was:<1>」)
  - Android の `DSLScrollCommandResolver.connect` から `pending.clear()` を外す。落ちたのは `引き直す前にハンドルを差し替えるとその命令はどの Host も動かさない` だけである (「外したハンドルの引き直し前の命令は Host へ届かない expected:<0> but was:<1>」)
  - 他の 50 件は通った。前提のアサーション (「前提: 命令はまだ A に届いていない」) を通過したうえで、争点のアサーションだけが落ちている

## 前回指摘の解消状況

| 指摘 (出典・採否) | 状況 | 根拠 |
|---|---|---|
| Android で `post` 待ちの命令が別の Host に届く (相方 Major・採用) | 解消 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsScrollController.kt:63-77`。`send` は、命令を出した時点の `WeakReference` を `connection` として控える。`post` の先では、`receiver === connection` のときだけ受け口へ渡す。`attach` / `detach` は接続のたびに `WeakReference` を作り直すか `null` にするので、A を外して B へつなぎ替えれば同一性が崩れて捨てられる。`attach` は同じ受け口への接続し直しで早期に戻る (:83)。これで同じ接続が続く限り命令は届く。回帰テストは `ScrollControlTest.kt:157`。前提として、つなぎ替えの時点で A が未処理であることを確かめている。そのうえで、A と B のどちらも処理件数 0・位置不変であること、つなぎ替えた後の命令が B に届くことを確かめる。ミューテーションで検出力を実測した。deviation.md 末尾の行に扱いが記録されている |
| DSL の受け口の「引き直し前の命令を捨てる」処理を単独で守るテストが無い (ホスト Minor・確定) | 解消 | Android: `ComposeScrollControlTest.kt:318` は `runOnUiThread` の中で `scrollToEnd` → `resolver.connect(second)` → `resolver.resolvePending()` の順に呼ぶ。命令を受け口に残したまま差し替えるので、(a) の経路を通る。`pending.clear()` を外すと落ちることを実測した。iOS: `DSLScrollControlTests.swift:350,386` は `scrollToEnd` の直後、`main.async` を経る前に `resolver.connect(second)` / `resolver.connect(nil)` を同期で呼ぶ。SwiftUI の更新の順序に依らず (a) を通る形になった。後から来る SwiftUI の更新による `connect` は同じハンドルなので早期に戻り、テストの前提を崩さない。iOS のミューテーションは制約により実測していないが、`resolvePending` は予約済みの `main.async` で必ず走る。`pending.removeAll()` を外せば命令が Host へ渡り、処理件数が 1 になる経路はコードから一意に読める |
| 対象の無い命令が先行のアニメーションを止める・SwiftUI DSL の保留命令 (相方 Major 1・2、code-001) | 解消済み (再確認) | 前回解消と確定したコード (`KsSettingsViewScrollControl.kt:156-159`、`KsSettingsViewController+ScrollControl.swift:257-262`、`DSLScrollCommandResolver.swift:52-61`) は、本サイクルで後退していない |
| Android `onLayout` ごとの既定 id の走査、Bridge `onPreDraw` の毎フレームの一時リスト (ホスト Suggestion・降格、code-001) | 修正対象外 | 降格済み。再度は指摘しない |

## 指摘事項

なし。

## 確認した観点 (指摘なし)

- **修正で入った変更の副作用**
  - `attach` の早期リターンは、同じ受け口がまだ生きている (`get()` が同じ) ときだけ効く。受け口が回収された後の接続し直しは新しい接続になる。Host の `scrollController` の setter と DSL の受け口の `connect` は同じ値で先に戻るので、早期リターンに届くのは、同じ受け口への直接の再接続だけである。既存の接続の規則 (最後の接続が勝つ・置き換え時の警告・同一のときだけ外す) は変わらない
  - `send` の、未接続時の早期リターンとメインスレッド判定の位置は前回と同じである。strictMode の既定の挙動 (メインスレッド以外からは例外) も変わらない
  - 「外して同じ Host へつなぎ直す」と、`post` 待ちの命令は捨てられる。外した時点で Host の setter が未実行の命令を捨てる扱い (deviation 記録済み) と同じ向きで、一貫している
- **MAUI の順序保証の前提**: MAUI facade は、内容・可視性・Section の差し替えを dispatcher で 1 回遅らせて配信する (`KsSettingsController.ScheduleFlush`)。スクロール命令は同期で gateway へ渡る。両 OS とも、MAUI の dispatcher と Native Host の 1 回の遅延は同じメインキューへ FIFO で積まれる。そのため、同じ処理で先に出した可視性の変更は、Native の遅延が明けるより先に Store へ届く。Android では、Store の通知の collect が `lifecycleScope` (Main.immediate) の上で同期に走る。iOS では Store から apply までが同期である。どちらも命令は反映の後に実行される。現行の設計は破れていない (テストで直接は押さえていない前提として記録しておく)
- **全体の再読** (Native の待ち行列・位置の解決・一方向の着地、SavedState の読み書きの順序、Bridge の位置追跡と控えの受け渡し、MAUI の解決順と準備完了の世代管理): 前回までの確認結果と食い違う点は無かった
- **足場の凍結と deviation**: proposal / design / specs に差分は無く、tasks.md の差分はチェックの更新だけだった。本サイクルでは、`post` 待ちの命令を捨てる扱いが deviation.md の末尾 1 行に記録されている。本サイクルで触れたファイルはいずれも tasks が名指す範囲か、既に deviation に記録された箇所で、L-009 の漏れは無い

## アクションプラン

1. (既知の未了) tasks.md 6.3 の連続フレームの撮り直し (Android の着地 3 か所、iOS の「Section へ」) を済ませ、チェックする

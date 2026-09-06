# レビュー結果: cell-style-dark-appearance-parity (003 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

修正サイクル 2 で残っていた 2 件 (Major 1 の観測点差し替え・Minor 1 の待機述語の null 安全化) はいずれも閉じている。核心の Major 1 は、`assertSame` を捨てて Adapter が発行した通知列の観測へ移す形が採られており、**review-002 が名指しした退行を実際に検出する**ことをミューテーション probe で実測した (`areItemsTheSame` の CellRow 分岐を全等価比較へ退行させると両テストが `[removed, inserted]` を検出して落ちる)。あわせて payload 無し `notifyItemChanged` への退行でも両テストが落ちることを確認しており、内容更新経路の 2 つの壊れ方が塞がっている。`ChangeRecordingObserver` の拡張は加算のみで、既存利用者 2 ファイル (`ContentUpdatePayloadTest` / `RootHeaderFooterAdapterTest`) が読む `changedPositions` / `payloads` の意味は変わっていない。Android 2854 件 / 失敗 0 を独立に再実行して確認した。Critical / Major は無い。

## 実行したテスト

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ./gradlew test --rerun-tasks` | 2854 件 / 失敗 0 / エラー 0 (`*/build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の `tests` / `failures` / `errors` 属性の合計。結果 XML 222 本) |
| Android (再現性) | `./gradlew :kssettingsview:testDebugUnitTest --rerun-tasks --tests '*DSLCellStyleUpdateTest' --tests '*StoreCellStyleReplaceTest'` を 3 回 | 3 回とも成功 (通知列の観測が間欠的にならないことの確認) |
| iOS / MAUI | 未実行 | 本サイクルで `ios/` / `maui/` のファイルは 1 件も変更されていない (作業ツリーの更新時刻で確認。review-002 の時刻より後に動いたのは Android テスト 3 本と `tasks.md` / `deviation.md` のみ)。verify-001 の iOS 1027 件 / 失敗 0 と review-002 の MAUI 520 件 / 失敗 0 が有効 |

## 照合した規約

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` (always) | 常時 | 適合。`python3 scripts/comment-policy-lint.py --advisory` は禁止 0 件 / 要確認 164 件で、本サイクルが触った 3 ファイルはいずれも要確認にも挙がらない。新規・改訂コメントに change 名・レビュー通番・タスク番号・デルタスペック構文キーワードの持ち込みは無く、`ChangeRecordingObserver` の KDoc は `RecycledViewPool` / `supportsChangeAnimations` というコード識別子だけで理由を説明していてファイル単独で読める |
| `kasane/handbook/cross/test-execution.md` | テスト実行・結果報告 / 収束を待つアサーション | 適合。両テストの待機は 3 条件 (実時間 deadline + ループ内 `Thread.sleep(1)` + 超過時の実測値付き `fail()`) を保っている。件数は `tasks.md` 8.1 が 2854 件へ追随済みで、本レビューの再実行値と一致 |
| `kasane/lessons/code-review.md` L-001 | 回帰検出力が争点になったとき | 適用した。判定は静的読解ではなく 2 通りのミューテーション probe の実測に基づく (下記) |
| `kotlin-impl-skill` (config `domain-skills.android.code-review`) | Android ドメインの実装レビュー | 本サイクルの差分に言語レベルの所見なし (`filterNot` による派生プロパティ・`singleOrNull()` による null 安全化はいずれも慣用形) |

`always` 以外では上記 2 本のみが担当範囲に当たる (本サイクルの差分は Android のテストコード 3 ファイルに限られ、`ios/` `maui/` `samples/` `skills/` `**/build.gradle.kts` のいずれにも触れていない)。

## 指摘事項

無し (Critical 0 / Major 0 / Minor 0 / Suggestion 0)。

## 確認した観点 (指摘に至らなかったもの)

### 1. 名指しされた退行を検出するか — probe で実測 (Major 1 の閉栓確認)

review-002 が「検出しない」と実証した退行を、同じ手で再現して検証した。改変前のファイルは scratchpad へ退避し、復元後に `shasum` 一致と `git status` / `git diff --stat` の差分 0 で原状復帰を確認している (`git checkout` / `stash` / `reset` は使っていない)。

| probe (`ui/KsSettingsListAdapter.kt` への 1 行改変) | 期待 | 実測 |
|---|---|---|
| `areItemsTheSame` の CellRow 分岐 `oldItem.cell.id == newItem.cell.id` → `oldItem.cell == newItem.cell` (style 更新が remove + insert へ退行) | 両テストが落ちる | **両テストが落ちた**。DSL 側 `expected:<[]> but was:<[removed(1,1), inserted(1,1)]>`、Store 側 `expected:<[]> but was:<[removed(0,1), inserted(0,1)]>` |
| `notifyItemChanged(position, KsSettingsView.PAYLOAD_CONTENT)` → `notifyItemChanged(position)` (payload 落ち) | 両テストが落ちる | **両テストが落ちた**。DSL 側 `style の差分が内容更新として発行されていない (通知列: [changed(1,1,payload=null)])`、Store 側は同文で `[changed(0,1,payload=null)]`。既存の `ContentUpdatePayloadTest` 2 件も同時に落ち、`RootHeaderFooterAdapterTest` は影響を受けず通過した |
| 改変なし (復元後) | 両テストが通る | 4 クラス 15 件すべて成功 |

review-002 が `assertSame` について実証した「両方の退行で緑のまま」という穴は、2 つの assert (`structuralNotifications` が空 / `payloads` に `PAYLOAD_CONTENT`) の組で塞がった。失敗メッセージにも実測の通知列が載るため、落ちたときに「どちらの壊れ方か」が読める。

### 2. `ChangeRecordingObserver` の拡張は既存利用者の意味を変えていないか

拡張は**加算のみ**である。`onItemRangeChanged` の本体は `changedPositions` / `payloads` への追加をそのまま残し、末尾に `notifications` への追加が 1 行増えただけで、既存の 2 つのリストへ入る値と順序は変わらない。新規の override (`onChanged` / `onItemRangeInserted` / `onItemRangeRemoved` / `onItemRangeMoved`) はいずれも `notifications` にしか書かない。

既存利用者は `ContentUpdatePayloadTest.kt:50,71` と `RootHeaderFooterAdapterTest.kt:90,101` の 4 箇所で、読むのは `changedPositions` と `payloads` だけ。全件実行 (2854 件) と probe 2 の targeted 実行の双方で、これらのクラスの結果が改変前と一致することを確認した (probe 2 で `ContentUpdatePayloadTest` が落ちたのは実装側を payload 無しへ壊したためで、観測側の変化ではない)。

`RootHeaderFooterAdapterTest` が別に持つ `TestObserver` は 2 引数版 `onItemRangeChanged` を override した独立クラスで、今回の拡張とは無関係。

### 3. Observer の登録・解除のタイミング

両テストとも、**初期表示が収束してから登録し、更新の収束を待ってから解除**している。

- `DSLCellStyleUpdateTest.kt:67-75`: `awaitTitleColor(lightTitle)` で初期描画の収束を待った後に登録 → `useDarkSide = true` → `awaitTitleColor(darkTitle)` → 解除
- `StoreCellStyleReplaceTest.kt:73-85`: `showView` (行のコミットと layout まで) と初期色の assert の後に登録 → `replaceCell` → `awaitConvergence` → 解除

解除位置が「収束後」であることに取りこぼしは無い。内容更新の通知は `submitListAndNotifyContent` のコミット callback (`ui/KsSettingsListAdapter.kt:107-116`) で発行され、行の再 bind はその後の layout で起きる。つまり**色の収束は通知の発行より必ず後**で、収束を待って解除すれば対象の通知は記録済みになる。登録位置も同様で、初期コミットは登録前に完了しているため初期の `inserted` を拾って偽陽性になることはない。3 回の反復実行でも間欠は出なかった。

### 4. 待機が handbook の 3 条件を保っているか (Minor 1 の閉栓確認)

`StoreCellStyleReplaceTest` の述語と診断は、`titleColorOfRow(view)` を `Int?` を返す形 (`labelRowHolder(view)?.views?.titleView?.currentTextColor`、`:137`) へ変え、`labelRowHolder` も `singleOrNull()` (`:145`) になった。行が無い瞬間は述語が `false`、診断は `現在の title 色: null` となり、**待機ループが例外で中断する経路は消えた**。最終アサーションだけが `requireTitleColorOfRow` (`:141`) で非 null を要求する形で、姉妹テスト `DSLCellStyleUpdateTest` (`:104,108`) と流儀が揃った。

待機本体は両テストとも変わっていない。DSL 側は自前ループ (`:99-116`) で実時間 deadline・`Thread.sleep(1)`・超過時に現在色と行一覧を載せた `fail()` を持ち、Store 側は `KsSettingsViewTestSupport.awaitConvergence` (`:41-63`) を使う。いずれも 3 条件を満たす。

### 5. 検討したが指摘しなかったもの

- **`structuralNotifications` が文字列の接頭辞 (`changed(`) で内容更新を判別している点**: 型ではなく整形済み文字列に依存する形ではあるが、`changed(` を生む経路は `onItemRangeChanged` の 1 本だけで、新しい override を足す人が同じ接頭辞を選ぶ余地は実質無い。失敗メッセージに列そのものが載る利点のほうが大きく、実害の証拠が無いため指摘しない
- **失敗時に `unregisterAdapterDataObserver` へ到達しない点**: 待機が `fail()` で落ちると解除されないが、observer も adapter もテストごとに作られて捨てられるため、他のテストへ漏れる経路が無い
- **DSL 側で `payloads.contains(...)` と等値比較のどちらを使うか**: 内容更新は 1 回とは限らない経路 (再 composition が複数回走る) なので `contains` が妥当。通知の宛先が誤った行だった場合は色が収束せず `awaitTitleColor` 側で落ちるため、position の assert が無いことは穴にならない

### 6. 逆流と作業範囲

作業ツリーの更新時刻から、review-002 (19:54) 以降に動いたのは `KsSettingsViewTestSupport.kt` / `DSLCellStyleUpdateTest.kt` / `StoreCellStyleReplaceTest.kt` / `tasks.md` / `deviation.md` の 5 本のみ。`specs/` / `proposal.md` / `ui/brief.md` は 18:10 以前のままで、**足場の書き換えは無い**。実装コード (`src/main/`) も本サイクルでは 1 行も動いていない (probe の改変は復元済み)。

### 7. 記録の追随

- `tasks.md` 8.1 (review-002 Minor 2) は「本体 1254 件 × 2 + bridge 173 件 × 2 = 計 2854 件」へ更新され、修正サイクル 2 後の再実行で件数の増減が無いことも併記されている。本レビューの実測 (2854 件 / 失敗 0) と一致する
- `deviation.md` に「3.1 / 3.2 (レビュー修正)」として観測点を通知列へ移した理由と、probe による裏付けが記録されている。記述は本レビューの実測と一致する

## アクションプラン

無し。蒸留 (ksn-distill) へ進んでよい。申し送りは `tasks.md` の「蒸留への申し送り」節に既にまとまっている。

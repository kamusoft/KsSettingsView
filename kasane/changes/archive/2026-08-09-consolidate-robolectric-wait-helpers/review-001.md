# レビュー結果: consolidate-robolectric-wait-helpers (001 回目)

**日付**: 2026-08-09
**判定**: APPROVED

## サマリー

exploration.md の決定事項どおり、4関数セット (`idle` / `awaitConvergence` / `committedTexts` / `visibleRowTexts`) が `KsSettingsViewTestSupport.kt` 1ファイルへ集約され、6ファイルから重複定義が消えている。スコープ外候補 (`HostActivity` / `layoutSettingsView` / `cellTitles` / `collectTextViews`) には手が入っておらず、S 級の境界は守られている。Android 全件テストは `./gradlew test --rerun-tasks` で 2024 tests / 0 failures / 0 errors、`comment-policy-lint.py` は 0 件。

等価性は静的読解に留めず実測した (下記「確認した観点」)。`committedTexts` の完全版統一は Section header/footer を持たない `StoreUnbindTest` に対して上位互換であることを確認済みで、exploration の未決論点は解消している。指摘は Minor 1 件・Suggestion 3 件で、いずれも失敗経路の診断品質と後続の再重複防止に関するもの。実装の正しさを損なう問題は見つからなかった。

## 指摘事項

### [🟡 Minor] AdapterReattachTest の待機失敗時の診断が実質的に劣化している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt:46-49`

**問題点**:

集約前の `AdapterReattachTest.awaitConvergence` は失敗メッセージに `内部 root: ${cellTitles(view)}` を載せていた。共有版はこれを `Theme: ${view.internalTheme()}` に置き換えている。

exploration.md は「差分は些細 (失敗メッセージに Theme を含めるか…)」(L12) と整理し、決定事項 L30 で「情報量の多い側 (Theme 併記) に寄せる」としているが、この前提は `AdapterReattachTest` が持っていた `内部 root` 変種を勘定に入れていない。実際にタイムアウト経路を走らせて得たメッセージは次のとおり (一時プローブでの実測、プローブは削除済み):

```
収束の待機条件が 200 ms 以内に成立しなかった (コミット済みリスト: [見出し, A, 脚注] / Theme: Theme(separatorColor=Color(0.784…), backgroundColor=…, cellBackgroundColor=…, （中略、全 30 フィールド）…, cellIconRadius=null))
```

`Theme` は data class の全フィールド展開で約 1200 文字に達し、判別に効く「コミット済みリスト」がその手前に埋もれる。`AdapterReattachTest` は detach 中に Store 更新が届かないこと / 再 attach で届くことを検証する性質上、時間切れ時に切り分けたいのは「Store → Host の root に届いているか」対「Adapter がコミットしたか」であり、その切り分けに効いていたのが失われた `cellTitles` (簡潔な `[A, B2]` 形式) である。差し引きでは、簡潔かつ判別力のある情報が、長大で当該テストの争点ではない情報に置き換わっている。

実害はタイムアウト時の診断のみ (テストの合否判定・待機挙動には影響しない) のため優先度は低い。`UnknownSectionAccessoryHostTest` は Theme が増えただけで損失はない。

**推奨修正**:

いずれか一方で足りる。

- `cellTitles` 相当 (`view.internalRoot()` の Cell title 列) を共有ヘルパへ移し、失敗メッセージに常に併記する。`cellTitles` は `AttachOrderRestoreTest.kt:83-86` と `AdapterReattachTest.kt:93-96` でバイト一致の重複でもあるため、集約の副次的な整理にもなる (ただしスコープ拡大になるため、オーナー判断で別変更に回してもよい)
- あるいは `awaitConvergence` に `extraDiagnostics: () -> String = { "" }` を足し、呼び出し側が必要な診断を渡せるようにする。合わせて `Theme` は全体 dump ではなく判別に効くフィールド (例: `backgroundColor`) に絞ると、メッセージ全体が読める長さに収まる

### [🔵 Suggestion] ファイル冒頭の説明が KDoc ではないのに KDoc リンク記法を使っている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt:12-17`

**問題点**: import 直後のブロックコメント (`/* … */`) 内で `[idle]` / `[awaitConvergence]` / `[committedTexts]` / `[visibleRowTexts]` という KDoc のリンク記法を使っているが、KDoc ではないためリンクとして解決されない。なお、この位置で `/** */` に変えると直後の `idle()` の doc comment として吸われてしまうので、ブロックコメントを選んだこと自体は妥当。

**推奨修正**: 角括弧を外して素の識別子名で書くか、ファイル概要をブロックコメントのまま `package` 宣言より前へ移す。

### [🔵 Suggestion] 同一パッケージの他テストに残るインライン `shadowOf(Looper.getMainLooper()).idle()`

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ContentUpdatePayloadTest.kt:56` ほか (`FullUpdateContentSyncTest` / `PickerDialogRecreationTest` / `DatePickerDialogIntegrationTest` / `DatePickerTodayShortcutTest` / `KsWheelViewTest` 等、同パッケージに約 30 箇所)

**問題点**: 本変更のスコープは「`fun idle` 等の関数定義の重複」であり、これらのインライン呼び出しは元々対象外なので違反ではない。ただし共有 `idle()` が同じパッケージに存在することを知らない書き手は、次のテストでも `shadowOf(Looper.getMainLooper()).idle()` を素で書き、やがて `private fun idle()` を再び切り出す。本変更が防ごうとした再重複の芽がそのまま残っている。

**推奨修正**: 別変更としてインライン呼び出しを共有 `idle()` へ寄せる。`idleFor(Duration)` を使っている箇所 (`KsWheelViewTest` / `DateSelectionSheetTest` 等) は意味が異なるので対象外にする。

### [🔵 Suggestion] レビュー対象外の作業ツリー変更が混在している

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md`、`docs/core-model.md`

**問題点**: レビュー開始時点の `git status` には無かった 2 ファイルの変更が、レビュー中 (ファイル更新時刻 19:26 / 19:29) に作業ツリーへ現れた。内容は docs-refresh 系の作業であり本変更とは無関係のため、レビュー対象からは除外した (未レビュー)。「working tree 変更一式」をそのままコミットすると、本変更に無関係な docs 変更が同一コミットへ混入する。

**推奨修正**: コミット時に `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/` 配下の 7 ファイルだけをパス指定で `git add` する。docs 側の 2 ファイルは別途 docs-refresh の流れで扱う。

## 確認した観点

指摘に至らなかったが確認した点を残す。

**リファクタとしての等価性**

- `committedTexts` の完全版統一 — `StoreUnbindTest` の root は Section header/footer を持たない (`StoreUnbindTest.kt:56-66`) ため、header/footer 分岐が増えても結果は Cell 行のみで不変。`awaitConvergence` の待機条件 (`StoreUnbindTest.kt:90,95,122,142,154`) は完全一致比較であり、header/footer が混入すれば必ず時間切れになるが実行は green。exploration の未決論点「簡略版を完全版に統一してよいか」は解消
- 一時プローブで共有 `committedTexts` の観測順序を実測 — header / cell / footer を上から順に `[見出し, A, 脚注]` として拾い、`SectionAccessory.Text` 以外と `LabelCell` 以外を落とす挙動が集約前の完全版と一致
- `awaitConvergence` の時間切れ経路 — グリーン実行では一度も通らないため一時プローブで実測。`AssertionError` が投げられ、メッセージが組み立てられることを確認 (診断内容の指摘は Minor へ)
- `flushMainQueue()` → `idle()` の改名 (`AttachOrderRestoreTest.kt:127,157,211,221`) は本体が同一で待機挙動は不変
- `visibleRowTexts` / `collectTexts` は 4 ファイルすべてで元からロジック一致。中間変数の有無だけの差
- `InitialThemeDecorationTest` は「なぜ `awaitConvergence` ではなく `idle()` で足りるか」の根拠 (検証対象が bind / attach 内で同期更新される) をクラス KDoc へ移して保存している (`InitialThemeDecorationTest.kt:35-37`)。ヘルパ削除で理由が消える典型的な劣化を避けている

**スコープ遵守**

- 決定事項の 4 関数以外 (`HostActivity` / `layoutSettingsView` / `cellTitles` / `rowTextColor` / `collectTextViews`) は各テストにローカルのまま。スコープ肥大なし
- 取り残しなし — exploration が挙げた 6 ファイルすべてでローカル定義が消えており、`fun idle` / `fun awaitConvergence` / `fun committedTexts` / `fun visibleRowTexts` / `fun collectTexts` の定義は ui テスト内で `KsSettingsViewTestSupport.kt` のみ (bridge の `KsBridgeTestHost.collectTexts` は別モジュール・別用途で対象外)
- Gradle 構成・`testFixtures` に変更なし (案 A どおり)。production コードへの変更なし
- 削除に伴う未使用 import の取り残しなし (`ViewGroup` / `TextView` / `fail` / `shadowOf` / `TimeUnit` / `Looper` が不要になったファイルからのみ除去され、`AttachOrderRestoreTest` の `ViewGroup` / `TextView` は `collectTextViews` が今も使用中)

**テスト実行** (cross/conventions/test-execution.md 準拠)

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL、**2024 tests / 0 failures / 0 errors / 0 skipped** (`*/build/test-results/*/TEST-*.xml` 集計、debug + release 両 variant)
- 初回の `./gradlew :ks-settingsview-ui:test` は全タスク UP-TO-DATE で 0 件実行だったため、`--rerun-tasks` で実行を確定させたうえで件数を確認している

**コメント規約**

- `python3 scripts/comment-policy-lint.py` → 検査対象 478 ファイル / 禁止 0 件
- 新設ファイルのコメントに変更提案 ID・フェーズ番号・アーカイブ文書パス・デルタスペック構文キーワードの混入なし。`submitList` の差分計算がバックグラウンドへ回る理由など、ファイル単独で意味が通る説明になっている

**レビュー手法の原状復帰**

- 一時プローブ (`ZzReviewProbeTest.kt`) は `trash` で削除済み。レビュー対象 7 ファイルは baseline との `shasum -c` 全件 OK で、レビューによる改変がないことを確認

## アクションプラン

1. (任意) Minor: `awaitConvergence` の失敗メッセージを、簡潔で判別に効く形へ戻す — `internalRoot()` 由来の Cell title 併記、または `extraDiagnostics` 引数の追加と `Theme` 出力の絞り込み
2. (任意) Suggestion: `KsSettingsViewTestSupport.kt` 冒頭コメントの `[...]` 記法を素の識別子名へ
3. コミット時に本変更の 7 ファイルのみをパス指定で staging し、docs 側 2 ファイルの混入を避ける
4. 後続変更の候補: 同パッケージのインライン `shadowOf(Looper.getMainLooper()).idle()` を共有 `idle()` へ寄せる / `cellTitles` の 2 ファイル重複を集約する

# レビュー結果: perf-android-customcell-composition-reuse (001 回目)

**日付**: 2026-08-16
**判定**: CHANGES_REQUESTED

## サマリー

実装本体 (`ComposeCellViewHolder` の pool-aware 破棄戦略 + `CustomCellViewHolder` の `ReusableContentHost` / `ReusableContent` + state 経由更新) は android/ADR-0015 の決定どおりで、デルタスペックの全 Scenario に対応するテストが揃っている。全モジュール全件テストも成功している (`./gradlew test --rerun-tasks`: debug 1171 / release 1171、計 2342 件・失敗 0)。新設テストの回帰検出力も本レビューで独立にミューテーションを当てて実測し、機構の中核 (破棄戦略・`ReusableContent`・cellId・reset の deactivate) がいずれもテストで固定されていることを確認した。

一方で、**機構の成立を実環境で裏取りする tasks 3.1 (実機高速フリック検証) が未実施**であり、これは形式的な残タスクではない — 新設テストはすべてテスト専用 Recomposer (`ComposeFrameDriver`) にフレームを手動送出させて駆動しており、production の Choreographer 駆動で「プール滞在中の detach 済み ComposeView が実際に再 composition されて deactivate が走るか」は検証範囲の外にある。ADR が Consequences として明示的に要求している再検証でもある。加えて tasks 2.7 (検出力確認) はチェック済みだが結果の記録がどこにも存在しない。以上により CHANGES_REQUESTED とする。コード品質そのものへの指摘は Minor 以下に留まる。

## 確認した観点

- ビルド・テスト: `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。実行件数 debug 1171 / release 1171、failures 0 / errors 0 / skipped 0 (test-execution 規約に従い XML 集計で確認)。新設テストクラスの実行も個別に確認 (`CustomCellRecycleTest` 8 件 / `CustomCellBuilderReleaseTest` 1 件 / `KsBridgeCustomCellDeactivateTest` 2 件 / 既存 `CustomCellRenderingTest` 25 件 / `KsBridgeCustomCellTest` 20 件、いずれも失敗 0)
- 足場アーティファクトの逆流: なし (`git log` / `git diff` 上、change 配下の変更は tasks.md のチェックのみ)
- 破棄戦略の影響範囲: `ComposeCellViewHolder` の派生は `CustomCellViewHolder` のみ (grep 済み)。他 Cell 種別への波及なし
- 依存の前提: recyclerview 1.3.2 (PoolingContainer 対応) / compose-bom 2024.10.01。`DisposeOnDetachedFromWindowOrReleasedFromPool` と `ReusableContent` / `ReusableContentHost` はいずれも安定 public API
- ソースコメント規約 (cross/conventions/comment-policy.md): `python3 scripts/comment-policy-lint.py --summary` → 禁止 0 件。追跡外の新規テスト 3 ファイルも手動 grep で禁止参照なしを確認。ADR 参照はすべて許容形式 (`android/ADR-0015`)
- 公開 API 変更: なし (`internal` クラスの内部構成変更に閉じている)

### 回帰検出力の実測 (lessons code-review L-001)

tasks 2.7 の結果記録が存在しないため、レビュー側で独立にミューテーションを当てて実測した (実行対象: `CustomCellRecycleTest` / `CustomCellRenderingTest` / `CustomCellBuilderReleaseTest` / `KsBridgeCustomCell*`、計 34 + 22 件)。

| ミューテーション | 落ちたテスト |
|---|---|
| (a) 破棄戦略を `DisposeOnDetachedFromWindow` へ戻す | `行がプールへ入っても Composition は生存しプール放逐で破棄される` / `itemViewCache 経由の再表示では content の状態と購読が維持される` / `同一ラップ関数 builder 間で埋め込み View が再利用される` / Bridge `リサイクルを挟んだ再表示で同一 platform view が再親付けされる` (計 4 件) |
| (b) `ReusableContent(key)` を `key(...)` へ置換 | `同一ラップ関数 builder 間で埋め込み View が再利用される` (1 件) |
| (c) `contentKey` を固定値にする | `間に再 composition を挟まない再 bind でも remember が持ち越されない` (1 件) |
| (d) `reset()` から `isContentActive.value = false` を削除 | `同一ラップ関数 builder 間で埋め込み View が再利用される` (1 件) |

いずれのミューテーションでも前提アサーションは通過し、争点のアサーションだけが落ちた。トートロジーではなく実際に機構を固定していると判断する。使用した一時変更は backup との `shasum` 一致で原状復帰を確認済み (`git status` の変更ファイル集合もレビュー開始時と同一)。

なお (c) は `別 Cell への再 bind では remember が持ち越されず DisposableEffect が dispose される` では検出されない — プール経由の経路は deactivate/reactivate が隔離を担うため cellId に依存しない。tasks 2.4 の「同一フレーム直接 bind」テストが cellId を固定する唯一のゲートであり、この 2 本を両方持つ設計は妥当。

## 指摘事項

### [🟠 Major] 実機検証 (tasks 3.1) が未実施で、機構の中核が実環境で裏取りされていない

**該当箇所**: `kasane/changes/perf-android-customcell-composition-reuse/tasks.md:28` (未チェック)、`verification-device.md` 不在

**問題点**:
android/ADR-0015 は Consequences に「deactivate 時に `AndroidViewHolder.onDeactivate` (removeAllViewsInLayout) という従来走らなかった経路が Bridge 埋め込みに対して実行される。理論上は親側操作のみで安全だが、実機での高速フリック再検証を要する」と明記しており、proposal も証跡を change 配下に残すと宣言している。この検証が未実施。

これは単なる手続き上の残タスクではない。新設テストは 3 クラスすべてが `ComposeFrameDriver` (テスト専用の `Recomposer` を親 `CompositionContext` として差し込み、`BroadcastFrameClock` から手動でフレームを送る) の上で動く。つまり「プールへ入って window から外れた `ComposeView` の composition が、production の Choreographer 駆動でも実際に再 composition されて deactivate が走るか」「その `onDeactivate` が実機の高速フリックで view の取り合い・空行・例外を起こさないか」は、どのテストもカバーしていない。`reset()` による購読停止・状態破棄が production でいつ成立するかはこの経路に依存するため、実機確認が機構の成立そのものを支える位置にある。

**推奨修正**: tasks 3.1 を実施し、手順と結果 (スクリーンショット等の証跡込み) を `verification-device.md` に残す (cross/conventions/runtime-behavior-verification.md の規律に準じ、可能なら旧実装ビルドとの A/B まで)。実施できない事情がある場合は、実装を直す話ではないためオーナー判断を仰ぎ deviation として記録する。

### [🟡 Minor] tasks 2.7 がチェック済みだが「結果を記録する」の記録が存在しない

**該当箇所**: `kasane/changes/perf-android-customcell-composition-reuse/tasks.md:21`

**問題点**: タスク本文が「結果を記録する」と要求しているが、change 配下にも deviation.md にも記録がない。チェック済み = 証跡ありという前提が成り立たない状態で、後からアーカイブしたときに何を確認したのかを追えない。

**推奨修正**: ミューテーション (a)(b)(c) それぞれで落ちたテスト名を change 配下に記録する。本レビューで独立に再実測した結果 (上表) をそのまま流用してよい。

### [🟡 Minor] `ComposeFrameDriver.kt` が 2 モジュールへ完全重複している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ComposeFrameDriver.kt` と `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/ComposeFrameDriver.kt`

**問題点**: 2 ファイルの差分は `package` 行 1 行のみ (diff で確認)。Robolectric と Compose の噛み合わせという壊れやすい前提を握った 100 行の駆動器が二重化しており、片方だけ直したときに気づけない。Compose 更新時の再検証点も 2 箇所になる。

**推奨修正**: `testFixtures` か共有 test-utils モジュール、あるいは `sourceSets` へのディレクトリ追加で単一の実体を共有する。共有手段が現行のビルド構成で取れないなら、両ファイルの KDoc に「もう一方と同一実体であり、片方だけ変更しない」旨を明記する。

### [🟡 Minor] `bind()` / `reset()` の KDoc・コメントが実際のタイミングより強く読める

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:103-104`, `:146-150`

**問題点**: 2 点ある。

1. `:103-104` の「同一性キーを先に入れてから活性化する。非活性のまま溜めた更新も 1 度の再 composition でまとめて反映される」は、代入順に機能的な意味があるかのように読める。実際は同一スナップショット内の書き込みであり、次の再 composition で全 state がまとめて読まれるだけなので、`isContentActive` を先頭に置いても結果は変わらない。「順序に依存しない」ことを書くのが正確。
2. `:146-150` の「content を非活性化して `remember` / `DisposableEffect` を破棄し（購読も止まる）」は、reset の呼び出し時点で破棄が完了するように読める。実際に破棄が走るのは非活性化が再 composition に観測された時点であり、`RecyclerView` が同一レイアウトパス内で recycle → bind を続けて行う経路では `isContentActive` の `false` は一度も観測されず、行間隔離は `contentKey` の変化だけが担う (このケースは `間に再 composition を挟まない再 bind でも remember が持ち越されない` が固定している)。

いずれも挙動は正しく、隔離はどちらの経路でも成立するため実害はないが、次にこのファイルを読む人が誤った不変条件を前提にしやすい。

**推奨修正**: 1 は順序非依存である旨に書き換える。2 は「非活性化が再 composition に反映された時点で破棄される。同一フレーム内で再 bind される経路では非活性化を経ず、隔離は同一性キーの変化が担う」といった形へ精度を上げる。

### [🔵 Suggestion] 二重否定のアサーション

**該当箇所**: `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCustomCellDeactivateTest.kt:161`

**問題点**: `assertFalse("表示中の行の埋め込みが window から外れている", !kept.isAttachedToWindow)` は否定が二重で、メッセージと式の対応を読み取るのに手間がかかる。

**推奨修正**: `assertTrue("表示中の行の埋め込みが window から外れている", kept.isAttachedToWindow)` にする。

### [🔵 Suggestion] `Int` 状態は `mutableIntStateOf` を使う

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:70`

**問題点**: `mutableStateOf(0)` は `Int` の autoboxing を伴い、Compose の Lint ルール `AutoboxingStateCreation` の対象になる (本プロジェクトは lint タスクを回していないため警告としては現れない)。

**推奨修正**: `heightDpState` を `mutableIntStateOf(0)` にし、読み出しを `intValue` にする。Boolean / 参照型の state は現状のままでよい。

### [🔵 Suggestion] `ComposeFrameDriver.frame()` が打ち切りを黙って握り潰す

**該当箇所**: `.../ui/ComposeFrameDriver.kt:62-70` (bridge 側も同一)

**問題点**: `MAX_FRAMES` 回で収束しなかった場合、保留中の再 composition を残したまま静かに返る。テストは「反映されていない」状態で assert に進むため、失敗の原因が「実装の問題」なのか「フレーム不足」なのか切り分けにくい。

**推奨修正**: 打ち切りに到達した場合は `error(...)` で明示的に失敗させる (収束しないこと自体がテスト環境の異常であり、正常系では到達しないはず)。

### [🔵 Suggestion] 余分な空行

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellRecycleTest.kt:527`

**問題点**: `companion object` の末尾に空行が 1 行残っている。

**推奨修正**: 削除する。

## アクションプラン

1. **[Major] tasks 3.1 の実機高速フリック検証を実施し、`verification-device.md` に証跡を残す。** 実施できない場合はオーナー判断を仰ぐ (deviation 化の可否)
2. **[Minor] tasks 2.7 の検出力確認結果を change 配下に記録する** (本レビューの実測表を流用可)
3. **[Minor] `ComposeFrameDriver` の重複を解消するか、重複である旨を両ファイルに明記する**
4. **[Minor] `CustomCellViewHolder` の bind / reset のコメントを実タイミングに合わせて精度を上げる**
5. [Suggestion] 二重否定アサーション / `mutableIntStateOf` / `frame()` の打ち切り明示化 / 余分な空行 — まとめて対応可

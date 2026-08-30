# 検証結果: perf-android-customcell-composition-reuse (002 回目)

**日付**: 2026-08-16
**判定**: INVALID

デルタスペックの全 Requirement / Scenario は実装・テストの対応が揃っており、対応表に ❌ はない (verify-001 と同じく 9 Scenario すべて ✅)。クラッシュ修正で追加された measure policy も、どの Scenario の充足も壊していないことをテスト実行で確認した。

verify-001 の INVALID 根拠 2 件のうち **❌1 (実機検証 tasks 3.1 未実施) は完全に解消**、**❌2 (tasks 2.7 の結果記録なし) は記録が新設されたものの、記録された実装ソースの SHA が提出コードと一致せず、tasks 2.7 の要求「結果を記録する」が提出コードに対して満たされていない**。この 1 件を ❌ として残すため INVALID とする。実装の修正は不要で、記録の取り直しだけで解消する。

---

## 対応表: settings-view-android-ui

パス基準: 実装 = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/`、テスト = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/`

### ADDED: Requirement: CustomCell Composition のプール生存と破棄境界

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 行のスクロールアウトでは破棄されない | `ComposeCellViewHolder.kt:39-41` (`DisposeOnDetachedFromWindowOrReleasedFromPool` の明示指定) | `CustomCellRecycleTest.kt:94` `行がプールへ入っても Composition は生存しプール放逐で破棄される` の前半 (`setItemViewCacheSize(0)` + 刻みスクロール後に `hasComposition` が全 true) | ✅ 一致 |
| itemViewCache 経由の再表示で content が継続する | `CustomCellViewHolder.kt:228-235` (`reset()` は `onViewRecycled` 経路のみ。cache 滞在では非活性化しない。呼び出し元 `KsSettingsListAdapter.kt:218-226`) | `CustomCellRecycleTest.kt:147` `itemViewCache 経由の再表示では content の状態と購読が維持される` (既定 cache 設定・ViewHolder 同一性・`counter-1` 維持・`disposeCount == 0`) | ✅ 一致 |
| プールからの放逐で破棄される | `ComposeCellViewHolder.kt:39-41` | `CustomCellRecycleTest.kt:113-118` (`recycledViewPool.clear()` 後に `hasComposition` が全 false) | ✅ 一致 |
| ホストの解放で破棄される | `ComposeCellViewHolder.kt:39-41` | `CustomCellRecycleTest.kt:122` `ホストの解放で保持中の行の Composition が破棄される` (プール滞在分 + 表示中分の両方) | ✅ 一致 |

### ADDED: Requirement: content ノードツリーの再利用

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一ラップ関数 builder 間でノードが再利用される | `CustomCellViewHolder.kt:120-121` (`ReusableContentHost(active)` + `ReusableContent(key = contentKey.value)`) | `CustomCellRecycleTest.kt:182` `同一ラップ関数 builder 間で埋め込み View が再利用される` (`onReset` 付き `AndroidView` プローブ・`factoryCount == 1`・View インスタンス同一・`resetCount >= 1`・新値 "b" 反映) | ✅ 一致 |
| 構造が異なる builder でも表示が壊れない | 同上 | `CustomCellRecycleTest.kt:218` `構造が異なる builder 間の再 bind でも新しい出力だけが現れる` (`beta-b` あり・`alpha*` なし) | ✅ 一致 |

補足: 「再利用が成立しない再 bind でも、新しい builder の出力が正しく表示される」は `CustomCellRecycleTest.kt:218` に加え、クラッシュ修正で追加された非活性経路について `CustomCellPooledRebindMeasureTest.kt:84` が `probe-b` の表示と `probe-a` の不在で押さえている。

### ADDED: Requirement: content 状態の行間隔離

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一フレーム内の再 bind でも remember 状態が持ち越されない | `CustomCellViewHolder.kt:121` + `:169` (`contentKey.value = cell.id`) | `CustomCellRecycleTest.kt:245` `間に再 composition を挟まない再 bind でも remember が持ち越されない` (世代番号 `b-2` あり・`a-*` なし) | ✅ 一致 |
| DisposableEffect の後始末が実行される | `CustomCellViewHolder.kt:121`, `:233` | `CustomCellRecycleTest.kt:245` (`disposeCount >= 1`) および `:273` `別 Cell への再 bind では remember が持ち越されず DisposableEffect が dispose される` (プール経路・`b-0` / `a-*` 不在 / `disposeCount >= 1`) | ✅ 一致 |

### ADDED: Requirement: reset による状態破棄と参照切断

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| reset 後に前の content と listener が残らない | `CustomCellViewHolder.kt:228-235` (`setOnClickListener(null)` / `isClickable=false` / `isContentActive=false` / `contentState=EMPTY_CELL_CONTENT`) | `CustomCellRecycleTest.kt:312` `リサイクルされた行は前の content と listener を保持しない` (実 RecyclerView 経路)、`CustomCellRenderingTest.kt:188` `reset で前の content とタップ listener が残らない` (直接 `reset()` 経路) | ✅ 一致 |
| Composition 破棄後に builder が解放可能になる | `CustomCellViewHolder.kt:234` (`contentState` の切り離し) + `ComposeCellViewHolder.kt:39-41` | `CustomCellBuilderReleaseTest.kt:67` `Composition 破棄後は builder が参照するものが解放可能になる` (`WeakReference` + pool clear + root 差し替え + `reachabilityFence(holder)` で ViewHolder 生存を保証) | ✅ 一致 |

## 対応表: maui-bridge

パス基準: テスト = `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/`

### ADDED: Requirement: deactivate+reuse 下での Bridge 埋め込み platform view の保全

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| リサイクルを挟んだ再表示で同一 platform view が再親付けされる | `CustomCellViewHolder.kt:120-121`, `:228-235` (Bridge 側にコード変更なし。ホスト側 deactivate 経路の保全契約) | `KsBridgeCustomCellDeactivateTest.kt:95` (`hasComposition == true`・`detachCount >= 1`・`marker` 保持・ViewHolder / ComposeView / probe View の同一性・`attachCount` 増加) | ✅ 一致 |
| deactivate が他の行の埋め込みを奪わない | 同上 | `KsBridgeCustomCellDeactivateTest.kt:129` `非活性化は表示中の他の行の埋め込みを取り外さない` (`kept.detachCount` 不変・同一インスタンス保持・`isAttachedToWindow`) | ✅ 一致 |

補足 (経路の実在確認): 本検証で bridge モジュールに一時的な診断テストを置き、`KsBridgeCustomCellDeactivateTest` と同条件でスクロールした直後の状態を実測した (実測後に削除済み)。`holderAt(1) == null` / `ComposeView.isAttachedToWindow == false` / `parent == null` / `RecycledViewPool` の当該 viewType が 1 件 / `hasComposition == true`。テストが assert している `hasComposition == true` は、確かに「プール滞在中の Composition 生存」を観測している。

## 追加検査

### tasks.md の完了状況

| タスク | 状態 |
|---|---|
| 1.1 破棄戦略の pool-aware 化 + KDoc 改訂 | ✅ `ComposeCellViewHolder.kt:8-19` / `:36-42`。`android/ADR-0015` 参照あり |
| 1.2 `ReusableContentHost` / `ReusableContent` / state 化 | ✅ `CustomCellViewHolder.kt:81-158`。`heightDp` は `mutableIntStateOf` |
| 1.3 View 側適用は composition 外 | ✅ `CustomCellViewHolder.kt:161-206` |
| 1.4 `reset()` の deactivate 化 + `setContent {}` 廃止 | ✅ `CustomCellViewHolder.kt:228-235`。`setContent {}` は消えている |
| 1.5 KDoc・コメントの旧前提の書き換え | ✅ 両ファイルとも新前提へ改訂済み |
| 2.1〜2.6 テスト新設 | ✅ 対応表どおり存在し全件成功。チェックと実体が一致 |
| **2.7 検出力確認 (ミューテーション)** | **❌ 記録 (`verification-mutation.md`) が提出コードと別バージョンに対するもの** (下記 ❌1) |
| 2.8 既存 `CustomCellRenderingTest` の追随 | ✅ `ComposeFrameDriver` 導入と `frame()` 呼び出しで追随。25 件成功 |
| 2.9 既存 `KsBridgeCustomCellTest` の回帰ゲート実行 | ✅ 20 件成功 |
| 2.10 完了ゲート (全モジュール全件) | ✅ 実行して確認 (下記) |
| **3.1 実機検証** | ✅ **解消**。`verification-device.md` が新設され、初回 (NG) / 再検証 (問題なし) の 2 回分と証跡が揃っている |

未実装なのにチェック済み、という虚偽は検出されなかった。

なお、クラッシュ修正 (`Layout` による measure policy) に対応するタスクは tasks.md に存在しない。tasks 1.2 の「`CustomCellViewHolder` を再構成する」に含めて実施されたものと読める。足場は凍結されているため tasks.md の追記は求めないが、**実装が tasks の記述範囲を超えた設計判断を含む**点は review-002.md の Major-2 として扱う。

### 逆流検査 (足場アーティファクトの書き換え)

`git log --oneline -- kasane/changes/perf-android-customcell-composition-reuse/` は起案コミット `9804cbc` の 1 件のみ。未コミット差分も `git diff --stat` 上 `tasks.md` のチェック更新 (16 行) だけで、`proposal.md` / `specs/*/spec.md` / `exploration.md` / `second-opinion-spec-*.md` は無変更。**逆流なし ✅**

### 未記録乖離

`deviation.md` は存在しない。対応表の ❌ はゼロであり、デルタスペックからの乖離は検出されなかった。

ただし、デルタスペックの Requirement に反しないものの**アーティファクトのどこにも記録がない挙動の追加**が 1 件ある: プールで非活性化された行を再 bind したとき、content が表示されるのは再活性化が composition へ反映された後 (通常は次のフレーム) になり、その間は行高さだけを確保した空の行が出得る (`CustomCellViewHolder.kt:69-72` / `:139-156`)。proposal の Impact は挙動変化を (1)〜(5) と列挙しているがこれを含まず、android/ADR-0015 の Consequences にもない。`verification-device.md` の再検証では実機で 1 件 (1 フレーム) 観測され、判定ドライバ側にこの性質を前提とした判定基準 (「持続しなければ NG としない」) が追加されている。デルタスペックとの一致検証としては ❌ にしないが、蒸留前に記録先を決めるべき事項として挙げる (review-002.md Major-2)。

### テスト実行 (test-execution 規約)

```
cd android && ./gradlew test --rerun-tasks   → BUILD SUCCESSFUL in 3m 55s
```

`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の集計:

| variant | tests | failures | errors | skipped |
|---|---:|---:|---:|---:|
| testDebugUnitTest | 1172 | 0 | 0 | 0 |
| testReleaseUnitTest | 1172 | 0 | 0 | 0 |
| **合計** | **2344** | **0** | **0** | **0** |

verify-001 時点の 1171 × 2 からの +1 × 2 は、新設 `CustomCellPooledRebindMeasureTest` (1 件) で説明がつく。

新設・改修クラスの個別確認: `CustomCellRecycleTest` 8 / `CustomCellPooledRebindMeasureTest` 1 / `CustomCellBuilderReleaseTest` 1 / `KsBridgeCustomCellDeactivateTest` 2 / `CustomCellRenderingTest` 25 / `KsBridgeCustomCellTest` 20、いずれも failures 0・skipped 0。**テスト全件成功 ✅**

### UI 変更

本変更に `ui/` アーティファクトはなく、モック承認ゲートの対象外 (視覚仕様の変更を含まないため妥当)。

## verify-001 の ❌ の解消確認

| verify-001 の ❌ | 現状 | 根拠 |
|---|---|---|
| ❌1 実機高速フリック検証 (tasks 3.1) が未実施 | **解消 ✅** | `verification-device.md` が新設され、初回検証 (修正前ビルドで FATAL を 21/23 セッションで再現) と再検証 (修正後ビルドで両端末計 850 ジェスチャ / 190 検査点を完走、空行 (持続) 0 / 混線 0 / FATAL 0 / ANR 0) の 2 回分が記録されている。参照される証跡 (`logs/crash-traces.txt`・`logs/session-*.log`・`logs/anr-during-verification.txt`・スクリーンショット 5 点・録画 1 点) はすべて実在し、セッションログ末尾の集計は本文の表と一致する。**再検証ビルドとして記録された `CustomCellViewHolder.kt` の SHA-1 `4ed1767afd785feb3203c315e28f6a58c12650a7` は、現ツリーの同ファイルと一致する** (`shasum` で確認) — 検証が提出コードそのものに対して行われたことの裏付けになる |
| ❌2 tasks 2.7 の結果記録が存在しない | **未解消 (形を変えて残存)** | `verification-mutation.md` は新設されたが、記録された実装ソース SHA が提出コードと一致しない (下記 ❌1) |

## ❌ の一覧と見立て

### ❌1: tasks 2.7 の記録が提出コードに対する証跡になっていない

- **内容**: `verification-mutation.md:88-95` は「変異前 / 復帰後の SHA-1」として `CustomCellViewHolder.kt` = `0946afdf18a04411d6df804c93d629f0026f7b08` を記録しているが、提出コードの同ファイルは `4ed1767afd785feb3203c315e28f6a58c12650a7` である。`0946afdf…` は `verification-device.md:43` が**初回検証 (FATAL が出た修正前ビルド)** の SHA として記録している値と同一であり、tasks 2.7 の測定はクラッシュ修正の**前**に行われたことになる。結果として、(a)〜(e) の再現性が修正後の構造でも成立するかが記録されておらず、修正の中核である measure guard (`CustomCellViewHolder.kt:140`) の検出力に至っては 1 件も記録がない。tasks 2.7 の「結果を記録する」は、提出コードに対しては満たされていない。
- **見立て**: **記録を取り直して解消すべき (deviation として合意する話ではない)**。実装の修正は不要である — 本検証と並行して行ったレビュー (review-002.md) が提出コードに対して独立にミューテーションを当て直し、(a) 5 件 / (c) 1 件 / (d) 1 件 / (e) 1 件が引き続き落ちること、および新規の (g) 「measure guard を無効化する」で `CustomCellPooledRebindMeasureTest` が実機と同一の `IllegalArgumentException: measure is called on a deactivated node` で落ちることを実測済み。いずれも前提アサーションは通過し争点のアサーションだけが落ちた。したがって `verification-mutation.md` を修正後 SHA で更新し (g) を追記すれば、チェックと実体が一致する。

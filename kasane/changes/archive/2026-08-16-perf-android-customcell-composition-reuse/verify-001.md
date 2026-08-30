# 検証結果: perf-android-customcell-composition-reuse (001 回目)

**日付**: 2026-08-16
**判定**: INVALID

デルタスペックの全 Requirement / Scenario は実装・テストの対応が揃っている (❌ なし)。一方、追加検査で tasks.md 側に 2 件の ❌ を検出したため INVALID とする。デルタスペックそのものの充足は問題ないため、指摘は 2 件の完了処理に閉じる。

---

## 対応表: settings-view-android-ui

パス基準: 実装 = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/`、テスト = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/`

### ADDED: Requirement: CustomCell Composition のプール生存と破棄境界

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 行のスクロールアウトでは破棄されない | `ComposeCellViewHolder.kt:39-41` (`DisposeOnDetachedFromWindowOrReleasedFromPool` 明示指定) | `CustomCellRecycleTest.kt:94` `行がプールへ入っても Composition は生存しプール放逐で破棄される` 前半 | ✅ 一致 |
| itemViewCache 経由の再表示で content が継続する | `CustomCellViewHolder.kt:159-166` (`reset()` は `onViewRecycled` 経路のみ。cache 滞在では非活性化しない) | `CustomCellRecycleTest.kt:147` `itemViewCache 経由の再表示では content の状態と購読が維持される` (既定 cache 設定・`disposeCount == 0`・`counter-1` 維持・ViewHolder 同一性) | ✅ 一致 |
| プールからの放逐で破棄される | `ComposeCellViewHolder.kt:39-41` | `CustomCellRecycleTest.kt:113-118` (`recycledViewPool.clear()` 後に `hasComposition` が全 false) | ✅ 一致 |
| ホストの解放で破棄される | `ComposeCellViewHolder.kt:39-41` | `CustomCellRecycleTest.kt:122` `ホストの解放で保持中の行の Composition が破棄される` (プール滞在分 + 表示中分の両方を対象) | ✅ 一致 |

### ADDED: Requirement: content ノードツリーの再利用

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一ラップ関数 builder 間でノードが再利用される | `CustomCellViewHolder.kt:81-93` (`ReusableContentHost` + `ReusableContent(key = contentKey)`) | `CustomCellRecycleTest.kt:182` `同一ラップ関数 builder 間で埋め込み View が再利用される` (`onReset` 付き `AndroidView` プローブ・`factoryCount == 1`・View インスタンス同一・`resetCount >= 1`・新値 "b" 反映) | ✅ 一致 |
| 構造が異なる builder でも表示が壊れない | 同上 | `CustomCellRecycleTest.kt:218` `構造が異なる builder 間の再 bind でも新しい出力だけが現れる` (`beta-b` あり・`alpha*` なし) | ✅ 一致 |

### ADDED: Requirement: content 状態の行間隔離

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一フレーム内の再 bind でも remember 状態が持ち越されない | `CustomCellViewHolder.kt:83` + `:105` (`contentKey.value = cell.id`) | `CustomCellRecycleTest.kt:245` `間に再 composition を挟まない再 bind でも remember が持ち越されない` (世代番号 `b-2` / `a-*` 不在) | ✅ 一致 |
| DisposableEffect の後始末が実行される | `CustomCellViewHolder.kt:83`, `:164` | `CustomCellRecycleTest.kt:245` (`disposeCount >= 1`) および `:273` `別 Cell への再 bind では remember が持ち越されず DisposableEffect が dispose される` (プール経路) | ✅ 一致 |

### ADDED: Requirement: reset による状態破棄と参照切断

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| reset 後に前の content と listener が残らない | `CustomCellViewHolder.kt:159-166` (`setOnClickListener(null)` / `isClickable=false` / `isContentActive=false` / `contentState=EMPTY_CELL_CONTENT`) | `CustomCellRecycleTest.kt:312` `リサイクルされた行は前の content と listener を保持しない` (実 RecyclerView 経路)、既存 `CustomCellRenderingTest.kt:195` 付近 (直接 `reset()` 呼び出し経路) | ✅ 一致 |
| Composition 破棄後に builder が解放可能になる | `CustomCellViewHolder.kt:165` (`contentState` の切り離し) + `ComposeCellViewHolder.kt:39-41` | `CustomCellBuilderReleaseTest.kt:64` `Composition 破棄後は builder が参照するものが解放可能になる` (`WeakReference` + pool clear + root 差し替え) | ✅ 一致 |

## 対応表: maui-bridge

パス基準: テスト = `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/`

### ADDED: Requirement: deactivate+reuse 下での Bridge 埋め込み platform view の保全

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| リサイクルを挟んだ再表示で同一 platform view が再親付けされる | `CustomCellViewHolder.kt:81-93`, `:159-166` (Bridge 側にコード変更なし。ホスト側 deactivate 経路の保全契約) | `KsBridgeCustomCellDeactivateTest.kt:96` (`hasComposition == true`・`detachCount >= 1`・`marker` 保持・ViewHolder / ComposeView / probe View の同一性・`attachCount` 増加) | ✅ 一致 |
| deactivate が他の行の埋め込みを奪わない | 同上 | `KsBridgeCustomCellDeactivateTest.kt:130` `非活性化は表示中の他の行の埋め込みを取り外さない` (`kept.detachCount` 不変・同一インスタンス保持) | ✅ 一致 |

## 追加検査

### tasks.md の完了状況

| タスク | 状態 |
|---|---|
| 1.1〜1.5 (実装) | ✅ 対応表どおり実装済み。チェックと実体が一致 |
| 2.1〜2.6 (テスト新設) | ✅ 対応表どおり存在し、全件成功。チェックと実体が一致 |
| 2.7 (検出力確認: ミューテーション) | ❌ チェック済みだが「結果を記録する」の記録が repo 内に存在しない |
| 2.8 (既存 `CustomCellRenderingTest` の追随) | ✅ `ComposeFrameDriver` 導入と `frame()` 呼び出しの追加で追随済み。25 件成功 |
| 2.9 (既存 `KsBridgeCustomCellTest` を回帰ゲート実行) | ✅ 20 件成功 |
| 2.10 (完了ゲート: 全モジュール全件) | ✅ 実行して確認 (下記) |
| 3.1 (実機検証) | ❌ 未チェック・未実施。`verification-device.md` 不在 |

### 逆流検査 (足場アーティファクトの書き換え)

`git log --oneline -- kasane/changes/perf-android-customcell-composition-reuse/` は起案コミット `9804cbc` の 1 件のみ。未コミット差分も change 配下は `tasks.md` のチェック更新だけで、`proposal.md` / `specs/*/spec.md` / `exploration.md` / `second-opinion-spec-*.md` は無変更。**逆流なし ✅**

### 未記録乖離

`deviation.md` は存在しない。対応表の ❌ はゼロであり、デルタスペックからの乖離は検出されなかった。

### テスト実行 (test-execution 規約)

```
cd android && ./gradlew test --rerun-tasks   → BUILD SUCCESSFUL in 4m 1s
```

`build/test-results/testDebugUnitTest|testReleaseUnitTest/TEST-*.xml` の集計:

| variant | tests | failures | errors | skipped |
|---|---:|---:|---:|---:|
| testDebugUnitTest | 1171 | 0 | 0 | 0 |
| testReleaseUnitTest | 1171 | 0 | 0 | 0 |
| **合計** | **2342** | **0** | **0** | **0** |

新設・改修クラスの実行も個別に確認: `CustomCellRecycleTest` 8 件 / `CustomCellBuilderReleaseTest` 1 件 / `KsBridgeCustomCellDeactivateTest` 2 件 / `CustomCellRenderingTest` 25 件 / `KsBridgeCustomCellTest` 20 件、いずれも failures 0・skipped 0。**テスト全件成功 ✅**

### UI 変更

本変更に `ui/` アーティファクトはなく、モック承認ゲートの対象外 (視覚仕様の変更を含まないため妥当)。

## ❌ の一覧と見立て

### ❌1: tasks 3.1 (実機高速フリック検証) が未実施

- **内容**: tasks.md:28 が未チェックで、要求されている `verification-device.md` が存在しない。
- **見立て**: **実施すべき (deviation で合意すべきではない)**。android/ADR-0015 の Consequences が「実機での高速フリック再検証を要する」と明示しており、proposal の Impact も証跡を残すと宣言している。さらに、新設テストはすべてテスト専用 Recomposer (`ComposeFrameDriver`) で手動駆動されているため、production の Choreographer 駆動における deactivate 経路は現状どのテストでもカバーされていない。省略すると ADR が明示した唯一の裏取り手段が失われる。実施できない事情があるならオーナー判断で deviation 化する。

### ❌2: tasks 2.7 の結果記録が存在しない

- **内容**: tasks.md:21 はチェック済みだが、「結果を記録する」に対応する記録が change 配下にも deviation.md にも存在しない。
- **見立て**: **記録を残して解消すべき**。検出力そのものは本検証と並行して行ったレビュー (review-001.md) の独立ミューテーションで再現済み — (a) 破棄戦略の巻き戻しで 4 件、(b) `ReusableContent` → `key` 置換で 1 件、(c) `contentKey` 固定で 1 件が、いずれも前提アサーションを通過したうえで争点のアサーションだけ落ちることを実測した。したがって実装の修正は不要で、結果を change 配下へ記載すればチェックと実体が一致する。

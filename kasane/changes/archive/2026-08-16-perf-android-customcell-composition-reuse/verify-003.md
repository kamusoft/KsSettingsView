# 検証結果: perf-android-customcell-composition-reuse (003 回目)

**日付**: 2026-08-16
**判定**: VALID

デルタスペックの全 Requirement / Scenario (9 件) が「✅ 一致」または「⚠️ deviation 記録済み」で、❌ はない。verify-002 の唯一の ❌ (tasks 2.7 の記録が提出コードに対する証跡になっていない) は、`verification-mutation.md` が提出コードの SHA で取り直されたことで解消した。虚偽チェックなし、逆流なし、テスト全件成功。

---

## 対応表: settings-view-android-ui

パス基準: 実装 = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/`、テスト = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/`

### ADDED: Requirement: CustomCell Composition のプール生存と破棄境界

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 行のスクロールアウトでは破棄されない | `ComposeCellViewHolder.kt:39-41` (`DisposeOnDetachedFromWindowOrReleasedFromPool` の明示指定) | `CustomCellRecycleTest.kt:94` `行がプールへ入っても Composition は生存しプール放逐で破棄される` の前半 (`setItemViewCacheSize(0)` + 刻みスクロール後に `hasComposition` が全 true) | ⚠️ deviation 記録済み (`deviation.md:3`: `scrollToPosition` 等の位置指定ジャンプでは RecyclerView の一時 detach 経路で破棄され旧挙動へ縮退する。2026-08-16 オーナー合意。刻みスクロール/フリック経路では spec どおり成立し、テストもその経路を通す。`CustomCellRecycleTest.kt:411` / `KsBridgeCustomCellDeactivateTest.kt:28` の KDoc にも経路差が明記されている) |
| itemViewCache 経由の再表示で content が継続する | `CustomCellViewHolder.kt:260-267` (`reset()` は `onViewRecycled` 経路のみ。呼び出し元 `KsSettingsListAdapter.kt:218-226`) | `CustomCellRecycleTest.kt:147` `itemViewCache 経由の再表示では content の状態と購読が維持される` (既定 cache 設定・ViewHolder 同一性・`counter-1` 維持・`disposeCount == 0`) | ✅ 一致 |
| プールからの放逐で破棄される | `ComposeCellViewHolder.kt:39-41` | `CustomCellRecycleTest.kt:94` の後半 (`recycledViewPool.clear()` 後に `hasComposition` が全 false) | ✅ 一致 |
| ホストの解放で破棄される | `ComposeCellViewHolder.kt:39-41` | `CustomCellRecycleTest.kt:122` `ホストの解放で保持中の行の Composition が破棄される` (プール滞在分 + 表示中分の両方) | ✅ 一致 |

### ADDED: Requirement: content ノードツリーの再利用

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一ラップ関数 builder 間でノードが再利用される | `CustomCellViewHolder.kt:141-142` (`ReusableContentHost(active)` + `ReusableContent(key = contentKey.value)`) | `CustomCellRecycleTest.kt:182` `同一ラップ関数 builder 間で埋め込み View が再利用される` (`onReset` 付き `AndroidView` プローブ・`factoryCount == 1`・View インスタンス同一・`resetCount >= 1`・新値 "b" 反映) | ✅ 一致 |
| 構造が異なる builder でも表示が壊れない | 同上 | `CustomCellRecycleTest.kt:218` `構造が異なる builder 間の再 bind でも新しい出力だけが現れる` (`beta-b` あり・`alpha*` なし)。非活性経路については `CustomCellPooledRebindMeasureTest.kt:100` が `probe-b` の表示と `probe-a` の不在で押さえている | ✅ 一致 |

### ADDED: Requirement: content 状態の行間隔離

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一フレーム内の再 bind でも remember 状態が持ち越されない | `CustomCellViewHolder.kt:142` + `:201` (`contentKey.value = cell.id`) | `CustomCellRecycleTest.kt:245` `間に再 composition を挟まない再 bind でも remember が持ち越されない` (世代番号 `b-2` あり・`a-*` なし) | ✅ 一致 |
| DisposableEffect の後始末が実行される | `CustomCellViewHolder.kt:142`, `:265` | `CustomCellRecycleTest.kt:245` (`disposeCount >= 1`) および `:273` `別 Cell への再 bind では remember が持ち越されず DisposableEffect が dispose される` (プール経路・`b-0` / `a-*` 不在 / `disposeCount >= 1`) | ✅ 一致 |

### ADDED: Requirement: reset による状態破棄と参照切断

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| reset 後に前の content と listener が残らない | `CustomCellViewHolder.kt:260-267` (`setOnClickListener(null)` / `isClickable=false` / `isContentActive=false` / `contentState=EMPTY_CELL_CONTENT`) | `CustomCellRecycleTest.kt:312` `リサイクルされた行は前の content と listener を保持しない` (実 RecyclerView 経路)、`CustomCellRenderingTest.kt:188` `reset で前の content とタップ listener が残らない` (直接 `reset()` 経路) | ✅ 一致 |
| Composition 破棄後に builder が解放可能になる | `CustomCellViewHolder.kt:266` (`contentState` の切り離し) + `ComposeCellViewHolder.kt:39-41` | `CustomCellBuilderReleaseTest.kt:67` `Composition 破棄後は builder が参照するものが解放可能になる` (`WeakReference` + pool clear + root 差し替え + `reachabilityFence(holder)`) | ✅ 一致 |

## 対応表: maui-bridge

パス基準: テスト = `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/`

### ADDED: Requirement: deactivate+reuse 下での Bridge 埋め込み platform view の保全

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| リサイクルを挟んだ再表示で同一 platform view が再親付けされる | `CustomCellViewHolder.kt:141-142`, `:260-267` (Bridge 側にコード変更なし。ホスト側 deactivate 経路の保全契約) | `KsBridgeCustomCellDeactivateTest.kt:95` (`hasComposition == true`・`detachCount >= 1`・`marker` 保持・ViewHolder / ComposeView / probe View の同一性・`attachCount` 増加) | ✅ 一致 |
| deactivate が他の行の埋め込みを奪わない | 同上 | `KsBridgeCustomCellDeactivateTest.kt:129` `非活性化は表示中の他の行の埋め込みを取り外さない` (`kept.detachCount` 不変・同一インスタンス保持・`isAttachedToWindow`) | ✅ 一致 |

## 追加検査

### tasks.md の完了状況

| タスク | 状態 |
|---|---|
| 1.1 破棄戦略の pool-aware 化 + KDoc 改訂 | ✅ `ComposeCellViewHolder.kt:12`, `:39-41`。`android/ADR-0015` 参照あり |
| 1.2 `ReusableContentHost` / `ReusableContent` / state 化 | ✅ `CustomCellViewHolder.kt:87-190`。`heightDp` は `mutableIntStateOf` |
| 1.3 View 側適用は composition 外 | ✅ `CustomCellViewHolder.kt:192-239` |
| 1.4 `reset()` の deactivate 化 + `setContent {}` 廃止 | ✅ `CustomCellViewHolder.kt:260-267`。`setContent {}` は消えている |
| 1.5 KDoc・コメントの旧前提の書き換え | ✅ 両ファイルとも新前提へ改訂済み |
| 2.1〜2.6 テスト新設 | ✅ 対応表どおり存在し全件成功。チェックと実体が一致 |
| **2.7 検出力確認 (ミューテーション)** | ✅ **解消**。下記「verify-002 の ❌ の解消確認」 |
| 2.8 既存 `CustomCellRenderingTest` の追随 | ✅ `ComposeFrameDriver` 導入と `frame()` 呼び出しで追随。25 件成功 |
| 2.9 既存 `KsBridgeCustomCellTest` の回帰ゲート実行 | ✅ 20 件成功 |
| 2.10 完了ゲート (全モジュール全件) | ✅ 実行して確認 (下記) |
| 3.1 実機検証 | ✅ `verification-device.md` に初回 (NG) / 再検証 (問題なし) の 2 回分と証跡。記録された修正後 SHA `4ed1767a…` は当時の提出コードのもので、その後 review-002 対応で高さ確保が変わり現ツリーは `404ca3bf…` になっている。差分は非活性中の確保高さ (measure が走らない側の分岐) に閉じており、実機検証で観測対象だった FATAL・空行・view 取り合いの経路 (measure guard 本体・破棄戦略・deactivate) は変わっていないため、再実施は求めない |

未実装なのにチェック済み、という虚偽は検出されなかった。tasks 2.7 は本文が (a)(b)(c) の 3 変異のみを求めているのに対し、記録は (a)〜(e) + (g)(h)(i)(j) の 9 変異を含む上位互換になっている。

なお measure policy と高さ確保に対応するタスクは tasks.md に存在しない (1.2 に含めて実施されたもの)。足場は凍結されているため追記は求めない。設計判断としての記録は android/ADR-0015 と `deviation.md` で担保されている (verify-002 で挙げた「記録先未定」は解消)。

### 逆流検査 (足場アーティファクトの書き換え)

`git log --oneline -- kasane/changes/perf-android-customcell-composition-reuse/` は起案コミット `9804cbc` の 1 件のみ。`kasane/` 配下の未コミット差分は `git diff --stat` 上、`tasks.md` (チェック更新 16 行) と `decisions/android/0015-…md` (追記 +3 / 出典行の書き換え -1) だけで、`proposal.md` / `specs/*/spec.md` / `exploration.md` / `second-opinion-spec-*.md` は無変更。ADR は長命層であり足場ではないため、逆流には当たらない。**逆流なし ✅**

### 未記録乖離

`deviation.md` に 2 件が記録されている (いずれも 2026-08-16 オーナー合意)。

1. 位置指定ジャンプ (`scrollToPosition` 等) では Composition が破棄され旧挙動へ縮退する — 対応表の「行のスクロールアウトでは破棄されない」に ⚠️ として反映
2. プール由来の再 bind で content の表示が最大 1 フレーム遅れる — spec が規定しない領域。設計判断と帰結は android/ADR-0015 の Decision (`:25`) / Consequences (`:43`) に記録済み

verify-002 が「記録先を決めるべき事項」として挙げた 2 の件は、ADR 追記 + deviation 記録で解消した。対応表の ❌ はゼロであり、**未記録の乖離は検出されなかった ✅**

### テスト実行 (test-execution 規約)

```
cd android && ./gradlew test --rerun-tasks   → BUILD SUCCESSFUL in 4m 12s
```

`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の集計:

| variant | tests | failures | errors | skipped |
|---|---:|---:|---:|---:|
| testDebugUnitTest | 1174 | 0 | 0 | 0 |
| testReleaseUnitTest | 1174 | 0 | 0 | 0 |
| **合計** | **2348** | **0** | **0** | **0** |

verify-002 時点の 1172 × 2 からの +2 × 2 は、`CustomCellPooledRebindMeasureTest` に追加された高さ確保テスト 2 件 (同クラス 1 → 3 件) で説明がつく。

新設・改修クラスの個別確認: `CustomCellPooledRebindMeasureTest` 3 / `CustomCellRecycleTest` 8 / `CustomCellBuilderReleaseTest` 1 / `KsBridgeCustomCellDeactivateTest` 2 / `CustomCellRenderingTest` 25 / `KsBridgeCustomCellTest` 20、いずれも failures 0・skipped 0。**テスト全件成功 ✅**

補足 (モジュール単独実行の安定性): 実装ワーカーの作業ログに `:ks-settingsview-ui:testDebugUnitTest --rerun-tasks` が `OutOfMemoryError` で 31 件失敗した記録が 3 回分残っていたため、提出ツリーで同じコマンドを 2 回連続で独立実行した。いずれも BUILD SUCCESSFUL・`OutOfMemoryError` 0 件で再現しなかった (詳細は review-003.md)。

### UI 変更

本変更に `ui/` アーティファクトはなく、モック承認ゲートの対象外 (視覚仕様の変更を含まないため妥当)。

## verify-002 の ❌ の解消確認

| verify-002 の ❌ | 現状 | 根拠 |
|---|---|---|
| ❌1 tasks 2.7 の記録が提出コードに対する証跡になっていない | **解消 ✅** | `verification-mutation.md:12-16` の「測定対象」および `:110-113` の「変異前 / 復帰後」の SHA-1 が、現ツリーの `shasum` と 3 ファイルとも一致する (`CustomCellViewHolder.kt` = `404ca3bfa0ea6e02502cc3e7f551475e9d0a9608` / `ComposeCellViewHolder.kt` = `937f71d76f5bae3301b1b485d90be6b5089e9c7a` / `CustomCellPooledRebindMeasureTest.kt` = `b634f1e07e17f7c6ea60dbcb4c4287f33d0bf53a`)。verify-002 が「1 件も記録がない」と指摘した measure guard の検出力は (g) として追加され、`CustomCellPooledRebindMeasureTest` の 3 件すべてが実機と同一の `IllegalArgumentException: measure is called on a deactivated node` で落ちることが記録されている。あわせて高さ確保の (h) (i)、および**検出できなかった (j)** まで、落ちなかった変異を隠さずに記録している。全件テストの実行件数 (debug 1174 / release 1174) も本検証の実測と一致する |

**❌ 件数: 0**

---

## 追記: 本検証後のコメント修正 (同日)

本検証の完了後、review-003 の Minor に対してオーケストレーターが 2 ファイルの**コメントのみ**を修正した (`CustomCellViewHolder.kt` の `lastContentHeightPx` KDoc / `CustomCellPooledRebindMeasureTest.kt:180-182`)。コード・アサーションは無変更であることを `diff` と該当箇所の読み直しで確認し、`CustomCellPooledRebindMeasureTest` が 3 件成功することも再実行して確認した。

これに伴い、上表で ❌1 の解消根拠にした SHA 一致が成立しなくなっている。

| ファイル | `verification-mutation.md` の記録 | 修正後の現ツリー |
|---|---|---|
| `CustomCellViewHolder.kt` | `404ca3bf…` | `2b96cee1928dd59b4198ba2d5a80057c93f1ad03` |
| `CustomCellPooledRebindMeasureTest.kt` | `b634f1e0…` | `6631f21f1cf771f651cdaf4b5e601b45f12be0a1` |
| `ComposeCellViewHolder.kt` | `937f71d7…` | `937f71d7…` (一致) |

**判定は VALID のまま**とする。差分がコメントに閉じることを確認済みであり、コメントは (a)〜(j) いずれの変異の成否にも影響しないため、tasks 2.7 の「結果を記録する」は提出コードに対して依然として満たされている。

ただし記録と提出物の SHA が合わない状態は、verify-002 の ❌1 とまったく同じ形の齟齬である。**アーカイブ前に `verification-mutation.md` へ 1 行足すこと** — 「その後、review-003 Minor 対応でコメントのみ修正し SHA は `2b96cee1…` / `6631f21f…` へ変わった (変異結果に影響なし)」。これを怠ると、蒸留時やアーカイブ後に証跡を辿った人が同じ齟齬に突き当たる。

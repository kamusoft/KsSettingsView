# レビュー結果: fix-adapter-not-restored-on-reattach (001 回目)

**日付**: 2026-08-03
**判定**: CHANGES_REQUESTED

## サマリー

adapter 復元そのものの実装・テストは質が高い。参照グラフを追った限りリーク対策は壊れておらず (detach 時の `adapter = null` は据え置きで、復元して増える辺は `KsSettingsView` 内部で閉じたサイクルのみ)、既存 `MemoryLeakTest` 2 件とも両立し、追加テストは症状そのもの (行が消える) を実経路で踏んでいることをミューテーション実測で確認した。公開 API の変更もない。

一方で、**Store 経路 (`bind(store)`) では detach 中に発行された Diff が失われ、再 attach 後に stale な内容で復帰する**ことを実測で確認した。これは proposal が挙げた到達シナリオ (ViewPager2 オフスクリーン / Compose `AndroidView`) そのものの上で起きる未対応経路であり、Non-Goals にも記載がない。本修正前は「空で復帰」という目に見える壊れ方だったものが、本修正後は「もっともらしいが古い内容で復帰」に変わるため、検知しにくい形の誤りとして残る。この 1 件のため CHANGES_REQUESTED とする。

## 検証したこと (証跡)

**ビルド / テスト実行**

- `./gradlew test --rerun-tasks` (`ANDROID_HOME` 指定、166 タスク実行) → BUILD SUCCESSFUL
- 実行件数 (debug / release とも同数):
  - `ks-settingsview-ui` 715 件 (failures 0 / errors 0)
  - `ks-settingsview-compose` 80 件 (failures 0 / errors 0)
  - `ks-settingsview-core` 74 件 (failures 0 / errors 0)
- ktlint / detekt は本プロジェクトの Gradle に設定されていない (grep で該当なし) ため対象外

**ミューテーション実測 (tasks 4.3 の追試)**

`KsSettingsView.onAttachedToWindow()` の復元ブロックをコメントアウトして `AdapterReattachTest` / `MemoryLeakTest` を実行:

| テスト | 結果 | 失敗メッセージ |
|---|---|---|
| `detach 後に再 attach すると adapter が戻る` | FAILED | `再 attach 後は adapter が戻っている` |
| `detach 後に再 attach するとリストの並びと可視状態が保たれる` | FAILED | `expected:<[見出し, A, C]> but was:<[]>` |
| `MemoryLeakTest` 2 件 | PASSED | — |

2 件目が空リストで落ちることから、追加テストは **adapter 参照という代理値ではなく症状そのもの (行が描画されない)** を固定している。`MemoryLeakTest` が落ちないことから、両者が固定する契約は直交している。

**原状復帰**: ミューテーションに使った一時変更はバックアップから戻し、shasum 一致 (`83fb7ff7ebfc6e06cd7a22292f51be5faf32a09d`) を確認済み。レビュー用に一時作成した調査テスト (`ZzReviewProbeTest.kt`) も `trash` で削除済み。`git status` は実装者の 3 ファイル (KsSettingsView.kt / tasks.md / AdapterReattachTest.kt) のみを示す状態に戻っている。

**リーク対策との両立 (参照グラフの確認)**

復元によって増える辺は `recyclerView.mAdapter → concatAdapter` と `concatAdapter の observer → recyclerView` のみで、両端とも `KsSettingsView` の private フィールドが所有する。より長命なオブジェクト (Activity / Fragment / `SettingsRootStore` / static な `KsCellRegistry`・`PickerRestoreRegistry`) から adapter へ入る辺は増えない — つまり `KsSettingsView` サブツリー内で閉じたサイクルであり、GC 到達性は変わらない。detach 時の `adapter = null` は据え置きなので、detach 中の ViewHolder / itemView 解放と observer 解除という元の効果もそのまま残る。凍結資料の `openspec/specs/settings-view-android-host/spec.md` 「メモリリーク防止」Requirement も detach 時点の契約しか課しておらず、抵触しない。

**実環境検証の要否**

`concepts/cross/conventions/runtime-behavior-verification.md` の適用範囲は「ユニットテストで症状自体を再現できない不具合」。本件は上記ミューテーション実測のとおり Robolectric で症状 (行が空になる) をそのまま再現できるため**対象外**と判断した。実機証跡の欠如は指摘しない。

## 指摘事項

### [🟠 Major] Store 経路では detach 中の更新が失われ、再 attach 後に stale な内容で復帰する

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:246-254` (`onAttachedToWindow` の購読再確立ブロック)

**問題点**:

`onDetachedFromWindow` は `storeCollectJob` を cancel する。`SettingsRootStore.diffs` / `contentUpdateBatches` は `MutableSharedFlow(replay = 0, extraBufferCapacity = 64)` であり、**購読者がいない間に発行された Diff は誰にも届かないまま捨てられる**。再 attach 時の `attachStoreCollection(store)` は collector を張り直すだけで `store.state.value` を再適用しないため、detach 中に Store が変化していた場合、View の `internalRoot` は detach 時点のまま取り残される。

本修正前はこの経路も「再 attach で空リスト」に飲み込まれていたが、本修正後は **detach 時点の内容がそのまま出る = もっともらしいが古い内容で復帰する**という、利用者からは正常に見える壊れ方になる。到達シナリオは proposal が挙げたものと同一 (ViewPager2 のオフスクリーンページ、Compose `AndroidView` の付け外し)。Compose ラッパ `KsSettingsViewComposable.kt` の `bindAndroidView` も `update` で再 bind しないため、Compose 経由でも自動復旧しない。

**実測**: Robolectric で以下を確認した (レビュー用の使い捨てテスト。確認後に削除済み)。

1. `bind(store)` 後、attach 中の `store.insertCell(...)` は `internalRoot` に届く (購読が実在することの確認 — このアサーションは通過)
2. detach → `store.insertCell(...)` → 再 attach の順で操作すると、`store.state` は 3 セルなのに `view.internalRoot()` は 2 セルのまま
   → `AssertionError: 再 attach 後、detach 中の Store 更新が内部 root に反映されているか expected:<3> but was:<2>`

**推奨修正**:

`onAttachedToWindow` で購読を張り直す際に、Store の現在値を再適用してから collect を開始する。素直な形は `attachStoreCollection(store)` の直前 (または内部) で `setRootDirect(store.state.value, store.theme.value)` を呼ぶこと。ただし以下に注意が必要なので、単純な 1 行追加で済ませずに挙動を確認すること。

- `setRootDirect` は Adapter の `theme` を直接差し替えるだけで通知を出さない。detach 中に Theme だけが変わっていた場合、`submitList` の差分が空になり再 bind が走らず、Theme が見た目に反映されない可能性がある (この経路まで直すなら `theme` setter 経由か `applyThemeInternal` の併用を検討する)
- 初回 attach では `bind()` が既に `setRootDirect` 済みのため、同内容の `submitList` が 1 回余分に走る。実害はないはずだが、`AsyncListDiffer` の差分計算が空振りすることを確認しておく
- 退行テストは「detach 中の Store 更新が再 attach 後に反映される」を Diff 経路 (`internalRoot` / 表示行) で固定する。上記の実測テストがそのまま雛形になる

**スコープの扱いについて**: 本件は本変更が作り込んだ不具合ではなく、既存の Store 購読ライフサイクル側のギャップである。ただし (a) 修正箇所が本変更で触れた `onAttachedToWindow` の中にあり、(b) proposal / tasks のどこにもスコープ外と明記されていない、(c) 本修正によって「空で気づく」から「古い値で気づかない」へ悪化する、の 3 点から、本サイクル内での対応が妥当と判断した。別 change へ切り出す判断をオーナーが取る場合も、**既知の制約として change 配下に記録を残すこと** (無記録での持ち越しは不可)。

### [🔵 Suggestion] `if (recyclerView.adapter == null)` ガードの意図がコメントにもテストにも残っていない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:242`

**問題点**:

このガードを外しても `AdapterReattachTest` / `MemoryLeakTest` を含む全テストが green のままになる (ガードは初回 attach 時の再代入を避けるための最適化であり、正しさには効かない)。`RecyclerView.setAdapter` は同一インスタンスを渡しても `removeAndRecycleViews()` を伴う作り直しになるため、ガードには意味があるが、その意図が直上のコメントに書かれていないため、将来「常に代入すればよい」と単純化されて初回 attach ごとに無駄な全再生成が入る余地がある。

**推奨修正**: 直上のコメントに 1 文足す (例:「初回 attach では既に同一 adapter が入っているため、同一インスタンスの再代入による全 ViewHolder 再生成を避ける」)。コード変更は不要。

## 確認したが指摘しないこと

- **スクロール位置の保持**: proposal / tasks でスコープ外と明記済み。実装コメントにも明記されており整合している
- **公開 API の変更**: diff は `onAttachedToWindow` 内の追加とコメント修正のみ。シグネチャ・可視性の変更なし (Non-Goals 遵守)
- **tasks.md の 1.2 / 1.3 が未チェック**: いずれも「再現しなかった場合」「Robolectric で再現しない場合」の条件付きタスクであり、1.1 で再現が取れた本件では実施対象外。虚偽チェックではない
- **足場アーティファクトの書き換え**: tasks.md の差分はチェックボックスのみ。本文の改変なし
- **`MemoryLeakTest` の検証強度**: 同テストは `internalDetachForTest()` (未 attach の View に対する直接呼び出し) という代理経路だが、これは既存テストであり本変更の責任ではない。むしろ新規 `AdapterReattachTest` の 1 件目が実 detach 経路で `adapter == null` を確認しており、実経路のカバレッジは本変更で改善している

## アクションプラン

1. **[Major]** `onAttachedToWindow` の購読再確立時に Store の現在値を再適用する。Theme 単独変更時の反映も併せて確認し、「detach 中の Store 更新が再 attach 後に反映される」退行テストを追加する。別 change へ切り出す場合は既知の制約として記録を残す
2. **[Suggestion]** `if (recyclerView.adapter == null)` ガードの意図を直上コメントに 1 文追記する

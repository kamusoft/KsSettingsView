# レビュー結果: fix-adapter-not-restored-on-reattach (002 回目)

**日付**: 2026-08-03
**判定**: APPROVED

## サマリー

review-001 の Major (Store 経路の stale 復帰) は解消している。レビュアー独自の使い捨てテストで、修正前 (HEAD) では落ち修正後では通ることを確認した。

Theme の扱いは review-001 でこちらが推奨した形 (`setRootDirect(store.state.value, store.theme.value)`) ではなく、root だけを取り込み Theme は `StateFlow` の collect に任せる形が採られている。**この判断は正しく、こちらの推奨のほうが誤りだった**。推奨形をミューテーションとして実際に入れると `ItemDecoration` が古い Theme のまま残る退行が起き、追加テストがそれを検出する。1 フレーム古い配色で描かれる瞬間がないことも、メッセージを一切回さない時点での観測で確認した。

追加された 3 テストはいずれも検出力を実測で確認済み。否定形アサーション (「差分通知を出さない」) にはテスト内の対照実験が仕込まれており、加えてこちらの側でも退行注入で実際に落ちることを確認した。review-001 の Suggestion (ガードの意図) も対応済み。

Critical / Major は無い。指摘は本変更のスコープ外にある既存不具合 1 件 (Minor) と、テストの待ち方に関する Suggestion 1 件のみ。

## 検証したこと (証跡)

### ビルド / テスト実行

- `./gradlew test --rerun-tasks` (166 タスク実行) → BUILD SUCCESSFUL
- 実行件数 (debug / release とも同数):
  - `ks-settingsview-ui` **718 件** (review-001 時点 715 件 → 追加 3 件、failures 0 / errors 0)
  - `ks-settingsview-compose` 80 件、`ks-settingsview-core` 74 件 (いずれも failures 0 / errors 0)
- `AdapterReattachTest` は 5 件に増加 (前回 2 件 + 今回 3 件)

### 1. review-001 の Major (Store 経路の stale 復帰) の解消確認

実装者のテストとは独立に、レビュアー側で使い捨てテストを書いて確認した (確認後に削除)。

| プローブ | 現行実装 | HEAD (修正前) |
|---|---|---|
| probe1: bind → attach 中更新 → detach → detach 中更新 → 再 attach で Store 現在値が反映される | **PASS** | FAIL (`expected:<3> but was:<2>`) |
| probe4: detach 中に **root と Theme が両方**変わった場合に両方反映される | **PASS** | FAIL (`expected:<[見出し, B, C]> but was:<[]>`) |

probe1 は購読が実在することを先にアサートしたうえで detach 中更新の取り込みを見ており、probe4 は実装者テストがカバーしていない「root と Theme の同時変更」を突いている (root 適用 → Theme 適用の順で `notifyItemRangeChanged` が旧件数に対して発行されうる経路)。いずれも表示行・`internalTheme`・`ItemDecoration` の 3 点まで正しく揃うことを確認した。

### 2. Theme を渡さない設計の妥当性 (重点確認項目)

**結論: 現行方式が正しい。review-001 の推奨は誤りだった。**

`resyncFromStore` の KDoc が述べる論理 — 「Theme を `setRootDirect` の引数で先に入れると `themeBacking` だけが新しくなり、続く collect の同値スキップ (`if (themeBacking == value) return`) に阻まれて `applyThemeInternal` が走らず、`ItemDecoration` が古い Theme のまま残る」— を、推奨形をミューテーションとして注入して実測した。

**M2 (review-001 の推奨形を注入)**: `resyncFromStore` を `setRootDirect(store.state.value, store.theme.value)` に変更

| テスト | 結果 |
|---|---|
| `detach 中の Theme 変更が再 attach 後に反映される` | **FAILED** — `ItemDecoration も新 Theme で作り直される` |
| 他 4 件 | PASSED |

`internalTheme` と RecyclerView 背景は `setRootDirect` が直接更新するため通過し、**`ItemDecoration` のアサーションだけが落ちる**。この 3 点を分けてアサートしている設計が、この見つけにくい退行を捕まえている。

依存の壊れやすさについて:

- Theme の復帰は `attachStoreCollection` が張る `store.theme.collect` の初回配信に依存する。`lifecycleScope` は `Dispatchers.Main.immediate` を持ち、`onAttachedToWindow` は main スレッド上なので collect は同期的に始まり、`StateFlow` の現在値がその場で流れる
- **1 フレーム古い配色で描かれる瞬間がないこと**を、`addView` 直後にメッセージを 1 つも回さない時点での観測で確認した (レビュアー probe2)。`internalTheme` / `ItemDecoration` / RecyclerView 背景の 3 つとも attach トラバーサル内で新 Theme になっている。実装者テストも同じ観点を `assertEquals("attach 直後に Store の現在 Theme が反映される", ...)` として明示的に固定しており、この依存が将来崩れれば検出される
- なお probe2 は HEAD でも PASS する。Theme 復帰自体は本変更以前から成立していた挙動であり、追加された Theme テストは「新機能の証明」ではなく **`resyncFromStore` に Theme を混ぜる改変を禁じるガード**として働く (その役割は上記 M2 で実証済み)

### 3. 追加テストの検出力 (ミューテーション実測)

| ミューテーション | 落ちるテスト | 他テスト |
|---|---|---|
| **M1**: `resyncFromStore(store)` の呼び出しを削除 | `detach 中の Store 更新が再 attach 後に反映される` (`expected:<[A, B2, C]> but was:<[A, B2]>`) | 他 4 件 + `MemoryLeakTest` 2 件は PASS |
| **M2**: `resyncFromStore` で `store.theme.value` を渡す | `detach 中の Theme 変更が再 attach 後に反映される` (ItemDecoration のみ) | 他 4 件 PASS |
| **M3**: 再適用時に `notifyItemRangeChanged(0, itemCount, PAYLOAD_THEME)` を余分に発行 | `初回 attach での再適用は差分通知を出さない` (`expected:<[]> but was:<[changed(0, 3, ks-theme)]>`) | 他 4 件 PASS |

3 件とも**狙ったテストだけが落ちる**形で分離できており、それぞれのテストが独立した性質を固定している。

否定形アサーションの空振り懸念について: `初回 attach での再適用は差分通知を出さない` はテスト内に対照実験 (内容を変えれば同じ観測系で通知が出ることの確認、`assertTrue("内容が変われば差分通知が出る", ...)`) を持っており、観測系そのものが死んでいないことを自己証明している。加えて M3 で「余計な通知が出る退行」を実際に検出できることを外から確認した。**空振りしていない。**

### 4. review-001 の Suggestion (ガードの意図)

`KsSettingsView.kt:240-242` に追記済み。`RecyclerView.setAdapter` が同一インスタンスでも `removeAndRecycleViews` を伴う旨まで書かれており、意図として十分。対応済みと判断する。

### その他の確認

- **リーク対策**: review-001 の参照グラフ分析から変化なし。`resyncFromStore` は `pendingStore` が既に保持している Store を読むだけで新たな強参照を作らない。detach 時の `adapter = null` は据え置き、`MemoryLeakTest` 2 件は M1〜M3 のいずれのミューテーション下でも PASS
- **公開 API**: `resyncFromStore` は private。シグネチャ・可視性の変更なし (Non-Goals 遵守)
- **ADR-0011 (ピッカー復元) との干渉**: `resyncFromStore` → `setRootDirect` は `isRootApplied = true` と `scheduleRestoreScanIfReady()` を呼ぶが、この時点で `isAttachedToHostWindow` はまだ false のため早期 return し、直後に `onAttachedToWindow` 本体が改めて予約する。順序として正しい。`PickerDialogRecreationTest` を含む全 718 件 green
- **足場アーティファクト**: `tasks.md` の差分はチェックボックスのみ。本文改変なし。`review-001.md` も未編集
- **スクロール位置**: スコープ外として指摘しない

### 原状復帰

ミューテーション M1〜M3 および HEAD 版との比較に使った一時改変はすべてバックアップから復帰し、**shasum 一致 (`fce0cb3b09a7820e5a1768d05d1414f012fee97a`) を確認済み**。レビュー用の使い捨てテスト 2 ファイル (`ZzReviewProbeTest.kt` / `ZzReviewProbe2Test.kt`) は `trash` で削除済み。復帰後に `AdapterReattachTest` / `MemoryLeakTest` / `PickerDialogRecreationTest` を再実行して BUILD SUCCESSFUL を確認した。`git status` は実装者の 3 ファイル + レビュー成果物のみを示す状態に戻っている。git の変更操作は行っていない。

## 指摘事項

### [🟡 Minor] 初期 Theme 付き Store を bind したとき `ItemDecoration` に初期 Theme が届かない (既存不具合・本変更のスコープ外)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:290-308` (`bind`) および `:372-386` (`setRootDirect`)

**問題点**:

`bind(store)` は `setRootDirect(store.state.value, store.theme.value)` を呼ぶが、`setRootDirect` は `applyDecoration(style)` を呼ばない。その直後に張られる `store.theme.collect` は `themeBacking` が既に同値になっているため同値スキップで `applyThemeInternal` を回避する。結果、**`ItemDecoration` は構築時の既定 `Theme()` のまま取り残される** (セパレータ色などが初期 Theme に追従しない)。

これは `resyncFromStore` の KDoc が指摘している同値スキップと同じ機構によるもので、実測で確認した:

- probe3 (attach 済み View に `bind`) / probe5 (attach 前に `bind` = Compose `AndroidView.factory` 相当) のいずれも `ItemDecoration も初期 Theme になるか` で FAIL
- **HEAD (本変更適用前) でも同じく FAIL** — つまり本変更が作り込んだものではなく、`resyncFromStore` の追加によって悪化も改善もしていない

到達経路: Compose DSL ラッパ `KsSettingsViewComposable.kt:92` が `SettingsRootStore(initialRoot = initialRoot, initialTheme = theme)` を作って factory で `bind` するため、`KsSettingsView(theme = カスタム Theme) { ... }` の初回描画がこの経路に乗る。

**推奨修正**: `setRootDirect` に `applyDecoration(style)` を足すのが最短だが、同メソッドの KDoc が「`AsyncListDiffer` 在中の `submitList` と競合する `notifyDataSetChanged` 多重呼び出しを避ける」ために通知を出さない設計であることを明記しており、Theme 反映経路 (ADR-0001 の payload 通知) との切り分けを踏まえた検討が要る。**数行で安全に済むとは限らない**。

**スコープの扱いについて**: review-001 の Major と異なり、本件は (a) 本変更が触れた `onAttachedToWindow` / `resyncFromStore` の経路上にはなく、(b) 本変更の有無で挙動が一切変わらない (HEAD でも同じく再現)。本変更を止める理由にはならないため判定には反映しない。別 change として切り出すのが妥当と考えるが、オーナー / オーケストレーターが本サイクル内での対応を選ぶ判断もあり得る。**いずれにせよ、切り出す場合は既知の不具合として記録を残すこと** (無記録での持ち越しは不可)。

### [🔵 Suggestion] テストの待ち合わせが実時間の `Thread.sleep` に依存している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/AdapterReattachTest.kt:95-101` (`idleUntilQuiet`)

**問題点**:

`repeat(30) { idle(); Thread.sleep(5) }` は `AsyncListDiffer` の差分計算がバックグラウンドスレッドへ回る事情への対処として妥当だが、待ち時間の上限が実時間 150ms 固定であり、負荷の高いマシンでは理論上取りこぼしうる。KDoc に理由が書かれている点は良い。

なお本レビューでは、この待ち方が原因で偽 green になっていないことを M1〜M3 で確認している (同じ `idleUntilQuiet` を通しても、退行を注入すればテストは落ちる)。したがって実害は現時点で観測されていない。

**推奨修正 (任意)**: 決定論的にしたい場合は `KsSettingsListAdapter` の `AsyncDifferConfig` にテスト用のバックグラウンド Executor 差し替え点を設ける。ただし本変更のスコープを超えるため、必要になった時点でよい。

## アクションプラン

1. 本変更はこのまま完了させてよい (追加の修正要求なし)
2. **[Minor]** 初期 Theme 付き Store の `ItemDecoration` 未追従は別 change として起票する。本サイクル内で対応する場合も、`setRootDirect` が通知を出さない設計上の理由 (ADR-0001 との関係) を踏まえて修正すること
3. **[Suggestion]** `idleUntilQuiet` の決定論化は、実際に flaky が観測されたら着手する

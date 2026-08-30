# レビュー結果: fix-decoration-theme-not-applied-on-initial-bind (001 回目)

**日付**: 2026-08-04
**判定**: APPROVED

## サマリー

合意済みスコープ (proposal.md の Why / What Changes / Non-Goals、tasks.md グループ 2 の A案) をそのまま満たしている。`setRootDirect` の末尾 (`internalTheme` 代入より後) に `applyDecoration(style)` を 1 行足すだけの修正で、公開 API・同値スキップ・ADR-0001 の通知経路のいずれにも触れていない。追加テスト 3 件は `ItemDecoration` 自体が保持する Theme を直接観測しており、代理値での空振りを避けている。

レビュアー側で全件テスト (1854 件 / 0 failures) と変異注入を独立に実行し、修正を外すと追加テスト 3 件すべてが FAIL することを実測で確認した。Critical / Major / Minor の指摘なし。

## 指摘事項

なし。

## 確認した観点

### 仕様充足

- **合意スコープとの一致**: tasks.md 2.1 の A案 (「`setRootDirect` の末尾 (`internalTheme` 代入より後) に `applyDecoration(style)` を足す。ガードなし」) と実装が完全に一致する (`KsSettingsView.kt:417-418`)。`internalTheme = theme` (408行) より後に置かれており、`applyDecoration` が読む `internalTheme` は常に新 Theme である
- **Non-Goals の遵守**: 公開 API 変更なし。同値スキップ (`themeBacking == value`, `KsSettingsView.kt:205`) は無改変。`applyThemeInternal` の payload 通知経路 (600-624行) も無改変
- **足場アーティファクトの書き換え**: tasks.md グループ 2 の記述更新は、コンテキストパッケージで「探索議論で確定済みの設計判断」として合意済み前提と明示されているため、足場凍結違反として扱わない。他の足場ファイル (proposal.md) は無改変
- **tasks.md の虚偽チェックなし**: 全 10 項目のチェックを個別に検証した (下記「テスト」節)
- **deviation.md**: なし。無断の仕様逸脱も検出しなかった

### 変異注入による裏取り (tasks.md 4.4 の独立再現)

`KsSettingsView.kt:418` の `applyDecoration(style)` をコメントアウトして `InitialThemeDecorationTest` のみを実行:

```
InitialThemeDecorationTest > attach 済み View に初期 Theme 付き Store を bind すると ItemDecoration が初期 Theme になる FAILED
InitialThemeDecorationTest > Modern スタイルでも初期 Theme が ItemDecoration に届く FAILED
InitialThemeDecorationTest > attach 前に初期 Theme 付き Store を bind すると ItemDecoration が初期 Theme になる FAILED
3 tests completed, 3 failed
```

3 件すべてが落ちる。テストが対象経路を実際に踏んでおり、アサーションに検出力があることを確認した (確認後、ファイルは変異前と同一ハッシュに原状復帰済み)。

### テスト

- **全件実行**: `cd android && ANDROID_HOME=… ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`build/test-results/*/TEST-*.xml` 集計で **tests 1854 / failures 0 / errors 0 / skipped 0** (debug + release variant 合計)
- **tasks.md 4.1** (両経路): attach 済み View への `bind` (`InitialThemeDecorationTest.kt:110`) と attach 前の `bind` (同 126行、Compose `AndroidView.factory` 相当) の両方を実装済み。後者は attach 後も初期 Theme が保たれることまで見ており、resync + 同値スキップ通過後の退行も塞いでいる
- **tasks.md 4.2** (既存 Theme 系との両立): `AdapterReattachTest` の `detach 中の Theme 変更が再 attach 後に反映される` (同ファイル 266-308行) と `初回 attach での再適用は差分通知を出さない` (同 311行〜) を含め全件 green。前者は `internalCurrentDecoration()` の Theme を既にアサートしており (306行)、本修正が resync 経路を壊していないことの直接証拠になっている
- **tasks.md 4.3** (ViewHolder 維持契約): `ContentUpdatePayloadTest` 系を含む全件 green。`applyDecoration` は `removeItemDecoration` / `addItemDecoration` / `invalidateItemDecorations` のみで Adapter 通知を一切発行しないため (`KsSettingsView.kt:824-833`)、ADR-0001 の payload 経路と ViewHolder 維持契約に干渉しない — コード上も実測上も確認した
- **観測対象の妥当性** (tasks.md 1.2): `decorationTheme()` は `internalCurrentDecoration()` から実オブジェクトを取り出して `theme` を見ている (`InitialThemeDecorationTest.kt:106-107`)。`internalTheme` や RecyclerView 背景色は `setRootDirect` が直接更新するため通過してしまう — その代理値を避けた設計になっている
- **既定 Theme との差の担保**: `assertNotEquals(Theme(), theme)` を各テストの先頭に置き、「取り残されたまま」でも一致してしまう空振りを防いでいる (116行 / 135行)
- **Robolectric の限界への配慮**: `concepts/cross/conventions/test-execution.md` の描画系アサーションの空振り (legacy graphics) を踏まない設計。描画結果ではなく decoration が保持する Theme を見ている。本件は純ロジック不具合 (ユニットテストで症状自体を再現できる) であり、`conventions/runtime-behavior-verification.md` の対象外 — 実機再現の要求は発生しない
- **既存テストとの一貫性**: `HostActivity` / `idle()` / `idleUntilQuiet()` / `@Config(sdk = [33])` はいずれも `AdapterReattachTest` と同形。プロジェクト内 29 箇所すべてが `sdk = [33]` で統一されている

### 設計品質

- **`resyncFromStore` の KDoc 更新の正確性** (`KsSettingsView.kt:326-331`): 修正後は decoration も `setRootDirect` で追従するため、「ItemDecoration が古い Theme のまま残る」という旧記述は事実でなくなる。新記述「各 ViewHolder への再 bind 通知が発行されず、既に bind 済みの Cell が古い Theme の配色のまま残る」は正しい — `resyncFromStore` は同一 root を渡すため `submitList` が DiffUtil no-op となり、`applyThemeInternal` の `notifyItemRangeChanged` も同値スキップで走らないため、既存 ViewHolder は再 bind されない
- **decoration 再生成による状態喪失なし**: `ClassicSectionDecoration` / `ModernSectionDecoration` はいずれも `theme` と定数・`Paint` / `RectF` のみを持ち、フレーム間で持ち越す状態を持たない。作り直しで失われる情報はない
- **`setRootDirect` の他の呼び出し元への波及**: `applyDiff` の `Full` (427行) / `ReplaceSection` (474行) / 可視性切替を伴う `ReplaceCell` (551行) / `resyncFromStore` (334行) / `bind` (301行・311行) でも decoration が毎回作り直される。tasks.md 2.1 が「ガードなし」を明示的に選択した合意事項であり、(a) Adapter 通知を発行しない、(b) decoration は状態を持たない、(c) 全件テスト green、の 3 点から実害を確認できなかったため指摘としては挙げない
- **`addItemDecoration` の呼び出しタイミング**: `RecyclerView.addItemDecoration` はレイアウト計算中に呼ぶと `IllegalStateException` を投げる。到達経路 (`bind` / `onAttachedToWindow` / coroutine collect) はいずれもレイアウト中ではなく、かつ公開 `theme` setter → `applyThemeInternal` → `applyDecoration` という同じ露出が修正前から存在するため、本修正で新たに増えるリスクではないと判断した (実測での再現なし)
- **コメント規約 (lessons impl.md L-001 / L-002)**: `python3 scripts/comment-policy-lint.py` で `KsSettingsView.kt` の禁止 0 件。追加・改稿されたコメントに変更提案・レビュー文書への参照、議論通番、デルタスペック語彙はなく、時間軸を持ち込む記述 (「〜で新規追加」等) もない。すべて現在形の仕様説明になっている。テストファイルは lint の検査対象外だが、目視でも同種の記述は検出しなかった
- **Kotlin イディオム**: 追加コードは 1 行の関数呼び出しのみ。`!!` / `GlobalScope` / `runBlocking` / 可変状態の追加はなく、指摘対象になる要素がない。本プロジェクトに ktlint / detekt の設定は存在しないため静的解析ゲートは非適用

### 変更範囲

`git status` で確認した変更は 3 ファイルのみ。iOS / MAUI / Compose モジュールへの波及なし。新規ファイル (untracked) の `InitialThemeDecorationTest.kt` も内容を全行読んだうえでレビュー対象に含めている。

## アクションプラン

対応すべき指摘なし。本変更はこのままオーナーレビュー / 蒸留 (ksn-distill) へ進めてよい。

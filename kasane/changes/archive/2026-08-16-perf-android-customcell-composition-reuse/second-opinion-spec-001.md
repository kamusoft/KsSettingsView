# セカンドオピニオン: perf-android-customcell-composition-reuse (spec-001)
**相方**: codex / **日付**: 2026-08-15 / **対象**: 提案一式 (proposal / exploration / specs / tasks / proposed ADR)
---
# レビュー結果: perf-android-customcell-composition-reuse

**日付**: 2026-08-15  
**判定**: **NEEDS_DISCUSSION**

## サマリー

pool-aware 戦略への移行自体には妥当性があります。しかし、現在の `reset()` はプール投入時に content subtree を空へ置き換えるため、提案が主張する再構築削減と両立していません。また、ライフサイクル境界と ADR の置換関係にも仕様上の矛盾があります。

指摘件数: Critical 0 / Major 4 / Minor 3 / Suggestion 0

## 指摘事項

### [🟠 Major] `reset()` が content subtree を破棄するため、性能目的が仕様上保証されない

**該当箇所**: [proposal.md:5](kasane/changes/perf-android-customcell-composition-reuse/proposal.md:5)、[spec.md:17](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:17)、[spec.md:45](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:45)、[KsSettingsListAdapter.kt:218](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:218)、[CustomCellViewHolder.kt:98](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:98)

**問題点**: ViewHolder が実際にプールへ送られる直前、`onViewRecycled()` が `reset()` を呼び、`composeView.setContent {}` によって `CustomCellRow` と利用者 content の subtree を Composition から除去します。ルート Composition オブジェクトは残っても、問題視している content の slot group・LayoutNode・`remember`・effect は再 bind 時に作り直されます。

したがって、仕様の「Composition を破棄・新規生成しない」は満たせても、proposal が動機としている「slot table + LayoutNode ツリーの再構築を避ける」ことは保証されません。また、この経路では `reset()` だけで行間状態が消えるため、`key(cell.id)` がなくても隔離テストが通ります。

**推奨修正**: 次のどちらを契約にするか決定してください。

- content subtree もプール中に保持するなら、リスナーだけを reset し、content の切断は別 Cell の bind またはプール release まで遅延させる。その場合に `key(cell.id)` が行間隔離を担う。
- 現行 `reset()` を維持するなら、改善対象を「ルート Composition/AndroidComposeView の再生成コスト」に限定し、LayoutNode/content subtree の再利用を主張しない。効果は計測で確認する。

### [🟠 Major] Composition の破棄境界が実際の RecyclerView ライフサイクルと一致しない

**該当箇所**: [spec.md:9](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:9)、[spec.md:23](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:23)、[ADR-0015:21](kasane/decisions/android/0015-customcell-pool-aware-composition-disposal.md:21)、[KsSettingsView.kt:270](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:270)

**問題点**: spec は破棄を「プールからの放逐または RecyclerView の破棄」としていますが、RecyclerView 1.3.2 の pooling-container 実装は、RecyclerView の一時的な window detach でも release を通知します。さらに本プロジェクトは `KsSettingsView.onDetachedFromWindow()` で `recyclerView.adapter = null` とし、保持中の ViewHolder を解放します。

つまり、ViewPager2 や `AndroidView` の一時 detach/reattach でも Composition は破棄され得ます。「RecyclerView の破棄」は Android View に明確なイベントがなく、受け入れ条件としても曖昧です。

加えて `ViewCompositionStrategy.Default` は現在 pool-aware ですが、Compose 側では実装詳細として変更可能です。厳密な SHALL を可変な既定値へ委ねています。

**推奨修正**:

- 「行だけが detach され、RecyclerView は window に attach されたまま」
- 「RecyclerView/KsSettingsView 自体が一時 detach された」
- 「pool clear・容量超過で release された」
- 「detach 後に同じ Host が再 attach された」

の各境界を明記してください。厳密な契約にするなら、既定値任せではなく `DisposeOnDetachedFromWindowOrReleasedFromPool` の明示指定も検討すべきです。

### [🟠 Major] 中核テストが pool-aware 戦略と `key` の欠落を検出できない

**該当箇所**: [tasks.md:11](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:11)、[tasks.md:12](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:12)、[KsBridgeCustomCellTest.kt:423](android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCustomCellTest.kt:423)

**問題点**: `scrollToPosition()` だけでは対象 ViewHolder が RecyclerView の offscreen cache に残ったのか、RecycledViewPool に入ったのかを判別できません。同じ ViewHolder が戻ったことも既存テストは確認していません。

また、content 内の `DisposableEffect` はルート Composition が生存していても `reset()` の空 content 化で dispose されます。このカウンタでは、ルート Composition の生存・pool release 時の破棄を区別できません。

そのため以下の誤実装でもテストが green になり得ます。

- pool-aware 戦略が機能していない
- `key(cell.id)` が実装されていない
- 実際には同じ ViewHolder を再利用していない

**推奨修正**:

- `setItemViewCacheSize(0)` などで cache と pool を区別する
- 対象 ViewHolder/ComposeView の同一性と pool 件数を確認する
- `ComposeView.hasComposition` でルート Composition の生存と clear 後の破棄を確認する
- `key` の検証は、`reset()` が状態を消してしまわない直接 A→B bind 経路、または Major 1 の設計変更後の実プール経路で行う
- 旧戦略復帰・`key` 削除のミューテーションでそれぞれ対象テストが失敗することを確認する

### [🟠 Major] proposed ADR が accepted ADR の一部だけを非公式に上書きしている

**該当箇所**: [android/ADR-0015:24](kasane/decisions/android/0015-customcell-pool-aware-composition-disposal.md:24)、[core/ADR-0015:18](kasane/decisions/core/0015-customcell-exemption-from-shared-row-layout.md:18)、[core/index.md:16](kasane/decisions/core/index.md:16)

**問題点**: accepted の `core/ADR-0015` は Android に `DisposeOnDetachedFromWindow` を必須としています。一方、新しい `android/ADR-0015` はその Android 条項だけを「置き換える」としていますが、`supersedes` 関係がなく、core index 上も旧 ADR は accepted のままです。

長命層には相反する二つの決定が残り、core 側だけを読んだ実装者は旧戦略を再導入できます。Kasane の ADR 規約にも「部分的な暗黙上書き」は定義されていません。

**推奨修正**: core 側に `core/ADR-0015` 全体を supersede する新 ADR を作り、以下を再記述してください。

- CustomCell が共通行レイアウトの適用除外であること
- Android lifecycle は `android/ADR-0015` に従うこと
- iOS 条項は従前どおり有効であること

旧 ADR と index も superseded として追跡可能にする必要があります。

### [🟡 Minor] `reset` の「builder 参照切断」が Scenario から判定できない

**該当箇所**: [spec.md:45](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:45)、[spec.md:49](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:49)、[tasks.md:13](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:13)

**問題点**: Requirement はリーク防止として「前 builder への参照を切る」と規定していますが、Scenario は表示と click listener しか確認しません。古い builder を内部に保持したまま空表示にしても合格します。

**推奨修正**: Major 1 の設計決定後、参照切断を契約に残すなら、reset 後の effect disposalや、holder 管理 state が旧 content を保持しないことを検証する Scenario を追加してください。

### [🟡 Minor] 完了ゲートが Android 全テスト実行規約を満たさない

**該当箇所**: [tasks.md:14](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:14)、[tasks.md:15](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:15)、[test-execution.md:68](kasane/concepts/cross/conventions/test-execution.md:68)

**問題点**: tasks は UI モジュール全件と Bridge の特定クラスだけを完了ゲートにしています。プロジェクト規約は Android ルートの `./gradlew test` と実行件数確認を要求しています。

**推奨修正**: targeted test は反復用として残し、最終タスクを `cd android && ./gradlew test --rerun-tasks` と debug/release の実行件数・失敗数確認に変更してください。

### [🟡 Minor] 実装コメントの ADR 参照形式が規約どおりでない

**該当箇所**: [tasks.md:5](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:5)、[comment-policy.md:19](kasane/concepts/cross/conventions/comment-policy.md:19)

**問題点**: タスクはコードコメントへ `ADR-0015` と記載するよう指示していますが、ドメイン分割後の正式形式は `<domain>/ADR-NNNN` です。

**推奨修正**: タスクを「`android/ADR-0015` コメントを残す」に修正してください。

## アクションプラン

1. content subtree をプール中も保持するのか、ルート Composition だけを再利用するのか決定する。
2. RecyclerView/KsSettingsView の一時 detach を含め、破棄境界を Scenario 化する。
3. `core/ADR-0015` と `android/ADR-0015` の supersede 構造を整理する。
4. cache・pool・root Composition を区別できるテスト設計へ変更し、ミューテーションで検出力を確認する。
5. reset の参照解放条件、Android 全件テスト、ADR コメント形式を tasks に反映する。

依頼どおり静的レビューのみを行い、ビルド・テスト実行およびファイル変更は行っていません。

## 突き合わせ結果 (ホスト側判定: 2026-08-15)

| # | 指摘 | 採否 | 判定根拠 |
|---|---|---|---|
| Major 1 | reset() の空 content 化により content subtree 再利用は保証されず、性能主張が過大 | **採用** | 相方のみ + 根拠強。onViewRecycled → reset() → setContent {} がプール滞在中の recomposition で subtree を除去するため、クロスフレーム経路では再 bind 時に subtree 再構築が起きる。ただし「key が無くても隔離テストが通る」は同一フレーム内 recycle→rebind (state 書き込みが coalesce する経路) には当てはまらず、key はこの経路の隔離に必要 — 契約の再スコープで対応 (方針はユーザー判断へ) |
| Major 2 | 破棄境界が実ライフサイクル (一時 detach・adapter=null) と不一致。既定値任せの SHALL は不安定 | **採用** | 相方のみ + 根拠強 (KsSettingsView.onDetachedFromWindow の adapter=null を特定)。境界の Scenario 化と、既定値依存をやめ `DisposeOnDetachedFromWindowOrReleasedFromPool` 明示指定への変更で対応 |
| Major 3 | テスト設計が cache/pool・key 欠落・holder 同一性を検出できない | **採用** | 相方のみ + 根拠強 (誤実装が green になる具体経路を提示)。tasks のテスト設計へ反映 (cache 0・同一性 assert・hasComposition・ミューテーション確認・同一フレーム A→B 直接 bind) |
| Major 4 | accepted な core/ADR-0015 の部分的・非公式上書き | **採用 (対応方式はユーザー判断)** | 構造課題として妥当。ただし本リポジトリには core/ADR-0015 自身が ADR-0011 を supersede せず適用範囲を絞った先例があり、全面 supersede (相方案) と index 注記による追跡可能化 (軽量案) の選択はオーナー判断へ |
| Minor 1 | 「builder 参照切断」が Scenario から判定不能 | **採用** | 参照切断の検証可能な Scenario (弱参照 GC) を追加 |
| Minor 2 | 完了ゲートが Android 全テスト実行規約 (test-execution.md) 不足 | **採用** | 規約実在を確認。tasks 修正 |
| Minor 3 | ADR コメント参照形式が `<domain>/ADR-NNNN` 規約と不一致 | **採用** | comment-policy 規約どおり。tasks 修正 |

降格: 0 件 / 未解決 (ユーザー判断待ち): Major 1 の契約方針・Major 4 の対応方式

## 最終処理 (2026-08-15 追記)

- Major 1: オーナー指示により目的を「content のリサイクル成立」に再固定し、相方の選択肢1の系譜である **ReusableContent / ReusableContentHost 方式**へ設計転換 (compose 1.7.5 実ソースで成立性を裏取り済み)。key 方式は android/ADR-0015 の Alternatives に却下理由付きで記録
- Major 4: オーナー判断で **supersede 方式**を採用。core/ADR-0022 (proposed) が core/ADR-0015 を supersede
- Major 2, 3, Minor 1-3: 書き直した spec / tasks に反映済み

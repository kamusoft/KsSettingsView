# セカンドオピニオン: perf-android-customcell-composition-reuse (spec-002)
**相方**: codex / **日付**: 2026-08-15 / **対象**: ReusableContent 方式への書き直し版一式 (proposal / specs / tasks / ADR android-0015, core-0022)
---
# レビュー結果: perf-android-customcell-composition-reuse

**判定**: **NEEDS_DISCUSSION**

## サマリー

`ReusableContent` 採用により旧案の主要問題は解消されていますが、RecyclerView の item cache、ノード保持と参照切断の両立、Compose が保証する再利用条件に仕様上の穴があります。また、Bridge の新 lifecycle 経路と ADR の委譲構造も現在の受け入れ基準では十分に固定されていません。

指摘件数: **Critical 0 / Major 5 / Minor 1 / Suggestion 0**

## 指摘事項

### [🟠 Major] itemViewCache 滞在中の content lifecycle が未定義

**該当箇所**: [spec.md:9](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:9)、[tasks.md:13](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:13)、[proposal.md:29](kasane/changes/perf-android-customcell-composition-reuse/proposal.md:29)、[KsSettingsListAdapter.kt:218](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:218)

**問題点**: `reset()` は `onViewRecycled()` でしか呼ばれません。RecyclerView 1.3.2 は、画面外になった holder を既定の itemViewCache に入れる場合、`onViewRecycled()` を通知しません。そのためキャッシュ滞在中は `active=true` のままで、前 Cell の `remember`・`DisposableEffect`・購読が画面外でも継続します。

テスト方針は `setItemViewCacheSize(0)` によってこの経路を意図的に除外しており、本番既定設定の挙動が未検証です。また、メモリ増加は RecycledViewPool 上限だけでなく itemViewCache と prefetch 保持分にも及びます。

**推奨修正**: 次のどちらを契約にするか決め、Scenario を追加してください。

- cache 滞在中も content を active のまま保持する。その場合、effect・購読の継続とメモリ上限を明記し、非ゼロ cache のテストを追加する。
- 行 detach 時点で content を deactivate し、attach 時に reactivate する。その場合、Adapter の attach/detach callback を含む lifecycle を設計する。

### [🟠 Major] ノード保持と「reset 時点で builder 参照を完全切断」は両立が保証されない

**該当箇所**: [spec.md:61](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:61)、[spec.md:71](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:71)、[tasks.md:18](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:18)、[android/ADR-0015:22](kasane/decisions/android/0015-customcell-pool-aware-composition-disposal.md:22)

**問題点**: `ReusableContentHost(active=false)` が保証するのは remembered object と effect の破棄であり、保持したノードが持つ任意の参照の消去ではありません。

Compose 1.7.5 では、deactivate 時も LayoutNode は保持されます。例えば以下は古い content 由来の参照を保持し得ます。

- Modifier やその callback
- `AndroidView(onReset=...)` の `ViewFactoryHolder` が持つ View
- `update` / `onReset` / `onRelease` callback
- Compose compiler が生成したパラメータ用 slot

したがって、ViewHolder 自身の `contentState = null` は保証できても、「任意 builder と、その builder だけが起点だった参照対象が reset 直後に GC 可能」という現在の SHALL は保証できません。WeakReference テストも、合法な builder の構造によって結果が変わります。

**推奨修正**:

- 契約を「ViewHolder 自身の content state と click listener が builder を保持しない」「effect は deactivation の反映完了までに dispose される」へ限定する。
- retained node が持つ参照は、次の reuse または Composition release まで残り得ることを Consequences に明記する。
- reset 時の完全な GC 可能性が必須なら、任意ノード保持との両立は困難なため、reuse の範囲を狭める設計判断が必要です。

### [🟠 Major] 「builder 出力の構造一致」だけではノード再利用を保証できない

**該当箇所**: [spec.md:29](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:29)、[spec.md:33](kasane/changes/perf-android-customcell-composition-reuse/specs/settings-view-android-ui/spec.md:33)、[proposal.md:13](kasane/changes/perf-android-customcell-composition-reuse/proposal.md:13)、[android/ADR-0015:34](kasane/decisions/android/0015-customcell-pool-aware-composition-disposal.md:34)

**問題点**: Compose の再利用条件は、見た目上の出力ツリーが同型であることだけではありません。Compose compiler の group/call-site 構造が互換で、かつ対象が reusable node である必要があります。異なる場所で定義された2つの builder が同じ `Row`・`Box` を出力しても、同一構造として再利用される保証はありません。

現 Scenario の「同一ラップ関数」は成立しやすい条件ですが、Requirement と ADR は任意の「構造一致部分」へ保証を広げています。

**推奨修正**: Requirement を次の範囲へ限定してください。

- 同一ラップ関数など、互換な composable group structure を持つ builder
- `ReusableComposeNode` として生成されるノード
- `AndroidView` は `onReset` が指定されたものだけ

構造不一致 Scenario も「B の testTag/text が存在し、A のものが存在しない」など、表示の正しさを判定できる基準へ具体化してください。

### [🟠 Major] Bridge 回帰ゲートが新しい onDeactivate 経路を通ったことを証明しない

**該当箇所**: [proposal.md:15](kasane/changes/perf-android-customcell-composition-reuse/proposal.md:15)、[tasks.md:21](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:21)、[tasks.md:26](kasane/changes/perf-android-customcell-composition-reuse/tasks.md:26)、[KsBridgeCustomCellTest.kt:421](android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCustomCellTest.kt:421)、[KsBridgeCellContentView.kt:63](android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCellContentView.kt:63)

**問題点**: Bridge の `AndroidView` は `onReset` を指定しておらず、ReusableContent の reuse 時には強制置換される設計です。ところが task 2.7 は既存テストを実行するだけで、既存テストは以下を確認していません。

- 対象 holder が itemViewCache ではなく pool に入ったこと
- Composition が生存したまま content が deactivate されたこと
- platform View が `AndroidViewHolder.onDeactivate` で外されたこと
- 同じ platform View が再 bind 後に再親付けされたこと

スクロール後に同じ View が表示されたという最終結果だけでは、新しい危険経路を通らなくても green になり得ます。デルタスペックにも Bridge の受け入れ Scenario がありません。

**推奨修正**: Bridge 用 Scenario と専用回帰テストを追加してください。`setItemViewCacheSize(0)`、旧 holder/ComposeView の保持、`hasComposition == true`、probe の detach/attach 回数、再表示後の View 同一性を組み合わせると新経路を判定できます。実機検証は同じ手順の証跡も change 配下へ残してください。

### [🟠 Major] core ADR の「全面委譲」と実際の Decision が矛盾している

**該当箇所**: [core/ADR-0022:6](kasane/decisions/core/0022-customcell-lifecycle-delegated-to-platform-adr.md:6)、[core/ADR-0022:17](kasane/decisions/core/0022-customcell-lifecycle-delegated-to-platform-adr.md:17)、[core/ADR-0022:18](kasane/decisions/core/0022-customcell-lifecycle-delegated-to-platform-adr.md:18)、[core/ADR-0022:28](kasane/decisions/core/0022-customcell-lifecycle-delegated-to-platform-adr.md:28)、[ios/index.md:1](kasane/decisions/ios/index.md:1)

**問題点**: ADR-0022 は ADR-0015 を supersede するとしながら、Decision では適用除外の決定を「core/ADR-0015 のまま維持する」と記述しています。superseded ADR が有効な決定の所在として残るように読めます。

また「具体機構は core では定めず platform ADR に委譲する」としながら、iOS の具体機構を `UIHostingConfiguration` の構成差し替えと core ADR 内で規定しています。対応する iOS ADR は存在しないため、「今後は core 改訂なしで完結する」という Consequence も iOS について成立しません。

**推奨修正**: 次のいずれかへ統一してください。

- ADR-0022 本文へ旧 ADR の platform 非依存部分を完全に引き継ぎ、Android・iOS 両方の具体機構を platform ADR へ移す。
- 今回は Android だけを委譲し、iOS は core に残す暫定非対称としてタイトル・Decision・Consequences を修正する。

### [🟡 Minor] proposal の domain が複数ドメイン変更と一致しない

**該当箇所**: [proposal.md:37](kasane/changes/perf-android-customcell-composition-reuse/proposal.md:37)、[concepts/rules.md:16](kasane/concepts/rules.md:16)

**問題点**: proposal は `android` コード・ADR に加えて `core/ADR-0022` を変更しています。プロジェクト規約では複数ドメインに触る proposal の `domain:` は `cross` です。現在値のままだと、スキル解決や蒸留先判断が Android 単独変更として扱われます。

**推奨修正**: `domain: cross` とし、実際に触る platform として Android の domain skill overlay を結合する前提を実装パッケージに含めてください。

## アクションプラン

1. itemViewCache 滞在中の active/effect lifecycle を決定する。
2. reset の参照切断保証を、ReusableContent が実際に保証できる範囲へ修正する。
3. ノード再利用条件を composable group structure と reusable node に限定する。
4. Bridge の deactivate→再親付けを明示的な Scenario とテストにする。
5. core ADR の全面委譲か Android 限定委譲かを決定する。
6. proposal の `domain` を複数ドメイン規約へ合わせる。

依頼どおり静的レビューのみを行い、ビルド・テスト実行およびファイル作成は行っていません。

## 突き合わせ結果 (ホスト側判定: 2026-08-15)

| # | 指摘 | 採否 | 判定根拠 |
|---|---|---|---|
| Major 1 | itemViewCache 滞在中の lifecycle 未定義 (reset は onViewRecycled のみ、cache 経由では active 継続) | **採用 (契約方針はユーザー判断)** | 根拠強。旧実装は detach で必ず破棄していたため cache 滞在中に effect が生きる経路は新規挙動。cache-active 容認 (公式 interop パターンと同型・有界) か detach 時 deactivate かの契約決定が必要 |
| Major 2 | 「reset 直後に builder が GC 可能」SHALL は retained node の残存参照により保証不能 | **採用** | 根拠強。deactivate が保証するのは remember/effect の破棄であり、保持ノードの Modifier・slot 等が旧 content 参照を持ち得る。契約を「ViewHolder 直接参照の切断 + プール放逐後の GC 可能性」へ限定 |
| Major 3 | 「構造一致」への保証が広すぎ (Compose の reuse 条件は call-site/group 互換 + reusable node) | **採用** | 根拠強。Requirement を同一 builder (同一 composable 呼び出し構造)・reusable node・onReset 付き AndroidView に限定し、縮退 Scenario の判定基準を具体化 |
| Major 4 | Bridge 回帰ゲートが新経路 (deactivate→強制置換→再親付け) の通過を証明しない | **採用** | 根拠強。既存テストは最終結果のみで経路を観測しない。Bridge 用デルタスペックと専用回帰テストを追加 |
| Major 5 | core/ADR-0022 の矛盾 (supersede なのに「0015 のまま維持」表現・iOS 機構が core 残留で委譲が不完全) | **採用** | 起草ミス。0022 本文へ決定を完全再記述し、委譲は Android 限定の暫定非対称として明記する方向で修正 |
| Minor 1 | 複数ドメイン (core ADR + android) に触るため proposal の domain は cross | **採用** | concepts/rules.md の規約どおり。domain: cross へ変更 |

降格: 0 件 / 未解決 (ユーザー判断待ち): Major 1 の cache 滞在中 lifecycle の契約

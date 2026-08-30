# Proposal: perf-android-customcell-composition-reuse

## Why

CustomCell はリサイクルで器 (ViewHolder / ComposeView) こそ再利用されるが、`DisposeOnDetachedFromWindow` の強制と `reset()` の空 content 化により、**中身 (Composition と content ツリー) はスクロールの度に全破棄・全再構築**されている。これはオリジナル AiForms が「型キーで器のリサイクルは効くが、中身は MauiView から毎回作り直し」だった積み残しと同型の問題であり、本変更の目的は **CustomCell の中身までリサイクルを効かせること**。

Compose には LazyColumn の item リサイクルに使われる公式機構 `ReusableContent` / `ReusableContentHost` (deactivate+reuse: 状態は Cell 単位で破棄し、ノードツリーの再利用可能部分は保持) が存在し、プレーンな ComposeView 直下でも機能することを compose 1.7.5 実ソースで確認済み (exploration.md 追記)。

## What Changes

- `ComposeCellViewHolder` の破棄戦略を `DisposeOnDetachedFromWindowOrReleasedFromPool` の明示指定へ変更する (行 detach では破棄せず、プール放逐・pooling container 都合の解放で破棄)
- `CustomCellViewHolder` を「setContent は生成時に一度だけ + `ReusableContentHost(active)` / `ReusableContent(cellId)` + state 経由更新」の構成へ変更する。bind = state 更新と View 側適用、reset (プール投入時のみ) = deactivate + content state 切り離し。itemViewCache 滞在中は active を維持する (行は bind なしで再表示され得るため)
- これにより: 同一ラップ関数 builder の Cell 間の再 bind で reusable ノード (reusable node・onReset 付き AndroidView) が再利用され、remember / DisposableEffect は Cell 単位で破棄され行間隔離が保たれる
- 回帰テスト新設: プール生存/破棄境界・cache 経由の継続・ノード再利用・同一フレーム再 bind の状態隔離・reset の参照切断 (テスト設計指針は tasks.md)
- MAUI Bridge: 保全契約をデルタスペック化 (`specs/maui-bridge/`) し、deactivate 経路を明示的に通す専用回帰テストを新設。実機高速フリック再検証の証跡も change 配下に残す
- 影響 capability: `settings-view-android-ui` / `maui-bridge`
- 決定層: [core/ADR-0022](../../decisions/core/0022-customcell-lifecycle-delegated-to-platform-adr.md) (proposed) が core/ADR-0015 を supersede (適用除外の決定を再記述し、Android の lifecycle 機構を platform ADR へ委譲)、[android/ADR-0015](../../decisions/android/0015-customcell-pool-aware-composition-disposal.md) (proposed) が ReusableContent 方式を規定。accepted 昇格は実装完了時の蒸留で

## Non-Goals

- **Section/Root Accessory 経路 (`SectionAccessoryViewHolders`) の変更** — H/F はセクション数分しか存在せず利得が小さい。CustomCell 側の効果確認後の追随候補
- **iOS 側の対応** — 計測スパイクで効果を確定してから別 change として判断 (2段構えの第2弾)。SwiftUI には deactivate+reuse に相当する公開機構がないため、iOS は同型の解が使えない可能性も含めて別途調査
- **id 未指定 CustomCell の UUID 既定 id の安定化** — cellId が変わる使い方では reuse の恩恵が effect 再実行込みになるだけで、現状より悪化しない。id 安定化は別論点
- **性能の定量実測 (Macrobenchmark)** — 機構の成立はテストで固定するが、フレーム時間の定量化は必要になった時点で別途

## Impact

- 公開 API 変更なし。破壊的変更なし。ViewHolder 内部構成の変更に閉じる (戻す場合は旧構成へ可逆)
- 挙動変化: (1) プール滞在中も Composition が生存し deactivated ノードがメモリに残る (RecycledViewPool・itemViewCache・prefetch の保持分に比例した有界の増加)、(2) **itemViewCache 滞在中は content が活性のまま維持され、画面外でも effect・購読が継続する** (cache 上限 + prefetch 分に有界。旧実装は detach で即破棄)、(3) 同一 Cell が生存 Composition へ再表示された場合に content 内部状態が維持されることがある (cache 経由は契約、プール経由は非契約)、(4) 保持ノードが持つ旧 content 由来の参照は次の再利用または Composition 破棄まで残り得る (ViewHolder 直接参照は reset で切断)、(5) `AndroidViewHolder.onDeactivate` が Bridge 埋め込みに対して新たに走る (専用回帰テスト + 実機再検証で裏取り)
- MAUI Bridge 経由の CustomCell も同経路のため恩恵を受ける (platform view 本体は既に保持済みで、殻の再構築が減る)
- 触る範囲: `ks-settingsview-ui` (ComposeCellViewHolder / CustomCellViewHolder) + テスト、`ks-settingsview-bridge` はテスト追加のみ。決定層は core / android の 2 ドメイン

## 級: M

コード変更は 2 ファイルに閉じるが、core ADR の supersede を伴い、reuse/隔離という新しい挙動契約への回帰テスト新設が本体のため。

domain: cross
(コードの実装対象は android 系統のみ — 実装時のスキル解決は android の domain-skills overlay を用いる)

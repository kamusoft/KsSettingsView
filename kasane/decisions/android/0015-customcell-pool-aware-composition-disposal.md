---
id: 0015
title: CustomCell の宣言 UI ホスティングは ReusableContent の deactivate+reuse でリサイクルする
status: accepted
date: 2026-08-16
---

## Context

CustomCell の Android ホスティングは `ComposeCellViewHolder` が `DisposeOnDetachedFromWindow` を強制し (初期設計 `add-settings-view-android-ui` design.md Decision 5 由来の予防的決定)、さらに `reset()` が `setContent {}` で content を空にしていた。この構成ではスクロールアウト毎に Composition と content ツリーが全破棄され、リサイクルの利得が「ComposeView インスタンスの再確保を省く」だけに縮む。当時のレビューも「重い経路」と指摘済みだった (review-result_001.md)。

2026-08-15 の調査で以下を確認した (compose-runtime / compose-ui 1.7.5 実ソース):

- 破棄戦略を pool-aware (`DisposeOnDetachedFromWindowOrReleasedFromPool`) にするだけでは、`reset()` の空 content 化が content subtree を除去するため、節約は root Composition の生成・破棄回避に留まる (second-opinion spec-001 Major 1)。
- Compose には LazyColumn の item リサイクルと同じ公式機構 `ReusableContent(key)` / `ReusableContentHost(active)` が public に存在し、プレーンな ComposeView 直下でも機能する。key (dataKey) 変更時に remember / DisposableEffect を破棄しつつ LayoutNode ツリーの構造一致部分を再利用し、active=false (deactivate) で状態を破棄しつつノードを保持する。
- onReset を渡していない `AndroidView` は非 reusable ノードとして強制置換されるため、MAUI Bridge の埋め込み (再親付け設計) は reuse 経路に乗らず、既存前提は維持される。

## Decision

- `ComposeCellViewHolder` の破棄戦略を `DisposeOnDetachedFromWindowOrReleasedFromPool` の明示指定に変更する (可変な既定値には委ねない)。行の detach では Composition を破棄せず、プールからの放逐・pooling container 都合の解放で破棄する。
- `CustomCellViewHolder` は `setContent` を生成時に一度だけ張り、`ReusableContentHost(active)` + `ReusableContent(cellId)` + state 経由のパラメータ更新で構成する。bind は state 更新 (cellId・content・表示パラメータ・active=true) と View 側適用のみとする。
- `reset()` は空 content 化 (`setContent {}`) をやめ、deactivate (active=false) と content state の切り離しで行う。reset 時点で確実に成立するのは ViewHolder が直接握る参照 (content state・click listener) の切断であり、remember / DisposableEffect の破棄・購読停止は非活性化が再 composition に反映された時点で成立する。保持されたノードが持つ旧 content 由来の参照 (Modifier・パラメータ slot 等) は、次の再利用または Composition の破棄まで残ることを許容する。
- deactivate はプール投入時 (onViewRecycled) のみ行い、itemViewCache 滞在中は content を活性のまま維持する (行は bind を経ずに再表示され得るため。画面外での effect・購読の継続は cache 上限 + prefetch 分に有界)。
- `cellId` には Cell インスタンスの同一性を表す安定 ID を用いる (position や content ハッシュは不可)。
- 宣言ツリーは `Layout` で包み、「content の composition が適用済み (活性) の間だけ子を measure し、非活性の間は行の高さだけ確保して measure を見送る」measure policy を自前で持たせる。非活性化された LayoutNode はツリーから外れずに残る一方、`ComposeView` のルート measure policy は非活性判定なしで子を measure し、RecyclerView は bind と同一レイアウトパスで同期 measure するため、このガードがないと `measure is called on a deactivated node` の FATAL になる (実機で実測)。`ReusableContentHost` は本来 `SubcomposeLayout` の「非活性 slot を measure しない」measure policy とセットで成立する機構であり、プレーンな ComposeView 直下で使う場合は同等の責務を利用側が持つ必要がある。

## Alternatives Considered

- **現状維持 (`DisposeOnDetachedFromWindow` + reset 空 content 化)** — 公式が削除を推奨する経路で、スクロール毎の Composition・content 全再構築という性能損失が明確。却下。
- **戦略撤廃 + `key(cellId)` による discard 型隔離** — key はサブツリーを破棄して作り直す機構であり、content ノードの再利用が原理的に成立しない。reset の空 content 化と合わせると節約が root Composition の生成回避のみに縮み、変更の主目的 (content のリサイクル) を満たさない (second-opinion spec-001 Major 1)。却下。
- **content を破棄も隔離もせず保持する** — 別の Cell の行として再 bind されたとき remember 状態・購読が行間で持ち越され、正しさが崩れる。却下。
- **setContent 一回化 + state 経由更新 (単独)** — `ComposeView.setContent` は内部的に state 書き込みのみで、単独では効果がない。ReusableContentHost の土台としてのみ採用。
- **measure パス内の `SubcomposeLayout.subcompose` による同期再活性化** — 非活性 measure 問題を同期 composition で解く案。content の materialize が measure パスに依存するため、RecyclerView の測定キャッシュで measure が省かれる経路では content が composition されないまま表示される。却下。

## Consequences

- 正: 同一 builder (同一の composable 呼び出し構造) の Cell 間の再 bind で、reusable なノード (reusable node・onReset 付き AndroidView) が再利用される。同一のラップ関数 builder を多数行で共有するケース (性能が最も問題になる形) が該当する。異なる builder 間や非 reusable ノードは作り直しに縮退し、現状より悪化しない (Compose の再利用条件は call-site の group 構造互換であり、見た目上の同型では保証されない)。
- 正: remember / DisposableEffect は Cell 単位で破棄され行間隔離が保たれる。builder 参照の reset 時切断 (リーク防止保証) も維持される。
- 負: deactivated なノードツリー (埋め込み View 含む) がプール滞在中メモリに残り、保持ノード経由の旧 content 参照も次の再利用または Composition 破棄まで残る (RecycledViewPool・itemViewCache・prefetch の保持分に比例した有界の増加)。
- 負: itemViewCache 滞在中は content が活性のままとなり、画面外でも effect・購読が継続する (旧実装は detach で即破棄していた)。
- 負: deactivate 時に `AndroidViewHolder.onDeactivate` (removeAllViewsInLayout) という従来走らなかった経路が Bridge 埋め込みに対して実行される。理論上は親側操作のみで安全だが、実機での高速フリック再検証を要する。
- 負: compose-runtime の reuse 実装詳細 (非 reusable ノードの強制置換等) への依存が増え、Compose メジャー更新時の再検証点が増える。
- 負: `scrollToPosition` 等の位置指定ジャンプ (全行再レイアウト) では、RecyclerView 内部の一時 detach 経路 (`detachViewFromParent` → `removeDetachedView`) が pooling container 外の解放と判定されるため Composition が破棄され、旧挙動 (再構築) へ縮退する。実装から制御できない内部経路であり、刻みスクロール・フリックでは本決定どおり成立し、ジャンプ時も従来より悪化しない (出典: 実装結果 — deviation.md、2026-08-16 オーナー合意)。
- 負: プール由来の再 bind では、再活性化が次の composition 適用で反映されるまで content の表示が最大 1 フレーム遅れる (measure policy が行の高さを確保するためレイアウト位置は保たれる。RecyclerView の prefetch により通常は表示前に bind が完了し、実機検証では 850 ジェスチャ中の単発フレーム空行 1 件として実測。持続はしない)。

出典: kasane/changes/archive/2026-08-16-perf-android-customcell-composition-reuse/exploration.md / 同 second-opinion-spec-001.md (Major 1, 2) / 同 verification-device.md (非活性 measure の FATAL 実測と修正後再検証) / 同 deviation.md (位置指定ジャンプ時の縮退) / 2026-08-15 オーナー承認 (ReusableContent 方式への転換) / 2026-08-16 オーナー承認 (measure policy と 1 フレーム遅延の追記) / kasane/decisions/core/0022-customcell-lifecycle-delegated-to-platform-adr.md (委譲元)

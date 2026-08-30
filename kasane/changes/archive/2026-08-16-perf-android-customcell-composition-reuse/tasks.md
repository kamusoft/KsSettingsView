# Tasks: perf-android-customcell-composition-reuse

## 1. 実装

- [x] 1.1 `ComposeCellViewHolder` の破棄戦略を `DisposeOnDetachedFromWindowOrReleasedFromPool` の明示指定へ変更し、KDoc を pool-aware 前提に改訂。該当箇所に `android/ADR-0015` コメントを残す (→ Requirement: CustomCell Composition のプール生存と破棄境界)
- [x] 1.2 `CustomCellViewHolder` を再構成する: setContent は生成時に一度だけ張り、`ReusableContentHost(active)` + `ReusableContent(cellId)` + パラメータの `mutableStateOf` 化 (content / showArrow / isEnabled / heightDp / isFixedHeight は composition 内から state を読む形にする。ラムダキャプチャでは更新が届かない)。cellId には Cell 同一性の安定 ID を用いる (→ Requirement: content ノードツリーの再利用 / content 状態の行間隔離)
- [x] 1.3 `bind` の View 側適用 (applyCellBackground / listener / isClickable / isEnabled / descendantFocusability / applyEffectiveHeight) は composition の外で従来どおり行う
- [x] 1.4 `reset()` を「active=false (deactivate) + content state の切り離し (ViewHolder 直接参照の切断) + View 側クリア」に変更し、`setContent {}` を廃止する。deactivate はプール投入時 (onViewRecycled) のみで、itemViewCache 滞在中は active を維持する (→ Requirement: reset による状態破棄と参照切断 / プール生存と破棄境界)
- [x] 1.5 KDoc・コメントの旧前提 (「Composition の破棄は DisposeOnDetachedFromWindow が担保」等) を新前提へ書き換える

## 2. テスト

テスト設計指針 (second-opinion spec-001 Major 3 / spec-002 Major 1, 4 反映): プール経路の検証は `setItemViewCacheSize(0)` で itemViewCache を除外し、cache 経路の検証は既定 (非ゼロ) 設定で行う / 再利用の検証は ViewHolder・ComposeView・View インスタンスの同一性 assert で行う / Composition の生存・破棄は `ComposeView.hasComposition` で観測する。

- [x] 2.1 新設: 実 RecyclerView スクロール経路で、プール滞在中の Composition 生存・プール clear 時の破棄・KsSettingsView の window detach 時の破棄を検証する (→ プール生存と破棄境界 Scenario 1, 3, 4)
- [x] 2.2 新設: 既定 itemViewCache 設定で、cache 経由の bind なし再表示において remember 状態が維持され DisposableEffect が dispose されていないことを検証する (→ プール生存と破棄境界 Scenario 2)
- [x] 2.3 新設: onReset 付き AndroidView プローブを含む同一ラップ関数 builder 間の再 bind で、View インスタンス同一・factory 非再実行・onReset 実行を検証する。異なる builder 間の再 bind でプローブ b のみが表示されプローブ a が表示ツリーに存在しないことも検証する (→ ノードツリーの再利用 Scenario 1-2)
- [x] 2.4 新設: 間に recomposition を挟まない同一 ViewHolder への A→B 直接 bind で remember 非持ち越し・DisposableEffect の dispose を検証する (→ 行間隔離 Scenario 1-2)
- [x] 2.5 新設: reset 後の非表示・listener 解除と、プール clear 後の builder GC 可能性 (WeakReference) を検証する (→ reset Scenario 1-2)
- [x] 2.6 新設 (Bridge 専用回帰): `ks-settingsview-bridge` に deactivate 経路を明示的に通す回帰テストを追加する — `setItemViewCacheSize(0)` でプール投入を保証し、旧 ViewHolder / ComposeView の同一性・`hasComposition == true`・platform view プローブの detach/attach 回数・再表示後の View 同一性・表示中他行の View 非取り外しを観測する (→ maui-bridge デルタ Scenario 1-2)
- [x] 2.7 検出力確認 (ミューテーション): (a) 戦略を `DisposeOnDetachedFromWindow` に戻す、(b) `ReusableContent` を `key` に置き換える、(c) cellId を固定値にする — のそれぞれで該当テストが失敗することを一時変更で確認し、結果を記録する
- [x] 2.8 既存 `CustomCellRenderingTest` (content 更新 / reset 残留なし / reset 後再 bind) の意味変化を確認し、必要な追随を行う
- [x] 2.9 既存 `KsBridgeCustomCellTest` を回帰ゲートとして実行する
- [x] 2.10 完了ゲート: `cd android && ./gradlew test --rerun-tasks` で全モジュール全件を実行し、debug/release の実行件数・失敗数を確認する (test-execution 規約)

## 3. 実機検証

- [x] 3.1 MAUI サンプルの CustomCell デモで高速フリック検証を再実施する (view-materialization.md の 2026-08-13 実証と同手順)。`AndroidViewHolder.onDeactivate` 新経路で空行・例外・view 取り合いが発生しないことを確認し、手順と結果の証跡を `kasane/changes/perf-android-customcell-composition-reuse/verification-device.md` に保存する

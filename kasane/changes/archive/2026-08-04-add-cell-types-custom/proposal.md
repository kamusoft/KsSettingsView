# Proposal: add-cell-types-custom

## Why

KsSettingsView には利用者が独自 UI のセルを得る手段として UserDefinedCell（自前 Cell 型 + Renderer + `register`。ADR-0013 で確立）しかなく、旧 AiForms.Maui.SettingsView の `CustomCell` に相当する「事前定義なしでその場に組み込める軽量カスタムセル」が存在しない。設定画面にプリセット外の UI（スライダー等）を1行だけ差し込みたい場合でも、ネイティブ Renderer の自作と登録を強いられるのは過剰である。

openspec 時代の同名 change は Registry の2系統登録 API を前提とした設計が現行コードと乖離し、CHANGES_REQUESTED のまま停止していた。探索（exploration.md）で現行アーキテクチャ（単一 `register`、`KsAnyView` の H/F 実運用実績、`ComposeCellViewHolder` 基盤）に合わせて全面再設計した。設計の根幹は core/ADR-0014（accepted）で確定済み。

## What Changes

対象能力: **cell-types-custom**（新規 capability）

1. **iOS `KsSettingsViewUI`**: `CustomCell`（content 値 + SwiftUI builder クロージャ）を追加。equality は content + 表示に効くスカラー（builder / onTap の関数値は除外。ADR-0014 改訂 2026-08-03）。Renderer は `UIHostingConfiguration` ベース（H/F の KsAnyView 描画と同パターン）
2. **Android `ks-settingsview-ui`**: `CustomCell` + `ComposeCellViewHolder` 派生の ViewHolder を追加（`DisposeOnDetachedFromWindow` 適用は基底が担保）
3. **挙動プロパティ**: `onTap`（既定 nil = 行タップ非対応。content 内要素がイベントを消費した場合は発火しない）/ `showArrow`（既定 false。true で CommandCell と同一の Disclosure Indicator を trailing accessory 領域に表示）/ `isEnabled`（false で行タップと content 内操作を抑止 — 視覚状態契約準拠。見た目の描き分けは利用者責務）/ `isVisible`（VisibilityAware 準拠）
4. **レイアウト**: 全面差し替え（accessory 領域を除く full-bleed）。中央スロット差し替えは提供しない
5. **DSL**: Android `DSLSectionScope.CustomCell(...)` 拡張関数 + 既存 CellHandle modifier チェーン対応、iOS は SectionBuilder（result builder)経路対応。content を持たない静的コンテンツ向けの省略糖衣も提供
6. **高さ**: self-sizing 全面委任（専用計測機構なし）。固定高さは既存の `.cellHeight()` 系 modifier に乗る
7. **Sample**: 両 Sample に CustomCell デモ画面を追加（既存デモと同じ SampleTheme を引き継ぐ）。内容: ①インライン利用 ②ラップ関数再利用（SliderCell 例）③動的高さ（展開/折りたたみ）④`showArrow` + `onTap`
8. **ユニットテスト**: bind 後の content 描画、recycle 後のリセット、equality（content のみ参加）、可視性フィルタ参加、DSL 経由配置

## Non-Goals

- **ネイティブ View レーン**（UIView / Android View を直接受ける口）— `UIViewRepresentable` / `AndroidView { }` の公式 interop で builder 内に埋め込めるため設けない（論点3a）
- **中央スロット差し替え・共通行レイアウト部品の public 開放** — 必要になったら別 change（論点2a）
- **AiForms の `LongCommand` / `IsMeasureOnce` / `KeepSelectedUntilBack` 相当** — 引き継がない（論点2c）
- **Section/Root H/F への任意 View** — `KsAnyView` で実装済み（スコープ境界は旧 proposal から踏襲）
- **MAUI 対応** — 将来の add-maui-* 群の責務。interop 経由で成立する見通しのみ探索で確認済み（論点5）
- **Registry の拡張** — CustomCell 専用登録 API は設けない（ADR-0014）
- **Sample 専用 Cell の置換** — 置換対象が現存しないため（論点4）

## Impact

- 破壊的変更なし（公開 API は追加のみ）
- ADR-0011（共通行レイアウト統一）との関係は core/ADR-0015（CustomCell を適用除外とする）で整理済み
- 旧 `openspec/changes/add-cell-types-custom/` は凍結規約に従い編集しない（本 change が実質的な置換であることを本 proposal が記録する）
- リスク: リサイクル × 宣言 UI ホスティングの本丸実装。ただし iOS は H/F での `UIHostingConfiguration` 実運用実績、Android は `ComposeCellViewHolder` 基盤があり、旧 proposal 時点の「リスク: 高」からは大幅に低減済み
- Android の動的高さ変化（RecyclerView 内 ComposeView の requestLayout 伝播）は機種依存のもたつきが理論上あり得るため、動的高さデモを受け入れ条件としてサンプル駆動で検証する

## UI アーティファクト

CustomCell 自体のライブラリ側見た目は accessory（矢印）のみで既存部品の再利用。mock は**プラットフォーム中立の共通デモ構成**（config の mock-variants=2 に従い 2 案作成し 1 案採用）とし、実装時は iOS / Android 各デモ画面を承認 mock の構成と照合する。SampleTheme 準拠は brief.md に明記する。

## 級: L

iOS / Android 2プラットフォームへの新規 capability 同時実装 + DSL + Sample + テストの横断規模のため（2026-08-03 ユーザー確定）。

domain: core

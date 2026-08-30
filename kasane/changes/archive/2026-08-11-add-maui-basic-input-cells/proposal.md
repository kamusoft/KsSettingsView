# Proposal: add-maui-basic-input-cells

## Why

MAUI facade (KsSettingsView.Maui) は現在 LabelCell 1種のみで、設定画面ライブラリとして実用に足りない。Native (iOS / Android) には基本7種 + 入力5種 + Custom の全 Cell が実装済みであり、MAUI 層の配線 (facade 公開面・Bridge 輸送・操作通知) だけが欠けている。phase-4 の議論 (全12論点、2026-08-10 解消) で設計判断は出揃った。

## What Changes

- **maui-cells** (facade 公開面): CellBase 派生11種 (Command / Button / Switch / Checkbox / Radio / SimpleCheck / Entry / Picker / NumberPicker / TimePicker / DatePicker) を AiForms 互換命名 (maui/ADR-0008) で追加。双方向バインド10プロパティ (正規一覧は design Decision 3。delegate 通知 → FindCell → SetValue → 必須コミット、入口同値チェック — maui/ADR-0012)。`DatePickerUIStyle { Calendar, Wheels }` 統一 enum (maui/ADR-0013)。Theme 系 BindableProperty + Cell の CellStyle / accentColor 公開。`Section.IsVisible` 公開 (ReplaceSection 単発配信)。`DataTemplateSelector` の SelectTemplate 解決。`ShouldPublish()` フック撤去
- **maui-bridge** (interop 拡張): per-type Bridge DTO 11種 × 両OS (maui/ADR-0011)。単一 interaction delegate/listener (maui/ADR-0003) の新設 — Native → C# のユーザー操作通知チャネル。`KsBridgeSection.isVisible` 輸送。壁時計値の ISO-8601 文字列輸送・Picker index 輸送 (maui/ADR-0012)
- **samples-maui**: BasicCells / InputCells / UnifyCellCommonFields / Visibility の4デモページを iOS/Android サンプルと完全一致で追加 (sample-parity)。「LabelCell 検証」ページを削除

## Non-Goals

- CustomCell (phase-5)・Header/Footer 任意 View (phase-6)・D&D 等の強化 (phase-7〜10)
- Store/DSL 方式デモの MAUI 対応 (sample-parity 規約へ例外条項を追加して正当化 — 蒸留時に concepts 改訂)
- 視覚スナップショットテストの導入 (fake gateway テスト + サンプル目視で賄う)
- ui/ アーティファクト (brief / mock 承認ゲート) — 見た目の正は既存 native 実装と sample-parity であり新規視覚設計が存在しないため省略 (オーナー承認済み。add-maui-core / add-maui-samples-foundation と同じ前例)
- 旧 AiForms からの移行ガイド (NuGet パッケージング着手時に docs-refresh 経由で作成)
- Native (iOS/Android) の Cell 実装・選択面への機能追加 (既存実装をそのまま輸送する。KsBridgeSection.isVisible など Bridge 層のフィールド追加はこの限りではない)

## Impact

- facade の公開 API と Native (Core / UI) の公開 API は additive / 不変。**Bridge interop のシグネチャは共通基底 DTO 型化により source-breaking** (`KsBridgeSection.cells` 等が `KsBridgeLabelCell` → 基底 `KsBridgeCell` に変わる) だが、Bridge / Binding assembly は未配布 (NuGet 非ゴール) かつ facade 経由のみが公開契約 (Binding 型の直接使用は禁止事項) のため、利用者影響なしと評価して許容する
- 影響範囲: maui/ (facade + Tests)、ios/Sources/KsSettingsViewBridge、android/ks-settingsview-bridge、samples/maui
- リスク: (1) delegate/listener の interop (コールバック方向) は本プロジェクト初の経路 — binding 層 (Native Library Interop) でのコールバック表現の検証が必要 (2) ReplaceSection による cellId 再採番と cellId Map の整合 (設計で確認) (3) 11種一括のボイラープレート量 — 機械的だが変換テストで各種を個別に固定する

## 級: L

複数能力横断 (facade / Bridge 両OS / interop 新設)、公開 API の大幅追加、per-type DTO 展開は覆すコスト高。

domain: maui
roadmap: maui-support/phase-4-basic-input-cells

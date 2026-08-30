# Proposal: add-maui-custom-cell

## Why

MAUI 対応の Cell として最後に残った CustomCell を提供する。プリセット外の任意 UI を C# / XAML だけで1行に差し込む需要は原典 AiForms でも主要ユースケースであり、これで全 13 Cell (基本7 + 入力5 + Custom1) が MAUI から揃う。旧 openspec 案の「`ToPlatform()` した view を `KsAnyView` 経由で content に格納」は、現行のカスタムセル3層契約 (content 等価性による再バインド制御) と衝突するため、フェーズ議論で全面再設計した (maui/ADR-0019〜0021)。

## What Changes

- **maui-cells**: `CustomCell : CellBase` を追加。`[ContentProperty]` の `Content : View?`、`Command` / `CommandParameter` / `Tapped`、`ShowArrowIndicator` (既定 false)。継承で露出する不適用プロパティ (Title / Description / IconSource / テキスト系 style) は silent no-op。内容変化は binding による live 更新 (再発行なし)、`Content` 差し替えのみ明示再発行
- **maui-core**: accessory View で建てた実体化機構 (materializer seam / 自己計測 wrapper / platform lease) を Cell content に拡張する。controller が cell 単位の lease を所有し、native content へ格納する世代トークンを発行する。退役順序 (Store 更新 → native 配信 → 破棄) と Handler 再接続時の復元は accessory と同じ規律
- **maui-bridge**: `KsBridgeCustomCell` DTO (platform view + content トークン + showArrowIndicator) を per-type 展開 (maui/ADR-0011) に追加。native 側では既存の「定数返し closure + detach」パターン (maui/ADR-0017) で native CustomCell の builder に埋め込む。行タップ通知を単一 interaction delegate (maui/ADR-0003) へ追加
- **samples-maui**: ①パリティ画面 `CustomCellDemoPage` — native (iOS / Android) の既存 CustomCellDemo と同一の画面構成・文言で追随する (sample-parity 規約が本フェーズでの追随を予定として明記)。②MAUI 固有デモ — MAUI 固有の意味論 (ItemTemplate 生成 / Handler 切断復元 / 派生サブクラス / Content 差し替え) は「MAUI 固有」区分の別画面として提供する (AccessoryViewsDemoPage のオーナー裁定前例と同型)

## Non-Goals

- UserDefinedCell (カスタムセル3層の③) と Registry の MAUI 公開 (maui/ADR-0019)
- `ContentTemplate` プロパティ・content 値駆動の template 再実体化 (DataTemplate 仮想化とセットで後続フェーズにて再考)
- 行ビューのリサイクルに伴う View の載せ替え (仮想化) — 生成された行の数だけ live View が存在する
- Native (iOS / Android) Core・UI の公開 API 変更 — 原則 bridge モジュール内で完結させる。ただし高さ追従 probe (design Decision 5) の結果、cell 対象の再計測口が必要と判明した場合は、accessory の `invalidateAccessoryMeasurement` と同型の一過性通知の追加を許容する (契約対称化のための追加であり、ロードマップ非ゴールの例外条項に該当)

## Impact

- 破壊的変更なし (公開面の追加のみ)。既存 Cell・accessory の挙動に影響しない
- 主リスク: cell content の platform view は accessory と違い**行のリサイクル**と交差する。native 側の再利用規律 (`prepareForReuse()` / `reset()` で埋め込み View を残さない) と「定数返し closure が返す前に detach する」既存手当の整合が実装の中心的な検証点
- AiForms の `IsSelectable` / `IsMeasureOnce` / `UseFullSize` / `LongCommand` は非提供のため、移行者向けに差分の明示が必要 (docs は別途 docs-refresh)

## 級: L

複数能力横断 (maui-cells / maui-core / maui-bridge / samples-maui) で、facade〜Bridge〜native の3層に波及する輸送・再バインド設計を含むため。

domain: maui
roadmap: maui-support/phase-5-custom-cell

# Proposal: align-sample-parity

## Why

cross/ADR-0016 で Sample を「プラットフォーム間パリティの検証装置」と位置づけ、規約 (cross/conventions/sample-parity.md) を制定した。しかし現状の samples/ios と samples/android は文言・構成の差異が多数あり (棚卸し結果は exploration.md)、検証装置として機能していない。iOS を正として両 platform を規約適合状態に収束させる。

## What Changes

**Phase 1: iOS を正に整える (samples-ios)**

1. 入力 Cell 5 種デモのスタイルを基本 Cell 7 種デモに統一 — 現在値プレビュー廃止 → 1行イベント表示 (`最後のイベント: ...`)、MAUI 互換 Theme 適用、「ニックネーム (callback)」セルは Section footer で経路の意図を明記して存続。「予約日」は「誕生日」との状態共有を解消して独立させる
2. 全7画面 (Minimal Diffable 検証を含む) の画面タイトルをルートメニュー文言と完全一致させる (Store / DSL はタイトル新設、他5画面は文言修正)
3. Android 先行の RadioCell「ダーク」hintText「推奨」と Section header 文言を iOS に追随
4. StoreDemoView を ContentView.swift から分離し1画面1ファイル化
5. DSL デモの動的 Section 見出しを API 名非依存の「動的 Section（繰り返し）」に変更 (ForEach/forEach ゆれの根本解消)。同様に入力 Cell 5 種デモの DatePicker Section の見出し・footer も形式中立の文言に変更 (design.md Decision 1)
6. ルートメニューを List の Section 分けにし、Minimal Diffable 検証をデモ群と別の「検証」Section に隔離 (規約の「デモ画面の集合には数えない」をメニュー構造で明示)

**Phase 2: Android を iOS に同期 (samples-android)**

7. 入力 Cell 5 種デモを iOS 構成に全面一致 (7 Section・Cell 数・全文言・全パラメータ・初期値。rootHeader は削除)。Store 方式デモの表示文言 (Sample Row 等) も iOS に一致させる
8. MainActivity.kt に同居する画面を1画面1ファイルに分割
9. ルートメニューを iOS と同じリスト形式に変更 (Button 列を廃止。検証 Section は iOS 固有画面のみのため Android には設けない)
10. DSL デモの見出しを 5. と同文言に変更
11. DatePickerCell の uiStyle 対応を調査し、iOS (.wheels / .calendar) と対応する表現に揃える。本体 API 差で一致不可能な場合は deviation.md に記録し本体側の統一課題とする (規約の定める手順)

影響する能力: samples-ios / samples-android

## Non-Goals

- 本体ライブラリ (core / ios / android の公開 API・挙動) の変更
- samples/maui の新設 (未実装のまま。規約上、着手時に一致させる)
- MinimalDiffableDemoView の Android 移植 (規約の例外枠)
- docs/・README の更新 (docs-refresh の責務)

## Impact

- 破壊的変更なし (Sample の表示文字列・デモデータは製品契約ではない — 規約に明記)
- 影響は samples/ 配下のみ。リスクは DatePickerCell の uiStyle・todayText が本体 API の platform 差で揃わない可能性 (deviation 記録で吸収)
- Phase 1 完了〜Phase 2 完了の間は片側先行状態になるが、同一 change の tasks.md で追跡するため規約上許容

## 級: L

samples-ios / samples-android の2能力横断のため ksn-core 基準で L (当初 M と判定していたが second-opinion-001 の指摘で昇格)。実態は samples のみ・API 変更なし・可逆で、design.md は判断4点 (DatePicker 対応・文字列一元管理・Theme 共有・検証方式) の簡潔な記録に留める。

domain: cross

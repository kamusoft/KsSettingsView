---
id: 0002
title: Bridge 公開 API は Store 公開操作と 1:1 のメソッド群とし、replaceCells は iOS Store へ追加して対称化する
status: accepted
date: 2026-08-04
---

## Context

maui-support / phase-1-native-bridge の議論。原案の旧 openspec `add-maui-bridge` は `KsSettingsRootDiffDTO` (SettingsRootDiff 全10ケースの union 型 DTO) を `applyDiff` 1メソッドへ渡す設計だった。

[ADR-0001](0001-maui-bridge-dsl-variant-internal-store.md) で Bridge の仕事は「Store 公開操作への変換」と決まったため、union DTO を挟むと「C# で union を組む → interop 境界で union を表現する → Native で decode して Store 操作へ振り分ける」の3段変換になる。`@objc` は Swift enum の associated value を表現できず、union の interop 表現は不格好になる。

また現行 Store 契約 (`store-and-update-streams.md`) では、複数 Cell の内容更新バッチ `replaceCells` は Android のみが持ち、iOS は個々の `replaceCell` を呼ぶ非対称な契約だった (iOS `SettingsRootStore` の公開操作は replaceAll / Section 4種 / Cell 4種 / updateAccessory / applyTheme の11個で、バッチなし)。

## Decision

- Bridge の公開 API は union DTO をやめ、**Store 公開操作と 1:1 のメソッド群**にする: `setRoot` (=replaceAll) / `insertSection` / `removeSection` / `moveSection` / `replaceSection` / `insertCell` / `removeCell` / `moveCell` / `replaceCell` / `updateAccessory` / `replaceCells` (バッチ) / `setTheme` の12本。
- `replaceCells` は **iOS の `SettingsRootStore` 本体へ公開操作として追加**し、Android と契約を対称化する。追加的な公開 API であり破壊的変更ではない。Bridge 内部で個々の `replaceCell` をループする誤魔化しはしない。

## Alternatives Considered

- **union DTO + applyDiff 1本 (旧案 + replaceCells ケース追加)**: interop 境界での union 表現が不格好で、decode → 振り分けの変換層が必要になるため却下。
- **replaceCells を Bridge 公開 API にのみ置き、iOS 側は Bridge 内部で replaceCell をループ**: ループで誤魔化す意味がなく、スマートに実現できるなら Native (iOS Store) へ追加すべきというオーナー判断で却下。

## Consequences

- 正: 変換層が最薄になり (ほぼ素通し)、ADR-0001 の位置づけと整合する。型も interop 境界をそのまま通る。
- 正: iOS Store の `replaceCells` 対称化は MAUI 以外の iOS 利用者 (SwiftUI Bridge・直接 Store 利用) にも恩恵がある。
- 負: メソッド本数は増える (1本 → 12本) が、各メソッドは単純。
- 負: iOS Store への `replaceCells` 追加実装とテストが phase-1 のスコープに入る (core 契約 `store-and-update-streams.md` の追随更新が蒸留時に必要)。

---
出典: 2026-08-04 ksn-agenda (maui-support / phase-1-native-bridge) での議論 (B案採用と iOS Store への追加はオーナー判断)

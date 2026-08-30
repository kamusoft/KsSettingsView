---
id: 0003
title: Native → C# のユーザー操作通知は単一 delegate/listener に集約する
status: accepted
date: 2026-08-04
---

## Context

maui-support / phase-1-native-bridge の議論。Native 側で起きるユーザー操作 (Switch 切替・Command タップ・Entry 入力等) を C# へ通知する interop 経路の設計。現行契約 (`declarative-ui-bridge.md` の「状態の橋渡し」) では、ユーザー操作は Cell の callback から宣言 UI 側の state へ戻すと定めており、Bridge 内部で Native Cell を組み立てる際に callback を仕込む形になる。

interop 境界を越える callback は、寿命管理する GC ハンドルの数がそのままコストとリークリスクになる。phase-2 では `WeakReference` リークテストを基盤化する予定がある。

## Decision

- ユーザー操作通知は単一の `KsCellInteractionDelegate` (iOS) / `Listener` (Android) に集約する (旧 add-maui-bridge 案の踏襲)。Cell 種別はメソッド名 (`onSwitchChanged(cellId, isOn)` 等) で識別する。
- C# 側は SettingsView あたり1個の delegate 実装を保持し、`cellId → CellBase` の Dictionary (phase-2 で整備) で該当 BindableObject へ配送する。
- Cell 種別の追加時は delegate へメソッドを additive に追加する (各 Cell フェーズが Bridge API `addXxxCell` を足すのと同じリズム)。

## Alternatives Considered

- **Cell 単位の callback 登録**: 種別追加時に interface が伸びない利点はあるが、Cell 数ぶんの interop ハンドルの生成・解放を追跡する必要があり、リークテストの検証対象が爆発するため却下。
- **単一メソッド + シリアライズ済みイベント (payload encode/decode)**: ハンドルは1個で済むが、メソッドシグネチャによる型安全性を失うため却下。

## Consequences

- 正: interop ハンドルが SettingsView あたり1個になり、寿命が SettingsView と一致してリーク管理が単純。
- 正: メソッドシグネチャで型が付き、payload の encode/decode が不要。
- 負: Cell 種別を追加するたびに delegate インターフェースが伸びる (additive 変更)。

---
出典: 2026-08-04 ksn-agenda (maui-support / phase-1-native-bridge) での議論 (A案採用はオーナー判断)

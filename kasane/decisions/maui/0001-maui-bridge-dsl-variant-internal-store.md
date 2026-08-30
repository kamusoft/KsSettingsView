---
id: 0001
title: MAUI Bridge は内部所有 Store を持つ DSL 方式の類型として位置づける
status: accepted
date: 2026-08-04
---

## Context

MAUI 対応ロードマップ (maui-support) phase-1-native-bridge の議論。原案である旧 openspec `add-maui-bridge` (凍結) は、Native Bridge が `SettingsRootStore` を介さず直接 `controller.applyDiff` を呼ぶ設計だった。

一方、現行のコア契約 `concepts/core/architecture/declarative-ui-bridge.md` は宣言 UI Bridge の利用方式を DSL 方式 (Bridge 内部所有 Store) と Store 方式 (利用者所有 Store) の二分法で定義し、「両方式は別の描画基盤を持たず、`SettingsRootStore → Native Host` の同じ更新経路へ収束する」ことを保証事項としている。旧案のままでは、この収束保証を迂回する第三の経路を新設することになる。

また現行 Store 契約 (`store-and-update-streams.md`) は複数 Cell 内容更新のバッチ (`replaceCells`)、Theme の構造 Diff 不発行・同値スキップ、購読開始前の状態復元を Store の責務として定めており、Store を迂回するとこれらの保証を Bridge 側で再実装する必要が生じる。

MAUI の構図は「宣言状態 (C# の BindableObject ツリー) を利用者側フレームワークが所有し、Bridge が差分を Native へ流す」形であり、SwiftUI / Compose の DSL 方式と同型である。

## Decision

- MAUI Bridge は Native 側に**内部所有の `SettingsRootStore`** を持ち、現行二分法における **DSL 方式の類型**として位置づける。
- Bridge の公開 API (`setRoot` / `applyDiff` / `replaceCells` / `setTheme` 等) は、Bridge 内部で Store の公開操作に変換する。Native Host は Store の通知を購読する (既存経路と同一)。
- 状態の所有者は C# 側 (MAUI の BindableObject ツリー)、Store は Bridge 内部所有。C# 側の Bridge API の見た目は旧案とほぼ同じ形を維持できる。

## Alternatives Considered

- **旧案のまま直接 `controller.applyDiff` を呼ぶ (Store 迂回)**: 実装コスト最小に見えるが、収束保証の例外 (第三経路) を concepts に明記する必要が生じ、replaceCells バッチ・Theme 分離・状態復元などの保証を Bridge 側で再実装することになり実質 Store の再発明になるため却下。
- **Native Store の handle を C# に公開する (Store 方式)**: 現行契約との整合はするが、Store の全公開操作 + 購読の binding が必要で API 表面積と実装コストが最大。大量データ・高頻度更新ユースケースが MAUI で必要になった時の将来拡張として保留。

## Consequences

- 正: `SettingsRootStore → Native Host` の収束保証を維持でき、concepts への追記は「MAUI は DSL 方式の類型」程度で済む。
- 正: replaceCells バッチ・Theme の同値スキップと Diff 不発行・購読開始前の状態復元が Store から無償で得られる。
- 正: 将来 Store 方式 (handle 公開) を追加拡張として後付けできる。
- 負: Bridge 内部に「Bridge API → Store 公開操作」の変換層を持つ (薄い層だが旧案には無かった実装)。
- 負: MAUI 利用者は当面、利用者所有 Store による命令型の部分操作を使えない。

---
出典: 2026-08-04 ksn-agenda (maui-support / phase-1-native-bridge) での議論 (A案採用はオーナー判断)

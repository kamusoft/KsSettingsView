---
id: 0004
title: MAUI 公開 API の Theme / CellStyle は MAUI 慣例型で公開し、interop DTO は非公開の輸送表現とする
status: accepted
date: 2026-08-04
---

## Context

maui-support / phase-1-native-bridge の議論。現行 styling 契約 (`concepts/core/styling/style-resolution.md`) は「共通化のための `KsColor` / `KsFont` のような中間表現を置かず、各 platform の型と慣例を利用者へ直接公開する」と定め、論理色・論理 font の中間型追加を禁じている (iOS は `UIColor`/`UIFont`、Android は Compose `Color`/`TextStyle` を直接公開)。

MAUI は単一の C# API で iOS / Android 両方を相手にするため、Theme / CellStyle を C# でどんな型で公開するかを決める必要がある。なお `setTheme` 自体は [ADR-0002](0002-bridge-api-per-store-operation.md) の12メソッドに含まれ、Store の `applyTheme` へ素通しすることで「Theme を Diff に混ぜない・同値スキップ」は Store の保証で満たされる。

## Decision

- MAUI の公開 Theme / CellStyle は **MAUI 慣例の型** (`Microsoft.Maui.Graphics.Color`、MAUI の Font 表現等) で定義する。XAML から自然に書けることを優先する。
- interop 境界では ARGB int・フォント記述子などのプリミティブへ marshalling する。この DTO は**非公開の輸送表現**であり、styling 契約が禁じる「公開 API の中間型」には該当しない。
- 「KsColor 禁止」規則の趣旨は「利用者に独自中間型を押し付けず各 platform の慣例に乗る」ことであり、MAUI platform の慣例 = MAUI 型の直接公開はこの趣旨の MAUI への適用である。
- platform 固有の Theme / CellStyle 項目 (例: Android 専用色) は、C# では platform 接頭辞付きの nullable プロパティとして持たせ、対象外 OS では無視する (入力 Cell 契約の「iOS/Android 固有引数を共通引数として扱わない」方針の MAUI 表現)。項目対応表の詳細は spec 化の責務。

## Alternatives Considered

- **共通論理型 (KsColor 的な中間型) を C# 公開 API に導入**: styling 契約の「中間型を追加しない」に正面から抵触し、利用者に独自型の学習を強いるため却下。
- **platform 別 Theme を条件コンパイル (`#if`) で公開**: 契約の文言には忠実だが、MAUI 利用者の API が `#if` 分岐だらけになり XAML から書けなくなるため却下。

## Consequences

- 正: MAUI 利用者は XAML / C# で慣例どおりに Theme / CellStyle を書ける。
- 正: iOS / Android の公開契約 (platform 型の直接公開) は変更不要。
- 負: interop 境界の marshalling 実装 (色・フォント・寸法の変換) が Bridge の責務に加わる。
- 負: platform 固有項目の対応表を spec で維持する必要がある。

---
出典: 2026-08-04 ksn-agenda (maui-support / phase-1-native-bridge) での議論 (A案採用はオーナー判断)

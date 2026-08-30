---
id: 0002
title: CustomCell の hosting 階層はリサイクル毎の再生成を維持する
status: accepted
date: 2026-08-16
---

## Context

Android 側 `perf-android-customcell-composition-reuse` (ReusableContent 方式で CustomCell の Composition をリサイクル) の2段構え第2弾として、iOS でも「CustomCell の中身のリサイクル」を検討した。iOS の現行実装は `CustomCellView.prepareForReuse` の `contentConfiguration = nil` により、リサイクル毎に SwiftUI hosting 階層を全破棄・全再構築している。

事前合意 (2026-08-15、Android 側 exploration): iOS は計測スパイクで効果を確定してから採否判断し、効果が小さければ「やらない」も正解とする。`UIHostingConfiguration` の in-place 更新は公式仕様からの推論止まりで、効果の定量根拠が公開情報に存在しなかった。

計測スパイク (2026-08-16、使い捨て計装): CustomCell 200 行のストレス画面を CADisplayLink 等速フリック (2500pt/s × 4 パス往復、Release ビルド) で駆動し、bind 数 / contentView 再生成数 / フレーム間隔 / プロセス CPU 時間を実測。対象は iPhone 15 (iOS 26.6) と iPhone 11 (iOS 16.6.1)。

結果:

| 条件 | 再生成数 (bind 約 735 中) | フレーム | CPU 時間 |
|---|---:|---|---|
| A: 現行 (nil 化あり) | bind 毎 (735) | 両端末 60fps 張り付き・hitch 0 | 基準 |
| B: nil 化撤廃 | プール成長分のみ (6〜7) | 同一 | 差はノイズ範囲 (±2%・方向不定) |
| C: B + `.id(cell.id)` | プール成長分のみ (6〜7) | 同一 | 同上 |

- 機構: nil 化撤廃だけで同型 `UIHostingConfiguration` 再代入の in-place 更新が成立する (iOS 16.6.1 / 26.6 の両方で確認。「iOS 18 系で in-place 前提が崩れる」という事前報告は非該当だった)。`.id(cell.id)` を付けても content view インスタンスの再利用は壊れない
- 効果: A13 (iPhone 11) ですらベースラインが 60fps 張り付きで、CPU 時間にも有意差なし。hosting 階層の再生成コストは、bind 毎にどのみち発生する SwiftUI content 更新コストに埋もれる規模しかない (Apple の「configuration は軽量」という言及と整合)

## Decision

`CustomCellView.prepareForReuse` の `contentConfiguration = nil` を維持し、CustomCell の hosting 階層はリサイクル毎の再生成を許容する。iOS では「中身のリサイクル」最適化 (Android の ReusableContent 方式に相当する対応) を行わない。

再検討の条件: 実利用形態で CustomCell 起因のフレーム落ち・CPU 消費が実測で観測された場合のみ、本 ADR を supersede して再計測から始める。

## Alternatives Considered

- **nil 化撤廃 + `.id(cell.id)` (スパイク変種 C)**: 機構の成立は実測で確認済みだが効果がゼロ。一方で採用すると (1) プール滞在中の hosting 階層のメモリ、(2) `.id` の引き回し (`.id` なしの変種 B では SwiftUI @State が行を跨いで持ち越されるリスクがあり C 形が必須)、(3) MAUI Bridge の退役順序レース (実機フリック再現もの) の再検証義務を負う。対価ゼロでコストのみのため不採用。
- **自前 UIHostingController 保持**: Apple が「UIHostingConfiguration が cell 内 SwiftUI の唯一の公式サポート経路」と明言しており非サポート。Android 側 exploration (2026-08-15) で却下済み。

## Consequences

- 正: 現行の再利用時保証 (前 content への参照切断・SwiftUI 購読の解放) が最も単純な形 (`nil` 代入) のまま維持される
- 正: Android (Composition 再構築が重い → ReusableContent 方式) と iOS (hosting 再生成が実測で十分軽い → 何もしない) の非対称が実測に基づく判断として記録され、対称性のためだけの無益な移植を防ぐ
- 負: CustomCell の bind 毎に hosting 階層の再生成コストは残り続ける (実測では両端末 60fps・CPU ノイズ範囲に収まることを確認済み)
- 負: 将来 SwiftUI の実装や利用実態 (極端に重い content 等) が変わって再生成コストが顕在化した場合、supersede と再計測が必要になる

出典: kasane/changes/archive/2026-08-16-perf-ios-customcell-configuration-reuse/exploration.md (計測スパイク全記録) / kasane/changes/archive/2026-08-16-perf-android-customcell-composition-reuse/exploration.md (iOS 節・2段構えの合意)

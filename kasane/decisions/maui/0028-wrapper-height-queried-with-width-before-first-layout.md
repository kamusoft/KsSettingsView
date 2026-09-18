---
id: 0028
title: 配置された View の行・領域の高さは初回から幅付きで wrapper に問い、intrinsic の無効化は内容変化の追従にだけ使う
status: accepted
date: 2026-09-18
amends: 0020
---

## Context

iOS で CustomCell の行の高さと Section の Footer の領域の高さは、MAUI 側の自己計測 wrapper (`KsAccessoryHostView`) が答える必要サイズ (`IntrinsicContentSize`) で決まる (maui/ADR-0016)。wrapper は幅がまだ渡っていない間 (`Bounds.Width` が 0) は無限幅で MAUI View を測るため、折り返す Label は 1 行ぶんの高さを答える。その後の配置で実幅が渡ると wrapper は必要サイズを無効化し、行の高さが測り直される。

この 2 段階が iOS 実機 (iPhone 11) で目に見える形になった: 内容は最初のフレームから折り返し後の高さで描かれているのに、行の高さだけが約 300ms かけて伸び、後続の Section が下へずれる。CustomCell の行 (MAUI Sample の折り返し Label を持つ動的高さ Section) と Section の Footer (view accessory) の両方で観測され、iOS Simulator (iPhone 17 / iOS 26.1) では観測されない。

裏取りで判明した外部の性質:

- UIKit は iOS 16 以降、表示中の cell の必要サイズ変化を `selfSizingInvalidation` の既定値で自動的に行の高さへ反映し、その変化は既定でアニメーションされる (WWDC22 Session 10068)。伸びる動きは UIKit の既定挙動が素直に働いた結果である
- MAUI 本体の `MauiView.SizeThatFits(width, height)` は幅・高さの制約ペアを `CrossPlatformMeasure` へ素通しし、結果を制約ペアで 1 段キャッシュする (`IsMeasureValid` / `_lastMeasuredSize`)
- そのキャッシュは `InvalidateMeasure` でのみ消え、`InvalidateIntrinsicContentSize()` / `SetNeedsLayout()` では消えない。`IntrinsicContentSize` の override は MAUI 本体には存在しない
- CustomCell の内容を SwiftUI へ埋め込む representable (`KsBridgeCellContentView.sizeThatFits`) は、幅の提案を受け取っているにもかかわらず幅を使わず `intrinsicContentSize` の高さを中継している

前提:

- UIKit の self-sizing 経路 (`UICollectionViewListCell` + `UIHostingConfiguration`) で、表示中の行の高さ変化がアニメーションされる既定が続いている
- SwiftUI は CustomCell の内容の初回計測から有限幅を提案する (実機・Simulator の probe で確認: 幅付きの問い合わせが intrinsic より先に来る)
- MauiView の計測キャッシュが制約ペア単位である
- view accessory の wrapper は取り付け直後から superview (`contentView`) が領域の幅を持ち、水平の inset は無い

## Decision

maui/ADR-0020 の決定のうち「行の高さは wrapper の intrinsic の答えとその無効化にだけ委ねる」という範囲を本決定で置き換える。「内容の変化は wrapper 自身の計測無効化だけで行の高さが追従し、native への一過性の再計測通知は要らない」という決定は維持する。

配置された View の行・領域の高さは、**幅が分かっている側が幅付きで wrapper に問う**。

- CustomCell では Bridge の representable が提案された幅で `sizeThatFits(幅, 上限なし)` を呼び、wrapper の `SizeThatFits` (MAUI 本体の `CrossPlatformMeasure` 経路) で初回から折り返し後の高さを得る
- 上限なしは有限の最大値で渡し、wrapper 側が高さ制約を無限に正規化して幅だけを制約にする (有限の巨大高と無限は MAUI の計測で同じではない)
- Section / Root の view accessory は Auto Layout 経路で幅付きの問い合わせができないため、wrapper が自分の幅が 0 のとき superview の幅で測る。Native (Swift) 側には幅を渡す口を足さない
- wrapper の `IntrinsicContentSize` と `MeasureInvalidated` → `InvalidateIntrinsicContentSize()` の中継 (maui/ADR-0016) は残し、**内容の変化の追従にだけ使う**。幅付きの経路と intrinsic の経路は同じ幅に対して同じ高さを答える
- wrapper は `MeasureInvalidated` を受けたら intrinsic の無効化と併せて MauiView の計測キャッシュを捨てる (`InvalidateConstraintsCache`。祖先へ伝播する `InvalidateMeasure` は外向きの通知を `measureInvalidated` コールバックが担うため採らない)
- 補正のアニメーションを止める手当 (`UIView.performWithoutAnimation`) は本決定には含めない。初回表示では幅付きの計測で補正自体が起きなくなるため要らず、後からの幅変化 (回転など) への保険としての採否は未決のまま残す

## Alternatives Considered

**補正のアニメーションを止める (`UIView.performWithoutAnimation` で無効化を包む) だけにする**: 単独案としては却下。幅付き計測の保険として後から足す余地は残す。却下理由:

- 症状の芯 (初回の高さが幅なしで決まっている) を残したまま見え方だけ変えるため、後続 Section のずれが「伸びる」から「一瞬で飛ぶ」に変わるだけで残る
- 発生源が collection view のバッチ更新側だと効かないという報告があり、効くかは実機でしか判定できない
- 展開操作など正当な後続の高さ変化のアニメーションまで消すと退行する

## Consequences

- 正: 折り返す内容を持つ CustomCell の行と view accessory の領域が、最初のフレームから定常状態の高さで作られる。補正が起きないので UIKit のアニメーションも起きず、後続 Section のずれが消える
- 正: native への再計測通知 (maui/ADR-0018 の `invalidateAccessoryMeasurement` 相当) を行に増やさない。maui/ADR-0020 の「通知不要」は保たれる
- 負: wrapper の高さの答えが intrinsic と `SizeThatFits` の 2 経路になる。両者が同じ幅で同じ高さを返す一貫性と、`MeasureInvalidated` でキャッシュを捨てる責務を wrapper が負う。怠ると幅付き経路が古い高さを返す
- 負: Bridge の representable が wrapper の `SizeThatFits` の意味論 (MAUI 本体の制約ペア計測) に依存する。iOS Bridge と MAUI facade の境界をまたぐ約束事になり、MAUI 本体の計測契約の変更に追随する必要がある
- 負: CustomCell (幅付きの問い合わせ) と view accessory (superview の幅) で初回計測の経路が非対称になる。superview も無い最初の 1 回 (取り付け前) は無限幅で答えるしかなく、行の高さを決めるのはその後の幅付きの問い合わせである
- 負: 幅付きの問い合わせ (幅, 無限) と配置の計測 (幅, 実高さ) で計測キャッシュの鍵が入れ替わるため、レイアウトパスごとに内容の計測が 1 回増える
- 負: 後からの幅変化 (回転など) の補正は従来どおり無効化経由で、UIKit の既定アニメーションが残る

## Revisit When

- MAUI 本体の `MauiView` が `IntrinsicContentSize` を持つようになるか、計測キャッシュの単位が変わったとき
- UIKit が self-sizing の高さ変化を既定でアニメーションしなくなったとき (幅付き計測の必要性は残るが、症状の重さが変わる)
- SwiftUI が CustomCell の内容の初回計測で有限幅を提案しなくなったとき (本決定の CustomCell 側の手段が成り立たない)
- accessory の取り付けに水平 inset が入り、superview の幅と内容の幅が一致しなくなったとき

---
出典: kasane/changes/archive/2026-09-18-ios-customcell-initial-height-grow-animation/exploration.md (見立て・案の比較・決定事項) / 同 evidence/README.md (probe 1a・1b と修正後の実機 A/B) / 同 review-001.md (キャッシュ破棄の選択と高さ正規化の裏取り) / kasane/changes/archive/2026-09-18-android-accessory-view-late-insert-animation/evidence/README.md (1.3 と 6.3-e の iOS 実機観測) / kasane/roadmaps/maui-support/phases/phase-5-custom-cell/artifacts/probe/2026-08-12-cell-content-size-follow.md (MauiView 計測キャッシュのリスク指摘)
関連: maui/ADR-0016 (wrapper の IntrinsicContentSize override と MeasureInvalidated 中継。本決定は残す) / maui/ADR-0018 (accessory の一過性再計測通知。行には足さない)

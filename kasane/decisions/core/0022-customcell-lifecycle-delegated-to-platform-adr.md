---
id: 0022
title: CustomCell の適用除外を再定義し、Android の宣言 UI lifecycle 機構を platform ADR へ委譲する
status: accepted
date: 2026-08-15
supersedes: 0015
---

## Context

core/ADR-0015 は CustomCell (full-bleed 宣言 UI ホスティング型 Cell) を ADR-0011 の共通行レイアウト統一の適用除外とし、あわせて lifecycle 管理の具体機構を core 層で名指しした (Android: `ComposeCellViewHolder` 経由の `DisposeOnDetachedFromWindow`、iOS: `UIHostingConfiguration` の構成差し替え)。

2026-08-15 の性能調査 (perf-android-customcell-composition-reuse) で、Android の `DisposeOnDetachedFromWindow` はスクロールアウト毎に Composition を全破棄させる非推奨経路であり見直しが必要と判明した。しかし機構名が accepted な core ADR に MUST として固定されているため、platform 側の決定 (android/ADR-0015) と core の決定が矛盾したまま併存する構造になる。この「accepted ADR の部分的・非公式上書き」は second-opinion (spec-001) Major 4 で指摘された。

## Decision

本 ADR は core/ADR-0015 を supersede し、その決定を以下のとおり引き継ぎ・再定義する。

- CustomCell (および将来の full-bleed 宣言 UI ホスティング型 Cell) は、ADR-0011 の「共通行レイアウトによる構成」「RecyclerView 内 `ComposeView.setContent` 不使用」の適用除外とする。ADR-0011 の適用範囲は「共通行構造を持つ Cell (標準の基本・入力 Cell 群および同型の利用者定義 Cell)」であり、標準 Cell 群に対する ADR-0011 の決定は引き続き有効。
- 適用除外の Cell は宣言 UI ホスティングの lifecycle 管理を必須とする。
- **Android の具体機構は core では定めず、android ドメインの ADR (現行: android/ADR-0015) に委譲する。**
- **iOS の具体機構は当面 core (本 ADR) で規定する**: `UIHostingConfiguration` の構成差し替え。iOS 機構を見直す決定が生じた場合は、ios ドメインの ADR を起票して同様に委譲する。
- Disclosure Indicator 等、既存 Cell と見た目を揃える装飾は hosted 宣言 UI 内で同一アセット・同一寸法定数を共有して合成する。

## Alternatives Considered

- **core/ADR-0015 を accepted のまま残し、android/ADR-0015 が該当条項を部分上書きする案** — 相反する 2 つの決定が長命層に併存し、core 側だけを読んだ実装者が旧機構を再導入し得る (second-opinion spec-001 Major 4)。却下。
- **core/ADR-0015 を残し index 注記で置換先を追跡可能にする案** — 部分上書きが非公式なまま残る。2026-08-15 オーナー判断「supersede を使うのが良い。例外はそんなに使うものではない」により却下。
- **iOS の具体機構も同時に ios ドメイン ADR へ移し、両 platform を完全委譲する案** — 決定の実質を変えない転記だけの ios ADR が増える。iOS 機構を見直す決定が生じた時点で委譲すれば足りるため却下 (second-opinion spec-002 Major 5 を受けた整理)。

## Consequences

- 正: Android の lifecycle 機構の見直しが、core 改訂なしに android ドメインの ADR で完結する。
- 正: core/ADR-0015 が superseded となり、有効な決定の所在が一意になる。
- 負: 委譲は Android 限定の暫定非対称であり、iOS の具体機構は引き続き core に残る (iOS 機構の見直し時には core 改訂または ios ADR への委譲が必要)。
- 負: CustomCell の lifecycle の全体像を知るには core → android ADR の 2 段の参照が必要になる。

出典: kasane/decisions/core/0015-customcell-exemption-from-shared-row-layout.md (引き継ぎ元) / kasane/changes/archive/2026-08-16-perf-android-customcell-composition-reuse/second-opinion-spec-001.md (Major 4) / 同 second-opinion-spec-002.md (Major 5) / 2026-08-15 オーナー判断 (supersede 方式の選択)

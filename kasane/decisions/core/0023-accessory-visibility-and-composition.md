---
id: 0023
title: Section Header / Footer の表示は可視トグルと内容有無の AND で判定する
status: accepted
date: 2026-08-19
---

## Context

現行契約は Section Header / Footer の「表示しない」を accessory の中身の不在 (nil または空 text) で表現し、「空の Header / Footer に表示領域を割り当てない」を保証している。内容を保持したまま一時的に隠す手段は無く、原典 AiForms の `FooterVisible` 相当 (内容があっても隠す) を Native 起点で再設計する必要があった (maui/ADR-0008 の方針。原典は Footer 側トグルのみで Header 側は存在しない)。

## Decision

Section に Header / Footer それぞれの可視トグル (IsHeaderVisible / IsFooterVisible 相当、命名は各層の慣例に従う) を bool・既定 `true` で追加し、表示判定を「トグル && 内容あり」の AND 合成とする。

- トグルは「内容があっても隠す」専用。内容が無いものをトグルで表示させることはできない。
- 既定 `true` では現行挙動と一致する (後方互換)。「空の Header / Footer に表示領域を割り当てない」保証はそのまま維持する。
- 「内容の不在」は **nil または空 text** と定義し、header / footer 共通・両 OS 共通とする (従来 Android は空文字 footer を隠しておらず、iOS へ対称化する)。
- view accessory は常に「内容あり」として扱う。空の view (例: MAUI の空 ContentView) でも領域は生成され、高さは自己計測または headerHeight 解決 (core/ADR-0021) に従う — spacer 用途は view accessory + 高さ指定で成立する。
- 高さ解決 (core/ADR-0021) は存在判定の**後**に適用する。`Section.headerHeight` / `Theme.headerHeight` は存在する Header の高さを決めるだけで、Header の存在を作らない (従来 iOS は header 不在でも `Section.headerHeight` 正値または `Theme.headerHeight > 0` で supplementary を生成しており — Android は同条件で生成しない — Android へ対称化する)。
- トグルは Section のフィールドとして持ち、Section の値等価性に参加する。更新は既存の Section 置換経路 (`replaceSection`) で運び、専用の Diff / Store 操作は追加しない (`isVisible` / `headerHeight` と同型。SectionAccessory へ表示都合を混ぜることは内容の値との責務分離から採らない)。
- 公開名は native が `isHeaderVisible` / `isFooterVisible`、MAUI が `IsHeaderVisible` / `IsFooterVisible` (.NET 慣例)。本機能は Native 起点の新概念であり、原典 AiForms の `FooterVisible` 命名の踏襲 (maui/ADR-0008) の対象としない。

## Alternatives Considered

- **三値 (auto / shown / hidden)**: 却下。shown (空でも領域を出す) の需要が現状無く、enum が core → native 両 OS → MAUI の各層を貫通する重さに見合わない。spacer 用途は view accessory + 高さ指定で代替でき、需要が出た際の enum 化は可逆。
- **トグル true で空でも強制表示 (領域確保)**: 却下。既定値 true のまま「空の Header / Footer に表示領域を割り当てない」保証を破り、後方互換が破綻する。

## Consequences

- 正: 既定 true で現行挙動と一致し、既存利用者への影響が無い。
- 正: AiForms `FooterVisible` の意味論 (内容保持のまま隠す) と一致し、Header 側にも対称に提供できる。
- 正: bool 1個で済み、輸送・値等価性への参加が単純。
- 負: 「内容が無いが領域だけ出す」は直接表現できない (view accessory + 高さ指定での代替が必要)。
- 負: 内容不在定義と存在判定先行の対称化により既存の公開挙動が変わる — Android の空文字 footer は非表示になり、iOS の header 不在 + 高さ指定 (`Section.headerHeight` / `Theme.headerHeight`) の空領域は生成されなくなる (対称化のための挙動変更は core/ADR-0021 の前例に倣う。text なし spacer は view accessory + 高さ指定で代替)。

出典: kasane/roadmaps/maui-support/phases/phase-9-accessory-visibility/history.md (2026-08-19)

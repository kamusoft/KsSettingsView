---
id: 0024
title: Modern の既定角丸は両 platform で同じ生値 26 に統一する
status: accepted
date: 2026-08-20
---

## Context

Modern style の Section 装飾4属性は、未指定 (nil / null) のとき style 別の platform 既定へ解決する。この既定寸法は従来「各 platform が所有し、platform 間で同じ生値に揃えない」方針で、concepts (core/styling/list-appearance.md) に記録されていた (対応する ADR は無い)。実値は iOS 角丸 26pt (iOS 標準設定画面に近い値をライブラリが定義)、Android 角丸 12dp (従来の Modern 実装値を継承) だった。

サンプルの Section 装飾デモを両 OS で並べたところ、Android の既定角丸 12dp は iOS の 26pt に比べ明らかに小さく、「角丸小」プリセット (両 OS とも 8) とほぼ同じに見える。単一 platform 利用者には各 OS らしい既定として成立するが、MAUI / KMP などクロスプラットフォームでライブラリを使う利用者は、同じ未指定 Theme なのに OS ごとに Section の見た目が大きく異なることに混乱する。

## Decision

Modern の既定角丸を両 platform で同じ生値 **26** (iOS 26pt / Android 26dp) に統一する。Android の `SectionBoxMetrics.MODERN_DEFAULT_CORNER_RADIUS` を 12dp から 26dp へ引き上げ、iOS は変更しない。

- 26 を正とする理由: Modern style の設計原点は iOS 標準設定画面の inset grouped 表現であり、26 はそれに合わせて選ばれた値。Android の 12dp は従来実装値の継承であってデザイン意図を持たない。また Material 3 の extra-large shape (28dp) に近く、Android 上でも 26dp は違和感がない。
- 既定 margin は引き続き platform 所有とし、揃えない (iOS: top 22 / 左右 16 / bottom 0 pt、Android: 上下 12 / 左右 16 dp)。
- 利用者が明示した値の扱いは従来どおり (各 platform の単位でそのまま適用し、両 OS に同じ数字を入れるかは利用者の自由)。

## Alternatives Considered

- **現状維持 (platform 別既定の容認)**: 却下。仕様どおりの挙動ではあるが、クロスプラットフォーム利用者の混乱 (同じ未指定 Theme で OS 間の見た目が大きく違う) を解消できない。
- **サンプル側のみ調整 (デモの「既定」プリセットが明示値を渡す)**: 却下。ライブラリ既定は非対称のまま残り、「既定」プリセットの名が実態と乖離する。
- **中間値 (16〜20) へ両 OS とも変更**: 却下。変更不要な iOS 利用者まで視覚変更に巻き込み、どちらの OS 標準の見た目からも外れる。
- **角丸に加えて margin も含む全既定の統一**: 却下。iOS の margin (top 22 / bottom 0) は iOS 標準設定画面準拠であり、崩すと iOS らしさが減る。隣接 Section 間隔は iOS 22pt / Android 24dp と既にほぼ同等で、角丸ほどの体感差がない。

## Consequences

- 正: 未指定 Theme の Modern 表示がクロスプラットフォームでほぼ揃い、MAUI / KMP 利用者の混乱を解消する。
- 正: iOS は無変更で、iOS 利用者への影響がない。
- 負: Android の既存利用者には既定の見た目が変わる視覚的 breaking change になる (12dp → 26dp)。
- 負: concepts の「既定値を platform 間で同じ生値へ統一しない」規定を角丸について改訂する必要がある (margin の platform 所有は維持するため、規定は全廃ではなく角丸のみの例外化)。

出典: kasane/changes/unify-modern-default-corner-radius/exploration.md / 探索の会話中の議論 (2026-08-20)

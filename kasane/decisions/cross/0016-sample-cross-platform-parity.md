---
id: 0016
title: Sample をプラットフォーム間パリティ検証装置と位置づける
status: accepted
date: 2026-07-31
---

## Context

KsSettingsView はプラットフォーム間で仕様・動作を統一することを製品目的とし、Sample アプリは各 platform 実装が同一に動くことを実行・目視で確認する役割を持つ。しかし現状の Sample は iOS / Android 間で文言・画面構成に差異が蓄積しており (画面タイトルの表記ゆれ、入力 Cell デモの Section / Cell 構成の大幅相違、片側のみの画面など)、画面上の差異が本体の仕様差なのか Sample の書き方の差なのか判別できず、検証にやや難がある状態だった。

## Decision

Sample を「プラットフォーム間パリティの検証装置」と位置づけ、全 platform で一字一句同じ文言・同じ画面構成で実装する。platform ごとに idiomatic な独自サンプルにすることよりも、厳密な一致を優先する。一致は**収束状態**への要求であり、実装順序による一時的な片側先行は追随の追跡を条件に許容する。規約の本文 (一致させる項目・許容される差異・片側先行の条件・platform 固有の技術検証画面の例外枠) は [concepts/cross/conventions/sample-parity.md](../../concepts/cross/conventions/sample-parity.md) に定める。

## Alternatives Considered

- **platform ごとに idiomatic なサンプルにする**: 利用者への「その platform らしい使い方」の見本としては自然だが、却下。サンプルが platform ごとに独自進化すると、画面上の差異が仕様差 (バグ) なのかサンプル差なのか判別不能になり、検証装置として機能しない。本体の仕様・動作自体を platform 間で統一するという製品目的の下では、見本としての価値も同一サンプルで十分に果たせる。

## Consequences

- 正: 目視検証が platform 間の比較として成立し、Sample 上の差異がそのまま本体のバグ・未統一のシグナルになる。
- 正: 文言・構成の二重管理による表記ゆれ (メニューと画面タイトルの不一致等) が規約違反として検出可能になる。
- 負: Sample の変更コストが最終的に全 platform 分になる。片側先行は許容されるが追随義務が残り、追随タスクを追跡し続ける管理コストが発生する。
- 負: platform らしい見せ方・独自の改善 (プレビュー UI 等) の自由度を失う。

出典: 2026-07-31 ksn-concept 対話 (オーナー指示) / samples/ios・samples/android の差異調査
現行照合: 2026-07-31 確認。samples/ios と samples/android を照合し、文言・構成の差異が多数存在 (入力 Cell デモの構成相違、タイトル表記ゆれ、iOS のみの MinimalDiffableDemoView 等)。判定: 乖離あり (規約の新設が本 ADR の目的であり、既存差異の解消は後続変更で行う)。

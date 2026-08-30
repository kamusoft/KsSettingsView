---
id: 0017
title: AiForms.Maui.SettingsView を Native ベースへ移植・リファインする
status: accepted
date: 2026-07-31
---

## Context

kamusoft のアプリ開発は .NET MAUI を利用してきたが、今後 KMP や Native への移行を予定している。設定画面ライブラリ AiForms.Maui.SettingsView は MAUI 専用であり、(1) MAUI の将来性への不安 — MAUI が終息すればライブラリも共倒れになる、(2) 純ネイティブアプリから利用できない、という制約を抱えていた。また既存実装には設計を仕切り直したい蓄積もあった。

## Decision

AiForms.Maui.SettingsView を Native ベースへ移植・リファインし、KsSettingsView として再構築する。Native (iOS / Android) を主、MAUI を副 (Native への binding) と位置付け、Native / KMP / MAUI のどのアプリ形態からも使える形で継続する。移植元との互換 shim は提供しない (独立ブランド。仕様と実装パターンのみ継承する)。移植完了までの移植元参照ルールは [kasane/handbook/cross/aiforms-origin-reference.md](../../handbook/cross/aiforms-origin-reference.md) に定める。

## Alternatives Considered

- **MAUI 版 (AiForms.Maui.SettingsView) をそのまま使い続ける**: 却下。MAUI 専用のため純ネイティブアプリから利用できず、MAUI が終息した場合にライブラリごと死ぬ。アプリ開発を KMP や Native へ移行していく予定とも整合しない。どのアプリ形態でも使える形で継続するには Native を主とする再構築が最も良いと判断した。

## Consequences

- 正: MAUI の存続に依存せず、Native / KMP / MAUI のどのアプリ形態からも利用できる。
- 正: 移植を機に設計を仕切り直せる (リファイン)。
- 負: 移植が完了するまで機能は移植元に劣り、移植元仕様と新実装の二重の知識を参照し続けるコストが掛かる。
- 負: iOS / Android / MAUI の3系統を自前で維持する保守コストが、MAUI 単一版より増える。

出典: 2026-07-31 ksn-concept 対話 (オーナー回答) / kasane/concepts/cross/reference/aiforms-spec-summary.md (独立ブランド方針)
現行照合: 2026-07-31 確認。ios/ と android/ のネイティブ実装が主として進行中、maui/ は将来の binding 用に予約された空の入口 (concepts/cross/architecture/repository-boundaries.md と整合)。判定: 維持。

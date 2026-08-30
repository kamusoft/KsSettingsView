---
id: 0017
title: SwitchCell の状態色はテーマ attr 直参照ではなく accent から導出する
status: accepted
date: 2026-08-25
---

## Context

SwitchCell (MaterialSwitch) のオン track は `cell.accentColor ?: effective.accentColor` で accent に追従する一方、オフ色は Material3 テーマ attr 直参照 (thumb = `colorOutline` / track = `colorSurfaceContainerHighest`)、オン thumb は `colorOnPrimary` だった。このためオフ状態はアプリテーマの色味 (紫系テーマなら紫がかったグレー) になり accent と無関係で、オン thumb はダークモードでテーマ primary の暗トーン (紫紺) に解決され、track が accent なのに thumb だけテーマ色が漏れていた。

## Decision

SwitchCell の状態色は accent を基準に導出し、テーマ attr を色の決定に直接使わない:

- **オフ track / thumb**: テーマ attr (`colorSurfaceContainerHighest` / `colorOutline`) は**明度の土台**としてのみ使い、色相 = accent、彩度 = 固定の淡さ (track 0.09 / thumb 0.04)、明度比 (track 1.0 / thumb 0.92) で導出する (`tintedFrom`)。素の MaterialSwitch が attr 経由でライト/ダークの明度関係を反転させる構造を模倣し、ダークでは自動的に「thumb が track より明るい」関係になる
- **オフ時の track 枠線** (`trackDecorationTintList` unchecked) は thumb と同一の導出色 (M3 既定の「枠線 = thumb 色」関係を踏襲)。オン時は透明 (M3 既定と同値)
- **オン thumb**: `colorOnPrimary` 参照を撤去し、accent に対するコントラスト色 (`onThumbColorFrom`) とする — 白とのコントラスト比 ≥ 1.5 なら白、下回る明色 accent のみ accent 色相の暗色 (彩度 0.10 / 明度 0.15)。閾値 1.5 は緑 #34C759 等の一般的な accent (対白比 2.2 前後) で白 thumb を維持するための実測値
- オン track (accent そのまま) は従来どおり

## Alternatives Considered

- **テーマ attr 直参照の維持**: オフ色が accent と無関係のテーマ色になり、ダークのオン thumb にテーマ色が漏れる (本 change の起点となったオーナー指摘) ため却下。
- **ライト実測ベースの固定係数乗算** (blend 比率 + 明度スケールの直値): ライトでは確定した見た目を出せるが、ダークで track と thumb の明度差が潰れる (実測差 2pt) ため却下。
- **オン thumb のコントラスト閾値を WCAG 非文字基準の 3.0 にする**: 緑・橙・青緑など一般的な accent が軒並み暗色 thumb に倒れ、ライトの確定した見た目が変わるため却下。thumb は文字ではなく面積のある図形で、素の M3 も同水準の比で白 thumb を置いている。

## Consequences

- 正: オフ状態にも accent の気配が残り、Cell / Theme の accent 指定に全状態が追従する。ダークモードでも素の M3 と同じ明度関係を自動で保つ。
- 正: `materialSwitchStyle` やアプリテーマの色構成に依存しないため、テーマ側の色変更で Switch だけ意図しない色になる事故が構造的に起きない。
- 負: Material3 標準のオフ色 (テーマの neutral 系) と厳密には一致しなくなる — テーマ色とライブラリ accent が大きく異なるアプリでは、標準 Switch と並べたときに差が見える。
- 負: 導出係数 (彩度・明度比・コントラスト閾値) はライブラリ内部定数で、利用者から調整できない。

出典: kasane/changes/archive/2026-08-25-adjust-section-spacing/summary.md (SwitchCell の状態色・採用値と根拠)

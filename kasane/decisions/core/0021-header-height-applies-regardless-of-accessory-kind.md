---
id: 0021
title: Section Header の固定高さは accessory 種別に依らず適用する (OS 対称)
status: accepted
date: 2026-08-11
---

## Context

`Section.headerHeight` / `Theme.headerHeight` の解決は concepts 上 text accessory の段落内でしか記述されておらず、view accessory (`SectionAccessory.View`) に効くかは未規定だった。native 実装も OS 非対称で、iOS (`makeHeaderBoundaryItem`) は accessory 種別を見ずに固定高さを適用する一方、Android は Text accessory にしか headerHeight を渡さず view では常に自動高さ (AiForms 原典準拠) だった。MAUI の view accessory 対応 (phase-6-accessory-views) は固定高さ semantics を両 OS 対称で前提にするため、契約の確定が先行して必要になった。

## Decision

`Section.headerHeight` / `Theme.headerHeight` は accessory 種別 (text / view) に依らず、同一の優先順位で適用する:

1. `Section.headerHeight` 正値 → 固定高さ (内容がはみ出す場合は clip)
2. `-1` (自動) かつ `Theme.headerHeight` 正値 → Theme の固定高さ
3. いずれも正値でなければ内容に応じた自動高さ

iOS の現行挙動を意図的拡張として確定し、Android を iOS へ対称化する (AiForms 原典非準拠を両 OS で受け入れる)。固定高さが解決されたとき、view accessory の hosted view は Header 領域いっぱいに配置する (iOS は contentView への 4 辺 pin、Android は `MATCH_PARENT` で対称)。`headerHeight` は Header 専用で、Footer の view accessory は対象外 (現行契約の維持)。

## Alternatives Considered

- **AiForms 原典踏襲 (view accessory は常に自動高さ、固定は text のみ)**: 却下。明示指定した `headerHeight` が無言で無視される罠になること、原典準拠へ倒すと iOS の公開挙動変更 (既存 iOS 利用者の見た目が変わる) になること、MAUI phase-6 が対称な固定高さ semantics を前提にできなくなることによる (phase-6-accessory-views 論点⑤の裁定)。
- **hosted view を内容なりの高さで top 揃えのまま残す (領域占有の非対称を許容)**: 却下。背景・枠線を持つ view で「iOS は領域全面が塗られ Android は内容分のみ」という見た目の差が残り、対称化の目的に反するため (実装レビュー review-001 の指摘を受けたオーナー裁定)。

## Consequences

- 正: 明示指定した固定高さが accessory 種別に依らず常に効き、MAUI 層は両 OS 対称の固定高さ semantics を前提にできる。
- 正: 高さのみの動的変更は Android では hosted view を維持したまま反映される (内部状態を失わない)。
- 負: AiForms 原典非準拠を両 platform で確定する (原典互換を期待する利用者には差異になる)。
- 負: view accessory と正の headerHeight を併用していた Android 既存利用者は、自動高さ → 固定高さ (はみ出しは clip) へ見た目が変わる。

出典: kasane/changes/archive/2026-08-11-align-view-accessory-header-height/exploration.md (裁定 2026-08-11) / kasane/roadmaps/maui-support/phases/phase-6-accessory-views/agenda.md (決定⑤) / 同 change の deviation.md (hosted view の領域占有)

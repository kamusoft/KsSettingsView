---
type: policy
title: 利用者向け Skill の API 掲載基準
description: skills/ (利用者向け Agent Skills) に公開 API をどこまで載せるか — 「簡潔でも網羅」の方針と意図的な掲載除外の基準・現行除外リスト
tags: [conventions, docs, skills, api-coverage]
timestamp: 2026-08-29
---

この文書は、利用者向け Agent Skills (`skills/{en,ja}/`) に公開 API をどこまで掲載するかの基準を説明する。読むと、docs-refresh スキルの Step 3e (API 名網羅検査。以下 3e) が報告した「未掲載の API 名」を、追加すべき漏れと意図的な除外に仕分けられる。skills の生成・更新手順そのものは docs-refresh スキルが定め、公開契約の正本は concepts (この文書もその一部) とコード・テストである。

## 方針: 簡潔でも網羅

公開 Skill は、利用者が使う機能について**簡潔でも網羅されている**ことを目指す。基準は「プロパティ名・機能名が Skill のどこかで一度は見つかること」— 詳細解説やコード例までは求めない。名前が一度も現れない公開 API は、利用者のエージェントがその機能の存在に気づけないため、掲載漏れとして扱う。

この基準が生まれた背景: skills 初期生成時に concepts から skills への翻訳で入力系 Cell のプロパティ・コールバックが 3 platform で同型に脱落した。docs-refresh の追従は concepts ファイル単位のハッシュ差分で駆動するため、concepts 側が変わらない限りこの取りこぼしは検出されず、繰り返し実行しても残り続けた (経緯は下記「関連」の change)。

## 意図的な掲載除外の基準

次のいずれかに当たる公開 API は、公開宣言であっても Skill に**載せない**:

| 基準 | 説明 |
|---|---|
| 低頻度の細部 API | 通常の利用フローで呼ぶ必要がない補助操作。機能説明レベルで足りるものはメソッド名も載せない |
| 内部層・interop 層 | 利用者が直接触らない層。MAUI facade と各 native 実装を繋ぐ interop 層 (Bridge / Handler / binding assembly) が該当し、SwiftPM product 非公開や「アプリからは直接使わない」と concepts が定めるものを含む |
| 機械的に導出できる名前 | 命名規約から一意に導ける宣言群。規約を1行書けば個別列挙は不要 |
| 可視性引き下げ候補 | 描画内部ユーティリティとして意図された public 宣言。掲載ではなく internal 化の検討対象 (変更として起票する) |

## 現行の除外リスト (2026-08-29 skills-api-coverage で合意)

| platform | 除外 API | 基準 |
|---|---|---|
| iOS | `KsSettingsViewController.disconnectStore()`、`SettingsRootStore.preview`、`KsCellRegistry.resolveRendererType()` / `removeAll()`、`SettingsRootBuilder` / `KsSectionBuilder` の型名 (DSL の記法自体は掲載し、scope 型の名前だけ除外する)、`rootHeaderElementKind` / `rootFooterElementKind` | 低頻度の細部 |
| iOS | `EffectiveStyle` | 可視性引き下げ候補 (changes: ios-effectivestyle-visibility) |
| iOS | `KsSettingsViewBridge` 配下一式 | 内部層 |
| Android | `KsSettingsView.unbind`、`SettingsRootStore.preview`、`KsCellRegistry.viewTypeOf` / `isRegistered` / `registerBasicCells` / `registerInputCells` / `registerCustomCell`、`CustomCellEmptyContent`、`SettingsRootDsl` / `SectionScope` の型名 (iOS と同じく DSL の記法は掲載し、scope 型の名前だけ除外する)、`DSLIconModifiableCell.withDSLIcon` | 低頻度の細部 (Registry 補助は可視性引き下げ候補を兼ねる) |
| Android | `ks-settingsview-bridge` 配下一式 | 内部層 |
| MAUI | `FooProperty` (BindableProperty フィールド) の個別列挙 | 機械的に導出できる名前 (「各 bindable プロパティは対応する `FooProperty` を持つ」の規約1行で代替) |
| MAUI | `MauiAppBuilderExtensions` の型名 (`AddKsSettingsView()` は掲載) | 低頻度の細部 |
| MAUI | `SettingsViewHandler`、`Internals/` 配下、`KsSettingsView.Binding.*` | 内部層 |

リストの更新は、3e 報告に対するオーナー判断 (掲載 / 除外) が確定したときに行う。3e はこの表を参照しないため、表を更新しても報告される名前は変わらない — 変わるのは仕分けの速さで、表にある名前は「判断済みの除外」として即座に落とせる。同じ名前を毎回オーナーに問い直している状態が、この表の更新漏れである。

## してはいけないこと

- 除外リストを理由に、リストに無い未掲載 API を独断で「低頻度だから」と除外しない — 新規の除外はオーナー判断で確定してからこの表へ載せる
- 除外 API を concepts から消さない — concepts は公開契約の正本であり、この基準は skills (利用者向け派生物) の掲載範囲だけを絞る

## 関連

- docs-refresh スキル (`.agents/skills/docs-refresh/SKILL.md`) — 3e API 名網羅検査と Step 4 の承認フロー
- [cross/ADR-0022](../../../decisions/cross/0022-user-docs-as-agent-skills.md) — 利用者向けドキュメントを Agent Skills として提供する決定
- 経緯: `kasane/changes/archive/2026-08-29-skills-api-coverage/exploration.md`

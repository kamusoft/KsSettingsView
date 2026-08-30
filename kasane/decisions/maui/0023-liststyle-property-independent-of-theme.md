---
id: 0023
title: style は非 nullable の `ListStyle` プロパティとして Theme と独立経路で公開する
status: accepted
date: 2026-08-20
---

## Context

maui-support / phase-11-modern-style の議論。Native の `KsSettingsViewStyle` (Classic / Modern) は両 OS とも Theme とは別経路で公開されている (iOS `KsSettingsViewController.style` / Android `KsSettingsView.style` の可変プロパティ。Theme は Store の `applyTheme` 経由)。MAUI facade には style の公開 API が存在せず、Bridge にも style を渡す口が無い (両 OS とも Classic 固定で生成) ため、公開の形をゼロから決める必要があった。

プロパティ名の制約として、素直な `Style` は `VisualElement.Style` (XAML の Style 機構) と衝突して使えない。候補として `Appearance` / `ListAppearance` / `ListStyle` / `DisplayStyle` / `VisualStyle` を比較した。

## Decision

- MAUI 層に統一 enum **`SettingsViewStyle { Classic, Modern }`** を新設し (maui/[ADR-0013](0013-datepicker-uistyle-unified-enum.md) の意味軸統一 enum の先例に従う)、`SettingsView` に BindableProperty **`ListStyle`** として公開する。
- プロパティは**非 nullable・既定 `Classic`** とする。Theme 4属性と異なり、既定 Classic は両 OS 共通の契約レベルの既定であり、null の「platform 既定へ委譲」意味論が不要なため。
- Theme snapshot (`KsThemeSnapshot`) には同梱せず、**独立の `SetStyle` 経路** (Gateway → Bridge の新設 API) で伝搬する。Native の「Theme は Store 経由・style は View/Controller プロパティ直接」という分離に対称な形を保つ。

## Alternatives Considered

- **プロパティ名 `Appearance` / `ListAppearance`**: concepts のドメイン名 (list-appearance /「設定 list の外観」) と一致し簡潔だが、Native の公開 API 用語 `style` と語が割れ、プラットフォームをまたぐ利用者に対応表が必要になる。"Style" の語を保ち Native との対応が自明な `ListStyle` をオーナーが選択。
- **プロパティ名 `SettingsViewStyle` / `DisplayStyle` / `ViewStyle`**: それぞれ冗長 (`SettingsView.SettingsViewStyle`)・漠然・XAML Style との紛らわしさ残りで不採用。`SectionStyle` は style が separator 規則等も変えるため誤誘導で却下。
- **nullable プロパティ (null = native 既定)**: 4属性や ADR-0013 と同じ形だが、style の既定は platform が所有する値ではなく契約共通の Classic であり、null の意味が空になるため不採用。
- **Theme snapshot への同梱**: 送る口は既存で済むが、Native 側で style が Theme に属さない (Store を通らない) 構造と非対称になり、style 切替 (装飾の全再構築) と Theme 再適用の意味の違いも失われるため不採用。

## Consequences

- 正: XAML で `ListStyle="Modern"` と1属性で切替でき、Native の `style` との対応が字面から追える。
- 正: 独立経路により、Native の style 切替意味論 (identity 維持・全 Section 再描画) を Theme 再送と混ぜずに伝えられる。
- 負: Gateway / Bridge の公開面に `SetStyle` が1メソッド増える (iOS ApiDefinition / Android Metadata の binding 追随を含む)。
- 負: 将来 style の case が増えた場合、MAUI enum への追随が必要 (統一 enum の宿命。ADR-0013 と同質)。
- 負: style は Store を通らないため Store の状態復元に乗らず、Host 再生成をまたぐ保持機構が別途必要になる — Bridge が Host 外のフィールドで style を保持し `makeHost*` 生成時に適用する (gateway は Host 解放をまたいで作り直されないため「再接続時の再送」では成立しない)。(出典: 実装結果)

---
出典: 2026-08-20 ksn-agenda (maui-support / phase-11-modern-style) での議論 (`ListStyle` の採用はオーナー判断)

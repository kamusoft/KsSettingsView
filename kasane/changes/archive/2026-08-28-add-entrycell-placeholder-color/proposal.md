# Proposal: add-entrycell-placeholder-color

## Why

AiForms.Maui.SettingsView の `EntryCell.PlaceholderColor` に相当する機能が KsSettingsView に無い (オーナー判定: 実装漏れ)。placeholder 文字列は全レイヤに通っているが色の指定経路がどの層にも無く、常に OS 既定色で描画される。移行元で普通に使われる機能であり代替も無いため、移行互換の穴になっている。

## What Changes

- `EntryCell` に placeholder 文字色の指定を追加する (iOS / Android / MAUI)。解決は標準4段: Cell 固有値 (`placeholderColor`) → `CellStyle.placeholderColor` → `Theme.cellPlaceholderColor` → platform default (concepts/core/styling/style-resolution.md の標準解決順)
- bridge (iOS / Android の Entry DTO と Theme DTO) および MAUI facade (`EntryCell.PlaceholderColor`・`SettingsView.CellPlaceholderColor`・snapshot・Gateway 両 platform) へ導線を追加する。MAUI は per-cell プロパティと重複するため CellStyle 段の指定 (および CellStyle 輸送へのフィールド追加) を持たない
- 付随修正: Android `EntryCell` の入力文字色を valueText の解決順 (`CellStyle` → Theme valueText 既定 → Theme title 既定 → platform default) に是正する。現状は `titleColor` 直参照で、iOS および style-resolution.md の規約と不一致
- サンプルアプリの Entry デモに placeholder 色の利用例を追加する (視覚検証の足場を兼ねる)
- 影響 capability: cell-types-input / settings-view-ios-ui / settings-view-android-ui / maui-cells / maui-core / maui-bridge / samples-{ios,android,maui}

## Non-Goals

- AiForms の Android 未指定時既定色 (`#D2D2D2`) の再現 — 探索で「OS 既定のまま」を決定済み。固定色はダークモードに追従せず、platform の慣例を直接公開するライブラリ方針に反する
- EntryCell 以外への placeholder 概念の導入 — placeholder は入力欄 (`EntryCell`) 固有の表示。他 Cell の未選択表示は valueText 系の既存契約の領分
- 移行 Skill (skills/{ja,en} の api-mapping) と README の追従更新 — docs-refresh スキルの責務 (蒸留後にユーザーが明示依頼する運用)

## Impact

- **ソース互換**: 保たれる (追加 API のみ。既存利用コードの修正は不要、全 platform)
- **バイナリ互換**: iOS `EntryCell` init / Android `EntryCell` コンストラクタ / `CellStyle` / `Theme` へのフィールド追加は、既定引数付きでもコンパイル済みバイナリとは非互換になり得る (利用側の再コンパイル前提。パッケージを同時リリースする現行運用では実害なし)
- **挙動**: placeholder の既定挙動 (OS 既定色) は不変。Android 入力文字色の是正は**意図した挙動変更** — Theme / CellStyle で valueText 色を明示指定している利用者は Android の入力中テキスト色が変わる (規約準拠への是正。valueText 未指定なら title 色へ fallback するため見た目不変)。探索で決定済みのため本 change に同梱し、別 change へ分離しない
- iOS は色指定時に `attributedPlaceholder` へ切り替えるため、font との順序依存が生まれる (AiForms と同構造)。テストで固定する

## 級: M

公開 API 追加が 3 platform + bridge / facade / テスト3系統 (約18ファイル) に及ぶが、設計判断は既存規約 (標準解決順・accentColor 導線) の踏襲で新規性は無いため。

domain: cross

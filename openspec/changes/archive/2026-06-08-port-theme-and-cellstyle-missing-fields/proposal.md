## Why

オリジナル `AiForms.Maui.SettingsView` の `SettingsView` クラスは「Cell 全体既定」として `CellTitleColor` / `CellTitleFontSize` / `CellValueText*` / `CellDescription*` / `CellHint*` / `CellIconSize` / `CellIconRadius` を持っていたが、KsSettingsView の `Theme` には `titleColor` / `titleFont` のみが昇格しており、残りが移植漏れになっている。利用者は「`Theme.cellHintTextColor` を 1 箇所セットすれば全 Cell のヒント文字色が変わる」というオリジナルの 2 段重ね運用ができず、各 Cell ごとに `CellStyle.hintTextColor` を個別指定する必要があり、表示互換性に直接影響する。

加えて `Theme.viewBackgroundColor` / `Theme.titleColor` はオリジナル命名（`BackgroundColor` / `CellTitleColor`）との表記揺れがあり、利用者がまだ少ない基礎段階で破壊的にリネームしておくのが望ましい。

詳細計画: `openspec/drafts/05-port-gap-change-plan-roadmap.md` §1 / 調査: `openspec/drafts/04-original-property-port-gap-survey.md` §3。

## What Changes

### Theme リネーム（**BREAKING**、互換シムなし）

- **BREAKING**: `Theme.viewBackgroundColor` → `Theme.backgroundColor`（iOS / Android 両方）
- **BREAKING**: `Theme.titleColor` → `Theme.cellTitleColor`（iOS / Android 両方）
- **BREAKING**: `Theme.titleFont` → `Theme.cellTitleFont`（iOS / Android 両方、`cellTitleColor` と命名整合）

### Theme へのフィールド追加（漏れ補完）

ヘッダ/フッタ系:
- `headerFontFamily`（相当）— `UIFont` / `TextStyle` 経由で family を持たせる
- `headerFontAttributes`（相当）— bold / italic 等
- `footerFontFamily`（相当）
- `footerFontAttributes`（相当）
- `headerHeight`（`Double`, 既定 `-1.0` = 自動）— SettingsView 全体既定としての `HeaderHeight` 復活

Cell 全体既定（オリジナル `CellXxx` 系の Theme 昇格）:
- `cellTitleFontSize`（独立 `Double`、`titleFont` と併存）
- `cellValueTextColor`
- `cellValueTextFont`
- `cellDescriptionColor`
- `cellDescriptionFont`
- `cellHintTextColor`
- `cellHintFont`
- `cellIconSize`
- `cellIconRadius`

### EffectiveStyle で解決順序を確立

全 Cell プロパティについて以下の順序で最終値を解決する：

```
最終値 = CellStyle.X           if 非 nil
       else Theme.cellX        if 非 nil
       else プラットフォーム既定
```

例外: `ButtonCell.titleColor` は既存 4 段優先（`ButtonCell.titleColor → CellStyle.titleColor → Theme.cellTitleColor → 既定`）を維持。

### fontFamily 課題の解決

Compose 側で `TextStyle.fontFamily` の equals 判定に課題があるとの既存コメントに対し、`fontFamily` 指定がレイアウトとして確実に効くことを検証するテストを追加（インスタンス再利用時の安定性、size 反映の e2e）。iOS は `UIFont.isEqual(_:)` ベースで実績ありのため確認のみ。

### スコープ外（Change 2 / 3 で別途扱う）

- 全 Cell（Switch / Checkbox / Radio / SimpleCheck / Button）の `description` / `icon` / `hintText` 共通化
- `Section.isVisible` / `Cell.isVisible`
- `RadioCell.accentColor` / `SimpleCheckCell.accentColor` 移植漏れ

## Capabilities

### New Capabilities

なし。

### Modified Capabilities

- `settings-view-android-style`: Theme のフィールドリネーム（`viewBackgroundColor` → `backgroundColor`, `titleColor` → `cellTitleColor`）、Cell 全体既定（Description / ValueText / HintText / Icon 系）と Header/Footer Font 系の追加、EffectiveStyle の解決順序 Requirement 明示化
- `settings-view-ios-style`: 同上（iOS 版 Theme / CellStyle / EffectiveStyle）

## Impact

### 影響モジュール

**iOS:**
- `ios/Sources/KsSettingsViewUI/Theme.swift`（リネーム + フィールド追加 + 既定値定数）
- `ios/Sources/KsSettingsViewUI/CellStyle.swift`（漏れ確認、追加は最小）
- `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift`（全プロパティの解決順序実装）
- `ios/Sources/KsSettingsViewUI/{LabelCellView,CommandCellView,...}.swift`（EffectiveStyle 経由で値取得するよう書き換え）
- `ios/Tests/KsSettingsViewUITests/`（解決順序 / UIFont equals / rename 反映のテスト追加）

**Android:**
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`（リネーム + フィールド追加 + 既定値 companion）
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellStyle.kt`（漏れ確認）
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt`（解決順序実装）
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/{LabelCellViewHolder,CommandCellViewHolder,...}.kt`（EffectiveStyle 経由）
- `android/ks-settingsview-ui/src/test/kotlin/`（解決順序 / TextStyle equals / fontFamily 反映のテスト追加）

**サンプル:**
- `samples/ios/` の Theme / CellStyle 利用箇所を新 API 名に書き換え
- `samples/android/` の Theme / CellStyle 利用箇所を新 API 名に書き換え

### Breaking Changes / Risks

- `Theme.viewBackgroundColor` / `Theme.titleColor` / `Theme.titleFont` を参照しているアプリは **コンパイルエラー** になる。互換シム（旧名 deprecated 残し）は意図的に置かない（利用者が少ない基礎段階のため）。
- サンプルアプリは本 change 内ですべて更新する。
- 外部利用者向けに CHANGELOG / README で rename を周知する。

### 着手前確認（実装フェーズ）

- 並行する in-progress change（`add-cell-types-input` / `add-cell-types-custom` / `add-maui-*` / `add-samples-maui`）が Theme / CellStyle / EffectiveStyle に新フィールドを追加していないことを再確認（着手時点で `openspec list --json` を再実行）。
- `archive/purify-core-extract-style-to-ui-layer` を読み、Theme が UI 層所属である設計思想を維持していることを確認。

# Exploration: add-entrycell-placeholder-color

起票日: 2026-08-26 / 起票元: rollout-user-skills のオーナー検収 (移行対応表レビュー中に発覚)
探索日: 2026-08-27

## 課題 / 動機

AiForms.Maui.SettingsView の `EntryCell.PlaceholderColor` に相当する機能が KsSettingsView に無い (移行 Skill の api-mapping で「提供しない」扱いになっていた)。オーナー判定は**実装漏れ** — placeholder 文字色の指定は移行元で普通に使われる機能であり、代替も無い。

### 現状 (調査結果 2026-08-27)

placeholder 文字列は全レイヤに通っているが、色を指定する経路はどの層にも存在せず、完全に OS 既定色。

- iOS: `textField.placeholder` のプレーン代入のみ (`ios/Sources/KsSettingsViewUI/EntryCellView.swift:137`)。`attributedPlaceholder` 未使用
- Android: `editText.hint` 代入のみ。`setHintTextColor` は EntryCell 経路で未使用 (`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:149-154`)
- MAUI facade: `PlaceholderColor` 相当プロパティなし (`maui/KsSettingsView.Maui/EntryCell.cs`)
- 移植元 AiForms: per-cell の BindableProperty のみ (SettingsView 全体プロパティ版は無し)。iOS は `AttributedPlaceholder`、Android は `SetHintTextColor` で適用。Android の未指定時 fallback は `#D2D2D2` ハードコード
- 既存の `hintTextColor` は行内右上の補助表示 (`hintText`) 用で placeholder とは別物。既定が accent 色へ落ちるため流用は不適
- 実装モデルの先例: `accentColor` (per-cell → CellStyle → Theme の解決 + bridge 導線が既にある)

## 検討した選択肢 (却下案と理由を含む)

1. **指定範囲**
   - Cell 単位のみ (AiForms 互換の最小形) — 却下: ユーザー判断で全 Cell 一括指定 (CellStyle / Theme 段) まで持たせる方針を採用
   - MAUI のみ — 実質不可 (MAUI facade は core の描画に乗るため core 対応が必須)
   - **採用: 標準4段解決** — `EntryCell.placeholderColor` → `CellStyle.placeholderColor` → `Theme.cellPlaceholderColor` → platform default (concepts/core/styling/style-resolution.md の標準解決順に準拠)
2. **未指定時の既定色**
   - AiForms 互換の固定色 (`#D2D2D2`、Android のみ) — 却下: ハードコード既定の持ち込みはライブラリ方針 (platform の型と慣例を直接公開) に反し、ダークモードに追従しない
   - **採用: OS 既定のまま** — iOS はシステムの placeholder 色、Android は Theme.Material3 の `textColorHint`。現状の見た目を維持しダークモードへ自動追従
3. **隣接課題の扱い** (調査中に発見: 入力中テキストの色が iOS は valueText 解決、Android は `titleColor` 直参照で不一致)
   - 別 change に逃がす — 却下: 同じ領域の課題は同じ change で直す方針
   - **採用: 本 change で同梱修正** — Android を concepts の valueText 解決順 (`CellStyle` → Theme valueText 既定 → Theme title 既定 → platform default) に揃える。valueText 未指定 Theme では title 色へ fallback するため、見た目が変わるのは valueText 色を明示指定していた利用者のみ

## 決定事項

- placeholder 色は標準4段解決 (Cell 固有値 → CellStyle → Theme → platform default) で追加する。命名は既存慣例に合わせ `placeholderColor` / `cellPlaceholderColor`
- 未指定時は OS 既定 (AiForms の `#D2D2D2` は再現しない)
- Android EntryCell の入力文字色を valueText の解決順に修正する (規約乖離の是正、同 change に同梱)
- iOS の適用は `attributedPlaceholder` 切替方式 (色指定時のみ attributed、未指定時はプレーン placeholder)。font との順序依存に注意 (AiForms 同様、attributed 側へ font を持ち込む必要あり)
- Android の適用は `setHintTextColor`。フォーカス中の再バインドが多いため既存作法どおり差分判定を入れる

## ADR 候補

なし。いずれの決定も既存の明文規約 (style-resolution.md の標準解決順・platform default 方針) に従う選択であり、新たな制約や境界の変更を持ち込まないため、探索メモの決定事項で足りると判断。

## 未決の論点

- Theme / CellStyle 段の追加に伴う bridge (KsBridgeCellStyle / KsBridgeTheme) と MAUI snapshot (KsThemeSnapshot 等) の具体的な変更点は提案フェーズで詳細化
- 移行 Skill (skills/{ja,en}/kssettingsview-aiforms-migration/references/api-mapping.md の「提供しない」記述、および kasane/concepts/cross/conventions/aiforms-spec-summary.md の一覧行) の更新 — 実装完了・蒸留後に docs-refresh をユーザーが明示依頼する運用

## UI 素材 (ui/references/ の一覧と注釈)

なし (既存 Cell への色プロパティ追加のため新規デザイン素材は不要)

## 変更級の推奨: M

理由:
- 触るレイヤが広い: iOS core (Cell / View / EffectiveStyle / Theme / CellStyle / bridge)、Android core (同)、MAUI facade (BindableProperty / snapshot / Gateway 両 platform)、テスト3系統 — 約18ファイル規模
- 公開 API 変更あり (EntryCell / CellStyle / Theme への色プロパティ追加、3 platform とも)
- 挙動修正の同梱あり (Android 入力文字色の解決順変更 — valueText 明示指定の利用者には見た目が変わる)
- UI への影響はあるが新規デザインは無し (色プロパティの追加)

## 実装時に触るレイヤ一覧 (調査より)

- iOS: `EntryCell.swift` (プロパティ + init/==/hash/withDSLID/withStyle/withIcon の計8箇所)、`EntryCellView.swift` (render の attributedPlaceholder 分岐 + prepareForReuse リセット + 差分は valueText 解決の確認)、`EffectiveStyle.swift`、`CellStyle.swift`、`Theme.swift`、`KsBridgeEntryCell.swift`、`KsBridgeCellStyle` / `KsBridgeTheme` 系
- Android: `EntryCell.kt`、`EntryCellViewHolder.kt` (setHintTextColor + 入力文字色の valueText 解決化)、`EffectiveStyle.kt`、`CellStyle.kt`、`Theme.kt`、`KsBridgeEntryCell.kt`、bridge の style/theme DTO
- MAUI: `EntryCell.cs`、`Internals/KsCellSnapshots.cs`、`SettingsView.cs` / theme snapshot、`Platforms/{iOS,Android}/KsBridgeGateway.cs`
- テスト: iOS EntryCell 系、`android/.../InputCellsTest.kt`、`maui/KsSettingsView.Maui.Tests/ConversionPathTests.cs` (bridge 変換パス網羅)

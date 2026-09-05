# Exploration: fix-default-colors-dark-appearance

## 課題 / 動機

ライブラリ本体の Theme 既定色が 3 platform とも外観 (iOS のダーク / Android の夜間モード) に追随しない。title / description の文字色だけがシステム色 (iOS `.label` / `.secondaryLabel`、Android の同梱 DayNight テーマの `textColorPrimary`) で外観に追随するため、Theme を渡さずにダーク外観で使うと**白地に淡色 (白) 文字**になり判読できない。

発見の文脈: add-sample-dark-mode-toggle の実装フェーズ (2026-09-05)。サンプルに外観切替を付けて「Theme を渡さない画面 (isVisible デモ・Section 装飾デモの箱)」をダークで開いたところ、Android Native・iOS Native の両面で再現 (証跡: `kasane/changes/add-sample-dark-mode-toggle/ui/verification/android-visibility-dark.png` / `ios-visibility-dark.png`。注意: config `distill.archive-media: delete` により archive 時に画像は削除されるので、必要なら archive 前に確認するか本 change の `ui/references/` へ写す。archive 後は `kasane/changes/archive/*-add-sample-dark-mode-toggle/ui/brief.md` の照合記録 (文章) だけが残る)。同 change では proposal の Non-Goals (本体の既定値・夜間モード解決の変更) を守って deviation で達成範囲を縮小し、本体側をこの change に切り出した (オーナー裁定 2026-09-05)。

該当箇所:
- iOS: `ios/Sources/KsSettingsViewUI/Theme.swift` — `defaultBackgroundColor` (白固定) / `cellBackgroundColor` の既定引数 `.white` / `defaultSeparatorColor` / `defaultHeaderTextColor` / `defaultFooterTextColor` が固定 RGB。dynamic なのは `defaultCellTitleColor` = `.label`、`defaultCellDescriptionColor` = `.secondaryLabel` のみ
- Android: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt` — `DEFAULT_BACKGROUND_COLOR` (#FFFFFF) / `cellBackgroundColor` 既定 `Color.White` / `DEFAULT_SEPARATOR_COLOR` / `DEFAULT_CELL_DESCRIPTION_COLOR` が固定値。`ui/EffectiveStyle.kt` の `resolveDefaultTitleColor` だけが `android.R.attr.textColorPrimary` から解決。ライブラリの res に `values-night` は無い
- MAUI: facade は native をラップするため同じ挙動。MAUI iOS / MAUI Android の両実行面で同じ症状を確認済み (証跡: add-sample-dark-mode-toggle の `ui/verification/maui-ios-visibility-dark.png` / `maui-android-visibility-dark.png`、同 `ui/brief.md` の照合結果)

長命層の記述の乖離 (本 change で直すか、ksn-drift / 蒸留で扱う):
- `kasane/concepts/core/styling/style-resolution.md` の「Android のライト / ダークは、同梱テーマが DayNight 派生であるため端末の夜間モードとアプリの uiMode 制御で決まる」— chrome・選択面・title 既定には当てはまるが、Theme の既定色定数には届いていない
- add-sample-dark-mode-toggle の exploration.md「iOS のライブラリ既定色はシステム色 (`UIColor.label` 等) でアプリ外観に自動追随する」— 文字色にしか当てはまらない

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- 既定の背景・Cell 背景・separator・header / footer 文字色を dynamic / 夜間対応にするか (公開 API の既定の見た目が変わる利用者可視の変更。既存利用者のライト時の見た目は変えない設計にできるか)
- 3 platform で既定の dark 値をどう揃えるか (iOS はシステム色、Android は同梱テーマの属性 or `values-night`、MAUI は native 追随でよいか)
- 既定色の platform 差 (sample-parity の「本体既定値の platform 差」) との関係。dark でも同じ扱いにするか
- 「文字色だけ追随して背景が追随しない」現状は既存不具合として扱うか (利用者がダーク端末で Theme を渡さずに使うと判読不能)
- concepts (style-resolution.md) の記述修正の範囲

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (3 platform の公開既定値変更で ADR を伴う見込みのため M 以上の暫定)

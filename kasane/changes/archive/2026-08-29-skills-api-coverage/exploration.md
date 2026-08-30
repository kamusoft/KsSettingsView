# Exploration: skills-api-coverage

## 課題 / 動機

利用者向け Agent Skills (`skills/{en,ja}/kssettingsview-{ios,android,maui}/`) は構成・見つけやすさは良いが、公開 API の網羅性に漏れがある疑い。公開スキルとしては、用意してある機能が簡潔でも網羅されていることが望ましい。iOS を起点に3プラットフォームを調査 (2026-08-29、ksn-scout ×3 並走)。

## 検討した選択肢 (却下案と理由を含む)

- 判明した漏れを skills に全量掲載する案 → 却下: 利用頻度の低い細部 API (Registry の個別メソッド名・`PickerItem.init`・`SettingsRootStore.preview`・`disconnectStore()`・ビルダー型名) まで載せると簡潔さを損なう
- 漏れ項目を concepts へ全量追記する案 → 却下: 同上の細部 API は concepts 追記も不要 (機能説明レベルで足りている)

## 決定事項

- スコープ: 確実な機能漏れ + Theme default 定数群。内部層 (iOS Bridge 等) は対象外 (ユーザー合意 2026-08-29)
- 導入手順の product / パッケージ名記載は公開前の仮置きのため対象外 (乖離として扱わない)
- concepts 追記は Theme default 定数 (iOS 17 個) のみ。Registry メソッド名・`PickerItem.init(text:subText:)`・`SettingsRootStore.preview`・`disconnectStore()`・`SettingsRootBuilder` / `KsSectionBuilder` は concepts 追記・skills 記載とも不要 (利用頻度が低い。起票も不要)
- skills への反映対象: concepts 記載済みの漏れ 33 サブ項目 + Theme default 定数。skills の更新は docs-refresh 経由 (concepts 追記後)
- `EffectiveStyle` の internal 化検討は別 change [[ios-effectivestyle-visibility]] として簡易起票済み
- Android / MAUI も同方向で対応する (iOS の基準を踏襲)

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- Theme default 定数 (iOS 17 / Android 13) と MAUI 書式系プロパティ群 (30 件) の concepts 追記先の最終確認 (core/styling か各 platform api か) — 実装時に決めてよい粒度
- MAUI の BindableProperty フィールド (`FooProperty` 161 宣言) は CLR プロパティ名からの導出規約として個別列挙しない方針 (規約1行の記載で足りる想定) — ユーザー確認前の暫定判断
- 3プラットフォームで入力系 Cell の漏れ方が同型だった件を docs-refresh スキルの再発防止観点 (公開 API 名の突き合わせチェック) として足すか
- 隣接課題を同 change で直すか: `skills/*/kssettingsview-maui/references/updates.md` の書き戻しプロパティ件数「10個」と仕様正 (`kasane/concepts/maui/api/maui-facade.md` は12件) のズレ
- 対象外 (別セッション対応中): `skills/*/kssettingsview-aiforms-migration/references/api-mapping.md` の「`PickerCell.SelectedCommand` は提供しない」誤記は別セッションで修正中 (2026-08-29 ユーザー申告)。実装時に対応済みかだけ確認する
- Theme default 定数の concepts 追記先 (core/styling か ios/api か — Android/MAUI にも同種の定数があるかで決まる)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S (理由: コード変更なし・公開 API 変更なし・完全可逆。内容は concepts への名前列挙の機械的追記 約60項目で、判断を要する仕分けは探索で完了済み。skills への反映は docs-refresh の守備範囲)

## 実装の段取り (2026-08-29 ユーザー合意済み。0. → 1. → 2. の順)

0. docs-refresh スキル改修 (`.agents/skills/docs-refresh/SKILL.md`) — 同梱決定 (2026-08-29):
   - API 名レベルの網羅検査を追加 (concepts のバッククォート付き API トークン → 対応 skills ファイルへの登場を突き合わせ。報告のみ / drift 所見扱いから開始)
   - 内容規約に「個数を地の文にハードコードしない (表の行数に語らせる。書くなら正本と照合)」を追記
   - 内容規約に「全称表現 (すべて・常に・必ず) は実装で全数確認できた場合のみ」を追記

1. concepts 追記: iOS Theme default 定数 17 / Android `DEFAULT_*` 定数 13 / MAUI 書式系プロパティ 30 (`CellBase` 12・`SettingsView` 17・`EntryCell.TextAlignment`)
2. docs-refresh 起動: concepts 記載済み漏れ全量 (iOS 33 / MAUI 7 / Android 約35) + 1. の追記分を skills 3プラットフォーム×2言語へ反映。隣接修正: Android SKILL.md の `CellHandle` 過剰記述、MAUI updates.md の件数「10個」→12。`SelectedCommand` 移行ガイド誤記は別セッション対応済みかを確認のみ
3. Android 追加除外 (iOS 基準の適用): `KsSettingsView.unbind`・`SettingsRootDsl` / `SectionScope` 型名・`withDSLIcon`・`CellTitleAlignment` 型名・`MauiAppBuilderExtensions` 型名は concepts 追記・skills 記載とも不要

## 調査結果の要点 (根拠)

- iOS 公開 API vs skills: 確実な漏れ 29 件 + 判断層 (Theme default 定数 17・DSL 型名等)。詳細は scout 応答 (counterpart-bridge responses: worker-scout-ios-skill-coverage-1)
- iOS 漏れ項目 vs concepts: サブ項目 57 件中、記載あり 33 / 未記載 24。未記載の主体は Theme default 定数 17 個 (worker-scout-ios-concepts-coverage-1)
- en / ja は行数・API トークン一致 — 抜けは両言語共通
- MAUI 公開 API vs skills: 未紹介の CLR プロパティ 34 件 (`CellBase` 書式系 12・`SettingsView` スタイル系 18・`Section.FooterView`・`PickerCell.SelectedCommand`・`EntryCell.TextAlignment`・`DatePickerCell.AndroidButtonColor`) + 型名 4 件 (`SettingsViewStyle`・`DatePickerUIStyle`・`PickerSelectionMode`・`MauiAppBuilderExtensions`)。逆方向の誤掲載なし。visibility 引き下げ候補なし。en/ja 共通 (worker-scout-maui-skill-coverage-1)
- MAUI 漏れ項目 vs concepts: 38 件中、記載あり 7 / 未記載 31。未記載の主体は書式系プロパティ名 (`CellBase` 12 件・`SettingsView` 17 件) + `EntryCell.TextAlignment` (MAUI 名)・`MauiAppBuilderExtensions` 型名。追記先は主に `kasane/concepts/maui/api/maui-facade.md` (worker-scout-maui-concepts-coverage-1)
- Android 公開 API vs skills: 名前掲載基準で 77 件。基準適用後の掲載対象は約 40 件 — iOS/MAUI と同型の入力系 Cell 漏れ (`textAlignment` / `onTextChanged` / Picker 系一式・`androidButtonColor`)、`Theme` の `DEFAULT_*` 定数 13、`SettingsRootDiff` 型+10 ケース、`SettingsRootStore.state` / `theme`、`KsSettingsView.unbind` / `applyDiff`、`KsAnyView.Compose` / `AndroidView`、`CellTitleAlignment`、Compose DSL スコープ型 5、`withDSLIcon`、Composable の `modifier` 引数。除外: Diff payload プロパティ 18 (ケース署名で自然にカバー)・`preview`・Registry 補助・`CustomCellEmptyContent`・Bridge 層。en/ja 共通 (worker-scout-android-skill-coverage-1)
- Android 逆方向の乖離: `skills/*/kssettingsview-android/SKILL.md` の「Cell の関数はすべて `CellHandle` を返す」は過剰 (`SettingsRootScope.section` / `SectionScope.cell` は `Unit` を返す)。同 change で修正する隣接課題
- 3プラットフォームで入力系 Cell の漏れ方が同型 — concepts→skills の初期変換時に系統的に落ちた可能性が高い
- Android 漏れ項目 vs concepts: 14 グループ中、記載あり (表記違い・部分含む) 13 / 完全欠落 1 (`DEFAULT_*` 定数 13 個)。入力系 Cell・`SettingsRootDiff`・Store・Compose 関連は concepts 記載済みで docs-refresh 対応可 (worker-scout-android-concepts-coverage-1)

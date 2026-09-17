# Exploration: android-accessory-view-late-insert-animation

## 課題 / 動機

(2026-09-17、`../ColorAnalyzer` の change `migrate-remaining-pages-to-kssettingsview` より越境の簡易起票。出典: `../ColorAnalyzer/kasane/changes/migrate-remaining-pages-to-kssettingsview/session.md`)

**症状** (Android 実機 Pixel 6a、KsSettingsView.Maui 0.1.0-beta.4 系、毎回再現):
`Section.FooterView` に MAUI View (Label) を置き、`FooterText` を持たない Section があるページへ遷移すると、遷移直後はフッターが表示されず下の Section が詰まった位置に出る。数百 ms 後にフッターが現れ、下の行がスライドダウンする。AiForms.Maui.SettingsView には無かった挙動。同じページの `CustomCell` は最初のフレームから正しく描画される。

**依頼元の調査で読んだ根拠** (起票時に実物で行の実在を確認し、行番号を実物に合わせた):

- 初回配信に footer が載らない: `maui/KsSettingsView.Maui/SettingsView.cs:984-989` で `Controller.Connect` (→ `SetRoot` 送信) が先、`AttachViews` が後。View の実体化は `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:80-124` の `Loaded` → `OnHostAttached` → `ApplyHostViews` まで遅れる (`maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:200-213` のコメント「実体化されていない位置は view なしになり…実体化の後に明示の更新で送り直される」)
- `FooterText` 未設定で FooterView だけの Section は、その時点で footer 行そのものが存在しない: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:1338-1366` の `flatten` が `shouldShowFooter` (`:1395-1397`、`hasAccessoryContent` で null / 空 text を内容なしとする) を満たさない footer 行を出さない
- Loaded 後の accessory 更新で footer 行が**挿入**として現れる: 同 `KsSettingsView.kt:981-1004` の `updateSectionAccessoryAndSubmit` → `mainListAdapter.submitList` (`KsSettingsListAdapter.kt:14` のとおり `AsyncListDiffer` による差分適用)
- 本体は change アニメーションだけを無効化している: 同 `KsSettingsView.kt:102` の `supportsChangeAnimations = false`。add / move は既定の `DefaultItemAnimator` が生きたまま
- accessory の再計測はブリッジ越しでディスパッチャ 1 tick 遅れる: `maui/KsSettingsView.Maui/Platforms/Android/KsAccessoryHostView.cs:43,74-78` の `MeasureInvalidated` → `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:691-700` (`_measureDirtySlots` に積んで `ScheduleFlush`) → `:1714` (`_dispatcher.Dispatch(Flush)`) → `:1726` の `Flush`。`CustomCell` の content は同 `:892` のとおり再計測コールバックが空 (`static () => { }`) で、この往復が無い
- 利用側で避ける公開設定は無い: `Section` に `FooterHeight` は無く (`maui/KsSettingsView.Maui/Section.cs:119,253` は `HeaderHeight` のみ)、ItemAnimator を差し替える公開 API も無い

**利用側の状況**: ColorAnalyzer は**本体の修正を先に行う方針** (オーナー決定 2026-09-17)。利用側では回避策 (`FooterText` への置き換え) をあえて入れず、`../ColorAnalyzer/ColorAnalyzer/Views/Others/AnalysisSettingsPage.xaml` の FooterView 2 箇所を修正確認のサンプルとして残している。同アプリのフィードバック画面 (FooterView に横スクロールの画像リスト) も移行予定で、同じ症状が出る見込み。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- **未探索 (簡易起票)** — 上記の根拠は依頼元の調査を行の実在まで確認しただけで、因果の検証・議論はしていない
- `HeaderView` (FooterText 相当の `HeaderText` なし) でも同じ遅延と挿入アニメーションが出るか (未検証)
- iOS で同種の症状が出るか (未検証)
- 遅れて見える主因の切り分け: 行の挿入 (add/move アニメーション) と、再計測の 1 tick 遅れのどちらが体感の大半か
- 直し方は未検討。挙がっている方向: 初回配信に accessory 行の枠を載せる / add・move アニメーションを切る / View の実体化を前倒しする 等 (どれも評価していない)
- core/ADR-0023 (内容の無い Header / Footer は行を生成しない) と、実体化前の View accessory の扱いとの関係

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S / M / L (理由)

未判定

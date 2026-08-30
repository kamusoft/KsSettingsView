# Exploration: fix-cell-icon-size-parity

統合起票 (2026-08-21、公開前トリアージ)。`fix-android-cell-icon-size-not-applied` と `fix-ios-cell-icon-intrinsic-width` を統合した。どちらも「`Theme.cellIconSize` は icon 列を**指定サイズの正方形**に揃える」という同じ契約の実現漏れで、証跡 (implement-modern-style の両 OS 比較) と視覚照合を 1 回で共有できる。

探索 (2026-08-22): コード読みで未決の論点 5 件をすべて解消した。

## 課題 / 動機

### Android: `cellIconSize` / `cellIconRadius` が描画に反映されない

`EffectiveStyle.effectiveIconSize` / `effectiveIconRadius` (`EffectiveStyle.kt:347` / `:357`) は static 解決関数として実装もテストもあるが、`EffectiveStyle` data class 自体に icon のフィールドが無く、`from()` からも呼ばれない (本体からの参照ゼロ)。`CellBaseLayout.kt:105` は `iconSize = 24dp` 固定で `iconView` を `ConstraintLayout.LayoutParams(iconSize, iconSize)` (`:142`) に構築し、角丸 clip も行わない。

iOS は `cellIconSize` / `cellIconRadius` を反映するため、同じ Theme 値 (例: 29 / 7) を渡しても Android だけ 24dp・角丸なしになる。implement-modern-style の Section 装飾デモでは両 OS に同値を渡しており、Android 実機で差が視認できる。

### iOS: 画像の intrinsic size が `cellIconSize` の制約に勝つ

`KsListCellBase.swift:201-204` で `iconImageView` の幅 / 高さ制約は `.defaultHigh` (750) で張られているのに対し、同 view の horizontal hugging / compression resistance は `.required` (1000、`:165-166`)。そのため SF Symbol のように字形ごとに幅が違う画像を `KsImage.systemName` で渡すと icon 列の幅が行ごとに変わり、title の開始位置が最大 9pt ほどずれる。同ファイル 14 行目の階層コメントは「Hugging=999, CCR=999」と書かれておりコードと食い違っている。

### 公開前に扱う理由

公開 Theme プロパティが Android で効かない・iOS で SF Symbols を渡すと揃わない、という状態で初回リリースすると、利用者向け Skills に書く `cellIconSize` の説明と実挙動が食い違う。直すとどちらの OS も見た目が変わる (指定が効くようになる) ため、リリース後より前に入れる方が利用者への影響が小さい。

出典: implement-modern-style の tasks 4.1 (iOS) / 4.2・6.2 (Android) で観測。`kasane/changes/archive/2026-08-20-implement-modern-style/ui/brief.md` の照合メモ。

## 検討した選択肢 (却下案と理由を含む)

### 非正方形画像と角丸の扱い (両 OS 共通契約)

現状は iOS `.scaleAspectFit`、Android `ImageView` 既定 `FIT_CENTER` で、どちらも既に正方形枠へ aspect fit している。枠に角丸をかけると、短辺側の透明余白が弧を吸収するため非正方形画像には角丸が効かず (余白 > radius)、radius が余白より大きいと画像の角が斜めに欠ける。

- **A: 角丸は正方形枠に対して適用する (採用)** — `cellIconRadius` の意味は「設定アプリ風の角丸バッジ」で、画像が正方形である前提の機能。iOS の現行挙動そのものであり、Android は outline を枠に張るだけで対称になる。非正方形では効かないことを契約として明記し、角丸を効かせたい icon は正方形で用意してもらう
- B: aspect fit 後の描画矩形に角丸を適用する (却下) — 両 OS で描画矩形を自前計算して mask / outline を毎 bind 更新する必要があり、iOS の既存の見た目も変わる。「非正方形に角丸をつけたい」需要が確認できておらずコストに見合わない
- C: aspect fill で正方形に切り取る (却下) — 角丸は常に効くが、利用者の画像を勝手に切り取る情報損失を伴い、iOS の aspect fit を捨てる破壊的変更になる

### Android の角丸 clip の手段

- **`iconView.clipToOutline = true` + `ViewOutlineProvider` (roundRect) (推奨)** — View の枠に対する clip なので A の契約と一致し、vector / bitmap を問わず効く
- drawable 側変換 (`RoundedBitmapDrawable` 等) (却下寄り) — bitmap 限定で vector drawable に効かない

### iOS の制約優先度

- **サイズ制約 999、horizontal hugging / CCR 750 (推奨)** — 999 > 750 で「サイズ指定 > intrinsic」、1000 > 999 で「UIStackView の非表示 (required の幅 0 制約) > サイズ指定」の順序になる
- サイズ制約 `.required` (却下) — `isHidden` 時に UIStackView が張る required の幅 0 制約と衝突し unsatisfiable constraints のログが出る

## 決定事項

- 公開前トリアージ (2026-08-21): **初回リリース前に対応**。旧 2 件を本 change に統合、旧ディレクトリは破棄
- 2026-08-22: **非正方形画像は `cellIconSize` の正方形枠に aspect fit で収め、`cellIconRadius` はその正方形枠に対して適用する** (案 A)。角丸を効かせたい icon は正方形で用意する、を契約として明記する
- 2026-08-22: Android の反映タイミングは追加経路不要。Theme 変更は `KsSettingsView.kt:702` の `notifyItemRangeChanged(PAYLOAD_THEME)` → rebind → `applyCellBaseLayout` に乗るため、bind 時 (`applyCellBaseLayout` の icon 解決部) で LayoutParams と outline を適用すれば足りる
- 2026-08-22: Android で icon を組むのは `buildCellBaseViews` の 1 箇所のみ (各 ViewHolder は `views.iconView` を参照するだけ。CustomCell は共通行の適用除外)。1 箇所の修正で全 Cell 種に効く
- 2026-08-22: `EffectiveStyle` (Android) に `iconSizeDp` / `iconRadiusDp` 相当のフィールドを追加し `from()` で解決する (既存 static 関数を呼ぶ)。iOS 側 `EffectiveStyle.iconSize` / `iconRadius` と対称になる
- 2026-08-22: `KsListCellBase.swift:14` の階層コメント (Hugging=999) は修正後の実値に合わせて直す
- 2026-08-22 (提案の相方レビュー後): **iOS の icon 枠制約は表示中 `.required`、icon 非表示時は制約を deactivate** する (相方案 1)。999/750 案は垂直 CCR と狭幅時の title CCR に負けるため却下
- 2026-08-22: **狭幅時の主行の配分は別 change に逃がさず本 change で iOS を既存契約へ揃える** (オーナー指示: 同じ領域はついでに直す)。iOS は現状 `valueLabel` CCR 250 < title CCR 1000 で valueText が先に省略され、概念文書 (title が先に省略・valueText は主行幅上限) と逆 = drift。EntryCell (title コンテンツ幅・フィールドが縮む) は据え置く
- 2026-08-22 (訂正): 移植元 AiForms を確認したところ、幅配分は**オリジナルの時点で iOS (valueText 先に省略) と Android (title 先に省略) が逆**で、それぞれ忠実に移植されていた。concepts の「iOS も同じ配分」が誤り。オーナー裁定で **B: title を守り valueText を省略する (iOS 側) に両 OS を統一** — 理由: title は開発者のラベル、valueText はユーザーデータで長くなりやすく、ラベルが残る方が設定画面として読める (iOS 設定アプリと同じ)。Android の既定配分を title `wrap_content` / valueText `0dp + weight 1` に入れ替え、iOS は無変更。→ core/ADR-0026 (proposed)。android/ADR-0002 の配分項目を置き換え
- 2026-08-22: icon 枠について、オリジナル iOS は幅制約が required (既定) で高さだけ 999。移植時に両方 750 へ落としたのが SF Symbols バグの直接原因で、「表示中は required」はオリジナルへ戻す方向
- 2026-08-22: **無効値の契約** — icon size は正の有限値のみ有効、radius は 0 以上の有限値のみ有効。無効値は未指定として次の段へ解決 (既存の `rowHeight > 0` / `cellTitleFontSize > 0` パターン)。半辺超えは clamp しない
- 2026-08-22: proposal の `domain:` は `cross` (複数 platform に触るため。concepts/rules.md)
- 修正後は `concepts/core/styling/style-resolution.md:47` の「Android は反映を保証しない」但し書きを蒸留で削除し、A の契約を追記する (ksn-distill の作業)
- サンプルの `SampleIconBadge` (色地の正方形バッジ生成) は本体修正後も「色地バッジ」というデザイン選択として残す。回避策ではなくなるだけで削除は不要

## ADR 候補

- 作成済み: core/ADR-0026 (status: accepted、2026-08-22 承認) 「主行の幅配分は title を守り valueText を省略する (両 platform)」— B 採用、A (iOS を Android へ) / C (platform 差容認) 却下。android/ADR-0002 の配分項目を置き換える
- 作成済み: core/ADR-0025 (status: accepted、2026-08-22 承認) 「Cell icon の角丸は aspect fit の正方形枠に対して適用する」— 案 A 採用、B / C 却下。両 platform をまたぐ公開契約 `cellIconRadius` の意味論を固定するため、境界を越える・将来を制約する決定に該当

## 未決の論点

なし (探索で 5 件すべて解消。提案の相方レビューで出た iOS 優先度・無効値・幅配分の 3 点も決定済み)。

## UI 素材

なし (iOS の既存挙動 = 「指定サイズどおりに揃う」が正。照合対象は `kasane/changes/archive/2026-08-20-implement-modern-style/ui/verification/` の両 OS スクリーンショットが流用可能)

## 変更級の推奨: M (理由)

それぞれは局所修正・公開 API 変更なしで S 相当だが、2 platform を同時に触り、`cellIconSize` / `cellIconRadius` を指定していた利用者には見た目が変わるため、視覚確認 (process L-003) と両 OS 比較の証跡が必要。両 OS 共通の契約 (案 A) をデルタスペックに書いて verify で担保したい。迷ったら 1 段上で M。

## 関連ファイル

- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt` (`buildCellBaseViews` の `iconView` 24dp 固定 LayoutParams、`applyCellBaseLayout` の icon 解決部)
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt` (`effectiveIconSize` / `effectiveIconRadius` — data class にフィールド無し・`from()` 未参照)
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:702` (Theme 変更の rebind 経路)
- `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` (`iconWidthConstraint` / `iconHeightConstraint` の優先度、hugging / compression 優先度、14 行目の階層コメント)
- `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:102-103` (iOS 側の `iconRadius` / `updateIconSize` 適用点 — 対称化の参照先)
- `kasane/concepts/core/styling/style-resolution.md:47` (蒸留で更新する但し書き)
- 出典: `kasane/changes/archive/2026-08-20-implement-modern-style/` (tasks 4.1 / 4.2 / 6.2、ui/brief.md の Android 照合メモ)

# 検証結果: fix-cell-icon-size-parity (001 回目)

**日付**: 2026-08-23
**判定**: VALID

デルタスペック 2 capability / Requirement 6 / Scenario 34 (Android 19 + iOS 15) をすべて実装・テストと突き合わせた。❌ は 0 件。

---

## 対応表: settings-view-android-ui

### Requirement: Cell icon の正方形枠への実効 icon size の反映 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Theme.cellIconSize が icon 枠に反映される | `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:472` → `:534` `applyIconFrame` | `CellIconFrameTest.kt:125` `Theme の cellIconSize が icon 枠の一辺になる` | ✅ 一致 |
| CellStyle.iconSize は Theme より優先される | `EffectiveStyle.kt:360` `effectiveIconSize` | `CellIconFrameTest.kt:141` / `EffectiveStyleResolutionTest.kt:158` | ✅ 一致 |
| 未指定なら iOS と同じ生値の既定枠になる | `EffectiveStyle.kt:363` (`Theme.DEFAULT_CELL_ICON_SIZE_DP_VALUE`) | `CellIconFrameTest.kt:155` (既定生値 24 / 0 を直接アサート) | ✅ 一致 |
| Theme 変更で表示中の行の枠が更新される | `CellBaseLayout.kt:472` (bind ごとに再評価) | `CellIconFrameTest.kt:209` (`KsSettingsView.theme` 差し替え → 同一 View で 24→48dp) | ✅ 一致 |
| 非正方形画像でも枠は正方形のまま | `CellBaseLayout.kt:539-546` (`FIT_CENTER` + LayoutParams を解決済み size の正方形へ) | `CellIconFrameTest.kt:178` (intrinsic 120×24 の drawable) | ✅ 一致 |
| icon のない Cell の配置は変わらない | `CellBaseLayout.kt:495-498` (`icon == null` → `GONE`) | `CellIconFrameTest.kt:196` (title 左端 = root padding) | ✅ 一致 |
| 無効な icon size は未指定として次の段へ解決する | `EffectiveStyle.kt:385` `isValidIconSize` | `EffectiveStyleResolutionTest.kt:214` / `:225` / `:236` | ✅ 一致 |
| 狭幅でも icon 枠は縮まない | `CellBaseLayout.kt:534` (icon は `contentRow` の外・固定 LayoutParams) | `CellRowWidthAllocationTest.kt:450` `狭幅でも icon 枠は縮まず title が末尾省略される` | ✅ 一致 |

### Requirement: Cell icon の正方形枠に対する角丸 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Theme.cellIconRadius で枠が角丸に clip される | `CellBaseLayout.kt:548-555` + `:571` `IconFrameOutlineProvider` | `CellIconFrameTest.kt:264` (outline の radius と矩形が枠と一致) | ✅ 一致 |
| CellStyle.iconRadius は Theme より優先される | `EffectiveStyle.kt:372` `effectiveIconRadius` | `CellIconFrameTest.kt:280` / `EffectiveStyleResolutionTest.kt:194` | ✅ 一致 |
| 角丸未指定なら clip しない | `CellBaseLayout.kt:556-562` (else で `clipToOutline = false`) | `CellIconFrameTest.kt:300` | ✅ 一致 |
| 角丸は枠に対してかかり画像の描画矩形には追従しない | `CellBaseLayout.kt:572` (`outline.setRoundRect(0, 0, view.width, view.height, radiusPx)`) | `CellIconFrameTest.kt:310` (120×24 drawable でも outline は 40dp 正方形) | ✅ 一致 |
| 再 bind で radius の変更と解除が反映される | `CellBaseLayout.kt:549-555` (radius 変化時に provider インスタンスを差し替え) | `CellIconFrameTest.kt:336` (12dp → 4dp → なし) | ✅ 一致 (ミューテーション実測で検出力を確認。review-002 参照) |
| 無効な radius は未指定として次の段へ解決する | `EffectiveStyle.kt:393` `isValidIconRadius` | `EffectiveStyleResolutionTest.kt:247` / `:258` / `:269` (0dp は有効な指定) | ✅ 一致 |

### Requirement: 主行の幅配分は title を守り valueText を省略する (iOS と同一契約) (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 長い valueText は省略され title は全文残る | `CellBaseLayout.kt:185-218` (既定配分の入れ替え) + `:466` `applyTitleWidthMode` | `CellRowWidthAllocationTest.kt:309` (実 ellipsis + icon / accessory の幅不変) | ✅ 一致 |
| 主行幅を超える title は上限で省略され valueText は残り幅になる | `CellBaseLayout.kt:189-200` (title は `wrap_content` + `ellipsize END`) | `CellRowWidthAllocationTest.kt:372` | ✅ 一致 |
| 行内 trailing がない Cell では title が主行の全幅を使う | `CellBaseLayout.kt:375` `applyTitleWidthMode` / `ButtonCellViewHolder.kt:121` | `CellRowWidthAllocationTest.kt:281` (全幅かつコンテンツ幅より広い) / `:694` (ButtonCell 中央揃え) | ✅ 一致 |
| 同じ行で valueText の有無が切り替わっても配分が追随する | `CellBaseLayout.kt:466-469` (bind ごとに評価) | `CellRowWidthAllocationTest.kt:409` (あり→なし→あり) | ✅ 一致 |
| EntryCell では title がコンテンツ幅を維持し入力フィールドが縮む | `CellBaseLayout.kt:352-353` `addFillingInlineTrailing` + `hasFillingInlineTrailing` | `CellRowWidthAllocationTest.kt:133` / `:203` (残り幅 0 の境界まで) | ✅ 一致 |

---

## 対応表: settings-view-ios-ui

### Requirement: Cell icon 枠の寸法が画像の intrinsic size に依存しない (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| intrinsic 幅が異なる SF Symbols でも icon 列幅が揃う | `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:205-211` (required の正方形制約) + `CellBaseLayout.swift:183` `normalizedIconImage` | `CellIconFrameTests.swift:25` (字形差が実在することを先にアサート) | ✅ 一致 |
| 枠より大きい intrinsic size の画像でも枠は解決済みサイズのまま | `KsListCellBase.swift:166-171` (Hugging / CCR を両軸とも低く) | `CellIconFrameTests.swift:65` | ✅ 一致 |
| CellStyle.iconSize は Theme より優先される | `EffectiveStyle.swift:190` `effectiveIconSize` | `CellIconFrameTests.swift:82` / `EffectiveStyleResolutionTests.swift:121` | ✅ 一致 |
| Theme 変更で表示中の行の枠が更新される | `CellBaseLayout.swift:105` (bind ごとに `showIcon(size:)`) | `CellIconFrameTests.swift:97` (`applyTheme(_:)` 経由 24→40) | ✅ 一致 |
| icon のない Cell では枠の制約が無効化される | `KsListCellBase.swift:235` `hideIcon()` | `CellIconFrameTests.swift:119` (`isActive == false` を直接観測) | ✅ 一致 |
| icon なし → icon ありの再 bind で枠が戻る | `KsListCellBase.swift:223` `showIcon(size:)` / `:360` `prepareForReuse` | `CellIconFrameTests.swift:136` / `:155` (`prepareForReuse` 経由も) | ✅ 一致 |
| 無効な icon size は未指定として次の段へ解決する | `EffectiveStyle.swift:210` `isValidIconSize` | `CellIconFrameTests.swift:176` (レイアウト結果まで) / `EffectiveStyleResolutionTests.swift:170` | ✅ 一致 |

### Requirement: Cell icon の正方形枠に対する角丸 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 角丸は枠に対してかかり画像の描画矩形には追従しない | `CellBaseLayout.swift:104` (`layer.cornerRadius` + `clipsToBounds`) | `CellIconFrameTests.swift:200` (非正方形と正方形で cornerRadius が一致) | ✅ 一致 |
| 角丸未指定なら clip しない | `EffectiveStyle.swift:203` (既定 `Theme.defaultCellIconRadius` = 0) | `CellIconFrameTests.swift:225` | ✅ 一致 |
| 無効な radius は未指定として次の段へ解決する | `EffectiveStyle.swift:218` `isValidIconRadius` | `CellIconFrameTests.swift:234` / `EffectiveStyleResolutionTests.swift:192` / `:214` | ✅ 一致 |

### Requirement: 主行の幅配分は title を守り valueText を省略する (Android と同一契約) (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 長い valueText は省略され title は全文残る | 現行の Hugging / CCR 優先度を維持 (`KsListCellBase.swift:180-186`) | `CellRowWidthAllocationTests.swift:28` (icon / accessory の幅不変・はみ出しなしまで) | ✅ 一致 |
| 主行幅を超える title は上限で省略され valueText は残り幅になる | 同上 | `CellRowWidthAllocationTests.swift:73` | ✅ 一致 |
| 行内 trailing がない Cell では title が主行の全幅を使う | 同上 | `CellRowWidthAllocationTests.swift:105` | ✅ 一致 |
| EntryCell では title がコンテンツ幅を維持し入力フィールドが縮む | 同上 | `CellRowWidthAllocationTests.swift:169` | ✅ 一致 |
| 狭幅でも icon 枠は縮まない | `KsListCellBase.swift:205-211` (required のサイズ制約) | `CellRowWidthAllocationTests.swift:199` (幅 160 で icon 44 のまま title 省略) | ✅ 一致 |

iOS spec は「iOS は現行の優先度のままこの契約を満たしており、本 Requirement はそれをテストで固定する」と明記しているため、実装差分なしでテストのみが追加されているのは仕様どおり。

---

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の虚偽チェック | **なし**。全 23 項目 `[x]`。各項目を対応表と突き合わせ、成果物が存在しない項目は無かった。2.3 は本文で「削除」と明記された項目 (iOS は現行優先度のまま契約を満たすため実装変更なし) で、2.7 のテストが代替を担っている。2.1 は入口名を `setIconVisible(_:size:)` と**例示**しており、実装が `showIcon(size:)` / `hideIcon()` の 2 入口に分かれていることは要求違反にならない (「表示/非表示を集約し、非表示時に制約を無効化する」という要求は満たしている) |
| 逆流検査 (足場の書き換え) | **なし**。`git status` で `specs/` `proposal.md` `exploration.md` に作業ツリーの変更なし。これらは `73c008e` で確定した内容のまま。変更されている足場は `tasks.md` (チェック消化) と `ui/brief.md` (視覚照合の結果の記入。ksn-ui の所定の書き込み先) のみ |
| 未記録乖離 | **なし**。対応表に ❌ が無く、diff にあって Scenario に対応しない変更 (`ButtonCellViewHolder` のボタンスタイル分岐 / `buildCellBaseViews` の初期値リテラル・周辺コメント / iOS `normalizedIconImage` / iOS `prepareForReuse` の `cornerRadius = 0` / `.gitignore`) はすべて `deviation.md` に `[付随修正]` として記録済み |
| 付随修正の同梱条件 | 5 件とも本務で触るファイル内または本務の変更が直接開けた穴の修復。`.gitignore` の 1 行だけがファイル外だが、tasks 2.8 が指定した証跡ファイルを追跡対象にするための最小変更で、`git check-ignore -v` で否定パターンが効いて `ios-test-constraints.log` が untracked (= コミット可能) になることを確認した |
| UI 変更の承認モック記録 | `ui/brief.md` に「承認モック: `mock/approved.png` を採用 (提案時に合意: 2026-08-22)」と記録あり。合意済み妥協 4 件も記載あり |
| 視覚証跡の実在 | `ui/verification/` の PNG 8 点 + ログ 1 点がすべて実在し、brief.md が引用するファイル名と一致 (review-001 で指摘された `-after.png` の不一致は解消済み) |
| テスト全件成功 (実行して確認) | Android: `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL、JUnit XML 集計で **tests=2582 / failures=0 / errors=0 / skipped=0**。iOS: `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → `** TEST SUCCEEDED **`、**Executed 581 tests, with 0 failures**、`Unable to simultaneously satisfy constraints` **0 件** |
| 証跡ログの同定可能性 | `ui/verification/ios-test-constraints.log` が併記する検証対象ソース 6 件の SHA-256 を、現在の作業ツリーに対して `shasum -a 256` で再計算し **6 件すべて一致**することを確認した (このログが提出コードに対応することが第三者に再検証できる) |

---

## 判定

**VALID** — 全 34 Scenario が「✅ 一致」、虚偽チェックなし、逆流なし、未記録乖離なし、両 platform のテスト全件成功。蒸留・アーカイブへ進める状態にある。

品質面の指摘 (Minor 1 / Suggestion 1) は `review-002.md` を参照 (いずれもコード契約ではなくコメント・証跡記述の精度に関するもので、本検証の判定には影響しない)。

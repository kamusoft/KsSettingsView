# Verify 001: implement-modern-style

- 検証日: 2026-08-20
- 対象: 未コミット working tree (HEAD `81bf2c4` + 未追跡新規ファイル)
- 対象能力: settings-view-ios-ui / settings-view-android-ui / samples-ios / samples-android
- 判定: **INVALID** (❌ 1件 — Android の Scenario 1件にテストが存在しない)

凡例: ✅ 一致 / ⚠️ deviation.md に記録済みの合意差分 / ❌ 欠落・乖離

---

## 1. settings-view-ios-ui

パスは `ios/Sources/KsSettingsViewUI/` と `ios/Tests/KsSettingsViewUITests/` からの相対。

### Requirement: Theme の Section 装飾4属性

| Scenario / 契約条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 4属性の公開・値等価性参加 | `Theme.swift:139-147`(宣言) / `:252-255`(`==`) | `SectionBoxDecorationTests.swift:127` `test_4属性の既定はnilで未指定を表す` / `:216` `test_4属性はThemeの値等価性に参加する` | ✅ |
| 未指定の Theme で Modern を表示する | `SectionBoxMetrics.swift:30-38`(既定値 top22/lead16/bottom0/trail16・radius26・border 0/透明) / `:46-69` | `:135` `test_未指定のModernはライブラリ既定の余白と角丸へ解決しボーダーは実効0` / `:231` `test_未指定のThemeでModernを表示すると既定の余白と角丸で箱が描かれボーダーは出ない` | ✅ (design Decision 6 の確定値と一致) |
| 指定値が箱の描画へ反映される | `SectionBoxLayout.swift:126-131` / `SectionBoxDecorationView.swift:37-41` | `:246` `test_指定値が箱の描画へ反映される` / `:269` `test_箱の塗り色はcellBackgroundColorから解決する` | ✅ |
| 実行時の Theme 変更が装飾へ反映される (identity 維持) | `KsSettingsViewController.swift:405-413`(`applyTheme`) / `:794-807`(`refreshSectionBoxAppearance`) / `SectionBoxAttributes.swift:38-45`(装飾値を `isEqual` に含める) | `:278` `test_実行時のTheme変更が装飾へ反映されidentityは維持される` | ✅ |
| sectionMargin は Header / Footer を含む Section 単位を包む | `KsSettingsViewController.swift:590`+`:723-726`(`interSectionSpacing`) / `:735-742`(list 端) / `:623-628`+`:664-691`(水平は section と supplementary 双方) | `:305` `test_sectionMarginはHeaderとFooterを含むSection単位を包む` | ⚠️ deviation 3行目 (margin の複合方式)・4行目 (Root H/F の内側配置。`:347`/`:386`/`:558`/`:574` が内側配置を検証) |
| 負の成分は 0 として扱う / cornerRadius は描画時 clamp | `SectionBoxMetrics.swift:49-68`(`max(0,…)`) / `:75-84`(`clampedCornerRadius`) | `:161` `test_負の寸法は0として扱う` / `:173` `test_負の値を持つThemeでもModernの表示が破綻しない` / `:192`・`:199` clamp 2件 | ✅ |
| Modern は新たな色既定を導入しない (SHALL NOT) | `SectionBoxLayout.swift:31`+`KsSettingsViewController.swift:717`(箱色は `cellBackgroundColor`)。`SectionBoxMetrics` に色定数は border の `.clear` のみ | `:269` `test_箱の塗り色はcellBackgroundColorから解決する` / `:957` `test_描画結果で箱とボーダーと下地が観察できる` | ✅ |

### Requirement: Modern の Section 箱描画

| Scenario / 契約条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| Header / Footer は箱の外に置かれる | `SectionBoxLayout.swift:139-150`(`cellRowsFrame` は item 属性のみで箱を作る) / `:120-133` | `:635` `test_HeaderとFooterは箱の外に置かれる` / `:659` `test_RootHeaderは箱に含まれない` | ✅ |
| 構造変更後も箱が Cell 範囲に追従する | `SectionBoxLayout.swift:711-714`(`cellCountInSection` は最新 visible projection を読む) / `KsSettingsViewController.swift:2363`(diff 後の clip 再適用) | `:683` `test_構造変更後も箱がCell範囲に追従する` / `:764`・`:780`(挿入・削除で隣接 Cell の clip が更新される) | ✅ |
| 可視 Cell が0件の Section は箱を生成しない | `SectionBoxLayout.swift:124`+`:140-141`(count 0 で `nil`) | `:702` `test_可視Cellが0件のSectionは箱を生成しない` | ✅ |
| `.insetGrouped` は使用しない (SHALL NOT) | `KsSettingsViewController.swift:994-998`(両 style とも `.plain`) | `KsSettingsViewStyleTests.swift:28` `test_modernもplainAppearanceを使いinsetGroupedを使わない` (旧 `== .insetGrouped` テストを置換済み) | ✅ |

### Requirement: 箱と Cell 背景の合成

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| ボーダーが Cell 背景に隠れない | `SectionBoxCellClip.swift:100-113`(内側形状の mask) / `KsCellViewSupport.swift:98-121` / `KsListCellBase.swift:305-309`・`CustomCellView.swift:91-95`(bounds 追従) | `:889` `test_ボーダーはCell背景に隠れない` / `:957` `test_描画結果で箱とボーダーと下地が観察できる` (実画素) | ⚠️ deviation 8行目 (zIndex 不可 → Cell 側 mask で実現。観察契約は充足) |
| 先頭 Cell の背景が角丸からはみ出さない | 同上 `SectionBoxCellClip.swift:51-93` | `:912` `test_先頭Cellの背景が角丸の外へはみ出さない` / `:803`・`:821`・`:857` | ✅ |
| 押下背景も箱形状に収まる | 同上 (mask は layer 単位なので `backgroundConfiguration` の押下色にも効く) | `:930` `test_押下背景も箱形状に収まる` | ✅ |

### Requirement: Modern の separator 規則

| Scenario / 契約条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 箱の上下端に separator が出ない | `KsSettingsViewController.swift:915-916`(既定 hidden) / `:930-932`(末尾は return) | `:1045` `test_Modernは箱の上下端にseparatorを描かない` | ✅ |
| 単一 Cell の Section に separator が出ない | 同上 (単一 Cell は `isLast`) | `:1098` `test_Modernの単一CellのSectionにseparatorが出ない` | ✅ |
| 中間は leading / trailing へ同量 inset・箱の内側基準・icon 非依存・色は Theme | `:933-939`(`inset = titleLeading + borderWidth`) / `:912`(色) / `:965-970`(icon 非依存の固定 16pt) | `:1065`・`:1084`・`:1108` | ✅ |

### Requirement: Classic への sectionMargin 上下適用

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未指定なら Classic の外観は従来と一致する | `SectionBoxMetrics.swift:34`(classic 既定 0) / `:55-62` | `:1140` `test_Classicは未指定なら従来と同じ間隔で並ぶ` | ✅ |
| 上下成分だけが効く (leading/trailing 無視・余白に backgroundColor) | `SectionBoxMetrics.swift:51-53`(classic は水平 0) / `KsSettingsViewController.swift:723-726`・`:735-742` | `:1157` `test_Classicはmarginの上下成分だけが効く` / `:150` `test_Classicは指定してもleadingとtrailingを無視する` / `:1120` `test_Classicのseparator規則は変わらない` | ✅ |

### Requirement: style 切替の整合

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Classic から Modern への切替 | `KsSettingsViewController.swift:48-55`(`style` didSet → `rebuildLayout` + `reconfigureVisibleCells`) / `:1005-1015` | `:1179` `test_ClassicからModernへの切替で内容と順序は変わらない` / `:1199` `test_ModernからClassicへの切替で装飾が外れる` | ✅ |

---

## 2. settings-view-android-ui

パスは `android/ks-settingsview-ui/src/{main,test}/kotlin/jp/kamusoft/kssettingsview/ui/` からの相対。

### Requirement: Theme の Section 装飾4属性

| Scenario / 契約条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 4属性の公開・値等価性 (PaddingValues 委譲) | `Theme.kt:111-114` (data class) | `ThemeTest.kt:147`・`:156`・`:171`・`:186` (4件) | ✅ |
| 未指定の Theme で Modern を表示する | `SectionBoxMetrics.kt:56-69`(既定 start/end 16dp・top/bottom 12dp・radius 12dp・border 0/透明) / `:84-117` | `SectionBoxMetricsTest.kt:36` / `ModernSectionDecorationTest.kt:519` `ボーダー未指定なら描画しない` | ✅ (design Decision 6 と一致) |
| 指定値が箱の描画へ反映される | `ModernSectionDecoration.kt:90-106`(塗り) / `:215-229`(ボーダー) / `:297-298`(余白) | `SectionBoxMetricsTest.kt:63` / `ModernSectionDecorationTest.kt:196`・`:491` | ✅ |
| 実行時の Theme 変更が装飾へ反映される | `KsSettingsView.kt:690-714`(`applyThemeInternal` → `applyDecoration`) / `ModernSectionDecoration.kt:234-243`(描画のたびに再解決) | `ModernSectionDecorationTest.kt:591` `実行時の Theme 変更で角丸が再解決される` | ✅ (identity は `notifyItemRangeChanged` のみで `submitList` を伴わないため構造的に維持。テストは角丸の再解決のみを明示検証) |
| sectionMargin は Header / Footer を含む Section 単位を包む | `SectionUnitMargin.kt:35-57` / `ModernSectionDecoration.kt:66-88` | `ModernSectionDecorationTest.kt:259`・`:298`・`:337` | ⚠️ deviation 4行目 (Root H/F の内側配置。`:298` が検証) |
| 負の成分は 0 / cornerRadius は描画時 clamp | `SectionBoxMetrics.kt:94`(`max(0f,…)`) / `:47-51` | `SectionBoxMetricsTest.kt:118`・`:137`・`:148` / `ModernSectionDecorationTest.kt:562` | ✅ |
| Modern は新たな色既定を導入しない (SHALL NOT) | `ModernSectionDecoration.kt:95`(`cellBackgroundColor`) / `:122`(`backgroundColor`) | `ModernSectionDecorationTest.kt:443` `角丸の外へ出た Cell 背景は下地色で覆われる` | ✅ |

### Requirement: Modern の Section 箱描画

| Scenario / 契約条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| Header / Footer は箱の外に置かれる | `ModernSectionDecoration.kt:255-316`(`collectSectionBoxes` は `CellListItem.CellRow` のみ集計) | `ModernSectionDecorationTest.kt:177` `箱は Section の Cell 行だけを覆い Header と Footer は箱の外に置かれる` | ✅ |
| **H/F 行を水平 inset 対象にしない (SHALL NOT)** | `ModernSectionDecoration.kt:80-87`(Section 行すべてに水平 inset を適用 = spec 文言と逆) | `ModernSectionDecorationTest.kt:207` `水平 inset は Section Header と Footer 行にも入り箱の中の Cell と水平で揃う` | ⚠️ deviation 5行目 (オーナー判断で SHALL NOT を上書き。iOS と共有契約に一致させる) |
| Root Header / Footer は装飾対象外 (SHALL NOT) | `ModernSectionDecoration.kt:83`+`:322-328`(`KsSettingsListAdapter` 以外は inset 0) / `SectionUnitMargin.kt:44-45` | `ModernSectionDecorationTest.kt:298`(Root 行自体は余白・inset を持たない) | ✅ |
| **構造変更後も箱が Cell 範囲に追従する** | `ModernSectionDecoration.kt:255-316`(描画のたびに現在の child から再集計するため構造的に追従) | **該当テストなし** — `ModernSectionDecorationTest.kt` に `SettingsRootDiff` / Cell 挿入を扱うケースが存在しない (`ApplyDiffTest.kt` 等の diff テストは装飾を観測しない) | ❌ |
| 可視 Cell が0件の Section は箱を生成しない | `ModernSectionDecoration.kt:274`(CellRow 以外は skip) / `:295` | `ModernSectionDecorationTest.kt:239` | ✅ |

### Requirement: 箱と Cell 背景の合成

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| ボーダーが Cell 背景に隠れない | `ModernSectionDecoration.kt:116-134`(`onDrawOver` の最後にボーダー) / `:215-229` | `ModernSectionDecorationTest.kt:491` `ボーダーは separator と Cell 背景より後に描かれ最前面に来る` | ⚠️ deviation 6行目 (design の onDraw → onDrawOver へ変更) |
| 先頭 Cell の背景が角丸からはみ出さない | `ModernSectionDecoration.kt:143-164`(`clipOutPath` + 下地色で被覆) | `ModernSectionDecorationTest.kt:443` | ⚠️ deviation 7行目 (角丸 clip の実現手段 = 被覆方式) |
| 押下背景も箱形状に収まる | 同上 (被覆は Cell 描画後に走るため押下背景にも効く) | `ModernSectionDecorationTest.kt:468` `押下中の Cell でも角の被覆は箱の外接矩形を覆う` | ✅ |

### Requirement: 長い Section の箱端描画

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| viewport より長い Section のスクロール中に偽の箱端が出ない | `ModernSectionDecoration.kt:286-312`(`hasRealTop`/`hasRealBottom` + `overhang` で画面外へ延長) | `ModernSectionDecorationTest.kt:528` | ✅ |

### Requirement: Modern の separator 規則

| Scenario / 契約条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 箱の上下端に separator が出ない | `ModernSectionDecoration.kt:193-195`(Section 末尾 Cell は skip。先頭は「前 Cell の下端」を描かないので出ない) | `ModernSectionDecorationTest.kt:362` / `:381`(単一 Cell) | ✅ |
| 背景色付き Cell でも separator が見える | `ModernSectionDecoration.kt:116-134`(`onDrawOver` で描く) | `ModernSectionDecorationTest.kt:417` | ✅ |
| 左右同量 inset・箱の内側基準・icon 非依存・色/1物理 pixel | `:180`(`16dp + borderWidth`) / `:198-208` / `:335-338` | `ModernSectionDecorationTest.kt:387`・`:405` | ✅ |

### Requirement: Classic への sectionMargin 上下適用

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未指定なら Classic の外観は従来と一致する | `ClassicSectionDecoration.kt:143-165` / `SectionBoxMetrics.kt:92`(classic 既定 0) | `ClassicSectionDecorationTest.kt:163` | ✅ |
| 上下成分だけが効く | `SectionBoxMetrics.kt:99-100`(classic は水平 0) / `ClassicSectionDecoration.kt:164`(`outRect.set(0, top, 0, bottom)`) | `ClassicSectionDecorationTest.kt:179`・`:214` / `SectionBoxMetricsTest.kt:97` | ✅ |

### Requirement: style 切替の整合

| Scenario / 契約条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| Classic から Modern への切替 | `KsSettingsView.kt:913-923`(`applyDecoration`) | `ModernSectionDecorationTest.kt:611` / `KsSettingsViewStyleTest.kt:39` | ✅ |
| Compose ラッパからの style 指定・切替でも同じ挙動 | 既存の `KsSettingsViewComposable` 経由 | `KsSettingsViewComposeTest.kt:385` `Compose ラッパの style 切替で装飾が切替後の規則で再評価される` (offset の実値で観測) | ✅ |

---

## 3. samples-ios / samples-android

### Requirement: style と Section 装飾のデモ (両能力で同文)

| Scenario / 契約条項 | 実装 (iOS) | 実装 (Android) | 証跡 | 状態 |
|---|---|---|---|---|
| style を切り替えて見比べる | `samples/ios/KsSettingsViewSample/SectionDecorationDemoView.swift:28`+`:39` / `SectionDecorationDemoControls.swift:17-21`(segmented) / `SampleScreen.swift`(メニュー登録) | `samples/android/.../SectionDecorationDemoScreen.kt:36`+`:54-57` / `SectionDecorationDemoControls.kt` / `SampleScreen.kt:25`・`:50` | `ui/verification/ios-classic-standard.png` / `ios-modern-standard.png` / `android-classic-standard.png` / `android-modern-standard.png` | ✅ |
| H/F 付き Section・icon 付き Cell・単一 Cell の Section を含む | `SectionDecorationDemoView.swift:41-93` (4 Section: icon 付き / H+F 付き / 単一 Cell / ボーダー観察用) | `SectionDecorationDemoScreen.kt:59-113` (同構成・同文言) | 同上 | ✅ (tasks 4.3 の一字一句一致を確認 — Section 構成・見出し・Footer 文・Cell タイトル・プリセット名がすべて一致) |
| 4属性の変更を確認する (プリセット切替) | `SectionDecorationPreset.swift:17-53` (既定 / 余白広め・角丸小 = margin 32・radius 8 / ボーダーあり = width 2) | `SectionDecorationPreset.kt:18-43` (同値) | `ios-modern-wide-margin.png` / `ios-modern-bordered.png` / `android-modern-wide-margin.png` / `android-modern-bordered.png` / `compare-mock-vs-*.png` | ✅ |

---

## 4. 追加検査

### tasks.md の虚偽チェック

全 17 タスクが `[x]`。対応表と突き合わせた結果、**未実装のまま完了扱いになっているタスクはない**。

- 1.1 スパイク: `kasane/changes/implement-modern-style/spike/` + `samples/ios/KsSettingsViewSample/SectionDecorationSpikeView.swift` (`SampleScreen.verifications` に登録され、parity 対象の `demos` からは除外) ✅
- 2.1 「`section.contentInsets` に margin を反映」→ 水平のみ `section.contentInsets`、上下は `interSectionSpacing` / `contentInset`。deviation 3行目に記録済み ⚠️
- 3.2 「ボーダー描画 (onDraw) を追加」→ 実装は `onDrawOver`。deviation 6行目に記録済み ⚠️
- 4.3 / 6.1 / 6.2: `ui/brief.md` に照合結果・許容差・証跡一覧が追記済み。ただし brief.md 自身が「オーナーの最終承認は未取得 (証跡を提出済み)」と明記しており、**モック承認ゲートの最終クローズはオーナー判断待ち**

### 逆流検査

`git diff HEAD --stat -- specs/ proposal.md design.md` は空。`git log` 上も `81bf2c4` (提案化コミット) 以降にこれらの更新はない。実装期間中に書き換えられた足場アーティファクトは `tasks.md` (チェック更新) と `ui/brief.md` (照合メモ追記) のみで、いずれも許容範囲。**逆流なし** ✅

### テスト実行結果

| 対象 | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17' test` | **885 passed / 0 failed**。`SectionBoxDecorationTests` 53件を含む全ケースが実行・成功 (xcresult で確認) |
| Android ks-settingsview-ui | `./gradlew :ks-settingsview-ui:testDebugUnitTest` | **905 passed / 0 failed / 0 skipped** |
| Android ks-settingsview-compose | `./gradlew :ks-settingsview-compose:testDebugUnitTest` | **111 passed / 0 failed / 0 skipped** |

### UI 変更の確認

- `ui/mock/approved.png` (案 A) が承認済みモックとして brief.md に記録され、不採用案 (variant-b) も明記 ✅
- 合意済み妥協 (4番目 Section のボーダーが全 Section へ一括で効く / アイコン字形 / 端末幅・行高 / 既定寸法の platform 差) が brief.md に列挙済み ✅
- brief.md に本 change の範囲外として記録された既存欠落 1件 (Android で `Theme.cellIconSize` / `cellIconRadius` が描画に反映されない) — 本 change の spec 対象外につき本検証の判定には含めない

---

## 5. ❌ の一覧と見立て

### ❌-1: [android] Scenario「構造変更後も箱が Cell 範囲に追従する」にテストが存在しない

- **spec**: `specs/settings-view-android-ui/spec.md:45-48` — GIVEN Modern で表示中の Section / WHEN `SettingsRootDiff` で Cell を末尾に挿入する / THEN 箱は挿入後の末尾 Cell までを覆う
- **実装の状態**: `ModernSectionDecoration.collectSectionBoxes`(`ModernSectionDecoration.kt:255-316`) が描画のたびに現在の child から箱を組み直すため、**実装としては満たしていると読める**。実装の欠落ではない
- **欠落しているもの**: この Scenario を実際に diff 適用後の描画で確認するテスト。`ModernSectionDecorationTest.kt` は静的表示のみを扱い、`ApplyDiffTest.kt` / `VisibilityApplyDiffTest.kt` / `FullUpdateContentSyncTest.kt` は装飾を観測しない。deviation.md にも記録なし
- **対比**: iOS は同一 Scenario を `SectionBoxDecorationTests.swift:683` `test_構造変更後も箱がCell範囲に追従する` で検証しており、さらに `:764` / `:780` で挿入・削除後の clip 追従まで見ている。両 OS で検証水準が非対称
- **見立て**: **テストを追加するのが妥当**。`ModernSectionDecorationTest` に host した Store へ末尾 Cell 挿入の diff を適用して `record(activity, over = false).roundRects` の下端が新末尾 Cell の下端に一致することを見るケースを 1 本足せば閉じる (既存ヘルパで書ける)。「Android は描画のたびに再集計するため構造的に自明」としてオーナーが deviation 合意する選択もありうるが、その場合は deviation.md への追記が必要。**判断は呼び出し元とユーザーに委ねる**

---

## 6. 判定

**INVALID** — ❌ 1件 (Android の Scenario 1件でテストが欠落、deviation.md に未記録)。

それ以外の全 Requirement / Scenario は ✅ 一致 または ⚠️ deviation 記録済み。虚偽チェックなし、逆流なし、テストは iOS 885 / Android 1016 すべて成功。❌-1 は実装欠陥ではなくテスト網羅の穴であり、テスト 1 本の追加、または deviation.md への追記のいずれかで VALID になる。

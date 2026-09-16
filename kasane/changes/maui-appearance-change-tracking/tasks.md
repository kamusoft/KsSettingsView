# Tasks: maui-appearance-change-tracking

挙動の正は `specs/`。設計判断は [design.md](design.md) の Decision と [exploration.md](exploration.md) の決定事項 (統合範囲 A)、[proposal.md](proposal.md)。UI の新規デザインは無く mock は持たない (既定色が正しく追随するようになるだけで見た目の契約は変えない)。実行時挙動の不具合なので handbook `cross/runtime-behavior-verification.md` に従い、修正前の再現 → 修正 → 同一手順での解消確認 → 証跡を `evidence/` に残す (識別子・端末名を含めない)。

## 0. 再現と切り分け (実装の先頭。修正箇所をここで確定する)
- [x] 0.1 MauiHost iOS (`maui/tests/KsSettingsView.MauiHost`、手順は handbook `maui/integration-host-verification.md`) の設定画面を表示したまま Simulator の外観をダークへ切り替え、行背景が白のまま文字だけ dark になる症状を再現する (ライトへ戻したときの挙動も記録する)。証跡を `evidence/maui-ios-mauihost-before-*.png` に残す。同じ手順を MauiHost Android (uiMode で再生成されない構成のまま) でも行い、追随することを確認して `evidence/maui-android-mauihost-before-*.png` に残す (→ Requirement: 表示中の外観変更で未指定色が追随する (MAUI) / Scenario: 色を指定しない行を表示中に外観を往復させる (iOS), (Android))
- [x] 0.2 iOS Native 単体で同じ手順を行う: iOS Sample の Theme を渡さない画面 (`samples/ios/KsSettingsViewSample/StoreDemoView.swift` または `DSLDemoView.swift`) を表示したまま外観を切り替え、行背景が追随するかを確認して `evidence/ios-native-before-*.png` に残す。**追随しない** = iOS 本体の問題 (1. を行う)。**追随する** = MAUI 経路固有 (2. を行う) (→ Requirement: 表示中の外観変更で行の背景と下地が描き直される (iOS) / Scenario: 表示中の外観切替で行の背景が dark 既定で描かれる)
- [x] 0.3 MAUI iOS で `KsHostContainment` (`maui/KsSettingsView.Maui/Platforms/iOS/KsHostContainment.cs`) の親 view controller 解決が成立しているか (Native の view controller の `ParentViewController` が null か) をログで確認し、0.2 の結果と合わせて修正箇所 (1. / 2. / 両方) を本 tasks の注記として確定する。**1. と 2. のどちらも該当しない結果 (仮説が両方外れた) なら以降に進まず探索へ戻す** (対応 Requirement なし。修正箇所の確定)

  **切り分けの結果 (2026-09-15): 仮説は両方とも外れた。修正箇所は「どちらも該当しない」。** 以降 (1. / 2.) へは進まず探索へ戻す。

  - **0.1 MauiHost iOS**: 症状が**再現しない**。設定画面 (Theme を渡さない画面) を表示したまま Simulator の外観をライト → ダーク → ライトと往復させると、行の背景・行の間の下地・文字がすべて現在の外観の既定で描き直された (「外観追随ボタン」の title 色も従来どおり緑 ↔ マゼンタで切り替わる)。証跡は `evidence/maui-ios-mauihost-before-01-light.png` / `-02-dark.png` / `-03-back-to-light.png`。次の 3 条件でも同じで、いずれも追随した: (a) 初回表示中の切替、(b) メニューへ戻って再訪問した後の切替 (Handler の再接続後)、(c) アプリを背面にしてから切り替えて復帰。iOS 26.0 の Simulator で確認し、iOS 18.6 の Simulator でも追随することを確認した (`evidence/maui-ios-mauihost-before-04-ios18-dark.png`)
  - **0.1 MauiHost Android**: 追随する (既知どおり)。同じ画面を表示したまま夜間モードを往復させると、行の背景・文字・「外観追随ボタン」の title 色がすべて現在の外観で描き直された。証跡は `evidence/maui-android-mauihost-before-01-light.png` / `-02-dark.png` / `-03-back-to-light.png`
  - **0.2 iOS Native 単体**: 追随する。iOS Sample の「Store 方式デモ」(Theme を渡さない画面) を表示したまま外観を往復させると、行の背景が light 既定 (白) ↔ dark 既定 (`#1C1C1E` 相当) で描き直された。証跡は `evidence/ios-native-before-01-light.png` / `-02-dark.png` / `-03-back-to-light.png`。よって「追随しない = iOS 本体の問題」の分岐には入らない
  - **0.3 親 view controller の解決**: 成立している。`KsHostContainment` に一時的な診断出力を入れて確認したところ、Host 生成時点で親が `PageViewController` として解決され、`ConfirmAdded` の時点で `ParentViewController` は null ではなく、Native の view controller / view の trait も window の外観と一致していた (`evidence/maui-ios-containment-parent-resolution.log`)。診断出力は確認後に取り除き、本体コードに差分が無いことを確認済み
  - **参考 (前 change の観察との食い違い)**: `kasane/changes/archive/2026-09-06-cell-style-dark-appearance-parity/tasks.md` の 8.4 が記録した「MAUI iOS の dark で行背景が白のまま残る」は、今回の手順では再現しなかった。原因は特定していない (当時の証跡画像は archive 時に削除済みで、使用した Simulator の OS 版も記録に無いため突き合わせができない)。今回の実行は worktree の HEAD (`dcde365`) をそのままビルドしたもので、症状を再現させるための追加条件は見つけられなかった

## 1. iOS 本体 (0.3 で該当した場合)
- [ ] 1.1 design.md Decision 1 のとおり、`KsSettingsViewController` (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`) の view を外観 (`UITraitUserInterfaceStyle`) 変化の唯一の観測主体にし、既存の Theme 再適用経路で標準 Cell (`KsListCellBase`) と CustomCell (`CustomCellView`) の行背景・list 下地・Section 装飾・text Header / Footer をまとめて再適用する。行を作り直さず、可視行とスクロール位置を保つ。CGColor を持つ既存の 2 箇所 (`KsCheckBoxView` / `SectionBoxDecorationView`) の購読と流儀を揃える (iOS 17+ は `registerForTraitChanges`、iOS 16 は `traitCollectionDidChange` のフォールバック — `KsCheckBoxView.swift` と同じ 2 経路) (→ Requirement: 表示中の外観変更で行の背景と下地が描き直される (iOS) / Scenario: 表示中の外観切替で行の背景が dark 既定で描かれる, CustomCell の行の背景も描き直される, スクロール位置と可視行を保ったまま往復する)
- [ ] 1.2 明示した固定色の行背景が再適用で置き換わらないことをコードで確認する (`Theme` の固定 `UIColor` はそのまま再適用される) (→ Scenario: 明示した固定色の行背景は外観切替で変わらない)
- [ ] 1.3 text の Header / Footer (Section と Root) の描画 (`KsSettingsViewController.swift` の accessory を list cell へ適用する箇所) に Theme の `headerBackgroundColor` / `footerBackgroundColor` を背景設定として適用する。View accessory には適用しない。未指定は Theme の既定 (dynamic) がそのまま効く (→ Requirement: text の Header / Footer の背景色を Theme から適用する (iOS) / Scenario: 全 Scenario)

## 2. MAUI iOS handler (0.3 で該当した場合)
- [ ] 2.1 design.md Decision 2 のとおり、`KsHostContainment` (`maui/KsSettingsView.Maui/Platforms/iOS/KsHostContainment.cs`) の child view controller の結び付けを必ず確立する: 0.3 で親が解決できなかった契機 (Host 生成時・`ConfirmAdded` の再試行時) を特定し、親 Page の view controller が確定する契機 (Handler 接続完了・`Loaded` 後の window への取り付け) でもう一度解決する。最後まで見つからない場合は診断ログを出し黙って成立させない。Handler の切断・再接続 (ページ再訪問) で親付けが二重・残留にならないことをテストで固定する (`LeakTests.cs` の流儀) (→ Requirement: 表示中の外観変更で未指定色が追随する (MAUI) / Scenario: 色を指定しない行を表示中に外観を往復させる (iOS))

## 3. iOS tests: 観測点の置き換え
- [ ] 3.1 `ios/Tests/KsSettingsViewUITests/ThemeDarkAppearanceRenderingTests.swift` の `appliedCellBackgroundColor` (保持している `UIColor` を `resolvedColor(with:)` で自前解決) を、切替後の描画に使われている値 (行の描画画像の背景領域の画素、または行の背景 view の layer に実際に載っている色) を読む形に置き換える。list 下地と同じく描画画像の画素で観測できるならそれに統一する (→ Scenario: 既存の外観切替テストは描画に効いている値を観測する, 表示中の外観切替で行の背景が dark 既定で描かれる)
- [ ] 3.2 `CellStyleAppearanceTests.swift` の同型の観測 (保持値の自前解決) も 3.1 と同じ形に置き換える (→ Scenario: 既存の外観切替テストは描画に効いている値を観測する)
- [ ] 3.3 CustomCell を含む root で表示中の外観切替を行い、CustomCell の行の描画値が dark 既定になるテストを追加する (→ Scenario: CustomCell の行の背景も描き直される)
- [ ] 3.6 画面に収まらない行数の root を途中までスクロールした状態でライト → ダーク → ライトと往復させ、各切替後の可視行の Cell ID の集合・インスタンス・content offset が不変で、背景と下地の描画値が各外観の既定になるテストを追加する (→ Scenario: スクロール位置と可視行を保ったまま往復する)
- [ ] 3.7 text Header / Footer の背景が Theme の明示色 / dark 既定で描かれ、View accessory には適用されないテストを追加する (描画値で観測) (→ Requirement: text の Header / Footer の背景色を Theme から適用する (iOS) / Scenario: 全 Scenario)
- [ ] 3.4 明示した固定色の行背景が切替後も描画値として固定色のままであるテストを追加する (→ Scenario: 明示した固定色の行背景は外観切替で変わらない)
- [ ] 3.5 **検出力の確認**: 1. の修正を一時的に外した (または 0.2 で追随しなかった) 状態で 3.1 のテストが失敗することを確認し、結果を本 tasks に注記する (0.2 で iOS Native 単体が追随していた場合は、観測点の置き換えが既存の緑を保ったまま描画値を読んでいることの確認に読み替える) (→ Scenario: 既存の外観切替テストは描画に効いている値を観測する)

## 4. MAUI: Theme プロパティの AppThemeBinding の回帰資産
- [ ] 4.1 `maui/tests/KsSettingsView.MauiHost/SettingsPage.xaml` の `SettingsView` に、Theme プロパティ 1 つ (`CellValueTextColor`。同じ画面の「外観追随ボタン」の title 色や未指定の行背景の観測を妨げない項目) へ light / dark で異なる色の `AppThemeBinding` を常設する。0.1 の「色を指定しない画面」の観測対象は行背景と title 文字なので、この 1 項目の指定と両立させる (→ Requirement: SettingsView の Theme プロパティに書いた AppThemeBinding は外観変更で Theme の再適用まで届く / Scenario: Theme プロパティの AppThemeBinding が表示中の外観切替で描き直される (検証ホスト))
- [ ] 4.2 (調査) `maui/KsSettingsView.Maui.Tests` で、`ImplicitStyleTests.cs` の前例 (`new Application()` を `Application.Current` に立てる) に倣い Application → Window → Page → `SettingsView` の最小 element ツリーを組み、`UserAppTheme` の切替で `AppThemeBinding` 付き Theme プロパティが再評価されて Theme snapshot が dark 側の値で gateway へ再配信されるかを試す。成立すればテストとして追加し、成立しなければ試した方法と結果を本 tasks に注記する (受け入れ基準は 4.1 の検証ホスト。対応 Requirement なし)
- [ ] 4.3 handbook `kasane/handbook/maui/integration-host-verification.md` の MauiHost 節に、(a) 色を指定しない行の背景と文字が表示中の外観の往復で両 OS とも現在の外観の既定になること、(b) 4.1 の `AppThemeBinding` 付き項目が往復で切り替わることを「期待される表示」と「完了条件」に追記する (確認項目の追加であり規約の変更ではない) (→ Requirement: 表示中の外観変更で未指定色が追随する (MAUI), SettingsView の Theme プロパティに書いた AppThemeBinding は外観変更で Theme の再適用まで届く)

## 5. spike: Section / Cell の element ツリー接続の可否 (記録のみ。本 change の契約には入れない)
- [x] 5.1 facade の `SettingsView` が `Section` を、`Section` が `Cell` を `AddLogicalChild` で論理子にする最小の一時改変を入れ、MAUI Sample の基本 Cell デモの ButtonCell `TitleColor` に `AppThemeBinding` を付けて表示したまま外観を切り替える (前回 change の 0.1 と同じ流儀。iOS / Android 各 1 面)。再評価されるか、`Parent` が入ることで既存テスト (`LeakTests.cs` / `BindingContextTests.cs` / 多重配置の検査) に影響が出るかを観察し、結果 (成立 / 不成立・観察・副作用) を [exploration.md](exploration.md) の「決定事項」に記録する。一時改変は戻し `git diff -- maui samples/maui` が空であることを確認する。結果に関わらず本 change には同梱しない。成立していれば core/ADR-0031 Decision 2 の改訂を伴う別 change として起票するかをオーナーが裁定する (対応 Requirement なし。記録のみ)

  **結果 (2026-09-15): 成立 (iOS / Android とも)。** 詳細は [exploration.md](exploration.md) の「spike ②-b の結果 (2026-09-15)」。`KsBindingContextBinder.Distribute` に `AddLogicalChild` を 1 行足すだけで両段が論理子になり、ButtonCell の `TitleColor` の `AppThemeBinding` が外観切替で再評価されて native の行まで届いた。改変込みの MAUI facade テストは 528 件 / 失敗 0 件 (改変なしと同数・同結果) で既存テストは 1 件も落ちなかったが、この最小改変は `RemoveLogicalChild` を対にしていないため安全の根拠にはならない (外す 4 経路の対応が本番の設計作業)。一時改変は退避したバイト列から書き戻し、`git diff -- maui samples/maui` が空であることを確認済み。本 change には同梱しない

## 6. 完了判定
- [ ] 6.1 修正後に 0.1 と同一手順を MauiHost iOS / Android でライト → ダーク → ライトと往復させ、行背景と文字がともに現在の外観の既定になること、4.1 の項目が切り替わること、「外観追随ボタン」の title 色が従来どおり切り替わることを確認して `evidence/maui-*-mauihost-after-*.png` に残す (→ Requirement: 表示中の外観変更で未指定色が追随する (MAUI) / Scenario: 全 Scenario、Theme プロパティの AppThemeBinding が表示中の外観切替で描き直される (検証ホスト))
- [ ] 6.2 1. を行った場合は iOS Native 単体 (0.2 の画面) でも同一手順で解消を確認し `evidence/ios-native-after-*.png` に残す (→ Requirement: 表示中の外観変更で行の背景と下地が描き直される (iOS))
- [ ] 6.3 iOS テスト全件 (`cd ios && xcodebuild test -scheme KsSettingsView ...`、実行件数をバンドルごとに確認) と MAUI facade テスト全件 (`maui/KsSettingsView.Maui.Tests`) を実行し、件数付きで報告する (handbook `cross/test-execution.md`)
- [ ] 6.4 `ios/Sources/` を触った場合、Swift 6 言語モードの一時設定ビルドで error 0 件を確認し `ios/Package.swift` の差分が無いことを確認する (handbook `ios/swift6-language-mode-check.md`)
- [ ] 6.5 `evidence/` に個体・個人を特定する値 (端末名・UDID 等) が写り込んでいないことを identity lint で確認する

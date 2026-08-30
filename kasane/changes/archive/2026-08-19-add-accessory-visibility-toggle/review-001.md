# レビュー結果: add-accessory-visibility-toggle (001 回目)

**日付**: 2026-08-19
**判定**: APPROVED

## サマリー

7 capability のデルタスペックが要求する Scenario は、iOS / Android / MAUI / Bridge / Samples の全層で実装され、いずれも表示結果 (iOS は layout attributes と実 supplementary view、Android は RecyclerView の行テキスト、MAUI は fake gateway の Section 置換) を観測するテストで固定されている。全 3 スイートを再実行して 0 failure を確認し、対称化 3 件を含む視覚証跡も実在と提出コードの対応を確認した。Critical / Major はなし。指摘は証跡の記録粒度に関する Minor 1 件と、設計整理の Suggestion 2 件。**Minor 1 件は本レビュー内で evidence README への追記により対応済み** (下記「対応確認」節。追記内容の裏取りとしてレビュアー側で binding のビルド生成物を突き合わせ、native の ObjC セレクタと managed 側セレクタの一致まで確認した)。

### 実行したビルド・テスト (レビュアー側で再実行)

| platform | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | `** TEST SUCCEEDED **` / 0 failures |
| Android | `./gradlew test --rerun-tasks` | BUILD SUCCESSFUL / tests=2418 failures=0 errors=0 (test-results XML 集計) |
| MAUI | `dotnet test KsSettingsView.Maui.Tests` | 合格 424 / 失敗 0 |

`python3 scripts/comment-policy-lint.py` は禁止 0 件。lint の対象外である untracked の新規テスト 6 件も個別に確認し、禁止参照 (change-id 裸参照・アーカイブ文書パス・SHALL 等の仕様キーワード) は無し。

## 指摘事項

### [🟡 Minor → ✅ 対応済み] MAUI の視覚証跡が Android host のみで、撮影 platform が evidence に記録されていない

**該当箇所**: `kasane/changes/add-accessory-visibility-toggle/evidence/README.md:12`、`maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:48,67,95`、`maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:697-707`

**問題点**: `maui-01`〜`maui-08` の画像は Android のステータスバー・Material 描画であり、MAUI サンプルは Android host で実行されている (`kasane/lessons/inbox/ios-incremental-build-runs-stale-binary.md` の新規 evidence 行も `dotnet build -f net10.0-android -t:Install` と記す)。しかし evidence README は「MAUI サンプル」としか書いておらず、どの host で撮ったかが証跡から読み取れない。その結果、以下が「静的読解のみを根拠に完了扱い」になっている:

- tasks 3.3「iOS Binding の `ApiDefinition.cs` へ 2 プロパティを追加し、生成される managed API を検証する」
- tasks 4.3「per-TFM gateway の変換に 2 フィールドを追加し、初期構築・挿入・置換の全経路で輸送されることを確認する」の iOS TFM 側

net10.0 のユニットテスト (`SectionVisibilityTests.cs`) は facade `Section` が gateway へ渡ることまでしか見ておらず、`Platforms/iOS/KsBridgeGateway.cs` と `ApiDefinition.cs` は**コンパイル対象にすら入らない**。実害リスクは低い (既存 `IsVisible` / `[Export("isVisible")]` と完全同型で、Swift の `@objc var isHeaderVisible` に対する `isHeaderVisible` / `setIsHeaderVisible:` セレクタ対応も既存と同じ) が、`ObjC` セレクタの取り違えはビルドを通り抜けて実行時にしか出ないため、無検証であること自体は記録されるべき。

**推奨修正**: evidence README に MAUI の撮影環境 (host platform と TFM) を iOS / Android と同じ粒度で明記し、MAUI-iOS host が未検証であることを README または deviation.md に残す。もしくは MAUI サンプルを iOS host で 1 組 (トグル ON/OFF) 撮って証跡を揃える。

**対応確認 (2026-08-19、レビュアー再確認)**: `evidence/README.md` に「MAUI の撮影 platform について」節が追記され、(a) `maui-01`〜`08` 行への host 明記 (`net10.0-android` / Pixel 6a / Android 16)、(b) net10.0-ios の実行時証跡が取れていない事実と理由 (.NET for iOS SDK と Xcode のバージョン不一致という環境要因)、(c) iOS TFM 側の根拠が静的検証である旨とその内訳 — の 3 点が記録された。**指摘の要求 (撮影 platform の記録 + 未検証範囲の明示) を満たしている。**

さらにレビュアー側で、README が根拠として挙げた静的検証の実在と内容を裏取りした (以下はすべて本ワークツリーのビルド生成物を直接確認したもの):

- `maui/macios/KsSettingsView.Binding.iOS/obj/Debug/net10.0-ios/compiled-api-definitions.xml` に `P:KsSettingsView.Bridge.KsBridgeSection.IsHeaderVisible` / `IsFooterVisible` が実在 — binding プロジェクトは新規 2 プロパティを含めて**実際にコンパイルが通っている** (`KsSettingsView.Binding.iOS.dll` も生成済み)
- 生成された managed 側 (`obj/.../iOS/KsSettingsView.Bridge/KsBridgeSection.g.cs` および `KsSettingsView.Binding.iOS.dll`) が持つセレクタは `isHeaderVisible` / `setIsHeaderVisible:` / `isFooterVisible` / `setIsFooterVisible:`
- native 側 (ビルド済み xcframework の `KsSettingsViewBridge-Swift.h`) の宣言は `@property (nonatomic) BOOL isHeaderVisible;` / `BOOL isFooterVisible;` — すなわち ObjC の getter / setter セレクタは上記 managed 側と**一致する**

これにより、指摘時に唯一残っていた「セレクタ取り違えはビルドを通り抜けて実行時にしか出ない」というリスクは、静的読解ではなく実ビルド生成物の突き合わせによって解消された。実行時 (net10.0-ios 上での目視) の証跡が無い点は README の記録どおり残るが、輸送経路の正しさの根拠としては十分と判断する。

なお `compiled-api-definitions.xml` は `obj/` 配下の未追跡ビルド生成物であり clean で消えるため、README の引用先は将来再現できない。上記の確認結果を本レビュー証跡 (change と共にアーカイブされる) に残すことで代替とする。追加対応は不要。

### [🔵 Suggestion] `supplementaryModes` / `makeListConfig` は実行時経路から外れており、そこへ足した新テストは利用者可視の回帰検出力を持たない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:490-503`, `:791-801`, `:1390-1397`、`ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift:128-163`

**問題点**: `makeLayout(for:)` は `listConfig.headerMode` / `footerMode` を `.supplementary` 固定にし、section ごとの出し分けは `sectionProvider` 内の `boundarySupplementaryItems` 間引きで行う (同ファイル :512-521 のコメントが明示)。そのため `makeListConfig` / `supplementaryModes` の呼び出し元はテストだけで、`layoutModesDiffer` に至っては呼び出し元が 1 つも無い。本 change はこの一群に AND 合成を織り込み、`test_表示トグルfalseのHeaderFooterはsupplementaryModesでnoneになる` などの新規アサーションを足しているが、これらが落ちても利用者の画面は変わらない。

実表示を観測する `SectionAccessoryVisibilityTests` が別途あるためカバレッジ上の欠落は無く、判定に影響する問題ではない。ただし「helper の一貫性テスト」であって「表示契約のテスト」ではない点は、後続で誤読されやすい。

**推奨修正**: 本 change での対応は不要 (dead code 自体は既存債務)。整理する場合は別 change で、`layoutModesDiffer` の削除と `supplementaryModes` / `makeListConfig` の位置づけ (実行時未使用) の明示をまとめて扱う。

### [🔵 Suggestion] Android の `settingsRoot { section(...) }` ビルダーだけトグル引数が無く、iOS の `ksSection` と非対称

**該当箇所**: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootScope.kt:38-77`

**問題点**: iOS は `ksSection` / `Section.init` の 4 経路すべてに `isHeaderVisible` / `isFooterVisible` を通したのに対し、Android は `DSLScope.Section` のみで、`SettingsRoot` を組む糖衣ビルダー `SettingsRootScope.section(...)` には引数が無い。デルタスペックが要求するのは `DSLScope.Section` だけなので**仕様違反ではない**。また Kotlin は `Section(id = ..., isHeaderVisible = false)` と data class を直接書けるため機能上の欠落でもない。

**推奨修正**: 対応するなら別 change で `section(...)` に既定 `true` の 2 引数を追加する。優先度は低い。

## 確認した観点 (問題なしと判定した範囲)

- **Section 再構築でのトグル保持 (最大リスクとされた箇所)**: `ios/Sources` 内の `KsSettingsViewCore.Section(` 構築サイトを全列挙 (Store 6 箇所 / Controller 6 箇所 / SwiftUI 4 箇所) し、**すべてで両トグルが引き継がれている**ことを確認。Android は data class の `copy` 経由で構造的に保持される。`SettingsRootStore` の insert/remove/replace/replaceCells/move/updateAccessory と visible projection、SwiftUI `copyWith`、Compose `SectionHandle.sectionHeader/sectionFooter` の全経路にテストが付いている
- **値等価性への参加**: iOS の手動 `==` / `hash(into:)` 双方に追加済み。iOS / Android 両方で「片方のトグルだけ違う Section は等価にならない」「同値なら hash も一致」を固定
- **AND 合成と内容不在の統一判定**: iOS `shouldShowHeader` / `shouldShowFooter` / `hasAccessoryContent`、Android の同名 companion 関数で対称。view accessory を常に「内容あり」とする ADR-0023 の規定も両 OS でテスト済み
- **高さ解決の後置**: `makeHeaderBoundaryItem` 冒頭の `guard shouldShowHeader` で存在判定が先行。逆契約を固定していた既存テストは新契約へ反転済みで、「表示する Header では `headerHeight = 40` が従来どおり効く」退行防止テストも併設されている
- **DSL 差分検出**: iOS / Android とも preflight にトグル変化検出を追加し、`.full` 1 件のみ発行 (内容変化併発時も二重適用しない) ことと、トグル不変時に通常 Diff へ戻ること (退行防止) の両方を固定。Store 経路との表示結果一致 (core/ADR-0018 の対称テスト義務) も両 OS で実表示比較として実装されている
- **視覚証跡の実在と対応 (lessons process/L-003)**: `evidence/` の 24 枚 + 対称化 11 枚 + 集計を実見。iOS `ios-02` / `ios-08` は Section D の header「観察対象 Section D（Header / Footer）」と footer「Header / Footer は内容を保持したまま隠れます」が提出コードの文言と一致し、トグル OFF で D-1 / D-2 を残したまま両 accessory が消えることが読み取れる。対称化は `symmetry-ios-full-target-theme-40` / `-control-theme-40` の対で「対照モード OFF なら検証 1〜3 の Header / Footer が生成されない・ON なら出る」が同一ビルド同一画面で確認できる
- **sample-parity**: 3 platform の追加文言 (「ヘッダー表示」「フッター表示」および各 description、Section D の header / footer / Cell 文言) が一字一句一致し、挿入位置 (アンカー Section の後・Section C の前) も揃っている
- **足場の凍結**: `tasks.md` の差分はチェックボックスのみ。proposal / specs / second-opinion は無改変
- **deviation の網羅**: SwiftUI `copyWith` への `isVisible` 追加と対称化証跡の一時改変は deviation.md 記載どおり。それ以外に spec からの無断逸脱は検出されず
- **コメント規約**: change-id 裸参照・アーカイブ文書パス・`SHALL` 等の仕様キーワード・履歴記述の混入なし。参照は `core/ADR-0023` / `core/ADR-0018` / `core/ADR-0021` の許容形式のみ

## アクションプラン

1. ~~(Minor) evidence README に MAUI の撮影 host を明記し、MAUI-iOS host 未検証を README または deviation.md へ記録する~~ — **対応済み (2026-08-19)**。残作業なし
2. (Suggestion) `layoutModesDiffer` を含む iOS の実行時未使用 layout helper の整理は別 change へ
3. (Suggestion) Android `SettingsRootScope.section(...)` へのトグル引数追加は別 change へ (優先度低)

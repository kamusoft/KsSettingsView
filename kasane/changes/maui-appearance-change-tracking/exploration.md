# Exploration: maui-appearance-change-tracking

`fix-maui-ios-dark-appearance-row-background` と `maui-appthemebinding-coverage` の 2 つの簡易起票を統合した change (2026-09-15、オーナー裁定)。統合前の 2 スタブの本文は本ファイルに畳み込み、元ディレクトリは破棄した。

## 課題 / 動機

MAUI で `SettingsView` を表示したまま OS の外観 (ライト / ダーク) を切り替えたときの追随が、不具合と回帰資産の欠落の両面で欠けている。利用者の体験としては 1 つ (「表示中に外観を切り替えても正しい色で描かれ続ける」) だが、原因は 3 つに分かれる。

### ① MAUI iOS で表示中にダークへ切り替えると行の背景がライトのまま残る (旧 fix-maui-ios-dark-appearance-row-background)

cell-style-dark-appearance-parity の tasks 8.4 (MauiHost の外観追随確認、2026-09-06) で発見。MAUI iOS で SettingsView を表示したまま OS の外観をダークへ切り替えると、行の背景がライトのまま (白) 残る一方、未指定のテキスト色だけが dark 既定 (白系) へ解決され、Cell の文字が読めなくなる。cell-style-dark-appearance-parity の変更を外した HEAD でも再現する既存不具合。MAUI Android では起きない。facade は Theme を全項目未指定で送っている (MauiHost の `SettingsPage.xaml` は色を一切指定しない)。証跡: `kasane/changes/archive/2026-09-06-cell-style-dark-appearance-parity/evidence/maui-host-buttoncell-retheme-ios-dark.png`。

関連: core/ADR-0030 (未指定色の外観既定への解決)、core/ADR-0031 Consequences (本不具合の起票元)、fix-default-colors-dark-appearance (archive 2026-09-06) の iOS 側の実装。

### ②-a `SettingsView` 単位の Theme プロパティに書く `AppThemeBinding` の回帰資産が無い (旧 maui-appthemebinding-coverage)

docs-refresh (2026-09-06) で、MAUI 利用者がライト / ダーク両外観の色を自分で決める手段として `AppThemeBinding` を facade (`SettingsView`) の色プロパティに使うレシピを `skills/{en,ja}/kssettingsview-maui/references/styling.md` と `skills/{en,ja}/kssettingsview-aiforms-migration/references/api-mapping.md` に掲載した。一方この経路が Theme の再適用まで届くという根拠は concepts (`maui/api/maui-styling.md`「未設定の色と外観」、`core/styling/style-resolution.md` の MAUI 行) の「Simulator / Emulator で確認済み」という記述のみで、`maui/KsSettingsView.Maui/`・`maui/KsSettingsView.Maui.Tests/`・`samples/maui/` には `AppThemeBinding` が 1 箇所も現れない (fix-default-colors-dark-appearance の tasks 0.1 spike で確認された経路だが常設の固定がない)。利用者向け文書が推奨する経路として、Sample かテストで回帰を固定する。

### ②-b Cell 単位の色プロパティでは `AppThemeBinding` / `DynamicResource` が外観切替で再評価されない (旧 maui-appthemebinding-coverage の追記論点)

- (2026-09-06、cell-style-dark-appearance-parity の tasks 0.1 spike) Cell 単位の色プロパティ (`ButtonCell.TitleColor`) に付けた `AppThemeBinding` は iOS / Android とも外観切替で再評価されず native まで届かない。同じ画面の素の `Label` は再評価され、同じ Cell プロパティへの直接代入は届く。見立て: `Section` / `CellBase` は `Element` だが MAUI の element ツリーに繋がれておらず (`Parent` が null)、ツリー外の `Element` では `AppThemeBinding` が外観変更を受け取らない (ツリーに載せない素の `Label` でも同様に再評価されないことを対照実験で確認)。証跡: `kasane/changes/archive/2026-09-06-cell-style-dark-appearance-parity/evidence/maui-*-basic-cells-buttoncell-*`。オーナー裁定 (2026-09-06): parity 側は「外観変更を購読して Cell プロパティへ再代入」に改めて続行し (core/ADR-0031 Decision 2 の MAUI 節)、`AppThemeBinding` を Cell で効かせる改修は別途探索する
- (2026-09-11、`../ColorAnalyzer` の AiForms 移行 `migrate-settingsview-to-kssettingsview` より) `DynamicResource` でも同じ。Cell に直接書いた `AccentColor="{DynamicResource AccentColor}"` は初期解決は届くが外観切替では再評価されない。実測 (iOS シミュレータ、画素値サンプリング): ライト起動時 `#3367CC`、ダーク起動時 `#6C91D9` で初期解決は外観に応じて正しい。ライト表示中にダークへ切り替えると `#3367CC` のまま。`SettingsView.CellAccentColor="Red"` を当てた状態で赤にならなかったことが Cell 直書きの解決が効いている証拠。コード上の裏付け: `maui/` 全体で `AddLogicalChild` を呼ぶのは accessory View / CustomCell content の経路のみ (`Internals/KsAccessoryViewOwnership.cs:52-54`) で、Section・Cell 自体を論理子に追加する箇所がない。一方 `SwitchCell.cs` の `AffectsSnapshot` は `AccentColor` を含み、変更さえ起きれば native まで届く配線は存在する
- 利用者向け文書への影響: `skills/{en,ja}/kssettingsview-maui/references/cells.md` と `kssettingsview-aiforms-migration/references/api-mapping.md` の「`x:Reference` と `{DynamicResource}` は解決されない」は不正確で、正しくは「一度きりの解決は届くが差し替え通知は Cell まで伝播しない」。この修正は `settingsview-migration-defects` 側で扱う

## 調査記録 (2026-09-15、ksn-scout 2 本)

### ① の経路

- iOS Native の既定色はすべて dynamic `UIColor` (`ios/Sources/KsSettingsViewUI/Theme.swift:262-312`)。bridge も未指定を潰さず native 既定へ委ねる (`ios/Sources/KsSettingsViewBridge/KsBridgeTheme.swift:117-156`)。MAUI iOS handler は MAUI 標準 `ViewMapper` のままでライブラリ側が固定色を書く箇所は無い。**「bridge / handler が固定色に解決している」仮説はコード上否定**
- 行背景の描画経路は `UIBackgroundConfiguration` 一択 (`ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:117-121`、押下戻しは `KsCellViewSupport.swift:78-96`)。`ios/Sources` 全体で trait 変化を購読しているのは `KsCheckBoxView.swift` と `SectionBoxDecorationView.swift` の 2 箇所 (CGColor に落とす箇所) だけで、`KsSettingsViewController` も cell 基底も trait 変化で Theme 再適用・configuration 更新をしない。行背景の追随は UIKit の自動更新に全面依存
- Android は本体が夜間モード変化を拾って未指定色を再解決し再適用する (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:329-335`、attach 時の照合 `:317`)。MauiHost Android は `ConfigChanges.UiMode` を宣言して再生成されない (`maui/tests/KsSettingsView.MauiHost/Platforms/Android/MainActivity.cs:18`)。iOS に対応物が無いのが構造上の非対称
- MAUI ライブラリ本体は外観変更を一切購読していない (`RequestedThemeChanged` / `TraitCollectionDidChange` の参照ゼロ)
- 副次仮説: MAUI 経路では Native Host の VC が親 VC の子として成立していない可能性 (`maui/KsSettingsView.Maui/Platforms/iOS/KsHostContainment.cs:23-37` は親が見つからなければ黙って return)。iOS Native 単体で再現するかの切り分けが先。未確認
- **既存 iOS テストの検出力**: `ios/Tests/KsSettingsViewUITests/ThemeDarkAppearanceRenderingTests.swift:200` (表示中に外観をダークへ切り替えると既定色が描き直される) と `CellStyleAppearanceTests.swift` は、保持している `UIColor` を自前で `resolvedColor(with:)` して見ているだけで、描画が古いままでも緑になる。回帰を固定するには観測点を「実際に描画に効いている値」へ移す必要がある
- MAUI 層に外観切替を扱うテストは無い (`ThemeAndCellStyleTests.cs` は配信内容の検証のみ)

### ②-a の現状

- Theme 経路 (色プロパティ → snapshot → bridge → 再適用) は `maui/KsSettingsView.Maui.Tests/ThemeAndCellStyleTests.cs` (`ThemeChangeWhileConnectedIsApplied` 等) で固定済み。固定されていないのは「`AppThemeBinding` が外観変更でこの経路を起動する」部分
- MAUI Sample の外観切替は `samples/maui/KsSettingsView.Sample.Maui/` の `MenuPage.cs` (システム / ライト / ダーク行)・`SampleAppearanceStore.cs`・`SampleThemeFollower.cs`・`SampleTheme.cs` で、各デモページが `SampleThemeFollower.Attach(...)` を呼ぶ形 (add-sample-dark-mode-toggle、2026-09-05)。`AppThemeBinding` は使っていない
- cell-style-dark-appearance-parity の「購読 + 再代入」の回帰資産は MauiHost 側 (`maui/tests/KsSettingsView.MauiHost/SettingsPage.xaml:47` の `ThemeFollowingButton`、`SettingsPage.xaml.cs:100-133`)。目視手順の正は `kasane/handbook/maui/integration-host-verification.md`

### ②-b の構造と副作用の見立て

- `SettingsView : View`、`SettingsRoot : ObservableCollection<Section>`、`Section : Element`、`CellBase : Element`。Section / Cell に `Parent` を設定する箇所は無い。BindingContext は手動配布 (`Internals/KsBindingContextBinder.cs:87` の `SetInheritedBindingContext`)。ItemsSource 由来の Cell / Section は明示設定 (`Internals/KsItemsSourceBinder.cs:339`)
- `AddLogicalChild` / `RemoveLogicalChild` は `Internals/KsAccessoryViewOwnership.cs` の 1 箇所のみ (accessory View と CustomCell content の論理所有、maui/ADR-0016)。その所有者 (Section / Cell) 自体がツリー外なので、accessory View も app のツリーへは到達していない (`kasane/concepts/maui/api/maui-rendering-lifecycle.md:33` の「logical tree に接続され」は所有者までの接続。文書と実挙動の突き合わせが要る、未検証)
- 根拠あり: BindingContext の二重伝播は起きない (同じ継承経路。手動配布は冗長化する)。共有 Style が効かないのは基底型が `NavigableElement` でないため (maui/ADR-0021) で、ツリー接続しても直らない。`AddLogicalChild` だけでは Handler は作られない
- 推測 (要検証): 多重配置の判定が controller の対応表 (`Internals/KsSettingsController.cs:2177-2228`) と `Parent` の二系統になる。Root 差し替え・Section 削除・ItemsSource 再生成・Host 切断の全経路で `RemoveLogicalChild` を対にしないと leak / 誤判定 (`LeakTests.cs`)。継承プロパティ伝播 (FlowDirection 等) が Section / Cell を経由するようになる
- **「ツリーに繋げば `AppThemeBinding` が再評価される」という前提自体が MAUI 本体未読の推論** (ローカル nuget キャッシュは dll と XML doc のみで裏取り不可)。着手前に最小 spike (Section / Cell を `AddLogicalChild` した状態で外観切替) で確かめる

## 検討した選択肢 (却下案と理由を含む)

統合の範囲 (2026-09-15):

| 軸 | A: ① + ②-a を統合、②-b は spike のみ | B: 3 つ全部を 1 つに統合 | C: 統合せず別々 |
|---|---|---|---|
| 利用者の体験 | 表示中の外観切替が iOS でも追随し、Theme 単位の推奨レシピが固定される | 加えて Cell 単位の `AppThemeBinding` も効く (spike が成立すれば) | 同じだが到達が 2 回 |
| 検証の共有 | 同じ手順で 1 回 | 同じ手順で 1 回 | 2 回 |
| ADR への影響 | なし (core/ADR-0031 は維持) | 0031 の改訂が確定で入る | ②側だけ |
| 規模 | M 級 | L 級寄り (spike 不成立なら空振り) | S + M/L |

- **B** 却下: ②-b は設計判断と ADR-0031 の改訂を伴い規模も別物。spike の結果を見ずに同梱を決めると change が膨らむ
- **C** 却下: ① と ②-a は「表示中の外観切替の追随」という 1 つの体験を不具合修正と回帰資産の両面から固める作業で、検証手順 (MauiHost / Sample で表示したまま切替) も同じ。二度やる理由が無い

## 決定事項

- (2026-09-15、オーナー裁定) **A を採る**。① と ②-a を本 change のスコープとし、②-b は本 change 内の spike で可否だけを確かめ、結果を見てから同梱か分離 (別 change + core/ADR-0031 の改訂) かを裁定する
- ① の修正方針の当たり: Android の `onConfigurationChanged` → Theme 再解決・再適用に相当するものを iOS 本体 (`KsSettingsViewController` または cell 基底) に trait 変化の購読で持たせ、両 OS を対称にする (core/ADR-0030 の「View は uiMode 変更で未指定色を再解決する」の iOS 側実装)。ただし副次仮説 (MAUI 経路で VC が親に繋がれていない) の切り分けを先に行い、そちらが原因なら handler 側の修正になる
- ① の回帰固定: iOS テストの観測点を「保持値の自前 resolve」から「描画に効いている値」へ移す

- (2026-09-15、オーナー裁定) 変更級は **L**、domain は **cross**。当初 M / core で確定したが、相方の提案レビュー (second-opinion-spec-001) の指摘 (ksn-core「複数能力横断 = L」、core では domain-skills が解決されない) で再判定した
- (2026-09-15) iOS の描画経路が Theme の `headerBackgroundColor` / `footerBackgroundColor` を消費していない (Android は消費) ことが相方レビューの照合で判明。隣接課題として本 change に同梱する (design.md Decision 4)
- (2026-09-15) ②-b の spike は記録のみとし、成立しても本 change には同梱しない。採用は core/ADR-0031 改訂込みの別 change として起票するかをオーナーが裁定する

## ADR 候補 (作成済み: なし / 未起票: design.md Decision 1 — iOS も本体が外観変化を観測して再適用する契約、core/ADR-0030 への追記候補)

- ① の修正は core/ADR-0030 / 0031 の範囲内 (iOS 側の実装の穴埋め) で新規 ADR は不要と見る。spike の結果 ②-b を採用する場合は core/ADR-0031 Decision 2 の MAUI 節の改訂 (Revisit When に明記済み) が ADR 候補になる

## 未決の論点

- ① の切り分け: iOS Native 単体 (samples/ios または iOS テストホスト) でも表示中の切替で行背景が残るか。残るなら iOS 本体の修正、残らないなら MAUI iOS handler の VC 結び付け (`KsHostContainment`) の修正
- ① の観測点: 「描画に効いている値」をテストでどう観測するか (cell の `configurationState` 更新後の `backgroundConfiguration` を読むのか、レンダリング画像の画素を読むのか)
- ②-a の固定の置き場: MAUI Sample の外観切替デモに `AppThemeBinding` 利用の Theme プロパティを常設するか、MAUI 層の自動テスト (外観変更 → `AppThemeBinding` 再評価 → snapshot → bridge) で固定するか、両方か。自動テストは `Application.RequestedTheme` の切替を単体テストで再現できるかに依る
- ②-a と `cross/conventions/sample-parity.md` (Sample 同等性の規約) との整合: iOS / Android の Sample に対応物 (dynamic `UIColor` / `isSystemInDarkTheme()` 分岐) を置くべきか
- ②-b spike の合否基準と、成立した場合の副作用検査の範囲 (多重配置・leak・ItemsSource 再生成・継承プロパティ伝播)
- accessory View / CustomCell content の `AppThemeBinding` が現状追随するかの実測 (文書は「例外で接続される」と読めるが所有者がツリー外)

## UI 素材 (ui/references/ の一覧と注釈)

なし (証跡は archive 側 `2026-09-06-cell-style-dark-appearance-parity/evidence/` を参照)

## 変更級の推奨: M (理由)

- 触る能力: iOS 本体 (または MAUI iOS handler) の外観追随、iOS テストの観測点、MAUI Sample またはテストの回帰資産、②-b の spike — 複数能力にまたがる
- 公開 API の変更: なし
- 可逆性: 高い (内部の再適用と回帰資産)
- UI: 見た目の変更は無い (既定色が正しく追随するようになるだけ)。モックは不要
- 迷ったら 1 段上の規則を適用しても L には届かない (②-b を同梱すると L 級寄りになるため、同梱の裁定は spike 後)

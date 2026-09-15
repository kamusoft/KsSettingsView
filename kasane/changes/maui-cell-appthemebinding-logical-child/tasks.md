# Tasks: maui-cell-appthemebinding-logical-child

前提: 実装前に settingsview-migration-defects の `maui-facade.md` 訂正が merge されているか確認する (未 merge なら 5.2 は rebase 後に行う)。

## 0. 前提の確認 (最初の一手)
- [ ] 0.1 `KsBindingContextBinder.Distribute` に `AddLogicalChild` を仮に入れた状態で `LeakTests` の「外部が root / CustomCell を保持したまま SettingsView と gateway が回収される」2 件を単独実行し、緑であることを確認する。落ちたら実装を止めて design.md Open Questions の判断をオーナーへ上げる (→ Requirement: 論理子化は SettingsView の回収を妨げない)

## 1. 所有の器 (facade 内部)
- [ ] 1.1 `KsBindingContextBinder<T>` を所有の器に育てる: 付けている子の集合を持ち、追加 / 除去 / Replace / Reset / 対象差し替え (`OnTargetChanged`) で `AddLogicalChild` / `RemoveLogicalChild` を対にする。他所 (期待する所有者以外の facade 所有者) に所有済みの子の追加は追加時に `InvalidOperationException` (`DuplicatePlacement` と同じ文言。handbook cross/diagnostic-message-language.md)。解除は `Parent` が自分でコレクションにもう含まれないときだけ (→ Requirement: Section / Cell は SettingsView の論理子である、同一インスタンスの重複配置の禁止)
- [ ] 1.2 controller は `Parent` を直接触らない。同じコレクション内の重複の数えあげ (`EnsureTreeHasNoDuplicates` / `EnsureCellsHaveNoDuplicates`) は現行のまま維持する (→ Requirement: 同一インスタンスの重複配置の禁止 / 「同じコレクションへの二重追加は変換時に例外」Scenario)
- [ ] 1.4 accessory View / `CustomCell.Content` の検査 (`EnsureViewIsFree` 系・`IKsCellContentGuard` / accessory guard) に「View の `Parent` が期待する所有者以外の facade 所有者を指す」を多重配置として加える (→ Requirement: 同一 View インスタンスの多重配置は例外になる)
- [ ] 1.3 継承 BindingContext の配布順序 (配布 → 引き取り) と冪等性を器のコメントに残し、`BindingContextTests` 5 件が緑であることを確認する (→ Requirement: 論理子化は既存の BindingContext 継承契約を変えない)

## 2. facade テスト
- [ ] 2.1 論理子の付け外し: XAML 構築時 (Host 未接続) の成立、Root からの Section 除去、Section からの Cell 除去、Root 差し替え、Reset と再追加、Cells 差し替え、同一 Cell 2 回追加 + 1 件除去、ItemsSource 差し替え、Host 接続中の除去 (9 Scenario) (→ Requirement: Section / Cell は SettingsView の論理子である)
- [ ] 2.2 binding の再評価: `ButtonCell.TitleColor` / `Section.HeaderText` の `AppThemeBinding` が `Application.UserAppTheme` 変更で再評価されること、`DynamicResource` がページ Resources / アプリ Resources の差し替えで再評価されること、除去後の Cell が旧ページ Resources に追随しないこと (5 Scenario) (→ Requirement: Section / Cell に設定した binding は外観変更と Resources の変更で再評価される)
- [ ] 2.2b 反映経路: 再評価が Cell の `ReplaceCell(s)` として配信され `RemoveCell` / `InsertCell` を伴わないこと、Section の `HeaderText` の再評価が `UpdateAccessory` として配信されること (2 Scenario。fake gateway の操作記録で判定) (→ Requirement: 再評価されたプロパティ値は通常のプロパティ変更と同じ経路で反映される)
- [ ] 2.3 多重配置: 同一 Cell の二重追加の例外 (既存テストの確認)、所属の経路が他所所有の Cell に触れないこと、別 SettingsView をまたぐ配置が変換時に例外になり A 側が無傷なこと、除去してから追加した Cell が新所属先の論理子になること、解放後の変換で論理子になり最初の snapshot が新所属先の値なこと、別 SettingsView に置かれた View を accessory に置くと変換時に例外 (6 Scenario) (→ Requirement: 同一インスタンスの重複配置の禁止、同一 View インスタンスの多重配置は例外になる)
- [ ] 2.4 `LeakTests` 2 件に「Section / Cell が論理子 (`Parent` あり) の状態で回収される」意図をテスト名またはコメントで明記する (→ Requirement: 論理子化は SettingsView の回収を妨げない)

## 3. 検証ホスト (MauiHost)
- [ ] 3.1 `SettingsPage` の `ThemeFollowingButton` を購読 + 再代入から XAML の `AppThemeBinding` へ置き換え、購読コード (`OnPageLoaded` / `OnPageUnloaded` / `OnRequestedThemeChanged` / `ApplyThemeColors`) を削除する (→ Requirement: Section / Cell に設定した binding は外観変更と Resources の変更で再評価される)
- [ ] 3.2 iOS Simulator / Android Emulator で、表示中に端末の外観 (Simulator / Emulator の設定) を切り替え、同じ画面・同じ行のまま `TitleColor` が描き直されることを確認し、証跡を `evidence/` に残す (端末外観の経路は単体テストで区別できないためここで押さえる。handbook maui/integration-host-verification.md)

## 4. Sample (samples-maui)
- [ ] 4.1 対象は `SampleThemeFollower` を使う 7 ページ (`BasicCellsDemoPage` / `InputCellsDemoPage` / `MauiSpecificCellFeaturesDemoPage` / `AccessoryViewsDemoPage` / `SectionDecorationDemoPage` / `CustomCellDemoPage` / `CustomCellMauiSpecificDemoPage`。`UnifyCellCommonFieldsDemoPage` / `VisibilityDemoPage` は共通配色を適用しておらず対象外)。SettingsView 用の共有 Style (Setter に `AppThemeBinding`、色は `SampleTheme` の定数を参照) と Section 装飾デモ用の別 Style を Sample の共有リソースに置き、7 ページの `SettingsView` が参照する。`SampleTheme.Apply` が持つ色以外の設定 (`RowHeight` / `HasUnevenRows`、装飾デモの `CellIconSize` / `CellIconRadius`) も Style へ移す (→ Requirement: MAUI Sample は XAML の AppThemeBinding だけで外観に追随する)
- [ ] 4.2 基本 Cell 7 種デモの ButtonCell「ログアウト」の `TitleColor` を XAML の `AppThemeBinding` にする (→ 同上)
- [ ] 4.3 `SampleThemeFollower`・`SampleTheme.Apply`・`SampleTheme.MauiTitleText`・`SampleTheme.IsDark` を削除し、`samples/maui` に `RequestedThemeChanged` の参照が無いことを確認する (→ 同上 / 「購読コードが無い」Scenario)
- [ ] 4.4 iOS / Android で 7 ページの light / dark 表示が実装前と同じ描画であること (画素比較または目視) と、表示中の外観切替で追随することを確認する (handbook cross/sample-parity.md: 文言・構成は変えない)

## 5. concepts
- [ ] 5.1 `kasane/concepts/maui/api/maui-styling.md`: 手段表の Cell 行を `AppThemeBinding` へ、「再評価されない」の説明と購読のコード例を外す
- [ ] 5.2 `kasane/concepts/maui/api/maui-facade.md`: 「Section / Cell は logical tree に載らない」の段落を「論理子であり `DynamicResource` / `AppThemeBinding` が解決・追随する」へ書き換える (`x:Reference` の記述は現行維持: namescope 経由で論理子とは別機構、本 change の対象外)
- [ ] 5.3 `kasane/concepts/maui/api/maui-rendering-lifecycle.md` / `architecture/view-materialization.md`: 論理所有の表に Section / Cell を加え、多重配置の段落に「他所に所有された Section / Cell の追加は追加時に例外 (別 SettingsView・外れた Section も)」「同じコレクション内の二重追加は変換時に例外」「accessory View / Content も期待する所有者以外の所有を検査」を反映する
- [ ] 5.4 `kasane/concepts/maui/index.md` の description を追随させ、doc-structure lint を通す

## 6. 完了
- [ ] 6.1 MAUI facade テスト全件 (現行 528 件 + 追加分) 緑 (handbook cross/test-execution.md)
- [ ] 6.2 deviation.md に付随修正・合意済み仕様からの乖離があれば記録する (調査結果は evidence / concepts へ)
- [ ] 6.3 リリースノート素材: 「同じ Section / Cell / View を複数の SettingsView で共有する配置が変換時に例外になる」を breaking change として proposal Impact から転記できる形で残す

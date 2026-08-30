以下が `kasane/changes/restore-maui-picker-selected-command/verify-003.md` の内容です。

```markdown
# 一致検証結果: restore-maui-picker-selected-command (003 回目)

**日付**: 2026-08-29  
**判定**: INVALID

## 対応表

| Requirement / Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| Requirement: PickerCell の選択完了 Command | `maui/KsSettingsView.Maui/PickerCell.cs:106`、`:255`、`:311`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1867`、`:1887` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:18` ほか同クラス。MAUI facade 全件 **516 tests / 0 failures / 0 skipped** | ✅ 一致 |
| 単一選択の完了後に選択項目を通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1867` で値を書き戻した後、`:1875` で単一選択として通知し、`maui/KsSettingsView.Maui/PickerCell.cs:311` で `SelectedItem` を渡す | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:30` | ✅ 一致 |
| 複数選択の完了後に選択項目列を通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1887` で正規化した値を書き戻した後、`:1899` で複数選択として通知し、`maui/KsSettingsView.Maui/PickerCell.cs:311` で `SelectedItems` を渡す | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:59` | ✅ 一致 |
| 選択面表示中にモードが変わっても確定した種類の引数を渡す | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1875`、`:1899` が確定通知の種類を明示し、`maui/KsSettingsView.Maui/PickerCell.cs:313` がその種類で引数を選ぶ | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:199`（単一通知時）、`:220`（複数通知時） | ✅ 一致 |
| 同じ選択の再確定も完了として通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1874`、`:1894` は同値時の不要な書き戻しを省き、`:1875`、`:1899` の完了通知は省かない | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:90`（単一・複数の2 cases） | ✅ 一致 |
| CanExecute が false でも完了を通知 | `maui/KsSettingsView.Maui/PickerCell.cs:316` は `CanExecute` を呼ばず `Execute` を直接呼ぶ | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:119` | ✅ 一致 |
| 公開選択値の直接設定では実行しない | `maui/KsSettingsView.Maui/PickerCell.cs:200`、`:207`、`:223`、`:242` の公開 setter は選択同期のみを行い、完了通知は controller の native 確定通知口からだけ呼ばれる | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:138` | ✅ 一致 |
| 選択を確定しなければ実行しない | facade の実行入口は `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1875`、`:1899` の確定 callback に限定される | `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:142`、`:166`、`:197`、`ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:477`、`:488` | ✅ 一致 |
| 未知の Cell 通知を無視 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1869`、`:1889` で対象を解決できなければ、値更新と完了通知の前に return する | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:158`（単一）、`:178`（複数） | ✅ 一致 |
| Requirement: 入力 Cell デモの複数選択を選択完了通知で受ける | `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:70`、`:78`、`samples/maui/KsSettingsView.Sample.Maui/ViewModels/InputCellsDemoViewModel.cs:32`、`:42`、`:195` | 下記3 Scenarioの実経路証跡18枚 | ✅ 一致 |
| 選択を確定すると直近イベントが更新される | `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:83`、`:84` と `samples/maui/KsSettingsView.Sample.Maui/ViewModels/InputCellsDemoViewModel.cs:44` により、選択位置のTwoWay反映後に Command が直近イベントを更新する | `evidence/ios-input-cells-confirm-{before,after}.png`、`evidence/android-input-cells-confirm-{before,after}.png` | ✅ 一致 |
| 同じ顔ぶれを再び確定しても直近イベントが更新される | `samples/maui/KsSettingsView.Sample.Maui/ViewModels/InputCellsDemoViewModel.cs:44` は値変化の有無ではなく Command 受信ごとに `LastEvent` を更新する | `evidence/ios-input-cells-reconfirm-{before,after}.png`、`evidence/android-input-cells-reconfirm-{before,after}.png` | ✅ 一致 |
| 確定せずに閉じると直近イベントが変わらない | `LastEvent` の更新は `samples/maui/KsSettingsView.Sample.Maui/ViewModels/InputCellsDemoViewModel.cs:44` の Command に限定され、選択位置 setter `:196` は更新しない | `evidence/ios-input-cells-{cancel,dismiss}-{before,after}.png`、`evidence/android-input-cells-{cancel,outsidetap,back}-{before,after}.png` | ✅ 一致 |
| Requirement: MAUI 固有 Cell 機能デモ画面 | `samples/maui/KsSettingsView.Sample.Maui/Pages/MauiSpecificCellFeaturesDemoPage.xaml:13`、`samples/maui/KsSettingsView.Sample.Maui/Pages/MauiSpecificCellFeaturesDemoPage.xaml.cs:10`、`samples/maui/KsSettingsView.Sample.Maui/ViewModels/MauiSpecificCellFeaturesDemoViewModel.cs:9`、`samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs:77` | 下記5 Scenarioの実経路証跡38枚 | ✅ 一致 |
| 画面がルートメニューから開ける | `samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs:77` が `MauiSpecific` 区分へ画面を登録し、ページ側は共通経路から同じタイトルを受け取る | `evidence/ios-maui-specific-open-{before,after}.png`、`evidence/android-maui-specific-open-{before,after}.png` | ✅ 一致 |
| 単一選択の確定で選択項目と受信回数がともに更新される | `samples/maui/KsSettingsView.Sample.Maui/Pages/MauiSpecificCellFeaturesDemoPage.xaml:18` と `samples/maui/KsSettingsView.Sample.Maui/ViewModels/MauiSpecificCellFeaturesDemoViewModel.cs:19`、`:38`、`:70` | `evidence/ios-maui-specific-single-confirm-{before,after}.png`、`evidence/android-maui-specific-single-confirm-{before,after}.png` | ✅ 一致 |
| 複数選択の確定で選択要素列と受信回数がともに更新される | `samples/maui/KsSettingsView.Sample.Maui/Pages/MauiSpecificCellFeaturesDemoPage.xaml:27` と `samples/maui/KsSettingsView.Sample.Maui/ViewModels/MauiSpecificCellFeaturesDemoViewModel.cs:24`、`:51`、`:74` | `evidence/ios-maui-specific-multi-confirm-{before,after}.png`、`evidence/android-maui-specific-multi-confirm-{before,after}.png` | ✅ 一致 |
| 同じ選択の再確定でも受信回数が増える | `samples/maui/KsSettingsView.Sample.Maui/ViewModels/MauiSpecificCellFeaturesDemoViewModel.cs:19`、`:24` は Command 受信ごとに回数を増やし、選択値の setter とは独立している | `evidence/ios-maui-specific-{single,multi}-reconfirm-{before,after}.png`、`evidence/android-maui-specific-{single,multi}-reconfirm-{before,after}.png` | ✅ 一致 |
| 確定せずに閉じると受信回数が増えない | 回数更新は `samples/maui/KsSettingsView.Sample.Maui/ViewModels/MauiSpecificCellFeaturesDemoViewModel.cs:19`、`:24` の Command 内だけで行われる | `evidence/ios-maui-specific-{single,multi}-{cancel,dismiss}-{before,after}.png`、`evidence/android-maui-specific-single-{cancel,back}-{before,after}.png`、`evidence/android-maui-specific-multi-{cancel,outsidetap,back}-{before,after}.png` | ✅ 一致 |

## 実経路証跡の確認

`evidence/` には iOS 26.4 Simulator と Android Emulator の証跡が合計56枚あり、全ファイルが有効なPNGだった。

ファイル名から確認できる操作前後の組は次のとおり。

- 入力 Cell デモ: iOS 4操作8枚、Android 5操作10枚
- MAUI 固有 Cell 機能デモ: iOS 18枚、Android 20枚
- iOSの確定・再確定・Cancel・対話的dismiss、およびAndroidの確定・再確定・Cancel・外側タップ・Backを含む
- 追加された `ios-input-cells-cancel-before.png` と `ios-input-cells-dismiss-before.png` を含め、現在は全操作でbefore/afterが対応している

代表証跡を目視し、以下を確認した。

- 入力 Cell デモの確定後、直近イベントと表示中の顔ぶれがともに更新される
- 別行のイベントが表示された状態から同じ顔ぶれを再確定すると、「通知先メンバー」のイベントへ戻る
- MAUI 固有デモの再確定では選択値を保ったまま完了通知回数だけが増える
- Backによる非確定終了では選択値と完了通知回数が変わらない
- 目視した証跡にアカウント、メールアドレス、通知、端末名などの個体・個人を特定する情報は見当たらない。全証跡については完了済みのtasks 5.5でも確認済み

## 追加検査

- tasks.md: **23 / 23タスク完了**。全タスクを実装・テスト・証跡と突き合わせ、未実装を完了扱いした虚偽チェックは検出されなかった。
- 逆流検査: スコープ拡張後の足場はコミット `5a2a7a7` で確定している。以後の作業ツリーで `proposal.md` と両 `spec.md` に差分はなく、実装中の逆流修正は検出されなかった。`tasks.md` の差分は実装・検証済みタスクの完了チェックである。
- deviation.md: 存在しない。Requirement / Scenario自体には未記録の欠落・挙動乖離はない。
- UIアーティファクト: `ui/` は存在しないが、オーナー裁定（2026-08-29）により、本画面は既存Cellの組み合わせだけで新たな視覚デザイン判断を含まないため、このchangeでは欠落として扱わない。
- テスト: 提示済み実行結果はMAUI facade **516 tests / 0 failures / 0 skipped**。本検証セッションでも `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj --no-restore --no-build --nologo` を再実行したが、read-only sandboxがMSBuildの一時ディレクトリ作成を拒否し、テスト開始前に終了した。この環境制約による終了はテスト失敗として扱わず、提示済みの同一作業ツリーに対する成功結果を採用した。
- Sampleビルド: 提示済み結果でiOS / Androidとも成功。
- プロセス成果物: `review-002.md`、`second-opinion-code-002.md`、`kasane/lessons/inbox/ui-scaffold-demanded-for-composition-only-sample-screen.md` はレビュー・教訓捕捉の成果物であり、Requirementを持たない実装差分としては扱わない。

## 未記録乖離

### ❌ 本changeと無関係なワーカー起動設定が差分へ混入している

**該当箇所**: `kasane/config.yaml:50`

`git diff afc15ae` と `git status` の全体確認で、`kasane/config.yaml` に `workers:` 設定の追加が含まれている。

この設定は `impl`、`verify`、`scout`、`extract` の起動先を `counterpart` に変更するものであり、PickerCellの公開API、Sample反映、実経路検証のいずれのRequirement / Scenarioにも対応しない。proposalのWhat Changesにも含まれず、deviation.mdによる付随修正または合意済み乖離の記録もない。

**見立て**: PickerCellの実装として直す箇所ではない。本changeのコミット対象から除外して独立した運用設定変更として扱うべきである。意図的に同梱する場合は、外部実行に関わる信頼境界変更を含めて合意し、deviation.mdへ記録する必要がある。

## 判定

デルタスペックの **3 Requirement / 16 Scenario** は、すべて実装・テスト・実経路証跡と一致する。23タスクにも虚偽チェックはなく、足場の逆流、テスト失敗、UIアーティファクトの欠落は検出されなかった。

ただし、検証対象である `afc15ae` から現在の作業ツリーまでの差分に、Requirementを持たずdeviation.mdにも記録されていない `kasane/config.yaml` のワーカー起動設定変更が1件含まれる。未記録乖離が1件存在するため、ksn-verifyの判定基準に従い **INVALID** とする。

`kasane/config.yaml` の該当差分を本changeから除外すれば、残る実装・テスト・証跡については **VALID** の条件を満たす。
```



## 未記録乖離の決着 (2026-08-29)

INVALID の唯一の理由だった `kasane/config.yaml` のワーカー起動設定 (`workers:` ブロック) について、オーナー裁定で次のとおり決着した。

- この差分は本 change のスコープ外の**ハーネス運用設定**であり、実装フェーズでワーカーの起動先 (impl / verify を counterpart へ) を決めるために develop の作業ツリーから取り込んだもの。develop 側でも未コミットの状態で保持されている
- **本 change のコミット対象から外す**。作業ツリーには残すため `git status` には現れるが、この change の成果物には含まれない
- したがって、この change が実際に持ち込む差分は本文書の対応表に挙げた実装・テスト・証跡のみであり、その範囲では未記録乖離は存在しない

以上により、本 change の成果物としての判定は **VALID** とする。

# レビュー結果: restore-maui-picker-selected-command (002 回目)

**日付**: 2026-08-29
**判定**: APPROVED

## サマリー

`afc15ae` からの全差分 (コミット済み 2 件と未コミットの作業ツリー・新規ファイル) をレビューした。facade の `SelectedCommand` は OneWay・既定 null の公開 API として追加され、native の確定通知を受けたときだけ、選択値と TwoWay バインド先の反映を終えてから 1 回実行される。実行引数は Cell の現在の `SelectionMode` ではなく届いた確定通知の種類で決まり、直接 setter・未知 Cell ID・確定通知なしでは発火しない。Sample 側は「通知先メンバー」が native と同じ「選択位置 TwoWay + 選択完了通知」の経路へ揃い、MAUI 固有の 2 契約 (バインド可能な完了通知 command / 選択要素列そのものの TwoWay) を集めた新画面が `MauiSpecific` 区分に追加された。デルタスペックの 2 Requirement / 12 Scenario はいずれも実装・テスト・実経路証跡のいずれかで裏付けられており、Critical / Major はない。

手元で再実行した結果: MAUI facade テスト **516 tests / 0 failures / 0 skipped**、Sample の Android ビルド **0 エラー** (警告 2 件はいずれも既存の `Metadata.xml` BG8A00 で本変更と無関係)。`comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` はいずれも検出 0 件。

指摘は Minor 2 件・Suggestion 3 件で、いずれも実装の修正を要求するものではない。

## 指摘事項

### [🟡 Minor] iOS の入力 Cell デモで cancel / dismiss の「操作前」証跡が欠けている

**該当箇所**: `tasks.md:36` (5.1) / `evidence/ios-input-cells-cancel-after.png` / `evidence/ios-input-cells-dismiss-after.png`

**問題点**: tasks 5.1 は「4 操作を、それぞれ操作前後で撮る」と定めて `[x]` になっているが、iOS の入力 Cell デモは confirm / reconfirm しか before/after の対が揃っていない。cancel と dismiss は `-after` だけで `-before` が無い。この 2 操作の主張は「直近イベント表示が操作前のまま変わらない」であり、対で残っていないと証跡単体では非発火を示せない (`ios-input-cells-cancel-after.png` は「担当者 → 田中 三郎」を映しているが、その直前の状態を映した画像が無いため、cancel 前後で同一だったことは画像から読み取れない)。Android 側は同じ画面で cancel / outsidetap / back の全対が揃っており、iOS だけ欠けている。

**推奨修正**: iOS の cancel / dismiss について `-before` を撮り直して `evidence/` へ追加するか、tasks 5.1 の記述と実際の撮り方 (直前操作の `-after` を before として兼用する) の食い違いを完了報告で明示する。なお非発火そのものは、両 OS の実選択面テスト (`ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt`) と Android 側の完全な対で冗長に担保されているため、優先度は低い。

### [🟡 Minor] 本 change と無関係な `kasane/config.yaml` の差分が作業ツリーに混ざっている

**該当箇所**: `kasane/config.yaml:50-65`

**問題点**: レビュー対象範囲 (`afc15ae` → 作業ツリー) に、ハーネス設定 `workers:` 節の追加が未コミットで含まれている。これは PickerCell の完了通知復元とも Sample 反映とも無関係な差分で、proposal の What Changes にも deviation.md にも記録がない。このままコミットすると、本 change の履歴に無関係な設定変更が混ざる。

**推奨修正**: コミット前に別変更として切り出すか、ハーネス運用設定の変更として付随修正の形で記録する。実装コードには影響しないため優先度は低い。

### [🔵 Suggestion] テスト足場 `PickerScope` が既存の `Writeback` とほぼ同一

**該当箇所**: `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:296` / `maui/KsSettingsView.Maui.Tests/NativeValueWritebackTests.cs:316`

**問題点**: `PickerScope(GatewayScope Scope, string CellId)` は `Gateway` / `Sink` / `For(cell)` の定義まで含めて `NativeValueWritebackTests` の private record `Writeback` の写しで、差は `PublishedCells()` の有無だけ。どちらも fixture の private 入れ子型なので共有できず、同じ足場が 2 つ育つ。

**推奨修正**: `Support/` へ共通の接続足場として切り出し、`PublishedCells()` のような用途固有の便宜は呼び出し側または拡張で持たせる。ただし本件は second-opinion-code-001 で「回帰検出力や仕様充足に影響しないスタイル上の提案」として一度降格済みであり、その判断を覆す新しい根拠は無い。今回 Sample 追加で差分がさらに増えたわけでもないため、方針維持でよい。

### [🔵 Suggestion] 新 Sample 画面の初期選択がリテラルの二重管理になっている

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/ViewModels/MauiSpecificCellFeaturesDemoViewModel.cs:11-12`

**問題点**: 初期選択が `_singleSelectedItem = "ダーク"` / `_multipleSelectedItems = new List<object> { "メール", "SMS" }` と、候補列 `SingleItems` / `MultipleItems` とは独立したリテラルで書かれている。候補の文言を直すと初期選択が候補に一致しなくなり、`SelectedItem` は候補に無い値を保持できないため「未選択」で起動するが、コンパイルもテストも落ちない。同じ Sample 内の `InputCellsDemoViewModel` は初期選択を `SampleMember.NotificationTargets[0]` のように候補側から引いており、書き方が揃っていない。

**推奨修正**: `SingleItems[1]` / `[MultipleItems[0], MultipleItems[2]]` のように候補列から引く形へ揃える。デモ専用コードのため優先度は低い。

### [🔵 Suggestion] 利用者向け移行表が「SelectedCommand は提供しない」のまま

**該当箇所**: `skills/ja/kssettingsview-aiforms-migration/references/api-mapping.md:186` / `skills/en/kssettingsview-aiforms-migration/references/api-mapping.md:186`

**問題点**: 両ファイルは `PickerCell.SelectedCommand` を「提供しない / 双方向バインドの裏の setter で受ける」と案内しており、本変更で公開された API と食い違う。

**推奨修正**: 本 change の Non-Goal (「`skills/` と README 群は直接更新しない」) とプロジェクト規約どおり、この場では触らない。蒸留で concepts に完了通知契約を反映したあと、ユーザーの明示依頼で `docs-refresh` を回す申し送りとして残す。second-opinion-code-001 と同じ申し送りの再掲。

## 確認した観点

**仕様充足**
- `specs/maui-cells/spec.md` の Requirement と 7 Scenario: 公開 API 形状 (型・既定値・`BindingMode.OneWay`)、書き戻し後の 1 回実行、単一/複数の引数選択、選択面表示中のモード変更に引数を依存させないこと、同値再確定での再書き戻し抑止と完了通知の維持、`CanExecute` 非確認、直接 setter・未知 Cell ID・非確定 dismiss の非発火。いずれも `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs` の 8 テスト (`[TestCase]` 展開で 10 ケース) が対応し、cancel / dismiss は native 実選択面テストが対応根拠 (tasks 3.5 の宣言どおり)。
- `specs/samples-maui/spec.md` の 2 Requirement と 5 Scenario: 「通知先メンバー」の経路差し替えと文言不変 (section header/footer・行 title・候補・初期選択 `[0, 2]`・`PageTitle` はいずれも iOS / Android と一字一句一致のまま)、新画面のルートメニュー登録・画面タイトル一致・選択値と受信回数の観測。実経路の証跡で確認した (例: `evidence/ios-maui-specific-single-reconfirm-{before,after}.png` は選択値「自動」不変のまま完了通知 1→2 回、`evidence/android-input-cells-reconfirm-{before,after}.png` は「担当者 → 高橋 次郎」から「通知先メンバー → …」への上書き)。
- 足場凍結: proposal / specs は差分に含まれず、書き換えられていない。`tasks.md` の変更は完了チェックのみ。
- deviation.md は無く、実装差分は proposal の What Changes の範囲 (`maui/` の facade・controller・テストと `samples/maui/`) に収まる。Native / Bridge / platform gateway に差分なし。

**堅牢性**
- 単一選択の `ApplyNativeValue` 展開は `FindCell` + `Write` と等価で、同値スキップと未知 ID の扱いが変わっていない。複数選択の早期 return 反転も「同値なら書き戻さない・完了通知はする」の最小変更で、maui/ADR-0012 のエコー抑止 (書き戻し入口の同値チェック) を壊していない。既存テスト `PickerMultiSelectionIgnoresOrderAndDuplicateOnlyNotifications` が今も通ることで裏取りした。
- `SelectedCommandProperty` を `AffectsSnapshot` に含めない判断は正しい (Command は輸送対象ではない)。`CanExecuteChanged` を購読しないため、リーク経路も増えていない。
- 完了通知は `_syncingSelection` ガードの外・書き戻しの後に呼ばれ、Command 内での再入は通常の dirty-set/flush へ合流する。Command 実行は native callback と同じ同期文脈で走るため、Description 更新などの連鎖も同一バッチに収まる。
- `ItemsSource` 未設定・空の退化構成では相互導出が走らず引数が古い値/null になり得るが、これは second-opinion-code-001 で「公開値そのものを引数と定める spec に従う」として降格済みの既知事項であり、再指摘しない。

**テスト**
- 全 516 件成功。追加テストのアサーションは検出力がある: 実行順の検証は `Execute` の callback 内で行い、後段で `ExecuteCount == 1` を確認しているため callback 未実行が素通りしない。モード取り違えの 2 テストは、実装が現在の `SelectionMode` を見る形に退行すると引数が `SelectedItem` ⇔ `SelectedItems` で入れ替わって落ちる。fake gateway / sink の経路に例外の握り潰しは無く、callback 内の `Assert` 失敗はテスト失敗として伝播する。
- 言い訳コメントによる実質スキップ、境界値・異常系の欠落は見当たらない。

**設計品質**
- maui/ADR-0008 (AiForms 互換命名 + 対応概念のある公開面のみ) と整合。native に選択完了 callback という対応概念があり、名前・型・既定値・binding mode は移植元と一致する。
- 同じライブラリ内で `CommandCell.Command` が `CanExecute` を行の実効無効に反映するのに対し、`PickerCell.SelectedCommand` は `CanExecute` を確認しない。これは spec が移植元互換として明示的に要求した差であり、`PickerCell.SelectedCommand` の XML doc に「実行可否は確認しない」と自己完結して書かれているため、仕様どおりとして受け入れる。
- `concepts/cross/conventions/sample-parity.md`: 「通知先メンバー」の経路差し替えは表示に現れずパリティを崩さない (むしろ native と経路が揃ってパリティが改善する)。新画面は「デモの主対象が platform 固有の公開 API と意味論」の例外に該当し、`SampleScreenCategory.MauiSpecific` に置かれてルートメニュー上でパリティ対象と区別されている。画面タイトルは `SampleScreen.All` の一元定義から `CreateTitledPage()` 経由で与えられ、メニュー文言と二重管理になっていない。
- `concepts/cross/conventions/comment-policy.md`: 追加・変更したコメントに change / レビュー通番・アーカイブ文書パス・デルタスペック構文キーワード・履歴記述は無い。`NotifySelectionCompleted` の remarks は外部文書を参照せず単独で理由が読める。
- `concepts/cross/conventions/runtime-behavior-verification.md`: 単体テストでは踏めない native 選択面 → gateway → controller → Cell の経路を、両 OS の Simulator / Emulator で 54 枚の証跡付きで確認しており、規約の要求を満たす。
- C# / MAUI のスタイル: フィールド `_camelCase` と型先頭配置、明示的な可視性、型名が右辺にある場合のみの target-typed `new()`、public API の XML doc、既存 Sample ページと同形の `InitializeComponent()` → `SampleTheme.Apply(Settings)` → `BindingContext` 設定、`x:DataType` によるコンパイル済みバインド。いずれも既存コードと一貫している。
- `ui/` が無い点は確認したうえで妥当と判断した。新画面は既存の `Section` / `PickerCell` と共用 `SampleTheme` の組み合わせに閉じており、新規の視覚デザイン判断 (mock の選定を要する論点) が無い。デルタスペックにも視覚パラメータの記述は無く UI lint に抵触しない。見た目の確認は `evidence/` の実機経路証跡が担っている。

## アクションプラン

1. (任意・低優先) `kasane/config.yaml` の `workers:` 差分をコミット前に本 change から切り離すか、記録する。
2. (任意・低優先) iOS の cancel / dismiss の `-before` 証跡を追加するか、tasks 5.1 の撮り方との差を完了報告に明記する。
3. (蒸留後) concepts への完了通知契約の反映と、ユーザーの明示依頼による `docs-refresh` での `skills/*/kssettingsview-aiforms-migration/references/api-mapping.md` の追従を申し送る。

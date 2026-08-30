# セカンドオピニオン: restore-maui-picker-selected-command (code-001)

**相方**: claude / **label**: so-code-restore-maui-picker-selected-command / **日付**: 2026-08-28 / **対象**: `kasane/changes/restore-maui-picker-selected-command/` と MAUI facade の実装差分

---

# レビュー結果: restore-maui-picker-selected-command

**判定**: **APPROVED** (Critical 0 / Major 0 / Minor 3 / Suggestion 2)

## サマリー

デルタスペックの Requirement / 7 Scenario は、実装・テストとも過不足なく対応している。発火境界 (native 確定通知のみ)・実行順 (公開値と TwoWay バインド先の反映後)・引数選択 (Single → `SelectedItem` / Multiple → `SelectedItems`)・`CanExecute` 非確認は移植元 AiForms の `InvokeCommand()` と 1 対 1 で一致しており、maui/ADR-0008 の互換方針にも沿う。単一選択側の `ApplyNativeValue` から `FindCell` + `Write` への展開は書き戻し意味論を変えておらず、複数選択側の早期 return 反転も「同値なら書き戻さない・完了通知はする」という spec どおりの最小変更に収まっている。指摘はいずれも実害の小さいドキュメント精度・テスト請求の正確さ・退化構成でのエッジであり、ブロッカーはない。

確認した観点: 再入・折り返し、同一通知での二重実行、`SelectedCommandProperty` を `AffectsSnapshot` に含めない判断、`CanExecuteChanged` を購読しないためリーク経路を増やしていないこと、`_syncingSelection` ガード下での相互導出、既定値 null・OneWay の分類規約との整合。

静的レビューのためビルド・テストは実行せず、ホスト側で確認済みの MAUI facade 513 件成功を前提とした。

## 指摘事項

### [Minor] 複数選択の完了引数が候補ゼロ構成でのみ null になり、移植元の非 null 保証と食い違う

**該当箇所**: `maui/KsSettingsView.Maui/PickerCell.cs:305` / `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1894`

`ItemsSource` が未設定または空の複数選択 Cell では相互導出が走らず、確定通知の空集合と現値 null が同値として書き戻しをスキップすると `Execute(null)` になる。候補がある状態から全解除した場合は空 `IList` が渡るため、同じ選択なしで引数が分かれる。移植元は選択面の初期化時に `SelectedItems` が null なら `ArrayList` を用意する。

推奨は、複数選択時に空列へ正規化するか、null 可を仕様とテストに明記すること。

### [Minor] 完了通知を追加したことが controller の comments に明記されていない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1866` / `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1878`

複数選択の remarks は同値書き戻し抑止だけを説明し、単一選択は `<inheritdoc/>` のみであるため、両メソッドに完了通知を必ず1回実行する旨を追記することを推奨する。

### [Minor] tasks.md 3.5 が主張する cancel 相当のテストが facade では実体化されていない

**該当箇所**: `tasks.md:18` / `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:136`

直接 setter のテストは cancel と区別できず、cancel の実体的な保証は iOS `PickerSelectionScreenTests` と Android `PickerSelectionSheetTest` に既に存在する。facade のテスト請求を実態に合わせるか、Native テストを対応根拠にすることを推奨する。

### [Suggestion] テスト足場が既存の共有 fixture と重複している

**該当箇所**: `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:176` / `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:231`

Picker 用 ViewModel、gateway 接続、ICommand fake の既存類似 fixture への共通化を候補とする。

### [Suggestion] 蒸留・docs-refresh 時の追従先が提供しないと明記されている

**該当箇所**: `kasane/concepts/maui/api/maui-facade.md` / `skills/ja/kssettingsview-aiforms-migration/references/api-mapping.md` / `skills/en/kssettingsview-aiforms-migration/references/api-mapping.md`

蒸留で concepts に完了通知契約を反映した後、docs-refresh の明示依頼で利用者向け移行表を追従することを推奨する。

## 突き合わせ結果

- 確定: ホスト側と相方側はいずれも Critical / Major なしで `APPROVED`。実装修正を要求する指摘なし
- 採用: cancel Scenario の実体的な検証根拠は facade の無通知テストではなく既存 Native テストにある、という指摘。独立 `ksn-verify` で両 OS の既存テストとの対応を再確認する
- 降格: 候補ゼロ時の null / 空列。デルタスペックは現在の `SelectedItems` 公開値そのものを引数と定め、`SelectedItems` は nullable。引数だけの正規化は公開値との一致を崩し、公開値まで空列化すると候補不在中の選択保留契約へ波及するため、本 change の不具合とは扱わない
- 降格: controller comments。既存の同値書き戻し抑止の説明は現在も正しく、実装は `NotifySelectionCompleted()` の命名と `PickerCell` 側の XML doc だけで読める。重複説明を追加しない
- 降格: テスト fixture の共通化。回帰検出力や仕様充足に影響しないスタイル上の提案であり、現在の小さな専用 fixture を維持する
- 降格: docs-refresh は本 change の Non-Goal とプロジェクト規約どおり。蒸留後にユーザーの明示依頼で追従する申し送りとして保持する
- 未解決: なし

# セカンドオピニオン: add-maui-basic-input-cells (code-001)
**相方**: codex / **日付**: 2026-08-10 / **対象**: working tree vs HEAD (9c2a4c0) 全差分 (untracked 含む 147 エントリ) + change アーティファクト一式
---
## 判定: CHANGES_REQUESTED

Critical 0件、Major 3件、Minor 1件、Suggestion 1件です。ホスト側の全緑結果は前提として採用し、ビルド・テストは再実行していません。ファイルへの書き込みも行っていません。

### Major — アイコン解決の世代番号が再利用され、古い結果が最新画像を上書きする

該当箇所: [KsSettingsController.cs](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:333)、[同 ResolveIcon](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:773)、[同 CompleteIcon](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:798)、[同 UnregisterCell](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1163)

問題点:

`ClearRegistrations` と `UnregisterCell` がセル単位の `_iconGenerations` を削除するため、同じセルを再登録すると世代番号が1から再利用されます。例えば次の順序で latest-wins が破られます。

1. Icon Aを世代1で解決開始
2. セルを削除し再登録
3. Icon Bを新しい世代1で解決
4. Bが完了
5. 古いAが完了

AとBはどちらも世代1であり、グローバルな `_imageGeneration` も変わらないため、古いAが検査を通過してBを上書きします。これは [maui-cells/spec.md](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:153) の latest-wins 要件に反します。

推奨修正:

世代番号を再利用しない単調増加トークンにするか、解決要求ごとの一意なトークンをセルの最新要求として保持してください。登録解除・再構築時も旧要求を確実に無効化する必要があります。アイコン変更→削除→再追加→完了順逆転を再現するテストも追加してください。

### Major — MAUI画像サービスの破棄契約を履行していない

該当箇所: [iOS KsImageResolver.cs](maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:38)、[Android KsImageResolver.cs](maui/KsSettingsView.Maui/Platforms/Android/KsImageResolver.cs:38)

問題点:

`GetImageAsync` の結果から `.Value` だけを取り出し、サービス結果自体を破棄も保持もしていません。使用中のMAUI 10.0.70アセンブリでは結果型が `IDisposable` であり、`Dispose` 時に登録された後処理が実行されます。[MicrosoftのAPI資料](https://learn.microsoft.com/en-us/dotnet/api/microsoft.maui.imagesourceserviceresult?view=net-maui-10.0)でもこの破棄契約が確認できます。

結果オブジェクトにファイナライザーはないため、アイコンの反復変更や接続し直しでストリーム・ネイティブ資源などの後処理が実行されない可能性があります。

推奨修正:

結果と画像を一体の所有済みリースとして扱い、画像が使用中の間は結果を保持し、置換・解除・失敗・stale判定時に確実に破棄してください。取得直後の単純な `using` は画像寿命を短くする可能性があるため避け、現在表示中の結果の寿命と同期させるのが安全です。偽の破棄可能結果を使った置換・解除・stale完了のテストも必要です。

### Major — DataTemplateSelectorの例外契約を満たしていない

該当箇所: [KsItemsSourceBinder.cs](maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs:349)、[DataTemplateSelectorTests.cs](maui/KsSettingsView.Maui.Tests/DataTemplateSelectorTests.cs:107)

問題点:

`selector.SelectTemplate` を直接呼んでいます。MAUI 10では、セレクターが別の `DataTemplateSelector` を返した場合に `NotSupportedException` が発生します。一方、[design.md](kasane/changes/add-maui-basic-input-cells/design.md:104) と [maui-cells/spec.md](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:207) は、このケースを既存ファサードと同じ `InvalidOperationException` 契約に含めています。

申し送り#10の「ファサードでは介入できない」という判断は妥当ではありません。呼び出し境界で変換できます。現在のテストもnullと不正型のみで、セレクター返却ケースを検証していません。

推奨修正:

該当する `NotSupportedException` を捕捉し、内部例外を保持した `InvalidOperationException` に変換してください。SettingsView直下とSection内の両経路について回帰テストを追加してください。

### Minor — 日本語IMEの実機検証が未完了

該当箇所: [tasks.md](kasane/changes/add-maui-basic-input-cells/tasks.md:74)、[design.md](kasane/changes/add-maui-basic-input-cells/design.md:124)

問題点:

EntryCellの「日本語IME・カーソル位置を実機で確認」が未チェックです。提示された検証結果はシミュレータ／エミュレータの視覚照合であり、実IMEの変換中入力やカーソル挙動を保証しません。設計自身も、文字単位のネイティブ→MAUI→ネイティブ往復をリスクとして明記しています。

推奨修正:

iOS/Android実機で、未確定文字列、変換候補選択、文中カーソル編集、連続入力を確認し、証跡を残してから完了扱いにしてください。

### Suggestion — AndroidバインディングにPickerCellのWhenMappingsが露出する

該当箇所: [Metadata.xml](maui/android/KsSettingsView.Binding.Android/Transforms/Metadata.xml:27)

問題点:

`KsSettingsBridge.WhenMappings` は除去していますが、`KsBridgePickerCell.WhenMappings` は対象外です。生成されたバインディングの公開面にKotlinコンパイラ生成型が残ります。実行時障害ではなく内部バインディングの表面汚染なのでSuggestionとします。

推奨修正:

PickerCell側にも対応する `remove-node` を追加し、変換コメントも実際の対象と一致させてください。

## 申し送り19項目の評価

- 修正が必要: #10。仕様上の例外型に変換できるため、現在の採否判断は不適切です。
- 改善推奨: #2。PickerCellの `WhenMappings` が残っています。
- #1、#3〜#9、#11〜#13、#15〜#18について、今回の静的レビューで判断を覆す追加問題は確認しませんでした。
- #14、#19および合意済みdeviationは、依頼どおり違反として扱っていません。

なお、`git status --short` を基準にtracked/untrackedを含む全147エントリを棚卸しし、対象範囲を確認しました。`git diff --check` による空白・パッチ形式上の問題はありませんでした。


## 突き合わせ結果 (orchestrator 判定 / 2026-08-10)

ホスト側 review-001.md (CHANGES_REQUESTED: Major 2 / Minor 7 / Suggestion 9) との突き合わせ。

- **採用** (相方のみ・根拠強 — ホスト側の見逃しとして修正サイクルへ):
  - Major「アイコン世代番号の再利用で latest-wins が破れる」— セル削除→再登録で `_iconGenerations` が1から再採番され、旧解決要求が検査を通過する実害シナリオが具体的。ホストは icon 世代管理を肯定評価しており相補的な検出
  - Major「MAUI 画像サービス結果の破棄契約不履行」— `IImageSourceServiceResult` は IDisposable。実装は `.Value` のみ取り出しで破棄経路がない
  - Major「DataTemplateSelector の NotSupportedException は呼び出し境界で InvalidOperationException に変換可能」— ホスト handoff #10 評価は「内部送出を防げない」ことを「介入不能」と読み違えており、相方の指摘が技術的に正しい (捕捉・変換は可能)。ホスト評価を覆して採用
- **確定** (双方一致): 「KsBridgePickerCell.WhenMappings の束縛面露出」(相方 Suggestion = ホスト Minor-5。重要度はホスト判定の Minor を基準に、変更由来である点はホスト側の特定が正確)
- **整理** (新規指摘ではない): 相方 Minor「日本語 IME 実機検証未完了」= tasks 7.2 の既知保留 (オーナー実機待ち)。修正サイクルの対象にしない
- **ホストのみの指摘** (相方は言及なし・そのまま確定): Major-1 (Android パスワードマスク破れ) / Major-2 (sample-parity 未記録) / Minor-3〜9 / Suggestion 1〜9

採用 3 / 確定 (一致) 1 / 降格 0 / 未解決 0。

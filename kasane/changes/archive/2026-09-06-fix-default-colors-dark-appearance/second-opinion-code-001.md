# セカンドオピニオン: fix-default-colors-dark-appearance (code-001)

**相方**: codex / **label**: so-code-fix-default-colors-dark-appearance / **日付**: 2026-09-05 / **対象**: 作業ツリーの未コミット差分全体 (android/kssettingsview, android/kssettingsview-bridge, ios/Sources/KsSettingsViewUI, ios/Tests/KsSettingsViewUITests, samples/android の SampleTheme) と kasane/changes/fix-default-colors-dark-appearance/ の成果物

---

# レビュー結果: fix-default-colors-dark-appearance

**日付**: 2026-09-05  
**指摘数**: Critical 0 / Major 2 / Minor 1 / Suggestion 1

## サマリー

色解決の構造、iOS の Dynamic Color、`CGColor` の再解決は概ね仕様に沿っています。提示されたテスト結果も成功しています。

一方、Android の再生成なし外観変更には古い `KsThemedContext` が残る経路があり、選択 UI を閉じて再度開くシナリオのテストも実経路を通っていません。また、MAUI Android の「Activity 再生成なし」という完了根拠は現行 Sample 実装と矛盾しています。

ビルド・テストは指示どおり再実行していません。

## 指摘事項

### [Major] 外観変更後も既存 ViewHolder が古い `KsThemedContext` を再利用する

**該当箇所**:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsThemedContext.kt:28`
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsThemedContext.kt:93`
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:206`
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarDialog.kt:199`
- `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeAppearanceResolutionTest.kt:387`
- `kasane/changes/fix-default-colors-dark-appearance/tasks.md:51`

**問題点**:

`ksThemedContext()` は、受け取った Context が既に `KsThemedContext` なら無条件で返します。

```kotlin
if (this is KsThemedContext) return this
```

しかし同ファイルの説明どおり、`ContextThemeWrapper` は生成時のテーマを保持するため、Activity を再生成しない `uiMode` 変更ではラッパーを作り直す必要があります。

既存 ViewHolder の `root.context` はすでにラッパーです。外観変更後に Picker や DatePicker を閉じて再度開いても、上記早期 return によって古い Material テーマを再利用する可能性があります。明示的な背景色は暗くなっても、テーマ属性に依存するダイアログ内部要素は古い外観のままになり得ます。

対応テストも実際のセルをタップしてダイアログを再生成しておらず、`PickerSheetStyle` に `darkTheme` を直接渡すだけなので、この経路を検出できません。仕様の「閉じて再度開く」シナリオに対して、`tasks.md` の完了チェックを裏付けていません。

**推奨修正**:

- `KsThemedContext` でも保持している night mode と現在値を比較し、不一致なら base Context から新しいラッパーを生成する。
- またはダイアログ生成時に常に host/base Context まで unwrap してから `ksThemedContext()` を適用する。
- 実際の PickerCell／DatePickerCellを表示し、タップ、外観変更、閉じる、再タップまで通すテストを追加する。
- 修正と実経路検証が完了するまで `tasks.md` の該当項目を未完了へ戻す。

### [Major] MAUI Android の「Activity 再生成なし」という完了根拠が現行 Sample 実装と矛盾する

**該当箇所**:

- `kasane/changes/fix-default-colors-dark-appearance/tasks.md:11`
- `kasane/changes/fix-default-colors-dark-appearance/ui/brief.md:40`
- `kasane/changes/fix-default-colors-dark-appearance/ui/brief.md:60`
- `samples/maui/KsSettingsView.Sample.Maui/Platforms/Android/MainActivity.cs:41`

**問題点**:

成果物は同一画面・同一 View を保ったままライトからダークへ切り替えたとしていますが、現行 `MainActivity.OnConfigurationChanged()` は `Recreate()` を呼び出しています。

したがって、スクリーンショットの見た目が切り替わっていても、次を証明できません。

- 同一 `KsSettingsView` インスタンスが維持されたこと
- `AppThemeBinding` の変更が既存 facade から Bridge、Native View まで伝播したこと
- 再生成後の初期構築で暗色になっただけではないこと

成果物からは一時的に `Recreate()` を無効化して検証した事実も確認できず、現行コードは記載された前提を反証しています。

**推奨修正**:

- `Recreate()` を使わない検証用ホストで同じ手順を再実施する。
- Activity と `KsSettingsView` のインスタンス識別情報を、個人情報を含まないログなどで前後比較する。
- `AppThemeBinding` の変更通知が既存 Native View の `applyTheme` まで到達したことを確認する。
- それが意図した保証ではない場合は、成果物の「再生成なし」という記載を訂正し、該当ゲートを未完了とする。

### [Minor] 新規コメントがコメント規約の内部事情・履歴記述禁止に抵触する

**該当箇所**:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:22`
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewDefaults.kt:11`
- `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewDefaultsTest.kt:12`
- `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeTest.kt:46`
- `ios/Tests/KsSettingsViewUITests/ThemeDefaultColorAppearanceTests.swift:6`
- `ios/Tests/KsSettingsViewUITests/ThemeDefaultColorAppearanceTests.swift:165`

**問題点**:

公開 KDoc に `core/ADR-0030` という内部識別子が含まれています。また、「本 change 前」「新規」といった変更履歴・進捗を表すコメントがあります。

`kasane/handbook/cross/comment-policy.md` では、公開 API コメントへの内部プロジェクト用語と、コードから読み取れない変更履歴の記載を禁止しています。

**推奨修正**:

公開 KDoc は現在の API 契約だけで自己完結させてください。テストコメントも「ライト外観の固定期待値」「セル全体の既定値」のように、現在成立すべき条件として書き換えてください。

### [Suggestion] 視覚照合記録の初回不一致と最終状態が同一セクション内で矛盾して見える

**該当箇所**:

- `kasane/changes/fix-default-colors-dark-appearance/ui/brief.md:56`
- `kasane/changes/fix-default-colors-dark-appearance/ui/brief.md:62`
- `kasane/changes/fix-default-colors-dark-appearance/ui/brief.md:125`

**問題点**:

前半には Android Native が「不一致」「未修正・判断待ち」と残っていますが、後半では解消済みとされています。さらに初回画像は上書き済みのため、記載された初回不一致を現在の evidence から監査できません。

**推奨修正**:

前半を「初回照合時・後続で解消」と明示し、冒頭に最終状態の一覧を置いてください。履歴を証跡として残すなら、初回画像と最終画像を別名で保持するのが明確です。

## 判定

**CHANGES_REQUESTED**

Android の実経路上のテーマラッパー再生成と、MAUI Android の実行時証拠を修正・再検証する必要があります。

---

## 突き合わせ結果 (ホスト review-001 との照合、2026-09-05)

| 相方の指摘 | ホスト側 | 採否 | 根拠 |
|---|---|---|---|
| [Major] 外観変更後も既存 ViewHolder が古い `KsThemedContext` を再利用する (`ksThemedContext()` の早期 return) | 指摘なし (「ContextThemeWrapper が resources を委譲するため構成変更後も正しい」と判定) | **採用 (要実証)** | 該当箇所と実害シナリオ (選択面を閉じて開き直したときテーマ属性依存の内部要素が古い外観) が特定されている。ホスト側の判定は Configuration の読み出しについてで、テーマ属性の解決については確認していない。修正サイクルで実経路 (タップ → 外観変更 → 閉じる → 再タップ) のテストを書いて実証し、古ければ修正する |
| [Major] MAUI Android の「Activity 再生成なし」という完了根拠が Sample の `MainActivity.OnConfigurationChanged()` の `Recreate()` と矛盾 | 指摘なし | **採用** | `samples/maui/.../Platforms/Android/MainActivity.cs` が `Recreate()` を呼ぶのは proposal Non-Goals にも記載の事実で、spike (tasks 0.1 注記) と brief.md の「表示中に描き替わった (再生成なし)」は現行 Sample では証明できない。`Recreate()` を一時的に無効化して spike と 8.3 の表示中追随を再実施し、記録を訂正する |
| [Minor] 新規コメントの内部識別子 (公開 KDoc の ADR ID) と履歴記述 (「本 change 前」「新規」) | Minor で一致 (公開 KDoc の ADR ID 2 箇所) | **確定** | 双方一致。相方の追加分 (テストコメントの履歴記述 4 箇所) も同じ修正で閉じるため同時に対処 |
| [Suggestion] brief.md の照合記録が初回不一致と最終状態で矛盾して見える / 初回画像が上書き済み | 指摘なし | **降格 (記録は改善)** | 好み・構成の域。ただし冒頭に最終状態の一覧を置く改善は再照合の記録更新時に安価に行う。初回画像の別名保持は行わない (乖離の内容は文章で記録済み) |

ホスト側のみの指摘 (相方は未検出): Minor 3 件 (`init` の装飾組み立て順序で `Unspecified` が ARGB へ落ちる経路 / 公開 KDoc の ADR ID / factory を外観と食い違って渡した場合の契約明示) と Suggestion 3 件。いずれもホスト側判定どおり修正サイクルへ含める。

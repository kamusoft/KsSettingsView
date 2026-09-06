# セカンドオピニオン: add-sample-dark-mode-toggle (code-001)
**相方**: codex / **label**: so-code-add-sample-dark-mode-toggle / **日付**: 2026-09-05 / **対象**: 作業ツリーの未コミット変更 (HEAD cedaf04 との差分。ios/Sources・ios/Tests・samples/{ios,android,maui})
---
# レビュー結果: `add-sample-dark-mode-toggle`

## Findings

### [Major] MAUI Androidでシステム外観変更時に表示中ページを維持・更新できない

該当箇所:

- `samples/maui/KsSettingsView.Sample.Maui/Platforms/Android/MainActivity.cs:14`
- `samples/maui/KsSettingsView.Sample.Maui/Platforms/Android/MainActivity.cs:22`
- `samples/maui/KsSettingsView.Sample.Maui/SampleThemeFollower.cs:47`
- `samples/maui/KsSettingsView.Sample.Maui/SampleTheme.cs:136`
- `samples/maui/KsSettingsView.Sample.Maui/App.cs:15`
- `kasane/changes/add-sample-dark-mode-toggle/specs/samples-maui/spec.md:24`
- `kasane/changes/add-sample-dark-mode-toggle/specs/samples-maui/spec.md:31`
- `kasane/changes/add-sample-dark-mode-toggle/tasks.md:27`
- `kasane/changes/add-sample-dark-mode-toggle/ui/brief.md:76`

問題点:

`MainActivity` の `ConfigurationChanges` から `ConfigChanges.UiMode` が外されています。このため端末のライト／ダーク変更は `OnConfigurationChanged` ではなく Activity 再生成として処理されます。コード内のコメントも、再生成によってデモ画面の入力状態が失われることを明記しています。

一方、表示中ページの再配色は `RequestedThemeChanged` を購読する `SampleThemeFollower` に依存しています。MAUI Androidでは `UiMode` をActivity側で処理する経路によってプラットフォームテーマの更新とテーマ変更通知が行われるため、現在の構成ではこの追随経路が保証されません。MAUI標準テンプレートも `ConfigChanges.UiMode` を保持しています。[MAUI標準MainActivity](https://github.com/dotnet/maui/blob/main/src/Templates/src/templates/maui-mobile/Platforms/Android/MainActivity.cs)、[Applicationのテーマ管理実装](https://github.com/dotnet/maui/blob/main/src/Controls/src/Core/Application/Application.cs)

少なくとも、基本Cellや入力Cellのページを表示した状態で端末外観を変更すると、ページがその場でdarkプリセットへ切り替わるのではなくActivity再生成によってルートメニューへ戻り、入力・ナビゲーション状態を失います。これは「外観が切り替わったとき表示中のページも追随する」というSHALL要件に反します。

現在の証跡はルートメニューの `maui-android-menu-system-device-dark.png` のみで、Themeを明示したデモページを表示したまま切り替える経路を証明していません。したがって、`tasks.md:34` の全実行面確認を完了扱いにはできません。

推奨修正:

- `ConfigChanges.UiMode` を復元し、MAUIのテーマ変更ライフサイクルを通す。
- 解決済みリソースを持つ既存Viewが更新されない場合は、`RequestedThemeChanged` の処理内で対象View／Themeを再適用または再構築する。
- MAUI Androidで基本Cellまたは入力Cellページを表示したまま、システム外観をライトからダークへ変更し、次を実機確認する。
  - 同じページに留まる
  - dark側の`SampleTheme`へ切り替わる
  - ナビゲーションと入力状態が保持される
- その証跡を追加してから5.1および5.2を完了扱いにする。

### [Minor] Androidエミュレーターの実シリアルが変更アーティファクトに残っている

該当箇所:

- `kasane/changes/add-sample-dark-mode-toggle/ui/brief.md:71`

問題点:

`emulator-5554` はAndroid端末シリアルの実値です。`ksn-core` の証跡規約は、本文へ手書きする値についても実値を残さず、Androidシリアルを `<android-serial>` に置換するよう要求しています。今回のlintが0件なのは、検出パターンが `serial=`、`adb -s` などの文脈を必要とするためであり、規約適合を意味しません。

推奨修正:

`Emulator (emulator-5554 / 1080x2340)` を、例えば `Emulator (<android-serial> / 1080x2340)` に変更してください。

## 確認結果

合意済みの `deviation.md` 2項目は指摘対象から除外しました。特に、オーナー裁定済みのiOS本体変更はスコープ違反として扱っていません。

それ以外については、以下に新たな問題は確認できませんでした。

- 3プラットフォーム間の文言、日付範囲、色ロール
- iOSの提示外観引き継ぎと対応テスト
- Swift 6言語モードの設定復元
- モックと4実行面の最終画像の視覚的一致
- コメントポリシーおよびサンプルパリティ

ビルド・テストは静的レビューの制約に従って再実行せず、提示された結果を前提としています。

## 判定

**CHANGES_REQUESTED**

MAUI Androidのシステム外観変更はデルタスペックの主要Scenarioに直接関わるため、修正と表示中デモページでの再検証が必要です。

## 突き合わせ結果 (ホスト review-001.md との照合、2026-09-05)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| MAUI Android: `ConfigChanges.UiMode` 除去により、デモページ表示中の端末外観変更が Activity 再生成になり「表示中のページも追随する (SHALL)」を満たせない疑い | 相方のみ (Major) | **採用** (Major) | spec samples-maui「外観に応じた Sample Theme の差し替え」の SHALL と機構 (UiMode を Activity が処理する経路が MAUI のテーマ更新通知の前提) を特定。証跡がルートメニューのみで、Theme 明示ページを表示したままの切替が未確認なのも事実。修正サイクルで、デモページ表示中の端末外観変更を実測し、ページ・状態が失われるなら `UiMode` を復元して `RequestedThemeChanged` 側で再適用する形へ直す (実測で spec を満たしていれば証跡を添えて降格) |
| brief.md に Emulator のシリアル実値 `emulator-5554` が残る | 相方のみ (Minor) | **採用** (Minor) | ksn-core references/evidence.md の置換規律に該当 (lint は文脈付きパターンしか拾わない)。`<android-serial>` へ置換 |
| PickerCell 選択面の実機証跡なし (提示経路 2 箇所のうち 1 箇所しか撮っていない) | ホストのみ (Major) | 確定 (Major) | process L-003 |
| `Configuration()` の fontScale 巻き込み / KDoc 参照先誤り / `SampleThemeFollower` の購読解除不能 | ホストのみ (Minor ×3) | 確定 | — |
| 「選択中」文言の一元化 / `SampleAppearance.kt` 切り出し / inputView ピッカーの撮影 / `remember` | ホストのみ (Suggestion ×4) | 確定 (Suggestion、修正サイクルで対処) | いずれも数行で閉じる |

未解決 (両者矛盾): なし。採用 2 / 降格 0。

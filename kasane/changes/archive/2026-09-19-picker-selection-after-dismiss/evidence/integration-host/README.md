# 統合ホスト・MAUI Sample の end-to-end 確認 (Simulator / Emulator)

`tasks.md` 7.3 のうち Simulator / Emulator 分の証跡。C# → Native → 閉じ切り通知 → `SelectedCommand` の
経路が両 OS で成立することと、閉じ切り通知を足した後も検証ホストの完了条件が崩れていないことを確認した。
実機 (iPhone 11) の導線確認はオーナー担当で未実施。

## 実行環境

| 対象 | 環境 |
|---|---|
| iOS | iPhone 17 Pro Simulator (iOS 26.0) / Xcode 26.5 / Debug |
| Android | Pixel 6 Emulator (Android 12 / API 31) / Debug |

Native (Swift / Kotlin) の変更をリンクし直すため、facade・binding・各ホスト・MAUI Sample の
`bin` / `obj` を捨ててから再ビルドした。iOS はアプリ実行ファイルに閉じ切り通知のシンボルが
含まれることを確認したうえで install している。

Android の配備は `-t:Run` に `EmbedAssembliesIntoApk=true` を添えた。この Emulator では Fast Deployment の
ツール配備が `XA0126` で失敗するため。アセンブリを apk に載せる形なので、C# と Kotlin (aar) の
世代ずれは起きない。

## `SelectedCommand` の閉じ切り実行 (MAUI Sample・MAUI 固有 Cell 機能デモ)

検証ホスト (IntegrationHost / MauiHost) は `SelectedCommand` を持つ PickerCell を置いていないため、
この経路は MAUI Sample の「MAUI 固有 Cell 機能デモ」で確認した。Cell の description が
「バインド先: <選択> / 完了通知: N 回」を表示する。

| 操作 | iOS / Android で確認した結果 |
|---|---|
| 単一選択で「ライト」を確定 | 選択面が閉じ切った後に画面が「バインド先: ライト / 完了通知: 1 回」へ変わる |
| 複数選択で「プッシュ」を足して確定 | 「バインド先: メール, プッシュ, SMS / 完了通知: 1 回」へ変わる |
| 単一選択を開いてキャンセルで閉じる | 完了通知の回数は 1 回のまま増えない |

証跡: `maui-selected-command-ios-completed.png` / `maui-selected-command-android-completed.png`
(いずれもキャンセル操作の後に撮影。両 Section が「完了通知: 1 回」のままであることを兼ねて示す)

## 検証ホストの完了条件

`kasane/handbook/maui/integration-host-verification.md` の完了条件を両 OS で確認した (結果は画面で
確認済み。画像は上記 2 枚のみ残す)。

- IntegrationHost: 固定シナリオの表示が規約の表と一致し、両 OS で一致した。「解放 → 再生成」後も
  規約の表 (root header / footer が消え、テーマ=「解放中に更新」・言語=`Français`・
  「通知設定 (解放中に更新)」・Section header がオレンジ) と一致した
- MauiHost: 「設定画面を開く」→ テーマの ValueText が「ライト」、「ValueText を更新」で「更新 1」、
  離脱中更新後の再訪問で「更新 2」、離脱中の Cell 追加後の再訪問で「追加 1」を確認した
- MauiHost の「外観追随ボタン」は、設定画面を表示したまま OS の外観を切り替えると両 OS で
  ライト=緑 / ダーク=マゼンタに描き直された

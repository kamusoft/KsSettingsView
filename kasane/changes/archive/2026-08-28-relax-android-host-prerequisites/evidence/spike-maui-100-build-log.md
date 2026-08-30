# スパイク: 利用者アプリだけ MAUI 10.0.100 / ライブラリ側は 10.0.70 のまま

判定: **成立** (追加措置なしでそのまま動く)

## 一時変更 (検証後に復元済み)

`samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj` の
`<MauiVersion>` を `10.0.70` → `10.0.100` の 1 行のみ。

MauiVersion の共有定義 (Directory.Build.props / Directory.Packages.props) はリポジトリに存在せず、
`MauiVersion` プロパティを持つのは Sample.Maui と `maui/spike/app/KsBindingSpikeApp` の 2 csproj のみ。
ライブラリ側 (`maui/KsSettingsView.Maui`・`maui/android/KsSettingsView.Binding.Android`・
`maui/tests/*`) は `Microsoft.Maui.Controls` を `10.0.70` 直書きで参照しているため、波及なし
(下表のとおり KsSettingsView.Maui 単体の解決は 10.0.70 のまま)。

## 解決された実体版 (obj/project.assets.json、net10.0-android)

| パッケージ | 10.0.70 時点 (baseline) | 10.0.100 (Sample.Maui) | 差分 |
| --- | --- | --- | --- |
| Microsoft.Maui.Controls | 10.0.70 | 10.0.100 | 上がる (意図どおり) |
| Microsoft.Maui.Core | 10.0.70 | 10.0.100 | 上がる (意図どおり) |
| Xamarin.AndroidX.Compose.Runtime | 1.11.4 | 1.11.4 | 変化なし |
| Xamarin.AndroidX.Compose.Runtime.Annotation | 1.11.4 | 1.11.4 | 変化なし |
| Xamarin.AndroidX.Compose.UI | 1.11.4 | 1.11.4 | 変化なし |
| Xamarin.AndroidX.Compose.Foundation | 1.11.4 | 1.11.4 | 変化なし |
| **Xamarin.AndroidX.Compose.Material3** | **1.4.0.3** | **1.4.0.3** | **変化なし** (事前懸念は不発) |
| Xamarin.AndroidX.Activity | 1.13.0.1 | 1.13.0.1 | 変化なし |
| Xamarin.AndroidX.NavigationEvent | 1.1.2.1 | 1.1.2.1 | 変化なし |
| Xamarin.AndroidX.Fragment | 1.8.9.3 | 1.8.9.3 | 変化なし |
| Xamarin.AndroidX.AppCompat | 1.7.1.4 | 1.7.1.4 | 変化なし |
| Xamarin.AndroidX.Core | 1.19.0.1 | 1.19.0.1 | 変化なし |
| Xamarin.Google.Android.Material | 1.12.0.5 | 1.12.0.5 | 変化なし |

参考: 同時刻の `maui/KsSettingsView.Maui` 単体の解決は Microsoft.Maui.Controls/Core 10.0.70、
AndroidX 群は上表と同一。つまり **MAUI 10.0.70 と 10.0.100 の AndroidX 依存集合は同一**で、
Gradle コンパイル版 (compose BOM 2025.11.01 / compose 1.11.4 / material3 1.4.0) との組は崩れない。

## restore / build

```
$ dotnet restore samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj
  復元対象のプロジェクトを決定しています...
  ... KsSettingsView.Sample.Maui.csproj を復元しました (8.03 秒)。
  4 個中 3 個の復元対象のプロジェクトは最新です。
```
NU 系のエラー・警告はゼロ (NU1107 / NU1605 / NU1608 いずれも発生せず)。
直前のピン留めスパイク (spike-consumer-pin-build-log.md) で必要だった
`NoWarn` / 直接 PackageReference / `ExcludeAssets="all"` は**一切不要**。

```
$ dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj -f net10.0-android
  ...
  BUILD SUCCESSFUL in 1s   (Gradle: 88 actionable tasks, 88 up-to-date)
  ビルドに成功しました。
    0 個の警告
    0 エラー
```

## 実機検証 (Pixel 6a / Android 16, `dotnet build -t:Run` で正規デプロイ)

| 確認項目 | 結果 | 証跡 |
| --- | --- | --- |
| 基本 Cell 7 種の表示 (CommandCell / LabelCell / SwitchCell / CheckboxCell / RadioCell ほか) | OK | spike-maui-100-01, -02 |
| TimePicker シートの提示 | OK | spike-maui-100-03 |
| TimePicker の OK 確定 (シート閉じ・値保持) | OK | spike-maui-100-04 |
| カレンダーダイアログの提示 | OK | spike-maui-100-05 |
| 日付選択 (6/1 → 6/18) | OK | spike-maui-100-06 (ヘッダー「2026年6月18日」) |
| 年選択ペインの展開 | OK | spike-maui-100-06 |
| **ダイアログを開いたまま横回転 → 生存・選択保持** | OK | spike-maui-100-07 |
| 縦復帰でも生存・選択保持 | OK | spike-maui-100-08 |
| OK 確定 (値反映・変更イベント発火) | OK | spike-maui-100-09 (「最後のイベント: 予約日 → 2026/06/18」) |

logcat (`adb logcat -d`、操作全体をカバーする 7,581 行) の検査結果:

```
$ grep -c "FATAL EXCEPTION"       => 0
$ grep -c "NoSuchMethodError"     => 0
$ grep -c "NoClassDefFoundError"  => 0
```
`AndroidRuntime` タグの出現 60 件はすべて `com.android.commands.{am,content,uiautomator}`
(操作自動化ヘルパのプロセス) 由来で、対象アプリのクラッシュではない。
対象アプリ関連の W レベルは fast deploy の `xamarin.sync` 実行 (SELinux avc: granted) と
デプロイ時のプロセス入れ替え (`app died, no saved state`) のみ。

静止画は目視で確認済み。写り込みは Sample の架空デモデータ
(`Tanaka Taro` / `tanaka.taro@example.com` / `090-0000-0000`) のみで、実在の個人情報は含まない。

## 後始末

- `MauiVersion` を `10.0.70` へ復元し、`dotnet restore` で解決版が baseline に戻ることを確認
- `dotnet build maui/KsSettingsView.slnx` が緑に戻ることを確認
- 端末へ現行版 (MauiVersion 10.0.70) の Sample.Maui を再デプロイ

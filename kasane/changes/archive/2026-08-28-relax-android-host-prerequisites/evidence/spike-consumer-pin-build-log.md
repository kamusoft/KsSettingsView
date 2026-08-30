# スパイク: 利用者アプリ側 csproj での Compose 旧版ピン留め (ビルドログ抜粋)

## 段階1: binding の Compose を 1.7.5.1 / material3 1.3.1.2 へ、Gradle BOM を 2024.10.01 へ (利用者側ピンなし)
=> restore で NU1107 (パッケージ 1.7.5.1 自身のメタデータ不整合)

error NU1107: Xamarin.AndroidX.Compose.UI.Geometry のバージョンの競合が検出されました。
 KsSettingsView.Binding.Android -> Xamarin.AndroidX.Compose.UI 1.7.5.1 -> Xamarin.AndroidX.Compose.UI.Android 1.7.6.1 -> Xamarin.AndroidX.Compose.UI.Geometry (>= 1.7.6.1 && < 1.7.7)
 KsSettingsView.Binding.Android -> Xamarin.AndroidX.Compose.UI 1.7.5.1 -> Xamarin.AndroidX.Compose.UI.Geometry (>= 1.7.5.1 && < 1.7.6).

## 段階2: compose 1.7.6.1 / BOM 2024.12.01 へ是正 (利用者側ピンなし)
=> restore は通り、D8 で二重定義 (既知の失敗モードを再現)

java error JAVA0000: Type androidx.compose.runtime.Immutable is defined multiple times:
  /Users/<USER>/.nuget/packages/xamarin.androidx.compose.runtime.annotation.android/1.11.3.1/buildTransitive/net10.0-android36.0/../../aar/runtime-annotation-android.aar:classes.jar:androidx/compose/runtime/Immutable.class,
  obj/Debug/net10.0-android/lp/204/jl/classes.jar:androidx/compose/runtime/Immutable.class
  (ディレクトリ 'obj/Debug/net10.0-android/lp/204' は 'androidx.compose.runtime.runtime-android.aar' からのディレクトリです。)

## 段階3: 利用者アプリ (Sample.Maui) csproj へ Compose 直接ピン + runtime-annotation の ExcludeAssets="all" + NoWarn
=> restore で別の NU1107 が新規発生 (直接参照化により範囲が厳格化)

error NU1107: Xamarin.AndroidX.Fragment のバージョンの競合が検出されました。
 KsSettingsView.Sample.Maui -> Xamarin.AndroidX.Compose.UI.ViewBinding 1.7.6.1 -> Xamarin.AndroidX.Fragment.Ktx 1.8.8.1 -> Xamarin.AndroidX.Fragment (>= 1.8.8.1 && < 1.8.9).
 KsSettingsView.Sample.Maui -> KsSettingsView.Maui -> KsSettingsView.Binding.Android -> Xamarin.AndroidX.AppCompat 1.7.1.4 -> Xamarin.AndroidX.Fragment (>= 1.8.9.3)

## 段階4: Xamarin.AndroidX.Fragment 1.8.9.3 の直接ピンを追加
=> ビルドに成功しました。0 エラー / 34 個の警告
   警告内訳: NU1608 x60 (再ビルド時。うち利用者アプリ由来は NoWarn で抑制済み、
   binding / KsSettingsView.Maui プロジェクト由来のものは利用者側 NoWarn では消えない)、
   BG8605 x40 / BG8606 x4 / BG8 x4 (binding 生成の既存警告、本スパイクとは無関係)

## 実機検証 (Pixel 6a / API 36)
`dotnet build -t:Run` で正規デプロイ。基本 Cell 7 種・入力 Cell 5 種の全 Cell 表示、
TimePicker シート提示と確定、カレンダーダイアログの提示・日付選択・年選択ペイン展開・
横回転と縦復帰をまたぐ選択保持・OK 確定まで正常。
logcat に FATAL / NoSuchMethodError / NoClassDefFoundError / AndroidRuntime の出力なし。

## 最終的な利用者アプリ csproj の追加記述 (net10.0-android 条件下)
<NoWarn>$(NoWarn);NU1605;NU1608</NoWarn>
<PackageReference Include="Xamarin.AndroidX.Compose.Foundation" Version="1.7.6.1" />
<PackageReference Include="Xamarin.AndroidX.Compose.Material3" Version="1.3.1.2" />
<PackageReference Include="Xamarin.AndroidX.Compose.Runtime" Version="1.7.6.1" />
<PackageReference Include="Xamarin.AndroidX.Compose.UI" Version="1.7.6.1" />
<PackageReference Include="Xamarin.AndroidX.Compose.UI.ViewBinding" Version="1.7.6.1" />
<PackageReference Include="Xamarin.AndroidX.Fragment" Version="1.8.9.3" />
<PackageReference Include="Xamarin.AndroidX.Compose.Runtime.Annotation" Version="1.11.3.1" ExcludeAssets="all" />
<PackageReference Include="Xamarin.AndroidX.Compose.Runtime.Annotation.Android" Version="1.11.3.1" ExcludeAssets="all" />

(注) 本スパイクの一時変更はすべて復元済み。リポジトリの現行構成は BOM 2025.11.01 / Compose 1.11.4 系のまま。

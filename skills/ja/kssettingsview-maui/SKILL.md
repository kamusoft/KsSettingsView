---
name: kssettingsview-maui
description: KsSettingsView で .NET MAUI の設定画面 (settings screen) を作る - XAML / C# の公開 API (SettingsView, Section, CellBase) が iOS / Android の Native 設定 list を描画し、組み込みの Cell (Label, Command, Button, Switch, Checkbox, Radio, SimpleCheck, Entry, Picker, NumberPicker, TimePicker, DatePicker) と任意の MAUI View を置ける CustomCell、ユーザー操作の双方向バインド (two-way binding)、ItemsSource / ItemTemplate、Header / Footer への View 配置、Classic / Modern の list 外観を扱う。KsSettingsView.Maui を参照する .NET MAUI アプリで設定ページを追加・変更・レビューするときに使う。
license: MIT
metadata:
  language: ja
  source: https://github.com/kamusoft/KsSettingsView
---

# KsSettingsView for .NET MAUI

KsSettingsView は、iOS の設定アプリのようなリスト形式の設定画面を組み立てる UI ライブラリ。画面は行 (Cell) を Section にまとめたツリーとして宣言し、そのツリーがそのまま画面になる。この Skill が扱うのは .NET MAUI 版で、XAML と C# から使えるコントロール一式 (`SettingsView`・`Section`・各 Cell) として提供される。記述は XAML でも C# でもよい。行を描くのは各 platform の Native 設定 list で、MAUI 側の型はそこへ渡すデータにあたる。

## できること

| やりたいこと | 参照先 |
|---|---|
| 行を置く: ラベル、操作、ボタン、スイッチ、チェックボックス、ラジオ、テキスト入力、リスト選択、数値、時刻、日付 | [references/cells.md](references/cells.md) |
| 行を Section にまとめる、アイコン・説明・ヒントを付ける、行を無効化・非表示にする | [references/cells.md](references/cells.md) |
| リスト選択の確定操作を Command で受け取る (`PickerCell.SelectedCommand`) | [references/cells.md](references/cells.md) |
| 表示中の画面を変える: 行と Section の追加・削除・移動・差し替え | [references/updates.md](references/updates.md) |
| ユーザーの操作を ViewModel で受け取る、コレクションから行を生成する、ページを離れて戻っても状態を保つ | [references/updates.md](references/updates.md) |
| 色・フォント・行高さ、Classic / Modern の list 外観、Section の箱 | [references/styling.md](references/styling.md) |
| スタイルプロパティの一覧を引く (画面全体の既定と行ごとの上書き) | [references/styling.md](references/styling.md) |
| Section と画面全体の Header / Footer (任意の View も置ける)、ページ上での配置場所 | [references/styling.md](references/styling.md) |
| 任意の MAUI View を行 (row) として表示する、再利用できる独自 Cell 型にまとめる | [references/custom-cells.md](references/custom-cells.md) |

## 導入

### ライブラリをビルドに取り込む

アプリの `.csproj` にパッケージ参照を足す。

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />
</ItemGroup>
```

足す参照はこの 1 本だけでよく、platform の Binding 層は NuGet の推移参照で入る。そのうえで起動時に 1 度だけ登録する。登録される Handler は 1 件だけで、Cell 種別ごとに足す Handler はない。

```csharp
using KsSettingsView;
using Microsoft.Maui.Hosting;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder();

        builder
            .UseMauiApp<App>()
            .AddKsSettingsView();

        return builder.Build();
    }
}
```

### バージョン

| 要件 | 最低バージョン |
|---|---|
| .NET SDK | 10.0.300 |
| ターゲットフレームワーク | net10.0-ios, net10.0-android |
| Microsoft.Maui.Controls | 10.0.70 |
| iOS | 16.0 |
| Android | API 29 |

`Microsoft.Maui.Controls` の下限は restore 時に効く: .NET 10 のプロジェクトテンプレートが `MauiVersion` に書く版は 10.0.70 より低く (SDK 10.0.300 時点で 10.0.20)、そのままだと restore が NU1605 (パッケージのダウングレード) で失敗するので、`MauiVersion` は 10.0.70 以上にする。OS の下限はビルド時に効く: パッケージが利用側プロジェクトへ持ち込む検査が、その TFM の `SupportedOSPlatformVersion` が iOS 16.0 / Android API 29 を下回ると `net10.0-ios` / `net10.0-android` のビルドをエラー `KSSV0001` で止める。Android は未設定でも SDK 既定値が下限を下回るため同じく止まるので、両方の値を明示的に宣言しておく。

ターゲットフレームワークは上表の API 版なしを推奨する。この形なら platform asset と推移依存の Binding が選ばれる。TFM に API 版を固定する場合は `net10.0-android36.0` / `net10.0-ios26.0` 以上にする。`net10.0-android35.0` / `net10.0-ios18.0` など古い版を固定すると、restore が警告なく成功しても platform 中立の `lib/net10.0` asset へ静かにフォールバックし、Native Binding 2 件が依存グラフに入らない。このパッケージ選択の挙動は .NET SDK 10.0.300 で検証済み。

```xml
<PropertyGroup>
  <MauiVersion>10.0.70</MauiVersion>
</PropertyGroup>

<PropertyGroup Condition=" $([MSBuild]::GetTargetPlatformIdentifier('$(TargetFramework)')) == 'ios' ">
  <SupportedOSPlatformVersion>16.0</SupportedOSPlatformVersion>
</PropertyGroup>

<PropertyGroup Condition=" $([MSBuild]::GetTargetPlatformIdentifier('$(TargetFramework)')) == 'android' ">
  <SupportedOSPlatformVersion>29</SupportedOSPlatformVersion>
</PropertyGroup>
```

### MAUI 本体の Cell との名前衝突

`SwitchCell` と `EntryCell` は `Microsoft.Maui.Controls` にも同名の型があり、ライブラリの公開型のうち同名になるのはこの 2 型。XAML は `ks:` prefix が名前空間を示すので影響しない。C# では、`using KsSettingsView;` と MAUI の暗黙 using が同居するファイルで型名だけを書くと解決できず、コンパイラが CS0104 (あいまい参照) を報告する。完全修飾 (`KsSettingsView.SwitchCell`) で書くか、そのファイルに using alias を宣言する。

```csharp
using KsSettingsView;
using SwitchCell = KsSettingsView.SwitchCell;

Section account = new() { HeaderText = "Account" };
account.Cells.Add(new SwitchCell { Title = "Push notifications", On = true });
account.Cells.Add(new KsSettingsView.EntryCell { Title = "Name", Placeholder = "Taro Yamada" });
```

### Android のテーマ

Android の行はライブラリが同梱する Material3 テーマの中で描画されるので、ホストアプリ側に用意するものはない — 最小テーマの素の `ComponentActivity` を含め、どの Activity 型・XML テーマでも動く。裏返すと隔離でもあり、ホストテーマの色 (dynamic color を含む) はライブラリの行には届かないため、見た目の調整は `SettingsView` のスタイル系プロパティで行う ([references/styling.md](references/styling.md))。ライト / ダークは端末の夜間モードとアプリ自身の uiMode 制御に従い、ホストテーマでは決まらない。

## 最小動作コード

```xml
<ContentPage xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ks="clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui"
             x:Class="MyApp.SettingsPage">
  <ks:SettingsView>
    <ks:Section HeaderText="General">
      <ks:LabelCell Title="Version" ValueText="1.0.0" />
      <ks:SwitchCell Title="Push notifications" On="True" />
    </ks:Section>
  </ks:SettingsView>
</ContentPage>
```

`Root` は `SettingsView` の content property なので Section を直下に並べて書き、`Cells` も `Section` の content property になっている。`SettingsView` は大きさがレイアウト側で決まる場所 — ページ直下・Grid の `*` 行・明示サイズ指定 — に置く。

## リファレンス

- [references/cells.md](references/cells.md) - 組み込み Cell ごとのレシピと、Section・アイコン・全 Cell 共通フィールド。
- [references/updates.md](references/updates.md) - 表示中の画面の更新、双方向バインド、`ItemsSource`、ページを離れても残るもの。
- [references/styling.md](references/styling.md) - 画面全体の既定値、行ごとの上書き、list の外観、Section 装飾、Header / Footer、配置。
- [references/custom-cells.md](references/custom-cells.md) - `CustomCell`、再利用のための派生クラス、CustomCell に効かないプロパティ。

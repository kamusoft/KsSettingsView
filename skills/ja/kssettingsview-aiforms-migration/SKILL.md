---
name: kssettingsview-aiforms-migration
description: .NET MAUI の設定画面を AiForms.Maui.SettingsView から KsSettingsView へ移行する。旧公開 API — AiForms.Settings namespace、UseSettingsView / AddSettingsViewHandler、SettingsView / SettingsRoot / Section / CellBase、AiForms の全 Cell 種別 (Label, Command, Button, Switch, Checkbox, Radio, SimpleCheck, Entry, Picker, TextPicker, NumberPicker, TimePicker, DatePicker, CustomCell)、画面全体の style プロパティ、Cell ごとの Handler / PropertyMapper のカスタマイズ、HandlerCleanUpHelper によるメモリリーク回避策 — を KsSettingsView.Maui の対応先へ写像し、廃止されたものは代替手段とともに示す。AiForms 由来の設定ページを移植・レビュー・調査するときに使う。
license: MIT
metadata:
  language: ja
  source: https://github.com/kamusoft/KsSettingsView
---

# AiForms.SettingsView から KsSettingsView への移行

KsSettingsView は、iOS の設定アプリのようなリスト形式の設定画面を組み立てる UI ライブラリ。画面は行 (Cell) を Section にまとめたツリーとして宣言し、そのツリーがそのまま画面になる。この Skill が扱うのは .NET MAUI アプリを AiForms.Maui.SettingsView から KsSettingsView へ乗り換える作業で、旧公開 API のメンバーごとの対応表を持つ。

KsSettingsView は AiForms.SettingsView の骨格 — `SettingsView` に `Section` を並べ、その中に Cell を置き、ViewModel からバインドする形 — をそのまま引き継ぐが、行を描くのは MAUI の Handler ではなく Native の設定 list になった。多くのプロパティは同名のまま移せる。一部は改名・型変更され、一部は無くなっている。この Skill は、XAML に残っている AiForms のメンバーごとにそのどれに当たるかを示す。

対応表が突き合わせているのは .NET MAUI 版の AiForms.Maui.SettingsView である。旧 Xamarin.Forms 版 AiForms.SettingsView からの移行でも、メンバー名の多くは MAUI 版へ引き継がれているので読み替えて使えるが、ここの表は Xamarin.Forms 版の API とは突き合わせていない。

## できること

| やりたいこと | 参照先 |
|---|---|
| パッケージ参照・XAML の namespace・起動時登録を差し替える | 下の「導入」と「最小移行例」 |
| 同名または新名で残っている Cell プロパティを読み替える | [references/api-mapping.md](references/api-mapping.md) |
| `TextPickerCell`・添付プロパティの `RadioCell.SelectedValue`・`EntryCell.CompletedCommand`・`IsAndroidSpinnerStyle` の代わりを探す | [references/api-mapping.md](references/api-mapping.md) |
| 画面全体のスタイル (`Cell*` 既定値・Header / Footer・行高さ・Section の枠) を移す | [references/api-mapping.md](references/api-mapping.md) |
| 廃止された機能 (ドラッグ並べ替え・`ScrollToTop`・`UseDescriptionAsValue`・`LongCommand`) の扱いを決める | [references/api-mapping.md](references/api-mapping.md) |
| Cell ごとの Handler / PropertyMapper のコードと `HandlerCleanUpHelper` の回避策を削除する | [references/api-mapping.md](references/api-mapping.md) |
| 移行した C# の Cell 構築コードで出るようになった `SwitchCell` / `EntryCell` の CS0104 を直す | [references/api-mapping.md](references/api-mapping.md)、詳細は kssettingsview-maui Skill |
| KsSettingsView 自体の API を調べる | kssettingsview-maui Skill |

## 導入

`AiForms.Maui.SettingsView` のパッケージ参照を外し、代わりに `KsSettingsView.Maui` を参照する。両ライブラリは型を共有しないので XAML の namespace は別々のままで、画面ごとに 1 つずつ移せる。両方を同時に参照する構成はこのプロジェクトでは検証していないため、併存させる場合も移行期間中の一時的なものと考える。

外した `PackageReference` の代わりに、アプリの `.csproj` へ次を足す。

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />
</ItemGroup>
```

参照はこの 1 行だけで、下層の binding パッケージは推移的に届く。

| 要件 | AiForms | KsSettingsView |
|---|---|---|
| .NET SDK | 9.0.314 | 10.0.300 |
| ターゲットフレームワーク | net9.0-ios, net9.0-android, net9.0-maccatalyst | net10.0-ios, net10.0-android |
| API 版付きターゲットフレームワーク (明示する場合) | - | net10.0-android36.0, net10.0-ios26.0 以上 |
| Microsoft.Maui.Controls | 9.0.120 | 10.0.70 |
| iOS | 14.2 | 16.0 |
| Android | API 27 | API 29 |

`Microsoft.Maui.Controls` の下限は restore 時に効く: AiForms のプロジェクトが持つ `MauiVersion` は 10.0.70 より低く、そのままだと restore が NU1605 (パッケージのダウングレード) で失敗するので、`MauiVersion` を 10.0.70 以上に上げる。OS の下限はビルド時に効く: パッケージが利用側プロジェクトへ持ち込む検査が、その TFM の `SupportedOSPlatformVersion` が iOS 16.0 / Android API 29 を下回ると `net10.0-ios` / `net10.0-android` のビルドをエラー `KSSV0001` で止める。AiForms の値 (14.2 / 27) はこれに引っかかるので両方を上げる。

表に示した API 版なしの platform TFM を優先する。この形なら正しい native binding パッケージが選ばれる。platform API 版を明示する場合は `net10.0-android36.0` / `net10.0-ios26.0` 以上にする。それより低い版では警告なく restore が成功しても platform 中立の `lib/net10.0` asset が選ばれ、iOS / Android の native binding 依存が静かに欠ける。この挙動は SDK 10.0.300 で検証済み。

Mac Catalyst は対象外になった。Android はホスト Activity の型・テーマに要求を置かない: ライブラリが自前の Material3 テーマを同梱してその中で行を描くため、ホストテーマでは行の見た目が変わらず — ホストテーマ頼みだった AiForms の画面は `SettingsView` のプロパティで整え直すまで見た目が変わり得る — ライト / ダークは端末の夜間モードに追従する。

## 最小移行例

起動時の登録は 1 呼び出しにまとまる。AiForms は `AddSettingsViewHandler()` の裏で Cell 種別ごとの Handler を登録し、`UseSettingsView(true)` でリーク回避のフラグを受け取っていた。KsSettingsView が登録するのは `SettingsViewHandler` 1 件だけで、回避策も要らない。

移行前 (AiForms):

```csharp
builder
    .UseMauiApp<App>()
    .UseSettingsView(true);
```

移行後 (KsSettingsView):

```csharp
builder
    .UseMauiApp<App>()
    .AddKsSettingsView();
```

XAML では namespace 宣言が変わり、Section の見出し文字列が `Section.Title` から `Section.HeaderText` へ移る。CLR namespace は `KsSettingsView`、アセンブリ (とパッケージ) は `KsSettingsView.Maui` で、`xmlns` の前半と後半が違うのは意図したものである。この例に出てくる Cell 名とプロパティはそのままである。

移行前 (AiForms):

```xml
<ContentPage xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:sv="clr-namespace:AiForms.Settings;assembly=SettingsView"
             x:Class="MyApp.SettingsPage">
  <sv:SettingsView>
    <sv:Section Title="General">
      <sv:LabelCell Title="Version" ValueText="1.0.0" />
      <sv:SwitchCell Title="Push notifications" On="{Binding NotificationsEnabled}" />
    </sv:Section>
  </sv:SettingsView>
</ContentPage>
```

移行後 (KsSettingsView):

```xml
<ContentPage xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ks="clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui"
             x:Class="MyApp.SettingsPage">
  <ks:SettingsView>
    <ks:Section HeaderText="General">
      <ks:LabelCell Title="Version" ValueText="1.0.0" />
      <ks:SwitchCell Title="Push notifications" On="{Binding NotificationsEnabled}" />
    </ks:Section>
  </ks:SettingsView>
</ContentPage>
```

`SettingsView` は大きさがレイアウト側で決まる場所 — ページ直下・Grid の `*` 行・明示サイズ指定 — に置く。AiForms では `Auto` 行や `VerticalStackLayout` の直下でも問題なかったが、こちらでは通用しない。内容に合わせて自分の大きさを決めるコンテナに置くと、Android で入力中の行が途中でフォーカスを失う。

## リファレンス

- [references/api-mapping.md](references/api-mapping.md) - 旧 API から新 API への対応表一式。namespace と登録、画面の骨格、Cell 共通フィールド、Cell 種別ごと、画面全体のスタイル、Header / Footer、テンプレート生成、Handler カスタマイズ、代替のないメンバー、という「移行で何をしたいか」で節立てしている。

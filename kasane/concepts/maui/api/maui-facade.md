---
type: concept
title: MAUI facade (KsSettingsView.Maui) の公開契約
description: XAML / C# から SettingsView を利用する facade 層の入口 — 経路・導入と前提・型名衝突・Root / Section / Cell 階層と header / footer・ItemsSource / ItemTemplate・禁止事項と現時点の範囲
tags: [maui, facade, xaml, handler]
timestamp: 2026-09-04
---

# MAUI facade (KsSettingsView.Maui) の公開契約

この文書を読むと、.NET MAUI アプリから `KsSettingsView.Maui` を導入するときに何が要り、公開面がどういう骨格 (Root / Section / Cell 階層、header / footer、テンプレート生成) で組まれているか、その保証と制約が分かる。公開面の詳細は主題ごとに 3 本へ分かれている — Cell の公開プロパティと双方向バインド・PickerCell・CustomCell は [Cell の MAUI 表現](maui-cells.md)、Theme / CellStyle / ListStyle の公開形は [スタイルの MAUI 表現](maui-styling.md)、変更がいつどう表示へ届き Host の寿命をまたいで何が保たれるかは [表示への反映と Host の寿命](maui-rendering-lifecycle.md)。下層の interop 境界は [MAUI Native Bridge の interop 境界](native-bridge.md)、前提となる Store の一般契約は [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md) を先に読むと分かりやすい。決定の経緯は maui/ADR-0008 (公開面方針)・maui/ADR-0009 (TFM とテスト seam — platform 依存を型で切り出した差し替え点)・maui/ADR-0025 (3 パッケージ構成と名前空間)。

## 目的

XAML / C# から Native SettingsView を使うための公開面。命名は既存の Xamarin/MAUI 向け設定画面ライブラリ **AiForms.Maui.SettingsView** との互換を意図している (踏襲の方針と例外は maui/ADR-0008)。経路は常に次の一本で、facade は Bridge の内部所有 Store (Bridge が Native 側に持つ、設定ツリーと Theme の状態コンテナ — [native-bridge.md](native-bridge.md)) へ操作を変換するだけであり、独自の描画や状態を持たない:

```
SettingsView (facade) → Binding assembly (KsSettingsView.Binding.*) → Bridge → 内部所有 Store → Native Host
```

Binding assembly は Bridge API を C# へ運ぶだけの層で、アプリからは直接使わない (詳細は [native-bridge.md](native-bridge.md))。利用開始は `MauiAppBuilder.AddKsSettingsView()` — 登録される Handler は `SettingsViewHandler` 1件のみで、Cell 種別ごとの Handler は存在しない (Cell は Bridge DTO へ変換される純粋なデータ)。

## 導入と前提

配布物は NuGet の 3 パッケージで、利用者が書くのは facade `KsSettingsView.Maui` の `PackageReference` 1 行だけである (binding 2 件は platform TFM の依存として推移的に届く — [maui/ADR-0025](../../../decisions/maui/0025-nuget-three-package-root-namespace.md))。nuget.org で公開している (初回 `0.1.0-beta.1`。prerelease の suffix を持つ版は NuGet 側で prerelease 扱いになる)。配布物を利用者と同じ経路 (NuGet フィード) で解決・ビルドできることは消費者検証 `verification/maui` が、`main` 宛て pull request の CI とリリースの publish 前 (dry-run)、公開後 (smoke) で確かめている (消費者検証 = 配布物を利用者と同じ経路で参照するプロジェクトでの検証。[リポジトリとビルドの責務境界](../../cross/architecture/repository-boundaries.md))。pack の構成は [MAUI binding の Native artifact 統合](../architecture/binding-build-integration.md) が持つ。

公開型の名前空間は `KsSettingsView` (配下 `KsSettingsView.Internals` / `KsSettingsView.Handlers`) で、アセンブリ名・Package ID の `KsSettingsView.Maui` とは意図的に非対称である ([公開識別子と配布座標](../../../handbook/cross/public-identifiers.md))。最小の導入は XAML の xmlns と `MauiProgram` の登録の 2 箇所:

```xml
<!-- xmlns はアセンブリ名 KsSettingsView.Maui で修飾する (名前空間は KsSettingsView) -->
<ContentPage xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ks="clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui">
```

```csharp
using KsSettingsView;
// MauiProgram — Handler の登録
builder.UseMauiApp<App>().AddKsSettingsView();
```

利用者アプリ側の前提は次の 5 つで、いずれも facade が利用者アプリ側に要求する値である。満たさないと右列の形で restore・ビルドが失敗する (最後の 1 つだけは失敗せず静かに欠ける)。

| 前提 | 値 | 満たさないときの現れ方 |
|---|---|---|
| `TargetFramework` | `net10.0-android` / `net10.0-ios` (.NET 10。参照用に素の `net10.0` も持つ) | .NET 10 より前の TFM ではパッケージを解決できない |
| `Microsoft.Maui.Controls` | 10.0.70 以上 | テンプレート既定 (SDK 10.0.300 時点で 10.0.20) のままだと restore が NU1605 (ダウングレード) で失敗する。iOS の icon 所有権分類 ([表示への反映と Host の寿命](maui-rendering-lifecycle.md) の「IconSource の解決」、maui/ADR-0026) が 10.0.60 以降の内部挙動に依存し、10.0.70 はその挙動を実測で確認した版のため、検証済み版を下限にしている |
| `SupportedOSPlatformVersion` (Android) | 29 以上 | facade 同梱のビルド時ガードが `KSSV0001` で platform ビルドを止める (依存 AndroidX の manifest merger エラーより先に出る)。未設定時は SDK 既定 21 のため同じく止まる |
| `SupportedOSPlatformVersion` (iOS) | 16.0 以上 | 同じく `KSSV0001` で止まる。未設定時は SDK 既定 (26.x) が要件を満たすためガードは発火しない |
| TFM の API 版 (明示する場合のみ) | `net10.0-android36.0` / `net10.0-ios26.0` 以上 (パッケージの TFM group は SDK 10.0.300 の既定 platform 版で付く) | 失敗しない — 古い API 版 (例: `net10.0-android35.0` / `net10.0-ios18.0`) を固定すると restore は警告なく成功するが、`lib/net10.0` (platform 中立) の assembly が選ばれ binding 2 件が依存グラフに入らず native 実装が静かに欠ける。API 版なしの `net10.0-android` / `net10.0-ios` なら常に platform 版が選ばれる |

複数 TFM のプロジェクトは TFM ごとの内部ビルド (inner build) に分かれるが、ガードが働くのはそのうち `net10.0-android` / `net10.0-ios` の内部ビルドだけで、TFM をまたぐ外側のビルド・素の `net10.0`・facade を間接参照するライブラリの非 platform TFM では何もしない。仕組みと宣言元は [MAUI binding の Native artifact 統合](../architecture/binding-build-integration.md) の「最低 OS 版のビルド時ガード」。

AndroidX Lifecycle の版競合 (NU1608 / NU1107) は ProjectReference 経路・NuGet 経路の両方で binding 層の明示宣言により解消済み — 利用側にピンや `NoWarn` は不要 (maui/ADR-0010)。

### MAUI 本体との型名衝突

`KsSettingsView.SwitchCell` と `KsSettingsView.EntryCell` は `Microsoft.Maui.Controls` の同名型と衝突する (facade の公開型のうちこの 2 型のみ)。XAML の `ks:` prefix では起きないが、C# で `using KsSettingsView;` と MAUI の暗黙 using を併用すると CS0104 (あいまい参照) になる。AiForms.Maui.SettingsView 互換の型名を保つ方針 (maui/ADR-0008) のため型名・名前空間は変えない (maui/ADR-0025)。C# から使うときは完全修飾 (`KsSettingsView.SwitchCell`) か using alias (`using SwitchCell = KsSettingsView.SwitchCell;`) を書く。

## 公開 API の形

### Root と Section

`SettingsView.Root` (`IList<Section>`、content property、既定は observable な `SettingsRoot` — `ObservableCollection<Section>` の既定実装。observable かどうかで構造変更の反映の仕方が変わる: [表示への反映と Host の寿命](maui-rendering-lifecycle.md) の「構造の更新と内容の更新」) — XAML では SettingsView 直下に Section を直接並べる。`Section.Cells` (`IList<CellBase>`、content property) も同形。Root と Section の header / footer は text と View の両方で指定でき、Root と Section で対応する対になっている (View で置くものを以下 **accessory View** と呼ぶ):

| プロパティ | 型 / 既定 | 意味 |
|---|---|---|
| `SettingsView.RootHeaderText` / `RootFooterText`、`Section.HeaderText` / `FooterText` | `string?` | header / footer のテキスト。null 設定はクリア |
| `SettingsView.RootHeaderView` / `RootFooterView`、`Section.HeaderView` / `FooterView` | `View?` | 任意の MauiView を header / footer に配置する。text と View の両方が設定されている間は **View 優先**で text は輸送 (interop 境界を越えた native への受け渡し) されず、View を null に戻すと text へフォールバックする。`DataTemplate` 版 (HeaderTemplate 等) は提供しない (maui/ADR-0016〜0018) |
| `Section.HeaderHeight` | 未指定は native の自動高さ | Section ごとの header 高さ |
| `Section.IsVisible` | `bool`、既定 true | Section 単位の表示・非表示 |
| `Section.IsHeaderVisible` / `IsFooterVisible` | `bool`、既定 true | Header / Footer を内容を保持したまま隠す表示トグル。表示は「トグル && 内容あり」で判定される (core/ADR-0023。内容が無いものをトグルで表示させることはできない) |

### Cell 階層

`CellBase` (`Title` / `Description` / `HintText` / `IsEnabled` / `IsVisible` / `IconSource` とスタイル上書きプロパティ) を基底に 13 種 — 表示 `LabelCell`、基本 `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`、入力 `EntryCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell`、任意 View を内容にする `CustomCell`。公開プロパティは **Bridge interop が輸送できる範囲に限る** (native の対応 Cell の状態フィールドと 1:1。輸送形は maui/ADR-0011)。各 Cell の公開プロパティのうち型の表し方が MAUI 慣例に依るもの (プロパティの網羅は各 Cell の XML doc が正)、ユーザー操作の書き戻し (TwoWay)、PickerCell の候補と選択、CustomCell の契約は [Cell の MAUI 表現](maui-cells.md) が持つ。

### スタイル

画面全体の既定値 (native の `Theme` に対応) は SettingsView の個別プロパティとして展開して公開し、Cell 単位の上書き (native の `CellStyle` に対応) は CellBase / 各 Cell のプロパティで公開する。設定 list の style 切替 (`ListStyle`) と Section 装飾 4 属性も含め、公開形とプロパティの全一覧は [スタイルの MAUI 表現](maui-styling.md) が持つ。

### ItemsSource / ItemTemplate

`ItemsSource` / `ItemTemplate` — SettingsView 直下は Section 生成、Section 配下は Cell 生成。生成物の `BindingContext` は対応する item。`TemplateStartIndex` は生成物を挿入し始める位置 (既定 0) で、手動で並べた Section / Cell と混在させるときにテンプレート生成分をどこから差し込むかを決める。observable な items は Add / Remove / Replace / Move / Reset がミラーされ、Reset と null 化はテンプレ生成分のみ除去して手動追加分を温存する。ここでのミラーはテンプレート生成分 (item → Section / Cell) の話で、`Root` / `Cells` そのものへの構造変更がどう反映されるかは [表示への反映と Host の寿命](maui-rendering-lifecycle.md) の「構造の更新と内容の更新」。

`ItemTemplate` には `DataTemplateSelector` も渡せる — テンプレート実体化直前に `SelectTemplate(item, container)` で実テンプレートへ解決される。`SelectTemplate` が (a) null を返した、(b) `DataTemplateSelector` を返した (入れ子は不可)、(c) テンプレートとして生成できない型を返した場合は `InvalidOperationException`。

## してはいけないこと・制約

Section / Cell は logical tree に載らない — `{Binding}` は BindingContext の明示配布で解決されるが、**`x:Reference` と `DynamicResource` は届かない**。accessory View と `CustomCell.Content` だけは例外で logical tree に接続される ([表示への反映と Host の寿命](maui-rendering-lifecycle.md))。

- Binding assembly (`KsSettingsView.Binding.*`) の型を直接使わない — アプリ向け公開契約は facade のみ ([native-bridge.md](native-bridge.md) の禁止事項と同じ理由)
- 内容サイズを問われる配置 ([表示への反映と Host の寿命](maui-rendering-lifecycle.md) の「配置の制約」) に Android で入力 Cell を置いて編集させない — フォーカス喪失の既知経路が残る

## 現時点の範囲

利用者定義 Cell 型の登録機構 (maui/ADR-0019)、CustomCell の `ContentTemplate` と行の仮想化、D&D 並べ替え・スクロール制御等の Native 起点強化 (native 側の機能追加を起点に MAUI へ伝搬する強化) は未提供 (ロードマップ `kasane/roadmaps/maui-support/` の後続フェーズ)。CustomCell は行数分の View が常存するため、大量行を並べる用途は仮想化の提供まで見送る。配布状況は上の「導入と前提」を参照。

## 関連

- [Cell の MAUI 表現](maui-cells.md) — 各 Cell の公開プロパティの型・双方向バインド・PickerCell の候補と選択・CustomCell
- [スタイルの MAUI 表現](maui-styling.md) — Theme / CellStyle / ListStyle / Section 装飾の公開形とプロパティ一覧
- [表示への反映と Host の寿命](maui-rendering-lifecycle.md) — 更新の意味論・lifecycle の保証・配置の制約 (Android の measure 契約)
- [MAUI Native Bridge の interop 境界](native-bridge.md)
- [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md)
- [入力 Cell](../../core/cells/input-cells.md) / [基本 Cell](../../core/cells/basic-cells.md) / [CustomCell](../../core/cells/custom-cell.md) — Cell 意味論の共通契約
- [MauiView の native 実体化機構](../architecture/view-materialization.md) — accessory View と `CustomCell.Content` を native へ届ける内部機構
- [MAUI binding の Native artifact 統合](../architecture/binding-build-integration.md) — pack の構成と最低 OS 版のビルド時ガード

決定の経緯: maui/ADR-0025 (3 パッケージ構成と名前空間 `KsSettingsView`)、maui/ADR-0008 (AiForms 互換公開面の方針)、maui/ADR-0009 (net10.0 TFM + テスト seam)、maui/ADR-0010 (AndroidX 版競合の binding 層吸収)、maui/ADR-0011 (per-type 輸送)、maui/ADR-0016〜0018 (accessory View の実体化・輸送・更新セマンティクス)、core/ADR-0023 (Header / Footer の表示トグル)

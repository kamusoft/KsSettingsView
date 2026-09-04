---
type: concept
title: MAUI facade (KsSettingsView.Maui) の公開契約
description: XAML / C# から SettingsView を利用する facade 層 — 導入と前提・公開 API・双方向バインド・更新の意味論・lifecycle・配置制約
tags: [maui, facade, xaml, handler]
timestamp: 2026-09-04
---

# MAUI facade (KsSettingsView.Maui) の公開契約

この文書を読むと、.NET MAUI アプリから `KsSettingsView.Maui` を使うときに何がどう表示・更新されるか、ユーザー操作がどうアプリ状態へ戻るか、その保証と制約が分かる。下層の interop 境界は [MAUI Native Bridge の interop 境界](native-bridge.md)、前提となる Store の一般契約は [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md) を先に読むと分かりやすい。決定の経緯は maui/ADR-0008 (公開面方針)・maui/ADR-0009 (TFM とテスト seam)・maui/ADR-0011〜0015 (Cell 輸送・双方向値・uiStyle・measure 契約・icon 実体化)。

## 目的

XAML / C# から Native SettingsView を使うための公開面。命名は既存の Xamarin/MAUI 向け設定画面ライブラリ **AiForms.Maui.SettingsView** との互換を意図している (踏襲の方針と例外は maui/ADR-0008)。経路は常に次の一本で、facade は Bridge の内部所有 Store へ操作を変換するだけであり、独自の描画や状態を持たない:

```
SettingsView (facade) → Binding assembly (KsSettingsView.Binding.*) → Bridge → 内部所有 Store → Native Host
```

Binding assembly は Bridge API を C# へ運ぶだけの層で、アプリからは直接使わない (詳細は [native-bridge.md](native-bridge.md))。利用開始は `MauiAppBuilder.AddKsSettingsView()` — 登録される Handler は `SettingsViewHandler` 1件のみで、Cell 種別ごとの Handler は存在しない (Cell は Bridge DTO へ変換される純粋なデータ)。

## 導入と前提

配布物は NuGet の 3 パッケージで、利用者が書くのは facade `KsSettingsView.Maui` の `PackageReference` 1 行だけである (binding 2 件は platform TFM の依存として推移的に届く — [maui/ADR-0025](../../../decisions/maui/0025-nuget-three-package-root-namespace.md))。nuget.org で公開している (初回 `0.1.0-beta.1`。prerelease の suffix を持つ版は NuGet 側で prerelease 扱いになる)。配布物を利用者と同じ経路 (NuGet フィード) で解決・ビルドできることは消費者検証 `verification/maui` が、`main` 宛て pull request の CI とリリースの publish 前 (dry-run)、公開後 (smoke) で確かめている ([リポジトリとビルドの責務境界](../../cross/architecture/repository-boundaries.md))。pack の構成は [MAUI binding の Native artifact 統合](../architecture/binding-build-integration.md) が持つ。

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
| `Microsoft.Maui.Controls` | 10.0.70 以上 | テンプレート既定 (SDK 10.0.300 時点で 10.0.20) のままだと restore が NU1605 (ダウングレード) で失敗する。iOS の icon 所有権分類 (maui/ADR-0026) が 10.0.60 以降の内部挙動に依存し、10.0.70 はその挙動を実測で確認した版のため、検証済み版を下限にしている |
| `SupportedOSPlatformVersion` (Android) | 29 以上 | facade 同梱のビルド時ガードが `KSSV0001` で platform ビルドを止める (依存 AndroidX の manifest merger エラーより先に出る)。未設定時は SDK 既定 21 のため同じく止まる |
| `SupportedOSPlatformVersion` (iOS) | 16.0 以上 | 同じく `KSSV0001` で止まる。未設定時は SDK 既定 (26.x) が要件を満たすためガードは発火しない |
| TFM の API 版 (明示する場合のみ) | `net10.0-android36.0` / `net10.0-ios26.0` 以上 (パッケージの TFM group は SDK 10.0.300 の既定 platform 版で付く) | 失敗しない — 古い API 版 (例: `net10.0-android35.0` / `net10.0-ios18.0`) を固定すると restore は警告なく成功するが、`lib/net10.0` (platform 中立) の assembly が選ばれ binding 2 件が依存グラフに入らず native 実装が静かに欠ける。API 版なしの `net10.0-android` / `net10.0-ios` なら常に platform 版が選ばれる |

複数 TFM のプロジェクトは TFM ごとの内部ビルド (inner build) に分かれるが、ガードが働くのはそのうち `net10.0-android` / `net10.0-ios` の内部ビルドだけで、TFM をまたぐ外側のビルド・素の `net10.0`・facade を間接参照するライブラリの非 platform TFM では何もしない。仕組みと宣言元は [MAUI binding の Native artifact 統合](../architecture/binding-build-integration.md) の「最低 OS 版のビルド時ガード」。

### MAUI 本体との型名衝突

`KsSettingsView.SwitchCell` と `KsSettingsView.EntryCell` は `Microsoft.Maui.Controls` の同名型と衝突する (facade の公開型のうちこの 2 型のみ)。XAML の `ks:` prefix では起きないが、C# で `using KsSettingsView;` と MAUI の暗黙 using を併用すると CS0104 (あいまい参照) になる。AiForms.Maui.SettingsView 互換の型名を保つ方針 (maui/ADR-0008) のため型名・名前空間は変えない (maui/ADR-0025)。C# から使うときは完全修飾 (`KsSettingsView.SwitchCell`) か using alias (`using SwitchCell = KsSettingsView.SwitchCell;`) を書く。

## 公開 API の形

- `SettingsView.Root` (`IList<Section>`、content property、既定は observable な `SettingsRoot`) — XAML では SettingsView 直下に Section を直接並べる。`Section.Cells` (`IList<CellBase>`、content property) も同形
- Cell 階層: `CellBase` (`Title` / `Description` / `HintText` / `IsEnabled` / `IsVisible` / `IconSource` とスタイル上書きプロパティ) を基底に 13 種 — 表示 `LabelCell`、基本 `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`、入力 `EntryCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell`、任意 View を内容にする `CustomCell` (下記)。公開プロパティは **Bridge interop が輸送できる範囲に限る** (native の対応 Cell の状態フィールドと 1:1。輸送形は maui/ADR-0011)
- 値の型は MAUI 慣例型 (maui/ADR-0004): `TimePickerCell.Time` は `TimeSpan`、`DatePickerCell.Date` は `DateTime` (日付部分のみ意味を持つ)、`EntryCell.Keyboard` は `Microsoft.Maui.Keyboard`。`TimePickerCell.Is24Hour` (bool、既定 true) は選択面の時制の唯一の決定源で、`Format` は行の表示専用 ([core/ADR-0028](../../../decisions/core/0028-timepickercell-is24hour-sole-hour-cycle-source.md))。`DatePickerCell.UIStyle` は `DatePickerUIStyle?` (両OS共通の enum `{ Calendar, Wheels }`、null = native 既定。意味マッピングは maui/ADR-0013。Android で `Calendar` を選ぶとカレンダーダイアログ固有の挙動 — テキスト入力モードへの切替 — が付随する。ホスト Activity 型・テーマへの要求はない)。`AndroidButtonColor` のような片OS固有項目は接頭辞付き nullable。`EntryCell.TextAlignment` (`TextAlignment?`、null = platform 既定) は入力テキストの行内揃えを指定する
- `PickerCell` は `SelectionMode` (Single / Multiple) で単一・複数選択を切り替える。単一選択の正は `SelectedIndex`、複数選択の正は `SelectedIndices` (昇順・重複除去)。候補と選択は AiForms 同型の object API ([core/ADR-0029](../../../decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md)):
  - `ItemsSource` (`IList?`) は任意の object 列。**null 要素は非対応** (設定時に `ArgumentException` — 許すと `SelectedItem == null` が「未選択」と区別できなくなる)。設定時に「元 object の snapshot」と「射影済み (text, subText) の snapshot」を同時に確定し、表示・逆引き・`SelectedItem(s)` は同一 snapshot を参照する (元コレクションの in-place 変更は観測しない — 差し替えで反映)
  - 射影は `DisplayMember` / `SubDisplayMember` (`string?`) のリフレクション sugar — 要素の実行時型の public instance の引数なし readable プロパティを解決し getter delegate を型別キャッシュする (ドット区切りパス式は非対応)。未指定・未解決は `ToString()` フォールバック (副表示は null)。値が string 以外なら `ToString()` 化、getter の例外は握りつぶさず伝播。`PropertyInfo` ベースで AOT の動的コード生成に依存しないが、文字列名でしか参照されないプロパティの trimming 保全は利用者契約 (未保全時は `ToString()` フォールバックへ退化)。旧 `DisplayFormatter` は削除済み
  - `SelectedItem` (`object?`) / `SelectedItems` (`IList?`、index 昇順) は正 (`SelectedIndex` / `SelectedIndices`) との相互導出 TwoWay。要素 → index の逆引きは値等価の最初一致で、公開値は直後に正から再導出される — 「設定した列がそのまま返る」保証はしない。`SelectedItems` の null と空リストはいずれも「選択なし」へ揃える
  - **候補が 1 件も無い間は相互導出を行わない**: `ItemsSource` 未設定 (または null / 空へ差し替えた後) に設定された `SelectedItem(s)` は未選択へ揃えず保留し、候補到着時に逆引きして復元する — XAML 属性順・バインディング適用順で ViewModel の初期選択が黙って破棄されない (順序非依存)。候補が既にある状態からの差し替えは従来どおり位置 (index) が正
  - 公開値の snapshot 実体への正規化は **Cell の公開プロパティまで**が契約 — TwoWay 先の ViewModel は値等価な別実体を持ち得る (値等価時に null を挟む強制伝播はしない。原典 AiForms も VM への実体書き戻しはピッカー操作時のみで同挙動)
  - `SelectedCommand` (`ICommand?`、OneWay・既定 null) は**利用者による選択操作の完了**を通知する — 値の TwoWay とは別軸で、同じ選択を確定し直しても実行される (値の変化ではなく確定操作の通知)。発火源は native の選択確定通知のみ: 公開選択値の直接設定・cancel・非確定 dismiss では実行されない。実行は選択値の書き戻しと相互導出の完了後で、ViewModel は Command 内から新しい選択値を観測できる。実行引数は届いた確定通知の種類で決まり、単一選択の通知で `SelectedItem`、複数選択の通知で `SelectedItems` (選択面表示中に `SelectionMode` が変わっても、利用者が確定した種類に対応する引数を渡す)。移植元互換で `CanExecute` は確認せず `Execute` を直接呼ぶ (`CommandCell.Command` が実効有効に反映するのと意図的に異なる)。任意の `CommandParameter` は提供しない
- スタイル: 画面全体の既定値 (native の `Theme` に対応) は SettingsView の個別プロパティとして展開して公開する。Cell 単位の上書き (native の `CellStyle` に対応) は CellBase / 各 Cell のプロパティと Cell 固有の `AccentColor`。フォントは FontFamily / FontSize / FontAttributes に分けて公開し facade が合成する (maui/ADR-0008)。プロパティの全一覧は下の「スタイルプロパティ一覧」
- `SettingsView.ListStyle` (`SettingsViewStyle { Classic, Modern }`、非 nullable・既定 `Classic`) — 設定 list の style 切替 ([設定 list の外観](../../core/styling/list-appearance.md))。素直な `Style` は `VisualElement.Style` (XAML の Style 機構) と衝突するため使えない。Theme とは独立の経路で native の style プロパティへ伝わり、切替は設定内容と Section / Cell の identity を変えない (maui/ADR-0023)
- Section 装飾4属性: `SectionMargin` (`Thickness?`) / `SectionCornerRadius` (`double?`) / `SectionBorderWidth` (`double?`) / `SectionBorderColor` (`Color?`) — Modern の Section 箱の外側余白・角丸・ボーダーを Theme として伝える。null (既定) は platform 既定へ委譲し、facade は既定値定数を持たず値の検証 (負値の正規化・radius clamp・例外送出) を行わない — 正規化は Native の描画時のみで、負値・非有限 (NaN・±∞) の寸法は 0 として描画される。`SectionMargin` の `Left` / `Right` は MAUI 標準の物理座標ではなく **leading / trailing (論理方向)** として解釈し、RTL の左右解決は Native に委ねる (facade は `FlowDirection` を監視しない)。Classic では上下成分のみ適用され左右は無視される (全幅契約) (maui/ADR-0024)
- header / footer テキスト: `SettingsView.RootHeaderText` / `RootFooterText` と `Section.HeaderText` / `FooterText` (Root と Section で同名の対)。null 設定はクリア。`Section.HeaderHeight` で Section ごとの header 高さを指定できる。`Section.IsVisible` (既定 true) で Section 単位の表示・非表示を切り替える。`Section.IsHeaderVisible` / `IsFooterVisible` (既定 true) は Header / Footer を内容を保持したまま隠す表示トグルで、表示は「トグル && 内容あり」で判定される (core/ADR-0023。内容が無いものをトグルで表示させることはできない)
- header / footer View: `SettingsView.RootHeaderView` / `RootFooterView` と `Section.HeaderView` / `FooterView` (いずれも `View?`) — 任意の MauiView を header / footer に配置する。text と View の両方が設定されている間は **View 優先**で text は輸送されず、View を null に戻すと text へフォールバックする。`DataTemplate` 版 (HeaderTemplate 等) は提供しない (maui/ADR-0016〜0018)
- `ItemsSource` / `ItemTemplate` — SettingsView 直下は Section 生成、Section 配下は Cell 生成。生成物の `BindingContext` は対応する item。`TemplateStartIndex` は生成物を挿入し始める位置 (既定 0) で、手動で並べた Section / Cell と混在させるときにテンプレート生成分をどこから差し込むかを決める。observable な items は Add / Remove / Replace / Move / Reset がミラーされ、Reset と null 化はテンプレ生成分のみ除去して手動追加分を温存する
- `ItemTemplate` には `DataTemplateSelector` も渡せる — テンプレート実体化直前に `SelectTemplate(item, container)` で実テンプレートへ解決される。`SelectTemplate` が (a) null を返した、(b) `DataTemplateSelector` を返した (入れ子は不可)、(c) テンプレートとして生成できない型を返した場合は `InvalidOperationException`

### スタイルプロパティ一覧

画面全体の既定値 (SettingsView、native の `Theme` に対応):

| 分類 | プロパティ |
|---|---|
| 色・挙動 | `SeparatorColor`、`SelectedColor`、`CellBackgroundColor`、`CellAccentColor`、`DisabledTextColor`、`CellPlaceholderColor`、`ScrollIndicatorVisible`、`RowHeight`、`HasUnevenRows` |
| Header 書式 | `HeaderTextColor`、`HeaderBackgroundColor`、`HeaderFontFamily`、`HeaderFontSize`、`HeaderFontAttributes`、`HeaderHeight` |
| Footer 書式 | `FooterTextColor`、`FooterBackgroundColor`、`FooterFontFamily`、`FooterFontSize`、`FooterFontAttributes` |
| Cell タイトル既定 | `CellTitleColor`、`CellTitleFontFamily`、`CellTitleFontSize`、`CellTitleFontAttributes` |
| Cell 値テキスト既定 | `CellValueTextColor`、`CellValueTextFontFamily`、`CellValueTextFontSize`、`CellValueTextFontAttributes` |
| Cell 説明文既定 | `CellDescriptionColor`、`CellDescriptionFontFamily`、`CellDescriptionFontSize`、`CellDescriptionFontAttributes` |
| Cell ヒント既定 | `CellHintTextColor`、`CellHintFontFamily`、`CellHintFontSize`、`CellHintFontAttributes` |
| icon | `CellIconSize`、`CellIconRadius` |
| Section 装飾 | `SectionMargin`、`SectionCornerRadius`、`SectionBorderWidth`、`SectionBorderColor` (上記「Section 装飾4属性」) |

Cell 単位の上書き (CellBase、native の `CellStyle` に対応):

| 分類 | プロパティ |
|---|---|
| タイトル | `TitleColor`、`TitleFontFamily`、`TitleFontSize`、`TitleFontAttributes` |
| 値テキスト | `ValueTextColor`、`ValueTextFontFamily`、`ValueTextFontSize`、`ValueTextFontAttributes` |
| 説明文 | `DescriptionColor`、`DescriptionFontFamily`、`DescriptionFontSize`、`DescriptionFontAttributes` |
| ヒント | `HintTextColor`、`HintFontFamily`、`HintFontSize`、`HintFontAttributes` |
| 行・icon | `BackgroundColor`、`IconSize`、`IconRadius`、`Height` |

## CustomCell (任意の View を行の内容にする)

プリセットの Cell では表せない UI を1行に差し込むための Cell。行の内容領域は Disclosure Indicator を除く全域で、共通行レイアウトのスロット (title / description / icon) を持たない ([CustomCell の共通契約](../../core/cells/custom-cell.md))。

- `CustomCell : CellBase`。`[ContentProperty]` は `Content` (`View?`、既定 null) で、XAML では CustomCell の直下に View を直書きする。null の間は空の内容の行になる
- 挙動プロパティは `Command` / `CommandParameter` / `Tapped` と `ShowArrowIndicator` (既定 false — CommandCell の `HideArrow` と向きが逆なのは、矢印の既定が Cell 種で逆であることによる)。実効有効状態 (`IsEnabled` と `CanExecute` の連動・`CanExecuteChanged` への追従) と発火順 (`Tapped` → `Command`) は CommandCell と同一
- **タップの棲み分け**: `Command` / `Tapped` をいずれも持たない行はタップ動作そのものを持たず、content 内の操作 (スライダー等) を妨げない。持つ行でも、content 内の操作対象がタップを消費したときは行タップは発火しない (二重発火しない)。購読の有無は表示後に変えても追従する
- **更新**: content の内部の変化 (バインド値の更新等) はプロパティの再設定なしに表示へ届く。native への再発行が起きるのは `Content` を別インスタンスへ差し替えたときだけで、同一インスタンスのまま内容が変わっても行の内容は作り直されない (maui/ADR-0020)。行の高さは content のサイズに追従し、表示中のサイズ変化にも追う
- **不適用プロパティは silent no-op**: `CellBase` から継承する `Title` / `Description` / `HintText` / `IconSource` とテキスト系のスタイル項目は表示に影響しない。設定しても例外・警告にはならず黙って無視される (同一のスタイル指定値を CustomCell を含む複数 Cell へまとめて当てられるようにするため。不適用の一覧は `CustomCell` の XML doc が正)。効くのは `IsEnabled` / `IsVisible` / `BackgroundColor` / `Height` と上記の固有プロパティ
- 同一の View インスタンスを複数の `Content` へ、または `Content` と accessory View へ同時に置くことはできない (`InvalidOperationException`)。検査は `Content` の値が確定する**前**に行われるため、失敗しても `Content` の公開値と表示はどちらも動かない (accessory View 側も同じ規律 — maui/ADR-0022)
- `ItemTemplate` から生成された各 CustomCell はそれぞれ独立した View 実体を持ち、対応する item を `BindingContext` として解決する
- 提供するのはこの CustomCell と、それを組み立てて返す再利用形 (ファクトリメソッド、または `CustomCell` 派生サブクラス — 派生も同じ経路で描画される) まで。**利用者が独自の Cell 型と描画を登録する機構 (native の `KsCellRegistry`) は MAUI では公開しない** (maui/ADR-0019。決定の詳細は maui/ADR-0021 の公開面と併せて参照)

## ユーザー操作と双方向バインド

- 対話型 Cell のユーザー操作は native から facade へ通知され、**12 プロパティ**が書き戻される (輸送規約と基礎 10 件の正規一覧は maui/ADR-0012): `SwitchCell.On` / `CheckboxCell.Checked` / `SimpleCheckCell.Checked` / `RadioCell.SelectedValue` / `EntryCell.ValueText` / `PickerCell.SelectedIndex` / `PickerCell.SelectedIndices` / `NumberPickerCell.Number` / `TimePickerCell.Time` / `DatePickerCell.Date`、および `SelectedIndex` / `SelectedIndices` からの相互導出で書き戻される `PickerCell.SelectedItem` / `PickerCell.SelectedItems` (core/ADR-0029 の復元で追加)。これらは **`BindingMode.TwoWay` が既定**で、ViewModel へそのまま流れる。その他のプロパティは OneWay 既定
- `RadioCell` の選択は同一 `GroupId` の全 RadioCell の `SelectedValue` へ同期される
- `CommandCell` / `ButtonCell` の実効有効状態は `IsEnabled && (Command?.CanExecute(CommandParameter) ?? true)` で、`CanExecuteChanged` に追随する。タップは実効有効のときだけ発火し、順序は `Tapped` イベント → `Command.Execute(CommandParameter)`
- `EntryCell` の値変更 event / callback は公開しない — 経路は `ValueText` の TwoWay バインドのみ (AiForms 原典にも値変更 callback は無く、TwoWay バインドで足りるとするオーナー判断)
- `PickerCell` の選択操作の完了は `SelectedCommand` で通知される — 値の書き戻し (TwoWay) だけでは初期化・プログラム更新と利用者操作を区別できないため、完了通知は独立の公開面を持つ (契約の詳細は上記 PickerCell の項)

## 更新の意味論

- `Root` / `Cells` の実体が `INotifyCollectionChanged` なら購読され、構造イベント (Add / Remove / Move / Replace) は即時に反映、`Reset` は全体再構築になる。observable でない実体 (素の `List<T>`) は接続時点の内容の静的描画で、以後の操作は反映されない
- Cell の内容更新は同一 UI サイクル内の変更がまとめて 1 回で画面に反映される (Store のバッチ契約に乗るため)。可視性 (`IsVisible`) と Section 単位の変更 (`Section.IsVisible` / `HeaderHeight` 等) はこのバッチに乗らず個別に反映されるが、いずれも同一サイクル内に描画されるため利用者から見た差はない
- `IconSource` は MAUI 標準の image source service で**非同期に** platform 画像へ解決されてから表示される (maui/ADR-0015)。Handler 未接続の間は解決が保留され、接続時にまとめて解決される。連続変更は最後の値が勝つ (latest-wins)。解決失敗は icon なしとして確定し、次の変更で再試行される。iOS で UIKit の名前付き画像キャッシュが所有する画像 (asset catalog・拡張子なしファイル名) に解決された場合、facade はその画像を破棄しない — 同一 UIImage が複数 Cell / SettingsView に共有され得るため、後片付けは解決時の所有権分類で facade 所有の画像だけに行う (maui/ADR-0026)
- accessory View の更新は「参照が正、内容は live」(maui/ADR-0018): View プロパティへ別インスタンスを設定すると表示が差し替わる。同一インスタンスの内部変化 (バインド値の更新等) はプロパティ再設定なしに表示へ反映され、サイズが変わる場合は自動高さの領域が追従する (`HeaderHeight` 指定時は固定高さで切り詰め)。`CustomCell.Content` も同じ規律に従う (maui/ADR-0020)
- accessory View と `CustomCell.Content` は (Section / Cell と異なり) **logical tree に接続され**、所有者 — Root 系は SettingsView、`Section.HeaderView` / `FooterView` は所有 Section、`Content` は所有 CustomCell (ItemsSource / ItemTemplate 生成では item が BindingContext) — の `BindingContext` を継承する。View 自身に明示的な BindingContext があれば上書きしない。継承と変更伝播は Handler 接続の有無に依らない
- **全操作は UI スレッドから行う** (呼び出し側契約。facade は marshal しない)
- 同一の Section / CellBase / accessory View / `CustomCell.Content` の View インスタンスを複数箇所へ配置すると `InvalidOperationException` (ItemsSource のテンプレートが既配置インスタンスを返す場合も同様)。View 配置プロパティ (accessory View / `Content`) の検査は値が確定する**前**に行われ、失敗しても公開値・論理所有・表示はいずれも動かない。構造変更バッチ (Section / Cell の追加・差し替え・Root 再構築) 内の重複は native へ触れる前に全件検査され、どの位置の要素が衝突しても部分更新を残さない。失敗後も公開コレクション (`Root` / `Cells`) はロールバックされず、回復は呼び出し元による Root の全体再構築 (再代入 / Reset) で行う (maui/ADR-0022)。設定ツリーに未参加の Section (XAML 構築中等) へ既配置の View を設定した場合は既存配置を奪わず、その Section が SettingsView の変換経路に加わった時点で例外になる — null に戻した View の別 slot への再利用はいつでも可

## lifecycle の保証

- ページ表示 (Handler 接続) で Native Host が生成され、その時点の状態が表示される。ページ離脱 (Handler 切断) で Host は解放されるが、**facade・Bridge・Store は生き続け、切断中の変更も Store へ流れ続ける** — 再訪問時は Store 現在状態から表示が復元される (maui/ADR-0007)
- `RootHeaderText` / `RootFooterText` は Store の復元対象外 (core/ADR-0019) のため facade が値を所有し、Host の attach 後に再適用する。利用者から見れば再訪問後もテキストは保持されている
- accessory View と `CustomCell.Content` も復元の正は facade が所有する VisualElement で、platform 実体 (wrapper) は Host 世代ごとに作り直される (maui/ADR-0016・0020)。切断中の View 差し替え・内容変化も再接続後の表示に反映され、利用者から見れば埋め込んだ View は保持されている
- ユーザー操作通知の購読は Handler の接続で開始・切断で解除される。切断中に操作は発生し得ない (Host が無い) ため、利用者から見て通知の取りこぼしはない
- iOS の Host は ViewController であり、facade が親 Page への子 VC embed (containment) を管理する。利用者側の作業はない
- コレクション・Cell への購読に限らず、model (Section / Cell) から SettingsView 側へ向かう内部参照 (配置検査の尋ね先 guard を含む — maui/ADR-0022) はすべて weak であり、外部 (ViewModel 等) がコレクションや Cell — `Content` 設定済みの CustomCell を含む — を保持し続けても SettingsView の回収は妨げられない

## 配置の制約 (Android の measure 契約)

Android では、SettingsView が「大きさの確定しない制約」で measure されると、measure の途中で内部の一覧の配置まで走り、編集中の入力欄が一時的に幅ゼロになってフォーカスを失うことがある (Android は幅ゼロになったフォーカス中 View のフォーカスを外すため)。これを避けるため、SettingsView は割当領域を fill するコントロールとして Android の `SettingsViewHandler` が measure 契約を閉じる (maui/ADR-0014):

- **大きさが決まる配置** (Grid の `*` 行 / ページ直下 / 固定サイズ指定 — 幅・高さとも有限制約): handler が制約から desired size を即答し、上記のフォーカス喪失経路が消える。**通常はこの配置を使う**
- **内容サイズを問われる配置** (`VerticalStackLayout` 直下 / 縦 `ScrollView` の content / Grid の `Auto` 行 — 制約の一方が無限): 内容ぶんの大きさに答えられるのは Native Host だけなので既定の measure へフォールバックする。表示は保たれるが、Android ではフォーカス喪失経路が残る。**この配置は推奨しない**

iOS の handler は measure を override しない。大きさが決まる配置では Android の即答値と measure 結果が一致するため差は出ず、内容サイズを問われる配置では両OSとも既定の measure に揃う — いずれも同一 XAML の配置結果が OS 間で割れない (非 Fill 配置の例外は maui/ADR-0014 の Consequences を参照)。

## してはいけないこと・制約

- Section / Cell は logical tree に載らない — `{Binding}` は BindingContext の明示配布で解決されるが、**`x:Reference` と `DynamicResource` は届かない**
- Binding assembly (`KsSettingsView.Binding.*`) の型を直接使わない — アプリ向け公開契約は facade のみ ([native-bridge.md](native-bridge.md) の禁止事項と同じ理由)
- 内容サイズを問われる配置 (上記) に Android で入力 Cell を置いて編集させない — フォーカス喪失の既知経路が残る

## 現時点の範囲

- 利用者定義 Cell 型の登録機構 (maui/ADR-0019)、CustomCell の `ContentTemplate` と行の仮想化、D&D 並べ替え・スクロール制御等の Native 起点強化は未提供 (ロードマップ `kasane/roadmaps/maui-support/` の後続フェーズ)。CustomCell は行数分の View が常存するため、大量行を並べる用途は仮想化の提供まで見送る
- 配布状況は上の「導入と前提」を参照。AndroidX Lifecycle の版競合 (NU1608 / NU1107) は ProjectReference 経路・NuGet 経路の両方で binding 層の明示宣言により解消済み — 利用側にピンや `NoWarn` は不要 (maui/ADR-0010)

## 関連

- [MAUI Native Bridge の interop 境界](native-bridge.md)
- [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md)
- [入力 Cell](../../core/cells/input-cells.md) / [基本 Cell](../../core/cells/basic-cells.md) / [CustomCell](../../core/cells/custom-cell.md) — Cell 意味論の共通契約
- [MauiView の native 実体化機構](../architecture/view-materialization.md) — accessory View と `CustomCell.Content` を native へ届ける内部機構
- 決定の経緯: maui/ADR-0025 (3 パッケージ構成と名前空間 `KsSettingsView`)、maui/ADR-0008 (AiForms 互換公開面の方針)、maui/ADR-0009 (net10.0 TFM + テスト seam)、maui/ADR-0007 (releaseHost)、core/ADR-0019 (attach 時復元)、maui/ADR-0011 (per-type 輸送)、maui/ADR-0012 (双方向値の輸送規約)、maui/ADR-0013 (DatePickerUIStyle)、maui/ADR-0014 (Android measure 契約)、maui/ADR-0015 (IconSource 実体化)、maui/ADR-0016〜0018 (accessory View の実体化・輸送・更新セマンティクス)、maui/ADR-0019〜0021 (CustomCell の提供層・content の live view と世代トークン・公開面と silent no-op)、maui/ADR-0023 (ListStyle の Theme 独立経路)、maui/ADR-0024 (SectionMargin の論理方向解釈)、maui/ADR-0026 (iOS icon 後片付けの所有権分類)

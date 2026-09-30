---
type: concept
title: MAUI facade (KsSettingsView.Maui) の公開契約
description: XAML / C# から SettingsView を利用する facade 層の入口 — 経路・導入と前提・型名衝突・Root / Section / Cell 階層と header / footer・ItemsSource / ItemTemplate・スクロール命令のハンドル・論理子と binding の解決・禁止事項と現時点の範囲
tags: [maui, facade, xaml, handler]
timestamp: 2026-09-30
---

# MAUI facade (KsSettingsView.Maui) の公開契約

この文書を読むと、.NET MAUI アプリから `KsSettingsView.Maui` を導入するときに何が要り、公開面がどういう骨格 (Root / Section / Cell 階層、header / footer、テンプレート生成、スクロール命令) で組まれているか、その保証と制約が分かる。公開面の詳細は主題ごとに 3 本へ分かれている — Cell の公開プロパティと双方向バインド・PickerCell・CustomCell は [Cell の MAUI 表現](maui-cells.md)、Theme / CellStyle / ListStyle の公開形は [スタイルの MAUI 表現](maui-styling.md)、変更がいつどう表示へ届き Host の寿命をまたいで何が保たれるかは [表示への反映と Host の寿命](maui-rendering-lifecycle.md)。下層の interop 境界は [MAUI Native Bridge の interop 境界](native-bridge.md)、前提となる Store の一般契約は [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md) を先に読むと分かりやすい。決定の経緯は maui/ADR-0008 (公開面方針)・maui/ADR-0009 (TFM とテスト seam — platform 依存を型で切り出した差し替え点)・maui/ADR-0025 (3 パッケージ構成と名前空間)。

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
| `Microsoft.Maui.Controls` | 10.0.71 以上 | テンプレート既定 (SDK 10.0.300 時点で 10.0.20) や 10.0.70 のままだと restore が NU1605 (ダウングレード) で失敗する。下限は検証済みの版で、理由は下記 |
| `SupportedOSPlatformVersion` (Android) | 29 以上 | facade 同梱のビルド時ガードが `KSSV0001` で platform ビルドを止める (依存 AndroidX の manifest merger エラーより先に出る)。未設定時は SDK 既定 21 のため同じく止まる |
| `SupportedOSPlatformVersion` (iOS) | 16.0 以上 | 同じく `KSSV0001` で止まる。未設定時は SDK 既定 (26.x) が要件を満たすためガードは発火しない |
| TFM の API 版 (明示する場合のみ) | `net10.0-android36.0` / `net10.0-ios26.0` 以上 (パッケージの TFM group は SDK 10.0.300 の既定 platform 版で付く) | 失敗しない — 古い API 版 (例: `net10.0-android35.0` / `net10.0-ios18.0`) を固定すると restore は警告なく成功するが、`lib/net10.0` (platform 中立) の assembly が選ばれ binding 2 件が依存グラフに入らず native 実装が静かに欠ける。API 版なしの `net10.0-android` / `net10.0-ios` なら常に platform 版が選ばれる |

`Microsoft.Maui.Controls` の下限は 2 つの事情で決まっている。iOS の icon 所有権分類 ([表示への反映と Host の寿命](maui-rendering-lifecycle.md) の「IconSource の解決」、maui/ADR-0026) が 10.0.60 以降の内部挙動に依存する。加えて 10.0.70 は、binding の依存 (AndroidX の Compose・RecyclerView 等) が引き上げる `Xamarin.AndroidX.Core` と合わない — その版では `AccessibilityNodeInfoCompat.Checked` の型が変わっており、10.0.70 の `Microsoft.Maui.dll` が呼ぶメンバーが無いため、Android でアクセシビリティの問い合わせ (TalkBack・uiautomator 等) が MAUI の経路を通った時点で `MissingMethodException` で落ちる。10.0.71 はこの呼び出しを持たない。実行時に落ちる代わりに restore の時点で気づけるよう、facade の依存の下限を 10.0.71 にしている (maui/ADR-0010 の範囲 — 版の競合は binding / CPM 側で吸収し、利用者に版のピンを書かせない)。

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

### スクロール命令

スクロールはインターフェース型の命令ハンドル `IScrollController` で命令する。SettingsView にメソッドは無く、ハンドルの実体は SettingsView が作って ViewModel へ渡す — ViewModel が UI の型に依存せず、テストでは命令を記録するだけの実装に差し替えられるようにするため (maui/ADR-0029)。命令は `ScrollTo(target, position, animated)` (Cell へ)・`ScrollToSection(target, position, animated)` (Section へ、見出しごと)・`ScrollToStart(animated)`・`ScrollToEnd(animated)` の 4 種で、`position` は独自の enum `ScrollPosition` (`Start` / `Center` / `End`、既定 `Start`)、`animated` の既定は true。MAUI 標準の `ScrollToPosition` は、`MakeVisible` に Native の対応が無いため使わない。位置の意味と実行時期 (同じ処理の中の変更が表示に反映された後。項目を足した直後の `ScrollToEnd` は足した項目を含む末尾へ届く) は 3 platform 共通で、[スクロール制御](../../core/architecture/scroll-control.md) が持つ。

| プロパティ | 型 / 既定 | 意味 |
|---|---|---|
| `SettingsView.ScrollController` | `IScrollController`、既定の向き OneWayToSource | SettingsView ごとに 1 つ、SettingsView 自身が作った実体。外から別の値を代入しても自身の実体に戻る。実装クラスは公開しない |
| `SettingsView.ScrollControllerReadyCommand` | `ICommand?` | 命令が効くようになった合図。Host が作られて取り付けが済むたびに (Host の世代ごとに 1 回)、`CanExecute(null)` が真なら `Execute(null)` を呼ぶ。ページの再訪問や Android の Activity 再生成で Host が作り直されたときも改めて呼ばれる |
| `Section.SectionId` / `CellBase.CellId` | `string?`、既定 null | 手で並べた Section / Cell を命令の対象として指す明示 ID。表示・Bridge の ID・値の書き戻しには影響しない |

```xml
<ks:SettingsView ScrollController="{Binding Scroll}"
                 ScrollControllerReadyCommand="{Binding ScrollReadyCommand}">
  <ks:Section HeaderText="通知" SectionId="notification">
    <ks:SwitchCell Title="プッシュ通知" />
  </ks:Section>
</ks:SettingsView>
```

```csharp
// ViewModel
public IScrollController? Scroll { get; set; }   // SettingsView が自身の実体を代入する
public ICommand ScrollReadyCommand { get; }

// コンストラクター: 開いたら「通知」Section へ送る
ScrollReadyCommand = new Command(() => Scroll?.ScrollToSection("notification"));
```

Host の作り直しでは、Bridge が手放す前の位置へ戻す要求を合図より先に済ませているため、合図の中で出した命令は戻しの後に実行され、最終位置を決める ([表示への反映と Host の寿命](maui-rendering-lifecycle.md) の復元の表)。合図は復元が画面に反映し終わるのを待たない。1 回だけ命令したい ViewModel は自分で判定する。

**対象の解決**: `target` は、明示 ID (`CellId` / `SectionId`) が等しい要素を表示順で最初に採り、無ければ ItemsSource の項目が等しい生成物 (Cell は Section の `ItemsSource`、Section は SettingsView の `ItemsSource` から生成したもの) を表示順で最初に採る。明示 ID を先にするのは、宣言 UI の「明示 ID が勝つ」(core/ADR-0036) とそろえるため。項目との対応は生成物の `BindingContext` ではなく生成時の項目の並びで持つので、生成物の `BindingContext` を差し替えても、ItemsSource の移動・置換の後も、元の項目で指せる。解決できないとき (null を含む) は命令を Native へ渡さず、Debug 出力に英語の警告を出す。非表示の要素は解決の対象に含め、表示されていないことによる「何もしない」は Native に委ねる。

**Host が無い間**: Host の初回生成前の命令は何もしない (Debug 出力に英語の警告を出し、後から接続しても届かない)。Handler の切断で Host が解放されている間の命令は Bridge まで届き、そこで何もしない。ハンドルは持ち主の SettingsView を弱参照で持つため、ViewModel がハンドルをページより長く保持しても SettingsView と Native Host を生かし続けない (回収後の命令は何もしない)。命令は UI スレッドから呼ぶ。

## してはいけないこと・制約

`Section` / `Cell` は所属先 (`SettingsView` / `Section`) の**論理子**であり、`{Binding}` は継承 BindingContext で解決される。設定したどの BindableProperty でも `DynamicResource` と `AppThemeBinding` が再評価される — 祖先 (ページ / アプリ) の Resources を差し替えたとき、およびアプリの外観が変わったときに追随し、色を入れ直す購読コードは要らない。所属を解かれた Section / Cell (`Parent` が null) は旧所属先の Resources に追随しない。`x:Reference` は namescope 経由の別機構で、**一度きりの初期解決は届くが、その後の追随がない**。accessory View と `CustomCell.Content` も同じく論理子になる ([表示への反映と Host の寿命](maui-rendering-lifecycle.md))。

- 同じ Section / Cell / View のインスタンスを複数箇所へ置かない — 他所に所有されたままの Section / Cell の追加は、その時点 (増減を通知しない素の `List<T>` では変換の時点) で `InvalidOperationException` になる ([表示への反映と Host の寿命](maui-rendering-lifecycle.md))
- Binding assembly (`KsSettingsView.Binding.*`) の型を直接使わない — アプリ向け公開契約は facade のみ ([native-bridge.md](native-bridge.md) の禁止事項と同じ理由)
- 内容サイズを問われる配置 ([表示への反映と Host の寿命](maui-rendering-lifecycle.md) の「配置の制約」) に Android で入力 Cell を置いて編集させない — フォーカス喪失の既知経路が残る

## 現時点の範囲

利用者定義 Cell 型の登録機構 (maui/ADR-0019)、CustomCell の `ContentTemplate` と行の仮想化、D&D 並べ替え等の Native 起点強化 (native 側の機能追加を起点に MAUI へ伝搬する強化) は未提供 (ロードマップ `kasane/roadmaps/maui-support/` の後続フェーズ)。CustomCell は行数分の View が常存するため、大量行を並べる用途は仮想化の提供まで見送る。配布状況は上の「導入と前提」を参照。

## 関連

- [Cell の MAUI 表現](maui-cells.md) — 各 Cell の公開プロパティの型・双方向バインド・PickerCell の候補と選択・CustomCell
- [スタイルの MAUI 表現](maui-styling.md) — Theme / CellStyle / ListStyle / Section 装飾の公開形とプロパティ一覧
- [表示への反映と Host の寿命](maui-rendering-lifecycle.md) — 更新の意味論・lifecycle の保証・配置の制約 (Android の measure 契約)
- [MAUI Native Bridge の interop 境界](native-bridge.md)
- [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md)
- [入力 Cell](../../core/cells/input-cells.md) / [基本 Cell](../../core/cells/basic-cells.md) / [CustomCell](../../core/cells/custom-cell.md) — Cell 意味論の共通契約
- [MauiView の native 実体化機構](../architecture/view-materialization.md) — accessory View と `CustomCell.Content` を native へ届ける内部機構
- [MAUI binding の Native artifact 統合](../architecture/binding-build-integration.md) — pack の構成と最低 OS 版のビルド時ガード
- [スクロール制御](../../core/architecture/scroll-control.md) — 3 platform 共通の命令・位置・実行順の契約

決定の経緯: cross/ADR-0032 (Section / Cell を SettingsView の論理子にする)、maui/ADR-0025 (3 パッケージ構成と名前空間 `KsSettingsView`)、maui/ADR-0008 (AiForms 互換公開面の方針)、maui/ADR-0009 (net10.0 TFM + テスト seam)、maui/ADR-0010 (AndroidX 版競合の binding 層吸収)、maui/ADR-0011 (per-type 輸送)、maui/ADR-0016〜0018 (accessory View の実体化・輸送・更新セマンティクス)、core/ADR-0023 (Header / Footer の表示トグル)、maui/ADR-0029 (スクロール命令のハンドルを SettingsView が作って ViewModel へ渡す)、maui/ADR-0030 (Host の作り直しをまたぐスクロール位置)、maui/ADR-0026 (iOS icon の所有権分類 — MAUI の最低版の前提)

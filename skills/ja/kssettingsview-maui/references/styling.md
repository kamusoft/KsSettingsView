# スタイル

画面の見た目にかかわるレシピ: 画面全体の既定値、Cell ごとの上書き、list の外観、Section 装飾、Header / Footer、配置場所。XAML の断片は [SKILL.md](../SKILL.md) の最小動作コードにある `ks` 名前空間宣言を前提とする。

描画値は次の順で解決される: Cell 種別が意味として持つ値 (ButtonCell のタイトル色など) → Cell ごとの上書き → `SettingsView` の画面全体の既定値 → platform 既定。未指定は「次の段から継承する」意思であって「何も使わない」ではない。

## 画面全体の既定値を決める

画面全体の値は `SettingsView` の個別プロパティとして並んでいる。

```xml
<ks:SettingsView BackgroundColor="#F2EFE6"
                 CellBackgroundColor="#FFFFFF"
                 SeparatorColor="#E6DAB9"
                 SelectedColor="#50FFBF00"
                 CellAccentColor="#FFBF00"
                 DisabledTextColor="#999999"
                 CellTitleColor="#555555"
                 CellPlaceholderColor="#B0A98F"
                 HeaderTextColor="#CC9900"
                 HeaderBackgroundColor="#FBF3DA"
                 FooterTextColor="#999999"
                 FooterBackgroundColor="#FBF3DA"
                 ScrollIndicatorVisible="True">
  <ks:Section HeaderText="General">
    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

`BackgroundColor` は list 全体の下地、`CellBackgroundColor` は Cell の既定背景で、一方から他方を推論しない。`HeaderBackgroundColor` / `FooterBackgroundColor` は Section の Header / Footer 領域を塗る指定だが、両 platform に届かない唯一の組でもある — iOS ではこれらは領域へ適用されず、効くのは `HeaderTextColor` / `FooterTextColor` だけになる。`CellPlaceholderColor` は全 `EntryCell` のプレースホルダ文字色の既定で、Cell ごとには `PlaceholderColor` で上書きする。どちらも未指定なら OS 既定のプレースホルダ色のままで、ダークモードにも自動で追従する。

## Cell 1 つの見た目を上書きする

同じ値が Cell ごとにも用意されていて、指定した Cell だけがその値で描かれる。

```xml
<ks:LabelCell Title="Danger zone"
              ValueText="enabled"
              TitleColor="#CC3333"
              ValueTextColor="#CC3333"
              BackgroundColor="#FFF3F3" />
```

操作系の Cell はさらに `AccentColor` を持ち、スイッチのつまみ・チェック印・Picker の強調表示の色になる。

```xml
<ks:SwitchCell Title="Push notifications" On="True" AccentColor="#34C759" />
```

## スタイルプロパティの一覧

画面全体の既定値は `SettingsView` に、Cell ごとの上書きは `CellBase` (全 Cell 共通の基底) に、次のプロパティとして並んでいる。個々の使い方はこのファイルの各レシピが扱う。いずれも bindable property で、対応する `FooProperty` という名前の `BindableProperty` フィールド (例: `CellTitleColorProperty`) を持つ。

`SettingsView` (画面全体の既定):

| 分類 | プロパティ |
|---|---|
| 色・挙動 | `SeparatorColor`、`SelectedColor`、`CellBackgroundColor`、`CellAccentColor`、`DisabledTextColor`、`CellPlaceholderColor`、`ScrollIndicatorVisible`、`RowHeight`、`HasUnevenRows` |
| Header 書式 | `HeaderTextColor`、`HeaderBackgroundColor`、`HeaderFontFamily`、`HeaderFontSize`、`HeaderFontAttributes`、`HeaderHeight` |
| Footer 書式 | `FooterTextColor`、`FooterBackgroundColor`、`FooterFontFamily`、`FooterFontSize`、`FooterFontAttributes` |
| Cell タイトル既定 | `CellTitleColor`、`CellTitleFontFamily`、`CellTitleFontSize`、`CellTitleFontAttributes` |
| Cell 値テキスト既定 | `CellValueTextColor`、`CellValueTextFontFamily`、`CellValueTextFontSize`、`CellValueTextFontAttributes` |
| Cell 説明文既定 | `CellDescriptionColor`、`CellDescriptionFontFamily`、`CellDescriptionFontSize`、`CellDescriptionFontAttributes` |
| Cell ヒント既定 | `CellHintTextColor`、`CellHintFontFamily`、`CellHintFontSize`、`CellHintFontAttributes` |
| アイコン | `CellIconSize`、`CellIconRadius` |
| Section 装飾 | `SectionMargin`、`SectionCornerRadius`、`SectionBorderWidth`、`SectionBorderColor` |

`CellBase` (Cell ごとの上書き):

| 分類 | プロパティ |
|---|---|
| タイトル | `TitleColor`、`TitleFontFamily`、`TitleFontSize`、`TitleFontAttributes` |
| 値テキスト | `ValueTextColor`、`ValueTextFontFamily`、`ValueTextFontSize`、`ValueTextFontAttributes` |
| 説明文 | `DescriptionColor`、`DescriptionFontFamily`、`DescriptionFontSize`、`DescriptionFontAttributes` |
| ヒント | `HintTextColor`、`HintFontFamily`、`HintFontSize`、`HintFontAttributes` |
| Cell・アイコン | `BackgroundColor`、`IconSize`、`IconRadius`、`Height` |

## フォントを変える

フォントはテキストのスロットごとに 3 つのプロパティに分けて公開されていて、画面全体にも Cell ごとにも指定できる。

```xml
<ks:SettingsView CellTitleFontFamily="OpenSansRegular"
                 CellTitleFontSize="16"
                 CellDescriptionFontSize="12"
                 HeaderFontAttributes="Bold">
  <ks:Section HeaderText="General">
    <ks:LabelCell Title="Highlighted" TitleFontAttributes="Bold" TitleFontSize="18" />
  </ks:Section>
</ks:SettingsView>
```

## Cell の高さを決める

`RowHeight` が画面全体の基準で、`Height` が Cell ごとの上書き。`HasUnevenRows="True"` なら高さは最低値として働き各 Cell が内容に応じて伸び、`False` なら固定される。`RowHeight` は正値だけが有効なので「自動」を意味する数値は無い — どの Cell も内容に合わせたいときは `RowHeight` を指定せず、`HasUnevenRows="True"` に任せる。

```xml
<ks:SettingsView HasUnevenRows="True">
  <ks:Section HeaderText="Account">
    <ks:CommandCell Title="Tanaka Taro" Description="tanaka.taro@example.com" Height="80" />
  </ks:Section>
</ks:SettingsView>
```

## Section の区切り方を切り替える (Classic の罫線 / Modern の角丸 Container)

`ListStyle` (`SettingsViewStyle` 型) は Section の区切り方を選ぶ。`Classic` は Cell と Section の境界を罫線で引くだけで、Cell は画面の全幅に並ぶ。`Modern` は Section の Cell だけを角丸の Container にまとめ、Section Header / Footer はその Container の外側に置く。切り替えても内容と identity は変わらない。

```xml
<ks:SettingsView ListStyle="Modern">
  <ks:Section HeaderText="General">
    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

## Modern の Section Container を調整する

Container と Section 周りの余白は 4 つのプロパティで表す。指定は画面内の全 Section に効く。`SectionMargin` の左右は leading / trailing の意味なので文字の流れる向きに従い、`Classic` では上下成分だけが効く。

```xml
<ks:SettingsView ListStyle="Modern"
                 SectionMargin="16,22,16,0"
                 SectionCornerRadius="12"
                 SectionBorderWidth="1"
                 SectionBorderColor="#C7C7CC">
  <ks:Section HeaderText="General">
    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

未指定のものは platform 既定へ落ちる — 既定の余白と角丸は両 platform で同じ値、Border は描かれない。

## Header と Footer を付ける

Section は `HeaderText` / `FooterText`、画面全体は `RootHeaderText` / `RootFooterText` を持つ。`HeaderHeight` は Section の Header を固定高さにし、はみ出した内容は切り詰められる。

```xml
<ks:SettingsView RootHeaderText="Settings"
                 RootFooterText="Version 1.0.0">
  <ks:Section HeaderText="General"
              FooterText="Applies to this device only."
              HeaderHeight="60">
    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

## Header と Footer に View を置く

テキストの代わりに任意の MAUI View を置ける。スロットは 4 つ — 画面全体の `RootHeaderView` / `RootFooterView` と Section の `HeaderView` / `FooterView`。View が設定されている間はテキストより優先され、View を null に戻すとテキストの表示に戻る。

```xml
<ks:SettingsView>
  <ks:SettingsView.RootHeaderView>
    <Border BackgroundColor="#DDEBFF" StrokeThickness="0" Padding="16,12">
      <Label TextColor="#1F4E9C" FontAttributes="Bold" Text="Account" />
    </Border>
  </ks:SettingsView.RootHeaderView>

  <ks:Section>
    <ks:Section.HeaderView>
      <Border BackgroundColor="#E4F3E6" StrokeThickness="0" Padding="16,12">
        <Label TextColor="#2E6B33" Text="{Binding SectionCaption}" />
      </Border>
    </ks:Section.HeaderView>

    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

これらの View はページの logical tree に載り、所有者の `BindingContext` を継承するので、中のバインドは追加の配線なしに解決される。View の中身が変わればその場で表示が更新され、`HeaderHeight` で高さを固定していない限り領域も追従する。

## アイコンの大きさを決める

アイコンのサイズと角丸は画面ごとにも Cell ごとにも解決される。

```xml
<ks:SettingsView CellIconSize="32" CellIconRadius="8">
  <ks:Section HeaderText="General">
    <ks:CommandCell Title="Wi-Fi" IconSource="ic_wifi.png" IconSize="24" IconRadius="0" />
  </ks:Section>
</ks:SettingsView>
```

## ページ上のどこに置くか

`SettingsView` は大きさがレイアウト側で決まる場所 — ページ直下・Grid の `*` 行・明示サイズ指定 — に置く。この置き方なら、内容がどれだけの高さになるかをコントロールに問い合わせる場面が生じない。

```xml
<Grid RowDefinitions="Auto,*">
  <Label Margin="16" Text="Settings" />
  <ks:SettingsView Grid.Row="1">
    <ks:Section HeaderText="General">
      <ks:LabelCell Title="Version" ValueText="1.0.0" />
    </ks:Section>
  </ks:SettingsView>
</Grid>
```

内容サイズを問われる配置 — `VerticalStackLayout` の直下、縦 `ScrollView` の content、Grid の `Auto` 行 — は避ける。表示自体は成立するが、Android では list の measure の途中で編集中の入力欄がフォーカスを失うことがある。

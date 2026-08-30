# レビュー結果: rollout-user-skills / kssettingsview-maui Skill (001 回目)

**日付**: 2026-08-26
**判定**: CHANGES_REQUESTED
**対象**: `skills/en/kssettingsview-maui/` と `skills/ja/kssettingsview-maui/` の各 5 ファイル (SKILL.md + references/{cells,updates,styling,custom-cells}.md)

## サマリー

構成・frontmatter・翻訳ロックステップ・リンク解決・コード例のコメントレス規約はすべて満たしており、レシピ形式と能力マップの設計原則も守られている。ja 訳は en と意味的に等価で、description の英語キーワードも発火に足りる。一方、**XAML コード例のコンパイル検証を実施した結果、公開されているレシピのうち 1 件が実際にビルドエラーになり、もう 1 件は本文の指示どおりに貼るとビルドエラーになる**ことが判明した。加えて API 挙動の記述に 3 件の事実誤り (Format の解釈系・ButtonCell の HintText・NuGet パッケージの存在) がある。いずれも「利用者がコピーして動く」というデルタスペックの要求に直接抵触するため CHANGES_REQUESTED とする。

### 実施した検証 (コンパイル検証あり)

- 作業ツリー外の一時領域に `net10.0` の MAUI プロジェクトを作り、`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` を ProjectReference して、**en 5 ファイルの XAML コードブロック 38 件すべて**を ContentPage 化してビルドした (ja はコードブロックが byte 一致のため同一)。C# コードブロック (MauiProgram / DataTemplateSelector / SliderCell / Root・Cells 操作 / DisplayFormatter / Content 差し替え) も同一プロジェクトで逐語コンパイルした。リポジトリは改変していない (`git status` にビルド生成物なし)。
- **Debug 構成では XamlC が検証を行わない**ことをミューテーション probe で確認した (既知プロパティを実在しない名前へ変えてもビルドが通る)。そのため `-c Release` (XamlC 有効) で再実行し、同じミューテーションが `XC0009` で落ちることを確かめたうえで本検証の結果を採用している (lessons/code-review L-001 の手法)。
- 結果: **Release ビルドのエラーは 1 件** (下記 Critical)。それ以外の 37 件の XAML と全 C# 断片はエラーなしでコンパイルされた。
- Android 専用の `MainActivity` 断片は net10.0 では検証できないため、`maui/tests/KsSettingsView.MauiHost/Platforms/Android/MainActivity.cs` との逐語突合で代替した (属性部分は完全一致)。
- 導入節のバージョン表は `global.json` と `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` の取得値と一致することを確認した。

## 指摘事項

### [🔴 Critical] `SectionMargin` の XAML 属性記法がコンパイルエラーになる

**該当箇所**: `skills/en/kssettingsview-maui/references/styling.md:93` / `skills/ja/kssettingsview-maui/references/styling.md:93`

**問題点**: レシピ「Adjust the section box / Section の箱を調整する」の

```xml
<ks:SettingsView ListStyle="Modern"
                 SectionMargin="16,22,16,0"
                 ...>
```

は Release ビルドで次のエラーになる (再現済み):

```
XamlC error XC0009: No property, BindableProperty, or event found for "SectionMargin",
or mismatching type between value and property.
```

原因は `maui/KsSettingsView.Maui/SettingsView.cs:443-448` で `SectionMarginProperty` の returnType が `typeof(Thickness?)` と宣言されていること。XAML の `ThicknessTypeConverter` は `Thickness` にしか結び付いておらず、`Thickness?` 相当の属性文字列変換が解決できない。Debug 構成では XamlC が検証しないためビルドは通るが、実行時のインフレートで同じ変換に失敗する。他の nullable プロパティ (`RowHeight` `int?` / `HeaderHeight` `double?` / `SectionCornerRadius` `double?` / `SectionBorderColor` `Color?` / `UIStyle` `DatePickerUIStyle?` / `TitleAlignment` `TextAlignment?` など) は属性記法で問題なくコンパイルされたので、破綻しているのは `Thickness?` のこの 1 プロパティだけである。

**推奨修正**: レシピを実際にコンパイルが通る形へ差し替える。プロパティ要素記法が通ることは検証済み:

```xml
<ks:SettingsView.SectionMargin>
  <Thickness>16,22,16,0</Thickness>
</ks:SettingsView.SectionMargin>
```

(C# から `SectionMargin = new Thickness(16, 22, 16, 0);` でも可)。en/ja でコードブロック byte 一致を保つこと。

なお、根本原因は facade 側の `typeof(Thickness?)` 宣言であり、「属性記法で書けるようにする」修正は公開 API の挙動変更にあたるため本 change のスコープ外と判断する。**実装側の課題として別途起票することを推奨する** (concepts の記述 `SectionMargin` (`Thickness?`) と実装は一致しており concepts/実装の矛盾ではない。XAML 利用性の欠落)。

### [🟠 Major] cells.md のリード文どおりに断片を貼るとコンパイルエラーになる

**該当箇所**: `skills/en/kssettingsview-maui/references/cells.md:3` / `skills/ja/kssettingsview-maui/references/cells.md:3`

**問題点**: リード文が「the fragments are meant to be pasted inside a `<ks:SettingsView>` element」/「断片は `<ks:SettingsView>` の中に貼る形で書いてある」と述べているが、このファイルの断片 13 件は Cell 単体 (`<ks:LabelCell>` `<ks:CommandCell>` `<ks:EntryCell>` …) であり、`SettingsView` の content property は `Root` (`IList<Section>`) なので Cell を直下に置くとビルドが落ちる。実測:

```
XamlC error XC0009: No property, BindableProperty, or event found for "Root",
or mismatching type between value and property.
```

初学者がこのリード文を字面どおりに実行すると最初のビルドで詰まる。同ファイル冒頭に「Rows always live inside a section.」の節はあるが、リード文の指示と矛盾している。

**推奨修正**: リード文を「断片は `<ks:SettingsView>` 内の `<ks:Section>` の中に貼る (Section 自体を含む断片はそのまま `<ks:SettingsView>` 直下)」の趣旨へ改める。`references/styling.md` / `references/custom-cells.md` のリード文は貼り付け位置を主張していないため、そちらに合わせるのでもよい。

### [🟠 Major] `TimePickerCell.Format` を「.NET の書式文字列」と説明しているが解釈系は platform

**該当箇所**: `skills/en/kssettingsview-maui/references/cells.md:144` / `skills/ja/kssettingsview-maui/references/cells.md:144`

**問題点**: 「`Format` is a standard .NET time format string」/「`Format` は .NET の標準的な時刻書式文字列」と書かれているが、実装の XML doc (`maui/KsSettingsView.Maui/TimePickerCell.cs:65-66`、`maui/KsSettingsView.Maui/DatePickerCell.cs:116-117`) は「書式の解釈は platform の日時フォーマッタに従う」と明記しており、`Format` は文字列のまま Bridge を通って iOS の `DateFormatter` / Android の `DateTimeFormatter` へ渡される。`Time` の型が `TimeSpan` であることと相まって、この説明を信じた利用者は .NET の `TimeSpan` 書式 (`hh\:mm` のエスケープ等) を書いて破綻するか、.NET 固有の指定子が効かない理由を追えなくなる。例に使われている `HH:mm` は両 platform のフォーマッタでたまたま成立するため、誤りが例からは見えない。

**推奨修正**: 「書式文字列は各 platform の日時フォーマッタ (iOS `DateFormatter` / Android `DateTimeFormatter`) が解釈する。.NET の書式指定子ではない」旨へ改める。`DatePickerCell` 側 (同ファイル 155 行付近) にも同じ注記を置くと対称になる。

### [🟠 Major] `HintText` が `ButtonCell` で使えないと書かれているが、実際には使える

**該当箇所**: `skills/en/kssettingsview-maui/references/cells.md:183` / `skills/ja/kssettingsview-maui/references/cells.md:183`

**問題点**: 「Every cell except `ButtonCell` and `CustomCell` carries `Description` under the title, **and the same set carries `HintText`**」/「`ButtonCell` と `CustomCell` を除く全 Cell が … `Description` を持ち、**同じ範囲の Cell** が … `HintText` を持つ」となっているが、除外範囲が `HintText` には当てはまらない。

- `ButtonCell`: `maui/KsSettingsView.Maui/ButtonCell.cs:114-121` の `CreateSnapshot` は `Description = null` で潰すが `HintText` は基底の写しをそのまま運ぶ。源泉 concept も `kasane/concepts/core/cells/basic-cells.md:36` で「全7種が … `hintText` … を持つ。`ButtonCell` だけは `description` を公開しない」と、`HintText` は 7 種すべてが持つと明言している。
- `CustomCell`: `maui/KsSettingsView.Maui/CustomCell.cs:215-223` は `Description` も `HintText` も写さない (除外は正しい)。

つまり `ButtonCell` は `Description` のみ非対応であり、現状の記述は使える機能を使えないと案内している。

**推奨修正**: 「`Description` は `ButtonCell` と `CustomCell` を除く全 Cell、`HintText` は `CustomCell` を除く全 Cell」へ分けて記述する。

### [🟡 Minor] 存在しない NuGet パッケージ経由の利用を示唆している

**該当箇所**: `skills/en/kssettingsview-maui/SKILL.md:30` / `skills/ja/kssettingsview-maui/SKILL.md:30`

**問題点**: 「Reference the `KsSettingsView.Maui` project (or package)」/「`KsSettingsView.Maui` プロジェクト (またはパッケージ) を参照し」と書かれているが、`kasane/concepts/maui/api/maui-facade.md:94` は「配布は ProjectReference のみ (NuGet パッケージングは別途)」「NuGet パッケージ参照経由の利用者への効果は未検証」と明記している。また `kasane/concepts/cross/conventions/public-identifiers.md` の「してはいけないこと」に「実装のない MAUI product / package ID を、現在利用可能な識別子として列挙しない」がある (同 concept は manifest 上 SKILL.md の源泉に含まれている)。

**推奨修正**: 現時点は ProjectReference のみである旨へ改める (将来パッケージ配布が始まった時点で docs-refresh が追従する)。

### [🟡 Minor] Material3 テーマ未適用時の「failure is silent」が裏付けのない挙動主張

**該当箇所**: `skills/en/kssettingsview-maui/SKILL.md:59` / `skills/ja/kssettingsview-maui/SKILL.md:59`

**問題点**: 「and the failure is silent」/「失敗しても何も表示されない」と断定しているが、源泉 concept (`kasane/concepts/core/styling/style-resolution.md:58`、`kasane/concepts/android/api/android-native-host.md:105-107`) はいずれも「`?attr/materialSwitchStyle` を解決できないテーマだけで動作すると想定してはならない」までしか述べておらず、失敗の現れ方 (無表示か例外か) を契約していない。同じ前提を説明している `skills/{en,ja}/kssettingsview-android/SKILL.md:44` はこの主張をしていないため、Skill 間でも不整合になっている。誤った失敗モードを案内すると、実際には別の症状 (インフレート例外等) が出たときに切り分けを誤らせる。

**推奨修正**: 失敗の現れ方への言及を落とし、「Material3 派生でないテーマでの動作は前提にできない」までに留める (android Skill の表現に揃える)。

### [🟡 Minor] `MainActivity` 断片だけ using が欠けており、そのままでは通らない

**該当箇所**: `skills/en/kssettingsview-maui/SKILL.md:61-71` / `skills/ja/kssettingsview-maui/SKILL.md:61-71`

**問題点**: 直前の `MauiProgram` 断片は `using KsSettingsView.Maui;` / `using Microsoft.Maui.Hosting;` を含めて完動する形で書かれているのに対し、`MainActivity` 断片は `[Activity]` (`Android.App`)、`LaunchMode` / `ConfigChanges` (`Android.Content.PM`)、`MauiAppCompatActivity` (`Microsoft.Maui`) の using を持たない。属性部分は `maui/tests/KsSettingsView.MauiHost/Platforms/Android/MainActivity.cs` と逐語一致しており内容は正しいが、同じ SKILL.md 内で完動度の基準が揃っていない。

**推奨修正**: 3 つの using を足して他の断片と同じ完動形にするか、「MAUI テンプレートの `Platforms/Android/MainActivity.cs` の `[Activity]` 属性を次で置き換える」と貼り先を明示する。

### [🟡 Minor] 選択面の「確定して初めて反映される」契約が抜けている

**該当箇所**: `skills/{en,ja}/kssettingsview-maui/references/cells.md:98-166` (Picker / NumberPicker / TimePicker / DatePicker の各レシピ)

**問題点**: manifest で cells.md の源泉に挙げられている 3 concept (`core/cells/picker-selection-surface.md` / `number-picker-selection-surface.md` / `date-picker-selection-surface.md`) の中核契約は「確定操作でのみ callback が 1 回発火し、非確定 dismiss はどの経路でも発火せず作業状態を破棄する」である (それぞれ「保証すること」の筆頭)。cells.md は選択面が開くことしか書いておらず、この契約が en/ja とも一切反映されていない。双方向バインドを張った利用者は「ホイールを回した時点で値が流れる」と誤解しうるし、複数選択の作業状態 (確定まで `SelectedIndices` へ書き戻さない) も同様に見えない。

**推奨修正**: 4 レシピに共通する 1〜2 行として「選択面での変更は確定操作 (iOS の Done / Android の OK、単一選択の Picker は候補タップ) の時点でのみバインド先へ書き戻され、キャンセル・外側タップ・Back では破棄される」を追加する。

### [🟡 Minor] EntryCell の `ValueText` 説明が en だけ自己矛盾している

**該当箇所**: `skills/en/kssettingsview-maui/references/cells.md:82`

**問題点**: 「`EntryCell` puts the editor in the row itself, so it has no `ValueText`; `ValueText` is the edited string and is two-way by default.」— 同一文中で「`ValueText` を持たない」と「`ValueText` が編集対象の文字列」を並べており、そのまま読むと矛盾する。ja 版 (`skills/ja/kssettingsview-maui/references/cells.md:82`) は「`ValueText` を表示用には使わない。`ValueText` が編集対象の文字列で」と意図どおり書けているため、意味レベルで en/ja が等価になっていない (機械検査は通る差分)。

**推奨修正**: en を ja に合わせる (例: "…so `ValueText` is not a separate display slot: it *is* the edited string, and it is two-way by default.")。

### [🔵 Suggestion] `Keyboard` の列挙から `Plain` が漏れている

**該当箇所**: `skills/{en,ja}/kssettingsview-maui/references/cells.md:96`

**問題点**: 「the standard MAUI keyboards (`Default`, `Email`, `Telephone`, `Numeric`, `Url`, `Text`, `Chat`)」と 7 種を挙げて閉じているが、輸送側の `maui/KsSettingsView.Maui/Internals/KsKeyboardKind.cs` は `Plain` を含む 8 種を持つ。網羅列挙の体裁で 1 件欠けている。

**推奨修正**: `Plain` を加えるか、「主なものは」と非網羅であることを示す。

### [🔵 Suggestion] `RowHeight="-1"` は未指定と同義で、説明として冗長・誤解を招きうる

**該当箇所**: `skills/{en,ja}/kssettingsview-maui/references/styling.md:65`

**問題点**: 「Pass `RowHeight="-1"` to ask for automatic heights everywhere」とあるが、`kasane/concepts/core/styling/cell-row-layout.md:66-70` と `style-resolution.md:47` は rowHeight について「正値のみ有効、非正値は未指定として次の段へ」と定めており、`-1` は Native 側 Theme の既定値そのもの (`ios/Sources/KsSettingsViewUI/Theme.swift:158`) である。つまり `RowHeight="-1"` は「RowHeight を書かない」と同じ意味で、自動高さを生んでいるのは `HasUnevenRows` の側。現在の書き方だと `-1` に固有の意味があるように読める。

**推奨修正**: 「行の高さを内容任せにしたいときは `RowHeight` を指定しない (`HasUnevenRows="True"` が既定の可変高さを与える)」の趣旨へ改める。

### [🔵 Suggestion] Picker 系の `ValueText` 明示上書きに触れていない

**該当箇所**: `skills/{en,ja}/kssettingsview-maui/references/cells.md:98-166`

**問題点**: 源泉 concept `kasane/concepts/core/cells/input-cells.md:27,34` は「Picker / NumberPicker / TimePicker / DatePicker は `valueText` も持ち、明示値があれば自動表示より優先する」と定めている。行の表示文言を自前で作りたいという要求は頻出だが、レシピからは `ValueText` が使えることが見えない。

**推奨修正**: 4 レシピのいずれかに「行の表示文字列を自分で決めたいときは `ValueText` を設定する (自動生成より優先される)」を 1 行足す。

## 確認して問題がなかった観点

- **Skill 一式の構成**: en/ja とも `SKILL.md` + `references/{cells,updates,styling,custom-cells}.md` の 5 ファイル、規定外ファイルなし。
- **frontmatter の標準準拠**: 4 フィールドのみ、`name` は en/ja 同一、`metadata.language` がパスと一致、`metadata.source` は `git remote` と一致 (他 3 Skill とも同値)。
- **翻訳ロックステップ**: 5 ファイルすべてで見出し階層列が完全一致、コードブロックは数・順序・言語タグ・内容 (byte) がすべて一致。
- **設計原則**: SKILL.md は発火情報・能力マップ表・導入 (索引へのリンクと最低バージョン)・最小動作コード・references 振り分けを持つ。references は「やりたいこと見出し + リード文 + 完動コード」で統一され、アーキテクチャ解説の読み物は混入していない。
- **内容規約**: コードブロック内のコメントは 0 件、ローカル絶対パス・`docs/`・`openspec` への参照は 0 件、内部リンクは全 32 本が解決。
- **バージョン整合**: .NET SDK 10.0.300 / TFM net10.0-ios・net10.0-android / Microsoft.Maui.Controls 10.0.70 / iOS 16.0 / Android API 29 はビルドファイルと一致。
- **ja description の英語キーワード**: `settings screen` / `facade` / 全 13 Cell 型名 / `two-way binding` / `ItemsSource` / `ItemTemplate` / `Header` / `Footer` / `Classic` / `Modern` / `KsSettingsView.Maui` を含み、日本語本文で発火しない場面を補える。
- **源泉 concepts との整合 (上記指摘を除く)**: 双方向 10 プロパティ表、`ItemsSource` / `ItemTemplate` / `TemplateStartIndex` / `DataTemplateSelector` の意味論、observable でないコレクションの静的描画、同一 UI サイクルのバッチ反映、ページ離脱・再訪の復元、多重配置の `InvalidOperationException` と検査時点、スタイル 4 段解決、`BackgroundColor` と `CellBackgroundColor` の別領域、iOS の header/footer 背景非適用、`SectionMargin` の leading/trailing 解釈と Classic の上下のみ適用、`ListStyle` 切替が identity を変えないこと、Header/Footer の View 優先と null 復帰、`HeaderHeight` の切り詰め、Android measure 契約 (推奨配置・非推奨配置)、CustomCell の full-bleed・タップ棲み分け・`IsEnabled=false` の淡色化・参照が正/内容は live・不適用プロパティの silent no-op — いずれも源泉 concept の記述と一致することを 1 件ずつ確認した。

## アクションプラン

1. `references/styling.md` の `SectionMargin` レシピをコンパイルの通る記法へ差し替える (Critical)。併せて facade 側 `Thickness?` の XAML 属性非対応を実装課題として別途起票する。
2. `references/cells.md:3` のリード文を貼り付け位置が正しくなるよう修正する (Major)。
3. `references/cells.md:144` の `Format` の解釈系を platform フォーマッタへ訂正する (Major)。
4. `references/cells.md:183` の `HintText` の除外範囲を `CustomCell` のみへ訂正する (Major)。
5. `SKILL.md:30` の「(or package)」、`SKILL.md:59` の失敗モード断定、`SKILL.md:61-71` の using 欠落を整える (Minor)。
6. `references/cells.md` の選択面レシピに確定/破棄の契約を追記し、en:82 の自己矛盾文を ja に合わせる (Minor)。
7. Suggestion 3 件 (`Plain` の追加 / `RowHeight="-1"` の書き換え / Picker 系 `ValueText`) をオーナー判断で取捨する。
8. 修正後、en/ja のコードブロック byte 一致と機械検査一式 (tasks 4.1) を再実行する。**その際、XAML の実コンパイル検証は `-c Release` で行うこと** — Debug 構成では XamlC が検証を行わず、本 Critical のようなエラーを検出できない。

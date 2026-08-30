# レビュー結果: rollout-user-skills / kssettingsview-aiforms-migration (001 回目)

**日付**: 2026-08-26
**判定**: CHANGES_REQUESTED

**レビュー対象**:
- `skills/en/kssettingsview-aiforms-migration/SKILL.md` / `references/api-mapping.md`
- `skills/ja/kssettingsview-aiforms-migration/SKILL.md` / `references/api-mapping.md`

## サマリー

構成・機械的品質は高い。frontmatter の 4 フィールド準拠、en/ja のロックステップ (見出し階層・コードブロック 4 本の byte 一致・表 210 行の対応)、内部リンク解決、コードコメント不在、`docs/` / `openspec/` 参照なし、ローカル絶対パスなしはすべて通過し、レシピ形式・能力マップ・「移行で何をしたいか」で切った節立ても設計原則どおりである。ja 訳は en と意味等価で、description の英語キーワード併記も適切。

一方、**対応表の「旧 API 側」の内容が移植元コードと突き合わされていない**。デルタスペック Requirement「生成の内容規約」④ (「API 署名とコード例は concepts の記載に加えて実装コード・テストで最終確認する」) が実行されておらず、生成物は凍結資料 `aiforms-spec-summary.md` の記述をほぼそのまま採っている。結果として、**存在しない旧 API 名 (`RadioCell.GroupProperty`) を対応表の見出し・XAML 例・能力マップの 4 箇所 (×2 言語) に据える** Critical 1 件、型・既定値の誤り 7 件、旧公開メンバーの取りこぼし 8 件、新 API 側の誤り 1 件 (`SettingsViewHandler` を internal と記述) を含む。移行対応表という成果物の性質上、旧 API 名と型の正確さが価値のほぼすべてであるため CHANGES_REQUESTED とする。

誤りの大半が spec-summary の記述と一致しており、根本原因は「凍結資料を正として扱った」1 点に集約される。修正は個別の指摘潰しではなく、**対応表の旧 API 列全体を移植元コードで 1 件ずつ再検証する**形で行うのが妥当 (下の「アクションプラン」参照)。

## 確認した観点と結果

| 観点 | 結果 |
|---|---|
| frontmatter 4 フィールド準拠 / `metadata.language` とパス一致 / en・ja で `name` 同一 | 通過 |
| ja description の英語キーワード併記 | 通過 (型名・API 名を原綴で保持) |
| en/ja ロックステップ (見出し階層・コードブロック数/順序/byte・表行数) | 通過 |
| en/ja の意味等価性 | 通過 (差分は訳語のみ。API 識別子は全行一致) |
| Skill 内容の設計原則 (能力マップ・最小動作コード・レシピ形式・読み物排除) | 通過 |
| 生成の内容規約 ①コメントレス ②絶対パス ③`docs/`・`openspec/` 参照 | 通過 |
| 生成の内容規約 ④実装コードでの最終確認 | **不通過** (指摘 C-1 / M-1〜M-4) |
| Scenario「移行対応表の網羅」(spec-summary 記載の旧 API) | 通過 (§2/§3/§4 の全プロパティが対応表に出現) |
| 旧 API の最終的な正 (移植元コード) との突合 | **不通過** (指摘 C-1 / M-1 / M-3) |
| 新 API 側の型・nullability (`maui/KsSettingsView.Maui/` 実宣言) | 1 件誤り (M-2)、`?` 欠落 2 件 (S-1) 以外は正確 |
| ツール最低バージョン一致 | 新側は一致。旧側に 1 件誤り (m-5) |
| 移行の最小例 (SKILL.md before/after) | after 側は成立。**before 側の RadioCell 例が非成立** (C-1) |

## 指摘事項

### [🔴 Critical] C-1: 旧 API に存在しない `RadioCell.GroupProperty` を対応表の主要識別子として据えている

**該当箇所**:
- `skills/en/kssettingsview-aiforms-migration/references/api-mapping.md:109` および `:115` (XAML 例)
- `skills/ja/kssettingsview-aiforms-migration/references/api-mapping.md:109` および `:115`
- `skills/en/kssettingsview-aiforms-migration/SKILL.md:20` / `skills/ja/kssettingsview-aiforms-migration/SKILL.md:20` (能力マップ)

**問題点**:
AiForms の `RadioCell` に `GroupProperty` という名前のメンバーは**存在しない**。移植元 (`../AiForms.Maui.SettingsView/SettingsView/Cells/RadioCell.cs:13-20`) の宣言は次のとおりで、添付プロパティの名前は `SelectedValue` (BP フィールドは `SelectedValueProperty`、アクセサは `GetSelectedValue` / `SetSelectedValue`) である。

```csharp
public static readonly BindableProperty SelectedValueProperty =
    BindableProperty.CreateAttached(
            "SelectedValue",
            typeof(object),
            typeof(RadioCell),
            default(object),
            BindingMode.TwoWay
        );
```

移植元 Sample の実使用も `../AiForms.Maui.SettingsView/Sample/Views/MainPage.xaml:96` で `sv:RadioCell.SelectedValue="1"` である。

影響は 3 つある。

1. **before の XAML 例 (`:115`) がコンパイルできない** — 存在しない添付プロパティを書いている。「移行の最小例が両側の API で実際に成立するか」というレビュー観点に対する明確な不成立。
2. **利用者の検索が空振りする** — 対応表の存在意義は「手元の XAML にある識別子を引く」ことであり、`sv:RadioCell.SelectedValue` を持つ利用者は `GroupProperty` の行に辿り着けない。SKILL.md の能力マップ (`:20`) も同じ誤った語で誘導している。
3. **実際の対応関係が隠れる** — 旧 `RadioCell.SelectedValue` (添付、`object`、TwoWay) → 新 `RadioCell.SelectedValue` (インスタンスプロパティ、`string`、TwoWay) という「同名のまま添付→インスタンスへ移った」構造が、対応表からは読み取れなくなっている。現在の `:110` 行「(添付プロパティが保持していた選択値)」という匿名の表現は、この同名対応を積極的に覆い隠している。

**推奨修正**:
- `:109` の旧側を `RadioCell.SelectedValue` (`object`、`Section` に付ける添付プロパティ、TwoWay) に改め、新側は `RadioCell.GroupId` (`string`) + `RadioCell.SelectedValue` (`string`、双方向) の 2 段で説明する (旧の添付 1 つが新の 2 プロパティに分かれた、という対応)。`:110` の匿名行は同名対応を明示する行に置き換える。
- `:115` の before XAML を `<sv:Section sv:RadioCell.SelectedValue="{Binding SelectedTheme}">` に修正する。
- SKILL.md `:20` の能力マップの `RadioCell.GroupProperty` を `RadioCell.SelectedValue` (添付プロパティ) に差し替える。
- en / ja 双方、同一位置に同じ修正を入れる (ロックステップ維持)。

---

### [🟠 Major] M-1: 旧 API 側の型・既定値の誤りが 7 件ある

**該当箇所**: `api-mapping.md` の下記行 (en / ja とも同一行番号)

いずれも移植元コードの実宣言と食い違う。「型・nullability の正確性」観点の直接の不通過。

| 行 | 記載 | 移植元の実宣言 (原典) |
|---|---|---|
| `:133` | `EntryCell.MaxLength` (`int`, **`int.MaxValue`**) | 既定は **`-1`** — `Cells/EntryCell.cs:58-64` |
| `:193` | `NumberPickerCell.Number` (型記載なし = 「型も変わらない」の意) | 旧は **`int?`** (既定 `null`)、新は `int` (既定 0) — `Cells/NumberPickerCell.cs:14-19`。**nullable → 非 nullable の型変更が無印** |
| `:197` | `Unit` (`string`) を **`(new)`** 行に配置 | `Unit` は**旧 API に既に存在する** (`string`、既定 `""`) — `Cells/NumberPickerCell.cs:116-129`。新規は `Step` のみ |
| `:201` | `DatePickerCell.Date` (**`DateTime`**) | 旧は **`DateTime?`** (既定 `null`)、新は `DateTime` — `Cells/DatePickerCell.cs:13-19`。C-1 と同じく nullable の取り違え |
| `:202` | 備考「null が無制限を表し、**`DateTime.MinValue` / `MaxValue`** の役目を引き継ぐ」 | 旧の既定は **`1900/1/1` と `2100/12/31`** — `Cells/DatePickerCell.cs:47-53, 68-73`。`MinValue`/`MaxValue` ではない |
| `:182` | `TextPickerCell.Items` (**`IList<string>`**) | **`IList`** (非 generic、既定 `new List<object>()`) — `Cells/TextPickerCell.cs:16-21` |
| `:240` | `SettingsView.RowHeight` (**`double`**, -1) | **`int`** (既定 -1) — `SettingsView.DefineProperites.cs:237-243` |

加えて `:183` の `TextPickerCell.SelectedItem` (**`string`**、双方向) も実際は **`object`** (`Cells/TextPickerCell.cs:79-84`)。

`Date` と `Number` の 2 件は利用者影響が大きい。旧側が nullable で「未設定」を表現できたものが新側では非 nullable になっており (新 `Date` の既定は `1970/1/1`、新 `Number` の既定は `0`)、`DateTime?` / `int?` をバインドしていた ViewModel はコンパイルエラーか意味変化を起こす。現在の表記ではこの移行作業が読み取れない。

**推奨修正**: 上表のとおり旧側の型・既定値を実宣言へ修正する。特に `Date` / `Number` は備考へ「旧は nullable で未設定を表現できたが新は非 nullable。null 許容の ViewModel プロパティは既定値の決定が必要」の趣旨を足す。`Unit` は `(new)` 行から外し、`NumberPickerCell.Unit` → `NumberPickerCell.Unit` の対応行として立てる。

---

### [🟠 Major] M-2: 新 API の `SettingsViewHandler` を「internal」と記述しているが public である

**該当箇所**: `skills/en/.../references/api-mapping.md:279` / `skills/ja/.../references/api-mapping.md:279`

**問題点**:
「`SettingsViewHandler` とその platform 別 partial | internal | `AddKsSettingsView()` が登録する。カスタマイズ点ではない」と書かれているが、実宣言 (`maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:32`) は public であり、namespace `KsSettingsView.Maui.Handlers` に置かれている。さらに public な差し替え口を持つ。

```csharp
public static readonly IPropertyMapper<SettingsView, SettingsViewHandler> Mapper = ...
public SettingsViewHandler()
public SettingsViewHandler(IPropertyMapper? mapper)   // 対応付けを差し替えられる
```

「internal」も「カスタマイズ点ではない」も、記述としては両方成立していない (`IPropertyMapper` を受けるコンストラクタは文字通りのカスタマイズ seam)。Handler カスタマイズを行っていた AiForms 利用者はまさにこの節を読むため、公開性の誤記は誘導として悪い方向に働く。

**推奨修正**: 実態に合わせる。「型は public だが、設定ツリーの反映は facade の変換経路が担うため Cell 描画のカスタマイズ点にはならない」旨に改める (概念側 `kasane/concepts/maui/api/maui-facade.md` の記述もこの立場)。`Mapper` 差し替えコンストラクタの位置づけをどう案内するかは実装側の判断に委ねる。

---

### [🟠 Major] M-3: 旧 `CellBase.Tapped` が public であることを取り落とし、機能の縮小を隠している

**該当箇所**: `skills/en/.../references/api-mapping.md:37` / `skills/ja/.../references/api-mapping.md:37`

**問題点**:
「`CellBase.OnTapped()` (internal) | `CommandCell` / `ButtonCell` / `CustomCell` の `Tapped` イベント | 公開イベントで、`Command` より先に発火する」と書かれ、旧側にはイベントが無かった (internal メソッドのみ) かのように読める。実際は移植元 `Cells/CellBase.cs:12-13` が

```csharp
public event EventHandler Tapped;
internal void OnTapped()
```

を持ち、**AiForms では全 Cell 種別が public な `Tapped` を公開していた**。新 API で `Tapped` を持つのは `CommandCell` / `ButtonCell` / `CustomCell` の 3 型のみ (`maui/KsSettingsView.Maui/` 実宣言で確認) であり、これは機能の**縮小**である。`LabelCell.Tapped` や `SwitchCell.Tapped` を購読していた利用者には代替が無く、本来「代替のないメンバー」節 (`:299` 以降) に載るべき事項が、現在の書き方では「もともと無かったものが公開された」という逆の印象になっている。

**推奨修正**: `:37` の旧側を `CellBase.Tapped` (public event、全 Cell) に改め、新側は「`CommandCell` / `ButtonCell` / `CustomCell` のみ」と範囲を明示する。他 Cell 種別で購読していた場合の代替 (該当 Cell を `CommandCell` / `CustomCell` に置き換える等) を備考に書き、「代替のないメンバー」節にも 1 行立てる。

---

### [🟠 Major] M-4: 移植元に実在する公開メンバー 8 件が対応表に現れない

**該当箇所**: `api-mapping.md` 全体 (en / ja)

**問題点**:
spec-summary 記載分の網羅は満たしているが、spec-summary に載っていない旧公開メンバーが漏れている。いずれも利用者の XAML / コードビハインドに現れうる BindableProperty または public メンバーであり、「検索で見つかるようにここへ集めた」(`:301`) という対応表の宣言と整合しない。

| 旧公開メンバー | 種別 | 原典 |
|---|---|---|
| `EntryCell.Completed` | public event | `Cells/EntryCell.cs:170` |
| `EntryCell.ShowDoneButtonOnIOS` | BindableProperty (`bool`) | `Cells/EntryCell.cs:305-317` |
| `EntryCell.SetFocus()` | public method | `Cells/EntryCell.cs` |
| `PickerCell.UsePickToClose` | BindableProperty (`bool`) | `Cells/PickerCell.cs:308-323` |
| `PickerCell.Padding` | BindableProperty (`Thickness`) | `Cells/PickerCell.cs:329-344` |
| `PickerCell.ShowCommand` | public `Command` (get only) | `Cells/PickerCell.cs:414` |
| `Section.TextColor` | BindableProperty (`Color`、既定 `Colors.Black`) | `SectionBase.cs` |
| `SettingsView.ClearCache()` | public static method (icon キャッシュ) | `SettingsView.cs` |

特に `EntryCell.Completed` は問題が大きい。EntryCell の節 (`:140`) は「値が出ていく経路は `ValueText` の双方向バインドのみ」と断言しているが、旧 API には `CompletedCommand` に加えて `Completed` イベントがあり、対応表はイベントの方に一切触れていない。`CompletedCommand` を消す指示だけを受け取った利用者は、`Completed` の購読が残ったままビルドが通らず立ち往生する。

`Section.TextColor` も同様に、`<sv:Section Title="..." TextColor="...">` と書いていた利用者に対する案内が無い (新 API の対応は `SettingsView.HeaderTextColor`)。

**推奨修正**: 上記 8 件を該当節または「代替のないメンバー」節へ追加する。合わせて、対応表の網羅根拠を spec-summary ではなく移植元コードの公開メンバー列挙に置き換えて再点検する (アクションプラン 1 参照)。

---

### [🟡 Minor] m-1: 旧 API に存在しないメンバーを 2 件挙げている

**該当箇所**: `api-mapping.md:219` / `:311` / `:295` (en / ja とも)

**問題点**:
- `CustomCell.LongCommandParameter` (`:219`、`:311`) — 移植元 `Cells/CustomCell.cs` にあるのは `LongCommand` のみで、`LongCommandParameter` は**存在しない** (長押しの実行は `SendLongCommand()` が `BindingContext` を引数に渡す)。
- `SettingsViewConfiguration.ShouldAutoDisconnect` (`:295`) — `SettingsViewConfiguration` は **internal static** クラスであり、`ShouldAutoDisconnect` も internal。利用者が書けたことはなく、「削除すべき旧 API」として挙げるのは誤り (利用者に見えていたのは `UseSettingsView(bool)` の引数のみで、これは `:292` に既出)。

**推奨修正**: `LongCommandParameter` を 2 箇所から削除する。`ShouldAutoDisconnect` の行を削除するか、「internal のため利用者コードには現れない」と明示する。

---

### [🟡 Minor] m-2: 「`ValueText` を新たに持つ Cell」の一覧が旧 API の継承関係と合っていない

**該当箇所**: `api-mapping.md:76` (en / ja)

**問題点**:
「新たに持つのは `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `SimpleCheckCell` / `RadioCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell`」とあるが、AiForms では `CommandCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` は `LabelCell` 派生であり、**既に `ValueText` を持っていた** (ただし NumberPicker / TimePicker / DatePicker / TextPicker は `private new` で隠蔽していた)。真に新規に得たのは `ButtonCell` / `SwitchCell` / `CheckboxCell` / `SimpleCheckCell` / `RadioCell` の 5 型。

また、新 API で `ValueText` を**持たない**唯一の Cell が `CustomCell` である点にも触れていない (`:221` の CustomCell 節にも記載なし)。

**推奨修正**: 「各 Cell が自分で宣言する形になった」(継承の平坦化) と「新たに値表示を得た 5 型」を書き分ける。`CustomCell` だけが `ValueText` を持たない旨を CustomCell 節に 1 行足す。

---

### [🟡 Minor] m-3: 併存可否について SKILL.md と api-mapping.md が矛盾している

**該当箇所**: `skills/en/.../SKILL.md:30` / `skills/ja/.../SKILL.md:30` ⇔ `api-mapping.md:305` (en / ja)

**問題点**:
SKILL.md は「両ライブラリは型を共有せず、同一プロジェクトへの併存を想定していない」と述べる一方、api-mapping.md の「代替のないメンバー」節はドラッグ並べ替えの代替として「その画面だけ旧ライブラリを使い続ける判断もあり得る」と案内している。後者は同一プロジェクトでの併存が前提であり、前者と両立しない。移行の可否判断に直接効く箇所で読者が判断できなくなる。

**推奨修正**: どちらかに寄せる。併存が技術的に可能である (旧 net9.0-* アセンブリは net10.0-* アプリから参照できる) なら SKILL.md 側を「型を共有しないため XAML namespace は別になる」程度の事実記述に緩める。併存を推奨しないなら api-mapping.md 側から旧ライブラリ継続の案内を外す。

---

### [🟡 Minor] m-4: 既定値が変わる 2 件が無記載で、無指定コードの挙動が黙って変わる

**該当箇所**: `api-mapping.md:154` (SelectionMode) / `:194` (NumberPickerCell.Min/Max)

**問題点**:
- `PickerCell.SelectionMode` — 旧の既定は **`Multiple`** (`Cells/PickerCell.cs`)、新の既定は **`Single`** (`maui/KsSettingsView.Maui/` 実宣言)。`SelectionMode` を XAML に書いていない利用者は、移行後に複数選択が単一選択へ黙って変わる。
- `NumberPickerCell.Max` — 旧の既定は **`9999`** (`Cells/NumberPickerCell.cs:56-61`)、新の既定は **`100`**。`Max` 未指定の行は移行後に上限が 1/100 になる。

対応表は既定値を網羅的には書いていないが、この 2 件は「無指定のまま移行すると挙動が変わる」型であり、移行対応表が拾うべき情報である。

**推奨修正**: 該当行の備考に既定値の変化を明記する。

---

### [🟡 Minor] m-5: 要件表の AiForms 側 .NET SDK が「-」だが移植元に指定がある

**該当箇所**: `api-mapping.md:324` および `SKILL.md:34` (en / ja とも)

**問題点**:
「.NET SDK | - | 10.0.300」と書かれているが、移植元は `../AiForms.Maui.SettingsView/global.json` で `9.0.314` (workloadVersion `9.0.314.3`) を固定している。「-」は「AiForms 側には SDK 要件が無い」と読め、事実に反する。

**推奨修正**: AiForms 列を `9.0.314` にする。

---

### [🟡 Minor] m-6: 旧 `Section` がコレクションそのものだった構造変化に触れていない

**該当箇所**: `api-mapping.md:25` (en / ja)

**問題点**:
新側の説明「content property は `Cells` (`IList<CellBase>`)」は正しいが、旧 `Section` は `SectionBase : Element, IList<CellBase>` であり **`Section` 自身がコレクションだった** (`Add` / `Clear` / `Insert` / `RemoveAt` / `Count` / インデクサ / `CollectionChanged` を public に持つ)。XAML の見た目は変わらないが、C# から `section.Add(cell)` / `section[0]` / `section.Count` を書いていたコードは `section.Cells.Add(cell)` 等への書き換えが要る。対応表にこの案内が無い。

**推奨修正**: `:25` の備考に「旧 `Section` は自身が `IList<CellBase>` だったため、C# のコレクション操作は `Section.Cells` 経由へ書き換える」旨を足す。

---

### [🔵 Suggestion] S-1: 新 API 側 2 件で nullable 注釈が落ちている

**該当箇所**: `api-mapping.md:149` (`PickerCell.ItemsSource`) / `:153` (`PickerCell.SelectedIndices`)

新側の実宣言は `IList<string>?` / `IList<int>?` (`maui/KsSettingsView.Maui/PickerCell.cs`)。対応表は他の全行で新側の `?` を厳密に付けているため、この 2 件だけ表記が不統一。

---

### [🔵 Suggestion] S-2: 「提供しない」と「ロードマップ上まだ無い」が区別できない

**該当箇所**: `api-mapping.md:305-307` ほか「代替のないメンバー」節

ドラッグ並べ替え・スクロール制御 (`ScrollToTop` / `ScrollToBottom`) は、概念側 (`kasane/concepts/maui/api/maui-facade.md` の「現時点の範囲」) では「未提供 — ロードマップの後続フェーズ」と位置づけられている。対応表の「提供していない」「公開していない」は恒久的な非提供と読めるため、移行するか待つかの判断材料が失われている。「現時点では未提供 (後続フェーズで検討)」の別を立てることを勧める。

---

### [🔵 Suggestion] S-3: 旧 `SelectionMode` が MAUI 標準型である旨の注記

**該当箇所**: `api-mapping.md:154`

旧側の `SelectionMode` は AiForms 独自型ではなく `Microsoft.Maui.Controls.SelectionMode` (メンバーは `None` / `Single` / `Multiple`)。新 `PickerSelectionMode` と名前が近く混同しやすいため、旧側が MAUI 標準型であること、`None` に対応先が無いことを備考に置くと引きやすい。

---

### [🔵 Suggestion] S-4: 公開補助型の扱い

`UseNaturalSort` の実体である `NaturalComparer` / `NaturalSortOrder` / `NaturalComparerOptions` は AiForms が public に公開している型で、利用者が直接使っている可能性がある。`:160` の `UseNaturalSort` 行の備考に「並べ替えを自前で行う際、旧 `NaturalComparer` 相当の実装は利用者側へ移る」旨を添えると親切。同様に `DropEventArgs` (`ItemDropped` の引数型) も `:305` から辿れるとよい。

---

## drift 所見 (指摘ではない — `aiforms-spec-summary.md` 側の記述誤り)

本レビューで移植元コードと突き合わせた結果、凍結資料 `kasane/concepts/cross/conventions/aiforms-spec-summary.md` に以下の誤りを確認した。同ファイルは「凍結された歴史資料であり、最終的な正は移植元コード」と明記されているため**本 change の指摘対象外**とするが、上記 Critical / Major の大半がこの誤りをそのまま引き継いだものであり、後続の ksn-drift で処理されるべき所見として記録する。

| spec-summary の記述 | 移植元コードの実宣言 |
|---|---|
| `:141` RadioCell の添付プロパティ名 `GroupProperty` | `SelectedValue` (`Cells/RadioCell.cs:13-20`) |
| `:154` EntryCell.MaxLength 既定 `int.MaxValue` | `-1` (`Cells/EntryCell.cs:58-64`) |
| `:196` NumberPickerCell.Number 型 `int` | `int?` (`Cells/NumberPickerCell.cs:14-19`) |
| `:198` NumberPickerCell.Max 既定 `100` | `9999` (`Cells/NumberPickerCell.cs:56-61`) |
| §3 NumberPickerCell に `Unit` の記載なし | `Unit` (`string`、既定 `""`) が存在 (`Cells/NumberPickerCell.cs:116-129`) |
| `:212` DatePickerCell.Date 型 `DateTime` | `DateTime?` (`Cells/DatePickerCell.cs:13-19`) |
| `:214-215` MaximumDate / MinimumDate 既定 `DateTime.MaxValue` / `MinValue` | `2100/12/31` / `1900/1/1` (`Cells/DatePickerCell.cs:47-53, 68-73`) |
| `:185` TextPickerCell.Items 型 `IList<string>` | `IList` (`Cells/TextPickerCell.cs:16-21`) |
| `:188` TextPickerCell.SelectedItem 型 `string` | `object` (`Cells/TextPickerCell.cs:79-84`) |
| `:230` CustomCell に `LongCommandParameter` の記載 | 存在しない (`LongCommand` のみ) |
| `:92` `Tapped` を internal 相当として記載 | `public event EventHandler Tapped` (`Cells/CellBase.cs:12`) |
| §4 の見出し「40+ 個」 | 実数 49 件 (`SettingsView.DefineProperites.cs`) |
| §4 `RowHeight` の型記載なし (`-1（自動）`) | `int` (`SettingsView.DefineProperites.cs:237-243`) |
| §3 に未記載の公開メンバー | `EntryCell.Completed` / `ShowDoneButtonOnIOS` / `SetFocus()`、`PickerCell.UsePickToClose` / `Padding` / `ShowCommand`、`Section.TextColor`、`SettingsView.ClearCache()` |

## アクションプラン

1. **(最優先) 対応表の旧 API 列を移植元コードで全件再検証する** — 個別指摘の逐次修正ではなく、`../AiForms.Maui.SettingsView/SettingsView/` の `Cells/*.cs` / `SectionBase.cs` / `Section.cs` / `SettingsRoot.cs` / `SettingsView.cs` / `SettingsView.DefineProperites.cs` / `MauiAppBuilderExtension.cs` から public BindableProperty・public プロパティ・public イベント・public メソッドを機械的に列挙し、対応表の行と突き合わせる。デルタスペック「生成の内容規約」④ が要求している手順であり、これを通せば C-1 / M-1 / M-3 / M-4 / m-1 / m-2 / m-4 / m-5 は一掃される。`aiforms-spec-summary.md` は導線としてのみ使い、型・既定値・メンバー名の根拠にしない。
2. **C-1 を最優先で修正** — 対応表の見出し・XAML 例・SKILL.md 能力マップの 4 箇所 (×2 言語)。before 例が成立することを確認する。
3. **M-2 を修正** — 新 API 側は `maui/KsSettingsView.Maui/` の実宣言が正。`SettingsViewHandler` の公開性と `Mapper` 差し替え口の扱いを決めて記述する。
4. **M-4 の 8 件を該当節へ追加** — 特に `EntryCell.Completed` は EntryCell 節の「`ValueText` の双方向バインドのみ」という断言と併せて書き直す。
5. **m-1 / m-2 / m-3 / m-6 / S-1 を反映**。
6. **S-2 / S-3 / S-4 は実装側の判断で採否を決めてよい**。
7. **修正は必ず en / ja 同時に行い**、反映後に機械検査 (tasks.md 4.1) を再実行してロックステップ (見出し階層・コードブロック byte 一致・表行数) の維持を確認する。C-1 の XAML 例修正はコードブロックに触れるため、byte 一致検査の再通過が必須。

## 補足

- ビルド・テストは本 change の対象外 (コード変更なし)。代わりに、対応表の全行を移植元コードおよび `maui/KsSettingsView.Maui/` の実宣言と突き合わせる検証を行った。
- spec-summary 記載分の網羅 (Scenario「移行対応表の網羅」の文言どおりの範囲) は満たしている。不通過なのは、その上位にある「旧 API の最終的な正は移植元コード」という前提と、Requirement「生成の内容規約」④ である。
- 新 API 側の記述精度は総じて高い。`PickerCell.DisplayFormatter` が BindableProperty ではない素の CLR プロパティである点、`CommandCell.HideArrow` の名称、`RadioCell` の `GroupId` / `SelectedValue` / `Value` がすべて非 nullable `string` である点、`SectionMargin` の leading / trailing 解釈、`Tapped` → `Command` の発火順、weak 参照による回収保証は、いずれも実装と概念の双方に一致していた。

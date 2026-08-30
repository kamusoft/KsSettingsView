# レビュー結果: rollout-user-skills (002 回目 — サイクル 1 指摘の検収)

**日付**: 2026-08-26
**判定**: APPROVED
**対象**: `skills/` 全体 (en/ja 36 ファイル) + 索引 2 枚 + ルート README 導線 + `manifest-draft.json`
**性格**: 新規の網羅的レビューではなく、review-001-{ios,android,maui,aiforms-migration,common,fresh} と second-opinion-code-001 の採用指摘に対する修正の反映確認 (検収)。修正箇所の周辺整合と、修正が持ち込んだ新規問題の有無も見た。

## サマリー

サイクル 1 の全 6 レビュー + セカンドオピニオン採用分の指摘 **56 件**を 1 件ずつ現物と突き合わせた結果、**Critical 2 / Major 16 / Minor 22 はすべて反映済み**、Suggestion 16 のうち 13 件が採用・3 件が妥当な見送り (うち 1 件はオーナー判断待ち、2 件は後続グループ担当) だった。特に指摘の中核だった「利用者がコピーして動く」の破れ (Swift の実引数順序 3 件・`@MainActor` 欠落・Android の import 前提・MAUI の `SectionMargin` 属性記法・移行対応表の旧 API 型誤り) は、いずれも実装コード・移植元コードへ独立に当たり直して**正しい形に直っていることを確認した**。

修正起因の新規問題は Critical / Major なし。en/ja のロックステップ (コードブロック byte 一致・見出し階層一致)、frontmatter、内部リンク解決、旧名残・`docs/` 参照・ローカル絶対パスの不在、manifest の網羅不変条件は、修正後の作業ツリーで再検査して全通過している。残るのは優先度の低い Minor 1 件と Suggestion 2 件のみのため APPROVED とする。

### 実施した検証

- **成果物と実装の突合 (再検証)**: 修正で新たに書かれた・書き換わった記述のうち、機械的に真偽が決まるものを実装コードへ当たり直した。iOS `ButtonCell` / `LabelCell` / `Theme` / `CellStyle` の宣言順 (styling.md に新設されたフィールド表 34 行 + 13 行が declaration order と完全一致することを含む)、Android `SettingsRootStore` の Section 操作 5 種と `Section` データクラスの引数名、`DSLReidentifiableCell` / `DSLStyleModifiableCell` / `DSLIconModifiableCell` のメソッド名と所属 package、`KsCellRegistry.CELL_VIEW_TYPE_MIN = 100`、Android モジュールの GAV (`jp.kamusoft.kssettingsview` / `0.1.0-SNAPSHOT`)、iOS `Package.swift` の products と tools version、MAUI `DatePickerCell` の `MinimumDate` / `MaximumDate` / `TodayText` / `PickerTitle`、`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` の実在。
- **移植元コードとの突合 (aiforms 指摘の再検証)**: `../AiForms.Maui.SettingsView/` の実宣言に対して、`RadioCell.SelectedValue` (添付・`object`・TwoWay)、`EntryCell.MaxLength` 既定 `-1`、`EntryCell.TextAlignment` 既定 `End`、`NumberPickerCell.Number` (`int?`) / `Min` 0 / `Max` 9999 / `Unit` (`string`, `""`)、`DatePickerCell.Date` (`DateTime?`) / `MinimumDate` 1900-01-01 / `MaximumDate` 2100-12-31、`Section.TextColor` (`Color`, `Colors.Black`)、`SettingsView.RowHeight` (`int`, -1)、`PickerCell.SelectionMode` 既定 `Multiple`、`CellBase.IconSize` (`Size`)、`global.json` 9.0.314 / MauiVersion 9.0.120 / iOS 14.2 / Android 27 を 1 件ずつ照合。**サイクル 1 で誤りとされた全項目が実宣言どおりに直っており、新たな誤りは出ていない**。
- **機械検査の再実行 (自前で実施)**: en/ja 17 ペアのコードブロック byte 一致と見出し階層一致 (差分 0)、frontmatter 4 フィールド・`metadata.language` とパス一致・`name` の en/ja 一致 (8 部すべて)、内部リンク全件解決、外部 URL 8 種がすべて `github.com/kamusoft/KsSettingsView` 系で言語対応も正しいこと、`docs/` / `openspec` 参照 0 件、ローカル絶対パス・`file://` 0 件、manifest の concept キー集合が作業ツリーと差分 0・SHA-256 全 35 件一致・網羅漏れ 0 件。
- コンパイル検証は各修正ワーカーの実施結果 (iOS スクラッチ SwiftPM / Android `compileDebugKotlin` / MAUI Release XamlC) を前提とし、疑わしい箇所 (順序依存・新規追加 API) のみ上記のとおり宣言レベルで抜き取り再確認した。

---

## 指摘 → 反映状況の対応表

### review-001-ios.md (Major 4 / Minor 3 / Suggestion 3)

| # | 指摘 | 状況 | 確認箇所・根拠 |
|---|---|---|---|
| Major 1 | cells.md の `hintText` / `icon` 順序違反 | **反映済み** | `skills/en/kssettingsview-ios/references/cells.md:230-236` が `valueText` → `icon` → `hintText` の順。`ios/Sources/KsSettingsViewUI/LabelCell.swift` の init 宣言順と一致 |
| Major 2 | styling.md の `LabelCell(title:style:)` と `CellStyle` 順序違反 2 件 | **反映済み** | 同 `styling.md:74-81` が `LabelCell(style: CellStyle(titleColor:cellHeight:backgroundColor:), title:)`。`CellStyle` の宣言順とも一致 |
| Major 3 | updates.md の Store 例に `@MainActor` 欠落 | **反映済み** | `updates.md:10` に `@MainActor`。加えてリード文 `:7` に「Store とその操作はすべて main actor 隔離」の 1 文 (推奨の追加分も採用) |
| Major 4 | 「全 Cell が `valueText` を持つ」の誤り | **反映済み** | `cells.md:225` が例外 2 件 (`ButtonCell` の `description` / `EntryCell` の `valueText`) に修正。ja も同文 |
| Minor 1 | 可視性切り替えの「アニメートされる」断定 | **反映済み** | `updates.md:169` が「rows being added and removed rather than as an in-place update」へ後退。アニメーション言及なし |
| Minor 2 | `.disabled(_:)` を SwiftUI modifier と説明 | **反映済み** | `cells.md:243` が「Cells also offer a `.disabled(_:)` modifier of their own - not SwiftUI's `View.disabled(_:)`」と書き分け |
| Minor 3 | 再利用 CustomCell が content 変化に追随しない | **反映済み** | `custom-cells.md:93-125` が `isDragging` + `shownValue` 方式へ。非ドラッグ時は `content.value` を描画 |
| Suggestion 1 | `titleAlignment` の値と指定例がない | **採用** | `cells.md:41` に `CellTitleAlignment` (`.start` / `.center` / `.end`) を明記、例にも `titleAlignment: .start` を追加。`ButtonCell.swift` の宣言順とも整合 |
| Suggestion 2 | CustomCell に `icon` modifier が効かない旨 | **採用** | `custom-cells.md:58` に追記 |
| Suggestion 3 | PickerCell 単一選択の「確定操作」表現 | **採用** | `cells.md:138` が「there is no separate confirmation step」「Closing the page with Cancel fires nothing」へ。複数選択側は `:152` で書き分け |

### review-001-android.md (Major 3 / Minor 3 / Suggestion 4)

| # | 指摘 | 状況 | 確認箇所・根拠 |
|---|---|---|---|
| Major 1 | 「全 Cell が `valueText`」の誤り | **反映済み** | `references/cells.md:280` が例外 2 件へ。iOS と同一の直し方で 4 ファイル横断で揃っている |
| Major 2 | リード文の import 前提ではコンパイル不能 | **反映済み** | 4 本すべてが完全な import ブロックを持つ。`cells.md` は `getValue` / `setValue` / `Color` / `java.time.*` / `InputType` を含み、`KsImage` / `DatePickerUIStyle` は `...ui` と明記。`styling.md` は `dp` / `sp` / modifier 拡張 7 種、`custom-cells.md` は Compose 側とプレーン Kotlin 側の 2 ブロックに分離。`updates.md` は宣言形 / Store 形の 2 ブロック + 名前衝突の回避策 (別ファイル / import alias) を明示 |
| Major 3 | Section 操作と `replaceAll` / `invalidateAccessoryMeasurement` の欠落 | **反映済み** | `updates.md:80-124`「Add, remove or replace a whole section after display」「Rebuild the whole screen at once」、`:201-211`「Remeasure a header whose Composable changed size」を新設。`SettingsRootStore.kt:87,95,105,128,289` の実シグネチャと引数名が一致 |
| Minor 1 | PickerCell 単一選択の確定意味論 | **反映済み** | `cells.md:180` が「there is no confirm button, and tapping a candidate writes the value back and closes the sheet right away」へ |
| Minor 2 | 導入表の「Gradle 9.5.0」が最低版でない | **反映済み** | `SKILL.md` の Versions 表から Gradle 行を外し、別段落で「Gradle 8.13 以降 (AGP 8.13 が受け付ける最低版)」「ライブラリ自身は 9.5.0 でビルド」と分離 |
| Minor 3 | `minDate` / `maxDate`・icon サイズ / 角丸の未収録 | **反映済み** | `cells.md` に `minDate` / `maxDate` レシピ、`styling.md:129-135`「Size the icon of a row」を追加 |
| Suggestion 1 | `Theme.rowHeight` と `CellStyle.cellHeight` の型差 | **採用** | `styling.md:119` に「`Dp?` と dp 数の `Int` (`-1` = 未指定)」を明記 |
| Suggestion 2 | 行高さの下限 60dp | **採用** | `styling.md:127`「`Theme(rowHeight = 40)` still gives 60dp rows」 |
| Suggestion 3 | DSL 対応 interface の所属 package | **採用** | `custom-cells.md:30` と `:247` の両方で core / ui を書き分け。実ソースの所属と一致 |
| Suggestion 4 | 依存の書き方が示されていない | **採用** | `SKILL.md`「Take the library into your build」に `includeBuild` + `dependencySubstitution` と依存宣言を追加。GAV は `android/gradle/libs.versions.toml:22` の `0.1.0-SNAPSHOT` と一致し、「no Maven release yet / not published to any repository」と明記して `public-identifiers.md` の禁止事項にも抵触しない |

### review-001-maui.md (Critical 1 / Major 3 / Minor 5 / Suggestion 3)

| # | 指摘 | 状況 | 確認箇所・根拠 |
|---|---|---|---|
| Critical | `SectionMargin` の属性記法がコンパイルエラー | **反映済み** | `references/styling.md:93-108` がプロパティ要素記法へ差し替え。加えて「型が nullable な `Thickness` で属性の型変換が届かない」と理由も明示 |
| Major 1 | cells.md リード文の貼り付け位置 | **反映済み** | `cells.md:3` が「`<ks:Section>` で始まる断片は `<ks:SettingsView>` 直下、素の Cell は `<ks:Section>` の中」へ |
| Major 2 | `Format` を「.NET の書式文字列」と説明 | **反映済み** | `cells.md` の TimePickerCell 節が platform フォーマッタ (`DateFormatter` / `DateTimeFormatter`) へ訂正、DatePickerCell 節にも対称の注記 |
| Major 3 | `HintText` の除外範囲 | **反映済み** | `cells.md` の共通フィールド節が「`Description` は `ButtonCell` と `CustomCell` を除く全 Cell、`HintText` は `CustomCell` だけが持たない」へ分離 |
| Minor 1 | 存在しない NuGet パッケージの示唆 | **反映済み** | `SKILL.md`「There is no NuGet package yet」+ `ProjectReference` 手順へ。移行 Skill 側の記述とも一致 |
| Minor 2 | 「failure is silent」の裏付けなき断定 | **反映済み** | 「cannot be assumed to work」へ後退。android Skill の表現と整合 |
| Minor 3 | `MainActivity` 断片の using 欠落 | **反映済み** | `Android.App` / `Android.Content.PM` / `Microsoft.Maui` の 3 using を追加 |
| Minor 4 | 選択面の「確定して初めて反映」契約の欠落 | **反映済み** | `cells.md` に「Rules the picker rows share」節を新設し、確定操作 / 破棄の契約を 4 レシピ共通で記述 |
| Minor 5 | en の `ValueText` 自己矛盾文 | **反映済み** | 「`ValueText` is not a separate display slot here: it is the edited string」へ。ja と意味等価 |
| Suggestion 1 | `Keyboard` から `Plain` が漏れ | **採用** | 8 種列挙へ |
| Suggestion 2 | `RowHeight="-1"` の説明 | **採用** | `styling.md:67` が「正値のみ有効。自動高さは未指定 + `HasUnevenRows="True"`」へ |
| Suggestion 3 | Picker 系の `ValueText` 明示上書き | **採用** | 「All four also carry `ValueText`」の 2 文を追加 |

### review-001-aiforms-migration.md (Critical 1 / Major 4 / Minor 6 / Suggestion 4)

| # | 指摘 | 状況 | 確認箇所・根拠 |
|---|---|---|---|
| C-1 | 実在しない `RadioCell.GroupProperty` | **反映済み** | `references/api-mapping.md:115` が旧側 `RadioCell.SelectedValue` (添付・`object`・TwoWay) へ。`:116` で新側 `GroupId` との 2 段対応を明示、before XAML `:123` も `sv:RadioCell.SelectedValue` へ、`SKILL.md:22` の能力マップも同語へ。移植元 `RadioCell.cs:13-20` と一致 |
| M-1 | 旧 API の型・既定値 7 件 (+1) | **反映済み (全 8 件)** | `MaxLength` `-1` / `Number` `int?` null → `int` 0 / `Unit` を `(new)` から通常行へ (新規は `Step` のみ) / `Date` `DateTime?` null → `DateTime` 1970-01-01 / `MinimumDate` 1900-01-01・`MaximumDate` 2100-12-31 / `TextPickerCell.Items` `IList` / `SelectedItem` `object` / `SettingsView.RowHeight` `int` -1。**8 件すべて移植元の実宣言と再照合して一致**。`Date` / `Number` には nullable → 非 nullable の移行作業も備考に追記されている |
| M-2 | `SettingsViewHandler` を internal と記述 | **反映済み** | `:317` が「public、`KsSettingsView.Maui.Handlers`」+ `Mapper` / `IPropertyMapper?` コンストラクタの位置づけ (View 共通であって Cell 単位ではない) へ |
| M-3 | 旧 `CellBase.Tapped` の public 取り落とし | **反映済み** | `:40` が「public event on every cell」→ 新側 3 型のみ・置き換え方針つき。`:350`「代替のないメンバー」にも 1 行立てている |
| M-4 | 移植元の公開メンバー 8 件の欠落 | **反映済み (8/8)** | `EntryCell.Completed` `:165` / `ShowDoneButtonOnIOS` `:168` / `SetFocus()` `:167` / `PickerCell.UsePickToClose` `:190` / `Padding` `:191` / `ShowCommand` `:192` / `Section.TextColor` `:28` / `SettingsView.ClearCache()` `:280`。EntryCell 節の「値が出ていく経路は `ValueText` のみ」も `Completed` 込みで書き直されている |
| m-1 | 存在しないメンバー 2 件 | **反映済み** | `LongCommandParameter` は `:255` で「旧 API にも無い」と明記する形へ、`ShouldAutoDisconnect` は `:333` で「クラスもメンバーも internal」と明示 (レビュー推奨の 2 択のうち後者) |
| m-2 | `ValueText` を新たに持つ Cell の一覧 | **反映済み** | `:80` が継承の平坦化と「新たに得た 5 型」を書き分け、`CustomCell` のみ非保持も明記。`:256` に旧 `CustomCell` が `CommandCell` 派生だった経緯も追加 |
| m-3 | 併存可否の SKILL.md ⇔ api-mapping 矛盾 | **反映済み** | `SKILL.md:32` を「型を共有しないので XAML namespace は別、画面単位で移行できる。両方参照する構成は未検証なので暫定扱い」へ緩和。`api-mapping.md:343` の「その画面だけ旧ライブラリを使い続ける」と両立する |
| m-4 | 既定値が変わる 2 件 | **反映済み** | `:182` の `SelectionMode` (`Multiple` → `Single`) と `:229` の `Max` (9999 → 100) を太字で明示 |
| m-5 | AiForms 側 .NET SDK が「-」 | **反映済み** | `api-mapping.md:371` / `SKILL.md:44` とも `9.0.314`。移植元 `global.json` と一致 |
| m-6 | 旧 `Section` がコレクションだった構造変化 | **反映済み** | `:25` に `section.Add(cell)` → `section.Cells.Add(cell)` の書き換え案内 |
| S-1 | 新 API 側 2 件の nullable 注釈落ち | **採用** | `:177` `IList<string>?` / `:181` `IList<int>?` |
| S-2 | 「提供しない」と「まだ無い」の区別 | **採用** | `:3` の凡例で "Not provided yet" を定義し、`:34` `:344` `:363` などで使い分け |
| S-3 | 旧 `SelectionMode` が MAUI 標準型である旨 | **採用** | `:182` に「The old type was the MAUI one, whose `None` member has no counterpart」 |
| S-4 | 公開補助型 (`NaturalComparer` 等 / `DropEventArgs`) | **採用** | `:188` `:357` に `NaturalComparer` / `NaturalSortOrder` / `NaturalComparerOptions`、`:343` に `DropEventArgs` |

**アクションプラン 1 (旧 API 列の全件再検証) について**: 抜き取り 12 項目を移植元の実宣言に当て直して全件一致、かつサイクル 1 で挙がっていなかった 8 メンバーが正しく追加されていることから、対応表の旧 API 列は spec-summary ではなく移植元コードを根拠に作り直されたと判断できる。

### review-001-common.md (Major 2 / Minor 5 / Suggestion 2)

| # | 指摘 | 状況 | 確認箇所・根拠 |
|---|---|---|---|
| Major 1 | コピー手順が実行不能 | **反映済み** | `skills/README.md:16-45` (ja 同) が ①clone + `KSSV` 変数 → ②`cd <your-project-root>` + `cp -R "$KSSV/skills/<lang>/<skill-name>" .agents/skills/` → ③エージェント再起動、の 3 ステップへ。作業ディレクトリの前提が両立する |
| Major 2 | コピー後に索引リンクが壊れる | **反映済み** | 全 8 部の `SKILL.md` の索引参照を公開 URL (`https://github.com/kamusoft/KsSettingsView/blob/HEAD/skills/README{,_ja}.md`) へ。en は `README.md`、ja は `README_ja.md` と言語対応も正しい。索引側 `:43` にも「リンクは公開 URL なのでコピー先でも解決する」と明記 |
| Minor 3 | 移行 Skill → maui Skill の相対リンク前提 | **反映済み** | 移行 SKILL.md の兄弟リンクも公開 URL 化 (en/ja それぞれの言語ツリーを指す)。索引の一覧表 `:12` にも「`kssettingsview-maui` と併せてコピーする」を追記 |
| Minor 4 | 日本語利用者が ja 索引に辿り着けない | **反映済み** | `skills/README.md:3` ⇔ `skills/README_ja.md:3` に相互リンク。3 要素の構成 (一覧表 / コピー手順 / 片言語コピー) は維持されている |
| Minor 5 | Cell 数の数え方が不統一 | **反映済み** | 索引 3 行とも「12 種の Cell + CustomCell」に統一 (en/ja 同) |
| Minor 6 | ADR-0017 / log.md の旧 `docs/` パス | **保留が正 (後続グループ 6)** | `kasane/decisions/cross/0017-port-aiforms-to-native.md:27` に旧パスが残存。tasks.md 6.x は未着手であり、コンテキストパッケージの指示どおり本サイクルの対象外 |
| Minor 7 | manifest の comment-policy 除外理由 | **保留が正 (後続 7.1)** | `manifest-draft.json` の理由は未更新。tasks.md 6.1 の追記後に 7.1 で反映する取り決め |
| Suggestion (8) | `lastUpdatedFiles` が空 | **保留が正 (後続 7.1)** | `[]` のまま。最終書き出しで確定する |
| Suggestion (9) | MAUI の「開発中」注記 | **見送り (オーナー判断)** | 索引の MAUI 行に状態注記なし。ルート README 側は proposal の Non-Goals (phase-9) により対象外であり、索引側の要否はオーナー検収 (5.4) の判断事項として残る |

### review-001-fresh.md (初見レビュー)

| 項目 | 状況 | 確認箇所 |
|---|---|---|
| 最重要 1: ライブラリ導入手順が 4 Skill すべてに無い | **反映済み** | iOS = SwiftPM path 依存 (`Package.swift` の products / tools version と一致、`package: "ios"` の理由も説明)、Android = composite build + `dependencySubstitution`、MAUI / 移行 = `ProjectReference` (csproj パス実在) |
| 最重要 2: コピーコマンド実行不能 | **反映済み** | common Major 1 と同じ |
| 最重要 3: コピー後に切れるリンク | **反映済み** | common Major 2 / Minor 3 と同じ |
| 最重要 4: `.agents/skills/` と `.claude/skills/` の分岐条件 | **反映済み** | 索引 `:27` / `:35` が条件つきの 2 択として書き分け |
| 最重要 5: Cell 数の数え方 | **反映済み** | common Minor 5 と同じ |
| 未定義語: `KsCellID` | **反映済み** | ios `updates.md:45` |
| 未定義語: 4 つの accessory 位置 / 3 型の同時初出 | **反映済み** | ios `updates.md:116`、android `updates.md:181-190` (`AccessoryTarget` 4 値を列挙し `SettingsAccessory` / `SectionAccessory` / `RootAccessory` の関係を説明) |
| 未定義語: Android の「Handle」 | **反映済み** | android `SKILL.md` 末尾で `CellHandle` / `SectionHandle` を定義 |
| 未定義語: iOS `registerBasicCells()` / `registerInputCells()` の内訳 | **反映済み** | ios `custom-cells.md:204` |
| 未定義語: `KsCell` / `Cell` の要求メンバ (id / style の要否) | **反映済み** | ios `custom-cells.md:129`、android `custom-cells.md:178-186` (`Cell` は `id` のみ、`style` は `DSLStyleModifiableCell` 由来と明記) |
| 未定義語: `ProgressCell` の 2 定義が矛盾して見える (Android) | **反映済み** | android `custom-cells.md:249` に「same `ProgressCell` as above with those two interfaces added」の橋渡し文 |
| 未定義語: iOS `DSLStyleModifiable` / `DSLIconModifiable` の要求メソッド名 | **反映済み** | ios `custom-cells.md:218` |
| 未定義語: iOS の列挙型名が leading-dot のみ | **反映済み** | `KsSettingsViewStyle` / `DatePickerUIStyle` / `CellTitleAlignment` を型名つきで導入 |
| 未定義語: `Theme` / `CellStyle` の全フィールド一覧 | **iOS のみ反映** | ios `styling.md` に Theme 34 行 + CellStyle 13 行の表を新設 (宣言順と完全一致)。Android 側は未追加 → 下記 Minor 1 |
| 未定義語: iOS「meaning-specific value」の例示 | **反映済み** | ios `styling.md:5` |
| 未定義語: MAUI `DisplayFormatter` の `picker` 取得手段 / `Tapped` 購読例 | **反映済み** | maui `cells.md` に `x:Name` + code-behind 例、`Tapped="OnOpenLogTapped"` + ハンドラ例 |
| 未定義語: 移行 Skill の before/after ラベル / 「起票する」の宛先 | **反映済み** | 全 before/after 対に "Before, in AiForms:" / "After, in KsSettingsView:" のラベル。`api-mapping.md:324` に issues URL |
| 矛盾: maui SKILL「プロジェクト (またはパッケージ)」⇔ 移行 Skill | **反映済み** | 両者とも「NuGet パッケージは無い / ProjectReference のみ」で一致 |
| 矛盾: MAUI `RowHeight="-1"` ⇔ api-mapping | **反映済み** | styling 側が「正値のみ有効・未指定で自動」へ。api-mapping `:277` の「-1 meant automatic; null means automatic」と整合 |
| 矛盾: ButtonCell の `Description` / `HintText` の扱い | **反映済み** | iOS / Android / MAUI の 3 Skill が「`ButtonCell` は `description` 非保持・`HintText` は保持」で一致 |
| 矛盾: iOS 独立 Registry ⇔ `autoRegisterBasicCells: false` | **反映済み** | ios `custom-cells.md:202` が「注入した registry は `autoRegister...` 引数によらず空で始まる」へ。例からも当該引数が消えている |
| 矛盾: Android `disabled(...)` modifier が紹介されていない | **反映済み** | android `styling.md:88` の modifier 一覧に `disabled` を収録し no-op と明記 |
| en 英文品質: 破綻表現 6 件 | **反映済み** | "animation-free position" / "lose their redraw" / "answers the measure" / "stands for" / "known Android focus-loss path" / maui の自己矛盾文はいずれも grep で 0 件 |
| en 英文品質: 英米綴りの混在 | **反映済み** | `colour` / `behaviour` / `customis*` / `honour` などの英綴りは skills/en 全体で 0 件 |
| en 英文品質: android updates:3 の長文 | **反映済み** | 名前衝突の説明が独立段落 + import ブロック 2 本に分解されている |

### second-opinion-code-001.md (採用 5 件)

| # | 相方の指摘 | 状況 |
|---|---|---|
| 1 | 索引のコピー手順 | **反映済み** (common Major 1 と同じ) |
| 2 | 単体コピー後のリンク破れ (索引 + 兄弟) | **反映済み** (common Major 2 / Minor 3 と同じ) |
| 3 | api-mapping の旧 API 型誤りと公開メンバー欠落 | **反映済み** (aiforms M-1 / M-3 / M-4 と同じ。相方が挙げた 5 メンバーもすべて収録) |
| 4 | android の import 前提 | **反映済み** (android Major 2 と同じ) |
| 5 | maui updates.md の TwoWay 一覧に `PickerCell.SelectedItem` が欠落 | **反映済み** — `references/updates.md:7` が「Native から書き戻される 10 件」と「`SelectedIndex` から導出される TwoWay の `SelectedItem`」を分けて説明し、表にも `PickerCell` (derived) 行を追加。相方の推奨した分離の形そのもの |

---

## 新規指摘 (修正起因・低優先度)

### [🟡 Minor] `Theme` / `CellStyle` の全フィールド一覧が iOS だけに入り、Android が置き去りになっている

**該当箇所**: `skills/en/kssettingsview-android/references/styling.md` 全体 / `skills/ja/kssettingsview-android/references/styling.md` 全体 (対比: `skills/{en,ja}/kssettingsview-ios/references/styling.md:29-67` および `:84-100`)

**問題点**: review-001-fresh.md の未定義語項目「iOS/Android `Theme` / `CellStyle` の全フィールド一覧が無い (MAUI にはある)」は iOS 側だけが解消され、Android 側は未対応のまま残っている。Android の styling.md がレシピ中で触れるのは `separatorColor` / `backgroundColor` / `cellAccentColor` / `cellTitleColor` / `rowHeight` / `hasUnevenRows` / `cellIconSize` / `cellIconRadius` / `sectionMargin` / `sectionCornerRadius` / `sectionBorderWidth` / `sectionBorderColor` と `CellStyle` の 5 フィールドで、`cellBackgroundColor` / `selectedColor` / `disabledTextColor` / `header*` / `footer*` / 各 `cellValueText*` / `cellDescription*` / `cellHint*` などは名前が一度も出ない。Kotlin は名前つき引数のため iOS のような**引数順序の必然性**は無く、対応しないという判断自体は成り立つが、判断の記録がどこにも無いため「同じ指摘に対する片側だけの対応」に見える。4 Skill を横並びで比較する索引を持つ成果物としては、この非対称は説明を要する。

**推奨修正**: Android の styling.md にも `Theme` / `CellStyle` のフィールド表 (型と未指定時の既定) を足して 3 Skill を揃えるか、揃えない判断をオーナー検収 (tasks 5.4) の確認事項として明示する。実害は低いため、次サイクルを回してまで直す性質ではない。

### [🔵 Suggestion] 索引の移行 Skill 行が Xamarin.Forms 版を対象として掲げているが、本文は「XF API とは突合していない」と断っている

**該当箇所**: `skills/README.md:12` / `skills/README_ja.md:12` ⇔ `skills/{en,ja}/kssettingsview-aiforms-migration/SKILL.md:14`

**問題点**: 索引は「`AiForms.Maui.SettingsView` (または Xamarin.Forms 版 `AiForms.SettingsView`) からの移行」と 2 つを並列に挙げるが、SKILL.md 本文は「対応表は Xamarin.Forms の API に対して検査していない」と限定している。索引だけを見た XF 利用者は、対応表が自分の API を保証していると読みうる。

**推奨修正**: 索引の 1 行説明を「`AiForms.Maui.SettingsView` からの移行 (Xamarin.Forms 版からも概ね読み替えられる)」程度に弱めるか、現状のままとするならオーナー判断として記録する。

### [🔵 Suggestion] Android styling.md の header 可視性レシピだけ、参照する状態変数の宣言が無い

**該当箇所**: `skills/en/kssettingsview-android/references/styling.md:201-205` / ja 同箇所 (対比: `skills/{en,ja}/kssettingsview-ios/references/styling.md:204-210`)

**問題点**: `isHeaderVisible = showHeaders` の `showHeaders` がスニペット内にも import ブロックにも現れない。iOS の同じレシピは `@State private var showHeaders = true` を含めており、Android 側だけ完動度が落ちている。Major 2 の import 整備で他のスニペットは前提が閉じたため、この 1 件だけが浮いている。

**推奨修正**: `var showHeaders by remember { mutableStateOf(true) }` の 1 行を足す (`remember` / `mutableStateOf` / `getValue` / `setValue` は同ファイルの import ブロックに未収録のため、足す場合は import も併せて確認すること)。

---

## 確認して問題がなかった観点 (再検査分)

- **翻訳ロックステップ**: en/ja 17 ペアすべてでコードブロックが数・順序・言語タグ・byte まで一致し、見出し階層の並びも完全一致。修正が en 片側だけに入った箇所は無い。ja 側の prose も抜き取り 8 箇所 (iOS の EntryCell 例外 / `.disabled` の書き分け / main actor、Android の Gradle 最低版、MAUI の `Format` / `HintText` / `SectionMargin`、移行の `SettingsViewHandler`) で同じ内容が入っていることを確認した
- **frontmatter の標準準拠**: 8 部すべて `name` / `description` / `license` / `metadata.{language,source}` の 4 フィールドのみ。`name` は en/ja 一致かつディレクトリ名と一致、`language` はパスと一致、`source` は 8 部で同一
- **内部リンク**: `skills/` 配下の相対リンクは全件解決。外部 URL は 8 種すべて `github.com/kamusoft/KsSettingsView` 系で、en は `README.md` / `skills/en/...`、ja は `README_ja.md` / `skills/ja/...` と言語対応も正しい
- **生成の内容規約**: `docs/` / `openspec` 参照 0 件、ローカル絶対パス・`file://` 0 件。新設された導入節のコードブロックにもコメントは入っていない
- **manifest の不変条件**: `concepts` キー集合が作業ツリーの concept 集合 (index / log / rules 除外、35 件) と差分 0、SHA-256 全件一致、`targets` 17 ファイル + `excluded` 8 件で網羅漏れ 0 件、`version: 3`。修正サイクルで skills/ のファイル構成 (36 本) が変わっていないことも確認した
- **足場アーティファクトの非改変**: proposal / specs / tasks / deviation に本レビュー時点で不正な書き換えの形跡なし。tasks.md のチェック状態 (5.3 以降が未完) は現在の進行と整合しており、虚偽チェックは無い
- **deviation.md**: 記録済みの乖離 1 件 (second-opinion 引用のサニタイズ) のみで、本サイクルの修正内容とは無関係
- **スコープ**: 修正はすべて `skills/` 配下と索引に閉じており、concepts / decisions / コードへの巻き込み変更は無い (concept ハッシュが manifest と全件一致していることが傍証)

## アクションプラン

1. 新規 Minor 1 (Android の `Theme` / `CellStyle` フィールド表) の対応要否をオーナー検収 (tasks 5.4) の確認事項に載せる。対応する場合も Android styling.md への表 1〜2 枚の追加で閉じる
2. 新規 Suggestion 2 件 (索引の XF 記述 / `showHeaders` の宣言) は採否をオーナー判断で決める
3. 上記に手を入れた場合のみ、完了検査の②③⑥ (節構成・コードブロック byte 一致・内部リンク) を再実行する
4. 保留中の common Minor 6 / Minor 7 / Suggestion (lastUpdatedFiles) は、予定どおりタスク 6.x と 7.1 で処理する。特に Minor 6 は 6.4 の残存 grep の通過条件になるため、着手前に方針 (ADR-0017 の出典行の差し替え / log.md 過去エントリの grep 除外) を確定させること

---

# 追補: review-002 後の 3 変更に対する独立確認

**日付**: 2026-08-26
**追補の判定**: NEEDS_DISCUSSION (本文の APPROVED は取り消さない — 変更 1 / 2 は完了、変更 3 に設計判断を要する食い違いがある)
**対象**: review-002 (APPROVED) 以降に入った 3 件の差分のみ

## 追補サマリー

変更 1 (Android の `Theme` / `CellStyle` 一覧表 + `showHeaders` 宣言) と変更 2 (索引の Xamarin.Forms 表記調整) は、**指摘した内容がそのとおり反映されており追加の指摘はない**。変更 3 (導入節のパッケージ前提への改稿) は、指定された確認項目 4 つ — 旧記述の残存 0 件 / en-ja ロックステップ / maui ⇔ 移行 Skill の `PackageReference` ブロック byte 一致 / iOS の product・package 修飾の整合 — をすべて通過している。

一方、変更 3 で新たに書かれた**配布座標が、本 change が属する package-distribution ロードマップ自身の決定 (cross/ADR-0018・android/ADR-0016) と食い違っている**ことを確認した。deviation.md は「パッケージが存在する前提で書く」ことと「仮の座標は ADR-0002 規範の `jp.kamusoft:ks-settingsview-*`」までを合意しており、「未公開なのに書いている」ことは指摘対象外という指示に従う。しかし ADR-0002 より後に、**その座標の粒度と artifactId を明示的に上書きする android/ADR-0016 と、SwiftPM の配布先そのものを別リポジトリと定める cross/ADR-0018 が起票されている**。これは deviation の合意時に参照されていなかった事実であり、仮座標を ADR に合わせるか現状のままとするかはオーナーの判断を要するため NEEDS_DISCUSSION とする。

---

## 変更 1: Android styling.md への一覧表追加と `showHeaders` 宣言 — **指摘なし**

review-002 の新規 Minor 1 および Suggestion 3 への対応。

| 確認項目 | 結果 |
|---|---|
| `Theme` 表と実装の一致 | **一致**。`android/ks-settingsview-ui/.../Theme.kt` の `data class Theme(...)` の 33 パラメータに対し、表も 33 行 (List 7 / Height 2 / Header 5 / Footer 4 / Cell defaults 11 / Section box 4)。**順序・フィールド名・型 (`Color` / `Color?` / `Boolean` / `Int` / `Double` / `TextStyle?` / `Dp?` / `PaddingValues?`)・既定値 (`Color.White` / `-1` / `-1.0` / `true` / `null`) がすべて宣言と一致**。iOS 版が `UIColor` / `UIFont?` / `CGFloat?` / `NSDirectionalEdgeInsets?` を使うのに対し Compose 型へ正しく読み替えられている |
| `CellStyle` 表と実装の一致 | **一致**。`CellStyle.kt` の 13 パラメータと表 13 行が順序・名前・型まで一致。全件 nullable である旨の注記も宣言どおり |
| iOS 版との書き分け | **適切**。iOS は「Arguments must be passed in this order」(位置引数の必然性) と書くのに対し、Android は「`Theme` is a data class, so pass them as named arguments in any order」と明示。同じ表を置きながら Kotlin と Swift の差を正しく反映しており、review-002 で懸念した「片側だけの対応」の非対称は解消 |
| `showHeaders` の宣言 | **追加済み**。`skills/en/kssettingsview-android/references/styling.md:264` に `var showHeaders by remember { mutableStateOf(true) }`。あわせてリード文の import ブロックへ `remember` / `mutableStateOf` / `getValue` / `setValue` の 4 本が追加されており、by 委譲がリード文の前提だけで解決する (指摘時に付記した確認点が満たされている) |
| en/ja byte 一致 | **一致**。表 2 枚は Markdown 本文なのでコードブロック検査の対象外だが、ja 側にも同じ 33 行 + 13 行が入っており、見出し階層列も en と完全一致。`showHeaders` を含むコードブロックは en/ja で byte 一致 |

## 変更 2: 索引の Xamarin.Forms 表記調整 — **指摘なし**

review-002 の新規 Suggestion 1 への対応。

- `skills/README.md:12` が「off `AiForms.Maui.SettingsView` (the .NET MAUI release)」、`skills/README_ja.md:12` が「`AiForms.Maui.SettingsView` (.NET MAUI 版) からの移行」へ。Xamarin.Forms 版を移行元として並列に掲げる表現が消えた
- 移行 Skill 本文 (`skills/{en,ja}/kssettingsview-aiforms-migration/SKILL.md:14`) の「対応表は Xamarin.Forms の API に対しては検査していない」という限定と、索引の宣言する範囲が一致した。XF 利用者が「自分の API も保証されている」と読む導線は無くなっている
- 一覧表の 5 列構成・3 要素の節構成は維持。en/ja の行構造も対応している

## 変更 3: 導入節のパッケージ前提への改稿

### 指定された 4 つの確認項目 — **すべて通過**

| 確認項目 | 結果 |
|---|---|
| 旧記述の残存 | **0 件**。`ProjectReference` / `includeBuild` / `dependencySubstitution` / `SNAPSHOT` / `composite build` / `Add Local` / "not published" / "no NuGet package" / "no Maven" / 「未公開」/「パッケージは無い」を `skills/` 全体に grep して 1 件もヒットしない。iOS の「A path dependency is identified by its directory name…」という path 依存前提の解説文も、前提が変わったのに合わせて正しく削除されている |
| en/ja ロックステップ | **維持**。17 ペアすべてでコードブロックが byte 一致、見出し階層列も完全一致。内部リンクも全件解決 |
| maui ⇔ 移行 Skill の `PackageReference` ブロック | **4 ファイルで byte 一致**。`skills/{en,ja}/kssettingsview-maui/SKILL.md` と `skills/{en,ja}/kssettingsview-aiforms-migration/SKILL.md` の当該ブロックはいずれも `<ItemGroup>` / `<PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />` / `</ItemGroup>` の 3 行で同一 |
| iOS の product / package 修飾の整合 | **自己整合**。`.package(url: "https://github.com/kamusoft/KsSettingsView", from: "0.1.0")` の SwiftPM identity は URL 末尾の `KsSettingsView` になり、`.product(name: ..., package: "KsSettingsView")` の修飾と一致する。`ios/Package.swift` の `name: "KsSettingsView"` とも一致し、path 依存時代の `package: "ios"` が正しく書き換わっている |

**バージョンの一貫性**: `skills/` 全体で version リテラルは `0.1.0` の 1 種のみ (`Version="0.1.0"` / `from: "0.1.0"` / Gradle 座標の `:0.1.0`)。cross/ADR-0019 (全 platform lockstep の単一 semver) と整合している。

**MAUI の Package ID**: `KsSettingsView.Maui` は maui/ADR-0025 の決定 (利用者が書くのはこの 1 点、binding 2 件は推移依存) と一致。「That one reference is all you add; the binding layer underneath comes in transitively.」という説明も同 ADR のとおりで、ここに食い違いはない。

### [🟠 Major] 追-1: Android の配布座標が android/ADR-0016 の決定と食い違う

**該当箇所**: `skills/en/kssettingsview-android/SKILL.md` の「Take the library into your build」/ `skills/ja/kssettingsview-android/SKILL.md` 同節 (en/ja とも同一コードブロック)

**問題点**: 導入節は次の 3 依存を書いている。

```kotlin
implementation("jp.kamusoft:ks-settingsview-core:0.1.0")
implementation("jp.kamusoft:ks-settingsview-ui:0.1.0")
implementation("jp.kamusoft:ks-settingsview-compose:0.1.0")
```

groupId を `jp.kamusoft` とする点は cross/ADR-0002 および `kasane/concepts/cross/conventions/public-identifiers.md`:28-30 のとおりで、deviation の記述とも合っている。しかし **android/ADR-0016 (`single-module-single-maven-artifact`、status: proposed、2026-08-21) が、この粒度と artifactId の綴りの両方を明示的に置き換えている**。

- 同 ADR の Decision: 「`core` / `ui` / `compose` の 3 module を**単一 Gradle module** に物理統合し、**`jp.kamusoft:kssettingsview`** の 1 artifact として Maven Central に公開する。利用者が書く座標はこの 1 点で、Compose DSL も同じ artifact に含まれる」
- 同 ADR の Decision: 「artifactId はブランド名 `kssettingsview` を 1 トークンとして扱い、**内部にハイフンを入れない**」
- 同 ADR の Alternatives Considered: 「**3 module を 3 artifact (`kssettingsview-core` / `-ui` / `-compose`) として公開**し…: **却下**」

つまり導入節が案内している形は、同 ADR が検討したうえで却下した案に近い構成であり、artifactId の綴り (`ks-settingsview-*`) も命名規則から外れている。cross/ADR-0018 の Context も「Android 側は単一 artifact `jp.kamusoft:kssettingsview` に統合する (android/ADR-0016)」と前提している。

deviation.md が根拠に挙げる ADR-0002 と public-identifiers.md は、いずれも android/ADR-0016 より前の記述であり (public-identifiers.md も ADR-0016 を未反映)、**deviation の合意時にこの ADR が参照されていなかった可能性が高い**。仮座標であっても、公開後に docs-refresh が書き換える先が既に決まっているのなら、いま決定と食い違う形を利用者向けに出す積極的な理由はない。

**選択肢**:
1. 導入節を android/ADR-0016 に合わせ、単一依存 `implementation("jp.kamusoft:kssettingsview:0.1.0")` にする (module 一覧の記述も「3 module」から「1 artifact・パッケージで層を表す」へ調整が必要)
2. 現状のままとし、「仮座標は ADR-0016 実施前の形でよい」ことを deviation に追記して合意記録を更新する
3. android/ADR-0016 側を見直す (本 change の範囲外)

### [🟠 Major] 追-2: iOS の SwiftPM 参照が cross/ADR-0018 の決定と食い違う

**該当箇所**: `skills/en/kssettingsview-ios/SKILL.md` の Setup 節 / `skills/ja/kssettingsview-ios/SKILL.md` 同節 (en/ja とも同一コードブロック)

**問題点**: 導入節は monorepo の URL を SwiftPM 依存として案内し、3 product をリンクさせている。

```swift
.package(url: "https://github.com/kamusoft/KsSettingsView", from: "0.1.0")
...
.product(name: "KsSettingsViewCore", package: "KsSettingsView"),
.product(name: "KsSettingsViewUI", package: "KsSettingsView"),
.product(name: "KsSettingsViewSwiftUI", package: "KsSettingsView")
```

`cross/ADR-0018` (`distribution-public-channels-root-swiftpm-manifest`、status: proposed、2026-08-21) は次を決めている。

- 「SwiftPM 専用の**公開配信リポジトリを別に持ち**、release CI が `ios/Package.swift` と `ios/Sources/` `ios/Tests/` (と LICENSE) のスナップショットを**配信リポジトリのルート**へ commit し、同じ version の semver tag を push する」
- 「**monorepo のルートには Package.swift を置かない**」
- 「product は **`KsSettingsView` 1 本**とし、`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の 3 target を束ねる (umbrella product)。利用者はこの 1 product をリンクし、`import` は使う module 名で書く」

さらに同 ADR の Context は「SwiftPM の git 配布は**リポジトリルート直下の Package.swift しか解決できず** (サブディレクトリ指定は未サポート)、現状の `ios/Package.swift` のままでは SwiftPM 配布が成立しない」と明記している。作業ツリーにもリポジトリルートの `Package.swift` は存在せず (`ios/Package.swift` のみ)、この決定に従う限り**将来も置かれない**。

したがって導入節の記述は、バージョンが仮であるという問題とは別に、次の 2 点で決定と食い違う。

1. **参照先リポジトリ**: monorepo の URL は、決定どおりに進めた場合いつまで経っても SwiftPM から解決できない (配信リポジトリが正しい参照先。名前は未確定)
2. **product 粒度**: 3 product は決定では公開されない。umbrella product 1 本をリンクし、`import` を module 名で書く形になる

なお `package:` 修飾自体は URL と `ios/Package.swift` の `name` の双方に対して自己整合しており、修飾の書き方に誤りがあるわけではない。問題は参照先と product 構成の選び方にある。

**選択肢**:
1. 配信リポジトリの名前が決まるまで iOS の導入節を保留し、他 3 Skill だけパッケージ前提で進める
2. 配信リポジトリ名を仮置きして ADR-0018 の形 (url = 配信リポジトリ、product 1 本) で書く
3. 現状のままとし、「iOS の仮座標は ADR-0018 実施前の形でよい」ことを deviation に追記して合意記録を更新する

### [🔵 Suggestion] 追-3: 配布まわりで「将来の形」と「現在の形」が混在している

**該当箇所**: `skills/{en,ja}/kssettingsview-maui/` および `skills/{en,ja}/kssettingsview-aiforms-migration/` の XAML / C# 例全般

**問題点**: MAUI の Package ID は maui/ADR-0025 の決定を先取りして `KsSettingsView.Maui` と書いている一方、同じ ADR が決めている**名前空間の変更 (`KsSettingsView.Maui` → `KsSettingsView`、XAML は `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui`) は取り込まれておらず**、例は現行実装どおり `clr-namespace:KsSettingsView.Maui;assembly=KsSettingsView.Maui` / `using KsSettingsView.Maui;` のままである。

コード例は現行実装を正とする規約 (生成の内容規約 ④) に照らせば**現状の書き方が正しく、これ自体は誤りではない**。ただし同じ ADR の中で「座標は将来形・名前空間は現在形」と割れているため、追-1 / 追-2 の判断を下すときに「どの決定をどこまで先取りするか」の線引きを 1 つ決めておかないと、次の docs-refresh で同じ判断を繰り返すことになる。

**推奨**: 追-1 / 追-2 の結論に合わせて、「先取りするのは利用者が手で書く配布座標のみ。API の綴り (名前空間・型名・シグネチャ) は常に現行実装を正とする」といった線引きを deviation か tasks.md に 1 行残す。

## 追補のアクションプラン

1. 追-1 / 追-2 について、オーナーが 3 択 (ADR に合わせる / 現状維持で deviation に追記 / ADR 側を見直す) を決める。**iOS (追-2) は配信リポジトリ名が未確定のため、決めるまで書けない部分がある**点に注意
2. 追-1 を「ADR に合わせる」で決めた場合は、Android SKILL.md の依存ブロックに加えて、3 module を列挙している導入リード文も 1 artifact 前提へ調整する (en/ja ロックステップ維持、コードブロック byte 一致の再検査が要る)
3. 追-3 の線引きを 1 行記録する
4. 変更 1 / 2 は追加作業なし

---

# 追補 2: オーナー検収 (5.4) 反映サイクル 3 の独立確認

**日付**: 2026-08-26
**追補 2 の判定**: APPROVED (Critical 0 / Major 0 / Minor 1 / Suggestion 1)
**対象**: 追補 1 以降に入った検収反映 8 項目の差分のみ。既存本文・追補 1 は不変更

## 追補 2 サマリー

検収指摘 8 項目はいずれも**指示どおりに反映されており、実装・概念との突合でも新たな誤りは出ていない**。deviation.md の新規 3 項 (SKILL 外参照の撤去 / 概念説明段落の追加と maui テーマ節の縮退 / `SectionMargin` 実装修正の同梱) は、指示された 4 つの spec 乖離を過不足なく記録しており、これらを spec 違反としては扱わなかった。

同梱された実装修正 (`SectionMargin` の TypeConverter 付与) は**最小・テスト付き・docs と整合**しており、テストは実際に実行して通過を確認した。

残るのは、Skill 側から参照を撤去した結果として索引に取り残された説明文 1 件 (Minor) と、新規英文 1 文の言い回し (Suggestion) のみ。判定は APPROVED を維持する。

### 実施した検証

- 8 項目それぞれについて en/ja 双方の現物を確認。加えて `skills/` 全体の機械検査 (en/ja 17 ペアのコードブロック byte 一致・見出し階層一致、内部リンク全解決、frontmatter 4 フィールドと `metadata.language` のパス一致、ファイル数 36) を自前で再実行し全通過
- `maui/KsSettingsView.Maui.Tests` の当該テストを**実際に実行**して通過を確認 (`SectionMarginAttributeTextMatchesThicknessAssignment`: 合格 1 / 失敗 0)
- `ios/Sources/KsSettingsViewUI/CustomCell.swift` の init 宣言、`kasane/concepts/maui/api/maui-facade.md` のページ離脱・再訪の契約と、新規記述を突合

---

## 項目別の確認結果

### 1. SKILL 外参照の全撤去 — **完了**

| 検査 | 結果 |
|---|---|
| `blob/HEAD` を含む URL | **0 件** (索引リンク 8 件・移行→maui リンク 2 件がすべて消えている) |
| Skill ディレクトリを出る相対リンク (`](../../` / `](/` / `](skills/`) | **0 件** |
| `README.md` / `README_ja.md` / 「Skills 索引」への言及 | **0 件** |
| 許容対象の残存 | `metadata.source` (8 件)、iOS 配布座標 `KsSettingsView-Swift` (en/ja 各 2 箇所)、issues URL (api-mapping.md:324)、XAML xmlns。いずれも指定どおり許容範囲 |
| 移行 Skill → maui Skill | `SKILL.md:28` が能力マップ内で「the kssettingsview-maui Skill」/「kssettingsview-maui Skill」と**スキル名のみの言及 (リンクなし)** へ。deviation の「他 Skill への言及はスキル名のみ」と一致 |

Skill 内に残る相対リンクは `references/*.md` と `../SKILL.md` および references 間の相互リンクのみで、いずれも `SKILL.md` + `references/` という配布単位の内側に閉じている。単体コピー後も解決する。

### 2. 概念説明段落の追加 — **完了**

- 8 部すべての `# 見出し` 直後に 1 段落が入っている。en は「KsSettingsView is a UI library for building settings screens - the list-style screens the iOS Settings app is made of. You declare the screen as a tree of rows (cells) grouped into sections, and that tree is the screen.」+ 当該 Skill が扱う版の説明、ja は「KsSettingsView は、iOS の設定アプリのようなリスト形式の設定画面を組み立てる UI ライブラリ。画面は行 (Cell) を Section にまとめたツリーとして宣言し、そのツリーがそのまま画面になる。」+ 同、という対応で **en/ja は意味等価**
- 4 部で共通部分が同一文になっており、Skill ごとに異なるのは 3 文目 (iOS = SwiftUI DSL と UIKit ホスト / Android = Compose DSL と XML View ホスト / MAUI = XAML・C# のコントロール一式 / 移行 = 乗り換え作業とメンバー単位の対応表) のみ。横並びの一貫性がある
- **アーキテクチャ読み物化していない**: 各段落は 3〜4 文で、層構造・同期経路・Bridge などの内部設計には触れず、「何を作る道具か」と「この Skill の守備範囲」に限定されている。設計原則の「アーキテクチャ解説の読み物を混ぜない」に抵触しない
- 移行 Skill だけは概念説明 → 既存の 2 段落 (骨格の引き継ぎ / 対応表の適用範囲) と続くが、重複ではなく粒度が下りていく構成で問題ない

### 3. facade 表現の撤去 — **完了**

`skills/` 全体を大小文字無視で grep して **0 件**。frontmatter の description も maui が「a public XAML / C# API (SettingsView, Section, CellBase) over the native ...」へ置き換わっており、能力マップ・本文にも残っていない。

### 4. `updates.md` の見出しと maui の離脱・再訪段落 — **完了**

- H1 が 3 platform × 2 言語すべてで `# Updating the screen while it is shown` / `# 表示中の画面の更新` へ。SKILL.md 側の参照文言 (能力マップ・reference 振り分け) も「表示中の画面の更新」「changing a screen that is already on display」で揃っている
- maui の離脱・再訪節 (`## Keep the screen across page visits` / `## ページを離れて戻っても画面を保つ`) は、保持されるもの (Section・Cell・値・Header/Footer View)、離脱中の変更も反映されること、自前の保存・復元が不要なこと、**毎回ツリーを作り直してはいけない (作り直すとユーザーの変更値ごと失われる)** を書く形に改まっている
- **概念との整合**: `kasane/concepts/maui/api/maui-facade.md`:69「ページ離脱 (Handler 切断) で Host は解放されるが、facade・Bridge・Store は生き続け、切断中の変更も Store へ流れ続ける — 再訪問時は Store 現在状態から表示が復元される」と一致。新たに加わった「作り直すな」の指示も、Store が状態の持ち主であることから正しく導かれている

### 5. custom-cells の「行 (row)」表現 — **完了**

- 見出しが 3 platform × 2 言語で `## Put arbitrary SwiftUI into a row of the list` / `## 任意の SwiftUI View を行 (row) として表示する` 系へ (Android・MAUI も同型)
- SKILL.md 能力マップの該当行も「into a row of the list」/「行 (row) として表示する」へ追随
- リード文も「shows an arbitrary MAUI view as one row in the list」「renders any Composable as one row in the list」と `in the list` が付き、「1 行 = テキスト 1 行」と読める余地が消えている
- 他ファイルに残る「1 行」/「one row」(`styling.md` の「1 行だけ見た目を上書きする」、`updates.md` の「1 行の内容を差し替える」) は**数量としての「1 つの行」**であり、検収指摘の対象だった曖昧さとは別の用法。修正不要と判断した

### 6. maui SKILL「Android のホストテーマ」節の縮退 — **完了**

- 節は前提の注記 2 文のみに縮退し、`MainActivity` の書き換えコードブロックと手順は削除された。en/ja とも同じ縮退
- 表現も「the runtime theme applied to the activity hosting the screen has to resolve the Material3 attributes」/「実行時テーマが Material3 系の属性を解決できる必要がある」と、**テンプレート既定を潰す具体的手順を示さずに前提だけを述べる**形になっており、deviation の記述 (前提自体の解消は別 change の責務) と一致する
- Android SKILL.md の前提 2 件 (`Theme.Material3.*` 派生 / `FragmentActivity`) は**不変更**。指示どおり

### 7. `SectionMargin` 実装修正の同梱 — **完了・妥当**

| 観点 | 結果 |
|---|---|
| 修正の最小性 | `maui/KsSettingsView.Maui/SettingsView.cs:851` に `[System.ComponentModel.TypeConverter(typeof(Microsoft.Maui.Converters.ThicknessTypeConverter))]` を CLR プロパティへ 1 行付与しただけ。`SectionMarginProperty` の宣言 (`typeof(Thickness?)`・既定値・`propertyChanged`) は無改変で、**挙動・公開シグネチャ・他プロパティへの波及はない**。XML doc の `<remarks>` に理由 (nullable `Thickness` は XAML の型変換が自動解決されないため型変換器を明示) も追記されており、コメントが単独で読める |
| テストの妥当性 | `maui/KsSettingsView.Maui.Tests/SectionDecorationThemeTests.cs:164-178` の `SectionMarginAttributeTextMatchesThicknessAssignment`。**属性記法の XAML 文字列を `LoadFromXaml` で実際にロードし**、結果が `new Thickness(16, 22, 16, 0)` と等しいことを検証する。トートロジーではなく、TypeConverter を外せば変換が成立せずアサーションが落ちる構造になっている (変換器が無ければ値は設定されないか例外になり、いずれにせよ `Is.EqualTo` が失敗する) |
| テストの実行 | **実際に実行して通過を確認** (合格 1 / 失敗 0 / 189 ms) |
| docs との整合 | `references/styling.md` の「Adjust the section box」/「Section の箱を調整する」が属性記法 `SectionMargin="16,22,16,0"` の 1 ブロックへ戻り、プロパティ要素記法と「型変換が届かない」の説明文が削除されている。en/ja でコードブロック byte 一致。`api-mapping.md` 側の `SectionMargin` 記述 (leading/trailing 解釈・Classic の上下のみ) は元から記法に触れておらず、矛盾は生じていない |

### 8. iOS custom-cells の再利用例補完 — **完了**

- 呼び出し側ブロックが追加された (`SoundSettingsView` が `ksSection` 内で `SliderCell(label:value:onValueChanged:)` を 2 箇所で使い、片方に `.cellHeight(56)` を chain)。「同じ関数を複数の画面・Section で使える」「modifier は chain できる」という文も添えられている
- **粒度の整合**: Android は `cell(...)` / `+cell` の呼び出しブロック、MAUI は `<local:SliderCell Value="..." />` の XAML ブロックを既に持っており、iOS だけ定義側で終わっていた非対称が解消した。3 platform とも「定義 → 呼び出し」で揃っている
- **`@Sendable` 除去の妥当性**: `ios/Sources/KsSettingsViewUI/CustomCell.swift:128` の builder は `@ViewBuilder builder: @escaping (C) -> V` で **`@Sendable` を要求していない** (`CustomCell` 自身が `@unchecked Sendable`)。したがって `onValueChanged` から `@Sendable` を外しても builder のキャプチャに制約は生じず、除去は正しい。`@Sendable` が付くのは `onTap` のみで、この例は `onTap` を使っていない

---

## 追補 2 の新規指摘

### [🟡 Minor] 追2-1: 索引が、撤去済みの「リポジトリへの公開 URL リンク」を前提にした説明を残している

**該当箇所**: `skills/README.md:43` / `skills/README_ja.md:43`

**問題点**: コピー手順の締めくくりに次の 1 文が残っている。

- en: 「Links from the documents back into this repository are public GitHub URLs, so they keep resolving from your copy.」
- ja: 「文書から本リポジトリへ張られたリンクは GitHub 上の公開 URL なので、コピー先でも解決する。」

この文は、追補 1 の時点で採られていた「SKILL 外参照を公開 URL へ絶対化する」設計を説明したものである。項目 1 の撤去によって **Skill 内にリポジトリへ張られたリンクは 1 本も残っていない**ため、存在しない仕組みを説明していることになる。実害 (壊れた案内) はないが、索引は利用者が最初に読む面であり、deviation が掲げる「配布単体利用の閉世界性」とも噛み合っていない。

**推奨修正**: 撤去後の実態に合わせて言い換える。例: 「コピーしたディレクトリだけで完結している (`SKILL.md` と `references/` の外へ張られたリンクは無い)」/ "A copied directory is self-contained: nothing in it links outside `SKILL.md` and its `references/`."。現状より強い保証を述べる形になり、3 要素の構成も崩さない。en/ja 同時に直すこと。

### [🔵 Suggestion] 追2-2: 新規英文 1 文の語順が不自然

**該当箇所**: `skills/en/kssettingsview-maui/references/updates.md:162`

**問題点**: 「Do not rebuild the tree on every visit, therefore.」— 文末に置いた `therefore` が不自然で、初見レビュー (review-001-fresh.md) が「en 版固有 (英文品質)」として挙げた破綻表現と同じ型になっている。ja 側 (「したがって、再訪のたびにツリーを作り直してはいけない。」) は自然。

**推奨修正**: 「So do not rebuild the tree on every visit.」など、接続詞を文頭へ移す。1 文の言い換えで済み、コードブロックに触れないためロックステップ検査への影響もない。

## 追補 2 のアクションプラン

1. 追2-1 を直す (索引 2 枚、en/ja 同時)。3 要素の構成と節数は維持されるため、機械検査は②節構成一致の再通過だけで足りる
2. 追2-2 の採否を判断する (任意)
3. 項目 1〜8 については追加作業なし。tasks.md 5.3 / 5.4 の消し込みへ進んでよい

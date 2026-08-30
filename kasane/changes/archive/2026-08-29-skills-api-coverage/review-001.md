# レビュー結果: skills-api-coverage (001 回目)

**日付**: 2026-08-29
**判定**: CHANGES_REQUESTED

## サマリー

concepts 3 ファイルへの追記は実装ソースと**完全一致**しており (iOS `Theme` default 18/18・Android `DEFAULT_*` 14/14・MAUI `SettingsView` スタイル 42/42 + `CellBase` 20/20 + `EntryCell.TextAlignment`)、exploration の掲載除外リスト (`disconnectStore` / `preview` / Registry 補助 / `EffectiveStyle` / Bridge 層 / `unbind` / `SettingsRootDsl` / `SectionScope` / `withDSLIcon` / `MauiAppBuilderExtensions` / `CustomCellEmptyContent` / `FooProperty` 個別列挙) の混入も無い。en/ja の見出し構成・コードブロック byte 一致、manifest のハッシュ・網羅性、標準 lint はいずれも通過した。

一方で、本 change が **自ら新設した内容規約⑧ (全称表現は実装で全数確認できた場合のみ)** に、同じ change の成果物が違反している箇所が 1 件ある。iOS skills の「Store 公開操作はこれがすべて」という宣言が実装と食い違い、しかも同じ Skill 内の別ファイルの記述と矛盾している。これは利用者が読む公開ドキュメントの事実誤りなので Major とする。

## 指摘事項

### [🟠 Major] iOS Store 公開操作表の全称宣言が実装と食い違う (新規約⑧ 違反)

**該当箇所**: `skills/ja/kssettingsview-ios/references/updates.md:41` / `skills/en/kssettingsview-ios/references/updates.md:41`

**問題点**:
新設した表の導入文が「`SettingsRootStore` の公開操作は**以下がすべてである**」("These are the public operations of `SettingsRootStore`.") と全称で宣言しているが、実装には表に無い public 操作 `invalidateAccessoryMeasurement(target:)` がある (`ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:322`)。

さらに悪いことに、この操作は**同じ Skill の別ファイルで既に利用者向けに紹介されている** — `skills/ja/kssettingsview-ios/references/styling.md:224` (en 版も同行) が `store.invalidateAccessoryMeasurement(target:)` の使い方を書いている。利用者は「すべて」と書かれた表に載っていない API を隣のファイルで使えと言われる形になり、表の信頼性が崩れる。

本 change は `.agents/skills/docs-refresh/SKILL.md` の内容規約⑧ として「全称表現 (「すべて」「常に」「必ず」、"all" / "always" / "every") で API の挙動を断定しない。実装で全数確認できた場合のみ許可し、例外が1つでもあるなら例外側を明記する」を新設している。その規約の初回適用対象である本成果物が違反しているため、規約の実効性の観点でも直すべき。

なお `kasane/concepts/ios/api/ios-native-host.md:32-38` の同種の表も `invalidateAccessoryMeasurement` を含まないが、concepts 側は「表示後の変更は次の公開操作を使う」であって完全性を主張していないため矛盾していない。exploration の決定事項 (concepts 追記は Theme default 定数のみ) を尊重し、concepts の書き換えは求めない。

**推奨修正**: 次のいずれか。
- (推奨) 全称表現を外す — 例:「表示後の変更に使う `SettingsRootStore` の操作は次のとおり」/ "The store operations used to change the screen after display are:"。en/ja 同時に直す
- または表に `invalidateAccessoryMeasurement(target:)` の行を足す (Header / Footer 行に併記。既に styling.md で紹介済みの API なので新規掲載判断は不要)

### [🟡 Minor] Android DSL scope 説明で `cell` の所有 scope が誤っている

**該当箇所**: `skills/ja/kssettingsview-android/references/updates.md:63` / `skills/en/kssettingsview-android/references/updates.md:63`

**問題点**:
「builder の receiver は `SettingsRootScope` で、**その** `section` / `cell` は返り値を持たない」("The receiver of the builder is `SettingsRootScope`, and **its** `section` / `cell` return nothing") と書かれているが、`SettingsRootScope` が持つのは `section` だけで、`cell` は `SectionScope` のメンバである (`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootScope.kt:29` と `:127`)。

この一文は exploration の「`SettingsRootScope.section` / `SectionScope.cell` は `Unit` を返す」という隣接課題修正そのものなので、修正の中で所有関係を取り違えたのは惜しい。なお `SectionScope` 型名は decision 3 の掲載除外に入っているため、型名を出さずに直す必要がある。

**推奨修正**: 所有を主張しない表現にする。例:「builder 側の scope の `section` / `cell` は返り値を持たない」/ "In the builder's scopes, `section` and `cell` return nothing."

### [🟡 Minor] docs-refresh のモード分岐記述が 3e に追従していない

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:57` / `:763`

**問題点**:
3e 本文 (`:194`) は「`--readme-only` ではスキップする。`--all` でも実行し…」と自分の分岐を書いているが、モードの正面の説明である `--all` のオプション定義 (`:57` 「網羅検査 (3c) はスキップしない」) と Guardrails (`:763` 「`--all` でスキップするのはハッシュ差分検出 (3a・3b) だけで、網羅検査 (3c) は実行する」) はどちらも 3c しか挙げていない。`--readme-only` 側 (`:125`) は 3e まで更新されているので、`--all` 側だけが取り残された非対称になっている。

矛盾ではないが、実行者が `--all` の節だけ読むと 3e を回さない読み方ができてしまう。本 change の目的の一つが「モード分岐整合」であることを踏まえると閉じておきたい。

**推奨修正**: `:57` と `:763` の「網羅検査 (3c)」を「網羅検査 (3c・3e)」に揃える。

### [🔵 Suggestion] 3e の検出ノイズが Step 4 の提示に耐えない量になる

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:189-241`

**問題点**:
現行の skills/ に対して 3e のスクリプトを実際に実行したところ、`kssettingsview-maui` だけで 20 近い concept ファイルから数百トークンが列挙された (`core/cells/*` や `core/styling/*` の他プラットフォーム実装名・内部型が大半)。SKILL.md はこの誤検出源を①②として明記し「候補外として畳んで提示してよい」と逃がしているが、実運用でこれを毎回人手 (エージェント) で畳むのは Step 4 の提示コストとして重い。

逆に、本 change で埋めた iOS の残差は `ios/api/ios-native-host.md` 由来 2 件まで落ちており、**platform 固有 concepts (`ios/` `android/` `maui/`) を源泉とする分の信号は十分に鋭い**。ノイズは `core/` `cross/` 由来にほぼ集中している。

**推奨修正** (今回必須ではない): 出力を「platform 固有 concepts 由来」と「core/cross 由来 (参考)」の 2 段に分けて印字する、または後者は件数のみ出して詳細は要求時に展開する。報告のみという位置づけは変えずに実用性だけ上げられる。

### [🔵 Suggestion] 3e は ja 側しか検査しないため、en 側だけの取りこぼしを検出できない

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:191`, `:226-227`

**問題点**:
「ja 版で代表 — コード・API 名は en/ja で一致するため片側で足りる」としているが、その一致を保証しているのは Step 6-② (見出し階層) と 6-③ (コードブロック byte 一致) だけで、**地の文に書かれた API 名は en/ja のどちらの検査にも掛からない**。今回の追記は本文の散文に API 名を足す形が多いため (例: iOS cells.md の `textAlignment` / `onTextChanged` の説明文)、en 側だけ落ちても誰も気づかない経路が残る。

**推奨修正** (今回必須ではない): `hay` に `skills/en/{rel}` も連結し、en/ja それぞれで判定して片側だけ欠けたら「片言語のみ掲載」として報告する。数行の変更で済む。

### [🔵 Suggestion] MAUI updates.md の件数修正が段取りと異なる解き方をしている (記録が無い)

**該当箇所**: `skills/ja/kssettingsview-maui/references/updates.md:7` / en 同行、`kasane/changes/skills-api-coverage/exploration.md:46`

**問題点**:
exploration の段取り 2 は隣接修正として「MAUI updates.md の件数「10個」→12」を挙げているが、実装は数値を 12 に直すのではなく「下の表のとおり」と数値そのものを削除している。これは本 change が同時に新設した内容規約⑦ (個数を地の文にハードコードしない) に沿った、より良い解き方だと判断できる — 指摘としては「直すべき」ではない。

ただし合意済み段取りからの意図的な逸脱であり、`deviation.md` にも記録が無いため、蒸留時に「12 に直っていない」と誤読される余地がある。

**推奨修正**: `kasane/changes/skills-api-coverage/deviation.md` に `[方針変更] 件数「10個」は 12 への訂正ではなく削除で解消 (新設した内容規約⑦に従った)` の 1 行を残す。

## 確認した観点 (問題なし)

- **concepts 追記の実装一致**: `Theme.swift` の `public static let default*` 18 件 = iOS 表 18 行 / `Theme.kt` companion の `DEFAULT_*` 14 件 = Android 表 14 行 / `SettingsView.cs` のスタイル系 public プロパティ 42 件 (Section 装飾 4 含む) = MAUI `SettingsView` 表 42 件 / `CellBase.cs` の上書き系 20 件 = `CellBase` 表 20 件 / `EntryCell.TextAlignment` (`TextAlignment?`) 実在。過不足なし
- **exploration が「iOS 17」「Android 13」としていた個数**は実装では 18 / 14 で、成果物は**実装側に合わせている**。正しい選択 (concepts の正は実装)
- **掲載除外 API の非混入**: `disconnectStore` / `SettingsRootStore.preview` / `SettingsRootBuilder` / `KsSectionBuilder` / `EffectiveStyle` / Bridge 層 / `KsSettingsView.unbind` / `SettingsRootDsl` / `SectionScope` / `withDSLIcon` / `MauiAppBuilderExtensions` / `CustomCellEmptyContent` はいずれも skills/ に登場しない。`FooProperty` も個別列挙せず規約 1 行 (`skills/*/kssettingsview-maui/references/styling.md`) に留めており、その「いずれも `FooProperty` を持つ」という主張は `CellBase.cs` 26/26・`SettingsView.cs` 51/51 で全数確認できるので規約⑧ に適合
- **en/ja 規律**: 24 ファイルすべて言語ペアで同時更新。6-② 見出し階層一致・6-③ コードブロック byte 一致とも全ペア OK。frontmatter に変更なし。各 SKILL.md の導入手順 (仮置きのパッケージ名) にも手が入っていない
- **manifest**: 更新 3 concepts のハッシュが現行ファイルの sha256 と一致、`targets` / `excluded` の網羅に `UNCOVERED` / `DELETED` なし、`lastUpdatedFiles` が実際の更新 24 ファイルと一致
- **実装との個別照合 (抜粋)**: iOS `EntryCell.textAlignment: CellTitleAlignment = .end` / `PickerCell.onMultiSelectionChanged` / `selectionMode` / `pageTitle` / `pickerTitle` (Number/Time/Date) / `KsAnyView.swiftUI` `.uiKit` / `makeController()` の DSL 時 `fatalError` / `KsSettingsViewController.applyDiff(_:)` `applyTheme(_:)` / `SettingsRootDiff` 10 case — すべて実装どおり。Android `SettingsRootDiff` 10 case・`store.state` / `theme` (StateFlow)・View の `applyDiff` / `invalidateAccessoryMeasurement`・`androidButtonColor` の `Spinner` 限定 — すべて実装および `kasane/concepts/core/cells/date-picker-selection-surface.md:90` どおり。MAUI `Section.HeaderView` / `FooterView` (4 スロット)・`PickerSelectionMode` / `DatePickerUIStyle?` / `SettingsViewStyle` の型名 — 実装どおり
- **別セッション対応の確認事項**: `skills/*/kssettingsview-aiforms-migration/references/api-mapping.md` の `PickerCell.SelectedCommand` は「同名で復元された」に修正済み。本 change 側で追加対応は不要
- **lint**: `scripts/local-path-lint.py` / `scripts/identity-lint.py` とも指摘なし
- **起票物**: `kasane/changes/ios-effectivestyle-visibility/exploration.md` と `kasane/lessons/inbox/api-enumeration-written-as-inline-list.md` は形式・内容とも妥当。lessons の記述 (「表にする」) は今回の成果物自身がすべて表形式で書かれている点と整合

## アクションプラン

1. (必須) iOS updates.md:41 の全称表現を修正、または `invalidateAccessoryMeasurement(target:)` を表に追加する — en/ja 同時
2. (必須) Android updates.md:63 の `section` / `cell` の所有 scope 表現を修正する — en/ja 同時
3. (必須) docs-refresh SKILL.md `:57` `:763` の「(3c)」を「(3c・3e)」に揃える
4. (任意) deviation.md に MAUI 件数の解き方変更を 1 行記録する
5. (任意・別 change でも可) 3e の出力段分けと en 側検査 — Suggestion 2 件

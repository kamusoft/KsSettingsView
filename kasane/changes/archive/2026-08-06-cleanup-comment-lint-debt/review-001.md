# レビュー結果: cleanup-comment-lint-debt (001 回目)

**日付**: 2026-08-06
**判定**: CHANGES_REQUESTED

## サマリー

この変更の**絶対制約である「機能コードを1文字も変えない」は機械的に証明できた** — 言語別コメント除去後のコード比較で 221 ファイル全件が変更前後で完全一致し、Android 1986 件 / iOS 624 件のテストも全て成功する。lint 禁止件数 0 も、lint スクリプト自体が未変更かつ `--selftest` 全件 OK であることまで確認したので信頼できる。ADR 参照 10 種は全て実在し、index.md のタイトルと主旨が一致しており、こじつけの参照は 1 件も無い。書き換えの全体品質は総じて高い。

一方で、**参照句を落として現在形の説明に書き直す過程で、コードと食い違う記述が 7 箇所混入した**。うち 3 箇所は依存関係の向き・aux 列の構成・存在しない API という、読んだ人が誤った前提で動きかねない誤りである。この change の目的がコメントの信頼性回復である以上、これらは通せない。修正は全てコメント 1〜3 行の書き換えで済む。

## 指摘事項

### [🟠 Major] 依存の向きが逆転し、成立しない循環依存の説明になっている

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLNodes.swift:19-21`

**問題点**:
```swift
// Core 配置の理由は、具象 Cell
// （`LabelCell` / `SwitchCell` 等）が `KsSettingsViewUI` 配置であるため、UI 層に置くと
// `KsSettingsViewUI → KsSettingsViewSwiftUI` の循環依存が生じるからである。
```
`ios/Package.swift` の `KsSettingsViewSwiftUI` ターゲットは `dependencies: ["KsSettingsViewUI", "KsSettingsViewCore"]` であり、**SwiftUI → UI の依存は既に存在する**。したがって `DSLReidentifiable` を UI 層に置いても `KsSettingsViewUI → KsSettingsViewSwiftUI` という向きの依存は発生せず、循環にならない (むしろ SwiftUI 側から素直に参照できる)。

書き換え前の原文は「具象 Cell が `KsSettingsViewUI` 配置になる際、`KsSettingsViewUI → KsSettingsViewSwiftUI` の循環依存を**回避する**ため」で、暗黙の対立候補は「SwiftUI 層に置く」ケースだった。書き換えで対立候補を「UI 層に置くと」と明示した結果、依存の矢印はそのままに前提だけが入れ替わり、技術的に成立しない主張になっている。レイヤリングの根拠を語る箇所なので、将来のリファクタを誤らせる。

**推奨修正**: 「SwiftUI 層に置くと `KsSettingsViewUI → KsSettingsViewSwiftUI` の依存が必要になり循環する」の意に直す。

---

### [🟠 Major] aux 列の構成から `icon` が脱落し、存在しないフィールドを根拠にしている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCellViewHolder.kt:19`

**問題点**:
```kotlin
 * [ButtonCell] は `description` を持たないため、aux 列に載るのは `valueText` / `hintText` のみである。
```
2 点で誤っている。

1. 同ファイル L75 の実装は `val hasAux = cell.icon != null || cell.valueText != null || cell.hintText != null` であり、aux は **`icon` を含む 3 点**。新文は `icon` を落としている。直前の L13-17 の KDoc 自身が「`icon` / `valueText` / `hintText` がすべて `null` のとき」と正しく 3 点を挙げているため、同一ブロック内で矛盾している
2. `ButtonCell` には `description` フィールドがそもそも存在しない (`ButtonCell.kt` は `valueText` / `icon` / `hintText` のみ)。「`description` を持たないため aux に載るのが〜」という因果関係が成立しない。`description` は aux の構成要素ではなく `descriptionView` (contentRow の外) に描かれる別枠

**推奨修正**: 削除するか、「`icon` / `valueText` / `hintText` のいずれかが指定されたとき通常レイアウトへ切り替える」の趣旨に直す。L13-17 と重複するなら削除でよい。

---

### [🟠 Major] 存在しない API `SettingsRoot.header` / `footer` を現在形で断言している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapterTest.kt:15`

**問題点**:
```kotlin
 * `SettingsRoot.header` / `footer` の有無に応じて項目数と変更通知が正しく出ることを保証する。
```
`android/ks-settingsview-core/.../SettingsRoot.kt` は `val sections: List<Section>` のみを持ち、`header` / `footer` プロパティは存在しない。当該テストも `SettingsRoot` を一切構築せず、`RootHeaderFooterAdapter.view` (型は `RootAccessory?`) を直接代入して itemCount / notify を検証している。実運用でこの値を供給するのは `KsSettingsView.rootHeader` / `rootFooter`。

書き換え前は `"Root H/F（SettingsRoot.header / footer）の描画" Requirement` という**引用された旧 spec 見出し**だったが、引用符が外れたことで、存在しない API の説明文に変質した。Root H/F を Core モデルから UI 層プロパティへ移したのは `core/ADR-0005` の決定そのものなので、逆方向の記述が残るのは害が大きい。

**推奨修正**: 実際の駆動点である `RootHeaderFooterAdapter.view` を主語にする。

---

### [🟡 Minor] `description` が共通フィールド列挙から脱落し、兄弟 Cell と不整合

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCell.kt:16`、同 `CheckboxCell.kt:14`

**問題点**: 両者とも「共通フィールドとして `valueText` / `icon` / `hintText` を持つ」と書かれているが、`SwitchCell.kt:23` / `CheckboxCell.kt:21` に `val description: String? = null` が存在する。書き換え前は「この変更で valueText / icon / hintText を**追加した**」という履歴記述だったため 3 点列挙で正しかったが、現在形の「〜を持つ」に変換した結果、列挙が網羅的に読めてしまう。兄弟の `RadioCell.kt:14` は「`description` / `valueText` / `icon` / `hintText` / `accentColor` を持つ」と正しく列挙しており、同種記述の書き換え方針が揃っていない。

**推奨修正**: `description` を列挙に加える (兄弟ファイルの書き方に揃える)。

---

### [🟡 Minor] テストクラスの検証範囲を Store overload に限定しているが DSL overload のテストも含む

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt:35-36`

**問題点**: 「検証対象は `store` 引数を受ける Store 方式の overload であり」と限定しているが、同ファイルには DSL overload のみを対象とするテストが 3 件ある (L193 / L252 / L304。いずれも `store =` を渡さず `KsSettingsView(style =, theme =) { Section { … } }` を呼ぶ)。クラス KDoc の唯一のスコープ記述がこれなので、読者に誤った範囲を伝える。同モジュールの `build.gradle.kts` の書き換えでは「2 overload を持つ」と正しく書けており、記述間でも整合が崩れている。

**推奨修正**: 「Store 方式・DSL 方式の両 overload を検証する」に直す。

---

### [🟡 Minor] 部分列挙が網羅リストとして読める

**該当箇所**: `ios/Sources/KsSettingsViewUI/Theme.swift:9-13`

**問題点**: 書き換え前は「この変更で何を追加/リネームしたか」のデルタ列挙だったため部分列挙で自然だったが、同じ導入句「このファイルでは以下を提供する：」の下に**現在形の内容一覧**として 4 項目を並べた結果、網羅リストに見える。実際の `Theme` は public フィールド 29 個を持ち、`separatorColor` / `cellBackgroundColor` / `selectedColor` / `cellAccentColor` / `disabledTextColor` / `scrollIndicatorVisible` / `rowHeight` / `hasUnevenRows` / `headerTextColor` / `headerBackgroundColor` / `headerFontSize` / `footerTextColor` / `footerBackgroundColor` / `footerFontSize` が漏れている。

**推奨修正**: 「主なフィールド」等と限定するか、導入句を変える。

---

### [🟡 Minor] 「これらの名前のみを公開し」が過剰断定

**該当箇所**: `ios/Tests/KsSettingsViewUITests/ThemeRenameTests.swift:7`

**問題点**: 「`Theme` はこれらの名前のみを公開し、`viewBackgroundColor` / `titleColor` / `titleFont` は持たない」— 後半は正しいが、前半は字義通りには誤り (`Theme` は 29 の public フィールドを持つ)。同ファイル L62 の `test_backgroundColor_と_cellBackgroundColor_は独立()` が `cellBackgroundColor` を使っていることと直接矛盾する。Android 側の同種ファイル `ThemeRenameTest.kt` は「[Theme] にこれらの名前のプロパティが存在しなければ本ターゲットのコンパイル自体が失敗するため…」と過不足なく書けており、iOS 側だけが過剰断定になっている。

**推奨修正**: Android 側の書き方に揃える。

---

### [🔵 Suggestion] 既存の陳腐化を `+` 行に引き継いでいる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DSLIconModifiable.kt:8-9`

**問題点**: 「UI 層で `icon` を持つ Cell（`LabelCell` / `CommandCell`）が準拠する」とあるが、実際に `DSLIconModifiableCell` を実装しているのは 12 種で、非準拠は `CustomCell` のみ。これは diff で触っていない同ファイル L15-21 の既存陳腐化と同一内容であり、書き換えがそれをそのまま引き継いだ形。**合意済みスコープ (deviation.md「ファイル全体の総点検はしない」) の外**だが、`+` 行が誤った事実を述べているため報告する。

**推奨修正**: この change で直すか、別債務として切り出すかはオーナー判断。

---

### [🔵 Suggestion] lint 非検出の規約違反が 33 件 / 22 ファイル残る (合意済みスコープ内)

**該当箇所**: 代表例 — `android/.../ui/Theme.kt:132`、`android/.../ui/SettingsRootStore.kt:266`、`android/.../compose/SettingsRootScope.kt:115`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1499`

**問題点**: 裸の change-id 参照と履歴記述が組み合わさった規約違反 (例: 「旧名 `DEFAULT_VIEW_BACKGROUND_COLOR` から `port-theme-and-cellstyle-missing-fields` change でリネームされた」) が 33 件残る。うち 19 ファイルはこの diff が触れているが、違反行はいずれも書き換えたブロックから離れた位置にあり、deviation.md の「書き換え対象ブロックの周辺に限る。ファイル全体の総点検はしない」に**合致するため違反としては指摘しない**。

ただし記録に残す価値のある副作用がある: **lint が全体 0 になったことで、この 33 件を指す機械的な信号が今後一切出なくなる**。lint は規約の近似にすぎないという前提が、0 達成後は「lint が黙っている = 規約準拠」と読み替えられやすい。

なお `samples/android/app/src/main/res/drawable/ic_notifications.xml:7` は、同一ディレクトリの兄弟 2 ファイル (`ic_account_circle.xml` / `ic_storage.xml`) が同じ位置の記述を清書済みなのに 1 つだけ `refine-cell-layout-after-unify-review でサンプル用に同梱。` が残り、見た目に不揃いになっている。

**推奨修正**: 残り 33 件の返済を別 change として起票する (CI ゲート新設と同じ扱い)。`ic_notifications.xml` の 1 行だけは兄弟との不揃いが目立つのでこの change に含めてもよい。

## 自分で再実行した検証

| 項目 | 結果 |
|---|---|
| `python3 scripts/comment-policy-lint.py --summary` | **禁止 0 件** (検査対象 401 ファイル) |
| `python3 scripts/comment-policy-lint.py --selftest` | **全件 OK** (lint が無音故障していないことを確認) |
| lint / hook スクリプト自体の変更 | **なし** (0 件は検査を緩めた結果ではない) |
| `comment-policy:allow` の追加 | **0 件** (除外マーカーで件数を落としていない) |
| Android `./gradlew test --rerun-tasks` | **1986 tests / 0 failures / 0 errors** (初回は UP-TO-DATE で未実行だったため強制再実行) |
| iOS `xcodebuild test` (iPhone 17 Pro) | **624 tests / TEST SUCCEEDED** |
| **機能コード差分** | **0** — 言語別コメント除去 (Swift/Kotlin/kts の文字列・raw string・ネスト block comment を状態機械で処理、XML は `<!-- -->`) 後に変更前後を正規化比較。**221 ファイル全件一致、未検査拡張子 0** |
| 追加行の禁止パターン | 通番 (Phase/Round/Decision/論点/Major-N 等) **0**、アーカイブ文書パス **0**、MUST/SHOULD 等 **0**、履歴記述 **0**、裸 change-id **0** |
| ADR 参照の実在性 | 10 種すべて実在 (core/0005,0006,0008,0009,0010,0011,0013・cross/0016・android/0002,0005) |
| ADR 参照の主旨一致 | 全件一致。最多の `core/ADR-0009` (61 箇所) は全て「スタイルは UI 層 + Native 型」の文脈で、ADR タイトルと完全に対応 |
| 事実訂正の妥当性 | `Theme`/`CellStyle` が UI 層にあること、`KsAccessoryReusableView` が不在なこと、`KsListCellBase.preferredLayoutAttributesFitting` が存在すること、`ModernSectionDecoration` が存在すること、ButtonCell titleColor の 4 段解決順序を実コードで確認 — **いずれも訂正内容が正しい** |
| doc comment の破壊 | 新規発生 **0** (検出された孤立 doc 1 件は diff 範囲外の既存箇所) |
| 空コメント・文の破綻 | **0** |
| 足場アーティファクト | 実装中の書き換えなし (exploration.md の変更は探索フェーズでの本文拡充) |
| git 操作 | コミット・ステージいずれもなし (制約遵守) |

## カバー範囲

**全件確認したもの**: 222 ファイルの機械的検証 (機能コード差分・禁止パターン・ADR 実在性・空コメント・doc comment 構造) は全数。

**人手で精読したもの**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の diff 全 441 行 (最大ファイル) はレビュアー自身が全読。それ以外は 3 領域 (Android 本体 / iOS 本体 / テスト・サンプル・ビルドスクリプト) に分けて並列に精読し、**報告された指摘は全てレビュアー自身が実コードを読んで再確認**した (裏取りできなかった指摘は採用していない)。

**カバーしなかったもの**: deviation.md で対象外と合意済みの 2 件 (assertion メッセージ文字列内の議論通番 / advisory 誤検知 2 件) は判定対象外とした。advisory 2 件は誤検知判定が妥当であることのみ確認済み。実機での視覚確認は本 change の性質 (コメントのみ) から不要と判断。

## アクションプラン

1. **Major 3 件を修正** — `DSLNodes.swift:19-21` の依存方向、`ButtonCellViewHolder.kt:19` の aux 構成、`RootHeaderFooterAdapterTest.kt:15` の存在しない API。いずれもコメント数行の書き換え
2. **Minor 4 件を修正** — `SwitchCell.kt:16` / `CheckboxCell.kt:14` の `description` 追記、`KsSettingsViewComposeTest.kt:35` のスコープ、`Theme.swift:9` の列挙限定、`ThemeRenameTests.swift:7` の断定緩和
3. **修正後に lint + 両プラットフォームのテストを再実行**し、機能コード差分 0 を再確認する
4. **Suggestion 2 件はオーナー判断** — `DSLIconModifiable.kt` の既存陳腐化をこの change に含めるか、残 33 件を別 change として起票するか

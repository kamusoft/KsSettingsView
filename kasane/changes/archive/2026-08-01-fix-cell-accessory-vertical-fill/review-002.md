# レビュー結果: fix-cell-accessory-vertical-fill (002 回目)

**日付**: 2026-08-01
**判定**: APPROVED

## サマリー

修正サイクル 1 周目で「修正サイクルへ回す」と確定した 5 件 (#1 Major / #2 Minor / #4 Minor / #5 Minor 部分採用 / #7 Suggestion) は、いずれも実質的に解消されている。新規コメントからは変更提案パス・裸の Scenario 参照・`MUST NOT` が除去され、ADR 参照は規約の許容形 `ios/ADR-0001` に揃った。Picker 系 4 種と EntryCell の renderer 単位配線テストが 5 件追加され、幾何テストの nil 分岐は spec 文言どおり `stackV.maxX == contentView.bounds.maxX - stackH.layoutMargins.right` を許容差 0.5pt で検証する形に強化された。テストは 337 件全 pass (1 周目 332 → 新規 5 件)。

残るのは、触れたコメントブロック内に `openspec/...` 以外の規約違反 (`MUST NOT` キーワード・変更提案 ID の裸参照・履歴記述) が残っている点と、`CellBaseLayout.swift` 冒頭の階層列挙が `accessoryHolder` を含んでいない点。いずれも挙動に影響せず、実装が新たに持ち込んだ瑕疵でもないため blocking とはしない。

指摘件数: **Critical 0 / Major 0 / Minor 2 / Suggestion 0**

## 前サイクル指摘の解消確認

| # | 指摘 | 状態 | 根拠 |
|---|---|---|---|
| 1 | 新規追加コメントのソースコメント規約違反 (Major) | **解消** | `kasane/changes/...` `openspec/...` パス参照は変更ファイル内に 0 件。ADR 参照は `CellBaseLayout.swift:11` / `KsListCellBase.swift:88` とも `ios/ADR-0001` 形。テストの `/// Scenario: <名前>` は自己完結した日本語説明に置換 (`UnifyCellCommonFieldsTests.swift:540-542` `:584-585` 等)。`// 旧経路の MUST NOT を維持` は `// contentConfiguration / accessories 経路を使わない状態が保たれることを確認する` (`:606`) へ書き換え済み |
| 2 | 触れたコメントブロックに残る `openspec/...` パス参照 (Minor) | **実質解消** | 対象 10 ブロックすべてから `openspec/changes/...` 行が消え、説明文だけが残っている。ただし同一ブロックの別カテゴリの違反が残る (下記 Minor 1) |
| 4 | Picker 系・EntryCell の renderer 単位配線テストがない (Minor) | **解消** | `InputCellsTests.swift:237-271` に共通ヘルパを置き、Picker / NumberPicker / TimePicker / DatePicker の 4 テスト (`:273-302`) と EntryCell の 1 テスト (`:305-318`) を追加。value label は `contentStack` に 1 個、chevron は `accessoryHolder` に 1 個、chevron が `contentStack` に無いことまで検証している |
| 5 | 幾何テストの nil 分岐が緩い (Minor 部分採用) | **解消** | `UnifyCellCommonFieldsTests.swift:745-751` で `stackV.maxX` と `contentView.bounds.maxX - stackH.layoutMargins.right` を許容差 0.5pt で比較。spec の「nil の場合は同条件で `stackV` が trailing margin まで広がる」を直接検証する形になった。降格された 2 点 (toggle 自身の中心 Y / holder 幅の `intrinsicContentSize` 比較) は未対応のままで問題ない |
| 7 | valueLabel 按分コメントが実態とずれた (Suggestion) | **解消** | `CellBaseLayout.swift:136-137` が「trailingViews が無ければ contentStack の右端 (= Cell 級アクセサリ列の手前) まで」へ更新済み |

## 指摘事項

### [🟡 Minor] 触れたコメントブロックに `openspec/...` 以外の規約違反が残る

**該当箇所**:

- `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:9`、`:34` — `(MUST NOT)`
- `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:4` — `unify-cell-common-fields-via-shared-row-layout`
- `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:8` — `本 change \`migrate-cell-base-to-stack-layout\``
- `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:10` — `全面移行したため`

**問題点**:

いずれも本 change が実際に書き換えたコメントブロックの中に残っている。`UnifyCellCommonFieldsTests.swift` の冒頭ブロックは本 change が 3 行を差し替えており (旧 18-20 行の `openspec/...` を削除し、13・16-17 行を追加)、その 4〜10 行目に変更提案 ID の裸参照 2 件と履歴記述 (「全面移行したため」) が同居している。`CellBaseLayout.swift` の冒頭ブロックと `applyCellBaseLayout` の doc comment も本 change が書き換えており、そこに `(MUST NOT)` が残る。

[ソースコメント規約](../../concepts/cross/conventions/comment-policy.md) は「変更提案の識別子の裸参照」「進捗ログ・履歴記述」「デルタスペック構文キーワードの混入」をいずれも禁止し、適用契機を「既存コメントに触れる実装をするとき」と定めている。前サイクルの Minor 指摘 (#2) が「触れたブロック内の `openspec/changes/...` 行も落として説明文だけを残す」と書いた文言どおりの部分は実行されているが、同じブロックの別カテゴリは残った。

特に `UnifyCellCommonFieldsTests.swift:8` の「本 change `migrate-cell-base-to-stack-layout` で〜」は、このファイルが既に別の change で編集されている以上、記述として現状と食い違っている。

挙動には影響せず、実装が新たに持ち込んだ文言でもなく、指す先 (`openspec/changes/archive/`) は凍結資料として残るため、アーカイブで即座に壊れる性質のものではない。よって blocking とはしない。

**推奨修正**: commit 前に以下へ書き換える (テスト再実行不要)。

- `(MUST NOT)` → 削除し「〜は使わない。」で文を閉じる
- `UnifyCellCommonFieldsTests.swift:4-10` → 変更 ID と「全面移行したため」を落とし、「共通フィールド (`description` / `valueText` / `icon` / `hintText` / `accentColor`) と `applyCellBaseLayout` の振る舞いを、自前 UIStackView 構造に対して検証する」という現在形の説明にまとめる

なお同一ファイルの `CellBaseLayout.swift:64` (`// 旧 UIListContentConfiguration / UICellAccessory 経路を完全に無効化`)、`:127` (`// （旧 valueText パラメータの代替、〜）`) も履歴記述だが、本 change が触れていない独立ブロックのため対象外とする (リポジトリ全体の掃除は別 change の責務)。

### [🟡 Minor] `CellBaseLayout.swift` 冒頭の階層列挙に `accessoryHolder` が入っていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:6-7`

**問題点**:

```
// `KsListCellBase` が `init(frame:)` で install した自前 UIStackView 階層
// （`stackH` / `stackV` / `contentStack` / `iconImageView` / `titleLabel` / `descriptionLabel`）の
```

本 change で `accessoryHolder` が `init(frame:)` install の恒常メンバーに加わり、`applyCellBaseLayout` はこれを `setAccessoryView(_:)` 経由で更新するようになった。にもかかわらず、この関数の役割を説明する冒頭ブロック (本 change が直後の 11-22 行を全面的に書き換えたブロック) の列挙は旧 6 メンバーのままで、直後に説明されている `accessoryView` 系統との対応が取れていない。

同種の指摘 (#7 = valueLabel 按分コメントの実態ずれ) が前サイクルで採用されており、同じ書き換えパスで直せる取りこぼしにあたる。`KsListCellBase.swift:11-22` の構造図は正しく `accessoryHolder` を含んでいるため、この 1 箇所だけがずれている。

**推奨修正**: 列挙に `/ \`accessoryHolder\`` を追記する。

## アクションプラン

1. commit 前に Minor 2 件を一掃する (5 行程度のコメント編集のみ。挙動変更なし、テスト再実行不要)
2. 一掃しない場合も蒸留 (ksn-distill) は進めてよい。ただし残存分は同種の既存違反としてリポジトリ全体の掃除 change に引き継ぐ

## 確認した観点 (指摘に至らなかったもの)

**ビルド・テスト**
- `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` → **337 tests / 0 failures**、`** TEST SUCCEEDED **`
- 今サイクル追加・改名された 10 テスト (Picker 系 4 / EntryCell 1 / accessoryHolder 系 5) が個別に pass していることをログで確認

**今サイクルの修正による挙動の非破壊**
- `ios/Sources/` 側の差分はコメントのみで、1 周目からロジックの変更は入っていない (`git diff` で確認)。`setAccessoryView(_:)` の除去ループ・同一インスタンス再指定分岐・`prepareForReuse` の後始末は 1 周目のまま
- 追加された `InputCellsTests` のヘルパは `KsListCellBase` を引数に取り、`contentStack` / `accessoryHolder` / `titleLabel` を読むだけの読み取り専用。既存テストへの副作用なし
- 幾何テストの nil 分岐強化は assert の追加のみで、既存 assert (description の非交差・アクセサリ垂直センター・折り返し発生) はそのまま残っている

**仕様充足 (再確認)**
- `cell-types-basic` の Scenario「Picker 系は valueText が行内・chevron が Cell 級」「EntryCell の入力フィールドは行内のまま (iOS)」が、今回追加された renderer 単位テストで初めて直接固定された (1 周目は共通関数テストからの推移でしか担保されていなかった)
- `settings-view-ios-host` の Scenario「レイアウト後の幾何関係」の後半文 (nil 時の `stackV` 回復) が spec 文言どおり検証されるようになった
- 足場アーティファクト (`specs/` `proposal.md` `ui/`) は未変更。`tasks.md` はチェック更新のみで、5.1 / 5.2 を含め虚偽のチェックは検出しなかった (5.1 / 5.2 の視覚照合は 1 周目にレビュアーが Simulator で独自確認済み、記録は `review-001.md`)
- deviation.md は今サイクルでも不要 (無断の仕様逸脱を検出せず)

**降格・情報提供扱いだった項目 (今回は指摘しない)**
- 実装後スクリーンショットのアーティファクト追加 (突き合わせ #3 で降格)
- valueText とアクセサリの間隔 6pt → 16pt (突き合わせ #6 で情報提供のみ)
- toggle 自身の中心 Y / holder 幅の `intrinsicContentSize` 比較 (突き合わせ #5 で spec 以上の要求として降格)
- リポジトリ全体の既存コメント掃除 (本 change のスコープ外)

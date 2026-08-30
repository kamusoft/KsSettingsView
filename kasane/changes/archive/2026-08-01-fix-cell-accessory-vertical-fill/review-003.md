# レビュー結果: fix-cell-accessory-vertical-fill (003 回目)

**日付**: 2026-08-01
**判定**: APPROVED

## サマリー

今サイクルで追加された 2 件の修正 (アクセサリ間隔の約 6pt 復帰 / 触れた 14 ファイルのコメント規約一掃) を中心にレビューした。間隔修正は `stackH.setCustomSpacing(contentStack.spacing, after: stackV)` の 1 行で、icon ↔ stackV の 16pt を温存しつつ stackV ↔ accessoryHolder のみを 6pt に詰める実装として正しく、既存の幾何 Scenario (description 非交差 / 垂直センター / nil 時の stackV 回復) を壊していないことをテストで確認した。コメント一掃も 14 ファイルから禁止参照 (`openspec/` / 変更提案 ID / レビュー通番 / spec 裸参照 / `MUST` 系キーワード) を完全に除去できており、review-002 で残していた Minor 2 件も解消済み。ただしテスト 2 ファイルに履歴記述類型の「新 API」が 14 箇所残っている (Minor)。ブロッキング事項はない。

**ビルド / テスト**: `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` → **337 tests / 0 failures / TEST SUCCEEDED**。

## 前サイクル (review-002) 指摘の解消確認

| 指摘 | 状態 |
|---|---|
| 🟡 触れたブロックに `(MUST NOT)` / 変更提案 ID / 「全面移行したため」が残る | **解消**。`CellBaseLayout.swift:9,34` の `(MUST NOT)` 削除、`UnifyCellCommonFieldsTests.swift:1-18` は現在形の説明に全面書き換え |
| 🟡 `CellBaseLayout.swift` 冒頭の階層列挙に `accessoryHolder` がない | **解消** (`CellBaseLayout.swift:7-9`) |
| (対象外扱いだった) `CellBaseLayout.swift:64` `:127` の履歴記述 | 今サイクルの範囲拡大により **併せて解消** |

## 指摘事項

### [🟡 Minor] テスト 2 ファイルに履歴記述類型「新 API」が 14 箇所残る

**該当箇所**:

- `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift:69, 135, 220, 221, 635`
- `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:135, 154, 155, 156, 164, 262, 267, 276, 512`

**問題点**:

いずれも「新 API では contentConfiguration は使わず〜」「// MARK: - 新 API 構造リグレッション防止テスト」の形。[ソースコメント規約](../../concepts/cross/conventions/comment-policy.md) の「禁止する記述類型 — 進捗ログ・履歴記述」は時間軸を含む記述 (「旧方式は〜」「〜へ移行」等) を禁じており、「新 API」はその鏡像にあたる。リポジトリ内に「旧 API」はもはや存在せず、このファイルだけを読む人にとって「新」は情報を持たない (どの API と対比しているのか追えない) 一方、時間が経つほど記述として古びる。書き換え判断基準の類型 3 (履歴記述型 → 現在の仕様の説明に書き換えるか削除) に該当する。

本サイクルで拡大したスコープ (触れた 14 ファイルのファイル単位一掃) の内側であり、`UnifyCellCommonFieldsTests.swift:512` の MARK は本 change が直下のテスト群を大幅に書き換えたブロックの見出しでもある。挙動には影響せず、指す先が壊れる性質のものでもないため blocking とはしない。

**推奨修正**: 「新 API」を落として現在形で閉じる。例:

- `XCTAssertNil(view.contentConfiguration, "新 API は contentConfiguration を使用しない")` → `"contentConfiguration 経路は使用しない"`
- `// MARK: - 新 API 構造リグレッション防止テスト` → `// MARK: - subview 構造のリグレッション防止テスト`
- `// MARK: - applyCellBaseLayout 経由の描画テスト（新 API）` → `// MARK: - applyCellBaseLayout 経由の描画テスト`
- `// MARK: - ButtonCell レイアウト分岐（新 API: 常に applyCellBaseLayout 経由）` → `（常に applyCellBaseLayout 経由）`

テスト再実行は不要 (コメント・アサーションメッセージのみ)。

### [🔵 Suggestion] 移植元 AiForms への行番号参照 `L656-758` は落として関数名で指す

**該当箇所**: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:25`、`ios/Sources/KsSettingsViewUI/KsListCellBase.swift:6`

```
//   - AiForms オリジナル `SettingsView/Native/iOS/Cells/CellBaseView.cs` L656-758（自前 stack 構造の根拠）
```

**問題点**:

**規約違反ではない**ことを先に明記する。[ソースコメント規約](../../concepts/cross/conventions/comment-policy.md) の「アーカイブ文書のパス / 行番号」禁止は `openspec/` と `kasane/changes/` を対象としており、AiForms への言及は同規約の「移植元 AiForms への言及」節で現在形の仕様説明として明示的に許容されている。この参照も「今この実装が守っている互換契約の根拠」であって履歴記述ではない。

一方で [移植元 AiForms リポジトリの参照](../../concepts/cross/conventions/aiforms-origin-reference.md) は、SettingsView が「upstream で不具合修正が継続中」であり移植元コードが動き続けることを記している。行番号は upstream の 1 コミットで無音のうちにずれ、こちら側で壊れたことに気づく手段がない (ファイル名・型名と違い grep でも検出できない)。同じ情報は同ファイル内の `KsListCellBase.swift:138` `:267` が既に採っている形 — `CellBaseView.cs` の `SetUpContentView()` / `SetUpHintLabel()` という**関数名**で指す形 — で、腐らずに表現できている。

**推奨修正**: 2 箇所の `L656-758` を落とし、`CellBaseView.cs` の `SetUpContentView()` 準拠、という関数名指定に揃える。今サイクルで対応せず据え置く判断でも実害は小さい (次に移植元を参照する人が数十行ずれた位置から読み始めるコスト)。

### [🔵 Suggestion] 約 6pt の間隔にリグレッションガードがない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:159`

**問題点**:

`deviation.md` に記録されたオーナー指示 (valueText と Cell 級アクセサリの間隔を約 6pt に維持) を担保しているのは `setCustomSpacing` の 1 行だけで、値を固定するテストがない。`settings-view-ios-host` の Requirement が「spacing・margin 等の視覚パラメータは本 spec の対象外」と定めている以上、テストは spec 上の必須ではない。ただしこの間隔は「承認済み mock (16px) をオーナー指示で覆した値」であり、mock だけを見て後から 16pt に戻される再発シナリオが現実に想定できる (`kasane/lessons/inbox/mock-shows-param-not-matching-current-impl.md` が count 2 でまさにこの型を数えている)。

既存の幾何テスト `test_レイアウト後にdescriptionはアクセサリと交差せずアクセサリは垂直センター` は `descFrame.maxX <= holderFrame.minX + 0.5` と nil 時の stackV 回復を見ているだけで、16pt に戻っても pass する。同テストは icon ↔ stackV の 16pt も見ていない。

**推奨修正**: 同テストに 2 行足すだけで両方を固定できる。

- `holderFrame.minX - stackVFrame.maxX` が `cell.contentStack.spacing` と一致すること
- icon ありのケースで `stackVFrame.minX - iconFrame.maxX` が `cell.stackH.spacing` と一致すること

## 確認した観点 (指摘に至らなかったもの)

- **今サイクル (A) が既存 Scenario を壊していないか**: `setCustomSpacing` は `after: stackV` にのみ効き `stackH.spacing = 16` は不変。`iconImageView` ↔ `stackV` は 16pt を維持する。`accessoryHolder` が `isHidden = true` のとき UIStackView は隣接 spacing ごと畳むため空領域は残らず、これは `test_レイアウト後に…` 後半の `stackVFrame.maxX == contentView.maxX - layoutMargins.right (accuracy 0.5)` が実際に担保している (16pt でも 6pt でも成立する形ではなく、余白が残れば落ちる形になっている)
- **`contentStack.spacing` を参照する結合の妥当性**: リポジトリ全体で `contentStack.spacing` / `stackH.spacing` を再代入する箇所はなく (install 時の初期値 6 / 16 のみ)、install 時に 1 度読む実装で不整合は起きない。「行内要素と同じリズムに揃える」意図はコメントに自己完結して書かれている
- **コメント一掃 (B) の網羅性**: 14 ファイルに対し `openspec/` / `Phase N` / `Round N` / `Decision N` / `Major-` / `論点` / `.md` / `proposal` / `review-` / `MUST` / `SHALL` / `SHOULD` / `result_` / `Suggestion` / `セクション N` / 変更提案 ID (`unify-` `migrate-` `fix-ios` `add-cell`) を grep → **全て 0 件**。残る `旧` は `旧内容` `旧クロージャ` `旧アクセサリ` の 3 種で、いずれも「再 render 時に置換される直前の内容」を指す現在形の実行時説明であり履歴記述ではない
- **規約が禁じる「書き換え作業中の新規 ADR 起票」**: `kasane/decisions/` に working tree の変更なし。参照されている `ios/ADR-0001` は前コミット (3c24f3c) 時点で存在し、内容 (アクセサリ列を contentStack 外へ) もコメントの主張と一致している
- **足場アーティファクトの保全**: working tree で書き換えられている足場は `tasks.md` のチェックボックス 13 件のみ (実装進捗の記録であり本文改変なし)。`specs/` `proposal.md` `ui/` `deviation.md` `review-001/002.md` `second-opinion-002.md` `verify-001.md` に変更なし
- **`deviation.md` 記録済みの乖離**: mock (gap 16px) との差異は合意済み差分として扱い、指摘していない
- **renderer 振り分けの一貫性**: 9 種すべてが `accessoryView:` へ移行し、`trailingViews` に Cell 級アクセサリを渡している箇所は残っていない。`ButtonCellView` / `LabelCellView` / `EntryCellView` は未変更で、EntryCell の入力フィールドは `contentStack` に残る (`test_EntryCellView_入力フィールドは行内でアクセサリ列は空` が担保)
- **`setAccessoryView(_:)` の堅牢性**: 旧内容の除去 → 同一インスタンス再指定時の温存 → nil 時の `isHidden` の順序に穴がなく、`removeArrangedSubview` + `removeFromSuperview` の対で view 階層に残骸を残さない (`test_再renderでaccessoryHolderの内容は常に0個または1個` が A→B→nil / 同一連続 / chevron 3 往復まで通している)
- **`prepareForReuse` の first responder 保護**: `accessoryHolder` に入るのは Switch / checkbox / checkmark / chevron のみで first responder を取り得ず、編集中 `UITextField` は従来どおり `clearContentStackTrailingViews()` 側の保護下に残る
- **テストの手抜き**: 言い訳コメントによる実質スキップ、`XCTSkip`、空アサーションは検出されず。`InputCellsTests.swift` の Picker 系 4 種は共通ヘルパで同一契約を検証しており、`file:` / `line:` を伝播させて失敗位置が呼び出し側に出る形になっている

## アクションプラン

1. commit 前に Minor 1 件 (「新 API」14 箇所) を一掃する。コメント・メッセージ文字列のみでテスト再実行不要
2. Suggestion 2 件は任意。特に AiForms 行番号は蒸留 (ksn-distill) 前に落としておくと移植期間中の腐りを避けられる
3. 上記いずれも未対応でも蒸留・アーカイブを進めてよい (挙動・spec 充足に影響しない)

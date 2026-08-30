# レビュー結果: fix-cell-accessory-vertical-fill (004 回目)

**日付**: 2026-08-01
**判定**: APPROVED

## サマリー

review-003 の残 3 件 (Minor 1 / Suggestion 2) の処理だけに範囲を絞って確認した。3 件とも妥当に処理されており、既存の spec 充足・renderer 振り分け・幾何挙動を壊していない。コメント規約の独立走査では「新 API」が完全に消え、禁止参照 4 種のうち 3 種 (変更提案 ID / アーカイブ文書パス / 通番) と禁止類型 4 種は 14 ファイル内 0 件だったが、**`InputCellsTests.swift:52` に「拡張子なしの裸参照」型が 1 件だけ残っている** (🟡 Minor / 非ブロッキング)。AiForms 参照の関数名置換は移植元の実体と厳密に一致することを確認済み。リグレッションガードは変異テストで実際に退行を検出することを実証した。**本 change の完了を妨げる欠陥はない。**

**ビルド / テスト**: `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` (`ios/` で実行) → **338 tests / 0 failures / TEST SUCCEEDED**。

## review-003 残 3 件の処理確認

| 指摘 | 状態 | 根拠 |
|---|---|---|
| 🟡 「新 API」履歴記述 14 箇所 | **解消** (ただし別型が 1 件残る → 後述) | 14 ファイル全走査で `新 ?API` **0 件**。書き換え後のコメントはいずれも自己完結した現在形 |
| 🔵 AiForms 行番号 `L656-758` → 関数名 | **解消・正確** | 移植元実体と厳密一致 (後述) |
| 🔵 6pt のリグレッションガード | **解消・有効性を実証** | 変異テストで退行検出を確認 (後述) |

---

### (1) コメント規約の独立走査

**「新 API」の除去**: 14 ファイルに対し `新 ?API` / `新しい API` / `新方式` / `旧方式` / `全面移行` / `刷新` / `撤去` / `廃止` / `以前` / `かつて` / `従来` / `レガシー` / `deprecated` / `だった` / `していた` / `変更前` / `変更後` を走査 → **履歴記述は 0 件**。

書き換え後の到達点も確認した。`UnifyCellCommonFieldsTests.swift:1-15` のヘッダは「`applyCellBaseLayout` は `UIListContentConfiguration` / `UICellAccessory` を使わず `KsListCellBase` の自前 `UIStackView` 階層を直接更新するため、以下を assert する」という現在形の契約説明に着地しており、対比相手 (旧 API) を知らない読者にも単独で意味が通る。アサーションメッセージ (`"contentConfiguration 経路は使用しない"`) と MARK (`// MARK: - subview 構造のリグレッション防止テスト`) も同様。

**禁止する参照 4 種の走査結果**:

| 類型 | 走査方法 | 結果 |
|---|---|---|
| 変更提案 ID の裸参照 | `kasane/changes` + `kasane/changes/archive` + `openspec/changes` + `openspec/changes/archive` から change-id を機械抽出 (**32 件**) し、各 ID を 14 ファイルに全件 grep | **0 件** |
| Phase / Round / Decision / Critical / Major / Minor / 論点 / 案 番号 | 正規表現一括 | **0 件** |
| アーカイブ文書のパス / 行番号 | `openspec/` `kasane/` `.md` `review-` `proposal` `result_` | **0 件** |
| 拡張子なしの裸参照 | `spec` を単語境界で走査 | **1 件残** → 下記 Minor |

**禁止する記述類型 4 種**: デルタスペック構文キーワード (`MUST` / `MUST NOT` / `SHALL` / `SHOULD` / `MAY`) **0 件**。レビュー指摘通番 (`C-1` / `M-1` / `#N` 等) **0 件**。

**許容参照の妥当性**: 残る外部参照は `ios/ADR-0001` (`CellBaseLayout.swift:11` / `KsListCellBase.swift:89`) のみで、`kasane/decisions/ios/0001-accessory-column-outside-content-stack.md` として実在し、規約の `<domain>/ADR-NNNN` 形式に適合する。`kasane/decisions/` `kasane/concepts/` に working tree の変更はなく、規約が禁じる「書き換え作業中の新規 ADR 起票」は起きていない。

---

### (2) AiForms 参照の関数名置換

移植元 `../AiForms.Maui.SettingsView/SettingsView/Native/iOS/Cells/CellBaseView.cs` を直接確認した。

- **L656** に `protected virtual void SetUpContentView()` の定義、その閉じ括弧が **L758** (ファイル全体は 762 行)。**旧表記 `L656-758` は `SetUpContentView()` と完全に同一範囲**であり、置換は情報を落とさず腐らない形に移せている
- 同ファイル L621 に `void SetUpHintLabel()` があり、`KsListCellBase.swift:267` の既存表記 `AiForms オリジナル \`CellBaseView.cs\` SetUpHintLabel() 準拠：` と整合する
- 表記の揃い: `KsListCellBase.swift` 内の 3 箇所 (`:6` `:138` `:267`) はすべて ``AiForms オリジナル `CellBaseView.cs` <関数名>() 準拠`` の形で統一されている。`CellBaseLayout.swift:25` のみリポジトリ相対パス付き (``AiForms オリジナル `SettingsView/Native/iOS/Cells/CellBaseView.cs` SetUpContentView()``) だが、同ファイル内で唯一の AiForms ファイル参照であり内部不整合はない。初出でフルパスを与える形は移植元を初めて開く読者にむしろ有用で、指摘に至らない

---

### (3) 6pt リグレッションガードの有効性

`UnifyCellCommonFieldsTests.swift:752-805` `test_アクセサリ列は左隣と6ptで並びicon側は16ptを保つ` を確認した。

**退行検出の実証**: `KsListCellBase.swift:159` の `stackH.setCustomSpacing(contentStack.spacing, after: stackV)` を一時的にコメントアウトして全テストを実行した結果:

```
UnifyCellCommonFieldsTests.swift:793: error: ... XCTAssertEqualWithAccuracy failed:
  ("16.0") is not equal to ("6.0") +/- ("0.5") - valueText と Cell 級アクセサリの間隔は 6pt
Executed 338 tests, with 1 failure   ** TEST FAILED **
```

- `setCustomSpacing` が失われると実測値はちょうど 16.0 に戻り、**当該テストが確実に落ちる**。失敗メッセージだけで原因と期待値が読み取れる
- 失敗が **この 1 件のみ**であることは、他のどのテストもこの間隔を守っていない (= 本テストが唯一のガードである) ことの裏返しであり、追加の必要性そのものを裏付けている
- 検証後、`KsListCellBase.swift` は sha1 一致 (`ee30578b...`) でバイト単位に復元済み。working tree に検証の痕跡は残っていない

**併走の 16pt 固定も有効**: `stackVFrame.minX - iconFrame.maxX == 16.0` は、詰めが icon 側へ波及しないことを固定する。`accuracy: 0.5` は 6 ↔ 16 の 10pt 差に対して十分な分解能を持つ。

**リテラル採用の判断について — 支持する**。実装変数 (`cell.contentStack.spacing`) を期待値に使うと、`contentStack.spacing` が将来 16 に変更された瞬間に期待値も追随し、`deviation.md` で合意した「約 6pt」の契約が無言で失われても pass してしまう (同語反復テストになる)。本テストが守っているのは実装の内部整合ではなく **オーナー合意済みの視覚的契約そのもの** であり、その値は外側から独立に固定されるべき。リテラルが正しい。加えて、`contentStack.spacing` を正当な理由で変えるときは本テストが落ちてアクセサリ間隔を意識的に再確認させる — 乖離ガードとして望ましい失敗の仕方になっている。

コメント (`:752-760`) が「なぜ 6pt か」「16pt に開くのは退行」を自己完結して説明しており、テストだけを読む人がリテラルの由来を追える点も適切。

---

## 指摘事項

### [🟡 Minor] `InputCellsTests.swift:52` に「拡張子なしの裸参照」型が 1 件残る

**該当箇所**: `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:52`

```swift
// spec: textAlignment の既定は `.end`（AiForms `EntryCell.TextAlignmentProperty` の `TextAlignment.End` 準拠）
```

**問題点**:

[ソースコメント規約](../../concepts/cross/conventions/comment-policy.md) の「禁止する参照 — 拡張子なしの裸参照」は `spec L31-32` `delta spec Requirement「〜」` 等を挙げ、「`.md` を書かなくても意味は同一で、grep で検出しづらいだけ」として禁じている。この `// spec:` は文頭の定型句として delta spec を指しており、同じ類型にあたる。

同じ書き換えで、**このファイルのヘッダからは `// 仕様: openspec/changes/add-cell-types-input/specs/cell-types-input/spec.md` が正しく除去されている**。同一系統の参照が同一ファイル内で 40 行下に残った形で、今サイクルの走査の取りこぼしと判断する (リポジトリ全体でも `// spec:` の出現はこの 1 箇所のみ)。

一方で **ブロッキングではない**: 挙動に影響せず、指す先が壊れる性質でもなく、接頭辞を落としても文意は完全に保たれる (規約の書き換え判断基準の類型 1「定型句型」— 参照句が装飾で本文と独立している)。

**推奨修正**: 接頭辞 `spec: ` を削除する。残る文はそのまま自己完結する。

```swift
// textAlignment の既定は `.end`（AiForms `EntryCell.TextAlignmentProperty` の `TextAlignment.End` 準拠）
```

コメント 1 行のみのためテスト再実行は不要。

---

## 確認した観点 (指摘に至らなかったもの)

- **今回の 3 件が既存を壊していないか**: 変更は (a) コメント・アサーションメッセージ文字列、(b) 新規テストメソッド 1 件の追加のみ。実装ロジックへの変更はなく、338 tests / 0 failures で既存 Scenario の充足は維持されている (spec 充足・renderer 振り分け・視覚照合・Android 側は review-001〜003 / verify-001 の確認済み範囲として再検査していない)
- **「旧」が残る 5 箇所は違反ではない**: `CellBaseLayout.swift:149` / `KsListCellBase.swift:240,247` の「旧内容」、`SwitchCellView.swift:58` の「旧クロージャ」、`UnifyCellCommonFieldsTests.swift:661` の「旧アクセサリ」は、いずれも **再 render 時に置換される直前の実行時の値** を指す現在形の説明であり、規約が禁じる「過去仕様の説明」ではない (review-003 の判断を再確認して支持)
- **「追加される」等が残る箇所も違反ではない**: `DatePickerCellView.swift:136` 「Today ボタンが Cancel と Done の間に追加される」、`BasicCellsTests.swift:93,98,107,115` 「contentStack に value label が追加される」等は実行時の振る舞いの記述であり、進捗ログではない
- **`test_...6ptで並びicon側は16ptを保つ` のコメントに含まれる「維持している」「退行」**: 時間軸を含む語だが、これは「今守っている契約」と「その契約が破れた状態」の定義であり、リグレッションガードの目的説明として必要。過去仕様の記述ではないため禁止類型に当たらない
- **足場アーティファクトの保全**: `specs/` `proposal.md` `ui/` `deviation.md` `review-001/002/003.md` `second-opinion-002.md` `verify-001.md` に変更なし。`tasks.md` の差分はチェックボックス 13 件の `[ ]` → `[x]` のみで本文改変なし
- **`deviation.md` 記録済みの乖離**: mock (gap 16px) に対する 6pt は合意済み差分として扱い、指摘していない。むしろ (3) のテストがこの合意を明示的に固定した点を評価する
- **未実装の虚偽チェック**: `tasks.md` の 13 項目はいずれも対応する実装・テストが存在する (4.6 の「全件 `swift test` pass」だけは、macOS ホストの `swift test` が `#if canImport(UIKit)` により UI テストを 1 件も実行しない点で表現が実態とずれるが、足場は書き換え禁止であり、この観測は `kasane/lessons/inbox/ios-ui-tests-not-run-by-swift-test.md` に既に捕捉されている)
- **テストの手抜き**: 追加された 1 件に `XCTSkip`・言い訳コメント・空アサーションはなく、`XCTAssertGreaterThan(holderFrame.width, 0)` でアクセサリ列が実体を持つ前提も併せて固定している

## アクションプラン

1. (任意 / 推奨) commit 前に Minor 1 件 (`InputCellsTests.swift:52` の `spec: ` 接頭辞削除) を落とす。1 行・テスト再実行不要
2. 上記が未対応でも蒸留 (ksn-distill) ・アーカイブを進めてよい。**本 change の完了を妨げる欠陥はない**

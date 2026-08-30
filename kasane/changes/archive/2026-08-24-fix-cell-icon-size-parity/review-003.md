# レビュー結果: fix-cell-icon-size-parity (003 回目)

**日付**: 2026-08-23
**判定**: APPROVED

## サマリー

review-002 の残り 2 件 (Minor 1 / Suggestion 1) は、いずれも**指摘の実体を解消している**。テスト KDoc は「唯一の構成」の主張を落とし、書き換え後の記述が実装の分岐 `fillsRow = valueText == null && !views.hasFillingInlineTrailing` および core/ADR-0026・`concepts/core/styling/cell-row-layout.md` の用語定義と正確に一致することを確認した。`brief.md` の追記も、PNG 点数・撮影時点・列挙された「以後の修正」の網羅性をファイル実体とタイムスタンプで裏取りし、事実として正しい。

新たな退行・欠陥・規約違反は見つからなかった。Android のユニットテストと comment-policy lint をレビュー側で再実行し、いずれも合格。指摘は体裁のみの Suggestion 1 件で、コード契約・テスト・証跡のいずれにも影響しない。

---

## 前回指摘 (review-002) の解消状況

| # | 重要度 | 指摘 | 状態 |
|---|---|---|---|
| 1 | 🟡 Minor | テスト KDoc の「唯一の構成」が実装の分岐と矛盾する | ✅ 解消 |
| 2 | 🔵 Suggestion | 視覚証跡の対象範囲が brief に書かれていない | ✅ 解消 |

### 1. テスト KDoc (`CellRowWidthAllocationTest.kt:674-676`)

書き換え後の記述:

> 行内 trailing が無い行では title が主行の全幅を取る（core/ADR-0026）ため、`titleAlignment` が配る余白が生まれる。ここではその全幅構成のうち、本体行が Cell 全体へ広がるボタンスタイルを測る。

**内容が現行契約と一致するかで判定した結果、一致している。** 根拠は 3 点:

- **用語の一致**: `concepts/core/styling/cell-row-layout.md:22` は「**行内 trailing** — valueText と、EntryCell の入力フィールド (両 platform)」と定義する。つまり `valueText` は行内 trailing の一種であり、「行内 trailing が無い行」は実装の `valueText == null && !views.hasFillingInlineTrailing` (`CellBaseLayout.kt:466-469`) と過不足なく対応する。前回 Minor が問題視した「aux なしだけが特別」という含意は消えている
- **ADR との一致**: `core/ADR-0026` の決定事項「行内 trailing がない Cell では title が主行の全幅を使える (従来どおり。`ButtonCell` の中央揃えなどが依存する)」(`0026-...md:26`) と同義。参照 ID の付け方も正しい
- **限定の方向が正しい**: 「その全幅構成のうち」「ボタンスタイルを**測る**」という書き方は、全幅構成が複数あることを前提にその一部を測ると宣言しており、review-002 が実測 (icon のみ: title 248px / 自然幅 91px、hintText のみ: 288px / 91px) で示した「icon のみ・hintText のみの ButtonCell でも alignment の余白は生まれる」という事実と矛盾しない。`deviation.md` の限定 (`valueText` または残り幅を占める行内 trailing を持つ ButtonCell) とも整合する

同ファイル内の既存記述 (`:273` の KDoc「行内 trailing がない場合は title が全幅を使う」、同名テストメソッド) とも用語が揃っており、ファイル内で一貫している。`grep 唯一` で本 change のソース・アーティファクトを全走査し、当該箇所以外に同型の残存はない (ヒットした 7 件はすべて本 change と無関係の既存記述)。

### 2. brief.md「証跡が対応する実装範囲」

追記された段落の主張を 1 つずつ実体で確認し、**すべて事実と一致**した:

- 「`verification/` の PNG 8 点」 → `ui/verification/` の実体は PNG 8 件 + `.log` 1 件。点数一致
- 「2026-08-23 のレビュー前実装で撮影した」 → PNG の mtime は 00:37〜00:53
- 「以後に入った修正は (1) Android の outline provider 方式 (2) iOS の icon 表示 API 分割 (3) テストコードとコメントの是正のみ」 → 撮影後 (00:53 以降) に変更された production ソースは `CellBaseLayout.kt` (01:34)・`CellBaseLayout.swift` (01:12)・`KsListCellBase.swift` (01:12) の 3 件のみで、(1)(2) と正確に対応する。`ButtonCellViewHolder.kt` (00:00)・`EntryCellViewHolder.kt` (23:59)・`EffectiveStyle.kt` (23:40)・`.gitignore` (00:30) はいずれも撮影前で、列挙漏れではない。**列挙は網羅的**
- 「`ios-test-constraints.log` のみ修正反映後に再実行し、対応する実装ソースの SHA-256 を併記している」 → ログの実行日時 01:22 は iOS 修正 (01:12) の後。ログが併記する 6 件の SHA-256 を現作業ツリーで再計算し**全件一致**。ログの対象が iOS ソースのみであることも、Android 側の 01:34 の変更がログの同定性を壊していないことを意味する
- 「いずれも描画結果を変えないため再撮影していない (レビュー側も同判定)」 → review-002 の「レビュー側の判定: 再撮影は不要」と一致。参照先 `review-002.md` は同一 change ディレクトリ配下でアーカイブされるため、リンクは事後も解決可能

---

## 指摘事項

### [🔵 Suggestion] 書き換えで生じた行の折り返し崩れと語の重複 (体裁のみ)

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:676-677`

**問題点**: 書き換えで文が伸びた結果、折り返し位置が段落内の他行と揃わなくなっている。

```
     * 測る。そのため gravity のフラグ値だけでなく
     * **テキストの実描画位置**まで測る:
```

676 行目末尾が極端に短く、また「〜ボタンスタイルを**測る**。そのため〜まで**測る**」と同じ動詞が連続する文の末尾で重なる。内容の正確さには影響しない純粋な体裁で、**このまま蒸留しても実害はない**。

**推奨修正** (任意): 段落を再整形し、動詞の重複を解く。例:

```
     * 行内 trailing が無い行では title が主行の全幅を取る（core/ADR-0026）ため、`titleAlignment`
     * が配る余白が生まれる。ここではその全幅構成のうち、本体行が Cell 全体へ広がるボタンスタイルを
     * 対象にし、gravity のフラグ値だけでなく**テキストの実描画位置**まで検証する:
```

---

## 確認した観点 (指摘なし)

- **テスト (レビュー側で再実行)**: `./gradlew :ks-settingsview-ui:testDebugUnitTest` → BUILD SUCCESSFUL (2m)、`testDebugUnitTest` は実際に実行 (up-to-date スキップではない)。JUnit XML 集計で tests 930 / failures 0 / errors 0 / skipped 0、`CellRowWidthAllocationTest` は 17 tests / failures 0
- **プロジェクト固有規約**: `python3 scripts/comment-policy-lint.py --summary` → 「合計: 0 ファイル / 禁止 0 件 (検査対象 670 ファイル)」。追記された `core/ADR-0026` 参照は許容形式で、アーカイブ配下 PNG 参照・変更提案内通番の持ち込みはない
- **足場の凍結**: `git status` で `specs/` `proposal.md` `exploration.md` `deviation.md` `review-001.md` `review-002.md` `second-opinion-code-001.md` `verify-001.md` に今回の変更なし。書き換わったのは対象の 2 ファイルのみ
- **変更範囲の限定**: `git diff` で今回の 2 ファイル以外に production ロジックの差分がないことを確認。KDoc の書き換えはテストの実行内容 (アサーション・bind 引数) に一切触れていない
- **前回の APPROVED を覆す事実の有無**: 見つからなかった。今回の 2 修正はいずれもドキュメント・コメントであり、review-002 が実測 (ミューテーション 3 本・SHA 突き合わせ・581/2582 件のテスト) で確定した検出力・整合の結論に影響しない
- **合意済み事項**: `deviation.md` 記載の付随修正 5 件・見送り 3 件・ButtonCell の titleAlignment はオーナー合意済みとして指摘対象から除外した

---

## アクションプラン

1. (Suggestion / 任意) `CellRowWidthAllocationTest.kt:676-677` の折り返しと動詞重複を整える

蒸留・アーカイブに進んで差し支えない。上記 1 を見送る場合も判定は APPROVED のまま。

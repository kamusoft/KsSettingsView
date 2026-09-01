# レビュー結果: fix-ios-test-pump-condition-wait (004 回目)

**日付**: 2026-09-01
**判定**: APPROVED
**範囲**: 修正サイクル 3 周目の差分に限定 (4 ファイル)。全体の再監査は review-001〜003 で完了済み

## サマリー

review-003 の 3 件 (Major 1 / Minor 1 / Suggestion 1) はいずれも解消している。層 2 は「レイアウトが kind の attributes を置いている」+「可視矩形と交差する分だけ実体化を要求する」へ書き換わり、画面外の Root Footer を待たなくなった。**「attributes が 1 件も無ければ不成立」で層 2 の目的が保たれている**という実装ワーカーの主張は、レビュアー側でスクラッチへ複製したツリー上で独立に再現して確認した (下表)。

この差分で新たに入った問題は見つからなかった。問い合わせ範囲をコンテンツ矩形へ広げたコストも実測したが、待機ループの負荷として問題にならない水準である。指摘は 🔵 Suggestion 2 件のみで、いずれも判定を左右しない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (コメント構文を持つ全ソース)。今周期で書き換わった doc 3 ブロックを規約本文の禁止類型で人手照合した |
| `kasane/handbook/cross/test-execution.md` | テストの実行と結果の報告 |
| `kasane/lessons/code-review.md` | 重点観点 L-001 (争点は実測で決着させる) |

照合結果:

- **comment-policy** — 今周期で書き換わったのは `CollectionRenderWait.swift` の 3 ブロック (層 2 の説明 `:22-25`、`isRequiredSupplementaryReady` の doc `:94-99`、`contentLayoutAttributes` の doc `:119-122`)。change-id の裸参照 / Phase・Round・Decision 通番 / タスク通番 / アーカイブ文書のパス / 拡張子なし裸参照 は 0 件。デルタスペック構文キーワードの混入 0 件。履歴記述 (「旧〜」「〜へ移行」) も 0 件で、すべて現在形。**適合**。review-003 が Major の一部として挙げた doc と実装の食い違い (`:30-32` の原則に層 2 だけが従っていない) は、層 2 の説明に「実体化を求めるのは可視矩形にかかる分だけで、画面外にある要素は 3 と同じく対象外」が入ったことで解消している
- **test-execution** — 完了判定に絞り込みなしの全件 Simulator 実行を使っている。レビュアー側でもクリーンな `derivedDataPath` で再実行し件数まで確認した (下記)。**適合**
- **lessons L-001** — 争点 (層 2 の目的が保たれているか / コストが問題になるか) を静的読解で終わらせず、複製ツリー上の probe で実測した。レビュー対象のツリーには本ファイルの作成以外の書き込みをしていない (復帰の確認方法は末尾)

## 前回指摘の解消状況

| # | review-003 の重要度 | 指摘 | 状態 |
|---|---|---|---|
| 1 | 🟠 Major | 層 2 が「その kind が可視であること」を無条件に要求し、画面外の Root Footer を持つ構成で必ず deadline 超過する | **解消** |
| 2 | 🟡 Minor | 本 change が Swift 6 警告を 4 件増やしている | **解消** |
| 3 | 🔵 Suggestion | 実験が層 2 の「必要性」しか測っていない | **解消** |

### 1 の確認 (レビュアー側の独立実測)

`ios/Tests/KsSettingsViewTestSupport/CollectionRenderWait.swift:100-117` の `isRequiredSupplementaryReady` が、`visibleSupplementaryViews(ofKind:).isEmpty == false` から次の 2 段へ変わっている。

- レイアウトが置く attributes を `contentLayoutAttributes` (コンテンツ矩形 ∪ 可視矩形) から取り、対象 kind が 1 件も無ければ **false** (`:108`)
- 置かれている分のうち **可視矩形と交差するものだけ** 実体化を要求する (`:110-115`)

複製ツリーへ検証用テストを足して実測した結果 (可視矩形の高さ 700、Root Header / Footer とも `awaitInitialRender` 前に設定):

| 構成 | contentH | footer の attributes | 可視矩形と交差 | 実体化 | `awaitInitialRender` の経過 | 結果 |
|---|---:|---:|---:|---:|---:|---|
| 60 行 + Root H/F | 3126.06 | 1 件 (y=3106, h=20) | 0 件 | 0 件 | **0.0017 秒** | 成立 |
| 2 行 + Root H/F | 153.67 | 1 件 (y=133.7, h=20) | 1 件 | 1 件 | 0.00008 秒 | 成立 |

review-003 が 3.018 秒の deadline 超過を実測した構成が、同じツリー上で 0.0017 秒で成立するようになっている。**画面外の Root Footer を待たなくなったことを確認した。**

### 2 の確認 (層 2 の存在理由が保たれているか)

Root Footer を設定していない controller に `ks-root-footer` を必須 supplementary として渡す probe を独立に組んで実測した。ワーカーの報告 (`placed=0` → deadline 超過) は**再現する**。

```
ZZRV004 C-before kind=ks-root-footer contentH=133.67 boundsH=700.0 placed=0 intersects=0 visible=0
ZZRV004 C elapsed=1.015 failed=true
条件が deadline 内に成立しなかった: 置かれていない kind の待機
 (期待 Section 構造: [2], 必須 supplementary: ["ks-root-footer"])
 / 経過 1.010 秒 (deadline 1.000 秒)
 / 実測: Section 1 [0] 行 2/2 / 必須 supplementary [ks-root-footer レイアウト 0 件/実体化 0 件]
 / 未実体化の可視要素 なし
```

「attributes が無い」(層 2 が埋める窓) と「attributes はあるが可視外」(層 3 が対象から外す状態) が分岐しており、**層 2 の目的は保たれている**。失敗メッセージがレイアウト上の件数と実体化の件数を分けて出すため、どちらで止まっているかがメッセージだけで判別できる点も確認した。

**条件を緩めたことで新たな穴が開いていないか**も確かめた。緩和は「可視矩形と交差しない attributes は実体化を求めない」だが、`intersects` は面積 0 の矩形も false にするため、**面積 0 の必須 supplementary が層 2 を素通りする**経路が理屈の上では残る。実際に到達できるかを 2 通りで測ったところ、Root accessory は空文字テキストでも `intrinsicContentSize` の高さ 0 の View でも layout が 22pt を割り当て、面積 0 にはならなかった (`placed=1 frames=[(0,0,375,22)] visible=1`、いずれも即時成立)。`expectedRootSupplementaryKinds` が出す kind は Root header / footer の 2 種だけなので、**現行の入力空間にこの経路は存在しない**。

### 3 の確認

`evidence/initial-render-predicate-detection.md` に実験 6 が追加され、(a) 修正前の可視外 Root Footer での deadline 超過、(b) 修正後の即時成立、(c) 置かれていない kind での不成立、の 3 点が数値付きで載っている。上記のとおり (b)(c) はレビュアー側で独立に再現した。まとめ表の層 2 の行も「過剰要求でないことは実験 6 で実測」へ更新されている。

## 新たな問題の確認

### `@MainActor` 付与 (`RootAccessoryThemeRefreshTests.swift:72` / `SectionAccessoryThemeRefreshTests.swift:74`)

いずれも `case` 2 つと `elementKind` の computed property だけを持つ private nested enum への型レベル注釈である。ネスト型は外側クラスの global actor 隔離を継承しないため注釈が要る一方、利用箇所 (`hasRootSupplementary` / `hasSupplementary` / `awaitLabelColor` 等) はすべて `@MainActor` のテストクラス内で、実行時の挙動・待機の粒度・アサーションの対象はいずれも変わらない。**警告を消すためだけの変更として妥当**。クリーンな `derivedDataPath` のフルビルドで、この 2 ファイルの警告は 0 件、リポジトリ全体のユニーク警告は **42 件でベースラインに一致**することを確認した。

### 問い合わせ範囲をコンテンツ矩形へ広げたコスト

ワーカーの「層 3 より重くなる」という自己申告は正しいが、待機ループの負荷としては問題にならない。`layoutAttributesForElements(in:)` の 1 回あたりの実測 (50 回平均):

| 行数 | contentH | content 範囲の attributes | 所要 | bounds 範囲の所要 |
|---:|---:|---:|---:|---:|
| 10 | 537.7 | 43 | 0.026 ms | 0.025 ms |
| 60 | 3126.1 | 243 | 0.119 ms | 0.026 ms |
| 300 | 15608.0 | 1203 | 0.536 ms | 0.025 ms |
| 1000 | 52013.9 | 4003 | 1.891 ms | 0.024 ms |

- このコストが発生するのは `requiredSupplementaryKinds` が非空のとき (= Root accessory を持つ構成) だけで、`awaitCollectionRender` の大半の呼び出しは 1 回も呼ばない
- 現行スイートの最大構成は 50 行 (`ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:373` 他)。ポーリング間隔 0.01 秒に対し 1 反復あたり 0.1 ms 台であり、無視できる
- 極端側 (1000 行 + Root H/F) でも `awaitInitialRender` 全体で 11.9 ms。層 2 が原因で待機が伸びる挙動は観測されなかった

判定を変える水準ではないため、Suggestion に留める。

## 指摘事項

### [🔵 Suggestion] `contentLayoutAttributes` が kind ごとに再計算される

**該当箇所**: `ios/Tests/KsSettingsViewTestSupport/CollectionRenderWait.swift:85-87`、`:179-185`

**問題点**: `isCollectionRendered` は `for kind in requiredSupplementaryKinds` の各周で `isRequiredSupplementaryReady` を呼び、その中で毎回 `contentLayoutAttributes` (コンテンツ全体への問い合わせ) を実行する。Root Header と Footer の両方を持つ構成では 1 反復あたり 2 回になる。`describeCollectionRender:179-185` も kind ごとに同じ呼び出しを繰り返す (こちらは失敗時のみ)。

上の実測どおり現行スイートでは実害が無いため、**判定には影響しない**。手を入れるなら、属性列を 1 度だけ取って kind の走査へ渡す形にすれば消える。

**推奨修正**: 任意。`isCollectionRendered` で `contentLayoutAttributes` を 1 度だけ評価し、`isRequiredSupplementaryReady` へ属性列を渡す。

### [🔵 Suggestion] `contentLayoutAttributes` の doc が「コンテンツサイズ未確定なら空になる」と読める

**該当箇所**: `ios/Tests/KsSettingsViewTestSupport/CollectionRenderWait.swift:121-122`

**問題点**: 「コンテンツサイズがまだ確定していない段階では空になり、必須 supplementary の判定は不成立になる」とあるが、問い合わせ範囲は可視矩形との和を取るため、コンテンツサイズが 0 でも範囲自体は空にならない (`CGRect(origin:.zero, size:.zero).union(CGRect(x:0, y:120, width:375, height:700))` = `(0, 0, 375, 820)` を実測)。実際に不成立になるのは「レイアウトがまだ attributes を返さない」からであって、範囲が空になるからではない。結論 (不成立になる) は正しいので実害は無いが、範囲が空になると読むと層 2 の成立条件を取り違える。

**推奨修正**: 任意。「レイアウトがまだ attributes を返さない段階では戻りが空になり、必須 supplementary の判定は不成立になる」のように、主語を戻り値に寄せる。

## 検証 (レビュアー側の再実行)

| 項目 | 結果 |
|---|---|
| 全件 Simulator 実行 (レビュー対象ツリー / クリーンな `derivedDataPath`) | Bridge 166 / Core 88 / SwiftUI 94 / TestSupport 7 / UITests 642 = **997 件・0 failures**、`** TEST SUCCEEDED **` |
| Swift 6 警告 | **ユニーク 42 件でベースラインに一致**。`RootAccessoryThemeRefreshTests.swift` / `SectionAccessoryThemeRefreshTests.swift` の警告は 0 件 |
| lint 3 本 | `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` いずれも 0 件 (`comment-policy-lint.py` は 685 ファイル検査)。追跡外の 2 本は規約本文で人手照合 |
| probe コードの残留 | `ios/` に検証用文字列 0 件 |
| 作業ツリーの状態 | `git status --short` 49 行 (本ファイル作成前)。レビュー開始時と一致 |
| 逆流検査 | `proposal.md` / `specs/ios-test-support/spec.md` / `triage.md` はいずれも未変更 |

デルタスペックの「setup ヘルパが内包する待機も初期反映の完了述語を待つ形にする SHALL」との整合も再確認した。層 2 の緩和後も、attributes の存在自体が「操作前には成立せず反映後に成立する遷移証拠」であることは上記 probe C で実測済みであり、review-003 の VALID 判定は維持される。

## 検証で行った一時変更と原状復帰

**レビュー対象のツリー (`ios/` および `kasane/`) には、本ファイルの作成以外の書き込みを一切行っていない。**

- 実測はいずれも `ios/` をスクラッチ領域へ複製し、その複製にのみ検証用テストファイルを追加して実行した。実行後に複製ごと `trash` で破棄した
- 全件実行はレビュー対象ツリーで行ったが、`derivedDataPath` はスクラッチ領域を指定した。作業ツリーに対する `git` の書き込み操作は行っていない

**復帰の確認方法**: `git status --short` が 49 行 (変更 38 件 + 未追跡 11 件) であること — 本ファイル作成後は未追跡 12 件 = 50 行になる。加えて `grep -rl "ZZRv004" ios/` と `grep -rl "ZZZERO" ios/` がいずれも 0 件であることで確認できる。

## アクションプラン

修正は不要。Suggestion 2 件はいずれも任意で、本 change で対応しても別 change へ送っても構わない。蒸留へ進んでよい。

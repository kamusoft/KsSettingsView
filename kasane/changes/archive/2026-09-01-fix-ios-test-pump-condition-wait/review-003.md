# レビュー結果: fix-ios-test-pump-condition-wait (003 回目)

**日付**: 2026-09-01
**判定**: CHANGES_REQUESTED
**範囲**: 修正サイクル 2 周目の差分に限定 (全体の再監査は review-001 / review-002 で完了済み)
**兼務**: verify — review-002 の ❌ 1 件は **解消 (VALID)**。ただし同じ述語に別系統の欠陥を新たに検出した (下記 Major)

## サマリー

review-002 の Major (Root accessory を待たない + 可視 Section 0 件で guard 素通り) は**解消されている**。`awaitCollectionRender` から早期 return の guard が消え、`requiredSupplementaryKinds` が追加され、`KsUITestWait` / `KsBridgeTestHost` の両ラッパが `controller.rootHeader` / `rootFooter` から kind を導出して渡すようになった。`SectionBoxDecorationTests.swift:524`, `:596` の経路は可視 Section 0 件でも Root header の実体化まで待つ。Suggestion 2 件 (`file`/`line` の引き回し、doc コメント) も実物で解消を確認した。

一方、新設された**層 2 は「その kind の supplementary が可視であること」を無条件に要求する**ため、レイアウト上は正常に存在するが可視矩形の外にある Root Footer (コンテンツが viewport より高い構成) では述語が永久に成立しない。複製ツリー上の実測で、deadline 3.0 秒を使い切って fail することを確認した。現行スイートに該当構成が無いため 997 件は緑のままだが、`awaitInitialRender` は Bridge の `attach` 67 箇所 + UITests の setup 17 箇所が通る共有経路であり、正当な入力クラスに対して決定的に落ちる待機を共有基盤に置くのは本 change の目的と逆行する。これを Major とした。

加えて、申し送りにあった Swift 6 警告を実測した。**本 change の変更が原因で新たに増えた警告が 4 件ある** (ベースライン 42 件 → 現在 46 件)。既存債務 42 件は本 change のスコープ外だが、4 件は本 change の責任である。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (コメント構文を持つ全ソース)。今周期で触られた 4 本の Swift ファイルの追加・変更コメントを規約本文の禁止類型で人手照合した |
| `kasane/handbook/cross/test-execution.md` | テストの実行と結果の報告 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 実行時挙動 (フレーム間タイミング) が絡む修正の完了判定 |
| `kasane/lessons/code-review.md` | 重点観点 L-001 (検出力の争点はミューテーションで実測) |

照合結果 (節ごと):

- **comment-policy** — 今周期で書き足された doc (`CollectionRenderWait.swift` の 3 層の説明、`KsUITestWait.swift` / `KsBridgeTestHost.swift` の `expectedRootSupplementaryKinds` の doc、`SectionBoxDecorationTests.swift:74-76`) を全行照合。change-id の裸参照 / Phase・Round・Decision 通番 / タスク通番 / アーカイブ文書のパス / 拡張子なし裸参照 は 0 件。デルタスペック構文キーワード (SHALL / MUST 等) の混入 0 件。**履歴記述も 0 件** — 「旧 pump では」「〜へ移行した」の類は無く、すべて現在形。`comment-policy-lint.py` は 0 件だが、規約が明記するとおり lint は履歴記述を検出しないため、追跡外の 2 本 (`CollectionRenderWait.swift` / `KsUITestWait.swift`) を含めて本文基準で人手照合した。**適合**。ただし `CollectionRenderWait.swift:30-32` の記述は実装と食い違う (下記 Major の一部)
- **test-execution** — 完了判定に絞り込みなしの全件 Simulator 実行を使っている。レビュアー側でも**クリーンな derivedDataPath** で再実行して再現した (下記)。**適合**
- **runtime-behavior-verification** — 述語の検出力をミューテーションで実測し、限界 (手元では差が出ない) を隠さず書く姿勢は維持されている。**適合**。ただし実験の設計に穴がある (下記 Suggestion)
- **lessons/code-review L-001** — 争点 (層 2 の穴の有無) をレビュアー側でスクラッチへ複製したツリー上で実測した。レビュー対象のツリーには**一切書き込んでいない** (復帰の確認方法は末尾)

## 前回指摘の解消状況

| # | 出典 / 重要度 | 指摘 | 状態 | 確認した実物 |
|---|---|---|---|---|
| 1 | review-002 🟠 Major / verify ❌ | 述語が Root accessory を待たず、可視 Section 0 件で guard 素通り | **解消** | `ios/Tests/KsSettingsViewTestSupport/CollectionRenderWait.swift` から早期 return の guard が消え、`requiredSupplementaryKinds` を層 2 として追加。`ios/Tests/KsSettingsViewUITests/KsUITestWait.swift:40-50` と `ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:79-92` が `controller.rootHeader` / `rootFooter` から kind を導出。`SectionBoxDecorationTests.swift:524` (`rootAccessoryContentFrame` を読む) と `:596` (`baseline = counter.count`) は Root header の実体化を待ってから戻る |
| 2 | review-002 🔵 Suggestion | `KsBridgeTestHost` が `file`/`line` を持たない | **解消** | `KsBridgeTestHost.swift:33-37` が `file: StaticString = #filePath, line: UInt = #line` を受け、`:50` から `awaitInitialRender` → `awaitCollectionRender` まで引き回す。既定値は `attach` の呼び出し側で評価されるため、失敗位置は各テストの `attach` 行を指す (誤りなし) |
| 3 | review-002 🔵 Suggestion | `hostWithRootAccessories` の doc が何を待つか書いていない | **解消** | `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:74-76` に「controller に設定済みの Root accessory の boundary supplementary が実体化するまで待つ」「可視 Section が 0 件の構成でもこの待機は効く」を明記 |

## 指摘事項

### [🟠 Major] 層 2 が「可視であること」を無条件に要求するため、画面外に出る Root Footer を持つ構成では述語が成立せず必ず fail する

**該当箇所**:
- `ios/Tests/KsSettingsViewTestSupport/CollectionRenderWait.swift:84-86` (層 2 の判定)、`:30-32` (実装と食い違う doc)
- `ios/Tests/KsSettingsViewUITests/KsUITestWait.swift:40-50` (`expectedRootSupplementaryKinds`)
- `ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:79-92` (同じ導出の複製)

**問題点**:

層 2 は `visibleSupplementaryViews(ofKind: kind).isEmpty == false` を成立条件にしている。一方で kind を渡す側 (`expectedRootSupplementaryKinds`) は `controller.rootHeader != nil` / `rootFooter != nil` だけを見て kind を並べる。**「設定されている」と「可視矩形に入る」は同値ではない。**

Root Footer は layout 全体の boundary supplementary で `alignment: .bottom` / `pinToVisibleBounds = false` (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:562-575`) であり、コンテンツと一緒にスクロールアウトする。コンテンツが viewport より高ければ初期表示時点で可視矩形の外にあり、`visibleSupplementaryViews` は空のままになる。**この状態は待っても解消しない** — 述語は deadline 3.0 秒を使い切り、`awaitCollectionRender` は setup の中で fail する。

レビュアー側でスクラッチへ複製したツリーに検証用テストを足して実測した (複製上での実測であり、レビュー対象のツリーは変更していない):

| 構成 | contentSize / bounds | 経過 | 失敗 | 可視 footer |
|---|---|---:|---:|---:|
| 60 行 + Root H + Root F | 3126.06 / 700.0 | **3.018 秒** | **1 件** | 0 |
| 2 行 + Root H + Root F | 153.67 / 700.0 | 0.001 秒 | 0 件 | 1 |

長いほうの失敗メッセージ (実測値):

```
条件が deadline 内に成立しなかった: 初期スナップショットの実描画
 (期待 Section 構造: [60], 必須 supplementary: ["ks-root-header", "ks-root-footer"])
 / 経過 3.011 秒 (deadline 3.000 秒)
 / 実測: Section 1 [0] 行 13/60 / 必須 supplementary [ks-root-header 1 件, ks-root-footer 0 件]
 / 未実体化の可視要素 なし
```

3 点を指摘する:

1. **仕様の自己矛盾**。`CollectionRenderWait.swift:30-32` は「表示領域の外にある要素と、面積を持たない要素は生成されないため待機の対象にしない」と書いている。層 3 (`visibleLayoutAttributes` の `intersects` フィルタ) はこの原則に従っているが、**層 2 だけが従っていない**。doc を信じて読むと層 2 の挙動は予測できない
2. **共有基盤としての射程**。`awaitInitialRender` は `KsBridgeTestHost.attach` (67 呼び出し) と UITests の setup ヘルパ 17 経路が通る唯一の初期反映待機である。Root Footer を持つ長いコンテンツは iOS の設定画面として何ら異常な構成ではなく、`bridge.updateAccessory(target: .rootFooter, ...)` を `attach` の前に呼ぶだけで到達する。将来のテスト作者は「3 秒待って `ks-root-footer 0 件`」という、自分のプロダクトコードが壊れているようにしか読めないメッセージに当たる
3. **手元の緑では検出できない類型そのもの**。現行スイートで Root accessory を `awaitInitialRender` 前に設定するのは `SectionBoxDecorationTests` の短いコンテンツだけなので 997 件は緑を維持する。evidence の実験 1・4 が指摘した非対称性 (待機を狭める欠陥は緑をすり抜ける) と同型で、今回は**待機を広げすぎた欠陥**が同じ理由ですり抜けている

**推奨修正** (いずれか):

- 層 2 の条件を「レイアウトがその kind の attributes を持つ」+「その attributes が可視矩形と交差するなら実体化済み」に変える。これなら (a) レイアウトがまだ kind を知らない段階は不成立のまま (層 2 の目的を維持)、(b) 画面外の footer は交差しないので成立、となり層 3 の原則とも揃う
- あるいは `expectedRootSupplementaryKinds` の導出側で「可視矩形に入ることが期待できる kind」だけを渡す (実質 Root Header のみ)。ただしこれは判断を呼び出し側へ移すだけなので、`awaitCollectionRender` の doc に層 2 の前提 (渡した kind は可視であることが前提) を明記すること
- どちらも過剰と判断する場合は、実測 (上表) を根拠に添えて `deviation.md` へ「層 2 は可視前提の kind にのみ使う」と記録し、`expectedRootSupplementaryKinds` の doc にその前提を書く

### [🟡 Minor] 本 change が Swift 6 言語モードの警告を 4 件新たに増やしている

**該当箇所**:
- `ios/Tests/KsSettingsViewUITests/RootAccessoryThemeRefreshTests.swift:78`, `:79` (`RootAccessorySlot.elementKind`)
- `ios/Tests/KsSettingsViewUITests/SectionAccessoryThemeRefreshTests.swift:80`, `:81` (`SectionAccessorySlot.elementKind`)

**問題点**:

申し送りの (a)(b) を実測で切り分けた。手順は「クリーンな `derivedDataPath` での現行ツリーのフルビルド」と「`git archive HEAD ios` で展開した変更前ツリーの `build-for-testing`」の 2 本を取り、`file:line:col + メッセージ` でユニーク化して差集合を取った。

- **(a) 実際の件数**: 報告された 33 件とは一致しない。ユニークで **46 件** (`xcodebuild` の生ログ上の `warning:` 行は 64 行、うち `mutation of captured var` はユニーク 24 件)。増分ビルドのキャッシュで見えていなかったという見立て自体は正しい
- **(b) 新規増分**: **ベースライン 42 件 → 現在 46 件で、差分は下記 4 件のみ**。既存債務側からは 1 件も消えていない

```
RootAccessoryThemeRefreshTests.swift: main actor-isolated class property 'rootHeaderElementKind'
  can not be referenced from a nonisolated context; this is an error in the Swift 6 language mode
RootAccessoryThemeRefreshTests.swift: main actor-isolated class property 'rootFooterElementKind'
  can not be referenced from a nonisolated context; this is an error in the Swift 6 language mode
SectionAccessoryThemeRefreshTests.swift: main actor-isolated class property 'elementKindSectionFooter'
  can not be referenced from a nonisolated context
SectionAccessoryThemeRefreshTests.swift: main actor-isolated class property 'elementKindSectionHeader'
  can not be referenced from a nonisolated context
```

4 件はいずれも本 change が旧 `pump` を置き換える過程で**新設した** `private enum RootAccessorySlot` / `private enum SectionAccessorySlot` の `var elementKind` に出ている (`git diff` で新規追加行であることを確認済み)。nonisolated な computed property から MainActor 隔離された static 定数を読んでいる。

したがって、**33 件のうち既存債務 (7 ファイルの `mutation of captured var` 系) は本 change のスコープ外**だが、上記 4 件は本 change が持ち込んだものであり本 change の責任である。`swift-tools-version: 5.10` のため現状はエラーにならないが、2 件は「Swift 6 言語モードではエラー」と明示されている。

**推奨修正**: 2 つの enum (または `elementKind` プロパティ) に `@MainActor` を付ける。数行で閉じる。

### [🔵 Suggestion] ミューテーション実験が層 2 の「必要性」しか測っておらず、「層 2 が課すコスト」を測っていない

**該当箇所**: `evidence/initial-render-predicate-detection.md` の実験 3〜5

**問題点**:

実験 3 (可視 Section 0 件の構成でレイアウトが attributes を置く) はレビュアー側でも独立に再現した — `sections=0 / attrCount=1 / kind=ks-root-header / frame=(0,0,375,20) / visibleHeader=1` まで一致する。実験 5 の「層 3 は kind を列挙せずレイアウトへ問い合わせるので Root accessory に到達する」も、実験 3 の観測から構造的に妥当である。実験 4 の「層 2 の必要性は手元の pass/fail では示せない」という限界の自己申告も正しく、隠していない点は評価する。

ただし**「層 3 が空振りする窓を層 2 が埋める」という主張は、その窓の存在を実測していない**。実験 3 が実際に示したのは「`layoutIfNeeded()` 直後には attributes が既に置かれている」であり、窓が開いている瞬間そのものは観測されていない。主張は構造からの推論であり、evidence もそう書いている — ここまでは誠実である。

問題は、実験がすべて「層 2 を外すと何を取りこぼすか」の方向だけを見ていて、**「層 2 を足すと何を過剰に要求してしまうか」を一度も試していない**ことである。上記 Major は、`requiredSupplementaryKinds` に渡した kind が成立し得ない構成 (画面外の Root Footer) を 1 つ試せば即座に出る。5 本の実験が全て「述語を弱める / 壊す」方向だったため、この方向の欠陥が漏れた。

**推奨修正**: Major の対応後に、実験を 1 本足す — 「必須 supplementary に指定した kind が可視矩形の外にある構成で、述語が成立するか」。成立するなら層 2 の条件が正しく定義できたことの実測になる。

## 検出力・証跡の検証 (レビュアー側の再実行)

| 項目 | 結果 |
|---|---|
| 全件 Simulator 実行 (クリーンな `derivedDataPath`) | Bridge 166 / Core 88 / SwiftUI 94 / TestSupport 7 / UITests 642 = **997 件・0 failures**、`** TEST SUCCEEDED **`。`evidence/full-suite-after-fix.md` の件数と一致 (所要時間は実行ごとに揺れる。UITests は 7.942 秒を観測) |
| 警告 | **46 件** (ユニーク)。review-002 の「warning 0 件」は誤り (増分ビルドのキャッシュによる)。本レビューで訂正する |
| lint 3 本 | `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` いずれも 0 件。追跡外の 2 本は規約本文で人手照合 |
| 旧パターンの残存 | `ios/Tests/` の `RunLoop.current.run` は `ConditionWait.swift:44` / `NegativeVerificationWait.swift:33` の 2 定義のみ。`func pump` / `pumpEntry` は 0 件 |
| ミューテーション残骸 | `ios/Tests/` に実験由来の文字列 0 件。今周期の差分は 4 本の Swift ファイルに閉じている |
| 逆流検査 | `proposal.md` / `specs/ios-test-support/spec.md` / `triage.md` はいずれも未変更 |

## 一致検証 (verify 兼務・限定範囲)

**判定**: **VALID** (review-002 の ❌ 1 件は解消)

| review-002 の ❌ | 現在の状態 |
|---|---|
| `awaitCollectionRender` が可視 Section 0 件の構成で待機せず戻り、Root accessory を kind として持たないため `SectionBoxDecorationTests.swift:478`, `:524`, `:596` の setup が「setup ヘルパが内包する待機も初期反映の完了述語を待つ形にする SHALL」を満たさない | **✅ 解消**。早期 return の guard は撤去済み。`:524` / `:596` は `hostWithRootAccessories` 経由で `requiredSupplementaryKinds: ["ks-root-header"]` を渡し、Root header の実体化まで待つ。`:478` は Root accessory を持たない `host` 経路で依然として RunLoop を回さずに戻るが、直後に読むのは `cv.contentInset` (レイアウトで確定) であり、その先の遷移は同ファイル `:513-518` の `awaitCondition` が待っている — review-002 が「実害なし」とした判断は現在も成立する |

**この節は前回 INVALID だった点の解消確認に絞った限定検証である。** 上記 Major は「述語が待たなすぎる」ではなく「待ちすぎて成立し得ない」欠陥であり、デルタスペックの Requirement / Scenario に対する不一致ではないため verify の ❌ には数えていない (品質欠陥として Major に計上した)。

## 検証で行った一時変更と原状復帰

**レビュー対象のツリー (`ios/` および `kasane/`) には、本ファイルの作成以外の書き込みを一切行っていない。**

- Major の実測は、レビュー対象ツリーの `ios/` をスクラッチ領域へ複製し、その複製にのみ検証用テストファイルを追加して実行した。実行後に複製ごと `trash` で破棄した
- ベースライン警告の測定は `git archive HEAD ios` で変更前ツリーをスクラッチ領域へ展開して行った。作業ツリーに対する `git` の書き込み操作 (checkout / stash / worktree 追加等) は行っていない
- 全件実行とベースラインビルドの `derivedDataPath` はスクラッチ領域を指定した

**復帰の確認方法**: `git status --short` の行数と内訳がレビュー開始時と一致すること (変更 38 件 + 未追跡 9 件 = 47 行。本レビュー結果ファイルの追加により、本ファイル作成後は未追跡 10 件 = 48 行になる)。加えて `ios/Tests/` 配下に `ZZReviewProbe` を含むファイルが存在しないこと (`grep -rn "ZZReviewProbe" ios/` が 0 件) で確認できる。

## アクションプラン

1. **[Major]** 層 2 の条件を「レイアウトが kind の attributes を持ち、それが可視矩形と交差するなら実体化済み」へ改める (`CollectionRenderWait.swift:84-86`)。あわせて `:30-32` の doc を実装と一致させる。前提を呼び出し側へ移す選択を取る場合は `expectedRootSupplementaryKinds` (`KsUITestWait.swift:40-50` / `KsBridgeTestHost.swift:79-92`) の doc に前提を明記し、`deviation.md` へ記録する
2. **[Minor]** `RootAccessorySlot` / `SectionAccessorySlot` に `@MainActor` を付け、本 change が持ち込んだ Swift 6 警告 4 件を消す。既存債務 42 件は本 change のスコープ外として据え置く (別 change で扱う)
3. **[Suggestion]** 1 の対応後、`evidence/initial-render-predicate-detection.md` に「必須 supplementary が可視矩形の外にある構成でも述語が成立する」実験を 1 本追加する
4. 1・2 の修正後、`ios` で全件 Simulator 実行を再度通す。件数は変わらない見込み。**警告件数 (期待値 42 件) も併せて記録する**と、以後の周回で同じ見落としが起きない

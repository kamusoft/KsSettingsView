# 初期反映の述語の検出力

`awaitCollectionRender` (`ios/Tests/KsSettingsViewTestSupport/CollectionRenderWait.swift`) の述語が実際に何を見ているかを、実装を一時的に壊して確かめた記録。実行日: 2026-09-01。Simulator は iPhone 17。

述語は 3 層でできている。以下の実験はこの層ごとの検出力を測る。

1. **Section 構造** — Section 数と Section ごとの行数
2. **必須 supplementary** — 呼び出し側が「あるはず」と渡した elementKind の attributes をレイアウトが置いていること (実体化を求めるのは可視矩形にかかる分だけ)
3. **レイアウトが置く要素の全走査** — `layoutAttributesForElements(in:)` が可視矩形へ返す attributes を kind に依らず走査し、Cell / supplementary の実体化を確かめる

## 実験 1: 述語を常に真にする (待機を無効化する)

`isCollectionRendered` の先頭で `return true` を返すように一時変更し、`KsSettingsViewBridgeTests` と `KsSettingsViewUITests` を実行した。

結果: **166 / 0 failures、642 / 0 failures。落ちない。**

手元の Simulator では、window へ載せて `layoutIfNeeded()` を呼んだ時点で初期反映が同期的に完了しており、待機は 1 度目の判定で抜けている。つまり**この環境では初期反映の待機の有無を pass / fail では判別できない**。待機が効くのは反映が遅れる環境 (実行機の混雑時) に限られ、それは evidence/ci-flaky-before-fix.md の CI 実測が示している状況にあたる。述語を広げた効果もこの実験では観測できない。

## 実験 2: 期待する Section 構造を 1 つ多くする (層 1)

`awaitInitialRender` が渡す `expectedItemCounts` の末尾に存在しない Section を 1 つ足し、`SectionBoxDecorationTests` を実行した。

結果: **54 件中、初期反映を通る全経路が deadline 超過で失敗した。**

失敗メッセージ (抜粋):

```
条件が deadline 内に成立しなかった: 初期スナップショットの実描画
 (期待 Section 構造: [1, 1], 必須 supplementary: ["ks-root-header"])
 / 経過 3.012 秒 (deadline 3.000 秒)
 / 実測: Section 1 [0] 行 1/1 / 必須 supplementary [ks-root-header 1 件] / 未実体化の可視要素 なし
```

確認できたこと:

- 述語はトートロジーではなく、成立しない構造では確実に deadline で落ちる (黙って戻らない)
- 実測値が Section ごとの行の実体化数・必須 supplementary の実体数・未実体化の可視要素の一覧を出す。テストが setup 直後に読む対象と、述語が見ている対象が同じであることをメッセージ上で確認できる
- 失敗メッセージの整形経路が実際に走ることも同時に確認できた (boundary supplementary の indexPath は要素が 1 つしかないため、`section` / `item` へ分解すると失敗時にだけ落ちる)

## 実験 3: 可視 Section 0 件 + Root accessory の構成で何が観測できるか (層 2 / 層 3 の前提)

`SettingsRoot(sections: [])` に Root Header を載せた controller を window へ置き、`layoutIfNeeded()` 直後にレイアウトへ問い合わせた。

結果:

```
sections=0 attrCount=1
attr cat=supplementaryView kind=ks-root-header path=[0] frame=(0.0, 0.0, 375.0, 20.0) resolved=true
visibleRootHeader=1
```

確認できたこと:

- 可視 Section が 0 件でも、レイアウトは `ks-root-header` の attributes を可視矩形へ置く。**「Section が無い構成には待つべき遷移が無い」は成り立たない**
- Root accessory の attributes は `layoutAttributesForElements(in:)` の戻りに含まれ、`supplementaryView(forElementKind:at:)` で実体を解決できる。層 3 の走査は Section に属さない kind もそのまま扱える
- boundary supplementary の indexPath は `[0]` (要素 1 つ) であり、Section 単位の supplementary の `[section, item]` とは形が違う
- 手元では `layoutIfNeeded()` 直後に既に実体化済みである (実験 1 と整合する)

## 実験 4: 必須 supplementary の層を外す (層 2 の検出力)

`awaitInitialRender` が渡す `requiredSupplementaryKinds` を空にし、`SectionBoxDecorationTests` を実行した。

結果: **54 / 0 failures。落ちない。**

Root accessory を待つのをやめても手元では pass / fail に出ない。**待機を狭める方向の欠陥は手元の緑では検出できない**という非対称性の実測であり、この change の完了判定を「全件緑」に依存させられない根拠にあたる。層 2 の必要性は実験 3 の「レイアウトがまだ attributes を置いていない段階では層 3 の走査が空振りする」という構造から来ており、pass / fail の差では示せない。

## 実験 5: 走査層だけで Root accessory を捕まえられるか (層 3 の検出力)

層 2 (必須 supplementary) を撤去したうえで、層 3 の走査が `ks-root-*` の kind を「未実体化」と判定するように一時変更し、`SectionBoxDecorationTests` を実行した。

結果: **54 件中 12 failures。Root accessory を載せる setup が全て deadline 超過で落ちた。**

失敗メッセージ (抜粋):

```
実測: Section 1 [0] 行 1/1 / 必須 supplementary [ks-root-header 1 件]
 / 未実体化の可視要素 ks-root-header@[0]
```

確認できたこと:

- 層 3 の走査は Root accessory の attributes に実際に到達しており、実体化していなければ述語は成立しない。**kind を列挙せずレイアウトへ問い合わせる形が、Root accessory を含む全 kind を覆っている**
- 失敗メッセージが未実体化の要素を kind + indexPath で名指しする

## 実験 6: 必須 supplementary が可視矩形の外にある構成 (層 2 が課すコスト)

Root Footer は layout 全体の boundary supplementary で下端揃え・非固定のため、コンテンツが可視矩形より高いと初期表示の時点で画面外にある。この構成で層 2 が成立するかを、`ios/` をスクラッチ領域へ複製したツリーに検証用テストを足して実測した (レビュー対象のツリーには書き込んでいない)。

構成は 1 Section・行数のみを変え、Root Header と Root Footer の両方を `awaitInitialRender` の前に設定する。可視矩形の高さは 700。

層 2 が「その kind の supplementary が可視であること」を無条件に要求する形での実測:

| 構成 | contentSize | footer の attributes | 可視矩形と交差 | 実体化 | 経過 | 結果 |
|---|---:|---:|---:|---:|---:|---|
| 60 行 + Root H/F | 3126.06 | 1 件 (y=3106) | 0 件 | 0 件 | **3.010 秒** | **失敗** |
| 2 行 + Root H/F | 153.67 | 1 件 (y=133.7) | 1 件 | 1 件 | 0.001 秒 | 成立 |

レイアウトは footer の attributes を置いているのに、画面外にあるため view が作られない。**待っても解消しない条件を待っており、deadline を使い切って落ちる。** 層 3 が `intersects` で画面外を対象から外しているのと食い違う。

層 2 を「レイアウトがその kind の attributes を置いている」+「可視矩形と交差する分は実体化済み」へ改めた後、同じ構成で再測した:

| 構成 | footer の attributes | 可視矩形と交差 | 実体化 | 経過 | 結果 |
|---|---:|---:|---:|---:|---|
| 60 行 + Root H/F | 1 件 (y=3106) | 0 件 | 0 件 | **0.001 秒** | 成立 |
| 2 行 + Root H/F | 1 件 (y=133.7) | 1 件 | 1 件 | 0.001 秒 | 成立 |

あわせて、条件を緩めた結果として層 2 の目的 (レイアウトがまだその kind の attributes を置いていない段階を不成立にする) が失われていないかを測った。Root Footer を設定していない controller に対し `ks-root-footer` を必須 supplementary として渡すと、レイアウトはこの kind の attributes を 1 件も置かない:

```
kind=ks-root-footer placed=0 intersectsBounds=0 visible=0
```

述語は成立せず deadline (1.000 秒) を使い切って落ちた。失敗メッセージ:

```
条件が deadline 内に成立しなかった: 置かれていない kind の待機
 (期待 Section 構造: [2], 必須 supplementary: ["ks-root-footer"])
 / 経過 1.008 秒 (deadline 1.000 秒)
 / 実測: Section 1 [0] 行 2/2 / 必須 supplementary [ks-root-footer レイアウト 0 件/実体化 0 件]
 / 未実体化の可視要素 なし
```

確認できたこと:

- **attributes が無い** (層 2 が埋める窓) と **attributes はあるが可視外** (層 3 が対象から外す状態) を区別できている。前者は不成立のまま、後者は成立する
- 失敗メッセージがレイアウト上の件数と実体化の件数を分けて出すため、どちらの状態で止まっているかがメッセージだけで判別できる

検証用テストと複製ツリーは実測後に `trash` で破棄し、`ios/` に残っていないことを文字列検索 0 件で確認した。

## まとめ

| 層 | 何を捕まえるか | 手元での検出力 |
|---|---|---|
| 1 Section 構造 | データソース未適用・行数不一致 | 実験 2 で実測 (落ちる) |
| 2 必須 supplementary | レイアウトが attributes を置く前の Root accessory | 実験 4 のとおり手元では差が出ない。必要性は実験 3 の構造から。過剰要求でないことは実験 6 で実測 |
| 3 全走査 | 可視領域の Cell / 全 kind の supplementary | 実験 5 で実測 (落ちる) |

述語がトートロジーでないこと・成立しないときに黙って戻らないことは実験 2 と実験 5 で担保した。述語を広げたことによる安定性の向上は、反映が遅れる環境でしか差が出ないため手元の pass / fail では示せない (実験 1・実験 4)。

すべての一時変更は実測後に元へ戻し、バックアップとの `diff` が差分なしであること・ミューテーション由来の文字列が `ios/Tests/` に 0 件であること・`git status` に想定外の未追跡ファイルが無いことを確認した。

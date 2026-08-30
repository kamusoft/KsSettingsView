# レビュー結果: perf-android-customcell-composition-reuse (003 回目)

**日付**: 2026-08-16
**判定**: APPROVED

## サマリー

review-002 の Major 2 件はいずれも解消している。(1) `verification-mutation.md` は提出コードの SHA (`404ca3bf…` / `937f71d7…` / `b634f1e0…`) で取り直され、現ツリーの `shasum` と 3 ファイルとも一致した。measure guard 除去 (g) に加え、高さ確保の 2 変異 (h) (i) と検出できなかった変異 (j) まで、落ちなかったものを含めて記録されている。(2) measure policy という設計判断と「再 bind 後 1 フレームの表示遅れ」は android/ADR-0015 の Decision / Alternatives / Consequences へ追記され、`deviation.md` にもオーナー合意 (2026-08-16) 付きで記録された。review-002 の Minor-1 (確保高さが `isFixedHeight` を見ない) は KDoc の精度を上げるだけでなく実装で解決され、専用の回帰テスト 2 件が付いた — second-opinion-code-002 の唯一の Suggestion もこれで満たされている。

全モジュール全件テストは成功している (`./gradlew test --rerun-tasks`: debug 1174 / release 1174、計 2348 件・失敗 0)。review-002 時点の 1172×2 からの +2×2 は新設の高さ確保テスト 2 件で説明がつく。実装ワーカーの作業中ログに `:ks-settingsview-ui:testDebugUnitTest` の `OutOfMemoryError` が残っていたため、提出ツリーで同じモジュール単独実行を独立に再現確認した (下記)。

(本文末尾に「追記: Minor 修正の照合」あり — 下記 Minor はオーケストレーターが同日にコメント修正で対応済み。照合の結果、判定は APPROVED のまま。ただし SHA 記録の持ち越しが 1 件生じている。)

新規の指摘は 🟡 Minor 1 件と 🔵 Suggestion 2 件。Minor は挙動ではなく**記録の正確さ**の問題で、新設テストのコメントと実装 KDoc が、同じ提出物である `verification-mutation.md` (j) の説明と正面から食い違っている (プローブを当てて実測で確定させた)。加えて review-002 の Minor-2 (ADR の「reset 時点で成立する」) と Suggestion 3 件は未対応のまま残っており、Minor-2 は蒸留での対応が前提になっているため取りこぼさないようアクションプランへ再掲する。Critical / Major はないため APPROVED とする。

## 前回指摘の対応状況

### review-002.md

| # | 指摘 (重要度) | 状態 | 確認内容 |
|---|---|---|---|
| 1 | 検出力記録が修正前ビルドに対するもの (🟠 Major) | ✅ 対応済み | `verification-mutation.md:12-16` の測定対象 SHA が `CustomCellViewHolder.kt` = `404ca3bf…` / `ComposeCellViewHolder.kt` = `937f71d7…` / `CustomCellPooledRebindMeasureTest.kt` = `b634f1e0…`。現ツリーの `shasum` と 3 ファイルとも一致。復帰後 SHA 表 (`:110-113`) も同値。変異は (a)〜(e) に加え、要求された (g) measure guard 除去 と、高さ確保の (h) (i)、検出できなかった (j) まで記録されている |
| 2 | measure policy と 1 フレーム遅延が決定層・提案に未記録 (🟠 Major) | ✅ 対応済み | android/ADR-0015 の Decision に measure policy の項 (`:25`)、Alternatives に却下案 (`SubcomposeLayout.subcompose` による同期再活性化、`:33`)、Consequences に 1 フレーム遅延 (`:43`) が追記され、出典に `verification-device.md` と 2026-08-16 オーナー承認が入っている。`deviation.md:4` にも「spec が規定しない領域」として記録され、記録先 (ADR) を明示している。記録内容と実装・実機観測 (850 ジェスチャ中 1 件・持続なし) は整合する |
| 3 | 非活性中の確保高さが `isFixedHeight` 非考慮 / KDoc が実態より強い (🟡 Minor) | ✅ 対応済み | `CustomCellViewHolder.kt:169-174` が `isFixedHeight` で分岐し、可変高さでは `maxOf(minHeight, lastContentHeightPx)` を使う。KDoc `:69-77` は「固定高さ…レイアウトは動かない / 可変高さ…新旧の行高さが違えばその 1 フレームだけ高さがずれる」へ精度が上がり、原理的に正確な値が得られない理由も書かれている。`CustomCellPooledRebindMeasureTest` に固定高さ / 可変高さの 2 件が追加され、(h) (i) で個別に検出力も確認済み |
| 4 | ADR-0015 の「reset 時点で成立する」が実装と食い違う (🟡 Minor) | ❌ 未対応 | `0015-…md:22` は「remember / DisposableEffect の破棄・購読停止…は reset 時点で成立する」のまま。同ファイルは本サイクルで編集されている (Decision / Consequences の追記) が、当該文は手つかず。review-002 の推奨自体が「蒸留時に改める」だったため新規 Major には上げないが、ADR が `accepted` へ昇格する前の必須事項として再掲する |
| 5 | GC テストの前提アサーションが本命より後 (🔵 Suggestion) | ❌ 未対応 | `CustomCellBuilderReleaseTest.kt:70-74` は `assertTrue(…, awaitCollected(reference))` が先、`assertFalse("前提: 対象 ViewHolder の Composition が破棄されていない", …)` が後のまま |
| 6 | 非活性ブランチの幅が `constraints.minWidth` 固定 (🔵 Suggestion) | ❌ 未対応 | `CustomCellViewHolder.kt:175` は `layout(constraints.minWidth, …)` のままで、EXACTLY 前提であることを示すコメントもない |
| 7 | `verification-device.md` のセッション数が節をまたいで食い違う (🔵 Suggestion) | ❌ 未対応 | `:102` 「有効セッション 23 件中 21 件で FATAL」/ `:220` 「初回検証で 21 セッション中 21 件発生していたもの」のまま |

### second-opinion-code-002.md (突き合わせで確定した 1 件)

| # | 指摘 (重要度) | 状態 | 確認内容 |
|---|---|---|---|
| A | 非活性期間の高さ確保を回帰テストで固定する (🔵 Suggestion) | ✅ 対応済み | `CustomCellPooledRebindMeasureTest.kt:132` `固定高さの行をプールから再 bind した直後は新しい Cell の高さが確保される` (A=120dp → B=72dp で B の高さを確保) と `:164` `可変高さの行をプールから再 bind した直後は確保される高さが最低高まで縮まない` (content 自然高 200dp が最低高 60dp まで縮まない) の 2 件。相方が指摘した「高さ 0 や古い Cell の高さを返しても通過する」穴は塞がっている |

## 確認した観点

- **ビルド・テスト**: `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL (4m12s)。`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の集計で debug 1174 / release 1174 (計 2348)、failures 0 / errors 0 / skipped 0 (test-execution 規約)。個別: `CustomCellPooledRebindMeasureTest` 3 / `CustomCellRecycleTest` 8 / `CustomCellBuilderReleaseTest` 1 / `CustomCellRenderingTest` 25 / `KsBridgeCustomCellDeactivateTest` 2 / `KsBridgeCustomCellTest` 20、いずれも失敗 0
- **`verification-mutation.md` の SHA 照合**: 記録の 3 ファイルすべてが現ツリーの `shasum` と一致 (Major-1 の解消根拠)。(a)〜(i) の変異再実行はレビュアー裁量の範囲として省略した — review-002 で提出コードに対する (a)(c)(d)(e)(g) の再現を独立実測済みであり、今回の記録はその範囲を含む上位互換になっている。一方 (j) については記録の説明自体に疑いを持ったため、独自にプローブを当てて実測した (下記 Minor)
- **原状復帰**: 本レビューで当てた 2 つのプローブは backup から復元し、`shasum` が `CustomCellPooledRebindMeasureTest.kt` = `b634f1e0…` / `CustomCellViewHolder.kt` = `404ca3bf…` / `ComposeCellViewHolder.kt` = `937f71d7…` へ戻ること、`git status` の変更ファイル集合がレビュー開始時と同一であること、復元後に対象テストが再び 3 件成功することを確認済み
- **足場アーティファクトの逆流**: なし。`git log -- kasane/changes/perf-android-customcell-composition-reuse/` は起案 `9804cbc` の 1 件のみ。`kasane/` 配下の未コミット差分は `tasks.md` のチェック更新 (16 行) と ADR-0015 の追記 (+3/-1) だけで、`proposal.md` / `specs/*/spec.md` / `exploration.md` / `second-opinion-spec-*.md` は無変更
- **ソースコメント規約** (cross/conventions/comment-policy.md): `python3 scripts/comment-policy-lint.py --summary` → 621 ファイル / 禁止 0 件
- **公開 API 変更**: なし (`internal` クラスの内部構成変更に閉じる)
- **実機証跡と提出コードの関係**: `verification-device.md` が再検証ビルドとして記録する `CustomCellViewHolder.kt` の SHA は `4ed1767a…` で、現ツリーの `404ca3bf…` とは異なる。差分は review-002 Minor-1 への対応 (非活性中の確保高さに `isFixedHeight` 分岐と `lastContentHeightPx` 下限を入れた) だけで、実機検証が観測対象にしていた経路 — measure guard 本体 (`if (!isContentComposed.value)`)・破棄戦略・deactivate/再 bind — は変わっていない。変わったのは「measure を見送る側で返す高さの値」であり、実機で唯一観測された注記 (C7 の 175px 一様帯・1 フレーム) をむしろ小さくする向きの変更である。したがって実機再検証は求めない。ただし**証跡の SHA が提出コードと一致しない状態ではある**ため、蒸留時に `verification-device.md` へ「その後 SHA は `404ca3bf…` へ変わったが、変更範囲は非活性時の確保高さに閉じる」旨の 1 行を足しておくと、後から辿る人が齟齬に突き当たらない
- **review-002 以降の差分の範囲**: ファイル更新時刻で確認したところ、review-002 (19:11) 以降に内容が変わったのは `CustomCellViewHolder.kt` / `CustomCellPooledRebindMeasureTest.kt` / `ADR-0015` / `deviation.md` / `verification-mutation.md` のみ。`ComposeCellViewHolder.kt` は更新時刻こそ新しいが SHA が review-002 記録値 (`937f71d7…`) と同一で、内容は変わっていない (変異の復元による touch)

### 確保高さロジックの妥当性 (依頼事項 3)

| 観点 | 判定 |
|---|---|
| `isFixedHeight` 分岐 (`:170-174`) | 妥当。固定高さの行は解決値がそのまま行高さなので、新しい Cell の解決値を確保するのが正しい。なお production の `RecyclerView` 経路では `applyEffectiveHeight` (`CellBaseLayout.kt:503-505`) が固定高さの行に `layoutParams.height = heightPx` を置くため親から EXACTLY 制約が来て、`constrainHeight` の時点でどのみち解決値へ丸められる。つまりこの分岐は「制約が EXACTLY で来ない場合」に効く防御であり、無害。テストが `UNSPECIFIED` で測っているのはその防御を直接観測するためで、(h) の検出力もそこに依存している |
| `lastContentHeightPx` 下限 (`:173`) | 妥当。可変高さの行では `applyEffectiveHeight` が `WRAP_CONTENT` を置くため制約は AT_MOST になり、確保値がそのまま行高さになる。最低高へ落とすと後続行がせり上がるので、直前の行高さを下限に置く判断は理にかなう。新旧の行高さが違えば逆に 1 フレームだけ高く出るが、その性質は KDoc `:74-77` に明記されている。ViewHolder ごとの値であり、活性測定のたびに更新されるので古い値が 2 フレーム以上残ることはない |
| snapshot state にしない選択 (`:126-128`) | 妥当。measure の中だけで読み書きし、値が変わるのはその測定結果が行高さへ反映された直後なので、state 化して再測定を誘発する必要がない |
| `isContentActive` 条件付き更新 (`:181-183`) | 実装としては妥当 (無害な防御)。ただし**この条件の根拠として書かれている説明が実態と食い違う** — 下記 Minor を参照 |

### tearDown の Composition 破棄の副作用 (依頼事項 4)

`CustomCellPooledRebindMeasureTest.kt:88-97` の追加分について確認した。

- **他テストへの影響**: なし。破棄対象はこのクラスが自分で作った `container` の子 `ComposeView` に限られる。`CustomCellRecycleTest` / `KsBridgeCustomCellDeactivateTest` の tearDown は従来どおり `frameDriver.stop()` のみで、変更されていない
- **`frameDriver.stop()` の後に破棄する順序**: 問題は出ていない。`Recomposer` を止めてから `disposeComposition()` を呼んでも、Composition の破棄は同期的に走り、全 3 件が緑
- **本来の目的 (OOM 抑止) の効き**: 実装ワーカーの作業ログには `:ks-settingsview-ui:testDebugUnitTest --rerun-tasks` が `OutOfMemoryError` で 31 件失敗した記録が 3 回分残っている (19:49 / 20:10 / 20:16 の各実行。いずれも本テストの現行版が固まる 20:24 より前)。失敗は毎回 `PickerSelectionSheetTest` 以降 (実行順の後半) に集中しており、前半で積み上がったヒープが枯れる形になっていた。提出ツリーで同じコマンドを 2 回連続で独立実行したところ、**2 回とも BUILD SUCCESSFUL・`OutOfMemoryError` 0 件** (2m11s / 2m01s)。全モジュール全件実行も成功しているため、この tearDown で解消していると判断してよい
- **弱点**: 破棄対象を「tearDown 時点の `container` の子」に限っているため、テストが `container.removeView(composeView)` と再 `addView` の**間**で失敗すると、その回の Composition は破棄されずに残る。3 件とも窓の中にアサーションがある (`:115-116` など) ため、失敗時には破棄漏れが起きる。失敗時に限られ件数も高々 1 件なので実害は小さいが、この tearDown の目的からすると取りこぼしになる → 🔵 Suggestion

## 指摘事項

### [🟡 Minor] `lastContentHeightPx` の更新条件の根拠説明が、同じ提出物の `verification-mutation.md` (j) と食い違う

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:123-125` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellPooledRebindMeasureTest.kt:180-181`

**問題点**:
実装の KDoc は条件の根拠をこう書いている。

> `[reset]` の後は content が空へ差し替わっており、非活性化が composition へ反映されるまでの測定を取り込むと、空の行の高さで下限が上書きされてしまう。

テストのコメントも同じ前提に立っている。

> プールへ入る瞬間の測定。content は空へ差し替わっているが非活性化はまだ composition へ届いていないため、この測定は空の行の高さになる。下限がこれで汚れてはいけない。

しかし `reset()` (`:260-267`) は `isContentActive.value = false` と `contentState.value = EMPTY_CELL_CONTENT` を**同一スナップショットへ続けて書く**。composition が片方だけを観測することはなく、次の再 composition で両方が同時に読まれ、`ReusableContentHost` はその場で非活性化される。したがって「content の差し替えは届いたが非活性化はまだ届いていない」という中間状態は生じない。`reset()` 直後の測定が見るのは**旧 content のノード**であり、空の行ではない。

これは推測ではなく、同じ提出物である `verification-mutation.md:71-73` が (j) を説明するために書いている内容そのものである。

> (`reset()` 直後に測ると、composition はまだ旧 content のノードを保持しているため、測定値は旧 content の高さになる)

(j) が検出できない理由もここにある。条件を外しても `lastContentHeightPx` に入るのは旧 content の高さ (= 条件ありのときと同じ値) なので、テストの結果が変わらない。つまり `CustomCellPooledRebindMeasureTest.kt:182` の `remeasureRow` は、コメントが主張する「空の行で下限が汚れる経路」を通していない。

**実測で確認した** (lessons code-review L-001)。2 つのプローブを当てた。実行はいずれも `:ks-settingsview-ui:testDebugUnitTest --tests '*CustomCellPooledRebindMeasureTest*' --rerun-tasks`。

| プローブ | 結果 | 意味 |
|---|---|---|
| `remeasureRow(composeView)` の直後に `assertEquals(pxOf(TALL_CONTENT_HEIGHT_DP), composeView.measuredHeight)` を一時的に挿入 | **成功** (3 件・失敗 0) | その測定値は 200dp 相当 = **旧 content の高さ**。空の行なら最低高 60dp になるはずで、コメントの「この測定は空の行の高さになる」は成立しない |
| 当該コメントと `remeasureRow(composeView)` の 1 行を一時的に削除 | **成功** (3 件・失敗 0) | この行は下限に影響を与えておらず、テストが通す経路も変わらない |

使用した一時変更は backup から復元し、`shasum` が `b634f1e07e17f7c6ea60dbcb4c4287f33d0bf53a` へ戻ること、および復元後に同テストが再び 3 件成功することを確認済み。`git status` の変更ファイル集合もレビュー開始時と同一。

実害は挙動ではなく記録の側にある。この条件はテストで守られていない (それ自体は (j) として明示され、実観測への防御として残す判断も妥当) 以上、**残す根拠は文書だけ**である。その文書が機構を取り違えていると、次に読む人が「テストは通るし説明も違う」と判断して条件を落としかねない。

**推奨修正**: KDoc とテストコメントを、`verification-mutation.md` (j) の説明と同じ機構に揃える。具体的には (1) `reset()` 直後の測定は旧 content の高さになるので下限を汚さないこと、(2) この条件が防いでいるのは「非活性化の要求後に空の content が測られる」時間窓であり、それはヘルパが描画パス経由で measure を走らせていた版で実際に観測された (`expected:<200> but was:<60>`) が現在のテストからは決め打ちで作れないこと、を書く。`CustomCellPooledRebindMeasureTest.kt:182` の `remeasureRow` を残すなら「この測定は旧 content を見るので下限は変わらない (経路をなぞるだけ)」と実態どおりに書く。

### [🔵 Suggestion] tearDown の破棄対象が「そのとき container の子であるもの」に限られる

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellPooledRebindMeasureTest.kt:93-96`

**問題点**: 3 件とも `container.removeView(composeView)` から再 `addView` までの間にアサーションを置いており (`:115-116`, `:146-151`, `:179-186`)、その窓で失敗すると `ComposeView` は tearDown 時に子ではないため `disposeComposition()` が呼ばれない。この tearDown が防ごうとしている「Recomposer と snapshot の監視の積み上がり」が、まさに失敗時に取りこぼされる。

**推奨修正**: 生成した `CustomCellViewHolder` をフィールドかリストで保持し、tearDown ではそちらを破棄対象にする (親から外れていても `disposeComposition()` は呼べる)。

### [🔵 Suggestion] 非活性ブランチの確保高さが親制約より大きくなり得るケースの扱いが読み取りにくい

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:169-175`

**問題点**: `maxOf(minHeight, lastContentHeightPx)` は `constrainHeight` で親制約へ丸められるので安全だが、丸められた結果は「直前の行高さでも最低高でもない値」になり得る。コメントは「行が縮む向きのずれは起こさないようにする」と書いており、制約で丸められた場合はその保証が成立しないことが読み取れない。実害はない (制約が上限を決めている以上その高さしか取れない) ものの、不変条件として読まれると誤りになる。

**推奨修正**: 「親制約に収まる範囲で」という限定を 1 語足す。前段の Minor の書き換えとまとめて対応できる。

## アクションプラン

1. **[蒸留必須]** android/ADR-0015 の Decision 第 3 項「remember / DisposableEffect の破棄・購読停止…は reset 時点で成立する」を実装に合わせて改める (review-002 Minor-2 の持ち越し)。`accepted` へ昇格させる前に必ず通す
2. [Minor] `CustomCellViewHolder.kt:123-125` と `CustomCellPooledRebindMeasureTest.kt:180-181` の説明を `verification-mutation.md` (j) の機構へ揃える
3. [Suggestion] tearDown の破棄対象を「生成した ViewHolder」に変える / 確保高さコメントに親制約の限定を足す / review-002 の残 Suggestion 3 件 (GC テストのアサーション順・非活性ブランチの幅の意図明示・`verification-device.md` のセッション数 23 と 21 の食い違い) — まとめて対応可
4. [蒸留時] `verification-device.md` に、記録した再検証ビルドの SHA (`4ed1767a…`) と提出コード (`404ca3bf…`) の差が非活性時の確保高さに閉じる旨を 1 行足す

---

## 追記: Minor 修正の照合 (同日)

上記 Minor に対し、オーケストレーターが 2 箇所のコメントを直接修正した。修正後のツリーを照合した結果を記す。**判定は APPROVED のまま変わらない。**

### 修正内容の照合

| 箇所 | 照合結果 |
|---|---|
| `CustomCellViewHolder.kt` の `lastContentHeightPx` KDoc | ✅ 指摘の趣旨どおり。「[reset] は空 content 化と非活性化を同一スナップショットに書くため両者は composition へ揃って届く — 反映前の測定が観測するのは旧 content の高さであり、反映後は非活性分岐が content を測らない」は、本レビューのプローブで実測した内容と一致する。続く「現行経路でこの条件が効く瞬間はなく、活性を要求していない間の測定を下限に取り込まないための防御として置いている」も、(j) に検出力がないこととの整合が取れており、条件を残す位置づけが正直に書かれている。`verification-mutation.md` (j) の「実際に観測した失敗への対処」とは、観測が現在は存在しないテストヘルパ経路でのものだったという点で両立する (矛盾しない) |
| `CustomCellPooledRebindMeasureTest.kt:180-182` のコメント | ✅ 指摘の趣旨どおり。「反映前のこの測定が観測するのは A の旧 content の高さ (空の行にはならない)」はプローブの実測値 (200dp 相当 = A の content 高) と一致する。「この測定を挟んでも B の確保高さが崩れないことを後段で確認する」へ書き換えたことで、この 1 行が「空の行で下限が汚れる経路」を通すという誤った読みも解消している |

### 副作用の確認

- **コード・アサーションの無変更**: テストファイルはレビュー開始時の backup と `diff` して**差分がコメント 2 行 → 3 行の置換のみ**であることを確認。`CustomCellViewHolder.kt` も measure policy の両分岐と `reset()` の本体が本レビュー時点と同一であることを確認した
- **テスト**: `:ks-settingsview-ui:testDebugUnitTest --tests '*CustomCellPooledRebindMeasureTest*' --rerun-tasks` → BUILD SUCCESSFUL、`tests="3" failures="0"`
- **ソースコメント規約**: `comment-policy-lint.py --summary` → 621 ファイル / 禁止 0 件

### この修正が生んだ新しい持ち越し (要対応)

コメントのみとはいえファイル内容が変わったため、**`verification-mutation.md` に記録された SHA が現ツリーと一致しなくなった**。

| ファイル | `verification-mutation.md` の記録 | 現ツリー |
|---|---|---|
| `CustomCellViewHolder.kt` | `404ca3bfa0ea6e02502cc3e7f551475e9d0a9608` | `2b96cee1928dd59b4198ba2d5a80057c93f1ad03` |
| `CustomCellPooledRebindMeasureTest.kt` | `b634f1e07e17f7c6ea60dbcb4c4287f33d0bf53a` | `6631f21f1cf771f651cdaf4b5e601b45f12be0a1` |

差はコメントだけなので (a)〜(j) の測定結果は有効であり、Major-1 が再発したとは扱わない。ただし**放置すると review-002 Major-1 とまったく同じ齟齬 (証跡の SHA が提出物と合わない) が残る**ため、アーカイブ前に `verification-mutation.md` へ 1 行足すこと — 「その後、review-003 Minor 対応でコメントのみ修正し SHA は `2b96cee1…` / `6631f21f…` へ変わった (変異結果に影響なし)」。アクションプランの 5 番目として扱う。

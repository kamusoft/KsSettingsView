# レビュー結果: perf-android-customcell-composition-reuse (002 回目)

**日付**: 2026-08-16
**判定**: CHANGES_REQUESTED

## サマリー

修正サイクルで実機検証 (tasks 3.1) が実施され、その過程で発見された FATAL (`measure is called on a deactivated node`) に対する修正 — 宣言ツリーを `Layout` で包み、非活性が composition に反映されている間は content を measure しない measure policy — が入った。この修正はデルタスペックの全 Requirement / Scenario と整合しており、専用の回帰テスト (`CustomCellPooledRebindMeasureTest`) も新設されている。review-001 の全指摘と second-opinion-code-001 で採用された 2 件も、いずれも対応済みであることを確認した。全モジュール全件テストも成功している (`./gradlew test --rerun-tasks`: debug 1172 / release 1172、計 2344 件・失敗 0)。

一方で **2 件の Major を新規に検出した**。いずれもコードの欠陥ではなく、**証跡と決定層が提出コードに追随していない**問題である。(1) tasks 2.7 の検出力記録 (`verification-mutation.md`) はクラッシュ修正**前**のビルド (SHA `0946afdf…`) に対する測定であり、提出コード (SHA `4ed1767a…`) の証跡になっていない。特に修正の中核である measure guard の検出力は一切記録されていない。(2) その measure policy という設計判断と、その帰結である「プール由来の再 bind で content の表示が最大 1 フレーム遅れる」という新しい挙動が、android/ADR-0015 にも proposal の Impact にも deviation.md にも記録されていない。

(1) については本レビューで独立にミューテーションを当て直し、**提出コードでも検出力が成立していること (新設の guard を含む) を実測で確認した** ため、実装の修正は不要で記録の更新だけで解消できる。(2) は記録先の選択にオーナー判断が要る。

## 確認した観点

- **ビルド・テスト**: `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL (3m55s)。`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の集計で debug 1172 / release 1172、failures 0 / errors 0 / skipped 0 (test-execution 規約)。review-001 時点の 1171 からの +1 は新設 `CustomCellPooledRebindMeasureTest` の 1 件で説明がつく。個別確認: `CustomCellRecycleTest` 8 / `CustomCellPooledRebindMeasureTest` 1 / `CustomCellBuilderReleaseTest` 1 / `KsBridgeCustomCellDeactivateTest` 2 / `CustomCellRenderingTest` 25 / `KsBridgeCustomCellTest` 20、いずれも失敗 0
- **足場アーティファクトの逆流**: なし。`git log` 上 change 配下のコミットは起案 `9804cbc` のみで、未コミット差分も `tasks.md` のチェック更新だけ (`proposal.md` / `specs/*/spec.md` / `exploration.md` / `second-opinion-spec-*.md` は無変更)
- **ソースコメント規約** (cross/conventions/comment-policy.md): `python3 scripts/comment-policy-lint.py --summary` → 621 ファイル / 禁止 0 件。追跡外の新規テスト 4 ファイルも個別に検査し禁止参照なし。ADR 参照はすべて許容形式 (`android/ADR-0015`)
- **公開 API 変更**: なし (`internal` クラスの内部構成変更に閉じる)
- **実行時挙動の検証規約** (cross/conventions/runtime-behavior-verification.md): 「修正前に実環境で再現 → 修正後に同一手順で解消確認 → 証跡を change 配下に残す」の 3 点をいずれも満たす (下記「実行時証跡の実在と対応」)

### 実行時証跡の実在と対応

| 確認 | 結果 |
|---|---|
| `verification-device.md` の実在 | ✅ 初回 (NG) と再検証 (問題なし) の 2 回分が時系列で記録されている |
| 記録された修正後ビルドの実装ソース SHA | `4ed1767afd785feb3203c315e28f6a58c12650a7` — **現ツリーの `CustomCellViewHolder.kt` と一致** (`shasum` で確認)。再検証が提出コードそのものに対して行われたことの裏付けになる |
| 参照されている証跡ファイルの実在 | ✅ `logs/crash-traces.txt` (26KB) / `logs/session-emu-api35-fix.log` / `logs/session-pixel6a-api36-fix.log` / `logs/anr-during-verification.txt` / スクリーンショット 5 点 + 録画 1 点がすべて実在 |
| セッションログと本文の整合 | ✅ 実機ログ末尾「ジェスチャ総数 267 / 検査点 60 / NG 0 / ANR 0 / 注記 1 (C7 の単発一様帯)」は本文の表と一致。エミュ側も同様 |
| `verification-mutation.md` の実在 | ✅ 実在。ただし記録されたソース SHA が提出コードと不一致 (Major-1) |

### 回帰検出力の再実測 (lessons code-review L-001)

`verification-mutation.md` の測定が修正前ビルドに対するものだったため、**提出コード (SHA `4ed1767a…`) に対して独立にミューテーションを当て直した**。実行対象は `:ks-settingsview-ui:testDebugUnitTest --tests '*CustomCell*'` (50 件) と `:ks-settingsview-bridge:testDebugUnitTest --tests '*KsBridgeCustomCell*'` (22 件)。変異なしでは両方 BUILD SUCCESSFUL。

| ミューテーション | 落ちたテスト |
|---|---|
| **(g) 新規**: measure policy の `if (!isContentComposed.value)` を `if (false)` にして常に子を測る | `CustomCellPooledRebindMeasureTest > プールで非活性化された行を再 bind した直後の measure が失敗しない` (1 件)。失敗理由は `java.lang.IllegalArgumentException: measure is called on a deactivated node` — 実機で観測された FATAL と同一の例外を再現しており、テストが実機の症状そのものを固定していることが確認できた |
| (a) 破棄戦略を `DisposeOnDetachedFromWindow` へ戻す | `CustomCellRecycleTest` の 3 件 (プール生存 / cache 経由の継続 / 埋め込み View の再利用) + `CustomCellPooledRebindMeasureTest` 1 件 + `KsBridgeCustomCellDeactivateTest > リサイクルを挟んだ再表示で同一 platform view が再親付けされる` (計 5 件) |
| (c) `contentKey.value = cell.id` を固定値へ | `CustomCellRecycleTest > 間に再 composition を挟まない再 bind でも remember が持ち越されない` (1 件) |
| (d) `reset()` から `isContentActive.value = false` を削除 | `CustomCellRecycleTest > 同一ラップ関数 builder 間で埋め込み View が再利用される` (1 件) |
| (e) `reset()` から `contentState.value = EMPTY_CELL_CONTENT` を削除 | `CustomCellBuilderReleaseTest > Composition 破棄後は builder が参照するものが解放可能になる` (1 件) |

いずれも前提アサーション (「前提: 〜」) は通過し、争点のアサーションだけが落ちた。**提出コードでも検出力は成立している**。使用した一時変更は backup からの復元後 `shasum` 一致 (`CustomCellViewHolder.kt` = `4ed1767a…` / `ComposeCellViewHolder.kt` = `937f71d7…`) と全 CustomCell 系テストの再 green で原状復帰を確認済み。`git status` の変更ファイル集合もレビュー開始時と同一。

### クラッシュ修正の妥当性 (依頼事項 1)

`Layout` の measure policy でデルタスペックの各 Requirement を破っていないか、新たな穴がないかを個別に確認した。

| 観点 | 判定 |
|---|---|
| プール生存と破棄境界 | 影響なし。破棄戦略は `ComposeCellViewHolder` 側で変わっていない。`CustomCellRecycleTest` 4 経路すべて green |
| itemViewCache 経由の継続 | 影響なし。cache 滞在の行は `reset()` を通らず `isContentActive` が `true` のままなので、`isContentComposed` も `true` を保つ |
| ノードツリーの再利用 | 影響なし。`ReusableContent` は `Layout` の content 側にそのまま入っており、reuse 時 (key 変更) はノードが活性のまま引き継がれる。`isContentComposed` は `onDispose` → 新 `DisposableEffect` の順で同一 apply フェーズ内に false→true と遷移するため、measure が非活性を誤認する窓は開かない |
| 行間隔離 | 影響なし。隔離は `contentKey` / 非活性化が担っており、measure policy は関与しない |
| reset による参照切断 | 影響なし。`contentState` の切り離しは維持されている |
| **高さ確保の挙動** | ⚠️ Minor-1。`isFixedHeight` を見ずに `heightDp` だけを確保するため、非固定高さかつ content の自然高が最低高を超える行では、再活性化が反映されるまでの 1 フレームだけ行が縮む |
| **isFixedHeight との関係** | 上記のとおり。活性時のブランチは `constrainWidth` / `constrainHeight` のみで、`isFixedHeight` の解決は従来どおり `CustomCellRow` の `height` / `heightIn` が担っており変わっていない |
| **空 content 時の measure** | 問題なし。`EMPTY_CELL_CONTENT` と非活性は `reset()` で同時に設定され、`bind` 前に活性化されることはない。未 bind の ViewHolder は `heightDp = 0` で高さ 0 を返すだけ |

### Bridge の Composition プール生存の裏取り (依頼事項 3)

「bridge ではプール投入時に `hasComposition == false` だった」という未確定の観測報告を検証するため、**bridge モジュールに一時的な診断テストを置いて実測した** (実測後に削除済み。`git status` で追跡外ファイルが増えていないことを確認)。

`setItemViewCacheSize(0)` で `KsBridgeCustomCell` の行を端まで送った直後の状態:

```
PROBE: before scroll hasComposition=true attached=true
PROBE: after scroll  hasComposition=true attached=false parent=null
PROBE: poolCount(viewType=1)=1
PROBE: holderAt(1)=null
```

- 行は adapter position から消え (`holderAt(1)=null`)、`ComposeView` は window から外れ (`attached=false`) parent も切れており、**`RecycledViewPool` に実際に 1 件入っている** (`poolCount=1`)
- その状態で `hasComposition == true` — **本変更の利得 (Composition のプール生存) は bridge 経路でも成立している**
- さらにミューテーション (a) で `KsBridgeCustomCellDeactivateTest` が落ちることを再現した。旧戦略なら同じ地点で `hasComposition == false` になるということであり、このテストが「プール滞在 = pooling container 内側での window detach」という実経路を確かに観測している証拠になる

したがって **矛盾は再現せず、テストが観測している経路と実挙動の乖離は検出できなかった**。報告された観測は、位置指定ジャンプ (`scrollToPosition`) のように行が作り直される別経路、または修正前ビルドでの観測だった可能性が高い (`KsBridgeCustomCellDeactivateTest` の KDoc 冒頭がまさにこの経路差を説明している)。

## 前回指摘の対応状況

### review-001.md

| # | 指摘 (重要度) | 状態 | 確認内容 |
|---|---|---|---|
| 1 | 実機検証 (tasks 3.1) 未実施 (🟠 Major) | ✅ 対応済み | `verification-device.md` を新設。初回検証で FATAL を再現 (有効 23 セッション中 21 件) → measure policy 修正 → 再検証で両端末計 850 ジェスチャ / 190 検査点を完走し、空行 (持続) 0 / 混線 0 / FATAL 0。証跡ファイルもすべて実在。記録された修正後 SHA が現ツリーと一致 |
| 2 | tasks 2.7 の結果記録が存在しない (🟡 Minor) | ⚠️ 部分対応 | `verification-mutation.md` を新設。ただし記録が修正前ビルド (`0946afdf…`) に対するもので提出コードの証跡になっていない → **Major-1 として再指摘** |
| 3 | `ComposeFrameDriver.kt` の 2 モジュール重複 (🟡 Minor) | ✅ 対応済み | 指摘の代替案どおり、両ファイルの KDoc に「# もう一方のコピーとの同期」節を追加し「差は `package` 宣言だけ」「片方だけを変更してはいけない」を明記。`diff <(tail -n +2 …) <(tail -n +2 …)` で 2 ファイルが package 行以外完全一致であることも確認 |
| 4 | `bind()` / `reset()` の KDoc が実タイミングより強く読める (🟡 Minor) | ✅ 対応済み | `bind` (`:167-168`) は「代入の順序は結果に影響しない」に、`reset` (`:212-219`) は「実際に破棄される…のは非活性化が再 composition に観測された時点であり、この関数の戻り時点ではない」「同一レイアウトパス経路では非活性は一度も観測されず、隔離は同一性キーが担う」に書き換え済み |
| 5 | 二重否定のアサーション (🔵 Suggestion) | ✅ 対応済み | `KsBridgeCustomCellDeactivateTest.kt:160` が `assertTrue(…, kept.isAttachedToWindow)` になっている |
| 6 | `Int` 状態は `mutableIntStateOf` (🔵 Suggestion) | ✅ 対応済み | `CustomCellViewHolder.kt:97` が `mutableIntStateOf(0)`、読み出しは `intValue` |
| 7 | `frame()` が打ち切りを黙って握り潰す (🔵 Suggestion) | ✅ 対応済み | `ComposeFrameDriver.kt:81-83` で `check(!recomposer.hasPendingWork)`。KDoc にも理由 (未反映を「変化がない」と読み違えるのを防ぐ) を明記 |
| 8 | 余分な空行 (🔵 Suggestion) | ✅ 対応済み | `CustomCellRecycleTest.kt` の `companion object` 末尾の空行は解消 |

### second-opinion-code-001.md (突き合わせで採用された 2 件)

| # | 指摘 (重要度) | 状態 | 確認内容 |
|---|---|---|---|
| A | GC テストが ViewHolder の builder 参照切断を検出できない (🟡 Minor) | ✅ 対応済み | `CustomCellBuilderReleaseTest` が判定対象の ViewHolder を戻り値で保持し、`Reference.reachabilityFence(holder)` で判定終了まで到達可能に保つ形へ変更。`hasComposition == false` の前提アサーションも追加。相方が求めた「builder 切断行を除くミューテーションでの失敗確認」も、本レビューの (e) で提出コードに対して再現済み |
| B | FrameDriver が未収束を黙って受け入れる (🟡 Minor) | ✅ 対応済み | 上記 7 と同一 (両コピーとも修正済み) |

## 指摘事項

### [🟠 Major] 検出力記録 (tasks 2.7) がクラッシュ修正前のビルドに対するもので、提出コードの証跡になっていない

**該当箇所**: `kasane/changes/perf-android-customcell-composition-reuse/verification-mutation.md:88-95` (原状復帰の SHA 表)

**問題点**:
記録は「変異前 / 復帰後の SHA-1」として `CustomCellViewHolder.kt` = `0946afdf18a04411d6df804c93d629f0026f7b08` を挙げているが、提出コードの同ファイルは `4ed1767afd785feb3203c315e28f6a58c12650a7` である。`0946afdf…` は `verification-device.md` の**初回検証 (FATAL が出た修正前ビルド)** の SHA として記録されている値そのものであり、tasks 2.7 の測定は measure policy の修正**前**に行われたことになる。

これは単なる日付のずれではない。修正で追加された `Layout` の measure policy と `isContentComposed` gate は、content が measure されるかどうかを制御する新しい分岐であり、既存テストの通過経路に直接影響し得る。にもかかわらず:

- (a)〜(e) の再現性が**修正後の構造でも成立するか**が記録されていない
- 修正の中核である **guard 自体の検出力が 1 つも記録されていない** (「guard を外したら本当にテストが落ちるのか」= `CustomCellPooledRebindMeasureTest` がトートロジーでないかが未証明)

記録が「復帰後にこの SHA へ戻ることを確認した」と書いている以上、読み手はその SHA が提出物だと受け取る。蒸留時やアーカイブ後に証跡を辿ると齟齬に突き当たる。

**推奨修正**: `verification-mutation.md` を提出コード (`4ed1767a…`) に対して取り直し、guard 除去のミューテーションを (g) として追加する。本レビューの「回帰検出力の再実測」表がそのまま使えるので、実測をやり直さず流用してよい。

### [🟠 Major] measure policy という設計判断と「再 bind 後 1 フレームの表示遅れ」が決定層・提案のどこにも記録されていない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:52-72`, `:117-157` / `kasane/decisions/android/0015-customcell-pool-aware-composition-disposal.md` / `kasane/changes/perf-android-customcell-composition-reuse/proposal.md` の Impact

**問題点**:
実装は「`ReusableContentHost` を `setContent` 直下に置けない」という Compose の制約に対して、**`SubcomposeLayout` の measure policy が担っている「非活性の slot を測らない」責務を自前の `Layout` で持つ**という解を採っている。これは ADR-0015 の Decision に書かれた 5 項目のどれにも含まれない、実装中に生まれた設計判断である。tasks.md にも対応するタスクがない (1.2 の「再構成する」に包含されると読むには具体性が違いすぎる)。

さらにこの解には、記録されていない挙動の帰結がある: **プールで非活性化された行を再 bind したとき、content が現れるのは再活性化が composition へ反映された後 (通常は次のフレーム) になり、その間は行の高さだけが確保された空の行が表示され得る**。KDoc (`:69-72`) にはこの性質が書かれているが、

- proposal の Impact は挙動変化を (1)〜(5) と列挙しているのに、この 6 番目が入っていない
- ADR-0015 の Consequences にも無い
- `deviation.md` も存在しない

実際、`verification-device.md` の再検証で実機に 1 件 (C7 の 175px 一様帯・1 フレーム) 観測されており、判定ドライバにも「持続しなければ NG としない」という**この性質を前提にした判定基準**がわざわざ追加されている。つまり検証手順を変えるほどの実挙動でありながら、長命層にも提案にも痕跡が残らない状態になっている。

デルタスペックのどの Requirement にも違反していないため verify 上の ❌ ではないが、**この判断と挙動を蒸留で拾い損ねると、次に同じ箇所を触る人が「なぜ Layout で包んでいるのか」「なぜ 1 フレーム遅れるのか」を再導出する羽目になる**。ADR-0015 が `proposed` のうちに直せる。

**推奨修正**: 記録先の選択はオーナー/オーケストレーター判断が要るため、次のいずれかを選んで確定する。

1. android/ADR-0015 の Decision に「非活性の間 content を測らない measure policy を ViewHolder 側で持つ」を追記し、Consequences に「プール由来の再 bind で content の表示が最大 1 フレーム遅れる (行高さは確保されるため持続的な空行にはならない)」を追記する (推奨)
2. 実装中に生じた仕様外の追加として `deviation.md` に記録し、蒸留で ADR へ昇格させる

### [🟡 Minor] 非活性中に確保する高さが `isFixedHeight` を考慮しておらず、KDoc が実態より強い

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:143-144` と `:69-72`

**問題点**:
確保高さは `heightDpState.intValue.dp.roundToPx()` の一択である。しかし `heightDp` の意味は `isFixedHeight` で変わり、`false` (= `Theme.hasUnevenRows == true`) のときは**最低高**にすぎず、content の自然高がそれを超える行では実際の行高さの方が大きい。したがって非活性中の確保値は実高さより小さくなり得る。

KDoc `:69-72` の「行の高さはその間も確保されるため、レイアウトは動かない」は、固定高さの行、または content が最低高に収まる行にしか当てはまらない。可変高さの背の高い行では、再活性化が反映されるまでの 1 フレームだけ行が縮み、後続行が一度せり上がってから戻る。実害は 1 フレームに限られるが、次に読む人が「レイアウトは絶対に動かない」という誤った不変条件を前提にしやすい。

**推奨修正**: KDoc を「行の高さは最低高まで確保されるため、レイアウトの動きは (可変高さ行で content が最低高を超える場合の) 1 フレームに限られる」といった精度へ落とす。より踏み込むなら、直前に測定した高さを ViewHolder 側に保持して確保値に使えば可変高さ行でもレイアウトが動かなくなる (実装の要否は Major-2 の記録方針とあわせて判断してよい)。

### [🟡 Minor] android/ADR-0015 の「reset 時点で成立する」が実装と食い違っている

**該当箇所**: `kasane/decisions/android/0015-customcell-pool-aware-composition-disposal.md` の Decision 第 3 項 / `CustomCellViewHolder.kt:212-215`

**問題点**:
ADR は「`remember` / `DisposableEffect` の破棄・購読停止と、ViewHolder が content state・listener 経由で builder を保持しないことは **reset 時点で成立する**」と書いている。一方、実装の KDoc は review-001 の指摘を受けて「実際に破棄される (購読が止まる) のは、非活性化が再 composition に観測された時点であり、この関数の戻り時点ではない」へ改められた。両者は正面から食い違う。

後者が正しい (デルタスペックも破棄の時点までは契約していない)。ADR は `proposed` なので蒸留で accepted へ昇格させる前に直せるが、そのまま昇格すると長命層に誤った不変条件が残る。

**推奨修正**: 蒸留時に ADR-0015 の当該文を「reset 時点で確実に切れるのは ViewHolder が直接握る参照 (content state と click listener)。`remember` / `DisposableEffect` の破棄は非活性化が再 composition に反映された時点」へ改める。

### [🔵 Suggestion] `CustomCellBuilderReleaseTest` の前提アサーションが本命アサーションより後にある

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellBuilderReleaseTest.kt:70-74`

**問題点**: 「Composition が破棄されている」は判定の前提なのに、`awaitCollected` の assert より後に置かれている。何らかの理由で Composition が破棄されなかった場合、失敗メッセージは「builder が参照する対象が回収されない」になり、原因の切り分けに一手余計にかかる。

**推奨修正**: `hasComposition == false` の前提アサーションを先に置く。

### [🔵 Suggestion] 非活性ブランチの幅が `constraints.minWidth` 固定

**該当箇所**: `CustomCellViewHolder.kt:144`

**問題点**: `layout(constraints.minWidth, …)` は、幅が EXACTLY で来る `RecyclerView` (縦 `LinearLayoutManager`) では正しいが、AT_MOST / UNSPECIFIED で測られると幅 0 になる。活性ブランチが `constrainWidth(width)` で最大幅へ寄せているのと非対称でもある。現状の使われ方では顕在化しないが、意図 (行幅は常に外から EXACTLY で決まる) がコードから読み取れない。

**推奨修正**: `constraints.constrainWidth(constraints.maxWidth)` 相当にするか、EXACTLY 前提であることを 1 行コメントで明示する。

### [🔵 Suggestion] `verification-device.md` の初回検証セッション数の記述が節をまたいで食い違う

**該当箇所**: `verification-device.md:102` (「有効セッション 23 件中 21 件で FATAL」) と `:220` (「初回検証で 21 セッション中 21 件発生していたもの」)

**問題点**: 同一文書内で母数が 23 と 21 に割れている。証跡としての信頼性を落とすだけの単純な不整合。

**推奨修正**: どちらかへ揃える (結果表の合計から 23 が正しいと読める)。

## アクションプラン

1. **[Major] `verification-mutation.md` を提出コード (`4ed1767a…`) に対して取り直し、measure guard 除去のミューテーション (g) を追加する。** 本レビューの再実測表を流用してよい
2. **[Major] measure policy の設計判断と「再 bind 後 1 フレームの表示遅れ」の記録先を確定する** (android/ADR-0015 への追記が推奨。deviation 化するならオーナー合意を取る) — 実装の修正は不要
3. [Minor] `CustomCellViewHolder.kt:69-72` の KDoc を可変高さ行の実態に合わせる (必要なら確保高さを直前の測定値にする)
4. [Minor] 蒸留時に android/ADR-0015 の「reset 時点で成立する」を実装に合わせて改める
5. [Suggestion] GC テストの前提アサーション順 / 非活性ブランチの幅の意図明示 / `verification-device.md` のセッション数の食い違い — まとめて対応可

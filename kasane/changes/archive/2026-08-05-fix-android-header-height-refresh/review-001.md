# レビュー結果: fix-android-header-height-refresh (001 回目)

**日付**: 2026-08-05
**判定**: APPROVED

## サマリー

合意済みスコープ (exploration.md 決定事項の選択肢 (a)) を、`CellListItemDiffCallback.areContentsTheSame` への `isSameHeaderHeight` 追加という最小の変更で満たしている。exploration.md が「実装時に決める」として残していた論点 (headerHeight 比較を無条件に行うか Text accessory 限定にするか) は Text 限定に決着しており、その判断は移植元 AiForms の挙動 (`BindCustomHeaderFooterView` が高さに触れない)・KsSettingsView の実装 (`SectionAnyViewAccessoryViewHolder.bind` が headerHeight を参照しない)・`bindKsAnyView` の `KsAnyView.AndroidView` 再生成コストのいずれとも整合し、既存 concept (`styling/list-appearance.md` の「text の Section Header は…」段落) とも矛盾しない。

テスト全件 2002 件が pass し、追加された 3 テストにはミューテーション実測で回帰検出力があることを確認した。Critical / Major はなし。指摘はコメント精度の Minor 1 件と、蒸留フェーズ向けの Suggestion 2 件のみ。

## 検証記録

### ビルド・テスト

`kasane/concepts/cross/conventions/test-execution.md` に従い全件実行した。

- 初回 `./gradlew test` は全タスク UP-TO-DATE (実行 0 件) だったため、規約どおり `--rerun-tasks` で実行し直した
- `cd android && ./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL / 2002 tests, 0 failures, 0 errors, 0 skipped** (`build/test-results/*/TEST-*.xml` の集計)
- `scripts/comment-policy-lint.py` を差分3ファイルへ実行 → 禁止 0 件

### ミューテーション実測 (lessons/code-review L-001)

追加テストの検出力を静的読解で終わらせず、実装へ一時的なミューテーションを入れて実測した。対象は `KsSettingsListAdapter.kt`、`--tests '*FullUpdateContentSyncTest' --tests '*ListAdapterDiffTest'` (計 38 件) で実行。

| ミューテーション | 結果 |
|---|---|
| A: `&& isSameHeaderHeight(oldItem, newItem)` を削除 (修正前の状態へ戻す) | 3 件 FAILED — `replaceSection による headerHeight 変更…` / `Full diff による headerHeight 変更…` / `SectionHeader Text は headerHeight の差で areContents 不等価になり payload が付く`。正の Scenario 3 件がちょうど落ち、他 35 件は通過 |
| B: `isSameHeaderHeight` の `if (oldItem.accessory !is SectionAccessory.Text) return true` ガードを削除 | 2 件 FAILED — `View accessory の Header では headerHeight の差で変更通知を発行しない` / `SectionHeader View は headerHeight が違っても areContents で等価`。負の Scenario 2 件がちょうど落ち、他 36 件は通過 |

両方向とも狙ったテストだけが落ちており、追加テストはトートロジーではなく回帰検出力を持つ。ミューテーション適用後は backup から復元し、shasum 一致 (`567cc6d359b3b8e14690c898f6a725af8ad0ee1c`) で原状復帰を確認済み。

### 確認して問題なしと判断した観点

- **スコープ適合**: 変更は `areContentsTheSame` の SectionHeader 分岐 + private helper 1 個 + テスト 3 件のみ。公開 API・`SectionTextAccessoryViewHolder.bind`・`setRootDirect` いずれも未変更で、S 級の範囲に収まっている
- **足場アーティファクト**: `exploration.md` の差分は「議論再開: 2026-08-05」の探索フェーズ内容 (AiForms 裏取り表・選択肢 (a)(b)(c)・決定事項・ADR 候補) であり、実装中の書き換えではない
- **KDoc の事実性**: 「`isSameAccessoryContent` が true のときだけ評価されるため old / new の accessory は同じ型」は `&&` の短絡評価と `isSameAccessoryContent` の実装 (Text 同士 / View 同士でしか true にならない) から成立。「`KsAnyView.AndroidView` の View が factory から作り直されて内部状態を失う」も `bindKsAnyView` の `AndroidView` 分岐 (`removeAllViews()` + `factory(context)`) のとおり
- **ADR-0012 との整合**: Decision 1 の「CellRow の常時 true は維持」「View accessory は参照比較」「payload 付き rebind へ落とす」はいずれも維持されている。`getChangePayload` は `areContentsTheSame` が false のときだけ到達するため、Header の高さ差でも `PAYLOAD_CONTENT` が正しく付く
- **payload rebind の実効性**: `KsSettingsListAdapter` は 3 引数版 `onBindViewHolder` を実装していないため、payload 付き通知は RecyclerView 既定動作で 2 引数版のフル bind へ委譲される。テストが 2 引数版を直接呼ぶのは実経路と同じ帰結になる
- **テストの実経路性 (lessons/test L-001)**: 正の 2 テストは新規 ViewHolder ではなく**同一 holder を使い回して**再 bind している。`SectionTextAccessoryViewHolder.bind` は `if (lp.height != targetHeight)` でガードするため、既に固定高さを持つ holder の更新経路 (48dp → 96dp) を実際に通す設計になっている。かつ検出力の本体は `ChangeRecord` の照合側にあり、ミューテーション A で実証された
- **負のテストの待機条件**: `View accessory の Header では…` は「変更通知が出ないこと」を待つ足場として s2 への Cell 追加 (`itemCount == 4`) を使う。DiffUtil の通知は 1 バッチで同期発行されるため取りこぼしの競合はなく、`NotificationRecorder` は payload なしの `notifyItemChanged` も (`AdapterDataObserver` の既定委譲により) `ChangeRecord(_, _, null)` として捕捉するので、payload を落とす退行も検出できる
- **境界値**: `headerHeight` の `-1.0`/`0.0` といった「自動高さ」表現の複数値が値比較で不等価になり無意味な rebind が 1 回走る余地はあるが、`concepts/core/core-model/settings-tree.md:40` が「意味が定まる値は `-1` と正値だけ」と公開契約で明示しているため指摘としない
- **Footer 側**: `Section` に `footerHeight` は存在せず、`SectionFooter` 分岐を変えないのは正しい

## 指摘事項

### [🟡 Minor] クラス KDoc とインラインコメントが「Text accessory 限定」の条件を落としている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:257`, 同 `:293-294`

**問題点**: 本変更の要点は「固定高さの差を内容差として扱うのは **Text accessory の Header だけ**」だが、その条件が private helper `isSameHeaderHeight` の KDoc (`:354-363`) にしか書かれていない。上位の 2 箇所は無条件の記述になっている。

- `:257` クラス KDoc: 「Section H/F は **accessory の内容を比較**する（Header は固定高さも含む）」
- `:293-294` `areContentsTheSame` 内: 「Header は表示高さも accessory と同じく再 bind でしか反映されないため、固定高さの差も内容差として扱う。」

クラス KDoc は DiffCallback の契約の要約であり、ここだけを読んだ将来の保守者は「View accessory の Header も高さ変更で rebind される」と誤読し得る。それは本変更が意図的に避けた挙動 (`KsAnyView.AndroidView` の内部状態喪失) そのものであり、誤読のコストが高い。加えて ADR-0012 の Consequences が「KsSettingsListAdapter のコメントの文言更新が必要」を追随項目として挙げており、契約要約の精度はこの変更で維持すべき対象にあたる。

**推奨修正**: 両箇所に Text 限定であることを 1 語加える。例:

- `:257` → 「Section H/F は **accessory の内容を比較**する（**Text** Header は固定高さも含む）」
- `:293-294` → 「Text Header は表示高さも accessory と同じく再 bind でしか反映されないため、固定高さの差も内容差として扱う（View Header は高さを表示に使わないため対象外）。」

### [🔵 Suggestion] concept「表示状態同期」の Section accessory 節が固定高さに触れていない

**該当箇所**: `kasane/concepts/core/architecture/display-state-synchronization.md:40-44`

**問題点**: 同節は Android の内容更新契約を「Section header / footer の **accessory** が非 null から非 null へ変わる更新」として記述しており、`CellListItemDiffCallback` が拾う対象を accessory に限定して読める。本変更で Text Header の固定高さも同じ経路に乗ったため、記述と実装に小さな差が生じる。

**推奨修正**: 本 change の蒸留 (ksn-distill) で、`:44` の Android 行へ「Text Header は `Section.headerHeight` の変化も同じ内容更新経路で反映する (View Header は高さを表示に使わないため対象外)」相当を追記する。実装レビューの範囲では修正不要。

### [🔵 Suggestion] exploration.md の「未決の論点」の決着を蒸留時に記録する

**該当箇所**: `kasane/changes/fix-android-header-height-refresh/exploration.md:59`

**問題点**: 「`headerHeight` 比較を無条件に行うか、Text accessory のときだけ行うか」は実装時決着とされ、実装は Text 限定を選んだ。その理由は `isSameHeaderHeight` の KDoc に残っているが、change 側 (deviation.md 等) には記録がない。S 級なので不足ではないものの、この判断は exploration の ADR 候補「`Section.headerHeight` は text accessory にのみ適用し、View accessory は Content 高さを優先する」の Android 側の先行実装にあたり、後続の `fix-ios-view-header-height-override` で ADR 起票を判断する際の入力になる。

**推奨修正**: 蒸留時に、Android 側は既に Text 限定で実装済みである旨を ADR 候補の判断材料として引き継ぐ。足場アーティファクトの書き換えはレビューでは指示しない。

## アクションプラン

1. (Minor) `KsSettingsListAdapter.kt:257` / `:293-294` のコメントに Text accessory 限定である旨を補う
2. (蒸留フェーズ) `display-state-synchronization.md:44` へ Text Header の固定高さ追随を反映する
3. (蒸留フェーズ) Text 限定の実装判断を、`fix-ios-view-header-height-override` での ADR 起票判断の入力として引き継ぐ

1 は 1 行程度の文言修正であり、本変更のマージを止める性質のものではない。

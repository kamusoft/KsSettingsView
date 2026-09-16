# レビュー結果: maui-cell-appthemebinding-logical-child (002 回目)

**日付**: 2026-09-16
**判定**: CHANGES_REQUESTED

## サマリー

前回サイクルの指摘 8 件 (相方の Major 1 / Minor 1、ホストの Minor 3 / Suggestion 3) はすべて閉じている。中核だった Major — 非 observable な `IList` を `Root` / `Cells` に置き、Host 接続前に要素を足すと論理子にならない — は、所有の器に再照合 (`KsLogicalChildOwnership<T>.Reconcile`) を設けて変換直前に呼ぶ形で解消されており、facade テストは失敗 0 / 合格 557 / 合計 557 (前回 552 + 新規 5) で緑を確認した。

`Reconcile` は「先に全件検査 → 不在の解放 → 引き取り」の順で、observable 経路では `_owned` とコレクションの内容が一致しているため何も変えない (冪等) — 読み取りで確認したとおり、`ReleaseAbsent` は解放対象を見つけず、`Adopt` は `Parent` が既に持ち主なので `AddLogicalChild` を呼ばない。`OnTargetChanged` も事前検査を解放・購読差し替えより前へ出したことで、前回 Minor 1 の「新コレクションが弾かれると旧要素が論理親を失う」中間状態が消え、テスト (`ReplacingRootWithASectionOwnedElsewhereLeavesTheOldPlacementUntouched`) で固定されている。

残る指摘は Critical / Major なし、Minor 1 / Suggestion 2。Minor 1 は、この修正サイクルで生まれた「非 observable コレクションでは所属の確定が変換時になる」という挙動が deviation.md にしか書かれておらず、長命層 (concepts) と release-note が無条件の記述のままである点。deviation.md は change と一緒にアーカイブされるため、この 1 点だけ同じサイクルで長命層へ移してほしい。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| handbook cross/comment-policy.md | 常時 (コメント構文を持つ全ソース) |
| handbook cross/test-execution.md | テストを実行し結果を報告するとき |
| handbook cross/diagnostic-message-language.md | 本体コードの例外 message に触れるとき |
| handbook cross/sample-parity.md | `samples/` のデモ画面を変更するとき |
| handbook cross/runtime-behavior-verification.md | 実行時挙動 (外観変更) の完了判定 |
| handbook maui/integration-host-verification.md | `maui/` facade 層の end-to-end 疎通確認 |
| lessons process.md L-002 / L-003 / L-005 / L-006 / L-009、lessons code-review.md | 昇格済みルール |

機械検査:

- `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` — 失敗 0 / 合格 557 / スキップ 0 / 合計 557 (自分で実行、警告なしでビルド)
- `comment-policy-lint.py --advisory` — 禁止 0 件 (検査対象 796 ファイル)。要確認 164 件はいずれも本 change の外 (`samples/` と android/ios の既存分) で、`maui/` 配下に公開 doc コメント内の ADR 参照は残っていない
- `local-path-lint.py` — 検出なし
- `doc-structure-lint.py` — 本 change が触った 7 文書に違反なし (`view-materialization.md` の散文量は違反ではない「注意」で、元から超過)
- `samples/maui` の `RequestedThemeChanged` 参照 0 件 (samples-maui spec の「購読コードが無い」Scenario)

L-003 (視覚証跡) の判定: このサイクルの実装差分は `Reconcile` の新設と呼び口 5 箇所・テスト・ドキュメントで、利用者の目に見える表示が変わる経路は非 observable な `IList` を `Root` / `Cells` に置いた構成に限られる。Sample も MauiHost も既定の observable コレクション (`SettingsRoot` / `ObservableCollection`) を使っており、observable 経路で `Reconcile` が無変化であることは上記のとおり確認できるため、前サイクルで撮った `evidence/` の 20 枚 + 2 本は引き続き提出コードに対応する。再撮影は不要と判断した。

## 前回指摘の解消状況

| # | 指摘 (出所) | 状況 | 確認した内容 |
|---|---|---|---|
| 1 | 🟠 Major: 非 observable コレクションの表示要素が論理子にならない (相方 code-001) | **解消** | `KsLogicalChildOwnership.Reconcile()` を新設し、変換直前の 5 箇所 (`RebuildRoot` の Root 全件 + 配下 Cell、`AddSections`、`ReplaceSections` の追加分、`ReplaceSectionKeepingCellIds`、`RebuildSectionCells`) から呼ぶ。テスト 4 件 (`SectionAddedToPlainRootListBeforeConnectBecomesLogicalChildOnConnect` / `CellAddedToPlainCellsListBeforeConnectBecomesLogicalChildOnConnect` / `SectionRemovedFromPlainRootListBeforeConnectIsReleasedOnConnect` / `CellAddedToPlainCellsListBeforeConnectFollowsTheAppearanceChange`) で `Parent` と `AppThemeBinding` の再評価まで固定。Section 側 / Cell 側の両方が要求どおり揃っている |
| 2 | 🟡 Minor: 公開 doc コメントと内部コメントの `cross/ADR-0032` 参照 (相方 code-001) | **解消** | `CellBase.cs` / `Section.cs` / `KsLogicalChildOwnership.cs` のいずれにも ADR 参照はなく、自己完結した契約説明になっている。`maui/` 配下の lint 要確認 0 件 |
| 3 | 🟡 Minor: コレクション差し替え時、新コレクションの検査より先に旧要素の所有を解いている (review-001) | **解消** | `OnTargetChanged` が `EnsureAdoptable(target)` を購読解除・`ReleaseAll()` より前に置き、`Root` の propertyChanged も器 → 変換経路の順。`LogicalChildTests.ReplacingRootWithASectionOwnedElsewhereLeavesTheOldPlacementUntouched` が、旧 Section の `Parent`・正しく置かれた側の `Parent`・gateway 呼び出しが空であること・監視先が旧コレクションのまま残ることまで見ている |
| 4 | 🟡 Minor: deviation.md の購読順の記述が実装と食い違う (review-001) | **解消** | 「`Section.Cells` も同じ理由で器を先へ移した (ItemsSource 生成 Cell が生成と同時に論理子になるようにするため)」へ書き直され、`Section.cs` の propertyChanged の実装と一致する |
| 5 | 🟡 Minor: `maui-facade.md` の description だけ本 change の主題を含まない (review-001) | **解消** | description に「論理子と binding の解決」が入り、`concepts/maui/index.md` の 1 行説明と揃った |
| 6 | 🔵 Suggestion: 再評価の配信テストが届いた値を見ていない (review-001) | **解消** | `ReevaluatedCellColorIsDeliveredAsAContentUpdate` が `ReplaceCell` / `ReplaceCells` の snapshot を集めて最後の `Style.TitleColor` が dark 値であることをアサートするようになった。`RemoveCell` / `InsertCell` が空であることの検査も残っている |
| 7 | 🔵 Suggestion: `EnsureCellContentsAreFree` だけ検査と数えあげの順序が逆 (review-001) | **解消** | `EnsureNotOwnedElsewhere(custom, content)` → `AddSeenView(seen, content)` の順になり、`EnsureTreeHasNoDuplicates` の 3 箇所と揃った |
| 8 | 🔵 Suggestion: release-note の accessory View の 1 文が例外の時点を誤読させ得る (review-001) | **解消** | 日英とも「View の場合、例外になる時点は配置の経路で異なる (設定時 / 所有者が表示へ繋がる時点)」の 1 節が加わった |

## 指摘事項

### [🟡 Minor] 非 observable コレクションでの所属確定の時点が長命層に反映されていない

**該当箇所**: `kasane/concepts/maui/api/maui-rendering-lifecycle.md:45`、同 `:53`、`release-note.md:15` / `:25`

**問題点**: このサイクルで入った挙動 — 増減を通知しないコレクション (素の `List<T>`) では、論理子としての付け外しが「コレクションを操作した時点」ではなく「表示へ変換する時点」に確定する — は deviation.md の最後から 3 番目の行にしか書かれていない。deviation.md は change と一緒にアーカイブされるため、残る記述は次の 2 つと食い違ったままになる。

- `maui-rendering-lifecycle.md:45`「コレクションへの所属が始まった時点で論理子になり、除去・コレクションの差し替え・Reset・ItemsSource の再生成で所属が終わった時点で `Parent` が null に戻る」 — 素の `List<T>` では成り立たない。`LogicalChildTests.SectionRemovedFromPlainRootListBeforeConnectIsReleasedOnConnect` が示すとおり、解除は接続 (変換) の時点
- 同 `:53` の多重配置表の 1 行目「`Root` / `Cells` への**追加の時点** (Handler の有無に依らない)」 — 素の `List<T>` への追加は通知が届かないので追加時には弾けず、実際は変換の時点。ここは「Handler の有無に依らない」と明示的に限定を否定している分、食い違いが強い

利用者から見える帰結もある。素の `List<T>` から要素を除いた後、次の変換が起きるまでその要素の `Parent` は旧所有者を指し続けるため、`release-note.md:15` / `:25` が案内する移行手順「先に元のコレクションから除去してから追加すれば新しい所属先の論理子になります」は、素の `List<T>` を使っている利用者では `InvalidOperationException` になる。移行手順として案内している以上、ここが効かない入力があることは書き分けが要る。

なお、修正自体は本 change が既に開いている文書への数行の追記で閉じる (lessons process.md L-005)。

**推奨修正**: `maui-rendering-lifecycle.md` の論理所有の段落と多重配置の表に「実体が `INotifyCollectionChanged` でない場合 (素の `List<T>`) は、付け外しも例外も表示へ変換する時点に揃う」旨の但し書きを 1 文入れる (同文書 `:17` の静的描画の記述と対になる形)。`release-note.md` の移行手順には「observable なコレクションでの手順」であることを 1 句添えるか、素の `List<T>` では `Root` / `Cells` の再代入で確定する旨を足す。

### [🔵 Suggestion] `Reconcile` の呼び口 5 箇所のうち、テストが通るのは 1 箇所だけ

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1139` (`AddSections`)、`:1184` (`ReplaceSections`)、`:1424` (`ReplaceSectionKeepingCellIds`)、`:1466` (`RebuildSectionCells`)

**問題点**: 新規テスト 4 件はいずれも「設定 → 素の `List` を操作 → `GatewayScope.Connect`」の形で、通るのは `RebuildRoot` (`:1019` / `:1022`) の呼び口だけ。残る 4 箇所 — 接続済みの `Root` へ Section を足す / 差し替える経路と、`Cells` 再構築・Section 差し替えの経路 — は、呼び出しを外しても新規テストは緑のままになる。Major の再発を防ぐ網としては、経路ごとの回帰検出力が片側に寄っている。

**推奨修正**: 接続済みの `SettingsView` の `Root` (observable) へ、`Cells` に素の `List<CellBase>` を持つ Section を追加したときにその Cell が論理子になることを 1 件足す (`AddSections` の呼び口を通る)。余力があれば `Cells` を素の `List` のまま再代入する経路も 1 件。

### [🔵 Suggestion] `ReplaceSectionKeepingCellIds` への `Reconcile` 追加が、構造と無関係な経路に例外送出点を増やす

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1424`

**問題点**: この経路は `Section.IsVisible` / `HeaderHeight` など Section 自身のプロパティ変更で走り、配下 Cell の構造検査 (`EnsureCellsHaveNoDuplicates` 等) を一切持たない。そこへ `Reconcile` を置くと、`Reconcile` 内の `EnsureAdoptable` だけが検査として働き、素の `List` に他所所有の Cell が混ざっている構成では「Section のプロパティを変えただけ」で `InvalidOperationException` が出る。到達条件は狭いが、この経路に検査が 1 つだけ生えている状態は後から読むと意図が分かりにくい。

**推奨修正**: 意図 (この経路も `Snapshot(section.Cells)` を native へ送るため所有を揃える必要がある) を呼び出し側に 1 行添えるか、`ReconcileCellOwnership` の remarks に「Section 単位の差し替えでも配下 Cell を送るため照合する」を足す。

## 所見 (指摘ではないもの)

- **`ReconcileCellOwnership(sections)` の途中失敗**: Section を順に照合するため、N 番目で例外になると 0〜N-1 は照合済みになる。ただし照合が行うのは「コレクションに居ない要素の解放」と「居る要素の引き取り」だけで、どちらも現在のコレクション内容に合わせる方向の変化なので、`maui-rendering-lifecycle.md:57` の「既存の配置は動かない」(追加時に弾かれた場合の話) と衝突しない。指摘にはしていない
- **`OnTargetChanged` の `ReleaseAll` による一瞬の切断**: `Root` / `Cells` の再代入で、旧側と新側の両方に居る要素は `ReleaseAll` → `Apply` で `Parent` が一度 null になってから付け直される。`DynamicResource` は付け直しで同じ値に再解決され、再代入は表示自体を作り直す経路なので実害は見当たらない。design Decision 1 が「旧コレクション由来の子を全部外してから」と定めた形どおりなので、変更は提案していない
- **`_owned` の保持**: 素の `List` から外れた要素は次の変換まで `_owned` に残る (強参照)。`Reconcile` が変換のたびに掃除するため滞留は変換までに限られ、設定画面の規模では実害なし
- **例外 message が失敗理由を区別しない**: 他所所有の追加も同一コレクションの二重追加も `The same {kind} instance cannot be placed more than once.`。前回と同じく tasks 1.1 が「`DuplicatePlacement` と同じ文言」を指定しているため実装の逸脱ではない
- **`cross/ADR-0032` は proposed のまま**: concepts 4 文書が参照しているが昇格は蒸留時。proposed を根拠にした判定は行っていない
- **ミューテーション実測は行っていない**: lessons code-review L-001 の手法 (`Reconcile` を空実装にして新規テストが落ちることを実測する) を試みたが、本レビューの制約 (`review-002.md` 以外を書き換えない) に触れるため実施していない。上の Suggestion 1 で挙げた呼び口ごとの検出力は、静的読解に基づく所見

## アクションプラン

1. Minor 1 (非 observable コレクションでの所属確定の時点) — `maui-rendering-lifecycle.md` の 2 箇所へ但し書き、`release-note.md` の移行手順に 1 句。deviation.md にしかない知識を長命層へ移す
2. Suggestion 1 (`AddSections` 経路の回帰) — テスト 1 件
3. Suggestion 2 (`ReplaceSectionKeepingCellIds` の意図) — コメント 1 行

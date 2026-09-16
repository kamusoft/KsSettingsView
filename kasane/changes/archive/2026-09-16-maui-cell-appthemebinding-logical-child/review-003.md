# レビュー結果: maui-cell-appthemebinding-logical-child (003 回目)

**日付**: 2026-09-16
**判定**: APPROVED

## サマリー

前サイクルの指摘 3 件 (Minor 1 / Suggestion 2) はすべて閉じている。中核だった Minor — 非 observable なコレクションでは所属の確定が変換の時点になるという挙動が deviation.md にしか無い — は、`kasane/concepts/maui/api/maui-rendering-lifecycle.md` の論理所有の段落と多重配置の表の両方へ但し書きが入り、release-note の移行手順にも日英そろって書き分けが加わった。追記の内容は現行コード (`KsLogicalChildOwnership.Reconcile` / `OnTargetChanged` と controller の 6 箇所の呼び口) を読み直して照合し、食い違いは見つからなかった。facade テストは失敗 0 / 合格 560 / 合計 560 (前回 557 + 新規 3) で緑。

release-note が案内する素の `List<T>` 向けの移行手順 (除去 → `Root` / `Cells` へ新しいコレクションを代入 → 新しい場所へ追加) も実装で成立する。`Root` / `Cells` の propertyChanged はどちらも器の `OnTargetChanged` を先に通し、そこで `ReleaseAll()` が控えの全要素を外すため、除去済みの要素の `Parent` はその代入の時点で null に戻る。同じコレクション実体を代入し直しても BindableProperty が変更と見なさず何も起きないので、「新しいコレクション」と書き切っているのも正しい。

残る指摘は Critical / Major なし、Suggestion 1 件のみ (`maui-facade.md` の 1 行要約に同じ書き分けが無い)。判定を妨げるものではないため APPROVED とする。

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
| ksn-core references/concepts.md・doc-structure.md | concepts / handbook を書き換えるとき |

機械検査:

- `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` — 失敗 0 / 合格 560 / スキップ 0 / 合計 560 (自分で実行、警告なしでビルド)
- `comment-policy-lint.py --advisory` — 禁止 0 件 (検査対象 796 ファイル)。要確認 164 件はいずれも本 change の外 (`samples/` と android/ios の既存分) で、このサイクルで足した 2 行のコメントと doc コメントに ADR 参照・作業文書参照・履歴記述はない
- `local-path-lint.py` — 検出なし
- `doc-structure-lint.py` — 本 change が触った 7 文書に違反なし (`view-materialization.md` の散文量は違反ではない「注意」で、元から超過)
- `samples/maui` の `RequestedThemeChanged` 参照 0 件 (samples-maui spec の「購読コードが無い」Scenario)

L-003 (視覚証跡) の判定: このサイクルの差分は concepts / release-note / deviation の文章、facade テスト 3 件、controller のコメント 2 行だけで、実行時の挙動を変えるコードは 1 行も無い (`ReconcileCellOwnership` の呼び口の追加は前サイクルで済んでいる)。したがって `evidence/` の 20 枚 + 2 本は引き続き提出コードに対応し、再撮影は不要と判断した。証跡の実在と内容 (画素比較 32 組すべて差分 0、外観切替の live 4 枚) は確認済み。

## 前回指摘の解消状況

| # | 指摘 (出所) | 状況 | 確認した内容 |
|---|---|---|---|
| 1 | 🟡 Minor: 非 observable コレクションでの所属確定の時点が長命層に反映されていない (review-002) | **解消** | `maui-rendering-lifecycle.md:45` の論理所有の段落に「この時点が『操作した時点』になるのは実体が `INotifyCollectionChanged` の場合で、observable でない実体 (素の `List<T>`) では…付け外しは表示へ**変換する時点**に揃う」と、外れた要素の `Parent` が変換の時点で null に戻ることまで入った。同 `:53` の多重配置表の 1 行目にも「observable でない実体では追加が届かないため変換の時点」が括弧で加わり、「Handler の有無に依らない」の断定が限定された。release-note は日英とも移行手順を 2 段に割り、`SettingsRoot` / `ObservableCollection<T>` では除去の時点で効くこと、素の `List<T>` では `Root` / `Cells` への新しいコレクションの代入で確定させることを書き分けている (`release-note.md:15` / `:25`) |
| 2 | 🔵 Suggestion: `Reconcile` の呼び口 5 箇所のうちテストが通るのは 1 箇所だけ (review-002) | **解消** | `LogicalChildTests` に 3 件追加。`CellInPlainCellsListBecomesLogicalChildWhenTheSectionIsAddedToConnectedRoot` (接続済み Root への `Add` → `AddSections`)、`...WhenItsSectionReplacesAConnectedOne` (`Root[0] =` → `ReplaceSections`)、`...WhenTheSectionItselfIsReplaced` (`HeaderHeight` 変更 → `ReplaceSectionKeepingCellIds`)。いずれも `cell.Parent` が新しい Section であることと、gateway へ届いた Section の Cells が 1 件であることの両方を見ており、対応する `Reconcile` を外せば `Parent` のアサートが落ちる形になっている。残る `RebuildSectionCells` の呼び口は下記「所見」のとおり到達可能な経路では常に no-op |
| 3 | 🔵 Suggestion: `ReplaceSectionKeepingCellIds` への `Reconcile` 追加が、構造と無関係な経路に例外送出点を増やす (review-002) | **解消** | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1423` に「Section 自身のプロパティ変更でも配下 Cell をそのまま native へ送るため、送る前に論理上の所有を今の内容へ揃える。」の 2 行が入った。コメント規約の許容範囲 (自己完結した説明、外部識別子なし) に収まっている |

review-001 (Minor 3 / Suggestion 3) と second-opinion-code-001 (Major 1 / Minor 1) の 8 件は review-002 で解消を確認済み。今回もコード側で該当箇所を読み直し、再発 (`OnTargetChanged` の検査順序、`EnsureCellContentsAreFree` の順序、`maui/` 配下の公開 doc コメントの ADR 参照、deviation の購読順の記述、`maui-facade.md` の description) が無いことを確かめた。

## 指摘事項

### [🔵 Suggestion] `maui-facade.md` の 1 行要約だけ「その時点で例外」の限定が付いていない

**該当箇所**: `kasane/concepts/maui/api/maui-facade.md:92`

**問題点**: 「してはいけないこと・制約」の箇条書きが「他所に所有されたままの Section / Cell の追加は、その時点で `InvalidOperationException` になる」と無条件に書いている。今回 `maui-rendering-lifecycle.md:53` の表へ入れた限定 (素の `List<T>` では追加が届かないので変換の時点) がここには掛かっておらず、前サイクルで Minor として直した記述と同じ型のずれが 1 箇所だけ残っている。

同じ文の末尾から `maui-rendering-lifecycle.md` へリンクしているため、詳細の正がどちらかは読み手が辿れる。実害はこの 1 文だけを読んだ利用者が例外の時点を取り違えることに限られるので Suggestion とした。

**推奨修正**: 「追加の時点 (増減を通知しないコレクションでは表示へ変換する時点)」のように括弧で 1 句添えるか、時点の断定を落として「多重配置は例外になる」とだけ書き、時点はリンク先に委ねる。

## 所見 (指摘ではないもの)

- **`RebuildSectionCells` の `Reconcile` は到達可能な経路では no-op**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1468` の呼び口に届くのは `Section.Cells` の再代入だけで (`:1383` の分岐)、`Section.Cells` の propertyChanged が先に `_cellOwnership.OnTargetChanged()` を通して新しい内容を引き取り済みになる (BindableProperty の `propertyChanged` デリゲートは `PropertyChanged` イベントより先に走る。`ReplacingCellsCollectionMovesOwnershipAndStopsWatchingTheOldOne` が緑であることがその順序の裏付け)。冗長だが他の 5 箇所と形が揃っており、外すと「変換の直前に照合する」という規律に穴が空いて読み手を惑わせるため、現状のままでよいと判断した。テストが無いこともこの理由で指摘にしていない
- **`ReconcileCellOwnership(sections)` の途中失敗**: Section を順に照合するため N 番目で例外になると 0〜N-1 は照合済みになる。照合が行うのは現在のコレクション内容へ寄せる方向の変化だけなので、`maui-rendering-lifecycle.md:57` の「追加の時点で弾かれた場合、既存の配置は動かない」と衝突しない (前回と同じ所見)
- **`RebuildRoot` は照合を重複検査より先に行う**: `:1019` / `:1022` の `Reconcile` が `EnsureTreeHasNoDuplicates` (`:1023`) より前にあるため、同一コレクション内の重複で失敗する構成でも `Parent` の付け直しは済んでいる。observable 経路では追加の時点で既に `Parent` が付いており状態は変わらず、素の `List<T>` 経路でも spec が禁じる「native への部分更新」は起きないので違反ではない
- **`_owned` の保持**: 素の `List<T>` から外れた要素は次の変換まで控えに残る (強参照)。`Reconcile` が変換のたびに掃除するため滞留は変換までに限られる
- **例外 message が失敗理由を区別しない**: 他所所有の追加も同一コレクションの二重追加も `The same {kind} instance cannot be placed more than once.`。tasks 1.1 が「`DuplicatePlacement` と同じ文言」を指定しているため実装の逸脱ではない
- **`cross/ADR-0032` は proposed のまま**: concepts 4 文書が参照しているが昇格は蒸留時。proposed を根拠にした判定は行っていない
- **ミューテーション実測は行っていない**: lessons code-review L-001 の手法 (呼び口を外して新規テストが落ちることを実測する) はレビュー対象ファイルの一時改変を伴い、本レビューの制約 (`review-003.md` 以外を書き換えない) に触れるため実施していない。呼び口とテストの対応は静的読解による

## アクションプラン

1. Suggestion 1 (`maui-facade.md:92` の 1 句) — 直すなら数語。判定を妨げないため、蒸留時にまとめて扱ってもよい

## 追記 (Suggestion 1 の解消確認)

`kasane/concepts/maui/api/maui-facade.md:92` が「その時点 (増減を通知しない素の `List<T>` では変換の時点) で `InvalidOperationException` になる」へ直り、`maui-rendering-lifecycle.md:53` の表の限定 (「observable でない実体では追加が届かないため変換の時点」) と同じ内容を指すようになった。現行コードとも一致する — observable 経路は変更通知から `KsLogicalChildOwnership.Adopt` が追加の時点で送出し、素の `List<T>` は変換の直前の `Reconcile` → `EnsureAdoptable` が送出する。

追記後に `doc-structure-lint.py` (`maui-facade.md` に違反なし) と `comment-policy-lint.py` (禁止 0 件) を再実行して問題なし。**判定は APPROVED を維持**し、未解消の指摘は残っていない。

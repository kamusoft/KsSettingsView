# レビュー結果: fix-ios-full-content-refresh (2 回目)

**日付**: 2026-08-06
**判定**: APPROVED

指摘件数: Critical 0 / Major 0 / Minor 0 / Suggestion 0
前回: `review-001.md` (CHANGES_REQUESTED — Minor 2 / Suggestion 2)

## サマリー

review-001 の Minor 2 件と Suggestion 1 件がいずれも解消され、新たな問題の持ち込みもない。特に中心的だった「単一 `.full` 適用で headerHeight と同一 ID Cell の内容の両方が表示へ届く」ことの回帰テストは、追加されただけでなく**ミューテーション実測で検出器として機能することを確認**した — 内容再適用の出口だけを塞ぐと、前提アサーション (header の高さ) は通過したまま争点のアサーション (Cell の title) だけが落ちる。

全件テストを検証者側で再実行し 624 件 / 0 failures。ミューテーションに使ったソースは backup から復元し shasum 一致を確認済み。

## 前回指摘の解消確認

### [🟡 Minor → 解消] 廃止した `.full` → `.replaceCell` 契約を現行仕様として説明する既存テストが残っている

**確認箇所**: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:629-680`

テストは `test_replaceAll1回でheader高さとCell内容の両方が表示へ反映される` へ改名され、doc コメント・インラインコメント・失敗メッセージのすべてから DSL preflight と `.replaceCell` 続発への言及が消えた。`store.replaceCell` の呼び出し自体も除去され、単一の `replaceAll` だけを適用する形になっている。指摘した「主張を検出できない」状態も同時に解消された (下の実測を参照)。

リポジトリ全体を走査しても、廃止契約を現行仕様として述べる記述は残っていない。`SectionAccessoryRenderingTests.swift:589` の「DSL の headerHeight preflight が発行する `.full` の適用先がこの経路になる」は新契約でも正しい記述であり、残置で問題ない。

### [🟡 Minor → 解消] MODIFIED Requirement の表示レベルの回帰テストが無い

**確認箇所**: 同 `:632-680`

同一 Section ID・同一 header accessory のまま `headerHeight` 40→90 と同一 ID Cell の title を 1 回の `replaceAll` で変え、(1) header の layout attributes 高さ、(2) 表示中 supplementary の実 frame 高さ、(3) 行の title、(4) 行の Native cell インスタンス同一性 の 4 点を検査している。指摘時に推奨した内容を満たすうえ、行 identity の検査は Requirement の identity 保証にも接地しており、推奨より一段強い。

**検出力の実測** (`lessons/code-review.md` L-001 に従い、静的読解で済ませず測定):

`KsSettingsViewController.swift:1201` の reconfigure 適用ブロックを `if false, ...` で無効化して当該テストクラスを実行した結果:

- `test_replaceAll1回でheader高さとCell内容の両方が表示へ反映される` が **failed**。失敗は `:674` の争点アサーションのみ (`"旧タイトル"` vs `"新タイトル"`) で、直前の高さアサーション 2 件は**通過**している。内容再適用だけを分離して捕捉できている
- 同クラスの `test_Store経由のreplaceAllのheaderHeight変更が表示中headerの実高さに反映される` は同条件で **passed**。高さ経路は元から別テストが押さえており、新テストが被覆を重複させず新しい面を足していることの裏付けになる
- 復元後の全件実行で 624 件 / 0 failures を確認。backup との shasum 一致 (`25f43286…`) で原状復帰を確認済み

### [🔵 Suggestion → 採用] `FullSnapshotContentTargets` の guard 順序の意図

**確認箇所**: `ios/Sources/KsSettingsViewUI/FullSnapshotContentTargets.swift:67-70`

「判定を Section 再構成の除外より前に置くのは意図的で、Section 再構成側でも内容は最新になるため実質冗長だが、Native cell を交換する判断を Section の再構成の有無に依存させない」と追記された。指摘した「片方の guard 漏れに見える」問題は解消している。挙動の変更はない。

### [🔵 Suggestion → 見送り合意済み] `#available(iOS 15.0, *)`

既存 3 箇所との一貫性を優先し本変更では対応しない、で合意済み。再指摘しない。

## 新たに確認した観点 (指摘に至らなかったもの)

- **新規テスト `test_replaceAll直後に同一Cellのタイトルをさらに変えても表示が追従する` の位置づけ**: ミューテーション下でも passed であり、本 fix の検出器**ではない**。ただしこれは欠陥ではなく、このテストの目的 (full 適用直後の snapshot に部分更新を重ねても表示が壊れないこと = 新しい reconfigure / reload マークが後続の部分適用と干渉しないこと) に照らせば妥当な振る舞い。上の検出器テストと役割が分かれており、片方がもう片方を代替する関係にはない
- **`FullSnapshotContentTargetsTests.swift:86` の追加アサーション**: `targets.reconfigure.count == 2` は、ヘルパ `ids()` が Set へ畳むことで同一 ID の二重登録を見逃す穴を塞いでいる。対象選定が重複エントリを返す誤実装を新たに検出できるようになった
- **既存テストとの契約衝突**: `test_fullDiffでheader不変ならCellは再構成されない` (`:492`) は名前が新契約と衝突するように見えるが、旧・新で同一の `cells` 配列を渡しており内容差分が無いケース。新契約 (差分のある Cell だけを reconfigure) と整合しており、むしろ「不要な再構成をしない」側のガードとして機能している
- **ソースコメント規約**: 修正で触れた 3 ファイルを `comment_policy_rules.py` で直接検査し clean (未追跡ファイルは `scripts/comment-policy-lint.py` の対象から落ちるため個別適用)
- **足場の逆流**: `proposal.md` / `specs/` は前回検証以降 mtime に変化なし。修正は `ios/` 配下に限定されている

## アクションプラン

なし。`ksn-distill` へ進んでよい。

蒸留時の申し送りとして proposal.md が挙げている「concepts『表示状態同期』の内容更新節へ iOS 側機構 (reconfigure による一括内容再適用) を追記」は、本レビュー時点で未実施のまま残っている (蒸留フェーズの作業であり、実装レビューの指摘対象ではない)。

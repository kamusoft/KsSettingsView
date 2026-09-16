# 経緯: 新 Requirement の保証が合流先の既存機構の契約と突き合わされないまま確定する (spec-review L-005)

inbox パターンとして pain 3 件で閾値到達し、2026-09-16 にオーナー承認で `spec-review.md` L-005 へ昇格した。

## 観測 (3 change)

- fix-dsl-header-height-diff (spec の「headerHeight + 内容の同時変更で両方反映」が、合流先 `applyFullSnapshot` の既存契約「`.full` は同一 ID Cell を再構成しない」[既存テストが固定] と両立しないまま提案確定しかけた。ホスト側は「担保方法は実装判断」に逃がし、相方 second-opinion-001 が Critical で検出)
- fix-ios-full-content-refresh (行 identity 保証が `reloadSections` の既知動作 [concepts 記載の全 Cell 再構成] と矛盾、`KsCellID` の UUID のみ等価判定で具象型変更ケースが未定義、既存 headerHeight preflight の発行列と新機構で同一 Cell の二重 reconfigure。ホスト自己レビュー 2 周は検出 0、相方 second-opinion-001 が Major 5 件を検出し全件採用)
- maui-cell-appthemebinding-logical-child (ADDED Requirement「Section / Cell は SettingsView の論理子である」の保証「所属が始まる時点で論理子になる」が、合流先のコレクション観測機構の既存契約 [concepts: 増減を通知しない素の `List<T>` も `Root` / `Cells` の正式な入力で、表示へ変換する時点の内容を静的描画] と突き合わされないまま確定。素の `List<T>` では所属の始まりを観測できず保証が成立しない。ホスト spec-review・相方 spec-review・ホスト code-review のいずれも検出せず、相方 code-review が Major で検出。変換直前の再照合を足して解消し deviation に記録)

## 経緯

- 2026-08-05 fix-dsl-header-height-diff: iOS の同時変更 Scenario は `.full` のみの発行を前提としたが、当時の `applyFullSnapshot` は同一 ID Cell を reconfigure しない契約 (既存テスト `test_fullDiffでheader不変ならCellは再構成されない` が固定) で、表示反映の保証が成立しなかった。相方 Critical を受けて `.full` + `.replaceCell` 続発方式へ spec を確定。
- 2026-08-06 fix-ios-full-content-refresh: 内容再適用機構の追加提案で、reloadSections との identity 矛盾・具象型変更の未定義・既存 preflight 続発との二重適用の 3 点がいずれも「新 Requirement × 既存機構の契約」の突き合わせ漏れとして相方から指摘された。うち二重適用 (M4) はオーナー裁定で続発廃止が確定し、前 change で入れた続発方式は 1 日で廃止された — 突き合わせを提案段階で行っていれば、依存 change の実装順 (full 側を先に直す) の検討機会もあった。
- 2026-09-16 maui-cell-appthemebinding-logical-child: 論理子化の Requirement が「所属が始まる時点」を保証したが、合流先 (コレクションの増減を見る器) が正式に受け付ける入力のうち増減を通知しない素の `List<T>` では成立しない。concepts (`maui-rendering-lifecycle.md`) が「observable でない実体は接続時点の内容を静的描画する」と契約として明記していたのに、Requirement を書く側もレビューする側もその入力集合と突き合わせなかった。提案段階の相方 spec-review は SettingsView をまたぐ配置の穴を突いたが、この入力集合の穴は code-review 段階で相方が検出 (Major 1)。実装は変換直前に器が現在の内容と照合し直す形で解消し、concepts と release-note に「素の `List<T>` では付け外し・例外が変換の時点に揃う」を書き分けることになった (review-002 Minor で長命層への反映漏れも 1 周)。3 件目で pain 閾値到達。

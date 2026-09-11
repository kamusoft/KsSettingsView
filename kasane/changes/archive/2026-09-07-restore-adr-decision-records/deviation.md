# Deviation: restore-adr-decision-records

S 級のため足場は exploration.md のみ。以下は「決定事項」「実行」節との差と、名指しされていない編集の記録。

## 決定事項との差

- 「8 本共通で footer に `現行照合` 1 行を足す」: 初回の実装では `整理` 行を 8 本すべてに置き、`現行照合` は実際にコードと照合した 2 本 (cross/0018・0019) だけに限った。理由: `現行照合` は ksn-core が「ADR の主張をコードで照合したら残す」と定めており、照合していないものに書くと裏取りのない断定になると判断した。review-001 Major 2 で「合意済みスコープは 8 本共通と明記しており、`整理` は作業記録であって主張の判定ではない」と差し戻され、残り 6 本を実際に照合したうえで `現行照合` を追加して解消 (2026-09-07)
- maui/0015 の是正範囲: 初回は「実装時点の残課題 3 件」の Consequences bullet ごと除去したが、この bullet は日付マーカーを持たず決定の帰結として書かれたものだった。review-001 Minor で合意範囲外と指摘され、bullet を本文へ復元し、除去対象を追記行 (2026-08-22) のみに戻した (2026-09-07)

## 付随修正

- [付随修正] `kasane/decisions/android/0019-datepickercell-calendar-compose-datepicker.md` の footer 出典: `kasane/changes/relax-android-host-prerequisites/exploration.md` → `kasane/changes/archive/2026-08-28-relax-android-host-prerequisites/exploration.md`。理由: 参照先が archive 済みでパスが解決しなくなっていた。footer は規約上可変 (2026-09-07)
- [付随修正] `kasane/decisions/cross/0019-lockstep-single-version.md` と `cross/0020-release-dispatch-tag-last-version-injection.md` の footer 出典ラベルから日付と「追記」の語を落とした (例: `出典 (2026-09-04 prerelease 形式・初回リリースの追記):` → `出典 (prerelease 形式):`)。理由: 本文の変更ログ化を直す作業で、footer のラベルだけ追記の履歴を語る形が残るため (2026-09-07)
- [付随修正] `kasane/decisions/cross/0020-release-dispatch-tag-last-version-injection.md` の Consequences 最終行と footer の間に空行を補った。理由: 実装結果ブロックの除去で空行が失われ、footer が本文の続きに見える形になっていた (2026-09-07)
- [付随修正] `kasane/lessons/inbox/` の 4 本 (`project-adr-carves-exception-into-harness-rule` / `implementation-results-accreted-into-adr-consequences` / `absence-fallback-masks-failed-search-command` / `review-fix-applied-only-to-cited-instance`): 探索と実装の各時点で踏んだ観測を捕捉した。理由: exploration の「実行」節は触る対象を長命層の 10 ファイルと数えて lessons を名指ししていないが、観測は発生時点で捕捉する規律 (ksn-lesson) に従う。前 2 本は探索の commit `8266087` に含まれる (2026-09-07)
- [付随修正] `kasane/concepts/cross/index.md` と `kasane/concepts/log.md`: 新概念の追加に伴う目次 1 行と更新履歴 1 件。理由: exploration の「実行」節は触る対象を 10 ファイルと数えてこの 2 つを名指ししていないが、concepts への書き込み経路は index / log の更新を必須とする (ksn-core references/concepts.md) (2026-09-07)

## 記録した所見 (本変更では直さない)

- `maui/0015` が指していた追跡先の change は `kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/` に実在し、残課題 (2) の共有破棄はそこから maui/ADR-0026 へ結実している (同 ADR の出典行が当該 archive を指す)。ADR-0015 の追記行を除いても追跡の経路は ADR-0026 側に残る
- ADR 4 本 (android/0003・0005・0007、maui/0022) が YAML frontmatter を持たない旧形式 (`- Status: accepted` のリスト形式) で、frontmatter を前提とする機械検査から漏れる。追記マーカーは 0 件のため本変更の対象外
- `cross/ADR-0021` の現行照合で、`kasane/changes/archive/**` の媒体 5 件 (2026-08-02〜03 の `ui/references` の webp) が追跡下に残っていることを確認した。archive 時の媒体削除を運用に入れる前の変更によるもの
- `kasane/concepts/` に構造 lint の既存違反が 29 件 / 9 ファイル (本変更が触っていないファイル群)

---
scope: all
timestamp: 2026-09-07
---

# lessons: 却下台帳

昇格せずに落としたパターンを 1 行ずつ残す (append-only)。捕捉モードはここを照合し、同型の再提案ループを断つ。
状況が変わったとオーナーが明言した場合のみ復活起票してよい。

| 日付 | scope | 要約 | 却下理由 |
|---|---|---|---|
| 2026-09-07 | process | ADR の Consequences に実装結果 (実測値・現況・消化状況) を積まない。proposed の更新は追記マーカーではなく本文の書き直しで行う (`implementation-results-accreted-into-adr-consequences`) | ハーネスへ反映済み。ksn-distill が Step 3c に「実装を経て判明したことの行き先」の表と昇格ゲートを持ち、禁止事項に「実装結果を Consequences に列挙する」「proposed の ADR に追記を重ねたまま昇格させる」を加えた。プロジェクト側に残すと同じ規律の正が 2 つになる |

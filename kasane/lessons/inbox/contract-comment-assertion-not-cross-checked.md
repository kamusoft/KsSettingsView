---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-08
last-seen: 2026-08-08
evidence:
  - release-host-without-bridge-dispose (second-opinion-002 Minor: 検証ホストの doc コメントが「DTO 自身が公開する ID は Store に存在せず、API の戻り値だけが有効」と断定するが、同じ diff 内のコードは `themeCell.CellID` 等 Builder / insert 経由で追加した DTO のプロパティ ID を保存して動作していた。ホスト review-001 は素通しし、相方が矛盾を検出)
---

## ルール文

公開契約を断定する doc コメント (「〜は無効」「〜は存在しない」「〜だけが有効」等) を含む diff では、その主張を同じ diff 内の実際の利用コードと読み比べて矛盾がないか確認する。コメントの断定と数行下の実利用の食い違いはコンパイルもテストも検出しない — 誤った契約説明は将来の利用者に誤実装を促す。

## 経緯

- 2026-08-08 release-host-without-bridge-dispose: `maui/tests/shared/KsBridgeScenarioHandles.cs` の doc コメントが ID 契約を過剰一般化して断定 (「DTO 自身が公開する ID は Store に存在しない」)。実際の契約は「Builder / insert で追加された DTO の ID は Store identity になる / replace に渡した新 DTO の ID は不採用」であり、コード自身が DTO プロパティ ID を保存して正常動作していた。ホスト側レビューは見逃し、相方 (codex) の code-review が矛盾として検出。突き合わせで採用され、コメントを実態に合わせて修正した。

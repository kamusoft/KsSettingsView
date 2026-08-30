---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-28
last-seen: 2026-08-28
evidence:
  - relax-android-host-prerequisites (Compose material3 DatePicker が @ExperimentalMaterial3Api である事実を、調査報告・比較表のリスク欄・ADR-0019 負の帰結・proposal Impact に記載したものの、独立した採否の判断点としてオーナーに確認しなかった。版ズレクラッシュの原因説明で experimental と知ったオーナーから「使用を許可した覚えはない」との指摘)
---

## ルール文

採用しようとする外部 API に安定性の但し書き (experimental / alpha / beta / deprecated / opt-in annotation) が付いているときは、リスク箇条書きへの記載で済ませず、**探索・提案の案提示の時点で「この API は安定保証がない。採用するか」を独立した選択肢 (採用 / 代替 / 見送りと各影響) としてオーナーに明示確認**し、確認の結果を ADR / proposal の Decision 側 (リスク欄ではなく) に書く。リスク欄の1項目は承認の文脈で流れ、オーナーの認知に残らない — 安定性クラスは実装詳細ではなく採否を左右する契約条件として扱う。

## 経緯

- 2026-08-28 relax-android-host-prerequisites: カレンダー選択面を Compose material3 DatePicker (experimental) に統一する決定 (ADR-0019) で、experimental である事実は複数の文書に「リスク」として記載され、文書ごとオーナー承認も得ていた。しかし採否の判断点として口頭確認したことはなく、実装完了後の版ズレクラッシュ (material3 1.3.1/1.4.0 のシグネチャ非互換) の原因説明で初めてオーナーが experimental であることを認知し、「使用を許可した覚えはない」との指摘を受けた。事実の記載と判断の確認は別物である、という観測。

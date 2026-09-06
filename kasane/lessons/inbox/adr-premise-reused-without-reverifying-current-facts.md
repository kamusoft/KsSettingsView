---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-06
last-seen: 2026-09-06
evidence:
  - cell-style-dark-appearance-parity (proposal の Impact が「ライブラリは未配信のため破壊的変更を受容」を core/ADR-0030 の Context からそのまま引き継いだが、Android は `0.1.0-beta.1` として 2026-09-04 に Maven Central へ配信済みで、ADR-0030 の accepted 時点 (09-05) で既に古い前提だった。探索・提案・ホスト側自己レビューのいずれも README のインストール例と concepts の公開後 smoke 記述を照合しておらず、相方 spec レビュー (second-opinion-spec-001 Major 1) が検出。オーナー裁定で「beta 期間中は互換経路なしで受容」に改め、ADR-0030 の前提訂正 (amends) が蒸留の作業になった)
---

## ルール文

提案が accepted ADR の Context の前提 (「未配信」「同時接続は N 以下」「ライブラリ X は保守中」のような外部事実) を判断の根拠として引き継ぐときは、その前提を現在のリポジトリの実体 (README のインストール例・release tag・配信レジストリ・concepts の配信状況記述) と照合してから proposal に書く。前提が崩れていたら、崩れた前提で決めた ADR の Revisit When に当たるとして、proposal の Impact に現状と改訂方針を書き、蒸留で amends / supersede を起票する。事後判定: proposal が ADR の前提を根拠に挙げる文には、照合した実体 (tag 名・日付・参照ファイル) が添えてある。

## 経緯

- 2026-09-06 cell-style-dark-appearance-parity: ADR-0030 (09-05 accepted) の「未配信で外部利用者がいない」を、翌日の探索・提案が破壊的変更の根拠として引き継いだ。実際には beta.1 が 09-04 に配信済みで、前回 change (fix-default-colors-dark-appearance) の Theme / CellStyle の型変更も配信後に行われていた。ADR の前提は accepted の瞬間に固定されるが外部事実は動く。相方レビューは README と concepts の配信記述から機械的に照合して検出した。

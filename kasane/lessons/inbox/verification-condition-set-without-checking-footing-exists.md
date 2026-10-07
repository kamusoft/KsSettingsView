---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-10-07
last-seen: 2026-10-07
evidence:
  - android-compose-scroll-restore-with-multiple-hosts (探索で「MAUI Android も端末で確かめる」を完了条件に決めたが、サンプルと検証用のホストアプリに `SettingsView` のページが 2 つ重なる経路があるかを確かめていなかった。実装フェーズでワーカーが着手前に停止し、サンプルへ経路を足すオーナー裁定の往復が生じた)
---

## ルール文

探索・提案で、端末や実環境での確かめを完了条件に入れるときは、決定の前に、その確かめを実行できる足場 (サンプル・検証用のホストアプリ・テストの土台に、確かめたい構成へ到達する経路) があるかを実物で確かめ、結果を exploration または proposal に書く。足場が無ければ、足場を足す作業をスコープに含めるか、確かめ方を変えるかを同じ決定の中で選ぶ。

確かめの対象にした挙動の見立て (何が変わるか) も、足場を調べるときに到達経路の実装 (ページや画面を重ねたときに View が階層に残るか等) で裏を取る。

事後判定: 完了条件に端末・実環境での確かめがある change で、その確かめに使う画面・経路の名前が exploration または proposal に書かれている。

## 経緯

- 2026-10-07 android-compose-scroll-restore-with-multiple-hosts: Android の修正が Fragment にも届くことから、MAUI Android でページが 2 つ重なる構成を確かめる項目を完了条件に足した。サンプルのデモページに進む先が無く、既存の「離れて戻る」は同じページを開き直すだけだったため、ワーカーは確かめに入れず停止した。経路を足した後の実測では、MAUI は上にページを積むと下のページの View を階層から外すため、修正前後で観測に差が無く、探索時の見立て (ページが 2 つ重なったときだけ変わる) も外れていた (deviation 記録済み)。関連: 対象の実在を確かめずに要求を書く型として [[spec-requirement-targets-nonexistent-external-resource]] と同族。

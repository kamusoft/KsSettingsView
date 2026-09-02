---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-02
last-seen: 2026-09-02
evidence:
  - add-maui-nuget-distribution (名前空間改名で `KsSettingsView.SwitchCell` が MAUI 本体の同名型と衝突する件を消費者検証ワーカーが 1 件報告し、オーナー合意・deviation 記録・ホスト側 review-001 までその 1 件を前提に進んだ。相方レビューが `EntryCell` の同型衝突を検出。facade の公開型を列挙して MAUI Controls と突き合わせれば 2 件と数え切れた)
---

## ルール文

公開名前空間・型名の変更で「既存フレームワークの型と衝突する」事例を 1 件見つけたら、その 1 件を記録して終えず、変更対象の公開型を機械的に列挙 (public class/record/enum/interface/struct の名前一覧) して衝突相手の名前空間の公開型と突き合わせ、衝突の総数を確定してから deviation・オーナー確認・注意書きの対象にする。レビューは deviation に記録された衝突件数が列挙結果と一致するかを判定条件にする。

## 経緯

- 2026-09-02 add-maui-nuget-distribution: 消費者検証中の CS0104 で SwitchCell の衝突が見つかり、オーナーが「受け入れて注意書き」と決定。deviation と review-001 は 1 件を前提にしたが、相方レビューが EntryCell も衝突すると指摘 (採用)。実装側の lessons/impl.md L-003 (単一検索軸で網羅と判定しない) と同型の見逃しがレビュー側で再現した。
- 2026-09-02 add-maui-nuget-distribution (同一 change 内の再発、カウント外): 同じ形が deviation の記録でも起きた — .NET Android SDK が自動生成する自 assembly 用 aar を facade の nupkg で観測して deviation に記録したが、同じ機構で生成される Android binding 側の aar は記録せず、review-002 が Scenario「aar 2 本」との不一致として検出。1 件目を見つけた時点で「同じ機構の産物」を全成果物で数える動きが要る (型名衝突と同じ、1 件で数え終える誤り)。

---
scope: process
kind: success
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - add-sample-dark-mode-toggle (M 級の提案に対しホスト側自己レビュー [整合性チェックリスト・UI lint・spec-review L-002 の 3 軸] は指摘 0。相方 spec-review second-opinion-spec-001 が Major 6 件を検出し、うち 6 件すべてを採用 — Android の「システム」を `UiModeManager.setApplicationNightMode` へ写像できない [公開値 NO / YES / AUTO / CUSTOM に端末追随が無い] という設計の誤り、Manifest の DayNight 化が android/ADR-0020 の検証条件を崩す、月初〜月末の範囲では Compose DatePicker が月外日を描かないため範囲外 disabled が見えない、dark プリセットに description / valueText の色ロールが無い [Android の description 既定は夜間に追随しない固定値]、MAUI の固定ナビバーと「アプリ全体」の契約の矛盾、検証計画が 4 実行面を覆わない。提案の再設計 [Android の手段変更・モック改訂・spec 3 面改訂] を実装前に済ませ、実装後の手戻りは 0)
---

## ルール文

提案が platform API の特定の契約 (受け付ける値の集合・描画構造・イベントの発火条件) に依存して Scenario を書いているとき、ホスト側自己レビューの指摘 0 を終点にせず、config `second-opinion.spec-review` の相方レビューを通し、相方が挙げた API 契約の主張は公式リファレンスまたは実装ソースで裏取りしてから採否を決める。突き合わせ結果には、採用した指摘ごとに「裏取りに使った参照」と「改訂した成果物」を書く。事後判定: second-opinion-spec の突き合わせ表に、採用行ごとの参照と反映先がある。

## 経緯

- 2026-09-05 add-sample-dark-mode-toggle: 探索は `UiModeManager.setApplicationNightMode` を「`ComponentActivity` のまま切り替えられる」手段として採用確定していたが、AUTO / CUSTOM は位置・時刻の自動切替で「端末に追随する」値が無く、選択肢「システム」を実現できなかった。ホスト側は L-002 の 3 軸 (Requirement の交差・実体との突合・責務の閉路) を回したと報告しつつ、API が受け付ける値の列挙までは読んでいなかった。相方 (codex) は API リファレンスと AndroidX の DatePicker 実装を引いて 6 件を挙げ、全件がオーナー再判断と提案改訂につながった (Android は Activity の Configuration 上書き + `recreate()` へ変更、範囲は 06/01〜06/20、色ロール 2 つ追加、MAUI バーは対象外を明記、tasks 5 を 4 実行面の表に)。M 級で spec-review の相方並走を有効にしている config の設定 (`second-opinion.spec-review: [m, l]`) が効いた実例。

---
scope: spec-review
kind: pain
severity: normal
count: 2
first-seen: 2026-08-26
last-seen: 2026-09-01
evidence:
  - retarget-docs-refresh-to-skills (ホスト側の提案自己レビューは 2 周とも指摘 0 で通過したが、相方 second-opinion-spec-001 が Major 8 件を提示し、大半が採用されて spec とタスクの改稿に至った — 初期生成の責務衝突・AGENTS 規約の閉路・manifest v3 の規範スキーマ不在・README への追従経路欠落・機械チェック表と現行コードの食い違い・skills/ が identity lint 検査外。いずれも「同じ読み方をもう一周する」では出ない検査軸だった)
  - adopt-android-explicit-api-mode (ホスト側の提案自己レビューは指摘 0 だったが、相方 second-opinion-spec-001 が Major 3 件を検出し、公開 ABI 差分、Strict 陽性対照、外部 consumer probe の Requirement・Scenario・task を追加した。いずれも「Strict compilation 成功」だけでは区別できない失敗状態を別の観測軸で見つけた)
---

## ルール文

提案レビューで自己レビューが指摘 0 になったとき、周回を重ねたことを spec 妥当性の根拠にしない。指摘 0 で 1 周終わったら、読み直す代わりに検査軸を変えて当てる — (1) Requirement 同士の交差 (モード・フラグ Requirement × 機能 Requirement の掛け合わせ)、(2) 記述が参照する現行リポジトリの実体 (パス・バージョン取得元・lint scope) との突合、(3) 決定が作る責務の閉路 (この手順を禁じたとき、誰がその作業をやるのか)。完了報告に「自己レビュー N 周・指摘なし」とだけ書かず、どの軸で当てたかを書く。

## 経緯

- 2026-08-26 retarget-docs-refresh-to-skills: 提案フェーズのホスト側自己レビューは 2 周実施して指摘 0。同じ入力を渡した相方 (codex) の spec-review は NEEDS_DISCUSSION で Major 8 件を返し、うち初期生成の責務衝突・manifest v3 の規範スキーマ不在・機械チェック表の現行コードとの食い違い・skills/ の identity lint scope 漏れなどが採用され、spec と tasks が改稿された。8 件はいずれも「spec 本文を読み返す」ことでは見つからず、他フェーズとの責務関係・リポジトリ実体との突合・Requirement の掛け合わせという別軸で初めて見える型だった。周回数は独立性を持たない。
- 2026-09-01 adopt-android-explicit-api-mode: ホスト側の提案自己レビューは指摘 0 だったが、相方の spec-review は公開面の過剰降格を Strict が検出できないこと、Strict が no-op でも診断 0 になり得ること、同一 module テストでは internal を外部境界として検証できないことを Major 3 件として検出した。公開 ABI の前後差分、明示前の陽性対照、独立 build の Sample probe を追加して全件解消した。成功観測を作るには「診断 0」を再確認するのでなく、誤実装時にだけ失敗する観測へ軸を変える必要があった。

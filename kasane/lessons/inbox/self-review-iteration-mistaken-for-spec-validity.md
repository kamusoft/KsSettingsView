---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-26
last-seen: 2026-08-26
evidence:
  - retarget-docs-refresh-to-skills (ホスト側の提案自己レビューは 2 周とも指摘 0 で通過したが、相方 second-opinion-spec-001 が Major 8 件を提示し、大半が採用されて spec とタスクの改稿に至った — 初期生成の責務衝突・AGENTS 規約の閉路・manifest v3 の規範スキーマ不在・README への追従経路欠落・機械チェック表と現行コードの食い違い・skills/ が identity lint 検査外。いずれも「同じ読み方をもう一周する」では出ない検査軸だった)
---

## ルール文

提案レビューで自己レビューが指摘 0 になったとき、周回を重ねたことを spec 妥当性の根拠にしない。指摘 0 で 1 周終わったら、読み直す代わりに検査軸を変えて当てる — (1) Requirement 同士の交差 (モード・フラグ Requirement × 機能 Requirement の掛け合わせ)、(2) 記述が参照する現行リポジトリの実体 (パス・バージョン取得元・lint scope) との突合、(3) 決定が作る責務の閉路 (この手順を禁じたとき、誰がその作業をやるのか)。完了報告に「自己レビュー N 周・指摘なし」とだけ書かず、どの軸で当てたかを書く。

## 経緯

- 2026-08-26 retarget-docs-refresh-to-skills: 提案フェーズのホスト側自己レビューは 2 周実施して指摘 0。同じ入力を渡した相方 (codex) の spec-review は NEEDS_DISCUSSION で Major 8 件を返し、うち初期生成の責務衝突・manifest v3 の規範スキーマ不在・機械チェック表の現行コードとの食い違い・skills/ の identity lint scope 漏れなどが採用され、spec と tasks が改稿された。8 件はいずれも「spec 本文を読み返す」ことでは見つからず、他フェーズとの責務関係・リポジトリ実体との突合・Requirement の掛け合わせという別軸で初めて見える型だった。周回数は独立性を持たない。

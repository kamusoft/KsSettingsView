# 自己レビューの周回数を spec 妥当性の根拠にする

`lessons/spec-review.md` L-002 の経緯。

## ルール文

提案レビューで自己レビューが指摘 0 になったとき、周回を重ねたことを spec 妥当性の根拠にしない。指摘 0 で 1 周終わったら、読み直す代わりに検査軸を変えて当てる — (1) Requirement 同士の交差 (モード・フラグ Requirement × 機能 Requirement の掛け合わせ)、(2) 記述が参照する現行リポジトリの実体 (パス・バージョン取得元・lint scope) との突合、(3) 決定が作る責務の閉路 (この手順を禁じたとき、誰がその作業をやるのか)。完了報告に「自己レビュー N 周・指摘なし」とだけ書かず、どの軸で当てたかを書く。

## 経緯

- 2026-08-26 retarget-docs-refresh-to-skills: 提案フェーズのホスト側自己レビューは 2 周実施して指摘 0。同じ入力を渡した相方 (codex) の spec-review は NEEDS_DISCUSSION で Major 8 件を返し、うち初期生成の責務衝突・manifest v3 の規範スキーマ不在・機械チェック表の現行コードとの食い違い・skills/ の identity lint scope 漏れなどが採用され、spec と tasks が改稿された。8 件はいずれも「spec 本文を読み返す」ことでは見つからず、他フェーズとの責務関係・リポジトリ実体との突合・Requirement の掛け合わせという別軸で初めて見える型だった。周回数は独立性を持たない。
- 2026-09-01 adopt-android-explicit-api-mode: ホスト側の提案自己レビューは指摘 0 だったが、相方の spec-review は公開面の過剰降格を Strict が検出できないこと、Strict が no-op でも診断 0 になり得ること、同一 module テストでは internal を外部境界として検証できないことを Major 3 件として検出した。公開 ABI の前後差分、明示前の陽性対照、独立 build の Sample probe を追加して全件解消した。成功観測を作るには「診断 0」を再確認するのでなく、誤実装時にだけ失敗する観測へ軸を変える必要があった。
- 2026-09-02 add-consumer-verification: 提案の自己レビューは整合性チェックリスト (proposal / spec / tasks の相互参照) で 2 周通過。相方の spec-review は Gradle の `content { includeGroup }` が排他でない・NuGet の packageSourceMapping が global packages folder の既存パッケージに働かない・SwiftPM の path 参照では `Package.resolved` が生成されない、という外部機構の実挙動との突合で Critical 2 / Major 2 を検出し、さらに単一 `version` 入力 × platform 別既定版、workflow_call の runner 分離 × 成果物受け渡しという Requirement の交差で Major 2 を検出した。12 件採用で級も M → L に改めた。交差の型は 1 件 (`smoke` × `artifact`) が提案・実装レビューをすり抜け、相方コードレビューの Major で捕まった — 提案段階で「mode の各値 × 任意入力の各組み合わせ」を表にしていれば見えていた。

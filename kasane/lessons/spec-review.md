---
scope: spec-review
timestamp: 2026-09-05
---

# lessons: spec-review

- [L-001] spec の Scenario / Requirement が置く前提 (数値境界への到達可能性、復元・取得系なら参照元がその状態を実際に保持していること、WHEN の操作が公開面・画面構成で実際に実行可能であること等) は、入力型の値域やソース照合で成立可能性を検算してから提案に載せる。成立不能なら、その前提の Scenario を書かずに Requirement 本文で扱いを明記する (対象外化・防御的ガードの注記等) か、前提を成立可能な形に引き直す。同一 Requirement 内の他の SHALL と WHEN が矛盾しないことも検算対象。経緯は [details/spec-scenario-premise-unreachable-in-input-domain.md](details/spec-scenario-premise-unreachable-in-input-domain.md)。(昇格: 2026-08-12、出典: android-datepicker-spinner-wheel / clarify-host-attach-order-contract / add-maui-custom-cell)
- [L-002] 提案レビューで自己レビューが指摘 0 になったとき、周回を重ねたことを spec 妥当性の根拠にしない。指摘 0 で 1 周終わったら、読み直す代わりに検査軸を変えて当てる — (1) Requirement 同士の交差 (モード・フラグ Requirement × 機能 Requirement、任意入力同士の組み合わせ)、(2) 記述が参照する現行リポジトリ・外部機構の実体 (パス・バージョン取得元・lint scope・ビルドツールの実挙動) との突合、(3) 決定が作る責務の閉路 (この手順を禁じたとき、誰がその作業をやるのか)。完了報告に「自己レビュー N 周・指摘なし」とだけ書かず、どの軸で当てたかを書く。経緯は [details/self-review-iteration-mistaken-for-spec-validity.md](details/self-review-iteration-mistaken-for-spec-validity.md)。(昇格: 2026-09-02、出典: retarget-docs-refresh-to-skills / adopt-android-explicit-api-mode / add-consumer-verification)
- [L-003] 提案レビューで、spec が「現行を維持する」と定める画面・パラメータ (初期値・選択肢・寸法値・行構成・文言) について、mock が描く該当値を現行実装のソースと 1 つずつ照合し、一致しない値・行・文言は承認前に mock を現行へ修正させる。brief に「生値は非規範」と書くだけでは実装が mock の生値に引きずられた実例があるため、mock 側の値を合わせる。事後判定: brief.md の承認記録に、現行との照合を行った対象 (画面・値) が書かれている。経緯は [details/mock-shows-param-not-matching-current-impl.md](details/mock-shows-param-not-matching-current-impl.md)。(昇格: 2026-09-05、出典: align-sample-parity / fix-cell-accessory-vertical-fill / add-sample-dark-mode-toggle)

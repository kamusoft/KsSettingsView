# 経緯: 相方 APPROVED は「問題なし」の証明にならない (process L-002)

pain 3 例で 2026-08-09 に昇格。いずれも「相方 (クロスモデル) code-review が APPROVED (指摘 0) を返した同じサイクルで、ホスト側レビューが実測またはプロジェクト固有文脈による指摘を検出した」型。

- 2026-08-02 android-picker-selection-sheet: second-opinion-004 が APPROVED (指摘0) を返した同じサイクルで、ホスト review-003 が実 MotionEvent の実測により「リストを普通にスクロールしただけでシートが全画面へ飛ぶ」Major を検出。突き合わせは「矛盾ではなく検出力の差」と整理された。
- 2026-08-02 ios-picker-selection-parity: second-opinion-002 が APPROVED (指摘0) の同サイクルで、ホスト review-001 が comment-policy 違反 9 箇所 (grep 検出) と navigation bar appearance の丸ごと置換の Major 2件を検出。突き合わせに「クロスモデルでも project 固有規約由来の指摘はホスト側レビューが担う」と明記された。
- 2026-08-09 harden-update-accessory-unknown-id: second-opinion-002 が APPROVED (指摘0) の同サイクルで、ホスト review-001 が CHANGES_REQUESTED (Major 1 / Minor 2 / Suggestion 2) — MAUI 層3箇所に残る新契約と正反対のコメント (Major)、Android 契約表ケースの回帰検出力欠落 (ミューテーション実測で空振りを証明、Minor)、`@discardableResult` による型ガードの無効化 (Minor) を検出。突き合わせで「ホスト側の全 5 指摘は相方の見逃し」と記録された。

観測の共通構造: 相方が静的レビューで見えない領域は (1) 実測でしか判定できない性質 (動的挙動・回帰検出力)、(2) プロジェクト固有の文脈 (規約・既存コメントとの整合・concepts の契約記述)。この2領域はホスト側レビューが固定責務として担う。

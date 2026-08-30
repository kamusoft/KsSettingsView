# spec-silent-visible-change-not-recorded (process L-001 の経緯)

inbox パターンとして pain 3 件で閾値到達し、2026-08-08 にオーナー承認で `process.md` L-001 へ昇格した。

## 観測 (3 change)

- 2026-08-01 fix-android-cell-width-allocation: (1) titleView の weight 化で ButtonCell の `titleAlignment` (既定 CENTER) が通常レイアウトで初めて実効化し、既存利用コードの title が左寄せ→中央寄せに変わる副作用が一切記録されていなかった。(2) title の 1 行化 (複数行折り返しの廃止) が未合意のまま brief.md の「合意済み妥協 / 申し送り」節に置かれていた。両方とも review-001 (NEEDS_DISCUSSION) の指摘で発覚し、事後にオーナー承認 → deviation.md 記録となった。
- 2026-08-02 android-picker-selection-sheet: APPROVED (review-005) 後にオーナー指示で入った配色変更 (確定ラベル色を輝度導出の白/黒から `Theme.backgroundColor` へ) が、新しいスタイリング契約の追加なのに deviation.md 未記録のまま進み review-006 が Minor 指摘。裁定自体は済んでいたため記録漏れのみだが、「裁定→即記録」が徹底されていない。
- 2026-08-08 release-host-without-bridge-dispose: `releaseHost()` → `makeHost*` の再生成で解放前に表示していた root header / footer が消えることを実装者が E2E で検出し、理由を検証ホスト (`maui/tests/shared/KsBridgeScenario.cs`) の doc コメントに記録したのみで、deviation.md 未記録・オーナー合意なしのまま完了報告に至った。review-001 が「この変更をアーカイブすると知識が失われる」と Major (NEEDS_DISCUSSION) で差し戻し、オーナー裁定 (現状を仕様として確定) → deviation.md 記録 + Bridge 公開 API doc 明記で決着。spec 文言には違反しないため verify は VALID のままで、記録の欠落は独立レビューだけが検出した。

## 共通構造

3 件とも「spec の語彙 (Scenario の THEN・部位対応表) の外側で起きる利用者可視の変化」であり、テスト green + verify VALID を素通りして独立レビューの差し戻しで初めて記録された。乖離の発生時点記録は ksn-orchestrator の責務 (ksn-core delta-spec: 記録のない乖離だけが問題) だが、規律として徹底されなかったため process scope のルールに昇格した。

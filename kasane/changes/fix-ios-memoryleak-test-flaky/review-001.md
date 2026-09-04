# レビュー結果: fix-ios-memoryleak-test-flaky (001 回目)

**日付**: 2026-09-04
**判定**: APPROVED

## サマリー

合意した 3 つの解放ケースだけが、main queue の固定 1 回待機から weak 参照の `nil` 条件を直接観測する既存の条件待機へ置き換えられている。待機クロージャは weak 変数を参照するだけで Controller / Host を強保持せず、製品コード・公開 API・UI に差分はない。Critical / Major / Minor / Suggestion の指摘はない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` — 常時適用。変更行に新規コメントはなく、対象ソースを本文の禁止類型とも照合した。
- `kasane/handbook/cross/test-execution.md` — Simulator テスト、実行件数の確認、収束条件そのものを待つアサーションの規約を適用した。
- `kasane/handbook/cross/runtime-behavior-verification.md` — タイミング依存の flaky 修正として、修正前観測・修正後反復・検出力確認・全件確認の証跡を照合した。
- `kasane/handbook/ios/swift6-language-mode-check.md` — 適用条件を確認した。今回は `ios/Tests/**` のみの変更で `ios/Sources/**` を触らないため、Swift 6 一時設定ビルドは適用外である。変更テストターゲットは通常の Simulator 全件実行でコンパイル済み。
- `kasane/lessons/code-review.md` の L-001 — 回帰検出力は、一時的な強参照を残すミューテーションで対象テストターゲットごとに deadline 超過を実測した証跡と、最終差分に一時変更が残っていないことを照合した。

## 合意スコープとの照合

- `exploration.md:21-26` が合意した対象は、Controller 直接生成、Store 経由の Controller、Bridge の旧 Host の 3 ケースである。実装差分は `ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:28-32`、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:55-59`、`ios/Tests/KsSettingsViewBridgeTests/KsBridgeHostReleaseTests.swift:146-150` のみで、対象の過不足はない。
- 3 ケースとも `until` が weak 参照の `nil` を直接判定する。`actual` も timeout 時点の `nil` / `non-nil` を報告する。
- `actual` と `until` は non-escaping クロージャで、weak ローカル変数を読むだけである。対象オブジェクトをクロージャの capture list や別の強参照へ取り込んでいない。
- 既存 `awaitCondition` は `ios/Tests/KsSettingsViewTestSupport/ConditionWait.swift:35-57` で単調時計による実時間 deadline、RunLoop への実行機会の譲渡、超過時の実測値付き failure を備える。
- timeout 後の既存 `XCTAssertNil` は条件待機の failure と同じ不具合を再度報告し得るが、条件待機は黙って戻らず最初の failure を必ず発火する。ミューテーション時の 2 failures はこの制御フローと一致し、偽陽性・偽陰性を生む穴ではない。
- 変更ファイル一覧に製品コード、`ios/Package.swift`、公開 API、Sample / UI は含まれない。

## テスト・証跡の確認

- レビュアー実行: iPhone 17 Pro / iOS 26.5 Simulator、scheme `KsSettingsView`、絞り込みなしで **1000 tests / 0 failures**。`evidence/full-suite-after-fix.md:5-14` と一致した。
- レビュアー実行: `KsBridgeHostReleaseTests` と `MemoryLeakTests` を `-test-iterations 10` で **90 test runs / 0 failures**。内訳は 7 件 × 10 と 2 件 × 10 で、`evidence/repeat-run-after-fix.md:5-10` と一致した。
- `evidence/detection-control.md:12-17` は、UI テストターゲットと Bridge テストターゲットの双方で強参照ミューテーションが deadline 超過し、`weakController=non-nil` / `weakHost=non-nil` と 2 failures を報告したことを記録している。実リーク相当を成功扱いしない回帰検出力がある。
- `python3 scripts/comment-policy-lint.py --summary` は検査対象 761 ファイルで禁止 0 件、`python3 scripts/local-path-lint.py` と `python3 scripts/identity-lint.py` も違反 0 だった。証跡にローカル絶対パス、Simulator UDID、個人・秘密の識別値はない。
- `git diff --check` は成功した。

## 指摘事項

なし。

## 残余リスク

- 初回 CI failure の XCTest 詳細ログが取得できないため、元の failure が expectation timeout か、その後の weak 参照非 `nil` かは確定していない。この不確実性は `exploration.md:24-28` と `evidence/detection-control.md:3-6` に明示され、原因を UIKit / Simulator 内部の一時保持と断定していない。
- 条件待機にも 3 秒の上限があるため、極端な負荷で解放がそれを超えれば failure になる。ただし成立時は即時 return し、固定 1 回待機より観測対象に忠実で、10 反復とミューテーションの双方で意図した成功・失敗を確認できている。

## アクションプラン

追加修正なし。現在の実装と証跡のまま次の完了ゲートへ進められる。

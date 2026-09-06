# Exploration: fix-ios-memoryleak-test-flaky

## 課題 / 動機

iOS の UI テスト `MemoryLeakTests` の「KsSettingsViewController はスコープを抜けると deinit される」(`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:29`) が、GitHub Actions の macOS ランナー上で非同期待機のタイムアウトにより失敗することがある。

発見の文脈: add-release-workflow の dry-run リハーサル (release.yml run 33836940680、2026-09-04) の attempt 1 で `ios / verify` (phase-3 の `verify-ios.yml`、無改修) がこのテストだけで失敗した。同じ commit の PR #8 の CI では同じ job が pass しており、再実行 (attempt 2) でも pass した。失敗時は release run の macOS 3 job (ios / maui / package-maui) と PR #8 の CI が同時に走っており、Simulator の負荷に左右される不安定テストと見られる。release workflow は test 段の失敗で publish に進まないため、不安定テストはリリースの再実行を 1 回増やす (今回は初回リリースの本番 run では発生しなかった)。証跡: `kasane/changes/archive/*-add-release-workflow/evidence/github-actions-runs.txt` 11 節。

## 検討した選択肢 (却下案と理由を含む)

- CI で失敗した Controller 解放テスト 1 件だけを直す案: 同じ解放観測を行うもう 1 件の Controller テストと Bridge のホスト解放テストに、同型の不安定要因が残るため却下した。
- 同じ解放観測を行う iOS テストをまとめて安定化する案: 対象がテスト 2 ファイルに収まり、同種の再発を防げるため採用した。
- メインキューの処理を 1 回だけ待つ時間を延長する案: 解放条件を観測せず、高負荷時の誤失敗を時間の長さで減らすだけになるため却下した。
- weak 参照が `nil` になることを既存の条件待機で直接観測する案: 実時間 deadline、RunLoop への実行機会の譲渡、超過時の実測値報告を備え、収束待ちの規約に一致するため採用した。
- 負荷時に解放が遅れる UIKit / Simulator 内部の機序まで特定する案: flaky の再現待ちや Instruments 計測へ調査が広がる一方、既知の観測不備を是正する目的には不要なため対象外とした。
- 解放条件の観測を正しくし、同型テストと CI を安定化する案: 不確かな内部機序を真因と断定せず、既知の不備と再発範囲へ変更を限定できるため採用した。

## 決定事項

- add-release-workflow のスコープ外として簡易起票する (オーナー指示 2026-09-04)
- `MemoryLeakTests` の Controller 解放 2 ケースと、同じ待機パターンを持つ Bridge のホスト解放テストを変更対象に含める (オーナー指示 2026-09-04)
- 解放確認はメインキューの処理回数や固定時間ではなく、weak 参照が `nil` になる条件を `KsSettingsViewTestSupport` の既存条件待機で観測する (`kasane/handbook/cross/test-execution.md` の収束待ち規約による)
- 完了目標は解放観測の是正と同型テスト・CI の安定化とし、負荷時の一時保持を生む UIKit / Simulator 内部の機序の特定は含めない (オーナー指示 2026-09-04)
- 修正前の実環境再現は、GitHub Actions run 33836940680 の attempt 1 で失敗し、同一 commit の attempt 2 で成功した記録を使う
- 修正後は Controller / Bridge の対象クラスを Simulator で 10 回連続実行し、絞り込みなしの iOS 全件テストも実行件数付きで確認する
- 解放検出力は、一時的に強参照を残した状態で条件待機が deadline 超過として失敗することを対象テストターゲットごとに確認し、その後に一時変更を戻して成功を確認する
- 初回 attempt の XCTest 詳細ログは現在取得できないため、expectation timeout と待機後の weak 参照非 `nil` のどちらだったかは断定しない
- 製品コードの静的な保持経路には明らかな循環参照がなく、テスト間の状態共有を原因とする証拠もない。UIKit / Simulator 内部の一時的な保持の特定は完了目標に含めない
- 変更級は S とする (オーナー指示 2026-09-04)

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- なし

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S (オーナー確定)

理由: iOS の単一能力内にある解放テスト 2 ファイルのバグ修正で、製品コード・公開 API・UI・データスキーマ・既存 ADR の決定に触れず、既存の共通条件待機を再利用する局所的で可逆な変更に閉じるため。

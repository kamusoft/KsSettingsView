# Exploration: fix-ios-memoryleak-test-flaky

## 課題 / 動機

iOS の UI テスト `MemoryLeakTests` の「KsSettingsViewController はスコープを抜けると deinit される」(`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:29`) が、GitHub Actions の macOS ランナー上で非同期待機のタイムアウトにより失敗することがある。

発見の文脈: add-release-workflow の dry-run リハーサル (release.yml run 33836940680、2026-09-04) の attempt 1 で `ios / verify` (phase-3 の `verify-ios.yml`、無改修) がこのテストだけで失敗した。同じ commit の PR #8 の CI では同じ job が pass しており、再実行 (attempt 2) でも pass した。失敗時は release run の macOS 3 job (ios / maui / package-maui) と PR #8 の CI が同時に走っており、Simulator の負荷に左右される不安定テストと見られる。release workflow は test 段の失敗で publish に進まないため、不安定テストはリリースの再実行を 1 回増やす (今回は初回リリースの本番 run では発生しなかった)。証跡: `kasane/changes/archive/*-add-release-workflow/evidence/github-actions-runs.txt` 11 節。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

- add-release-workflow のスコープ外として簡易起票する (オーナー指示 2026-09-04)

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- deinit の待機が負荷時にタイムアウトする原因 (autoreleasepool の解放タイミング・待機時間の短さ・テスト間の状態共有のいずれか) の切り分け
- 待機時間の延長で足りるか、deinit の観測方法 (weak 参照 + RunLoop の回し方) を変えるべきか
- handbook cross/runtime-behavior-verification.md と test-execution.md の規律 (実機での確認・実行件数の検査) との整合

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (テスト 1 件の修正に閉じれば S 級の見込み)

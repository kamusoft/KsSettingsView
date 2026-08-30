# Deviation: harden-compose-settingsroot-dsl

proposal / デルタスペックと実際の作業の差分を記録する (spec 本体は書き換えない)。

- `.gitignore`: proposal の What Changes に含まれない → 指示によりルート `.gitignore` の Android / Gradle セクションへ `.kotlin/` (Kotlin 2.x のビルドセッション作業ディレクトリ) を追加。理由: 相方レビューが未追跡の `android/.kotlin/` を検出し、別 change に切り出すまでもない 1 行の既存不備としてオーナーが本 change 内での対応を指示 (2026-08-22)
- `DSLAccessoryVisibilityRenderingTest.kt`: proposal の対象は `settingsRoot` builder と DSL marker のみ → 指示により既存 flaky (Store 経路のみ待機して DSL 経路と比較・`awaitRows` の `Thread.yield()`) の修正を本 change 内で実施。理由: review-001.md の Minor-1 として検出されたが、別 change に切り出さず一度に片付けるとオーナーが判断 (2026-08-22)
- `KsSettingsViewTestSupport.kt` (`android/ks-settingsview-ui`): proposal の対象 module は `ks-settingsview-compose` のみ → 指示により `awaitConvergence` / `awaitDifferCommit` の待機ループ内 `Thread.yield()` を `Thread.sleep(1)` へ置換。理由: `concepts/cross/conventions/test-execution.md` が待機の適用実例として名指しする関数が規約 (yield ではなく sleep で譲る) に反したままだったため、オーナーが本 change 内での是正を指示 (2026-08-22)。同 module の `CustomCellRecycleTest.pump()` / `CustomCellBuilderReleaseTest.pump()` は反復回数で区切る形で条件ベースへの作り替えが必要なため、指示により本 change では対象外とし申し送りに残す

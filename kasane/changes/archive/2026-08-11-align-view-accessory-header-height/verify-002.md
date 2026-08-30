# 一致検証: align-view-accessory-header-height (002 回目)

**日付**: 2026-08-11
**判定**: VALID

デルタスペック `specs/settings-view-android-ui/spec.md` の 2 Requirement / 11 Scenario に加え、`deviation.md` の追加契約 1 件を対象に、実装とテストの対応を突き合わせた。

パスは以下の略記を使う:

- `SAVH` = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt`
- `KSLA` = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt`
- `VAHHT` = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ViewAccessoryHeaderHeightTest.kt`
- `SART(and)` = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryRenderingTest.kt`
- `FUCST` = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/FullUpdateContentSyncTest.kt`
- `LADT` = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ListAdapterDiffTest.kt`
- `SART(ios)` = `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift`

## Requirement: Section Header の固定高さは accessory 種別に依らず適用される (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| view accessory + Section.headerHeight 正値で固定高さになる | `SAVH:322-346` (`applySectionHeaderHeight`)、`SAVH:147-155` (`bind`)、`KSLA:167-173` (伝搬) | `VAHHT:122` (layoutParams)、`VAHHT:227` (内容超過でも指定値を維持 = clip の measure 検証)、`ui/verification/android-header-height-states.png` + `-scrolled.png` (境界外の赤帯が 1px も出ない画素計数)、`SART(ios):test_viewヘッダはSectionのheaderHeight正値で固定高さになる` | ✅ 一致 |
| view accessory + Theme.headerHeight フォールバック | `SAVH:328-333` (解決順の 2 段目) | `VAHHT:135`、`SART(ios):test_viewヘッダはThemeのheaderHeightにフォールバックする` | ✅ 一致 |
| view accessory + Section と Theme が両方正値なら Section が勝つ | `SAVH:328-333` (`headerHeight > 0.0` が Theme より先) | `VAHHT:149`、`SART(ios):test_viewヘッダのSection指定はThemeより優先される` | ✅ 一致 |
| view accessory + 高さ未指定は自動高さのまま | `SAVH:334-339` (`WRAP_CONTENT`) | `VAHHT:163`、`SART(ios):test_viewヘッダは高さ未指定なら内容に応じた自動高さになる` | ✅ 一致 |
| 固定高さの Header と自動高さの Header が混在しても互いに影響しない | `SAVH:340-344` (bind ごとに必ず目標値へ戻す) | `VAHHT:176` (同一 ViewHolder を固定→自動→固定で再利用)、`SART(ios):test_固定高さと自動高さのviewヘッダが同一list内で互いに影響しない`、`ui/verification/android-header-height-rebound.png` (スクロール往復後の再表示) | ✅ 一致 |
| Footer の view accessory は高さ指定の対象外 | `SAVH:329` (`!isHeader -> -1.0`)、`KSLA:177-181` (Footer は headerHeight を渡さない) | `VAHHT:205`、`SART(ios):test_viewフッタはheaderHeightの影響を受けない` | ✅ 一致 |
| text accessory の高さ解決は変更されない | `SAVH:75-80` (Text 側も同一の `applySectionHeaderHeight` へ委譲)、`KSLA:160-166` | `SART(and):227` (正値)、`:248` (`-1` で WRAP_CONTENT)、`:267` (Theme フォールバック)、`:289` (Section 優先) — いずれも既存テストが無改変で PASS | ✅ 一致 |

## Requirement: 表示済み Header の headerHeight 変更は hosted view を維持したまま反映される (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 自動高さ → 固定高さへの動的変更 | `KSLA:416-419` (`isSameHeaderHeight` が高さ差を内容差に)、`KSLA:367-384` (`getChangePayload`)、`KSLA:204-216` (3 引数版振り分け)、`SAVH:171-179` (`applyHeaderHeight`) | `VAHHT:344` (Store→Host→RecyclerView の実経路) | ✅ 一致 |
| 固定高さ → 自動高さへの動的変更 | 同上 | `VAHHT:354` | ✅ 一致 |
| 固定高さの値変更 | 同上 | `VAHHT:367` | ✅ 一致 |
| 高さのみの変更で view の内部状態が維持される | `KSLA:204-216` (高さ payload は `bindKsAnyView` を通さない)、`KSLA:247-255` (`PAYLOAD_HEADER_HEIGHT`) | `VAHHT:376` (`EditText` の入力内容と View インスタンス同一性)、`FUCST:「View accessory の Header は高さ payload で中身を作り直さずに固定高さだけを更新する」`、`FUCST:「View accessory の Header では headerHeight の差が高さ payload 付きの通知になる」`、`LADT:「SectionHeader View は headerHeight の差で areContents 不等価になり高さ payload が付く」` / `「…中身と headerHeight が同時に変わると内容 payload が付く」` | ✅ 一致 |

## deviation.md 記録済みの追加契約

| 追加契約 | 実装 | テスト | 状態 |
|---|---|---|---|
| 固定高さが解決されたとき hosted view を領域いっぱいに配置する (iOS の 4 辺 pin と対称) | `SAVH:359-373` (`applyHostedViewFill`)、`SAVH:171-179` から固定/自動の双方で呼ばれる | `VAHHT:258` (固定時は領域いっぱい)、`VAHHT:280` (自動時は内容なり)、`VAHHT:302` (再利用での往復)、`VAHHT:397` (高さのみ変更時の追随)、`ui/verification/android-header-height-states.png` の画素検査 (状態B の y 398-493 = 96px が一様に accessory 背景色) | ⚠️ deviation 記録済み |

## 追加検査

- **tasks.md の虚偽チェック**: 全 12 タスクが `[x]`。上表の対応と突き合わせて、未実装のままチェックされているものはなし
  - 1.1〜1.3 → `SAVH:147-179` / `KSLA:167-181` / `SAVH:322-346` の共通化で充足
  - 1.4〜1.5 → `KSLA:204-216` / `:367-384` / `:416-419`、旧契約テストは `LADT` / `FUCST` で新契約へ置換済み (旧アサーション `assertTrue(areContentsTheSame)` / `emptyList<ChangeRecord>()` は残っていない)
  - 2.1〜2.4 → `VAHHT` 15 件 + `SART(and)` 既存 4 件 + `SART(ios)` 新規 6 件
  - 3.1〜3.3 → 下記のテスト実行結果と `ui/verification/` 3 枚
- **逆流検査**: `git diff HEAD -- kasane/changes/align-view-accessory-header-height/` は `tasks.md` (チェック反転のみ) と `ui/brief.md` (「視覚照合結果」節の追記のみ) の 2 ファイル。`proposal.md` / `specs/` / `exploration.md` は無変更
- **未記録乖離**: ❌ なし。対応表に欠落・乖離はない
- **UI 変更の記録**: `ui/brief.md:27` に承認モック (`mock/height-states.html` → `approved.png`、2026-08-11 ユーザー承認) の記録あり。`:31-38` に実装後の視覚照合結果があり、合意済み妥協は 0 件。deviation の追加契約反映後に `verification/` 3 枚を再取得済み (states/rebound の SHA-256 は `5c4c8dda…`、scrolled は `39a34796…`)
- **テスト実行 (実施して確認)**:
  - Android: `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` 集計で **2222 件 / failures 0 / errors 0 / skipped 0** (1111 件 × debug/release)。うち `ViewAccessoryHeaderHeightTest` は 15 件 × 2 variant
  - iOS: `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` → `Executed 457 tests, with 0 failures` / `** TEST SUCCEEDED **`

## 判定

**VALID** — 全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」。虚偽チェックなし、足場の逆流なし、テストは Android / iOS とも全件成功。

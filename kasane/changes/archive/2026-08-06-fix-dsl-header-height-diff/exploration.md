# Exploration: fix-dsl-header-height-diff

- 起票日: 2026-08-05
- 起票経緯: [fix-android-header-height-refresh](../archive/fix-android-header-height-refresh/exploration.md) の実機検証中に Android 側を発見し、続けて iOS 側も同型と確認。実装は未着手・議論も未実施の簡易起票

## 課題

宣言的 DSL (Android Compose / iOS SwiftUI) で `Section` の `headerHeight` だけを動的に変えても、diff が生成されず表示が更新されない。**両 platform に存在する**。Store / View API 経由の修正 ([fix-android-header-height-refresh](../archive/fix-android-header-height-refresh/exploration.md)) では解消しない別レイヤーの欠落。

## 構図 (2026-08-05 コード裏取り)

両 platform とも `DSLDiffCalculator.compute` が旧/新ツリーを比較して `SettingsRootDiff` 列を返す構造だが、**headerHeight の変化に対応する diff をどの段でも生成しない**。到達の仕方だけが異なる。

| | Android ([DSLDiffCalculator.kt](../../../android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt)) | iOS ([DSLDiffCalculator.swift](../../../ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift)) |
|---|---|---|
| 早期 return の条件 | `sameStructure()` = Section の id / header / footer と cells の size・id を比較。**headerHeight を含まない** | `old.sections == new.sections` の値等価。`Section.==` は headerHeight を含む |
| headerHeight のみ変更時 | **早期 return で `emptyList()`** — 以降の段に到達しない | 早期 return は**抜ける** (変化を検知できている) |
| その後の各段 | — | `sectionLevelDiffs` は id ベース (削除・追加・移動)、`1.5` は header/footer accessory のみ → **どの段も diff を出さず結果的に空** |
| 呼び出し側のフォールバック | なし (空リストなら何もしない) | なし (`for diff in diffs` を回すだけ) |

結果はどちらも「diff 0 件 → 表示が更新されない」で同じ。iOS のほうが惜しい形 (変化を検知しているのに出力する diff がない)。

- header text の変更は両 platform とも早期 return を抜けて `UpdateAccessory` が出るため正常
- `isVisible` は両 platform とも preflight で `Full` に落として拾われている
- 問題は `headerHeight` に限定される

## 到達経路の切り分け

| 経路 | headerHeight 変更の反映 |
|---|---|
| Store / View API / Bridge (Android) | 反映される (fix-android-header-height-refresh で修正済み。実機 A/B 確認済み) |
| **Compose DSL (Android)** | **反映されない (本 change の対象)** |
| **SwiftUI DSL (iOS)** | **反映されない (本 change の対象)** |
| Store / View API (iOS) | 未確認 (iOS は `visibleSections` 更新後の `invalidateLayout()` で sectionProvider が再評価される設計) |

## 修正方向の議論と決定 (2026-08-05 探索で確定)

**A を採用**: 可視性変化と同型の preflight で headerHeight 差を検出し `Full` に落とす。

決定の根拠 (コード裏取りで判明した追加事実):

- **`ReplaceSection` は両 platform とも内部で常に Full 経路に倒れる** — Android は `applyDiff` の `ReplaceSection` 分岐が `setRootDirect` 相当で処理 ([KsSettingsView.kt:473](../../../android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt))、iOS は `applyFullSnapshot` 相当で処理 ([KsSettingsViewController.swift:1350](../../../ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift))。したがって B の「局所更新で安い」という想定優位は存在せず、A と B の実行コストは同じ Full 出口に合流する
- 「Full = 全再描画」ではない。両 platform とも全体を**突合**するだけで、再描画は差分のみ (Android は DiffUtil が header 行の高さ差を内容差として payload rebind [android/ADR-0012]、iOS は snapshot 不変 + `invalidateLayout()` で supplementary サイズのみ再評価)

却下案:

- **B: 1.5 段で `ReplaceSection` を発行** — 局所性の優位が誤認だった上、複数 Section の高さが同時に変わると Full が複数回走り A より悪化する。新しい発行パターンを増やす一貫性の欠点もある
- **C: Android の `sameStructure` に headerHeight 比較を足すだけ** — 早期 return は抜けるが以降の段が何も出さず単独では不成立 (iOS の現状と同じ形になるだけ。コード確認済み)
- **`SettingsRootDiff` に headerHeight 専用種別を足す** — 公開 API 変更で級が上がる割に、既存の Full で表現できるため不要

## 決定事項

- 修正方向は A (preflight で Full)。可視性 preflight (`containsVisibilityChange`) と同型で両 platform に入れる。Android は `contentUpdates` も可視性時と同様に空を返す対称処理が要る点に注意
- OS 対称性は core/ADR-0018 のとおり「実装形は platform 自由・観測結果の等価のみ保証」。今回は結果的に両 platform 同型の preflight になる
- 本 change の実装は ADR-0018 の対称テスト義務の最初の適用例 — 両 platform の DSLDiffCalculator テストに「headerHeight のみ変更 → 表示更新の diff が出る」を追加する (Store 経由側のテストは fix-android-header-height-refresh で追加済み)

## 提案への申し送り

- デルタスペックは「headerHeight + Cell 内容の**同時変更**」シナリオを明記すること。Android は Full が `setRootDirect` の内容通知を内包するため安全だが、iOS は `.full` 適用時の内容反映機構に疑いがある ([fix-ios-full-content-refresh](../fix-ios-full-content-refresh/exploration.md) — 可視性 preflight にも既存する同型の疑い)。スペックでは観測結果 (高さと内容の両方が反映される) を要求し、iOS 側の担保方法は実装判断とする

## 級の推奨

**M** — コード差分自体は小さい (preflight 関数 + テスト) が、2 platform に跨り、ADR-0018 対称テスト義務の初適用として仕様との対応を verify で追跡する価値があるため、迷ったら1段上の原則で M とする。公開 API 変更なし・可逆。

## 関連

- 出典: [fix-android-header-height-refresh](../archive/fix-android-header-height-refresh/exploration.md) の実機検証 (2026-08-05) — [verification/README.md](../archive/fix-android-header-height-refresh/verification/README.md)
- 関連 ADR: android/ADR-0012 (Section H/F 内容検出の DiffCallback 化)
- 関連 ADR: core/ADR-0018 (Store と DSL の観測結果対称性 + 対称テスト義務 — 本件を出典に 2026-08-05 起票・accepted)。本 change の実装は headerHeight の Store 経由 / DSL 経由の対称テストが ADR-0018 の最初の適用例になる想定。概念化は `concepts/core/architecture/declarative-ui-bridge.md`「両方式の観測結果対称性」節

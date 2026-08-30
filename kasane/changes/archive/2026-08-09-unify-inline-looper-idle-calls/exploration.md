# Exploration: unify-inline-looper-idle-calls

## 課題 / 動機

[consolidate-robolectric-wait-helpers/review-001.md](../consolidate-robolectric-wait-helpers/review-001.md) の Suggestion (同パッケージに残るインライン `shadowOf(Looper.getMainLooper()).idle()`) を起点に、共有ヘルパ `KsSettingsViewTestSupport.kt` の `idle()` へ呼び出しを寄せる。共有ヘルパの存在を知らない書き手が素の Looper 待機を書き、やがて `private fun idle()` を再び切り出す — という再重複の芽を摘むのが目的。

## 現状の実態 (探索 2026-08-09)

| 対象 | 実測 |
|---|---|
| インライン `shadowOf(Looper.getMainLooper()).idle()` | 26箇所 / 7ファイル (共有ヘルパ本体の1定義を除く) |
| `idleFor(Duration.ofSeconds(2))` | 14箇所 / 4ファイル (`DateSelectionSheetTest` / `KsWheelViewTest` / `NumberSelectionSheetTest` / `PickerSelectionSheetTest`) |

ファイル別のインライン idle 数: `PickerDialogRecreationTest` 11 / `ContentUpdatePayloadTest` 4 / `DatePickerDialogIntegrationTest` 4 / `DatePickerTodayShortcutTest` 3 / `FullUpdateContentSyncTest` 2 / `KsWheelViewTest` 1 / `PickerSelectionSheetTest` 1。

**import の後始末**: 置換後に `android.os.Looper` / `org.robolectric.Shadows.shadowOf` を除去できるのは5ファイル。`KsWheelViewTest` と `PickerSelectionSheetTest` は `idleFor` が残るため import 継続。

### 探索中の発見: 名前違いの待機ヘルパ重複

`awaitDifferCommit` が2ファイルに**完全一致で重複**している ([ContentUpdatePayloadTest.kt:49-67](../../../android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ContentUpdatePayloadTest.kt) / [FullUpdateContentSyncTest.kt:61-79](../../../android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/FullUpdateContentSyncTest.kt))。待機ループの骨格 (deadline → while → idle → condition 判定 → `Thread.yield()`) は共有ヘルパの `awaitConvergence` と同一で、違いは観測対象のレイヤのみ (`awaitConvergence` は `KsSettingsView` 経由 / `awaitDifferCommit` は `KsSettingsListAdapter` 直接)。

随伴する `committedSummary` も2ファイルにあるが、CellRow の要約だけ意図的に異なる (`EntryCell.text` と `LabelCell.title` — テスト対象の Cell 型が違うため)。

前変更の調査・レビューがいずれも「`idle` / `awaitConvergence` / `committedTexts` / `visibleRowTexts` の4つの関数名」で重複を探したため、名前の違う `awaitDifferCommit` を取りこぼしていた。

## 検討した選択肢 (却下案と理由を含む)

- **案A: インライン `idle()` の置換のみ (依頼どおりのスコープ)**
  - 機械的置換で等価性の検証が容易。ただし `awaitDifferCommit` という名前違いの同じ重複が残り、動機 (再重複の芽を摘む) に対して中途半端
- **案B (推奨): 案A + `awaitDifferCommit` を共有ヘルパへ集約**
  - 前変更で `awaitConvergence` に追加済みの `extraDiagnostics: (() -> String)?` がそのまま使えるため、`committedSummary` の意図的な差分は呼び出し側に残したまま関数本体だけ共有できる。追加の設計判断が不要
- **案C (却下): 待機ループの骨格を共通基盤関数へ統合し `awaitConvergence` / `awaitDifferCommit` の双方から呼ぶ**
  - 観測レイヤ (View 基点 / Adapter 基点) の一般化という設計判断が要る一方、重複解消の効果は案B とほぼ同じ。費用対効果で見送る

## 決定事項

- 案B (インライン `idle()` 置換 + `awaitDifferCommit` の共有ヘルパ集約) を S 級として採用 — ユーザー確定 (2026-08-09)
- `idleFor(Duration)` を使う14箇所は待機の意味が異なる (時間経過を進める) ため対象外 — 依頼時点で確定
- `committedSummary` の CellRow 要約の差分 (`EntryCell.text` / `LabelCell.title`) は意図的な差異として呼び出し側に残し、`extraDiagnostics` 経由で失敗メッセージに渡す
- 案C (待機ループ骨格の共通基盤化) は見送り。効果が案B とほぼ同じ一方で観測レイヤの一般化という設計判断を要するため
- 先行変更のコミットを待たず、同一作業ツリーで続けて実装する (ユーザー確定)

## ADR 候補

- なし (テスト専用・可逆・境界を越えないため ksn-core の選別基準に該当せず)

## 未決の論点

- `PickerDialogRecreationTest` の `settle(activity)` (`:727-729`) など、各ファイルローカルの複合ヘルパ (待機 + FragmentManager の pending transaction 実行など) は対象外とする。単一ファイル内でしか使われず重複していないため (内部のインライン `idle()` 呼び出しは置換対象に含む)
- `committedSummary` 自体の共有化 (差分を引数化して1つにまとめる) は今回行わない — 2定義の差異がテスト対象の Cell 型に由来し、共有すると呼び出し側の意図が読みにくくなるため

## UI 素材

- なし (UI 変更を含まない)

## 変更級の推奨: S (理由)

テストコードのみ・production コード無変更・公開 API 変更なし・単一プラットフォーム (Android)・完全に可逆・UI なし。案B を採っても触るファイル数は案A と同じ7ファイルで、全件テストによる等価性検証が可能。

## 前提・注意

先行変更 [consolidate-robolectric-wait-helpers](../consolidate-robolectric-wait-helpers/) が**未コミット**であり、本変更はその成果物 `KsSettingsViewTestSupport.kt` に依存する。同一作業ツリーで続けると両変更のコミットが混ざるため、先行変更のコミット後に着手するのが望ましい。あわせて並行作業中の docs-refresh 由来の変更 (`README.md` / `docs/*.md` 8ファイル / `.agents/skills/docs-refresh/SKILL.md`) も作業ツリーに存在する。

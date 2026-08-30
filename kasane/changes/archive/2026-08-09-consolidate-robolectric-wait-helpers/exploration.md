# Exploration: consolidate-robolectric-wait-helpers

## 課題 / 動機

harden-update-accessory-unknown-id のレビュー ([review-001.md](../harden-update-accessory-unknown-id/review-001.md) Suggestion) で、Robolectric テストの待機ヘルパ (`idle()` / `awaitConvergence()` / `committedTexts()` / `visibleRowTexts()`) が `UnknownSectionAccessoryHostTest` で5ファイル目の複製になったと指摘された。今後テストが増えるたびに複製が増える前に共有ヘルパへ集約する。

## 現状の実態 (ksn-scout 調査 2026-08-09)

- 4関数セットの重複は **ks-settingsview-ui/src/test 内に閉じている**。bridge 側に同セットは存在しない
  - フルセット保持: `AttachOrderRestoreTest.kt:76-144` (idle を `flushMainQueue()` 別名でラップ) / `AdapterReattachTest.kt:88-187` / `StoreUnbindTest.kt:75-137` (`committedTexts` は header/footer 分岐なしの簡略版) / `UnknownSectionAccessoryHostTest.kt:85-142`
  - `idle()` のみ部分重複: `InitialThemeDecorationTest.kt` / `CustomCellRenderingTest.kt`
  - 差分は些細 (失敗メッセージに Theme を含めるか、中間関数の別名) でロジックは同一
- bridge には既に **モジュールローカル共有ヘルパの前例**がある: `KsBridgeTestHost.kt` (`pump()` が待機+レイアウトを共通化)。bridge 側の重複は `KsBridgeLifecycleTest.kt:220` の inline idle 1箇所のみで、集約の必要性は薄い
- Gradle 構成: 4モジュール構成、`java-test-fixtures` / testFixtures は未使用。bridge → ui の一方向依存 (`bridge/build.gradle.kts:64`) あり
- 4関数は kotlinx-coroutines-test 非依存 (Thread.yield + Looper ポーリングの独自待機)。依存は Robolectric / Looper / KsSettingsView の internal アクセサ / SettingsRootStore

## 検討した選択肢 (却下案と理由を含む)

- **案A (推奨): ui モジュールの src/test 内に共有ヘルパファイルを新設し、6ファイルを差し替える**
  - Gradle 変更不要。bridge の `KsBridgeTestHost` と同じ「モジュールローカル共有ファイル」パターンで一貫する
- **案B (却下): ui に testFixtures を有効化して src/testFixtures に置き、bridge からも参照可能にする**
  - 現状 bridge に4関数の需要が無く、Gradle 公開面を増やすのは過剰。需要が生まれたときに案Aから昇格すればよい (可逆)
- **案C (却下): テスト支援専用モジュール新設**
  - モジュール追加コストに見合う横断需要が無い

## 決定事項

- 案A (ui モジュールの src/test 内共有ヘルパファイル新設) を S 級として採用 — ユーザー確定 (2026-08-09)
- スコープは4関数セット (`idle` / `awaitConvergence` / `committedTexts` / `visibleRowTexts`) と付随最小限に絞る。`HostActivity` / `layoutSettingsView` 等の他候補は本変更に含めない
- `awaitConvergence` の失敗メッセージは情報量の多い側 (Theme 併記) に寄せる
- `committedTexts` は完全版 (Section header/footer 分岐あり) に統一する (簡略版が上位互換で置換可能なことを実装時に確認)

## ADR 候補

- なし (テスト専用・可逆・境界を越えないため ksn-core の選別基準に該当せず)

## 未決の論点

- 共有化のスコープ: 4関数セットに絞るか、`HostActivity` (9ファイル) / `layoutSettingsView` (4ファイル) / `collectTexts` (4ファイル) 等の他候補まで含めるか。推奨は4関数+付随最小限に絞り、他候補は別途 (スコープ肥大は S 級を壊す)
- `awaitConvergence` 失敗メッセージの差分 (Theme 併記の有無) をどちらに寄せるか — 情報量の多い側 (Theme 併記) に寄せるのが自然
- `committedTexts` の簡略版 (StoreUnbindTest) を完全版に統一してよいか (完全版は簡略版の上位互換のはず — 実装時に要確認)

## UI 素材

- なし (UI 変更を含まない)

## 変更級の推奨: S (理由)

テストコードのみ・公開 API 変更なし・単一プラットフォーム (Android)・完全に可逆・UI なし。触る能力は android テストハーネスのみ。案A なら Gradle にも触らない。

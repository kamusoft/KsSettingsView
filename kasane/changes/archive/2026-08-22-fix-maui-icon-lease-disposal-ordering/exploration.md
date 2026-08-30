# Exploration: fix-maui-icon-lease-disposal-ordering

## 課題 / 動機

MAUI facade の icon 実体化 (maui/ADR-0015) で導入した `KsImageLease` の破棄が、native への反映より**先に**走る箇所が 3 つあると `add-maui-basic-input-cells` の review-002 (Minor-10) が指摘した。実害は image loader 経路 (Uri / Stream) に限られ、最悪ケースは Android での bitmap リサイクルによる表示欠け。

## 探索の結論 (2026-08-22): 対応不要 — 実装もテストも完了済み

指摘の 3 箇所は**すべて review-002 の推奨どおりに修正済み**であり、**順序を固定する回帰テストも 4 本存在する**。修正・テストとも指摘元の change 内で取り込まれ、コミット `37cd415` に squash されている (`git log -S"_retiredIcons"` で確認)。本 change の起票 (蒸留時のスタブ作成) は review-002 の記述のみに基づいており、コードとテストの現状を確認していなかった。

### 実装 (すべて遅延破棄へ移行済み)

| 指摘箇所 | 現在の実装 | 位置 |
|---|---|---|
| ① `StoreIcon` の即時破棄 | `previous` は `_retiredIcons` へ退避。破棄は `Flush()` 末尾の `DisposeRetired()` (= `ReplaceCell(s)` 送信の後) | `KsSettingsController.cs:1612` / `:1729` |
| ② `UnregisterCell` → `RemoveCells` | 同じく `_retiredIcons` へ退避。破棄は `OnObservedCollectionChanged` 末尾 (= `RemoveCell` 送信の後) | `:2028` / `:447` |
| ③ `ClearRegistrations` → `RebuildRoot` | `_retiredIcons.AddRange(_icons.Values)` のみ。破棄は `SetRoot` の後の `DisposeRetired()` | `:1072` / `:1014` |

`Disconnect()`・gateway 未接続時の `Flush()` にも破棄経路があり、退避したまま漏れる穴も見当たらなかった。

### 回帰テスト (`IconSourceTests`)

`DisposeProbe.OnDispose` で破棄の瞬間に `scope.Calls` を写し取り、「その時点で対象の gateway 呼び出しが記録済みである」ことを assert する形で、4 経路が固定されている。

| テスト | 固定する順序 | 位置 |
|---|---|---|
| `ReplacedIconLeaseIsDisposedAfterNativeUpdate` | 破棄は `ReplaceCell` 配信の後 | `IconSourceTests.cs:333` |
| `RemovedCellLeaseIsDisposedAfterNativeRemoval` | 破棄は `RemoveCell` 配信の後 | `:357` |
| `ReplacedSectionCellsLeaseIsDisposedAfterNativeUpdate` | 破棄は `ReplaceSection` 配信の後 | `:380` |
| `RebuiltRootLeasesAreDisposedAfterNativeRebuild` | 破棄は `SetRoot` 配信の後 | `:403` |

**検出力を実測で確認済み** (2026-08-22): 3 経路を即時破棄へ戻すミューテーションを当てると、**この 4 本だけが落ちる** (439 件中 4 件失敗)。復元後は 439 件全成功。代理値ではなく順序そのものを観測しており、トートロジーではない。

## 検討した選択肢 (却下案と理由を含む)

| 案 | 内容 | 評価 |
|---|---|---|
| A: change を破棄 (対応不要と判定) | 実装・テストとも完了済み | **採用** |
| B: 順序固定テストの追加 | 探索途中で採用しかけたが、**同等のテストが既に 4 本存在**したため不要と判明 | 却下 (実在確認済み) |
| C: B + 保留 (b) の同時修正 | `ReferenceEquals(previous.Image, lease.Image)` なら破棄しない | **却下 (分離)** — Cell をまたぐ共有を検出できず、破棄 `Action` が参照カウントなら抑止がそのままリークになる。S 級に収まらない |

## 決定事項

- 公開前トリアージ (2026-08-21): 初回リリース前に対応 (当時は未修正と認識)
- 探索 (2026-08-22): 実装・回帰テストとも完了済みと実測で確認。**本 change は対応不要**
- 保留 (b) は本 change から分離し、別 change `investigate-maui-icon-lease-sharing` として簡易起票 (オーナー判断 2026-08-22)。画像の取り扱いの根本設計の見直しに及ぶ可能性があるため

## ADR 候補

なし (maui/ADR-0015 の Consequences に既知の残課題として記録済み。破棄順序は解消済みのため、該当行を「解消済み」へ追随させるかは蒸留時に判断)。

## 未決の論点

なし。

## UI 素材

なし (見た目の変更なし)。

## 変更級の推奨: 対応不要 (コード変更ゼロ)

実装・テストとも完了済みのため、実装フェーズを持たない。アーカイブ (蒸留) のみ。

## 関連ファイル

- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs` (StoreIcon / UnregisterCell / ClearRegistrations / DisposeRetired)
- `maui/KsSettingsView.Maui.Tests/IconSourceTests.cs:331-421` (順序固定テスト 4 本)
- 出典: `kasane/changes/archive/2026-08-11-add-maui-basic-input-cells/review-002.md` (Minor-10・保留 (b))
- 分離先: `kasane/changes/investigate-maui-icon-lease-sharing/exploration.md`

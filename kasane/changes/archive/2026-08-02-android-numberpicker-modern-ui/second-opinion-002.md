# セカンドオピニオン: android-numberpicker-modern-ui (002 回目)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 実装 diff 全量 (ワーキングツリー未コミット変更。ホスト側 review-001 対応の code-review 並走)
---
# レビュー結果: android-numberpicker-modern-ui

**日付**: 2026-08-02  
**判定**: **CHANGES_REQUESTED**  
**指摘件数**: Critical 0 / Major 2 / Minor 1 / Suggestion 0

## サマリー

`unit` のパリティ、スタイル解決、dismiss 経路、アクセシビリティ操作は概ね仕様に沿っています。proposal/spec の書き換えや未記録の deviation もありません。

一方、スナップ完了前に選択値が更新される競合と、仕様上有効な候補数での巨大な eager allocation があり、いずれも実行時の仕様違反につながるため修正が必要です。指定されたテスト成功結果は確認済みとして扱い、再実行していません。

## 指摘事項

### [🟠 Major] SnapHelper の補正移動開始時にも選択が確定される

**該当箇所**:

- [android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:233](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:233)
- [android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:368](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:368)
- [kasane/changes/android-numberpicker-modern-ui/tasks.md:14](kasane/changes/android-numberpicker-modern-ui/tasks.md:14)

**問題点**: `SCROLL_STATE_IDLE` だけを条件に `findSnapView()` の最寄り候補を確定しています。しかし、ドラッグが行間で止まった場合、その `IDLE` は `LinearSnapHelper` が中央への補正スクロールを開始する契機でもあります。`findSnapView()` は「中央に最も近い View」を返すだけで、既に中央へスナップ済みとは限りません。

RecyclerView 1.3.2 では後から登録された SnapHelper の listener が先に同じ `IDLE` を処理して補正移動を開始し、その後、本 listener が元の `IDLE` 通知で選択値を更新します。その補正移動中に確定すると、仕様が要求する「直前にスナップ静止した候補」ではなく、新しい候補が通知され得ます。

また、タスク2.3は「移動中確定の採用値」のテストを完了扱いにしていますが、現テストは整数行分の移動後の状態とアクセシビリティ操作後の確定しか検証しておらず、この競合を通していません。

**推奨修正**: `calculateDistanceToFinalSnap()` が `(0, 0)` の場合だけ選択を更新するなど、実際に中央へ整列済みであることを確認してください。併せて、行間で停止して SnapHelper の補正移動中に確定した場合は旧選択値、最終 `IDLE` 後は新選択値になるテストを追加してください。

### [🟠 Major] 有効な候補件数を eager List 化し、メインスレッドで OOM／停止する

**該当箇所**:

- [android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:91](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:91)
- [android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:100](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:100)
- [android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:63](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:63)

**問題点**: 候補数は `Int.MAX_VALUE` を超える場合だけ拒否されますが、上限ちょうどを含む有効範囲は `List(count)` で全整数を生成し、さらに `map` で全表示文字列を生成します。`count == Int.MAX_VALUE` では確実に巨大確保となり、より小さい範囲でもタップ時のメインスレッドで OOMまたは長時間停止を起こします。

これは「候補件数が Int 上限を超えなければ提示する」という仕様を満たせません。現在のテストも「上限超過」だけで、上限以下の大規模境界を検証していません。

**推奨修正**: 候補を `min・step・count` で保持し、Adapter の position から64bit演算で値と表示文字列を都度生成してください。確定値も index から直接算出すれば、候補値リストと文字列リストの両方を除去できます。`Int.MAX_VALUE` 件を生成せずに `itemCount`、先頭・末尾の値、確定値を検証する境界テストも追加してください。

### [🟡 Minor] 変更資料への参照を含む禁止コメントが残っている

**該当箇所**:

- [android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCell.kt:10](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCell.kt:10)
- [android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:24](android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:24)
- [samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:35](samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:35)

**問題点**: `openspec/changes/...`、`kasane/changes/...`、`Decision 3` など、アーカイブされる作業資料やローカル通番への参照が残っています。これは `concepts/cross/conventions/comment-policy.md` がコードコメントで明示的に禁止している形式です。特に `NumberPickerCell` は同じKDocブロックを今回更新しています。

**推奨修正**: 現在の公開契約を自己完結する説明へ書き換えてください。恒久的な設計根拠が必要な箇所だけ、対応する `android/ADR-NNNN` 形式で参照してください。

## アクションプラン

1. スナップ距離を確認して最終整列時だけ選択を更新し、行間停止からの補正移動中確定テストを追加する。
2. 候補列を index ベースの遅延生成へ変更し、大規模境界テストを追加する。
3. 変更資料参照のコメントを自己完結した説明またはADR参照へ置き換える。
4. `tasks.md` の2.3は、移動中確定テストが実装されるまで完了扱いにしない。


---

## 突き合わせ結果 (ksn-orchestrator, 2026-08-02)

ホスト側 review-001.md との突き合わせ。採否規則 (ksn-second-opinion Step 3) に基づく。

| # | 指摘 | 出典 | 採否 | 確定重要度 | 根拠 |
|---|---|---|---|---|---|
| 1 | comment-policy 禁止参照の残存 (NumberPickerCell.kt ほか) | 双方 (ホスト Major / 相方 Minor) | **確定** | Major | 双方一致。重要度はホスト側判定を採用 |
| 2 | SnapHelper 補正移動開始時の IDLE で選択が確定される | 相方のみ (Major) | **採用** | Major | 該当箇所 (KsWheelView.kt:233-239) を実コードで検証。IDLE 時に findSnapView を無条件確定しており、行間停止→補正スクロール開始時点で選択が更新される。spec「選択中候補の更新はスナップ静止時のみ」の違反シナリオが成立。根拠強 |
| 3 | 有効候補数での eager List 生成による OOM/停止 | 双方 (相方 Major / ホスト Suggestion) | **確定** | Major | 箇所一致 (NumberPickerCellViewHolder.kt:100, :63)・重要度割れ。count == Int.MAX_VALUE の有効入力で確実に巨大確保となり「Int 上限以下なら提示する SHALL」を実行時に満たせないため、高い方 (Major) を採用。修正方向 (index ベース遅延生成) は双方同一 |
| 4 | bindRow フックが実バインド経路と別コード | ホストのみ (Minor) | 確定 (ホスト指摘) | Minor | 相方は言及なし。突き合わせ対象外だがホスト指摘として修正サイクルへ |
| 5 | 選択変更時の AccessibilityEvent 非送出 | ホストのみ (Minor) | 確定 (ホスト指摘) | Minor | 同上 |

- 降格: 0 件 / 未解決 (NEEDS_DISCUSSION 行き): 0 件
- 相方指摘 3 件はすべて確定または採用。以後ホスト側指摘と同格に修正サイクルで処理する

---

## 相方による修正確認 (2周目, 2026-08-02)

修正サイクル1周目の実装に対し、同一セッション (turn 2) で相方に解消確認を依頼した結果:

| 前回指摘 | 判定 |
|---|---|
| SnapHelper 補正移動開始時の選択確定 | **解消** (KsWheelView.kt:245 の残距離ゼロ確認 + KsWheelViewTest.kt:200 の遷移テスト。新規問題なし) |
| 有効候補数での eager List 生成 | **解消** (NumberCandidates による遅延計算 + 境界テスト。新規問題なし) |
| 変更資料参照コメントの残存 | **解消** (禁止参照の検出なし。ADR 参照は許可形式のみ。新規問題なし) |

追加指摘なし。相方の最終判定: **APPROVED** (raw: `~/.kasane/counterpart-bridge/responses/so-code-review-android-numberpicker-modern-ui-2.md`)

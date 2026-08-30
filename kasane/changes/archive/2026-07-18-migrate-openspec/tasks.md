# OpenSpec 移行タスク

このファイルをセッション間の再開点の SSoT とする。capability の状態は `未着手 → 抽出済 → 統合済 → 確定` で管理する。

## 0. 移行範囲

- [x] 0.1 `openspec/specs/` 13 capability を棚卸しする
- [x] 0.2 `openspec/changes/archive/` 20件と `design.md` 20件を棚卸しする
- [x] 0.3 `docs/` 9文書を concepts 抽出時の補助資料として登録する
- [x] 0.4 進行中の `openspec/changes/` 7件を今回の対象外とする（ユーザー確認済み）

## 1. 規約移植

- [x] 1.1 `openspec/config.yaml` の `context` を `kasane/config.yaml` と照合する
- [x] 1.2 capability 命名規約・タクソノミの有無を確認し、必要なら `concepts/conventions/` へ移植する
- [x] 1.3 OpenSpec artifact 固有ルールを移植対象外として記録する

判定（2026-07-17）:

- 旧 `context` の日本語記述規約は、現 `kasane/config.yaml` の `context` に包含済み。
- 旧 config に capability 命名規約・タクソノミは存在しないため、`concepts/conventions/` への移植対象なし。
- proposal/spec/design/tasks の形式規約は OpenSpec artifact 固有であり、`ksn-core` と重複するため移植しない。

## 2. ADR backfill

- [x] 2.1 archive 20件の `design.md` と `proposal.md` を走査する
- [x] 2.2 ADR 候補のトリアージ一覧をユーザーへ提示する（`candidates/adr-triage.md`）
- [x] 2.3 承認された候補を `status: proposed` でドラフトする（ADR 0001〜0011）
- [x] 2.4 ユーザーレビュー後に accepted とし、`decisions/index.md` を更新する
- [x] 2.5 バッチAのdrift判断に基づき、ADR-0012 / ADR-0013をproposedで起票する
- [x] 2.6 ADR-0012 / ADR-0013のユーザーレビューを反映する（0012は却下・候補へ退避、0013はaccepted、0003はsuperseded、index更新）

採否（2026-07-17）: 推奨11候補をユーザーが一括承認し、ADR 0001〜0011 を accepted として確定。

追加採否（2026-07-18）: ADR-0012案は却下。ADR-0002のMaven `groupId` = `jp.kamusoft` を維持し、現行Gradleとの差は実装driftとして後続対応する。ADR-0013はaccepted、ADR-0003はsupersededとして確定。

## 3. concepts 抽出規律

- [x] 3.1 読み順を「コードとテスト → spec → `docs/`」とする
- [x] 3.2 `docs/legacy-aiforms-reference.md` は現仕様の根拠ではなく、移植元との比較資料としてのみ扱う
- [x] 3.3 各 candidate にコード根拠、spec/docs 照合結果、drift 所見を記録する
- [x] 3.4 各バッチの統合前に `kasane/concepts/rules.md` を再確認する

## 4. バッチA — 基盤と Core

照合資料: `docs/overview.md`、`docs/core-model.md`、`docs/architecture.md`

| capability | 状態 |
|---|---|
| `monorepo-foundation` | 確定 |
| `settings-view-core` | 確定 |

- [x] 4.1 capability ごとの抽出を完了する
- [x] 4.2 バッチ内候補を統合し、drift 所見と ADR 候補を提示する
- [x] 4.3 承認分を concepts に確定し、index/log を更新する

## 5. バッチB — プラットフォーム Host と DSL

照合資料: `docs/architecture.md`、`docs/platform-guide-ios.md`、`docs/platform-guide-android.md`

| capability | 状態 |
|---|---|
| `settings-view-ios-host` | 確定 |
| `settings-view-ios-swiftui` | 確定 |
| `settings-view-android-host` | 確定 |
| `settings-view-android-compose` | 確定 |

- [x] 5.1 capability ごとの抽出を完了する
- [x] 5.2 バッチ内候補を統合し、drift 所見と ADR 候補を提示する（`candidates/batch-b-integration.md`）
- [x] 5.3 承認分を concepts に確定し、index/log を更新する

## 6. バッチC — Styling と Theme Bridge

照合資料: `docs/styling-and-theming.md`、両 platform guide

| capability | 状態 |
|---|---|
| `settings-view-ios-style` | 確定 |
| `settings-view-ios-theme-bridge` | 確定 |
| `settings-view-android-style` | 確定 |
| `settings-view-android-theme-bridge` | 確定 |

- [x] 6.1 capability ごとの抽出を完了する
- [x] 6.2 バッチ内候補を統合し、drift 所見と ADR 候補を提示する（`candidates/batch-c-integration.md`）
- [x] 6.3 承認分を concepts に確定し、index/log を更新する

## 7. バッチD — Cell

照合資料: `docs/cells.md`、`docs/styling-and-theming.md`

| capability | 状態 |
|---|---|
| `cell-types-basic` | 確定 |

- [x] 7.1 capability の抽出を完了する
- [x] 7.2 既存 concepts 候補と統合し、drift 所見と ADR 候補を提示する（`candidates/batch-d-integration.md`）
- [x] 7.3 承認分を concepts に確定し、index/log を更新する

## 8. バッチE — Samples

照合資料: `docs/platform-guide-ios.md`、`docs/platform-guide-android.md`

| capability | 状態 |
|---|---|
| `samples-ios` | 確定 |
| `samples-android` | 確定 |

- [x] 8.1 capability ごとの抽出を完了する
- [x] 8.2 長命概念として残す価値がある候補だけを統合する（`candidates/batch-e-integration.md`）
- [x] 8.3 承認分がある場合のみ concepts に確定し、index/log を更新する

## 9. 凍結と完了

- [x] 9.1 全 capability が「確定」であることを確認する
- [x] 9.2 `AGENTS.md` に OpenSpec 凍結宣言を追記する
- [x] 9.3 `openspec/` と周辺資産を編集・削除・リネームしていないことを確認する
- [x] 9.4 移行サマリを提示する（`summary.md`）
- [x] 9.5 蒸留対象が残っていないことを確認し、change を archive する

# OpenSpec 資産を Kasane の長命層へ移行する

## Why

本プロジェクトの開発ハーネスを OpenSpec から Kasane へ移行したため、旧資産に含まれる長命な判断と概念を `kasane/decisions/` および `kasane/concepts/` へ蒸留する。移行後も現仕様の SSoT はコードとテストとし、OpenSpec の Requirements / Scenario は機械変換しない。

## What

### 資産インベントリ

- `openspec/specs/`: 13 capability
- `openspec/changes/archive/`: 20 change（全件 `design.md` あり）
- `openspec/drafts/`: 5文書
- `docs/`: 9文書（concepts 抽出時の補助資料）
- `openspec/changes/`: 進行中7件（今回の移行対象外）

### 移行内容

1. `openspec/config.yaml` の長命な記述規約を Kasane 側へ移植する。
2. archive 済み change の重要判断をトリアージし、承認されたものを ADR として起こす。
3. capability ごとにコードとテストを先に確認し、spec と `docs/` を後から照合して、低腐食情報だけを concepts へ統合する。
4. コード・spec・`docs/` 間の矛盾は drift 所見として提示し、解消方向は独断で決めない。
5. 移行完了後、`openspec/` を歴史資料として凍結する。

## Non-Goals

- `openspec/specs/` の Requirements / Scenario を concepts へ丸写ししない。
- コードから再導出できる内部実装フローやファイル一覧を移さない。
- Kasane と重複する OpenSpec artifact ルールを移植しない。
- 進行中の `openspec/changes/` 7件は移設・編集・archive しない。これらは別途対応する。
- `openspec/drafts/` は今回一括移行せず、必要になった時点で個別に蒸留する。
- 実装コードは変更しない。

## 変更級

`migrate`（S/M/L、実装レビュー、verify、deviation の対象外）


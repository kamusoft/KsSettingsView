---
type: policy
title: concepts 配置ルール
description: ドメイン導出規則と、この concepts/ のカテゴリ定義・配置基準
timestamp: 2026-08-30
---

## ドメイン導出規則

ドメイン一覧の正は `kasane/config.yaml` の `domains` (core / ios / android / maui。`cross` は予約ドメインとして常に存在する)。knowledge・決定の行き先は次で判定する:

- 単一 platform のビルドルート (`ios/` `android/` `maui/`) に閉じる知識・決定 → その platform ドメイン
- 全 platform が共有する契約 (設定ツリーモデル・Cell 意味論・styling 規則・共通 architecture) → `core`
- リポジトリ構成・命名規約・docs 運用・ハーネス運用などリポジトリ横断のメタ事項 → `cross`
- 新しい platform・パッケージ系統が増えた場合: 既存系統に属するなら該当ドメインへ。属さないならユーザー合意の上で `config.yaml` の `domains` に追加する
- 変更 (proposal) が複数ドメインに触る場合の `domain:` 欄は `cross`。蒸留時の ADR / concepts の行き先は内容ごとに本規則で判定する

## 規範は handbook へ

規約・手順 (コードがそれに従うべきもの) は concepts ではなく `kasane/handbook/<domain>/` に置く。判定は「この文書とコードが食い違ったとき、直すのはどちらか」— コードを直すなら handbook、文書を直す余地があるなら concepts。

移植元・外部システムの仕様要約のように、外部の実物と照合して真偽が決まる文書は concepts (`cross/reference/`) に置く。その外部資産を「どう参照するか」を定めた文書は handbook に置く。

## カテゴリ定義

### core/

| カテゴリ | 対象 | 主な type |
|---|---|---|
| architecture/ | レイヤ構造・責務境界・状態同期・プラットフォーム間の共通原則 | concept |
| core-model/ | 公開 Core API・モデル・差分・アクセサリ | concept, glossary, reference |
| cells/ | Cell 共通契約・各 Cell の公開 API・利用例 | concept, reference |
| styling/ | Theme・CellStyle・レイアウト・視覚的契約 | concept, design-tokens, reference |

### ios/ / android/ / maui/

| カテゴリ | 対象 | 主な type |
|---|---|---|
| api/ | platform 固有の公開 API・利用例・Bridge 境界 | concept, reference |
| architecture/ | platform 内部の共有基盤・機構の契約、platform のビルドツールチェーンの契約 (2026-08-12 オーナー合意で新設。maui / android で使用) | concept |

### cross/

| カテゴリ | 対象 | 主な type |
|---|---|---|
| architecture/ | リポジトリ・ビルド構成の責務境界 | concept |
| reference/ | 外部資産 (移植元リポジトリ等) の仕様要約。真偽は外部の実物との照合で決まる | reference |

## 配置判断

まずドメイン導出規則でドメインを決め、次にドメイン内のカテゴリを選ぶ。公開 API や利用例は、その主題に応じて core-model/、cells/、styling/、`<platform>/api/` のいずれかへ配置する。複数プラットフォームに共通する責務境界や設計原則は core/architecture/ へ配置し、特定プラットフォームに閉じる内容は `<platform>/api/` へ配置する。複数カテゴリにまたがる場合は、最も中心となる契約を持つカテゴリを選び、他の概念からリンクする。

本プロジェクトは UI コントロールライブラリであるため、公開 API の契約と利用コード例は製品知識として concepts に記載できる。一方、内部実装フローや単なるファイル一覧は記載しない。

概念間リンクは実配置基準の相対パス。他ドメインへは `../../<domain>/<category>/<concept>.md` の形になる。他ドメインの ADR を参照するときは `<domain>/ADR-NNNN` の正式形で表記する。

## 新カテゴリの条件

既存カテゴリに適切に収まらない概念が3つ以上蓄積した場合に、ユーザー合意の上で新設する。

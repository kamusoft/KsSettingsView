# Design: consolidate-readmes-and-contribution

## Context

方針は cross/ADR-0023 (README はルート 2 枚、開発者向け知識は concepts に一本化) と cross/ADR-0024 (貢献は Issue で受け外部 PR は受け付けない) で確定している。本書が扱うのは、その方針を実装可能な粒度へ落とす 3 つの判断 — 廃止する README の中身をどのファイルへ移すか、docs-refresh のどの記述を取り除くか、README の所在要件をどの範囲に適用するか。いずれも spec-review (second-opinion-spec-001) で「tasks に埋もれる」「範囲が未定義」と指摘された箇所にあたる。

## Goals / Non-Goals

- **Goals**: 移送の取りこぼしを防ぐ対応表を確定する。docs-refresh の除去箇所を漏れなく列挙する。README 所在要件の適用範囲を定義する。
- **Non-Goals**: 配布座標の値の確定 (配信フェーズの責務。本変更では暫定値のまま文書間の一致だけを保つ)。`maui/spike/` の存廃 (phase-2 の論点)。

## Decisions

### Decision 1: 廃止する README の中身の移送先

**採用案:** 節ごとに次の対応で移す。「移送しない」は同等の内容が他所に実在することを 1 件ずつ確認してから破棄する (tasks 1.11)。

| 移送元 | 内容 | 移送先 | 新規/既存 |
|---|---|---|---|
| `maui/README.md` 「binding 層」「Native artifact の生成」「SDK 標準アイテムの採否」「既知の制約」「SDK 更新時に再検証する箇所」 | binding のビルド構成、SDK 内部ターゲットへの割り込み、`BG8605` / `BG8A00` の意味、共有 scheme の役割、`KsBridgeFont` 等の platform 差 | `kasane/concepts/maui/architecture/binding-build-integration.md` | 新規 |
| `maui/README.md` 「検証ホストの使い方」 | IntegrationHost / MauiHost の起動手順 (`DEVELOPER_DIR` 指定含む) と期待される表示 | `kasane/concepts/maui/conventions/integration-host-verification.md` | 新規 |
| `android/README.md` 「必要環境」「Android SDK ロケーションの設定」「トラブルシューティング」 / `samples/maui/README.md` 「Xcode のバージョン指定」「Android SDK ロケーションの設定」 | 開発環境のセットアップ手順 (`ANDROID_HOME`、2 つの `local.properties`、`DEVELOPER_DIR`) | `kasane/concepts/cross/conventions/local-development-setup.md` | 新規 |
| `samples/*/README.md` 「開き方」「実行手順」「本体ライブラリのデバッグ」 | Sample の起動手順と本体へのステップイン | 同上 `local-development-setup.md` へ統合 | 上で新規作成 |
| `samples/ios/README.md` 「実機目視確認チェックリスト」 | 目視で確認する項目 | `kasane/concepts/cross/conventions/runtime-behavior-verification.md` | 既存へ統合 |
| `samples/android/README.md` 「サードパーティ通知」 | Material Symbols (Apache 2.0) | ルート README のライセンス節 | — |
| `samples/*/README.md` 「デモ画面一覧」 | — | **移送しない** — `SampleScreen` の実ソースが正 (concepts に転記すると腐る) | — |
| `android/README.md` 「モジュール構成」/ `maui/README.md` 「構成」 | モジュール一覧・ディレクトリ構成 | **移送しない** — `cross/architecture/repository-boundaries.md` と各 concepts index に既出 | — |
| `android/README.md` 「利用アプリ側の前提」 | Material3 Theme 必須・`FragmentActivity` 必須 | **移送しない** — `android/api/android-native-host.md` と Android Skill に既出 | — |
| `android/README.md` 「ビルド・テスト」 | 基本のビルド / テストコマンド | **移送しない** — `cross/conventions/test-execution.md` とルート README に既出 | — |
| `samples/maui/README.md` 「画面を追加するには」「依存関係」「ディレクトリ構成」 | Sample の拡張手順 | **移送しない** — `cross/conventions/sample-parity.md` と実ソースが正 | — |

**理由:** 移送先をディレクトリ単位で指示すると「既存ファイルへの統合か新規作成か」が実装者判断になり、移送の取りこぼしと配置のばらつきが起きる。新規 3 本は、いずれも既存 concepts のどれにも属さない主題 (binding のビルド構成 / 検証ホストの運用 / 開発環境のセットアップ) を持つ。既存へ統合する 1 本 (実機目視確認) は `runtime-behavior-verification.md` が同じ主題を扱っている。

**代替案:**
- **A: すべて新規ファイルを起こす** — 実機目視確認チェックリストを独立させると、同じ主題の規約が 2 ファイルに割れる。却下。
- **B: すべて既存ファイルへ統合する** — binding のビルド構成を `native-bridge.md` (interop 境界の公開契約) に混ぜることになり、責務が濁る。却下。
- **C: 移送先は実装時に判断する** — spec-review の指摘どおり、対応表がないと廃止時に失われる。却下。

### Decision 2: docs-refresh から取り除く記述の範囲

**採用案:** コード正の機械チェックは①モジュール一覧と② Sample デモ画面一覧をともに廃止し、③ツール最低バージョンの 1 種だけを残す。除去は次の 6 箇所すべてに及ぶ。

| # | 箇所 | 取り除くもの |
|---|---|---|
| 1 | 追従対象の表 | `android/README.md` / `maui/README.md` / `samples/*/README.md` の行と、デモ画面一覧が最重要の追従点である旨 |
| 2 | Step 3d の突合表 | ①②の行、③の突合先から platform README の記載 |
| 3 | Step 4 の実行例 | 例示に現れる platform / Sample README |
| 4 | README 委譲プロンプト (5b) の「README 種別ごとの確認事項」 | ルート README のモジュール表を確認せよという指示、platform / Sample README 向けの確認事項 |
| 5 | 整合性チェック | platform / Sample README を前提とする検査項目 |
| 6 | 完了サマリ | 対象枚数の例示 |

**理由:** ①の突合先 (ルート README のモジュール表・`android/README.md`・`maui/README.md`) と②の突合先 (`samples/*/README.md`) がいずれも存在しなくなるため、機械チェックとしては対象消滅である。表だけを直して委譲プロンプト (4) を残すと、将来の docs-refresh 実行がルート README にモジュール表を再導入し、「README は利用者の入口に純化する」(ADR-0023) を静かに破る。

**代替案:**
- **A: ①を残し、突合先をルート README のモジュール表に限定する** — ルート README にモジュール一覧を残すことになり ADR-0023 と衝突する。却下。
- **B: 機械チェックの表だけを直す** — 委譲プロンプトの指示が残り、上記の再導入が起きる。却下。

### Decision 3: 「公開ドキュメント面」の定義

**採用案:** README の所在要件を適用する範囲を、リポジトリルート直下と `skills/` `android/` `ios/` `maui/` `samples/` の配下と定める。`kasane/` (変更管理・決定記録・ロードマップとその検証証跡)、`openspec/` (凍結済み歴史資料)、`.claude/` は対象外とする。

**理由:** `kasane/changes/archive/*/verification/` と `evidence/` に検証証跡としての README が 4 枚実在する。これらは過去の実験記録であり、廃止した README への言及を含んでいてよい。リポジトリ全体を対象にすると、過去の記録の書き換えを要求することになる (ADR・history・archive は append-only の歴史)。

**代替案:**
- **A: リポジトリ全体を対象にする** — archive の 4 枚が違反となり、歴史の書き換えを招く。却下。
- **B: ルート直下だけを対象にする** — `android/` `samples/` に README を戻せてしまい、要件が骨抜きになる。却下。

## Risks / Trade-offs

- **移送の取りこぼし**: 最大のリスク。Decision 1 の対応表と tasks 1.1 の突き合わせで対処し、廃止 (tasks 4.1) は移送 (グループ 1) の完了後に行う。
- **新規 concept 3 本の粒度**: `kasane/concepts/rules.md` の配置基準に照らして粒度が合わない可能性がある。実装時に確認し、合わなければ既存ファイルへの統合に切り替える (その場合は deviation に記録)。
- **`local-development-setup.md` の肥大化**: 3 platform の環境手順に Sample の実行手順とステップインが加わる。節で分けて可読性を保ち、肥大化が過ぎるようなら platform 別への分割を検討する。
- **docs-refresh の除去漏れ**: 6 箇所の列挙から漏れると旧指示が残る。デルタスペックの Scenario「旧指示の残存がないこと」で機械的に検査する。

## Migration Plan

1. **移送** (グループ 1) — concepts へ内容を移し、`native-bridge.md` の逆参照を解消する
2. **スクリーンショット撮影と承認** (グループ 2) — 実装と並行可
3. **ルート README 作成** (グループ 3)
4. **旧 README 廃止** (グループ 4) — 1 の完了が前提
5. **`.github/` 一式** (グループ 5)
6. **docs-refresh 更新** (グループ 6) — 4 の完了が前提 (対象消滅を反映するため)
7. **skills の座標修正** (グループ 7)
8. **検証** (グループ 8)

移送を先頭に置くのは、廃止によって内容が失われるのを防ぐため。docs-refresh の更新を廃止の後に置くのは、対象消滅を反映する変更だから。

## Open Questions

なし。配布座標の値の確定は配信フェーズ (phase-4〜8) の責務であり、本変更では暫定値のまま文書間の一致だけを保つ (spec-review の Major 5 はスコープ外としてオーナー却下)。

## ADR 候補

なし。3 つの Decision はいずれも cross/ADR-0023 / ADR-0024 の適用範囲の具体化であり、覆すコストが低く境界も越えない (選別 3 基準に該当しない)。移送先の配置は concepts と index が、docs-refresh の除去範囲は SKILL.md 自身が正となる。

# Proposal: backport-release-workflow-hardening

## Why

sibling の KsDialogs が、このリポジトリの release 機構を翻案して初回リリースに到達する過程で見つけた不具合と改良を知らせてきた (`../KsDialogs/kasane/outbox/KsSettingsView/2026-09-10-release-workflow-fixes-and-improvements.md`)。現物を確認した結果、publish の内部に 3 つの弱点がある。

- **Maven Central の公開待ちが 30 分で足りない可能性がある**: 上限の根拠は初回リリースの実測 11 分だが、KsDialogs の初回は Central の同期に約 60 分を要した。公開そのものは済んでいるのに job が失敗する。検証の決着待ちと同じ環境変数を共有しているため、公開待ちだけを延ばすこともできない。
- **公開レジストリの照会が障害と未反映を区別できない**: `wait-for-registries.sh` は NuGet 側で通信失敗・空応答・5xx を「未反映」に畳み込み、Maven 側も HTTP status を取得しながら 200 以外を一様に扱う。上限まで待って失敗したとき、レジストリが遅いのか壊れているのかが出力から分からない。
- **引き継いだ deployment ID の読み込みが、失敗しうる成果物取得より後ろにある**: 取得に失敗するとその attempt は引き継ぎを読まずに終わる。

いずれもリリースを不必要に赤くする、または失敗の原因を隠す方向に働く。

## What Changes

影響する能力: **release-workflow** (単一)。いずれも publish 内部の挙動で、配布物と利用者向けドキュメントには触れない。

1. **Maven Central の公開待ちの上限を分離して延ばす** — `wait-published` の上限を `wait-validated` と別の環境変数に分け、既定を 90 分にする。publish job の実行時間上限を算出式ごと組み直す。
2. **公開レジストリの照会に状態分類を入れる** — Maven 座標 1 件と NuGet Package ID 3 件のそれぞれについて、反映済み・未反映・判定不能を区別し、判定不能は失敗の種別まで残す。
3. **引き継いだ deployment ID の読み込み位置を移す** — 失敗しうる成果物の取得より前へ置き、読み込み済みであることを観測できるようにする。
4. **待機スクリプトの自己テストを CI に接続する** — `wait-for-registries.sh --selftest` を lint job で走らせ、`central-portal.sh --selftest` と揃える。

## Non-Goals

- **インストール例の version 表記・GitHub Release の prerelease 印・Release ノートの分類**: `kasane/changes/install-examples-and-release-notes/` へ分離した。利用者から見える契約に関わり、ブランチ運用 ([cross/ADR-0028](../../decisions/cross/0028-ci-triggers-by-branch-role.md)) と Skill の閉世界性 ([cross/ADR-0022](../../decisions/cross/0022-user-docs-as-agent-skills.md)) に未解決の論点が残るため、探索を続けてから提案化する。
- **`check-signatures.sh` の自己テスト不具合 (知らせ #1)**: このリポジトリの当該スクリプトには自己テストが存在せず、指摘された構造も `scripts/release/` に無い。直す対象が無い。
- **再実行の続行判定を印 (marker) 方式にする (知らせ #4)**: `github.run_attempt` に依存した resume 判定を持たないこちらでは、KsDialogs が Critical とした事象の原因が存在しない。再実行方式そのものの設計判断であり、別の設計判断を要する。

## Impact

- 破壊的変更: なし。公開 API・配布物・利用者向けドキュメントのいずれも変わらない。
- 影響範囲: `.github/workflows/release.yml` (publish job の待機と step 順序、実行時間上限)、`.github/workflows/ci.yml` (lint job に自己テスト 1 本)、`scripts/release/central-portal.sh`、`scripts/release/wait-for-registries.sh`。
- リスク: 待機の挙動は `dry-run` では確認できない (publish job が走らないため)。自己テストで分類ロジックを検査し、経路全体は次回の本番リリースで確かめる。
- 既存 ADR との関係: 新規・改訂ともに無し。

## 級: M

publish の待機と順序という不可逆操作の周辺に触れ、時間予算の再算出を伴うため。公開 API・UI・利用者向け契約の変更は無い。

domain: cross

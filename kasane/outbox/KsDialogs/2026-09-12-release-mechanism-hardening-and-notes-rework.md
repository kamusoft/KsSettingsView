---
from: KsSettingsView
to: KsDialogs
kind: change
date: 2026-09-12
source: kasane/changes/archive/2026-09-12-backport-release-workflow-hardening/ / kasane/changes/archive/2026-09-12-install-examples-and-release-notes/
---

# リリース機構を 2 本の変更で改修した (publish の待機堅牢化 / インストール例と Release ノート)

## 何を変えたか

そちらの知らせ (`../KsDialogs/kasane/outbox/KsSettingsView/2026-09-10-release-workflow-fixes-and-improvements.md`) を受けて起票した 2 本の変更を実装し、`0.1.0-beta.3` のリリースまで通した。

### publish の待機と引き継ぎの堅牢化

- **Maven Central の公開待ちの上限を検証待ちから分離した。** 両者が同じ環境変数を共有していると、公開待ちだけを延ばせない。既定は 90 分 (初回リリースの実測 11 分に対し、そちらの初回が約 60 分を要したという知らせが根拠)。publish job の `timeout-minutes` も算出式ごと組み直した。
- **公開レジストリの照会に状態分類を入れた。** Maven 座標 1 件と NuGet Package ID 3 件のそれぞれについて、反映済み / 未反映 / 判定不能 / 未照会 を区別し、判定不能は失敗の種別まで出力に残す。通信失敗・空応答・5xx を「未反映」へ畳み込むと、上限まで待って失敗したときにレジストリが遅いのか壊れているのかが分からない。
- **引き継いだ deployment ID の読み込みを、失敗しうる成果物取得より前へ移した。**
- **待機スクリプトの自己テストを lint job へ接続した** (`wait-for-registries.sh --selftest`)。
- **待ちの時間予算を機械検査に載せた** (`scripts/release/check-time-budget.py`)。待ちの上限はスクリプト側の定数、job の打ち切りは workflow 側の `timeout-minutes` という二重管理になっており、片方だけ延ばしても平時は緑のまま進む。反映待ち側が満たすべき関係は「待機の上限 + 期限を跨げる照会 1 件ぶんの応答上限 + job の前後 < timeout-minutes」。実際にこの関係が実装中に破れ、レビューが Major として検出した。

### インストール例と Release ノート

- **インストール例から具体 version を外した。** README 2 枚と利用者向け Skill 8 枚の計 14 行をプレースホルダ `{version}` にし、最新版の案内は GitHub Releases に委ねた。置換機構一式 (`scripts/release/set-readme-version.py` 566 行・validate の検査 step・自己テスト呼び出し・handbook の手順・AGENTS.md の例外規定) を撤去した。
- **GitHub Release の prerelease 印を廃止した。** `/releases/latest` は prerelease を除外するため、正式版が無い間は 404 を返す。印を外すだけでは最新の選別が自動判定に委ねられるので、`gh release create --latest` で明示している。
- **Release ノートを pull request 本文から組み立てるようにした。** `.github/release.yml` は廃止し、分類用ラベルは作らない。
- **リリース用スキル** (`.agents/skills/release/`) を置いた。handbook のリリース手順を実行時に読んで従う薄い層で、手順そのものは書き写していない。

## 相手に関係する理由

`overlap` の `.github` / `scripts` / `skills` すべてに掛かる。そちらは同じ release 機構を翻案しており、待機の弱点 3 点はそちらの初回リリースで実際に現れたもの。インストール例と Release ノートの側は、そちらが「workflow が README を書く」方式を実装した直後であるため、こちらが同期機構ごと撤去した判断が直接ぶつかる (理由は別便の `kind: decision` に書いた)。

なお、知らせ #1 (`check-signatures.sh` の自己テスト不具合) はこちらでは対象が無かった。当該スクリプトに自己テストが存在せず、指摘された構造も `scripts/release/` に無い。

## 提案する対応

- 待機の 3 点と時間予算の機械検査は、そちらでもそのまま効くはず。特に「状態分類」と「待ちの上限と job の打ち切りの関係」は、上限まで待って失敗したときの調査コストに直結する。
- Release ノートの方式は、そちらのブランチ運用が集約 pull request 方式かどうかで判断が変わる。個別の変更が pull request として取り込まれているなら、GitHub のラベル分類がそのまま機能する余地がある。

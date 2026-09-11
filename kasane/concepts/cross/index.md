# cross 目次

リポジトリ横断のメタ事項。カテゴリ定義と配置基準は [../rules.md](../rules.md) を参照。

## architecture/

- [architecture/repository-boundaries.md](architecture/repository-boundaries.md) — 横断変更をまとめる monorepo と、独立した platform build・Sample・消費者検証 (`verification/`) の責務分担
- [architecture/release-pipeline.md](architecture/release-pipeline.md) — 3 platform を 1 本の release workflow で公開する段の構成、publish 段の内部順序、version の注入経路、各チャネルへの publish 機構と公開確認の手段

## reference/

- [reference/aiforms-spec-summary.md](reference/aiforms-spec-summary.md) — 移植元 AiForms の公開 API・構造・実装パターンの要約 (凍結資料。最終的な正は移植元コード)

リポジトリ横断の規約・手順は [handbook: cross](../../handbook/cross/index.md) にある。

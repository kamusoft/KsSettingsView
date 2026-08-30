# concepts 目次 (ドメイン地図)

concepts はドメイン別に分割して管理する ([cross/ADR-0015](../decisions/cross/0015-domain-axis-core-plus-platforms.md))。カテゴリ定義・配置基準・ドメイン導出規則は [rules.md](rules.md) を参照。

| ドメイン | 内容 |
|---|---|
| [core](core/index.md) | 全 platform が共有する契約 (architecture / core-model / cells / styling) |
| [ios](ios/index.md) | iOS 固有の公開 API・Bridge 境界 |
| [android](android/index.md) | Android 固有の公開 API・Bridge 境界 |
| [maui](maui/index.md) | .NET MAUI 固有の知識 (Bridge 境界・binding 構成) |
| [cross](cross/index.md) | リポジトリ横断のメタ事項 (リポジトリ構成・命名規約) |

[log.md](log.md) は全ドメイン共通の append-only 履歴。

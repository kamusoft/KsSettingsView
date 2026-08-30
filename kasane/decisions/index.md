# ADR 一覧 (ドメイン地図)

ADR はドメイン別に管理する ([cross/ADR-0015](cross/0015-domain-axis-core-plus-platforms.md))。参照の正式形は `<domain>/ADR-NNNN` (同一ドメイン内からは `ADR-NNNN` だけでもよい)。

| ドメイン | 説明 |
|---|---|
| [core](core/index.md) | 全 platform が共有する契約の決定 |
| [ios](ios/index.md) | iOS 固有の決定 |
| [android](android/index.md) | Android 固有の決定 |
| [maui](maui/index.md) | .NET MAUI 固有の決定 |
| [cross](cross/index.md) | リポジトリ横断のメタ決定 (リポジトリ構成・命名・docs 運用・ハーネス運用) |

採番規則: 旧フラット時代の ADR (0001〜0015、欠番 0012) は番号を温存したまま各ドメインへ再配置した。新規はドメインごとに「そのドメインの既存最大値 + 1」から採番する (既存 ADR のないドメインは 0001 から)。

# maui 目次

.NET MAUI 系統 (maui/ ビルドルート、および両 OS の Bridge 層) 固有の知識。カテゴリ定義と配置基準は [../rules.md](../rules.md) を参照。

## api/

- [api/maui-facade.md](api/maui-facade.md) — XAML / C# から SettingsView を利用する facade 層の公開契約 (公開 API・CustomCell・双方向バインド・更新の意味論・lifecycle・配置制約)
- [api/native-bridge.md](api/native-bridge.md) — C# から Native SettingsView を操作する Bridge 層の interop 境界 (内部所有 Store・ID 採番・操作通知・lifecycle・binding 構成)

## architecture/

- [architecture/binding-build-integration.md](architecture/binding-build-integration.md) — MAUI binding が iOS xcframework と Android aar を生成・取り込みする構成、既知の制約、SDK 更新時の再検証箇所
- [architecture/view-materialization.md](architecture/view-materialization.md) — MauiView を platform view へ実体化する facade 内の共有基盤 (seam 契約・自己計測 wrapper・論理所有と lease の寿命分離・退役順序・native への埋め込みの継ぎ目。accessory View と CustomCell の Content が利用)

## conventions/

- [conventions/integration-host-verification.md](conventions/integration-host-verification.md) — IntegrationHost と MauiHost を両 OS で起動し、binding / facade の end-to-end 疎通を確認する手順
- [conventions/performance-verification.md](conventions/performance-verification.md) — 描画性能を測るビルド構成の規約 (Android Debug はインタープリタ実行で実力より大幅に遅く見えるため、性能の評価・裏取りは Release で行う。Pixel 6a 実測付き)

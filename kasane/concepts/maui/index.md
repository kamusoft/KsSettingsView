# maui 目次

.NET MAUI 系統 (maui/ ビルドルート、および両 OS の Bridge 層) 固有の知識。カテゴリ定義と配置基準は [../rules.md](../rules.md) を参照。

## api/

- [api/maui-facade.md](api/maui-facade.md) — XAML / C# から SettingsView を利用する facade 層の公開契約 (導入と前提 — NuGet 座標・要件・型名衝突、公開 API・CustomCell・双方向バインド・更新の意味論・lifecycle・配置制約)
- [api/native-bridge.md](api/native-bridge.md) — C# から Native SettingsView を操作する Bridge 層の interop 境界 (内部所有 Store・ID 採番・操作通知・lifecycle・binding 構成)

## architecture/

- [architecture/binding-build-integration.md](architecture/binding-build-integration.md) — MAUI binding が iOS xcframework と Android aar を生成・取り込みする構成、既知の制約、SDK 更新時の再検証箇所、NuGet 3 パッケージの pack 構成 (共通設定の置き場・最低 OS 版ガード・受け入れた SDK 挙動・自 assembly 用 aar の除外・restore 元の固定)
- [architecture/view-materialization.md](architecture/view-materialization.md) — MauiView を platform view へ実体化する facade 内の共有基盤 (seam 契約・自己計測 wrapper・論理所有と lease の寿命分離・退役順序・native への埋め込みの継ぎ目。accessory View と CustomCell の Content が利用)

MAUI 系統の規約・手順 (検証ホストの実行・描画性能の計測構成) は [handbook: maui](../../handbook/maui/index.md) にある。

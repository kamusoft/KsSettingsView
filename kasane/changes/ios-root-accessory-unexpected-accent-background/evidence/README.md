# iOS View accessory 背景修正の実行時証跡

## 環境

| 項目 | 内容 |
|---|---|
| 端末 | iOS Simulator「iPhone 17」(iOS 26.5) |
| Native アプリ | `samples/ios/KsSettingsViewSample.xcodeproj` の `KsSettingsViewSample` (Debug) |
| MAUI アプリ | `samples/maui/KsSettingsView.Sample.Maui` (Debug、`net10.0-ios`) |
| 撮影日 | 2026-09-22 |

保存した 6 枚を原寸で目視した。写っているのは Sample または共有元アプリの画面と端末の標準ステータス表示だけで、通知、アカウント名、個人名などの個人情報は含まれない。

## 別アプリから共有された Section の再現

2026-09-22 に別アプリの利用画面として共有された `reported-section-accessory-blue-1.png` と `reported-section-accessory-blue-2.png` では、Root だけでなく View 形式の Section Header / Footer の領域にも system tint と同じ鮮青色が現れている。

この追加例も Root と同じ supplementary cell 共通描画を通る。修正は Root 専用分岐ではなく共通描画の View 形式へ適用し、回帰テストでは Section Header / Footer と Root Header / Footer の双方を覆う。

## Native Sample の修正前後 A/B

同じ Simulator、同じ Native Sample の「DSL 方式デモ」、同じ Root Footer を比較した。

修正前は、製品コードの対象行だけを一時的に `.clear` から従来の `nil` へ戻してビルドした。撮影後すぐ `.clear` に復元し、対象ファイルの SHA-256 が一時変更前の `fd3cd43489806f41023cfd3ab799cdb52ce92a82f840ee2284f95022a364a0b2` と一致することを確認した。作業ツリーへ修正前コードは残していない。

| ファイル | 観測結果 |
|---|---|
| `before-native-dsl-root-footer.png` | View 形式の Root Footer の cell 全体が system tint の青で塗られ、caption の可読性が落ちる |
| `after-native-dsl-root-footer.png` | 青い背景が消え、利用者 View の背後に list 下地が見える。caption は通常どおり読める |

修正前後とも次の手順で確認した。

1. `KsSettingsViewSample` を Simulator 向け Debug でビルドし、インストールする
2. アプリを起動して「DSL 方式デモ」を開く
3. 画面下端の Root Footer を撮影する

## MAUI Sample の修正後確認

ネイティブ変更が古い成果物に隠れないよう、MAUI 本体、iOS Binding、MAUI Sample の各 `bin` / `obj` を `trash` で除いてから、次を実行した。

```bash
dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj \
  -f net10.0-ios -c Debug
```

結果は成功、警告 0、エラー 0。Simulator へインストールし、「Header / Footer への View 配置デモ」を開いて `after-maui-root-header.png` と `after-maui-section-header-footer.png` を撮影した。

観測結果は次のとおり。

- XAML で利用者が指定した Root Header の淡青色背景はそのまま表示される
- Root Header の直下にあった未指定の鮮青帯は消え、その領域には list 下地が見える
- Section Header / Footer は利用者が指定した淡緑色背景を保ち、その外側の余白には system tint ではなく list 下地が見える
- Header の内容、後続セル、ナビゲーションの表示に目視上の崩れはない

## 判定

Native Sample の同一環境 A/B で原因となる system tint 背景が消えた。MAUI Sample でも Root と Section の双方で利用者指定色を保ったまま余計なアクセント色だけが消えたため、View 形式 accessory の背景を明示的な透明色にする修正は意図どおり動作している。

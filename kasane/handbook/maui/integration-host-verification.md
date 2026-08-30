---
kind: rule
applies-when:
  always: false
  paths: ["maui/**"]
  tasks: [binding / facade の end-to-end 疎通確認]
title: MAUI 検証ホストの実行規約
description: IntegrationHost と MauiHost を起動して binding 層と facade 層の end-to-end 疎通を確認する手順
timestamp: 2026-08-29
---

# MAUI 検証ホストの実行規約

この文書は、MAUI の検証ホストを iOS Simulator / Android Emulator で起動し、C# から Native 表示までの疎通を確認する手順を定める。読むと、IntegrationHost と MauiHost の役割、起動コマンド、成功時に確認する表示が分かる。

検証対象に出てくる Builder / Host の生成と解放 / root 設定 / Theme といった interop の語は [Native Bridge の interop 境界](../../concepts/maui/api/native-bridge.md) が定義する。環境の準備 (Xcode の選択・Simulator と Emulator の起動) は [ローカル開発環境と Sample の実行](../cross/local-development-setup.md) を参照する。

## 検証対象

| host | 対象 | 主な確認 |
|---|---|---|
| `KsSettingsView.IntegrationHost.iOS` / `.Android` | binding 層 | Builder、Host 生成・解放、root 設定、更新 API、Theme が C# から Native へ届くこと |
| `KsSettingsView.MauiHost` | facade 層 | XAML の Section / Cell、内容更新、ページ再訪問後の復元が両 OS で成立すること |

検証ホストは回帰確認用の資産であり、使い捨てのサンプルではない。binding の生成構成は [MAUI binding の Native artifact 統合](../../concepts/maui/architecture/binding-build-integration.md)、facade の公開契約は [MAUI facade の公開契約](../../concepts/maui/api/maui-facade.md) を参照する。

## Xcode の選択

`.NET for iOS` が要求する Xcode と `xcode-select` の選択が異なる環境では、`DEVELOPER_DIR` を明示する。binding project 単体では Xcode 版の不一致を検出せず、host application のビルドで初めて失敗する場合がある。

```bash
export DEVELOPER_DIR=<使用する Xcode.app の Developer ディレクトリ>
```

以下の iOS 手順に出る `<simulator-udid>` は `xcrun simctl list devices available` で確認し、`xcrun simctl boot <simulator-udid>` で起動しておく。Android の接続先は `adb devices` で確認する。

## IntegrationHost

### iOS

```bash
dotnet build maui/tests/KsSettingsView.IntegrationHost.iOS/KsSettingsView.IntegrationHost.iOS.csproj -c Debug

xcrun simctl install <simulator-udid> \
  maui/tests/KsSettingsView.IntegrationHost.iOS/bin/Debug/net10.0-ios/iossimulator-arm64/KsSettingsView.IntegrationHost.iOS.app

xcrun simctl launch <simulator-udid> jp.kamusoft.kssettingsview.integrationhost.ios
```

### Android

```bash
dotnet build maui/tests/KsSettingsView.IntegrationHost.Android/KsSettingsView.IntegrationHost.Android.csproj \
  -c Debug -t:Run -p:AdbTarget="-s emulator-5554"
```

接続先が 1 台だけなら `AdbTarget` は省略できる。複数台を接続している場合は `adb devices` で確認した serial を指定する。

### 期待される表示

起動時に `maui/tests/shared/KsBridgeScenario.cs` の固定シナリオが自動実行される。次の表は、そのシナリオが root 設定と更新 API の適用を終えた後の表示である。

| 位置 | 内容 |
|---|---|
| root header | `KsSettingsView Bridge` |
| Section 1 | 「一般」、テーマ=`ダーク`、言語=`English`、footer「アプリ全体の設定」 |
| Section 2 | 「通知設定」、プッシュ通知=`オン` |
| Section 3 | 「ストレージ」、バージョン=`0.1.0`、キャッシュ=`0 MB`、同期=`無効`、footer「端末内に保存されたデータ」 |
| root footer | 「C# から Native Bridge を操作しています」 |

Section header は Theme で指定した緑色になる。両 OS で表の内容が一致することを確認する。

画面上の「解放 → 再生成」を操作すると、Host の解放中に別の固定更新シナリオを適用してから、新しい Host を **Store (Bridge 側が保持する設定ツリーの状態。詳細は [Native Bridge の interop 境界](../../concepts/maui/api/native-bridge.md)) の現在状態**で作り直す。再生成後の表示は次のとおり。

| 位置 | 再生成後の内容 |
|---|---|
| root header | 表示されない (Store の復元対象ではないため) |
| Section 1 | テーマ=「解放中に更新」、言語=`Français` |
| Section 2 | 「通知設定 (解放中に更新)」 |
| Section header の色 | オレンジ |
| root footer | 表示されない (同上) |

root header / footer が消えるのは正常である。

## MauiHost

MauiHost は 1 つの project が iOS / Android の両方を対象にする (`TargetFrameworks` が 2 つ) ため、**IntegrationHost と違い `-f` でターゲットを指定する**。

### iOS

```bash
dotnet build maui/tests/KsSettingsView.MauiHost/KsSettingsView.MauiHost.csproj \
  -f net10.0-ios -c Debug

xcrun simctl install <simulator-udid> \
  maui/tests/KsSettingsView.MauiHost/bin/Debug/net10.0-ios/iossimulator-arm64/KsSettingsView.MauiHost.app

xcrun simctl launch <simulator-udid> jp.kamusoft.kssettingsview.mauihost
```

### Android

```bash
dotnet build maui/tests/KsSettingsView.MauiHost/KsSettingsView.MauiHost.csproj \
  -f net10.0-android -c Debug -t:Run -p:AdbTarget="-s emulator-5554"
```

次の最小手順を両 OS で確認する。

1. 「設定画面を開く」を操作し、XAML で定義した「テーマ」Cell の ValueText が「ライト」と表示されることを確認する。
2. 設定画面の「ValueText を更新」を操作し、ValueText が「更新 1」になることを確認する。
3. 戻る操作でメニューへ移動し、「離脱中に ValueText を更新」を操作する。
4. 同じ「設定画面を開く」から再訪問し、ValueText が「更新 2」のまま表示されることを確認する。
5. メニューへ戻って「離脱中に Cell を追加」を操作し、再訪問後に「追加 1」Cell が表示されることを確認する。

設定画面内の残りのボタンは、Header View の内容・固定高さ・Section 可視性・View の差し替えと取り外しを個別に確認する入口である。**完了条件には含まない**ので、該当箇所を触ったときに使う。

## 完了条件

- host application が対象 Simulator / Emulator で起動する。
- IntegrationHost の固定シナリオが期待される内容を表示し、両 OS で一致する。
- MauiHost の内容更新とページ再訪問後の復元が両 OS で成立する。

上記を確認するとき、`DEVELOPER_DIR` や接続先 ID は暗黙のローカル状態に依存させず実行時に明示する (どの Xcode / どの端末で確認したかが結果に効くため)。

## 関連

- [MAUI binding の Native artifact 統合](../../concepts/maui/architecture/binding-build-integration.md)
- [MAUI facade の公開契約](../../concepts/maui/api/maui-facade.md)
- [Native Bridge の interop 境界](../../concepts/maui/api/native-bridge.md)
- [ローカル開発環境と Sample の実行](../cross/local-development-setup.md) — Xcode の選択・Simulator / Emulator の起動

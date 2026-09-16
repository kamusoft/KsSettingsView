# Exploration: maui-android-fast-deployment-xa0126

## 課題 / 動機

maui-appearance-change-tracking (2026-09-15) の実装ゲート (tasks 0.1 の MauiHost Android での再現確認) と spike 5.1 (MAUI Sample の Android 配備) で、handbook どおりの Emulator 配備コマンドが 2 回とも失敗し、回避策を足して初めて配備できた。handbook `cross/local-development-setup.md` (MAUI Sample の配備節) と `maui/integration-host-verification.md` (MauiHost の起動節) はどちらも `-t:Run` を正とし、この失敗と回避策に触れていない。

### 調査済みの内容 (同じ調査を繰り返さないための記録)

- **失敗するコマンド** (handbook の記載どおり): `dotnet build <MauiHost または Sample の csproj> -f net10.0-android -c Debug -t:Run -p:AdbTarget="-s emulator-5554"`
- **エラー**: `error XA0126` (Fast Deployment の高速展開ツールがアセンブリ群を端末へコピーする段階で失敗)。ビルド自体は通り、配備で止まる
- **成功した回避策**: 同じコマンドに `-p:EmbedAssembliesIntoApk=true` を足す (Fast Deployment を止め、アセンブリを apk に埋め込む)。ビルドフラグのみでリポジトリのファイル変更は不要。MauiHost・Sample の両方で有効だった
- **環境**: macOS、Android Emulator (Pixel 6 相当 / API 35、serial `emulator-5554`)、.NET 10 SDK + MAUI workload。Emulator は同日に 2 回起動し直しても同じ症状
- **未確認**: (1) 原因が Emulator 側 (API 35 の Fast Deployment 非対応・ストレージ権限) か SDK / workload 側かは切り分けていない。(2) 実機で同じ症状が出るか未確認。(3) `-p:EmbedAssembliesIntoApk=true` で配備した apk と Fast Deployment の apk で、handbook が警告する「`adb install` 単体では古い内容が走る」の挙動が変わるか未確認 (埋め込みなら apk 単体で走るはず)。(4) csproj の Debug 構成に `EmbedAssembliesIntoApk` を既定で入れる案の副作用 (ビルド時間・配備サイズ) は未計測

関連: handbook `cross/local-development-setup.md` 157〜162 行 (MAUI Sample の Android 配備と Fast Deployment の注意)、`maui/integration-host-verification.md` 54〜57 / 105 行 (MauiHost の配備)。発見元の証跡は `../maui-appearance-change-tracking/tasks.md` 0.1 の注記と 5.1 の注記。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- 原因の切り分け (Emulator / API 版 / workload 版のどれに依るか) をしてから handbook に書くか、切り分けせず「XA0126 が出たら `-p:EmbedAssembliesIntoApk=true` を足す」の回避策だけを両 handbook に追記するか
- MauiHost / Sample の csproj の Debug 構成で既定にするか (配備手順を短くできるが Fast Deployment の利点を捨てる)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (handbook への回避策追記だけなら S 級の見込み)

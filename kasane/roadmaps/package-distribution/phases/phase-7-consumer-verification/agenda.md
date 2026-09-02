# phase-7-consumer-verification

配布物を参照する消費者プロジェクトを `verification/` に platform ごとに持ち、publish 前の dry-run (ローカルフィード) と publish 後の smoke (実レジストリ) で配信経路を検証できるようにする。Sample はソース参照のまま維持する。

## 論点


## 決定事項

### 検証範囲は「解決 + Release ビルド」まで。起動・`dotnet publish`・実機は含めない

消費者検証 (dry-run / smoke とも) が確かめるのは配信経路 — レジストリからの解決、メタデータ、推移依存、利用者側 SDK での Release ビルド — であり、ライブラリの実行時挙動はユニットテストと Sample の目視 (sample-parity / integration-host-verification の手元手順) が担う。phase-3 の「検証ホストの E2E は CI に載せない」と同じ役割分担。MAUI の Release ビルドは trimming / R8 / AOT まで走り linked アセンブリの残存を検出できる (phase-6 証跡) ため `dotnet publish` は追加検証にならず、実機 RID は public CI に署名情報を置く必要があり採らない。Simulator / Emulator 起動は CI で最も不安定・高コストで、publish 前ゲートの信頼性を下げるため含めない。実行時のみ出る欠陥の受け皿は手元の Sample 起動と初回リリース後の実機確認 (人の作業) (2026-09-02)。

### `verification/` は iOS = SwiftPM パッケージ、Android・MAUI = アプリ。README 最小例を無編集で同梱し lint で一致を担保する

`verification/{ios,android,maui}/` の platform 別に消費者プロジェクトを置く。iOS は phase-4 の https 解決検証で使った消費者パッケージの形 (`Package.swift` + 1 target、`platforms: iOS 16`) — README の例は SwiftUI View でライブラリ target にコンパイルでき、pbxproj を持たないため DEVELOPMENT_TEAM の書き戻し事故が構造的に起きない。Android は `com.android.application` の app module 1 つ — 利用者が踏む manifest merger の minSdk 検査 (phase-6 で AndroidX minSdk 23 の衝突を検出した経路) と R8 / dex マージは app ビルドでしか走らない。MAUI は `dotnet new maui` 相当のアプリ (README 例の `MauiProgram` を含むため)。phase-6 証跡 7-2 の MyApp が雛形。README (英語) の最小例 3 コードブロックを各消費者のソースとして 1 文字も変えずに置き、README とファイルの一致は `scripts/` の Python lint 1 本で検査して CI の lint job に足す (phase-9 申し送り「README の例が実際にビルドできるか未検証」の恒久解消)。`README_ja` は既存の docs-refresh の英日同期に任せる (2026-09-02)。

### dry-run の参照先は iOS = スナップショットの `path:`、Android = mavenLocal、MAUI = ローカルフォルダフィード + packageSourceMapping

| platform | 方式 | 取得元の保証 |
|---|---|---|
| iOS | `scripts/spm-snapshot/sync-snapshot.sh` で一時ディレクトリ `KsSettingsView-SPM` (identity をディレクトリ名から実レジストリと同一にする) にスナップショットを配置し、消費者 `Package.swift` が `.package(path:)` で参照。配信リポジトリに tag を打たない | 配置先が唯一の解決先 |
| Android | `publishToMavenLocal` で発行し、消費者 repositories に `mavenLocal()` を `content { includeGroup("jp.kamusoft") }` 付きで置く。他依存は Google / Maven Central | `includeGroup` |
| MAUI | `dotnet pack -o` のフォルダをフィードにし `nuget.config` に nuget.org と併記。packageSourceMapping で `KsSettingsView.*` はローカルフィード、それ以外は nuget.org | mapping (フィードに無ければ NU1101 で落ちる) |

prerelease tag 方式は phase-4 のオーナー裁定 (検証用 tag を配信リポジトリに残さない) と cross/ADR-0020 (tag は publish 全成功後) に緊張しつづけるため採らない。Central Portal 保留状態からの取得は upload 後にしか動かせず publish 前の dry-run にならない (smoke の領分)。(2026-09-02 提案化のセカンドオピニオンで訂正) Gradle の `content { includeGroup }` は排他ではなく他リポジトリの検索を止めないため `exclusiveContent` で割り当てる。NuGet の mapping は global packages folder に既にあるパッケージには働かないため、実行ごとに空の packages path を使い、取得元はパッケージ単位の `.nupkg.metadata` で検査する (事後検査は不要にならず、mapping と併用する)。mapping は phase-8 申し送りの NU1507 恒久対処 (候補 a) と同型で先行実証になる。実レジストリとの経路差 (https 解決・Portal 検証・nuget.org インデックス反映) は smoke が埋める (2026-09-02)。

### dry-run / smoke は 1 構成 + 2 引数 (モードと version) で切り替える

消費者コード (README 最小例) は platform ごとに 1 部だけ持ち、モード (dry-run / smoke) と version を外から受け取る。version はリポジトリのファイルに置かない (cross/ADR-0020) ため smoke の version は必ず引数になり、静的な 2 構成は成立しない。

| platform | version | 参照先 |
|---|---|---|
| iOS | `Package.swift` をテンプレートから生成 (smoke は `url:` + `exact:`、dry-run は `path:`) | 生成時の引数 |
| Android | Gradle プロパティ (`-Pkssettingsview.version=`) | 同プロパティで `mavenLocal()` / `mavenCentral()` を切り替え、`includeGroup("jp.kamusoft")` は両方に付ける |
| MAUI | MSBuild プロパティ (`-p:KsvVersion=`) を `PackageReference` の `Version` へ | `nuget.config` はプロパティを読めないため dry-run 用 / smoke 用の 2 ファイル (差分は mapping の向き先 1 行) を `-p:RestoreConfigFile=` で選ぶ |

iOS で環境変数を `Package.swift` 内で読む形は、SwiftPM / Xcode がマニフェスト評価をファイル内容で cache し環境変数の変更が反映されない落とし穴があるため採らない (2026-09-02)。

### CI へは再利用可能 workflow 1 本 + platform 別スクリプトを届け、PR / push の検証 CI にも dry-run を載せる

platform 別の `verify-consumer-{ios,android,maui}.yml` 3 本 (workflow_call、入力 `mode` = dry-run / smoke と `version`、publish 成果物を受け取る `artifact`。2026-09-02 提案化のセカンドオピニオンで「1 本 3 job」から cross/ADR-0025 の形に改めた — 呼び出し側と呼ばれる側の runner は別で成果物は artifact 経由でしか渡らない) と `verification/<platform>/` の実行スクリプトを本フェーズで作る。スクリプトは「フィード準備 (スナップショット配置 / `publishToMavenLocal` / `pack`)」と「消費者ビルド」の 2 段に分け、release では publish する成果物そのものを消費者に渡せる形にする。検証 CI (`ci.yml`) は `mode=dry-run` + 開発用 version (Android `0.1.0-SNAPSHOT` / MAUI `0.0.0-dev` を消費者側の既定にも使う) で毎 PR 動かし、README 例とパッケージメタデータの壊れをリリース直前まで持ち越さない (phase-9 申し送りの意図)。release workflow (phase-8) は同じ workflow を publish 前 (`dry-run`) と publish 後 (`smoke`) に 1 回ずつ呼ぶ — smoke の呼び出し自体は phase-8 の実装。本フェーズの受け入れ条件は dry-run の CI 通過と、手元での smoke 実行が動くことまで (2026-09-02)。

### MAUI 消費者は restore 警告をエラー化し、binding 2 件が facade と同版で解決されたことを検査する

NU1605 / NU1608 / NU1107 を `WarningsAsErrors` でエラーにし (phase-6 の「restore 警告 0 件」要求の CI 固定)、`project.assets.json` から `KsSettingsView.Binding.iOS` / `.Android` の解決版が facade と一致することを Python 数行で検査する (lockstep cross/ADR-0019 の消費者側の現れ。facade → binding は下限指定のため smoke で古い binding が混じる事故はこの検査でしか捕まらない)。ビルド警告全体はエラーにしない (Android Release の XA4301 4 件は phase-8 で扱う)。AndroidX の解決版は期待値を持たない (CPM 更新との二重管理になり、競合の不在は NU1107 / NU1608 で担保済み)。Android / iOS の消費者は解決結果 (`dependencies` 出力 / `Package.resolved`) を証跡に残すだけに留める (2026-09-02)。

### `verification/` はパリティ対象外。ADR-0016 / sample-parity 規約は改訂せず、役割は concepts に記述する

sample-parity 規約の適用範囲は `samples/**` で閉じており、cross/ADR-0016 の決定文も `verification/` に触れないため、対象外であることは改訂なしに読み取れる。規約に対象外の列挙を足すと規範が二重化し、`verification/` の中身 (README 最小例の写し) が 3 platform で一致するのは README 一致 lint の帰結であって独自の一致義務ではない。`verification/` の役割 (配信経路の検証装置、README 最小例の写し、パリティ対象外、`samples/` はソース参照のまま) は concepts `cross/architecture/repository-boundaries.md` に 1 段落として蒸留時に追随する (2026-09-02)。

## TODO

- [x] 論点の解消 (2026-09-02 全 7 論点)
- [ ] ksn-propose で変更提案を起こす
- [ ] **決定「CI への届け方」に伴う人の作業** (2026-09-02): 消費者検証 3 job を `develop` の branch protection に必須 status check として追加する (change の実装完了時)。phase-8 の `main` 作成申し送りにも追記済み
- [ ] **docs-refresh の明示依頼をユーザーへ依頼する** (phase-5・6 からの申し送りの併合): 内容は下表「docs-refresh 依頼の内容」。phase-6 は change 完了直後の依頼を求めており、本フェーズの着手を待たずに依頼してよい。API 版付き TFM の SDK 要件は下の確認結果を待たず「SDK 10.0.300 で検証」の形で先に載せてよい
- [ ] **phase-6 からの申し送り: API 版付き TFM の解決要件** (2026-09-02): change のタスクとして実測し、結果を docs-refresh 依頼へ追加する (詳細は下表)

### API 版付き TFM の解決要件の実測 (phase-6 申し送り)

| 項目 | 内容 |
|---|---|
| 確認対象 | facade の TFM `net10.0-android36.0` / `net10.0-ios26.0` を、利用者側の SDK 版 / `TargetPlatformVersion` がどこまで古くても解決できるか |
| 方法 | 古い `TargetPlatformVersion` を固定した消費者での解決可否を実測する (change のタスク) |
| 出口 | README / skills の互換情報に SDK 要件として載せる (docs-refresh 依頼へ追加) |

### docs-refresh 依頼の内容 (phase-5・6 からの申し送りの集約)

| platform | 追随内容 |
|---|---|
| Android (phase-5) | skills/ とルート README の module 統合追随、互換情報 Kotlin 2.3+ / minSdk 29 / compileSdk 35 の明記 |
| MAUI (phase-6) | 名前空間 `KsSettingsView` / xmlns `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui` への追随 (skills maui + aiforms-migration。README の例は change 内で追随済み) |
| MAUI (phase-6) | 互換情報: `Microsoft.Maui.Controls` 10.0.70 以上と NU1605 の注意、最低 OS 版 Android 29 / iOS 16.0 とビルド時ガード `KSSV0001` |
| MAUI (phase-6) | `SwitchCell` / `EntryCell` が MAUI 本体の同名型と衝突するため C# では完全修飾または using alias が要る注意書き |
| MAUI (phase-6) | API 版付き TFM の SDK 要件 (「SDK 10.0.300 で検証」の形で先に載せてよい) |

### 完了した申し送り

| 申し送り | 結果 |
|---|---|
| phase-9 (2026-08-30): `verification/` に README / skills と同じ最小コード例を入れて CI でビルドさせる | 決定「`verification/` の構成」で README 例の無編集同梱 + 一致 lint として確定 |
| phase-5 (2026-09-01): Central Portal で `jp.kamusoft` を登録し DNS TXT 検証を通す (人の作業) | 2026-09-01 完了 (namespace 追加 → apex に TXT → Verified → TXT 削除済み)。KsDialogs 含む `jp.kamusoft` 配下で共用できる |
| phase-5 (2026-09-01): Explicit API mode の導入を消費者検証と併せて実施するか判断する | [changes/archive/2026-09-01-adopt-android-explicit-api-mode](../../../../changes/archive/2026-09-01-adopt-android-explicit-api-mode/exploration.md) として 2026-09-01 に M 級で実装・蒸留済み。本フェーズの検証は Explicit API 適用後の API 面に対して行う (逆順の懸念は解消) |
| phase-6 (2026-09-02): MAUI 消費者の `nuget.config` 設計 (`<clear/>` + ローカルフィードのみでは NU1101、nuget.org 併記 + 隔離 packages path + `.nupkg.metadata` 検査。証跡 [consumer-verification.txt](../../../../changes/archive/2026-09-02-add-maui-nuget-distribution/evidence/consumer-verification.txt) 0 節・6-1) | 決定「dry-run の参照先」で packageSourceMapping 方式に置き換え (nuget.org 併記は維持、事後検査は不要) |
| phase-6 (2026-09-02): `dotnet publish` (フル trimming) と実機起動を smoke に含めるか | 決定「検証範囲」で含めないと確定。Release ビルドまでで trimming 後のアセンブリ残存は確認済み、同梱 README 最小例の無編集ビルド証跡が MAUI 消費者の雛形 |

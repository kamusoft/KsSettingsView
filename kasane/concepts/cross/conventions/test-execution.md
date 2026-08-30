---
type: policy
title: テスト実行規約
description: iOS / Android / MAUI のテストの正しい実行コマンドと、黙って検証にならない範囲 (macOS 上の swift test で失われるテスト・Robolectric の描画検証限界と非同期反映の待機・MAUI facade テストが触らない platform TFM)
tags: [test, ios, android, maui, verification]
timestamp: 2026-08-29
---

# テスト実行規約

この文書は、各 platform のテストを「実際に全件走らせる」ための実行方法と、**実行や検証が黙って空振りする範囲**を定める。読むと、どのコマンドで何が実行され、何が黙って検証にならないかが分かる。

テストが 1 件も実行されなくてもコマンド自体は成功で終わるため、終了コードだけでは検証したことにならない。**実行件数を確認するところまでが検証**であり、テスト結果を報告するときは platform を問わず実行件数 (`N tests / M failures`) を併記する。件数の得方は各 platform の節に示す。

iOS / Android / MAUI の 3 platform を記載する。いずれも実際に実行して確かめた手順である (未検証の手順は書かない)。

## iOS

### 正しい実行方法

```
cd ios
xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=<機種名>'
```

- Swift Package のルートは `ios/` であり、**リポジトリルートで実行すると `does not contain an Xcode project` で失敗する**
- scheme は `KsSettingsView-Package` (パッケージ全体) を使う。個別ターゲットの scheme (`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI`) もあるが、全件検証にはパッケージ全体の scheme を使う
- `<機種名>` は手元で利用可能な Simulator の機種名に置き換える。一覧は `xcrun simctl list devices available` で得られる
- 実行件数は `xcodebuild` 出力末尾の `Executed N tests, with M failures` で確認する

### `swift test` では検証にならない

`ios/Tests/` の 3 テストターゲットはいずれも `#if canImport(UIKit)` でガードされたテストを含む。macOS 上で `swift test` を実行すると、これらは**コンパイル対象から外れ、失敗ではなく最初から存在しないものとして扱われる**。

ガードの及ぶ範囲はターゲットによって大きく異なる:

| テストターゲット | macOS 上の `swift test` での扱い |
|---|---|
| `KsSettingsViewUITests` | 全ファイルがガード対象。**1 件も実行されない** |
| `KsSettingsViewSwiftUITests` | ほぼ全ファイルがガード対象。**ほとんど実行されない** |
| `KsSettingsViewCoreTests` | 大半は実行される (一部のみガード) |

つまり `swift test` で実質的に走るのは Core のロジックだけである。UIKit の Cell レイアウト・Renderer に関わる変更も、SwiftUI Bridge に関わる変更も、検証の中心はガードされた側にある。`swift test` だけで完了と判断すると、**変更の中核が 1 件も検証されないまま「テスト全 pass」と報告される**ことになる。

`swift test` で走るのは全体の一部にすぎない (2026-08-01 実測: `swift test` 88 件 / Simulator 全件 338 件)。件数は変動するため、重要なのは**差の大きさ**であって値そのものではない。

したがって iOS の変更では、タスクリストや依頼文に「`swift test` を通す」としか書かれていない場合も Simulator 実行に読み替える。テスト結果を報告するときは実行件数 (`N tests / M failures`) を併記する。

### 一部のテストだけ回したいとき

反復中に特定ターゲットだけ回したい場合も、`swift test` に戻らず Simulator 実行のまま絞り込む:

```
cd ios
xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=<機種名>' -only-testing:KsSettingsViewCoreTests
```

こうすればガードの穴を作らずに実行範囲だけを狭められる。ただし**完了判定には絞り込みなしの全件実行を使う**。

### `swift test` を案内している文書は無い

リポジトリ内のどの文書も `swift test` をテスト手順として案内していない。ルート README は利用者の入口に純化されており開発者向けのビルド / テスト手順を持たず ([cross/ADR-0023](../../../decisions/cross/0023-readme-root-only-and-developer-knowledge-in-concepts.md))、利用者向けドキュメント (`skills/` の Agent Skills) もテスト実行手順を案内していない。**完了判定に使うのは上の Simulator 実行だけ**であり、他所で見かけた `swift test` を代替に使わない。

## Android

### 正しい実行方法

```
cd android
./gradlew test
```

- Gradle ビルドルートは `android/`。テストは Robolectric を含む JVM 単体テストで、debug / release の両 variant が実行される (2026-08-21 実測: 1261 件 × 2 = 2522 件。件数は変動する)。instrumented test (`androidTest/`) は現状存在しない
- Gradle は up-to-date なテストタスクをスキップするため、**差分なしの再実行は「テスト 0 件で BUILD SUCCESSFUL」になり得る**。全件を確実に回し直して件数を確認するときは `--rerun-tasks` を付ける
- 実行件数はコンソールに出ない。各モジュールの `build/test-results/testDebugUnitTest/TEST-*.xml` (release は `testReleaseUnitTest/`) の `tests` / `failures` 属性の合計、または `build/reports/tests/testDebugUnitTest/index.html` で確認する。**ディレクトリ名は variant 名 (`debug` / `release`) ではなくタスク名**であり、`debugUnitTest` 等と読み替えると集計対象が 0 件になる
- 反復中に絞り込むときは `./gradlew :ks-settingsview-ui:testDebugUnitTest --tests '<クラス名のパターン>'` を使えるが、**完了判定には絞り込みなしの全件実行を使う** (iOS と同じ規律)
- Gradle を動かす JDK は `JAVA_HOME` で選ぶ (JDK 17 / 21 / 25 で実測済み)。成果物のターゲットが Java 17 のため、どの JDK で動かす場合も JDK 17 がローカルにインストールされている必要がある ([Android ビルドツールチェーンの契約](../../android/architecture/build-toolchain.md))

### Robolectric で「検証したつもり」になる描画系アサーション

Robolectric の既定 (legacy graphics モード) では一部の描画処理が実行されず、描画結果を見るアサーションが空振りする。実測で確認済みの 2 点 (適用実例: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt`):

- **実 ellipsize**: legacy graphics では `TextUtils.ellipsize` が動作せず、`Layout.getEllipsisCount` が**常に 0 を返す**。末尾省略の発生を検証するテストにはクラスへ `@GraphicsMode(GraphicsMode.Mode.NATIVE)` が必要 (実 Skia を動かすため Robolectric nativeruntime の取得を伴い、起動コストと CI の環境依存が増える)
- **singleLine な TextView の実描画位置**: `isSingleLine = true` の TextView は内部 `Layout` の幅が `VERY_WIDE` (約 100 万 px) になり、`Layout` 座標は View 座標と一致しない。実描画位置は `viewTreeObserver.dispatchOnPreDraw()` で `TextView.bringTextIntoView()` の `scrollX` 補正を発火させてから `layout.getLineLeft(0) - scrollX` で測る。`root.draw(Canvas)` を呼ぶだけでは補正が入らない。得られる値は **content box (padding を除いた領域) の左端起点**であり、この経路は実機で毎フレーム描画前に走る補正そのものなので Robolectric 固有の抜け道ではない

### 非同期反映を待たないアサーション

`RecyclerView` の `ListAdapter` (`AsyncListDiffer`) は差分計算を**バックグラウンドスレッド**で行い、結果を main looper へ post して `currentList` / `itemCount` を更新する。post 前は main looper のキューが空であり、`shadowOf(Looper.getMainLooper()).idle()` も Compose の `waitForIdle()` も**即座に戻る**。idle 系の呼び出しを何度重ねても差分計算の完了は待てない。

したがって反映待ちは次の形で書く。いずれを欠いても「待ったつもり」になる:

- 上限は**実時間の deadline** で区切る。反復回数で区切ると、対象がバックグラウンドにある間にループが燃え尽きる
- ループ内でバックグラウンドスレッドへ実行機会を譲る (`Thread.sleep(1)` 等)。`Thread.yield()` は OS へのヒントに留まり、CPU が飽和した状況では譲れる保証がない
- deadline 超過時は黙って戻らず、その時点の実測値をメッセージに載せて `fail()` で落とす。黙って戻る待機は収束前の状態を検証したことにされ、「実装が壊れた」と「待機が足りない」も区別できなくなる

この失敗は CPU が競合したときだけ起きるため、単体では常に緑で、全モジュール並列実行 (`android/gradle.properties` の `org.gradle.parallel=true`) で間欠的に落ちる flaky として表面化する。適用実例: `ks-settingsview-compose` の `KsSettingsViewComposeTest.waitForAdapterItemCount` / `DSLAccessoryVisibilityRenderingTest.awaitRows`、`ks-settingsview-ui` の `KsSettingsViewTestSupport.awaitConvergence`。

## MAUI

### 正しい実行方法

```
dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj
```

- facade (`KsSettingsView.Maui`) の純ロジックを素の `net10.0` で検証する。Simulator / Emulator は不要 (2026-08-29 実測: 516 件 / 0 失敗、約 0.3 秒)
- 実行件数はコンソール末尾の `失敗: N、合格: M、スキップ: K、合計: T` (英語ロケールでは `Failed: N, Passed: M, Skipped: K, Total: T`) で確認する
- Binding assembly は platform TFM でしか参照できないため、facade は Bridge 呼び出しを internal な gateway 抽象越しに行い、gateway の実体だけを platform TFM が持つ ([maui/ADR-0009](../../../decisions/maui/0009-net10-tfm-and-gateway-seam.md))

### 黙って検証にならない範囲

**このコマンドは platform TFM (`net10.0-ios` / `net10.0-android`) のコードを 1 行も実行しない**。gateway の実体・binding 経由の native 呼び出し・実際の描画は対象外であり、facade のテストが全件通っても native まで届いているかは分からない。C# から native 表示までの end-to-end 疎通は検証ホストアプリで確認する ([MAUI 検証ホストの実行規約](../../maui/conventions/integration-host-verification.md))。

## 関連

- [リポジトリと platform build の責務分担](../architecture/repository-boundaries.md) — `ios/` / `android/` が独立したビルドルートである理由

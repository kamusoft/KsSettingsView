---
type: concept
title: MAUI Native Bridge の interop 境界
description: C# から Native SettingsView を操作する Bridge 層の公開契約 — 内部所有 Store・ID 採番・操作通知・lifecycle・binding 構成
tags: [maui, bridge, interop, binding]
timestamp: 2026-08-29
---

# MAUI Native Bridge の interop 境界

この文書を読むと、.NET MAUI 対応の土台である Native Bridge 層 (iOS / Android の `KsSettingsBridge`) が何を保証し、C# からの操作がどの経路で Native の表示へ届くかが分かる。前提となる Store と Host の一般契約は [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md) を先に読むと分かりやすい。決定の経緯は [maui ドメインの ADR 一覧](../../../decisions/maui/index.md) (ADR-0001〜0007) を参照。

## 目的

C# から Native (iOS Swift / Android Kotlin) の SettingsView を利用するための interop 境界。経路は常に次の一本である:

```
C# (Binding assembly) → Bridge → 内部所有 SettingsRootStore → Native Host
```

`SettingsRootStore` は設定画面の現在状態 (設定ツリーと Theme) を一元保持する既存の状態コンテナ、Native Host は Store を購読して設定画面を表示する既存の View / ViewController である。Bridge は Native 側に内部所有の Store を持ち、公開 API を Store の公開操作へ変換する (maui/ADR-0001)。狙いは Store → Host の既存保証 — 状態と通知の整合、複数 Cell 内容更新の1バッチ配信、同値 Theme の再適用スキップ、Host 接続時の現在状態からの表示復元 (詳細はリンク先) — をそのまま利用することで、Store を迂回する別の更新経路は作らない。

## 責務境界

| 層 | 実体 | 責務 |
|---|---|---|
| Binding | `maui/macios/KsSettingsView.Binding.iOS` / `maui/android/KsSettingsView.Binding.Android` (net10.0-ios / net10.0-android) | Bridge API を C# へ運ぶ層。**アプリ利用者向けの公開契約ではない** — MAUI 慣例型 (`Microsoft.Maui.Graphics.Color` 等) への変換は上位の facade 層の責務 (maui/ADR-0004) |
| gateway | facade 内部の `KsBridgeGateway` (per-TFM) | facade と Binding の間に立つ薄い C# 変換層。facade と同寿命で、facade 型 ⇔ DTO の変換と delegate / listener 実装の保持を担う (facade 内部実装であり公開契約ではない) |
| Bridge | iOS `ios/Sources/KsSettingsViewBridge/` / Android `android/ks-settingsview-bridge/` | `@objc` / JVM 互換の公開 API、Store 公開操作への変換、Store と Host の所有 (maui/ADR-0005) |
| Store / Host | 既存の `SettingsRootStore` / `KsSettingsViewController` / `KsSettingsView` | 既存契約のまま。Bridge 専用の特別な挙動はない |

## 公開 API の形

- 更新 API は Store 公開操作と 1:1 の 12 メソッド (maui/ADR-0002) + accessory view 系 2 メソッド (1:1 原則からの意図的な一歩 — maui/ADR-0017・0018) + Store 外の `setStyle` (下記)。union DTO や独自の diff 表現は持たない:
  - `setRoot` / Section 構造操作 4種 (`insertSection` / `removeSection` / `moveSection` / `replaceSection`) / Cell 構造操作 4種 (`insertCell` / `removeCell` / `moveCell` / `replaceCell`)
  - `updateAccessory` — accessory (root または指定 Section の header / footer テキスト) の更新と clear
  - `updateAccessoryView` — accessory への任意 platform view (iOS `UIView?` / Android `View?`、null でクリア) の設定。Bridge 内部で定数返し closure に包み (返す前に既存親から detach)、Store の `updateAccessory` 経路に合流する (maui/ADR-0017)
  - `invalidateAccessoryMeasurement` — accessory 内容のサイズ変化を native の行/領域高さ再計算へ届ける一過性通知。Store の復元可能状態は変えない (maui/ADR-0018)
  - `replaceCells` — 複数 Cell の内容更新を1バッチで適用
  - `setTheme`
  - `setStyle` — 設定 list の style (Classic / Modern) の設定。**Store 公開操作 1:1 の枠外にある唯一の更新 API** — Native 側でも style は Store ではなく Host (View / Controller) の可変プロパティが所有するため、Bridge も Store を経由せず Host の style プロパティを直接叩く (Native の Theme / style 分離との対称性。maui/ADR-0023)。輸送は enum 序数 int で、定義域外の序数は Classic へ正規化する。Bridge は style を Host 外のフィールドで保持し、`makeHost*` 生成時に適用・生きた Host には即時適用する — `releaseHost()` 後の再生成でも style は維持される
- Root の構築は Builder (`KsBridgeRootBuilder`) で行う。interop 境界を通せる Cell 種は既存 API を壊さず追加のみで増やす
- Cell DTO は **Cell 種ごとの per-type 展開** (maui/ADR-0011) — 共通基底 `KsBridgeCell` (ID 採番と共通フィールドを持つ) の派生として、native の個別 Cell 型と 1:1 の DTO (`KsBridgeSwitchCell` / `KsBridgeEntryCell` 等 13 種) を両OSに置く。`KsBridgeSection.cells` や置換 API は基底型で受ける。単一 wide DTO + cellType 判別は採らない
- `KsBridgeCustomCell` は行の内容として `view` (platform view。null は内容なし) と `contentToken` (その実体の世代を表す文字列) の対に加え、`showArrowIndicator` / `hasTapHandler` を輸送する。native の content にはトークンだけを格納するため、native から見た内容の等価性はトークンの値等価で決まり、同一トークンの間は再バインドが起きても埋め込まれる view は同一インスタンスのまま維持される (maui/ADR-0020)。共通行レイアウトのスロットを持たない Cell のため、基底の `title` / `descriptionText` / `valueText` / `hintText` / `icon` は native へ写さない
- 双方向値の輸送は maui/ADR-0012 の規約に従う: Bool / Int / String は素通し、Picker の選択は index (複数選択は昇順・重複除去の Int 配列)、Time / Date は壁時計値の ISO-8601 文字列 ("HH:mm" / "yyyy-MM-dd")。Picker の候補は per-item DTO (`KsBridgePickerItem`: 主表示 `text` + nullable `subText`) の列で運ぶ — 平行配列 (`texts` + `subTexts`) にしない。件数不一致と null 要素の binding 表現という不変条件が型で閉じ、Objective-C / .NET binding では nullable string プロパティとして表現できる (core/ADR-0029)。Optional を型で表せない scalar (uiStyle の enum 序数等) は「未指定を表す値」(負値センチネル等) を DTO ごとに定める (個々の表現は実装とテストが正)。DTO の未指定時の既定値は native 既定のリテラル複製ではなく native インスタンスからの導出で持つ — リテラルを複製すると native 側の既定が変わっても DTO 側が古い値のまま残り、誰も気づけないため
- `KsBridgeSection` は `isVisible`・`headerHeight` (未指定は native の自動高さ) に加え、`headerView` / `footerView` (platform view) も輸送する。text と view の両指定時は view 優先で native Section を構築する
- Native Host は Bridge が生成・公開する — iOS `makeHostViewController()` が返す ViewController を子 VC として embed し、Android `makeHostView(context)` が返す View を view 階層へ追加して表示する
- Theme は primitive (ARGB int・フォント記述子等) で表現した DTO `KsBridgeTheme` で受ける。DTO は iOS / Android の各 Bridge に同名で存在し、それぞれの platform の `Theme` 公開項目と名前まで 1:1 に対応する。未指定 (null) は Theme 側の未指定になる。Cell 単位のスタイル上書きは per-type DTO の style フィールド (`KsBridgeCellStyle`) で輸送する。icon は解決済みの platform 画像 (iOS `UIImage` / Android `Drawable`) を DTO に載せ、native の画像表現 `KsImage` のうち platform 画像を保持するケース (`uiImage` / `Drawable`) で受ける (解決は facade 側の責務 — maui/ADR-0015)。Section 装飾4属性は `KsBridgeTheme` のフラットな7フィールド (margin は論理4成分 top / leading / bottom / trailing、radius / borderWidth、borderColor は ARGB int。入れ子 DTO は作らない) で運び、margin は all-or-none — 部分 null は margin 全体を未指定として resolve する。Bridge の resolve が4成分から platform の directional 型 (iOS `NSDirectionalEdgeInsets` / Android `PaddingValues(start, end)`) を組み立てる。装飾値は検証せず生のまま運ぶ — 正規化 (負値・非有限 → 0) は Native の描画時のみが正で、Android は Compose 標準の `PaddingValues(...)` ファクトリが構築時に全成分 0 以上を要求するため、非検証実装 `KsBridgeSectionMargin` を用いて生値を描画時正規化まで届ける (maui/ADR-0024)

## ID の interop 契約

- Section / Cell の ID は canonical UUID 文字列。**Store 上で有効な ID を確定するのは Bridge (Builder / insert 系 API) であり、呼び出し側は API が返した ID だけを更新 API に渡す**
- DTO (`KsBridgeSection` / `KsBridgeLabelCell` 等) はインスタンス生成時に自分でも ID を採番して公開するが、Store 上の identity になるのは insert / Builder 経由で追加された時点の ID だけである。`replaceSection` / `replaceCell` は**対象の既存 ID を維持したまま**内容を作り直すため、渡した DTO 自身の `sectionID` / `cellID` は Store のどこにも存在しない ID になる — DTO が公開する ID を後続操作に使ってはいけない
- `replaceSection` / `replaceCell` の戻り値は置換後に有効な ID (= 対象の ID)。対象が見つからず no-op になった場合は null を返す
- **`replaceSection` の cellId 温存**: 置換 DTO の cells の cellID として既存の canonical UUID を指定して渡すと (Cell DTO は生成時に ID を指定できる)、配下 Cell は再採番されずその ID を維持する。facade はこれを同一 Section インスタンス由来の差し替え (`Section.IsVisible` / header 等の内容変更) に使い、双方向バインドの cellId 逆引きを保つ。別インスタンスへの置換は新規 Section 扱いで再採番してよい
- 未知・不正な ID を指定した Cell / Section 操作は no-op で、この検証と結果は iOS / Android で同一。`updateAccessory` の section 系 target (header / footer) も同じ契約に含まれる — canonical UUID でも未知の sectionID なら Store 側で no-op になり、state 更新も更新通知も発生しない (core/ADR-0020)。Root 系 target は `sectionID` 引数を参照しないため未知 ID 判定の対象外で、どんな sectionID を渡しても更新と通知が行われる

## lifecycle の保証

- Bridge は同時に 1 つの Host をサポートする。生きている Host がある間の `makeHost*` 再呼び出しは同じ handle (= `makeHost*` が返す Host 実体) を返す
- `releaseHost()` は Host のみを解放し、Store (設定ツリーと Theme) は維持する (maui/ADR-0007)。冪等で、Host 不在時および `dispose()` 後は no-op。解放時に旧 handle の Store 購読は解除・無効化される — 解放後の Store 更新は旧 handle の表示に反映されない。旧 handle の view 階層からの取り外しと参照破棄は呼び出し側の責務
- 解放後の `makeHost*` は Store 現在状態から表示を復元した**新しい** handle を返す。Host 不在中の更新は Store にだけ適用され、次の Host 生成時の表示復元で反映される — MAUI Handler の切断 (`releaseHost()`) / 再接続 (`makeHost*`) をまたいで Store 内容が保持されるのはこの機構による
- ただし root の header / footer は Store 現在状態に含まれない Host 単位のプロパティで、解放 → 再生成には引き継がれない (core/ADR-0019 で復元対象外と確定)。所有者が値を保持し、Host 生成のたびに `updateAccessory` で再適用する。Android では再適用を Host の view 階層への取り付け**後**に行う — attach 前は Store 購読が張られておらず root 対象の更新通知は黙って失われる (iOS は Host 生成時に購読を張るため順序に依存しない)
- 破棄 (`dispose`) は冪等。破棄後の全操作 API は no-op で、破棄後に Host の表示が更新されることはない。`dispose` は保持中の Host の購読解除を行わない — `releaseHost()` との非対称は意図的 (破棄後は Store が更新されないため表示も変化しない、maui/ADR-0005 の既存契約)。`dispose` 後は Bridge ごと参照を手放して破棄する前提であり、Host と Store が残留してもリークにはならない
- Android の `Context` は Host 生成 API の引数で受け取り、Bridge はフィールドとして保持しない。`releaseHost()` 後の Bridge は旧 Host が保持していた資源 (`Context` を含む) への参照を持たず、再生成は別の `Context` で行える。Activity より長いスコープに Bridge を置く場合は、切断時に `releaseHost()` を呼べば `Context` リークは生じない
- 全 API は各 platform の UI スレッドから呼ぶ (呼び出し側契約)。Bridge 自身は marshal しない

## してはいけないこと

- Binding assembly の型をアプリ利用者向け API として公開・文書化しない — これを破ると facade 層 (MAUI 慣例型) との二重契約になり、interop 表現の変更が破壊的変更になる
- DTO が公開する ID を「生きている ID」と見なさない — 有効なのは API が返した ID だけ。破ると以後の操作が無言 no-op になる
- 既存 UI モジュール (`KsSettingsViewUI` / `ks-settingsview-ui`) へ interop 都合の型を持ち込まない — Bridge が別モジュールなのはこのため

## ユーザー操作通知 (interaction delegate / listener)

Native → C# のユーザー操作通知は、**設定画面 1 つ (= Bridge インスタンス 1 つ) につき 1 個の通知チャネル**に集約する (maui/ADR-0003)。iOS は `@objc public protocol KsBridgeInteractionDelegate`、Android は `interface KsBridgeInteractionListener`:

- メソッドは Cell 種別ごとに分かれ (`switchCellChanged(cellID, isOn)` / `entryCellTextChanged(cellID, text)` / `datePickerCellChanged(cellID, date)` 等。全メソッドの一覧は実装と maui/ADR-0012 の書き戻し正規一覧が正)、値は maui/ADR-0012 の輸送規約 (index / ISO-8601 文字列) に従う
- タップだけを伝えるメソッド (`commandCellTapped(cellID)` / `buttonCellTapped(cellID)` / `customCellTapped(cellID)`) は値を運ばず、書き戻しの対象にもならない。`customCellTapped` は DTO の `hasTapHandler` が true のときだけ native Cell に `onTap` を持たせて通知する — false の行はタップ動作そのものを持たず、内容の中の操作を妨げない
- Bridge が DTO → native Cell 変換時に各 Cell のコールバック (`onTap` / `onValueChanged` 等) を注入し、delegate / listener へ転送する。通知は **native UI スレッド上で同期に**呼ばれる (marshal 不要)
- 寿命: iOS の delegate 参照は **weak**、Android の listener は Bridge が保持し null 設定で解除する。C# 側は gateway (前掲の表) が delegate / listener 実装を強参照で保持し、C# 側実装オブジェクトが GC されて native から呼べなくなることを防ぐ。登録は Handler 接続時・解除は切断時 (操作は Host 表示中にしか発生しないため connect / disconnect で必要十分)。native Cell 内のコールバックは通知転送用オブジェクトと cellId しか参照せず、facade インスタンスを GC から到達可能にしない (SettingsView の回収を妨げない)

## 現時点の範囲

- interop 境界を通せる Cell は 13 種 (LabelCell + 基本6 + 入力5 + CustomCell)。CustomCell が通せるのは platform view 1 個 + 世代トークンであり、利用者定義の Cell 型を登録する機構は通さない (maui/ADR-0019)。accessory は text と任意 platform view の両方を通せる (maui/ADR-0017)

## binding 構成の要点

- iOS: .NET SDK (Native Library Interop) の標準 `XcodeProject` アイテム + `CreateNativeReference=false` + 手動 `NativeReference` の構成。手動登録が必要な理由と増分入力の補正は [MAUI binding の Native artifact 統合](../architecture/binding-build-integration.md) を参照する
- Android: .NET SDK の `AndroidGradleProject` アイテムは複数モジュールの Gradle 構成で成立しないため、`android/gradlew` を Exec で直接呼び `AndroidLibrary` で束縛する (maui/ADR-0006)。Exec が `assembleRelease` するのは core / ui / bridge の 3 module (compose は束縛しない)。aar 再生成の判定入力には module ソースと `build.gradle.kts` のほか `android/gradle/libs.versions.toml`・wrapper 設定・`gradle.properties` を含める。Gradle JVM の要件と catalog の扱いは [Android ビルドツールチェーンの契約](../../android/architecture/build-toolchain.md)
- Native artifact の生成、binding 固有の既知の制約、SDK 更新時の再検証箇所は [MAUI binding の Native artifact 統合](../architecture/binding-build-integration.md) に集約する
- `maui/tests/` の検証用ホストアプリ (iOS / Android) は、C# から Native 表示までの end-to-end 疎通を確認する回帰テスト資産として維持する (使い捨てにしない)

## 関連

- [MAUI facade (KsSettingsView.Maui) の公開契約](maui-facade.md) — この interop 境界の上に載るアプリ向け公開面
- [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md)
- [iOS Native Host の利用と更新境界](../../ios/api/ios-native-host.md)
- [Android Native Host](../../android/api/android-native-host.md)
- [MauiView の native 実体化機構](../architecture/view-materialization.md) — 輸送する platform view を作る側の機構と、native への埋め込みの継ぎ目
- [MAUI binding の Native artifact 統合](../architecture/binding-build-integration.md) — binding の生成経路・既知の制約・SDK 更新時の再検証箇所
- [MAUI 検証ホストの実行規約](../conventions/integration-host-verification.md) — binding / facade の end-to-end 疎通手順
- 決定の経緯: [maui ドメインの ADR 一覧](../../../decisions/maui/index.md) (基盤は maui/ADR-0001〜0007、輸送と操作通知は maui/ADR-0011〜0012・0015、view の輸送は maui/ADR-0017・0020、style と Section 装飾の輸送は maui/ADR-0023・0024)。Host の view load / attach 時の復元契約は core/ADR-0019

# Probe 結果: add-maui-basic-input-cells (tasks 1.1〜1.5)

後続の Bridge / facade 実装が前提にしてよい interop の事実をまとめる。全項目を実ビルドと実行時
往復で確認しており、机上判断は含まない。**設計前提が崩れた項目はない。**

- 検証日: 2026-08-10
- 実行環境: .NET SDK 10.0.101 / Microsoft.Android.Sdk 36.1.2 / DEVELOPER_DIR=Xcode-26.1.1
- 実行対象: iOS Simulator (iPhone 17 Pro / iOS 26) / Android 実機 (`0B261JEC216142`)
- 最小例: [artifacts/probe/](artifacts/probe/) の 5 ファイル (再現手順は末尾)

## 結論一覧

| # | 検証項目 | 可否 | 採用方式 |
|---|---|---|---|
| 1.1 | iOS `@objc` protocol delegate の binding | 可 | `[Protocol]` + `[Model]` + `[BaseType(NSObject)]`、selector は Swift 側で `@objc(...)` 明示 |
| 1.2 | Android listener interface の binding | 可 | Kotlin `interface` → 自動生成の C# interface。`eventName` 空 attr で event 化を抑止 |
| 1.3 | platform 画像 (UIImage / Drawable) の interop | 可 | DTO に `UIImage?` / `Drawable?` フィールドを持たせて直接輸送 |
| 1.4 | 共通基底 DTO による異種 Cell 混載 | 可 | iOS `@objc class` 基底 / Android `abstract class` 基底 + 基底型の配列・List |
| 1.5 | nullable scalar の interop 表現 | 可 | **boxed を採用** (`NSNumber?` / `Java.Lang.Integer?`)。センチネルは不要 |

---

## 1.1 iOS `@objc` protocol delegate の binding 生成 — 可

### 採用方式

Swift 側は `@objc` protocol を `NSObjectProtocol` 継承で宣言し、**selector を `@objc(...)` で明示する**。

```swift
@objc(KsBridgeProbeDelegate)
public protocol KsBridgeProbeDelegate: NSObjectProtocol {
    @objc(probeSwitchChanged:isOn:)
    func probeSwitchChanged(cellID: String, isOn: Bool)
}
```

binding 側は `[Protocol]` + `[Model]` + `[BaseType(typeof(NSObject))]` で宣言し、C# は生成された
抽象クラス (`KsBridgeProbeDelegate`) を継承して `override` する。Bridge 側の保持プロパティは
weak export + `[Wrap]` の 2 本立てにする。

```csharp
[NullAllowed]
[Export("delegate", ArgumentSemantic.Weak)]
NSObject WeakDelegate { get; set; }

[Wrap("WeakDelegate")]
[NullAllowed]
KsBridgeProbeDelegate Delegate { get; set; }
```

### 実行時に確認できたこと

- 4 種の引数形 (`String` / `String+Bool` / `String+[Int]` / `String+String`) がすべて C# 実装へ到達した
  → `tapped(cell-1) / switch(cell-2,True) / indices(cell-3,[1,2,3]) / time(cell-4,09:30)`
- `Delegate = null` で解除でき、以後の発火は 1 件も届かない (`hasDelegate=false`)
- 通知は Swift 側の呼び出しスレッド (main) で同期に届く。**marshal は不要** (design Decision 2 の前提どおり)

### 制約 / 実装時の注意

- **selector の明示は必須。** Swift の既定生成は `func probeTapped(cellID:)` → `probeTappedWithCellID:`
  のように第1引数ラベルを `With...` へ畳み込む (既存 `replaceSectionWithSectionID:newSection:` が実例)。
  `@objc(...)` で固定し、ApiDefinition の `[Export]` と 1 文字違わず合わせる。
- `[Abstract]` を付けた Model メソッドは C# 側で `override` しないと実行時に例外になる。
  delegate のメソッドは Cell 種ごとに増えるため、**全メソッドに `[Abstract]` を付けるかは要判断** —
  付けない (= 任意実装) 方が Cell 種追加時に facade 側の破壊がない。probe では全件 `[Abstract]` で通した。
- Swift の `[Int]` は `NSNumber[]` として binding される。C# 側は `Int32Value` で読む
  (`SelectedIndices` の wire 表現がこれに該当)。
- **weak 保持の帰結**: probe では C# 側のローカル参照を落として GC を回しても `hasDelegate=true` の
  ままで、weak による消失は再現しなかった (managed peer が回収されるまで ObjC 側実体が生きるため)。
  つまり probe は「消えない」ことを保証していない。design Decision 2 のとおり
  **gateway が delegate 実装を strong 保持する**方針で進めること。

---

## 1.2 Android listener interface の binding 生成 — 可

### 採用方式

Kotlin の素の `interface` を宣言するだけでよい。binding generator が C# interface
(`IKsBridgeProbeListener`) と Invoker を自動生成する。C# 実装は `Java.Lang.Object` を継承して
interface を実装する (ACW は消費側アプリのビルドが生成する)。

```csharp
private sealed class ProbeListener : Java.Lang.Object, IKsBridgeProbeListener
{
    public void ProbeSwitchChanged(string cellId, bool isOn) { /* ... */ }
}
```

保持プロパティは Kotlin の `var listener: KsBridgeProbeListener?` がそのまま
`IKsBridgeProbeListener? Listener { get; set; }` になり、`null` 設定で解除できる。

### 実行時に確認できたこと

- 4 種の引数形がすべて C# 実装へ到達 (iOS と同一の出力)
- `Listener = null` で解除でき、以後の発火は届かない (`count=0`)
- Kotlin の `IntArray` は C# の `int[]` として往復する

### 制約 / 実装時の注意

- **`setListener` が event 化されようとして `BG8501` 警告が出る。** Metadata.xml に空の `eventName`
  attr を置くと抑止でき、`Listener` プロパティはそのまま残ることを確認済み。

  ```xml
  <attr path="/api/package[@name='jp.kamusoft.kssettingsview.bridge']/class[@name='KsBridgeProbe']/method[@name='setListener' and count(parameter)=1 and parameter[1][@type='jp.kamusoft.kssettingsview.bridge.KsBridgeProbeListener']]"
      name="eventName"></attr>
  ```

  (`KsBridgeProbe` / `KsBridgeProbeListener` の箇所を実装名に置き換えて使う)
- **命名の非対称**: Kotlin の `isOn` は C# で `On` になる (`is` 接頭辞が落ちる)。iOS は `IsOn` のまま。
  既存の `IsEnabled` / `Enabled` と同種の非対称で、facade 側で吸収する (maui/README の既知の制約と同じ)。

---

## 1.3 platform 画像 (UIImage / Drawable) の interop — 可 (両OS)

### 採用方式

DTO に platform 画像の nullable フィールドを持たせて直接輸送する。

| | Swift / Kotlin | binding 後の C# |
|---|---|---|
| iOS | `@objc public var icon: UIImage?` | `[NullAllowed][Export("icon", ArgumentSemantic.Strong)] UIImage Icon { get; set; }` |
| Android | `var icon: Drawable?` | `Android.Graphics.Drawables.Drawable? Icon { get; set; }` |

### 実行時に確認できたこと

- iOS: C# で `UIGraphicsImageRenderer` から作った 24x24 の `UIImage` を渡し、Swift 側で
  `UIImage(24x24)` として観測できた
- Android: C# で作った `ColorDrawable` を渡し、Kotlin 側で `ColorDrawable` として観測できた
- 双方とも `null` を渡した場合は native 側で `nil` / `null` として観測できた

### 制約 / 実装時の注意

- `KsImage.uiImage(_:)` / `KsImage.Drawable(...)` への詰め替えは **Bridge 内 (Swift / Kotlin) の処理**で
  interop 境界に露出しない。design Decision 7 の「native 変更なし」は成立する
  (Swift の enum associated value は `@objc` 表現を持たないため、境界を越えるのは `UIImage?` /
  `Drawable?` の素の値であり、KsImage への包み込みは Bridge 側が行う)。
- **DTO の構築コードは platform 別アセンブリにしか書けない** (`UIImage` / `Drawable` は TFM 固有)。
  net10.0 の facade は `ImageSource` のまま保持し、解決済み platform 画像は gateway seam
  (maui/ADR-0009) の platform 実装側で DTO へ載せる。facade 層に platform 画像型を漏らさないこと。

---

## 1.4 共通基底 DTO による異種 Cell の混載 — 可 (両OS)

### 採用方式

| | 基底 | 派生 | コレクション |
|---|---|---|---|
| iOS | `@objc(KsBridgeCell) public class KsBridgeCell: NSObject` | `public final class ...: KsBridgeCell` | `@objc public var cells: [KsBridgeCell]` → `[Export("cells", ArgumentSemantic.Copy)] KsBridgeCell[] Cells { get; set; }` |
| Android | `abstract class KsBridgeCell` | `class ...: KsBridgeCell()` | `List<KsBridgeCell>` → `IList<KsBridgeCell> Cells { get; set; }` |

binding 側の派生は `[BaseType(typeof(KsBridgeCell))]` で宣言する (iOS)。Android は自動生成で
`public abstract partial class KsBridgeCell : Java.Lang.Object` + 派生クラスが出る。

### 実行時に確認できたこと

- C# から `Label` と `Switch` の 2 種を基底型のコレクションに混ぜて渡し、native 側の
  `as?` (Swift) / `is` (Kotlin) で正しく判別できた
- `addCell(基底型)` の 1 件ずつの受け渡しでも同じ結果
- **読み戻し** (native → C#) でも CLR 側の派生型が保たれる
  (`clrType=KsBridgeProbeLabelCell` / `clrType=KsBridgeProbeSwitchCell`)
- 基底に載せた共通フィールド (title / icon / nullable scalar) は派生越しに正しく読める

### 制約 / 実装時の注意 — コンストラクタの扱いが両OSで非対称

- **iOS: コンストラクタは継承されない。** 派生の ApiDefinition に
  `[Export("initWith...")] NativeHandle Constructor(...)` を**再宣言する必要がある**
  (基底の init をそのまま使う派生でも再宣言が要る)。`[DisableDefaultCtor]` は基底・派生の双方に付ける。
  probe では基底 `initWithTitle:` と派生固有 `initWithTitle:isOn:` の両方が C# から呼べることを確認済み。
  → **11 種 × 派生ごとに ctor 再宣言が要るので、ボイラープレート量の見積りに含めること。**
- **Android: 再宣言は不要。** Java のコンストラクタから各クラスに自動生成される。
- 基底に `let cellID` (iOS) / `val cellID` (Android) を引き上げると、C# 側では基底の読み取り専用
  プロパティとして 1 か所で見える。ID 採番の基底集約 (design Decision 1) は素直に成立する。

---

## 1.5 nullable scalar の interop 表現 — **boxed を採用** (センチネルは不採用)

### 採用方式

| | 宣言 | binding 後の C# | 生成 |
|---|---|---|---|
| iOS | `@objc public var uiStyle: NSNumber?` | `[NullAllowed][Export("uiStyle", ArgumentSemantic.Strong)] NSNumber UiStyle` | `NSNumber.FromInt32(1)` |
| Android | `var uiStyle: Int?` | `Java.Lang.Integer? UiStyle` | `Java.Lang.Integer.ValueOf(1)` |

`Double?` も同様 (`NSNumber` / `Java.Lang.Double?`)。

### 実行時に確認できたこと

- 値あり (`uiStyle=1`) と null (`uiStyle=nil` / `uiStyle=null`) の双方が native 側で正しく観測できた
- `iconSize` (`Double?` 相当) も同様に往復した

### 判断根拠 (センチネルを採らない理由)

- **既存の実装済み precedent がある。** `KsBridgeTheme` (phase-1 実装済み) が両OSでこの方式を使っており、
  検証ホストで動作実績がある。probe はそれが Cell DTO でも同じく成立することを確かめただけ。
- センチネル (負値) は「有効な負値」と「未指定」を将来区別できなくなるうえ、両OSで同じ約束を
  重ねて守る必要があり、boxed に対する利点がない。

### 制約 / 実装時の注意

- **Kotlin の enum をそのまま輸送しない。** Kotlin `enum class` は C# 側で `Java.Lang.Enum` 派生の
  クラス (静的プロパティ) として binding され、iOS の C# enum と形が揃わない
  (既存 `KsBridgeAccessoryTarget` がその実例 — maui/README の既知の制約に記載済み)。
  `DatePickerUIStyle` / `Keyboard` は design Decision 5 / 6 のとおり **Int 序数 + boxed null** で輸送する。
- `new Java.Lang.Integer(1)` / `new Java.Lang.Double(1.0)` は API 33 で deprecated (`CA1422` が出る)。
  `Java.Lang.Integer.ValueOf(...)` / `Java.Lang.Double.ValueOf(...)` を使う。

---

## 付随所見 (probe 中に判明した toolchain の事実)

- **iOS の xcframework 増分ビルドは Swift ファイルの「削除」を検知しない。** `_XcbInputs` は
  timestamp 比較のため、Bridge から Swift を削除しても xcframework には旧シンボルが残る。
  削除を伴う変更をしたら `maui/macios/KsSettingsView.Binding.iOS/obj` を消してから再ビルドする
  (maui/README の「作り直すときは obj を消す」と同じ操作が、削除時は**必須**になる)。
- Android の Gradle ビルドは削除を正しく検知して aar から落とす。
- probe の追加・撤去を通じて、両 binding のビルドは 0 エラーで安定していた。撤去後に
  iOS / Android の binding を再ビルドし、生成物 (xcframework / aar / 生成 C#) に probe が
  残っていないことを確認済み。MAUI ユニットテストは 115 件全緑。

## 設計への影響

- design.md Decision 2 (delegate/listener) / Decision 5 (値の輸送) / Decision 7 (IconSource) /
  Risks の「コールバック方向の interop」「Drawable / UIImage の interop 輸送」は**すべて前提どおり成立**。
  設計変更を要する事実は見つからなかった。
- Decision 5 に残っていた未確定点「センチネル or boxed を probe で確定する」は **boxed で確定**。
- ボイラープレート量の見積りには、iOS 側の派生 DTO ごとのコンストラクタ再宣言 (1.4 の制約) を
  加味すること。

## 最小例と再現手順

最小例は [artifacts/probe/](artifacts/probe/) に置いてある (ビルドツリーからは撤去済み)。
再現するときは次の位置へ戻して各プロジェクトをビルドする。

| ファイル | 戻す位置 |
|---|---|
| `KsBridgeProbe.swift` | `ios/Sources/KsSettingsViewBridge/` |
| `ApiDefinition.probe.cs` | 中身を `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs` の末尾へ追記 |
| `KsBridgeProbeRunner.iOS.cs` | `maui/tests/KsSettingsView.IntegrationHost.iOS/KsBridgeProbeRunner.cs` として置き、`AppDelegate.FinishedLaunching` の先頭で `KsBridgeProbeRunner.Run();` を呼ぶ |
| `KsBridgeProbe.kt` | `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/` |
| `KsBridgeProbeRunner.Android.cs` | `maui/tests/KsSettingsView.IntegrationHost.Android/` に置き、`MainActivity.OnCreate` の先頭で `KsBridgeProbeRunner.Run(this);` を呼ぶ |

```bash
# iOS
export DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer
dotnet build maui/tests/KsSettingsView.IntegrationHost.iOS/KsSettingsView.IntegrationHost.iOS.csproj -c Debug
xcrun simctl install <udid> maui/tests/KsSettingsView.IntegrationHost.iOS/bin/Debug/net10.0-ios/iossimulator-arm64/KsSettingsView.IntegrationHost.iOS.app
xcrun simctl launch --console-pty <udid> jp.kamusoft.kssettingsview.integrationhost.ios   # 出力に "KsBridgeProbe:" 行

# Android
dotnet build maui/tests/KsSettingsView.IntegrationHost.Android/KsSettingsView.IntegrationHost.Android.csproj \
  -c Debug -t:Run -p:AdbTarget="-s <serial>"
adb -s <serial> logcat -d -s KsBridgeProbe:I
```

生成された C# の確認は次の位置を読む。

- Android: `maui/android/KsSettingsView.Binding.Android/obj/Debug/net10.0-android/generated/src/*.cs`
- iOS: xcframework のシンボル確認は
  `nm maui/macios/KsSettingsView.Binding.iOS/obj/Debug/net10.0-ios/xcode/*/xcframeworks/*/ios-arm64_x86_64-simulator/KsSettingsViewBridge.framework/KsSettingsViewBridge`

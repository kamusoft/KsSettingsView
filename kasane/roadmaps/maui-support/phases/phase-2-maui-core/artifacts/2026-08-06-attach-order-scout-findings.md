# 調査メモ: Host 取り付け順序 (論点11) の裏取り

2026-08-06、ksn-scout 2並列 (①KsSettingsView Native 両 OS / ②MAUI ソース) の要約。出典コードパス付き。

## ① Native Host の attach 前 Diff の挙動

### iOS (`KsSettingsViewController`)

- Store 購読は `init` 時点で開始 (`connectStore`)。`applyDiff` は `dataSource == nil` (= viewDidLoad 前) の間 `updateInternalRoot(for:)` へ迂回し、**`.full` 以外の Diff は内部 `root` にすら反映せず破棄**する (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:987-1033`、コメントで意図明言)
- `viewDidLoad` は Store を再 pull せず `self.root` (init 時キャプチャ + `.full` のみ上書き) から `applyFullSnapshot` する (`351-358`)。→ 実測の取りこぼしはこの「viewDidLoad で Store 現在状態を見ない」実装ギャップが原因
- 修正するなら `viewDidLoad` で `connectedStore?.root / theme` を pull し直すだけ (`connectedStore` は既存の weak プロパティ)。変更規模 S〜小M

### Android (`KsSettingsView`)

- `bind(store)` 時に attach 前だと `findViewTreeLifecycleOwner()` が null で **collect ジョブ自体を開始しない** (`KsSettingsView.kt:344-353`)。`diffs` は replay なし SharedFlow なので購読者不在の Diff は消える
- ただし `onAttachedToWindow` で `resyncFromStore(store)` が **`store.state.value` (現在状態) を pull して全復元**し、その後 collect を開始する (`239-267`, `318-335`)。→ **イベントは失うが状態は必ず復元される設計。Android に取りこぼしは無い**
- attach/detach と購読開始/cancel が明示的に連動している (iOS に対応物なし)

### Store の復元保証の実態

- 「購読開始時点の Root と Theme を Store の現在値から復元できる」(store-and-update-streams.md) は **pull 型の保証** — Store が現在値プロパティを維持することの保証で、push 再通知ではない
- Android は bind と onAttachedToWindow の2箇所で pull、iOS は init の1回のみ。ここが両 OS の頑健さの分岐点。iOS の取りこぼしは Store 契約違反ではなく Host 実装が保証を使っていないだけ

## ② MAUI Handler の適用順序

- **マッパー適用は必ず親 view への追加より前に完結する**: 子 Handler の `SetVirtualView` 内で `CreatePlatformElement` → `ConnectHandler` → `_mapper.UpdateProperties` が同期完了してから、親が `AddView`/`AddSubview` する (`ElementHandler.cs:41-99`、`LayoutHandler.Android.cs:39-42`、`LayoutHandler.iOS.cs:37-40`)。追加後に全マッパーが再適用されることはない (以後は個別 `UpdateValue` のみ)
- iOS 標準は `LoadViewIfNeeded` を使わない。`IsLoaded()` (= `Window != null`) + `OnLoaded(...)` で **MovedToWindow まで遅延**するのが標準パターン (`ViewExtensions.cs:698-`, `IUIViewLifeCycleEvents.cs`)
- UIViewController 内包の前例 = `PageHandler` + `ContainerViewController`: `.View` ゲッターに触れず (viewDidLoad を早期発火させず) platform view を先に作ってキャッシュする設計 (`PageHandler.iOS.cs:7-22`, `ContainerViewController.cs:15-81`)
- `VisualElement.Loaded` は window attach 時点で発火し、attach 後まで native 操作を遅延する枠組みとして信頼できる (`VisualElement.Platform.cs:19-93`)。ネイティブホスト直接埋め込みでも同機構が効く

## 論点11 への含意

- MAUI Handler は構造上「makeHost → Bridge 操作 (マッパー) → 取り付け」の順で動くことが確定 → 隙間は現実に踏む
- Android は既に安全。iOS だけが `viewDidLoad` で pull しない実装ギャップを持つ
- 案B (Host 側で塞ぐ) は「iOS を Android の resyncFromStore パターンに対称化する」小さな変更で実現できる

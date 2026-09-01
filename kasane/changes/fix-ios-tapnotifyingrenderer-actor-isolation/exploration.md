# Exploration: fix-ios-tapnotifyingrenderer-actor-isolation

起票日: 2026-08-28 / 起票元: restore-pickercell-object-items の実装中 (iOS core モデル実装ワーカーが既存警告として発見、オーナー指示で起票)
探索日: 2026-09-01

## 課題 / 動機

iOS のビルドで Swift concurrency の警告が出ている: 行タップ通知プロトコル (`TapNotifyingRenderer`) への `PickerCellView` の準拠が main actor 分離コードをまたぐ、という内容 (`conformance of 'PickerCellView' to protocol 'TapNotifyingRenderer' crosses into main actor-isolated code`)。

- 該当: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` のプロトコル定義 (2450 行付近) と準拠 extension 群 (2454 行以降の 11 CellView: Command / Button / Checkbox / Radio / SimpleCheck / Picker / NumberPicker / TimePicker / DatePicker / Entry / Custom)
- 警告文が明示するとおり、**Swift 6 言語モードではエラーになる**。現在は警告のまま動作しており、restore-pickercell-object-items 以前から存在する
- toolchain / 言語モード更新時に必須対応となるため、先に潰しておきたい

## 探索の裏取り (Swift 6 言語モードでのビルド試行)

`ios/Package.swift` に `swiftLanguageVersions: [.version("6")]` を一時追加し (toolchain は Swift 6.3.2 / Xcode 26.5)、`xcodebuild build -scheme KsSettingsView-Package -destination 'generic/platform=iOS Simulator'` でエラーを段階的に洗い出した (各段階で仮修正を当てて次の層を露出させ、調査後にすべて revert 済み)。source 4 ターゲット (Core / UI / SwiftUI / Bridge) が対象。テストターゲットは build アクションの対象外のため未確認。

Swift 6 モードでエラーになるのは次の 3 群で全部 (3 群を仮修正すると BUILD SUCCEEDED):

1. **`TapNotifyingRenderer` 準拠 11 件** (本命)。プロトコル定義への `@MainActor` 付与 1 行で全件解消することを仮当てで確認済み
2. **`KsCellViewSupport.installSelectedColorHandler` の closure 1 件**: `configurationUpdateHandler` (SDK 型は nonisolated) の closure 内から main actor 分離の `defaultBackgroundConfiguration()` を呼び、非 Sendable の `UIBackgroundConfiguration` を受け取っている。closure への `@MainActor` 付与は SDK 型とのミスマッチで不可 (試行済み)。`MainActor.assumeIsolated` で包み、`cellState` は Sendable な Bool (`isPressed`) に落としてから持ち込む形でコンパイル通過を確認済み (handler は UIKit が main thread で呼ぶ契約なので assumeIsolated は安全)
3. **`KsSettingsViewController.deinit` の 8 件**: nonisolated deinit から main actor 分離の非 Sendable プロパティ (`storeSubscription` 等の AnyCancellable 4 本、および続く dataSource / delegate / index 解放) に触れている。ソースコメントの「プロパティへの直接アクセスは deinit の特例で許される」という前提が Swift 6 モードでは通用しない。解放順序に意味があるとされる設計 (Cycle 断ち) のため、機械的な置換では閉じない設計課題

付随の観測 (Swift 6 モードではエラーにならない):
- Swift 5 モードで出る `sectionTextGap` (2260/2349 行) と `dataSource`/`delegate` 変異 (237/238 行) の警告は、Swift 6 モードの診断では消える (region isolation で安全と証明される側)
- concurrency 以外の警告 2 件: `KsCellViewSupport.swift:80` の無意味な条件 downcast、`KsSettingsViewController.swift:617` の deprecated `supplementariesFollowContentInsets` (iOS 16.0)

## 検討した選択肢 (却下案と理由を含む)

警告の解消手段 (論点1):

- **A (採用): プロトコル定義そのものを `@MainActor` 分離にする** — internal プロトコルで呼び手は main actor 上の didSelectItemAt 1 箇所のみ。定義 1 行で 11 準拠が全件解消。UI 専用の契約であることを定義側で明示できる
- B (却下): 準拠側 11 箇所を isolated conformance (`extension X: @MainActor TapNotifyingRenderer`) にする — 描画契約 (`KsCellRenderer`) と方式が揃うが、あちらは public プロトコルで利用者側の準拠自由度を残す意味があるのに対し、こちらは internal で非 UI 利用の想定がなく、11 箇所の変更は過剰

## 決定事項

- 警告の解消手段は A (プロトコル定義への `@MainActor` 付与)。internal 1 行で覆すコストが小さいため ADR にはしない (2026-09-01)
- スコープは Swift 6 モード試行で見つかった 3 群すべて (本命の準拠 11 件 + closure 1 件 + deinit 8 件) を本 change で潰す。「隣接課題は同じ change で直す」方針による (2026-09-01)

## ADR 候補

(なし)

## 未決の論点

- (3) deinit の解消方針: 明示 cancel/nil 代入が本当に必要か (AnyCancellable は解放時に自動 cancel、`UICollectionView.dataSource`/`delegate` は weak) の再検証を含む設計判断が要る。`isolated deinit` (SE-0371) は runtime 要件が deployment target iOS 16 を超える可能性があり要確認 (→ 提案フェーズで詰める)
- Swift 6 言語モードへの切替 (Package.swift) 自体をいつやるかは本 change の範囲外 (toolchain 更新のタイミング)
- テストターゲットの Swift 6 モード適合は未確認 (build アクションの対象外だったため)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級: M (確定)

3 群すべてを本 change で潰す。(1)(2) は機械的 (実証済みの書き換え) だが、(3) が deinit の解放設計に踏み込みリーク検証を要するため M。公開 API 変更なし。

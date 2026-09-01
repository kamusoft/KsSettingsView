# 分類台帳: 固定時間待機の全数仕分け

固定時間待機の実装パターン (旧 `pump` 系ヘルパ・`pumpEntry`・テスト本文の直接の `RunLoop.current.run(until:)`) の全呼び出しを A/B/C に仕分けた台帳。実装・レビュー・検証はこの台帳を正とする (出典: scout 全数調査 2026-09-01 + second-opinion spec-001 指摘によるスコープ拡張)。行番号は 2026-09-01 時点。

- **A: 収束待ち** → 条件ベース待機へ置換。述語は「操作前には成立せず、非同期反映後に初めて成立する遷移証拠」で書く。deadline は全箇所共通の既定値 (個別上書きは根拠がある場合のみ)
- **B: レイアウト駆動のみ** → 待機なしのレイアウト実行ヘルパへ置換
- **C: 負の検証** → 意図明示の固定待機へ置換 (cross/ADR-0027)

集計: 呼び出し 206 (A 160 / B 16 / C 30)、定義 20 (旧 `pump` 19 + `pumpEntry` 1) — 全定義を撤去する。

| ターゲット | 呼び出し | A | B | C |
|---|---:|---:|---:|---:|
| KsSettingsViewBridgeTests | 64 | 50 | 2 | 12 |
| KsSettingsViewSwiftUITests | 12 | 11 | 1 | 0 |
| KsSettingsViewUITests | 130 | 99 | 13 | 18 |

## A: 収束待ち (160)

### KsSettingsViewBridgeTests (50)

| 箇所 | 待つ遷移 (述語候補) |
|---|---|
| `KsBridgeAccessoryViewTests.swift:51,70,84,101,109,208,256,290,313,339` | Section / Root accessory の attach・clear・再構成後、実物の supplementary view、同一 `UIView`、または初期 frame が確定する |
| `KsBridgeAccessoryViewTests.swift:137` | 既知 Cell の置換後、実描画タイトルが `"A2"` になる |
| `KsBridgeAccessoryViewTests.swift:265,266` | `contentOffset` 移動後、先頭 accessory が画面外へ出て再利用される |
| `KsBridgeAccessoryViewTests.swift:271` | 先頭へ戻した後、同じ accessory view が再表示される |
| `KsBridgeCustomCellTests.swift:206,256,291,313,441` | CustomCell の embedded view、content、enabled 状態、listener が再バインドされる |
| `KsBridgeCustomCellTests.swift:356,357` | リサイクル後、先頭 Cell が画面外になり旧 view が別行へ残らない (CI flaky の実例) |
| `KsBridgeCustomCellTests.swift:371` | スクロール復帰後、embedded view が再表示される |
| `KsBridgeHostTests.swift:26` | `setRoot` 後の Cell が実描画される |
| `KsBridgeRootTests.swift:36,51` | Root / Cell 更新後、実描画タイトル・header が更新される |
| `KsBridgeSectionVisibilityTests.swift:77,136` | Section / Header の可視性変更が実描画へ反映される |
| `KsBridgeStyleTests.swift:43,59,62,86,102,105` | style 変更に伴う layout・snapshot・Cell 再構成後、行位置が更新される (style setter は `applyFullSnapshot` / `reconfigureVisibleCells` を通る) |
| `KsBridgeTestHost.swift:45` | Host attach 後、初期 Cell / supplementary が実描画される (setup 内待機 — 初期反映の完了述語を待つ形にする) |
| `KsBridgeThemeTests.swift:95` | Theme 変更後、表示中 Cell の色が更新される |
| `KsBridgeThemeTests.swift:197` | style 再適用後、行が生成され装飾値が正規化される |
| `KsBridgeThemeTests.swift:207` | Theme 変更後、表示中 Cell と layout が更新される |
| `KsBridgeUpdateTests.swift:25` | Cell 挿入・削除後、行タイトルが更新される |
| `KsBridgeUpdateTests.swift:40,44,48` | Section 挿入・移動・削除後、行と header が更新される |
| `KsBridgeUpdateTests.swift:60` | Section 置換後、実描画タイトルが更新される |
| `KsBridgeUpdateTests.swift:99,110,170` | Cell 挿入・置換後、タイトル・identity が更新される |
| `KsBridgeUpdateTests.swift:198` | 複数 Cell のバッチ内容更新が実描画へ反映される |
| `KsBridgeUpdateTests.swift:241` | Cell 移動後、表示順が更新される |
| `KsBridgeUpdateTests.swift:261,271` | Section accessory の text 更新・clear が表示へ反映される |
| `KsBridgeUpdateTests.swift:303` | no-op 後の後続 Cell 更新が表示へ届く |

### KsSettingsViewSwiftUITests (11)

| 箇所 | 待つ遷移 (述語候補) |
|---|---|
| `DSLAccessoryVisibilityTests.swift:305,316` | DSL 再評価による Header toggle の false / true が layout へ反映される |
| `DSLAccessoryVisibilityTests.swift:341,358,387,403` | DSL 経路と Store 経路の Header / Footer 表示結果が一致する |
| `DSLTimePickerHourCycleTests.swift:79` | 初期 SwiftUI-hosted picker が実 Cell として生成される |
| `DSLTimePickerHourCycleTests.swift:131,157` | DSL 再評価後、picker の `is24Hour` が更新される |
| `DSLTimePickerHourCycleTests.swift:187,209` | DSL 経路と Store 経路の picker 状態が一致する |

### KsSettingsViewUITests (99)

| 箇所 | 待つ遷移 (述語候補) |
|---|---|
| `AccessoryMeasureInvalidationTests.swift:55` | 初期 Section accessory supplementary が生成され、実 frame が取得できる |
| `AccessoryMeasureInvalidationTests.swift:217` | Store 経由の Root accessory 追加後、boundary supplementary が生成される |
| `AccessoryViewDetachDiagnosticTests.swift:40,78,116,121,123,128,150,172,177,200,205,236,240` | accessory view の attach・replace・detach・スクロール再利用が完了する |
| `AccessoryViewLiveProbeTests.swift:55` | 初期 Section supplementary が生成され、実 frame が取得できる |
| `AccessoryViewLiveProbeTests.swift:284` | Root accessory 追加後、boundary supplementary が生成される |
| `CellIconFrameTests.swift:111` | Theme 再適用後、表示中 Cell の icon frame が更新される |
| `CellIconFrameTests.swift:313` | 初期 Cell が生成され、icon frame が取得できる |
| `ContentUpdateBatchTests.swift:30,75` | 初期 Cell の生成、および `replaceCells` 後のタイトル・Cell identity が確定する |
| `CustomCellTests.swift:81` | standalone SwiftUI-hosted content が view tree に現れる |
| `CustomCellTests.swift:113` | CustomCell の実描画が生成される |
| `CustomCellTests.swift:365` | CustomCell の content 差し替えが view tree に反映される |
| `CustomCellTests.swift:717,731` | SwiftUI 内部 state による Cell 高さの伸縮が完了する |
| `CustomCellTests.swift:842,875` | UIHostingController の content が描画される |
| `CustomCellTests.swift:922` | (直接 `RunLoop.current.run`) セル生成後の高さが取得できる |
| `FullSnapshotContentRefreshTests.swift:36` | 初期 Cell が実描画される |
| `FullSnapshotContentRefreshTests.swift:98,131,165,195,222,252,284,313` | full / replace 更新後、Cell・header・visibility・具象型が表示へ反映される (131 は identity ではなくタイトル反映を述語にする — 早期 return 注意の実例) |
| `HostViewLoadRestoreTests.swift:36` | view load 後、Store 現在状態の Cell / supplementary が実描画される |
| `HostViewLoadRestoreTests.swift:287` | view load 後に再適用した Root accessory が表示される |
| `InputCellsTests.swift:334` | (`pumpEntry`) Theme 変更後、表示中 EntryCell の placeholder 色が更新される |
| `InputCellsTests.swift:413` | (`pumpEntry`、setup 内待機) 初期 EntryCell が実描画される |
| `InputCellsTests.swift:1071` | (直接 `RunLoop.current.run`) tapHandler の async Task 経由で `becomeFirstResponder()` が反映される (`isFirstResponder == true`) |
| `ReplaceCellTypeChangeTests.swift:37` | 初期 Cell が生成される |
| `ReplaceCellTypeChangeTests.swift:82,112,142,175` | Cell 置換後、具象型・タイトル・identity が反映される |
| `RootAccessoryThemeRefreshTests.swift:58,265` | 初期 Root accessory が生成される |
| `RootAccessoryThemeRefreshTests.swift:97,125,151,176,273` | Root text の色・font・Store 経由 Theme が表示へ反映される |
| `SectionAccessoryRenderingTests.swift:393,417` | 初期 Section supplementary が実描画される |
| `SectionAccessoryRenderingTests.swift:484,509,534,559,625,667,714,759,796` | accessory / full Diff / headerHeight / Cell 更新後の実物 view・frame・identity が確定する |
| `SectionAccessoryThemeRefreshTests.swift:53` | 初期 Section accessory が生成される |
| `SectionAccessoryThemeRefreshTests.swift:101,135,166,194,225,249,358,391,399` | Section header / footer の text・font・Theme 更新が表示へ反映される |
| `SectionAccessoryVisibilityTests.swift:191,206,233,242,278,486` | Header / Footer toggle、再表示、Cell 挿入後の area 状態が確定する |
| `SectionBoxDecorationTests.swift:338` | Theme 変更後、decoration の corner radius と identity が更新される |
| `SectionBoxDecorationTests.swift:488,504,525` | Section 構造変更後、content inset・Root 間隔が更新される |
| `SectionBoxDecorationTests.swift:599,608` | Section 数・margin 変更後、Root factory の再生成が完了する |
| `SectionBoxDecorationTests.swift:678` | Theme の section margin が Root / box frame へ反映される |
| `SectionBoxDecorationTests.swift:747` | Cell 挿入後、Section box frame が追従する |
| `SectionBoxDecorationTests.swift:827,843` | Cell 挿入・削除後、末尾 Cell の角丸 clip が更新される |
| `SectionBoxDecorationTests.swift:998` | highlight state 後、背景色と clip mask が反映される |
| `SectionBoxDecorationTests.swift:1248,1263` | style 切替後、decoration・separator・Cell mask が更新される |
| `StoreDisconnectionTests.swift:30` | 初期 Cell が実描画される |
| `TimePickerHourCycleStoreUpdateTests.swift:40` | 初期 picker が生成される |
| `TimePickerHourCycleStoreUpdateTests.swift:91,117` | Store 経由の `is24Hour` 変更が picker へ反映される |

## B: レイアウト駆動のみ (16)

| 箇所 | 直後の assert が見ている同期的状態 |
|---|---|
| `KsBridgeAccessoryViewTests.swift:300` | Section Header の再計測後の `frame.height == 100` |
| `KsBridgeAccessoryViewTests.swift:322` | Root Header の再計測後の `frame.height == 120 + 22` |
| `DSLAccessoryVisibilityTests.swift:46` | 初期 Header / Footer の layout attributes の存在・不在 |
| `AccessoryMeasureInvalidationTests.swift:103` | Section Header の再計測後の高さ 140pt |
| `AccessoryMeasureInvalidationTests.swift:128` | Section Footer の再計測後の高さ 120pt |
| `AccessoryMeasureInvalidationTests.swift:154` | 固定高さ Header が 80pt のまま変化しないこと |
| `AccessoryMeasureInvalidationTests.swift:233` | Root Header の再計測後の高さ 130 + 22pt |
| `AccessoryViewLiveProbeTests.swift:172` | Section Header の明示的 invalidation 後の高さ 140pt |
| `AccessoryViewLiveProbeTests.swift:194` | 対象 Header の明示的 invalidation 後の高さ 140pt |
| `AccessoryViewLiveProbeTests.swift:222` | Section Footer の明示的 invalidation 後の高さ 120pt |
| `AccessoryViewLiveProbeTests.swift:255` | Header 高さ変更後の後続 Section の `origin.y` |
| `AccessoryViewLiveProbeTests.swift:306` | Root Header の明示的 invalidation 後の高さ 130 + 22pt |
| `SectionAccessoryRenderingTests.swift:958` | layout 再計算後も固定 Header 40pt / 自動 Header 70pt を維持 |
| `SectionAccessoryVisibilityTests.swift:36` | 初期 Header / Footer area の layout attributes の存在・不在 |
| `SectionBoxDecorationTests.swift:44` | 初期 Section box decoration・inset の layout attributes |
| `SectionBoxDecorationTests.swift:98` | Root accessory 付き初期 layout の box / inset attributes |

## C: 負の検証 (30)

いずれも条件ベース化の対象外 (cross/ADR-0027)。意図明示の固定待機へ置換する。

| 箇所 | 負の検証の内容 |
|---|---|
| `KsBridgeAccessoryViewTests.swift:125,156` | 未知 section ID の no-op (更新イベント自体が発生しない) |
| `KsBridgeAccessoryViewTests.swift:172` | dispose 済み Bridge の no-op (遅延した誤更新が来ないこと) |
| `KsBridgeAccessoryViewTests.swift:347` | 未知 measurement target の no-op (frame 不変) |
| `KsBridgeHostReleaseTests.swift:80` | Host 解放後の Store 更新が表示へ届かない |
| `KsBridgeLifecycleTests.swift:42` | dispose 後に表示が変わらない |
| `KsBridgeOperationContractTests.swift:457` | 複合: 同一 call site が更新ケースと no-op ケースの両方を通る → 呼び出し側で分岐し A/C を使い分ける |
| `KsBridgeRootTests.swift:78` | 未知 Cell ID の remove no-op |
| `KsBridgeUpdateTests.swift:153,229,290` | 未知 ID の replace / batch / accessory no-op |
| `KsBridgeUpdateTests.swift:319` | Root accessory 更新後に Controller プロパティのみ assert (pump の目的が不明確 → 実装時に待機自体の要否を判断し、不要なら撤去) |
| `AccessoryMeasureInvalidationTests.swift:174,197,247` | 未知 target・購読解除後・未設定 Root footer (何も変わらない) |
| `AccessoryViewLiveProbeTests.swift:159,166,217,299` | invalidation や supplementary 自身の layout だけでは変化しないことの診断プローブ |
| `ContentUpdateBatchTests.swift:108` | 存在しない Cell ID の batch no-op |
| `RootAccessoryThemeRefreshTests.swift:209,235` | Theme 変更で View accessory factory が再実行されない |
| `SectionAccessoryRenderingTests.swift:476` | 未知 section ID の accessory 更新が表示を変えない |
| `SectionAccessoryThemeRefreshTests.swift:292,324` | Theme 変更で View accessory factory が再実行されない |
| `SectionAccessoryVisibilityTests.swift:270` | 非表示 Section の accessory 更新が表示を発生させない |
| `SectionBoxDecorationTests.swift:563,568,573` | 無関係な Diff / Theme で Root accessory factory が再実行されない |
| `StoreDisconnectionTests.swift:73` | 購読解除後の構造・内容・Theme 更新が表示へ届かない |

## 撤去する定義 (20)

- 共有版: `KsBridgeTestHost.swift:50` (`pump`)
- private コピー 18: `SectionAccessoryRenderingTests.swift:422` / `CellIconFrameTests.swift:318` / `HostViewLoadRestoreTests.swift:41` / `CustomCellTests.swift:86` / `AccessoryViewDetachDiagnosticTests.swift:20` / `ReplaceCellTypeChangeTests.swift:42` / `SectionAccessoryThemeRefreshTests.swift:57` / `FullSnapshotContentRefreshTests.swift:41` / `AccessoryViewLiveProbeTests.swift:59` / `StoreDisconnectionTests.swift:35` / `SectionAccessoryVisibilityTests.swift:41` / `ContentUpdateBatchTests.swift:35` / `TimePickerHourCycleStoreUpdateTests.swift:45` / `AccessoryMeasureInvalidationTests.swift:59` / `SectionBoxDecorationTests.swift:48` / `RootAccessoryThemeRefreshTests.swift:62` / `DSLTimePickerHourCycleTests.swift:83` / `DSLAccessoryVisibilityTests.swift:50`
- `InputCellsTests.swift:418` (`pumpEntry`)

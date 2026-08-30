# レビュー結果: add-maui-custom-cell (001 回目)

**日付**: 2026-08-12
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペック 3 能力 (maui-cells / maui-bridge / samples-maui) の Requirement はいずれも実装・テストの対応が取れており、3 プラットフォームのビルドとテストは全件成功、comment-policy lint も 0 件。accessory の実体化機構 (maui/ADR-0016〜0018) を cell content へ拡張する設計はそのまま踏襲され、退役順序・Handler 1:1・多重配置検査の対称化まで一貫している。probe (tasks 1.1) は負の対照付きで実測されており、native 公開 API を触らない判断の根拠として十分。

CHANGES_REQUESTED とするのは 1 点のみ — **Android 側で行タップの操作証跡が撮られていない**こと。行タップは「MAUI が輸送した platform view (MAUI の gesture recognizer を持つ ViewGroup) と native 行の click の消費関係」に依存し、ユニットテストの `performClick()` では通らない経路であるため、iOS と同等の操作スクリーンショット 2 枚が要る (lessons/process.md L-003(4))。残りは Minor 1 件・Suggestion 2 件で、いずれも本変更の可否を左右しない。

## 確認した観点と実測結果

- **ビルド**: `dotnet build KsSettingsView.Maui -f net10.0` / `-f net10.0-ios` / `-f net10.0-android` いずれも 0 警告 0 エラー (platform 別 gateway の変更が net10.0 テストだけでは検証されないため、TFM 別に確認)
- **テスト**: MAUI `dotnet test` = 395 tests / 0 failures、iOS `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` = 476 tests / 0 failures、Android `./gradlew test --rerun-tasks` = 2310 tests / 0 failures (debug + release 合算、`build/test-results/*/TEST-*.xml` 集計)
- **規約**: `python3 scripts/comment-policy-lint.py --summary` = 禁止 0 件 / 596 ファイル。新規ファイルのコメントは ADR 参照とソースファイル名参照のみで、change-id・Decision 番号の裸参照なし
- **足場凍結**: `git diff HEAD` で書き換わっている足場は tasks.md のチェックのみ。specs/ ・design.md ・proposal.md は無改変
- **deviation.md**: 記録済み 3 件 (共有 Style Scenario の読み替え / ReleaseHost 時の空世代再発行 / iOS の入れ物化と引き取り規則) は合意済みとして扱った。それ以外に無断の仕様逸脱は見つからなかった。probe の結果 native 再計測口の追加分岐 (tasks 1.2) は採られておらず、native Core/UI に diff がないことと整合する
- **証跡と提出コードの対応**: iOS の最終証跡 `ios-final2-*` (19:43〜20:09) と `ios-fix5-specific-04-reconnect-restored.png` (19:34) は、iOS bridge の最終更新 `KsBridgeCellContentHostView.swift` (19:16) より後。Android の `android-reverify-*` (16:35〜16:47) は Android/共有コードの最終更新 (`KsSettingsController.cs` 15:21 / `KsBridgeCustomCell.kt` 15:19) より後。19:16 の変更は iOS bridge 限定で Android 表示に影響しないため、Android 証跡は有効
- **sample-parity**: MAUI のメニュー文言「CustomCell デモ」・画面タイトル・5 Section の header/footer・Cell 表示文言・デモデータ (40 行 / 初期値 70・40・60 / 本文) を native 2 platform と 1 文字ずつ照合し、完全一致を確認 (`CustomCellDemoPage.xaml` ⇔ `CustomCellDemoView.swift` / `CustomCellDemoScreen.kt`)。スライダーの thumb 描画差は MAUI = Android (thumbColor 指定あり) / iOS native = SwiftUI Slider の既定という native 側の制約で、本変更に起因しない
- **XML doc の記述の実証**: `CustomCell.ContentProperty` の doc が主張する「変換経路が先に所有を確定させる」順序を、一時テスト (レビュー後に削除・`git status` で復元確認済み) で実測。既配置 View を Content に代入して例外が出た後も既存内容の論理親と実体が保たれることを確認した (doc の記述は正しく、失敗した配置が既存配置を壊す経路はない)

## 指摘事項

### [🟠 Major] Android の行タップに操作証跡がなく、子要素タップの二重発火なしが未確認

**該当箇所**: `kasane/changes/add-maui-custom-cell/screenshots/`（`android-reverify-parity-06-showarrow-ontap.png` が該当箇所だが「0 回」の静止画のみ）/ 対応 Requirement: maui-cells「行タップは Command / Tapped で通知される」

**問題点**:
maui-cells の Scenario「行タップで Command が発火する」「content 内の操作はタップを消費し二重発火しない」について、iOS は操作後の状態を残している (`ios-final2-parity-09-rowtap-count2.png` = カウンタ 2 回、`ios-final-parity-05-pill-tap-no-double-fire.png` = ピルタップで進まない) が、Android 側には対応する証跡が 1 枚もない。`android-reverify-parity-06-showarrow-ontap.png` はカウンタが「0 回」のままの初期状態であり、行タップが発火したことも、ピル (子要素) タップで発火しないことも示していない。

この Scenario は native 契約そのままではなく **MAUI 固有の合成部分**にリスクがある — MAUI が輸送する platform view は MAUI の `TapGestureRecognizer` を持つ ViewGroup であり、それが native 行の click よりも先にタッチを消費するかどうかは Android の touch dispatch に依存する。ユニットテストは `itemViewAt(host, CELL_ROW)!!.performClick()` (`KsBridgeCustomCellTest.kt:446`) で行の click を直接呼ぶため、子 View のタッチ消費経路を一切通らず、二重発火の有無を測っていない (iOS 側 `KsBridgeCustomCellTests.swift:392` の `didSelectItemAt` 直接呼び出しも同じ性質)。つまり両 OS とも「消費関係」はユニットテストの射程外であり、iOS だけが実機確認で埋めている状態。

**推奨修正**:
Android (エミュレータ可) で パリティ画面「showArrow / onTap」Section を操作し、次の 2 枚を `screenshots/` へ追加する。
1. 「行タップカウンタ」行の**行領域**をタップした後 — カウンタが進んでいること
2. 同じ行の**ピル (子要素)** をタップした後 — カウンタが進まず 0 に戻ること

あわせて `IsEnabled = false` の行 (「無効」スライダー) を実際にドラッグしようとして動かないことも同じ機会で撮れると、maui-cells「無効時は content 内部の操作も抑止される」の操作証跡も揃う (こちらは現状 iOS/Android とも視覚状態の静止画のみ)。

### [🟡 Minor] バッチ追加では内容 View の重複が事前検査を抜け、部分適用してから例外になる

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1175-1192` (`AddCells`) と `:2086-2100` (`EnsureCellsAreNotPlaced`)

**問題点**:
`AddCells` が通す事前検査は `EnsureCellsHaveNoDuplicates` (Cell インスタンス同士の重複のみ) と `EnsureCellsAreNotPlaced` (各 Cell の Content を**既に置かれている** View と照合するのみ) の 2 つで、**追加バッチ内で同じ View インスタンスを Content に持つ 2 つの CustomCell** を弾かない。この場合、1 件目は `_gateway.InsertCell` + `RegisterCell` まで完了し、2 件目の `SetCellContent` → `EnsureContentViewIsNotPlaced` (`:2137`) で初めて `InvalidOperationException` になる。結果として native には 1 件目だけが挿入され、対応表も片方だけ埋まった状態で例外が投げられる。

Root 再構築の経路は `EnsureTreeHasNoDuplicates` (`:2046-2049`) が `AddSeenView` でバッチ内重複まで事前に弾いており、`CustomCellContentTests.RebuildingWithADuplicateContentThrowsWithoutTouchingTheCurrentTree` が「現在の木に触れずに例外」を保証している。この経路だけ保証の粒度が落ちる。

なお同型の穴は accessory 側にも既にあり (`EnsureSectionsAreNotPlaced` (`:2071-2084`) はバッチ内の HeaderView / FooterView 重複を見ない)、本変更が新規に持ち込んだものではない。spec は「検出時に `InvalidOperationException` を送出する」ことしか要求しておらず、その点は満たしている。

**推奨修正**:
`EnsureCellsAreNotPlaced` にバッチ内の `HashSet<View>` を持たせ、`AddSeenView` と同じ判定でバッチ内重複も事前に弾く。accessory 側 (`EnsureSectionsAreNotPlaced`) にも同じ手当を入れると対称になる。テストは `section.Cells` へ同一 View を Content に持つ 2 Cell を 1 回の Add で入れ、例外後に 1 件目も未挿入であることを確認する形。

### [🔵 Suggestion] CustomCell に IconSource を設定すると空振りの画像解決と再配信が走る

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1956` (`RegisterCell`) / `:1440-1444` (`HandleCellPropertyChanged`)

**問題点**:
`ResolveIcon` は Cell 種別に依らず呼ばれるため、CustomCell に `IconSource` を設定すると画像解決が走り、解決結果が変わった時点で `StoreIcon` → `MarkContentDirty` (`:1569`) で `ReplaceCell` が飛ぶ。`KsBridgeCustomCell.makeCell` は `icon` を読まないので表示は変わらず、spec の silent no-op (「例外・警告は発生せず、Content の表示は変わらない」) には反しない。ただし画像デコードと native への再配信が完全に空振りする。

`CustomCellTests.InapplicablePropertiesAreIgnoredWithoutAnyDelivery` (`:189-214`) は `IconSize` / `IconRadius` は設定するが `IconSource` は設定しておらず、この経路は測られていない。

**推奨修正**:
`ResolveIcon` の入口で CustomCell を早期に「画像なし」確定にする (`_icons` に載せない) か、少なくとも「IconSource を設定しても表示は変わらないが配信は起きる」ことをテストで明示し、意図した挙動として固定する。

### [🔵 Suggestion] 入れ物の表示状態変化ごとに生存入れ物を全件走査している

**該当箇所**: `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentHostView.swift:161-174` (`didMoveToWindow`)

**問題点**:
表示への出入りのたびに `liveHosts` (弱参照の全生存入れ物) を線形走査し、同じ content を待つ入れ物へ `setNeedsLayout()` を送る。入れ物は行のリサイクルで作られ続けるため、スクロール中は (生存入れ物数 × 出入り回数) のコストになる。40 行規模の実測 (`ios-final2-scroll-s1〜s4`) では問題が出ていないので現状の実害はない。

**推奨修正**:
必要なら `content` をキーにした待機入れ物の索引 (`NSMapTable<UIView, NSHashTable<KsBridgeCellContentHostView>>` 等) を持たせ、走査対象を同じ content を待つ入れ物だけに絞る。現状は実害が観測されていないため、後続で埋め込み規模が増えたときの選択肢として残す位置づけでよい。

## アクションプラン

1. **[Major]** Android で行タップ / ピルタップの操作証跡 2 枚を撮り `screenshots/` へ追加する (あわせて無効行の操作抑止も撮れると尚良い)。これで判定は APPROVED へ移る
2. **[Minor]** `EnsureCellsAreNotPlaced` にバッチ内重複検査を足す (accessory 側も対称に)。テスト 1 本追加
3. **[Suggestion]** CustomCell の icon 解決スキップ、または現挙動をテストで固定
4. **[Suggestion]** `liveHosts` 走査の索引化 — 実害が出るまで保留可

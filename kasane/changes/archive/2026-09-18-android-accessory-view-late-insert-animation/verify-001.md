# Verify 001: android-accessory-view-late-insert-animation

- 日付: 2026-09-18
- 対象: デルタスペック 4 本 (`specs/android-host/spec.md` / `specs/ios-host/spec.md` / `specs/maui-core/spec.md` / `specs/maui-cells/spec.md`) と、作業ツリーの未コミット差分 (ベース: HEAD = develop)
- 判定: **VALID**

デルタスペック 4 本の Requirement 6 件・Scenario 31 件すべてに実装とテストの対応が付いた。未記録の乖離は無く、足場アーティファクト (proposal / design / specs) の逆流も無い。3 platform のテストを実行して全件成功を確認した。

未完了として残るのは tasks 6.5 のみで、tasks の但し書き (「不可ならリリース後の確認項目としてオーナーへ申し送る」) に沿った未チェックである (下の「tasks.md の検査」)。

---

## 対応表

状態の記号: ✅ 一致 / ⚠️ deviation 記録済み / ❌ 欠落・乖離

### specs/android-host/spec.md

#### MODIFIED Requirement: window attach 時の Store 現在状態からの復元

要件文の変更は「Root の header / footer は別 Requirement が扱う」旨への記述改めのみで、Scenario は現行のまま。実装変更を伴わない (tasks 2.5 は既存回帰の再確認)。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| attach 前の更新が attach 後に反映される | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:411` (既存の attach 時復元。変更なし) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/AttachOrderRestoreTest.kt:118` | ✅ |
| detach 中の更新が再 attach で反映される | 同上 | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/AttachOrderRestoreTest.kt:195` | ✅ |

#### ADDED Requirement: 購読できていない間に渡された Root の header / footer の保持

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| attach 前に渡した Root の header が attach 後に表示される | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:280` `:357` → `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:664` `:689` `:430` | `.../AttachOrderRestoreTest.kt:275` | ✅ |
| attach 前に渡した Root の footer が attach 後に表示される | 同上 | `.../AttachOrderRestoreTest.kt:296` | ✅ |
| detach 中に渡した値が再 attach で反映される | 同上 (`KsSettingsView.kt:430` が attach 時に未反映分を先に流す) | `.../AttachOrderRestoreTest.kt:317` | ✅ |
| 複数回渡した場合は最後の値が反映される | `KsSettingsView.kt:689` (控えは target ごとに上書き、`nil` も値として保持) | `.../AttachOrderRestoreTest.kt:343` | ✅ |
| attach 中の更新は一度だけ適用される | `KsSettingsView.kt:619` (取り付け中の通知購読が Root 対象を読み飛ばす) | `.../AttachOrderRestoreTest.kt:554` | ✅ |
| bind 直後にキューを流さず渡した値が表示される | `KsSettingsView.kt:549` (`bind` の中で受け口を登録)・`SettingsRootStore.kt:357` (同期配送) | `.../AttachOrderRestoreTest.kt:367` | ✅ |
| キューを流さない多数回の連続更新でも最後の値が表示される | 同上 (通知の容量に依存しない同期配送) | `.../AttachOrderRestoreTest.kt:386` (200 回 > 容量 64) | ✅ |
| 別 Store へ bind し直した後は以前の Store の値が反映されない | `KsSettingsView.kt:542` `:546` `:709` (登録解除と配送元の差し替え・控えの破棄を同一ロック内で行う) | `.../AttachOrderRestoreTest.kt:511`、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RootAccessoryRebindRaceTest.kt:165` | ✅ |
| unbind されずに手放された Host は回収できる | `SettingsRootStore.kt:336` (弱参照で登録)・`:372` (切れた登録の除去) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RootAccessoryReceiverReleaseTest.kt:68` | ✅ |
| unbind 後に渡した値は反映されない | `KsSettingsView.kt:571`〜`:575` | `.../AttachOrderRestoreTest.kt:534`、`.../RootAccessoryRebindRaceTest.kt:182` | ✅ |
| 同じ Store に bind した 2 つの Host の両方が更新される | `SettingsRootStore.kt:357` (登録全件へ配送) | `.../AttachOrderRestoreTest.kt:410` | ✅ |
| 同じ Store へ bind し直しても適用は 1 回である | `SettingsRootStore.kt:336`〜`:341` (同一相手は重複登録しない)・`KsSettingsView.kt:534` (同一 Store の再 bind は購読維持) | `.../AttachOrderRestoreTest.kt:580` | ✅ |
| 一方の unbind は他方の受け取りを妨げない | `SettingsRootStore.kt:343` (指定した相手の登録だけを外す) | `.../AttachOrderRestoreTest.kt:431` | ✅ |
| メインスレッド以外から渡した値が attach 後に表示される | `KsSettingsView.kt:676`〜`:680` (控えは呼ばれた場で、反映はメインスレッドへ) | `.../AttachOrderRestoreTest.kt:455` | ✅ |
| attach 中にメインスレッド以外から渡した値はメインスレッドで反映される | 同上 | `.../AttachOrderRestoreTest.kt:477` | ✅ |

Scenario を持たない Requirement 本文の SHALL / SHALL NOT についても対応を確認した:

| 要件文の条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| ある Host での反映の失敗は呼び出し元へ伝わらず、他の Host の受け取りと通知の発行を妨げない | `SettingsRootStore.kt:357`〜`:370` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RootAccessoryDeliveryFailureTest.kt:134` `:174` | ✅ |
| Host のプロパティ (`rootHeader` / `rootFooter`) への直接代入は従来どおり有効 | `KsSettingsView.kt:280` `:291` (プロパティは据え置き、受け口も同じ setter を通る) | 既存の Root accessory テスト群が継続 | ✅ |

### specs/ios-host/spec.md

#### MODIFIED Requirement: view load 時の Store 現在状態からの復元

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| view load 前の構造操作が load 時に反映される | `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:476` (`resyncFromStore`。変更なし) | `ios/Tests/KsSettingsViewUITests/HostViewLoadRestoreTests.swift:125` | ✅ |
| view load 前の Cell 内容更新が load 時に反映される (両経路) | 同上 | `.../HostViewLoadRestoreTests.swift:167` (単発)・`:189` (バッチ) | ✅ |
| view load 前の Section accessory / theme 変更が load 時に反映される | 同上 | `.../HostViewLoadRestoreTests.swift:217` | ✅ |
| Store 接続中の直接 applyTheme は view load 時に Store theme で上書きされる | 同上 | `.../HostViewLoadRestoreTests.swift:256` | ✅ |
| Store 非接続 init は従来どおり init 時の root で表示する | `KsSettingsViewController.swift:1396` (`applyBeforeViewLoad` は `.full` のみ root を差し替える従来の扱いを維持) | `.../HostViewLoadRestoreTests.swift:415` | ✅ |
| (取り除く指定) Root accessory は復元対象外で、所有者の再適用により反映される | — | `test_RootAccessoryは復元対象外で所有者の再適用により表示される` は削除済み | ✅ |

#### ADDED Requirement: view load 前に渡された Root の header / footer の保持

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| view load 前に渡した Root の header が load 時に表示される | `KsSettingsViewController.swift:1338`〜`:1341` → `:1396`〜`:1410` (`applyBeforeViewLoad` がプロパティへ控える) | `.../HostViewLoadRestoreTests.swift:290` | ✅ |
| view load 前に渡した Root の footer が load 時に表示される | 同上 | `.../HostViewLoadRestoreTests.swift:319` | ✅ |
| 値を渡しても view load は起きない | 同上 (プロパティの `didSet` は `collectionView` 未生成の間は何もしない) | `.../HostViewLoadRestoreTests.swift:346` | ✅ |
| 複数回渡した場合は最後の値が反映される | 同上 (プロパティ代入なので最後の値が残る) | `.../HostViewLoadRestoreTests.swift:365` | ✅ |
| view load 前の解除が load 時に反映される | 同上 (`nil` も値として代入) | `.../HostViewLoadRestoreTests.swift:393` | ✅ |

### specs/maui-core/spec.md

#### ADDED Requirement: 配置済みの view accessory は Native Host の最初の表示に含まれる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 初回の表示で Section の View だけの Footer が最初から表示される | `maui/KsSettingsView.Maui/SettingsView.cs:995` (実体化の口を接続より前へ)・`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1280` `:1309` (`MaterializeTreeViews`) | `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:35` | ✅ |
| 初回の表示で Section の HeaderView が最初から表示される | 同上 | `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:53` | ✅ |
| Root の view accessory は取り付けを待たずに適用される | `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:77` (Host 生成直後・`AddToParent` より前)・`KsSettingsController.cs:459` | `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:68`、`maui/KsSettingsView.Maui.Tests/HandlerTests.cs:219` | ✅ |
| 再接続でも view accessory は Native Host の生成までに届く | `SettingsView.cs:989`〜`:990` (`AttachViews` → `ApplyStoreViews`)・`KsSettingsController.cs:479` | `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:96` | ✅ |
| 切断中に配置した View も再接続時の最初の表示に含まれる | 同上 | `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:125` | ✅ |
| 取り付けの通知では view accessory を適用し直さない | `SettingsViewHandler.cs:167` (`OnHostAttached` は `ConfirmAdded` のみ) | `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:146`、`maui/KsSettingsView.Maui.Tests/HandlerTests.cs:236` | ✅ |

補足: 表示中の Root 作り直しでは、後片付け待ちの実体を持つ View を事前実体化から外す (`KsSettingsController.cs:1340` `:1353`)。これは deviation.md の「表示中の Root 作り直しでは後片付け待ちの View を事前実体化から外す」(オーナー追認) にあたり、本 Requirement が対象外とする「表示中の SettingsView に対して実行時に View を配置・差し替え・解除した場合」の経路である。⚠️ deviation 記録済み。

### specs/maui-cells/spec.md

#### ADDED Requirement: CustomCell の Content は Native Host の最初の表示に含まれる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 初回の表示で Content が最初から行に入っている | `KsSettingsController.cs:1326`〜`:1331` (配信データを組む前に Content を実体化) | `maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:29` | ✅ |
| 再接続でも Content は Native Host の生成までに届く (1 バッチ) | `KsSettingsController.cs:479`〜`:501` (`ApplyStoreViews` が `DeliverCellContents` を 1 回だけ呼ぶ) | `maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:51` | ✅ |
| 取り付けの通知では Content を配信し直さない | `SettingsViewHandler.cs:167` | `maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:83` | ✅ |

---

## 追加検査

### tasks.md の検査

- 全 25 項目のうち 24 項目が `[x]`、1 項目 (6.5) が `[ ]`。
- 対応表と突き合わせた結果、**未実装なのにチェック済みの項目は無い**。実環境の観測タスク (1.1 / 1.2 / 1.3 / 6.1 / 6.2 / 6.3 / 6.4) は `evidence/README.md` に対応する節 (1.1 / 1.2 / 1.3 (Simulator) / 1.3 (実機) / 6.1 / 6.2 / 6.3 (Simulator) / 6.3 (実機) / 6.4) と証跡ファイルがあり、tasks 5.1 は `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ConsecutiveAccessoryViewSwapTest.kt:67` が対応する。
- **tasks 6.5 (`../ColorAnalyzer` での確認)**: 未チェックのままで正しい。tasks の但し書きは「ローカル参照でのビルドが可能な場合。不可ならリリース後の確認項目としてオーナーへ申し送る」であり、相手リポジトリが公開パッケージ版の `PackageReference` でローカル参照ビルドができない以上、確認は実施できず、`[x]` を付けると虚偽になる。申し送りが済めば項目自体は閉じられるが、本 change の完了条件として「確認の実施」は求められていない。
  - ただし申し送りの記録は `review-003.md:111` / `review-004.md:148` / `review-005.md:136` / `review-006.md:103` の「方針と申告されている」という伝聞形でしか残っていない。change 自身の長命な記録 (deviation.md か tasks の但し書き行への追記) に残すことを推奨する。**判定には影響しない** (Requirement / Scenario の欠落ではなく、虚偽チェックでもないため)。

### 逆流検査 (足場アーティファクトの書き換え)

- 作業ツリーの未コミット差分に `proposal.md` / `design.md` / `specs/**` は含まれない (変更されているのは `tasks.md` のみ)。
- `git log` 上でも足場 3 種は `1a9555b` の 1 コミットで作られたきりで、以後の書き換えは無い。
- `tasks.md` の差分はチェックボックス (`[ ]` → `[x]`) のみで、本文の書き換えは無い。
- → **逆流なし**。

### 未記録乖離の洗い出し

対応表に ❌ は無い。Scenario に対応しない差分についても、すべて deviation.md か tasks.md に対応がある:

| 差分 | 対応 |
|---|---|
| `android/.../RootAccessoryReceiver.kt` (新規・受け口の切り出し) | deviation.md「Android の受け口を…`RootAccessoryReceiver` として切り出し」「Host が private に持つ受け口オブジェクトへ移し」 |
| `SettingsRootStore.kt` のキャンセル合図の投げ直し | deviation.md「Android の Store の配送失敗の握り潰しから、コルーチンのキャンセルの合図を除外して投げ直す」 |
| `android/.../RootAccessoryRebindRaceTest.kt` / `RootAccessoryDeliveryFailureTest.kt` (新規) | 上記 2 件の deviation に対する回帰テスト |
| `android/.../ConsecutiveAccessoryViewSwapTest.kt` (新規) | tasks 5.1 |
| `maui/.../SettingsViewHandler.cs` の `RollbackHost` と失敗単位化 | deviation.md「MAUI の Handler は、Native Host の生成・Root の accessory の適用・親子関係の登録を 1 つの失敗単位として扱う」 |
| `maui/.../KsSettingsController.cs` の `Disconnect` / `DisposePlacedViews` / `ReleaseHost` / `TryCleanUp` / 預かり破棄 | deviation.md の該当 5 件 (実体化の口の解放・対応表の直接走査・接続失敗の全件試行・`ReleaseHost` の 4 単位独立試行・退役の知らせが失敗した実体の破棄の延期) |
| `maui/.../MaterializeTreeViews` が後片付け待ちの View を外す | deviation.md「MAUI の設定ツリー全体の事前実体化 (`MaterializeTreeViews`) は、後片付け待ちの実体を持つ View を対象から外す」(オーナー追認あり) |
| `maui/.../Fakes/*` (`FakeSettingsGateway` / `FakeViewMaterializer` / `RecordingHostContainment` / `UnsubscribableCellCollection` 新規)・`SettingsViewHandler.Standard.cs` の生成失敗の切り替え | deviation.md の `[付随修正]` 5 件 |
| `maui/.../Fakes/GatewayScope.cs` の `Attach()` 削除と呼び出し 39 箇所の除去 | deviation.md `[付随修正]`「空になった `Attach()` と、その呼び出し 39 箇所」 |
| `maui/.../Fakes/GatewayScope.cs` の `ConnectFacade` / `CreateHost` への役割変更、`GatewayCall.cs` の `SectionTransport` 追加 | tasks 4.3 (「テスト足場の『取り付け』操作を、親子関係の確定だけを表すものへ役割を変え」) |
| `maui/.../AccessoryTests.cs` / `ImplicitStyleTests.cs` / `CustomCellTests.cs` / `LeakTests.cs` / `LogicalChildTests.cs` / `ThemeAndCellStyleTests.cs` | deviation.md `[付随修正]` 3 件 (`ApplyRootAccessories` への追随・旧契約を語る記述の修正・`Attach()` 呼び出しの除去) |
| `samples/maui/.../AccessoryViewsDemoPage.xaml` / `.xaml.cs` (折り返す FooterView の追加と番号の繰り下げ) | deviation.md `[付随修正]`「折り返して複数行になる Label を持つ Section FooterView を新しい ③ として足し」 |
| `kasane/handbook/cross/local-development-setup.md` の追記 | deviation.md `[付随修正]`「MAUI Android」節の追記 (オーナー承認済み) |
| `kasane/changes/ios-customcell-initial-height-grow-animation/exploration.md` への追記 | tasks 6.3 の明示指示 |
| `kasane/changes/maui-teardown-failure-safety/` (新規) | deviation.md「レビューの収束…残りは `kasane/changes/maui-teardown-failure-safety` へ簡易起票」 |
| doc コメントの改訂 (Android Host / Store / Bridge、iOS Host / Bridge、MAUI Handler / controller / SettingsView) | tasks 2.6 / 3.5 / 4.8 |

→ **未記録の乖離は無し**。

### UI 変更の検査

本 change に `ui/` アーティファクト (デザインブリーフ・モック) は無い。変更の対象は「既に配置されている View が最初の表示に含まれるか」という観察可能な状態遷移であり、視覚デザインの新規決定を伴わないため、モック承認ゲートは適用外。実環境での見え方の確認は tasks 1.x / 6.x の証跡 (`evidence/`) が担っており、修正前後の比較と判定が `evidence/README.md` に記録されている。

### テストの実行結果 (本検証で実行)

| platform | コマンド | 結果 |
|---|---|---|
| Android | `./gradlew test --rerun-tasks` (`android/` で実行) | 2942 tests / 0 failures / 0 errors (`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の合算) |
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,...'` (`ios/` で実行) | 1050 tests / 0 failures (バンドル集計行の合算: Bridge 166 + Core 88 + SwiftUI 98 + TestSupport 7 + UI 691) |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 589 tests / 0 failures |

標準 lint も実行して確認した: `scripts/local-path-lint.py` / `scripts/identity-lint.py` とも違反なし (tasks 7.2)。

---

## 判定

**VALID**

- Requirement 6 件・Scenario 31 件すべてが ✅ 一致 (⚠️ deviation 記録済みの補足が 1 件。対象外経路のため Scenario の判定には影響しない)
- tasks.md に虚偽チェックなし。未チェックの 6.5 は tasks の但し書きに沿った正しい状態
- 足場アーティファクトの逆流なし
- 未記録の乖離なし
- 3 platform のテストを実行し全件成功

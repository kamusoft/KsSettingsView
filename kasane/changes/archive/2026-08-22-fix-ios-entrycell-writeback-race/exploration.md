# Exploration: fix-ios-entrycell-writeback-race

## 課題 / 動機

Android で確認・修正した EntryCell の書き戻しレース (fix-entrycell-writeback-caret-race / android/ADR-0014) は、構造としてはプラットフォーム非依存のラウンドトリップ競合である: 打鍵 → `onTextChanged` 通知 → 呼び出し側の Store コミット → 次フレームの再バインドが**古い値**で入力欄を巻き戻す。「配信スナップショット確定 → 再バインド適用」の窓に次の打鍵が挟まると、文字の欠落・並び替え・キャレット移動が起き、壊れた値が書き戻しでアプリ状態まで確定する。

起票時点 (2026-08-11) では iOS 側は未検証だったが、2026-08-21 の机上確認 (下記) で**同型の構造が iOS にも存在する**ことを確認した。

## 机上確認の結果 (2026-08-21、ksn-scout)

**結論: レースの構造は iOS にも存在する。ガードは同値ガードのみで、android/ADR-0014 が「同値でないからこそ setText が走る」と不十分と判定した形と同一。**

1. 再バインド時の text 代入 — `ios/Sources/KsSettingsViewUI/EntryCellView.swift:131-135` (`render(cell:theme:)`):
   ```swift
   isProgrammaticUpdate = true
   if textField.text != entry.text {
       textField.text = entry.text
   }
   isProgrammaticUpdate = false
   ```
   - `isFirstResponder` ガード: **なし** (同ファイルに参照ゼロ。iOS 側で `isFirstResponder` ガードを持つのは NumberPicker / TimePicker / DatePicker の `embeddedField` のみ)
   - 同値ガード: あり (AiForms 由来、日本語 IME の同値再代入による markedText 破壊回避が目的)
   - `markedTextRange` ガード: **なし** (リポジトリ全体で参照ゼロ)
   - `isProgrammaticUpdate` は `handleEditingChanged` の再入抑止であり、巻き戻し自体は防がない。キャレット復元もない (UITextField は `text` 代入で選択位置が末尾へ移る)
2. ラウンドトリップ: 打鍵 → `handleEditingChanged` → `textChangedHandler?(...)` (`EntryCellView.swift:207`、同期) → 呼び出し側 `SettingsRootStore.replaceCell` → `diffSubject.send` (`SettingsRootStore.swift:208`) → `KsSettingsViewController.swift:305` の `.sink` (同期) → `applyContentUpdate` が `reconfigureItems` + `dataSource.apply(snapshot, animatingDifferences: true)` (`KsSettingsViewController.swift:1985-1988`)。Store コミットは同期だが、`reconfigureItems` の実適用 (= `render()` 再実行) は diffable data source の適用完了時 = 次のレイアウトパスで、Android の「notifyItemChanged → 次フレームで bind」と同型の窓が開く。MAUI 経由 (`KsSettingsController.ScheduleFlush()`) では dispatcher post 分だけ窓が広がる点も Android と同じ
3. 既存テスト: フォーカス中の再バインドで text が巻き戻らないことを固定するテストは `ios/Tests/` 全体で **0 件**

不確かな点: 「次のレイアウトパスで render される」は `reconfigureItems` の一般挙動からの推論で、本リポジトリでの実測 (ログ / 実機) はまだ。実機での再現率 (Android の adb バースト注入相当) は未検証。

### 経路別の窓の整理 (2026-08-22 探索)

| 経路 | 打鍵 → Store コミット | 窓 |
|---|---|---|
| Store 直接利用 | `handleEditingChanged` → `onTextChanged` → `store.replaceCell` → `diffSubject.send` → `applyDiff` まで全部同期 | `dataSource.apply(animatingDifferences: true)` の実適用タイミング次第 (同期なら同値ガードで止まる / 次レイアウトパスなら窓あり)。未実測 |
| SwiftUI DSL | Binding setter → `@State` → body 再評価 → `updateUIViewController` → `store.replaceCell` (`KsSettingsView.swift:343,415`) | 構造的に非同期 → 窓あり確定 |
| MAUI | `KsSettingsController.ScheduleFlush()` の dispatcher post (`KsSettingsController.cs:1645`) | 構造的に非同期 → 窓あり確定 |

Store 直接経路が未実測でも、SwiftUI / MAUI の 2 経路は構造だけで窓の存在が言える。

iOS 固有の材料: `boundCellId` 相当がなく同一性判定の保持変数を足す必要がある (`prepareForReuse` で破棄) / フォーカス喪失フックは `textFieldDidEndEditing` が実装済み / `isProgrammaticUpdate` で再同期の `onTextChanged` 抑止が既存機構で満たせる / `KsListCellBase.clearContentStackTrailingViews` に first responder 保護の先例あり / `UIWindow.makeKeyAndVisible()` で first responder を成立させるテストパターンが `InputCellsTests.swift:678` にある。

### 公開前に扱う理由

入力セルでの文字欠落・並び替えはデータ破損級の欠陥で、構造上の穴が確認できた以上、初回リリース前に塞ぐ。

## 調査・実装の進め方 (論点確定後、2026-08-22)

1. **再現スクリプト**: Android の `repro-burst-loop.sh` を mobilecli + Simulator `<ios-simulator-udid>` 向けに移植する (置換マッピングは「論点 1 の調査結果」を参照)。対象はサンプルのメール欄 (`tanaka.taro@example.com`、maxLength なし) と「ニックネーム (callback)」欄 (Store 経路)。先に `io text` が 1 文字ずつ `editingChanged` を発火させることをログで確認する
2. **修正前の再現** (A): 有効 15 試行以上でレース由来の欠落・並び替えを記録する
3. **ガード実装**: ios/ADR-0004 の Decision どおり `EntryCellView` に同一性保持 (`boundCellID` 相当) と first responder 中の text 代入スキップ、`textFieldDidEndEditing` での再同期 (`isProgrammaticUpdate` で通知抑止) を実装する
4. **unit test 7 本** (論点 3 の決定を参照) + 既存テストの回帰確認
5. **修正後の確認** (B): 同一手順で有効 15 試行 FAIL 0、バースト後の入力継続、日本語 IME 変換中のエコーで markedText 維持 (tap 操作 + スクリーンショット)。証跡は change 配下の evidence.md に残す
6. 蒸留時に ios/ADR-0004 を accepted へ昇格し、concepts (input-cells.md の「iOS の同型契約は未検証・未導入」を更新、ios ホスト知識へ契約を追記) を追随させる

## 論点 1 の調査結果 (2026-08-22、ksn-scout)

### 注入手段の候補評価

| 候補 | キーボード経路 | 速度/制御性 | Sim | 実機 | 読み戻し | シェルループ |
|---|---|---|---|---|---|---|
| mobilecli `io text` + `dump ui` (mobile-mcp 同梱の Go CLI) | ○ XCUITest runner 経由 (推定) | 一括注入、頻度指定なし | ◎ 導入済み・`dump ui` 実測 OK | △ agent install + プロビジョニングプロファイル要 | ◎ `dump ui` JSON の `value` / `rect` | ◎ |
| XCUITest `typeText` (samples/ios に UI テストターゲット新設) | ◎ | ◎ テスト内でループ・アサート | ◎ | ○ (署名要) | ◎ `element.value` | △ xcodebuild test の終了コード |
| WDA `POST /wda/keys` (curl 直叩き) | ◎ `FBTypeText()` の HID 合成 | ◎ `frequency` を明示指定できる唯一の手段 | ○ WDA runner を別途導入 | ○ | ○ `/source` | ◎ |
| idb `ui text` | ○ | ○ | 未インストール | 実機対応の明記なし | ○ | ◎ |
| `xcrun simctl` 単体 | 不可 (テキスト入力サブコマンドなし) | | | | | |
| Simulator ハードウェアキーボード + osascript | △ ソフトキーボード / IME を通らない | ◎ | ○ | ✗ | ✗ | ○ |

### この Mac の環境

- Xcode 26.5 (17F42)。Booted Simulator 2 台: `3B42B268-…` iPhone 17 (iOS 26.0、サンプル ios/maui インストール済み、mobilecli agent 未導入) / `<ios-simulator-udid>…` iPhone 17 (iOS 26.5、mobilecli agent 0.0.23 導入済み、サンプルあり)
- 物理 iPhone: `pixie5` (iPhone 15 / iOS 26.6) が paired・online。mobilecli agent 未導入 (`mobilecli agent install --provisioning-profile` が必要)。`pixie4` (iOS 16.6.1) は go-ios トンネル非対応
- ツール: idb なし / appium 3.1.2・go-ios v1.0.188 あり / mobilecli 1.0.0 は npx キャッシュ内 (パス不安定 → `npm i -g mobilecli` か `npx mobilecli` で呼ぶ)

### リポジトリ内の既存資産

- `samples/ios/KsSettingsViewSample.xcodeproj`: アプリターゲットのみ (XCUITest ターゲットなし、リポジトリ全体で `XCUIApplication` 0 件)。`ios/Tests/KsSettingsViewUITests/` は名前に反して SwiftPM のユニットテスト
- `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift`: 「最後のイベント」ラベルあり (81 行目)。名前欄は `maxLength: 20` (Android と同じ飽和問題) → **メール欄 `tanaka.taro@example.com` (maxLength なし) を対象にする**。「ニックネーム (callback)」は Store 経路で別に取れる
- `EntryCellView` に accessibilityIdentifier なし → `dump ui` では値の PREFIX で行を引く (Android と同方式)
- `scripts/` に iOS 向けスクリプトなし。MAUI サンプルの iOS 手順は `samples/maui/README.md` 70-100 行目 (README は Xcode 26.1.1 の DEVELOPER_DIR を指定、現環境 26.5 との整合は未検証)

### repro-burst-loop.sh の置換マッピング

| Android | iOS |
|---|---|
| `adb get-state` | `mobilecli devices` / `simctl list devices` |
| タップ + `KEYCODE_MOVE_END` | `mobilecli io tap` で**入力欄の右端**をタップ (MOVE_END 相当なし、MAUI 版と同方式) |
| `adb shell input text abcde` | `mobilecli io text --device <id> abcde` |
| `uiautomator dump` + regex | `mobilecli dump ui` の JSON を python3 で `value` PREFIX 検索、`rect` で bounds 不変チェック |
| 判定・MIN_VALID=15・非ゼロ終了 | そのまま流用 |
| A/B のリビルド | `xcodebuild build` (`-derivedDataPath` 固定) → `simctl install` → `simctl launch` |

### 不確かな点

1. mobilecli `io text` が本当に HID 合成で 1 文字ずつ `editingChanged` を発火させるかは未確認 → 採用前にログで 1 回確認する
2. `io text` の文字間隔は制御不可。書き戻し往復より速いかは実測待ち。足りなければ WDA `/wda/keys` + `frequency` へ切り替え (WDA の `maxTypingFrequency` の単位はドキュメント間で矛盾: letters/sec vs keystrokes/min)
3. 物理 iPhone (pixie5) は一切未検証
4. 日本語 IME (markedText) はどの手段でも未検証。`io text` が IME に食わせるのか確定文字として挿入するのかも不明
5. `<ios-simulator-udid>` に `jp.kamusoft.ksdialogs.axprobe.uitests.xctrunner` が存在 — 姉妹プロジェクト KsDialogs で XCUITest ベースの AX プローブを作った先例に見えるがソースは未発見

## 検討した選択肢 (却下案と理由を含む)

論点 0 (修正方針) で比較した (2026-08-22):

| 軸 | 同型ガード (採用) | 世代トークン | MAUI Controller 側エコー抑止 | キャレット復元 |
|---|---|---|---|---|
| 3 経路への効き | 全部 | 全部 | MAUI のみ | 全部 (欠落は解けない) |
| 境界を越えるか | ui 層のみ | bridge DTO + core 輸送契約 | MAUI のみ | ui 層のみ |
| Android との対称性 | 一致 | Android 側も作り直し | — | — |
| IME 保護 | フォーカス中は一切代入しないので markedText 破壊も消える | 同左 | 効かない | 効かない |

`markedTextRange` の個別ガード追加も検討したが、markedText は first responder 中にしか存在せず同型ガードに包含されるため不要 (論点 2 の解)。

## 決定事項

- 簡易 change として scaffold のみ作成 (オーナー指示 2026-08-11、fix-entrycell-writeback-caret-race の蒸留時)
- 公開前トリアージ (2026-08-21): 机上確認で同型構造を確認 → 調査タスクから修正タスクへ格上げ、**初回リリース前に対応**。実装は未着手
- 論点 0 (2026-08-22): android/ADR-0014 と同型の「フォーカス中は入力欄が SSoT」ガード (`isFirstResponder` 中は同一 Cell の再 render で `text` を代入しない + `textFieldDidEndEditing` で再同期) を採用。既存の同値ガードは非フォーカス時のキャレット維持として残す
- 論点 1 (2026-08-22): バースト注入は **mobilecli `io text` + `dump ui` で Simulator `<ios-simulator-udid>` (iPhone 17 / iOS 26.5) を駆動**し、Android の `repro-burst-loop.sh` を同構造で移植する。採用前に `io text` が 1 文字ずつ `editingChanged` を発火させる経路であることをログで 1 回確認する。速度不足で再現しない場合のみ WDA `/wda/keys` (`frequency` 指定) へ切替。**物理 iPhone での確認は不要** (オーナー決定)。ただし **Simulator で再現・A/B が取れない場合のフォールバックとして物理デバイス pixie4 (iPhone 11、WDA 導入済み) の使用を許可** (オーナー、2026-08-22)。その場合は WDA `/wda/keys` (`frequency` 指定可) を直叩きする経路になる — 注意: ksn-scout の環境調査では pixie4 は iOS 16.6.1 で go-ios のトンネルが非対応と出ているため、mobilecli 経路ではなく WDA 直結 (iproxy 等でポート転送) を使う。accepted 昇格の証跡は Simulator + 実 IME の A/B で足りる (実行時挙動の検証規約は Simulator を実環境に含む)。Mac の画面制御 (computer-use / osascript) は使わない (オーナー指示)
- 論点 3 (2026-08-22): テストの役割分担を次のとおりとする
  - **unit test (`ios/Tests/KsSettingsViewUITests/InputCellsTests.swift`、`UIWindow.makeKeyAndVisible()` で first responder を成立させる既存パターン)**: Android 版 tasks 2.1〜2.6 と対称の 7 本 — フォーカス中の同一 id・異 text 再 render で text / キャレット不変 (stale render を明示的に挟む) / フォーカス喪失で再同期・`onTextChanged` 非発火 / 喪失直前入力の保全 / 同一性 3 種 (同 id 異 text・異 id 同 text・`prepareForReuse` 後) / 非フォーカス時の反映 / フォーカス中の placeholder 変更 / フォーカス中の `isEnabled = false` で編集終了 (UIKit が first responder を自動で手放すかは未確認 — テストで assert し、手放さなければ実装側で明示 resign)。既存 EntryCell テストの回帰確認を含める
  - **Simulator 証跡 (ユニットの緑だけでは完了にしない)**: 高速連続入力の完全性 (mobilecli バーストループ、修正前後 A/B、有効 15 試行 FAIL 0) / バースト後の入力継続 / 日本語 IME 変換中のエコーで markedText 維持 (日本語キーボードを `mobilecli io tap` で操作し `simctl io screenshot` で確認。自動ループには乗らないのでスクリーンショット + 手順記録の形)

## ADR 候補

- 作成済み: [ios/ADR-0004](../../decisions/ios/0004-entrycell-focused-editor-owns-text.md) (status: proposed、2026-08-22)。accepted への昇格は実機 A/B の証跡取得後

## 未決の論点

番号は探索中の議論と共通 (論点 0 = 修正方針は決定済み)。

- ~~論点 1: iOS でのバースト注入手段~~ → mobilecli + Simulator で決定 (決定事項を参照)
- ~~論点 2: `markedTextRange` 保護と `isFirstResponder` ガードの重なり・適用範囲~~ → 同型ガードに包含されるため個別ガード不要 (ios/ADR-0004 Decision に記載)
- ~~論点 3: テストの形~~ → unit test 7 本 + Simulator 証跡の役割分担で決定 (決定事項を参照)
- Store 直接経路の窓の有無 (`dataSource.apply` の実適用タイミング) の実測 — 提案段階の相方レビュー (second-opinion-spec-001 Major-1) で「ニックネーム (callback)」欄は DSL 経路と判明。`StoreDemoView` に Store 経路の EntryCell を追加して実測する (tasks 1.0 / 1.5、オーナー裁定 2026-08-22)

## UI 素材

なし (挙動のみ)

## 変更級の推奨: M (理由)

判定材料 (2026-08-22 探索で確定):

- 触る能力: ui 層 1 箇所 (`EntryCellView.render` / `textFieldDidEndEditing` / `prepareForReuse`) + unit test 7 本 + Simulator 向け再現スクリプト (新規、`repro-burst-loop.sh` の移植) + 証跡 (evidence.md)
- 公開 API 変更: なし。ただし**挙動契約の変更** (フォーカス中のプログラム的 text 更新が喪失時まで遅延 / 値 + callback 経路の利用側契約の必須化) を伴い ADR 級 (ios/ADR-0004 proposed)
- 可逆性: ガード自体は戻せるが、契約として concepts (input-cells.md / 新設予定の ios ホスト知識) に載せるため覆すコストは高い
- UI: なし (挙動のみ)
- 実行時挙動の検証規約の対象 (コード修正 + Simulator A/B 証跡が完了条件)

ガードの実装自体は S 相当だが、デルタスペック (Android 版 `settings-view-android-ui` と対称の ios 版)・実機相当の証跡・ADR 確定を伴うため M。Android 版 (fix-entrycell-writeback-caret-race、M) と同規模。

## 関連ファイル

- `ios/Sources/KsSettingsViewUI/EntryCellView.swift` (`render(cell:theme:)` の text 代入・`handleEditingChanged`)
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` (`applyContentUpdate` の `reconfigureItems` 経路)
- `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift` (EntryCell 系テストの置き場)
- `kasane/changes/archive/2026-08-11-fix-entrycell-writeback-caret-race/` (exploration.md の原因分析・repro-burst-loop.sh・evidence.md)
- `kasane/decisions/android/0014-entrycell-focused-editor-owns-text.md` (同型契約の先例)

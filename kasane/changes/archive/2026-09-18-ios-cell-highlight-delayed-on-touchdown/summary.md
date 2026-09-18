# Live Summary: ios-cell-highlight-delayed-on-touchdown

## 最終状態 (何がどうなったか)

iOS で Cell を速くタップしたとき、指を置いた直後に押下色 (`Theme.selectedColor`) が出るようになり、push 遷移中は押下色が残って戻りのアニメーション中にフェードアウトする。スクロールを始めるタッチでは押下色が出ない。

- **押下の伝達**: collection view のタッチ遅延 (`delaysContentTouches`) を切り、内部サブクラス `ImmediateTouchCollectionView` で `touchesShouldCancel(in:)` を制御する。UISwitch / UISlider (とその子孫) の上から始まったタッチはリストがキャンセルせずコントロール優先、それ以外はスクロール開始でキャンセルする
- **押下色の立ち上がり**: 押下状態に入ってから 0.1 秒待ってフェードイン (0.12 秒) を始める。待ちの間にキャンセルされたら何も出ない。待ちの途中で選択された (速いタップ) 場合は待たずに塗り始める
- **押下色の消え方**: 選択を経て解除されるときは 0.25 秒のフェードアウト。選択されずにキャンセルされたとき (スクロール開始) はフェードアウトせず平常色へ戻す (遅延中のキャンセルは何も出ない。フェードイン進行中のキャンセルでは進行中のフェードが完了してから戻ることがあるが、オーナー判断で許容: review-001 A-1)
- **選択解除のタイミング**: 遷移の合図 (`isDisappearing`) はタップハンドラ呼び出しの前にリセットし、ハンドラ内で同期的に push が始まる UIKit 直接ホストでも合図が消えない (review-003)。タップ後 0.1 秒の猶予内に画面が隠れ始めれば (push 遷移など `viewWillDisappear` が届くもの) 選択を残す。pageSheet で開く Picker 系のシート提示では届かないため、シートを開くタップは遷移しないタップと同じく猶予後にフェードアウトする (review-001 A-2、オーナー判断で実態を採用)。戻り際は 0.08 秒待ってから、通常の pop なら遷移中にフェードアウト開始、エッジスワイプなら完了して戻ったときだけ解除 (取り消しなら残す)。遷移しないタップは猶予後に解除
- **再構成との共存**: タップで値が変わる Cell (Radio / Checkbox 等) は直後に再構成されるが、押下色の表示中は render が平常色で上書きしない
- **再利用**: `prepareForReuse` で押下色の予約・表示状態をリセットする

## 採用値と根拠 (却下試行の要点)

| 値 | 採用値 | 根拠 |
|---|---|---|
| `delaysContentTouches` | false | 既定 (true) では速いタップで押下色が TouchUp と同時になり「遷移してから色が変わる」ように見えた (試行 1 で即時性を確認) |
| `touchesShouldCancel` | UISwitch / UISlider 上は false、他は true | 常に true (試行 1) ではスイッチの反応が鈍くばらついた。試行 2 でスイッチ優先にして解消。縦横の向き判定でスクロールに振る案はオーナー判断で不要 |
| `selectedColorHighlightDelay` | 0.1 秒 | 即時塗り (試行 1〜4) は実機でスクロール開始時に一瞬光るのが気になった。0.1 秒の待ちでスクロール開始では反応しなくなった (試行 5) |
| `selectedColorFadeInDuration` | 0.12 秒 | 試行 3 で導入。単独では解除が即時のため一瞬で消えて却下、解除側の調整 (試行 4〜) と組み合わせて採用 |
| `selectedColorFadeOutDuration` | 0.25 秒 | 即時復帰 (試行 4) では Radio / Checkbox のタップがチラついた。フェードアウト導入 (試行 5) で解消 |
| `selectionHoldDuration` | 0.1 秒 | 即時 deselect (試行 1〜3) が「遷移後に色が変わる」体感の主因。猶予内に viewWillDisappear が来るかで遷移の有無を判定 (試行 4) |
| 戻り際の解除開始 | `viewWillAppear` + 0.08 秒 (`returnDeselectDelay`)、遷移中に開始 | `viewWillAppear` で即開始 (試行 4/6) は iPhone 15 で少し早く、`viewDidAppear` (試行 5) と遷移完了後 + 0.08 秒 (試行 7) は遅すぎた。遷移中 + 0.08 秒 (試行 7b) で iPhone 11 / iPhone 15 とも完璧 |

## 触ったファイル

- `ios/Sources/KsSettingsViewUI/ImmediateTouchCollectionView.swift` (新規。init で `delaysContentTouches = false`)
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` (loadView / didSelectItemAt / viewWillAppear / viewWillDisappear / deselectAllItems / `pendingDeselectTask` / 定数 2 本)
- `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` (押下色の状態機械と定数 3 本、`applyRenderedBackgroundColor`、`resetSelectedColor`)
- `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`、`CustomCellView.swift` (render の背景適用の順序と経路、prepareForReuse)
- `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` (prepareForReuse)
- `ios/Tests/KsSettingsViewUITests/CellTouchFeedbackTimingTests.swift` (新規、8 件。review-002 でテストの検出力を強化、review-003 で UIKit 直接ホストの同期 push の回帰テストを追加)
- `kasane/changes/ios-cell-highlight-delayed-on-touchdown/{deviation,evidence}.md`

Sample への確認用の push 遷移は撤去済み (deviation.md)。

## 決定事項 / ADR 候補

- スイッチ (スライド系コントロール) の上から始まったタッチはスイッチ優先で、縦ドラッグでもリストはスクロールしない (オーナー判断)
- 秒数 4 本 (0.1 / 0.12 / 0.25 / 0.1 / 0.08) は実機 2 機種 (iOS 18.7 / iOS 26.6) の体感で決めた値であり、テストでは固定しない
- ADR 候補: 「iOS の押下色は UIKit のタッチ遅延に頼らず、ライブラリ側の遅延・フェード・遷移連動の解除で制御する」— 全 Cell 種の render がこの機構を経由する前提を置くため、ios/ADR-0005 として proposed で起票済み (オーナー選択)

## 検証

- iOS テスト一式: 1062 tests / 0 failures (Simulator iPhone 17、review-001〜003 の指摘対応後)
- Swift 6 言語モードの一時ビルド確認: error 0 件 (新設の関数に `@MainActor` 付与で解消)
- 実機確認: evidence.md
- 独立レビュー: review-001 (CHANGES_REQUESTED) → review-002 (CHANGES_REQUESTED、挙動変更なし) → review-003 (Major 1) → review-004 (APPROVED)

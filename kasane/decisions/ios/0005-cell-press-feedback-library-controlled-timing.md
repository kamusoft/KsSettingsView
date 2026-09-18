---
id: 0005
title: Cell の押下色は UIKit のタッチ遅延に頼らず、ライブラリ側の遅延・フェード・遷移連動の解除で制御する
status: proposed
date: 2026-09-18
---

## Context

iOS で Cell を速くタップすると、押下色 (`Theme.selectedColor`) が指を置いた瞬間に出ず、色が出る前に指が離れて選択・遷移が始まるため「遷移してから色が変わっている」ように見えていた (長押しなら出る)。依存側リポジトリ KsAppKMP のリファレンスアプリでオーナーが実機確認した症状で、Android は同じ `Theme.selectedColor` の指定でタップ直後にリップルが出るため OS 間の体感差になっていた。

実機 (iOS 18.7 / iOS 26.6 の 2 機種) での確認で、症状に効いていた層は 2 つあった:

- 押下が Cell に届くまでの遅れ: `UIScrollView` の既定 `delaysContentTouches = true` により、速いタップでは指を離した瞬間に押下と選択がまとめて Cell へ届く
- 選択解除のタイミング: `didSelectItemAt` で即座に `deselectItem(animated:)` していたため、タップハンドラ (遷移) と同時に押下色のフェードアウトが始まる

一方で、タッチ遅延を単純に切って即時にべた塗りすると、スクロールを始めるタッチでも一瞬光る (Simulator では目立たず実機で目立つ)。また UIKit 既定の `touchesShouldCancel(in:)` は UIControl 上のタッチをキャンセルしないため、遅延を切ると Switch の反応がばらつく。さらに、タップで値が変わる Cell (Radio / Checkbox 等) は直後に再構成され、render が背景を平常色で上書きして押下色を消していた。

前提: 押下色の描画は `configurationUpdateHandler` (`KsCellViewSupport.installSelectedColorHandler`) に集約されており、全 Cell 種 (標準 Cell と CustomCell) の render の背景適用がライブラリ側の 1 経路を通る。Apple 標準の設定アプリの流儀 (push 中は選択色が残り、戻りのアニメーションに合わせて解除) を利用者の期待値とする。

## Decision

iOS の押下色のタイミングは UIKit のタッチ遅延に任せず、ライブラリ側で制御する:

- collection view は `delaysContentTouches = false` とし、内部サブクラスの `touchesShouldCancel(in:)` で UISwitch / UISlider (とその子孫) 上のタッチはキャンセルせずコントロールを優先し、それ以外はスクロール開始でキャンセルする
- 押下色は押下状態に入ってから短い遅延の後にフェードインで立ち上げる。遅延中にキャンセルされたら何も出さず、遅延中に選択された (速いタップ) 場合は待たずに塗り始める
- 押下色の復帰は、選択を経て解除されるときはフェードアウトし、選択されずにキャンセルされたとき (スクロール開始) はフェードアウトせずに戻す (遅延中のキャンセルは何も出さない。フェードイン進行中のキャンセルで進行中のフェードが完了してから戻ることは許容する)
- 選択の解除はタップ直後に行わず、短い猶予内に画面が隠れ始めれば (push 遷移のように `viewWillDisappear` が届くもの) 遷移中は選択を残す
- 戻り際 (`viewWillAppear`) は短い待ちの後、通常の pop なら遷移中にフェードアウトを開始し、インタラクティブな pop (エッジスワイプ) は完了して戻ったときだけ解除する
- 遷移しないタップ (pageSheet で開く Picker 系のシート提示を含む) は猶予後に解除する
- 押下色の表示中は render の背景適用が平常色で上書きしない (`applyRenderedBackgroundColor` 経由に統一)。Cell の再利用時は押下色の予約・表示状態をリセットする

遅延・フェード・猶予の秒数は実機の体感で決めた値であり、ライブラリ内の定数 (公開 API ではない) として持ち、テストでは固定しない。

## Alternatives Considered

- **タッチ遅延だけを切る (`delaysContentTouches = false` + 常にキャンセル)**: 押下色は即時に出るが、Switch の反応がばらつき鈍くなる。実機で不採用
- **Switch 上のドラッグを縦横の向きで判定してスクロールに振る**: Apple 標準の設定アプリに近づくが、オーナー判断で「スイッチ上のスクロール制御は不要」とし、スイッチ優先で確定
- **フェードインだけ足して解除は即時のまま**: 立ち上がりを緩めても解除が即時のため、フェードインの途中で消え始めて一瞬で終わる。実機で不採用
- **戻り際の解除を遷移完了後 (`viewDidAppear`) に行う**: 戻り切ってから消え始めるため遅すぎる。遷移中に開始する形を採用
- **既定のタッチ遅延 (約 0.15 秒) のまま解除のタイミングだけ変える**: 出典の探索・調整では試していない (押下の即時性が先に確認されたため)。本 ADR の前提が崩れた場合の候補として残る

## Consequences

- 正: 速いタップでも指を置いた直後に押下色が出て、push 中は残り、戻りに合わせて消える。Android のリップルと同じ「押した瞬間に反応する」体感になる
- 正: スクロールを始めるタッチでは押下色が出ず、タップで値が変わる Cell でも押下色が出る
- 負: 押下色の見え方が UIKit 既定から離れ、遅延・フェード・猶予の秒数という「体感で決めた値」をライブラリが持つ。OS の遷移アニメーションが変わると値の再調整が要る
- 負: 全 Cell 種の render の背景適用がライブラリ側の 1 経路を通る前提を置くため、新しい Cell 種を足すときはこの経路を通す必要がある
- 負: Switch / Slider の上から始めた縦ドラッグではリストがスクロールしない (Apple 標準の設定アプリと異なる)

## Revisit When

- 新しい OS で戻りの遷移中の消え方や速いタップの立ち上がりが体感で崩れたとき (秒数の再調整、または前提の見直し)
- Switch 上からのスクロールが利用者から求められたとき (向き判定の案を再検討)
- 押下色の描画経路が `configurationUpdateHandler` 集約でなくなったとき

---
出典: kasane/changes/ios-cell-highlight-delayed-on-touchdown/exploration.md (探索で分かった仕組み・決定事項) / kasane/changes/ios-cell-highlight-delayed-on-touchdown/summary.md (最終状態・採用値と根拠) / kasane/changes/ios-cell-highlight-delayed-on-touchdown/evidence.md

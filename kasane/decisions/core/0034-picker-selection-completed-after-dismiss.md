---
id: 0034
title: モーダル提示する選択面 (PickerCell / DatePickerCell) は「確定して閉じ切った後」を値付きの callback で知らせ、MAUI の SelectedCommand はその時点で実行する
status: proposed
date: 2026-09-18
---

## Context

AiForms.Maui.SettingsView から KsSettingsView.Maui へ移行した利用者アプリで、`PickerCell` の選択確定後にダイアログを出す導線が iOS で動かなくなった。

- KsSettingsView iOS の選択面は UIKit のモーダル提示 (ページシート) で、閉じ切るまで提示元の `presentedViewController` が残る。この間に利用側が別のモーダル (ダイアログ等) を提示しようとすると UIKit が提示を無視し、利用側の `await` が宙づりになる (例外も出ない)
- 確定 callback (`onSelectionChanged` / `onMultiSelectionChanged`) は選択面が閉じる**前**に届き、MAUI の `SelectedCommand` もその直後に実行される。利用側が「閉じ切った」ことを知る手段はどの層にも無く、dismiss アニメーションの尺を推測した固定待ち (`Task.Delay`) を書くしかなかった
- AiForms の `SelectedCommand` は選択ページの `OnDisappearing` の中で値を書き戻してから実行されていた。すなわち「選択面が消えてからコマンドが来る」意味論であり、移行利用者はこれに依存していた。退行の正体は API の有無ではなく発火タイミングの差である
- iOS で同じ提示競合が起きるのは、モーダル提示を使う PickerCell と DatePickerCell (カレンダー) の選択面。NumberPicker / TimePicker / DatePicker (ホイール) は `inputView` でキーボード位置に出るため提示 VC を作らず、競合しない。Android は全選択面がボトムシートで、ダイアログは Activity の FragmentManager に出るため競合しない
- 選択面の既存契約 (concepts `core/cells/picker-selection-surface.md`): 確定 callback は確定操作の 1 回だけ発火し、非確定 dismiss はどの経路でも発火しない。値の書き戻しは通知時点で行う (maui/ADR-0012)

前提: beta 配信中で、互換経路なしの破壊的変更を受容する方針が既にある (core/ADR-0031 と同じ性格)。「閉じ切りを知りたいが `SelectedCommand` は確定直後に欲しい」利用者は見つかっていない。

## Decision

1. **Native の `PickerCell` モデルに、確定操作で選択面が閉じ切った後に 1 回だけ発火する callback を足す。** 単一 / 複数で既存の対と揃え、確定した値を運ぶ 2 本とする (仮名 `onSelectionCompleted(Int)` / `onMultiSelectionCompleted(Set<Int>)`。名前は提案時に確定する)
   - 値の callback (`onSelectionChanged` 系) の**後**に届き、同じ値を運ぶ。値の callback のタイミング (確定直後・閉じる前) は変えない
   - 非確定 dismiss (Cancel・外側タップ・Back・下スワイプ) ではどの callback も発火しない。既存の保証はそのまま
   - 「閉じ切った後」の定義は「プラットフォームが選択面の dismiss 完了として報告する時点」。iOS は確定時 dismiss の completion、Android はダイアログの dismiss リスナー + 確定済みの控えを起点とする。Android のカレンダーダイアログ (通常の Dialog) だけは window のフェード完了前に報告されるが、Android に提示競合は無いため意図的なプラットフォーム差として受容する (退場完了の検出に公式手段が無く、待つと固定待ちをライブラリに持ち込む)
2. **同じ callback を DatePickerCell にも足す** (値は確定した日付、1 本)。uiStyle に関係なく発火させ、カレンダーは dismiss 完了、ホイールは inputView の非表示完了を起点にする。対象は iOS でモーダル提示し提示競合を踏む選択面 (PickerCell / DatePickerCell) に限り、NumberPicker / TimePicker (iOS は inputView 提示で競合しない) には足さない
3. **MAUI の `SelectedCommand` は、この閉じ切り通知を bridge 経由で受けた時点で実行する。** `SelectedIndex` / `SelectedIndices` の書き戻しは今までどおり確定直後に行い、コマンドだけを閉じ切り後へ移す。新しい公開 API は足さない

「値はバインディング (確定直後)、次の行動はコマンド (閉じ切り後)」と、役割ごとにタイミングを 1 つずつにする。

## Alternatives Considered

- **MAUI に別の閉じ切り通知 (コマンドまたはイベント) を新設し、`SelectedCommand` は確定直後のまま**: 却下。公開 API と使い分けの説明が増え、移行利用者は新 API へ書き換える必要がある。確定直後のコマンドを必要とする利用者は見つかっていない
- **Native の確定 callback (`onSelectionChanged` 系) 自体を dismiss 完了へ移す**: 却下。Native 利用者の Store 更新と MAUI の値の書き戻しが遅れ、閉じるアニメーション中に行の表示 (valueText) が古い値のまま残る
- **閉じたら毎回知らせる callback + 確定フラグ**: 却下。既存の保証「非確定 dismiss はどの経路でも callback を発火しない」の書き換えが要り、iOS の対話的 dismiss (下スワイプ) の完了検出を Picker に新設する必要がある。キャンセル通知が必要になれば callback の追加で足せる (追加的な拡張)
- **値を運ばない 1 本の callback**: 却下。公開面は最小だが、閉じ切り時に値を使う利用者が確定 callback で受けた値を自分で保持する必要がある
- **PickerCell だけに足す**: 却下。DatePickerCell (カレンダー) は iOS で同じ提示競合を踏み、同じ利用者が同じ症状に当たる。同じ領域の同じ症状を別の変更に逃がさない
- **選択面を持つ 4 種すべてに足す**: 却下。NumberPicker / TimePicker は iOS で `inputView` 提示のため提示競合が起きず、Android は全種で競合しない。配線の増分が対称性のためだけになる

## Consequences

- 正: 移行利用者の「`SelectedCommand` で次の画面・ダイアログを出す」書き方が、固定待ちなしにそのまま動く
- 正: Swift / Kotlin の直接利用者も、選択後にモーダルを出す導線を推測待ちなしで書ける
- 正: 公開 API の増分は Native の callback (PickerCell 2 本・DatePickerCell 1 本) だけで、MAUI の公開面は変わらない
- 負: KsSettingsView.Maui の既存利用者にとって `SelectedCommand` の実行が dismiss アニメーション分 (約 0.4 秒) 遅れる。閉じる前の実行に依存していた利用者があれば挙動が変わる
- 負: 値の callback と閉じ切りの callback の 2 タイミングが Native 公開面に並び、契約の説明が要る
- 負: bridge の interaction 経路 (iOS delegate / Android listener / MAUI sink) と binding 定義にメソッドが増える
- 負: Android のカレンダーダイアログだけ退場アニメーションの途中で発火する (意図的差異として concepts に記す)
- 補: 提案段階の計測で、iOS のページシート dismiss は UIKit 既定で約 530〜585ms かかり、ライブラリ側に遅延要因は無いと判定した (`kasane/changes/picker-selection-after-dismiss/evidence/dismiss-timing/README.md`)。利用側の固定待ち 500ms が足りなかった観測はこの尺で説明できる

## Revisit When

- キャンセルで閉じたことを知りたい利用者が現れたとき (閉じ切り通知の非確定側を追加的に足す)
- `SelectedCommand` を確定直後に実行したい利用者が現れたとき (別コマンドの新設を再検討)
- iOS の選択面の提示形態がモーダルでなくなったとき (提示競合そのものが消える)
- NumberPicker / TimePicker の iOS 提示が `inputView` からモーダルへ変わったとき (対象範囲を広げる)

---
出典: kasane/changes/picker-selection-after-dismiss/exploration.md (課題 / 動機・調査済みの内容・決定事項)

---
id: 0034
title: モーダル提示する選択面 (PickerCell / DatePickerCell) は「確定して閉じ切った後」を値付きの callback で知らせ、MAUI の SelectedCommand はその時点で実行する
status: accepted
date: 2026-09-19
---

## Context

AiForms.Maui.SettingsView から KsSettingsView.Maui へ移行した利用者アプリで、`PickerCell` の選択確定後にダイアログを出す導線が iOS で動かなくなった。

KsSettingsView iOS の選択面は UIKit のモーダル提示 (ページシート) で、閉じ切るまで提示元の `presentedViewController` が残る。この間に利用側が別のモーダル (ダイアログ等) を提示しようとすると UIKit が提示を無視し、利用側の `await` が宙づりになる (例外も出ない)。確定 callback (`onSelectionChanged` / `onMultiSelectionChanged`) は選択面が閉じる**前**に届き、MAUI の `SelectedCommand` もその直後に実行されるため、利用側が「閉じ切った」ことを知る手段はどの層にも無く、dismiss アニメーションの尺を推測した固定待ち (`Task.Delay`) を書くしかなかった。

AiForms の `SelectedCommand` は選択ページの `OnDisappearing` の中で値を書き戻してから実行されていた。すなわち「選択面が消えてからコマンドが来る」意味論であり、移行利用者はこれに依存していた。退行の正体は API の有無ではなく発火タイミングの差である。

提示競合が起きる範囲はプラットフォームと提示形態で決まる:

| 選択面 | iOS の提示形態 | 提示競合 |
|---|---|---|
| PickerCell / DatePickerCell (カレンダー) | モーダル提示 (ページシート) | 起きる |
| DatePickerCell (ホイール) / NumberPicker / TimePicker | `inputView` (キーボード位置) | 起きない (提示 VC を作らない) |
| Android の全選択面 | ボトムシート / ダイアログ | 起きない (ダイアログは Activity の FragmentManager に出る) |

選択面の既存契約 (concepts `core/cells/picker-selection-surface.md`) では、確定 callback は確定操作の 1 回だけ発火し、非確定 dismiss はどの経路でも発火しない。値の書き戻しは通知時点で行う (maui/ADR-0012)。

前提: beta 配信中で、互換経路なしの破壊的変更を受容する方針が既にある (core/ADR-0031 と同じ性格)。「閉じ切りを知りたいが `SelectedCommand` は確定直後に欲しい」利用者は見つかっていない。

## Decision

### Native に「確定して閉じ切った後」の値付き callback を足す

`PickerCell` に、確定操作で選択面が閉じ切った後に 1 回だけ発火する callback を単一 / 複数の対で足す — `onSelectionCompleted(Int)` / `onMultiSelectionCompleted(Set<Int>)`。既存の値 callback (`onSelectionChanged` 系) と語幹を揃え、接尾だけを Changed → Completed に変えて「同じ値が後から届く」関係を名前から読めるようにする。

- 値の callback の**後**に届き、同じ値を運ぶ。値の callback のタイミング (確定直後・閉じる前) は変えない
- 非確定 dismiss (Cancel・外側タップ・Back・下スワイプ) ではどの callback も発火しない。既存の保証はそのまま
- 省略可能な引数として足し、既存の呼び出しは変更なしに動く

### 「閉じ切った後」の定義

「プラットフォームが選択面の dismiss 完了として報告する時点」を閉じ切りとする。iOS は確定時 dismiss の completion、ホイールは `inputView` の非表示完了、Android は確定値の控えとダイアログの dismiss 経路の合流を起点にする。

Android のカレンダーダイアログ (通常の Dialog) だけは window のフェード完了前に報告されるが、Android に提示競合は無いため意図的なプラットフォーム差として受容する。退場完了の検出に公式手段が無く、待てば固定待ちをライブラリに持ち込むことになる。

### DatePickerCell にも同じ callback を足す

`DatePickerCell` には確定した日付を運ぶ `onValueCompleted` を 1 本足し、uiStyle (カレンダー / ホイール) に関係なく発火させる。対象は iOS でモーダル提示し提示競合を踏む選択面 (PickerCell / DatePickerCell) に限り、NumberPicker / TimePicker には足さない。

### MAUI の SelectedCommand は閉じ切り通知の受信時点で実行する

bridge は Picker の閉じ切りだけを `pickerCellSelectionCompleted` / `pickerCellMultiSelectionCompleted` の 2 メソッドで中継し、MAUI はそれを受けた時点で `SelectedCommand` を実行する。`SelectedIndex` / `SelectedIndices` の書き戻しは今までどおり確定直後に行い、コマンドだけを閉じ切り後へ移す。MAUI の公開 API は足さない。DatePickerCell の `onValueCompleted` は MAUI に消費者が無いため bridge へ通さない。

「値はバインディング (確定直後)、次の行動はコマンド (閉じ切り後)」と、役割ごとにタイミングを 1 つずつにする。

## Alternatives Considered

### 通知の設計 (何をどの層に知らせるか)

- **MAUI に別の閉じ切り通知 (コマンドまたはイベント) を新設し、`SelectedCommand` は確定直後のまま**: 却下。公開 API と使い分けの説明が増え、移行利用者は新 API へ書き換える必要がある。確定直後のコマンドを必要とする利用者は見つかっていない
- **Native の確定 callback (`onSelectionChanged` 系) 自体を dismiss 完了へ移す**: 却下。Native 利用者の Store 更新と MAUI の値の書き戻しが遅れ、閉じるアニメーション中に行の表示 (valueText) が古い値のまま残る
- **閉じたら毎回知らせる callback + 確定フラグ**: 却下。既存の保証「非確定 dismiss はどの経路でも callback を発火しない」の書き換えが要り、iOS の対話的 dismiss (下スワイプ) の完了検出を Picker に新設する必要がある。キャンセル通知が必要になれば callback の追加で足せる (追加的な拡張)
- **値を運ばない 1 本の callback**: 却下。公開面は最小だが、閉じ切り時に値を使う利用者が確定 callback で受けた値を自分で保持する必要がある

### 対象範囲

- **PickerCell だけに足す**: 却下。DatePickerCell (カレンダー) は iOS で同じ提示競合を踏み、同じ利用者が同じ症状に当たる。同じ領域の同じ症状を別の変更に逃がさない
- **選択面を持つ 4 種すべてに足す**: 却下。NumberPicker / TimePicker は iOS で `inputView` 提示のため提示競合が起きず、Android は全種で競合しない。配線の増分が対称性のためだけになる
- **ホイールは対象外にし、カレンダーだけに足す**: 却下。同じ Cell の同じ callback が uiStyle で発火したりしなかったりするのは契約として説明できない。iOS のホイールで提示競合は起きないが、契約の一様性を優先する

### 名前

- **`onSelectionCommitted`**: 却下。「確定」の意味が強く、確定直後に発火する既存 callback との違い (閉じ切った後) が名前に出ない

### iOS の起点

- **`presentationControllerDidDismiss` と `viewDidDisappear` で閉じ切りを一律に拾い、確定済みフラグで振り分ける**: 却下。キャンセル通知を足さない範囲では検出経路が増えるだけで、「毎回通知 + フラグ」と同じ構造になる
- **`PickerCellView` 側で `presentedViewController == nil` をポーリングする**: 却下。時刻依存の推測を利用側からライブラリ側へ移すだけで、脆さは変わらない
- **ホイールは `resignFirstResponder()` の次の runloop で発火する**: 却下。`inputView` の下がるアニメーションの途中で発火し、「閉じ切った後」の契約にならない
- **ホイールは `UIView.animate` の completion を自前で使う**: 却下。`inputView` のアニメーションは UIKit が駆動しており、自前のアニメーションと同期する手段が無い

### Android の起点

- **選択面の中で `setOnDismissListener` を呼ぶ**: 却下。リスナーは 1 本しか持てず、選択面の提示関数が購読解除のために占有している。上書きすると購読解除が壊れる
- **提示関数にリスナーの多重化を足す**: 却下。汎用の仕組みを 1 用途のために増やす。確定値の控えで足りる
- **選択面が `onStop()` / `onDetachedFromWindow()` を override して発火する**: 却下。Dialog のライフサイクル hook はホストの Activity 停止でも呼ばれ、「利用者が閉じた」と区別できない

### bridge と MAUI の配線

- **DatePicker も bridge で中継して MAUI 側で捨てる**: 却下。境界の面積が増えるだけで利用者に価値が無い。将来 MAUI DatePicker にコマンドを足すときに一緒に足せる
- **既存の `pickerCellSelectionChanged` に「閉じ切り済み」フラグを足す**: 却下。1 メソッドが 2 回呼ばれる形になり、MAUI 側の書き戻しがフラグ分岐を持つ。1 事象 1 メソッドの流儀 (maui/ADR-0002) から外れる
- **changed 通知を受けた MAUI 側で dismiss 尺だけ遅延させる**: 却下。推測待ちをライブラリに持ち込むだけで、閉じ切りの事実に基づかない
- **completed 通知でも値を書き戻す**: 却下。同じ値の二重書き戻しになり、changed と completed の責務が曖昧になる
- **確定通知で項目を控え、閉じ切り通知で控えを引数にする (確定時 snapshot)**: 却下。controller に「控えて消費する」状態と、cellID・種類・値が食い違う閉じ切り通知の扱いの規則が増える。守る対象 (窓の間のアプリ自身の状態変更) はアプリの判断であり、ライブラリの契約にする根拠が無い

## Consequences

- 正: 移行利用者の「`SelectedCommand` で次の画面・ダイアログを出す」書き方が、固定待ちなしにそのまま動く
- 正: Swift / Kotlin の直接利用者も、選択後にモーダルを出す導線を推測待ちなしで書ける
- 正: 公開 API の増分は Native の callback (PickerCell 2 本・DatePickerCell 1 本) だけで、MAUI の公開面は変わらない
- 負: KsSettingsView.Maui の既存利用者にとって `SelectedCommand` の実行が選択面の dismiss アニメーション分だけ遅れる。閉じる前の実行に依存していた利用者があれば挙動が変わる
- 負: 値の callback と閉じ切りの callback の 2 タイミングが Native 公開面に並び、契約の説明が要る
- 負: bridge の interaction 経路 (iOS delegate / Android listener / MAUI sink) と binding 定義にメソッドが増える
- 負: Android のカレンダーダイアログだけ退場アニメーションの途中で発火する (意図的差異として concepts に記す)
- 負: 将来 NumberPicker / TimePicker やキャンセル通知へ閉じ切り通知を広げるときも、ここで選んだ起点の構造 (iOS はモーダル / 入力面それぞれの非表示完了、Android は確定値の控えと既存の dismiss 経路の合流) を踏襲することになる
- 補: iOS のページシート dismiss の実時間は UIKit 既定の尺どおりで、ライブラリ側に遅延要因は無いと判定した (Simulator と実機の計測は `kasane/changes/archive/2026-09-19-picker-selection-after-dismiss/evidence/dismiss-timing/README.md`)

## Revisit When

- キャンセルで閉じたことを知りたい利用者が現れたとき (閉じ切り通知の非確定側を追加的に足す)
- `SelectedCommand` を確定直後に実行したい利用者が現れたとき (別コマンドの新設を再検討)
- iOS の選択面の提示形態がモーダルでなくなったとき (提示競合そのものが消える)
- NumberPicker / TimePicker の iOS 提示が `inputView` からモーダルへ変わったとき (対象範囲を広げる)

---
関連: maui/ADR-0012 (interaction の書き戻し値の輸送規約) / maui/ADR-0002 (Store 操作と 1:1 の 1 事象 1 メソッド) / core/ADR-0031 (beta 配信中は互換経路なしの破壊的変更を受容する)
出典: kasane/changes/archive/2026-09-19-picker-selection-after-dismiss/exploration.md (課題 / 動機・調査済みの内容・決定事項) / kasane/changes/archive/2026-09-19-picker-selection-after-dismiss/design.md (Decisions 1・3〜7)

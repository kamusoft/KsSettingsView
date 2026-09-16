# Exploration: picker-selection-after-dismiss

## 課題 / 動機

`../ColorAnalyzer` の移行作業 (`../ColorAnalyzer/kasane/changes/migrate-settingsview-to-kssettingsview`) で、`PickerCell` の選択確定後にアプリ側がダイアログを出す導線が **iOS で動かなくなった**。AiForms.Maui.SettingsView では動いていた導線であり、移行による退行として観測された。

原因は KsSettingsView の不具合ではなく**提示形態の違い**にある。AiForms の選択面は MAUI ページの push で、presented view controller を作らない。KsSettingsView iOS の選択面は UIKit モーダルなので、閉じ切るまで `presentedViewController` が非 nil のまま残る。この間に利用側が別のモーダル (ダイアログ等) を出そうとすると、UIKit が提示を無視する。

**利用側からは「選択面が閉じ切った」ことを知る手段が無い。** `SelectedCommand` は dismiss の**前**に発火するため、これに乗っても競合を避けられない。結果として利用側は dismiss アニメーションの尺を推測した固定待ち (`Task.Delay`) を書くことになり、端末速度やアニメーション設定に依存する脆い実装を強いられる。

**「選択面が閉じたタイミングで選択値を流す」通知をライブラリ側が公開すれば、この推測が不要になる** (`../ColorAnalyzer` のオーナー判断)。

### 調査済みの内容 (同じ調査を繰り返さないための記録)

移行側で実測済みの連鎖。すべて実装ソース / 逆コンパイル結果に基づく。

- **KsSettingsView iOS の提示**: `ios/Sources/KsSettingsViewUI/PickerCellView.swift:109` が `KeyWindowResolver.topPresentedViewController()` に対して `presenter.present(nav, animated: true, completion: nil)` を実行する
- **確定と dismiss の順序**: `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:266-272` で `onSingleDone?(row)` を呼んだ**直後**に `dismissModal()` (`:312-316` = `dismiss(animated: true)`) が走る。つまり確定コールバックは常に「閉じる前」に届く
- **completion を上へ通していない**: Swift 側は `present(..., completion: nil)` / `dismiss(animated: true)` で、閉じ切りを bridge より上へ知らせる経路が無い
- **MAUI 側にも API が無い**: `maui/KsSettingsView.Maui/` に選択面クローズを通知する API は存在しない (`PickerCell` にあるのは `SelectedCommand` 系のみ)
- **AiForms の提示** (比較対象): `AiForms.Settings.PickerCell` が `Navigation.PushAsync(new PickerPage(this))` を実行する。push されたページは presented VC を作らないため、利用側が掴む「最前面の VC」は常にウィンドウの root になり競合しなかった
- **利用側の症状** (参考): `AiForms.Dialogs.iOS.DialogHelpers.RootViewController` は key window の root から `PresentedViewController` を辿れるだけ辿った先を返す。dismiss アニメーション中はこれが消えかけの選択面を返し、そこへの `present` が無視される。さらに `ReusableDialog.ShowAsync()` は `return await tcs.Task` で待つため、ダイアログが出ないと **await が宙づりになり例外も出ない** (最も切り分けにくい失敗の形)
- **Android では起きない**: Android 側のダイアログは Activity の FragmentManager に出すため、選択面の提示状態と競合しない。iOS 固有

### 当面の回避策 (利用側) と、そこで見つかった二次的な観測

`../ColorAnalyzer` では固定待ち (`Task.Delay`) の値を伸ばして凌ぐ判断をした。dismiss が完了すれば `presentedViewController` は nil に戻るため、アニメーションの尺を超えて待てば通る。ただし**どの値なら足りるかを保証できない** (端末速度・アニメーション設定・dismiss アニメの尺に依存) ため、恒久策としては弱い。

**この値決めの過程で、選択面の dismiss が UIKit 既定より遅いことを示す観測が出た。** 本起票の主題 (閉じ切り通知の追加) とは別の課題だが、同じ場所の話なので記録する。

- **実測** (iPhone 11 実機 / iOS 18 系 / Debug 構成、選択面を開いて 1 項目選ぶ導線を毎回アプリ再起動後の 1 回目で試行): 待機 150ms ❌ / 500ms ❌ (2 回とも) / 625ms ○ / 750ms ○ / 1000ms ○ / 2000ms ○
- **500ms 版では、モーダルが閉じてから体感で分かるラグを置いてダイアログが表示される**。UIKit 既定の modal dismiss (0.3-0.5 秒) で閉じ切っているなら、500ms 待ってなお体感ラグが残るのは説明がつかない
- **移行前 (AiForms.Maui.SettingsView、選択面はページの push) ではこのラグは発生していなかった**。利用側のダイアログ実装は移行で一切変更していないため、**ダイアログ側の生成が重いという説明は成り立たない**
- **静的な読解では尺を伸ばす要素が見つかっていない**: `modalPresentationStyle` / `modalTransitionStyle` / `transitioningDelegate` / sheet 系の設定はいずれも未指定 (iOS 既定の `.automatic` = pageSheet)。`dismissModal()` は `dismiss(animated: true)` 一発で、遅延指定もカスタムアニメーション尺も二段階 dismiss も無い
- **つまりコードの読解と実機の観測が矛盾している。** 未解明であり、実測 (dismiss 開始から完了までの実時間取得) で切り分ける必要がある。調査範囲は Swift 層だけでなく bridge 層・MAUI 層・Handler 側、およびホスト側でメインスレッドが詰まっていないかまで広げるべき (閉じるアニメーション自体が既定でも、メインスレッドが詰まれば体感は遅くなる)

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- **公開する形**: 既存の `SelectedCommand` の発火タイミングを「閉じた後」に変えるか、別イベント / 別コマンドとして足すか。前者は既存利用者の挙動を変えるため互換性の判断が要る (`SelectedCommand` に依存して「閉じる前」に動いている利用者がいる可能性)
- **Android との対称性**: Android 側は競合しないが、API としては両プラットフォームで同じ意味で発火すべきか。iOS だけ意味のあるイベントにするのは筋が悪い可能性がある
- **他の Cell への波及**: `concepts/core/cells/` に `picker-selection-surface.md` / `number-picker-selection-surface.md` / `time-picker-selection-surface.md` / `date-picker-selection-surface.md` の 4 つが既にあり、いずれも「行タップで開く**モーダルな**選択 UI」と定義されている (`number-picker-selection-surface.md:72`)。**同じ提示競合は 4 種すべてで起こり得る**ため、通知は `PickerCell` 限定ではなく選択面を持つ Cell に共通の契約として設計する必要がある
  - 4 種とも「確定 callback は確定操作の 1 回だけ発火し、非確定 dismiss はどの経路でも発火しない」という契約を持つ。**この契約は「閉じる前に発火する」とは別の軸**なので、閉じ切り通知を足しても既存契約とは両立し得る
  - 非確定 dismiss (キャンセル・外側タップ・Back・下スワイプ) でも選択面は閉じる。**閉じ切り通知をどう扱うかは確定/非確定の両方で定義が要る** — 利用側が「閉じ切ってから何かを出す」目的で使うなら、非確定で閉じたときも知りたいはず
- **bridge の経路**: `dismissModal()` の completion から MAUI 側へ上げる経路をどう通すか (既存の accessory / snapshot の配線に乗るか、新設か)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (公開 API を足すため S 級には収まらない見込み)

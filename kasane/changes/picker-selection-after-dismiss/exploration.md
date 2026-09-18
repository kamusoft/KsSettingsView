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

- (2026-09-18, 論点 1) **MAUI の `SelectedCommand` は選択面が閉じ切った後に実行する。値の書き戻し (`SelectedIndex` / `SelectedIndices`) は今までどおり確定直後**。根拠: AiForms の `SelectedCommand` は選択ページの `OnDisappearing` で発火しており「閉じた後」の意味論だった (`../AiForms.Maui.SettingsView/SettingsView/Pages/PickerPage.xaml.cs:117-140`)。移行利用者の書き方がそのまま動き、公開 API が増えず、「値はバインディング・次の行動はコマンド」と役割ごとにタイミングが 1 つになる。既存利用者への挙動変化 (コマンドが約 0.4 秒遅れる) は beta 配信中の破壊的変更として受容する (core/ADR-0031 と同じ性格)
  - 却下 B「別の閉じ切り通知を新設し `SelectedCommand` は現状維持」: API と使い分けの説明が増える。「閉じ切りを知りたいがコマンドは確定直後に欲しい」利用者は見つかっていない
  - 却下 C「Native の確定 callback ごと閉じ切り後へ」: 閉じるアニメーション中に行の表示 (ValueText) が古い値のまま残る

- (2026-09-18, 論点 2) **Native の Cell モデルに「確定して閉じ切った後」に 1 回だけ発火する callback を足す**。単一 / 複数で既存の対と揃え、値を運ぶ 2 本 (仮名 `onSelectionCompleted(Int)` / `onMultiSelectionCompleted(Set<Int>)`)。値の callback (`onSelectionChanged` 系) の後に届き同じ値を運ぶ。非確定 dismiss ではどの callback も発火しない (既存の保証文はそのまま)。iOS は確定時 dismiss の completion、Android はボトムシートの `OnDismissListener` + 確定済みフラグが起点。bridge はこれを MAUI へ通し、`SelectedCommand` を実行する (論点 1)
  - 却下 (a)「閉じたら毎回知らせる + 確定フラグ」: 保証文「非確定 dismiss は callback を発火しない」の書き換えが要り、iOS の対話的 dismiss (下スワイプ) の完了検出を Picker に新設する必要がある。キャンセル通知は後から callback 追加で足せる (追加的)
  - 却下 (c)「既存の確定 callback を閉じ切り後へ移す」: Native 利用者の Store 更新が遅れ、閉じるアニメーション中に行の表示が古いまま残る
  - 却下「値を運ばない 1 本」: 公開面は最小だが、閉じ切り時に値を使う利用者が自分で値を保持する必要がある

- (2026-09-18, 論点 3) **対象は PickerCell と DatePickerCell の 2 種** (iOS でモーダル提示し提示競合を踏む選択面)。DatePickerCell は uiStyle に関係なく発火させる (カレンダーは既存の dismiss 完了通知、ホイールは inputView の非表示完了が起点。詳細は propose で設計)。Android も同じ意味で発火する (競合はしないが契約は対称)。MAUI で消費するのは PickerCell の `SelectedCommand` のみで、DatePicker の MAUI 面にコマンドは無いため bridge へ通す必要はない
  - 却下「PickerCell だけ」: DatePicker カレンダーの同じ競合を別 change に逃がす形になる
  - 却下「選択面 4 種すべて」: Number / Time は iOS で inputView 提示のため競合せず、配線の増分が対称性のためだけになる

- (2026-09-18, 論点 4) **「dismiss が UIKit 既定より遅い」観測の切り分けをこの change に同梱する**。新しい閉じ切り callback と確定 callback の時刻差で dismiss の実時間を実機計測する (追加の仕掛けは不要)。原因がライブラリ側なら修正も同梱、ライブラリの外 (ホストアプリのメインスレッド・利用側実装) と判明したら計測結果を記録して修正は追わない
  - 仮説 (静的読解、未検証): 確定直後の値の書き戻しが MAUI → bridge → iOS Store `replaceCell` と流れ、iOS 側は該当行ではなくセクション単位の reload (`KsSettingsViewController.swift` の `reloadSections`) で反映する。dismiss アニメーション開始と同じフレームでメインスレッドに乗るため、セクションが大きいと閉じる動きが重くなる可能性がある
  - 却下「計測だけ同梱」「別 change に切り離す」: 同じ計測器を別に組み直すことになり、隣接する同じ領域の課題を逃がす形になる

- (2026-09-18, 論点 4 の顛末) 提案段階で一時パッチにより計測 (`evidence/dismiss-timing/README.md`)。Simulator で Native 直接 546ms / MAUI 経由 533ms、確定 callback の処理は最大 7ms。ライブラリ側の遅延要因は無く、UIKit のページシート dismiss がもともと約 545ms かかることで「500ms で足りず 625ms で通る」観測が説明できる。セクション reload 仮説は棄却。修正の分岐は提案から外し、実装フェーズは実機 1 系列の確認のみ

## ADR 候補 (作成済み: core/ADR-0034 (proposed) / 未起票: なし)

- core/ADR-0034 `kasane/decisions/core/0034-picker-selection-completed-after-dismiss.md` — 論点 1・2 の決定を 1 本にまとめたドラフト

## 未決の論点

- 閉じ切り callback の**名前** (仮: `onSelectionCompleted` / `onMultiSelectionCompleted` / DatePicker は `onDateCompleted` 相当)。propose で確定する
- DatePicker **ホイール側**の起点 (inputView の非表示完了をどう拾うか: キーボード非表示の完了通知か、`resignFirstResponder` 後の次フレームか)。propose の design で決める
- Android の**確定済みフラグ**の持ち方 (シートの dismiss リスナーは確定 / 非確定を区別しないため、確定経路で立てるフラグと dismiss 完了の合流)。実装詳細
- iOS で `SelectedCommand` が閉じ切り後に動くようになったとき、**Android では確定直後との差がほぼ無い**が同じ経路 (閉じ切り後) に揃えること自体は決定済み。テストで両プラットフォームの順序 (値の callback → 閉じ切り callback) を固定する
- DatePicker の MAUI 面: コマンドが無いため bridge へは通さない。将来 `DateChangedCommand` のようなものを足すなら閉じ切り後に揃える (この change では扱わない)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: L (2026-09-18 オーナー確定)

- 触る能力: core 契約 (concepts 2 本: picker / date-picker selection-surface)、iOS UI (Picker の dismiss completion・DatePicker カレンダー / ホイールの 2 経路)、Android UI (Picker シート・Date シート・カレンダーダイアログ)、bridge (iOS delegate + Android listener + binding ApiDefinition)、MAUI (`SelectedCommand` の実行時点の移動・controller・テスト)。4 ドメインを横断し、プラットフォーム間で同じ意味に揃える設計判断を含む
- 公開 API の変更: Native に callback を足す (追加)。MAUI は公開面不変だが `SelectedCommand` の実行タイミングが変わる (beta の破壊的変更として受容)
- 可逆性: 利用者が閉じ切り後の意味論に依存し始めると戻せない。ADR-0034 で固定
- UI の有無: 見た目の変更なし (mock 不要)。実機での実時間計測が検証に入る
- 実体は各層とも小さいが、能力横断 + 公開契約 + 遅延調査の同梱で M に収めるには重い。迷ったら 1 段上で L

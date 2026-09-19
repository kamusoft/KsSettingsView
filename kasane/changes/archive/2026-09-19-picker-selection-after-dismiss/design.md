# Design: picker-selection-after-dismiss

## Context

選択面 (PickerCell / DatePickerCell) の確定 callback は選択面が閉じる前に届き、閉じ切ったことを知る手段がどの層にも無い。iOS のモーダル提示では閉じ切る前の別モーダル提示が UIKit に無視されるため、利用側は固定待ちを書いていた。core/ADR-0034 (proposed) が「確定して閉じ切った後の値付き callback を Native に足し、MAUI の `SelectedCommand` はそれを受けて実行する」を決めている。本 design はその配線を各層でどう成立させるかを決める。

現状の配線 (scout の棚卸し 2026-09-18):

- iOS Picker: `PickerListViewController` が確定経路で `onSingleDone` / `onMultiDone` を呼んだ直後に `dismissModal()` (completion を受ける口が無い)。`PickerCellView` が VC の closure を Cell の callback へ結線する
- iOS DatePicker: カレンダー (`DatePickerCalendarSheetController`) は確定・キャンセル・下スワイプの全経路で `onDismissed` を dismiss 完了時に発火済み (用途は参照解放のみ)。ホイールは `handleWheelsDone()` が `resignFirstResponder()` → `onValueChanged` の順で、inputView の非表示完了を拾う仕掛けは無い (キーボード通知の購読はリポジトリ内にゼロ)
- Android: 全選択面は `Dialog.showAnchoredTo(anchor, onDismissed)` (`HostAnchoredDialog.kt`) 経由で表示され、この関数が `setOnDismissListener` を占有している。各 ViewHolder が `onDismissed` に参照解放を渡している
- bridge: iOS は `KsBridgeInteractionDelegate` (@objc protocol) + `KsBridgeInteractionRelay`、Android は `KsBridgeInteractionListener` + relay。binding は iOS が手書き (`ApiDefinition.cs`)、Android は .aar からの自動生成
- MAUI: `KsSettingsController.PickerCellSelectionChanged / MultiSelectionChanged` が書き戻しの直後に `PickerCell.NotifySelectionCompleted(mode)` で `SelectedCommand` を実行。テスト `PickerSelectedCommandTests.cs` は全ケース「値通知 = コマンド実行」を前提にしている

## Goals / Non-Goals

Goals:
- PickerCell (単一 / 複数) と DatePickerCell に、確定して閉じ切った後に 1 回だけ発火する値付き callback を両プラットフォームで同じ意味で足す
- MAUI の `SelectedCommand` を閉じ切り通知の受信時点で実行し、値の書き戻しは確定直後のまま保つ
- 非確定 dismiss ではどの callback も発火しないという既存の保証を維持する
- 実機で dismiss の実時間が Simulator の計測 (UIKit 既定の尺) と一致することを確かめる

Non-Goals: proposal.md の Non-Goals に従う。

## Decisions

### Decision 1: callback の名前は既存の値 callback と対にし、「Completed」で「閉じ切った後」を表す

**採用案:**
- iOS `PickerCell`: `onSelectionCompleted: (@Sendable (Int) -> Void)?` / `onMultiSelectionCompleted: (@Sendable (Set<Int>) -> Void)?`
- Android `PickerCell`: `onSelectionCompleted: ((Int) -> Unit)?` / `onMultiSelectionCompleted: ((Set<Int>) -> Unit)?`
- iOS `DatePickerCell`: `onValueCompleted: (@Sendable (Date) -> Void)?`、Android `DatePickerCell`: `onValueCompleted: ((LocalDate) -> Unit)?`
- bridge: `pickerCellSelectionCompleted(cellID:index:)` / `pickerCellMultiSelectionCompleted(cellID:indices:)` (iOS delegate / Android listener / iOS binding とも同名)
- MAUI sink: `PickerCellSelectionCompleted(string cellId, int index)` / `PickerCellMultiSelectionCompleted(string cellId, IReadOnlyList<int> indices)`

**理由:** 既存の対 (`onSelectionChanged` / `onMultiSelectionChanged`、DatePicker は `onValueChanged`) と語幹を揃え、接尾だけを Changed → Completed に変えることで「同じ値が後から届く」関係が名前から読める。MAUI 内部の既存名 `NotifySelectionCompleted` とも一致する。
**代替案:**
- **A: `onSelectionCommitted`** — 「確定」の意味が強く、確定直後に発火する既存 callback との違い (閉じ切った後) が名前に出ない
- **B: `onSelectionSurfaceClosed` (値なし 1 本)** — core/ADR-0034 で却下済み (値を運ばないと利用者が値を保持する必要がある)

### Decision 2: iOS PickerCell の 4 本の公開 init と再構築のすべてで新 callback を受け渡す

**採用案:** 入口一覧を次のとおりとし、すべてに既定 `nil` / `null` の省略可能引数として足す。
- iOS `PickerCell`: プロパティ宣言、internal 全部入り init、公開 init 4 本 (単一 Store / 単一 DSL Binding / 複数 Store / 複数 DSL Binding)、object 射影の init 群 (`PickerCell+ItemProjection.swift` のジェネリック init と String 特殊化、core/ADR-0029 の縁)、`with*` 系の再構築 3 箇所
- iOS `DatePickerCell`: 値 init / Binding init、`withDSLID` / `withStyle` / `withIcon` の転送
- Android `PickerCell` / `DatePickerCell`: data class (`copy` は自動)、object 射影の factory 群 (`PickerCellItemProjection.kt`)、Compose DSL の overload (`compose/InputCellDsl.kt`)

見出しの「4 本」は基本 init の数であり、射影 init を含めた全経路が対象である。
**理由:** sample を含む DSL Binding 経路の利用者が「値は Binding、閉じ切り後は callback」と書けるようにする。Binding 経路に足さないと、閉じ切り通知が欲しい利用者は Store 経路へ書き換えを強いられる。再構築で漏らすと Store の `replaceCell` 後に callback が消える不具合になる。
**代替案:**
- **A: Store 経路の init だけに足す** — DSL 利用者 (Binding) に閉じ切り通知を渡す手段が無くなる
- **B: プロパティを `var` にして後から代入させる** — 既存の Cell モデルは不変値型 (`let`) で、代入型のモデルは他の Cell と流儀が揃わない

### Decision 3: iOS Picker は確定経路だけ dismiss の completion に「値付きの完了 closure」を渡す

**採用案:** `PickerListViewController` に `onSingleCompleted: ((Int) -> Void)?` / `onMultiCompleted: ((Set<Int>) -> Void)?` を init で受け取らせ、`dismissModal(completion:)` を completion 付きに拡張する。確定経路 (候補タップ / 完了ボタン) は値 callback を呼んだ後に completion で completed を呼ぶ。キャンセル経路は completion を渡さない。`PickerCellView` が VC の closure を Cell の新 callback へ結線する。
**理由:** 確定時の dismiss はライブラリ自身が呼ぶので completion が確実に取れる。キャンセル経路に completion を渡さないだけで非確定不発火が構造的に保たれ、対話的 dismiss (下スワイプ) の検出も要らない。
**代替案:**
- **A: `presentationControllerDidDismiss` と `viewDidDisappear` で閉じ切りを一律に拾い、確定済みフラグで振り分ける** — キャンセル通知を足さない今回の範囲では検出経路が増えるだけ。core/ADR-0034 の却下案「毎回通知 + フラグ」と同じ構造
- **B: `PickerCellView` 側で `presentedViewController == nil` をポーリングする** — 時刻依存の推測を利用側からライブラリ側へ移すだけで、脆さは変わらない

### Decision 4: iOS DatePicker は、カレンダーは確定時 dismiss の completion、ホイールはキーボード非表示完了の通知を起点にする

**採用案:**
- カレンダー: `DatePickerCalendarSheetController` に `onDoneCompleted: ((Date) -> Void)?` を足し、`handleDone()` の `dismiss(animated:completion:)` の completion で確定した日付を渡して呼ぶ。既存の `onDismissed` (全経路・参照解放用) は役割を変えない
- ホイール: `handleWheelsDone()` は次の順で進む状態機械にする。(1) `UIResponder.keyboardDidHideNotification` の一回限りの購読を張る、(2) `resignFirstResponder()` を呼ぶ、(3) 値 callback (`onValueChanged`) を呼ぶ (現行どおり resign の後)、(4) 通知が届いたら購読を外して `onValueCompleted` を呼ぶ。(2) が `false` を返した (first responder でなかった) 場合も (3) を先に済ませてから購読を外し `onValueCompleted` を呼ぶ — どの経路でも Changed → Completed の順を崩さない。通知が上限時間 (1 秒) 内に届かなければ購読を外して `onValueCompleted` を呼ぶ (発火しないより早く発火する側に倒す)。Cell の View が再利用・破棄されるときは未消費の購読を外し、発火しない。購読中に再度 Done が来た場合は先の購読を外して新しい 1 回として扱う (Completed は最後の確定に対して 1 回)。購読は Done を押した瞬間にだけ張るので、キャンセルや他画面のキーボード非表示には反応しない
- 値 callback (`onValueChanged`) のタイミングは変えない (カレンダーは Done 押下時、ホイールは resign 直後)

**理由:** カレンダーは既存の completion 経路に相乗りするだけで済む。ホイールは inputView の非表示完了に completion が無く、UIKit がその完了を公式に知らせる手段はキーボード通知しかない。一回限りの購読にすれば「どのキーボードの非表示か」を取り違えない。
**代替案:**
- **A: ホイールは `resignFirstResponder()` の次の runloop で発火する** — inputView の下がるアニメーション (約 0.25 秒) の途中で発火し、「閉じ切った後」の契約にならない
- **B: ホイールは `UIView.animate` の completion を自前で使う** — inputView のアニメーションは UIKit が駆動しており、自前のアニメーションと同期する手段が無い
- **C: ホイールは対象外にし、カレンダーだけに足す** — 同じ Cell の同じ callback が uiStyle で発火したりしなかったりするのは契約として説明できない。iOS では inputView 提示のため提示競合は起きないが、契約の一様性を優先する

### Decision 5: Android は選択面が確定値を控え、ViewHolder が `showAnchoredTo` の `onDismissed` で読み出して発火する

**採用案:** `PickerSelectionSheet` / `DateSelectionSheet` / `DateCalendarDialog` に「確定経路で立てる確定値の控え」(単一: `Int?`、複数: `Set<Int>?`、日付: `LocalDate?`) を持たせ、確定経路は値 callback を呼んだ後に控えを立てて `dismiss()` する。各 ViewHolder は `showAnchoredTo(anchor, onDismissed)` の `onDismissed` で既存の参照解放に加えて控えを読み、立っていれば Cell の completed callback を呼ぶ。キャンセル経路は控えを立てないので発火しない。`setOnDismissListener` の所有は `HostAnchoredDialog` に残す。
**理由:** `setOnDismissListener` は 1 本しか持てず `showAnchoredTo` が購読解除のために占有している。選択面の中で直接呼ぶと購読解除が壊れる。控え + 既存の `onDismissed` 経由なら所有権を動かさずに済み、ボトムシートの hide アニメーション完了後に dismiss リスナーが呼ばれる Material の挙動で「閉じ切った後」になる。
**代替案:**
- **A: 選択面の中で `setOnDismissListener` を呼ぶ** — `showAnchoredTo` の購読解除を上書きして壊す
- **B: `HostAnchoredDialog` にリスナーの多重化を足す** — 汎用の仕組みを 1 用途のために増やす。控え方式で足りる
- **C: 選択面が `onStop()` / `onDetachedFromWindow()` を override して発火する** — Dialog のライフサイクル hook はホストの Activity 停止でも呼ばれ、「利用者が閉じた」と区別できない

補足: `DateCalendarDialog` は BottomSheetDialog ではなく通常の Dialog で、dismiss リスナーは window のフェードアウトを待たずに呼ばれる。「閉じ切り」の共通定義は「プラットフォームが dismiss 完了として報告する時点」とし、Android カレンダーだけフェード前に発火することを意図的なプラットフォーム差として proposal / ADR-0034 / spec に明記する (オーナー確定 2026-09-18。セカンドオピニオン spec-001 の指摘)。退場完了まで待つ案は、通常の Dialog の window アニメーション完了を検出する公式手段が無く固定待ちをライブラリに持ち込むため却下。

### Decision 6: bridge は Picker の閉じ切りだけを 2 メソッドで中継し、DatePicker は中継しない

**採用案:** iOS `KsBridgeInteractionDelegate` と Android `KsBridgeInteractionListener` に `pickerCellSelectionCompleted(cellID:index:)` / `pickerCellMultiSelectionCompleted(cellID:indices:)` を必須メンバとして足す。relay は既存の `*Changed` と同じ正規化 (複数は昇順・重複なし) を通して転送する。`KsBridgePickerCell.makeCell` が Cell の新 callback を relay に結線する。iOS binding の `ApiDefinition.cs` に 2 メソッドを手書き追加、Android binding は自動生成に任せる。DatePicker の `onValueCompleted` は bridge の Cell 生成で結線しない (MAUI に消費者が無い)。
**理由:** MAUI 側が閉じ切りで実行するのは `SelectedCommand` だけであり、消費者の無い通知を interop 境界に増やさない。必須メンバにするのは、既存メンバと同じ流儀で、実装漏れをコンパイル時に検出するため (fake 3 本 × 2 プラットフォームに override を足す)。
**代替案:**
- **A: DatePicker も中継して MAUI 側で捨てる** — 境界の面積が増えるだけで利用者に価値が無い。将来 MAUI DatePicker にコマンドを足すときに一緒に足せる
- **B: 既存の `pickerCellSelectionChanged` に「閉じ切り済み」フラグを足す** — 1 メソッドが 2 回呼ばれる形になり、MAUI 側の書き戻しがフラグ分岐を持つ。maui/ADR-0002 の「Store 操作と 1:1」の流儀 (1 事象 1 メソッド) から外れる

### Decision 7: MAUI は sink の completed メソッドで `SelectedCommand` を実行し、changed メソッドは書き戻しだけにする

**採用案:** `IKsInteractionSink` に `PickerCellSelectionCompleted` / `PickerCellMultiSelectionCompleted` を足し、`KsSettingsController` はそこで `PickerCell.NotifySelectionCompleted(mode)` を呼ぶ。`mode` は届いた completed メソッドの種類で決める (現行と同じ根拠)。`PickerCellSelectionChanged / MultiSelectionChanged` からは `NotifySelectionCompleted` の呼び出しを外し、書き戻しだけを残す。両 OS の `KsBridgeGateway` に転送を足す。`PickerSelectedCommandTests.cs` は「値通知ではコマンド未実行、completed 通知で 1 回実行」へ書き換える。
**理由:** 公開面 (`SelectedCommand` プロパティ) を変えずに実行時点だけを動かせる。引数 (`SelectedItem` / `SelectedItems`) は閉じ切り時点の現値から解決する。`SelectedCommand` は「選択操作の完了通知」であり引数は「Cell の現在の選択」という意味論で現行・AiForms と一貫する。確定から閉じ切りまでの約 0.5 秒にアプリが `ItemsSource` や選択を変えれば変更後の値が渡るが、それはアプリ自身の変更の反映であり、ライブラリが確定時の object を保証する契約は置かない。
**代替案:**
- **A: changed 通知を受けた MAUI 側で dismiss 尺だけ遅延させる** — 推測待ちをライブラリに持ち込むだけで、閉じ切りの事実に基づかない
- **B: completed 通知にも値を書き戻す** — 同じ値の二重書き戻し。エコー抑止 (同値チェック) で無害だが意味が無く、changed と completed の責務が曖昧になる
- **C: 確定通知で項目を控え、閉じ切り通知で控えを引数にする (確定時 snapshot)** — controller に「控えて消費する」状態と、cellId・種類・値が食い違う閉じ切り通知の扱いの規則が増える。守る対象 (窓の間のアプリ自身の状態変更) はアプリの判断で、ライブラリの契約にする根拠が無い (セカンドオピニオン spec-001 の指摘を検討して却下)

### Decision 8: dismiss の遅延はライブラリ側の要因なしと判定し、本 change では修正を持たない

**採用案:** 提案段階で一時パッチ (確定 callback の前後と dismiss completion の時刻) により Simulator で計測した (`evidence/dismiss-timing/README.md`)。Native 直接 546ms / MAUI 経由 533ms (中央値)、確定 callback の処理は最大 7ms で、両者に差が無い。iOS のページシート dismiss は UIKit 既定で約 530〜585ms かかり、利用側の固定待ち 500ms で足りず 625ms で通った観測はこの尺で説明できる。よってライブラリ側の修正は持たず、本 change は閉じ切り通知の追加に専念する。実装フェーズでは新 callback を使い iPhone 11 実機 (iOS 18 系 / Debug) で 1 系列 (候補 47 件、5 回) を取り、Simulator と同じ尺であることを証跡に追記する。
**理由:** 計測を実装後に回すと、原因が分かってから足場を書き足す (凍結規律に反する) 構造になる。提案段階で切り分けたことで、修正の分岐そのものが不要になった。
**代替案:**
- **A: 実装後に計測し、ライブラリ側原因なら修正を同梱する** — 却下。未仕様の変更を実装する入口になる (セカンドオピニオン spec-001 の指摘)。計測を前倒しして解消した
- **B: 値の書き戻しが起こすセクション reload を先回りで行単位に変える** — 却下。計測で確定 callback の処理は最大 7ms と判明し、変える根拠が無い

## Risks / Trade-offs

- ホイールのキーボード非表示通知への依存: 他の入力欄が同時に first responder を持つ状況 (通常は起きない) や、iPad のフローティングキーボードで通知の届き方が変わる可能性。一回限りの購読と `resignFirstResponder()` の戻り値による fallback で最悪でも「発火しない」ではなく「早く発火する」側に倒す
- Android の控え方式: 確定経路で控えを立て忘れると発火しない、キャンセル経路で立てると誤発火する。両経路を Robolectric テストで固定する
- `SelectedCommand` の遅延 (約 0.4 秒) は既存利用者に見える挙動変化。proposal の Impact どおり beta の破壊的変更として受容
- bridge の必須メンバ追加は、interop 境界を実装している外部の C# / Kotlin コードがあれば壊す (現時点で本リポジトリ内の fake のみ)
- 実機 (iPhone 11) の dismiss 実時間は未計測。Simulator と大きく違えば (中央値の差 100ms 超) 計測結果を記録し、対処は別 change で扱う (本 change では修正しない)

## Migration Plan

- 既存の Native 呼び出しは省略可能引数の追加のみで壊れない
- MAUI 利用者への告知: `SelectedCommand` の実行が選択面の閉じ切り後になる旨をリリースノートに書く (release スキルの `## Changes` 下書きで扱う)
- 長命層 (concepts 3 本・maui の interaction 輸送の記述) の追随は蒸留で行う

## Open Questions

- なし (callback の名前は Decision 1 で確定。ホイールの起点・Android の控え方式も確定)

## ADR 候補

- core/ADR-0034 (proposed、探索で起票済み) が Decision 1・3・4・5・6・7 の上位決定 (閉じ切り後の値付き callback・MAUI のコマンド実行時点・対象範囲) を記録している。蒸留時に accepted へ昇格させる。本 design の Decision 個別は ADR-0034 の実現手段であり、別 ADR にはしない
- Decision 4 (ホイールの起点をキーボード非表示通知にする) と Decision 5 (Android の dismiss リスナー所有を `HostAnchoredDialog` に残し控え方式で合流する) は、将来 Number / Time やキャンセル通知へ広げるときに同じ構造を強いる。ADR-0034 の Consequences か蒸留時の concepts への追記で足りるかは ksn-distill が判定する

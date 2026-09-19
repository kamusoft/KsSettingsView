# Proposal: picker-selection-after-dismiss

## Why

AiForms.Maui.SettingsView から KsSettingsView.Maui へ移行した利用者アプリ (`../ColorAnalyzer`) で、`PickerCell` の選択確定後にダイアログを出す導線が iOS で動かなくなった。KsSettingsView iOS の選択面は UIKit のモーダル提示 (ページシート) で、閉じ切るまで提示元の `presentedViewController` が残る。この間に利用側が別のモーダルを提示しようとすると UIKit が提示を無視し、利用側の `await` は例外も出さずに宙づりになる。

確定 callback (`onSelectionChanged` 系) は選択面が閉じる**前**に届き、MAUI の `SelectedCommand` もその直後に実行される。利用側が「選択面が閉じ切った」ことを知る手段はどの層にも無く、dismiss アニメーションの尺を推測した固定待ち (`Task.Delay`) を書くしかない。一方 AiForms の `SelectedCommand` は選択ページの `OnDisappearing` の中で実行されていた。すなわち「選択面が消えてからコマンドが来る」意味論であり、移行利用者はこれに依存していた。退行の正体は API の有無ではなく発火タイミングの差である。

同じ提示競合は iOS でモーダル提示する DatePickerCell (カレンダー) でも起きる。NumberPicker / TimePicker / DatePicker (ホイール) は `inputView` でキーボード位置に出るため提示 VC を作らず、Android は全選択面がボトムシートで、ダイアログとは競合しない。

固定待ちの値決めの過程で、選択面の dismiss が UIKit 既定の尺より遅く見える観測 (500ms 待ってもダイアログが出ない) も出ていたが、提案段階の計測でライブラリ側の要因は無いと判定した (What Changes を参照)。

## What Changes

- **Native (iOS / Android) の PickerCell に「確定して閉じ切った後」の callback を足す** — 確定操作で選択面が閉じ切った後に 1 回だけ発火し、確定した値を運ぶ。単一 / 複数で既存の対と揃えた 2 本 (仮名 `onSelectionCompleted(Int)` / `onMultiSelectionCompleted(Set<Int>)`。名前は design で確定)。値の callback (`onSelectionChanged` 系) の後に届き、同じ値を運ぶ。値の callback のタイミングは変えない。非確定 dismiss (Cancel・外側タップ・Back・下スワイプ) ではどの callback も発火しない (既存の保証を維持)。「閉じ切った後」とは、プラットフォームが選択面の dismiss 完了として報告する時点を指す (iOS はモーダル dismiss の completion、Android はダイアログの dismiss リスナー)。Android のボトムシート 2 種は hide アニメーション完了後、カレンダーダイアログだけは window のフェード完了前に報告されるが、Android には提示競合が無いため意図的なプラットフォーム差として受容する。Android は提示競合が起きないが、契約は両プラットフォームで同じ定義で発火する
- **Native の DatePickerCell にも同じ callback を足す** — 確定した日付を運ぶ 1 本。カレンダー / ホイールの uiStyle に関係なく発火する
- **MAUI の `SelectedCommand` を閉じ切り後に実行する** — bridge が閉じ切り通知を MAUI へ通し、`KsSettingsController` はそれを受けた時点で `SelectedCommand` を実行する。`SelectedIndex` / `SelectedIndices` の書き戻しは今までどおり確定直後に行う。MAUI の公開 API (プロパティ・イベント) は増やさない。DatePicker の MAUI 面にはコマンドが無いため、DatePicker の閉じ切り通知は bridge へ通さない
- **dismiss の遅延観測は提案段階で計測済み** — 確定 callback と dismiss completion の時刻差を一時パッチで取り、Simulator で Native 直接と MAUI 経由を比較した (`evidence/dismiss-timing/README.md`)。両者に差は無く (中央値 546ms / 533ms)、確定 callback の処理は最大 7ms。iOS のページシート dismiss は UIKit 既定で約 530〜585ms かかり、「500ms 待っても足りない」観測はこの尺で説明できる。ライブラリ側の修正は不要で、本 change は閉じ切り通知の追加に専念する。実装フェーズで新 callback を使った実機 (iPhone 11) の確認 1 系列だけを証跡に足す
- **変えないもの**: 確定 callback は確定操作の 1 回だけ発火し非確定 dismiss では発火しないという保証、値の書き戻しは通知時点で行うという輸送規約 (maui/ADR-0012)、選択面の器と提示形態、NumberPicker / TimePicker の callback 面
- 影響能力: settings-view-ios-ui (Picker / DatePicker の閉じ切り通知)、settings-view-android-ui (同)、maui-bridge (iOS delegate / Android listener / binding に閉じ切り通知の口を足す)、maui-cells (`SelectedCommand` の実行時点)

## Non-Goals

- **NumberPickerCell / TimePickerCell への閉じ切り callback** — iOS で `inputView` 提示のため提示競合が起きず、Android は全種で競合しない。配線の増分が対称性のためだけになる (core/ADR-0034 の却下案)
- **キャンセルで閉じたことの通知** — 既存の保証「非確定 dismiss はどの経路でも callback を発火しない」の書き換えと、iOS の対話的 dismiss (下スワイプ) の完了検出の新設を伴う別の設計判断。必要になれば callback の追加で足せる (core/ADR-0034 の Revisit When)
- **MAUI DatePickerCell へのコマンド系プロパティの新設** — 現行の MAUI 面にコマンドが無く、AiForms にも無かった。利用者要望が無い状態で公開面を増やす判断は別に要る
- **Native sample (`samples/ios` / `samples/android`) への閉じ切り callback のデモ追加** — 現行 sample は callback を明示せず Binding 経路で書かれており、公開 API 追加だけなら追随は不要。デモを足すなら handbook `cross/sample-parity.md` により両 OS 同時追加が要り、MAUI sample の `SelectedCommand` デモとの画面対応も決める必要があるため別途判断する。計測 (What Changes 4 点目) は一時コードで行い sample には残さない
- **`skills/` と README の追従** — docs-refresh スキルの責務で、ユーザーの明示依頼により別途行う
- **dismiss の遅延への対処** — 提案段階の計測でライブラリ側の要因は無いと判定済み (`evidence/dismiss-timing/README.md`)。残る体感ラグがあれば利用側 (ダイアログ実装・ホストのメインスレッド) の領分で、このリポジトリの変更では直せない

## Impact

- **利用者可視の変更**: KsSettingsView.Maui で `SelectedCommand` の実行が dismiss アニメーション分 (約 0.4 秒) 遅れる。閉じる前の実行に依存していた利用者があれば挙動が変わる (beta 配信中の破壊的変更として受容。core/ADR-0031 と同じ性格)。選択後にダイアログや画面遷移を行う導線は固定待ちなしで動くようになる
- **公開 API**: Native に callback を追加 (PickerCell 2 本・DatePickerCell 1 本。既存 init への省略可能引数の追加で、既存の呼び出しは壊れない)。MAUI の公開面は不変。bridge の interaction 経路 (iOS delegate / Android listener / binding 定義 / MAUI sink) にメソッドが増える
- **既存の決定との関係**: core/ADR-0034 (proposed) が本提案の決定を記録する。既存の accepted ADR との衝突は無い (maui/ADR-0012 の書き戻し規約・concepts の確定 callback の保証はそのまま)
- **リスク**: (1) iOS の DatePicker ホイールは `inputView` の非表示完了を起点にするため、キーボード非表示の通知経路に依存する。実機で発火順序 (値の callback → 閉じ切り callback) を確認する。(2) Android のシートは dismiss リスナーが確定 / 非確定を区別しないため、確定経路で立てるフラグとの合流を誤ると非確定でも発火する。両経路をテストで固定する。(3) 実機 (iPhone 11) の dismiss 実時間は未計測で、Simulator の結果 (UIKit 既定の尺) からの推定に留まる。実装フェーズで新 callback を使って 1 系列取る
- **`ui/` (brief・モック) は作らない**: 見た目の変更が無い。確認は統合ホストの end-to-end と iOS 実機での導線確認 (handbook `cross/runtime-behavior-verification.md`) で行う
- 長命層: `kasane/concepts/core/cells/picker-selection-surface.md`・`date-picker-selection-surface.md`・`input-cells.md` (callback の一覧とタイミング)、`kasane/concepts/maui/` の interaction 輸送の記述が変わる (蒸留で追随)

## 級: L

core 契約・iOS UI・Android UI・bridge・MAUI の 4 ドメインを横断し、両プラットフォームで閉じ切り通知を同じ意味に揃える設計判断と Native 公開 API の追加を含むため (オーナー確定 2026-09-18)。

domain: cross

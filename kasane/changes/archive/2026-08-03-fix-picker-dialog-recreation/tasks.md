# Tasks: fix-picker-dialog-recreation

## 1. tag 生成・照合の共有基盤

- [x] 1.1 Fragment tag の生成規則を cell.id ベースへ変更し、生成と照合を共有ヘルパーに集約する。符号化は任意の id 文字列と世代を曖昧なく分離できる形式とする (世代を id より前に置く固定書式等 — 末尾サフィックス剥がしは `id = "foo.r1"` と `id = "foo"` の世代1を区別できないため不可)。TimePickerCellViewHolder / DatePickerCellViewHolder は同ヘルパー経由で tag を生成する (→ Requirement: 再生成後のピッカーダイアログの完全復元 / 対応付け不能時の dismiss フォールバック)
- [x] 1.2 tag 照合の単体テスト: 通常 tag・世代付き tag・ドット等の区切り文字を含む id・`.r<n>` 風の文字列で終わる id (`"foo.r1"`)・空文字 id・Unicode を含む id・複数世代、他所の Fragment tag (非対象) の除外 (→ Scenario: id に区切り文字を含む Cell でも対応付けが成立する / 作り直し世代のダイアログも復元される)

## 2. 即時適用経路の追加

- [x] 2.1 TimePickerColorizer に復元用の再 attach 経路を追加する。契約は Fragment View の生成状態に依存しない形とする: 先に lifecycle フックを登録し、現在 View が生成済みなら即時適用も行う。即時適用は window 背景 (onFragmentStarted 相当)・View 階層の着色 (onFragmentViewCreated 相当)・pre-draw 再適用の張り直し・破棄時の解除を含む (→ Requirement: 再生成後のピッカーダイアログの完全復元 2)
- [x] 2.2 DatePickerColorizer に同様の状態非依存の再 attach 経路を追加する (→ 同上)
- [x] 2.3 DatePickerTodayShortcut の再注入経路を追加する (即時適用時に「今日」ボタンを注入し、作り直しコールバックも復元時の文脈で張り直す) (→ Requirement: 再生成後のピッカーダイアログの完全復元 3)

## 3. コンテナ駆動の復元走査

- [x] 3.1 KsSettingsView に one-shot ラッチ (「attach 済み」∧「root 反映済み」の初回のみ) の復元走査を実装する (→ Requirement: 復元走査の駆動条件)
- [x] 3.2 走査本体: FragmentManager から対象 tag の復元 Fragment を列挙 → cell.id で現 root と照合 (適格条件: 同一 id・同型・一意) → 一致時は OK リスナー再登録 + 即時着色 + 「今日」再注入、不一致時は dismiss (→ Requirement: 再生成後のピッカーダイアログの完全復元 / 対応付け不能時の dismiss フォールバック)
- [x] 3.3 複数インスタンス規則: attach 中の KsSettingsView をプロセス内レジストリ (FragmentManager 単位、弱参照) で数え、走査時に複数いれば復元せず dismiss。処理済み (復元または dismiss 済み) の Fragment は claim して後続インスタンスの走査対象から除外する (→ Requirement: 対応付け不能時の dismiss フォールバック — 複数インスタンス時の規則)
- [x] 3.4 重複防御: 復元走査が複数回実行されてもリスナー・着色・「今日」操作が重複しないこと (→ Scenario: 確定操作の発火は1回に保たれる)

## 4. テスト (Robolectric: Activity 再生成シナリオ)

- [x] 4.1 TimePicker: 再生成後の確定で `onValueChanged` が1回発火する / キャンセル・dismiss では発火しない (→ Scenario: TimePicker の値確定が回復する / キャンセルでは発火しない)
- [x] 4.2 DatePicker (Material): 再生成後の確定で発火する / `todayText` 指定時に「今日」操作が再提示され機能する (→ Scenario: DatePicker の値確定が回復する / 「今日」操作が再提示される)
- [x] 4.3 配色: 再生成後のダイアログにテーマ色が再適用される (→ Scenario: 配色が再適用される)
- [x] 4.4 フォールバック: 該当 id なしの root で dismiss され誤発火がない / 既定ランダム id で dismiss になる (→ Requirement: 対応付け不能時の dismiss フォールバック)
- [x] 4.5 駆動条件: attach 前 bind / attach 後 bind の両順序で復元が成立する (→ Requirement: 復元走査の駆動条件)
- [x] 4.6 既存テストの回帰確認 (tag 形式変更の影響: Colorizer 系・TodayShortcut 系・DialogIntegration 系)
- [x] 4.7 統合ケース: 作り直し世代を経た実ダイアログの再生成復元 / uiStyle 変更時の dismiss / 同一 id 複数時の dismiss (→ Scenario: 作り直し世代のダイアログも復元される / uiStyle が変更されていたら閉じられる / 同一 id の候補が複数なら閉じられる)
- [x] 4.8 複数インスタンス: 同一 Activity に2つの KsSettingsView (同一 id の Cell を双方の root に配置) で再生成 → ダイアログが一度だけ閉じられ、いずれの Cell にも発火しない (→ Scenario: 複数の KsSettingsView が存在すると閉じられ、二重発火しない)

## 5. 実環境検証 (concepts/cross/conventions/runtime-behavior-verification.md 準拠)

- [x] 5.1 修正前ビルドのサンプルアプリで症状を再現する (回転後のダイアログで OK が効かない / 配色が既定に戻る / 「今日」ボタンが消える)。再現手順を記録し、スクリーンショット証跡を `ui/verification/` に保存する
- [x] 5.2 修正後ビルドで同一手順により解消を確認する (TimePicker / DatePicker / todayText 付き / 明示 id なし Cell の dismiss フォールバック)。証跡を `ui/verification/` に保存する

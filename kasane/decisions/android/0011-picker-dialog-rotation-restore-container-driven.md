---
id: 0011
title: Material ピッカーダイアログの回転復元はコンテナ駆動の完全復元とし、対応付けは cell.id で行う
status: superseded
date: 2026-08-03
---

## Context

MaterialTimePicker / MaterialDatePicker は DialogFragment であり、表示中に Activity が再生成 (画面回転等) されると FragmentManager の saved state からダイアログ自体は復元される。しかしコードで登録した状態は失われる:

1. `addOnPositiveButtonClickListener` が復元されず、OK を押しても `onValueChanged` が呼ばれない
2. TimePickerColorizer / DatePickerColorizer の着色フック (`FragmentLifecycleCallbacks`) が復元されず、Material 既定配色に戻る
3. DatePickerColorizer 経由で注入される「今日」ボタン (DatePickerTodayShortcut) が消える

つまり「見た目は出ているが値確定が効かない・配色も崩れたゾンビダイアログ」が残る。

対象範囲はコード確認により **TimePickerCell と DatePickerCell (Material) の2箇所のみ**と確定した。ボトムシート系 (PickerCell / NumberPickerCell / DatePickerCell (Spinner)) は素の `BottomSheetDialog` であり、Activity と共に消滅するためゾンビ化しない (対象外)。

出典: kasane/changes/archive/2026-08-02-timepickercell-color-adjust/review-001.md Minor-1、同 second-opinion-002.md 指摘 #2 (相方 codex は Major 評価)。「着色だけの救済は中途半端 (色は戻るが値確定が効かない)」としてリスナー復元と着色復元を一体で設計する独立変更とするオーナー決定 (2026-08-02) を経て、fix-picker-dialog-recreation の探索 (2026-08-03) で本決定に至った。

## Decision

1. **コンテナ (KsSettingsView) 駆動の完全復元とする**。ViewHolder の bind 駆動ではなく、KsSettingsView が FragmentManager を走査して復元済みピッカーを検出し、「OK リスナー再登録 + 生成済み View への即時着色 + (DatePicker なら)「今日」ボタン再注入」を行う。着色と「今日」ボタンには、既存のフック待ち (`onFragmentViewCreated` / `onFragmentStarted`) とは別の「生成済み View への即時適用」経路を設ける
2. **Fragment → Cell の対応付けは cell.id ベースとする**。Fragment tag を現行の `bindingAdapterPosition` ベースから cell.id ベースへ変更する。世代表現は維持するが、符号化は任意の id 文字列と世代を曖昧なく分離できる形式 (世代を id より前に置く固定書式等) とする — 末尾サフィックス剥がしは `id = "foo.r1"` と世代1の `"foo"` を区別できず誤対応に至り得るため採らない (second-opinion-001 Major による補正、2026-08-03)。対応付けの適格条件は「同一 id・同型・現 root にちょうど1つ」で、満たさない場合は**復元を諦めて dismiss にフォールバック**する
3. **駆動点は「Window に attach 済み」かつ「root 反映済み」の両条件が揃った最初の時点で一度だけ走査する (one-shot ラッチ)**。bind(store) が attach 前にも後にも来得る既存設計 (pendingStore パターン) と同型で、どちらの順序でも動く。復元ゾンビは Activity 再生成の瞬間にしか生まれず KsSettingsView インスタンスも Activity と共に作り直されるため、インスタンスごとに1回で十分
4. **完全復元は単独インスタンス構成のみとする**。attach 中の KsSettingsView をプロセス内レジストリ (FragmentManager 単位) で数え、走査時に同一 FragmentManager 上に複数いれば所有者を一意に決められないため復元せず dismiss する。処理済み Fragment は claim し、複数インスタンスが走査しても1つの Fragment への処理は一度だけとする (2026-08-03 オーナー決定。second-opinion-001 Critical — 同一 id が複数インスタンスの root にあると双方が復元して二重発火する — への対処)

**追補 (2026-08-03, オーナー承認)**: 実装レビュー (review-001 Major) で、復元走査が「再生成で復元された Fragment」と「今まさに表示中の生きた Fragment」を区別しておらず、Activity 再生成と無関係な attach/detach 経路 (別 KsSettingsView の後着 attach 等) から「生きたダイアログの dismiss / 別 root の Cell への値書き込み」が成立し得ることが判明した。対処として Decision 4 の claim に第2の役割を追加する — **表示側 (`TimePickerCellViewHolder` / `MaterialDatePickerPresenter`) が `show()` 直後に自分の Fragment を claim し、生きたダイアログを復元走査の対象から恒久的に除外する**。Activity 再生成で saved state から戻る Fragment は別インスタンスのため claim されず、復元経路はそのまま成立する。claim は「複数走査の排他」と「復元 Fragment と表示中 Fragment の区別」の両方を担い、position ベース tag を却下した failure mode を走査対象の選別側からも塞ぐ (出典: review-001 Major / review-002 蒸留申し送り)。

## Alternatives Considered

- **bind 時の再 attach (ViewHolder 駆動。timepickercell-color-adjust review-001 の最小修正案)** — 却下。構造的欠陥が2つある。(1) 復元されたダイアログの持ち主 Cell が回転後に画面外にいると ViewHolder が bind されず、復元処理が一度も走らない (ダイアログは表示中なのにリスナーが永久に付かない)。(2) Colorizer のフック発火点は `onFragmentViewCreated` / `onFragmentStarted` だが、復元 Fragment の View 生成は Activity 起動中に終わっており bind より前。bind 時のフック再登録では発火済みで着色されない
- **復元検出して dismiss のみ (S 級最小対処)** — 主経路としては却下。整合は取れるが「回転でダイアログと選択中の値が消える」UX 劣化を仕様化してしまう。DialogFragment が回転を生き残るのは Android の標準挙動であり、ライブラリとしてそれを壊さない。ただし cell.id 対応付けに失敗した場合の**フォールバックとしては採用** (安全側の劣化)
- **position ベースの対応付け (現行 tag 流用)** — 却下。回転前後でリスト構成が変わった場合に「別の Cell の `onValueChanged` に値を書き込む」誤対応が起こり得る (データ破壊で最悪の failure mode)。cell.id ベースは対応相手が見つからなければ dismiss に安全に倒せ、アダプタ自身の同一性判定 (`areItemsTheSame` / `getItemId` が cell.id ベース、「表示状態同期の三層分離」) とも一致する
- **複数インスタンスの所有者識別に KsSettingsView の View id を tag へ含める案** — 見送り (Decision 4 の代替案 b)。android:id 設定済みの複数インスタンス構成でも完全復元できるが、tag 形式が膨らみ利用者への id 設定案内が必要になる。稀な構成のためのコスト増と判断し、需要が出た時点の拡張余地として残す
- **FragmentManager 単位で全インスタンスの root を一括照合し候補が一意なら復元する案 (相方提案)** — 却下 (Decision 4 の代替案 c)。id 非衝突なら複数構成でも復元できるが、後着インスタンスの root を待てず「その時点で一意」の誤判定 (誤所有) のリスクが残る

## Consequences

- 正: DSL (Compose) 経路 (`withDSLId` で構造由来の安定 ID に rebind される) および明示 id を付与するアプリでは、回転してもダイアログ・配色・「今日」ボタン・値確定がすべて維持される
- 負: 完全復元が効く条件は「再生成の前後で同じ id であること」。DSL 経路・明示 id 指定・root/Store インスタンスの再生成後保持で成立するが、再生成時に Cell を再構築して既定のランダム UUID が変わる経路では dismiss フォールバックになる (= 現状のゾンビよりは安全だが完全復元は効かない)。ドキュメントで「回転復元には安定 id を推奨」と案内する (second-opinion-001 Minor による表現補正)
- 負: 同一 Activity に複数の KsSettingsView が attach されている構成では一律 dismiss となり、完全復元は単独インスタンス構成 (通常の利用形態) に限られる
- 負: Fragment tag の形式が変わる。Colorizer / TodayShortcut に即時適用経路が増え、保守対象のコードパスが増える
- 正: 復元ロジックがコンテナ側に集約され、ViewHolder は表示時の tag 生成規則を共有するだけで済む

出典: kasane/changes/fix-picker-dialog-recreation/exploration.md (2026-08-03 の探索での決定。対象範囲・bind 駆動の欠陥・failure mode 比較はコード読解による裏取り済み) / 同 second-opinion-001.md (2026-08-03 提案レビューによる補正: tag 符号化の曖昧性解消・id 安定性条件の精緻化・Decision 4 の追加)

# セカンドオピニオン: restore-maui-picker-selected-command (spec-001)

**相方**: codex / **label**: so-spec-restore-maui-picker-selected-command / **日付**: 2026-08-29 / **対象**: `kasane/changes/restore-maui-picker-selected-command/` の proposal / exploration / specs / tasks と実装済み範囲の差分

---

# レビュー結果: restore-maui-picker-selected-command

**日付**: 2026-08-29  
**判定**: **NEEDS_DISCUSSION**

## サマリー

実装済みの通常経路はデルタスペックと概ね一致しており、提示された **MAUI facade 513 tests / 0 failures** も受け入れます。ただし、公開挙動が未決のエッジケースと、未実装 Sample の UI・検証仕様に Major が3件あります。残りの実装前に仕様判断が必要です。

指摘件数: Critical 0 / Major 3 / Minor 3 / Suggestion 0

## 指摘事項

### [🟠 Major] 選択面表示中に SelectionMode が変わった場合の Command 引数が未定義

**該当箇所**: `specs/maui-cells/spec.md:7`、`maui/KsSettingsView.Maui/PickerCell.cs:304`  
**問題点**: 選択面は表示時のモードを保持しますが、`NotifySelectionCompleted()` は通知時点の facade の `SelectionMode` で引数を選びます。表示中に Single→Multiple または逆方向へ変更すると、single 通知で古い `SelectedItems` を渡すなど、利用者が確定した種類と異なる引数になり得ます。spec はこの到達可能な状態を定義していません。  
**推奨修正**: 次のいずれかを仕様として決め、モード変更を挟む Scenario を追加してください。

- 通知メソッドの種類を正とし、single 通知は常に `SelectedItem`、multiple 通知は常に `SelectedItems` を渡す
- 現在モードと一致しない古い通知を無視する
- 選択面表示中のモード変更時に選択面を閉じる

### [🟠 Major] 新規 Sample 画面に必要な UI 仕様がなく、実装者判断へ委ねられている

**該当箇所**: `proposal.md:16`、`exploration.md:71`、`specs/samples-maui/spec.md:33`  
**問題点**: 新しいメニュー項目と画面を追加し、「選択要素列」と「受信回数」を1行で観測させるのは明確な UI 変更です。しかし `ui/` がなく、回数をどの表示フィールドへ出すか、初期表示、行・Section の文言や構成が決まっていません。「既存 Cell の組み合わせなので UI 素材なし」という判断は、Kasane の UI アーティファクト規律とも整合しません。  
**推奨修正**: 残作業へ進む前に、表示方法と目視受け入れ基準を owner と合意し、凍結規律に従った記録経路で `ui/brief.md` または mock 相当を残してください。

### [🟠 Major] 既存入力デモの「同じ顔ぶれを再確定」が決定的に観測できない

**該当箇所**: `specs/samples-maui/spec.md:17`、`tasks.md:31`  
**問題点**: 再確定前後で「直近イベント」の文言が同一になるため、表示やスクリーンショットが変わらない可能性があります。特に tasks の操作順では、顔ぶれ変更後の再確定は完全に同じ表示になり、Command が発火したか判定できません。これは本変更の中心である「値が同じでも完了通知する」の検証穴です。  
**推奨修正**: Scenario の前提と操作手順を、再確定直前の直近イベントが別内容になるよう固定してください。各 OS で操作前後を撮影するか、既存文言を変えずに確認できるカウンタ等の観測点を用意してください。

### [🟡 Minor] iOS の非確定 dismiss が検証範囲から漏れている

**該当箇所**: `specs/maui-cells/spec.md:41`、`tasks.md:18`  
**問題点**: core 契約は iOS の Cancel とページシート標準 dismiss の双方を非確定経路としていますが、参照された iOS テストは `_simulateCancel()` のみです。実経路タスクの「確定せず閉じる」も具体的な閉じ方を定めていません。  
**推奨修正**: Cancel と標準 dismiss を分け、少なくとも iOS の対話的 dismiss で callback・Command が発火しないことを検証対象にしてください。

### [🟡 Minor] 未知 Cell ID テストが複数選択値の不変を検証していない

**該当箇所**: `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:156`  
**問題点**: single・multiple の未知 ID 通知を両方呼んでいますが、確認している値は `SelectedIndex` だけです。`SelectedIndices` だけが誤って変更される退行を検出できません。  
**推奨修正**: single と multiple を分け、後者では初期 `SelectedIndices` と `SelectedItems` が変わらないことも明示的に assert してください。

### [🟡 Minor] 両 OS の証跡ファイル名が衝突する

**該当箇所**: `tasks.md:31`  
**問題点**: `evidence/<画面>-<状態>.png` では iOS と Android の同画面・同状態が同名になり、上書きまたは出所不明になります。  
**推奨修正**: `evidence/<os>-<画面>-<操作前後>.png` のように OS と観測時点を必須要素にしてください。

## アクションプラン

1. 選択面表示中の `SelectionMode` 変更時の契約を決定する。
2. 新規 Sample 画面の UI と観測方法を合意する。
3. 同一選択再確定と iOS dismiss の検証手順を決定的にする。
4. 未知 ID テストと証跡命名を補強する。
5. その後、未実装の Sample・実経路検証へ進む。

制約に従いビルド・テスト実行および `review-002.md` の作成は行っていません。



## 突き合わせ結果

ホスト側の自己レビュー (ksn-propose Step 8 チェックリスト) は、証跡の置き場が規約と違っていた点と、選択要素列プロパティが宙に浮く点の 2 件を検出して修正済みだった。相方の 6 件との突き合わせ:

- **未解決: [Major] 選択面表示中の `SelectionMode` 変更時の Command 引数** — 到達可能性をコードで確認した (`SelectionMode` は `maui/KsSettingsView.Maui/PickerCell.cs:299` の `AffectsSnapshot` に含まれ、変更しても表示中の選択面は閉じない)。公開契約の決定を要するためオーナー判断へ回す
- **採用: [Major] 「同じ顔ぶれを再確定」が決定的に観測できない** — ホスト側の自己レビューでも同じ穴を検出しており、MAUI 固有画面の受信回数表示で解決していたが、パリティ画面側の Scenario が未解決のまま残っていた。Scenario の GIVEN を「別の行の操作で直近イベントを上書きした状態」に変え、tasks の操作手順もそれに合わせた
- **一部採用: [Major] 新規 Sample 画面の UI 仕様がない** — 観測内容が実装者判断に委ねられていた点は採用し、tasks 4.3 に行の構成 (単一・複数の 2 行) と各行の表示内容、文言の拠り所を書き足した。`ui/` 一式の要求は**降格** — ksn-core references/ui-artifacts.md の ui/ は「見た目の正」を固定する仕組みで、対象は新しい視覚デザインを伴う変更である。本画面は既存 Cell の組み合わせのみで新規デザインを含まず、同じく MAUI 固有デモ画面を新設した `kasane/changes/archive/2026-08-12-add-maui-accessory-views/` も ui/ を持たない
- **採用: [Minor] iOS の非確定 dismiss が検証範囲から漏れている** — tasks 5.1 / 5.2 の非確定操作を Cancel と対話的 dismiss に分け、Android 側 (5.3) も Cancel・外側タップ・Back の 3 通りを踏む形にした
- **採用: [Minor] 未知 Cell ID テストが複数選択値の不変を検証していない** — tasks 3.7 として追加した
- **採用: [Minor] 両 OS の証跡ファイル名が衝突する** — 命名を `evidence/<os>-<画面>-<操作>-<前|後>.png` に変更した

なお相方は単一選択の `SelectedCommand` が Sample に demo 行を持たない時点のアーティファクトをレビューしている。オーナー判断により、その後 MAUI 固有画面へ単一選択の行を追加した。

### 未解決分の決着 (2026-08-29)

[Major] 選択面表示中の `SelectionMode` 変更については、オーナー判断により**通知メソッドの種類を正とする**案を採用した。`specs/maui-cells/spec.md` の Requirement 本文で実行引数の根拠を確定通知の種類と定め、Scenario「選択面表示中にモードが変わっても確定した種類の引数を渡す」を追加。実装とテストは tasks 1.3 / 2.2 / 3.8 に起票した。これにより相方の指摘 6 件はすべて決着した (確定 0 / 採用 5 / 一部降格 1 / 未解決 0)。

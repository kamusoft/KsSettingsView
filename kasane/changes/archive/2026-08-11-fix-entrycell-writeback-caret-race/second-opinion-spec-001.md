# セカンドオピニオン: fix-entrycell-writeback-caret-race (spec-001)
**相方**: codex / **日付**: 2026-08-11 / **対象**: 提案一式 (proposal.md / specs/settings-view-android-ui/spec.md / tasks.md、入力素材 exploration.md)
---
# レビュー結果: fix-entrycell-writeback-caret-race

**日付**: 2026-08-11  
**判定**: NEEDS_DISCUSSION  
**指摘件数**: Critical 1 / Major 4 / Minor 1 / Suggestion 0

## サマリー

フォーカス中の stale bind を抑止する方針自体は、実測された原因と整合しています。しかし、フォーカス喪失時に「最後に bind された値」を無条件で戻す契約は、直前のユーザー入力を再び巻き戻し、その値をアプリ状態へ書き戻す新たな競合を作ります。

また、Cell 同一性、IME 状態、入力関連プロパティ変更時の優先順位、実機試験の合格条件が十分に定義されていません。このままでは実装方法によって結果が変わるため、実装前に仕様判断が必要です。指定に従いビルド・テストは実行していません。

## 指摘事項

### [🔴 Critical] blur 再同期が直前の入力を失わせ、古い値を再コミットし得る

**該当箇所**: `kasane/changes/fix-entrycell-writeback-caret-race/specs/settings-view-android-ui/spec.md:29`  
関連: `kasane/changes/fix-entrycell-writeback-caret-race/tasks.md:7`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:184`

**問題点**: 次の到達可能な順序が未規定です。

1. 最後に bind 済みの値が `"a"`
2. ユーザーが `"ab"` を入力し、`TextWatcher` が書き戻しを開始
3. `"ab"` の bind が届く前にフォーカスを失う
4. 仕様どおり最後の bind 値 `"a"` を `setText`
5. 現行構造では装着中の `TextWatcher` が `"a"` を通知し、アプリ状態まで `"a"` に戻る

MAUI 側は変更を dispatcher flush まで集約するため、blur の `"a"` が直前の `"ab"` を上書きした状態で送信され得ます。修正対象と同じ文字欠落を、blur 境界で再導入します。通知を抑止して再同期しても、遅れて届く `"ab"` と blur 時の `"a"` のどちらを最終値とするかが仕様上決まっていません。

**推奨修正**: stale echo・外部プログラム更新・ローカル入力の競合規則を先に決定してください。最低限、以下の Scenario が必要です。

- `"a"` が最後の bind 値で、`"ab"` の書き戻しが未反映のまま blur しても、静穏化後の表示値とアプリ状態から `"b"` が失われない
- プログラム的な再同期では `onTextChanged` を発火させない
- 外部更新を優先するなら、単なる「最後に bind された値」ではなく世代または更新元を識別して stale echo と区別する

この競合方針は実装だけでは決められません。

### [🟠 Major] 「同一 Cell」の判定基準が未定義

**該当箇所**: `kasane/changes/fix-entrycell-writeback-caret-race/specs/settings-view-android-ui/spec.md:12`  
関連: `kasane/changes/fix-entrycell-writeback-caret-race/tasks.md:9`

**問題点**: 同一性が参照同一性、`equals`、`cell.id` のどれか規定されていません。`EntryCell.equals` は `text` を比較するため、実装者が `==` を使うと、まさに text が変化した再バインドを「別 Cell」と判定して `setText` し、不具合が残ります。現行 Adapter と android/ADR-0001 の identity は同一 `cell.id` です。

**推奨修正**: 「同一 Cell = 同じ安定 `cell.id`」と明記し、保持 ID は初回 bind・別 ID bind・`reset()` でどう更新／破棄するかを定義してください。テストも次を分離すべきです。

- 同一 ID・異なる text はフォーカス中に上書きしない
- 異なる ID・同じ text でも別 Cell と判定する
- `reset()` 後の再利用では前 Cell の保留値や ID を持ち越さない

### [🟠 Major] IME desync の解消が Requirement になっていない

**該当箇所**: `kasane/changes/fix-entrycell-writeback-caret-race/proposal.md:9`  
関連: `kasane/changes/fix-entrycell-writeback-caret-race/tasks.md:31`

**問題点**: proposal は IME composing 破壊と入力不能化を修正対象として説明していますが、デルタスペックは text とキャレットしか規定していません。「連続バースト後も入力可能」「日本語変換中の composing が維持される」は tasks にだけあり、Requirement／Scenario へ追跡できません。

したがって、文字列の最終値だけ合うものの InputConnection や composing が壊れる実装でも、仕様上は合格し得ます。

**推奨修正**: IME 継続性を独立 Requirement にし、少なくとも以下を観察可能な契約として追加してください。

- stale な同一 Cell 再 bind 後も入力を継続できる
- 日本語 IME の未確定文字列・変換操作が再 bind だけで確定または破棄されない
- 入力不能化が発生しないことを、修正前と同じ実機手順で確認する

### [🟠 Major] text 保護と入力関連プロパティの即時反映が両立しない場合の優先順位がない

**該当箇所**: `kasane/changes/fix-entrycell-writeback-caret-race/specs/settings-view-android-ui/spec.md:58`  
関連: `kasane/changes/fix-entrycell-writeback-caret-race/tasks.md:11`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:121`

**問題点**: 「text／キャレットを変えてはならない」と同時に、`inputType`、`isEnabled`、`maxLength` などをフォーカス中も従来どおり反映する方針です。しかし、現行コード自身が `inputType` 更新は `restartInput` を起こすと明記しています。`isEnabled=false` はフォーカス喪失を誘発し、Critical 指摘の blur 再同期へ入ります。

placeholder のテストだけでは、これらの衝突を判定できません。

**推奨修正**: プロパティを次のように分類し、優先順位を仕様化してください。

- フォーカス中も安全に即時反映するもの
- IME／入力値へ影響するため blur まで保留するもの
- `isEnabled=false` のように意図的に編集を終了させるもの

少なくとも keyboardType、isPassword、maxLength、isEnabled の変更中 Scenario を追加してください。

### [🟠 Major] 実機試験が全件 skip／キャレット未検証でも合格できる

**該当箇所**: `kasane/changes/fix-entrycell-writeback-caret-race/tasks.md:25`  
関連: `kasane/changes/fix-entrycell-writeback-caret-race/repro-burst-loop.sh:8`

**問題点**: 「欠落・並び替え 0」に試行数と最低有効件数がありません。スクリプトは全試行が `SKIP`、または `FAIL > 0` でも終了コード 0 です。また、確認対象は最終 text と View bounds だけで、Scenario が要求するキャレット移動や composing 状態を観測していません。

確率的な競合なので、少数試行や全 skip で誤って完了判定する危険があります。

**推奨修正**:

- MAUI／native ごとの固定試行数と最低有効件数を定義する
- `FAIL > 0` または有効件数不足ならスクリプトを失敗終了させる
- 実行結果と環境情報を change 配下の証跡として保存する
- stale bind を明示的に挟む決定論的な Robolectric テストも追加する
- キャレット、継続入力、日本語 composing は別の実機受け入れ項目として記録する

### [🟡 Minor] 新提案と既存 ADR の関係整理が不足している

**該当箇所**: `kasane/changes/fix-entrycell-writeback-caret-race/proposal.md:33`  
関連: `kasane/decisions/android/0001-content-update-preserves-viewholder.md:24`

**問題点**: android/ADR-0001 は高速入力の取りこぼしを「解消」と記録し、入力元への通知抑止を外部更新取りこぼしの懸念から却下しています。本提案は新しい実測で前者の適用範囲を覆し、後者に近い「フォーカス中の反映抑止」を採用しますが、補足／変更関係が説明されていません。

また、参照する maui/ADR-0012 は `proposed` 状態であり、accepted な契約としては扱えません。

**推奨修正**: ADR 候補に次を明記してください。

- 新決定は android/ADR-0001 を置換するのか、補完するのか
- ADR-0001 の「高速入力解消」という Consequence をどう限定するか
- 今回のフォーカスガードが、ADR-0001 で却下された一方向化とどう異なるか
- maui/ADR-0012 は proposed であり、現行挙動の根拠はコード／テストであること

## アクションプラン

1. blur 時の競合規則を決め、直前入力を失わない Scenario を追加する。
2. 同一 Cell を `cell.id` と明文化する。
3. IME 継続性と入力関連プロパティの優先順位を Requirement 化する。
4. 実機試験の試行数・失敗条件・証跡を定量化する。
5. android/ADR-0001 および proposed 状態の maui/ADR-0012 との関係を整理する。

**判定: NEEDS_DISCUSSION**


---

## 突き合わせ結果

ホスト側自己レビュー (2周、指摘なし) との突き合わせ。全指摘が相方のみの検出。

| 指摘 | 採否 | 根拠 |
|---|---|---|
| Critical: blur 再同期が直前入力を喪失・古い値を再コミット | **採用** | 到達可能な順序をホスト側で追試し実害を確認 (再同期 setText → TextWatcher 発火 → 古い値がアプリ状態へ逆流)。再同期の watcher 非発火 + 静穏化収束の Scenario 化で解消する |
| Major: 同一 Cell 判定の未定義 (equals は text 比較の罠) | **採用** | 実装者が `==` を選ぶと text 変化時に「別 Cell」と誤判定する具体的な壊れ方の指摘。cell.id 明記 + 判別テスト 3 種を spec / tasks に反映 |
| Major: IME desync 解消が Requirement に未昇格 | **採用** | proposal の主張と spec の契約の不整合。入力継続性を独立 Requirement 化 |
| Major: text 保護と入力系プロパティ即時反映の優先順位欠如 | **採用** | 現行コード自身が inputType 更新 = restartInput と明記しており矛盾は実在。プロパティ分類と優先順位を spec に追加 |
| Major: 実機試験が全 skip / FAIL>0 でも合格し得る | **採用** | 判定基準の定量化 (試行数・最低有効件数・失敗終了・証跡保存) を tasks に反映 |
| Minor: ADR-0001 / ADR-0012 (proposed) との関係整理不足 | **採用** (文書整理) | ADR 候補の申し送りに補完/限定関係と ADR-0012 の proposed 状態を明記 |

未解決 (相方と割れた論点): なし

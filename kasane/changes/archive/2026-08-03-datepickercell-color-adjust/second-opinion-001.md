# セカンドオピニオン: datepickercell-color-adjust (001 回目)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 提案一式 (proposal / specs / tasks / ui/brief + 承認モック) — 実装前 spec-review
---
判定: **CHANGES_REQUESTED**。Critical はありませんが、このまま実装すると判定不能・既定色への戻り・状態表現の破壊が起こり得ます。

件数: Critical 0 / Major 7 / Minor 2 / Suggestion 1

## 指摘事項

### [Major] 半透明アクセントのコントラスト判定が既存実装と矛盾する

**該当箇所**: [proposal.md](kasane/changes/datepickercell-color-adjust/proposal.md:14)、[spec.md](kasane/changes/datepickercell-color-adjust/specs/cell-types-input/spec.md:12)、[ui/brief.md](kasane/changes/datepickercell-color-adjust/ui/brief.md:28)、[TimePickerColors.kt](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColors.kt:26)

**問題点**: 提案側は「アクセント色とのコントラスト比／輝度」で白黒を選ぶと読める一方、再利用対象の `TimePickerColors.onAccent` は、半透明アクセントを背景へ合成した `accentSurface` とのコントラストで判定します。たとえば白背景上の半透明黒では、前者は白、既存実装は黒を選び得ます。

**推奨修正**: 全アーティファクトを「アクセントをダイアログ背景へ合成した実効面」と明記して統一し、不透明・半透明アクセント双方の Scenario／テストを追加してください。

### [Major] 日付変更時に Material 側が同じ View を再着色する経路が仕様・タスクから漏れている

**該当箇所**: [spec.md](kasane/changes/datepickercell-color-adjust/specs/cell-types-input/spec.md:28)、[tasks.md](kasane/changes/datepickercell-color-adjust/tasks.md:17)、[ADR-0008](kasane/decisions/android/0008-datepicker-dialog-coloring-and-header-fix-via-view-traversal.md:25)

**問題点**: material 1.12.0 は日付選択時に `notifyDataSetChanged()` を呼び、`MonthAdapter`／`YearGridAdapter` が既存 TextView の背景と文字色を Material の `CalendarItemStyle` で上書きします。Scenario は月移動と年選択しか扱わず、「別の日を選ぶ」操作後の配色維持がありません。TimePicker と同じ「View ごとに静的適用1回」を機械的に横展開すると、同じ View への塗り戻しを見逃します。

**推奨修正**: 「別の日を選択した後、旧選択日・新選択日・今日・無効日の各状態が正しいロールになる」Scenario を追加してください。同一 View へ Material 配色を再適用した後でも Colorizer が戻せるテストを tasks に明記すべきです。

### [Major] 無効状態を含む色ロール表が不完全

**該当箇所**: [spec.md](kasane/changes/datepickercell-color-adjust/specs/cell-types-input/spec.md:9)、[ui/brief.md](kasane/changes/datepickercell-color-adjust/ui/brief.md:41)、[承認モック HTML](kasane/changes/datepickercell-color-adjust/ui/mock/variant-a-compact-selection-text.html:13)

**問題点**: 次の部位・状態が未定義です。

- 不正／未完了入力時に disabled となる OK ボタン
- 非フォーカス時の入力欄枠
- placeholder／helper text
- ヘッダ区切り線
- 年選択での「今年だが未選択」の状態

特に OK を単色 accent にすると disabled の視覚状態を消し、既定 CSL を残すと「プラットフォーム既定配色を残さない」と衝突します。エラー色を対象外にしていても、全 TextView 走査ではエラーテキストを通常文字色で上書きする危険もあります。

**推奨修正**: 部位だけでなく状態を軸にした対応表へ更新し、enabled／disabled／focused／error／selected／today の各状態を明示してください。対象外部位を汎用 TextView 分岐から除外する受け入れ条件も必要です。

### [Major] ヘッダ要件の適用範囲が無制限で、現在のテストでは判定できない

**該当箇所**: [spec.md](kasane/changes/datepickercell-color-adjust/specs/cell-types-input/spec.md:56)、[tasks.md](kasane/changes/datepickercell-color-adjust/tasks.md:5)、[ui/brief.md](kasane/changes/datepickercell-color-adjust/ui/brief.md:65)

**問題点**: Requirement は任意の `pickerTitle` または既定タイトルについて「両方の全体が視認できる」と保証していますが、Scenario は日本語タイトル1件だけです。フォント倍率、画面幅、縦横表示、fullscreen layout、長いタイトルでの扱いが未定です。また Robolectric の bounds 非重複だけでは、文字のクリップや ellipsize を検出できません。

**推奨修正**: 保証する構成を明示してください。最低でも locale、fontScale、画面幅、portrait／landscape、dialog／fullscreen、タイトル長と超過時の省略・折返し方針が必要です。テストには bounds に加え、クリップ／ellipsis／全行表示の判定を含めてください。

### [Major] 必須状態の年選択画面が承認モックに存在しない

**該当箇所**: [ui/brief.md](kasane/changes/datepickercell-color-adjust/ui/brief.md:7)、[approved.png](kasane/changes/datepickercell-color-adjust/ui/mock/approved.png)、[tasks.md](kasane/changes/datepickercell-color-adjust/tasks.md:30)

**問題点**: brief と spec は年選択グリッドを対象にし、tasks は年選択スクリーンショットを `approved.png` と照合するとしています。しかし承認モックにはカレンダー表示とテキスト入力しかなく、選択年・今年・通常年の見た目を比較できません。

**推奨修正**: 年選択状態を承認モックへ追加し、選択年、今年、通常年を同時に示してください。既存画像を維持するなら、年選択専用の承認画像を追加して brief に記録します。

### [Major] Activity 再生成時の保証範囲が未定義

**該当箇所**: [proposal.md](kasane/changes/datepickercell-color-adjust/proposal.md:23)、[DatePickerCellViewHolder.kt](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:97)、[TimePickerColorizer.kt](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizer.kt:114)

**問題点**: MaterialDatePicker は Activity 再生成時に FragmentManager から復元されますが、クリック時に追加した positive-button listener と Colorizer の lifecycle callback は復元されません。Requirement はダイアログ全般を無条件に保証しており、Non-Goals にもこの例外がないため、回転後は仕様違反になります。

**推奨修正**: 今回直さないなら「Activity／構成再生成後の復元」を明示的な Non-Goal とし、Requirement の適用範囲も初回表示セッションへ限定してください。保証対象にするなら、回転後の配色と OK callback の Scenario／タスクが必要です。

### [Major] 背景色・通常文字色を ViewHolder から渡す実装タスクが欠落している

**該当箇所**: [proposal.md](kasane/changes/datepickercell-color-adjust/proposal.md:9)、[tasks.md](kasane/changes/datepickercell-color-adjust/tasks.md:9)、[TimePickerCellViewHolder.kt](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:59)

**問題点**: tasks は `DatePickerCell.accentColor` の接続だけを明記し、`Theme.backgroundColor` と実効タイトル色の解決・Colorizer への受け渡しを扱っていません。先行 TimePicker には `resolveDialogColors` と bind 時の受け渡しが明示されています。このままではホストテーマ色を使ったままでもタスク完了扱いになり得ます。

**推奨修正**: DatePicker 用の色束解決と、bind 時の背景／accent／text の取得・クリック時の受け渡しを独立タスクにしてください。3色すべてを検証する resolver テストも必要です。

### [Minor] デルタスペックが Kasane の UI lint に反する

**該当箇所**: [spec.md](kasane/changes/datepickercell-color-adjust/specs/cell-types-input/spec.md:7)

**問題点**: 選択日、今日の枠、入力欄、キャレット、年選択など特定コントロールの見た目がデルタスペックに残っています。Kasane のデルタスペックは観察可能な状態遷移、部位・視覚詳細は brief／mock が担当する規約です。

**推奨修正**: spec は「解決済みテーマ色が全対象モード・状態へ反映される」という契約と状態遷移に絞り、部位割当は brief へ一本化してください。

### [Minor] 「アクセント色4段解決」と実際の3段解決が不整合

**該当箇所**: [tasks.md](kasane/changes/datepickercell-color-adjust/tasks.md:24)、[spec.md](kasane/changes/datepickercell-color-adjust/specs/cell-types-input/spec.md:40)

**問題点**: アクセントは Cell 固有値 → CellStyle → 非 null の Theme 値という3段です。また「未指定時のフォールバック」1 Scenario では、CellStyle 採用と Theme 採用のどちらか一方しか試さなくても合格できます。

**推奨修正**: 「3段解決」へ修正し、Cell 固有値優先／CellStyle 採用／Theme 採用を個別 Scenario・個別テストに分けてください。

### [Suggestion] material 1.12.0 の内部構造を継続的に固定する契約テストを追加する

**該当箇所**: [tasks.md](kasane/changes/datepickercell-color-adjust/tasks.md:7)、[ADR-0008](kasane/decisions/android/0008-datepicker-dialog-coloring-and-header-fix-via-view-traversal.md:39)

**問題点**: 内部 ID・型・背景 Drawable の前提はスパイクで確認するだけで、将来 dependency が変わった際に壊れたことを自動検出できません。

**推奨修正**: 必須 ID、View 型、選択日の再スタイル、年 View の識別方法を最小限の Robolectric 契約テストとして残してください。

実装前に最低限、Major 1〜5 と 7 の解消が必要です。Major 6 は今回のスコープに含めないなら、Non-Goal と Requirement の適用範囲を明文化すれば収束できます。


## 突き合わせ結果 (2026-08-02、ホスト自己レビューは指摘ゼロだったため全件「相方のみ」判定)

| # | 指摘 | 採否 | 根拠・処置 |
|---|---|---|---|
| Major 1 | onAccent 判定の記述が既存実装 (accentSurface 合成) と不一致 | **採用** | 該当箇所特定・実害シナリオ (白背景×半透明黒で白黒が逆転) あり。spec / proposal / brief の文言を「背景へ合成した実効面とのコントラスト」に統一、半透明アクセントのテストを tasks へ追加 |
| Major 2 | 日付再選択時の adapter 塗り戻し (notifyDataSetChanged) が Scenario から漏れ | **採用** | material 1.12.0 の実挙動を根拠に特定。「日付を選び直しても配色が維持される」Scenario と再着色テストを追加 |
| Major 3 | 状態軸 (OK disabled / 非フォーカス枠 / helper / 区切り線 / 今年未選択) が部位対応表に不足 | **採用** | brief の部位対応表に状態行を追加、対象外部位の走査除外を tasks の受け入れ条件に明記 |
| Major 4 | ヘッダ要件の保証範囲が無制限で判定不能 | **採用 (限定方向)** | Requirement に保証構成 (ダイアログ表示・縦横・端末既定フォント倍率・タイトル超過時は省略許容) を明示、テストにクリップ/省略判定を追加。相方提示の全構成マトリクスは過剰のため保証構成の明示で収束 |
| Major 5 | 年選択グリッドが承認モックに存在しない | **採用** | モック A に年選択ペインを追加し再承認を取る |
| Major 6 | Activity 再生成時の復元が未保証 | **採用 (Non-Goal 化)** | 既知の構造問題として fix-picker-dialog-recreation (起票済み・DatePickerCellViewHolder も対象と明記) の領分。proposal の Non-Goals に明記し Requirement の適用範囲を表示セッション内に限定 (相方も可とした収束方法) |
| Major 7 | 背景・文字色の解決と受け渡しタスクが欠落 | **採用** | TimePicker の resolveDialogColors 相当が tasks に無いのは事実。色束解決タスクと 3 色 resolver テストを追加 |
| Minor 1 | 部位列挙が Kasane UI lint に反する | **降格** | 同形式の timepickercell-color-adjust デルタスペックがオーナー承認・蒸留済みで、プロジェクトの lint 運用は「部位名の列挙 (ロール定義の範囲指定)」を許容している。見た目の詳細は書いておらず brief への一本化も済んでいる |
| Minor 2 | 「4 段解決」表記と実際の 3 段の不整合、fallback Scenario の判定曖昧 | **採用** | tasks の表記を解決順の明示に修正、fallback を CellStyle 採用 / Theme 採用の 2 Scenario に分割 |
| Suggestion | 内部構造の契約テスト | **採用 (軽量)** | 必須内部 ID・View 型の存在検証を Robolectric 契約テストとして tasks に追加 (ADR-0008 の追随確認を自動化する安価なガード) |

採用 9 / 降格 1 / 未解決 0。

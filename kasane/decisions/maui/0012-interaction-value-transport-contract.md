---
id: 0012
title: 双方向値の輸送規約 — 壁時計値は ISO-8601 文字列、選択は index、書き戻しは必須コミット
status: accepted
date: 2026-08-10
---

## Context

maui-support / phase-4-basic-input-cells の議論。対話型 Cell の双方向バインド8プロパティ (Switch.On / Checkbox.IsChecked / Radio.SelectedValue / Entry.ValueText / Picker.SelectedItem / NumberPicker.Number / TimePicker.Time / DatePicker.Date) を [ADR-0003](0003-single-interaction-delegate.md) の単一 delegate/listener で C# へ通知し、facade から Store へ書き戻す経路を設計するにあたり、interop 境界の値表現を決める必要があった。

前提事実 (コード調査で確定):

- 両OSとも Cell view はコールバック (`onValueChanged` 等) を呼ぶだけで Store へ書き込まない。**通知時点で native Store は旧値のまま**であり、Store 更新は呼び出し側の責務。
- 値の型は Bool / Int / String が `@objc` / JNI を素通しできる。Picker の選択値は native 両OSとも Int (index)。Time / Date は iOS が `Date`、Android が `LocalTime` / `LocalDate` (java.time 型は JNI 境界を直接越えられない)。

## Decision

- **書き戻しは必須コミット**: delegate 通知を受けた facade は `FindCell(cellId)` → CellBase へ SetValue → 既存 dirty-set/flush → `replaceCell(s)` の経路で Store へ書き戻す。この送信は Store へ新値をコミットする唯一の経路であり、抑止してはならない。
- **エコー抑止は書き戻しの入口で行う**: delegate 通知値が **CellBase の現値**と同値なら書き戻さず停止する (同値チェックの比較対象は CellBase 現値 — Store は通知時点で旧値のため比較対象にならない)。折り返しループは「delegate → 入口同値チェック → SetValue → flush → コミット → Store diff → Host 再描画 → (listener 再発火) → delegate → 同値で停止」で必ず収束する。native 側にも同値 setText ガードが両OSに実在し (iOS: IME マークドテキスト保護 / Android: カーソル位置維持)、二重に安全。
- **発行抑止フック `ShouldPublish()` は撤去する**: phase-2 が口だけ確保した発行時点の抑止は、必須コミットを欠落させ得る構造的に誤った置き場と判明したため、常に true のまま使わず撤去する。phase-1 決定の「`updateCellValue` 直行パス・debounce は作らない」は踏襲。
- **輸送規約**:
  - Bool / Int / String のプロパティは素通し。
  - Picker の選択値は **index (Int) を輸送**する。`SelectedItem` (object) ⇔ index の解決は facade の ItemsSource 側で行い、Native には持ち込まない。複数選択 (`SelectedIndices`) の wire 表現は**昇順・重複除去に正規化した Int 配列**とし、入口同値チェックは**集合等価** (順序・重複無視) で比較する — 順序違いによる再配信ループを防ぐ。
  - Time / Date は壁時計値として **ISO-8601 文字列** ("HH:mm" / "yyyy-MM-dd") で輸送する。facade 型は AiForms 互換 (TimeSpan / DateTime)、native 側は各OS の慣例型 (iOS `Date`、Android `LocalTime` / `LocalDate`) へ Bridge 内で変換する。生成・解釈は culture 非依存の固定書式で行い、パース失敗 (契約違反) は操作によらず一律に該当フィールドを型の既定値 (時刻 00:00 / 日付 1970-01-01) で構築 + DEBUG 診断とし、実行時例外にしない。
- **書き戻し対象の正規一覧は 10 プロパティ** (agenda 時点の名目「8プロパティ」を実装で確定): `SwitchCell.On` / `CheckboxCell.Checked` / `SimpleCheckCell.Checked` / `RadioCell.SelectedValue` (同一 GroupId の全 RadioCell へ適用) / `EntryCell.ValueText` / `PickerCell.SelectedIndex` / `PickerCell.SelectedIndices` / `NumberPickerCell.Number` / `TimePickerCell.Time` / `DatePickerCell.Date`。いずれも `BindingMode.TwoWay` 既定。

## Alternatives Considered

- **Time / Date の epoch millis 輸送**: 壁時計値にタイムゾーン依存の表現を混ぜることになり、TZ 変換事故を作り込むため却下。
- **Time / Date の分割 int 輸送 (hour/minute, y/m/d)**: パース不要で堅牢だが、DTO フィールド増に対し ISO 文字列 (1フィールド・両OS/C# に標準パーサ・生成側も自前でパース失敗の余地が実質ない) を上回る利点がなく却下。
- **Picker の選択値を item 文字列で輸送**: native の実体 (index) と乖離し、ItemsSource を Native へ持ち込まない方針 ([ADR-0008](0008-aiforms-compatible-api-surface-policy.md)) とも衝突するため却下。
- **`ShouldPublish()` (発行時点) でのエコー抑止**: 書き戻し = 必須コミットである以上、発行経路での抑止はコミット欠落バグの温床になるため却下。
- **書き戻し中フラグで dirty 化自体を抑止**: 書き戻しが Store に届かなくなる (コミット経路の遮断) ため却下。

## Consequences

- 正: 輸送は素通し可能なプリミティブ (Bool / Int / String) のみで構成され、interop 境界に型変換の特殊機構が要らない。
- 正: 「通知時点で Store は旧値」の意味論が明文化され、エコー抑止設計 (同値チェック) の前提が確定する。
- 負: ISO 文字列の生成・解釈が Bridge (Swift / Kotlin) と facade (C#) の両端に入る (ただし形式は固定2種で機械的)。
- 負: Picker の SelectedItem 解決が facade の ItemsSource に依存するため、ItemsSource 未設定時の意味論 (index のみ公開等) を提案時に明確化する必要がある (実装では「未設定・範囲外のとき `SelectedItem` は null、`SelectedIndex` が正」で確定)。
- 補足: 書き戻し (replaceCell) が Android EntryCell の入力欄へ届いたときの反映側の契約は android/ADR-0014 (フォーカス中の入力欄は値の SSoT — 反映はフォーカス喪失まで遅延) が定める。書き戻し = 必須コミットの原則は不変で、遅延するのは表示反映のみ (出典: fix-entrycell-writeback-caret-race の実装結果)。

---
出典: 2026-08-10 ksn-agenda (maui-support / phase-4-basic-input-cells) での議論 (論点2) / add-maui-basic-input-cells design.md Decision 3・5 (正規一覧・wire 契約の確定)

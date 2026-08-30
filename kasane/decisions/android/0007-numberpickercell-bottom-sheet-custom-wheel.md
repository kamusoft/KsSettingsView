# ADR-0007: NumberPickerCell の選択 UI はボトムシート + 自作ホイール (RecyclerView + SnapHelper) で実装する

- Status: accepted
- Date: 2026-08-02
- Domain: android
- 関連: [ADR-0005](0005-pickercell-selection-ui-bottom-sheet.md) (PickerCell のボトムシート化 — 本決定はその続編)

## Context

Android の NumberPickerCell は選択 UI として `AlertDialog` + `android.widget.NumberPicker` (`setDisplayedValues` で候補文字列を設定) を表示している (`NumberPickerCellViewHolder.showNumberPickerDialog`)。オーナーの評価は「10年前からある古いスピナー形式は流石に刷新したい」。

関連する事実:

- **単位表示の欠落**: Android 版には `unit` プロパティ自体が存在しない。iOS 版は `unit` を持ち、`"<value> <unit>"` を自動生成する AiForms 互換フォーマッタ (`NumberPickerCell.format(value:unit:)`) を備える。AiForms オリジナルも「15 px」のように単位を表示する。よって修正は表示バグ修正ではなく iOS とのプロパティパリティ追加である
- **先行 change の資産**: ADR-0005 で PickerCell はボトムシート化済み。`PickerSelectionSheet` に BottomSheetDialog 継承・ドラッグハンドル・ヘッダー (取消/タイトル/確定)・高さ制御・スタイル解決の骨格があり再利用できる。同 change の proposal は Non-Goals に「NumberPickerCell / TimePickerCell / DatePickerCell の選択 UI 変更は将来の別変更候補」と明記しており、本決定はその続編にあたる
- **DatePickerCell (Spinner 版) の不具合**: XML の `datePickerMode="spinner"` が使えず `calendarViewShown = false` で「近い見た目」を作る弱い実装であり、Material テーマ環境ではホイール指定でもカレンダーが表示されてしまう。土台のウィジェット (`android.widget.DatePicker`) 自体の限界である

スクリーンショット: `kasane/changes/android-numberpicker-modern-ui/ui/references/` (現実装と AiForms オリジナルの比較)

## Decision

NumberPickerCell の選択 UI を、ボトムシート (ADR-0005 の `PickerSelectionSheet` と同系の器) + 自作ホイール (RecyclerView + LinearSnapHelper によるスナップ式ホイール) に変更する。

- ホイール部品は再利用可能な部品として作り、将来 DatePicker ホイール版 (3連ホイール) へ展開して Spinner 版不具合を同じ部品で根治する前提とする
- `unit` プロパティを iOS パリティで追加し、単位を表示に反映する
- 本 change のスコープは「unit パリティ追加 + NumberPickerCell のシート化」に絞り、DatePicker への展開は続編 change とする

## Alternatives Considered

- **A案: ボトムシート + `android.widget.NumberPicker` 流用** — 却下: 器をシートに載せ替えても、古さの根源である Holo 時代のウィジェットの見た目 (divider) とテーマ非対応 (色制御がリフレクション頼み) がそのまま残る。また DatePicker への展開時に結局 `widget.DatePicker` の限界に戻り、Spinner 版不具合を根治できない。実装コストの小ささだけが利点

## Consequences

- 正: フォント・色・フェード・中央ハイライトを完全制御でき、Material テーマと整合するモダンな見た目になる
- 正: 追加依存なし (RecyclerView + LinearSnapHelper は既存依存のみ)
- 正: 同じホイール部品を3連にして DatePicker ホイール版へ展開でき、カレンダーが出てしまう不具合を根治できる
- 正: PickerCell (ADR-0005) と「下から出る選択面」の体験が揃う
- 負: 実装コストは A案より大きい (スナップ・慣性・中央強調の作り込みが必要)

出典: kasane/changes/android-numberpicker-modern-ui/ の探索会話 (2026-08-02)

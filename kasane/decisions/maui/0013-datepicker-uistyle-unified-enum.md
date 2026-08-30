---
id: 0013
title: DatePicker の uiStyle は MAUI 統一 enum で意味マッピングし、器の差は native のまま透過する
status: accepted
date: 2026-08-10
---

## Context

maui-support / phase-4-basic-input-cells の議論。Native の `DatePickerUIStyle` は case がプラットフォームごとに異なり (iOS `.wheels` / `.calendar`、Android `Material` / `Spinner`)、concepts ([DatePickerCell の選択面](../../concepts/core/cells/date-picker-selection-surface.md)) は「case を同一と仮定してはならない — 名前も器も対応関係にない」と定める。MAUI facade がこのフィールドをどう公開するかを決める必要があった。

当初案は maui/[ADR-0004](0004-maui-idiomatic-types-for-styling.md) の延長で接頭辞付き別プロパティ (`IOSUIStyle` / `AndroidUIStyle`) だったが、オーナーから「MAUI 層では同一プロパティが望ましい。どちらも意味的にはカレンダー形式かホイール形式かにマッピングできる」との指摘があった。

## Decision

- MAUI 層に統一 enum **`DatePickerUIStyle { Calendar, Wheels }`** を新設し、次の意味マッピングで native case へ変換する:

| MAUI | iOS | Android |
|---|---|---|
| `Calendar` | `.calendar` (カレンダーシート) | `Material` (MaterialDatePicker ダイアログ) |
| `Wheels` | `.wheels` (埋め込み UIDatePicker) | `Spinner` (ボトムシート + 3連ホイール) |

- プロパティは nullable とし、**null = 各 native の既定に従う** (facade は既定を握らない)。
- concepts の「case 同一視禁止」との関係: 器も名前も別物であるという事実はそのまま維持し、MAUI 層が**意味軸 (カレンダー形式かホイール形式か) での明示的な対応付けを新設**する。case の同一視ではない。
- Android で `Calendar` を選ぶと Material 固有挙動 (テキスト入力モード・`FragmentActivity` 要求・走査配色の既知の限界) が付随することは facade 契約 (concepts maui/api/maui-facade.md) に明記する。
- `androidButtonColor` は iOS に対応概念がないため統一の対象外とし、ADR-0004 の接頭辞付き nullable (`AndroidButtonColor`) を維持する。選択面の挙動契約 (確定のみ反映・非確定 dismiss 破棄・スタイル継承等) は native 内で完結するため facade は透過する。

## Alternatives Considered

- **接頭辞付き別プロパティ (`IOSUIStyle` / `AndroidUIStyle`)**: concepts の禁止事項に最も忠実だが、利用者が1つの意図 (カレンダーで見せたい / ホイールで見せたい) を2プロパティに書く冗長さがあり、MAUI 層の抽象として意味軸で統一できるというオーナー判断で不採用。
- **値名 `Spinner` (Android case 名に揃える)**: Android では `Spinner` の第一義がドロップダウン widget であり多義。プロジェクト共通語彙も「ホイール」(`KsWheelView` / 3連ホイール) であるため却下。
- **値名 `Wheel` (単数形)**: 意味は同じだが、年/月/日の複数ホイール構成と iOS `.wheels` との字面対応から複数形を採用。

## Consequences

- 正: XAML / C# で1プロパティ・1値の指定が両OSに効き、意図 (見た目の形式) がそのまま API になる。
- 正: null 既定により native 側の既定変更へ自動追従する。
- 負: `Calendar` / `Wheels` の各値にプラットフォーム固有挙動が付随する (特に Android `Calendar` の Material 固有挙動)。facade 契約への明記で吸収する。
- 負: 将来 native に第3の uiStyle case が片OSだけ増えた場合、意味軸マッピングの再検討が必要になる (統一 enum に押し込めるか、接頭辞付きプロパティの併設か)。

---
出典: 2026-08-10 ksn-agenda (maui-support / phase-4-basic-input-cells) での議論 (統一 enum への転換と `Wheels` 採用はオーナー判断)

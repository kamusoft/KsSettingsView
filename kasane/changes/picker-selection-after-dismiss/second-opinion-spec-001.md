# セカンドオピニオン: picker-selection-after-dismiss (spec-001)
**相方**: codex / **label**: so-spec-picker-selection-after-dismiss / **日付**: 2026-09-18 / **対象**: 提案一式 (proposal.md / design.md / specs/ 4 本 / tasks.md)
---
静的レビューの結果、方向性は妥当ですが、実装前に解消すべき仕様・設計上の穴があります。件数は Critical 0 / Major 6 / Minor 1 / Suggestion 0 です。制約に従い、ビルド・テスト・ファイル書き込みは行っていません。

照合対象には、`MODIFIED` Requirement は変更後全文を記載する規約、実行時挙動の修正前後検証、現行 `SelectedCommand` テスト、core/ADR-0029 のジェネリック縁、maui/ADR-0012 の書き戻し契約を含みます。

### [🟠 Major] MODIFIED Requirement から既存の SelectedCommand 契約が脱落している

**該当箇所**: `specs/maui-cells/spec.md:3-4`、`tasks.md:30`

**問題点**: 現行契約には、OneWay・既定 null、同じ選択の再確定でも実行、公開値の直接設定では不発火、`CanExecute` を確認せず `Execute` を直接呼ぶ、という保証があります（`kasane/concepts/maui/api/maui-cells.md:44-48`、`kasane/changes/archive/2026-08-29-restore-maui-picker-selected-command/specs/maui-cells/spec.md:5-11`）。今回の `MODIFIED` Requirement はこれらを含まず、tasks もテストを「書き換える」としながら当該ケースを列挙していません。変更後全文というデルタスペック規約上、単なる省略ではなく契約削除と読めます。

**推奨修正**: 既存契約をすべて残した変更後全文へ展開し、同値再確定・直接 setter・`CanExecute=false`・公開プロパティ形状の Scenario とテスト維持を tasks に明記してください。

### [🟠 Major] 新 callback が既存の公開構築経路を網羅していない

**該当箇所**: `design.md:41-44`、`tasks.md:4-9`、`tasks.md:12-15`

**問題点**: design は iOS Picker の「公開 init 4 本」だけを全経路として扱っていますが、現行には object 射影用の多数のジェネリック init（`ios/Sources/KsSettingsViewUI/PickerCell+ItemProjection.swift:26-209`）、Android のジェネリック factory（`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellItemProjection.kt:27-178`）、Compose DSL overload（`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:113-348`）があります。core/ADR-0029 がこれらを Native 利用者向け API の縁として確定しています。

また iOS `DatePickerCell` は `withDSLID` / `withStyle` / `withIcon` が全 callback を手動転送する構造ですが（`ios/Sources/KsSettingsViewUI/DatePickerCell.swift:165-191`）、新 callback の保持は spec・tasks にありません。SwiftUI modifier 適用後に callback が消える実装が成立してしまいます。

**推奨修正**: Picker/DatePicker の Store・Binding・object 射影・String 特殊化・SwiftUI/Compose DSL・`with*`/`copy` を入口一覧として明記し、全経路で指定・保持できる Scenario と対称テストを追加してください。

### [🟠 Major] 遅延中に状態が変わると、SelectedCommand が確定した項目と別の値を受け取り得る

**該当箇所**: `specs/maui-bridge/spec.md:3-14`、`specs/maui-cells/spec.md:4`、`design.md:89-95`

**問題点**: Bridge は completed 通知に確定 index を運びますが、MAUI はその値を使わず、通知時点の `SelectedItem` / `SelectedItems` を引数にします。現行 `NotifySelectionCompleted` も現在値を読む実装です（`maui/KsSettingsView.Maui/PickerCell.cs:311-316`）。値通知から dismiss 完了までの間に `ItemsSource` の並べ替え・差し替え、選択値のプログラム更新が起きると、利用者が確定した項目とは別の object が Command に渡り得ます。`SelectionMode` の途中変更だけを扱っており、より一般的な可変状態との交差が未決です。

**推奨修正**: Command 引数を「確定時 snapshot」と「completed 時点の最新値」のどちらにするか決めてください。確定時 snapshot を採るなら、changed 受信時に項目を保持し、種類・cellId・値が対応する completed でのみ消費する設計と、ItemsSource/選択値変更・不一致 completed の Scenario が必要です。

### [🟠 Major] Android だけ「閉じ切った後」の意味が異なる

**該当箇所**: `proposal.md:15-16`、`design.md:70-79`、`specs/settings-view-android-ui/spec.md:34-45`

**問題点**: proposal は「選択面が閉じ切った後」を両プラットフォーム同じ意味で保証するとしています。一方、Android DatePicker の spec は「退場アニメーション完了を待たない」とし、design も通常 Dialog の dismiss listener がフェード完了前に呼ばれると認識しています。これは中心契約である「閉じ切った後」と直接矛盾します。

**推奨修正**: 共通契約を次のどちらかに確定してください。

- 実際の退場完了後まで待ち、両 OS で本当に「閉じ切った後」とする。
- Android は「プラットフォームが dismiss を報告した時点」とする意図的差異を明記し、共通・同義という記述を撤回する。

後者なら callback 名・利用者向け説明も、その差を誤認させないか再検討が必要です。

### [🟠 Major] 計測結果次第で未仕様の描画更新変更を実装できる構造になっている

**該当箇所**: `proposal.md:18`、`design.md:97-103`、`design.md:111`、`tasks.md:35-36`

**問題点**: セクション reload が原因なら行単位更新へ置換する等の修正を本 change に同梱するとしていますが、その変更に対応する Requirement、Scenario、影響範囲、回帰テストがありません。実装開始後は足場を凍結するため、この分岐はレビューされていない変更を実装する入口になります。また proposal は「ライブラリ側なら修正」、design/tasks は「第一容疑なら同梱、それ以外は相談」と条件が一致していません。

**推奨修正**: 本 change は計測と原因特定までに限定し、修正が必要なら別提案にするのが安全です。同梱するなら、行単位更新の適用条件・既存表示同期との整合・identity/スクロール位置・更新範囲・回帰テストまで事前にデルタスペック化してください。

### [🟠 Major] dismiss 計測の合否が再現可能な基準になっていない

**該当箇所**: `design.md:97-100`、`tasks.md:33-36`、`exploration.md:31-35`

**問題点**: 元の症状は iPhone 11 実機・iOS 18系・Debug で観測されていますが、計画は「iOS 実機」までしか指定していません。「候補数/Section 行数の少・多」「0.5秒前後」も具体値・許容上限がなく、実装者によって合否が変わります。また修正後の Native 対 MAUI 比較だけで、修正前ビルドで同じ手順を再現する A/B がありません。実行時挙動規約の「修正前の再現→修正後に同一手順で解消確認」を満たせません。

**推奨修正**: 少なくとも端末・OS・構成、少/多の件数、試行順と回数、時計、中央値以外のばらつき、絶対時間の合格上限、修正前後 A/B、モーダル提示成功の観測方法を事前に固定してください。元端末を使えない場合は代替端末であることを記録し、元環境での解消保証とは区別すべきです。

### [🟡 Minor] iOS ホイール経路の fallback で callback 順序が逆転し得る

**該当箇所**: `design.md:57-64`、`specs/settings-view-ios-ui/spec.md:70-73`

**問題点**: design は `resignFirstResponder()` が false の場合、その場で `onValueCompleted` を呼ぶとしています。一方、現行の値 callback は resign の後に呼ばれます（`ios/Sources/KsSettingsViewUI/DatePickerCellView.swift:177-185`）。素直に実装すると false 経路で Completed → Changed となり、spec の順序と逆転します。通知が届かなかった場合の observer 解放や、後続の別キーボード通知による遅延誤発火も未定義です。

**推奨修正**: Changed を必ず一度確定してから Completed を送る状態機械を明記し、false fallback、通知未到達、再利用・破棄、重複 Done をテスト対象に追加してください。

以上から、実装へ進める前に少なくとも「MAUI の遅延中状態変更」「Android の閉じ切り定義」「計測後修正の扱い」の設計判断が必要です。

判定: NEEDS_DISCUSSION



## 突き合わせ結果 (2026-09-18)

ホスト側の自己レビュー (2 周、指摘なし) との突き合わせ。相方の 7 件はすべて実物 (concepts `maui/api/maui-cells.md`、archive `2026-08-29-restore-maui-picker-selected-command`、`PickerCell+ItemProjection.swift`、`DatePickerCell.swift` の `with*`、`PickerCellItemProjection.kt` / `compose/InputCellDsl.kt`、`DatePickerCellView.swift` の Done 経路) で裏が取れ、ホスト側の見逃しとして扱う。

| # | 指摘 | 採否 | 処置 |
|---|---|---|---|
| Major 1 | MODIFIED から既存の SelectedCommand 契約が脱落 | 採用 | `specs/maui-cells/spec.md` を既存契約込みの全文へ展開。再確定 / 直接 setter / CanExecute false の Scenario と tasks 5.3 の維持テストを追加 |
| Major 2 | 新 callback が公開構築経路を網羅していない | 採用 | design Decision 2 に入口一覧 (iOS 射影 init・DatePicker `with*`・Android factory / Compose DSL) を明記。iOS / Android spec に Requirement・Scenario、tasks 1.1 / 1.3 / 2.1 / 2.4 を更新 |
| Major 3 | 遅延中の状態変更で確定した項目と別の値が渡り得る | 採用 (設計判断: 閉じ切り時点の現値、オーナー確定) | spec に「引数は閉じ切り時点の現値」と Scenario を追加。design Decision 7 に却下案 C (確定時 snapshot) と理由を追記 |
| Major 4 | Android だけ「閉じ切った後」の意味が異なる | 採用 (設計判断: 意図的差異として明記、オーナー確定) | 共通定義を「プラットフォームが dismiss 完了として報告する時点」とし、Android カレンダーのフェード前発火を proposal / ADR-0034 / Android spec / design Decision 5 に明記 |
| Major 5 | 計測結果次第で未仕様の変更を実装できる構造 | 採用 (計測を提案段階へ前倒しして解消) | 一時パッチで Simulator 計測。ライブラリ側の遅延要因なしと判定し、修正の分岐を proposal / design / tasks から削除 (`evidence/dismiss-timing/README.md`) |
| Major 6 | 計測の合否が再現可能な基準になっていない | 採用 (同上) | 計測方法・環境・試行・判定基準を README に固定。実機 (iPhone 11) は tasks 6.1 で 1 系列 (判定基準付き) |
| Minor | ホイール fallback で callback 順序が逆転し得る | 採用 | design Decision 4 を Changed → Completed を崩さない状態機械に書き換え (false 経路・通知未到達の上限時間・再利用 / 破棄・重複 Done) |

未解決: なし。判定は NEEDS_DISCUSSION → 3 件の設計判断をオーナーが確定し、反映済み。

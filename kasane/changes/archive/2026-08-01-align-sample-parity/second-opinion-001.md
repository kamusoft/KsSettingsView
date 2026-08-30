# セカンドオピニオン: align-sample-parity (001 回目)
**相方**: codex / **日付**: 2026-07-31 / **対象**: 提案一式 (proposal / specs / tasks / ui、spec-review モード)
---
# レビュー結果: align-sample-parity

**判定**: `NEEDS_DISCUSSION`  
**件数**: Critical 0 / Major 7 / Minor 3 / Suggestion 1

## サマリー

承認済みモックは存在しますが、このまま実装しても cross/ADR-0016 の完全一致へ収束しない既存差分が残ります。特に DatePicker の一致条件は現行公開 API と両立せず、仕様選択が必要です。

また、Scenario の期待値と検証手順が十分に具体化されておらず、実装完了を客観的に判定できません。実装開始前に proposal・specs・tasks・UI アーティファクトを再整合させる必要があります。

## 指摘事項

### [🟠 Major] 変更級が Kasane の L 級基準と矛盾する

**該当箇所**: [proposal.md:26](kasane/changes/align-sample-parity/proposal.md:26)、[proposal.md:41](kasane/changes/align-sample-parity/proposal.md:41)

**問題点**: proposal 自身が `samples-ios` と `samples-android` の2能力を影響対象に挙げ、iOS／Android の複数ドメインを横断しています。これは ksn-core の「複数能力横断」に該当するため L 級です。M 級扱いの結果、DatePicker の対応関係、共有 Theme、検証方式などの設計判断を記録する `design.md` がありません。

**推奨修正**: L 級へ変更し、`design.md` に少なくとも以下を Decision として明記してください。

- DatePicker の `.wheels/.calendar` と `Spinner/Material` の対応
- Android に存在しない `todayText` の扱い
- parity の比較表と検証方式
- メニュー／タイトル文字列の単一管理方法

---

### [🟠 Major] Store 方式デモの既存不一致が変更対象から漏れている

**該当箇所**: [proposal.md:5](kasane/changes/align-sample-parity/proposal.md:5)、[ContentView.swift:63](samples/ios/KsSettingsViewSample/ContentView.swift:63)、[MainActivity.kt:339](samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:339)

**問題点**: iOS は `Sample Row 1/2/3`、Android は `Sample Label 1/2/3` で、表示文言が既に不一致です。しかし、どの Requirement・実装タスクにも修正がありません。タスク3.1で検出できても、足場凍結後には対応する実装タスクがなく、主目的である完全一致を達成できません。

**推奨修正**: iOS または Android のどちらを正とするか決め、Store デモの Section header/footer、Cell title、追加時の文言を一致対象として両 spec と tasks に追加してください。他の5画面についても同様の完全棚卸し結果を明示してください。

---

### [🟠 Major] DatePicker の完全一致要求が現行 API と両立しない

**該当箇所**: [samples-android/spec.md:15](kasane/changes/align-sample-parity/specs/samples-android/spec.md:15)、[samples-android/spec.md:28](kasane/changes/align-sample-parity/specs/samples-android/spec.md:28)、[InputCellsDemoView.swift:182](samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:182)、[DatePickerCell.kt:29](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCell.kt:29)

**問題点**: iOS は両 DatePicker に `todayText: "今日"` を渡し、footer も Toolbar の「今日」ボタンや「iOS カレンダーアプリ風」と説明しています。一方、Android 公開 API には `todayText` がなく、形式も `Spinner/Material` です。それにもかかわらず Android spec は全文言・全パラメータ一致を SHALL とし、例外分岐を `uiStyle` の不可能時にしか設けていません。

そのまま文言だけコピーすると、Android 上で `.wheels` や「iOS カレンダーアプリ風」と表示しながら別 UI が開く、誤解を招く Sample になります。

**推奨修正**: 実装前に次のいずれかを選択してください。

- iOS 側を「ホイール形式」「カレンダー形式」などの中立文言へ変更し、Android の Spinner／Material と対応付ける。
- 本体変更をスコープへ含め、Android に Today 相当機能を追加する。
- 一致不能を既知の deviation として提案段階で明記し、Requirement・Scenario・Non-Goalsを整合させる。

---

### [🟠 Major] 「最後のイベント」の受け入れ値が定義されていない

**該当箇所**: [samples-ios/spec.md:21](kasane/changes/align-sample-parity/specs/samples-ios/spec.md:21)、[samples-ios/spec.md:24](kasane/changes/align-sample-parity/specs/samples-ios/spec.md:24)、[samples-android/spec.md:15](kasane/changes/align-sample-parity/specs/samples-android/spec.md:15)

**問題点**: `<イベント内容>` の形式が未定義です。Cell名、変更後の値、選択肢の index／表示名、日付形式など、実装者ごとに異なる文字列でも Scenario を満たしたと解釈できます。また「いずれかの入力 Cell」が任意の1種類なのか、全 Cell 操作が更新対象なのかも曖昧です。

複数選択の4件目拒否時にイベント表示を更新するかどうかも決まっていません。

**推奨修正**: 5種類すべてについて、成功操作時の完全な期待文字列を表で定義してください。少なくとも Entry、単一／複数Picker、Number、Time、2つのDateについて個別 Scenario を置き、拒否・キャンセル時に表示を維持するかも定義してください。

---

### [🟠 Major] 画面タイトルの対象数が proposal・spec・tasks で矛盾する

**該当箇所**: [proposal.md:12](kasane/changes/align-sample-parity/proposal.md:12)、[samples-ios/spec.md:13](kasane/changes/align-sample-parity/specs/samples-ios/spec.md:13)、[tasks.md:10](kasane/changes/align-sample-parity/tasks.md:10)、[MinimalDiffableDemoView.swift:67](samples/ios/KsSettingsViewSample/MinimalDiffableDemoView.swift:67)

**問題点**: proposal は「全6画面」、spec は Minimal Diffable を含む「全7画面」、tasks は「全7画面」としながら括弧内では Store／DSL＋他4画面しか列挙していません。現行 Minimal Diffable のタイトルは実際にメニュー文言と不一致です。

**推奨修正**: 対象を7画面と統一し、`Minimal Diffable 検証`への変更を proposal と task 1.4 に明記してください。

---

### [🟠 Major] 検証計画では Scenario の合否を判定できない

**該当箇所**: [tasks.md:3](kasane/changes/align-sample-parity/tasks.md:3)、[tasks.md:15](kasane/changes/align-sample-parity/tasks.md:15)、[tasks.md:24](kasane/changes/align-sample-parity/tasks.md:24)、[plan-b.html:37](kasane/changes/align-sample-parity/ui/mock/plan-b.html:37)

**問題点**: task 1.9 は「全画面」を `approved.png` と照合するとしていますが、承認モックに存在するのはiOSルートメニューと入力Cell画面だけです。また静止スクリーンショットだけでは、次を判定できません。

- 4件目の複数選択が拒否されること
- 各入力 Cell 操作によるイベント表示更新
- DatePicker の実際の表示形式
- min/max/step、キーボード種別、maxLength
- 全7遷移先のタイトル一致

**推奨修正**: Requirement／Scenarioごとの検証マトリクスを tasks に追加してください。静的構成は画面別スクリーンショット、動的挙動は操作手順・期待値付きの手動確認または UI テストへ分け、各 Scenario に一対一で対応させてください。

---

### [🟠 Major] デルタスペックが Kasane の UI lint に違反している

**該当箇所**: [samples-ios/spec.md:5](kasane/changes/align-sample-parity/specs/samples-ios/spec.md:5)、[samples-ios/spec.md:21](kasane/changes/align-sample-parity/specs/samples-ios/spec.md:21)、[samples-android/spec.md:7](kasane/changes/align-sample-parity/specs/samples-android/spec.md:7)、[samples-android/spec.md:15](kasane/changes/align-sample-parity/specs/samples-android/spec.md:15)

**問題点**: グループ／一覧形式、Section構成、特定 Cell の配置、Themeなど、見た目とレイアウトの詳細がデルタスペックへ入っています。Kasane のデルタスペックは観察可能な状態遷移、mock は見た目の正という役割分離になっています。

現状は静的UI契約が spec と mock に重複する一方、mock は対象画面の一部しか表現していません。

**推奨修正**: 先に brief／mockを必要な全画面・状態へ拡張し、レイアウト・Theme・Section構成をそちらへ移してください。specには、遷移後タイトル、値変更後イベント、選択上限拒否などの状態遷移だけを残してください。

---

### [🟡 Minor] Android の共有デモグループ見出しが未規定

**該当箇所**: [samples-android/spec.md:7](kasane/changes/align-sample-parity/specs/samples-android/spec.md:7)、[ui/brief.md:7](kasane/changes/align-sample-parity/ui/brief.md:7)、[plan-b.html:43](kasane/changes/align-sample-parity/ui/mock/plan-b.html:43)

**問題点**: 承認済みiOS案は「デモ」見出しを表示しますが、Androidは「検証グループなし」とだけ規定され、「デモ」見出しを表示するか不明です。共有部分のSection headerも完全一致対象です。

**推奨修正**: Androidにも「デモ」見出しを表示するのか、platform固有検証画面の例外に伴うメニュー構造差として許容するのかを明記してください。

---

### [🟡 Minor] 2つの日付 Cell が同じ状態を共有する既存挙動が未決

**該当箇所**: [InputCellsDemoView.swift:203](samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:203)、[samples-ios/spec.md:22](kasane/changes/align-sample-parity/specs/samples-ios/spec.md:22)

**問題点**: iOS の「予約日」も `birthdayDate` にバインドされており、一方を変更すると他方も変化します。specは現行構成維持とするだけで、この状態共有を意図したデモ仕様とするか、既存バグとして分離するか決めていません。Androidが独立状態のままだと操作後に差異が生じます。

**推奨修正**: 通常は `reservationDate` を新設して独立させ、両platformで同じ初期値と独立更新を要求してください。意図的な共有なら、その旨と連動更新Scenarioを明記してください。

---

### [🟡 Minor] Impact のリスク参照番号が誤っている

**該当箇所**: [proposal.md:38](kasane/changes/align-sample-parity/proposal.md:38)

**問題点**: `uiStyle` は What Changes の11番ですが、「リスクは10」と記載されています。

**推奨修正**: 11番へ修正するか、番号依存を避けて項目名で参照してください。

---

### [🔵 Suggestion] メニュー文言とタイトルの単一管理を設計へ落とす

**該当箇所**: [exploration.md:30](kasane/changes/align-sample-parity/exploration.md:30)、[samples-ios/spec.md:13](kasane/changes/align-sample-parity/specs/samples-ios/spec.md:13)

**問題点**: exploration は二重管理の解消方法を propose で設計するとしていますが、現在の成果物は「一致させる」という結果だけで、再発防止策がありません。

**推奨修正**: platformごとに route／画面記述子へ表示名を一度だけ定義し、メニュー項目と画面タイトルの双方がそこから参照する設計を `design.md` と tasks に追加してください。

## アクションプラン

1. 変更級を L に再分類し、DatePicker・Theme・文字列管理・検証方式を `design.md` で決定する。
2. Store デモを含む全6デモ画面の parity 棚卸しをやり直し、漏れを spec／tasksへ反映する。
3. DatePicker の platform 中立文言と `todayText` の扱いを決める。
4. イベント文字列、日付状態、Androidメニュー見出しを明文化する。
5. UI lintに従って brief／mockとデルタスペックの責務を分離する。
6. 全Scenarioに対応する検証マトリクスを tasksへ追加してから実装へ進む。

静的レビューのみ実施し、ファイル変更・ビルド・テストは行っていません。



## 突き合わせ結果 (ホスト側判定: 2026-07-31)

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| Major 1 | 変更級 M が L 基準 (複数能力横断) と矛盾 | **未解決 → ユーザー判断** | ksn-core の字面上は L が正 (samples-ios + samples-android の2能力)。級の確定はユーザーのため再提示 |
| Major 2 | Store デモの文言不一致 (Sample Row vs Sample Label) が漏れ | **採用** | samples-android spec に Requirement 追加、tasks 2.3 追加。ホスト側棚卸しの見逃し |
| Major 3 | DatePicker の完全一致が現行 API (todayText 無し) と両立しない | **採用 → 方針はユーザー判断** | 実コード裏取りあり根拠強。中立文言化 / deviation 記録 / 本体変更 の3択を提示 |
| Major 4 | 「最後のイベント」の受け入れ値未定義 | **採用 (軽減形)** | 全 Cell の期待文字列表は過剰と判断し、文言形式規則 + 非更新条件 + Scenario 2件を spec に定義 |
| Major 5 | タイトル対象数の 6/7 矛盾、Minimal Diffable 未対応 | **採用** | proposal / tasks を全7画面に統一、Minimal Diffable 検証のタイトル修正を明記 |
| Major 6 | 検証計画で Scenario 合否を判定できない | **採用 (軽減形)** | 完全な検証マトリクスは過剰と判断し、tasks 1.9 / 2.7 に照合対象と動的 Scenario の操作確認を明記 |
| Major 7 | デルタスペックが UI lint 違反 (構成・形式の記述) | **降格** | sample-parity 規約自体が「画面構成の一致 (Section 数・Cell 数・並び順・パラメータ)」を保証事項と定義しており、本変更ではそれが観察可能な契約そのもの。px・色・配置の記述は spec に無く、Theme は brief/mock 側に分離済み。mock のカバレッジ不足の実質は Major 6 で吸収 |
| Minor 1 | Android の「デモ」見出し未規定 | **採用** | samples-android spec に見出し文言一致と検証グループ非保持 (許容差) を明記 |
| Minor 2 | 予約日が誕生日と状態共有 | **採用** | 独立状態に分離、初期値 2026/06/01 を spec に定義。良い検出 |
| Minor 3 | Impact のリスク参照番号ずれ | **採用** | 番号依存をやめ項目名参照に変更 |
| Suggestion | メニュー文言・タイトルの単一管理設計 | **採用 (tasks レベル)** | tasks 1.4 に表示名定義の一元化を明記。design.md への昇格は級判断 (Major 1) に従属 |

採用 9 / 降格 1 / 未解決 (ユーザー判断) 2

# セカンドオピニオン: add-maui-basic-input-cells (001)
**相方**: codex / **日付**: 2026-08-10 / **対象**: 提案一式 (proposal / design / specs ×3 / tasks) — spec-review モード
---
# レビュー結果: add-maui-basic-input-cells

**日付**: 2026-08-10  
**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 1 / Major 9 / Minor 2 / Suggestion 0

## サマリー

変更の目的と大枠は明確ですが、interop の型構造、Cell identity、公開 API、非同期画像解決など、実装開始前に確定すべき契約が残っています。特に per-type DTO を既存の「LabelCell 固定コンテナ」でどう混載するかが未設計で、このままでは両OSの Bridge API を確定できません。

指定どおり静的レビューのみ実施し、ビルド・テスト・ファイル作成は行っていません。

## 指摘事項

### [🔴 Critical] per-type DTO を混載する共通輸送型が定義されていない

**該当箇所**: [design.md:15](kasane/changes/add-maui-basic-input-cells/design.md:15)、[maui-bridge/spec.md:9](kasane/changes/add-maui-basic-input-cells/specs/maui-bridge/spec.md:9)

**問題点**: 11種を個別 DTO とする方針はありますが、それらを1つの Sectionや`replaceCells`へ混載する型がありません。現状はiOS/Androidとも`KsBridgeSection.cells`と`KsBridgeCellUpdate.cell`が`KsBridgeLabelCell`固定です。`ToDto()`の型スイッチ後の戻り型、RootBuilder・insert/replace API、C# binding上の共通型も未定義です。単一wide DTOを禁止しただけでは、異種DTOのコンテナを実装できません。

**推奨修正**: 両OSとC# bindingで成立する共通基底型・protocol/interface、またはCell種別ごとのadd/replace APIを選定し、Section・Builder・CellUpdate・gatewayの全シグネチャをdesignへ明記してください。最初のprobeにも「異種Cellを含むSectionとbatch updateがbindingを越える」検証を追加してください。

### [🟠 Major] replaceSectionのcellId温存範囲と対応規則が未定義

**該当箇所**: [design.md:87](kasane/changes/add-maui-basic-input-cells/design.md:87)、[maui-bridge/spec.md:63](kasane/changes/add-maui-basic-input-cells/specs/maui-bridge/spec.md:63)

**問題点**: Requirementはすべての`replaceSection`で配下cellIdを温存すると読めますが、Cell追加・削除・移動・別インスタンスへの置換時に、旧IDをどのCellへ対応させるか決まっていません。現行実装はSection IDだけ維持し、配下Cellを再採番します。designの「同一KsBridgeSectionハンドルを更新」も、現行gatewayが置換ごとに新DTOを生成する構造との接続が説明されていません。

**推奨修正**: 要求を「同じfacade Sectionの`IsVisible`変更」に限定するか、Cellオブジェクトidentityを使う対応規則を定義してください。追加・削除・移動・置換それぞれのID期待値をScenario化する必要があります。

### [🟠 Major] 公開APIが列挙されず、受け入れ判定不能

**該当箇所**: [maui-cells/spec.md:9](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:9)、[maui-cells/spec.md:25](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:25)、[maui-cells/spec.md:135](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:135)、[maui-cells/spec.md:151](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:151)

**問題点**: 「nativeと同じ状態フィールド」「Theme相当」「`TitleColor`等」という表現では、公開する名前・型・nullability・既定値・BindablePropertyのbinding modeを判定できません。例えばnativeに存在するTime/Dateの`Format`、Entryの`AccentColor`、Buttonの`TitleAlignment`などを公開するかが不明です。ADR-0004がspecの責務としている項目対応表もありません。

**推奨修正**: Cell種別、CellStyle、SettingsView Themeについて、`MAUI名 / 型 / nullability / 既定値 / binding mode / nativeフィールド / OS固有時の扱い`の完全な対応表をspecまたはdesignへ追加してください。

### [🟠 Major] 「双方向8プロパティ」と実際の対象集合が矛盾

**該当箇所**: [proposal.md:9](kasane/changes/add-maui-basic-input-cells/proposal.md:9)、[design.md:40](kasane/changes/add-maui-basic-input-cells/design.md:40)、[maui-cells/spec.md:51](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:51)、[tasks.md:45](kasane/changes/add-maui-basic-input-cells/tasks.md:45)

**問題点**: proposal/tasksは8プロパティとしていますが、specはSimpleCheckを追加し、Pickerを`SelectedIndex`と`SelectedIndices`に分けており、列挙上は10プロパティです。一方ADR-0012はPickerを`SelectedItem`として数えています。SimpleCheckはAiForms由来ではOneWayですが、今回TwoWayへ変更するのかも明記されていません。

**推奨修正**: 書き戻し対象の正規一覧を1つに確定し、各BindablePropertyの既定binding modeを規定してください。Pickerのsingle/multiple、`SelectedItem`との関係も個別Scenarioにしてください。

### [🟠 Major] IconSourceの解決所有者とlatest-wins保証が欠落

**該当箇所**: [design.md:70](kasane/changes/add-maui-basic-input-cells/design.md:70)、[maui-cells/spec.md:117](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:117)

**問題点**: designはCellのHandler接続を前提とする書き方ですが、既存facadeではHandlerを持つのはSettingsViewだけで、Section/Cellはlogical treeに載りません。`ImageSourcePartLoader`へどのHandler/MauiContextを渡すかが決まっていません。また、Aの解決中にBまたはnullへ変更した場合、遅れて完了したAが新しい状態を上書きできます。

**推奨修正**: SettingsView/controllerがMauiContextを供給して解決を所有するなど、既存Handler構造に合う責務を定義してください。Cellごとの世代番号またはキャンセルによるlatest-wins、切断・再接続、A→B、A→null、失敗後再試行をScenarioとテストへ追加してください。

### [🟠 Major] sample-parityの上位規約と恒久的に矛盾する

**該当箇所**: [proposal.md:16](kasane/changes/add-maui-basic-input-cells/proposal.md:16)、[samples-maui/spec.md:9](kasane/changes/add-maui-basic-input-cells/specs/samples-maui/spec.md:9)、[samples-maui/spec.md:15](kasane/changes/add-maui-basic-input-cells/specs/samples-maui/spec.md:15)

**問題点**: 現行sample-parityは全platformで画面集合を一致させる規約ですが、本提案はStore/DSLをNon-Goalとし、Scenario内の括弧書きだけで欠落を許容しています。CustomCellは後続phaseで追随できますが、Store/DSLは恒久例外になるため、単なる一時的片側先行ではありません。「蒸留時に例外を追加」では、実装時点の受け入れ基準との衝突を解消できません。

**推奨修正**: Store/DSLも追加するか、sample-parityへ「そのplatformで公開されない利用方式は対象外」とする正式な例外を本提案段階で合意・仕様化してください。CustomCellは後続change IDまで追跡してください。

### [🟠 Major] ICommandのCanExecute契約がない

**該当箇所**: [maui-cells/spec.md:39](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:39)、[tasks.md:46](kasane/changes/add-maui-basic-input-cells/tasks.md:46)

**問題点**: specはタップ時に`Command.Execute`を呼ぶとだけ規定し、`CanExecute`がfalseの場合や`CanExecuteChanged`後の表示状態を決めていません。このまま字義どおり実装すると、実行不可のICommandも実行されます。`IsEnabled`との優先関係、`Tapped`の発火順も不明です。

**推奨修正**: 実効有効状態を`IsEnabled && Command.CanExecute(parameter)`とするか、少なくともExecute前にCanExecuteを確認する契約を追加してください。CanExecute=false、CanExecuteChanged、TappedとCommandの順序をScenario化してください。

### [🟠 Major] interaction delegate/listenerの寿命管理が未設計

**該当箇所**: [design.md:20](kasane/changes/add-maui-basic-input-cells/design.md:20)、[design.md:32](kasane/changes/add-maui-basic-input-cells/design.md:32)、[maui-bridge/spec.md:17](kasane/changes/add-maui-basic-input-cells/specs/maui-bridge/spec.md:17)

**問題点**: 初のcallback方向interopであるにもかかわらず、iOS delegateのweak/strong、Android listenerの保持、managed peerのroot化、解除時機、Store内Cell callbackとの循環参照回避が決まっていません。未設定時の安全性だけでは、GC後のcallbackやSettingsViewリークを防げません。

**推奨修正**: 所有グラフと登録・解除時機をdesignへ明記してください。Handler切断・再接続、SettingsView回収、listener差し替え、dispose後通知についてWeakReferenceを使った寿命テストをtasksへ追加してください。

### [🟠 Major] Keyboard・nullable enum・不正文字列の輸送契約が不足

**該当箇所**: [design.md:56](kasane/changes/add-maui-basic-input-cells/design.md:56)、[design.md:64](kasane/changes/add-maui-basic-input-cells/design.md:64)、[maui-bridge/spec.md:33](kasane/changes/add-maui-basic-input-cells/specs/maui-bridge/spec.md:33)

**問題点**: `Microsoft.Maui.Keyboard`は単純なenumではありませんが、正規化enumの値と各Keyboard/native型の対応表がありません。iOSの`@objc`境界でnullable scalar enumをどう表すかも未定です。また、不正な日付・時刻文字列、範囲外index、未知enumに対するno-op・fallback・例外の区別が決まっていません。

**推奨修正**: wire enumの数値とMAUI/iOS/Android対応表、nullable表現（例: boxed number）、InvariantCultureでの厳密なISO処理、変換失敗時の挙動を規定してください。正常系だけでなく未知値・不正形式のScenarioを追加してください。

### [🟠 Major] proposalのdomain指定が変更範囲と一致しない

**該当箇所**: [proposal.md:25](kasane/changes/add-maui-basic-input-cells/proposal.md:25)、[proposal.md:32](kasane/changes/add-maui-basic-input-cells/proposal.md:32)

**問題点**: proposalは`domain: maui`ですが、変更対象には`ios/`と`android/`の公開Bridgeモジュールが含まれます。Kasaneのdomain規約では複数domainをまたぐproposalは`cross`です。現指定のままでは実装時の用途キー解決でSwift/Kotlinのplatformスキルが結合されません。

**推奨修正**: `domain: cross`へ変更し、実際に触る`maui + ios + android`のdomain-skillsを結合する前提を明示してください。

### [🟡 Minor] DataTemplateSelectorの適用範囲とcontainerが曖昧

**該当箇所**: [design.md:92](kasane/changes/add-maui-basic-input-cells/design.md:92)、[maui-cells/spec.md:175](kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:175)

**問題点**: SettingsView直下のSection生成とSection配下のCell生成の両方に`ItemTemplate`がありますが、Scenarioは後者だけです。`SelectTemplate(item, container)`のcontainer、null返却、誤った型の生成、ItemsSource Replace時の再選択も未定義です。

**推奨修正**: 両階層への適用有無とcontainerを明記し、各階層1件ずつScenarioを追加してください。null・型不一致時は既存DataTemplateと同じ例外契約に揃えてください。

### [🟡 Minor] tasksに最終検証ゲートと一部異常系がない

**該当箇所**: [tasks.md:62](kasane/changes/add-maui-basic-input-cells/tasks.md:62)、[tasks.md:71](kasane/changes/add-maui-basic-input-cells/tasks.md:71)

**問題点**: テスト追加タスクはありますが、MAUI unit test、iOS Simulator全件、Android全件、両binding/sample buildを実行して件数を確認する最終タスクがありません。IconSource解決失敗、非表示中の内容変更、delegate寿命など、Requirementにある異常・lifecycle系の検証も明示されていません。

**推奨修正**: platform別の最終ビルド・全テスト・実行件数確認を独立タスクにし、上記の不足Scenarioをテストタスクへ割り当ててください。

## アクションプラン

1. per-type DTOの共通輸送型とreplaceSectionのidentity規則を先に確定する。
2. 公開API・binding mode・wire形式を完全な対応表として固定する。
3. IconSourceとinteraction callbackの所有・寿命・競合規則を設計する。
4. sample-parity例外と`domain:`をオーナー判断で確定する。
5. Scenarioとtasksを補完後、実装着手前に再レビューする。


## 突き合わせ結果 (2026-08-10)

ホスト側自己レビュー (2周、radio group 同期・ui/ 省略明記を検出済み) との突き合わせ。相方のみの指摘が大半で、根拠の強いものを採用する。

| # | 指摘 | 採否 | 判定理由 |
|---|---|---|---|
| 1 | (Critical) per-type DTO 混載の共通輸送型未定義 | **採用** | 根拠強。KsBridgeSection.cells / KsBridgeCellUpdate が LabelCell 固定である事実と整合。共通基底 DTO 型の Decision を design へ追加し probe にも混載検証を足す |
| 2 | replaceSection の cellId 温存範囲 | **採用** | 根拠強。現行実装は配下 Cell を再採番しており、全面温存の Requirement は過大。IsVisible/内容変更の同一 Section 差し替えに限定し対応規則を明記 |
| 3 | 公開 API の対応表なし | **採用 (縮小)** | 受け入れ判定可能性の観点は妥当。phase-2 の aiforms-surface-inventory を参照した対応表を design へ追加 (完全表の再作成はしない) |
| 4 | 双方向「8プロパティ」と実集合の矛盾 | **採用** | 根拠強。正規一覧 (10プロパティ: SimpleCheck 含む、Picker は single/multi 別) と binding mode 既定を確定 |
| 5 | IconSource の解決所有者と latest-wins 欠落 | **採用** | 根拠強。Cell は Handler を持たない — SettingsView/controller が MauiContext を供給し解決を所有する形へ設計修正。世代番号の latest-wins と競合 Scenario を追加 |
| 6 | sample-parity と恒久矛盾 | **部分採用** | 例外化の合意自体は phase-4 agenda 論点8でオーナー確定済み (蒸留時に concepts 改訂)。spec に例外の根拠と改訂予定を明記して受け入れ基準を自足させる。規約改訂タイミングの前倒しは不採用 (Kasane フローどおり) |
| 7 | ICommand の CanExecute 契約なし | **採用** | 根拠強。MAUI 標準契約。実効有効 = IsEnabled && CanExecute、Scenario 追加 |
| 8 | delegate/listener の寿命管理未設計 | **採用** | 根拠強。初のコールバック方向 interop。所有グラフ・解除時機を design へ、寿命テストを tasks へ |
| 9 | Keyboard/nullable enum/不正文字列の契約不足 | **採用** | wire 対応表・nullable 表現・InvariantCulture・失敗時挙動を design へ規定 |
| 10 | domain: maui は cross であるべき | **降格 (部分採用)** | concepts/maui/index.md が Bridge 層を maui ドメインと定義済みで、蒸留物 (ADR-0011〜0013・native-bridge.md) の行き先も maui。phase-1 が cross だったのは iOS Store 本体 (core 契約) に触れたためで、本 change は Native core/UI 非接触。domain: maui を維持。ただし実装時に ios/android の impl スキル (swift/kotlin) を併読する注記は tasks へ反映 |
| 11 | (Minor) Selector の適用範囲と container | **採用 (軽)** | 両階層の Scenario と container・null 時の契約を明記 |
| 12 | (Minor) 最終検証ゲートタスクなし | **採用 (軽)** | platform 別の最終ビルド・全テスト実行タスクを追加 |

**集計**: 採用 10 (うち部分採用 2) / 降格 1 / 未解決 0

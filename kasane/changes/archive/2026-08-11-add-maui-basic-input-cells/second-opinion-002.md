# セカンドオピニオン: add-maui-basic-input-cells (002 — 反映後の再レビュー)
**相方**: codex / **日付**: 2026-08-10 / **対象**: second-opinion-001 反映後の提案一式 — spec-review モード (新規セッション)
---
# レビュー結果: add-maui-basic-input-cells

**判定**: `NEEDS_DISCUSSION`  
**内訳**: Critical 0 / Major 10 / Minor 4 / Suggestion 0

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。公開 API、interop の寿命、既存 Theme 契約、sample-parity との整合に実装前の決定が必要です。

## 指摘事項

### [🟠 Major] 公開 API と DTO の正確な形が確定していない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:9`、`:25`、`:153`、`kasane/changes/add-maui-basic-input-cells/specs/maui-bridge/spec.md:9`、`kasane/changes/add-maui-basic-input-cells/design.md:91`  
**問題点**: 「native と同じ状態フィールド」「等」「対応概念がある項目のみ」では、名前・型・nullability・既定値・BindingMode・OS別対応を判定できません。特に Picker の公開面、Time/Date の `Format`、各 Cell の `ValueText` / `AccentColor`、Theme 全フィールドが閉じていません。design が正とする phase-2 inventory も派生11 Cell の棚卸しを含んでいません。  
**推奨修正**: 11 Cell、CellBase/CellStyle、SettingsView Theme、各DTOについて `MAUI名 / 型 / 既定値 / BindingMode / wire型 / iOS対応 / Android対応` の表を変更内に置き、`CellShapeTests` と変換テストを表の全行に対応させてください。

### [🟠 Major] IconSize / IconRadius の扱いが既存判断と矛盾する

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/design.md:80`、`:91`、`kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:167`  
**問題点**: design は「native に対応概念がないため公開しない」としますが、正と指定した inventory は両項目を A 分類としており、native `CellStyle` にも `iconSize` / `iconRadius` が存在します。  
**推奨修正**: 公開するか明示的に除外するかを決めてください。除外するなら ADR-0008 の A 分類から外す根拠とOS差を記録し、inventory 参照をそのまま「正」としないでください。

### [🟠 Major] 共通基底 DTO 化は additive ではない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/proposal.md:24`、`kasane/changes/add-maui-basic-input-cells/design.md:17`、`:127`  
**問題点**: `KsBridgeSection.cells`、`addCell`、`KsBridgeCellUpdate.cell`、`insertCell` / `replaceCell` 等の型を `KsBridgeLabelCell` から `KsBridgeCell` へ置換すると、Swift/Kotlin/C# binding の既存シグネチャが変わります。少なくとも source compatibility、Android では JVM descriptor を含む binary compatibility に影響し得るため、「Bridge も additive」は成立しません。  
**推奨修正**: 既存オーバーロードを残す、または未配布APIとして破壊変更を明示してください。Impact と Migration Plan に互換性評価を追加してください。

### [🟠 Major] 不正な ISO 値の「現値維持」が操作別に成立しない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/design.md:63`、`:66`、`kasane/changes/add-maui-basic-input-cells/specs/maui-bridge/spec.md:39`、`:43`  
**問題点**: design はパース失敗を「現値維持」、Scenario は「日付だけ無視して他フィールドを反映」とします。しかし `setRoot` / insert には現値がなく、native Date/Time Cell の値は必須です。replace で値だけ温存するにも、現在の単純な DTO→Cell 変換では旧 Cell とのマージができません。  
**推奨修正**: `setRoot`・insert・replace・native通知の方向別に、操作全体を拒否するのか、既定値へ落とすのか、旧値とマージするのかを規定してください。各OSで同一結果になる Scenario が必要です。

### [🟠 Major] SelectedIndices の集合意味論が未定義でエコー収束を保証できない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/design.md:54`、`kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:63`、`:83`、`kasane/changes/add-maui-basic-input-cells/specs/maui-bridge/spec.md:41`  
**問題点**: MAUI は `IList<int>`、native は `Set<Int>` です。同値チェックを参照等価や順序付き比較にすると、同じ集合でも配列順が違うたびに再配信され、収束保証が崩れます。重複、通知順、範囲外値の集合内での扱いも未定義です。  
**推奨修正**: wire は昇順・重複除去、入口同値は集合等価など、canonicalization と比較規則を明記してください。順序違い・重複・範囲外を含む往復 Scenario を追加してください。

### [🟠 Major] Theme の表示中更新要求が既存 iOS 契約と衝突する

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/design.md:89`、`:91`、`kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:151`、`:161`  
**問題点**: Scenario は表示中の Theme 変更で「表示が新しい Theme で更新」と要求しますが、既存契約は iOS の表示済み Header/Footer への即時再適用を保証していません（`kasane/concepts/core/styling/style-resolution.md:61`）。一方、計画は既存 `setTheme` 経路を使うだけで、native Host 改修をスコープ化していません。  
**推奨修正**: Header/Footer を即時更新対象外として明記するか、iOS Host 改修とテストを Impact/tasks に追加してください。Theme フィールド別の動的更新保証も表で確定してください。

### [🟠 Major] interaction listener の解除時点に実在する lifecycle がない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/design.md:35`、`kasane/changes/add-maui-basic-input-cells/tasks.md:58`  
**問題点**: design は「facade の破棄（Bridge dispose）時」に解除するとしますが、現行 facade/gateway は破棄口を持たず、Handler 切断時は `releaseHost` だけです（`maui/KsSettingsView.Maui/Internals/IKsSettingsGateway.cs:14`、`maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:90`）。Android Bridge の strong listener と managed peer の相互保持を、どの経路で確実に切るか決まっていません。  
**推奨修正**: Handler connect/disconnect に登録・解除を結び付ける、明示的な所有者の dispose 経路を設ける、または Android 側も weak 化するなど、実在する lifecycle を選んでください。切断→再接続、回収、旧listener無通知を検証対象にしてください。

### [🟠 Major] IconSource 解決が Handler 世代をまたぐ場合の所有規則がない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/design.md:80`、`:81`、`kasane/changes/add-maui-basic-input-cells/tasks.md:41`、`:57`  
**問題点**: latest-wins は IconSource の変更世代しか扱いません。Android Activity を使った解決中に Handler が切断・再接続された場合、旧 Context 由来の Drawable が後から採用される可能性があります。また image service result の dispose、置換された platform 画像の所有・解放も未定義です。  
**推奨修正**: Handler/MauiContext の世代も競合判定へ含め、切断時のキャンセル・結果破棄・再接続時の再解決を規定してください。service result と platform 画像の所有者・解放時点も設計に追加してください。

### [🟠 Major] Section 配下 DataTemplateSelector の container 要求が構築順序と両立しない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/design.md:103`、`:105`、`:106`、`kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:193`  
**問題点**: Section は SettingsView へ配置される前でも ItemsSource/ItemTemplate 設定時に即座に Cell を生成できます。その時点では「所属 SettingsView」を container として渡せません。現行 binder はプロパティ設定時に即生成するため、この仕様のままでは構築順序によって契約を満たせません。  
**推奨修正**: 未配置 Section は生成を保留する、仮containerを許可する、配置時に全再生成する、のいずれかを決めてください。未配置・配置・別SettingsViewへの再配置・item Replace の Scenario を追加してください。

### [🟠 Major] Store / DSL デモ除外が現在の sample-parity 規約に反する

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/proposal.md:16`、`kasane/changes/add-maui-basic-input-cells/specs/samples-maui/spec.md:9`、`:11`、`:17`  
**問題点**: 現行規約が許すのは platform 固有の技術検証画面か、追随予定のある一時的な片側先行です。MAUI に公開 API を作らない恒久的除外は、現在の `sample-parity` にはありません。「蒸留時に例外を追加して正当化」は、実装時点の上位規約違反を事後承認する形になります。  
**推奨修正**: 実装前に例外を正式決定するか、Store/DSL 画面を技術検証として再分類するか、MAUI 側の追随計画を残すかを選択してください。

### [🟡 Minor] ButtonCell の Description 非公開条件が検証不能

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:9`、`kasane/changes/add-maui-basic-input-cells/tasks.md:33`  
**問題点**: `Description` は公開 `CellBase` から継承されるため、ButtonCell から完全には除去できません。`private new` で直接アクセスを隠しても、`CellBase` へキャストすれば設定できます。  
**推奨修正**: 「ButtonCell 型から直接公開しない」「基底経由で設定されてもDTOへ輸送せず表示しない」など、実現可能な契約へ具体化してください。

### [🟡 Minor] Command 交換時の購読解除契約がない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/design.md:111`、`:113`、`kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:41`、`kasane/changes/add-maui-basic-input-cells/tasks.md:52`  
**問題点**: `CanExecuteChanged` 追随は規定されていますが、Command 差し替え、Cell 除去、SettingsView 回収時に旧 Command の購読を解除する条件がありません。旧 Command の通知による誤更新やリークを検出できません。  
**推奨修正**: 旧Commandの通知が無視されることと、Cell/SettingsViewの回収を妨げないことを Scenario・テストへ追加してください。

### [🟡 Minor] 非表示中の内容更新に対応する Scenario がない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:177`、`:179`、`kasane/changes/add-maui-basic-input-cells/tasks.md:54`  
**問題点**: Requirement は非表示中の内容変更を復帰後に反映すると要求しますが、Scenario とテストタスクは可視性切替・ID温存・書き戻しだけです。  
**推奨修正**: 非表示中に Cell の値・文言・構造を変更し、再表示後に最新状態となる Scenario を追加してください。

### [🟡 Minor] Android Calendar の Host 前提が facade Scenario に現れない

**該当箇所**: `kasane/changes/add-maui-basic-input-cells/specs/maui-cells/spec.md:109`、`:113`、`kasane/changes/add-maui-basic-input-cells/design.md:72`  
**問題点**: maui/ADR-0013 は Android Material 形式に `FragmentActivity` 要求などの固有前提が付随するとしていますが、Scenario は無条件に選択面が開くよう読めます。  
**推奨修正**: 対応 Host の前提と、満たさない場合の挙動（明示例外、安全なfallback等）を facade/Bridge の契約へ追加してください。

## アクションプラン

1. 公開 API・DTO・既定値・BindingMode の対応表を確定する。
2. listener と IconSource の lifecycle/所有モデルを確定する。
3. ISO失敗、複数選択、DataTemplateSelector の境界挙動を Scenario 化する。
4. Theme の iOS 更新範囲と sample-parity 例外をオーナー判断する。
5. Bridge の互換性評価を Impact/Migration Plan に反映する。
6. Minor の不足 Scenario を tasks の具体的テスト項目へ接続する。


## 突き合わせ結果 (2026-08-10)

Critical は 0 に収束。指摘14件の採否:

| # | 指摘 | 採否 | 判定理由 |
|---|---|---|---|
| 1 | 公開 API の完全対応表が無い (再指摘) | **降格 (部分採用)** | 完全表は腐り度原則 (実装と共に腐る詳細) により作らない。spec の列挙 + inventory + CellShapeTests を正とする構成を維持。列挙の穴 (Time/Date の Format) は spec に追記 |
| 2 | IconSize / IconRadius が inventory A 分類・native CellStyle と矛盾 | **採用** | コードで実在を確認 (CellStyle.swift:36-38)。design の「対応概念なし」は誤りと訂正し、CellStyle 系プロパティとして公開に転換 |
| 3 | 共通基底 DTO 化は additive でない | **採用** | 正しい。Bridge は未配布・facade 経由のみが公開契約のため source-breaking を許容と proposal Impact に明記 |
| 4 | 不正 ISO の「現値維持」が操作別に破綻 | **採用** | 正しい (setRoot/insert に現値なし)。操作によらず既定値 (00:00 / 1970-01-01) fallback + DEBUG 診断へ単純化 |
| 5 | SelectedIndices の集合意味論未定義 | **採用** | wire = 昇順・重複除去、入口同値 = 集合等価に規定。往復 Scenario 追加 |
| 6 | Theme 表示中更新が iOS 既存契約と衝突 | **採用** | concepts (style-resolution.md) で確認 — 表示済み Header/Footer の即時再適用は保証外。Scenario を既存契約準拠に修正 (native Host 改修はスコープ外のまま) |
| 7 | listener 解除の実在 lifecycle なし | **採用** | 登録 = Handler connect、解除 = disconnect (releaseHost) に結び付け (操作通知は Host 表示中のみ発生するため必要十分) |
| 8 | IconSource の Handler 世代またぎ | **採用** | MauiContext 世代を競合判定に含め、切断時キャンセル・再接続時再解決を規定 |
| 9 | Section 配下 Selector の container が構築順序と両立しない | **採用** | 正しい (Section は未配置でも即生成)。container = テンプレート設定先の BindableObject へ修正 |
| 10 | Store/DSL 除外は現行規約違反・事後承認 (再指摘) | **降格** | 例外化は phase-4 agenda 論点8のオーナー決定として正式に記録済み (history)。規約本文の追従が蒸留時なのは Kasane の通常フロー (concepts 更新は蒸留) であり事後承認ではない。spec に例外根拠を明記済み。前倒し改訂 (ksn-concept) の選択肢はオーナーに提示 |
| 11 | (Minor) ButtonCell の Description 非公開が検証不能 | **採用** | 「輸送・表示しない」契約へ具体化 |
| 12 | (Minor) Command 交換時の購読解除なし | **採用** | design Decision 11 と Scenario に追加 |
| 13 | (Minor) 非表示中の内容更新 Scenario なし | **採用** | Scenario とテスト観点を追加 |
| 14 | (Minor) Android Calendar の FragmentActivity 前提 | **採用** | native 契約準拠 (facade は追加保証しない) を spec に注記 |

**集計**: 採用 12 (うち部分 1) / 降格 2 / 未解決 0

**収束判定**: Critical 0・採用分は全件反映済み・降格2件は方針判断済み (レビュー3周目は回さない — 自己レビュー2周上限と同じ収束規律を外部レビューにも適用)。

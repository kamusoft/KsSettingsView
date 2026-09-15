# セカンドオピニオン: maui-cell-appthemebinding-logical-child (spec-001)
**相方**: codex / **label**: so-spec-maui-cell-appthemebinding-logical-child / **日付**: 2026-09-15 / **対象**: kasane/changes/maui-cell-appthemebinding-logical-child/ の proposal.md / design.md / specs/ / tasks.md (提案一式、自己レビュー 2 周後)
---
# レビュー結果: maui-cell-appthemebinding-logical-child

**日付**: 2026-09-15  
**判定**: **NEEDS_DISCUSSION**

## サマリー

論理子化の目的と解除経路は整理されていますが、「同一 Cell を別 SettingsView に配置できる」という現行実装上の穴を契約として残すと、単一の `Parent`・BindingContext・ページ Resources を複数所有者へ同時に成立させられません。加えて、所有集合と controller による再所有の状態同期、native 送信との順序が設計されておらず、このままでは仕様どおり実装できない可能性があります。

指摘は Critical 0 / Major 4 / Minor 4 です。静的レビューのためビルド・テストは実行していません。

## 照合した規約

- `cross/comment-policy.md`（always）
- `cross/test-execution.md`（テスト計画・完了報告）
- `cross/runtime-behavior-verification.md`（表示中の外観変更）
- `cross/sample-parity.md`（`samples/maui` の変更）
- `maui/integration-host-verification.md`（MauiHost の両 OS 検証）

## 指摘事項

### [🟠 Major] 別 SettingsView 間の多重配置契約は論理所有と両立しない

**該当箇所**: `specs/maui-core/spec.md:87`、`specs/maui-core/spec.md:99`、`design.md:47`、`kasane/concepts/maui/api/maui-rendering-lifecycle.md:43`

**問題点**: Requirement 冒頭では同一インスタンスの複数配置を禁止しつつ、別 SettingsView 間では検出せず、後から変換された側が `Parent` を奪うことを契約化しています。しかし `Parent` は1つしか持てないため、A/B 両方のコレクションに残る Cellを、それぞれの Section の論理子にはできません。

さらに、B への追加時は `Parent` をAのままBの BindingContextだけを配る仕様です。現行 binder は他所所有かを問わず `SetInheritedBindingContext` を実行するため（`maui/KsSettingsView.Maui/Internals/KsBindingContextBinder.cs:83`）、Aで正常表示中の Cell の binding値を、Bの変換前に変更し得ます。これは「多重配置の契約を変えない」「既存配置は無傷」と矛盾します。

**推奨修正**: 次のどちらかを仕様判断として確定してください。

- 推奨: SettingsView をまたぐ重複も変換前に拒否し、既存所有者の `Parent`・BindingContext・表示を変更しない。
- 共有を残す場合: 「最後に変換した所有者が勝つ」「他方の表示・ページ Resources は保証しない」と明記し、論理子・ページ Resources 追随の保証対象から共有インスタンスを除外する。

この選択は実装だけでは解決できないため、判定を `NEEDS_DISCUSSION` としています。

### [🟠 Major] 所有集合と controller による付け替えの状態が同期されない

**該当箇所**: `design.md:25`、`design.md:31`、`design.md:47`

**問題点**: binder は「自分が付けている子の集合」を持つ一方、他所所有の子を引き取る処理は controller の `RegisterSection` / `RegisterCell` に置かれます。Aの集合には子が残り、Bの集合には入らないまま `Parent` だけがA→Bへ変わるため、その後のReset・コレクション差し替え・除去で集合と実際の `Parent` が食い違います。

また、集合を単純な `HashSet` とすると、Host未接続中に同じインスタンスを2回追加し、そのうち1件だけ除去した場合、残る所属があるのに論理子を解除してしまいます。重複は変換時まで許容される設計なので、これは実際に到達可能です。

**推奨修正**: 論理所有の変更口を所有オブジェクトへ一本化し、controllerからは「検査済みの採用」を同じ器へ依頼してください。重複を変換時まで許すなら、参照数または現在のコレクション内容で所有継続を判定します。少なくとも以下をScenario化してください。

- A所有中にBへ追加→B変換→A/BそれぞれでReset・除去・コレクション差し替え
- Host未接続で同一要素を2回追加→1件だけ除去
- controllerによる付け替え後、旧所有者のResetが新しい所有を奪わない

### [🟠 Major] `RegisterSection` / `RegisterCell` では論理所有の確定がnative送信より後になる

**該当箇所**: `tasks.md:10`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1024`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1245`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1485`

**問題点**: task 1.2 は論理所有の確定を `RegisterSection` / `RegisterCell` に置いていますが、現行経路では次の順です。

- `gateway.SetRoot` → `RegisterSection`
- `gateway.InsertCell` → `RegisterCell`
- `gateway.ReplaceSection` → `RegisterCell`

他所所有の要素では、旧親の Resources で解決された値が先にnativeへ送られ、その後で新しい親へ付け替わります。付け替えによる再評価をcontrollerが購読する前に行えば更新を取りこぼし、購読後なら余分な二段更新になります。どちらにするかも仕様化されていません。

**推奨修正**: 「全件検査→新しい論理所有とBindingContextの確定→snapshot生成／gateway呼び出し」の順序を設計に明記してください。各Root追加・Cell追加・全体再構築・Cells差し替えについて、最初のgateway payloadが新所属先の値であることをテストします。

### [🟠 Major] 「任意の BindableProperty」の保証が広すぎ、受け入れ試験で判定できない

**該当箇所**: `specs/maui-core/spec.md:54`、`tasks.md:15`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1519`

**問題点**: Requirement は「任意の BindableProperty」の再評価値が内容更新としてnativeへ届き、行が作り直されないと保証しています。一方、Sectionには `Cells` / `ItemsSource` / `ItemTemplate` のような構造プロパティ、Cellにはcommandなど表示値でないプロパティがあり、すべてを同じ「内容更新」として扱うことはできません。現行controllerも `AffectsSnapshot`、icon、CustomCell content、Section更新を別経路に分けています。

テスト計画は `TitleColor` と `HeaderText` の2例だけで、次も未検証です。

- Application Resourcesからの`DynamicResource`
- 旧Resources変更が除去済みCellへ届かないこと
- `Application.UserAppTheme` と端末外観変更の双方
- 「行を作り直さない」の観測方法

**推奨修正**: 契約を次の2段に分けてください。

- facadeで定義する対象BindablePropertyについて、MAUI bindingが再評価される。
- 再評価後の反映経路は、そのプロパティの通常変更と同じ経路になる。

対象範囲またはプロパティ分類表を明示し、各分類の代表Scenario、Application/Page Resources、除去後の旧Resources不達を追加してください。「行を作り直さない」はgateway操作種別またはnative identityなど、判定可能な観測値で定義します。

### [🟡 Minor] `Cells` コレクション自体の差し替えを検証するScenarioがない

**該当箇所**: `design.md:32`、`tasks.md:14`

**問題点**: designは `Root` / `Cells` の再代入を明示していますが、specとtask 2.1が具体的に検証するのはRoot差し替えだけです。旧Cellsの購読解除・旧Cellの論理所有解除は主要な漏れリスクです。

**推奨修正**: `Section.Cells` を別コレクションへ再代入し、旧Cellがnull、新CellがSectionをParentに持ち、旧コレクションの以後の変更が影響しないScenarioを追加してください。

### [🟡 Minor] Reset Scenarioの例がNotifyCollectionChangedのイベント単位と一致しない

**該当箇所**: `specs/maui-core/spec.md:26`

**問題点**: `ObservableCollection.Clear()` のReset通知時点ではコレクションは空です。その後のX再追加は別のAdd通知なので、「Xを残しYを除いた状態でReset通知」の例にはなりません。

**推奨修正**: 次のいずれかにしてください。

- `Clear()` のReset直後はX/Yともnull、その後のAddでXだけ再所有される。
- Reset時点の最終内容がXだけになる独自observable collectionをGIVENで明示する。

### [🟡 Minor] 実装中に凍結対象のdesignを書き換える計画になっている

**該当箇所**: `design.md:100`、`tasks.md:18`、`tasks.md:38`

**問題点**: `x:Reference` の結果を「deviation.mdまたはdesign.md」に記録するとしていますが、proposal/design/specは実装開始後の凍結対象です。またdeviation.mdは調査結果一般の記録先ではなく、合意済み仕様からの乖離と付随修正の記録先です。

**推奨修正**: `x:Reference` を提案承認前に決着させるか、実装中はevidenceとconcepts更新へ記録してください。実際に合意仕様から逸脱する場合だけdeviation.mdを使い、design.mdへの実装中の追記は削除します。

### [🟡 Minor] changeのdomainとADR配置理由が一致していない

**該当箇所**: `proposal.md:39`、`kasane/decisions/cross/0032-maui-section-cell-as-logical-children.md:6`

**問題点**: proposalは`domain: maui`ですが、ADRはcore/ADR-0031をamendし、core/maui横断なのでcrossへ置くと説明されています。Kasaneのdomain規則では複数domainに該当するchangeはcrossであり、現在の記述だとスキル解決・蒸留先判断が不一致になります。

**推奨修正**: `domain: cross`へ直すか、変更domainをmauiに留めたままcross ADRを起こす例外理由と蒸留先をproposalに明記してください。

## アクションプラン

1. 別SettingsView間の同一インスタンス配置を禁止するか、last-owner-winsとして制限付きで契約化するかを決定する。
2. 所有の唯一の変更口、集合の多重度、controllerによる再所有時の両所有者の状態更新を設計し直す。
3. 論理所有確定をnative送信より前に置き、全変換経路の順序を固定する。
4. 「任意のBindableProperty」の保証範囲を分類し、Resources・外観・解除・Cells差し替えのScenarioを追加する。
5. Reset例、凍結対象への追記、domain欄を修正した後、提案を再レビューする。


## 突き合わせ結果

ホスト側の自己レビュー (2 周: チェックリスト → 前提の検算・実体との突合・責務の閉路) は指摘 0 で通過していたため、下記はすべて「相方のみ」の指摘。根拠 (該当箇所の特定・実害シナリオ) で採否を判定した。

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| Major 1 | 別 SettingsView 間の多重配置契約は論理所有と両立しない | **採用 (仕様判断としてオーナーへ)** | `Parent` は 1 つしか持てず、「引き取らないが BindingContext は配る」は A で表示中の Cell を B の変換前に触る。既存配置は無傷という契約と矛盾。実害シナリオが具体的 |
| Major 2 | 所有集合と controller の付け替えが同期しない / 同一要素の 2 回追加 + 1 件除去で誤解除 | **採用** | 所有の変更口が 2 つ (器と controller) に割れている設計の穴。2 回追加→1 件除去は Host 未接続で到達可能。所有継続の判定を「現在のコレクション内容」にし、変更口を器へ一本化する |
| Major 3 | 論理所有の確定が native 送信より後になる | **採用** | 現行経路は gateway 呼び出し → Register の順で、最初の payload が旧親 (または親なし) で解決した値になる。accessory と同じ「検査 → 確定 → snapshot → 配信」の順序を design に固定する |
| Major 4 | 「任意の BindableProperty」の保証が広すぎ判定できない | **採用** | 構造プロパティ (Cells / ItemsSource) や command は内容更新ではない。契約を「binding の再評価」と「反映は通常変更と同じ経路」の 2 段に分け、「行を作り直さない」を gateway 操作種別で定義する |
| Minor 1 | `Cells` 差し替えの Scenario が無い | **採用** | design に書いた経路が spec に無い漏れ |
| Minor 2 | Reset の例がイベント単位と一致しない | **採用** | `Clear()` の Reset 時点はコレクションが空。例を直す |
| Minor 3 | 凍結対象 design.md を実装中に書き換える計画 | **採用** | `x:Reference` は namescope 経由で `Parent` と別機構なので提案段階で「本 change の対象外」と決着させ、実測は evidence に残す。design.md への実装中追記を消す |
| Minor 4 | change の domain と ADR 配置理由の不一致 | **採用 (明記)** | domain は maui のまま (触るコード・concepts の行き先は maui)。ADR が cross にあるのは amends のドメイン横断規則によるものと proposal に明記する |

降格: なし / 未解決: Major 1 (オーナー裁定待ち)

---
## 2 ラウンド目 (改訂後の再レビュー、同 label への send)

# 再レビュー結果: maui-cell-appthemebinding-logical-child

**日付**: 2026-09-15  
**判定**: **NEEDS_DISCUSSION**

## サマリー

前回8件のうち7件は解消、1件は部分解消です。多重配置禁止への方針転換、所有変更口の一本化、native送信順、binding反映経路の分離は明確になりました。

一方、「他所所有中に追加し、後から旧所有が解除された要素」のHost未接続時の扱いが、Host非依存の論理所有契約と一致していません。ここは例外時点・自動引き取り・保証除外のどれを選ぶか設計判断が必要です。新規指摘は Major 3 / Minor 2です。静的レビューのためビルド・テストは実行していません。

## 前回指摘の追跡

| # | 前回指摘 | 判定 | 確認結果 |
|---:|---|---|---|
| 1 | 別SettingsView間の多重配置と単一Parentの矛盾 | **解消** | 別SettingsView間も変換時に例外とし、last-owner-winsを却下。所属時にParent/BindingContextへ触れない契約と、既存配置を保全するScenarioも追加されています。`design.md:47`、`design.md:59`、`specs/maui-core/spec.md:119` |
| 2 | 所有集合とcontroller再所有の不整合、重複要素1件除去 | **解消** | 論理所有の変更口を器へ一本化し、controllerは器へ依頼する形になりました。除去時の残存確認と「2回追加→1件除去」Scenarioも追加されています。`design.md:45`、`design.md:48`、`specs/maui-core/spec.md:38` |
| 3 | `RegisterSection` / `RegisterCell` がnative送信より後 | **解消** | 「全件検査→所有確定→snapshot→gateway」の順が明文化され、最初のpayloadを確認するScenarioとtaskが追加されています。`design.md:49`、`specs/maui-core/spec.md:144`、`tasks.md:10` |
| 4 | 全BindableProperty保証が広すぎ、反映経路が曖昧 | **解消** | binding再評価とnative反映を別Requirementに分け、内容・accessory・構造・非表示値の経路が分類されました。ページ／アプリResources、解除後の不達、gateway操作の検証も追加されています。`specs/maui-core/spec.md:66`、`specs/maui-core/spec.md:94`、`tasks.md:16` |
| 5 | `Cells`コレクション差し替えScenario欠落 | **解消** | 旧購読の無効化を含むScenarioとtaskが追加されています。`specs/maui-core/spec.md:33`、`tasks.md:15` |
| 6 | Reset Scenarioのイベント単位が不正確 | **解消** | `Clear()`のResetと、その後の`Add`を別のWHEN/THENとして記述しています。`specs/maui-core/spec.md:26` |
| 7 | 実装中のdesign書き換えとdeviation誤用 | **解消** | `x:Reference`はNon-Goalとして確定し、design凍結も明記されました。deviationも乖離・付随修正だけに限定されています。`proposal.md:24`、`design.md:103`、`tasks.md:39` |
| 8 | proposalのdomainとcross ADRの不一致 | **部分** | cross ADRとmaui conceptsに分かれる理由は追記されましたが、`domain:`値と説明を同じ行に書いており、定義済みdomain名として解釈できない形です。また複数domainなら`cross`とする規則自体にはなお一致していません。`proposal.md:41` |

## 照合した規約

- `cross/comment-policy.md`（always）
- `cross/test-execution.md`（テスト計画・完了報告）
- `cross/runtime-behavior-verification.md`（表示中の端末外観変更）
- `cross/sample-parity.md`（MAUI Sample変更）
- `maui/integration-host-verification.md`（MauiHost両OS検証）

## 指摘事項

### [🟠 Major] 旧所有解除後、変換まで論理子にならない空白がHost非依存契約と矛盾する

**該当箇所**: `design.md:47`、`design.md:49`、`design.md:51`、`specs/maui-core/spec.md:3`、`specs/maui-core/spec.md:144`

**問題点**: 次の順序では、Bだけが有効な所属先になった後も、Bが変換されるまでCellの`Parent`がnullのままです。

1. A所有中のCellを、Host未接続のBへ追加する。Bの器は何もしない。
2. AからCellを除去する。Aの器が`Parent`をnullにする。
3. Bにはコレクション変更が起きないため、Bの器は引き取らない。
4. BのHandler接続まで`Parent == null`が続く。

ところがRequirementは、`Root` / `Cells`への所属中はHandlerの有無に依らず論理子であるとしています。BindingContext変更時の`Apply`も「付け外しはしない」設計なので、この空白は別イベントでも解消されません。

**推奨修正**: 次のどれを契約にするか決定してください。

- 他所所有の要素は、Host未接続でもコレクション追加時に即座に例外にする。
- 他所所有中はpendingとして弱く監視し、旧`Parent`がnullになった時点でBが自動的に引き取る。
- pending配置は変換まで論理所有保証の対象外と明記し、`Remove`→`Add`による再追加を利用者に要求する。

現在の「変換時まで例外を遅らせる」と「所属期間は常に論理子」の両方は、そのままでは成立しません。

### [🟠 Major] Handler接続失敗時の「Bのnativeには何も配信されない」を現行順序では満たせない

**該当箇所**: `specs/maui-core/spec.md:134`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:196`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:203`

**問題点**: Scenarioは外部所有のCellを検出した際、「Bのnativeには何も配信されない」と要求しています。しかし現行`Connect`は、設定ツリーの検査を行う`RebuildRoot()`より先に`gateway.SetTheme()`と`gateway.SetStyle()`を呼びます。

したがって、Root/snapshotの送信を防いでもgatewayへの配信は既に発生しています。fake gatewayで「呼び出しゼロ」をassertすると必ず失敗し、仕様どおりにするには接続処理全体の順序変更が必要です。

**推奨修正**: 意図に応じて次のどちらかへ揃えてください。

- 「設定ツリー・構造・snapshotは何も配信されない」とTHENを限定し、Theme/Styleの先行設定は許容する。
- 本当に全配信ゼロを要求するなら、Root全件検査を`SetTheme` / `SetStyle`より前へ移す設計とtaskを追加する。

### [🟠 Major] 拡張した多重配置契約のうちSectionとCustomCell.Contentがテスト対象から漏れている

**該当箇所**: `specs/maui-core/spec.md:117`、`specs/maui-core/spec.md:149`、`tasks.md:18`

**問題点**: 新契約は別SettingsView間のSection・Cell・accessory View・`CustomCell.Content`をすべて対象にしていますが、task 2.3の回帰テストはCellとaccessory Viewだけです。Sectionと`CustomCell.Content`は別の検査経路を通るため、この2例だけでは契約全体を固定できません。

また、「設定ツリー外のfacade所有者」が、別SettingsViewから既に外れたSection／CustomCellも含むかが明示されていません。例えば、切り離されたSection AがCellを所有したまま、そのCellをBへ追加した場合も複数配置ですが、`Parent`の祖先に別SettingsViewは存在しません。

**推奨修正**:

- 別SettingsView間のSection共有
- 別SettingsView間の`CustomCell.Content`共有
- SettingsViewから外れたSection／CustomCellがまだ所有する要素の再配置

をScenarioまたはtaskへ追加してください。判定条件は「別SettingsViewの祖先があるか」ではなく、原則として「期待する所有者以外のfacade所有者を`Parent`が指しているか」と明文化するのが安全です。

### [🟡 Minor] `domain:`欄が有効なdomain名として解釈できない

**該当箇所**: `proposal.md:41`

**問題点**: `domain: maui   (説明...)`は、機械的・定型的に読むとdomain値全体が`maui (...)`になります。これは`config.yaml`に定義された`maui`でも`cross`でもありません。

さらに、このchangeはcross ADRを生成してcore ADRをamendするため、domain-axisの「複数該当ならcross」に照らすと`domain: cross`が自然です。

**推奨修正**: `domain: cross`を単独行にしてください。MAUI実装スキルを解決する必要は、cross changeが実際に触るdomainとしてmauiを結合する規則で扱えます。説明は次の段落へ分離してください。

### [🟡 Minor] Sample Requirementの対象範囲と「7ページ」が一致していない

**該当箇所**: `specs/samples-maui/spec.md:3`、`tasks.md:26`

**問題点**: Requirementは「MAUI Sampleのデモ画面」と全体を対象にしていますが、taskは7ページだけを共有Styleへ移します。現行`Pages/`には`SettingsView`を持つXAMLページが9件あり、対象外の2ページを保証外とする理由が仕様から判断できません。

**推奨修正**: Requirementで対象の7ページを列挙するか、「現在`SampleThemeFollower`を使用するページ」に限定してください。9ページすべてが対象なら、task 4.1・4.4も9ページへ合わせます。

## アクションプラン

1. add-before-removeによるpending配置を、即時例外・自動引き取り・保証除外のどれにするか決定する。
2. Handler接続失敗時の「配信なし」がRoot payloadだけか、Theme/Styleも含むかを明確にする。
3. Section・`CustomCell.Content`・切り離されたfacade所有者の多重配置Scenarioを追加する。
4. `domain:`を単独の有効値へ直し、Sample対象ページを仕様とtaskで一致させる。
5. 上記改訂後に再レビューする。


## 突き合わせ結果 (2 ラウンド目)

前回 8 件: 解消 7 / 部分 1 (Minor 4 の domain) — 相方の追跡どおり。

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| Major 1 | 旧所有解除後、変換まで `Parent` null の空白が Host 非依存契約と矛盾 | **採用** | 論理的な矛盾の指摘で反論の余地なし。3 択のうち「他所所有の要素はコレクション追加時に即例外」を採用 (最も単純で、pending 監視も保証除外も要らない。design Decision 2 / 3 と spec の MODIFIED を書き直し、変換経路の確定を廃止) |
| Major 2 | 「native に何も配信されない」が `SetTheme` / `SetStyle` 先行のため満たせない | **採用** | `Connect` の実装順 (controller:203) の突合で具体的。THEN を「構造・snapshot は配信されない (Theme / Style の先行は許容)」に限定 |
| Major 3 | Section / `CustomCell.Content` / 外れた所有者の Scenario 漏れ、判定条件の明文化 | **採用** | 契約が対象を列挙しているのにテストが 2 例のみは固定になっていない。3 Scenario 追加、判定条件を「期待する所有者以外の facade 所有者を `Parent` が指す」に統一 |
| Minor 1 | `domain:` 行が有効値として読めない / cross にすべき | **半採用** | 行を `domain: maui` 単独にし説明を段落へ分離 (採用)。`cross` への変更は降格: change が触るコード・concepts の行き先は maui で、cross にすると蒸留の行き先が誤る。ADR が cross なのは amends の規則 (proposal に明記) |
| Minor 2 | Sample Requirement の対象範囲と 7 ページの不一致 | **採用** | 9 ページ中 2 ページ (共通配色未適用) を除く根拠が spec に無かった。Requirement で 7 画面を列挙、tasks 4.1 にページ名と除外理由 |

降格: Minor 1 の `domain: cross` 部分 / 未解決: なし。3 ラウンド目は回さない (ksn-propose の周回上限)。

# セカンドオピニオン: add-maui-core (001 回目)
**相方**: codex / **日付**: 2026-08-08 / **対象**: 提案一式 (proposal.md / design.md / specs/maui-core/spec.md / tasks.md)
---
## 判定

**修正要求**です。静的レビューのみ実施し、ビルド・テスト・ファイル変更は行っていません。

Critical 1件、Major 8件、Minor 3件です。

## 指摘

1. **Critical — `IsVisible` 更新が Bridge 契約上禁止されたバッチ経路へ流れる**

   - 該当: [spec.md:65](kasane/changes/add-maui-core/specs/maui-core/spec.md:65)、[spec.md:107](kasane/changes/add-maui-core/specs/maui-core/spec.md:107)、[design.md:31](kasane/changes/add-maui-core/design.md:31)、[tasks.md:28](kasane/changes/add-maui-core/tasks.md:28)、[iOS Bridge:201](ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:201)、[Android Bridge:240](android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsSettingsBridge.kt:240)
   - 問題点: spec/design は全 `CellBase.PropertyChanged` を dirty set に積み、複数 Cell なら `replaceCells` に送ります。一方、両 Bridge は「可視性を変える更新は `replaceCells` ではなく `replaceCell`」と明記しています。複数 Cell の `IsVisible` を同一サイクルで変更すると、visible projection が正しく再構築されない可能性があり、`IsVisible` Requirement とバッチ Requirement を同時に満たせません。
   - 推奨修正: バッチ対象を可視性以外の内容変更に限定してください。可視性変更を含む Cell は単発 `replaceCell`、残りだけ `replaceCells` とする規則を design/spec/tasks に明記し、「複数 Cell の可視性を同一サイクルで変更」する Scenario を追加してください。

2. **Major — `SettingsView.Root` の公開形が上位決定から spec に落ちていない**

   - 該当: [proposal.md:10](kasane/changes/add-maui-core/proposal.md:10)、[tasks.md:21](kasane/changes/add-maui-core/tasks.md:21)、[spec.md:33](kasane/changes/add-maui-core/specs/maui-core/spec.md:33)、[agenda.md:18](kasane/roadmaps/maui-support/phases/phase-2-maui-core/agenda.md:18)、[agenda.md:28](kasane/roadmaps/maui-support/phases/phase-2-maui-core/agenda.md:28)、[ADR-0008:21](kasane/decisions/maui/0008-aiforms-compatible-api-surface-policy.md:21)
   - 問題点: 上位決定は AiForms 同形の `SettingsView.Root` と XAML での Section 直置きを要求しています。しかしデルタスペックは未定義の `Sections` を直接操作し、`SettingsRoot` 型、`Root` の型・既定値・差し替え、`ContentProperty`、`Section.Cells` の公開形を規定していません。tasks の「Root = Sections コンテナ」も、独立した `SettingsRoot` 型なのか単なる `IList<Section>` なのか判別不能です。
   - 推奨修正: 公開 API Requirement を追加し、少なくとも `SettingsRoot`、`SettingsView.Root`、`Section.Cells`、各コレクションの型・既定実体・nullability・差し替え時の再購読、XAML ContentProperty を完全なシグネチャで固定してください。Scenario に実際の XAML 構造を含めてください。

3. **Major — 切断中の購読維持と facade 回収保証が両立する条件がない**

   - 該当: [design.md:54](kasane/changes/add-maui-core/design.md:54)、[agenda.md:22](kasane/roadmaps/maui-support/phases/phase-2-maui-core/agenda.md:22)、[spec.md:155](kasane/changes/add-maui-core/specs/maui-core/spec.md:155)
   - 問題点: design は Handler 切断後も facade のコレクション購読を維持します。一方、agenda は切断時の「delegate/購読解除」を要求し、spec は facade 参照を捨てれば gateway ごと回収可能とします。外部 ViewModel が `ObservableCollection` や Cell を保持したままなら、通常の強い `CollectionChanged` / `PropertyChanged` 購読は SettingsView を逆に保持するため回収できません。現在の GC Scenario はイベント元も一緒に解放すれば通ってしまい、このリークを検出しません。
   - 推奨修正: Handler/attach に属する購読と facade/model に属する購読を区別してください。後者は弱参照購読にするなど寿命方式を design で確定し、外部コレクションと Cell を強く保持したまま SettingsView だけを解放するリーク Scenario を追加してください。

4. **Major — iOS Handler の ViewController containment が設計されていない**

   - 該当: [design.md:54](kasane/changes/add-maui-core/design.md:54)、[tasks.md:38](kasane/changes/add-maui-core/tasks.md:38)、[native-bridge.md:39](kasane/concepts/maui/api/native-bridge.md:39)、[ApiDefinition.cs:343](maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:343)
   - 問題点: iOS Bridge が返すのは `UIView` ではなく `UIViewController` で、既存契約は子 ViewController として embed することを要求しています。`CreatePlatformView → makeHost*` だけでは、親 VC の取得、`AddChild` / `DidMove`、切断時の `WillMove(nil)` / `RemoveFromParent`、controller と view の参照解放順が決まりません。単に `controller.View` を返す実装では lifecycle と回収保証が壊れます。
   - 推奨修正: iOS 固有の containment 手順と接続・切断の順序を Decision 4 に追加し、親子関係成立と再接続時の旧 controller 解放を検証する Scenario/task を追加してください。

5. **Major — ItemsSource 機能の状態遷移が未定義**

   - 該当: [design.md:42](kasane/changes/add-maui-core/design.md:42)、[spec.md:123](kasane/changes/add-maui-core/specs/maui-core/spec.md:123)、[tasks.md:31](kasane/changes/add-maui-core/tasks.md:31)
   - 問題点: 次の公開挙動が決まっていません。

     - `ItemsSource` を先に設定し、後から `ItemTemplate` を設定した場合
     - `ItemTemplate` または `TemplateStartIndex` を表示中に変更した場合
     - `ItemsSource = null`、Template が nullや不正な型を生成した場合
     - `NotifyCollectionChangedAction.Move` の扱い
     - 複数項目の Remove / Replace
     - 生成区間へ手動要素を挿入した後の Reset

     特に「開始 index + 生成数」だけで管理すると、生成区間内へ手動追加された要素を Reset が誤って削除し、「手動追加分を温存する」という SHALL を破ります。
   - 推奨修正: ItemsSource/ItemTemplate/TemplateStartIndex の状態遷移表を design に追加してください。生成物の provenance を追跡するか、手動変更可能な範囲を制限して例外挙動を定義し、上記ケースの Scenario を追加してください。

6. **Major — 双方向対応表は同一オブジェクトの重複配置を表現できない**

   - 該当: [design.md:42](kasane/changes/add-maui-core/design.md:42)、[spec.md:43](kasane/changes/add-maui-core/specs/maui-core/spec.md:43)、[native-bridge.md:42](kasane/concepts/maui/api/native-bridge.md:42)
   - 問題点: `CellBase → cellId` と `Section → sectionId` が単数である一方、公開 `IList<T>` は同じ Cell/Section インスタンスを複数箇所へ追加できます。この場合、一方の ID が上書きされ、プロパティ更新・削除・再構築の対象が不定になります。
   - 推奨修正: 同一インスタンスの複数配置を禁止し、どの時点でどの例外を出すかを Requirement/Scenario に明記するか、位置を含む一対多 identity モデルへ変更してください。ItemsSource の template が同一インスタンスを返す場合も対象にしてください。

7. **Major — Bridge の UI スレッド契約が facade の公開契約へ伝播していない**

   - 該当: [design.md:29](kasane/changes/add-maui-core/design.md:29)、[native-bridge.md:57](kasane/concepts/maui/api/native-bridge.md:57)、[spec.md:43](kasane/changes/add-maui-core/specs/maui-core/spec.md:43)
   - 問題点: 構造イベントは同期で gateway に流す設計ですが、`INotifyCollectionChanged` はバックグラウンドスレッドからも発火できます。Bridge は全 API を UI スレッドから呼ぶ契約で、自身では marshal しません。違反時に dispatch、例外、未保証のどれになるか決まっていません。
   - 推奨修正: 「Sections/Cells/BindableProperty の変更は UI スレッドのみ」という呼び出し側契約を明記するか、facade が dispatcher へ marshal する規則を定義してください。違反時の Scenario も追加してください。

8. **Major — native Host 回収の受け入れ基準がテスト計画に存在しない**

   - 該当: [proposal.md:16](kasane/changes/add-maui-core/proposal.md:16)、[design.md:66](kasane/changes/add-maui-core/design.md:66)、[spec.md:155](kasane/changes/add-maui-core/specs/maui-core/spec.md:155)、[tasks.md:48](kasane/changes/add-maui-core/tasks.md:48)
   - 問題点: Requirement は Handler / platform view / native Host 実体の回収を保証しますが、Scenario の `WeakReference` 対象は Handler と platform view だけです。design は native 実体を検証ホストで確認するとしていますが、task 7.6 は表示・更新・再訪問のみでリーク確認を含みません。
   - 推奨修正: native Host 回収を保証に残すなら、iOS controller と Android Host view を対象とする両 OS の検証方法・外部参照解放手順・成功条件を task に追加してください。安定して判定できないなら、保証を「Bridge が旧 Host への強参照を保持しない」など観測可能な契約へ狭めてください。

9. **Major — 削除済み Section からの通知を止める保証がない**

   - 該当: [spec.md:75](kasane/changes/add-maui-core/specs/maui-core/spec.md:75)、[native-bridge.md:47](kasane/concepts/maui/api/native-bridge.md:47)、[phase-4 agenda.md:17](kasane/roadmaps/maui-support/phases/phase-4-basic-input-cells/agenda.md:17)
   - 問題点: 削除済み Cell の遅延更新は規定されていますが、削除済み Section の `HeaderText` / `FooterText` 通知は未規定です。`updateAccessory` は未知 section ID の安全な no-op 契約対象外で、既存資料では iOS assertion / Android strict 経路の危険性が明記されています。購読解除漏れが単なる無駄な呼び出しではなく Host 停止につながります。
   - 推奨修正: Section の Remove/Replace/Reset 時に accessory/property 購読を同期解除することを design に明記し、「削除済み Section の HeaderText を変更しても gateway 呼び出し・例外が発生しない」Scenario を追加してください。

10. **Minor — 「同一 UI サイクル」の判定境界が曖昧**

    - 該当: [spec.md:65](kasane/changes/add-maui-core/specs/maui-core/spec.md:65)、[design.md:31](kasane/changes/add-maui-core/design.md:31)
    - 問題点: 「同一イベントハンドラ」と「同一 UI サイクル」が同値か、dispatcher callback 実行前までかが定義されていません。dispatcher 実装差でテスト結果が変わります。
    - 推奨修正: 「最初の変更で1回だけ flush callback を予約し、その callback が実行されるまでの変更を1バッチとする」のように観測可能な境界へ書き換えてください。

11. **Minor — `RootHeaderView` / `RootFooterView` の“名前予約”が公開契約として曖昧**

    - 該当: [proposal.md:11](kasane/changes/add-maui-core/proposal.md:11)、[proposal.md:25](kasane/changes/add-maui-core/proposal.md:25)、[tasks.md:21](kasane/changes/add-maui-core/tasks.md:21)
    - 問題点: 名前予約が「今フェーズで no-op な公開 BindableProperty を追加する」のか、「将来名だけ設計上確保し、APIはまだ追加しない」のか不明です。前者なら設定しても何も起きない公開 API、後者なら task 3.4 の実装対象ではありません。
    - 推奨修正: どちらかを明記してください。後者なら tasks から除外し、phase-6 の予約名として design にだけ残すのが検証しやすい形です。

12. **Minor — ADR-0008 の A 分類である `Section.IsVisible` の担当フェーズがない**

    - 該当: [ADR-0008:18](kasane/decisions/maui/0008-aiforms-compatible-api-surface-policy.md:18)、[公開面棚卸し:29](kasane/roadmaps/maui-support/phases/phase-2-maui-core/artifacts/2026-08-06-aiforms-surface-inventory.md:29)、[design.md:9](kasane/changes/add-maui-core/design.md:9)
    - 問題点: design は Bridge が運べない公開面を後続へ送っていますが、`Section.IsVisible` は現行コアに対応する A 分類でありながら、本変更の Non-Goalsにも後続 agenda にも担当が見当たりません。このままではロードマップ完了後も ADR-0008 の公開面方針を満たしたか判定できません。
    - 推奨修正: 本変更へ含める必要はありませんが、Bridge 拡張を含む具体的な担当フェーズを roadmap/agenda に割り当て、proposal の Non-Goals に明記してください。

上位決定との整合では、内部所有 Store、`releaseHost()`、attach 後の root accessory 再適用、単一 Handler 方針は概ね正しく反映されています。実装前ゲートとしては、少なくとも Critical と Major の解消が必要です。

## 突き合わせ結果

ホスト側自己レビュー (2周、整合性3点修正) と突き合わせ。ホスト側と重複する指摘はなし — 以下すべて相方のみの指摘。

| # | 指摘 | 採否 | 判定根拠 |
|---|---|---|---|
| 1 | IsVisible 更新が replaceCells 禁止契約へ流れる (Critical) | **採用** | 両 OS Bridge の doc コメントで裏取り済み (iOS KsSettingsBridge.swift / Android KsSettingsBridge.kt)。flush 時に可視性変更 Cell を単発 replaceCell に分離する規則へ修正 |
| 2 | SettingsView.Root の公開形が spec に落ちていない (Major) | **採用** | agenda 決定 (AiForms 同形の Root) と spec (Sections 直接操作) の不整合は事実。公開 API 形状の Requirement を追加 |
| 3 | 切断中の購読維持と facade 回収の両立条件がない (Major) | **採用** | 外部保持コレクションによる逆保持は実害シナリオ明確。model 帰属購読を weak proxy 化し、リーク Scenario を追加。agenda「購読の解除」は Handler 帰属分と解釈することを design に明記 (ADR-0007 の Store 復元機構との両立のため) |
| 4 | iOS の ViewController containment 未設計 (Major) | **採用** | makeHostViewController が返すのは UIViewController で embed 契約あり (concept 明記)。Decision 4 に containment 手順を追加 |
| 5 | ItemsSource の状態遷移が未定義 (Major) | **採用** | 列挙されたケースはいずれも公開挙動の穴。特に「開始 index+生成数」管理での Reset 誤削除は SHALL 違反の実害あり。provenance 追跡へ変更し状態遷移を定義 |
| 6 | 対応表が同一インスタンスの重複配置を表現できない (Major) | **採用** | IList 公開ゆえに起こり得る。重複配置は追加時点で検出し例外とする Requirement を追加 |
| 7 | UI スレッド契約が facade 公開契約へ伝播していない (Major) | **採用** | Bridge は marshal しない契約 (concept 明記)。呼び出し側契約として facade の公開契約に明記 |
| 8 | native Host 回収の受け入れ基準がない (Major) | **採用 (保証を縮小)** | 保証を「facade / Handler が解放済み Host への強参照を保持しない」へ狭め、native 実体の回収は Bridge 既存契約 (releaseHost) に依拠する形へ。検証ホストでの確認は best effort として task に残す |
| 9 | 削除済み Section の accessory 通知を止める保証がない (Major) | **採用** | updateAccessory は未知 ID no-op 契約の対象外 (concept 明記) で実害あり。同期購読解除 + 対応表ガードを design/spec に追加 |
| 10 | 「同一 UI サイクル」の境界が曖昧 (Minor) | **採用** | 観測可能な境界 (flush callback 予約〜実行) へ書き換え |
| 11 | RootHeaderView/RootFooterView の名前予約が曖昧 (Minor) | **採用** | 「公開 API は追加しない (phase-6 の予約名として design に記録)」に確定 |
| 12 | Section.IsVisible の担当フェーズ不在 (Minor) | **採用** | Non-Goals に明記し、phase-4 agenda へ論点として引き継ぎ (Bridge 輸送拡張と併せて扱う) |

- 確定 (双方一致): 0件 / 採用 (相方のみ・根拠強): 12件 / 降格: 0件 / 未解決: 0件

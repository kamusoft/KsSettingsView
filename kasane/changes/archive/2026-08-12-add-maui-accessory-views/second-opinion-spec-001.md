# セカンドオピニオン: add-maui-accessory-views (spec-001)
**相方**: codex / **日付**: 2026-08-11 / **対象**: 提案一式 (proposal.md / design.md / specs 3 capability / tasks.md)
---
# レビュー結果: add-maui-accessory-views

**判定**: **NEEDS_DISCUSSION**

**指摘件数**: Critical 0 / Major 5 / Minor 3 / Suggestion 0

先行 change は完了済みとして扱いました。提案の方向性は理解できますが、実装中にスコープ判断が必要になる未確定事項と、既存契約との衝突が残っています。仕様凍結前に設計判断を閉じる必要があります。

## 指摘事項

### [🟠 Major] iOS の高さ追従が未確定のまま、native 変更を Non-Goal にしている

**該当箇所**: [proposal.md:17](kasane/changes/add-maui-accessory-views/proposal.md:17)、[proposal.md:24](kasane/changes/add-maui-accessory-views/proposal.md:24)、[design.md:81](kasane/changes/add-maui-accessory-views/design.md:81)、[design.md:89](kasane/changes/add-maui-accessory-views/design.md:89)、[tasks.md:5](kasane/changes/add-maui-accessory-views/tasks.md:5)

**問題点**: 自動高さへの追従は SHALL 要件ですが、wrapper の invalidation だけで iOS の UICollectionView 再計測まで届くか未確定です。一方、native Core/UI は Non-Goal とされ、Open Questions は「なし」です。実装時の検証結果によっては Non-Goal を破る必要があり、凍結後の spec では実装者が正しいスコープを判断できません。

**推奨修正**: 実装前に次のどちらかへ確定してください。

- wrapper のみで成立することを先行検証し、native 無変更を確定する。
- native UI の再計測口を本 change のスコープ、デルタスペック、tasks に含め、Non-Goal を修正する。

別 change に分けるなら、そちらを明示的な前提 change にしてください。

### [🟠 Major] Section accessory の BindingContext 契約が既存の facade 意味論と衝突する

**該当箇所**: [maui-core/spec.md:49](kasane/changes/add-maui-accessory-views/specs/maui-core/spec.md:49)、[design.md:20](kasane/changes/add-maui-accessory-views/design.md:20)

**問題点**: spec はすべての accessory View が `SettingsView.BindingContext` を継承するとしています。しかし現行 facade は `SettingsView → Section → Cell` の順でコンテキストを配り、ItemsSource 生成 Section は item を自身の BindingContext として保持します（[KsBindingContextBinder.cs:12](maui/KsSettingsView.Maui/Internals/KsBindingContextBinder.cs:12)、[Section.cs:183](maui/KsSettingsView.Maui/Section.cs:183)）。移植元も Section Header/Footer View には Section の BindingContext を設定しています（[Section.cs:32](../AiForms.Maui.SettingsView/SettingsView/Section.cs:32)）。

現在の記述のままでは、ItemsSource 生成 Section の HeaderView が item ではなく画面全体の ViewModel を参照し、既存の階層意味論から外れます。また、View 自身に明示 BindingContext がある場合の優先順位も未定義です。

**推奨修正**: 少なくとも次を明文化してください。

- RootHeaderView / RootFooterView の継承元
- Section.HeaderView / FooterView の継承元
- View に明示 BindingContext がある場合の優先順位
- Section の BindingContext 変更時の追従

現行 facade と揃えるなら、Root は SettingsView、Section accessory は所有 Section の BindingContext を継承し、View の明示値は上書きしない契約が自然です。ItemsSource 生成 Section の Scenario も追加してください。

### [🟠 Major] wrapper の所有権と破棄状態機械が Host 切断以外で未定義

**該当箇所**: [design.md:20](kasane/changes/add-maui-accessory-views/design.md:20)、[design.md:59](kasane/changes/add-maui-accessory-views/design.md:59)、[tasks.md:21](kasane/changes/add-maui-accessory-views/tasks.md:21)、[tasks.md:27](kasane/changes/add-maui-accessory-views/tasks.md:27)、[tasks.md:39](kasane/changes/add-maui-accessory-views/tasks.md:39)

**問題点**: Host 切断時の全破棄は定義されていますが、次の通常操作で旧 wrapper をいつ・どの順番で破棄するかが決まっていません。

- View を別インスタンスへ差し替える
- View を null にする
- accessory を持つ Section を削除・置換する
- Root 全再構築で Section が外れる
- accessory View を別の配置先へ再利用する

Store の定数 closure は wrapper を強参照します。また、native が旧 wrapper をまだ子 View として保持している間に `DisconnectHandlers` すると、表示中 View を破棄する窓が生じます。Decision 2 の detach は「新しく返す View」を親から外す処理で、旧 wrapper の安全な退役順序を定めるものではありません。現行 icon 実装には退役リースを gateway 配信後まで遅延させる仕組みがありますが、View には同等の規定がありません。

**推奨修正**: accessory slot ごとの所有状態機械を設計に追加してください。生成、公開、置換、クリア、Section 登録解除、Host 解放の各遷移について、Store 更新・platform 親からの detach・logical child 解除・`DisconnectHandlers` の順序を定めます。リークテストも Host 切断だけでなく、差し替え、null、Section 削除、Root 再構築を含めてください。

### [🟠 Major] 「MAUI 固有」扱いは sample-parity の例外条件を満たしていない

**該当箇所**: [proposal.md:11](kasane/changes/add-maui-accessory-views/proposal.md:11)、[design.md:69](kasane/changes/add-maui-accessory-views/design.md:69)、[samples-maui/spec.md:7](kasane/changes/add-maui-accessory-views/specs/samples-maui/spec.md:7)

**問題点**: sample-parity の例外は「デモ対象の公開 API が対象 platform に存在しない場合」に限られます（[sample-parity.md:35](kasane/concepts/cross/conventions/sample-parity.md:35)）。しかし proposal 自身が、native には `KsAnyView` による Root / Section accessory が既に存在すると説明しています。7項目のうち、任意 View の表示、サイズ追従、HeaderHeight の clip は native 公開 API にも対応概念があります。

ページ全体を MAUI 専用例外にすると、accepted な cross/ADR-0016 の適用範囲を過度に狭めます。

**推奨修正**: 次のいずれかにしてください。

- 共通 accessory 表示・高さ挙動は3 platform 共通デモとして追加する。
- ページを分割し、BindingContext、text/view 優先、Handler 再接続など本当に MAUI facade 固有の挙動だけを「MAUI 固有」に置く。
- ページ全体を例外にするなら、native に対応 API があるにもかかわらず例外とする新しい設計判断を ADR として明示する。

### [🟠 Major] サンプルの「再訪問」は同一インスタンスの Handler 再接続を検証できない

**該当箇所**: [samples-maui/spec.md:17](kasane/changes/add-maui-accessory-views/specs/samples-maui/spec.md:17)、[samples-maui/spec.md:22](kasane/changes/add-maui-accessory-views/specs/samples-maui/spec.md:22)、[tasks.md:34](kasane/changes/add-maui-accessory-views/tasks.md:34)

**問題点**: 現行メニューは選択のたびに `CreatePage()` で新しい Page を生成します（[SampleScreen.cs:77](samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs:77)、[MenuPage.cs:72](samples/maui/KsSettingsView.Sample.Maui/MenuPage.cs:72)）。デモを pop して再度選択すると、新しい SettingsView が初期状態から表示されます。そのため lifecycle 復元が壊れていても「再表示された」ように見え、Scenario が偽陽性になります。

**推奨修正**: 同じ Page / SettingsView インスタンスを維持したまま Handler の切断・再接続が起きる操作手順を定めてください。例えばデモから子 Page を push して pop する、または同一インスタンス識別値と切断中変更を表示して復元を確認します。E2E にも「同一 facade インスタンス」を前提として明記してください。

### [🟡 Minor] ADR-0018 の Android DiffUtil に関する現状認識が不正確

**該当箇所**: [ADR-0018:10](kasane/decisions/maui/0018-accessory-view-update-semantics.md:10)

**問題点**: ADR は Android DiffUtil が View の差し替えを検出できないとしていますが、現行 adapter は `KsAnyView` の参照同一性を明示比較し、別参照を内容変更として検出します（[KsSettingsListAdapter.kt:330](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:330)、[ListAdapterDiffTest.kt:181](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ListAdapterDiffTest.kt:181)）。Core の値等価だけでは検出できない、という説明とは区別が必要です。

**推奨修正**: 「Core の値等価には参加しないが、Android UI は参照比較を持つ」と現状を訂正してください。そのうえで、OS 共通の確実な経路として明示 `updateAccessoryView` を採る、と理由付けすれば Decision 自体は維持できます。

### [🟡 Minor] 4対象の公開契約に対し Scenario とテストタスクが Header 側へ偏っている

**該当箇所**: [maui-core/spec.md:5](kasane/changes/add-maui-accessory-views/specs/maui-core/spec.md:5)、[maui-bridge/spec.md:5](kasane/changes/add-maui-accessory-views/specs/maui-bridge/spec.md:5)、[tasks.md:38](kasane/changes/add-maui-accessory-views/tasks.md:38)

**問題点**: API は Root/Section × Header/Footer の4対象ですが、表示・クリア・差し替え Scenario は主に Section Header と Root Header だけです。Root は Host 所有、Section は Store 所有で経路が異なり、Footer も別 renderer なので単純な代表テストでは不足します。また、BindingContext 変更後の再解決 Scenarioが tasks 6.1/6.3 の明示対象にありません。

**推奨修正**: 少なくとも4対象の初期表示、クリア、差し替え、text フォールバックをマトリクス化してください。BindingContext の初期伝播と変更追従もテストタスクへ明記してください。

### [🟡 Minor] 多重配置の検出範囲・タイミング・失敗後状態が未定義

**該当箇所**: [maui-core/spec.md:103](kasane/changes/add-maui-accessory-views/specs/maui-core/spec.md:103)、[tasks.md:28](kasane/changes/add-maui-accessory-views/tasks.md:28)

**問題点**: 「accessory と他の配置先」まで対象としていますが、既存の重複検出は同一 controller 内の Section / Cell 登録表に限定されています（[KsSettingsController.cs:1283](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1283)）。別 SettingsView、通常 Layout の子、Handler 接続前のXAML初期化をどう検出するかは新しい設計が必要です。また、BindableProperty の変更 callback で例外を出す場合、失敗したプロパティ値を元へ戻すのかも決まっていません。

**推奨修正**: 検出対象と例外タイミングを明文化し、接続前重複、別 SettingsView、通常 Layout 配下、null解除後の再利用、失敗後のプロパティ状態を Scenario に追加してください。

## アクションプラン

1. iOS 高さ再計測の実装分岐を確定し、native 変更のスコープ矛盾を解消する。
2. Root/Section 別の BindingContext 契約と wrapper 所有状態機械を design/spec に追加する。
3. sample-parity の扱いを再判断し、共通デモと MAUI 固有デモの境界を整理する。
4. 同一 Page インスタンスによる lifecycle 検証手順を定義する。
5. 4 accessory 対象、BindingContext、破棄、多重配置の Scenario／テスト対応を補完する。

依頼どおり、ファイル書き込みおよびビルド・テスト実行は行っていません。

## 突き合わせ結果 (2026-08-11)

ホスト側自己レビュー (2周) と突き合わせ。8件中、全件になんらかの対応を実施:

- **Major-1 (iOS 高さ追従 vs Non-Goal 矛盾)**: **採用** — proposal Non-Goals に条件付き例外 (tasks 1.1 の検証で必要確定時は native 再計算口をスコープに含める) を明文化、design Open Questions に登録 (deviation.md 記録も指示)
- **Major-2 (BindingContext が facade 意味論と衝突)**: **採用** — 根拠強 (KsBindingContextBinder / ItemsSource Section / 原典)。継承元を「Root → SettingsView / Section accessory → 所有 Section」へ修正、明示値の非上書き・変更追従・ItemsSource Scenario を spec に追加、design Decision 1 に反映
- **Major-3 (wrapper 退役の状態機械未定義)**: **採用** — design Decision 5 に accessory slot 状態機械 (差し替え / null / Section 削除 / Root 再構築の「Store 更新 → 配信 → 破棄」順序、icon retired lease パターン踏襲) を追加。tasks 4.6 とリークテスト対象拡大 (6.2)
- **Major-4 (sample-parity 例外の文言非該当)**: **部分採用** — 配置自体は規約を踏まえたオーナー裁定済み (phase-6 決定⑥) のため覆さない。ただし例外の適用拡張である事実は正しく、design Decision 6 に注記 + 蒸留時の sample-parity.md 明文化を申し送り
- **Major-5 (再訪問デモの偽陽性)**: **採用** — 根拠強 (SampleScreen.cs:77 の毎回 CreatePage を確認)。samples spec と tasks 5.2 を「同一 facade インスタンスでの切断・再接続手順」へ修正、E2E (6.3) にも明記
- **Minor-1 (ADR-0018 の Android DiffUtil 記述不正確)**: **採用** — ADR-0018 Context を「Core の値等価では検出不能 (Android UI の参照比較はローカル実装で OS 共通保証ではない)」へ訂正。Decision は不変
- **Minor-2 (4対象マトリクスの偏り)**: **採用** — maui-core spec に4対象適用を明記、tasks 6.1 / 6.3 をマトリクス化、BindingContext テストを明示
- **Minor-3 (多重配置の範囲・例外後状態未定義)**: **採用** — 検出範囲を既存の Section / CellBase 多重配置検出と同一契約に限定 (別 SettingsView・Layout 配下は platform 層に委ねる)、null 解除後再利用の Scenario を追加

採用 7 / 部分採用 1 / 降格 0 / 未解決 0。判定 NEEDS_DISCUSSION の主論点 (iOS 高さ・sample-parity) は条件付きスコープの明文化とオーナー裁定済みの記録でそれぞれ解消。

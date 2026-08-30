# Design: add-maui-core

## Context

phase-1 で Bridge (Store 操作 1:1 の 12 メソッド + `updateAccessory` + `setTheme` + lifecycle) と Binding csproj が完成し、C# → Native の経路は検証ホストで疎通済み。本変更は MAUI facade 層 (`KsSettingsView.Maui`) を新設する。

上位入力は phase-2 agenda の決定事項10件 (maui/ADR-0007 / ADR-0008 / core/ADR-0019 を含む)。それらはここで再決定しない — 本書は agenda 決定を実装可能な形に詳細化する MAUI 層内部の設計判断を扱う。

制約 (Bridge の既存契約より):

- interop が現時点で運べるのは LabelCell (title / descriptionText / valueText / hintText / isEnabled / isVisible) と Section (headerText / footerText / cells) のみ。facade の公開プロパティはこの範囲に限定し、後続フェーズで additive に拡張する
- **可視性 (`isVisible`) を変える Cell 更新は `replaceCells` (バッチ) ではなく `replaceCell` (単発) で行う** (両 OS Bridge の契約)
- `updateAccessory` は「未知 ID no-op」契約の**対象外** — 削除済み Section の ID で呼ぶと危険 (素通しされる)
- Bridge の全 API は UI スレッドから呼ぶ (呼び出し側契約。Bridge は marshal しない)
- iOS の Host は `UIViewController` であり、子 ViewController として embed する契約

## Goals / Non-Goals

proposal.md のとおり。本書は実装方式の確定のみを扱う。

## Decisions

### Decision 1: TargetFrameworks に素の net10.0 を含め、Bridge 呼び出しを internal gateway 抽象で隔離する

**採用案:** `KsSettingsView.Maui` の TargetFrameworks は `net10.0;net10.0-ios;net10.0-android`。変換経路・対応表・dirty set・ItemsSource 器などの純ロジックは platform 非依存コードに置き、Bridge 呼び出しは internal な gateway インターフェース (Store 操作 1:1 + `updateAccessory` + lifecycle) 越しに行う。platform TFM のみが Binding 参照・gateway 実装・Handler の platform 部分を持つ。ユニットテストは素の net10.0 で fake gateway を注入して検証する。

**理由:** Binding assembly は platform TFM でしか参照できず、本丸である変換経路を `dotnet test` で高速に網羅するには platform 非依存の seam が必要。後続 Cell フェーズは gateway に DTO 変換メソッドを足すだけで同じテスト戦略に乗れる。

**代替案:**
- **A: platform TFM のみでデバイス/シミュレータテストに寄せる** — 変換経路の網羅検証が遅く、回帰のたびに実機系ビルドが要る。却下
- **B: 純ロジックを別アセンブリに分離** — 利用者から見えるアセンブリが2つに割れ公開面の管理が複雑化する。internal seam で同じ効果が得られる。却下

### Decision 2: 変換経路の実装形 (購読の一元所有と flush 規律)

**採用案:** SettingsView (facade) が全購読を一元所有する。

- **スレッド契約:** facade の操作 (コレクション操作・BindableProperty 変更) は UI スレッドで行う呼び出し側契約とする。facade は marshal せず、Bridge の UI スレッド契約へそのまま乗せる (MAUI の UI オブジェクトの一般慣例と同じ)
- 構造イベント (`CollectionChanged` の Add/Remove/Move/Replace) は UI スレッド上で同期に gateway の構造操作へ 1:1 変換する。`Reset` のみ `setRoot` 再構築 + 対応表の全再構築
- **構造イベントでの購読整理:** Section / Cell がコレクションから除去・置換されるとき (Reset 含む)、その要素への `PropertyChanged` / accessory 購読を**同期的に解除**し、対応表から除去する。特に Section の `HeaderText` / `FooterText` は `updateAccessory` が「未知 ID no-op」契約の対象外のため、**発行前に対応表に ID が生きていることを必ずガードする** — 削除済み Section からの遅延通知が Bridge へ届く経路を残さない
- 内容更新は `CellBase.PropertyChanged` → dirty set へ追加 → 未予約なら dispatcher で flush callback を1回予約する。**バッチの境界は「最初の変更で予約された flush callback が実行されるまで」** — この間の変更が1バッチになる (dispatcher 実装差に依存しない観測可能な定義)
- flush は dirty set を snapshot し、**可視性 (`IsVisible`) が変化した Cell は各1件の `replaceCell`**、残りの内容変更は 1件なら `replaceCell` / 複数なら `replaceCells` で発行する (Bridge の可視性契約)。可視性の単発発行 → 内容バッチの順に適用する
- flush 時点で対応表に存在しない Cell (保留中に構造操作で除去) は skip する — Bridge 側の「未知 ID no-op」契約と併せて二重に安全
- エコー抑止フックの口: flush の発行直前に internal な抑止判定 (`ShouldPublish` 相当) を通す。phase-2 では常に true を返す実装のみを置き、同値チェックの実装は phase-4 が差す (agenda 決定「エコー抑止の移管」)

**理由:** agenda 決定 (二層方式) の実装形。可視性の分離は Bridge 契約 (visible projection の再構築が replaceCells に乗らない) からの強制。抑止フックを flush 境界に置くことで、phase-4 は判定関数を差し替えるだけで済み経路の再設計が不要。

**代替案:**
- **A: 内容更新も即時単発 replace** — Android の連続 submitList による notifyItemChanged 破棄を踏む。agenda で却下済み
- **B: 構造イベントも遅延バッチ化** — Store の構造操作は元々1件ずつの契約で、遅延させると対応表と実コレクションの不整合窓が生まれる。却下
- **C: 可視性変更もバッチに含める** — 両 OS Bridge の契約違反 (可視性はバッチ不可)。却下

### Decision 3: 公開コンテナ形状・対応表・ItemsSource 器の形

**採用案 (コンテナ形状):** AiForms 同形の XAML 直置きを次のシグネチャで固定する:

- `SettingsRoot` : `ObservableCollection<Section>` 派生の薄い型 (AiForms 命名踏襲)
- `SettingsView.Root` : `IList<Section>` の BindableProperty。既定値は `SettingsRoot` の新規インスタンス。`[ContentProperty(nameof(Root))]` で XAML では Section を直接並べる。差し替え時は旧コレクションの購読解除 → 新コレクションの購読 (実体が `INotifyCollectionChanged` の場合) → `setRoot` 再構築
- `Section` : `[ContentProperty(nameof(Cells))]`。`Cells` : `IList<CellBase>`、既定値は `ObservableCollection<CellBase>` の新規インスタンス。差し替え規則は Root と同じ
- 静的コレクション (`INotifyCollectionChanged` 非実装) が渡された場合は初回 `setRoot` のみの静的描画 (agenda 決定)

**採用案 (対応表):** SettingsView (facade) が「sectionId ↔ Section」「cellId ↔ CellBase」の双方向対応表を一元所有する。登録するのは gateway (Bridge) が返した ID のみ (DTO 自身の ID は使わない — interop 契約)。`setRoot` / `Reset` 時は全再構築、insert / remove / replace は差分更新する。**同一の Section / CellBase インスタンスを複数箇所へ配置することは禁止し、対応表への登録時 (= 構造操作への変換時) に検出して `InvalidOperationException` を投げる** — 単数 identity の上書きで更新対象が不定になるのを防ぐ。ItemsSource のテンプレートが同一インスタンスを返す場合も同じ検出に乗る。

**採用案 (ItemsSource 器):** `ItemsSource` / `ItemTemplate` / `TemplateStartIndex` を AiForms 同形の公開挙動で踏襲する。ただし内部管理は AiForms の「開始 index + 生成数」ではなく **provenance 追跡 (テンプレ生成した要素インスタンスの集合を保持)** とする — 生成区間へ手動要素が挿入されても Reset がテンプレ生成分だけを正確に除去できる。状態遷移:

| 状態 / 操作 | 挙動 |
|---|---|
| `ItemsSource` 設定、`ItemTemplate` 未設定 | 生成しない (Template が後から設定された時点で生成) |
| `ItemTemplate` / `TemplateStartIndex` を表示中に変更 | 既存のテンプレ生成分を除去して再生成 |
| `ItemsSource = null` | テンプレ生成分のみ除去 (手動追加分は温存) |
| items の Add / Remove / Replace / Move | 生成先コレクションの対応する操作へミラー (複数項目のイベントも項目ごとに適用) |
| items の Reset | テンプレ生成分のみ除去して再生成 (手動追加分は温存) |
| Template が CellBase (Section) 以外を生成 | `InvalidOperationException` |

生成された要素の `BindingContext` は対応する item。生成・除去は**通常の構造操作として Decision 2 の変換経路に流す** — テンプレ専用の Bridge 経路は作らない。

**理由:** コンテナ形状は agenda 決定 (AiForms 同形の `SettingsView.Root` + XAML 直置き) と二層方式決定 (`IList<T>` 公開・静的許容) の両方を満たす最小形。対応表の facade 所有は agenda 決定③の帰結 (Handler は切断で消えるが facade は VirtualView と同寿命)。provenance 追跡は「手動追加分の温存」SHALL を生成区間への手動挿入があっても守るため。

**代替案:**
- **A: Handler 側で対応表を管理 (旧 openspec 原案)** — Handler は切断で破棄され対応表が失われる。再接続復元 (ADR-0007) と寿命が合わない。却下
- **B: テンプレ生成物を専用経路で setRoot 再構築** — 部分更新の利点を捨て、生成のたびに全再描画になる。却下
- **C: 「開始 index + 生成数」で生成区間を管理 (AiForms 実装同形)** — 生成区間内へ手動挿入されると Reset が手動分を誤削除し温存 SHALL を破る。却下
- **D: 同一インスタンスの重複配置を許容 (位置込みの一対多 identity)** — 対応表・dirty set・購読の全てが複雑化する割に、重複配置の実用ケースがない。却下

### Decision 4: Handler lifecycle・購読の帰属・iOS containment

**採用案 (購読の帰属):** 購読を2種に区別する。

- **Handler 帰属** (platform view の attach 監視・native 側 delegate 等): `DisconnectHandler` で解除する — agenda 決定①の「delegate / 購読の解除」はこちらを指すと整理する
- **facade / model 帰属** (Root / Cells の `CollectionChanged`、CellBase / Section の `PropertyChanged`): facade と同寿命で維持し、切断中の変更も Store へ流し続ける — 再接続時は ADR-0007 の復元機構 (Store 現在状態からの表示復元) に乗る。この購読は **MAUI 本体の ItemsSource 購読と同じ weak proxy パターン** (`WeakNotifyCollectionChangedProxy` 相当) で張る — 外部 (ViewModel 等) がコレクションや Cell を保持し続けても、イベント購読が SettingsView を逆に保持してリークさせない

**採用案 (lifecycle):** `SettingsViewHandler` は `CreatePlatformView` で `makeHost*` (Android は新 Context)、`DisconnectHandler` で `releaseHost()` を呼ぶ 1:1 対応とする。

- **iOS containment:** Bridge が返すのは `UIViewController`。接続時は handler の `MauiContext` から親 ViewController を解決し、`AddChildViewController` → platform view として `controller.View` を返す → view 階層への追加後に `DidMoveToParentViewController` の順で embed する。切断時は `WillMoveToParentViewController(null)` → view の取り外し → `RemoveFromParentViewController` → `releaseHost()` → controller 参照破棄の順。再接続は新しい controller で同じ手順を繰り返す (旧 controller への参照は残さない)
- **Android:** `makeHostView(context)` の View をそのまま platform view として返す。切断時は `releaseHost()` → view 参照破棄

`RootHeaderText` / `RootFooterText` は facade が値を所有し、**platform view の view load / attach 後**に `updateAccessory` (root) で適用する。両 OS とも「attach 検知 → 適用」の同一経路に統一する (Android は attach 前だと root 対象 Diff が黙って失われるため必須、iOS は順序非依存だが経路を分けない。core/ADR-0019 / deviation 申し送り)。

**理由:** 切断中も Bridge へ流し続けることで、再接続復元を Store の既存保証だけで成立させる (facade 側に「切断中の変更の記録・再生」機構を持たない)。weak proxy は「facade 解放で Bridge / Store も回収可能」の保証を外部保持があっても成立させるための標準手段。iOS containment は既存契約 (子 VC embed) の履行。

**代替案:**
- **A: 切断時に model 帰属の購読も解除し、再接続時に setRoot で全再構築** — ADR-0007 が用意した Store 復元機構を捨てて同じことを facade で二重実装することになる。却下
- **B: root accessory を property mapper で素直に適用** — mapper は `CreatePlatformView` 直後・view tree 追加前に走り得るため Android で適用が失われる。却下 (deviation 申し送りで確認済み)
- **C: model 帰属の購読を強参照で張る** — 外部がコレクション / Cell を保持し続けると SettingsView が回収されず、「facade 解放で gateway も回収可能」の保証を破る。却下
- **D: iOS で `controller.View` だけ取り出して containment を省く** — 子 VC embed 契約に違反し、appearance callbacks と lifecycle が壊れる。却下

### Decision 5: リークテスト基盤の形と回収保証の範囲

**採用案:** `WeakReference` + `GC.Collect` ループの検証ヘルパ (「参照が回収されるまで待つ / されなければ fail」) をテスト共通基盤として置く。net10.0 ユニットテストで検証するのは:

- 切断後に Handler・(fake) platform view への強参照が facade 側に残らないこと
- **外部がコレクションと Cell を強く保持したまま** SettingsView への参照を捨てた場合に、SettingsView と gateway が回収可能なこと (weak proxy の検証)
- 切断後の facade / Handler が解放済み Host への強参照を保持しないこと

native Host 実体 (iOS controller / Android view) 自体の回収は Bridge の `releaseHost()` 既存契約 (解放・購読解除) に依拠し、本変更の保証には含めない。検証ホスト (maui/tests) での目視確認は best effort として行う。後続 Cell フェーズは同じヘルパを再利用する。

**理由:** agenda 決定 (DisconnectHandler とライフサイクル③)。リークの主戦場は facade の購読解除漏れと weak proxy の張り忘れであり、これは net10.0 で高速に検証できる。native 実体の GC 判定はデバイス上でしか安定せず、受け入れ基準にすると判定不能な SHALL になる — Bridge 側契約への依拠が観測可能な境界。

**代替案:**
- **A: デバイステストのみで検証** — 回帰検出が遅く、リーク原因の切り分け (facade か binding か) もできない。却下
- **B: native Host 実体の回収まで本変更の SHALL に含める** — デバイス GC の挙動に依存し安定判定できない。Bridge 契約への依拠で足りる。却下

## Risks / Trade-offs

- flush が dispatcher に依存するため、ユニットテストでは dispatcher の差し替え口が要る (gateway と同様に seam を設ける)
- net10.0 TFM はアプリ実行では使われない「テスト用の顔」になる。NuGet パッケージングは非ゴールのため現時点で害はないが、パッケージング時に TFM 構成を再検討する
- provenance 追跡は AiForms の index 管理より状態を持つ — 状態遷移表をそのままテストケース化して固定する
- weak proxy は購読の生存期間がテストで見えにくい — リークテスト基盤の外部保持ケースで固定する

## Migration Plan

純粋な追加。`maui/KsSettingsView.slnx` へ 2 プロジェクト (本体 + テスト) を追加する。既存資産 (Binding / 検証ホスト) の変更はない。

## Open Questions

- Theme 系 BindableProperty と `setTheme` 経路の置き場 (phase-4 着手時に確定 — proposal の Non-Goals)
- `Section.IsVisible` (ADR-0008 の A 分類) の担当フェーズ — Bridge 輸送拡張と併せて phase-4 が候補 (phase-4 agenda へ論点として引き継ぎ)

## ADR 候補

- **Decision 1** (net10.0 TFM + gateway seam): 後続全 Cell フェーズのテスト戦略とプロジェクト構成を制約するため候補
- Decision 2〜5: agenda 決定の詳細化ないし MAUI 層内部の可逆な実装判断のため昇格しない (agenda 側で「ADR は起こさない」と整理済み)

## 補足: 予約名

`RootHeaderView` / `RootFooterView` は phase-6 のための**予約名であり、本変更では公開 API を追加しない** (設定しても何も起きない no-op プロパティを置くこともしない)。輸送・実体化・`RootHeaderText` との優先順位は phase-6 の責務。

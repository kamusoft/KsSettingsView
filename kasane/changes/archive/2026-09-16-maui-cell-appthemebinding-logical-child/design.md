# Design: maui-cell-appthemebinding-logical-child

## Context

facade の論理所有は 2 系統ある。accessory View / `CustomCell.Content` は `KsAccessoryViewOwnership` が `AddLogicalChild` / `RemoveLogicalChild` を対にして論理子にし (maui/ADR-0016: 所有は View プロパティの寿命、Host 非依存)、入口は「検査を通った変換経路から呼ぶ `Reassign`」と「検査相手のいない経路から呼ぶ `ReassignIfFree` (他所に所有済みなら引き取らない)」の 2 つ。一方 Section / Cell は `KsBindingContextBinder` が継承 BindingContext を配るだけで `Parent` は null。

Section / Cell が外れる 4 経路 (Root からの除去・Section からの除去・ItemsSource 再生成・Root 差し替え) の対応表からの除去は全部 `KsSettingsController` の中にあり、Handler 未接続なら購読すら張られない。付ける側 (binder) は Host 非依存なので、外す側を controller に置くと非対称になる。

決定の経緯は exploration.md (論点 1〜4) と cross/ADR-0032 (proposed)。

## Goals / Non-Goals

Goals:
- Section / Cell を SettingsView の論理子にし、`AppThemeBinding` / `DynamicResource` の再評価を全 BindableProperty で成立させる
- 論理所有の型を accessory View と揃える (所属の寿命・Host 非依存・付け外しが対)
- 多重配置の契約 (例外の型・タイミング・部分更新を残さない) を変えない
- 利用者向け手段を `AppThemeBinding` / `DynamicResource` に一本化し、concepts / Sample / MauiHost を追随させる

Non-Goals: proposal.md のとおり (Skill・`VisualElement` 化・マルチウィンドウ・CSS・親 change の iOS 観測)

## Decisions

### Decision 1: 付け外しは所属の増減を見る所有の器が Host 非依存に行う

**採用案:** `KsBindingContextBinder<T>` を所有の器 (`KsLogicalChildOwnership<T>` 相当。名前は実装が決める) に育てる。器は「持ち主 + 配布先コレクション + 今この持ち主に付けている子の集合」を持ち、次の契機で付け外しする:

| 契機 | 動き |
|---|---|
| コレクションへの追加 (`Add` / `Insert` / `Replace` の新側) | 継承 BindingContext を配り、論理子として引き取る (Decision 2 の規則) |
| コレクションからの除去 (`Remove` / `Replace` の旧側) | 子の `Parent` がこの持ち主なら `RemoveLogicalChild`。集合から外す |
| `Reset` | 集合にあってコレクションに無い子を外し、コレクションにあって集合に無い子を引き取る |
| 配布先コレクションの差し替え (`Root` / `Cells` の再代入、`OnTargetChanged`) | 旧コレクション由来の子を全部外してから新コレクションを購読・全件引き取る |
| 持ち主の BindingContext 変更 (`Apply`) | 継承 BindingContext を配り直す。付け外しはしない (冪等) |

ItemsSource 再生成 (`KsItemsSourceBinder.RemoveGenerated` / `MirrorRemove`) は生成先コレクション自体から要素を除くので、上の「除去」に合流する。専用の経路は作らない。

**理由:** Section / Cell にとっての「プロパティの寿命」は「コレクションへの所属」であり、その増減を Host 非依存に見ているのが今の binder。ここに付け外しを置くと accessory View と同じ規律 (XAML 構築時から効く・Host 解放中も維持) に揃い、継承 BindingContext の配布と論理子化を 1 手順に畳める。外す側 4 経路は「除去」「Reset」「差し替え」の 3 動きで全部覆える。

**代替案:**
- **A: controller の `RegisterSection` / `RegisterCell` / `UnregisterSection` / `UnregisterCell` に寄せ、Host 接続中だけ論理子にする** — 却下。論理所有が Host 世代の寿命になり accessory View と規律が割れる。表示前の `DynamicResource` 解決と BindingContext 継承が Host 接続まで遅れ、Host 接続 / 切断時の全付け外しも要る。binder の手配布も二重系のまま残る
- **B: spike の最小改変 (追加時に `AddLogicalChild` だけ、外さない)** — 却下。片道なので 4 経路で `Parent` が古い持ち主を指し続け、多重配置の誤判定や古い親の Resources 参照を生む

### Decision 2: 論理所有の変更口は所有の器 1 つで、他所に所有された要素の追加は所属の時点で例外にする

**採用案:** 論理子の付け外し (`AddLogicalChild` / `RemoveLogicalChild`) を行うのは Decision 1 の器だけにし、controller は `Parent` を触らない。器の規則:

- **所属 (コレクションへの追加)**: 要素の `Parent` が null か自分なら、継承 BindingContext を配ってから論理子にする。**他所 (期待する所有者以外の facade 所有者 — 別の Section / SettingsView / CustomCell。SettingsView から既に外れた Section が所有したままの要素を含む) に所有されている要素は、追加の時点で `InvalidOperationException` にする** (Host の有無に依らず)。正しく置かれている側には触れない
- **所属の解除 (除去 / Reset / 差し替え)**: 要素の `Parent` が自分で、かつ**配布先コレクションにもう含まれていない**ときだけ外す (同じ要素が 2 回追加され 1 件だけ除去された場合に、残る所属を解除しない)。Reset は「集合にあってコレクションに無い要素を外し、コレクションにあって `Parent` が null の要素を引き取る」
- **変換経路 (controller)**: 論理所有の確定は行わない (所属の時点で確定済み)。同じコレクション内の重複 (`Parent` が自分なので所属では弾けない) は現行の数えあげで変換時に例外にする

**理由:** 所属の時点で `Parent` を見れば、別の Section・別の SettingsView・外れた Section のいずれに所有されていても同じ 1 条件で即座に弾ける。「他所所有のまま追加され、後で旧所有が解除された要素が変換まで `Parent` null のまま」という空白 (相方レビュー 2 回目 Major 1) は、追加自体を例外にすれば生じない。「所属中は Host に依らず常に論理子」の契約とも矛盾しない。同じコレクションへの二重追加だけは `Parent` で見分けられないため数えあげが残る。

例外は追加の直後 (CollectionChanged の処理中) に送出され、公開コレクションはロールバックされない (現行の多重配置例外と同じ。回復は Root の全体再構築)。

**代替案:**
- **A: 所属の経路でも常に引き取る (奪う)** — 却下。正しく置かれている側の論理親と継承 BindingContext を黙って奪う
- **B: 所属では触れず、変換経路 (検査後) で `Parent` null の要素を引き取る (前回の案)** — 却下。旧所有が解除された後、変換まで `Parent` null の空白が残り、Host 非依存の論理所有契約と矛盾する
- **C: 他所所有中は pending として旧 `Parent` の解除を監視し、自動的に引き取る** — 却下。監視の仕組み (要素→複数所有者の弱い逆参照) が増え、誤用のために機構を足すことになる
- **D: pending 配置は変換まで保証外と明記し、利用者に Remove → Add を要求する** — 却下。契約に例外規定が増え、黙って効かない状態が残る
- **E: 変換経路で `Parent` が他所を指す要素を付け替える (最後に変換した側が勝つ)** — 却下。`Parent` は 1 つしか持てない

### Decision 3: 多重配置は「他所所有の要素の追加は所属時に例外、同一コレクション内の重複は変換時に例外」とし、accessory View / CustomCell.Content も SettingsView をまたぐ所有を検査する

**採用案:** Section / Cell については Decision 2 のとおり所属時の `Parent` 検査 + 変換時の数えあげ。accessory View / `CustomCell.Content` は現行の検査時点 (View 配置プロパティは値の確定前、未参加所有者は変換時) を維持しつつ、検査に 1 条件を足す: View の `Parent` が期待する所有者以外の facade 所有者 (別の SettingsView・その配下の Section / CustomCell・既に外れた Section / CustomCell) を指していれば多重配置として例外にする。例外の型と「native へ触れる前に全件検査し部分更新を残さない」契約は変えない。

**理由:** Section / Cell はただの C# オブジェクトで、同じインスタンスを 2 つの SettingsView のコレクションへ入れることを今は何も止めていない (ViewModel / static に持った Cell を 2 ページで並べる、テンプレートがキャッシュを返す、Pop 後も旧ページが Root を握っている等)。concepts の契約は「複数箇所へ置くことは禁止」と SettingsView を限定せずに書いており、検出できていないのは実装の穴。論理子化で `Parent` は 1 つしか持てない物理制約が加わるため、誤用として例外にして塞ぐ (オーナー裁定 2026-09-15)。accessory View / `CustomCell.Content` も同じ穴 (`_placedViews` が controller ごと) を持ち、同じ 1 条件で塞げるので同梱する。判定条件は「別 SettingsView の祖先があるか」ではなく「期待する所有者以外の facade 所有者を `Parent` が指しているか」で書く (外れた Section が所有したままの要素も含める)。

**代替案:**
- **A: SettingsView をまたぐ配置は検出せず「最後に変換した側が勝つ」と契約化する** — 却下。Decision 2 の代替案 E と同じ理由
- **B: `Parent` を判定の唯一の材料にする (数えあげを廃す)** — 却下。同じコレクションへの二重追加は `Parent` が自分なので見分けられない

### Decision 4: Section / CellBase は素の `Element` のまま

**採用案:** 基底クラスを変えない。論理子化で効くのは `Parent` チェーンに依存する機構 (継承 BindingContext・`DynamicResource`・`AppThemeBinding`・CSS StyleSheet) だけで、`Style` / 暗黙 Style (`NavigableElement` 以上) は当たらない。

**理由:** 暗黙 Style が当たる・Window ごとの外観解決に乗るのは別の設計判断で、本 change の目的 (binding の再評価) には不要。

**代替案:**
- **A: `NavigableElement` / `VisualElement` へ変える** — 却下。Style の適用範囲と Handler の期待が変わる大きな公開挙動の変更で、本 change の Non-Goal

### Decision 5: Sample は共有 Style と XAML の `AppThemeBinding` で外観に追随し、購読の補助クラスを持たない

**採用案:** SettingsView 用の共有 Style (Setter の値に `AppThemeBinding`。light / dark の色は `SampleTheme` の定数を `StaticResource` 化するか、Style 内で参照) を Sample のアプリ / 共有リソースに置き、7 ページの `SettingsView` が参照する。Section 装飾デモ (`ApplySectionDecorationDemo`) は下地色だけ外観追随するので、同じ形の別 Style にする。ButtonCell の `TitleColor` は XAML の `AppThemeBinding`。`SampleThemeFollower`・`SampleTheme.Apply`・`SampleTheme.MauiTitleText`・`SampleTheme.IsDark` を削除する。外観トグル (`SampleAppearanceStore` → `Application.UserAppTheme`) は現行のまま。

**理由:** Theme プロパティは ADR-0030 の時点で `AppThemeBinding` が効き、Cell も本 change で効くので、購読して入れ直す理由が無くなる。Sample がライブラリの推奨する書き方 (XAML だけで外観追随) の実例になる。色の実値は `SampleTheme` の定数のまま (cross/ADR-0016: 3 platform 同一 RGBA)。

**代替案:**
- **A: Cell 分だけ XAML へ移し、補助クラスは Theme 用に残す** — 却下 (オーナー裁定 2026-09-15)。購読が Sample に残り、「購読 + 再代入を記述から外す」決定と食い違う
- **B: 各ページの XAML に 15 本の `AppThemeBinding` を直書きする** — 却下。7 ページで同じ 15 本を繰り返し、`SampleTheme.Apply` が防いでいた色値の二重管理を XAML 側で復活させる

## Risks / Trade-offs

- **`Element.Parent` の参照の強さ**: 論理子化後も「外部が root / Cell を保持したまま SettingsView が回収される」(LeakTests 2 件) が成り立つことに依存する。spike で緑だったが機序は未確認。実装の最初の一手で確認し、テストに意図を明記する。落ちたら設計に戻る (Open Questions)
- **Handler 接続時の配信順序**: `Connect` は `SetTheme` / `SetStyle` を Root の全件検査より先に呼ぶ。変換時の多重配置例外 (同一コレクション内の重複・accessory View) で「native に何も配信されない」と保証するのは構造・snapshot に限り、Theme / Style の先行配信は許容する (spec の THEN もそう限定する)
- **BindingContext の二重配布**: 器の手配布と MAUI 本体の `Parent` 経由配布が同じ規則で 2 回走る。無害だが、変更通知が 2 回出る経路が無いか `BindingContextTests` で確認する
- **例外の時点が早まる (挙動変更)**: 別の Section / SettingsView に所有された Section / Cell の追加は、現行の「変換時」ではなく「追加時」に例外になる (Host 未接続でも)。「同じ Cell / Section / View を 2 つの SettingsView で共有」は今まで黙って動いていたが例外になる。誤用であり、beta 中は破壊的変更を受容する (core/ADR-0031 Decision 3)。リリースノートに記載する
- **`AppThemeBinding` の観測**: 単体テストは `Application.UserAppTheme` の切替で再評価を観測する。端末外観の変更は同じ `RequestedThemeChanged` を経るため単体では区別せず、MauiHost の両 OS 確認で押さえる
- **CSS StyleSheet**: `Parent` 設定で `ApplyStyleSheets()` が走る。Non-Goal (契約化しない)
- **Sample の Style 化**: `SettingsView` は `VisualElement` なので Style は当たる (`ImplicitStyleTests` が基底コンストラクタ内の Setter 適用順序でも壊れないことを固定済み)。明示 Style (`x:Key`) を使い、暗黙 Style で MauiHost 等に波及させない

## Migration Plan

利用者の移行は不要 (購読 + 再代入は動作し続ける)。concepts の記述順序: settingsview-migration-defects の `maui-facade.md` 訂正が先に merge される前提で、本 change は同じ段落を「論理子なので解決・追随する」へ書き換える。Skill は蒸留後に docs-refresh。

## Open Questions

- `Element.Parent` が弱参照でない場合 (tasks 0.1 の LeakTests が落ちた場合) の設計: 器の側で持ち主への参照を弱くしても MAUI 本体の `Parent` は変えられないため、Decision 1 の「Host 非依存」を諦めて Host 接続中だけ論理子にする (却下した代替案 A) へ戻す判断になる。実装の最初の一手で決着させ、落ちた場合は実装を止めてオーナーへ上げる (design は凍結対象なので実装中に書き換えない)

## ADR 候補

- Decision 1 / 2 / 3 / 4: cross/ADR-0032 (proposed) として起票済み。提案段階の決定 (変更口の一本化・変換経路の確定順序・SettingsView をまたぐ所有の例外化) は ADR-0032 Decision 2〜3 に反映済み
- Decision 5: ADR 候補にしない (Sample の書き方であり、cross/ADR-0016 (Sample 一致) と ADR-0032 から導ける)

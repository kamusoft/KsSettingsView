# Exploration: maui-cell-appthemebinding-logical-child

## 課題 / 動機

MAUI で `Section` / `Cell` の色プロパティに書いた `AppThemeBinding` は、現行契約では外観変更で再評価されない (core/ADR-0031 Decision 2 の MAUI 節: `Section` / `Cell` は `SettingsView` の element ツリーに繋がない。利用者の手段は外観変更の購読 + 再代入)。maui-appearance-change-tracking の spike ②-b (tasks 5.1、2026-09-15) で、`Section` / `Cell` を論理子に繋ぐと `AppThemeBinding` が再評価されて native の行の描画まで届くことが iOS / Android の両方で**成立**した。採用するなら core/ADR-0031 Decision 2 の MAUI 節と Revisit When の改訂を伴う設計変更になるため、本 change として起票する (オーナー裁定 2026-09-15、maui-appearance-change-tracking の proposal / tasks 5.1 の取り決めどおり同 change には同梱しない)。

### spike の調査結果 (同じ調査を繰り返さないための記録。全文は `../maui-appearance-change-tracking/exploration.md` の「spike ②-b の結果 (2026-09-15)」)

- **原因の同定**: ツリー外の `Element` では `AppThemeBinding` が外観変更を受け取らない、という見立ては実測で裏付けられ、`Parent` を入れるだけで解消した
- **試した最小改変 (一時、戻し済み)**: facade の BindingContext 配布処理 (`maui/KsSettingsView.Maui/Internals/KsBindingContextBinder.cs` の `Distribute`) で `SetInheritedBindingContext` に続けて `owner.AddLogicalChild(child)` を 1 行 (`child.Parent` が null のときだけ)。この処理は SettingsView → Section と Section → Cell の両方で使われるため 1 箇所で両段が論理子になる。`Section` / `Cell` / `SettingsView` 自体は未改変
- **観察 (iOS Simulator iOS 26.0 / Android Emulator API 35)**: MAUI Sample「基本 Cell 7 種デモ」の ButtonCell `TitleColor` に `{AppThemeBinding Light=#FF008000, Dark=#FFFF00FF}` を付け (code-behind の再代入を外す)、表示したまま外観を切り替えると同じ画面・同じ行のまま緑 → マゼンタへ描き直された。Android は `MainActivity` の `Recreate()` を外し同一 Activity のまま確認。証跡は `../maui-appearance-change-tracking/evidence/maui-spike-logical-child-{ios,android}-{light,dark}.png`
- **既存テストへの影響**: 改変込みで MAUI facade テスト 528 件 / 失敗 0 (改変なしと同じ)。`LeakTests` / `BindingContextTests` / 多重配置の検査は落ちなかったが、これは安全の根拠にならない (下記)
- **構造上残る問題 (本 change の設計論点)**: (1) 最小改変は `AddLogicalChild` が片道で、Section を Root から外す・Cell を Section から外す・ItemsSource の再生成・Root 差し替えの 4 経路で `RemoveLogicalChild` が対にならず `Parent` が古い所有者を指し続ける。実装では外す側 (`KsItemsSourceBinder` / controller の対応表と同じ 4 経路) を全部対にする必要がある。(2) `child.Parent is null` のときだけ繋いだため、既に `Parent` を持つ Cell は黙って繋がれない。多重配置の判定が controller の対応表と `Parent` の二系統になる論点は未解消。(3) BindingContext の二重伝播・継承プロパティの伝播変化は改変込みテストが緑だった範囲では観測されなかったが、積極的には検査していない。(4) `AppThemeBinding` の再評価は MAUI 本体 (`Application.RequestedTheme`) が担うため、ライブラリ側に新しい購読は不要に見える (未検証)
- **前史**: cell-style-dark-appearance-parity (archive 2026-09-06) の 0.1 で「Cell の `AppThemeBinding` は再評価されない」を実測し、手段を購読 + 再代入に改めた経緯。利用者向け Skill の `DynamicResource` 記述の訂正は settingsview-migration-defects が扱う

関連: core/ADR-0031 (Decision 2 の MAUI 節・Revisit When)、`kasane/concepts/maui/api/maui-styling.md`、`maui/api/maui-facade.md`。

## 検討した選択肢 (却下案と理由を含む)

探索 (2026-09-15、ksn-explore) で論点を 4 つに区分し、順に決めた。

### 論点 1: 所有モデル

現状は「accessory View / `CustomCell.Content` は `KsAccessoryViewOwnership` が `AddLogicalChild` / `RemoveLogicalChild` を対にして論理子にする (maui/ADR-0016)、Section / Cell は `KsBindingContextBinder` が継承 BindingContext を配るだけで `Parent` は null」という非対称。

| 案 | 評価 |
|---|---|
| **A. accessory View と同じ論理所有の型に完全に載せる** (採用) | 所有の正が一系統に揃い、外す側 4 経路は「所有解除」として既存の規律をなぞれる |
| B. `AppThemeBinding` を効かせる最小の `Parent` 付け (所有モデルには触れない) | 却下。所有の正が controller の対応表と `Parent` の二系統になり、ADR-0016 と食い違う型が facade 内に二つ残る |

### 論点 2: 付け外しの置き場 (外す側 4 経路の対称化)

scout の発見: 付ける側 (binder) は Host 非依存で Section 生成時から動くが、外す側 4 経路 (Root から Section を外す / Section から Cell を外す / ItemsSource 再生成 / Root 差し替え) は全部 controller の中にあり、Handler 未接続なら購読すら張られない。controller に `RemoveLogicalChild` を足すだけでは表示前に付け外しした要素の `Parent` が取り残される。

| 軸 | 案 A: controller に寄せる (Host 接続中だけ論理子) | **案 B: binder を「所有」に育てる (所属の増減で付け外し、採用)** |
|---|---|---|
| ADR-0016 の型との整合 | 外れる (論理所有が Host 世代の寿命に) | 揃う (所属の寿命 = View プロパティの寿命に相当) |
| 表示前の挙動 | `DynamicResource` 解決・BindingContext 継承が Host 接続まで遅れる | XAML 構築時から効く |
| 改変箇所 | controller 4 箇所 + Host 接続 / 切断時の全付け外し | binder 1 箇所 (追加分・削除分 (`OldItems`)・Reset・対象差し替え時の旧コレクション掃除) |
| BindingContext 配布 | 二重系が残る | `Parent` 経由の MAUI 本体の配布と同じ規則なので 1 手順に畳める |
| 多重配置 | controller の数えあげのまま | 例外の契約は controller の検査のまま。`Parent` は「他所に所有済みなら引き取らない」信号 (accessory の `ReassignIfFree` と同じ思想) |

### 論点 3: 副作用 (go / no-go)

scout による MAUI 本体 (10.0.x) のソース読みと facade の grep (根拠は ADR cross/0032 の Context):

- 継承 BindingContext: `Element.SetParent` が配る。明示設定済みは上書きしない規則が現行 binder と同じ → 二重系になるだけで新規の副作用なし
- 暗黙 Style / `Style`: `NavigableElement` 以上にしかなく、素の `Element` の Section / CellBase には**当たらない**。`ImplicitStyleTests` は `SettingsView` 対象のみで影響なし
- CSS StyleSheet: `Parent` 設定で `ApplyStyleSheets()` が走る。`.css` 利用アプリでセレクタに掛かり得る (実害は薄い見立て、未実測)
- `DynamicResource` / `AppThemeBinding`: 親の Resources 変更通知だけが再評価契機。spike の実測と機序が一致 (本命)
- `AppThemeBinding.GetValue` は `VisualElement` でないと `Application.Current` の外観に落ちる → マルチウィンドウで窓ごとに外観が違う構成だけ誤り得る (未実測)
- leak: scout は「`Parent` が強参照なら外部が root を握ったまま SettingsView が回収される LeakTests 2 件が落ちるはず」と指摘したが、spike は 528 件緑 (LeakTests 含む)。MAUI 本体が `Element.Parent` を弱参照にしている (実測に整合) という理解で進める。この 2 件を「論理子化しても回収される」回帰資産として意図を明記する

結論: **go**。採用を止める副作用は無い。

### 論点 4: 利用者向け契約と 3 面追随

契約はプロパティを限定せず「Section / Cell は SettingsView の論理子で、ページ配下の Element と同じ binding の解決を受ける」。購読 + 再代入は動作し続けるが記述からは外す (併記案は却下: 手段が割れる非対称 (ADR-0031 の負の帰結) を解消するのが本 change の価値)。

| 面 | 現行 | 本 change での扱い |
|---|---|---|
| concepts `maui/api/maui-styling.md` 手段表 | Cell 行は購読 + 再代入、下に「再評価されない」説明とコード例 | Cell 行を `AppThemeBinding` へ、説明とコード例を外す |
| concepts `maui/api/maui-facade.md` (`DynamicResource` は届かない) | settingsview-migration-defects が「初期解決は届くが追随しない」へ訂正予定 (先に入る) | その上に乗って「論理子なので解決・追随する」へ |
| concepts `maui/api/maui-rendering-lifecycle.md` / `architecture/view-materialization.md` | 論理所有は accessory View / cell content の話として記述 | Section / Cell も同じ型に載ることを追記 |
| Sample `samples/maui/KsSettingsView.Sample.Maui/SampleThemeFollower.cs` | 基本 Cell 7 種デモで購読 + 再適用 | 削除し XAML の `AppThemeBinding` へ (spike と同じ形) |
| 検証ホスト MauiHost 既定シナリオ | 購読 + 再代入が回帰資産 | `AppThemeBinding` 再評価シナリオへ置換 |
| Skill (`skills/{en,ja}/kssettingsview-maui/references/styling.md`、`kssettingsview-aiforms-migration/references/api-mapping.md`) | 購読 + 再代入 | 本 change では触らず、蒸留後に docs-refresh を依頼 |

## 決定事項

1. Section / Cell を accessory View と同じ論理所有の型に完全に載せる (論点 1、オーナー 2026-09-15)
2. 付け外しは `KsBindingContextBinder` を Host 非依存の「所有」の器に育てて所属の増減で対にする。controller には寄せない。継承 BindingContext の配布と同じ器の 1 手順にする (論点 2、同日)
3. 多重配置の例外の正は controller の検査のまま。`Parent` は「引き取らない」信号として使う (論点 2)
4. 利用者向け手段は `AppThemeBinding` / `DynamicResource` に一本化し、購読 + 再代入を記述から外す全面置換 (Sample の補助クラス削除・MauiHost 既定シナリオ置換を含む)。Skill は docs-refresh 経由 (論点 4、同日)
5. `Section` / `CellBase` は素の `Element` のまま (VisualElement にしない)
6. (提案段階 2026-09-15、相方 spec レビューを受けた裁定) 別の SettingsView をまたぐ同一 Section / Cell / View の配置は誤用として例外にする (オーナー)。他所に所有された Section / Cell の追加は追加時に例外、同じコレクション内の二重追加は変換時に例外、論理所有の変更口は器 1 つで controller は `Parent` を触らない (design.md Decision 2 / 3)
7. (提案段階) Sample は SettingsView 用の共有 Style と XAML の `AppThemeBinding` で外観追随し、`SampleThemeFollower` / `SampleTheme.Apply` を削除する (design.md Decision 5)

## ADR 候補 (作成済み: cross/ADR-0032 (proposed、core/ADR-0031 の amends。ドメイン横断のため cross に配置) / 未起票: なし)

- 昇格 (蒸留) 時に core/ADR-0031 の frontmatter に `amended-by: cross/0032`、core と maui の index に注記行を足す

## 未決の論点

- LeakTests 2 件 (外部が root / Cell を保持したまま SettingsView が回収される) が論理子化後も緑であることの機序確認 (MAUI 本体 `Element.Parent` が弱参照か)。実装の最初の一手として最小改変込みで単独実行し、意図をテストに明記する (tasks 0.1)
- `x:Reference` は namescope 経由で論理子とは別機構 → 本 change の対象外 (proposal Non-Goals)
- CSS StyleSheet 利用時に Section / Cell がセレクタに掛かることの実害有無 (未実測。契約に書くか Consequences に留めるか)
- 拘束の順序: settingsview-migration-defects (maui-facade.md の `DynamicResource` 訂正) が先に merge される前提。本 change はその上で同じ行を書き換える

## UI 素材 (ui/references/ の一覧と注釈)

なし (見た目の変更はない。spike の証跡は `../maui-appearance-change-tracking/evidence/`)

## 変更級の推奨: L (確定、オーナー 2026-09-15)

- 触る能力: MAUI facade 1 能力 (公開 API の追加・変更なし。挙動の拡張のみ)
- 覆すコスト: 高い。所有モデル (論理所有の型) を Section / Cell へ広げる設計変更で、accepted の core/ADR-0031 を amends する
- 影響面: facade 内部 (binder → 所有の器)・concepts 4 文書・Sample・検証ホスト・(蒸留後に) Skill 4 ファイル
- 可逆性: 論理子化を戻すと利用者の XAML (`AppThemeBinding`) が黙って効かなくなるため、契約として一度出すと戻しにくい
- 単一能力・公開 API 不変なら M だが、「覆すコストが高い」に該当し、外す側 4 経路の設計 (design.md の Decision 節) を書き切る価値があるため、迷ったら 1 段上で L を推奨

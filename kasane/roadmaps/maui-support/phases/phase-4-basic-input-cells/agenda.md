# phase-4-basic-input-cells

残り11種 (基本6: Command / Button / Switch / Checkbox / Radio / SimpleCheck、入力5: Entry / Picker / NumberPicker / TimePicker / DatePicker) の MAUI Handler・Bridge API・サンプルページを実装する。

原案: `openspec/changes/add-maui-cells` の一部 (凍結・参照のみ。CustomCell は phase-5 へ分離)

## 論点

(なし — 全論点解消済み 2026-08-10)

## 決定事項

- (2026-08-10) **移行ガイドは要・ただし本フェーズ外 — NuGet パッケージング着手時に docs-refresh 経由で作成** — 配布が ProjectReference のみの現状では読者 (乗り換え組) に届かず、CustomCell (phase-5)・Header/Footer View (phase-6) が揃う前は対応表が中途半端になる。対応関係の一次情報 (maui/ADR-0008 の A/B 分類・ADR-0013 等) は ADR/concepts に蓄積済みで後から書ける。移行ガイドは利用者向け = `docs/` の領分のため作成時は docs-refresh スキル経由 (プロジェクト規約)

- (2026-08-10) **視覚スナップショットテストは導入しない** — facade は描画を持たない純変換層であり、視覚の正は native (native テスト資産 + sample-parity の目視検証装置) が担う。MAUI 層の検証は maui/ADR-0009 の fake gateway net10.0 テスト拡張で賄う: 11種の Conversion / CellShape、delegate 書き戻し + 入口同値チェック、Section.IsVisible 単発配信、DataTemplateSelector 解決。視覚品質はサンプル4ページの実機/シミュレータ目視 (iOS/Android サンプルとの見比べ) を実装フローの確認手順として置く
- (2026-08-10) **`Section.IsVisible` は Cell 可視性と対称の形で本フェーズ実装** — native core モデルは両OSとも `Section.isVisible` (既定 true) を実装済みで、未着手は Bridge 輸送と facade 公開のみと調査で確定。設計: (1) `KsBridgeSection` (両OS) に `isVisible` フィールドを追加し `makeSection()` へ渡す (2) facade `Section.IsVisible` (BindableProperty、既定 true) を公開 — ADR-0008 A 分類の踏襲命名 (3) 更新経路は Cell の可視性単発配信と対称に Section 版 visibility dirty-tracking を追加し `ReplaceSection` を単発で送る。専用 Bridge メソッド (`setSectionVisibility`) は Store 公開操作に存在せず maui/ADR-0002 (1:1 原則) に反するため却下。実装時の申し送り: `ReplaceSection` で cellId が再採番されないか (cellId Map との整合) を提案の設計で確認する。ADR は起こさない (既存 ADR からの対称導出のため)

- (2026-08-10) **Theme 系は phase-4 のスコープに含める** — SettingsView レベルの Theme 公開面 (BindableProperty) と Cell の CellStyle / accentColor 公開を本フェーズで実装する。決定理由: (1) native の BasicCellsDemo / InputCellsDemo は `.theme(SampleTheme.maui)`・CellStyle・accentColor を使っており、サンプル完全一致 (論点6決定) が Theme に依存する (2) Bridge の `setTheme` 輸送は maui/ADR-0002 の12メソッドとして phase-1 実装済み、型方針も maui/ADR-0004 で決定済みのため、残作業は facade 公開面と変換・テストのみで薄い (3) Cell 単位の style はどのみち per-type DTO の輸送対象であり、切り出せるのは SettingsView レベルの Theme だけで利得が小さい
- (2026-08-10) **サンプルスコープは BasicCells / InputCells / UnifyCellCommonFields / Visibility の4ページ追加** — native デモページと文言・構成・デモデータを一字一句一致で追加 (`SampleTheme` 相当の共通色定義も MAUI 側に対応)。対象4ページは CustomCell 不使用を確認済みで phase-4 の Cell だけで完結する。これにより MAUI サンプルの画面集合は規約上一致すべき集合に収束し、phase-3 の片側先行が解消 (残る未追随は CustomCellDemo のみ = phase-5 で解消)。StoreDemo / DSLDemo は対象外 (論点8決定)、LabelCell 検証ページは削除 (論点7決定)
- (2026-08-10) **Store/DSL デモは MAUI 対応せず、sample-parity 規約へ一般例外条項を追加する (案c)** — sample-parity 規約 (concepts/cross/conventions/sample-parity.md) に「デモ画面の一致対象は、デモ対象の公開 API がその platform に存在する場合に限る」条項を追加し、StoreDemo / DSLDemo は native 2 platform のみの画面として正当化する。MAUI での対応 (案a) は facade 契約の禁止事項 (Binding assembly 型の直接使用禁止・Store 非公開) のデモ化になり自己矛盾のため却下。個別 deviation (案b) は、逆方向の同型ケース (DataTemplateSelector / ItemsSource など MAUI にしか対応概念がないデモ) が控えており一般条項の方が管理コストが低いため不採用。cross/ADR-0016 は「規約本文は concepts に定める」構造のため ADR 本体の改訂は不要。concepts の改訂は本フェーズ change の蒸留時に追随として行う
- (2026-08-10) **LabelCell 検証ページは基本 Cell デモページ追加時に削除・置換する** — phase-3 申し送りどおり確定。暫定の「LabelCell 検証」ページは基本 Cell デモページ追加と同一 change 内で削除し (残すと sample-parity の「画面の集合は全 platform で同一」に対する余剰画面になる)、置換後のページ構成・文言は iOS/Android サンプルの対応ページと一致させる
- (2026-08-10) **選択面のプラットフォーム差の MAUI 露出 — DatePicker の uiStyle は統一 enum で意味マッピング** — MAUI 層に `DatePickerUIStyle { Calendar, Wheels }` を新設し、`Calendar` → iOS `.calendar` / Android `Material`、`Wheels` → iOS `.wheels` / Android `Spinner` へマッピングする (オーナー判断: MAUI 層では同一プロパティが望ましく、意味軸「カレンダー形式かホイール形式か」で対応可能)。プロパティは nullable で null = 各 native の既定。値名は `Wheels` — Android の "Spinner" は第一義がドロップダウン widget で多義であり、プロジェクト共通語彙「ホイール」とも一致するため。concepts の「case を同一と仮定してはならない」との関係は「器も名前も別物のまま、MAUI 層が意味軸での明示的対応付けを新設する」であり矛盾しない。Android で `Calendar` を選ぶと Material 固有挙動 (テキスト入力モード・`FragmentActivity` 要求) が付随することは facade 契約に明記する。`AndroidButtonColor` は iOS に対応概念がないため maui/ADR-0004 の接頭辞付き nullable を維持。選択面の挙動契約 (確定のみ反映・非確定 dismiss 破棄・スタイル継承等) は native 内で完結するため facade は透過
- (2026-08-10) **ItemsSource / ItemTemplate は MAUI 層のみを維持し、DataTemplateSelector は本フェーズで対応する** — Native に足さない方針 (roadmap 非ゴール / maui/ADR-0008) は維持。`DataTemplateSelector` は `DataTemplate` の派生型で ItemTemplate に代入可能 (API 形状変更なし) だが、selector への直接 `CreateContent()` は MAUI が例外を投げるため、テンプレート実体化直前に `SelectTemplate(item, container)` で実テンプレートへ解決する処理を facade に挿入する。phase-2 の「渡せない」制約 (facade 契約・deviation 記録) を解除。非対応のままだと selector 代入経路が型的に開いたまま実行時例外になる口が残るため、ガードを書くより解決を書く (オーナー指摘による方針転換)
- (2026-08-10) **エコー抑止は書き戻し入口の同値チェックで行い、`ShouldPublish()` フックは撤去する** — 同値チェックの比較対象は **CellBase の現値** (「通知時点で native Store が新値を持つか」は論点2の調査で「持たない」と確定したため、Store との比較は不要かつ不可能)。delegate 通知値が CellBase 現値と同値なら書き戻さず停止、異なれば SetValue → 既存 flush → 必須コミット。発行時点の抑止 (phase-2 で口だけ確保した `ShouldPublish()`) はコミット欠落を作り込む構造的に誤った置き場と判明したため撤去。native 側にも同値 setText ガードが両OSに実在 (iOS: IME マークドテキスト保護 / Android: カーソル位置維持) し、折り返しは二重に安全。phase-1 決定の「`updateCellValue` 直行パス・debounce は作らない」は踏襲
- (2026-08-10) **双方向バインド8プロパティの経路と型マップ** — 経路は「native UI 操作 → Cell コールバック → Bridge delegate/listener (cellId + 値) → C# `FindCell(cellId)` → CellBase へ SetValue → 既存 dirty-set/flush → `replaceCell(s)` で Store へコミット」の一本。調査により**両OSとも通知時点で native Store は旧値のまま** (Cell view は Store へ書き込まずコールバックを呼ぶだけ) と確定 — facade からの書き戻し送信は冗長なエコーではなく**必須のコミット**である。型マップ: Bool / Int / String は素通し。Picker は native の実体どおり **index (Int) を輸送**し、SelectedItem ⇔ index の解決は facade の ItemsSource で行う (MAUI 層で閉じる方針と整合)。Time / Date は壁時計値なので **ISO-8601 文字列輸送** ("HH:mm" / "yyyy-MM-dd") — epoch millis は TZ 変換事故の作り込みになるため不採用、分割 int 輸送は DTO フィールド増に対し利点が薄く不採用
- (2026-08-10) **Handler パターンの11種への適用は per-type 展開 (案A)** — facade 派生クラス (AiForms 互換命名、maui/ADR-0008 A 分類)・Snapshot・Bridge DTO を Cell 種ごとに 1:1 で追加し、`KsBridgeGateway.ToDto()` を型スイッチ化する。単一 wide DTO + cellType 判別 (案B) は、判別の二重化 (C#/native 双方でのスイッチ) と maui/ADR-0002 の union DTO 却下の経緯との不整合により不採用。根拠: Native Store の Cell は両OSとも11種が個別型で実装済みであり、per-type DTO が輸送先と 1:1 で対応する。interop DTO は非公開の輸送表現 (maui/ADR-0004) のため型数増加は利用者に露出しない
- (2026-08-09 実装完了反映) **`updateAccessory` の未知 sectionID 契約** — 案A (Store の no-op 契約を `updateAccessory` へ拡大) を採用し、独立 M 級 [harden-update-accessory-unknown-id](../../../../changes/archive/2026-08-09-harden-update-accessory-unknown-id/proposal.md) として本フェーズ着手前に実装・蒸留完了 (core/ADR-0020 accepted)。未知 sectionID の section 系 target は state 更新も Diff 発行もない no-op、Root 系 target は従来どおり無条件発行。Host 側の missing ID 検出 (iOS DEBUG assert / Android strictMode) は「Store が契約を守る限り到達しない内部整合性チェック」として温存 — 案B (Host 側の安全化) は却下。Android の storeCollectJob 沈黙経路への Host 側防御はスコープ外と確定 (将来必要になったら別途起票)。C# 到達面 (Bridge / facade) はこの契約が透過するため、本フェーズでの追加対処は不要

## TODO

- [x] 論点の解消 (2026-08-10 全12論点)
- [x] ksn-propose で変更提案を起こす (add-maui-basic-input-cells、L 級)
- [ ] (論点10決定 2026-08-10) NuGet パッケージング着手時に旧 AiForms からの移行ガイドを docs-refresh 経由で作成する (本フェーズでは作業なし・追跡のみ)
- [x] (論点8決定 2026-08-10) sample-parity 規約 (concepts/cross/conventions/sample-parity.md) へ「デモ対象の公開 API が存在する platform に限る」例外条項を追加する — 蒸留時に実施済み (2026-08-11)

- [x] (phase-2 実装結果より引き継ぎ 2026-08-08) add-maui-core の review Suggestion 未対応6件を実装時に評価して適用/却下を確定する — 実装フェーズで全件評価し**全却下で確定** (デルタスペックに対応 Requirement なし。review-001 が review-handoff #9 として妥当性を確認済み)
- [x] (phase-3 実装結果より 2026-08-09) add-maui-samples-foundation の review Suggestion 2件を評価 — `MenuPage.OnSelectionChanged` の選択解除は `await` 前へ**適用済み** (MenuPage.cs)。ReactiveProperty の破棄作法デモは **ReactiveProperty.Core 参照ごと削除されたため対象消滅**
- [x] (phase-3 実装結果より 2026-08-09) Sample csproj の `Microsoft.Maui.Controls.Compatibility` 参照の要否 — 本フェーズで**参照削除で確定** (未使用の参照を除去、コード側に使用なし)

## 実装結果 (2026-08-11 反映)

change: [changes/archive/2026-08-11-add-maui-basic-input-cells](../../../../changes/archive/2026-08-11-add-maui-basic-input-cells/proposal.md) (L 級)。付随して同フェーズ検出の不具合 2 件を独立 change で解消: [fix-maui-entrycell-focus-loss](../../../../changes/archive/2026-08-11-fix-maui-entrycell-focus-loss/exploration.md) (S 級、maui/ADR-0014)・[fix-entrycell-writeback-caret-race](../../../../changes/archive/2026-08-11-fix-entrycell-writeback-caret-race/proposal.md) (M 級、android/ADR-0014)。

- deviation 5件 (詳細は change の deviation.md): ニックネーム callback デモの対比不成立 / Android IME フォーカス喪失 (→ fix-maui-entrycell-focus-loss で解消) / Android パスワードマスクの native 修正 (Non-Goal を越えるスコープ追加、オーナー指示) / アイコンの色・形状差の許容 / Section.HeaderHeight のスコープ追加
- ADR: maui/ADR-0011〜0013 を accepted 昇格、maui/ADR-0015 (IconSource 実体化)・maui/ADR-0014 (measure 契約)・android/ADR-0014 (EntryCell SSoT) を新規 accepted

申し送りのルーティング (2026-08-11 オーナー確定):

- EntryCell の値変更通知 (onTextChanged 相当) の公開 → **見送り** (オーナー判断: AiForms 原典にも値変更 callback は無く、TwoWay バインドのみで問題ない。maui-facade.md に「公開しない」契約として明記済み)
- icon リース破棄時機 (review-002 Minor-10 + 保留b) → 簡易 change [fix-maui-icon-lease-disposal-ordering](../../../../changes/fix-maui-icon-lease-disposal-ordering/exploration.md) を起票
- ks-settingsview-compose の flaky テスト (review-001 Suggestion-9) → 簡易 change [fix-compose-dsl-double-update-flaky-test](../../../../changes/archive/2026-08-22-fix-compose-dsl-double-update-flaky-test/exploration.md) を起票
- iOS の EntryCell 書き戻しレース同型調査 (caret-race の Non-Goal) → 簡易 change [fix-ios-entrycell-writeback-race](../../../../changes/fix-ios-entrycell-writeback-race/exploration.md) を起票
- samples/maui README の未追随 (削除済みページ・外れた参照の記載) → docs-refresh の次回実施時に回収 (プロジェクト規約によりユーザー明示依頼制)
- CustomCellDemo の MAUI 追随 → phase-5 (custom-cell) の既存スコープで解消予定

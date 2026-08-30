# phase-4-basic-input-cells 議論履歴

## 2026-08-10: 移行ガイドの要否とスコープ (論点10)

- 選択肢: 本フェーズで作る / 要だが本フェーズ外 (パッケージング時) / 不要
- 採用: **要・本フェーズ外**。NuGet パッケージング着手時に docs-refresh 経由で作成 (TODO で追跡)
- 理由: (1) ProjectReference のみの現状では乗り換え読者に届かない (2) CustomCell / Header・Footer View が揃う前は対応表が中途半端 (3) 素材 (ADR-0008 A/B 分類等) は ADR/concepts に蓄積済みで後から書ける。docs/ の領分のため docs-refresh 経由が規約
- ADR 非該当

## 2026-08-10: Snapshot テストの方針 (論点9)

- 前提確認: 視覚スナップショットテストは両 native とも存在しない (iOS の "snapshot" ヒットは NSDiffableDataSourceSnapshot 関連)。MAUI テストは ADR-0009 の fake gateway 方式 (net10.0)
- 採用: **視覚スナップショットは導入しない**。facade は描画を持たない純変換層で、視覚 snapshot が検出できる内容は ConversionPathTests (決定的検証) と重複し、両OS のシミュレータランナーという重いインフラ新設に見合わない
- テスト方針: ADR-0009 路線の拡張 — 11種 Conversion / CellShape、delegate 書き戻し + 同値チェック、Section.IsVisible 単発配信、DataTemplateSelector 解決。視覚はサンプル4ページの目視 (sample-parity) で確認
- ADR 非該当 (ADR-0009 の既定路線の適用)

## 2026-08-10: Section.IsVisible の輸送と公開 (論点12)

- 前提調査 (ksn-scout): native core モデルは両OSとも `Section.isVisible` (既定 true) を実装済み (iOS `KsSettingsViewCore.Section`、Android `core.Section`)。可視性は visible projection で反映され、Section 操作は `replaceSection` 1経路で可視性・内容両方をカバー (Cell のような「バッチに isVisible を混ぜるな」制約は Section には構造上ない)。未着手は Bridge 輸送 (KsBridgeSection に isVisible なし) と facade 公開 (Section.cs に痕跡なし) のみ
- 採用: Cell 可視性と対称の設計 — Bridge フィールド追加 + facade `Section.IsVisible` 公開 + Section 版 visibility dirty-tracking から `ReplaceSection` 単発配信
- 却下: 専用 `setSectionVisibility` Bridge メソッド追加 (Store 公開操作に存在せず ADR-0002 の 1:1 原則に反する)
- 申し送り (ksn-propose の設計へ): `ReplaceSection` による cellId 再採番の有無と cellId Map との整合確認
- ADR 非該当 (既存 ADR-0002 / Cell 可視性単発配信契約からの対称導出)

## 2026-08-10: Theme 系 BindableProperty と setTheme 経路の置き場 (論点11)

- 前提調査: native の BasicCellsDemoView / InputCellsDemoView が `.theme(SampleTheme.maui)` を適用し、CellStyle (titleColor)・Cell の accentColor も使用 — 論点6で確定した「4ページ完全一致」が Theme / CellStyle 公開面に依存すると判明
- 選択肢: 案A = phase-4 に含める / 案B = 独立 change・新フェーズへ切り出し (サンプル2ページは色違いの部分先行 + deviation 追跡、roadmap フェーズ追加が必要)
- 採用: **案A (phase-4 に含める)**。Bridge の setTheme は phase-1 実装済み・型方針は ADR-0004 決定済みで残作業は facade 公開面のみと薄く、切り出しの利得が小さい
- ADR 非該当 (スコープ配置の決定。設計は既存 ADR-0002/0004 に規定済み)

## 2026-08-10: 各 Cell のサンプルページ追加 (論点6)

- 論点7・8の決定からスコープを導出: 追加は BasicCellsDemo / InputCellsDemo / UnifyCellCommonFieldsDemo / VisibilityDemo の4ページ (native と完全一致)。CustomCellDemo は phase-5、Store/DSL は対象外、LabelCell 検証は削除
- 対象4ページが CustomCell (SampleSliderCell) を使っていないことを grep で確認 — 使用箇所は CustomCellDemo とメニュー/テーマ共通部のみ
- 採用: 上記4ページ追加で確定 (ADR 非該当・スコープ決定のため history 記録のみ)

## 2026-08-10: Store/DSL 方式デモの MAUI 対応要否 (論点8)

- 対象確認: 両 native サンプルは7デモ画面 (+iOS のみの検証枠 MinimalDiffable) で完全対応。問題は StoreDemo / DSLDemo の2画面 — native API 方式のデモで MAUI に対応概念がない
- 選択肢: 案a = MAUI にも対応 / 案b = 個別例外化 (deviation 記録) / 案c = sample-parity 規約へ一般例外条項を追加
- 採用: **案c** — 規約に「デモ画面の一致対象は、デモ対象の公開 API がその platform に存在する場合に限る」を追加
- 理由: (1) 案a は facade 契約の禁止事項 (Store 非公開・Binding 型直接使用禁止) のデモ化で自己矛盾 (2) 逆方向の同型ケース (DataTemplateSelector 等 MAUI 固有デモ) が控えており一般条項が将来に効く (3) 既存の「技術検証画面」例外枠は生 UIKit 検証用でありライブラリデモの流用は規約の意味を歪める。cross/ADR-0016 は本文を concepts へ委譲する構造のため ADR 改訂不要、concepts 改訂は蒸留時に実施 (TODO 化)

## 2026-08-10: LabelCell 検証ページの削除と置換 (論点7)

- phase-3 申し送りに実行内容まで書かれており判断の余地は実質なし。申し送りどおり確定 (ADR 非該当・history 記録のみ)
- 内容: 基本 Cell デモページ追加と同一 change 内で「LabelCell 検証」ページを削除・置換、文言・構成は iOS/Android サンプルと一致 (sample-parity)

## 2026-08-10: 選択面のプラットフォーム差の MAUI API 露出 (論点5)

- 前提: 選択面の挙動契約 (確定のみ反映・非確定 dismiss 破棄・上限・スタイル継承) は native 提示 UI 内で完結するため facade は透過。API 判断が要るのはモデル上のプラットフォーム固有フィールド (`DatePickerCell.uiStyle` / `androidButtonColor`) のみ
- 当初推奨は ADR-0004 延長の「接頭辞付き別プロパティ (IOSUIStyle / AndroidUIStyle)」だったが、オーナーから「MAUI 層では同一プロパティが望ましい。意味的にはカレンダー形式かホイール形式かにマッピングできる」との指摘で統一 enum 方式へ転換
- 値名は `Calendar` は即決、非カレンダー側は `Wheel` / `Spinner` で迷いがあり議論。**`Wheels` を採用** — (1) Android の "Spinner" は第一義がドロップダウン widget で多義 (2) concepts の共通語彙「ホイール」「3連ホイール」「KsWheelView」と一致 (3) 意味軸の命名は OS API 史の固有名より見た目の記述が適切 (4) 複数形は年/月/日の複数ホイール構成と iOS `.wheels` との字面対応
- 採用: `DatePickerUIStyle { Calendar, Wheels }` (nullable、null = native 既定)。`AndroidButtonColor` は接頭辞付き維持。concepts の「case 同一視禁止」とは「MAUI 層の明示的な意味対応付けの新設」として整理し矛盾なし。Android `Calendar` に Material 固有挙動が付随する旨は facade 契約へ明記

## 2026-08-10: ItemsSource / ItemTemplate 方針と DataTemplateSelector 対応 (論点4)

- 方針確認: ItemsSource / ItemTemplate を MAUI 層のみに閉じ Native に足さない方針は維持 (roadmap 非ゴール + maui/ADR-0008 で既定、phase-2 実装も同形)
- DataTemplateSelector: 当初推奨は「本フェーズも非対応 (実需要が出たら別 change)」だったが、オーナーから「Selector は MAUI 組み込みで ItemTemplate (DataTemplate 型) にそのまま代入できるはず」との指摘。事実関係: 代入は型的に可能だが selector への直接 CreateContent() は例外になるため、実体化直前の SelectTemplate 解決 (数行) が必要 — 逆に非対応のままでも代入経路は開いており実行時例外の口が残る
- 採用: **SelectTemplate 解決を挿入して本フェーズで対応** (推奨を修正して確定)。ガードを書くくらいなら解決を書く規模感で、11種化により item ごとの Cell 種出し分けという selector 本来の用途も立つ
- ADR 非該当 (MAUI 層内で閉じる additive な決定のため history 記録のみ)

## 2026-08-10: Native 由来の値変更のエコー抑止 (論点3)

- 前提: 論点2の調査で「通知時点で native Store は旧値」= 書き戻しは必須コミットと確定。また両OSの EntryCell に同値 setText ガードが実在することをコードで確認 (iOS `EntryCellView.swift:132` = IME マークドテキスト破壊防止、Android `EntryCellViewHolder.kt:95` = カーソル位置維持)
- 選択肢: 推奨案 = 書き戻し入口の同値チェック (比較対象 = CellBase 現値) + `ShouldPublish()` 撤去 / 案B = 同値チェック + `ShouldPublish()` 温存 / 案C = 書き戻し中フラグで dirty 化自体を抑止
- 採用: **推奨案**。ループは「user 操作 → delegate → 入口同値チェック → SetValue → flush → replaceCell → Store diff → Host 再描画 → (listener 再発火) → delegate → 同値で停止」で必ず収束する
- 理由: 発行時点の抑止はコミット欠落バグの温床であり構造的に誤った置き場 (案C は書き戻しが Store に届かなくなるため明確に却下)。常に true のフック温存 (案B) は誤用リスクのみで利点なし。phase-1 決定 (`updateCellValue` 直行パスなし・debounce なし) は変更なし

## 2026-08-10: 双方向バインド8プロパティの cellId Map 経路と型マップ (論点2)

- 前提調査 (ksn-scout): 両OSとも Cell view はコールバック (`onValueChanged` 等) を呼ぶだけで Store へ書き込まない — **通知時点で native Store は旧値のまま**。よって facade からの書き戻しは「冗長なエコー」ではなく「必須のコミット」(論点3 の同値チェック設計の前提になる事実)。delegate/listener 本体は Bridge 未実装 (maui/ADR-0003 の決定のみ)。`FindCell` (cellId → CellBase 逆引き) は phase-2 で実装済み
- 値の型: Bool / Int / String は interop 素通し可。Picker の選択値は native 両OSとも Int (index)。Time / Date は iOS `Date` / Android `LocalTime`・`LocalDate` で、java.time 型は JNI 境界を直接越えられない
- 採用: 経路は「delegate 通知 → FindCell → SetValue → 既存 dirty-set/flush → replaceCell(s) コミット」。型マップは Picker = index 輸送 (SelectedItem 解決は facade の ItemsSource)、Time/Date = ISO-8601 文字列輸送 ("HH:mm" / "yyyy-MM-dd")
- 理由: Time/Date は壁時計値であり epoch millis だと TZ 変換事故を作り込む。ISO 文字列は1フィールドで両OS・C# に標準パーサがあり、生成側も自前でパース失敗の余地が実質ない。分割 int 案は堅牢だがフィールド増に見合う利点なし

## 2026-08-10: Handler パターンの11種への適用方針 (論点1)

- 前提調査 (ksn-scout): Native 側は iOS/Android とも 11 Cell 種 + Custom が Store/UI に実装済み。phase-2 の facade パターンは「CellBase 派生 + `CreateSnapshot()` / `AffectsSnapshot()` → KsSettingsController の dirty-set → `KsBridgeGateway.ToDto()`」だが、現状 `ToDto()` は戻り値が `KsBridgeLabelCell` 固定で Cell 種判別機構が存在しない。Bridge DTO も両OSとも LabelCell 用のみ
- 選択肢: 案A = Cell 種ごとに facade 派生・Snapshot・Bridge DTO を 1:1 で増やす per-type 展開 (`ToDto()` を型スイッチ化) / 案B = 全プロパティ nullable の単一 wide DTO + cellType 判別
- 採用: **案A (per-type 展開)**
- 理由: (1) Native Store の個別型と 1:1 で素直に対応 (2) maui/ADR-0002 が union DTO を却下した経緯と整合 (3) interop DTO は非公開輸送表現 (maui/ADR-0004) で型数増のコストが利用者に露出しない (4) ConversionPathTests へ Cell 種ごとの変換テストを足す既存テスト構造と噛み合う。案Bの弱点は判別の二重化 (C# と native の両方で分解スイッチ) と nullable まみれの実行時検証頼み

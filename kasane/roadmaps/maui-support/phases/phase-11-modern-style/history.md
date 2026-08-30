# phase-11-modern-style 議論履歴

## 2026-08-20: style 公開 API の形

事前調査 (ksn-scout) で確認した実態: Native は style を Theme と別経路で公開 (iOS `KsSettingsViewController.style` / Android `KsSettingsView.style`、どちらも可変プロパティ)。MAUI facade に Theme クラスは無くフラットな BindableProperty 群 → `KsThemeSnapshot` (全項目 nullable = Native 既定委譲) を全量再送する既存パターン。Bridge には style を渡す API が存在せず両 OS とも Classic 固定生成。

- **選択肢 (プロパティ名)**: `Style` は `VisualElement.Style` と衝突するため除外。候補として `ListStyle` / `ViewStyle` / `SettingsViewStyle` を提示、ユーザーから `Appearance` 案が出て候補を再拡張 (`Appearance` / `ListAppearance` / `ListStyle` / `DisplayStyle` / `VisualStyle` / `SectionStyle` は誤誘導で却下)。判断軸は衝突回避・Native 用語 (`style`) との一致・意味の自己説明性。
- **採用**: プロパティ **`ListStyle`** + enum **`SettingsViewStyle { Classic, Modern }`** (非 nullable・既定 `Classic`)。Theme snapshot に同梱せず独立の `SetStyle` 経路で Bridge へ伝搬。
- **理由**: "Style" の語を保ち Native との対応が自明。enum 新設は maui/ADR-0013 の先例どおり。非 nullable は「既定 Classic が両 OS 共通の契約既定で、null の platform 委譲意味論が不要」なため (nullable が正な 4属性との対比)。独立経路は Native の Theme/style 分離との対称性のため。
- **ADR**: maui/ADR-0023 として proposed 起票 (公開 API 名は覆すコスト高・MAUI↔Bridge↔Native の境界を越えるため)。

## 2026-08-20: Theme 4属性の型写像と方向意味論

agenda の「4属性の型写像」「方向意味論の写像」は `sectionMargin` の型選びが方向意味論を決める表裏一体のため統合して議論。

- **自明部分**: `SectionCornerRadius` / `SectionBorderWidth` = `double?`、`SectionBorderColor` = `Color?`。全て nullable で null = platform 既定へ委譲 (maui/ADR-0004・既存 `KsThemeSnapshot` の全項目 nullable パターン)。現行 Theme に矩形 inset 型の前例は無く `SectionMargin` が完全新規。
- **選択肢 (SectionMargin)**: A. `Thickness?` で公開し Left/Right を leading/trailing と解釈 (RTL は Native 解決) / B. 独自 `DirectionalThickness` 型新設 (意味論は型に現れるが TypeConverter 自作・MAUI 慣例から外れ ADR-0004 と緊張) / C. `Thickness` を物理座標とし MAUI 側で FlowDirection 変換 (監視・再送機構が必要で「値の伝搬のみ」の申し送りに反する)。
- **採用**: **案 A**。XAML 組込 TypeConverter (`SectionMargin="16,12"`) がそのまま使え、RTL 解決を Native の既存機構 (`NSDirectionalEdgeInsets` / `LayoutDirection` 付き resolve) に委ねて MAUI 層に機構を持ち込まない。MAUI 標準の Thickness は物理座標で RTL 自動反転しないため、このプロパティに限り論理方向である旨を契約に明記する。
- **ADR**: maui/ADR-0024 として proposed 起票 (公開 API の意味論で覆すコスト高・MAUI 型↔Native directional 型の境界を跨ぐため)。

## 2026-08-20: Bridge 伝搬経路

- **採用**: 「4属性は既存 Theme 経路に同乗・style だけ新設」の最小構成。`KsThemeSnapshot` / `KsBridgeTheme` に7フィールド追加 (margin はフラット論理4成分の `double?`×4・all-or-none、radius/borderWidth は `double?`、borderColor は ARGB `int?`)、Bridge `resolve()` が directional 型を組み立て。実行時変更は既存の全量再送パターンにタダ乗り。style は `IKsSettingsGateway.SetStyle` + Bridge `setStyle:` (enum 序数 int 輸送) 新設で Native 可変プロパティを叩き、`KsSettingsController` が `_style` 保持・Handler 再接続時に再送。生成時引数は不要。
- **却下**: margin の入れ子 DTO (@objc binding の面積増だけで利点なし)。
- **留意**: `setStyle` は Store を通らない初の Bridge API。Native 側も style は Store 外なので対称であり、ADR-0023 の独立経路決定がカバー。実装時に maui/ADR-0002 (Store 操作 1:1) との関係を注記する。
- **ADR**: 起こさない (配線詳細。境界レベルの判断は ADR-0023/0024 が既にカバー)。

## 2026-08-20: 未指定時の既定値の写しとバリデーションの委譲

- **採用**: MAUI 側は完全素通し。既定値定数を持たず null を素通し (Native の resolve 時に style 別 platform 既定へ解決、既定値変更へ自動追従)、ドキュメントに具体値を書かない。負値正規化・radius clamp も Native の描画時正規化に委譲し、facade で validateValue / coerce / 例外送出をしない。
- **理由**: 契約の所有者は Native (「MAUI は値の伝搬のみ」の申し送り)。Native 契約「Theme 構築時には拒否しない」なので facade が先に弾くと挙動非対称。clamp 規則の二重実装は乖離の温床。既存 Theme 項目の素通し先例とも一致。
- **却下**: facade での早期負値検出 (開発時に気づきやすいが、Native が受け入れる値を MAUI が拒む非対称のデメリットが上回る)。
- **ADR**: 起こさない (論点2の決定と既存パターンの帰結の確認)。

## 2026-08-20: Classic での sectionMargin 上下のみ適用の表現

- **採用**: doc-only。値は素通しのまま、`SectionMargin` の XML doc と facade 契約に「Classic では上下成分のみ適用・左右無視 (全幅契約)」を明記。Native concepts (list-appearance) の記述を facade 契約へ写すだけ。
- **却下**: Classic 用の別プロパティ (Native に無い区分の発明 = 新たな視覚契約)、facade での Classic 時左右成分削ぎ落とし (Native が既に無視するので無意味な二重実装、style 切替のたびに再送が必要になる)。
- **ADR**: 起こさない (doc の書き方の話で3基準に該当せず)。

## 2026-08-20: サンプルページ

- **前提**: Native は両 OS とも実装済み (iOS `SectionDecorationDemoView/Controls/Preset.swift` + `SampleTheme.swift` / Android 同名 .kt 構成)。MAUI 側 (7ページ) には未実装。
- **採用**: Native の `SectionDecorationDemo` 一式を正に、MAUI へ sample-parity (cross/ADR-0016) で1ページ追加。Classic/Modern 切替 + 4属性 preset controls、`MenuPage` 登録。ファイル命名は既存ページの parity 対応慣行に実装時に倣う。サンプル用の新色既定は足さない (既存 MAUI 互換 Theme の PaleBackColorPrimary 下地が視認性前提を満たす。「Modern は新たな色既定を導入しない」とも整合)。
- **ADR**: 起こさない (サンプル追加は既存規約 cross/ADR-0016 の適用)。

→ 全論点解消。次のステップは ksn-propose によるフェーズ提案化。

## 2026-08-20: 提案化と相方スペックレビューによる精密化

ksn-propose で change **add-maui-modern-style** (M 級) を作成。相方 (codex) のスペックレビュー (second-opinion-spec-001) の指摘6件を全採用し、以下を決定事項から更新:

- **Bridge 経路の精密化**: 「Handler 再接続時再送」は既存 lifecycle (gateway は Host 解放をまたいで作り直されない) と不整合と判明。契約を「Bridge が style を Host 外で保持し makeHost 生成時に適用 + facade は gateway 初回接続時に配信」へ置き換え。
- **非有限数 (NaN・±∞) の扱い** (オーナー判断・案 A): Native の描画時正規化を「非有限→0」へ拡張する堅牢化デルタ (settings-view-ios-ui / settings-view-android-ui) を change に含める。MAUI の完全素通し決定は維持。
- **サンプル初期 style**: native デモの初期値は Modern (両 OS 実コードで確認)。parity に合わせ Scenario を修正。
- 細部: margin DTO の部分 null は全体未指定として解決 / 定義域外の style 序数は Classic へ正規化 / 視覚照合はチェックリスト化。

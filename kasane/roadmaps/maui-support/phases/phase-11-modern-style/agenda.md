# Modern style の MAUI 伝搬

Native (iOS / Android) の `KsSettingsViewStyle.Modern` 完全実装 (kasane/changes/implement-modern-style) を MAUI から利用できるようにする。style の公開 API 自体が MAUI には存在しないため、ゼロからの新設になる。

## 論点

(なし — 全論点解消済み)

## 申し送り (implement-modern-style から)

- Modern の視覚契約 (Section 単位 margin・箱は Cell 行のみ・Header/Footer 箱外・左右対称の中間 separator・合成順・空 Section・長い Section の箱端) は Native 側で確定済み。正は蒸留後の concepts (styling/list-appearance) と Native コード・テスト。MAUI は値の伝搬のみで新たな視覚契約を作らない
- Modern は新たな色既定を導入しない (second-opinion-spec-001 M1 の決着)。箱の視認性はアプリの Theme 指定に依存する

## 決定事項

- **style 公開 API の形** (2026-08-20): `SettingsView` に新設 enum `SettingsViewStyle { Classic, Modern }` の非 nullable BindableProperty **`ListStyle`** (既定 `Classic`) を生やす。`Style` は `VisualElement.Style` (XAML Style) と衝突するため使用不可。Theme snapshot には同梱せず、Native の「Theme は Store 経由・style は View/Controller プロパティ直接」の分離に対称な**独立の `SetStyle` 経路**で Bridge へ渡す (Bridge には現状 style API が無く新設)。enum は maui/ADR-0013 の先例 (意味軸で MAUI 統一 enum 新設) に従う。非 nullable とするのは、4属性と違い既定 Classic が両 OS 共通の契約レベル既定で、null に「platform 既定へ委譲」の意味が無いため。→ maui/ADR-0023 (proposed)
- **Theme 4属性の型写像と方向意味論** (2026-08-20): 4属性は全て nullable の BindableProperty で公開し、null = platform 既定へ委譲 (maui/ADR-0004・既存 `KsThemeSnapshot` パターン踏襲)。`SectionCornerRadius` / `SectionBorderWidth` は `double?`、`SectionBorderColor` は `Color?` (ARGB int パック輸送)。`SectionMargin` は **`Thickness?` で公開し、`Left` / `Right` を leading / trailing (論理方向) として解釈**して Native の directional 型 (`NSDirectionalEdgeInsets` / `PaddingValues(start,end)`) へ直接写す。RTL の左右反転は Native の解決機構に委ね、MAUI 層は FlowDirection を監視しない (「MAUI は値の伝搬のみ」の申し送りに準拠)。MAUI 標準の Thickness は物理座標のため、このプロパティに限り論理方向である旨をドキュメントに明記する。→ maui/ADR-0024 (proposed)
- **Bridge 伝搬経路** (2026-08-20): 4属性は既存 Theme 経路に同乗 — `KsThemeSnapshot` / `KsBridgeTheme` に7フィールド追加 (margin はフラットな論理4成分 `SectionMarginTop/Leading/Bottom/Trailing` の `double?`×4、all-or-none で詰める。radius / borderWidth は `double?`、borderColor は ARGB `int?`)。Bridge の `resolve()` が4成分から directional 型を組み立てる。入れ子 DTO は binding 面積が増えるだけで不採用。実行時変更は既存の `propertyChanged → ApplyTheme() → 全量再送` にタダ乗り。style は `IKsSettingsGateway.SetStyle` + Bridge `setStyle:` を新設し enum 序数 int 輸送 (既存 KsWireValues 先例)、Native の可変プロパティを叩く (実行時再構築は Native の didSet/setter が担う)。`KsSettingsController` が `_style` を保持し gateway 初回接続時に配信、**Bridge が style を Host 外のフィールドで保持して `makeHost*` 生成時に適用** (releaseHost 後の再生成でも維持 — gateway は Host 解放をまたいで作り直されないため「再接続時再送」は成立せず、相方スペックレビュー Major 1 で精密化)。生成時引数は足さない。`setStyle` は Store を通らない初の Bridge API だが、Native 側でも style は Store 外なので対称 (ADR-0002 との関係は実装時に注記)。
- **既定値・バリデーションの完全素通し** (2026-08-20): MAUI facade は 4属性の既定値定数を持たず null を素通しし、Native の resolve 時の style 別 platform 既定解決に委譲する (ドキュメントに具体値を書かず「platform 既定へ解決」とだけ書く — Native 側の既定値変更へ自動追従)。負値の 0 正規化・radius の幾何 clamp も Native の描画時正規化に委譲し、facade で `validateValue` / coerce / 例外送出をしない (Native 契約「Theme 構築時には拒否しない」との対称性維持・clamp 規則の二重実装回避)。
- **Classic での sectionMargin 上下のみ適用は doc-only** (2026-08-20): API では表現せず (別プロパティ化・facade での左右成分の削ぎ落としは却下)、値は素通しのまま `SectionMargin` の XML doc と facade 契約に「Classic では上下成分のみ適用され、左右は無視される (全幅契約)」を明記する。style 切替時に同じ Theme が両 style へ正しく効く。
- **サンプルページ** (2026-08-20): Native の `SectionDecorationDemo` 一式 (iOS: `SectionDecorationDemoView/Controls/Preset` + `SampleTheme` / Android: 同名 .kt 構成) を正として、MAUI に sample-parity (cross/ADR-0016、一字一句・構成一致) で1ページ追加する。Classic/Modern の style 切替 + 4属性の preset 切替 controls を持ち、`MenuPage` に登録。正確なファイル命名は MAUI サンプル既存ページの parity 対応慣行に実装時に倣う。サンプル用の新しい色既定は足さない — 既存 MAUI 互換 Theme (PaleBackColorPrimary 下地) が箱の視認性前提 (`backgroundColor` × `cellBackgroundColor` の対比) を満たす。既存 public API のみで構成可能。

## TODO

- [x] 論点の解消 (2026-08-20 全6論点を決定事項へ昇格)
- [x] ksn-propose で変更提案を起こす (→ changes/archive/2026-08-20-add-maui-modern-style)

## 実装結果 (2026-08-20 反映)

change: [changes/archive/2026-08-20-add-maui-modern-style](../../../../changes/archive/2026-08-20-add-maui-modern-style/proposal.md) (M 級・review-002 APPROVED・verify-001 VALID・deviation なし)

- 決定事項どおりに実装完了。maui/ADR-0023・0024 は実装結果の帰結を追記のうえ accepted へ昇格
- 実装中の追加事実: Android の輸送層は Compose 標準 `PaddingValues(...)` ファクトリが構築時に 0 以上を要求するため、素通し契約の成立に非検証実装 `KsBridgeSectionMargin` が必要だった (review-001 Critical → 解消。maui/ADR-0024 Consequences に記録)
- proposal の native 堅牢化 (非有限 → 0 の描画時正規化拡張、両 OS) も実装済み — concepts `core/styling/list-appearance.md` へ反映
- 申し送り: review-002 の任意提案 2 件 (`KsBridgeSectionMargin` ⇔ `RawPaddingValues` の KDoc 相互参照 1 行 / iOS テストヘルパ `firstRowLeading` の失敗時 `XCTFail` 化) は、レビュー自身が「マージを止めない任意」と明記しており対応不要と判断して見送り (対応する後続フェーズなし)

# Proposal: scroll-indicator-visible-not-applied

## Why

Theme のプロパティのうち 2 組が、宣言・輸送されているのに画面へ反映されていない (2026-09-17 の棚卸しで Theme 全項目 / CellStyle 14 項目 / MAUI の BindableProperty 51 個を確認し、未適用はこの 2 組のみ)。

- **スクロールバーの表示 (`scrollIndicatorVisible`、既定 true)**: iOS / Android とも反映箇所が無い。Android は一覧がスクロールバー無しで生成されるため常に出ず、iOS は OS 既定に乗っているだけで false を指定しても消えない。`../ColorAnalyzer` の AiForms からの移行で、Android のスクロールバーが出ない退行として見つかった
- **Header / Footer の背景色 (`headerBackgroundColor` / `footerBackgroundColor`)**: Android は反映するが iOS は反映しない (concepts に既知の platform 非対称として記録されている)

Header / Footer の背景色を iOS にも反映すると、既定値 (light #F2F2F7) のままでは、何も指定していない iOS 画面に薄いグレーの帯が新たに出る。移植元 AiForms の既定は透明であり、オーナー判断 (2026-09-17) で既定を両 OS とも透明に改める。これは core/ADR-0030 Decision 1 (ライト側の既定値は現行値のまま) の一部改訂にあたり、core/ADR-0032 (proposed、amends 0030) として起票した。

## What Changes

- **スクロールバー**: `Theme.scrollIndicatorVisible` を iOS / Android の設定リストの縦スクロールバー表示へ、初期構築時と Theme 差し替え時の両方で反映する。既定 true・縦のみ・公開 API は不変。PickerCell の選択面の候補リストも同じ設定に従わせる (開いた時点の値。既定では出る — オーナー指示 2026-09-17)。Android の有効化は設定リストと候補リストに限定し、回転ホイール (数値・日付・時刻) など他のライブラリ所有リストへ波及させない
- **利用者所有コンテンツの Context (Android)**: スクロールバー有効化のために一覧の生成元 Context を同梱テーマ側へ替えても、CustomCell の内容と任意 View の Header / Footer には、ホストが `KsSettingsView` に渡した Context が従来どおり届く
- **Header / Footer 背景色の反映 (iOS)**: text 形式の Section / Root Header・Footer の領域に Theme の背景色を反映し、Theme 差し替えと外観切替に追従する。View 形式の accessory は塗らない (Android と同じ範囲)
- **Header / Footer 背景色の既定を透明に (iOS / Android)**: light / dark とも既定を透明にする。iOS は公開定数 (`Theme.defaultHeaderBackgroundColor` / `defaultFooterBackgroundColor`) の値を、Android はライブラリ所有の light / dark 既定セット (`KsSettingsViewDefaults`) の値を改める。iOS の公開定数は他の既定色と同じ「両外観の値を持つ色」の形のまま値だけ透明にし、等価性の契約を保つ (core/ADR-0032 Decision 2)。未指定色を描画時に既定へ解決する仕組み (core/ADR-0030) は変えない
- **MAUI**: facade・bridge の実装と wire 形式は変えない。未指定を null で渡す現行契約のまま native の契約に追随するため、MAUI の画面でも見た目が変わる (Android でスクロールバーが出る・既定の帯が消える、`ScrollIndicatorVisible = false` が両 platform で効く)。この追随を契約として明記し、テストと実行面の確認で担保する
- **夜間モード変更 (Android)**: Activity を再生成しない夜間モード変更に、スクロールバーのつまみの外観を追従させる。リスト端の overscroll 効果の色は、生成済みの一覧を維持したまま差し替える手段が platform に無いため保証の対象外とする
- 影響能力: settings-view-ios-ui / settings-view-android-ui / maui-bridge (契約の明記とテストのみ)

## Non-Goals

- **`skills/` と README の追従** — 既定色の記載が古くなるが、追従は docs-refresh スキルの責務でユーザーの明示依頼により別途行う (AGENTS.md の運用)
- **横スクロールバー** — 設定リストは縦にしかスクロールしないため、AiForms の `vertical|horizontal` は引き継がない (探索で決定)
- **View 形式の Header / Footer への背景色の適用** — Android も適用しておらず、hosted view の見た目は利用者の所有物であるため (探索・実装時に Android の範囲へ揃えると決定)
- **スクロール位置の制御 (ScrollTo 系)** — maui-support ロードマップ phase-8-scroll-control の領分

## Impact

- **利用者可視の変更 (既定の見た目)**: (1) Android (MAUI の Android を含む) で、何も指定していない画面の設定リストと PickerCell の候補リストにスクロールバーが出るようになる。(2) Android (同) で、今出ている Header / Footer の既定の帯 (light の薄いグレー) が消え、list 下地が見える。iOS の既定の見た目は変わらない。いずれもリリースノートに明記する
- **公開 API**: 型・シグネチャの変更なし。iOS の公開定数 `defaultHeaderBackgroundColor` / `defaultFooterBackgroundColor` は残し、値だけが変わる。beta 配信中 (0.1.0-beta.2) のため互換経路は置かない
- **既存の決定との関係**: core/ADR-0030 Decision 1 のうち Header / Footer 背景の既定値だけを core/ADR-0032 (proposed、amends 0030) で置き換える。仕組み (未指定色の描画時解決・明示色の扱い・Theme の型・等価性の契約) は core/ADR-0030 のまま。既定値が platform 間で同じであることの検証 (定数比較テスト) は新しい値に追随させる
- **リスク**: Android で一覧の生成元 Context を替えることによる副作用。利用者所有コンテンツの Context は回帰テストで固定し、他のライブラリ所有リストへの波及は独立レビューの指摘 (review-001) に基づき限定する
- **実装の現況**: S 級として着手済みで、スクロールバーの反映・Context の維持・iOS の背景色反映は作業ツリーに実装とテストがある (review-001 の修正サイクル中)。本提案で新たに加わる実装は既定値の変更とその追随のみ
- **`ui/` (brief・モック) は作らない (ハーネス規約からの合意済みの逸脱。オーナー確定 2026-09-17)**: UI に触れる変更は級を問わず `ui/` とモック承認が必須だが、本 change は見た目として新たに決める要素が無いため省略する。レビュー・verify はこの省略を違反として扱わない。スクロールバーは OS 標準の描画をそのまま使い、Header / Footer は既定で背景を描かない (list 下地が見える) だけで、配色・寸法・配置の選択を伴わない。見た目の確認は tasks 5.2 の実機証跡 (両 OS・Classic / Modern の撮り比べ) で行う
- 長命層: core/styling/list-appearance.md (既知の非対称の記述)・style-resolution.md (既定色の表) ほかの記述が変わる (蒸留で追随)

## 級: M

既定値の変更 (公開契約) を含み iOS / Android にまたがるが、変更の実体は既定色 2 つと反映処理の追加で、能力間で揃えるべき設計判断が小さいため (オーナー確定 2026-09-17。当初 S から引き上げ)。

domain: cross

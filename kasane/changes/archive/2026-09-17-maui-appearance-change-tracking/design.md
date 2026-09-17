# Design: maui-appearance-change-tracking

## Context

MAUI iOS で `SettingsView` を表示したまま外観をダークへ切り替えると、行の背景がライトのまま残り文字だけが dark 既定へ解決される ([proposal.md](proposal.md) の Why、[exploration.md](exploration.md) の調査記録)。iOS Native 本体は既定色を dynamic `UIColor` で持つが、外観変化を受けて行の背景設定を貼り直す機構を持たず (trait 購読はチェックボックスと Section 箱の枠線だけ)、UIKit の自動更新に全面依存している。Android は本体が夜間モード変化を拾って Theme を再解決・再適用する ([core/ADR-0030](../../decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md) の「View は uiMode 変更で未指定色を再解決する」)。MAUI iOS handler は Native の view controller を親 Page の child として embed する契約 (`kasane/concepts/maui/api/maui-rendering-lifecycle.md`) だが、親が解決できないと黙って成立する経路がある。

相方の提案レビュー ([second-opinion-spec-001.md](second-opinion-spec-001.md)) の照合で、iOS の描画経路が Theme の `headerBackgroundColor` / `footerBackgroundColor` を消費していない (Android は消費) ことも分かり、隣接課題として同梱する。

## Goals / Non-Goals

Goals と Non-Goals は [proposal.md](proposal.md) のとおり。要約: 表示中の外観変更で未指定色が両方向に追随し (iOS 本体と MAUI iOS 経路)、iOS テストが描画値を観測し、Theme プロパティの `AppThemeBinding` が検証ホストで固定され、iOS の text Header / Footer 背景に Theme の色が効く。Cell 単位の `AppThemeBinding` (element ツリー接続) は spike の記録のみ。

## Decisions

### Decision 1: iOS の外観変化の観測主体は view controller の view 1 箇所とし、既存の Theme 再適用経路で全領域を再適用する

**採用案:** `KsSettingsViewController` の view (collection view を持つ view) が外観 (`UITraitUserInterfaceStyle`) の変化を観測する唯一の主体になる (iOS 17+ は `registerForTraitChanges`、iOS 16 は `traitCollectionDidChange` のフォールバック — `KsCheckBoxView.swift` と同じ 2 経路)。変化を受けたら、既存の Theme 再適用経路 (構造・内容更新と分かれた Theme 経路、`kasane/concepts/core/architecture/display-state-synchronization.md`) を現在の Theme のまま再実行し、表示中の標準 Cell (`KsListCellBase`)・CustomCell (`CustomCellView`)・list 下地・Section 装飾 (`SectionBoxDecorationView`)・text Header / Footer の背景設定を現在の外観で貼り直す。行は作り直さず、可視行の集合とスクロール位置は変えない。Theme の値そのものは変わらない (dynamic `UIColor` を再度適用するだけ) ので、同値 Theme を再適用しない既存の早期 return は「外観変化起因の再適用」では通さない。

**理由:** Android と対称の契約 (ライブラリ自身が外観変化で未指定色を再解決・再適用する) になり、UIKit の自動更新に依存しない。観測主体が 1 つなら、標準 Cell と CustomCell という 2 系統の背景設定経路と、行以外の描画領域 (下地・装飾・supplementary) をまとめて覆える。

**代替案:**
- **A: 行ごとに購読する (`KsListCellBase` と `CustomCellView` の双方)** — 却下。2 系統に購読が分散し、list 下地・Section 装飾・supplementary の再適用主体を別に持つ必要が残る。行単位のテストだけ通して他領域が古いまま残る実装を許す
- **B: 現状維持 (UIKit の自動更新に任せる)** — 却下。MAUI 経路で現に破綻しており、iOS Native 単体でも保証の根拠が無い (既存テストは保持値を自前解決していて検出力が無かった)。Android との非対称も残る
- **C: 描画関数 (`CellBaseLayout.swift`) で観測する** — 却下。描画関数は購読の寿命を所有できない

### Decision 2: MAUI iOS の child view controller の結び付けは必ず確立し、失敗を黙って成立させない

**採用案:** `KsHostContainment` は、Host 生成時と `ConfirmAdded` の再試行に加えて、親 Page の view controller が確定する契機 (Handler 接続完了・`Loaded` 後の window への取り付け) でもう一度親を解決し、`AddChildViewController` → `DidMoveToParentViewController` の順で containment を確立する。最後まで親が見つからない場合は診断ログを出す (handbook `cross/diagnostic-message-language.md`)。Handler の切断・再接続で親付けが二重・残留にならないことをテストで固定する。切り分け (tasks 0.3) で MAUI 経路が原因でないと分かった場合、この Decision は「現状の結び付けが成立していることの確認」に縮退し、実装変更は行わない。

**理由:** MAUI の Host は親 Page の child VC として embed する契約で、UIKit の custom container の契約 (子 VC の view を追加する前に containment を確立する) にも沿う。外観 (trait) の伝播だけでなく appearance / lifecycle forwarding も containment に依存するため、trait だけを別経路で届ける解は不正状態を残す。

**代替案:**
- **A: 親 VC 無しでも window 直下から trait を Native の view controller へ届ける** — 却下。UIKit の containment 契約に反し、外観だけ直っても lifecycle / appearance forwarding が不正のまま残る。view-only 利用の公式サポートは別の設計判断 (proposal の Non-Goals)
- **B: 親の解決を「接続後まで遅らせる」だけにする** — 却下。`ConfirmAdded` が既に取り付け後に再試行しており、新しい解にならない。契機の特定と失敗時の可視化が要る

### Decision 3: iOS テストの観測境界は「描画に効いている値」とし、保持値の自前解決を根拠にしない

**採用案:** 表示中の外観切替を検証するテストは、行の描画画像の背景領域の画素、または行の背景 view の layer に実際に載っている色を読む。list 下地が既に描画画像の画素で観測されているのと同じ流儀に統一する。`backgroundConfiguration?.backgroundColor` を `resolvedColor(with:)` で検証側が解決する観測は禁止する。修正を一時的に外した状態でテストが赤になること (検出力) を tasks 3.5 で確認する。

**理由:** 保持している dynamic `UIColor` を自前解決した値は、UIKit が実際に貼り直したかを示さない。現に既存テストは不具合を検出できなかった。

**代替案:**
- **A: 保持値の自前解決のまま、修正側にログを足す** — 却下。テストが実装の意図しか固定できず、回帰を検出しない

### Decision 4: iOS の text Header / Footer の背景には Theme の色を適用し、View accessory には適用しない

**採用案:** Section と Root の text の Header / Footer の背景設定に Theme の `headerBackgroundColor` / `footerBackgroundColor` を適用する (未指定は既定の dynamic 色)。利用者の view を載せた Header / Footer の背景は塗り替えない。Decision 1 の再適用対象に含める。

**理由:** Android は `SectionAccessoryViewHolders` で同じ項目を消費しており、iOS だけ Theme の項目が死んでいるのは parity の穴。外観追随の保証対象を語るとき、消費されていない項目を含められない。View accessory の見た目は利用者のもの (`core/ADR-0021` / `0023` の accessory 契約)。

**代替案:**
- **A: 適用しないまま (現状)** — 却下。parity の穴が残り、Theme の公開項目が iOS で無意味なまま
- **B: View accessory の背景も塗る** — 却下。利用者の view の見た目を侵食する
- **C: 別 change として起票** — 却下。同じ描画箇所 (accessory を list cell へ適用する処理) を本 change の Decision 1 で触るため、分けると同じ箇所を二度触る (オーナーの方針: 隣接課題は同じ change で直す)

## Risks / Trade-offs

- Decision 1 の再適用が Theme の早期 return (同値なら再適用しない) と衝突する。外観起因の再適用は別の入口にし、Theme 更新の同値判定を変えない
- 再適用が行を作り直す実装になると identity・スクロール位置が壊れる。spec の往復 Scenario (tasks 3.6) が歯止め
- Decision 4 で `headerBackgroundColor` を明示していた利用者には初めて色が効く (見た目の変化)。既定色は list 下地と同じ値なので Theme を渡さない画面は変わらない
- Decision 2 で親の解決契機を増やすと、ページ再訪問時に二重に `AddChildViewController` する事故があり得る。`ParentViewController` の事前チェックとテストで防ぐ

## Migration Plan

利用者側の作業は無い。公開 API・wire 形式は変わらない。次の prerelease のリリースノートに「iOS: 表示中の外観切替で行背景が追随するようになった / text Header / Footer に Theme の背景色が効くようになった」を記載する。

## Open Questions

- tasks 0.2 / 0.3 の切り分け結果で Decision 1 / 2 のどちらが実装を伴うかが決まる (両方の可能性もある)。切り分け前に確定しない
- MAUI 層ユニットテストで `AppThemeBinding` の再評価を再現できるか (tasks 4.2 の調査)

## ADR 候補

- Decision 1: 「iOS も本体が外観変化を観測して未指定色を再適用する (Android と対称)」は core/ADR-0030 の iOS の実現方法 (dynamic `UIColor` に任せる) を補う契約変更。覆すコストが中程度で 3 platform の対称性 (境界) に関わるため、ADR-0030 への追記 (amends) 候補として蒸留で判断する
- Decision 4: 契約の追加だが ADR-0030 の既定色表 (Header / Footer 背景の light / dark 値) が既に存在し、その適用先を iOS で埋めるだけ。ADR は不要 (concepts の追随のみ)
- Decision 2 / 3: 実装と検証の流儀であり ADR 不要

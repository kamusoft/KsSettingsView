# Exploration: ios-customcell-initial-height-grow-animation

## 課題 / 動機

(2026-09-17、`../ColorAnalyzer` の change `migrate-remaining-pages-to-kssettingsview` より越境の簡易起票。出典: `../ColorAnalyzer/kasane/changes/migrate-remaining-pages-to-kssettingsview/session.md`)

**症状** (オーナーの実機観察。iOS 実機 iPhone 11、表示言語は日本語、KsSettingsView.Maui 0.1.0-beta.4 系):
利用側アプリのリアルタイム色設定ページへ遷移した直後、先頭 Section の `CustomCell` (中身は `ContentView Padding="16"` + 折り返し `Label`) の**高さが後から増え**、それに合わせて後続の Section 以下がわずかにスライドダウンする。AiForms.Maui.SettingsView には無かった挙動。機能面の問題は無い。

**依頼元の調査で読んだ根拠** (起票時に実物で行の実在を確認し、行番号を実物に合わせた。因果は未検証):

- 幅未確定の間は無限幅で測る: `maui/KsSettingsView.Maui/Platforms/iOS/KsAccessoryHostView.cs:49-59` の `IntrinsicContentSize` は `Bounds.Width` が 0 の間 `double.PositiveInfinity` を幅制約にして measure する (折り返し Label は無限幅だと 1 行分の高さを返す)。同 `:70-80` の `LayoutSubviews` で幅の変化を検知すると `InvalidateIntrinsicContentSize()` し、実幅で測り直す
- この wrapper は accessory と CustomCell の content で共通: `maui/KsSettingsView.Maui/Platforms/iOS/KsViewMaterializer.cs:17-23`。Android 版は `OnMeasure` の measureSpec で測るため、この状態は無い (iOS 固有と見られる)
- content には Native へ再計測を送り直す経路が無い: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:873-893` (remarks のとおり意図的で、`:892` のコールバックは `static () => { }`)。iOS は intrinsic size の無効化 → self-sizing 任せで、`ios/Sources/KsSettingsViewBridge/KsBridgeCellContentHostView.swift:188-193` (`layoutSubviews` 内で高さの変化を検知して `invalidateIntrinsicContentSize`) → `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentView.swift:40-53` (`sizeThatFits`) → `ios/Sources/KsSettingsViewUI/CustomCellView.swift:101-106` (`preferredLayoutAttributesFitting`) と伝わる
- `ios/Sources/KsSettingsViewUI/CustomCellRowPlacement.swift:19-21` のコメントが「行の高さは content のサイズ変化より 1 レイアウトパス遅れて追いつく」と明言している。この Layout は content のはみ出し対策で、行そのものの高さ遷移と後続行のシフトは抑止していない
- `ios/Sources/` 配下に `UIView.performWithoutAnimation` / `setAnimationsEnabled(false)` は無い。あるのは `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:123` の mask 更新用 `CATransaction.setDisableActions(true)` のみ。内容差し替え時の snapshot 適用は `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1948` で `animatingDifferences: true` (同ファイルの他の `apply` 呼び出しも同様)

**関連する既存の記録**:

- `kasane/decisions/maui/0016-mauiview-materialization-self-measuring-wrapper.md` (自己計測 wrapper の決定。起票時に存在のみ確認)
- `kasane/decisions/maui/0020-customcell-content-live-view-generation-token.md`
- `kasane/decisions/ios/0002-customcell-hosting-recreation-accepted.md`
- `kasane/concepts/maui/architecture/view-materialization.md`
- `kasane/roadmaps/maui-support/phases/phase-5-custom-cell/artifacts/probe/2026-08-12-cell-content-size-follow.md`
- 関連: `kasane/changes/android-accessory-view-late-insert-animation` (Android 固有の FooterView 遅延挿入。別症状のため合流させない)

**利用側の状況**: ColorAnalyzer は**本体の修正を先に行う方針** (オーナー決定 2026-09-17)。利用側では回避策 (Label の高さ固定・テキストの見出しへの移動) をあえて入れず、`../ColorAnalyzer/ColorAnalyzer/Views/Others/RealtimeColorSettingsPage.xaml` の先頭 Section の CustomCell を修正確認のサンプルとして残している。

## 検討した選択肢 (却下案と理由を含む)

(2026-09-18 探索。裏取り: MAUI 本体 `MauiView.cs` (main / release/10.0.1xx) と WWDC22 Session 10068 の `selfSizingInvalidation`)

### 見立て (因果の仮説。実機ログでの確認は未了)

iOS では CustomCell の行の高さも Section Footer の高さも、自己計測 wrapper (`maui/KsSettingsView.Maui/Platforms/iOS/KsAccessoryHostView.cs`) が答える `IntrinsicContentSize` で決まる。

1. 最初の問い合わせ時点で wrapper は幅を持たない (`Bounds.Width` = 0) ため無限幅で Label を測り、1 行ぶんの高さを答える。行はその高さで作られる
2. 最初の配置で wrapper に実幅が渡り、MAUI は Label を実幅で配置する。Content が最初のフレームから 4 行ぶんに描かれ、行の枠からはみ出す (実機証跡と一致)。`LayoutSubviews` が幅の変化を検知して必要サイズを無効化する
3. 無効化が `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentHostView.swift` の高さ変化検知を経て UICollectionView の self-sizing 再計算に届く。iOS 16 以降、表示中の行の高さ変化は既定でアニメーションされる (`selfSizingInvalidation`)。約 300ms かけて伸びるのはこれ

「content の後着」ではなく「初回の答えが幅なしで出ている」ことが芯。Footer も同じ wrapper なので同型 (ただし Footer は Auto Layout 経路で、Native 側 `applyAccessoryToListCell` の uiKit backing が四辺を制約で留める)。

### 案の比較

| 軸 | A: 初回の計測を幅付きにする | B: 補正のアニメーションを止める | C: A + B |
|---|---|---|---|
| 何が直るか | 最初のフレームから正しい高さ。ずれ自体が消える | 伸びる動きが消えるが、ずれは一瞬で起きる | A で芯を消し、後からの幅変化の補正も静かにする |
| 触る場所 | CustomCell は Bridge の幅付き問い合わせ (`KsBridgeCellContentView.sizeThatFits` が `intrinsicContentSize` ではなく `sizeThatFits(幅, ∞)` を使う → `MauiView.SizeThatFits` → `CrossPlatformMeasure`)。Footer は wrapper か Native 側で幅のヒントを与える | 無効化の呼び出しを `UIView.performWithoutAnimation` で包む (Swift 側・C# 側) | 両方 |
| 確からしさ | `MauiView.SizeThatFits(width, ∞)` は設計上想定の経路。SwiftUI が初回に有限幅で問い合わせるかは probe が要る | Apple 公式の正攻法だが、発生源が collection view のバッチ更新側だと効かない報告あり。実機でしか判定できない | 同左 |
| 落とし穴 | `MauiView` は制約ペアで計測結果をキャッシュする (`IsMeasureValid` / `_lastMeasuredSize`)。内容変化時に `InvalidateMeasure` を呼ばないと古い高さを返す (2026-08-12 probe が残したリスク)。`InvalidateIntrinsicContentSize()` / `SetNeedsLayout()` ではキャッシュは消えない | 正当な後続の高さ変化 (展開操作など) のアニメーションまで消えると退行。範囲の絞り込みが要る | 両方 |

- B 単独は却下: 症状の芯 (初回の高さが違う) を残したまま見え方だけ変える。後続 Section のずれは一瞬で起きる形で残る
- C は A の結果を実機で見てから判断する (B が効くかは実機でしか分からない)

いずれの案も maui/ADR-0016 (wrapper の `IntrinsicContentSize` override と `MeasureInvalidated` 中継) と maui/ADR-0020 (行高さは wrapper の計測無効化で追従、native 通知不要) とは衝突しない。A は intrinsic 経路を残したまま幅付き経路を足し、native への再計測通知も増やさない。

## 決定事項

- (2026-09-18) 直し方の主軸は **A (初回の計測を幅付きにして症状の芯を消す)**。B は保険として A の実機確認の後に要否を見る
- 合否はシミュレータでは判定できない (再現しない) ため、iPhone 11 実機の目視で行う。実装の前に「wrapper が初回にどの幅で測ったか」を実機ログで 1 回確かめる工程を挟む (未解明の 3 点もまとめて片付ける)

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

- 作成済み: maui/ADR-0028 (proposed、amends maui/ADR-0020) — 行・領域の高さは初回から幅付きで wrapper に問い、intrinsic の無効化は内容変化の追従にだけ使う。アニメーション抑止のみの案は却下、保険としての要否は実機確認後

## 未決の論点

- **未探索 (簡易起票)** — 上記の根拠は依頼元のコード読みを行の実在まで確認しただけで、因果の検証・議論はしていない。コード読みを事実とせず、実機の観察を正とする
- **未解明**: 同じアプリの解析設定ページにも XAML 上完全に同型の説明行 (文言はより長く日本語で 3 行ぶん。増分はより大きいはず) があるが、そちらではスライドダウンが観測されていない。同じ経路を通るはずなので、差はコードから説明できていない (レイアウト順序・画面構成への依存は推測)。再現条件の特定が要る
- 初回の問い合わせ時点で `Bounds.Width` が実機で実際に 0 になるか、高さ変化が実際にアニメーションとして出る条件は、コードだけでは断定できていない。これを固定するテストも見つかっていない
- iOS シミュレータ (iPhone 17 Pro) の自動確認では気づかれなかった (取得粒度の問題か、機種・幅の問題かは不明)
- accessory (Header / Footer の View) も同じ wrapper を通るので、同種の初回高さのブレが起きるかは未検証 (利用側の観察では、iOS の解析設定ページのフッターに遅延・アニメーションは出ていない)
- 競合する原因仮説 (2026-09-17、`kasane/changes/android-accessory-view-late-insert-animation` の探索より): CustomCell の content も初回配信には載らず `Loaded` 後に遅れて届く (`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:346-354`)。iOS は内容差し替えを `animatingDifferences: true` で適用するので、「無限幅での初回計測」ではなく「content の後着」でも同じ見え方になり得る。あちらの change が content も初回配信に間に合わせる形になった場合は、その後に再現確認をすると切り分けになる
- 直し方は A に決定 (決定事項)。実装の詳細で残る点:
  - SwiftUI (`UIHostingConfiguration` → `CustomCellRowPlacement` → representable) が**初回に有限幅の proposal で `sizeThatFits` を呼ぶか**。呼ばない (幅 nil) 経路では intrinsic に落ちるので、A が効かない可能性がある。probe で確かめる
  - Footer (Auto Layout 経路) への幅ヒントの与え方: wrapper が `Bounds.Width` = 0 のとき superview (`contentView`) の幅で測る案 / Native の uiKit backing が取り付け時に inner の frame 幅を `contentView` 幅で先に与える案。どちらが素直かは実装時に決める
  - `MauiView` の計測キャッシュ: `OnMeasureInvalidated` で `InvalidateMeasure` 相当を呼んでキャッシュを捨てる必要がある
- 実機ログで確かめる 3 点 (見立ての裏取り): ① シミュレータで出ない理由 (初回の問い合わせ時点で幅が既に渡っているか、補正が最初の描画前に収まっているか) ② Footer が 1→3 行ではなく 2→3 行だった理由 (初回が無限幅ではなく「実際より広い有限幅」だった可能性) ③ 解析設定ページの同型の行で出ない理由

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S / M / L (理由)

**S で確定** (2026-09-18、ユーザー確定。ハンドオフ先: ksn-orchestrator での直接実装)。判定材料:

| 材料 | 評価 |
|---|---|
| 性格 | バグ修正 (iOS 実機でだけ出る初回高さのブレ)。機能追加なし |
| 公開 API | 変更なし。MAUI facade・Native Core / UI / Bridge のいずれも公開面に触れない |
| 触る能力 | 1 つ (配置された View の実体化と行・領域への埋め込み: `kasane/concepts/maui/architecture/view-materialization.md` の範囲)。ビルドルートは ios (Bridge の representable、accessory の幅ヒント次第で UI) と maui (wrapper) の 2 つにまたがるが、能力間で揃える設計判断は無い |
| 可逆性 | 局所的で可逆。wrapper の `SizeThatFits` 経路の追加とキャッシュ破棄、representable の問い合わせ方の変更 |
| UI | 見た目の仕様変更なし (ui/ は不要。合否は実機の目視) |
| 重要判断 | maui/ADR-0028 (proposed) に捕捉済み。実装者を縛るのはこの ADR と本メモの「実装で残る点」 |

M に上げない理由: デルタスペックに起こす振る舞いは「折り返す内容を持つ行・領域が最初のフレームから定常状態の高さで作られる」の 1 シナリオで、ADR とこのメモに既に書かれている。proposal を挟む利得が薄い。

S の注意点 (実装フェーズへの申し送り):

- 実装前に **probe を 1 回** 挟む: SwiftUI が初回に有限幅で `sizeThatFits` を呼ぶか (ADR-0028 の未確認前提) と、wrapper が初回にどの幅で測っているか (実機ログ)。崩れていれば ADR-0028 の Revisit When に該当し、探索へ戻す
- 合否は iPhone 11 実機の目視 (Sample `CustomCellDemoPage` の動的高さ Section と `AccessoryViewsDemoPage` の Footer ③)。Simulator では再現しないため、Simulator の自動確認は退行検知にしか使えない
- A の実機確認後に、保険 B (`performWithoutAnimation`) の要否を判断する
- 独立レビューは S でも必須 (ksn-orchestrator)

### 追記 (2026-09-18): Section の FooterView でも同系の高さのブレを実機で観測

出典: `kasane/changes/android-accessory-view-late-insert-animation/evidence/README.md` の「1.3 (iOS 実機) 修正前の観測」(証跡 `before-ios-device-accessory-views-*.png`)。オーナー判断で本 change へ合流 (2026-09-18)。

- iOS 実機 (iPhone 11) の MAUI Sample `AccessoryViewsDemoPage` で、3 行に折り返す Label を持つ Section の FooterView (③) が、画面内容の最初の描画から約 50ms の間だけ 2 行ぶんの高さで、その後 3 行ぶんへ 1 行 (約 19pt) 伸びる。後続の ④ 以下がその分だけ下へずれる
- iOS Simulator (iPhone 17 / iOS 26.1) では観測されない。上記 change の修正前から在り、同 change による退行ではない
- 同じ実機・同じ収録で、CustomCell の行の高さの変動は「CustomCell の MAUI 固有デモ」では観測されなかった
- CustomCell に限らず view accessory でも出るため、原因が自己計測 wrapper (accessory と CustomCell の content で共通) の「幅が決まる前の計測」にある見立てを強める材料。因果は未検証

### 追記 (2026-09-18): CustomCell の行でも本症状を実機で観測 (Sample で再現)

出典: `kasane/changes/android-accessory-view-late-insert-animation/evidence/README.md` の「6.3 (iOS 実機) 修正後の観測」6.3-e (証跡 `after-ios-device-customcell-*.png`)。
あちらの change の修正後ビルドでの観測だが、tasks 6.3 の取り決めにより本症状は本 change の対象として切り分け、合否には含めていない。

- iOS 実機 (iPhone 11) の MAUI Sample `CustomCellDemoPage`「動的高さ」Section (利用規約 = 折りたたみ / プライバシーポリシー = 展開中、本文は 4 行に折り返す `Label`) で、**Content は遷移の最初のフレームから 4 行ぶんの位置に描かれている**のに、**行の高さだけが約 300ms かけて 149px 増える**。後続の Section Footer 領域がその分だけ下へスライドする
- 経過時間と、次の Footer 領域の上端 y: t=0 → 1543、+17ms → 1544、+33ms → 1547、+100ms → 1580、+200ms → 1660、+300ms 以降 → 1692 (初回)。再訪問でも同じ (1543 / 1545 / 1548 / 1583 / 1662 / 1692)
- 途中のフレームでは**本文が行の枠からはみ出して**描かれている (行の高さが content のサイズに 1 レイアウトパス遅れて追いつく、という `CustomCellRowPlacement.swift` のコメントと整合する見え方)
- 同じ画面の他の Section (インライン CustomCell / 再利用 SliderCell) の行の境界は t=0 から落ち着いた状態まで 1px も動かない。**折り返す `Label` を持つ行だけ**で起きている
- 同じ収録日・同じ端末の「CustomCell の MAUI 固有デモ」では起きない (証跡で見る限り、その画面の CustomCell の Content はいずれも 1 行で折り返しが無い)
- **本症状が Sample だけで再現できることが分かった**のが今回の収穫 (これまでは利用側アプリ `../ColorAnalyzer` でしか観測されていなかった)。iOS Simulator (iPhone 17 / iOS 26.1) の同じ画面では初回・再訪問とも観測されないため、**実機でだけ出る**という点は Section FooterView (③) の追記と同じ
- `android-accessory-view-late-insert-animation` が「content も初回配信に間に合わせる」形になった後の観測であるため、上の「未決の論点」に挙がっていた競合仮説 (**content の後着でも同じ見え方になり得る**) は切り分けられた: Content は最初のフレームから在るのに高さだけが遅れるので、**後着では説明できない**

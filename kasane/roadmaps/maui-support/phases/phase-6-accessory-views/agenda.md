# phase-6-accessory-views

Root / Section の Header・Footer に任意 MauiView を設定できるようにする。MauiView→native 実体化機構の初出フェーズ (phase-5-custom-cell が再利用する前提)。

## 論点

(なし — 全論点解消済み)

## 決定事項

- **① MauiView → platform view 実体化機構は案A′「三層配置 + 自己計測 wrapper」** (2026-08-11)
  - 配置は IconSource (maui/ADR-0015) と同型の三層: Handler が per-TFM の materializer seam を注入、`KsSettingsController` が実体化タイミングと寿命を所有、gateway が `object?` → platform 型キャストで輸送
  - seam の産物は bare な platform view ではなく **MAUI 公式骨格で組んだ自己計測 wrapper platform view** (iOS: `MauiView` + `ICrossPlatformLayout` 自前サブクラス / Android: `ItemContentView` 同型の自作 ViewGroup)。wrapper が計測・arrange・`MeasureInvalidated` 中継・破棄 (`DisconnectHandlers`) を自蔵する
  - 生成手順は MAUI 公式骨格: `PropagatePropertyChanged` → BindingContext を Handler 生成より先に設定 → `ToHandler` → detach → attach → 最後に `AddLogicalChild`
  - AiForms のハック (`FindMauiContext()` 親チェーン横取り / `Parent = MainPage` 直代入 / リフレクション descendant 購読) は採らない
  - phase-5 (custom-cell) とは「wrapper + 公式骨格」を共有部とし、accessory / cell 固有ポリシーは上に載せる (AiForms のコピー分岐を反面教師とする)
  - 根拠調査: [素材](#素材) の2本
- **② Bridge の accessory 輸送は案a「native view インスタンスの直接輸送」** (2026-08-11)
  - 新 API `updateAccessoryView(target, sectionID, view: UIView? / View?)` (null でクリア)。Bridge 内部で `KsAnyView.uiKit { view }` / `KsAnyView.AndroidView { _ in view }` の定数返し closure に包んで既存の Store 経路に乗せる — native (Core / UI) は無変更
  - `KsBridgeSection` DTO に `headerView` / `footerView` フィールドを追加し、`setRoot` / `replaceSection` の初期構築経路を text と対称にする (ADR-0015 の platform 画像輸送と同型)
  - 噛み合わせ対策: native の KsAnyView factory はリサイクル時に再呼び出しされるため、Bridge の closure 内で「返す前に既存親から detach」((parent as? ViewGroup)?.removeView 等) を仕込む。MAUI 本体の再親付け作法 (detach → attach) と一貫
  - 案b (factory 輸送) は VisualElement の Handler 1:1 制約で都度生成が構造的に不可能なため却下、案c (binding 範囲拡大) は Swift associated value enum が @objc 非互換で不成立
- **③ view accessory の更新セマンティクスは「差し替え=明示経路で再発行 / 内容変化=再発行せず live 追従」** (2026-08-11)
  - `HeaderView` プロパティの差し替え (新インスタンス): facade が再実体化し、必ず ② の `updateAccessoryView` (明示 Diff 経路) で再発行する。Store の `updateAccessory` に同値スキップは無いことを実証済み ([SettingsRootStore.swift:274](ios/Sources/KsSettingsViewUI/SettingsRootStore.swift)) — 明示経路は KsAnyView の case 等価に握りつぶされない
  - 値比較に依存する経路 (`replaceSection` 由来の差分検出・Android DiffUtil) では view accessory の変化検出を保証しない — view の変更輸送に使わないと明文化
  - 同一インスタンスの内部内容変化 (binding 更新等): Store / Bridge へ何も再発行しない。live platform view が直接描画更新される (「view accessory は参照が正、内容は live」)
  - サイズ変化の native への伝播 (iOS の行高さ再計算含む) は ADR-0016 wrapper の invalidation 中継の責務。iOS の UICollectionView self-sizing は自動で繋がらない可能性があり、その場合の native 側の再計算口は実装フェーズで検証・追加する (native 一般のギャップとしてパリティ整備の範囲。TODO 化)
  - 案B (内容変化も再発行) は live view には再発行するものが無く実質無意味、案C (KsAnyView へ世代トークン) は Core 契約変更で非ゴール抵触のため却下
- **④ 公開 API の形状は AiForms 互換 + 予約名の実体化** (2026-08-11)
  - (a) `Section.HeaderView` / `FooterView` (`View?` 型) — maui/ADR-0008 の AiForms 命名踏襲
  - (b) `RootHeaderView` / `RootFooterView` (`View?`) — phase-2 決定の予約名を実体化 (`RootHeaderText` / `RootFooterText` と対)
  - (c) text と view の競合は **View 優先** (原典の `HeaderView == null ? Text : Custom` 判定を踏襲)。View 非 null の間 text は輸送されず facade が保持、View を null に戻すと text へフォールバックして再発行 (③ の明示経路で)
  - (d) `DataTemplate` 版 (HeaderTemplate 等) は提供しない (原典に無く、テンプレート系は phase-10 の領域)
  - 型を `VisualElement?` に広げる案・競合を後勝ちにする案は却下 (MAUI 慣例・設定順依存の罠)。単独 ADR は起こさない — ADR-0008 の適用であり、(c) の優先規則はデルタスペックで固定する
- **⑦ view accessory の Host 世代管理は「切断時破棄 + 接続時再実体化」** (2026-08-11)
  - 復元の正は VisualElement (Root は controller、Section は Section オブジェクトが所有)。platform wrapper は Host 世代ごとの派生物
  - Handler 切断時: 全 view accessory の wrapper を ADR-0016 の破棄手順で破棄。Section 系は Store 内の stale closure を `updateAccessory` で除去 (text があれば text へ、なければ nil へ書き戻し — Host 不在中は Store のみ更新の既存契約で安全)。Root 系は Store に無いため書き戻し不要
  - Handler 再接続時: `OnHostAttached` (取り付け後 — Android attach-order 罠のため mapper では行わない) で新 MauiContext により再実体化し、Root + Section の全 view accessory を `updateAccessoryView` で再発行 (既存 `ApplyRootAccessory` の拡張)
  - icon lease (ADR-0015 の「release で破棄しない」) とは逆の判断 — UIImage / Drawable は Context 非依存だが View は Context を強参照するため。wrapper 維持案・Section 系を Store 復元に任せる案は Android Context リークで却下
- **⑤ headerHeight との相互作用は「固定値が view accessory にも勝つ」で OS 対称化 (案A)** (2026-08-11)
  - 優先順位: `HeaderHeight` 正値 → 固定 (view でも。はみ出しは clip) / `-1` + Theme.headerHeight → Theme 値 / `-1` のみ → wrapper 自己計測の自動高さ (既定の体験)。デルタスペックに明文化する
  - native は現状 OS 非対称 (iOS は view にも固定が効く、Android は view では常に自動 = 原典準拠) — [align-view-accessory-header-height](../../../changes/align-view-accessory-header-height/exploration.md) (旧 fix-ios-view-header-height-override) の未決論点を本決定で裁定: **iOS 挙動を意図的拡張として確定し、Android を iOS に合わせて対称化する** (原典非準拠を両 OS で受け入れ。契約対称化のパリティ整備であり非ゴール例外 — iOS への replaceCells 追加と同じ理屈)
  - 同 change はこのフェーズで巻き取る (方向は上記に転換。concepts の headerHeight 記述の明文化・ADR を伴う)
  - 案B (原典踏襲 = view は常に自動高さ) は「明示指定が無言で無視される」罠と iOS の公開挙動変更 (既存 iOS 利用者の見た目が変わる) のため却下
- **⑥ サンプルは AccessoryViewsDemoPage 1ページを「MAUI のみの画面」として追加** (2026-08-11)
  - 内容 7 項目: (1) RootHeaderView / RootFooterView (2) Section HeaderView / FooterView (ViewModel バインド付き = BindingContext 伝播の実証) (3) text/view 競合と View null 戻しのフォールバック (4) 新インスタンス差し替え (5) サイズが変わる内容変化 (iOS 行高さ再計算 TODO の実地確認を兼ねる) (6) headerHeight 固定 + view の clip (7) ページ離脱 → 再訪問の復元
  - sample-parity 上の位置づけ: [sample-parity.md](../../../concepts/cross/conventions/sample-parity.md) の例外「デモ対象の公開 API が存在しない platform」の逆方向 (MAUI にしか対応概念がないデモ — ItemsSource デモと同じ枠)。主対象は VisualElement 埋め込み・text/view 併存プロパティ・Handler lifecycle 復元という MAUI 固有の公開 API と意味論であり、native の KsAnyView (factory closure) とは API も目的も別物。**native への追随義務なし** (片側先行の追跡対象に数えない)。将来 native に KsAnyView デモを作る場合も対応画面とはみなさない
  - メニュー配置: iOS の ContentView (「デモ」「検証」の Section 区切り) と同様に、MAUI の一覧ページに **「MAUI 固有」の Section を区切って配置**し、パリティ対象のデモ群と視覚的に区別する (Section 名の最終文言は propose で確定)

## 素材

- [artifacts/research-aiforms-accessory-materialization.md](artifacts/research-aiforms-accessory-materialization.md) — AiForms 原典の HeaderView/FooterView 実体化機構の調査 (実体化経路・計測・BindingContext・更新・寿命)
- [artifacts/research-maui-view-embedding.md](artifacts/research-maui-view-embedding.md) — MAUI 本体の VisualElement→native 埋め込みの公式作法 (ToPlatform 契約・計測部品・CollectionView Header/Footer の模範実装・寿命)

## TODO

- [x] 論点の解消 (2026-08-11 全6論点 + 派生⑦を決定事項へ昇格。ADR 3本起票: maui/ADR-0016〜0018)
- [x] [align-view-accessory-header-height](../../../changes/archive/2026-08-11-align-view-accessory-header-height/exploration.md) (旧 fix-ios-view-header-height-override) を**先行 M 級 change** として実装する (phase-6 実装の前提。2026-08-11 提案一式作成済み — 相方 spec-review の指摘4件を全採用、動的高さ変更は payload 方式で hosted view 維持と裁定。mock 承認済み) — **完了・蒸留済み (2026-08-11)**: core/ADR-0021 accepted。実装レビューで判明した追加契約 1 件 (固定高さ時は hosted view が領域を占有 — iOS の 4 辺 pin と対称、deviation 記録) を含む。phase-6 本体は両 OS 対称の固定高さ semantics を前提にできる
- [ ] (実装フェーズで検証) native が同一 Section の view accessory を `updateAccessory` で差し替えたとき、旧 view を正しく剥がして新 view を貼るか (iOS / Android)
- [ ] (実装フェーズで検証) iOS: wrapper の invalidation 中継が UICollectionView self-sizing の行高さ再計算まで届くか。届かない場合は native 側に再計算の口を足す (native 一般のギャップとしてパリティ整備の範囲)
- [x] ksn-propose で変更提案を起こす (2026-08-11 [add-maui-accessory-views](../../../changes/add-maui-accessory-views/proposal.md) として L 級の提案一式を作成。相方 spec-review の指摘8件を採用7・部分採用1で反映 — second-opinion-spec-001.md 参照)

## 実装結果 (2026-08-12 反映)

[add-maui-accessory-views](../../../changes/archive/2026-08-12-add-maui-accessory-views/proposal.md) として実装完了 (L 級)。verify-001 VALID、maui/ADR-0016〜0018 accepted、concepts 追随済み (maui-facade / native-bridge / store-and-update-streams / sample-parity / 新規 maui/architecture/view-materialization.md)。

- 検証 TODO 2 件の結果: (1) iOS の行高さ再計算は wrapper の invalidation 中継だけでは**届かない**と実測確定 → オーナー裁定 (案A) で native 再計算口 `invalidateAccessoryMeasurement` を追加 (パリティ整備、deviation 1件目)。(2) 旧 view の剥がしは Android は closure detach で閉じる (detach なしは crash を実証)、iOS は wrapper 破棄手順に superview 除去を追加して吸収 (deviation 2件目)
- 実装レビューで確定した追加判断: 論理所有 (logical tree + BindingContext) を BindableProperty 寿命に分離 (deviation 3件目)、未参加 Section の受け皿は既配置 View を奪わない (オーナー裁定、deviation 4件目)。いずれも ADR-0016 へ反映済み
- 申し送りのルーティング:
  - phase-5 (CustomCell) への引き継ぎ → phase-5 agenda の TODO に追記済み (共有機構の再利用前提)
  - 既知挙動 2 件 (多重配置例外後のプロパティ無言破棄 / 設定ツリー未参加の所有者の View は多重配置検査に載らない) → maui-facade.md の多重配置段落で契約委譲として言及済み。それ以上の詳細記録は既存 Section / CellBase 契約と同型のため**見送り** (価値 lint: 再導出可能)
  - docs/ と samples/maui/README.md の追随 (「収録している画面」表が stale) → docs-refresh スキルの対象。ユーザーの明示依頼で実施する運用のため本蒸留では見送り

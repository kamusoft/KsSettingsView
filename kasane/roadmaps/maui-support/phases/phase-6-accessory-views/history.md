# phase-6-accessory-views 議論履歴

## 2026-08-11: ① MauiView → platform view 実体化機構の切り出し方

**経緯**: 初回提示は自リポジトリの先例 (IconSource / maui/ADR-0015) のみを根拠とする案A (三層踏襲)。ユーザー指摘により AiForms 原典と MAUI 本体 (dotnet/maui のローカルクローン) の調査を先行実施 (ksn-scout 2並列、結果は artifacts/research-*.md)。

**選択肢**:
- 案A: IconSource 三層構造の踏襲 (Handler が materializer seam 注入・controller 所有・gateway キャスト輸送)
- 案B: gateway 直変換 (MauiContext を gateway へ渡し gateway が ToPlatform)
- 案C: Handler 変換 (Handler が変換し controller は platform view のみ扱う)

**採用**: 案A′ = 案A の三層配置 + 調査で判明した「中身」の具体化 (自己計測 wrapper platform view を MAUI 公式骨格で生成)。

**理由**:
- AiForms は IMauiContext を FindMauiContext() (親チェーン横取り、原典自身が「壊れうる」と TODO 明記) で入手しており、Handler 注入 seam はこのハックへの構造的回答になる
- MAUI 本体の CollectionView Header/Footer 実装が「生 View は自己計測 wrapper (GeneralWrapperView 相当) で包む」ことをコメントで明記。iOS は MauiView + ICrossPlatformLayout、Android は ItemContentView 同型が公式ルート
- AiForms の実装から「実体化は一発変換で終わらず、更新・再計測・寿命管理が続く」ことが確認され、状態を持てる controller 所有 (案A) が必須に。案B/C は却下
- AiForms は header/footer と CustomCell を共有ゼロのコピー分岐で持つ (反面教師)。共有部を「wrapper + 公式骨格」に切ることで phase-5 再利用に備える

## 2026-08-11: ② Bridge の accessory 輸送拡張

**選択肢**:
- 案a: native view インスタンスを直接渡す新 API (`updateAccessoryView`) + KsBridgeSection への view フィールド追加。Bridge 内部で定数返し closure (`KsAnyView.uiKit { view }` 等) に包む
- 案b: C# デリゲートを @objc block / JVM functional interface として渡す factory 輸送
- 案c: binding 範囲を広げ Kotlin/Swift の KsAnyView を C# から直接見せる

**採用**: 案a (+ Bridge closure 内の detach 対策 + DTO フィールド追加による setRoot 経路の対称化)。

**理由**:
- ADR-0015 で確立した「解決済み platform 値の輸送」(UIImage / Drawable) と完全同型で、@objc 境界を素通しできる。native (Core / UI) は無変更
- 案b は「factory 契約に忠実」に見えるが、VisualElement は Handler 1:1 で都度生成が構造的に不可能なため、結局同一インスタンスを返すことになり interop の複雑さ (デリゲート寿命・GC 管理) だけが残る
- 案c は Swift の associated value enum が @objc 非互換で実質不成立。binding 方針 (Bridge モジュールのみ Bind) の転換にもなる
- 罠と対策: native の KsAnyView factory は「呼ばれるたびに view を作る」契約でリサイクル時に再呼び出しされる。定数返し closure だと2回目に「親付き view」が返り Android の addView は crash する。Bridge closure 内で返す前に既存親から detach する (MAUI 本体の再親付け作法と一貫)。iOS の addSubview は自動 reparent だが同じ detach 作法に揃える

## 2026-08-11: ③ KsAnyView が等価比較へ参加しない契約下での更新セマンティクス

**前提の実証**: Store の updateAccessory (SettingsRootStore.swift:274) に同値スキップのガードは無い。Section 系は unknown ID 検証のみ、Root 系は常に Diff 発行。KsAnyView の case 等価が変化を握りつぶすのは値比較に依存する経路 (replaceSection 由来の差分検出・Android DiffUtil) だけ。

**選択肢**:
- 案A: 差し替え (新インスタンス) = 明示 updateAccessoryView 経路で再発行 / 内容変化 (同一インスタンス) = 再発行せず live 追従
- 案B: 内容変化も再発行 (AiForms 型 descendant 購読 + デバウンス)
- 案C: KsAnyView に世代トークンを足して等価比較へ参加させる

**採用**: 案A + 補強 (ユーザー質問「View 内の変更・サイズ変更は反映されるか」への回答から): サイズ変化の native への伝播 (iOS の行高さ再計算含む) は ADR-0016 wrapper の invalidation 中継の責務と明記。描画内容の変更は live view の直接再描画で確実、Android のサイズ追従は requestLayout → RecyclerView OnMeasure 再走で確実、iOS の UICollectionView self-sizing はセル内制約変更だけでは高さを測り直さないため未確証 — 自動で繋がらない場合の native 側再計算口は実装フェーズで検証・追加 (native の KsAnyView accessory でも同じ問題が起きる native 一般のギャップであり、非ゴール例外のパリティ整備に収まる)。TODO 2件を起票。

**理由**:
- 案B は AiForms が「view を作り直して貼り直す」構造だから意味がある方式で、live view + 自己計測 wrapper の構造では再発行するものが無い。リフレクション descendant 購読 (ADR-0016 で不採用のハック) の復活にもなる
- 案C は Core の KsAnyView 契約変更 = 非ゴール「XAML 都合の native 変更」に抵触、両 OS の等価比較テスト改修も重い

## 2026-08-11: ④ 原典 API との対応 (プロパティ形状と命名)

**採用**: (a) `Section.HeaderView`/`FooterView` (`View?`) の AiForms 互換命名 (b) Root は phase-2 予約名 `RootHeaderView`/`RootFooterView` の実体化 (c) text と view の競合は View 優先 (原典判定の踏襲。View null 戻しで text へフォールバックし ③ の明示経路で再発行) (d) DataTemplate 版は非提供 (phase-10 の領域)。

**却下**: 型の `VisualElement?` 拡大 (MAUI 慣例・原典互換で View に劣る) / 競合の後勝ち方式 (XAML 属性設定順に挙動が依存する罠)。

**ADR**: 起こさない — maui/ADR-0008 (AiForms 互換命名) の適用であり、(c) の優先規則はデルタスペックで固定する。

**派生論点**: ユーザー指摘により ⑦「view accessory の Host 世代管理 (復元)」を新規論点として追加。Root 系は core/ADR-0019 で Store 復元対象外 (text は facade 保持 + OnHostAttached 再適用のイディオム確立済み — release-host-without-bridge-dispose の deviation.md、phase-2 agenda「root H/F 再適用の実装注意」)。view は platform wrapper が MauiContext 世代に縛られる点で text より複雑。

## 2026-08-11: ⑦ view accessory の Host 世代管理 (復元)

**発端**: ④ の確定時にユーザーが「RootHeaderView/RootFooterView は Store に保存されないので MAUI 側で復元手段が必要」と指摘。deviation.md (release-host-without-bridge-dispose) と phase-2 agenda の再読で、text の確立イディオム (所有者保持 + OnHostAttached 再適用、Android attach-order 罠) を確認。

**論点の拡大**: 調査済み事実の重ね合わせで、問題は Root だけでないと判明 — Section 系は Store が復元する closure (ADR-0017 の定数返し) が旧 MauiContext の platform view を包んだまま残り、再接続時の表示復元で旧 Activity Context を抱えた view が返る (Android Context リーク + 破棄済み Handler の view 表示)。「Store が復元するせいで壊れる」側。

**選択肢**:
- 推奨案: 切断時破棄 + 接続時再実体化 (VisualElement が復元の正、wrapper は Host 世代の派生物。切断時に Section 系の stale closure を Store から書き戻し除去、接続時に OnHostAttached で再実体化 + 全 view accessory 再発行)
- 代替1: icon 同様 wrapper を release 後も維持 — View は Context を強参照するため Activity リークで却下 (icon は UIImage/Drawable が Context 非依存だから成立していた)
- 代替2: Section 系は Store 復元に任せ Root だけ再適用 — stale closure 問題が残り却下

**採用**: 推奨案。text の「所有者保持 + attach 後再適用」イディオムの拡張であり、Root と Section の復元経路も facade 側で一本化される。

## 2026-08-11: ⑤ headerHeight (自動 -1 / 固定正値) との相互作用

**訂正**: 当初提示は「view accessory も text と同じ headerHeight 経路に乗る (両 OS)・native 変更ゼロ」としていたが、ユーザーが保留 change fix-ios-view-header-height-override を提示し誤りと判明 — native は OS 非対称 (iOS は view accessory にも固定 headerHeight が効く / Android は view では headerHeight を渡さず常に自動 = 原典準拠)。

**選択肢**:
- 案A: 固定値が view accessory にも勝つ (iOS 挙動を意図的拡張として確定し、Android を iOS へ対称化。原典から両 OS とも離れる)
- 案B: 原典踏襲 (view は常に自動高さ。iOS の公開挙動を変更し Android に合わせる)

**採用**: 案A。優先順位は「正値固定 (clip 許容) > -1 + Theme.headerHeight > -1 自己計測」。理由: (1) 明示指定が無言で無視される罠 (案B) を避ける (2) 案B は既存 iOS 利用者の見た目が変わる公開挙動変更 (3) 対称化は契約対称化のパリティ整備で非ゴール例外 (iOS への replaceCells 追加、maui/ADR-0002 と同じ理屈)。

**巻き取り**: fix-ios-view-header-height-override の未決論点 (オリジナル準拠 vs 意図的拡張の維持) を本決定で裁定 (維持 + Android 対称化に転換)。同 change は phase-6 で巻き取り、concepts (core/styling/list-appearance.md の headerHeight 記述) の明文化と ADR を伴う。

**巻き取り形態の確定 (同日追記)**: 案1「先行 M 級 change として phase-6 実装前に実施」を採用 (phase-2 の release-host-without-bridge-dispose と同じ形態)。1フェーズ=1change を維持し、Android native の公開挙動変更を単独でレビュー・テスト可能にする。phase-6 のデルタスペックは「対称化済み」を前提にできる。agenda TODO へ反映済み。

## 2026-08-11: ⑥ サンプルページ追加

**採用**: AccessoryViewsDemoPage 1ページ (既存の「テーマ別1ページ」パターン踏襲)。内容は決定①〜⑦から逆算した7項目 — Root/Section の view accessory、BindingContext 伝播、text/view 競合とフォールバック、差し替え、サイズが変わる内容変化 (iOS 行高さ再計算 TODO の実地確認兼用)、headerHeight 固定 + clip、離脱→再訪問の復元。

**規約上の位置づけ (ユーザー指摘で追加)**: 当初案はパリティ規約 (cross/ADR-0016・sample-parity.md) への位置づけを欠いていた。ユーザー指摘「MAUI 固有のサンプルページという区別が必要。Native で作るとしても目的が違う」を受けて確認し、sample-parity の例外「デモ対象の公開 API が存在しない platform」の逆方向 (MAUI にしか対応概念がないデモ = ItemsSource デモと同じ枠) に置くと確定。native 追随義務なし・片側先行の追跡対象外。将来 native に KsAnyView デモを作る場合も対応画面とみなさない (API も目的も別物)。

**メニュー配置 (ユーザー提案)**: iOS ContentView が「デモ」「検証」を List の Section で区切っている形に倣い、MAUI 一覧ページに「MAUI 固有」Section を区切って配置。パリティ対象デモ群との視覚的区別をメニュー構造で表現する。Section 名の最終文言は propose で確定。

**却下**: Root 用と Section 用の2ページ分割 (1画面に自然に同居できる題材で、分けると薄い2ページになるだけ) / パリティ対象デモとして native 追随させる案 (項目 3,4,5,7 は native に対応する意味論が無く完全一致が成立しない)。

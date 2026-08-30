# phase-2-maui-core 議論履歴

## 2026-08-06: Host 単独解放の不在 (releaseHost)

- 選択肢: 案A `releaseHost()` を Bridge に追加 (M級) / 案B 「Bridge の寿命 = control の寿命、DisconnectHandler では破棄しない」と契約で固める (S級)
- 採用: **案A**
- 理由 (MAUI ソース (dotnet/maui のローカルクローン) を ksn-scout で裏取り):
  - ページ pop → 再訪問では `DisconnectHandler` が呼ばれ、platform view は必ず新 Handler + `CreatePlatformView` で作り直される (ElementHandler.cs の状態遷移で確認)。「同じ handle を返す」現行契約のままの案B はこの再接続契約に逆行する
  - 既定テンプレートは `ConfigurationChanges` 属性で回転を吸収するため Activity 再生成は既定では起きないが、configChanges 改変・マルチウィンドウでは新 Activity 由来の新 MauiContext が作られるパスが実在する。Host 生成ごとに Context を再取得する案A はこれにも耐える
  - 案A なら切断中は Host 不在となり、再接続時に Store 現在状態から復元されるため、非表示中の更新取りこぼしも構造的に消える
- 決定の詳細: `releaseHost()` は Host のみ解放・Store 維持・冪等。解放後の `makeHost*` は Store 現在状態から復元した新 handle を返す。MAUI Handler は DisconnectHandler → `releaseHost()` / 再接続 → `makeHost*`(新 Context)
- 派生: maui/ADR-0007 を proposed で起票。実装は release-host-without-bridge-dispose を M 級として phase-2 実装前に先行させる。Bridge 本体の最終破棄 (dispose) の呼び時機は論点6 (DisconnectHandler 必須化) で扱う

## 2026-08-06: Host 取り付け順序の契約の隙間

- 選択肢: 案A 契約明文化のみ (「取り付け前の操作は setRoot のみ保証」を文書化、S級) / 案B Host 側で塞ぐ (view load 時に Store 現在状態から復元、M級)
- 採用: **案B**
- 調査 (ksn-scout 2並列、詳細: artifacts/2026-08-06-attach-order-scout-findings.md):
  - iOS: viewDidLoad 前の非 full Diff は内部 root にすら反映されず破棄。viewDidLoad は Store を再 pull しない (init 時キャプチャの root から構築) — Store の pull 型復元保証を Host が使っていない実装ギャップ
  - Android: attach 前は collect 自体を開始せず、onAttachedToWindow の resyncFromStore が Store 現在状態を pull して全復元 — 既に安全 (exploration の「Android 未確認」を解消)
  - MAUI: Handler のマッパー適用は親 view への追加より必ず前に完結 (ElementHandler.SetVirtualView) — 案A だと Handler に Loaded 遅延機構が常設で必要になる
- 理由: iOS を Android の resyncFromStore パターンへ対称化する小変更で「attach/load 時の現在状態復元」保証が両 OS で穴なしに成立し、MAUI Handler が順序を意識せず済む。maui/ADR-0007 の makeHost 復元と同じ復元意味論で一貫。案A は Handler 側へ恒久コストを転嫁し、Android の実態 (既に安全) とも乖離するため却下
- 派生: core/ADR-0019 を proposed で起票。実装は clarify-host-attach-order-contract を M 級として phase-2 実装前に先行させる (TODO 追加)

## 2026-08-06: コレクション購読 → Diff 変換経路 (旧論点2・3・4 の統合)

- 整理: 「IList 公開方式」「バッチ戦略」「cellId→CellBase 対応管理」は一続きの設計のため1論点に統合してから議論した (論点8個以上の分割トリガー発火に対する定性判断は「テーマ独立性なし」で、分割ではなく統合を選択)
- 選択肢 (実質の分岐は内容更新のバッチ戦略): 即時1件ずつ replaceCell (旧 openspec 案) / 遅延フラッシュ + replaceCells / 明示 Begin/EndUpdate API
- 採用: **二層方式** — 構造イベントは即時 1:1 変換 (Reset のみ setRoot)、内容更新は dirty set + Dispatcher による同一 UI サイクル末尾フラッシュ (1件 replaceCell / 複数 replaceCells)
- 理由: CollectionChanged は元々1操作=1イベントで構造側にバッチ問題はない。内容更新はバインディング一斉更新で複数 Cell が同時に変わるのが普通で、連続単発 replace は Android の notifyItemChanged 破棄を踏むため Store の replaceCells バッチ契約に乗せる。明示 Begin/End は囲み忘れがバグになるため却下。フラッシュと構造操作の交錯は Bridge の「未知 ID no-op」契約で安全側に落ちる
- 対応表: SettingsView (facade) が CellBase↔cellId の双方向を一元管理 (Bridge の「返した ID だけが生きている」契約のため)
- ADR: 起こさない — Store の既存バッチ契約からの帰結で MAUI 層内部の可逆な設計。propose の design.md で詳細化し、蒸留時に必要なら昇格を再検討

## 2026-08-07: AiForms 互換 BindableObject 階層の踏襲範囲

- 材料: AiForms 公開面 × 現行 core 契約の突き合わせ棚卸し (artifacts/2026-08-06-aiforms-surface-inventory.md)。Theme/CellStyle/共通 Cell 属性は camelCase 化のみでほぼ 1:1 (A 大半)、B はドラッグソート・Android 専用切替・非同期画像等に集中、通知思想は根本的に異なる (INotify* 集約 vs 値+Diff)
- 採用: 3原則 (A は AiForms 命名踏襲 / B は互換提供しない / C は機構ごと選別) + 個別判断 (Font 分割公開+facade 合成、Tapped 非共通化、ItemsSource/ItemTemplate は phase-2 で器から実装、Root コンテナ形状は AiForms 同形)
- オーナー修正: D&D (UseDragSort 含む)・Scroll 制御・FooterVisible 相当 (IsHeaderVisible/IsFooterVisible として)・DataTemplate 仮想化 (オリジナル由来の大量 MAUI View 生成問題への対処) は「切る」ではなく **Native から作り直す強化課題としてロードマップ最終フェーズへ追加**する方針に変更。ksn-roadmap 改訂でフェーズ追加
- 派生: maui/ADR-0008 (公開 API 踏襲方針) を proposed で起票。Section.Title → HeaderTitle 改名 (Footer と対称でない、オーナー提起) は置き場未定のため新規論点として積んだ

## 2026-08-07: MAUI Section の header/footer テキスト命名

- 経緯: 論点1 の議論でオーナーが AiForms の `Title` / `FooterText` の非対称を指摘し改名 (HeaderTitle 案) を提起。当初「Native 層の名前まで統一し Native を後で修正」の方向だったが、オーナーの「native 層はそもそも title なのか?」の問いで実装を確認した結果、Native は `Section.header` / `Section.footer` (SectionAccessory) で最初から完全対称・「title」という語は存在しないと判明 (ios/Sources/KsSettingsViewCore/Section.swift:25-27)
- 採用: MAUI は `HeaderText` / `FooterText` の対称対で phase-2 初出から公開 (破壊的変更なしのタイミング)。Native 修正は不要。phase-9 の命名統一課題も不要になり除外済み
- 却下: `HeaderTitle` / `FooterText` (接尾辞の非対称が残る)、AiForms 互換名 `Title` のまま公開して後で改名 (公開後の改名は破壊的変更)
- 派生: maui/ADR-0008 (proposed) の Decision に命名例外として追記

## 2026-08-07: DisconnectHandler 必須化とライフサイクル (論点6)

- 論点: ADR-0007 (DisconnectHandler → releaseHost) を前提に、①DisconnectHandler の責務、②Bridge 本体の最終破棄 (dispose) の呼び時機 (ADR-0007 で保留)、③リークテスト基盤
- 採用: DisconnectHandler = releaseHost + delegate/購読解除 (MAUI .NET 8+ の自動 disconnect に乗る。Manual ポリシー利用者の責務を文書化) / Bridge は control と同寿命で明示 dispose なし (GC + ファイナライザ回収) / WeakReference リークテスト基盤を phase-2 で設置
- 却下: Unloaded/Window destroy での dispose (Unloaded はページ再訪でも来るため Store が消え ADR-0007 の狙いと矛盾) / 明示 Dispose API (呼び時機の判断を利用者に転嫁。必要になれば後から非破壊で追加可能)
- 根拠: releaseHost 後の Bridge は Context を保持しない (ADR-0005 で Context は Host 生成引数のみ) ため、生存し続けても残るのは Store データだけで、それは再接続復元のために意図的に残す仕様
- ADR: 起こさない (明示 Dispose の後付けは非破壊で可逆。design.md で詳細化)

## 2026-08-07: エコー抑止 (論点5) の phase-4 への移管

- 判断: 同値チェックの実装は phase-2 で決めず phase-4-basic-input-cells へ移管
- 理由: エコーの発生経路 (delegate 通知 → 書き戻し) が phase-2 に存在しない (ユーザー操作通知は最初の対話型 Cell のフェーズが実装する契約)。同値チェックの正しい設計は「通知時点で native Store が既に新値を持つか」という delegate の意味論に依存し、phase-2 で推測して決めると腐る。LabelCell は表示専用で検証手段もない
- phase-2 の担当: dirty-set フラッシュ基盤に発行抑止フックの口を確保するのみ

## 2026-08-07: AddKsSettingsView() の Handler 登録範囲 (論点7)

- 旧案: SettingsViewHandler + LabelCellHandler を登録し、残り Cell の Handler を各フェーズで追加
- 採用: **SettingsViewHandler 1件のみ**。Cell Handler は Bridge 方式では存在しない — Cell 描画は native Host (Store から描く) の責務で、CellBase は DTO へ変換される純粋データのため MAUI Handler 機構を通らない。旧案は AiForms レンダラー方式の名残と判定
- 帰結: 後続フェーズで増えるのは DTO 変換と Bridge の addXxxCell 系のみ。AddKsSettingsView() のセットアップは将来も不変。phase-5/6 の MauiView 実体化は MAUI 標準の Handler 解決で足りる
- ADR: 起こさない (ADR-0001/0002 からの自明な帰結)

## 2026-08-07: SettingsRootDefinition 不採用の再確認 (論点8)

- 採用: 旧 openspec 案の SettingsRootDefinition (ルート定義の別オブジェクト) は不採用のまま確定
- 理由: コンテナ形状は AiForms 同形 (論点1 決定) で別オブジェクトを挟む場所がなく、SettingsView 自身のルート保持 + setRoot/変換経路 (論点2 決定) で役割が充足済み

## 2026-08-07: RootHeader / RootFooter の MAUI API マッピング (論点10)

- 採用: `RootHeaderText` / `RootFooterText` (string) を phase-2 で公開、既存 `updateAccessory` (root) へ変換。View 版 (`RootHeaderView` / `RootFooterView`) は名前のみ予約して phase-6 へ
- 却下: 素の `HeaderText` / `FooterText` 命名 (Theme 系の `HeaderTextColor` 等 = Section ヘッダスタイルと接頭辞衝突し「HeaderText は Root、HeaderTextColor は Section」の罠になる)
- これで phase-2 の論点は全て解消 (11論点 → 統合1・移管1・決定9)。残 TODO は先行 M 変更2件の propose と phase-2 本体の spec 化

# phase-2-maui-core

`KsSettingsView.Maui` 本体 (SettingsView / Section / CellBase の BindableObject 階層と Handler 基盤) を整備し、LabelCell で Bridge↔Handler 経路を動作証明する。

原案: `openspec/changes/add-maui-core` (凍結・参照のみ)

## 論点


## 決定事項

- **Host 単独解放 (2026-08-06)**: 案A 採用 — Bridge に `releaseHost()` を追加する (Host のみ解放・Store 維持・冪等)。解放後の `makeHost*` は Store 現在状態から表示復元した**新しい** handle を返す (「再呼び出しは同じ handle」は「生きている Host があれば同じ handle」に改訂)。MAUI Handler は DisconnectHandler → `releaseHost()` / 再接続 → `makeHost*`(新 Context) と 1:1 対応させる。根拠: MAUI はページ再訪問時に必ず新 Handler + `CreatePlatformView` で platform view を作り直す (MAUI ソース裏取り済み、history 参照)。実装は [release-host-without-bridge-dispose](../../../../changes/release-host-without-bridge-dispose/exploration.md) を M 級変更として phase-2 実装前に先行させる。ADR: maui/ADR-0007 (proposed)

- **Host 取り付け順序 (2026-08-06)**: 案B 採用 — 契約として「Host は view load (iOS: viewDidLoad) / window attach (Android: onAttachedToWindow) 時に Store 現在状態 (root / theme) を pull して表示を復元する」を両 OS の Host 保証に加える。iOS を Android の `resyncFromStore` パターンへ対称化 (viewDidLoad で接続中 Store を pull)。Android は現行実装が既に契約を満たすため変更なし、両 OS 対称の回帰テストで固定。これにより MAUI Handler は取り付け順序を意識せず実装できる。実装は [clarify-host-attach-order-contract](../../../../changes/archive/2026-08-08-clarify-host-attach-order-contract/exploration.md) を M 級として phase-2 実装前に先行させる。調査根拠: [artifacts/2026-08-06-attach-order-scout-findings.md](artifacts/2026-08-06-attach-order-scout-findings.md)。ADR: core/ADR-0019 (proposed) — (追記 2026-08-08: 相方 spec-review を経て L 級で実施・実装完了・蒸留してアーカイブ済み。core/ADR-0019 は accepted。復元対象は Store 現在状態のもの (構造・Cell 内容・Section accessory・theme) に限定され、Root header / footer は所有者が view load / attach 後に適用する — MAUI facade は `RootHeaderText` / `RootFooterText` の再適用を Handler 取り付け後に行うこと)

- **コレクション購読 → Diff 変換経路 (2026-08-06、旧「IList 公開」「バッチ戦略」「cellId→CellBase 対応管理」の統合論点)**: 二層方式を採用 — ①`Sections` / `Cells` は `IList<T>` で公開し、実体が `INotifyCollectionChanged` なら購読して Diff 適用、そうでなければ初回 `setRoot` のみの静的描画。②構造イベント (`CollectionChanged` の Add/Remove/Move/Replace) は対応する Bridge 構造操作へ**即時 1:1 変換** (`Reset` のみ `setRoot` 再構築)。内容更新 (`CellBase` のプロパティ変更) は即時に Bridge を呼ばず **dirty set に積んで `Dispatcher.Dispatch` で同一 UI サイクル末尾に1回フラッシュ** (1件なら `replaceCell`、複数なら `replaceCells`) — Android の連続単発 replace による notifyItemChanged 破棄を回避し Store のバッチ契約に乗せる。フラッシュ前に対象 Cell が構造操作で消えるケースは Bridge の「未知 ID no-op」契約で安全。③対応表は SettingsView (facade) が「CellBase → 割当済み cellId」+「cellId → CellBase」の双方向を一元管理 (Bridge が返した ID だけが生きている契約のため)。ADR は起こさない (Store の既存バッチ契約からの帰結で MAUI 層内部の可逆な設計。propose の design.md で詳細化し、蒸留時に必要なら昇格)

- **BindableObject 階層の踏襲範囲 (2026-08-07)**: 3原則 — ①現行契約に対応がある公開面 (A 分類) は AiForms 命名を踏襲、②現行仕様に無い機能 (B 分類) は互換 API として提供しない、③MAUI 層固有 (C 分類) は機構ごとに選別。個別判断: Font 系は MAUI 慣例の分割公開 (FontFamily/FontSize/FontAttributes) とし facade で合成 / `Tapped` は CellBase 共通イベントにしない (現行 LabelCell の「操作 control を持たない」契約と衝突。onTap は対応 Cell にのみ各フェーズで載せる) / ItemsSource・ItemTemplate は必須機能として踏襲し phase-2 で器から実装 (SettingsView 直下の Section 生成 + Section 配下の Cell 生成。テンプレ生成物は決定済みの Diff 変換経路に乗せる。非仮想化の AiForms 方式) / コンテナ形状 (SettingsView.Root) も AiForms 同形。**B のうち D&D (UseDragSort 含む)・Scroll 制御・Section の Header/Footer 表示トグル (IsHeaderVisible/IsFooterVisible)・DataTemplate 仮想化 (大量 MAUI View 生成問題対処) は「切る」ではなく Native から作り直す強化課題としてロードマップ最終フェーズへ追加** (ksn-roadmap 改訂で対応)。突き合わせ棚卸し: [artifacts/2026-08-06-aiforms-surface-inventory.md](artifacts/2026-08-06-aiforms-surface-inventory.md)。ADR: maui/ADR-0008 (proposed)

- **MAUI Section の header/footer テキスト命名 (2026-08-07)**: `HeaderText` / `FooterText` の対称対で phase-2 初出から公開する (AiForms の `Title` は踏襲しない)。Native は `Section.header` / `Section.footer` (SectionAccessory) で最初から完全対称で「title」という語が存在しないため (ios/Sources/KsSettingsViewCore/Section.swift:25-27)、Native 側の修正は不要 — 非対称は AiForms 側だけの命名だった。ADR-0008 の Decision に命名例外として追記済み

- **DisconnectHandler とライフサイクル (2026-08-07)**: ①DisconnectHandler では `releaseHost()` + delegate/購読の解除を行う。MAUI (.NET 8+) の自動 `DisconnectHandlers()` ウォークに乗り、「必須化」の中身は「`DisconnectPolicy = Manual` にした利用者は自分で `DisconnectHandlers()` を呼ぶ責務」の文書化とする。②Bridge の明示 dispose API は作らない — Bridge/Store は SettingsView (VirtualView) と同寿命とし、最終回収は GC + binding のファイナライザに任せる (`releaseHost()` 後の Bridge は Context を保持しないため安全 — ADR-0005 の設計による。Store が残るのは再接続復元のための仕様)。後から明示 Dispose を足すのは非破壊で可逆なため ADR は起こさない。③「切断後に Handler / platform view / Host native 実体が回収される」ことを `WeakReference` + GC ループで検証するリークテスト基盤を phase-2 で設置し、後続 Cell フェーズが再利用する

- **エコー抑止の移管 (2026-08-07)**: Native 由来の値変更のエコー抑止 (同値チェック) は **phase-4-basic-input-cells へ移管**。delegate 通知が phase-2 に存在せず (LabelCell は表示専用)、同値チェックの設計は通知の意味論 (通知時点で Store が新値を持つか) に依存するため、通知を実装するフェーズで確定するのが正しい置き場。phase-2 側は dirty-set フラッシュ基盤 (決定済みの二層方式) に**発行抑止フックを差せる口を確保する**ことのみ担う

- **`AddKsSettingsView()` の Handler 登録範囲 (2026-08-07)**: `SettingsViewHandler` **1件のみ**登録する。旧案の LabelCellHandler は作らない — Bridge 方式 (maui/ADR-0001/0002) では Cell を描画するのは native Host であり、MAUI 層の CellBase は Bridge DTO へ変換される純粋なデータで platform view に個別変換されないため、Cell の Handler 自体が存在しない (旧案は AiForms のレンダラー方式の名残)。後続 Cell フェーズで増えるのは DTO 変換と Bridge の `addXxxCell` 系 API だけで、`AddKsSettingsView()` は将来にわたり 1 Handler 登録のまま。phase-5/6 の MauiView 実体化も MAUI 標準の Handler 解決で足り登録不要。ADR は起こさない (ADR-0001/0002 からの自明な帰結)

- **SettingsRootDefinition 不採用の再確認 (2026-08-07)**: 不採用のまま確定。コンテナ形状は AiForms 同形 (`SettingsView.Root` + XAML で Section を直接並べる) と決定済み (論点1) で、別の定義オブジェクトを挟む場所がない。SettingsView (facade) がルートを保持し、初回は `KsBridgeRootBuilder` 経由の `setRoot`、以後は変換経路 (論点2) で差分を流す。native 側の SettingsRoot (値型) は Bridge の内側で MAUI 公開面に出ない

- **RootHeader / RootFooter の MAUI API マッピング (2026-08-07)**: `RootHeaderText` / `RootFooterText` (string) を phase-2 で公開し、既存 Bridge の `updateAccessory` (root ターゲット) へ変換する。素の `HeaderText` 命名は Theme 系プロパティ群 (`HeaderTextColor` 等 = Section ヘッダのスタイル) と接頭辞が衝突して罠になるため `Root` プレフィックス必須 (native の `rootHeader` / `rootFooter` とも一致)。View 版 (`RootHeaderView` / `RootFooterView`) は名前だけ予約し、輸送・実体化と両設定時の優先順位は phase-6-accessory-views の責務

- **root H/F 再適用の実装注意 (2026-08-08、release-host-without-bridge-dispose の蒸留申し送り)**: 決定事項「Host 取り付け順序」の追記どおり、`RootHeaderText` / `RootFooterText` の再適用は Handler 取り付け後に行うこと。機構的な根拠: Android の `makeHostView` 内の `bind(store)` は attach 前だと `findViewTreeLifecycleOwner()` が null で購読を張らず、root 対象の Diff は replay されない — `makeHostView` → `updateAccessory` → `addView` の順では黙って失われる。MAUI の property mapper は `CreatePlatformView` 直後・view tree 追加前に走り得るため、mapper で素直に再適用すると Android だけこの穴を踏む (iOS は Host 生成時に購読を張るため順序非依存)。検証ホストが `host.Post(() => ...)` で取り付け後に操作しているのは既存の対処例。詳細: [deviation.md](../../../../changes/archive/2026-08-08-release-host-without-bridge-dispose/deviation.md)

## TODO

- [x] 論点の解消 (2026-08-07 全論点を決定事項へ昇格。エコー抑止のみ phase-4 へ移管)
- [x] release-host-without-bridge-dispose を M 級として ksn-propose する (phase-2 実装の前提) (2026-08-07 提案一式作成・相方 spec-review 反映済み)
- [x] clarify-host-attach-order-contract を ksn-propose する (phase-2 実装の前提) (2026-08-07 提案一式作成・相方 spec-review の指摘で **L 級へ昇格**し design.md 追加。Root accessory は復元対象外・Theme は Store 正・収束境界定義をオーナー裁定で確定、core/ADR-0019 へ反映済み)
- [x] ksn-propose で変更提案を起こす (2026-08-08 [add-maui-core](../../../../changes/archive/2026-08-08-add-maui-core/proposal.md) として L 級の提案一式を作成。相方 spec-review の指摘12件を反映済み — second-opinion-001.md 参照)

## 実装結果 (2026-08-08 反映)

- change: [add-maui-core](../../../../changes/archive/2026-08-08-add-maui-core/proposal.md) (L 級) — review-001/002 APPROVED・verify-001 VALID・両 OS E2E 全項目 PASS (ユニットテスト 115件)。相方 spec-review 12件 / code-review 採用3件を反映
- spec 沈黙領域の実装判断 7件は [deviation.md](../../../../changes/archive/2026-08-08-add-maui-core/deviation.md) に記録 (Element 基底 / Title null→空文字 / BindingContext は SetInheritedBindingContext 明示配布 / Cells Reset は setRoot / 重複配置例外の後状態 / DataTemplateSelector 非対応 / iOS 親 VC 解決は Page handler 経由)
- E2E で実バグ2件 (iOS 親 VC 解決が Host 自身を返す / Android の PlatformArrange が measure しない) を発見・修正 — ユニットテスト非到達の層。教訓として捕捉済み (lessons/inbox e2e-in-impl-wave-caught-handler-platform-bugs)
- ADR: maui/ADR-0009 (net10.0 TFM + gateway seam) accepted
- 申し送りのルーティング:
  - Theme 系プロパティ / `Section.IsVisible` → [phase-4 agenda](../phase-4-basic-input-cells/agenda.md) の論点へ引き継ぎ済み
  - DataTemplateSelector 非対応 → phase-4 agenda の ItemsSource 論点へ注記済み
  - review Suggestion 未対応6件 (review-001 4件 / review-002 2件) → phase-4 agenda の TODO へ引き継ぎ済み (同じコードを触る際に評価)
  - NU1608 / NU1107 (AndroidX Lifecycle 版競合、利用者アプリにも波及) → **見送り** (NuGet パッケージングは roadmap 非ゴール。パッケージング変更の着手時に本行を参照して扱う — オーナー合意 2026-08-08)
  - iOS containment の UIKit 実呼び出しは自動テスト到達不能 → **見送り** (恒常制約。maui/tests/KsSettingsView.MauiHost の E2E が担保 — maui/ADR-0009 Consequences に記録済み)

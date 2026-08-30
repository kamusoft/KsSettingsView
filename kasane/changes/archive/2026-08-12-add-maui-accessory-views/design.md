# Design: add-maui-accessory-views

## Context

MAUI facade の header / footer は text 限定 (`RootHeaderText` 等)、Bridge の accessory 輸送も text と clear に限定されている。native は任意 View accessory (`KsAnyView` factory closure) を完全にサポート済み。MauiView (`VisualElement`) を native の UIView / Android View に実体化する機構は facade に前例がなく (`ToPlatform()` 呼び出しゼロ)、本 change で新設する。この機構は CustomCell 対応 (phase-5) が再利用する。

設計判断は phase 議論 (kasane/roadmaps/maui-support/phases/phase-6-accessory-views/) で確定済み。本書はそれを Decision 形式で固定する。根拠調査は同 phase の artifacts/ (AiForms 原典・MAUI 本体の2本)。

前提: 先行 change align-view-accessory-header-height により、headerHeight の view accessory への適用は OS 対称化済み。

## Goals / Non-Goals

**Goals**: proposal.md の What Changes を参照。
**Non-Goals**: proposal.md の Non-Goals を参照。

## Decisions

### Decision 1: MauiView の実体化は三層配置 + 自己計測 wrapper (maui/ADR-0016)

**採用案:** IconSource (maui/ADR-0015) と同型の三層 — `SettingsViewHandler` が per-TFM の materializer seam (`IKsViewMaterializer` 相当、`IKsImageResolver` と並ぶ) を注入し、`KsSettingsController` が実体化タイミングと寿命を所有し、gateway が `object?` → platform 型キャストで輸送する。seam の産物は **自己計測 wrapper platform view**: iOS は `MauiView` + `ICrossPlatformLayout` の自前サブクラス (本体 `GeneralWrapperView` 相当・internal のため自作)、Android は本体 `ItemContentView` 同型の自作 ViewGroup。wrapper が計測 (`IView.Measure` / `Arrange`)・`MeasureInvalidated` の native 中継・破棄 (`DisconnectHandlers`) を自蔵する。生成手順は MAUI 公式骨格: `PropagatePropertyChanged` → BindingContext を Handler 生成より先に設定 → `ToHandler` → detach → attach → 最後に `AddLogicalChild`。

BindingContext の継承元は既存の facade 意味論 (SettingsView → Section → Cell) に合わせる: Root accessory は SettingsView、Section accessory は**所有 Section** から継承する (ItemsSource 生成 Section では item が継承される)。View の明示的な BindingContext は上書きしない (MAUI 標準の継承規則。原典も Section accessory へ Section の BindingContext を配っている)。

**理由:** 「controller は IMauiContext を知らない」既存の層規律を維持でき、fake seam 注入で net10.0 ユニットテスト (maui/ADR-0009) が成立する。wrapper と生成骨格は MAUI 本体の CollectionView Header/Footer 実装と同形 (公式作法)。

**代替案:**
- **A: gateway 直変換 (MauiContext を gateway へ)** — 実体化後も続く再実体化・再計測・寿命管理の状態機械が変換層に生えて層違反。fake gateway テストから変換責務が消える。却下
- **B: Handler 変換 (Handler が変換し controller は platform view のみ)** — Handler は接続時にしか関与せず、接続後の View 差し替え・内容変化を扱う場所がない。却下
- **C: AiForms 方式 (`FindMauiContext()` 親チェーン横取り + `Parent` 直代入 + リフレクション descendant 購読)** — 動作実績はあるが MAUI 内部依存のハックで、原典自身が破損リスクを TODO 明記。却下

### Decision 2: Bridge は native view インスタンスを直接輸送 (maui/ADR-0017)

**採用案:** 新 API `updateAccessoryView(target, sectionID, view)` (iOS `UIView?` / Android `View?`、null でクリア) と `KsBridgeSection` への `headerView` / `footerView` フィールド追加。Bridge 内部で定数返し closure (`KsAnyView.uiKit { view }` / `KsAnyView.AndroidView { _ in view }`) に包んで既存 Store 経路へ乗せる。closure は**返す前に view を既存親から detach** する (リサイクル再バインド時の Android `addView` crash 対策)。native (Core / UI) は無変更。

**理由:** ADR-0015 の platform 画像輸送 (UIImage / Drawable) と同型で `@objc` 境界を素通しできる。detach → attach は MAUI 本体の再親付け作法と一貫。

**代替案:**
- **A: factory 輸送 (C# デリゲートを block / functional interface で渡す)** — `VisualElement` は Handler 1:1 で都度生成が構造的に不可能なため結局同一インスタンスを返すことになり、interop を越えるデリゲートの寿命・GC 管理だけが残る。却下
- **B: binding 範囲拡大 (`KsAnyView` を C# へ直接公開)** — Swift の associated value enum は `@objc` 非互換で実質不成立。binding 方針 (Bridge のみ Bind) の転換にもなる。却下

### Decision 3: 更新セマンティクスは「差し替え = 明示経路 / 内容変化 = live 追従」(maui/ADR-0018)

**採用案:** View プロパティの差し替え (新インスタンス) は facade が再実体化し、必ず明示経路 (`updateAccessoryView` → Store `updateAccessory`) で再発行する (Store に同値スキップが無いことを実証済み — 明示経路は `KsAnyView` の case 等価に握りつぶされない)。値比較に依存する経路 (`replaceSection` 由来の差分検出) を view の変更輸送に使わない。同一インスタンスの内部内容変化は何も再発行しない — live platform view が直接描画更新し、サイズ変化は wrapper の invalidation 中継が native の再レイアウトへ届ける。

**理由:** 「view accessory は参照が正、内容は live」。live view + 自己計測 wrapper の構造では内容変化に再発行するものが存在しない。

**代替案:**
- **A: 内容変化も再発行 (AiForms 型 descendant 購読 + デバウンス)** — AiForms は view を作り直して貼り直す構造だから意味がある方式。live view では無意味で、リフレクション購読 (Decision 1 で不採用のハック) の復活になる。却下
- **B: `KsAnyView` に世代トークンを足して等価比較へ参加させる** — 両 OS Core の契約変更で「XAML 都合の native 変更はしない」に抵触。却下

### Decision 4: 公開 API は AiForms 互換命名 + View 優先の競合解決

**採用案:** `Section.HeaderView` / `FooterView`・`RootHeaderView` / `RootFooterView` (いずれも `View?`)。text と view の両設定時は View 優先 (原典の判定を踏襲)。View 非 null の間 text は輸送されず facade が保持、View を null に戻すと text へフォールバックして明示経路で再発行。`DataTemplate` 版は提供しない。

**理由:** maui/ADR-0008 (AiForms 互換命名) の適用。Root 側は phase-2 決定の予約名の実体化で選択の余地なし。native の accessory は text XOR view の sum type のため facade 側での優先解決が必須。

**代替案:**
- **A: 型を `VisualElement?` に広げる** — MAUI 慣例 (CollectionView.Header 等) と原典互換で `View?` に劣る。却下
- **B: 競合は後勝ち (最後に設定した方)** — XAML の属性設定順に挙動が依存する罠。却下

### Decision 5: wrapper の寿命は Host 世代一致 (maui/ADR-0016 追記)

**採用案:** 復元の正は VisualElement (facade が所有)。Handler 切断時に全 view accessory の wrapper を破棄し、Section 系は Store 内の stale closure を `updateAccessory` の書き戻し (text があれば text、なければ nil) で除去する。再接続時は Host 取り付け後 (`OnHostAttached` — Android の attach-order 罠のため mapper では行わない) に新 MauiContext で再実体化し、Root + Section の全 view accessory を再発行する (既存 `ApplyRootAccessory` の拡張)。

Host 切断以外の通常操作でも旧 wrapper の退役を定義する — **accessory slot ごとの所有状態機械**を controller に置き、次の遷移すべてで「Store 更新 (新状態の発行) → native への配信 → 旧 wrapper の破棄 (`RemoveLogicalChild` + `DisconnectHandlers`)」の順序を守る: (a) View の別インスタンスへの差し替え (b) View の null 化 (c) accessory を持つ Section の削除・置換 (d) Root 全再構築で Section が外れる場合。旧 wrapper の破棄を配信後まで遅延させるのは icon の retired lease (`_retiredIcons` → Flush 後破棄) と同じパターンで、native がまだ旧 wrapper を子 view として保持している間に破棄する窓を作らないため。null 解除後の View 再利用 (別 slot への設定) は新規実体化として扱う。

**理由:** View は Context (Android では Activity) を強参照するため、icon lease (release 後も維持) と逆の判断が必須。text accessory の確立イディオム (所有者保持 + attach 後再適用) の拡張であり、Root と Section の復元経路が一本化される。退役順序は icon の実証済みパターンの踏襲。

**代替案:**
- **A: icon 同様に wrapper を release 後も維持** — Activity リーク + 破棄済み Handler の view 表示。却下
- **B: Section 系は Store 復元に任せ Root だけ再適用** — Store が復元する stale closure (旧 Context の view) がそのまま表示される。却下

### Decision 6: サンプルは「MAUI のみの画面」として追加

**採用案:** `AccessoryViewsDemoPage` 1ページ。sample-parity (cross/ADR-0016・sample-parity.md) の例外「デモ対象の公開 API が存在しない platform」の逆方向 (MAUI にしか対応概念がないデモ — ItemsSource デモと同じ枠) に置き、native 追随義務なし。一覧ページは iOS の「デモ」「検証」区分と同様に Section を分け、「MAUI 固有」区分に配置する。

**理由:** 主対象は VisualElement 埋め込み・text/view 併存プロパティ・Handler lifecycle 復元という MAUI 固有の公開 API と意味論で、native の `KsAnyView` とは API も目的も別物。パリティ判定を汚さない。

**注記 (規約との関係):** sample-parity.md の例外文言は「デモ対象の公開 API がその platform に存在しない場合」であり、7項目のうち表示・高さ挙動は native にも対応概念がある。本 Decision は「デモの主対象が facade 固有の意味論である場合はページ全体を platform 固有画面としてよい」という例外の適用拡張にあたる (オーナー裁定 2026-08-11、phase-6 決定⑥)。蒸留時に sample-parity.md へこの扱いを明文化する (申し送り)。

**代替案:**
- **A: パリティ対象のデモとして native にも追随させる** — 競合解決・差し替え・復元は native に対応する意味論が無く完全一致が成立しない。却下
- **B: 技術検証枠 (「検証」区分) に置く** — 本ページはライブラリ公開 API のデモであり、「ライブラリを経由しない技術検証」の枠と目的が合わない。却下

## Risks / Trade-offs

- **iOS の行高さ再計算 (未確証)**: UICollectionView self-sizing はセル内制約変更だけでは高さを測り直さない可能性がある。wrapper の invalidation 中継が届かない場合、native 側に再計算の口を足す (native の `KsAnyView` accessory でも同じ問題が起きる一般ギャップ = パリティ整備の範囲)。実装フェーズ冒頭の検証タスクで要否を確定する
- **view 差し替え時の旧 view 剥がし (未確証)**: native が `updateAccessory` の view 差し替えで旧 view を正しく剥がすか。問題があれば Decision 2 の detach 対策の範囲で吸収する想定
- wrapper (本家 internal 実装相当) を両 OS で自作・保守するコスト。MAUI 本体の計測契約変更に追随する必要がある

## Migration Plan

追加のみで破壊的変更なし。既存利用者への影響なし。先行 change align-view-accessory-header-height の完了が前提。

## Open Questions

- **iOS の高さ再計算経路** (tasks 1.1 で確定): wrapper の invalidation 中継だけで UICollectionView self-sizing の行高さ再計算まで届くか。届く → native 無変更を確定。届かない → native 側の再計算口を本 change のスコープに含める (proposal Non-Goals の例外条項)。どちらに確定したかは deviation.md に記録する

## ADR 候補

- maui/ADR-0016〜0018 を phase 議論時に**起票済み (proposed)**。本 change の蒸留時に実装結果と突き合わせて accepted 化を確認する。新規の ADR 候補はなし (Decision 4・6 は既存 ADR (maui/ADR-0008・cross/ADR-0016) の適用であり、詳細はデルタスペックとサンプル自体が固定する)

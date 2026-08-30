---
id: 0016
title: MauiView の native 実体化は自己計測 wrapper を産物とする三層構造で行う
status: accepted
date: 2026-08-12
---

## Context

Root / Section の Header・Footer (および後続の CustomCell content) に任意の MauiView (`VisualElement`) を設定するには、C# の VisualElement を native の platform view (UIView / Android View) へ実体化する機構が要る。facade 内に前例はなく (`ToPlatform()` 呼び出しはゼロ)、既存の platform 値実体化は IconSource の image source service 解決 ([ADR-0015](0015-iconsource-materialized-via-image-source-service.md)) のみ。

裏取り調査 (原典 AiForms.Maui.SettingsView と MAUI 本体、詳細は出典の調査2本) で次が判明した:

- 原典は IMauiContext を親チェーン横取りの `FindMauiContext()` で入手し、`Parent = Application.Current.MainPage` 直代入・リフレクションによる descendant 購読を併用する。いずれも MAUI 内部依存で原典自身が「壊れうる」と TODO 明記している
- 原典の実体化は一発変換で終わらず、プロパティ差し替え時の再実体化・内容変化時の再計測 (`MeasureInvalidated`)・Handler の破棄管理が実体化後も続く
- MAUI 本体は「native コンテナへ生の View を埋め込むときは自己計測 wrapper で包む」ことを CollectionView の Header/Footer 実装でコメント明記しており、生成手順 (`PropagatePropertyChanged` → BindingContext → `ToHandler` → detach → attach → `AddLogicalChild`) と破棄手順 (購読解除 + `RemoveLogicalChild` + `DisconnectHandlers`) が公式作法として一貫している

## Decision

- MauiView → platform view の実体化は ADR-0015 と同型の**三層構造**で行う: `SettingsViewHandler` が per-TFM の materializer seam を注入し、`KsSettingsController` が実体化のタイミングと寿命を所有し、gateway が `object?` → platform 型キャストで native へ輸送する。controller は `IMauiContext` を知らない (seam 内に閉じ込める)。
- seam の産物は bare な platform view ではなく**自己計測 wrapper platform view** とする — iOS は `MauiView` + `ICrossPlatformLayout` の自前サブクラス (本体 `GeneralWrapperView` 相当。本家は internal のため自作)、Android は 本体 `ItemContentView` 同型の自作 ViewGroup (`OnMeasure` → `IView.Measure`)。wrapper が計測・arrange・`MeasureInvalidated` の native への中継・破棄 (`DisconnectHandlers`) を自蔵する。
- **論理所有 (logical tree 接続 + 継承 BindingContext) と platform lease (wrapper + Handler) の寿命は分離する** — 論理所有は View プロパティの寿命 (配置時に確定し、解除・差し替え・所有 Section 削除で解放)、platform lease は Host 世代の寿命。これにより Handler 未接続中 (XAML 構築時・Host 解放中) も「論理ツリー接続 + BindingContext 継承・変更伝播」の無条件契約が保たれる。順序は MAUI 公式骨格を層をまたいで維持する: 継承プロパティ配布 → BindingContext 確定 → (実体化時に) `ToHandler` / `ToPlatform` → 旧親から detach → native へ attach — Handler は必ず BindingContext の定まった View に対して作られる。
- 設定ツリーに未参加の所有者 (XAML 構築中の Section 等) の受け皿経路は多重配置の検査を通れないため、**他所に所有されている View を引き取らない** — 黙って奪取する代わりに、所有者が変換経路へ参加した時点で多重配置例外になり、既存配置は無傷で残る。
- 旧 wrapper の退役は全経路 (差し替え / null 化 / Section 削除・置換 / Root 再構築 / Host 切断) で「Store 更新 → native への配信 → 旧 wrapper 破棄」の順序を守る (native が旧 wrapper を子 view として保持している間に破棄する窓を作らない)。破棄手順は購読解除 → platform view を superview から外す (iOS は退役した supplementary セルが旧 view を掴んだまま自動 detach しないことを実測確認) → `DisconnectHandlers`。同一 View を再実体化する際は Handler が VisualElement と 1:1 のため、**包み直しの前に**その View を掴む退役待ち実体を破棄する (後から破棄すると新 wrapper の使用中 Handler まで切断される)。
- 原典のハック (`FindMauiContext()` / `Parent` 直代入 / リフレクション descendant 購読) は採らない。
- この機構 (wrapper + 公式骨格) は Header/Footer と CustomCell content の**共有部**として切り出し、用途固有のポリシー (計測キャッシュ・幅補正等) は共有部の上に載せる。原典が両用途をコピー分岐で持ち保守負債化している構造を反面教師とする。
- **wrapper の寿命は Host 世代に一致させる**。復元の正は VisualElement (facade が所有) であり、wrapper は Host 世代ごとの派生物とする:
  - Handler 切断時: 全 view accessory の wrapper を上記破棄手順で破棄する。Section 系は Store 内に残る stale closure ([ADR-0017](0017-accessory-view-instance-transport.md) の定数返し) を `updateAccessory` の書き戻し (text があれば text、なければ nil) で除去する — Host 不在中の `updateAccessory` は Store のみ更新される既存契約により安全。Root 系は Store に無いため書き戻し不要
  - Handler 再接続時: Host 取り付け**後** (Android は attach 前の root 対象更新が黙って失われるため mapper では行わない — native-bridge.md の lifecycle 契約) に、新 MauiContext で再実体化し、Root + Section の全 view accessory を明示経路で再発行する
  - icon lease ([ADR-0015](0015-iconsource-materialized-via-image-source-service.md) の「release で破棄しない」) とは**逆の判断**である — UIImage / Drawable は Context 非依存で Store 復元がそのまま有効だが、View は Context (Android では Activity) を強参照するため、維持すると Context リークと破棄済み Handler の view 表示を起こす

## Alternatives Considered

- **gateway 直変換 (MauiContext を gateway へ渡し gateway が ToPlatform)**: 実体化後も続く再実体化・再計測・寿命管理の状態機械が変換層の gateway に生えて層違反になる。fake gateway 注入の net10.0 テスト ([ADR-0009](0009-net10-tfm-and-gateway-seam.md)) からも変換責務ごと消える。却下。
- **Handler 変換 (Handler が変換し controller は platform view のみ扱う)**: Handler は接続時にしか関与せず、接続後の View プロパティ差し替え・内容変化を扱う場所がない。却下。
- **原典方式の踏襲 (FindMauiContext + Parent 直代入)**: 動作実績はあるが MAUI 内部依存のハックで、原典自身が破損リスクを TODO 明記している。Handler 注入 seam で構造的に不要にできるため採らない。

## Consequences

- 正: MauiContext の閉じ込め・controller 所有・gateway 輸送という既存の層規律 (ADR-0015 で確立) をそのまま維持できる。
- 正: 計測と invalidation が MAUI 公式経路 (`IView.Measure` / `MeasureInvalidated`) に乗り、原典が iOS 専用の débounce 再計測で補っていた内容変化追従を両 OS で同型に扱える。
- 正: 共有部が seam 契約 (「VisualElement → 自己計測 wrapper」) として明文化され、CustomCell 対応が同じ機構を再利用できる。
- 負: wrapper platform view を両 OS で自作・保守する (本家の internal 実装に相当するコードを持つ)。MAUI 本体の計測契約変更に追随する必要がある。
- 負: 単一 VisualElement インスタンスは複数箇所へ置けない (Handler 1:1 + platform 親一意) ため、facade の既存制約 (同一インスタンス多重配置の `InvalidOperationException`) を accessory View にも適用する必要がある。
- 正: Host 世代一致の寿命により、Handler 切断・再接続 (ページ離脱・再訪問) をまたいでも Android の Context リークが生じず、再接続後は新 Context で作られた wrapper が表示される。text accessory の確立イディオム (所有者保持 + attach 後再適用) の拡張であり、Root と Section の復元経路が facade 側で一本化される。
- 負: iOS の accessory 自動高さは Auto Layout 経由でのみ決まるため、wrapper は `IntrinsicContentSize` の override と `MeasureInvalidated` → `InvalidateIntrinsicContentSize()` の中継が必須 (`SizeThatFits` の override だけでは header が潰れる — 実測。出典: 実装結果)。
- 負: net10.0 テストの fake materializer は Handler 1:1 の共有 (接続中は再利用・切断済みなら作り直し) を模擬する必要がある — 模擬しない fake は退役順序の誤りを検出できない (出典: 実装結果)。

出典: phase 議論 2026-08-11 (kasane/roadmaps/maui-support/phases/phase-6-accessory-views/history.md) / 調査: 同 artifacts/research-aiforms-accessory-materialization.md・artifacts/research-maui-view-embedding.md / 実装結果 2026-08-12: kasane/changes/archive/2026-08-12-add-maui-accessory-views/ の deviation.md (論理所有分離・iOS superview 剥がし・受け皿ガードの裁定) と review-002〜004.md (退役順序・Handler 1:1 の実測)

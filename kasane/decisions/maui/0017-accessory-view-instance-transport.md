---
id: 0017
title: accessory の任意 View は native view インスタンスを直接輸送し Bridge が定数返し closure で包む
status: accepted
date: 2026-08-11
---

## Context

Root / Section の Header・Footer に任意 MauiView を設定するには、[ADR-0016](0016-mauiview-materialization-self-measuring-wrapper.md) で実体化した wrapper platform view を Bridge の interop 境界 (`@objc` / JVM) 越しに native の accessory へ届ける必要がある。現行の Bridge accessory 契約は text と clear (null) に限定で、`KsBridgeSection` DTO も `headerText` / `footerText` の string のみ。

native 側の受け口 `KsAnyView` は factory closure (`.uiKit(() -> UIView)` / `.AndroidView((Context) -> View)`) を保持する型消去ラッパで、「呼ばれるたびに view を作る」契約 — リサイクル時 (ViewHolder 再バインド等) に再呼び出しされる。一方 MAUI の `VisualElement` は Handler 1:1 制約により platform view を都度生成できず、単一の wrapper インスタンスしか持てない。

## Decision

- Bridge に新 API `updateAccessoryView(target, sectionID, view)` を追加する (iOS は `UIView?`、Android は `android.view.View?`。null でクリア)。既存 `updateAccessory` (text) と対をなし、Store の accessory 更新経路にそのまま乗せる。
- Bridge 内部で受け取った view インスタンスを**定数返し closure** (`KsAnyView.uiKit { view }` / `KsAnyView.AndroidView { _ in view }`) に包んで Store へ渡す。native (Core / UI) は無変更。
- `KsBridgeSection` DTO に `headerView` / `footerView` (platform view 型) を追加し、`setRoot` / `replaceSection` の初期構築経路を text と対称にする ([ADR-0015](0015-iconsource-materialized-via-image-source-service.md) の platform 画像輸送と同型)。
- **detach 対策**: 定数返し closure は factory 契約の「都度生成」前提を破るため、リサイクル再呼び出し時に「既に親がいる view」が返り、Android の `addView` は crash する (iOS の `addSubview` は自動 reparent)。Bridge の closure 内で**返す前に既存親から detach** する ((`parent as? ViewGroup)?.removeView(view)` 等。iOS も同じ作法に揃える)。MAUI 本体の再親付け作法 (必ず detach → attach) と一貫させる。実装フェーズの検証で、Android は detach なしの再バインドが実際に `IllegalStateException` になることを実測し、必須性を確認済み。

## Alternatives Considered

- **factory 輸送 (C# デリゲートを @objc block / JVM functional interface で渡す)**: factory 契約には忠実だが、`VisualElement` は Handler 1:1 で都度生成が構造的に不可能なため、結局同一インスタンスを返すことになる。interop を越えるデリゲートの寿命・GC 管理の複雑さだけが残る。却下。
- **binding 範囲の拡大 (Kotlin / Swift の `KsAnyView` を C# から直接見せる)**: Swift の associated value enum は `@objc` 非互換で実質不成立。binding 方針 (Bridge モジュールのみ Bind) の転換にもなる。却下。

## Consequences

- 正: interop 境界は UIView / View の素通しで、ADR-0015 (UIImage / Drawable) の前例と同型。native 契約 (KsAnyView / SectionAccessory / RootAccessory) に触れない。
- 正: CustomCell content の view 輸送も同じ経路・同じ detach 作法を再利用できる。
- 負: Bridge の closure が「都度生成」でなく「同一インスタンスの再親付け」を返す点は KsAnyView の本来の契約から外れる。detach 対策が Bridge 側の責務として増え、native 側の factory 呼び出しタイミングの変更に影響を受けうる。
- 負: Bridge 公開 API が Store 公開操作と完全 1:1 (maui/ADR-0002) から一歩広がる (`updateAccessoryView` は Store 側では `updateAccessory` と同じ操作に合流する)。
- 負: closure detach は「次に factory が呼ばれるとき」しか働かないため、差し替えで退役した旧 view の剥がしには効かない — iOS では旧 supplementary セルが旧 view を superview に掴んだまま残ることを実測確認。旧 view の取り外しは [ADR-0016](0016-mauiview-materialization-self-measuring-wrapper.md) の wrapper 破棄手順 (native 配信後の superview 除去) が引き受ける (出典: 実装結果)。

出典: phase 議論 2026-08-11 (kasane/roadmaps/maui-support/phases/phase-6-accessory-views/history.md) / 調査: 同 artifacts/research-aiforms-accessory-materialization.md・artifacts/research-maui-view-embedding.md

# Design: add-maui-native-bridge

## Context

maui-support ロードマップ phase-1。主要な設計判断は ksn-agenda での議論を経て maui/ADR-0001〜0004 (accepted) として確定済み。本 design は各 Decision の要点と、ADR に含まれない実装構成の決定を記録する。議論の経緯は `kasane/roadmaps/maui-support/phases/phase-1-native-bridge/history.md` と `second-opinion-001.md` (突き合わせ結果) を参照。

## Goals / Non-Goals

proposal.md の Why / Non-Goals を正とする。要点: LabelCell 1種で C# → Bridge → Store → Native Host の経路を縦に疎通させる。

## Decisions

### Decision 1: Bridge は内部所有 Store を持つ DSL 方式の類型
**採用案:** Bridge は Native 側に内部所有 `SettingsRootStore` を持ち、公開 API を Store 公開操作へ変換する。Native Host は Store の通知を購読する (既存経路と同一)。
**理由:** `SettingsRootStore → Native Host` の収束保証 (declarative-ui-bridge.md) を維持し、replaceCells バッチ・Theme 同値スキップ・状態復元を Store から得る。
**代替案:**
- **A: 直接 `controller.applyDiff` (旧 openspec 案)** — 収束保証を迂回する第三経路になり、Store 保証の再実装が必要なため却下
- **B: Store handle の C# 公開 (Store 方式)** — API 表面積が最大。将来拡張として保留

→ [maui/ADR-0001](../../decisions/maui/0001-maui-bridge-dsl-variant-internal-store.md)

### Decision 2: Bridge 公開 API は Store 操作 1:1 の12メソッド、replaceCells は iOS Store へ追加
**採用案:** `setRoot` / `insertSection` / `removeSection` / `moveSection` / `replaceSection` / `insertCell` / `removeCell` / `moveCell` / `replaceCell` / `updateAccessory` / `replaceCells` / `setTheme` の12本。`replaceCells` は iOS `SettingsRootStore` 本体へ公開操作として追加し Android と対称化する (additive・非破壊)。観察可能挙動は Android 実装 (`SettingsRootStore.kt` の replaceCells) を契約基準とする: 空リスト no-op・未知 ID 無視・適用0件は無配信・状態1回更新+1バッチ配信。未知 ID の no-op 契約は **Cell / Section 操作に限る** — 現行 `updateAccessory` は対象 Section 不在でも Diff を発行する (iOS `SettingsRootStore.swift:223-237`) ため、Bridge は現行 Store 挙動へ素通しする。
**理由:** `@objc` は Swift enum の associated value を表現できず union DTO の interop 表現が不格好。1:1 なら変換層が最薄で Decision 1 と整合。iOS 側のループ実装は誤魔化しであり、対称化は SwiftUI 利用者にも恩恵。
**代替案:**
- **A: union DTO (`KsSettingsRootDiffDTO`) + applyDiff 1本 (旧案)** — interop 境界の union 表現と decode 層が必要なため却下
- **B: replaceCells を Bridge 内 iOS ループで実現** — 誤魔化しに過ぎずスマートに Native へ追加できる (オーナー判断) ため却下

→ [maui/ADR-0002](../../decisions/maui/0002-bridge-api-per-store-operation.md)

### Decision 3: ユーザー操作通知は単一 delegate/listener (実装は最初の対話型 Cell フェーズへ)
**採用案:** 通知方式は単一 `KsCellInteractionDelegate` / `Listener` 集約 (ADR-0003)。ただし LabelCell は表示専用で対話メソッドが0本になるため、**delegate の登録 API 実装は最初の対話型 Cell を追加するフェーズ (phase-4) に送る**。phase-1 の Bridge 表面は「12メソッド + Builder + Host 生成 API」のみ。
**理由:** メソッド0本の空 interface を先行定義するのは、疎通範囲の決定 (先行インターフェース定義の廃止) と矛盾する。
**代替案:**
- **A: 空の delegate 登録 API を phase-1 で用意** — 検証不能な推測定義になるため却下

→ [maui/ADR-0003](../../decisions/maui/0003-single-interaction-delegate.md)

### Decision 4: Theme の interop は非公開輸送表現、MAUI 慣例型の公開は上位層の責務
**採用案:** Bridge の `setTheme` はプリミティブ (ARGB int・フォント記述子等) の Theme DTO を受け、Store `applyTheme` へ変換する。**phase-1 の Binding csproj は輸送層 assembly であり、その API を利用者向け公開契約として文書化しない。** MAUI 慣例型 (`Microsoft.Maui.Graphics.Color` 等) での公開 facade と Theme 項目対応表 (色・font・寸法・nullable・platform 固有値・同値判定) は phase-2 の spec の責務。
**理由:** Theme を Diff に混ぜない・同値スキップは Store 保証で満たされる。binding 層の DTO は非公開輸送表現であり styling 契約の「中間型禁止」の対象外 (ADR-0004 の役割分担)。
**代替案:** ADR-0004 の Alternatives を参照 (共通論理型の公開 / 条件コンパイル、いずれも却下)

→ [maui/ADR-0004](../../decisions/maui/0004-maui-idiomatic-types-for-styling.md)

### Decision 5: モジュール配置
**採用案:** Native Bridge は各ビルドルート内の独立モジュールとする — iOS は `ios/` 配下の Bridge 用ライブラリ (binding 用 Xcode project を含む)、Android は `android/` 配下の新規 Gradle module。Binding csproj (iOS 用 / Android 用) は `maui/` 配下に置き、既存 `KsSettingsView.slnx` に追加する。
**理由:** 既存公開 API (`KsSettingsViewUI` / `ks-settingsview-ui`) を `@objc` / interop 都合で汚染しない (旧案 Decision 1 と同方針)。
**代替案:**
- **A: 既存 UI モジュールに `@objc` / `@JvmStatic` API を直接追加** — interop 都合の型が既存公開 API に混入するため却下

### Decision 6: net10.0 toolchain の spike を先頭タスクにする
**採用案:** binding テンプレート (XcodeProject / AndroidGradleProject) の net10.0 最小スケルトンをビルドする spike を実装の先頭に置く。**成功ゲート**: (1) Native artifact (xcframework / aar) の生成 (2) binding assembly の生成 (3) それを参照する C# コードの compile / link (4) シミュレータ / エミュレータでの最小アプリ起動。いずれかが失敗したら後続タスクへ進まず change を blocked とし、phase agenda に差し戻す。
**理由:** SDK・Xcode・gradle の組み合わせ問題は動かさないと分からない。議論で結論が出ない性質。
**代替案:**
- **A: ドキュメント調査で事前確認** — 実ビルドより信頼性が低く二度手間のため却下

### Decision 7: Native Host は Bridge が生成・公開し、lifecycle も Bridge が担う
**採用案:** Bridge は内部 Store に接続済みの Native Host handle — iOS は `KsSettingsViewController`、Android は `KsSettingsView` (bind 済み) — を生成して公開する API を持つ。呼び出し側 (phase-2 の MAUI Handler・検証ホスト) はこの handle を view 階層へ取り付ける。lifecycle 契約: Bridge は同時に1つの Host をサポートし、再生成は破棄後に行う。Android の `Context` は Host 生成 API の引数で受け取り Bridge は保持しない。Bridge の破棄 API は冪等で、破棄後の操作 API は no-op、破棄後に Host は更新されない。`setRoot` は Host 生成の前後どちらで呼んでもよい (Store の購読開始前状態復元の保証による)。
**理由:** Store を Bridge 内部に隠す Decision 1 の帰結として、`KsSettingsViewController(store:)` / `KsSettingsView.bind(store)` が要求する Store を外部から渡せない。Host の生成も Bridge の責務にするのが所有関係として最も単純で、phase-2 の MAUI Handler が platform view を要求する構図にも素直に繋がる。
**代替案:**
- **A: Native 側 factory が Bridge と Host を同時生成** — C# から見た所有・破棄の関係が複雑になるため却下
- **B: Host が Bridge を受け取って内部 Store に接続** — 既存 Host の公開 API 変更が必要で、所有が逆転するため却下

→ [maui/ADR-0005](../../decisions/maui/0005-bridge-ownership-model.md) (出典: second-opinion-001/002 の指摘とオーナー裁定)

### Decision 8: phase-1 の accessory 輸送は text ベースに限定する
**採用案:** Builder の Section header / footer と `updateAccessory` が輸送する accessory は text (および clear = null) に限定する。任意 View を内包する accessory は Non-Goals。
**理由:** `SettingsAccessory` / `AccessoryTarget` は associated value 付き union で、任意の SwiftUI / UIKit / Compose View を内包できる。これを interop 境界へそのまま公開することはできず (Decision 2 で union DTO を避けたのと同じ問題)、LabelCell 疎通という phase-1 の範囲には text で十分。
**代替案:**
- **A: 任意 View accessory の輸送を phase-1 で対応** — C# View の Native 変換は CustomCell (phase-5) 級の難度で、疎通フェーズの範囲を超えるため却下

### Decision 9: Section / Cell の ID は Bridge が採番する
**採用案:** interop 境界の ID は canonical UUID 文字列とし、**Bridge (Builder / insert 系 API) が採番して呼び出し側へ返す**。呼び出し側は返された ID だけを更新 API に渡す。未知・不正な ID は Cell / Section 操作で no-op (両 OS 同一の検証・同一の結果)。
**理由:** 呼び出し側採番 (旧記述) では、iOS だけが UUID 解釈に失敗するため同じ C# 操作の結果が OS で非対称になり、「不正 ID で新規作成」という未定義ケースも生じる。Bridge 採番ならこのケース自体が消える。
**代替案:**
- **A: 呼び出し側が任意 String を採番 (iOS のみ UUID 解釈)** — OS 非対称・未定義ケースが残るため却下 (second-opinion-002 指摘)
- **B: 不正 ID を明示的エラーにする** — エラー契約の interop 表現が増える割に、採番を Bridge に寄せれば不要になるため却下

### Decision 10: UI スレッドは呼び出し側契約とする
**採用案:** Bridge の全 API は各 platform の UI スレッドから呼び出す (呼び出し側契約)。Bridge 自身は marshal しない。違反時の挙動は保証しない。
**理由:** iOS Store は `@MainActor`、Android の View 生成・bind も UI スレッド依存。phase-2 の MAUI Handler は UI スレッドで動作するため呼び出し側契約で自然に満たせる。Bridge での marshal は薄い Bridge の原則に反し二重 dispatch の原因になる。
**代替案:**
- **A: Bridge が全操作を UI スレッドへ marshal** — 二重 dispatch と順序保証の複雑化を招くため却下

→ Decision 9・10 とも [maui/ADR-0005](../../decisions/maui/0005-bridge-ownership-model.md) に含めて記録

## Risks / Trade-offs

- binding toolchain の net10.0 非対応が発覚した場合、phase 全体が停止する → spike の先頭配置と成功ゲート (Decision 6) で早期検出
- iOS の内容更新バッチ通知は新設経路 (Android の `_contentUpdateBatches` 相当)。iOS Host のバッチ反映実装が必要
- C# → Native の実行時疎通検証ホストは `maui/` 配下の**テスト資産として維持**する (使い捨てにすると後続フェーズ・SDK 更新時に回帰を再検証できないため)

## Migration Plan

なし (全て additive。既存 API の変更・削除なし)

## Open Questions

- spike の結果 (binding テンプレートの net10.0 対応状況)。問題があれば agenda へ差し戻し

## ADR 候補

起票済みのため申し送りなし。maui/ADR-0001〜0004 は agenda 議論の時点で、**maui/ADR-0005 (Bridge の所有モデル — Decision 5・7・9・10 を包含)** は second-opinion-002 の指摘を受けて、いずれも accepted で起票済み。Decision 8 (accessory text 限定) は phase-1 のスコープ限定であり、恒久契約化するなら後続フェーズの蒸留で判断する。

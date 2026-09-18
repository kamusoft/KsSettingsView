# セカンドオピニオン: android-accessory-view-late-insert-animation (spec-002)
**相方**: codex / **label**: so-spec-android-accessory-view-late-insert-animation / **日付**: 2026-09-17 / **対象**: spec-001 の指摘を反映した提案一式の再確認
---
# 再レビュー結果: android-accessory-view-late-insert-animation

**判定**: NEEDS_DISCUSSION  
**前回指摘**: 5件すべて解消  
**新規指摘**: Critical 0 / Major 3 / Minor 1 / Suggestion 0

静的レビューのみで、ビルド・テスト・ファイル書き込みは行っていません。

## 前回指摘の追跡

### 1. Android の保証と lossy SharedFlow の不整合

**判定**: 解消

**確認箇所**:

- `kasane/changes/android-accessory-view-late-insert-animation/design.md:67`
- `kasane/changes/android-accessory-view-late-insert-animation/specs/android-host/spec.md:31`
- `kasane/changes/android-accessory-view-late-insert-animation/tasks.md:15`
- `kasane/decisions/core/0033-root-accessory-survives-pre-attach-delivery.md:29`

Root 更新を既存の非 replay・容量制限付き `SharedFlow` から拾う方式をやめ、Store から bind 中 Host への同期受け口へ変更しています。bind 直後、通知容量超過、別 Store への再 bind の Scenario とタスクも追加され、前回指摘の配送保証は閉じています。

### 2. 二重適用を検出できない Scenario

**判定**: 解消

**確認箇所**:

- `kasane/changes/android-accessory-view-late-insert-animation/specs/android-host/spec.md:61`
- `kasane/changes/android-accessory-view-late-insert-animation/tasks.md:16`

行数ではなく Adapter の変更通知回数を1回と確認する形に改訂され、二重適用を実際に検出できます。

### 3. iOS 実環境の完了条件不足

**判定**: 解消

**確認箇所**:

- `kasane/changes/android-accessory-view-late-insert-animation/tasks.md:49`
- `kasane/changes/android-accessory-view-late-insert-animation/design.md:106`

iOS でも Root header/footer、Section HeaderView/FooterView、CustomCell Content、初回表示、再訪問を確認し、view accessory の後着または新規の高さ変動があれば本 change の完了を止める条件になっています。既存 CustomCell 高さ問題との境界も明確です。

### 4. iOS の `nil` 保持契約に Scenario がない

**判定**: 解消

**確認箇所**:

- `kasane/changes/android-accessory-view-late-insert-animation/specs/ios-host/spec.md:74`
- `kasane/changes/android-accessory-view-late-insert-animation/tasks.md:24`

view load 前の値設定後に `nil` で解除する Scenario と対応テストが追加されています。

### 5. Android の受け口が Host を延命しないことを検証していない

**判定**: 解消

**確認箇所**:

- `kasane/changes/android-accessory-view-late-insert-animation/specs/android-host/spec.md:81`
- `kasane/changes/android-accessory-view-late-insert-animation/tasks.md:17`
- `kasane/changes/android-accessory-view-late-insert-animation/design.md:107`

Host の回収可能性と、次回 Root 更新時の孤立登録除去まで Scenario・タスク化されています。

## 新規指摘事項

### [🟠 Major] 同期受け口のスレッド境界と例外伝播が未定義

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/design.md:69`  
**関連箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:255`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:537`

**問題点**:  
新設する受け口は「知らせを受けた場で Host のプロパティへ値を控える」としています。現在の `updateAccessory` にはメインスレッド限定の契約や `@MainThread` がなく、既存の Host 反映は `lifecycleScope` の collector を介してメインスレッド側で行われます。

同期受け口から直接 `rootHeader` / `rootFooter` を更新すると、バックグラウンドスレッドから呼ばれた場合に RecyclerView Adapter の通知まで呼び出し元スレッドで実行されます。また、Host 側の処理が例外を送出すると、従来は非同期購読側に閉じていた失敗が公開 `Store.updateAccessory` の呼び出し元へ伝播し、後続 Host への配送や Diff 発行まで中断し得ます。

**推奨修正**:  
次のどちらを契約とするか決定し、design・spec・tasks に反映してください。

- Android の Store 更新 API をメインスレッド限定とし、公開契約・注釈・違反時挙動を定める。
- 同期受け口では thread-safe な pending 値の保持だけを行い、View / Adapter への適用はメインスレッドへ送る。

併せて、複数登録のうち1件の処理失敗が他 Hostへの配送と既存 Diff 発行へどう影響するかを定め、バックグラウンド呼び出しまたは明示的なスレッド違反テストを追加してください。

### [🟠 Major] 受け口登録の多重 Host・同一 Store 再 bind の意味論が決まっていない

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/design.md:69`  
**関連箇所**: `kasane/changes/android-accessory-view-late-insert-animation/tasks.md:18`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:459`

**問題点**:  
現在の `SharedFlow` は複数購読者へ通知でき、Native API に「1 Store は1 Host専用」という制約はありません。また、現行 `bind` は同一 Store への再 bind を明示的に扱っています。

新しい受け口については、次が未決です。

- 同じ Store に複数 Host が bind したとき、全 Hostへ配送するのか。
- 同一 Host が同じ Store へ再 bind したとき、登録を重複させないか。
- 一方の Host の `unbind` や回収時に、他方の登録を残せるか。

単一 callback なら後から bind した Host が前の Host を追い出し、単純なリスト追加なら同一 Store への再 bind で二重適用が起きます。現在の「1更新につき1適用」Scenarioは、bind を一度しか行わないためこの回帰を検出しません。

**推奨修正**:  
登録を Host identity ごとの token とするなど、登録の cardinality・冪等性・解除単位を design に定めてください。少なくとも以下を Scenario / tasks に追加してください。

- 1 Store に2つの Host を bind すると両方が更新される。
- 同じ Host を同じ Storeへ再 bind しても1回だけ適用される。
- 一方を unbind しても他方は更新を受け続ける。

複数 Host を非保証とする場合は、それ自体が既存 Native API の制約追加になるため、公開契約として明示する必要があります。

### [🟠 Major] core/ADR-0006 の Diff 適用境界に対する改訂関係が欠落している

**該当箇所**: `kasane/decisions/core/0033-root-accessory-survives-pre-attach-delivery.md:6`  
**関連箇所**: `kasane/decisions/core/0033-root-accessory-survives-pre-attach-delivery.md:29`、`kasane/decisions/core/0006-structural-diff-ui-store-boundary.md:16`

**問題点**:  
accepted の core/ADR-0006 は、Store の明示操作を単一 Diff として発行し、Native UI が1操作につき1回適用する境界を採用しています。

改訂案では Android の Root 更新だけを同期受け口から適用し、従来の Diff は発行を続けるものの Host側では反映しません。これは少なくとも「Android の Root accessory は通常の Diff 購読経路を使わない」という例外追加です。しかし core/ADR-0033 の `amends` は ADR-0019 のみで、proposal の既存決定との関係にも ADR-0006 がありません。

**推奨修正**:  
次のいずれかを決定してください。

- 同期受け口にも既存の `SettingsRootDiff.UpdateAccessory` をそのまま配送し、「Diff の型と1操作1適用は維持し、配送路だけを分ける」と明記する。
- Diff 適用境界そのものの例外とするなら、core/ADR-0006 への改訂関係を proposal と ADR に明記する。

どちらの場合も、Android の通常購読側が Root Diff を無視する位置と、公開 `applyDiff(UpdateAccessory)` の直接利用は従来どおり有効であることを design に明示してください。

### [🟡 Minor] デルタスペックが内部バッファ容量を受け入れ条件にしている

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/specs/android-host/spec.md:71`

**問題点**:  
「Store の通知の容量を超える回数」は、現行 `SharedFlow` の内部構成を知らないとテスト入力を決められません。さらに改訂後の契約は同期受け口によるもので、通知容量は保証の本質ではありません。内部容量が将来変わると Scenario の入力も暗黙に変わります。

**推奨修正**:  
spec は「メインスレッドのキューを流さず多数回連続更新しても最後の値が反映される」という観察可能な契約に留め、具体的に現在の容量を超える件数を使うことは `tasks.md` のテスト手順へ置いてください。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（常時）
- `kasane/handbook/cross/runtime-behavior-verification.md`（タイミング不具合の完了判定）
- `kasane/handbook/maui/integration-host-verification.md`（MAUI end-to-end確認）
- `kasane/handbook/ios/swift6-language-mode-check.md`（iOS source変更）
- `kasane/handbook/cross/local-development-setup.md`（実機・Sample確認）

## アクションプラン

1. Android 同期受け口のスレッド・例外境界を決定する。
2. 登録の多重 Host対応と同一 Store再 bindの冪等性を仕様化する。
3. core/ADR-0006 との関係を裁定し、proposal / design / ADRをそろえる。
4. 通知容量の具体値を spec からテスト手順へ移す。
5. 再改訂後にスペックレビューを再実施する。

## 突き合わせ結果

前回の 5 件は相方の再確認で全件解消。新規 4 件は、前回の指摘 1 への対処 (同期の受け口) が生んだ論点である。

| # | 指摘 | 採否 | 根拠・対処 |
|---|---|---|---|
| 1 | 同期の受け口のスレッド境界と例外伝播が未定義 (Major) | 採用 (オーナー裁定 2026-09-17) | 更新口を呼べるスレッドの契約は変えない。受け口は値をスレッド安全に控え、反映はメインスレッドで行う。Host での反映の失敗は呼び出し元へ伝えず、他の Host と通知の発行を続ける。design Decision 3 (却下案 E を追加)・spec (Scenario 2 件)・tasks 2.1 / 2.4・core/ADR-0033 に反映 |
| 2 | 複数 Host・同じ Store への再 bind の意味論が未定義 (Major) | 採用 | 現行の通知は複数購読者へ届き、`bind` は同一 Store の再 bind を扱っている。登録は Host ごとに 1 件・全 Host へ知らせる・再 bind で増えない、を design と spec (Scenario 3 件)・tasks に追加 |
| 3 | core/ADR-0006 の適用境界との関係が欠落 (Major) | 採用 | 受け口へは通知と同じ更新の値を渡し、型と 1 操作 1 適用の境界は変えず配送路だけを分ける、と design と core/ADR-0033 に明記。公開の `applyDiff` の直接利用は有効のまま。ADR-0006 の改訂は不要 |
| 4 | spec が内部の通知容量を受け入れ条件にしている (Minor) | 採用 | spec は「キューを流さない多数回の連続更新」に改め、具体の件数は tasks へ移した |

確定 0 / 採用 4 / 降格 0 / 未解決 0。

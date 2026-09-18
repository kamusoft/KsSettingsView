# セカンドオピニオン: android-accessory-view-late-insert-animation (spec-001)
**相方**: codex / **label**: so-spec-android-accessory-view-late-insert-animation / **日付**: 2026-09-17 / **対象**: 提案一式 (proposal.md / design.md / specs/ / tasks.md、core/ADR-0033・maui/ADR-0027)
---
# レビュー結果: android-accessory-view-late-insert-animation

**判定**: NEEDS_DISCUSSION  
**指摘件数**: Critical 0 / Major 3 / Minor 2 / Suggestion 0

## サマリー

変更方針は既存の責務分離と整合していますが、Android の新保証を現在の通知経路で確実に実現できるかが設計上閉じていません。また、二重適用と iOS 実環境の受け入れ基準に検出不能・判定不足があります。実装開始前に仕様・design・tasks の修正が必要です。

静的レビューのため、ビルド・テストは実行していません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（常時）
- `kasane/handbook/cross/runtime-behavior-verification.md`（アニメーション・初回フレームの不具合）
- `kasane/handbook/maui/integration-host-verification.md`（MAUI facade から Native 表示までの疎通）
- `kasane/handbook/ios/swift6-language-mode-check.md`（iOS source 変更の完了判定）
- `kasane/handbook/cross/local-development-setup.md`（Sample・実機検証）

## 指摘事項

### [🟠 Major] Android の「bind 後は失わない」保証が lossy な SharedFlow と整合していない

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/specs/android-host/spec.md:25`  
**関連箇所**: `kasane/changes/android-accessory-view-late-insert-animation/design.md:67`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:52`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:306`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:465`

**問題点**:  
Requirement は `bind(store)` から `unbind()` までに渡された Root 値を失わず、複数回なら最後の値を反映すると無条件に保証しています。しかし現在の `diffs` は `replay = 0`、容量 64 の `MutableSharedFlow` へ、戻り値を無視した `tryEmit` で送っています。

このため、次の経路が未解決です。

- `bind()` から新しい受け口の collect 開始までに同期的に更新された値は、購読者不在なら失われる。
- メインスレッドを譲らず 65 件以上を連続送信すると、遅い購読者に対するバッファが満杯になり、最後の値を含む `tryEmit` が失敗し得る。
- Section 等は Store 現在状態から再同期できますが、Root は意図的に Store 状態へ含めないため回復手段がない。

設計は「Host が聞く時間を広げる」ことだけを扱い、通知自体が受け口まで到達しない場合を扱っていません。このままでは Requirement の SHALL を実装だけで満たせない可能性があります。

**推奨修正**:  
Root 通知について、次の両方を保証できる配送方式を design に確定してください。

1. `bind()` の戻り時点で受け口が同期的に有効である。
2. 連続更新でも最新値を落とさない。

Store に Root の現在状態を置かない制約を保つなら、同期的な Host observer 登録、Root 専用の latest-wins 経路などが候補です。tasks には少なくとも以下の回帰テストを追加してください。

- `bind()` 直後、キューを流さず Root を更新する。
- 現在の容量を超える連続更新後も最後の値が表示される。
- 別 Store への再 bind 後、旧 Store の通知が反映されない。

### [🟠 Major] 「attach 中は一度だけ適用」の Scenario が二重適用を検出できない

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/specs/android-host/spec.md:55`  
**関連箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt:11`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt:39`

**問題点**:  
Scenario の THEN は「Root header の行が1つだけ」としていますが、Root 用 Adapter は設計上 `itemCount` が常に 0 または 1 です。同じ値が新旧2系統の購読から二重適用されても、2行にはならず `notifyItemChanged` が余分に発行されるだけです。

したがって、この Scenario は Requirement の「二重には適用されない」を検証できません。二重適用により View factory の再実行、内部状態の喪失、不要な再 bind が起きてもテストが通ります。

**推奨修正**:  
THEN を適用回数そのものが観測できる形へ変更してください。例えば以下のいずれかです。

- Root 更新を Host が処理した回数をテスト seam で記録し、正確に1回と確認する。
- 状態を持つ `RootAccessory.View` の factory / bind 回数が1回であることを確認する。
- Adapter observer で挿入・変更通知の回数を確認する。

tasks.md の「attach 中は一度だけ適用」も、行数ではなくこの観測点を明記すべきです。

### [🟠 Major] iOS 実環境の完了条件が MAUI の SHALL を検証していない

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/tasks.md:47`  
**関連箇所**: `kasane/changes/android-accessory-view-late-insert-animation/specs/maui-core/spec.md:7`、`kasane/changes/android-accessory-view-late-insert-animation/specs/maui-cells/spec.md:7`、`kasane/changes/android-accessory-view-late-insert-animation/tasks.md:46`

**問題点**:  
maui-core / maui-cells の Requirement はプラットフォームを限定せず、初回接続と再接続の最初の表示に View / Content が含まれることを要求しています。Android の 6.2 はこの結果を明示的に確認しますが、iOS の 6.3 は「修正前より新しい遅延・高さのブレが増えていない」ことしか要求していません。

そのため、修正前の iOS に既存の後着があった場合、それが残ったままでも 6.3 は成功し得ます。また、FooterView に新しい高さのブレが出た場合は現在の maui-core Requirement 違反ですが、別 change への記録だけで本 change を完了できるようにも読めます。

**推奨修正**:  
iOS 実機にも Android と同じ受け入れマトリクスを設定してください。

- Root header / footer
- Section HeaderView / FooterView
- CustomCell Content
- 初回接続とページ再訪問
- 最初にページ内容が描画されるフレームから存在すること

新たな FooterView の高さ変動は本 change の完了を阻止する、と明記してください。Non-Goal の既存 CustomCell 計測問題だけは、内容が後着していないことと高さ計測問題を区別して判定します。

### [🟡 Minor] iOS の `nil` 保持契約に対応する Scenario と task がない

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/specs/ios-host/spec.md:46`  
**関連箇所**: `kasane/changes/android-accessory-view-late-insert-animation/tasks.md:22`

**問題点**:  
Requirement は `nil` による解除も値として保持すると明記していますが、iOS の Scenario と task は非 nil 値 A → B だけです。「非 nil だけを控える」誤実装でも全予定テストが通ります。Android には footer を値 C から `nil` にする Scenario があり、非対称です。

**推奨修正**:  
view load 前に header または footerへ値を設定し、続けて `nil` を渡した場合、viewDidLoad 後に表示されない Scenario を追加してください。task 3.1 にも明記します。

### [🟡 Minor] Android の常時受け口が Host を延命しないことを検証する task がない

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/design.md:71`  
**関連箇所**: `kasane/decisions/core/0033-root-accessory-survives-pre-attach-delivery.md:48`、`kasane/changes/android-accessory-view-late-insert-animation/tasks.md:15`

**問題点**:  
新しい受け口は bind から unbind まで生存し、unbind されずに Host が破棄された場合は weak 参照と次回通知で自己停止する設計です。しかし task 2.1 は機能挙動だけで、Host 非保持や自己停止を検証しません。実装時に coroutine のクロージャが誤って Host を強参照しても検出できず、長命 Store が画面を延命する回帰になり得ます。

**推奨修正**:  
長命 Store に bind した Host を unbind せず解放し、弱参照が回収可能であること、および次の通知で孤立した受け口が停止することを確認するライフサイクルテストを task 2.1 に追加してください。

## アクションプラン

1. Android Root 通知の配送方式を再設計し、非 replay・容量制限・bind 直後の購読確立を明文化する。
2. Android の二重適用 Scenario を、実際の適用回数を検出できる受け入れ基準へ変更する。
3. iOS 実機検証を仕様結果そのものの確認へ引き上げ、違反時は完了不可と明記する。
4. iOS の `nil` 解除と Android の受け口寿命テストを追加する。
5. proposal / design / specs / tasks / proposed ADR の表現をそろえた後、再レビューする。

## 突き合わせ結果

突き合わせの相手: ホスト側の自己レビュー (整合性チェックリスト・上位層違反チェックを通過、検査軸は合流先の契約 / 条項ペア / 責務の閉路 / 前提の成立可能性)。ホスト側は下の 5 件をいずれも検出していなかった。

| # | 指摘 | 採否 | 根拠・対処 |
|---|---|---|---|
| 1 | Android の保証が再生なし・容量制限つきの通知と整合しない (Major) | 採用 | `SettingsRootStore.kt` の `MutableSharedFlow(replay = 0, extraBufferCapacity = 64)` と戻り値を見ない `tryEmit` を照合して確認。MAUI facade が Host 生成直後に同期で Root を渡す主経路に当たる。design Decision 3 を同期の受け口へ改め、core/ADR-0033・spec・tasks を追随 |
| 2 | 「一度だけ適用」の Scenario が二重適用を検出できない (Major) | 採用 | Root 用 Adapter の行数は常に 0 か 1。THEN を変更通知の回数へ変更 |
| 3 | iOS 実環境の完了条件が SHALL を検証していない (Major) | 採用 | tasks 6.3 を Android と同じ項目へ引き上げ、view accessory の後着・新規の高さ変動は完了を止める条件にした |
| 4 | iOS の解除 (`nil`) の Scenario が無い (Minor) | 採用 | Scenario と task 3.1 に追加 |
| 5 | Host を延命しないことの検証が無い (Minor) | 採用 | Scenario と task 2.3 を追加 |

確定 0 / 採用 5 / 降格 0 / 未解決 0。再確認は second-opinion-spec-002.md。

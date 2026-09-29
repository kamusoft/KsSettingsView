# セカンドオピニオン: add-scroll-control (spec-001)
**相方**: codex / **label**: so-spec-add-scroll-control / **日付**: 2026-09-29 / **対象**: 提案一式 (proposal.md / design.md / specs/ / tasks.md)
---
# 提案レビュー: add-scroll-control

**判定: NEEDS_DISCUSSION** — Major 5 件、Minor 2 件。静的レビューのみで、ビルド・テストは実行していません。承認済みの「復元を済ませてから準備完了を通知する」という決定と提案の実行順が食い違い、Compose の復元方法も未確定です。このまま実装に渡す前に、仕様と設計を確定する必要があります。

## 指摘事項

### 🟠 Major — 準備完了が位置の復元完了より先に通知される

**該当箇所**: [design.md:157](kasane/changes/add-scroll-control/design.md:157)、[design.md:192](kasane/changes/add-scroll-control/design.md:192)、[agenda.md:76](kasane/roadmaps/maui-support/phases/phase-8-scroll-control/agenda.md:76)

**問題点**: design は `makeHost*` 内で復元を待ち行列へ積み、Host の取り付け後に準備完了コマンドを実行します。命令の実行順は指定されていますが、合図の時点では復元が完了していません。これは承認済み決定の「復元を済ませてから準備完了の合図」と異なります。

**推奨修正**: 復元の完了を観測してから合図を出す経路を定めてください。合図を「命令を受け付けられる時点」と再定義するなら、承認済み決定との差をオーナーと議論してください。

### 🟠 Major — Compose の Activity 再生成時の復元方法が未決

**該当箇所**: [design.md:248](kasane/changes/add-scroll-control/design.md:248)、[tasks.md:28](kasane/changes/add-scroll-control/tasks.md:28)

**問題点**: 提案は Compose 内の View Host が保存状態を受け取るかを Open Question とし、受け取らなければ実装を止める計画です。一方、フェーズ決定は Compose 経路を**提案化で確かめる**としており、Android spec は Activity 再生成後の復元を保証しています。実装ワーカーが仕様どおり進められる状態になっていません。

**推奨修正**: 提案段階で経路を確認し、Compose 側の保存方法、または保証の適用範囲を決定して spec に反映してください。

### 🟠 Major — Android の commit callback を待つ設計では待ち行列が止まり得る

**該当箇所**: [design.md:78](kasane/changes/add-scroll-control/design.md:78)、[tasks.md:21](kasane/changes/add-scroll-control/tasks.md:21)

**問題点**: 既存実装は、連続する `submitList` で先行世代の完了 callback が破棄されることを明記しています（[KsSettingsListAdapter.kt:97](android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:97)）。全提出の callback を数えて待つと、破棄された callback 分が残り、命令を消化できません。

**推奨修正**: 破棄された世代を含めて待機を解消できる方式を設計し、連続提出で古い callback が呼ばれない Scenario を追加してください。

### 🟠 Major — 宣言 UI の更新後に解決する順序が保証されていない

**該当箇所**: [design.md:93](kasane/changes/add-scroll-control/design.md:93)

**問題点**: iOS の `main.async` 1 回、Android の 1 フレーム待ちを、宣言ツリーの更新完了の代わりにしています。しかし、この遅延と更新完了を結ぶ条件が設計されていません。「状態を追加して同じ処理で新 ID へ命令する」Scenario を満たす根拠が不足します。

**推奨修正**: ラッパーが更新後のツリーを Host に反映したことを命令の解決条件にし、その条件を待つ仕組みを設計してください。

### 🟠 Major — MAUI の生成元項目と生成物の対応を保持する計画がない

**該当箇所**: [design.md:207](kasane/changes/add-scroll-control/design.md:207)、[tasks.md:44](kasane/changes/add-scroll-control/tasks.md:44)

**問題点**: design は binder が項目と生成物の並びを保持する前提ですが、現行 binder が保持するのは生成物の `_generated` だけです（[KsItemsSourceBinder.cs:31](maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs:31)）。`ItemsSource` は `IEnumerable` なので、命令時の再列挙を生成時の項目との対応として扱うこともできません。

**推奨修正**: 生成・追加・置換・移動・削除の各経路で「生成元項目と生成物」の対応を保持する作業を tasks に明記し、再列挙に頼らない Scenario を追加してください。

### 🟡 Minor — 表示要素のない Section のスクロール先が未定義

**該当箇所**: [settings-view-ios-ui/spec.md:105](kasane/changes/add-scroll-control/specs/settings-view-ios-ui/spec.md:105)、[settings-view-android-ui/spec.md:105](kasane/changes/add-scroll-control/specs/settings-view-android-ui/spec.md:105)

**問題点**: 見出しがなければ最初の表示 Cell、Footer がなければ最後の表示 Cell を境界としますが、空の Cell 配列は既存契約で有効です（[settings-tree.md:72](kasane/concepts/core/core-model/settings-tree.md:72)）。表示 Cell がなく、片方または両方の accessory もない Section の範囲を決められません。

**推奨修正**: 表示要素が一つだけある場合と、まったくない場合の扱いを両 OS の spec で定めてください。

### 🟡 Minor — Store への再接続時にハンドルが戻るか不明

**該当箇所**: [settings-view-ios-ui/spec.md:27](kasane/changes/add-scroll-control/specs/settings-view-ios-ui/spec.md:27)、[settings-view-android-ui/spec.md:37](kasane/changes/add-scroll-control/specs/settings-view-android-ui/spec.md:37)

**問題点**: `disconnectStore()` / `unbind()` では接続を外す一方、同じ Host を再び Store に接続した場合、保持中の `scrollController` を自動でつなぎ直すかが指定されていません。

**推奨修正**: 再接続時の規則を決め、同じハンドルで再び命令できるかを Scenario にしてください。

**照合した規約**: `ksn-review`、`ksn-core`、handbook の comment-policy・diagnostic-message-language・sample-parity・runtime-behavior-verification。tasks.md に長命層を書き換える実装タスクはありません。



## 突き合わせ結果

ホスト側の自己レビュー (2 周、指摘 2 件を自己修正: UI 層に置く型の理由の明記・通常遷移中の命令の扱い / テスト実行コマンドの参照先) はいずれの指摘とも重ならず、相方の 7 件はすべて「相方のみ」。根拠をコードと規約で確かめて採否を決めた。

| # | 指摘 | 判定 | 根拠と対応 |
|---|---|---|---|
| M1 | 準備完了の合図が復元の完了より先に出る | 採用 (オーナー判断で文言を明確化) | agenda 論点4 の決定文言「復元を済ませてから合図」と design の実行順が文字どおりには一致しなかった。オーナー判断で、合図は「命令が効く時点」に出し、復元は合図の中の命令より先に処理される、という意味に agenda・maui/ADR-0030 (proposed)・design Decision 9 をそろえた。復元完了の通知経路は足さない |
| M2 | Compose の Activity 作り直しでの復元が未決 | 採用 | agenda は「提案化で確かめる」と決めていた。Compose UI 1.9.5 の `ViewFactoryHolder` が `SaveableStateRegistry` に登録して `saveHierarchyState` / `restoreHierarchyState` で中の View の保存状態を運ぶことを確認し、design Decision 7・Open Questions・Android spec (Compose の Scenario)・tasks 2.10 に反映 |
| M3 | commit callback を数えて待つと連続提出で止まる | 採用 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt` の既存コメントが先行世代の callback の破棄を記録済み。最新世代の callback だけを待つ方式に変え、design Decision 3・Android spec (連続提出の Scenario)・tasks 2.3 / 2.4 に反映 |
| M4 | 宣言 UI の更新後に解決する根拠が無い | 採用 | 前提 (同じ処理の状態変更は 1 回の遅延より前に update される) と根拠 (KsCollectionView の iOS / Android のテスト)、世代の控え方、前提が崩れたら止める規律を design Decision 4 と tasks 1.8 / 2.9 に明記。spec の既存 Scenario がテストの網になる |
| M5 | MAUI の生成元項目と生成物の対応を保持していない | 採用 | `maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs` は生成物の並びだけを持つことを確認。binder に生成元の項目の並びを持たせることを design Decision 10・maui-core spec (移動・置換後の Scenario)・tasks 4.4 に反映 |
| m6 | 表示要素の無い Section の範囲が未定義 | 採用 | 表示されている見出し・Cell・Footer をつないで範囲を取り、1 つも無ければ no-op と両 OS の spec と design Decision 5 に明記 |
| m7 | Store への再接続時のハンドルの扱いが不明 | 採用 | 切断でプロパティを `nil` / `null` に戻し、自動ではつなぎ直さない (再代入が要る) と両 OS の spec・design Decision 2・tasks 1.2 / 2.2 に明記 |

集計: 確定 0 / 採用 7 / 降格 0 / 未解決 0 (M1 はオーナー判断を経て採用)

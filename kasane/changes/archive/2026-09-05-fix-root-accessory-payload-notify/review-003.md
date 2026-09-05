# レビュー結果: fix-root-accessory-payload-notify (003 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

前回 (review-002) の指摘 3 件 (Minor 1 / Suggestion 2) はいずれも閉じている。修正はコメント本文のみで、実装ロジック・テストの構造には変更がない。新規の指摘はなし。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 今回触れた 3 箇所のコメントについて、許容参照・禁止参照・禁止する記述類型・公開 doc コメントの内部用語禁止を本文から照合した。`python3 scripts/comment-policy-lint.py --summary` は 761 ファイル / 禁止 0 件、`python3 scripts/local-path-lint.py` も検出 0 件
- 他の handbook 文書は今回の差分 (コメント文言のみ) に当たらないため適用外。ADR・実装スキルの観点は review-001 / review-002 で照合済みで、今回の修正はそのどれにも触れていない

## 前回指摘の閉じ方

| 前回指摘 | 状態 | 確認内容 |
|---|---|---|
| Minor: 存在しない型名 `KsAnyView.View` | 閉 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt:47` が「中身（`RootAccessory.View` が持つ `KsAnyView`）」に修正済み。`RootAccessory.kt:30` の `public class View(public val view: KsAnyView)` と一致し、両識別子とも grep で到達できる。差分検出が効かない理由 (`View` 形式は等価性判定でクラス一致のみを見る) を指す説明として文意も通っている |
| Suggestion: 3 引数版 `onBindViewHolder` の KDoc から payload なしの経路が落ちた | 閉 | 同ファイル `:119-120` の列挙が「[KsSettingsView.PAYLOAD_CONTENT] を伴う内容差し替え・payload なし・text 形式・Theme 以外の payload が混ざる通知」に戻っている。`themeOnly` の `payloads.isNotEmpty()` ガードで `super` へ落ちる経路が KDoc から読み取れる状態に復帰し、姉妹実装 `KsSettingsListAdapter.kt:206` の列挙 (「…・payload なし・text accessory・Cell」) と記述の厚みが揃った |
| Suggestion: 共有テストヘルパの範囲宣言が追加内容を含まない | 閉 | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt:13-19` の冒頭コメントが「待機・表示内容の観測・変更通知の記録」の 3 本立てに更新され、列挙にも `変更通知の記録 (ChangeRecordingObserver)` が入った。ファイル冒頭の宣言と実際の中身 (`:168-182` の `ChangeRecordingObserver`) が一致している |

## comment-policy の再照合 (今回触れた箇所)

- 禁止参照なし: 作業文書のパス・変更識別子の裸参照・レビュー通番はいずれも含まれない。`android/ADR-0001` は許容形式 (`<domain>/ADR-NNNN`) で、置かれている `RootHeaderFooterAdapter` は `internal class` のため公開 doc コメントの内部用語禁止には当たらない
- 禁止する記述類型なし: 進捗ログ・過去仕様の説明・デルタスペック構文キーワードはいずれも含まれず、すべて現在形の仕様説明になっている
- 単独可読性: 3 箇所ともファイル単独で意味が通る。参照している識別子 (`RootAccessory.View` / `KsAnyView` / `KsSettingsView.PAYLOAD_CONTENT` / `SimpleItemAnimator.canReuseUpdatedViewHolder` / `ChangeRecordingObserver`) はすべて実在し到達可能

## 指摘事項

なし。

## アクションプラン

なし。本 change はアーカイブ可能な状態。蒸留への申し送りは review-002 のものをそのまま引き継ぐ (`kasane/decisions/android/0001-content-update-preserves-viewholder.md` の残課題・現存表記と、`kasane/decisions/android/0012-full-update-content-sync-diffcallback-and-setrootdirect.md` の Consequences 関連項の追随)。

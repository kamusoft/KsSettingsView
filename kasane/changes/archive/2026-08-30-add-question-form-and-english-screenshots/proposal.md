# Proposal: add-question-form-and-english-screenshots

## Why

phase-2 (public 化) は公開ツリーの作成手前で止まっている。手順書 2 節へ進むには、phase-9 から申し送られた 3 件の決定をリポジトリに反映しておく必要がある — 公開リポジトリの履歴に「直すと決めたのに直っていない状態」を initial commit として刻まないため (cross/ADR-0021 が履歴を引き継がない単一 commit と定めているので、公開後に直しても最初の 1 commit は残らない)。

3 件はいずれも公開リポジトリを訪れた人が最初に触れる面に関わる。

- **質問の行き先が無い**: Issue 作成画面の選択肢がバグ報告と提案の 2 本だけで、`blank_issues_enabled: false` のため自由記述の Issue も作れない。「使い方が分からない」「仕様か不具合か判断できない」利用者は、必須の再現手順を埋められないままバグ報告テンプレートへ流し込むか、諦めることになる (cross/ADR-0024 の負の帰結)
- **英語 README の第一印象が読めない**: ルート `README.md` のスクリーンショット 4 枚が Sample アプリの日本語 UI で、英語話者には製品が何を作れるのか伝わらない
- **公開しない資産への言及が残る**: `maui/spike/` を公開リポジトリに載せないと決めたため、docs-refresh がその README を追従対象外として名指ししている記述が、存在しないファイルを指すことになる

## What Changes

phase-2 agenda の決定事項 3 件 (「Issue の質問窓口」「英語 README のスクリーンショットの言語」「`maui/spike/` は公開リポジトリに載せない」) を反映する。

| 対象 | 変更 | 能力 |
|---|---|---|
| `.github/ISSUE_TEMPLATE/question.yml` | 質問用 Issue Form を新設 (英語 1 本、`question` ラベル)。必須項目はバージョン / platform / 試したこと / 参照した Skill・README の箇所 | repository-docs |
| `.github/CONTRIBUTING.md` / `CONTRIBUTING_ja.md` | 「How to open an issue」に質問テンプレートの段落を追加 (英日ロックステップ) | repository-docs |
| `assets/*.png` (4 枚) | 英語表示で撮り直して差し替え | repository-docs |
| `.agents/skills/docs-refresh/SKILL.md` | `maui/spike/README.md` への言及を、公開リポジトリに存在しない前提へ更新 | docs-refresh |

**スクリーンショットの撮り方**: Section 装飾デモ画面の表示文字列を**一時的に英語へ書き換えて撮影し、撮影後に元へ戻す**。`samples/` には恒久的な差分を残さない。ロケールリソースは導入しない。撮影に使った英訳文言は `ui/brief.md` に記録し、後日の撮り直しを再現可能にする。

## Non-Goals

- **Sample のローカライズ (リソース基盤の導入・恒久的な英語化)** — 撮影のためだけに機構を持ち込まない (オーナー判断)。`samples/` に恒久差分を残さないため、[Sample のプラットフォーム間一致](../../concepts/cross/conventions/sample-parity.md) が要求する 3 platform の文言一致は committed 状態で保たれ、MAUI 追随の義務も生じない
- **`maui/spike/` の削除・除外操作** — 公開ツリーを組む時の除外であり、リポジトリ内の変更ではない。phase-2 の実施手順書 2 節が担う
- **ルート README 本文の変更** — 貢献節は「用意された Issue テンプレートを使う」としか書いておらずテンプレート数に依存しない。docs-refresh の追従対象でもあるため触らない
- **`question` ラベルの作成** — GitHub の既定ラベルに含まれるため repo 操作は不要 (既存の `bug` / `enhancement` も既定ラベル)。新 repo 作成時の存在確認だけを実施手順書へ申し送る
- **GitHub 設定の変更 (Discussions / Pull requests)** — リポジトリ操作であり実施手順書 3 節が担う
- **phase-2 の他の決定事項の実行** — 履歴スキャン・履歴の扱い・evidence 媒体の範囲・ローカル絶対パス・端末識別子・public 化の実施手順は、リポジトリを公開する作業そのものであり、[実施手順書](../../roadmaps/package-distribution/phases/phase-2-public-readiness/artifacts/publish-procedure.md) が担う。本変更が引き受けるのは、公開前にリポジトリの中身として直しておく必要がある 3 件だけ

## Impact

- 破壊的変更なし。ライブラリ本体 (`ios/` `android/` `maui/`) と `samples/` に恒久的な差分を残さない
- **英語 README のスクリーンショットは、Sample のどの状態にも対応しない**表示になる。Sample を実際に動かすと日本語で表示される。撮影のためだけの一時改変という性質上避けられず、Sample を英語化しない選択の対価として受け入れる
- スクリーンショット差し替えはオーナー承認ゲートを通す (phase-9 と同じく `ui/` に候補を置く形)
- 公開ドキュメント面の README 枚数は変わらない (ルート 2 + `skills/` 索引 2)

## 級: M

repository-docs と docs-refresh の 2 能力にまたがるため、ksn-core の判定表では「複数能力横断」= L に当たる。それでも **M とするのはオーナー判断** (ksn-core「判定は ksn-explore / ksn-propose が推奨し、ユーザーが確定する」) であり、根拠は次のとおり:

- 変更対象はドキュメント・設定・画像だけで、プロダクションコードもデータスキーマも公開 API も含まない。L 基準が挙げる「アーキテクチャ・データスキーマ・認証・外部連携 / 覆すコストが高い」のいずれにも当たらない
- docs-refresh 側の変更は `SKILL.md` の 1 文の書き換えだけで、repository-docs 側の決定 (`maui/spike/` を公開しない) から機械的に導かれる。独立した設計判断を持たない
- design.md に書くべき Decision が残っていない (当初案にあったロケール機構の選定は撤回済み)

デルタスペックは Issue Form の必須項目とスクリーンショットの提示条件という検証可能な契約があるため作る。3 件を分割しないのは、いずれも「公開ツリー作成の前提」という単一のゲートに紐づいており、別々に出すと独立レビューを 2 周走らせることになるため。

domain: cross
roadmap: package-distribution/phase-2-public-readiness

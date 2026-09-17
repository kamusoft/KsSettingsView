# セカンドオピニオン: scroll-indicator-visible-not-applied (spec-001)
**相方**: codex / **label**: so-spec-scroll-indicator-visible-not-applied / **日付**: 2026-09-17 / **対象**: 提案一式 (proposal.md / specs/settings-view-ios-ui/spec.md / specs/settings-view-android-ui/spec.md / tasks.md)
---
# レビュー結果: scroll-indicator-visible-not-applied

**判定**: NEEDS_DISCUSSION  
**指摘件数**: Critical 0 / Major 5 / Minor 1 / Suggestion 0

## サマリー

公開既定色の変更が accepted の `core/ADR-0030` と衝突しており、ADR 改訂なしには提案を確定できません。また、MAUI の観察可能な挙動、Android の外観切替時のスクロールバー、複数の SHALL に対応する Scenario が未確定です。

`HEAD` のコード・テストを現仕様として照合し、未コミット実装は仕様判断の根拠にしていません。依頼どおりビルド・テストは実行していません。

## 照合した規約

- ksn-core デルタスペック形式・UI アーティファクト規約
- `kasane/lessons/spec-review.md` L-001〜L-005
- `comment-policy.md`（always）
- `test-execution.md`
- `runtime-behavior-verification.md`
- `sample-parity.md`
- `local-development-setup.md`
- `swift6-language-mode-check.md`
- `core/ADR-0030`
- `android/ADR-0020`
- Theme 更新、Native Host、DSL/Store、Header/Footer 所有境界に関する concepts

## 指摘事項

### [🟠 Major] accepted ADR と正面衝突しているが、改訂手続きがない

**該当箇所**: `proposal.md:17`、`proposal.md:31`、`kasane/decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md:28`

**問題点**: `core/ADR-0030` は、ライト側既定色を従来値のまま保ち、iOS の既定色定数を dynamic `UIColor` とすることを明示的に決定しています。本提案は Header/Footer のライト既定を透明へ変えるため、この決定の例外ではなく改訂です。

`proposal.md:31` は「既存利用者のライト時の見た目を変えない」を当時の change 限定の制約として扱っていますが、accepted ADR の Decision として現在も有効です。exploration の「ADR 候補なし」とも整合しません。

**推奨修正**: `core/ADR-0030` を部分改訂する proposed ADR を起票し、少なくとも以下を明記してください。

- Header/Footer 背景だけを従来値維持の例外とすること
- 透明化する理由と、Android・MAUI を含む既存画面への影響
- iOS の公開定数を dynamic のまま保つか、固定色へ変えるか
- 明示色と既定色の等価性への影響

ADR の方向が確定してから proposal/spec をそれに合わせる必要があります。

### [🟠 Major] M 級の利用者可視 UI 変更なのに必須の `ui/` ゲートを省略している

**該当箇所**: `proposal.md:34`

**問題点**: スクロールバーの出現、既定の帯の消失、Classic/Modern における背景範囲は利用者可視の UI 変更です。ksn-core / ksn-propose は、UI に触れる変更では級を問わず `ui/brief.md` と承認モックを必須としています。M 級では「見た目として新たに決める要素がない」という理由による省略規定はありません。

`evidence/` の実装スクリーンショットは動作証跡であり、実装前の見た目の正である承認モックの代替にはなりません。

**推奨修正**: `ui/brief.md` と承認モックを追加し、少なくとも以下を正として固定してください。

- Classic / Modern
- 既定透明と明示背景色
- Section / Root の Header / Footer
- スクロールバー表示・非表示
- iOS / Android で塗る領域と塗らない View accessory

`tasks.md` にモックとの視覚照合も追加してください。

### [🟠 Major] MAUI の観察可能な変更がデルタスペックから欠落している

**該当箇所**: `proposal.md:17`、`proposal.md:18`、`proposal.md:29`、`tasks.md:22`

**問題点**: proposal 自身が、MAUI の未指定値は native の新しい既定へ追随すると述べています。したがって facade・bridge のコードを変更しなくても、次の利用者可視挙動は MAUI でも変わります。

- MAUI Android の既定スクロールバーが表示される
- `ScrollIndicatorVisible=false` が両 native へ反映される
- 未指定の Header/Footer 背景が透明へ変わる
- MAUI Android では既定の帯が消える

ところが影響能力は native 2能力だけで、MAUI の Requirement / Scenario がありません。`tasks.md:22` も「テストがあれば含む」という条件付きで、契約の必須検証になっていません。これは lessons L-005 の「受け付ける入力の全種」との照合不足です。

**推奨修正**: `specs/maui-bridge/spec.md` を追加し、未指定 `null`、明示値、`ScrollIndicatorVisible` の輸送結果を定めてください。MAUI iOS / Android の実行面も tasks 5.2 の明示的な検証対象にしてください。

### [🟠 Major] Requirement 本文の複数の SHALL に対応する Scenario とテスト計画がない

**該当箇所**:

- `specs/settings-view-ios-ui/spec.md:5`
- `specs/settings-view-ios-ui/spec.md:29`
- `specs/settings-view-android-ui/spec.md:5`
- `specs/settings-view-android-ui/spec.md:36`
- `tasks.md:7`
- `tasks.md:9`
- `tasks.md:17`

**問題点**: 次の保証は Requirement 本文にありますが、Scenario で判定できません。

- 両OS: Theme 変更時のスクロール位置維持
- 両OS: Section / Cell identity の維持
- Android: 構造更新と Theme 適用が同時に起きる経路
- iOS: text 形式から View 形式へ差し替えた際に以前の背景色を残さない
- Android: 利用者へ渡されるものがホスト Context そのものであること  
  現 Scenario は属性値の解決だけなので、別ラッパでも通過できます

特に tasks 1.2、1.4、3.2 は既存 Scenario の件数だけを担保するとしており、上記 SHALL をテスト対象に追加していません。

**推奨修正**: 保証ごとに Scenario を追加し、tasks に一対一で対応付けてください。identity は「IDが同じ」なのか「Native Viewインスタンスも同じ」なのかを明記し、スクロール位置も比較可能な観測値を定めてください。

### [🟠 Major] Android の外観切替時のスクロールバー挙動が実装者選択のまま

**該当箇所**: `tasks.md:10`、`kasane/concepts/android/api/android-native-host.md:122`

**問題点**: tasks 1.5 は「追従させる」か「追従しない制約をコメントする」かを実装者に委ねています。どちらでもタスク完了になり、受け入れ結果が一意ではありません。

スクロールバーは同梱 DayNight テーマから初期化される新しいライブラリ所有 UI です。既存契約では同梱テーマ由来の値が uiMode に追随します。一方、RecyclerView は生成済みインスタンスを維持するため、明示的な対処なしでは古い外観の drawable が残る実装リスクがあります。

**推奨修正**: 次のどちらかを proposal/spec で決定してください。

- 推奨: Activity を再生成しない夜間モード変更でも新しい外観へ追従し、Scenarioと実行時検証を追加する
- 非追従を許容するなら、理由・利用者影響・既存外観契約との関係を Non-Goal または ADR 改訂として明記する

コードコメントだけで製品契約を決めないでください。

### [🟡 Minor] iOS の「透明な既定色」と既存の等価性契約の関係が曖昧

**該当箇所**: `specs/settings-view-ios-ui/spec.md:55`、`specs/settings-view-ios-ui/spec.md:57`、`tasks.md:20`

**問題点**: 現契約では既定色定数は dynamic `UIColor` であり、同じ生値を固定色で明示した Theme は既定 Theme と等価ではありません。本 spec は「他の等価性契約は変えない」としつつ、task は dark 側内部値の整理を求めています。

`defaultHeaderBackgroundColor = .clear` のような固定色にすると、利用者が明示した `.clear` が既定 Theme と等価になる可能性があり、従来契約が変わります。現在の Scenario は公開定数を明示した場合しか検証していません。

**推奨修正**: ADR 改訂と合わせ、以下のどちらかを明文化してください。

- dynamic provider を維持し、固定 `.clear` の明示は既定と非等価
- 固定 `.clear` へ変更し、等価性契約も意図的に改める

固定透明色を明示した Theme との等価性 Scenarioを追加してください。

## アクションプラン

1. `core/ADR-0030` の改訂方針とAndroidの外観切替契約をオーナー判断で確定する。
2. proposal/specをMAUIまで拡張する。
3. 欠落している Scenario と対応テストタスクを追加する。
4. `ui/` 一式を作成し、モック承認を取得する。
5. 修正後、M級で必須のクロスモデル・スペックレビューを再実施してから実装フェーズへ戻る。



## 突き合わせ結果

突き合わせの相手: ホスト側の自己レビュー (ksn-propose Step 8。2 周・指摘なし。検査軸は Scenario の前提の成立可能性 / 過去の既定色 Requirement との条項ペア / 合流先 (表示状態同期) の契約 / UI lint)。2026-09-17。

| # | 相方の指摘 | 採否 | 根拠と反映先 |
|---|---|---|---|
| 1 | accepted の core/ADR-0030 と衝突しているが改訂手続きがない (Major) | 採用 | 相方のみ・根拠強 (Decision 1 の文言「ライト側の既定値は現行の固定 RGB をそのまま使い」と直接衝突)。ホスト側は「仕組みは変えないので衝突なし」と誤判定していた。core/ADR-0032 (proposed、amends 0030) を起票し、proposal の Why / Impact に反映 |
| 2 | M 級の利用者可視 UI 変更なのに `ui/` ゲートを省略している (Major) | 降格 (オーナー判断 2026-09-17) | 規約上は相方の指摘どおり (UI に触れる変更は級を問わず `ui/` が必須で、省略規定は S 級の微調整のみ)。ホスト側は「見た目として新たに決める要素が無い」として省略した。オーナーに諮り、モックは作らず規約からの逸脱として proposal の Impact に記録すると確定 (選ぶ見た目が無く、実機証跡で確認できるため) |
| 3 | MAUI の観察可能な変更がデルタスペックから欠落している (Major) | 採用 | 相方のみ・根拠強 (proposal 自身が MAUI の追随を述べており、lessons L-005 の「受け付ける入力の全種」に当たる)。specs/maui-bridge/spec.md を追加、tasks 4.5 と 5.2 に反映 |
| 4 | Requirement 本文の SHALL に対応する Scenario とテスト計画がない (Major) | 採用 | 相方のみ・根拠強 (該当 SHALL を列挙)。identity を「ID と表示中の行の View インスタンス」、スクロール位置を比較可能な観測値で定義し、両 OS に Scenario を追加。Android の構造更新と同時の Theme 適用、iOS の text → View 差し替え、Context の同一インスタンスも Scenario 化。tasks 1.2 / 1.4 / 2.1 / 3.2 を「全 Scenario」へ改めた |
| 5 | Android の外観切替時のスクロールバー挙動が実装者選択のまま (Major) | 採用 | 相方のみ・根拠強 (tasks 1.5 がどちらでも完了になる)。つまみの追従を SHALL とし Scenario を追加。overscroll 効果の色は platform に差し替え手段が無いため保証の対象外と Requirement 本文に明記 |
| 6 | iOS の「透明な既定色」と等価性契約の関係が曖昧 (Minor) | 採用 | 相方のみ・根拠強 (固定色にすると「明示した透明」が既定と等価になり core/ADR-0030 の等価性契約が変わる)。両外観の値を持つ色の形を保つと決め (core/ADR-0032 Decision 2)、Requirement と Scenario、tasks 4.1 に反映 |

集計: 採用 5 / 降格 1 (指摘 2。オーナー判断) / 未解決 0

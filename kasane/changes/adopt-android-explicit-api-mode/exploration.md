# Exploration: adopt-android-explicit-api-mode

## 課題 / 動機

Android 本体 module (`android/kssettingsview`) は Maven Central へ配る公開ライブラリになった (add-android-maven-distribution) が、Kotlin の Explicit API mode (`explicitApi()`) が未設定で、公開/内部の境界がコンパイラで強制されない。

発見の文脈: add-android-maven-distribution の独立レビュー (review-001 Suggestion)。同レビューの Major — 公開 data class `Theme` の `sectionMargin: PaddingValues?` が露出する外部型の依存 (`androidx.compose.foundation:foundation-layout`) が `api` になっていなかった — は、発行 aar を javap で全走査して「どの宣言が public か」を手作業で判定して初めて検出できた。recyclerview の `api` 漏れも同型 (spec の列挙が公開面の走査を経ていなかった — lessons/inbox/principle-with-enumeration-not-swept-against-public-surface.md に捕捉済み)。公開 API 面が宣言ベースで機械可読になっていれば、依存スコープ設計 (design Decision 6 の原理「公開 ABI に露出する外部型の依存は `api`」) の適用判定と意図しない API 公開の検出をコンパイラに委ねられる。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

未探索 (簡易起票)。分かっている疑問点:

- 有効化モードの選択: `Strict` (エラー) か `Warning` (警告) で始めるか
- 有効化に伴う棚卸し: 現在暗黙 public の宣言のうち、意図せず public のもの (本来 `internal` に落とすべきもの) がどれだけあるか。`internal` への降格は初回リリース後だと破壊的変更になるため、実施タイミングは初回リリース前が安い
- 対象範囲: 本体 module のみか、`kssettingsview-bridge` (非公開 module) も含めるか
- phase-7 (消費者検証) との連携: API 面の棚卸しと消費者視点の検証は同じ問いを別角度から見る作業のため、併せて実施すると判断材料が揃う (review-001 の推奨)。逆順 (phase-7 後の API 面変更) は検証やり直しを招く

## UI 素材 (ui/references/ の一覧と注釈)

なし (UI に触れない)。

## 変更級の推奨: 未判定

暫定の見立て: 設定自体は 1 行だが、全公開宣言への `public` 修飾子・戻り値型の明示と public/internal の棚卸し判断を伴うため S では収まらない可能性が高い。判定は探索時に diff 規模を実測してから。

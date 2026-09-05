# Exploration: ios-effectivestyle-visibility

## 課題 / 動機

iOS スキルの公開 API 網羅性調査 (2026-08-29、ksn-scout による `ios/` 公開面と `skills/*/kssettingsview-ios/` の突き合わせ) の副産物。`EffectiveStyle` (`ios/Sources/KsSettingsViewUI/EffectiveStyle.swift`) とその public フィールド・解決メソッド一式は、描画内部ユーティリティとして意図された public 宣言に見える。利用者ドキュメントに載せる API ではなく、`internal` 化 (アクセスレベルの引き下げ) を検討すべき候補。

## 現状の把握 (2026-09-05 探索)

- 参照範囲: `EffectiveStyle` を使うのは `ios/Sources/KsSettingsViewUI/` 配下の各 CellView・`KsCellViewSupport`・`CellBaseLayout`・`KsSettingsViewController`・`Theme` と、`ios/Tests/KsSettingsViewUITests/` の 6 ファイルだけ。`samples/`・`ios/Sources/KsSettingsViewBridge`・`maui/` には参照なし
- テスト: UI テストはすべて `@testable import KsSettingsViewUI` で書かれており、internal 化しても変更不要
- 利用者向け拡張境界: 独自 Renderer は `KsCellRenderer.render(cell:theme:)` で Theme だけを受け取る。利用者ドキュメント (`skills/*/kssettingsview-ios/references/custom-cells.md`) の例も `Theme` の公開既定値 (`Theme.defaultCellTitleColor` 等) から手で解決しており、`EffectiveStyle` を前提にした利用者経路は存在しない。`EffectiveStyle` を受け取る内部ヘルパ (`KsCellViewSupport.applyEffectiveHeight` / `applyCellBaseLayout`) はいずれも既に internal
- Android との対称性: Android の同名クラス (`android/kssettingsview/.../ui/EffectiveStyle.kt`) は既に `internal data class`。iOS を internal 化すると両 platform で揃う
- Android 側の可視性候補 (起票時の未決論点): 2026-09-01 の adopt-android-explicit-api-mode で判断済み (`SettingsRootStore.preview` / `KsCellRegistry.viewTypeOf` / `isRegistered` は internal 化、一括登録 API と `CustomCellEmptyContent` は公開維持)。同探索で「ios-effectivestyle-visibility は iOS 限定のまま維持する」と合意済み

## 検討した選択肢 (却下案と理由を含む)

| 案 | 内容 | 評価 |
|---|---|---|
| A: internal 化 | `EffectiveStyle` とその static 解決メソッド・プロパティを internal に降格し、handbook の除外リストから外す | **採用**。参照が本体内に閉じ、初回リリース前で互換性の問題がなく、Android と揃う |
| B: 公開 API として正式化 | Renderer 作者向けの解決ユーティリティとして skills / concepts の Renderer 拡張境界に掲載する | 却下。`render(cell:theme:)` は Theme しか渡さず、既存の利用者経路・ドキュメント例が `EffectiveStyle` を必要としていない。Android (internal) との非対称も生む。要望が出た時点で改めて起票する |
| C: 現状維持で close | public のまま除外リストの表記だけ見直す | 却下。「可視性引き下げ候補」という宙に浮いた分類が残り、Android との非対称も解消しない |

## 決定事項

- スキル網羅性対応のスコープからは除外し、可視性の検討を本 change として独立起票する (2026-08-29 ユーザー合意)
- `EffectiveStyle` (型・static 解決メソッド・プロパティ・`minRowHeight`) を internal 化する (2026-09-05 ユーザー合意、案 A)
- 付随修正: `kasane/handbook/cross/user-skill-api-listing.md` の iOS 除外リストから `EffectiveStyle` の行を削除する (adopt-android-explicit-api-mode の Android 3 API と同じ扱い)
- 本 change は iOS 限定のまま。Android の可視性候補は adopt-android-explicit-api-mode で判断済みのため扱わない

## ADR 候補 (作成済み: なし / 未起票: なし)

ADR 不要と判断。1 型のアクセスレベル変更は覆すコストが低く (初回リリース前)、platform 境界を越えず、将来の決定も制約しない。styling を UI 層の Native 型で表す core/ADR-0009 とは矛盾しない。iOS には Android の android/ADR-0022 (Explicit API Strict) に相当する公開境界の強制機構がないが、Swift は public を明示する言語仕様のため同種の ADR は不要。

## 未決の論点

なし

## UI 素材 (ui/references/ の一覧と注釈)

なし (見た目は変わらない)

## 変更級の推奨: S

理由: 触るのは `EffectiveStyle.swift` のアクセス修飾子と handbook の除外リスト 1 行だけ。公開 API の縮小のみで挙動・見た目は不変、テストは `@testable` のため変更不要。デルタスペック不要で、Plan モード + 既存テスト (ios の `swift test`) の通過で完了とする。package-distribution の消費者検証 (phase-7) より前に iOS の公開面を確定しておく。

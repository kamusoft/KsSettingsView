# Exploration: ios-effectivestyle-visibility

## 課題 / 動機

iOS スキルの公開 API 網羅性調査 (2026-08-29、ksn-scout による `ios/` 公開面と `skills/*/kssettingsview-ios/` の突き合わせ) の副産物。`EffectiveStyle` (`ios/Sources/KsSettingsViewUI/EffectiveStyle.swift`) とその public フィールド・解決メソッド一式は、描画内部ユーティリティとして意図された public 宣言に見える。利用者ドキュメントに載せる API ではなく、`internal` 化 (アクセスレベルの引き下げ) を検討すべき候補。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

- スキル網羅性対応のスコープからは除外し、可視性の検討を本 change として独立起票する (2026-08-29 ユーザー合意)

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- `EffectiveStyle` を internal 化して問題ないか (外部・サンプル・テストからの参照有無の確認が必要)
- internal 化しない場合、利用者向けドキュメントに載せるべきか
- Android 側にも同種の visibility 引き下げ候補あり (skills-api-coverage の Android 調査 2026-08-29 より): `KsCellRegistry.viewTypeOf` / `isRegistered`、`KsCellRegistry.registerBasicCells` / `registerInputCells` / `registerCustomCell` (View 初期化時に自動登録。ただし KDoc は利用者からの明示呼び出しも示唆 — 正式 API とするならドキュメント漏れ側)、`SettingsRootStore.preview` (Preview/Test 用)、`CustomCellEmptyContent`。本 change を cross-platform の可視性棚卸しに広げるか、iOS 限定に留めるかは探索時に判断

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (参照箇所が本体内に閉じていれば S 級の見込み)

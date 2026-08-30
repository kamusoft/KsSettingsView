# Exploration: unify-modern-default-corner-radius

## 課題 / 動機

サンプル「Section 装飾デモ (style 切替)」で、Modern の「既定」プリセットが Android だけ角丸が明らかに小さく見える (iOS の「角丸小」プリセット相当)。

調査の結果、バグ (単位換算ミス等) ではなく**ライブラリ既定値そのものの非対称**が原因と特定:

- iOS: `ios/Sources/KsSettingsViewUI/SectionBoxMetrics.swift:32` — `modernDefaultCornerRadius: CGFloat = 26`
- Android: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionBoxMetrics.kt:63` — `MODERN_DEFAULT_CORNER_RADIUS = 12.dp`
- 「角丸小」プリセットは3プラットフォームとも 8 を渡すため、Android 既定 12dp が「角丸小」に見えるのは妥当な体感
- dp→px 換算は `SectionBoxMetrics.kt` の `px()` 一箇所のみで二重換算・未換算なし
- サンプルの「既定」プリセットは3プラットフォームとも4属性すべて未指定 (null)
- MAUI は既定を持たず null をそのまま native へ委譲するため、native 既定に落ちる

この非対称は concepts (core/styling/list-appearance.md) に「Modern の既定寸法は各 platform が所有し、platform 間で同じ生値に揃えない」と仕様として明記済みだった (対応 ADR は無し。コミットの ADR-0023/0024 言及は maui ドメインの別件)。

## 検討した選択肢 (却下案と理由を含む)

1. **現状維持** — 却下。単一 platform 利用者は良いが、MAUI / KMP のクロスプラットフォーム利用者は同じ未指定 Theme で OS 間の見た目が大きく違い混乱する (ユーザー判断)
2. **Android 既定を 26 へ引き上げ、角丸のみ統一** — ★採用。26 は Modern の設計原点 (iOS 標準設定画面) 由来の値で、M3 extra-large shape 28dp にも近い
3. **サンプル側のみ調整** — 却下。「既定」プリセットの名が実態と乖離する
4. **中間値 (16〜20) へ両 OS 変更** — 却下。iOS 利用者まで視覚変更に巻き込み、どちらの OS 標準からも外れる
5. **角丸+margin 全既定統一** — 却下。iOS の margin (top22/bottom0) は iOS 標準準拠。隣接 Section 間隔は iOS 22pt / Android 24dp で既にほぼ同等

## 決定事項

- Modern の既定角丸を両 platform で生値 26 に統一する (Android `MODERN_DEFAULT_CORNER_RADIUS` 12dp→26dp、iOS 無変更)
- margin の既定は引き続き platform 所有 (揃えない)
- 利用者明示値の扱いは従来どおり

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

- 作成済み: core/ADR-0024 (status: accepted、2026-08-20 ユーザー承認済み)

## 未決の論点

- なし (実装時の注意のみ下記)
- 実装対象: `SectionBoxMetrics.kt:63` の定数、`SectionBoxMetricsTest.kt:43` の「既定 12dp」固定テスト、concepts/core/styling/list-appearance.md の既定値記述と「してはいけないこと」の統一禁止規定 (角丸のみ例外化)
- 実装コード該当箇所へ `ADR-0024` コメントを残す (decisions 規約)

## UI 素材 (ui/references/ の一覧と注釈)

- `ios-modern-wide-margin-small-radius.png` — iOS ネイティブサンプル、Modern +「余白広め・角丸小」プリセット表示。比較用
- `android-modern-default.png` — Android サンプル、Modern +「既定」プリセット表示。角丸が iOS の「角丸小」相当に見える問題の実写
- `android-modern-default-after-26dp.png` — 修正後 (26dp) の実機確認スクショ (Pixel 実機、2026-08-20)

## 変更級の推奨: S (理由)

触る能力は styling 1つ、公開 API 変更なし (既定値の視覚挙動のみ変更)、定数1つ+テスト1件+concepts 追随で完結し可逆。UI 変更だが新規レイアウトではなく既存パラメータの値変更のためモック不要、実機/エミュのスクショ確認で足りる。

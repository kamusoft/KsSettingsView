# deviation: ios-effectivestyle-visibility

S 級 (デルタスペックなし)。合意済みスコープは exploration.md の「決定事項」。

- [付随修正] `ios/Sources/KsSettingsViewUI/KsCellRenderer.swift` の公開 protocol `KsCellRenderer` のドキュメントコメント: 「`render` 実装は cellStyle 合成 (`EffectiveStyle`) → サブビュー反映の順で行う」が、internal 化により利用者から参照できない型を案内する記述になったため、「`theme` と Cell 個別のスタイルから描画値を解決してから反映する。組み込み Cell の合成はライブラリ内部で閉じ、利用者定義 Renderer は `Theme` の公開既定値から自前で解決する」に書き換えた。本 change が直接原因で生じた不整合であり、同一ファイル群の数行で閉じる (2026-09-05)
- [付随修正] `ios/Sources/KsSettingsViewUI/Theme.swift` の公開型 `Theme` の doc コメント 3 箇所 (型 doc・`defaultButtonTitleColor`・`defaultHeaderFooterFont`) と区切りコメント 1 箇所 (`// MARK:`) が internal 化後の `EffectiveStyle` を名指ししていたため、上と同じ方針で解決順序・意味の記述へ書き換えた (review-001 Major の対応、2026-09-05)

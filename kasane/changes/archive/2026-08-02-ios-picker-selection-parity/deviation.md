# Deviation: ios-picker-selection-parity

- 既知制約 (spec 沈黙領域、未検証): 選択面のナビバー appearance は `viewDidLoad` 時点の「現在有効な appearance」を複製してタイトル文字色のみ差し替える。`UIAppearance` プロキシ経由のナビバーカスタマイズはプロキシ適用タイミング (window 追加時) の関係でこの選択面に反映されない恐れがあるが、実害は未検証。対応は `viewWillAppear` への移動ではなく、実装コメントの保証範囲を「現在有効な appearance の引き継ぎ」に狭める形に留めた (review-002 Minor への裁定、2026-08-02)

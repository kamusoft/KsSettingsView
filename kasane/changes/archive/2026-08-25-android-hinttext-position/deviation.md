# Deviation: android-hinttext-position

- [付随修正] ButtonCellViewHolder buttonStyleSet: root の padding 廃止により、ボタンスタイル行 (contentRow を parent へ直接続) が行余白を失う回帰が出るため、同じ余白 (横16dp / 縦4dp) をマージンとして付与。本変更 (余白のマージン再配分) が直接原因・同一能力・公開 API 不変・回帰テスト追加済み。(2026-08-24)
- [隣接修正] ButtonCell ボタンスタイルの光学中心補正: buttonStyleSet が contentRow の translationY を 0 に戻していたため、光学中心補正 (-1dp、android/ADR-0004) をボタンスタイルにも適用 (review-001 Suggestion-2 起因)。本変更前と比べボタンスタイル行の本体行が 1dp 上へ動く利用者可視の変更。(2026-08-24)
- [隣接修正] hintTextView の左端ガード: iOS の hintLabel は `leading >= cell.leading + 16` を持つが Android には無く、長い hint が title 側へ無制限に伸び得る。同じ hintTextView の制約群への数行追加で閉じるため同 change で対応 (START>=parent+16 のクランプ + 既存の末尾省略)。iOS のフォント自動縮小 (adjustsFontSizeToFitWidth) 相当は導入せず、極端に長い hint の挙動は「縮小 (iOS) vs 末尾省略 (Android)」の差として残る。(2026-08-24)

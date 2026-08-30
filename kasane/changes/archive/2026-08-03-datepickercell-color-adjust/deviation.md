# Deviation: datepickercell-color-adjust

- Requirement「DatePickerCell の日付選択ダイアログはテーマ配色を反映する」/ ui/brief.md 部位対応表 (通常日・選択日・今日の別ロール): spec と部位対応表は構成を限定せず部位ごとのロール適用を規定 → 指示により、ホストアプリが material の calendar style (materialCalendarDay 等) へ可視の塗り/枠をカスタム指定した構成に限り、通常セルを選択/今日と初回誤分類しうる挙動を「合意済みの制約」として受容。既定 Material 構成では発生せず、フレーム間で振動しないことはテストで固定済み。理由: material が状態を非公開 (`isSelected` も `AbsListView.setupChild()` で潰れることを統合テストで実証) で、android/ADR-0008 の走査方式内に描画非依存の代替が無いため (2026-08-02)
- ui/mock/approved.png (A 案ヘッダ): mock ではタイトルと選択日テキストが近接 → 実装は Material 標準ヘッダ高さ (120dp) の本来のベースライン位置を復元するため間隔が mock より広い。理由: A 案の決定「ヘッダのコンテナ高さには手を入れない」に沿った標準配置としてオーナー承認 (2026-08-02)

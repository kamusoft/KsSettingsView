# Deviation: fix-android-accessory-header-refresh

- full 更新経路の内容通知の範囲: ADR-0012 Decision 2 (補正後) の「旧∩新 visible の cell id だけへ発行」を**通知対象の上限**として解釈し、実装は「旧∩新 かつ Cell の値 (equals) が変化したもの」のみへ通知する。デルタスペックの SHALL (「内容が変わった場合に反映」「新規・削除へ通知しない」) には適合。既存契約 2 件 (再 attach 時の差分通知ゼロ / Bridge の header 不変 replaceSection の内容通知ゼロ) を維持するため。CustomCell の builder のみ差し替えが通知対象外となるのは core/ADR-0014 の既存利用者契約どおりで、本実装による新たな穴ではない。理由: 破壊的変更の回避 (ユーザー承認 2026-08-05。ADR-0012 に解釈の注記を追記済み)

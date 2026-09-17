# deviation: settingsview-migration-defects

- 論点 1 (暗黙 Style): 決定事項では「controller と binder の遅延生成を主案」→ 実装もその形。副作用として `RootHeaderView` / `RootFooterView` の多重配置検査が構築より前 (暗黙 Style の Setter 適用時) にも走るようになり、旧 doc の「構築前に来た値は検査しない」記述は削除した。理由: 遅延生成で検査が常に可能になり、既配置 View の二重配置を早期に例外にする方が契約として一貫する (2026-09-15)
- 論点 2 の解消確認: 決定事項では「修正前と同じ導線 (ColorAnalyzer の通常遷移)」→ 実際は MAUI Sample に一時的な子ページ Push 導線を足して確認 (verification.md)。理由: ColorAnalyzer は NuGet 0.1.0-beta.2 参照で、修正した Native を取り込むには別リポジトリのビルド構成変更が要る。同じ性質の導線 (Push して戻る) で A/B が成立している (2026-09-15)

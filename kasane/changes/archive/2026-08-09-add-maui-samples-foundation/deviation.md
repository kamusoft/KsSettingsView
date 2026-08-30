# Deviation: add-maui-samples-foundation

- サンプル csproj の MAUI パッケージ宣言: spec は沈黙 (デルタスペックにパッケージ構成の規定なし)。当初実装は MauiHost 踏襲で `Microsoft.Maui.Controls` を未宣言 (推移参照依存、MA002 警告あり) としていた → オーナー指示により `<MauiVersion>` プロパティ + `Microsoft.Maui.Controls` / `Microsoft.Maui.Controls.Compatibility` の明示宣言を追加 (ColorAnalyzer.csproj と同パターン)。理由: MAUI アプリのテンプレートとして必須 (2026-08-09)
  - バージョン値の判断: オーナー例示は 10.0.90 だったが、いったん `KsSettingsView.Maui` の既存ピンおよび MA002 が要求する SDK バンドル版と整合する 10.0.1 を採用 (参照元 ColorAnalyzer の実値も変数 `AppMauiVersion` = 10.0.70 であり、数値はパターン例示と解釈) → オーナー指示「本体と合わせて 10.0.70 に」により **10.0.70 へ統一** (2026-08-09)
- Microsoft.Maui.Controls の 10.0.70 統一: spec は沈黙 (MAUI 本体バージョンの規定なし)。オーナー指示により `KsSettingsView.Maui` / `KsSettingsView.Maui.Tests` / `KsSettingsView.Sample.Maui` のピンを 10.0.1 → 10.0.70 へ更新。理由: 10.0.1 は古すぎるため本体と合わせて更新 (2026-08-09)
  - 随伴修正: `KsSettingsView.MauiHost` にも `Microsoft.Maui.Controls 10.0.70` を明示宣言。未宣言のままだと SDK 暗黙参照 (バンドル版 10.0.1) が本体の 10.0.70 と衝突して NU1605 (ダウングレード) で restore が失敗するため、10.0.70 統一の成立に必須。副作用として MauiHost の MA002 警告も解消

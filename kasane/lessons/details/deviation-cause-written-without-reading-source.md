# L-004 (impl) の経緯: 推測の原因分析が記録に固定されかけた 3 件

pain 3 件 (2026-08-01 〜 2026-08-19) で昇格。deviation・証跡・完了報告に書かれた「制約でできない」「環境要因」の原因分析が、いずれも裏取りなしの推測で、後から事実誤認と判明した。

- 2026-08-01 align-sample-parity: deviation.md の原因分析 2 箇所が本体コード未確認の推測で、独立レビュー review-001 / review-002 が両方とも事実誤認として指摘。
  1. Android の `DatePickerUIStyle.Spinner` がカレンダー表示になる件を「Material3 テーマ配下では `calendarViewShown` が無効化され Sample 側から強制できない」と記録したが、実際は `android.widget.DatePicker` のモードが `android:datePickerMode` スタイル属性でしか決まらず `calendarViewShown` はモード切替の手段ではない (= 本体の実装バグ)。Material3 は無関係。
  2. ButtonCell の既定 titleColor を「iOS は `tintColor` 由来の `#007AFF`」と記録したが、実際は `Theme.defaultButtonTitleColor = .systemBlue` (`#0088FF`)。`#007AFF` は別定数 `Theme.defaultAccentColor` の値との取り違え。
- 2026-08-05 add-maui-native-bridge: Binding csproj を proposal 記載の標準アイテム形式ではなく Exec 方式で実装した理由「SDK 制約により標準アイテムが使えない」が検証なしの推測。second-opinion の無記録逸脱指摘を機に ksn-dual-research の実測実験で iOS 側は反証され (SDK と同一引数で archive 成功。当初失敗の真因は scheme 衝突の可能性が高い)、標準 XcodeProject 方式へ復帰。Android 側の制約 (init script の buildDirectory 束ね) だけが実在した。誤った制約主張がコードコメント・README にも断定形で残り、review-002 で追加修正が必要になった。
- 2026-08-19 add-accessory-visibility-toggle: MAUI サンプルの net10.0-ios ビルド失敗を、エラーメッセージの額面どおり「.NET for iOS と Xcode 26.5 の不一致という環境要因」と断定して evidence README・review-001 の対応記録・verify-001 の観察事項・完了報告まで連鎖して固定されかけた。オーナーが「Xcode 26.5 対応済みで他プロジェクトでは動く」と反証し、実測で真因はリポジトリに global.json が無く親ディレクトリの古い workloadVersion ピン (10.0.101 → iOS SDK 26.1) を継承していたことと判明 — 動作している兄弟プロジェクト (KsApp / ColorAnalyzer) は自前 global.json で 10.0.300.3 → iOS SDK 26.5 を解決していた。差分の照合先 (同一マシンで動く構成) が存在したのに突き合わせず、反証後の照合では 10 分で真因に到達した。リポジトリ直下への global.json 追加で解消。

deviation は蒸留で長命層 (本体の後続課題) へ引き継がれるため、誤った原因分析は本体の修正方針を誤誘導する。書く時点のコストは「該当ファイルを 1 つ開く」「兄弟構成と diff を取る」程度で済む。

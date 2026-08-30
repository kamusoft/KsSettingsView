# レビュー申し送り (add-maui-basic-input-cells / orchestrator 蓄積分)

レビュアーへ渡す確認観点 (実装者の経緯報告ではなく、orchestrator が採否判断した論点の検証依頼):

1. **BG8605 警告3件の容認判断** (Android binding): `KsBridgeValueTransport` 内部ヘルパの enum 戻り値が class-parse で解決不可。公開 DTO 面は全メンバー生成済みを実物確認済み。~~解消の代償 (変換ロジック3重複製) が大きく見送り~~
   **[実測 2026-08-11]** 判断根拠を差し替える。`datePickerUIStyle` / `titleAlignment` / `selectionMode` に `@JvmSynthetic` を付けて aar を再生成し (`javap -v` で 3 メソッドに `ACC_SYNTHETIC` が付くことを確認)、Binding を `obj`/`bin` 削除から再ビルドして比較した結果、**警告の内訳・件数はまったく変わらなかった** (付与前後とも BG8605 16 / BG8606 4 / BG8A00 4 の出現、`java-resolution-report.log` の 3 行も同一)。class-parse はメンバーの signature を classpath に対して解決した時点で診断を出すため、synthetic 化しても抑止されない。根本原因は `ks-settingsview-core` / `ks-settingsview-ui` の aar が `Bind="false"` で解決集合に入らないことであり、メソッド側の可視性では解けない。効果のない注釈を残さないため付与は取り消し、容認を継続する
2. **BG8606 / BG8A00 警告** (~~facade 前半ワーカーの発見・変更由来ではない~~): `Metadata.xml` の `remove-node` が `KsSettingsBridge.WhenMappings` に一致しない一方、`KsBridgePickerCell.WhenMappings` が public 束縛面に漏れている。実害なしだが束縛面の汚れとして要評価
   **[訂正 2026-08-11]** 「変更由来ではない」は誤り。`KsBridgePickerCell.kt` は本変更の新規ファイルであり、その `WhenMappings` の漏れは本変更由来。対処として `makeCell` の enum `when` を `if` へ書き換え、合成クラスを生成させないようにした。再ビルド後の `classes.jar` に `KsBridgePickerCell$WhenMappings.class` は存在せず、`api.xml` / `api.xml.class-parse` に残る `WhenMappings` は `KsSettingsBridge` の 1 件のみであることを実測確認。`KsSettingsBridge.WhenMappings` 側 (既存) の `remove-node` と、それが generator 段で出す BG8A00 は Metadata.xml の説明どおりで変更なし
3. **spec 沈黙箇所の安全側判断**: Native → C# の時刻/日付通知が輸送書式として解釈不能な場合、facade は通知を捨てる (現在値を壊さない)。spec が既定値化を定めるのは C# → Native 方向のみ。妥当性を確認してほしい (deviation 記録はしていない — 既存挙動の変更ではなく新規経路の無規定領域のため)
4. **AndroidButtonColor の platform 固有無視が net10.0 テストで固定できていない**: iOS gateway が送らないことは platform TFM コードでしか表現されず、ビルド通過と目視のみ。テスト戦略として許容できるか
5. **uiStyle 未指定時の native 既定の非対称** (iOS Wheels / Android Material): spec の「未指定時は native 既定」に従った意図的な非対称 — spec 解釈として正しいか
6. **DTO 輸送面は native Cell struct 全件を正とした** (inventory 列挙は CellBase/LabelCell 止まりのため)。`Section.headerHeight` は対象外として未輸送
7. **`ValueText` の配り方**: ~~Switch / Checkbox / Radio / SimpleCheck / Button には付けていない~~ **[訂正 2026-08-11]** この記述は facade 前半時点のもので、追補 (B) により5種すべてに `ValueText` を公開済み (spec「native の対応 Cell と同じ状態フィールド」の読み直しによる)。実装は #15 の配置判断と整合
8. **`CanExecuteChanged` 追随を `OnPropertyChanged(nameof(IsEnabled))` で通知** (値は不変だが行の有効表示が変わる信号)
9. **9.1 Suggestion 6件全却下** (根拠: デルタスペック対応 Requirement なし・元レビューも後続/蒸留分類)。R1-S2 のみ facade 後半 4.13 で適用可否を再判断

10. **DataTemplateSelector の例外契約**: selector が selector を返すと MAUI 内部が先に `NotSupportedException` を投げ facade から介入不能 (spec は InvalidOperationException 系)。null 返却のみ自前で `InvalidOperationException`。spec 解釈として妥当か
11. **`ValueText*` スタイルの置き場**: AiForms は LabelCell 側だが native CellStyle が基底に持つため CellBase に配置 (inventory と異なる)。spec の「CellBase は CellStyle に対応するスタイルプロパティを公開」に従った判断
12. **tasks 5.1 の「style / icon 含む」**: ConversionPathTests ではなく ThemeAndCellStyleTests / IconSourceTests で担保 (前半時点で style/icon 未実装だったため)。カバレッジの実体を確認してほしい
13. **R1-S2 を適用** (facade 後半): `_generated` 更新を `target.Insert` より前へ。再現→修正→回帰テスト (戻すと落ちることを確認) の手順を踏んでいる

14. **Section.HeaderHeight 追補 (deviation.md 記録済み)**: 実行時変更を silent no-op にしないため dirty-tracking (`_visibilityDirtySections` → `_replacePendingSections` 改名) に載せた。deviation 記述「輸送を追加」より一歩踏み込んだ実装 — 妥当性を確認してほしい
15. **ValueText の配置は各 Cell 個別** (CellBase 昇格せず): EntryCell の TwoWay 特例との同名 BindableProperty 並立回避・既存6種の置き方との一貫性が根拠
16. **samples iOS アプリビルドは `DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer` 必須** (Xcode 26.5 既定と .NET for iOS 26.1 の不整合。環境要因でありコード問題ではない — orchestrator が実測確認済み)

17. **Xamarin.Google.Android.Material を 1.12.0.5 に固定** (Binding csproj、視覚照合で検出したクラッシュの修正): 1.14.0.6 は aar の R.txt が非推移的で `colorPrimary` 欠落 → 実行時 NoSuchFieldError。1.12.0.5 は Gradle 解決の material 1.12.0 と同系列かつ Microsoft.Maui.Core 10.0.70 の要求版と一致。版選定と根拠コメントの妥当性を確認してほしい。native 層の恒久対策 (宣言元 androidx.appcompat.R への参照変更) は別課題として切り出し済み
18. **サンプル App.cs に PrefersLargeTitles 追加** (orchestrator 直接修正・オーナー指示): iOS ナビバーを native の Large Title と揃える1行。完全修飾呼び出しの形
19. **アイコンの色・形状差はオーナー裁定で許容** (deviation.md 記録済み): サイズ感一致が条件

## 実機確認 (グループ7) への追加観点
- IconSource の完了コールバックは MAUI main SynchronizationContext 依存 (net10.0 テストでは fake が同期完了のため未検証)。実機でアイコン表示を確認する

## 蒸留への申し送り (レビューではない)
- **KsImageLease の受容済みエッジ2件** (review-002 保留評価より): (1) ページを恒久的に離れて再訪問しない場合、リースにファイナライザが無いため画像ローダーの後片付けは走らないまま GC される (ADR-0007 の復元契約優先の帰結・修正前は破棄自体が無かったので後退ではない) (2) 画像サービスが同一 platform 画像インスタンスをキャッシュ返却した場合、旧リース破棄が共有画像に影響し得る理論的余地 (file/font 経路は破棄 no-op で実害なし、ローダー経路のキャッシュ挙動依存の低確率)。いずれも受容済み — concepts (maui アーキテクチャ) への注記候補。関連: `StoreIcon` が前後同一画像インスタンスを検出した場合は dirty 化されず、その回の旧リースは次の flush / 構造変更まで `_retiredIcons` 待ち行列に残る (破棄が遅れる方向のみで表示への害なし・同根の受容挙動)
- R1-S4: test-execution.md への MAUI 節追加は ksn-distill / docs-refresh の責務。必要な事実は確定済み: `dotnet test maui/KsSettingsView.Maui.Tests -f net10.0`、3 TFM ビルド (iOS は DEVELOPER_DIR 指定要)
- Decision 7 (IconSource 実体化方式) は ADR 候補 (design.md 記載)

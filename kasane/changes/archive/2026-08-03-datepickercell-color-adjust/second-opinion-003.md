# セカンドオピニオン: datepickercell-color-adjust (code-review 2 周目、review-002 対応)
**相方**: codex (同一セッション継続 turn 2) / **日付**: 2026-08-02 / **対象**: 修正サイクル 1 周目後の作業ツリー全変更 (HEAD 22a20fd 比)

---

ホスト側の 1,480 件成功・lint 指摘 0 を確認済み証拠として扱い、静的レビューのみ実施しました。

| 前回指摘 | 判定 | 現在の扱い |
|---|---|---|
| Major: 描画色による状態判定 | **部分解消** | Major 残存 |
| Major: 実ダイアログ統合・契約テスト不足 | **部分解消** | 残存は Suggestion |
| Minor: brief への実装寸法混在 | **解消** | 指摘終了 |

### Major — 描画色による状態判定: 部分解消

該当箇所: [DatePickerColorizer.kt:380](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:380)、[impl-notes.md:55](kasane/changes/datepickercell-color-adjust/impl-notes.md:55)

解消した内容:

- Drawable identity ごとの `WeakHashMap` キャッシュにより、自分で適用した色を次回判定に使わなくなっています。
- 透明アクセントによる「選択→通常→選択」の振動は解消されています。
- 日付・年セル双方の透明アクセント回帰テストも追加されています。

残存内容:

- 新規 Drawable の初回ロール判定は、依然として fill/stroke の可視性です。[DatePickerColorizer.kt:462](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:462)
- 通常項目に可視 fill を持たせると選択、可視 stroke を持たせると今日に誤分類されます。コード自身も既知の限界として認識しています。[DatePickerColorizer.kt:446](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:446)
- 追加テストも誤分類を防ぐのではなく、誤分類後に振動しないことを固定しています。[DatePickerColorizerTest.kt:321](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizerTest.kt:321)
- 部位対応表では通常日・選択日・今日を別ロールとして規定しており、この限界を許容する `deviation.md` はありません。KDoc・impl-notes への記録だけでは仕様乖離の承認にはなりません。

問題点: ホスト側 Material Calendar style によって、多数の通常日が選択状態に見える可能性があります。状態の意味を変えるため、Major を維持します。

推奨修正: 実 MaterialDatePicker の選択値と、GridView の adapter item／位置などから日付・年を特定し、描画色とは独立して選択・今日・通常を決めてください。限界を意図的に受容する場合は、デルタスペックの適用範囲変更または正式な deviation 承認が必要です。

### Suggestion — 実ダイアログ統合・契約テスト不足: 部分解消

該当箇所: [DatePickerDialogIntegrationTest.kt:65](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerDialogIntegrationTest.kt:65)、[DatePickerMaterialContractTest.kt:58](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerMaterialContractTest.kt:58)

解消した内容:

- 実 `FragmentActivity` 上で `MaterialDatePicker` を表示し、`attach()`、Fragment tag、lifecycle callback、pre-draw、window 背景の実経路を通しています。
- 実階層でヘッダ、操作ボタン、日付セル、再選択、テキスト入力モード切替を検証しています。
- 契約テストは `MaterialIds` の全 ID、型、主要な親子関係まで拡張されています。

残存内容:

- 実ダイアログ上で月送りボタンを操作するテストと、年選択を開いて年を選ぶテストはありません。デルタスペックの「カレンダー操作後」シナリオは、合成テスト・契約テスト・実機スクリーンショットによる分担に留まっています。

推奨修正: 将来の回帰検出力を高めるなら、実ダイアログ統合テストに月移動と年選択操作を追加してください。現時点では、前回の Major の核心は解消しており、ブロッキングとはしません。

### Minor — 新規: 状態判定根拠についてテスト実態を過大記載している

該当箇所: [DatePickerColorizer.kt:437](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:437)、[impl-notes.md:66](kasane/changes/datepickercell-color-adjust/impl-notes.md:66)

問題点: KDoc と impl-notes は「実ダイアログの統合テストで、選択日を含む全セルが `isSelected == false` と確認」と記載していますが、現在の `DatePickerDialogIntegrationTest` に `isSelected` の assertion はありません。設計選択の主要根拠が自動テストで固定されているように読めますが、実際には固定されていません。

推奨修正: 実ダイアログの日付セルについて `none { it.isSelected }` を明示的に検証するか、記述を「実機スパイクで確認」に修正してください。

### Minor — brief への実装寸法混在: 解消

実測 px、dp、縮小率などは [impl-notes.md:11](kasane/changes/datepickercell-color-adjust/impl-notes.md:11) に分離されています。`ui/brief.md` はデザイン判断・部位対応・照合結果を保持し、具体値は impl-notes へ委譲しているため、前回指摘は解消です。

総合件数: Critical 0 / Major 1 / Minor 1 / Suggestion 1

## 総合判定: CHANGES_REQUESTED

透明アクセントの振動とテスト基盤は大きく改善していますが、通常セルの fill/stroke を状態として誤認する仕様不整合が、正式な deviation なしで残っています。

## 突き合わせ結果 (ksn-orchestrator、2026-08-02)

ホスト側 review-002.md (APPROVED / 低優先 Minor 2・Suggestion 2) との突き合わせ。

| 論点 | ホスト | 相方 | 採否 |
|---|---|---|---|
| C1 残存 (カスタム calendar style での初回誤分類) | 受容可 — Scenario の GIVEN (Theme 3 色) の外・THEN 非違反・方式内に代替なし・deviation 不要 | Major 維持 — 部位対応表は通常日/選択日/今日を別ロールと規定。受容には deviation 記録か spec 適用範囲変更が必要。技術的には selection 値 + adapter 位置での状態導出を提案 | **未解決 → NEEDS_DISCUSSION** (2 周連続残存の収束シグナルにも該当。オーナーへ) |
| isSelected 確認の過大記載 (KDoc / impl-notes にテスト固定と読める記述、実テストに assertion なし) | Minor (低) | Minor (新規) | **確定** (双方一致、Minor) — 修正対象 |
| 実ダイアログでの月送り・年選択の操作テスト | — (再指摘なし) | Suggestion (非ブロッキング) | **降格** — 報告に出典付きで残す。修正サイクルは回さない |
| Colorizer KDoc の「契約テストとの取り決め」文言と実態のずれ | Minor (低) | — | ホスト側指摘として修正対象 |
| C2 (統合・契約テスト) / C3 (brief 寸法) / H1 / H2 | 解消 | C2 部分解消 (残存は Suggestion 降格) / C3 解消 | **クローズ** |

- 総合: 実装本体とテスト基盤は双方が改善を認定。残る対立は「既知の限界の受容手続き」1 点のみで、機械的ループを回さずオーナー判断に委ねる

## 未解決論点のクローズ (ksn-orchestrator、2026-08-03)

C1 残存 (カスタム calendar style での初回誤分類) は NEEDS_DISCUSSION としてオーナーに提示し、**「deviation 記録で受容」の決定**を得た (2026-08-02)。相方が受容の条件として挙げた「正式な deviation 承認」を deviation.md への記録で満たしたため、本論点はクローズ。追加実装 (selection 値 + adapter 位置からの状態導出) は不採用。双方一致で確定した Minor (isSelected 記載の過大) は修正サイクル 2 周目でアサーション追加により解消済み。

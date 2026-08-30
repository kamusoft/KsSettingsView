# Exploration: fix-picker-dialog-recreation

**状態: 探索完了 (2026-08-03)。** 復元設計の骨格が確定し、ADR-0011 (proposed) を起票済み。M 級として ksn-propose へのハンドオフ待ち。

## 課題 / 動機

MaterialTimePicker / MaterialDatePicker のダイアログ表示中に Activity が再生成 (画面回転等) されると、ダイアログ自体は FragmentManager の saved state から復元されるが、コードで登録した状態が失われる:

1. `addOnPositiveButtonClickListener` が復元されず、OK を押しても `onValueChanged` が呼ばれない — TimePickerCellViewHolder / DatePickerCellViewHolder 共通の**既存構造問題**
2. timepickercell-color-adjust で導入した `TimePickerColorizer` の着色フック (`FragmentLifecycleCallbacks`) も復元されず、Material 既定配色に戻る
3. DatePickerCell (Material) の todayText ボタンは DatePickerColorizer のフックで注入されるため、再生成後は消える (datepickercell-today-shortcut からの申し送り)

## 出典

- `kasane/changes/archive/2026-08-02-timepickercell-color-adjust/review-001.md` Minor-1 (該当箇所: TimePickerCellViewHolder.kt / TimePickerColorizer.kt。DatePickerCellViewHolder.kt も同型と指摘)
- `kasane/changes/archive/2026-08-02-timepickercell-color-adjust/second-opinion-002.md` 指摘 #2 (相方 codex は Major 評価) とオーナー決定
- datepickercell-today-shortcut の申し送り (「今日」ボタンの復元対象追加、tag の世代サフィックス `.r<n>` への注意)

## 探索で確定した事実 (コード読解による裏取り、2026-08-03)

- **対象範囲は TimePickerCell + DatePickerCell (Material) の2箇所のみ**。ボトムシート系 (PickerCell / NumberPickerCell / DatePickerCell (Spinner)) は素の `BottomSheetDialog` (PickerSelectionSheet.kt:178 等) で、Activity と共に消滅するためゾンビ化しない
- **bind 駆動の再 attach には構造的欠陥が2つ**: (1) 持ち主 Cell が回転後に画面外だと ViewHolder が bind されず取り逃す。(2) Colorizer のフック発火点 (`onFragmentViewCreated` / `onFragmentStarted`) は復元 Fragment の View 生成時に発火済みで、bind 時の再登録では手遅れ
- **Cell の同一性基盤は既に cell.id ベース**: アダプタは `setHasStableIds(true)` + `getItemId`=cell.id の安定ハッシュ (KsSettingsListAdapter.kt)。ただし cell.id の既定値は構築ごとのランダム UUID (TimePickerCell.kt:25)。DSL (Compose) 経路では `withDSLId` で構造由来の安定 ID に rebind されるため再生成をまたいで安定、命令的経路では明示 id が必要
- **KsSettingsView は attach と root 到着の順序が不定な設計**: `pendingStore` + `onAttachedToWindow` リトライが既にある (KsSettingsView.kt:197)。FragmentManager は context からしか解決できないため走査には attach 済みが必要。復元 Fragment 自体は Activity の onCreate で FM に戻っているので attach 以降なら確実に見つかる

## 検討した選択肢 (却下案と理由を含む)

- **A: bind 時の再 attach (review-001 の最小修正案)** — 却下。上記の構造的欠陥2点
- **B: 復元検出して dismiss のみ** — 主経路としては却下 (回転でダイアログが消える UX 劣化の仕様化)。対応付け失敗時のフォールバックとして採用
- **C: コンテナ駆動の完全復元** — **採用** (2026-08-03 オーナー決定)
- **position ベースの対応付け** — 却下。誤対応時に別 Cell の `onValueChanged` へ値を書き込む危険 (最悪の failure mode)。cell.id ベースは不一致→dismiss に安全に倒せる

## 決定事項 (2026-08-03 オーナー確定)

1. **復元経路**: コンテナ (KsSettingsView) 駆動の完全復元。OK リスナー再登録 + 生成済み View への即時着色 + 「今日」ボタン再注入。着色/「今日」ボタンには既存フック待ちとは別の「即時適用」経路を追加する
2. **対応付け**: Fragment tag を cell.id ベースに変更 (世代サフィックス `.r<n>` 維持、プレフィックス一致 + サフィックス剥がしで照合)。不一致時は dismiss にフォールバック
3. **駆動点**: 「attach 済み」かつ「root 反映済み」の両条件が揃った最初の時点で一度だけ走査する one-shot ラッチ (pendingStore パターンと同型)
4. **複数インスタンス規則** (提案フェーズの second-opinion-001 Critical を受けた追加決定): 完全復元は単独インスタンス構成のみ。同一 FragmentManager 上に複数の KsSettingsView が attach されていれば一律 dismiss、処理済み Fragment は claim して重複処理を防ぐ。tag 符号化も「世代を id より前」の曖昧性のない形式へ補正 (詳細は ADR-0011 Decision 2/4)

## ADR 候補

- 作成済み: **android/ADR-0011** (accepted、2026-08-03 オーナー承認) — 上記3決定と却下案を収録

## 未決の論点

- (propose の設計事項) Colorizer / TodayShortcut への「即時適用」経路の具体設計、tag 生成規則の共有方法、復元ロジックの配置 (内部ヘルパーの切り方)、ドキュメントでの「安定 id 推奨」の案内先
- 大きな論点は残っていない

## UI 素材

- なし (新規 UI なし。既存ダイアログの状態復元のみ)

## 変更級の推奨: M

判定材料:
- 触る能力: TimePickerCell / DatePickerCell (Material) の表示経路、TimePickerColorizer / DatePickerColorizer、DatePickerTodayShortcut、KsSettingsView コンテナ — 複数ファイル横断
- 公開 API 変更: なし (tag は internal)。ただし「回転復元には安定 id 推奨」という利用者向け案内が増える
- 可逆性: tag 形式変更 + コンテナへの復元ロジック追加で、単純 revert しにくい
- UI: 新規 UI なし (mock 不要)。挙動の観察可能な変更 (回転時のダイアログ維持) はデルタスペックで記述

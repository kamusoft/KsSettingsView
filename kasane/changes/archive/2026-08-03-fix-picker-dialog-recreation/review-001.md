# レビュー結果: fix-picker-dialog-recreation (001 回目)

**日付**: 2026-08-03
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの Requirement / Scenario はすべて実装とテストで満たされており (詳細は `verify-001.md`)、ビルド・テストとも green (`:ks-settingsview-ui` / `:ks-settingsview-compose` 計 861 件、失敗 0)。tag 符号化の設計 (可変長 `cellId` を最終フィールドに置く固定書式) は難所を正面から解いており、実環境検証の証跡も規約が要求する水準を超えて揃っている。

一方で、復元走査が **「Activity 再生成で復元された Fragment」と「今まさに表示中の生きた Fragment」を区別していない**。単独インスタンス・単一画面の通常経路では到達しないが、同一 Activity 上で別の KsSettingsView が後から attach する構成では、生きたダイアログが閉じられる / 別画面の Cell に再束縛されて確定値が書き込まれる、という本変更が防ごうとした failure mode そのものが発生し得る。Compose DSL の id が構造由来 (位置ベース) で画面間衝突しやすいことが、この経路の到達性を実質的に押し上げている。

## 実施した検証

- `./gradlew testDebugUnitTest --rerun-tasks` (ANDROID_HOME 環境変数指定) → BUILD SUCCESSFUL / tests 861, failures 0, errors 0, skipped 0
- 変異注入によるテストの検出力確認 (実施後、実装は元のバイト列に復元済み。`shasum` で同一性確認)
  - `runRestoreScan()` を即 return → `PickerDialogRecreationTest` 24 件中 **18 件が FAILED**
  - `hasUniqueOwner` を常に true → 複数インスタンスの Scenario テストのみ FAILED (規則を正しく突いている)
  - `post { runRestoreScan() }` を同期呼び出しに変更 → 複数インスタンスの Scenario テストが FAILED (`post` による遅延が設計上 load-bearing であり、テストがそれを固定していることを確認)
- 足場アーティファクトの逆流検査: `git diff HEAD -- kasane/changes/.../tasks.md` はチェックボックスのみの差分。proposal / specs / ui/brief は無変更
- 公開 API 不変の確認: 追加された `PickerDialogTag` / `PickerDialogKind` / `PickerRestoreRegistry` / `MaterialDatePickerPresenter` / `resolve*DialogColors` / `notifyTimePickerSelection` / `KsSettingsView.restoreTodayProvider` はすべて `internal`。公開シグネチャの追加・変更・削除なし
- Non-Goals への侵食の確認: 変更ファイルは 5 本 + 新規 3 本のみで、`PickerCellViewHolder` / `NumberPickerCellViewHolder` / `DateSelectionSheet` 等のボトムシート系には一切触れていない
- コメント規約 (`concepts/cross/conventions/comment-policy.md`) の機械照合: 新規・変更コメントに禁止参照 (change-id 裸参照 / `kasane/changes` パス / Phase・Decision 通番 / `MUST` `SHALL` 等のデルタスペック構文キーワード) の混入なし。ADR 参照は許容形式 `android/ADR-0011`
- 実環境検証証跡のスポットチェック: `before/after-timepicker-dialog-after-rotate.png` (既定紫 → amber 維持)、`after-datepicker-material-after-rotate.png` (クリーム背景・amber 選択日丸・「今日」ボタン在) を目視し、`repro-steps.md` の記述と一致することを確認

## 指摘事項

### [🟠 Major] 復元走査が「復元された Fragment」と「表示中の生きた Fragment」を区別していない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` — `runRestoreScan()` (対象 Fragment の選別ループ)

**問題点**:

走査は `fragmentManager.fragments` のうち「`DialogFragment` かつ tag が `PickerDialogTag.decode()` できるもの」をすべて処理対象にする。ここには **その時点で正常に表示中のダイアログ (このプロセスが自分で `show()` したもの)** も含まれ、両者を区別する述語がない。

到達経路 (いずれも Activity 再生成とは無関係に起こる):

1. **生きたダイアログの意図しない dismiss** — 画面 A の KsSettingsView がピッカーを表示中に、同一 Activity へ別の KsSettingsView が attach され root が反映される。後着インスタンスの走査で `attachedViewCount >= 2` となり `hasUniqueOwner = false` → `cell == null` → `fragment.dismiss()`。表示中のダイアログが第三者によって閉じられる
2. **別 Cell への値の書き込み (より重い)** — 画面 A のダイアログ表示中に画面 A が detach され (Compose Navigation の `AndroidView` 破棄等で `unregister` が走る)、画面 B の KsSettingsView が attach + root 反映。走査時点では attach 中インスタンスが 1 つなので `hasUniqueOwner = true` となり、**画面 B の root にある同一 id の Cell** に対して `addOnPositiveButtonClickListener` が追加される。以後、画面 A から開いたダイアログの確定が画面 B の Cell の `onValueChanged` を発火させる

経路 2 の到達性は「id が画面をまたいで衝突するか」に依存するが、Compose DSL 経路の既定 id は構造由来 (`DSLNodes.kt` の `DSLIdentityHint.RootPosition(rootIdx)` + cell index) であり、**構成が似た 2 画面は同一 id を生成する**。したがって「稀な構成」では片付かない。

これは position ベース tag を却下した理由 (android/ADR-0011 Alternatives「別の Cell の `onValueChanged` に値を書き込む誤対応」) と同じ failure mode であり、id ベース化で塞いだ穴が「走査対象の選別」という別の入口から残っている。デルタスペックの Requirement は GIVEN が「Activity 再生成後」に限定されているため Scenario 対応表としては欠落にならない (`verify-001.md` は VALID) が、実装品質としては塞ぐべきと判断する。

**推奨修正**:

自分で `show()` したダイアログを走査対象から外す。既存の `PickerRestoreRegistry.claim()` (弱参照・Fragment インスタンス単位) をそのまま流用でき、追加の状態を持たずに済む:

- `MaterialDatePickerPresenter.show()` および `TimePickerCellViewHolder.showTimePicker()` で `picker.show(fm, tag)` の直後に `PickerRestoreRegistry.claim(picker)` を呼ぶ
- Activity 再生成後に saved state から戻る Fragment は**別インスタンス**なので claim されておらず、復元経路は現状のまま成立する
- 回帰テストとして「ダイアログ表示中に 2 つ目の KsSettingsView が attach + root 反映されても、表示中のダイアログが閉じられず、いずれの Cell にも発火しない」を追加する

（この対処が本変更のスコープ外だとオーナーが判断する場合は、`deviation.md` に既知の限界として記録し、`proposal.md` の「既知の制約」相当の扱いに揃えるのが筋。無記録のまま通さないこと。）

### [🟡 Minor] one-shot ラッチが走査の「実行」ではなく「予約」で立つため、取りこぼすと二度と復元されない

**該当箇所**: `KsSettingsView.kt` — `scheduleRestoreScanIfReady()` / `runRestoreScan()`

**問題点**:

`isRestoreScanScheduled = true` は `post` の**予約時点**で立ち、その後 `runRestoreScan()` が先頭の `if (!isAttachedToHostWindow) return` で抜けてもフラグは戻らない。attach + root 反映の直後・posted runnable の実行前に detach が挟まると (RecyclerView / ViewPager2 配下での再バインド、レイアウト直後の付け外し等)、そのインスタンスは以後 **一度も走査しない**。結果、復元も dismiss も行われずゾンビダイアログが修正前と同じ状態で残る (dismiss フォールバックにすら倒れない)。

デルタスペックの「両条件を最初に満たした時点で実行される」は満たしているとも読めるが、駆動条件の趣旨 (どちらの順序でも必ず一度は走る) を取りこぼす形になっている。

**推奨修正**: 早期 return するパスでは `isRestoreScanScheduled = false` に戻し、次の attach で再予約されるようにする (走査を実際に完了したときだけラッチを維持する)。

### [🟡 Minor] KDoc が別の関数に付いてしまっている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:165-195`

**問題点**:

`Context.findFragmentManager()` のための KDoc (165-175 行) の直後に、新規追加された `resolveTimePickerDialogColors` の KDoc (176-186 行) が挿入されたため、KDoc ブロックが 2 連続で並んでいる。Kotlin / Dokka は宣言の直前のブロックだけを doc として拾うため、**`findFragmentManager` の説明 (「ホスト Activity が `FragmentActivity` を継承している必要がある」という利用上の前提を含む) が宙に浮いて失われている**。

**推奨修正**: `findFragmentManager()` の KDoc をその宣言の直前へ移す (新規 2 関数はその上か下にまとめる)。

### [🔵 Suggestion] `PickerRestoreRegistry` の登録解除を固定するテストがない

**該当箇所**: `PickerRestoreRegistryTest.kt` / `MemoryLeakTest.kt`

レジストリ単体の `register` / `unregister` / `claim` は固定されているが、「`KsSettingsView.onDetachedFromWindow()` が確かに `unregister` を呼ぶ」ことを固定するテストがない。ここが抜けると複数インスタンス判定が detach 後も 2 のままになり、単独構成なのに一律 dismiss へ倒れる退行が無検出になる。参照はすべて弱参照でリーク自体は起きない (`WeakHashMap` の value 側も view を弱参照で保持しており、value→key の強参照はない) ため、リーク観点の追加は不要。

### [🔵 Suggestion] 横向き「通常表示」の参照ショットが証跡に無い

**該当箇所**: `ui/verification/`

`ui/brief.md` は視覚の正を「同じ Cell / Theme で通常表示したダイアログ」と定めているが、`after-datepicker-material-after-rotate.png` (横向き) に対する比較対象は `after-datepicker-material-normal.png` (縦向き) しかない。配色と「今日」ボタンの有無という**仕様が要求している点**は確かに検証できている一方、横向きレイアウトでの配置一致は A/B されていない。今後同種の検証を行うときは「回転してから開いた通常表示」を 1 枚足すと、比較の軸が揃う。

### [🔵 Suggestion / 本変更のスコープ外] 既存コメントの規約違反

`DatePickerColorizer.kt:488` に `kasane/changes/datepickercell-color-adjust/impl-notes.md` へのパス参照があり、`comment-policy.md`「禁止する参照 — アーカイブ文書のパス」に抵触する。本変更が追加したものではないため修正を求めないが、記録として残す。

## アクションプラン

1. **Major**: `runRestoreScan()` の走査対象から「自分で show した生きたダイアログ」を除外する (show 直後の `claim` が最小の対処)。あわせて回帰テストを追加。スコープ外と判断する場合は `deviation.md` に記録
2. **Minor**: `isRestoreScanScheduled` を早期 return 時に戻す
3. **Minor**: `TimePickerCellViewHolder.kt` の KDoc を正しい宣言へ付け直す
4. **Suggestion**: detach 時の `unregister` を固定するテストを追加 (任意)

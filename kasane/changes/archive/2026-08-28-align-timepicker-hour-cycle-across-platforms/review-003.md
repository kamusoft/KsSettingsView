# レビュー結果: align-timepicker-hour-cycle-across-platforms (003 回目)

**日付**: 2026-08-28
**判定**: APPROVED

## スコープ

**限定レビュー**。review-002 (CHANGES_REQUESTED) の指摘 2 件 — 🟡-1 (サンプル 3 面のコメントが 12時間制の系列順を断定) と 🔵 (Android DSL 表示テストの観測点) — の解消確認のみを対象とする。002 で確認済みの仕様充足・回帰検出力・証跡整合・破壊的変更の受容は再評価していない (一致検証は [verify-002.md](verify-002.md) の VALID が有効)。

## サマリー

002 の指摘 2 件はいずれも解消している。サンプル 3 面のコメントは系列順の断定をやめ、「午前／午後を含む 3 系列」+「並び順は端末 Locale の時刻表記に従う」の形になり、deviation.md の合意内容および本 change 自身の証跡画像 (ja = 午前/午後前置き、en = 後置き) と矛盾しなくなった。3 面とも同一文で、差は公開 API の命名 (`is24Hour` / `Is24Hour`、`format` / `Format`) のみ — sample-parity が許容する「画面文言に出ないコード上の差」に収まる。Android の `seriesCountByTappingRow` はタップ前の dialog インスタンスを控えて別物であることを確かめてから数える形になり、観測点が「今回のタップで開いた選択面」に締まった。

指摘の 2 箇所以外に、この修正サイクルで新たに持ち込まれた変更はない。

## 確認したこと

### 🟡-1 (サンプル 3 面のコメント) — 解消

修正後の 3 箇所は以下の同一文になっている。

- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:225-226`
- `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:213-214`
- `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:87-88`

> `is24Hour = false` を指定すると選択面は午前／午後を含む 3 系列になる (並び順は端末 Locale の時刻表記に従う)。
> `format` は行の表示にだけ効き、選択面の時制には関与しない。

- **事実との整合**: 系列**数** (3) だけを断定し、**順序**は locale 由来と述べる形になった。deviation.md の合意 (「系列の順序は端末 Locale の時刻パターン由来」)、および evidence の画像 3 枚 (ja-JP × 2 = 午前/午後前置き、en-rUS = 後置き) のいずれとも矛盾しない。ja 環境の利用者がコメントと画面の食い違いを不具合と誤読する入口は閉じた
- **旧文言の残存なし**: `samples/` `android/` `ios/` `maui/` を「時・分・午前」「時・分・AM」で grep して 0 件
- **sample-parity**: 3 面とも同一文。MAUI の `Is24Hour="False"` / `Format` は公開 API の命名差であり、cross/conventions/sample-parity が「許容される差異」に挙げる「本体公開 API の platform 命名差に由来する、画面文言に出ないコード上の差」に該当する (コメントはそもそも表示文言ではない)
- **他箇所との無矛盾**: 002 で「こちらは正しい」とされた宣言側コメント (`InputCellsDemoView.swift:73` の「選択面が午前／午後のホイールを持つ形になる」) は並びに言及しておらず、今回の文とも整合する
- **comment-policy**: 禁止参照・禁止記述類型なし。`comment-policy-lint.py --summary` = 禁止 0 件 (検査対象 678 ファイル)

### 🔵 (Android DSL 表示テストの観測点) — 解消

`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLTimePickerHourCycleRenderingTest.kt:89,92-93`

タップ前に `ShadowDialog.getLatestDialog()` を `previousDialog` として控え、タップ後に得たインスタンスが同一 (`dialog === previousDialog`) なら 0 を返して `awaitSeriesCount` の待機を続けさせる形になった。dismiss 済みの選択面を数え続ける経路 (対称性テストでは別 index の選択面を数えてしまう経路) が閉じている。KDoc (`:81-83`) にこの理由が自己完結して書かれており、外部参照にも依存していない。

ガードが空振り (常に 0 を返して timeout) になっていないことは、テストが実際に緑で通ること (下記) で確認した — 各タップが新しい Dialog インスタンスを生成しており、識別子比較は生きている。

### テスト実行

`ANDROID_HOME=... ./gradlew :ks-settingsview-compose:test --rerun-tasks` = **BUILD SUCCESSFUL** (`--rerun-tasks` を付けたのは、初回実行が `UP-TO-DATE` で実行証跡にならなかったため)。

- モジュール全体: 125 tests / 0 skipped / 0 failures / 0 errors
- `DSLTimePickerHourCycleRenderingTest`: 2 tests / 0 failures — `DSL 再評価で is24Hour 変更が選択面の系列へ反映される` / `Store 経路と DSL 経路で is24Hour 変更後の選択面が一致する` の両方が実行されている (`TEST-*.xml` で確認)

🟡-1 はコメントのみの変更のため、他 platform のビルド・テストは再実行していない (002 で 3 platform 全件緑を確認済み、以降コード変更なし)。

### 足場・lint

- `git diff HEAD -- kasane/` は tasks.md のみ、かつ差分行は全てチェックボックス行 (`- [ ]` / `- [x]`)。足場の逆流なし
- `identity-lint.py` / `local-path-lint.py` = 違反 0

## 指摘事項

なし。

## 申し送り (指摘ではない)

- `DSLTimePickerHourCycleRenderingTest.kt:206-210` の最後の `assertEquals` は、直前の `awaitSeriesCount(0, 3)` / `awaitSeriesCount(1, 3)` が両脚を 3 に固定した後の比較であり、実質的にはトートロジーに近い (両方が 0 を返す状況では `0 == 0` で通る)。ただしこのテストの検出力は timeout で失敗する 2 本の `awaitSeriesCount` 側にあり、そちらは今回の修正で観測点が締まっている。実害はないため修正は求めない
- (002 から継続) `kasane/concepts/core/cells/time-picker-selection-surface.md` の「時制の決定と候補系列 (Android)」節は、`format` の `a` 判定を時制の決定源として記述したままで ADR-0028 と矛盾する。系列順の locale 由来化も同じ節の対象。proposal の Non-Goals どおり ksn-distill で書き換える

## アクションプラン

なし。本 change は蒸留 (ksn-distill) へ進んでよい。蒸留時に上記「申し送り」の concepts 追随を必ず含めること。

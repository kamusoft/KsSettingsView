# レビュー結果: align-timepicker-hour-cycle-across-platforms (002 回目)

**日付**: 2026-08-28
**判定**: CHANGES_REQUESTED

## サマリー

001 の指摘 3 件と、相方レビュー由来で採用された Major (Store / DSL の対称テスト欠落) は**すべて解消**している。新設の対称テストは Android / iOS とも実経路 (Store 公開操作 → Host → 実バインド → 実タップ → 実選択面 / DSL 再評価 → 差分検出 → Store → 表示) を通しており、等価判定から `is24Hour` を外すミューテーションで DSL 側 5 件が実際に落ちることまで実測で確認した — core/ADR-0018 が想定する「検出層の無音の取りこぼし」を検出できる形になっている。MAUI Android ホストの `Set24Hour` 直呼び経路も実行時証跡で埋まり、`capture-environment.txt` はアプリ単位 Locale の実測で書き直されて証跡画像と整合する。テストは 3 platform とも全件緑 (Android 2626 / iOS 619 / MAUI 475、いずれも 0 failures)、MAUI は両 TFM とも 0 警告 0 エラー、足場の逆流なし、comment-policy / identity / local-path の 3 lint も 0 件。

deviation 済みの系列順 (12時間制の並びを端末 Locale の時刻パターン由来にする) は、Android では `TimeWheelLabels.isPeriodLeading` として OS のパターン解決に委ねる形で実装され、ja / en の対テストで行の並びまで固定されている。iOS / MAUI-iOS は `UIDatePicker` が同じ規則で列を組み替えるため、3 面の 12時間制提示が locale 準拠で揃った — 証跡画像 4 枚 (Android native ja / MAUI-Android en / MAUI-iOS ja / iOS native en) がその成立を示している。

差し戻しの理由は 1 件だけ。**サンプル 3 面に追加されたコメントが、この deviation で変わった当の挙動を誤って断定している** — 3 面とも「選択面は時・分・午前／午後の 3 系列になる」と固定順で書かれているが、本 change 自身が提出した ja 環境の証跡画像は「午前/午後・時・分」を示しており、コメントが証跡と矛盾する。Sample は platform 間比較の検証装置であり (cross/conventions/sample-parity)、次に読む人 (docs-refresh / ksn-distill を含む) が誤った契約を拾う入口になる。3 行で閉じる。

一致検証の詳細は [verify-002.md](verify-002.md) (判定 **VALID**)。

## 確認した観点

- **仕様充足**: デルタスペック 8 本の全 Requirement / Scenario を対応表で潰した (verify-002.md)。虚偽チェックなし、足場の逆流なし (`git diff HEAD -- kasane/` は tasks.md のチェックボックス 16 行のみ)、deviation.md 記載の 1 件以外に未記録の乖離なし。付随修正の記載はなく、diff にも付随修正に相当する変更は見当たらない
- **ビルド・テスト (レビュアー側で実行)**: Android `./gradlew test --rerun-tasks` = 2626 tests / 0 failures / 0 errors、iOS `xcodebuild test -scheme KsSettingsView-Package` = 619 tests / 0 failures (** TEST SUCCEEDED **)、MAUI `dotnet test` = 475 tests / 0 failures、MAUI `dotnet build -f net10.0-android` / `-f net10.0-ios` = ともに 0 警告 0 エラー。新設 5 テスト (iOS) が実際に実行されていることは `-only-testing` の名指し実行で確認、Android の新設 2 クラスは `TEST-*.xml` の生成で確認
- **回帰検出力 (lessons/code-review L-001)**: `TimePickerCell` の等価判定から `is24Hour` を外すミューテーションを両 OS に投入。Android は `DSLTimePickerHourCycleRenderingTest` 2 件 (Store 脚・DSL 脚の両方が失敗メッセージに出る) + `InputCellsTest` / `TimeSelectionSheetTest` 各 1 件、iOS は `DSLTimePickerHourCycleTests` 3 件が失敗した。Store 経路テストはこのミューテーションでは落ちないが、これは `replaceCell` が明示操作で等価判定に依存しないためであり、観測点 (実選択面の系列 / picker の実 locale) は実物を見ているので空振りではない。原状復帰は backup からの復元と shasum 一致で確認済み
- **証跡と提出コードの対応 (lessons/process L-003 (4))**: evidence/ の画像 8 枚をすべて開いた。Android native はアプリ単位 Locale ja-JP の記載どおり日本語 UI (キャンセル / 午前・午後) かつ午前/午後前置き、MAUI-Android はアプリ単位 Locale 未設定 = システム en-rUS の記載どおり英語 UI (Cancel / AM・PM) かつ後置きで、`capture-environment.txt` の記述と画像が完全に整合する (001 🟡-2 解消)。MAUI-Android の 1 枚は本リポジトリ唯一の setter 直呼び (`dto.Set24Hour(...)`) を実行時に通した証跡として成立している (001 🟡-1 解消)
- **規約**: cross/conventions の comment-policy (`comment-policy-lint --summary` = 0 件、および規約本文の禁止類型を目視でも確認 — 新規コメントの外部参照は `core/ADR-0028` `android/ADR-0018` の許容形式のみ)・sample-parity・test-execution・runtime-behavior-verification を適用。`identity-lint.py` / `local-path-lint.py` も違反 0
- **設計品質**: `HourCycleLocale` のキャッシュ (001 🔵) は `Locale.current` をキーにした作り直し + `NSLock` で入っており、`base:` 明示経路はテスト用に残っている。`TimeSelectionSheet.orderedWheels` はプロパティ初期化順 (hour / minute / period の後) が正しく、行レイアウトと単一の真実源を共有している。`resolvePeriodLeading` は ICU の `getBestPattern` に委ね、引用符外のパターン文字だけを見る形で自前 Locale 一覧を持たない — 撤去した `timeFormatUsesAmPm` と同じ引用符解釈を再利用しており一貫している
- **破壊的変更**: Android の `format = "h:mm a"` 依存と iOS の端末設定追随の廃止は ADR-0028 / proposal「Impact」で受容済み。既定 (`is24Hour = true` × `format = "HH:mm"`) で見た目が変わらないことは 3 面の既定テストで担保
- **concepts の未追随 (指摘ではない)**: `kasane/concepts/core/cells/time-picker-selection-surface.md:35-41` は依然 `format` の `a` 判定を時制の決定源として記述しており、ADR-0028 と矛盾する。proposal の Non-Goals で concepts 追従は ksn-distill の責務と明示されているため本レビューの指摘にはしないが、**蒸留時に必ず書き換わる箇所**として記録しておく (系列順の locale 由来化も同じ節の対象)

## 指摘事項

### [🟡 Minor / 優先度: 高] サンプル 3 面の新規コメントが 12時間制の系列順を誤って断定している

**該当箇所**:
- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:225`
- `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:213`
- `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:87`

**問題点**:

3 面とも同一文で「`is24Hour = false` を指定すると選択面は**時・分・午前／午後**の 3 系列になる」と、系列の並びまで断定している。しかし deviation.md で合意された変更により、12時間制の並びは端末 / アプリの Locale の時刻パターン由来になった (Android は `TimeWheelLabels.isPeriodLeading` → `TimeSelectionSheet.orderedWheels`、iOS / MAUI-iOS は `UIDatePicker` が同じ規則で組み替える)。

本 change 自身が提出した証跡がこれと食い違う:

- `evidence/android-input-cells-demo-bedtime-picker-12h.png` (アプリ単位 Locale ja-JP) = **午前/午後・時・分**
- `evidence/maui-input-cells-demo-bedtime-picker-12h.png` (ja_JP) = **午前/午後・時・分**
- `evidence/maui-android-input-cells-demo-bedtime-picker-12h.png` (en-rUS) = 時・分・AM/PM

つまり日本語環境で動かした利用者は、コメントの説明と画面が一致しない。Sample は platform 間比較の検証装置であり (`kasane/concepts/cross/conventions/sample-parity.md`)、「Sample の書き方の差」と「本体の仕様差 (= バグ)」を切り分けるための基準になる。その基準側に事実と異なる説明が入ると、次の読み手が ja 環境のスクリーンショットを不具合と誤読しうる。comment-policy の「現在の仕様の説明であること」にも反する。

同ファイル内の `bedTime` / `bedDate` 宣言側のコメント (「選択面が午前／午後のホイールを持つ形になる」) は並びに言及しておらず、こちらは正しい。

なお 3 面とも本 change が触れているファイルで、修正は 1 行ずつ計 3 行に収まる (lessons/process L-005 の「到達可能な修正はそのサイクル内で直す」に該当する)。

**推奨修正**:

並びの断定をやめ、系列の構成だけを述べるか、locale 由来であることを明示する。3 面で同一文にすること (sample-parity)。例:

```
// `is24Hour = false` を指定すると選択面に午前／午後の系列が加わり 3 系列になる。
// 系列の並びは端末 Locale の 12時間表記に従う（日本語は午前／午後が先、英語は後）。
// `format` は行の表示にだけ効き、選択面の時制には関与しない。
```

### [🔵 Suggestion] Android の DSL 表示テストが直前の選択面を数えてしまう余地がある

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLTimePickerHourCycleRenderingTest.kt` の `seriesCountByTappingRow`

**問題点**:

`row.performClick()` の直後に `ShadowDialog.getLatestDialog()` を読むが、`ShadowDialog` は dismiss 済みのダイアログも「最後に表示されたもの」として返し続ける。行が存在するのにタップで新しい選択面が開かなかった場合、直前の観測 (対称性テストでは **別 index の選択面**) を数えたまま `awaitSeriesCount` が成立してしまう。

現状のプロダクションコードでは `isEnabled` な TimePickerCell 行のタップは必ず選択面を開くため実害は観測されていない (実際、ミューテーション投入時には期待どおり両脚が失敗した)。ただし観測点が「今回のタップで開いた選択面」であることを保証していないため、将来 bind の抜けが起きたときに緑のまま通る余地が残る。

**推奨修正**:

タップ前の `ShadowDialog.getLatestDialog()` を控えておき、タップ後に得たインスタンスが別物であることを確認してから数える (同一なら 0 を返して待機を続ける)。

## アクションプラン

1. **🟡-1** サンプル 3 面 (Android / iOS / MAUI) のコメントから系列順の断定を外すか locale 由来である旨に書き換える (3 行、3 面で同一文)。修正後にビルド or lint の再実行は不要 (コメントのみ)
2. **🔵** 余力があれば `seriesCountByTappingRow` の観測点を「今回のタップで開いた選択面」に締める
3. (蒸留への申し送り) `kasane/concepts/core/cells/time-picker-selection-surface.md` の「時制の決定と候補系列 (Android)」節は ADR-0028 と系列順の deviation の両方を反映して書き換える必要がある

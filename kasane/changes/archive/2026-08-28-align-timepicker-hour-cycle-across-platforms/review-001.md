# レビュー結果: align-timepicker-hour-cycle-across-platforms (001 回目)

**日付**: 2026-08-28
**判定**: CHANGES_REQUESTED

## サマリー

core/ADR-0028 の実装として、時制の決定源を 3面 + bridge で `is24Hour` に一本化する骨格は正確に入っている。Android は `timeFormatUsesAmPm` を関数ごと撤去して旧判定の残骸を残さず、iOS は「hour cycle だけを差し替え、表記の言語は端末 Locale 由来のまま保つ」という spec の難所を `HourCycleLocale` (`Locale.Components` の hourCycle 差し替え) で正面から満たし、その性質を en_US / ja_JP 双方向の実測テストで固定している。更新検知 (equals/hash・`AffectsSnapshot`) への参加も 3面すべてで漏れがない。テストは 3 platform とも全件緑 (Android 2614 / iOS 616 / MAUI 475、いずれも 0 failures)、足場アーティファクトの逆流もなし。コメントは新規参照がすべて `core/ADR-0028` の許容形式で、comment-policy lint も 0 件。

Critical / Major はない。差し戻しの理由は、**完了判定の証跡側に 3 件の穴**があること — (a) MAUI の唯一の新規 platform 固有コード (Android gateway の `Set24Hour` 直呼び) が自動テストも視覚証跡も持たない、(b) Android 証跡の撮影環境記録が証跡画像自身と食い違っている、(c) MAUI サンプルの新規デモ行のイベント表示が非英語 Locale で 3面パリティを崩す。いずれも数行〜数分で閉じる。

一致検証の詳細は [verify-001.md](verify-001.md) (判定 INVALID / ❌ 1 件、内容は下記 🟡-1 と同じもの)。

## 確認した観点

- 仕様充足: デルタスペック 8 本の全 Requirement / Scenario を対応表で潰した (verify-001.md)。虚偽チェックなし、足場の逆流なし、未記録の仕様逸脱なし
- ビルド・テスト: Android `./gradlew test --rerun-tasks` (2614 tests / 0 failures)、iOS `xcodebuild test -scheme KsSettingsView-Package` (616 tests / 0 failures)、MAUI `dotnet test` (475 tests / 0 failures)、MAUI Android TFM `dotnet build -f net10.0-android` (0 警告 0 エラー) をレビュアー側で実行して確認
- 回帰検出力: 追加された assertion はいずれも実装に直結する (`datePicker.locale` を設定しなければ `resolvedIs24Hour` が nil を返して落ちる / `TimeCandidates.of` が `format` 判定に戻れば Android の 3 テストが落ちる / `AffectsSnapshot` から `Is24Hour` を外せば `scope.Single<ReplaceCell>()` が落ちる)。Android の更新反映テストは ViewHolder の実 bind → 実タップ → 実シートを通しており、代理値ではない (lessons/test L-001 準拠)
- 規約: cross/conventions の comment-policy (`python3 scripts/comment-policy-lint.py --summary` = 0 件、および規約本文の禁止類型を目視でも確認)・sample-parity・test-execution を適用。ktlint / detekt / swiftlint は本リポジトリに未導入
- 破壊的変更: Android で `format = "h:mm a"` に依存していた 12時間制利用は `is24Hour = false` の明示が必要になるが、これは ADR-0028 と proposal「Impact」で受容済み。既定 `true` × 既定 `format = "HH:mm"` の組み合わせは見た目が変わらないことを Android / iOS / MAUI の既定テストで確認した

## 指摘事項

### [🟡 Minor / 優先度: 高] MAUI Android ホストの gateway 経路に自動テストも視覚証跡もない

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:414-416`

**問題点**:
maui-bridge の Scenario「MAUI facade の値が gateway を透過する」は THEN で「**両 OS** の TimePicker bridge DTO の `is24Hour` は `false` である」と両 OS を名指ししている。iOS 側は `evidence/maui-input-cells-demo-bedtime-picker-12h.png` が facade→snapshot→iOS gateway→DTO→native を通しで示す (ja_JP・端末既定 24時間制の Simulator で 12時間制 picker が出ている) が、Android 側は自動テストも証跡もない。

しかもこの 1 行は本リポジトリで唯一の「binding 生成器がプロパティにまとめないので setter を直接呼ぶ」手書き経路である。他の bool 系 (`isOn` → `dto.On`、`isEnabled` → `dto.Enabled`) はすべてプロパティ代入で書けており、`is24Hour` だけが例外になっている (Kotlin が `is` を剥がした残りが `24Hour` で C# 識別子にならないため)。将来 Kotlin 側の名前が変われば静かに壊れる形なので、実行される証跡が 1 枚欲しい。lessons/process L-003 (4) の「レビューは証跡の実在と提出コードとの対応を判定条件にし、実機確認を Suggestion へ格下げしない」に該当する。

なお実装の誤りは見つかっていない。レビュアー側で `dotnet build -f net10.0-android` の成功と、生成 binding が実際にメソッド `Is24Hour()` / `Set24Hour(Java.Lang.Boolean?)` を出し JNI シグネチャが `set24Hour.(Ljava/lang/Boolean;)V` で Kotlin 側 setter と一致することまで確認済み (`maui/android/KsSettingsView.Binding.Android/obj/Debug/net10.0-android/generated/src/KsSettingsView.Bridge.KsBridgeTimePickerCell.cs:317-341`)。残る未確認は実行時の値伝達だけである。

**推奨修正**:
MAUI サンプルを Android エミュレータで起動し、12時間制デモ行 (就寝) の選択面を 1 枚 `evidence/maui-android-input-cells-demo-bedtime-picker-12h.png` として追加し、`evidence/capture-environment.txt` に撮影環境を追記する。証跡を足さない判断をするなら「MAUI の視覚確認は iOS ホストのみで代表させる」を deviation.md に記録してオーナー合意を取る (実装の誤りではないため、この選択肢も成立する)。

### [🟡 Minor / 優先度: 中] Android 証跡の撮影環境記録が証跡画像自身と食い違っている

**該当箇所**: `kasane/changes/align-timepicker-hour-cycle-across-platforms/evidence/capture-environment.txt` の `[Android]` ブロック

**問題点**:
記録は「Locale: en-US (ro.product.locale)」「端末: Android Emulator (AVD: ksn_custcell_api35 / model sdk_gphone64_arm64)」としているが、同じディレクトリの `android-input-cells-demo-bedtime-picker-12h.png` / `-alarm-picker-24h.png` には **OS 提供の文字列リソースが日本語で写っている** — シートヘッダーの「キャンセル」は `android.R.string.cancel` (`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SheetChrome.kt:198`)、系列ラベルの「午前 / 午後」は端末 Locale から導出される表記であり、いずれも en-US 端末なら "Cancel" / "AM" / "PM" になる。

レビュアー側で当該 AVD (`ksn_custcell_api35`) を確認したところ、現在の設定は `am get-config` = `...-en-rUS-...`、`persist.sys.locale` 未設定、`ro.product.locale` = `en-US` であり、この構成からは画像の日本語 UI は出ない。サンプルアプリ側の locale 強制もない (`samples/android/app/src/main/res/` に `values-ja` はなく、`localeConfig` / `setApplicationLocales` の使用もなし)。つまり記録された環境と実際の撮影環境が違う (日本語 Locale の別端末で撮影された可能性が高い)。

本 change は「時制が端末設定に依存しない」ことが主題であり、証跡の意味は撮影環境の記述と一体でしか読めない。結論自体は揺らがない (日本語 Locale の端末も既定は 24時間制なので「24時間制の端末で 12時間制シートが出る」という主張は成立する) が、記録が誤っていると後から証跡の意味を取り違える。

**推奨修正**:
実際に撮影した端末・Locale・時刻表示設定を確認して `capture-environment.txt` を書き直すか、記録どおりの AVD で撮り直す。前者なら `adb -s <device> shell am get-config` と `settings get system time_12_24` の実測値を根拠にするのが確実 (`ro.product.locale` は製品既定であって現在の Locale ではない)。

### [🟡 Minor / 優先度: 中] MAUI サンプルの新規デモ行のイベント表示が非英語 Locale で 3面パリティを崩す

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/ViewModels/InputCellsDemoViewModel.cs:265-266` (`FormatTime12Hour`)

**問題点**:
新規追加の `FormatTime12Hour` は `s_culture` (= `CultureInfo.InvariantCulture`、同ファイル 17 行目) で `"h:mm tt"` を整形するため、どの端末でも「就寝 → 10:15 PM」になる。一方 iOS は `Self.time12HFormatter` が `Locale.current` で解決され (`samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:333-337`)、Android は `DateTimeFormatter.ofPattern("h:mm a")` が JVM 既定 Locale で解決される (`samples/android/.../InputCellsDemoScreen.kt:349`)。したがって日本語端末では iOS / Android が「就寝 → 10:15 午後」、MAUI だけが「就寝 → 10:15 PM」になり、画面上部の「最後のイベント:」行が 3面で食い違う。

これは本 change が新設したデモ行で初めて生じる差異である (既存の MAUI イベント表示は `hh\:mm` や `yyyy/MM/dd` など Locale 非依存の書式ばかりで、たまたま一致していた)。Cell 本体の valueText は 3面とも native が端末 Locale で整形するため一致しており (提出済み証跡でも MAUI ja は「10:15 午後」)、ずれるのはサンプル側のイベント表示だけである。sample-parity 規約 (`kasane/concepts/cross/conventions/sample-parity.md`) の「表示文言の完全一致」と、「Sample 自体がばらばらだと platform 間比較の検証装置として機能しない」という目的に照らすと、直しておくべき差異にあたる。

**推奨修正**:
`FormatTime12Hour` だけ `CultureInfo.CurrentCulture` を使う (1 行)。VM 全体の `s_culture = InvariantCulture` を変える必要はない — 他の書式は Locale 非依存なので現状のままで一致する。

```csharp
// before
private static string FormatTime12Hour(TimeSpan value)
    => DateTime.MinValue.Add(value).ToString("h:mm tt", s_culture);

// after (Cell の valueText と同じく端末 Locale の表記に揃える)
private static string FormatTime12Hour(TimeSpan value)
    => DateTime.MinValue.Add(value).ToString("h:mm tt", CultureInfo.CurrentCulture);
```

### [🔵 Suggestion] iOS: `HourCycleLocale.forcing` を bind ごとに構築している

**該当箇所**: `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift:69` / `ios/Sources/KsSettingsViewUI/HourCycleLocale.swift:22-26`

**問題点**:
`render(cell:theme:)` はセル再利用のたびに呼ばれるが、そのたびに `Locale.Components(locale:)` の分解と `Locale(components:)` の再構築 (ICU の identifier 解決を伴う) が走る。値域は `is24Hour` の二値しかなく、`base` も実質 `Locale.current` 固定なので、結果はキャッシュできる。スクロール中の bind 頻度を考えると無駄が小さくない。

**推奨修正**:
`HourCycleLocale` 内に `Locale.current` 由来の 2 値を lazy に持つ (端末 Locale の変更に追随する必要があるなら、`Locale.current` をキーにしたメモ化にする)。テストが `base:` 引数を使っているため、引数付き経路は残す。

### [🔵 Suggestion] iOS テスト名の接頭辞が実際の検証対象と合っていない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift` の `test_TimePickerCellView_時制の強制でも表記の言語は端末Localeを保つ`

**問題点**:
このテストは `TimePickerCellView` を一切生成せず、`HourCycleLocale.forcing` と `DateFormatter` の `amSymbol` / `pmSymbol` だけを見ている。直前の `test_HourCycleLocale_時制は基準Localeの既定時制に依存しない` と同じ対象なので、接頭辞が揃っていないと「View 経路まで確認済み」と誤読されうる (検証内容そのものは spec の Scenario を正しく満たしている)。

**推奨修正**:
`test_HourCycleLocale_時制の強制でも表記の言語は基準Localeを保つ` のように接頭辞を対象に合わせる。

## アクションプラン

1. **🟡-1** MAUI サンプルを Android エミュレータで起動し、12時間制デモ行の選択面証跡を 1 枚追加する (+ `capture-environment.txt` に環境追記)。足さない場合は deviation.md に「MAUI の視覚確認は iOS ホストで代表」を記録してオーナー合意を取る
2. **🟡-2** `evidence/capture-environment.txt` の `[Android]` ブロックを実測値で書き直す (または記録どおりの AVD で撮り直す)
3. **🟡-3** `FormatTime12Hour` を `CultureInfo.CurrentCulture` に変える (1 行)
4. **🔵** 余力があれば `HourCycleLocale` の結果キャッシュとテスト名の接頭辞是正

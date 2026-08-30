# Exploration: restore-maui-picker-selected-command

## 課題 / 動機

MAUI 版 `PickerCell` に、選択面での選択完了を ViewModel へ通知する `SelectedCommand` が実装されていない。
利用者向け移行表では `SelectedIndex` / `SelectedIndices` の TwoWay バインドで代替できるとしていたが、値の変更と選択操作の完了は別の意味であり、setter の観測だけでは ViewModel が完了を検知できない。

移植元 AiForms は `SelectedCommand` を OneWay の `ICommand` として公開し、選択値の反映後に実行する。単一選択では `SelectedItem`、複数選択では `SelectedItems` を実行引数に渡す。

### 追補 (2026-08-29): Sample 反映と検証の欠落

初回の探索は MAUI facade の公開 API 復元だけを範囲とし、Sample アプリへの反映と実経路の検証が計画から漏れていた。再探索で次の 3 点を確認した。

- **実経路が一度も通っていない。** 追加テストは controller の interaction sink (`PickerCellSelectionChanged` / `PickerCellSelectionIndicesChanged`) を直接呼ぶだけで、native 選択面 → gateway → sink → Cell の経路を踏んでいない。`SelectedCommand` は「native 確定通知でしか発火しない」設計のため、この経路が未検証であることは公開 API の動作そのものが未確認であることを意味する。verify-001 / verify-002 が確認したのは cancel 時の非発火 (発火しない側) であり、発火側の実経路ではない
- **Sample の「通知先メンバー」が共通 footer の宣言と食い違っている。** 3 platform で一字一句同じ footer は「通知先メンバーは確定時に選択要素の一覧を受け取る経路」と述べ、iOS / Android は `onItemsSelected` callback で実装している。MAUI だけ `SelectedItems` の TwoWay バインドで書かれており、footer が描く「担当者 = バインド / 通知先メンバー = 確定通知」の対比が成立していない。原因は対応する公開 API (`SelectedCommand`) が MAUI に無かったことであり、本 change が塞ぐべき穴の実体である
- **tasks.md 3.5 の請求が実態と食い違う。** 「確定通知のない状態 (cancel 相当)」を facade テストで担保したと書いているが、直接 setter のテストは cancel と区別できない (second-opinion code-001 Minor-3)。cancel の非発火は既存の native 選択面テストが根拠になっている

なお実装差分そのものに問題は見つからなかった。`ApplyNativeValue` から `FindCell` + `Write` への展開は書き戻しの意味論を変えておらず、複数選択の早期 return 反転も「同値なら書き戻さない・完了通知はする」で spec どおり。発火口は Android / iOS の gateway 2 本に閉じており、直接 setter からは到達できない。

## 検討した選択肢 (却下案と理由を含む)

- 採用候補: MAUI facade に `SelectedCommand` を復元し、native から選択確定通知を受けた後、公開選択値を書き戻してから実行する。単一選択では `SelectedItem`、複数選択では `SelectedItems` を引数にする
- 却下: `SelectedIndex` / `SelectedIndices` の TwoWay setter だけで完了を観測する — ViewModel からの初期値設定・プログラム更新と、利用者による選択完了を区別できない
- 却下: Native / Bridge に Command 概念を追加する — 現行の確定通知が既に C# facade まで到達しており、MAUI 固有の `ICommand` を interop 境界へ持ち込む必要がない

### Sample 反映の形 (2026-08-29)

- 採用: 既存の「通知先メンバー」を `SelectedIndices` バインド + `SelectedCommand` へ組み替える。native の `selectedIndices` + `onItemsSelected` と同じ経路になり、表示文言を一切変えずにパリティが回復する。同時に実経路の検証装置が手に入る
- 却下: `SelectedCommand` 専用のセクションを新設する — 3 platform に同じ header / footer を足す必要があり、native 2 面を巻き込んで本 change の範囲を超える
- 却下: 単一選択 (担当者) にも `SelectedCommand` を足す — native の Sample は単一選択を `selectedItem` バインドで書いており、MAUI だけ Command にするとかえってパリティが崩れる

### `SelectedItems` TwoWay デモの退避先 (2026-08-29)

native の複数選択 PickerCell が TwoWay で受けるのは `selectedIndices` だけで、要素列のバインドは iOS / Android のいずれにも存在しない (要素列は `onItemsSelected` からのみ得られる)。したがって `SelectedItems` の TwoWay は MAUI 固有の公開契約であり、sample-parity の「対応概念が他 platform に存在しない画面は片側のみでよい」例外に該当する。

- 採用: MAUI 固有画面「MAUI 固有 Cell 機能デモ」を新設して収容する。以後の Cell 横断の facade 固有機能もここへ同居させられる器にする
- 却下: デモを失わせる — `SelectedItems` の TwoWay は MAUI 固有の公開契約であり、目視確認の手段を残す価値がある
- 却下: 既存「CustomCell の MAUI 固有デモ」へ同居させる — あの画面は CustomCell 1 種の意味論に閉じており、Cell 横断の器ではない

## 決定事項

- `SelectedCommand` は MAUI facade の `PickerCell` に属する公開 API として実装する
- 発火源は native から届く選択確定通知だけとし、`SelectedIndex` / `SelectedIndices` / `SelectedItem` / `SelectedItems` の直接設定では発火しない
- Command は選択値の書き戻しと相互導出が完了した後に実行し、ViewModel から新しい選択値を観測できる順序にする
- 実行引数は受け取った確定通知の種類で決める (単一選択の通知で `SelectedItem`、複数選択の通知で `SelectedItems`)。選択面は表示開始時のモードで動くため、Cell の現在の `SelectionMode` を根拠にすると、表示中にモードが変わったときに利用者が確定した種類と違う引数を渡してしまう。任意の `CommandParameter` は追加しない
- 移植元互換として `CanExecute` は確認せず、選択完了時に `ICommand.Execute` を直接呼ぶ
- cancel / 非確定 dismiss は現行 Native が確定通知を送らないため発火しない
- Native / Bridge の変更は不要

### Sample 反映と検証 (2026-08-29 決定)

- Sample の「通知先メンバー」は `SelectedIndices` バインド + `SelectedCommand` へ組み替え、native と同じ確定通知経路にする。footer を含む表示文言は変更しない
- `SelectedItems` の TwoWay デモは新規 MAUI 固有画面「MAUI 固有 Cell 機能デモ」(`SampleScreenCategory.MauiSpecific`) へ移す。native に対応概念が無いため sample-parity の例外に該当し、他 platform への追随義務を負わない。既存の「CustomCell の MAUI 固有デモ」とは統合せず、Cell 横断の facade 固有機能の器として別に立てる
- この画面には単一選択の行も置く。native は選択完了を関数として渡す形でしか公開しておらず、バインド可能な command として公開する形は MAUI facade 固有の意味論であるため、sample-parity の「主対象が platform 固有の公開 API と意味論なら platform 固有画面としてよい」例外に該当する。これにより単一選択の `SelectedCommand` も実経路で踏めるようになり、公開 API の片側だけが e2e 未検証で残る状態を解消する
- 各行は選択値に加えて完了通知の受信回数を表示する。同じ選択を確定し直したときの発火は値の表示だけでは観測できず、この change の中心である「値が同じでも完了を通知する」を目視で判定できないため
- 検証は iOS Simulator と Android Emulator の両方で行う。発火口が platform 別の gateway 2 本しかなく、iOS だけ `nint` → `int` のキャストが挟まるため、片側だけでは設計の半分が未検証で残る
- 検証で踏む操作は「単一の確定」「複数の確定」「同一値の再確定」「cancel」の 4 つ。同一値の再確定はバインド setter が走らないため `SelectedCommand` でしか観測できず、この change の存在理由そのものを確認する経路になる
- 実機検証は行わない。選択面は OS 標準 UI の操作だけで完結し、実機固有の要素が無い
- 証跡としてスクリーンショットを change 配下に残す
- `tasks.md` 3.5 の cancel 請求は既存 native 選択面テストへ付け替え、facade テストの請求は「直接 setter・未知 Cell ID」に限定する
- 変更範囲は MAUI facade・controller・テストに加えて `samples/maui` とする

## ADR 候補

なし。MAUI facade 内で完結する局所的な公開 API の復元であり、既存の `maui/ADR-0008` (対応概念は AiForms 命名で公開) と `maui/ADR-0012` (選択確定通知の書き戻し経路) から導出できる。Sample の扱いも `cross/ADR-0016` と concepts の sample-parity から導出できる適用判断であり、新しい決定を立てない。

## 未決の論点

なし。

申し送り: 複数選択で候補が 1 件も無い構成のとき、完了通知の引数が空列ではなく null になる (second-opinion code-001 で降格済み)。Sample は候補が常に存在するため実挙動には現れない。

## UI 素材

なし。既存の選択面 UI と native の確定通知をそのまま利用し、見た目は変更しない。新規 Sample 画面も既存 Cell の組み合わせで構成する。

## 変更級の推奨: M

MAUI の単一能力内で完結し UI 変更もないが、`PickerCell.SelectedCommand` という公開 API の小変更を伴うため M 級。Sample 反映と検証を加えても MAUI 系統に閉じ、native 2 面にも全 platform 共通文言にも触れないため据え置く。`samples-maui` のデルタスペックを追加する。

## 根拠

- `../AiForms.Maui.SettingsView/SettingsView/Cells/PickerCell.cs:242` — 移植元の `SelectedCommand` 公開面
- `../AiForms.Maui.SettingsView/SettingsView/Cells/PickerCell.cs:475` — 単一/複数選択ごとの実行引数
- `../AiForms.Maui.SettingsView/SettingsView/Pages/PickerPage.xaml.cs:117` — 選択反映後の Command 実行順
- `maui/KsSettingsView.Maui/PickerCell.cs:63` — 現行の選択 BindableProperty
- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1867` — native 確定通知の書き戻し入口
- `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:613` — iOS gateway の `nint` → `int` キャスト (platform 別の発火口)
- `maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:611` — Android gateway の発火口
- `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:51` — 追加テストが sink を直接呼んでいる箇所 (実経路を踏んでいない根拠)
- `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:78` — MAUI の「通知先メンバー」が TwoWay バインドで書かれている箇所
- `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:215` — iOS の同 Cell が `onItemsSelected` を使う箇所
- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:220` — Android の同 Cell が `onItemsSelected` を使う箇所
- `ios/Sources/KsSettingsViewUI/PickerCell.swift:175` — iOS の複数選択が `selectedIndices` のみを TwoWay で受ける公開面
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:330` — Android の同上
- `samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs:14` — `SampleScreenCategory.MauiSpecific` 区分の定義
- `kasane/decisions/maui/0008-aiforms-compatible-api-surface-policy.md` — 対応概念を AiForms 命名で公開する方針
- `kasane/decisions/maui/0012-interaction-value-transport-contract.md` — Picker 選択確定通知と書き戻し契約
- `kasane/concepts/cross/conventions/sample-parity.md` — Sample のパリティ規約と、対応概念が無い platform の例外
- `kasane/concepts/cross/conventions/runtime-behavior-verification.md` — 実環境での再現・確認と証跡の規約

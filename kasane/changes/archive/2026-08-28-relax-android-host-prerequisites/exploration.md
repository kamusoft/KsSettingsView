# Exploration: relax-android-host-prerequisites

起票日: 2026-08-26 / 起票元: rollout-user-skills のオーナー検収 (Android Skill の導入前提レビューで発覚)
探索開始: 2026-08-27

## 課題 / 動機

ks-settingsview の利用前提が Compose アプリ (メインターゲット) と噛み合っていない。オーナー判定: **両方の解消が必須**。

1. **XML テーマが `Theme.Material3.*` 派生であること**: ホストが MaterialSwitch / MaterialCheckBox を使い `?attr/materialSwitchStyle` 等を要求するため。Compose 専業アプリは XML テーマが最小限なのが普通で、この前提を満たさないと壊れる → 必要なスタイルを**ライブラリに同梱**し、アプリ側テーマに依存しない解決にする
2. **ホスト Activity が `FragmentActivity` であること**: TimePickerCell / DatePickerCell が Material ピッカーを DialogFragment で出すため。Compose テンプレートの標準は `ComponentActivity` であり、該当行をタップしても何も起きない → **ComponentActivity 対応を必須**とする

## 現状の裏取り (2026-08-27 調査)

### テーマ依存 (前提1)

- ライブラリは themes.xml / styles.xml を持たず、ホストテーマに全面依存。壊れ方は二層:
  - クラッシュ: `MaterialSwitch` / `MaterialCheckBox` の生成 (android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:249、CheckboxCellViewHolder.kt:91) は非 Material3 テーマで初期化時例外
  - 無言劣化: 色解決は `MaterialColors.getColor` の fallback 付きで、attr 欠落時は色ずれのみ
- Context 注入点は「ViewHolder 生成の `parent.context`」と「シート/ダイアログの `views.root.context`」の2系統に集約。ContextThemeWrapper での同梱テーマ適用は現実的
- DialogFragment (Material ピッカー) は Activity 側 Context で inflate されるため View 側のラップが届かない → ただしピッカー方針の決定 (下記) で DialogFragment 自体が消滅する

### FragmentActivity 依存 (前提2)

- 依存点は `findFragmentManager()` (TimePickerCellViewHolder.kt:210) の1関数のみ。null 時は全経路 `?: return` の完全な無言 no-op
- ボトムシート系 (PickerCell / NumberPickerCell / DatePicker の Spinner 型) は `BottomSheetDialog` 直接で FragmentManager 不要 — 代替経路の実績がライブラリ内に既存
- 回転復元 (android/ADR-0011) は FragmentManager + DialogFragment 前提で全面構築。シート系は復元対象外
- Web 裏取り (2026-08-27): `MaterialDatePicker` / `MaterialTimePicker` は DialogFragment サブクラスで正規の FragmentManager が必須 (内部で childFragmentManager 使用)。ComponentActivity で表示する公式手段は無し。`FragmentController` による自前 Fragment ホストは「public API だがサポートされたユースケースではない」領域で非推奨。世間の相場解は「AppCompatActivity にする」か「Compose Material3 の DatePicker を使う」
- Compose 経路の足場: appcompat 1.7.0+ は `AppCompatDialog` (BottomSheetDialog 含む) に ViewTree owner (Lifecycle / SavedStateRegistry / OnBackPressedDispatcher) を自動設定 — ComposeView を Dialog に載せる構成が成立する見込み (ViewModelStoreOwner は含まれない。実機検証は未)
- 依存の裏取り: ui モジュールは CustomCell の Compose ホスティングのため compose runtime / ui / foundation / material3 に依存済み (android/ADR-0016)。MAUI binding (maui/android/KsSettingsView.Binding.Android) も Xamarin.AndroidX.Compose.Runtime / UI / Foundation / Material3 / UI.ViewBinding と AppCompat 1.7.1.4 を配達済み — Compose DatePicker 路線に新規依存は不要で、MAUI からも追加負担なしで動作見込み

## 検討した選択肢 (却下案と理由を含む)

ComponentActivity でのピッカー表示方式:

- **FragmentManager が取れないホストのみシートに切替 (フォールバック)**: 却下。DatePickerCell はカレンダー/ホイールを利用者が選べるのが仕様であり、ホストの Activity 型で片方に固定されるのは致命的 (オーナー判定)
- **全ホストでシート表示に統一 (カレンダー廃止)**: 却下。同上 (カレンダー型の喪失)
- **DialogFragment をやめ Dialog 直接表示で Material ピッカー相当を再構築**: 却下。実装コスト最大
- **`FragmentController` / `FragmentHostCallback` で自前 Fragment ホストを合成**: 却下。公式サポート外の高リスク領域
- **ホストに AppCompatActivity を要求し続ける**: 却下。ComponentActivity 対応必須のオーナー要件に反する
- **FragmentActivity は MaterialDatePicker 維持、ComponentActivity のみ Compose 版 (併用)**: 却下。カレンダー実装2系統 (配色・今日ジャンプ・復元) の恒久保守になり機能追加が全て2倍になる
- **TimePicker を Compose Material3 TimePicker (時計ダイヤル) にする**: 却下。時計ダイヤル自体が使いにくいという評価

## 決定事項

1. **TimePickerCell は全ホストでボトムシート + 時分ホイールに統一する** (MaterialTimePicker の時計ダイヤル廃止)。OS の時計ダイヤルは使いにくく、iOS のホイール型との対照性も確保できる (2026-08-27 オーナー確定) → ADR-0018
2. **DatePickerCell のカレンダー型は Compose Material3 DatePicker のダイアログ表示に全ホスト統一する** (MaterialDatePicker 廃止)。カレンダー/ホイール選択可の仕様を全ホストで維持しつつ FragmentActivity 依存を完全撤去 (2026-08-27 オーナー確定) → ADR-0019
3. 上記により FragmentActivity 依存 (`findFragmentManager` と Fragment ベース回転復元機構) は撤去可能になる
4. maui サンプルの MainActivity はテンプレート既定 (`Maui.SplashTheme`) へ戻す (rollout-user-skills 検収からの受け入れ条件)
5. **テーマ非依存化は同梱 Material3 派生テーマの常時ラップで行う** (全ホストで挙動同一。既定 accent はラップ前のホスト Context から `colorPrimary` を解決して追従を維持) (2026-08-27 オーナー確定) → ADR-0020
6. 無言劣化の可視化 (旧論点): 決定 1・2・5 により無言 no-op (FragmentManager 欠落) と attr 欠落の色ずれが構造ごと消えるため、**追加の検知・ログ機構は設けない**
7. **ホストテーマからの色引き継ぎは完全に行わない**: 現行で唯一の反映だった ButtonCell タイトル既定の `colorPrimary` 動的解決も廃止し固定既定色へ統一 (統一性とコスト優先)。accent 既定のホスト primary 追従化は有望だが今回は見送り (2026-08-27 オーナー確定、ADR-0020 に訂正注記)

## ADR 候補

- 作成済み (accepted, 2026-08-27): android/ADR-0018 (TimePicker ホイール統一, supersedes 0006)、android/ADR-0019 (DatePicker カレンダーの Compose 統一, supersedes 0008/0010/0011)
- 作成済み (proposed, 2026-08-27): android/ADR-0020 (同梱テーマ常時ラップ)

## 未決の論点 (いずれも探索レベルの決定は済み。propose の設計・実装事項)

1. **回転復元の新方式**: Compose DatePicker ダイアログの表示中状態の保存 + rememberSaveable による復元設計
2. **実測スパイク**: ComposeView-in-Dialog (ComponentActivity ホスト) の ViewTree owner 自動設定と回転挙動の実機確認 — 実装着手時の最初のタスクにする
3. **今日ジャンプの再実装**: 旧 ADR-0010 の機能を Compose ダイアログ側でどう提供するか (状態操作で正面から実現する想定)
4. **MAUI の版整合規律**: Gradle 側 compose 版と Xamarin.AndroidX.Compose.* 版のズレ管理 (csproj の Material 1.12 ピンと同様のコメント整備)
5. **同梱テーマの中身**: EffectiveStyle.kt:490 (appcompat `colorPrimary` 参照) と material attr 参照の両名前空間の扱い、`bottomSheetDialogTheme` の同梱テーマ側定義

## UI 素材 (ui/references/ の一覧と注釈)

- なし (時刻ホイールシートは ksn-propose でモック作成予定)

## MAUI 側の受け入れ条件 (rollout-user-skills 検収から統合、2026-08-26)

- MAUI テンプレート既定の `MainActivity` (`Theme = "@style/Maui.SplashTheme"`) を**一切書き換えずに**全 Cell が正常動作すること。スタイル同梱によりアプリ側テーマへの Material3 要求を消すのが本筋
- 現在の samples/maui/KsSettingsView.Sample.Maui/Platforms/Android/MainActivity.cs は SplashTheme を Material3 テーマで置き換えており (オーナー却下済みの形)、本 change でテンプレート既定へ戻すこと
- 解消後、skills/ の Android/MAUI 導入前提の記述は docs-refresh で追従する

## 変更級: L (2026-08-27 オーナー確定)

判定材料: 実装領域5つ (TimePicker シート新設 / DatePicker Compose 化 / テーマ常時ラップ基盤 / 回転復元の撤去+新設 / MAUI サンプル復帰)、公開前提の撤廃 + 既存ホストへの視覚的破壊的変更、UI 新設 (モック承認あり)、accepted ADR 4本の supersede、実測スパイクあり。単一ゴール (ホスト前提の撤廃) に従属するためロードマップ分割はせず L 級1本。

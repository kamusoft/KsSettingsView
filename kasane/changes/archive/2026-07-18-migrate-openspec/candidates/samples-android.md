# Candidate: samples-android

## 概念候補

独立した概念候補はない。

Android Sample は、ライブラリの公開経路を実行可能な形で例示し、実機・エミュレータ上で目視確認するための利用側アプリである。ただし、その画面一覧、操作手順、使用する Cell、API 呼び出し、ファイル構成はすべてコードから再導出でき、変更頻度も高い。したがって L2 の独立概念にはせず、必要なら Sample 全般の目的を既存の開発ワークフロー／アーキテクチャ概念へ短く合流させるに留める。

出典: `samples/android/app/src/main/`、`samples/android/settings.gradle.kts`、`samples/android/README.md`、`docs/platform-guide-android.md`

## ADR 候補

- **Android Sample は本体 Android ライブラリを Gradle composite build でソース参照する** — Sample と本体を独立した Gradle build のまま接続し、公開 GAV から included build の 3 モジュールへ明示的に置換する。Sample から本体へステップインできる開発ループを意図している。出典: `samples/android/settings.gradle.kts`、`samples/android/README.md`「本体ライブラリのデバッグ」、`openspec/specs/samples-android/spec.md`「Android Sample アプリの存在 / KsSettingsView パッケージへの依存」。選別基準: **能力・コンポーネント境界を越える**、**将来のビルド／配布方式を制約する**。
- **Android の UI ホストは Material3 派生テーマを使用する** — UI 層が Material3 のテーマ属性を必要とするウィジェットを生成するため、Sample を含むホストアプリ側で Material3 派生テーマを提供する。Sample 固有ではなく Android UI ホスト全体の決定として統合するのが妥当。出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt`、`samples/android/app/src/main/AndroidManifest.xml`、`openspec/specs/samples-android/spec.md`「Material3 派生テーマの使用」。選別基準: **能力・コンポーネント境界を越える**、**将来のホストテーマ選定を制約する**。

## drift 所見

- spec の Purpose は `TBD` のままだが、実装と README からは「公開 API の利用例と、ライブラリ UI の手動・視覚的確認を提供する実行可能なクライアント」という目的が明確に存在する。(`openspec/specs/samples-android/spec.md` / `samples/android/README.md` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt`)
- 「基本 Cell を含むデモ画面」Requirement 本文は起動直後に設定一覧を表示するとしているが、実装の開始先はトップメニューであり、Store 方式デモは利用者が選択した後に表示される。同 Requirement 内の「起動時の画面表示」Scenario は実装側と同じくメニュー選択を前提としており、spec 内でも表現が不整合。(`openspec/specs/samples-android/spec.md` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt`)
- MAUI 互換 Theme のフィールド列挙に旧名 `viewBackgroundColor` / `titleColor` が残っている。現行コードと Android ガイドは `backgroundColor` / `cellTitleColor` を使用する。(`openspec/specs/samples-android/spec.md` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/BasicCellsDemoScreen.kt` / `docs/platform-guide-android.md`)
- Material3 Requirement は RadioCell の内部実装を `AppCompatRadioButton` とし、Scenario でも ring/dot 描画を要求しているが、現行 UI 層は独自の単純チェック表示を使用する。Material3 制約自体は `MaterialSwitch` により現在も生きているが、RadioCell を根拠とする説明は腐っている。(`openspec/specs/samples-android/spec.md` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RadioCellViewHolder.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt`)
- JVM ターゲット確認 Scenario は `kotlinOptions.jvmTarget` の存在を要求するが、現行 Sample は Java の source/target compatibility と Kotlin の JVM toolchain で 17 を指定している。要求する結果は満たしている一方、検証方法が現行 Gradle 構成と一致しない。(`openspec/specs/samples-android/spec.md` / `samples/android/app/build.gradle.kts`)
- README はトップメニューを 3 画面と説明し、ディレクトリ構成にも 2 つのデモ実装しか掲載していない。実装には Store、DSL、基本 Cell、共通フィールド、可視性、入力 Cell の 6 経路がある。Android ガイドの Sample 一覧も 4 経路のみで、Store と入力 Cell を欠く。(`samples/android/README.md` / `docs/platform-guide-android.md` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt`)
- Android ガイドは利用者定義 Cell の「詳細実装」として `samples/android/` を案内しているが、Sample 側に独自 Cell 型や ViewHolder の実装はなく、公開 Cell の利用例だけがある。(`docs/platform-guide-android.md`「利用者定義 Cell の登録」 / `samples/android/app/src/main/kotlin/`)

## 用語

- **Android Sample**: Android ライブラリの利用側として動作し、公開経路の例示と手動・視覚的確認に使う実行可能アプリ。
- **composite build**: Sample と本体を別々の Gradle build として保ちつつ、Sample の外部モジュール依存を本体のローカル project へ置換する接続方式。
- **DSL 方式**: Compose の宣言的なツリー記述を外部状態から再評価して表示へ反映する利用経路。
- **Store 方式**: 保持した設定ツリーへ命令的な更新を発行し、差分として表示へ反映する利用経路。
- **目視確認**: 自動テストではなく、実機またはエミュレータ上で描画・操作・アニメーションを観察する検証。

## 抽出メモ

- **件数**: 概念候補 0 件、ADR 候補 2 件、drift 所見 7 件。
- Sample 自身には `src/test` / `src/androidTest` がない。現行の生きた自動契約は、依存先 3 モジュールの unit / Robolectric テストにあり、Sample はそれらを組み合わせた手動統合・視覚確認の場になっている。
- 関連テストでは、Compose と Store の両経路、基本 Cell、入力 Cell、可視性、差分適用、テーマ適用が確認されている。これらの挙動はそれぞれの Android capability へ帰属し、Sample の概念候補には重複させない。
- Material3 制約は Sample より `settings-view-android-host` または Android テーマ境界の概念／決定へ、composite build は開発ワークフローまたはモノレポ構成の決定へ統合するのが自然。ただし統合・確定は指揮側に委ねる。
- `docs/overview.md` には Android core をプラットフォーム非依存かつ Kotlin stdlib のみとする説明がある一方、現行 core module は Android Library で Compose Runtime と Android framework 型に依存する。この乖離は `samples-android` の責務外なので件数には含めず、該当するアーキテクチャ capability 側で扱うべき所見として残す。
- エミュレータ起動、描画結果、iOS Sample との一字一句の一致は、この抽出では実行・比較していない。コード静的照合で未確認のため、drift と断定していない。

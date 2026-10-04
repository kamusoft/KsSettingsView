# Exploration: sample-api35-status-bar-and-edittext

## 課題 / 動機

起票元: ../KsDialogs/kasane/changes/archive/2026-09-27-add-page-layout-area/

**Android Sample の API 35 で見つかった 2 件の修正 (ステータスバーのアイコンが白地に溶ける / AndroidView の EditText の初期値が出ない)**

### 何を変えたか

KsDialogs の add-page-layout-area で Sample の撮影証跡を API 35 の AVD で撮り直したとき、Android Native と KMP Android の Sample で次の 2 件が見つかり、同じ change の付随修正として直した (記録: `kasane/changes/archive/2026-09-27-add-page-layout-area/deviation.md` の [付随修正] 2 項)。

1. **ステータスバーのアイコンが見えない**: API 35 (targetSdk 35 の edge-to-edge 強制) で、明るい背景の画面でもステータスバーのアイコン (時刻・電波・電池) が白で描かれ、白地に溶けて見えなかった。メニュー画面にも以前からあった。API 31 / 33 では出ていない
   - 対処: `MainActivity.kt` で API 35 以上のときだけ `WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true` にした (`samples/android/.../MainActivity.kt`、`samples/kmp/androidApp/.../MainActivity.kt`)
   - 副作用として、撮影証跡の状態違いの 2 枚が画素まで一致してしまう問題 (アイコンが見えず差が出ない) も消えた
2. **`AndroidView` に載せた `EditText` の初期値が表示されない**: Compose の画面に `AndroidView` で載せた数値入力欄 (負の値を打てるように既存の `EditText` を使っている) で、API 35 だと初期値「0」が見えなかった。既定の横スクロールが、寸法が決まる前の文字配置を残していたのが原因
   - 対処: `setHorizontallyScrolling(false)` と `maxLines = 1` を指定した (`samples/android/.../SampleOffsetField.kt`、`samples/kmp/androidApp/.../SampleOffsetField.kt`)

### 相手に関係する理由

relations の overlap の `samples` に当たる。KsSettingsView も同じ Native + MAUI 構成の Android Sample (Android Native / KMP Android) を持ち、Sample を撮影証跡の検証装置として使っているため、同じ targetSdk・同じ作りなら同じ症状が出ている可能性がある。特に 1 は、撮影証跡のステータスバー部分に効く。

### 提案する対応

API 35 のエミュレータ (または実機) で KsSettingsView の Android Sample を開き、明るい背景の画面でステータスバーのアイコンが見えるか、`AndroidView` 経由の入力欄があれば初期値が出ているかを確かめる。同じ症状があれば上の対処が参考になる。採るかどうか・直し方は KsSettingsView 側の判断に任せる。

なお、同じ時期に KsDialogs のライブラリ本体でも「API 35 で透明な覆いのダイアログを出している間、ステータスバーのアイコンが白地に溶ける」ように見える現象があり、別途調べた (`kasane/changes/archive/2026-09-27-fix-android-container-system-bar-appearance/`)。原因は、テスト用の画面が暗いアイコンを指定していなかったこと (テストの測り方) と、ライブラリの Loading / Toast の器が画面のシステムバーの指定を引き継いでいなかったことで、上の Sample の件とは別の系統だった。overlap に当たる範囲の話ではないため、KsSettingsView への追いの知らせは無い。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)

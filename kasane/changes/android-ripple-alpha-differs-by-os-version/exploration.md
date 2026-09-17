# Exploration: android-ripple-alpha-differs-by-os-version

## 課題 / 動機

Android で Cell を押したときのタッチフィードバック (リップル。色は `Theme.selectedColor`) の濃さが、同じ色を渡しても OS の版によって違って見える。これは Android フレームワーク側 (`RippleDrawable`) の版ごとの仕様差によるもので、ライブラリのコードの不具合ではない。

オーナーの意向は、この仕様を**このライブラリの concepts に知識として残すこと**。コード変更が主目的ではない (進め方によっては change を経ず ksn-concept での直接入力で済む)。

発見の文脈: 依存側リポジトリ KsAppKMP のリファレンスアプリで KsSettingsView を採用した際、同じビルド (`#007AFF` の不透明度 45% を `selectedColor` に指定) を入れた Pixel 6a (Android 16) では濃いめ、Pixel 4a (Android 13) では薄めに見えた。Android 16 では不透明度 15% は見えず、30% は薄く、100% は濃すぎた。経緯は `../KsAppKMP/kasane/changes/kssettingsview-adoption/deviation.md` (「押下時の地色」の各行) と同 change の `ui/brief.md` の照合記録にある。

### 既存の記述との関係

- `kasane/concepts/core/styling/cell-visual-states.md` は「Android は enabled 行の `RippleDrawable` に `Theme.selectedColor`」とだけ書き、版による濃さの違いは書いていない
- `kasane/concepts/core/styling/style-resolution.md` に既定値 (`selectedColor` はライト `#D9D9D9` / ダーク `#2C2C2E`) がある
- 進行中の change に同じ題材は無い (`ios-cell-highlight-delayed-on-touchdown` は iOS の押下ハイライトのタイミングの話で別題材。ただし「押下時のフィードバックの OS 間の体感差」という点で隣接する)

### 分かっている事実 (起点側で AOSP の版ごとのソースとコミットで裏取り済み、2026-09-17)

- **ライブラリ側の組み方**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt` で Cell の背景を `RippleDrawable(ColorStateList.valueOf(selectedColor), ColorDrawable(backgroundColor), null)` の形で組む (mask は null)。`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt` も `rippleColor` に `theme.selectedColor` を使う
- **描画方式の切り替わり**: Android 12 以降の `RippleDrawable` は波が広がる方式 (`STYLE_PATTERNED`) が既定 (`FORCE_PATTERNED_STYLE = true`)。Android 11 以前はべた塗りの方式のみ。style を切り替える API は非公開で、アプリ・ライブラリから固定できない
- **不透明度の二重適用**: 波が広がる方式ではシェーダ (`RippleShader`) が色の不透明度 (`in_color.a`) を掛ける。Android 13 までは Paint 側にも同じ不透明度が設定され、二重に掛かっていた。Android 14 で修正 (AOSP commit f0c528a "Fix RippleDrawable alpha" — Paint は不透明にしてシェーダだけで扱う)
- **50% 前後の丸め (`clampAlpha`)**:
  - Android 11 以前と 12: 50% 超を 50% へ引き下げ
  - Android 13 のみ: 50% 未満を 50% へ引き上げ (commit 475648a "Fix faint ripple")
  - Android 14 以降: 丸め自体を削除 (commit 7b08444 "Remove alpha clamping from ripple drawable")
- **帰結**: 同じ `selectedColor` でも描かれる濃さが版で違う
  - 例: 不透明度 45% を渡すと、Android 13 は 50% に引き上げたうえで二重に掛かり約 25%、Android 14 以降は約 45%
  - Android 12 は最大でも 0.5 × 0.5 = 約 25% が上限
  - Android 14 以降は渡した値に線形に効く (不透明な色を渡すとべた塗りに近くなる)
- **押下保持中も少し薄い**: 押下を保持している間もシェーダの fade が 1.0 に達しない (約 0.83 付近) ため、渡した不透明度より少し薄く描かれる
- **実機の観察と一致**: 上記「発見の文脈」の Pixel 6a (Android 16) / Pixel 4a (Android 13) の見え方は、この版差で説明がつく
- **iOS との差**: iOS は `selectedColor` を押下中の背景にそのまま塗る (`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` の `installSelectedColorHandler`) ので、渡した値どおりの濃さになる。同じ値を両 OS に渡すと Android のほうが薄く見える版がある
- **公式の推奨は無い**: 版をまたいで濃さを揃える公式の推奨は見つからなかった (developer.android.com の Android 12 の behavior changes にもリップル刷新の記述なし。追えるのは AOSP のコミットのみ)
- 一次情報: aosp-mirror/platform_frameworks_base の android11〜16-release ブランチの `graphics/java/android/graphics/drawable/RippleDrawable.java` と `RippleShader.java`、および上記 3 commit

## 検討した選択肢 (却下案と理由を含む)

(未検討。起点側の調査から導出した手当ての候補を「未決の論点」に列挙している)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)
- **進め方の選択肢** (オーナーの意向は知識の定着が主):
  - ksn-concept での直接入力に進む — 版差の仕様を concepts (候補: `kasane/concepts/core/styling/cell-visual-states.md` の Android 行の補足、または Android 側の concepts) に書き、この change は破棄する。コード変更が不要と決まった場合の最短経路
  - この change を通常の探索 (ksn-explore Step 1〜) で深掘りし、コード側の手当てが要るかを決める。手当てを入れるなら concepts の追随は蒸留 (ksn-distill) で行う
- **ライブラリ側で取り得る手当ての候補** (公式の裏付けは無く、調査からの導出):
  - (a) 知識として concepts と利用者向け文書に残すだけ (`Theme.selectedColor` の既定値と、利用者が色を選ぶときの注意)。利用者向け文書 (`skills/` と README 群) への反映は docs-refresh 経由
  - (b) OS の版で渡す不透明度を出し分ける (見た目を揃えられる可能性があるが、版ごとの係数を持ち続ける保守コストと、fade の約 0.83 まで含めてどこまで揃えるかの判断が要る)
  - (c) リップルをやめて押下時の背景を単色で塗る (版差は消え iOS とも揃うが、Material の見た目から外れる)
- **既定値での見え方 (未検証)**: `Theme.selectedColor` の既定は `#D9D9D9` (不透明のグレー)。不透明な色は Android 13 以前で 50% 付近に丸められたうえ二重に掛かり、14 以降はそのまま描かれるため、既定値でも版で見え方が違うはず。ダークの既定 `#2C2C2E` も同様。実機で確かめる
- **未確認の点** (起点側の調査で追えていない):
  - ネイティブ側 (`RecordingCanvas.drawRipple`) で Paint の不透明度がどう乗るかのコード (修正 commit のメッセージは二重適用を明言している)
  - Android 10〜11 のべた塗り方式での実効の濃さ
  - 各 release ブランチがその版の最終更新かどうか (後続のパッチで挙動が変わっていないか)
- 隣接題材: `ios-cell-highlight-delayed-on-touchdown` (iOS の押下ハイライトの出るタイミング)。押下時フィードバックの OS 間の見え方をまとめて concepts に書くなら、同じ節で扱うかを探索時に決める

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定

(a) だけなら change ではなく ksn-concept の領分。(b) なら Android 系統の小変更 (暫定 S / android)、(c) は Cell の見た目の取り決めが変わり iOS との整合も論点になるため M 以上の見込み。

# レビュー結果: fix-default-colors-dark-appearance (003 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

2 周目の指摘 3 件 (Major 1 / Minor 1 / Suggestion 1) はすべて解消した。Major の「行の ViewHolder が生成時 Context のテーマ属性を保持する」は、`EntryCellViewHolder` が hint 色の解決元 Context を同一性で照合して引き直す形と、`SwitchCellViewHolder` が attr の解決元を `ksThemedContext()` へ差し替える形で閉じており、実経路の回帰テスト 2 本と撮り直した証跡の両方で確認できた。実装側の「他に同種の捕捉は無い」という主張も、本体ソースをテーマ属性の読み取り (`MaterialColors` / `obtainStyledAttributes` / `R.attr.` / tint 設定) で横断走査して裏を取った — 該当は 3 箇所だけで、残る 1 箇所 (`SheetChrome`) は選択面の生成経路であり、提示のたびに新しい同梱テーマ付き Context を受け取るため対象外である。

修正で入った新規の欠陥は見つからなかった。残るのは、この周で書き直した placeholder 周りのコメントが解決元を「ホストテーマ」と説明している点 (実際の解決元は同梱テーマで、ホストテーマ前提は android/ADR-0020 で撤廃済み) の Minor 1 件と、副作用を持つ関数が getter の名前を持つ Suggestion 1 件で、いずれも挙動に影響しない。

### 実行した検証

- Android: `ANDROID_HOME=<SDK> ./gradlew test --rerun-tasks` (`android/`) — 216 の結果 XML を集計して **2810 tests / 0 failures / 0 errors / 0 skipped** (debug + release、`124 actionable tasks: 124 executed` で全件再実行を確認)。新規の回帰 2 本 (`placeholder 未指定の行の hint 色は夜間モードへの切替後に引き直される` / `Switch のオフ色は夜間モードへの切替後に引き直される`) が debug / release 両方の結果 XML に存在し成功している
- iOS: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,id=<iPhone 17 Pro>'` — `** TEST SUCCEEDED **`。保持したログ末尾で UI バンドル 666 tests / 0 failures。iOS は本周でテストコメントとテスト名のみの変更で、全バンドルの内訳は 2 周目に 1021 / 0 で確認済み
- lint: `scripts/comment-policy-lint.py` 禁止 0 件 (778 ファイル)、`scripts/local-path-lint.py` 0 件、`scripts/identity-lint.py` 0 件
- 足場凍結: `specs/` / `proposal.md` / `design.md` / `exploration.md` / `ui/mock/` に diff なし
- 証跡: 撮り直した 2 枚と新規 2 枚を自分で開いて確認した (下の「2 周目 Major の解消」を参照)

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (always) |
| cross/test-execution.md | テスト実行・テスト結果の報告 |
| cross/runtime-behavior-verification.md | 外観の実行時切替の完了判定 |
| cross/sample-parity.md | `samples/android` の `SampleTheme` を触る |
| cross/local-development-setup.md | Android SDK の解決先を確かめてテストを実行する |
| ios/swift6-language-mode-check.md | `ios/Sources/` を触る変更の完了判定 |
| core/ADR-0030 (proposed) / core/ADR-0009 / android/ADR-0017 / android/ADR-0020 / cross/ADR-0016 | 本 change の設計根拠と隣接決定 |
| concepts/core/styling/style-resolution.md | 実効値解決の現行記述 |
| lessons/code-review.md (L-001) / lessons/process.md (L-002・L-003・L-005・L-006) | 昇格済みルール |

core / android のドメイン handbook は存在しない。`cross/public-identifiers.md` / `release-procedure.md` / `user-skill-api-listing.md` / `aiforms-origin-reference.md` は担当範囲に当たらないため本文まで読んでいない。

なお `kotlin-impl-skill` (config の `domain-skills.android.code-review`) の観点は、null 安全 (`ColorStateList?` の扱い)・可視性 (追加した拡張関数は `private`)・不変性 (再解決が必要な 2 フィールドだけ `var` へ変えている) の順に当て、いずれも適合と判定した。

## 2 周目指摘の解消状況

| 出所 | 指摘 | 判定 | 根拠 |
|---|---|---|---|
| review-002 [Major] | 行の ViewHolder が生成時 Context のテーマ属性を保持し live 切替に追随しない | **解消** | 下の詳細を参照 |
| review-002 [Minor] | iOS テストコメントの履歴記述 3 箇所 | **解消** | `ThemeDefaultColorAppearanceTests.swift:61` は `// MARK: - ライト外観の既定色は固定値`、`:63` は `test_既定色のライト解決値が固定値と一致する`、`SectionAccessoryRenderingTests.swift:293` は「ライトはグレー（≒ #6D6D72）、ダークは dark セットのグレー（#8E8E93）に解決されることを期待する」へ書き換わった。本 change が新規追加・変更した iOS / Kotlin のテストを「本 change」「従来」で横断検索して残存 0 を確認 (他ヒットは本 diff の触れていない既存ファイル) |
| review-002 [Suggestion] | `ThemeTest.kt:30` の裸の変更識別子 | **解消** | 「オリジナル AiForms iOS / Android 踏襲の『Auto 高さ + 下限保証』を既定とする」へ置き換わり、識別子が消えている |
| second-opinion code-002 [Minor] | `ThemeDefaultColorAppearanceTests.swift:61` | **解消** | 上と同一箇所 |

### 2 周目 Major の解消 (詳細)

**実装**: `EntryCellViewHolder.kt:366-374` の `currentHostHintTextColors()` が `views.root.context.ksThemedContext()` を引き、`hostHintTextColorsSource` との同一性で解決済み値の古さを判定する。ずれていれば `android:textColorHint` を引き直し、あわせて適用済みの記録 (`appliedPlaceholderColor` / `placeholderColorApplied`) を落として次の適用を初回扱いへ戻す。`SwitchCellViewHolder.kt:164-174` は `MaterialColors.getColor` の第 1 引数を `switchView` から `views.root.context.ksThemedContext()` へ差し替えている。

**経路の確認**: `applyThemeInternal` → `notifyItemRangeChanged(0, itemCount, PAYLOAD_THEME)` は Cell 行を 2 引数版のフル `bind` へ落とすため、`bind` 内で無条件に呼ばれる `applyPlaceholderColor` / オフ色の導出がどちらも走る。行の View の Context は `buildCellBaseViews(parent.ksThemedContext())` で作られた `KsThemedContext` なので、外観が変わると `ksThemedContext()` はキャッシュから別インスタンスを返し、同一性の判定が成立する。ライト → ダーク → ライトと戻す場合も、行が持つ元のラッパは夜間モードが一致した時点で自身を返すため、識別子は正しく切り替わる。

**走査**: `android/kssettingsview/src/main/kotlin/` 全体を `MaterialColors` / `obtainStyledAttributes` / `resolveAttribute` / `R.attr.` / `ContextCompat` / `*TintList` で横断し、外観依存のテーマ属性を読む箇所を列挙した。

| 箇所 | 判定 |
|---|---|
| `EntryCellViewHolder.kt:523` (`android:textColorHint`) | 本修正で解消 |
| `SwitchCellViewHolder.kt:165,170` (`colorSurfaceContainerHighest` / `colorOutline`) | 本修正で解消 |
| `SheetChrome.kt:112` (`colorOnSurfaceVariant`) | 対象外。呼び出し元 4 つの選択面はいずれも `BottomSheetDialog(hostContext.ksThemedContext())` で提示のたびに構築されるため、常に現在の外観のラッパを受け取る |
| `CheckboxCellViewHolder.kt:42` / `SwitchCellViewHolder.kt:181-193` の tint | 対象外。実効 accent から組み立てており、テーマ属性を読まない |
| `CellBaseLayout.kt` の背景・ripple・文字色 | 対象外。すべて解決済み `EffectiveStyle` の値 |
| `SectionAccessoryViewHolders.kt` の Header / Footer | 対象外。色は解決済み Theme から。テーマ属性の読み取りは無い |

よって「他に同種の捕捉は無い」は成立する。

**テスト**: 追加の 2 本はいずれも実描画値 (`EditText.hintTextColors.defaultColor` / `MaterialSwitch.trackTintList` `thumbTintList` の非 checked 状態) を観測点に取り、切替後の値が「現在の外観の同梱テーマ付き Context が解決する値」と一致することと、ライトと夜間で実際に値が違うこと (空振り防止の逆アサート) の両方を固定している。`switchToNight` は `RuntimeEnvironment.setQualifiers("+night")` の後に `dispatchConfigurationChanged` を投げるため、Activity を再生成しないホストの経路そのものを踏んでいる。

**証跡**: `ui/verification/android-native-input-cells-live-dark-after.png` を開き、「ニックネーム (callback)」行の placeholder 「callback 経路で更新」が明るいグレーで判読できることを確認した (review-002 が実測した修正前の文字 #252220 / 地 #2A2620 の潰れは解消している)。同じ画面の「表示名」行 (placeholder 色を Cell 個別に指定) は指定色のまま。`ui/verification/android-native-unify-common-fields-live-dark-after.png` では SwitchCell「Wi-Fi のみ同期」のオフ track が暗色・thumb が明色でダーク側の明度になっている。4 枚のファイル更新時刻も本修正後 (2026-09-06) で、brief.md の「再撮影」節の記述と一致する。

## 指摘事項

### [🟡 Minor] placeholder の解決元を「ホストテーマ」と説明するコメントが、実際の解決元 (同梱テーマ) と食い違う

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:35`、`:70`、`:76`、`:193`、`:329-330`、`:344`、`:355`

**問題点**:

本周の修正で hint 色の解決元は `views.root.context.ksThemedContext()`、すなわち同梱テーマ `Theme.KsSettingsView.Internal` を被せた Context に明示された。同梱テーマの親 `Theme.Material3.DayNight.NoActionBar` は `android:textColorHint` を直接持つ (Material 1.12.0 の `Base.V14.Theme.Material3.Light` / `.Dark` がそれぞれ `@color/m3_hint_foreground` / `@color/m3_dark_hint_foreground` を設定する) ため、`ContextThemeWrapper` の属性解決はホストの値を必ず覆う。つまり解決元はホストテーマではない。

にもかかわらず、この周で書き直した / 追記したコメント群が解決元をホストテーマとして説明している。

- `:76` — 「ホストテーマが `android:textColorHint` を持つことを前提にする（Android Host が要求する `Theme.Material3.*` 派生テーマは必ず持つ）」。ホストへの要求そのものが存在しない (android/ADR-0020 の視覚隔離により同梱テーマを常時かぶせる方針で、`HostThemeIndependenceTest` は「素の AppCompat テーマのホスト」「テーマを被せない素の Context」「非 Material3 テーマのホスト」を明示的に通している)。同じパッケージの `EffectiveStyle.kt:454-455` は「4 段目はホストのテーマを参照せず、ライブラリが所有する外観別の既定値である。ライブラリ UI の配色はホストの XML テーマから隔離され」と正しい契約を書いており、ファイル間で矛盾している
- `:344` の「戻し先が無い（ホストテーマが hint 色を持たない）」も、同梱テーマが常に属性を持つ以上、成立しない前提で防御コードを説明している
- `:35` / `:70` / `:193` / `:329-330` / `:355` の「ホスト既定」「ホストテーマ由来」「ホストテーマ既定」も同様。フィールド名 `hostHintTextColors` と関数名 `currentHostHintTextColors` も同じ語を運んでいる

comment-policy「アーカイブされた過去仕様の説明」の禁止 (現在の仕様を現在形で書く) に当たる。実害としては、この説明を信じた後続の変更が解決元を `ksHostContext()` へ寄せると、android/ADR-0020 の視覚隔離を破ってホストテーマの hint 色が漏れる。挙動には影響しないため優先度は低い。

**推奨修正**: 解決元を「同梱テーマ付き Context が解決する `android:textColorHint`」と書き直し、`:76` のホスト前提の段落は削除する (同梱テーマは常に属性を持つため、`null` 分岐は防御であると書けば足りる)。あわせて `hostHintTextColors` / `currentHostHintTextColors` / `hostDefault` の識別子も `themeHintTextColors` 等へ寄せると、名前と契約が一致する。`:355` の要約行も「現在の外観の同梱テーマが解決する hint 色を返す」で足りる。

なお凍結中の `specs/settings-view-android-ui/spec.md:31` にも「placeholder のホスト hint 色」という括弧書きがあるが、同 Requirement の規範部分 (両セットでも `Unspecified` のまま / platform 既定へ落ちる) は満たされており、挙動の逸脱ではないため spec 側の指摘としては扱わない。蒸留で concepts へ書き起こすときに解決元を同梱テーマとして記述するのが妥当と考える。

### [🔵 Suggestion] getter の名前を持つ関数が適用済み状態をリセットする

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:366-374`

**問題点**: `currentHostHintTextColors()` は値を返すだけの名前だが、解決元がずれていたときに `hostHintTextColors` / `hostHintTextColorsSource` に加えて `appliedPlaceholderColor` / `placeholderColorApplied` まで書き換える。このリセットは正しさに必要 (落とさないと `applyPlaceholderColor` の差分判定が「未指定のまま変化なし」と見なして古い色を残す) で KDoc にも書かれているが、名前からは読み取れない。`:338` は「差分判定の前に引く」というコメントで呼び出し順の意図を守っているものの、将来この関数をログや別の分岐から素直に「現在値の取得」として呼ぶと、意図せず適用済み記録が落ちる。

**推奨修正**: 副作用を名前に出す (`refreshHostHintTextColorsIfStale()` 等) か、「古さの判定と引き直し」と「現在値の取得」を 2 つに割って、`applyPlaceholderColor` と `unbind` の側で前者を呼んでから後者を読む形にする。

## 確認して問題が無かった観点

- **仕様充足**: 2 周目から仕様面の変更は無い。settings-view-android-ui の「夜間モード変更時の未指定色の再解決」が求める「表示中の行への再適用」は、本修正で行のテーマ属性まで含めて成立した
- **tasks.md の虚偽チェック**: 未チェックは 9.1 (蒸留への申し送り) のみで、2 周目から増減なし。8.5 の照合記録は `ui/brief.md` に「再撮影 (2026-09-06、review-002 Major の修正後)」節として追記され、期待値の置き方 (「同じ外観で新規生成した ViewHolder が示す値」を規範として採る) と撮影条件・撮り直した / 追加したファイル・個人要素の確認まで書かれている
- **deviation**: 7 件の乖離と 5 件の `[付随修正]` に増減なし。本周の修正は review 指摘への対応であり、新たな仕様逸脱を含まない
- **再解決のコスト**: 外観切替後は毎 bind で `themeBaseContext()` の巻き戻しと `synchronized` なキャッシュ参照が走る (行の Context が古いラッパのままのため早期 return に入らない)。`EntryCell` は打鍵ごとに再 bind される経路を持つが、参照はキャッシュヒットで終わり `obtainStyledAttributes` は最初の 1 回だけなので、同 bind 内の `EffectiveStyle.from` に比べて無視できる
- **キャッシュとリーク**: `hostHintTextColorsSource` は `KsThemedContext` を強参照するが、ViewHolder は `views` 経由で同じ Activity を既に保持しており、保持関係は増えない。`themedContextCache` は夜間モードごとに 1 エントリで置き換わり、値は `WeakReference` のまま
- **ライト → ダーク → ライトの往復**: 行が持つ元のラッパは夜間モードが一致した時点で自身を返すため、戻したときの解決元は元のラッパに戻る。同梱テーマは `editTextStyle` を上書きしないので、構築時に `EditText` が持っていた hint 色と `hintTextColorsFromTheme()` の結果は同一の属性に由来し、往復で値がずれない
- **再適用のトリガー**: `onConfigurationChanged` は `themeBacking.resolvedFor(darkTheme)` の同値判定を挟まず無条件に `applyThemeInternal` を呼ぶため、全色を明示指定した利用者でも行の再 bind は走り、テーマ属性由来の値が取り残されない
- **iOS**: 本周の変更はテストコメントとテスト名のみ。`SectionBoxDecorationView` の trait 追随 (iOS 17+ の `registerForTraitChanges` と iOS 16 フォールバック) は 2 周目から変更なし
- **テストの手抜き (lessons L-001)**: 新規 2 本はどちらも実描画値を観測し、逆アサートで空振りを防いでいる。オフ色は HSL の明度成分で attr との対応を見ており、accent の色相を載せる導出 (android/ADR-0017) と両立する形になっている
- **証跡 (lessons L-003)**: 視覚に影響する修正の後に該当面を撮り直しており、撮っていない範囲 (選択面の開き直し・他の面) と、修正前ビルドでの A/B を取っていないことが brief.md に明記されている
- **sample-parity**: 本周で `samples/` に差分は増えていない

## アクションプラン

1. (任意・蒸留前が望ましい) `EntryCellViewHolder` の placeholder 周りのコメントと識別子から「ホストテーマ」の語を落とし、解決元を同梱テーマとして書き直す。あわせて `:76` のホスト前提の段落を削除する
2. (任意) `currentHostHintTextColors()` の副作用を名前に出すか、判定と取得に分割する
3. 蒸留時、concepts へ placeholder の解決契約を書き起こすときは解決元を同梱テーマとして記述する

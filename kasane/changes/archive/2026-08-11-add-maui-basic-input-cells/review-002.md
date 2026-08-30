# レビュー結果: add-maui-basic-input-cells (002 回目)

**日付**: 2026-08-11
**判定**: APPROVED

## サマリー

review-001 の指摘 11 件はすべて指摘の趣旨を満たして解消されている。特に Major-1 (Android パスワードマスク) は `passwordInputType` による variation 上書き方式で 4 つの InputType クラスすべてを正しく処理しており、追加テストは実装の写しではなく Robolectric の実 `transformationMethod` を観測しているため回帰検出力がある。Minor-4 (BG8605) は実装ワーカーの反証が正しく、**私の review-001 の推奨が機構的に誤っていた** — 根拠を実測で確認した。

新たに持ち込まれた問題として、`KsImageLease` の破棄時機が native への反映より先に走る箇所が 3 つある (Minor-10)。ただし影響するのは image loader 経路 (Uri / Stream) の画像に限られ、サンプル・テストが通る file / resource 経路は破棄処理が no-op であることを MAUI アセンブリの IL 解析で確認した。Critical / Major はなく、残る指摘は Minor 1 件と Suggestion 2 件のため APPROVED とする。

## 検証したビルド・テスト (すべて実測)

| 対象 | 結果 | 前回比 | 報告値との一致 |
|---|---|---|---|
| MAUI (`net10.0`) | 237 / 237 成功 | +10 | ✅ |
| iOS (`KsSettingsView-Package`) | 711 / 711 成功 | +1 | ✅ |
| Android (`./gradlew test --rerun-tasks`) | 2164 / 2164 成功 | +12 | ✅ |
| facade 3 TFM ビルド | 0 エラー / 0 警告 | 変化なし | — |
| Android Binding rebuild | 0 エラー (BG8605 系のみ) | 変化なし | — |
| `comment-policy-lint.py --summary` | 禁止 0 件 (479 ファイル) | 変化なし | — |

iOS の集計について補足: `^Test Case .* passed` の行数は 710 になるが、これは `MemoryLeakTests.test_KsSettingsViewControllerはスコープを抜けるとdeinitされる` の結果行が `CHHapticPattern` のシステムログと同一行に連結されて出力されたための計数漏れで、当該テストは `passed (0.046 seconds)` を実出力している。バンドル別集計 (Bridge 101 / Core 83 / SwiftUI 76 / UI 451 = 711) が正。**テストの消失・スキップではない**ことを確認した。

---

## 修正 11 件の検証

### ✅ Major-1: Android パスワードマスク — 完全に解消

`EntryCellViewHolder.kt:238-249` の `passwordInputType` が variation フィールドを上書きする方式になった。全クラスの結果を手計算で検証した。

| keyboard | 旧 (OR 合成) | 新 | フレームワーク判定 |
|---|---|---|---|
| Default `0x01` | `0x81` ✓ | `0x81` | `isPasswordInputType` ✓ |
| Url `0x11` | `0x91` ✗ | `0x81` | ✓ |
| Email `0x21` | `0xA1` ✗ | `0x81` | ✓ |
| Numeric `0x2002` | `0x2082` ✗ | `0x2012` | `isNumberPasswordInputType` ✓ (DECIMAL フラグ保持) |
| Phone `0x03` | `0x83` ✗ | `0x81` (TEXT へ倒す) | ✓ |

`TYPE_CLASS_PHONE` にパスワード variation が存在しないためテキストクラスへ倒す判断は妥当で、コメントにも明記されている。

**テストの検出力**: `InputCellsTest.kt:123-197` は `vh.editText.transformationMethod is PasswordTransformationMethod` を見ている。これは実装の写しではなくフレームワークが `inputType` から導出する実際の状態であり、旧実装 (`0xA1` 等) では `isPasswordInputType` / `isNumberPasswordInputType` のいずれの等値比較にも一致しないため確実に落ちる。複数行フラグ保持のケースも含めて 4 テスト追加されており、手抜きはない。deviation.md にスコープ追加の記録もある。

### ✅ Major-2: sample-parity の deviation 記録 — 解消

`deviation.md` 冒頭に「ニックネーム (callback)」の項が追加され、(1) 何が一致不可能か (2) なぜ文言を据え置くか (3) 本体側の統一課題として後続で扱うこと、が記述されている。規約条項 (「一致不可能箇所の deviation 記録」) への参照もあり、sample-parity の要求を満たす。

### ✅ Minor-3: `PickerCell.SelectedItem` の stale 値 — 解消

`PickerCell.cs:242-262` で `SyncIndexFromSelectedItem` が `SelectedIndex` に加えて `SelectedItem` も導出値へ揃えるようになった。テストも `SettingUnknownSelectedItemClearsIndex` に `SelectedItem, Is.Null` の assert が追加され、review-001 の PROBE-A に対応する `SettingSelectedItemWithoutItemsSourceLeavesUnselected` が新設されている。spec の SHALL を満たす。

### ✅ Minor-4: BG8605 容認判断 — **反証を受理。私の指摘が誤りだった**

実装ワーカーの実測 (`review-handoff.md` #1 の追記) を独立に検証した。

- `KsSettingsView.Binding.Android.csproj:32-33` で `ks-settingsview-core` / `ks-settingsview-ui` の aar が `Bind="false"` であることを確認 — 解決集合に入らないため enum 型が引けない、という根本原因の説明は正しい
- 警告の出力元が `obj/Debug/net10.0-android/api.xml.class-parse` であることは review-001 で私自身が観測済み。class-parse は **fixup / 除去より前の段階**で signature 解決の診断を出すため、メンバーの可視性 (`@JvmSynthetic` による `ACC_SYNTHETIC`) では抑止されない — 機構として整合する
- 私が review-001 で `@JvmSynthetic` を根拠にした前例 (`makeCell` 等) は、いずれも `internal` による `$` マングリングを**同時に**受けており、synthetic 単独の効果を示す証拠になっていなかった。推論の誤り

効果のない注釈を残さず取り消した判断、および判断根拠を実測へ差し替えた記録の残し方はいずれも適切。**容認継続を支持する。**

### ✅ Minor-5: `KsBridgePickerCell.WhenMappings` — 解消

`KsBridgePickerCell.kt:54-58` で enum の `when` が `if` に置き換えられ、合成クラスが生成されなくなった。Binding を rebuild して `api.xml` を実読し、`WhenMappings` が `KsSettingsBridge` の 1 件のみに減っていることを確認した。`review-handoff.md` #2 の「変更由来ではない」という誤りも訂正されている。

### ✅ Minor-6: native 既定値の導出化 — 解消 (両OS 8 箇所)

リテラル複製が全廃され、native インスタンスからの導出になった。

- iOS: `KsBridgeDatePickerCell.swift:47-55` (`nativeDefaults()` から format / uiStyle)、`KsBridgeTimePickerCell.swift:34`、`KsBridgeEntryCell.swift:44`、`KsBridgeButtonCell.swift:28`
- Android: `KsBridgeDatePickerCell.kt:90,93`、`KsBridgeTimePickerCell.kt:65`、`KsBridgeEntryCell.kt:83`、`KsBridgeButtonCell.kt:60`

いずれも `static let` / `private val` (companion) で 1 回だけ評価され、生成する捨てインスタンスは副作用のない値モデル。native 既定が変われば Bridge も自動追随するため、乖離が無検出になる問題は解消した。

### ✅ Minor-7: `CellStyle` 13 項目の 1:1 テスト — 解消 (両OS)

Android `KsBridgeCellConversionTest.kt:400-433`、iOS `KsBridgeCellConversionTests.swift:270-304` に、13 項目すべてへ相異なる値を入れて変換後を突き合わせるテストが追加された。色は `0xFF010203` 〜 `0xFF0D0E0F` と項目ごとに別値になっており、引数の取り違えが確実に検出される。

### ✅ Minor-8: トートロジー 2 件 — 解消

- `KsBridgeCellConversionTest.kt:197-200`: 被検証関数の呼び出しをやめ `InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS` の直書きに変更
- `KsBridgeSectionHeaderHeightTest.kt:18-26`: `-1.0` を直書きで固定する専用テストを新設し、期待値の導出をやめた。「Native 既定が変わったときに検出できるようにするため」という意図もコメントに残っている

### ✅ Minor-9: `App.cs` の裸参照 — 解消

`(sample-parity)` → `(cross/ADR-0016)` に置換され、同一 change 内の他ファイルと表記が揃った。

### ✅ 相方 Major: アイコン世代番号の再利用 — 解消

`KsSettingsController.cs:83` に controller 寿命を通じて単調増加する `_iconRequestSequence` が導入され、`ResolveIcon` は `++_iconRequestSequence` を払い出す。Cell ごとに数え直す実装では、Cell を外して入れ直したときに古い要求と番号が衝突して追い抜かれた結果を最新と誤判定していた。

**テストの検出力**: `IconSourceTests.ResolutionsStartedBeforeReRegistrationDoNotWin` は stale な解決を 3 件残したうえで全件を後から完了させる構成で、「どの番号が衝突するかに依らず検出できる」設計になっている。旧実装 (Cell ごとに数え直し) では stale-2 の世代が再登録後の最新と一致して上書きするため確実に落ちる。良い回帰テスト。

### ✅ 相方 Major: `IImageSourceServiceResult` の破棄契約 — 概ね解消 (時機に Minor-10)

`KsImageLease` が画像と後片付けの口を一体で持ち、採用されなかった結果・置換された結果・登録解除・解決口切り替えのすべてで破棄されるようになった。`FakeImageResolver.DisposeProbe` による破棄観測テストが 6 件追加され、「破棄される側」と「破棄されない側」の両方を assert しているためトートロジーではない。破棄時機については Minor-10 を参照。

### ✅ 相方 Major: `DataTemplateSelector` の `NotSupportedException` 変換 — 解消 (Suggestion-2 あり)

`KsItemsSourceBinder.cs:358-368` で MAUI が投げる `NotSupportedException` を `InvalidOperationException` へ包み直し、元の例外を `InnerException` に保持している。spec の「既存 DataTemplate 経路と同じ例外契約」を満たす形になった。

---

## 新規の指摘

### [🟡 Minor-10] リースの破棄が native への反映より先に走る

**該当箇所**
- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:855-864` (`StoreIcon`)
- 同 `:1198-1201` (`UnregisterCell`) — 呼び出し元 `RemoveCells` は `UnregisterCell` の**後**に `_gateway.RemoveCell` を呼ぶ
- 同 `:368-372` (`ClearRegistrations`) — 呼び出し元 `RebuildRoot` は破棄の**後**に `_gateway.SetRoot` を呼ぶ

**問題点**

`StoreIcon` は `previous?.Dispose()` を実行してから `MarkContentDirty(cell)` を呼ぶ。実際に native へ新しい画像が届くのは flush (dispatcher の次のターン) なので、**後片付けが走ってから置換が反映されるまでの間、native は破棄済みリースの画像を表示し続ける**。`UnregisterCell` / `ClearRegistrations` も同様に「破棄 → native 側の除去・再構築」の順になっている。

MAUI アセンブリを逆アセンブルして破棄の実体を確認した。

```
// Microsoft.Maui.ImageSourceServiceResult::Dispose()
IL_0011:  ldfld  System.Action ImageSourceServiceResult::_dispose
IL_001c:  callvirt instance void System.Action::Invoke()
```

`Dispose()` は `Value` に触れず、サービスが登録した `Action` を呼ぶだけ。そしてその `Action` の有無は経路で分かれる。

- `FileImageSourceService` (resource / file 経路、Android): `ImageSourceServiceResult::.ctor(Drawable, Action)` に `ldnull` を渡す → **破棄は no-op**
- `ImageLoaderResultCallback::OnSuccess(Drawable drawable, Action dispose)` (image loader 経路 — Uri / Stream 等): 実体のある `Action` を結果へ渡す → **破棄でローダー側の後片付けが走る**

つまり、サンプルとテストが通る file / resource 経路では実害がなく (だから緑になる)、`UriImageSource` / `StreamImageSource` を使ったときだけ、置換・除去が画面へ出る前にローダーの後片付けが走る窓がある。最悪ケースは Android での bitmap リサイクルによる表示欠けである。

**推奨修正**

`UnregisterCell` 経路と `RebuildRoot` 経路は、gateway 呼び出しの後に破棄する順序へ入れ替えるだけで解消する (数行)。`StoreIcon` は置換されたリースを保留リストへ積み、`Flush()` で `ReplaceCell` / `ReplaceCells` を送った後にまとめて破棄する形が素直。

本変更で新たに導入された機構であり、到達経路が現状のサンプル・テストに無いため APPROVED は妨げないが、**アーカイブ前に対処するか、蒸留で追跡対象として残すことを推奨する**。

### [🔵 Suggestion-1] `NotSupportedException` の捕捉範囲が広い

**該当箇所** `maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs:359-368`

`selector.SelectTemplate(item, _container)` 全体を `try` で囲んでいるため、利用者の `OnSelectTemplate` が自分の都合で投げた `NotSupportedException` も「DataTemplateSelector must not return another DataTemplateSelector.」に翻訳される。元の例外は `InnerException` に残るのでデバッグは可能だが、メッセージは誤誘導になる。MAUI 側の例外メッセージで絞り込むか、この誤訳可能性を doc コメントに書き添えると親切。

### [🔵 Suggestion-2] resolver の `catch` がリースを破棄しない

**該当箇所** `Platforms/iOS/KsImageResolver.cs:50-53` / `Platforms/Android/KsImageResolver.cs:51-54`

`catch (Exception) { lease = null; }` は、リース生成後に例外が起きた場合に生成済みリース (と結果の後片付け) を捨てる。現状 `new KsImageLease(...)` の後に例外源が無いため到達しないが、破棄契約を導入した以上 `lease?.Dispose()` を添えておくと意図が明確になる。

---

## 保留 3 点の評価 (依頼 3)

### (a) `ReleaseHost` でリースを破棄しない (既存契約優先) — **支持する**

`ReleaseHost` は `_imageGeneration++` と `_images = null` は行うが、`_icons` のリースは破棄しない。これは正しい。maui/ADR-0007 により Native Host 解放後も Store と設定ツリーの状態は生き続け、再訪問時は Store 現在状態から表示が復元される。このとき Bridge の DTO は解決済み platform 画像を保持したままであり、`ReleaseHost` で後片付けを走らせると**復元表示のアイコンが壊れる**。実際 `IconSourceTests.ReconnectResolvesCurrentIconAgain` は再接続で新しい解決口から解決し直され、そこで `StoreIcon` が旧リースを破棄する経路になっており、後片付けは遅れて確実に行われる。既存契約を優先した判断は妥当。

補足: ページを恒久的に離れて再訪問しない場合、`KsImageLease` にファイナライザが無いため後片付けは走らないまま GC される。修正前は破棄自体が無かったので後退ではないが、蒸留メモに残す価値はある。

### (b) 同一画像インスタンスを複数リースが包む理論的余地 — **実在するが受容可。記録を推奨**

`StoreIcon` は `previous` と `lease` が別インスタンスなら `previous.Dispose()` を実行する。両者が同じ platform 画像 (サービスがキャッシュした同一 `UIImage` / `Drawable`) を指している場合、`previous` の後片付けが共有画像に影響し得る。実装は `ReferenceEquals(previous?.Image, lease?.Image)` で dirty 化は正しく抑止しているが、破棄は抑止していない。

同じ問題は Cell をまたいでも起こる (2 つの Cell が同一 `ImageSource` を持ち、サービスが同一インスタンスを返す場合)。ただし file / font サービスは呼び出しごとに結果を作り、破棄 Action は null なので実害はない。ローダー経路のキャッシュ挙動に依存する低確率の穴であり、**今回の受容は妥当**。Minor-10 と同じ蒸留メモにまとめて残すのが良い。

### (c) `@JvmSynthetic` の注釈取り消し — **支持する**

実測で効果がないと確定した以上、注釈を残すと「これで抑止されている」という誤読を生む。取り消して判断根拠を `review-handoff.md` #1 の実測記録へ差し替えた対応は適切。ファイルに注釈が残っていないことも確認した。

---

## 確認して問題がなかった観点 (再確認分)

- **足場アーティファクトの保全**: `proposal.md` / `design.md` / `specs/` は今回も無変更。更新は `deviation.md` (2 行追加、合意済み) と `artifacts/review-handoff.md` (#1 / #2 / #7 の訂正追記) のみで、いずれも記録の正確化にあたる
- **`review-handoff.md` #7 の訂正**: review-001 で指摘した記述の誤り (5 種すべてが実際には `ValueText` を持つ) が取り消し線付きで訂正され、#15 の配置判断との整合も明記された
- **修正の波及**: 変更されたのは icon 経路・PickerCell の導出・Bridge の既定値導出・テストのみで、書き戻し経路・cellId 温存・delegate 寿命・Theme 経路には手が入っていない。既存 227 件のテストが 1 件も落ちていないことがこれを裏付ける
- **`KsImageLease` 自体の実装**: 破棄が冪等 (`_disposed` ガード)、`Image` は不変、`handle` は optional。設計は素直で問題ない
- **導出方式の副作用**: 8 箇所の `nativeDefaults()` はいずれも静的初期化 1 回で、生成対象は副作用のない値モデル。初期化順序の循環も無い

---

## アクションプラン

APPROVED のため必須の差し戻しは無い。以下は任意の後続対応。

1. **Minor-10 (リース破棄の時機)** — `UnregisterCell` / `RebuildRoot` 経路は gateway 呼び出し後へ順序入れ替えするだけで解消するため、アーカイブ前に対処する価値が高い。`StoreIcon` の遅延破棄まで含めるか、蒸留で追跡対象にするかはオーナー判断
2. **保留 (b) の記録** — 同一画像インスタンスの共有破棄について、Minor-10 とあわせて蒸留メモまたは concepts の icon 実体化の節へ残す
3. **Suggestion-1 / Suggestion-2** — 蒸留・後続へ
4. review-001 の Suggestion 群 (README の `docs-refresh` 追従、`ks-settingsview-compose` の flaky テスト追跡ほか) は申し送り済みのため、蒸留時に取りこぼしがないか確認する

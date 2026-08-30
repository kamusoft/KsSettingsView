# レビュー結果: fix-maui-entrycell-focus-loss (002 回目)

**日付**: 2026-08-11
**判定**: APPROVED

## サマリー

review-001 の指摘 5 件 (Major 2 / Minor 2 / Suggestion 1) はいずれも解消されている。無限制約時は `base.GetDesiredSize` へ委譲する形になり、Major-1 で問題にした「無警告で高さ 0 になる」経路は消えた。制約解決の純ロジックは `KsFillMeasure` として全 TFM 共通の internal ヘルパへ切り出され、net10.0 ユニットテスト 11 件が追加されている (全体 252 件 / 失敗 0)。実機証跡も change 配下に揃い、`dumpsys` の `.F` フラグ追跡という review-001 時点より強い形の A/B になっている。ADR-0014 も起票済み。

追加のミューテーション実測で、新規テストが実際に回帰を検出できることを確認した (下記)。残る指摘は Minor 1 件・Suggestion 2 件で、いずれも実装の正しさに関わらない文書・証跡の追跡性の話であり、承認を妨げない。

---

## review-001 指摘の解消判定

| 指摘 | 判定 | 根拠 |
|---|---|---|
| 🟠 Major-1 無限制約で 0 を返し SettingsView が消える | **解消** | `SettingsViewHandler.cs:47-58` — `KsFillMeasure.CanResolve` が両制約とも `double.IsFinite` のときだけ true を返し、偽なら `base.GetDesiredSize(...)` へ委譲。合意方針 (「一方でも非有限 (∞/NaN) なら override せず修正前と同一経路」) と実装が完全に一致。`double.IsFinite` は NaN と ±∞ の両方を弾くため、NaN 制約も自動的にフォールバックへ回る |
| 🟠 Major-2 実機証跡が change 配下にない | **解消** | round-1 分 12 点 (`verify-device-03〜16-17-side.png`)、round-2 分 5 点 + `verify-device-fix2-dumpsys.txt` が change 配下に配置済み。索引の不在のみ Minor として下記に残す |
| 🟡 Minor-1 純ロジックが Android TFM 専用でテスト不能 | **解消** | `maui/KsSettingsView.Maui/Internals/KsFillMeasure.cs` へ移設。`Internals/` は全 TFM 共通のコンパイル対象 (`Compile Remove` の対象は `Platforms/**` のみ) であり、`InternalsVisibleTo` 済みのテストプロジェクトから参照できる。maui/ADR-0009 の seam の趣旨どおり |
| 🟡 Minor-2 measure 契約の ADR が未起票 | **解消** | `kasane/decisions/maui/0014-android-host-measure-contract-fill-with-unbounded-fallback.md` (proposed) を起票、`decisions/maui/index.md` にも行を追加済み。Decision の 2 分岐は実装と一致し、Alternatives に却下案 4 つ (全面 fill / native 層正規化 / requestFocus 対症療法 / 書き戻し抑止) が残されている |
| 🔵 Suggestion MAUI 本体との微差 (0 下限 / NaN / PlatformView null) | **解消** | NaN は `CanResolve` が弾くため到達しない。0 下限は `NegativeConstraintHasNoLength` で意図をテストに明示。`PlatformView` null 時の差は、非有限でない限り制約から即答する設計として ADR-0014 に明文化された |

---

## 新規コードの評価

### MAUI 本体との等価性

`KsFillMeasure.ResolveLength` の 3 分岐は、MAUI 10.0.70 相当の `Microsoft.Maui.Platform.ContextExtensions.CreateMeasureSpec(Context, constraint, explicitSize, minimumSize, maximumSize)` が組み立てる constraint 値と一致する (review-001 で本家ソースを確認済み。round-2 で計算式に変更はない)。

有限制約に限定されたことで、等価性の主張も明確になった:

- **明示指定あり** → 本体は `Exactly` spec を作る。Exactly では Android の measure 結果 = spec 値なので、platform へ降りずに同値を返すのは厳密に等価
- **最大指定 < 制約 / 制約のみ** → 本体は `AtMost` spec を作り、measure 結果は「制約以下の内容サイズ」になり得る。ここが唯一の意味論的差分 (= fill 化) であり、ADR-0014 の Decision がその差分そのものを規定している
- **非有限** → override しないので本体そのまま

### テスト

`FillMeasureTests.cs` 11 件。境界値カバレッジは十分:

- `CanResolve`: 有限 / 0 / ∞ 片側 2 通り / ∞ 両側 / NaN 片側 2 通り
- `ResolveLength`: 指定なし / 明示 (制約より大・小) / 明示 + 最小 (効く・効かない) / 明示 + 最大 (最小より後に効くことを `(100, min 150, max 120) → 120` で固定) / 最大のみ (制約より小・大) / 最小のみ / 非有限 (∞ / NaN) / 非有限 + 明示・最大 / 負の制約

**検出力の実測 (lessons code-review L-001 のミューテーション法)**: `KsFillMeasure.cs` を backup したうえで 3 通りの改変を入れ、テストが落ちることを確認した。

| ミューテーション | 結果 |
|---|---|
| `CanResolve` の `&&` → `\|\|` | 失敗 1 / 252 |
| 明示指定時の最大クランプ (`Math.Min`) を除去 | 失敗 1 / 252 |
| 明示指定時の最小クランプ (`Math.Max`) を除去 | 失敗 2 / 252 |

いずれも改変前は全 pass、改変後にその分岐のテストだけが落ちる。トートロジーではなく回帰検出力があることを実測で確認した。改変は毎回 backup から復元し、`shasum` 一致 (`71f30b6bff3b1491895b9750cbaa2ea08fa1236b`) で原状復帰を確認済み。

### コメントポリシー

`comment-policy-lint.py` は `git ls-files` ベースで untracked を走査しないため、新規 2 ファイルは自動検査の対象外だった。共有ロジック `comment_policy_rules.scan_text` を 3 ファイルへ直接かけて **禁止 0 件**を確認した。change-id / Phase / review 通番 / spec パス / `MUST` 等の混入なし、日本語で自己完結しており規約準拠。

### 設計品質

- `CanResolve` / `ResolveLength` の 2 メソッド分割は責務が明確で、handler 側は「引き受けられるか判定 → 引き受ける or 委譲」の 3 行に収まっている
- handler 側 remarks が「制約が定まっていない方向がある場合だけは既定の問い合わせに委ねる (この配置では上記の経路が残る)」と、残存経路まで正直に書いている点は good
- `Dimension` のエイリアス using が Android ファイルから消え、`Microsoft.Maui` 1 本の追加で済んでいる

---

## 実機証跡の妥当性

`verify-device-fix2-dumpsys.txt` が本 round の中核証跡で、review-001 時点より質が上がっている:

- 名前欄タップ後 → ASCII `a`〜`e` の 5 打鍵 → BackSpace 5 回の計 11 時点すべてで `mServedView` が **同一 EditText インスタンス (`2377337`)** かつ **`.F` フラグ維持**
- フレームが全時点で `103,0-996,124` (幅 893px) で固定 = 幅ゼロ化が発生していない。exploration.md の A/B 表 (修正前は 1 文字目で `.F` 喪失) と直接対応する形になっている

スクリーンショットも整合: `fix2-02` は `Tanaka T` に `abcde` を追記して `Tanaka Tabcde` + 末尾キャレット、`fix2-03` は BackSpace 5 回で `Tanaka T` に戻っている。`fix2-04` / `fix2-05` の基本 Cell デモは表示・スクロールとも正常で回帰なし。

**日本語 IME を round-2 で再取得していない件は妥当**と判断する。サンプルの SettingsView は `Grid RowDefinitions="Auto,*"` の `*` 行 (幅も暗黙 `*`) に置かれており、幅・高さとも有限制約で降りてくる。round-1 と round-2 で有限制約時の計算式 (`ResolveLength`) は 1 文字も変わっておらず、round-2 の追加は非有限時のフォールバック分岐のみ。したがって round-1 の IME 証跡 (`verify-device-09/10/12/13-crop.png`) が指す経路と round-2 の実行経路は同一である。

無限制約配置の実機未検証も、フォールバック先が `base.GetDesiredSize` = 修正前と同一経路であることをコード上で確認できるため、既知事項として許容できる (ADR-0014 Consequences に記載済み)。

---

## 指摘事項

### [🟡 Minor] round-1 分の証跡に索引がなく、crop 画像の対応が追えない

**該当箇所**: `kasane/changes/fix-maui-entrycell-focus-loss/verify-device-06-crop.png`, `-07-crop.png`, `-09-crop.png`, `-10-crop.png`, `-12-crop.png`, `-13-crop.png`

**問題点**: round-2 分は `fix2-02-ascii-continuous` のようにファイル名が確認項目を語っているが、round-1 分の crop 6 点は連番だけで内容が分からない。実際にはこの 6 点が日本語 IME の composing → 変換 → 確定 → 確定後継続入力をカバーする最重要証跡であり、「IME の確認はどれで取ったのか」を change 配下だけでは判別できない。アーカイブ後の蒸留・再検証で価値を落とす。

**推奨修正**: change 配下に `evidence.md` (または README) を 1 枚置き、各画像 1 行で確認項目との対応を書く。あるいは round-2 と同じ命名規則へリネームする (`verify-device-09-kana-composing.png` 等)。

### [🔵 Suggestion] ADR-0014 の「OS 間で割れない」の適用範囲

**該当箇所**: `kasane/decisions/maui/0014-android-host-measure-contract-fill-with-unbounded-fallback.md:25`

**問題点**: 「無限制約時の挙動が両 OS で platform measure に揃うため、同一 XAML の配置結果が OS 間で割れない」とあるが、揃うのは無限制約の場合だけである。有限制約 + 非 Fill の配置 (`VerticalOptions="Start"` / `Center` / `End` を明示した場合) では、Android は制約いっぱいを返し、iOS は `SizeThatFits` 由来の値を返すため、desired size は一致しない。既定の `Fill` では表示結果が変わらないため実害は小さいが、文面は「無限制約については割れない」と読める範囲に限定した方が誤読を防げる。

**推奨修正**: status が `proposed` のうちに Consequences へ「有限制約 + 非 Fill 配置では desired size が OS 間で一致しない」を 1 行足す。蒸留時の確定作業でまとめても構わない。

### [🔵 Suggestion] `ResolveLength` の非有限分岐は production からは到達しない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsFillMeasure.cs:56`、`maui/KsSettingsView.Maui.Tests/FillMeasureTests.cs:85-103`

**問題点**: handler は `CanResolve` が true のときしか `ResolveLength` を呼ばないため、`double.IsFinite(constraint) ? ... : 0d` の false 側と、それを検証する 2 テスト (`NonFiniteConstraintHasNoLength` / `SpecifiedSizeStillAppliesWithoutAConstraint`) は現状の呼び出し経路からは到達しない。関数単体の完全性としては自然だが、将来の読み手が「無限制約では 0 を返す仕様」と誤読しうる (それは Major-1 で却下された案そのもの)。

**推奨修正**: 必須ではない。`ResolveLength` の remarks に「呼び出し側は `CanResolve` で有限制約を保証してから使う」旨を 1 文足すか、該当テストの doc comment に「関数単体の防御的な既定値であり、handler はこの経路を通らない」と書き添えるだけで誤読は防げる。

### [🔵 Suggestion] 新規ファイルは comment-policy lint の自動検査を受けていない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsFillMeasure.cs`, `maui/KsSettingsView.Maui.Tests/FillMeasureTests.cs`

**問題点**: `scripts/comment-policy-lint.py` は `git ls-files` で対象を決めるため、untracked のままでは走査されない (今回のレビューでは `comment_policy_rules.scan_text` を直接呼んで 0 件を確認済み)。commit 前に「lint 緑」を根拠にすると、新規ファイルだけ検査されていない状態になり得る。

**推奨修正**: `git add` 後に `python3 scripts/comment-policy-lint.py --summary` を再実行して緑を取り直す。

---

## 確認した観点 (指摘なしの範囲)

- **ビルド**: `net10.0-android` / `net10.0-ios` の両 TFM で成功、警告 0 / エラー 0。iOS 側は無変更 (`Compile Remove="Platforms/**/*.cs"` + TFM 別 Include により Android 実体は iOS ビルドに入らない) で、`KsFillMeasure` が共通コンパイル対象へ入っても iOS の挙動に影響しない
- **テスト**: `dotnet test KsSettingsView.Maui.Tests` → **252 件 / 失敗 0** (round-1 の 241 件 + 新規 11 件)。既存 241 件に退行なし
- **ミューテーション実測**: 上記 3 通りすべてで想定どおりのテストのみ失敗、`shasum` で原状復帰確認済み
- **足場の凍結**: `git status -uall` で確認したところ、`exploration.md` は untracked のまま無改変。`review-001.md` も改変なし。既存の kasane 文書の変更は `decisions/maui/index.md` への 1 行追記のみで、ADR 起票に伴う正当な更新
- **無断の仕様逸脱**: なし。合意方針からの逸脱は確認されず、`deviation.md` を要する乖離も見当たらない
- **スコープ外事象**: round-1 で観測されたキャレット位置ずれは round-2 の証跡 (`fix2-02`) では再現しておらず、今回の変更が悪化させる構造も差分に含まれない

---

## アクションプラン

1. **[Minor]** 証跡索引 (`evidence.md`) の追加、または round-1 crop 6 点のリネーム。アーカイブ前に済ませておくと蒸留が楽になる
2. **[Suggestion]** ADR-0014 Consequences への 1 行追記 (有限制約 + 非 Fill での OS 差)。`proposed` の確定作業とまとめてよい
3. **[Suggestion]** `ResolveLength` remarks への呼び出し前提の明示
4. **[Suggestion]** commit 後に comment-policy lint を再実行

いずれも実装の正しさに関わらないため、1〜4 を実施せずアーカイブへ進む判断も許容できる (その場合 1 は蒸留時に回収すること)。

---

## クローズ記録 (2026-08-11、レビュアー確認)

- **[🟡 Minor] 証跡索引**: クローズ。`evidence.md` を確認 — round-1 12 点・round-2 6 点の全ファイルが 1 行ずつ内容と対応づけられ、指摘の中心だった crop 6 点 (06/07/09/10/12/13) も「中間キャレット挿入」「IME composing / 確定 / 確定後継続」まで判別できる。検証端末・アプリ・フォーカス判定方法 (`mServedView` の `.F`) の前提、IME 再取得不要の根拠、修正前記録の所在 (exploration.md) も揃っており、change 単体で証跡が読める状態になった。
- **[🔵 Suggestion] ADR-0014 の適用範囲**: クローズ。Consequences に負の項目として「OS 間の挙動一致が保たれるのは無限制約の配置に限る。有限制約かつ非 Fill 配置では Android=制約 fill / iOS=`SizeThatFits` 由来で一致しない (Fill 前提のコントロールとして許容)」が追記され、Decision 本文の「割れない」が無限制約に限った主張であることが読み取れるようになった。
- **[🔵 Suggestion] `ResolveLength` の非有限分岐**: クローズ。remarks に「呼び出し側は `CanResolve` が true の制約に対して使う前提であり、非有限の制約に対する 0 は『無限制約では 0 とする』仕様ではなく防御の既定値」が追記され、Major-1 で却下された案との取り違えを防げる形になった。コメント規約スキャンも 0 件。
- **[🔵 Suggestion] commit 後の lint 再実行**: 未実施 (commit 後作業として完了報告へ引き継ぎ)。
- 追記後の再確認: `dotnet test KsSettingsView.Maui.Tests` → **252 件 / 失敗 0**。コード変更はコメントのみで挙動に影響なし。判定 APPROVED は維持。

# レビュー結果: fix-maui-entrycell-focus-loss (001 回目)

**日付**: 2026-08-11
**判定**: CHANGES_REQUESTED

## サマリー

修正の狙い (MAUI handler 層で measure 契約を閉じ、Android の auto-measure による measure 中 layout 経路を消す) は原因分析と整合しており、`ResolveLength` の分岐は MAUI 本体 `ContextExtensions.CreateMeasureSpec` の制約組み立て規則と厳密に一致している。ビルド緑・net10.0 ユニットテスト 241 件全 pass・comment-policy lint 0 件で、コード品質そのものに問題はない。

一方、**高さ制約が無限で降りてくる親 (VerticalStackLayout / ScrollView / Grid の Auto 行) に置いたとき Android の SettingsView が高さ 0 になる**という、公開挙動の後退が新たに入っている (iOS は従来どおりのため platform 非対称)。あわせて、実機検証の証跡が change 配下に無い (session scratchpad のみ) 点が `runtime-behavior-verification` 規約に抵触する。この 2 点を Major として差し戻す。

---

## 指摘事項

### [🟠 Major] 制約が無限の方向で 0 を返すため、SettingsView が高さ 0 で消える親配置がある

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/Android/SettingsViewHandler.cs:88`

```csharp
return double.IsInfinity(constraint) ? 0d : Math.Max(0d, constraint);
```

**問題点**:

- MAUI の親レイアウトは、子の「内容ぶんの大きさ」を知りたい方向に `double.PositiveInfinity` を渡す。代表例は `VerticalStackLayout` (子を常に高さ無限で measure する)、縦 `ScrollView` の content、`Grid` の `Auto` 行/列。
- 変更前は、この場合 MAUI 本体が `MeasureSpecMode.Unspecified` で platform を measure し、Host (`KsSettingsView` = `FrameLayout` + 内部 `RecyclerView`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:53`) の auto-measure が内容高さを返していた。変更後は無条件に `0` を返すため、**そのまま高さ 0 で配置され、SettingsView が画面から消える**。エラーも警告も出ないため利用者からは原因が分からない。
- 本リポジトリは UI コントロールライブラリであり、`ContentPage` 直下 / `Grid` の `*` 行以外の配置も利用者コードでは普通に起こる。Sample・`maui/tests/KsSettingsView.MauiHost/SettingsPage.xaml` はいずれも `*` 行かページ直下のみなので、「サンプル他ページの回帰なし」の確認ではこの経路は踏めていない。
- iOS 側 (`maui/KsSettingsView.Maui/Platforms/iOS/SettingsViewHandler.cs`) は `GetDesiredSize` を上書きしていないため、同じ XAML が iOS では内容高さ・Android では 0 になる。cross-platform facade の契約が platform で割れる。

なお、探索で合意された案A の「SettingsView は割当領域を fill する」という方針自体を否定するものではない。合意スコープに含まれていたのは fill 方針までで、**「満たすべき領域が無限のときどうするか」は exploration の未決論点 (「非 EXACT spec が飛んでくる正確な条件 — 修正実装時に確認」) のまま実装で 0 に確定している**。この確定が無警告の消失を生む点を差し戻す。

**推奨修正** (いずれか):

1. 無限制約の方向だけ `base.GetDesiredSize`(= platform measure) にフォールバックする。フォーカス喪失経路は「内容サイズを問い合わせる親に置いた場合」に限定され、既定のページ配置では完全に消える。
2. 直近の arrange で確定した大きさを保持し、無限制約ではそれを返す (初回は 0 のままなので 1 フレーム遅れる)。
3. 0 を返す仕様で確定するなら、**無警告の消失にしない**こと。少なくとも `concepts/maui/api/maui-facade.md` の「してはいけないこと・制約」へ「高さ制約を与えない親 (VerticalStackLayout / ScrollView / Grid Auto 行) には置けない」を明記し、iOS を同じ契約に揃えるか、揃えない理由を残す。Debug ビルドでの警告ログも検討に値する。

### [🟠 Major] 実機検証の証跡が change 配下に残っていない

**該当箇所**: `kasane/changes/fix-maui-entrycell-focus-loss/` (exploration.md のみ)

**問題点**:

- `concepts/cross/conventions/runtime-behavior-verification.md` は完了条件の 3 番目として「証跡 (スクリーンショット・ログ等) を change 配下に残す。レビューと蒸留が『解消した』の主張を検証できる形にする」を定めている。
- 実際の証跡 (01〜17 のスクリーンショット、`jdb_out.txt`、`viewdump.txt`) は session scratchpad (`/private/tmp/claude-501/.../scratchpad/`) にしか無く、change 配下には 1 件も無い。scratchpad は session 固有かつ一時領域であり、アーカイブ後に検証不能になる。
- 内容自体は今回レビューで確認し、完了条件をカバーしていると判断した (下記「確認した観点」参照)。問題は所在だけである。

**推奨修正**: 完了条件に対応するスクリーンショット (ASCII 連続入力 / BackSpace 連続 / IME 変換・確定・確定後継続 / 他ページ回帰) を `kasane/changes/fix-maui-entrycell-focus-loss/evidence/` へコピーし、どの画像がどの確認項目に対応するかを 1 行ずつ記した索引を添える。

### [🟡 Minor] `ResolveLength` が Android TFM 専用ファイルにあり、自動テストがゼロ

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/Android/SettingsViewHandler.cs:71-89`

**問題点**:

- `ResolveLength` は 4 分岐を持つ純粋関数で、依存は `Microsoft.Maui.Primitives.Dimension` のみ — platform 非依存であり素の net10.0 でコンパイル・実行できる。
- にもかかわらず `Platforms/Android/` 配下にあるため `net10.0` のユニットテストプロジェクト (`KsSettingsView.Maui.Tests`、TFM は net10.0 単独) のコンパイル対象に入らず、**回帰検出が実機目視だけ**になっている。maui/ADR-0009 が定めた「純ロジックは platform 非依存側に置き net10.0 ユニットテストで網羅する」という seam の趣旨から外れる。
- 分岐 (明示指定 + min/max クランプ / max < constraint / 無限 / それ以外) はいずれも境界値テストが安価に書ける。

**推奨修正**: `ResolveLength` を `KsSettingsView.Maui/Internals/` 等の全 TFM 共通の internal static ヘルパへ移し (`InternalsVisibleTo` は既に設定済み)、Android 側は `GetDesiredSize` の組み立てだけを持つ。移した先で 4 分岐の境界値テストを追加する。上記 Major-1 の挙動 (無限制約時に何を返すか) も、この形なら仕様としてテストに固定できる。

### [🟡 Minor] measure 契約の ADR が未起票で、公開契約の文書にも記載がない

**該当箇所**: `kasane/decisions/maui/`、`kasane/concepts/maui/api/maui-facade.md`

**問題点**:

- exploration.md は「修正方針が確定した時点で『MAUI Host の measure 契約』に関する ADR を起票する価値が高い (境界を越える契約であり、覆すコストが高い)」と明記しているが、ADR は起票されていない。
- 「SettingsView は割り当て領域を満たし、Native Host の measure へは降りない」は Handler と Native Host の境界をまたぐ契約であり、後から `base.GetDesiredSize` へ戻すと今回の不具合が再発する。覆すコストが高い判断であり、ADR の選別基準に合致する。
- `concepts/maui/api/maui-facade.md` にも大きさ・レイアウトに関する記述が一切無く、利用者から見た契約 (どう置けば期待どおり表示されるか) が文書化されていない。

**推奨修正**: Major-1 の方針確定とセットで、`kasane/decisions/maui/0014-*.md` として measure 契約を起票し、`maui-facade.md` に配置制約を追記する。蒸留フェーズ (ksn-distill) で回収する運用でも構わないが、その場合は本 change に「ADR 未起票・distill で回収」を残しておくこと。

### [🔵 Suggestion] MAUI 本体との微差 2 点

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/Android/SettingsViewHandler.cs:88`, `:50-54`

**問題点**:

- `Math.Max(0d, constraint)` の 0 下限は MAUI 本体 `CreateMeasureSpec` には無い (本体は負値をそのまま pixel 変換する)。実害は無く、むしろ安全側だが、コメントが「MAUI が制約を組み立てるときの規則に合わせる」と述べているので、厳密には合わせていない箇所がある。また `constraint` が `NaN` の場合 `Math.Max(0d, NaN)` は `NaN` を返す (`.NET` の仕様) ため、0 下限のガードは NaN を止めない。
- MAUI 本体は `platformView == null` のとき `Size.Zero` を返すが、本実装は `VirtualView` だけを見るため PlatformView 未生成でも制約ぶんの大きさを返す。実害は確認できていないが、等価性を主張するなら差分として認識しておく方がよい。

**推奨修正**: 必須ではない。Minor-1 の移設とテスト追加の際に、境界値 (負値・NaN・無限) をどう扱う仕様かをテストで明示すれば十分。

---

## 確認した観点 (指摘なしの範囲)

- **ビルド**: `dotnet build KsSettingsView.Maui.csproj -f net10.0-android -c Debug` → 成功、警告 0 / エラー 0。
- **テスト**: `dotnet test KsSettingsView.Maui.Tests` → **241 件 / 失敗 0**。ただし Android TFM は対象外 (Minor-1 参照)。
- **comment-policy lint**: `python3 scripts/comment-policy-lint.py --summary` → 0 ファイル / 禁止 0 件 (検査対象 569 ファイル)。新規コメントは自己完結しており、禁止参照 (change-id / Phase / spec パス / MUST 等) の混入も無い。日本語で書かれており規約準拠。
- **MAUI 本体との等価性 (観点 b)**: `ResolveLength` の分岐は MAUI 10.0.70 相当の `Microsoft.Maui.Platform.ContextExtensions.CreateMeasureSpec(this Context, double constraint, double explicitSize, double minimumSize, double maximumSize)` が組み立てる `constraint` 値と**一致**する — (1) `IsExplicitSet` → `Math.Max(explicitSize, ResolveMinimum(minimumSize))` を `IsMaximumSet` なら `Math.Min(…, maximumSize)`、(2) `IsMaximumSet && maximumSize < constraint` → `maximumSize`、(3) `double.IsInfinity(constraint)` → `0`、(4) それ以外 → `constraint` (AtMost)。`Dimension` の意味論 (`Unset = NaN` / `Maximum = +∞` / `ResolveMinimum` の 0 既定) の使い方も正しい。**違いは「その制約で platform を measure した結果を返す」か「制約そのものを返す」かの 1 点**であり、コメントが「制約の組み立て規則に合わせる」と限定して書いているのは正確。ただしその 1 点の帰結が Major-1。
- **GetDesiredSize / WidthRequest・Min/Max の意味論 (観点 a)**: `WidthRequest` / `HeightRequest` 指定時は MAUI 本体が `MeasureSpecMode.Exactly` を作り、Exactly では Android の measure 結果 = spec 値になるため、本実装が platform へ降りずに同じ値を返すのは等価。`MaximumWidthRequest` が制約より小さい場合も等価。`MinimumWidthRequest` 単独指定 (明示指定なし) は本体も spec に反映しないため差は無い。`VerticalOptions` が `Fill` (View の既定) である限り、有限制約時の表示結果も変わらない。
- **iOS への影響 (観点 c)**: iOS handler は無変更で、コンパイル対象も分離されている (`Compile Remove="Platforms/**/*.cs"` + TFM 別 Include) ため、iOS のビルド・挙動に影響は無い。ただし契約の非対称は残る (Major-1 に含めた)。
- **実機証跡の妥当性**: scratchpad の 01〜17 を確認した。ASCII 連続入力 (03)、BackSpace 連続 5 回 (04)、末尾追記 (05)、文字列中間への挿入 (06/07)、かなキーボード起動〜composing (08〜10)、変換 (11)、確定 (12)、**確定直後の継続入力** (13)、他ページ回帰 (14 基本 Cell / 15 スクロール / 16 共通フィールド / 17 visibility) をカバーしており、完了条件 (ASCII + 日本語 IME + BackSpace の連続入力でフォーカス維持、サンプル他ページ回帰なし) を満たしている。所在の問題のみ Major-2 として指摘した。
- **既知・スコープ外の事象との関係**: 03 の表示 (`Tanakabcdea Taro`) に現れているキャレット位置ずれは、共有済みのスコープ外事象と同系。今回の修正が本事象を悪化させる構造 (書き戻し時の selection 復元経路への介入) は差分に含まれておらず、**悪化の要因は認められない**。
- **足場・deviation**: `git status -uall` で確認したところ変更は `SettingsViewHandler.cs` 1 ファイルのみ、untracked は `exploration.md` のみ。exploration.md の書き換え無し、無断の仕様逸脱・虚偽チェックも無し。

---

## アクションプラン

1. **[Major-1]** 無限制約時の挙動を確定する — フォールバック実装 (推奨 1 か 2) を入れるか、0 で確定したうえで `maui-facade.md` へ配置制約を明記し iOS との非対称の扱いを決める。方針が変わる可能性があるため最初に決める。
2. **[Major-2]** 実機検証の証跡を `kasane/changes/fix-maui-entrycell-focus-loss/evidence/` へ移送し、確認項目との対応索引を添える。
3. **[Minor-1]** `ResolveLength` を全 TFM 共通の internal ヘルパへ移設し、境界値 (明示指定 + min/max、max < constraint、無限、負値/NaN) のユニットテストを追加する。1 で決めた無限時の仕様もここで固定する。
4. **[Minor-2]** measure 契約を maui ドメインの ADR として起票する (または本 change に「distill で回収」と明記する)。
5. **[Suggestion]** 3 のテストで境界値仕様を明示すれば個別対応は不要。
6. Major-1 でコードを変更した場合は、**Pixel 6a での連続入力確認をやり直す** (フォールバックを入れると measure 経路が復活しうるため、緑の再取得が必須)。

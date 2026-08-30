# 実機検証の記録: perf-android-customcell-composition-reuse

**対象タスク**: tasks 3.1 (MAUI サンプルの CustomCell デモでの高速フリック検証)
**実施者**: 実装ワーカー (review-001 Major「新経路が production の Choreographer 駆動で裏取りされていない」への対応)
**参照手順**: `kasane/concepts/maui/architecture/view-materialization.md` の 2026-08-13 エミュレータ実証と同手順

本検証は 2 回実施している。以下は時系列の記録である。

| 回 | 日付 | 対象 | 判定 |
|---|---|---|---|
| 初回 | 2026-08-16 | 修正前 (`ReusableContentHost` を `setContent` 直下に置く構成) | **NG** — 数ジェスチャで FATAL |
| 再検証 | 2026-08-16 | 修正後 (宣言ツリーを `Layout` で包み、非活性の間は content を measure しない構成) | 後述 |

---

# 初回検証 (修正前・NG)

**日付**: 2026-08-16

## 判定

**問題あり (NG)**。高速フリック開始から数ジェスチャ以内に、アプリが必ず落ちる。

```
FATAL EXCEPTION: main
java.lang.IllegalArgumentException: measure is called on a deactivated node
  at androidx.compose.ui.node.MeasurePassDelegate.remeasure(...)
  at androidx.compose.ui.layout.RootMeasurePolicy.measure(...)
  ...
  at androidx.compose.ui.platform.AbstractComposeView.onMeasure(ComposeView.android.kt:476)
  at androidx.recyclerview.widget.RecyclerView$LayoutManager.measureChildWithMargins(...)
  at androidx.recyclerview.widget.LinearLayoutManager.fill(...)
```

`AndroidViewHolder.onDeactivate` の安全性を確かめるまでもなく、その手前で **非活性化された Compose ノードが
`RecyclerView` の measure に当たって即クラッシュする**。tasks 3.1 が求める「空行・例外・view 取り合いが
発生しないこと」は満たされていない (例外が発生する)。したがって tasks 3.1 は未達のままとする。

## 検証対象のビルド

| 項目 | 値 |
|---|---|
| 実装ソース | 本 change の実装後の状態 (`CustomCellViewHolder.kt` SHA-1 `0946afdf…`、`ComposeCellViewHolder.kt` SHA-1 `937f71d7…` — verification-mutation.md の復帰後 SHA と一致) |
| ビルド構成 | `dotnet build samples/maui/… -f net10.0-android -c Debug` (Debug) |
| aar の再生成 | binding csproj の `_BuildKsSettingsViewAars` により gradle `assembleRelease` が走ったことを確認。生成された `ks-settingsview-ui-release.aar` の `CustomCellViewHolder` に `ReusableContent` / `startReusableGroup` と `isContentActive` 等の state フィールドが含まれることを `javap` で確認済み (= 新実装が確かに載っている) |
| MAUI アプリの compose | `androidx.compose.ui` / `androidx.compose.runtime` ともに **1.11.4** (apk 内 `META-INF/*.version`)、`androidx.recyclerview` 1.4.0 |
| ネイティブサンプルの compose | **1.7.5** (compose-bom 2024.10.01。2026-08-13 実証時と同じ版) |

## 環境

| 端末 | OS | 解像度 / 密度 | 用途 |
|---|---|---|---|
| エミュレータ `ksn_custcell_api35` (pixel_5 / arm64-v8a / google_apis_playstore) | Android 15 (API 35) | 1080x2340 / 440dpi | 主 (2026-08-13 実証と同じ AVD) |
| 実機 Pixel 6a | Android 16 (API 36) | 1080x2400 / 420dpi | 実機での裏取り |

対象アプリ: `jp.kamusoft.kssettingsview.samples.maui` (MAUI サンプル) の「CustomCell デモ」画面。
切り分け用に `jp.kamusoft.kssettingsview.samples.android` (Android ネイティブサンプル、Bridge 非経由) も使用。

## 手順

1. logcat を全期間記録する。

   ```sh
   adb -s <serial> logcat -c
   adb -s <serial> logcat -v threadtime > logcat.txt &
   ```

2. アプリを配備・起動し、メニューから「CustomCell デモ」へ遷移する。

   ```sh
   dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj \
     -f net10.0-android -c Debug -t:Run -p:AdbTarget="-s <serial>"
   adb -s <serial> shell input tap 300 795     # メニューの「CustomCell デモ」
   ```

3. 画面右寄り (スライダーを掴まない x) で縦の高速フリックを連続送出する。1 ジェスチャごとに
   `pidof` でプロセス生存を確認し、落ちたフリック番号を記録する。

   ```sh
   adb -s <serial> shell input swipe 972 1895 972 491 55    # 高速フリック (下方向へスクロール)
   adb -s <serial> shell pidof jp.kamusoft.kssettingsview.samples.maui
   ```

4. 6 フリックごとに逆方向のフリックを挟んで往復させる (2026-08-13 と同じく全域往復)。
   iOS 再現手順と同型の「高速フリック → 減速の遅いドラッグ」の組、および遅いドラッグ単独も別途実施した。

各試行は `am force-stop` → `am start` からやり直し、毎回まっさらな状態で開始している。

## 結果

| # | 端末 / アプリ | 試行内容 | 試行数 | 結果 |
|---|---|---|---|---|
| A | エミュ / MAUI | 6 連続高速フリックのバースト | 2 | 2/2 で最初のバースト中に FATAL |
| B | エミュ / MAUI | 1 フリック粒度で生存確認 | 5 | 5/5 で FATAL (3, 2, 2, 1, 1 フリック目) |
| C | Pixel 6a / MAUI | 同上 | 5 | 5/5 で FATAL (5, 3, 2, 2, 2 フリック目) |
| D | エミュ / MAUI | フリック間隔 0 秒 / 0.5 秒 | 各 2 | 4/4 で FATAL (2, 2 / 1, 2 フリック目) |
| E | エミュ / MAUI | **遅いドラッグのみ** (1500ms / 1195px、フリックなし) | 1 | FATAL (2 ドラッグ目) |
| F | エミュ / **ネイティブサンプル** | 高速フリック | 1 | FATAL (4 フリック目) |
| G | エミュ / MAUI | 画面録画つきの高速フリック | 3 | 1/3 で FATAL (3 フリック目)。残り 2 は 1 フリック・5 フリックで打ち切ったため未発生 |
| H | エミュ・Pixel 6a / MAUI | 証跡取得用の通し | 各 1 | 2/2 で FATAL (いずれも 1〜2 フリック目) |

**有効セッション 23 件中 21 件で FATAL**。クラッシュまでのジェスチャ数の中央値は 2。
未発生の 2 件は、いずれもジェスチャ数を 1 回・5 回で打ち切った短いセッション。

(注: 上記とは別に「120 フリック × 3 セッション」を流したが、途中で通知シェードが前面に出て
スワイプがアプリへ届いていなかったため無効として除外した。)

## logcat の例外

- 上記の全 FATAL は **同一の例外** `java.lang.IllegalArgumentException: measure is called on a deactivated node`。
- スタックの末端は 2 系統:
  - `LinearLayoutManager.fill` → `measureChildWithMargins` → `AbstractComposeView.onMeasure` (レイアウト経路。多数派)
  - `RecyclerView.draw` → `AndroidComposeView.dispatchDraw` → `measureAndLayout` (描画経路)
  いずれも `RootMeasurePolicy.measure` から非活性ノードの `remeasure` に入って落ちている。
- 代表 3 件の完全なスタックは [`logs/crash-traces.txt`](logs/crash-traces.txt) に保存した
  (MAUI/エミュ、MAUI/実機、ネイティブサンプル/エミュ)。
- FATAL 以外に、CustomCell 由来の警告・例外は観測していない。

## 追加の切り分け

- **Bridge 固有ではない**: MAUI を介さない Android ネイティブサンプル (同じ `ks-settingsview-ui` を composite build で
  ソース参照) でも同じ例外で落ちる。したがって `AndroidViewHolder.onDeactivate` (platform view の埋め込み) 固有の
  問題ではなく、`CustomCellViewHolder` の非活性化そのものが原因側にある。
- **compose の版に固有でもない**: MAUI 側 1.11.4 / ネイティブ側 1.7.5 の両方で再現する。
- **高速フリック固有ではない**: 遅いドラッグだけでも 2 回目で落ちる。行がプールへ入って再 bind される経路を
  通れば速度に関係なく起きる、という性格の不具合。
- **旧実装との A/B は未実施**。本タスクはコード変更禁止のため、旧構成のビルドを作っていない。ただし例外の文言
  (deactivated node) は `ReusableContentHost(active = false)` による非活性化がなければ発生し得ないもので、
  旧実装 (`setContent {}` による content 差し替え、非活性化なし) には該当経路が存在しない。

### 発生条件の見立て (未確定・修正側で確定させること)

`bind()` は `MutableState` への書き込みだけで、非活性 → 活性の切り替えが実際に反映されるのは次の再 composition。
一方 `RecyclerView` は bind と同じレイアウトパスの中で `ComposeView` を measure する。プール滞在中に非活性化が
再 composition へ反映済みの ViewHolder を再 bind すると、その直後の measure が非活性ノードに当たる — という筋が
スタックとは整合する。**ただし本検証はこの因果を確かめていない** (観測したのは例外の再現性のみ)。

## 未達の項目

数ジェスチャでプロセスが落ちるため、2026-08-13 と同規模のセッション (約 600 フリック / 約 100 検査点) を
完走できていない。結果として、次の 2 点は **判定不能** (問題なしとは言えない):

- 空行・描画欠けの有無
- view の取り合い (他行の platform view が消える / 別行に表示される) の有無

判定用のドライバ (可視行ごとの描画領域の一様色判定による空行検出、ダミー行の title 番号と subtitle 番号の
突き合わせによる混線検出、重複・順序の検査) は用意済みだが、検査点に到達する前にアプリが落ちるため
有効なサンプルが得られなかった。クラッシュの解消後に再実施が必要。

## 証跡

| ファイル | 内容 |
|---|---|
| [`screenshots/emu-api35-maui-01-demo-top.png`](screenshots/emu-api35-maui-01-demo-top.png) | エミュ: フリック開始前の CustomCell デモ (正常表示) |
| [`screenshots/emu-api35-maui-02-flick-crash.mp4`](screenshots/emu-api35-maui-02-flick-crash.mp4) | エミュ: デモ表示 → 高速フリック → 3 フリック目で画面が消える (クラッシュ) までの録画 |
| [`screenshots/emu-api35-maui-03-after-crash-flick02.png`](screenshots/emu-api35-maui-03-after-crash-flick02.png) | エミュ: クラッシュ直後 (アプリが消え、ランチャーが見えている) |
| [`screenshots/pixel6a-api36-maui-01-demo-top.png`](screenshots/pixel6a-api36-maui-01-demo-top.png) | 実機 Pixel 6a: フリック開始前の CustomCell デモ (正常表示) |
| [`logs/crash-traces.txt`](logs/crash-traces.txt) | 代表 3 件の完全なスタックトレース |

実機のクラッシュ後スクリーンショットは、端末のホーム画面 (個人のアプリ一覧) が写るため保存していない。

---

# 再検証 (修正後)

**日付**: 2026-08-16
**対象**: 初回検証の FATAL に対する修正 (宣言ツリーを `Layout` で包み、非活性の間は content を measure せず
`heightDp` 由来の行高さだけ確保する measure policy を追加) が入ったビルド

## 判定

**問題なし**。エミュレータ・実機の両方で、2026-08-13 実証と同規模のセッションを完走した。
空行 (持続) 0 件 / 内容混線 0 件 / 例外 0 件。

## 検証対象のビルド

| 項目 | 値 |
|---|---|
| 実装ソース | 修正後 (`CustomCellViewHolder.kt` SHA-1 `4ed1767afd785feb3203c315e28f6a58c12650a7`) |
| 修正が載っていることの確認 | 再ビルドした `ks-settingsview-ui-release.aar` の `CustomCellViewHolder` に、修正で新設された `isContentComposed` フィールドが存在することを `javap` で確認 (初回検証のビルドには存在しない) |
| ビルド構成 | `dotnet build … -f net10.0-android -c Debug` (初回検証と同じ) |

## セッションの規模

| セッション | 内容 | エミュ (API 35) | 実機 (Pixel 6a / API 36) |
|---|---|---|---|
| A | 高速フリック 6 連続 → 静止検査 の往復 | 25 サイクル | 12 サイクル |
| B | 高速フリック → 減速の遅いドラッグ (iOS 再現手順と同型) | 20 サイクル | 10 サイクル |
| C | fling に次のフリックを重ねる連続フリック | 20 サイクル | 8 サイクル |
| **合計** | | **583 ジェスチャ / 130 検査点** | **267 ジェスチャ / 60 検査点** |

検査点の内訳: 画素ベースの静止検査が全数、うちエミュ 36 回・実機 17 回は a11y ダンプ込み
(混線・重複・順序・潰れ行の判定を含む)。両端末の合計で **850 ジェスチャ / 190 検査点**。

## 判定ドライバ

| 判定 | 方法 |
|---|---|
| 空行 | 可視行の描画領域 (または viewport 内の横帯) が一様色かを標準偏差で判定。正常表示時の最大一様帯を毎回較正し、しきい値 = 較正値 + 40px (実測 130〜134px) |
| 空行の持続性 | 検出時に 0.4 秒後・0.8 秒後へ再測。**3 回とも一様なら「持続」= NG、解消したら「単発フレーム」= 注記**（修正が織り込む「プール由来の再 bind で最大 1 フレーム遅れ得る」性質を NG と誤認しないため） |
| 内容混線 | ダミー行の title 番号 (`ダミー行 #NN`) と subtitle 番号 (`content: DummyItem(N)`) の一致を全可視行で照合 |
| 重複・順序 | 同一 title の同時表示の有無、可視ダミー行の番号が y 座標順に単調か |
| 潰れ行 | viewport に完全に収まる行の高さが 40px 未満か |
| 例外 | セッション全期間の `logcat -v threadtime` を保存し、FATAL / `deactivated node` / アプリ pid 由来の E・F ログを集計 |

viewport の上端・下端に掛かる行は、uiautomator の bounds がクリップされて「高さが低い」「子テキストが
足りない」ように見えるため、潰れ行・subtitle 欠落の判定からは除外している (この除外を入れる前は
偽陽性が 2 件出た)。

## 結果

| 観測項目 | エミュ | 実機 | 備考 |
|---|---|---|---|
| 空行 (持続) | 0 | 0 | |
| 空行 (単発フレーム) | 0 | 1 | 実機 C7 の動作中連写で 175px の一様帯が 1 フレームのみ出現し、続く 4 フレームでは 71px (正常時の水準) に戻った。修正が織り込む 1 フレーム遅れと整合する挙動で、持続はしない |
| 内容混線 (title と subtitle の不一致) | 0 | 0 | a11y 検査点 計 53 回 |
| ダミー行の重複表示・順序異常 | 0 | 0 | |
| 潰れ行 | 0 | 0 | |
| FATAL EXCEPTION | 0 | 0 | |
| `measure is called on a deactivated node` | 0 | 0 | 初回検証で 21 セッション中 21 件発生していたもの |
| アプリ pid 由来の E / F ログ | 0 | 0 | Play ストアのメタデータ取得失敗 (Finsky) を除く |
| プロセスの生存 | 開始 pid のまま完走 | 同左 | |

セッション後の画面も初期表示と一致している (証跡のスクリーンショット)。

## ANR について (ドライバ起因・アプリ側の問題ではない)

最初に組んだドライバは**検査点ごとに `uiautomator dump` を撃つ**構成で、約 290 ジェスチャ /
約 95 ダンプの時点で ANR (`Input dispatching timed out`) が 1 回発生した。切り分けの結果:

- **純ジェスチャ 300 回 (ダンプ・スクショなし) では ANR も例外も発生しない**
- ANR 時のアプリのメインスレッドは Mono の管理コードを実行中 (wchan 0 = 実行中、ブロックではない)。
  CPU は全体 35%・アプリ 21%、メモリ・IO 圧はほぼゼロで、リソース枯渇でもない
- `uiautomator dump` は Compose → `AndroidView` → MAUI View の a11y ツリー生成をアプリのメインスレッドで
  走らせる。Debug ビルドの MAUI ではこれが重く、高頻度に撃つとメインスレッドを占有する

以上から、a11y ダンプを 3 回に 1 回へ間引いた本番セッションで再実行し、**両端末とも ANR 0 件**で
完走した。ANR のログと切り分けの記録は [`logs/anr-during-verification.txt`](logs/anr-during-verification.txt)。

## 観測した気になる挙動 (NG ではない)

エミュレータのセッション後、「プライバシーポリシー（展開中）」行が折りたたまれていた。追試すると:

- 高速フリック往復 20 回、遅いドラッグ 3 往復では**再現しない** (展開状態は維持)
- Session C と同じ連続フリック (40ms) を 100 ジェスチャ流すと、今度は「利用規約（タップで展開）」行が
  **展開** に変わった

すなわち `adb shell input swipe` の合成ジェスチャが、ときどき content 側のタップとして成立している。
行の同一性は保たれたまま (その行自身の開閉状態だけが変わる) であり、**別の行の内容が現れる・消える
といった混線ではない**。実機の指の操作では移動量がタッチスロープを超えるため、合成ジェスチャ固有の
性質と考えられる。本変更の可否には影響しないが、以後この手順で検証する際の注意点として記録する。

## 証跡

| ファイル | 内容 |
|---|---|
| [`screenshots/emu-api35-maui-fix-01-after-session.png`](screenshots/emu-api35-maui-fix-01-after-session.png) | エミュ: 583 ジェスチャ完走後の CustomCell デモ |
| [`screenshots/pixel6a-api36-maui-fix-01-after-session.png`](screenshots/pixel6a-api36-maui-fix-01-after-session.png) | 実機: 267 ジェスチャ完走後の CustomCell デモ (初期表示と一致) |
| [`logs/session-emu-api35-fix.log`](logs/session-emu-api35-fix.log) | エミュのセッション全ログ (サイクルごとの累計と最終集計) |
| [`logs/session-pixel6a-api36-fix.log`](logs/session-pixel6a-api36-fix.log) | 実機のセッション全ログ |
| [`logs/anr-during-verification.txt`](logs/anr-during-verification.txt) | ドライバ初期版で発生した ANR のログと切り分け記録 |

## 追記 (2026-08-16, 蒸留時): 再検証ビルドと提出コードの差

再検証を実施したビルドの `CustomCellViewHolder.kt` は SHA-1 `4ed1767afd785feb3203c315e28f6a58c12650a7`。提出コードはその後 `404ca3bf…` (review-002 Minor-1 対応: 非活性時の確保高さの `isFixedHeight` 分岐 + 回帰テスト 2 件) を経て `2b96cee1928dd59b4198ba2d5a80057c93f1ad03` (review-003 Minor 対応: コメントのみ修正) へ変わっているが、差は非活性時の確保高さの分岐とコメントに閉じており、実機検証が対象とした deactivate / 再活性化の経路は不変 (確保高さの検出力は review-003 が提出コードへの独立ミューテーション (h)(i) で確認済み — verification-mutation.md)。蒸留時の ADR 参照更新 (コメントのみ) で最終的に `736c6713…` へ変わっている。

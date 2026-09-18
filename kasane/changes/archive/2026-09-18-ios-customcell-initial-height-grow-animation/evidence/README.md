# probe (実装前) の証跡と手順

実装に入る前の probe 2 本の記録。目的は maui/ADR-0028 の未確認前提 —「SwiftUI が CustomCell の内容の初回計測で有限幅を提案する」— を確かめることと、wrapper が初回にどの幅で測っているかを実機で押さえることである。

実測日: 2026-09-18 / 実測者: ksn-implementer

## 結論 (先に)

- **1a: 有限幅が来る。** 行の初回表示の最初の問い合わせで、representable は幅 375.0 の提案を受け取っている。ADR-0028 の前提は成立し、実装へ進める
- **1b: 実機・Simulator とも wrapper の初回の答えは無限幅で出ている。** 折り返す内容を持つ行・領域は、初回に 1 行ぶんの高さを答え、配置の後に折り返し後の高さへ測り直されている。この 2 段階は **Simulator でも実機と同じように起きている** — 実機でだけ見えるのは補正の見え方 (アニメーション) であって、計測そのものの差ではない

## 1a: SwiftUI が初回計測で有限幅を提案するか (Simulator、自動テスト)

### 環境

| 項目 | 値 |
|---|---|
| 実行面 | iOS Simulator (iPhone 17 / iOS 26.5) |
| 対象 | `ios/` の Swift Package (`KsSettingsViewUITests`) |

### 手順

使い捨てのテストを `ios/Tests/KsSettingsViewUITests/` へ一時的に置き、実描画で測った。実測後に削除し、再現用の全文を同ディレクトリの [CustomCellInitialWidthProposalProbeTests.swift](CustomCellInitialWidthProposalProbeTests.swift) に残している。製品コードは触っていない。

```
cd ios
xcodebuild test -scheme KsSettingsView \
  -destination 'platform=iOS Simulator,id=<uuid>' \
  -only-testing:KsSettingsViewUITests/CustomCellInitialWidthProposalProbeTests
```

実行結果: `Executed 2 tests, with 0 failures`

埋め込んだのは実 wrapper (C#) ではなく、同じ計測契約を持つ代役の `UIView` である (幅 0 の間は無限幅相当で 1 行ぶんの高さ 60、幅が付いたら折り返し後の高さ 200 を返す)。前例の probe (`kasane/roadmaps/maui-support/phases/phase-5-custom-cell/artifacts/probe/2026-08-12-cell-content-size-follow.md`) と同じ制約に立つ。

経路は実物と同じで、`CustomCell` → `CustomCellView` → `UIHostingConfiguration` → `CustomCellRowPlacement` → representable と通る。

### 観測

生の記録は [probe-1a-first-width-proposal.log](probe-1a-first-width-proposal.log)。

現行の測り方 (包んだ view の `intrinsicContentSize` を中継する。`KsBridgeCellContentView` と同じ形):

| 順序 | 起きたこと |
|---|---|
| 0 | representable の `sizeThatFits` に **提案幅 375.0** が届く。`uiView.bounds.width` はこの時点で **0.0** |
| 1 | 代役の `intrinsicContentSize` が問われる。`bounds.width` が 0 なので**無限幅で測り、1 行ぶんの高さ 60 を返す** |
| 2 | representable は提案幅 375.0 と高さ 60 を返す |
| 3〜5 | 同じ問い合わせがもう 1 往復 (答えも同じ 60) |
| 6 | 配置が走り、`bounds.width` が 0 → 375 に変わって必要サイズが無効化される |
| 7〜12 | 測り直し。`bounds.width` が 375 になったので折り返し後の高さ 200 が返る |

幅付きで問い直す測り方 (ADR-0028 が CustomCell について採る形。`sizeThatFits(提案幅, ∞)` を包んだ view に投げる) では、**順序 0 の時点で高さ 200 が返る**。以降の測り直しでも答えは 200 で変わらない。

### 何が言えるか

- **提案幅は初回から有限である** (375.0)。`nil` でも無限でもない。ADR-0028 の「SwiftUI が CustomCell の内容の初回計測で有限幅を提案する」という前提は成立する
- 初回の問い合わせ時点で、埋め込んだ view の `bounds.width` は **0** である。つまり intrinsic 経路は「幅を知らないまま答えている」一方、**同じ瞬間に提案として幅は届いている**
- 順序は「幅付きの問い合わせ → (その中で) intrinsic の問い合わせ → 配置 → 幅の変化を検知 → 測り直し」であり、幅付きの問い合わせのほうが intrinsic より先にある
- したがって、representable が提案幅を使って包んだ view に問い直せば、初回から折り返し後の高さを返せる

### 前提と射程

- 代役は MAUI 本体の `MauiView` が持つ制約ペア単位の計測キャッシュを持たない。実 wrapper で同じ結果になるかはキャッシュの扱い次第であり、ADR-0028 の「`MeasureInvalidated` でキャッシュも捨てる」はこの probe では検証していない
- 行は 1 行のみ・スクロールなし・再利用なしの条件で測った

## 1b: wrapper が初回にどの幅で測っているか (実機とシミュレータ、ログ)

### 環境

| 項目 | 値 |
|---|---|
| 対象アプリ | MAUI Sample `samples/maui/KsSettingsView.Sample.Maui` (Debug) |
| 実機 | iPhone 11 / iOS 18.7.8 (論理幅 414pt) |
| シミュレータ | iPhone 17 / iOS 26.5 (論理幅 402pt) |
| 対象コード | `maui/KsSettingsView.Maui/Platforms/iOS/KsAccessoryHostView.cs` |

### 手順

1. `KsAccessoryHostView` の `IntrinsicContentSize` と `LayoutSubviews` に一時的な `Console.WriteLine` を入れた (問い合わせ時刻・`Bounds.Width`・使った幅制約・返した高さ・包んだ View の型・superview とその幅)。**probe 終了時に外し、`shasum` が元と一致することを確認済み**
2. Sample と facade の `bin` / `obj` を捨ててフルビルドした
3. 実機へ配備し、アプリの stdout を取りながら、初回訪問で次の 2 画面を開いた
   - 「CustomCell デモ」の「動的高さ」Section (プライバシーポリシー = 展開中。本文の `Label` が折り返す)
   - 「Header / Footer への View 配置デモ」の ③ (折り返す `Label` の Section FooterView)
4. 同じ操作を iOS Simulator でも行った

実機ビルド・配備のコマンドの形 (署名の指定は手元の証明書とプロファイルに依存する):

```
dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj \
  -f net10.0-ios -c Debug -r ios-arm64 \
  -p:CodesignKey=<signing-identity> -p:CodesignProvision=<uuid>
xcrun devicectl device install app --device <ios-udid> \
  samples/maui/KsSettingsView.Sample.Maui/bin/Debug/net10.0-ios/ios-arm64/KsSettingsView.Sample.Maui.app
xcrun devicectl device process launch --device <ios-udid> --console <application id>
```

Simulator は `xcrun simctl install` / `xcrun simctl launch --console-pty` で同じログを取った。

ログは抜粋のみを置く (全文は手元保管)。対象の wrapper の行だけを切り出し、sanitize を通してある。

### 観測 — CustomCell の行 (動的高さ Section)

実機: [probe-1b-device-customcell.log](probe-1b-device-customcell.log) / シミュレータ: [probe-1b-simulator.log](probe-1b-simulator.log) 前半

| 実行面 | 初回の答え | 使った幅 | 測り直し後 | 差 |
|---|---|---|---|---|
| 実機 (414pt 幅) | 92.5pt | `+∞` (`Bounds.Width` = 0) | 167pt (幅 414 で測り直し) | **+74.5pt** |
| シミュレータ (402pt 幅) | 92.33pt | `+∞` (`Bounds.Width` = 0) | 166.67pt (幅 402 で測り直し) | **+74.33pt** |

実機の +74.5pt は @2x で 149px にあたり、exploration.md の追記に記録された「行の高さだけが約 300ms かけて 149px 増える」という実測と一致する。**見立て (初回の答えが幅なしで出ている) の裏取りが取れた。**

経過 (実機):

- t=489ms: `Bounds.Width` = 0、superview なし → 無限幅で測って 92.5
- t=552ms: `Bounds.Width` = 0、superview の幅も 0 → 92.5
- t=553ms: `Bounds.Width` = 0 のまま、**superview の幅は 414 になっている** → それでも無限幅で測って 92.5
- t=563ms: `Bounds.Width` = 414 → 167 に測り直し

### 観測 — Section FooterView ③ (view accessory、Auto Layout 経路)

実機: [probe-1b-device-footer.log](probe-1b-device-footer.log) / シミュレータ: [probe-1b-simulator.log](probe-1b-simulator.log) 後半

| 実行面 | 初回の答え | 使った幅 | 測り直し後 | 差 |
|---|---|---|---|---|
| 実機 (414pt 幅) | 38.5pt | `+∞` (`Bounds.Width` = 0) | 67pt | **+28.5pt** |
| シミュレータ (402pt 幅) | 38.33pt | `+∞` (`Bounds.Width` = 0) | 67pt | **+28.67pt** |

同じ画面の他の accessory (折り返さない `Border`) は初回も測り直し後も同じ高さ (41 / 57.5 / 38.5) で、**折り返す内容を持つ ③ だけが変わる**。

Footer では `LayoutSubviews` が実際に「幅の変化 前=0 後=414」を検知して無効化を出しており、CustomCell の行 (検知前に既に幅付きで問い直されている) とは無効化の起点が異なる。

**この経路でも、初回の問い合わせの時点で superview の幅は既に 414 (Simulator では 402) になっている。** `Bounds.Width` だけが 0 である。

### 未決の論点への答え

exploration.md「未決の論点」の「実機ログで確かめる 3 点」について:

① **Simulator で出ない理由**: 計測の側には差が無い。Simulator でも実機と同じ 2 段階 (92.33 → 166.67 / 38.33 → 67) が同じ順序で起きている。実機との差は「補正が最初の描画より前に収まるかどうか」であり、Simulator では見えるフレームになる前に収束している。取得粒度や機種・幅の問題ではない。**Simulator の目視・自動確認は本症状の合否判定に使えない**という exploration の判断はそのまま維持される

② **Footer が 2→3 行だった理由**: wrapper の答えは初回が 38.5pt で、これは折り返さない (1 行ぶんの) 高さである。「初回が実際より広い有限幅だった」という仮説は**否定される** — 初回も無限幅 (`+∞`) で測っている。オーナーの収録で 2 行ぶんに見えたのは、収録の最初のフレームが既に補正の途中だったためと考えるのが素直である (実機の補正は t=391910ms → t=391924ms の 14ms で答えが変わり、その後アニメーションで見た目が追いつく)

③ **同型の行で出ない理由の手がかり**: 今回の 2 画面のログからは、折り返す内容を持つ wrapper だけが初回と測り直しで違う高さを答え、折り返さないものは答えが変わらないことが確認できた。利用側アプリの解析設定ページで出ない理由はこの probe の射程外だが、**「同型に見えても折り返しが実際に起きているか (= 初回の無限幅の答えと実幅の答えに差が出るか) で分かれる」**という説明が第一候補になる

### 前提と残るリスク

- ログは初回訪問 1 回ずつ。再訪問・回転・スクロールは測っていない
- 実機とシミュレータで画面幅が違う (414pt / 402pt) ため、高さの実数は直接比較できない。比較しているのは「2 段階が起きるかどうか」と「差の大きさの桁」である
- 目視の証跡 (伸びる動きそのもの) はこの probe では撮っていない。伸びる動きの記録は `kasane/changes/archive/2026-09-18-android-accessory-view-late-insert-animation/evidence/README.md` の 1.3 / 6.3-e が持つ

---

# 修正後の検証 (実装後)

実測日: 2026-09-18 / 実測者: ksn-implementer

## 結論 (先に)

- **実機で症状の芯が消えた。** CustomCell の行も Section FooterView も、**初回の答えが測り直し後の答えと同じ**になった。以前の 2 段階 (92.5 → 167 / 38.5 → 67) が 1 段階 (最初から 167 / 67) になっている
- **内容の変化への追従は保たれている。** 同じ幅のまま高さだけが変わる操作 (展開・折りたたみ) で、wrapper が伸縮の両方向に新しい高さを答えている。計測の控えが捨てられていることの直接の裏取りになる
- **Simulator の遷移に退行は無い。** 2 画面とも、内容は最初のフレームから在り、落ち着くまでの間の**縦のずれはどのフレームでも 0px** である

## 実機 (iPhone 11) の A/B

環境と手順は上の 1b と同じ (同じ端末・同じ 2 画面・同じ一時ログ・初回訪問)。ログは probe 終了後に外し、ログ無しのクリーンなビルドを実機へ入れ直してある。

### CustomCell の行 (動的高さ Section)

修正前: [probe-1b-device-customcell.log](probe-1b-device-customcell.log) / 修正後: [probe-1b-after-device-customcell.log](probe-1b-after-device-customcell.log)

| | 修正前 | 修正後 |
|---|---|---|
| 取り付け前の問い合わせ (`super=none`) | 92.5pt (無限幅) | 92.5pt (無限幅) |
| 領域に取り付いた後・自分の幅が決まる前 | **92.5pt** (無限幅で測っている) | **167pt** (親の幅 414 で測っている) |
| 自分の幅が決まった後 | 167pt | 167pt |
| `LayoutSubviews` の「幅の変化 → 必要サイズを無効化」 | **出る** | **出ない** |
| 行の高さに使われる答えの変化 | 92.5 → 167 (+74.5pt = @2x で 149px) | 変化なし |

`super=none` の 1 件だけは修正後も無限幅のままである。これは wrapper がまだどこにも取り付いていない時点の問い合わせで、幅の手がかりがどこにも無い。実機のログでは、この後に必ず親の幅が付いた問い合わせが来て 167pt を答えており、**行の高さを決めるのはそちら**である (修正前はこの 2 回目も 92.5pt だった)。

### Section FooterView ③ (view accessory、Auto Layout 経路)

修正前: [probe-1b-device-footer.log](probe-1b-device-footer.log) / 修正後: [probe-1b-after-device-footer.log](probe-1b-after-device-footer.log)

| | 修正前 | 修正後 |
|---|---|---|
| 初回の答え (`Bounds.Width` = 0) | **38.5pt** (無限幅) | **67pt** (親の幅 414) |
| 測り直し後 | 67pt | 67pt |
| 領域の高さの変化 | 38.5 → 67 (+28.5pt) | 変化なし |

`LayoutSubviews` の「幅の変化 前=0 後=414 → 必要サイズを無効化」は修正後も出る (自分の幅が実際に 0 から 414 へ変わるため)。ただし**測り直した答えが初回と同じ 67pt** なので、領域の高さは変わらず、UIKit の高さ遷移も起きない。同じ画面の折り返さない accessory (41 / 57.5 / 38.5) も初回と測り直し後が一致している。

### 内容の変化への追従 (同じ画面の「利用規約」の展開・折りたたみ)

[probe-1b-after-device-customcell.log](probe-1b-after-device-customcell.log) の後半。同じ wrapper・**同じ幅 414** のまま、

- 初回表示: 39.5pt (折りたたみ)
- 展開の操作後: **167pt**
- 折りたたみの操作後: **39.5pt**

同じ幅に対して違う高さが返っているので、幅・高さの制約の組ごとの計測の控えが実際に捨てられている。表示上も、展開で本文が現れて後続の Section が下へ送られ、折りたたみで元に戻ることを画面の読み出しで確認した。

## Simulator の遷移 (退行の確認)

| 項目 | 内容 |
|---|---|
| 端末 | iOS Simulator「iPhone 17」(iOS 26.5) |
| アプリ | `samples/maui/KsSettingsView.Sample.Maui` (Debug、ログ無しの修正後ビルド) |
| 操作 | アプリを起動し直してルートメニューを出し、録画開始の約 2 秒後にメニュー項目をタップする (初回訪問) |
| 切り出し | デモ画面の内容が最初に描画されたフレームを t=0 とし、`AVAssetImageGenerator` (許容誤差 0) で切り出す。動画本体は証跡に含めず手元保管 |

置いた静止画は t0000ms (最初に内容が描かれたフレーム)・t0500ms (スライド完了後)・t1400ms (落ち着いた後) の 3 枚ずつ。t0100ms 〜 t0400ms を含む全フレームは下の突き合わせで数値として見ている。

### 突き合わせ

各フレームについて、落ち着いた状態のフレームと最も一致する平行移動量を、水平は全幅・垂直は ±8px の範囲で探した (ナビゲーションバーより下の領域、内容のある行だけを対象にする)。

| フレーム | ③ Header / Footer デモ | CustomCell デモ |
|---|---|---|
| t0000ms | 水平 1095px ずれ / **垂直 0px** | 水平 853px ずれ / **垂直 0px** |
| t0100ms | 水平 281px / **垂直 0px** | 水平 257px / **垂直 0px** |
| t0200ms | 水平 53px / **垂直 0px** | 水平 48px / **垂直 0px** |
| t0300ms | 水平 6px / **垂直 0px** | 水平 6px / **垂直 0px** |
| t0400ms | 水平 1px / **垂直 0px** | 水平 1px / **垂直 0px** |
| t0500ms | 水平 0px / **垂直 0px** | 水平 0px / **垂直 0px** |
| t1400ms | 水平 0px / **垂直 0px** | 水平 0px / **垂直 0px** |

**どのフレームでも垂直のずれは 0px** である。遷移の各フレームは、落ち着いた状態の画面を水平にずらしただけのものになっている。

スライドが終わった後 (t0467ms 以降) のフレーム間の画素差は 0.12/255 以下で、残っているのはスクロールインジケータが消える動きだけである (t1233ms ごろに消え切る)。**スライド完了後に段差のある変化は無い** — 症状のような「後から高さが増える」動きが残っていればここに現れる。

### 観測の要点

- `after-ios-sim-accessory-views-t0000ms.png`: デモ画面が右端から入ってくる最初のフレーム。① Root Header View・② Section Header / Footer View・③ 折り返す Label の Footer View・④ Header View・⑤ 差し替え前の Header View が**すべて在り**、③ は折り返した後の高さで確保されている
- `after-ios-sim-customcell-t0000ms.png`: 同期ステータス行・Slider 3 本・利用規約 (折りたたみ) / プライバシーポリシー (展開中、**本文は 4 行**) が**すべて在る**。行は最初のフレームから折り返し後の高さで、直後の説明文もその位置に在る
- `*-t0500ms.png` / `*-t1400ms.png`: 落ち着いた状態。③ の Footer は 3 行、プライバシーポリシーの本文は 4 行で、t0000ms から位置が動いていない

## オーナーへの目視確認のお願い (手順)

実機 (iPhone 11) の目視は合否の正である。次の手順で見る。

1. 端末に入っている `KsSettingsView Sample` を起動し、ルートメニューを出す
2. 「CustomCell デモ」をタップして**遷移直後の 1 秒**を見る
   - 見るところ: 「動的高さ」Section の**プライバシーポリシーの本文 (4 行)** と、その下の説明文「content 内の状態で展開/折りたたみ。…」
   - 直っていれば: 遷移が終わった位置から**下へずれる動きが無い**。本文が行の枠からはみ出して見える瞬間も無い
   - 直っていなければ: 本文の下の説明文と以降が、遷移後に 0.3 秒ほどかけて下へスライドする
3. 同じ画面で「利用規約（タップで展開）」を開いて閉じる (退行の確認)
   - 開くと本文が現れて以降が下へ、閉じると元に戻る。どちらも操作に追従して動く
4. ルートメニューへ戻り、「Header / Footer への View 配置デモ」をタップして**遷移直後の 0.2 秒**を見る
   - 見るところ: **③ Footer View (折り返す Label)** の領域と、その下の「④ Header View（Header Text より優先）」の上端
   - 直っていれば: ③ は最初から 3 行ぶんの高さで、④ 以降が下へずれない
   - 直っていなければ: ③ が一瞬だけ低く、直後に 1 行ぶん伸びて ④ 以降が下へずれる
5. どちらの画面も、一度戻ってもう一度開く (再訪問) でも同じことを見る

## オーナーの目視結果 (2026-09-18)

iPhone 11 実機で上の手順 1〜5 を実施。「CustomCell デモ」の動的高さ Section も「Header / Footer への View 配置デモ」の ③ Footer View も、遷移直後の下へのずれは目視では起きなかった (初回・再訪問とも)。展開・折りたたみの追従に退行なし。記録: ksn-orchestrator (オーナーの口頭報告による)。

## Swift 6 言語モード確認の記録 (handbook/ios/swift6-language-mode-check.md)

実装者の完了報告 (2026-09-18) より: `ios/Package.swift` に一時設定を入れたビルドで error 0 件。確認後に設定を戻し、`ios/Package.swift` の shasum が元と一致することを確認済み。記録: ksn-orchestrator。

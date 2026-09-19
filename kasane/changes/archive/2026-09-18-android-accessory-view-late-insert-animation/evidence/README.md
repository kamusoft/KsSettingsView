# 修正前の再現 (tasks 1.1・1.2・1.3) の証跡と手順

この文書は tasks.md「1. 修正前の再現 (実環境)」で取った証跡と、その撮り方をまとめる。
6.2 (Android の解消確認)・6.3 (iOS の解消確認) は**ここに書いた手順と同じ操作・同じ切り出し位置**で撮り、修正前後を並べて比較する。

対象は修正前の状態 (製品コード無変更) のビルド。
以下の「環境」「手順」と 1.1・1.2・6.2 は Android を扱う。iOS の環境と手順は 1.3・6.3 の節が別に持つ。

## Sample への追加項目 (折り返す Label を持つ FooterView)

`samples/maui` の `AccessoryViewsDemoPage` に、**折り返して複数行になる Label を持つ Section FooterView** を ③ として足した (既存の ③〜⑦ は ④〜⑧ へ繰り下げた)。6.3 が要求する「折り返す Label を持つ FooterView」の高さのブレを、初回表示の画面内で観測できる場所が Sample にしか無いためである。

この追加は Sample だけの変更なので、**修正前のビルドにも同じ追加分だけを当てて**撮っている。修正前後の差は製品コードの差だけになる。

画面上の番号と項目の対応は次のとおり。以下の表はこの番号で位置を示す。

| 番号 | 項目 |
|---|---|
| ① | Root Header View / Root Footer View |
| ② | Section Header View / Section Footer View (1 行の Label) |
| ③ | Section Footer View (折り返して 3 行になる Label) |
| ④ | Header Text と Header View の併存 (View 優先・View を外すと text) |
| ⑤ | Header View の別インスタンスへの差し替え |
| ⑥ | 内容変化への高さ追従 Header View |
| ⑦ | HeaderHeight = 44 の Header View |
| ⑧ | 切断・再接続をまたぐ Header View |

## 環境 (Android)

| 項目 | 内容 |
|---|---|
| 端末 | Android 実機 Pixel 6a (Android 16) |
| アプリ | `samples/maui/KsSettingsView.Sample.Maui` (Debug) |
| 撮影日 | 1.2 (CustomCell) は 2026-09-17、1.1・6.2 は Sample 追加後に 2026-09-18 へ撮り直した |

## 手順 (Android)

1. 端末へ配備する (接続端末が複数あるため対象を明示する)

   ```bash
   dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj \
     -f net10.0-android -c Debug -t:Run -p:AdbTarget="-s <android-serial>"
   ```

2. アプリを起動し直してルートメニューを表示した状態にする (初回訪問の観測にするため)
3. 端末側で画面録画を開始する (`adb shell screenrecord --bit-rate 20M /data/local/tmp/rec.mp4`)
4. 録画開始から約 2 秒後にメニュー項目をタップする
   - 1.1 / 6.2 accessory views: 「Header / Footer への View 配置デモ」
   - 1.2 / 6.2 customcell: 「CustomCell デモ」
5. 4 秒ほど待ってから録画を止め、動画を手元へ取り出す
6. **デモ画面の内容が最初に描画されたフレーム**を t=0 とし、そこから 100ms 刻みでフレームを切り出す
   (`AVAssetImageGenerator` で許容誤差 0 の静止画切り出し。動画本体は証跡に含めず手元保管)

切り出したフレームのファイル名の `tNNNNms` は、上記 t=0 からの経過時間を表す。
遷移はメニューとデモ画面のクロスフェードであるため、t=0 はメニューがまだ薄く重なって見えるフレームになる。重なりは約 100ms で解ける。

再訪問 (`revisit-`) は、デモ画面から戻る操作でメニューへ戻り、同じ項目をもう一度タップして同じ手順で撮る。

## 1.1 Root header・Section の View だけの Header / Footer (AccessoryViewsDemoPage)

| ファイル | 観測内容 |
|---|---|
| `before-android-accessory-views-t0000ms.png` | デモ画面の内容が出た最初のフレーム。view accessory が**どれも無い**。先頭行は最初の LabelCell「Header View のバインド」で、① Root Header View・② Section Header View / Section Footer View・③ 折り返す Label の Footer View・⑤・⑥・⑦・⑧ の各 View が出ていない。④ の Section は View が未到着のため text のフォールバック「④ Header Text（View を外すと現れる）」が出ている |
| `before-android-accessory-views-t0100ms.png` | 同上 (変化なし) |
| `before-android-accessory-views-t0200ms.png` | 同上 (変化なし) |
| `before-android-accessory-views-t0300ms.png` | view accessory の行がまとめて挿入される。③ の折り返す Footer の領域も含めて行の高さは確保されるが、まだ中身が描かれておらず空の帯に見える。④ は text のフォールバックが消える |
| `before-android-accessory-views-t0400ms.png` | 挿入された各 View の中身が描かれ、② Section Header / Footer View・③ 折り返す Label の Footer View (3 行)・④ Header View・⑤ 差し替え前の Header View が現れる。先頭行は依然として「Header View のバインド」で、① Root Header View と ② Section Header View は上へ押し出されて画面外にある |
| `before-android-accessory-views-t0500ms.png` | 落ち着いた状態 (t0400ms と同じ) |
| `before-android-accessory-views-settled-scrolled-to-top.png` | 落ち着いた後に手で先頭までスクロールした状態。① Root Header View と ② Section Header View は**存在する**が、遷移直後の表示位置より上に挿入されたため画面外にあった |

観測の要点:

- 画面内容が出てから view accessory の行が挿入されるまで **約 300ms**、中身が描かれるまで **約 400ms** かかる。その間、View だけの header / footer は行そのものが無い
- 挿入は先頭側 (① Root Header View・② Section Header View) にも起きるため、遷移直後に見えていた位置が相対的にずれる。挿入後の表示は先頭行から始まらず、先頭を見るには手でスクロールし直す必要がある
- ④ のように text と View を併記した Section では、遷移直後に text が見え、View 到着時に View へ切り替わる二段の見え方になる
- ③ の折り返す Label の Footer View も同じく後から挿入される。行の挿入 (t0300ms) と中身の描画 (t0400ms) が別フレームに分かれるため、折り返し後の高さが決まるのは中身が描かれた時点になる

## 1.2 CustomCell の Content (CustomCellDemoPage)

| ファイル | 観測内容 |
|---|---|
| `before-android-customcell-t0000ms.png` | デモ画面の内容が出た最初のフレーム。Section の header / footer の text は出ているが、CustomCell の行は**すべて空**で、Content (同期ステータス行・明るさ / 音量の Slider・利用規約 / プライバシーポリシーの展開行) が無い |
| `before-android-customcell-t0100ms.png` | 同上 (変化なし) |
| `before-android-customcell-t0250ms.png` | Content がまとめて入り、各行の高さが変わる。行の高さが伸びた分だけ後続の行が下へずれる |
| `before-android-customcell-t0300ms.png` | 同上 (反映後) |
| `before-android-customcell-t0400ms.png` | 落ち着いた状態 |

観測の要点:

- 行そのものは最初から存在する (Section の header / footer text の位置で分かる) が、Content が**約 250ms** 遅れて入る
- Content が入った時点で行の高さが変わり、以降の行が下へずれる (1.1 の行挿入とは別の見え方)
- **観測できた**ため、tasks 1.2 の「観測できない場合はその旨を記録する」には当たらない
- この画面は Sample の追加項目 (③) の影響を受けないため、修正前のフレームは Sample 追加前に撮ったものをそのまま使っている

## 1.3 (iOS Simulator) 修正前の観測

1.3 は iOS 実機の画面録画が手元の環境で取れないため、**まず iOS Simulator で修正前を観測**し、6.3 の比較元とする。実機 (iPhone 11) の観測はオーナーの画面収録で後から追加する (deviation.md)。この節は Simulator 分だけを扱う。実機分は下の「1.3 (iOS 実機) 修正前の観測」が持つ。

### 環境

| 項目 | 内容 |
|---|---|
| 端末 | iOS Simulator「iPhone 17」(iOS 26.1) |
| アプリ | `samples/maui/KsSettingsView.Sample.Maui` (Debug、iOS Simulator 向け) |
| ビルド元 | 修正前の commit の内容をリポジトリ外の一時領域へ読み取り専用で展開し、Sample の追加項目 (③) だけを当てたもの (作業ツリーは修正後のため使わない) |
| 撮影日 | AccessoryViews は Sample 追加後に 2026-09-18 へ撮り直した。CustomCell は 2026-09-17 |

修正後 (6.3) の Simulator 観測は**同じデバイス種別・同じ OS 版**で撮る。

### 手順

1. Simulator を起動し、上記の一時領域から `-f net10.0-ios -c Debug` でビルドして `simctl install` / `simctl launch` で配備する (接続先は UDID で明示する)
2. アプリを起動し直してルートメニューを表示した状態にする (初回訪問の観測にするため)
3. `xcrun simctl io <udid> recordVideo --codec h264` で録画を開始する
4. 録画開始から約 2 秒後にメニュー項目をタップする
   - accessory views: 「Header / Footer への View 配置デモ」
   - customcell: 「CustomCell デモ」
5. 4 秒ほど待ってから録画を止め、動画を手元へ取り出す
6. **デモ画面の内容が最初に描画されたフレーム**を t=0 とし、そこから 100ms 刻みでフレームを切り出す
   (`AVAssetImageGenerator` で許容誤差 0 の静止画切り出し。動画本体は証跡に含めず手元保管)

Simulator の録画は画面が変わったときだけフレームを書き出す可変フレームレートである。1.1・1.2 (Android) と同じく t=0 は画面内容で決め、時刻は t=0 からの経過時間で表す。`tNNNNms` の意味は Android の節と同じ。

遷移は iOS の push (右からのスライドイン) であるため、t=0 はデモ画面がまだ右端の一部しか出ていないフレームになる。スライドは約 500ms で終わり、`t1400ms` は落ち着いた状態にあたる。

### 1.3-a Root header・Section の View だけの Header / Footer (AccessoryViewsDemoPage)

| ファイル | 観測内容 |
|---|---|
| `before-ios-sim-accessory-views-t0000ms.png` | デモ画面の内容が出た最初のフレーム (右端に約 1/3 幅だけ見えている)。この時点で ① Root Header View・② Section Header View / Section Footer View・③ 折り返す Label の Footer View・④ Header View・⑤ 差し替え前の Header View が**すべて在る**。④ は text のフォールバック「④ Header Text」ではなく View 表示で出ている。③ の Label は 3 行に折り返した状態で、領域の高さもその 3 行分で確保されている |
| `before-ios-sim-accessory-views-t0100ms.png` | スライド途中。表示内容に変化なし |
| `before-ios-sim-accessory-views-t0200ms.png` | 同上 |
| `before-ios-sim-accessory-views-t0300ms.png` | ほぼスライド完了。内容に変化なし |
| `before-ios-sim-accessory-views-t0400ms.png` | スライド完了。先頭行は ① Root Header View で、手でスクロールし直す必要はない |
| `before-ios-sim-accessory-views-t0500ms.png` | 落ち着いた状態 |
| `before-ios-sim-accessory-views-t1400ms.png` | 落ち着いた後。t0400ms 以降と同じ |
| `before-ios-sim-accessory-views-settled-scrolled-to-bottom.png` | 落ち着いた後に手で末尾までスクロールした状態。⑥ 高さが追従する Header View・⑦ HeaderHeight = 44 の Header View (固定高さで 2 行目以降が切り詰められる)・⑧ 切断・再接続をまたぐ Header View・① Root Footer View が在る |

観測の要点:

- **修正前の時点で既に、view accessory は最初のフレームから在る。**Android の 1.1 で見えた「画面内容が出てから約 300〜400ms 後にまとめて挿入される」現象は Simulator の iOS では起きていない
- 先頭側への後からの挿入も起きないため、遷移直後の表示がリスト先頭 (① Root Header View) から始まる
- ④ の text → View の二段の見え方も起きない
- ③ の折り返す Label の Footer View も最初のフレームから 3 行の高さで在り、落ち着くまでの間に高さが変わらない
- t0000ms 〜 t0500ms の各フレームは、落ち着いた状態の画面を水平にずらしただけのものである (各フレームについて最も一致する平行移動量を水平 ±900px・垂直 ±8px の範囲で探すと、**縦のずれはどのフレームでも 0px**、その位置での画素差の平均は 2.6/255 以下)。**行の追加・高さの変化・縦方向のずれは無い**

### 1.3-b CustomCell の Content (CustomCellDemoPage)

| ファイル | 観測内容 |
|---|---|
| `before-ios-sim-customcell-t0000ms.png` | デモ画面の内容が出た最初のフレーム (右端に約 2/3 幅が見えている)。同期ステータス行・明るさ / 音量 / 無効の Slider・利用規約 (折りたたみ) / プライバシーポリシー (展開中、4 行の本文) が**すべて在る**。空の行は無い |
| `before-ios-sim-customcell-t0100ms.png` | スライド途中。表示内容に変化なし |
| `before-ios-sim-customcell-t0200ms.png` | 同上 |
| `before-ios-sim-customcell-t0300ms.png` | スライド完了 |
| `before-ios-sim-customcell-t0400ms.png` | 落ち着いた状態 |
| `before-ios-sim-customcell-t0500ms.png` | 同上 |
| `before-ios-sim-customcell-t1400ms.png` | 落ち着いた後。t0300ms 以降と同じ |

観測の要点:

- **修正前の時点で既に、CustomCell の Content は最初のフレームから在る。**Android の 1.2 で見えた「行はあるが Content が約 250ms 遅れて入り、行の高さが変わる」現象は Simulator の iOS では起きていない
- 1.3-a と同じ突き合わせ (平行移動量を求めてから画素差の平均を取る) で、t0000ms 〜 t0500ms はいずれも落ち着いた状態の水平ずれに収まる (差の平均 3/255 以下)。**行の高さの変化も縦方向のずれも無い**
- 遷移が終わった後 (t=1.3 秒ごろ) から録画終了までの間に画面の変化は無い (差の平均 5/255 以下で、これはステータスバーの時刻表示を除いた領域の圧縮誤差にあたる)。`kasane/changes/ios-customcell-initial-height-grow-animation` が扱う「Content は最初から在るが行の高さだけが後から変わる」現象は、**この Simulator・この画面の初回表示では観測できなかった**

### 6.3 で見るときの注意

- 修正後の Simulator 観測は、同じデバイス種別・同じ OS 版・同じ手順・同じ t=0 の決め方で撮り、`after-ios-sim-` 接頭辞で対応させる
- 修正前がすでに「最初のフレームから在る」状態であるため、6.3 の判定は「修正前と同じく後着が無いこと」(退行が無いこと) になる。1.3 に無かった後着や高さの変動が出た場合は、tasks 6.3 のとおり完了を止めてオーナーへ報告する

## 1.3 (iOS 実機) 修正前の観測

Simulator の 1.3 に続けて、**iOS 実機 (iPhone 11) の修正前**をオーナーの画面収録から観測した (deviation.md)。この節は実機分だけを扱う。
今回の収録は**初回訪問だけ**で、ページの再訪問は含まない。

### 環境

| 項目 | 内容 |
|---|---|
| 端末 | iOS 実機 iPhone 11 |
| アプリ | `samples/maui/KsSettingsView.Sample.Maui` (Debug) |
| ビルド元 | 修正前 + Sample の追加項目 (③) だけを当てたもの (1.3 の Simulator と同じ内容) |
| 収録 | オーナーが端末の画面収録機能で撮影。**動画はリポジトリに置かず手元保管** |
| 画像 | 端末の画面解像度そのまま (828 × 1792)。縮小はしていない (既存の Simulator 証跡 1206 × 2622 より小さいため) |
| 撮影日 | 2026-09-18 |

### 手順 (オーナーの手動収録)

1. アプリを起動し直してルートメニューを表示した状態にする (初回訪問の観測にするため)
2. 端末の画面収録を開始する
3. 収録開始から約 2 秒後にメニュー項目をタップする
   - accessory views: 「Header / Footer への View 配置デモ」
   - customcell: 「CustomCell の MAUI 固有デモ」
4. 約 4 秒待ってから収録を止め、動画を手元へ渡す
5. **デモ画面の内容が最初に描画されたフレーム**を t=0 とし、そこから 100ms 刻みでフレームを切り出す

切り出しは、復号したフレームをフレーム番号で取り出す小道具で行った (時刻の丸めを挟まない。動画本体は証跡に含めず手元保管)。
実フレームの表示時刻を全数列挙して t=0 を取り違えていないことを確認している: この収録は固定 60fps で、t=0 のフレームから各フレームが 16.7ms 刻みで並ぶ。
ただし **t=0 から +500ms ちょうどの位置にはフレームが 1 枚欠けている**ため、その位置だけ直近の +483ms のフレームを使い、ファイル名も `t0483ms` とした。

遷移は iOS の push (右からのスライドイン)。実機の収録では t=0 のフレームでデモ画面が右端に**約 45px (画面幅の約 5%)** しか出ていない。Simulator の 1.3 の t=0 は約 1/3 幅であり、実機の収録のほうが早い段階のフレームを捉えている。

保存した 14 枚はすべて目視した。ステータスバーに写っているのは時刻・位置情報の矢印・電波 / Wi-Fi / 電池のみで、通知バナー・アカウント名・個人名の類は写っていない。

### 1.3-c Root header・Section の View だけの Header / Footer (AccessoryViewsDemoPage)

| ファイル | 観測内容 |
|---|---|
| `before-ios-device-accessory-views-t0000ms.png` | デモ画面の内容が出た最初のフレーム (右端に約 45px)。見えている範囲の帯の切れ目は ① Root Header View・② Section Header View / Section Footer View・③ 折り返す Label の Footer View・④ Header View・⑤ 差し替え前の Header View の各領域と一致し、**view accessory は最初のフレームから在る**。y=1068px (③ の領域の上端) までは落ち着いた状態と同じ縦位置だが、そこから下は落ち着いた状態より 57px 上にある (③ の Footer 領域が 1 行分低い) |
| `before-ios-device-accessory-views-t0100ms.png` | スライド途中 (画面幅の約 6 割)。すべての view accessory が在り、③ も 3 行の高さで確定している。以降の縦位置は落ち着いた状態と同じ |
| `before-ios-device-accessory-views-t0200ms.png` | スライド途中 (約 9 割)。内容に変化なし |
| `before-ios-device-accessory-views-t0300ms.png` | ほぼスライド完了。内容に変化なし |
| `before-ios-device-accessory-views-t0400ms.png` | スライド完了。先頭行は ① Root Header View で、手でスクロールし直す必要はない |
| `before-ios-device-accessory-views-t0483ms.png` | 落ち着いた状態 (収録に +500ms ちょうどのフレームが無いため直近の +483ms) |
| `before-ios-device-accessory-views-t1400ms.png` | 落ち着いた後。t0400ms 以降と同じ |

観測の要点:

- **修正前の時点で既に、view accessory は最初のフレームから在る。**Android の 1.1 で見えた「画面内容が出てから約 300〜400ms 後にまとめて挿入される」現象は実機 iOS でも起きていない (Simulator の 1.3-a と同じ)
- 先頭側への後からの挿入も起きないため、遷移直後の表示がリスト先頭 (① Root Header View) から始まる
- ④ の text → View の二段の見え方も起きない
- 落ち着いた状態 (`t1400ms`) を基準に、各フレームの最も一致する平行移動量を水平 ±600px・垂直 ±8px の範囲で探すと、**t0100ms 以降は縦のずれがどのフレームでも 0px**、その位置での画素差の平均は 5.9/255 以下 (残りの差は遷移中に navigation bar のタイトルが動くため)。t0000ms は見えている幅が約 45px しかなく平行移動の当てはめが利かないので、全幅に伸びる帯の境界の y 位置で突き合わせた
- **③ 折り返す Label を持つ Footer View だけは、最初の 3 フレームで 1 行分 (57px = 19pt) 低い。**④ の領域の上端は t=0・+17ms・+33ms のフレームで y=1273px、**+50ms 以降は y=1330px** で落ち着いた状態と一致する。③ の領域の**上端** (y=1068px) はどのフレームでも動かないため、伸びているのは ③ の領域そのものである (折り返しが 2 行 → 3 行に確定する)。後続の ④〜⑧ はその分だけ上にあり、+50ms で下へ 57px ずれて確定する
- この高さのブレは Simulator の 1.3-a では観測されていない (縦のずれ 0px)。**実機でだけ見える差**である。修正前から在る挙動なので本 change が持ち込んだものではないが、6.3 (修正後の実機) では同じ位置 (④ 領域の上端の y) を同じ方法で測り、ブレが増えていないことを確認する

### 1.3-d CustomCell の Content (CustomCell の MAUI 固有デモ)

**注意: 今回の収録は「CustomCell デモ」ではなく「CustomCell の MAUI 固有デモ」への遷移である。**Simulator の 1.3-b (CustomCellDemoPage) とは画面が異なるため、両者は同じ画面どうしの比較にはならない。どちらも CustomCell の Content を持つ画面であり、tasks 6.3 が合否とする「Content が後着していないこと」の観測には使える。

| ファイル | 観測内容 |
|---|---|
| `before-ios-device-customcell-t0000ms.png` | デモ画面の内容が出た最初のフレーム (右端に約 45px)。見えている範囲に ① Content の差し替えと null 遷移・② ItemTemplate 生成の 3 行・③ 切断・再接続をまたぐ復元の各領域があり、**空の行は無い** |
| `before-ios-device-customcell-t0100ms.png` | スライド途中 (画面幅の約 6 割)。① Content A・テンプレート行 A / B / C・③ 離脱前の Content がすべて在る |
| `before-ios-device-customcell-t0200ms.png` | スライド途中 (約 9 割)。内容に変化なし |
| `before-ios-device-customcell-t0300ms.png` | スライド完了 |
| `before-ios-device-customcell-t0400ms.png` | 落ち着いた状態 |
| `before-ios-device-customcell-t0483ms.png` | 同上 (収録に +500ms ちょうどのフレームが無いため直近の +483ms) |
| `before-ios-device-customcell-t1400ms.png` | 落ち着いた後。t0400ms 以降と同じ |

観測の要点:

- **修正前の時点で既に、CustomCell の Content は最初のフレームから在る。**Android の 1.2 で見えた「行はあるが Content が約 250ms 遅れて入り、行の高さが変わる」現象は実機 iOS では起きていない
- 全幅に伸びる帯の境界 (各 Section の header / footer 領域の上下端 y=994 / 1105 / 1217 / 1329 / 1516 / 1629 / 1725px) は、**t0000ms の時点で落ち着いた状態と完全に一致する**。1.3-c の ③ のような高さの確定の遅れは無い
- 平行移動の突き合わせでも t0100ms 以降は**縦のずれ 0px**・画素差の平均 4.3/255 以下
- `kasane/changes/ios-customcell-initial-height-grow-animation` が扱う「Content は最初から在るが行の高さだけが後から変わる」現象は、**この実機・この画面の初回表示では観測できなかった**。あちらの change への追記材料はこの収録からは得られていない

### 6.3 (実機) で見るときの注意

- 同じ端末・同じ手順・同じ t=0 の決め方で撮り、`after-ios-device-` 接頭辞で対応させる
- customcell は今回と同じ「CustomCell の MAUI 固有デモ」を撮る。Simulator 側 (CustomCellDemoPage) と揃えたければ「CustomCell デモ」を別に足す
- 遷移中はスクロールしない (t=0 の決定と平行移動の突き合わせが崩れる)
- 通知バナーが出ない状態にしてから収録する (写り込むとそのフレームは証跡に残せない)
- 判定は Simulator と同じく「修正前と同じく後着が無いこと」(退行が無いこと)。加えて **③ の高さのブレが 1.3-c と同じ 1 行分以内に収まっていること**を、④ 領域の上端の y の推移で比べる。後着が出た場合、または ③ 以外にも高さの変動が出た場合は、tasks 6.3 のとおり完了を止めてオーナーへ報告する
- tasks 6.3 はページの再訪問も求める。今回の実機収録は初回訪問だけなので、修正後は初回と再訪問の両方を撮る

## 6.1 統合ホストの疎通確認

`kasane/handbook/maui/integration-host-verification.md` の完了条件どおりに、レビュー修正まで入れた最終コードで確認し直した (facade の接続順序と切断時の後片付けが変わったため)。

| ファイル | 観測内容 |
|---|---|
| `integration-host-android-initial.png` | IntegrationHost (Android)。root header「KsSettingsView Bridge」/ 3 Section (一般=ダーク・English・footer「アプリ全体の設定」、通知設定=オン、ストレージ=0.1.0・0 MB・無効・footer「端末内に保存されたデータ」) / root footer「C# から Native Bridge を操作しています」が規約の表どおり。Section header は緑 |
| `integration-host-android-recreated.png` | 「解放 → 再生成」後。root header / footer が出ず、テーマ=「解放中に更新」・言語=`Français`・「通知設定 (解放中に更新)」・Section header がオレンジで、規約の表どおり |
| `integration-host-ios-initial.png` | IntegrationHost (iOS)。上の Android と表の内容が一致する |
| `integration-host-ios-recreated.png` | 「解放 → 再生成」後。Android と一致する |
| `maui-host-android-revisit.png` | MauiHost (Android) の再訪問後。ValueText「更新 2」・離脱中に足した「追加 1」Cell・Root Header View / Root Footer View が出ている |
| `maui-host-ios-revisit.png` | MauiHost (iOS) の同じ状態 |

MauiHost の最小手順 (「テーマ」の ValueText が最初「ライト」→「更新 1」→ 離脱中の更新を挟んだ再訪問で「更新 2」→ 離脱中に足した Cell が再訪問で出る) は両 OS で成立した。
「外観追随ボタン」の色が OS の外観切り替えで緑 ⇔ マゼンタへ変わることも両 OS で確認した (証跡は残していない)。

## 6.2 修正後の Android 実機 (解消確認)

上の「手順 (Android)」と同じ操作・同じ切り出し位置で撮った。t=0 の決め方 (デモ画面の内容が最初に描画されたフレーム) も同じ。
修正後のフレームは `after-` 接頭辞で、修正前の `before-` と同じ名前で対応する。

### Root header・Section の View だけの Header / Footer (AccessoryViewsDemoPage)

| ファイル | 観測内容 |
|---|---|
| `after-android-accessory-views-t0000ms.png` | デモ画面の内容が出た最初のフレーム (メニューとのクロスフェードの途中)。この時点で ① Root Header View・② Section Header View / Section Footer View・③ 折り返す Label の Footer View (3 行)・④ Header View・⑤ 差し替え前の Header View が**すべて在る**。④ は text のフォールバックではなく View 表示で出ている。先頭行は ① Root Header View |
| `after-android-accessory-views-t0100ms.png` | 同上 (重なりが解けた後)。行の挿入・位置のずれは起きない |
| `after-android-accessory-views-t0200ms.png` | 同上 (変化なし) |
| `after-android-accessory-views-t0300ms.png` | 同上 (変化なし。修正前はここで行がまとめて挿入されていた) |
| `after-android-accessory-views-t0400ms.png` | 同上 (変化なし。修正前はここで中身が描かれていた) |
| `after-android-accessory-views-t0500ms.png` | 同上 (変化なし) |
| `after-android-accessory-views-settled-no-scroll.png` | 落ち着いた後、**手でスクロールせずに**撮った状態。リスト先頭が ① Root Header View であり、修正前のように先頭側へ後から挿入されて画面外へ出ることがない |
| `after-android-accessory-views-revisit-t0000ms.png` | ページを再訪問したときの、デモ画面の内容が出た最初のフレーム。初回と同じくすべての view accessory が在り、③ の折り返す Footer も 3 行の高さで在る |

観測の要点:

- 修正前にあった「画面内容が出てから view accessory が現れるまでの約 300〜400ms」が無くなり、最初のフレームから在る
- 先頭側への後からの挿入が無くなったため、遷移直後の表示がリスト先頭から始まる
- ④ の text → View の二段の見え方も無くなった
- 落ち着いた状態を基準に、各フレームの最も一致する平行移動量を水平 ±24px・垂直 ±24px の範囲で探すと、初回・再訪問とも**縦のずれは 0px** である。t0000ms と再訪問 t0000ms の画素差が大きい (それぞれ 8.0/255・14.7/255) のはメニューとのクロスフェードによる重なりで、t0100ms 以降は 1.7/255 で一定になる。**③ の折り返す Footer を含め、行の高さが後から変わる動きは無い**

### CustomCell の Content (CustomCellDemoPage)

| ファイル | 観測内容 |
|---|---|
| `after-android-customcell-t0000ms.png` | デモ画面の内容が出た最初のフレーム。同期ステータス行・明るさ / 音量 / 無効の Slider・利用規約 / プライバシーポリシーの展開行が**すべて在る**。空の行は無い |
| `after-android-customcell-t0100ms.png` | 同上 (変化なし) |
| `after-android-customcell-t0250ms.png` | 同上 (変化なし。修正前はここで Content がまとめて入り高さが変わっていた) |
| `after-android-customcell-t0300ms.png` | 同上 (変化なし) |
| `after-android-customcell-t0400ms.png` | 同上 (変化なし) |
| `after-android-customcell-revisit-t0000ms.png` | ページを再訪問したときの最初のフレーム。初回と同じく Content が在る |

観測の要点:

- Content の約 250ms の遅れと、それに伴う行の高さ変化・後続行のずれが無くなった

## 6.3 修正後の iOS Simulator (退行確認)

1.3 と同じデバイス種別・同じ OS 版 (iPhone 17 / iOS 26.1)・同じ手順・同じ t=0 の決め方で、作業ツリー (レビュー修正まで入れた最終コード) のビルドを撮った。実機 (iPhone 11) の観測はオーナーの画面収録で後から追加する。

### 6.3-a Root header・Section の View だけの Header / Footer (AccessoryViewsDemoPage)

| ファイル | 観測内容 |
|---|---|
| `after-ios-sim-accessory-views-t0000ms.png` | デモ画面の内容が出た最初のフレーム (右端に約 1/3 幅だけ見えている)。① Root Header View・② Section Header View / Section Footer View・③ 折り返す Label の Footer View (3 行)・④ Header View・⑤ 差し替え前の Header View が**すべて在る**。④ は View 表示で、text のフォールバックは出ていない |
| `after-ios-sim-accessory-views-t0100ms.png` | スライド途中。表示内容に変化なし |
| `after-ios-sim-accessory-views-t0200ms.png` | 同上 |
| `after-ios-sim-accessory-views-t0300ms.png` | ほぼスライド完了。内容に変化なし |
| `after-ios-sim-accessory-views-t0400ms.png` | スライド完了。先頭行は ① Root Header View |
| `after-ios-sim-accessory-views-t0500ms.png` | 落ち着いた状態 |
| `after-ios-sim-accessory-views-t1400ms.png` | 落ち着いた後。t0500ms と同じ |
| `after-ios-sim-accessory-views-settled-no-scroll.png` | 落ち着いた後、手でスクロールせずに撮った状態。先頭が ① Root Header View で、③ の折り返す Footer も画面内に収まる |
| `after-ios-sim-accessory-views-settled-scrolled-to-bottom.png` | 末尾までスクロールした状態。⑥・⑦・⑧ と ① Root Footer View が在る |
| `after-ios-sim-accessory-views-revisit-t0000ms.png` | ページを再訪問したときの、デモ画面の内容が出た最初のフレーム。初回と同じくすべての view accessory が在り、③ も 3 行の高さで在る |
| `after-ios-sim-accessory-views-revisit-t1400ms.png` | 再訪問が落ち着いた状態。初回の落ち着いた状態と同じ表示 |

観測の要点:

- 修正前 (1.3-a) と同じく、view accessory は最初のフレームから在る。**後着は出ていない** (退行なし)
- 落ち着いた状態 (`t1400ms`) を基準に、各フレームの最も一致する平行移動量を水平 ±900px・垂直 ±8px の範囲で探すと、初回・再訪問とも**縦のずれは 0px**、その位置での画素差の平均は初回 3.0/255 以下・再訪問 3.5/255 以下。**行の追加・高さの変化・縦方向のずれは無い**
- **折り返す Label を持つ FooterView (③) の高さのブレは無い。**遷移直後のフレームから落ち着いた状態まで、縦のずれが 0px で一定であることが上の突き合わせで確認できる。3 行に折り返した高さは最初のフレームで既に確定している
- 修正前と修正後の落ち着いた状態は**画素単位で同一**である (`before-ios-sim-accessory-views-t1400ms.png` と `after-ios-sim-accessory-views-t1400ms.png` の差の平均 0.00/255、`settled-scrolled-to-bottom` 同士も 0.00/255)。最終的な見た目は変わっていない

### 6.3-b CustomCell の Content (CustomCellDemoPage)

| ファイル | 観測内容 |
|---|---|
| `after-ios-sim-customcell-t0000ms.png` | デモ画面の内容が出た最初のフレーム (右端に約 2/3 幅が見えている)。同期ステータス行・明るさ / 音量 / 無効の Slider・利用規約 (折りたたみ) / プライバシーポリシー (展開中、4 行の本文) が**すべて在る**。空の行は無い |
| `after-ios-sim-customcell-t0100ms.png` | スライド途中。表示内容に変化なし |
| `after-ios-sim-customcell-t0200ms.png` | 同上 |
| `after-ios-sim-customcell-t0300ms.png` | スライド完了 |
| `after-ios-sim-customcell-t0400ms.png` | 落ち着いた状態 |
| `after-ios-sim-customcell-t0500ms.png` | 同上 |
| `after-ios-sim-customcell-t1400ms.png` | 落ち着いた後。t0400ms 以降と同じ |
| `after-ios-sim-customcell-revisit-t0000ms.png` | ページを再訪問したときの最初のフレーム。初回と同じく Content が在る |
| `after-ios-sim-customcell-revisit-t1400ms.png` | 再訪問が落ち着いた状態 |

観測の要点:

- 修正前 (1.3-b) と同じく Content は最初のフレームから在る。**後着は出ていない** (退行なし)
- 同じ突き合わせで、初回・再訪問とも**縦のずれは 0px**、画素差の平均は 3.1/255 以下。行の高さが後から変わる動きは観測できなかった
- 1.3-b と同じく、`kasane/changes/ios-customcell-initial-height-grow-animation` が扱う「Content は最初から在るが行の高さだけが後から変わる」現象は、**この Simulator・この画面の初回表示でも再訪問でも観測できなかった**。あちらの change への追記材料はこの環境からは得られていない

## 6.3 (iOS 実機) 修正後の観測

Simulator の 6.3 に続けて、**iOS 実機 (iPhone 11) の修正後**をオーナーの画面収録から観測した。
1.3 (実機) が初回訪問だけだったのに対し、今回の収録は**初回訪問と再訪問の両方**を含む。

撮影されたビルドは、レビュー修正まで入れた最終コード + Sample の追加項目 (③) のフルビルドである。

### 環境

| 項目 | 内容 |
|---|---|
| 端末 | iOS 実機 iPhone 11 (1.3 の実機と同じ) |
| アプリ | `samples/maui/KsSettingsView.Sample.Maui` (Debug) |
| ビルド元 | 修正後の最終コード + Sample の追加項目 (③) |
| 収録 | オーナーが端末の画面収録機能で撮影。**動画はリポジトリに置かず手元保管** |
| 画像 | 端末の画面解像度そのまま (828 × 1792)。縮小はしていない |
| 撮影日 | 2026-09-18 |

### 収録された 3 本の内容

| 収録 | 遷移先のメニュー項目 | 初回 | 再訪問 | 証跡の接頭辞 |
|---|---|---|---|---|
| 1 本目 | 「Header / Footer への View 配置デモ」 | あり | あり | `after-ios-device-accessory-views-` |
| 2 本目 | 「CustomCell の MAUI 固有デモ」 | あり | あり | `after-ios-device-customcell-maui-` |
| 3 本目 | 「CustomCell デモ」 | あり | あり | `after-ios-device-customcell-` |

**接頭辞の対応に注意**: 修正前の実機証跡 `before-ios-device-customcell-*` は「**CustomCell の MAUI 固有デモ**」(1.3-d) のものである。
したがって修正前後で同じ画面どうしの比較になるのは `before-ios-device-customcell-*` ⇔ `after-ios-device-customcell-maui-*` の組である。
`after-ios-device-customcell-*` は Simulator 側 (1.3-b / 6.3-b) と同じ「CustomCell デモ」(`CustomCellDemoPage`) で、実機の修正前は撮られていない。

### 手順 (1.3 の実機と同じ)

1. アプリを起動し直してルートメニューを表示した状態にする (初回訪問の観測にするため)
2. 端末の画面収録を開始し、約 2 秒後にメニュー項目をタップする
3. 落ち着いてから戻る操作でメニューへ戻り、同じ項目をもう一度タップする (再訪問)
4. 収録を止め、動画を手元へ渡す
5. **デモ画面の内容が最初に描画されたフレーム**を t=0 とし、そこから 100ms 刻みでフレームを切り出す

切り出しは 1.3 (実機) と同じく、復号したフレームをフレーム番号で取り出す小道具で行った (時刻の丸めを挟まない)。
実フレームの表示時刻を全数列挙して t=0 を取り違えていないことを確認している: 3 本とも 60fps で、t=0 の直前は画面が 10 フレーム以上まったく動いていない (差分 0.000) ため、t=0 は遷移の最初のフレームである。
1.3 (実機) と同じく **t=0 から +500ms ちょうどの位置にはフレームが 1 枚欠けている** (6 通りの遷移すべてで同じ)。その位置だけ直近の +483ms のフレームを使い、ファイル名も `t0483ms` とした。

遷移は iOS の push (右からのスライドイン)。t=0 のフレームでデモ画面が右端に出ている幅は収録ごとに 44〜80px (画面幅の約 5〜10%) で、1.3 (実機) の約 45px と同じ段階にあたる。

再訪問は `-revisit-` を名前に入れ、t=0 と落ち着いた状態 (`t1400ms`) の 2 枚を残した (Simulator の 6.3 と同じ粒度)。

保存した 27 枚は、写り込みが起きうる上部の帯 (ステータスバーと navigation bar) を全枚数分並べて目視し、加えて各収録の t=0・落ち着いた状態は全面を目視した。
ステータスバーに写っているのは時刻・集中モード / 位置情報のアイコン・電波 / Wi-Fi / 電池のみで、通知バナー・アカウント名・個人名・端末名の類は写っていない。画面の中身は Sample の架空のデモデータだけである。

### 測り方 (修正前と同じ)

- **行の境界の y**: 遷移中のフレームから新しいページの左端 x を「多くの行で同時に立つ縦の境界」として求め、その内側の左余白 (文字の無い帯) の色が変わる y を列挙する。ページが画面全体に出た後は左端 x = 0 として同じことをする
- **平行移動の突き合わせ**: 落ち着いた状態 (`t1400ms`) を基準に、最も一致する平行移動量を水平は左端 x の近傍・垂直 ±8px の範囲で探し、その位置での画素差の平均を取る (ステータスバーと home indicator の帯は除く)
- 修正前の証跡 (`before-ios-device-*`) も**同じ小道具で測り直した**。1.3-c の本文にある y の値 (③ の高さのブレを ④ 領域の上端 y=1273 → 1330 と記した) は、この測り方では **y=1271 → 1327** になる (2〜3px は境界の拾い方の差)。以下の比較はすべて測り直した値どうしで行う

### 6.3-c Root header・Section の View だけの Header / Footer (AccessoryViewsDemoPage)

| ファイル | 観測内容 |
|---|---|
| `after-ios-device-accessory-views-t0000ms.png` | デモ画面の内容が出た最初のフレーム (右端に約 80px)。見えている範囲に ① Root Header View (帯と青いバーの中身)・② Section Header View / Section Footer View・③ 折り返す Label の Footer View・④ Header View・⑤ 差し替え前の Header View の各領域が**すべて在る**。④ は text のフォールバックではなく View (紫の帯) で出ている。行の境界は ③ の領域の上端 (y=1068) まで落ち着いた状態と同じで、そこから下は 56px 上にある (③ の Footer 領域が 1 行分低い) |
| `after-ios-device-accessory-views-t0100ms.png` | スライド途中 (画面幅の約 7 割)。すべての view accessory が在り、③ も 3 行の高さで確定している。以降の縦位置は落ち着いた状態と同じ |
| `after-ios-device-accessory-views-t0200ms.png` | スライド途中 (約 9 割)。内容に変化なし |
| `after-ios-device-accessory-views-t0300ms.png` | ほぼスライド完了。内容に変化なし |
| `after-ios-device-accessory-views-t0400ms.png` | スライド完了。先頭行は ① Root Header View で、手でスクロールし直す必要はない |
| `after-ios-device-accessory-views-t0483ms.png` | 落ち着いた状態 (収録に +500ms ちょうどのフレームが無いため直近の +483ms) |
| `after-ios-device-accessory-views-t1400ms.png` | 落ち着いた後。t0400ms 以降と同じ |
| `after-ios-device-accessory-views-revisit-t0000ms.png` | 再訪問の、デモ画面の内容が出た最初のフレーム (右端に約 70px)。初回と同じくすべての view accessory が在り、③ の 1 行分の低さも初回と同じ |
| `after-ios-device-accessory-views-revisit-t1400ms.png` | 再訪問が落ち着いた状態。初回の落ち着いた状態と**画素単位で同一** (差の平均 0.00/255) |

観測の要点:

- **view accessory は初回・再訪問とも最初のフレームから在る。後着は出ていない** (退行なし)
- 先頭側への後からの挿入も起きないため、遷移直後の表示がリスト先頭 (① Root Header View) から始まる。④ の text → View の二段の見え方も無い
- 平行移動の突き合わせで、初回・再訪問とも**縦のずれはどのフレームでも 0px**。画素差の平均は t0100ms 以降 2.2/255 以下 (t0000ms は見えている幅が狭く、③ の 1 行分のずれと navigation bar のタイトルの動きが効くため 8.6/255 以下)
- 修正前と修正後の落ち着いた状態は**画素単位で同一**である (`before-ios-device-accessory-views-t1400ms.png` との差の平均 0.00/255)

**③ 折り返す Label を持つ FooterView の高さのブレ** (tasks 6.3 の比較対象) — ④ 領域の上端 y の推移:

| 経過時間 | 修正前 (1.3-c) | 修正後 初回 | 修正後 再訪問 |
|---|---|---|---|
| t=0 | 1271 (測り直し) | 1271 | 1271 |
| +17ms | 1273 (1.3-c 本文) | 1271 | 1271 |
| +33ms | 1273 (1.3-c 本文) | **1327** | **1327** |
| +50ms | **1330** (1.3-c 本文) | 1327 | 1327 |
| +100ms 以降 | 1327 (測り直し) | 1327 | 1327 |
| 落ち着いた状態 | 1327 (測り直し) | 1327 | 1327 |

「測り直し」は `before-ios-device-accessory-views-*.png` を今回と同じ小道具で測った値、「1.3-c 本文」はその節に書かれている値である (系の差は 2〜3px)。
修正前の +17ms / +33ms / +50ms のフレームは証跡として残っていないため、1.3-c 本文の記載をそのまま引いた。

- ③ の領域の**上端** (y=1068) はどのフレームでも動かない。伸びているのは ③ の領域そのもの (折り返しが 2 行 → 3 行に確定する) であり、修正前と同じ現象である
- ブレの**大きさは 56px (1 行分) で修正前と同じ**。**増えていない**。落ち着くまでの時間はむしろ短い (修正前は +50ms、修正後は +33ms)
- ③ 以外の行の境界は t=0 から落ち着いた状態まで一致しており、**修正前に無かった高さの変動は出ていない**

**この収録で確認できない項目**: ① Root Footer View はリストの末尾にあり、この収録では遷移中もその後も画面外である (収録中にスクロール操作は無い)。Root footer の表示は Simulator の 6.3-a (`after-ios-sim-accessory-views-settled-scrolled-to-bottom.png`)・6.1 の MauiHost (`maui-host-ios-revisit.png`)・6.4 の Native Sample で確認済みで、実機でだけ違う挙動になる要素ではない。

### 6.3-d CustomCell の Content (CustomCell の MAUI 固有デモ)

修正前の実機証跡 `before-ios-device-customcell-*` (1.3-d) と同じ画面である。

| ファイル | 観測内容 |
|---|---|
| `after-ios-device-customcell-maui-t0000ms.png` | デモ画面の内容が出た最初のフレーム (右端に約 55px)。① Content の差し替えと null 遷移 (Content A のピンクの領域)・② ItemTemplate 生成の 3 行 (各行のシアンの Content)・③ 切断・再接続をまたぐ復元 (離脱前の Content) が**すべて在る**。空の行は無い |
| `after-ios-device-customcell-maui-t0100ms.png` | スライド途中 (約 7 割)。表示内容に変化なし |
| `after-ios-device-customcell-maui-t0200ms.png` | スライド途中 (約 9 割)。内容に変化なし |
| `after-ios-device-customcell-maui-t0300ms.png` | ほぼスライド完了 |
| `after-ios-device-customcell-maui-t0400ms.png` | スライド完了 |
| `after-ios-device-customcell-maui-t0483ms.png` | 落ち着いた状態 (収録に +500ms ちょうどのフレームが無いため直近の +483ms) |
| `after-ios-device-customcell-maui-t1400ms.png` | 落ち着いた後。t0300ms 以降と同じ |
| `after-ios-device-customcell-maui-revisit-t0000ms.png` | 再訪問の最初のフレーム (右端に約 65px)。初回と同じく Content が在る |
| `after-ios-device-customcell-maui-revisit-t1400ms.png` | 再訪問が落ち着いた状態。初回の落ち着いた状態と同じ (差の平均 0.02/255) |

観測の要点:

- **Content は初回・再訪問とも最初のフレームから在る。後着は出ていない** (退行なし)
- 行の境界 (各 Section の header / footer 領域の上下端 y=288 / 383 / 807 / 994 / 1329 / 1516) は、**t=0 の時点で落ち着いた状態と完全に一致する**。修正前 (1.3-d) を同じ小道具で測り直した値と 1px も違わない
- 平行移動の突き合わせでも、初回・再訪問とも**縦のずれは 0px**・画素差の平均 1.7/255 以下
- 修正前と修正後の落ち着いた状態は**画素単位で同一**である (`before-ios-device-customcell-t1400ms.png` との差の平均 0.00/255)
- 1.3-d と同じく、行の高さが後から変わる現象は**この画面では観測できなかった**

### 6.3-e CustomCell の Content (CustomCell デモ)

Simulator の 1.3-b / 6.3-b と同じ画面 (`CustomCellDemoPage`) である。実機の修正前は撮られていないため、修正前との直接比較は無い。

| ファイル | 観測内容 |
|---|---|
| `after-ios-device-customcell-t0000ms.png` | デモ画面の内容が出た最初のフレーム (右端に約 55px)。同期ステータス行の緑のドット・明るさ / 音量 / 無効の行・利用規約 (折りたたみ、▶)・プライバシーポリシー (展開中、▼ と本文の領域) が**すべて在る**。空の行は無い |
| `after-ios-device-customcell-t0100ms.png` | スライド途中 (約 7 割)。同期ステータス・Slider 3 行・展開中の本文 4 行がいずれも在り、文言・位置とも落ち着いた状態と同じ。ただし「動的高さ」Section の行の塊の末尾 (次の Footer 領域の上端) だけが落ち着いた状態より上にある |
| `after-ios-device-customcell-t0200ms.png` | スライド途中 (約 9 割)。同上。末尾が落ち着いた状態へ近づく |
| `after-ios-device-customcell-t0300ms.png` | ほぼスライド完了。末尾が落ち着いた状態と一致する |
| `after-ios-device-customcell-t0400ms.png` | スライド完了 |
| `after-ios-device-customcell-t0483ms.png` | 落ち着いた状態 (収録に +500ms ちょうどのフレームが無いため直近の +483ms) |
| `after-ios-device-customcell-t1400ms.png` | 落ち着いた後。t0300ms 以降と同じ |
| `after-ios-device-customcell-revisit-t0000ms.png` | 再訪問の最初のフレーム (右端に約 44px)。初回と同じく Content が在る |
| `after-ios-device-customcell-revisit-t1400ms.png` | 再訪問が落ち着いた状態。初回の落ち着いた状態と同じ (差の平均 0.01/255) |

観測の要点:

- **Content は初回・再訪問とも最初のフレームから在る。後着は出ていない** — tasks 6.3 が CustomCell の合否とする条件は満たしている
- 「インライン CustomCell」「再利用 (SliderCell ラップ関数)」の各 Section の行の境界 (y=288 / 383 / 599 / 786 / 1109 / 1254) は、**t=0 から落ち着いた状態まで 1px も動かない**
- いっぽう **「動的高さ」Section (利用規約 / プライバシーポリシー) の行の塊だけが、Content が在るまま高さを増やす。**次の Footer 領域の上端の y は下表のとおり約 300ms かけて 149px 下がる。展開中の本文そのものは t=0 の時点で 4 行ぶんの位置に描かれており、行の高さがそれに追いつく形である (途中のフレームでは本文が行の枠からはみ出して見える)

| 経過時間 | 初回 | 再訪問 |
|---|---|---|
| t=0 | 1543 | 1543 |
| +17ms | 1544 | 1545 |
| +33ms | 1547 | 1548 |
| +100ms | 1580 | 1583 |
| +200ms | 1660 | 1662 |
| +300ms | 1692 | 1692 |
| +400ms 以降・落ち着いた状態 | 1692 | 1692 |

- これは view accessory の高さの変動ではなく、**CustomCell の行の高さだけが後から変わる現象**である。tasks 6.3 の指示どおり本 change の合否には含めず、`kasane/changes/ios-customcell-initial-height-grow-animation` の exploration へ観測内容を追記した (2026-09-18 の追記)
- 平行移動の突き合わせでは初回・再訪問とも**縦のずれ 0px**・画素差の平均 2.7/255 以下。上の高さの変化は画面末尾の限られた範囲に閉じているため、この突き合わせでは出ない (行の境界の y でのみ見える)

### 6.3 (実機) の判定

| 判定項目 (tasks 6.3) | 結果 |
|---|---|
| Root の header・Section の HeaderView / FooterView (折り返す ③ を含む)・CustomCell の Content が、初回と再訪問の両方で最初のフレームから在る (後着が無い) | **満たす** (6.3-c / 6.3-d / 6.3-e)。Root の footer だけは画面外のためこの収録では見えない (上記のとおり別の証跡で確認済み) |
| 1.3 (実機) の修正前には無かった view accessory の高さの変動が出ていない | **満たす**。③ 以外の行の境界は t=0 から一致する |
| ③ の高さのブレが 1.3-c と同じ 1 行分以内に収まっている | **満たす**。56px で修正前と同じ大きさ、収まる時刻はむしろ早い (+50ms → +33ms) |
| CustomCell の「Content は最初から在るが行の高さだけが後から変わる」現象 | 「CustomCell デモ」で観測。合否には含めず `ios-customcell-initial-height-grow-animation` へ追記した |

**合格。**

## 6.4 Native Sample の退行確認

| ファイル | 観測内容 |
|---|---|
| `native-sample-android-dsl.png` | Android Native Sample の DSL 方式デモ。root header「DSL 方式のデモ画面」と root footer「© 2026 KsSettingsView Sample」が出ている |
| `native-sample-ios-dsl.png` | iOS Native Sample の同じ画面。root header / footer が出ている。root footer の背景がアクセント色で塗られる見え方は修正前のビルドでも同じで、本 change による変化ではない |

iOS Native Sample は Simulator で確認した (root header / footer の表示は実行時タイミングに依存しないため)。

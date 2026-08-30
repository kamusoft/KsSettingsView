# Exploration: investigate-maui-icon-lease-sharing

## 課題 / 動機

MAUI facade の icon 実体化 (maui/ADR-0015) で、**複数の `KsImageLease` が同一の platform 画像インスタンスを包む**余地がある。片方のリースを破棄すると、まだ表示に使われているもう片方の画像の後片付け (Android の bitmap リサイクル等) が走り、表示欠け・最悪 recycled bitmap 参照によるクラッシュに至り得る。

発見の文脈: `fix-maui-icon-lease-disposal-ordering` の探索 (2026-08-22) で、同 change のスコープから切り出した。出典は `add-maui-basic-input-cells` の review-002「保留 (b)」。

### 2 枚のリースが同時に存在する経路 (いずれも実在)

1. **同じ Cell の再解決 (再接続)** — `ReleaseHost()` は意図的にリースを破棄せず (maui/ADR-0007、review-002 保留 (a) で支持済み)、再訪問時の `AttachImages()` が登録済み全 Cell に `ResolveIcon` をやり直す。この瞬間、表示を支えている旧リースと新リースが同時に存在する
   - `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:247-292` (AttachImages / ReleaseHost)
2. **2 つの Cell が同一 `ImageSource` を持つ** — `_icons` は Cell ごとにリースを持つため 2 枚になる。片方の Cell の削除・差し替えで、残った Cell の画像が壊れ得る

### 現在の実装

`StoreIcon` は `ReferenceEquals(previous?.Image, lease?.Image)` で **dirty 化は抑止しているが、破棄は抑止していない** (`KsSettingsController.cs:1609-1618`)。

### 引き金の条件と確度

| 条件 | 状況 |
|---|---|
| 2 枚のリースが同時に存在する | **普通に起きる** (ページ再訪問は日常操作) |
| その 2 枚が同一 platform 画像を指す | **未確認** — MAUI ローダーのキャッシュ挙動に依存 |
| 破棄 `Action` が共有資源を落とす | **未確認** — 参照カウントのデクリメントなら無害 |

file / resource / font 経路は破棄 `Action` が null で実害なし (review-002 の IL 解析)。image loader 経路 (Uri / Stream) のみが対象。

## 調査結果 (2026-08-24, ksn-scout / dotnet/maui タグ 10.0.70 実ソース)

前提: 当プロジェクトは Microsoft.Maui.Controls 10.0.70。Android は `ImageView` を渡さない Context オーバーロード (`GetDrawableAsync(source, context)`) を使うため Glide の Target は `MauiCustomTarget` (破棄 Action あり)。

### 論点 2 の答え: 破棄 Action の意味論は OS で真逆

- **Android: 参照カウントのデクリメント (安全)**。`MauiCustomTarget.clear()` → `Glide.clear(Target)` → `EngineResource.release()` (`--acquired`)。リース1つ = Glide 参照1つ。片方の破棄で他方は壊れない。MAUI ソース内 TODO コメントが「C# 側が dispose しないと Glide キャッシュエントリが解放されない」と明記 — **破棄は正規手順であり、スキップは即リーク**
  - ~~注意: `clear()` は main looper へ post されるため破棄は1ループ遅延する~~ → **訂正 (2026-08-24 ローカルソース検証)**: `MauiCustomTarget.post()` は**メインスレッド上ならその場で同期実行**する (`looper.isCurrentThread()` 分岐)。当ライブラリの破棄は UI スレッド契約下で走るため実際には遅延しない。`fix-maui-icon-lease-disposal-ordering` の順序保証 (破棄は native 配信の後) は Android でもそのまま成立する
- **iOS: 一発破壊**。file/font/uri/stream の4サービス全てが `() => image.Dispose()` を破棄 Action に渡す。`ImageSourceServiceResult` に参照カウントは無い。共有された UIImage を包む片方のリースを破棄すると他方は即壊れる

### 論点 1 の答え: 同一 platform 画像インスタンスの共有は iOS の1経路のみ

| OS / 経路 | 共有 | 備考 |
|---|---|---|
| iOS file 主経路 (`CGImageSource.FromUrl`) | なし | 毎回新規 UIImage |
| iOS file フォールバック (`UIImage.FromBundle`) | **あり (条件付き)** | asset catalog 画像等で主経路が失敗した場合。システムキャッシュが同一 UIImage を返す |
| iOS font / uri / stream | なし | uri のキャッシュはディスク上 NSData のみ、UIImage は毎回新規 |
| Android file (resource id) | 実質なし | ConstantState 共有の別インスタンス。破棄 Action も null |
| Android file/font/uri (Glide) | Bitmap は共有、Drawable は毎回新規 | refcount 保護で破棄は無害 |
| Android GIF | あり (推定) | `GifDrawableResource` が同一インスタンス返し。refcount 上は安全だが同一 GifDrawable の複数 View 添付はアニメーション状態が壊れる別問題あり |
| Android stream | なし | `skipMemoryCache(true)` 固定 |

### 総合判定

**「2枚のリースが同一 platform 画像を包み、片方の破棄が他方を壊す」シナリオは iOS にのみ実在** (FromBundle フォールバック時)。Android には実質存在せず、破棄はむしろ必須。

### 副産物 (既存所見の訂正)

- review-002 保留 (b) の「iOS の file / font サービスは破棄 Action が null で実害なし」は**誤り** (実際は `image.Dispose()` を渡している)。「主経路では実害なし」という結論自体はインスタンス非共有により維持されるが、理由が違う。Android の file (resource id) 分岐のみ「破棄 Action null」が正しい
- 残る不確実性: `UIImage.FromBundle` が同一 managed peer を返す点と GIF の同一インスタンス返しは実ソースからの推定で未実測。確証には probe テストが要る

### ローカルソースによる再検証 (2026-08-24, `../maui/` = dotnet/maui main f27ca83)

- ✅ iOS 4サービス全てが `() => image.Dispose()` を破棄 Action に渡すことを実物確認 (`src/Core/src/ImageSources/*/**.iOS.cs`)
- ✅ FromBundle フォールバック実在を確認 (`src/Core/src/ImageSources/iOS/ImageSourceExtensions.cs:67` — `UIImage.FromBundle(bundleName) ?? UIImage.FromFile(file)`、`cgImageSource` null 時)
- ✅ `MauiCustomTarget.clear()` の TODO コメント (C# 側 dispose が Glide キャッシュ解放に必要) を確認
- ⚠️ 「1ループ遅延」は上記のとおり訂正 (メインスレッドなら同期実行)

### probe 実測: iOS の同一 UIImage 共有 (2026-08-25, tasks 1.1)

未実測だった「`UIImage.FromBundle` 経由の 2 回解決が同一インスタンスを返すか」を実機 (Simulator) 上で計測した。

**計測環境**

| 項目 | 値 |
|---|---|
| 実行環境 | iOS Simulator (iPhone 17 Pro) / iOS 26.0.1。実機は未実施 |
| ビルド | `net10.0-ios` Debug (SDK 10.0.300 / workload 10.0.300.3)、Xcode 26.5.0 |
| MAUI | Microsoft.Maui.Controls 10.0.20 (workload manifest) |
| probe 足場 | `maui/spike/app/KsBindingSpikeApp` を一時改造 (計測後に元へ戻し済み) |
| 反復数 | 1 ケースあたり 5 回 |
| 観測点 | `ReferenceEquals` (managed 参照) と `UIImage.Handle` (native handle) の双方を、1 回目の結果と比較 |

計測は 2 系統で行った。(1) UIKit を直に叩く `UIImage.FromBundle` / `UIImage.FromFile`、(2) MAUI の
`IImageSourceService.GetImageAsync` 経由 (`KsImageResolver` と同じ経路)。asset 種別は 3 つ用意した
— asset catalog (`Assets.xcassets` の imageset)、bundle 直下 png (`MauiAsset` で配置)、
resizetizer 生成の bundle 直下 png (`MauiImage` の `dotnet_bot.png`)。

**結果 (5 回とも同じ判定。`ReferenceEquals` と handle 一致は全ケースで完全に一致した)**

| 系統 / 呼び出し | asset 種別 | 2 回目以降が 1 回目と同一か |
|---|---|---|
| (1) `UIImage.FromBundle("probe_asset")` | asset catalog | **同一** (ref / handle とも一致) |
| (1) `UIImage.FromBundle("probe_bundle.png")` | bundle 直下 png | **同一** |
| (1) `UIImage.FromBundle("probe_bundle")` | bundle 直下 png / 拡張子なし名 | **同一** |
| (1) `UIImage.FromBundle("dotnet_bot")` | MauiImage 生成 png / 拡張子なし名 | **同一** |
| (1) `UIImage.FromBundle("dotnet_bot.png")` | MauiImage 生成 png | **同一** |
| (1) `UIImage.FromFile("probe_bundle.png")` | bundle 直下 png | 毎回別 (ref / handle とも不一致) |
| (2) `GetImageAsync(FromFile("probe_asset"))` | asset catalog | **同一** (FromBundle フォールバック) |
| (2) `GetImageAsync(FromFile("probe_bundle"))` | 拡張子なし名 | **同一** (FromBundle フォールバック) |
| (2) `GetImageAsync(FromFile("probe_bundle.png"))` | bundle 直下 png | 毎回別 (主経路 `CGImageSource`) |
| (2) `GetImageAsync(FromFile("dotnet_bot.png"))` | MauiImage 生成 png | 毎回別 (主経路 `CGImageSource`) |

いずれのケースでも解決に使われたサービスは `FileImageSourceService` だった。

**事実としての読み取り**

- **共有は再現した。** `KsImageResolver` が使う MAUI の解決経路 (系統 2) で、asset catalog 画像と
  拡張子なしファイル名の 2 種別について、5 回の解決がすべて同一の `UIImage` を返した
- **共有時は managed 参照と native handle が常に同時に一致した。** 一方だけが一致するケースは
  1 件も観測されなかった (native 側が同一オブジェクトなら runtime が同じ managed peer を返すため)。
  したがって共有表のキーは managed 参照・native handle のどちらでも同じ集合を作る
- 拡張子付きで実ファイルが存在する名前 (`probe_bundle.png` / `dotnet_bot.png`) は主経路
  (`CGImageSource.FromUrl`) が成功するため毎回別インスタンスであり、「調査結果」の表と一致した
- 共有ケースでは 2 つのリースが**文字どおり同じ managed オブジェクト**を保持する。したがって
  片方のリースの `Dispose()` はもう片方が持つ `UIImage` の `Dispose()` そのものであり、
  「片方の破棄が他方の表示を壊す」条件は成立している (追加の計測を要しない論理的帰結)

**計測範囲の限界** — Simulator (iOS 26.0.1) の 1 環境のみ。実機・他 iOS バージョン・`Uri` / `Stream` /
font 経路は未計測。ただし共有の発生源は UIKit の名前付き画像キャッシュであり、asset 種別による
差 (拡張子の有無で主経路が成否を分ける) は上記で押さえた。

### 付随確認: 破棄順序保証が Android でも成立するか (2026-08-25, tasks 4.1)

**結論: 成立する。**

成立条件のうち「`MauiCustomTarget` の後片付けは要求時点より後ろへ送られることはあっても先行しない」は
上記「ローカルソースによる再検証」で確定済み (メインスレッド上なら `post()` は同期実行)。残る確認
「facade のリース破棄が UI スレッド以外から走る経路が無い」をコードで検めた。

根拠:

1. **facade はスレッドを跨ぐ構造を一切持たない。** `maui/KsSettingsView.Maui/` 全体に対し
   `Task.Run` / `ThreadPool` / `new Thread` / `Timer` / `ConfigureAwait` / ファイナライザ / `GC.*` の
   いずれも該当なし (0 件)
2. **リース破棄の口はすべて同期呼び出しの延長にある。** `KsImageLease` の破棄は
   `KsSettingsController` の 4 箇所 (`CompleteIcon` の不採用結果、`StoreIcon` の未登録 Cell、
   `DisposeRetired`) に限られ、`DisposeRetired` の呼び出し元は `Disconnect` / `RebuildRoot` /
   `OnObservedCollectionChanged` / `HandleSectionPropertyChanged` の `Cells` 分岐 / `Flush` の 5 系統。
   いずれも自前でスレッドを移さない
3. **唯一の非同期は解決完了の戻りで、そこは呼び出し元のスレッドへ戻る。**
   `Platforms/{iOS,Android}/KsImageResolver.ResolveAsync` は `ConfigureAwait` を付けずに `await` する
   ため、継続は `Resolve` 呼び出し時に捕捉した同期コンテキスト (UI スレッド) 上で走る
4. **バッチ配信の予約先も UI スレッド。** `Flush` は `KsMauiDispatcher` 経由で MAUI の
   `IDispatcher.Dispatch` に積まれる
5. **呼び出し側契約が UI スレッドに固定されている。** `concepts/maui/api/maui-facade.md` の
   「全操作は UI スレッドから行う (呼び出し側契約。facade は marshal しない)」。したがって
   observer / property changed 起点の経路も契約下では UI スレッド上にある

以上より、Android でも破棄要求は UI (メイン) スレッド上でのみ発行され、`MauiCustomTarget.post()` は
同期実行される。既存の順序固定テスト 4 本 (`IconSourceTests`) が固定する「破棄は native 配信の後」は
Android の実挙動としてもそのまま成立する。

## 検討した選択肢 (却下案と理由を含む)

調査結果を受けた対処の選択肢 (2026-08-24 議論):

- **(a) iOS のみ画像単位の共有防御を足す局所修正** — **採用**。iOS の `KsImageResolver` 側で platform 画像 (native handle) 単位の参照カウントを持ち、最後のリース解放時のみ実際の `Dispose` を呼ぶ。同一 Cell の再解決・Cell 間共有の両経路を1機構でカバー。Android は現行破棄を維持
- **(b) facade 全体の所有権モデル見直し (L 級)** — 却下。守るべき穴が iOS の FromBundle フォールバック1経路に絞られたため過剰装備。MAUI 側が既に per-request でほぼ分離している
- **(c) 対処なしで許容** — 即時は却下だが**条件付き出口として保持**: probe 実測で FromBundle 共有が再現しなければ (c) に格下げして閉じる
- **(両 OS 一律の `ReferenceEquals` 破棄抑止)** — 却下。Android の破棄は Glide 参照カウントのデクリメントであり、スキップは即リーク (調査結果より)

## 決定事項

- `fix-maui-icon-lease-disposal-ordering` (破棄順序の回帰テスト、S 級) からは分離する (オーナー判断 2026-08-22)。理由: 単純な `ReferenceEquals` 抑止では (a) Cell をまたぐ共有を検出できず、(b) 破棄 `Action` が参照カウントだった場合は抑止がそのままリークになるため、S 級に収まらない
- **対処方針は「iOS のみ局所修正 + probe 実証ゲート」** (オーナー確定 2026-08-24)。実装冒頭に probe で FromBundle 共有を実証し、再現したら画像単位 refcount で防御、再現しなければ許容で閉じる。Android の破棄は維持 (スキップ禁止 — Glide 参照カウントの解放が必要)
- 完了済み `fix-maui-icon-lease-disposal-ordering` の順序保証が Android の破棄1ループ遅延 (`MauiCustomTarget.clear()` の main looper post) の下でも成立するかの再確認を、本 change に同梱する (隣接課題を別へ逃がさない)
- **spec-review 反映の設計決定** (オーナー確定 2026-08-24、second-opinion-spec-001 Major-2 / Major-4 への対応):
  - 共有表の所有は**プロセス全域 static** (Internals の純ロジック、UI スレッド契約下)。解決口の世代交代 (Host 再接続で resolver が作り直される) と SettingsView 間の共有をカバーするため。controller 所有案は世代交代は守れるが SettingsView 間が対象外になるため不採用
  - 同一画像に再解決されたときの旧リースは**即時解放**に変更 (`StoreIcon` 修正)。現行は dirty 化されず配信が起きないため退役キューに滞留し、Scenario の前提が成立しなかった。表示内容が変わらない以上配信は不要で、各リースの後片付け口は独立 (共有時は共有表が防御) のため即時で安全。遅延退役の明文化案は解放時機が非決定的なままのため不採用

## ADR 候補

未起票。probe の結果防御を実装する場合、「破棄意味論の OS 非対称 (Android = refcount 必須破棄 / iOS = 一発破壊) と iOS 側の画像単位 refcount 防御」を maui/ADR-0015 の後継 ADR として起票する (proposal 段階で判断)。probe 不成立で許容に倒れた場合は ADR 不要 — 調査結果は本 exploration が根拠として残る。

## 未決の論点

1. ~~MAUI の image loader が platform 画像インスタンスを共有するか~~ → **調査済み** (上記「調査結果」)。iOS の FromBundle フォールバックのみ共有あり
2. ~~破棄 `Action` の意味論~~ → **調査済み**。Android = 参照カウント (破棄必須)、iOS = 一発破壊
3. ~~**対処の方向性** (オーナー指摘 2026-08-22 の設計見直し案を含む) — 調査結果により「Android は現行破棄を維持すべき (スキップ不可)」「守るべきは iOS の共有ケースのみ」と判明したため、選択肢は (a) iOS のみ画像単位の共有防御を足す局所修正、(b) facade 全体の所有権モデル見直し、(c) 発生条件の狭さを踏まえ許容 (対処なし) に絞られた~~ → **決着済み**。(a) を採用 (「決定事項」参照)。probe で共有が再現したためゲートを通過し、(c) の条件付き出口は使われなかった
4. ~~再現テストの形式 — `FakeImageResolver` は解決ごとに別インスタンスを返す前提のため、共有シナリオを再現するには Fake 側に「同一画像を返す」経路の追加が要る。また iOS 実機/シミュレータでの FromBundle 共有の probe 実測も未了~~ → **決着済み**。probe 実測は上記「probe 実測: iOS の同一 UIImage 共有」で完了 (共有あり)。テスト足場は共有表を通す完了経路と、解決口インスタンスを差し替えられる再接続経路を Fake 側へ足す形で用意した
5. ~~副産物の扱い~~ → 決定事項へ昇格 (完了済み disposal-ordering の前提再確認を本 change に同梱)
6. ~~防御機構の置き場所とキー~~ → **決定済み** (2026-08-24 spec-review 反映)。所有はプロセス全域 static (世代交代・SettingsView 間をカバー)、キーは probe で `ReferenceEquals` / native handle 双方を記録して確定 (オーナー確定。second-opinion-spec-001 Major-2 への対応)

## UI 素材

なし。

## 変更級の推奨

**M** (2026-08-24 探索時点の推奨)。判定材料:

- 触る能力: MAUI icon 実体化まわり1つだが、iOS probe / iOS 防御機構 / Fake 拡張 / disposal-ordering 前提再確認と作業面が複数
- 公開 API 変更: なし
- 可逆性: 高 (facade 内部の防御機構のみ)
- UI: なし
- probe 結果次第で「許容で閉じる」出口があり、proposal でその分岐を明示する必要がある → S には収まらない

## 関連ファイル

- `maui/KsSettingsView.Maui/Internals/KsImageLease.cs`
- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs` (AttachImages / ReleaseHost / ResolveIcon / CompleteIcon / StoreIcon)
- `maui/KsSettingsView.Maui/Platforms/{iOS,Android}/KsImageResolver.cs`
- `maui/KsSettingsView.Maui.Tests/Fakes/FakeImageResolver.cs`
- 出典: `kasane/changes/archive/2026-08-11-add-maui-basic-input-cells/review-002.md` (保留 (b)、152-165 行)

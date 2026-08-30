# レビュー結果: investigate-maui-icon-lease-sharing (003 回目)

**日付**: 2026-08-25
**判定**: CHANGES_REQUESTED

## サマリー

設計の重心を「参照カウントで破棄を遅らせる」から「解決時に所有権を分類し、キャッシュ所有には後片付け口を付けない」へ移した判断そのものは妥当で、deviation.md の理由づけ (UIKit キャッシュ所有の画像に対する `Dispose()` は native を解放せず managed peer だけを無効化する) は依存先の実物と一致している。`KsSharedImageRegistry` の撤去も残存参照ゼロで完了しており、ビルド (net10.0 / net10.0-ios) とテスト (458 件全成功) も通る。

ただし**分類の核心 — 「同じ名前でキャッシュを引き直して `ReferenceEquals` で確かめる」— が、MAUI が実際に使う名前と違う名前で引いている**。参照している Microsoft.Maui.Core 10.0.70 の iOS 実装は拡張子とディレクトリを落とした名前で `imageNamed:` を呼ぶのに対し、本実装は `IFileImageSource.File` を素のまま渡す。この不一致は「キャッシュ所有なのに facade 所有と判定する」= 表示破壊側の誤分類を生み、deviation.md が置いた「誤分類は破棄しない側にだけ倒す」という安全側の不変条件が成立していない。registry を外したことで多重防御も無いため、誤分類はそのまま表示破壊に直結する。

## 指摘事項

### [🟠 Major] キャッシュの引き直しが MAUI と違う名前で行われ、自己検証になっていない

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:96` (`static name => UIImage.FromBundle(name)`) / `maui/KsSettingsView.Maui/Internals/KsFileImageOwnership.cs:61`

**問題点**:

参照している Microsoft.Maui.Core **10.0.70** (`lib/net10.0-ios26.0/Microsoft.Maui.dll` を逆コンパイルして確認) の file 画像フォールバックは次の形になっている。

```csharp
// Microsoft.Maui.ImageSourceExtensions.GetPlatformImage(IFileImageSource)
string file = imageSource.File;
string fileNameWithoutExtension = Path.GetFileNameWithoutExtension(file);
return UIImage.FromBundle(fileNameWithoutExtension) ?? UIImage.FromFile(file);
```

つまり MAUI は**拡張子とディレクトリを落とした名前**でキャッシュを引く。一方この実装は `file.File` を素のまま `UIImage.FromBundle` へ渡す。`UIImage.FromBundle(string)` は `imageNamed:` を名前そのままで呼ぶだけで、managed 側の正規化は一切入らない (Microsoft.iOS の当該 binding を逆コンパイルして確認)。したがって次の入力で照合が空振りする。

- **asset catalog 画像を拡張子付きで指定** — `IconSource = ImageSource.FromFile("logo.png")` で `Assets.xcassets` に imageset `logo` がある場合。MAUI は主経路 (`CGImageSource`) が実ファイル不在で失敗 → `imageNamed:"logo"` でキャッシュ所有の共有画像を得る。本実装は `packagedFileExists("logo.png")` が false → `imageNamed:"logo.png"` を引くが、asset catalog の資産名に拡張子は付かないため一致せず (nil か別エントリ) → **facade 所有と誤判定して後片付け口を付ける**
- **ディレクトリ付きの指定** — `"images/logo"` の場合、MAUI は `imageNamed:"logo"`、本実装は `imageNamed:"images/logo"` を引く

誤判定した後は、`KsSettingsController.StoreIcon` (`:1630-1633`) が同一画像への再解決で旧リースを**即座に破棄する**ため、Host 再接続 1 回で表示中の共有 UIImage が壊れる。本 change が塞ごうとしている欠陥そのものが、指定の書き方 (拡張子を付けるかどうか) だけで残る。

なお `probe` (tasks 1.1) と `evidence/ios-wiring-before-after.txt` の計測はすべて拡張子なしの名前 (`probe_asset` / `probe_bundle` / `probe_icon`) か、実ファイルが bundle 直下にある名前 (`probe_bundle.png` / `dotnet_bot.png` — 短絡で分類が決まる) で行われており、素の名前と MAUI の名前が食い違う組み合わせは一度も通っていない。

**推奨修正**: 引き直しの名前を MAUI の解決に合わせる (`Path.GetFileNameWithoutExtension` を通した名前で `UIImage.FromBundle` を呼ぶ)。純ロジック側 (`KsFileImageOwnership`) は「照合はキャッシュを解決に使ったのと同じ名前で行う」という契約を持っているので、その契約の中身 (どの名前が「解決に使った名前」か) を正す形になる。合わせて `maui/KsSettingsView.Maui.Tests/FileImageOwnershipTests.cs:99` の期待値を正す (下の Minor を参照)。修正後は asset catalog 画像を**拡張子付きで**指定したケースを Simulator で実測し、`evidence/` に追記してほしい。

### [🟠 Major] 実ファイルの有無による短絡が自己検証を飛ばして「破棄する側」を決めている

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsFileImageOwnership.cs:56-59`

**問題点**:

deviation.md は「誤分類時の失敗モードは破棄しない側に倒し、表示破壊側には決して倒さない」を安全側の不変条件として置いているが、`packagedFileExists(fileName)` が true の枝は `ReferenceEquals` の自己検証を通らずに `false` (= facade 所有 = 破棄する) を返す。この枝が MAUI の分岐と食い違えば、その食い違いはそのまま表示破壊側の誤分類になる。不変条件が成立するのは「短絡が MAUI の分岐と完全に一致する」ことが保証されている場合だけで、実際には一致していない。

10.0.70 の分岐は `File.Exists` ではなく **`CGImageSource` からの復号が成功したかどうか**である:

```csharp
UIImage image = platformImageSource?.GetPlatformImage(scale2) ?? imageSource.GetPlatformImage();
```

`GetPlatformImageSource` は `PlatformGetFullAppPackageFilePath` + `GetScaledFile` で解決したパスを使い、無ければ元の相対パスの URL へ落とす。したがって短絡と食い違う入力が実在する:

- **実ファイルはあるが復号できない** (重点論点 (iii)) — 実装者は「解決自体が失敗するため実害なし」と判断しているが、これは成り立たない。復号に失敗しても MAUI は例外にせず `GetPlatformImage()` へ落ち、**拡張子を落とした名前**で `imageNamed:` を引く。同名 (拡張子違い) の asset があればキャッシュ所有の共有画像が返り、こちらは短絡で既に facade 所有と決めているため破棄口が付く。確率は低いが「実害なし」ではなく、失敗の向きが安全側でもない
- **`@2x` / `@3x` だけが bundle にある名前** — MAUI は `GetScaledFile` で拾って主経路が成功する (facade 所有) が、本実装の短絡は false になり、キャッシュへの引き直し (= 新たな読み込みと常駐) が走る

**推奨修正**: 短絡を「判定」ではなく「副作用回避のためのスキップ」に戻す。すなわち短絡が成立するときだけ安全側 (キャッシュ所有 = 破棄しない) に倒すか、または短絡の成立条件を MAUI の分岐に寄せた (`GetScaledFile` 相当まで含む) うえで、残る食い違いを許容する根拠を deviation.md に明記する。少なくとも現状のように「短絡 → 破棄する」を無検証で確定させる形は避けたい。

### [🟠 Major] iOS 配線に検出力がなく、facade 所有分岐は実行時証跡も無い

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:85-99` / `kasane/changes/investigate-maui-icon-lease-sharing/evidence/ios-wiring-before-after.txt`

**問題点**:

ミューテーション実測 (lessons code-review L-001) を行った。`CleanupFor` の分類を無効化して**本 change 以前の欠陥状態に戻す**変異 (`return cacheOwned ? result : result;`) を入れても、**失敗するテストは 0 件**だった (458 件全成功のまま。iOS ビルドも通る)。つまり分類の配線は自動テストからは一切守られていない。

一方、実行時証跡はキャッシュ所有の枝しか押さえていない (`evidence` 末尾に「facade 所有の分岐は … controller レベルのテストが受け持つ」と明記されている)。しかしその controller レベルのテストは `FakeImageResolver` が所有分類の結果を**決め打ちで与える**形なので、分類そのものも配線も検証していない。結果として「どの asset 種別がどちらに分類されるか」を確かめている検査が、単体テスト (差し替えた述語の分岐のみ) 以外に一つも無い。上の Major 1 が素通りしたのはまさにこの隙間である。

**推奨修正**: Simulator ハーネス上で「解決した各 asset 種別について、後片付け口が付いたか付かなかったか」を直接観測して `evidence/` に残す (分類結果を一時ログに出すだけで足りる)。最低限、asset catalog (拡張子なし / **拡張子付き**)・bundle 直下 png・MauiImage 生成 png の 4 通り。facade 所有と分類された画像が実際に破棄されること (解放漏れが無いこと) も同じハーネスで押さえられると、Requirement の「非共有画像は直ちに後片付け」に実測が付く。

### [🟡 Minor] doc コメントが保証できない性質を断言している

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsFileImageOwnership.cs:33` (「この引き直しは既にキャッシュ済みの名前に対してのみ行うため、新たな読み込みを起こさない」)

**問題点**: 引き直しは「パッケージ内に実ファイルが無い名前」に対して行われるだけで、その名前が既にキャッシュ済みである保証はない。`@2x` だけが存在する名前や、MAUI 側がキャッシュを経由しなかった名前では、この呼び出しが新規の読み込みと UIKit キャッシュへの常駐を起こす。ファイルだけを読む人に誤った安心を与える。

**推奨修正**: 「解決がキャッシュ経由だった場合は追加の読み込みにならない」程度に弱めるか、引き直しが新規読み込みになり得る条件を明記する。

### [🟡 Minor] 単体テストが誤った契約を固定している

**該当箇所**: `maui/KsSettingsView.Maui.Tests/FileImageOwnershipTests.cs:99-114` (`TheCacheIsQueriedWithTheResolvedFileName`)

**問題点**: 「照合はキャッシュを解決に使ったのと同じ名前で行う」という意図は正しいが、期待値が `"asset_name"` を素のまま渡す形になっており、Major 1 の欠陥をテストが追認している。名前を正した後にこのテストが落ちないなら、テストは意図を守っていない。

**推奨修正**: 拡張子付き・ディレクトリ付きの入力 (`"images/logo.png"` 等) で「引かれる名前は `logo`」を固定する形にする。

### [🟡 Minor] 分類の誤りに対する二段目の守りが無くなった前提が明示されていない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1630-1634` / `maui/KsSettingsView.Maui.Tests/IconSharingTests.cs:56-76`

**問題点**: registry 撤去後は「facade 所有と分類された画像は決して共有されない」が唯一の防波堤になり、`StoreIcon` の即時解放 (同一画像への再解決で旧リースをその場で破棄) は、分類が誤れば**その場で表示を壊す**操作に変わる。`ReresolvingSameCellToSameImageReleasesPreviousLeaseImmediately` は後片付け口を持つリース同士が同一画像を共有する状況を作って即時破棄を固定しており、この前提が崩れたときの挙動を「壊れて当然」として受け入れる形になっている。即時解放自体はデルタスペックの要求なので実装の誤りではないが、前提が単一で多重防御が無いことはコード上どこにも書かれていない。

**推奨修正**: `StoreIcon` の該当箇所か `KsFileImageOwnership` の remarks に「共有され得るのはキャッシュ所有 (= 後片付け口なし) の画像だけであり、facade 所有の画像が共有されないことがこの即時解放の前提」と書き添える。

### [🔵 Suggestion] `CleanupFor` が例外を投げた場合に解決結果を取りこぼす

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:55` / `:62-67`

**問題点**: `CleanupFor` の中で例外が出るとリース生成前に catch へ抜け、`lease` が null のため `result` が破棄されない (現状 `PackagedFileExists` は握り潰し、`FromBundle` も投げにくいので実害はほぼない)。

**推奨修正**: 気になるなら分類を try で包んで安全側 (キャッシュ所有扱い) に倒すか、catch 側で `result` を破棄できる形にする。

### [🔵 Suggestion] tasks 2.1 / 3.2 の文言と実装の対応が読み取れない

**該当箇所**: `kasane/changes/investigate-maui-icon-lease-sharing/tasks.md` 2.1 / 3.2

**問題点**: 両タスクは「参照カウント機構」とその不変条件 (underflow・カウント 0 での全後片付け実行) を要求する文言のままチェック済みになっている。deviation.md が設計変更を記録しているので虚偽ではないが、蒸留時に「実装されていない要求がチェックされている」と読める。

**推奨修正**: 足場は書き換えず、deviation.md の当該項に「tasks 2.1 / 3.2 は所有権分類 (`KsFileImageOwnership`) と `FileImageOwnershipTests` で満たしたものとして読む」の一行を足す。

## 確認した観点 (指摘に至らなかったもの)

- **ビルドとテスト**: `net10.0` テスト 458 件成功 / 0 失敗、`net10.0-ios` ビルド成功 (警告 0)
- **ミューテーション実測** (L-001。使用した一時変更は backup と shasum 一致で原状復帰を確認済み):
  - `StoreIcon` の即時解放を退役キュー行きに戻す → **1 件失敗** (検出力あり)
  - `StoreIcon` の同一画像時に配信を起こす → **1 件失敗** (検出力あり)
  - `KsFileImageOwnership` の実ファイル短絡を無効化 → **2 件失敗** (検出力あり)
  - iOS `CleanupFor` の分類を無効化 (= 本 change 以前の欠陥へ戻す) → **0 件失敗** (Major 3)
- **registry 撤去の残骸**: ソース・テストとも `KsSharedImageRegistry` / `SharedImageRegistryTests` への参照ゼロ (残るのは過去のレビュー成果物のみ)
- **結果オブジェクトを破棄せず手放す判断**: `ImageSourceServiceResult` は `Value` と `Action? _dispose` のみでファイナライザを持たないことを 10.0.70 の実物で確認。GC 任せで実資源の取りこぼしは無い (コメントの主張は正しい)
- **`DisposeRetired` / `DisposeRetiredViews` の例外集約** (deviation の `[付随修正]`): 待ち行列を先に空にする既存規律を保ったまま全件試行に変わっており、`AggregateException.Flatten()` で失敗 1 件ずつ取り出せる。テストも両側 (icon / View) に付いている
- **Android 非影響**: 分類は iOS の `KsImageResolver` 内に閉じており、Android 側は無変更
- **lint**: comment-policy / local-path / identity いずれも 0 件
- **足場の逆流なし**: `proposal.md` と `specs/maui-cells/spec.md` は無変更。`tasks.md` の差分はチェックボックスのみ、`exploration.md` の追記は tasks 1.1 / 1.2 / 4.1 が指示した記録

## 重点論点への見解

**(i) 判定の重心を `ReferenceEquals` 自己検証へ移した点**

方向としては deviation の意図の範囲内で、むしろ望ましい (deviation 自身が `ReferenceEquals` を「分類の自己検証」として挙げている)。問題は移し切れていないこと。(a) 自己検証が MAUI と違う名前で行われているため自己検証として機能しない (Major 1)、(b) `File.Exists` が副作用回避のガードではなく「破棄する側」を無検証で確定する短絡のままである (Major 2)。この 2 点により、「誤分類は破棄しない側にだけ倒れる」という deviation の主張は現状**成立していない**。名前を揃え、短絡を安全側にしか倒さない形にすれば、主張どおりの性質になる。

**(iii) 「実ファイルは存在するが復号不能」の残余ケース**

「解決自体が失敗するため実害なし」という評価は誤り。10.0.70 の `FileImageSourceService` は復号失敗を例外にせず、拡張子を落とした名前でのキャッシュ引きへフォールバックする。したがって「解決は成功し、返るのはキャッシュ所有の共有画像、しかし分類は短絡で facade 所有」という組み合わせが原理的に起こる。発生確率は低い (bundle 内に復号不能な同名ファイルがあり、かつ拡張子違いの同名 asset がある場合) が、未実測のまま「実害なし」とせず、Major 2 の修正に合わせて安全側へ倒すか、残余リスクとして deviation に明記するのが妥当。

## アクションプラン

1. **[Major]** キャッシュ引き直しの名前を MAUI の解決 (`Path.GetFileNameWithoutExtension`) に合わせる。`FileImageOwnershipTests` の期待値も拡張子・ディレクトリ付き入力で固定し直す
2. **[Major]** 実ファイル短絡が「破棄する側」を無検証で決める形を改める (安全側にしか倒さない形にするか、食い違いの残余を deviation に明記する)。復号不能ケースの評価も同時に更新する
3. **[Major]** 分類結果そのものを Simulator で asset 種別ごとに実測し `evidence/` へ追記する (asset catalog の拡張子付き指定を必ず含める)
4. **[Minor]** doc コメントの「新たな読み込みを起こさない」を実態に合わせる
5. **[Minor]** 即時解放の前提 (共有され得るのはキャッシュ所有の画像だけ) をコメントに明記する
6. **[Suggestion]** deviation に tasks 2.1 / 3.2 の読み替えを 1 行足す / `CleanupFor` 例外時の取りこぼし

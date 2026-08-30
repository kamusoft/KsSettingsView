# レビュー結果: add-maui-native-bridge (003 回目)

**日付**: 2026-08-05
**判定**: APPROVED (当初 CHANGES_REQUESTED → 残指摘の解消を確認して更新)

## サマリー

review-002 の 4 件 (Minor 2 / Suggestion 2) は**すべて解消を自分の手で確認した**。Minor-1 の除外実効は `-getItem:_XcbInputs` の probe を再実行して直接確認し、Suggestion-1 の絞り込みも同じ probe と正負のコントロール (SwiftUI touch → `_BuildXcodeProjects` が省略 / Bridge touch → 実行) で裏を取った。テストは iOS 587 件 0 failures、`dotnet build maui/KsSettingsView.slnx` は 0 警告 0 エラーで回帰なし。

当初は、修正作業の副産物として空ファイル `ios/Sources/KsSettingsViewCore/Theme.swift` (0 バイト) が作業ツリーに残っていたため CHANGES_REQUESTED とした。その後オーケストレーターが当該ファイルを削除し、**削除状態をレビュー側でも確認したため判定を APPROVED へ更新した** (経緯は下記「指摘事項」に残す)。他に未解消の指摘はない。

### 実行した検証

| 対象 | コマンド / 手段 | 結果 |
|---|---|---|
| Minor-1 除外実効 | `dotnet build …Binding.iOS.csproj -t:_GetBuildXcodeProjectsInputs -getItem:_XcbInputs` (`ios/binding/build/` `DerivedData/` に probe ファイルを置いて実測) | **両 probe とも除外された** (review-002 時点は残存) |
| Suggestion-1 絞り込み | 同上 probe の内訳 | Bridge 9 / Core 13 / UI 57 / `Package.swift` 1 / pbxproj 1 / csproj 1。**`KsSettingsViewSwiftUI` は 0 件** |
| 増分ビルド (負) | `touch ios/Sources/KsSettingsViewSwiftUI/*.swift` → `dotnet build -v:n` | 「すべての出力ファイルが入力ファイルに対して最新なので、ターゲット "_BuildXcodeProjects" を省略します」 |
| 増分ビルド (正) | `touch ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift` → `dotnet build -v:n` | `_BuildXcodeProjects` が**実行**され stamp が更新された |
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<ios-simulator-udid>…'` | **587 tests / 0 failures** (Bridge 28 / Core 83 / SwiftUI 68 / UI 408) / TEST SUCCEEDED |
| MAUI | `DEVELOPER_DIR=… dotnet build maui/KsSettingsView.slnx` | **成功 / 0 警告 0 エラー** |
| Android 資産の不変性 | 上記ビルド中の Gradle 出力「82 actionable tasks: 82 up-to-date」/ `Transforms/Metadata.xml` の shasum が review-002 時点と一致 | Android 側は本サイクルで未変更。review-002 の 1940 tests / 0 failures が有効 |

probe 用に作成した `ios/binding/build/` `ios/binding/DerivedData/` は削除済み (残骸なしを確認)。

---

## review-002 指摘への対応確認

### Minor-1 (`_XcbInputs` の除外が一致しない) — 解決

`KsSettingsView.Binding.iOS.csproj:81-86` が `$([MSBuild]::NormalizeDirectory(...))` / `$([MSBuild]::NormalizePath(...))` を使う形になり、`Remove` / `Include` とも SDK 側 item と同じ正規化済み絶対パス表記に揃った。review-002 と同じ probe を再実行した結果:

```
### -t:_GetBuildXcodeProjectsInputs
   1 ios/Package.swift
   9 ios/Sources/KsSettingsViewBridge
  13 ios/Sources/KsSettingsViewCore
  57 ios/Sources/KsSettingsViewUI
   1 ios/binding/KsSettingsViewBridge.xcodeproj
   1 maui/macios/KsSettingsView.Binding.iOS   (= csproj 自身)
```

`ios/binding/build/probe/probe.swift` と `ios/binding/DerivedData/probe/probe.h` は**どちらも現れない**。review-002 時点では両方とも残っていたので、除外が実際に効くようになったことが直接確認できた。csproj:75-76 のコメントも「SDK 側の item は正規化済みの絶対パスなので、Remove / Include とも NormalizeDirectory で表記を揃える (`..` を含んだままでは Remove が一致しない)」と、実測どおりの理由を書いている。

### Minor-2 (`build-xcframework.sh` の署名コメント) — 解決

`ios/binding/build-xcframework.sh:12-13` が「静的ライブラリなので署名は不要。archive では署名関連の設定を無効化して、署名環境の有無に結果が左右されないようにしている。」となり、反証済みの因果 (署名を無効化しないと framework が install されない) が消えた。フラグを渡す事実と、その理由 (環境非依存性) だけの記述で、`maui/README.md`「SDK 標準アイテムの採否」の説明とも矛盾しない。

### Suggestion-1 (増分入力の絞り込み) — 解決

`Include` が Bridge / Core / UI の 3 ディレクトリ + `Package.swift` になり、Xcode target の実際の構成 (同期グループ = `Sources/KsSettingsViewBridge`、package product = Core / UI) と一致した。`KsSettingsViewSwiftUI` が入力から外れたことは probe で 0 件を確認済み。csproj:70-71 のコメントに「追加するのは Xcode target が実際にビルドする 3 つだけで、target の依存 (同期グループと package product) を増やしたらここも足す」と追随条件が書かれており、絞り込みの保守方法が読める。

挙動側も自分で正負のコントロールを取った。SwiftUI を touch しても `_BuildXcodeProjects` は省略され、Bridge を touch すると実行される。取りこぼしと過剰の両方向で意図どおり。

### Suggestion-2 (空ディレクトリ) — 解決

`ios/binding/` の直下は `KsSettingsViewBridge.xcodeproj` と `build-xcframework.sh` のみになり、参照されていなかった `ios/binding/KsSettingsViewBridge/` は削除された。

---

## 指摘事項

### [🟡 Minor → 解消済み] 修正作業の副産物として空ファイル `ios/Sources/KsSettingsViewCore/Theme.swift` が残っている

**該当箇所**: `ios/Sources/KsSettingsViewCore/Theme.swift` (0 バイト、未追跡)

**問題点**:

review-002 の時点では存在しなかった 0 バイトのファイルが `KsSettingsViewCore` 配下に増えている。`git ls-tree HEAD` にも index にも無い純粋な新規ファイルで、未追跡のまま commit 候補に入っている。

`Theme` の実体は `ios/Sources/KsSettingsViewUI/Theme.swift` にあり、Core 側にこの名前のファイルが要る理由はない。増分ビルドの検証で Core 側を touch しようとしてパスを取り違え、`touch` が空ファイルを新規作成したものと見られる (probe の Core 件数が HEAD の 12 件ではなく **13 件**になっているのは、この空ファイルが native ビルドの入力に数えられているため)。

実害は次の 3 点:

- ライブラリの公開モジュール配下に、意味のない空ファイルが混入したまま commit される
- `Theme` を Core で探した読み手 (人間・エージェント) が空ファイルに当たり、定義が消えたと誤解する
- 中身が空で Swift のコンパイルも通るため、テストもビルドも素通りする (実際 587 件すべて green)

あわせて、この経緯からは実装者が報告した「Core touch で再ビルドあり」の実測が、既存ファイルの更新ではなく**新規ファイルの作成**によるものだったことになる。結論 (Core の変更で native が再ビルドされる) 自体は本レビューで別途 Bridge touch の正のコントロールを取って確認済みなので、判断は変わらない。

**推奨修正**: `ios/Sources/KsSettingsViewCore/Theme.swift` を削除する。空ファイルで参照もないため、削除後の再ビルド・再テストは不要 (念のため確認するなら iOS の全件実行 1 回で足りる)。

**解消の確認 (2026-08-05)**: オーケストレーターが 0 バイトであることを確認のうえ `trash` で削除。レビュー側でも次を確認した。

- `ios/Sources/KsSettingsViewCore/Theme.swift` は存在しない (`No such file or directory`)
- Core 配下は 12 ファイルで、`git ls-tree --name-only HEAD ios/Sources/KsSettingsViewCore/` と `diff` で完全一致
- `git status --porcelain ios/Sources/KsSettingsViewCore/` は出力なし (クリーン)

削除対象は 0 バイトかつ参照なしのため、他ファイルへの影響はなく再ビルド・再テストは行っていない。これにより本サイクルの残指摘は 0 件となり、判定を APPROVED へ更新した。

---

## 確認したが問題なしと判断した観点

- **回帰なし**: iOS 587 件 (Bridge 28 / Core 83 / SwiftUI 68 / UI 408) が 0 failures。`maui/KsSettingsView.slnx` は binding 2 本 + 検証ホスト 2 本すべて 0 警告 0 エラー
- **Android への波及なし**: 本サイクルで変更されたのは iOS binding の csproj・`build-xcframework.sh`・`ios/binding/` の空ディレクトリ・上記空ファイルのみ。slnx ビルド中の Gradle が「82 actionable tasks: 82 up-to-date」で、Android ソースが未変更であることを裏づけている。`Transforms/Metadata.xml` の shasum も review-002 時点と一致
- **`NormalizeDirectory` 化の副作用なし**: `Include` の対象が 3 ディレクトリに絞られた後も `ios/binding/KsSettingsViewBridge.xcodeproj` (pbxproj) と csproj 自身は入力に残っており、SDK 既定の検出範囲を壊していない
- **足場の凍結**: `proposal.md` / `design.md` / `specs/` は引き続き未変更。`tasks.md` はチェックボックスのみ
- **review-001 / review-002 の既解決分**: replace 系の戻り値 ID 契約、Metadata.xml の `remove-node`、Android binding 方式の deviation 記録は本サイクルで触られておらず、再確認の必要なし

---

## アクションプラン

残作業なし。唯一の指摘だった空ファイルの削除は完了・確認済みで、review-001 / review-002 / review-003 の全指摘が解消している。

蒸留フェーズへの申し送り (指摘ではなく記録):

- `maui/` ドメインの concepts はまだ未作成 (`kasane/concepts/index.md` で「最初の書き込み時にディレクトリを掘る」扱い)。Bridge の公開 API 契約・ID の interop 規約・binding 方式の採否は利用者向けにも価値があるため、蒸留時に `concepts/maui/` を掘る候補になる
- `deviation.md` の Android 方式 (gradlew Exec) は、SDK 側が `AndroidGradleProject` の複数モジュール対応を入れた時点で見直せる。`maui/README.md` の「SDK 更新時に再検証する箇所」の表と対で維持すると再検証の入口が揃う
- phase-2 への持ち越しとして起票済みの `kasane/changes/release-host-without-bridge-dispose/` (Host 単独解放) は、Handler の DisconnectHandler 設計に入る前に決着させる必要がある

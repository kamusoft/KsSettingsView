# レビュー結果: investigate-maui-icon-lease-sharing (002 回目)

**日付**: 2026-08-25
**判定**: APPROVED

## サマリー

前サイクルで確定した 3 指摘 (後片付け口の無限成長 / `DisposeRetired` の例外安全 / exploration「未決の論点」の整理) はいずれも解消している。焦点だった「代表 1 件だけ保持し後着の口は実行せず落とす」設計は、デルタスペックの文言に適合し、前提 (iOS の後片付け口は全て同一 `UIImage` の破棄で等価・解決結果オブジェクトは固有資源を持たない) も MAUI 実ソースで裏取りできたため**許容**と判定する。ミューテーション probe 4 本で、変更部分のテストが実際に回帰を捕まえることも実測した。

残るのは Minor 2 件 (`DisposeRetired` の doc コメントが実際より広い保証を述べている / proposal 記述との差が deviation として未記録) と Suggestion 3 件で、いずれも優先度は低い。

指摘は Critical 0 件 / Major 0 件 / Minor 2 件 / Suggestion 3 件。

## ビルド・テスト実行結果

| 対象 | 結果 |
|---|---|
| `maui/KsSettingsView.Maui.Tests` (net10.0) | **464 件成功 / 0 失敗 / 0 スキップ** |
| `maui/KsSettingsView.Maui` net10.0-ios (`--no-incremental`) | **0 エラー / 0 警告** |
| `maui/KsSettingsView.Maui` net10.0-android | **0 エラー / 0 警告** |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 (検査対象 677 ファイル) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 検出なし |

パッケージが客観的事実として提示した数値をレビュー側で再実行し一致を確認した。iOS 側は差分が確実にコンパイルされていることを確かめるため `--no-incremental` で取り直した。

## 判定を求められた論点への回答

### (i) 「代表 1 件だけ保持し、後着の口は実行せず落とす」設計は許容か → **許容**

**spec 文言との関係**: Requirement 本文は「その画像について**保持されていた**全ての後片付け口が各 1 回だけ実行され」と書いており、保持する集合の大きさは規定していない。保持が 1 件なら「保持されていた全て」= その 1 件で、各 1 回だけ実行されている。Scenario「最後のリースの破棄で後片付けが実行される」も同文であり、文言違反はない。review-001 が推奨修正 1 として提示した形そのものでもある。

**前提の妥当性 (実ソースで裏取り済み)**: dotnet/maui の実ソースを直接検めた。

- `src/Core/src/ImageSources/ImageSourceServiceResult.cs` — `ImageSourceServiceResult` が持つ状態は `Value` (画像) と `Action? _dispose` と `IsDisposed` フラグだけで、`Dispose()` は `_dispose?.Invoke()` を呼ぶのみ。**固有の資源を持たない**という doc コメントの記述は正しく、落とした結果オブジェクトが実行されないまま GC 対象になっても取りこぼしは生じない
- iOS の 4 サービス (`FileImageSourceService.iOS.cs:36` / `UriImageSourceService.iOS.cs:35` / `StreamImageSourceService.iOS.cs:31` / `FontImageSourceService.iOS.cs:32`) が渡す `Action` はいずれも `() => image.Dispose()` のみ。**同一画像への後片付けは互いに等価**という前提も正しい

前提は `KsSharedImageRegistry` の `<remarks>` に「この表へ通してよい解決結果の条件」として明記されており、前提が崩れる platform を通してはいけないことも書かれている。**落とした口を保持し続けない**点 (`Acquire` は `entry.Cleanup ??= disposer` で参照すら残さない) も確認した — 落ちた結果は即座に到達不能になるため、review-001 が問題にした蓄積は構造的に発生しない。

**残る前提リスク**: 利用側アプリが独自の `IImageSourceService` を登録し、その結果が (a) 同一の `UIImage` を返し、かつ (b) 破棄 `Action` に画像破棄以外の固有資源解放を含む場合のみ前提が崩れる。MAUI 標準サービスには該当がなく、doc コメントで条件が明示されているため対処は求めない (蒸留時に concepts へ残す価値のある前提だと考える)。

### (ii) 初回指摘の解消状況 → **3 件とも解消**

| 前サイクルの確定指摘 | 状態 | 根拠 |
|---|---|---|
| 1. `DisposeRetired` の例外で後続の退役リースが取りこぼされる (Major) | **解消** | `KsSettingsController.cs:1646-1668` が 1 件ごとに `try` で保護し、失敗を集めて最後に `AggregateException` で投げる。`IconSharingTests.FailingCleanupDoesNotStrandTheRemainingRetiredLeases` が 3 画像・先頭 2 件失敗で残り 1 件の破棄と `TrackedImageCount == 0` を固定。ミューテーション probe M3 で検出力も実測 (後述) |
| 2. 後片付け口が再解決のたびに無限成長する (Major) | **解消** | `KsSharedImageRegistry.cs:93` が `entry.Cleanup ??= disposer` の 1 スロットになり、`Entry` にコレクションが存在しない。保持は画像 1 つにつき高々 1 件で、構造上増えようがない |
| 3. exploration「未決の論点」3・4 が完了済みの状態と矛盾 (Minor) | **解消** | `exploration.md` の 3・4 が取り消し線 + 決着内容 (採用案・probe 結果・テスト足場の形) へ更新済み |

review-001 の Suggestion 2 件 (`Release` の防御分岐コメント / evidence の環境注記) も反映されている (`KsSharedImageRegistry.cs:111`、`evidence/ios-wiring-before-after.txt` 冒頭 3 行)。

### (iii) テストの意味変更・削除が検出力を落としていないか → **主要な検出力は維持。ただし 2 本は構造上通るだけのテストになっている**

lessons code-review L-001 に従い、静的読解ではなくミューテーションで実測した (全て計測後に原状復帰し、`shasum` 一致と `git status` で確認済み)。

| # | ミューテーション | 結果 |
|---|---|---|
| M1 | `Acquire` の `entry.Cleanup ??= disposer` を「後着で上書き」へ変更 | **7 件失敗** (`CleanupIsWithheldWhileAnyLeaseRemains` / `LaterDisposersAreDroppedWithoutRunning` / `ReleasingSameHandleTwiceIsIdempotent` / `RetainedCleanupDoesNotGrowWithRepeatedReacquisition` ほか) |
| M2 | 実行はせず**保持だけ**を積み上げる (`Entry` に `List<IDisposable>` を足して全 disposer を追加) | **0 件失敗 (464 件全通過)** |
| M3 | `DisposeRetired` の 1 件ごとの `try` を外し無保護ループへ戻す | **1 件失敗** (`FailingCleanupDoesNotStrandTheRemainingRetiredLeases`) |
| M4 | `StoreIcon` の同一画像分岐を退役キュー投入へ戻す | **4 件失敗** (`ReresolvingSameCellToSameImage` 2 本 / `SharingAcrossResolverGenerationsKeepsIconAlive` / `RepeatedReconnectDoesNotGrowRetainedCleanup`) |

読み取り:

- **削除された 1 本 (全 disposer が各 1 回実行されることの固定) の穴は埋まっている。** 「どの口が実行され、どの口が実行されないか」は M1 が 7 件で捕まえており、`LaterDisposersAreDroppedWithoutRunning` が契約を正面から固定している
- **意味変更した 3 本も検出力を持つ** (M1 / M4 で失敗する)
- **M2 が通ってしまう点は把握しておくべき限界** — Suggestion として後述する

## 指摘事項

### [🟡 Minor] `DisposeRetired` の doc コメントが、実際には保証していない範囲まで述べている

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1629-1630` (および `:737-751` の `DisposeRetiredViews`)

**問題点**:

`DisposeRetired` の `<remarks>` は「待ち行列を空にした後は取り出したリースが唯一の持ち主になるため、1 件の後片付けが失敗しても残りを取りこぼさないよう、**全件の破棄を試みてから**失敗をまとめて投げる」と書いている。しかしこの保証が成立するのは `_retiredIcons` のループだけで、同じメソッドが最初に呼ぶ `DisposeRetiredViews()` (`:737-751`) は**待ち行列 `_retiredViews` を先に空にしたうえで無保護の `foreach`** のままである。1 件目の `entry.Lease.Dispose()` が投げると残りの `RetiredView` は二度と破棄されない — 前サイクルで確定した指摘 1 とまったく同じ形の欠陥が、View 側にそのまま残っている。

`DisposeRetired` は失敗を `AggregateException` に集約するようになったので、外側では 1 件の例外として観測できるが、取りこぼし自体は防げていない。View リースの破棄は Handler の切断を伴い、取りこぼすと「作り直した実体が切断済み Handler を抱える」(`DisposeRetiredViewsOf` の `<remarks>` が説明している状況) を招き得る。

この欠陥自体は本変更以前から在り、画像リースと違って共有表を経由しないため本変更が発火面を広げたわけではない。指摘するのは、(a) 本変更が加えたコメントがメソッド全体の保証として読めてしまうこと、(b) 修正が数行で閉じ、本変更が既に触っているメソッドの中にあること (lessons process L-005) の 2 点による。

**推奨修正**: `DisposeRetiredViews` のループも 1 件ごとに保護して失敗を呼び出し元へ集約する (3〜5 行)。それを採らないなら、`DisposeRetired` のコメントを「icon の待ち行列については」と範囲を限る文へ改める。前者なら「View リースの 1 件が失敗しても残りが破棄される」ことを固定するテストを 1 本足したい。

### [🟡 Minor] proposal の「全てを各 1 回実行する」との差が deviation として記録されていない

**該当箇所**: `proposal.md` の What Changes 2 (「各解決結果の後片付け口は共有表が保持し、カウント 0 で全てを各 1 回実行する」) / `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:93`

**問題点**:

デルタスペックの文言には適合している (前掲 (i)) が、proposal は「各解決結果の後片付け口は共有表が**保持し**」「カウント 0 で**全て**を各 1 回実行する」と、保持する集合が解決結果の数だけあることを前提に書かれている。実装は代表 1 件しか保持せず、後着は実行もされない。`deviation.md` は存在せず、この差はどこにも記録がない。

proposal は足場アーティファクトなので書き換えてはいけない。一方で蒸留 (`ksn-distill`) は proposal と実装を読んで ADR / concepts を起こすため、記録が無いままだと「全ての後片付け口を実行する機構」という誤った知識が長命層へ流れる。今回は代表 1 件で足りる根拠 (iOS の後片付け口の等価性・結果オブジェクトが固有資源を持たないこと) が判断の核であり、これこそ残すべき知識である。

**推奨修正**: `deviation.md` に 1 項起こし、「proposal の『全てを各 1 回実行』に対し、実装は画像 1 つにつき代表 1 件のみ保持・実行する。根拠は iOS の 4 画像解決サービスが渡す破棄 `Action` が全て `image.Dispose()` であり、`ImageSourceServiceResult` が固有資源を持たないこと」と、レビューで確認したソース位置を添えて記録する。実装の修正は不要。

### [🔵 Suggestion] 「保持が増えない」を謳う 2 本のテストは、保持のみの再蓄積を検出できない

**該当箇所**: `maui/KsSettingsView.Maui.Tests/SharedImageRegistryTests.cs:79` (`RetainedCleanupDoesNotGrowWithRepeatedReacquisition`) / `maui/KsSettingsView.Maui.Tests/IconSharingTests.cs:333` (`RepeatedReconnectDoesNotGrowRetainedCleanup`) / 観測点は `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:52-67`

**問題点**: `RetainedCleanupCount` は「`Cleanup` が非 null のエントリ数」を数えており、定義上 `TrackedImageCount` を超えられない。したがって「50 回再解決しても `RetainedCleanupCount == 1`」というアサーションは、口を溜め込む実装へ戻しても (溜めた口を実行しない限り) 通る。実測 (M2): `Entry` に `List<IDisposable>` を足して全 disposer を追加する変異で **464 件全通過**した。前サイクルの Major が指摘した「保持だけが増える」という失敗モードそのものに対して、この 2 本は検出力を持たない。

実際に守っているのは `LaterDisposersAreDroppedWithoutRunning` と `CleanupIsWithheldWhileAnyLeaseRemains` (後着の口が実行されないこと = M1 で 7 件失敗) と、`Entry` が単一スロットであるという構造そのものである。実害は無い (構造上、保持は増えない) ため修正は求めない。

**推奨修正**: 対処不要。ただしテスト名と doc コメントが「増えないことを検出する」と読めるので、`RetainedCleanupCount` の `<remarks>` に「画像 1 つにつき高々 1 件であることは `Entry` の形が保証しており、この値はその不変条件の観測点である」旨を足すか、テスト側の `<summary>` を「単一スロット設計が維持されていることの確認」と実状に合わせると、後から読む人が回帰ネットの範囲を取り違えない。

### [🔵 Suggestion] iOS 配線の A/B 証跡は前サイクルの実装で取得したもの

**該当箇所**: `evidence/ios-wiring-before-after.txt`

**問題点**: この証跡は「全ての口を実行する」実装の時点で計測されている。今回の変更で実行される口は代表 1 件になったが、観測している `UIImage.Handle` の推移 (片方除去で維持 / 最後の除去で `0x0`) は変わらない — 落とした口も実行された口も同じ `UIImage.Dispose()` であることが MAUI 実ソースで確認できるため (前掲 (i))。結論は有効だが、証跡の日付と実装の版が一致していないことは記録として見えていない。lessons process L-003 (3) の「承認済み照合の後に修正を入れたら証跡範囲を明記する」に照らした注記の提案。

**推奨修正**: 証跡の注記に「後片付け口の保持を代表 1 件へ絞る修正の前に計測。実行される口が 1 件になっても同一 `UIImage` の破棄であるため handle の推移は変わらない」を 1 行足す (再撮影までは求めない)。

### [🔵 Suggestion] `StoreIcon` の即時解放と `completed(lease)` は例外を素通しする

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1613` / `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:61`

**問題点**: `StoreIcon` が新しく行う `previous?.Dispose()` は保護されておらず、投げれば `CompleteIcon` → `completed(lease)` を経て `ResolveAsync` の `try` の外 (`KsImageResolver.cs:61`) へ抜け、`_ = ResolveAsync(...)` の未観測例外になる。前サイクルの Minor で触れられていた 2 経路のうち、`DisposeRetired` だけが対処された形。

ただし今回の変更で registry の後片付けは代表 1 件の `UIImage.Dispose()` だけになり (`AggregateException` を投げるのは `DisposeRetired` のみ)、`completed` の中から例外が抜ける経路自体は `CompleteIcon` の stale 破棄として本変更前から存在する。発火確率と本変更の寄与のどちらも小さいため、指摘の格を Suggestion に留める。

**推奨修正**: 対処不要。iOS 側で `completed(lease)` を `try` の内側へ入れる (あるいは別の `try` で包む) 判断をするなら、本変更ではなく resolver の例外方針として独立に扱うのが筋だと考える。

## 確認した観点 (指摘に至らなかったもの)

- **`Acquire` / `Release` の不変条件**: `Handle._released` による冪等化 (`:141-153`)、`Release` が `_entries.Remove` を後片付けより先に行うこと (`:122-123`)、カウント underflow が構成不能であること (エントリはカウント 0 で消えるため、`Count--` は必ず 1 以上から始まる) を確認。`FailingCleanupLeavesNoResidualTracking` が例外時のエントリ除去まで固定している
- **最初の口が null のケース**: `entry.Cleanup ??= disposer` により後から来た非 null が保持される (`FirstNonNullDisposerIsRetained`)。口を持たない預かりもカウントには参加する (`LeaseWithoutDisposerStillHoldsCleanupBack`)
- **`StoreIcon` の 4 組合せの等価性**: `previous`/`lease` の null 有無 × 画像同一性を新旧で突き合わせ、共有ケース以外 (退役キュー投入・`MarkContentDirty` の発火・両方 null での無配信) が変わっていないことを確認。`ReferenceEquals(null, null)` が true になる経路も、旧実装の `!ReferenceEquals(...)` 判定と同じ「配信しない」に落ちる
- **落とした結果オブジェクトの寿命**: `Acquire` は保持せず、`KsImageLease` は registry の `Handle` だけを持つため、落とした `ImageSourceServiceResult` は即座に到達不能になる。static 表に溜まる参照は増えない
- **Android 経路の不変性**: `Platforms/Android/KsImageResolver.cs` は無変更で共有表を通らない。`StoreIcon` の即時解放でも新リースが Glide 参照を保つため表示は壊れない
- **static のテスト汚染なし**: テストは `KsSharedImageRegistry.Shared` を使わず `GatewayScope.ImageRegistry` でインスタンスを注入している。`SharingAcrossSettingsViewsKeepsIconAlive` は 2 つの scope に同じ表を渡す形で SettingsView 間共有を再現している
- **足場アーティファクトの逆流なし**: `proposal.md` と `specs/maui-cells/spec.md` に差分なし。`tasks.md` は `[ ]`→`[x]` のみ。`exploration.md` の変更は tasks 1.1 / 1.2 / 4.1 が明示的に求めた記録と、確定済み論点への取り消し線 (削除ではなく打ち消しでの上書き)
- **コメント規約**: 新規コメントに変更 ID・タスク通番・delta spec キーワード・履歴記述の混入なし。`ImageSourceServiceResult` はコード識別子への参照であり許容範囲。lint も 0 件
- **リース放棄時の static 強参照** (second-opinion 指摘 4): 突き合わせで降格済みのため蒸し返さない。指摘 1・2 の修正後もこの構造は変わっていないことだけ確認した

## アクションプラン

1. **[Minor]** `DisposeRetiredViews` のループも 1 件ごとに保護する (数行)。採らない場合は `DisposeRetired` のコメントを icon 側に限定する文へ改める
2. **[Minor]** `deviation.md` に「代表 1 件保持」と proposal 記述の差を、根拠 (iOS 4 サービスの破棄 `Action` / `ImageSourceServiceResult` の構造) 付きで記録する
3. **[Suggestion]** `RetainedCleanupCount` の位置づけ (構造保証の観測点であって蓄積の検出網ではない) を doc / テスト `<summary>` に明記する / evidence に計測時点の注記を 1 行足す

---

## 修正確認 (2026-08-25、指摘者による再確認)

**判定は APPROVED のまま変更なし。** Minor 2 件はいずれも対処されている。新たに Minor 1 件 (付随修正のテスト未担保) と Suggestion 1 件を挙げるが、優先度はいずれも低く判定を動かさない。

再実行: net10.0 テスト **464 件成功 / 0 失敗**、`comment-policy-lint.py` 禁止 0 件 (677 ファイル)、`local-path-lint.py` / `identity-lint.py` 検出なし。

### Minor 1 (`DisposeRetiredViews` の無保護ループ) → **解消**

`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:741-770` が icon 側と同型の「全件試行 + 失敗収集 + `AggregateException`」になり、`<remarks>` も保証範囲と一致した。指摘した「1 件目の失敗で残りの `RetiredView` が二度と破棄されない」経路は塞がっている。コメント限定ではなく実装で塞ぐ選択に異論はない (推奨の第一案)。

なお `DisposeRetiredViewsOf` (`:782` 以降) は同型に見えるが、`_retiredViews.RemoveAt(i)` の後に破棄しており、途中で例外が抜けても**残りの要素は待ち行列に残る** (次の `DisposeRetiredViews` が拾う)。取りこぼしは起きないため、こちらへ手を入れる必要はない。

### Minor 2 (deviation 未記録) → **解消**

`deviation.md` が新規作成され、代表 1 件保持への変更が根拠 (iOS の後片付け口の等価性・`ImageSourceServiceResult` の構造・dotnet/maui 実ソースでの裏取り)、spec 適合判定、蒸留時の扱い (「proposal の当該記述ではなくこの記録を正とする」) まで含めて記録されている。求めた内容を満たしている。`[付随修正]` 行の形式も ksn-core の規定形どおり。

### [🟡 Minor] 付随修正 (`DisposeRetiredViews` の例外安全) がテストで担保されていない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:741-770` / `deviation.md` の `[付随修正]` 行

**問題点**: ミューテーション probe で実測した — 追加した `try` / 失敗収集 / `throw` を丸ごと外して無保護ループへ戻しても、**464 件が全て通過する**。この付随修正には回帰ネットが 1 本も掛かっていない。ksn-core の付随修正の同梱条件④は「既存テストの通過と、必要なら 1 件のテスト追加で担保できる」であり、レビューは同梱条件とテストの有無を見ることになっている。icon 側は `IconSharingTests.FailingCleanupDoesNotStrandTheRemainingRetiredLeases` が固定しているのに、View 側だけ素通しになっている。

外向きの挙動も変わっている点も踏まえたい — 後片付けが投げた場合に公開経路 (`Root` の差し替え等) から出る例外型が、生の例外から `AggregateException` に変わる。deviation.md に記録済みなので合意済み差分として扱うが、記録した以上はテストで固定しておきたい類のもの。

**推奨修正**: `FakeViewMaterializer` のリース破棄を失敗させて、複数の退役 View のうち先頭が投げても残りが破棄されることを固定するテストを 1 本足す (icon 側テストと同型)。難しければ、テストを持たないことを deviation.md の `[付随修正]` 行に明記して、後から読む人が「担保済み」と誤解しないようにする。

### [🔵 Suggestion] `DisposeRetired` の集約が入れ子の `AggregateException` になり得る

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1655-1662` / `:1686`

**問題点**: `DisposeRetiredViews()` が投げる `AggregateException` を `failures` にそのまま足すため、View 側と icon 側が同時に失敗すると `AggregateException(AggregateException(...), ...)` になる。取りこぼし防止という目的には影響せず、`Flatten()` を持つ利用側なら扱えるため、許容という判断に異論はない。

**推奨修正**: 対処不要。気になるなら `throw new AggregateException(failures)` を `throw new AggregateException(failures).Flatten()` にするだけで入れ子は消える (1 語)。テストの `InnerExceptions` 件数アサーションも現状のまま通る。

---

## 最終確認 (2026-08-25、指摘者による再確認)

**判定は APPROVED のまま変更なし。未解消の指摘は残っていない。** 修正確認で挙げた Minor 1 件と Suggestion 1 件はいずれも解消した。

再実行: net10.0 テスト **465 件成功 / 0 失敗**、net10.0-ios / net10.0-android とも `--no-incremental` で **0 エラー / 0 警告**、`comment-policy-lint.py` 禁止 0 件 (677 ファイル)、`local-path-lint.py` / `identity-lint.py` 検出なし。

### Minor (付随修正のテスト未担保) → **解消**

`maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:392` の `FailingContentDisposalDoesNotStrandTheRemainingRetiredViews` が、指摘した失敗モードを正面から固定していることを実測で確認した。

- **ミューテーション M6**: `DisposeRetiredViews` (`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:741-770`) の `try` / 失敗収集 / `throw` を丸ごと外して無保護ループへ戻す → **この 1 件だけが失敗** (他 464 件は通過)。前回「保護を外しても 465 件全通過」だった穴は塞がっている
- テストが観測しているものも妥当。`FakeViewLease.Dispose` は `DisposeCount++` → `Handler.Disconnect()` → `OnDispose?.Invoke()` の順であり、`thirdLease.DisposeCount == 1` は「先行 2 件が投げた後も 3 件目の破棄が試行され、Handler 切断まで到達した」ことを示す。取りこぼしの検出として過不足ない
- 破棄順に依存しないよう 2 件を失敗させる形にした点も、icon 側 `FailingCleanupDoesNotStrandTheRemainingRetiredLeases` と揃っていて良い。`Root` 差し替え経由で `DisposeRetired` → `DisposeRetiredViews` の実経路を通しており、icon リースが空の状態を選んでいるため View 側の集約だけを観測できている
- 新設の Fake は無い (`FakeViewLease.OnDispose` / `DisposeCount` / `LatestFor` は既存)。テスト足場の増築を伴わない追加である点も確認した

### Suggestion (入れ子 `AggregateException`) → **解消**

`KsSettingsController.cs:1686` が `throw new AggregateException(failures).Flatten()` になり、View 側の集約が入れ子のまま外へ届く形は解消した。

**icon 側アサーションの意味は変わっていない** — 実測で確認した。**ミューテーション M7**: `.Flatten()` だけを外す → **失敗するのは `FailingContentDisposalDoesNotStrandTheRemainingRetiredViews` の 1 件のみ**で、icon 側の `FailingCleanupDoesNotStrandTheRemainingRetiredLeases` は通過する。icon リースの破棄が投げるのは `KsSharedImageRegistry.Release` の `entry.Cleanup?.Dispose()` から上がる生の例外であり、元から入れ子ではないため `Flatten()` の影響を受けない。この非対称 (View 側だけが Flatten に依存する) は、`DisposeRetiredViews` が自分で集約してから投げる構造の当然の帰結であり、問題ではない。

補足: `Flatten()` は「1 件だけ失敗した場合も `AggregateException` で包む」挙動を変えないため、外向きの例外型の一貫性 (deviation.md に記録済み) も維持されている。

### 実測の原状復帰

M6 / M7 とも計測後に復帰し、`shasum` 一致 (`266c6668…`) と `git status` で working tree が確認依頼時点と同一であることを確かめた。

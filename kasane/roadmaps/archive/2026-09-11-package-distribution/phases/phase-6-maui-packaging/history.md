# phase-6-maui-packaging 議論履歴

## 2026-09-02: 名前空間改名の範囲

実物の出現箇所を数えた (facade namespace 宣言 74、テスト 45、MauiHost は using と xmlns のみ、IntegrationHost 0、Sample 13 ファイル)。選択肢は A: テストの自前名前空間 (`KsSettingsView.Maui.Tests`) も `KsSettingsView.Tests` に揃える / B: 公開面 (facade + 利用側の using / xmlns) だけ改名。A を採用。理由: ADR-0025 の非対称 (アセンブリ名は `.Maui` を保ち名前空間からは落とす) をテストにも同じ規則で当てられ、「名前空間に `.Maui` は出ない」の 1 ルールで説明でき、機械置換のみでアセンブリ名・InternalsVisibleTo は無傷のため。改名は独立 change に切らず、phase-5 の前例どおり同一 change の最初のタスク (独立コミット) とする。着手前提の確認: maui-support 系の進行中 change は全件 exploration.md のみのスタブで、worktree も無く、改名との同時進行の衝突は無い。

## 2026-09-02: 共通メタデータと版集約の中身 (nuget.org 固有要件の確認を含む)

前提調査: KsDialogs には props の前例なし (csproj 直書き、binding は IsPackable=false)。リポジトリ内にアイコン画像なし、`maui/README.md` は phase-9 で消滅、ルート README はスクリーンショットを相対パス参照。SDK 既定は全プロジェクト IsPackable=true。nuget.org の必須要件はライセンスのみ。

README の扱いは A: facade 専用の短い package README を同梱 / B: ルート README をそのまま同梱 (画像が壊れる) / C: 同梱しない (一覧に readme なし表示) から A を採用。アイコンは当初「素材なし」で見送る案だったが、オーナーが原典 AiForms.Maui.SettingsView の icon.png (300×300 PNG、nuget.org 制約内) を指定したため `assets/icon.png` にコピーして同梱する。

名乗りは、オーナーの「Company は kamusoft LLC の方がよいか」「LLC は LICENSE 含め一般的に付けるものか」の問いに対し、著作権表示は法的意味を持つため法人主導 OSS では正式名が多く、レジストリの表示名 (Authors / developers) はブランド名が慣習、Company は表示に出ない属性と整理。A: Authors `kamusoft` / Company `kamusoft LLC` / Copyright は LICENSE と同じ `kamusoft` / B: すべて `kamusoft` / C: LICENSE・POM 含め LLC 全面採用 (phase-6 外に波及) から A を採用し、LICENSE の正式名化は別作業の TODO とした。

その他: Version 既定値 `0.0.0-dev` (ADR-0020)、IsPackable 既定 false、SDK 同梱 SourceLink + snupkg、CPM で Maui.Controls・AndroidX 14 本・テスト系 3 本を集約、binding の版整合コメントを Packages.props 側へ移す。

## 2026-09-02: binding の pack 設定 (IsPackable / PackageId / Description)

PackageId は未指定ならアセンブリ名になり ADR-0025 の ID と一致するため binding は既定に任せ、facade のみ明示。IsPackable は 3 プロジェクトで true を明示 (props 既定 false)。Description の言語は 英語 + AiForms 言及 / 英語のみ / 日本語 の 3 案から、nuget.org の読者と英語正典 README との整合、AiForms 利用者の発見性を理由に「英語 + AiForms 言及」を採用。PackageTags は facade のみ。

## 2026-09-02: pack PoC

ksn-scout に委譲して binding 2 件 + facade を SDK 標準の `dotnet pack` で実測 (結果は artifacts/pack-poc.md)。3 パッケージとも成功し、ADR-0025 の前提 (標準経路のみ・TFM 別依存の自動変換・3 パッケージの分離・aar 2 本と両スライスの同梱) を裏付けた。判断点 3 つ (iOS manifest の絶対パス / binding 依存版の下限指定 / API 版付き TFM) は、いずれも自作 MSBuild で SDK 内部構造に依存しないと変えられず、受け入れても利用者に実害が無い (CI ランナーのパス・lockstep 同時発行・同一 SDK なら解決可) ことから SDK 挙動のまま受け入れることにした。(c) は phase-7 と README 互換情報へ申し送り。補足: nuspec メタデータ空は props で解消、iOS binding の xml doc 無しは揃えない、Gradle 10 deprecation は phase-6 外。

## 2026-09-02: AndroidX Lifecycle 競合の NuGet 経路実証と Release 消費者ビルド

ksn-scout に消費者 PoC (素の MAUI アプリ + ローカルフィードの facade 1 行) を委譲。NU1608 / NU1107 は 0 件で LiveData family が 2.11.0.1 に解決 (ADR-0010 の未検証項目を解消)。Release ビルドは Android / iOS (simulator) とも警告ゼロで成功、trimming 後も facade / binding が成果物に残る。両論点を検証済みとして決定事項へ。副産物として、テンプレート既定 (Maui.Controls 10.0.20、Android 21 / iOS 15) のままでは NU1605 と minSdk エラーで失敗することが判明し、論点 10 (MAUI 最低版) と論点 11 (最低 OS 版の伝え方) を新設した。

## 2026-09-02: facade が要求する MAUI 本体の最低版

当初 A: SDK 既定の 10.0.20 に下げる / B: 10.0.70 のまま README で案内 / C: SDK 既定に追随させる運用 を提示し A を推奨したが、オーナーの「iOS 側は大丈夫か」の問いを受けて実測 (リポジトリのコピーを 10.0.20 に書き換え): ビルド・テスト 516 件・利用側 iOS リンクは成功したが、MAUI の `ImageSourceExtensions.GetPlatformImage` が 10.0.30 以前と 10.0.60 以降で照合キーの形が異なり、ADR-0026 の icon 所有権分類の前提が 10.0.20 では崩れると判明 (GitHub の tag 実ソースと IL 逆アセンブルの 2 系統で一致。10.0.40 / 10.0.50 は nuget.org からの取得が許可されず未確認)。改訂案として A': 下限 10.0.60 (検証済み最低版) / B: 照合キーを両形式対応にして 10.0.20 まで下げる / C: 10.0.70 のまま を提示し A' を推奨したが、オーナーの「それなら 10.0.70 のままでよい」の判断で C を採用。理由: 10.0.60 との差は利用者体験上同じで、10.0.70 は ADR-0026 の probe を実際に通した版であり再検証が不要。README に MauiVersion 10.0.70 以上を明記する。

## 2026-09-02: 最低 OS 版の利用者への伝え方

消費者 PoC の追加確認で iOS は 15.0 のまま黙って通り、Android の 21 失敗も AndroidX の minSdk 23 の副作用と判明。A: README / skills 明記のみ / B: facade 同梱の buildTransitive .targets によるビルド時ガード + README から B を採用。理由: iOS 15 端末で本体 (UIHostingConfiguration、iOS 16 以上) が実行時に落ちる形の出荷をドキュメントでは防げず、ガードは数行で済み、Android でも manifest merger の読みにくいエラーより先に要件を書いた文面を出せるため。ADR-0025 の「自作 pack MSBuild を足さない」は pack 内部構造依存の回避が趣旨で、利用者ビルド資産の同梱は対象外 (蒸留時に Consequences へ追記)。

## 2026-09-02: slnx の Sample 同居

A: 含めたまま / B: 外して samples/ 側に別 slnx から A を採用。CI・pack とも csproj 単位で slnx は無関係、Sample は maui/ の外で props の影響を受けない、改名の追随を 1 ソリューションで追えるため。

## 2026-09-02: docs 追随の仕分け

phase-5 の 3 段仕分けを踏襲。元論点の「maui README」は phase-9 で消滅済みのため対象外とし concepts に引き受けさせる。change 同梱: handbook public-identifiers (NuGet ID 行と非対称) と cross/ADR-0018 配布先表。蒸留: concepts maui-facade / binding-build-integration / native-bridge、ADR-0010・0025 の Consequences 追記と accepted 昇格。docs-refresh: skills maui + aiforms-migration、README 群 + package README。改名直後に README / skills の XAML 例が食い違うため、change 完了直後の docs-refresh 依頼を TODO に明記。全 11 論点が解消しフェーズ議論を終了。

## 2026-09-02: package README の撤回 (ksn-propose の上位層照合)

提案作成時の照合で、論点 2 の「facade 専用の package README を新設し docs-refresh 対象に追加」が cross/ADR-0023 (README はルート 2 + skills 索引 2 の 4 枚のみ、platform 別 README の新設は ADR 改訂を要する、docs-refresh 対象の変更は変更フロー承認) と衝突すると判明。A: ルート README をそのまま同梱し画像参照を public リポジトリの絶対 URL に改める / B: 新設して ADR-0023 を改訂 / C: 同梱しない から A を採用。理由: README を新設せず ADR-0023 と docs-refresh 対象に触れない、nuget.org と GitHub で同じ 1 枚が表示できる。README の画像参照の書き換えは change に同梱する。

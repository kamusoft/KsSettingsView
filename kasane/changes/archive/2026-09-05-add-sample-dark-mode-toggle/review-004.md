# レビュー結果: add-sample-dark-mode-toggle (004 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

サイクル 3 の指摘 (review-003 の Minor 2 / second-opinion-code-003 の Major 1・Minor 2) への対応だけを確認範囲とした差分レビュー。全 7 項目とも対応が成立しており、Critical / Major は無い。コード変更は `samples/maui/KsSettingsView.Sample.Maui/SampleTheme.cs` の doc コメント 1 行のみで、review-003 以降に他のソースは更新されていない (mtime で確認)。

追加された Android Native の証跡 2 枚は記録どおりの内容 (「ダーク」の選択印 + ダーク外観 / 「システム」の選択印 + ダーク外観) で、個人要素・シリアル実値の写り込みは無い。オーナー裁定 (承認モックの規範範囲の限定) は deviation.md・brief.md 2 箇所・second-opinion-code-003 の突き合わせ結果で同じ内容に揃っている。

新規の指摘は Minor 2 件 (いずれも低優先度) と Suggestion 1 件で、判定を妨げない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | **常時** (rule)。今サイクルで改訂された doc コメント |
| `kasane/handbook/cross/test-execution.md` | ビルド・テストの実行と結果報告 (MAUI) |
| ksn-core `references/ui-artifacts.md` | `ui/verification/` と `evidence/` の置き場、撮影・保存時の個人情報規律 |
| ksn-core `references/evidence.md` | 証跡のプレースホルダ語彙 (`<android-serial>`) |
| ksn-core `references/paths.md` | 成果物に書くパスの形式 |
| `kasane/lessons/code-review.md` (L-001) | レビュー観点 (「指摘しないこと」は昇格済みルールなし) |

範囲限定のため、review-003 で照合済みの `cross/sample-parity.md` / `ios/swift6-language-mode-check.md` / `cross/ADR-0016` / `android/ADR-0020` はコードが変わっていない範囲として再照合していない (前サイクルの照合が有効)。

## 確認対象の判定

| # | 確認対象 | 判定 | 根拠 |
|---|---|---|---|
| 1 | `SampleTheme.cs` の「従来どおり」を現在形へ | **成立** | `samples/maui/KsSettingsView.Sample.Maui/SampleTheme.cs:146` は `/// light 側は未指定のまま残す。`。追跡対象のソースに「従来どおり」の新規混入は無い (残る検出は本 change が触っていない既存コメントと gitignore 済み `bin/` 配下の生成 XML のみ) |
| 2 | 修正前画像を `evidence/` へ移動し brief の参照を更新 | **成立** | `evidence/maui-ios-calendar-dark-range-before.png` が実在し、`ui/verification/` から消えている。`ui/brief.md:71` が change 相対で `evidence/...` を指し、「verification/ には最終画像だけを置く」と理由も添えている。画像を開いて内容 (ライトのままのシート地色) と個人要素なしを確認した |
| 3 | 後続 change スタブの MAUI 記述 | **成立** | `kasane/changes/fix-default-colors-dark-appearance/exploration.md:12` が「MAUI iOS / MAUI Android の両実行面で同じ症状を確認済み」+ 証跡 2 枚と brief.md の照合結果への参照になっている。「未確認」の記述は残っていない (下記 Minor 2 は同ファイルの別行) |
| 4 | Android Native の証跡 2 枚と brief への記録 | **成立** | `ui/verification/android-menu-dark-relaunch.png` は「ダーク」にチェック + chrome / セルともダーク描画、`android-menu-system-device-dark.png` は「システム」にチェック + ダーク描画で、`ui/brief.md:40` の記録と一致する。項目群 (外観 = システム / ライト / ダーク) と後続「デモ」群の構成も Android Native 面のもの。ステータスバーは時刻・電波・電池のみで端末名・アカウント・通知の写り込みなし。本文もシリアル実値ではなく `<android-serial>` を使っている |
| 5 | オーナー裁定の記録の整合 | **成立** | deviation.md 3 項目目 / `ui/brief.md:107` (合意済み妥協) / `ui/brief.md:115` (モックとの既知の差分) / second-opinion-code-003 の突き合わせ結果が、いずれも「規範範囲 = 配色 (色ロール対応表) とルートメニューの外観 UI」「行構成・文言 (「無効なボタン」行・ButtonCell「登録」→ 現行 3 面は行なし・「ログアウト」) は合意済み差分」で一致。`ui/brief.md:110` の「未合意の乖離: なし」とも矛盾しない。deviation が指す `kasane/lessons/inbox/mock-shows-param-not-matching-current-impl.md` も当該 change を count 3 の evidence として持つ |
| 6 | `ui/verification/` と brief.md の双方向突き合わせ | **一部不成立** | brief.md が参照する画像はすべて実在する (`-1.png` / `-2.png` の省略記法も含めて解決できる)。逆方向で 7 枚が brief.md のどの行からも名前で参照されていない (下記 Minor 1)。中間ラウンドの画像は混じっていない |
| 7 | 標準 lint 3 本 | **成立** | `local-path-lint.py` 0 件 / `identity-lint.py` 0 件 / `comment-policy-lint.py` 0 件 (検査対象 771 ファイル)。本レビューで再実行した |

## 実行した客観確認

| 検査 | 結果 |
|---|---|
| MAUI Sample `net10.0-ios` (`dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj -f net10.0-ios`) | **ビルド成功 / 警告 0 / エラー 0** |
| MAUI Sample `net10.0-android` | **ビルド成功 / 警告 0 / エラー 0** |
| MAUI facade テスト (`dotnet test maui/KsSettingsView.Maui.Tests/...`) | **成功 / 516 tests / 0 failures** |
| 標準 lint 3 本 | いずれも 0 件 |
| review-003 以降のソース更新 | `samples/maui/.../SampleTheme.cs` のみ (doc コメント 1 行)。`ios/Sources` / `ios/Tests` / `samples/android` は review-003 の実行時点から未更新のため、iOS 全件テスト・Android / iOS Sample ビルドは review-003 の結果を有効とみなし再実行していない |
| 足場の凍結 | `proposal.md` (10:26) / `specs/*/spec.md` (10:22–10:26) / `exploration.md` (10:26) / `ui/mock/*` (10:22) はいずれも今サイクルで未更新。更新は記録側の `deviation.md` / `ui/brief.md` / 証跡と、後続 change のスタブだけ |

## 指摘事項

### [🟡 Minor] `ui/verification/` の 7 枚が brief.md の照合記録から名前で参照されていない

**該当箇所**: `ui/brief.md` (照合結果 (Android Native) / (iOS Native) / (MAUI iOS) の各節)、`ui/verification/`

**問題点**:
ksn-core `references/ui-artifacts.md` は「最終承認の時点で verification/ にあるのは、brief.md の照合記録が指す画像と同じ集合」「画像は証跡の実体、brief.md が索引」と定めている。逆方向に突き合わせると、次の 7 枚が brief.md のどの行からもファイル名で参照されていない。

- `android-menu-light.png` / `android-menu-dark.png` / `android-input-cells-dark.png`
- `ios-menu-light.png` / `ios-menu-dark.png`
- `maui-ios-menu-light.png` / `maui-ios-menu-dark.png`

いずれも中間ラウンドの画像ではなく最終周の画像であり (ルートメニューのライト / ダークは、まさにモックの規範範囲である「外観 UI」の証跡)、削除すべきものではない。欠けているのは索引側で、各節の「構造」の記述がこれらの画像に当たるにもかかわらず名前を書いていない。MAUI Android の節だけは `maui-android-menu-light.png` / `maui-android-menu-dark.png` を非回帰の行で名指ししており (`ui/brief.md:91,98`)、面ごとに索引の粒度が揃っていない。

低優先度と判断した理由: 画像は残っており、承認の事実は各節の照合記録が文章で持っている。索引が指していないだけで、証跡が失われる性質の欠落ではない。

**推奨修正**: 各節の「構造」または「状態」の行にファイル名を 1 つずつ添える (例: Android Native の構造行に `android-menu-light.png` / `android-menu-dark.png`、状態行に `android-input-cells-dark.png`)。撮影は不要で、brief.md に 3 行分の追記で閉じる。

### [🟡 Minor] 後続 change のスタブが「archive 後は archive 配下に画像がある」と書いている

**該当箇所**: `kasane/changes/fix-default-colors-dark-appearance/exploration.md:7`

**問題点**:
「証跡: `kasane/changes/add-sample-dark-mode-toggle/ui/verification/android-visibility-dark.png` / `ios-visibility-dark.png`。archive 後は `kasane/changes/archive/*-add-sample-dark-mode-toggle/` 配下」とあるが、`kasane/config.yaml` の `distill.archive-media: delete` により、ksn-distill は archive へ移す前に媒体ファイルを削除する。archive 配下に画像は存在しない (ksn-core `references/ui-artifacts.md`「archive 配下に媒体は存在しない」)。

後続 change の担当者はこの行を頼りに archive を探し、見つからずに再現からやり直すことになる。今サイクルで追記された MAUI 側の証跡参照 (`:12`) にも同じ前提が引き継がれている。

低優先度と判断した理由: 症状の記述そのものは exploration.md 本文と brief.md の照合記録に文章で残るため、画像が無くても後続 change は着手できる。

**推奨修正**: 「archive 後は…配下」を削り、「画像は archive 時に削除されるため、症状の記述は同 change の `ui/brief.md` の照合結果に残る (画像の実体は git 履歴)」に置き換える。1 行の書き換えで閉じる。

### [🔵 Suggestion] lessons の evidence が ButtonCell の文言差分を含んでいない

**該当箇所**: `kasane/lessons/inbox/mock-shows-param-not-matching-current-impl.md` (evidence / 経緯の add-sample-dark-mode-toggle エントリ)

**問題点**:
deviation.md 3 項目目はモックとの差分を 2 つ (「無効なボタン」行の創作、ButtonCell の文言「登録」対「ログアウト」) 記録しているが、lessons 側の evidence と経緯は行の創作しか書いていない。文言差分はサイクル 3 の相方レビューで初めて表面化した観測で、同じパターン (mock が現行実装から写経されていない) の同一 change 内の 2 例目にあたる。count の増加は不要だが、昇格時に「値・行・文言のいずれもが対象」と読める記述になっていた方が、ルール文の適用範囲が狭く受け取られない。

**推奨修正**: evidence の当該行に文言差分を一言足す。蒸留・昇格のタイミングでまとめてでよく、本サイクルでの対応は不要。

## 確認して問題がなかった観点

- **doc コメントの修正が意味を壊していないか**: `SampleTheme.cs:141-147` の remarks は「dark 側は light 側の色ロールに加えて description と valueText の色も明示する / この 2 つは未指定のままだと暗い下地に追随しない既定色へ解決されるため / light 側は未指定のまま残す」で、時間軸の参照を落としても因果が閉じている。iOS `SampleTheme.swift` / Android `SampleTheme.kt` の対応コメントと同じ現在形の説明になった
- **公開 doc コメントの内部用語**: 改訂された `Apply` の remarks は Sample アプリ内の型であり、ADR・change・デルタスペックの語を持ち込んでいない
- **`evidence/` の中身**: 置かれているのは静止画 1 枚のみ (動画なし、生ログなし)。ファイル名 `maui-ios-calendar-dark-range-before.png` は `<画面>-<状態>` 形式に沿い、どのレビューが撮ったかは brief.md の該当行から辿れる
- **証跡本文のプレースホルダ**: brief.md の Emulator 表記は 4 箇所とも `<android-serial>`。iOS 側は機種名と OS バージョンのみで、識別子の実値は無い。identity-lint も 0 件
- **パスの書き方**: 今サイクルで追記された参照は、change 内は change 相対 (`evidence/...`)、change をまたぐ参照はリポジトリ相対で、ローカル絶対パスは無い (local-path-lint 0 件)
- **足場の非改変**: モック (`ui/mock/*`) は裁定の対象になりながら書き換えられていない。裁定の記録は deviation.md と brief.md の記録側だけに入っており、規律どおり

## アクションプラン

1. **[Minor]** `ui/brief.md` の 3 節に、索引から漏れている 7 枚のファイル名を添える
2. **[Minor]** `kasane/changes/fix-default-colors-dark-appearance/exploration.md:7` の「archive 後は…配下」を書き換える
3. **[Suggestion]** lessons の evidence に ButtonCell 文言差分を追記 (蒸留時でよい)

1 と 2 はいずれも数行で閉じ、APPROVED を妨げない。着手せずそのまま蒸留へ進んでもよいが、2 は後続 change が読む前提の記述であるため、蒸留の前に直しておくと手戻りが無い。

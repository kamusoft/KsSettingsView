# README のスクリーンショット参照 URL の到達性 (2026-09-02)

README.md / README_ja.md の画像参照 4 箇所 × 2 枚を
https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/<file> に改めた。
両ファイルとも相対パス参照 (](assets/...) は 0 件。

curl -s -o /dev/null -w '%{http_code} %{content_type} %{size_download}':

  ios-modern.png      : 200 image/png 276114
  ios-classic.png     : 200 image/png 262906
  android-modern.png  : 200 image/png 160952
  android-classic.png : 200 image/png 151652

facade の nupkg ルートに置かれた README.md でも同じ 4 URL が絶対 URL で、
相対パス参照は残っていない。

# README のリンク参照 URL の到達性 (2026-09-02 追記)

package README は nuget.org 上で単体レンダリングされ、相対パスの解決先が存在しない。
画像に続けてリンク参照も
https://github.com/kamusoft/KsSettingsView/blob/develop/<path> の絶対 URL に改めた
(README.md / README_ja.md を同時、各 12 箇所、一意 10 URL)。

書き換えたリンク (README.md / README_ja.md の順に対応):
  README_ja.md            / README.md                    (言語切替)
  skills/README.md        / skills/README_ja.md          (導入節)
  skills/en|ja/kssettingsview-maui/SKILL.md              (MAUI 節の設定手順)
  skills/en|ja/kssettingsview-ios/SKILL.md               (Skills 節)
  skills/en|ja/kssettingsview-android/SKILL.md           (Skills 節)
  skills/en|ja/kssettingsview-maui/SKILL.md              (Skills 節)
  skills/en|ja/kssettingsview-aiforms-migration/SKILL.md (Skills 節)
  skills/README.md        / skills/README_ja.md          (Skills 索引)
  AGENTS.md                                              (両枚共通)
  kasane/concepts/index.md                               (両枚共通)
  .github/CONTRIBUTING.md / .github/CONTRIBUTING_ja.md   (貢献ガイドライン)
  LICENSE                                                (両枚共通)

残存確認: 両ファイルとも https:// / # 以外で始まる ](...) 参照は 0 件
  $ grep -cP '\]\((?!https?://|#)' README.md README_ja.md  => 0 / 0

curl -s -o /dev/null -w '%{http_code}' -I <url> (17 URL、いずれも 200):
  README.md / README_ja.md / LICENSE / AGENTS.md / kasane/concepts/index.md
  .github/CONTRIBUTING.md / .github/CONTRIBUTING_ja.md
  skills/README.md / skills/README_ja.md
  skills/{en,ja}/kssettingsview-{ios,android,maui,aiforms-migration}/SKILL.md

再 pack 後の nupkg 内 README (KsSettingsView.Maui.0.1.0-alpha.1.nupkg の README.md) でも
相対パス参照は 0 件 (画像 4 件・リンク 12 箇所がすべて絶対 URL)。

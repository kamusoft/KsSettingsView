# Exploration: fix-release-central-validation-wait

## 課題 / 動機

release workflow の publish が、Maven Central の deployment の検証完了を待たずに release へ進み、失敗することがある。

発見の文脈: 0.1.0-beta.2 のリリース (run 34039941831) の publish (attempt 1) が `Release Maven Central deployment` で失敗した。

```
##[error]release できる状態ではない (VALIDATED を期待、実際は VALIDATING): <deployment-id>
```

このステップは NuGet push の後に置かれており、次の 2 行を続けて実行する (`.github/workflows/release.yml`)。

```
scripts/release/central-portal.sh release "${KS_DEPLOYMENT_ID}"
scripts/release/central-portal.sh wait-published "${KS_DEPLOYMENT_ID}"
```

`scripts/release/central-portal.sh` のサブコマンドは次の構成で、**VALIDATED になるまで待つサブコマンドがない**。

| サブコマンド | 振る舞い |
|---|---|
| `status` | 状態を出力する |
| `release` | VALIDATED を再確認してから release する (待たない。VALIDATING なら失敗) |
| `wait-published` | PUBLISHED になるまで待つ |
| `drop` | VALIDATED / FAILED のときだけ削除する |

影響: publish は不可逆な公開を含む段であり、この失敗は NuGet.org への push が済んだ後に起きる。同じ version で「失敗した job から再実行」すれば復旧できるが、リリースのたびに再実行が 1 回増える。

### 探索で分かった現状 (2026-09-07、コードで確認)

- upload step (`Publish Android to Central Portal`) のコメントに「upload 直後 (VALIDATED)」とあり、fresh な upload の経路では検証の決着を待っていない。この前提が今回の失敗の直接原因
- 一方、**再実行の経路 (前の attempt の deployment ID を引き継いだとき) には PENDING / VALIDATING を最大 1800 秒待つループが workflow に直書きで存在する**。待ちが無いのではなく、片方の経路にだけ、スクリプトの外にある
- publish の順序は upload → NuGet push → Maven release → tag。NuGet push も実質不可逆 (unlist のみ) なので、検証が FAILED になる bundle でも NuGet には先に出てしまい、その version が NuGet だけに存在する lockstep 崩れが起こり得る
- 後始末 (`Drop pending deployment`) が VALIDATING で削除できない経路は既に設計済み: ID を残し、次の attempt が引き継いで再実行経路の待ちに入る (handbook リリース手順「失敗したとき」にも記述あり)

## 検討した選択肢 (却下案と理由を含む)

論点: 検証の決着 (VALIDATED / FAILED) をどこで待つか。

| 判断軸 | A: upload 直後 (NuGet push の前) で待つ | B: Maven release の直前で待つ |
|---|---|---|
| 今回の失敗 | 直る | 直る |
| 検証 FAILED のときの被害 | NuGet に出る前に止まる。version を焼かずにやり直せる | NuGet は出てしまい、Maven だけ無い version が残る |
| publish の所要時間 | 検証時間ぶん NuGet push が後ろにずれる (合計はほぼ同じ) | NuGet push と検証が重なるので数十秒〜数分速い |
| 触る範囲 | upload step + スクリプト + 自己テスト。再実行経路の直書きループも置換 | release step の前に 1 行 + スクリプト + 自己テスト |
| handbook への影響 | 「失敗したとき」の表 (Maven の upload 行) の文言を少し直す | ほぼ無し |

- **A を採用**。理由: 失敗の原因である非対称を直すだけでなく、「不可逆な操作の前に、取り消せる段階で分かる失敗はすべて出し切る」という publish の設計意図に合う
- **B は却下**。数分の短縮より、検証 FAILED 時に NuGet だけ出てしまう lockstep 崩れの方が重い

待ちの上限超過は「失敗にして deployment は残す → 次の attempt に委ねる」で、現行の後始末・再実行の設計とそのまま整合するため、新しい仕組みは足さない。

## 決定事項

- 検証の決着は **upload の直後、NuGet push より前** で待つ (2026-09-07)
- 待ちはスクリプトのサブコマンド (`wait-validated`) として実装し、`wait-published` と同じ骨格・同じ環境変数 (ポーリング間隔・上限) を流用する
  - 契約の精緻化 (実装時、2026-09-07): 検証の決着 (PENDING / VALIDATING を抜けること) を待ち、決着した状態を `status` と同じ語彙で標準出力に返す。失敗にするのは上限超過だけで、FAILED / NOT_FOUND は状態として返す。理由: 再実行経路は FAILED を「drop して upload をやり直す」回復に使うため、サブコマンド側で失敗にすると置換できない。upload 直後の経路では呼び出し側が VALIDATED 以外を失敗にする
- 再実行経路に直書きされている検証待ちループも同じサブコマンドに置き換え、待ちの実装を一本化する
- `release` サブコマンドの「VALIDATED を再確認してから送る」契約は安全弁としてそのまま残す
- upload step のコメント「upload 直後 (VALIDATED)」は実態 (検証の決着を待ってから VALIDATED) に合わせて直す
- handbook リリース手順「失敗したとき」の表の Maven upload 行を、待ちの位置が変わることに合わせて直す

## ADR 候補 (作成済み: なし / 未起票: なし)

局所的な並び替えで覆すコストも低く、ADR の選別基準 (覆すコスト高 / 境界を越える / 将来を制約) に該当しない。

## 未決の論点

なし。

## UI 素材 (ui/references/ の一覧と注釈)

なし。

## 変更級の推奨: S

- 触る範囲: workflow 1 ファイル (upload step の待ち追加・再実行経路のループ置換・release step は変更なし)、スクリプト 1 本 (サブコマンド追加 + 自己テスト)、handbook 1 ファイルの表 1 行
- 公開 API 変更なし、UI なし、可逆 (戻せる)
- 検証は `scripts/release/central-portal.sh --selftest` と、dry-run では publish が走らないため実リリース時の実測で確認する

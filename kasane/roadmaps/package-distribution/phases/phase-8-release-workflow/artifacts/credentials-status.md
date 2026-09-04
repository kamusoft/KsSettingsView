# 発行用の認証情報の準備状況 (phase-5 からの申し送り、2026-09-01)

phase-8 agenda の TODO から退避した記録。secrets 登録手順書の材料。secrets の置き場所は 2026-09-03 に GitHub Environment `release` と決定した (agenda の決定事項)。

## Central Portal User Token

発行済み (2026-09-01、オーナー保管)。`mavenCentralUsername` / `mavenCentralPassword` に入れるのは Portal のログイン資格情報ではなく User Token のペア (vanniktech plugin の公式が明記)。トークンは https://central.sonatype.com/usertoken で発行し、生成時のモーダルを閉じると二度と再表示できない (失った場合は再生成)。

## GPG 署名鍵

| 項目 | 値 |
|---|---|
| 生成場所 | オーナーのローカル |
| 構成 | RSA 4096 / 無期限 / プライマリキー自身が署名鍵 (`[SC]`、サブキーなし)。Sonatype が「Maven/Nexus はプライマリキーでのみ署名を検証する」と明記しているため意図的にこの構成 |
| fingerprint | `85EDDCDA8CF524FB5C4CA3C154DAFFF896DB9B8F` |
| `signingInMemoryKeyId` に入れる短い ID | `96DB9B8F` |
| 公開鍵の公開先 | keyserver.ubuntu.com と keys.openpgp.org (両方から HTTP 取得を確認済み。openpgp.org はメール未検証だと UID を剥がして鍵本体のみ公開するが、Maven Central の検証は fingerprint で引ければ通る) |
| `signingInMemoryKey` の中身 | `gpg --export-secret-keys --armor` の全文。平文の秘密鍵をディスクに残さないため Secrets 登録の直前に export する |
| 失効証明書 | 鍵生成時に自動生成、オーナー保管 |

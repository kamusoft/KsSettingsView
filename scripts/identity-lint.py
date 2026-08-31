#!/usr/bin/env python3
"""個体・個人・秘密を特定する値の検査 (Kasane 標準 lint / hook)。

端末の生ログ・ビルドログ・クラッシュログを証跡として貼るときに混入しやすい値 —
実機シリアル・Simulator UDID・session id・署名アイデンティティ (氏名 + Team ID)・ホスト名・
MAC アドレス・メールアドレス・IP・JWT 等 — がアーティファクトに書き込まれるのを防ぐ。
規約とプレースホルダの語彙は ksn-core references/evidence.md。

ローカル絶対パスの検査は local-path-lint.py が担当する (判定の置き場を分けている)。
検査範囲を `lint.identity.scope` (既定: kasane) に限るのは、ソースコードとテストには
正当な UUID 定数・fixture があるため。

使い方:
  python3 scripts/identity-lint.py            # lint: 範囲内のファイルを検査 (untracked 含む。違反があれば exit 1)
  python3 scripts/identity-lint.py --hook     # hook: PreToolUse の stdin JSON を検査し deny を返す
  python3 scripts/identity-lint.py --paths a b  # 指定ファイルだけ lint
  python3 scripts/identity-lint.py --selftest   # 検出ロジック・git grep 候補拾い・hook の疎通確認

検出群 (kasane/config.yaml の `lint.identity.disable` で群ごとに無効化できる):
  device     UUID (Simulator UDID / session id / incident id)、serial= / udid= / adb -s / adb: の実値
  developer  署名アイデンティティ `Apple Development: 氏名 (TEAMID)`、DEVELOPMENT_TEAM、ホスト名 (*.local / Host Name:)、端末名
  network    MAC アドレス、SSID (keyed)、プライベート IP (changes/ 限定)、config `lint.internal-hosts` のホスト名
  personal   メールアドレス・緯度経度 (changes/ 限定)、電話番号 (keyed)
  secret     JWT、Authorization ヘッダの値、push token (keyed)。汎用の API key 等は gitleaks の領分

置換先のプレースホルダは PLACEHOLDERS (log-sanitize.py が同じ表で置換する)。
正当な値 (concepts に載せる名前空間 UUID 等) は `lint.identity.allow` に列挙して除外する。`@ドメイン` の形で
書くとそのドメインのメールアドレス (デモデータの自社ドメイン等) を許可する。
"""

from __future__ import annotations

import importlib.util
import ipaddress
import json
import os
import re
import sys


def _load_sibling():
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "local-path-lint.py")
    spec = importlib.util.spec_from_file_location("local_path_lint", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


LP = _load_sibling()

DEFAULT_SCOPE = ["kasane"]
CHANGES_ONLY_PREFIXES = ("kasane/changes/", "kasane/roadmaps/")

# kind → (群, プレースホルダ)
PLACEHOLDERS: dict[str, tuple[str, str]] = {
    "uuid": ("device", "<uuid>"),
    "ios-udid": ("device", "<ios-udid>"),
    "android-serial": ("device", "<android-serial>"),
    "signing-identity": ("developer", "<signing-identity>"),
    "team-id": ("developer", "<team-id>"),
    "host": ("developer", "<host>"),
    "device-name": ("developer", "<device-name>"),
    "mac": ("network", "<mac>"),
    "ssid": ("network", "<ssid>"),
    "ip": ("network", "<ip>"),
    "internal-host": ("network", "<internal-host>"),
    "email": ("personal", "<email>"),
    "location": ("personal", "<location>"),
    "phone": ("personal", "<phone>"),
    "jwt": ("secret", "<jwt>"),
    "auth-token": ("secret", "<token>"),
    "push-token": ("secret", "<push-token>"),
}
CHANGES_ONLY_KINDS = {"ip", "email", "location"}

UUID = re.compile(r"\b[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}\b")
ZERO_UUID = re.compile(r"0{8}")
KEYED_UDID = re.compile(r"\budid\s*[=:]\s*([^\s,;)\]}\"'`]+)", re.IGNORECASE)
KEYED_SERIAL = re.compile(r"\bserial(?:no|number)?\s*[=:]\s*([^\s,;)\]}\"'`]+)", re.IGNORECASE)
ADB_S = re.compile(r"\badb\s+(?:[^\s]+\s+)*-s\s+([^\s]+)")
ADB_LABEL = re.compile(r"\badb\s*[:：]\s*([^\s,;)\]}\"'`]+)")
ANDROID_SERIAL = re.compile(r"^(?=.*[A-Z])(?=.*[0-9])[A-Z0-9]{8,24}$")

SIGNING = re.compile(
    r"((?:Apple (?:Development|Distribution)|iPhone (?:Developer|Distribution)|Developer ID Application|Mac Developer): )"
    r"([^()\n]+?) \(([A-Z0-9]{10})\)"
)
TEAM_ID = re.compile(r"\b(DEVELOPMENT_TEAM|TeamIdentifier|Team ID|teamID|teamId)\s*[=:]\s*\"?([A-Z0-9]{10})\b")
# mDNS ホスト名 (`mymac.local`)。`.local` の直後にドット + 英数が続くものは
# ファイル名の一部 (`settings.local.json` 等) なのでホスト名として扱わない
HOST_LOCAL = re.compile(r"\b([A-Za-z0-9][A-Za-z0-9-]{1,62})\.local\b(?!\.[A-Za-z0-9])")
HOST_KEYED = re.compile(r"\b(Host Name|hostname|HostName|ComputerName|LocalHostName)\s*[=:]\s*\"?([^\s\"',;]+)", re.IGNORECASE)
DEVICE_NAME_KEYED = re.compile(r"\b(Device Name|DeviceName|deviceName|device_name)\s*[=:]\s*\"?([^\"\n,;/|]+)")
DEVICE_NAME_POSSESSIVE = re.compile(
    r"([^\s\"'(/|]+(?:'s|’s)\s?(?:iPhone|iPad|iPod|Apple Watch|Mac(?:Book)?(?: Pro| Air| mini)?|Pixel|Galaxy)[^\"\n,;)/|]*)"
)
# 日本語の既定端末名 (`太郎のiPhone`)。空白なしの `の` に限り、指示語・一人称は除外する
DEVICE_NAME_JA = re.compile(
    r"([^\s\"'(/|、。]+?の(?:iPhone|iPad|iPod|Apple Watch|Mac(?:Book)?(?: Pro| Air| mini)?|Pixel|Galaxy)(?:\s?(?:Pro|Air|mini|Max|Plus|\d+))*)"
)
DEVICE_NAME_JA_IGNORE = ("この", "その", "あの", "どの", "私", "自分", "ユーザ", "ユーザー", "自身", "各", "他", "同じ", "別")

MAC = re.compile(r"\b(?:[0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}\b")
MAC_IGNORE = {"00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "02:00:00:00:00:00"}
SSID = re.compile(r"\bSSID\s*[=:]\s*(?:\"([^\"\n]+)\"|'([^'\n]+)'|([^\s\"',;]+))", re.IGNORECASE)
IP = re.compile(r"\b(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})\b")
IP_IGNORE = {"10.0.2.2", "10.0.2.15"}  # Android エミュレータのホスト / 自身
# 検出するのはプライベート・リンクローカル・CGNAT の帯だけ (public IP はバージョン表記 `2.11.0.1` との誤検出が多く、モバイルアプリのログでは稀)
IP_NETWORKS = [ipaddress.ip_network(n) for n in ("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "169.254.0.0/16", "100.64.0.0/10")]

EMAIL = re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")
EMAIL_IGNORE_DOMAIN_PREFIXES = ("example.",)  # example.com / example.jp / 派生 (タイプ中の example.comabc 等)
EMAIL_IGNORE_DOMAIN_SUFFIXES = ("noreply.github.com",)  # git 著者の noreply (公開情報)
EMAIL_IGNORE_LOCAL = ("noreply", "no-reply", "donotreply")
LOCATION_PAIR = re.compile(r"(?<![\d.])(-?\d{1,2}\.\d{4,}),\s*(-?\d{1,3}\.\d{4,})(?![\d.])")
LOCATION_KEYED = re.compile(r"\b(lat(?:itude)?|lon(?:g|gitude)?)\s*[=:]\s*(-?\d{1,3}\.\d{3,})", re.IGNORECASE)
PHONE_KEYED = re.compile(r"\b(tel:\s*|phone(?:Number|_number)?\s*[=:]\s*|Phone:\s*)(\+?[\d][\d\-() ]{7,}\d)", re.IGNORECASE)

JWT = re.compile(r"\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b")
AUTH_HEADER = re.compile(r"(Authorization\s*[=:]\s*(?:Bearer|Basic|Token)?\s*)([^\s\"'<$]{8,})", re.IGNORECASE)
PUSH_TOKEN = re.compile(
    r"\b(deviceToken|device_token|apnsToken|apns_token|fcmToken|fcm_token|pushToken|push_token|registrationToken)"
    r"\s*[=:]\s*\"?([A-Za-z0-9_:\-]{20,})"
)

PLACEHOLDER_VALUE = re.compile(r"^[<$\"'`{]|^\.\.\.$|^-+$|^x{4,}$", re.IGNORECASE)

# 候補行を粗く絞るためのパターン (最終判定は find_identities が行う)。`git grep -E` に渡すため
# POSIX ERE の構文だけで書く — `\b` (単語境界) は GNU 拡張であり、Linux では効いて macOS では
# 効かないため、使うとプラットフォームによって候補行の集合がずれる
GREP_PATTERN = (
    r"[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-|serial|udid|adb .*-s |adb *[:：]|Apple (Development|Distribution)|iPhone (Developer|Distribution)"
    r"|DEVELOPMENT_TEAM|TeamIdentifier|Team ID|\.local|Host ?Name|hostname|ComputerName|Device ?Name|'s (iPhone|iPad|Mac)|の ?(iPhone|iPad|Mac)"
    r"|([0-9A-Fa-f]{2}:){5}|SSID|[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+|@[A-Za-z0-9.-]+\.[A-Za-z]{2,}|lat|lon|tel:|[Pp]hone"
    r"|eyJ|Authorization|[Tt]oken"
)


# lint スクリプト自身は検査しない: ソースには自己テストの違反例 (検出対象の見本) がリテラルで
# 載るため。同名判定の意図は local-path-lint.py の is_self_file と同じ
SELF_BASENAME = os.path.basename(os.path.abspath(__file__))


def _is_self_file(path: str) -> bool:
    return bool(path) and os.path.basename(path) == SELF_BASENAME


def _is_placeholder(value: str) -> bool:
    return bool(PLACEHOLDER_VALUE.match(value.strip())) or "<" in value


class Settings:
    def __init__(self, root: str):
        cfg = LP.load_config(root)
        scope = LP.config_get(cfg, "lint.identity.scope", DEFAULT_SCOPE)
        self.scope = [str(s) for s in (scope if isinstance(scope, list) else [scope])]
        allow = LP.config_get(cfg, "lint.identity.allow", [])
        allow = [str(a).lower() for a in (allow if isinstance(allow, list) else [allow])]
        self.allow = {a for a in allow if not a.startswith("@")}
        self.allow_domains = tuple(a[1:] for a in allow if a.startswith("@"))  # `@kamusoft.jp` = そのドメインのメールを許可
        disable = LP.config_get(cfg, "lint.identity.disable", [])
        self.disabled = {str(d) for d in (disable if isinstance(disable, list) else [disable])}
        hosts = LP.config_get(cfg, "lint.internal-hosts", [])
        self.internal_hosts = [str(h) for h in (hosts if isinstance(hosts, list) else [hosts]) if str(h)]
        self.excludes = LP.load_excludes(root)

    def in_scope(self, rel: str) -> bool:
        if LP.is_excluded(rel, self.excludes):
            return False
        return rel.split("/", 1)[0] in self.scope


# ---------- 判定 ----------

def find_identities(line: str, settings: Settings | None = None, changes_scope: bool = True) -> list[tuple[str, str, int, int]]:
    """1 行から (kind, 値, start, end) を列挙する。changes_scope=False のとき changes/ 限定の kind は検出しない。"""
    s = settings
    found: list[tuple[str, str, int, int]] = []

    def add(kind: str, value: str, start: int, end: int) -> None:
        group = PLACEHOLDERS[kind][0]
        if s and group in s.disabled:
            return
        if kind in CHANGES_ONLY_KINDS and not changes_scope:
            return
        if s and value.lower() in s.allow:
            return
        if _is_placeholder(value):
            return
        found.append((kind, value, start, end))

    # device
    for m in UUID.finditer(line):
        if not ZERO_UUID.search(m.group(0)):
            add("uuid", m.group(0), m.start(), m.end())
    for m in KEYED_UDID.finditer(line):
        v = m.group(1)
        if UUID.fullmatch(v) or re.fullmatch(r"[0-9a-fA-F]{40}|[0-9a-fA-F]{8}-[0-9a-fA-F]{16}", v):
            add("ios-udid", v, m.start(1), m.end(1))
    for pat in (KEYED_SERIAL, ADB_S, ADB_LABEL):
        for m in pat.finditer(line):
            v = m.group(1)
            if ANDROID_SERIAL.match(v) or UUID.fullmatch(v):
                add("android-serial", v, m.start(1), m.end(1))
    # developer
    for m in SIGNING.finditer(line):
        add("signing-identity", m.group(2) + " (" + m.group(3) + ")", m.start(2), m.end(3) + 1)
    for m in TEAM_ID.finditer(line):
        add("team-id", m.group(2), m.start(2), m.end(2))
    for m in HOST_LOCAL.finditer(line):
        add("host", m.group(0), m.start(), m.end())
    for m in HOST_KEYED.finditer(line):
        add("host", m.group(2), m.start(2), m.end(2))
    for m in DEVICE_NAME_KEYED.finditer(line):
        add("device-name", m.group(2).strip(), m.start(2), m.end(2))
    for m in DEVICE_NAME_POSSESSIVE.finditer(line):
        add("device-name", m.group(1).strip(), m.start(1), m.end(1))
    for m in DEVICE_NAME_JA.finditer(line):
        owner = m.group(1).split("の", 1)[0]
        if owner and not owner.startswith(DEVICE_NAME_JA_IGNORE):
            add("device-name", m.group(1).strip(), m.start(1), m.end(1))
    # network
    for m in MAC.finditer(line):
        if m.group(0).lower() not in MAC_IGNORE:
            add("mac", m.group(0), m.start(), m.end())
    for m in SSID.finditer(line):
        g = next(i for i in (1, 2, 3) if m.group(i) is not None)
        add("ssid", m.group(g).strip(), m.start(g), m.end(g))
    for m in IP.finditer(line):
        v = m.group(1)
        if v in IP_IGNORE:
            continue
        try:
            ip = ipaddress.ip_address(v)
        except ValueError:
            continue
        if any(ip in n for n in IP_NETWORKS):
            add("ip", v, m.start(1), m.end(1))
    if s:
        for h in s.internal_hosts:
            for m in re.finditer(re.escape(h), line, re.IGNORECASE):
                add("internal-host", m.group(0), m.start(), m.end())
    # personal
    for m in EMAIL.finditer(line):
        v = m.group(0)
        local, domain = v.lower().rsplit("@", 1)
        if domain.startswith(EMAIL_IGNORE_DOMAIN_PREFIXES) or domain.endswith(EMAIL_IGNORE_DOMAIN_SUFFIXES) or local in EMAIL_IGNORE_LOCAL:
            continue
        if s and (domain in s.allow_domains or domain.endswith(tuple("." + d for d in s.allow_domains))):
            continue
        add("email", v, m.start(), m.end())
    for m in LOCATION_PAIR.finditer(line):
        lat, lon = float(m.group(1)), float(m.group(2))
        if -90 <= lat <= 90 and -180 <= lon <= 180:
            add("location", m.group(0), m.start(), m.end())
    for m in LOCATION_KEYED.finditer(line):
        add("location", m.group(2), m.start(2), m.end(2))
    for m in PHONE_KEYED.finditer(line):
        add("phone", m.group(2).strip(), m.start(2), m.end(2))
    # secret
    for m in JWT.finditer(line):
        add("jwt", m.group(0), m.start(), m.end())
    for m in AUTH_HEADER.finditer(line):
        add("auth-token", m.group(2), m.start(2), m.end(2))
    for m in PUSH_TOKEN.finditer(line):
        add("push-token", m.group(2), m.start(2), m.end(2))

    # 重なり (UUID と udid= 等) は長い方・先に見つかった方を残す
    found.sort(key=lambda f: (f[2], -(f[3] - f[2])))
    merged: list[tuple[str, str, int, int]] = []
    last_end = -1
    for f in found:
        if f[2] < last_end:
            continue
        merged.append(f)
        last_end = f[3]
    return merged


def is_changes_scope(rel: str) -> bool:
    return rel.startswith(CHANGES_ONLY_PREFIXES)


def describe(found: list[tuple[str, str, int, int]]) -> str:
    return ", ".join(f"{k}: {v}" for k, v, _, _ in found)


# ---------- lint モード ----------

def lint(root: str, paths: list[str] | None) -> int:
    import subprocess
    settings = Settings(root)
    if paths is None:
        out = subprocess.run(["git", "grep", "--untracked", "-n", "-I", "-E", GREP_PATTERN, "--"] + settings.scope,
                             cwd=root, capture_output=True, text=True).stdout
        candidates = [l for l in out.splitlines() if l]
    else:
        candidates = []
        for p in paths:
            rel = LP.normalize_rel(p, root)
            try:
                with open(os.path.join(root, rel), encoding="utf-8", errors="replace") as f:
                    candidates += [f"{rel}:{i}:{l}" for i, l in enumerate(f.read().splitlines(), 1)]
            except OSError:
                continue
    hits = []
    for entry in candidates:
        parts = entry.split(":", 2)
        if len(parts) < 3:
            continue
        rel, lineno, text = parts
        rel = LP.normalize_rel(rel, root)
        if _is_self_file(rel) or not settings.in_scope(rel):
            continue
        found = find_identities(text, settings, is_changes_scope(rel))
        if found:
            hits.append(f"{rel}:{lineno}: {describe(found)}")
    if not hits:
        return 0
    print("個体・個人・秘密を特定する値が含まれています (ksn-core references/evidence.md。"
          "`python3 scripts/log-sanitize.py <file>` でプレースホルダに置換できます):")
    for h in hits:
        print("  " + h)
    return 1


# ---------- 自己テスト ----------

# (説明, 行, 期待する kind のリスト)。検出ロジック単体と、一時リポジトリでの lint 疎通の両方で使う。
# lint 疎通は実際の `git grep` に GREP_PATTERN を通すので、「候補拾いがこのプラットフォームで
# 無音になっていないか」(POSIX ERE 非対応構文の混入等) をここで検出できる
SELFTEST_CASES: list[tuple[str, str, list[str]]] = [
    ("uuid", "session 12345678-1234-1234-1234-1234567890AB を取得", ["uuid"]),
    ("ios-udid (keyed 40hex)", "udid=0123456789abcdef0123456789abcdef01234567", ["ios-udid"]),
    ("android-serial (keyed)", "serial=R58M12ABCDE", ["android-serial"]),
    ("signing-identity", "sign: Apple Development: Taro Yamada (ABCDE12345)", ["signing-identity"]),
    ("team-id", "DEVELOPMENT_TEAM = ABCDE12345", ["team-id"]),
    ("host (.local)", "ホスト mymac-book.local から接続", ["host"]),
    ("settings.local.json は許容", "設定は .claude/settings.local.json にある", []),
    ("mac", "MAC: aa:bb:cc:dd:ee:01", ["mac"]),
    ("ip (private)", "server=192.168.10.21", ["ip"]),
    ("ip (public はバージョン表記とみなし許容)", "version 2.11.0.1 をリリース", []),
    ("email", "author: taro@kamusoft-demo.co.jp", ["email"]),
    ("email (example ドメインは許容)", "demo@example.com に送る", []),
    ("jwt", "token eyJabcdefghijk.eyJabcdefghijk.abcdefghijklmn", ["jwt"]),
    ("phone (keyed)", "phone: 090-1234-5678", ["phone"]),
    ("device-name (日本語)", "太郎のiPhone 15 Pro から送信", ["device-name"]),
    ("device-name (一人称は許容)", "私のiPhoneでは再現しない", []),
]


def selftest() -> int:
    import contextlib
    import io
    import subprocess
    import tempfile

    failures = 0

    def check(ok: bool, name: str, detail: str = "") -> None:
        nonlocal failures
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail else ''}")

    print("[検出ロジック]")
    for name, line, expected in SELFTEST_CASES:
        actual = [k for k, _, _, _ in find_identities(line)]
        check(actual == expected, name, f"期待 {expected or 'なし'} / 実際 {actual or 'なし'}")
    check(find_identities("server=192.168.10.21", changes_scope=False) == [],
          "changes/ 限定 kind は範囲外で抑制", "ip")

    print("[lint 疎通 (git grep 候補拾い)]")
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["git", "init", "-q"], cwd=tmp, check=True, capture_output=True)
        changes_dir = os.path.join(tmp, "kasane", "changes", "selftest")
        os.makedirs(changes_dir)
        with open(os.path.join(changes_dir, "log.md"), "w", encoding="utf-8") as f:
            f.write("\n".join(l for _, l, _ in SELFTEST_CASES) + "\n")
        concepts_dir = os.path.join(tmp, "kasane", "concepts")
        os.makedirs(concepts_dir)
        with open(os.path.join(concepts_dir, "note.md"), "w", encoding="utf-8") as f:
            f.write("server=192.168.10.21\n")
        with open(os.path.join(changes_dir, SELF_BASENAME), "w", encoding="utf-8") as f:
            f.write("MAC: aa:bb:cc:dd:ee:01\n")
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            code = lint(tmp, None)
        out = buf.getvalue()
        reported: dict[int, str] = {}
        for h in out.splitlines():
            m = re.match(r"\s*kasane/changes/selftest/log\.md:(\d+): (.+)", h)
            if m:
                reported[int(m.group(1))] = m.group(2)
        for i, (name, _line, expected) in enumerate(SELFTEST_CASES, 1):
            actual = [p.split(":", 1)[0] for p in reported.get(i, "").split(", ") if p]
            check(actual == expected, name, f"期待 {expected or 'なし'} / 実際 {actual or 'なし'}")
        check(code == 1, "違反ありで exit 1")
        check("concepts/note.md" not in out, "changes/ 限定 kind は concepts/ で報告しない")
        check(SELF_BASENAME not in out, "自分自身 (同名ファイル) は検査しない")

        print("[hook 疎通]")
        for name, fname, content, expect_deny in [
            ("違反の書き込みは deny", "new.md", "MAC: aa:bb:cc:dd:ee:01", True),
            ("プレースホルダは通す", "new.md", "MAC: <mac>", False),
            ("自分自身 (同名ファイル) への書き込みは検査しない", SELF_BASENAME, "MAC: aa:bb:cc:dd:ee:01", False),
        ]:
            payload = {"tool_name": "Write", "cwd": tmp,
                       "tool_input": {"file_path": os.path.join(tmp, "kasane", "changes", "selftest", fname), "content": content}}
            proc = subprocess.run([sys.executable, os.path.abspath(__file__), "--hook"],
                                  input=json.dumps(payload), capture_output=True, text=True)
            denied = '"deny"' in proc.stdout
            check(proc.returncode == 0 and denied == expect_deny, name,
                  f"期待 deny={expect_deny} / 実際 deny={denied}")

    print(f"\n自己テスト: {'全件 OK' if not failures else f'{failures} 件 NG'}")
    return 1 if failures else 0


# ---------- hook モード ----------

def hook() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return 0
    cwd = data.get("cwd") or os.getcwd()
    root = LP.repo_root(cwd)
    settings = Settings(root)
    hits: list[str] = []
    for path, text in LP.texts_from_hook_input(data):
        if _is_self_file(path):
            continue
        rel = LP.normalize_rel(path, root) if path else ""
        if rel and not settings.in_scope(rel):
            continue
        for i, line in enumerate(text.splitlines(), 1):
            found = find_identities(line, settings, is_changes_scope(rel) if rel else True)
            if found:
                hits.append(f"{rel or '(本文)'}:{i}: {describe(found)}")
    if not hits:
        return 0
    reason = (
        "個体・個人・秘密を特定する値 (UDID / シリアル / 署名アイデンティティ / ホスト名 / MAC / メール / トークン等) "
        "の書き込みをブロックします。生ログは `python3 scripts/log-sanitize.py <file>` を通してから貼り、"
        "本文ではプレースホルダ (`<android-serial>` `<email>` 等。ksn-core references/evidence.md) に置き換えてください。\n"
        + "\n".join(hits[:10])
    )
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }, ensure_ascii=False))
    return 0


def main(argv: list[str]) -> int:
    if "--hook" in argv:
        return hook()
    if "--selftest" in argv:
        return selftest()
    paths: list[str] | None = None
    if "--paths" in argv:
        paths = argv[argv.index("--paths") + 1:]
    return lint(LP.repo_root(), paths)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

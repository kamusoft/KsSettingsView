// DeclarativeDSLIdentity.swift
// KsSettingsViewSwiftUI
//
// DSL（`KsSettingsView { Section { Cell... } }`）における Section / Cell の
// 同一性判定戦略を実装するユーティリティ。
//
// 宣言ツリーの安定同一性の原則は core/ADR-0008 を参照。
//
// 重要事項:
//   - `Section.id` / `KsCell.id` は `UUID` で表現される（Core 層既存仕様）。
//   - DSL 経路では body 再評価のたびに新規 `Section` / Cell 値が構築されるため、
//     `UUID()` で毎回採番する方式は Diff 同一性判定を破壊する。
//   - 本ユーティリティは「位置 + ヘッダ文字列 / 明示 ID / ForEach の item.id」等の
//     ヒントから **安定した UUID** を導出する。
//   - 導出 UUID は `AnyHashable` をシードとした name-based UUID（バージョン 5 風）として
//     決定的に生成する（Foundation の標準 UUID は v4 ランダムのみのため、自前実装）。

import Foundation

/// DSL における Section / Cell の ID 識別ヒント。
///
/// DSL のビルド時に各 Section / Cell に紐付けられ、Diff 算出ロジックは本ヒントを
/// `UUID` に変換して使用する。
public enum DSLIdentityHint: Hashable {
    /// ForEach 配下：`item.id` または `id:` KeyPath から取り出した識別子。
    case forEach(AnyHashable)
    /// 明示指定 modifier（`.sectionID(...)` / `.cellID(...)`）。
    case explicit(AnyHashable)
    /// ヘッダ文字列ベースの安定化（Section 限定）。
    case headerText(rootIdx: Int, text: String)
    /// 位置 + Cell 型ベースの安定化（Cell 限定）。
    case positional(sectionID: UUID, indexInSection: Int, cellType: String)
    /// ルート位置ベースのフォールバック（Section 限定）。
    case rootPosition(rootIdx: Int)
}

/// `DSLIdentityHint` を `UUID` に決定的に変換するユーティリティ。
///
/// Foundation の `UUID()` は v4 ランダムで決定的でないため、本ユーティリティは
/// SHA-1 風に圧縮した `Hasher` 由来の値域から **安定** な UUID を構築する。
/// 同じヒントに対しては必ず同じ UUID を返す。
public enum DSLIdentityUUID {

    /// 名前空間 UUID（KsSettingsView DSL 用に固定）。
    /// 一意性を保つために v4 ランダムで一度生成した値を定数化している。
    private static let namespace: UUID = UUID(uuidString: "5F0A1B2C-3D4E-5F60-7182-93A4B5C6D7E8")!

    /// ヒントから決定的な `UUID` を生成する。
    /// - Parameter hint: 採用するヒント
    /// - Returns: ヒントに対応する安定 UUID
    public static func uuid(from hint: DSLIdentityHint) -> UUID {
        var hasher = StableHasher()
        // 名前空間 UUID を先頭に combine してドメインを区別する。
        hasher.combine(uuid: Self.namespace)
        // ヒントの種別（discriminator）と値を組み込む。
        switch hint {
        case .forEach(let id):
            hasher.combine(byte: 1)
            hasher.combine(anyHashable: id)
        case .explicit(let id):
            hasher.combine(byte: 2)
            hasher.combine(anyHashable: id)
        case let .headerText(rootIdx, text):
            hasher.combine(byte: 3)
            hasher.combine(int: rootIdx)
            hasher.combine(string: text)
        case let .positional(sectionID, indexInSection, cellType):
            hasher.combine(byte: 4)
            hasher.combine(uuid: sectionID)
            hasher.combine(int: indexInSection)
            hasher.combine(string: cellType)
        case .rootPosition(let rootIdx):
            hasher.combine(byte: 5)
            hasher.combine(int: rootIdx)
        }
        return hasher.finalizeAsUUID()
    }
}

/// 決定的な UUID を生成するための内部ハッシュ器。
///
/// Swift 標準の `Hasher` は per-process でランダムシードを持つため、決定的な UUID 生成に
/// は使えない。本ハッシュ器は FNV-1a を 16 バイトに拡張した自前実装で、入力バイト列が
/// 同じであれば常に同じ 16 バイトを返す。
internal struct StableHasher {

    // FNV-1a の 64bit シード値を 2 本並列で回し、合計 128bit（16byte = UUID）を作る。
    private var state0: UInt64 = 0xcbf29ce484222325
    private var state1: UInt64 = 0x84222325cbf29ce4
    private let prime: UInt64 = 0x100000001b3

    mutating func combine(byte: UInt8) {
        state0 ^= UInt64(byte)
        state0 = state0 &* prime
        state1 = (state1 &+ UInt64(byte)) &* prime
    }

    mutating func combine(bytes: [UInt8]) {
        for b in bytes {
            combine(byte: b)
        }
    }

    mutating func combine(int: Int) {
        let value = UInt64(bitPattern: Int64(int))
        for shift in stride(from: 0, through: 56, by: 8) {
            combine(byte: UInt8((value >> shift) & 0xff))
        }
    }

    mutating func combine(uuid: UUID) {
        let u = uuid.uuid
        combine(bytes: [
            u.0, u.1, u.2, u.3, u.4, u.5, u.6, u.7,
            u.8, u.9, u.10, u.11, u.12, u.13, u.14, u.15,
        ])
    }

    mutating func combine(string: String) {
        // UTF-8 バイト列を直接 combine。
        combine(bytes: Array(string.utf8))
        // 終端マーカ（同一プレフィクスの衝突回避）。
        combine(byte: 0)
    }

    mutating func combine(anyHashable: AnyHashable) {
        // `AnyHashable` の中身が `String` / `Int` / `UUID` のような頻出型なら型ごとに
        // 安定 bytes 化、それ以外は `description` を採用する（Hashable は per-process なため不可）。
        if let s = anyHashable.base as? String {
            combine(byte: 100)
            combine(string: s)
        } else if let i = anyHashable.base as? Int {
            combine(byte: 101)
            combine(int: i)
        } else if let u = anyHashable.base as? UUID {
            combine(byte: 102)
            combine(uuid: u)
        } else {
            // フォールバック：型名 + description で安定化を試みる。
            combine(byte: 103)
            combine(string: String(describing: type(of: anyHashable.base)))
            combine(string: String(describing: anyHashable.base))
        }
    }

    /// 現在の状態を 16 バイトに展開し `UUID` として返す。
    func finalizeAsUUID() -> UUID {
        var bytes: [UInt8] = []
        bytes.reserveCapacity(16)
        for shift in stride(from: 56, through: 0, by: -8) {
            bytes.append(UInt8((state0 >> shift) & 0xff))
        }
        for shift in stride(from: 56, through: 0, by: -8) {
            bytes.append(UInt8((state1 >> shift) & 0xff))
        }
        // RFC 4122 の version / variant bit を設定（v5 風）。
        var b = bytes
        b[6] = (b[6] & 0x0f) | 0x50 // version 5
        b[8] = (b[8] & 0x3f) | 0x80 // variant
        return UUID(uuid: (
            b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7],
            b[8], b[9], b[10], b[11], b[12], b[13], b[14], b[15]
        ))
    }
}

# Deviation: align-view-accessory-header-height

- Requirement「Section Header の固定高さは accessory 種別に依らず適用される」: spec では Header **領域の高さ**のみ規定 (hosted view の領域内配置は未規定) → review-001 Major の指摘を受けたオーナー裁定 (案1) により「固定高さが解決されたとき、hosted view は領域いっぱいに配置する (iOS の 4 辺 pin と対称)」を追加契約として実装する。理由: 本 change の目的 (OS 対称化) と承認モック状態 B の描画 (全面塗り) に一致させるため (2026-08-11)

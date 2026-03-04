# Shadow Proxy 詳細設計書 v2.1
Author: Shadow Proxy Design
Target Runtime: JVM (Java 21+ / Quarkus 想定)
Purpose: Development-phase API response comparison tool

---

# 1. ドキュメント概要

本書は Shadow Proxy v2.1 の**詳細設計書**である。
本ツールは既存 API (master) と開発中 API (shadow) のレスポンス差分を検出する開発者向け比較ツールである。

本書は以下を含む：

- アーキテクチャ設計
- コンポーネント分解（7コンポーネント）
- データフロー
- 並行制御設計
- JSON比較アルゴリズム
- メモリ戦略
- エラー処理仕様
- コンフィグ詳細
- ログ設計
- 非機能要件

---

# 2. システム概要

## 2.1 目的

- API migration の検証
- リファクタリング互換性確認
- Golden Master testing

## 2.2 利用フェーズ

- 開発初期〜中期
- 本番利用不可

理由：
- レスポンス遅延許容設計
- 高可用性・高耐障害性未考慮

---

# 3. 全体アーキテクチャ

```
Client
   |
   v
Shadow Proxy
   |--------------------|
   |                    |
   v                    v
Master API         Shadow API
```

---

# 4. コンポーネント設計（7 Components）

Shadow Proxy は以下の7つのコンポーネントに分解される。

---

## 4.1 RequestReceiver

責務:

- HTTPリクエスト受付
- RequestContext生成
- RequestId生成

入力:
- HTTP Request

出力:
- RequestContext

処理:

- 常に新しい `X-Request-Id` を生成
- クライアント提供IDは `X-Client-Request-Id` に保存
- ヘッダー透過コピー（Hop-by-hop除外）

---

## 4.2 ParallelDispatcher

責務:

- master / shadow への並列送信

実装方針:

- CompletableFuture / Structured Concurrency
- 最大60秒タイムアウト
- 両方完了まで待機

設定:

```
masterTimeout = 60s
shadowTimeout = 60s
```

---

## 4.3 UpstreamClient

責務:

- HTTP通信実行
- gzip解凍
- ボディ取得

処理仕様:

- Content-Encoding=gzip → 解凍
- ボディ最大10MB
- 超過時 TOO_LARGE

---

## 4.4 ComparisonLimiter

責務:

- 同時比較数制限

設定:

```
maxConcurrentComparisons = 50
```

実装:

- Semaphore(50)
- acquire失敗 → ERROR

---

## 4.5 JsonNormalizer

責務:

- JSON semantic normalization

ルール:

### Object

- キーソート

### Array

- 順序保持

### null vs missing

- 別値

### Canonical JSON

- 安定シリアライズ

---

## 4.6 JsonComparator

責務:

- 正規化後比較
- JSONPath diff生成

比較手順:

1. Status比較
2. Canonical JSON比較
3. 不一致ならJSONツリー再帰diff

出力:

```
List<DiffEntry>
{
  path,
  masterValue,
  shadowValue
}
```

ignorePaths適用:

- JSONPathマッチ除外

---

## 4.7 DiffReporter

責務:

- 標準出力出力

フォーマット:

- JSON Lines

例:

```json
{
  "requestId": "...",
  "result": "DIFF",
  "diff": [...]
}
```

---

# 5. データモデル

## 5.1 RequestContext

```
requestId
method
path
query
headers
startTime
```

---

## 5.2 UpstreamResponse

```
status
headers
body
latencyMs
```

---

## 5.3 ComparisonResult

```
result: SAME | DIFF | TIMEOUT | TOO_LARGE | ERROR
diffEntries[]
```

---

# 6. JSON比較アルゴリズム

擬似コード:

```
normalize(masterJson)
normalize(shadowJson)

if status !=:
   return DIFF

if canonical(master) == canonical(shadow):
   return SAME

return recursiveDiff(master, shadow)
```

再帰diff:

- 型が違う → diff
- Object → キー集合統合して比較
- Array → index単位比較

---

# 7. メモリ戦略

Worst case:

```
10MB × 2 × 50 ≈ 1GB
```

対策:

- 同時比較数制限
- ボディサイズ上限
- gzip解凍後サイズで判定

---

# 8. エラー処理

| ケース | result |
|--------|--------|
| master timeout | TIMEOUT |
| shadow timeout | TIMEOUT |
| body超過 | TOO_LARGE |
| semaphore失敗 | ERROR |
| JSON parse失敗 | ERROR |

クライアント応答:

- 常に master レスポンス

---

# 9. ログ設計

形式:

- JSON Lines

項目:

```
requestId
method
path
statusMaster
statusShadow
latencyMaster
latencyShadow
result
diff[]
```

---

# 10. コンフィグ詳細

```
proxy:
  upstream:
    master: URL
    shadow: URL

  timeout:
    master: duration
    shadow: duration

  body:
    maxBytes: long

  concurrency:
    maxComparisons: int

  compare:
    enabled: boolean
    arrayOrderSensitive: true
    nullVsMissingEqual: false
    ignorePaths: list

  requestId:
    header: X-Request-Id
```

---

# 11. 非機能要件

## 11.1 可用性

- Best effort
- 再試行なし

## 11.2 セキュリティ

- 本番用途不可
- PIIログ出力注意

## 11.3 性能

- 60秒待機許容
- 比較処理はCPU依存

---

# 12. 非目標

- WebSocket
- Streaming
- gRPC
- HTML比較
- Binary比較

---

# 13. 将来拡張候補

- 配列順序無視モード
- 差分閾値設定
- レポート出力先拡張（S3 / Slack）
- UIダッシュボード

---

# 14. 設計原則

- Developer-first
- Deterministic comparison
- Simplicity over completeness
- Full configurability

---

# 15. まとめ

本設計は

- 明確なコンポーネント分離
- シンプルなJSON semantic比較
- 開発初期用途への最適化

を目的としている。

本書は実装開始可能な粒度の設計を含む。

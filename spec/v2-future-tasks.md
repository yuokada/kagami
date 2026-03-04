# Shadow Proxy 改善点（優先度順・詳細付き）

---

## 🔴 P0（最優先：設計破綻を防ぐ）

### 1. 結果の分類を分離（DIFF と 比較不能を明確化）

現在の仕様では、API差分（DIFF）と環境起因の失敗（TIMEOUT / ERROR）が
同列に扱われる可能性がある。

開発者にとって重要なのは
「APIが壊れている」のか
「比較できなかっただけ」なのかの区別である。

改善案:
- `category` フィールドを導入
- DIFF / COMPARISON_ERROR / UPSTREAM_ERROR を分離
- レポート集計時にノイズを除去可能にする

---

### 2. 差分ログの爆発対策（重複抑制）

同一リクエストパターンで同一差分が大量発生すると、
ログが読めなくなり、開発者が本質的な問題を見失う。

特に CI や負荷テスト時に同一 DIFF が数百回出る可能性がある。

改善案:
- diff内容から `diffHash` を生成
- 一定時間内（例: 10秒）に同一hashが出たら抑制
- suppressカウントを別途出力する

---

### 3. Shadow失敗の独立分類

master成功・shadow失敗はAPI差分ではない。

しかし現在は TIMEOUT などが DIFF と同等に扱われる可能性がある。

改善案:
- SHADOW_TIMEOUT
- SHADOW_ERROR
- MASTER_TIMEOUT を明示的に分離
- API差分とは別の分析対象にする

---

## 🟡 P1（実用性を大きく上げる改善）

### 4. フルレスポンス保存オプション

diffだけでは原因分析が困難な場合がある。

開発初期では、実際のmaster/shadowレスポンス全文を
確認したいケースが頻発する。

改善案:
- `report.includeFullBody` フラグ
- サイズ上限付き（例: 100KB）
- デフォルトは false（ログ肥大防止）

---

### 5. 比較時間の可視化

現在は upstream latency しか記録されていない。

しかし、JSON正規化やdiff生成にもCPU時間がかかる。

改善案:
- comparisonLatencyMs を出力
- 性能劣化の原因を upstream か比較処理か切り分け可能にする

---

### 6. Diff件数上限

巨大JSONや構造的差分が多い場合、
diffエントリが数百〜数千件出る可能性がある。

これによりログが巨大化し、可読性が低下する。

改善案:
- `maxDiffEntries = 100`
- 超過時は truncated=true を付与
- 開発者が必要なら上限変更可能

---

## 🟢 P2（あると便利な拡張）

### 7. strict / relaxed モード

現在は strict モードのみ。

しかしAPIによっては
- null と missing を同値扱い
- 数値 1 と 1.0 を同値扱い
したいケースがある。

改善案:
- compare.mode = STRICT | RELAXED
- RELAXED は将来的拡張として定義

---

### 8. Health Endpoint

開発環境でもコンテナ起動確認や
CI での疎通チェックが必要。

改善案:
- GET /health
- upstream疎通は確認しない（軽量）

---

### 9. レスポンス返却モード切替

現在は master を常に返す。

しかし検証目的によっては
shadowの挙動をそのまま見たい場合がある。

改善案:
- responseMode = RETURN_MASTER（default）
- RETURN_SHADOW
- 実験用途として明示的に利用

---

# まとめ（現場投入前に入れるべき順）

P0 は必須。
P1 は早期導入推奨。
P2 は余力があれば。

この優先順位で進めると、
プロトタイプから実用ツールへ自然に進化できる。

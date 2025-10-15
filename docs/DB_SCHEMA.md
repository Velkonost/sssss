# DB Schema (PostgreSQL)

## Tables
- candles(symbol, interval, open_time, close_time, open, high, low, close, volume, source, inserted_at)
  - unique: (symbol, interval, open_time)
  - indexes: (symbol, interval, open_time)
- signals(source_type, source_id?, symbol, timeframe, signal_type, weight, confidence, payload_json, created_at)
  - indexes: (symbol, timeframe, created_at)
- aggregated_signals(symbol, timeframe, window_start, window_end, score, decision, basis_json, created_at)
  - unique: (symbol, timeframe, window_end)
  - indexes: (symbol, timeframe, window_end)
- analysis_results(symbol, timeframe, analysis_type, inputs_json, outputs_json, created_at)
  - indexes: (symbol, timeframe, analysis_type, created_at)
- audit_logs(event_type, ref_type, ref_id?, level, message, meta_json, created_at)
  - indexes: (ref_type, ref_id, created_at), (event_type, created_at)

## Config
- Env vars via `EnvSecureConfigProvider`:
  - `AIFB_POSTGRES_URL`, `AIFB_POSTGRES_USER`, `AIFB_POSTGRES_PASSWORD`

## Repositories (Exposed)
- CandlesRepository: batchInsertIgnore, findRange(symbol, interval, from, to)
- SignalsRepository: insert, find(symbol, timeframe, fromTs, toTs)
- AggregatedSignalsRepository: upsertIgnore(list), latest(symbol, timeframe)
- AnalysisResultsRepository: insert
- AuditLogRepository: log(eventType, refType, refId, level, message, metaJson)

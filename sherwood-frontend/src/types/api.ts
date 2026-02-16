export interface SarifResponse {
  id: string;
  filename: string;
  storageKey: string;
  downloadUrl: string;
  vendor: string;
  repository: string;
  identifier: string;
  created: string;
  updated: string;
}

export interface SarifResultResponse {
  id: string;
  sarifId: string;
  location: string;
  lineNumber: number;
  fingerprint?: string;
  snippet?: string;
  description?: string;
  ruleId: string;
  confidence: number;
  reachable: boolean;
  graph?: string;
  created: string;
  updated: string;
}

export interface SarifResultSimilarityResponse {
  matchingResultId: string;
  location: string;
  lineNumber: number;
  ruleId: string;
  vendor: string;
  similarity: number;
  reason: string;
}

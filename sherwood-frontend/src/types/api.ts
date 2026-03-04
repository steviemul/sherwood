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

export interface SimilarityScore {
  title: string;
  score: number;
  weight: number;
  available: boolean;
  additionalInformation: string;
}

export interface ResultSimilarityScore {
  availableScore: number;
  totalScore: number;
  reasons: SimilarityScore[];
}

export interface SarifResultSimilarityResponse {
  matchingResultId: string;
  sarifId: string;
  location: string;
  lineNumber: number;
  ruleId: string;
  vendor: string;
  similarity: ResultSimilarityScore;
  description?: string;
  snippet?: string;
}

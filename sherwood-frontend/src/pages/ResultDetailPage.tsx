import { useState, useEffect, useRef } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Card,
  CardContent,
  CircularProgress,
  Alert,
  Typography,
  Box,
  Breadcrumbs,
  Stack,
  Chip,
  Divider,
  Paper,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChevronDown } from '@fortawesome/free-solid-svg-icons';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus, vs } from 'react-syntax-highlighter/dist/esm/styles/prism';
import mermaid from 'mermaid';
import type { SarifResultResponse, SarifResultSimilarityResponse } from '../types/api';
import { useThemeContext } from '../context/ThemeContext';

export const ResultDetailPage = () => {
  const { sarifId, resultId } = useParams<{ sarifId: string; resultId: string }>();
  const { mode } = useThemeContext();
  const [result, setResult] = useState<SarifResultResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [mermaidSvg, setMermaidSvg] = useState<string | null>(null);
  const mermaidRef = useRef<HTMLDivElement>(null);
  
  // Similar results state
  const [similaritiesExpanded, setSimilaritiesExpanded] = useState(false);
  const [similarities, setSimilarities] = useState<SarifResultSimilarityResponse[]>([]);
  const [similaritiesLoading, setSimilaritiesLoading] = useState(false);
  const [similaritiesError, setSimilaritiesError] = useState<string | null>(null);
  const [similaritiesFetched, setSimilaritiesFetched] = useState(false);
  const [reasonDialogOpen, setReasonDialogOpen] = useState(false);
  const [selectedReason, setSelectedReason] = useState<string>('');

  useEffect(() => {
    const fetchResult = async () => {
      if (!sarifId || !resultId) return;

      try {
        setLoading(true);
        setError(null);
        const response = await fetch(`/api/sherwood/sarifs/${sarifId}/results/${resultId}`);
        if (!response.ok) {
          throw new Error(`Failed to fetch result: ${response.statusText}`);
        }
        const data = await response.json();
        setResult(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'An error occurred');
      } finally {
        setLoading(false);
      }
    };

    fetchResult();
  }, [sarifId, resultId]);

  const fetchSimilarities = async () => {
    if (!sarifId || !resultId || similaritiesFetched) return;

    try {
      setSimilaritiesLoading(true);
      setSimilaritiesError(null);
      const response = await fetch(`/api/sherwood/sarifs/${sarifId}/results/${resultId}/similarities`);
      if (!response.ok) {
        throw new Error(`Failed to fetch similarities: ${response.statusText}`);
      }
      const data = await response.json();
      setSimilarities(data);
      setSimilaritiesFetched(true);
    } catch (err) {
      setSimilaritiesError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setSimilaritiesLoading(false);
    }
  };

  const handleSimilaritiesAccordionChange = (_event: React.SyntheticEvent, expanded: boolean) => {
    setSimilaritiesExpanded(expanded);
    if (expanded && !similaritiesFetched) {
      fetchSimilarities();
    }
  };

  const handleSimilarityClick = (reason: string) => {
    setSelectedReason(reason);
    setReasonDialogOpen(true);
  };

  useEffect(() => {
    if (result?.graph) {
      try {
        // Base64 decode the mermaid diagram
        const decodedGraph = atob(result.graph);
        
        // Initialize mermaid
        mermaid.initialize({ 
          startOnLoad: false,
          theme: mode === 'dark' ? 'dark' : 'default',
        });

        // Render mermaid diagram
        const renderDiagram = async () => {
          try {
            const { svg } = await mermaid.render('mermaid-diagram', decodedGraph);
            setMermaidSvg(svg);
          } catch (err) {
            console.error('Failed to render mermaid diagram:', err);
          }
        };

        renderDiagram();
      } catch (err) {
        console.error('Failed to decode graph:', err);
      }
    }
  }, [result?.graph, mode]);

  const getLanguageFromLocation = (location: string): string => {
    const ext = location.split('.').pop()?.toLowerCase();
    const languageMap: Record<string, string> = {
      'js': 'javascript',
      'jsx': 'jsx',
      'ts': 'typescript',
      'tsx': 'tsx',
      'py': 'python',
      'java': 'java',
      'c': 'c',
      'cpp': 'cpp',
      'cs': 'csharp',
      'go': 'go',
      'rb': 'ruby',
      'php': 'php',
      'rs': 'rust',
      'swift': 'swift',
      'kt': 'kotlin',
    };
    return languageMap[ext || ''] || 'text';
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        {error}
      </Alert>
    );
  }

  if (!result) {
    return (
      <Alert severity="warning">
        Result not found
      </Alert>
    );
  }

  return (
    <Box>
      <Breadcrumbs aria-label="breadcrumb" sx={{ mb: 3 }}>
        <Link to="/" style={{ textDecoration: 'none', color: 'inherit' }}>
          SARIF Files
        </Link>
        <Link
          to={`/sarifs/${sarifId}/results`}
          style={{ textDecoration: 'none', color: 'inherit' }}
        >
          Results
        </Link>
        <Typography color="text.primary">Detail</Typography>
      </Breadcrumbs>

      <Typography variant="h4" component="h1" gutterBottom>
        Result Detail
      </Typography>

      <Stack spacing={3}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Basic Information
            </Typography>
            <Divider sx={{ mb: 2 }} />
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                gap: 2,
              }}
            >
              <Box>
                <Typography variant="body2" color="text.secondary">
                  ID
                </Typography>
                <Typography variant="body1">{result.id}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  SARIF
                </Typography>
                <Link
                  to={`/sarifs/${result.sarifId}/results`}
                  style={{ textDecoration: 'none' }}
                >
                  {result.sarifId}
                </Link>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Location
                </Typography>
                <Typography variant="body1">{result.location}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Line Number
                </Typography>
                <Typography variant="body1">{result.lineNumber}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Rule ID
                </Typography>
                <Typography variant="body1">{result.ruleId}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Confidence
                </Typography>
                <Typography variant="body1">{result.confidence.toFixed(2)}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Reachable
                </Typography>
                <Chip
                  label={result.reachable ? 'Yes' : 'No'}
                  color={result.reachable ? 'success' : 'default'}
                  size="small"
                />
              </Box>
              {result.fingerprint && (
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Fingerprint
                  </Typography>
                  <Typography variant="body1" sx={{ wordBreak: 'break-all' }}>
                    {result.fingerprint}
                  </Typography>
                </Box>
              )}
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Created
                </Typography>
                <Typography variant="body1">
                  {new Date(result.created).toLocaleString()}
                </Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Updated
                </Typography>
                <Typography variant="body1">
                  {new Date(result.updated).toLocaleString()}
                </Typography>
              </Box>
            </Box>
          </CardContent>
        </Card>

        {/* Similar Results Section */}
        <Accordion 
          expanded={similaritiesExpanded} 
          onChange={handleSimilaritiesAccordionChange}
        >
          <AccordionSummary
            expandIcon={<FontAwesomeIcon icon={faChevronDown} />}
            aria-controls="similar-results-content"
            id="similar-results-header"
          >
            <Typography variant="h6">Similar Results</Typography>
          </AccordionSummary>
          <AccordionDetails>
            {similaritiesLoading && (
              <Box display="flex" justifyContent="center" py={3}>
                <CircularProgress />
              </Box>
            )}
            {similaritiesError && (
              <Alert severity="error">{similaritiesError}</Alert>
            )}
            {!similaritiesLoading && !similaritiesError && similarities.length === 0 && (
              <Typography variant="body2" color="text.secondary" align="center" py={2}>
                No similar results found
              </Typography>
            )}
            {!similaritiesLoading && !similaritiesError && similarities.length > 0 && (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>ID</TableCell>
                      <TableCell>Location</TableCell>
                      <TableCell>Line Number</TableCell>
                      <TableCell>Rule ID</TableCell>
                      <TableCell>Similarity</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {similarities.map((similarity, index) => (
                      <TableRow
                        key={similarity.matchingResultId}
                        hover
                        sx={{ backgroundColor: index % 2 === 0 ? 'action.hover' : 'inherit' }}
                      >
                        <TableCell>
                          <Link
                            to={`/sarifs/${sarifId}/results/${similarity.matchingResultId}`}
                            style={{ textDecoration: 'none', color: 'inherit', fontWeight: 'bold' }}
                          >
                            {similarity.matchingResultId.substring(0, 8)}...
                          </Link>
                        </TableCell>
                        <TableCell>{similarity.location}</TableCell>
                        <TableCell>{similarity.lineNumber}</TableCell>
                        <TableCell>{similarity.ruleId}</TableCell>
                        <TableCell>
                          <Button
                            size="small"
                            onClick={() => handleSimilarityClick(similarity.reason)}
                            sx={{ textTransform: 'none' }}
                          >
                            {(similarity.similarity * 100).toFixed(0)}%
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </AccordionDetails>
        </Accordion>

        {result.description && (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Description
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>
                {result.description}
              </Typography>
            </CardContent>
          </Card>
        )}

        {result.snippet && (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Code Snippet
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Box sx={{ overflow: 'auto' }}>
                <SyntaxHighlighter
                  language={getLanguageFromLocation(result.location)}
                  style={mode === 'dark' ? vscDarkPlus : vs}
                  showLineNumbers
                >
                  {result.snippet}
                </SyntaxHighlighter>
              </Box>
            </CardContent>
          </Card>
        )}

        {result.graph && mermaidSvg && (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Code Path Graph
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Paper
                elevation={0}
                sx={{
                  p: 2,
                  backgroundColor: mode === 'dark' ? 'grey.900' : 'grey.50',
                  overflow: 'auto',
                }}
              >
                <Box
                  ref={mermaidRef}
                  dangerouslySetInnerHTML={{ __html: mermaidSvg }}
                />
              </Paper>
            </CardContent>
          </Card>
        )}
      </Stack>

      {/* Reason Dialog */}
      <Dialog 
        open={reasonDialogOpen} 
        onClose={() => setReasonDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Similarity Reason</DialogTitle>
        <DialogContent>
          <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap', mt: 1 }}>
            {selectedReason}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReasonDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};


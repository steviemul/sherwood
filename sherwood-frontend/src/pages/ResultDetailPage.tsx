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
  IconButton,
  Tooltip,
} from '@mui/material';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChevronDown } from '@fortawesome/free-solid-svg-icons';
import ZoomInIcon from '@mui/icons-material/ZoomIn';
import ZoomOutIcon from '@mui/icons-material/ZoomOut';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import CompareArrowsIcon from '@mui/icons-material/CompareArrows';
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch';
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
  
  // Comparison state
  const [comparisonDialogOpen, setComparisonDialogOpen] = useState(false);
  const [selectedSimilarity, setSelectedSimilarity] = useState<SarifResultSimilarityResponse | null>(null);
  const [matchingResult, setMatchingResult] = useState<SarifResultResponse | null>(null);
  const [matchingResultLoading, setMatchingResultLoading] = useState(false);
  const [matchingResultError, setMatchingResultError] = useState<string | null>(null);

  useEffect(() => {
    // Reset similarities state when navigating to a different result
    setSimilarities([]);
    setSimilaritiesExpanded(false);
    setSimilaritiesFetched(false);
    setSimilaritiesError(null);
    
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

  const handleCompareClick = async (similarity: SarifResultSimilarityResponse) => {
    setSelectedSimilarity(similarity);
    setComparisonDialogOpen(true);
    setMatchingResult(null);
    setMatchingResultError(null);
    
    try {
      setMatchingResultLoading(true);
      const response = await fetch(`/api/sherwood/sarifs/${sarifId}/results/${similarity.matchingResultId}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch result: ${response.statusText}`);
      }
      const data = await response.json();
      setMatchingResult(data);
    } catch (err) {
      setMatchingResultError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setMatchingResultLoading(false);
    }
  };

  const handleComparisonDialogClose = () => {
    setComparisonDialogOpen(false);
    setSelectedSimilarity(null);
    setMatchingResult(null);
    setMatchingResultError(null);
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
                      <TableCell></TableCell>
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
                        <TableCell>
                          <Tooltip title="Compare" placement="left">
                            <IconButton
                              size="small"
                              onClick={() => handleCompareClick(similarity)}
                              color="primary"
                            >
                              <CompareArrowsIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
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
                  position: 'relative',
                  backgroundColor: mode === 'dark' ? 'grey.900' : 'grey.50',
                  overflow: 'hidden',
                  height: '600px',
                }}
              >
                <TransformWrapper
                  initialScale={1.5}
                  minScale={0.3}
                  maxScale={4}
                  wheel={{ step: 0.1 }}
                  doubleClick={{ mode: 'reset' }}
                  panning={{ disabled: false }}
                >
                  {({ zoomIn, zoomOut, resetTransform }) => (
                    <>
                      {/* Zoom Control Buttons */}
                      <Box
                        sx={{
                          position: 'absolute',
                          top: 8,
                          right: 8,
                          display: 'flex',
                          flexDirection: 'column',
                          gap: 1,
                          zIndex: 1,
                        }}
                      >
                        <Tooltip title="Zoom In" placement="left">
                          <IconButton
                            size="small"
                            onClick={() => zoomIn()}
                            sx={{
                              backgroundColor: mode === 'dark' ? 'grey.800' : 'white',
                              '&:hover': {
                                backgroundColor: mode === 'dark' ? 'grey.700' : 'grey.100',
                              },
                              boxShadow: 1,
                            }}
                          >
                            <ZoomInIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Zoom Out" placement="left">
                          <IconButton
                            size="small"
                            onClick={() => zoomOut()}
                            sx={{
                              backgroundColor: mode === 'dark' ? 'grey.800' : 'white',
                              '&:hover': {
                                backgroundColor: mode === 'dark' ? 'grey.700' : 'grey.100',
                              },
                              boxShadow: 1,
                            }}
                          >
                            <ZoomOutIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Reset Zoom" placement="left">
                          <IconButton
                            size="small"
                            onClick={() => resetTransform()}
                            sx={{
                              backgroundColor: mode === 'dark' ? 'grey.800' : 'white',
                              '&:hover': {
                                backgroundColor: mode === 'dark' ? 'grey.700' : 'grey.100',
                              },
                              boxShadow: 1,
                            }}
                          >
                            <RestartAltIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Box>

                      {/* Diagram Container */}
                      <TransformComponent
                        wrapperStyle={{
                          width: '100%',
                          height: '100%',
                          cursor: 'grab',
                        }}
                      >
                        <Box
                          ref={mermaidRef}
                          dangerouslySetInnerHTML={{ __html: mermaidSvg }}
                          sx={{
                            display: 'inline-block',
                            '& svg': {
                              display: 'block',
                              maxWidth: '100%',
                              height: 'auto',
                            },
                          }}
                        />
                      </TransformComponent>
                    </>
                  )}
                </TransformWrapper>
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

      {/* Comparison Dialog */}
      <Dialog 
        open={comparisonDialogOpen} 
        onClose={handleComparisonDialogClose}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>Result Comparison</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            {/* Side-by-side comparison */}
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
                gap: 3,
                mb: 3,
              }}
            >
              {/* Main Result (Left) */}
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="h6" gutterBottom color="primary">
                    Main Result
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  <Stack spacing={2}>
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Rule ID
                      </Typography>
                      <Typography variant="body1">{result?.ruleId || 'N/A'}</Typography>
                    </Box>
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Location
                      </Typography>
                      <Typography variant="body1">{result?.location || 'N/A'}</Typography>
                    </Box>
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Line Number
                      </Typography>
                      <Typography variant="body1">{result?.lineNumber ?? 'N/A'}</Typography>
                    </Box>
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Description
                      </Typography>
                      <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>
                        {result?.description || 'N/A'}
                      </Typography>
                    </Box>
                    {result?.snippet && (
                      <Box>
                        <Typography variant="body2" color="text.secondary" gutterBottom>
                          Snippet
                        </Typography>
                        <Box sx={{ overflow: 'auto', maxHeight: '300px' }}>
                          <SyntaxHighlighter
                            language={getLanguageFromLocation(result.location)}
                            style={mode === 'dark' ? vscDarkPlus : vs}
                            showLineNumbers
                            customStyle={{ fontSize: '0.85rem' }}
                          >
                            {result.snippet}
                          </SyntaxHighlighter>
                        </Box>
                      </Box>
                    )}
                    {!result?.snippet && (
                      <Box>
                        <Typography variant="body2" color="text.secondary">
                          Snippet
                        </Typography>
                        <Typography variant="body1">N/A</Typography>
                      </Box>
                    )}
                  </Stack>
                </CardContent>
              </Card>

              {/* Matching Result (Right) */}
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="h6" gutterBottom color="secondary">
                    Similar Result
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  {matchingResultLoading && (
                    <Box display="flex" justifyContent="center" py={3}>
                      <CircularProgress />
                    </Box>
                  )}
                  {matchingResultError && (
                    <Alert severity="error">{matchingResultError}</Alert>
                  )}
                  {!matchingResultLoading && !matchingResultError && matchingResult && (
                    <Stack spacing={2}>
                      <Box>
                        <Typography variant="body2" color="text.secondary">
                          Rule ID
                        </Typography>
                        <Typography variant="body1">{matchingResult.ruleId || 'N/A'}</Typography>
                      </Box>
                      <Box>
                        <Typography variant="body2" color="text.secondary">
                          Location
                        </Typography>
                        <Typography variant="body1">{matchingResult.location || 'N/A'}</Typography>
                      </Box>
                      <Box>
                        <Typography variant="body2" color="text.secondary">
                          Line Number
                        </Typography>
                        <Typography variant="body1">{matchingResult.lineNumber ?? 'N/A'}</Typography>
                      </Box>
                      <Box>
                        <Typography variant="body2" color="text.secondary">
                          Description
                        </Typography>
                        <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>
                          {matchingResult.description || 'N/A'}
                        </Typography>
                      </Box>
                      {matchingResult.snippet && (
                        <Box>
                          <Typography variant="body2" color="text.secondary" gutterBottom>
                            Snippet
                          </Typography>
                          <Box sx={{ overflow: 'auto', maxHeight: '300px' }}>
                            <SyntaxHighlighter
                              language={getLanguageFromLocation(matchingResult.location)}
                              style={mode === 'dark' ? vscDarkPlus : vs}
                              showLineNumbers
                              customStyle={{ fontSize: '0.85rem' }}
                            >
                              {matchingResult.snippet}
                            </SyntaxHighlighter>
                          </Box>
                        </Box>
                      )}
                      {!matchingResult.snippet && (
                        <Box>
                          <Typography variant="body2" color="text.secondary">
                            Snippet
                          </Typography>
                          <Typography variant="body1">N/A</Typography>
                        </Box>
                      )}
                    </Stack>
                  )}
                </CardContent>
              </Card>
            </Box>

            {/* Similarity and Reason Section */}
            {selectedSimilarity && (
              <Card variant="outlined" sx={{ backgroundColor: 'action.hover' }}>
                <CardContent>
                  <Stack spacing={2}>
                    <Box>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        Similarity Score
                      </Typography>
                      <Chip 
                        label={`${(selectedSimilarity.similarity * 100).toFixed(0)}%`}
                        color="primary"
                        size="medium"
                      />
                    </Box>
                    <Divider />
                    <Box>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        Similarity Reason
                      </Typography>
                      <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>
                        {selectedSimilarity.reason}
                      </Typography>
                    </Box>
                  </Stack>
                </CardContent>
              </Card>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleComparisonDialogClose}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};


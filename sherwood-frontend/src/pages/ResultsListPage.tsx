import { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  Alert,
  Typography,
  Box,
  TableSortLabel,
  Breadcrumbs,
  Chip,
  TextField,
  IconButton,
  InputAdornment,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import CloseIcon from '@mui/icons-material/Close';
import type { SarifResultResponse } from '../types/api';

type SortField = 'location' | 'confidence' | 'reachable' | 'created' | 'updated' | 'ruleId';
type SortOrder = 'asc' | 'desc';

export const ResultsListPage = () => {
  const { sarifId } = useParams<{ sarifId: string }>();
  const [results, setResults] = useState<SarifResultResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>('created');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [locationFilterVisible, setLocationFilterVisible] = useState(false);
  const [ruleIdFilterVisible, setRuleIdFilterVisible] = useState(false);
  const [locationFilter, setLocationFilter] = useState('');
  const [ruleIdFilter, setRuleIdFilter] = useState('');

  useEffect(() => {
    const fetchResults = async () => {
      if (!sarifId) return;
      
      try {
        setLoading(true);
        setError(null);
        const response = await fetch(`/api/sherwood/sarifs/${sarifId}/results`);
        if (!response.ok) {
          throw new Error(`Failed to fetch results: ${response.statusText}`);
        }
        const data = await response.json();
        setResults(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'An error occurred');
      } finally {
        setLoading(false);
      }
    };

    fetchResults();
  }, [sarifId]);

  const handleSort = (field: SortField) => {
    const isAsc = sortField === field && sortOrder === 'asc';
    setSortOrder(isAsc ? 'desc' : 'asc');
    setSortField(field);
  };

  const filteredResults = results.filter(result => {
    const matchesLocation = result.location.toLowerCase().includes(locationFilter.toLowerCase());
    const matchesRuleId = result.ruleId.toLowerCase().includes(ruleIdFilter.toLowerCase());
    return matchesLocation && matchesRuleId;
  });

  const sortedResults = [...filteredResults].sort((a, b) => {
    let aValue: string | number | boolean = a[sortField];
    let bValue: string | number | boolean = b[sortField];

    if (typeof aValue === 'string' && typeof bValue === 'string') {
      return sortOrder === 'asc' 
        ? aValue.localeCompare(bValue)
        : bValue.localeCompare(aValue);
    }

    if (typeof aValue === 'number' && typeof bValue === 'number') {
      return sortOrder === 'asc' ? aValue - bValue : bValue - aValue;
    }

    if (typeof aValue === 'boolean' && typeof bValue === 'boolean') {
      return sortOrder === 'asc' 
        ? (aValue === bValue ? 0 : aValue ? 1 : -1)
        : (aValue === bValue ? 0 : aValue ? -1 : 1);
    }

    return 0;
  });

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

  return (
    <Box>
      <Breadcrumbs aria-label="breadcrumb" sx={{ mb: 3 }}>
        <Link to="/" style={{ textDecoration: 'none', color: 'inherit' }}>
          SARIF Files
        </Link>
        <Typography color="text.primary">Results</Typography>
      </Breadcrumbs>

      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1">
          Results for SARIF {sarifId?.substring(0, 8)}...
        </Typography>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>
                <Box display="flex" alignItems="center" gap={0.5}>
                  <TableSortLabel
                    active={sortField === 'location'}
                    direction={sortField === 'location' ? sortOrder : 'asc'}
                    onClick={() => handleSort('location')}
                  >
                    Location
                  </TableSortLabel>
                  <IconButton
                    size="small"
                    onClick={() => setLocationFilterVisible(!locationFilterVisible)}
                    color={locationFilterVisible ? 'primary' : 'default'}
                    aria-label="toggle location filter"
                  >
                    <SearchIcon fontSize="small" />
                  </IconButton>
                </Box>
              </TableCell>
              <TableCell>Line Number</TableCell>
              <TableCell>
                <Box display="flex" alignItems="center" gap={0.5}>
                  <TableSortLabel
                    active={sortField === 'ruleId'}
                    direction={sortField === 'ruleId' ? sortOrder : 'asc'}
                    onClick={() => handleSort('ruleId')}
                  >
                    Rule ID
                  </TableSortLabel>
                  <IconButton
                    size="small"
                    onClick={() => setRuleIdFilterVisible(!ruleIdFilterVisible)}
                    color={ruleIdFilterVisible ? 'primary' : 'default'}
                    aria-label="toggle rule id filter"
                  >
                    <SearchIcon fontSize="small" />
                  </IconButton>
                </Box>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'confidence'}
                  direction={sortField === 'confidence' ? sortOrder : 'asc'}
                  onClick={() => handleSort('confidence')}
                >
                  Confidence
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'reachable'}
                  direction={sortField === 'reachable' ? sortOrder : 'asc'}
                  onClick={() => handleSort('reachable')}
                >
                  Reachable
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'created'}
                  direction={sortField === 'created' ? sortOrder : 'asc'}
                  onClick={() => handleSort('created')}
                >
                  Created
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'updated'}
                  direction={sortField === 'updated' ? sortOrder : 'asc'}
                  onClick={() => handleSort('updated')}
                >
                  Updated
                </TableSortLabel>
              </TableCell>
            </TableRow>
            {(locationFilterVisible || ruleIdFilterVisible) && (
              <TableRow>
                <TableCell />
                <TableCell>
                  {locationFilterVisible && (
                    <TextField
                      size="small"
                      placeholder="Filter location..."
                      value={locationFilter}
                      onChange={(e) => setLocationFilter(e.target.value)}
                      fullWidth
                      InputProps={{
                        endAdornment: locationFilter && (
                          <InputAdornment position="end">
                            <IconButton
                              size="small"
                              onClick={() => setLocationFilter('')}
                              edge="end"
                            >
                              <CloseIcon fontSize="small" />
                            </IconButton>
                          </InputAdornment>
                        ),
                      }}
                    />
                  )}
                </TableCell>
                <TableCell />
                <TableCell>
                  {ruleIdFilterVisible && (
                    <TextField
                      size="small"
                      placeholder="Filter rule ID..."
                      value={ruleIdFilter}
                      onChange={(e) => setRuleIdFilter(e.target.value)}
                      fullWidth
                      InputProps={{
                        endAdornment: ruleIdFilter && (
                          <InputAdornment position="end">
                            <IconButton
                              size="small"
                              onClick={() => setRuleIdFilter('')}
                              edge="end"
                            >
                              <CloseIcon fontSize="small" />
                            </IconButton>
                          </InputAdornment>
                        ),
                      }}
                    />
                  )}
                </TableCell>
                <TableCell />
                <TableCell />
                <TableCell />
                <TableCell />
              </TableRow>
            )}
          </TableHead>
          <TableBody>
            {sortedResults.map((result, index) => (
              <TableRow
                key={result.id}
                hover
                sx={{ backgroundColor: index % 2 === 0 ? 'action.hover' : 'inherit' }}
              >
                <TableCell>
                  <Link
                    to={`/sarifs/${sarifId}/results/${result.id}`}
                    style={{ textDecoration: 'none', color: 'inherit', fontWeight: 'bold' }}
                  >
                    {result.id.substring(0, 8)}...
                  </Link>
                </TableCell>
                <TableCell>{result.location}</TableCell>
                <TableCell>{result.lineNumber}</TableCell>
                <TableCell>{result.ruleId}</TableCell>
                <TableCell>{result.confidence.toFixed(2)}</TableCell>
                <TableCell>
                  <Chip
                    label={result.reachable ? 'Yes' : 'No'}
                    color={result.reachable ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell>{new Date(result.created).toLocaleString()}</TableCell>
                <TableCell>{new Date(result.updated).toLocaleString()}</TableCell>
              </TableRow>
            ))}
            {sortedResults.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  <Typography variant="body2" color="text.secondary">
                    {locationFilter || ruleIdFilter
                      ? 'No results match the current filters.'
                      : 'No results found for this SARIF file.'}
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};


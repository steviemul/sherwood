import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Alert,
  Typography,
  Box,
  TableSortLabel,
  IconButton,
  Snackbar,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUpload } from '@fortawesome/free-solid-svg-icons';
import type { SarifResponse } from '../types/api';

type SortField = 'vendor' | 'repository' | 'created';
type SortOrder = 'asc' | 'desc';

export const SarifListPage = () => {
  const [sarifs, setSarifs] = useState<SarifResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>('created');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');

  const fetchSarifs = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch('/api/sherwood/sarifs');
      if (!response.ok) {
        throw new Error(`Failed to fetch sarifs: ${response.statusText}`);
      }
      const data = await response.json();
      setSarifs(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSarifs();
  }, []);

  const handleUploadClick = () => {
    setUploadDialogOpen(true);
    setSelectedFile(null);
    setUploadError(null);
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] || null;
    setSelectedFile(file);
    setUploadError(null);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setUploadError('Please select a file');
      return;
    }

    try {
      setUploading(true);
      setUploadError(null);
      
      const formData = new FormData();
      formData.append('sarif', selectedFile);

      const response = await fetch('/api/sherwood/sarifs', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`Upload failed: ${response.statusText}`);
      }

      setUploadDialogOpen(false);
      setSelectedFile(null);
      await fetchSarifs();
    } catch (err) {
      setUploadError(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (sarifId: string) => {
    try {
      const response = await fetch(`/api/sherwood/sarifs/${sarifId}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error(`Delete failed: ${response.statusText}`);
      }

      setSnackbarMessage('Sarif deleted successfully');
      setSnackbarOpen(true);
      await fetchSarifs();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    }
  };

  const handleSort = (field: SortField) => {
    const isAsc = sortField === field && sortOrder === 'asc';
    setSortOrder(isAsc ? 'desc' : 'asc');
    setSortField(field);
  };

  const sortedSarifs = [...sarifs].sort((a, b) => {
    const aValue = a[sortField];
    const bValue = b[sortField];
    
    if (aValue < bValue) {
      return sortOrder === 'asc' ? -1 : 1;
    }
    if (aValue > bValue) {
      return sortOrder === 'asc' ? 1 : -1;
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
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1">
          SARIF Files
        </Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<FontAwesomeIcon icon={faUpload} />}
          onClick={handleUploadClick}
        >
          Upload SARIF
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Filename</TableCell>
              <TableCell>Storage Key</TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'vendor'}
                  direction={sortField === 'vendor' ? sortOrder : 'asc'}
                  onClick={() => handleSort('vendor')}
                >
                  Vendor
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'repository'}
                  direction={sortField === 'repository' ? sortOrder : 'asc'}
                  onClick={() => handleSort('repository')}
                >
                  Repository
                </TableSortLabel>
              </TableCell>
              <TableCell>Identifier</TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'created'}
                  direction={sortField === 'created' ? sortOrder : 'asc'}
                  onClick={() => handleSort('created')}
                >
                  Created
                </TableSortLabel>
              </TableCell>
              <TableCell>Updated</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {sortedSarifs.map((sarif, index) => (
              <TableRow
                key={sarif.id}
                hover
                sx={{ backgroundColor: index % 2 === 0 ? 'action.hover' : 'inherit' }}
              >
                <TableCell>
                  <Link
                    to={`/sarifs/${sarif.id}/results`}
                    style={{ textDecoration: 'none', color: 'inherit', fontWeight: 'bold' }}
                  >
                    {sarif.id.substring(0, 8)}...
                  </Link>
                </TableCell>
                <TableCell>{sarif.filename}</TableCell>
                <TableCell>
                  <a
                    href={sarif.downloadUrl}
                    download={sarif.filename}
                    style={{ textDecoration: 'none', color: 'inherit', fontWeight: 'bold' }}
                  >
                    {sarif.storageKey}
                  </a>
                </TableCell>
                <TableCell>{sarif.vendor}</TableCell>
                <TableCell>{sarif.repository}</TableCell>
                <TableCell>{sarif.identifier}</TableCell>
                <TableCell>{new Date(sarif.created).toLocaleString()}</TableCell>
                <TableCell>{new Date(sarif.updated).toLocaleString()}</TableCell>
                <TableCell>
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => handleDelete(sarif.id)}
                    aria-label="delete"
                  >
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
            {sortedSarifs.length === 0 && (
              <TableRow>
                <TableCell colSpan={9} align="center">
                  <Typography variant="body2" color="text.secondary">
                    No SARIF files found. Upload one to get started.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={uploadDialogOpen} onClose={() => setUploadDialogOpen(false)}>
        <DialogTitle>Upload SARIF File</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2 }}>
            <input
              type="file"
              accept=".json,.sarif"
              onChange={handleFileChange}
              style={{ display: 'block', marginBottom: '16px' }}
            />
            {selectedFile && (
              <Typography variant="body2" color="text.secondary">
                Selected: {selectedFile.name}
              </Typography>
            )}
            {uploadError && (
              <Alert severity="error" sx={{ mt: 2 }}>
                {uploadError}
              </Alert>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)} disabled={uploading}>
            Cancel
          </Button>
          <Button
            onClick={handleUpload}
            variant="contained"
            disabled={!selectedFile || uploading}
          >
            {uploading ? <CircularProgress size={24} /> : 'Upload'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbarOpen}
        autoHideDuration={4000}
        onClose={() => setSnackbarOpen(false)}
        message={snackbarMessage}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />
    </Box>
  );
};


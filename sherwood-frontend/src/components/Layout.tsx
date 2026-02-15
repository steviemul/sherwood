import type { ReactNode } from 'react';
import { AppBar, Toolbar, Typography, IconButton, Container, Box } from '@mui/material';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faMoon, faSun } from '@fortawesome/free-solid-svg-icons';
import { useThemeContext } from '../context/ThemeContext';

interface LayoutProps {
  children: ReactNode;
}

export const Layout = ({ children }: LayoutProps) => {
  const { mode, toggleTheme } = useThemeContext();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Sherwood SARIF Viewer
          </Typography>
          <IconButton color="inherit" onClick={toggleTheme} aria-label="toggle theme">
            <FontAwesomeIcon icon={mode === 'light' ? faMoon : faSun} />
          </IconButton>
        </Toolbar>
      </AppBar>
      <Container maxWidth="xl" sx={{ mt: 4, mb: 4, flex: 1 }}>
        {children}
      </Container>
    </Box>
  );
};

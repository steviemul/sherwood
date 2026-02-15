import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import { ThemeContextProvider, useThemeContext } from './context/ThemeContext';
import App from './App';

const ThemedApp = () => {
  const { mode } = useThemeContext();
  
  const theme = createTheme({
    palette: {
      mode,
    },
  });

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <App />
    </ThemeProvider>
  );
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeContextProvider>
      <ThemedApp />
    </ThemeContextProvider>
  </StrictMode>
);


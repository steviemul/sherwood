import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { SarifListPage } from './pages/SarifListPage';
import { ResultsListPage } from './pages/ResultsListPage';
import { ResultDetailPage } from './pages/ResultDetailPage';

function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<SarifListPage />} />
          <Route path="/sarifs/:sarifId/results" element={<ResultsListPage />} />
          <Route path="/sarifs/:sarifId/results/:resultId" element={<ResultDetailPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}

export default App;


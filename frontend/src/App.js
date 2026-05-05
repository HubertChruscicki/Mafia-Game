import './App.css';
import MainLayout from './layouts/mainLayout/MainLayout';
import StartView from './views/startView/StartView';

function App() {
  return (
    <div className="app">
      <MainLayout>
        <StartView />
      </MainLayout>
    </div>
  );
}

export default App;

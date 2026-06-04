import Header from '../../components/header/Header';
import Footer from '../../components/footer/Footer';
import './MainLayout.css';

function MainLayout({ children }) {
  return (
    <div className="main-layout">
      <Header />
      <main className="main-layout__content">{children}</main>
      <Footer />
    </div>
  );
}

export default MainLayout;

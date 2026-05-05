import '../common/ViewShell.css';

function DashboardView() {
  return (
    <main className="view-shell">
      <section className="view-shell__card">
        <h1 className="view-shell__title">Dashboard</h1>
        <p className="view-shell__description">
          Panel glowny zostanie rozbudowany o liste pokojow i akcje gracza.
        </p>
      </section>
    </main>
  );
}

export default DashboardView;

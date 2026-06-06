import UserManagementTable from '../../components/userManagementTable/UserManagementTable';
import './AdminPanelView.css';

function AdminPanelView() {
  return (
    <div className="admin-panel">
      <h1 className="admin-panel__title">Admin Panel — User Management</h1>
      <UserManagementTable />
    </div>
  );
}

export default AdminPanelView;

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, qs } from '../api';
import type { AuditEntry, Page } from '../types';
import { Empty, ErrorBox, Pager, Spinner, StatusPill, fmtDate, useLoad } from '../ui';

export default function Audit() {
  const [page, setPage] = useState(0);
  const { data, error, loading } = useLoad(() => api<Page<AuditEntry>>('/api/audit' + qs({ page, size: 25 })), [page]);
  return (
    <>
      <div className="page-h"><h1>Audit log</h1><span className="muted small">Every status change across all work orders (append-only)</span></div>
      <ErrorBox err={error} />{loading && !data && <Spinner />}
      {data && (data.content.length === 0 ? <div className="card"><Empty title="No activity yet" /></div> : (
        <div className="card" style={{ overflowX: 'auto' }}><table><thead><tr><th>When</th><th>Work order</th><th>Change</th><th>By</th><th>Note</th></tr></thead><tbody>
          {data.content.map(a => (
            <tr key={a.id}><td>{fmtDate(a.changedAt)}</td>
              <td><Link to={`/work-orders/${a.workOrderId}`}>{a.workOrderCode}</Link><br /><span className="muted small">{a.workOrderTitle}</span></td>
              <td>{a.fromStatus && a.fromStatus !== a.toStatus ? <><StatusPill s={a.fromStatus} /> → </> : a.fromStatus ? 'Reassigned · ' : 'Created · '}<StatusPill s={a.toStatus} /></td>
              <td>{a.changedBy}</td><td>{a.note ?? ''}</td></tr>))}</tbody></table></div>))}
      {data && <Pager page={data} onPage={setPage} />}
    </>
  );
}

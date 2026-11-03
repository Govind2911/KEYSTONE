export type Role = 'DISPATCHER' | 'TECHNICIAN' | 'MANAGER' | 'CUSTOMER';
export type Status = 'NEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CLOSED' | 'CANCELLED';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type Sla = 'ON_TRACK' | 'AT_RISK' | 'BREACHED' | 'MET' | 'NONE';
export type Category = 'HVAC' | 'ELECTRICAL' | 'PLUMBING' | 'GENERAL';
export const CATEGORIES: Category[] = ['GENERAL', 'HVAC', 'ELECTRICAL', 'PLUMBING'];
export const STATUSES: Status[] = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CLOSED', 'CANCELLED'];
export const BOARD: Status[] = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED'];
export const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export interface User { id: number; name: string; email: string; phone?: string; role: Role; customerId?: number; customerName?: string; active: boolean; createdAt: string; skills?: string }
export interface LoginResponse { token: string; tokenType: string; expiresAt: string; user: User }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface Ref { id: number; name: string }
export interface History { id: number; fromStatus?: Status; toStatus: Status; changedBy?: string; changedAt: string; note?: string }
export interface WorkOrder {
  id: number; code: string; title: string; description: string; priority: Priority; status: Status; category: Category;
  customer: Ref; site: Ref; siteAddress: string; assignee?: Ref; createdBy?: Ref;
  createdAt: string; updatedAt: string; scheduledAt?: string; startedAt?: string; completedAt?: string; closedAt?: string;
  slaDueAt?: string; slaStatus?: Sla; slaPercentElapsed?: number; slaBreached?: boolean;
  partsCost?: number; labourMinutes?: number; history?: History[];
  allowedTransitions?: Status[]; canAssign?: boolean; canEdit?: boolean;
}
export interface Customer { id: number; name: string; contactName?: string; contactEmail?: string; contactPhone?: string; address?: string; active: boolean; siteCount: number }
export interface Site { id: number; customerId: number; customerName: string; name: string; addressLine: string; city: string; state?: string; postalCode?: string; country?: string }
export interface Part { id: number; sku: string; name: string; description?: string; unitCost: number; stockQty: number }
export interface PartUsage { id: number; partId: number; sku: string; partName: string; quantity: number; unitCost: number; lineCost: number; usedBy: string; usedAt: string; note?: string }
export interface TimeLog { id: number; technicianName: string; minutes: number; note?: string; loggedAt: string }
export interface Attachment { id: number; fileName: string; contentType: string; sizeBytes: number; uploadedBy: string; uploadedById: number; uploadedAt: string; storage: string; url: string; downloadUrl: string; publicUrl?: string }
export interface Notification { id: number; type: string; message: string; workOrderId?: number; seen: boolean; createdAt: string }
export interface Technician { id: number; name: string; email: string; skills?: string; skillMatch: boolean; openJobs: number }
export interface Summary {
  filters: { customerId?: number; siteId?: number; technicianId?: number; days?: number };
  total: number; open: number; overdue: number; atRisk: number; breached: number;
  statusCounts: Record<Status, number>;
  slaCompliance: { met: number; total: number; percent: number | null };
  byTechnician: { technicianId: number; name: string; open: number; completed: number; overdue: number }[];
  bySite: { siteId: number; name: string; total: number; open: number; overdue: number }[];
  overdueOrders: WorkOrder[]; generatedAt: string;
}
export interface AuditEntry { id: number; workOrderId: number; workOrderCode: string; workOrderTitle: string; fromStatus?: Status; toStatus: Status; changedBy: string; changedAt: string; note?: string }

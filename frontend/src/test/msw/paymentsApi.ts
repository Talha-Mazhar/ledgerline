import { http, HttpResponse } from 'msw';
import type {
  Answer,
  AuditList,
  Limits,
  Money,
  Notification,
  NotificationList,
  Payment,
  PaymentList,
  PaymentStatus,
  ReviewQueue,
  StaffPayment,
  StaffPaymentList,
  StaffUser,
  StaffUserList,
  StatementMonthList,
  User,
  Verification,
} from '@/shared/api/generated';
import { zChangeRoleRequest, zQuestion, zRejectRequest } from '@/shared/api/generated/zod.gen';
import { allUsers, currentUser, setRole } from '@/test/msw/authApi';
import { API, post, problem, put } from '@/test/msw/http';

/**
 * Fakes of the payments, notifications, verification, statements, assistant and back-office APIs,
 * following api/openapi.yaml like the wallet fake. The generated client checks every response
 * against the contract, so a fixture that drifts from it fails the test.
 */

interface StoredPayment {
  readonly payment: StaffPayment;
  readonly description: string;
  readonly direction: 'IN' | 'OUT';
}

let payments: StoredPayment[] = [];
let notifications: Notification[] = [];
let verification: Verification = { status: 'NOT_STARTED', tier: 'BASIC' };
let reviewQueue: ReviewQueue['items'] = [];
let audit: AuditList['items'] = [];
let assistantOnline = true;
let sequence = 0;

export function resetPayments(): void {
  payments = [];
  notifications = [];
  verification = { status: 'NOT_STARTED', tier: 'BASIC' };
  reviewQueue = [];
  audit = [];
  assistantOnline = true;
  sequence = 0;
}

function nextId(): string {
  sequence += 1;
  return `00000000-0000-4000-9000-${String(sequence).padStart(12, '0')}`;
}

const NOW = '2026-10-31T09:30:00Z';

function party(user: User) {
  return { id: user.id, email: user.email, displayName: user.displayName };
}

/** Records a payment, as the backend does for every movement of money. */
export function recordPayment(options: {
  readonly customer: User;
  readonly kind: StaffPayment['kind'];
  readonly amount: Money;
  readonly description: string;
  readonly status?: PaymentStatus;
  readonly holdReason?: string;
  readonly counterparty?: User;
}): StaffPayment {
  const status = options.status ?? 'COMPLETED';
  const payment: StaffPayment = {
    id: nextId(),
    kind: options.kind,
    status,
    amount: options.amount,
    customer: party(options.customer),
    counterparty: options.counterparty === undefined ? null : party(options.counterparty),
    holdReason: options.holdReason ?? null,
    firstApprover: null,
    secondApprover: null,
    rejectionReason: null,
    refundable: status === 'COMPLETED' && options.kind !== 'EXCHANGE',
    createdAt: NOW,
    updatedAt: NOW,
  };
  payments.unshift({ payment, description: options.description, direction: 'OUT' });
  return payment;
}

/** A notification in the signed-in user's inbox. */
export function givenNotification(title: string, body: string, link: string | null = null) {
  const notification: Notification = {
    id: nextId(),
    kind: 'TEST',
    title,
    body,
    link,
    createdAt: NOW,
    read: false,
  };
  notifications.unshift(notification);
  return notification;
}

/** An identity document waiting for staff. */
export function givenDocumentToReview(user: User) {
  reviewQueue.push({
    userId: user.id,
    email: user.email,
    displayName: user.displayName,
    status: 'PENDING_REVIEW',
    documentName: 'passport.pdf',
    contentType: 'application/pdf',
    submittedAt: NOW,
  });
}

export function givenAssistantOffline(): void {
  assistantOnline = false;
}

function staffOnly() {
  const user = currentUser();
  return user === null || user.role === 'CUSTOMER'
    ? problem(403, 'Not allowed', 'This needs a member of staff.')
    : null;
}

function find(paymentId: unknown) {
  return payments.find((stored) => stored.payment.id === paymentId);
}

function update(stored: StoredPayment, changes: Partial<StaffPayment>): StaffPayment {
  const payment = { ...stored.payment, ...changes, updatedAt: NOW };
  payments = payments.map((candidate) =>
    candidate === stored ? { ...candidate, payment } : candidate,
  );
  return payment;
}

function record(action: string, targetType: string, targetId: string, details: string) {
  audit.unshift({
    id: nextId(),
    actor: currentUser()?.email ?? null,
    action,
    targetType,
    targetId,
    details,
    occurredAt: NOW,
  });
}

export const paymentsHandlers = [
  http.get(`${API}/payments`, () =>
    HttpResponse.json<PaymentList>({
      items: payments
        .filter((stored) => stored.payment.customer.id === currentUser()?.id)
        .map(({ payment, description, direction }): Payment => ({
          id: payment.id,
          kind: payment.kind,
          status: payment.status,
          direction,
          description,
          amount: payment.amount,
          holdReason: payment.holdReason ?? null,
          rejectionReason: payment.rejectionReason ?? null,
          createdAt: payment.createdAt,
          updatedAt: payment.updatedAt,
        })),
    }),
  ),

  http.get(`${API}/notifications`, () =>
    HttpResponse.json<NotificationList>({
      unread: notifications.filter((notification) => !notification.read).length,
      items: notifications,
    }),
  ),

  post('/notifications/read-all', () => {
    notifications = notifications.map((notification) => ({ ...notification, read: true }));
    return new HttpResponse(null, { status: 204 });
  }),

  post('/notifications/:notificationId/read', ({ params }) => {
    notifications = notifications.map((notification) =>
      notification.id === params['notificationId'] ? { ...notification, read: true } : notification,
    );
    return new HttpResponse(null, { status: 204 });
  }),

  http.get(`${API}/verification`, () => HttpResponse.json<Verification>(verification)),

  post('/verification', async ({ request }) => {
    // The upload's file name, read from the raw multipart body: parsing it as FormData depends on
    // how the Node version under test streams jsdom's File, which differs between versions.
    const body = await request.text().catch(() => '');
    const documentName = /filename="([^"]+)"/.exec(body)?.[1] ?? 'document';
    verification = {
      status: 'PENDING_REVIEW',
      tier: 'BASIC',
      documentName,
      submittedAt: NOW,
    };
    return HttpResponse.json<Verification>(verification, { status: 202 });
  }),

  http.get(`${API}/limits`, () =>
    HttpResponse.json<Limits>(
      verification.tier === 'VERIFIED'
        ? { tier: 'VERIFIED', singlePayment: '10000.00', daily: '25000.00', currency: 'EUR' }
        : { tier: 'BASIC', singlePayment: '1000.00', daily: '2000.00', currency: 'EUR' },
    ),
  ),

  http.get(`${API}/wallets/:walletId/statements`, () =>
    HttpResponse.json<StatementMonthList>({ items: [{ month: '2026-09' }, { month: '2026-10' }] }),
  ),

  post('/assistant/questions', async ({ request }) => {
    const { question } = zQuestion.parse(await request.json());
    if (!assistantOnline) {
      return problem(503, 'Assistant unavailable', 'The assistant is not available right now.');
    }
    return HttpResponse.json<Answer>({
      answer: question.toLowerCase().includes('balance')
        ? 'You have €1,250.00 in your EUR wallet.'
        : 'This month you sent €30.00 to Jana Nováková and added €100.00.',
    });
  }),

  // The back office.

  http.get(`${API}/back-office/payments`, ({ request }) => {
    const forbidden = staffOnly();
    if (forbidden !== null) {
      return forbidden;
    }
    // The client sends ?status=HELD,AWAITING_SECOND_APPROVAL; Spring reads either form.
    const asked = new URL(request.url).searchParams
      .getAll('status')
      .flatMap((value) => value.split(','));
    // Without a status, the backend answers with the approval queue.
    const wanted = asked.length === 0 ? ['HELD', 'AWAITING_SECOND_APPROVAL'] : asked;
    return HttpResponse.json<StaffPaymentList>({
      items: payments
        .map((stored) => stored.payment)
        .filter((payment) => wanted.includes(payment.status)),
    });
  }),

  post('/back-office/payments/:paymentId/approve', ({ params }) => {
    const stored = find(params['paymentId']);
    const me = currentUser();
    if (stored === undefined || me === null) {
      return problem(404, 'Payment not found', 'There is no payment with this id.');
    }
    if (stored.payment.status === 'HELD') {
      record('PAYMENT_APPROVED', 'PAYMENT', stored.payment.id, 'First approval');
      return HttpResponse.json<StaffPayment>(
        update(stored, { status: 'AWAITING_SECOND_APPROVAL', firstApprover: me.email }),
      );
    }
    if (stored.payment.status === 'AWAITING_SECOND_APPROVAL') {
      if (stored.payment.firstApprover === me.email) {
        return problem(
          409,
          'Second approver needed',
          'You already approved this payment. A second, different person must approve it.',
        );
      }
      record('PAYMENT_APPROVED', 'PAYMENT', stored.payment.id, 'Second approval; released');
      return HttpResponse.json<StaffPayment>(
        update(stored, { status: 'COMPLETED', secondApprover: me.email, refundable: true }),
      );
    }
    return problem(409, 'Not waiting for approval', 'This payment is not waiting for approval.');
  }),

  post('/back-office/payments/:paymentId/reject', async ({ params, request }) => {
    const stored = find(params['paymentId']);
    if (stored === undefined) {
      return problem(404, 'Payment not found', 'There is no payment with this id.');
    }
    const { reason } = zRejectRequest.parse(await request.json());
    record('PAYMENT_REJECTED', 'PAYMENT', stored.payment.id, reason);
    return HttpResponse.json<StaffPayment>(
      update(stored, { status: 'REJECTED', rejectionReason: reason }),
    );
  }),

  post('/back-office/payments/:paymentId/refund', ({ params }) => {
    const stored = find(params['paymentId']);
    if (stored === undefined) {
      return problem(404, 'Payment not found', 'There is no payment with this id.');
    }
    record('PAYMENT_REFUNDED', 'PAYMENT', stored.payment.id, 'Refunded');
    return HttpResponse.json<StaffPayment>(
      update(stored, { status: 'REFUNDED', refundable: false }),
    );
  }),

  http.get(`${API}/back-office/verifications`, () => {
    const forbidden = staffOnly();
    return forbidden ?? HttpResponse.json<ReviewQueue>({ items: reviewQueue });
  }),

  post('/back-office/verifications/:userId/approve', ({ params }) => {
    reviewQueue = reviewQueue.filter((item) => item.userId !== params['userId']);
    record('VERIFICATION_APPROVED', 'USER', String(params['userId']), 'Identity confirmed');
    return HttpResponse.json<Verification>({
      status: 'VERIFIED',
      tier: 'VERIFIED',
      reviewedAt: NOW,
    });
  }),

  post('/back-office/verifications/:userId/reject', async ({ params, request }) => {
    const { reason } = zRejectRequest.parse(await request.json());
    reviewQueue = reviewQueue.filter((item) => item.userId !== params['userId']);
    record('VERIFICATION_REJECTED', 'USER', String(params['userId']), reason);
    return HttpResponse.json<Verification>({
      status: 'REJECTED',
      tier: 'BASIC',
      rejectionReason: reason,
      reviewedAt: NOW,
    });
  }),

  http.get(`${API}/back-office/users`, ({ request }) => {
    const forbidden = staffOnly();
    if (forbidden !== null) {
      return forbidden;
    }
    const query = (new URL(request.url).searchParams.get('query') ?? '').toLowerCase();
    const items: StaffUser[] = allUsers().filter(
      (user) => user.email.includes(query) || user.displayName.toLowerCase().includes(query),
    );
    return HttpResponse.json<StaffUserList>({ items });
  }),

  put('/back-office/users/:userId/role', async ({ params, request }) => {
    if (currentUser()?.role !== 'ADMIN') {
      return problem(403, 'Not allowed', 'Only an admin can change roles.');
    }
    const { role } = zChangeRoleRequest.parse(await request.json());
    const admins = allUsers().filter((user) => user.role === 'ADMIN');
    if (role !== 'ADMIN' && admins.length === 1 && admins[0]?.id === params['userId']) {
      return problem(409, "Can't remove the last admin", 'Make someone else an admin first.');
    }
    const changed = setRole(String(params['userId']), role);
    if (changed === undefined) {
      return problem(404, 'User not found', 'There is no user with this id.');
    }
    record('ROLE_CHANGED', 'USER', changed.id, `Role set to ${role}`);
    return HttpResponse.json<StaffUser>(changed);
  }),

  http.get(`${API}/back-office/audit`, () => {
    const forbidden = staffOnly();
    return forbidden ?? HttpResponse.json<AuditList>({ items: audit });
  }),
];

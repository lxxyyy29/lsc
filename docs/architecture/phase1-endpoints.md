# Phase 1 Backend-to-Frontend Contract

This document summarizes the phase-1 integration contract between the backend service, the Web admin app, and the H5 field app for the Dongguan Changping closed loop.

Status legend used in this document:

- Implemented: endpoint and payload are backed by current backend code.
- Reserved / pending: payload is defined for delivery alignment, but the current backend codebase does not yet expose the endpoint.

Authentication note for this phase:
- Login endpoints return a bearer token in `data.token`.
- Protected H5 endpoints require `Authorization: Bearer <token>`.
- H5 business APIs also enforce H5 client type and explicit API permissions via `PermissionGuard`.
- Legacy `X-Foundation-*` headers still exist only as a test-only fallback path in `FoundationActorResolver`; they are not the steady-state integration contract for real clients.

## Common response envelope

Implemented backend controllers return a unified envelope:

```json
{
  "success": true,
  "code": "OK",
  "message": "Success",
  "data": {}
}
```

Failure shape:

```json
{
  "success": false,
  "code": "BUSINESS_ERROR_CODE",
  "message": "Human readable error",
  "data": null
}
```

Source of truth:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/common/response/ApiResponse.java`

## Identity / actor context

Current phase-1 auth/RBAC behavior is bearer-token based:

- Web login: `POST /api/auth/login`
- H5 login: `POST /api/h5/auth/login`
- Clients send `Authorization: Bearer <token>` on protected endpoints.
- The bearer token resolves the acting user from the authenticated security context.
- H5 endpoints additionally require the token's client type to be `H5`.
- H5 business endpoints also require explicit API permissions, in addition to assignee ownership checks inside the work-order service.

Important implementation detail:
- `FoundationActorResolver` now prefers the authenticated bearer-token principal.
- `X-Foundation-User-Id` and `X-Foundation-User-Name` are retained only as a test-scenario fallback, not as the delivery contract for real Web/H5 integration.

Source of truth:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/common/security/SecurityConfig.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/auth/security/BearerTokenAuthenticationFilter.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/common/security/FoundationActorResolver.java`

## Status and result vocabulary

### Event statuses

- `PENDING_AUDIT`
- `IN_AUDIT`
- `WAITING_DISPATCH`
- `DISPATCHED_TO_WORK_ORDER`
- `CLOSED`

### Work-order statuses

- `WAITING_ACCEPT`
- `PROCESSING`
- `WAITING_VERIFY`
- `WAITING_CLOSE_CONFIRM`
- `COMPLETED`
- `CLOSED`

### H5 result mapping

UI copy and backend result codes should map as follows:

| UI label | Backend field | Code |
| --- | --- | --- |
| 属实并已处理 | `handle.result` | `RESOLVED` |
| 不属实 | `handle.result` | `NOT_TRUE` |
| 需补充 | `verify.result` | `NEEDS_MORE_EVIDENCE` |
| 属实但需继续处理 | `verify.result` | `CONTINUE_PROCESSING` |

The H5 demo page currently exposes the four Chinese options in the frontend mock layer, while backend APIs accept the English enum-like codes above.

## 1. Event intake payload

Status: Implemented

Endpoint:
- `POST /api/events`

Request body:

```json
{
  "externalEventId": "EXT-20260314-0001",
  "sourceType": "DIRECT_REPORT",
  "sourceSystem": "grid-platform",
  "eventType": "ILLEGAL_BUILDING",
  "title": "占道经营事件",
  "description": "群众反映路口占道经营，影响通行。",
  "occurredAt": "2026-03-14T09:10:00",
  "location": "常平镇振兴路与站前路交叉口",
  "longitude": 113.939521,
  "latitude": 22.971231,
  "evidenceReferences": [
    "https://example.com/media/event-photo-1.jpg",
    "https://example.com/media/event-video-1.mp4"
  ]
}
```

Required fields:
- `externalEventId`
- `sourceType`
- `sourceSystem`
- `eventType`
- `title`
- `occurredAt`
- `location`
- `evidenceReferences` with at least one non-empty item

Success response `data` shape:

```json
{
  "id": 101,
  "eventCode": "EVT-20260314-000101",
  "externalEventId": "EXT-20260314-0001",
  "sourceType": "DIRECT_REPORT",
  "sourceSystem": "grid-platform",
  "eventType": "ILLEGAL_BUILDING",
  "title": "占道经营事件",
  "description": "群众反映路口占道经营，影响通行。",
  "status": "PENDING_AUDIT",
  "occurredAt": "2026-03-14T09:10:00",
  "location": "常平镇振兴路与站前路交叉口",
  "longitude": 113.939521,
  "latitude": 22.971231,
  "evidenceReferences": [
    "https://example.com/media/event-photo-1.jpg",
    "https://example.com/media/event-video-1.mp4"
  ]
}
```

Source of truth:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/controller/EventController.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/dto/CreateEventRequest.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/vo/EventDetailVo.java`

## 2. Audit start payload

Status: Implemented

Endpoint:
- `POST /api/audits/{eventId}/start`

Request body:

```json
{
  "templateId": 12,
  "overrideAuthorized": false
}
```

Notes:
- Body may be omitted; backend tolerates a null request and defaults to `(templateId = null, overrideAuthorized = false)`.
- `templateId` should point to the selected process template.

Success response `data` shape:

```json
{
  "id": 501,
  "processNo": "PI-101-550e8400-e29b-41d4-a716-446655440000",
  "templateId": 12,
  "templateVersion": 1,
  "businessType": "EVENT_AUDIT",
  "businessId": 101,
  "status": "RUNNING",
  "currentNodeOrder": 1,
  "startedAt": "2026-03-14T09:20:00",
  "finishedAt": null,
  "processInstanceId": 501,
  "eventId": 101,
  "eventStatus": "IN_AUDIT",
  "processStatus": "RUNNING",
  "nodes": []
}
```

Source of truth:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/audit/controller/AuditController.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/entity/ProcessInstanceEntity.java`

## 3. Approve payload

Status: Implemented

Endpoint:
- `POST /api/processes/instances/{id}/approve`

Request body:

```json
{
  "nodeId": 1001,
  "remark": "通过"
}
```

Success response `data` shape:

```json
{
  "id": 501,
  "eventId": 101,
  "processStatus": "RUNNING",
  "eventStatus": "IN_AUDIT",
  "nodes": []
}
```

When the final approval node is completed, expect:
- `processStatus = "APPROVED"`
- `eventStatus = "WAITING_DISPATCH"`

Test evidence for the final-node case exists in:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/workorder/DispatchWorkOrderFlowTest.java`

## 4. Reject payload

Status: Implemented

Endpoint:
- `POST /api/processes/instances/{id}/reject`

Request body:

```json
{
  "nodeId": 1001,
  "remark": "退回补充说明"
}
```

Success response `data` shape follows the same `ProcessInstanceEntity` structure as approve.

## 5. Dispatch payload

Status: Implemented

Endpoint:
- `POST /api/work-orders/{eventId}/dispatch`

Request body:

```json
{
  "assigneeUserId": 3001,
  "assigneeName": "处置员张三",
  "remark": "立即处置"
}
```

Behavior:
- Event must already be in `WAITING_DISPATCH`.
- Assignee user must exist.
- Duplicate dispatch for the same event is rejected.

Success response `data` shape:

```json
{
  "id": 9001,
  "workOrderNo": "WO-101-1710400000000",
  "sourceEventId": 101,
  "status": "WAITING_ACCEPT",
  "assigneeUserId": 3001,
  "assigneeName": "处置员张三",
  "dispatcherUserId": 2001,
  "dispatcherName": "Authenticated Dispatcher",
  "acceptedAt": null,
  "completedAt": null,
  "closedAt": null,
  "closeReason": null,
  "createdAt": "2026-03-14T09:45:00",
  "updatedAt": "2026-03-14T09:45:00"
}
```

Source of truth:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/controller/WorkOrderController.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/service/WorkOrderService.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/entity/WorkOrderEntity.java`

## 6. H5 login payload

Status: Implemented

Endpoint:
- `POST /api/h5/auth/login`

Request body:

```json
{
  "account": "worker-zhangsan",
  "password": "123456"
}
```

Success response `data` shape:

```json
{
  "token": "jwt-or-bearer-token",
  "userId": 3001,
  "userName": "巡查员张三",
  "account": "worker-zhangsan",
  "roleCodes": [
    "H5_WORKER"
  ],
  "permissionCodes": [
    "menu:h5:workbench:view",
    "api:auth:h5:me",
    "api:h5:workbench:view",
    "api:h5:workorder:list",
    "api:h5:workorder:detail",
    "api:h5:workorder:accept",
    "api:h5:workorder:arrive",
    "api:h5:workorder:handle",
    "api:h5:workorder:verify"
  ]
}
```

Notes:
- The current contract uses `data.token`, not `accessToken`, `tokenType`, `expiresIn`, or a nested `user` wrapper.
- Successful H5 login requires the user to have the H5 entry permission set.
- The same auth module also exposes `GET /api/h5/auth/me` and `POST /api/h5/auth/logout`.

Source of truth:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/auth/controller/H5AuthController.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/auth/AuthControllerTest.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/api/auth.ts`

## 7. H5 accept payload

Status: Implemented

Endpoint:
- `POST /api/h5/work-orders/{id}/accept`

Auth and authorization requirements:
- `Authorization: Bearer <token>`
- Token client type must be `H5`
- Required API permission: `api:h5:workorder:accept`
- Current user must also be the dispatched assignee for the target work order

Request body:

```json
{
  "remark": "已接单"
}
```

Success response `data` shape:

```json
{
  "id": 9001,
  "status": "PROCESSING",
  "acceptedAt": "2026-03-14T10:00:00"
}
```

## 8. H5 arrive / on-site confirmation payload

Status: Implemented

Endpoint:
- `POST /api/h5/work-orders/{id}/arrive`

Auth and authorization requirements:
- `Authorization: Bearer <token>`
- Token client type must be `H5`
- Required API permission: `api:h5:workorder:arrive`
- Current user must also be the dispatched assignee for the target work order

Request body:

```json
{
  "remark": "已到现场"
}
```

Success response `data` shape:

```json
{
  "id": 9001,
  "status": "PROCESSING"
}
```

## 9. H5 handle payload

Status: Implemented

Endpoint:
- `POST /api/h5/work-orders/{id}/handle`

Auth and authorization requirements:
- `Authorization: Bearer <token>`
- Token client type must be `H5`
- Required API permission: `api:h5:workorder:handle`
- Current user must also be the dispatched assignee for the target work order

Request body for 属实并已处理:

```json
{
  "result": "RESOLVED",
  "remark": "已整改完成"
}
```

Request body for 不属实:

```json
{
  "result": "NOT_TRUE",
  "remark": "现场核查不属实"
}
```

Expected outcomes:
- `RESOLVED` -> work order enters `WAITING_CLOSE_CONFIRM`
- `NOT_TRUE` -> work order enters `CLOSED` and event enters `CLOSED`

## 10. H5 verify payload

Status: Implemented

Endpoint:
- `POST /api/h5/work-orders/{id}/verify`

Auth and authorization requirements:
- `Authorization: Bearer <token>`
- Token client type must be `H5`
- Required API permission: `api:h5:workorder:verify`
- Current user must also be the dispatched assignee for the target work order

Request body for 需补充:

```json
{
  "result": "NEEDS_MORE_EVIDENCE",
  "remark": "补充现场照片"
}
```

Request body for 属实但需继续处理:

```json
{
  "result": "CONTINUE_PROCESSING",
  "remark": "继续跟进处置"
}
```

Expected outcomes:
- `NEEDS_MORE_EVIDENCE` -> work order enters `WAITING_VERIFY`
- `CONTINUE_PROCESSING` -> work order remains `PROCESSING`

Source of truth for H5 accept/arrive/handle/verify:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/controller/H5WorkOrderController.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/dto/AcceptWorkOrderRequest.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/dto/ArriveWorkOrderRequest.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/dto/HandleWorkOrderRequest.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/dto/VerifyWorkOrderRequest.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/service/impl/WorkOrderServiceImpl.java`

## 11. Media upload request / response payloads

Status: Reserved / pending dedicated upload API

Current phase-1 reality:
- Event intake persists `evidenceReferences` as file URLs.
- The backend schema contains media records and event-media persistence paths.
- The H5 and Web UIs include upload-related placeholders and history views.
- No dedicated upload controller endpoint was found in the current backend source tree.

Recommended integration contract for the missing dedicated upload API:

Endpoint:
- `POST /api/media/upload`

Recommended request shape:
- `multipart/form-data`
- Fields:
  - `file`: binary file
  - `businessType`: `EVENT` or `WORK_ORDER`
  - `businessId`: numeric business id
  - `fileType`: `IMAGE` or `VIDEO`

Example response body inside the standard envelope:

```json
{
  "success": true,
  "code": "OK",
  "message": "Success",
  "data": {
    "id": 7001,
    "businessType": "WORK_ORDER",
    "businessId": 9001,
    "fileName": "现场整改完成照片.jpg",
    "fileUrl": "https://example.com/media/2026/03/14/photo-7001.jpg",
    "fileType": "IMAGE",
    "mimeType": "image/jpeg",
    "status": "ACTIVE"
  }
}
```

Important delivery note:
- This payload is documented to unblock cross-team integration discussions, but it is not backed by a current controller implementation in this repository.

Relevant persistence references:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/mapper/EventMapper.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/workorder/WorkOrderDetailView.vue`
- `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/verify/VerifyView.vue`
- `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/history/HistoryView.vue`

## 12. Close-confirm payload

Status: Implemented

Endpoint:
- `POST /api/work-orders/{id}/confirm-close`

Approve close request body:

```json
{
  "approved": true,
  "remark": "确认办结"
}
```

Reject close request body:

```json
{
  "approved": false,
  "remark": "退回继续处理"
}
```

Expected outcomes:
- `approved = true` -> work order becomes `COMPLETED`, event becomes `CLOSED`
- `approved = false` -> work order returns to `PROCESSING`, event remains `DISPATCHED_TO_WORK_ORDER`

## 13. H5 read/query endpoints used in manual verification

Status: Implemented

- `GET /api/h5/workbench`
- `GET /api/h5/work-orders`
- `GET /api/h5/work-orders/{id}`

Auth and authorization requirements:
- `Authorization: Bearer <token>`
- Token client type must be `H5`
- Required API permissions:
  - `GET /api/h5/workbench` -> `api:h5:workbench:view`
  - `GET /api/h5/work-orders` -> `api:h5:workorder:list`
  - `GET /api/h5/work-orders/{id}` -> `api:h5:workorder:detail`

Important behavior:
- Returned work orders are filtered to the authenticated assignee.
- Detail and action endpoints also enforce assignee ownership through the work-order service.
- This is the backend basis for checking that only the dispatched user sees and operates on the assigned work order in H5.

## 14. Automated evidence available before manual verification

The following backend tests already cover core transition rules relevant to this contract:

- dispatch flow and duplicate dispatch protection
- H5 ownership filtering
- accept / arrive transitions
- verify branch transitions
- handle resolved and not-true transitions
- close confirm approve / reject transitions

Relevant files:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/workorder/DispatchWorkOrderFlowTest.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/workorder/H5WorkOrderFlowTest.java`

## 15. Known integration gaps to track during delivery

1. `docs/architecture` did not exist before this task; this document establishes the delivery reference location.
2. Dedicated media upload API is not implemented in the current backend source tree.
3. The H5 frontend result labels are Chinese, while backend APIs use English result codes; integration mapping must be applied when wiring the real submit actions.
4. Legacy `X-Foundation-*` header fallback still exists in `FoundationActorResolver` for test scenarios, but delivery verification should use bearer-token auth as the real integration path.

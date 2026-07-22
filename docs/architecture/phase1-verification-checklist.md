# Phase 1 Integration Verification Checklist

This checklist is the delivery-time manual verification reference for the phase-1 closed loop.

Important notes:

- This file describes required manual verification steps. It does not claim that every step has been executed in this documentation subtask.
- Backend, Web, and H5 were previously reported as implemented and passing their automated test/build steps before this subtask.
- In this Task 14 documentation pass, the checklist is being written and aligned to the implemented code and existing automated tests; it is not a claim of fresh end-to-end manual execution.
- Because this environment may not expose a global `pnpm` binary, use `npx pnpm` where needed.

## A. Preconditions

Before running the manual checklist, prepare the following:

1. Start backend, web, and h5 locally.
2. Ensure backend database and required environment configuration are ready.
3. Prepare at least:
   - one Web operator account able to start audit, approve process nodes, dispatch, and confirm close
   - one concrete H5 field user that will be used as the dispatched assignee
   - one different H5 user used to prove that non-assignees cannot see the dispatched work order
4. Prepare or confirm at least one process template that can be selected when starting audit.
5. Prepare test image/video files for evidence upload.
6. Prepare valid Web and H5 test accounts with the required menu/API permissions for the flows being verified.
7. If upload verification is in scope, confirm ahead of time whether the environment exposes any temporary file hosting path, because a dedicated backend upload controller is still a known gap.

## B. Required startup and verification commands

These are the required commands for phase-1 verification. Record actual outputs separately when you run them.

### Backend

```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw test
```

### Web

```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npx pnpm test && npx pnpm build
```

### H5

```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npx pnpm test && npx pnpm build
```

## C. Manual closed-loop checklist

Use a fresh event for each branch case when necessary so that one branch result does not block another.

### Main business path

1. **create event**
   - Create a new event through the agreed intake path.
   - Verify the event is created successfully and receives an event code/id.
   - Verify intake evidence references are saved.

2. **view event in Web**
   - Open the Web event list/detail page.
   - Verify the newly created event is visible.
   - Verify title, type, source, occurred time, location, description, and evidence are displayed correctly.

3. **start audit with a process template**
   - In Web, start audit for the event and select a process template.
   - Verify the event enters audit and the process instance is created.

4. **approve nodes until the event is waiting for dispatch**
   - Approve each required audit node in Web.
   - Verify the final approval moves the event into `WAITING_DISPATCH`.
   - Verify the process instance is approved/completed according to the UI/backend contract.

5. **dispatch to a concrete user**
   - Dispatch the event to the chosen concrete H5 user.
   - Verify a work order is generated.
   - Verify the event becomes `DISPATCHED_TO_WORK_ORDER` and the work order becomes `WAITING_ACCEPT`.

6. **log into H5 as that concrete dispatched user**
   - Sign into H5 as the exact dispatched assignee through `POST /api/h5/auth/login`.
   - Verify the login response returns `data.token`, `data.userId`, `data.userName`, `data.account`, `data.roleCodes`, and `data.permissionCodes`.
   - Verify subsequent H5 requests use `Authorization: Bearer <token>`.

7. **verify only that user sees the assigned work order in the workbench**
   - In H5 workbench and work-order list, verify the dispatched user can see the assigned work order.
   - Log in as a different H5 user and verify that different user does not see the assigned work order.
   - Record both user identities used for this proof.

8. **accept work order**
   - In H5, open the work order and perform accept.
   - Verify work order status becomes `PROCESSING`.
   - Verify acceptance time or operation record is visible if the UI exposes it.

9. **record arrive/on-site confirmation**
   - In H5, submit arrive/on-site confirmation.
   - Verify the work order remains `PROCESSING`.
   - Verify an arrive record or operation log is visible where applicable.

10. **attempt image/video evidence upload and record whether the step is blocked**
    - Attempt to upload at least one image and one video as evidence from H5.
    - If the current environment has a working temporary upload path, verify the upload succeeds without client or server error.
    - Record the file names used and where they are stored/displayed.
    - If upload is still connected through a placeholder or pending dedicated API, mark this step as blocked/pending rather than claiming success.

11. **submit 属实并已处理 and verify the work order enters waiting-close-confirm**
    - Submit the handling result corresponding to `属实并已处理`.
    - Verify the backend/UI mapping uses `RESOLVED` if applicable.
    - Verify the work order status becomes `WAITING_CLOSE_CONFIRM`.

12. **confirm close in Web and verify event is closed and work order is completed**
    - In Web, perform close confirmation approval.
    - Verify the event becomes `CLOSED`.
    - Verify the work order becomes `COMPLETED`.
    - Verify completion/close timestamps appear where expected.

### Branch cases

13. **run branch case: submit 需补充 and verify work order enters waiting-verify**
    - Use a fresh dispatched work order in `PROCESSING`.
    - Submit the verification result corresponding to `需补充`.
    - Verify backend/UI mapping uses `NEEDS_MORE_EVIDENCE` if applicable.
    - Verify the work order enters `WAITING_VERIFY`.

14. **run branch case: submit 属实但需继续处理 and verify work order stays processing**
    - Use a fresh dispatched work order in `PROCESSING`.
    - Submit the verification result corresponding to `属实但需继续处理`.
    - Verify backend/UI mapping uses `CONTINUE_PROCESSING` if applicable.
    - Verify the work order remains `PROCESSING`.

15. **run branch case: submit 不属实 and verify both event and work order are closed**
    - Use a fresh dispatched work order in `PROCESSING`.
    - Submit the handling result corresponding to `不属实`.
    - Verify backend/UI mapping uses `NOT_TRUE` if applicable.
    - Verify both the event and work order become `CLOSED`.

16. **run branch case: reject close confirmation and verify work order returns to processing while the event remains dispatched**
    - Use a fresh work order already in `WAITING_CLOSE_CONFIRM`.
    - In Web, reject close confirmation.
    - Verify the work order returns to `PROCESSING`.
    - Verify the event remains `DISPATCHED_TO_WORK_ORDER` and does not close.

17. **verify uploaded evidence is visible in H5 history and the corresponding Web event/work-order detail views**
    - Open H5 history and verify the uploaded evidence can be located.
    - Open the related Web event detail and work-order detail pages.
    - Verify the same uploaded evidence is visible in the corresponding detail views.
    - If dedicated upload/view integration is still incomplete, record the exact missing surface rather than marking the step passed.

## D. Evidence recording template

For each executed run, capture at least:

- execution date/time
- operator name
- backend commit/build identifier if applicable
- web build identifier if applicable
- h5 build identifier if applicable
- test environment URL(s)
- event id / event code used
- work order id / work order no used
- dispatched user id/name
- alternate user id/name used for isolation verification
- screenshots or screen recordings for key checkpoints
- uploaded file names
- pass/fail result per checklist item
- notes for any workaround, gap, or blocker

## E. Known delivery caveats to watch during manual verification

1. H5 login is implemented and should be verified through the real bearer-token flow, not through `X-Foundation-*` request headers.
2. Dedicated media upload API appears to be a delivery contract item, but a backend upload controller was not found in the current source tree.
3. H5 UI labels use Chinese result text, while backend business APIs currently use English result codes such as `RESOLVED`, `NOT_TRUE`, `NEEDS_MORE_EVIDENCE`, and `CONTINUE_PROCESSING`.
4. H5 read/write endpoints also depend on client-type and permission checks; if a user can log in but still cannot access a step, record the exact missing permission or forbidden response.
5. If any remaining gap affects manual verification, mark the affected steps as pending or blocked with specifics.

## F. Automated coverage already present in codebase

The following implemented automated tests materially support this checklist, but they are not a substitute for the required manual walkthrough:

- dispatch flow and duplicate protection
- H5 assignee-only visibility and ownership restrictions
- accept transition
- arrive transition
- wait-for-more-evidence branch
- continue-processing branch
- resolved-to-waiting-close-confirm branch
- not-true close branch
- close-confirm approve and reject branches

Reference files:
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/workorder/DispatchWorkOrderFlowTest.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/workorder/H5WorkOrderFlowTest.java`
- `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/tests/h5-closed-loop-pages.spec.ts`

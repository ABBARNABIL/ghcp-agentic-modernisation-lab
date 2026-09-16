# Order summary candidate: code review exercise

> **TRAINING ONLY - INTENTIONALLY FLAWED - DO NOT MERGE.**
> This candidate deliberately violates parts of the contract below. It is not
> production-ready and must not be imported into the application.

Review `OrderSummaryCandidate.java` as a proposed implementation of an in-memory
order-summary and payment-reconciliation component. Assess observable correctness
against this contract, not style or the absence of a framework.

## Input and execution contract

- Use synthetic orders only. The caller supplies a non-null list of non-null,
  immutable orders. Identifiers are nonblank; `(customerId, orderId)` is unique
  within the snapshot. Different customers may use the same `orderId`.
- All money uses one currency. Subtotals and payments are nonnegative, exact
  multiples of one cent. Decimal representations may have different scales.
  `discountPercent` is an integer from 0 through 100 inclusive.
- The candidate takes a defensive snapshot and is called sequentially, not
  concurrently. It has no network, persistence, application routing, or deployment
  integration. Input validation outside these preconditions is not part of this
  exercise; the pagination argument rules below are part of the contract.

## Expected behavior

| Operation | Required behavior |
| --- | --- |
| Amount due per order | Subtract `subtotal * discountPercent / 100` from the subtotal, then round that order's result to two decimal places using `HALF_UP`. |
| `summarize(customerId)` | Include every order for that customer, regardless of pagination. Return the order count, sum of rounded amounts due, sum of payments, and count of reconciled orders. Both returned monetary totals have two decimal places. |
| Reconciliation | An order is reconciled when its payment and rounded amount due are numerically equal. Display formatting must not affect this decision. Underpayment and overpayment are both unreconciled. |
| Customer isolation | Each summary depends only on that customer's orders. Results must remain the same when other customers are summarized before, after, or between repeated calls. |
| `page(customerId, pageIndex, pageSize)` | Filter by customer first and preserve the original snapshot order. Use zero-based pages: skip `pageIndex * pageSize` orders, then return up to `pageSize` remaining orders. A full page contains exactly `pageSize` orders, including when `pageSize` is 1. Returned lists are read-only. |
| Empty and invalid cases | An unknown customer has a zero summary and empty pages. A page starting beyond the last order is empty. Negative page indices or nonpositive page sizes throw `IllegalArgumentException`. |

## Review and isolation

Report reproducible correctness problems with a small synthetic input, expected
output, observed output, and the responsible lines. Do not suppress a real defect
because this is labeled a training fixture. The answer key and instructor probes
are deliberately not included in the candidate.

The source uses only the Java standard library. Compile it with Java 25's `javac
--release 25 -d <scratch-classes>`, supplying the candidate file as the source.
Use a scratch output directory outside the application; there is no executable
entry point or additional dependency to install.

This directory is outside the Order Service's Maven production and test source
roots. No POM, application source, frontend, modernization artifact, or deployment
file is changed. The separate review instruction applies only to this exercise's
Java files.
